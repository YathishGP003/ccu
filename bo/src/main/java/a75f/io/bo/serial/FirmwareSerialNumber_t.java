package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class FirmwareSerialNumber_t extends Struct
{
	
	public final Unsigned8[] array = array(new Unsigned8[MessageConstants.MAX_SERIAL_NUMBER_LENGTH]);
}
