package a75f.io.logic.bo.building.vav;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import org.projecthaystack.UnknownRecException;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavReheatProfile extends VavProfile
{

    public VavReheatProfile(String equipRef, Short nodeAddress) {
        super(equipRef, nodeAddress, ProfileType.VAV_REHEAT);
    }


    //TODO - Only for backward compatibility during development. Should be removed.
    public VavReheatProfile() {
        super(null, null, ProfileType.VAV_REHEAT);
    }
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
        CcuLog.i(L.TAG_CCU_ZONE, "--->VavReheatProfile<--- "+nodeAddr);
        if (isRFDead()) {
            handleRFDead();
            return;
        }else if (isZoneDead()) {
            updateZoneDead();
            return;
        }
        initLoopVariables();
        double roomTemp = getCurrentTemp();

        int loopOp = 0;
        //If supply air temperature from air handler is greater than room temperature, Cooling shall be
        //locked out.
        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        Equip equip = new Equip.Builder()
                .setHashMap(CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddr + "\"")).build();
        CcuLog.e(L.TAG_CCU_ZONE, "Run Zone algorithm for "+nodeAddr+" setTempCooling "+setTempCooling+
                                    "setTempHeating "+setTempHeating+" systemMode "+systemMode);

        CcuLog.i(L.TAG_CCU_ZONE, "PI Tuners: proportionalGain " + proportionalGain + ", integralGain " + integralGain +
                ", proportionalSpread " + proportionalSpread + ", integralMaxTimeout " + integralMaxTimeout);
        if (vavEquip.getEnableCFMControl().readPriorityVal() > 0) {
            CcuLog.i(L.TAG_CCU_ZONE, "CFM PI Tuners: cfmProportionalGain " + cfmController.getProportionalGain() + ", cfmIntegralGain " + cfmController.getIntegralGain() +
                    ", cfmProportionalSpread " + cfmController.getProportionalSpread() + ", cfmIntegralMaxTimeout " + cfmController.getIntegralMaxTimeout());
        }

        if (roomTemp > setTempCooling && systemMode != SystemMode.OFF ) {
            //Zone is in Cooling
            if (state != COOLING) {
                handleCoolingChangeOver();
            }
            if (conditioning == SystemController.State.COOLING) {
                loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);

                vavEquip.getCoolingLoopOutput().writePointValue(loopOp);
                loopOp = (int) vavEquip.getCoolingLoopOutput().readHisVal();
            }
        } else if (roomTemp < setTempHeating && systemMode != SystemMode.OFF) {
            //Zone is in heating
            if (state != HEATING) {
                handleHeatingChangeOver();
            }
            int heatingLoopOp = (int) heatingLoop.getLoopOutput(setTempHeating, roomTemp);
            if (conditioning == SystemController.State.COOLING) {
                updateReheatDuringSystemCooling(heatingLoopOp, roomTemp, vavEquip.getId());
                loopOp =  getGPC36AdjustedHeatingLoopOp(heatingLoopOp, roomTemp, vavEquip.getDischargeAirTemp().readHisVal(), equip);
            } else if (conditioning == SystemController.State.HEATING) {
                loopOp = heatingLoopOp;
            }
            vavEquip.getHeatingLoopOutput().writePointValue(loopOp);
            loopOp = (int) vavEquip.getHeatingLoopOutput().readHisVal();
        } else {
            //Zone is in deadband
            if (state != DEADBAND) {
                handleDeadband();
            }
        }
        try {
            updateIaqCompensatedMinDamperPos(nodeAddr, equip);
        } catch (UnknownRecException e) {
            CcuLog.e(L.TAG_CCU_ZONE, "IaqCompensation cannot be performed ", e);
        }
        damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;

        CcuLog.d(L.TAG_CCU_ZONE,"VAVLoopOp :"+loopOp+", adjusted minposition "+damper.iaqCompensatedMinPos+","+damper.currentPosition);

        if (systemMode == SystemMode.OFF|| valveController.getControlVariable() == 0) {
            valve.currentPosition = 0;
        }

        if (TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), vavEquip.getId())) {
            updateDamperPosForTrueCfm(CCUHsApi.getInstance(), conditioning);
        }

        //When in the system is in heating, REHEAT control does not follow RP-1455.
        if (conditioning == SystemController.State.HEATING && state == HEATING) {
            updateReheatDuringSystemHeating(equip);
        }

        damper.currentPosition = Math.max(damper.currentPosition, damper.minPosition);
        damper.currentPosition = Math.min(damper.currentPosition, damper.maxPosition);
        valve.applyLimits();

        vavEquip.getDamperCmd().writeHisVal(damper.currentPosition);
        vavEquip.getReheatCmd().writePointValue(valve.currentPosition);
        valve.currentPosition = (int) vavEquip.getReheatCmd().readHisVal();

        logLoopParams(nodeAddr, roomTemp, loopOp);

        updateTRResponse((short)nodeAddr);

        CcuLog.d(L.TAG_CCU_ZONE, "buildingLimitMaxBreached "+buildingLimitMaxBreached()+" buildingLimitMinBreached "+buildingLimitMinBreached());

        setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                : state == COOLING ? buildingLimitMaxBreached() : false));
        updateLoopParams();
    }


    private void logLoopParams(int node, double roomTemp, int loopOp) {
        
        CcuLog.d(L.TAG_CCU_ZONE,"CoolingLoop Op: "+coolingLoop.getLoopOutput());
        coolingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE,"HeatingLoop Op: "+heatingLoop.getLoopOutput());
        heatingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
                                 +", valve:"+valve.currentPosition);
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
            vavEquip.getEquipStatusMessage().writeDefaultVal("Zone Temp Dead");
        }
    }

    private void handleRFDead() {
        CcuLog.d(L.TAG_CCU_ZONE,"RF Signal Dead : equipRef: "+vavEquip.getEquipRef());
        vavEquip.getEquipStatus().writeHisVal(RFDEAD.ordinal());
        vavEquip.getEquipStatusMessage().writeDefaultVal(RFDead);
    }

    private void initLoopVariables() {
        dischargeSp = 0;
        setTempCooling = vavEquip.getDesiredTempCooling().readPriorityVal();
        setTempHeating = vavEquip.getDesiredTempHeating().readPriorityVal();

        if (hasPendingTunerChange()) refreshPITuners();

        setDamperLimits( (short) nodeAddr, damper);
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
    
    private void updateReheatDuringSystemCooling(int heatingLoopOp, double roomTemp, String vavEquipId) {
        CcuLog.i(TAG, "updateReheatDuringSystemCooling: ");
        double maxDischargeTemp = TunerUtil.readTunerValByQuery("max and discharge and air and temp", vavEquipId);
        double heatingLoopOffset = TunerUtil.readTunerValByQuery("offset  and discharge and air and temp", vavEquipId);
        double dischargeTemp = vavEquip.getDischargeAirTemp().readHisVal();
        double supplyAirTemp = vavEquip.getEnteringAirTemp().readHisVal();
        double datMax = Math.min((roomTemp + heatingLoopOffset), maxDischargeTemp);
        
        if (!isSupplyAirTempValid(supplyAirTemp)) {
            CcuLog.d(L.TAG_CCU_ZONE, "updateReheatDuringSystemCooling : Invalid SAT , Use roomTemp "+roomTemp);
            supplyAirTemp = roomTemp;
        }
        dischargeSp = supplyAirTemp + (datMax - supplyAirTemp) * Math.min(heatingLoopOp, 50) / 50;
        valveController.updateControlVariable(dischargeSp, dischargeTemp);
        valveController.dump();
        int valvePosition = (int) (valveController.getControlVariable() * 100 / valveController.getMaxAllowedError());
    
        CcuLog.d(L.TAG_CCU_ZONE,
                 "updateReheatDuringSystemCooling :  supplyAirTemp: " + supplyAirTemp +  " datMax: " + datMax+
                 " dischargeTemp: " + dischargeTemp +" dischargeSp "+dischargeSp);
        
        valve.currentPosition = valvePosition < 0 ? 0 : Math.min(valvePosition, 100);
    }
    
    /**
     * Thermistor measurements interpret junk reading when they are actually not connected.
     * Avoid running the loop when the air temps are outside a reasonable range to make sure they are not picked by the
     * algorithm.
     */
    private boolean isSupplyAirTempValid(double sat) {
        return !(sat < 0) && !(sat > 200);
    }
    
    private void updateReheatDuringSystemHeating(Equip equip) {
        
        double valveStartDamperPercent = TunerUtil.readTunerValByQuery("vav and valve and start and damper and equipRef == \""+equip.getId()+"\"");
        double maxHeatingPos = vavEquip.getMaxHeatingDamperPos().readDefaultVal();
        double minHeatingPos = vavEquip.getMinHeatingDamperPos().readPriorityVal();
        double valveStart = minHeatingPos + (maxHeatingPos - minHeatingPos) * valveStartDamperPercent / 100;
        CcuLog.d(L.TAG_CCU_ZONE," valveStartDamperPercent "+valveStartDamperPercent+" valveStart "+valveStart );
        if (damper.currentPosition > valveStart) {
            valve.currentPosition = (int) ((damper.currentPosition - valveStart) * 100 / (maxHeatingPos - valveStart));
        } else {
            valve.currentPosition = 0;
        }
    }
    
    private void updateIaqCompensatedMinDamperPos(Integer node, Equip equip) {

        boolean  enabledCO2Control = vavEquip.getEnableCo2Control().readDefaultVal() > 0;
        if (enabledCO2Control) { CcuLog.e(L.TAG_CCU_ZONE, "DCV Tuners: co2Target " + co2Loop.getCo2Target() + ", co2Threshold " + co2Loop.getCo2Threshold()); }

        String zoneId = HSUtil.getZoneIdFromEquipId(equip.getId());
        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied())
                           || (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING);
        
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if(epidemicState != EpidemicState.OFF && L.ccu().oaoProfile != null) {
            double smartPurgeDABDamperMinOpenMultiplier = TunerUtil.readTunerValByQuery("purge and system and vav and damper and pos and min and multiplier", L.ccu().oaoProfile.getEquipRef());
            damper.iaqCompensatedMinPos = (int)(damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        } else {
            damper.iaqCompensatedMinPos = damper.minPosition;
        }
        //CO2 loop output from 0-50% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(vavEquip.getZoneCO2().readHisVal()) > 0) {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, co2Loop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE,"CO2LoopOp :"+co2Loop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos);
        }
        
    }

    @Override
    public ZoneState getState() {
        return state;
    }
}
