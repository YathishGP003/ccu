package a75f.io.logic.bo.building.system.vav;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import android.content.Intent;

import java.util.HashMap;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Schedule;
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

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.jobs.ScheduleProcessJob.ACTION_STATUS_CHANGE;

/**
 * Default System handles PI controlled op
 */
public class VavFullyModulatingRtu extends VavSystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
    private static final int ANALOG_SCALE = 10;
    
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
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return (getConfigVal("analog1 and output and enabled") > 0);
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
    
    private synchronized void updateSystemPoints() {
        updateOutsideWeatherParams();
        SystemMode systemMode = SystemMode.values()[(int)getUserIntentVal("conditioning and mode")];
        if (VavSystemController.getInstance().getSystemState() == COOLING && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO))
        {
            double satSpMax = VavTRTuners.getSatTRTunerVal("spmax");
            double satSpMin = VavTRTuners.getSatTRTunerVal("spmin");
            CcuLog.d(L.TAG_CCU_SYSTEM, "satSpMax :" + satSpMax + " satSpMin: " + satSpMin + " SAT: " + getSystemSAT());
            systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;
        } else {
            systemCoolingLoopOp = 0;
        }
        
        int signal;
        double analogMin, analogMax;
        setSystemLoopOp("cooling", systemCoolingLoopOp);
        if (getConfigVal("analog1 and output and enabled") > 0)
        {
            analogMin = getConfigVal("analog1 and cooling and sat and min");
            analogMax = getConfigVal("analog1 and cooling and sat and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: "+analogMin+" analog1Max: "+analogMax+" SAT: "+getSystemSAT());
    
            if (analogMax > analogMin)
            {
                signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemCoolingLoopOp/100)));
            }
            else
            {
                signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemCoolingLoopOp/100)));
            }
        } else {
            signal = 0;
        }
        
        if (signal != getCmdSignal("cooling")) {
            setCmdSignal("cooling", signal);
        }
        ControlMote.setAnalogOut("analog1", signal);
        
    
        if (VavSystemController.getInstance().getSystemState() == HEATING)
        {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
        
        setSystemLoopOp("heating", systemHeatingLoopOp);
        if (getConfigVal("analog3 and output and enabled") > 0)
        {
            analogMin = getConfigVal("analog3 and heating and min");
            analogMax = getConfigVal("analog3 and heating and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog3Min: "+analogMin+" analog3Max: "+analogMax+" HeatingSignal : "+VavSystemController.getInstance().getHeatingSignal());
            if (analogMax > analogMin)
            {
                signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemHeatingLoopOp / 100)));
            }
            else
            {
                signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemHeatingLoopOp / 100)));
            }
            
        } else {
            signal = 0;
        }
        
        if (signal != getCmdSignal("heating")) {
            setCmdSignal("heating", signal);
        }
        ControlMote.setAnalogOut("analog3", signal);
        
        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", getSystemEquipRef());
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && (L.ccu().oaoProfile != null)){
            double smartPurgeVAVFanLoopOp = TunerUtil.readTunerValByQuery("system and purge and vav and fan and loop and output", L.ccu().oaoProfile.getEquipRef());
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");
            double staticPressureLoopOutput = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
            if((VavSystemController.getInstance().getSystemState() == COOLING) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
                if(staticPressureLoopOutput < ((spSpMax - spSpMin) * smartPurgeVAVFanLoopOp))
                    systemFanLoopOp = ((spSpMax - spSpMin) * smartPurgeVAVFanLoopOp);
                else systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
            }else if(VavSystemController.getInstance().getSystemState() == HEATING)
                systemFanLoopOp = Math.max((int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier),smartPurgeVAVFanLoopOp);
            else
                systemFanLoopOp = smartPurgeVAVFanLoopOp;
        }else if ((VavSystemController.getInstance().getSystemState() == COOLING) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO))
        {
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");
    
            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure());
            systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
        systemFanLoopOp = Math.min(systemFanLoopOp, 100);
        setSystemLoopOp("fan", systemFanLoopOp);
        
        if (getConfigVal("analog2 and output and enabled") > 0)
        {
            analogMin = getConfigVal("analog2 and staticPressure and min");
            analogMax = getConfigVal("analog2 and staticPressure and max");
    
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
        
        if (signal != getCmdSignal("fan")) {
            setCmdSignal("fan", signal);
        }
        ControlMote.setAnalogOut("analog2", signal);
        
        systemCo2LoopOp = VavSystemController.getInstance().getSystemState() == SystemController.State.OFF
                                         ? 0 :(SystemConstants.CO2_CONFIG_MAX - getSystemCO2()) * 100 / 200 ;
        setSystemLoopOp("co2", systemCo2LoopOp);
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp "+systemCoolingLoopOp+ " systemHeatingLoopOp "+ systemHeatingLoopOp
                                   + "systemFanLoopOp "+systemFanLoopOp+" systemCo2LoopOp "+systemCo2LoopOp);
        
        if (getConfigVal("analog4 and output and enabled") > 0)
        {
            analogMin = getConfigVal("analog4 and co2 and min");
            analogMax = getConfigVal("analog4 and co2 and max");
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
        
        if (signal != getCmdSignal("co2")) {
            setCmdSignal("co2", signal);
        }
        ControlMote.setAnalogOut("analog4", signal);
        
        if (getConfigVal("relay3 and output and enabled") > 0)
        {
            double systemStaticPressureOoutput = getStaticPressure() - SystemConstants.SP_CONFIG_MIN;
            signal = 0;
            if((systemMode != SystemMode.OFF ) && (ScheduleProcessJob.getSystemOccupancy() != Occupancy.UNOCCUPIED && ScheduleProcessJob.getSystemOccupancy() != Occupancy.VACATION))
                signal = 1;
            else if((VavSystemController.getInstance().getSystemState() == COOLING) && (systemStaticPressureOoutput > 0) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO))
                signal = 1;
            else if((VavSystemController.getInstance().getSystemState() == HEATING) && (systemFanLoopOp > 0))
                signal = 1;
            else if((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && (L.ccu().oaoProfile != null) && (systemFanLoopOp > 0)){
                signal = 1;
            }
        } else {
            signal = 0;
        }
    
        if(signal != getCmdSignal("occupancy")) {
            setCmdSignal("occupancy", signal);
        }
        ControlMote.setRelayState("relay3", signal );
        
        if (getConfigVal("relay7 and output and enabled") > 0 && systemMode != SystemMode.OFF
                                                && ScheduleProcessJob.getSystemOccupancy() != Occupancy.UNOCCUPIED
                                                && ScheduleProcessJob.getSystemOccupancy() != Occupancy.VACATION)
        {
            double humidity = VavSystemController.getInstance().getAverageSystemHumidity();
            double targetMinHumidity = TunerUtil.readSystemUserIntentVal("target and min and inside and humidity");
            double targetMaxHumidity = TunerUtil.readSystemUserIntentVal("target and max and inside and humidity");
    
            boolean humidifier = getConfigVal("humidifier and type") == 0;
            
            double humidityHysteresis = TunerUtil.readTunerValByQuery("humidity and hysteresis", getSystemEquipRef());
    
            if (humidifier) {
                //Humidification
                int curSignal = (int)ControlMote.getRelayState("relay7");
                if (humidity < targetMinHumidity) {
                    signal = 1;
                } else if (humidity > (targetMinHumidity + humidityHysteresis)) {
                    signal = 0;
                } else {
                    signal = curSignal;
                }
                setCmdSignal("humidifier",signal);
            } else {
                //Dehumidification
                int curSignal = (int)ControlMote.getRelayState("relay7");
                if (humidity > targetMaxHumidity) {
                    signal = 1;
                } else if (humidity < (targetMaxHumidity - humidityHysteresis)) {
                    signal = 0;
                } else {
                    signal = curSignal;
                }
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
    
        setSystemPoint("operating and mode", VavSystemController.getInstance().systemState.ordinal());
        String systemStatus = getStatusMessage();
        String scheduleStatus = ScheduleProcessJob.getSystemStatusString();
        CcuLog.d(L.TAG_CCU_SYSTEM, "StatusMessage: "+systemStatus);
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: " +scheduleStatus);
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and status and message").equals(systemStatus))
        {
            CCUHsApi.getInstance().writeDefaultVal("system and status and message", systemStatus);
            Globals.getInstance().getApplicationContext().sendBroadcast(new Intent(ACTION_STATUS_CHANGE));
        }
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and scheduleStatus").equals(scheduleStatus))
        {
            CCUHsApi.getInstance().writeDefaultVal("system and scheduleStatus", scheduleStatus);
        }
    }
    
    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        status.append(systemFanLoopOp > 0 ? " Fan ON ":((getCmdSignal("occupancy") > 0) ? " Fan ON " : ""));
        status.append(systemCoolingLoopOp > 0 ? " | Cooling ON ":"");
        status.append(systemHeatingLoopOp > 0 ? " | Heating ON ":"");
        
        if (systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
            status.insert(0, "Free Cooling Used |");
        }
    
        return status.toString().equals("")? "System OFF" : status.toString();
    }
    
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_ANALOG_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                initTRSystem();
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
                                   .setProfile(ProfileType.SYSTEM_VAV_ANALOG_RTU.name())
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
        new ControlMote(equipRef);
        initTRSystem();
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
            if (val > 0) {//dehumidifier
                HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and humidifier");
                if(cmd != null && (cmd.size() > 0)) {
                    HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
                    String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
                    Point cmdPoint = new Point.Builder().setHashMap(cmd).removeMarker("humidifier").removeMarker("runtime").addMarker("dehumidifier").setEnums("off,on").setDisplayName(equipDis + "-dehumidifier").build();
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
                    Point cmdPoint = new Point.Builder().setHashMap(cmd).removeMarker("dehumidifier").removeMarker("runtime").addMarker("humidifier").setEnums("off,on").setDisplayName(equipDis + "-humidifier").build();
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
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+config);
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
                                    .setTz(tz)
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
                                    .setTz(tz)
                                    .build();
                            String cmdDehumidPtId = CCUHsApi.getInstance().addPoint(dehumidPt);
                            CCUHsApi.getInstance().writeHisValById(cmdDehumidPtId,0.0);
                        }
                    }
                    break;
            }

            CCUHsApi.getInstance().syncEntityTree();
        }
    }
    
    private void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
    }
}
