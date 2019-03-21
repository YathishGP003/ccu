package a75f.io.logic.bo.building.system.dab;

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
import a75f.io.logic.bo.building.system.SystemController;

import static a75f.io.logic.bo.building.ZonePriority.NONE;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;

/**
 * Created by samjithsadasivan on 1/9/19.
 */

public class DabSystemController extends SystemController
{
    public double dxCI_CO_MA = 0;
    public double dxCI_WA = 0;
    
    private static DabSystemController instance = new DabSystemController();
    
    ControlLoop piController;
    
    int coolingSignal;
    int heatingSignal;
    
    EvictingQueue<Double> dxCIMABuffer = EvictingQueue.create(15);;
    
    int ciDesired;
    
    private DabSystemController()
    {
        piController = new ControlLoop();
        piController.setProportionalSpread(2);
    }
    
    public static DabSystemController getInstance() {
        return instance;
    }
    
    public void runDxCIAlgo() {
        
        double dxCISumCO = 0;
        double prioritySum = 0;
        ciDesired = 2;//TODO - (int)SystemTunerUtil.getDesiredCI();
        Log.d("CCU", "runDxCIAlgo-> ciDesired: " + ciDesired);
        
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
        
        if (isAllZonesCooling() || dxCI_CO_MA >= ciDesired * 1 /*MULTIPLIER to be a tuner*/) {
            if (systemState != COOLING) {
                systemState = COOLING;
                piController.reset();
            }
        } else if (isAllZonesHeating() || dxCI_CO_MA <= -1 * ciDesired * 1 /*MULTIPLIER*/) {
            if (systemState != HEATING) {
                systemState = HEATING;
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
                Log.d("CCU", "WA zone_dxCI: "+zone_dxCI+" zone_dp: "+zone_dp+" dxCISum: "+dxCISum+" dxState: "+systemState);
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
        Log.d("CCU", "runDxCIAlgo-> dxCI_CO_MA: "+dxCI_CO_MA+" dxCI_WA: "+dxCI_WA+" dxState: "+systemState+" coolingSignal: "+coolingSignal+" heatingSignal: "+heatingSignal);
    }
    
    @Override
    public int getCoolingSignal() {
        if (systemState != COOLING) {
            return 0;
        }
        return coolingSignal;
    }
    
    @Override
    public int getHeatingSignal() {
        if (systemState != HEATING) {
            return 0;
        }
        return heatingSignal;
    }
    
    @Override
    public int getSystemOccupancy() {
        return 0;
    }
    
    @Override
    public double getAverageSystemHumidity() {
        return 0;
    }
    @Override
    public double getAverageSystemTemperature() {
        return 0;
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
    
    public boolean isAllZonesCooling() {
        for (ZoneProfile p: L.ccu().zoneProfiles)
        {
            System.out.println(" Zone State " +p.state);
            if (p.getState() != ZoneState.COOLING) {
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
            if (p.getEquip().getRoomRef().equals(zoneRef) && p.getCurrentTemp() > 0) {
                tempSum += p.getCurrentTemp();
                tempCount++;
            }
        }
        return tempCount > 0 ? tempSum/tempCount : 0;
    }
    
    public int getZonePriority(String zoneRef) {
        ZonePriority priority = NONE;
        for (ZoneProfile p : L.ccu().zoneProfiles)
        {
            if (p.getEquip().getRoomRef().equals(zoneRef) && p.getPriority().ordinal() > priority.ordinal()) {
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
        return systemState;
    }
}
