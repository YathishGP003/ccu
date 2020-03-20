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
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;

/**
 * System profile to handle AHU via IE gateways.
 *
 */
public class VavIERtu extends VavSystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
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
    
    public String getProfileName() {
        return "Daikin IE Rtu";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_IE_RTU;
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
    
    @Override
    public void doSystemControl() {
        if (trSystem != null) {
            trSystem.processResetResponse();
        }
        VavSystemController.getInstance().runVavSystemControlAlgo();
        updateSystemPoints();
        setTrTargetVals();
    }
    
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_IE_RTU.name())) {
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
                                   .setProfile(ProfileType.SYSTEM_VAV_IE_RTU.name())
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
        new ControlMote(equipRef);
        initTRSystem();
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
        
        
    }
    
    @Override
    public void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_IE_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
    
    private synchronized void updateSystemPoints() {
        updateOutsideWeatherParams();
        
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
        setSystemLoopOp("cooling", systemCoolingLoopOp);
        if (getConfigVal("analog1 and output and enabled") > 0)
        {
            double coolingDatMin = getConfigVal("analog1 and cooling and dat and min");
            double coolingDatMax = getConfigVal("analog1 and cooling and dat and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "coolingDatMin: "+coolingDatMin+" coolingDatMax: "+coolingDatMax+" SAT: "+getSystemSAT());
            
            if (coolingDatMax > coolingDatMin)
            {
                signal = (int) (coolingDatMax - (coolingDatMax - coolingDatMin) * (systemCoolingLoopOp/100));
            }
            else
            {
                signal = (int) (coolingDatMin - (coolingDatMin - coolingDatMax) * (systemCoolingLoopOp/100));
            }

            setCmdSignal("cooling",signal);
            
        } else {
            signal = 0;
        }
        
        if (VavSystemController.getInstance().getSystemState() == HEATING)
        {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
        
        setSystemLoopOp("heating", systemHeatingLoopOp);
        if (getConfigVal("analog3 and output and enabled") > 0)
        {
            double heatingDatMin = getConfigVal("analog3 and heating and min");
            double heatingDatMax = getConfigVal("analog3 and heating and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "heatingDatMin: "+heatingDatMin+" heatingDatMax: "+heatingDatMax+" HeatingSignal : "+VavSystemController.getInstance().getHeatingSignal());
            if (heatingDatMax > heatingDatMin)
            {
                signal = (int) (heatingDatMin + (heatingDatMax - heatingDatMin) * (systemHeatingLoopOp / 100));
            }
            else
            {
                signal = (int) (heatingDatMin - (heatingDatMax - heatingDatMin) * (systemHeatingLoopOp / 100));
            }
            setCmdSignal("heating", signal);
        } else {
            signal = 0;
        }
    
        double datSp = VavSystemController.getInstance().getSystemState() == COOLING ? getCmdSignal("cooling") : getCmdSignal("heating");
        setCmdSignal("dat", datSp);
        ControlMote.setAnalogOut("analog1", datSp);
        
        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", getSystemEquipRef());
        if (VavSystemController.getInstance().getSystemState() == COOLING)
        {
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");
            
            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure());
            systemFanLoopOp = (int) (getStaticPressure()  * 100 / spSpMax) ;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
        systemFanLoopOp = Math.min(systemFanLoopOp, 100);
        setSystemLoopOp("fan", systemFanLoopOp);
        double spSignal = 0;
        if (getConfigVal("analog2 and output and enabled") > 0)
        {
            double staticPressureMin = getConfigVal("analog2 and staticPressure and min");
            double staticPressureMax = getConfigVal("analog2 and staticPressure and max");
            
            CcuLog.d(L.TAG_CCU_SYSTEM, "staticPressureMin: "+staticPressureMin+" staticPressureMax: "+staticPressureMax+" systemFanLoopOp: "+systemFanLoopOp);
            
            if (staticPressureMax > staticPressureMin)
            {
                spSignal = CCUUtils.roundToTwoDecimal(staticPressureMin + (staticPressureMax - staticPressureMin) * (systemFanLoopOp / 100.0));
            }
            else
            {
                spSignal = CCUUtils.roundToTwoDecimal(staticPressureMin - (staticPressureMin - staticPressureMax) * (systemFanLoopOp/100.0));
            }
            setCmdSignal("fan", spSignal);
        } else {
            spSignal = 0;
        }
        ControlMote.setAnalogOut("analog2", spSignal);
        
        ControlMote.setAnalogOut("analog3", VavSystemController.getInstance().getAverageSystemHumidity());
    
        systemCo2LoopOp = VavSystemController.getInstance().getSystemState() == OFF
                                  ? 0 : (SystemConstants.CO2_CONFIG_MAX - getSystemCO2()) * 100 / 200 ;
        setSystemLoopOp("co2", systemCo2LoopOp);
        
        if (L.ccu().oaoProfile != null) {
            ControlMote.setAnalogOut("analog4", CCUHsApi.getInstance().readHisValByQuery("point and his and outside and air and damper and cmd"));
        } else {
            ControlMote.setAnalogOut("analog4",0);
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
        status.append(VavSystemController.getInstance().getSystemState() == COOLING ? " Cooling DAT (F): " + getCmdSignal("cooling"):"");
        status.append(VavSystemController.getInstance().getSystemState() == HEATING ? " Heating DAT (F): " + getCmdSignal("heating"):"");
        status.append(VavSystemController.getInstance().getSystemState() != OFF ? " | Static Pressure (inch wc): " + (getCmdSignal("fan")):"");
        
        if (systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable()) {
            status.insert(0, "Free Cooling Used |");
        }
        
        return status.toString().equals("")? "System OFF" : status.toString();
    }
    
    private void addCmdPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();


        Point DATClgSetpoint = new Point.Builder()
                .setDisplayName(equipDis+"-"+"DATClgSetpoint")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .addMarker("system").addMarker("cmd").addMarker("dat").addMarker("setpoint").addMarker("temp").addMarker("his").addMarker("equipHis")
                .setUnit("\u00B0F").setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(DATClgSetpoint);
        /*Point coolingSignal = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"coolingDat")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("discharge").addMarker("air").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("runtime")
                                      .setUnit("\u00B0F").setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(coolingSignal);
        
        Point heatingSignal = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"heatingDat")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("discharge").addMarker("air").addMarker("temp").addMarker("his").addMarker("equipHis").addMarker("runtime")
                                      .setUnit("\u00B0F").setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(heatingSignal);
    
        Point DATClgSetpoint = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"DATClgSetpoint")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("cmd").addMarker("dat").addMarker("setpoint").addMarker("temp").addMarker("his").addMarker("equipHis")
                                      .setUnit("\u00B0F").setTz(tz)
                                      .build();
        CCUHsApi.getInstance().addPoint(DATClgSetpoint);
        
        Point fanSignal = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"ductStaticPressure")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipref)
                                  .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("his").addMarker("equipHis").addMarker("runtime")
                                  .setUnit("inch wc").setTz(tz)
                                  .build();
        CCUHsApi.getInstance().addPoint(fanSignal);*/
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
    
        Point humidificationEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"humidificationEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("humidification")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String humidificationEnabledId = hayStack.addPoint(humidificationEnabled);
        hayStack.writeDefaultValById(humidificationEnabledId, 0.0 );
        
        
        Point minCoolingDat = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"minCoolingDat")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("min").addMarker("cooling").addMarker("dat").addMarker("writable").addMarker("sp")
                                               .setUnit("\u00B0F")
                                               .setTz(tz)
                                               .build();
        String minCoolingDatId = hayStack.addPoint(minCoolingDat);
        hayStack.writeDefaultValById(minCoolingDatId, 55.0 );
        
        Point maxCoolingDat = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"maxCoolingDat")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("max").addMarker("cooling").addMarker("dat").addMarker("writable").addMarker("sp")
                                               .setUnit("\u00B0F")
                                               .setTz(tz)
                                               .build();
        String maxCoolingDatId = hayStack.addPoint(maxCoolingDat);
        hayStack.writeDefaultValById(maxCoolingDatId, 70.0 );
        
        Point minStaticPressure = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"minStaticPressure")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("min").addMarker("staticPressure").addMarker("writable").addMarker("sp")
                                                   .setUnit("inch wc")
                                                   .setTz(tz)
                                                   .build();
        String minStaticPressureId = hayStack.addPoint(minStaticPressure);
        hayStack.writeDefaultValById(minStaticPressureId, 0.2 );
        
        Point maxStaticPressure = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"maxStaticPressure")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("max").addMarker("staticPressure").addMarker("writable").addMarker("sp")
                                                   .setUnit("inch wc")
                                                   .setTz(tz)
                                                   .build();
        String maxStaticPressureId = hayStack.addPoint(maxStaticPressure);
        hayStack.writeDefaultValById(maxStaticPressureId, 1.0 );
        
        Point minHeatingDat = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"minHeatingDat")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog3")
                                            .addMarker("min").addMarker("heating").addMarker("dat").addMarker("writable").addMarker("sp")
                                            .setUnit("\u00B0F")
                                            .setTz(tz)
                                            .build();
        String minHeatingDatId = hayStack.addPoint(minHeatingDat);
        hayStack.writeDefaultValById(minHeatingDatId, 75.0 );
        
        Point maxHeatingDat = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"maxHeatingDat")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog3")
                                            .addMarker("max").addMarker("heating").addMarker("dat").addMarker("writable").addMarker("sp")
                                            .setUnit("\u00B0F")
                                            .setTz(tz)
                                            .build();
        String maxHeatingDatId = hayStack.addPoint(maxHeatingDat);
        hayStack.writeDefaultValById(maxHeatingDatId, 100.0 );
    
        Point equipmentIP = new Point.Builder()
                                      .setDisplayName(equipDis+"-"+"equipmentIPAddress")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .addMarker("system").addMarker("config").addMarker("ie").addMarker("ip")
                                      .addMarker("address").addMarker("writable").addMarker("sp")
                                      .setKind("string")
                                      .setTz(tz)
                                      .build();
        String equipmentIPId = hayStack.addPoint(equipmentIP);
        hayStack.writeDefaultValById(equipmentIPId, "10.100.11.71" );
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
    
    public double getConfigVal(String tags) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and "+tags);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }
    
    public void setConfigVal(String tags, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
    }
    
    public double getConfigEnabled(String config) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and enabled and "+config);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
        
    }
    /*public void setConfigEnabled(String config, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and output and enabled and "+config, val);
    }*/
    
    public void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
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
                                .setDisplayName(equipDis+"-"+"coolingDat")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("modulating").addMarker("discharge").addMarker("air").addMarker("temp").addMarker("his").addMarker("equipHis")
                                .setUnit("\u00B0F").setTz(tz)
                                .build();
                        CCUHsApi.getInstance().addPoint(coolingSignal);
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
                                .setDisplayName(equipDis+"-"+"ductStaticPressure")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("modulating").addMarker("his").addMarker("equipHis")
                                .setUnit("inch wc").setTz(tz)
                                .build();
                        CCUHsApi.getInstance().addPoint(fanSignal);
                    }
                    break;
                case "analog3":
                    HashMap cmdHeat = CCUHsApi.getInstance().read("point and system and cmd and heating and modulating");
                    if(cmdHeat != null && cmdHeat.size() > 0) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmdHeat.get("id").toString());
                        }
                    }else {
                        Point heatingSignal = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"heatingDat")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("modulating").addMarker("discharge").addMarker("air").addMarker("temp").addMarker("his").addMarker("equipHis")
                                .setUnit("\u00B0F").setTz(tz)
                                .build();
                        CCUHsApi.getInstance().addPoint(heatingSignal);
                    }
                    break;
                case "humidification":
                    HashMap cmdHumid = CCUHsApi.getInstance().read("point and system and cmd and humidifier");
                    if(cmdHumid != null && cmdHumid.size() > 0) {
                        if(val == 0.0) {
                            CCUHsApi.getInstance().deleteEntityTree(cmdHumid.get("id").toString());
                        }
                    }else {
                        Point heatingSignal = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"humidifier")
                                .setSiteRef(siteRef)
                                .setEquipRef(configEnabledPt.getEquipRef()).setHisInterpolate("cov")
                                .addMarker("system").addMarker("cmd").addMarker("humidifier").addMarker("his").addMarker("equipHis")
                                .setEnums("off,on")
                                .setUnit("%").setTz(tz)
                                .build();
                        CCUHsApi.getInstance().addPoint(heatingSignal);
                    }
                    break;
            }

            CCUHsApi.getInstance().syncEntityTree();
        }
    }
}
