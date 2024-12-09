package a75f.io.logic.bo.building.vav;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.projecthaystack.UnknownRecException;

import a75.io.algos.tr.TrimResponseRequest;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.domain.VavAcbEquip;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.config.ProfileConfiguration;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavAcbUnit;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.tuners.TunerUtil;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavAcbProfile extends VavProfile
{

    public VavAcbProfile(String equipRef, Short nodeAddress) {
        super(equipRef, nodeAddress, ProfileType.VAV_ACB);
    }

    Valve chwValve;

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
        if (isRFDead()) {
            handleRFDead();
            return;
        }else if (isZoneDead()) {
            updateZoneDead();
            return;
        }
        initLoopVariables();
        double roomTemp = getCurrentTemp();
        boolean condensate = getCondensate();
        CcuLog.d(L.TAG_CCU_ZONE, "Condensate detected? " + condensate);

        int loopOp = 0;
        //If supply air temperature from air handler is greater than room temperature, Cooling shall be
        //locked out.
        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
        SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal("conditioning and mode")];
        Equip equip = new Equip.Builder()
                .setHashMap(CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddr + "\"")).build();
        CcuLog.d(L.TAG_CCU_ZONE, "Run Zone algorithm for "+nodeAddr+" setTempCooling "+setTempCooling+
                "setTempHeating "+setTempHeating+" systemMode "+systemMode+" roomTemp "+roomTemp);

        CcuLog.d(L.TAG_CCU_ZONE, "PI Tuners: proportionalGain " + proportionalGain + ", integralGain " + integralGain +
                ", proportionalSpread " + proportionalSpread + ", integralMaxTimeout " + integralMaxTimeout);
        if (vavEquip.getEnableCFMControl().readPriorityVal() > 0) {
            CcuLog.d(L.TAG_CCU_ZONE, "CFM PI Tuners: cfmProportionalGain " + cfmController.getProportionalGain() + ", cfmIntegralGain " + cfmController.getIntegralGain() +
                    ", cfmProportionalSpread " + cfmController.getProportionalSpread() + ", cfmIntegralMaxTimeout " + cfmController.getIntegralMaxTimeout());
        }

        if (roomTemp > setTempCooling && systemMode != SystemMode.OFF ) {
            //Zone is in Cooling
            if (state != COOLING) {
                handleCoolingChangeOver();
            }
            if (conditioning == SystemController.State.COOLING) {
                loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
                chwValve.currentPosition = condensate ? 0 : loopOp;
                vavEquip.getCoolingLoopOutput().writePointValue(loopOp);
                loopOp = (int) vavEquip.getCoolingLoopOutput().readHisVal();
            } else {
                // Damper and CHW Valve go to minimum if system is in heating
                chwValve.currentPosition = 0;
            }
        } else if (roomTemp < setTempHeating && systemMode != SystemMode.OFF && ((VavAcbEquip)vavEquip).getValveType().readDefaultVal() > 0) {
            //Zone is in heating
            if (state != HEATING) {
                handleHeatingChangeOver();
            }
            chwValve.currentPosition = 0;
        } else {
            //Zone is in deadband
            handleDeadband();
            if (coolingLoop.getEnabled()) {
                loopOp = (int) coolingLoop.getLoopOutput(roomTemp, setTempCooling);
            }
        }

        try {
            updateIaqCompensatedMinDamperPos(nodeAddr, equip);
        } catch (UnknownRecException e) {
            CcuLog.e(L.TAG_CCU_ZONE, "IaqCompensation cannot be performed ", e);
        }
        damper.currentPosition = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * loopOp / 100;
        damper.currentPosition = Math.max(damper.currentPosition, damper.minPosition);
        damper.currentPosition = Math.min(damper.currentPosition, damper.maxPosition);
        CcuLog.d(L.TAG_CCU_ZONE,"AcbLoopOp :"+loopOp+", adjusted minposition "+damper.iaqCompensatedMinPos+","+damper.currentPosition);

        if (systemMode == SystemMode.OFF || coolingLoop.getLoopOutput() == 0) {
            chwValve.currentPosition = 0;
        }

        if (TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), vavEquip.getId())) {
            updateDamperPosForTrueCfm(CCUHsApi.getInstance(), conditioning);
        }

        chwValve.applyLimits();

        vavEquip.getDamperCmd().writeHisVal(damper.currentPosition);
        ((VavAcbEquip)vavEquip).getChwValveCmd().writeHisVal(chwValve.currentPosition);
        if (((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNO().pointExists()) {
            ((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNO().writeHisVal(getShutOffValveCmdNO());
        } else {
            ((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNC().writeHisVal(getShutOffValveCmdNC());
        }

        logLoopParams(loopOp);

        updateTRResponse((short)nodeAddr);

        CcuLog.d(L.TAG_CCU_ZONE, "buildingLimitMaxBreached "+buildingLimitMaxBreached()+" buildingLimitMinBreached "+buildingLimitMinBreached());

        setStatus(state.ordinal(), VavSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                : state == COOLING ? buildingLimitMaxBreached() : false));
        updateLoopParams();
    }

    private void handleRFDead() {
        vavEquip.getEquipStatus().writeHisVal(RFDEAD.ordinal());
        vavEquip.getEquipStatusMessage().writeDefaultVal(RFDead);
    }

    private boolean getCondensate() {
        if (((VavAcbEquip)vavEquip).getThermistor2Type().readPriorityVal() > 0.0) {
            return CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.condensateNC + "\" and group == \"" + nodeAddr + "\"") > 0.0;
        } else {
            return CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.condensateNO + "\" and group == \"" + nodeAddr + "\"") > 0.0;
        }
    }

    private void logLoopParams(int loopOp) {

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
            if (((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNO().pointExists()) {
                ((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNO().writeHisVal(0.0);
            } else {
                ((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNC().writeHisVal(0.0);
            }

            ((VavAcbEquip)vavEquip).getChwValveCmd().writeHisVal(0.0);
            vavEquip.getEquipStatus().writeHisVal(TEMPDEAD.ordinal());
            vavEquip.getEquipStatusMessage().writeDefaultVal("Zone Temp Dead");}
    }

    private double getShutOffValveCmdNO() {
        boolean shutOffValveCmd;
        if (((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNO().readHisVal() > 0.0) {
            shutOffValveCmd = (chwValve.currentPosition > 0.0);
        } else {
            double relayActivationHysteresis = ((VavAcbEquip) vavEquip).getRelayActivationHysteresis().readPriorityVal();
            if (relayActivationHysteresis == 0.0) { relayActivationHysteresis = 10.0; }
            shutOffValveCmd = (chwValve.currentPosition > relayActivationHysteresis);
        }
        return shutOffValveCmd ? 1.0 : 0.0;
    }

    private double getShutOffValveCmdNC() {
        boolean shutOffValveCmd;
        if (((VavAcbEquip)vavEquip).getChilledWaterValveIsolationCmdPointNC().readHisVal() > 0.0) {
            shutOffValveCmd = (chwValve.currentPosition > 0.0);
        } else {
            double relayActivationHysteresis = ((VavAcbEquip) vavEquip).getRelayActivationHysteresis().readPriorityVal();
            if (relayActivationHysteresis == 0.0) { relayActivationHysteresis = 10.0; }
            shutOffValveCmd = (chwValve.currentPosition > relayActivationHysteresis);
        }
        return shutOffValveCmd ? 1.0 : 0.0;
    }
    
    private void initLoopVariables() {
        chwValve = ((VavAcbUnit)vavUnit).chwValve;
        setTempCooling = vavEquip.getDesiredTempCooling().readPriorityVal();
        setTempHeating = vavEquip.getDesiredTempHeating().readPriorityVal();

        if (hasPendingTunerChange()) refreshPITuners();

        setDamperLimits( (short) nodeAddr, damper);
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
        if (enabledCO2Control) { CcuLog.e(L.TAG_CCU_ZONE, "DCV Tuners: co2Target " + co2Loop.getCo2Target() + ", co2Threshold " + co2Loop.getCo2Threshold()); }

        String zoneId = HSUtil.getZoneIdFromEquipId(equip.getId());
        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied())
                           || (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING);
        
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if(epidemicState != EpidemicState.OFF && L.ccu().oaoProfile != null) {
            double smartPurgeDABDamperMinOpenMultiplier = L.ccu().oaoProfile.getOAOEquip().getSystemPurgeVavDamperMinOpenMultiplier().readPriorityVal();
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


    @Override
    public ProfileConfiguration getDomainProfileConfiguration() {
        Equip equip = getEquip();
        NodeType nodeType = equip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
        return new AcbProfileConfiguration(nodeAddr, nodeType.name(),
                (int) vavEquip.getZonePriority().readPriorityVal(),
                equip.getRoomRef(),
                equip.getFloorRef() ,
                profileType,
                (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()))
                .getActiveConfiguration();

    }

    @Override
    public void handleDeadband() {
        if (state != DEADBAND) {
            deadbandTransitionState = state;
            state = DEADBAND;
        }
        chwValve.currentPosition = 0;
    }
}
