package a75f.io.logic.bo.building.system;

import android.util.Log;

import com.google.common.collect.EvictingQueue;

import a75.io.algos.ControlLoop;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.tuners.SystemTunerUtil;

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
    
    enum State {NA, COOLING, HEATING};
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
        
        for (Floor f: L.ccu().getFloors())
        {
            for(Zone z: f.mZoneList) {
                if (z.getZoneCurrentTemp() == 0) {
                    continue;
                }
                double zone_dxCI = z.getZoneCurrentTemp() - z.getZoneDesiredTemp();
                double zone_dp = z.getDynamicPriority(ciDesired);
    
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
            Log.d("CCU", "runDxCIAlgo-> dxCIMABuffer: "+val);
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
        
        for (Floor f: L.ccu().getFloors())
        {
            for(Zone z: f.mZoneList) {
                if (z.getZoneCurrentTemp() == 0) {
                    continue;
                }
                double zone_dxCI = z.getZoneCurrentTemp() - z.getZoneTempTarget();
                double zone_dp = z.getDynamicPriority(ciDesired);
            
                dxCISum += zone_dxCI * zone_dp;
                Log.d("CCU", "WA zone_dxCI: "+zone_dxCI+" zone_dp: "+zone_dp+" dxCISum: "+dxCISum);
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
        return coolingSignal;
    }
    
    public boolean isAllZonesHeating() {
        for (Floor f: L.ccu().getFloors())
        {
            for(Zone z: f.mZoneList) {
                for (ZoneProfile p: z.mZoneProfiles) {
                    if (p.state != ZoneState.HEATING) {
                        return false;
                    }
                }
            }
        
        }
        return true;
    }
    
    public boolean isAllZonesCooling() {
        for (Floor f: L.ccu().getFloors())
        {
            for(Zone z: f.mZoneList) {
                for (ZoneProfile p: z.mZoneProfiles) {
                    if (p.state != ZoneState.COOLING) {
                        return false;
                    }
                }
            }
            
        }
        return true;
    }
    
    public int getHeatingSignal() {
        return heatingSignal;
    }
    
}
