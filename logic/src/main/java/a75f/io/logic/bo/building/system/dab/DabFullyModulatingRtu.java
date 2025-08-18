package a75f.io.logic.bo.building.system.dab;

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Ccu;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.api.PhysicalPoint;
import a75f.io.domain.api.Point;
import a75f.io.domain.equips.DabModulatingRtuSystemEquip;
import a75f.io.domain.util.CalibratedPoint;
import a75f.io.domain.util.CommonQueries;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.ModulatingProfileAnalogMapping;
import a75f.io.logic.bo.building.hvac.ModulatingProfileRelayMapping;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemControllerFactory;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemStageHandler;
import a75f.io.logic.bo.building.system.vav.config.DabModulatingRtuProfileConfig;
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuAnalogOutMinMaxConfig;
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig;
import a75f.io.logic.bo.util.CCUUtils;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

import static a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getAnalogMax;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getAnalogMin;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getChilledWaterValveSignal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getCompressorSpeedSignal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getFanSpeedSignal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getHeatingSignal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getModulatedAnalogVal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getModulatedAnalogValDuringEcon;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getOutsideAirDamperSignal;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;

public class DabFullyModulatingRtu extends DabSystemProfile {
    private static final int ANALOG_SCALE = 10;
    public DabModulatingRtuSystemEquip systemEquip;

    private final SystemControllerFactory factory = new SystemControllerFactory(controllers);
    private SystemStageHandler systemStatusHandler;

