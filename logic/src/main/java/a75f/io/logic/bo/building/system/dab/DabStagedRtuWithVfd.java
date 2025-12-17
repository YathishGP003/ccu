package a75f.io.logic.bo.building.system.dab;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.equips.DabStagedVfdSystemEquip;
import a75f.io.domain.util.CommonQueries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;

import static a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_1;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_2;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_3;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_4;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_5;
import static a75f.io.logic.bo.building.system.util.AdvancedAhuUtilKt.getModulatedOutput;
import static a75f.io.logic.bo.util.SystemScheduleUtil.isSystemOccupiedForDcv;

public class DabStagedRtuWithVfd extends DabStagedRtu
{
    private static final int ANALOG_SCALE = 10;
    private static final int MAX_RELAY_COUNT = 8;

    public String getProfileName() {
        if(BuildConfig.BUILD_TYPE.equalsIgnoreCase(CARRIER_PROD)){
            return "VVT-C Staged RTU with VFD Fan";
        } else {
            return "DAB Staged RTU with VFD Fan";
        }
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_DAB_STAGED_VFD_RTU;
    }
    
    @Override
    public void doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo();
        updateSystemPoints();
    }
    @Override
    public void addSystemEquip() {
        systemEquip = (DabStagedVfdSystemEquip) Domain.systemEquip;
        updateStagesSelected();
    }

    public synchronized void updateSystemPoints() {
        super.updateSystemPoints();
        systemEquip = (DabStagedVfdSystemEquip) Domain.systemEquip;
        DabStagedVfdSystemEquip vfdSystemEquip = (DabStagedVfdSystemEquip) systemEquip;
        if (vfdSystemEquip.getAnalog2OutputEnable().readDefaultVal() > 0) {
            handleAnalogOutControl(vfdSystemEquip);
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Analog2 output is disabled, skipping analog mapping.");
        }
    }

    public void handleAnalogOutControl(DabStagedVfdSystemEquip vfdSystemEquip) {
        if(vfdSystemEquip.getAnalog2OutputEnable().readDefaultVal() > 0) {
            int association = (int) vfdSystemEquip.getAnalog2OutputAssociation().readDefaultVal();
            AnalogMapping mapping = AnalogMapping.values()[association];
            switch (mapping) {
                case FAN_SPEED:
                    handleFanSpeed(vfdSystemEquip);
                    break;
                case COMPRESSOR_SPEED:
                    handleCompressorSpeed(vfdSystemEquip);
                    break;
                case DCV_MODULATION:
                    handleDcvModulation(vfdSystemEquip);
                    break;
            }
        }
    }


    public void handleFanSpeed(DabStagedVfdSystemEquip vfdSystemEquip) {
        boolean isEconomizingAvailable = CCUHsApi.getInstance().readHisValByQuery("point and oao and economizing and available") > 0;
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\"" + getSystemEquipRef() + "\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        double signal = 0;

        if (isCoolingActive() || isCompressorActive()) {
            if (getDomainPointForStage(COOLING_5).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_5).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2CoolStage5().readDefaultVal();
            } else if (getDomainPointForStage(COOLING_4).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_4).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2CoolStage4().readDefaultVal();
            } else if (getDomainPointForStage(COOLING_3).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_3).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2CoolStage3().readDefaultVal();
            } else if (getDomainPointForStage(COOLING_2).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_2).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2CoolStage2().readDefaultVal();
            } else if (getDomainPointForStage(COOLING_1).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_1).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2CoolStage1().readDefaultVal();
            }
        } else if (isHeatingActive() || isCompressorActive()) {
            if (getDomainPointForStage(Stage.HEATING_5).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_5).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2HeatStage5().readDefaultVal();
            } else if (getDomainPointForStage(Stage.HEATING_4).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_4).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2HeatStage4().readDefaultVal();
            } else if (getDomainPointForStage(Stage.HEATING_3).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_3).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2HeatStage3().readDefaultVal();
            } else if (getDomainPointForStage(Stage.HEATING_2).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_2).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2HeatStage2().readDefaultVal();
            } else if (getDomainPointForStage(Stage.HEATING_1).readHisVal() > 0 || getDomainPointForStage(Stage.COMPRESSOR_1).readHisVal() > 0) {
                signal = vfdSystemEquip.getAnalog2HeatStage1().readDefaultVal();
            }
        } else if (isEconomizingAvailable && (systemCoolingLoopOp > 0)) {
            signal = vfdSystemEquip.getAnalog2Economizer().readDefaultVal();
        } else if (systemEquip.getConditioningStages().getFanEnable().readHisVal() > 0) {
            signal = vfdSystemEquip.getAnalog2Recirculate().readDefaultVal();
        } else {
            //For all other cases analog2-out should be the minimum config value
            signal = vfdSystemEquip.getAnalog2Default().readDefaultVal();
        }
        if ((epidemicState == EpidemicState.PREPURGE) && L.ccu().oaoProfile != null) {
            double smartPrePurgeFanSpeed = L.ccu().oaoProfile.getOAOEquip().getSystemPrePurgeFanSpeedTuner().readPriorityVal();
            signal = Math.max(signal, smartPrePurgeFanSpeed / ANALOG_SCALE);
        } else if ((epidemicState == EpidemicState.POSTPURGE) && L.ccu().oaoProfile != null) {
            double smartPurgeFanLoopOp = L.ccu().oaoProfile.getOAOEquip().getSystemPostPurgeFanSpeedTuner().readPriorityVal();
            signal = Math.max(signal, smartPurgeFanLoopOp / ANALOG_SCALE);
        }

        signal = ANALOG_SCALE * signal;

        if (!isSystemOccupied() && isLockoutActiveDuringUnoccupied()) {
            signal = 0;
        }
        vfdSystemEquip.getFanSignal().writePointValue(signal);
        Domain.cmBoardDevice.getAnalog2Out().writePointValue(signal);
        CcuLog.d(L.TAG_CCU_SYSTEM, " analog2 fan Signal : " + signal);
    }

    public void handleCompressorSpeed(DabStagedVfdSystemEquip vfdSystemEquip) {
        double min = vfdSystemEquip.getAnalog2MinCompressorSpeed().readDefaultVal();
        if ((systemCoolingLoopOp > 0 && isCoolingLockoutActive()) ||
                (systemHeatingLoopOp > 0 && isHeatingLockoutActive())) {
            double signal = ANALOG_SCALE * min; // Use minimum value when in lockout
            vfdSystemEquip.getCompressorSpeed().writePointValue(signal);
            Domain.cmBoardDevice.getAnalog2Out().writePointValue(signal);
            CcuLog.d(L.TAG_CCU_SYSTEM, " Lockout Active : Compressor speed Signal : " + signal);
            return;
        }
        double max = vfdSystemEquip.getAnalog2MaxCompressorSpeed().readDefaultVal();
        double signal = ANALOG_SCALE * getModulatedOutput(systemCompressorLoop, min, max);
        CcuLog.d(L.TAG_CCU_SYSTEM, " analog2 Compressor speed Signal : " + signal);
        vfdSystemEquip.getCompressorSpeed().writePointValue(signal);
        Domain.cmBoardDevice.getAnalog2Out().writePointValue(signal);
    }

    public void handleDcvModulation(DabStagedVfdSystemEquip vfdSystemEquip) {
        double signal;
        double min = vfdSystemEquip.getAnalog2MinDCVDamper().readDefaultVal();
        if (isSystemOccupiedForDcv() && vfdSystemEquip.getConditioningMode().readPriorityVal() > 0) {
            double max = vfdSystemEquip.getAnalog2MaxDCVDamper().readDefaultVal();
            signal = ANALOG_SCALE * getModulatedOutput(systemCo2LoopOp, min, max);
        } else {
            signal = ANALOG_SCALE * min; // Use minimum value when not occupied
        }
        CcuLog.d(L.TAG_CCU_SYSTEM, " analog2 DCV Damper Signal : " + signal);
        vfdSystemEquip.getDamperModulation().writePointValue(signal);
        Domain.cmBoardDevice.getAnalog2Out().writePointValue(signal);
    }

    @Override
    public synchronized void deleteSystemEquip() {
        ArrayList<HashMap<Object, Object>> listOfEquips = CCUHsApi.getInstance().readAllEntities(CommonQueries.SYSTEM_PROFILE);
        for (HashMap<Object, Object> equip : listOfEquips) {
            if(equip.get("profile") != null){
                if(ProfileType.getProfileTypeForName(equip.get("profile").toString()) != null){
                    if (ProfileType.getProfileTypeForName(equip.get("profile").toString()).name().equals(ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name())) {
                        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "DabStagedRtuWithVfd removing profile with it id-->"+equip.get("id").toString());
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
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
}
