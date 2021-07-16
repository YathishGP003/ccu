package a75f.io.logic.bo.building.system.vav;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import android.content.Intent;

import java.util.HashMap;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemConstants;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;
import static a75f.io.logic.jobs.ScheduleProcessJob.ACTION_STATUS_CHANGE;

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
        return "Daikin IE RTU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_IE_RTU;
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return (getConfigVal("cooling and output and enabled") > 0);
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (getConfigVal("heating and output and enabled") > 0);
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
                addNewSystemUserIntentPoints(equip.get("id").toString());
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
        addIEPoints(siteDis+"-SystemEquip", siteRef, equipRef, siteMap.get("tz").toString(), hayStack);
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
        
        int coolingDat, heatingDat;
        setSystemLoopOp("cooling", systemCoolingLoopOp);
        if (getConfigEnabled("cooling") > 0) {
            double coolingDatMin = getConfigVal("analog1 and cooling and dat and min");
            double coolingDatMax = getConfigVal("analog1 and cooling and dat and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "coolingDatMin: "+coolingDatMin+" coolingDatMax: "+coolingDatMax+" SAT: "+getSystemSAT());
            
            if (coolingDatMax > coolingDatMin) {
                coolingDat = (int) (coolingDatMax - (coolingDatMax - coolingDatMin) * (systemCoolingLoopOp/100));
            } else {
                coolingDat = (int) (coolingDatMin - (coolingDatMin - coolingDatMax) * (systemCoolingLoopOp/100));
            }
            
        } else {
            coolingDat = 70;
        }
        setCmdSignal("dat and cooling", coolingDat);
        
        if (VavSystemController.getInstance().getSystemState() == HEATING)
        {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
        
        setSystemLoopOp("heating", systemHeatingLoopOp);
        if (getConfigEnabled("heating") > 0) {
            double heatingDatMin = getConfigVal("analog3 and heating and min");
            double heatingDatMax = getConfigVal("analog3 and heating and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "heatingDatMin: "+heatingDatMin+" heatingDatMax: "+heatingDatMax+" HeatingSignal : "+VavSystemController.getInstance().getHeatingSignal());
            if (heatingDatMax > heatingDatMin) {
                heatingDat = (int) (heatingDatMin + (heatingDatMax - heatingDatMin) * (systemHeatingLoopOp / 100));
            } else {
                heatingDat = (int) (heatingDatMin - (heatingDatMax - heatingDatMin) * (systemHeatingLoopOp / 100));
            }
        } else {
            heatingDat = 75;
        }
    
        setCmdSignal("dat and heating", heatingDat);
        
        double datSp = VavSystemController.getInstance().getSystemState() == COOLING ? coolingDat : heatingDat;
        setCmdSignal("dat and setpoint", datSp);
        
        double analogFanSpeedMultiplier = TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", getSystemEquipRef());
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        if(epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE){
            double smartPurgeDabFanLoopOp = TunerUtil.readTunerValByQuery("system and purge and vav and fan and loop and output", getSystemEquipRef());
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");

            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure());
            double staticPressureLoopOutput = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin)) ;
            if(VavSystemController.getInstance().getSystemState() == COOLING && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO)) {
                if(staticPressureLoopOutput < ((spSpMax - spSpMin) * smartPurgeDabFanLoopOp))
                    systemFanLoopOp = ((spSpMax - spSpMin) * smartPurgeDabFanLoopOp);
                else
                    systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin))  ;
            }else if(VavSystemController.getInstance().getSystemState() == HEATING)
                systemFanLoopOp = Math.max((int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier),smartPurgeDabFanLoopOp);
        }else if ((VavSystemController.getInstance().getSystemState() == COOLING) && (systemMode == SystemMode.COOLONLY || systemMode == SystemMode.AUTO))
        {
            double spSpMax = VavTRTuners.getStaticPressureTRTunerVal("spmax");
            double spSpMin = VavTRTuners.getStaticPressureTRTunerVal("spmin");
            
            CcuLog.d(L.TAG_CCU_SYSTEM,"spSpMax :"+spSpMax+" spSpMin: "+spSpMin+" SP: "+getStaticPressure());
            systemFanLoopOp = (int) ((getStaticPressure() - spSpMin) * 100 / (spSpMax -spSpMin))  ;
        } else if (VavSystemController.getInstance().getSystemState() == HEATING){
            systemFanLoopOp = (int) (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier);
        } else {
            systemFanLoopOp = 0;
        }
        systemFanLoopOp = Math.min(systemFanLoopOp, 100);
        setSystemLoopOp("fan", systemFanLoopOp);
       
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
        status.append(VavSystemController.getInstance().getSystemState() == COOLING ?
                          " Cooling DAT (F): " + getCmdSignal("cooling and data"):"");
        status.append(VavSystemController.getInstance().getSystemState() == HEATING ?
                          " Heating DAT (F): " + getCmdSignal("heating and dat"):"");
        status.append(VavSystemController.getInstance().getSystemState() != OFF ? " | Static Pressure (inch wc): " + (getStaticPressure()):"");
        
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
                .addMarker("system").addMarker("cmd").addMarker("dat").addMarker("setpoint").addMarker("temp").addMarker("his")
                .setUnit("\u00B0F").setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(DATClgSetpoint);
    }
    
    private void addIEPoints(String equipDis, String siteRef, String equipRef, String tz, CCUHsApi hayStack) {
        Point alarmWarnings = new Point.Builder()
                                   .setDisplayName(equipDis+"-"+"alarmWarnings")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef)
                                   .addMarker("system").addMarker("alarm").addMarker("warning").addMarker("ie")
                                   .addMarker("sp").addMarker("his")
                                   .setTz(tz)
                                   .build();
        CCUHsApi.getInstance().addPoint(alarmWarnings);
        Point alarmProblems = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"alarmProblems")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("alarm").addMarker("problem").addMarker("ie")
                                  .addMarker("sp").addMarker("his")
                                  .setTz(tz)
                                  .build();
        CCUHsApi.getInstance().addPoint(alarmProblems);
    
        Point alarmFaults = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"alarmFaults")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("alarm").addMarker("fault").addMarker("ie")
                                  .addMarker("sp").addMarker("his")
                                  .setTz(tz)
                                  .build();
        CCUHsApi.getInstance().addPoint(alarmFaults);
    
        Point systemClock = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"systemClock")
                                .setSiteRef(siteRef)
                                .setEquipRef(equipRef)
                                .addMarker("system").addMarker("clock").addMarker("ie")
                                .addMarker("sp").addMarker("his")
                                .setTz(tz)
                                .build();
        CCUHsApi.getInstance().addPoint(systemClock);
    
        Point occStatus = new Point.Builder()
                                .setDisplayName(equipDis+"-"+"occStatus")
                                .setSiteRef(siteRef)
                                .setEquipRef(equipRef)
                                .addMarker("system").addMarker("occStatus").addMarker("ie")
                                .addMarker("sp").addMarker("his")
                                .setTz(tz)
                                .build();
        CCUHsApi.getInstance().addPoint(occStatus);
    }
    
    private void addConfigPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point coolingEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"coolingEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("cooling")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String coolingEnabledId = hayStack.addPoint(coolingEnabled);
        hayStack.writeDefaultValById(coolingEnabledId, 0.0 );
        
        Point fanEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"fanEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("fan")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String fanEnabledId = hayStack.addPoint(fanEnabled);
        hayStack.writeDefaultValById(fanEnabledId, 0.0 );
        
        Point heatingEnabled = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"heatingEnabled")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("config").addMarker("heating")
                                             .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                             .setEnums("false,true").setTz(tz)
                                             .build();
        String heatingEnabledId = hayStack.addPoint(heatingEnabled);
        hayStack.writeDefaultValById(heatingEnabledId, 0.0 );
    
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
    
        Point multiZoneEnabled = new Point.Builder()
                                          .setDisplayName(equipDis+"-"+"multiZoneEnabled")
                                          .setSiteRef(siteRef)
                                          .setEquipRef(equipref)
                                          .addMarker("system").addMarker("config").addMarker("multiZone")
                                          .addMarker("enabled").addMarker("writable").addMarker("sp")
                                          .setEnums("false,true").setTz(tz)
                                          .build();
        String multiZoneEnabledId = hayStack.addPoint(multiZoneEnabled);
        hayStack.writeDefaultValById(multiZoneEnabledId, 0.0 );
        
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
        hayStack.writeDefaultValById(maxStaticPressureId, 2.0 );
        
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
                                      .addMarker("system").addMarker("config").addMarker("ie")
                                      .addMarker("ipAddress").addMarker("writable").addMarker("sp")
                                      .setKind(Kind.STRING)
                                      .setTz(tz)
                                      .build();
        String equipmentIPId = hayStack.addPoint(equipmentIP);
        hayStack.writeDefaultValById(equipmentIPId, "172.16.0.1" );
    
        Point macAddress = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"macAddress")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("system").addMarker("config").addMarker("ie")
                                     .addMarker("macAddress").addMarker("writable").addMarker("sp")
                                     .setKind(Kind.STRING)
                                     .setTz(tz)
                                     .build();
        String macAddressId = hayStack.addPoint(macAddress);
        hayStack.writeDefaultValById(macAddressId, "" );
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
                case "cooling":
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
                                .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("modulating").addMarker("discharge").addMarker("air").addMarker("temp").addMarker("his")
                                .setUnit("\u00B0F").setTz(tz)
                                .build();
                        CCUHsApi.getInstance().addPoint(coolingSignal);
                    }
                    break;
                case "fan":
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
                                .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("modulating").addMarker("his")
                                .setUnit("inch wc").setTz(tz)
                                .build();
                        CCUHsApi.getInstance().addPoint(fanSignal);
                    }
                    break;
                case "heating":
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
                                .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("modulating").addMarker("discharge").addMarker("air").addMarker("temp").addMarker("his")
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
                                .addMarker("system").addMarker("cmd").addMarker("humidifier").addMarker("his")
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
