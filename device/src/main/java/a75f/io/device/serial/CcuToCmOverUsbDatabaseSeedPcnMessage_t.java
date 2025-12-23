package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

import a75f.io.device.pcn;

/**
 * Created by pramod isOn 22/9/2025.
 */

public class CcuToCmOverUsbDatabaseSeedPcnMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	public final Unsigned16 smartNodeAddress = new Unsigned16();
	public final Unsigned8[] encryptionKey = array(new Unsigned8[MessageConstants.MESH_ENCRYPTION_KEY_LENGTH]);
	public final SmartNodeSettings_t settings = inner(new SmartNodeSettings_t());
	public final SmartNodeSettings2_t settings2 = inner(new SmartNodeSettings2_t());

	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
	
	public void putEncrptionKey(byte[] encryptionKeyBytes) throws Exception
	{
		for (int i = 0; i < encryptionKeyBytes.length; i++)
		{
			encryptionKey[i].set(encryptionKeyBytes[i]);
		}
	}
}
