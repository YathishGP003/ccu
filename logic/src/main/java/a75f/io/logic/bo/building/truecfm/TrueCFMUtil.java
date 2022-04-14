package a75f.io.logic.bo.building.truecfm;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.definitions.DamperShape;

public class TrueCFMUtil {
    
    private static double getFlowVelocity(CCUHsApi hayStack, String equipRef) {
        
        double kFactor = hayStack.readDefaultVal("cfm and kfactor and equipRef == \""+equipRef+"\"");
        double pressureInPascals = hayStack.readDefaultVal("pressure and sensor and equipRef == \""+equipRef+"\"");
        double pressureInWGUnit = pressureInPascals/248.84 ;
        return 4005 * Math.sqrt(pressureInWGUnit/kFactor);
    }
    
    private static double getDuctCrossSectionArea(CCUHsApi hayStack, String equipRef) {
        
        double damperSize = hayStack.readDefaultVal("damper and size and equipRef == \""+equipRef+"\"");
        int damperShapeVal = hayStack.readDefaultVal("damper and shape and equipRef == \""+equipRef+"\"").intValue();
        DamperShape damperShape = DamperShape.values()[damperShapeVal];
        if (damperShape == DamperShape.ROUND) {
            return 3.14 * damperSize * damperSize;
        } else {
            return damperSize * damperSize; //TODO - check if rectangular dimension available.
        }
    }
    
    public static double getCalculatedCfm(CCUHsApi hayStack, String equipRef) {
        double flowVelocity = getFlowVelocity(hayStack, equipRef);
        double ductArea = getDuctCrossSectionArea(hayStack, equipRef);
        return flowVelocity * ductArea;
    }
    
    public static double getMaxCFMCooling(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and max and cfm and cooling and equipRef == \""+equipId+"\"");
    }
    
    public static double getMinCFMCooling(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and min and cfm and cooling and equipRef == \""+equipId+"\"");
    }
    
    public static double getMaxCFMReheating(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and max and cfm and heating and equipRef == \""+equipId+"\"");
    }
    
    public static double getMinCFMReheating(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and min and cfm and heating and equipRef == \""+equipId+"\"");
    }
}
