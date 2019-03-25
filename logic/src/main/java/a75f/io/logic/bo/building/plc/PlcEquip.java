package a75f.io.logic.bo.building.plc;

import java.util.HashMap;

import a75.io.algos.GenericPIController;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by samjithsadasivan on 2/25/19.
 */

public class PlcEquip
{
    ProfileType profileType ;
    int nodeAddr;
    
    GenericPIController plc;
    String equipRef = null;
    
    double targetValue;
    double spSensorOffset;
    boolean isEnabledAnalog2InForSp;
    boolean isEnabledZeroErrorMidpoint;
    
    CCUHsApi hayStack = CCUHsApi.getInstance();
    public PlcEquip(ProfileType type, int node) {
        profileType = type;
        nodeAddr = node;
    }
    
    public void init() {
        if (equipRef == null) {
            HashMap equip = hayStack.read("equip and pid and group == \""+nodeAddr+"\"");
            equipRef = equip.get("id").toString();
        }
        plc = new GenericPIController();
        plc.setMaxAllowedError(hayStack.readDefaultVal("point and proportional and range and equipRef == \""+equipRef+"\""));
        plc.setIntegralGain(TunerUtil.readTunerValByQuery("pid and igain and equipRef == \""+equipRef+"\""));
        plc.setProportionalGain(TunerUtil.readTunerValByQuery("pid and pgain and equipRef == \""+equipRef+"\""));
        plc.setIntegralMaxTimeout((int)TunerUtil.readTunerValByQuery("pid and itimeout and equipRef == \""+equipRef+"\""));
        
        targetValue = hayStack.readDefaultVal("point and config and target and value and equipRef == \""+equipRef+"\"");
        spSensorOffset = hayStack.readDefaultVal("point and config and OCCUPIED and sensor and offset and equipRef == \""+equipRef+"\"");
        isEnabledAnalog2InForSp = hayStack.readDefaultVal("point and config and enabled and analog2 and OCCUPIED and equipRef == \""+equipRef+"\"") > 0;
        isEnabledZeroErrorMidpoint = hayStack.readDefaultVal("point and config and enabled and zero and error and midpoint and equipRef == \""+equipRef+"\"") > 0;
    
    }
    
    public GenericPIController getPIController() {
        return plc;
    }
    
    public void createEntities(PlcProfileConfiguration config, String floorRef, String roomRef) {
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis+"-PID-"+nodeAddr;
        String ahuRef = null;
        HashMap systemEquip = hayStack.read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            ahuRef = systemEquip.get("id").toString();
        }
        
        Equip b = new Equip.Builder()
                                  .setSiteRef(siteRef)
                                  .setDisplayName(equipDis)
                                  .setRoomRef(roomRef)
                                  .setFloorRef(floorRef)
                                  .setProfile(profileType.name())
                                  .addMarker("equip").addMarker("pid").addMarker("zone").addMarker("equipHis")
                                  .setAhuRef(ahuRef)
                                  .setTz(tz)
                                  .setGroup(String.valueOf(nodeAddr)).build();
    
        equipRef = hayStack.addEquip(b);
        BuildingTuners.getInstance().addEquipPlcTuners(siteDis + "-PID-" + nodeAddr, equipRef);
    
