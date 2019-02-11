package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class SensorReading_t extends Struct
{
	
	public final Enum4<SensorType_t> sensorType = new Enum4<>(SensorType_t.values());
	
	public final Unsigned16 sensorData = new Unsigned16(12);
}
