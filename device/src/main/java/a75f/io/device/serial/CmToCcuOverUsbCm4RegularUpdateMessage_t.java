package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

public class CmToCcuOverUsbCm4RegularUpdateMessage_t extends Struct {
    public final Enum8<MessageType> messageType     = new Enum8<>(MessageType.values());

    public SensorReading_t[] sensorReadings = array(new SensorReading_t[MessageConstants.NUM_SN_TYPE_VALUE_SENSOR_READINGS]);
    @Override
    public ByteOrder byteOrder()
    {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
