package a75f.io.logic.bo.building.system;

import static a75f.io.logic.bo.building.system.SystemController.State.OFF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 3/20/19.
 */

public abstract class SystemController
{
    public enum State {OFF, COOLING, HEATING}
    public enum EffectiveSatConditioning {SAT_OFF, SAT_COOLING, SAT_HEATING}
    public State systemState = OFF;
    public static final int MIN_DAMPER_FOR_CUMULATIVE_CALCULATION = 1;
    public boolean emergencyMode = false;
    public Set<String> deadZones = new HashSet<>();
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
            double currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and temp and sensor and (current or space) and equipRef == \""+q.getId()+"\"");
            double zonePriority = CCUHsApi.getInstance().readPointPriorityValByQuery
                    ("zone and priority and config and equipRef ==  \"" + q.getId() + "\"");
            if (currentTemp < buildingLimitMin && currentTemp >= (buildingLimitMin-tempDeadLeeway) && zonePriority!= 0) {
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
            double currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and temp and sensor and (current or space) and equipRef == \""+q.getId()+"\"");
            double zonePriority = CCUHsApi.getInstance().readPointPriorityValByQuery
                    ("zone and priority and config and equipRef ==  \"" + q.getId() + "\"");
            if (currentTemp > buildingLimitMax && currentTemp <= (buildingLimitMax+tempDeadLeeway) && zonePriority!=0) {
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

    public boolean isZoneDead(String equipRef) {

        /*HashMap<Object, Object> point = CCUHsApi.getInstance().readEntity("point and (heartbeat or heartBeat) and equipRef == \""+equipRef+"\"");
        if(!point.isEmpty()){
            HisItem hisItem = CCUHsApi.getInstance().curRead(point.get("id").toString());
            if (hisItem == null) {
                CcuLog.e(L.TAG_CCU_SYSTEM, "Equip dead! "+equipRef+" , Heartbeat does not exist");
                return true;
            }
            if ((System.currentTimeMillis() - hisItem.getDateInMillis()) > 15 * 60 * 1000) {
                CcuLog.e(L.TAG_CCU_SYSTEM, "Equip dead! "+equipRef+" , Heartbeat "+hisItem.getDate().toString());
                return true;
            }
        }*/

        double currentTemp = CCUHsApi.getInstance().readHisValByQuery("(current or space) and temp and sensor and equipRef == \"" + equipRef + "\"");

        double buildingLimitMax =  BuildingTunerCache.getInstance().getBuildingLimitMax();
        double buildingLimitMin =  BuildingTunerCache.getInstance().getBuildingLimitMin();

        double tempDeadLeeway = BuildingTunerCache.getInstance().getTempDeadLeeway();

        if (currentTemp > (buildingLimitMax + tempDeadLeeway) || (currentTemp < buildingLimitMin - tempDeadLeeway)) {
            CcuLog.e(L.TAG_CCU_SYSTEM, "Equip dead! "+equipRef+" , Current temp "+currentTemp);
            return true;
        }
        return false;
    }

    public boolean isRFDead(String equip) {

        if (equip == null) {
            CcuLog.e(L.TAG_CCU_SYSTEM, "Profile does not have linked equip , assume RF is dead");
            return true;
        }

        HashMap<Object, Object> point = CCUHsApi.getInstance().readEntity("point and (heartbeat or heartBeat) and equipRef == \""+equip+"\"");
        if(!point.isEmpty()){
            HisItem hisItem = CCUHsApi.getInstance().curRead(point.get("id").toString());
            if (hisItem == null) {
                CcuLog.e(L.TAG_CCU_SYSTEM, "RF dead! , Heartbeat does not exist for "+equip);
                return true;
            }
            double zoneDeadTime = TunerUtil.readTunerValByQuery("zone and dead and time", equip);
            if (zoneDeadTime == 0) {
                CcuLog.e(L.TAG_CCU_SYSTEM, "Invalid value for zoneDeadTime tuner, use default "+equip);
                zoneDeadTime = 15;
            }
            if ((System.currentTimeMillis() - hisItem.getDateInMillis()) > zoneDeadTime * 60 * 1000) {
                return true;
            }
        }
        return false;
    }
}
