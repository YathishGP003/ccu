package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan on 8/2/17.
 */

public class SmartNodeSensorReading_t extends Struct
{
	
	public final BitField sensorType = new BitField(4);
	
	public final BitField sensorData = new BitField(12);
}
