package a75f.io.logic.tuners;

import java.util.HashMap;

/***
 * Caches commonly used building tuners and refreshes them every minute.
 */
public class BuildingTunerCache {
    
    private static final String BUILDING_LIMIT_MIN = "buildingLimitMin";
    private static final String BUILDING_LIMIT_MAX = "buildingLimitMax";
    private static final String TEMP_DEAD_LEEWAY = "tempDeadLeeway";
    private static final String MIN_COOLING_USER_LIMIT = "minCoolingUserLimit";
    private static final String MAX_COOLING_USER_LIMIT = "maxCoolingUserLimit";
    private static final String MIN_HEATING_USER_LIMIT = "minHeatingUserLimit";
    private static final String MAX_HEATING_USER_LIMIT = "maxHeatingUserLimit";
    
    private static BuildingTunerCache instance = null;
    
    BuildingTunerCache() {
        updateTuners();
    }
    
    private HashMap<String, Double> tunerValMap = new HashMap<>();
    
    public static BuildingTunerCache getInstance() {
        if (instance == null) {
            instance = new BuildingTunerCache();
        }
        return instance;
    }
    
    public void updateTuners() {
        tunerValMap.put(BUILDING_LIMIT_MIN, TunerUtil.readBuildingTunerValByQuery("building and limit and min"));
        tunerValMap.put(BUILDING_LIMIT_MAX, TunerUtil.readBuildingTunerValByQuery("building and limit and max"));
        tunerValMap.put(TEMP_DEAD_LEEWAY, TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway"));
        tunerValMap.put(MIN_COOLING_USER_LIMIT, TunerUtil.readBuildingTunerValByQuery("limit and min and cooling and user"));
        tunerValMap.put(MAX_COOLING_USER_LIMIT, TunerUtil.readBuildingTunerValByQuery("limit and max and cooling and user"));
        tunerValMap.put(MIN_HEATING_USER_LIMIT, TunerUtil.readBuildingTunerValByQuery("limit and min and heating and user"));
        tunerValMap.put(MAX_HEATING_USER_LIMIT, TunerUtil.readBuildingTunerValByQuery("limit and max and heating and user"));
    }
    
    public Double getBuildingLimitMin() {
        return tunerValMap.get(BUILDING_LIMIT_MIN);
    }
    
    public Double getBuildingLimitMax() {
        return tunerValMap.get(BUILDING_LIMIT_MAX);
    }
    
    public Double getTempDeadLeeway() {
        return tunerValMap.get(TEMP_DEAD_LEEWAY);
    }
    
    public Double getMinCoolingUserLimit() {
        return tunerValMap.get(MIN_COOLING_USER_LIMIT);
    }
    
    public Double getMaxCoolingUserLimit() {
        return tunerValMap.get(MAX_COOLING_USER_LIMIT);
    }
    
    public Double getMinHeatingUserLimit() {
        return tunerValMap.get(MIN_HEATING_USER_LIMIT);
    }
    
    public Double getMaxHeatingUserLimit() {
        return tunerValMap.get(MAX_HEATING_USER_LIMIT);
    }
}
