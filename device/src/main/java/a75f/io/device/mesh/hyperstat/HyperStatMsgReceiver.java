package a75f.io.device.mesh.hyperstat;

import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.device.HyperStat;
import a75f.io.device.HyperStat.HyperStatLocalControlsOverrideMessage_t;
import a75f.io.device.HyperStat.HyperStatRegularUpdateMessage_t;
import a75f.io.device.HyperStat.HyperStatIduStatusMessage_t;
import a75f.io.device.mesh.AnalogUtil;
import a75f.io.device.mesh.DLog;
import a75f.io.device.mesh.DeviceHSUtil;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.mesh.ThermistorUtil;
import a75f.io.device.serial.CcuToCmOverUsbDeviceTempAckMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil;
import a75f.io.logic.bo.building.hyperstat.common.PossibleConditioningMode;
import a75f.io.logic.bo.building.hyperstat.common.PossibleFanMode;
import a75f.io.logic.bo.building.sensors.SensorType;
import a75f.io.logic.bo.haystack.device.HyperStatDevice;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.jobs.HyperStatScheduler;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.pubnub.ZoneDataInterface;

import static a75f.io.device.mesh.Pulse.getHumidityConversion;

import android.util.Log;

public class HyperStatMsgReceiver {
    
    private static final int HYPERSTAT_MESSAGE_ADDR_START_INDEX = 1;
    private static final int HYPERSTAT_MESSAGE_ADDR_END_INDEX = 5;
    private static final int HYPERSTAT_MESSAGE_TYPE_INDEX = 13;
    private static final int HYPERSTAT_SERIALIZED_MESSAGE_START_INDEX = 17;
    private static ZoneDataInterface currentTempInterface = null;
    
