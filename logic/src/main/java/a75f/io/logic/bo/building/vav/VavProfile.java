package a75f.io.logic.bo.building.vav;

import static a75f.io.logic.bo.building.ZonePriority.NONE;
import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75.io.algos.CO2Loop;
import a75.io.algos.ControlLoop;
import a75.io.algos.GenericPIController;
import a75.io.algos.VOCLoop;
import a75.io.algos.tr.TrimResetListener;
import a75.io.algos.tr.TrimResponseRequest;
import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.domain.VavAcbEquip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.domain.VavEquip;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.config.ProfileConfiguration;
import a75f.io.domain.logic.DeviceBuilder;
import a75f.io.domain.logic.EntityMapper;
import a75f.io.domain.logic.ProfileEquipBuilder;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.VavAcbUnit;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemProfile;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

import org.projecthaystack.HDict;

/**
 *
 * Created by samjithsadasivan on 5/31/18.
 */

public abstract class VavProfile extends ZoneProfile {
    
    public static String TAG = VavProfile.class.getSimpleName().toUpperCase();
    public static final int REHEAT_THRESHOLD_HEATING_LOOP = 50;
    
    //public HashMap<Short, VavEquip> vavDeviceMap;
    SatResetListener satResetListener;
    CO2ResetListener co2ResetListener;
    SpResetListener spResetListener;
    HwstResetListener hwstResetListener;
    VavTRSystem trSystem = null;
    
    double setTempCooling;
    double setTempHeating;
    
    boolean exceedsThreeDegree = false;
    boolean exceedsFiveDegree = false;
    
    //VavEquip           vavDevice;
    ControlLoop         coolingLoop;
    ControlLoop         heatingLoop;
    CO2Loop             co2Loop;
    VOCLoop             vocLoop;
    GenericPIController valveController;
    Damper              damper;
    Valve valve;
    
    private SystemController.State cfmLoopState;

    a75f.io.domain.VavEquip vavEquip;

    int    integralMaxTimeout = 30;
    int proportionalSpread = 20;
    double proportionalGain = 0.2;
    double integralGain = 0.8;
    double dischargeSp;

    VavUnit             vavUnit;

    ControlLoop cfmController;

    public TrimResponseRequest satResetRequest;
    public TrimResponseRequest co2ResetRequest;
    public TrimResponseRequest spResetRequest;
    public TrimResponseRequest hwstResetRequest;

    int nodeAddr;
    ProfileType profileType;

    double co2Target = TunerConstants.ZONE_CO2_TARGET;
    double co2Threshold = TunerConstants.ZONE_CO2_THRESHOLD;
    double vocTarget = TunerConstants.ZONE_VOC_TARGET;
    double vocThreshold = TunerConstants.ZONE_VOC_THRESHOLD;
    CCUHsApi hayStack= CCUHsApi.getInstance();
    String equipRef = null;
        
    public VavProfile(String equipRef, Short addr, ProfileType profileType) {
        CcuLog.i(L.TAG_CCU_ZONE, "VavProfile ");
        this.profileType = profileType;
        coolingLoop = new ControlLoop();
        heatingLoop = new ControlLoop();
        co2Loop = new CO2Loop();
        vocLoop = new VOCLoop();
        valveController = new GenericPIController();
        valveController.setIntegralMaxTimeout(integralMaxTimeout);
        valveController.setMaxAllowedError(proportionalSpread);
        valveController.setProportionalGain(proportionalGain);
        valveController.setIntegralGain(integralGain);

        satResetRequest = new TrimResponseRequest();
        co2ResetRequest = new TrimResponseRequest();
        spResetRequest = new TrimResponseRequest();
        hwstResetRequest = new TrimResponseRequest();

        nodeAddr = addr;

        //createHaystackPoints();
        cfmController = new ControlLoop();

        //vavDeviceMap = new HashMap<>();
        satResetListener = new SatResetListener();
        co2ResetListener = new CO2ResetListener();
        spResetListener = new SpResetListener();
        hwstResetListener = new HwstResetListener();
        //initTRSystem();
        this.equipRef = equipRef;

        init();
        CcuLog.i(L.TAG_CCU_ZONE, "VavProfile Done");
    }

