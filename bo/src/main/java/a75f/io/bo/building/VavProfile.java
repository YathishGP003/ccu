package a75f.io.bo.building;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.TrimResetListener;
import a75.io.algos.TrimResponseRequest;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.hvac.Damper;
import a75f.io.bo.building.hvac.Valve;
import a75f.io.bo.building.hvac.VavUnit;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

import static a75f.io.bo.building.VavProfile.ZonePriority.LOW;
import static a75f.io.bo.building.VavProfile.ZoneState.COOLING;
import static a75f.io.bo.building.VavProfile.ZoneState.DEADBAND;
import static a75f.io.bo.building.VavProfile.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 5/31/18.
 */

public class VavProfile extends ZoneProfile implements TrimResetListener
{
    
    public static final int MAX_DISCHARGE_TEMP = 90;
    public static final int HEATING_LOOP_OFFSET = 20;
    public static final int REHEAT_THRESHOLD_TEMP = 50;
    ControlLoop coolingLoop;
    ControlLoop heatingLoop;
    ControlLoop damperLoop;
    
    double  setTemp = 72.0; //TODO
    double  zoneTemp;
    int deadBand = 1;
    
    VavUnit vavReheatUnit;
    
    double dischargeTemp;
    double supplyAirTemp;
    
    int minValvePosition = 40; //TODO - Tuners
    int    maxValvePosition = 80;
    int    integralMaxTimeout = 15;
    int proportionalSpread = 5;
    double proportionalGain = 0.5;
    double integralGain = 0.5;
    
    int unmodDamperOp = 0;
    
    enum ZoneState {
        COOLING,
        HEATING,
        DEADBAND
    }
    
    enum ZonePriority {
        NO(0), LOW(1), MEDIUM(2), HIGH(3);
        
        int multiplier;
        ZonePriority(int m) {
            multiplier = m;
        }
    }
    

    ZoneState state = COOLING;
    ZonePriority priority = LOW;
    TrimResponseRequest satResetRequest;
    
    GenericPIController valveController;// Use GenericPI as we need unmodulated op.
    
    
    public VavProfile() {
        coolingLoop = new ControlLoop();
        heatingLoop = new ControlLoop();
        damperLoop = new ControlLoop();
        vavReheatUnit = new VavUnit();
        satResetRequest = new TrimResponseRequest(priority.multiplier);
    }
    
    @Override
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
        zoneTemp = (float) regularUpdateMessage.update.roomTemperature.get() / 10.0f;// TODO - temp
        dischargeTemp = 70;//(float) regularUpdateMessage.update.airflow1Temperature.get() / 10.0f;
        supplyAirTemp = 60;//(float) regularUpdateMessage.update.airflow2Temperature.get() / 10.0f;
    
