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

public class PlcEquip {
    ProfileType profileType;
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
            HashMap equip = hayStack.read("equip and pid and group == \"" + nodeAddr + "\"");
            equipRef = equip.get("id").toString();
        }
        plc = new GenericPIController();
        plc.setMaxAllowedError(hayStack.readDefaultVal("point and prange and equipRef == \"" + equipRef + "\""));
        plc.setIntegralGain(TunerUtil.readTunerValByQuery("pid and igain and equipRef == \"" + equipRef + "\""));
        plc.setProportionalGain(TunerUtil.readTunerValByQuery("pid and pgain and equipRef == \"" + equipRef + "\""));
        plc.setIntegralMaxTimeout((int) TunerUtil.readTunerValByQuery("pid and itimeout and equipRef == \"" + equipRef + "\""));

        targetValue = hayStack.readDefaultVal("point and config and target and value and equipRef == \"" + equipRef + "\"");
        spSensorOffset = hayStack.readDefaultVal("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"");
        isEnabledAnalog2InForSp = hayStack.readDefaultVal("point and config and enabled and analog2 and setpoint and equipRef == \"" + equipRef + "\"") > 0;
        isEnabledZeroErrorMidpoint = hayStack.readDefaultVal("point and config and enabled and zero and error and midpoint and equipRef == \"" + equipRef + "\"") > 0;

    }

    public GenericPIController getPIController() {
        return plc;
    }

    public void createEntities(PlcProfileConfiguration config, String floorRef, String roomRef, String processVar, String dynamicTargetTag) {
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;
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
                .addMarker("equip").addMarker("pid").addMarker("zone")
                /*.setAhuRef(ahuRef)*/
                .setGatewayRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(nodeAddr)).build();

        equipRef = hayStack.addEquip(b);
        BuildingTuners.getInstance().addPlcEquipTuners(siteDis + "-PID-" + nodeAddr, equipRef, roomRef, floorRef);

        Point analog1InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-analog1InputSensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("analog1").addMarker("input").addMarker("sensor")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("mV")
                .setTz(tz)
                .build();
        String analog1InputSensorId = hayStack.addPoint(analog1InputSensor);
        hayStack.writeDefaultValById(analog1InputSensorId, (double) config.analog1InputSensor);

        Point th1InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-th1InputSensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("th1").addMarker("input").addMarker("sensor")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("Ohm")
                .setTz(tz)
                .build();
        String th1InputSensorId = hayStack.addPoint(th1InputSensor);
        hayStack.writeDefaultValById(th1InputSensorId, (double) config.th1InputSensor);

        Point pidTargetValue = new Point.Builder()
                .setDisplayName(equipDis + "-pidTargetValue")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("target").addMarker("value")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("V")
                .setTz(tz)
                .build();
        String pidTargetValueId = hayStack.addPoint(pidTargetValue);
        hayStack.writeDefaultValById(pidTargetValueId, (double) config.pidTargetValue);

        Point pidProportionalRange = new Point.Builder()
                .setDisplayName(equipDis + "-pidProportionalRange")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("prange")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String pidProportionalRangeId = hayStack.addPoint(pidProportionalRange);
        hayStack.writeDefaultValById(pidProportionalRangeId, (double) config.pidProportionalRange);

        Point analog1AtMinOutput = new Point.Builder()
                .setDisplayName(equipDis + "-analog1AtMinOutput")
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
                .setDisplayName(equipDis + "-analog1AtMaxOutput")
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
                .setDisplayName(equipDis + "-useAnalogIn2ForSetpoint")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("enabled").addMarker("analog2").addMarker("setpoint")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String useAnalogIn2ForSetpointId = hayStack.addPoint(useAnalogIn2ForSetpoint);
        hayStack.writeDefaultValById(useAnalogIn2ForSetpointId, config.useAnalogIn2ForSetpoint ? 1.0 : 0);

        Point analog2InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-analog2InputSensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("analog2").addMarker("input").addMarker("sensor")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("mV")
                .setTz(tz)
                .build();
        String analog2InputSensorId = hayStack.addPoint(analog2InputSensor);
        hayStack.writeDefaultValById(analog2InputSensorId, (double) config.analog2InputSensor);

        Point setpointSensorOffset = new Point.Builder()
                .setDisplayName(equipDis + "-setpointSensorOffset")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("setpoint").addMarker("sensor").addMarker("offset").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String setpointSensorOffsetId = hayStack.addPoint(setpointSensorOffset);
        hayStack.writeDefaultValById(setpointSensorOffsetId, config.setpointSensorOffset);
        hayStack.writeHisValById(setpointSensorOffsetId, config.setpointSensorOffset);

        Point expectZeroErrorAtMidpoint = new Point.Builder()
                .setDisplayName(equipDis + "-expectZeroErrorAtMidpoint")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("enabled").addMarker("zero").addMarker("error").addMarker("midpoint")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String expectZeroErrorAtMidpointId = hayStack.addPoint(expectZeroErrorAtMidpoint);
        hayStack.writeDefaultValById(expectZeroErrorAtMidpointId, config.expectZeroErrorAtMidpoint ? 1.0 : 0);

        Point controlVariable = new Point.Builder()
                .setDisplayName(equipDis + "-controlVariable")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical")
                .addMarker("control").addMarker("variable")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String controlVariableId = hayStack.addPoint(controlVariable);
        hayStack.writeHisValById(controlVariableId, 0.0);

        Point equipStatusMessage = new Point.Builder()
                .setDisplayName(equipDis + "-equipStatusMessage")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("status").addMarker("message").addMarker("pid").addMarker("writable").addMarker("logical").addMarker("zone")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .setKind("string")
                .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        hayStack.writeDefaultValById(equipStatusMessageLd, "Output Loop Signal is 0%");

        Point equipScheduleType = new Point.Builder()
                .setDisplayName(equipDis + "-scheduleType")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("pid").addMarker("scheduleType").addMarker("writable").addMarker("his")
                .setGroup(String.valueOf(nodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz)
                .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);

        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);

        if (config.analog1InputSensor > 0) {
            String processVariableId = updateProcessVariable(floorRef, roomRef, processVar, getUnit(config.analog1InputSensor));
            device.analog1In.setPointRef(processVariableId);
            device.analog1In.setEnabled(true);
            device.analog1In.setType(String.valueOf(config.analog1InputSensor - 1));
        } else {
            String processVariableId = updateProcessVariable(floorRef, roomRef, processVar, "F");
            device.th1In.setPointRef(processVariableId);
            device.th1In.setEnabled(true);
            device.th1In.setType(String.valueOf(config.th1InputSensor - 1));
        }

        if (config.useAnalogIn2ForSetpoint) {
            String setPointVariableId = updateDynamicTargetInput(config.analog2InputSensor, floorRef, roomRef, dynamicTargetTag);
            device.analog2In.setPointRef(setPointVariableId);
            device.analog2In.setEnabled(true);
            device.analog2In.setType(String.valueOf(config.analog2InputSensor));
        }

        device.analog1Out.setPointRef(controlVariableId);
        device.analog1Out.setType(config.analog1AtMinOutput + "-" + config.analog1AtMaxOutput);
        device.analog1Out.setEnabled(true);
        device.addPointsToDb();

        hayStack.syncEntityTree();
    }

    private String getUnit(int analog1InputSensor) {
        String unit = "V";
        if (analog1InputSensor == 1 || analog1InputSensor == 0) {
            unit = "V";
        } else if (analog1InputSensor == 2 || analog1InputSensor == 3) {
            unit = "WC";
        } else if (analog1InputSensor == 4 || analog1InputSensor == 5) {
            unit = "%";
        } else if (analog1InputSensor == 6 || analog1InputSensor == 7 || analog1InputSensor == 8) {
            unit = "PPM";
        } else if (analog1InputSensor == 9 || analog1InputSensor == 10 || analog1InputSensor == 11) {
            unit = "AMPS";
        }
        return unit;
    }

    private String getUnitForDy(int dynamicInputSensor) {
        String unit = "V";
        if (dynamicInputSensor == 0) {
            unit = "V";
        } else if (dynamicInputSensor == 1 || dynamicInputSensor == 2) {
            unit = "WC";
        } else if (dynamicInputSensor == 3 || dynamicInputSensor == 4) {
            unit = "%";
        } else if (dynamicInputSensor == 5 || dynamicInputSensor == 6 || dynamicInputSensor == 7) {
            unit = "PPM";
        } else if (dynamicInputSensor == 8 || dynamicInputSensor == 9 || dynamicInputSensor == 10) {
            unit = "AMPS";
        }
        return unit;
    }

    public PlcProfileConfiguration getProfileConfiguration() {
        PlcProfileConfiguration p = new PlcProfileConfiguration();
        p.analog1InputSensor = hayStack.readDefaultVal("point and config and analog1 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
        p.th1InputSensor = hayStack.readDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
        p.pidTargetValue = hayStack.readDefaultVal("point and config and target and value and equipRef == \"" + equipRef + "\"");
        p.pidProportionalRange = hayStack.readDefaultVal("point and prange and equipRef == \"" + equipRef + "\"");
        p.useAnalogIn2ForSetpoint = hayStack.readDefaultVal("point and config and enabled and analog2 and setpoint and equipRef == \"" + equipRef + "\"") > 0;
        p.analog2InputSensor = hayStack.readDefaultVal("point and config and analog2 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();

        p.setpointSensorOffset = hayStack.readDefaultVal("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"");

        p.analog1AtMinOutput = hayStack.readDefaultVal("point and config and analog1 and min and output and equipRef == \"" + equipRef + "\"");

        p.analog1AtMaxOutput = hayStack.readDefaultVal("point and config and analog1 and max and output and equipRef == \"" + equipRef + "\"");
        p.expectZeroErrorAtMidpoint = hayStack.readDefaultVal("point and config and enabled and zero and error and midpoint and equipRef == \"" + equipRef + "\"") > 0;


        return p;
    }

    public void update(PlcProfileConfiguration config, String floorRef, String roomRef, String processTag, String dynamicTargetTag) {

        HashMap dynamicTarget = hayStack.read("point and dynamic and target and value and equipRef == \"" + equipRef + "\"");
        HashMap targetValuePoint = hayStack.read("point and config and target and value and equipRef == \"" + equipRef + "\"");

        //delete last point
        if (dynamicTarget != null && dynamicTarget.get("id") != null && (config.useAnalogIn2ForSetpoint && getProfileConfiguration().analog2InputSensor != config.analog2InputSensor)) {

            CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
        }

        if (config.useAnalogIn2ForSetpoint) {
            String id = null;

            //delete target values
            if (targetValuePoint != null && targetValuePoint.get("id") != null){
                CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
            }

            if (dynamicTarget != null && dynamicTarget.get("id") != null) {
                id = dynamicTarget.get("id").toString();
            }
            if (id == null || getProfileConfiguration().analog2InputSensor != config.analog2InputSensor) {
                id = updateDynamicTargetInput(config.analog2InputSensor, floorRef, roomRef, dynamicTargetTag);
            }

            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_TWO.name(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_TWO.name(), id);
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_TWO.name(), String.valueOf(config.analog2InputSensor));
        }

        HashMap processVariable = hayStack.read("point and process and variable and equipRef == \"" + equipRef + "\"");

        //delete  processVariable last point
        if (processVariable.get("id") != null && (config.analog1InputSensor > 0 && getProfileConfiguration().analog1InputSensor != config.analog1InputSensor)
                || (config.th1InputSensor > 0 && getProfileConfiguration().th1InputSensor != config.th1InputSensor)) {

            CCUHsApi.getInstance().deleteEntityTree(processVariable.get("id").toString());
        }

        if (config.analog1InputSensor > 0) {
            //add target value points if useAnalogIn2ForSetpoint off
            if (!config.useAnalogIn2ForSetpoint){
                if (dynamicTarget != null && dynamicTarget.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
                }
                if (targetValuePoint == null || targetValuePoint.get("id") == null){
                    updateTargetValue(config.pidTargetValue, floorRef, roomRef);
                }
            }

            String id = processVariable.get("id").toString();
            if (getProfileConfiguration().analog1InputSensor != config.analog1InputSensor) {
                id = updateProcessVariable(floorRef, roomRef, processTag, getUnit(config.analog1InputSensor));
            }

            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), true);
            SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), false);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_ONE.name(), id);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name(), null);
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_ONE.name(), String.valueOf(config.analog1InputSensor - 1));


        } else if (config.th1InputSensor > 0) {

            //add target value points if useAnalogIn2ForSetpoint off and analog1InputSensor not used
            if (!config.useAnalogIn2ForSetpoint && config.analog1InputSensor == 0){

                if (dynamicTarget != null && dynamicTarget.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
                }

                if (targetValuePoint == null || targetValuePoint.get("id") == null){
                    updateTargetValue(config.pidTargetValue, floorRef, roomRef);
                }
            }

            String id = processVariable.get("id").toString();
            if (getProfileConfiguration().th1InputSensor != config.th1InputSensor) {
                id = updateProcessVariable(floorRef, roomRef, processTag, "F");
            }

            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), false);
            SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_ONE.name(), null);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name(), id);
            SmartNode.updatePhysicalPointType(nodeAddr, Port.TH1_IN.name(), String.valueOf(config.th1InputSensor - 1));
        }

        hayStack.writeDefaultVal("point and config and analog1 and input and sensor and equipRef == \"" + equipRef + "\"", (double) config.analog1InputSensor);

        hayStack.writeDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + equipRef + "\"", (double) config.th1InputSensor);

        if (!config.useAnalogIn2ForSetpoint){
            hayStack.writeDefaultVal("point and config and target and value and equipRef == \"" + equipRef + "\"", config.pidTargetValue);
        }

        hayStack.writeDefaultVal("point and config and prange and equipRef == \"" + equipRef + "\"", config.pidProportionalRange);

        hayStack.writeDefaultVal("point and config and enabled and analog2 and setpoint and equipRef == \"" + equipRef + "\"", config.useAnalogIn2ForSetpoint ? 1.0 : 0);

        hayStack.writeDefaultVal("point and config and analog2 and input and sensor and equipRef == \"" + equipRef + "\"", (double) config.analog2InputSensor);

        hayStack.writeDefaultVal("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"", config.setpointSensorOffset);
        hayStack.writeHisValByQuery("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"", config.setpointSensorOffset);

        hayStack.writeDefaultVal("point and config and analog1 and min and output and equipRef == \"" + equipRef + "\"", config.analog1AtMinOutput);

        hayStack.writeDefaultVal("point and config and analog1 and max and output and equipRef == \"" + equipRef + "\"", config.analog1AtMaxOutput);

        hayStack.writeDefaultVal("point and config and enabled and zero and error and midpoint and equipRef == \"" + equipRef + "\"", config.expectZeroErrorAtMidpoint ? 1.0 : 0);
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.toString(), (int) config.analog1AtMinOutput + "-" + (int) config.analog1AtMaxOutput);


        targetValue = config.pidTargetValue;
        spSensorOffset = config.setpointSensorOffset;
        isEnabledAnalog2InForSp = config.useAnalogIn2ForSetpoint;
        isEnabledZeroErrorMidpoint = config.expectZeroErrorAtMidpoint;
    }

    private String updateProcessVariable(String floorRef, String roomRef, String processTag, String unit) {

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();

        Point processVariableTag = new Point.Builder()
                .setDisplayName("ProcessVariable - " + processTag)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("logical").addMarker("pid").addMarker("zone").addMarker("his").addMarker("cur")
                .addMarker("writable").addMarker("sp")
                .addMarker("process").addMarker("variable")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit(unit)
                .setTz(tz)
                .build();
        String processVariableTagId = hayStack.addPoint(processVariableTag);
        hayStack.writeDefaultValById(processVariableTagId, 0.0);

        return processVariableTagId;
    }

    private String updateDynamicTargetInput(int inputSensor, String floorRef, String roomRef, String dynamicTargetTag) {

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String tz = siteMap.get("tz").toString();

        Point DynamicTargetValueTag = new Point.Builder()
                .setDisplayName("DynamicTargetValue - " + dynamicTargetTag)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("logical").addMarker("pid").addMarker("zone").addMarker("his").addMarker("cur")
                .addMarker("writable").addMarker("sp")
                .addMarker("dynamic").addMarker("target").addMarker("value")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit(getUnitForDy(inputSensor))
                .setTz(tz)
                .build();
        String DynamicTargetValueTagId = hayStack.addPoint(DynamicTargetValueTag);
        hayStack.writeDefaultValById(DynamicTargetValueTagId, 0.0);

        return DynamicTargetValueTagId;
    }

    private String updateTargetValue(double targetValue, String floorRef, String roomRef){

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;

        Point pidTargetValue = new Point.Builder()
                .setDisplayName(equipDis + "-pidTargetValue")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("target").addMarker("value")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("V")
                .setTz(tz)
                .build();
        String pidTargetValueId = hayStack.addPoint(pidTargetValue);
        hayStack.writeDefaultValById(pidTargetValueId,targetValue);

        return pidTargetValueId;
    }

    public double getProcessVariable() {
        return hayStack.readHisValByQuery("point and process and variable and equipRef == \"" + equipRef + "\"");
    }

    public void setProcessVariable(double processVariable) {
        hayStack.writeHisValByQuery("point and process and variable and equipRef == \"" + equipRef + "\"", processVariable);
    }

    public double getSpVariable() {
        return hayStack.readHisValByQuery("point and dynamic and target and value and equipRef == \"" + equipRef + "\"");
    }

    public void setSpVariable(double spVariable) {
        hayStack.writeHisValByQuery("point and dynamic and target and value and equipRef == \"" + equipRef + "\"", spVariable);

    }

    public double getControlVariable() {
        return hayStack.readHisValByQuery("point and control and variable and equipRef == \"" + equipRef + "\"");
    }

    public void setControlVariable(double controlVariable) {
        hayStack.writeHisValByQuery("point and control and variable and equipRef == \"" + equipRef + "\"", controlVariable);

    }

    public void setEquipStatus(int signal) {
        hayStack.writeDefaultVal("point and status and message and equipRef == \"" + equipRef + "\"", "Output Loop Signal is " + signal + "%");

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
