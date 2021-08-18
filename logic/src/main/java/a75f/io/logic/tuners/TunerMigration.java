package a75f.io.logic.tuners;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;

/**
 * Created by mahesh on 19-02-2021.
 */
class TunerMigration {
    static String TAG = "CCU_migration";

    public static void migrateTunerIfRequired(String id, String shortDis) {

        getBuildingValMap().entrySet().forEach(m -> {

            if (m.getKey().equalsIgnoreCase(shortDis)) {
                ValueObj valueObj = m.getValue();
                boolean isUpdated = false;
                HashMap tunerMap = CCUHsApi.getInstance().readMapById(id);
                Point tunerPoint = new Point.Builder().setHashMap(tunerMap).build();
                Point.Builder pointBuilder = new Point.Builder().setHashMap(tunerMap);

                if (!tunerPoint.getMaxVal().equals(valueObj.getMaxVal())) {
                    pointBuilder.setMaxVal(valueObj.maxVal);
                    isUpdated = true;
                }
                if (!tunerPoint.getMinVal().equals(valueObj.getMinVal())) {
                    pointBuilder.setMinVal(valueObj.minVal);
                    isUpdated = true;
                }
                if (!tunerPoint.getIncrementVal().equals(valueObj.getIncVal())) {
                    pointBuilder.setIncrementVal(valueObj.incVal);
                    isUpdated = true;
                }
                Point updatedPoint = pointBuilder.build();

                if (isUpdated) {
                    CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.getId());

                    CcuLog.i(TAG, "Migration success" + updatedPoint.getId());
                }
            }
        });
    }

    /*
     * insert new ValueObj if any tuner value update
     * */
    public static HashMap<String, ValueObj> getBuildingValMap() {
        HashMap<String, ValueObj> valueObjMap = new HashMap<>();
        valueObjMap.put("zonePriorityMultiplier", new ValueObj("0.0", "10", "0.1"));
        valueObjMap.put("stage1CoolingAirflowTempLowerOffset", new ValueObj("-120", "0.0", "1.0"));
        valueObjMap.put("adrHeatingDeadband", new ValueObj("0.0", "10", "0.5"));
        valueObjMap.put("adrCoolingDeadband", new ValueObj("0.0", "10", "0.5"));
        valueObjMap.put("snHeatingAirflowTemp", new ValueObj("65", "150", "1.0"));
        valueObjMap.put("snCoolingAirflowTemp", new ValueObj("35", "75", "1.0"));
        valueObjMap.put("zoneDeadTime", new ValueObj("1.0", "300", "1.0"));
        valueObjMap.put("forcedOccupiedTime", new ValueObj("30", "300", "1.0"));
        valueObjMap.put("autoAwayTime", new ValueObj("40", "300", "1.0"));
        valueObjMap.put("coolingDeadband", new ValueObj("0.0", "10", "0.5"));
        valueObjMap.put("heatingDeadband", new ValueObj("0.0", "10", "0.5"));
        valueObjMap.put("standaloneStage1Hysteresis", new ValueObj("0.0", "10", "0.5"));
        valueObjMap.put("standaloneHeatingDeadband", new ValueObj("0.0", "10", "0.5"));
        valueObjMap.put("standaloneCoolingDeadband", new ValueObj("0.0", "10", "0.5"));
        valueObjMap.put("2PipeFancoilHeatingThreshold", new ValueObj("80", "150", "1.0"));
        valueObjMap.put("2PipeFancoilCoolingThreshold", new ValueObj("35", "70", "1.0"));
        valueObjMap.put("zoneTemperatureDeadLeeway", new ValueObj("0.0", "20", "1.0"));
        valueObjMap.put("ccuAlarmVolumeLevel", new ValueObj("0.0", "7", "1.0"));
        valueObjMap.put("co2TimeInterval", new ValueObj("0.0", "10", "1.0"));
        valueObjMap.put("staticPressureSPTrim", new ValueObj("-0.5", "-0.01", "0.01"));
        valueObjMap.put("valveActuationStartDamperPosDuringSysHeating", new ValueObj("1.0", "100", "5"));
        valueObjMap.put("coolingDeadbandMultiplier", new ValueObj("0.5", "5", "0.1"));
        valueObjMap.put("heatingDeadbandMultiplier", new ValueObj("0.5", "5", "0.1"));
        valueObjMap.put("temperatureProportionalRange", new ValueObj("0", "10", "1"));
        valueObjMap.put("temperatureIntegralTime", new ValueObj("1", "60", "1"));
        valueObjMap.put("proportionalKFactor", new ValueObj("0.1", "1", "0.1"));
        valueObjMap.put("integralKFactor", new ValueObj("0.1", "1", "0.1"));
        valueObjMap.put("autoAwayZoneSetbackTemp", new ValueObj("1", "2", "1"));
        return valueObjMap;
    }
}