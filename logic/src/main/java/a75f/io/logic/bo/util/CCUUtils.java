package a75f.io.logic.bo.util;

import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.logic.bo.building.definitions.Port;

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
        HisItem hisItem = hayStack.curRead(point.get("id").toString());
        return (hisItem == null) ? null : hisItem.getDate();
    }

    public static Date getLastReceivedTimeForModBus(String slaveId){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and modbus and group == \"" + slaveId + "\"");
        HashMap heartBeatPoint = hayStack.read("point and heartbeat and equipRef == \""+equip.get("id")+ "\"");
        if(heartBeatPoint.size() == 0){
            return null;
        }
        HisItem heartBeatHisItem = hayStack.curRead(heartBeatPoint.get("id").toString());
        return (heartBeatHisItem == null) ? null : heartBeatHisItem.getDate();
    }
}
