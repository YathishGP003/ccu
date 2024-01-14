package a75f.io.logic.bo.building.vav;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.projecthaystack.UnknownRecException;

import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.domain.VavAcbEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavAcbUnit;
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

public class VavAcbProfile extends VavProfile
{

    public VavAcbProfile(String equipRef, Short nodeAddress) {
        super(equipRef, nodeAddress, ProfileType.VAV_ACB);
    }

    Valve chwValve;

    //TODO - Only for backward compatibility during development. Should be removed.
    public VavAcbProfile() {
        super(null, null, ProfileType.VAV_ACB);
    }
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_ACB;
    }

    //VAV damper and reheat coil control logic is implemented according to section 1.3.E.6 of
    //ASHRAE RP-1455: Advanced Control Sequences for HVAC Systems Phase I, Air Distribution and Terminal Systems
    @Override
    public void updateZonePoints() {
        
        if(mInterface != null)
        {
            mInterface.refreshView();
        }

        /*if (vavDeviceMap.get(node) == null) {
            addLogicalMap(node);
            CcuLog.d(L.TAG_CCU_ZONE, " Logical Map added for node " + node);
            continue;
        }*/
        if (isZoneDead()) {
            updateZoneDead();
            return;
        }
        initLoopVariables();
        double roomTemp = getCurrentTemp();
        boolean condensate = getCondensate();
        CcuLog.e(L.TAG_CCU_ZONE, "Condensate detected? " + condensate);

        int loopOp = 0;
        //If supply air temperature from air handler is greater than room temperature, Cooling shall be
        //locked out.
        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        Equip equip = new Equip.Builder()
                .setHashMap(CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddr + "\"")).build();
        CcuLog.e(L.TAG_CCU_ZONE, "");
        CcuLog.e(L.TAG_CCU_ZONE, "Run Zone algorithm for "+nodeAddr+" setTempCooling "+setTempCooling+
                "setTempHeating "+setTempHeating+" systemMode "+systemMode+" roomTemp "+roomTemp);
        if (roomTemp > setTempCooling && systemMode != SystemMode.OFF ) {
            //Zone is in Cooling
            if (state != COOLING) {
                handleCoolingChangeOver();
            }
            if (conditioning == SystemController.State.COOLING) {
                loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
                chwValve.currentPosition = condensate ? 0 : loopOp;
            } else {
                // Damper and CHW Valve go to minimum if system is in heating
                chwValve.currentPosition = 0;
            }
        } else if (roomTemp < setTempHeating && systemMode != SystemMode.OFF) {
            //Zone is in heating
            if (state != HEATING) {
                handleHeatingChangeOver();
            }
            chwValve.currentPosition = 0;
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

        if (systemMode == SystemMode.OFF || coolingLoop.getLoopOutput() == 0) {
            chwValve.currentPosition = 0;
        }

        if (TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), vavEquip.getId())) {
            updateDamperPosForTrueCfm(CCUHsApi.getInstance(), conditioning);
        }

        chwValve.applyLimits();

        vavEquip.getDamperCmd().writeHisVal(damper.currentPosition);
        ((VavAcbEquip)vavEquip).getChwValveCmd().writeHisVal(chwValve.currentPosition);
        ((VavAcbEquip)vavEquip).getChwShutOffValve().writeHisVal(getShutOffValveCmd());

        logLoopParams(nodeAddr, roomTemp, loopOp);

        updateTRResponse((short)nodeAddr);

        CcuLog.d(L.TAG_CCU_ZONE, "buildingLimitMaxBreached "+buildingLimitMaxBreached()+" buildingLimitMinBreached "+buildingLimitMinBreached());

        setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                : state == COOLING ? buildingLimitMaxBreached() : false));
        updateLoopParams();
    }

    private boolean getCondensate() {
        if (((VavAcbEquip)vavEquip).getThermistor2Type().readPriorityVal() > 0.0) {
            return ((VavAcbEquip)vavEquip).getCondensateNC().readHisVal() > 0.0;
        } else {
            return ((VavAcbEquip)vavEquip).getCondensateNO().readHisVal() > 0.0;
        }
    }

    private void logLoopParams(int node, double roomTemp, int loopOp) {

        CcuLog.d(L.TAG_CCU_ZONE,"CoolingLoop Op: "+coolingLoop.getLoopOutput());
        coolingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE,"HeatingLoop Op: "+heatingLoop.getLoopOutput());
        heatingLoop.dump();
        CcuLog.d(L.TAG_CCU_ZONE, "STATE :"+state+" ,loopOp: " + loopOp + " ,damper:" + damper.currentPosition
                                 +", chwValve:"+ chwValve.currentPosition);
    }
    
    private void updateZoneDead() {
        
        CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead "+nodeAddr+" roomTemp : "+vavEquip.getCurrentTemp().readHisVal());
        state = TEMPDEAD;
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
            ((VavAcbEquip)vavEquip).getChwShutOffValve().writeHisVal(0.0);
            ((VavAcbEquip)vavEquip).getChwValveCmd().writeHisVal(0.0);
            CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" + nodeAddr + "\"", (double) TEMPDEAD.ordinal());
        }
    }

    private double getShutOffValveCmd() {
        boolean shutOffValveCmd;
        if (((VavAcbEquip)vavEquip).getChwShutOffValve().readHisVal() > 0.0) {
            shutOffValveCmd = (chwValve.currentPosition > 0.0);
        } else {
            double relayActivationHysteresis = ((VavAcbEquip) vavEquip).getRelayActivationHysteresis().readPriorityVal();
            shutOffValveCmd = (chwValve.currentPosition > relayActivationHysteresis);
        }
        return shutOffValveCmd ? 1.0 : 0.0;
    }
    
    private void initLoopVariables() {
        
       /* vavDevice = vavDeviceMap.get(node);
        coolingLoop = vavDevice.getCoolingLoop();
        heatingLoop = vavDevice.getHeatingLoop();
        co2Loop = vavDeviceMap.get(node).getCo2Loop();
        vocLoop = vavDeviceMap.get(node).getVOCLoop();
        cfmControlLoop = Objects.requireNonNull(vavDeviceMap.get(node)).getCfmController();
        valveController = vavDevice.getValveController();
        setTempCooling = vavDevice.getDesiredTempCooling();
        setTempHeating = vavDevice.getDesiredTempHeating();
        VavUnit vavUnit = vavDevice.getVavUnit();
        damper = vavUnit.vavDamper;
        valve = vavUnit.reheatValve;*/
        vavEquip = new VavAcbEquip(equipRef);
        setDamperLimits( (short) nodeAddr, damper);
        chwValve = ((VavAcbUnit)vavUnit).chwValve;
        setTempCooling = vavEquip.getDesiredTempCooling().readPriorityVal();
        setTempHeating = vavEquip.getDesiredTempHeating().readPriorityVal();
    }
    
    private void handleCoolingChangeOver() {
        state = COOLING;
        coolingLoop.setEnabled();
        heatingLoop.setDisabled();
    }
    
    private void handleHeatingChangeOver() {
        state = HEATING;
        chwValve.currentPosition = 0;
        heatingLoop.setEnabled();
        coolingLoop.setDisabled();
    }
    
    private void handleDeadband() {
        
        state = DEADBAND;
        chwValve.currentPosition = 0;
        heatingLoop.setDisabled();
        coolingLoop.setDisabled();
    }
    
    /**
     * Thermistor measurements interpret junk reading when they are actually not connected.
     * Avoid running the loop when the air temps are outside a reasonable range to make sure they are not picked by the
     * algorithm.
     */
    private boolean isSupplyAirTempValid(double sat) {
        return !(sat < 0) && !(sat > 200);
    }
    
    private void updateIaqCompensatedMinDamperPos(Integer node, Equip equip) {
    

        boolean  enabledCO2Control = vavEquip.getEnableCo2Control().readDefaultVal() > 0;
        boolean  enabledIAQControl = vavEquip.getEnableIAQControl().readDefaultVal() > 0;
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
    
        //VOC loop output from 0-50% modulates damper min position.
        if (enabledIAQControl && occupied && vocLoop.getLoopOutput(vavEquip.getZoneVoc().readHisVal()) > 0) {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * Math.min(50, vocLoop.getLoopOutput()) / 50;
            CcuLog.d(L.TAG_CCU_ZONE,"VOCLoopOp :"+vocLoop.getLoopOutput()+", adjusted minposition "+damper.iaqCompensatedMinPos);
        }
        
    }

    // Zones do not have reheat coils, so they do not generate HWST requests
    @Override
    @JsonIgnore
    public TrimResponseRequest getHwstRequests(short node)
    {
        hwstResetRequest.currentRequests = 0;
        hwstResetRequest.handleRequestUpdate();
        return hwstResetRequest;
    }
    
    @Override
    public ZoneState getState() {
        return state;
    }
}
