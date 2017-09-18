package a75f.io.logic;

import a75f.io.bo.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

import static a75f.io.logic.LLog.Logd;
import static a75f.io.logic.LLog.objectNullString;

/**
 * Created by Yinten on 9/15/2017.
 */

class Pulse
{
	public static void regularSNUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t)
	{
		Logd("Regualar update pulse" + objectNullString(smartNodeRegularUpdateMessage_t));
		
		//LSmartNode.updateProfilesWithRegularUpdate(smartNodeRegularUpdateMessage_t.update
		//													.smartNodeAddress);
	}
	
	public static void regularCMUpdate(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t)
	{
		Logd("Regualar cm update pulse" + objectNullString((cmRegularUpdateMessage_t)));
	}
}
