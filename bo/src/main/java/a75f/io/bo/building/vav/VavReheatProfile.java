package a75f.io.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.hvac.Damper;
import a75f.io.bo.building.hvac.Valve;
import a75f.io.bo.building.hvac.VavUnit;

import static a75f.io.bo.building.vav.VavProfile.ZoneState.COOLING;
import static a75f.io.bo.building.vav.VavProfile.ZoneState.DEADBAND;
import static a75f.io.bo.building.vav.VavProfile.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavReheatProfile extends VavProfile
{
    
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_REHEAT;
    }
    
    //VAV damper and reheat coil control logic is implemented according to section 1.3.E.6 of
    //ASHRAE RP-1455: Advanced Control Sequences for HVAC Systems Phase I, Air Distribution and Terminal Systems
    @JsonIgnore
    @Override
    public void updateZoneControls(double desiredTemp) {
        
        setTemp = desiredTemp;
        
        for (short node : getNodeAddresses())
        {
            if (vavDeviceMap.get(node) == null) {
                Log.d(TAG, " Logical Map does not exist for node " + node);
                continue;
            }
            VAVLogicalMap vavDevice = vavDeviceMap.get(node);
            ControlLoop coolingLoop = vavDevice.getCoolingLoop();
            ControlLoop heatingLoop = vavDevice.getHeatingLoop();
            CO2Loop co2Loop = vavDeviceMap.get(node).getCo2Loop();
            VavUnit vavUnit = vavDevice.getVavUnit();
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
                    coolingLoop.setEnabled();
                    heatingLoop.setDisabled();
                }
                int coolingOp = (int) coolingLoop.getLoopOutput(roomTemp, setTemp+deadBand);
                loopOp = coolingOp;
                valveController.reset();
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
                
                if (heatingLoopOp <= 50)
                {
                    //Control reheat valve when heating loop is <=50
                    double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                    dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp/50;
                    vavDevice.setDischargeSp(dischargeSp);
                    valveController.updateControlVariable(dischargeSp, dischargeTemp);
                    valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                    loopOp = 0;
                    Log.d(TAG,"dischargeTempSP: "+dischargeSp);
                }
                else
                {
                    //Control airflow when heating loop is 51-100
                    //Also update valve control to account for change in dischargeTemp
                    if (dischargeSp == 0) {
                        double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                        dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp/100;
                        vavDevice.setDischargeSp(dischargeSp);
                    }
                    valveController.updateControlVariable(dischargeSp, dischargeTemp);
                    valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                    loopOp = heatingLoopOp;
                }
                
            }
            else
            {
                //Zone is in deadband
                state = DEADBAND;
                loopOp = 0;
                valveController.reset();
                heatingLoop.setDisabled();
                coolingLoop.setDisabled();
            }
            
            if (valveController.getControlVariable() == 0)
            {
                valve.currentPosition = 0;
            }
            
            setDamperLimits(node, damper);
            
            //CO2 loop output from 0-50% modulates damper min position.
            if (/*mode == OCCUPIED && */co2Loop.getLoopOutput(co2) <= 50)
            {
                damper.co2CompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * co2Loop.getLoopOutput() / 50;
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
            Log.d(TAG, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition+", valve:"+valve.currentPosition);
            
            //In any Mode except Unoccupied, the hot water valve shall be
            //modulated to maintain a supply air temperature no lower than 50°F.
            if (state != HEATING && supplyAirTemp < REHEAT_THRESHOLD_TEMP/* && mode != UNOCCUPIED*/)
            {
                valveController.updateControlVariable(REHEAT_THRESHOLD_TEMP, supplyAirTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                Log.d(TAG, "SAT below threshold valve :  " + valve.currentPosition);
            }
            
            //Normalize
            damper.normalize();
            valve.normalize();
        }
    }
}
