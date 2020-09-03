package a75f.io.logic.bo.building.system.vav;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.EpidemicState;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.haystack.device.ControlMote;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.hvac.Stage.COOLING_5;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_1;

/**
 * Created by samjithsadasivan on 2/18/19.
 */

public class VavStagedRtuWithVfd extends VavStagedRtu
{
    private static final int ANALOG_SCALE = 10;
    private static final int MAX_RELAY_COUNT = 8;
    
    @Override
    public String getProfileName()
    {
        return "VAV Staged RTU with VFD Fan";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_STAGED_VFD_RTU;
    }
    
    @Override
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                initTRSystem();
                addNewSystemUserIntentPoints(equip.get("id").toString());
                updateStagesSelected();
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
                                   .setProfile(ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name())
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
        
        addAnalogConfigPoints(equipRef);
        if (getConfigEnabled("analog2") > 0){
            addAnalogCmdPoints(equipRef);
        }
        updateAhuRef(equipRef);
        //sysEquip = new SystemEquip(equipRef);
        new ControlMote(equipRef);
        initTRSystem();
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
        
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
        return (coolingStages > 0 );
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0);
    }
    
    public synchronized void updateSystemPoints()
    {
        super.updateSystemPoints();
        boolean isEconomizingAvailable = CCUHsApi.getInstance().readHisValByQuery("point and oao and economizing and available") > 0.0;
        double epidemicMode = CCUHsApi.getInstance().readHisValByQuery("point and sp and system and epidemic and state and mode and equipRef ==\""+getSystemEquipRef()+"\"");
        EpidemicState epidemicState = EpidemicState.values()[(int) epidemicMode];
        double signal = 0;
        if (getConfigEnabled("analog2") > 0)
        {
            if (isCoolingActive())
            {
                for (int relayCount = 1; relayCount < MAX_RELAY_COUNT; relayCount++)
                {
                    if (getConfigEnabled("relay" + relayCount) > 0)
                    {
                        int val = (int) getConfigAssociation("relay" + relayCount);
                        if (val <= Stage.COOLING_5.ordinal())
                        {
                            if(  getCmdSignal("cooling and stage" + (Stage.values()[val].ordinal()+1)) > 0)
                                signal = getConfigVal("analog2 and cooling and stage" + (val + 1));
                        }
                    }
                }
            }
            else if (isHeatingActive())
            {
                for (int relayCount = 1; relayCount < MAX_RELAY_COUNT; relayCount++)
                {
                    if (getConfigEnabled("relay" + relayCount) > 0 )
                    {
                        int val = (int) getConfigAssociation("relay" + relayCount);
                        if (val >= Stage.HEATING_1.ordinal() && val <= Stage.HEATING_5.ordinal())
                        {
                            if(getCmdSignal("heating and stage" + (Stage.values()[val].ordinal() - COOLING_5.ordinal())) > 0)
                                signal = getConfigVal("analog2 and heating and stage" + (val - Stage.HEATING_1.ordinal() + 1));
                        }
                    }
                }
            } else if (isEconomizingAvailable && (systemCoolingLoopOp > 0)){
                signal = getConfigVal("analog2 and economizer");
            }
            else if (stageStatus[FAN_1.ordinal()] > 0)
            {
                for (int relayCount = 1; relayCount < MAX_RELAY_COUNT; relayCount++)
                {
                    if (getConfigEnabled("relay" + relayCount) > 0 && getCmdSignal("fan and stage1") > 0)
                    {
                        int val = (int) getConfigAssociation("relay" + relayCount);
                        if (val == Stage.FAN_1.ordinal())
                        {
                            signal = getConfigVal("analog2 and recirculate");
                        }
                    }
                }
            }
            else {
                //For all other cases analog2-out should be the minimum config value
                signal = getConfigVal("analog2 and default");
            }
            
            if((epidemicState == EpidemicState.PREPURGE) && L.ccu().oaoProfile != null){
                double smartPrePurgeFanSpeed = TunerUtil.readTunerValByQuery("system and prePurge and fan and speed", L.ccu().oaoProfile.getEquipRef());
                signal = Math.max(signal,smartPrePurgeFanSpeed / ANALOG_SCALE);
            }else if((epidemicState == EpidemicState.POSTPURGE) && L.ccu().oaoProfile != null){
                double smartPurgeFanLoopOp = TunerUtil.readTunerValByQuery("system and postPurge and fan and speed", L.ccu().oaoProfile.getEquipRef());
                signal = Math.max(signal,smartPurgeFanLoopOp / ANALOG_SCALE);
            }
            
            signal = ANALOG_SCALE * signal;
            if (signal != getCmdSignal("fan and modulating")) {
                setCmdSignal("fan and modulating",signal);
            }
        }
        
        ControlMote.setAnalogOut("analog2", signal);
        CcuLog.d(L.TAG_CCU_SYSTEM, " analog2 Signal : "+ signal);
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_STAGED_VFD_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
    
    private void addAnalogConfigPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
    
        Point analog2OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog2OutputEnabled")
                                                        .setSiteRef(siteRef).setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog2").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setTz(tz).build();
        String analog2OutputEnabledId = hayStack.addPoint(analog2OutputEnabled);
        hayStack.writeDefaultValById(analog2OutputEnabledId, 0.0);
        
        Point analog2AtEconomizer = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog2AtEconomizer")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog2")
                                            .addMarker("economizer").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog2AtEconomizerId = hayStack.addPoint(analog2AtEconomizer);
        hayStack.writeDefaultValById(analog2AtEconomizerId, 7.0 );
        Point analog2AtRecirculate = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog2AtRecirculate")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog2")
                                            .addMarker("recirculate").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog2AtRecirculateId = hayStack.addPoint(analog2AtRecirculate);
        hayStack.writeDefaultValById(analog2AtRecirculateId, 4.0 );
    
        Point analog2Default = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"analog2Default")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipref)
                                     .addMarker("system").addMarker("config").addMarker("analog2")
                                     .addMarker("default").addMarker("writable").addMarker("sp")
                                     .setUnit("V")
                                     .setTz(tz)
                                     .build();
        String analog2DefaultId = hayStack.addPoint(analog2Default);
        hayStack.writeDefaultValById(analog2DefaultId, 0.0 );
        
        addConfigPointForStage(siteMap, equipref, "analog2AtCoolStage1", "cooling", "stage1", 7);
        addConfigPointForStage(siteMap, equipref, "analog2AtCoolStage2", "cooling", "stage2", 10);
        addConfigPointForStage(siteMap, equipref, "analog2AtCoolStage3", "cooling", "stage3", 10);
        addConfigPointForStage(siteMap, equipref, "analog2AtCoolStage4", "cooling", "stage4", 10);
        addConfigPointForStage(siteMap, equipref, "analog2AtCoolStage5", "cooling", "stage5", 10);
    
        addConfigPointForStage(siteMap, equipref, "analog2AtHeatStage1", "heating", "stage1", 7);
        addConfigPointForStage(siteMap, equipref, "analog2AtHeatStage2", "heating", "stage2", 10);
        addConfigPointForStage(siteMap, equipref, "analog2AtHeatStage3", "heating", "stage3", 10);
        addConfigPointForStage(siteMap, equipref, "analog2AtHeatStage4", "heating", "stage4", 10);
        addConfigPointForStage(siteMap, equipref, "analog2AtHeatStage5", "heating", "stage5", 10);
    
    }
    
    private void addConfigPointForStage(HashMap siteMap, String equipref,String name, String coolheat, String stage, double val) {
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
    
        Point analog2 = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+name)
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .addMarker("system").addMarker("config").addMarker("analog2")
                                 .addMarker(coolheat).addMarker(stage).addMarker("writable").addMarker("sp")
                                 .setUnit("V")
                                 .setEnums("false,true").setTz(tz)
                                 .build();
        String analog2Id = CCUHsApi.getInstance().addPoint(analog2);
        CCUHsApi.getInstance().writeDefaultValById(analog2Id, val );
    }
    
    public double getConfigVal(String tags) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and config and "+tags);
        return hayStack.readPointPriorityVal(cdb.get("id").toString());
    }
    
    public void setConfigVal(String tags, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and config and "+tags, val);
    }
    
    public void addAnalogCmdPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        Point analog2Signal = new Point.Builder().setDisplayName(equipDis + "-" + "FanSignal").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("fan").addMarker("modulating").addMarker("his").setUnit("%").setTz(tz).build();
        String cmdPointAnalogId = CCUHsApi.getInstance().addPoint(analog2Signal);
        CCUHsApi.getInstance().writeHisValById(cmdPointAnalogId, 0.0);
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
}
