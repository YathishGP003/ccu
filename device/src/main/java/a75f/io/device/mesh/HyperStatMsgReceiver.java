package a75f.io.device.mesh;

import com.google.protobuf.InvalidProtocolBufferException;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.device.HyperStat;
import a75f.io.device.HyperStat.HyperStatCmToCcuSerializedMessage_t;
import a75f.io.device.HyperStat.HyperStatRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.sensors.SensorType;
import a75f.io.logic.bo.haystack.device.HyperStatDevice;
import a75f.io.logic.bo.util.CCUUtils;

public class HyperStatMsgReceiver {
    
    public static void processMessage(byte[] data, MessageType messageType, CCUHsApi hayStack) {
        try {
            HyperStatCmToCcuSerializedMessage_t message = HyperStatCmToCcuSerializedMessage_t.parseFrom(data);
    
            if (messageType == MessageType.HYPERSTAT_REGULAR_UPDATE_MESSAGE) {
                HyperStatRegularUpdateMessage_t regularUpdate =
                    HyperStat.HyperStatRegularUpdateMessage_t.parseFrom(message.getSerializedMessageData());
    
                handleRegularUpdate(regularUpdate, message.getAddress(), hayStack);
            }
            
        } catch (InvalidProtocolBufferException e) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Cant parse protobuf data: "+e.getMessage());
        }
    }
    
    private static void handleRegularUpdate(HyperStatRegularUpdateMessage_t regularUpdateMessage, int nodeAddress,
                                     CCUHsApi hayStack) {
        CcuLog.e(L.TAG_CCU_DEVICE, "handleRegularUpdate: "+regularUpdateMessage.toString());
        HashMap device = hayStack.read("device and addr == \"" + nodeAddress + "\"");
        DeviceUtil.getRawPointsWithRefForDevice(device, hayStack)
                    .forEach( point -> writePortInputsToHaystackDatabase( point, regularUpdateMessage, hayStack));
        
        if (regularUpdateMessage.getSensorReadingsList().size() > 0) {
            writeSensorInputsToHaystackDatabase(regularUpdateMessage.getSensorReadingsList(), nodeAddress);
        }
    }
    
    private static void writePortInputsToHaystackDatabase(RawPoint rawPoint,
                                                     HyperStatRegularUpdateMessage_t regularUpdateMessage,
                                                     CCUHsApi hayStack) {
        
        Point point = DeviceUtil.getLogicalPointForRawPoint(rawPoint, hayStack);
        if (point == null) {
            return;
        }
        if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_RT) {
            writeRoomTemp(rawPoint, point, regularUpdateMessage, hayStack);
        } else if (Port.valueOf(rawPoint.getPort()) == Port.TH1_IN ||
                                        Port.valueOf(rawPoint.getPort()) == Port.TH2_IN) {
            writeThermistorVal(rawPoint, point, regularUpdateMessage, hayStack);
        } else if (Port.valueOf(rawPoint.getPort()) == Port.ANALOG_IN_ONE) {
            writeAnalogInputVal(rawPoint, point, regularUpdateMessage,
                                hayStack, regularUpdateMessage.getExternalAnalogVoltageInput1());
        } else if (Port.valueOf(rawPoint.getPort()) == Port.ANALOG_IN_TWO) {
            writeAnalogInputVal(rawPoint, point, regularUpdateMessage,
                                hayStack, regularUpdateMessage.getExternalAnalogVoltageInput2());
        } else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_RH) {
            writeHumidityVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
    
    }
    
    private static void writeRoomTemp(RawPoint rawPoint, Point point,
                               HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        
        hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getRoomTemperature());
        hayStack.writeHisValById(point.getId(),
                                 Pulse.getRoomTempConversion((double) regularUpdateMessage.getRoomTemperature()));
    }
    
    private static void writeThermistorVal(RawPoint rawPoint, Point point,
                                    HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        double thInputVal = ThermistorUtil.getThermistorValueToTemp(
                                regularUpdateMessage.getExternalThermistorInput1() * 10);
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getExternalThermistorInput1());
        hayStack.writeHisValById(point.getId(), CCUUtils.roundToOneDecimal(thInputVal));
    }
    
    private static void writeAnalogInputVal(RawPoint rawPoint, Point point,
                                     HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack,
                                     double val) {
        double rawAnalogInput = regularUpdateMessage.getExternalAnalogVoltageInput1();
        hayStack.writeHisValById(rawPoint.getId(), rawAnalogInput);
        hayStack.writeHisValById(point.getId(), AnalogUtil.getAnalogConversion(rawPoint, rawAnalogInput));
    }
    
    private static void writeHumidityVal(RawPoint rawPoint, Point point,
                                  HyperStatRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getHumidity());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getHumidity());
    }
    
    private static void writeSensorInputsToHaystackDatabase(List<HyperStat.SensorReadingPb_t> sensorReadings, int addr) {
        
        HyperStatDevice node = new HyperStatDevice(addr);
        int emVal = 0;
       
        for (HyperStat.SensorReadingPb_t r : sensorReadings) {
            SensorType t = SensorType.values()[r.getSensorType()];
            Port p = t.getSensorPort();
            if (p == null) {
                continue;
            }
            double val = r.getSensorData();
            RawPoint sp = node.getRawPoint(p);
            if (sp == null) {
                sp = node.createSensorPoints(p);
                CcuLog.d(L.TAG_CCU_DEVICE, " Sensor Added , type "+t+" port "+p);
            }
            CcuLog.d(L.TAG_CCU_DEVICE,"HyperStatSensor update: "+t+" : "+val);
            switch (t) {
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
                    CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
                    CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),val);
                    break;
                case OCCUPANCY:
                    //TODO
                    break;
                case ENERGY_METER_HIGH:
                    emVal = emVal > 0 ?  (emVal | (r.getSensorData() << 12)) : r.getSensorData();
                    break;
                case ENERGY_METER_LOW:
                    emVal = emVal > 0 ? ((emVal << 12) | r.getSensorData()) : r.getSensorData();
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
}
