package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class CmToCcuOverUsbCm4RegularUpdateMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType     = new Enum8<>(MessageType.values());

	public final SensorReading_t[] sensorReadings = array(new SensorReading_t[MessageConstants.NUM_SS_TYPE_VALUE_SENSOR_READINGS]);
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
