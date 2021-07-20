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
import a75f.io.device.mesh.AnalogUtil;
import a75f.io.device.mesh.DLog;
import a75f.io.device.mesh.DeviceHSUtil;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.mesh.ThermistorUtil;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.sensors.SensorType;
import a75f.io.logic.bo.haystack.device.HyperStatDevice;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.pubnub.ZoneDataInterface;

import static a75f.io.device.mesh.Pulse.getHumidityConversion;

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
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_DEVICE, "handleOverrideMessage: "+message.toByteString());
        }
    
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+nodeAddress+"\"");
        Equip hsEquip = new Equip.Builder().setHashMap(equipMap).build();
    
        writeDesiredTemp(message, hsEquip, hayStack);
        //TODO
        //Update fanMode and conditioning mode once profile is ready.
        //Send Ack
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
        if(currentTempInterface != null) {
            currentTempInterface.refreshScreen(null);
        }
    }

    private static void writeRoomTemp(RawPoint rawPoint, Point point,
                               HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        
        hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getRoomTemperature());
        hayStack.writeHisValById(point.getId(),
                                 Pulse.getRoomTempConversion((double) regularUpdateMessage.getRoomTemperature()));
    }
    
    private static void writeThermistorVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, double val) {
        double thInputVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
        hayStack.writeHisValById(rawPoint.getId(), val);
        hayStack.writeHisValById(point.getId(), CCUUtils.roundToOneDecimal(thInputVal));
    }
    
    private static void writeAnalogInputVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, double val) {
        hayStack.writeHisValById(rawPoint.getId(), val);
        hayStack.writeHisValById(point.getId(), AnalogUtil.getAnalogConversion(rawPoint, val));
    }
    
    private static void writeHumidityVal(RawPoint rawPoint, Point point,
                                  HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getHumidity());
        hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getHumidity()));
    }

    private static void writeOccupancyVal(RawPoint rawPoint, Point point, HyperStatRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
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
        double coolingDesiredTemp = (double)message.getSetTempCooling();
        double heatingDesiredTemp = (double)message.getSetTempHeating();
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
        
        ScheduleProcessJob.handleManualDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint).build(),
                                                         new Point.Builder().setHashMap(heatingDtPoint).build(),
                                                         new Point.Builder().setHashMap(dtPoint).build(),
                                                         coolingDesiredTemp, heatingDesiredTemp, averageDesiredTemp);
    
    }

    public static void setCurrentTempInterface(ZoneDataInterface in) {
        currentTempInterface = in; }
}
