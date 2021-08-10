package a75f.io.logic.bo.building.system.vav;

import android.content.Intent;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

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
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;
import static a75f.io.logic.jobs.ScheduleProcessJob.ACTION_STATUS_CHANGE;

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
    
    private int stageUpTimerCounter = 0;
    private int stageDownTimerCounter = 0;
    private boolean changeOverStageDownTimerOverrideActive = false;
    SystemController.State currentConditioning = OFF;
    
    int[] stageStatus = new int[17];
    
    
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
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_STAGED_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                initTRSystem();
                addNewSystemUserIntentPoints(equip.get("id").toString());
                addNewTunerPoints(equip.get("id").toString());
                updateStagesSelected();
                return;
            }
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"System Equip does not exist. Create Now");
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        Equip systemEquip= new Equip.Builder()
                                   .setSiteRef(siteRef)
                                   .setDisplayName(siteDis+"-SystemEquip")
                                   .setProfile(ProfileType.SYSTEM_VAV_STAGED_RTU.name())
                                   .addMarker("equip").addMarker("system").addMarker("vav")
                                   .setTz(siteMap.get("tz").toString())
                                   .build();
        String equipRef = hayStack.addEquip(systemEquip);
        addSystemLoopOpPoints(equipRef);
        addUserIntentPoints(equipRef);
        addCmdPoints(equipRef);
        addConfigPoints(equipRef);
        addTunerPoints(equipRef);
        addVavSystemTuners(equipRef);
        updateAhuRef(equipRef);
        //sysEquip = new SystemEquip(equipRef);
        new ControlMote(equipRef);
        initTRSystem();
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
        
        
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
        updateOutsideWeatherParams();
        SystemMode systemMode = SystemMode.values()[(int)getUserIntentVal("conditioning and mode")];
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
    
        if ((VavSystemController.getInstance().getSystemState() == COOLING)
                    && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
            double satSpMax = VavTRTuners.getSatTRTunerVal("spmax");
            double satSpMin = VavTRTuners.getSatTRTunerVal("spmin");
    
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
        
        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", getSystemEquipRef());
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        
        if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE ) && (L.ccu().oaoProfile != null)) {
            double smartPurgeDabFanLoopOp = TunerUtil.readTunerValByQuery("system and purge and vav and fan and loop and output", L.ccu().oaoProfile.getEquipRef());
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");

            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure()+","+smartPurgeDabFanLoopOp);
            double staticPressureLoopOutput = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
            if((VavSystemController.getInstance().getSystemState() == COOLING)
                                        && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
                if(staticPressureLoopOutput < ((spSpMax - spSpMin) * smartPurgeDabFanLoopOp))
                    systemFanLoopOp = ((spSpMax - spSpMin) * smartPurgeDabFanLoopOp);
                else
                    systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
            } else if(VavSystemController.getInstance().getSystemState() == HEATING) {
                systemFanLoopOp = Math.max((int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier), smartPurgeDabFanLoopOp);
            } else {
                systemFanLoopOp = smartPurgeDabFanLoopOp;
            }

        } else if (VavSystemController.getInstance().getSystemState() == COOLING
                                            && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");
    
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
        
        setSystemLoopOp("cooling", systemCoolingLoopOp);
        setSystemLoopOp("heating", systemHeatingLoopOp);
        setSystemLoopOp("fan", systemFanLoopOp);
        setSystemLoopOp("co2", systemCo2LoopOp);
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp "+systemCoolingLoopOp+
                                   " systemHeatingLoopOp "+ systemHeatingLoopOp+" " + "systemFanLoopOp "+systemFanLoopOp);
        
        updateStagesSelected();
    
        updateRelayStatus(epidemicState);
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "stageUpTimerCounter "+stageUpTimerCounter+
                                   " stageDownTimerCounter "+ stageDownTimerCounter+" " +
                                   "changeOverStageDownTimerOverrideActive "+changeOverStageDownTimerOverrideActive);
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "Relays Status: " + Arrays.toString(stageStatus));
    
        setSystemPoint("operating and mode", VavSystemController.getInstance().systemState.ordinal());
        String systemStatus = getStatusMessage();
        String scheduleStatus =  ScheduleProcessJob.getSystemStatusString();
        CcuLog.d(L.TAG_CCU_SYSTEM, "StatusMessage: "+systemStatus);
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: " +scheduleStatus);
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and status and message").equals(systemStatus)) {
            CCUHsApi.getInstance().writeDefaultVal("system and status and message", systemStatus);
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and scheduleStatus").equals(scheduleStatus)) {
            CCUHsApi.getInstance().writeDefaultVal("system and scheduleStatus", scheduleStatus);
        }
    
    }
    
    private void updateRelayStatus(EpidemicState epidemicState) {
        
        double relayDeactHysteresis = TunerUtil.readTunerValByQuery("relay and deactivation and hysteresis", getSystemEquipRef());
        SystemMode systemMode = SystemMode.values()[(int)getUserIntentVal("conditioning and mode")];
        
        int[] tempStatus = new int[17];
        
        if (stageUpTimerCounter > 0) {
            stageUpTimerCounter--;
        }
        if (stageDownTimerCounter > 0) {
            stageDownTimerCounter--;
        }
        
        for (int relayCount = 1; relayCount <= 7; relayCount++) {
            Stage stage = Stage.values()[(int) getConfigAssociation("relay" + relayCount)];
            tempStatus[stage.ordinal()] = (int)getNewRelayState(relayCount, epidemicState, relayDeactHysteresis,
                                                                systemMode, stage);;
        }
        
        //Handle stage down transitions
        for (int stageIndex = HEATING_5.ordinal(); stageIndex >= COOLING_1.ordinal(); stageIndex-- ) {
            Stage stage = Stage.values()[stageIndex];
            HashSet<Integer> relaySet = getRelayMappingForStage(stage);
            CcuLog.d(L.TAG_CCU_SYSTEM, "Relays mapped to stage "+stage+" "+relaySet.toString());
            for (Integer relay : relaySet) {
                double curRelayState = ControlMote.getRelayState("relay" + relay);
                stageStatus[stage.ordinal()] = (int) curRelayState;
                if (stageUpTimerCounter == 0 && stageDownTimerCounter == 0) {
                    double relayState = tempStatus[stage.ordinal()];
                    if (curRelayState > 0 && relayState == 0) {
                        if (!changeOverStageDownTimerOverrideActive) {
                            stageDownTimerCounter = (int) getStageDownTimeMinutes();
                        }
                        stageStatus[stage.ordinal()] = (int) relayState;
                        setStageStatus(stage, relayState);
                        ControlMote.setRelayState("relay" + relay, relayState);
                        CcuLog.d(L.TAG_CCU_SYSTEM, "Stage Down : "+stage);
                    }
                }
            }
        }
        
        //Handle stage up transitions
        for (int stageIndex = COOLING_1.ordinal(); stageIndex <= HEATING_5.ordinal(); stageIndex++ ) {
            Stage stage = Stage.values()[stageIndex];
            HashSet<Integer> relaySet = getRelayMappingForStage(stage);
            CcuLog.d(L.TAG_CCU_SYSTEM, "Relays mapped to stage "+stage+" "+relaySet.toString());
            for (Integer relay : relaySet) {
                if (stageUpTimerCounter == 0 && stageDownTimerCounter == 0) {
                    double relayState = tempStatus[stage.ordinal()];
                    if (stageStatus[stage.ordinal()] == 0 && relayState > 0) {
                        stageUpTimerCounter = (int) getStageUpTimeMinutes();
                        stageStatus[stage.ordinal()] = (int) relayState;
                        setStageStatus(stage, relayState);
                        ControlMote.setRelayState("relay" + relay, relayState);
                        CcuLog.d(L.TAG_CCU_SYSTEM, "Stage Up "+stage);
                    }
                }
            }
        }
    
        //Stage down timer might delay stage-turn off. Make sure the fan is ON during that time
        // even if the loopOp is 0
        if (stageStatus[COOLING_1.ordinal()] > 0 || stageStatus[HEATING_1.ordinal()] > 0) {
            int fanStatus = isStageEnabled(FAN_1) ? 1 : 0;
            tempStatus[FAN_1.ordinal()] = fanStatus;
        }
        
        for (int stageIndex = FAN_1.ordinal(); stageIndex < DEHUMIDIFIER.ordinal(); stageIndex++) {
            stageStatus[stageIndex] = tempStatus[stageIndex];
            HashSet<Integer> relaySet = getRelayMappingForStage(Stage.values()[stageIndex]);
            for (Integer relay : relaySet) {
                ControlMote.setRelayState("relay" + relay, stageStatus[stageIndex]);
            }
            Stage stage = Stage.values()[stageIndex];
            setStageStatus(stage, tempStatus[stage.ordinal()]);
        }
    }
    
    
    private void setStageStatus(Stage stage, double relayState) {
        if (stage.getValue() <= COOLING_5.getValue()) {
            double currState = getCmdSignal("cooling and stage" + (stage.ordinal() + 1));
            if (currState != relayState) {
                setCmdSignal("cooling and stage" + (stage.ordinal() + 1), relayState);
            }
        } else if (stage.getValue() >= HEATING_1.getValue() && stage.getValue() <= HEATING_5.getValue()) {
            double currState = getCmdSignal("heating and stage" + (stage.ordinal() - COOLING_5.ordinal()));
            if (currState != relayState) {
                setCmdSignal("heating and stage" + (stage.ordinal() - COOLING_5.ordinal()), relayState);
            }
        } else if (stage.getValue() >= FAN_1.getValue() && stage.getValue() <= FAN_5.getValue()) {
            double currState = getCmdSignal("fan and stage" + (stage.ordinal() - HEATING_5.ordinal()));
            if (currState != relayState) {
                setCmdSignal("fan and stage" + (stage.ordinal() - HEATING_5.ordinal()), relayState);
            }
        } else if (stage.getValue() == HUMIDIFIER.getValue()) {
            double currState = getCmdSignal("humidifier");
            if (currState != relayState) {
                setCmdSignal("humidifier", relayState);
            }
        }  else if (stage.getValue() == DEHUMIDIFIER.getValue()) {
            double currState = getCmdSignal("dehumidifier");
            if (currState != relayState) {
                setCmdSignal("dehumidifier", relayState);
            }
        }
    }
    
    private HashSet<Integer> getRelayMappingForStage(Stage stage) {
        HashSet<Integer> relaySet= new HashSet<>();
        for (int relayCount = 1; relayCount <= 7; relayCount++) {
            if (getConfigEnabled("relay" + relayCount) > 0 &&
                                stage.ordinal() == getConfigAssociation("relay" + relayCount)) {
                relaySet.add(relayCount);
            }
        }
        return relaySet;
    }
    
    public double getNewRelayState(int relayNum, EpidemicState epidemicState, double relayDeactHysteresis,
                                                                                    SystemMode systemMode, Stage stage) {
    
        double relayState = 0;
        double currState = 0;
        double stageThreshold = 0;
        
        if (getConfigEnabled("relay"+ relayNum) == 0) {
            relayState = 0;
        } else {
            switch (stage) {
                case COOLING_1:
                case COOLING_2:
                case COOLING_3:
                case COOLING_4:
                case COOLING_5:
                    currState = getCmdSignal("cooling and stage" + (stage.ordinal() + 1));
                    if (L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
                        stageThreshold = 100 * (stage.ordinal() +1) / (coolingStages + 1);
                    } else {
                        stageThreshold = 100 * stage.ordinal() / coolingStages;
                    }
                    if (currState == 0) {
                        relayState = systemCoolingLoopOp > stageThreshold ? 1 : 0;
                    } else {
                        relayState = systemCoolingLoopOp > Math.max(stageThreshold - relayDeactHysteresis ,0 ) ? 1 :0;
                    }
                    break;
                case HEATING_1:
                case HEATING_2:
                case HEATING_3:
                case HEATING_4:
                case HEATING_5:
                    currState = getCmdSignal("heating and stage" + (stage.ordinal() - COOLING_5.ordinal()));
                    stageThreshold = 100 * (stage.ordinal() - HEATING_1.ordinal()) / heatingStages;
                    if (currState == 0) {
                        relayState = systemHeatingLoopOp > stageThreshold ? 1 : 0;
                    } else {
                        relayState = systemHeatingLoopOp > Math.max(stageThreshold - relayDeactHysteresis, 0) ? 1: 0;
                    }
                    break;
                case FAN_1:
                    if ((systemMode != SystemMode.OFF && (ScheduleProcessJob.getSystemOccupancy() != Occupancy.UNOCCUPIED
                                                          && ScheduleProcessJob.getSystemOccupancy() != Occupancy.VACATION))
                                                          || ((L.ccu().systemProfile.getProfileType() != ProfileType.SYSTEM_VAV_STAGED_VFD_RTU)
                                                            && (systemFanLoopOp > 0))) {
                        relayState = 1;
                    }else if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU) {
                        if(epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE)
                            relayState = systemFanLoopOp > 0 ? 1 : 0;
                        else
                            relayState =  (systemCoolingLoopOp > 0 || systemHeatingLoopOp > 0) ? 1 :0;
                    } else {
                        relayState = 0;
                    }
                    break;
                case FAN_2:
                    if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU) {
                        relayState =  (systemCoolingLoopOp > 0 || systemHeatingLoopOp > 0) ? 1 :0;
                    } else {
                        relayState = systemFanLoopOp > 0 ? 1 : 0;
                    }
                    break;
                case FAN_3:
                case FAN_4:
                case FAN_5:
                    currState = getCmdSignal("fan and stage" + (stage.ordinal() - HEATING_5.ordinal()));
                    stageThreshold = 100 * (stage.ordinal() - FAN_2.ordinal()) / (fanStages - 1);
                    if (currState == 0) {
                        relayState = systemFanLoopOp >= stageThreshold ? 1: 0;
                    } else {
                        relayState = systemFanLoopOp > (stageThreshold - relayDeactHysteresis) ? 1 : 0;
                    }
                    break;
                case HUMIDIFIER:
                case DEHUMIDIFIER:
                    if (systemMode == SystemMode.OFF || ScheduleProcessJob.getSystemOccupancy() == Occupancy.UNOCCUPIED
                        || ScheduleProcessJob.getSystemOccupancy() == Occupancy.VACATION) {
                        relayState = 0;
                    } else {
                        double humidity = VavSystemController.getInstance().getAverageSystemHumidity();
                        double targetMinHumidity = TunerUtil.readSystemUserIntentVal("target and min and inside and humidity");
                        double targetMaxHumidity = TunerUtil.readSystemUserIntentVal("target and max and inside and humidity");
                        double humidityHysteresis = TunerUtil.readTunerValByQuery("humidity and hysteresis", getSystemEquipRef());
                        if (stage == HUMIDIFIER) {
                            currState = getCmdSignal("humidifier");
                            //Humidification
                            if (humidity < targetMinHumidity) {
                                relayState = 1;
                            } else if (humidity > (targetMinHumidity + humidityHysteresis)) {
                                relayState = 0;
                            } else {
                                relayState = currState;
                            }
                        } else {
                            currState = getCmdSignal("dehumidifier");
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
        return TunerUtil.readTunerValByQuery("vav and stageUp and timer and counter", getSystemEquipRef());
    }
    
    private double getStageDownTimeMinutes() {
        return TunerUtil.readTunerValByQuery("vav and stageDown and timer and counter", getSystemEquipRef());
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
        if (isCoolingActive())
        {
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
        if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU) {
            if (getConfigEnabled("analog2") > 0)
            {
                status.append(getCmdSignal("fan and modulating") > 0 ? " Analog Fan ON " : "");
            }
        }
        return status.toString().equals("")? "System OFF" : status.toString();
    }
    
    public void updateStagesSelected() {
       
        coolingStages = 0;
        heatingStages = 0;
        fanStages = 0;
        
        for (int i = 1; i < 8; i++)
        {
            if (getConfigEnabled("relay"+i) > 0)
            {
                int val = (int)getConfigAssociation("relay"+i);
                if (val <= Stage.COOLING_5.ordinal() && val >= coolingStages)
                {
                    coolingStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Cooling stage : "+coolingStages);
                } else if (val >= Stage.HEATING_1.ordinal() && val <= HEATING_5.ordinal() && val >= heatingStages)
                {
                    heatingStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Heating stage : "+heatingStages);
                } else if (val >= Stage.FAN_1.ordinal() && val <= Stage.FAN_5.ordinal() && val >= fanStages)
                {
                    fanStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Fan stage : "+fanStages);
                }
            }
        }
        
        if ((heatingStages > 0)) {
            heatingStages -= Stage.HEATING_1.ordinal();
        }
        
        if (fanStages > 0) {
            fanStages -= Stage.FAN_1.ordinal();
        }
        
    }
    
    public boolean isStageEnabled(Stage s) {
        for (int i = 1; i < 8; i++)
        {
            if (getConfigEnabled("relay" + i) > 0)
            {
                int val = (int) getConfigAssociation("relay" + i);
                if (val == s.ordinal())  {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean isStageMapped(Stage stage) {
        for (int relayCount = 1; relayCount <= 7; relayCount++) {
            if (stage.ordinal() == getConfigAssociation("relay" + relayCount)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_STAGED_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
    
    public void addCmdPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        addCmdPoint(COOLING_1.displayName,"cooling","stage1", equipDis, siteRef, equipref, tz);
        addCmdPoint(COOLING_2.displayName,"cooling","stage2", equipDis, siteRef, equipref, tz);
        addCmdPoint(FAN_1.displayName,"fan","stage1", equipDis, siteRef, equipref, tz);
        addCmdPoint(HEATING_1.displayName,"heating","stage1", equipDis, siteRef, equipref, tz);
        addCmdPoint(HEATING_2.displayName,"heating","stage2", equipDis, siteRef, equipref, tz);
        addCmdPoint(FAN_2.displayName,"fan","stage2", equipDis, siteRef, equipref, tz);
        addHumidityCmdPoint(HUMIDIFIER.displayName,"humidifier", equipDis, siteRef, equipref, tz);
    }
    
    private void addCmdPoint(String name, String relayMap, String stage, String equipDis, String siteRef, String equipref, String tz){
        //Name to be updated
        Point relay1Op = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+name)
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref).setHisInterpolate("cov")
                                 .addMarker("system").addMarker("cmd").addMarker(relayMap).addMarker(stage).addMarker("his").addMarker("runtime")
                                 .setTz(tz)
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
                .setTz(tz)
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
    
    public double getConfigEnabled(String config) {

        //return sysEquip.getConfigEnabled(config)? 1:0;
        //return CCUHsApi.getInstance().readDefaultVal("point and system and config and output and enabled and "+config);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+config);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());

    }
    public void setConfigEnabled(String config, double val) {
        //sysEquip.setConfigEnabled(config, val);
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and output and enabled and "+config, val);
    }
    
    public double getConfigAssociation(String config) {
        //return sysEquip.getRelayOpAssociation(config);
        //return CCUHsApi.getInstance().readDefaultVal("point and system and config and output and association and "+config);

        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and association and "+config);
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
                    if (!isStageMapped(curstage)) {
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
                    if (!isStageMapped(curstage)) {
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
                    if (!isStageMapped(curstage)) {
                        curStageNum = curstage.ordinal() - HEATING_5.ordinal();
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and fan and stage" + curStageNum);
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
                case HUMIDIFIER:
                    if (!isStageMapped(curstage)) {
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and humidifier");
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
                case DEHUMIDIFIER:
                    if (!isStageMapped(curstage)) {
                        cmd = CCUHsApi.getInstance().read("point and system and cmd and dehumidifier");
                        oldPoint = new Point.Builder().setHashMap(cmd).build();
                    }
                    break;
            }
    
            String timeZone = CCUHsApi.getInstance().getTimeZone();
    
            Equip systemEquip = new Equip.Builder()
                                    .setHashMap(CCUHsApi.getInstance().read("system and equip")).build();
    
            if (val <= Stage.COOLING_5.ordinal() && val >= COOLING_1.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and cooling and cmd and stage"+newStageNum).isEmpty()) {
                    newCmdPoint =
                        new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("stage" + newStageNum).addMarker(
                        "his").addMarker("runtime").setTz(timeZone).build();
                }
            } else if (val >= Stage.HEATING_1.ordinal() && val <= HEATING_5.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and heating and cmd and stage"+newStageNum).isEmpty()) {
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("heating").addMarker("stage" + newStageNum).addMarker(
                        "his").addMarker("runtime").setTz(timeZone).build();
                }
            } else if (val >= Stage.FAN_1.ordinal() && val <= Stage.FAN_5.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and fan and cmd and stage"+newStageNum).isEmpty()) {
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("fan").addMarker("stage" + newStageNum).addMarker(
                        "his").addMarker("runtime").setTz(timeZone).build();
                }
            } else if (val == HUMIDIFIER.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and cmd and humidifier").isEmpty()) {
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("humidifier").addMarker("his").setEnums("off,on").setTz(
                        timeZone).build();
                }
            } else if (val == DEHUMIDIFIER.ordinal()) {
                if (CCUHsApi.getInstance().read("point and system and cmd and dehumidifier").isEmpty()) {
                    newCmdPoint = new Point.Builder().setSiteRef(systemEquip.getSiteRef()).setEquipRef(systemEquip.getId()).setDisplayName(
                        equipDis + "-" + updatedStage.displayName).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("dehumidifier").addMarker("his").setEnums("off,on").setTz(
                        timeZone).build();
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
        
    }
    
    public void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
    }
    
}
