package a75f.io.renatus.tuners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

public class TunerValidationHandler {

    public static boolean validateTunersForLimitsViolation(CCUHsApi hayStack, List<HashMap> updatedTunerValues) {

        double coolingDbVal = 0;
        double heatingDbVal = 0;
        double zoneSetback = 0;
        double zoneDifferential = 0;
        //Multiple deadband tuners might have changed in a single update.
        //Iterate through all the updates and find highest values for cooling and heating deadbands.
        for (Map<Object, Object> updatedTuner : updatedTunerValues) {
            Object id = updatedTuner.get("id");
            Object val = updatedTuner.get("newValue");
            Object level = updatedTuner.get("newLevel");

            if (id != null && val != null && level != null) {
                if (Integer.parseInt(level.toString()) == 16) {
                    Point updatedPoint = new Point.Builder()
                            .setHashMap(hayStack.readMapById(id.toString())).build();
                    List<String> markers = updatedPoint.getMarkers();
                    double pointVal = Double.parseDouble(val.toString());
                    if (markers.contains("deadband") && markers.contains("base") && markers.contains("cooling")
                            && pointVal > coolingDbVal) {
                        coolingDbVal = pointVal;
                    } else if (markers.contains("deadband") && markers.contains("base") && markers.contains("heating")
                            && pointVal > heatingDbVal) {
                        heatingDbVal = pointVal;
                    } else if (markers.contains("unoccupied") && markers.contains("setback")) {
                        zoneSetback = pointVal;
                    } else if (markers.contains("building") && markers.contains("zone") &&
                            markers.contains("differential")) {
                        zoneDifferential = pointVal;
                    }
                }
            }
        }
        if (coolingDbVal > 0 || heatingDbVal > 0) {
            String tunerEquipRef = hayStack.readEntity("tuner and equip").get("id").toString();
            double updatedCoolDb = coolingDbVal == 0? TunerUtil.getCoolingDeadband(tunerEquipRef) : coolingDbVal;
            double updatedHeatDb = heatingDbVal == 0? TunerUtil.getHeatingDeadband(tunerEquipRef) : heatingDbVal;
            CcuLog.d(L.TAG_CCU_TUNER, "checkLimitsWithUpdatedDeadBand cdb " +updatedCoolDb+" hdb "+updatedHeatDb);
            if (!checkLimitsWithUpdatedDeadBand(updatedCoolDb, updatedHeatDb)) {
                return false;
            }
        }

        if (zoneSetback > 0 || zoneDifferential > 0) {
            double updatedZoneSetback = zoneSetback == 0?
                    TunerUtil.readBuildingTunerValByQuery("unocc and setback") : zoneSetback;
            double updatedZoneDifferential = zoneDifferential == 0 ?
                    TunerUtil.readBuildingTunerValByQuery("building and zone and differential") : zoneDifferential;
            CcuLog.d(L.TAG_CCU_TUNER, "checkLimitsSetbackAndZoneDifferential setback " +updatedZoneSetback+
                                                    " differential: "+updatedZoneDifferential);
            if (!checkLimitsSetbackAndZoneDifferential(updatedZoneSetback, updatedZoneDifferential)) {
                return false;
            }
        }

        return true;
    }

    private static boolean checkLimitsWithUpdatedDeadBand(double coolingDb, double heatingDb) {
        double deadbandSum = coolingDb + heatingDb;
        BuildingTunerCache buildingTuner = BuildingTunerCache.getInstance();
        if ((buildingTuner.getMaxCoolingUserLimit() - buildingTuner.getMaxHeatingUserLimit()) >= deadbandSum
                && (buildingTuner.getMaxCoolingUserLimit() - buildingTuner.getMinCoolingUserLimit()) >= coolingDb
                && (buildingTuner.getMinCoolingUserLimit() - buildingTuner.getMinHeatingUserLimit()) >= deadbandSum
                && (buildingTuner.getMaxHeatingUserLimit() - buildingTuner.getMinHeatingUserLimit()) >= heatingDb) {
            return true;
        }
        CcuLog.d(L.TAG_CCU_TUNER, " checkLimitsWithUpdatedDeadBand Failed!! "+" coolingDb "+coolingDb
                +" heatingDb "+heatingDb+" getMaxCoolingUserLimit "+buildingTuner.getMaxCoolingUserLimit()
                +" getMinCoolingUserLimit "+buildingTuner.getMinCoolingUserLimit()
                +" getMaxHeatingUserLimit "+buildingTuner.getMaxHeatingUserLimit()
                +" getMinHeatingUserLimit "+buildingTuner.getMinHeatingUserLimit()
        );
        return false;
    }

    private static boolean checkLimitsSetbackAndZoneDifferential(double setback, double zoneDifferential) {
        double setbackSum = setback + zoneDifferential;
        BuildingTunerCache buildingTuner = BuildingTunerCache.getInstance();
        if ((buildingTuner.getBuildingLimitMax() - buildingTuner.getMaxCoolingUserLimit()) >= setbackSum
                && (buildingTuner.getMinHeatingUserLimit() - buildingTuner.getBuildingLimitMin()) >= setbackSum) {
            return true;
        }
        CcuLog.d(L.TAG_CCU_TUNER, " checkLimitsSetbackAndZoneDifferential Failed!! "+" setback "+setback
                +" zoneDifferential "+zoneDifferential+" getBuildingLimitMax "+buildingTuner.getBuildingLimitMax()
                +" getBuildingLimitMin "+buildingTuner.getBuildingLimitMin()
                +" getMaxCoolingUserLimit "+buildingTuner.getMaxCoolingUserLimit()
                +" getMinHeatingUserLimit "+buildingTuner.getMinHeatingUserLimit()
        );
        return false;
    }
}
