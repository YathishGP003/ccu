package a75f.io.messaging.handler;

import static a75f.io.logic.bo.building.BackfillUtilKt.updateBackfillDuration;
import static a75f.io.messaging.handler.DataSyncHandler.isCloudEntityHasLatestValue;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HGrid;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HVal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.sync.PointWriteCache;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.vrv.VrvControlMessageCache;
import a75f.io.logic.bo.util.DemandResponseMode;
import a75f.io.logic.bo.util.DesiredTempDisplayMode;
import a75f.io.logic.interfaces.IntrinsicScheduleListener;
import a75f.io.logic.interfaces.ModbusDataInterface;
import a75f.io.logic.interfaces.ModbusWritableDataInterface;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.messaging.MessageHandler;
import a75f.io.messaging.exceptions.MessageHandlingFailed;
import static a75f.io.api.haystack.HayStackConstants.WRITABLE_ARRAY_DURATION;
import static a75f.io.api.haystack.HayStackConstants.WRITABLE_ARRAY_LEVEL;
import static a75f.io.api.haystack.HayStackConstants.WRITABLE_ARRAY_VAL;
import static a75f.io.api.haystack.HayStackConstants.WRITABLE_ARRAY_WHO;
public class UpdatePointHandler implements MessageHandler
{
    public static final String CMD = "updatePoint";
    private static ZoneDataInterface zoneDataInterface = null;
    private static IntrinsicScheduleListener intrinsicScheduleListener = null;
    private static ModbusDataInterface modbusDataInterface = null;
    private static ModbusWritableDataInterface modbusWritableDataInterface = null;

    public static void handlePointUpdateMessage(final JsonObject msgObject, Long timeToken, Boolean isDataSync) throws MessageHandlingFailed {
        String pointUid = "@" + msgObject.get("id").getAsString();
        String pointLevel = msgObject.get("level").getAsString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> pointEntity = hayStack.readMapById(pointUid);
        if(!isCloudEntityHasLatestValue(pointEntity, timeToken)){
            Log.i("ccu_read_changes","CCU HAS LATEST VALUE ");
            return;
        }
        PointWriteCache.Companion.getInstance().clearPointWriteInCache(pointUid, pointLevel);

        if (HSUtil.isBuildingTuner(pointUid, hayStack)  ||  (HSUtil.isSchedulable(pointUid, hayStack))) {
            HashMap<Object, Object> buildingTunerPoint = hayStack.readMapById(pointUid);
            TunerUpdateHandler.updateBuildingTuner(msgObject, CCUHsApi.getInstance());
            if (buildingTunerPoint.containsKey("displayUnit") && zoneDataInterface != null) {
                zoneDataInterface.refreshScreen("", true);
            }
            return;
        } else if (hayStack.readMapById(pointUid).containsKey(Tags.TUNER)) {
            TunerUtil.refreshEquipTuners();
        }

        Point localPoint = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(pointUid)).build();
        CcuLog.d(L.TAG_CCU_PUBNUB, " handleMessage for" + Arrays.toString(localPoint.getMarkers().toArray()));

        //move this class to a separate file
        if(HSUtil.isPhysicalPointUpdate(localPoint)){
            String value = msgObject.get(WRITABLE_ARRAY_VAL).getAsString();
            CcuLog.i(L.TAG_CCU_PUBNUB, "update physical point : "+localPoint.getDisplayName() +
                    " Value: "+value);
            if(value.isEmpty()){
                //When a level is deleted, it currently generates a message with empty value.
                //Handle it here.
                int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
                hayStack.clearPointArrayLevel(localPoint.getId(), level, true);
                hayStack.writeHisValById(localPoint.getId(), HSUtil.getPriorityVal(localPoint.getId()));
            } else {
                hayStack.writePointLocal(localPoint.getId(), msgObject.get(WRITABLE_ARRAY_LEVEL).getAsInt(),
                        msgObject.get(WRITABLE_ARRAY_WHO).getAsString(), Double.parseDouble(value), msgObject.has(WRITABLE_ARRAY_DURATION) ?
                                msgObject.get(WRITABLE_ARRAY_DURATION).getAsInt() : 0);
                hayStack.writeHisValById(localPoint.getId(),Double.parseDouble( value));
                // Read priority array list to get duration of levels.
                // read all points once
                fetchRemotePoint(pointUid, isDataSync, msgObject);
            }
            return;
        }
        /*
        Reconfiguration handled for PI profile
         */
        if (HSUtil.isPIConfig(pointUid, CCUHsApi.getInstance())) {
            PIReConfigHandler.updateConfigPoint(msgObject, localPoint);
            updatePoints(localPoint);
            return;
        }


