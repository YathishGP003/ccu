package a75f.io.logic.bo.building.truecfm;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.DamperShape;

public class TrueCFMUtil {
    
    private static double getFlowVelocity(CCUHsApi hayStack, String equipRef) {
        
        double kFactor = hayStack.readDefaultVal("cfm and kfactor and equipRef == \""+equipRef+"\"");
        double pressureInPascals = hayStack.readHisValByQuery("pressure and sensor and equipRef == \""+equipRef+"\"");
        CcuLog.i(L.TAG_CCU_ZONE,"kFactor " + kFactor + " pressureInPascals " + pressureInPascals);
        double pressureInWGUnit = pressureInPascals/248.84 ;
        return 4005 * Math.sqrt(pressureInWGUnit/kFactor);
    }
    
    private static double getDuctCrossSectionArea(CCUHsApi hayStack, String equipRef) {
        
        double damperSize = hayStack.readDefaultVal("damper and size and equipRef == \""+equipRef+"\"");
        int damperShapeVal = hayStack.readDefaultVal("damper and shape and equipRef == \""+equipRef+"\"").intValue();
        DamperShape damperShape = DamperShape.values()[damperShapeVal];
        double damperSizeInFeet = damperSize/12;
        if (damperShape == DamperShape.ROUND) {
            return 3.14 * damperSizeInFeet/2 * damperSizeInFeet/2;
        } else {
            return damperSizeInFeet * damperSizeInFeet; //TODO - check if rectangular dimension available.
        }
    }
    
    public static boolean isTrueCfmEnabled(CCUHsApi hayStack, String equipRef) {
        return hayStack.readDefaultVal("config and cfm and enabled and equipRef ==\""
                                                                +equipRef+"\"").intValue() > 0;
    }
    
    public static double getCalculatedCfm(CCUHsApi hayStack, String equipRef) {
        double flowVelocity = getFlowVelocity(hayStack, equipRef);
        double ductArea = getDuctCrossSectionArea(hayStack, equipRef);
        CcuLog.i(L.TAG_CCU_ZONE,"flowVelocity " + flowVelocity + " ductArea " + ductArea);
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
