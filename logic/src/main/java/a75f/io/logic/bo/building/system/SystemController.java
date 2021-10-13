package a75f.io.logic.bo.building.system;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Occupied;
import a75f.io.logic.L;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;

/**
 * Created by samjithsadasivan on 3/20/19.
 */

public abstract class SystemController
{
    public enum State {OFF, COOLING, HEATING};
    public State systemState = OFF;
    
    public boolean emergencyMode = false;
    
    public abstract int getCoolingSignal() ;
    public abstract int getHeatingSignal();
    public abstract double getAverageSystemHumidity();
    public abstract double getAverageSystemTemperature();
    
    public boolean buildingLimitMinBreached(String equipType) {
        double buildingLimitMin =  BuildingTunerCache.getInstance().getBuildingLimitMin();
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> equips = hayStack.readAll("equip and zone and "+equipType);
        
        for (HashMap h : equips)
        {
            Equip q = new Equip.Builder().setHashMap(h).build();
            double currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and temp and sensor and current and equipRef == \""+q.getId()+"\"");
            if (currentTemp < buildingLimitMin && currentTemp > (buildingLimitMin-tempDeadLeeway)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean buildingLimitMaxBreached(String equipType) {
        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> equips = hayStack.readAll("equip and zone and "+equipType);
        
        for (HashMap h : equips)
        {
            Equip q = new Equip.Builder().setHashMap(h).build();
            double currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and temp and sensor and current and equipRef == \""+q.getId()+"\"");
            if (currentTemp > buildingLimitMax && currentTemp < (buildingLimitMax+tempDeadLeeway)) {
                return true;
            }
        }
        return false;
    }
    
    public State getSystemState() {
        return systemState;
    }
    
    public boolean isEmergencyMode(){
        return emergencyMode;
    }
    
    public double getSystemCO2WA() {
        return 0;
    }
    
    public abstract SystemController.State getConditioningForecast(Occupied occupiedSchedule);
    
    public void reset() {
    
    }
}
