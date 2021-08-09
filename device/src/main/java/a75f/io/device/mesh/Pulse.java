package a75f.io.device.mesh;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import a75f.io.alerts.AlertManager;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Zone;
import a75f.io.device.alerts.AlertGenerateHandler;
import a75f.io.device.serial.CcuToCmOverUsbCmResetMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDeviceTempAckMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbCmRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSmartStatRegularUpdateMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnLocalControlsOverrideMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartNodeSensorReading_t;
import a75f.io.device.serial.SmartStatFanSpeed_t;
import a75f.io.device.serial.SnRebootIndicationMessage_t;
import a75f.io.device.serial.WrmOrCmRebootIndicationMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.building.sensors.SensorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.StandaloneLogicalFanSpeeds;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.bo.haystack.device.SmartStat;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.pubnub.ZoneDataInterface;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.alerts.AlertsConstantsKt.CM_DEAD;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_DEAD;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_LOW_SIGNAL;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_REBOOT;
import static a75f.io.device.mesh.MeshUtil.checkDuplicateStruct;
import static a75f.io.device.mesh.MeshUtil.sendStructToNodes;
import static a75f.io.device.serial.SmartStatFanSpeed_t.FAN_SPEED_HIGH;
import static a75f.io.device.serial.SmartStatFanSpeed_t.FAN_SPEED_HIGH2;

/**
 * Created by Yinten on 9/15/2017.
 */

public class Pulse
{
	private static ZoneDataInterface currentTempInterface = null;
	private static int mTimeSinceCMDead = 0;
	private static boolean mDataReceived = false;
	private static HashMap mDeviceLowSignalCount = new HashMap();
	private static HashMap mDeviceLowSignalAlert = new HashMap();
	private static HashMap<Short, Long> mDeviceUpdate = new HashMap();

