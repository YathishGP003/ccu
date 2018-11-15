package a75f.io.device.mesh;

import android.util.Log;

import java.util.ArrayList;
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
			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and input and deviceRef == \"" + device.get("id") + "\"");
			
			for(HashMap phyPoint : phyPoints) {
				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				double val;
				//Log.d(TAG,"phyPoint : "+phyPoint);
				//Log.d(TAG,"logPoint : "+logPoint);
				switch (phyPoint.get("port").toString()){
					case "RTH":
						val = smartNodeRegularUpdateMessage_t.update.roomTemperature.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getRoomTempConversion(val));
						Log.d(TAG,"regularSmartNodeUpdate : roomTemp "+getRoomTempConversion(val));
						break;
					case "ANALOG_IN_ONE":
						val = smartNodeRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(val));
						Log.d(TAG,"regularSmartNodeUpdate : analog1In "+getAnalogConversion(val));
						break;
					case "ANALOG_IN_TWO":
						val = smartNodeRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(val));
						Log.d(TAG,"regularSmartNodeUpdate : analog2In "+getAnalogConversion(val));
						break;
					case "TH1_IN":
						val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getThermistorConversion(val));
						Log.d(TAG,"regularSmartNodeUpdate : Thermistor1 "+val);
						break;
					case "TH2_IN":
						val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput2.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getThermistorConversion(val));
						Log.d(TAG,"regularSmartNodeUpdate : Thermistor2 "+val);
						break;
				}
			}
			
		}
	}
	
	public static Double getRoomTempConversion(Double temp) {
		return temp/10.0;
	}
	public static Double getThermistorConversion(Double val) {
		return val/100.0;
	}
	
	public static Double getAnalogConversion(Double val) {
		return val/10.0;
	}
	
	public static void regularCMUpdate(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t)
	{
		DLog.Logd("Regualar cm update pulse" + DLog.objectNullString((cmRegularUpdateMessage_t)));
	}
}
