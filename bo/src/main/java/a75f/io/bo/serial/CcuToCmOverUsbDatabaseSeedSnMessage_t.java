package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by ryanmattison on 7/25/17.
 */

public class CcuToCmOverUsbDatabaseSeedSnMessage_t extends Struct
{
	
	public String CCU = "DefaultCCU";
	
	
	public final Enum8<MessageType>  messageType      =  new Enum8<>(MessageType.values());
	
	public final Struct.Unsigned16   smartNodeAddress = new Unsigned16();
	
	
	public final Unsigned8[]         encryptionKey    = array(new Unsigned8[SerialConsts.APP_KEY_LENGTH]);
	
	public final SmartNodeSettings_t settings         = inner(new SmartNodeSettings_t());
	
	public final SmartNodeControls_t controls         = inner(new SmartNodeControls_t());
	
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