        //Handle DCWB specific system config here.
        if (HSUtil.isDcwbConfig(pointUid, CCUHsApi.getInstance())) {
            DcwbConfigHandler.updateConfigPoint(msgObject, localPoint, CCUHsApi.getInstance());
            updateUI(localPoint);
            return;
        }
        
        if (HSUtil.isSystemConfigOutputPoint(pointUid, CCUHsApi.getInstance())
                || HSUtil.isSystemConfigHumidifierType(pointUid, CCUHsApi.getInstance())
                || HSUtil.isSystemConfigIE(pointUid, CCUHsApi.getInstance())
                || (HSUtil.skipUserIntentForV2(localPoint) && HSUtil.isAdvanceAhuV2(pointUid, CCUHsApi.getInstance()))) {
            ConfigPointUpdateHandler.updateConfigPoint(msgObject, localPoint, CCUHsApi.getInstance());
            updatePoints(localPoint);
            hayStack.scheduleSync();
            return;
        }

        if (HSUtil.isSSEConfig(pointUid, CCUHsApi.getInstance())) {
            CCUHsApi ccuHsApi = CCUHsApi.getInstance();
            SSEConfigHandler.updateConfigPoint(msgObject, localPoint, ccuHsApi);
            updatePoints(localPoint);
            SSEConfigHandler.updateTemperatureMode(localPoint, ccuHsApi);
            return;
        }

        if (HSUtil.isMonitoringConfig(pointUid, CCUHsApi.getInstance())) {
            HyperStatMonitoringConfigHandler.updateConfigPoint(msgObject, localPoint, CCUHsApi.getInstance());
            updatePoints(localPoint);
            return;
        }

        if (HSUtil.isHyperStatConfig(pointUid, CCUHsApi.getInstance())
                && !localPoint.getMarkers().contains(Tags.DESIRED)
                && !localPoint.getMarkers().contains(Tags.SCHEDULE_TYPE)
                && !localPoint.getMarkers().contains(Tags.TUNER)) {
            HyperstatReconfigurationHandler.Companion.handleHyperStatConfigChange(msgObject, localPoint, CCUHsApi.getInstance());
            updatePoints(localPoint);
            if (localPoint.getMarkers().contains(Tags.USERINTENT) && localPoint.getMarkers().contains(Tags.CONDITIONING)) {
                DesiredTempDisplayMode.setModeTypeOnUserIntentChange(localPoint.getRoomRef(), CCUHsApi.getInstance());
            }
            if (localPoint.getMarkers().contains(Tags.VRV)) {
                VrvControlMessageCache.getInstance().setControlsPending(Integer.parseInt(localPoint.getGroup()));
            }
            return;
        }

        if (HSUtil.isHyperStatSplitConfig(pointUid, CCUHsApi.getInstance())
                && !localPoint.getMarkers().contains(Tags.DESIRED)
                && !localPoint.getMarkers().contains(Tags.SCHEDULE_TYPE)
                && !localPoint.getMarkers().contains(Tags.TUNER)) {
            HyperstatSplitReconfigurationHandler.Companion.handleHyperStatSplitConfigChange(msgObject, localPoint, CCUHsApi.getInstance());
            updatePoints(localPoint);
            return;
        }

        /* Only the config changes require profile specific handling.
         * DesiredTemp or Schedule type updates are handled using generic implementation below.
         */
        if (HSUtil.isStandaloneConfig(pointUid, CCUHsApi.getInstance())
                        && !localPoint.getMarkers().contains(Tags.DESIRED)
                        && !localPoint.getMarkers().contains(Tags.SCHEDULE_TYPE)
                && !localPoint.getMarkers().contains(Tags.TUNER)) {
            StandaloneConfigHandler.updateConfigPoint(msgObject, localPoint, CCUHsApi.getInstance());
            updateUI(localPoint);
            return;
        }
    
