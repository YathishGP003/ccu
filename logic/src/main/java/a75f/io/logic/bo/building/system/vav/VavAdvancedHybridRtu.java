package a75f.io.logic.bo.building.system.vav;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.ControlMote;

import static a75f.io.logic.bo.building.hvac.Stage.COOLING_1;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_2;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_3;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_4;
import static a75f.io.logic.bo.building.hvac.Stage.COOLING_5;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_1;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_2;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_3;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_4;
import static a75f.io.logic.bo.building.hvac.Stage.HEATING_5;
import static a75f.io.logic.bo.building.system.SystemController.State.COOLING;
import static a75f.io.logic.bo.building.system.SystemController.State.HEATING;

/**
 * Created by samjithsadasivan on 2/11/19.
 */

public class VavAdvancedHybridRtu extends VavStagedRtu
{
    private static final int ANALOG_SCALE = 10;
    
    @Override
    public String getProfileName()
    {
        return "VAV Advanced Hybrid AHU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_HYBRID_RTU;
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
                //sysEquip = new SystemEquip(equip.get("id").toString());
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
        //sysEquip = new SystemEquip(equipRef);
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
        setTrTargetVals();
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return (coolingStages > 0 || getConfigVal("analog1 and output and enabled") > 0);
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return (heatingStages > 0 || getConfigVal("analog3 and output and enabled") > 0);
    }
    
    @Override
    public boolean isCoolingActive(){
        return stageStatus[COOLING_1.ordinal()] > 0 || stageStatus[COOLING_2.ordinal()] > 0 || stageStatus[COOLING_3.ordinal()] > 0
               || stageStatus[COOLING_4.ordinal()] > 0 || stageStatus[COOLING_5.ordinal()] > 0 || systemCoolingLoopOp > 0;
    }
    
