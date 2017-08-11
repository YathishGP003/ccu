package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan on 8/2/17.
 */

public class SmartNodeSensorReading_t extends Struct
{
	
	public final Unsigned8 sensorType = new Unsigned8(4);
	
	public final Unsigned8 sensorData = new Unsigned8(12);
}
