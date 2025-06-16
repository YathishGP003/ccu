package a75f.io.device.mesh;

import org.projecthaystack.HDict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class DeviceHSUtil {
    
    public static Point getLogicalPointForRawPoint(RawPoint rawPoint, CCUHsApi hayStack) {
        HDict logPoint = hayStack.readHDictById(rawPoint.getPointRef());
        if (logPoint.isEmpty()) {
            CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for " + rawPoint.getDisplayName());
            return null;
        }
        return new Point.Builder()
                   .setHDict(logPoint)
                   .build();
    }
    
    public static List<RawPoint> getEnabledSensorPointsWithRefForDevice(HashMap device, CCUHsApi hayStack) {
        
        List<HDict> rawPoints = hayStack.readAllHDictByQuery("point and physical and sensor and deviceRef == \"" +
                                                        device.get("id") + "\"");
        return rawPoints.stream()
                        .filter( p -> p.get("pointRef") != null)
                        .map( p -> new RawPoint.Builder().setHDict(p).build())
                        .collect(Collectors.toList());
        
    }
    
    public static List<RawPoint> getEnabledCmdPointsWithRefForDevice(HashMap device, CCUHsApi hayStack) {
        List<HDict> rawPoints = hayStack.readAllHDictByQuery("point and physical and cmd and deviceRef == \"" +
                                                        device.get("id") + "\"");
        
        return rawPoints.stream()
                        .filter( p -> p.get("pointRef") != null || p.has(Tags.WRITABLE))
                        .filter(p -> p.get("portEnabled").toString().equals("true"))
                        .map(p -> new RawPoint.Builder().setHDict(p).build())
                        .collect(Collectors.toList());
        
    }
    
    public static double getTempOffset(int nodeAddr) {
        try {
            return CCUHsApi.getInstance().readDefaultVal(
                "point and zone and config and (temp or temperature) and offset and group == \"" + nodeAddr + "\"");
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_DEVICE," Temperature offset point does not exist for "+nodeAddr, e);
            return 0;
        }
    }
}
