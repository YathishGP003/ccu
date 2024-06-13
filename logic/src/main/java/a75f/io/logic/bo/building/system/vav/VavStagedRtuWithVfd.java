package a75f.io.logic.bo.building.system.vav;

import static a75f.io.logic.bo.building.hvac.Stage.COOLING_1;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_2;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_3;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_4;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_5;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.Domain;
import a75f.io.domain.equips.VavStagedVfdSystemEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 2/18/19.
 */

public class VavStagedRtuWithVfd extends VavStagedRtu
{
    private static final int ANALOG_SCALE = 10;
    private static final int MAX_RELAY_COUNT = 8;

    //VavStagedVfdSystemEquip systemEquip;
    @Override
    public String getProfileName()
    {
        return "VAV Staged RTU with VFD Fan";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_STAGED_VFD_RTU;
    }
    
    @Override
    public void addSystemEquip() {
        systemEquip = (VavStagedVfdSystemEquip) Domain.systemEquip;
        initTRSystem();
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
        return (coolingStages > 0 );
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0);
    }
    
    public synchronized void updateSystemPoints() {
        super.updateSystemPoints();
        VavStagedVfdSystemEquip vfdSystemEquip = (VavStagedVfdSystemEquip) systemEquip;
        boolean isEconomizingAvailable = CCUHsApi.getInstance().readHisValByQuery("point and oao and economizing and available") > 0.0;
        double epidemicMode = vfdSystemEquip.getEpidemicModeSystemState().readHisVal();
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        double signal = 0;
        if (vfdSystemEquip.getAnalog2OutputEnable().readDefaultVal() > 0) {
            if (isCoolingActive()) {
                if (getDomainPointForStage(COOLING_5).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2CoolStage5().readDefaultVal();
                } else if (getDomainPointForStage(COOLING_4).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2CoolStage4().readDefaultVal();
                } else if (getDomainPointForStage(COOLING_3).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2CoolStage3().readDefaultVal();
                } else if (getDomainPointForStage(COOLING_2).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2CoolStage2().readDefaultVal();
                } else if (getDomainPointForStage(COOLING_1).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2CoolStage1().readDefaultVal();
                }
            }
            else if (isHeatingActive()) {
                if (getDomainPointForStage(Stage.HEATING_5).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2HeatStage5().readDefaultVal();
                } else if (getDomainPointForStage(Stage.HEATING_4).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2HeatStage4().readDefaultVal();
                } else if (getDomainPointForStage(Stage.HEATING_3).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2HeatStage3().readDefaultVal();
                } else if (getDomainPointForStage(Stage.HEATING_2).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2HeatStage2().readDefaultVal();
                } else if (getDomainPointForStage(Stage.HEATING_1).readHisVal() > 0) {
                    signal = vfdSystemEquip.getAnalog2HeatStage1().readDefaultVal();
                }
            } else if (isEconomizingAvailable && (systemCoolingLoopOp > 0)){
                signal = vfdSystemEquip.getAnalog2Economizer().readDefaultVal();//getConfigVal("analog2 and economizer");
            }
            else if (systemEquip.getFanStage1().readHisVal() > 0/*stageStatus[FAN_1.ordinal()] > 0*/) {
                signal = vfdSystemEquip.getAnalog2Recirculate().readDefaultVal();
            }
            else {
                //For all other cases analog2-out should be the minimum config value
                signal = vfdSystemEquip.getAnalog2Default().readDefaultVal();
            }

            //TODO- Part of OAO. Should be moved to domainName after OAO is moved to DM
            if((epidemicState == EpidemicState.PREPURGE) && L.ccu().oaoProfile != null){
                double smartPrePurgeFanSpeed = TunerUtil.readTunerValByQuery("system and prePurge and fan and speed", L.ccu().oaoProfile.getEquipRef());
                signal = Math.max(signal,smartPrePurgeFanSpeed / ANALOG_SCALE);
            }else if((epidemicState == EpidemicState.POSTPURGE) && L.ccu().oaoProfile != null){
                double smartPurgeFanLoopOp = TunerUtil.readTunerValByQuery("system and postPurge and fan and speed", L.ccu().oaoProfile.getEquipRef());
                signal = Math.max(signal,smartPurgeFanLoopOp / ANALOG_SCALE);
            }
            
            signal = ANALOG_SCALE * signal;
        }

        vfdSystemEquip.getFanSignal().writeHisVal(signal);
        //ControlMote.setAnalogOut("analog2", signal);
        Domain.cmBoardDevice.getAnalog2Out().writeHisVal(signal);
        CcuLog.d(L.TAG_CCU_SYSTEM, " analog2 Signal : "+ signal);
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system and not modbus");
        if (ProfileType.getProfileTypeForName(equip.get("profile").toString()).name().equals(ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
        removeSystemEquipModbus();
    }
}
