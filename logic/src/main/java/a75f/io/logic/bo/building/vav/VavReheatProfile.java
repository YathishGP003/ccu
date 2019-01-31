package a75f.io.logic.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavReheatProfile extends VavProfile
{
  
    private boolean satCompensationEnabled = false;
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
    public void updateZonePoints() {
        
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
            if (vavDeviceMap.get(node).getCurrentTemp() == 0) {
                Log.d(TAG,"Invalid Temp , skip controls update for "+node+" roomTemp : "+vavDeviceMap.get(node).getCurrentTemp());
                continue;
            }
            VAVLogicalMap vavDevice = vavDeviceMap.get(node);
            ControlLoop coolingLoop = vavDevice.getCoolingLoop();
            ControlLoop heatingLoop = vavDevice.getHeatingLoop();
            CO2Loop co2Loop = vavDeviceMap.get(node).getCo2Loop();
            VOCLoop vocLoop = vavDeviceMap.get(node).getVOCLoop();
            VavUnit vavUnit = vavDevice.getVavUnit();
            GenericPIController valveController = vavDevice.getValveController();
    
            double roomTemp = vavDevice.getCurrentTemp();
            double dischargeTemp = vavDevice.getDischargeTemp();
            double supplyAirTemp = vavDevice.getSupplyAirTemp();
            double co2 = vavDeviceMap.get(node).getCO2();
            double voc = vavDeviceMap.get(node).getVOC();
            double dischargeSp = vavDevice.getDischargeSp();
            setTemp = vavDevice.getDesiredTemp();
            
            Damper damper = vavUnit.vavDamper;
            Valve valve = vavUnit.reheatValve;
    
            Equip vavEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
            int loopOp = 0;//New value of loopOp
            //TODO
            //If supply air temperature from air handler is greater than room temperature, Cooling shall be
            //locked out.
            if (roomTemp > (setTemp + VavTunerUtil.getCoolingDeadband(vavEquip.getId())))
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
            else if (roomTemp < (setTemp - VavTunerUtil.getHeatingDeadband(vavEquip.getId())))
            {
                //Zone is in heating
                if (state != HEATING)
                {
                    state = HEATING;
                    heatingLoop.setEnabled();
                    coolingLoop.setDisabled();
                }
                int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTemp - deadBand, roomTemp);
                if (VavSystemController.getInstance().getSystemState() == VavSystemController.State.COOLING)
                {
                    if (heatingLoopOp <= 50)
                    {
                        //Control reheat valve when heating loop is <=50
                        double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                        dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp / 50;
                        vavDevice.setDischargeSp(dischargeSp);
                        valveController.updateControlVariable(dischargeSp, dischargeTemp);
                        valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                        loopOp = 0;
                        Log.d(TAG, "dischargeTempSP: " + dischargeSp);
                    }
                    else
                    {
                        //Control airflow when heating loop is 51-100
                        //Also update valve control to account for change in dischargeTemp
                        if (dischargeSp == 0)
                        {
                            double datMax = (roomTemp + HEATING_LOOP_OFFSET) > MAX_DISCHARGE_TEMP ? MAX_DISCHARGE_TEMP : (roomTemp + HEATING_LOOP_OFFSET);
                            dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * heatingLoopOp / 100;
                            vavDevice.setDischargeSp(dischargeSp);
                        }
                        valveController.updateControlVariable(dischargeSp, dischargeTemp);
                        valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                        loopOp = heatingLoopOp;
                    }
                } else
                {
                    loopOp = heatingLoopOp;
                }
            }
            else
            {
                //Zone is in deadband
                if (state != DEADBAND) {
                    state = DEADBAND;
                    //valveController.reset();
                    valve.currentPosition = 0;
                    heatingLoop.setDisabled();
                    coolingLoop.setDisabled();
                    
                }
                loopOp = 0;
            }
            
            if (valveController.getControlVariable() == 0)
            {
                valve.currentPosition = 0;
            }
            
            //setDamperLimits(node, damper);
            
            //CO2 loop output from 0-50% modulates damper min position.
            if (/*mode == OCCUPIED && */co2Loop.getLoopOutput(co2) <= 50)
            {
                damper.iaqCompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * co2Loop.getLoopOutput() / 50;
                Log.d("VAV","CO2LoopOp :"+co2Loop.getLoopOutput()+", adjusted minposition "+damper.minPosition);
            }
    
            //VOC loop output from 0-50% modulates damper min position.
            if (/*mode == OCCUPIED && */vocLoop.getLoopOutput(voc) <= 50)
            {
                damper.iaqCompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * vocLoop.getLoopOutput() / 50;
                Log.d("VAV","VOCLoopOp :"+vocLoop.getLoopOutput()+", adjusted minposition "+damper.minPosition);
            }
            
            if (loopOp == 0)
            {
                damper.currentPosition = damper.iaqCompensatedMinPos;
            }
            else
            {
                damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
            }
            //In any Mode except Unoccupied, the hot water valve shall be
            //modulated to maintain a supply air temperature no lower than 50°F.
            /*if (state != HEATING && supplyAirTemp < REHEAT_THRESHOLD_TEMP*//* && mode != UNOCCUPIED*//*)
            {
                satCompensationEnabled = true;
                valveController.updateControlVariable(REHEAT_THRESHOLD_TEMP, supplyAirTemp);
                valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                Log.d(TAG, "SAT below threshold "+supplyAirTemp+" => valve :  " + valve.currentPosition);
            } else if (satCompensationEnabled) {
                satCompensationEnabled = false;
                valveController.reset();
            }*/
    
            //REHEAT control during heating does not follow RP1455.
            if (VavSystemController.getInstance().getSystemState() == VavSystemController.State.HEATING
                                && state == HEATING)
            {
                double valveStartDamperPercent = TunerUtil.readTunerValByQuery("vav and valve and start and damper and equipRef == \""+vavEquip.getId()+"\"");
                double maxHeatingPos = vavDevice.getDamperLimit("heating", "max");
                double minHeatingPos = vavDevice.getDamperLimit("heating", "min");
                double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
                Log.d("VAV"," valveStartDamperPercent "+valveStartDamperPercent+" valveStart "+valveStart );
                if (damper.currentPosition > valveStart)
                {
                    valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
                }
                else
                {
                    valve.currentPosition = 0;
                }
            }
            
            Log.d("VAV","CoolingLoop "+node +" roomTemp :"+roomTemp+" setTemp: "+setTemp);
            coolingLoop.dump();
            Log.d("VAV","HeatingLoop "+node +" roomTemp :"+roomTemp+" setTemp: "+setTemp);
            heatingLoop.dump();
            
    
            Log.d(TAG, "STATE :"+state+" ZoneState : "+getState()+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition+", valve:"+valve.currentPosition);
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