    public void init() {
        CcuLog.i(L.TAG_CCU_ZONE, "VavProfile Init");
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");
        equipRef = equipMap.get("id").toString();

        switch (profileType) {
            case VAV_REHEAT:
                vavUnit = new VavUnit();
                vavEquip = new VavEquip(equipRef);
                break;
            case VAV_SERIES_FAN:
                vavUnit = new SeriesFanVavUnit();
                vavEquip = new VavEquip(equipRef);
                break;
            case VAV_PARALLEL_FAN:
                vavUnit = new ParallelFanVavUnit();
                vavEquip = new VavEquip(equipRef);
                break;
            case VAV_ACB:
                vavUnit = new VavAcbUnit();
                vavEquip = new VavAcbEquip(equipRef);
        }

        if (equipMap != null && equipMap.size() > 0) {
            String equipId = equipMap.get("id").toString();
            proportionalGain = vavEquip.getVavProportionalKFactor().readPriorityVal();
            integralGain = vavEquip.getVavIntegralKfactor().readPriorityVal();
            proportionalSpread = (int) vavEquip.getVavTemperatureProportionalRange().readPriorityVal();
            integralMaxTimeout = (int) vavEquip.getVavTemperatureIntegralTime().readPriorityVal();

            co2Target = (int) vavEquip.getVavZoneCo2Target().readPriorityVal();
            co2Threshold = (int) vavEquip.getVavZoneCo2Threshold().readPriorityVal();
            vocTarget = (int) vavEquip.getVavZoneVocTarget().readPriorityVal();
            vocThreshold = (int) vavEquip.getVavZoneVocThreshold().readPriorityVal();

            CcuLog.i(L.TAG_CCU_ZONE,"node "+nodeAddr+" proportionalGain "+proportionalGain
                    +" integralGain "+integralGain
                    +" proportionalSpread "+proportionalSpread
                    +" integralMaxTimeout "+integralMaxTimeout
                    +" co2Target "+co2Target
                    +" co2Threshold "+co2Threshold
                    +" vocTarget "+vocTarget
                    +" vocThreshold "+vocThreshold
                    +" integralMaxTimeout "+integralMaxTimeout);

            initializeCfmController(equipId);
        }

        coolingLoop.setProportionalGain(proportionalGain);
        coolingLoop.setIntegralGain(integralGain);
        coolingLoop.setProportionalSpread(proportionalSpread);
        coolingLoop.setIntegralMaxTimeout(integralMaxTimeout);
        coolingLoop.reset();

        heatingLoop.setProportionalGain(proportionalGain);
        heatingLoop.setIntegralGain(integralGain);
        heatingLoop.setProportionalSpread(proportionalSpread);
        heatingLoop.setIntegralMaxTimeout(integralMaxTimeout);
        heatingLoop.reset();

        co2Loop.setCo2Target(co2Target);
        co2Loop.setCo2Threshold(co2Threshold);
        vocLoop.setVOCTarget(vocTarget);
        vocLoop.setVOCThreshold(vocThreshold);

        satResetRequest.setImportanceMultiplier(ZonePriority.NORMAL.multiplier);
        co2ResetRequest.setImportanceMultiplier(ZonePriority.NORMAL.multiplier);
        spResetRequest.setImportanceMultiplier(ZonePriority.NORMAL.multiplier);
        hwstResetRequest.setImportanceMultiplier(ZonePriority.NORMAL.multiplier);

        vavUnit.vavDamper.minPosition = 0;
        vavUnit.vavDamper.maxPosition = 100;
        damper = vavUnit.vavDamper; //This is not necessary. Keeping to maintain legacy code.
        valve = vavUnit.reheatValve;
        CcuLog.i(L.TAG_CCU_ZONE, "VavProfile Init Done");
    }

    public void refreshPITuners() {

        proportionalGain = vavEquip.getVavProportionalKFactor().readPriorityVal();
        integralGain = vavEquip.getVavIntegralKfactor().readPriorityVal();
        proportionalSpread = (int) vavEquip.getVavTemperatureProportionalRange().readPriorityVal();
        integralMaxTimeout = (int) vavEquip.getVavTemperatureIntegralTime().readPriorityVal();

        coolingLoop.setProportionalGain(proportionalGain);
        coolingLoop.setIntegralGain(integralGain);
        coolingLoop.setProportionalSpread(proportionalSpread);
        coolingLoop.setIntegralMaxTimeout(integralMaxTimeout);

        heatingLoop.setProportionalGain(proportionalGain);
        heatingLoop.setIntegralGain(integralGain);
        heatingLoop.setProportionalSpread(proportionalSpread);
        heatingLoop.setIntegralMaxTimeout(integralMaxTimeout);

        double cfmProportionalGain = (vavEquip.getVavAirflowCFMProportionalKFactor().readPriorityVal() > 0) ? vavEquip.getVavAirflowCFMProportionalKFactor().readPriorityVal() : 0.5;
        double cfmIntegralGain = (vavEquip.getVavAirflowCFMIntegralKFactor().readPriorityVal() > 0) ? vavEquip.getVavAirflowCFMIntegralKFactor().readPriorityVal() : 0.5;
        int cfmProportionalSpread = (vavEquip.getVavAirflowCFMProportionalRange().readPriorityVal() > 0.0) ? (int)vavEquip.getVavAirflowCFMProportionalRange().readPriorityVal(): 200;
        int cfmIntegralMaxTimeout = (vavEquip.getVavAirflowCFMIntegralTime().readPriorityVal() > 0.0) ? (int)vavEquip.getVavAirflowCFMIntegralTime().readPriorityVal() : 30;

        cfmController.setProportionalGain(cfmProportionalGain);
        cfmController.setIntegralGain(cfmIntegralGain);
        cfmController.setProportionalSpread(cfmProportionalSpread);
        cfmController.setIntegralMaxTimeout(cfmIntegralMaxTimeout);

        co2Target = (int) vavEquip.getVavZoneCo2Target().readPriorityVal();
        co2Threshold = (int) vavEquip.getVavZoneCo2Threshold().readPriorityVal();
        vocTarget = (int) vavEquip.getVavZoneVocTarget().readPriorityVal();
        vocThreshold = (int) vavEquip.getVavZoneVocThreshold().readPriorityVal();

        co2Loop.setCo2Target(co2Target);
        co2Loop.setCo2Threshold(co2Threshold);
        vocLoop.setVOCTarget(vocTarget);
        vocLoop.setVOCThreshold(vocThreshold);

    }

