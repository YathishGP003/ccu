package a75f.io.logic.bo.building.system.vav;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getAnalogMax;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getAnalogMin;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getCompressorSpeedSignal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getFanSpeedSignal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getHeatingSignal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getModulatedAnalogVal;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getModulatedAnalogValDuringEcon;
import static a75f.io.logic.bo.building.system.ModulatingProfileUtil.getOutsideAirDamperSignal;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.PhysicalPoint;
import a75f.io.domain.equips.VavModulatingRtuSystemEquip;
import a75f.io.domain.util.CommonQueries;
import a75f.io.domain.util.ModelLoader;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.ModulatingProfileAnalogMapping;
import a75f.io.logic.bo.building.hvac.ModulatingProfileRelayMapping;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemControllerFactory;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemStageHandler;
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuAnalogOutMinMaxConfig;
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig;
import a75f.io.logic.bo.haystack.device.ControlMote;
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective;

/**
 * Default System handles PI controlled op
 */
public class VavFullyModulatingRtu extends VavSystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
    private static final int ANALOG_SCALE = 10;

    public VavModulatingRtuSystemEquip systemEquip;

    private int lastSystemSATRequests = 0;

    private final SystemControllerFactory factory = new SystemControllerFactory();
    private SystemStageHandler systemStatusHandler;

    public VavFullyModulatingRtu() {
    }
    
    public void initTRSystem() {
        trSystem =  new VavTRSystem();
    }
    
    public  int getSystemSAT() {
        return ((VavTRSystem)trSystem).getCurrentSAT();
    }
    
    public  int getSystemCO2() {
        return ((VavTRSystem)trSystem).getCurrentCO2();
    }
    
    public  int getSystemOADamper() {
        return (((VavTRSystem)trSystem).getCurrentCO2() - CO2_MIN) * 100 / (CO2_MAX - CO2_MIN);
    }
    
    public double getStaticPressure() {
        return ((VavTRSystem)trSystem).getCurrentSp();
    }
    
    public int getAnalog1Out() {
        return (int)ControlMote.getAnalogOut("analog1");
    }
    
    public int getAnalog2Out() {
        return (int)ControlMote.getAnalogOut("analog2");
    }
    
    public int getAnalog3Out() {
        return (int)ControlMote.getAnalogOut("analog3");
    }
    
    public int getAnalog4Out() {
        return (int)ControlMote.getAnalogOut("analog4");
    }
    
    public String getProfileName() {
        return "VAV Fully Modulating AHU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_ANALOG_RTU;
    }
    
    @Override
    public void doSystemControl() {
        if (trSystem != null) {
            trSystem.processResetResponse();
        }
        VavSystemController.getInstance().runVavSystemControlAlgo();
        updateSystemPoints();
        setTrTargetVals();
        if (trSystem != null) {
            trSystem.resetRequests();
        }
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return systemEquip.getCoolingSignal().pointExists() || systemEquip.getCompressorSpeed().pointExists();
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return systemEquip.getHeatingSignal().pointExists() || systemEquip.getCompressorSpeed().pointExists();
    }
    
    @Override
    public boolean isCoolingActive(){
        return systemCoolingLoopOp > 0;
    }
    
    @Override
    public boolean isHeatingActive(){
        return systemHeatingLoopOp > 0;
    }

    private synchronized void updateSystemPoints() {

        systemEquip = (VavModulatingRtuSystemEquip) Domain.systemEquip;
        updateOutsideWeatherParams();
        updateMechanicalConditioning(CCUHsApi.getInstance());
        addControllers();
        SystemMode systemMode = SystemMode.values()[(int)systemEquip.getConditioningMode().readPriorityVal()];
        updateSystemCoolingLoop(systemMode);
        updateSystemHeatingLoop();
        updateCompressorLoop();
        double epidemicMode = systemEquip.getEpidemicModeSystemState().readHisVal();
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        updateSystemFanLoop(systemMode, epidemicState);
        updateSystemCo2Loop();

        SeventyFiveFProfileDirective model = (SeventyFiveFProfileDirective) ModelLoader.INSTANCE.getVavModulatingRtuModelDef();
        ModulatingRtuProfileConfig config = new ModulatingRtuProfileConfig(model).getActiveConfiguration();

        Domain.cmBoardDevice.getAnalog1Out().writePointValue(getAnalog1Output(config));
        Domain.cmBoardDevice.getAnalog2Out().writePointValue(getAnalog2Output(config));
        Domain.cmBoardDevice.getAnalog3Out().writePointValue(getAnalog3Output(config));
        Domain.cmBoardDevice.getAnalog4Out().writePointValue(getAnalog4Output(config));

        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp "+systemCoolingLoopOp+ " systemHeatingLoopOp "+ systemHeatingLoopOp
                                   +" systemFanLoopOp "+systemFanLoopOp+" systemCompressorLoopOp "+systemCompressorLoop
                                   +" systemCo2LoopOp "+systemCo2LoopOp);

        updatePrerequisite();
        systemStatusHandler.runControllersAndUpdateStatus(systemEquip, (int) systemEquip.getConditioningMode().readPriorityVal());
        updateRelays();

        setSystemPoint("operating and mode", VavSystemController.getInstance().systemState.ordinal());
        String systemStatus = getStatusMessage();
        String scheduleStatus = ScheduleManager.getInstance().getSystemStatusString();
        CcuLog.d(L.TAG_CCU_SYSTEM, "StatusMessage: "+systemStatus);
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: " +scheduleStatus);
        if (!systemEquip.getEquipStatusMessage().readDefaultStrVal().equals(systemStatus)) {
            systemEquip.getEquipStatusMessage().writeDefaultVal(systemStatus);
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        if (!systemEquip.getEquipScheduleStatus().readDefaultStrVal().equals(scheduleStatus)) {
            systemEquip.getEquipScheduleStatus().writeDefaultVal(scheduleStatus);
        }
    }

    private void updateSystemCoolingLoop(SystemMode systemMode) {
        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemCoolingLoopOp = VavSystemController.getInstance().getCoolingSignal();
        } else if (VavSystemController.getInstance().getSystemState() == COOLING &&
                (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
            double satSpMax = systemEquip.getSatSPMax().readPriorityVal();
            double satSpMin = systemEquip.getSatSPMin().readPriorityVal();
            CcuLog.d(L.TAG_CCU_SYSTEM, "satSpMax :" + satSpMax + " satSpMin: " + satSpMin + " SAT: " + getSystemSAT());
            /*
                During Unoccupied Mode, AHU should only run if a sufficient # of zones are generating SAT requests. Once enabled,
                AHU should run until all zones are satisfied.

                This logic sets setupModeActive=true when systemSATRequests exceeds systemSATIgnores. It remains true until
                systemSATRequests has dropped to zero or the system returns to Occupied.

                In Unoccupied Mode, coolingLoopOutput is now set to 0 unless setupModeActive=true. This will prevent the AHU from
                waiting to trim all loops down to minimum before shutting off after the schedule goes Unoccupied.
             */
            if (!isSystemOccupied()) {
                int systemSATRequests = getSystemSATRequests();
                double systemSATIgnores = systemEquip.getSatIgnoreRequest().readPriorityVal();
                if ((!setupModeActive) && systemSATRequests > systemSATIgnores) {
                    CcuLog.i(L.TAG_CCU_SYSTEM, "# of zone SAT Requests (" + systemSATRequests + ") is above threshold of " + systemSATIgnores + "; system entering setup mode for unoccupied conditioning");
                    setupModeActive = true;
                }
                if (setupModeActive) {
                    systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;
                    // Once systemSATRequests has dropped to zero, exit setup mode
                    if (systemSATRequests == 0 && lastSystemSATRequests == 0) {
                        CcuLog.i(L.TAG_CCU_SYSTEM, "No more zone SAT requests and 0 coolingLoopOutput; system exiting unoccupied Setup Mode");
                        setupModeActive = false;
                    }
                } else {
                    systemCoolingLoopOp = 0;
                }
                lastSystemSATRequests = 0;
            } else {
                setupModeActive = false;
                systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;
            }
        } else {
            systemCoolingLoopOp = 0;
        }
        systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);
        systemCoolingLoopOp = systemEquip.getCoolingLoopOutput().readHisVal();
    }

    private void updateSystemHeatingLoop() {
        if (VavSystemController.getInstance().getSystemState() == HEATING) {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
        systemEquip.getHeatingLoopOutput().writePointValue(systemHeatingLoopOp);
        systemHeatingLoopOp = systemEquip.getHeatingLoopOutput().readHisVal();
    }

    private void updateSystemFanLoop(SystemMode systemMode, EpidemicState epidemicState) {
        double analogFanSpeedMultiplier = systemEquip.getVavAnalogFanSpeedMultiplier().readPriorityVal();

        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemFanLoopOp = getSingleZoneFanLoopOp(analogFanSpeedMultiplier);

        } else if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE)
                && (L.ccu().oaoProfile != null)){

            double smartPurgeVAVFanLoopOp = L.ccu().oaoProfile.getOAOEquip().getSystemPurgeVavMinFanLoopOutput().readPriorityVal();
            double spSpMax = systemEquip.getStaticPressureSPMax().readPriorityVal();
            double spSpMin = systemEquip.getStaticPressureSPMin().readPriorityVal();
            double staticPressureLoopOutput = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax - spSpMin)) ;
            if((VavSystemController.getInstance().getSystemState() == COOLING) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
                if(staticPressureLoopOutput < ((spSpMax - spSpMin) * smartPurgeVAVFanLoopOp)){
                    systemFanLoopOp = ((spSpMax - spSpMin) * smartPurgeVAVFanLoopOp);
                } else {
                    systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
                }
            } else if(VavSystemController.getInstance().getSystemState() == HEATING) {
                systemFanLoopOp = Math.max((int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier), smartPurgeVAVFanLoopOp);
            } else {
                systemFanLoopOp = smartPurgeVAVFanLoopOp;
            }

        } else if ((VavSystemController.getInstance().getSystemState() == COOLING)
                && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {

            double spSpMax = systemEquip.getStaticPressureSPMax().readPriorityVal();
            double spSpMin = systemEquip.getStaticPressureSPMin().readPriorityVal();

            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure());

            // If schedule is Unoccupied, fan should only run if there is conditioning. In this case, that translates to CoolingLoopOp > 0.
            if (isSystemOccupied() || systemCoolingLoopOp > 0) {
                systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax - spSpMin));
            } else {
                systemFanLoopOp = 0;
            }
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
        systemFanLoopOp = Math.min(systemFanLoopOp, 100);
        systemEquip.getFanLoopOutput().writePointValue(systemFanLoopOp);
        systemFanLoopOp = systemEquip.getFanLoopOutput().readHisVal();
    }

    private void updateSystemCo2Loop() {
        systemCo2LoopOp = 0;

        if (VavSystemController.getInstance().getSystemState() == COOLING) {
            systemCo2LoopOp = (systemEquip.getCo2SPMax().readPriorityVal() - getSystemCO2()) * 100 / 200;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            double co2Val = VavSystemController.getInstance().getSystemCO2WA();
            if (co2Val > 0) {
                systemCo2LoopOp = (co2Val - systemEquip.getCo2Threshold().readPriorityVal()) * 100 / 200;
            }
        }

        systemCo2LoopOp = Math.min(systemCo2LoopOp, 100);
        systemCo2LoopOp = Math.max(systemCo2LoopOp, 0);

        systemEquip.getCo2LoopOutput().writePointValue(systemCo2LoopOp);
        systemCo2LoopOp = systemEquip.getCo2LoopOutput().readHisVal();

        systemEquip.getDcvLoopOutput().writePointValue(systemCo2LoopOp);
        systemDcvLoopOp = systemEquip.getDcvLoopOutput().readHisVal();
    }

    private void updateCompressorLoop() {
        if (VavSystemController.getInstance().getSystemState() == COOLING && !isCoolingLockoutActive()) {
            systemCompressorLoop = systemCoolingLoopOp;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING && !isHeatingLockoutActive()) {
            systemCompressorLoop = systemHeatingLoopOp;
        } else {
            systemCompressorLoop = 0;
        }
        systemEquip.getCompressorLoopOutput().writePointValue(systemCompressorLoop);
        systemCompressorLoop = systemEquip.getCompressorLoopOutput().readHisVal();
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
        }
        CcuLog.d(L.TAG_CCU_SYSTEM, "Analog Out Mapping: " + analogMapping + " Signal: " + signal);
        return signal;
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
                return getModulatedAnalogValDuringEcon(analogMin, analogMax, systemCoolingLoopOp, economizingToMainCoolingLoopMap);
            }
        }
        return  (int)(analogMin * ANALOG_SCALE);
    }

    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        boolean economizingOn = systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable();
        status.append((systemFanLoopOp > 0 || Domain.cmBoardDevice.getRelay3().readHisVal() > 0.01 ) ? " Fan ON ": "");
        if(economizingOn) {
            double economizingToMainCoolingLoopMap =  L.ccu().oaoProfile.getOAOEquip().getEconomizingToMainCoolingLoopMap().readPriorityVal();
            status.append((systemCoolingLoopOp >= economizingToMainCoolingLoopMap && !isCoolingLockoutActive())? " | Cooling ON ":"");
        } else {
            status.append((systemCoolingLoopOp > 0 && !isCoolingLockoutActive())? " | Cooling ON ":"");
        }
        status.append((systemHeatingLoopOp > 0 && !isHeatingLockoutActive())? " | Heating ON ":"");
        
        if (economizingOn) {
            status.insert(0, "Free Cooling Used |");
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
        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "VavFullyModulatingRtu adding system equip");
        systemEquip = (VavModulatingRtuSystemEquip) Domain.systemEquip;
        initTRSystem();
        systemStatusHandler = new SystemStageHandler(systemEquip.getConditioningStages());
        //updateSystemPoints();
    }
    
    @Override
    public synchronized void deleteSystemEquip() {

        ArrayList<HashMap<Object, Object>> listOfEquips = CCUHsApi.getInstance().readAllEntities(CommonQueries.SYSTEM_PROFILE);
        for (HashMap<Object, Object> equip : listOfEquips) {
            if(equip.get("profile") != null){
                if(ProfileType.getProfileTypeForName(equip.get("profile").toString()) != null){
                    if (ProfileType.getProfileTypeForName(equip.get("profile").toString()).name().equals(ProfileType.SYSTEM_VAV_ANALOG_RTU.name())) {
                        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "VavFullyModulatingRtu removing profile with it id-->"+equip.get("id").toString());
                        CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
                    }
                }
            }
        }

        removeSystemEquipModbus();
        removeSystemEquipBacnet();
        deleteSystemConnectModule();
    }

    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        try {
            CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and his and " + cmd, val);
        }catch (Exception e){
            e.printStackTrace();
        }
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
        HashMap<Object, Object> configPoint = hayStack.readEntity("point and system and config and output and enabled and "+config);
        if (configPoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_SYSTEM," !!!  System config enable point does not exist !!! - "+config);
            return 0;
        }
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());

    }

    public Map<a75f.io.domain.api.Point, PhysicalPoint> getLogicalPhysicalMap() {
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
        factory.addHumidifierController(systemEquip,
                systemEquip.getAverageHumidity(),
                systemEquip.getSystemtargetMinInsideHumidity(),
                systemEquip.getVavRelayDeactivationHysteresis(),
                systemEquip.getCurrentOccupancy()
        );

        factory.addDeHumidifierController(systemEquip,
                systemEquip.getAverageHumidity(),
                systemEquip.getSystemtargetMaxInsideHumidity(),
                systemEquip.getVavRelayDeactivationHysteresis(),
                systemEquip.getCurrentOccupancy()
        );

        factory.addChangeCoolingChangeOverRelay(systemEquip, systemEquip.getCoolingLoopOutput());
        factory.addChangeHeatingChangeOverRelay(systemEquip, systemEquip.getHeatingLoopOutput());
        factory.addOccupiedEnabledController(systemEquip, systemEquip.getCurrentOccupancy());
        factory.addFanEnableController(
                systemEquip, systemEquip.getFanLoopOutput(), systemEquip.getCurrentOccupancy()
        );
        factory.addDcvDamperController(
                systemEquip,
                systemEquip.getDcvLoopOutput(),
                systemEquip.getVavRelayDeactivationHysteresis(),
                systemEquip.getCurrentOccupancy()
        );
    }

    private void updatePrerequisite() {
        systemEquip.getCurrentOccupancy().setData(ScheduleManager.getInstance().getSystemOccupancy().ordinal());
        double economization = 0.0;
        if (systemCoolingLoopOp > 0) {
            economization = L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable() ? 1.0 : 0.0;
        }
        systemEquip.getEconomizationAvailable().setData(economization);
        if (systemStatusHandler == null) {
            systemStatusHandler = new SystemStageHandler(systemEquip.getConditioningStages());
        }
    }

    private void updateRelays() {
        getRelayAssiciationMap().forEach( (relay, association) -> {
            double newState = 0;
            if (relay.readDefaultVal() > 0) {
                ModulatingProfileRelayMapping mappedStage = ModulatingProfileRelayMapping.values()[(int) association.readDefaultVal()];
                CcuLog.d(L.TAG_CCU_SYSTEM, "Updating relay: " + relay.getDomainName() +
                        ", Association: " + association.getDomainName()+" mappedStage: " + mappedStage);
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
        return null;
    }
}
