package a75f.io.device.mesh;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

import static a75f.io.device.DeviceConstants.TAG;

/**
 * Created by Yinten on 9/15/2017.
 */

public class Pulse
{
	public static void regularSNUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t)
	{
		Log.d(TAG,"regularSmartNodeUpdate");
		short nodeAddr = (short)smartNodeRegularUpdateMessage_t.update.smartNodeAddress.get();
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{
			HashMap currentTemp = hayStack.read("point and physical and current and deviceRef == \""+device.get("id")+"\"");
			HashMap logicalTemp = hayStack.read("point and id=="+currentTemp.get("pointRef"));
			
			double temp = smartNodeRegularUpdateMessage_t.update.roomTemperature.get() / 10.0;
			hayStack.writeHisValById(currentTemp.get("id").toString(), temp);
			System.out.println("roomTemp Updated Val "+temp );
			hayStack.writeHisValById(logicalTemp.get("id").toString(), temp);
		}
	}
	
	public static void regularCMUpdate(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t)
	{
		DLog.Logd("Regualar cm update pulse" + DLog.objectNullString((cmRegularUpdateMessage_t)));
	}
}