        if (HSUtil.isDamperReheatTypeConfig(pointUid, hayStack)) {
            DamperReheatTypeHandler.updatePoint(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
            return;
        }

        if(HSUtil.isVAVTrueCFMConfig(pointUid, CCUHsApi.getInstance())){
            TrueCFMVAVConfigHandler.updateVAVConfigPoint(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
            return;
        }

        if (HSUtil.isVAVZonePriorityConfig(pointUid, CCUHsApi.getInstance())) {
            VAVZonePriorityHandler.updateVAVZonePriority(msgObject, localPoint);
        }

        if(HSUtil.isDABTrueCFMConfig(pointUid, CCUHsApi.getInstance())){
            TrueCFMDABConfigHandler.updateDABConfigPoint(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
            return;
        }

        if(HSUtil.isMaxCFMCoolingConfigPoint(pointUid, CCUHsApi.getInstance())){
            TrueCFMVAVConfigHandler.updateMinCoolingConfigPoint(msgObject, localPoint, hayStack);
            TrueCFMVAVConfigHandler.updateAirflowCFMProportionalRange(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
            return;
        }

        if(HSUtil.isMaxCFMReheatingConfigPoint(pointUid, CCUHsApi.getInstance())){
            TrueCFMVAVConfigHandler.updateMinReheatingConfigPoint(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
            return;
        }

        if (HSUtil.isACBRelay1TypeConfig(pointUid, CCUHsApi.getInstance())) {
            ACBUpdatePointHandler.Companion.updateACBRelay1Type(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
            return;
        }

        if (HSUtil.isACBCondensateTypeConfig(pointUid, CCUHsApi.getInstance())) {
            ACBUpdatePointHandler.Companion.updateACBCondensateType(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
            return;
        }

        if (HSUtil.isACBValveTypeConfig(pointUid, CCUHsApi.getInstance())) {
            ACBUpdatePointHandler.Companion.updateACBValveType(msgObject, localPoint, hayStack);
            hayStack.scheduleSync();
        }

        if(HSUtil.isTIProfile(pointUid, CCUHsApi.getInstance())){
            TIConfigHandler.Companion.updateTIConfig(msgObject,localPoint,hayStack);
        }
        if(DemandResponseMode.isDemandResponseConfigPoint(pointEntity)){
            DemandResponseMode.handleDRMessageUpdate(pointEntity, hayStack, msgObject, zoneDataInterface);
            return;
        }
        if (HSUtil.isPointBackfillConfigPoint(pointUid, CCUHsApi.getInstance())) {
            JsonElement backFillVal = msgObject.get("val");
            if (!backFillVal.isJsonNull()){
                updateBackfillDuration(backFillVal.getAsDouble());
            }
        }

        if (localPoint.getMarkers().contains("modbus")){
            ModbusHandler.updatePoint(msgObject,localPoint);
            if (modbusDataInterface != null) {
                modbusDataInterface.refreshScreen(localPoint.getId());
            }
            if (localPoint.getMarkers().contains(Tags.WRITABLE) && modbusWritableDataInterface != null) {
                modbusWritableDataInterface.writeRegister(localPoint.getId());
            }
        }

        if (CCUHsApi.getInstance().isEntityExisting(pointUid))
        {
            fetchRemotePoint(pointUid, isDataSync, msgObject);

            //TODO- Should be removed one pubnub is stable
            logPointArray(localPoint);
        
            try {
                Thread.sleep(10);
                updatePoints(localPoint);
            } catch (InterruptedException e) {
                CcuLog.e(L.TAG_CCU_MESSAGING, "Error in thread sleep", e);
            }
        
        } else {
            CcuLog.d(L.TAG_CCU_PUBNUB, "Received for invalid local point : " + pointUid);
        }

        if (HSUtil.isPointUpdateNeedsSystemProfileReset(pointUid, hayStack)
                    && L.ccu().systemProfile != null) {
            L.ccu().systemProfile.reset();
        }
        if (localPoint.getMarkers().contains(Tags.VRV)) {
            VrvControlMessageCache.getInstance().setControlsPending(Integer.parseInt(localPoint.getGroup()));
        }
    }
    
    /**
     * Replace local point array with point array values from server
     */
    private static void fetchRemotePoint(String pointUid, Boolean isDataSync, JsonObject msgObject) throws MessageHandlingFailed {
        double level;
        double val;
        double duration;
        HDateTime lastModifiedDateTime;
        CCUHsApi.getInstance().deletePointArray(pointUid);
        if (isDataSync) {
            level = Double.parseDouble(msgObject.get("level").getAsString());
            val = Double.parseDouble(msgObject.get("val").getAsString());
            lastModifiedDateTime = HDateTime.make(msgObject.get("lastModifiedDateTime").getAsString());
            duration = Double.parseDouble(msgObject.get("duration").getAsString());
            CcuLog.i(L.TAG_CCU_READ_CHANGES,"Read changes point "+"Level: "+level+"value: "+val+
                    "lastModifiedDateTime: "+lastModifiedDateTime+"duration: "+duration);
            CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(pointUid), (int) level,
                    CCUHsApi.getInstance().getCCUUserName(), HNum.make(val), HNum.make(duration), lastModifiedDateTime);
        } else {
            HGrid pointGrid = CCUHsApi.getInstance().readPointArrRemote(pointUid);
            if (pointGrid == null) {
                CcuLog.d(L.TAG_CCU_PUBNUB, "Failed to read remote point : " + pointUid);
                throw new MessageHandlingFailed("Failed to read remote point");
            }
            //CcuLog.d(L.TAG_CCU_PUBNUB+ " REMOTE ARRAY: ", HZincWriter.gridToString(pointGrid));
            Iterator it = pointGrid.iterator();
            lastModifiedDateTime = null;
            while (it.hasNext()) {
                HRow r = (HRow) it.next();
                String who = r.get("who").toString();

                try {
                    level = Double.parseDouble(r.get("level").toString());
                    val = Double.parseDouble(r.get("val").toString());
                    HVal durHVal = r.get("duration", false);
                    Object lastModifiedTimeTag = r.get("lastModifiedDateTime", false);
                    if (lastModifiedTimeTag != null) {
                        lastModifiedDateTime = (HDateTime) lastModifiedTimeTag;
                    } else {
                        lastModifiedDateTime = HDateTime.make(System.currentTimeMillis());
                    }
                    double durationRemote = durHVal == null ? 0d : Double.parseDouble(durHVal.toString());
                    //If duration shows it has already expired, then just write 1ms to force-expire it locally.
                    duration = (durationRemote == 0 ? 0 : (durationRemote - System.currentTimeMillis()) > 0 ? (durationRemote - System.currentTimeMillis()) : 1);
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Remote point:  level " + level + " val " + val + " who " + who + " duration " + durationRemote + " dur " + duration);
                    CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(pointUid), (int) level,
                            CCUHsApi.getInstance().getCCUUserName(), HNum.make(val), HNum.make(duration), lastModifiedDateTime);

                } catch (NumberFormatException e) {
                    CcuLog.e(L.TAG_CCU_MESSAGING, "Error in parsing remote point array", e);
                }

            }

        }

    }

    private static void logPointArray(Point localPoint) {
        ArrayList values = CCUHsApi.getInstance().readPoint(localPoint.getId());
        if (values != null && !values.isEmpty()) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    double duration = Double.parseDouble(valMap.get("duration").toString());
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Updated point " + localPoint.getDisplayName() + " , level: " + l + " , val :" + Double.parseDouble(valMap.get("val").toString())
                                               + " duration " + (duration > 0 ? duration - System.currentTimeMillis() : duration));
                }
            }
        }
    }