    public String getProfileName() {
        if(BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD)){
            return "VVT-C Fully Modulating AHU";
        }else{
            return "DAB Fully Modulating AHU";
        }
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_DAB_ANALOG_RTU;
    }
    
    @Override
    public void doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo();
        updateSystemPoints();
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return systemEquip.getCoolingSignal().pointExists()
                || systemEquip.getCompressorSpeed().pointExists();
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return systemEquip.getHeatingSignal().pointExists()
                || systemEquip.getCompressorSpeed().pointExists();
    }
    
    @Override
    public boolean isCoolingActive(){
        return systemCoolingLoopOp > 0;
    }
    
    @Override
    public boolean isHeatingActive(){
        return systemHeatingLoopOp > 0;
    }
    
    private double systemDCWBValveLoopOutput;
    private DcwbAlgoHandler dcwbAlgoHandler = null;

    private synchronized void updateSystemPoints() {

        systemEquip = (DabModulatingRtuSystemEquip) Domain.systemEquip;
        if (currentOccupancy == null) {
            currentOccupancy = new CalibratedPoint(DomainName.occupancyMode, systemEquip.getEquipRef(), 0.0);
        }

        addControllers();
        updateOutsideWeatherParams();
        updateMechanicalConditioning(CCUHsApi.getInstance());
        
        DabSystemController dabSystem = DabSystemController.getInstance();

        if (isDcwbEnabled()) {
            boolean isAdaptiveDelta = systemEquip.getAdaptiveDeltaEnable().readDefaultVal() > 0;

            if (dcwbAlgoHandler == null) {
                dcwbAlgoHandler = new DcwbAlgoHandler(isAdaptiveDelta, getSystemEquipRef(), CCUHsApi.getInstance());
            } else {
                // Update separately in else block since the above if block is entered only once.
                dcwbAlgoHandler.setAdaptiveDelta(isAdaptiveDelta);
            }
        }
        updateSystemDcwbLoopOutput(dabSystem);
        updateCoolingLoopOutput(dabSystem);
        updateFanLoop(dabSystem);
        updateSystemHeatingLoopOutput(dabSystem);
        updateCompressorLoopOutput(dabSystem);
        updateSystemCo2LoopOp();

        SeventyFiveFProfileDirective model = (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getVavModulatingRtuModelDef();
        ModulatingRtuProfileConfig config = new DabModulatingRtuProfileConfig(model).getActiveConfiguration();

        CcuLog.d(L.TAG_CCU_SYSTEM, config.toString());

        Domain.cmBoardDevice.getAnalog1Out().writePointValue(getAnalog1Output(config));
        Domain.cmBoardDevice.getAnalog2Out().writePointValue(getAnalog2Output(config));
        Domain.cmBoardDevice.getAnalog3Out().writePointValue(getAnalog3Output(config));
        Domain.cmBoardDevice.getAnalog4Out().writePointValue(getAnalog4Output(config));

        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp "+systemCoolingLoopOp+ " systemHeatingLoopOp "+ systemHeatingLoopOp
                +" systemFanLoopOp "+systemFanLoopOp+" systemCompressorLoopOp "+systemCompressorLoop
                +" systemCo2LoopOp "+systemCo2LoopOp+" systemDCWBValveLoopOutput "+systemDCWBValveLoopOutput);

        updatePrerequisite();
        systemStatusHandler.runControllersAndUpdateStatus(controllers,
                (int) systemEquip.getConditioningMode().readPriorityVal(),
                systemEquip.getConditioningStages());
        updateRelays();


        systemEquip.getOperatingMode().writeHisVal(dabSystem.systemState.ordinal());
        String systemStatus = getStatusMessage();
        String scheduleStatus = ScheduleManager.getInstance().getSystemStatusString();
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemStatusMessage: "+systemStatus);
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: " +scheduleStatus);
        
        if (!systemEquip.getEquipStatusMessage().readDefaultStrVal().equals(systemStatus)) {
            systemEquip.getEquipStatusMessage().writeDefaultVal(systemStatus);
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        if (!systemEquip.getEquipScheduleStatus().readDefaultStrVal().equals(scheduleStatus)) {
            systemEquip.getEquipScheduleStatus().writeDefaultVal(scheduleStatus);
        }
    }

    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        boolean economizingOn = systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable();
        status.append((systemFanLoopOp > 0 || Domain.cmBoardDevice.getRelay3().readPointValue() > 0) ? " Fan ON ": "");
        if(economizingOn) {
            double economizingToMainCoolingLoopMap =  L.ccu().oaoProfile.getOAOEquip().getEconomizingToMainCoolingLoopMap().readPriorityVal();
            status.append((systemCoolingLoopOp >= economizingToMainCoolingLoopMap && !isCoolingLockoutActive()) ? " | Cooling ON ":"");
        } else {
            status.append((systemCoolingLoopOp > 0 && !isCoolingLockoutActive()) ? " | Cooling ON ":"");
        }
        status.append((systemHeatingLoopOp > 0 && !isHeatingLockoutActive()) ? " | Heating ON ":"");
        if (economizingOn) {
            status.insert(0, "Free Cooling Used |");
        }

        if (status.toString().isEmpty() && systemEquip.getConditioningStages().getFanEnable().readHisVal() > 0) {
            status.append("Fan ON");
        }

        if (systemEquip.getRelay3OutputEnable().readDefaultVal() > 0) {
            double relay3Association = systemEquip.getRelay3OutputAssociation().readDefaultVal();
            if (relay3Association == ModulatingProfileRelayMapping.HUMIDIFIER.ordinal()) {
                status.append((systemEquip.getHumidifier().readHisVal() > 0) ? " | Humidifier ON " : " | Humidifier OFF ");
            } else if (relay3Association == ModulatingProfileRelayMapping.DEHUMIDIFIER.ordinal()) {
                status.append((systemEquip.getDehumidifier().readHisVal() > 0) ? " | Dehumidifier ON " : " | Dehumidifier OFF ");
            }
        }
        if (systemEquip.getRelay7OutputEnable().readDefaultVal() > 0) {
            double relay7Association = systemEquip.getRelay7OutputAssociation().readDefaultVal();
            if (relay7Association == ModulatingProfileRelayMapping.HUMIDIFIER.ordinal()) {
                status.append((systemEquip.getHumidifier().readHisVal() > 0) ? " | Humidifier ON " : " | Humidifier OFF ");
            } else if (relay7Association == ModulatingProfileRelayMapping.DEHUMIDIFIER.ordinal()) {
                status.append((systemEquip.getDehumidifier().readHisVal() > 0) ? " | Dehumidifier ON " : " | Dehumidifier OFF ");
            }
        }
        return status.toString().isEmpty() ? "System OFF" : status.toString();
    }
    
    public void addSystemEquip() {
        systemEquip = (DabModulatingRtuSystemEquip) Domain.systemEquip;
        systemStatusHandler = new SystemStageHandler(systemEquip.getConditioningStages());
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        ArrayList<HashMap<Object, Object>> listOfEquips = CCUHsApi.getInstance().readAllEntities(CommonQueries.SYSTEM_PROFILE);
        for (HashMap<Object, Object> equip : listOfEquips) {
            if(equip.get("profile") != null){
                if(ProfileType.getProfileTypeForName(equip.get("profile").toString()) != null){
                    if (ProfileType.getProfileTypeForName(equip.get("profile").toString()).name().equals(ProfileType.SYSTEM_DAB_ANALOG_RTU.name())) {
                        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "DabFullyModulatingRtu removing profile with it id-->"+equip.get("id").toString());
                        CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
                    }
                }
            }
        }

        removeSystemEquipModbus();
        removeSystemEquipBacnet();
        deleteSystemConnectModule();
    }

    private void updateCoolingLoopOutput(DabSystemController dabSystem) {
        if (dabSystem.getSystemState() == COOLING) {
            systemCoolingLoopOp = dabSystem.getCoolingSignal();
        } else {
            systemCoolingLoopOp = 0;
        }
        systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);
        systemCoolingLoopOp = systemEquip.getCoolingLoopOutput().readHisVal();
    }

    private void updateSystemDcwbLoopOutput(DabSystemController dabSystem) {
        if (isDcwbEnabled()) {
            if (dabSystem.getSystemState() == COOLING) {
                dcwbAlgoHandler.runLoopAlgorithm();
                systemDCWBValveLoopOutput = CCUUtils.roundToTwoDecimal(dcwbAlgoHandler.getChilledWaterValveLoopOutput());
            } else {
                systemDCWBValveLoopOutput = 0;
                dcwbAlgoHandler.resetChilledWaterValveLoop();
            }
        } else {
            systemDCWBValveLoopOutput = 0;
        }
        systemEquip.getSystemDCWBValveLoopOutput().writePointValue(systemDCWBValveLoopOutput);
        systemDCWBValveLoopOutput = systemEquip.getSystemDCWBValveLoopOutput().readHisVal();
    }

    private void updateSystemHeatingLoopOutput(DabSystemController dabSystem) {
        if (dabSystem.getSystemState() == HEATING) {
            systemHeatingLoopOp = dabSystem.getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
        systemEquip.getHeatingLoopOutput().writePointValue(systemHeatingLoopOp);
        systemHeatingLoopOp = systemEquip.getHeatingLoopOutput().readHisVal();
    }
    
    private void updateFanLoop(DabSystemController dabSystem) {
        
        double analogFanSpeedMultiplier = systemEquip.getDabAnalogFanSpeedMultiplier().readPriorityVal();
        double epidemicMode = systemEquip.getEpidemicModeSystemState().readHisVal();
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        
        if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && (L.ccu().oaoProfile != null)){
            
            double smartPurgeDabFanLoopOp = L.ccu().oaoProfile.getOAOEquip().getSystemPurgeDabMinFanLoopOutput().readPriorityVal();
            if(L.ccu().oaoProfile.isEconomizingAvailable()) {
                double economizingToMainCoolingLoopMap = L.ccu().oaoProfile.getOAOEquip().getEconomizingToMainCoolingLoopMap().readPriorityVal();
                systemFanLoopOp = Math.max(Math.max(systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap, systemHeatingLoopOp), smartPurgeDabFanLoopOp);
            }else if(dabSystem.getSystemState() == COOLING){
                systemFanLoopOp = Math.max(systemCoolingLoopOp * analogFanSpeedMultiplier, smartPurgeDabFanLoopOp);
            }else if(dabSystem.getSystemState() == HEATING){
                systemFanLoopOp = Math.max(systemHeatingLoopOp * analogFanSpeedMultiplier, smartPurgeDabFanLoopOp);
            }else {
                systemFanLoopOp = smartPurgeDabFanLoopOp;
            }
        } else if (dabSystem.getSystemState() == COOLING) {
            //When the system is economizing we need to ramp up the fan faster to take advantage of the free cooling. In such a case
            //systemFanLoopOutput = systemCoolingLoopOutput * 100/economizingToMainCoolingLoopMap
            if (L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
                double economizingToMainCoolingLoopMap = L.ccu().oaoProfile.getOAOEquip().getEconomizingToMainCoolingLoopMap().readPriorityVal();
                systemFanLoopOp = dabSystem.getCoolingSignal() * 100/economizingToMainCoolingLoopMap ;
            } else {
                systemFanLoopOp = (int) (dabSystem.getCoolingSignal() * analogFanSpeedMultiplier);
            }
        } else if (dabSystem.getSystemState() == HEATING){
            systemFanLoopOp = (int) (dabSystem.getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
        systemFanLoopOp = Math.min(systemFanLoopOp, 100);

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            systemEquip.getFanLoopOutput().writePointValue(systemFanLoopOp);
            systemFanLoopOp = systemEquip.getFanLoopOutput().readHisVal();
        }
        systemEquip.getFanLoopOutput().writePointValue(systemFanLoopOp);
        systemFanLoopOp = systemEquip.getFanLoopOutput().readHisVal();
    }

    private void updateCompressorLoopOutput(DabSystemController dabSystem) {
        if (dabSystem.getSystemState() == COOLING && !isCoolingLockoutActive()) {
            systemCompressorLoop = systemCoolingLoopOp;
        } else if (dabSystem.getSystemState() == HEATING && !isHeatingLockoutActive()) {
            systemCompressorLoop = systemHeatingLoopOp;
        } else {
            systemCompressorLoop = 0;
        }
        systemEquip.getCompressorLoopOutput().writePointValue(systemCompressorLoop);
        systemCompressorLoop = systemEquip.getCompressorLoopOutput().readHisVal();
    }

    private void updateSystemCo2LoopOp() {
        if (systemEquip.getConditioningMode().readPriorityVal() != SystemMode.OFF.ordinal()) {
            systemCo2LoopOp = getCo2LoopOp();
        } else {
            systemCo2LoopOp = 0;
        }
        systemEquip.getCo2LoopOutput().writePointValue(systemCo2LoopOp);
        systemCo2LoopOp = systemEquip.getCo2LoopOutput().readHisVal();

        systemEquip.getDcvLoopOutput().writePointValue(systemCo2LoopOp);
        systemDcvLoopOp = systemEquip.getDcvLoopOutput().readHisVal();
    }

    private int getCoolingSignal(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig, ModulatingProfileAnalogMapping analogMapping) {
        double analogMin = getAnalogMin(minMaxConfig, analogMapping);
        double analogMax = getAnalogMax(minMaxConfig, analogMapping);
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" SAT: "+getSystemSAT());
        if (isCoolingLockoutActive()) {
            return  (int)(analogMin * ANALOG_SCALE);
        } else {
            double economizingToMainCoolingLoopMap = 0;
            boolean economizingOn = false;

            if(L.ccu().oaoProfile != null) {
                economizingToMainCoolingLoopMap =  L.ccu().oaoProfile.getOAOEquip().getEconomizingToMainCoolingLoopMap().readPriorityVal();
                economizingOn =L.ccu().oaoProfile.isEconomizingAvailable();
            }
            if(!economizingOn) {
                /* OAT > Max Economizing Temp */
                return getModulatedAnalogVal(analogMin, analogMax, systemCoolingLoopOp);
            } else if (economizingOn && systemCoolingLoopOp < economizingToMainCoolingLoopMap) {
                /* When only Economizing is active, (CLO < 30% (economizingToMainCoolingLoopMap) */
                return  (int) (analogMin * ANALOG_SCALE);
            } else if  (economizingOn && systemCoolingLoopOp >= economizingToMainCoolingLoopMap) {
                /* Economizing + Analog Cooling */
                return (int)getModulatedAnalogValDuringEcon(analogMin, analogMax, systemCoolingLoopOp, economizingToMainCoolingLoopMap);
            }
        }
        return  (int)(analogMin * ANALOG_SCALE);
    }

    private double getAnalog1Output(ModulatingRtuProfileConfig config) {
        double signal = 0;
        if (config.analog1OutputEnable.getEnabled()) {
            ModulatingRtuAnalogOutMinMaxConfig minMaxConfig = config.getAnalog1OutMinMaxConfig();
            int analog1Association = config.analog1OutputAssociation.getAssociationVal();
            ModulatingProfileAnalogMapping analogMapping = ModulatingProfileAnalogMapping.values()[analog1Association];
            signal = getAnalogOut(minMaxConfig, analogMapping);
        }
        return signal;
    }

    private double getAnalog2Output(ModulatingRtuProfileConfig config) {
        double signal = 0;
        if (config.analog2OutputEnable.getEnabled()) {
            ModulatingRtuAnalogOutMinMaxConfig minMaxConfig = config.getAnalog2OutMinMaxConfig();
            int analog2Association = config.analog2OutputAssociation.getAssociationVal();
            ModulatingProfileAnalogMapping analogMapping = ModulatingProfileAnalogMapping.values()[analog2Association];
            signal = getAnalogOut(minMaxConfig, analogMapping);
        }
        return signal;
    }

    private double getAnalog3Output(ModulatingRtuProfileConfig config) {
        double signal = 0;
        if (config.analog3OutputEnable.getEnabled()) {
            ModulatingRtuAnalogOutMinMaxConfig minMaxConfig = config.getAnalog3OutMinMaxConfig();
            int analog3Association = config.analog3OutputAssociation.getAssociationVal();
            ModulatingProfileAnalogMapping analogMapping = ModulatingProfileAnalogMapping.values()[analog3Association];
            signal = getAnalogOut(minMaxConfig, analogMapping);
        }
        return signal;
    }

    private double getAnalog4Output(ModulatingRtuProfileConfig config) {
        double signal = 0;
        if (config.analog4OutputEnable.getEnabled()) {
            ModulatingRtuAnalogOutMinMaxConfig minMaxConfig = config.getAnalog4OutMinMaxConfig();
            int analog4Association = config.analog4OutputAssociation.getAssociationVal();
            ModulatingProfileAnalogMapping analogMapping = ModulatingProfileAnalogMapping.values()[analog4Association];
            signal = getAnalogOut(minMaxConfig, analogMapping);
        }
        return signal;
    }

    private double getAnalogOut(ModulatingRtuAnalogOutMinMaxConfig minMaxConfig,
                                ModulatingProfileAnalogMapping analogMapping) {
        double signal = 0;
        switch (analogMapping) {
            case FanSpeed:
                signal = getFanSpeedSignal(minMaxConfig, analogMapping, systemFanLoopOp);
                systemEquip.getFanSignal().writePointValue(signal);
                signal = systemEquip.getFanSignal().readHisVal();
                break;
            case CompressorSpeed:
                signal = getCompressorSpeedSignal(minMaxConfig, analogMapping, systemCompressorLoop);
                systemEquip.getCompressorSpeed().writePointValue(signal);
                signal = systemEquip.getCompressorSpeed().readHisVal();
                break;
            case OutsideAirDamper:
                signal = getOutsideAirDamperSignal(minMaxConfig, analogMapping, systemCo2LoopOp);
                systemEquip.getOutsideAirDamper().writePointValue(signal);
                signal = systemEquip.getOutsideAirDamper().readHisVal();
                break;
            case Cooling:
                signal = getCoolingSignal(minMaxConfig, analogMapping);
                systemEquip.getCoolingSignal().writePointValue(signal);
                signal = systemEquip.getCoolingSignal().readHisVal();
                break;
            case Heating:
                signal = getHeatingSignal(minMaxConfig, analogMapping, isHeatingLockoutActive(), systemHeatingLoopOp);
                systemEquip.getHeatingSignal().writePointValue(signal);
                signal = systemEquip.getHeatingSignal().readHisVal();
                break;
            case ChilledWaterValve:
                signal = getChilledWaterValveSignal(minMaxConfig, analogMapping, systemDCWBValveLoopOutput);
                systemEquip.getChilledWaterValveSignal().writePointValue(signal);
                signal = systemEquip.getChilledWaterValveSignal().readHisVal();
                break;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM, "Analog Out Mapping: " + analogMapping + " Signal: " + signal);
        return signal;
    }

    public double getConfigVal(String tags) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> configPoint = hayStack.readEntity("point and system and config and "+tags);
        if (configPoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_SYSTEM," !!!  System config point does not exist !!! - "+tags);
            return 0;
        }
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }

    public double getConfigEnabled(String config) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> configPoint =
            hayStack.readEntity("point and system and config and output and enabled and "+config);
        if (configPoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_SYSTEM," !!!  System config enable point does not exist !!! - "+config);
            return 0;
        }
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }
    
    public boolean isDcwbEnabled() {
        return systemEquip.getDcwbEnable().readPriorityVal() > 0;
    }

    public Map<Point, PhysicalPoint> getLogicalPhysicalMap() {
        Map<a75f.io.domain.api.Point, a75f.io.domain.api.PhysicalPoint> map = new HashMap<>();
        if (systemEquip == null) {
            return map;
        }
        map.put(systemEquip.getAnalog1OutputEnable(), Domain.cmBoardDevice.getAnalog1Out());
        map.put(systemEquip.getAnalog2OutputEnable(), Domain.cmBoardDevice.getAnalog2Out());
        map.put(systemEquip.getAnalog3OutputEnable(), Domain.cmBoardDevice.getAnalog3Out());
        map.put(systemEquip.getAnalog4OutputEnable(), Domain.cmBoardDevice.getAnalog4Out());
        map.put(systemEquip.getRelay3OutputEnable(), Domain.cmBoardDevice.getRelay3());
        map.put(systemEquip.getRelay7OutputEnable(), Domain.cmBoardDevice.getRelay7());
        return map;
    }

    public void addControllers() {
        factory.addHumidifierController(
                systemEquip.getAverageHumidity(),
                systemEquip.getSystemtargetMinInsideHumidity(),
                systemEquip.getDabHumidityHysteresis(),
                currentOccupancy,
                systemEquip.getConditioningStages().getHumidifierEnable().pointExists()
        );

        factory.addDeHumidifierController(
                systemEquip.getAverageHumidity(),
                systemEquip.getSystemtargetMaxInsideHumidity(),
                systemEquip.getDabHumidityHysteresis(),
                currentOccupancy,
                systemEquip.getConditioningStages().getDehumidifierEnable().pointExists()
        );

        factory.addChangeCoolingChangeOverRelay(
                systemEquip.getCoolingLoopOutput(),
                systemEquip.getConditioningStages().getChangeOverCooling().pointExists()
        );

        factory.addChangeHeatingChangeOverRelay(
                systemEquip.getHeatingLoopOutput(),
                systemEquip.getConditioningStages().getChangeOverHeating().pointExists()
        );

        factory.addOccupiedEnabledController(currentOccupancy,
                systemEquip.getConditioningStages().getOccupiedEnabled().pointExists());
        factory.addFanEnableController(
                systemEquip.getFanLoopOutput(), currentOccupancy,
                systemEquip.getConditioningStages().getFanEnable().pointExists()
        );
        factory.addDcvDamperController(
                systemEquip.getDcvLoopOutput(),
                systemEquip.getDabRelayDeactivationHysteresis(),
                currentOccupancy,
                systemEquip.getConditioningStages().getDcvDamper().pointExists()
        );
    }

    private void updatePrerequisite() {
        currentOccupancy.setData(ScheduleManager.getInstance().getSystemOccupancy().ordinal());
        double economization = 0.0;
        if (systemCoolingLoopOp > 0) {
            economization = L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable() ? 1.0 : 0.0;
        }
        economizationAvailable.setData(economization);
        if (systemStatusHandler == null) {
            systemStatusHandler = new SystemStageHandler(systemEquip.getConditioningStages());
        }
    }

    private void updateRelays() {
        getRelayAssiciationMap().forEach( (relay, association) -> {
            double newState = 0;
            if (relay.readDefaultVal() > 0) {
                ModulatingProfileRelayMapping mappedStage = ModulatingProfileRelayMapping.values()[(int) association.readDefaultVal()];
                newState = getStageStatus(mappedStage);
                getLogicalPhysicalMap().get(relay).writePointValue(newState);
                CcuLog.i(L.TAG_CCU_SYSTEM, "Relay updated: " + relay.getDomainName() + ", Stage: " + mappedStage + ", State: " + newState);
            }
        });
    }

    private Map<a75f.io.domain.api.Point, a75f.io.domain.api.Point> getRelayAssiciationMap() {
        Map<a75f.io.domain.api.Point, a75f.io.domain.api.Point> associations = new LinkedHashMap<>();
        if (systemEquip == null) {
            return associations;
        }
        associations.put(systemEquip.getRelay3OutputEnable(), systemEquip.getRelay3OutputAssociation());
        associations.put(systemEquip.getRelay7OutputEnable(), systemEquip.getRelay7OutputAssociation());
        return associations;
    }

    private double getStageStatus(ModulatingProfileRelayMapping stage) {
        return getDomainPointForStage(stage).readHisVal();
    }

    protected a75f.io.domain.api.Point getDomainPointForStage(ModulatingProfileRelayMapping stage) {
        switch (stage) {
            case HUMIDIFIER:
                return systemEquip.getConditioningStages().getHumidifierEnable();
            case DEHUMIDIFIER:
                return systemEquip.getConditioningStages().getDehumidifierEnable();
            case CHANGE_OVER_COOLING:
                return systemEquip.getConditioningStages().getChangeOverCooling();
            case CHANGE_OVER_HEATING:
                return systemEquip.getConditioningStages().getChangeOverHeating();
            case FAN_ENABLE:
                return systemEquip.getConditioningStages().getFanEnable();
            case OCCUPIED_ENABLED:
                return systemEquip.getConditioningStages().getOccupiedEnabled();
            case DCV_DAMPER:
                return systemEquip.getConditioningStages().getDcvDamper();
        }
        CcuLog.e(L.TAG_CCU_SYSTEM, "No domain point found for stage: " + stage);
        return null;
    }
}
