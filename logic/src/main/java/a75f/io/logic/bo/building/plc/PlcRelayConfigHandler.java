package a75f.io.logic.bo.building.plc;

import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.sensors.NativeSensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.haystack.device.SmartNode;

/**
 * Util class that handles create/update of points for PI equip relays and initialize them.
 */
public class PlcRelayConfigHandler {
    
    /**
     * Create Relay points if they are enabled during the first time pairing.
     * Device instance is directly used here, since it is not added to the database yet.
     * @param equip
     * @param config
     * @param device
     * @param relayType
     * @param hayStack
     */
    public static void createRelayConfigPoints(Equip equip, PlcProfileConfiguration config, SmartNode device,
                                               String relayType, CCUHsApi hayStack) {
    
        
        String relayConfigId = addRelayConfigPoint(equip, relayType, hayStack);
        
        String relayCmdId = addRelayCmdPoint(equip, relayType, hayStack);
        hayStack.writeHisValById(relayCmdId, 0.0);
    
        
        String relayOnThresholdId = addRelayOnThresholdPoint(equip, relayType, hayStack);
        String relayOffThresholdId = addRelayOffThresholdPoint(equip, relayType, hayStack);
    
        if (relayType.contains(Tags.RELAY1)) {
            device.relay1.setPointRef(relayCmdId);
            device.relay1.setEnabled(config.relay1ConfigEnabled);
            device.relay1.setType(OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay1ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay1OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay1OffThresholdVal);
        } else if (relayType.contains(Tags.RELAY2)) {
            device.relay2.setPointRef(relayCmdId);
            device.relay2.setEnabled(config.relay2ConfigEnabled);
            device.relay2.setType(OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay2ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay2OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay2OffThresholdVal);
        }
    }
    
    /**
     * Update the relay config points when relay status is changed during a profile update.
     * Here we use static method to update relay configurations on the device.
     * @param equip
     * @param config
     * @param relayType
     * @param hayStack
     */
    public static void updateRelayConfigPoints(Equip equip, PlcProfileConfiguration config,
                                               String relayType, CCUHsApi hayStack) {
    
        String relayConfigId = addRelayConfigPoint(equip, relayType, hayStack);
    
        String relayCmdId = addRelayCmdPoint(equip, relayType, hayStack);
        hayStack.writeHisValById(relayCmdId, 0.0);
    
    
        String relayOnThresholdId = addRelayOnThresholdPoint(equip, relayType, hayStack);
        String relayOffThresholdId = addRelayOffThresholdPoint(equip, relayType, hayStack);
    
        int nodeAddress = Integer.parseInt(equip.getGroup());
        if (relayType.contains(Tags.RELAY1)) {
            
            SmartNode.updatePhysicalPointRef(nodeAddress, Port.RELAY_ONE.name(), relayCmdId);
            SmartNode.setPointEnabled(nodeAddress, Port.RELAY_ONE.name(),
                                      config.relay1ConfigEnabled);
            SmartNode.updatePhysicalPointType(nodeAddress, Port.RELAY_ONE.name(), OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay1ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay1OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay1OffThresholdVal);
        } else if (relayType.contains(Tags.RELAY2)) {
            SmartNode.updatePhysicalPointRef(nodeAddress, Port.RELAY_TWO.name(), relayCmdId);
            SmartNode.setPointEnabled(nodeAddress, Port.RELAY_TWO.name(), config.relay2ConfigEnabled);
            SmartNode.updatePhysicalPointType(nodeAddress, Port.RELAY_TWO.name(),
                                              OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay2ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay2OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay2OffThresholdVal);
        }
    }
    
    
    private static String addRelayConfigPoint(Equip equip, String relayType, CCUHsApi hayStack) {
        Point relayConfig = new Point.Builder()
                                .setDisplayName(equip.getDisplayName() + "-"+relayType+"ConfigEnabled")
                                .setEquipRef(equip.getId())
                                .setSiteRef(equip.getSiteRef())
                                .setRoomRef(equip.getRoomRef())
                                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("writable").addMarker("logical")
                                .addMarker("config").addMarker(relayType).addMarker("enabled")
                                .setGroup(equip.getGroup())
                                .setTz(hayStack.getTimeZone())
                                .build();
        return hayStack.addPoint(relayConfig);
    }
    
