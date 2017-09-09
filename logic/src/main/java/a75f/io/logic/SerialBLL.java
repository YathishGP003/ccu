package a75f.io.logic;

import android.util.Log;

import org.javolution.io.Struct;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SnToCmOverAirSnRegularUpdateMessage_t;
import a75f.io.bo.serial.comm.SerialAction;
import a75f.io.bo.serial.comm.SerialEvent;
import a75f.io.usbserial.UsbService;

import static a75f.io.logic.LogBLL.logStructAsJSON;
import static android.content.ContentValues.TAG;

/**
 * Created by Yinten isOn 8/21/2017.
 */

public class SerialBLL
{
	private static SerialBLL  mSerialBLL;
	private        UsbService mUsbService;
	
	
	/***
	 * Default empty constructor for a singleton.
	 */
	private SerialBLL()
	{
	}
	
	
	public static SerialBLL getInstance()
	{
		if (mSerialBLL == null)
		{
			mSerialBLL = new SerialBLL();
		}
		
		return mSerialBLL;
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
		Log.i(TAG, "Event Type: " + event.getSerialAction().name());
		if (event.getSerialAction() == SerialAction.MESSAGE_FROM_SERIAL_PORT)
		{
			byte[] data = (byte[]) event.getBytes();
			Log.i(TAG, "Data return size: " + data.length);
			MessageType messageType = MessageType.values()[(event.getBytes()[0] & 0xff)];
			String pojoAsString = null;
			Log.i(TAG, "Message Type: " + messageType.name());
			if (messageType == MessageType.CM_REGULAR_UPDATE)
			{
				CmToCcuOverUsbCmRegularUpdateMessage_t regularUpdateMessage_t =
						new CmToCcuOverUsbCmRegularUpdateMessage_t();
				Log.i(TAG, "CmToCcuOverUsbCmRegularUpdateMessage_t Message Expected Size: " +
				           regularUpdateMessage_t.size());
				regularUpdateMessage_t
						.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
				Log.i(TAG, "Regular Update Message: " + regularUpdateMessage_t.toString());
				try
				{
					pojoAsString = JsonSerializer.toJson(regularUpdateMessage_t, true);
					System.out.println("POJO as string:\n" + pojoAsString + "\n");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
			else if (messageType == MessageType.CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE)
			{
				CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t =
						new CmToCcuOverUsbSnRegularUpdateMessage_t();
				Log.i(TAG, "CmToCcuOverUsbSnRegularUpdateMessage_t Message Expected Size: " +
				           smartNodeRegularUpdateMessage_t.size());
				Log.i(TAG, "CmToCcuOverUsbSnRegularUpdateMessage_t size: " +
				           smartNodeRegularUpdateMessage_t.size());
				Log.i(TAG, "Buffer size with smart node regular update message: " + data.length);
				Log.i(TAG, "Size of inner struct SnToCmOverAirSnRegularUpdateMessage_t: " +
				           new SnToCmOverAirSnRegularUpdateMessage_t().size());
				smartNodeRegularUpdateMessage_t
						.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
				Log.i(TAG, "Smart Node Regular Update Message: " +
				           smartNodeRegularUpdateMessage_t.toString());
				try
				{
					pojoAsString = JsonSerializer.toJson(smartNodeRegularUpdateMessage_t, true);
					System.out.println("POJO as string:\n" + pojoAsString + "\n");
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
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
	 * only place the usbService should be interacted with is through the SerialBLL.
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
	 * either the CM or the SmartNode.
	 *
	 */
	public boolean sendSerialStruct(Struct struct)
	{
		logStructAsJSON(struct);
		if (mUsbService == null)
		{
			LogBLL.logUSBServiceNotInitialized();
			return false;
		}
		mUsbService.write(struct.getOrderedBuffer());
		return true;
	}
}
