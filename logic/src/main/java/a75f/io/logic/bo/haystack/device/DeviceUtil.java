package a75f.io.logic.bo.haystack.device;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.RawPoint;
import a75f.io.logic.Globals;

public class DeviceUtil {
    public static void updatePhysicalPointType(int addr, String port, String type) {
        Log.d("CCU"," Update Physical point "+port+" "+type);

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }

        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (!point.get("analogType" ).equals(type))
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setType(type);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
        }
    }

    public static void updatePhysicalPointRef(int addr, String port, String pointRef) {
        Log.d("CCU"," Update Physical point "+port);

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }

        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        RawPoint p = new RawPoint.Builder().setHashMap(point).build();
        p.setPointRef(pointRef);
        CCUHsApi.getInstance().updatePoint(p,p.getId());

    }

    public static void setPointEnabled(int addr, String port, boolean enabled) {
        Log.d("CCU"," Enabled Physical point "+port+" "+enabled);

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }

        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (point != null && point.size() > 0)
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setEnabled(enabled);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
            CCUHsApi.getInstance().writeHisValById(p.getId(), 0.0);
        }
    }

    public static RawPoint getPhysicalPoint(int addr, String port) {

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return null;
        }

        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (point != null && point.size() > 0)
        {
            return new RawPoint.Builder().setHashMap(point).build();
        }
        return null;
    }

    public static List<RawPoint> getEnabledCmdPointsWithRefForDevice(HashMap device, CCUHsApi hayStack) {
        ArrayList<HashMap> rawPoints = hayStack.readAll("point and physical and cmd and deviceRef == \"" +
                device.get("id") + "\"");

        return rawPoints.stream()
                .filter( p -> p.get("pointRef") != null)
                .filter(p -> p.get("portEnabled").toString().equals("true"))
                .map(p -> new RawPoint.Builder().setHashMap(p).build())
                .collect(Collectors.toList());

    }

}
