package a75f.io.logic.bo.util;

import android.content.Context;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.HisItem;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.R;

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
        HashMap equip = hayStack.read("equip and modbus and group == \"" + slaveId + "\"");
        if(equip.size() == 0){
            return null;
        }
        HashMap heartBeatPoint = hayStack.read("point and heartbeat and equipRef == \""+equip.get("id")+ "\"");
        if(heartBeatPoint.size() == 0){
            return null;
        }
        HisItem heartBeatHisItem = hayStack.curRead(heartBeatPoint.get("id").toString());
        return (heartBeatHisItem == null) ? null : heartBeatHisItem.getDate();
    }

    public static Date getLastReceivedTimeForCloudConnectivity(){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Map<Object, Object> cloudConnectivityPoint = hayStack.readEntity("cloud and connectivity and diag and point");
        if(cloudConnectivityPoint.isEmpty()){
            return null;
        }
        HisItem heartBeatHisItem = hayStack.curRead(cloudConnectivityPoint.get("id").toString());
        return (heartBeatHisItem == null) ? null : heartBeatHisItem.getDate();
    }

    public static void writeFirmwareVersion(String firmwareVersion, short address, boolean isCMReboot){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device;
        if(isCMReboot){
            device = CCUHsApi.getInstance().read("device and cm");
        }
        else {
            device = hayStack.read("device and addr == \"" + address + "\"");
        }
        if (!device.isEmpty()) {
            Device deviceInfo = new Device.Builder().setHashMap(device).build();
            HashMap firmwarePoint =
                    hayStack.read("point and physical and firmware and version and deviceRef == \"" + deviceInfo.getId() + "\"");
            
            if (!hayStack.readDefaultStrValById(firmwarePoint.get("id").toString()).equals(firmwareVersion)) {
                hayStack.writeDefaultValById(firmwarePoint.get("id").toString(), firmwareVersion);
            }
        }

    }

    public static boolean isDaikinEnvironment(Context context){
        return BuildConfig.BUILD_TYPE.equalsIgnoreCase(context.getString(R.string.Daikin_Environment));
    }

    public static String getSupportMsgContent(Context context){
        if(isDaikinEnvironment(context))
            return "please contact SiteLine\u2122 Customer Support.";
        else
            return "please contact 75F Customer Support.";
    }
}