        if(mInterface != null)
        {
            mInterface.refreshView();
        }
    }
    
    //VAV damper and reheat coil control logic is implemented according to section 1.3.E.6 of
    //ASHRAE RP-1455: Advanced Control Sequences for HVAC Systems Phase I, Air Distribution and Terminal Systems
    @JsonIgnore
    public VavUnit getVavControls(double desiredTemp) {
        
        if (zoneTemp == 0 || desiredTemp == 0) {
            Log.d("VAV","Skip PI update : setTemp: "+desiredTemp+" zoneTemp :"+zoneTemp);
            return null;
        }
        
        setTemp = desiredTemp;
    
        Damper damper = vavReheatUnit.vavDamper;
        Valve valve = vavReheatUnit.reheatValve;
    
        int loopOp;
        
        Log.d("VAV","setTemp: "+setTemp+", roomTemp: "+zoneTemp);
    
        //TODO -
        //If supply air temperature from air handler is greater than room temperature, Cooling shall be
        //locked out.
    
        if (zoneTemp > (setTemp + deadBand)) {
            //Zone is in Cooling
            if (state != COOLING) {
                state = COOLING;
                coolingLoop.setEnabled();
                heatingLoop.setDisabled();
                damperLoop.reset();
                Log.d("VAV"," Enable cooling Loop & Disable heating Loop");
            }
            int coolingOp = (int) coolingLoop.getLoopOutput(zoneTemp, setTemp);
            loopOp =  coolingOp;
            valveController = null;
            Log.d("VAV"," Zone in Cooling coolingOp : "+coolingOp+" damperSP :"+loopOp);
        
        } else if (zoneTemp < (setTemp - deadBand)){
            //Zone is in heating
            if (state != HEATING) {
                state = HEATING;
                heatingLoop.setEnabled();
                coolingLoop.setDisabled();
                damperLoop.reset();
                valveController = new GenericPIController();
                valveController.setIntegralMaxTimeout(integralMaxTimeout);
                valveController.setMaxAllowedError(proportionalSpread);
                valveController.setProportionalGain(proportionalGain);
                valveController.setIntegralGain(integralGain);
                Log.d("VAV"," Enable heating Loop & Disable Cooling Loop");
            }
            
            //Control discharge temp when heating loop is <= 50
            int heatingLoopOp = (int)heatingLoop.getLoopOutput(setTemp, zoneTemp);
            if ( heatingLoopOp <= 50) {
                double reheatSp = (zoneTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : zoneTemp + HEATING_LOOP_OFFSET;
                valveController.updateControlVariable(reheatSp, dischargeTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
    
                loopOp = damper.minPosition;
                Log.d("VAV"," Zone in Heating reheatSp : "+reheatSp+"   valve :"+valve.currentPosition+" loopOp "+loopOp);
            } else {
                //Control airflow when heating loop is 51-100
    
                double reheatSp = (zoneTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : zoneTemp + HEATING_LOOP_OFFSET;
                valveController.updateControlVariable(reheatSp, dischargeTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                
                loopOp = heatingLoopOp;
                Log.d("VAV"," Zone in Heating heatingOp : "+heatingLoopOp+"   damperSP :"+loopOp);
                
            }
            
        
        } else {
            //Zone is in deadband
            state = DEADBAND;
            loopOp = damper.minPosition;
            valveController = null;
            heatingLoop.setDisabled();
            coolingLoop.setDisabled();
            Log.d("VAV", "In deadband damper "+loopOp);
        }
        
        if (valveController == null) {
            valve.currentPosition = 0;
        }
        
        if (loopOp == 0) {
            damper.currentPosition = damper.minPosition;
        }
        else {
            unmodDamperOp = (int) damperLoop.getLoopOutput(loopOp, 0);
            damper.currentPosition = damper.minPosition + (damper.maxPosition - damper.minPosition) * unmodDamperOp / 100;
        }
        
        Log.d("VAV", "unmodDamperOp "+unmodDamperOp+" currentPos :"+damper.currentPosition);
        
        
        //In any Mode except Unoccupied, the hot water valve shall be
        //modulated to maintain a supply air temperature no lower than 50°F.
        if (state != HEATING && supplyAirTemp < REHEAT_THRESHOLD_TEMP/* && mode != UNOCCUPIED*/) {
            if (valveController == null) {
                valveController = new GenericPIController();
                valveController.setIntegralMaxTimeout(integralMaxTimeout);
                valveController.setMaxAllowedError(proportionalSpread);
                valveController.setProportionalGain(proportionalGain);
                valveController.setIntegralGain(integralGain);
            }
            valveController.updateControlVariable(REHEAT_THRESHOLD_TEMP, supplyAirTemp);
            valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
    
            Log.d("VAV", "SAT below threshold valve :  "+valve.currentPosition);
        }
        
        return vavReheatUnit;
    }
    
    @JsonIgnore
    public VavUnit getVavControls() {
        return vavReheatUnit;
    }
    
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV;
    }
    
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return mProfileConfiguration.get(address);
    }
    
    @JsonIgnore
    public TrimResponseRequest getSATRequest() {
        
        if (state == COOLING) {
            if (coolingLoop.getLoopOutput() < 85) {
                satResetRequest.currentRequests = 0;
            }
            if (coolingLoop.getLoopOutput() > 95) {
                satResetRequest.currentRequests = 1;
            }
            if ((zoneTemp - setTemp) >= 3) {//TODO - for 2 mins
                satResetRequest.currentRequests = 2;
            }
            if ((zoneTemp - setTemp) >= 5) {//TODO - for 5 mins
                satResetRequest.currentRequests = 3;
            }
        } else {
            satResetRequest.currentRequests = 0;
        }
        satResetRequest.handleRequestUpdate();
        return satResetRequest;
    }
    
    @JsonIgnore
    public int getHWSTRequests() {
        int requestCount = 0;
        switch (state) {
            case HEATING:
                if (vavReheatUnit.reheatValve.currentPosition > 95) {
                    requestCount = 1;
                }
                if ((setTemp - supplyAirTemp) >=  15) {//TODO - for 2 mins
                    requestCount = 2;
                }
                if ((setTemp - supplyAirTemp) >=  30) {//TODO - for 2 mins
                    requestCount = 3;
                }
                break;
        }
        
        return requestCount;
    }
    
    
    @JsonIgnore
    public int getConditioningMode() {
        return state.ordinal();
    }
    
    @JsonIgnore
    public int getImportanceMultiplier() {
        return priority.multiplier;
    }
    
    @JsonIgnore
    public void handleSystemReset() {
        Log.d("VAV","handleSystemReset");
        satResetRequest.handleReset();
    }
    
    @JsonIgnore
    public double getDisplayCurrentTemp()
    {
        return zoneTemp;
    }
}
