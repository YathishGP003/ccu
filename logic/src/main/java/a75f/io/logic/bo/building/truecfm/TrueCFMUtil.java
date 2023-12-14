package a75f.io.logic.bo.building.truecfm;

import java.util.Arrays;
import java.util.List;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.VavEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.DamperShape;
import a75f.io.logic.bo.building.schedules.Occupancy;

public class TrueCFMUtil {
    
    private static double getFlowVelocity(CCUHsApi hayStack, String equipRef) {
        /*
            Prior to version ___, the Haystack logical point for pressure was given in Pa, but incorrectly
            displayed units in inH2O.

            Starting with version ___, Haystack logical points were converted to read accurately in inH20.
            So, the unit conversion has been removed here, since it is already done when writing to the logical point.
         */
        double kFactor = hayStack.readDefaultVal("(trueCfm or trueCFM) and (kfactor or KFactor) and equipRef == \""+equipRef+"\"");
        double pressureInWc = hayStack.readHisValByQuery("pressure and sensor and equipRef == \""+equipRef+"\"");
        CcuLog.i(L.TAG_CCU_ZONE,"kFactor " + kFactor + " pressureInWc " + pressureInWc);
        double pressureInWGUnit = Math.abs(pressureInWc);
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
        return hayStack.readDefaultVal("config and (trueCfm or trueCFM) and enable and equipRef ==\""
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

    private static double getFlowVelocityVav(CCUHsApi hayStack, String equipRef, VavEquip equip) {
        /*
            Prior to version ___, the Haystack logical point for pressure was given in Pa, but incorrectly
            displayed units in inH2O.

            Starting with version ___, Haystack logical points were converted to read accurately in inH20.
            So, the unit conversion has been removed here, since it is already done when writing to the logical point.
         */
        double kFactor = equip.getKFactor().readDefaultVal();
        double pressureInWc = equip.getPressureSensor().readHisVal();
        CcuLog.i(L.TAG_CCU_ZONE,"kFactor " + kFactor + " pressureInWc " + pressureInWc);
        double pressureInWGUnit = Math.abs(pressureInWc);
        return 4005 * Math.sqrt(pressureInWGUnit/kFactor);
    }

    private static double getDuctCrossSectionAreaVav(CCUHsApi hayStack, String equipRef, VavEquip equip) {
        double damperSize = equip.getDamperSize().readDefaultVal();
        int damperShapeVal = (int)equip.getDamperShape().readDefaultVal();
        DamperShape damperShape = DamperShape.values()[damperShapeVal];
        double damperSizeInFeet = getDamperSizeFromIndex((int)damperSize)/12;
        if (damperShape == DamperShape.ROUND) {
            return 3.14 * damperSizeInFeet/2 * damperSizeInFeet/2;
        } else {
            return damperSizeInFeet * damperSizeInFeet; //TODO - check if rectangular dimension available.
        }
    }
    private static double getDamperSizeFromIndex(int index) {
        List<Double> damperSizes = Arrays.asList(4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0, 18.0, 20.0, 22.0, 24.0);
        try {
            return damperSizes.get(index);
        } catch(Exception e) {
            CcuLog.e(L.TAG_CCU_ZONE, "Failed to fetch Damper Size from Haystack enum");
            return 4.0;
        }
    }
    public static double calculateAndUpdateCfmVav(CCUHsApi hayStack, String equipRef, VavEquip equip) {
        double flowVelocity = getFlowVelocityVav(hayStack, equipRef, equip);
        double ductArea = getDuctCrossSectionAreaVav(hayStack, equipRef, equip);
        double airflowCfm = flowVelocity * ductArea;
        equip.getAirFlowSensor().writeHisVal(flowVelocity);
        equip.getAirVelocity().writeHisVal(flowVelocity);
        CcuLog.i(L.TAG_CCU_ZONE,"flowVelocity " + flowVelocity + " ductArea "
                + ductArea+" airflowCfm "+airflowCfm);
        return airflowCfm;
    }

    public static double getMaxCFMCooling(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and max and (trueCfm or trueCFM) and cooling and equipRef == \""+equipId+"\"");
    }
    
    public static double getMinCFMCooling(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and min and (trueCfm or trueCFM) and cooling and equipRef == \""+equipId+"\"");
    }
    
    public static double getMaxCFMReheating(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and max and (trueCfm or trueCFM) and (heating or reheat) and equipRef == \""+equipId+"\"");
    }
    
    public static double getMinCFMReheating(CCUHsApi hayStack, String equipId) {
        return hayStack.readDefaultVal("config and min and (trueCfm or trueCFM) and (heating or reheat) and equipRef == \""+equipId+"\"");
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
