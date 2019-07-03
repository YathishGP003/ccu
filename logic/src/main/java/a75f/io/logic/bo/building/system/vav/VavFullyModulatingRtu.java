package a75f.io.logic.bo.building.system.vav;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import java.util.HashMap;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;

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
        return "VAV Fully Modulating RTU";
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
        
        if (VavSystemController.getInstance().getSystemState() == COOLING)
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
    
        setCmdSignal("cooling",signal);
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
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog3Min: "+analogMin+" analog3Max: "+analogMax+" Heating : "+VavSystemController.getInstance().getHeatingSignal());
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
        setCmdSignal("heating", signal);
        ControlMote.setAnalogOut("analog3", signal);
        
        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", getSystemEquipRef());
        if (VavSystemController.getInstance().getSystemState() == COOLING)
        {
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");
    
            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure());
            systemFanLoopOp = (int) ((getStaticPressure() - spSpMin)  * 100 / (spSpMax - spSpMin)) ;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
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
        setCmdSignal("fan", signal);
        ControlMote.setAnalogOut("analog2", signal);
        
        systemCo2LoopOp = VavSystemController.getInstance().getSystemState() == SystemController.State.OFF
                                         ? 0 :(SystemConstants.CO2_CONFIG_MAX - getSystemCO2()) * 100 / 200 ;
        setSystemLoopOp("co2", systemCo2LoopOp);
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
        setCmdSignal("co2",signal);
        ControlMote.setAnalogOut("analog4", signal);
    
        SystemMode systemMode = SystemMode.values()[(int)getUserIntentVal("rtu and mode")];
        if (getConfigVal("relay3 and output and enabled") > 0 && systemMode != SystemMode.OFF)
        {
            double staticPressuremOp = getStaticPressure() - SystemConstants.SP_CONFIG_MIN;
            signal = (ScheduleProcessJob.getSystemOccupancy() != Occupancy.UNOCCUPIED || staticPressuremOp > 0) ? 1 : 0;
            setCmdSignal("occupancy",signal * 100);
            ControlMote.setRelayState("relay3", signal );
        } else {
            ControlMote.setRelayState("relay3", 0 );
        }
        
        if (getConfigVal("relay7 and output and enabled") > 0 && systemMode != SystemMode.OFF
                                                && ScheduleProcessJob.getSystemOccupancy() != Occupancy.UNOCCUPIED)
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
                setCmdSignal("humidifier",signal * 100);
                setCmdSignal("dehumidifier",0);
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
                setCmdSignal("dehumidifier",signal * 100);
                setCmdSignal("humidifier",0);
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
        }
        if (!CCUHsApi.getInstance().readDefaultStrVal("system and scheduleStatus").equals(scheduleStatus))
        {
            CCUHsApi.getInstance().writeDefaultVal("system and scheduleStatus", scheduleStatus);
        }
    }
    
    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
        status.append(systemFanLoopOp > 0 ? " Fan ON ":"");
        status.append(systemCoolingLoopOp > 0 ? " | Cooling ON ":"");
        status.append(systemHeatingLoopOp > 0 ? " | Heating ON ":"");
    
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
                //sysEquip = new SystemEquip(equip.get("id").toString());
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
                                   .addMarker("equipHis")
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
        new ControlMote(siteRef);
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
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        Point coolingSignal = new Point.Builder()
                            .setDisplayName(equipDis+"-"+"coolingSignal")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipref)
                            .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("his").addMarker("equipHis")
                            .setTz(tz)
                            .build();
        CCUHsApi.getInstance().addPoint(coolingSignal);
    
        Point heatingSignal = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"heatingSignal")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("his").addMarker("equipHis")
                                      .setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(heatingSignal);
    
        Point fanSignal = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"fanSignal")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("his").addMarker("equipHis")
                                      .setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(fanSignal);
    
        Point co2Signal = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"co2Signal")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("system").addMarker("cmd").addMarker("co2").addMarker("his").addMarker("equipHis")
                                  .setTz(tz)
                                  .build();
        CCUHsApi.getInstance().addPoint(co2Signal);
        Point occupancySignal = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"occupancySignal")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("system").addMarker("cmd").addMarker("occupancy").addMarker("his").addMarker("equipHis")
                                  .setTz(tz)
                                  .build();
        CCUHsApi.getInstance().addPoint(occupancySignal);
        Point humidifierSignal = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"humidifierSignal")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("system").addMarker("cmd").addMarker("humidifier").addMarker("his").addMarker("equipHis")
                                  .setTz(tz)
                                  .build();
        CCUHsApi.getInstance().addPoint(humidifierSignal);
    
        Point dehumidifierSignal = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"dehumidifierSignal")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipref)
                                         .addMarker("system").addMarker("cmd").addMarker("dehumidifier").addMarker("his").addMarker("equipHis")
                                         .setTz(tz)
                                         .build();
        CCUHsApi.getInstance().addPoint(dehumidifierSignal);
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
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
                                      .setTz(tz)
                                      .build();
        String analog1OutputEnabledId = hayStack.addPoint(analog1OutputEnabled);
        hayStack.writeDefaultValById(analog1OutputEnabledId, 0.0 );
    
        Point analog2OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"analog2OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("analog2")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setTz(tz)
                                             .build();
        String analog2OutputEnabledId = hayStack.addPoint(analog2OutputEnabled);
        hayStack.writeDefaultValById(analog2OutputEnabledId, 0.0 );
    
        Point analog3OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"analog3OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("analog3")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setTz(tz)
                                             .build();
        String analog3OutputEnabledId = hayStack.addPoint(analog3OutputEnabled);
        hayStack.writeDefaultValById(analog3OutputEnabledId, 0.0 );
    
        Point analog4OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"analog4OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("analog4")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setTz(tz)
                                             .build();
        String analog4OutputEnabledId = hayStack.addPoint(analog4OutputEnabled);
        hayStack.writeDefaultValById(analog4OutputEnabledId, 0.0 );
    
        Point relay3OutputEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"relay3OutputEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("relay3")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setTz(tz)
                                             .build();
        String relay3OutputEnabledId = hayStack.addPoint(relay3OutputEnabled);
        hayStack.writeDefaultValById(relay3OutputEnabledId, 0.0 );
    
        Point relay7OutputEnabled = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"relay7OutputEnabled")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("relay7")
                                            .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                            .setTz(tz)
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
                                        .setUnit("V")
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
    
    private void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
    }
}
