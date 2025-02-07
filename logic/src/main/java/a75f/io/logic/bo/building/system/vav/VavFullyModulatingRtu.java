package a75f.io.logic.bo.building.system.vav;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import static a75f.io.logic.bo.building.schedules.ScheduleUtil.ACTION_STATUS_CHANGE;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.util.DesiredTempDisplayMode.setSystemModeForVav;

import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.PhysicalPoint;
import a75f.io.domain.equips.VavModulatingRtuSystemEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.autocommission.AutoCommissioningUtil;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

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
        return systemEquip.getAnalog1OutputEnable().readDefaultVal() > 0;
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
    
    private synchronized void updateSystemPoints() {

        systemEquip = (VavModulatingRtuSystemEquip) Domain.systemEquip;
        updateOutsideWeatherParams();
        updateMechanicalConditioning(CCUHsApi.getInstance());
        
        SystemMode systemMode = SystemMode.values()[(int)systemEquip.getConditioningMode().readPriorityVal()];
    
        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemCoolingLoopOp = VavSystemController.getInstance().getCoolingSignal();
        } else if (VavSystemController.getInstance().getSystemState() == COOLING &&
             (systemMode == SystemMode.COOLONLY ||
              systemMode == SystemMode.AUTO)) {

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

        int signal;
        double analogMin, analogMax;
        systemEquip.getCoolingLoopOutput().writePointValue(systemCoolingLoopOp);
        systemCoolingLoopOp = systemEquip.getCoolingLoopOutput().readHisVal();
        if (systemEquip.getAnalog1OutputEnable().readPriorityVal() > 0)
        {
            analogMin = systemEquip.getAnalog1MinCooling().readPriorityVal();
            analogMax = systemEquip.getAnalog1MaxCooling().readPriorityVal();
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: "+analogMin+" analog1Max: "+analogMax+" SAT: "+getSystemSAT());
            CcuLog.d("CCU_DEVICE", "test-writable write signal analog1Min: "+analogMin+" analog1Max: "+analogMax+" SAT: "+getSystemSAT()+ "<systemCoolingLoopOp-->"+systemCoolingLoopOp);
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

        CcuLog.d("CCU_DEVICE", "test-writable write signal :=======signal value====>"+signal + "<--point name :> "+systemEquip.getCoolingSignal().getDis() + "<--point id :-->"+systemEquip.getCoolingSignal().getId());
        systemEquip.getCoolingSignal().writeHisVal(signal);
        Domain.cmBoardDevice.getAnalog1Out().writePointValue(signal);

    
        if (VavSystemController.getInstance().getSystemState() == HEATING)
        {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }

        systemEquip.getHeatingLoopOutput().writePointValue(systemHeatingLoopOp);
        systemHeatingLoopOp = systemEquip.getHeatingLoopOutput().readHisVal();
        if (systemEquip.getAnalog3OutputEnable().readPriorityVal() > 0)
        {
            analogMin = systemEquip.getAnalog3MinHeating().readPriorityVal();
            analogMax = systemEquip.getAnalog3MaxHeating().readPriorityVal();
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog3Min: "+analogMin+" analog3Max: "+analogMax+" HeatingSignal : "+VavSystemController.getInstance().getHeatingSignal());
            if (isHeatingLockoutActive()) {
                signal = (int)(analogMin * ANALOG_SCALE);
            } else {
                if (analogMax > analogMin) {
                    signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemHeatingLoopOp / 100)));
                } else {
                    signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemHeatingLoopOp / 100)));
                }
            }
            
        } else {
            signal = 0;
        }

        systemEquip.getHeatingSignal().writeHisVal(signal);
        Domain.cmBoardDevice.getAnalog3Out().writePointValue(signal);

        double analogFanSpeedMultiplier = systemEquip.getVavAnalogFanSpeedMultiplier().readPriorityVal();
        double epidemicMode = systemEquip.getEpidemicModeSystemState().readHisVal();
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
    
        if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            systemFanLoopOp = getSingleZoneFanLoopOp(analogFanSpeedMultiplier);
        } else if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && (L.ccu().oaoProfile != null)){
            double smartPurgeVAVFanLoopOp = L.ccu().oaoProfile.getOAOEquip().getSystemPurgeVavMinFanLoopOutput().readPriorityVal();
            double spSpMax = systemEquip.getStaticPressureSPMax().readPriorityVal();
            double spSpMin = systemEquip.getStaticPressureSPMin().readPriorityVal();
            double staticPressureLoopOutput = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax - spSpMin)) ;
            if((VavSystemController.getInstance().getSystemState() == COOLING) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
                if(staticPressureLoopOutput < ((spSpMax - spSpMin) * smartPurgeVAVFanLoopOp)){
                    systemFanLoopOp = ((spSpMax - spSpMin) * smartPurgeVAVFanLoopOp);
                }else {
                    systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
                }
            }else if(VavSystemController.getInstance().getSystemState() == HEATING) {
                systemFanLoopOp = Math.max((int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier), smartPurgeVAVFanLoopOp);
            }else {
                systemFanLoopOp = smartPurgeVAVFanLoopOp;
            }
        }else if ((VavSystemController.getInstance().getSystemState() == COOLING) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO))
        {
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

        if (systemEquip.getAnalog2OutputEnable().readPriorityVal() > 0)
        {
            analogMin = systemEquip.getAnalog2MinStaticPressure().readPriorityVal();
            analogMax = systemEquip.getAnalog2MaxStaticPressure().readPriorityVal();
    
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

        systemEquip.getFanSignal().writeHisVal(signal);
        Domain.cmBoardDevice.getAnalog2Out().writePointValue(signal);
        systemCo2LoopOp = 0;
        if (VavSystemController.getInstance().getSystemState() == COOLING) {
            systemCo2LoopOp = (SystemConstants.CO2_CONFIG_MAX - getSystemCO2()) * 100 / 200;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            double co2Val = VavSystemController.getInstance().getSystemCO2WA();
            if (co2Val > 0) {
                systemCo2LoopOp = (co2Val - SystemConstants.CO2_CONFIG_MIN) * 100 / 200;
            }
        }
        systemCo2LoopOp = Math.min(systemCo2LoopOp, 100);
        systemCo2LoopOp = Math.max(systemCo2LoopOp, 0);
        
        systemEquip.getCo2LoopOutput().writePointValue(systemCo2LoopOp);
        systemCo2LoopOp = systemEquip.getCo2LoopOutput().readHisVal();

        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp "+systemCoolingLoopOp+ " systemHeatingLoopOp "+ systemHeatingLoopOp
                                   + "systemFanLoopOp "+systemFanLoopOp+" systemCo2LoopOp "+systemCo2LoopOp);
        
        if (systemEquip.getAnalog4OutputEnable().readPriorityVal() > 0)
        {
            analogMin = systemEquip.getAnalog4MinOutsideDamper().readPriorityVal();
            analogMax = systemEquip.getAnalog4MaxOutsideDamper().readPriorityVal();
            CcuLog.d(L.TAG_CCU_SYSTEM,"analog4Min: "+analogMin+" analog4Max: "+analogMax+" CO2: "+getSystemCO2());
            if (analogMax > analogMin)
            {
                signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * systemCo2LoopOp/100));
            } else {
                signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * systemCo2LoopOp/100));
            }
        } else {
            signal = 0;
        }

        systemEquip.getOutsideAirDamper().writeHisVal(signal);
        Domain.cmBoardDevice.getAnalog4Out().writePointValue(signal);

        if (systemEquip.getRelay3OutputEnable().readPriorityVal() > 0)
        {
            signal = 0;
            if(systemMode != SystemMode.OFF  && (isSystemOccupied() || isReheatActive(CCUHsApi.getInstance())))
                signal = 1;
            // all the below cases are for what happens in Unoccupied Mode (run only if enabled by Epidemic Mode or an active heating/cooling loop)
            else if((VavSystemController.getInstance().getSystemState() == COOLING) && (systemCoolingLoopOp > 0 || systemFanLoopOp > 0) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO))
                signal = 1;
            else if((VavSystemController.getInstance().getSystemState() == HEATING) && (systemHeatingLoopOp > 0 || systemFanLoopOp > 0))
                signal = 1;
            else if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && (L.ccu().oaoProfile != null) && (systemFanLoopOp > 0)){
                signal = 1;
            }
        } else {
            signal = 0;
        }
        systemEquip.getFanEnable().writeHisVal(signal);
        Domain.cmBoardDevice.getRelay3().writePointValue(signal);
        
        if (systemEquip.getRelay7OutputEnable().readPriorityVal() > 0 && systemMode != SystemMode.OFF
                                                && isSystemOccupied())
        {
            double humidity = VavSystemController.getInstance().getAverageSystemHumidity();
            double targetMinHumidity = systemEquip.getSystemtargetMinInsideHumidity().readPriorityVal();
            double targetMaxHumidity =  systemEquip.getSystemtargetMaxInsideHumidity().readPriorityVal();

            boolean humidifier = systemEquip.getRelay7OutputAssociation().readPriorityVal() ==  0;
            double humidityHysteresis = systemEquip.getVavHumidityHysteresis().readPriorityVal();
    
            if (humidifier) {
                //Humidification
                int curSignal = (int)Domain.cmBoardDevice.getRelay7().readHisVal();
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
                int curSignal = (int)Domain.cmBoardDevice.getRelay7().readHisVal();
                if (humidity > targetMaxHumidity) {
                    signal = 1;
                } else if (humidity < (targetMaxHumidity - humidityHysteresis)) {
                    signal = 0;
                } else {
                    //Dehumidification
                    if (humidity > targetMaxHumidity) {
                        signal = 1;
                    } else if (humidity < (targetMaxHumidity - humidityHysteresis)) {
                        signal = 0;
                    } else {
                        signal = curSignal;
                    }
                    setCmdSignal("dehumidifier", signal);
                }
                systemEquip.getDehumidifier().writeHisVal(signal);
            }
            CcuLog.d(L.TAG_CCU_SYSTEM,"humidity :"+humidity+" targetMinHumidity: "+targetMinHumidity+" humidityHysteresis: "+humidityHysteresis+
                                      " targetMaxHumidity: "+targetMaxHumidity+" signal: "+signal*100);

            Domain.cmBoardDevice.getRelay7().writePointValue(signal);
        } else {
            systemEquip.getHumidifier().writeHisVal(0);
            systemEquip.getDehumidifier().writeHisVal(0);
            Domain.cmBoardDevice.getRelay7().writePointValue(0);
        }
    
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

    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        status.append((systemFanLoopOp > 0 || Domain.cmBoardDevice.getRelay3().readHisVal() > 0.01 ) ? " Fan ON ": "");
        status.append((systemCoolingLoopOp > 0 && !isCoolingLockoutActive())? " | Cooling ON ":"");
        status.append((systemHeatingLoopOp > 0 && !isHeatingLockoutActive())? " | Heating ON ":"");
        
        if (systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
            status.insert(0, "Free Cooling Used |");
        }

        String sts = systemEquip.getRelay7OutputEnable().readDefaultVal() == 0 ? "":
                (systemEquip.getRelay7OutputAssociation().readDefaultVal()) == 0 ?
                (systemEquip.getHumidifier().readHisVal() > 0 ? " | Humidifier ON " : " | Humidifier OFF ") :
                (systemEquip.getDehumidifier().readHisVal() > 0 ? " | Dehumidifier ON " : " | Dehumidifier OFF ");

        return status.toString().isEmpty() ? "System OFF" + sts :
                status + sts;
    }

    public void addSystemEquip() {
        systemEquip = (VavModulatingRtuSystemEquip) Domain.systemEquip;
        initTRSystem();
        updateSystemPoints();
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("system and equip and not modbus and not connectModule");
        if ((ProfileType.getProfileTypeForName(equip.get("profile").toString()).name()).equals(ProfileType.SYSTEM_VAV_ANALOG_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
        removeSystemEquipModbus();
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
    
    private void addConfigPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point analog1OutputEnabled = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"analog1OutputEnabled")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("config").addMarker("analog1")
                                      .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                      .setEnums("false,true").setTz(tz)
                                      .build();
        String analog1OutputEnabledId = hayStack.addPoint(analog1OutputEnabled);
        hayStack.writeDefaultValById(analog1OutputEnabledId, 0.0 );
    
        Point analog2OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"analog2OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("analog2")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String analog2OutputEnabledId = hayStack.addPoint(analog2OutputEnabled);
        hayStack.writeDefaultValById(analog2OutputEnabledId, 0.0 );
    
        Point analog3OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"analog3OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("analog3")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String analog3OutputEnabledId = hayStack.addPoint(analog3OutputEnabled);
        hayStack.writeDefaultValById(analog3OutputEnabledId, 0.0 );
    
        Point analog4OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"analog4OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("analog4")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String analog4OutputEnabledId = hayStack.addPoint(analog4OutputEnabled);
        hayStack.writeDefaultValById(analog4OutputEnabledId, 0.0 );
    
        Point relay3OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"relay3OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("relay3")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String relay3OutputEnabledId = hayStack.addPoint(relay3OutputEnabled);
        hayStack.writeDefaultValById(relay3OutputEnabledId, 0.0 );
    
        Point relay7OutputEnabled = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"relay7OutputEnabled")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("relay7")
                                            .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                            .setEnums("false,true").setTz(tz)
                                            .build();
        String relay7OutputEnabledId = hayStack.addPoint(relay7OutputEnabled);
        hayStack.writeDefaultValById(relay7OutputEnabledId, 0.0 );
    
        Point analog1AtMinCoolingSat = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"analog1AtMinCoolingSat")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("analog1")
                                             .addMarker("min").addMarker("cooling").addMarker("sat").addMarker("writable").addMarker("sp")
                                             .setUnit("V")
                                             .setTz(tz)
                                             .build();
        String analog1AtMinCoolingSatId = hayStack.addPoint(analog1AtMinCoolingSat);
        hayStack.writeDefaultValById(analog1AtMinCoolingSatId, 2.0 );
    
        Point analog1AtMaxCoolingSat = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog1AtMaxCoolingSat")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("max").addMarker("cooling").addMarker("sat").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog1AtMaxCoolingSatId = hayStack.addPoint(analog1AtMaxCoolingSat);
        hayStack.writeDefaultValById(analog1AtMaxCoolingSatId, 10.0 );
    
        Point analog2AtMinStaticPressure = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog2AtMinStaticPressure")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog2")
                                               .addMarker("min").addMarker("staticPressure").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog2AtMinStaticPressureId = hayStack.addPoint(analog2AtMinStaticPressure);
        hayStack.writeDefaultValById(analog2AtMinStaticPressureId, 2.0 );
    
        Point analog2AtMaxStaticPressure = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog2AtMaxStaticPressure")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog2")
                                               .addMarker("max").addMarker("staticPressure").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog2AtMaxStaticPressureId = hayStack.addPoint(analog2AtMaxStaticPressure);
        hayStack.writeDefaultValById(analog2AtMaxStaticPressureId, 10.0 );
    
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
    
        Point analog4AtMinCO2 = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog4AtMinCO2")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog4")
                                                   .addMarker("min").addMarker("co2").addMarker("writable").addMarker("sp")
                                                   .setUnit("V")
                                                   .setTz(tz)
                                                   .build();
        String analog4AtMinCO2Id = hayStack.addPoint(analog4AtMinCO2);
        hayStack.writeDefaultValById(analog4AtMinCO2Id, 2.0 );
    
        Point analog4AtMaxCO2 = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog4AtMaxCO2")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog4")
                                                   .addMarker("max").addMarker("co2").addMarker("writable").addMarker("sp")
                                                   .setUnit("V")
                                                   .setTz(tz)
                                                   .build();
        String analog4AtMaxCO2Id = hayStack.addPoint(analog4AtMaxCO2);
        hayStack.writeDefaultValById(analog4AtMaxCO2Id, 10.0 );
    
        Point humidifierType = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"humidifierType")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("system").addMarker("config").addMarker("relay7")
                                        .addMarker("humidifier").addMarker("type").addMarker("writable").addMarker("sp")
                                        .setTz(tz)
                                        .build();
        String humidifierTypeId = hayStack.addPoint(humidifierType);
        hayStack.writeDefaultValById(humidifierTypeId, 0.0 );
    
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
    public void setHumidifierConfigVal(String tags, double val) {
        double curHumidifierType = CCUHsApi.getInstance().readDefaultVal("point and system and config and "+tags);
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
        if(curHumidifierType != val) {
            if (val > 0) {//dehumidifier
                HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and humidifier");
                if(cmd != null && (cmd.size() > 0)) {
                    HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                    String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
                    Point cmdPoint = new Point.Builder().setHashMap(cmd).removeMarker("humidifier").removeMarker("runtime").addMarker("dehumidifier").setEnums("off,on").setDisplayName(equipDis + "-dehumidifier")
                            .setBacnetId(BacnetIdKt.DEHUMIDIFIERENABLEDID).build();
                    CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + cmdPoint.getDisplayName());
                    CCUHsApi.getInstance().deleteEntityTree(cmd.get("id").toString());
                    CCUHsApi.getInstance().addPoint(cmdPoint);
                    //CCUHsApi.getInstance().updatePoint(cmdPoint, cmdPoint.getId());
                    CCUHsApi.getInstance().scheduleSync();
                }
            } else {//humidifier
                HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and dehumidifier");
                if(cmd != null && cmd.size() > 0) {
                    HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                    String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
                    Point cmdPoint = new Point.Builder().setHashMap(cmd).removeMarker("dehumidifier").removeMarker("runtime").addMarker("humidifier").setEnums("off,on").setDisplayName(equipDis + "-humidifier")
                            .setBacnetId(BacnetIdKt.HUMIDIFIERENABLEDID).build();
                    CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + cmdPoint.getDisplayName());
                    CCUHsApi.getInstance().deleteEntityTree(cmd.get("id").toString());
                    CCUHsApi.getInstance().addPoint(cmdPoint);
                    CCUHsApi.getInstance().scheduleSync();
                }

            }
        }
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
    public void setConfigEnabled(String tags, double val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+tags);
        Point configEnabledPt = new Point.Builder().setHashMap(configPoint).build();
        double curConfig = hayStack.readPointPriorityVal(configEnabledPt.getId());
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and output and enabled and "+tags, val);
        if(curConfig != val){
            HashMap siteMap = hayStack.read(Tags.SITE);
            String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
            String siteRef = siteMap.get("id").toString();
            String tz = siteMap.get("tz").toString();
            switch (tags){
                case "analog1":
                    HashMap cmdCool = CCUHsApi.getInstance().read("point and system and cmd and cooling and modulating");
                    if(cmdCool != null && cmdCool.size() > 0) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmdCool.get("id").toString());
                        }
                    }else {
                        Point coolingSignal = new Point.Builder()
                                .setDisplayName(equipDis + "-" + "coolingSignal")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("modulating").addMarker("his").setUnit("%")
                                .setTz(tz).setBacnetId(BacnetIdKt.COOLINGSIGNALID).setBacnetType(BacnetUtilKt.ANALOG_VALUE)
                                .build();
                        String cmdCoolingPtId = CCUHsApi.getInstance().addPoint(coolingSignal);
                        CCUHsApi.getInstance().writeHisValById(cmdCoolingPtId,0.0);
                    }
                    break;
                case "analog2":
                    HashMap cmdFan = CCUHsApi.getInstance().read("point and system and cmd and fan and modulating");
                    if(cmdFan != null && cmdFan.size() > 0) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmdFan.get("id").toString());
                        }
                    }else {
                        Point fanSignal = new Point.Builder()
                                .setDisplayName(equipDis + "-" + "fanSignal")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("modulating").addMarker("his").setUnit("%")
                                .setTz(tz).setBacnetType(BacnetUtilKt.ANALOG_VALUE).setBacnetId(BacnetIdKt.FANSIGNALID)
                                .build();
                        String cmdFanSignalPtId = CCUHsApi.getInstance().addPoint(fanSignal);
                        CCUHsApi.getInstance().writeHisValById(cmdFanSignalPtId,0.0);
                    }
                    break;
                case "analog3":
                    HashMap cmdHeat = CCUHsApi.getInstance().read("point and system and cmd and heating and modulating");
                    if(cmdHeat != null && cmdHeat.size() > 0) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmdHeat.get("id").toString());
                        }
                    }else {
                        Point heatSignal = new Point.Builder()
                                .setDisplayName(equipDis + "-" + "heatingSignal")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("modulating").addMarker("his").setUnit("%")
                                .setTz(tz).setBacnetId(BacnetIdKt.HEATINGSIGNALID).setBacnetType(BacnetUtilKt.ANALOG_VALUE)
                                .build();
                        String cmdHeatingPtId = CCUHsApi.getInstance().addPoint(heatSignal);
                        CCUHsApi.getInstance().writeHisValById(cmdHeatingPtId,0.0);
                    }
                    break;
                case "analog4":
                    HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and co2 and modulating");
                    if(cmd != null && cmd.size() > 0) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmd.get("id").toString());
                        }
                    }else {
                        Point co2Signal = new Point.Builder()
                                .setDisplayName(equipDis + "-" + "co2Signal")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("co2").addMarker("modulating").addMarker("his").setUnit("%")
                                .setTz(tz).setBacnetId(BacnetIdKt.CO2SIGNALID).setBacnetType(BacnetUtilKt.ANALOG_VALUE)
                                .build();
                        String cmdCo2PtId = CCUHsApi.getInstance().addPoint(co2Signal);
                        CCUHsApi.getInstance().writeHisValById(cmdCo2PtId,0.0);
                    }
                    break;
                case "relay3":
                    HashMap cmdOccu = CCUHsApi.getInstance().read("point and system and cmd and occupancy");
                    if(cmdOccu != null && cmdOccu.size() > 0) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmdOccu.get("id").toString());
                        }
                    }else {
                        Point occupancySignal = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"occupancySignal")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("occupancy").addMarker("his").addMarker("runtime")
                                .setTz(tz)
                                .build();
                        String cmdOccPtId = CCUHsApi.getInstance().addPoint(occupancySignal);
                        CCUHsApi.getInstance().writeHisValById(cmdOccPtId,0.0);
                    }
                    break;
                case "relay7":


                    double curHumidifierType = CCUHsApi.getInstance().readDefaultVal("point and system and config and relay7 and humidifier and type");
                    if(curHumidifierType == 0.0) {
                        HashMap cmdHumid = CCUHsApi.getInstance().read("point and system and cmd and humidifier");
                        if (cmdHumid != null && cmdHumid.size() > 0) {
                            if (val == 0.0) {
                                CCUHsApi.getInstance().deleteEntityTree(cmdHumid.get("id").toString());
                            }
                        } else {
                            Point humidPt = new Point.Builder()
                                    .setDisplayName(equipDis + "-" + "humidifier")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                    .addMarker("system").addMarker("cmd").addMarker("humidifier").addMarker("his")
                                    .setEnums("off,on")
                                    .setTz(tz).setBacnetId(BacnetIdKt.HUMIDIFIERENABLEDID).setBacnetType(BacnetUtilKt.BINARY_VALUE)
                                    .build();
                            String cmdHumidPtId = CCUHsApi.getInstance().addPoint(humidPt);
                            CCUHsApi.getInstance().writeHisValById(cmdHumidPtId,0.0);
                        }
                    }else {

                        HashMap cmdDeHumid = CCUHsApi.getInstance().read("point and system and cmd and dehumidifier");
                        if (cmdDeHumid != null && cmdDeHumid.size() > 0) {
                            if (val == 0.0) {
                                CCUHsApi.getInstance().deleteEntityTree(cmdDeHumid.get("id").toString());
                            }
                        } else {
                            Point dehumidPt = new Point.Builder()
                                    .setDisplayName(equipDis + "-" + "dehumidifier")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                    .addMarker("system").addMarker("cmd").addMarker("dehumidifier").addMarker("his").addMarker("runtime")
                                    .setTz(tz).setBacnetId(BacnetIdKt.DEHUMIDIFIERENABLEDID).setBacnetType(BacnetUtilKt.BINARY_VALUE)
                                    .build();
                            String cmdDehumidPtId = CCUHsApi.getInstance().addPoint(dehumidPt);
                            CCUHsApi.getInstance().writeHisValById(cmdDehumidPtId,0.0);
                        }
                    }
                    break;
            }

            CCUHsApi.getInstance().syncEntityTree();
        }
        setSystemModeForVav(CCUHsApi.getInstance());
    }
    
    private void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
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
}