    private void initializeCfmController(String equipId) {
        CcuLog.i(L.TAG_CCU_ZONE, "VavProfile initializeCfmController");
        double cfmProportionalGain = (vavEquip.getVavAirflowCFMProportionalKFactor().readPriorityVal() > 0) ? vavEquip.getVavAirflowCFMProportionalKFactor().readPriorityVal() : 0.5;
        double cfmIntegralGain = (vavEquip.getVavAirflowCFMIntegralKFactor().readPriorityVal() > 0) ? vavEquip.getVavAirflowCFMIntegralKFactor().readPriorityVal() : 0.5;
        int cfmProportionalSpread = (vavEquip.getVavAirflowCFMProportionalRange().readPriorityVal() > 0.0) ? (int)vavEquip.getVavAirflowCFMProportionalRange().readPriorityVal(): 200;
        int cfmIntegralMaxTimeout = (vavEquip.getVavAirflowCFMIntegralTime().readPriorityVal() > 0.0) ? (int)vavEquip.getVavAirflowCFMIntegralTime().readPriorityVal() : 30;

        CcuLog.i(L.TAG_CCU_ZONE,"node "+nodeAddr+" cfmProportionalGain "+cfmProportionalGain
                +" cfmIntegralGain "+cfmIntegralGain
                +" cfmProportionalSpread "+cfmProportionalSpread
                +" cfmIntegralMaxTimeout "+cfmIntegralMaxTimeout);
        cfmController.setProportionalGain(cfmProportionalGain);
        cfmController.setIntegralGain(cfmIntegralGain);
        cfmController.setProportionalSpread(cfmProportionalSpread);
        cfmController.setIntegralMaxTimeout(cfmIntegralMaxTimeout);
        cfmController.reset();
        CcuLog.i(L.TAG_CCU_ZONE, "VavProfile initializeCfmController Done");
    }
    
    public void initTRSystem() {
        trSystem = (VavTRSystem) L.ccu().systemProfile.trSystem;
        trSystem.getSystemSATTRProcessor().addTRListener(satResetListener);
        trSystem.getSystemCO2TRProcessor().addTRListener(co2ResetListener);
        trSystem.getSystemSpTRProcessor().addTRListener(spResetListener);
        trSystem.getSystemHwstTRProcessor().addTRListener(hwstResetListener);
    }
    
    public void updateTRResponse(short node) {
        if (L.ccu().systemProfile instanceof VavSystemProfile) {
            if (trSystem == null) {
                initTRSystem();
            }
            VavTRSystem trSystem = (VavTRSystem) L.ccu().systemProfile.trSystem;
            trSystem.updateSATRequest(getSATRequest(node));
            trSystem.updateCO2Request(getCO2Requests(node));
            Log.i(L.TAG_CCU_ZONE, "Damper Position before T&R: " + damper.currentPosition);
            trSystem.updateSpRequest(getSpRequests(node));
            trSystem.updateHwstRequest(getHwstRequests(node));
        }
    }
    
    @Override
    public void updateZonePoints() {
        Log.d(TAG, " Invalid VAV Unit Type");
    }
    
    protected void setDamperLimits(short node, Damper d) {
        if (vavEquip.getEnableCFMControl().readDefaultVal() > 0.0) {
            d.minPosition = 0;
            d.maxPosition = 100;
        } else {
            d.minPosition = (int) (state == HEATING ? vavEquip.getMinHeatingDamperPos().readDefaultVal()
                    : vavEquip.getMinCoolingDamperPos().readDefaultVal());
            d.maxPosition = (int) (state == HEATING ? vavEquip.getMaxHeatingDamperPos().readDefaultVal()
                    : vavEquip.getMaxCoolingDamperPos().readDefaultVal());
        }

        d.iaqCompensatedMinPos = d.minPosition;
    }

