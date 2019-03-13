package a75f.io.logic.bo.building.system.vav;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemEquip;
import a75f.io.logic.bo.haystack.device.ControlMote;

import static a75f.io.logic.bo.building.system.vav.VavSystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.vav.VavSystemController.State.HEATING;

/**
 * Created by samjithsadasivan on 2/11/19.
 */

public class VavAdvancedHybridRtu extends VavStagedRtu
{
    private static final int ANALOG_SCALE = 10;
    
    @Override
    public String getProfileName()
    {
        return "VAV Advanced Hybrid RTU";
    }
    
    @Override
    public void addSystemEquip() {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap equip = hayStack.read("equip and system");
        if (equip != null && equip.size() > 0) {
            if (!equip.get("profile").equals(ProfileType.SYSTEM_VAV_HYBRID_RTU.name())) {
                hayStack.deleteEntityTree(equip.get("id").toString());
            } else {
                initTRSystem();
                sysEquip = new SystemEquip(equip.get("id").toString());
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
                                   .setProfile(ProfileType.SYSTEM_VAV_HYBRID_RTU.name())
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
        
        addAnalogConfigPoints(equipRef);
        addAnalogCmdPoints(equipRef);
        updateAhuRef(equipRef);
        sysEquip = new SystemEquip(equipRef);
        new ControlMote(siteRef);
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
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return (coolingStages > 0 || getConfigVal("analog1 and output and enabled") > 0);
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0 || getConfigVal("analog3 and output and enabled") > 0);
    }
    
    public synchronized void updateSystemPoints() {
        super.updateSystemPoints();
        
        double analogMin = getConfigVal("analog1 and cooling and min");
        double analogMax = getConfigVal("analog1 and cooling and max");
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: " + analogMin + " analogMax: " + analogMax + " systemCoolingLoopOp: " + systemCoolingLoopOp);
    
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
        if (getConfigEnabled("analog1") > 0)
        {
            ControlMote.setAnalogOut("analog1", signal);
        }
    
        analogMin = getConfigVal("analog2 and fan and min");
        analogMax = getConfigVal("analog2 and fan and max");
    
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
        if (getConfigEnabled("analog2") > 0)
        {
            ControlMote.setAnalogOut("analog2", signal);
        }
        
        
        analogMin = getConfigVal("analog3 and heating and min");
        analogMax = getConfigVal("analog3 and heating and max");
    
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" systemHeatingLoopOp : "+systemHeatingLoopOp);
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
        if (getConfigEnabled("analog3") > 0)
        {
            ControlMote.setAnalogOut("analog3", signal);
        }
    
        if (VavSystemController.getInstance().getSystemState() == COOLING)
        {
            analogMin = getConfigVal("analog4 and cooling and min");
            analogMax = getConfigVal("analog4 and cooling and max");
            if (analogMax > analogMin)
            {
                signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * systemCoolingLoopOp/100));
            } else {
                signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * systemCoolingLoopOp/100));
            }
        } else if (VavSystemController.getInstance().getSystemState() == HEATING)
        {
            analogMin = getConfigVal("analog4 and heating and min");
            analogMax = getConfigVal("analog4 and heating and max");
            if (analogMax > analogMin)
            {
                signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * systemHeatingLoopOp/100));
            } else {
                signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * systemHeatingLoopOp/100));
            }
        } else {
            signal = 0;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" Composite: "+signal);
        
        setCmdSignal("composite",signal);
        if (getConfigEnabled("analog4") > 0)
        {
            ControlMote.setAnalogOut("analog4", signal);
        }
        
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_HYBRID_RTU.name())) {
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
        Point analog1OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog1OutputEnabled")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipref)
                                                         .addMarker("system").addMarker("config").addMarker("analog1").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                         .setTz(tz).build();
        String analog1OutputEnabledId = hayStack.addPoint(analog1OutputEnabled);
        hayStack.writeDefaultValById(analog1OutputEnabledId, 0.0);
        Point analog2OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog2OutputEnabled")
                                                        .setSiteRef(siteRef).setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog2").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setTz(tz).build();
        String analog2OutputEnabledId = hayStack.addPoint(analog2OutputEnabled);
        hayStack.writeDefaultValById(analog2OutputEnabledId, 0.0);
        Point analog3OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog3OutputEnabled")
                                                        .setSiteRef(siteRef)
                                                        .setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog3").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setTz(tz).build();
        String analog3OutputEnabledId = hayStack.addPoint(analog3OutputEnabled);
        hayStack.writeDefaultValById(analog3OutputEnabledId, 0.0);
        Point analog4OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog4OutputEnabled")
                                                        .setSiteRef(siteRef)
                                                        .setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog4").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setTz(tz).build();
        String analog4OutputEnabledId = hayStack.addPoint(analog4OutputEnabled);
        hayStack.writeDefaultValById(analog4OutputEnabledId, 0.0);
    
        Point analog1AtMinCooling = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog1AtMinCooling")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("min").addMarker("cooling").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog1AtMinCoolingId = hayStack.addPoint(analog1AtMinCooling);
        hayStack.writeDefaultValById(analog1AtMinCoolingId, 2.0 );
    
        Point analog1AtMaxCooling = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"analog1AtMaxCooling")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipref)
                                               .addMarker("system").addMarker("config").addMarker("analog1")
                                               .addMarker("max").addMarker("cooling").addMarker("writable").addMarker("sp")
                                               .setUnit("V")
                                               .setTz(tz)
                                               .build();
        String analog1AtMaxCoolingId = hayStack.addPoint(analog1AtMaxCooling);
        hayStack.writeDefaultValById(analog1AtMaxCoolingId, 10.0 );
    
        Point analog2AtMinFan = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog2AtMinFan")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("min").addMarker("fan").addMarker("writable").addMarker("sp")
                                                   .setUnit("V")
                                                   .setTz(tz)
                                                   .build();
        String analog2AtMinFanId = hayStack.addPoint(analog2AtMinFan);
        hayStack.writeDefaultValById(analog2AtMinFanId, 2.0 );
    
        Point analog2AtMaxFan = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"analog2AtMaxFan")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipref)
                                                   .addMarker("system").addMarker("config").addMarker("analog2")
                                                   .addMarker("max").addMarker("fan").addMarker("writable").addMarker("sp")
                                                   .setUnit("V")
                                                   .setTz(tz)
                                                   .build();
        String analog2AtMaxFanId = hayStack.addPoint(analog2AtMaxFan);
        hayStack.writeDefaultValById(analog2AtMaxFanId, 10.0 );
    
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
    
        Point analog4AtMinCooling = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"analog4AtMinCooling")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("system").addMarker("config").addMarker("analog4")
                                        .addMarker("min").addMarker("cooling").addMarker("writable").addMarker("sp")
                                        .setUnit("V")
                                        .setTz(tz)
                                        .build();
        String analog4AtMinCoolingId = hayStack.addPoint(analog4AtMinCooling);
        hayStack.writeDefaultValById(analog4AtMinCoolingId, 7.0 );
    
        Point analog4AtMaxCooling = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"analog4AtMaxCooling")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .addMarker("system").addMarker("config").addMarker("analog4")
                                        .addMarker("max").addMarker("cooling").addMarker("writable").addMarker("sp")
                                        .setUnit("V")
                                        .setTz(tz)
                                        .build();
        String analog4AtMaxCoolingId = hayStack.addPoint(analog4AtMaxCooling);
        hayStack.writeDefaultValById(analog4AtMaxCoolingId, 10.0 );
    
        Point analog4AtMinHeating = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog4AtMinHeating")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog4")
                                            .addMarker("min").addMarker("heating").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog4AtMinHeatingId = hayStack.addPoint(analog4AtMinHeating);
        hayStack.writeDefaultValById(analog4AtMinHeatingId, 5.0 );
    
        Point analog4AtMaxHeating = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"analog4AtMaxHeating")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipref)
                                            .addMarker("system").addMarker("config").addMarker("analog4")
                                            .addMarker("max").addMarker("heating").addMarker("writable").addMarker("sp")
                                            .setUnit("V")
                                            .setTz(tz)
                                            .build();
        String analog4AtMaxHeatingId = hayStack.addPoint(analog4AtMaxHeating);
        hayStack.writeDefaultValById(analog4AtMaxHeatingId, 2.0 );
    }
    
    public double getConfigVal(String tags) {
        
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
    
    private void addAnalogCmdPoints(String equipref)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString() + "-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        Point coolingSignal = new Point.Builder().setDisplayName(equipDis + "-" + "coolingSignal")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipref)
                                                 .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("his").addMarker("equipHis")
                                                 .setTz(tz).build();
        CCUHsApi.getInstance().addPoint(coolingSignal);
        Point heatingSignal = new Point.Builder().setDisplayName(equipDis + "-" + "heatingSignal")
                                                 .setSiteRef(siteRef).setEquipRef(equipref)
                                                 .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("his").addMarker("equipHis")
                                                 .setTz(tz).build();
        CCUHsApi.getInstance().addPoint(heatingSignal);
        Point fanSignal = new Point.Builder().setDisplayName(equipDis + "-" + "fanSignal")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("his").addMarker("equipHis")
                                             .setTz(tz).build();
        CCUHsApi.getInstance().addPoint(fanSignal);
        Point compositeSignal = new Point.Builder().setDisplayName(equipDis + "-" + "CompositeSignal")
                                                   .setSiteRef(siteRef).setEquipRef(equipref)
                                                   .addMarker("system").addMarker("cmd").addMarker("composite").addMarker("his").addMarker("equipHis")
                                                   .setTz(tz).build();
        CCUHsApi.getInstance().addPoint(compositeSignal);
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
}
