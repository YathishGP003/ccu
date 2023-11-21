package a75f.io.logic.bo.building.vav;

import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

/**
 * Created by samjithsadasivan on 8/23/18.
 *
 */

public class VavParallelFanProfile extends VavProfile
{
    public VavParallelFanProfile(String equipRef, Short nodeAddress) {
        super(equipRef, nodeAddress, ProfileType.VAV_PARALLEL_FAN);
    }

    //TODO - Only for backward compatibility during development. Should be removed.
    public VavParallelFanProfile() {
        super(null, null, ProfileType.VAV_PARALLEL_FAN);
    }
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_PARALLEL_FAN;
    }

    @Override
    public void updateZonePoints() {
        CcuLog.d(L.TAG_CCU_ZONE, "VAV Parallel Fan Control");
        
        if(mInterface != null) {
            mInterface.refreshView();
        }

        initLoopVariables((short)nodeAddr);
        double roomTemp = getCurrentTemp();

        Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"")).build();

        if (isZoneDead()) {
            updateZoneDead();
            return;
        }

        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
        int loopOp = getLoopOp(conditioning, roomTemp, equip);

        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        if (systemMode == SystemMode.OFF || valveController.getControlVariable() == 0) {
            valve.currentPosition = 0;
        }

        boolean occupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), equip.getRoomRef(), Occupancy.OCCUPIED);
        updateIaqCompensatedMinDamperPos(occupied, (short)nodeAddr);
        if (loopOp == 0) {
            damper.currentPosition = damper.iaqCompensatedMinPos;
        } else {
            damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
        }

        updateDamperPosForTrueCfm(CCUHsApi.getInstance(), conditioning);

        //When in the system is in heating, REHEAT control does not follow RP-1455.
        if (conditioning == SystemController.State.HEATING && state == HEATING) {
            updateReheatDuringSystemHeating(vavEquip.getId());
        }

        updateFanStatus();

        logLoopParams((short)nodeAddr, roomTemp, loopOp);
        updateLoopParams((short) nodeAddr);
    }
    
    private void initLoopVariables(int node) {
        setDamperLimits((short) node, damper);
    }
    
    private int getLoopOp(SystemController.State conditioning, double roomTemp, Equip equip) {
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
            
            int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
            if (conditioning == SystemController.State.COOLING) {
                updateReheatDuringSystemCooling(heatingLoopOp, vavEquip.getId());
                loopOp =  getGPC36AdjustedHeatingLoopOp(heatingLoopOp, roomTemp, vavEquip.getDischargeAirTemp().readHisVal(), equip);
            } else if (conditioning == SystemController.State.HEATING) {
                loopOp = heatingLoopOp;
            }
        } else {
            //Zone is in deadband
            handleDeadband();
        }
        return loopOp;
    }
    
    private void updateLoopParams(short node) {
        valve.applyLimits();
        damper.applyLimits();
        
        updateTRResponse(node);
        vavEquip.getDamperCmd().writeHisVal(damper.currentPosition);
        vavEquip.getReheatCmd().writeHisVal(valve.currentPosition);
        setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                                                                         : state == COOLING ? buildingLimitMaxBreached() : false));
        updateLoopParams();
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
        
        double dischargeTemp = vavEquip.getDischargeAirTemp().readHisVal();
        double supplyAirTemp = vavEquip.getEnteringAirTemp().readHisVal();
        double maxDischargeTemp = TunerUtil.readTunerValByQuery("max and discharge and air and temp", equipId);
        double dischargeSp = supplyAirTemp + (maxDischargeTemp - supplyAirTemp) * loopOp / 100;
        vavEquip.getDischargeAirTempSetpoint().writeHisVal(dischargeSp);
        valveController.updateControlVariable(dischargeSp, dischargeTemp);
        valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
        CcuLog.d(L.TAG_CCU_ZONE," dischargeTemp "+dischargeTemp+" dischargeSp " +dischargeSp+" supplyAirTemp "+supplyAirTemp);
    }
    
    private void updateReheatDuringSystemHeating(String equipId) {
        
        double valveStartDamperPercent = TunerUtil.readTunerValByQuery("vav and valve and start and damper and equipRef == \""+equipId+"\"");
        double maxHeatingPos = vavEquip.getMaxHeatingDamperPos().readDefaultVal();
        double minHeatingPos = vavEquip.getMinHeatingDamperPos().readDefaultVal();
        double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
        if (damper.currentPosition > valveStart) {
            valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
        } else {
            valve.currentPosition = 0;
        }
    }
    
    private boolean getZoneOccupancy(String equipId) {
        String zoneId = HSUtil.getZoneIdFromEquipId(equipId);
        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
        return occ != null && occ.isOccupied();
    }
    
    private void updateIaqCompensatedMinDamperPos(boolean occupied, short node) {
        
        double co2 = vavEquip.getZoneCO2().readHisVal();
        double voc = vavEquip.getZoneVoc().readHisVal();
        
        boolean  enabledCO2Control = vavEquip.getEnableCo2Control().readDefaultVal() > 0 ;
        boolean  enabledIAQControl = vavEquip.getEnableIAQControl().readDefaultVal() > 0 ;
    
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
    
    private void updateZoneDead() {
        if (vavEquip.getEquipStatus().readHisVal() != state.ordinal()) {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + nodeAddr + "\"", "Zone Temp Dead");
            SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            double damperMin = (int) (state == HEATING ? vavEquip.getMinHeatingDamperPos().readDefaultVal()
                    : vavEquip.getMinCoolingDamperPos().readDefaultVal());
            double damperMax = (int) (state == HEATING ? vavEquip.getMaxHeatingDamperPos().readDefaultVal()
                    : vavEquip.getMaxCoolingDamperPos().readDefaultVal());
            double damperPos = (damperMax+damperMin)/2;
            if(systemMode == SystemMode.OFF) {
                damperPos = vavEquip.getDamperCmd().readHisVal() > 0 ? vavEquip.getDamperCmd().readHisVal() : damperMin;
            }
            vavEquip.getDamperCmd().writeHisVal(damperPos);
            vavEquip.getNormalizedDamperCmd().writeHisVal(damperPos);
            vavEquip.getReheatCmd().writeHisVal(damperPos);
            CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" + nodeAddr + "\"", (double) TEMPDEAD.ordinal());

            setFanOn("parallel", false);
            CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" + nodeAddr + "\"", (double) TEMPDEAD.ordinal());
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + nodeAddr + "\"",
                                                                            "Zone Temp Dead "+getFanStatusMessage());
        }
    }
    
    private void updateFanStatus () {
        if (state == HEATING) {
            if (!isFanOn("parallel")) {
                setFanOn("parallel", true);
            }
        } else {
            if (isFanOn("parallel")) {
                setFanOn("parallel", false);
            }
        }
    }
    
    @Override
    public ZoneState getState() {
        return state;
    }
}
