package a75f.io.logic.bo.util;

import android.content.Context;
import android.util.Log;

import org.joda.time.DateTime;
import org.projecthaystack.HDict;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.R;
import a75f.io.logic.schedule.SpecialSchedule;
import a75f.io.logic.util.PreferenceUtil;

/**
 * Created by Yinten on 10/11/2017.
 */

public class CCUUtils
{
    public static double roundToOneDecimal(double number) {
        DecimalFormat df = new DecimalFormat("#.#");
        return Double.parseDouble(df.format(number));
    }
    public static double roundToTwoDecimal(double number) {
        DecimalFormat df = new DecimalFormat("#.##");
        return Double.parseDouble(df.format(number));
    }

    public static Date getLastReceivedTimeForRssi(String nodeAddr){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap point = CCUHsApi.getInstance().read("point and heartbeat and group == \""+nodeAddr+"\"");
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
        for( HashMap<Object, Object> equip : equipList){
            HashMap<Object, Object> heartBeatPoint =
                    hayStack.readEntity("point and heartbeat and equipRef == \""+equip.get("id")+ "\"");
            if(heartBeatPoint.size() > 0){
                HisItem heartBeatHisItem = hayStack.curRead(heartBeatPoint.get("id").toString());
                return (heartBeatHisItem == null) ? null : heartBeatHisItem.getDate();
            }
        }
        return null;
    }

    public static Date getLastReceivedTimeForCloudConnectivity(){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Map<Object, Object> cloudConnectivityPoint = hayStack.readEntity("cloud and connected and diag and point");
        if(cloudConnectivityPoint.isEmpty()){
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
        else if (isCarrierEnvironment(context)) {
            return "please contact ClimaVision Customer Support.";
        } else
            return "please contact 75F Customer Support.";
    }

    public static void updateCcuSpecificEntitiesWithCcuRef(CCUHsApi ccuHsApi){
        if(CCUHsApi.getInstance().readEntity("ccu").size() == 0){
            return;
        }
        ArrayList<HashMap<Object, Object>> zoneList = ccuHsApi.readAllEntities("room");
        for(HashMap<Object, Object> zoneMap : zoneList){
            Zone zone =  new Zone.Builder().setHashMap(zoneMap).build();
            ccuHsApi.updateZone(zone, zone.getId());
        }
        ArrayList<HashMap<Object, Object>> zoneOccupancyPointList = ccuHsApi.readAllEntities("zone and occupancy and " +
                "state");

        for(HashMap<Object, Object> zoneOccupancyPoint : zoneOccupancyPointList){
            Point point = new Point.Builder().setHashMap(zoneOccupancyPoint).build();
            ccuHsApi.updatePoint(point, point.getId());
        }

        ArrayList<HashMap<Object, Object>> entityList = ccuHsApi.readAllEntities("equip and not tuner");
        for(HashMap<Object, Object> entityMap : entityList){
            Equip equip = new Equip.Builder().setHashMap(entityMap).build();
            ccuHsApi.updateEquip(equip, equip.getId());
            ArrayList<HashMap<Object, Object>> equipPoints = ccuHsApi.readAllEntities("point and equipRef == \"" + equip.getId()+"\"");
            for(HashMap<Object, Object> equipPoint : equipPoints){
                Point point = new Point.Builder().setHashMap(equipPoint).build();
                ccuHsApi.updatePoint(point, point.getId());
            }
        }

        ArrayList<HashMap<Object, Object>> deviceList = ccuHsApi.readAllEntities("device and not ccu");
        for(HashMap<Object, Object> deviceMap : deviceList){
            Device device = new Device.Builder().setHashMap(deviceMap).build();
            ccuHsApi.updateDevice(device, device.getId());
            ArrayList<HashMap<Object, Object>> devicePoints =
                    ccuHsApi.readAllEntities("point and deviceRef == \"" + device.getId()+"\"");
            for(HashMap<Object, Object> devicePoint : devicePoints){
                RawPoint point = new RawPoint.Builder().setHashMap(devicePoint).build();
                ccuHsApi.updatePoint(point, point.getId());
            }
        }
        String ccuId = CCUHsApi.getInstance().readEntity("ccu").get("id").toString();
        ArrayList<HashMap<Object, Object>> settingPoints = ccuHsApi.readAllEntities("point and deviceRef == \"" + ccuId +
                "\"");
        for(HashMap<Object, Object> settingPoint : settingPoints){
            SettingPoint point = new SettingPoint.Builder().setHashMap(settingPoint).build();
            ccuHsApi.updateSettingPoint(point, point.getId());
        }

        ArrayList<HashMap<Object, Object>> zoneScheduleList = CCUHsApi.getInstance().readAllEntities("zone and not " +
                "special and schedule");
        for(HashMap<Object, Object> zoneSchedule : zoneScheduleList){
            zoneSchedule.put("ccuRef", ccuHsApi.getCcuId());
            Schedule schedule = CCUHsApi.getInstance().getScheduleById(zoneSchedule.get("id").toString());
            CCUHsApi.getInstance().updateZoneSchedule(schedule, zoneSchedule.get("roomRef").toString());
        }
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
            Log.i("Adding CCURef tag", "Error while parsing special schedule");
            exception.printStackTrace();
            PreferenceUtil.setCcuRefTagMigration(false);
        }

    }

    public static boolean isCarrierEnvironment(Context context){
        return BuildConfig.BUILD_TYPE.equalsIgnoreCase(context.getString(R.string.Carrier_Environment));
    }
}