    private static String addRelayCmdPoint(Equip equip, String relayType, CCUHsApi hayStack) {
        Point relayCmd = new Point.Builder()
                             .setDisplayName(equip.getDisplayName() + "-"+relayType+"Cmd")
                             .setEquipRef(equip.getId())
                             .setSiteRef(equip.getSiteRef())
                             .setRoomRef(equip.getRoomRef())
                             .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                             .addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical")
                             .addMarker("cmd").addMarker(relayType)
                             .setGroup(equip.getGroup())
                             .setTz(hayStack.getTimeZone())
                             .build();
        return hayStack.addPoint(relayCmd);
    }
    
    private static String addRelayOnThresholdPoint(Equip equip, String relayType, CCUHsApi hayStack) {
        Point relayOnThreshold = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-"+relayType+"OnThreshold")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("writable").addMarker("logical")
                                     .addMarker("config").addMarker(relayType).addMarker("on").addMarker("threshold")
                                     .setUnit("%")
                                     .setGroup(equip.getGroup())
                                     .setTz(hayStack.getTimeZone())
                                     .build();
        return hayStack.addPoint(relayOnThreshold);
    }
    
    private static String addRelayOffThresholdPoint(Equip equip, String relayType, CCUHsApi hayStack) {
        Point relayOffThreshold = new Point.Builder()
                                      .setDisplayName(equip.getDisplayName() + "-"+relayType+"OffThreshold")
                                      .setEquipRef(equip.getId())
                                      .setSiteRef(equip.getSiteRef())
                                      .setRoomRef(equip.getRoomRef())
                                      .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                      .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("writable").addMarker("logical")
                                      .addMarker("config").addMarker(relayType).addMarker("off").addMarker("threshold")
                                      .setUnit("%")
                                      .setGroup(equip.getGroup())
                                      .setTz(hayStack.getTimeZone())
                                      .build();
        return hayStack.addPoint(relayOffThreshold);
    }


    public static void updateInputSensor(JsonObject msgObject, Point configPoint) {
        //logical point tags- process variable
        Log.d("updateInputSensor", "updateInputSensor ++");
        HashMap<Object, Object> equipMap =
                CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String floorRef = equip.getFloorRef();
        String equipRef = configPoint.getEquipRef();
        String nodeAddr = equip.getGroup();
        String roomRef = equip.getRoomRef();
        int configVal = msgObject.get("val").getAsInt();

        HashMap targetValuePoint = CCUHsApi.getInstance().read("point and config and target and " +
                "value and equipRef == \"" + equipRef + "\"");

        if (configPoint.getMarkers().contains(Tags.ANALOG1)) {
            CCUHsApi.getInstance().writeDefaultVal("point and config and analog1 and" +
                    " input and sensor and equipRef == \"" + equipRef + "\"", (double) configVal);
            /*CCUHsApi.getInstance().writeDefaultVal("point and config and th1 and input and " +
                    "sensor and equipRef == \"" + equipRef + "\"", 0.0);
            CCUHsApi.getInstance().writeDefaultVal("point and config and native and input" +
                    " and sensor and equipRef == \"" + equipRef + "\"", 0.0);*/
            HashMap processVariable = CCUHsApi.getInstance().read("point and process and " +
                    "variable and equipRef == \"" + equipRef + "\"");

            //delete prev targetpoint and create new.

            if(configVal > 0){
                if (targetValuePoint != null && targetValuePoint.get("id") != null) {
                    CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                    createTargetValuePoint(floorRef, roomRef, Tags.ANALOG1, nodeAddr, equipRef,
                            configVal);
                }
                //delete  processVariable last point
                if (processVariable.get("id") != null) {
                    CCUHsApi.getInstance().deleteEntityTree(processVariable.get("id").toString());
                }

                String id = createProcessVariablePoint(floorRef, roomRef, Tags.ANALOG1, nodeAddr,
                        equipRef, configVal);
                SmartNode.setPointEnabled(Integer.parseInt(nodeAddr), Port.ANALOG_IN_ONE.name(), true);
                SmartNode.updatePhysicalPointRef(Integer.parseInt(nodeAddr),
                        Port.ANALOG_IN_ONE.name(), id);
                SmartNode.updatePhysicalPointType(Integer.parseInt(nodeAddr), Port.ANALOG_IN_ONE.name(),
                        String.valueOf(configVal - 1));
            }


        }

        if (configPoint.getMarkers().contains(Tags.TH1)) {
            CCUHsApi.getInstance().writeDefaultVal("point and config and th1 and input and " +
                    "sensor and equipRef == \"" + equipRef + "\"", (double) configVal);

            if(configVal > 0) {
                HashMap processVariable = CCUHsApi.getInstance().read("point and process and " +
                        "variable and equipRef == \"" + equipRef + "\"");

                //delete prev targetpoint and create new.
                if (targetValuePoint != null && targetValuePoint.get("id") != null) {
                    CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                    createTargetValuePoint(floorRef, roomRef, Tags.TH1, nodeAddr, equipRef, configVal);
                }

                //delete  processVariable last point
                if (processVariable.get("id") != null) {
                    CCUHsApi.getInstance().deleteEntityTree(processVariable.get("id").toString());
                }
                String id = createProcessVariablePoint(floorRef, roomRef, Tags.TH1, nodeAddr,
                        equipRef, configVal);
                SmartNode.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH1_IN.name(), true);
                SmartNode.updatePhysicalPointRef(Integer.parseInt(nodeAddr), Port.TH1_IN.name(), id);

                SmartNode.updatePhysicalPointType(Integer.parseInt(nodeAddr), Port.TH1_IN.name(),
                        String.valueOf(configVal - 1));
            }

        }

