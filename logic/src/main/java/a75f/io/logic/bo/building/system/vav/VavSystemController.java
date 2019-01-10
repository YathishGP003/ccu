package a75f.io.logic.bo.building.system.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.EvictingQueue;

import a75.io.algos.ControlLoop;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;

import static a75f.io.logic.bo.building.ZonePriority.NO;
import static a75f.io.logic.bo.building.system.vav.VavSystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.vav.VavSystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.vav.VavSystemController.State.OFF;
/**
 * Created by samjithsadasivan on 11/9/18.
 */

/**
 * DxController applies Weighted average and Moving average filters on temperature diffs.
 * MA value is used to determine AHU change over. WA determines the heating signal.
 */
public class VavSystemController
{
    
    private static VavSystemController instance = new VavSystemController();
    
    ControlLoop piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> weightedAverageCoolingOnlyLoadMAQueue = EvictingQueue.create(15);
    EvictingQueue<Double> weightedAverageHeatingOnlyLoadMAQueue = EvictingQueue.create(15);;
    
    int ciDesired;
    int comfortIndex = 0;
    
    public enum State {OFF, COOLING, HEATING};
    State systemState = OFF;
    
    double totalCoolingLoad = 0;
    double totalHeatingLoad = 0;
    int zoneCount = 0;
    
    double weightedAverageCoolingOnlyLoadSum;
    double weightedAverageHeatingOnlyLoadSum;
    double weightedAverageLoadSum;
    
    double weightedAverageCoolingOnlyLoad;
    double weightedAverageHeatingOnlyLoad;
    double weightedAverageLoad;
    
    double weightedAverageCoolingOnlyLoadMA;
    double weightedAverageHeatingOnlyLoadMA;
    
    private VavSystemController()
    {
        piController = new ControlLoop();
        piController.setProportionalSpread(2);
    }
    
    public static VavSystemController getInstance() {
        if (instance == null) {
            instance = new VavSystemController();
        }
        return instance;
    }
    
    public void runVavSystemControlAlgo() {
        
        double prioritySum = 0;
        VavSystemProfile profile = (VavSystemProfile) L.ccu().systemProfile;
        ciDesired = (int)profile.getUserInputVal("desired and ci");
        Log.d("CCU", "runVavSystemControlAlgo -> ciDesired: "+ciDesired);
    
        weightedAverageCoolingOnlyLoadSum = weightedAverageHeatingOnlyLoadSum = weightedAverageLoadSum = 0;
        
        for (Floor f: HSUtil.getFloors())
        {
            for(Zone z: HSUtil.getZones(f.getId())) {
                if (getZoneCurrentTemp(z.getId()) == 0) {
                    continue;
                }
                
                double zoneCurTemp = getZoneCurrentTemp(z.getId());
                double zoneTargetTemp = getZoneTempTarget();
                
                double zoneCoolingLoad = zoneTargetTemp >= zoneCurTemp ? 0 : zoneCurTemp -zoneTargetTemp - 1/* Cool DB*/ ;
                double zoneHeatingLoad = zoneTargetTemp <= zoneCurTemp ? 0 : zoneTargetTemp - zoneCurTemp - 1 /*Heat DB*/;
    
                double zoneDynamicPriority = getDynamicPriority(zoneCoolingLoad > 0 ? zoneCoolingLoad : zoneHeatingLoad, z.getId());
                
                totalCoolingLoad += zoneCoolingLoad;
                totalHeatingLoad += zoneHeatingLoad;
                
                zoneCount++;
                
                weightedAverageCoolingOnlyLoadSum += zoneCoolingLoad * zoneDynamicPriority;
                weightedAverageHeatingOnlyLoadSum += zoneHeatingLoad * zoneDynamicPriority;
                weightedAverageLoadSum =+ (zoneCoolingLoad * zoneDynamicPriority) - (zoneHeatingLoad * zoneDynamicPriority);
                
                
                double zone_dxCI = getZoneCurrentTemp(z.getId()) - getZoneDesiredTemp();
                
                prioritySum += zoneDynamicPriority;
                Log.d("CCU", z.getDisplayName()+" zoneDynamicPriority: "+zoneDynamicPriority+" zoneCoolingLoad: "+zoneCoolingLoad+" zoneHeatingLoad: "+zoneHeatingLoad);
            }
        
        }
    
        if (prioritySum == 0) {
            Log.d("CCU", "No valid temperature, Skip DxCiAlgo");
            return;
        }
        
        weightedAverageCoolingOnlyLoad = weightedAverageCoolingOnlyLoad / prioritySum;
        weightedAverageHeatingOnlyLoad = weightedAverageHeatingOnlyLoad / prioritySum;
        weightedAverageLoad = weightedAverageLoadSum / prioritySum;
        
        comfortIndex = (int)(totalCoolingLoad + totalHeatingLoad) /zoneCount;
    
        weightedAverageCoolingOnlyLoadMAQueue.add(weightedAverageCoolingOnlyLoad);
        weightedAverageHeatingOnlyLoadMAQueue.add(weightedAverageHeatingOnlyLoad);
        
        double weightedAverageCoolingOnlyLoadMASum = 0;
        for (double val : weightedAverageCoolingOnlyLoadMAQueue) {
            weightedAverageCoolingOnlyLoadMASum += val;
        }
        weightedAverageCoolingOnlyLoadMA = weightedAverageCoolingOnlyLoadMASum/weightedAverageCoolingOnlyLoadMAQueue.size();
        
        double weightedAverageHeatingOnlyLoadMASum = 0;
        for (double val : weightedAverageHeatingOnlyLoadMAQueue) {
            weightedAverageHeatingOnlyLoadMASum += val;
        }
        weightedAverageHeatingOnlyLoadMA = weightedAverageHeatingOnlyLoadMASum/weightedAverageHeatingOnlyLoadMAQueue.size();
    
        if (weightedAverageCoolingOnlyLoadMA > 0) {
            if (systemState != COOLING) {
                systemState = COOLING;
                piController.reset();
            }
        } else if ((weightedAverageCoolingOnlyLoadMA == 0 && weightedAverageHeatingOnlyLoadMA > 0)) {
            if (systemState != HEATING)
            {
                systemState = HEATING;
                piController.reset();
            }
            
        } else {
            systemState = OFF;
        }
        
        
        piController.dump();
        if (systemState == COOLING) {
            heatingSignal = 0;
            coolingSignal = (int)piController.getLoopOutput(weightedAverageCoolingOnlyLoadMA, 0);
        } else if (systemState == HEATING){
            coolingSignal = 0;
            heatingSignal = (int)piController.getLoopOutput(weightedAverageHeatingOnlyLoadMA, 0);
        } else {
            coolingSignal = 0;
            heatingSignal = 0;
        }
        piController.dump();
        Log.d("CCU", "weightedAverageCoolingOnlyLoadMA: "+weightedAverageCoolingOnlyLoadMA+" weightedAverageHeatingOnlyLoadMA: "
                                                    +weightedAverageHeatingOnlyLoadMA +" systemState: "+systemState+" coolingSignal: "+coolingSignal+" heatingSignal: "+heatingSignal);
    }
    
