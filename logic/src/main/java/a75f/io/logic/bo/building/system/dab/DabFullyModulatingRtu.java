package a75f.io.logic.bo.building.system.dab;

import android.content.Intent;
import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;
import static a75f.io.logic.bo.building.system.dab.DcwbProfileUtil.createAnalog4LoopConfigPoints;
import static a75f.io.logic.bo.building.system.dab.DcwbProfileUtil.createChilledWaterConfigPoints;
import static a75f.io.logic.bo.building.system.dab.DcwbProfileUtil.createConfigPoints;
import static a75f.io.logic.bo.building.system.dab.DcwbProfileUtil.createLoopPoints;
import static a75f.io.logic.bo.building.system.dab.DcwbProfileUtil.deleteAnalog4LoopConfigPoints;
import static a75f.io.logic.bo.building.system.dab.DcwbProfileUtil.deleteConfigPoints;
import static a75f.io.logic.bo.building.system.dab.DcwbProfileUtil.deleteLoopOutputPoints;
import static a75f.io.logic.jobs.ScheduleProcessJob.ACTION_STATUS_CHANGE;

public class DabFullyModulatingRtu extends DabSystemProfile
{
    private static final int ANALOG_SCALE = 10;
    
    public int getAnalog1Out() {
        return (int) ControlMote.getAnalogOut("analog1");
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
        return "DAB Fully Modulating AHU";
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
            return getConfigVal("analog4 and output and enabled") > 0;
        } else {
            return getConfigVal("analog1 and output and enabled") > 0;
        }
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (getConfigVal("analog3 and output and enabled") > 0);
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
        
        updateOutsideWeatherParams();
        
        DabSystemController dabSystem = DabSystemController.getInstance();
        
        if (isDcwbEnabled()) {
            
            if (dcwbAlgoHandler == null) {
                boolean isAdaptiveDelta = getConfigVal("adaptive and delta") > 0;
                dcwbAlgoHandler = new DcwbAlgoHandler(isAdaptiveDelta, getSystemEquipRef(), CCUHsApi.getInstance());
            }
    
            //Analog1 controls water valve when the DCWB enabled.
            updateAnalog1DcwbOutput(dabSystem);
    
            //Could be mapped to cooling or co2 based on configuration.
            updateAnalog4Output(dabSystem);
            
        } else {
            //Analog1 controls cooling when the DCWB is disabled
            updateAnalog1DabOutput(dabSystem);
        }
        
        //Analog2 controls Central Fan
        updateAnalog2Output(dabSystem);
        
        //Analog3 controls heating.
        updateAnalog3Output(dabSystem);
        
        updateRelayOutputs(dabSystem);
        
