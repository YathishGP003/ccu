package a75f.io.logic.bo.building.system.vav;

import static a75f.io.logic.bo.building.hvac.Stage.COMPRESSOR_1;
import static a75f.io.logic.bo.building.hvac.Stage.COMPRESSOR_2;
import static a75f.io.logic.bo.building.hvac.Stage.COMPRESSOR_3;
import static a75f.io.logic.bo.building.hvac.Stage.COMPRESSOR_4;
import static a75f.io.logic.bo.building.hvac.Stage.COMPRESSOR_5;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_1;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_2;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_3;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_4;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_5;
import static a75f.io.logic.bo.building.hvac.Stage.DEHUMIDIFIER;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_1;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_2;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_3;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_4;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_5;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_1;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_2;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_3;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_4;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_5;
import static a75f.io.logic.bo.building.hvac.Stage.HUMIDIFIER;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;
import static a75f.io.logic.bo.util.DesiredTempDisplayMode.setSystemModeForVav;
import static a75f.io.logic.controlcomponents.util.ControllerNames.COOLING_STAGE_CONTROLLER;
import static a75f.io.logic.controlcomponents.util.ControllerNames.FAN_SPEED_CONTROLLER;
import static a75f.io.logic.controlcomponents.util.ControllerNames.HEATING_STAGE_CONTROLLER;

import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.equips.ConditioningStages;
import a75f.io.domain.equips.VavStagedSystemEquip;
import a75f.io.domain.equips.VavStagedVfdSystemEquip;
import a75f.io.domain.util.CommonQueries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemControllerFactory;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.SystemStageHandler;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;
import a75f.io.logic.util.SystemProfileUtil;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

