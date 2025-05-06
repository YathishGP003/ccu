package a75f.io.logic.bo.building.system.dab;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.domain.api.Domain;
import a75f.io.domain.equips.DabStagedVfdSystemEquip;
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
    
    public synchronized void updateSystemPoints()
    {
        super.updateSystemPoints();
        DabStagedVfdSystemEquip vfdSystemEquip = (DabStagedVfdSystemEquip) systemEquip;
        boolean isEconomizingAvailable = CCUHsApi.getInstance().readHisValByQuery("point and oao and economizing and available") > 0 ;
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        double signal = 0;
        if (vfdSystemEquip.getAnalog2OutputEnable().readDefaultVal() > 0)
        {
            if (isCoolingActive())
            {
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
            else if (isHeatingActive())
            {
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
            } else if(isEconomizingAvailable && (systemCoolingLoopOp > 0)){
                signal = vfdSystemEquip.getAnalog2Economizer().readDefaultVal();
            }
            else if (systemEquip.getFanStage1().readHisVal() > 0)
            {
                signal = vfdSystemEquip.getAnalog2Recirculate().readDefaultVal();
            }
            else {
                //For all other cases analog2-out should be the minimum config value
                signal = vfdSystemEquip.getAnalog2Default().readDefaultVal();
            }
            if((epidemicState == EpidemicState.PREPURGE) && L.ccu().oaoProfile != null){
                double smartPrePurgeFanSpeed = L.ccu().oaoProfile.getOAOEquip().getSystemPrePurgeFanSpeedTuner().readPriorityVal();
                signal = Math.max(signal,smartPrePurgeFanSpeed / ANALOG_SCALE);
            }else if((epidemicState == EpidemicState.POSTPURGE) && L.ccu().oaoProfile != null){
                double smartPurgeFanLoopOp = L.ccu().oaoProfile.getOAOEquip().getSystemPostPurgeFanSpeedTuner().readPriorityVal();
                signal = Math.max(signal,smartPurgeFanLoopOp / ANALOG_SCALE);
            }
    
            signal = ANALOG_SCALE * signal;
        }

        if(!isSystemOccupied() && isLockoutActiveDuringUnoccupied()){
            signal = 0;
        }
        vfdSystemEquip.getFanSignal().writeHisVal(signal);
        Domain.cmBoardDevice.getAnalog2Out().writeHisVal(signal);
        CcuLog.d(L.TAG_CCU_SYSTEM, " analog2 Signal : "+signal);
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("system and equip and not modbus and not connectModule");
        if (ProfileType.getProfileTypeForName(equip.get("profile").toString()).name().equals(ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
        removeSystemEquipModbus();
        deleteSystemConnectModule();
    }

    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
}