    @JsonIgnore
    public TrimResponseRequest getSATRequest(short node) {

        double roomTemp = getCurrentTemp();
        
        if (state == COOLING) {
            if (coolingLoop.getLoopOutput() < 85) {
                satResetRequest.currentRequests = 0;
            }
            if (coolingLoop.getLoopOutput() > 95) {
                satResetRequest.currentRequests = 1;
            }
            if ((roomTemp - setTempCooling) >= 3 ) {
                if (exceedsThreeDegree) {
                    satResetRequest.currentRequests = 2;
                } else {
                    exceedsThreeDegree = true;
                }
            } else {
                exceedsThreeDegree = false;
            }
            if ((roomTemp - setTempCooling) >= 5) {
                if (exceedsFiveDegree) {
                    satResetRequest.currentRequests = 3;
                } else {
                    exceedsFiveDegree = true;
                }
            } else {
                exceedsFiveDegree = false;
            }
        } else {
            satResetRequest.currentRequests = 0;
        }
        satResetRequest.handleRequestUpdate();
        return satResetRequest;
    }
    
    @JsonIgnore
    public TrimResponseRequest getCO2Requests(short node) {
        int co2LoopOp = co2Loop.getLoopOutput();
        if (co2LoopOp == 100) {
            co2ResetRequest.currentRequests = 4;
        } else if (co2LoopOp > 90){
            co2ResetRequest.currentRequests = 3;
        } else if (co2LoopOp > 75) {
            co2ResetRequest.currentRequests = 2;
        } else if (co2LoopOp > 60) {
            co2ResetRequest.currentRequests = 1;
        } else if (co2LoopOp < 50) {
            co2ResetRequest.currentRequests = 0;
        }
        co2ResetRequest.handleRequestUpdate();
        return co2ResetRequest;
        
    }
    
    @JsonIgnore
    public TrimResponseRequest getSpRequests(short node)
    {

        Damper d = vavUnit.vavDamper;
        int damperLoopOp = d.currentPosition;
        if (damperLoopOp > 95) {
            spResetRequest.currentRequests = 3;
        } else if (damperLoopOp > 85) {
            spResetRequest.currentRequests = 2;
        } else if (damperLoopOp > 75) {
            spResetRequest.currentRequests = 1;
        } else if (damperLoopOp < 65) {
            spResetRequest.currentRequests = 0;
        }
        spResetRequest.handleRequestUpdate();
        return spResetRequest;
    }
    
    @JsonIgnore
    public TrimResponseRequest getHwstRequests(short node)
    {

        Valve v = vavUnit.reheatValve;
        double sat = vavEquip.getEnteringAirTemp().readHisVal();

        if (state == HEATING) {
            if ((setTempHeating - sat) > 30) { // TODO- 5 mins
                hwstResetRequest.currentRequests = 3;
            }
            else if ((setTempHeating - sat) > 15) { //TODO - 5 mins
                hwstResetRequest.currentRequests = 2;
            }
            else if (v.currentPosition > 95) {
                hwstResetRequest.currentRequests = 1;
            }
            else if (v.currentPosition < 85) {
                hwstResetRequest.currentRequests = 0;
            }
        } else {
            hwstResetRequest.currentRequests = 0;
        }
        hwstResetRequest.handleRequestUpdate();
        return hwstResetRequest;
    }

    public int getConditioningMode() {
        return state.ordinal();
    }

    public void handleSystemReset() {
        Log.d("VAV","handleSystemReset");
    }

    public double getDisplayCurrentTemp()
    {
        return getAverageZoneTemp();
    }
    

    @Override
    public double getAverageZoneTemp()
    {
        return 0;
    }

    public Set<Short> getNodeAddresses() {
        //TODO
        Set<Short> nodeSet = new HashSet();
        nodeSet.add((short)nodeAddr);
        return nodeSet;
    }

