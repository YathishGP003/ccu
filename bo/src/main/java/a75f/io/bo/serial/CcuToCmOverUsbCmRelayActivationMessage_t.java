package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class CcuToCmOverUsbCmRelayActivationMessage_t extends Struct
{
	
	// note one more byte is added at end. By default if CCU sends old message with shorter structure, all the ext i/0 will be output.
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned8 relayBitmap              = new Unsigned8(); // digital out for HVAC activation
	public final Unsigned8 analog0                  = new Unsigned8(); // analog outputs for variable freq drive
	public final Unsigned8 analog1                  = new Unsigned8();
	public final Unsigned8 analog2                  = new Unsigned8();
	public final Unsigned8 analog3                  = new Unsigned8();
	public final Unsigned8 pwmRedLed                = new Unsigned8(); //pwm for the led colors
	public final Unsigned8 pwmGreenLed              = new Unsigned8();
	public final Unsigned8 pwmBlueLed               = new Unsigned8();
	public final Unsigned8 externalOutput           = new Unsigned8(); // bitmap to use for activating external I/O
	public final Unsigned8 externalOutputActivation = new Unsigned8(); // bitmap to decide if external I/O is input or output. 1 is input. 0 is output
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
