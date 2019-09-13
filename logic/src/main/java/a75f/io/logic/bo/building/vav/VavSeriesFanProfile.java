package a75f.io.logic.bo.building.vav;

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
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMP_DEAD;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

//@startuml
public class VavSeriesFanProfile extends VavProfile
{
    
    private boolean fanReady = false;
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_SERIES_FAN;
    }
    
    @Override
    public void updateZonePoints() {
        CcuLog.d(L.TAG_CCU_ZONE, "VAV Series Fan Control");
        
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
            VOCLoop vocLoop = vavDeviceMap.get(node).getVOCLoop();
            SeriesFanVavUnit vavUnit = (SeriesFanVavUnit)vavDevice.getVavUnit();
            GenericPIController valveController = vavDevice.getValveController();
        
            double roomTemp = vavDevice.getCurrentTemp();
            double dischargeTemp = vavDevice.getDischargeTemp();
            double supplyAirTemp = vavDevice.getSupplyAirTemp();
            double co2 = vavDeviceMap.get(node).getCO2();
            double voc = vavDeviceMap.get(node).getVOC();
            double dischargeSp = vavDevice.getDischargeSp();
            setTempCooling = vavDevice.getDesiredTempCooling();
            setTempHeating = vavDevice.getDesiredTempHeating();
            double averageDesiredTemp = (setTempCooling+setTempHeating)/2;
            if (averageDesiredTemp != vavDevice.getDesiredTemp())
            {
                vavDevice.setDesiredTemp(averageDesiredTemp);
            }
            Equip vavEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
    
            if (isZoneDead()) {
                CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead : "+node+" roomTemp : "+vavDeviceMap.get(node).getCurrentTemp());
                state = TEMP_DEAD;
                String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+node+"\"");
                if (!curStatus.equals("Zone Temp Dead"))
                {
                    CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
                    vavDevice = vavDeviceMap.get(node);
                    double damperMin = vavDevice.getDamperLimit(state == HEATING ? "heating":"cooling", "min");
                    double damperMax = vavDevice.getDamperLimit(state == HEATING ? "heating":"cooling", "max");
                    double damperPos = (L.ccu().systemProfile.getSystemController().getSystemState() == SystemController.State.OFF) ? damperMin : (damperMax+damperMin)/2;
                    vavDevice.setDamperPos(damperPos);
                    vavDevice.setNormalizedDamperPos(damperPos);
                    vavDevice.setReheatPos(0);
                    CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + node + "\"", (double)TEMP_DEAD.ordinal());
                }
                continue;
            }
            
            Damper damper = vavUnit.vavDamper;
            Valve valve = vavUnit.reheatValve;
            setDamperLimits(node, damper);
            int loopOp = 0;
            SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
            //If supply air temperature from air handler is greater than room temperature, Cooling shall be
            //locked out.
            if (roomTemp > setTempCooling)
            {
                //Zone is in Cooling
                if (state != COOLING)
                {
                    state = COOLING;
                    valveController.reset();
                    valve.currentPosition = 0;
                    coolingLoop.setEnabled();
                    heatingLoop.setDisabled();
                }
                if (conditioning == SystemController.State.COOLING )
                {
                    loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempHeating);
                }
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
    
                loopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
                if (conditioning == VavSystemController.State.COOLING )
                {
                    dischargeSp = supplyAirTemp + (MAX_DISCHARGE_TEMP - supplyAirTemp) * loopOp / 100;
                    vavDevice.setDischargeSp(dischargeSp);
                    valveController.updateControlVariable(dischargeSp, dischargeTemp);
                    valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
    
                }
               
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
               
            }
        
            if (valveController.getControlVariable() == 0)
            {
                valve.currentPosition = 0;
            }
    
            boolean  enabledCO2Control = vavDevice.getConfigNumVal("enable and co2") > 0 ;
            boolean  enabledIAQControl = vavDevice.getConfigNumVal("enable and iaq") > 0 ;
            String zoneId = HSUtil.getZoneIdFromEquipId(vavEquip.getId());;
            Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
            boolean occupied = (occ == null ? false : occ.isOccupied());
            
            if (!damper.isOverrideActive())
            {
                //CO2 loop output from 0-50% modulates damper min position.
                if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0)
                {
                    damper.iaqCompensatedMinPos = damper.minPosition + (damper.maxPosition - damper.minPosition) * Math.min(50, co2Loop.getLoopOutput()) / 50;
                    CcuLog.d(L.TAG_CCU_ZONE, "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
                }
    
                //VOC loop output from 0-50% modulates damper min position.
                if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0)
                {
                    damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, vocLoop.getLoopOutput()) / 50;
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
            }
            
            //When in the system is in heating, REHEAT control that does not follow RP-1455.
            if (conditioning == SystemController.State.HEATING && state == HEATING)
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
            
            
            if (occupied) {
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
            
            CcuLog.d(L.TAG_CCU_ZONE,"CoolingLoop "+node +"roomTemp :"+roomTemp+" setTempCooling: "+setTempCooling);
            coolingLoop.dump();
            CcuLog.d(L.TAG_CCU_ZONE,"HeatingLoop "+node +"roomTemp :"+roomTemp+" setTempHeating: "+setTempHeating);
            heatingLoop.dump();
    
            CcuLog.d(L.TAG_CCU_ZONE, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
                                                        +", valve:"+valve.currentPosition+" fanStart: "+vavUnit.fanStart);
    
            valve.applyLimits();
            damper.applyLimits();
            
            updateTRResponse(node);
            vavDevice.setDamperPos(damper.currentPosition);
            vavDevice.setReheatPos(valve.currentPosition);
            vavDevice.setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                         : state == COOLING ? buildingLimitMaxBreached() : false));
            vavDevice.updateLoopParams();
        }
    }
    
    @Override
    public ZoneState getState() {
        return state;
    }
    
}
//@enduml
