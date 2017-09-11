package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/9/17.
 */

public class CcuToCmOverUsbDatabaseSeedSmartStatMessage_t extends Struct
{
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	public final Unsigned16 address = new Unsigned16();
	
	public final Unsigned8[] encryptionKey = array(new Unsigned8[MessageConstants.MESH_ENCRYPTION_KEY_LENGTH]);
	
	public final SmartStatSettings_t settings = inner(new SmartStatSettings_t());
	
	public final SmartStatControls_t controls = inner(new SmartStatControls_t());
	
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