    private static void updatePoints(Point p){
        String luid = p.getId();
        boolean updateZoneUi = false;
        boolean isScheduleType = false;

        if (p.getMarkers().contains("desired") && !p.getMarkers().contains("modbus")) {
            SystemScheduleUtil.handleDesiredTempUpdate(p, false, 0);
            updateZoneUi = true;
        }

        if (p.getMarkers().contains("scheduleType") && !p.getMarkers().contains("modbus")) {
            SystemScheduleUtil.handleScheduleTypeUpdate(p);
            updateZoneUi = true;
            isScheduleType = true;
        }
    
        if (p.getMarkers().contains("his")) {
            CCUHsApi.getInstance().writeHisValById(luid, CCUHsApi.getInstance().readPointPriorityVal(luid));
            updateZoneUi = true;
        }

        if (updateZoneUi && zoneDataInterface != null) {
            Log.i("PubNub","Zone Data Received Refresh "+p.getDisplayName());
            zoneDataInterface.refreshScreen(luid, true);
            if(intrinsicScheduleListener != null)
                intrinsicScheduleListener.updateIntrinsicSchedule();
        }
        
        if (p.getMarkers().contains("modbus")){
            if (modbusDataInterface != null) {
                modbusDataInterface.refreshScreen(luid);
            }
            if (p.getMarkers().contains(Tags.WRITABLE) && modbusWritableDataInterface != null) {
                modbusWritableDataInterface.writeRegister(p.getId());
            }
        }
        if(isScheduleType){
            UpdateScheduleHandler.refreshIntrinsicSchedulesScreen();
        }
    }
    