        setSystemPoint("operating and mode", dabSystem.systemState.ordinal());
        String systemStatus = (dabSystem.systemState == OFF) ? "System OFF " : getStatusMessage();
        Log.i("TAG", "getSystemStatusString: oam");
        String scheduleStatus = ScheduleProcessJob.getSystemStatusString();
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemStatusMessage: "+systemStatus);
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: " +scheduleStatus);
        
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and status and message").equals(systemStatus)) {
            CCUHsApi.getInstance().writeDefaultVal("system and status and message", systemStatus);
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and scheduleStatus").equals(scheduleStatus)) {
            CCUHsApi.getInstance().writeDefaultVal("system and scheduleStatus", scheduleStatus);
        }
    }
    
    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        status.append((systemFanLoopOp > 0 || getCmdSignal("occupancy") > 0) ? " Fan ON ":"");
        status.append(systemCoolingLoopOp > 0 ? " | Cooling ON ":"");
        status.append(systemHeatingLoopOp > 0 ? " | Heating ON ":"");
    
        if (systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
            status.insert(0, "Free Cooling Used |");
        }
        if(status.toString().isEmpty())
            status.append("System OFF");
        return status.toString();
    }
    
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_DAB_ANALOG_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                addNewSystemUserIntentPoints(equip.get("id").toString());
                addNewTunerPoints(equip.get("id").toString());
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
                                   .setProfile(ProfileType.SYSTEM_DAB_ANALOG_RTU.name())
                                   .addMarker("equip").addMarker("system").addMarker("dab")
                                   .setTz(siteMap.get("tz").toString())
                                   .build();
        String equipRef = hayStack.addEquip(systemEquip);
        addSystemLoopOpPoints(equipRef);
        addUserIntentPoints(equipRef);
        addCmdPoints(equipRef);
        addConfigPoints(equipRef);
        addDabSystemTuners(equipRef);
        updateAhuRef(equipRef);
        //sysEquip = new SystemEquip(equipRef);
        new ControlMote(equipRef);
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
        
        
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_ANALOG_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
    
    private void addCmdPoints(String equipref) {
        /*HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        Point coolingSignal = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"coolingSignal")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("modulating").addMarker("logical").addMarker("his").addMarker("runtime")
                                      .setUnit("%").setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(coolingSignal);
        
        Point heatingSignal = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"heatingSignal")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("modulating").addMarker("logical").addMarker("his").addMarker("runtime")
                                      .setUnit("%").setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(heatingSignal);
        
        Point fanSignal = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"fanSignal")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .setUnit("%").addMarker("system").addMarker("cmd").addMarker("fan").addMarker("modulating").addMarker("logical").addMarker("his").addMarker("runtime")
                                  .setTz(tz)
                                  .build();
        CCUHsApi.getInstance().addPoint(fanSignal);
        
        Point occupancySignal = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"occupancySignal")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("system").addMarker("cmd").addMarker("occupancy").addMarker("his").addMarker("runtime")
                                        .setTz(tz)
                                        .build();
        CCUHsApi.getInstance().addPoint(occupancySignal);
        Point humidifierSignal = new Point.Builder()
                                         .setDisplayName(equipDis+"-humidifier")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("system").addMarker("cmd").addMarker("humidifier").addMarker("his").addMarker("runtime")
                                         .setTz(tz)
                                         .build();
        CCUHsApi.getInstance().addPoint(humidifierSignal);*/
        
        /*Point dehumidifierSignal = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"dehumidifierSignal")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipref)
                                           .addMarker("system").addMarker("cmd").addMarker("dehumidifier").addMarker("his")
                                           .setUnit("%").setTz(tz)
                                           .build();
        CCUHsApi.getInstance().addPoint(dehumidifierSignal);*/
    }
    
    private void updateAnalog1DabOutput(DabSystemController dabSystem) {
        
        double signal = 0;
        if (dabSystem.getSystemState() == COOLING) {
            systemCoolingLoopOp = dabSystem.getCoolingSignal();
        } else {
            systemCoolingLoopOp = 0;
        }
        setSystemLoopOp("cooling", systemCoolingLoopOp);
    
        if (getConfigVal("analog1 and output and enabled") > 0) {
            double analogMin = getConfigVal("analog1 and cooling and min");
            double analogMax = getConfigVal("analog1 and cooling and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: "+analogMin+" analog1Max: "+analogMax+" systemCoolingLoop : "+systemCoolingLoopOp);
            signal = getModulatedAnalogVal(analogMin, analogMax, systemCoolingLoopOp);
        }
        if (signal != getCmdSignal("cooling")) {
            setCmdSignal("cooling", signal);
        }
        ControlMote.setAnalogOut("analog1", signal);
    }
    
    private void updateAnalog1DcwbOutput(DabSystemController dabSystem) {
        
        boolean isAnalog1Enabled = getConfigVal("analog1 and output and enabled") > 0;
    
        if (isAnalog1Enabled && dabSystem.getSystemState() == COOLING) {
            dcwbAlgoHandler.runLoopAlgorithm();
            systemDCWBValveLoopOutput = CCUUtils.roundToTwoDecimal(dcwbAlgoHandler.getChilledWaterValveLoopOutput());
        } else {
            systemDCWBValveLoopOutput = 0;
            dcwbAlgoHandler.resetChilledWaterValveLoop();
        }
    
        double signal = 0;
    
        if (isAnalog1Enabled) {
            double analogMin = getConfigVal("analog1 and min");
            double analogMax = getConfigVal("analog1 and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: "+analogMin+" analog1Max: "+analogMax+" systemDCWBValveLoopOutput : "+systemDCWBValveLoopOutput);
            signal = getModulatedAnalogVal(analogMin, analogMax, systemDCWBValveLoopOutput);
        }
    
        setSystemLoopOp("valve", systemDCWBValveLoopOutput);
    
        if (signal != getCmdSignal("valve")) {
            setCmdSignal("valve", signal);
        }
        ControlMote.setAnalogOut("analog1", signal);
        
        setSystemPoint("dcwb and exit and temp and target",
                       dcwbAlgoHandler.getChilledWaterTargetExitTemperature());
    }
    
    private void updateAnalog2Output(DabSystemController dabSystem) {
        updateFanLoop(dabSystem);
    
        double signal = 0;
        if (getConfigVal("analog2 and output and enabled") > 0) {
            double analogMin = getConfigVal("analog2 and fan and min");
            double analogMax = getConfigVal("analog2 and fan and max");
        
            CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemFanLoopOp: "+systemFanLoopOp);
    
            signal = getModulatedAnalogVal(analogMin, analogMax, systemFanLoopOp);
        
        }
        
        if (signal != getCmdSignal("fan")) {
            setCmdSignal("fan", signal);
        }
        ControlMote.setAnalogOut("analog2", signal);
        
    }
    
    private void updateAnalog3Output(DabSystemController dabSystem) {
        double signal = 0;
        if (dabSystem.getSystemState() == HEATING) {
            systemHeatingLoopOp = dabSystem.getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
        setSystemLoopOp("heating", systemHeatingLoopOp);
    
        if (getConfigVal("analog3 and output and enabled") > 0) {
            double analogMin = getConfigVal("analog3 and heating and min");
            double analogMax = getConfigVal("analog3 and heating and max");
        
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog3Min: "+analogMin+" analog3Max: "+analogMax+" systemHeatingLoop : "+systemHeatingLoopOp);
            signal = getModulatedAnalogVal(analogMin, analogMax, systemHeatingLoopOp);
        }
        
        if (signal != getCmdSignal("heating")) {
            setCmdSignal("heating", signal);
        }
        ControlMote.setAnalogOut("analog3", signal);
    }
    
    private void updateAnalog4Output(DabSystemController dabSystem) {
        double loopType = CCUHsApi.getInstance().readDefaultVal("analog4 and loop and output and type");
        String loopTag = loopType == 0 ? Tags.COOLING : Tags.CO2;
        
        double signal = 0;
        if (dabSystem.getSystemState() == COOLING) {
            systemCoolingLoopOp = dabSystem.getCoolingSignal();
        } else {
            systemCoolingLoopOp = 0;
        }
        setSystemLoopOp("cooling", systemCoolingLoopOp);
    
        systemCo2LoopOp = getCo2LoopOp();
        setSystemLoopOp("co2", systemCo2LoopOp);
    
        if (getConfigVal("analog4 and output and enabled") > 0) {
            
            double analogMin = getConfigVal("analog4 and  min and "+loopTag);
            double analogMax = getConfigVal("analog4 and  max and "+loopTag);
        
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog4Min: "+analogMin+" analog4Max: "+analogMax+
                                       " systemCoolingLoopOp : "+systemCoolingLoopOp + " systemCo2LoopOp : "+systemCo2LoopOp);
            signal = getModulatedAnalogVal(analogMin, analogMax, loopType == 0 ? systemCoolingLoopOp : systemCo2LoopOp);
        }
    
        if (signal != getCmdSignal(loopTag)) {
            setCmdSignal(loopTag, signal);
        }
        ControlMote.setAnalogOut("analog4", signal);
    }
    
    private void updateRelayOutputs(DabSystemController dabSystem) {
        
        double signal = 0;
        SystemMode systemMode = SystemMode.values()[(int)getUserIntentVal("conditioning and mode")];
        if (getConfigVal("relay3 and output and enabled") > 0 && systemMode != SystemMode.OFF) {
            signal = (isSystemOccupied() || systemFanLoopOp > 0) ? 1 : 0;
        
        }
        if(signal != getCmdSignal("occupancy")) {
            setCmdSignal("occupancy", signal);
        }
        ControlMote.setRelayState("relay3", signal);
    
        if (getConfigVal("relay7 and output and enabled") > 0 && systemMode != SystemMode.OFF
                    && isSystemOccupied()) {
            
            double humidity = dabSystem.getAverageSystemHumidity();
            double targetMinHumidity = TunerUtil.readSystemUserIntentVal("target and min and inside and humidity");
            double targetMaxHumidity = TunerUtil.readSystemUserIntentVal("target and max and inside and humidity");
        
            boolean humidifier = getConfigVal("humidifier and type") == 0;
        
            double humidityHysteresis = TunerUtil.readTunerValByQuery("humidity and hysteresis", getSystemEquipRef());
            int curSignal = (int)ControlMote.getRelayState("relay7");
            if (humidifier) {
                //Humidification
                if (humidity < targetMinHumidity) {
                    signal = 1;
                } else if (humidity > (targetMinHumidity + humidityHysteresis)) {
                    signal = 0;
                } else {
                    signal = curSignal;
                }
                if(signal != getCmdSignal("humidifier"))
                    setCmdSignal("humidifier",signal);
            } else {
                //Dehumidification
                if (humidity > targetMaxHumidity) {
                    signal = 1;
                } else if (humidity < (targetMaxHumidity - humidityHysteresis)) {
                    signal = 0;
                } else {
                    signal = curSignal;
                }
                if(signal != getCmdSignal("dehumidifier"))
                    setCmdSignal("dehumidifier",signal);
            }
            CcuLog.d(L.TAG_CCU_SYSTEM,"humidity :"+humidity+" targetMinHumidity: "+targetMinHumidity+" humidityHysteresis: "+humidityHysteresis+
                                      " targetMaxHumidity: "+targetMaxHumidity+" signal: "+signal*100);
        
            ControlMote.setRelayState("relay7", signal);
        } else {
            setCmdSignal("humidifier",0);
            setCmdSignal("dehumidifier",0);
            ControlMote.setRelayState("relay7", 0);
        }
    
    }
    
    private void updateFanLoop(DabSystemController dabSystem) {
        
        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", getSystemEquipRef());
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        
        if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && (L.ccu().oaoProfile != null)){
            
            double smartPurgeDabFanLoopOp = TunerUtil.readTunerValByQuery("system and purge and dab and fan and loop and output", L.ccu().oaoProfile.getEquipRef());
            if(L.ccu().oaoProfile.isEconomizingAvailable()) {
                double economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery("oao and economizing and main and cooling and loop and map",
                                                                                       L.ccu().oaoProfile.getEquipRef());
                systemFanLoopOp = Math.max(Math.max(systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap, systemHeatingLoopOp), smartPurgeDabFanLoopOp);
            }else if(dabSystem.getSystemState() == COOLING){
                systemFanLoopOp = Math.max(systemCoolingLoopOp * analogFanSpeedMultiplier, smartPurgeDabFanLoopOp);
            }else if(dabSystem.getSystemState() == HEATING){
                systemFanLoopOp = Math.max(systemHeatingLoopOp * analogFanSpeedMultiplier, smartPurgeDabFanLoopOp);
            }else
                systemFanLoopOp = smartPurgeDabFanLoopOp;
            
        } else if (dabSystem.getSystemState() == COOLING) {
            //When the system is economizing we need to ramp up the fan faster to take advantage of the free cooling. In such a case
            //systemFanLoopOutput = systemCoolingLoopOutput * 100/economizingToMainCoolingLoopMap
            if (L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
                double economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery("oao and economizing and main and cooling and loop and map",
                                                                                       L.ccu().oaoProfile.getEquipRef());
            
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
        setSystemLoopOp("fan", systemFanLoopOp);
    
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
    
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        try {
            CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and " + cmd, val);
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
                                               .setDisplayName(equipDis+"-"+"analog1AtMinCooling")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("min").addMarker("cooling").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog1AtMinCoolingSatId = hayStack.addPoint(analog1AtMinCoolingSat);
        hayStack.writeDefaultValById(analog1AtMinCoolingSatId, 2.0 );
        
        Point analog1AtMaxCoolingSat = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog1AtMaxCooling")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("max").addMarker("cooling").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog1AtMaxCoolingSatId = hayStack.addPoint(analog1AtMaxCoolingSat);
        hayStack.writeDefaultValById(analog1AtMaxCoolingSatId, 10.0 );
        
        Point analog2AtMinStaticPressure = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog2AtMinFan")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("min").addMarker("fan").addMarker("writable").addMarker("sp")
                                                   .setUnit("V")
                                                   .setTz(tz)
                                                   .build();
        String analog2AtMinStaticPressureId = hayStack.addPoint(analog2AtMinStaticPressure);
        hayStack.writeDefaultValById(analog2AtMinStaticPressureId, 2.0 );
        
        Point analog2AtMaxStaticPressure = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog2AtMaxFan")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("max").addMarker("fan").addMarker("writable").addMarker("sp")
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
        
        Point humidifierType = new Point.Builder()
                                       .setDisplayName(equipDis+"-"+"humidifierType")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .addMarker("system").addMarker("config").addMarker("relay7")
                                       .addMarker("humidifier").addMarker("type").addMarker("writable").addMarker("sp")
                                       .setUnit("V")
                                       .setTz(tz)
                                       .build();
        String humidifierTypeId = hayStack.addPoint(humidifierType);
        hayStack.writeDefaultValById(humidifierTypeId, 0.0 );
    
        createDcwbEnabledPoint(hayStack, siteRef, equipDis, equipref, tz);
        
    }
    
    private void createDcwbEnabledPoint(CCUHsApi hayStack, String siteRef, String equipDis, String equipRef,
                                        String tz) {
        Point dcwbEnabled = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"dcwbEnabled")
                                .setSiteRef(siteRef)
                                .setEquipRef(equipRef)
                                .addMarker("system").addMarker("config").addMarker("dcwb")
                                .addMarker("enabled").addMarker("writable").addMarker("sp")
                                .setEnums("false,true").setTz(tz)
                                .build();
        String dcwbEnabledId = hayStack.addPoint(dcwbEnabled);
        hayStack.writeDefaultValById(dcwbEnabledId, 0.0 );
    }
    
    public double getConfigVal(String tags) {
        //return CCUHsApi.getInstance().readDefaultVal("point and system and config and "+tags);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and "+tags);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }
    
    public void setConfigVal(String tags, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
    }
    
    public void setHumidifierConfigVal(String tags, double val) {
        double curHumidifierType = CCUHsApi.getInstance().readDefaultVal("point and system and config and "+tags);
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
        if(curHumidifierType != val) {
            //CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
            if (val > 0) {//dehumidifier
                HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and humidifier");
                if(cmd != null &&(cmd.size() > 0)) {
                    HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                    String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
                    Point cmdPoint = new Point.Builder().setHashMap(cmd).removeMarker("humidifier").removeMarker("runtime").addMarker("dehumidifier").setEnums("off,on").setDisplayName(equipDis + "-dehumidifier").build();
                    CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + cmdPoint.getDisplayName()+","+cmdPoint.getMarkers().toString()+","+cmd.get("id").toString()+","+cmdPoint.getId());
                    CCUHsApi.getInstance().deleteEntityTree(cmd.get("id").toString());
                    CCUHsApi.getInstance().addPoint(cmdPoint);
                    CCUHsApi.getInstance().scheduleSync();
                }


            } else {//humidifier
                HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and dehumidifier");
                if(cmd != null && (cmd.size() > 0)) {
                    HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                    String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
                    Point cmdPoint = new Point.Builder().setHashMap(cmd).removeMarker("dehumidifier").removeMarker("runtime").addMarker("humidifier").setEnums("off,on").setDisplayName(equipDis + "-humidifier").build();
                    CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point " + cmdPoint.getDisplayName()+","+cmdPoint.getMarkers().toString()+","+cmd.get("id").toString()+","+cmdPoint.getId());
                    CCUHsApi.getInstance().deleteEntityTree(cmd.get("id").toString());
                    CCUHsApi.getInstance().addPoint(cmdPoint);
                    CCUHsApi.getInstance().scheduleSync();
                }

            }
        }
    }
    public double getConfigEnabled(String config) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+config);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        
    }

    public void setConfigEnabled(String tags, double val) {
        CcuLog.i(L.TAG_CCU_SYSTEM, "setConfigEnabled "+tags+" val "+val);
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
                                .setTz(tz)
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
                                .setTz(tz)
                                .build();
                        String cmdFanPtId = CCUHsApi.getInstance().addPoint(fanSignal);
                        CCUHsApi.getInstance().writeHisValById(cmdFanPtId,0.0);
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
                                .setTz(tz)
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
                                .setTz(tz)
                                .build();
                        String co2SignalId = CCUHsApi.getInstance().addPoint(co2Signal);
                        CCUHsApi.getInstance().writeHisValById(co2SignalId,0.0);
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
                        String cmdOccupancyPtId = CCUHsApi.getInstance().addPoint(occupancySignal);
                        CCUHsApi.getInstance().writeHisValById(cmdOccupancyPtId,0.0);
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
                                    .setTz(tz)
                                    .build();
                            String cmdHumdityPtId = CCUHsApi.getInstance().addPoint(humidPt);
                            CCUHsApi.getInstance().writeHisValById(cmdHumdityPtId,0.0);
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
                                    .addMarker("system").addMarker("cmd").addMarker("dehumidifier").addMarker("his")
                                    .setEnums("off,on")
                                    .setTz(tz)
                                    .build();
                            String cmdDehumidPtId = CCUHsApi.getInstance().addPoint(dehumidPt);
                            CCUHsApi.getInstance().writeHisValById(cmdDehumidPtId,0.0);
                        }
                    }
                    break;
            }

            CCUHsApi.getInstance().scheduleSync();
        }
    }
    
    /**
     * DCWB when enabled takes control of analog1 and analog4. Other ports are still managed as DAB.
     * @param tags
     * @param val
     */
    public void setDcwbConfigEnabled(String tags, double val) {
        CcuLog.i(L.TAG_CCU_SYSTEM, "setDcwbConfigEnabled "+tags+" val "+val);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+tags);
        Point configEnabledPt = new Point.Builder().setHashMap(configPoint).build();
        double curConfig = hayStack.readPointPriorityVal(configEnabledPt.getId());
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and output and enabled and "+tags, val);
        
        if(curConfig != val){
            HashMap equipMap = hayStack.read("equip and system");
            Equip systemEquip = new Equip.Builder().setHashMap(equipMap).build();
            switch (tags){
                case Tags.ANALOG1:
                    HashMap cmdValve = CCUHsApi.getInstance().read("point and system and cmd and valve");
                    if(!cmdValve.isEmpty()) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmdValve.get("id").toString());
                        }
                    }else {
                        Point valveSignal = new Point.Builder()
                                                  .setDisplayName(systemEquip.getDisplayName() + "-" + "chwValveSignal")
                                                  .setSiteRef(systemEquip.getSiteRef())
                                                  .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                                  .addMarker("system").addMarker("cmd").addMarker("valve")
                                                  .addMarker("chilled").addMarker("water").addMarker("his").setUnit("%")
                                                  .setTz(systemEquip.getTz())
                                                  .build();
                        String cmdCoolingPtId = CCUHsApi.getInstance().addPoint(valveSignal);
                        CCUHsApi.getInstance().writeHisValById(cmdCoolingPtId,0.0);
                    }
                    break;
    
                case Tags.ANALOG4:
                    double loopType = hayStack.readDefaultVal("analog4 and loop and output and type");
                    String loopTag = loopType == 0 ? Tags.COOLING : Tags.CO2;
                    HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and "+loopTag);
                    
                    if(!cmd.isEmpty()) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmd.get("id").toString());
                        }
                    }else {
                        Point loopSignal = new Point.Builder()
                                              .setDisplayName(systemEquip.getDisplayName() + "-" + loopTag+"Signal")
                                              .setSiteRef(systemEquip.getSiteRef())
                                              .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                              .addMarker("system").addMarker("cmd").addMarker(loopTag).addMarker("his").setUnit("%")
                                              .setTz(systemEquip.getTz())
                                              .build();
                        String loopSignalId = CCUHsApi.getInstance().addPoint(loopSignal);
                        CCUHsApi.getInstance().writeHisValById(loopSignalId,0.0);
                    }
                    break;
            }
        }
    }
    
    /**
     * Enable (Dynamic Chilled Water Balancing) DCWB extension to the DAB profile.
     * Creates all the necessary config/loop/output points for DCWB.
     */
    public void enableDcwb(CCUHsApi hayStack) {
        //Remove analog1 Command points
        setConfigEnabled(Tags.ANALOG1, 0);
        HashMap equipMap = hayStack.read("equip and system");
        Equip systemEquip = new Equip.Builder().setHashMap(equipMap).build();
        createConfigPoints(systemEquip, hayStack);
        createAnalog4LoopConfigPoints(Tags.COOLING,systemEquip, hayStack);
        createChilledWaterConfigPoints(systemEquip, hayStack);
        createLoopPoints(systemEquip, hayStack);
    
        //Needed for upgrades
        HashMap configPoint = hayStack.read("point and system and config and dcwb and enabled");
        if (configPoint.isEmpty()) {
            createDcwbEnabledPoint(hayStack, systemEquip.getSiteRef(), systemEquip.getDisplayName(),
                                   systemEquip.getId(), systemEquip.getTz());
        }
        
        setConfigVal("dcwb and enabled",1);
        CCUHsApi.getInstance().scheduleSync();
    }
    
    /**
     * Disable DCWB extension to the DAB profile.
     * Deletes all the necessary config/loop/output points for DCWB.
     */
    public void disableDcwb(CCUHsApi hayStack) {
        setConfigVal("dcwb and enabled",0);
        setDcwbConfigEnabled(Tags.ANALOG1, 0);
        setDcwbConfigEnabled(Tags.ANALOG4, 0);
        deleteConfigPoints(hayStack);
        deleteLoopOutputPoints(hayStack);
        double loopType = hayStack.readDefaultVal("analog4 and loop and output and type");
        deleteAnalog4LoopConfigPoints(loopType == 0 ? Tags.COOLING : Tags.CO2, hayStack);
        CCUHsApi.getInstance().scheduleSync();
    }
    
    /**
     * Analog4 could be switched between CoolingLoop and CO2Loop.
     * This method takes care of managing haystack entities when there is a change.
     */
    public void updateDcwbAnalog4Mapping(double newLoopType) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        double loopType = hayStack.readDefaultVal("analog4 and loop and output and type");
        if (loopType == newLoopType) {
            return;
        }
        CcuLog.i(L.TAG_CCU_SYSTEM, "updateDcwbAnalog4Mapping "+loopType+" -> "+newLoopType);
        deleteAnalog4LoopConfigPoints(loopType == 0 ? Tags.COOLING : Tags.CO2, hayStack);
        HashMap equipMap = hayStack.read("equip and system");
        Equip systemEquip = new Equip.Builder().setHashMap(equipMap).build();
        createAnalog4LoopConfigPoints(newLoopType == 0 ? Tags.COOLING : Tags.CO2, systemEquip, hayStack);
        hayStack.writeDefaultVal("analog4 and loop and output and type", newLoopType);
        CCUHsApi.getInstance().scheduleSync();
    }
    
    /**
     * Used to reinitialize the algorithm loop variables when config changes.
     */
    public void invalidateAlgorithmLoop() {
        dcwbAlgoHandler = null;
    }
    
    public boolean isDcwbEnabled() {
        return CCUHsApi.getInstance().readDefaultVal("system and dcwb and enabled") > 0;
    }
}