    public int getCoolingSignal() {
        return coolingSignal;
    }
    
    public int getHeatingSignal() {
        return heatingSignal;
    }
    
    public boolean isAllZonesHeating() {
        for (ZoneProfile p: L.ccu().zoneProfiles)
        {
            if (p.getState() != ZoneState.HEATING) {
                Log.d("dx"," Equip "+p.getProfileType()+" is not in Heating");
                return false;
            }
        }
        return true;
    }
    
    public boolean isAnyZoneCooling() {
        for (ZoneProfile p: L.ccu().zoneProfiles)
        {
            System.out.println(" Zone State " +p.state);
            if (p.getState() == ZoneState.COOLING) {
                Log.d("dx"," Equip "+p.getProfileType()+" is in Cooling");
                return true;
            }
        }
        return false;
    }
    
    public double getZoneCurrentTemp(String zoneRef) {
        double tempSum = 0;
        int tempCount = 0;
        for (ZoneProfile p : L.ccu().zoneProfiles)
        {
            if (p.getEquip().getZoneRef().equals(zoneRef) && p.getCurrentTemp() > 0) {
                tempSum += p.getCurrentTemp();
                tempCount++;
            }
        }
        return tempCount > 0 ? tempSum/tempCount : 0;
    }
    
    public ZonePriority getZonePriority(String zoneRef) {
        ZonePriority priority = NO;
        for (ZoneProfile p : L.ccu().zoneProfiles)
        {
            if (p.getEquip().getZoneRef().equals(zoneRef) && p.getPriority().ordinal() > priority.ordinal()) {
                priority = p.getPriority();
            }
        }
        return priority;
    }
    
    @JsonIgnore
    public double getZoneDesiredTemp()
    {
        return 72;//TODO - TEMP
    }
    
    @JsonIgnore
    public double getZoneTempTarget() //Humidity compensated
    {
        return getZoneDesiredTemp();//TODO - TEMP
    }
    
    @JsonIgnore
    public double getDynamicPriority(double zoneLoad, String zoneRef) {
        
        ZonePriority p = getZonePriority(zoneRef);
        if (getZoneCurrentTemp(zoneRef) == 0) {
            return p.val;
        }
    
        double zonePrioritySpread = 2;//TODO - Tuner
        double zonePriorityMultiplier = 1.3; //TODO - Tuner
        //zoneDynamicPriority = zoneBasePriority*((zonePriorityMultiplier )^(rounddownTo10(zoneCoolingLoad/zonePrioritySpread))
        
        return p.val * Math.pow(zonePriorityMultiplier, (zoneLoad/zonePrioritySpread) > 10 ? 10 : (zoneLoad/zonePrioritySpread));
    }
    
    public State getSystemState() {
        return systemState;
    }
    
}