    /**
     * Should separate his write from above method.
     * There are cases where received his val is not valid and should be ignored.
     *
     */
    private static void updateUI(Point updatedPoint) {
        if (zoneDataInterface != null) {
            Log.i("PubNub","Zone Data Received Refresh");
            zoneDataInterface.refreshScreen(updatedPoint.getId(), true);
        }
    }

    public static void setZoneDataInterface(ZoneDataInterface in) { zoneDataInterface = in; }
    public static void setModbusDataInterface(ModbusDataInterface in) { modbusDataInterface = in; }
    public static void setIntrinsicScheduleListener(IntrinsicScheduleListener in){ intrinsicScheduleListener = in;}
    public static void setSystemDataInterface(ZoneDataInterface in) { zoneDataInterface = in; }
    public static void setModbusWritableDataInterface(ModbusWritableDataInterface in) { modbusWritableDataInterface = in; }
    private static boolean canIgnorePointUpdate(String pbSource, String pointUid, CCUHsApi hayStack) {
        HashMap ccu = hayStack.read("ccu");
        String ccuName = ccu.get("dis").toString();
    
        //Notification for update from the same CCU by using ccu_deviceId format..
        if (pbSource.equals(hayStack.getCCUUserName())) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "PubNub received for CCU write : Ignore "+pbSource);
            return true;
        }
    
        //Notification for updates which are local to a CCU.
        //Some places we still update the user as ccu_displayName. Until that is removed, we will keep name
        //comparison too.
        if (pbSource.equals("ccu_"+ccuName) || pbSource.equals("Scheduler") || pbSource.equals("manual")
            || pbSource.equals("OccupancySensor")) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "PubNub received for CCU write : Ignore");
            return true;
        }

        //If point does not exist on this CCU, return true (ignore)
        if (! CCUHsApi.getInstance().isEntityExisting(pointUid)) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "Point to update does not exist (so ignore): " + pointUid);
            return true;
        }

        return false;
    }

    @NonNull
    @Override
    public List<String> getCommand() {
        return Collections.singletonList(CMD);
    }

    @Override
    public void handleMessage(@NonNull JsonObject jsonObject, @NonNull Context context) throws MessageHandlingFailed {
        long timeToken = jsonObject.get("timeToken").getAsLong();
        handlePointUpdateMessage(jsonObject, timeToken, false);
    }

    @Override
    public boolean ignoreMessage(@NonNull JsonObject jsonObject, @NonNull Context context) {

        String src = jsonObject.get("who").getAsString();
        String pointUid = "@" + jsonObject.get("id").getAsString();
        return canIgnorePointUpdate(src, pointUid, CCUHsApi.getInstance());
    }
}
