package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by pramod isOn 22/09/2025.
 */

public class SnToCmOverAirSnDiagonasticUpdateMessage_t extends Struct
{
	public final Unsigned8 slaveId = new Unsigned8(); /* Slave Id */
	
	public final Unsigned16 seqLastRunTime = new Unsigned16(); /* only for router we have next hop */
	public final Unsigned16 seqMaxRunTime = new Unsigned16();
	public final Unsigned16 seqStatus = new Unsigned16();
	public final Unsigned16 seqErrorCode = new Unsigned16();


	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