public class VavStagedRtu extends VavSystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
    public int heatingStages = 0;
    public int coolingStages = 0;
    public int fanStages = 0;
    public int compressorStages = 0;

    public int stageUpTimerCounter = 0;
    public int stageDownTimerCounter = 0;
    public boolean changeOverStageDownTimerOverrideActive = false;
    SystemController.State currentConditioning = OFF;

    public VavStagedSystemEquip systemEquip;

    private int lastSystemSATRequests = 0;

    public void initTRSystem() {
        trSystem =  new VavTRSystem();
    }
    
    public String getProfileName()
    {
        return "VAV Staged RTU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_STAGED_RTU;
    }
    
    public VavStagedRtu() {
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

    SystemControllerFactory factory = new SystemControllerFactory();
    SystemStageHandler systemStatusHandler;

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
    public void addSystemEquip() {
        systemEquip = (VavStagedSystemEquip) Domain.systemEquip;
        initTRSystem();
        updateStagesSelected();
        systemStatusHandler = new SystemStageHandler(systemEquip.getConditioningStages());
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return (coolingStages > 0 || compressorStages > 0);
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0 || compressorStages > 0);
    }

    @Override
    public boolean isCoolingActive(){
        return systemEquip.getConditioningStages().getCoolingStage1().readHisVal() > 0
                || systemEquip.getConditioningStages().getCoolingStage2().readHisVal() > 0
                || systemEquip.getConditioningStages().getCoolingStage3().readHisVal() > 0
                || systemEquip.getConditioningStages().getCoolingStage4().readHisVal() > 0
                || systemEquip.getConditioningStages().getCoolingStage5().readHisVal() > 0;

    }

    @Override
    public boolean isHeatingActive(){
        return systemEquip.getConditioningStages().getHeatingStage1().readHisVal() > 0
                || systemEquip.getConditioningStages().getHeatingStage2().readHisVal() > 0
                || systemEquip.getConditioningStages().getHeatingStage3().readHisVal() > 0
                || systemEquip.getConditioningStages().getHeatingStage4().readHisVal() > 0
                || systemEquip.getConditioningStages().getHeatingStage5().readHisVal() > 0;
    }

    public boolean isCompressorActive() {
        return systemEquip.getConditioningStages().getCompressorStage1().readHisVal() > 0
                || systemEquip.getConditioningStages().getCompressorStage2().readHisVal() > 0
                || systemEquip.getConditioningStages().getCompressorStage3().readHisVal() > 0
                || systemEquip.getConditioningStages().getCompressorStage4().readHisVal() > 0
                || systemEquip.getConditioningStages().getCompressorStage5().readHisVal() > 0;
    }

    public void addControllers() {
        factory.addCoolingControllers(systemEquip,
                systemEquip.getCoolingLoopOutput(),
                systemEquip.getVavRelayDeactivationHysteresis(),
                systemEquip.getVavStageUpTimerCounter(),
                systemEquip.getVavStageDownTimerCounter(),
                systemEquip.getEconomizationAvailable(),
                coolingStages);

        factory.addHeatingControllers(systemEquip,
                systemEquip.getHeatingLoopOutput(),
                systemEquip.getVavRelayDeactivationHysteresis(),
                systemEquip.getVavStageUpTimerCounter(),
                systemEquip.getVavStageDownTimerCounter(),
                heatingStages);

        factory.addFanControllers(systemEquip,
                systemEquip.getFanLoopOutput(),
                systemEquip.getVavRelayDeactivationHysteresis(),
                systemEquip.getVavStageUpTimerCounter(),
                systemEquip.getVavStageDownTimerCounter(),
                fanStages);

        factory.addCompressorControllers(systemEquip,
                systemEquip.getCompressorLoopOutput(),
                systemEquip.getRelayActivationHysteresis(),
                systemEquip.getVavStageUpTimerCounter(),
                systemEquip.getVavStageDownTimerCounter(),
                systemEquip.getEconomizationAvailable(),
                compressorStages
        );

        factory.addHumidifierController(systemEquip,
                systemEquip.getAverageHumidity(),
                systemEquip.getSystemtargetMinInsideHumidity(),
                systemEquip.getRelayActivationHysteresis(),
                systemEquip.getCurrentOccupancy()
        );

        factory.addDeHumidifierController(systemEquip,
                systemEquip.getAverageHumidity(),
                systemEquip.getSystemtargetMaxInsideHumidity(),
                systemEquip.getRelayActivationHysteresis(),
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
                systemEquip.getRelayActivationHysteresis(),
                systemEquip.getCurrentOccupancy()
        );


    }

    protected synchronized void updateSystemPoints() {

        updateStagesSelected();
        addControllers();

        updateOutsideWeatherParams();
        updateMechanicalConditioning(CCUHsApi.getInstance());

        SystemMode systemMode = SystemMode.values()[(int)systemEquip.getConditioningMode().readPriorityVal()];
    
        if (currentConditioning == OFF) {
            currentConditioning = getSystemController().getSystemState();
            changeOverStageDownTimerOverrideActive = false;
        } else if (currentConditioning != getSystemController().getSystemState()) {
            currentConditioning = getSystemController().getSystemState();
            changeOverStageDownTimerOverrideActive = true;
        } else {
            changeOverStageDownTimerOverrideActive = false;
        }

        if (changeOverStageDownTimerOverrideActive) {
            resetControllers(factory, systemEquip);
        }
        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemCoolingLoopOp = VavSystemController.getInstance().getCoolingSignal();
        } else if ((VavSystemController.getInstance().getSystemState() == COOLING)
                    && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
            double satSpMax = systemEquip.getSatSPMax().readPriorityVal();
            double satSpMin = systemEquip.getSatSPMin().readPriorityVal();
    
            CcuLog.d(L.TAG_CCU_SYSTEM,"satSpMax :"+satSpMax+" satSpMin: "+satSpMin+" SAT: "+getSystemSAT());

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

                double systemSATIgnores = TunerUtil.readTunerValByQuery("sat and ignoreRequest", getSystemEquipRef());

                if ((!setupModeActive) && (systemSATRequests > systemSATIgnores)) {
                    CcuLog.i(L.TAG_CCU_SYSTEM, "# of zone SAT Requests (" + systemSATRequests + ") is above threshold of " + systemSATIgnores + "; system entering setup mode for unoccupied conditioning");
                    setupModeActive = true;
                }

                if (setupModeActive) {
                    systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;

                    // Once systemSATRequests has dropped to zero, exit setup mode
                    if (systemSATRequests == 0 && lastSystemSATRequests == 0) {
                        CcuLog.i(L.TAG_CCU_SYSTEM, "No more zone SAT requests; system exiting unoccupied Setup Mode");
                        setupModeActive = false;
                    }

                } else {
                    systemCoolingLoopOp = 0;
                }

                lastSystemSATRequests = systemSATRequests;

            } else {
                setupModeActive = false;
                systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;
            }
        } else {
            systemCoolingLoopOp = 0;
        }

        if (VavSystemController.getInstance().getSystemState() == HEATING) {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }

        if (currentConditioning == COOLING) {
            systemCompressorLoop = systemCoolingLoopOp;
        } else if (currentConditioning == HEATING) {
            systemCompressorLoop = systemHeatingLoopOp;
        } else {
            systemCompressorLoop = 0;
        }

        double analogFanSpeedMultiplier = systemEquip.getVavAnalogFanSpeedMultiplier().readPriorityVal();
        double epidemicMode = systemEquip.getEpidemicModeSystemState().readHisVal();
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
    
        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemFanLoopOp = getSingleZoneFanLoopOp(analogFanSpeedMultiplier);
        } else if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE ) && (L.ccu().oaoProfile != null)) {
            //TODO- Part OAO. Will be replaced with domanName later.
            double smartPurgeDabFanLoopOp = L.ccu().oaoProfile.getOAOEquip().getSystemPurgeVavMinFanLoopOutput().readPriorityVal();
            double spSpMax = systemEquip.getStaticPressureSPMax().readPriorityVal();
            double spSpMin = systemEquip.getStaticPressureSPMin().readPriorityVal();

            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure()+","+smartPurgeDabFanLoopOp);
            double staticPressureLoopOutput = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
            if((VavSystemController.getInstance().getSystemState() == COOLING)
                                        && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
                if(staticPressureLoopOutput < ((spSpMax - spSpMin) * smartPurgeDabFanLoopOp)) {
                    systemFanLoopOp = ((spSpMax - spSpMin) * smartPurgeDabFanLoopOp);
                }
                else {
                    systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax - spSpMin));
                }
            } else if(VavSystemController.getInstance().getSystemState() == HEATING) {
                systemFanLoopOp = Math.max((int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier), smartPurgeDabFanLoopOp);
            } else {
                systemFanLoopOp = smartPurgeDabFanLoopOp;
            }

        } else if (VavSystemController.getInstance().getSystemState() == COOLING
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
            
        } else if (VavSystemController.getInstance().getSystemState() == HEATING) {
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
        systemFanLoopOp = Math.min(systemFanLoopOp, 100);

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

        systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);
        systemCoolingLoopOp = systemEquip.getCoolingLoopOutput().readHisVal();
        systemEquip.getHeatingLoopOutput().writePointValue(systemHeatingLoopOp);
        systemHeatingLoopOp = systemEquip.getHeatingLoopOutput().readHisVal();
        systemEquip.getFanLoopOutput().writePointValue(systemFanLoopOp);
        systemFanLoopOp = systemEquip.getFanLoopOutput().readHisVal();
        systemEquip.getCo2LoopOutput().writePointValue(systemCo2LoopOp);
        systemCo2LoopOp = systemEquip.getCo2LoopOutput().readHisVal();
        systemEquip.getCompressorLoopOutput().writePointValue(systemCompressorLoop);
        systemCompressorLoop = systemEquip.getCompressorLoopOutput().readHisVal();

        systemEquip.getDcvLoopOutput().writePointValue(systemCo2LoopOp);
        systemDcvLoopOp = systemEquip.getDcvLoopOutput().readHisVal();

        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp "+systemCoolingLoopOp+
                                   " systemHeatingLoopOp "+ systemHeatingLoopOp+" " + "systemFanLoopOp "+systemFanLoopOp);

        updatePrerequisite();
        systemStatusHandler.runControllersAndUpdateStatus(systemEquip, (int) systemEquip.getConditioningMode().readPriorityVal());
        updateRelays();
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "stageUpTimerCounter "+stageUpTimerCounter+
                                   " stageDownTimerCounter "+ stageDownTimerCounter+" " +
                                   "changeOverStageDownTimerOverrideActive "+changeOverStageDownTimerOverrideActive);

        systemEquip.getOperatingMode().writeHisVal(VavSystemController.getInstance().systemState.ordinal());
        String systemStatus = getStatusMessage();
        String scheduleStatus =  ScheduleManager.getInstance().getSystemStatusString();
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

    protected a75f.io.domain.api.Point getDomainPointForStage(Stage stage) {
        switch (stage) {
            case COOLING_1:
                return systemEquip.getConditioningStages().getCoolingStage1();
            case COOLING_2:
                return systemEquip.getConditioningStages().getCoolingStage2();
            case COOLING_3:
                return systemEquip.getConditioningStages().getCoolingStage3();
            case COOLING_4:
                return systemEquip.getConditioningStages().getCoolingStage4();
            case COOLING_5:
                return systemEquip.getConditioningStages().getCoolingStage5();
            case HEATING_1:
                return systemEquip.getConditioningStages().getHeatingStage1();
            case HEATING_2:
                return systemEquip.getConditioningStages().getHeatingStage2();
            case HEATING_3:
                return systemEquip.getConditioningStages().getHeatingStage3();
            case HEATING_4:
                return systemEquip.getConditioningStages().getHeatingStage4();
            case HEATING_5:
                return systemEquip.getConditioningStages().getHeatingStage5();
            case FAN_1:
                return systemEquip.getConditioningStages().getFanStage1();
            case FAN_2:
                return systemEquip.getConditioningStages().getFanStage2();
            case FAN_3:
                return systemEquip.getConditioningStages().getFanStage3();
            case FAN_4:
                return systemEquip.getConditioningStages().getFanStage4();
            case FAN_5:
                return systemEquip.getConditioningStages().getFanStage5();
            case HUMIDIFIER:
                return systemEquip.getConditioningStages().getHumidifierEnable();
            case DEHUMIDIFIER:
                return systemEquip.getConditioningStages().getDehumidifierEnable();
            case COMPRESSOR_1:
                return systemEquip.getConditioningStages().getCompressorStage1();
            case COMPRESSOR_2:
                return systemEquip.getConditioningStages().getCompressorStage2();
            case COMPRESSOR_3:
                return systemEquip.getConditioningStages().getCompressorStage3();
            case COMPRESSOR_4:
                return systemEquip.getConditioningStages().getCompressorStage4();
            case COMPRESSOR_5:
                return systemEquip.getConditioningStages().getCompressorStage5();
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
    
    private Set<a75f.io.domain.api.Point> getRelayMappingForStage(Stage stage) {
        Set<a75f.io.domain.api.Point> relaySet= new HashSet<>();

        getRelayAssiciationMap().forEach( (relay, association) -> {
            if (relay.readDefaultVal() > 0 && stage.ordinal() == association.readDefaultVal()) {
                relaySet.add(relay);
            }
        });
        return relaySet;
    }
    
    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();

        ConditioningStages systemStages = systemEquip.getConditioningStages();
        status.append(systemStages.getFanStage1().readHisVal() > 0 ? "1":"");
        status.append(systemStages.getFanStage2().readHisVal() > 0 ? ",2":"");
        status.append(systemStages.getFanStage3().readHisVal() > 0 ? ",3":"");
        status.append(systemStages.getFanStage4().readHisVal() > 0 ? ",4":"");
        status.append(systemStages.getFanStage5().readHisVal() > 0 ? ",5":"");
        if (status.length() > 0) {
            status.insert(0, "Fan Stages ");
            status.append(" ON ");
        }

        if (isCoolingActive() || (systemCoolingLoopOp > 0 && isCompressorActive())) {
            status.append("| Cooling Stage " + ((systemStages.getCoolingStage1().readHisVal() > 0) || (systemStages.getCompressorStage1().readHisVal() > 0) ? "1" : ""));
            status.append((systemStages.getCoolingStage2().readHisVal() > 0 || systemStages.getCompressorStage2().readHisVal() > 0) ? ",2" : "");
            status.append((systemStages.getCoolingStage3().readHisVal() > 0 || systemStages.getCompressorStage3().readHisVal() > 0) ? ",3" : "");
            status.append((systemStages.getCoolingStage4().readHisVal() > 0 || systemStages.getCompressorStage4().readHisVal() > 0) ? ",4" : "");
            status.append((systemStages.getCoolingStage5().readHisVal() > 0 || systemStages.getCompressorStage5().readHisVal() > 0) ? ",5 ON " : " ON ");

        }

        if (isHeatingActive() || (systemHeatingLoopOp > 0 && isCompressorActive())) {
            status.append("| Heating Stage " + (((systemStages.getHeatingStage1().readHisVal() > 0) ||
                    systemStages.getCompressorStage1().readHisVal() > 0) ? "1" : ""));
            status.append((systemStages.getHeatingStage2().readHisVal() > 0 || systemStages.getCompressorStage2().readHisVal() > 0) ? ",2" : "");
            status.append((systemStages.getHeatingStage3().readHisVal() > 0 || systemStages.getCompressorStage3().readHisVal() > 0) ? ",3" : "");
            status.append((systemStages.getHeatingStage4().readHisVal() > 0 || systemStages.getCompressorStage4().readHisVal() > 0) ? ",4" : "");
            status.append((systemStages.getHeatingStage5().readHisVal() > 0 || systemStages.getCompressorStage5().readHisVal() > 0) ? ",5 ON " : " ON ");
        }

        if (systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
            status.insert(0, "Free Cooling Used | ");
        }
        if (L.ccu().systemProfile instanceof VavStagedRtuWithVfd) {
            VavStagedVfdSystemEquip vfdSystemEquip = (VavStagedVfdSystemEquip) Domain.systemEquip;
            status.append(vfdSystemEquip.getFanSignal().readHisVal() > 0 ? " Analog Fan ON " : "");
        }

        if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_HYBRID_RTU) {
            return status.toString().isEmpty() ? "System OFF" + SystemProfileUtil.isDeHumidifierOn()
                    + SystemProfileUtil.isHumidifierOn() : status + SystemProfileUtil.isDeHumidifierOn()
                    + (SystemProfileUtil.isHumidifierOn());
        }

        String humidifierStatus = getRelayMappingForStage(HUMIDIFIER).isEmpty() ? "" :
                systemStages.getHumidifierEnable().readHisVal() > 0 ? " | Humidifier ON " : " | Humidifier OFF ";
        String dehumidifierStatus = getRelayMappingForStage(DEHUMIDIFIER).isEmpty() ? "" :
                systemStages.getDehumidifierEnable().readHisVal() > 0 ? " | Dehumidifier ON " : " | Dehumidifier OFF ";

        return status.toString().equals("") ? "System OFF" + humidifierStatus + dehumidifierStatus :
                status + humidifierStatus + dehumidifierStatus;
    }
    
    public void updateStagesSelected() {
        systemEquip = (VavStagedSystemEquip) Domain.systemEquip;
        coolingStages = 0;
        heatingStages = 0;
        fanStages = 0;
        compressorStages = 0;
        getRelayAssiciationMap().forEach( (relay, association) -> {
            CcuLog.i(L.TAG_CCU_SYSTEM, relay.getDomainName()+" enabled "+relay.readDefaultVal()+" association "+association.readDefaultVal());
            if (relay.readDefaultVal() > 0) {
                int val = (int)association.readDefaultVal();
                if (val <= Stage.COOLING_5.ordinal() && val >= coolingStages) {
                    coolingStages = val + 1;
                } else if (val >= Stage.HEATING_1.ordinal() && val <= HEATING_5.ordinal() && val >= heatingStages) {
                    heatingStages = val + 1;
                } else if (val >= Stage.FAN_1.ordinal() && val <= Stage.FAN_5.ordinal() && val >= fanStages) {
                    fanStages = val + 1;
                } else if (val >= Stage.COMPRESSOR_1.ordinal() && val <= Stage.COMPRESSOR_5.ordinal() && val >= compressorStages) {
                    compressorStages = val + 1;
                }
            }
        });
        
        if ((heatingStages > 0)) {
            heatingStages -= Stage.HEATING_1.ordinal();
        }
        
        if (fanStages > 0) {
            fanStages -= Stage.FAN_1.ordinal();
        }

        if (compressorStages > 0) {
            compressorStages -= Stage.COMPRESSOR_1.ordinal();
        }
        CcuLog.i(L.TAG_CCU_SYSTEM, "updateStagesSelected Cooling stages: "+coolingStages+" Heating stages: "+heatingStages+" Fan stages: "+fanStages+
                " Compressor stages: "+compressorStages);
    }
    
    public boolean isStageEnabled(Stage s) {
        AtomicBoolean enabled = new AtomicBoolean(false);
        getRelayAssiciationMap().forEach( (relay, association) -> {
            if (relay.readDefaultVal() > 0 && s.ordinal() == association.readDefaultVal()) {
                enabled.set(true);
            }
        });
        return enabled.get();
    }
    
    private boolean isStageMapped(Stage stage) {
        for (int relayCount = 1; relayCount <= 7; relayCount++) {
            if (stage.ordinal() == getConfigAssociation("relay" + relayCount)) {
                return true;
            }
        }
        return false;
    }
    public double getStageStatus(Stage stage) {
        return getDomainPointForStage(stage).readHisVal();
    }
    private void updateRelays() {
        getRelayAssiciationMap().forEach( (relay, association) -> {
            double newState = 0;
            if (relay.readDefaultVal() > 0) {
                Stage mappedStage = Stage.values()[(int) association.readDefaultVal()];
                newState = getStageStatus(mappedStage);
                getLogicalPhysicalMap().get(relay).writePointValue(newState);
                CcuLog.i(L.TAG_CCU_SYSTEM, "Relay updated: " + relay.getDomainName() + ", Stage: " + mappedStage + ", State: " + newState);
            }
        });
    }
    protected Map<a75f.io.domain.api.Point, a75f.io.domain.api.Point> getRelayAssiciationMap() {
        Map<a75f.io.domain.api.Point, a75f.io.domain.api.Point> associations = new LinkedHashMap<>();
        if (systemEquip == null) {
            return associations;
        }
        associations.put(systemEquip.getRelay1OutputEnable(), systemEquip.getRelay1OutputAssociation());
        associations.put(systemEquip.getRelay2OutputEnable(), systemEquip.getRelay2OutputAssociation());
        associations.put(systemEquip.getRelay3OutputEnable(), systemEquip.getRelay3OutputAssociation());
        associations.put(systemEquip.getRelay4OutputEnable(), systemEquip.getRelay4OutputAssociation());
        associations.put(systemEquip.getRelay5OutputEnable(), systemEquip.getRelay5OutputAssociation());
        associations.put(systemEquip.getRelay6OutputEnable(), systemEquip.getRelay6OutputAssociation());
        associations.put(systemEquip.getRelay7OutputEnable(), systemEquip.getRelay7OutputAssociation());
        return associations;
    }

    public Map<a75f.io.domain.api.Point, a75f.io.domain.api.PhysicalPoint> getLogicalPhysicalMap() {
        Map<a75f.io.domain.api.Point, a75f.io.domain.api.PhysicalPoint> map = new HashMap<>();
        if (systemEquip == null) {
            return map;
        }
            map.put(systemEquip.getRelay1OutputEnable(), Domain.cmBoardDevice.getRelay1());
            map.put(systemEquip.getRelay2OutputEnable(), Domain.cmBoardDevice.getRelay2());
            map.put(systemEquip.getRelay3OutputEnable(), Domain.cmBoardDevice.getRelay3());
            map.put(systemEquip.getRelay4OutputEnable(), Domain.cmBoardDevice.getRelay4());
            map.put(systemEquip.getRelay5OutputEnable(), Domain.cmBoardDevice.getRelay5());
            map.put(systemEquip.getRelay6OutputEnable(), Domain.cmBoardDevice.getRelay6());
            map.put(systemEquip.getRelay7OutputEnable(), Domain.cmBoardDevice.getRelay7());
        return map;
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
    public void setConfigEnabled(String config, double val) {
        //sysEquip.setConfigEnabled(config, val);
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        ccuHsApi.writeDefaultVal("point and system and config and output and enabled and "+config, val);
        setSystemModeForVav(ccuHsApi); //TODO
    }
    
    public double getConfigAssociation(String config) {
        //return sysEquip.getRelayOpAssociation(config);
        //return CCUHsApi.getInstance().readDefaultVal("point and system and config and output and association and "+config);

        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and association and "+config);
        if (configPoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_SYSTEM," !!!  System config point does not exist !!! - "+config);
            return 0;
        }
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }
    public void setConfigAssociation(String config, double val) {

        double curConfigVal = getConfigAssociation(config);
        if (curConfigVal == val) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "setConfigAssociation not changed cur:"+curConfigVal+" new:"+val);
            return;
        }

        CCUHsApi.getInstance().writeDefaultVal("point and system and config and output and association and " + config, val);
        Stage curstage = Stage.values()[(int)curConfigVal];
    
        HashMap cmd = null;
        Point newCmdPoint = null;
        Point oldPoint = null;
        Stage updatedStage = Stage.values()[(int)val];
        int newStageNum = 1;
        int curStageNum = 0;
        switch (updatedStage){
            case COOLING_1:
            case COOLING_2:
            case COOLING_3:
            case COOLING_4:
            case COOLING_5:
                newStageNum = updatedStage.ordinal() + 1;
                break;
            case HEATING_1:
            case HEATING_2:
            case HEATING_3:
            case HEATING_4:
            case HEATING_5:
                newStageNum = updatedStage.ordinal() - COOLING_5.ordinal();
                break;
            case FAN_1:
            case FAN_2:
            case FAN_3:
            case FAN_4:
            case FAN_5:
                newStageNum = updatedStage.ordinal() - HEATING_5.ordinal();
                break;
        }

        CcuLog.d(L.TAG_CCU_SYSTEM, " vavStageRTU setConfigAssociation for relay ="+newStageNum+","+curstage+","+updatedStage);
        if(curstage != updatedStage) {
            HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
            String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
            switch (curstage) {
                case COOLING_1:
                case COOLING_2:
                case COOLING_3:
                case COOLING_4:
                case COOLING_5:
                    if (!isStageEnabled(curstage)) {
                        curStageNum = curstage.ordinal() + 1;
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and cooling and stage" + curStageNum);
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
                case HEATING_1:
                case HEATING_2:
                case HEATING_3:
                case HEATING_4:
                case HEATING_5:
                    if (!isStageEnabled(curstage)) {
                        curStageNum = curstage.ordinal() - COOLING_5.ordinal();
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and heating and stage" + curStageNum);
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
                case FAN_1:
                case FAN_2:
                case FAN_3:
                case FAN_4:
                case FAN_5:
                    if (!isStageEnabled(curstage)) {
                        curStageNum = curstage.ordinal() - HEATING_5.ordinal();
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and fan and stage" + curStageNum);
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
                case HUMIDIFIER:
                    if (!isStageEnabled(curstage)) {
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and humidifier");
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
                case DEHUMIDIFIER:
                    if (!isStageEnabled(curstage)) {
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and dehumidifier");
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
            }
    
            String timeZone = CCUHsApi.getInstance().getTimeZone();
    
            Equip systemEquip = new Equip.Builder()
                                    .setHashMap(CCUHsApi.getInstance().read(CommonQueries.SYSTEM_PROFILE)).build();
    
            if (val <= Stage.COOLING_5.ordinal() && val >= COOLING_1.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and cooling and cmd and stage"+newStageNum).isEmpty()) {
                    int bacnetId =  BacnetUtilKt.getBacnetId(val);
                    newCmdPoint =
                        new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("stage" + newStageNum).addMarker(
                        "his").addMarker("runtime").setTz(timeZone).setBacnetId(bacnetId).setBacnetType(BacnetUtilKt.BINARY_VALUE).build();
                }
            } else if (val >= Stage.HEATING_1.ordinal() && val <= HEATING_5.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and heating and cmd and stage"+newStageNum).isEmpty()) {
                    int bacnetId =  BacnetUtilKt.getBacnetId(val);
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("heating").addMarker("stage" + newStageNum).addMarker(
                        "his").addMarker("runtime").setTz(timeZone).setBacnetId(bacnetId).setBacnetType(BacnetUtilKt.BINARY_VALUE).build();
                }
            } else if (val >= Stage.FAN_1.ordinal() && val <= Stage.FAN_5.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and fan and cmd and stage"+newStageNum).isEmpty()) {
                    int bacnetId =  BacnetUtilKt.getBacnetId(val);
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("fan").addMarker("stage" + newStageNum).addMarker(
                        "his").addMarker("runtime").setTz(timeZone).setBacnetId(bacnetId).setBacnetType(BacnetUtilKt.BINARY_VALUE).build();
                }
            } else if (val == HUMIDIFIER.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and cmd and humidifier").isEmpty()) {
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("humidifier").addMarker("his").setEnums("off,on").setTz(
                        timeZone).setBacnetId(BacnetIdKt.HUMIDIFIERENABLEDID).setBacnetType(BacnetUtilKt.BINARY_VALUE)
                            .build();
                }
            } else if (val == DEHUMIDIFIER.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and cmd and dehumidifier").isEmpty()) {
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("dehumidifier").addMarker("his").setEnums("off,on").setTz(
                        timeZone).setBacnetId(BacnetIdKt.DEHUMIDIFIERENABLEDID).setBacnetType(BacnetUtilKt.BINARY_VALUE).build();
                }
            }
            if(oldPoint != null && oldPoint.getId() != null) {
                CCUHsApi.getInstance().deleteEntity(oldPoint.getId());
            }
            if(newCmdPoint != null){
                String newCmdPointId = CCUHsApi.getInstance().addPoint(newCmdPoint);
                CCUHsApi.getInstance().writeHisValById(newCmdPointId, 0.0);
            }
            CCUHsApi.getInstance().scheduleSync();
        }
        setSystemModeForVav(CCUHsApi.getInstance()); //TODO -
    }

    @Override
    public void deleteSystemEquip() {

        ArrayList<HashMap<Object, Object>> listOfEquips = CCUHsApi.getInstance().readAllEntities(CommonQueries.SYSTEM_PROFILE);
        for (HashMap<Object, Object> equip : listOfEquips) {
            if(equip.get("profile") != null){
                if(ProfileType.getProfileTypeForName(equip.get("profile").toString()) != null){
                    if (ProfileType.getProfileTypeForName(equip.get("profile").toString()).name().equals(ProfileType.SYSTEM_VAV_STAGED_RTU.name())) {
                        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "VavStagedRtu removing profile with it id-->"+equip.get("id").toString());
                        CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
                    }
                }
            }
        }

        removeSystemEquipModbus();
        removeSystemEquipBacnet();
        deleteSystemConnectModule();
    }

    public void addCmdPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        addCmdPoint(COOLING_1.displayName,"cooling","stage1", equipDis, siteRef, equipref, tz,
                BacnetIdKt.COOLINGSTAGE1ID, BacnetUtilKt.BINARY_VALUE);
        addCmdPoint(COOLING_2.displayName,"cooling","stage2", equipDis, siteRef, equipref, tz,
                BacnetIdKt.COOLINGSTAGE2ID, BacnetUtilKt.BINARY_VALUE);
        addCmdPoint(FAN_1.displayName,"fan","stage1", equipDis, siteRef, equipref, tz,
                BacnetIdKt.FANSTAGE1ID, BacnetUtilKt.BINARY_VALUE);
        addCmdPoint(HEATING_1.displayName,"heating","stage1", equipDis, siteRef, equipref, tz,
                BacnetIdKt.HEATINGSTAGE1ID, BacnetUtilKt.BINARY_VALUE);
        addCmdPoint(HEATING_2.displayName,"heating","stage2", equipDis, siteRef, equipref, tz,
                BacnetIdKt.HEATINGSTAGE2ID, BacnetUtilKt.BINARY_VALUE);
        addCmdPoint(FAN_2.displayName,"fan","stage2", equipDis, siteRef, equipref, tz,
                BacnetIdKt.FANSTAGE2ID, BacnetUtilKt.BINARY_VALUE);
        addHumidityCmdPoint(HUMIDIFIER.displayName,"humidifier", equipDis, siteRef, equipref, tz);
    }

    private void addCmdPoint(String name, String relayMap, String stage, String equipDis, String siteRef, String equipref, String tz,
                             int bacnetId, String bacnetType){
        //Name to be updated
        Point relay1Op = new Point.Builder()
                .setDisplayName(equipDis+"-"+name)
                .setSiteRef(siteRef)
                .setEquipRef(equipref).setHisInterpolate("cov")
                .addMarker("system").addMarker("cmd").addMarker(relayMap).addMarker(stage).addMarker("his").addMarker("runtime")
                .setTz(tz).setBacnetId(bacnetId).setBacnetType(bacnetType)
                .build();
        String cmdPointId = CCUHsApi.getInstance().addPoint(relay1Op);
        CCUHsApi.getInstance().writeHisValById(cmdPointId,0.0);
    }
    private void addHumidityCmdPoint(String name, String relayMap, String equipDis, String siteRef, String equipref, String tz){
        //Name to be updated
        Point relay1Op = new Point.Builder()
                .setDisplayName(equipDis+"-"+name)
                .setSiteRef(siteRef)
                .setEquipRef(equipref).setHisInterpolate("cov")
                .addMarker("system").addMarker("cmd").addMarker(relayMap).addMarker("his").addMarker("runtime")
                .setEnums("off,on")
                .setTz(tz).setBacnetId(BacnetIdKt.HUMIDIFIERENABLEDID).setBacnetType(BacnetUtilKt.BINARY_VALUE)
                .build();
        String cmdPointId = CCUHsApi.getInstance().addPoint(relay1Op);
        CCUHsApi.getInstance().writeHisValById(cmdPointId,0.0);
    }
    public double getCmdSignal(String cmd) {
        try {
            return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and his and " + cmd);
        }catch (Exception e){
            return 0;
        }
    }
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and his and "+cmd, val);
    }

    public void addConfigPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        addConfigPointEnabled("relay1", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay2", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay3", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay4", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay5", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay6", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay7", equipDis, siteRef, equipref, tz);
        addConfigPointAssociation("relay1", equipDis, siteRef, equipref, tz, Stage.COOLING_1);
        addConfigPointAssociation("relay2", equipDis, siteRef, equipref, tz, COOLING_2);
        addConfigPointAssociation("relay3", equipDis, siteRef, equipref, tz, FAN_1);
        addConfigPointAssociation("relay4", equipDis, siteRef, equipref, tz, HEATING_1);
        addConfigPointAssociation("relay5", equipDis, siteRef, equipref, tz, HEATING_2);
        addConfigPointAssociation("relay6", equipDis, siteRef, equipref, tz, FAN_2);
        addConfigPointAssociation("relay7", equipDis, siteRef, equipref, tz, Stage.HUMIDIFIER);

    }

    private void addConfigPointEnabled(String relay, String equipDis, String siteRef, String equipref, String tz) {
        Point relayEnabled = new Point.Builder()
                .setDisplayName(equipDis+"-"+relay+"OutputEnabled")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("system").addMarker("config").addMarker(relay)
                .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                .setEnums("false,true").setTz(tz)
                .build();
        String relayEnabledId = CCUHsApi.getInstance().addPoint(relayEnabled);
        CCUHsApi.getInstance().writeDefaultValById(relayEnabledId, 0.0 );
    }

    private void addConfigPointAssociation(String relay, String equipDis, String siteRef, String equipref, String tz, Stage init) {
        Point relayEnabled = new Point.Builder()
                .setDisplayName(equipDis+"-"+relay+"OutputAssociation")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("system").addMarker("config").addMarker(relay)
                .addMarker("output").addMarker("association").addMarker("writable").addMarker("sp")
                .setTz(tz)
                .build();
        String relayEnabledId = CCUHsApi.getInstance().addPoint(relayEnabled);
        CCUHsApi.getInstance().writeDefaultValById(relayEnabledId, (double)init.ordinal() );
    }
    public void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
    }
}
