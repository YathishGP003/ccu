package a75f.io.logic.bo.building.system;

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
import a75f.io.logic.tuners.SystemTunerUtil;

import static a75f.io.logic.bo.building.ZonePriority.NO;
import static a75f.io.logic.bo.building.system.DxCIController.State.COOLING;
import static a75f.io.logic.bo.building.system.DxCIController.State.HEATING;
import static a75f.io.logic.bo.building.system.DxCIController.State.NA;

/**
 * Created by samjithsadasivan on 11/9/18.
 */

public class DxCIController
{
    
    public double dxCI_CO_MA = 0;
    public double dxCI_WA = 0;
    
    private static DxCIController instance = new DxCIController();
    
    ControlLoop piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> dxCIMABuffer = EvictingQueue.create(15);;
    
    int ciDesired;
    
    public enum State {NA, COOLING, HEATING};
    State dxState = NA;
    
    private DxCIController()
    {
        piController = new ControlLoop();
        piController.setProportionalSpread(2);
    }
    
    public static DxCIController getInstance() {
        if (instance == null) {
            instance = new DxCIController();
        }
        return instance;
    }
    
    public void runDxCIAlgo() {
        
        double dxCISumCO = 0;
        double prioritySum = 0;
        ciDesired = (int)SystemTunerUtil.getDesiredCI();
        Log.d("CCU", "runDxCIAlgo-> ciDesired: "+ciDesired);
        
        for (Floor f: HSUtil.getFloors())
        {
            for(Zone z: HSUtil.getZones(f.getId())) {
                if (getZoneCurrentTemp(z.getId()) == 0) {
                    continue;
                }
                double zone_dxCI = getZoneCurrentTemp(z.getId()) - getZoneDesiredTemp();
                double zone_dp = getDynamicPriority(ciDesired, z.getId());
    
                dxCISumCO += zone_dxCI * zone_dp;
                prioritySum += zone_dp;
                Log.d("CCU", "MA zone_dxCI: "+zone_dxCI+" zone_dp: "+zone_dp+" dxCISumCO: "+dxCISumCO);
            }
        
        }
        if (prioritySum == 0) {
            Log.d("CCU", "No valid temperature, Skip DxCiAlgo");
            return;
        }
        
        dxCIMABuffer.add(dxCISumCO / prioritySum);
        
        double dxCiMASum = 0;
        for (double val : dxCIMABuffer) {
            dxCiMASum += val;
        }
        
        // A positive number means that we need cooling on average in the building and a negative number
        // means that we need heating on average in the building
        dxCI_CO_MA = dxCiMASum/dxCIMABuffer.size();
        
        if (isAllZonesCooling() || dxCI_CO_MA > ciDesired * 1 /*MULTIPLIER to be a tuner*/) {
            if (dxState != COOLING) {
                dxState = COOLING;
                piController.reset();
            }
        } else if (isAllZonesHeating() || dxCI_CO_MA < -1 * ciDesired * 1 /*MULTIPLIER*/) {
            if (dxState != HEATING) {
                dxState = HEATING;
                piController.reset();
            }
        }
        
        double dxCISum = 0;
        
        for (Floor f: HSUtil.getFloors())
        {
            for(Zone z: HSUtil.getZones(f.getId())) {
                if (getZoneCurrentTemp(z.getId()) == 0) {
                    continue;
                }
                double zone_dxCI = getZoneCurrentTemp(z.getId()) - getZoneTempTarget();
                double zone_dp = getDynamicPriority(ciDesired, z.getId());
            
                dxCISum += zone_dxCI * zone_dp;
                Log.d("CCU", "WA zone_dxCI: "+zone_dxCI+" zone_dp: "+zone_dp+" dxCISum: "+dxCISum+" dxState: "+dxState);
            }
        }
        
        dxCI_WA = dxCISum/prioritySum ;
        
        //The RTU will start cooling whenever the dxCI_WA > dxCI offset ?
        piController.dump();
        double loopOp = piController.getLoopOutput(Math.abs(dxCI_WA), 0);
        piController.dump();
        
        if (dxCI_WA < 0) {
            coolingSignal = 0;
            heatingSignal = (int)loopOp;
        } else {
            heatingSignal = 0;
            coolingSignal = (int) loopOp;
        }
        Log.d("CCU", "runDxCIAlgo-> dxCI_CO_MA: "+dxCI_CO_MA+" dxCI_WA: "+dxCI_WA+" dxState: "+dxState+" coolingSignal: "+coolingSignal+" heatingSignal: "+heatingSignal);
    }
    
    public int getCoolingSignal() {
        if (dxState != COOLING) {
            return 0;
        }
        return coolingSignal;
    }
    
    public int getHeatingSignal() {
        if (dxState != HEATING) {
            return 0;
        }
        return heatingSignal;
    }
    
    public boolean isAllZonesHeating() {
        for (ZoneProfile p: L.ccu().zoneProfiles)
        {
            System.out.println(" Zone State " +p.state);
            if (p.state != ZoneState.HEATING) {
                Log.d("dx"," Equip "+p.getProfileType()+" is not in Heating");
                return false;
            }
        }
        return true;
    }
    
    public boolean isAllZonesCooling() {
        for (ZoneProfile p: L.ccu().zoneProfiles)
        {
            if (p.state != ZoneState.COOLING) {
                Log.d("dx"," Equip "+p.getProfileType()+" is not in Cooling");
                return false;
            }
        }
        return true;
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
    
    public int getZonePriority(String zoneRef) {
        ZonePriority priority = NO;
        for (ZoneProfile p : L.ccu().zoneProfiles)
        {
            if (p.getEquip().getZoneRef().equals(zoneRef) && p.getPriority().ordinal() > priority.ordinal()) {
                priority = p.getPriority();
            }
        }
        return priority.multiplier;
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
    public double getDynamicPriority(int ciTarget, String zoneRef) {
        
        if (getZoneCurrentTemp(zoneRef) == 0) {
            return getZonePriority(zoneRef);
        }
        /* Dynamic priority is generated by multiplying zone priority by 1.3 for every multiple of comfort slider value away
           from desired. The idea is that occupants in a zone that is further away from the desired are exponentially more
           likely to feel uncomfortable
         */
        return Math.pow(1.3 , (Math.abs(getZoneCurrentTemp(zoneRef) - getZoneDesiredTemp())/ciTarget));
    }
    
    public State getDxCIRtuState() {
        return dxState;
    }
    
}
