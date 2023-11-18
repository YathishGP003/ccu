package a75f.io.logic.bo.building.vav;

import static a75f.io.logic.bo.building.ZonePriority.NONE;
import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
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
import a75f.io.domain.logic.DeviceBuilder;
import a75f.io.domain.logic.EntityMapper;
import a75f.io.domain.logic.ProfileEquipBuilder;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ZonePriority;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;
import a75f.io.logic.bo.building.hvac.ParallelFanVavUnit;
import a75f.io.logic.bo.building.hvac.SeriesFanVavUnit;
import a75f.io.logic.bo.building.hvac.Valve;
import a75f.io.logic.bo.building.hvac.VavUnit;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemProfile;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;
import io.seventyfivef.domainmodeler.client.ModelDirective;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

import static a75f.io.logic.bo.building.ZonePriority.NONE;
import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.truecfm.TrueCfmLoopState.*;
import static a75f.io.logic.bo.building.system.SystemController.*;

import static a75f.io.logic.bo.building.ZonePriority.NONE;
import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.*;
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
    ControlLoop cfmControlLoop;
    
    private SystemController.State cfmLoopState;

    a75f.io.domain.VavEquip vavEquip;

    int    integralMaxTimeout = 30;
    int proportionalSpread = 20;
    double proportionalGain = 0.2;
    double integralGain = 0.8;

    double      currentTemp;
    double      humidity;
    double desiredTemp;
    double supplyAirTemp;
    double dischargeTemp;
    double co2;
    double dischargeSp;

    double staticPressure;

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
        
    public VavProfile(String equipRef, Short addr) {

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

        /*profileType = T;
        switch (T) {
            case VAV_REHEAT:
                vavUnit = new VavUnit();
                break;
            case VAV_SERIES_FAN:
                vavUnit = new SeriesFanVavUnit();
                break;
            case VAV_PARALLEL_FAN:
                vavUnit = new ParallelFanVavUnit();
                break;
        }*/
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
    }

    public void init() {

        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");
        equipRef = equipMap.get("id").toString();

        if (equipMap != null && equipMap.size() > 0)
        {
            String equipId = equipMap.get("id").toString();
            proportionalGain = TunerUtil.readTunerValByQuery("pgain and not trueCfm",equipId);
            integralGain = TunerUtil.readTunerValByQuery("igain and not trueCfm",equipId);
            proportionalSpread = (int) TunerUtil.readTunerValByQuery("pspread and not trueCfm",equipId);
            integralMaxTimeout = (int) TunerUtil.readTunerValByQuery("itimeout and not trueCfm",equipId);

            co2Target = (int) TunerUtil.readTunerValByQuery("zone and vav and co2 and target and equipRef == \""+equipId+"\"");
            co2Threshold = (int) TunerUtil.readTunerValByQuery("zone and vav and co2 and threshold and equipRef == \""+equipId+"\"");
            vocTarget = (int) TunerUtil.readTunerValByQuery("zone and vav and voc and target and equipRef == \""+equipId+"\"");
            vocThreshold = (int) TunerUtil.readTunerValByQuery("zone and vav and voc and threshold and equipRef == \""+equipId+"\"");

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
    }

    private void initializeCfmController(String equipId) {
        double cfmProportionalGain = TunerUtil.readTunerValByQuery("pgain and trueCfm",equipId);
        double cfmIntegralGain = TunerUtil.readTunerValByQuery("igain and trueCfm",equipId);
        int cfmProportionalSpread = (int) TunerUtil.readTunerValByQuery("prange and trueCfm",equipId);
        int cfmIntegralMaxTimeout = (int) TunerUtil.readTunerValByQuery("itimeout and trueCfm",equipId);

        CcuLog.i(L.TAG_CCU_ZONE,"node "+nodeAddr+" cfmProportionalGain "+cfmProportionalGain
                +" cfmIntegralGain "+cfmIntegralGain
                +" cfmProportionalSpread "+cfmProportionalSpread
                +" cfmIntegralMaxTimeout "+cfmIntegralMaxTimeout);
        cfmController.setProportionalGain(cfmProportionalGain);
        cfmController.setIntegralGain(cfmIntegralGain);
        cfmController.setProportionalSpread(cfmProportionalSpread);
        cfmController.setIntegralMaxTimeout(cfmIntegralMaxTimeout);
        cfmController.reset();
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

    /*public void addLogicalMap(short addr) {
        VavEquip deviceMap = new VavEquip(getProfileType(), addr);
        vavDeviceMap.put(addr, deviceMap);
        deviceMap.satResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.co2ResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.spResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.hwstResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.init();
    }*/


    /*public void addLogicalMapAndPoints(short addr, VavProfileConfiguration config, String floorRef, String roomRef, NodeType nodeType) {
        VavEquip deviceMap = new VavEquip(getProfileType(), addr);
        deviceMap.createHaystackPoints(config, floorRef, roomRef, nodeType);
        vavDeviceMap.put(addr, deviceMap);
        deviceMap.satResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.co2ResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.spResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.hwstResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.init();
    }

    public void updateLogicalMapAndPoints(short addr, VavProfileConfiguration config) {
        VavEquip deviceMap = vavDeviceMap.get(addr);
        deviceMap.updateHaystackPoints(config);
    
        deviceMap.satResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.co2ResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.spResetRequest.setImportanceMultiplier(getPriority().multiplier);
        deviceMap.hwstResetRequest.setImportanceMultiplier(getPriority().multiplier);
    }*/
    
    
    @Override
    public void updateZonePoints() {
        Log.d(TAG, " Invalid VAV Unit Type");
    }
    
    protected void setDamperLimits(short node, Damper d) {
        d.minPosition = (int) (state == HEATING ? vavEquip.getMinHeatingDamperPos().readDefaultVal()
                                : vavEquip.getMinCoolingDamperPos().readDefaultVal());
        d.maxPosition = (int) (state == HEATING ? vavEquip.getMaxHeatingDamperPos().readDefaultVal()
                : vavEquip.getMaxCoolingDamperPos().readDefaultVal());
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

    public Set<Short> getNodeAddresses()
    {
        return null;
    }

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
        return 0;
    }

    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
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
                                                 double dischargeTemp, Equip vavEquip) {
        double reheatDatDifferential = TunerUtil.readTunerValByQuery("vav and reheat and dat and min and " +
                                                                     "differential and equipRef == \""+vavEquip.getId()+"\"");
        
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

    private void updateDamperWhenSystemHeating(CCUHsApi hayStack, String equipId, double currentCfm) {

        if (cfmLoopState != State.HEATING) {
            cfmLoopState = State.HEATING;
            cfmControlLoop.reset();
        }

        boolean isMinCfmRequiredInDeadband = getEquipOccupancyHandler().getCurrentOccupiedMode().equals(Occupancy.OCCUPIED)
                || getEquipOccupancyHandler().getCurrentOccupiedMode().equals(Occupancy.FORCEDOCCUPIED)
                || getEquipOccupancyHandler().getCurrentOccupiedMode().equals(Occupancy.AUTOFORCEOCCUPIED);

        CcuLog.i(L.TAG_CCU_ZONE, "occupancyMode = " + getEquipOccupancyHandler().getCurrentOccupiedMode());
        CcuLog.i(L.TAG_CCU_ZONE, "isMinCfmRequiredInDeadband = " + isMinCfmRequiredInDeadband);

        double minCfmHeating = TrueCFMUtil.getMinCFMReheating(hayStack, equipId);
        double cfmLoopOp = 0;

        if (isMinCfmRequiredInDeadband) {
            if (currentCfm < minCfmHeating) {
                cfmLoopOp = cfmControlLoop.getLoopOutput(minCfmHeating, currentCfm);
                updateDamperForMinCfm(cfmLoopOp);
            }
        } else {
            if (state == HEATING || state == COOLING) {
                cfmLoopOp = cfmControlLoop.getLoopOutput(minCfmHeating, currentCfm);
            } else {
                cfmLoopOp = 0;
            }
        }

        CcuLog.i(L.TAG_CCU_ZONE,
                 " updateDamperSystemHeating cfmLoopOp "+cfmLoopOp+" CFM required:"+minCfmHeating);
    }
    
    private void updateDamperForMinCfm(double cfmLoopOp) {
        CcuLog.d(L.TAG_CCU_ZONE,
                 "updateDamperForMinCfm currentPosition: "+damper.currentPosition+" cfmLoopOp "+cfmLoopOp);
        damper.currentPosition = (int)(damper.currentPosition + cfmLoopOp);
    }
    
    private void updateDamperWhenSystemCooling(CCUHsApi hayStack, String equipId, double currentCfm) {
        if (cfmLoopState != State.COOLING) {
            cfmLoopState = State.COOLING;
            cfmControlLoop.reset();
        }

        double cfmSp;

        if (state == COOLING) {
            double maxCfmCooling = TrueCFMUtil.getMaxCFMCooling(hayStack, equipId);
            double minCfmCooling = TrueCFMUtil.getMinCFMCooling(hayStack, equipId);
            cfmSp = minCfmCooling + (maxCfmCooling - minCfmCooling) * damper.currentPosition / 100;
            CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling cfmSp "+cfmSp+" Cooling CFM required: "
                                                                    +minCfmCooling+"-"+maxCfmCooling);
        } else if (state == HEATING){
            double minCfmHeating = TrueCFMUtil.getMinCFMReheating(hayStack, equipId);
            double maxCfmHeating = TrueCFMUtil.getMaxCFMReheating(hayStack, equipId);
            cfmSp = minCfmHeating + (maxCfmHeating - minCfmHeating) * damper.currentPosition / 100;
            CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling cfmSp "+cfmSp+" Reheat CFM required: "
                                                                   +minCfmHeating+"-"+maxCfmHeating);
        } else {
            if (!TrueCFMUtil.cfmControlNotRequired(hayStack, equipId)) {
                cfmSp = TrueCFMUtil.getMinCFMReheating(hayStack, equipId);
                CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling Deadband cfmSp (Occupied) "+cfmSp);

            } else {
                cfmSp = 0;
                CcuLog.i(L.TAG_CCU_ZONE, " updateDamperWhenSystemCooling Deadband cfmSp (Unoccupied/Autoaway) "+cfmSp);

            }

        }
        damper.currentPosition = (int)cfmControlLoop.getLoopOutput(cfmSp, currentCfm);
    }

    private void updateDamperWhenSystemOff(CCUHsApi hayStack, String equipId, double currentCfm) {
        if (cfmLoopState != State.OFF) {
            cfmLoopState = State.OFF;
            cfmControlLoop.reset();
        }
        // Set dampers to 50% in SYSTEM=OFF so that they are not all closed when AHU starts up again.
        // 50% is below the threshold needed to trigger static pressure T&R requests (75%).
        damper.currentPosition = 50;

    }
    
    public void updateDamperPosForTrueCfm(CCUHsApi hayStack, SystemController.State systemState) {
        
        String equipId = getEquip().getId();
        if (!TrueCFMUtil.isTrueCfmEnabled(hayStack, equipId)) {
            CcuLog.d(L.TAG_CCU_ZONE,"TrueCFM not enabled "+equipId);
            return;
        }
        double currentCfm = TrueCFMUtil.calculateAndUpdateCfm(hayStack, equipId,"");

        CcuLog.i(L.TAG_CCU_ZONE, " updateDamperPosForTrueCfm: doCFM - iaqCompensatedLoopOp "+damper.currentPosition);
        if (systemState == SystemController.State.COOLING) {
            updateDamperWhenSystemCooling(hayStack, equipId, currentCfm);
        } else if (systemState == SystemController.State.HEATING) {
            updateDamperWhenSystemHeating(hayStack, equipId, currentCfm);
        } else {
            updateDamperWhenSystemOff(hayStack, equipId, currentCfm);
        }
        
        damper.currentPosition = Math.max(damper.currentPosition, 0);
        damper.currentPosition = Math.min(damper.currentPosition, 100);
        CcuLog.i(L.TAG_CCU_ZONE,
                 " updateDamperPosForTrueCfm: newDamperPos "+damper.currentPosition+" cfmLoopState "+cfmLoopState);
        cfmControlLoop.dump();
    }

    public void setStatus(double status, boolean emergency) {

        if (vavEquip.getEquipStatus().readHisVal() != status )
        {
            CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" + nodeAddr + "\"", status);
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

        message += getFanStatusMessage();

        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \""+nodeAddr+"\"");
        if (!curStatus.equals(message))
        {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + nodeAddr + "\"", message);
        }
    }

    public String getFanStatusMessage() {
        if (profileType == ProfileType.VAV_SERIES_FAN) {
            return isFanOn("series") ? ", Fan ON" : ", Fan OFF";
        } else if (profileType == ProfileType.VAV_PARALLEL_FAN) {
            return isFanOn("parallel") ? ", Fan ON" : ", Fan OFF";
        }
        return "";
    }

    public boolean isFanOn(String type)
    {
        double fanState = CCUHsApi.getInstance().readHisValByQuery(type+" and point and " +
                "fan and cmd and group == \""+nodeAddr+ "\"");
        return fanState > 0;
    }
    public void setFanOn(String type, boolean state)
    {
        CCUHsApi.getInstance().writeHisValByQuery(type+" and point and fan and cmd and group == \""+nodeAddr+"\"",
                state ? 1.0 : 0);
    }

    public void updateLoopParams() {

        CCUHsApi.getInstance().writeHisValByQuery("point and heating and loop and sp and his and group == \""+nodeAddr+"\"", heatingLoop.getLoopOutput());


        CCUHsApi.getInstance().writeHisValByQuery("point and cooling and loop and sp and his and group == \""+nodeAddr+"\"", coolingLoop.getLoopOutput());

        CCUHsApi.getInstance().writeHisValByQuery("point and discharge and air and temp and sp and his and group == \""+nodeAddr+"\"", dischargeSp);


        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                "supply and air and temp and his and group == \""+nodeAddr+"\"", satResetRequest.cumulativeRequestHoursPercent);

        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                "co2 and his and group == \""+nodeAddr+"\"", co2ResetRequest.cumulativeRequestHoursPercent);
        /*CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                                                  "hwst and his and group == \""+nodeAddr+"\"", hwstResetRequest.cumulativeRequestHoursPercent);*/
        CCUHsApi.getInstance().writeHisValByQuery("point and request and hour and cumulative and tr and " +
                "staticPressure and his and group == \""+nodeAddr+"\"", spResetRequest.cumulativeRequestHoursPercent);

        CCUHsApi.getInstance().writeHisValByQuery("point and request and current and tr and " +
                "sat and his and group == \""+nodeAddr+"\"", (double)satResetRequest.currentRequests);

        CCUHsApi.getInstance().writeHisValByQuery("point and request and current and tr and " +
                "co2 and his and group == \""+nodeAddr+"\"", (double)co2ResetRequest.currentRequests);

        CCUHsApi.getInstance().writeHisValByQuery("point and request and current and tr and " +
                "staticPressure and his and group == \""+nodeAddr+"\"", (double)spResetRequest.currentRequests);

    }
}
