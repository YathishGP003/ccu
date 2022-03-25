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
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import android.util.Log;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavReheatProfile extends VavProfile
{
  
    private boolean satCompensationEnabled = false;
    
    VavEquip    vavDevice;
    ControlLoop coolingLoop;
    ControlLoop heatingLoop;
    CO2Loop co2Loop;
    VOCLoop vocLoop;
    VavUnit vavUnit;
    GenericPIController valveController;
    Damper damper;
    Valve valve;
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_REHEAT;
    }
    
    //VAV damper and reheat coil control logic is implemented according to section 1.3.E.6 of
    //ASHRAE RP-1455: Advanced Control Sequences for HVAC Systems Phase I, Air Distribution and Terminal Systems
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
                CcuLog.d(L.TAG_CCU_ZONE, " Logical Map added for node " + node);
                continue;
            }
            if (isZoneDead()) {
                updateZoneDead(node);
                continue;
            }
            
            initLoopVariables(node);
            
            double roomTemp = vavDevice.getCurrentTemp();
            double averageDesiredTemp = (setTempCooling+setTempHeating)/2;
            if (averageDesiredTemp != vavDevice.getDesiredTemp()) {
                vavDevice.setDesiredTemp(averageDesiredTemp);
            }
            
            damper = vavUnit.vavDamper;
            valve = vavUnit.reheatValve;
            setDamperLimits(node, damper);
            
            int loopOp = 0;
            //If supply air temperature from air handler is greater than room temperature, Cooling shall be
            //locked out.
            SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
            SystemMode systemMode = SystemMode.values()[(int)(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            Equip vavEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
    
            if (roomTemp > setTempCooling && systemMode != SystemMode.OFF ) {
                //Zone is in Cooling
                if (state != COOLING) {
                    handleCoolingChangeOver();
                }
                if (conditioning == SystemController.State.COOLING) {
                    loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
                }
            } else if (roomTemp < setTempHeating && systemMode != SystemMode.OFF) {
                //Zone is in heating
                if (state != HEATING) {
                    handleHeatingChangeOver();
                }
                int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
                if (conditioning == SystemController.State.COOLING) {
                    updateReheatDuringSystemCooling(heatingLoopOp, roomTemp, vavEquip.getId());
                    loopOp =  getGPC36AdjustedHeatingLoopOp(heatingLoopOp, roomTemp, vavDevice.getDischargeTemp(), vavEquip);
                } else if (conditioning == SystemController.State.HEATING) {
                    loopOp = heatingLoopOp;
                }
            } else {
                //Zone is in deadband
                if (state != DEADBAND) {
                    handleDeadband();
                }
            }
            
            updateIaqCompensatedMinDamperPos(node, vavEquip);
            CcuLog.d(L.TAG_CCU_ZONE,"VAVLoopOp :"+loopOp+", adjusted minposition "+damper.iaqCompensatedMinPos+","+damper.currentPosition);
    
            damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
            
            if (systemMode == SystemMode.OFF|| valveController.getControlVariable() == 0) {
                valve.currentPosition = 0;
            }
    
            //When in the system is in heating, REHEAT control does not follow RP-1455.
            if (conditioning == SystemController.State.HEATING && state == HEATING) {
                updateReheatDuringSystemHeating(vavEquip);
            }
    
            logLoopParams(node, roomTemp, loopOp);;
            
            updateTRResponse(node);
    
            valve.applyLimits();
            damper.applyLimits();
    
            vavDevice.setDamperPos(damper.currentPosition);
            vavDevice.setReheatPos(valve.currentPosition);
            CcuLog.d(L.TAG_CCU_ZONE, "buildingLimitMaxBreached "+buildingLimitMaxBreached()+" buildingLimitMinBreached "+buildingLimitMinBreached());
            
            vavDevice.setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                         : state == COOLING ? buildingLimitMaxBreached() : false));
            vavDevice.updateLoopParams();
        }
    }
    
    private void logLoopParams(short node, double roomTemp, int loopOp) {
        
        CcuLog.d(L.TAG_CCU_ZONE,"CoolingLoop "+node +" roomTemp :"+roomTemp+" setTempCooling: "+setTempCooling+
                                " Op: "+coolingLoop.getLoopOutput());
        coolingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE,"HeatingLoop "+node +" roomTemp :"+roomTemp+" setTempHeating: "+setTempHeating+
                                " Op: "+heatingLoop.getLoopOutput());
        heatingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
                                 +", valve:"+valve.currentPosition);
    }
    
    private void updateZoneDead(Short node) {
        
        CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead "+node+" roomTemp : "+vavDeviceMap.get(node).getCurrentTemp());
        state = TEMPDEAD;
        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+node+"\"");
        
        if (!curStatus.equals("Zone Temp Dead")) {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"", "Zone Temp Dead");
            VavEquip vavDevice = vavDeviceMap.get(node);
            SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            double damperMin = vavDevice.getDamperLimit(state == HEATING ? "heating":"cooling", "min");
            double damperMax = vavDevice.getDamperLimit(state == HEATING ? "heating":"cooling", "max");
            double damperPos = (damperMax+damperMin)/2;
            if(systemMode == SystemMode.OFF) {
                damperPos = vavDevice.getDamperPos() > 0 ? vavDevice.getDamperPos() : damperMin;
            }
            vavDevice.setDamperPos(damperPos);
            vavDevice.setNormalizedDamperPos(damperPos);
            vavDevice.setReheatPos(0);
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + node + "\"", (double) TEMPDEAD.ordinal());
        }
    }
    
    private void initLoopVariables(short node) {
        
        vavDevice = vavDeviceMap.get(node);
        coolingLoop = vavDevice.getCoolingLoop();
        heatingLoop = vavDevice.getHeatingLoop();
        co2Loop = vavDeviceMap.get(node).getCo2Loop();
        vocLoop = vavDeviceMap.get(node).getVOCLoop();
        valveController = vavDevice.getValveController();
        setTempCooling = vavDevice.getDesiredTempCooling();
        setTempHeating = vavDevice.getDesiredTempHeating();
        vavUnit = vavDevice.getVavUnit();
        damper = vavUnit.vavDamper;
        valve = vavUnit.reheatValve;
        setDamperLimits(node, damper);
    }
    
    private void handleCoolingChangeOver() {
        state = COOLING;
        valveController.reset();
        valve.currentPosition = 0;
        coolingLoop.setEnabled();
        heatingLoop.setDisabled();
    }
    
    private void handleHeatingChangeOver() {
        
        state = HEATING;
        heatingLoop.setEnabled();
        coolingLoop.setDisabled();
    }
    
    private void handleDeadband() {
        
        state = DEADBAND;
        valveController.reset();
        valve.currentPosition = 0;
        heatingLoop.setDisabled();
        coolingLoop.setDisabled();
    }
    
    private void updateReheatDuringSystemCooling(int heatingLoopOp, double roomTemp, String vavEquip) {
        Log.i(TAG, "updateReheatDuringSystemCooling: ");
        double maxDischargeTemp = TunerUtil.readTunerValByQuery("max and discharge and air and temp", vavEquip);
        double heatingLoopOffset = TunerUtil.readTunerValByQuery("offset  and discharge and air and temp", vavEquip);
        double dischargeTemp = vavDevice.getDischargeTemp();
        double supplyAirTemp = vavDevice.getSupplyAirTemp();
        double datMax = (roomTemp + heatingLoopOffset) > maxDischargeTemp ? maxDischargeTemp : (roomTemp + heatingLoopOffset);
        
        if (!isSupplyAirTempValid(supplyAirTemp, dischargeTemp)) {
            CcuLog.d(L.TAG_CCU_ZONE, "updateReheatDuringSystemCooling : Invalid SAT , Use roomTemp "+roomTemp);
            supplyAirTemp = roomTemp;
        }
        double dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * Math.min(heatingLoopOp, 50) / 50;
        valveController.updateControlVariable(dischargeSp, dischargeTemp);
        valveController.dump();
        vavDevice.setDischargeSp(dischargeSp);
        int valvePosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
    
        CcuLog.d(L.TAG_CCU_ZONE,
                 "updateReheatDuringSystemCooling :  supplyAirTemp: " + supplyAirTemp +  " datMax: " + datMax+
                 " dischargeTemp: " + dischargeTemp +" dischargeSp "+dischargeSp);
        
        valve.currentPosition = valvePosition < 0 ? 0 : valvePosition > 100 ? 100 : valvePosition;
    }
    
    /**
     * Thermistor measurements interpret junk reading when they are actually not connected.
     * Avoid running the loop when the air temps are outside a reasonable range to make sure they are not picked by the
     * algorithm.
     */
    private boolean isSupplyAirTempValid(double sat, double dat) {
        if (sat < 0 || sat > 200) {
            return false;
        }
        
        return true;
    }
    
    private void updateReheatDuringSystemHeating(Equip vavEquip) {
        
        double valveStartDamperPercent = TunerUtil.readTunerValByQuery("vav and valve and start and damper and equipRef == \""+vavEquip.getId()+"\"");
        double maxHeatingPos = vavDevice.getDamperLimit("heating", "max");
        double minHeatingPos = vavDevice.getDamperLimit("heating", "min");
        double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
        CcuLog.d(L.TAG_CCU_ZONE," valveStartDamperPercent "+valveStartDamperPercent+" valveStart "+valveStart );
        if (damper.currentPosition > valveStart) {
            valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
        } else {
            valve.currentPosition = 0;
        }
    }
    
    private void updateIaqCompensatedMinDamperPos(Short node, Equip vavEquip) {
    
        double co2 = vavDeviceMap.get(node).getCO2();
        double voc = vavDeviceMap.get(node).getVOC();
        
        boolean  enabledCO2Control = vavDevice.getConfigNumVal("enable and co2") > 0 ;
        boolean  enabledIAQControl = vavDevice.getConfigNumVal("enable and iaq") > 0 ;
        String zoneId = HSUtil.getZoneIdFromEquipId(vavEquip.getId());
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied()) || (ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING);
        
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if(epidemicState != EpidemicState.OFF && L.ccu().oaoProfile != null) {
            double smartPurgeDABDamperMinOpenMultiplier = TunerUtil.readTunerValByQuery("purge and system and vav and damper and pos and min and multiplier", L.ccu().oaoProfile.getEquipRef());
            damper.iaqCompensatedMinPos = (int)(damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        } else {
            damper.iaqCompensatedMinPos = damper.minPosition;
        }
        //CO2 loop output from 0-50% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0) {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, co2Loop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE,"CO2LoopOp :"+co2Loop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos);
        }
    
        //VOC loop output from 0-50% modulates damper min position.
        if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0) {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, vocLoop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE,"VOCLoopOp :"+vocLoop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos);
        }
        
    }
    
    @Override
    public ZoneState getState() {
        return state;
    }
}