        if (configPoint.getMarkers().contains("native")) {
            CCUHsApi.getInstance().writeDefaultVal("point and config and native and input" +
                    " and sensor and equipRef == \"" + equipRef + "\"", (double) configVal);
            HashMap processVariable = CCUHsApi.getInstance().read("point and process and " +
                    "variable and equipRef == \"" + equipRef + "\"");

            if(configVal > 0) {
                //delete prev targetpoint and create new.
                if (targetValuePoint != null && targetValuePoint.get("id") != null) {
                    CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                    createTargetValuePoint(floorRef, roomRef, "native", nodeAddr, equipRef, configVal);
                }

                //delete  processVariable last point
                if (processVariable.get("id") != null) {
                    CCUHsApi.getInstance().deleteEntityTree(processVariable.get("id").toString());
                }
                String id = createProcessVariablePoint(floorRef, roomRef, "native", nodeAddr,
                        equipRef, configVal);

                NativeSensor sensor =
                        SensorManager.getInstance().getNativeSensorList().get(configVal - 1);
                Port sensorPort = sensor.sensorType.getSensorPort();
                RawPoint sensorPortPoint = SmartNode.getPhysicalPoint(Integer.valueOf(nodeAddr), sensorPort.toString());
                SmartNode device = new SmartNode(Integer.valueOf(nodeAddr));
                if (sensorPortPoint == null) {
                    device.addSensor(sensorPort, id);
                } else {
                    SmartNode.updatePhysicalPointRef(Integer.valueOf(nodeAddr), sensorPort.toString(), id);
                }
            }

        }

