package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

/*
typedef struct {
  ProtocolError_t errorType; ///< The code that identifies the type of error
  uint16_t errorDetail; ///< For errors involving a device not being in the database, this will contain the device address
} CmToCcuOverUsbErrorReportMessage_t;


 */

public class CmToCcuOverUsbErrorReportMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Enum8<ErrorType> errorType = new Enum8<>(ErrorType.values());
	
	public final Unsigned16 errorDetail = new Unsigned16(); // this will be the WRM address if its not in Dbase
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
