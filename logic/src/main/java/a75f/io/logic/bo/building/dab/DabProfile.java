package a75f.io.logic.bo.building.dab;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;
import static a75f.io.logic.bo.util.CCUUtils.DEFAULT_COOLING_DESIRED;
import static a75f.io.logic.bo.util.CCUUtils.DEFAULT_HEATING_DESIRED;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.domain.api.Domain;
import a75f.io.domain.config.ProfileConfiguration;
import a75f.io.domain.equips.DabEquip;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabProfile extends ZoneProfile
{
    public int nodeAddr;
    ProfileType profileType;

    double damperPos= 0;

    GenericPIController damperController;
    String equipRef = null;

    CO2Loop co2Loop;
    VOCLoop  vocLoop;
    DabEquip dabEquip;


    double   co2Target = TunerConstants.ZONE_CO2_TARGET;
    double   co2Threshold = TunerConstants.ZONE_CO2_THRESHOLD;

    private boolean pendingTunerChange;
    public boolean hasPendingTunerChange() { return pendingTunerChange; }

    private ControlLoop heatingLoop;

    public static final String CARRIER_PROD = "carrier_prod";

    double co2;
    
    Damper damper = new Damper();

    private static final int LOOP_OP_MIDPOINT = 50;

    public DabProfile(short node) {
        profileType = getProfileType();
        nodeAddr = node;
        co2Loop = new CO2Loop();
        vocLoop = new VOCLoop();
        heatingLoop = new ControlLoop();
        init();
    }

    public void init() {

        pendingTunerChange = false;

        HashMap<Object, Object>  equipMap = CCUHsApi.getInstance().readEntity("equip and group == \"" + nodeAddr + "\"");

        if (equipMap != null && !equipMap.isEmpty())
        {
            equipRef = equipMap.get("id").toString();
            dabEquip = (DabEquip) Domain.getEquip(equipRef);
            if (dabEquip == null) {
                CcuLog.e(L.TAG_CCU_ZONE, "No domain equip found during init for equipRef: " + equipRef);
                dabEquip = new DabEquip(equipRef);
            }
            damperController = new GenericPIController();
            damperController.setMaxAllowedError(dabEquip.getDabTemperatureProportionalRange().readPriorityVal());
            damperController.setIntegralGain(dabEquip.getDabIntegralKFactor().readPriorityVal());
            damperController.setProportionalGain(dabEquip.getDabProportionalKFactor().readPriorityVal());
            damperController.setIntegralMaxTimeout((int) dabEquip.getDabTemperatureIntegralTime().readPriorityVal());

            co2Target = (int) dabEquip.getDabZoneCO2Target().readPriorityVal();
            co2Threshold = (int) dabEquip.getDabZoneCo2Threshold().readPriorityVal();

            co2Loop.setCo2Target(co2Target);
            co2Loop.setCo2Threshold(co2Threshold);


            heatingLoop.setProportionalSpread((int) dabEquip.getDabReheatTemperatureProportionalRange().readPriorityVal());
            heatingLoop.setIntegralMaxTimeout((int) dabEquip.getDabReheatTemperatureIntegralTime().readPriorityVal());
            heatingLoop.setProportionalGain(dabEquip.getDabReheatProportionalKFactor().readPriorityVal());
            heatingLoop.setIntegralGain(dabEquip.getDabReheatIntegralKFactor().readPriorityVal());
            CcuLog.d(L.TAG_CCU_ZONE,
                    "DAB Reheat Tuners: DabReheatProportionalKFactor " + dabEquip.getDabReheatProportionalKFactor() +
                            ", DabReheatIntegralKFactor " + dabEquip.getDabReheatIntegralKFactor() +
                            ", DabReheatTemperatureProportionalRange " + dabEquip.getDabReheatTemperatureProportionalRange() +
                            ", DabReheatTemperatureIntegralTime " + dabEquip.getDabReheatTemperatureIntegralTime());
        }

    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.DAB;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {
        return null;
    }
    
    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>(){{
            add((short)nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip() {
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \""+nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    public void setPendingTunerChange() { pendingTunerChange = true; }

    @Override
    public void updateZonePoints() {

        dabEquip = (DabEquip) Domain.getEquip(equipRef);
        if (dabEquip == null) {
            CcuLog.e(L.TAG_CCU_ZONE, "No domain equip found for equipRef: " + equipRef);
            dabEquip = new DabEquip(equipRef);
        }

        if (isRFDead()) {
            updateRFDead();
            return;
        }else if (isZoneDead()) {
            updateZoneDead();
            return;
        }
        
        double setTempCooling ;
        double setTempHeating ;
        CCUHsApi hayStack = CCUHsApi.getInstance();

        if (hayStack.isScheduleSlotExitsForRoom(dabEquip.getId())) {
            Double unoccupiedSetBack = hayStack.getUnoccupiedSetback(dabEquip.getId());
            CcuLog.d(TAG, "Schedule slot Not  exists for room:  DabEquip: " + dabEquip.getId() + "node address : " + nodeAddr);
            setTempCooling = DEFAULT_COOLING_DESIRED + unoccupiedSetBack;
            setTempHeating = DEFAULT_HEATING_DESIRED - unoccupiedSetBack;
            dabEquip.getDesiredTempHeating().writePointValue(setTempHeating);
            dabEquip.getDesiredTempCooling().writePointValue(setTempCooling);
            dabEquip.getDesiredTemp().writePointValue((setTempCooling + setTempHeating) / 2);
        } else {
            setTempCooling = dabEquip.getDesiredTempCooling().readPriorityVal();
            setTempHeating = dabEquip.getDesiredTempHeating().readPriorityVal();
        }


        double roomTemp = dabEquip.getCurrentTemp().readHisVal();
        GenericPIController damperOpController = damperController;

        if (hasPendingTunerChange()) refreshPITuners();
    
        co2Loop = getCo2Loop();
        vocLoop = getVOCLoop();
        
        co2 = dabEquip.getZoneCO2().readHisVal();

        heatingLoop = getHeatingLoop();

        SystemController.State conditioning = L.ccu().systemProfile.getSystemController().getSystemState();
        SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];

        int satConditioning = CCUHsApi.getInstance().readHisValByQuery("system and sat and conditioning").intValue();
        if (systemMode != SystemMode.OFF) {
            if (satConditioning > 0) {
                //Effective SAT conditioning is available. Run the PI loop based on that.
                //But setTemp is still determined based on the current system operating mode.
                double setTempOpMode = (conditioning == SystemController.State.COOLING) ? setTempCooling : setTempHeating;
                if (satConditioning == SystemController.EffectiveSatConditioning.SAT_COOLING.ordinal()) {
                    damperOpController.updateControlVariable(roomTemp, setTempOpMode);
                } else {
                    damperOpController.updateControlVariable(setTempOpMode, roomTemp);
                }
            } else {
                //Fall back to System-conditioning based PI loop.
                if (conditioning == SystemController.State.COOLING) {
                    damperOpController.updateControlVariable(roomTemp, setTempCooling);
                } else {
                    damperOpController.updateControlVariable(setTempHeating, roomTemp);
                }
            }

            if (dabEquip.getReheatType().readDefaultVal() > 0) {
                handleReheat(setTempHeating, roomTemp);
                heatingLoop.dump();
            }

        } else {
            damperOpController.reset();
            heatingLoop.reset();
            dabEquip.getReheatCmd().writeHisVal(0.0);
        }

        updateZoneState(roomTemp, setTempCooling, setTempHeating);

        CcuLog.d(L.TAG_CCU_ZONE, "DAB-"+nodeAddr+" : roomTemp " + roomTemp
                + " setTempCooling:  " + setTempCooling+" setTempHeating: "+setTempHeating
                + " satConditioning "+satConditioning);
        CcuLog.i(L.TAG_CCU_ZONE, "PI Tuners: proportionalGain " + damperController.getProportionalGain() + ", integralGain " + damperController.getIntegralGain() +
                ", proportionalSpread " + damperController.getMaxAllowedError() + ", integralMaxTimeout " + damperController.getIntegralMaxTimeout());

        damperOpController.dump();

        //Loop Output varies from 0-100% such that, it is 50% at 0 error, 0% at maxNegative error, 100% at maxPositive
        //error
        double midPointBalancedLoopOp = LOOP_OP_MIDPOINT +
                damperOpController.getControlVariable() * LOOP_OP_MIDPOINT / damperOpController.getMaxAllowedError();

        setDamperLimits(damper, conditioning);
        updateDamperIAQCompensation();

        damper.currentPosition =
            (int)(damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * midPointBalancedLoopOp / 100);

        dabEquip.getDamper1Cmd().writeHisVal(damper.currentPosition);
        damperPos = damper.currentPosition;
        dabEquip.getDamper2Cmd().writeHisVal(damper.currentPosition);
        damperPos = damper.currentPosition;
        setStatus(state.ordinal(), DabSystemController.getInstance().isEmergencyMode() && (state == HEATING ? buildingLimitMinBreached()
                                                    : state == COOLING && buildingLimitMaxBreached()));
        CcuLog.d(L.TAG_CCU_ZONE, "System STATE :" + conditioning
                                 + " ZoneState : " + getState()
                                 + " ,CV: " + damperOpController.getControlVariable()
                                 +" , midPointBalancedLoopOp "+midPointBalancedLoopOp
                                 + " ,damper:" + damper.currentPosition);
    }

    private void updateZoneState(double roomTemp, double setTempCooling, double setTempHeating) {
        if (roomTemp > setTempCooling) {
            state = COOLING;
        } else if (roomTemp < setTempHeating) {
            state = HEATING;
        } else {
            state = DEADBAND;
        }

    }
    
    private void updateZoneDead() {
        CcuLog.d(L.TAG_CCU_ZONE,"Zone Temp Dead: "+nodeAddr+" roomTemp : "+dabEquip.getCurrentTemp());
        state = TEMPDEAD;
        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+nodeAddr+"\"");
        if (!curStatus.equals("Zone Temp Dead"))
        {
            dabEquip.getEquipStatusMessage().writeDefaultVal("Zone Temp Dead");
        
            SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            double damperMin = getDamperLimit(state == HEATING ? "heating":"cooling", "min");
            double damperMax = getDamperLimit(state == HEATING ? "heating":"cooling", "max");
        
            double damperPos =(damperMax+damperMin)/2;
            if(systemMode == SystemMode.OFF) {
                damperPos = getDamperPos() > 0 ? getDamperPos() : damperMin;
            }
            dabEquip.getDamper1Cmd().writeHisVal(damperPos);
            dabEquip.getDamper2Cmd().writeHisVal(damperPos);
            dabEquip.getNormalizedDamper1Cmd().writePointValue(damperPos);
            dabEquip.getNormalizedDamper2Cmd().writePointValue(damperPos);
            dabEquip.getReheatCmd().writeHisVal(0.0);
            dabEquip.getEquipStatus().writeHisVal(TEMPDEAD.ordinal());
        }
    }
    private void updateRFDead() {
        CcuLog.d(L.TAG_CCU_ZONE, RFDead+": " + nodeAddr);
        dabEquip.getEquipStatus().writeHisVal(RFDEAD.ordinal());
        dabEquip.getEquipStatusMessage().writeDefaultVal(RFDead);
    }
    private void updateDamperIAQCompensation() {
        boolean  enabledCO2Control = dabEquip.getEnableCo2Control().readDefaultVal() > 0 ;
        String zoneId = HSUtil.getZoneIdFromEquipId(dabEquip.getId());
        boolean occupied = ScheduleUtil.isZoneOccupied(CCUHsApi.getInstance(), zoneId, Occupancy.OCCUPIED);

        if (enabledCO2Control) { CcuLog.e(L.TAG_CCU_ZONE, "DCV Tuners: co2Target " + co2Loop.getCo2Target() + ", co2Threshold " + co2Loop.getCo2Threshold()); }

        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+L.ccu().systemProfile.getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if((epidemicState != EpidemicState.OFF) && (L.ccu().oaoProfile != null)) {
            double smartPurgeDABDamperMinOpenMultiplier = L.ccu().oaoProfile.getOAOEquip().getSystemPurgeDabDamperMinOpenMultiplier().readPriorityVal();
            damper.iaqCompensatedMinPos =(int) (damper.minPosition * smartPurgeDABDamperMinOpenMultiplier);
        } else
            damper.iaqCompensatedMinPos = damper.minPosition;
        //CO2 loop output from 0-100% modulates damper min position.
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0)
        {
            damper.iaqCompensatedMinPos = damper.iaqCompensatedMinPos + (damper.maxPosition - damper.iaqCompensatedMinPos) * co2Loop.getLoopOutput() / 100;
            CcuLog.d(L.TAG_CCU_ZONE, "CO2LoopOp :" + co2Loop.getLoopOutput() + ", adjusted minposition " + damper.iaqCompensatedMinPos);
        }
    }
    
    protected void setDamperLimits(Damper d, SystemController.State conditioning) {
        d.minPosition = (int)getDamperLimit(conditioning == SystemController.State.HEATING ? "heating":"cooling", "min");
        d.maxPosition = (int)getDamperLimit(conditioning == SystemController.State.HEATING ? "heating":"cooling", "max");
        d.iaqCompensatedMinPos = d.minPosition;
    }
    
    public double getCo2LoopOp() {
        return getCo2Loop().getLoopOutput();
    }
    
    @Override
    public void reset(){
        double damperMin = getDamperLimit(state == HEATING ? "heating":"cooling", "min");
        dabEquip.getDamper1Cmd().writeHisVal(damperMin);
        dabEquip.getDamper2Cmd().writeHisVal(damperMin);
        dabEquip.getCurrentTemp().writeHisVal(0);
    }

    private void handleReheat(double desiredTempHeating, double currentTemp) {
        double reheatOffset = TunerUtil.readTunerValByQuery("tuner and reheat and offset and equipRef == \"" +
                                            dabEquip.getId()+"\"");
        double heatingLoopOp = Math.max(0, heatingLoop.getLoopOutput(desiredTempHeating - reheatOffset, currentTemp));
        CcuLog.i(L.TAG_CCU_ZONE, "handleReheat : reheatOffset "+reheatOffset+" heatingLoopOp "+heatingLoopOp);
        if (isSystemFanOn()) {
            dabEquip.getReheatCmd().writeHisVal((int) heatingLoopOp);
        } else {
            CcuLog.i(L.TAG_CCU_ZONE, "handleReheat disabled. System Fan not active");
            heatingLoop.reset();
            dabEquip.getReheatCmd().writeHisVal(0.0);
        }
    }

    private boolean isSystemFanOn() {
        //This is short cut and not a way to do this. But currently required only for Dab profile.
        String systemStatusMessage = L.ccu().systemProfile.getStatusMessage();
        if (systemStatusMessage.contains("Fan"))
            return true;
        return L.ccu().systemProfile.systemFanLoopOp > 0;
    }

    public static String getDisName(String siteDis, String nodeAddr){
        String tempEquipDis = siteDis+"-DAB-"+nodeAddr;
        if(BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD)){
            tempEquipDis = tempEquipDis.replaceAll("(?i)-DAB-","-VVT-");
        }
        return tempEquipDis;
    }

    public void refreshPITuners() {
        DabEquip dabEquip = new DabEquip(equipRef);
        if (equipRef != null) {
            damperController.setMaxAllowedError(dabEquip.getDabTemperatureProportionalRange().readPriorityVal());
            damperController.setIntegralGain(dabEquip.getDabIntegralKFactor().readPriorityVal());
            damperController.setProportionalGain(dabEquip.getDabProportionalKFactor().readPriorityVal());
            damperController.setIntegralMaxTimeout((int) dabEquip.getDabTemperatureIntegralTime().readPriorityVal());

            co2Target = (int) dabEquip.getDabZoneCO2Target().readPriorityVal();
            co2Threshold = (int)  dabEquip.getDabZoneCo2Threshold().readPriorityVal();
            co2Loop.setCo2Target(co2Target);
            co2Loop.setCo2Threshold(co2Threshold);
            heatingLoop.setProportionalSpread((int) dabEquip.getDabReheatTemperatureProportionalRange().readPriorityVal());
            heatingLoop.setIntegralMaxTimeout((int) dabEquip.getDabReheatTemperatureIntegralTime().readPriorityVal());
            heatingLoop.setProportionalGain(dabEquip.getDabReheatProportionalKFactor().readPriorityVal());
            heatingLoop.setIntegralGain(dabEquip.getDabReheatIntegralKFactor().readPriorityVal());
            pendingTunerChange = false;
        }
    }

    public double getDamperPos()
    {
        return damperPos;
    }

    public CO2Loop getCo2Loop()
    {
        return co2Loop;
    }
    public VOCLoop getVOCLoop()
    {
        return vocLoop;
    }

    public ControlLoop getHeatingLoop () {
        return heatingLoop;
    }

    public void setStatus(double status, boolean emergency) {
        if (getStatus() != status ) {
            dabEquip.getEquipStatus().writeHisVal(status);
        }

        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" : "Emergency Heating");
        } else
        {
            if (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING) {
                message = "In Preconditioning ";
            } else
            {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" : "Warming Space");
            }
        }

        String curStatus = dabEquip.getEquipStatusMessage().readDefaultStrVal();
        if (!curStatus.equals(message))
        {
            dabEquip.getEquipStatusMessage().writeDefaultVal(message);
        }
    }

    public double getDamperLimit(String coolHeat, String minMax)
    {
        if(minMax.equals("min") && coolHeat.equals("cooling")) {
            return dabEquip.getMinCoolingDamperPos().readPriorityVal();
        } else if(minMax.equals("max") && coolHeat.equals("cooling")) {
            return dabEquip.getMaxCoolingDamperPos().readPriorityVal();
        } else if(minMax.equals("min") && coolHeat.equals("heating")) {
            return dabEquip.getMinHeatingDamperPos().readPriorityVal();
        } else if(minMax.equals("max") && coolHeat.equals("heating")) {
            return dabEquip.getMaxHeatingDamperPos().readPriorityVal();
        }
        return 0;
    }

    public double getStatus() {
        return dabEquip.getEquipStatus().readHisVal();
    }

    @Override
    public ProfileConfiguration getDomainProfileConfiguration() {
        Equip equip = getEquip();
        NodeType nodeType = equip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
        return new DabProfileConfiguration(nodeAddr, nodeType.name(),
                (int) dabEquip.getZonePriority().readPriorityVal(),
                equip.getRoomRef(),
                equip.getFloorRef() ,
                profileType,
                (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()))
                .getActiveConfiguration();

    }
}
