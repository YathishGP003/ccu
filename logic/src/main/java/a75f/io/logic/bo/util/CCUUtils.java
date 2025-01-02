package a75f.io.logic.bo.util;

import android.content.Context;

import org.joda.time.DateTime;
import org.projecthaystack.HDict;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Zone;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.R;
import a75f.io.logic.schedule.SpecialSchedule;
import a75f.io.logic.util.PreferenceUtil;

/**
 * Created by Yinten on 10/11/2017.
 */

public class CCUUtils
{
    private static String TAG_CCU_REF = "CCU_REF";

    public static double roundToOneDecimal(double number) {
        DecimalFormat df = new DecimalFormat("#.#");
        return Double.parseDouble(df.format(number));
    }
    public static double roundToTwoDecimal(double number) {
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.parseDouble(df.format(number));
    }

    public static Date getLastReceivedTimeForRssi(String nodeAddr){
        if(isDomainEquip(nodeAddr, "node")){
            HashMap<Object, Object> point = CCUHsApi.getInstance()
                    .readEntity("domainName == \"" + DomainName.heartBeat + "\" and group == \"" + nodeAddr + "\"");
            HisItem hisItem = CCUHsApi.getInstance().curRead(point.get("id").toString());
            return (hisItem == null) ? null : hisItem.getDate();
        }
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap point = CCUHsApi.getInstance().read("point and (heartBeat or heartbeat) and group == \""+nodeAddr+"\"");
        if(point.size() == 0){
            return null;
        }
        HisItem hisItem = hayStack.curRead(point.get("id").toString());
        return (hisItem == null) ? null : hisItem.getDate();
    }

    public static Date getLastReceivedTimeForModBus(String slaveId){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        List<HashMap<Object, Object>> equipList =
                hayStack.readAllEntities("equip and modbus and group == \"" + slaveId + "\"");
        if(equipList.size() == 0){
            return null;
        }
        for(HashMap<Object, Object> equip : equipList) {
            if(isModbusHeartbeatRequired(equip, hayStack)) {
                HashMap<Object, Object> heartBeatPoint =
                        hayStack.readEntity("point and (heartbeat or heartBeat) and equipRef == \"" + equip.get("id") + "\"");
                if(heartBeatPoint.size() > 0) {
                    HisItem heartBeatHisItem = hayStack.curRead(heartBeatPoint.get("id").toString());
                    return (heartBeatHisItem == null) ? null : heartBeatHisItem.getDate();
                }
            }
        }
        return null;
    }

    public static Date getLastReceivedTimeForCloudConnectivity(){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Map<Object, Object> cloudConnectivityPoint = hayStack.readEntityByDomainName(DomainName.ccuHeartbeat);
        if(cloudConnectivityPoint.isEmpty()) {
            return null;
        }
        HisItem heartBeatHisItem = hayStack.curRead(cloudConnectivityPoint.get("id").toString());
        return (heartBeatHisItem == null) ? null : heartBeatHisItem.getDate();
    }

    public static void writeFirmwareVersion(String firmwareVersion, short address, boolean isCMReboot){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> device;
        if(isCMReboot){
            device = CCUHsApi.getInstance().readEntity("device and cm");
            writeFirmwareVersionForTiDevices(hayStack, firmwareVersion);
        }
        else {
            device = hayStack.readEntity("device and addr == \"" + address + "\"");
        }
        if (!device.isEmpty()) {
            Device deviceInfo = new Device.Builder().setHashMap(device).build();
            HashMap<Object, Object> firmwarePoint =
                    hayStack.readEntity("point and physical and firmware and version and deviceRef == \"" + deviceInfo.getId() + "\"");
            hayStack.writeDefaultValById(firmwarePoint.get("id").toString(), firmwareVersion);
        }
    }

    private static void writeFirmwareVersionForTiDevices(CCUHsApi ccuHsApi, String firmwareVersion) {
        ArrayList<HashMap<Object, Object>> tiDevices = ccuHsApi.readAllEntities("device and ti");
        for (HashMap<Object, Object> tiDevice : tiDevices) {
            Device deviceInfo = new Device.Builder().setHashMap(tiDevice).build();
            HashMap<Object, Object> firmwarePoint =
                    ccuHsApi.readEntity("point and physical and firmware and version and deviceRef == \"" + deviceInfo.getId() + "\"");
            ccuHsApi.writeDefaultValById(firmwarePoint.get("id").toString(), firmwareVersion);
        }
    }

    public static boolean isDaikinEnvironment(Context context){
        return BuildConfig.BUILD_TYPE.equalsIgnoreCase(context.getString(R.string.Daikin_Environment));
    }

