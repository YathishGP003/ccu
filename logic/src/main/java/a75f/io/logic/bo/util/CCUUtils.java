package a75f.io.logic.bo.util;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
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

    public static Date getLastReceivedTime(String nodeAddr){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
        if (device != null && device.size() > 0) {
            Device deviceInfo = new Device.Builder().setHashMap(device).build();
            ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + deviceInfo.getId() + "\"");
            for(HashMap phyPoint : phyPoints) {
                if((Port.valueOf(phyPoint.get("port").toString()) == Port.RSSI) && (hayStack.curRead(phyPoint.get("id").toString())!= null)){
                    return hayStack.curRead(phyPoint.get("id").toString()).getDate();
                }
            }
        }
        return null;
    }
}