    public static void processMessage(byte[] data, CCUHsApi hayStack) {
        try {
    
            /*
            * Message Type - 1 byte
            * Address - 4 bytes
            * CM lqi - 4 bytes
            * CM rssi - 4 bytes
            * Message types - 4 bytes
            *
            * Actual Serialized data starts at index 17.
            */
            CcuLog.e(L.TAG_CCU_DEVICE, "processMessage processMessage :"+ Arrays.toString(data));
    
            byte[] addrArray = Arrays.copyOfRange(data, HYPERSTAT_MESSAGE_ADDR_START_INDEX,
                                                  HYPERSTAT_MESSAGE_ADDR_END_INDEX);
            int address = ByteBuffer.wrap(addrArray)
                                    .order(ByteOrder.LITTLE_ENDIAN)
                                    .getInt();
    
            MessageType messageType = MessageType.values()[data[HYPERSTAT_MESSAGE_TYPE_INDEX]];
    
            byte[] messageArray = Arrays.copyOfRange(data, HYPERSTAT_SERIALIZED_MESSAGE_START_INDEX, data.length);
            
            if (messageType == MessageType.HYPERSTAT_REGULAR_UPDATE_MESSAGE) {
                HyperStatRegularUpdateMessage_t regularUpdate =
                        HyperStatRegularUpdateMessage_t.parseFrom(messageArray);
    
                handleRegularUpdate(regularUpdate, address, hayStack);
            } else if (messageType == MessageType.HYPERSTAT_LOCAL_CONTROLS_OVERRIDE_MESSAGE) {
                HyperStatLocalControlsOverrideMessage_t overrideMessage =
                        HyperStatLocalControlsOverrideMessage_t.parseFrom(messageArray);
                handleOverrideMessage(overrideMessage, address, hayStack);
            } else if (messageType == MessageType.HYPERSTAT_IDU_STATUS_MESSAGE) {
                HyperStatIduStatusMessage_t p1p2Status =
                    HyperStatIduStatusMessage_t.parseFrom(messageArray);
                HyperStatIduMessageHandler.handleIduStatusMessage(p1p2Status, address, hayStack);
            }
    
            if(currentTempInterface != null) {
                currentTempInterface.refreshScreen(null);
            }
            
        } catch (InvalidProtocolBufferException e) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Cant parse protobuf data: "+e.getMessage());
        }
    }
    
    private static void handleRegularUpdate(HyperStatRegularUpdateMessage_t regularUpdateMessage, int nodeAddress,
                                     CCUHsApi hayStack) {
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_DEVICE, "handleRegularUpdate: "+regularUpdateMessage.toString());
        }
        HashMap device = hayStack.read("device and addr == \"" + nodeAddress + "\"");
        DeviceHSUtil.getEnabledSensorPointsWithRefForDevice(device, hayStack)
                    .forEach( point -> writePortInputsToHaystackDatabase( point, regularUpdateMessage, hayStack));
        
        if (regularUpdateMessage.getSensorReadingsList().size() > 0) {
            writeSensorInputsToHaystackDatabase(regularUpdateMessage.getSensorReadingsList(), nodeAddress);
        }
    }
    
    private static void handleOverrideMessage(HyperStatLocalControlsOverrideMessage_t message, int nodeAddress,
                                              CCUHsApi hayStack) {

        CcuLog.i(L.TAG_CCU_DEVICE, "OverrideMessage: "+message.toString());

        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+nodeAddress+"\"");
        Equip hsEquip = new Equip.Builder().setHashMap(equipMap).build();

        writeDesiredTemp(message, hsEquip, hayStack);
        updateConditioningMode(hsEquip.getId(),message.getConditioningModeValue(),nodeAddress);
        updateFanMode(hsEquip.getId(),message.getFanSpeed().ordinal(),nodeAddress);

        /** send the updated control  message*/
        HyperStatMessageSender.sendControlMessage(nodeAddress, hsEquip.getId());
        sendAcknowledge(nodeAddress);
    }
    
    private static void writePortInputsToHaystackDatabase(RawPoint rawPoint,
                                                     HyperStatRegularUpdateMessage_t regularUpdateMessage,
                                                     CCUHsApi hayStack) {
        Point point = DeviceHSUtil.getLogicalPointForRawPoint(rawPoint, hayStack);
    
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_DEVICE,
                     "writePortInputsToHaystackDatabase: logical point for " + rawPoint.getDisplayName() + " " + point);
        }
        if (point == null) {
            return;
        }
        if (Port.valueOf(rawPoint.getPort()) == Port.RSSI) {
            writeHeartbeat(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_RT) {
            writeRoomTemp(rawPoint, point, regularUpdateMessage, hayStack);
        } else if (Port.valueOf(rawPoint.getPort()) == Port.TH1_IN) {
            writeThermistorVal(rawPoint, point, hayStack,
                               regularUpdateMessage.getExternalThermistorInput1());
        } else if (Port.valueOf(rawPoint.getPort()) == Port.TH2_IN) {
            writeThermistorVal(rawPoint, point, hayStack,
                               regularUpdateMessage.getExternalThermistorInput2());
        } else if (Port.valueOf(rawPoint.getPort()) == Port.ANALOG_IN_ONE) {
            writeAnalogInputVal(rawPoint, point, hayStack,
                                regularUpdateMessage.getExternalAnalogVoltageInput1());
        } else if (Port.valueOf(rawPoint.getPort()) == Port.ANALOG_IN_TWO) {
            writeAnalogInputVal(rawPoint, point, hayStack,
                                regularUpdateMessage.getExternalAnalogVoltageInput2());
        } else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_RH) {
            writeHumidityVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_ILLUMINANCE) {
            writeIlluminanceVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_OCCUPANCY) {
            writeOccupancyVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
    }

    private static void writeHeartbeat(RawPoint rawPoint, Point point,
                                       HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack){
        hayStack.writeHisValueByIdWithoutCOV(rawPoint.getId(), (double)regularUpdateMessage.getRssi());
        hayStack.writeHisValueByIdWithoutCOV(point.getId(), (double)regularUpdateMessage.getRssi());
    }

    private static void writeRoomTemp(RawPoint rawPoint, Point point,
                               HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        
        hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getRoomTemperature());
        
        double receivedRoomTemp = Pulse.getRoomTempConversion((double) regularUpdateMessage.getRoomTemperature());
        double curRoomTemp = hayStack.readHisValById(point.getId());
        if (curRoomTemp != receivedRoomTemp) {
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getRoomTemperature()));
            if (currentTempInterface != null) {
                currentTempInterface.updateTemperature(curRoomTemp, Short.parseShort(point.getGroup()));
            }
        }
    }

    /**
     *  For door window sensor and keycard sensor no need to calculate the voltage conversion
     *  so position of the door window and keycard sensor are 12 and 13 so only for those two sensors
     *  we are applying the required logic according to the sensor and saving into logical point
     Keycard:

     Thermisotrs:
     >= 10K : No occupant(0)
     < 10K : Occupied(1)
     Analog:
     >= 2V : Not Occupied(0)
     < 2V : occupant(1)

     Door:

     Thermisor:
     >= 10K : Door Open(1)
     < 10K : Door Close(0)
     Analog:
     >= 2V : Door Open(1)
     < 2V : Door Close(0)

     Find the sensor
     KEY_CARD_SENSOR -> "12"
     DOOR_WINDOW_SENSOR -> "13"

     */
    private static void writeThermistorVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, double val) {

        hayStack.writeHisValById(rawPoint.getId(), val);
        int index = (int)Double.parseDouble(rawPoint.getType());

        if(index == SensorType.DOOR_WINDOW_SENSOR.ordinal()){ // it is DOOR_WINDOW_SENSOR
            hayStack.writeHisValById(point.getId(),((val*10) >= 10000)? 1.0 : 0.0);
            Log.d(L.TAG_CCU_DEVICE, "DOOR_WINDOW_SENSOR Thermistor input : "+(((val*10) >= 10000)? 1.0 : 0.0));
            return;
        }

        double thInputVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
        hayStack.writeHisValById(point.getId(), CCUUtils.roundToOneDecimal(thInputVal));
        Log.d(L.TAG_CCU_DEVICE, "Sensor input : Thermistor type " + rawPoint.getType() + " val " + thInputVal);

    }
    
    private static void writeAnalogInputVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, double val) {
        Log.i(L.TAG_CCU_DEVICE, "writeAnalogInputVal: "+rawPoint.getPort()+" " +rawPoint.getType());
        hayStack.writeHisValById(rawPoint.getId(), val);
        double voltageReceived = val/1000;
        int index = (int)Double.parseDouble(rawPoint.getType());

        if(index == SensorType.KEY_CARD_SENSOR.ordinal()){ //12 it is KEY_CARD_SENSOR
            Log.d(L.TAG_CCU_DEVICE, "KEY_CARD_SENSOR Sensor Analog input :"+voltageReceived);
            hayStack.writeHisValById(point.getId(), ((voltageReceived) >= 2)? 0.0 : 1.0);
            return;
        }
        if(index == SensorType.DOOR_WINDOW_SENSOR.ordinal()){ //13 it is DOOR_WINDOW_SENSOR
            Log.d(L.TAG_CCU_DEVICE, "DOOR_WINDOW_SENSOR Sensor Analog input :"+voltageReceived);
            hayStack.writeHisValById(point.getId(), ((voltageReceived) >= 2)? 1.0 : 0.0);
            return;
        }
        // Analog in is connected some other sensor
        hayStack.writeHisValById(point.getId(), AnalogUtil.getAnalogConversion(rawPoint, val));
    }
    
    private static void writeHumidityVal(RawPoint rawPoint, Point point,
                                  HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getHumidity());
        hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getHumidity()));
    }

    private static void writeOccupancyVal(RawPoint rawPoint, Point point, HyperStatRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        Log.i(L.TAG_CCU_DEVICE, "OccupantDetected : "+regularUpdateMessage.getOccupantDetected());
        hayStack.writeHisValById(rawPoint.getId(), regularUpdateMessage.getOccupantDetected()?1.0:0);
        hayStack.writeHisValById(point.getId(), regularUpdateMessage.getOccupantDetected()?1.0:0);
    }
    private static void writeIlluminanceVal(RawPoint rawPoint, Point point, HyperStatRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getIlluminance());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getIlluminance());
    }

    private static void writeSensorInputsToHaystackDatabase(List<HyperStat.SensorReadingPb_t> sensorReadings, int addr) {
        
        HyperStatDevice node = new HyperStatDevice(addr);
        int emVal = 0;
       
        for (HyperStat.SensorReadingPb_t sensorReading : sensorReadings) {
            SensorType sensorType = SensorType.values()[sensorReading.getSensorType()];
            Port port = sensorType.getSensorPort();
            if (port == null) {
                continue;
            }
            //Some of the sensors are optional.So these points are created only when we receive
            //the first update for a specific sensor type.
            double val = sensorReading.getSensorData();
            RawPoint sp = node.getRawPoint(port);
            if (sp == null) {
                sp = node.createSensorPoints(port);
                CcuLog.d(L.TAG_CCU_DEVICE, " Sensor Added , type "+sensorType+" port "+port);
            }
            CcuLog.d(L.TAG_CCU_DEVICE,"HyperStatSensor update: "+sensorType+" : "+val);
            switch (sensorType) {
                case HUMIDITY:
                    CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
                    CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), CCUUtils.roundToOneDecimal(val/10.0));
                    break;
                case CO2:
                case CO:
                case NO:
                case VOC:
                case PRESSURE:
                case SOUND:
                case CO2_EQUIVALENT:
                case ILLUMINANCE:
                case UVI:
                case PM2P5:
                case PM10:
                case OCCUPANCY:
                    CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
                    CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),val);
                    break;
                case ENERGY_METER_HIGH:
                    emVal = emVal > 0 ?  (emVal | (sensorReading.getSensorData() << 12)) : sensorReading.getSensorData();
                    break;
                case ENERGY_METER_LOW:
                    emVal = emVal > 0 ? ((emVal << 12) | sensorReading.getSensorData()) : sensorReading.getSensorData();
                    break;
            }
        }
        
        if (emVal > 0) {
            RawPoint sp = node.getRawPoint(Port.SENSOR_ENERGY_METER);
            if (sp == null) {
                sp = node.createSensorPoints(Port.SENSOR_ENERGY_METER);
            }
            CCUHsApi.getInstance().writeHisValById(sp.getId(), (double)emVal );
            CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),(double)emVal);
        }
        
    }
    
    private static void writeDesiredTemp(HyperStatLocalControlsOverrideMessage_t message, Equip hsEquip,
                                         CCUHsApi hayStack) {
        HashMap coolingDtPoint = hayStack.read("point and temp and desired and cooling and sp and equipRef == \""
                                               +hsEquip.getId()+ "\"");
        double coolingDesiredTemp = (double)message.getSetTempCooling()/2;
        double heatingDesiredTemp = (double)message.getSetTempHeating()/2;
        double averageDesiredTemp = (coolingDesiredTemp + heatingDesiredTemp)/2;
        
        if (!coolingDtPoint.isEmpty()) {
            CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);
        } else {
            CcuLog.e(L.TAG_CCU_DEVICE, "coolingDtPoint does not exist: "+hsEquip.getDisplayName());
        }
    
    
        HashMap heatingDtPoint = hayStack.read("point and temp and desired and heating and sp and equipRef == \""
                                               +hsEquip.getId()+ "\"");
        if (!heatingDtPoint.isEmpty()) {
            CCUHsApi.getInstance().writeHisValById(heatingDtPoint.get("id").toString(), heatingDesiredTemp);
        } else {
            CcuLog.e(L.TAG_CCU_DEVICE, "heatingDtPoint does not exist: "+hsEquip.getDisplayName());
        }
    
        HashMap dtPoint = hayStack.read("point and temp and desired and average and sp and equipRef == \""
                                               +hsEquip.getId()+ "\"");
        if (!dtPoint.isEmpty()) {
            CCUHsApi.getInstance().writeHisValById(dtPoint.get("id").toString(), (double)message.getSetTempCooling());
        } else {
            CcuLog.e(L.TAG_CCU_DEVICE, "dtPoint does not exist: "+hsEquip.getDisplayName());
        }
    
        SystemScheduleUtil.handleManualDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint).build(),
                                                         new Point.Builder().setHashMap(heatingDtPoint).build(),
                                                         new Point.Builder().setHashMap(dtPoint).build(),
                                                         coolingDesiredTemp, heatingDesiredTemp, averageDesiredTemp);
    
    }

    public static void setCurrentTempInterface(ZoneDataInterface in) {
        currentTempInterface = in; }


    public static void updateConditioningMode(String equipId, double mode, int nodeAddress){
        PossibleConditioningMode possibleMode =
                HSHaystackUtil.Companion.getPossibleConditioningModeSettings(nodeAddress);
        int conditioningMode;
        if (mode == HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_AUTO.ordinal()){
            if (possibleMode != PossibleConditioningMode.BOTH) {
                Log.i(L.TAG_CCU_DEVICE, mode+" Invalid conditioning mode "); return;
            }
            conditioningMode = StandaloneConditioningMode.AUTO.ordinal();
        } else if (mode == HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_HEATING.ordinal()){
            if (possibleMode == PossibleConditioningMode.COOLONLY) {
                Log.i(L.TAG_CCU_DEVICE, mode+"Invalid conditioning mode"); return;
            }
            conditioningMode = StandaloneConditioningMode.HEAT_ONLY.ordinal();
        } else if (mode == HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_COOLING.ordinal()){
            if (possibleMode == PossibleConditioningMode.HEATONLY) {
                Log.i(L.TAG_CCU_DEVICE, mode+"Invalid conditioning mode"); return;
            }
            conditioningMode = StandaloneConditioningMode.COOL_ONLY.ordinal();
        } else {
            if (mode != PossibleConditioningMode.OFF.ordinal()) {
                Log.i(L.TAG_CCU_DEVICE, mode+" Invalid conditioning mode "); return;
            }
            conditioningMode = StandaloneConditioningMode.OFF.ordinal();
        }
        HyperStatScheduler.Companion.updateHyperstatUIPoints(equipId,
                "temp and conditioning and mode and cpu", conditioningMode);
    }

    public static void updateFanMode(String equipId, int mode, int nodeAddress){
        PossibleFanMode possibleMode =
                HSHaystackUtil.Companion.getPossibleFanModeSettings(nodeAddress);
        int fanMode = getLogicalFanMode(possibleMode,mode);
        if(fanMode!= -1) {
            HyperStatScheduler.Companion.updateHyperstatUIPoints(
                    equipId, "fan and operation and mode and cpu", fanMode);
        }

    }

    private static void sendAcknowledge(int address){
        Log.i(L.TAG_CCU_DEVICE, "Sending Acknowledgement");
        if (!LSerial.getInstance().isConnected()) {
            CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
            return;
        }
        CcuToCmOverUsbDeviceTempAckMessage_t msg = new CcuToCmOverUsbDeviceTempAckMessage_t();
        msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SET_TEMPERATURE_ACK);
        msg.smartNodeAddress.set(address);
        MeshUtil.sendStructToCM(msg);
    }


    private static int getLogicalFanMode(PossibleFanMode mode,int selectedMode){
        Log.i(L.TAG_CCU_DEVICE, "PossibleFanMode: "+mode + " selectedMode  "+selectedMode);
        if(selectedMode == 0 ) return StandaloneFanStage.OFF.ordinal();
        switch (mode){
            case OFF:
                if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_OFF.ordinal() == selectedMode ) {
                    return StandaloneFanStage.OFF.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case LOW:
                if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                }else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case MED:
                if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                }else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case HIGH:
                if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case LOW_MED:
                if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case LOW_HIGH:
                if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case MED_HIGH:
                if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case LOW_MED_HIGH:
                if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(selectedMode == HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_MED.ordinal()){
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else if(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
        }
        return -1;
    }

}