        //call updateAn2Value for an2
        if (configPoint.getMarkers().contains(Tags.ANALOG2)) {
            updateAn2Value(floorRef, roomRef, nodeAddr, equipRef, configVal);
        }

        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateTargetValue(JsonObject msgObject, Point configPoint) {
        CCUHsApi.getInstance().writeDefaultVal("point and target and value" +
                        " and equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(configPoint.getId(),
                msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateRangeValue(JsonObject msgObject, Point configPoint) {
        //try writeDefaultValById with configPoint.getID
        CCUHsApi.getInstance().writeDefaultVal("point and config and prange and equipRef == \""
                + configPoint.getEquipRef() + "\"", msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV("point and config and prange and " +
                "equipRef == \""
                + configPoint.getEquipRef() + "\"", msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateMidpointValue(JsonObject msgObject, Point configPoint) {
        CCUHsApi.getInstance().writeDefaultVal("point and config and enabled and zero and " +
                        "error and midpoint and equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateInversionValue(JsonObject msgObject, Point configPoint) {
        CCUHsApi.getInstance().writeDefaultVal("point and control and loop and " +
                        "inversion and equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateEnableAn2Value(JsonObject msgObject, Point configPoint) {
        //set useAnalogIn2ForSetpoint(analog2,enabled,setpoint) - 1
        //set analog2InputSensor(analog2 and sensor) - last created avlue
        //create setpointSensorOffset(setpoint, sensor) and update default value
        HashMap<Object, Object> equipMap =
                CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String floorRef = equip.getFloorRef();
        String equipRef = configPoint.getEquipRef();
        String nodeAddr = equip.getGroup();
        String roomRef = equip.getRoomRef();
        int configVal = msgObject.get("val").getAsInt();

        HashMap targetValuePoint = CCUHsApi.getInstance().read("point and config and target and " +
                "value and equipRef == \"" + equipRef + "\"");
        CCUHsApi.getInstance().writeDefaultVal("point and analog2 and enabled and " +
                        "setpoint and equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());
        if (configVal == 0) {
            HashMap offsetSensorPoint = CCUHsApi.getInstance().read("point and config and " +
                    "setpoint " +
                    "and sensor and offset and equipRef == \"" + equipRef + "\"");
            HashMap dynamicTarget = CCUHsApi.getInstance().read("point and dynamic and target and" +
                    " value and equipRef == \"" + equipRef + "\"");
            if (dynamicTarget != null && dynamicTarget.get("id") != null) {
                CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
            }
            if (offsetSensorPoint != null && offsetSensorPoint.get("id") != null) {
                CCUHsApi.getInstance().deleteEntityTree(offsetSensorPoint.get("id").toString());
            }

            int analog1InputSensor = CCUHsApi.getInstance().readDefaultVal("point and config and analog1 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
            int th1InputSensor = CCUHsApi.getInstance().readDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();
            int nativeSensorInput = CCUHsApi.getInstance().readDefaultVal("point and config and native and input and sensor and equipRef == \"" + equipRef + "\"").intValue();

            if(analog1InputSensor > 0)
                createTargetValuePoint(floorRef, roomRef, Tags.ANALOG1, nodeAddr, equipRef, analog1InputSensor);
            else if(th1InputSensor > 0)
                createTargetValuePoint(floorRef, roomRef, Tags.TH1, nodeAddr, equipRef, th1InputSensor);
            else if(nativeSensorInput > 0)
                createTargetValuePoint(floorRef, roomRef, "native" , nodeAddr, equipRef, nativeSensorInput);


        } else {
            //delete target values
            if (targetValuePoint != null && targetValuePoint.get("id") != null){
                CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
            }

            int inpSensor = CCUHsApi.getInstance().readDefaultVal("point and config and analog2 and input and sensor and equipRef == \"" + equipRef + "\"").intValue();

            updateAn2Value(floorRef, roomRef, nodeAddr, equipRef, inpSensor );
        }

        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateAn2Value(String floorRef, String roomRef,
                                      String nodeAddr, String equipRef, int val) {
        //check useAnalogIn2ForSetpoint(analog2,enabled,setpoint) is 1
        //set analog2InputSensor(analog2 and sensor) - sent value
        //delete old setpointSensorOffset(setpoint, sensor)
        // create setpointSensorOffset(setpoint, sensor) and update default value

        HashMap dynamicTarget = CCUHsApi.getInstance().read("point and dynamic and target and " +
                "value and equipRef == \"" + equipRef + "\"");
        HashMap offsetSensorPoint = CCUHsApi.getInstance().read("point and config and setpoint " +
                "and sensor and offset and equipRef == \"" + equipRef + "\"");

        String id = null;

        CCUHsApi.getInstance().writeDefaultVal("point and config and analog2 and" +
                " input and sensor and equipRef == \"" + equipRef + "\"", (double) val);
        if (dynamicTarget != null && dynamicTarget.get("id") != null) {
            CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
        }

        if (offsetSensorPoint != null && offsetSensorPoint.get("id") != null) {
            CCUHsApi.getInstance().deleteEntityTree(offsetSensorPoint.get("id").toString());
        }

        id = createDynamicTargetInputPoint(val, floorRef, roomRef, nodeAddr, equipRef);

        // add offset sensor point
        createSetpointSensorOffsetPoint( val, floorRef, roomRef, nodeAddr, equipRef);


        SmartNode.setPointEnabled(Integer.parseInt(nodeAddr), Port.ANALOG_IN_TWO.name(), true);
        SmartNode.updatePhysicalPointRef(Integer.parseInt(nodeAddr), Port.ANALOG_IN_TWO.name(), id);
        SmartNode.updatePhysicalPointType(Integer.parseInt(nodeAddr), Port.ANALOG_IN_TWO.name(),
                String.valueOf(val));
        // add offset value with dynamic target value and update
        double spval = CCUHsApi.getInstance().readHisValByQuery("point and dynamic and target and" +
                " value and equipRef == \"" + equipRef + "\"");
        double spSensorOffset = CCUHsApi.getInstance().readDefaultVal("point and config and " +
                "setpoint and sensor and offset and equipRef == \"" + equipRef + "\"");
        CCUHsApi.getInstance().writeHisValByQuery("point and dynamic and target and value and " +
                "equipRef == \"" + equipRef + "\"", spval + spSensorOffset);
        CCUHsApi.getInstance().writeDefaultVal("point and dynamic and target and value and " +
                "equipRef == \"" + equipRef + "\"", spval + spSensorOffset);

    }

    public static void updateAn2SetpointValue(JsonObject msgObject, Point configPoint) {

        // update setpointSensorOffset(setpoint, sensor) with sent value

        CCUHsApi.getInstance().writeDefaultVal("point and config and setpoint and sensor and " +
                        "offset and" +
                        " equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().writeHisValByQuery("point and config and setpoint and sensor" +
                        " and offset and equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());

        double spval = CCUHsApi.getInstance().readHisValByQuery("point and dynamic and target and" +
                " value and equipRef == \"" + configPoint.getEquipRef() + "\"");

        CCUHsApi.getInstance().writeHisValByQuery("point and dynamic and target and value and " +
                "equipRef == \"" + configPoint.getEquipRef() + "\"", spval + msgObject.get("val").getAsDouble());

        CCUHsApi.getInstance().writeDefaultVal("point and dynamic and target and value and " +
                "equipRef == \"" + configPoint.getEquipRef() + "\"", spval + msgObject.get("val").getAsDouble());

        CCUHsApi.getInstance().scheduleSync();
    }

    public static void updateAn1OutputValue(JsonObject msgObject, Point configPoint) {
        //check if min or max
        //update the value
        String tag = "min";
        HashMap<Object, Object> equipMap =
                CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String equipRef = configPoint.getEquipRef();
        String nodeAddr = equip.getGroup();

        if (configPoint.getMarkers().contains("max")) {
            tag = "max";
        }
        CCUHsApi.getInstance().writeDefaultVal("point and config and analog1 and " + tag + " and" +
                        " output and equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());
        CCUHsApi.getInstance().writeHisValByQuery("point and config and analog1 and " + tag +
                        " and output and equipRef == \"" + configPoint.getEquipRef() + "\"",
                msgObject.get("val").getAsDouble());

       double min = CCUHsApi.getInstance().readDefaultVal("point and config and analog1 and min and output and equipRef == \"" + equipRef + "\"");

        double max = CCUHsApi.getInstance().readDefaultVal("point and config and analog1 and max and output and equipRef == \"" + equipRef + "\"");

        SmartNode.updatePhysicalPointType(Integer.parseInt(nodeAddr), Port.ANALOG_OUT_ONE.toString(), (int) min + "-" + (int) max +"v");

        CCUHsApi.getInstance().scheduleSync();

    }

    public static void updateRelayValue(JsonObject msgObject, Point configPoint) {
        //check if toggle change
        //if toggle off set toggle to 0
        //if toggle on set toggle to 1
        //check what relay cmd
        //create a state point
        HashMap equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        double configVal = msgObject.get("val").getAsDouble();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        String relayType = null;
        int nodeAddress = Integer.parseInt(equip.getGroup());
        String equipRef = configPoint.getEquipRef();
        if (configPoint.getMarkers().contains(Tags.RELAY1)) {
            relayType = Tags.RELAY1;
        } else if (configPoint.getMarkers().contains(Tags.RELAY2)) {
            relayType = Tags.RELAY2;
        }
        HashMap relayConfigPoint = hayStack.read("point and config and " + relayType + " and " +
                "enabled and equipRef == \"" + equipRef + "\"");
        HashMap relayOnThresholdPoint = hayStack.read("point and config and " + relayType + " and" +
                " on and threshold and equipRef == \"" + equipRef + "\"");
        HashMap relayOffThresholdPoint =
                hayStack.read("point and config and " + relayType + " and off and threshold and " +
                        "equipRef == \"" + equipRef + "\"");
        HashMap relaycmdholdPoint =
                hayStack.read("point and " + relayType + " and cmd and equipRef == \"" + equipRef + "\"");

        String relayConfigId = relayConfigPoint.get("id").toString();
        String relayCmdId = relaycmdholdPoint.get("id").toString();
        String relayOnThresholdId = relayOnThresholdPoint.get("id").toString();
        String relayOffThresholdId = relayOffThresholdPoint.get("id").toString();

        if (configPoint.getMarkers().contains("on")) {
            hayStack.writeDefaultValById(relayOnThresholdId, configVal);
        }

        if (configPoint.getMarkers().contains("off")) {
            hayStack.writeDefaultValById(relayOffThresholdId, configVal);
        }


        if (relayType.contains(Tags.RELAY1) && configPoint.getMarkers().contains("enabled")) {
            SmartNode.updatePhysicalPointRef(nodeAddress, Port.RELAY_ONE.name(), relayCmdId);
            SmartNode.setPointEnabled(nodeAddress, Port.RELAY_ONE.name(),
                    configVal > 0);
            SmartNode.updatePhysicalPointType(nodeAddress, Port.RELAY_ONE.name(),
                    OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, configVal);

        } else if (relayType.contains(Tags.RELAY2) && configPoint.getMarkers().contains("enabled")) {
            SmartNode.updatePhysicalPointRef(nodeAddress, Port.RELAY_TWO.name(), relayCmdId);
            SmartNode.setPointEnabled(nodeAddress, Port.RELAY_TWO.name(), configVal > 0);
            SmartNode.updatePhysicalPointType(nodeAddress, Port.RELAY_TWO.name(),
                    OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, configVal);

        }

        CCUHsApi.getInstance().scheduleSync();
    }

    public static String createProcessVariablePoint(String floorRef, String roomRef, String type,
                                                     String nodeAddr, String equipRef, int val) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;
        Bundle bundle = new Bundle();
        if (type.equals(Tags.ANALOG1)) {
            bundle = getAnalog1Bundle(val);
        } else if (type.equals(Tags.TH1)) {
            bundle = getThermistorBundle(val);
        } else if (type.equals("native")) {
            bundle = getNativeSensorBundle(val);
        }

        String shortDis = bundle.getString("shortDis");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String[] markers = bundle.getStringArray("markers");

        Point.Builder processVariableTag = new Point.Builder()
                .setDisplayName(equipDis + "-processVariable- " + shortDis)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis(shortDis)
                .setHisInterpolate("cov")
                .addMarker("logical").addMarker("pid").addMarker("zone").addMarker("his").addMarker("cur")
                .addMarker("sp")
                .addMarker("process").addMarker("variable")
                .setGroup(String.valueOf(nodeAddr))
                .setMinVal(minVal)
                .setMaxVal(maxVal)
                .setUnit(unit)
                .setTz(tz);
        if (markers != null) {
            for (String marker : markers) {
                processVariableTag.addMarker(marker);
            }
        }

        String processVariableTagId = hayStack.addPoint(processVariableTag.build());
        hayStack.writeHisValById(processVariableTagId, 0.0);

        return processVariableTagId;
    }

    public static String createTargetValuePoint(String floorRef, String roomRef, String type,
                                                 String nodeAddr, String equipRef, int val) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Bundle bundle = new Bundle();
        if (type.equals(Tags.ANALOG1)) {
            bundle = getAnalog1Bundle(val);
        } else if (type.equals(Tags.TH1)) {
            bundle = getThermistorBundle(val);
        } else if (type.equals("native")) {
            bundle = getNativeSensorBundle(val);
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
        hayStack.writeDefaultValById(pidTargetValueId, Double.valueOf(minVal));
        hayStack.writeHisValueByIdWithoutCOV(pidTargetValueId, Double.valueOf(minVal));

        return pidTargetValueId;
    }

    public static Bundle getThermistorBundle(int thermistorInputSensor) {
        Bundle mBundle = new Bundle();
        String shortDis = "Temperature";
        String shortDisTarget = "Target Temperature";
        String unit = "\u00B0F";
        String maxVal = "302";
        String minVal = "-40";
        String incrementVal = "0.5";
        String[] markers = null;
        switch (thermistorInputSensor) {
            case 0:
            case 1:
            case 2:
                shortDis = "Temperature";
                shortDisTarget = "Target Temperature";
                unit = "\u00B0F";
                maxVal = "302";
                minVal = "-40";
                incrementVal = "0.5";
                markers = new String[]{"temp"};
                break;
        }

        mBundle.putString("shortDis", shortDis);
        mBundle.putString("shortDisTarget", shortDisTarget);
        mBundle.putString("unit", unit);
        mBundle.putString("maxVal", maxVal);
        mBundle.putString("minVal", minVal);
        mBundle.putString("incrementVal", incrementVal);
        mBundle.putStringArray("markers", markers);

        return mBundle;
    }

    /**
     * Dynamically generates whole bunch of String parameters required for creating NativeSensor
     * related points.
     */
    public static Bundle getNativeSensorBundle(int nativeSensorInput) {
        Bundle mBundle = new Bundle();
        NativeSensor selectedSensor =
                SensorManager.getInstance().getNativeSensorList().get(nativeSensorInput - 1);
        String shortDis = selectedSensor.sensorName;

        //Does the name formatting as it is done with the existing sensor types.
        //ShortDisTarget to have everything stripped off except the sensor type like (CO2/Sound etc)
        String shortDisTarget = shortDis.replace("Native-", "Target ");
        String marker = selectedSensor.sensorName
                .replace("Native-", "")
                .replaceAll("\\s", "")
                .toLowerCase();


        mBundle.putString("shortDis", shortDis);
        mBundle.putString("shortDisTarget", shortDisTarget);
        mBundle.putString("unit", selectedSensor.engineeringUnit);
        mBundle.putString("maxVal", String.valueOf(selectedSensor.maxEngineeringValue));
        mBundle.putString("minVal", String.valueOf(selectedSensor.minEngineeringValue));
        mBundle.putString("incrementVal", String.valueOf(selectedSensor.incrementEngineeringValue));
        mBundle.putStringArray("markers", new String[]{marker});

        return mBundle;
    }

    public static Bundle getAnalog1Bundle(int analog1InputSensor) {
        Bundle mBundle = new Bundle();
        String shortDis = "Generic 0-10 Voltage";
        String shortDisTarget = "Target Voltage";
        String unit = "V";
        String maxVal = "10";
        String minVal = "0";
        String incrementVal = "0.1";
        String[] markers = null;
        switch (analog1InputSensor) {
            case 0:
            case 1:
                shortDis = "Generic 0-10 Voltage";
                shortDisTarget = "Target Voltage";
                unit = "V";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                markers = null;
                break;
            case 2:
                shortDis = "Pressure [0-2 in.]";
                shortDisTarget = "Target Pressure";
                unit = "Inch wc";
                maxVal = "2";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"pressure"};
                break;
            case 3:
                shortDis = "Pressure[0-0.25 in. Differential]";
                shortDisTarget = "Target Pressure Differential";
                unit = "Inch wc";
                maxVal = "0.25";
                minVal = "-0.25";
                incrementVal = "0.01";
                markers = new String[]{"pressure"};
                break;
            case 4:
                shortDis = "Airflow";
                shortDisTarget = "Target Airflow";
                unit = "CFM";
                maxVal = "1000";
                minVal = "0";
                incrementVal = "10";
                markers = new String[]{"airflow"};
                break;
            case 5:
                shortDis = "Humidity";
                shortDisTarget = "Target Humidity";
                unit = "%";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"humidity"};
                break;
            case 6:
                shortDis = "CO2 Level";
                shortDisTarget = "Target CO2 Level";
                unit = "ppm";
                maxVal = "2000";
                minVal = "0";
                incrementVal = "100";
                markers = new String[]{"co2"};
                break;
            case 7:
                shortDis = "CO Level";
                shortDisTarget = "Target CO Level";
                unit = "ppm";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"co"};
                break;
            case 8:
                shortDis = "NO2 Level";
                shortDisTarget = "Target NO2 Level";
                unit = "ppm";
                maxVal = "5";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"no2"};
                break;
            case 9:
                shortDis = "Current Drawn[CT 0-10]";
                shortDisTarget = "Target Current Draw";
                unit = "amps";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 10:
                shortDis = "Current Drawn[CT 0-20]";
                shortDisTarget = "Target Current Draw";
                unit = "amps";
                maxVal = "20";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 11:
                shortDis = "Current Drawn[CT 0-50]";
                shortDisTarget = "Target Current Draw";
                unit = "amps";
                maxVal = "50";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 12:
                shortDis = "ION Density";
                shortDisTarget = "Target Ion Density";
                unit = "ions/cc";
                maxVal = "1000000";
                minVal = "0";
                incrementVal = "1000";
                markers = new String[]{"ion", "density"};
                break;
        }

        mBundle.putString("shortDis", shortDis);
        mBundle.putString("shortDisTarget", shortDisTarget);
        mBundle.putString("unit", unit);
        mBundle.putString("maxVal", maxVal);
        mBundle.putString("minVal", minVal);
        mBundle.putString("incrementVal", incrementVal);
        mBundle.putStringArray("markers", markers);

        return mBundle;
    }

    private static String createDynamicTargetInputPoint(int inputSensor, String floorRef,
                                                        String roomRef,
                                                        String nodeAddr, String equipRef) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-PID-" + nodeAddr;
        String tz = siteMap.get("tz").toString();

        Bundle bundle = getAnalog2Bundle(inputSensor);
        String shortDis = bundle.getString("shortDis");
        String shortDisTarget = bundle.getString("shortDisTarget");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String incrementVal = bundle.getString("incrementVal");
        String[] markers = bundle.getStringArray("markers");

        Point.Builder dynamicTargetValueTag = new Point.Builder()
                .setDisplayName(equipDis + "-dynamicTargetValue-" + shortDis)
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

        String dynamicTargetValueTagId =
                CCUHsApi.getInstance().addPoint(dynamicTargetValueTag.build());
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(dynamicTargetValueTagId, 0.0);


        return dynamicTargetValueTagId;
    }

    private static String createSetpointSensorOffsetPoint(int spSensorOffset, String floorRef,
                                                          String roomRef,
                                                          String nodeAddr, String equipRef) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-PID-" + nodeAddr;
        String tz = siteMap.get("tz").toString();

        Bundle bundle = getAnalog2Bundle(spSensorOffset);

        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String incrementVal = bundle.getString("incrementVal");


        Point setpointSensorOffset = new Point.Builder()
                .setDisplayName(equipDis + "-setpointSensorOffset")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("writable")
                .addMarker("setpoint").addMarker("sensor").addMarker("offset").addMarker("his")
                .setMinVal(minVal)
                .setMaxVal(maxVal)
                .setIncrementVal(incrementVal)
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
        String setpointSensorOffsetId = CCUHsApi.getInstance().addPoint(setpointSensorOffset);
        CCUHsApi.getInstance().writeDefaultValById(setpointSensorOffsetId, minVal);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(setpointSensorOffsetId, Double.valueOf(minVal));
        return setpointSensorOffsetId;
    }

    public static Bundle getAnalog2Bundle(int dynamicInputSensor) {
        Bundle mBundle = new Bundle();
        String shortDis = "Generic 0-10 Voltage";
        String shortDisTarget = "Dynamic Target Voltage";
        String unit = "V";
        String maxVal = "10";
        String minVal = "0";
        String incrementVal = "0.1";
        String[] markers = null;
        switch (dynamicInputSensor) {
            case 0:
                shortDis = "Generic 0-10 Voltage";
                shortDisTarget = "Dynamic Target Voltage";
                unit = "V";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                markers = null;
                break;
            case 1:
                shortDis = "Pressure [0-2 in.]";
                shortDisTarget = "Dynamic Target Pressure";
                unit = "Inch wc";
                maxVal = "2";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"pressure"};
                break;
            case 2:
                shortDis = "Pressure[0-0.25 in. Differential]";
                shortDisTarget = "Dynamic Target Pressure Differential";
                unit = "Inch wc";
                maxVal = "0.25";
                minVal = "-0.25";
                incrementVal = "0.01";
                markers = new String[]{"pressure"};
                break;
            case 3:
                shortDis = "Airflow";
                shortDisTarget = "Dynamic Target Airflow";
                unit = "CFM";
                maxVal = "1000";
                minVal = "0";
                incrementVal = "10";
                markers = new String[]{"airflow"};
                break;
            case 4:
                shortDis = "Humidity";
                shortDisTarget = "Dynamic Target Humidity";
                unit = "%";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"humidity"};
                break;
            case 5:
                shortDis = "CO2 Level";
                shortDisTarget = "Dynamic Target CO2 Level";
                unit = "ppm";
                maxVal = "2000";
                minVal = "0";
                incrementVal = "100";
                markers = new String[]{"co2"};
                break;
            case 6:
                shortDis = "CO Level";
                shortDisTarget = "Dynamic Target CO Level";
                unit = "ppm";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"co"};
                break;
            case 7:
                shortDis = "NO2 Level";
                shortDisTarget = "Dynamic Target NO2 Level";
                unit = "ppm";
                maxVal = "5";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"no2"};
                break;
            case 8:
                shortDis = "Current Drawn[CT 0-10]";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "amps";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 9:
                shortDis = "Current Drawn[CT 0-20]";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "amps";
                maxVal = "20";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 10:
                shortDis = "Current Drawn[CT 0-50]";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "amps";
                maxVal = "50";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
        }

        mBundle.putString("shortDis", shortDis);
        mBundle.putString("shortDisTarget", shortDisTarget);
        mBundle.putString("unit", unit);
        mBundle.putString("maxVal", maxVal);
        mBundle.putString("minVal",minVal);
        mBundle.putString("incrementVal", incrementVal);
        mBundle.putStringArray("markers", markers);

        return mBundle;
    }
}
