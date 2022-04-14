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
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

//@startuml
public class VavSeriesFanProfile extends VavProfile
{
    
    private VavEquip vavDevice;
    
    private boolean fanReady = false;
    private boolean damperOverride = false;
    private int fanOnDelayCounter = 0;
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_SERIES_FAN;
    }
    
    @Override
    public void updateZonePoints() {
        CcuLog.d(L.TAG_CCU_ZONE, "VAV Series Fan Control");
        
        if(mInterface != null) {
            mInterface.refreshView();
        }
        
        for (short node : vavDeviceMap.keySet()) {
            
            if (vavDeviceMap.get(node) == null) {
                addLogicalMap(node);
                CcuLog.d(L.TAG_CCU_ZONE, " Logical Map added for node " + node);
                continue;
            }
            
            initLoopVariables(node);
            double roomTemp = vavDevice.getCurrentTemp();
            double averageDesiredTemp = (setTempCooling+setTempHeating)/2;
            if (averageDesiredTemp != vavDevice.getDesiredTemp()) {
                vavDevice.setDesiredTemp(averageDesiredTemp);
            }
            Equip vavEquip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + node + "\"")).build();
    
            if (isZoneDead()) {
                updateZoneDead(node);
                continue;
            }
            
            SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
            int loopOp = getLoopOp(conditioning, roomTemp,vavEquip.getId());
            
            SystemMode systemMode = SystemMode.values()[(int)(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            if (systemMode == SystemMode.OFF|| valveController.getControlVariable() == 0) {
                valve.currentPosition = 0;
            }
            
            boolean occupied = getZoneOccupancy(vavEquip.getId());
            updateIaqCompensatedMinDamperPos(occupied, node);
    
            if (loopOp == 0) {
                damper.currentPosition = damper.iaqCompensatedMinPos;
            } else {
                damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
            }
            
            //When in the system is in heating, REHEAT control does not follow RP-1455.
            if (conditioning == SystemController.State.HEATING && state == HEATING) {
                updateReheatDuringSystemHeating(vavEquip.getId());
            }
    
            updateFanStatus(occupied, vavEquip.getId(), systemMode);
            
            logLoopParams(node, roomTemp, loopOp);
            updateLoopParams(node);
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
        SeriesFanVavUnit vavUnit = (SeriesFanVavUnit)vavDevice.getVavUnit();
        damper = vavUnit.vavDamper;
        valve = vavUnit.reheatValve;
        setDamperLimits(node, damper);
    }
    
    private int getLoopOp(SystemController.State conditioning, double roomTemp, String vavEquip) {
        int loopOp = 0;
        SystemMode systemMode = SystemMode.values()[(int)(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        if (roomTemp > setTempCooling && systemMode != SystemMode.OFF) {
            //Zone is in Cooling
            if (state != COOLING) {
                handleCoolingChangeOver();
            }
            if (conditioning == SystemController.State.COOLING ) {
                loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
            }
        } else if (roomTemp < setTempHeating && systemMode != SystemMode.OFF) {
            //Zone is in heating
            if (state != HEATING) {
                handleHeatingChangeOver();
            }
            loopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
            if (conditioning == VavSystemController.State.COOLING ) {
                updateReheatDuringSystemCooling(loopOp,vavEquip);
            }
        } else {
            //Zone is in deadband
            handleDeadband();
        }
        return loopOp;
    }
    
    private void updateLoopParams(short node) {
        valve.applyLimits();
        
        if (!damperOverride) {
            damper.applyLimits();
        }
    
        updateTRResponse(node);
        vavDevice.setDamperPos(damper.currentPosition);
        vavDevice.setReheatPos(valve.currentPosition);
        vavDevice.setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                                                                         : state == COOLING ? buildingLimitMaxBreached() : false));
        vavDevice.updateLoopParams();
    }
    
    private void logLoopParams(short node, double roomTemp, int loopOp) {
        
        CcuLog.d(L.TAG_CCU_ZONE,"CoolingLoop "+node +" roomTemp :"+roomTemp+" setTempCooling: "+setTempCooling+
                                                                                " Op: "+coolingLoop.getLoopOutput());
        coolingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE, "HeatingLoop "+node +" roomTemp :"+roomTemp+" setTempHeating: "+setTempHeating+
                                                                                    " Op "+heatingLoop.getLoopOutput());
        heatingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
                                 +", valve:"+valve.currentPosition+" damperOverride: "+damperOverride);
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
        valve.currentPosition = 0;
        heatingLoop.setDisabled();
        coolingLoop.setDisabled();
    }
    
    private void updateReheatDuringSystemCooling(int loopOp, String equipId) {
        
        double dischargeTemp = vavDevice.getDischargeTemp();
        double supplyAirTemp = vavDevice.getSupplyAirTemp();
        double maxDischargeTemp = TunerUtil.readTunerValByQuery("max and discharge and air and temp", equipId);
        double dischargeSp = supplyAirTemp + (maxDischargeTemp - supplyAirTemp) * loopOp / 100;
        vavDevice.setDischargeSp(dischargeSp);
        valveController.updateControlVariable(dischargeSp, dischargeTemp);
        valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
        CcuLog.d(L.TAG_CCU_ZONE, " dischargeTemp "+dischargeTemp+" dischargeSp "+dischargeSp+" supplyAirTemp "+supplyAirTemp);
    }
    
    private void updateReheatDuringSystemHeating(String equipId) {
        
        double valveStartDamperPercent = TunerUtil.readTunerValByQuery("vav and valve and start and damper and equipRef == \""+equipId+"\"");
        double maxHeatingPos = vavDevice.getDamperLimit("heating", "max");
        double minHeatingPos = vavDevice.getDamperLimit("heating", "min");
        double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
        if (damper.currentPosition > valveStart) {
            valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
        } else {
            valve.currentPosition = 0;
        }
    }
    
    private boolean getZoneOccupancy(String equipId) {
        String zoneId = HSUtil.getZoneIdFromEquipId(equipId);
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        return occ != null && occ.isOccupied();
    }
    
    private void updateIaqCompensatedMinDamperPos(boolean occupied, short node) {
    
        double co2 = vavDeviceMap.get(node).getCO2();
        double voc = vavDeviceMap.get(node).getVOC();
    
        boolean  enabledCO2Control = vavDevice.getConfigNumVal("enable and co2") > 0 ;
        boolean  enabledIAQControl = vavDevice.getConfigNumVal("enable and iaq") > 0 ;
    
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if(epidemicState != EpidemicState.OFF && L.ccu().oaoProfile != null) {
            double smartPurgeDABDamperMinOpenMultiplier = TunerUtil.readTunerValByQuery("purge and system and vav and damper and pos and min and multiplier", L.ccu().oaoProfile.getEquipRef());
            damper.iaqCompensatedMinPos = (int)(damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        }else {
            damper.iaqCompensatedMinPos = damper.minPosition;
        }
        //CO2 loop output from 0-50% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0) {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.minPosition) * Math.min(50, co2Loop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE, "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
        }
    
        //VOC loop output from 0-50% modulates damper min position.
        if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0) {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, vocLoop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE, "VOCLoopOp :" + vocLoop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
        }
    }
    
    private void updateZoneDead(short node) {
    
        CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead "+node+" roomTemp : "+vavDeviceMap.get(node).getCurrentTemp());
        state = TEMPDEAD;
        double zoneStatus = vavDevice.getStatus();
        if (zoneStatus != state.ordinal()) {
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
            vavDevice.setFanOn("series", false);
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + node + "\"", (double) TEMPDEAD.ordinal());
            
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + node + "\"",
                                                   "Zone Temp Dead"+vavDevice.getFanStatusMessage());
        }
    }
    
    private void updateFanStatus (boolean occupied, String equipId, SystemMode mode) {
        if ((occupied || valve.currentPosition > 0) && mode != SystemMode.OFF) {
            //Prior to starting the fan, the damper is first driven fully closed to ensure that the fan is not rotating backwards.
            //Once the fan is proven on for a fixed time delay (15 seconds), the damper override is released
            CcuLog.d(L.TAG_CCU_ZONE,
                     "updateFanStatus fanOnDelayCounter: "+fanOnDelayCounter+" fanOn: "+vavDevice.isFanOn("series"));
            if (!vavDevice.isFanOn("series")) {
                double fanOnDelay = TunerUtil.readTunerValByQuery("vav and fan and control and delay " +
                                                                  "and equipRef == \""+equipId+"\"");
                if (fanOnDelayCounter == fanOnDelay) {
                    vavDevice.setFanOn("series", true);
                    damperOverride = false;
                } else {
                    damperOverride = true;
                    damper.currentPosition = 0;
                    fanOnDelayCounter++;
                }
            }
        
        } else {
            CcuLog.d(L.TAG_CCU_ZONE, "updateFanStatus false");
            if (vavDevice.isFanOn("series")) {
                vavDevice.setFanOn("series", false);
            }
            fanOnDelayCounter = 0;
            damperOverride = false;
        }
    }
    
    @Override
    public boolean isDamperOverrideActive() {
        return damperOverride;
    }
    
    @Override
    public ZoneState getState() {
        return state;
    }
    
}
//@enduml
