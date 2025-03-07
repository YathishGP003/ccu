package a75f.io.logic.bo.building.vav;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 8/23/18.
 *
 */

public class VavParallelFanProfile extends VavProfile
{
    public VavParallelFanProfile(String equipRef, Short nodeAddress) {
        super(equipRef, nodeAddress, ProfileType.VAV_PARALLEL_FAN);
    }

    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_PARALLEL_FAN;
    }

    @Override
    public void updateZonePoints() {
        CcuLog.i(L.TAG_CCU_ZONE, "--->VavParallelFanProfile<--- "+nodeAddr);
        
        if(mInterface != null) {
            mInterface.refreshView();
        }

        initLoopVariables((short)nodeAddr);
        double roomTemp = getCurrentTemp();

        Equip equip = new Equip.Builder().setHashMap(CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"")).build();

        if (isRFDead()) {
            handleRFDead();
            return;
        }else if (isZoneDead()) {
            updateZoneDead();
            return;
        }

        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();

        CcuLog.e(L.TAG_CCU_ZONE, "Run Zone algorithm for "+nodeAddr+" setTempCooling "+setTempCooling+
                "setTempHeating "+setTempHeating+" systemMode "+conditioning+" roomTemp "+roomTemp);

        CcuLog.i(L.TAG_CCU_ZONE, "PI Tuners: proportionalGain " + proportionalGain + ", integralGain " + integralGain +
                ", proportionalSpread " + proportionalSpread + ", integralMaxTimeout " + integralMaxTimeout);
        if (vavEquip.getEnableCFMControl().readPriorityVal() > 0) {
            CcuLog.i(L.TAG_CCU_ZONE, "CFM PI Tuners: cfmProportionalGain " + cfmController.getProportionalGain() + ", cfmIntegralGain " + cfmController.getIntegralGain() +
                    ", cfmProportionalSpread " + cfmController.getProportionalSpread() + ", cfmIntegralMaxTimeout " + cfmController.getIntegralMaxTimeout());
        }

        int loopOp = getLoopOp(conditioning, roomTemp, equip);
        loopOp = Math.max(0, loopOp);
        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        if (systemMode == SystemMode.OFF || valveController.getControlVariable() == 0) {
            valve.currentPosition = 0;
        }

        boolean occupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), equip.getRoomRef(), Occupancy.OCCUPIED);
        updateIaqCompensatedMinDamperPos(occupied);
        if (loopOp == 0) {
            damper.currentPosition = damper.iaqCompensatedMinPos;
        } else {
            damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
        }

        if (TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), vavEquip.getId())) {
            updateDamperPosForTrueCfm(CCUHsApi.getInstance(), conditioning);
        }

        //When in the system is in heating, REHEAT control does not follow RP-1455.
        if (conditioning == SystemController.State.HEATING && state == HEATING) {
            updateReheatDuringSystemHeating(vavEquip.getId());
        }

        updateFanStatus();

        logLoopParams((short)nodeAddr, roomTemp, loopOp);
        updateLoopParams((short) nodeAddr);
        CcuLog.e(L.TAG_CCU_ZONE, "LoopStatus HeatingLoop "+heatingLoop.getEnabled()+" CoolingLoop "+coolingLoop.getEnabled());
    }


    private void initLoopVariables(int node) {
        dischargeSp = 0;
        setTempCooling = vavEquip.getDesiredTempCooling().readPriorityVal();
        setTempHeating = vavEquip.getDesiredTempHeating().readPriorityVal();

        if (hasPendingTunerChange()) refreshPITuners();

        setDamperLimits((short) node, damper);
    }
    
    private int getLoopOp(SystemController.State conditioning, double roomTemp, Equip equip) {
        int loopOp = 0;
        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        boolean reheatEnabled = vavEquip.getReheatType().readPriorityVal() > 0;
        if (roomTemp > setTempCooling && isCoolingAvailable(systemMode)) {
            //Zone is in Cooling
            if (state != COOLING) {
                handleCoolingChangeOver();
            }
            if (conditioning == SystemController.State.COOLING ) {
                loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
                vavEquip.getCoolingLoopOutput().writePointValue(loopOp);
                loopOp = (int) vavEquip.getCoolingLoopOutput().readHisVal();
            }
        } else if (roomTemp < setTempHeating && isHeatingAvailable(systemMode, reheatEnabled)) {
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
            vavEquip.getHeatingLoopOutput().writePointValue(loopOp);
            loopOp = (int) vavEquip.getHeatingLoopOutput().readHisVal();
        } else {
            //Zone is in deadband
            handleDeadband(systemMode, reheatEnabled);
            if (heatingLoop.getEnabled()) {
                loopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
                loopOp = Math.max(0, loopOp);
                if (conditioning == SystemController.State.COOLING && isHeatingAvailable(systemMode, reheatEnabled)) {
                    updateReheatDuringSystemCooling(loopOp, vavEquip.getId());
                    loopOp =  getGPC36AdjustedHeatingLoopOp(loopOp, roomTemp, vavEquip.getDischargeAirTemp().readHisVal(), equip);
                }
            } else if (coolingLoop.getEnabled()) {
                loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
            }
        }
        return loopOp;
    }
    
    private void updateLoopParams(short node) {
        valve.applyLimits();
        damper.applyLimits();
        
        updateTRResponse(node);
        vavEquip.getDamperCmd().writeHisVal(damper.currentPosition);
        vavEquip.getReheatCmd().writePointValue(valve.currentPosition);
        valve.currentPosition = (int) vavEquip.getReheatCmd().readHisVal();
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
    
    private void updateReheatDuringSystemCooling(int loopOp, String equipId) {
        
        double dischargeTemp = vavEquip.getDischargeAirTemp().readHisVal();
        double supplyAirTemp = vavEquip.getEnteringAirTemp().readHisVal();
        double maxDischargeTemp = vavEquip.getReheatZoneMaxDischargeTemp().readPriorityVal();
        dischargeSp = supplyAirTemp + (maxDischargeTemp - supplyAirTemp) * loopOp / 100;
        valveController.updateControlVariable(dischargeSp, dischargeTemp);
        valve.currentPosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
        CcuLog.d(L.TAG_CCU_ZONE," dischargeTemp "+dischargeTemp+" dischargeSp " +dischargeSp+" supplyAirTemp "+supplyAirTemp);
    }
    
    private void updateReheatDuringSystemHeating(String equipId) {
        
        double valveStartDamperPercent = vavEquip.getValveActuationStartDamperPosDuringSysHeating().readPriorityVal();
        double maxHeatingPos = vavEquip.getMaxHeatingDamperPos().readDefaultVal();
        double minHeatingPos = vavEquip.getMinHeatingDamperPos().readPriorityVal();
        double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
        if (damper.currentPosition > valveStart) {
            valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
        } else {
            valve.currentPosition = 0;
        }
        CcuLog.d(L.TAG_CCU_ZONE,"updateReheatDuringSystemHeating valveStart "+valveStart);
    }

    private void updateIaqCompensatedMinDamperPos(boolean occupied) {
        
        double co2 = vavEquip.getZoneCO2().readHisVal();
        boolean  enabledCO2Control = vavEquip.getEnableCo2Control().readDefaultVal() > 0 ;
        if (enabledCO2Control) { CcuLog.e(L.TAG_CCU_ZONE, "DCV Tuners: co2Target " + co2Loop.getCo2Target() + ", co2Threshold " + co2Loop.getCo2Threshold()); }

        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if(epidemicState != EpidemicState.OFF && L.ccu().oaoProfile != null) {
            double smartPurgeDABDamperMinOpenMultiplier = L.ccu().oaoProfile.getOAOEquip().getSystemPurgeVavDamperMinOpenMultiplier().readPriorityVal();
            damper.iaqCompensatedMinPos = (int)(damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        }else {
            damper.iaqCompensatedMinPos = damper.minPosition;
        }
        
        //CO2 loop output from 0-50% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0) {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.minPosition) * Math.min(50, co2Loop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE, "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
        }
        
    }
    
    private void updateZoneDead() {
        CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead "+nodeAddr+" roomTemp : "+vavEquip.getCurrentTemp().readHisVal());
        state = TEMPDEAD;
        if (vavEquip.getEquipStatus().readHisVal() != state.ordinal()) {

            double damperMin = (int) (state == HEATING ? vavEquip.getMinHeatingDamperPos().readPriorityVal()
                    : vavEquip.getMinCoolingDamperPos().readPriorityVal());
            double damperMax = (int) (state == HEATING ? vavEquip.getMaxHeatingDamperPos().readDefaultVal()
                    : vavEquip.getMaxCoolingDamperPos().readDefaultVal());
            double damperPos = (damperMax+damperMin)/2;
            SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            if(systemMode == SystemMode.OFF) {
                damperPos = vavEquip.getDamperCmd().readHisVal() > 0 ? vavEquip.getDamperCmd().readHisVal() : damperMin;
            }
            vavEquip.getDamperCmd().writeHisVal(damperPos);
            vavEquip.getNormalizedDamperCmd().writePointValue(damperPos);
            vavEquip.getReheatCmd().writePointValue(0);
            vavEquip.getEquipStatus().writeHisVal((double) TEMPDEAD.ordinal());
            vavEquip.getEquipStatusMessage().writeDefaultVal("Zone Temp Dead : "+getFanStatusMessage().replace(", ", ""));
            vavEquip.getParallelFanCmd().writeHisVal(0);
        }
    }
    private void handleRFDead() {
        vavEquip.getEquipStatusMessage().writeDefaultVal(RFDead);
        vavEquip.getEquipStatus().writeHisVal(RFDEAD.ordinal());
    }
    private void updateFanStatus () {
        vavEquip.getParallelFanCmd().writeHisVal(state == HEATING ? 1 : 0);
    }
    
    @Override
    public ZoneState getState() {
        return state;
    }
}