    @Override
    public boolean isHeatingActive(){
        return stageStatus[HEATING_1.ordinal()] > 0 || stageStatus[HEATING_2.ordinal()] > 0 || stageStatus[HEATING_3.ordinal()] > 0
               || stageStatus[HEATING_4.ordinal()] > 0 || stageStatus[HEATING_5.ordinal()] > 0 || systemHeatingLoopOp > 0;
    }
    
    
    public synchronized void updateSystemPoints() {
        super.updateSystemPoints();
        updateOutsideWeatherParams();
        
        int signal;
        double analogMin = 0, analogMax = 0;
        if (getConfigEnabled("analog1") > 0)
        {
            analogMin = getConfigVal("analog1 and cooling and min");
            analogMax = getConfigVal("analog1 and cooling and max");
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog1Min: " + analogMin + " analog1Max: " + analogMax + " systemCoolingLoopOp: " + systemCoolingLoopOp);
    
    
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
        
        if (getConfigEnabled("analog2") > 0)
        {
            analogMin = getConfigVal("analog2 and fan and min");
            analogMax = getConfigVal("analog2 and fan and max");
    
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
        
        if (getConfigEnabled("analog3") > 0)
        {
            analogMin = getConfigVal("analog3 and heating and min");
            analogMax = getConfigVal("analog3 and heating and max");
    
            CcuLog.d(L.TAG_CCU_SYSTEM, "analog3Min: "+analogMin+" analog3Max: "+analogMax+" systemHeatingLoopOp : "+systemHeatingLoopOp);
            if (analogMax > analogMin)
            {
                signal = (int) (ANALOG_SCALE * (analogMin + (analogMax - analogMin) * (systemHeatingLoopOp / 100)));
            }
            else
            {
                signal = (int) (ANALOG_SCALE * (analogMin - (analogMin - analogMax) * (systemHeatingLoopOp / 100)));
            }
        } else  {
            signal = 0;
        }
        setCmdSignal("heating", signal);
        ControlMote.setAnalogOut("analog3", signal);
        
        if (getConfigEnabled("analog4") > 0)
        {
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
                double coolingMin = getConfigVal("analog4 and cooling and min");
                double heatingMin = getConfigVal("analog4 and heating and min");
        
                signal = (int) (ANALOG_SCALE * (coolingMin + heatingMin) /2);
            }
            CcuLog.d(L.TAG_CCU_SYSTEM, "analogMin: "+analogMin+" analogMax: "+analogMax+" Composite: "+signal);
        } else {
            signal = 0;
            
        }
        setCmdSignal("composite",signal);
        ControlMote.setAnalogOut("analog4", signal);
        
    }
    
    @Override
    public String getStatusMessage(){
        StringBuilder status = new StringBuilder();
    
        if (getConfigEnabled("analog2") > 0)
        {
            status.append(systemFanLoopOp > 0 ? " Fan ON " : "");
        }
        if (getConfigEnabled("analog1") > 0)
        {
            status.append(systemCoolingLoopOp > 0 ? "| Cooling ON " : "");
        }
        if (getConfigEnabled("analog3") > 0)
        {
            status.append(systemHeatingLoopOp > 0 ? "| Heating ON " : "");
        }
        
        if (!status.toString().equals("")) {
            status.insert(0, super.getStatusMessage()+" ; Analog ");
        } else {
            status.append(super.getStatusMessage());
        }
    
        return status.toString().equals("")? "OFF" : status.toString();
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
                                                         .setEnums("false,true").setTz(tz).build();
        String analog1OutputEnabledId = hayStack.addPoint(analog1OutputEnabled);
        hayStack.writeDefaultValById(analog1OutputEnabledId, 0.0);
        Point analog2OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog2OutputEnabled")
                                                        .setSiteRef(siteRef).setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog2").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setEnums("false,true").setTz(tz).build();
        String analog2OutputEnabledId = hayStack.addPoint(analog2OutputEnabled);
        hayStack.writeDefaultValById(analog2OutputEnabledId, 0.0);
        Point analog3OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog3OutputEnabled")
                                                        .setSiteRef(siteRef)
                                                        .setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog3").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setEnums("false,true").setTz(tz).build();
        String analog3OutputEnabledId = hayStack.addPoint(analog3OutputEnabled);
        hayStack.writeDefaultValById(analog3OutputEnabledId, 0.0);
        Point analog4OutputEnabled = new Point.Builder().setDisplayName(equipDis + "-" + "analog4OutputEnabled")
                                                        .setSiteRef(siteRef)
                                                        .setEquipRef(equipref)
                                                        .addMarker("system").addMarker("config").addMarker("analog4").addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                                        .setEnums("false,true").setTz(tz).build();
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
        Point coolingSignal = new Point.Builder().setDisplayName(equipDis + "-" + "coolingSignal")
                                                 .setSiteRef(siteRef)
                                                 .setEquipRef(equipref)
                                                 .addMarker("system").addMarker("cmd").addMarker("cooling").addMarker("modulating").addMarker("analog1").addMarker("his").addMarker("equipHis")
                                                 .setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(coolingSignal);
        Point heatingSignal = new Point.Builder().setDisplayName(equipDis + "-" + "heatingSignal")
                                                 .setSiteRef(siteRef).setEquipRef(equipref)
                                                 .addMarker("system").addMarker("cmd").addMarker("heating").addMarker("modulating").addMarker("analog3").addMarker("his").addMarker("equipHis")
                                                 .setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(heatingSignal);
        Point fanSignal = new Point.Builder().setDisplayName(equipDis + "-" + "fanSignal")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipref)
                                             .addMarker("system").addMarker("cmd").addMarker("fan").addMarker("analog2").addMarker("his").addMarker("modulating").addMarker("equipHis")
                                             .setTz(tz).build();
        CCUHsApi.getInstance().addPoint(fanSignal);
        Point compositeSignal = new Point.Builder().setDisplayName(equipDis + "-" + "CompositeSignal")
                                                   .setSiteRef(siteRef).setEquipRef(equipref)
                                                   .addMarker("system").addMarker("cmd").addMarker("composite").addMarker("analog4").addMarker("his").addMarker("equipHis")
                                                   .setUnit("%").setTz(tz).build();
        CCUHsApi.getInstance().addPoint(compositeSignal);
    }
    
    public double getCmdSignal(String cmd) {
        return CCUHsApi.getInstance().readHisValByQuery("point and system and cmd and "+cmd);
    }
    
    public void setCmdSignal(String cmd, double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and system and cmd and "+cmd, val);
    }
}
