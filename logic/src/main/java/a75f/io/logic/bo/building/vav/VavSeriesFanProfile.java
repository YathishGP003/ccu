package a75f.io.logic.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

//@startuml
public class VavSeriesFanProfile extends VavProfile
{
    
    private boolean fanReady = false;
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_SERIES_FAN;
    }
    
    @JsonIgnore
    @Override
    public void updateZonePoints() {
        Log.d(TAG, "VAV Series Fan Control");
        
        if(mInterface != null)
        {
            mInterface.refreshView();
        }
        
        for (short node : vavDeviceMap.keySet())
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
            SeriesFanVavUnit vavUnit = (SeriesFanVavUnit)vavDevice.getVavUnit();
            //GenericPIController valveController = vavDevice.getValveController();
        
            double roomTemp = vavDevice.getCurrentTemp();
            double dischargeTemp = vavDevice.getDischargeTemp();
            double supplyAirTemp = vavDevice.getSupplyAirTemp();
            double co2 = vavDeviceMap.get(node).getCO2();
            double dischargeSp = vavDevice.getDischargeSp();
            setTemp = vavDevice.getDesiredTemp();
            Equip vavEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
    
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
                    //valveController.reset();
                    valve.currentPosition = 0;
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
                /*dischargeSp = supplyAirTemp + (MAX_DISCHARGE_TEMP - supplyAirTemp) * heatingLoopOp/100;
                vavDevice.setDischargeSp(dischargeSp);
                valveController.updateControlVariable(dischargeSp, dischargeTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());*/
                
                //REHEAT control that does not follow RP-1455.
                loopOp = heatingLoopOp;
            }
            else
            {
                //Zone is in deadband
                if (state != DEADBAND)
                {
                    state = DEADBAND;
                    //valveController.reset();
                    valve.currentPosition = 0;
                    heatingLoop.setDisabled();
                    coolingLoop.setDisabled();
                }
                
                loopOp = 0;
            }
        
            /*if (valveController.getControlVariable() == 0)
            {
                valve.currentPosition = 0;
            }*/
            
            if (!damper.isOverrideActive())
            {
                setDamperLimits(node, damper);
                //CO2 loop output from 0-50% modulates damper min position.
                if (/*mode == OCCUPIED && */co2Loop.getLoopOutput(co2) <= 50)
                {
                    damper.co2CompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * co2Loop.getLoopOutput() / 50;
                    Log.d("VAV", "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.minPosition);
                }
                if (loopOp == 0)
                {
                    damper.currentPosition = damper.co2CompensatedMinPos;
                }
                else
                {
                    damper.currentPosition = damper.co2CompensatedMinPos + (damper.maxPosition - damper.co2CompensatedMinPos) * loopOp / 100;
                }
                //Normalize
                damper.normalize();
            }
            
            //REHEAT control that does not follow RP-1455.
            if (state == HEATING)
            {
                double valveStartDamperPercent = TunerUtil.readTunerValByQuery("vav and valve and start and damper and equipRef == \""+vavEquip.getId()+"\"");
                double maxHeatingPos = vavDevice.getDamperLimit("heating", "max");
                double minHeatingPos = vavDevice.getDamperLimit("heating", "min");
                double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
                if (damper.currentPosition > valveStart)
                {
                    valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
                }
                else
                {
                    valve.currentPosition = 0;
                }
            }
            
            
            if (true /* mode == OCCUPIED*/) {
                //Prior to starting the fan, the damper is first driven fully closed to ensure that the fan is not rotating backwards.
                //Once the fan is proven on for a fixed time delay (15 seconds), the damper override is released
    
                if (fanReady == true && vavUnit.fanStart) {
                    if (damper.isOverrideActive())
                    {
                        damper.releaseOverride();
                    }
                }
                
                if (fanReady) {
                    vavUnit.fanStart = true;
                    vavUnit.fanSpeed = 100;
                }
                
                if (vavUnit.fanStart && damper.isOverrideActive() == false) {
                    damper.applyOverride(0);
                    fanReady = true;
                }
                
            } else {
                vavUnit.fanStart = false;
                vavUnit.fanSpeed = 0;
                fanReady = false;
            }
    
            Log.d("VAV","CoolingLoop "+node +"roomTemp :"+roomTemp+" setTemp: "+setTemp);
            coolingLoop.dump();
            Log.d("VAV","HeatingLoop "+node +"roomTemp :"+roomTemp+" setTemp: "+setTemp);
            heatingLoop.dump();
            
            Log.d(TAG, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
                                                        +", valve:"+valve.currentPosition+" fanStart: "+vavUnit.fanStart);
            updateTRResponse(node);
            vavDevice.setDamperPos(damper.currentPosition);
            vavDevice.setReheatPos(valve.currentPosition);
            vavDevice.updateLoopParams();
        }
    }
    
    @Override
    public ZoneState getState() {
        return state;
    }
    
}
//@enduml
