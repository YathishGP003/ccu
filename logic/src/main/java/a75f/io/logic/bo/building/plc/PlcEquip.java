package a75f.io.logic.bo.building.plc;

import android.os.Bundle;

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
                .setUnit("Ohm")
                .setTz(tz)
                .build();
        String th1InputSensorId = hayStack.addPoint(th1InputSensor);
        hayStack.writeDefaultValById(th1InputSensorId, (double) config.th1InputSensor);

        Point analog1AtMinOutput = new Point.Builder()
                .setDisplayName(equipDis + "-analog1AtMinOutput")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("his").addMarker("writable")
                .addMarker("analog1").addMarker("min").addMarker("output")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("V")
                .setTz(tz)
                .build();
        String analog1AtMinOutputId = hayStack.addPoint(analog1AtMinOutput);
        hayStack.writeDefaultValById(analog1AtMinOutputId, config.analog1AtMinOutput);
        hayStack.writeHisValById(analog1AtMinOutputId, config.analog1AtMinOutput);

        Point analog1AtMaxOutput = new Point.Builder()
                .setDisplayName(equipDis + "-analog1AtMaxOutput")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("his").addMarker("writable")
                .addMarker("analog1").addMarker("max").addMarker("output")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("V")
                .setTz(tz)
                .build();
        String analog1AtMaxOutputId = hayStack.addPoint(analog1AtMaxOutput);
        hayStack.writeDefaultValById(analog1AtMaxOutputId, config.analog1AtMaxOutput);
        hayStack.writeHisValById(analog1AtMaxOutputId, config.analog1AtMaxOutput);

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
            updateTargetValue(config.pidTargetValue,floorRef, roomRef, getAnalog1Bundle(config.analog1InputSensor));
            if (!config.useAnalogIn2ForSetpoint) {
                updateProportionalRange(config.pidProportionalRange, floorRef, roomRef, getAnalog1Bundle(config.analog1InputSensor));
            }
            String processVariableId = updateProcessVariable(floorRef, roomRef, processVar, getAnalog1Bundle(config.analog1InputSensor));
            device.analog1In.setPointRef(processVariableId);
            device.analog1In.setEnabled(true);
            device.analog1In.setType(String.valueOf(config.analog1InputSensor - 1));
        } else {
            updateTargetValue(config.pidTargetValue,floorRef, roomRef, getThermistorBundle(config.th1InputSensor));
            if (!config.useAnalogIn2ForSetpoint){
                updateProportionalRange(config.pidProportionalRange,floorRef, roomRef,getThermistorBundle(config.th1InputSensor));
            }
            String processVariableId = updateProcessVariable(floorRef, roomRef, processVar, getThermistorBundle(config.th1InputSensor));
            device.th1In.setPointRef(processVariableId);
            device.th1In.setEnabled(true);
            device.th1In.setType(String.valueOf(config.th1InputSensor - 1));
        }

        if (config.useAnalogIn2ForSetpoint) {
            String setPointVariableId = updateDynamicTargetInput(config.analog2InputSensor, floorRef, roomRef, dynamicTargetTag);
            updateProportionalRange(config.pidProportionalRange, floorRef, roomRef, getAnalog2Bundle(config.analog2InputSensor));
            device.analog2In.setPointRef(setPointVariableId);
            device.analog2In.setEnabled(true);
            device.analog2In.setType(String.valueOf(config.analog2InputSensor));
            updateOffsetSensorValue(config.setpointSensorOffset, floorRef, roomRef);
        }

        device.analog1Out.setPointRef(controlVariableId);
        device.analog1Out.setType(config.analog1AtMinOutput + "-" + config.analog1AtMaxOutput);
        device.analog1Out.setEnabled(true);
        device.addPointsToDb();

        hayStack.syncEntityTree();
    }

    private Bundle getAnalog1Bundle(int analog1InputSensor) {
        Bundle mBundle = new Bundle();
        String shortDis = "Generic 0-10 Voltage";
        String unit = "V";
        String maxVal = "10";
        String incrementVal = "0.1";
        String[] markers = null;

        if (analog1InputSensor == 0 || analog1InputSensor == 1) {
            shortDis = "Generic 0-10 Voltage";
            unit = "V";
            maxVal = "10";
            incrementVal = "0.1";
            markers = null;
        } else if (analog1InputSensor == 2) {
            shortDis = "Pressure [0-2 in.]";
            unit = "Inch wc";
            maxVal = "2";
            incrementVal = "0.1";
            markers = new String[]{"pressure"};
        } else if (analog1InputSensor == 3) {
            shortDis = "Pressure[0-0.25 in. Differential]";
            unit = "Inch wc";
            maxVal = "0.25";
            incrementVal = "0.01";
            markers = new String[]{"pressure"};
        } else if (analog1InputSensor == 4) {
            shortDis = "Airflow";
            unit = "CFM";
            maxVal = "1000";
            incrementVal = "10";
            markers = new String[]{"airflow"};
        } else if (analog1InputSensor == 5) {
            shortDis = "Humidity";
            unit = "%";
            maxVal = "100";
            incrementVal = "1.0";
            markers = new String[]{"humidity"};
        } else if (analog1InputSensor == 6) {
            shortDis = "CO2 Level";
            unit = "ppm";
            maxVal = "2000";
            incrementVal = "100";
            markers = new String[]{"co2"};
        } else if (analog1InputSensor == 7) {
            shortDis = "CO Level";
            unit = "ppm";
            maxVal = "100";
            incrementVal = "1.0";
            markers = new String[]{"co"};
        } else if (analog1InputSensor == 8) {
            shortDis = "NO2 Level";
            unit = "ppm";
            maxVal = "5";
            incrementVal = "0.1";
            markers = new String[]{"no2"};
        } else if (analog1InputSensor == 9) {
            shortDis = "Current Drawn[CT 0-10]";
            unit = "A";
            maxVal = "10";
            incrementVal = "0.1";
            markers = new String[]{"current","transformer"};
        } else if (analog1InputSensor == 10) {
            shortDis = "Current Drawn[CT 0-20]";
            unit = "A";
            maxVal = "20";
            incrementVal = "0.1";
            markers = new String[]{"current","transformer"};
        } else if (analog1InputSensor == 11) {
            shortDis = "Current Drawn[CT 0-30]";
            unit = "A";
            maxVal = "50";
            incrementVal = "0.1";
            markers = new String[]{"current","transformer"};
        }
        mBundle.putString("shortDis",shortDis);
        mBundle.putString("unit",unit);
        mBundle.putString("maxVal",maxVal);
        mBundle.putString("incrementVal",incrementVal);
        mBundle.putStringArray("markers",markers);

        return mBundle;
    }

    private Bundle getAnalog2Bundle(int dynamicInputSensor) {
        Bundle mBundle = new Bundle();
        String shortDis = "Generic 0-10 Voltage";
        String unit = "V";
        String maxVal = "10";
        String incrementVal = "0.1";
        String[] markers = null;

        if (dynamicInputSensor == 0) {
            shortDis = "Generic 0-10 Voltage";
            unit = "V";
            maxVal = "10";
            incrementVal = "0.1";
            markers = null;
        } else if (dynamicInputSensor == 1) {
            shortDis = "Pressure [0-2 in.]";
            unit = "Inch wc";
            maxVal = "2";
            incrementVal = "0.1";
            markers = new String[]{"pressure"};
        } else if (dynamicInputSensor == 2) {
            shortDis = "Pressure[0-0.25 in. Differential]";
            unit = "Inch wc";
            maxVal = "0.25";
            incrementVal = "0.01";
            markers = new String[]{"pressure"};
        } else if (dynamicInputSensor == 3) {
            shortDis = "Airflow";
            unit = "CFM";
            maxVal = "1000";
            incrementVal = "10";
            markers = new String[]{"airflow"};
        } else if (dynamicInputSensor == 4) {
            shortDis = "Humidity";
            unit = "%";
            maxVal = "100";
            incrementVal = "1.0";
            markers = new String[]{"humidity"};
        } else if (dynamicInputSensor == 5) {
            shortDis = "CO2 Level";
            unit = "ppm";
            maxVal = "2000";
            incrementVal = "100";
            markers = new String[]{"co2"};
        } else if (dynamicInputSensor == 6) {
            shortDis = "CO Level";
            unit = "ppm";
            maxVal = "100";
            incrementVal = "1.0";
            markers = new String[]{"co"};
        } else if (dynamicInputSensor == 7) {
            shortDis = "NO2 Level";
            unit = "ppm";
            maxVal = "5";
            incrementVal = "0.1";
            markers = new String[]{"no2"};
        } else if (dynamicInputSensor == 8) {
            shortDis = "Current Drawn[CT 0-10]";
            unit = "A";
            maxVal = "10";
            incrementVal = "0.1";
            markers = new String[]{"current", "transformer"};
        } else if (dynamicInputSensor == 9) {
            shortDis = "Current Drawn[CT 0-20]";
            unit = "A";
            maxVal = "20";
            incrementVal = "0.1";
            markers = new String[]{"current", "transformer"};
        } else if (dynamicInputSensor == 10) {
            shortDis = "Current Drawn[CT 0-30]";
            unit = "A";
            maxVal = "50";
            incrementVal = "0.1";
            markers = new String[]{"current", "transformer"};
        }
        mBundle.putString("shortDis", shortDis);
        mBundle.putString("unit", unit);
        mBundle.putString("maxVal", maxVal);
        mBundle.putString("incrementVal", incrementVal);
        mBundle.putStringArray("markers", markers);

        return mBundle;
    }

    private Bundle getThermistorBundle(int thermistorInputSensor) {
        Bundle mBundle = new Bundle();
        String shortDis = "10K type 2 probe \u00B0F";
        String unit = "\u00B0F";
        String maxVal = "10";
        String incrementVal = "0.1";
        String[] markers = null;

        if (thermistorInputSensor == 0 || thermistorInputSensor == 1) {
            shortDis = "10K type 2 probe \u00B0F";
            unit = "\u00B0F";
            maxVal = "10";
            incrementVal = "0.1";
            markers = null;
        } else if (thermistorInputSensor == 2) {
            shortDis = "Generic 1-100k ohms \u00B0F";
            unit = "\u00B0F";
            maxVal = "10";
            incrementVal = "0.1";
            markers = null;
        }
        mBundle.putString("shortDis", shortDis);
        mBundle.putString("unit", unit);
        mBundle.putString("maxVal", maxVal);
        mBundle.putString("incrementVal", incrementVal);
        mBundle.putStringArray("markers", markers);

        return mBundle;
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
        HashMap offsetSensorPoint = hayStack.read("point and config and setpoint and sensor and offset and equipRef == \"" + equipRef + "\"");
        HashMap prangePoint = hayStack.read("point and config and prange and equipRef == \"" + equipRef + "\"");

        //delete last point
        if (dynamicTarget != null && dynamicTarget.get("id") != null && (config.useAnalogIn2ForSetpoint && getProfileConfiguration().analog2InputSensor != config.analog2InputSensor)) {
            CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
            CCUHsApi.getInstance().deleteEntityTree(prangePoint.get("id").toString());
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
                updateProportionalRange(config.pidProportionalRange,floorRef,roomRef,getAnalog2Bundle(config.analog2InputSensor));
            }
            // add offset sensor point
            if (offsetSensorPoint == null || offsetSensorPoint.get("id") == null){
                updateOffsetSensorValue(config.setpointSensorOffset, floorRef, roomRef);
            }
            // add offset sensor point
            if (prangePoint == null || prangePoint.get("id") == null){
                updateProportionalRange(config.pidProportionalRange,floorRef,roomRef,getAnalog2Bundle(config.analog2InputSensor));
            }

            SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_TWO.name(), true);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_TWO.name(), id);
            SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_TWO.name(), String.valueOf(config.analog2InputSensor));
            // add offset value with dynamic target value and update
            setSpVariable(getSpVariable() + getSpSensorOffset());
        }

        HashMap processVariable = hayStack.read("point and process and variable and equipRef == \"" + equipRef + "\"");

        //delete  processVariable last point
        if (processVariable.get("id") != null && (config.analog1InputSensor > 0 && getProfileConfiguration().analog1InputSensor != config.analog1InputSensor)
                || (config.th1InputSensor > 0 && getProfileConfiguration().th1InputSensor != config.th1InputSensor)) {

            CCUHsApi.getInstance().deleteEntityTree(processVariable.get("id").toString());
            if (!config.useAnalogIn2ForSetpoint){
                CCUHsApi.getInstance().deleteEntityTree(prangePoint.get("id").toString());
            }
        }

        if (config.analog1InputSensor > 0) {
            //add target value points and offset sensor if useAnalogIn2ForSetpoint off
            if (!config.useAnalogIn2ForSetpoint){
                if (dynamicTarget != null && dynamicTarget.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(dynamicTarget.get("id").toString());
                }

                if (offsetSensorPoint != null && offsetSensorPoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(offsetSensorPoint.get("id").toString());
                }
                
                if (targetValuePoint == null || targetValuePoint.get("id") == null){
                    updateTargetValue(config.pidTargetValue, floorRef, roomRef, getAnalog1Bundle(config.analog1InputSensor));
                }

                if (prangePoint == null || prangePoint.get("id") == null){
                    updateProportionalRange(config.pidProportionalRange, floorRef, roomRef, getAnalog1Bundle(config.analog1InputSensor));
                }
            }

            String id = processVariable.get("id").toString();
            if (getProfileConfiguration().analog1InputSensor != config.analog1InputSensor) {
                id = updateProcessVariable(floorRef, roomRef, processTag, getAnalog1Bundle(config.analog1InputSensor));
                if (!config.useAnalogIn2ForSetpoint) {
                    updateProportionalRange(config.pidProportionalRange, floorRef, roomRef, getAnalog1Bundle(config.analog1InputSensor));
                }

                //delete  and update target values
                if (targetValuePoint != null && targetValuePoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                    if (!config.useAnalogIn2ForSetpoint) {
                        updateTargetValue(config.pidTargetValue, floorRef, roomRef, getAnalog1Bundle(config.analog1InputSensor));
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

                if (offsetSensorPoint != null && offsetSensorPoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(offsetSensorPoint.get("id").toString());
                }

                if (targetValuePoint == null || targetValuePoint.get("id") == null){
                    updateTargetValue(config.pidTargetValue, floorRef, roomRef, getThermistorBundle(config.th1InputSensor));
                }
                if (prangePoint == null || prangePoint.get("id") == null){
                    updateProportionalRange(config.pidProportionalRange, floorRef, roomRef, getThermistorBundle(config.th1InputSensor));
                }
            }

            String id = processVariable.get("id").toString();
            if (getProfileConfiguration().th1InputSensor != config.th1InputSensor) {
                id = updateProcessVariable(floorRef, roomRef, processTag, getThermistorBundle(config.th1InputSensor));
                if (!config.useAnalogIn2ForSetpoint) {
                    updateProportionalRange(config.pidProportionalRange, floorRef, roomRef, getThermistorBundle(config.th1InputSensor));
                }

                //delete  and update target values
                if (targetValuePoint != null && targetValuePoint.get("id") != null){
                    CCUHsApi.getInstance().deleteEntityTree(targetValuePoint.get("id").toString());
                    if (!config.useAnalogIn2ForSetpoint) {
                        updateTargetValue(config.pidTargetValue, floorRef, roomRef, getThermistorBundle(config.th1InputSensor));
                    }
                }
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
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.toString(), (int) config.analog1AtMinOutput + "-" + (int) config.analog1AtMaxOutput);


        targetValue = config.pidTargetValue;
        spSensorOffset = config.setpointSensorOffset;
        isEnabledAnalog2InForSp = config.useAnalogIn2ForSetpoint;
        isEnabledZeroErrorMidpoint = config.expectZeroErrorAtMidpoint;
    }

    private String updateProcessVariable(String floorRef, String roomRef, String processTag, Bundle bundle) {

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;

        String shortDis = bundle.getString("shortDis");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String[] markers = bundle.getStringArray("markers");

        Point.Builder processVariableTag = new Point.Builder()
                .setDisplayName(equipDis + "-processVariable- " + processTag)
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
                .setMinVal("0")
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

    private String updateDynamicTargetInput(int inputSensor, String floorRef, String roomRef, String dynamicTargetTag) {

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-PID-" + nodeAddr;
        String tz = siteMap.get("tz").toString();

        Bundle bundle = getAnalog2Bundle(inputSensor);
        String shortDis = bundle.getString("shortDis");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String incrementVal = bundle.getString("incrementVal");

        Point DynamicTargetValueTag = new Point.Builder()
                .setDisplayName(equipDis +"-dynamicTargetValue-" + dynamicTargetTag)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("Dynamic"+" "+shortDis)
                .setHisInterpolate("cov")
                .addMarker("logical").addMarker("pid").addMarker("zone").addMarker("his").addMarker("cur")
                .addMarker("sp")
                .addMarker("dynamic").addMarker("target").addMarker("value")
                .setGroup(String.valueOf(nodeAddr))
                .setMinVal("0")
                .setMaxVal(maxVal)
                .setIncrementVal(incrementVal)
                .setUnit(unit)
                .setTz(tz)
                .build();
        String DynamicTargetValueTagId = hayStack.addPoint(DynamicTargetValueTag);
        hayStack.writeHisValById(DynamicTargetValueTagId, 0.0);

        return DynamicTargetValueTagId;
    }

    private String updateOffsetSensorValue(double spSensorOffset,String floorRef, String roomRef) {

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
        hayStack.writeHisValById(setpointSensorOffsetId, spSensorOffset);
        return setpointSensorOffsetId;
    }

    private String updateTargetValue(double targetValue, String floorRef, String roomRef, Bundle bundle){

        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;

        String shortDis = bundle.getString("shortDis");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String incrementVal = bundle.getString("incrementVal");

        Point pidTargetValue = new Point.Builder()
                .setDisplayName(equipDis + "-pidTargetValue")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("Target"+" "+shortDis)
                .addMarker("config").addMarker("pid").addMarker("zone").addMarker("his").addMarker("writable")
                .addMarker("target").addMarker("value")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit(unit)
                .setMinVal("0")
                .setMaxVal(maxVal)
                .setIncrementVal(incrementVal)
                .setTz(tz)
                .build();
        String pidTargetValueId = hayStack.addPoint(pidTargetValue);
        hayStack.writeDefaultValById(pidTargetValueId,targetValue);
        hayStack.writeHisValById(pidTargetValueId, targetValue);

        return pidTargetValueId;
    }

    private String updateProportionalRange(double proportionalRange, String floorRef, String roomRef, Bundle bundle){
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-PID-" + nodeAddr;

        String maxVal = bundle.getString("maxVal");
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
                .setMinVal("0")
                .setMaxVal(maxVal)
                .setIncrementVal(incrementVal)
                .setTz(tz)
                .build();
        String pidProportionalRangeId = hayStack.addPoint(pidProportionalRange);
        hayStack.writeDefaultValById(pidProportionalRangeId, (double) proportionalRange);
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