        Point analog1InputSensor = new Point.Builder()
                                   .setDisplayName(equipDis+"-analog1InputSensor")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef)
                                   .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                   .addMarker("analog1").addMarker("input").addMarker("sensor")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String analog1InputSensorId = hayStack.addPoint(analog1InputSensor);
        hayStack.writeDefaultValById(analog1InputSensorId, (double)config.analog1InputSensor);
    
        Point th1InputSensor = new Point.Builder()
                                           .setDisplayName(equipDis+"-th1InputSensor")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("config").addMarker("pid").addMarker("th1").addMarker("zone").addMarker("writable")
                                           .addMarker("th1").addMarker("input").addMarker("sensor")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .build();
        String th1InputSensorId = hayStack.addPoint(th1InputSensor);
        hayStack.writeDefaultValById(th1InputSensorId, (double)config.th1InputSensor);
    
        Point pidTargetValue = new Point.Builder()
                                       .setDisplayName(equipDis+"-pidTargetValue")
                                       .setEquipRef(equipRef)
                                       .setSiteRef(siteRef)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef)
                                       .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                       .addMarker("target").addMarker("value")
                                       .setGroup(String.valueOf(nodeAddr))
                                       .setTz(tz)
                                       .build();
        String pidTargetValueId = hayStack.addPoint(pidTargetValue);
        hayStack.writeDefaultValById(pidTargetValueId, (double)config.pidTargetValue);
    
        Point pidProportionalRange = new Point.Builder()
                                       .setDisplayName(equipDis+"-pidProportionalRange")
                                       .setEquipRef(equipRef)
                                       .setSiteRef(siteRef)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef)
                                       .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                       .addMarker("proportional").addMarker("range")
                                       .setGroup(String.valueOf(nodeAddr))
                                       .setTz(tz)
                                       .build();
        String pidProportionalRangeId = hayStack.addPoint(pidProportionalRange);
        hayStack.writeDefaultValById(pidProportionalRangeId, (double)config.pidProportionalRange);
    
        Point analog1AtMinOutput = new Point.Builder()
                                             .setDisplayName(equipDis+"-analog1AtMinOutput")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef)
                                             .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                             .addMarker("analog1").addMarker("min").addMarker("output")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("V")
                                             .setTz(tz)
                                             .build();
        String analog1AtMinOutputId = hayStack.addPoint(analog1AtMinOutput);
        hayStack.writeDefaultValById(analog1AtMinOutputId, config.analog1AtMinOutput);
    
        Point analog1AtMaxOutput = new Point.Builder()
                                           .setDisplayName(equipDis+"-analog1AtMaxOutput")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                           .addMarker("analog1").addMarker("max").addMarker("output")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setUnit("V")
                                           .setTz(tz)
                                           .build();
        String analog1AtMaxOutputId = hayStack.addPoint(analog1AtMaxOutput);
        hayStack.writeDefaultValById(analog1AtMaxOutputId, config.analog1AtMaxOutput);
    
        Point useAnalogIn2ForSetpoint = new Point.Builder()
                                           .setDisplayName(equipDis+"-useAnalogIn2ForSetpoint")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                           .addMarker("enabled").addMarker("analog2").addMarker("OCCUPIED")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .build();
        String useAnalogIn2ForSetpointId = hayStack.addPoint(useAnalogIn2ForSetpoint);
        hayStack.writeDefaultValById(useAnalogIn2ForSetpointId, config.useAnalogIn2ForSetpoint ? 1.0: 0);
    
        Point analog2InputSensor = new Point.Builder()
                                           .setDisplayName(equipDis+"-analog2InputSensor")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                           .addMarker("analog2").addMarker("input").addMarker("sensor")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .build();
        String analog2InputSensorId = hayStack.addPoint(analog2InputSensor);
        hayStack.writeDefaultValById(analog2InputSensorId, (double)config.analog2InputSensor);
    
        Point setpointSensorOffset = new Point.Builder()
                                           .setDisplayName(equipDis+"-setpointSensorOffset")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                           .addMarker("OCCUPIED").addMarker("sensor").addMarker("offset")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .build();
        String setpointSensorOffsetId = hayStack.addPoint(setpointSensorOffset);
        hayStack.writeDefaultValById(setpointSensorOffsetId, (double)config.setpointSensorOffset);
    
        Point expectZeroErrorAtMidpoint  = new Point.Builder()
                                             .setDisplayName(equipDis+"-setpointSensorOffset")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef)
                                             .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                             .addMarker("enabled").addMarker("zero").addMarker("error").addMarker("midpoint")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setTz(tz)
                                             .build();
        String expectZeroErrorAtMidpointId  = hayStack.addPoint(expectZeroErrorAtMidpoint );
        hayStack.writeDefaultValById(expectZeroErrorAtMidpointId, config.expectZeroErrorAtMidpoint ? 1.0 : 0);
    
        Point processVariable  = new Point.Builder()
                                           .setDisplayName(equipDis+"-processVariable")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical").addMarker("equipHis")
                                           .addMarker("process").addMarker("variable")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .build();
        String processVariableId  = hayStack.addPoint(processVariable );
        hayStack.writeHisValById(processVariableId, 0.0);
    
        Point controlVariable  = new Point.Builder()
                                         .setDisplayName(equipDis+"-controlVariable")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef)
                                         .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical").addMarker("equipHis")
                                         .addMarker("control").addMarker("variable")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String controlVariableId  = hayStack.addPoint(controlVariable );
        hayStack.writeHisValById(controlVariableId, 0.0);
    
        Point setpointVariable  = new Point.Builder()
                                         .setDisplayName(equipDis+"-setpointVariable")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef)
                                         .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical").addMarker("equipHis")
                                         .addMarker("OCCUPIED").addMarker("variable")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String setpointVariableId  = hayStack.addPoint(setpointVariable );
        hayStack.writeHisValById(setpointVariableId, 0.0);
    
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
    
        if (config.analog1InputSensor > 0) {
            device.analog1In.setPointRef(processVariableId);
            device.analog1In.setEnabled(true);
            device.analog1In.setType(String.valueOf(config.analog1InputSensor-1));
        } else
        {
            device.th1In.setPointRef(processVariableId);
            device.th1In.setEnabled(true);
            device.th1In.setType(String.valueOf(config.th1InputSensor-1));
        }
        
        if (config.useAnalogIn2ForSetpoint)
        {
            device.analog2In.setPointRef(setpointVariableId);
            device.analog2In.setEnabled(true);
            device.analog2In.setType(String.valueOf(config.analog2InputSensor));
        }
        
        device.analog1Out.setPointRef(controlVariableId);
        device.analog1Out.setType(config.analog1AtMinOutput+"-"+config.analog1AtMaxOutput);
        device.analog1Out.setEnabled(true);
        device.addPointsToDb();
        
        hayStack.syncEntityTree();
    }
    
    public PlcProfileConfiguration getProfileConfiguration() {
        PlcProfileConfiguration p = new PlcProfileConfiguration();
        p.analog1InputSensor = hayStack.readDefaultVal("point and config and analog1 and input and sensor and equipRef == \""+equipRef+"\"").intValue();
        p.th1InputSensor = hayStack.readDefaultVal("point and config and th1 and input and sensor and equipRef == \""+equipRef+"\"").intValue();
        p.pidTargetValue = hayStack.readDefaultVal("point and config and target and value and equipRef == \""+equipRef+"\"");
        p.pidProportionalRange = hayStack.readDefaultVal("point and proportional and range and equipRef == \""+equipRef+"\"");
        p.useAnalogIn2ForSetpoint = hayStack.readDefaultVal("point and config and enabled and analog2 and OCCUPIED and equipRef == \""+equipRef+"\"") > 0;
        p.analog2InputSensor = hayStack.readDefaultVal("point and config and analog2 and input and sensor and equipRef == \""+equipRef+"\"").intValue();
    
        p.setpointSensorOffset = hayStack.readDefaultVal("point and config and OCCUPIED and sensor and offset and equipRef == \""+equipRef+"\"");
    
        p.analog1AtMinOutput = hayStack.readDefaultVal("point and config and analog1 and min and output and equipRef == \""+equipRef+"\"");
    
        p.analog1AtMaxOutput = hayStack.readDefaultVal("point and config and analog1 and max and output and equipRef == \""+equipRef+"\"");
        p.expectZeroErrorAtMidpoint = hayStack.readDefaultVal("point and config and enabled and zero and error and midpoint and equipRef == \""+equipRef+"\"") > 0;
    
        
        return p;
    }
    
    public void update(PlcProfileConfiguration config) {
        hayStack.writeDefaultVal("point and config and analog1 and input and sensor and equipRef == \""+equipRef+"\"", (double)config.analog1InputSensor);
    
        hayStack.writeDefaultVal("point and config and th1 and input and sensor and equipRef == \""+equipRef+"\"", (double)config.th1InputSensor);
    
        hayStack.writeDefaultVal("point and config and target and value and equipRef == \""+equipRef+"\"", config.pidTargetValue);
        
        hayStack.writeDefaultVal("point and config and proportional and range and equipRef == \""+equipRef+"\"", config.pidProportionalRange);
    
        hayStack.writeDefaultVal("point and config and enabled and analog2 and OCCUPIED and equipRef == \""+equipRef+"\"", config.useAnalogIn2ForSetpoint ? 1.0 :0);
    
        hayStack.writeDefaultVal("point and config and analog2 and input and sensor and equipRef == \""+equipRef+"\"", (double)config.analog2InputSensor);
    
        hayStack.writeDefaultVal("point and config and OCCUPIED and sensor and offset and equipRef == \""+equipRef+"\"", config.setpointSensorOffset);
    
        hayStack.writeDefaultVal("point and config and analog1 and min and output and equipRef == \""+equipRef+"\"", config.analog1AtMinOutput);
    
        hayStack.writeDefaultVal("point and config and analog1 and max and output and equipRef == \""+equipRef+"\"", config.analog1AtMaxOutput);
    
        hayStack.writeDefaultVal("point and config and enabled and zero and error and midpoint and equipRef == \""+equipRef+"\"", config.expectZeroErrorAtMidpoint ? 1.0 :0);
    
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.toString(), (int) config.analog1AtMinOutput+"-"+(int)config.analog1AtMaxOutput);
    
        HashMap sv = hayStack.read("point and OCCUPIED and variable and equipRef == \""+equipRef+"\"");
        
        if (config.useAnalogIn2ForSetpoint) {
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_TWO.name(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_TWO.name(), sv.get("id").toString());
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_TWO.name(), String.valueOf(config.analog2InputSensor));
        }
    
        HashMap pv = hayStack.read("point and process and variable and equipRef == \""+equipRef+"\"");
        
        if (config.analog1InputSensor > 0)
        {
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), true);
            SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), false);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_ONE.name(), pv.get("id").toString());
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_ONE.name(), String.valueOf(config.analog1InputSensor-1));
        } else if (config.th1InputSensor> 0){
            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), false);
            SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name(), pv.get("id").toString());
            SmartNode.updatePhysicalPointType(nodeAddr, Port.TH1_IN.name(), String.valueOf(config.th1InputSensor-1));
        }
    
        targetValue = config.pidTargetValue;
        spSensorOffset = config.setpointSensorOffset;
        isEnabledAnalog2InForSp = config.useAnalogIn2ForSetpoint;
        isEnabledZeroErrorMidpoint = config.expectZeroErrorAtMidpoint;
    }
    
    public double getProcessVariable()
    {
        return hayStack.readHisValByQuery("point and process and variable and equipRef == \""+equipRef+"\"");
    }
    public void setProcessVariable(double processVariable)
    {
        hayStack.writeHisValByQuery("point and process and variable and equipRef == \""+equipRef+"\"", processVariable);
    }
    public double getSpVariable()
    {
        return hayStack.readHisValByQuery("point and OCCUPIED and variable and equipRef == \""+equipRef+"\"");
    }
    public void setSpVariable(double spVariable)
    {
        hayStack.writeHisValByQuery("point and OCCUPIED and variable and equipRef == \""+equipRef+"\"", spVariable);
    
    }
    
    public double getControlVariable()
    {
        return hayStack.readHisValByQuery("point and control and variable and equipRef == \""+equipRef+"\"");
    }
    public void setControlVariable(double controlVariable)
    {
        hayStack.writeHisValByQuery("point and control and variable and equipRef == \""+equipRef+"\"", controlVariable);
        
    }
    
    public double getTargetValue() {
        return targetValue;
    }
    
    public boolean isEnabledAnalog2InForSp() {
        return isEnabledAnalog2InForSp;
    }
    
    public double getSpSensorOffset() {
        return spSensorOffset;
    }
    
    public boolean isEnabledZeroErrorMidpoint() {
        return isEnabledZeroErrorMidpoint;
    }
}