    // This method does not look like it would work post-DM migration.
    // As far as I can see, it's not used anywhere? This should be investigated further.
    @Override
    public ZonePriority getPriority() {
        ZonePriority priority = NONE;
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddr+"\"");
        if (ZonePriority.valueOf(equip.get("priorityLevel").toString()).ordinal() > priority.ordinal()) {
            priority = ZonePriority.valueOf(equip.get("priorityLevel").toString());
        }
        return priority;
    }
    @Override
    public double getCurrentTemp() {
        return vavEquip.getCurrentTemp().readHisVal();
    }

    @Override
    public Equip getEquip() {
        HDict equip = CCUHsApi.getInstance().readHDict("equip and group == \""+nodeAddr+"\"");
        return new Equip.Builder().setHDict(equip).build();
    }
    @Override
    public <T extends BaseProfileConfiguration> T getProfileConfiguration(short address) {
        return null;
    }
    
    class SatResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleSATReset");
            satResetRequest.handleReset();
        }
    }
    
    @JsonIgnore
    public SatResetListener getSatResetListener() {
        return satResetListener;
    }
    
    class CO2ResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleCO2Reset");
            co2ResetRequest.handleReset();
        }
    }
    
    @JsonIgnore
    public CO2ResetListener getCO2ResetListener() {
        return co2ResetListener;
    }
    
    class SpResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleSPReset");
            spResetRequest.handleReset();
        }
    }
    
    @JsonIgnore
    public SpResetListener getSpResetListener() {
        return spResetListener;
    }
    
    class HwstResetListener implements TrimResetListener {
        public void handleSystemReset() {
            Log.d("VAV","handleHWSTReset");
            hwstResetRequest.handleReset();
        }
    }
    
    @JsonIgnore
    public HwstResetListener getHwstResetListener() {
        return hwstResetListener;
    }
    
    @Override
    public void reset(){
        /*for (short node : vavDeviceMap.keySet()) {
            vavDeviceMap.get(node).setCurrentTemp(0);
            vavDeviceMap.get(node).setDamperPos(vavDeviceMap.get(node).getDamperLimit(state == HEATING ? "heating":"cooling", "min"));
            vavDeviceMap.get(node).setReheatPos(0);
           
        }*/
    }
    
    /**
     * GPC-36 recommendation:
     * if the DAT is greater than room temperature plus differential tuner , the heating-loop output shall reset
     * the airflow set point from the heating minimum airflow set point to the heating maximum airflow set point.
     */
    public int getGPC36AdjustedHeatingLoopOp(double heatingLoop, double roomTemp,
                                                 double dischargeTemp, Equip equip) {
        double reheatDatDifferential = vavEquip.getReheatZoneToDATMinDifferential().readPriorityVal();
        
        CcuLog.i(L.TAG_CCU_ZONE,
                 "getGPC36AdjustedHeatingLoopOp heatingLoop "+heatingLoop+" dischargeTemp "+dischargeTemp+" " +
                 "reheatDatDifferential "+reheatDatDifferential);
        //Damper should be at the min position when heatingLoop is less than the threshold
        //or when dischargeTemp is still within the roomTemp+tuner limit.
        if (heatingLoop < REHEAT_THRESHOLD_HEATING_LOOP || dischargeTemp <= (roomTemp + reheatDatDifferential)) {
            return 0;
        }
        
        return (int)(heatingLoop - REHEAT_THRESHOLD_HEATING_LOOP) * 2;
    }

    private void updateDamperWhenSystemHeating(
            CCUHsApi hayStack, String equipId, double currentCfm,
            double co2, double voc, boolean enabledCO2Control,
            boolean enabledIAQControl, boolean occupied) {

        if (cfmLoopState != State.HEATING) {
            cfmLoopState = State.HEATING;
            cfmController.reset();
        }

        boolean isMinCfmRequiredInDeadband = getEquipOccupancyHandler().getCurrentOccupiedMode().equals(Occupancy.OCCUPIED)
                || getEquipOccupancyHandler().getCurrentOccupiedMode().equals(Occupancy.FORCEDOCCUPIED)
                || getEquipOccupancyHandler().getCurrentOccupiedMode().equals(Occupancy.AUTOFORCEOCCUPIED);

        CcuLog.i(L.TAG_CCU_ZONE, "occupancyMode = " + getEquipOccupancyHandler().getCurrentOccupiedMode());
        CcuLog.i(L.TAG_CCU_ZONE, "isMinCfmRequiredInDeadband = " + isMinCfmRequiredInDeadband);

        double minCfmHeating = TrueCFMUtil.getMinCFMReheating(hayStack, equipId);

        // Max Cooling CFM is the max CFM for DCV, even in heating mode
        double maxCfmCooling = TrueCFMUtil.getMaxCFMCooling(hayStack, equipId);
        double co2CompensatedMinCfm = 0;
        double iaqCompensatedMinCfm = 0;

        //CO2 loop output from 0-50% modulates min cfm (up to max cooling cfm)
        if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0) {
            co2CompensatedMinCfm = minCfmHeating + (maxCfmCooling - minCfmHeating) * Math.min(50, co2Loop.getLoopOutput()) / 50;
            CcuLog.i(L.TAG_CCU_ZONE, "state: HEATING, co2: " + co2 + ", co2LoopOp: " + co2Loop.getLoopOutput() + ", co2CompensatedMinCfm " + co2CompensatedMinCfm);
        }

        //VOC loop output from 0-50% modulates min cfm (up to max cooling cfm)
        if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0) {
            iaqCompensatedMinCfm = minCfmHeating + (maxCfmCooling - minCfmHeating) * Math.min(50, vocLoop.getLoopOutput()) / 50;
            CcuLog.i(L.TAG_CCU_ZONE, "state: HEATING, voc: " + voc + ", vocLoopOp: " + vocLoop.getLoopOutput() + ", vocCompensatedMinCfm " + iaqCompensatedMinCfm);
        }

        double effectiveMinCfm = Math.max(minCfmHeating, Math.max(co2CompensatedMinCfm, iaqCompensatedMinCfm));

        double cfmLoopOp = 0;

        /*
            True CFM with system in heating mode works a little differently.

            DAB-calculated damper position is left as-is UNLESS measured airflow is less than minimum CFM setpoint
            (heating min CFM or the min CFM derived from DCV/IAQ control).

            If this happens, DAB-calculated damper position (including DCV minimum position) is discarded
         */
        if (isMinCfmRequiredInDeadband) {
            if (currentCfm < effectiveMinCfm) {
                cfmLoopOp = cfmController.getLoopOutput(effectiveMinCfm, currentCfm);
                damper.currentPosition = (int)cfmLoopOp;
            }
        } else {
            if (state == HEATING || state == COOLING) {
                cfmLoopOp = cfmController.getLoopOutput(effectiveMinCfm, currentCfm);
            } else {
                cfmLoopOp = 0;
            }
        }

        CcuLog.i(L.TAG_CCU_ZONE,
                " updateDamperSystemHeating cfmLoopOp "+cfmLoopOp+" Min CFM Heating:"+minCfmHeating+" Min CFM after DCV:"+effectiveMinCfm);
    }
    
    private void updateDamperForMinCfm(double cfmLoopOp) {
        CcuLog.d(L.TAG_CCU_ZONE,
                 "updateDamperForMinCfm currentPosition: "+damper.currentPosition+" cfmLoopOp "+cfmLoopOp);
        damper.currentPosition = (int)(damper.currentPosition + cfmLoopOp);
    }
    
    private void updateDamperWhenSystemCooling(
            CCUHsApi hayStack, String equipId, double currentCfm,
            double co2, double voc, boolean enabledCO2Control,
            boolean enabledIAQControl, boolean occupied) {

        if (cfmLoopState != State.COOLING) {
            cfmLoopState = State.COOLING;
            cfmController.reset();
        }

        double cfmSp;
        double co2CompensatedMinCfm = 0;
        double iaqCompensatedMinCfm = 0;

        // Max cooling CFM is used as max CFM for DCV regardless of zone state
        double maxCfmCooling = TrueCFMUtil.getMaxCFMCooling(hayStack, equipId);

        if (state == COOLING) {
            double minCfmCooling = TrueCFMUtil.getMinCFMCooling(hayStack, equipId);

            double cfmSpForCooling = minCfmCooling + (maxCfmCooling - minCfmCooling) * damper.currentPosition / 100;

            //CO2 loop output from 0-50% modulates min cfm (up to max cooling cfm)
            if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0) {
                co2CompensatedMinCfm = minCfmCooling + (maxCfmCooling - minCfmCooling) * Math.min(50, co2Loop.getLoopOutput()) / 50;
                CcuLog.i(L.TAG_CCU_ZONE, "state: COOLING, co2: " + co2 + ", co2LoopOp: " + co2Loop.getLoopOutput() + ", co2CompensatedMinCfm " + co2CompensatedMinCfm);
            }

            //VOC loop output from 0-50% modulates min cfm (up to max cooling cfm)
            if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0) {
                iaqCompensatedMinCfm = minCfmCooling + (maxCfmCooling - minCfmCooling) * Math.min(50, vocLoop.getLoopOutput()) / 50;
                CcuLog.i(L.TAG_CCU_ZONE, "state: COOLING, voc: " + voc + ", vocLoopOp: " + vocLoop.getLoopOutput() + ", vocCompensatedMinCfm " + iaqCompensatedMinCfm);
            }

            cfmSp = Math.max(cfmSpForCooling, Math.max(co2CompensatedMinCfm, iaqCompensatedMinCfm));

            CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling cfmSp "+cfmSp+" Cooling CFM required: "
                    +minCfmCooling+"-"+maxCfmCooling + ", Cooling CFM Sp: " + cfmSpForCooling
                    +", CO2 DCV CFM Sp: " + co2CompensatedMinCfm+", IAQ DCV CFM Sp: " + iaqCompensatedMinCfm);

        } else if (state == HEATING){
            double minCfmHeating = TrueCFMUtil.getMinCFMReheating(hayStack, equipId);
            double maxCfmHeating = TrueCFMUtil.getMaxCFMReheating(hayStack, equipId);
            double cfmSpForHeating = minCfmHeating + (maxCfmHeating - minCfmHeating) * damper.currentPosition / 100;

            //CO2 loop output from 0-50% modulates min cfm (up to max cooling cfm)
            if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0) {
                co2CompensatedMinCfm = minCfmHeating + (maxCfmCooling - minCfmHeating) * Math.min(50, co2Loop.getLoopOutput()) / 50;
                CcuLog.i(L.TAG_CCU_ZONE, "state: HEATING, co2: " + co2 + ", co2LoopOp: " + co2Loop.getLoopOutput(co2) + ", co2CompensatedMinCfm " + co2CompensatedMinCfm);
            }

            //VOC loop output from 0-50% modulates min cfm (up to max cooling cfm)
            if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0) {
                iaqCompensatedMinCfm = minCfmHeating + (maxCfmCooling - minCfmHeating) * Math.min(50, vocLoop.getLoopOutput()) / 50;
                CcuLog.i(L.TAG_CCU_ZONE, "state: HEATING, voc: " + voc + ", vocLoopOp: " + vocLoop.getLoopOutput() + ", vocCompensatedMinCfm " + iaqCompensatedMinCfm);
            }

            cfmSp = Math.max(cfmSpForHeating, Math.max(co2CompensatedMinCfm, iaqCompensatedMinCfm));

            CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling cfmSp "+cfmSp+" Reheat CFM required: "
                    +minCfmHeating+"-"+maxCfmHeating + ", Heating CFM Sp: " + cfmSpForHeating
                    +", CO2 DCV CFM Sp: " + co2CompensatedMinCfm+", IAQ DCV CFM Sp: " + iaqCompensatedMinCfm);

        } else {
            if (!TrueCFMUtil.cfmControlNotRequired(hayStack, equipId)) {
                double minCfmHeating = TrueCFMUtil.getMinCFMReheating(hayStack, equipId);

                //CO2 loop output from 0-50% modulates min cfm (up to max cooling cfm)
                if (enabledCO2Control && occupied && co2Loop.getLoopOutput(co2) > 0) {
                    co2CompensatedMinCfm = minCfmHeating + (maxCfmCooling - minCfmHeating) * Math.min(50, co2Loop.getLoopOutput()) / 50;
                    CcuLog.i(L.TAG_CCU_ZONE, "state: DEADBAND, co2: " + co2 + ", co2LoopOp: " + co2Loop.getLoopOutput(co2) + ", co2CompensatedMinCfm " + co2CompensatedMinCfm);
                }

                //VOC loop output from 0-50% modulates min cfm (up to max cooling cfm)
                if (enabledIAQControl && occupied && vocLoop.getLoopOutput(voc) > 0) {
                    iaqCompensatedMinCfm = minCfmHeating + (maxCfmCooling - minCfmHeating) * Math.min(50, vocLoop.getLoopOutput()) / 50;
                    CcuLog.i(L.TAG_CCU_ZONE, "state: HEATING, voc: " + voc + ", vocLoopOp: " + vocLoop.getLoopOutput() + ", vocCompensatedMinCfm " + iaqCompensatedMinCfm);
                }

                cfmSp = Math.max(minCfmHeating, Math.max(co2CompensatedMinCfm, iaqCompensatedMinCfm));

                CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling Deadband co2LoopOp: " + co2Loop.getLoopOutput(co2) + "cfmSp (Occupied): "+cfmSp
                        + ", Min Heating CFM Sp: " + minCfmHeating
                        +", CO2 DCV CFM Sp: " + co2CompensatedMinCfm+", IAQ DCV CFM Sp: " + iaqCompensatedMinCfm);

            } else {
                cfmSp = 0;
                CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling Deadband cfmSp (Unoccupied/Autoaway) "+cfmSp);

            }

        }
        damper.currentPosition = (int)cfmController.getLoopOutput(cfmSp, currentCfm);
    }

    private void updateDamperWhenSystemOff(CCUHsApi hayStack, String equipId, double currentCfm) {
        if (cfmLoopState != State.OFF) {
            cfmLoopState = State.OFF;
            cfmController.reset();
        }
        // Set dampers to 50% in SYSTEM=OFF so that they are not all closed when AHU starts up again.
        // 50% is below the threshold needed to trigger static pressure T&R requests (75%).
        damper.currentPosition = 50;

    }

    public void setStatus(double status, boolean emergency) {

        if (vavEquip.getEquipStatus().readHisVal() != status ) {
            vavEquip.getEquipStatus().writeHisVal(status);
        }

        String message;
        if (emergency) {
            if (profileType.equals(ProfileType.VAV_ACB)) {
                message = (status == 1 ? "Emergency Cooling" : "Recirculating Air");
            } else {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" : "Emergency Heating");
            }
        } else
        {
            if (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING) {
                message = "In Preconditioning ";
            } else
            {
                if (profileType.equals(ProfileType.VAV_ACB)) {
                    message = (status == 1 ? "Cooling Space" : "Recirculating Air");
                } else {
                    message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" : "Warming Space");
                }

            }
        }

        message += getFanStatusMessage();
        message += getAcbStatusMessage();

        String curStatus = vavEquip.getEquipStatusMessage().readDefaultStrVal();
        CcuLog.i(L.TAG_CCU_ZONE, "setStatus "+status+" : "+message);
        if (!curStatus.equals(message)) {
            vavEquip.getEquipStatusMessage().writeDefaultVal(message);
        }
    }

    protected String getFanStatusMessage() {
        if (profileType == ProfileType.VAV_SERIES_FAN) {
            return vavEquip.getSeriesFanCmd().readHisVal() > 0 ? ", Fan ON" : ", Fan OFF";
        } else if (profileType == ProfileType.VAV_PARALLEL_FAN) {
            return vavEquip.getParallelFanCmd().readHisVal() > 0 ? ", Fan ON" : ", Fan OFF";
        }
        return "";
    }

    protected String getAcbStatusMessage() {
        String message = "";
        if (profileType == ProfileType.VAV_ACB) {
            VavAcbEquip acbEquip = (VavAcbEquip)vavEquip;
            if (((VavAcbEquip)vavEquip).getThermistor2Type().readPriorityVal() > 0.0) {
                if (CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.condensateNC + "\" and group == \"" + nodeAddr + "\"") > 0.0) {
                    message += ", Condensation Detected";
                }
            } else {
                if (CCUHsApi.getInstance().readHisValByQuery("point and domainName == \"" + DomainName.condensateNO + "\" and group == \"" + nodeAddr + "\"") > 0.0) {
                    message += ", Condensation Detected";
                }
            }
            if (acbEquip.getValveType().readPriorityVal() > 0.0) {
                if (acbEquip.getChwValveCmd().readHisVal() > 0.0) {
                    message += ", CHW Valve ON";
                } else {
                    message += ", CHW Valve OFF";
                }
            } else {
                if (acbEquip.getChwShutOffValve().readHisVal() > 0.0) {
                    message += ", CHW Valve ON";
                } else {
                    message += ", CHW Valve OFF";
                }
            }
        }
        return message;
    }

    protected void updateDamperPosForTrueCfm(CCUHsApi hayStack, SystemController.State systemState) {
        CcuLog.d(L.TAG_CCU_ZONE,"System state "+systemState);
        String equipId = getEquip().getId();
        if (!TrueCFMUtil.isTrueCfmEnabled(hayStack, equipId)) {
            CcuLog.d(L.TAG_CCU_ZONE,"TrueCFM not enabled "+equipId);
            return;
        }
        double currentCfm = TrueCFMUtil.calculateAndUpdateCfm(hayStack, equipId,"");

        // Parameters needed for DCV algorithm
        double co2 = vavEquip.getZoneCO2().readHisVal();
        double voc = vavEquip.getZoneVoc().readHisVal();
        boolean enabledCO2Control = vavEquip.getEnableCo2Control().readPriorityVal() > 0.0;
        boolean enabledIAQControl = vavEquip.getEnableIAQControl().readPriorityVal() > 0.0;

        String zoneId = HSUtil.getZoneIdFromEquipId(equipId);

        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied())
                || (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING);

        if (systemState == SystemController.State.COOLING) {
            updateDamperWhenSystemCooling(hayStack, equipId, currentCfm, co2, voc, enabledCO2Control, enabledIAQControl, occupied);
            CcuLog.d(L.TAG_CCU_ZONE, "updateDamperWhenSystemCooling cfmControlLoop: " + cfmController.getLoopOutput() + ", damper.currentPosition: " + damper.currentPosition);
        } else if (systemState == SystemController.State.HEATING) {
            updateDamperWhenSystemHeating(hayStack, equipId, currentCfm, co2, voc, enabledCO2Control, enabledIAQControl, occupied);
            CcuLog.d(L.TAG_CCU_ZONE, "updateDamperWhenSystemHeating cfmControlLoop: " + cfmController.getLoopOutput() + ", damper.currentPosition: " + damper.currentPosition);
        } else {
            updateDamperWhenSystemOff(hayStack, equipId, currentCfm);
        }

        damper.currentPosition = Math.max(damper.currentPosition, 0);
        damper.currentPosition = Math.min(damper.currentPosition, 100);
        CcuLog.i(L.TAG_CCU_ZONE,
                " updateDamperPosForTrueCfm: newDamperPos "+damper.currentPosition+" cfmLoopState "+cfmLoopState);
        cfmController.dump();
    }

    protected void updateLoopParams() {
        vavEquip.getHeatingLoopOutput().writeHisVal(Math.min(heatingLoop.getLoopOutput(), 100));
        vavEquip.getCoolingLoopOutput().writeHisVal(Math.min(coolingLoop.getLoopOutput(), 100));
        vavEquip.getDischargeAirTempSetpoint().writeHisVal(dischargeSp);
        vavEquip.getSatRequestPercentage().writeHisVal(satResetRequest.cumulativeRequestHoursPercent);
        vavEquip.getCo2RequestPercentage().writeHisVal(co2ResetRequest.cumulativeRequestHoursPercent);
        vavEquip.getPressureRequestPercentage().writeHisVal(spResetRequest.cumulativeRequestHoursPercent);
        vavEquip.getSatCurrentRequest().writeHisVal(satResetRequest.currentRequests);
        vavEquip.getCo2CurrentRequest().writeHisVal(co2ResetRequest.currentRequests);
        vavEquip.getPressureCurrentRequest().writeHisVal(spResetRequest.currentRequests);
    }

    @Override
    public ProfileConfiguration getDomainProfileConfiguration() {
        Equip equip = getEquip();
        NodeType nodeType = equip.getDomainName().contains("helionode") ? NodeType.HELIO_NODE : NodeType.SMART_NODE;
        return new VavProfileConfiguration(nodeAddr, nodeType.name(),
                (int) vavEquip.getZonePriority().readPriorityVal(),
                equip.getRoomRef(),
                equip.getFloorRef() ,
                profileType,
                (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getModelForDomainName(equip.getDomainName()))
                .getActiveConfiguration();

    }
}
