package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class CmToCcuOverUsbCmRegularUpdateMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType     = new Enum8<>(MessageType.values());
	public final Unsigned16         hvacSupply      = new Unsigned16(); // hvac voltage in 100 mv
	public final Unsigned16         roomTemperature = new Unsigned16(); // room temp in 1/10 F
	
	public final Unsigned16 airHandlerTemperature = new Unsigned16(); // air temp in 1/10 F
	
	public final Unsigned8 humidity = new Unsigned8(); // percent
	
	public final Unsigned16 analogSense1 = new Unsigned16(); // external voltage sense in mv units
	
	public final Unsigned16 analogSense2 = new Unsigned16(); // external voltage sense in mv units
	
	public final Unsigned16 analogSense3 = new Unsigned16(); // external voltage sense in mv units
	
	public final Unsigned16 analogSense4 = new Unsigned16(); // external voltage sense in mv units
	
	public final Unsigned8 relayBitmap = new Unsigned8(); // digital out for HVAC activation
	
	public final Unsigned8 digitalIoBitmap = new Unsigned8(); // readout of the GPIO pins
	
	public final Unsigned8 analog0 = new Unsigned8(); // analog inputs for variable freq drive
	
	public final Unsigned8 analog1 = new Unsigned8(); // using just 0 and 255 here will make it like a digital output
	
	public final Unsigned8 analog2 = new Unsigned8();
	
	public final Unsigned8 analog3 = new Unsigned8();
	
	public final Unsigned8 analog4 = new Unsigned8();
	
	public final Unsigned8 analog5 = new Unsigned8();
	
	public final Unsigned8 analog6 = new Unsigned8();
	
	public final Unsigned8 analog7 = new Unsigned8();
	
	public final Unsigned16 energyPulseCount = new Unsigned16(); // energy meter pulse count
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
