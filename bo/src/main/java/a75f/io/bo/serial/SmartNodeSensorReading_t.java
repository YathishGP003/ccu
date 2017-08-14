package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan on 8/2/17.
 */

public class SmartNodeSensorReading_t extends Struct
{
	
	public final Unsigned16 sensorType = new Unsigned16(4);
	
	public final Unsigned16 sensorData = new Unsigned16(12);
}
