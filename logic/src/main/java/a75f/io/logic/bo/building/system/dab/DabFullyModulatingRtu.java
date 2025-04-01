package a75f.io.logic.bo.building.system.dab;

import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.PhysicalPoint;
import a75f.io.domain.api.Point;
import a75f.io.domain.equips.DabModulatingRtuSystemEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BuildConfig;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.util.CCUUtils;

import static a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;

public class DabFullyModulatingRtu extends DabSystemProfile
{
    private static final int ANALOG_SCALE = 10;
    public DabModulatingRtuSystemEquip systemEquip;
    
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
        if (isDcwbEnabled()) {
            return systemEquip.getAnalog4OutputEnable().readDefaultVal() > 0;
        } else {
            return systemEquip.getAnalog1OutputEnable().readDefaultVal() > 0;
        }
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return systemEquip.getAnalog3OutputEnable().readDefaultVal() > 0;
    }
    
    @Override
    public boolean isCoolingActive(){
        return systemCoolingLoopOp > 0;
    }
    
    @Override
    public boolean isHeatingActive(){
        return systemHeatingLoopOp > 0;
    }
    
    public double systemDCWBValveLoopOutput;
    DcwbAlgoHandler dcwbAlgoHandler = null;

    private synchronized void updateSystemPoints() {

        systemEquip = (DabModulatingRtuSystemEquip) Domain.systemEquip;
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
    
            //Analog1 controls water valve when the DCWB enabled.
            updateAnalog1DcwbOutput(dabSystem);
    
            //Could be mapped to cooling or co2 based on configuration.
            updateAnalog4Output(dabSystem);
            
        } else {
            //Analog1 controls cooling when the DCWB is disabled
            if(L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
                updateAnalog1DabOutput(dabSystem, true);
            } else {
                updateAnalog1DabOutput(dabSystem, false);
            }
        }
        
        //Analog2 controls Central Fan
        updateAnalog2Output(dabSystem);
        
        //Analog3 controls heating.
        updateAnalog3Output(dabSystem);
        
        updateRelayOutputs(dabSystem);
        
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

    /**
     * @brief Determines the current status of the humidifier and returns a string message.
     *
     * This function checks the status of the humidifier based on relay settings and other conditions.
     * If the current humidifier type is 1.0 or Relay 7 output is disabled, it checks the value of the humidifier
     * and returns either "Humidifier ON" or "Humidifier OFF" accordingly.
     *
     * @return A string indicating the humidifier status:
     *         - " | Humidifier ON " if the humidifier is on.
     *         - " | Humidifier OFF " if the humidifier is off.
     *         - An empty string if the humidifier facility is not available.
     */
    public String isHumidifierOn() {
        double isHumidifierOn = systemEquip.getHumidifier().readHisVal();
        double curHumidifierType = systemEquip.getRelay7OutputAssociation().readDefaultVal();
        boolean isRelay7OutputEnabled = systemEquip.getRelay7OutputEnable().readDefaultVal() > 0;

        if(curHumidifierType == 1.0 || !isRelay7OutputEnabled){
            return "";
        }else {
            if(isHumidifierOn > 0){
                return " | Humidifier ON ";
            }else {
                return " | Humidifier OFF ";
            }
        }
    }

    /**
     * @brief Determines the current status of the dehumidifier and returns a string message.
     *
     * This function checks the status of the dehumidifier based on relay settings and other conditions.
     * it checks the value of the dehumidifier and returns either "DeHumidifier ON" or "DeHumidifier OFF" accordingly.
     *
     * @return A string indicating the dehumidifier status:
     *         - " | DeHumidifier ON " if the dehumidifier is on.
     *         - " | DeHumidifier OFF " if the dehumidifier is off.
     *         - An empty string if the dehumidifier facility is not available.
     */
    public String isDeHumidifierOn() {
        double isDeHumidifierOn = systemEquip.getDehumidifier().readHisVal();
        double curHumidifierType = systemEquip.getRelay7OutputAssociation().readDefaultVal();
        boolean isRelay7OutputEnabled = systemEquip.getRelay7OutputEnable().readDefaultVal() > 0;

        if(curHumidifierType == 0.0 || !isRelay7OutputEnabled){
            return "";
        }else {
            if(isDeHumidifierOn > 0){
                return " | Dehumidifier ON ";
            }else {
                return " | Dehumidifier OFF ";
            }
        }
    }

    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        Boolean economizingOn = systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable();
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
        return status.toString().equals("")? "System OFF" + isDeHumidifierOn() + isHumidifierOn() : status.toString() + isDeHumidifierOn() + isHumidifierOn();
    }
    
    public void addSystemEquip() {
        systemEquip = (DabModulatingRtuSystemEquip) Domain.systemEquip;
        updateSystemPoints();
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("system and equip and not modbus and not connectModule");
        if (equip.get("profile").equals(ProfileType.SYSTEM_DAB_ANALOG_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
        removeSystemEquipModbus();
        deleteSystemConnectModule();
    }
    
    private void updateAnalog1DabOutput(DabSystemController dabSystem, Boolean economizingOn) {
        
        double signal = 0;
        if (dabSystem.getSystemState() == COOLING) {
                systemCoolingLoopOp = dabSystem.getCoolingSignal();
        } else {
                systemCoolingLoopOp = 0;
        }
        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);
            systemCoolingLoopOp = systemEquip.getCoolingLoopOutput().readHisVal();
        }
        systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);

        if (systemEquip.getAnalog1OutputEnable().readPriorityVal() > 0) {
            double analogMin = systemEquip.getAnalog1MinCooling().readPriorityVal();
            double analogMax = systemEquip.getAnalog1MaxCooling().readPriorityVal();
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: "+analogMin+" analog1Max: "+analogMax+" systemCoolingLoop : "+systemCoolingLoopOp);
    
            if (isCoolingLockoutActive()) {
                signal = analogMin * ANALOG_SCALE;
            } else {
                double economizingToMainCoolingLoopMap = 0;
                if(L.ccu().oaoProfile != null) {
                    economizingToMainCoolingLoopMap =  L.ccu().oaoProfile.getOAOEquip().getEconomizingToMainCoolingLoopMap().readPriorityVal();
                }
                if(!economizingOn) {
                    /* OAT > Max Economizing Temp */
                    signal = getModulatedAnalogVal(analogMin, analogMax, systemCoolingLoopOp);
                } else if (economizingOn && systemCoolingLoopOp < economizingToMainCoolingLoopMap) {
                    /* When only Economizing is active, (CLO < 30% (economizingToMainCoolingLoopMap) */
                    signal = analogMin * ANALOG_SCALE;
                } else if (economizingOn && systemCoolingLoopOp >= economizingToMainCoolingLoopMap) {
                    /* Economizing + Analog Cooling */
                    signal = getModulatedAnalogValDuringEcon(analogMin, analogMax, systemCoolingLoopOp, economizingToMainCoolingLoopMap);
                }
            }
        }
        
        if (signal != systemEquip.getCoolingSignal().readHisVal()) {
            systemEquip.getCoolingSignal().writeHisVal(signal);
        }
        Domain.cmBoardDevice.getAnalog1Out().writeHisVal(signal);
    }
    
    private void updateAnalog1DcwbOutput(DabSystemController dabSystem) {
        
        boolean isAnalog1Enabled = systemEquip.getAnalog1OutputEnable().readPriorityVal() > 0;

        if (isAnalog1Enabled && dabSystem.getSystemState() == COOLING) {
            dcwbAlgoHandler.runLoopAlgorithm();
            systemDCWBValveLoopOutput = CCUUtils.roundToTwoDecimal(dcwbAlgoHandler.getChilledWaterValveLoopOutput());
        } else {
            systemDCWBValveLoopOutput = 0;
            dcwbAlgoHandler.resetChilledWaterValveLoop();
        }
    
        double signal = 0;
    
        if (isAnalog1Enabled) {
            double analogMin = systemEquip.getAnalog1MinCooling().readPriorityVal();
            double analogMax = systemEquip.getAnalog1MaxCooling().readPriorityVal();

            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: "+analogMin+" analog1Max: "+analogMax+" systemDCWBValveLoopOutput : "+systemDCWBValveLoopOutput);
            signal = getModulatedAnalogVal(analogMin, analogMax, systemDCWBValveLoopOutput);
        }
    
        systemEquip.getSystemDCWBValveLoopOutput().writePointValue(systemDCWBValveLoopOutput);
    
        if (signal != systemEquip.getChilledWaterValveSignal().readHisVal()) {
            systemEquip.getChilledWaterValveSignal().writeHisVal(signal);
        }
        Domain.cmBoardDevice.getAnalog1Out().writePointValue(signal);

        systemEquip.getChilledWaterExitTemperatureTarget().writePointValue(dcwbAlgoHandler.getChilledWaterTargetExitTemperature());
    }
    
    private void updateAnalog2Output(DabSystemController dabSystem) {
        updateFanLoop(dabSystem);
    
        double signal = 0;
        if (systemEquip.getAnalog2OutputEnable().readPriorityVal() > 0) {
            double analogMin = systemEquip.getAnalog2MinFan().readPriorityVal();
            double analogMax = systemEquip.getAnalog2MaxFan().readPriorityVal();

            CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemFanLoopOp: "+systemFanLoopOp);
    
            signal = getModulatedAnalogVal(analogMin, analogMax, systemFanLoopOp);
        
        }
        
        if (signal != systemEquip.getFanSignal().readHisVal()) {
            systemEquip.getFanSignal().writeHisVal(signal);
        }
        Domain.cmBoardDevice.getAnalog2Out().writePointValue(signal);
    }
    
    private void updateAnalog3Output(DabSystemController dabSystem) {
        double signal = 0;
        if (dabSystem.getSystemState() == HEATING) {
            systemHeatingLoopOp = dabSystem.getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            systemEquip.getHeatingLoopOutput().writeHisVal(systemHeatingLoopOp);
            systemHeatingLoopOp = systemEquip.getHeatingLoopOutput().readHisVal();
        }
        systemEquip.getHeatingLoopOutput().writeHisVal(systemHeatingLoopOp);
    
        if (systemEquip.getAnalog3OutputEnable().readPriorityVal() > 0) {
            double analogMin = systemEquip.getAnalog3MinHeating().readPriorityVal();
            double analogMax = systemEquip.getAnalog3MaxHeating().readPriorityVal();

            CcuLog.d(L.TAG_CCU_SYSTEM, "analog3Min: "+analogMin+" analog3Max: "+analogMax+" systemHeatingLoop : "+systemHeatingLoopOp);
    
            if (isHeatingLockoutActive()) {
                signal = analogMin * ANALOG_SCALE;
            } else {
                signal = getModulatedAnalogVal(analogMin, analogMax, systemHeatingLoopOp);
            }
        }
        
        if (signal != systemEquip.getHeatingSignal().readHisVal()) {
            systemEquip.getHeatingSignal().writeHisVal(signal);
        }
        Domain.cmBoardDevice.getAnalog3Out().writePointValue(signal);
    }
    
    private void updateAnalog4Output(DabSystemController dabSystem) {
        double loopType = systemEquip.getAnalog4OutputAssociation().readPriorityVal();

        double signal = 0;
        if (dabSystem.getSystemState() == COOLING) {
            systemCoolingLoopOp = dabSystem.getCoolingSignal();
        } else {
            systemCoolingLoopOp = 0;
        }

        if(AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.COOLING,systemCoolingLoopOp);
            systemCoolingLoopOp = getSystemLoopOutputValue(Tags.COOLING);
        }
        systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);

        systemCo2LoopOp = getCo2LoopOp();
        systemEquip.getCo2LoopOutput().writePointValue(systemCo2LoopOp);
    
        if (systemEquip.getAnalog4OutputEnable().readPriorityVal() > 0) {

            double analogMin = loopType > 0 ? systemEquip.getAnalog4MinOutsideDamper().readPriorityVal() : systemEquip.getAnalogOut4MinCoolingLoop().readPriorityVal();
            double analogMax = loopType > 0 ? systemEquip.getAnalog4MaxOutsideDamper().readPriorityVal() : systemEquip.getAnalogOut4MaxCoolingLoop().readPriorityVal();
        
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog4Min: "+analogMin+" analog4Max: "+analogMax+
                                       " systemCoolingLoopOp : "+systemCoolingLoopOp + " systemCo2LoopOp : "+systemCo2LoopOp);
            if (loopType == 0 && isCoolingLockoutActive()) {
                signal = analogMin * ANALOG_SCALE;
            } else {
                signal = getModulatedAnalogVal(analogMin, analogMax,
                                               loopType == 0 ? systemCoolingLoopOp : systemCo2LoopOp);
            }
        }
    
        if (signal != systemEquip.getCoolingSignal().readHisVal()) {
            systemEquip.getCoolingSignal().writePointValue(signal);
        }
        Domain.cmBoardDevice.getAnalog4Out().writePointValue(signal);
    }
    
    private void updateRelayOutputs(DabSystemController dabSystem) {
        
        double signal = 0;
        SystemMode systemMode = SystemMode.values()[(int)systemEquip.getConditioningMode().readPriorityVal()];
        if (systemEquip.getRelay3OutputEnable().readPriorityVal() > 0 && systemMode != SystemMode.OFF) {
            signal = (isSystemOccupied() || systemFanLoopOp > 0) ? 1 : 0;
        }
        if(signal != systemEquip.getOccupancySignal().readHisVal()) {
            systemEquip.getOccupancySignal().writeHisVal(signal);
        }
        systemEquip.getFanEnable().writeHisVal(signal);
        Domain.cmBoardDevice.getRelay3().writePointValue(signal);
        if (systemEquip.getRelay7OutputEnable().readPriorityVal() > 0 && systemMode != SystemMode.OFF
                    && isSystemOccupied()) {

            double humidity = dabSystem.getAverageSystemHumidity();
            double targetMinHumidity = systemEquip.getSystemtargetMinInsideHumidity().readPriorityVal();
            double targetMaxHumidity = systemEquip.getSystemtargetMaxInsideHumidity().readPriorityVal();

            boolean humidifier = systemEquip.getRelay7OutputAssociation().readPriorityVal() > 0;

            double humidityHysteresis = systemEquip.getDabHumidityHysteresis().readPriorityVal();
            int curSignal = (int)Domain.cmBoardDevice.getRelay7().readHisVal();
            if(humidity == 0) {
                signal = 0;
                CcuLog.d(L.TAG_CCU_SYSTEM, "Humidity is 0");
            } else {
                if (!humidifier) {
                    //Humidification
                    if (humidity < targetMinHumidity) {
                        signal = 1;
                    } else if (humidity > (targetMinHumidity + humidityHysteresis)) {
                        signal = 0;
                    } else {
                        signal = curSignal;
                    }
                    systemEquip.getHumidifier().writeHisVal(signal);
                } else {
                    //Dehumidification
                    if (humidity > targetMaxHumidity) {
                        signal = 1;
                    } else if (humidity < (targetMaxHumidity - humidityHysteresis)) {
                        signal = 0;
                    } else {
                        signal = curSignal;
                    }
                    systemEquip.getDehumidifier().writeHisVal(signal);
                }
                CcuLog.d(L.TAG_CCU_SYSTEM,"humidity :"+humidity+" targetMinHumidity: "+targetMinHumidity+" humidityHysteresis: "+humidityHysteresis+
                        " targetMaxHumidity: "+targetMaxHumidity+" signal: "+signal*100);
            }

            Domain.cmBoardDevice.getRelay7().writeHisVal(signal);
        } else {
            systemEquip.getHumidifier().writeHisVal(0);
            systemEquip.getDehumidifier().writeHisVal(0);
            Domain.cmBoardDevice.getRelay7().writePointValue(0);
        }
    
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

    
    }

    /**
     * Calculates a modulated analog value during economizing based on the given range,
     * the input percentage value, and the economizing threshold.
     *
     * @param analogMin                        The minimum value of the analog range.
     * @param analogMax                        The maximum value of the analog range.
     * @param val                              The current cooling loop percentage (expected between 0 and 100)
     *                                         representing the position within the range.
     * @param economizingToMainCoolingLoopMap  The threshold percentage value at which economizing ends
     *                                         and modulation begins.
     * @return                                 analogMin if loop output is less than economizingToMainCoolingLoopMap or return
     *                                         the scaled value between analogMin and analogMax based on the input percentage.
     */
    private double getModulatedAnalogValDuringEcon(double analogMin, double analogMax, double val, double economizingToMainCoolingLoopMap) {
        double modulatedVal;
        if(val < economizingToMainCoolingLoopMap) {
            return analogMin * ANALOG_SCALE;
        }
        if(economizingToMainCoolingLoopMap >= 100) {
            return analogMax * ANALOG_SCALE;
        }
        if (analogMax > analogMin) {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * ((val - economizingToMainCoolingLoopMap) / (100 - economizingToMainCoolingLoopMap))));
        } else {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * ((val - economizingToMainCoolingLoopMap) / (100 - economizingToMainCoolingLoopMap))));
        }
        return modulatedVal;
    }

    private double getModulatedAnalogVal(double analogMin, double analogMax, double val) {
        double modulatedVal;
        if (analogMax > analogMin) {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (val/100)));
        } else {
            modulatedVal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (val/100)));
        }
        return modulatedVal;
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
}
