package a75f.io.logic.bo.building.plc;

import android.os.Bundle;

import java.util.HashMap;

import a75.io.algos.GenericPIController;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.bo.building.sensors.NativeSensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.tuners.PlcTuners;
import a75f.io.logic.tuners.TunerUtil;
import a75f.io.logic.util.RxTask;

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
                .setGatewayRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(nodeAddr)).build();

        equipRef = hayStack.addEquip(b);
    
        RxTask.executeAsync(() -> PlcTuners.addPlcEquipTuners(hayStack,
                                                              siteRef,
                                                              siteDis + "-PID-" + nodeAddr,
                                                              equipRef,
                                                              roomRef,
                                                              floorRef,
                                                              tz));

        Point analog1InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-analog1InputSensor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("Analog1 Input Config")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("analog1").addMarker("input").addMarker("sensor")
                .setGroup(String.valueOf(nodeAddr))
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
                .setTz(tz)
                .build();
        String th1InputSensorId = hayStack.addPoint(th1InputSensor);
        hayStack.writeDefaultValById(th1InputSensorId, (double) config.th1InputSensor);
    
        Point nativeInputSensor = new Point.Builder()
                                   .setDisplayName(equipDis + "-nativeInputSensor")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef)
                                   .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                   .addMarker("native").addMarker("input").addMarker("sensor")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        String nativeInputSensorId = hayStack.addPoint(nativeInputSensor);
        hayStack.writeDefaultValById(nativeInputSensorId, (double) config.nativeSensorInput);
        
        Point analog1AtMinOutput = new Point.Builder()
                .setDisplayName(equipDis + "-analog1AtMinOutput")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("his").addMarker("writable")
                .addMarker("analog1").addMarker("min").addMarker("output")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String analog1AtMinOutputId = hayStack.addPoint(analog1AtMinOutput);
        hayStack.writeDefaultValById(analog1AtMinOutputId, config.analog1AtMinOutput);
        hayStack.writeHisValueByIdWithoutCOV(analog1AtMinOutputId, config.analog1AtMinOutput);

        Point analog1AtMaxOutput = new Point.Builder()
                .setDisplayName(equipDis + "-analog1AtMaxOutput")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("his").addMarker("writable")
                .addMarker("analog1").addMarker("max").addMarker("output")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String analog1AtMaxOutputId = hayStack.addPoint(analog1AtMaxOutput);
        hayStack.writeDefaultValById(analog1AtMaxOutputId, config.analog1AtMaxOutput);
        hayStack.writeHisValueByIdWithoutCOV(analog1AtMaxOutputId, config.analog1AtMaxOutput);

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
                .setTz(tz)
                .build();
        String analog2InputSensorId = hayStack.addPoint(analog2InputSensor);
        hayStack.writeDefaultValById(analog2InputSensorId, (double) config.analog2InputSensor);

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
                .setUnit("%")
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
                .setKind(Kind.STRING)
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
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(equipScheduleTypeId, 0.0);
    
        Point controlLoopInversion = new Point.Builder()
                                              .setDisplayName(equipDis + "-controlLoopInversion")
                                              .setEquipRef(equipRef)
                                              .setSiteRef(siteRef)
                                              .setRoomRef(roomRef)
                                              .setFloorRef(floorRef)
                                              .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                                              .addMarker("control").addMarker("loop").addMarker("inversion")
                                              .setGroup(String.valueOf(nodeAddr))
                                              .setEnums("false,true")
                                              .setTz(tz)
                                              .build();
        String controlLoopInversionId = hayStack.addPoint(controlLoopInversion);
        hayStack.writeDefaultValById(controlLoopInversionId, config.controlLoopInversion ? 1.0 : 0);
        String heartBeatId = CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis, equipRef,
                siteRef, roomRef, floorRef, nodeAddr, "pid", tz, false));
        
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);

        device.rssi.setPointRef(heartBeatId);
        device.rssi.setEnabled(true);
        if (config.analog1InputSensor > 0) {
            if (!config.useAnalogIn2ForSetpoint) {
                createTargetValuePoint(floorRef, roomRef, config);
                createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                        PlcRelayConfigHandler.getAnalog1Bundle(config.analog1InputSensor));
            }
            String processVariableId = PlcRelayConfigHandler.createProcessVariablePoint(floorRef, roomRef, Tags.ANALOG1,
                     String.valueOf(nodeAddr), equipRef, config.analog1InputSensor);
            device.analog1In.setPointRef(processVariableId);
            device.analog1In.setEnabled(true);
            device.analog1In.setType(String.valueOf(config.analog1InputSensor - 1));
        } else if(config.th1InputSensor > 0) {
            if (!config.useAnalogIn2ForSetpoint){
                createTargetValuePoint(floorRef, roomRef, config);
                createProportionalRangePoint(config.pidProportionalRange,floorRef, roomRef,
                        PlcRelayConfigHandler.getThermistorBundle(config.th1InputSensor));
            }
            String processVariableId = PlcRelayConfigHandler.createProcessVariablePoint(floorRef, roomRef, Tags.TH1,
                    String.valueOf(nodeAddr), equipRef, config.th1InputSensor);
            device.th1In.setPointRef(processVariableId);
            device.th1In.setEnabled(true);
            device.th1In.setType(String.valueOf(config.th1InputSensor - 1));
        } else if (config.nativeSensorInput > 0) {
            if (!config.useAnalogIn2ForSetpoint){
                createTargetValuePoint(floorRef, roomRef, config);
                createProportionalRangePoint(config.pidProportionalRange,floorRef, roomRef,
                        PlcRelayConfigHandler.getNativeSensorBundle(config.nativeSensorInput));
            }
            String processVariableId = PlcRelayConfigHandler.createProcessVariablePoint(floorRef, roomRef, "native",
                    String.valueOf(nodeAddr), equipRef, config.nativeSensorInput);
            NativeSensor sensor =
                SensorManager.getInstance().getNativeSensorList().get(config.nativeSensorInput - 1);
            
            device.addSensor(sensor.sensorType.getSensorPort(), processVariableId);
        }

        if (config.useAnalogIn2ForSetpoint) {
            String setPointVariableId = createDynamicTargetInputPoint(config.analog2InputSensor, floorRef, roomRef, dynamicTargetTag);
            createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                    PlcRelayConfigHandler.getAnalog2Bundle(config.analog2InputSensor));
            device.analog2In.setPointRef(setPointVariableId);
            device.analog2In.setEnabled(true);
            device.analog2In.setType(String.valueOf(config.analog2InputSensor));
            createSetpointSensorOffsetPoint(config.setpointSensorOffset, floorRef, roomRef);
        }

        device.analog1Out.setPointRef(controlVariableId);
        device.analog1Out.setType((int)config.analog1AtMinOutput + "-" + (int)config.analog1AtMaxOutput+"v");
        device.analog1Out.setEnabled(true);
    
        HashMap equipHash = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");
        Equip equip = new Equip.Builder().setHashMap(equipHash).build();
        
        PlcRelayConfigHandler.createRelayConfigPoints(equip, config, device, Tags.RELAY1, CCUHsApi.getInstance());
        PlcRelayConfigHandler.createRelayConfigPoints(equip, config, device, Tags.RELAY2, CCUHsApi.getInstance());
        
        device.addPointsToDb();

        hayStack.syncEntityTree();
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
        p.nativeSensorInput =
            hayStack.readDefaultVal("point and config and native and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
        p.relay1ConfigEnabled =
            hayStack.readDefaultVal("point and config and relay1 and enabled and equipRef == \"" + equipRef + "\"") > 0;
        p.relay1OnThresholdVal =
            hayStack.readDefaultVal("point and config and relay1 and on and threshold and equipRef == \"" + equipRef + "\"");
        p.relay1OffThresholdVal =
            hayStack.readDefaultVal("point and config and relay1 and off and threshold and equipRef == \"" + equipRef + "\"");
        p.relay2ConfigEnabled =
            hayStack.readDefaultVal("point and config and relay2 and enabled and equipRef == \"" + equipRef + "\"") > 0;
        p.relay2OnThresholdVal =
            hayStack.readDefaultVal("point and config and relay2 and on and threshold and equipRef == \"" + equipRef + "\"");
        p.relay2OffThresholdVal =
            hayStack.readDefaultVal("point and config and relay2 and off and threshold and equipRef == \"" + equipRef + "\"");
    
        p.controlLoopInversion =
            hayStack.readDefaultVal("point and config and control and loop and inversion and equipRef == \"" + equipRef + "\"") > 0;
    
        return p;
    }

    public void update(PlcProfileConfiguration config, String floorRef, String roomRef, String processTag, String dynamicTargetTag) {

        HashMap dynamicTarget = hayStack.read("point and dynamic and target and value and equipRef == \"" + equipRef + "\"");
        HashMap targetValuePoint = hayStack.read("point and config and target and value and equipRef == \"" + equipRef + "\"");
        HashMap offsetSensorPoint = hayStack.read("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"");
        HashMap prangePoint = hayStack.read("point and config and prange and equipRef == \"" + equipRef + "\"");

        PlcProfileConfiguration currentConfig = getProfileConfiguration();
        //delete last point
        if (dynamicTarget != null && dynamicTarget.get("id") != null && (config.useAnalogIn2ForSetpoint
                                                                         && currentConfig.analog2InputSensor != config.analog2InputSensor)) {
            CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
        }

        if (config.useAnalogIn2ForSetpoint) {
            String id = null;

            //delete target values
            if (targetValuePoint != null && targetValuePoint.get("id") != null){
                CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
            }
            //delete old prange values
            if (prangePoint != null && prangePoint.get("id") != null && ((currentConfig.useAnalogIn2ForSetpoint != config.useAnalogIn2ForSetpoint)
                    || (currentConfig.analog2InputSensor != config.analog2InputSensor))) {
                CCUHsApi.getInstance().deleteEntityTree(prangePoint.get("id").toString());
            }

            if (dynamicTarget != null && dynamicTarget.get("id") != null) {
                id = dynamicTarget.get("id").toString();
            }
            if (id == null || currentConfig.analog2InputSensor != config.analog2InputSensor) {
                id = createDynamicTargetInputPoint(config.analog2InputSensor, floorRef, roomRef, dynamicTargetTag);
            }
            // add offset sensor point
            if (offsetSensorPoint == null || offsetSensorPoint.get("id") == null){
                createSetpointSensorOffsetPoint(config.setpointSensorOffset, floorRef, roomRef);
            }
            // add proportional range point
            if (prangePoint == null || prangePoint.get("id") == null || (currentConfig.analog2InputSensor != config.analog2InputSensor)
            || (currentConfig.useAnalogIn2ForSetpoint != config.useAnalogIn2ForSetpoint)){
                createProportionalRangePoint(config.pidProportionalRange,floorRef,roomRef,
                        PlcRelayConfigHandler.getAnalog2Bundle(config.analog2InputSensor));
            }

            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_TWO.name(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_TWO.name(), id);
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_TWO.name(), String.valueOf(config.analog2InputSensor));
            // add offset value with dynamic target value and update
            setSpVariable(getSpVariable() + getSpSensorOffset());
        }

        HashMap processVariable = hayStack.read("point and process and variable and equipRef == \"" + equipRef + "\"");

        //delete  processVariable last point
        if (processVariable.get("id") != null && (config.analog1InputSensor > 0 && currentConfig.analog1InputSensor != config.analog1InputSensor)
                || (config.th1InputSensor > 0 && currentConfig.th1InputSensor != config.th1InputSensor)
                || (config.nativeSensorInput > 0 && currentConfig.nativeSensorInput != config.nativeSensorInput)) {

            CCUHsApi.getInstance().deleteEntityTree(processVariable.get("id").toString());
        }

        if (config.analog1InputSensor > 0) {
            //add target value points and offset sensor if useAnalogIn2ForSetpoint off
            if (!config.useAnalogIn2ForSetpoint){
                if (dynamicTarget != null && dynamicTarget.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
                }
                if (prangePoint != null && prangePoint.get("id") != null
                    && ((currentConfig.useAnalogIn2ForSetpoint != config.useAnalogIn2ForSetpoint)
                        || (currentConfig.analog1InputSensor != config.analog1InputSensor))){
                    CCUHsApi.getInstance().deleteEntityTree(prangePoint.get("id").toString());
                    createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                            PlcRelayConfigHandler.getAnalog1Bundle(config.analog1InputSensor));
                }

                if (offsetSensorPoint != null && offsetSensorPoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(offsetSensorPoint.get("id").toString());
                }
                
                if (targetValuePoint == null || targetValuePoint.get("id") == null){
                    createTargetValuePoint(floorRef, roomRef, config);
                }

                if (prangePoint == null || prangePoint.get("id") == null){
                    createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                            PlcRelayConfigHandler.getAnalog1Bundle(config.analog1InputSensor));
                }
            }

            String id = processVariable.get("id").toString();
            if (currentConfig.analog1InputSensor != config.analog1InputSensor) {
                id = PlcRelayConfigHandler.createProcessVariablePoint(floorRef, roomRef, Tags.ANALOG1,
                        String.valueOf(nodeAddr), equipRef, config.analog1InputSensor);

                //delete  and update target values
                if (targetValuePoint != null && targetValuePoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                    if (!config.useAnalogIn2ForSetpoint) {
                        createTargetValuePoint(floorRef, roomRef, config);
                    }
                }
            }

            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), true);
            SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), false);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_ONE.name(), id);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name(), null);
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_ONE.name(), String.valueOf(config.analog1InputSensor - 1));


        } else if (config.th1InputSensor > 0) {

            //add target value points  and offset sensor point if useAnalogIn2ForSetpoint off and analog1InputSensor not used
            if (!config.useAnalogIn2ForSetpoint && config.analog1InputSensor == 0){

                if (dynamicTarget != null && dynamicTarget.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
                }
                if (prangePoint != null && prangePoint.get("id") != null && ((currentConfig.useAnalogIn2ForSetpoint != config.useAnalogIn2ForSetpoint)
                        ||(currentConfig.th1InputSensor != config.th1InputSensor))){
                    CCUHsApi.getInstance().deleteEntityTree(prangePoint.get("id").toString());
                    createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                            PlcRelayConfigHandler.getThermistorBundle(config.th1InputSensor));
                }

                if (offsetSensorPoint != null && offsetSensorPoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(offsetSensorPoint.get("id").toString());
                }

                if (targetValuePoint == null || targetValuePoint.get("id") == null){
                    createTargetValuePoint(floorRef, roomRef, config);
                }
                if (prangePoint == null || prangePoint.get("id") == null){
                    createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                            PlcRelayConfigHandler.getThermistorBundle(config.th1InputSensor));
                }
            }

            String id = processVariable.get("id").toString();
            if (currentConfig.th1InputSensor != config.th1InputSensor) {
                id = PlcRelayConfigHandler.createProcessVariablePoint(floorRef, roomRef, Tags.TH1,
                        String.valueOf(nodeAddr), equipRef, config.th1InputSensor);

                //delete  and update target values
                if (targetValuePoint != null && targetValuePoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                    if (!config.useAnalogIn2ForSetpoint) {
                        createTargetValuePoint(floorRef, roomRef, config);
                    }
                }
            }

            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), false);
            SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_ONE.name(), null);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.TH1_IN.name(), id);
            SmartNode.updatePhysicalPointType(nodeAddr, Port.TH1_IN.name(), String.valueOf(config.th1InputSensor - 1));
        } else if (config.nativeSensorInput > 0) {
            handleNativeInputSensorUpdate(currentConfig, config, processTag, floorRef, roomRef);
        }

        hayStack.writeDefaultVal("point and config and analog1 and input and sensor and equipRef == \"" + equipRef + "\"", (double) config.analog1InputSensor);

        hayStack.writeDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + equipRef + "\"", (double) config.th1InputSensor);
    
        hayStack.writeDefaultVal("point and config and native and input and sensor and equipRef == \"" + equipRef + "\"", (double) config.nativeSensorInput);
    
        if (!config.useAnalogIn2ForSetpoint){
            hayStack.writeDefaultVal("point and config and target and value and equipRef == \"" + equipRef + "\"", config.pidTargetValue);
            hayStack.writeHisValByQuery("point and config and target and value and equipRef == \"" + equipRef + "\"", config.pidTargetValue);
        }

        hayStack.writeDefaultVal("point and config and prange and equipRef == \"" + equipRef + "\"", config.pidProportionalRange);

        hayStack.writeDefaultVal("point and config and enabled and analog2 and setpoint and equipRef == \"" + equipRef + "\"", config.useAnalogIn2ForSetpoint ? 1.0 : 0);

        hayStack.writeDefaultVal("point and config and analog2 and input and sensor and equipRef == \"" + equipRef + "\"", (double) config.analog2InputSensor);

        if (config.useAnalogIn2ForSetpoint) {
            hayStack.writeDefaultVal("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"", config.setpointSensorOffset);
            hayStack.writeHisValByQuery("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"", config.setpointSensorOffset);
        }

        hayStack.writeDefaultVal("point and config and analog1 and min and output and equipRef == \"" + equipRef + "\"", config.analog1AtMinOutput);
        hayStack.writeHisValByQuery("point and config and analog1 and min and output and equipRef == \"" + equipRef + "\"", config.analog1AtMinOutput);

        hayStack.writeDefaultVal("point and config and analog1 and max and output and equipRef == \"" + equipRef + "\"", config.analog1AtMaxOutput);
        hayStack.writeHisValByQuery("point and config and analog1 and max and output and equipRef == \"" + equipRef + "\"", config.analog1AtMaxOutput);

        hayStack.writeDefaultVal("point and config and enabled and zero and error and midpoint and equipRef == \"" + equipRef + "\"", config.expectZeroErrorAtMidpoint ? 1.0 : 0);
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.toString(), (int) config.analog1AtMinOutput + "-" + (int) config.analog1AtMaxOutput +"v");
    
        targetValue = config.pidTargetValue;
        spSensorOffset = config.setpointSensorOffset;
        isEnabledAnalog2InForSp = config.useAnalogIn2ForSetpoint;
        isEnabledZeroErrorMidpoint = config.expectZeroErrorAtMidpoint;
    
        hayStack.writeDefaultVal("point and config and control and loop and inversion and equipRef == \"" + equipRef + "\"",
                                 config.controlLoopInversion ? 1.0 : 0);
        
        handleRelayConfigUpdate(config);
        resetRelayOutput(currentConfig, config);
    }
    
    private void handleRelayConfigUpdate(PlcProfileConfiguration config) {
    
        HashMap relay1Config = CCUHsApi.getInstance().read("point and config and relay1 and enabled and equipRef == " +
                                                           "\"" + equipRef + "\"");
        
        //Required during upgrades of PI profiles which do not have relay config points.
        if (relay1Config.isEmpty()) {
            HashMap equipHash = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");
            Equip equip = new Equip.Builder().setHashMap(equipHash).build();
    
            PlcRelayConfigHandler.updateRelayConfigPoints(equip, config, Tags.RELAY1, CCUHsApi.getInstance());
            PlcRelayConfigHandler.updateRelayConfigPoints(equip, config, Tags.RELAY2, CCUHsApi.getInstance());
            return;
        }
        
        hayStack.writeDefaultVal("point and config and relay1 and enabled and equipRef == \"" + equipRef + "\"",
                                 config.relay1ConfigEnabled ? 1.0 : 0);
        hayStack.writeDefaultVal("point and config and relay1 and on and threshold and equipRef == \"" + equipRef +
                                 "\"", config.relay1OnThresholdVal);
        hayStack.writeDefaultVal("point and config and relay1 and off and threshold and equipRef == \"" + equipRef +
                                 "\"", config.relay1OffThresholdVal);
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_ONE.name(), config.relay1ConfigEnabled);
        
        hayStack.writeDefaultVal("point and config and relay2 and enabled and equipRef == \"" + equipRef + "\"",
                                 config.relay2ConfigEnabled ? 1.0 : 0);
        hayStack.writeDefaultVal("point and config and relay2 and on and threshold and equipRef == \"" + equipRef +
                                 "\"", config.relay2OnThresholdVal);
        hayStack.writeDefaultVal("point and config and relay2 and off and threshold and equipRef == \"" + equipRef +
                                 "\"", config.relay2OffThresholdVal);
        SmartNode.setPointEnabled(nodeAddr, Port.RELAY_TWO.name(), config.relay2ConfigEnabled);
    }
    
    /**
     * Reset relay output if relay thresholds have been updated.
     * @param curConfig
     * @param newConfig
     */
    private void resetRelayOutput(PlcProfileConfiguration curConfig, PlcProfileConfiguration newConfig) {
        
        if (curConfig.relay1OnThresholdVal != newConfig.relay1OnThresholdVal
                || curConfig.relay1OffThresholdVal != newConfig.relay1OffThresholdVal) {
            setCmdVal(Tags.RELAY1, 0);
        }
    
        if (curConfig.relay2OnThresholdVal != newConfig.relay2OnThresholdVal
                || curConfig.relay2OffThresholdVal != newConfig.relay2OffThresholdVal) {
            setCmdVal(Tags.RELAY2, 0);
        }
    }
    
    /**
     *  Handles creation of new points and deletion of some of the existing points based on new profile configuration.
     *  A lot of this code can be removed if these operations are combined for different types of input sensors.
     *  We currently handle them separately.
     * (TODO - Revisit during PI profile reconfiguration)
     * @param currentConfig
     * @param config
     * @param processTag
     * @param floorRef
     * @param roomRef
     */
    private void handleNativeInputSensorUpdate(PlcProfileConfiguration currentConfig, PlcProfileConfiguration config,
                                               String processTag, String floorRef, String roomRef) {
        
        HashMap dynamicTarget = hayStack.read("point and dynamic and target and value and equipRef == \"" + equipRef + "\"");
        HashMap targetValuePoint = hayStack.read("point and config and target and value and equipRef == \"" + equipRef + "\"");
        HashMap offsetSensorPoint = hayStack.read("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"");
        HashMap prangePoint = hayStack.read("point and config and prange and equipRef == \"" + equipRef + "\"");
    
        //add target value points  and offset sensor point if useAnalogIn2ForSetpoint off and analog1InputSensor not used
        if (!config.useAnalogIn2ForSetpoint){
        
            if (!dynamicTarget.isEmpty()){
                CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
            }
            
            if (!offsetSensorPoint.isEmpty()){
                CCUHsApi.getInstance().deleteEntityTree(offsetSensorPoint.get("id").toString());
            }
        
            if (targetValuePoint.isEmpty()){
                createTargetValuePoint(floorRef, roomRef, config);
            }
            
            if (prangePoint.isEmpty()){
                createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                        PlcRelayConfigHandler.getNativeSensorBundle(config.nativeSensorInput));
                
            } else if (currentConfig.useAnalogIn2ForSetpoint != config.useAnalogIn2ForSetpoint ||
                       currentConfig.nativeSensorInput != config.nativeSensorInput) {
                
                CCUHsApi.getInstance().deleteEntityTree(prangePoint.get("id").toString());
                createProportionalRangePoint(config.pidProportionalRange, floorRef, roomRef,
                        PlcRelayConfigHandler.getNativeSensorBundle(config.nativeSensorInput));
            }
        }
        
        if (currentConfig.nativeSensorInput != config.nativeSensorInput) {
            String id = PlcRelayConfigHandler.createProcessVariablePoint(floorRef, roomRef, "native",
                    String.valueOf(nodeAddr), equipRef, config.nativeSensorInput);
        
            //delete and update target values
            if (!targetValuePoint.isEmpty()){
                CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                if (!config.useAnalogIn2ForSetpoint) {
                    createTargetValuePoint(floorRef, roomRef, config);
                }
            }
            NativeSensor sensor =
                SensorManager.getInstance().getNativeSensorList().get(config.nativeSensorInput - 1);
            Port sensorPort = sensor.sensorType.getSensorPort();
            RawPoint sensorPortPoint = SmartNode.getPhysicalPoint(nodeAddr, sensorPort.toString());
            SmartNode device = new SmartNode(nodeAddr);
            if (sensorPortPoint == null) {
                device.addSensor(sensorPort, id);
            } else {
                SmartNode.updatePhysicalPointRef(nodeAddr, sensorPort.toString(), id);
            }
            
        }
    
        SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), false);
        SmartNode.setPointEnabled(nodeAddr, Port.TH1_IN.name(), false);
        
    }

    private String createDynamicTargetInputPoint(int inputSensor, String floorRef, String roomRef,
                                             String dynamicTargetTag) {

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-PID-" + nodeAddr;
        String tz = siteMap.get("tz").toString();

        Bundle bundle = PlcRelayConfigHandler.getAnalog2Bundle(inputSensor);
        String shortDis = bundle.getString("shortDis");
        String shortDisTarget = bundle.getString("shortDisTarget");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String incrementVal = bundle.getString("incrementVal");
        String[] markers = bundle.getStringArray("markers");

        Point.Builder dynamicTargetValueTag = new Point.Builder()
                .setDisplayName(equipDis + "-dynamicTargetValue-" + dynamicTargetTag)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis(shortDisTarget)
                .setHisInterpolate("cov")
                .addMarker("logical").addMarker("pid").addMarker("zone").addMarker("his").addMarker("cur")
                .addMarker("sp")
                .addMarker("dynamic").addMarker("target").addMarker("value")
                .setGroup(String.valueOf(nodeAddr))
                .setMinVal(minVal)
                .setMaxVal(maxVal)
                .setIncrementVal(incrementVal)
                .setUnit(unit)
                .setTz(tz);
        if (markers != null) {
            for (String marker : markers) {
                dynamicTargetValueTag.addMarker(marker);
            }
        }

        String dynamicTargetValueTagId = hayStack.addPoint(dynamicTargetValueTag.build());
        hayStack.writeHisValueByIdWithoutCOV(dynamicTargetValueTagId, 0.0);

        return dynamicTargetValueTagId;
    }

    private String createSetpointSensorOffsetPoint(double spSensorOffset,String floorRef, String roomRef) {

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-PID-" + nodeAddr;
        String tz = siteMap.get("tz").toString();

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
        hayStack.writeDefaultValById(setpointSensorOffsetId, spSensorOffset);
        hayStack.writeHisValueByIdWithoutCOV(setpointSensorOffsetId, spSensorOffset);
        return setpointSensorOffsetId;
    }

    private String createTargetValuePoint(String floorRef, String roomRef, PlcProfileConfiguration config){

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;
        Bundle bundle = new Bundle();
        if (config.analog1InputSensor > 0){
            bundle = PlcRelayConfigHandler.getAnalog1Bundle(config.analog1InputSensor);
        } else if (config.th1InputSensor > 0){
            bundle = PlcRelayConfigHandler.getThermistorBundle(config.th1InputSensor);
        } else if (config.nativeSensorInput > 0) {
            bundle = PlcRelayConfigHandler.getNativeSensorBundle(config.nativeSensorInput);
        }

        String shortDis = bundle.getString("shortDis");
        String shortDisTarget = bundle.getString("shortDisTarget");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String incrementVal = bundle.getString("incrementVal");
        String[] markers = bundle.getStringArray("markers");

        Point.Builder pidTargetValue = new Point.Builder()
                .setDisplayName(equipDis + "-pidTargetValue")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis(shortDisTarget)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("his").addMarker("writable")
                .addMarker("target").addMarker("value")
                .setHisInterpolate("cov")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit(unit)
                .setMinVal(minVal)
                .setMaxVal(maxVal)
                .setIncrementVal(incrementVal)
                .setTz(tz);
        if (markers != null) {
            for (String marker : markers) {
                pidTargetValue.addMarker(marker);
            }
        }
        String pidTargetValueId = hayStack.addPoint(pidTargetValue.build());
        hayStack.writeDefaultValById(pidTargetValueId,config.pidTargetValue);
        hayStack.writeHisValueByIdWithoutCOV(pidTargetValueId, config.pidTargetValue);

        return pidTargetValueId;
    }

    private String createProportionalRangePoint(double proportionalRange, String floorRef, String roomRef,
                                               Bundle bundle){
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;

        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String incrementVal = bundle.getString("incrementVal");

        Point pidProportionalRange = new Point.Builder()
                .setDisplayName(equipDis + "-pidProportionalRange")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("PID Proportional Range")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("prange")
                .setGroup(String.valueOf(nodeAddr))
                .setMinVal(minVal)
                .setMaxVal(maxVal)
                .setIncrementVal(incrementVal)
                .setTz(tz)
                .build();
        String pidProportionalRangeId = hayStack.addPoint(pidProportionalRange);
        hayStack.writeDefaultValById(pidProportionalRangeId, (double) proportionalRange);
        hayStack.writeHisValueByIdWithoutCOV(pidProportionalRangeId, (double) proportionalRange);
        return pidProportionalRangeId;
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

    public void setEquipStatus(String message) {
        hayStack.writeDefaultVal("point and status and message and equipRef == \"" + equipRef + "\"", message);

    }
    
    public void setConfigNumVal(String tags,double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and config and pid and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public void setCmdVal(String tags,double val) {
        CCUHsApi.getInstance().writeHisValByQuery("point and cmd and pid and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public double getConfigNumVal(String tags) {
        return CCUHsApi.getInstance().readDefaultVal("point and config and pid and "+tags+" and group == \""+nodeAddr+ "\"");
    }
    
    public double getCmdVal(String tags) {
        return CCUHsApi.getInstance().readHisValByQuery("point and cmd and pid and "+tags+" and group == \""+nodeAddr+ "\"");
    }

    public double getTargetValue() {
        return CCUHsApi.getInstance().readDefaultVal("point and config and target and value and group == \"" + nodeAddr + "\"");
    }

    public boolean isEnabledAnalog2InForSp() {
        return hayStack.readDefaultVal("point and config and enabled and analog2 and setpoint and group == \"" + nodeAddr + "\"") > 0;
    }

    public double getSpSensorOffset() {
        return hayStack.readDefaultVal("point and config and setpoint and sensor and offset and group == \"" + nodeAddr + "\"");
    }

    public boolean isEnabledZeroErrorMidpoint() {
        return hayStack.readDefaultVal("point and config and enabled and zero and error and midpoint and group == \"" + nodeAddr + "\"") > 0;
    }
}
