package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class CcuToCmOverUsbPcnSequenceMetaMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	public final Unsigned16 lwMeshAddress = new Unsigned16();
	public final PcnSequenceMeta_t metadata = inner(new PcnSequenceMeta_t());
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
