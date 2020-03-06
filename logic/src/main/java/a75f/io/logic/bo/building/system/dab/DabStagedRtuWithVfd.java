package a75f.io.logic.bo.building.system.dab;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.haystack.device.ControlMote;

import static a75f.io.logic.bo.building.hvac.Stage.COOLING_5;
import static a75f.io.logic.bo.building.hvac.Stage.FAN_1;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_5;

public class DabStagedRtuWithVfd extends DabStagedRtu
{
    
    public String getProfileName() {
        return "DAB Staged RTU with VFD Fan";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_DAB_STAGED_VFD_RTU;
    }
    
    @Override
    public void doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo();
        updateSystemPoints();
    }
    @Override
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
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
                                   .setProfile(ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name())
                                   .addMarker("equip").addMarker("system").addMarker("dab")
                                   .addMarker("equipHis")
                                   .setTz(siteMap.get("tz").toString())
                                   .build();
        String equipRef = hayStack.addEquip(systemEquip);
        
        addSystemLoopOpPoints(equipRef);
        addUserIntentPoints(equipRef);
        addCmdPoints(equipRef);
        addConfigPoints(equipRef);
        addDabSystemTuners(equipRef);
        addAnalogConfigPoints(equipRef);
        addAnalogCmdPoints(equipRef);
        updateAhuRef(equipRef);
        
        new ControlMote(siteRef);
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public synchronized void updateSystemPoints()
    {
        super.updateSystemPoints();
        updateOutsideWeatherParams();
        
        double signal = 0;
        if (getConfigEnabled("analog2") > 0)
        {
            if (isCoolingActive())
            {
                for (int i = 1; i < 8; i++)
                {
                    if (getConfigEnabled("relay" + i) > 0)
                    {
                        int val = (int) getConfigAssociation("relay" + i);
                        if (val <= Stage.COOLING_5.ordinal())
                        {
                            if(getCmdSignal("cooling and stage" +Stage.values()[val]+1) > 0)
                                signal = getConfigVal("analog2 and cooling and stage" + (val + 1));
                        }
                    }
                }
            }
            else if (isHeatingActive())
            {
                for (int i = 1; i < 8; i++)
                {
                    if (getConfigEnabled("relay" + i) > 0 )
                    {
                        int val = (int) getConfigAssociation("relay" + i);
                        if (val >= Stage.HEATING_1.ordinal() && val <= Stage.HEATING_5.ordinal())
                        {
                            if(getCmdSignal("heating and stage" + (Stage.values()[val].ordinal() - COOLING_5.ordinal())) > 0)
                                signal = getConfigVal("analog2 and heating and stage" + (val - Stage.HEATING_1.ordinal() + 1));
                        }
                    }
                }
            }
            else if (stageStatus[FAN_1.ordinal()] > 0)
            {
                for (int i = 1; i < 8; i++)
                {
                    if (getConfigEnabled("relay" + i) > 0 && getCmdSignal("fan and stage1") > 0)
                    {
                        int val = (int) getConfigAssociation("relay" + i);
                        if (val == Stage.FAN_1.ordinal())
                        {
                            signal = getConfigVal("analog2 and recirculate");
                        }
                    }
                }
            }
        }
        if(signal != getCmdSignal("fan and modulating"))
            setCmdSignal("fan and modulating",10*signal);
        ControlMote.setAnalogOut("analog2", 10 * signal);
        CcuLog.d(L.TAG_CCU_SYSTEM, " analog2 Signal : "+10 * signal);
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_DAB_STAGED_VFD_RTU.name())) {
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
                                                        .setEnums("false,true").setTz(tz).build();
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
                                .setTz(tz)
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
    
    private void addAnalogCmdPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        Point coolingSignal = new Point.Builder().setDisplayName(equipDis + "-" + "anlog2Signal").setSiteRef(siteRef).setEquipRef(equipref).setHisInterpolate("cov").addMarker("system").addMarker("cmd").addMarker("fan").addMarker("modulating").addMarker("his").addMarker("equipHis").setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(coolingSignal);
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
}
