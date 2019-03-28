package a75f.io.logic.bo.building.vav;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 8/23/18.
 *
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
        CcuLog.d(L.TAG_CCU_ZONE, "VAV Parallel Fan Control");
        if(mInterface != null)
        {
            mInterface.refreshView();
        }
    
        for (short node : vavDeviceMap.keySet())
        {
            if (vavDeviceMap.get(node) == null) {
                addLogicalMap(node);
                CcuLog.d(L.TAG_CCU_ZONE, " Logical Map added for node " + node);
                continue;
            }
            VAVLogicalMap vavDevice = vavDeviceMap.get(node);
            ControlLoop coolingLoop = vavDevice.getCoolingLoop();
            ControlLoop heatingLoop = vavDevice.getHeatingLoop();
            CO2Loop co2Loop = vavDeviceMap.get(node).getCo2Loop();
            ParallelFanVavUnit vavUnit = (ParallelFanVavUnit) vavDevice.getVavUnit();
            GenericPIController valveController = vavDevice.getValveController();
        
            double roomTemp = vavDevice.getCurrentTemp();
            double dischargeTemp = vavDevice.getDischargeTemp();
            double supplyAirTemp = vavDevice.getSupplyAirTemp();
            double co2 = vavDeviceMap.get(node).getCO2();
            double voc = vavDeviceMap.get(node).getVOC();
            double dischargeSp = vavDevice.getDischargeSp();
            setTempCooling = vavDevice.getDesiredTempCooling();
            setTempHeating = vavDevice.getDesiredTempHeating();
            vavDevice.setDesiredTemp((setTempCooling+setTempHeating)/2);
            Equip vavEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
    
            VOCLoop vocLoop = vavDeviceMap.get(node).getVOCLoop();
            if (roomTemp == 0) {
                CcuLog.d(L.TAG_CCU_ZONE,"Skip PI update for "+node+" roomTemp : "+roomTemp);
                continue;
            }
            
            Damper damper = vavUnit.vavDamper;
            Valve valve = vavUnit.reheatValve;
            setDamperLimits(node, damper);
            int loopOp;//New value of loopOp
            //TODO
            //If supply air temperature from air handler is greater than room temperature, Cooling shall be
            //locked out.
            if (roomTemp > setTempCooling)
            {
                //Zone is in Cooling
                if (state != COOLING)
                {
                    state = COOLING;
                    //valveController.reset();
                    coolingLoop.setEnabled();
                    heatingLoop.setDisabled();
                }
                int coolingOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
                loopOp = coolingOp;
            
            }
            else if (roomTemp < setTempHeating)
            {
                //Zone is in heating
                if (state != HEATING)
                {
                    state = HEATING;
                    heatingLoop.setEnabled();
                    coolingLoop.setDisabled();
                }
            
                int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
                if (VavSystemController.getInstance().getSystemState() == VavSystemController.State.COOLING)
                {
                    dischargeSp = supplyAirTemp + (MAX_DISCHARGE_TEMP - supplyAirTemp) * heatingLoopOp / 100;
                    vavDevice.setDischargeSp(dischargeSp);
                    valveController.updateControlVariable(dischargeSp, dischargeTemp);
                    valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
                }
    
                
                loopOp = heatingLoopOp;
            }
            else
            {
                //Zone is in deadband
                if (state != DEADBAND)
                {
                    state = DEADBAND;
                    valveController.reset();
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
    
            boolean  enabledCO2Control = vavDevice.getConfigNumVal("enable and co2") > 0 ;
            boolean  enabledIAQControl = vavDevice.getConfigNumVal("enable and iaq") > 0 ;
            String zoneId = HSUtil.getZoneIdFromEquipId(vavEquip.getId());
            Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
            boolean occupied = (occ == null ? false : occ.isOccupied());
            
            //CO2 loop output from 0-50% modulates damper min position.
            if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) <= 50)
            {
                //When HEATING , maxPosition = maxPosition - parallel fan factor.
                int parallelFanFactor = 0 ;//TODO - Tuner
                int maxDamper = damper.maxPosition - parallelFanFactor;
                damper.iaqCompensatedMinPos = damper.minPosition + ( maxDamper - damper.minPosition) * co2Loop.getLoopOutput() / 50;
                CcuLog.d(L.TAG_CCU_ZONE,"CO2LoopOp :"+co2Loop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos);
            }
    
            //VOC loop output from 0-50% modulates damper min position.
            if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) <= 50)
            {
                damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * vocLoop.getLoopOutput() / 50;
                CcuLog.d(L.TAG_CCU_ZONE, "VOCLoopOp :" + vocLoop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
            }
        
            if (loopOp == 0)
            {
                damper.currentPosition = damper.iaqCompensatedMinPos;
            }
            else
            {
                damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
            }
    
            //REHEAT control that does not follow RP-1455.
            if (VavSystemController.getInstance().getSystemState() == VavSystemController.State.HEATING &&
                                                        state == HEATING)
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
    
            valve.currentPosition = Math.max(valve.currentPosition, 0);
            valve.currentPosition = Math.min(valve.currentPosition, 100);
    
            CcuLog.d(L.TAG_CCU_ZONE,"CoolingLoop "+node +"roomTemp :"+roomTemp+" setTempHeating: "+setTempCooling);
            coolingLoop.dump();
            CcuLog.d(L.TAG_CCU_ZONE,"HeatingLoop "+node +"roomTemp :"+roomTemp+" setTempCooling: "+setTempHeating);
            heatingLoop.dump();
    
            CcuLog.d(L.TAG_CCU_ZONE, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
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
