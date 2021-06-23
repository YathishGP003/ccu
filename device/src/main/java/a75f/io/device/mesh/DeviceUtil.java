package a75f.io.device.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class DeviceUtil {
    
    public static Point getLogicalPointForRawPoint(RawPoint rawPoint, CCUHsApi hayStack) {
        HashMap logPoint = hayStack.readMapById(rawPoint.getPointRef());
        if (logPoint.isEmpty()) {
            CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for " + rawPoint.getDisplayName());
            return null;
        }
        return new Point.Builder()
                   .setHashMap(logPoint)
                   .build();
    }
    
    public static List<RawPoint> getRawPointsWithRefForDevice(HashMap device, CCUHsApi hayStack) {
        ArrayList<HashMap> rawPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" +
                                                        device.get("id") + "\"");
        
        return rawPoints.stream()
                        .map( p -> new RawPoint.Builder().setHashMap(p).build())
                        .filter( p -> !p.getPointRef().isEmpty())
                        .collect(Collectors.toList());
        
    }
    
    public static List<RawPoint> getEnabledRawPointsWithRefForDevice(HashMap device, CCUHsApi hayStack) {
        ArrayList<HashMap> rawPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" +
                                                        device.get("id") + "\"");
        
        return rawPoints.stream()
                        .map( p -> new RawPoint.Builder().setHashMap(p).build())
                        .filter( p -> !p.getPointRef().isEmpty())
                        .collect(Collectors.toList());
        
    }
}
