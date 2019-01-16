package a75f.io.logic.bo.building.system.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.EvictingQueue;

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.ControlLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.tuners.TunerUtil;

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
                double zoneTargetTemp = getZoneTempTarget(z.getId());
                
                double zoneCoolingLoad = zoneTargetTemp >= zoneCurTemp ? 0 : zoneCurTemp -zoneTargetTemp - 1/* Cool DB*/ ;
                double zoneHeatingLoad = zoneTargetTemp <= zoneCurTemp ? 0 : zoneTargetTemp - zoneCurTemp - 1 /*Heat DB*/;
    
                double zoneDynamicPriority = getDynamicPriority(zoneCoolingLoad > 0 ? zoneCoolingLoad : zoneHeatingLoad, z.getId());
                
                totalCoolingLoad += zoneCoolingLoad;
                totalHeatingLoad += zoneHeatingLoad;
                
                zoneCount++;
                
                weightedAverageCoolingOnlyLoadSum += zoneCoolingLoad * zoneDynamicPriority;
                weightedAverageHeatingOnlyLoadSum += zoneHeatingLoad * zoneDynamicPriority;
                weightedAverageLoadSum =+ (zoneCoolingLoad * zoneDynamicPriority) - (zoneHeatingLoad * zoneDynamicPriority);
                
                prioritySum += zoneDynamicPriority;
                Log.d("CCU", z.getDisplayName()+" zoneDynamicPriority: "+zoneDynamicPriority+" zoneCoolingLoad: "+zoneCoolingLoad+" zoneHeatingLoad: "+zoneHeatingLoad);
                Log.d("CCU", z.getDisplayName()+" weightedAverageCoolingOnlyLoadSum:"+weightedAverageCoolingOnlyLoadSum+" weightedAverageHeatingOnlyLoadSum "+weightedAverageHeatingOnlyLoadSum);
            }
        
        }
    
        if (prioritySum == 0) {
            Log.d("CCU", "No valid temperature, Skip VavSystemControlAlgo");
            return;
        }
        
        weightedAverageCoolingOnlyLoad = weightedAverageCoolingOnlyLoadSum / prioritySum;
        weightedAverageHeatingOnlyLoad = weightedAverageHeatingOnlyLoadSum / prioritySum;
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
    
        normalizeAirflow();
        adjustDamperForCumulativeTarget();
        setDamperLimits();
    
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
    public double getZoneDesiredTemp(String zoneRef)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and air and temp and desired and zoneRef == \""+zoneRef+"\"");
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 72;
    }
    
    @JsonIgnore
    public double getZoneTempTarget(String zoneRef) //Humidity compensated
    {
        return getZoneDesiredTemp(zoneRef);//TODO - TEMP
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
    
    /*
    * Of all the zones, find the zone with the largest damperPosition. If this is not 100, then proportionally
    * increase damper positions in all zones such that this chosen one  has 100% damper opening.  Eg. if there are
    * calculated damper positions of 40, 70, 80, 90. Then we increase each by 11% to get rounded values of
    * 44, 78,89, 100% after normalizing.
    * */
    public void normalizeAirflow() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        double maxDamperPos = 0;
        for (HashMap m : vavEquips) {
            HashMap damper = hayStack.read("point and damper and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPos = hayStack.readHisValById(damper.get("id").toString());
            if ( damperPos >= maxDamperPos) {
                maxDamperPos = damperPos;
            }
        }
        
        if (maxDamperPos == 0) {
            Log.d("CCU"," Abort normalizeAirflow : maxDamperPos = "+maxDamperPos);
        }
        
        double targetPercent = (100 - maxDamperPos) * 100/ maxDamperPos ;
    
        for (HashMap m : vavEquips) {
            HashMap damper = hayStack.read("point and damper and base and cmd and equipRef == \""+m.get("id").toString()+"\"");
        
            double damperPos = hayStack.readHisValById(damper.get("id").toString());
            int normalizedDamperPos = (int) (damperPos + damperPos * targetPercent/100);
            HashMap normalizedDamper = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            hayStack.writeHisValById(normalizedDamper.get("id").toString(), (double)normalizedDamperPos);
        }
        
        
    }
    /*
     * Take weighted damper opening (where weightedDamperOpening = (zone1_damper_opening*zone1_damper_size +
     * zone2_damper_opening*zone2_damper_size +..)/(zone1_damper_size + zone2_damper_size + .. )
     * If weighted damper opening is < targetCumulativeDamper  increase the dampers proportionally(each damper
     * by 1% of its value) till it is at least targetCumulativeDamper
     **/
    public void adjustDamperForCumulativeTarget() {
        
        double cumulativeDamperTarget = TunerUtil.readTunerValByQuery("target and cumulative and damper");
        double weightedDamperOpening = getWeightedDamperOpening();
        Log.d("CCU","weightedDamperOpening : "+weightedDamperOpening +" cumulativeDamperTarget : "+cumulativeDamperTarget);
    
        while(weightedDamperOpening > 0 && weightedDamperOpening < cumulativeDamperTarget) {
            adjustDamperOpening(1);
            weightedDamperOpening = getWeightedDamperOpening();
            Log.d("CCU","weightedDamperOpening : "+weightedDamperOpening +" cumulativeDamperTarget : "+cumulativeDamperTarget);
        }
    }
    
    public double getWeightedDamperOpening() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        int damperSizeSum = 0;
        int weightedDamperOpeningSum = 0;
        for (HashMap m : vavEquips) {
            HashMap damperPos = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            HashMap damperSize = hayStack.read("point and config and damper and size and equipRef == \""+m.get("id").toString()+"\"");
        
            double damperPosVal = hayStack.readHisValById(damperPos.get("id").toString());
            double damperSizeVal = hayStack.readDefaultValById(damperSize.get("id").toString());
            weightedDamperOpeningSum += damperPosVal * damperSizeVal;
            damperSizeSum += damperSizeVal;
        }
        return damperSizeSum == 0 ? 0 : weightedDamperOpeningSum / damperSizeSum;
    }
    
    public void adjustDamperOpening(int percent) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        for (HashMap m : vavEquips) {
            HashMap damperPos = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double damperPosVal = hayStack.readHisValById(damperPos.get("id").toString());
            double adjustedDamperPos = damperPosVal + (damperPosVal * percent) /100.0;
            hayStack.writeHisValById(damperPos.get("id").toString() , adjustedDamperPos);
        }
    }
    
    public void setDamperLimits() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> vavEquips = hayStack.readAll("equip and vav and zone");
        for (HashMap m : vavEquips) {
            HashMap damperPos = hayStack.read("point and damper and normalized and cmd and equipRef == \""+m.get("id").toString()+"\"");
            double limitedDamperPos = hayStack.readHisValById(damperPos.get("id").toString());
            double minLimit = 0, maxLimit = 0;
            if (getSystemState() == COOLING) {
                minLimit = hayStack.readDefaultVal("point and zone and config and vav and min and damper and cooling");
                maxLimit = hayStack.readDefaultVal("point and zone and config and vav and max and damper and cooling");
            } else {
                minLimit = hayStack.readDefaultVal("point and zone and config and vav and min and damper and heating");
                maxLimit = hayStack.readDefaultVal("point and zone and config and vav and max and damper and heating");
            }
            Log.d("CCU"," minLimit "+minLimit+" maxLimit "+maxLimit);
            limitedDamperPos = Math.min(limitedDamperPos, maxLimit);
            limitedDamperPos = Math.max(limitedDamperPos, minLimit);
            hayStack.writeHisValById(damperPos.get("id").toString() , limitedDamperPos);
        }
    }
}
