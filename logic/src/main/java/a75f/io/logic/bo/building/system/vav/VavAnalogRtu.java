package a75f.io.logic.bo.building.system.vav;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import java.util.ArrayList;
import java.util.HashMap;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

import static a75f.io.logic.bo.building.system.vav.VavSystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.vav.VavSystemController.State.HEATING;

/**
 * Default System handles PI controlled op
 */
public class VavAnalogRtu extends VavSystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
    private static final int ANALOG_SCALE = 10;
    
    public VavAnalogRtu() {
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
    public void doSystemControl() {
        if (trSystem != null) {
            trSystem.processResetResponse();
        }
        VavSystemController.getInstance().runVavSystemControlAlgo();
        updateSystemPoints();
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
        
        double analogMin = getConfigVal("analog1 and cooling and sat and min");
        double analogMax = getConfigVal("analog1 and cooling and sat and max");
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" SAT: "+getSystemSAT());
        
        int signal = 0;
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemCoolingLoopOp/100)));
        }
        else
        {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemCoolingLoopOp/100)));
        }
        
        setSystemLoopOp("cooling", systemCoolingLoopOp);
        setCmdSignal("cooling",signal);
        if (getConfigVal("analog1 and output and enabled") > 0)
        {
            ControlMote.setAnalogOut("analog1", signal);
        }
        
    
        analogMin = getConfigVal("analog3 and heating and min");
        analogMax = getConfigVal("analog3 and heating and max");
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" Heating : "+VavSystemController.getInstance().getHeatingSignal());
        if (VavSystemController.getInstance().getSystemState() == VavSystemController.State.HEATING)
        {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
    
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemHeatingLoopOp / 100)));
        }
        else
        {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemHeatingLoopOp / 100)));
        }
        
        setSystemLoopOp("heating", systemHeatingLoopOp);
        setCmdSignal("heating", signal);
        if (getConfigVal("analog3 and output and enabled") > 0)
        {
            ControlMote.setAnalogOut("analog3", signal);
        }
        
        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier");
        if (VavSystemController.getInstance().getSystemState() == COOLING)
        {
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");
    
            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SAT: "+getStaticPressure());
            systemFanLoopOp = (int) ((getStaticPressure() - spSpMin)  * 100 / (spSpMax - spSpMin)) ;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        }
        
    
        analogMin = getConfigVal("analog2 and staticPressure and min");
        analogMax = getConfigVal("analog2 and staticPressure and max");
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemFanLoopOp: "+systemFanLoopOp);
    
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemFanLoopOp/100)));
        }
        else
        {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemFanLoopOp/100)));
        }
        setSystemLoopOp("fan", systemFanLoopOp);
        setCmdSignal("fan", signal);
        if (getConfigVal("analog2 and output and enabled") > 0)
        {
            ControlMote.setAnalogOut("analog2", signal);
        }
        
    
        analogMin = getConfigVal("analog4 and co2 and min");
        analogMax = getConfigVal("analog4 and co2 and max");
        CcuLog.d(L.TAG_CCU_SYSTEM,"analogMin: "+analogMin+" analogMax: "+analogMax+" CO2: "+getSystemCO2());
        if (analogMax > analogMin)
        {
            signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (getSystemCO2() - SystemConstants.CO2_CONFIG_MIN) / 200));
        } else {
            signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (getSystemCO2() - SystemConstants.CO2_CONFIG_MIN) / 200));
        }
        setCmdSignal("co2",signal);
        if (getConfigVal("analog4 and output and enabled") > 0)
        {
            ControlMote.setAnalogOut("analog4", signal);
        }
        
        if (getConfigVal("relay3 and output and enabled") > 0)
        {
            //TODO - TEMP
            boolean occupancy = true;
            double staticPressuremOp = getStaticPressure() - SystemConstants.SP_CONFIG_MIN;
            signal = (occupancy || staticPressuremOp > 0) ? 1 : 0;
            setCmdSignal("occupancy",signal * 100);
            ControlMote.setRelayState("relay3", signal );
            
        }
        //TODO- TEMP
        if (getConfigVal("relay7 and output and enabled") > 0)
        {
            double humidity = VavSystemController.getInstance().getSystemHumidity();
            double targetMinHumidity = TunerUtil.readSystemUserIntentVal("target and min and inside and humidity");
            double targetMaxHumidity = TunerUtil.readSystemUserIntentVal("target and max and inside and humidity");
    
            boolean humidifier = getConfigVal("humidifier and type") == 0;
            
            double humidityHysteresis = TunerUtil.readTunerValByQuery("humidity and hysteresis");
    
            if (humidifier) {
                //Humidification
                //signal = (int)ControlMote.getRelayState("relay7");
                if (humidity < targetMinHumidity) {
                    signal = 1;
                } else if (humidity > (targetMinHumidity + humidityHysteresis)) {
                    signal = 0;
                }
                setCmdSignal("humidifier",signal * 100);
                setCmdSignal("dehumidifier",0);
            } else {
                //Dehumidification
                //signal = (int)ControlMote.getRelayState("relay7");
                if (humidity > targetMaxHumidity) {
                    signal = 1;
                } else if (humidity < (targetMaxHumidity - humidityHysteresis)) {
                    signal = 0;
                }
                setCmdSignal("dehumidifier",signal * 100);
                setCmdSignal("humidifier",0);
            }
            CcuLog.d(L.TAG_CCU_SYSTEM,"humidity :"+humidity+" targetMinHumidity: "+targetMinHumidity+" humidityHysteresis: "+humidityHysteresis+
                                      " targetMaxHumidity: "+targetMaxHumidity+" signal: "+signal*100);
    
            ControlMote.setRelayState("relay7", signal);
        }
        
    }
    
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_ANALOG_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                initTRSystem();
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
    
    
        /*Point sat = new Point.Builder()
                            .setDisplayName(equipDis+"-"+"SAT")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipref)
                            .addMarker("tr").addMarker("sat").addMarker("his").addMarker("system").addMarker("equipHis")
                            .setUnit("\u00B0F")
                            .setTz(tz)
                            .build();
        CCUHsApi.getInstance().addPoint(sat);
    
        Point co2 = new Point.Builder()
                            .setDisplayName(equipDis+"-"+"CO2")
                            .setSiteRef(siteRef)
                            .setEquipRef(equipref)
                            .addMarker("tr").addMarker("co2").addMarker("his").addMarker("system").addMarker("equipHis")
                            .setUnit("\u00B0ppm")
                            .setTz(tz)
                            .build();
        CCUHsApi.getInstance().addPoint(co2);
    
        Point sp = new Point.Builder()
                           .setDisplayName(equipDis+"-"+"SP")
                           .setSiteRef(siteRef)
                           .setEquipRef(equipref)
                           .addMarker("tr").addMarker("sp").addMarker("his").addMarker("system").addMarker("equipHis")
                           .setUnit("\u00B0in")
                           .setTz(tz)
                           .build();
        CCUHsApi.getInstance().addPoint(sp);*/
        
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
        HashMap cdb = hayStack.read("point and system and config and "+tags);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public void setConfigVal(String tags, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
    }
    
    /*public double getConfigVal(String tags, int level) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and config and "+tags);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(level-1));
            if (valMap.get("val") != null) {
                return Double.parseDouble(valMap.get("val").toString());
            }
        }
        return 0;
    }
    
    public void setConfigVal(String tags, int level, double val) {
    
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and config and "+tags);
    
        String id = cdb.get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        hayStack.writePoint(id, level, "ccu", val, 0);
    }*/
    
    public double getConfigEnabled(String config) {
        return CCUHsApi.getInstance().readDefaultVal("point and system and config and output and enabled and "+config);
    }
    public void setConfigEnabled(String config, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and output and enabled and "+config, val);
    }
    
    private void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
    }
}
