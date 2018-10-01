package a75f.io.logic.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;

import static a75f.io.logic.bo.building.vav.VavProfile.ZoneState.COOLING;
import static a75f.io.logic.bo.building.vav.VavProfile.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.vav.VavProfile.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavParallelFanProfile extends VavProfile
{
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_PARALLEL_FAN;
    }
    
    @JsonIgnore
    @Override
    public void updateZonePoints() {
        Log.d(TAG, "VAV Parallel Fan Control");
    
        setTemp = 72.0;
    
        for (short node : getNodeAddresses())
        {
            if (vavDeviceMap.get(node) == null) {
                addLogicalMap(node);
                Log.d(TAG, " Logical Map added for node " + node);
                continue;
            }
            VAVLogicalMap vavDevice = vavDeviceMap.get(node);
            ControlLoop coolingLoop = vavDevice.getCoolingLoop();
            ControlLoop heatingLoop = vavDevice.getHeatingLoop();
            CO2Loop co2Loop = vavDeviceMap.get(node).getCo2Loop();
            ParallelFanVavUnit vavUnit = (ParallelFanVavUnit) vavDevice.getVavUnit();
            GenericPIController valveController = vavDevice.getValveController();
        
            double roomTemp = vavDevice.getRoomTemp();
            double dischargeTemp = vavDevice.getDischargeTemp();
            double supplyAirTemp = vavDevice.getSupplyAirTemp();
            double co2 = vavDeviceMap.get(node).getCO2();
            double dischargeSp = vavDevice.getDischargeSp();
        
            if (roomTemp == 0) {
                Log.d(TAG,"Skip PI update for "+node+" roomTemp : "+roomTemp);
                continue;
            }
        
        
            Damper damper = vavUnit.vavDamper;
            Valve valve = vavUnit.reheatValve;
            int loopOp;//New value of loopOp
            //TODO
            //If supply air temperature from air handler is greater than room temperature, Cooling shall be
            //locked out.
            if (roomTemp > (setTemp + deadBand))
            {
                //Zone is in Cooling
                if (state != COOLING)
                {
                    state = COOLING;
                    valveController.reset();
                    coolingLoop.setEnabled();
                    heatingLoop.setDisabled();
                }
                int coolingOp = (int) coolingLoop.getLoopOutput(roomTemp, setTemp+deadBand);
                loopOp = coolingOp;
            
            }
            else if (roomTemp < (setTemp - deadBand))
            {
                //Zone is in heating
                if (state != HEATING)
                {
                    state = HEATING;
                    heatingLoop.setEnabled();
                    coolingLoop.setDisabled();
                }
            
                int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTemp-deadBand, roomTemp);
                dischargeSp = supplyAirTemp + (MAX_DISCHARGE_TEMP - supplyAirTemp) * heatingLoopOp/100;
                vavDevice.setDischargeSp(dischargeSp);
                valveController.updateControlVariable(dischargeSp, dischargeTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                loopOp = heatingLoopOp;
            }
            else
            {
                //Zone is in deadband
                if (state != DEADBAND)
                {
                    state = DEADBAND;
                    valveController.reset();
                    heatingLoop.setDisabled();
                    coolingLoop.setDisabled();
                }
            
                loopOp = 0;
            }
        
            if (valveController.getControlVariable() == 0)
            {
                valve.currentPosition = 0;
            }
        
            setDamperLimits(node, damper);
        
            //CO2 loop output from 0-50% modulates damper min position.
            if (/*mode == OCCUPIED && */co2Loop.getLoopOutput(co2) <= 50)
            {
                //When HEATING , maxPosition = maxPosition - parallel fan factor.
                int parallelFanFactor = 0 ;//TODO - Tuner
                int maxDamper = damper.maxPosition - parallelFanFactor;
                damper.co2CompensatedMinPos = damper.minPosition + ( maxDamper - damper.minPosition) * co2Loop.getLoopOutput() / 50;
                Log.d("VAV","CO2LoopOp :"+co2Loop.getLoopOutput()+", adjusted minposition "+damper.minPosition);
            }
        
            if (loopOp == 0)
            {
                damper.currentPosition = damper.co2CompensatedMinPos;
            }
            else
            {
                damper.currentPosition = damper.co2CompensatedMinPos + (damper.maxPosition - damper.co2CompensatedMinPos) * loopOp / 100;
            }
    
            int OAminDamper = 0 ;//TODO - Tuner
            if (state == HEATING) {
                vavUnit.fanStart = true;
            } else if(true/* ventillation ==  California_64*/) {
                //Fan shall run in SDeadband and Cooling when the primary supply air volume is less than OA-min for one minute,
                //and shall shut off when primary air volume is above OA-min by 10% for 3 minutes.
                vavUnit.fanStart = false;
            } else {
                vavUnit.fanStart = false;
            }
        
            //Normalize
            damper.normalize();
            valve.normalize();
    
            Log.d(TAG, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
                                                    +", valve:"+valve.currentPosition+" fanStart: "+vavUnit.fanStart);
    
            updateTRResponse(node);
        }
    }
}