    public static String getSupportMsgContent(Context context){
        if(isDaikinEnvironment(context))
            return "please contact SiteLine\u2122 Customer Support.";
        else if (isCarrierEnvironment(context))
            return "please contact ClimaVision Customer Support.";
        else if (isAiroverseEnvironment(context))
            return "please contact Airoverse for Facilities Customer Support.";
        else
            return "please contact 75F Customer Support.";
    }

    public static void updateCcuSpecificEntitiesWithCcuRef(CCUHsApi ccuHsApi, Boolean isCcuReregistration){
        if(CCUHsApi.getInstance().readEntity("ccu").size() == 0){
            return;
        }
        CountDownLatch latch = new CountDownLatch(8);
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        String ccuId = CCUHsApi.getInstance().readEntity("ccu").get("id").toString();

        executeTask(executorService, latch, () -> updateZoneWithUpdatedCcuRef(ccuHsApi, isCcuReregistration));
        executeTask(executorService, latch, () -> updateZoneOccupancyPointWithUpdatedCcuRef(ccuHsApi, isCcuReregistration));
        executeTask(executorService, latch, () -> updateNonTunerEquipAndPointsWithUpdatedCcuRef(ccuHsApi, isCcuReregistration));
        executeTask(executorService, latch, () -> updateDeviceAndPointsWithUpdatedCcuRef(ccuHsApi, isCcuReregistration));
        executeTask(executorService, latch, () -> updateSettingPointsWithUpdatedCcuRef(ccuHsApi, isCcuReregistration, ccuId));
        executeTask(executorService, latch, () -> updateZoneSchedulesWithUpdatedCcuRef(ccuHsApi, isCcuReregistration));
        executeTask(executorService, latch, () -> updateSpecialSchedulesWithUpdatedCcuRef(ccuHsApi));
        executeTask(executorService, latch, () -> updateZoneSchedulablePointsWithUpdatedCcuRef(ccuHsApi, isCcuReregistration, ccuId));

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static boolean isCarrierEnvironment(Context context){
        return BuildConfig.BUILD_TYPE.equalsIgnoreCase(context.getString(R.string.Carrier_Environment));
    }

    public static boolean isAiroverseEnvironment(Context context){
        return BuildConfig.BUILD_TYPE.equalsIgnoreCase("airoverse_prod");
    }

    public static boolean isRecommendedVersionCheckIsNotFalse() {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method method = systemPropertiesClass.getMethod("get", String.class);
            return !method.invoke(null, "recommended_version_check").toString().equalsIgnoreCase("false");
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
    public static boolean isCCUOfflinePropertySet() {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method method = systemPropertiesClass.getMethod("get", String.class);
            return method.invoke(null, "ccu_offline_check").toString().equalsIgnoreCase("false");
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
    public static void setCCUReadyProperty(String propertyStatus) {
        try {
            Class<?> systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method setMethod = systemPropertiesClass.getMethod("set", String.class, String.class);
            setMethod.invoke(null, "ccu_ready_property", propertyStatus);
            CcuLog.i("CCU_PROPERTY","setCCUReadyProperty"+propertyStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static boolean isModbusHeartbeatRequired(HashMap<Object, Object> equip, CCUHsApi hayStack) {
        if(equip.containsKey("equipRef")) {
            HashMap<Object, Object> parentEquip = hayStack.readMapById(equip.get("equipRef").toString());
            return !equip.get("group").toString().equals(parentEquip.get("group"));
        }
        return true;
    }

    public static void executeTask(ExecutorService executorService, CountDownLatch latch, Runnable task) {
        executorService.submit(() -> {
            task.run();
            latch.countDown();
        });
    }

    public static void updateZoneWithUpdatedCcuRef(CCUHsApi ccuHsApi, boolean isCcuReregistration) {
        CcuLog.d(TAG_CCU_REF,"Executing updateZoneWithUpdatedCcuRef");
        ArrayList<HashMap<Object, Object>> zoneList = ccuHsApi.readAllEntities("room");
        for(HashMap<Object, Object> zoneMap : zoneList){
            Zone zone =  new Zone.Builder().setHashMap(zoneMap).build();
            if(zone != null && (isCcuReregistration || zone.getCcuRef() == null)) {
                ccuHsApi.updateZone(zone, zone.getId());
            }
        }
        CcuLog.d(TAG_CCU_REF,"Executed updateZoneWithUpdatedCcuRef");
    }

    public static void updateZoneOccupancyPointWithUpdatedCcuRef(CCUHsApi ccuHsApi, boolean isCcuReregistration) {
        CcuLog.d(TAG_CCU_REF,"Executing updateZoneOccupancyPointWithUpdatedCcuRef");
        ArrayList<HashMap<Object, Object>> zoneOccupancyPointList = ccuHsApi.readAllEntities("zone and occupancy and " +
                "state");

        for(HashMap<Object, Object> zoneOccupancyPoint : zoneOccupancyPointList){
            Point point = new Point.Builder().setHashMap(zoneOccupancyPoint).build();
            if(point != null && (isCcuReregistration || point.getCcuRef() == null)) {
                ccuHsApi.updatePoint(point, point.getId());
            }
        }
        CcuLog.d(TAG_CCU_REF,"Executed updateZoneOccupancyPointWithUpdatedCcuRef");
    }

    public static void updateNonTunerEquipAndPointsWithUpdatedCcuRef(CCUHsApi ccuHsApi, boolean isCcuReregistration) {
        CcuLog.d(TAG_CCU_REF,"Executing updateNonTunerEquipAndPointsWithUpdatedCcuRef");
        ArrayList<HashMap<Object, Object>> entityList = ccuHsApi.readAllEntities("equip and not tuner");
        for(HashMap<Object, Object> entityMap : entityList){
            Equip equip = new Equip.Builder().setHashMap(entityMap).build();
            ccuHsApi.updateEquip(equip, equip.getId());
            ArrayList<HashMap<Object, Object>> equipPoints = ccuHsApi.readAllEntities("point and equipRef == \"" + equip.getId()+"\"");
            for(HashMap<Object, Object> equipPoint : equipPoints){
                if(equipPoint.get("id") == null) continue;
                HDict pointDict = CCUHsApi.getInstance().readHDictById(equipPoint.get("id").toString());
                Point point = new Point.Builder().setHDict(pointDict).build();
                if(point != null && (isCcuReregistration || point.getCcuRef() == null)) {
                    ccuHsApi.updatePoint(point, point.getId());
                }
            }
        }
        CcuLog.d(TAG_CCU_REF,"Executed updateNonTunerEquipAndPointsWithUpdatedCcuRef");
    }

    public static void updateDeviceAndPointsWithUpdatedCcuRef(CCUHsApi ccuHsApi, boolean isCcuReregistration) {
        CcuLog.d(TAG_CCU_REF,"Executing updateDeviceAndPointsWithUpdatedCcuRef");
        List<HDict> deviceDictList = ccuHsApi.readAllHDictByQuery("device and not ccu");
        for (HDict deviceDict : deviceDictList) {
            Device device = new Device.Builder().setHDict(deviceDict).build();
            if(device != null && (isCcuReregistration || device.getCcuRef() == null)) {
                ccuHsApi.updateDevice(device, device.getId());
            }
            ArrayList<HashMap<Object, Object>> devicePoints =
                    ccuHsApi.readAllEntities("point and deviceRef == \"" + device.getId()+"\"");
            for(HashMap<Object, Object> devicePoint : devicePoints){
                RawPoint point = new RawPoint.Builder().setHashMap(devicePoint).build();
                if(point != null && (isCcuReregistration || point.getCcuRef() == null)) {
                    ccuHsApi.updatePoint(point, point.getId());
                }
            }
        }
        String ccuId = ccuHsApi.getCcuId();
        CcuLog.d(TAG_CCU_REF,"Executed updateDeviceAndPointsWithUpdatedCcuRef");
    }

    public static void updateSettingPointsWithUpdatedCcuRef(CCUHsApi ccuHsApi, boolean isCcuReregistration, String ccuId) {
        CcuLog.d(TAG_CCU_REF,"Executing updateSettingPointsWithUpdatedCcuRef");
        ArrayList<HashMap<Object, Object>> settingPoints = ccuHsApi.readAllEntities("point and deviceRef == \"" + ccuId +
                "\"");
        for(HashMap<Object, Object> settingPoint : settingPoints){
            SettingPoint point = new SettingPoint.Builder().setHashMap(settingPoint).build();
            if(point != null && (isCcuReregistration || point.getCcuRef() == null)) {
                ccuHsApi.updateSettingPoint(point, point.getId());
            }
        }
        CcuLog.d(TAG_CCU_REF,"Executed updateSettingPointsWithUpdatedCcuRef");
    }

    public static void updateZoneSchedulesWithUpdatedCcuRef(CCUHsApi ccuHsApi, boolean isCcuReregistration) {
        CcuLog.d(TAG_CCU_REF,"Executing updateZoneSchedulesWithUpdatedCcuRef");
        ArrayList<HashMap<Object, Object>> zoneScheduleList = CCUHsApi.getInstance().readAllEntities("zone and not " +
                "special and schedule");
        for(HashMap<Object, Object> zoneSchedule : zoneScheduleList){
            zoneSchedule.put("ccuRef", ccuHsApi.getCcuId());
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(zoneSchedule.get("id").toString());
            if(schedule != null && (isCcuReregistration || schedule.getCcuRef() == null)) {
                CCUHsApi.getInstance().updateZoneSchedule(schedule, zoneSchedule.get("roomRef").toString());
            }
        }
        CcuLog.d(TAG_CCU_REF,"Executed updateZoneSchedulesWithUpdatedCcuRef");
    }

    public static void updateSpecialSchedulesWithUpdatedCcuRef(CCUHsApi ccuHsApi) {
        CcuLog.d(TAG_CCU_REF,"Executing updateSpecialSchedulesWithUpdatedCcuRef");
        try{

            ArrayList<HashMap<Object, Object>> zoneSpecialScheduleList = CCUHsApi.getInstance().readAllEntities("zone " +
                    "and special and schedule");
            for(HashMap<Object, Object> zoneSpecialSchedule : zoneSpecialScheduleList){
                zoneSpecialSchedule.put("ccuRef", ccuHsApi.getCcuId());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                HDict range = (HDict) zoneSpecialSchedule.get("range");

                String beginDateTimeString = range.get("stdt").toString() + " " + (int)Double.parseDouble(range.get("sthh").toString())
                        + ":" + (int)Double.parseDouble(range.get("stmm").toString());
                DateTime beginDateTime = new DateTime(sdf.parse(beginDateTimeString));

                String endDateTimeString = range.get("etdt").toString()+ " " + (int)Double.parseDouble(range.get("ethh").toString())
                        + ":" + (int)Double.parseDouble(range.get("etmm").toString());
                DateTime endDateTime = new DateTime(sdf.parse(endDateTimeString));


                SpecialSchedule.createSpecialSchedule(zoneSpecialSchedule.get("id").toString(),
                        zoneSpecialSchedule.get("dis").toString(), beginDateTime, endDateTime,
                        Double.parseDouble(range.get("coolVal").toString()),
                        Double.parseDouble(range.get("heatVal").toString()),
                        Double.parseDouble(range.get("coolingUserLimitMax" ).toString()),
                        Double.parseDouble(range.get("coolingUserLimitMin" ).toString()),
                        Double.parseDouble(range.get("heatingUserLimitMax" ).toString()),
                        Double.parseDouble(range.get("heatingUserLimitMin" ).toString()),
                        Double.parseDouble(range.get("coolingDeadband" ).toString()),
                        Double.parseDouble(range.get("heatingDeadband" ).toString()),
                        true,
                        zoneSpecialSchedule.get("roomRef").toString());


            }
        }
        catch(ParseException exception){
            CcuLog.e(TAG_CCU_REF, "Error while parsing special schedule");
            exception.printStackTrace();
            PreferenceUtil.setCcuRefTagMigration(false);
        }
        CcuLog.d(TAG_CCU_REF,"Executed updateSpecialSchedulesWithUpdatedCcuRef");
    }

    private static void updateZoneSchedulablePointsWithUpdatedCcuRef(CCUHsApi ccuHsApi, boolean isCcuReregistration, String ccuId) {
        CcuLog.d(TAG_CCU_REF,"Executing updateZoneSchedulablePointsWithUpdatedCcuRef");
        ArrayList<HashMap<Object, Object>> zoneSchedulePointList = CCUHsApi.getInstance().readAllEntities("point and zone and (schedulable or hvacMode) and not tuner and ccuRef and ccuRef!=\""+ccuId+"\"");
        for(HashMap<Object, Object> zoneSchedulePoint: zoneSchedulePointList) {
            Point point = new Point.Builder().setHashMap(zoneSchedulePoint).build();
            if(point != null && (isCcuReregistration || point.getCcuRef() == null)) {
                ccuHsApi.updatePoint(point, point.getId());
            }
        }
        CcuLog.d(TAG_CCU_REF,"Executed updateZoneSchedulablePointsWithUpdatedCcuRef");
    }

    public static boolean isDomainEquip(String val, String filter) {
        if (filter.equals("node")) {
            return CCUHsApi.getInstance().readEntity("equip and group == \"" + val + "\"").containsKey("domainName");
        } else {
            return CCUHsApi.getInstance().readMapById(val).containsKey("domainName");
        }
    }
}
