package a75f.io.device;

import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten on 9/15/2017.
 */

class Pulse
{
	public static void regularSNUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t)
	{
		/*LLog.Logd("Regualar update pulse" + LLog.objectNullString(smartNodeRegularUpdateMessage_t));
		for(Floor floor : L.ccu().getFloors())
		{
			for(Zone zone : floor.mRoomList)
			{
				zone.mapRegularUpdate(smartNodeRegularUpdateMessage_t);
			}
		}*/
	}
	
	public static void regularCMUpdate(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t)
	{
		LLog.Logd("Regualar cm update pulse" + LLog.objectNullString((cmRegularUpdateMessage_t)));
	}
}
