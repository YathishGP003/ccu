package a75f.io.logic.pubnub;

import android.util.Log;

import com.google.gson.JsonObject;

import org.projecthaystack.HGrid;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HVal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.jobs.ScheduleProcessJob;

public class UpdatePointHandler
{
    public static final String CMD = "updatePoint";
    private static ZoneDataInterface zoneDataInterface = null;
    private static ModbusDataInterface modbusDataInterface = null;

    public static void handleMessage(final JsonObject msgObject) {
        String src = msgObject.get("who").getAsString();
        String pointGuid = msgObject.get("id").getAsString();
        CCUHsApi hayStack = CCUHsApi.getInstance();

        if (canIgnorePointUpdate(src, pointGuid, hayStack)) {
            return;
        }

        String luid = hayStack.getLUID("@" + pointGuid);

        if (HSUtil.isBuildingTuner(luid, hayStack)) {
            TunerUpdateHandler.updateBuildingTuner(msgObject, CCUHsApi.getInstance());
            return;
        }

        Point localPoint = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(luid)).build();
        if (HSUtil.isSystemConfigOutputPoint(luid, CCUHsApi.getInstance())
                            || HSUtil.isSystemConfigHumidifierType(luid, CCUHsApi.getInstance())) {
            ConfigPointUpdateHandler.updateConfigPoint(msgObject, localPoint, CCUHsApi.getInstance());
            updatePoints(localPoint);
            return;
        }

        if (luid != null && luid != "") {
            HGrid pointGrid = CCUHsApi.getInstance().readPointArrRemote("@" + pointGuid);
            if (pointGrid == null) {
                CcuLog.d(L.TAG_CCU_PUBNUB, "Failed to read remote point point : " + pointGuid);
                return;
            }
            //CcuLog.d(L.TAG_CCU_PUBNUB+ " REMOTE ARRAY: ", HZincWriter.gridToString(pointGrid));
            CCUHsApi.getInstance().deletePointArray(luid);
            Iterator it = pointGrid.iterator();
            while (it.hasNext()) {
                HRow r = (HRow) it.next();
                String who = r.get("who").toString();

                try {
                    double level = Double.parseDouble(r.get("level").toString());
                    double val = Double.parseDouble(r.get("val").toString());
                    HVal durHVal = r.get("duration", false);
                    double duration = durHVal == null ? 0d : Double.parseDouble(durHVal.toString());
                    //If duration shows it has already expired, then just write 1ms to force-expire it locally.
                    double dur = (duration == 0 ? 0 : (duration - System.currentTimeMillis() ) > 0 ? (duration - System.currentTimeMillis()) : 1);
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Remote point:  level " + level + " val " + val + " who " + who + " duration "+duration+" dur "+dur);
                    CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(luid), (int) level, CCUHsApi.getInstance().getCCUUserName(), HNum.make(val), HNum.make(dur));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            //TODO- Should be removed one pubnub is stable
            logPointArray(localPoint);
        
            try {
                Thread.sleep(100);
                updatePoints(localPoint);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        
        } else {
            CcuLog.d(L.TAG_CCU_PUBNUB, "Received for invalid local point : " + luid);
        }
    }

    private static void logPointArray(Point localPoint) {
        ArrayList values = CCUHsApi.getInstance().readPoint(localPoint.getId());
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    Double duration = Double.parseDouble(valMap.get("duration").toString());
                    CcuLog.d(L.TAG_CCU_PUBNUB, "Updated point " + localPoint.getDisplayName() + " , level: " + l + " , val :" + Double.parseDouble(valMap.get("val").toString())
                                               + " duration " + (duration > 0 ? duration - System.currentTimeMillis() : duration));
                }
            }
        }
    }


    private static void updatePoints(Point p){
        String luid = p.getId();
        if (p.getMarkers().contains("his"))
        {
            CCUHsApi.getInstance().writeHisValById(luid, CCUHsApi.getInstance().readPointPriorityVal(luid));
            if (zoneDataInterface != null) {
                Log.i("PubNub","Zone Data Received Refresh");
                zoneDataInterface.refreshScreen(luid);
            }
            /*if (systemDataInterface != null) {
                Log.i("PubNub","System Data Received Refresh");
                systemDataInterface.refreshScreen(luid);
            }*/
        }

        if (p.getMarkers().contains("desired"))
        {
            ScheduleProcessJob.handleDesiredTempUpdate(p, false, 0);
            if (zoneDataInterface != null) {
                Log.i("PubNub","Zone Data Received Refresh");
                zoneDataInterface.refreshScreen(luid);
            }
        }

        if (p.getMarkers().contains("scheduleType")) {
            ScheduleProcessJob.handleScheduleTypeUpdate(p);
            if (zoneDataInterface != null) {
                Log.i("PubNub","Zone Data Received Refresh");
                zoneDataInterface.refreshScreen(luid);
            }
        }
        if (p.getMarkers().contains("modbus")){
            if (modbusDataInterface != null) {
                modbusDataInterface.refreshScreen(luid);
            }
        }

        if (HSUtil.isBuildingTunerPoint(luid, CCUHsApi.getInstance())) {
            BuildingTunerUpdateHandler.updateZoneModuleSystemPoints(luid);
        }
    }

    public static void setZoneDataInterface(ZoneDataInterface in) { zoneDataInterface = in; }
    public static void setModbusDataInterface(ModbusDataInterface in) { modbusDataInterface = in; }
    public static void setSystemDataInterface(ZoneDataInterface in) { zoneDataInterface = in; }
    
    private static boolean canIgnorePointUpdate(String pbSource, String pointGuid, CCUHsApi hayStack) {
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
        if (pbSource.equals("ccu_"+ccuName) || pbSource.equals("Scheduler") || pbSource.equals("manual")) {
            CcuLog.d(L.TAG_CCU_PUBNUB, "PubNub received for CCU write : Ignore");
            return true;
        }
        
        //Point does not exist on this CCU.
        String luid = hayStack.getLUID("@" + pointGuid);
        if (luid == null) {
            return true;
        }
        
        return false;
    }
}
