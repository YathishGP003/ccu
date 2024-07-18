package a75f.io.logic.tuners;

import java.util.HashMap;

import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;

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

    private static final String BUILDING_ZONE_DIFFERENTIAL = "buildingToZoneDifferential";

    
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
        tunerValMap.put(BUILDING_LIMIT_MIN, Domain.buildingEquip.getBuildingLimitMin().readPriorityVal());
        tunerValMap.put(BUILDING_LIMIT_MAX, Domain.buildingEquip.getBuildingLimitMax().readPriorityVal());
        tunerValMap.put(MIN_COOLING_USER_LIMIT, Domain.buildingEquip.getCoolingUserLimitMin().readPriorityVal());
        tunerValMap.put(MAX_COOLING_USER_LIMIT, Domain.buildingEquip.getCoolingUserLimitMax().readPriorityVal());
        tunerValMap.put(MIN_HEATING_USER_LIMIT, Domain.buildingEquip.getHeatingUserLimitMin().readPriorityVal());
        tunerValMap.put(MAX_HEATING_USER_LIMIT, Domain.buildingEquip.getHeatingUserLimitMax().readPriorityVal());
        tunerValMap.put(TEMP_DEAD_LEEWAY, TunerUtil.readBuildingAndSystemTunerValByDomainName(DomainName.zoneTemperatureDeadLeeway));
        tunerValMap.put(BUILDING_ZONE_DIFFERENTIAL, Domain.buildingEquip.getBuildingToZoneDifferential().readPriorityVal());
       /* tunerValMap.put(COOLING_DEADBAND, SchedulabeLimits.Companion.getCoolingDeadBand());
        tunerValMap.put(HEATING_DEADBAND, SchedulabeLimits.Companion.getHeatingDeadBand());*/
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
