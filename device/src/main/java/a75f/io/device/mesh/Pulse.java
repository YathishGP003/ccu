package a75f.io.device.mesh;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.device.serial.CcuToCmOverUsbDeviceTempAckMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartNodeSensorReading_t;
import a75f.io.device.serial.SmartStatFanSpeed_t;
import a75f.io.device.serial.WrmOrCmRebootIndicationMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Sensor;
import a75f.io.logic.bo.building.SensorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

/**
 * Created by Yinten on 9/15/2017.
 */

public class Pulse
{
	public static void regularSNUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t)
	{
		short nodeAddr = (short)smartNodeRegularUpdateMessage_t.update.smartNodeAddress.get();
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{
			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + device.get("id") + "\"");
			
			for(HashMap phyPoint : phyPoints) {
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					continue;
				}
				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				double val = 0;
				switch (Port.valueOf(phyPoint.get("port").toString())){
					case SENSOR_RT:
						val = smartNodeRegularUpdateMessage_t.update.roomTemperature.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getRoomTempConversion(val));
						CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : roomTemp " + getRoomTempConversion(val));
						break;
					case DESIRED_TEMP:
						val = smartNodeRegularUpdateMessage_t.update.setTemperature.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						double desiredTemp = getDesredTempConversion(val);
						if (desiredTemp > 0)
						{
							hayStack.writeHisValById(logPoint.get("id").toString(), desiredTemp);
							updateDesiredTemp(nodeAddr, desiredTemp);
						}
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : desiredTemp "+desiredTemp);
						break;
					case ANALOG_IN_ONE:
						val = smartNodeRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : analog1In "+getAnalogConversion(phyPoint, logPoint, val));
						break;
					case ANALOG_IN_TWO:
						val = smartNodeRegularUpdateMessage_t.update.externalAnalogVoltageInput2.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : analog2In "+getAnalogConversion(phyPoint, logPoint, val));
						break;
					case TH1_IN:
						val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), ThermistorUtil.getThermistorValueToTemp(val * 10 ));
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : Thermistor1 "+ThermistorUtil.getThermistorValueToTemp(val * 10 ));
						break;
					case TH2_IN:
						val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput2.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), ThermistorUtil.getThermistorValueToTemp(val * 10));
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : Thermistor2 "+ThermistorUtil.getThermistorValueToTemp(val * 10));
						break;
					case SENSOR_RH:
						SmartNodeSensorReading_t[] sensorReadingsHumidity = smartNodeRegularUpdateMessage_t.update.sensorReadings;
						for (SmartNodeSensorReading_t r : sensorReadingsHumidity) {
							if (r.sensorType.get() == SensorType.HUMIDITY.ordinal()) {
								val = r.sensorData.get();
								break;
							}
						}
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getHumidityConversion(val));
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : Humidity "+getHumidityConversion(val));
						break;
					case SENSOR_CO2:
						SmartNodeSensorReading_t[] sensorReadingsCO2 = smartNodeRegularUpdateMessage_t.update.sensorReadings;
						for (SmartNodeSensorReading_t r : sensorReadingsCO2) {
							if (r.sensorType.get() == SensorType.CO2.ordinal()) {
								val = r.sensorData.get();
								break;
							}
						}
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), val);
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : CO2 "+val);
						break;
					case SENSOR_VOC:
						SmartNodeSensorReading_t[] sensorReadingsVOC = smartNodeRegularUpdateMessage_t.update.sensorReadings;
						for (SmartNodeSensorReading_t r : sensorReadingsVOC) {
							if (r.sensorType.get() == SensorType.VOC.ordinal()) {
								val = r.sensorData.get();
								break;
							}
						}
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), val);
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : VOC "+val);
						break;
					
				}
			}
			
		}
	}
	
	public static Double getRoomTempConversion(Double temp) {
		return temp/10.0;
	}
	public static Double getHumidityConversion(Double h) {
		return h/10.0;
	}
	public static Double getDesredTempConversion(Double val) {
		return val/2;
	}
	
	public static Double getAnalogConversion(HashMap pp, HashMap lp, Double val) {
		val = val/1000;
		Sensor analogSensor;
		try
		{
			int index = (int)Double.parseDouble(pp.get("type").toString());
			analogSensor = Sensor.getSensorList().get(index);
		}catch (NumberFormatException e) {
			e.printStackTrace();
			return val;
		}
		Log.d("regularSmartNode ","sensor type "+pp.get("type").toString()+" val "+val);
		return analogSensor.minEngineeringValue +
		                (analogSensor.maxEngineeringValue- analogSensor.minEngineeringValue) * val / (analogSensor.maxVoltage - analogSensor.minVoltage);
		
	}
	
	private static void updateDesiredTemp(int node, Double dt) {
		HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+node+"\"");
		Equip q = new Equip.Builder().setHashMap(equipMap).build();
		
		double cdb = TunerUtil.readTunerValByQuery("deadband and base and cooling and equipRef == \""+q.getId()+"\"");
		double hdb = TunerUtil.readTunerValByQuery("deadband and base and heating and equipRef == \""+q.getId()+"\"");
		
		double coolingDesiredTemp = dt + cdb;
		double heatingDesiredTemp = dt - hdb;
		
		
		HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+q.getId()+"\"");
		if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
		CCUHsApi.getInstance().pointWrite(HRef.copy(coolingDtPoint.get("id").toString()), HayStackConstants.DESIREDTEMP_OVERRIDE_LEVEL,"manual", HNum.make(coolingDesiredTemp), HNum.make(120*60*1000, "ms"));
		CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);
		
		HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+q.getId()+"\"");
		if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
		CCUHsApi.getInstance().pointWrite(HRef.copy(heatinDtPoint.get("id").toString()), HayStackConstants.DESIREDTEMP_OVERRIDE_LEVEL,"manual", HNum.make(heatingDesiredTemp), HNum.make(120*60*1000, "ms"));
		CCUHsApi.getInstance().writeHisValById(heatinDtPoint.get("id").toString(), heatingDesiredTemp);
	}

    private static void updateSmartStatDesiredTemp(int node, Double dt) {
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+node+"\"");
        Equip q = new Equip.Builder().setHashMap(equipMap).build();

        double cdb = StandaloneTunerUtil.readTunerValByQuery("deadband and base and cooling and equipRef == \""+q.getId()+"\"");
        double hdb = StandaloneTunerUtil.readTunerValByQuery("deadband and base and heating and equipRef == \""+q.getId()+"\"");

        double coolingDesiredTemp = dt + cdb;
        double heatingDesiredTemp = dt - hdb;


        HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+q.getId()+"\"");
        if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().pointWrite(HRef.copy(coolingDtPoint.get("id").toString()), HayStackConstants.DESIREDTEMP_OVERRIDE_LEVEL,"manual", HNum.make(coolingDesiredTemp), HNum.make(120*60*1000, "ms"));
        CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);

        HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+q.getId()+"\"");
        if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().pointWrite(HRef.copy(heatinDtPoint.get("id").toString()), HayStackConstants.DESIREDTEMP_OVERRIDE_LEVEL,"manual", HNum.make(heatingDesiredTemp), HNum.make(120*60*1000, "ms"));
        CCUHsApi.getInstance().writeHisValById(heatinDtPoint.get("id").toString(), heatingDesiredTemp);
    }
	
	public static void regularCMUpdate(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t)
	{
		CcuLog.d(L.TAG_CCU_DEVICE,"Regualar cm update pulse" + DLog.objectNullString((cmRegularUpdateMessage_t)));
	}


	public static void regularSmartStatUpdate(CmToCcuOverUsbSmartStatRegularUpdateMessage_t smartStatRegularUpdateMessage_t)
	{
		short nodeAddr = (short)smartStatRegularUpdateMessage_t.update.smartNodeAddress.get();
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{
			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + device.get("id") + "\"");

			String logicalCurTempPoint = "";
			double curTempVal = 0.0;
			double th2TempVal = 0.0;
			boolean isTh2Enabled = false;
			for(HashMap phyPoint : phyPoints) {
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					continue;
				}
				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				double val;
				switch (Port.valueOf(phyPoint.get("port").toString())){
					case SENSOR_RT:
						val = smartStatRegularUpdateMessage_t.update.roomTemperature.get();
						curTempVal = getRoomTempConversion(val);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						logicalCurTempPoint =  logPoint.get("id").toString();
						break;
					case TH2_IN:
						val = smartStatRegularUpdateMessage_t.update.externalThermistorInput2.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						isTh2Enabled = phyPoint.get("enabled").toString().equals("true");
						if(isTh2Enabled)
							th2TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
						else
							hayStack.writeHisValById(logPoint.get("id").toString(), val * 10);
						break;
					case ANALOG_IN_ONE:
						val = smartStatRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						break;
					case ANALOG_IN_TWO:
						val = smartStatRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint,val));
						break;
					case TH1_IN:
						val = smartStatRegularUpdateMessage_t.update.externalThermistorInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), ThermistorUtil.getThermistorValueToTemp(val * 10 ));
						break;
					case SENSOR_RH:
						SmartNodeSensorReading_t[] sensorReadingsHumidity = smartStatRegularUpdateMessage_t.update.sensorReadings;
						val = sensorReadingsHumidity[SensorType.HUMIDITY.ordinal()].sensorData.get();

						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getHumidityConversion(val));
						break;
					case SENSOR_CO2:
						SmartNodeSensorReading_t[] sensorReadingsCO2 = smartStatRegularUpdateMessage_t.update.sensorReadings;
						val = sensorReadingsCO2[SensorType.CO2.ordinal()].sensorData.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), val);
						break;
					case SENSOR_VOC:
						SmartNodeSensorReading_t[] sensorReadingsVOC = smartStatRegularUpdateMessage_t.update.sensorReadings;
						val = sensorReadingsVOC[SensorType.VOC.ordinal()].sensorData.get();

						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), val);
						break;
				}
			}

			//Write Current temp point based on th2 enabled or not
			if(isTh2Enabled && !logicalCurTempPoint.isEmpty())
				hayStack.writeHisValById(logicalCurTempPoint, th2TempVal);
			else
				hayStack.writeHisValById(logicalCurTempPoint, curTempVal);

		}
	}

	public static void rebootMessageFromCM(WrmOrCmRebootIndicationMessage_t wrmOrCMReootMsgs){
		Log.d("Pulse","Reboot Messages from CM for = "+wrmOrCMReootMsgs.wrmAddress+","+wrmOrCMReootMsgs.rebootCause);
		short address = (short)wrmOrCMReootMsgs.wrmAddress.get();
		if(address == 0x00 || (address == 0x01) || (address == L.ccu().getSmartNodeAddressBand()+99)){
			LSerial.getInstance().setResetSeedMessage(true);
		}
	}
	public static void updateSetTempFromSmartNode(CmToCcuOverUsbSnLocalControlsOverrideMessage_t setTempUpdate){
		short nodeAddr = (short)setTempUpdate.smartNodeAddress.get();
		double temp = setTempUpdate.setTemperature.get();
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{

			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + device.get("id") + "\"");

			for(HashMap phyPoint : phyPoints) {
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					continue;
				}
				hayStack.writeHisValById(phyPoint.get("id").toString(), temp);

				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				switch (Port.valueOf(phyPoint.get("port").toString())) {
					case DESIRED_TEMP:
						double desiredTemp = getDesredTempConversion(temp);
						if (desiredTemp > 0) {
							hayStack.writeHisValById(logPoint.get("id").toString(), desiredTemp);
							updateDesiredTemp(nodeAddr, desiredTemp);
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "updateSetTempFromDevice : desiredTemp " + desiredTemp);
						sendSetTemperatureAck(nodeAddr);

					break;
				}
			}
		}
	}

	public static void updateSetTempFromSmartStat(CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t setTempUpdate){
		short nodeAddr = (short)setTempUpdate.smartNodeAddress.get();
		double temp = setTempUpdate.setTemperature.get();
		SmartStatFanSpeed_t fanSpeed = setTempUpdate.fanSpeed.get();
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{

			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + device.get("id") + "\"");

			for(HashMap phyPoint : phyPoints) {
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					continue;
				}
				hayStack.writeHisValById(phyPoint.get("id").toString(), temp);

				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				switch (Port.valueOf(phyPoint.get("port").toString())) {
					case DESIRED_TEMP:
						double desiredTemp = getDesredTempConversion(temp);
						if (desiredTemp > 0) {
							hayStack.writeHisValById(logPoint.get("id").toString(), desiredTemp);
							updateSmartStatDesiredTemp(nodeAddr, desiredTemp);
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "updateSetTempFromSmartStat : desiredTemp " + desiredTemp);
						sendSetTemperatureAck(nodeAddr);

						break;
				}
			}
		}
	}


	public static void sendSetTemperatureAck(short address) {
		if (!LSerial.getInstance().isConnected()) {
			CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
			return;
		}
		CcuToCmOverUsbDeviceTempAckMessage_t msg = new CcuToCmOverUsbDeviceTempAckMessage_t();
		msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SET_TEMPERATURE_ACK);
		msg.smartNodeAddress.set(address);
		MeshUtil.sendStructToCM(msg);


	}
}
