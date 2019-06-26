package a75f.io.logic.bo.building.system.vav;

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
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.tuners.VavTRTuners;

import static a75f.io.logic.bo.building.hvac.Stage.COOLING_1;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_2;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_3;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_4;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_5;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_1;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_2;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_3;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_4;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_5;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_1;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_2;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_3;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_4;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_5;
import static a75f.io.logic.bo.building.hvac.Stage.HUMIDIFIER;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;
import static a75f.io.logic.bo.building.system.SystemController.State.OFF;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

public class VavStagedRtu extends VavSystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    
    public int heatingStages = 0;
    public int coolingStages = 0;
    public int fanStages = 0;
    
    int[] stageStatus = new int[17];
    
    
    public void initTRSystem() {
        trSystem =  new VavTRSystem();
    }
    
    public String getProfileName()
    {
        return "VAV Staged RTU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_STAGED_RTU;
    }
    
    public VavStagedRtu() {
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
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_STAGED_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                initTRSystem();
                updateStagesSelected();
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
                                   .setProfile(ProfileType.SYSTEM_VAV_STAGED_RTU.name())
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
    public boolean isCoolingAvailable() {
        return (coolingStages > 0);
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0);
    }
    
    @Override
    public boolean isCoolingActive(){
        return stageStatus[COOLING_1.ordinal()] > 0 || stageStatus[COOLING_2.ordinal()] > 0 || stageStatus[COOLING_3.ordinal()] > 0
                                || stageStatus[COOLING_4.ordinal()] > 0 || stageStatus[COOLING_5.ordinal()] > 0;
    }
    
    @Override
    public boolean isHeatingActive(){
        return stageStatus[HEATING_1.ordinal()] > 0 || stageStatus[HEATING_2.ordinal()] > 0 || stageStatus[HEATING_3.ordinal()] > 0
               || stageStatus[HEATING_4.ordinal()] > 0 || stageStatus[HEATING_5.ordinal()] > 0;
    }
    
    protected synchronized void updateSystemPoints() {
    
        stageStatus = new int[17];
        if (VavSystemController.getInstance().getSystemState() == COOLING)
        {
            double satSpMax = VavTRTuners.getSatTRTunerVal("spmax");
            double satSpMin = VavTRTuners.getSatTRTunerVal("spmin");
    
            CcuLog.d(L.TAG_CCU_SYSTEM,"satSpMax :"+satSpMax+" satSpMin: "+satSpMin+" SAT: "+getSystemSAT());
            systemCoolingLoopOp = (int) ((satSpMax - getSystemSAT())  * 100 / (satSpMax - satSpMin)) ;
        } else {
            systemCoolingLoopOp = 0;
        }
        
        if (VavSystemController.getInstance().getSystemState() == HEATING)
        {
            systemHeatingLoopOp = VavSystemController.getInstance().getHeatingSignal();
        } else {
            systemHeatingLoopOp = 0;
        }
        
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
        
        setSystemLoopOp("cooling", systemCoolingLoopOp);
        setSystemLoopOp("heating", systemHeatingLoopOp);
        setSystemLoopOp("fan", systemFanLoopOp);
        
        
        updateStagesSelected();
    
        double relayDeactHysteresis = TunerUtil.readTunerValByQuery("relay and deactivation and hysteresis", getSystemEquipRef());
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp: "+systemCoolingLoopOp + " systemHeatingLoopOp: " + systemHeatingLoopOp+" systemFanLoopOp: "+systemFanLoopOp);
        CcuLog.d(L.TAG_CCU_SYSTEM, "coolingStages: "+coolingStages + " heatingStages: "+heatingStages+" fanStages: "+fanStages);
        SystemMode systemMode = SystemMode.values()[(int)getUserIntentVal("rtu and mode")];
        for (int i = 1; i <=7 ;i++)
        {
            double relayState = 0;
            double currState = 0;
            double stageThreshold = 0;
            Stage stage = Stage.values()[(int) getConfigAssociation("relay" + i)];
            if (getConfigEnabled("relay"+i) == 0) {
                relayState = 0;
            } else
            {
                switch (stage)
                {
                    case COOLING_1:
                        if (coolingStages > 0 && systemCoolingLoopOp > 0)
                        {
                            relayState = 1;
                        }
                        else
                        {
                            relayState = 0;
                        }
                        break;
                    case COOLING_2:
                    case COOLING_3:
                    case COOLING_4:
                    case COOLING_5:
                        currState = getCmdSignal("relay" + i);
                        stageThreshold = 100 * stage.ordinal() / coolingStages;
                        if (currState == 0)
                        {
                            if (coolingStages > 0 && systemCoolingLoopOp > stageThreshold)
                            {
                                relayState = 1;
                            }
                            else
                            {
                                relayState = 0;
                            }
                        }
                        else
                        {
                            if (coolingStages > 0 && systemCoolingLoopOp >= (stageThreshold - relayDeactHysteresis))
                            {
                                relayState = 1;
                            }
                            else
                            {
                                relayState = 0;
                            }
                        }
                        break;
                    case HEATING_1:
                        if (heatingStages > 0 && systemHeatingLoopOp > 0)
                        {
                            relayState = 1;
                        }
                        else
                        {
                            relayState = 0;
                        }
                        break;
                    case HEATING_2:
                    case HEATING_3:
                    case HEATING_4:
                    case HEATING_5:
                        currState = getCmdSignal("relay" + i);
                        stageThreshold = 100 * (stage.ordinal() - HEATING_1.ordinal()) / heatingStages;
                        if (currState == 0)
                        {
                            if (heatingStages > 0 && systemHeatingLoopOp > stageThreshold)
                            {
                                relayState = 1;
                            }
                            else
                            {
                                relayState = 0;
                            }
                        }
                        else
                        {
                            if (heatingStages > 0 && systemHeatingLoopOp >= (stageThreshold - relayDeactHysteresis))
                            {
                                relayState = 1;
                            }
                            else
                            {
                                relayState = 0;
                            }
                        }
                        break;
                    case FAN_1:
                        if ((fanStages > 0 && systemMode != SystemMode.OFF && (ScheduleProcessJob.getSystemOccupancy() != Occupancy.UNOCCUPIED
                                            || VavSystemController.getInstance().getSystemState() != OFF))
                                || (fanStages > 0 && systemFanLoopOp > 0)) {
                            relayState = 1;
                        } else {
                            relayState = 0;
                        }
                        break;
                    case FAN_2:
                        if (L.ccu().systemProfile.getProfileType() == ProfileType.SYSTEM_VAV_STAGED_VFD_RTU) {
                            relayState =  (fanStages > 0 && (systemCoolingLoopOp > 0 || systemHeatingLoopOp > 0)) ? 1 :0;
                        }
                        else if (fanStages > 0 && systemFanLoopOp > 0)
                        {
                            relayState = (fanStages > 0 && systemFanLoopOp > 0) ? 1 : 0;
                        }
                        break;
                    case FAN_3:
                    case FAN_4:
                    case FAN_5:
                        currState = getCmdSignal("relay" + i);
                        stageThreshold = 100 * (stage.ordinal() - FAN_2.ordinal()) / (fanStages - 1);
                        if (currState == 0)
                        {
                            if (fanStages > 0 && systemFanLoopOp >= stageThreshold)
                            {
                                relayState = 1;
                            }
                            else
                            {
                                relayState = 0;
                            }
                        }
                        else
                        {
                            if (fanStages > 0 && systemFanLoopOp >= (stageThreshold - relayDeactHysteresis))
                            {
                                relayState = 1;
                            }
                            else
                            {
                                relayState = 0;
                            }
                        }
                        break;
                    case HUMIDIFIER:
                    case DEHUMIDIFIER:
                        if (systemMode == SystemMode.OFF || ScheduleProcessJob.getSystemOccupancy() == Occupancy.UNOCCUPIED) {
                            relayState = 0;
                        } else
                        {
                            double humidity = VavSystemController.getInstance().getAverageSystemHumidity();
                            double targetMinHumidity = TunerUtil.readSystemUserIntentVal("target and min and inside and humidity");
                            double targetMaxHumidity = TunerUtil.readSystemUserIntentVal("target and max and inside and humidity");
                            double humidityHysteresis = TunerUtil.readTunerValByQuery("humidity and hysteresis", getSystemEquipRef());
                            currState = getCmdSignal("relay" + i);
                            if (stage == HUMIDIFIER)
                            {
                                //Humidification
                                if (humidity < targetMinHumidity)
                                {
                                    relayState = 1;
                                }
                                else if (humidity > (targetMinHumidity + humidityHysteresis))
                                {
                                    relayState = 0;
                                }
                                else
                                {
                                    relayState = currState;
                                }
                            }
                            else
                            {
                                //Dehumidification
                                if (humidity > targetMaxHumidity)
                                {
                                    relayState = 1;
                                }
                                else if (humidity < (targetMaxHumidity - humidityHysteresis))
                                {
                                    relayState = 0;
                                }
                                else
                                {
                                    relayState = currState;
                                }
                            }
                            CcuLog.d(L.TAG_CCU_SYSTEM, "humidity :" + humidity + " targetMinHumidity: " + targetMinHumidity + " humidityHysteresis: " + humidityHysteresis + " targetMaxHumidity: " + targetMaxHumidity);
                        }
                        break;
                }
            }
            stageStatus[stage.ordinal()] = (int)relayState;
            setCmdSignal("relay"+i, relayState);
            ControlMote.setRelayState("relay"+i, relayState);
            CcuLog.d(L.TAG_CCU_SYSTEM, stage+ " Set Relay"+i+", threshold: "+stageThreshold+", state : "+relayState);
        }
    
        setSystemPoint("operating and mode", VavSystemController.getInstance().systemState.ordinal());
        String systemStatus = getStatusMessage();
        String scheduleStatus =  ScheduleProcessJob.getSystemStatusString();
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
        
        status.append((stageStatus[FAN_1.ordinal()] > 0) ? "1":"");
        status.append((stageStatus[FAN_2.ordinal()] > 0) ? ",2":"");
        status.append((stageStatus[FAN_3.ordinal()] > 0) ? ",3":"");
        status.append((stageStatus[FAN_4.ordinal()] > 0) ? ",4":"");
        status.append((stageStatus[FAN_5.ordinal()] > 0) ? ",5":"");
        
        if (!status.toString().equals("")) {
            status.insert(0, "Fan Stage ");
            status.append(" ON ");
        }
        if (systemCoolingLoopOp > 0)
        {
            status.append("| Cooling Stage " + ((stageStatus[COOLING_1.ordinal()] > 0) ? "1" : ""));
            status.append((stageStatus[COOLING_2.ordinal()] > 0) ? ",2" : "");
            status.append((stageStatus[COOLING_3.ordinal()] > 0) ? ",3" : "");
            status.append((stageStatus[COOLING_4.ordinal()] > 0) ? ",4" : "");
            status.append((stageStatus[COOLING_5.ordinal()] > 0) ? ",5 ON " : " ON ");
        }
        
        if (systemHeatingLoopOp > 0) {
            status.append("| Heating Stage " + ((stageStatus[HEATING_1.ordinal()] > 0) ? "1" : ""));
            status.append((stageStatus[HEATING_2.ordinal()] > 0) ? ",2" : "");
            status.append((stageStatus[HEATING_3.ordinal()] > 0) ? ",3" : "");
            status.append((stageStatus[HEATING_4.ordinal()] > 0) ? ",4" : "");
            status.append((stageStatus[HEATING_5.ordinal()] > 0) ? ",5 ON" : " ON");
        }
    
    
        return status.toString().equals("")? "System OFF" : status.toString();
    }
    
    public void updateStagesSelected() {
       
        coolingStages = 0;
        heatingStages = 0;
        fanStages = 0;
        
        for (int i = 1; i < 8; i++)
        {
            if (getConfigEnabled("relay"+i) > 0)
            {
                int val = (int)getConfigAssociation("relay"+i);
                if (val <= Stage.COOLING_5.ordinal() && val >= coolingStages)
                {
                    coolingStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Cooling stage : "+coolingStages);
                } else if (val >= Stage.HEATING_1.ordinal() && val <= HEATING_5.ordinal() && val >= heatingStages)
                {
                    heatingStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Heating stage : "+heatingStages);
                } else if (val >= Stage.FAN_1.ordinal() && val <= Stage.FAN_5.ordinal() && val >= fanStages)
                {
                    fanStages = val + 1;
                    //CcuLog.d(L.TAG_CCU_SYSTEM," Fan stage : "+fanStages);
                }
            }
        }
        
        if ((heatingStages > 0)) {
            heatingStages -= Stage.HEATING_1.ordinal();
        }
        
        if (fanStages > 0) {
            fanStages -= Stage.FAN_1.ordinal();
        }
        
    }
    
    public boolean isStageEnabled(Stage s) {
        for (int i = 1; i < 8; i++)
        {
            if (getConfigEnabled("relay" + i) > 0)
            {
                int val = (int) getConfigAssociation("relay" + i);
                if (val == s.ordinal())  {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_STAGED_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
    
    public void addCmdPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        addCmdPoint(COOLING_1.displayName,"relay1", equipDis, siteRef, equipref, tz);
        addCmdPoint(COOLING_2.displayName,"relay2", equipDis, siteRef, equipref, tz);
        addCmdPoint(HEATING_1.displayName,"relay3", equipDis, siteRef, equipref, tz);
        addCmdPoint(HEATING_2.displayName,"relay4", equipDis, siteRef, equipref, tz);
        addCmdPoint(FAN_1.displayName,"relay5", equipDis, siteRef, equipref, tz);
        addCmdPoint(FAN_2.displayName,"relay6", equipDis, siteRef, equipref, tz);
        addCmdPoint(HUMIDIFIER.displayName,"relay7", equipDis, siteRef, equipref, tz);
    }
    
    private void addCmdPoint(String name, String relay, String equipDis, String siteRef, String equipref, String tz){
        //Name to be updated
        Point relay1Op = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+name)
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("system").addMarker("cmd").addMarker(relay).addMarker("his").addMarker("equipHis")
                                 .setTz(tz)
                                 .build();
        CCUHsApi.getInstance().addPoint(relay1Op);
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and his and "+cmd);
    }
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and his and "+cmd, val);
    }
    
    public void addConfigPoints(String equipref) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        addConfigPointEnabled("relay1", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay2", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay3", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay4", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay5", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay6", equipDis, siteRef, equipref, tz);
        addConfigPointEnabled("relay7", equipDis, siteRef, equipref, tz);
        addConfigPointAssociation("relay1", equipDis, siteRef, equipref, tz, Stage.COOLING_1);
        addConfigPointAssociation("relay2", equipDis, siteRef, equipref, tz, COOLING_2);
        addConfigPointAssociation("relay3", equipDis, siteRef, equipref, tz, Stage.HEATING_1);
        addConfigPointAssociation("relay4", equipDis, siteRef, equipref, tz, HEATING_2);
        addConfigPointAssociation("relay5", equipDis, siteRef, equipref, tz, Stage.FAN_1);
        addConfigPointAssociation("relay6", equipDis, siteRef, equipref, tz, FAN_2);
        addConfigPointAssociation("relay7", equipDis, siteRef, equipref, tz, Stage.HUMIDIFIER);
    
    }
    
    private void addConfigPointEnabled(String relay, String equipDis, String siteRef, String equipref, String tz) {
        Point relayEnabled = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+relay+"OutputEnabled")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker(relay)
                                            .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                            .setTz(tz)
                                            .build();
        String relayEnabledId = CCUHsApi.getInstance().addPoint(relayEnabled);
        CCUHsApi.getInstance().writeDefaultValById(relayEnabledId, 0.0 );
    }
    
    private void addConfigPointAssociation(String relay, String equipDis, String siteRef, String equipref, String tz, Stage init) {
        Point relayEnabled = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+relay+"OutputAssociation")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("system").addMarker("config").addMarker(relay)
                                     .addMarker("output").addMarker("association").addMarker("writable").addMarker("sp")
                                     .setTz(tz)
                                     .build();
        String relayEnabledId = CCUHsApi.getInstance().addPoint(relayEnabled);
        CCUHsApi.getInstance().writeDefaultValById(relayEnabledId, (double)init.ordinal() );
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
    
    public double getConfigAssociation(String config) {
        //return sysEquip.getRelayOpAssociation(config);
        //return CCUHsApi.getInstance().readDefaultVal("point and system and config and output and association and "+config);

        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap configPoint = hayStack.read("point and system and config and output and association and "+config);
        return hayStack.readPointPriorityVal(configPoint.get("id").toString());
    }
    public void setConfigAssociation(String config, double val) {
        //sysEquip.setRelayOpAssociation(config, val);
        HashMap cmd = CCUHsApi.getInstance().read("point and system and cmd and "+config);
        Stage updatedStage = Stage.values()[(int)val];
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        Point cmdPoint = new Point.Builder().setHashMap(cmd).setDisplayName(equipDis+"-"+updatedStage.displayName).build();
        CcuLog.d(L.TAG_CCU_SYSTEM, "updateDisplaName for Point "+cmdPoint.getDisplayName());
        CCUHsApi.getInstance().updatePoint(cmdPoint, cmdPoint.getId());
        CCUHsApi.getInstance().syncEntityTree();
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and output and association and "+config, val);
        
    }
    
    public void addTunerPoints(String equipref) {
        VavTRTuners.addSatTRTunerPoints(equipref);
        VavTRTuners.addStaticPressureTRTunerPoints(equipref);
        VavTRTuners.addCO2TRTunerPoints(equipref);
    }
    
}
