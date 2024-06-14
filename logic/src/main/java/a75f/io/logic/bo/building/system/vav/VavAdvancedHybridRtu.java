package a75f.io.logic.bo.building.system.vav;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
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
import a75f.io.logic.bo.haystack.device.ControlMote;
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

import android.content.Intent;

/**
 * Created by samjithsadasivan on 2/11/19.
 */

public class VavAdvancedHybridRtu extends VavStagedRtu
{
    private static final int ANALOG_SCALE = 10;
    
    @Override
    public String getProfileName()
    {
        return "VAV Advanced Hybrid AHU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_HYBRID_RTU;
    }
    
    @Override
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system and not modbus and not connectModule");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_HYBRID_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
                removeSystemEquipModbus();
                deleteSystemConnectModule();
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
                                   .setProfile(ProfileType.SYSTEM_VAV_HYBRID_RTU.name())
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
        
        addAnalogConfigPoints(equipRef);
        addAnalogCmdPoints(equipRef);
        updateAhuRef(equipRef);
        //sysEquip = new SystemEquip(equipRef);
        new ControlMote(equipRef);
        initTRSystem();
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
        
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
    public boolean isCoolingAvailable() {
        return (coolingStages > 0 || getConfigVal("analog1 and output and enabled") > 0
                                    || getConfigVal("analog4 and output and enabled") > 0);
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0 || getConfigVal("analog3 and output and enabled") > 0
                                    || getConfigVal("analog4 and output and enabled") > 0);
    }
    
    @Override
    public boolean isCoolingActive(){
        return stageStatus[COOLING_1.ordinal()] > 0 || stageStatus[COOLING_2.ordinal()] > 0 || stageStatus[COOLING_3.ordinal()] > 0
               || stageStatus[COOLING_4.ordinal()] > 0 || stageStatus[COOLING_5.ordinal()] > 0;
    }

    // isCoolingActive() currently only covers cooling stages; this implementation is needed for status messages
    // In Advanced Hybrid profile, this method covers modulating-only case.
    public boolean isModulatingCoolingActive() {
        if (getConfigVal("analog1 and output and enabled") > 0 || getConfigVal("analog4 and output and enabled") > 0) {
            return systemCoolingLoopOp > 0;
        }

        return false;
    }

    @Override
    public boolean isHeatingActive(){
        return stageStatus[HEATING_1.ordinal()] > 0 || stageStatus[HEATING_2.ordinal()] > 0 || stageStatus[HEATING_3.ordinal()] > 0
               || stageStatus[HEATING_4.ordinal()] > 0 || stageStatus[HEATING_5.ordinal()] > 0;
    }

    // isHeatingActive() currently only covers heating stages; this implementation is needed for status messages
    // In Advanced Hybrid profile, this method covers modulating-only case.
    public boolean isModulatingHeatingActive() {
        if (getConfigVal("analog3 and output and enabled") > 0 || getConfigVal("analog4 and output and enabled") > 0) {
            return systemHeatingLoopOp > 0;
        }

        return false;
    }
    
    public synchronized void updateSystemPoints() {
        updateOutsideWeatherParams();
        updateMechanicalConditioning(CCUHsApi.getInstance());

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

        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemCoolingLoopOp = VavSystemController.getInstance().getCoolingSignal();
        } else if ((VavSystemController.getInstance().getSystemState() == COOLING)
                && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
            double satSpMax = VavTRTuners.getSatTRTunerVal("spmax");
            double satSpMin = VavTRTuners.getSatTRTunerVal("spmin");

            CcuLog.d(L.TAG_CCU_SYSTEM,"satSpMax :"+satSpMax+" satSpMin: "+satSpMin+" SAT: "+getSystemSAT());
            systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;
        } else {
            systemCoolingLoopOp = 0;
        }

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.COOLING, systemCoolingLoopOp);
            systemCoolingLoopOp = getSystemLoopOutputValue(Tags.COOLING);
        }

        if (VavSystemController.getInstance().getSystemState() == HEATING) {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.HEATING, systemHeatingLoopOp);
            systemHeatingLoopOp = getSystemLoopOutputValue(Tags.HEATING);
        }

        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", getSystemEquipRef());
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];

        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemFanLoopOp = getSingleZoneFanLoopOp(analogFanSpeedMultiplier);
        } else if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE ) && (L.ccu().oaoProfile != null)) {
            double smartPurgeDabFanLoopOp = TunerUtil.readTunerValByQuery("system and purge and vav and fan and loop and output", L.ccu().oaoProfile.getEquipRef());
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");

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

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.FAN, systemFanLoopOp);
            systemFanLoopOp = getSystemLoopOutputValue(Tags.FAN);
        }

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
        String scheduleStatus =  ScheduleManager.getInstance().getSystemStatusString();
        CcuLog.d(L.TAG_CCU_SYSTEM, "StatusMessage: "+systemStatus);
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: " +scheduleStatus);
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and status and message").equals(systemStatus)) {
            CCUHsApi.getInstance().writeDefaultVal("system and status and message", systemStatus);
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and scheduleStatus").equals(scheduleStatus)) {
            CCUHsApi.getInstance().writeDefaultVal("system and scheduleStatus", scheduleStatus);
        }
        
        int signal;
        double analogMin = 0, analogMax = 0;
        if (getConfigEnabled("analog1") > 0)
        {
            analogMin = getConfigVal("analog1 and cooling and min");
            analogMax = getConfigVal("analog1 and cooling and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: " + analogMin + " analog1Max: " + analogMax + " systemCoolingLoopOp: " + systemCoolingLoopOp);
    
    
            if (isCoolingLockoutActive()) {
                signal = (int)(analogMin * ANALOG_SCALE);
            } else {
                if (analogMax > analogMin) {
                    signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemCoolingLoopOp / 100)));
                } else {
                    signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemCoolingLoopOp / 100)));
                }
            }
        } else {
            signal = 0;
        }
        
        if (signal != getCmdSignal("cooling and modulating")) {
            setCmdSignal("cooling and modulating", signal);
        }
        ControlMote.setAnalogOut("analog1", signal);
        
        if (getConfigEnabled("analog2") > 0)
        {
            analogMin = getConfigVal("analog2 and fan and min");
            analogMax = getConfigVal("analog2 and fan and max");
    
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog2Min: "+analogMin+" analog2Max: "+analogMax+" systemFanLoopOp: "+systemFanLoopOp);
    
            if (analogMax > analogMin)
            {
                signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemFanLoopOp/100)));
            }
            else
            {
                signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemFanLoopOp/100)));
            }
            
        } else {
            signal = 0;
        }

        if (signal != getCmdSignal("fan and modulating")) {
            setCmdSignal("fan and modulating", signal);
        }
        ControlMote.setAnalogOut("analog2", signal);
        
        if (getConfigEnabled("analog3") > 0)
        {
            analogMin = getConfigVal("analog3 and heating and min");
            analogMax = getConfigVal("analog3 and heating and max");
    
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog3Min: "+analogMin+" analog3Max: "+analogMax+" systemHeatingLoopOp : "+systemHeatingLoopOp);
            if (isHeatingLockoutActive()) {
                signal = (int)(analogMin * ANALOG_SCALE);
            } else {
                if (analogMax > analogMin) {
                    signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemHeatingLoopOp / 100)));
                } else {
                    signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemHeatingLoopOp / 100)));
                }
            }
            
        } else  {
            signal = 0;
        }
        
        if (signal != getCmdSignal("heating and modulating")) {
            setCmdSignal("heating and modulating", signal);
        }
        ControlMote.setAnalogOut("analog3", signal);
        
        if (getConfigEnabled("analog4") > 0)
        {
            if (VavSystemController.getInstance().getSystemState() == COOLING)
            {
                analogMin = getConfigVal("analog4 and cooling and min");
                analogMax = getConfigVal("analog4 and cooling and max");
                if (analogMax > analogMin)
                {
                    signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * systemCoolingLoopOp/100));
                } else {
                    signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * systemCoolingLoopOp/100));
                }
            } else if (VavSystemController.getInstance().getSystemState() == HEATING)
            {
                analogMin = getConfigVal("analog4 and heating and min");
                analogMax = getConfigVal("analog4 and heating and max");
                if (analogMax > analogMin)
                {
                    signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * systemHeatingLoopOp/100));
                } else {
                    signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * systemHeatingLoopOp/100));
                }
            } else {
                double coolingMin = getConfigVal("analog4 and cooling and min");
                double heatingMin = getConfigVal("analog4 and heating and min");
        
                signal = (int) (ANALOG_SCALE * (coolingMin + heatingMin) /2);
            }
            CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" Composite: "+signal);
        } else {
            signal = 0;
            
        }
        if (signal != getCmdSignal("composite")) {
            setCmdSignal("composite",signal);
        }
        ControlMote.setAnalogOut("analog4", signal);
        
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

    /**
     * Each of the 7 relays could be mapped to any of the 17 logical stages ( Cooling1-5, Heating1-5,Fan1-5,
     * Humidifier, dehumidifier.
     * Only one logical point is maintained even if more than one stage is mapped to multiple relays.
     * We first determine status of logical stages here and then change the physical relay state based on that.
     * @param epidemicState
     */
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
            int stageStatus = (int)getNewRelayState(relayCount, epidemicState, relayDeactHysteresis,
                    systemMode, stage);
            tempStatus[stage.ordinal()] = tempStatus[stage.ordinal()] | stageStatus;
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
            HashSet<Integer> relaySet = getRelayMappingForStage(stage);
            CcuLog.d(L.TAG_CCU_SYSTEM, "Relays mapped to stage "+stage+" "+relaySet.toString());
            for (Integer relay : relaySet) {
                double curRelayState = ControlMote.getRelayState("relay" + relay);
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

    public double getStageStatus(Stage stage) {
        if (stage.getValue() <= COOLING_5.getValue()) {
            return getCmdSignal("cooling and stage" + (stage.ordinal() + 1));
        } else if (stage.getValue() >= HEATING_1.getValue() && stage.getValue() <= HEATING_5.getValue()) {
            return getCmdSignal("heating and stage" + (stage.ordinal() - COOLING_5.ordinal()));
        } else if (stage.getValue() >= FAN_1.getValue() && stage.getValue() <= FAN_5.getValue()) {
            return getCmdSignal("fan and stage" + (stage.ordinal() - HEATING_5.ordinal()));
        } else if (stage.getValue() == HUMIDIFIER.getValue()) {
            return getCmdSignal("humidifier");
        }  else if (stage.getValue() == DEHUMIDIFIER.getValue()) {
            return getCmdSignal("dehumidifier");
        }
        return 0;
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
                    if (isCoolingLockoutActive()) {
                        relayState = 0;
                    } else {
                        currState = getCmdSignal("cooling and stage" + (stage.ordinal() + 1));
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
                        currState = getCmdSignal("heating and stage" + (stage.ordinal() - COOLING_5.ordinal()));
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
                    if (systemMode == SystemMode.OFF ||
                            ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.UNOCCUPIED ||
                            ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.VACATION ||
                            ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.DEMAND_RESPONSE_UNOCCUPIED ||
                            ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.NONE) {
                        relayState = 0;
                    } else {
                        double humidity = VavSystemController.getInstance().getAverageSystemHumidity();
                        double targetMinHumidity = TunerUtil.readSystemUserIntentVal("target and min and inside and humidity");
                        double targetMaxHumidity = TunerUtil.readSystemUserIntentVal("target and max and inside and humidity");
                        double humidityHysteresis = TunerUtil.readTunerValByQuery("humidity and hysteresis", getSystemEquipRef());
                        if(humidity == 0){
                            relayState = 0;
                            CcuLog.d(L.TAG_CCU_SYSTEM, "Humidity is 0");
                            break;
                        }
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

    private double getStageDownTimeMinutes() {
        return TunerUtil.readTunerValByQuery("vav and stageDown and timer and counter", getSystemEquipRef());
    }

    private double getStageUpTimeMinutes() {
        return TunerUtil.readTunerValByQuery("vav and stageUp and timer and counter", getSystemEquipRef());
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

    private void updateRelays() {
        for (int relayCount = 1; relayCount <= 7; relayCount++) {
            Double newState = 0.0;
            if (getConfigEnabled("relay" + relayCount) > 0) {
                Stage mappedStage = Stage.values()[(int) getConfigAssociation("relay" + relayCount)];
                newState = getStageStatus(mappedStage);
            }
            Double curState = ControlMote.getRelayState("relay"+relayCount);
            if (newState.intValue() != curState.intValue()) {
                ControlMote.setRelayState("relay" + relayCount, newState);
            }
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

    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
    
        if (getConfigEnabled("analog2") > 0)
        {
            status.append(systemFanLoopOp > 0 ? " Fan ON " : "");
        }
        if (getConfigEnabled("analog1") > 0)
        {
            status.append((systemCoolingLoopOp > 0 && !isCoolingLockoutActive()) ? "| Cooling ON " : "");
        }
        if (getConfigEnabled("analog3") > 0)
        {
            status.append((systemHeatingLoopOp > 0 && !isHeatingLockoutActive())? "| Heating ON " : "");
        }
        
        if (!status.toString().equals("")) {
            status.insert(0, super.getStatusMessage()+" ; Analog ");
        } else {
            status.append(super.getStatusMessage());
        }
    
        return status.toString().equals("")? "System OFF" : status.toString();
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system and not modbus");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_HYBRID_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
        removeSystemEquipModbus();
    }
    
    private void addAnalogConfigPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point analog1OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog1OutputEnabled")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipref)
                                                         .addMarker("system").addMarker("config").addMarker("analog1").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                         .setEnums("false,true").setTz(tz).build();
        String analog1OutputEnabledId = hayStack.addPoint(analog1OutputEnabled);
        hayStack.writeDefaultValById(analog1OutputEnabledId, 0.0);
        Point analog2OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog2OutputEnabled")
                                                        .setSiteRef(siteRef).setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog2").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setEnums("false,true").setTz(tz).build();
        String analog2OutputEnabledId = hayStack.addPoint(analog2OutputEnabled);
        hayStack.writeDefaultValById(analog2OutputEnabledId, 0.0);
        Point analog3OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog3OutputEnabled")
                                                        .setSiteRef(siteRef)
                                                        .setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog3").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setEnums("false,true").setTz(tz).build();
        String analog3OutputEnabledId = hayStack.addPoint(analog3OutputEnabled);
        hayStack.writeDefaultValById(analog3OutputEnabledId, 0.0);
        Point analog4OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog4OutputEnabled")
                                                        .setSiteRef(siteRef)
                                                        .setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog4").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setEnums("false,true").setTz(tz).build();
        String analog4OutputEnabledId = hayStack.addPoint(analog4OutputEnabled);
        hayStack.writeDefaultValById(analog4OutputEnabledId, 0.0);
    
        Point analog1AtMinCooling = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog1AtMinCooling")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("min").addMarker("cooling").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog1AtMinCoolingId = hayStack.addPoint(analog1AtMinCooling);
        hayStack.writeDefaultValById(analog1AtMinCoolingId, 2.0 );
    
        Point analog1AtMaxCooling = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog1AtMaxCooling")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("max").addMarker("cooling").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog1AtMaxCoolingId = hayStack.addPoint(analog1AtMaxCooling);
        hayStack.writeDefaultValById(analog1AtMaxCoolingId, 10.0 );
    
        Point analog2AtMinFan = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog2AtMinFan")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("min").addMarker("fan").addMarker("writable").addMarker("sp")
                                                   .setUnit("V")
                                                   .setTz(tz)
                                                   .build();
        String analog2AtMinFanId = hayStack.addPoint(analog2AtMinFan);
        hayStack.writeDefaultValById(analog2AtMinFanId, 2.0 );
    
        Point analog2AtMaxFan = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog2AtMaxFan")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("max").addMarker("fan").addMarker("writable").addMarker("sp")
                                                   .setUnit("V")
                                                   .setTz(tz)
                                                   .build();
        String analog2AtMaxFanId = hayStack.addPoint(analog2AtMaxFan);
        hayStack.writeDefaultValById(analog2AtMaxFanId, 10.0 );
    
        Point analog3AtMinHeating = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog3AtMinHeating")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog3")
                                            .addMarker("min").addMarker("heating").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog3AtMinHeatingId = hayStack.addPoint(analog3AtMinHeating);
        hayStack.writeDefaultValById(analog3AtMinHeatingId, 2.0 );
    
        Point analog3AtMaxHeating = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog3AtMaxHeating")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog3")
                                            .addMarker("max").addMarker("heating").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog3AtMaxHeatingId = hayStack.addPoint(analog3AtMaxHeating);
        hayStack.writeDefaultValById(analog3AtMaxHeatingId, 10.0 );
    
        Point analog4AtMinCooling = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"analog4AtMinCooling")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("system").addMarker("config").addMarker("analog4")
                                        .addMarker("min").addMarker("cooling").addMarker("writable").addMarker("sp")
                                        .setUnit("V")
                                        .setTz(tz)
                                        .build();
        String analog4AtMinCoolingId = hayStack.addPoint(analog4AtMinCooling);
        hayStack.writeDefaultValById(analog4AtMinCoolingId, 7.0 );
    
        Point analog4AtMaxCooling = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"analog4AtMaxCooling")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("system").addMarker("config").addMarker("analog4")
                                        .addMarker("max").addMarker("cooling").addMarker("writable").addMarker("sp")
                                        .setUnit("V")
                                        .setTz(tz)
                                        .build();
        String analog4AtMaxCoolingId = hayStack.addPoint(analog4AtMaxCooling);
        hayStack.writeDefaultValById(analog4AtMaxCoolingId, 10.0 );
    
        Point analog4AtMinHeating = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog4AtMinHeating")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog4")
                                            .addMarker("min").addMarker("heating").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog4AtMinHeatingId = hayStack.addPoint(analog4AtMinHeating);
        hayStack.writeDefaultValById(analog4AtMinHeatingId, 5.0 );
    
        Point analog4AtMaxHeating = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog4AtMaxHeating")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog4")
                                            .addMarker("max").addMarker("heating").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog4AtMaxHeatingId = hayStack.addPoint(analog4AtMaxHeating);
        hayStack.writeDefaultValById(analog4AtMaxHeatingId, 2.0 );
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
    
    public void setConfigVal(String tags, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
    }
    
    private void addAnalogCmdPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        Point coolingSignal = new Point.Builder().setDisplayName(equipDis + "-" + "coolingSignal")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipref).setHisInterpolate("cov")
                                                 .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("modulating").addMarker("his")
                                                 .setUnit("%").setTz(tz)
                .setBacnetType(BacnetUtilKt.ANALOG_VALUE).setBacnetId(BacnetIdKt.COOLINGSIGNALID).build();
        String coolingSignalId = CCUHsApi.getInstance().addPoint(coolingSignal);
        CCUHsApi.getInstance().writeHisValById(coolingSignalId, 0.0);
        
        Point heatingSignal = new Point.Builder().setDisplayName(equipDis + "-" + "heatingSignal")
                                                 .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov")
                                                 .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("modulating").addMarker("his")
                                                 .setUnit("%").setTz(tz)
                .setBacnetType(BacnetUtilKt.ANALOG_VALUE).setBacnetId(BacnetIdKt.HEATINGSIGNALID).build();
        String heatingSignalId = CCUHsApi.getInstance().addPoint(heatingSignal);
        CCUHsApi.getInstance().writeHisValById(heatingSignalId, 0.0);
        
        Point fanSignal = new Point.Builder().setDisplayName(equipDis + "-" + "fanSignal")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref).setHisInterpolate("cov")
                                             .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("his").addMarker("modulating")
                                             .setUnit("%").setBacnetType(BacnetUtilKt.ANALOG_VALUE).setBacnetId(BacnetIdKt.FANSIGNALID)
                                             .setTz(tz).build();
        String fanSignalId = CCUHsApi.getInstance().addPoint(fanSignal);
        CCUHsApi.getInstance().writeHisValById(fanSignalId, 0.0);
        
        Point compositeSignal = new Point.Builder().setDisplayName(equipDis + "-" + "CompositeSignal")
                                                   .setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov")
                                                   .addMarker("system").addMarker("cmd").addMarker("composite").addMarker("modulating").addMarker("his")
                                                   .setUnit("%").setTz(tz)
                .setBacnetType(BacnetUtilKt.ANALOG_VALUE).setBacnetId(BacnetIdKt.COMPOSITESIGNALID).build();
        String compositeSignalId = CCUHsApi.getInstance().addPoint(compositeSignal);
        CCUHsApi.getInstance().writeHisValById(compositeSignalId, 0.0);
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
}
