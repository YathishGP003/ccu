package a75f.io.logic.bo.building.truecfm;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.DamperShape;
import a75f.io.logic.bo.building.schedules.Occupancy;

public class TrueCFMUtil {
    
    private static double getFlowVelocity(CCUHsApi hayStack, String equipRef) {
        
        double kFactor = hayStack.readDefaultVal("trueCfm and kfactor and equipRef == \""+equipRef+"\"");
        double pressureInPascals = hayStack.readHisValByQuery("pressure and sensor and equipRef == \""+equipRef+"\"");
        CcuLog.i(L.TAG_CCU_ZONE,"kFactor " + kFactor + " pressureInPascals " + pressureInPascals);
        double pressureInWGUnit = Math.abs(pressureInPascals)/248.84 ;
        return 4005 * Math.sqrt(pressureInWGUnit/kFactor);
    }
    
    private static double getDuctCrossSectionArea(CCUHsApi hayStack, String equipRef, String damperOrder) {
        String damperOrderQuery = damperOrder.isEmpty() ? "" : damperOrder+" and ";
        
        double damperSize = hayStack.readDefaultVal(damperOrderQuery+"damper and size and equipRef == \""+equipRef+"\"");
        int damperShapeVal = hayStack.readDefaultVal(damperOrderQuery+"damper and shape and equipRef == \""+equipRef+
                                                     "\"").intValue();
        DamperShape damperShape = DamperShape.values()[damperShapeVal];
        double damperSizeInFeet = damperSize/12;
        if (damperShape == DamperShape.ROUND) {
            return 3.14 * damperSizeInFeet/2 * damperSizeInFeet/2;
        } else {
            return damperSizeInFeet * damperSizeInFeet; //TODO - check if rectangular dimension available.
        }
    }
    
    public static boolean isTrueCfmEnabled(CCUHsApi hayStack, String equipRef) {
        return hayStack.readDefaultVal("config and trueCfm and enable and equipRef ==\""
                                                                +equipRef+"\"").intValue() > 0;
    }
    
    public static double calculateAndUpdateCfm(CCUHsApi hayStack, String equipRef, String damperOrder) {
        double flowVelocity = getFlowVelocity(hayStack, equipRef);
        double ductArea = getDuctCrossSectionArea(hayStack, equipRef, damperOrder);
        double airflowCfm = flowVelocity * ductArea;
        hayStack.writeHisValByQuery("air and velocity and equipRef == \""+equipRef+"\"", flowVelocity);
        hayStack.writeHisValByQuery("air and flow and equipRef == \""+equipRef+"\"", airflowCfm);
        CcuLog.i(L.TAG_CCU_ZONE,"flowVelocity " + flowVelocity + " ductArea "
                                            + ductArea+" airflowCfm "+airflowCfm);
        return airflowCfm;
    }
    
    public static double getMaxCFMCooling(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and max and trueCfm and cooling and equipRef == \""+equipId+"\"");
    }
    
    public static double getMinCFMCooling(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and min and trueCfm and cooling and equipRef == \""+equipId+"\"");
    }
    
    public static double getMaxCFMReheating(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and max and trueCfm and heating and equipRef == \""+equipId+"\"");
    }
    
    public static double getMinCFMReheating(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and min and trueCfm and heating and equipRef == \""+equipId+"\"");
    }
    
    public static boolean cfmControlNotRequired(CCUHsApi hayStack, String equipRef) {
    
        int occupancyMode = hayStack.readHisValByQuery("point and occupancy and mode and equipRef ==\""+equipRef+"\"")
                                                    .intValue();
        
        return !TrueCFMUtil.isTrueCfmEnabled(hayStack, equipRef)
                                    || (occupancyMode != Occupancy.OCCUPIED.ordinal()
                                        && occupancyMode != Occupancy.FORCEDOCCUPIED.ordinal()
                                        && occupancyMode != Occupancy.AUTOFORCEOCCUPIED.ordinal());
    }
}
