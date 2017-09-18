package a75f.io.logic;

import org.javolution.io.Struct;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import a75f.io.bo.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.comm.SerialAction;
import a75f.io.bo.serial.comm.SerialEvent;
import a75f.io.usbserial.UsbService;

import static a75f.io.logic.LLog.Logd;
import static a75f.io.logic.LLog.LogdSerial;
import static a75f.io.logic.LLog.LogdStructAsJson;

/**
 * Created by Yinten isOn 8/21/2017.
 */

class LSerial
{
	private static LSerial    mLSerial;
	private        UsbService mUsbService;
	
	
	/***
	 * Default empty constructor for a singleton.
	 */
	private LSerial()
	{
	}
	
	
	public static LSerial getInstance()
	{
		if (mLSerial == null)
		{
			mLSerial = new LSerial();
		}
		
		return mLSerial;
	}
	
	/***
	 * This method will construct the Struct based on a class type.   It will log message type, data return size, incoming hexadecimal, and json.
	 * This is a lot of parsing, so it should only be used for
	 * @param data
	 * @param pojoClass
	 * @param <T>
	 * @return
	 */
	public static <T extends Struct> T fromBytes(byte[] data, Class<T> pojoClass)
	{
		T struct = pojoClass.cast(new Struct());
		struct.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
		Logd("Message Type: " + pojoClass.getSimpleName());
		Logd("Data return size: " + data.length);
		//Log hexadecimal
		Logd("Incoming Hexadecimal: " + struct.toString());
		LogdStructAsJson(struct);
		return struct;
	}

	
	/***
	 * This method will handle all incoming messages from the CM.   It will parse them and
	 * determine where they should be sent.
	 *
	 * Logs to logcat.
	 *
	 * @param event The serial event from the CM
	 */
	
	public static void handleSerialEvent(SerialEvent event)
	{
		
		LogdSerial("Event Type: " + event.getSerialAction().name());
		
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = event.getBytes();
			MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
		
			if (messageType == MessageType.CM_REGULAR_UPDATE)
			{
					Pulse.regularCMUpdate(fromBytes(data, CmToCcuOverUsbCmRegularUpdateMessage_t.class));
			}
			else if (messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE)
			{
					Pulse.regularSNUpdate(fromBytes(data, CmToCcuOverUsbSnRegularUpdateMessage_t.class));
			}
		}
	}
	
	
	public boolean isConnected()
	{
		if(mUsbService == null)
		{
			return false;
		}
		return mUsbService.isConnected();
	}
	
	
	/***
	 * This is the setter method for the USB Service.
	 *
	 * All the members of BaseSerialAppCompatActivity are private and shouldn't be used.   The
	 * only place the usbService should be interacted with is through the LSerial.
	 *
	 * This will be help when we move onto the state machines.
	 *
	 * @param usbService
	 */
	public void setUSBService(UsbService usbService)
	{
		mUsbService = usbService;
	}
	
	
	/***
	 * This method will handle all  messages being sent to the CM.
	 *
	 * Logs to logcat.
	 *
	 * @param struct This is a representation of a C Struct, loggable to hexadecimal and JSON.
	 *                  It is a convience to deal with ByteOrder and following interface
	 *                  documentation isOn Sharepoint.
	 * @return success If serial was open and the usbService was successfully able to try to send
	 * to CM without Android stopping it.  It doesn't nessacarily mean any messages went to
	 * either the CM or the Node.
	 *
	 */
	public boolean sendSerialStruct(Struct struct)
	{
		LogdStructAsJson(struct);
		if (mUsbService == null)
		{
			LLog.logUSBServiceNotInitialized();
			return false;
		}
		mUsbService.write(struct.getOrderedBuffer());
		return true;
	}
}
