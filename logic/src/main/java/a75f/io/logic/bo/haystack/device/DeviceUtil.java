package a75f.io.logic.bo.haystack.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.RawPoint;
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class DeviceUtil {

    public static void updatePhysicalPointType(int addr, String port, String type) {
        CcuLog.d(L.TAG_CCU," Update Physical point "+port+" "+type);

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
        CcuLog.d(L.TAG_CCU," Update Physical point "+port);

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

    public static void updatePhysicalPointUnit(int addr, String port, String unit) {
        CcuLog.d(L.TAG_CCU," Update Physical point "+port+" "+unit);

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }

        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (!point.get("unit" ).equals(unit))
        {
            RawPoint p = new RawPoint.Builder().setHashMap(point).build();
            p.setUnit(unit);
            CCUHsApi.getInstance().updatePoint(p,p.getId());
        }
    }

    public static void setPointEnabled(int addr, String port, boolean enabled) {

        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        if (device == null)
        {
            return ;
        }

        HashMap point = CCUHsApi.getInstance().read("point and physical and deviceRef == \"" + device.get("id").toString() + "\""+" and port == \""+port+"\"");
        if (point != null && !point.isEmpty())
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
        if (point != null && !point.isEmpty())
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
    /*Not version tag is used to ignore firmware version point in the collection*/
    public static List<RawPoint> getPortsForDevice(Short deviceAddress, CCUHsApi hayStack) {
        HashMap<Object, Object> device = hayStack.readEntity("device and addr == \""+deviceAddress+"\"");
        if(!device.isEmpty()) {
            ArrayList<HashMap<Object, Object>> rawPoints = hayStack.readAllEntities("point and physical" +
                    " and not version and deviceRef == \"" + device.get("id").toString() + "\"");

            return rawPoints.stream()
                    .map(p -> new RawPoint.Builder().setHashMap(p).build())
                    .collect(Collectors.toList());
        } else {
            return null;
        }

    }
    public static List<RawPoint> getUnusedPortsForDevice(Short deviceAddress, CCUHsApi hayStack) {
        List<RawPoint> rawPoints = getPortsForDevice(deviceAddress, hayStack);
        if (rawPoints != null && !rawPoints.isEmpty()) {
            return rawPoints.stream()
                    .filter(rawPoint -> rawPoint.getDomainName() != null
                            && !rawPoint.getMarkers().contains("sensor")
                            && !(rawPoint.getDomainName().equals(DomainName.analog1In)
                            || rawPoint.getDomainName().equals(DomainName.analog2In)
                            || rawPoint.getDomainName().equals(DomainName.th1In)
                            || rawPoint.getDomainName().equals(DomainName.th2In)) && !rawPoint.getEnabled())
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }
}
