package a75f.io.logic.bo.building.system.vav;

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

import android.content.Intent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.equips.VavStagedSystemEquip;
import a75f.io.domain.equips.VavStagedVfdSystemEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

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
    
    public int stageUpTimerCounter = 0;
    public int stageDownTimerCounter = 0;
    public boolean changeOverStageDownTimerOverrideActive = false;
    SystemController.State currentConditioning = OFF;
    
    int[] stageStatus = new int[17];
    
    public VavStagedSystemEquip systemEquip;
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
    
    @Override
    public void doSystemControl() {
        if (trSystem != null) {
            trSystem.processResetResponse();
        }
        VavSystemController.getInstance().runVavSystemControlAlgo();
        updateSystemPoints();
        setTrTargetVals();
    }
    
    @Override
    public void addSystemEquip() {
        systemEquip = (VavStagedSystemEquip) Domain.systemEquip;
        initTRSystem();
        updateStagesSelected();
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return (coolingStages > 0);
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0);
    }
    
    @Override
    public boolean isCoolingActive(){
        return stageStatus[COOLING_1.ordinal()] > 0 || stageStatus[COOLING_2.ordinal()] > 0 || stageStatus[COOLING_3.ordinal()] > 0
                                || stageStatus[COOLING_4.ordinal()] > 0 || stageStatus[COOLING_5.ordinal()] > 0;
    }
    
    @Override
    public boolean isHeatingActive(){
        return stageStatus[HEATING_1.ordinal()] > 0 || stageStatus[HEATING_2.ordinal()] > 0 || stageStatus[HEATING_3.ordinal()] > 0
               || stageStatus[HEATING_4.ordinal()] > 0 || stageStatus[HEATING_5.ordinal()] > 0;
    }
    
    protected synchronized void updateSystemPoints() {

        updateStagesSelected();
        updateOutsideWeatherParams();
        updateMechanicalConditioning(CCUHsApi.getInstance());

        SystemMode systemMode = SystemMode.values()[(int)systemEquip.getConditioningMode().readPriorityVal()];
        stageStatus = new int[17];
    
        if (currentConditioning == OFF) {
            currentConditioning = getSystemController().getSystemState();
            changeOverStageDownTimerOverrideActive = false;
        } else if (currentConditioning != getSystemController().getSystemState()) {
            currentConditioning = getSystemController().getSystemState();
            changeOverStageDownTimerOverrideActive = true;
        } else {
            changeOverStageDownTimerOverrideActive = false;
        }
    
        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemCoolingLoopOp = VavSystemController.getInstance().getCoolingSignal();
        } else if ((VavSystemController.getInstance().getSystemState() == COOLING)
                    && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
            double satSpMax = systemEquip.getSatSPMax().readPriorityVal();
            double satSpMin = systemEquip.getSatSPMin().readPriorityVal();
    
            CcuLog.d(L.TAG_CCU_SYSTEM,"satSpMax :"+satSpMax+" satSpMin: "+satSpMin+" SAT: "+getSystemSAT());
            systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;
        } else {
            systemCoolingLoopOp = 0;
        }

        if (VavSystemController.getInstance().getSystemState() == HEATING) {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }


        double analogFanSpeedMultiplier = systemEquip.getVavAnalogFanSpeedMultiplier().readPriorityVal();
        double epidemicMode = systemEquip.getEpidemicModeSystemState().readHisVal();
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
    
        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemFanLoopOp = getSingleZoneFanLoopOp(analogFanSpeedMultiplier);
        } else if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE ) && (L.ccu().oaoProfile != null)) {
            //TODO- Part OAO. Will be replaced with domanName later.
            double smartPurgeDabFanLoopOp = TunerUtil.readTunerValByQuery("system and purge and vav and fan and loop and output", L.ccu().oaoProfile.getEquipRef());
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
            systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
            
        } else if (VavSystemController.getInstance().getSystemState() == HEATING) {
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
        systemFanLoopOp = Math.min(systemFanLoopOp, 100);


        systemCo2LoopOp = VavSystemController.getInstance().getSystemState() == SystemController.State.OFF
                                  ? 0 : (SystemConstants.CO2_CONFIG_MAX - getSystemCO2()) * 100 / 200 ;

        systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);
        systemCoolingLoopOp = systemEquip.getCoolingLoopOutput().readHisVal();
        systemEquip.getHeatingLoopOutput().writePointValue(systemHeatingLoopOp);
        systemHeatingLoopOp = systemEquip.getHeatingLoopOutput().readHisVal();
        systemEquip.getFanLoopOutput().writePointValue(systemFanLoopOp);
        systemFanLoopOp = systemEquip.getFanLoopOutput().readHisVal();
        systemEquip.getCo2LoopOutput().writePointValue(systemCo2LoopOp);
        systemCo2LoopOp = systemEquip.getCo2LoopOutput().readHisVal();

        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp "+systemCoolingLoopOp+
                                   " systemHeatingLoopOp "+ systemHeatingLoopOp+" " + "systemFanLoopOp "+systemFanLoopOp);
    
        updateRelayStatus(epidemicState);
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "stageUpTimerCounter "+stageUpTimerCounter+
                                   " stageDownTimerCounter "+ stageDownTimerCounter+" " +
                                   "changeOverStageDownTimerOverrideActive "+changeOverStageDownTimerOverrideActive);
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "Relays Status: " + Arrays.toString(stageStatus));

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

    /**
     * Each of the 7 relays could be mapped to any of the 17 logical stages ( Cooling1-5, Heating1-5,Fan1-5,
     * Humidifier, dehumidifier.
     * Only one logical point is maintained even if more than one stage is mapped to multiple relays.
     * We first determine status of logical stages here and then change the physical relay state based on that.
     * @param epidemicState
     */
    private void updateRelayStatus(EpidemicState epidemicState) {
        
        double relayDeactHysteresis = systemEquip.getVavRelayDeactivationHysteresis().readPriorityVal();
        SystemMode systemMode = SystemMode.values()[(int)systemEquip.getConditioningMode().readPriorityVal()];
        
        int[] tempStatus = new int[17];
        
        if (stageUpTimerCounter > 0) {
            stageUpTimerCounter--;
        }
        if (stageDownTimerCounter > 0) {
            stageDownTimerCounter--;
        }
        getRelayAssiciationMap().forEach( (relay, association) -> {
            Stage stage = Stage.values()[(int) association.readDefaultVal()];
            int stageStatus = (int)getNewRelayState(relay, epidemicState, relayDeactHysteresis,
                    systemMode, stage);
            CcuLog.d(L.TAG_CCU_SYSTEM, "New relayState stage "+stage+" "+stageStatus+" "+relay.getDomainName());
            tempStatus[stage.ordinal()] = tempStatus[stage.ordinal()] | stageStatus;
        });

        //Handle stage down transitions
        for (int stageIndex = HEATING_5.ordinal(); stageIndex >= COOLING_1.ordinal(); stageIndex-- ) {
            Stage stage = Stage.values()[stageIndex];
            Set<a75f.io.domain.api.Point> relaySet = getRelayMappingForStage(stage);
            CcuLog.d(L.TAG_CCU_SYSTEM, "Relays mapped to stage "+stage+" "+Arrays.toString(relaySet.toArray()));
            for (a75f.io.domain.api.Point relay : relaySet) {
                //double curRelayState = ControlMote.getRelayState(relay);
                double curRelayState = getLogicalPhysicalMap().get(relay).readPointValue();
                stageStatus[stage.ordinal()] = (int) curRelayState;
                if (stageUpTimerCounter == 0 && stageDownTimerCounter == 0) {
                    double relayState = tempStatus[stage.ordinal()];
                    if (curRelayState > 0 && relayState == 0) {
                        if (!changeOverStageDownTimerOverrideActive) {
                            stageDownTimerCounter = (int) getStageDownTimeMinutes();
                        }
                        stageStatus[stage.ordinal()] = (int) relayState;
                        setStageStatus(stage, relayState);
                        CcuLog.d(L.TAG_CCU_SYSTEM, "Stage Down : "+stage);
                    }
                }
            }
            //There are no mapped & enabled relays for this stage. Deactivate it if currently active.
            if (relaySet.isEmpty()) {
                Double stageState = getStageStatus(stage);
                if (stageState.intValue() > 0) {
                    setStageStatus(stage, 0);
                }
            }
        }
        
        //Handle stage up transitions
        for (int stageIndex = COOLING_1.ordinal(); stageIndex <= HEATING_5.ordinal(); stageIndex++ ) {
            Stage stage = Stage.values()[stageIndex];
            Set<a75f.io.domain.api.Point> relaySet = getRelayMappingForStage(stage);
            CcuLog.d(L.TAG_CCU_SYSTEM, "Relays mapped to stage "+stage+" "+Arrays.toString(relaySet.toArray()));
            for (a75f.io.domain.api.Point relay : relaySet) {
                double curRelayState = getLogicalPhysicalMap().get(relay).readPointValue();
                if (stageUpTimerCounter == 0 && stageDownTimerCounter == 0) {
                    double relayState = tempStatus[stage.ordinal()];
                    if (curRelayState == 0 && relayState > 0) {
                        stageUpTimerCounter = (int) getStageUpTimeMinutes();
                        stageStatus[stage.ordinal()] = (int) relayState;
                        setStageStatus(stage, relayState);
                        CcuLog.d(L.TAG_CCU_SYSTEM, "Stage Up "+stage);
                    }
                }
            }
            //There are no mapped & enabled relays for this stage. Deactivate it if currently active.
            if (relaySet.isEmpty()) {
                Double stageState = getStageStatus(stage);
                if (stageState.intValue() > 0) {
                    setStageStatus(stage, 0);
                }
            }
        }
    
        //Stage down timer might delay stage-turn off. Make sure the fan is ON during that time
        // even if the loopOp is 0
        if (stageStatus[COOLING_1.ordinal()] > 0 || stageStatus[HEATING_1.ordinal()] > 0) {
            int fan1Status = isStageEnabled(FAN_1) ? 1 : 0;
            tempStatus[FAN_1.ordinal()] = fan1Status;
            int fan2Status = isStageEnabled(FAN_2) ? 1 : 0;
            tempStatus[FAN_2.ordinal()] = fan2Status;
        }
        
        for (int stageIndex = FAN_1.ordinal(); stageIndex <= DEHUMIDIFIER.ordinal(); stageIndex++) {
            stageStatus[stageIndex] = tempStatus[stageIndex];
            Stage stage = Stage.values()[stageIndex];
            setStageStatus(stage, tempStatus[stage.ordinal()]);
        }
        
        updateRelays();
    }

    protected a75f.io.domain.api.Point getDomainPointForStage(Stage stage) {
        switch (stage) {
            case COOLING_1:
                return systemEquip.getCoolingStage1();
            case COOLING_2:
                return systemEquip.getCoolingStage2();
            case COOLING_3:
                return systemEquip.getCoolingStage3();
            case COOLING_4:
                return systemEquip.getCoolingStage4();
            case COOLING_5:
                return systemEquip.getCoolingStage5();
            case HEATING_1:
                return systemEquip.getHeatingStage1();
            case HEATING_2:
                return systemEquip.getHeatingStage2();
            case HEATING_3:
                return systemEquip.getHeatingStage3();
            case HEATING_4:
                return systemEquip.getHeatingStage4();
            case HEATING_5:
                return systemEquip.getHeatingStage5();
            case FAN_1:
                return systemEquip.getFanStage1();
            case FAN_2:
                return systemEquip.getFanStage2();
            case FAN_3:
                return systemEquip.getFanStage3();
            case FAN_4:
                return systemEquip.getFanStage4();
            case FAN_5:
                return systemEquip.getFanStage5();
            case HUMIDIFIER:
                return systemEquip.getHumidifierEnable();
            case DEHUMIDIFIER:
                return systemEquip.getDehumidifierEnable();
        }
        return null;
    }
    
    private void setStageStatus(Stage stage, double relayState) {
        a75f.io.domain.api.Point point = getDomainPointForStage(stage);
        if(point.isWritable()){
            point.writePointValue(relayState);
        }else{
            point.writeHisVal(relayState);
        }
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

    
    public double getNewRelayState(a75f.io.domain.api.Point relayPoint, EpidemicState epidemicState, double relayDeactHysteresis,
                                   SystemMode systemMode, Stage stage) {
    
        double relayState = 0;
        double currState = getDomainPointForStage(stage).readHisVal();;
        double stageThreshold = 0;
        
        if (relayPoint.readDefaultVal() == 0) {
            relayState = 0;
        } else {
            switch (stage) {
                case COOLING_1:
                case COOLING_2:
                case COOLING_3:
                case COOLING_4:
                case COOLING_5:
                    if (isCoolingLockoutActive()) {
                        relayState = 0;
                    } else {
                        if (L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
                            stageThreshold = 100 * (stage.ordinal() + 1) / (coolingStages + 1);
                        } else {
                            stageThreshold = 100 * stage.ordinal() / coolingStages;
                        }
                        if (currState == 0) {
                            relayState = systemCoolingLoopOp > stageThreshold ? 1 : 0;
                        } else {
                            relayState = systemCoolingLoopOp > Math.max(stageThreshold - relayDeactHysteresis, 0) ? 1
                                             : 0;
                        }
                    }
                    break;
                case HEATING_1:
                case HEATING_2:
                case HEATING_3:
                case HEATING_4:
                case HEATING_5:
                    if (isHeatingLockoutActive()) {
                        relayState = 0;
                    } else {
                        stageThreshold = 100 * (stage.ordinal() - HEATING_1.ordinal()) / heatingStages;
                        if (currState == 0) {
                            relayState = systemHeatingLoopOp > stageThreshold ? 1 : 0;
                        } else {
                            relayState = systemHeatingLoopOp > Math.max(stageThreshold - relayDeactHysteresis, 0) ? 1 : 0;
                        }
                    }
                    break;
                case FAN_1:
                    if ((systemMode != SystemMode.OFF && (isSystemOccupied() || isReheatActive(CCUHsApi.getInstance())))
                            || ((L.ccu().systemProfile.getProfileType() != ProfileType.SYSTEM_VAV_STAGED_VFD_RTU)
                                    && (systemFanLoopOp > 0))) {
                        relayState = 1;
                    }else if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU) {
                        if(epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE)
                            relayState = systemFanLoopOp > 0 ? 1 : 0;
                        else
                            relayState =  (systemCoolingLoopOp > 0 || systemHeatingLoopOp > 0 || systemFanLoopOp > 0) ? 1 :0;
                    } else {
                        relayState = 0;
                    }
                    break;
                case FAN_2:
                    if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU) {
                        relayState =  (systemCoolingLoopOp > 0 || systemHeatingLoopOp > 0 || systemFanLoopOp > 0) ? 1 :0;
                    } else {
                        relayState = systemFanLoopOp > 0 ? 1 : 0;
                    }
                    break;
                case FAN_3:
                case FAN_4:
                case FAN_5:
                    stageThreshold = 100 * (stage.ordinal() - FAN_2.ordinal()) / (fanStages - 1);
                    if (currState == 0) {
                        relayState = systemFanLoopOp >= stageThreshold ? 1: 0;
                    } else {
                        relayState = systemFanLoopOp > (stageThreshold - relayDeactHysteresis) ? 1 : 0;
                    }
                    break;
                case HUMIDIFIER:
                case DEHUMIDIFIER:
                    if (systemMode == SystemMode.OFF ||
                        ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.UNOCCUPIED ||
                        ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.VACATION ||
                            ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.DEMAND_RESPONSE_UNOCCUPIED ||
                            ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.NONE) {
                        relayState = 0;
                    } else {
                        double humidity = VavSystemController.getInstance().getAverageSystemHumidity();
                        double targetMinHumidity = systemEquip.getSystemtargetMinInsideHumidity().readPriorityVal();
                        double targetMaxHumidity = systemEquip.getSystemtargetMaxInsideHumidity().readPriorityVal();
                        double humidityHysteresis = systemEquip.getVavHumidityHysteresis().readPriorityVal();
                        if (stage == HUMIDIFIER) {
                            currState = systemEquip.getHumidifierEnable().readHisVal();
                            //Humidification
                            if (humidity < targetMinHumidity) {
                                relayState = 1;
                            } else if (humidity > (targetMinHumidity + humidityHysteresis)) {
                                relayState = 0;
                            } else {
                                relayState = currState;
                            }
                        } else {
                            currState = systemEquip.getDehumidifierEnable().readHisVal();
                            //Dehumidification
                            if (humidity > targetMaxHumidity) {
                                relayState = 1;
                            } else if (humidity < (targetMaxHumidity - humidityHysteresis)) {
                                relayState = 0;
                            } else {
                                relayState = currState;
                            }
                        }
                        CcuLog.d(L.TAG_CCU_SYSTEM, "humidity :" + humidity + " targetMinHumidity: " + targetMinHumidity + " humidityHysteresis: " + humidityHysteresis + " targetMaxHumidity: " + targetMaxHumidity);
                    }
                    break;
            }
        }
        return relayState;
    }
    
    private double getStageUpTimeMinutes() {
        return systemEquip.getVavStageUpTimerCounter().readPriorityVal();
    }
    
    private double getStageDownTimeMinutes() {
        return systemEquip.getVavStageDownTimerCounter().readPriorityVal();
    }
    
    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        
        status.append((stageStatus[FAN_1.ordinal()] > 0) ? "1":"");
        status.append((stageStatus[FAN_2.ordinal()] > 0) ? ",2":"");
        status.append((stageStatus[FAN_3.ordinal()] > 0) ? ",3":"");
        status.append((stageStatus[FAN_4.ordinal()] > 0) ? ",4":"");
        status.append((stageStatus[FAN_5.ordinal()] > 0) ? ",5":"");
        
        if (!status.toString().equals("")) {
            status.insert(0, "Fan Stage ");
            status.append(" ON ");
        }
        if (isCoolingActive()) {
            status.append("| Cooling Stage " + ((stageStatus[COOLING_1.ordinal()] > 0) ? "1" : ""));
            status.append((stageStatus[COOLING_2.ordinal()] > 0) ? ",2" : "");
            status.append((stageStatus[COOLING_3.ordinal()] > 0) ? ",3" : "");
            status.append((stageStatus[COOLING_4.ordinal()] > 0) ? ",4" : "");
            status.append((stageStatus[COOLING_5.ordinal()] > 0) ? ",5 ON " : " ON ");
        }
        
        if (isHeatingActive()) {
            status.append("| Heating Stage " + ((stageStatus[HEATING_1.ordinal()] > 0) ? "1" : ""));
            status.append((stageStatus[HEATING_2.ordinal()] > 0) ? ",2" : "");
            status.append((stageStatus[HEATING_3.ordinal()] > 0) ? ",3" : "");
            status.append((stageStatus[HEATING_4.ordinal()] > 0) ? ",4" : "");
            status.append((stageStatus[HEATING_5.ordinal()] > 0) ? ",5 ON" : " ON");
        }
        if (systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
            status.insert(0, "Free Cooling Used | ");
        }
        if (L.ccu().systemProfile instanceof VavStagedRtuWithVfd) {
            VavStagedVfdSystemEquip vfdSystemEquip = (VavStagedVfdSystemEquip) Domain.systemEquip;
            status.append(vfdSystemEquip.getFanSignal().readHisVal() > 0 ? " Analog Fan ON " : "");
        }
        String humidifierStatus = getRelayMappingForStage(HUMIDIFIER).isEmpty() ? "" :
                                        systemEquip.getHumidifierEnable().readHisVal() > 0 ? " | Humidifier ON " : " | Humidifier OFF ";
        String dehumidifierStatus = getRelayMappingForStage(DEHUMIDIFIER).isEmpty() ? "" :
                                        systemEquip.getDehumidifierEnable().readHisVal() > 0 ? " | Dehumidifier ON " : " | Dehumidifier OFF ";

        return status.toString().equals("") ? "System OFF" + humidifierStatus + dehumidifierStatus :
                status + humidifierStatus + dehumidifierStatus;
    }
    
    public void updateStagesSelected() {
        systemEquip = (VavStagedSystemEquip) Domain.systemEquip;
        coolingStages = 0;
        heatingStages = 0;
        fanStages = 0;
        getRelayAssiciationMap().forEach( (relay, association) -> {
            CcuLog.i(L.TAG_CCU_SYSTEM, relay.getDomainName()+" enabled "+relay.readDefaultVal()+" association "+association.readDefaultVal());
            if (relay.readDefaultVal() > 0) {
                int val = (int)association.readDefaultVal();
                if (val <= Stage.COOLING_5.ordinal() && val >= coolingStages) {
                    coolingStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Cooling stage : "+coolingStages);
                } else if (val >= Stage.HEATING_1.ordinal() && val <= HEATING_5.ordinal() && val >= heatingStages) {
                    heatingStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Heating stage : "+heatingStages);
                } else if (val >= Stage.FAN_1.ordinal() && val <= Stage.FAN_5.ordinal() && val >= fanStages) {
                    fanStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Fan stage : "+fanStages);
                }
            }
        });
        
        if ((heatingStages > 0)) {
            heatingStages -= Stage.HEATING_1.ordinal();
        }
        
        if (fanStages > 0) {
            fanStages -= Stage.FAN_1.ordinal();
        }
        
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
                //ControlMote.setRelayState(relay, newState);
                getLogicalPhysicalMap().get(relay).writePointValue(newState);
            }
        });
    }
    protected Map<a75f.io.domain.api.Point, a75f.io.domain.api.Point> getRelayAssiciationMap() {
        Map<a75f.io.domain.api.Point, a75f.io.domain.api.Point> associations = new HashMap<>();
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
                                    .setHashMap(CCUHsApi.getInstance().read("system and equip and not modbus and not connectModule")).build();
    
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
        HashMap equip = CCUHsApi.getInstance().read("system and equip and not modbus and not connectModule");
        if (ProfileType.getProfileTypeForName(equip.get("profile").toString()).name().equals(ProfileType.SYSTEM_VAV_STAGED_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
        removeSystemEquipModbus();
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
