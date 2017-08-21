package a75f.io.logic;

import android.util.Log;

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

import static android.content.ContentValues.TAG;

/**
 * Created by Yinten on 8/21/2017.
 */

public class SerialBLL
{
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
				CmToCcuOverUsbCmRegularUpdateMessage_t regularUpdateMessage_t = new CmToCcuOverUsbCmRegularUpdateMessage_t();
				regularUpdateMessage_t.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
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
				CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t = new CmToCcuOverUsbSnRegularUpdateMessage_t();
				Log.i(TAG, "CmToCcuOverUsbSnRegularUpdateMessage_t size: " + smartNodeRegularUpdateMessage_t.size());
				Log.i(TAG, "Buffer size with smart node regular update message: " + data.length);
				Log.i(TAG, "Size of inner struct SnToCmOverAirSnRegularUpdateMessage_t: " + new SnToCmOverAirSnRegularUpdateMessage_t().size());
				smartNodeRegularUpdateMessage_t.setByteBuffer(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN), 0);
				Log.i(TAG, "Smart Node Regular Update Message: " + smartNodeRegularUpdateMessage_t.toString());
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
}