	public static void setCMDeadTimerIncrement(boolean isReboot){
		if(isReboot)mTimeSinceCMDead = 0;
		else
			mTimeSinceCMDead++;
		//TODO need to replace this 15 minutes to Tuner

		if(mTimeSinceCMDead > 15){
			mTimeSinceCMDead = 0;
			HashMap ccu = CCUHsApi.getInstance().read("ccu");
			String ccuName = ccu.get("dis").toString();
			AlertGenerateHandler.handleMessage(CM_DEAD, ccuName +" has stopped reporting data properly and needs to be serviced. Please contact 75F support for assistance.");
		}
	}
	public static void regularSNUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t)
	{
		short nodeAddr = (short)smartNodeRegularUpdateMessage_t.update.smartNodeAddress.get();
		int rssi = smartNodeRegularUpdateMessage_t.update.rssi.get();
		if (!mDeviceLowSignalAlert.containsKey(nodeAddr)) {
			mDeviceLowSignalAlert.put(nodeAddr,false);
		}
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{
			Device deviceInfo = new Device.Builder().setHashMap(device).build();
			//update last updated time
			mDeviceUpdate.put(nodeAddr,Calendar.getInstance().getTimeInMillis());
			if((rssi - 128) < 40) {
				if (!mDeviceLowSignalCount.containsKey(nodeAddr)) {
					mDeviceLowSignalCount.put(nodeAddr, 1);
				}
				int mLowSignalCount = (int)mDeviceLowSignalCount.get(nodeAddr);
				if (mLowSignalCount < 100) {
					mLowSignalCount++;
					mDeviceLowSignalCount.put(nodeAddr, mLowSignalCount);
				}
				mLowSignalCount = (int)mDeviceLowSignalCount.get(nodeAddr);
				if (!(boolean)mDeviceLowSignalAlert.get(nodeAddr) && mLowSignalCount >= 50) {
					mDeviceLowSignalAlert.put(nodeAddr,true);
					HashMap ccu = CCUHsApi.getInstance().read("ccu");
					String ccuName = ccu.get("dis").toString();
					AlertGenerateHandler.handleMessage(DEVICE_LOW_SIGNAL, "For"+" "+ccuName + " ," + deviceInfo.getDisplayName() + " is having an issues and has reported low signal for last 50 updates. If you continue to receive this alert, please contact 75F support.");
				}
			} else {
				mDeviceLowSignalCount.remove(nodeAddr);
				mDeviceLowSignalAlert.put(nodeAddr,false);
			}
			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + device.get("id") + "\"");
			boolean isSse = false;
			String logicalCurTempPoint = "";
			double curTempVal = 0.0;
			double th2TempVal = 0.0;
			boolean isTh2Enabled = false;
			for(HashMap phyPoint : phyPoints) {
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					continue;
				}
				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				if (logPoint.isEmpty()) {
					CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for "+phyPoint.get("dis"));
					continue;
				}
				Point logPointInfo = new Point.Builder().setHashMap(logPoint).build();
				isSse = logPointInfo.getMarkers().contains("sse");
				double val = 0;
				switch (Port.valueOf(phyPoint.get("port").toString())){
					case RSSI:
						hayStack.writeHisValueByIdWithoutCOV(phyPoint.get("id").toString(), (double)rssi);
						hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), (double)rssi);
						if(currentTempInterface != null) {
							currentTempInterface.refreshScreen(null);
						}
						break;
					case SENSOR_RT:
						val = smartNodeRegularUpdateMessage_t.update.roomTemperature.get();
						curTempVal = getRoomTempConversion(val);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						logicalCurTempPoint =  logPoint.get("id").toString();

						CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : roomTemp " + getRoomTempConversion(val));
						break;
					case TH2_IN:
						if (isMATDamperConfigured(logPoint, nodeAddr, "secondary", hayStack)) {
							CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : update DAB-dat2");
							hayStack.writeHisValById(logPoint.get("id").toString(),
							                         (double)smartNodeRegularUpdateMessage_t.update.airflow1Temperature.get()/10);
						} else {
							val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput2.get();
							isTh2Enabled = phyPoint.get("portEnabled").toString().equals("true");
							if (isTh2Enabled && isSse) {
								th2TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
								th2TempVal = CCUUtils.roundToOneDecimal(th2TempVal);
							} else {
								double oldEntTempVal = hayStack.readHisValById(logPoint.get("id").toString());
								double curEntTempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
								curEntTempVal = CCUUtils.roundToOneDecimal(curEntTempVal);
								hayStack.writeHisValById(phyPoint.get("id").toString(), val);
								if ((oldEntTempVal != curEntTempVal) && !isSse)
									hayStack.writeHisValById(logPoint.get("id").toString(), curEntTempVal);
							}
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "regularSmartNodeUpdate : Thermistor2 " + th2TempVal + "," + (val * 10) + "," +
							         logicalCurTempPoint + "," + isTh2Enabled + "," + logPointInfo.getMarkers().toString());
						}
						break;
					case ANALOG_IN_ONE:
						val = smartNodeRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						double oldDisAnalogVal = hayStack.readHisValById(logPoint.get("id").toString());
						double curDisAnalogVal = getAnalogConversion(phyPoint, logPoint, val);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						if (oldDisAnalogVal != curDisAnalogVal) {
							hayStack.writeHisValById(logPoint.get("id").toString(), curDisAnalogVal);
							if (currentTempInterface != null) {
								currentTempInterface.updateSensorValue(nodeAddr);
							}
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : analog1In " + curDisAnalogVal +" " +oldDisAnalogVal);
						break;
					case ANALOG_IN_TWO:
						val = smartNodeRegularUpdateMessage_t.update.externalAnalogVoltageInput2.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						double oldDynamicVar = hayStack.readHisValById(logPoint.get("id").toString());
						double dynamicVar = getAnalogConversion(phyPoint, logPoint, val);
						if (oldDynamicVar != dynamicVar) {
							if (logPointInfo.getMarkers().contains("pid")) {
								hayStack.writeHisValById(logPoint.get("id").toString(), dynamicVar + getPiOffsetValue(nodeAddr));
								if (currentTempInterface != null) {
									currentTempInterface.updateSensorValue(nodeAddr);
								}
							} else
								hayStack.writeHisValById(logPoint.get("id").toString(), dynamicVar);
						}
						break;
					case TH1_IN:
						if (isMATDamperConfigured(logPoint, nodeAddr, "primary", hayStack)) {
							CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : update DAB-dat1");
							hayStack.writeHisValById(logPoint.get("id").toString(),
							                         (double)smartNodeRegularUpdateMessage_t.update.airflow1Temperature.get()/10);
						} else {
							val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput1.get();
							double oldDisTempVal = hayStack.readHisValById(logPoint.get("id").toString());
							double curDisTempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
							curDisTempVal = CCUUtils.roundToOneDecimal(curDisTempVal);
							hayStack.writeHisValById(phyPoint.get("id").toString(), val);
							if (oldDisTempVal != curDisTempVal) {
								hayStack.writeHisValById(logPoint.get("id").toString(), curDisTempVal);
								if (currentTempInterface != null && logPointInfo.getMarkers().contains("pid")) {
									currentTempInterface.updateSensorValue(nodeAddr);
								}
							}
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "regularSmartNodeUpdate : Thermistor1 " + curDisTempVal + "," + oldDisTempVal +
							         "," + logPointInfo.getMarkers().toString() + "," + logPoint.get("id").toString());
						}
						break;
				}
			}
			
			SmartNodeSensorReading_t[] sensorReadings = smartNodeRegularUpdateMessage_t.update.sensorReadings;
			if (sensorReadings.length > 0) {
				handleSensorEvents(sensorReadings, nodeAddr);
			}

			//Write Current temp point based on th2 enabled or not
			if(isTh2Enabled && !logicalCurTempPoint.isEmpty() && isSse) {
				double oldCurTempVal = hayStack.readHisValById(logicalCurTempPoint);
				hayStack.writeHisValById(logicalCurTempPoint, th2TempVal);
				if ((currentTempInterface != null) && (oldCurTempVal != th2TempVal)) {
					Log.i("PubNub", "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(th2TempVal, nodeAddr);
				}
			}
			else if(!logicalCurTempPoint.isEmpty()){
				double oldCurTempVal = hayStack.readHisValById(logicalCurTempPoint);
				hayStack.writeHisValById(logicalCurTempPoint, curTempVal);
				if ((currentTempInterface != null) && (oldCurTempVal != curTempVal)) {
					Log.i("PubNub", "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(curTempVal, nodeAddr);
				}
			}
		}
	}
	
	private static boolean isMATDamperConfigured(HashMap logicalPoint, Short nodeAddr, String primary,
	                                             CCUHsApi hayStack) {
		return logicalPoint.containsKey(Tags.DAB) && hayStack.readDefaultVal(
			"damper and type and "+primary+" and group == \""+nodeAddr+"\"").intValue() == DamperType.MAT.ordinal();
	}
	
	private static void handleSensorEvents(SmartNodeSensorReading_t[] sensorReadings, short addr) {
		SmartNode node = new SmartNode(addr);
		int emVal = 0;
		
		for (SmartNodeSensorReading_t r : sensorReadings) {
			DLog.LogdStructAsJson(r);
			SensorType t = SensorType.values()[r.sensorType.get()];
			Port p = t.getSensorPort();
			if (p == null) {
				CcuLog.d(L.TAG_CCU_DEVICE, " Unknown sensor type : "+t.toString());
				continue;
			}
			double val = r.sensorData.get();
			RawPoint sp = node.getRawPoint(p);
			if (sp == null) {
				sp = node.addSensor(p);
                CcuLog.d(L.TAG_CCU_DEVICE, " Sensor Added , type "+t+" port "+p);
			}
			CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : "+t+" : "+val);
			switch (t) {
				case HUMIDITY:
					double oldHumidityVal = CCUHsApi.getInstance().readHisValById(sp.getId());
					double curHumidityVal = getHumidityConversion(val);
					CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
					if(oldHumidityVal != curHumidityVal)
					CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), curHumidityVal);
					break;
				case PRESSURE:
				case UVI:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
					CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),val);
					break;
				case OCCUPANCY:
				case ILLUMINANCE:
				case CO2:
				case CO:
				case NO:
				case SOUND:
				case VOC:
				case CO2_EQUIVALENT:
				case PM2P5:
				case PM10:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), CCUUtils.roundToOneDecimal(val) );
					CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),CCUUtils.roundToOneDecimal(val));
					break;
				case ENERGY_METER_HIGH:
					emVal = emVal > 0 ?  (emVal | (r.sensorData.get() << 12)) : r.sensorData.get();
					break;
				case ENERGY_METER_LOW:
					emVal = emVal > 0 ? ((emVal << 12) | r.sensorData.get()) : r.sensorData.get();
					break;
			}
		}
		
		if (emVal > 0) {
			RawPoint sp = node.getRawPoint(Port.SENSOR_ENERGY_METER);
			if (sp == null) {
				sp = node.addSensor(Port.SENSOR_ENERGY_METER);
			}
			if (emVal != CCUHsApi.getInstance().readHisValById(sp.getId()))
			{
				CCUHsApi.getInstance().writeHisValById(sp.getId(), (double) emVal);
				CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), (double) emVal);

				if (currentTempInterface != null) {
						currentTempInterface.updateSensorValue(addr);
				}
			}
			CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : EMR "+emVal);
		}
	
	}
	
	public static double round(double val) {
		return Math.round(100*val)/100;
	}
	
	public static Double getRoomTempConversion(Double temp) {
		return CCUUtils.roundToOneDecimal(temp/10.0);
	}

	public static Double getCMRoomTempConversion(Double temp, double tempOffset) {
		return CCUUtils.roundToOneDecimal((temp+tempOffset)/10.0);
	}
	public static Double getAnalogConversion(Double val) {
		return val/10.0;
	}
	public static Double getHumidityConversion(Double h) {
		return CCUUtils.roundToOneDecimal(h/10.0);
	}
	public static Double getDesredTempConversion(Double val) {
		return val/2;
	}
	
	public static Double getAnalogConversion(HashMap pp, HashMap lp, Double val) {
		double analogVal = val/1000;
		Sensor analogSensor;
		//If the analogType of physical point is set to one of the sensor types (Sensor.getSensorList) , corresponding sensor's
		//conversion formula is applied. Otherwise the input value that is already divided by 1000 is just returned.
		try
		{
			int index = (int)Double.parseDouble(pp.get("analogType").toString());
			analogSensor = SensorManager.getInstance().getExternalSensorList().get(index);
		}catch (NumberFormatException e) {
			e.printStackTrace();
			return analogVal;
		}
		Log.d(L.TAG_CCU_DEVICE,"Sensor input : type "+pp.get("analogType").toString()+" val "+analogVal);
		double analogConversion = analogSensor.minEngineeringValue +
				(analogSensor.maxEngineeringValue- analogSensor.minEngineeringValue) * analogVal / (analogSensor.maxVoltage - analogSensor.minVoltage);
		return CCUUtils.roundToTwoDecimal(analogConversion);
		
	}
	
	private static void updateDesiredTemp(int node, Double dt) {
		HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+node+"\"");
		Equip q = new Equip.Builder().setHashMap(equipMap).build();
		
		double cdb = TunerUtil.readTunerValByQuery("deadband and base and cooling and equipRef == \""+q.getId()+"\"");
		double hdb = TunerUtil.readTunerValByQuery("deadband and base and heating and equipRef == \""+q.getId()+"\"");
		String zoneId = HSUtil.getZoneIdFromEquipId(q.getId());
		Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
		if(occ != null) {
			cdb = occ.getCoolingDeadBand();
			hdb = occ.getHeatingDeadBand();
		}
		
		double coolingDesiredTemp = dt + cdb;
		double heatingDesiredTemp = dt - hdb;
		
		CcuLog.d(L.TAG_CCU_DEVICE,"updateDesiredTemp : dt "+dt+" cdb : "+cdb+" hdb: "+hdb+" coolingDesiredTemp: "+coolingDesiredTemp+" heatingDesiredTemp: "+heatingDesiredTemp);
		HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+q.getId()+"\"");
		if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
		//ScheduleProcessJob.handleDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint).build(), true, coolingDesiredTemp);
		CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);

		HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+q.getId()+"\"");
		if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
		//ScheduleProcessJob.handleDesiredTempUpdate(new Point.Builder().setHashMap(heatinDtPoint).build(), true, heatingDesiredTemp);
		CCUHsApi.getInstance().writeHisValById(heatinDtPoint.get("id").toString(), heatingDesiredTemp);


		HashMap singleDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and equipRef == \""+q.getId()+"\"");
		if (singleDtPoint == null || singleDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
		//ScheduleProcessJob.handleDesiredTempUpdate(new Point.Builder().setHashMap(singleDtPoint).build(), true, dt);
		CCUHsApi.getInstance().writeHisValById(singleDtPoint.get("id").toString(), dt);

		ScheduleProcessJob.handleManualDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint).build(),new Point.Builder().setHashMap(heatinDtPoint).build(),new Point.Builder().setHashMap(singleDtPoint).build(),coolingDesiredTemp,heatingDesiredTemp,dt);
		sendSNControlMessage((short)node,q.getId());
		sendSetTemperatureAck((short)node);

	}

    private static void updateSmartStatDesiredTemp(int node, Double dt, boolean sendAck) {
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+node+"\"");
        Equip q = new Equip.Builder().setHashMap(equipMap).build();

        double cdb = StandaloneTunerUtil.readTunerValByQuery("deadband and base and cooling and equipRef == \""+q.getId()+"\"");
        double hdb = StandaloneTunerUtil.readTunerValByQuery("deadband and base and heating and equipRef == \""+q.getId()+"\"");
        String zoneId = HSUtil.getZoneIdFromEquipId(q.getId());
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
		if(occ != null) {
			cdb = occ.getCoolingDeadBand();
			hdb = occ.getHeatingDeadBand();
		}
        double coolingDesiredTemp = dt + cdb;
        double heatingDesiredTemp = dt - hdb;


        HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \""+q.getId()+"\"");
        if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
	    //ScheduleProcessJob.handleDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint).build(), true, coolingDesiredTemp);
        try{
            CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);
        }catch (Exception e){
            e.printStackTrace();
        }

        HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+q.getId()+"\"");
        if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
	    //ScheduleProcessJob.handleDesiredTempUpdate(new Point.Builder().setHashMap(heatinDtPoint).build(), true, heatingDesiredTemp);
        try{
            CCUHsApi.getInstance().writeHisValById(heatinDtPoint.get("id").toString(), heatingDesiredTemp);
        }catch (Exception e){
            e.printStackTrace();
        }

		HashMap singleDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and equipRef == \""+q.getId()+"\"");
		if (singleDtPoint == null || singleDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
	    //ScheduleProcessJob.handleDesiredTempUpdate(new Point.Builder().setHashMap(singleDtPoint).build(), true, dt);
		try {
		    CCUHsApi.getInstance().writeHisValById(singleDtPoint.get("id").toString(), dt);
        }catch (Exception e){
		    e.printStackTrace();
        }
		ScheduleProcessJob.handleManualDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint).build(),new Point.Builder().setHashMap(heatinDtPoint).build(),new Point.Builder().setHashMap(singleDtPoint).build(),coolingDesiredTemp,heatingDesiredTemp,dt);
        if(sendAck) {
			sendSmartStatControlMessage((short) node, q.getId());
			sendSetTemperatureAck((short) node);
		}

	}
	
	public static void regularCMUpdate(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t) {
		mDataReceived = true;
		mTimeSinceCMDead = 0;
		CCUHsApi hayStack = CCUHsApi.getInstance();
		String addr = String.valueOf(L.ccu().getSmartNodeAddressBand());
		addr = addr.substring(0, addr.length()-2).concat("99");
		HashMap device = hayStack.read("device and addr == \""+Short.parseShort(addr)+"\"");
		double curTempVal = 0.0;
		if (device != null && device.size() > 0) {
			Device deviceInfo = new Device.Builder().setHashMap(device).build();
			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + deviceInfo.getId() + "\"");
			String logicalCurTempPoint = "";
			double th2TempVal = 0.0;
			boolean isTh2Enabled = false;
			for (HashMap phyPoint : phyPoints) {
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					continue;
				}
				HashMap logPoint = hayStack.read("point and id==" + phyPoint.get("pointRef"));
				
				if (logPoint.isEmpty()) {
					CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for "+phyPoint.get("dis"));
					continue;
				}
				
				double val;
				switch (Port.valueOf(phyPoint.get("port").toString())) {
					case RSSI:
						hayStack.writeHisValueByIdWithoutCOV(phyPoint.get("id").toString(), 1.0);
						hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), 1.0);
						if(currentTempInterface != null) {
							currentTempInterface.refreshScreen(null);
						}
						break;
					case SENSOR_RT:
						val = cmRegularUpdateMessage_t.roomTemperature.get();
						double tempOffset = CCUHsApi.getInstance().readPointPriorityValByQuery("point and zone and config and ti and temperature and offset and equipRef == \"" + deviceInfo.getEquipRef() + "\"");
						curTempVal = getCMRoomTempConversion(val,tempOffset);
						hayStack.writeHisValById(phyPoint.get("id").toString(), curTempVal);
						logicalCurTempPoint = logPoint.get("id").toString();
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : currentTemp " + curTempVal+","+tempOffset+","+val);
						break;
					case TH2_IN:
						val = cmRegularUpdateMessage_t.thermistor2.get();
						isTh2Enabled = phyPoint.get("portEnabled").toString().equals("true");
						if (isTh2Enabled) {
							th2TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
							th2TempVal = CCUUtils.roundToOneDecimal(th2TempVal);
						}else {

							double oldTh2TempVal = hayStack.readHisValById(logPoint.get("id").toString());
							double curTh2TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10 );
							curTh2TempVal = CCUUtils.roundToOneDecimal(curTh2TempVal);
							hayStack.writeHisValById(phyPoint.get("id").toString(), val);
							if(oldTh2TempVal != curTh2TempVal)
								hayStack.writeHisValById(logPoint.get("id").toString(), curTh2TempVal);
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Thermistor2 " + th2TempVal + "," + (val * 10) + "," + logicalCurTempPoint + "," + isTh2Enabled);
						break;
					case ANALOG_IN_ONE:
						val = cmRegularUpdateMessage_t.analogSense1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : analog1In " + getAnalogConversion(phyPoint, logPoint, val));
						break;
					case ANALOG_IN_TWO:
						val = cmRegularUpdateMessage_t.analogSense2.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : analog2In " + getAnalogConversion(phyPoint, logPoint, val));
						break;
					case TH1_IN:
						val = cmRegularUpdateMessage_t.thermistor1.get();
						double oldTh1TempVal = hayStack.readHisValById(logPoint.get("id").toString());
						double curTh1TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10 );
						curTh1TempVal = CCUUtils.roundToOneDecimal(curTh1TempVal);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						if(oldTh1TempVal != curTh1TempVal)
							hayStack.writeHisValById(logPoint.get("id").toString(), curTh1TempVal);
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Thermistor1 " + curTh1TempVal);
						break;
					case SENSOR_RH:
						val = cmRegularUpdateMessage_t.humidity.get();
						double oldHumidityVal = hayStack.readHisValById(logPoint.get("id").toString());
						if (val > 0 && (oldHumidityVal != val)) {
							hayStack.writeHisValById(phyPoint.get("id").toString(), val);
							hayStack.writeHisValById(logPoint.get("id").toString(), val/*getHumidityConversion(val)*/);
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Humidity " + val /*getHumidityConversion(val)*/);
						break;
				}
			}
			//Write Current temp point based on th2 enabled or not,
			if (isTh2Enabled && !logicalCurTempPoint.isEmpty()) {
				hayStack.writeHisValById(logicalCurTempPoint, th2TempVal);
				if (currentTempInterface != null) {
					Log.i("PubNub", "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + deviceInfo.getAddr() + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(th2TempVal, Short.parseShort(deviceInfo.getAddr()));
				}
			} else if (!logicalCurTempPoint.isEmpty()) {
				double oldCurTemp = hayStack.readHisValById(logicalCurTempPoint);
				if(oldCurTemp != curTempVal) {
					hayStack.writeHisValById(logicalCurTempPoint, curTempVal);
					if (currentTempInterface != null) {
						Log.i("PubNub", "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + deviceInfo.getAddr() + " currentTempVal:" + curTempVal);
						currentTempInterface.updateTemperature(curTempVal, Short.parseShort(deviceInfo.getAddr()));
					}
				}
			}
		}
		HashMap cmCurrentTemp = hayStack.read("point and system and cm and temp and current");
		if (cmCurrentTemp != null && cmCurrentTemp.size() > 0) {

			double val = cmRegularUpdateMessage_t.roomTemperature.get();
			if(curTempVal == 0.0) curTempVal = getCMRoomTempConversion(val,0);
			hayStack.writeHisValById(cmCurrentTemp.get("id").toString(), curTempVal);
			CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : CM currentTemp " + curTempVal+","+val);
		}
		HashMap cmHumidity = hayStack.read("point and system and cm and humidity");
		if (cmHumidity != null && cmHumidity.size() > 0) {
			double val = CCUUtils.roundToOneDecimal(cmRegularUpdateMessage_t.humidity.get());
			if (val > 0) {
				hayStack.writeHisValById(cmHumidity.get("id").toString(), val);
			}
			CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Humidity " + val );
		}
		
		//Done as per requirement from support team and not used in system operation.
		updateCMPhysicalPoints(cmRegularUpdateMessage_t);
	}
	
	private static void updateCMPhysicalPoints(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t) {
		
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and cm");
		if (!device.isEmpty()) {
			String deviceId = device.get("id").toString();
			HashMap analog1In = hayStack.read("point and analog1 and in and deviceRef == \""+deviceId+"\"");
			if (!analog1In.isEmpty()) {
				hayStack.writeHisValById(analog1In.get("id").toString(),
				                         (double) cmRegularUpdateMessage_t.analogSense1.get());
			}
			
			HashMap analog2In = hayStack.read("point and analog2 and in and deviceRef == \""+deviceId+"\"");
			if (!analog1In.isEmpty()) {
				hayStack.writeHisValById(analog2In.get("id").toString(),
				                         (double) cmRegularUpdateMessage_t.analogSense2.get());
			}
			
			HashMap th1In = hayStack.read("point and th1 and in and deviceRef == \""+deviceId+"\"");
			if (!th1In.isEmpty()) {
				hayStack.writeHisValById(th1In.get("id").toString(),
				                         (double) cmRegularUpdateMessage_t.thermistor1.get());
			}
			
			HashMap th2In = hayStack.read("point and th2 and in and deviceRef == \""+deviceId+"\"");
			if (!th2In.isEmpty()) {
				hayStack.writeHisValById(th2In.get("id").toString(),
				                         (double) cmRegularUpdateMessage_t.thermistor2.get());
			}
		}
	}


	public static void regularSmartStatUpdate(CmToCcuOverUsbSmartStatRegularUpdateMessage_t smartStatRegularUpdateMessage_t)
	{
		short nodeAddr = (short)smartStatRegularUpdateMessage_t.update.smartNodeAddress.get();
		double occupancyDetected  = smartStatRegularUpdateMessage_t.update.occupancyDetected.get();
		int rssi = smartStatRegularUpdateMessage_t.update.rssi.get();
		if (!mDeviceLowSignalAlert.containsKey(nodeAddr)) {
			mDeviceLowSignalAlert.put(nodeAddr,false);
		}
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{
			Device deviceInfo = new Device.Builder().setHashMap(device).build();
			mDeviceUpdate.put(nodeAddr,Calendar.getInstance().getTimeInMillis());
			if((rssi - 128) < 40) {
				if (!mDeviceLowSignalCount.containsKey(nodeAddr)) {
					mDeviceLowSignalCount.put(nodeAddr, 1);
				}
				int mLowSignalCount = (int)mDeviceLowSignalCount.get(nodeAddr);
				if (mLowSignalCount < 100) {
					mLowSignalCount++;
					mDeviceLowSignalCount.put(nodeAddr, mLowSignalCount);
				}
				mLowSignalCount = (int)mDeviceLowSignalCount.get(nodeAddr);
				if (!(boolean)mDeviceLowSignalAlert.get(nodeAddr) && mLowSignalCount >= 50){
					mDeviceLowSignalAlert.put(nodeAddr,true);
					HashMap ccu = CCUHsApi.getInstance().read("ccu");
					String ccuName = ccu.get("dis").toString();
					AlertGenerateHandler.handleMessage(DEVICE_LOW_SIGNAL, "For"+" "+ccuName + " ," + deviceInfo.getDisplayName() + " is having an issues and has reported low signal for last 50 updates. If you continue to receive this alert, please contact 75F support.");
				}
			} else {
				mDeviceLowSignalCount.remove(nodeAddr);
				mDeviceLowSignalAlert.put(nodeAddr,false);
			}
			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + deviceInfo.getId() + "\"");
			boolean is2pfcu = deviceInfo.getMarkers().contains("pipe2");
			String logicalCurTempPoint = "";
			double curTempVal = 0.0;
			double th2TempVal = 0.0;
			boolean isTh2Enabled = false;
			for(HashMap phyPoint : phyPoints) {
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					continue;
				}
				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				
				if (logPoint.isEmpty()) {
					CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for "+phyPoint.get("dis"));
					continue;
				}
				
				double val;
				switch (Port.valueOf(phyPoint.get("port").toString())){
					case RSSI:
						hayStack.writeHisValueByIdWithoutCOV(phyPoint.get("id").toString(), (double)rssi);
						hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), (double)rssi);
						if(currentTempInterface != null) {
							currentTempInterface.refreshScreen(null);
						}
						break;
					case SENSOR_RT:
						val = smartStatRegularUpdateMessage_t.update.roomTemperature.get();
						curTempVal = getRoomTempConversion(val);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						logicalCurTempPoint =  logPoint.get("id").toString();
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartStatUpdate : currentTemp "+curTempVal);
						break;
					case TH2_IN:
						val = smartStatRegularUpdateMessage_t.update.externalThermistorInput2.get();
						isTh2Enabled = phyPoint.get("portEnabled").toString().equals("true");
						if(isTh2Enabled && !is2pfcu){
							th2TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
							th2TempVal = CCUUtils.roundToOneDecimal(th2TempVal);
							}
						else if(isTh2Enabled && is2pfcu){
							double th2TempVal1 = ThermistorUtil.getThermistorValueToTemp(val * 10);
							hayStack.writeHisValById(phyPoint.get("id").toString(), val);
							hayStack.writeHisValById(logPoint.get("id").toString(), CCUUtils.roundToOneDecimal(th2TempVal1));
						}
						break;
					case ANALOG_IN_ONE:
						val = smartStatRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						break;
					case ANALOG_IN_TWO:
						val = smartStatRegularUpdateMessage_t.update.externalAnalogVoltageInput2.get();
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint,val));
						break;
					case TH1_IN:
						val = smartStatRegularUpdateMessage_t.update.externalThermistorInput1.get();

						double oldTh1TempVal = hayStack.readHisValById(logPoint.get("id").toString());
						double curTh1TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10 );
						curTh1TempVal = CCUUtils.roundToOneDecimal(curTh1TempVal);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						if(oldTh1TempVal != curTh1TempVal)
							hayStack.writeHisValById(logPoint.get("id").toString(), curTh1TempVal);
						break;
					case SENSOR_RH:
						val = smartStatRegularUpdateMessage_t.update.humidity.get();
						double oldHumidityVal = hayStack.readHisValById(logPoint.get("id").toString());
						double curHumidityVal = getHumidityConversion(val);
						if(curHumidityVal != oldHumidityVal) {
							hayStack.writeHisValById(phyPoint.get("id").toString(), val);
							hayStack.writeHisValById(logPoint.get("id").toString(), curHumidityVal);
						}
						CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartStatUpdate : Humidity "+curHumidityVal+","+smartStatRegularUpdateMessage_t.update.sensorReadings);
						break;
				}
			}
			SmartNodeSensorReading_t[] sensorReadings = smartStatRegularUpdateMessage_t.update.sensorReadings;
			if (sensorReadings.length > 0) {
				handleSmartStatSensorEvents(sensorReadings, nodeAddr, deviceInfo, occupancyDetected );
			}else if(occupancyDetected > 0){

				SmartStat node = new SmartStat(nodeAddr);
				RawPoint sp = node.getRawPoint(Port.SENSOR_OCCUPANCY);
				if(sp == null)
					sp = node.addSensor(Port.SENSOR_OCCUPANCY);

				updateOccupancyStatus(sp,occupancyDetected, deviceInfo,nodeAddr);
			}
			//Write Current temp point based on th2 enabled or not, except for 2pfcud
            double oldCurTempVal = hayStack.readHisValById(logicalCurTempPoint);
			if(isTh2Enabled && !logicalCurTempPoint.isEmpty() && !is2pfcu) {
				hayStack.writeHisValById(logicalCurTempPoint, th2TempVal);
				if ((currentTempInterface != null) && (oldCurTempVal != th2TempVal)) {
					Log.i("PubNub", "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(th2TempVal, nodeAddr);
				}
			}
			else if(!logicalCurTempPoint.isEmpty()){
				if(oldCurTempVal != curTempVal) {
					hayStack.writeHisValById(logicalCurTempPoint, curTempVal);
					if (currentTempInterface != null) {
						Log.i("PubNub", "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
						currentTempInterface.updateTemperature(curTempVal, nodeAddr);
					}
				}
			}
		}
	}

	public static void rebootMessageFromCM(WrmOrCmRebootIndicationMessage_t wrmOrCMReootMsgs){
		Log.d(L.TAG_CCU_DEVICE,"Reboot Messages from CM for = "+wrmOrCMReootMsgs.wrmAddress+","+wrmOrCMReootMsgs.rebootCause);
		short address = (short)wrmOrCMReootMsgs.wrmAddress.get();
		if(address == 0x00 || (address == 0x01) || (address == L.ccu().getSmartNodeAddressBand()+99)){
			LSerial.getInstance().setResetSeedMessage(true);

			String str = "addr:"+address;
			str+= ", master_fw_ver:"+wrmOrCMReootMsgs.majorFirmwareVersion+"."+wrmOrCMReootMsgs.minorFirmwareVersion;
			switch (wrmOrCMReootMsgs.rebootCause.get()){
				case MeshUtil.POWER_ON_RESET:
					str+= ", cause:"+"POWER_ON_RESET";
					break;
				case MeshUtil.CORE_BROWNOUT_RESET:
					str+= ", cause:"+"CORE_BROWNOUT_RESET";
					break;
				case MeshUtil.VDD_BROWNOUT_RESET:
					str+= ", cause:"+"VDD_BROWNOUT_RESET";
					break;
				case MeshUtil.EXTERNAL_RESET:
					str+= ", cause:"+"EXTERNAL_RESET";
					break;
				case MeshUtil.WATCHDOG_RESET:
					str+= ", cause:"+"WATCHDOG_RESET";
					break;
				case MeshUtil.SOFTWARE_RESET:
					str+= ", cause:"+"SOFTWARE_RESET";
					break;
				case MeshUtil.BACKUP_RESET:
					str+= ", cause:"+"BACKUP_RESET";
					break;
				default:
					str+= ", cause:"+"UNDEFINED";
					break;
			}
			str += ", device:"+ Arrays.toString(wrmOrCMReootMsgs.deviceId).replaceAll("[\\[\\]]","");
			str += ", serialnumber:"+ Arrays.toString(wrmOrCMReootMsgs.deviceSerial).replaceAll("[\\[\\]]","");

			AlertGenerateHandler.handleMessage(DEVICE_REBOOT,"Device reboot info - "+str);
		}
	}
	public static void smartDevicesRebootMessage(SnRebootIndicationMessage_t snRebootIndicationMsgs){
		Log.d(L.TAG_CCU_DEVICE,"smartDevicesRebootMessage = "+snRebootIndicationMsgs.smartNodeAddress+","+snRebootIndicationMsgs.rebootCause);
		short address = (short)snRebootIndicationMsgs.smartNodeAddress.get();
			LSerial.getInstance().setResetSeedMessage(true);

			String str = "addr:"+address;
			str+= ", master_fw_ver:"+snRebootIndicationMsgs.smartNodeMajorFirmwareVersion+"."+snRebootIndicationMsgs.smartNodeMinorFirmwareVersion;
			switch (snRebootIndicationMsgs.rebootCause.get()){
				case MeshUtil.POWER_ON_RESET:
					str+= ", cause:"+"POWER_ON_RESET";
					break;
				case MeshUtil.CORE_BROWNOUT_RESET:
					str+= ", cause:"+"CORE_BROWNOUT_RESET";
					break;
				case MeshUtil.VDD_BROWNOUT_RESET:
					str+= ", cause:"+"VDD_BROWNOUT_RESET";
					break;
				case MeshUtil.EXTERNAL_RESET:
					str+= ", cause:"+"EXTERNAL_RESET";
					break;
				case MeshUtil.WATCHDOG_RESET:
					str+= ", cause:"+"WATCHDOG_RESET";
					break;
				case MeshUtil.SOFTWARE_RESET:
					str+= ", cause:"+"SOFTWARE_RESET";
					break;
				case MeshUtil.BACKUP_RESET:
					str+= ", cause:"+"BACKUP_RESET";
					break;
				default:
					str+= ", cause:"+"UNDEFINED";
					break;
			}
		    str += ", device type:"+ snRebootIndicationMsgs.smartNodeDeviceType.get().name();
			str += ", device:"+ snRebootIndicationMsgs.smartNodeDeviceId;
			str += ", serialnumber:"+ snRebootIndicationMsgs.smartNodeSerialNumber;

			AlertGenerateHandler.handleMessage(DEVICE_REBOOT,"Device reboot info - "+str);
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
						//Compare with what was sent out.
						double curValue = LSmartNode.getDesiredTemp(nodeAddr);//hayStack.readHisValById(phyPoint.get("id").toString());
						double desiredTemp = getDesredTempConversion(temp);
						CcuLog.d(L.TAG_CCU_DEVICE, "updateSetTempFromDevice : desiredTemp " + desiredTemp+","+curValue);
						if (desiredTemp > 0 && (curValue != desiredTemp)) {
							hayStack.writeHisValById(logPoint.get("id").toString(), desiredTemp);
							updateDesiredTemp(nodeAddr, desiredTemp);
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "updateSetTempFromSmartStat : desiredTemp updated" +curValue+"->"+ desiredTemp);
						} else {
							sendSetTemperatureAck((short)nodeAddr);
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "updateSetTempFromSmartStat : desiredTemp not changed" +curValue+"->"+ desiredTemp);
						}
					break;
				}
			}
		}
	}
	public static void updateSetTempFromBacnet(short nodeAddr, double temp, String coolheat){

		HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+nodeAddr+"\"");
		Equip equip = new Equip.Builder().setHashMap(equipMap).build();
		double heatDB = TunerUtil.getZoneHeatingDeadband(equip.getRoomRef());
		double coolDB = TunerUtil.getZoneCoolingDeadband(equip.getRoomRef());
		double updatedHeatingDt = 0;
		double updatedCoolingDt = 0;
		HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \"" + equip.getId() + "\"");
		if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
		double coolingDesiredTemp = CCUHsApi.getInstance().readPointPriorityVal(coolingDtPoint.get("id").toString());
		Point coolingPt = new Point.Builder().setHashMap(coolingDtPoint).build();

		HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+equip.getId()+"\"");
		if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
			throw new IllegalArgumentException();
		}
		double heatingDesiredTemp = CCUHsApi.getInstance().readPointPriorityVal(heatinDtPoint.get("id").toString());
		Point heatingPt = new Point.Builder().setHashMap(heatinDtPoint).build();
		try{
			if(coolheat.equals("heating") && (heatingDesiredTemp != temp)) {
				if((temp + heatDB + coolDB) > coolingDesiredTemp) {
					updatedCoolingDt = temp + heatDB + coolDB;
					CCUHsApi.getInstance().writeHisValById(coolingPt.getId(), updatedCoolingDt);
				}
				updatedHeatingDt = temp;
				CCUHsApi.getInstance().writeHisValById(heatingPt.getId(), temp);
			}else if(coolheat.equals("cooling") && (coolingDesiredTemp != temp)) {
				if((temp - (heatDB + coolDB)) < heatingDesiredTemp) {
					updatedHeatingDt = temp - (heatDB + coolDB);
					CCUHsApi.getInstance().writeHisValById(heatingPt.getId(), updatedHeatingDt);
				}
				updatedCoolingDt = temp;
				CCUHsApi.getInstance().writeHisValById(coolingPt.getId(), updatedCoolingDt);
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		if(updatedCoolingDt != 0 || updatedHeatingDt != 0)
			ScheduleProcessJob.handleManualDesiredTempUpdate(coolingPt,heatingPt,null,updatedCoolingDt,updatedHeatingDt, 0);

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
						double curValue = LSmartStat.getDesiredTemp(nodeAddr);//hayStack.readHisValById(phyPoint.get("id").toString());
						double desiredTemp = getDesredTempConversion(temp);
						if (desiredTemp > 0 && (curValue != desiredTemp)) {
							hayStack.writeHisValById(logPoint.get("id").toString(), desiredTemp);
							updateSmartStatDesiredTemp(nodeAddr, desiredTemp, true);
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "updateSetTempFromSmartStat : desiredTemp updated" +curValue+"->"+ desiredTemp);
						} else {
							sendSetTemperatureAck((short)nodeAddr);
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "updateSetTempFromSmartStat : desiredTemp not changed" + desiredTemp+"->"+curValue);
						}
						
						break;
				}
			}
			HashMap fanOpModePoint = CCUHsApi.getInstance().read("point and standalone and fan and operation and mode and his and equipRef== \"" + device.get("equipRef") + "\"");
			double curFanMode = CCUHsApi.getInstance().readHisValById(fanOpModePoint.get("id").toString());
			StandaloneLogicalFanSpeeds curFanSpeeds = StandaloneLogicalFanSpeeds.values()[(int)curFanMode];

			CcuLog.d(L.TAG_CCU_DEVICE, "updateSetTempFromSmartStat : FanMode " + fanSpeed.name()+","+curFanSpeeds.name());
			boolean isFanModeChanged = false;
			
			if(fanOpModePoint != null && fanOpModePoint.size() > 0) {
				
				//FCU profiles on SmartStat extended the serial protocol to support medium Fan levels.
				//FanHigh2 when comes from an FCU device, it is in fact Medium.
				if (fanOpModePoint.containsKey(Tags.FCU)) {
					if (fanSpeed == FAN_SPEED_HIGH2) {
						fanSpeed = FAN_SPEED_HIGH;
					} else if (fanSpeed == FAN_SPEED_HIGH) {
						fanSpeed = FAN_SPEED_HIGH2;
					}
				}
				StandaloneLogicalFanSpeeds fanSpeed_t = StandaloneLogicalFanSpeeds.AUTO;
				switch (fanSpeed){
					case FAN_SPEED_LOW:
						if((curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_LOW_ALL_TIMES) && (curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_LOW_CURRENT_OCCUPIED) && (curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_LOW_OCCUPIED)) {
							isFanModeChanged = true;
							fanSpeed_t = StandaloneLogicalFanSpeeds.FAN_LOW_CURRENT_OCCUPIED;
						}
						break;
					case FAN_SPEED_HIGH:
						if((curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_HIGH_ALL_TIMES) && (curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED) && (curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_HIGH_OCCUPIED)) {
							isFanModeChanged = true;
							fanSpeed_t = StandaloneLogicalFanSpeeds.FAN_HIGH_CURRENT_OCCUPIED;
						}
						break;
					case FAN_SPEED_HIGH2:
						if((curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_HIGH2_ALL_TIMES) && (curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_HIGH2_CURRENT_OCCUPIED) && (curFanSpeeds != StandaloneLogicalFanSpeeds.FAN_HIGH2_OCCUPIED)) {
							isFanModeChanged = true;
							fanSpeed_t = StandaloneLogicalFanSpeeds.FAN_HIGH2_CURRENT_OCCUPIED;
						}
						break;
					case FAN_SPEED_OFF:
						if(curFanSpeeds != StandaloneLogicalFanSpeeds.OFF ){
							isFanModeChanged = true;
							fanSpeed_t = StandaloneLogicalFanSpeeds.OFF;
						}
						break;
					case FAN_SPEED_AUTO:
						if(curFanSpeeds != StandaloneLogicalFanSpeeds.AUTO ){
							isFanModeChanged = true;
							fanSpeed_t = StandaloneLogicalFanSpeeds.AUTO;
						}
						break;
				}
				if(isFanModeChanged) {
					CCUHsApi.getInstance().writePoint(fanOpModePoint.get("id").toString(), TunerConstants.UI_DEFAULT_VAL_LEVEL, "manual",
					                                  (double) fanSpeed_t.ordinal(), 0);
					CCUHsApi.getInstance().writeHisValById(fanOpModePoint.get("id").toString(),
					                                       (double)fanSpeed_t.ordinal());
				}
			}
		}
	}


	public static boolean sendSNControlMessage(short addr, String equipRef){
		if (!LSerial.getInstance().isConnected()) {
			CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
			return false;
		}
		CcuToCmOverUsbSnControlsMessage_t controlsMessage = LSmartNode.getControlMessage(null,addr,equipRef);
		if(!checkDuplicateStruct((short)controlsMessage.smartNodeAddress.get(),controlsMessage)){

			controlsMessage = LSmartNode.getCurrentTimeForControlMessage(controlsMessage);
			sendStructToNodes(controlsMessage);
		}
		return true;
	}
	public static boolean sendSmartStatControlMessage(short addr, String equipRef){
		if (!LSerial.getInstance().isConnected()) {
			CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
			return false;
		}
		CcuToCmOverUsbSmartStatControlsMessage_t controlsMessage = LSmartStat.getControlMessage(null,addr,equipRef);
		if(!checkDuplicateStruct((short)controlsMessage.address.get(),controlsMessage)){

			controlsMessage = LSmartStat.getCurrentTimeForControlMessage(controlsMessage);
			sendStructToNodes(controlsMessage);
		}
		return true;
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


	private static void handleSmartStatSensorEvents(SmartNodeSensorReading_t[] sensorReadings, short addr, Device device, double occupancyDetected) {
		SmartStat node = new SmartStat(addr);
		int emVal = 0;
		boolean hasSensorOccupancy = false;
		for (SmartNodeSensorReading_t r : sensorReadings) {
			SensorType t = SensorType.values()[r.sensorType.get()];
			Port p = t.getSensorPort();
			if (p == null) {
				continue;
			}
			double val = r.sensorData.get();
			RawPoint sp = node.getRawPoint(p);
			if (sp == null) {
				sp = node.addSensor(p);
				CcuLog.d(L.TAG_CCU_DEVICE, " Sensor Added , type "+t+" port "+p);
			}
			CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartStatUpdate : "+t+" : "+val);
			switch (t) {
				case HUMIDITY:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
					CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), getHumidityConversion(val));
					break;
				case CO2:
				case CO:
				case NO:
				case VOC:
				case PRESSURE:
				case SOUND:
				case CO2_EQUIVALENT:
				case ILLUMINANCE:
				case UVI:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
					CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),val);
					break;
				case OCCUPANCY:
					hasSensorOccupancy = true;
					updateOccupancyStatus(sp,val, device,addr);
					break;
				case ENERGY_METER_HIGH:
					emVal = emVal > 0 ?  (emVal | (r.sensorData.get() << 12)) : r.sensorData.get();
					break;
				case ENERGY_METER_LOW:
					emVal = emVal > 0 ? ((emVal << 12) | r.sensorData.get()) : r.sensorData.get();
					break;
			}
		}
		if( !hasSensorOccupancy){
			RawPoint sp = node.getRawPoint(Port.SENSOR_OCCUPANCY);
			if(sp == null)
				sp = node.addSensor(Port.SENSOR_OCCUPANCY);

			updateOccupancyStatus(sp,occupancyDetected, device,addr);
		}

		if (emVal > 0) {
			RawPoint sp = node.getRawPoint(Port.SENSOR_ENERGY_METER);
			if (sp == null) {
				sp = node.addSensor(Port.SENSOR_ENERGY_METER);
			}
			CCUHsApi.getInstance().writeHisValById(sp.getId(), (double)emVal );
			CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),(double)emVal);
		}

	}

	private static void updateOccupancyStatus(RawPoint sp, double val,Device device, short addr){

		double occuEnabled =  CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and enable and occupancy and group == \""+addr+"\"");
		double curOccuStatus = CCUHsApi.getInstance().readHisValById(sp.getPointRef());
		if((occuEnabled > 0) && (curOccuStatus != val) ) { //only if occupancy enabled
			if(val > 0) {
				Occupied occupied = ScheduleProcessJob.getOccupiedModeCache(device.getRoomRef());
				if (occupied != null)
					Log.d("Occupancy", "pulse occupancy sensor22=" + occupied.isOccupied() + "," + occupied.isPreconditioning());
				if ((occupied != null) && !occupied.isOccupied() && !occupied.isPreconditioning()) {
					//update desired temp for forced occupied
					double dt = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and desired and average and sp and equipRef == \"" + device.getEquipRef() + "\"");
					updateSmartStatDesiredTemp(addr, dt, false);
				}
			}
			HashMap occDetPoint = CCUHsApi.getInstance().read("point and occupancy and detection and his and equipRef== \"" + device.getEquipRef() + "\"");
			if ((occDetPoint != null) && (occDetPoint.size() > 0))
				CCUHsApi.getInstance().writeHisValById(occDetPoint.get("id").toString(),val);
		}
		CCUHsApi.getInstance().writeHisValById(sp.getId(), val);
		CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), val);
	}

	public static double getDesiredTemp(short node, String tag)
	{
		HashMap point = CCUHsApi.getInstance().read("point and air and temp and desired and "+tag+" and sp and group == \""+node+"\"");
		if (point == null || point.size() == 0) {
			Log.d(L.TAG_CCU_DEVICE, " Desired Temp point does not exist for equip , sending 0");
			return 0;
		}
		return CCUHsApi.getInstance().readPointPriorityVal(point.get("id").toString());
	}
	public static void setCurrentTempInterface(ZoneDataInterface in) { currentTempInterface = in; }

	public static void sendCMResetMessage(){
		CcuToCmOverUsbCmResetMessage_t msg = new CcuToCmOverUsbCmResetMessage_t();
		msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_CM_RESET);
		msg.reset.set((short)1);
		MeshUtil.sendStructToCM(msg);
	}

	public static void checkForDeviceDead(){

		for (Short address: mDeviceUpdate.keySet()){
			if (mDeviceUpdate.get(address) == null){
				return;
			}
             long lastUpdateTime = mDeviceUpdate.get(address);
             long currentTime = Calendar.getInstance().getTimeInMillis();
             //trigger device dead alert if no signal update over 15min
			 Device d = HSUtil.getDevice(address);
			 double zoneDeadTime = TunerUtil.readTunerValByQuery("zone and dead and time",d.getEquipRef());
			 zoneDeadTime = zoneDeadTime > 0 ? zoneDeadTime : 15;
             if ((currentTime - lastUpdateTime) > ( zoneDeadTime *60 * 1000)){
				 HashMap ccu = CCUHsApi.getInstance().read("ccu");
				 String ccuName = ccu.get("dis").toString();

				 AlertGenerateHandler.handleMessage(DEVICE_DEAD, "For"+" "+ccuName + "," +d.getDisplayName() +" has stopped reporting data. Please contact 75F support.");
				 mDeviceUpdate.remove(address);
				 break;
			 } else {
				 AlertManager.getInstance().fixDeviceDead(String.valueOf(address));
			 }
		}
	}


	public static void sendSeedMessage(boolean isSmartStat, boolean isCcuAsZone, Short addr, String roomRef, String floorRef){
		NodeType deviceType = NodeType.SMART_NODE;
		if(isSmartStat)
			deviceType = NodeType.SMART_STAT;
		else if(isCcuAsZone)
			deviceType = NodeType.CONTROL_MOTE;
		CcuLog.d(L.TAG_CCU_DEVICE,"=================NOW SEEDING NEW PROFILE====================="+addr+","+roomRef+","+isSmartStat);
		Device d = HSUtil.getDevice(addr);
		Zone zone = HSUtil.getZone(roomRef, floorRef);
		CcuLog.d(L.TAG_CCU_DEVICE,"=================NOW SEEDING NEW PROFILE22====================="+zone.getDisplayName()+","+roomRef+","+isSmartStat);
		switch (deviceType) {
			case SMART_NODE:
				String snprofile = "dab";
				if(d.getMarkers().contains("sse"))
					snprofile = "sse";
				else if(d.getMarkers().contains("lcm"))
					snprofile = "lcm";
				else if(d.getMarkers().contains("iftt"))
					snprofile = "iftt";
				CcuLog.d(L.TAG_CCU_DEVICE,"=================NOW SENDING SN SEEDS====================="+zone.getDisplayName()+","+addr);
				CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(zone, Short.parseShort(d.getAddr()),d.getEquipRef(),snprofile);
				MeshUtil.sendStructToCM(seedMessage);
				LSerial.getInstance().setNodeSeeding(false);
				break;
			case SMART_STAT:
				String profile = "cpu";
				if(d.getMarkers().contains("hpu"))
					profile = "hpu";
				else if(d.getMarkers().contains("pipe2"))
					profile = "pipe2";
				else if(d.getMarkers().contains("pipe4"))
					profile = "pipe4";
				CcuLog.d(L.TAG_CCU_DEVICE,"=================NOW SENDING SS SEEDS====================="+zone.getDisplayName()+","+addr);
				CcuToCmOverUsbDatabaseSeedSmartStatMessage_t seedSSMessage = LSmartStat.getSeedMessage(zone,Short.parseShort(d.getAddr()),d.getEquipRef(),profile);
				MeshUtil.sendStructToCM( seedSSMessage);
				LSerial.getInstance().setNodeSeeding(false);
				break;
		}
	}

	private static double getPiOffsetValue(short nodeAddr){
		double isAnalog2Enabled = CCUHsApi.getInstance().readDefaultVal("point and pid and config and analog2 and enabled and setpoint and group == \"" + nodeAddr + "\"");
		if(isAnalog2Enabled > 0)
			return CCUHsApi.getInstance().readDefaultVal("point and pid and config and setpoint and sensor and offset and group == \"" + nodeAddr + "\"");
		else
			return 0;
	}
}
