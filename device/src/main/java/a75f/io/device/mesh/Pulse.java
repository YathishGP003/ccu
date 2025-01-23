package a75f.io.device.mesh;

import static a75f.io.alerts.AlertsConstantsKt.CM_DEAD;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_DEAD;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_LOW_SIGNAL;
import static a75f.io.alerts.AlertsConstantsKt.DEVICE_REBOOT;
import static a75f.io.device.mesh.DLog.tempLogdStructAsJson;
import static a75f.io.device.mesh.MeshUtil.checkDuplicateStruct;
import static a75f.io.device.mesh.MeshUtil.sendStructToNodes;
import static a75f.io.device.serial.SmartStatFanSpeed_t.FAN_SPEED_HIGH;
import static a75f.io.device.serial.SmartStatFanSpeed_t.FAN_SPEED_HIGH2;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import a75f.io.constants.WhoFiledConstants;
import a75f.io.device.alerts.AlertGenerateHandler;
import a75f.io.device.cm.ControlMoteMessageHandlerKt;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSmartStatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDeviceTempAckMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSmartStatControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettings2Message_t;
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
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.devices.CmBoardDevice;
import a75f.io.domain.equips.TIEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.ccu.CazEquipUtil;
import a75f.io.logic.bo.building.definitions.DamperType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.StandaloneLogicalFanSpeeds;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.building.sensors.SensorType;
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu;
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu;
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.bo.haystack.device.SmartStat;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.tuners.BuildingTunerCache;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.TunerUtil;

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
	public static HashMap<Short, Long> mDeviceUpdate = new HashMap();

	public static void setCMDeadTimerIncrement(boolean isReboot){
		if(isReboot)mTimeSinceCMDead = 0;
		else
			mTimeSinceCMDead++;
		//TODO need to replace this 15 minutes to Tuner

		if(mTimeSinceCMDead > 15){
			mTimeSinceCMDead = 0;
			String ccuName = Domain.ccuDevice.getCcuDisName();
			AlertGenerateHandler.handleMessage(CM_DEAD, ccuName +" has stopped reporting data properly and needs to " +
					"be serviced. "+CCUUtils.getSupportMsgContent(Globals.getInstance().getApplicationContext()));
		}
	}
	public static void regularSNUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t smartNodeRegularUpdateMessage_t)
	{
		long time = System.currentTimeMillis();
		short nodeAddr = (short)smartNodeRegularUpdateMessage_t.update.smartNodeAddress.get();
		int rssi = smartNodeRegularUpdateMessage_t.update.rssi.get();
		if (!mDeviceLowSignalAlert.containsKey(nodeAddr)) {
			mDeviceLowSignalAlert.put(nodeAddr,false);
		}
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		CcuLog.d(L.TAG_CCU_DEVICE, "Found device "+device);
		if (device != null && !device.isEmpty())
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
					String ccuName = Domain.ccuDevice.getCcuDisName();
					AlertGenerateHandler.handleDeviceMessage(DEVICE_LOW_SIGNAL,
							"For"+" "+ccuName + " ," + deviceInfo.getDisplayName() + " is having an issue and has reported low signal for last 50 updates. If you continue to receive this alert, "+CCUUtils.getSupportMsgContent(Globals.getInstance().getApplicationContext()),
							deviceInfo.getId());
				}
			} else {
				mDeviceLowSignalCount.remove(nodeAddr);
				mDeviceLowSignalAlert.put(nodeAddr,false);
			}
			if(Globals.getInstance().isTemporaryOverrideMode()) {
				updateRssiPointIfAvailable(hayStack, device.get("id").toString(), rssi, 1, nodeAddr);
				return;
			}
			HashMap equipMap = hayStack.read("equip and id == " + device.get("equipRef"));
			Equip equip = new Equip.Builder().setHashMap(equipMap).build();
			boolean isDomainEquip = equipMap.containsKey("domainName") ? !equip.getDomainName().equals(null) : false;
			boolean isAcb = isDomainEquip ? (equip.getDomainName().equals(DomainName.smartnodeActiveChilledBeam) || equip.getDomainName().equals(DomainName.helionodeActiveChilledBeam)) : false;

			boolean isCondensateNc = false;
			if (hayStack.readPointPriorityValByQuery("point and equipRef == \"" + device.get("equipRef") + "\" and domainName == \"" + DomainName.thermistor2Type + "\"") != null) {
				if (hayStack.readPointPriorityValByQuery("point and equipRef == \"" + device.get("equipRef") + "\" and domainName == \"" + DomainName.thermistor2Type + "\"") > 0.0) { isCondensateNc = true; }
			}
			boolean isBypassDamper = isDomainEquip && equip.getDomainName().equals(DomainName.smartnodeBypassDamper);

			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and sensor and deviceRef == \"" + device.get("id") + "\"");
			boolean isSse = false;
			String logicalCurTempPoint = "";
			double curTempVal = 0.0;
			double th2TempVal = 0.0;
			boolean isTh2Enabled = false;
			for(HashMap phyPoint : phyPoints) {
				CcuLog.d(L.TAG_CCU_DEVICE, "Physical point "+phyPoint);
				if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
					CcuLog.d(L.TAG_CCU_DEVICE, "No logical point for "+phyPoint.get("dis"));
					continue;
				}
				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				if (logPoint.isEmpty()) {
					CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for "+phyPoint.get("dis"));
					continue;
				}
				Point logPointInfo = new Point.Builder().setHashMap(logPoint).build();
				isSse = logPointInfo.getMarkers().contains("sse");
				double val;
				Port currentPort = getPhysicalPointPort(phyPoint);
				CcuLog.i(L.TAG_CCU_DEVICE, "regularSNUpdate: PORT "+currentPort);
				if (currentPort == null) {
					CcuLog.d(L.TAG_CCU_DEVICE, "Port not found for "+phyPoint);
					continue;
				}
				switch (currentPort){
					case RSSI:
						hayStack.writeHisValueByIdWithoutCOV(phyPoint.get("id").toString(), (double)rssi);
						hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), 1.0);
						if(currentTempInterface != null) {
							currentTempInterface.refreshHeartBeatStatus(String.valueOf(nodeAddr));
						}
						break;
					case SENSOR_RT:
						val = smartNodeRegularUpdateMessage_t.update.roomTemperature.get();
						curTempVal = getRoomTempConversion(val);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						logicalCurTempPoint =  logPoint.get("id").toString();

						CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : roomTemp " + curTempVal);
						break;
					case TH2_IN:
						if (isMATDamperConfigured(logPoint, nodeAddr, DomainName.damper2Type, hayStack)) {
							CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : update DAB-dat2");
							hayStack.writeHisValById(logPoint.get("id").toString(),
							                         (double)smartNodeRegularUpdateMessage_t.update.airflow2Temperature.get()/10);
						} else {
							val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput2.get();
							isTh2Enabled = phyPoint.get("portEnabled").toString().equals("true");
							if (isTh2Enabled && isSse) {
								th2TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
								th2TempVal = CCUUtils.roundToOneDecimal(th2TempVal);
								hayStack.writeHisValById(logPoint.get("id").toString(), th2TempVal);
							} else if (isTh2Enabled && isAcb) {
								boolean curCondensateStatus = isCondensateNc ? ((val*10) >= 10000) : ((val*10) < 10000);
								double curCondensateSensor = curCondensateStatus ? 1.0 : 0.0;
								hayStack.writeHisValById(logPoint.get("id").toString(), curCondensateSensor);
							} else {
								double curEntTempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
								curEntTempVal = CCUUtils.roundToOneDecimal(curEntTempVal);
								if (!isSse) {
									hayStack.writeHisValById(logPoint.get("id").toString(), curEntTempVal);
								}
							}
							hayStack.writeHisValById(phyPoint.get("id").toString(), val/100);
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "regularSmartNodeUpdate : Thermistor2 " + th2TempVal + "," + (val * 10) + "," +
							         logicalCurTempPoint + "," + isTh2Enabled + "," + logPointInfo.getMarkers().toString());
						}
						break;
					case ANALOG_IN_ONE:
						val = smartNodeRegularUpdateMessage_t.update.externalAnalogVoltageInput1.get();
						CcuLog.i(L.TAG_CCU_DEVICE, "regularSNUpdate: "+val);
						boolean isPressureOnAI1 = hayStack.readDefaultVal("point and domainName == \"" + DomainName.pressureSensorType + "\" and equipRef == \"" + equip.getId() + "\"") > 0.0;
						double oldDisAnalogVal = hayStack.readHisValById(logPoint.get("id").toString());
						double curDisAnalogVal = (isBypassDamper && isPressureOnAI1) ? getPressureConversion(equip, val) : getAnalogConversion(phyPoint, logPoint, val);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val);
						CcuLog.i(L.TAG_CCU_DEVICE, " Feedback regularSNUpdate: id "+logPoint.get("id").toString());
						hayStack.writeHisValById(logPoint.get("id").toString(), curDisAnalogVal);
						if (oldDisAnalogVal != curDisAnalogVal) {
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
						double dynamicVar =  getAnalogConversion(phyPoint, logPoint, val);
						double newDynamicVar = oldDynamicVar;
						if (oldDynamicVar != dynamicVar) {
							newDynamicVar = dynamicVar;
							if (logPointInfo.getMarkers().contains("pid")) {
								newDynamicVar = dynamicVar + getPiOffsetValue(nodeAddr);
								if (currentTempInterface != null) {
									currentTempInterface.updateSensorValue(nodeAddr);
								}
							}
						}
						hayStack.writeHisValById(logPoint.get("id").toString(), newDynamicVar);
						break;
					case TH1_IN:
						if (isMATDamperConfigured(logPoint, nodeAddr, DomainName.damper1Type, hayStack)) {
							CcuLog.d(L.TAG_CCU_DEVICE, "regularSmartNodeUpdate : update DAB-dat1");
							hayStack.writeHisValById(logPoint.get("id").toString(),
							                         (double)smartNodeRegularUpdateMessage_t.update.airflow1Temperature.get()/10);
						} else {
							val = smartNodeRegularUpdateMessage_t.update.externalThermistorInput1.get();
							double oldDisTempVal = hayStack.readHisValById(logPoint.get("id").toString());
							double curDisTempVal;
							if (logPoint.containsKey("domainName")) {
								String domainName = logPoint.get("domainName").toString();
								if ("genericAlarmNO".equalsIgnoreCase(domainName)) {
									curDisTempVal = (val >= 1000) ? 0.0 : 1.0;
								} else if ("genericAlarmNC".equalsIgnoreCase(domainName)) {
									curDisTempVal = (val >= 1000) ? 1.0 : 0.0;
								} else {
									curDisTempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
								}
							} else {
								curDisTempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
							}

							curDisTempVal = CCUUtils.roundToOneDecimal(curDisTempVal);
							hayStack.writeHisValById(phyPoint.get("id").toString(), val/100);
							hayStack.writeHisValById(logPoint.get("id").toString(), curDisTempVal);
								if (currentTempInterface != null && logPointInfo.getMarkers().contains("pid")) {
									currentTempInterface.updateSensorValue(nodeAddr);
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
				handleSensorEvents(sensorReadings, nodeAddr, deviceInfo, isDomainEquip);
			}

			//Write Current temp point based on th2 enabled or not
			if(isTh2Enabled && !logicalCurTempPoint.isEmpty() && isSse) {
				hayStack.writeHisValById(logicalCurTempPoint, th2TempVal);
				if ((currentTempInterface != null)) {
					CcuLog.i(L.TAG_CCU_DEVICE,
					    "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(th2TempVal, nodeAddr);
				}
			} else if(!logicalCurTempPoint.isEmpty()){
				hayStack.writeHisValById(logicalCurTempPoint, curTempVal);
				if ((currentTempInterface != null)) {
					CcuLog.i(L.TAG_CCU_DEVICE,
					    "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(curTempVal, nodeAddr);
				}
			}

			boolean isVav = isDomainEquip ?
					(equip.getDomainName().equals(DomainName.smartnodeActiveChilledBeam)
						|| equip.getDomainName().equals(DomainName.helionodeActiveChilledBeam)
						|| equip.getDomainName().equals(DomainName.smartnodeVAVReheatNoFan)
						|| equip.getDomainName().equals(DomainName.helionodeVAVReheatNoFan)
						|| equip.getDomainName().equals(DomainName.smartnodeVAVReheatParallelFan)
						|| equip.getDomainName().equals(DomainName.helionodeVAVReheatParallelFan)
						|| equip.getDomainName().equals(DomainName.smartnodeVAVReheatSeriesFan)
						|| equip.getDomainName().equals(DomainName.helionodeVAVReheatSeriesFan)
					) : false;

			if (isVav && TrueCFMUtil.isTrueCfmEnabled(hayStack, equip.getId())) {
				if (TrueCFMUtil.isCfmOnEdgeActive(hayStack, equip.getId())) {
					double damperCmd = smartNodeRegularUpdateMessage_t.update.damperPositionCfmLoop.get();
					double reheatCmd =  smartNodeRegularUpdateMessage_t.update.reheatPositionAfterDat.get();
					// old regular message will not send any feedback so reading CCU calculated
					// Why 127 a75f.io.device.mesh.LSerial.modifyByteArray
					if (smartNodeRegularUpdateMessage_t.update.damperPositionCfmLoop.get() == 127) {
						damperCmd = hayStack.readHisValByQuery("point and domainName == \"" + DomainName.damperCmd + "\" and equipRef == \"" + equip.getId() + "\"");
					}
					if (smartNodeRegularUpdateMessage_t.update.reheatPositionAfterDat.get() == 127) {
						reheatCmd = hayStack.readHisValByQuery("point and domainName == \"" + DomainName.reheatCmd + "\" and equipRef == \"" + equip.getId() + "\"");
					}

					CcuLog.d(L.TAG_CCU_SERIAL, "Update calculated damper/reheat positions: damperCmd: "+damperCmd +" reheatCmd: "+reheatCmd );
					hayStack.writeHisValByQuery("point and domainName == \"" + DomainName.damperCmdCal + "\" and equipRef == \"" + equip.getId() + "\"",damperCmd);
					hayStack.writeHisValByQuery("point and domainName == \"" + DomainName.reheatCmdCal + "\" and equipRef == \"" + equip.getId() + "\"",reheatCmd);
				} else {
					hayStack.writeHisValByQuery("point and domainName == \"" + DomainName.damperCmdCal + "\" and equipRef == \"" + equip.getId() + "\"", 0.0);
					hayStack.writeHisValByQuery("point and domainName == \"" + DomainName.reheatCmdCal + "\" and equipRef == \"" + equip.getId() + "\"", 0.0);
				}
			}
		}
		CcuLog.i(L.TAG_CCU_DEVICE, nodeAddr+" : regularSNUpdate timeMS "+(System.currentTimeMillis()-time));
	}

	private static double getPressureConversion(Equip equip, double val) {
		double minVoltage = CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + DomainName.sensorMinVoltage + "\" and equipRef == \""+equip.getId()+"\"");
		double maxVoltage = CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + DomainName.sensorMaxVoltage + "\" and equipRef == \""+equip.getId()+"\"");
		double minPressure = CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + DomainName.pressureSensorMinVal + "\" and equipRef == \""+equip.getId()+"\"");
		double maxPressure = CCUHsApi.getInstance().readPointPriorityValByQuery("point and domainName == \"" + DomainName.pressureSensorMaxVal + "\" and equipRef == \""+equip.getId()+"\"");

		double i = ((.001*val) - minVoltage) / (maxVoltage - minVoltage);
		if (i < 0) i = 0;
		if (i > 1) i = 1;

        return i * (maxPressure - minPressure) + minPressure;
	}

	private static boolean isMATDamperConfigured(HashMap logicalPoint, Short nodeAddr, String domainName,
	                                             CCUHsApi hayStack) {
		return logicalPoint.containsKey(Tags.DAB) && hayStack.readDefaultVal(
				"point and domainName == \""+domainName+"\" " +
						" and group == \""+nodeAddr+"\"").intValue() == DamperType.MAT.ordinal();

	}

	private static void handleSensorEvents(SmartNodeSensorReading_t[] sensorReadings, short addr,Device device, boolean isDomainEquip) {
		SmartNode node = new SmartNode(addr);
		int emVal = 0;

		for (SmartNodeSensorReading_t r : sensorReadings) {
			DLog.LogdStructAsJson(r);
			SensorType t = SensorType.values()[r.sensorType.get()];
			Port p = t.getSensorPort();
			if (p == null) {
				CcuLog.d(L.TAG_CCU_DEVICE, " Unknown sensor type : "+ t);
				continue;
			}
			double val = r.sensorData.get();
			/* Smartnode  represents the lower 11 bits as unsigned, as uses bit 12 to indicate
			  if value is negative or not, if value is negative bit 12 is set to 1 and binary value
			  becomes greater than 2048.*/
			if(t == SensorType.PRESSURE && val > 2048 ){
				val = getPressureValue(Integer.toBinaryString((int) val));
			}
			RawPoint sp = node.getRawPoint(p);
			if (sp == null) {
				if (isDomainEquip) {
					CcuLog.d(L.TAG_CCU_DEVICE, " Sensor point not available in the model, hence not created. port:" + p);
					continue;
				}
				sp = node.addSensor(p);
                CcuLog.d(L.TAG_CCU_DEVICE, " Sensor Added , type "+t+" port "+p);
			} else if (sp.getPointRef() == null) {
				if (isDomainEquip) {
					sp = node.addDomainEquipSensorFromRawPoint(sp, p);
				} else {
					sp = node.addEquipSensorFromRawPoint(sp, p);
				}
			}
			CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : "+t+" : "+val);
			switch (t) {
				case HUMIDITY:
					double curHumidityVal = getHumidityConversion(val);
					CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
					if(sp.getPointRef() != null)
						CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), curHumidityVal);
					break;
				case PRESSURE:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
					if (sp.getPointRef() != null) { CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), convertPressureFromPaToInH2O(val)); }
					break;
				case UVI:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), val );
					if (sp.getPointRef() != null) { CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),val); }
					break;
				case OCCUPANCY:
					updateOTNOccupancyStatus(sp, val, device);
					break;
				case ILLUMINANCE:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), CCUUtils.roundToOneDecimal(val * 10) );
					if (sp.getPointRef() != null) { CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),CCUUtils.roundToOneDecimal(val*10)); }
					break;
				case CO2:
				case CO:
				case NO:
				case SOUND:
				case VOC:
				case CO2_EQUIVALENT:
				case PM2P5:
				case PM10:
					CCUHsApi.getInstance().writeHisValById(sp.getId(), CCUUtils.roundToOneDecimal(val) );
					if (sp.getPointRef() != null)  { CCUHsApi.getInstance().writeHisValById(sp.getPointRef(),CCUUtils.roundToOneDecimal(val)); }
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
			double emValFinal = CCUUtils.roundToOneDecimal(emVal/10); //SN sends multiples of 10
			RawPoint sp = node.getRawPoint(Port.SENSOR_ENERGY_METER);
			if (sp == null) {
				sp = node.addSensor(Port.SENSOR_ENERGY_METER);
			}
			if (emValFinal != CCUHsApi.getInstance().readHisValById(sp.getId()))
			{
				CCUHsApi.getInstance().writeHisValById(sp.getId(), emValFinal);
				CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), emValFinal);

				if (currentTempInterface != null) {
						currentTempInterface.updateSensorValue(addr);
				}
			}
			CcuLog.d(L.TAG_CCU_DEVICE,"regularSmartNodeUpdate : EMR "+emValFinal);
		}
	
	}

	private static double getPressureValue(String pressureBinary) {
		int originalValue = Integer.parseInt(pressureBinary, 2);
		if ((originalValue & (1 << 11)) != 0) {
			// If the 12th bit is set to 1, remove it by setting it to 0
			int modifiedValue = originalValue & ~(1 << 11);
			String modifiedBinaryData = String.format("%12s", Integer.toBinaryString(modifiedValue)).replace(' ', '0');
			return Integer.parseInt(modifiedBinaryData, 2)*(-1);
		} else {
			// If the 12th bit is not set to 1, no modification needed
			return Integer.parseInt(pressureBinary, 2);
		}
	}

	public static double convertPressureFromPaToInH2O(double Pa) {
		double inH2O = Pa / 248.84;
		return CCUUtils.roundToTwoDecimal(inH2O);
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
		CcuLog.i(L.TAG_CCU_DEVICE, "Feedback Node address "+ pp.get("group")+" Feedback  type"+pp.get("analogType"));
		double analogVal = val/1000;
		CcuLog.i(L.TAG_CCU_DEVICE, "Feedback Node address analogVal after devide "+analogVal);
		if(lp.containsKey("vav") || lp.containsKey("dab") || lp.containsKey("dualDuct") || lp.containsKey("bypassDamper")) {
			double damperPercent= DeviceUtil.getPercentageFromVoltage(analogVal,
					Objects.requireNonNull(pp.get("analogType")).toString());
			CcuLog.i(L.TAG_CCU_DEVICE, "Feedback Reversed damper percent  : "+damperPercent);
			return (double) Math.round(damperPercent);
		}
		Sensor analogSensor;
		//If the analogType of physical point is set to one of the sensor types (Sensor.getSensorList) , corresponding sensor's
		//conversion formula is applied. Otherwise the input value that is already divided by 1000 is just returned.
		try
		{
			int index = (int)Double.parseDouble(pp.get("analogType").toString());
			analogSensor = SensorManager.getInstance().getExternalSensorList().get(index);
			if (lp.containsKey("pid")) {
				Double pressureValue = convertToPressureValue(val, analogSensor);
				if (pressureValue != null) return pressureValue;
			}

		}catch (NumberFormatException e) {
			CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
			return analogVal;
		}
		CcuLog.d(L.TAG_CCU_DEVICE,"Sensor input : type "+pp.get("analogType").toString()+" val "+analogVal);
		double analogConversion = analogSensor.minEngineeringValue +
				(analogSensor.maxEngineeringValue- analogSensor.minEngineeringValue) * analogVal / (analogSensor.maxVoltage - analogSensor.minVoltage);
		return CCUUtils.roundToTwoDecimal(analogConversion);
		
	}

	private static Double convertToPressureValue(Double val, Sensor analogSensor) {
		if (externalSensors().contains(analogSensor.sensorName)) {
			double i = ((0.001 * val) - analogSensor.minVoltage) /
					(analogSensor.maxVoltage - analogSensor.minVoltage);

			if (i < 0) {
				i = 0;
			}
			if (i > 1) {
				i = 1;
			}

			double pressureValue = i * (analogSensor.maxEngineeringValue - analogSensor.minEngineeringValue) +
					analogSensor.minEngineeringValue;
			CcuLog.i(L.TAG_CCU_DEVICE, "AI1 Pressure Sensor : "+pressureValue);
			return CCUUtils.roundToTwoDecimal(pressureValue);
		}
		CcuLog.i(L.TAG_CCU_DEVICE, "analogType is not pressure sensor");
		return null;
	}

	public static List<String> externalSensors() {
		return Arrays.asList(
				"Pressure Sensor (0-2)",
				"Differential Pressure Sensor (0-0.25)"
		);
	}

	private static void updateDesiredTemp(int node, Double dt) {
		HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+node+"\"");
		Equip equip = new Equip.Builder().setHashMap(equipMap).build();
		if( equip == null ) return;
		double coolingDesiredTemp;
		double heatingDesiredTemp = 0;
		double averageTemp;
		double cdb = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and cooling and deadband and not multiplier and roomRef == \""+equip.getRoomRef()+"\"");
		double hdb = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and heating and deadband and not multiplier and roomRef == \""+equip.getRoomRef()+"\"");
		String zoneId = HSUtil.getZoneIdFromEquipId(equip.getId());
		Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
		if(occ != null) {
			cdb = occ.getCoolingDeadBand();
			hdb = occ.getHeatingDeadBand();
		}

		double coolingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and cooling and deadband and not multiplier and roomRef == \""+equip.getRoomRef()+"\"");
		double heatingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and heating and deadband and not multiplier and roomRef == \""+equip.getRoomRef()+"\"");


		 coolingDesiredTemp = DeviceUtil.getValidDesiredCoolingTemp(
				 dt,coolingDeadband,DeviceUtil.getMaxCoolingUserLimit(zoneId),
				DeviceUtil.getMinCoolingUserLimit(zoneId)
		);
		HashMap<Object, Object> coolingDtPoint = CCUHsApi.getInstance().readEntity("point and air and temp and desired and cooling and sp and equipRef == \""+equip.getId()+"\"");
		HashMap<Object, Object> heatingDtPoint = CCUHsApi.getInstance().readEntity("point and air and temp and desired and heating and sp and equipRef == \""+equip.getId()+"\"");
		int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
				" == \"" + equip.getRoomRef() + "\"").intValue();
		TemperatureMode temperatureMode = TemperatureMode.values()[modeType];
		if(temperatureMode == TemperatureMode.COOLING){
			coolingDesiredTemp = dt;
			averageTemp = (dt + CCUHsApi.getInstance().readPointPriorityVal(heatingDtPoint.get("id").toString())) / 2;
		}else if(temperatureMode == TemperatureMode.HEATING) {
			heatingDesiredTemp = dt;
			averageTemp = (dt + CCUHsApi.getInstance().readPointPriorityVal(coolingDtPoint.get("id").toString())) / 2;
		}
		else {
			coolingDesiredTemp = DeviceUtil.getValidDesiredCoolingTemp(
					dt, coolingDeadband, DeviceUtil.getMaxCoolingUserLimit(zoneId),
					DeviceUtil.getMinCoolingUserLimit(zoneId)
			);

			heatingDesiredTemp = DeviceUtil.getValidDesiredHeatingTemp(
					dt, heatingDeadband, DeviceUtil.getMaxHeatingUserLimit(zoneId),
					DeviceUtil.getMinHeatingUserLimit(zoneId)
			);
			averageTemp = dt;
		}


		CcuLog.d(L.TAG_CCU_DEVICE,"updateDesiredTemp : dt "+dt+" cdb : "+cdb+" hdb: "+hdb+" coolingDesiredTemp: "+coolingDesiredTemp+" heatingDesiredTemp: "+heatingDesiredTemp+" desiredTemp: "+averageTemp);
		if (coolingDtPoint == null || coolingDtPoint.isEmpty()) {
			throw new IllegalArgumentException();
		}
		CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);

		if (heatingDtPoint == null || heatingDtPoint.isEmpty()) {
			throw new IllegalArgumentException();
		}
		CCUHsApi.getInstance().writeHisValById(heatingDtPoint.get("id").toString(), heatingDesiredTemp);


		HashMap singleDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and (avg or average) and sp and equipRef == \""+equip.getId()+"\"");
		if (singleDtPoint == null || singleDtPoint.isEmpty()) {
			throw new IllegalArgumentException();
		}
		CCUHsApi.getInstance().writeHisValById(singleDtPoint.get("id").toString(), dt);

		String who = WhoFiledConstants.SMARTNODE_WHO;
		if(equipMap.containsKey(Tags.HELIO_NODE)){
			who = WhoFiledConstants.HELIONODE_WHO;
		}
		DeviceUtil.updateDesiredTempFromDevice(new Point.Builder().setHashMap(coolingDtPoint).build(),
				new Point.Builder().setHashMap(heatingDtPoint).build(),
				new Point.Builder().setHashMap(singleDtPoint).build(),
				coolingDesiredTemp, heatingDesiredTemp, dt, CCUHsApi.getInstance(), who);
		sendSNControlMessage((short)node,equip.getId());
		sendSetTemperatureAck((short)node);

	}

    private static void updateSmartStatDesiredTemp(int node, Double dt, boolean sendAck) {
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+node+"\"");
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
		String zoneId = HSUtil.getZoneIdFromEquipId(equip.getId());
		if( equip == null ) return;
		double coolingDesiredTemp = 0;
		double heatingDesiredTemp= 0;
		int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
				" == \"" + equip.getRoomRef() + "\"").intValue();
		TemperatureMode temperatureMode = TemperatureMode.values()[modeType];
		BuildingTunerCache buildingTuner = BuildingTunerCache.getInstance();
		double coolingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and cooling and deadband and roomRef == \""+equip.getRoomRef()+"\"");
		double heatingDeadband = CCUHsApi.getInstance().readPointPriorityValByQuery("zone and heating and deadband and roomRef == \""+equip.getRoomRef()+"\"");

		HashMap<Object, Object> coolingDtPoint = CCUHsApi.getInstance().readEntity("point and air and temp and desired and cooling and sp and equipRef == \""+equip.getId()+"\"");
		HashMap<Object, Object> heatinDtPoint = CCUHsApi.getInstance().readEntity("point and air and temp and desired and heating and sp and equipRef == \""+equip.getId()+"\"");
		if(temperatureMode == TemperatureMode.COOLING){
			coolingDesiredTemp = dt;
		}else if(temperatureMode == TemperatureMode.HEATING){
			heatingDesiredTemp = dt;
		}else {
			coolingDesiredTemp = DeviceUtil.getValidDesiredCoolingTemp(
					dt, coolingDeadband, DeviceUtil.getMaxCoolingUserLimit(zoneId),
					DeviceUtil.getMinCoolingUserLimit(zoneId)
			);

			heatingDesiredTemp = DeviceUtil.getValidDesiredHeatingTemp(
					dt, heatingDeadband, DeviceUtil.getMaxHeatingUserLimit(zoneId),
					DeviceUtil.getMinHeatingUserLimit(zoneId)
			);
		}

        if (coolingDtPoint == null || coolingDtPoint.isEmpty()) {
            throw new IllegalArgumentException();
        }
        try{
			if(!(temperatureMode == TemperatureMode.HEATING)) {
				CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);
			}
        }catch (Exception e){
			CcuLog.e(L.TAG_CCU_DEVICE, "error", e);
        }

        if (heatinDtPoint == null || heatinDtPoint.isEmpty()) {
            throw new IllegalArgumentException();
        }
        try{
			if(!(temperatureMode == TemperatureMode.COOLING)) {
				CCUHsApi.getInstance().writeHisValById(heatinDtPoint.get("id").toString(), heatingDesiredTemp);
			}
        }catch (Exception e){
			CcuLog.e(L.TAG_CCU_DEVICE, "error", e);
        }

		HashMap singleDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and average and sp and equipRef == \""+equip.getId()+"\"");
		if (singleDtPoint == null || singleDtPoint.isEmpty()) {
			throw new IllegalArgumentException();
		}
			try {
		    CCUHsApi.getInstance().writeHisValById(singleDtPoint.get("id").toString(), dt);
        }catch (Exception e){
				CcuLog.e(L.TAG_CCU_DEVICE, "error", e);
        }
	    DeviceUtil.updateDesiredTempFromDevice(new Point.Builder().setHashMap(coolingDtPoint).build(),
				new Point.Builder().setHashMap(heatinDtPoint).build(),
				new Point.Builder().setHashMap(singleDtPoint).build(),
				coolingDesiredTemp,heatingDesiredTemp,dt, CCUHsApi.getInstance(), WhoFiledConstants.SMARTSTAT_WHO);
        if(sendAck) {
			sendSmartStatControlMessage((short) node, equip.getId());
			sendSetTemperatureAck((short) node);
		}

	}

	public static void regularCMUpdate(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t) {
		CCUHsApi hayStack = CCUHsApi.getInstance();
		double curTempVal = 0.0;
		CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate" );
		mDataReceived = true;
		mTimeSinceCMDead = 0;
		if (L.ccu().systemProfile instanceof VavAdvancedAhu
				|| L.ccu().systemProfile instanceof DabAdvancedAhu) {
			ControlMoteMessageHandlerKt.handleAdvancedAhuCmUpdate(cmRegularUpdateMessage_t);
		}
		HashMap device = hayStack.readEntity("domainName == \"" + DomainName.ccuTiDevice + "\"");
		CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate "+device );
		if (!device.isEmpty()) {
			String deviceRef = device.get(Tags.ID).toString();
			String nodeAddress = device.get(Tags.ADDR).toString();

			TIEquip tiEquip = new TIEquip(device.get(Tags.EQUIPREF).toString());
			ArrayList<HashMap<Object, Object>> phyPoints = hayStack.readAllEntities("point and physical and sensor and deviceRef == \"" + deviceRef + "\"");

			double tempOffset = tiEquip.getTemperatureOffset().readPriorityVal() * 10;

			String logicalCurTempPoint = "";
			double th2TempVal = 0.0;
			double th1TempVal = 0.0;
			boolean isTh2Enabled = false;
			boolean isTh1Enabled = false;
			boolean isTh1RoomTempInTI = false;
			boolean isTh2RoomTempInTI = false;


			for (HashMap phyPoint : phyPoints) {
				if (phyPoint.get(Tags.POINTREF) == null || phyPoint.get(Tags.POINTREF) == "") {
					continue;
				}
				HashMap<Object, Object> logPoint = hayStack.readMapById(phyPoint.get("pointRef").toString());

				if (logPoint.isEmpty()) {
					CcuLog.d(L.TAG_CCU_DEVICE, "Logical mapping does not exist for " + phyPoint.get("dis"));
					continue;
				}

				double val;
				Port currentPort = getPhysicalPointPort(phyPoint);
				CcuLog.i(L.TAG_CCU_DEVICE, "regularCMUpdate: PORT " + currentPort);
				switch (currentPort) {
					case RSSI:
						hayStack.writeHisValueByIdWithoutCOV(phyPoint.get("id").toString(), 1.0);
						hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), 1.0);
						if (currentTempInterface != null) {
							currentTempInterface.refreshHeartBeatStatus(nodeAddress);
						}
						break;
					case SENSOR_RT:
						val = cmRegularUpdateMessage_t.roomTemperature.get();
						curTempVal = getCMRoomTempConversion(val, tempOffset);
						hayStack.writeHisValById(phyPoint.get("id").toString(), curTempVal);
						logicalCurTempPoint = logPoint.get("id").toString();
						if (phyPoint.get("portEnabled").toString().equals("true") && device.containsKey("ti")) {
							tiEquip.getRoomTemperature().writeHisVal(curTempVal);
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : currentTemp " + curTempVal + "," + tempOffset + "," + val);
						break;
					case TH2_IN:
						val = cmRegularUpdateMessage_t.thermistor2.get();
						isTh2Enabled = phyPoint.get("portEnabled").toString().equals("true");
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : pointID - th2 " + phyPoint.get("id").toString());
						if (isTh2Enabled) {
							double offSet = tempOffset;
							boolean isPortMappedToSAT = CazEquipUtil.isPortMappedToSupplyAirTemprature(phyPoint.get(
									"pointRef").toString());
							if (isPortMappedToSAT) {
								offSet = 0.0;
							}
							th2TempVal =
									getCMRoomTempConversion(ThermistorUtil.getThermistorValueToTemp(val * 10) * 10, offSet);
							double oldTh2TempVal = hayStack.readHisValById(logPoint.get("id").toString());
							double curTh2TempVal =
									getCMRoomTempConversion(ThermistorUtil.getThermistorValueToTemp(val * 10) * 10, offSet);
							curTh2TempVal = CCUUtils.roundToOneDecimal(curTh2TempVal);
							if (logPoint.containsKey(Tags.TI) && !isPortMappedToSAT) {
								curTempVal = curTh2TempVal;
								isTh2RoomTempInTI = true;
							}
							hayStack.writeHisValById(phyPoint.get("id").toString(), val / 100);
							if (oldTh2TempVal != curTh2TempVal)
								hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), curTh2TempVal);
							tiEquip.getCurrentTemp().writeHisVal(th2TempVal);
							CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : curtempvalth2 " + hayStack.readHisValById(logPoint.get("id").toString()));

						}
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Thermistor2 " + th2TempVal + "," + (val * 10) + "," + logicalCurTempPoint + "," + isTh2Enabled);
						break;
					case ANALOG_IN_ONE:
						val = cmRegularUpdateMessage_t.analogSense1.get();

						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						hayStack.writeHisValById(phyPoint.get("id").toString(), val / 100);
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : analog1In " + getAnalogConversion(phyPoint, logPoint, val));
						break;
					case ANALOG_IN_TWO:
						val = cmRegularUpdateMessage_t.analogSense2.get();
						hayStack.writeHisValById(logPoint.get("id").toString(), getAnalogConversion(phyPoint, logPoint, val));
						hayStack.writeHisValById(phyPoint.get("id").toString(), val / 100);
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : analog2In " + getAnalogConversion(phyPoint, logPoint, val));
						break;
					case TH1_IN:
						val = cmRegularUpdateMessage_t.thermistor1.get();
						double oldTh1TempVal = hayStack.readHisValById(logPoint.get("id").toString());
						isTh1Enabled = phyPoint.get("portEnabled").toString().equals("true");
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : pointID - th1 " + phyPoint.get("id").toString());
						if (isTh1Enabled) {
							double offSet = tempOffset;
							boolean isPortMappedToSAT = CazEquipUtil.isPortMappedToSupplyAirTemprature(phyPoint.get(
									"pointRef").toString());
							if (isPortMappedToSAT) {
								offSet = 0.0;
							}
							double curTh1TempVal =
									getCMRoomTempConversion(ThermistorUtil.getThermistorValueToTemp(val * 10) * 10, offSet);
							th1TempVal = curTh1TempVal;
							hayStack.writeHisValById(phyPoint.get("id").toString(), val / 100);
							if (logPoint.containsKey(Tags.TI) && !isPortMappedToSAT) {
								curTempVal = curTh1TempVal;
								isTh1RoomTempInTI = true;
							}
							if (oldTh1TempVal != curTh1TempVal)
								hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), curTh1TempVal);
							tiEquip.getCurrentTemp().writeHisVal(th1TempVal);
							CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : curtempval " + hayStack.readHisValById(logPoint.get("id").toString()));
							CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Thermistor1 " + curTh1TempVal);
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Thermistor1 " + th1TempVal + "," + (val * 10) + "," + logicalCurTempPoint + "," + isTh1Enabled);
						break;
					case SENSOR_RH:
						val = cmRegularUpdateMessage_t.humidity.get();
						double oldHumidityVal = hayStack.readHisValById(logPoint.get("id").toString());
						if (val > 0 && (oldHumidityVal != val)) {
							hayStack.writeHisValById(phyPoint.get("id").toString(), val);
							hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), val/*getHumidityConversion(val)*/);
						}
						CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Humidity " + val /*getHumidityConversion(val)*/);
						break;
				}
			}

			if (isTh1Enabled && !logicalCurTempPoint.isEmpty() && isTh1RoomTempInTI) {
				hayStack.writeHisValById(logicalCurTempPoint, th1TempVal);
				if (currentTempInterface != null) {
					CcuLog.i(L.TAG_CCU_DEVICE, "Current Temp Refresh th1:" + logicalCurTempPoint + " Node Address:" + nodeAddress + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(th1TempVal, Short.parseShort(nodeAddress));
				}
			}//Write Current temp point based on th2 enabled or not,
			else if (isTh2Enabled && !logicalCurTempPoint.isEmpty() && isTh2RoomTempInTI) {
				hayStack.writeHisValById(logicalCurTempPoint, th2TempVal);
				if (currentTempInterface != null) {
					CcuLog.d(L.TAG_CCU_DEVICE, "Current Temp Refresh th2:" + logicalCurTempPoint + " Node Address:" + nodeAddress+ " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(th2TempVal, Short.parseShort(nodeAddress));
				}
			} else if (!logicalCurTempPoint.isEmpty()) {
				double oldCurTemp = hayStack.readHisValById(logicalCurTempPoint);
				if(oldCurTemp != curTempVal) {
					hayStack.writeHisValueByIdWithoutCOV(logicalCurTempPoint, curTempVal);
					if (currentTempInterface != null) {
						CcuLog.i(L.TAG_CCU_DEVICE, "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddress + " currentTempVal:" + curTempVal);
						currentTempInterface.updateTemperature(curTempVal, Short.parseShort(nodeAddress));
					}
				}
			}
		}
		HashMap cmCurrentTemp = hayStack.read("point and system and temp and (current or space)");
		if (cmCurrentTemp != null && !cmCurrentTemp.isEmpty()) {

			double val = cmRegularUpdateMessage_t.roomTemperature.get();
			if(curTempVal == 0.0) curTempVal = getCMRoomTempConversion(val,0);
			hayStack.writeHisValById(cmCurrentTemp.get("id").toString(), curTempVal);
			CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : CM currentTemp " + curTempVal+","+val);
		}
		HashMap cmHumidity = hayStack.read("point and system and cm and humidity");
		if (cmHumidity != null && !cmHumidity.isEmpty()) {
			double val = CCUUtils.roundToOneDecimal(cmRegularUpdateMessage_t.humidity.get());
			if (val > 0) {
				hayStack.writeHisValById(cmHumidity.get("id").toString(), val);
			}
			CcuLog.d(L.TAG_CCU_DEVICE, "regularCMUpdate : Humidity " + val );
		}

		//Done as per requirement from support team and not used in system operation.
		updateCMPhysicalPoints(cmRegularUpdateMessage_t);
	}


	private static String getRoomTempSensorId(Device deviceInfo) {

		CCUHsApi hayStack = CCUHsApi.getInstance();
		String roomTempId = null;
		HashMap<Object, Object> roomTempSensorPoint = hayStack.readEntity(
				"point and space and not type and temp and equipRef == \"" + deviceInfo.getEquipRef() + "\"");
		if (!roomTempSensorPoint.isEmpty()) {
			roomTempId = roomTempSensorPoint.get("id").toString();
		}
		return roomTempId;
	}

	private static void updateCMPhysicalPoints(CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t) {

		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and cm");
		if (!device.isEmpty()) {
			String deviceId = device.get("id").toString();
			if (device.containsKey("domainName")) {
				updateDomainCmUpdates(deviceId, cmRegularUpdateMessage_t);
				return;
			}

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

	public static void updateDomainCmUpdates(String deviceId, CmToCcuOverUsbCmRegularUpdateMessage_t cmRegularUpdateMessage_t) {
		CmBoardDevice cmBoardDevice = new CmBoardDevice(deviceId);
		cmBoardDevice.getAnalog1In().writeHisVal(cmRegularUpdateMessage_t.analogSense1.get());
		cmBoardDevice.getAnalog2In().writeHisVal(cmRegularUpdateMessage_t.analogSense2.get());
		cmBoardDevice.getTh1In().writeHisVal(cmRegularUpdateMessage_t.thermistor1.get());
		cmBoardDevice.getTh2In().writeHisVal(cmRegularUpdateMessage_t.thermistor2.get());
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
		if (device != null && !device.isEmpty())
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
					String ccuName = Domain.ccuDevice.getCcuDisName();
					AlertGenerateHandler.handleDeviceMessage(DEVICE_LOW_SIGNAL,
							"For"+" "+ccuName + " ," + deviceInfo.getDisplayName() + " is having an issue and has " +
									"reported low signal for last 50 updates. If you continue to receive this alert, "+CCUUtils.getSupportMsgContent(Globals.getInstance().getApplicationContext()), deviceInfo.getId());
				}
			} else {
				mDeviceLowSignalCount.remove(nodeAddr);
				mDeviceLowSignalAlert.put(nodeAddr,false);
			}
			if(Globals.getInstance().isTemporaryOverrideMode()) {
				updateRssiPointIfAvailable(hayStack, deviceInfo.getId(), rssi, 1, nodeAddr);
				return;
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
						hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), 1.0);
						if(currentTempInterface != null) {
							currentTempInterface.refreshHeartBeatStatus(String.valueOf(nodeAddr));
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
						th2TempVal = ThermistorUtil.getThermistorValueToTemp(val * 10);
						th2TempVal = CCUUtils.roundToOneDecimal(th2TempVal);
						double th2TempVal1 = ThermistorUtil.getThermistorValueToTemp(val * 10);
						hayStack.writeHisValById(phyPoint.get("id").toString(), val/100);
						hayStack.writeHisValById(logPoint.get("id").toString(), CCUUtils.roundToOneDecimal(th2TempVal1));
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
						hayStack.writeHisValById(phyPoint.get("id").toString(), val/100);
						if(oldTh1TempVal != curTh1TempVal)
							hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), curTh1TempVal);
						break;
					case SENSOR_RH:
						val = smartStatRegularUpdateMessage_t.update.humidity.get();
						double oldHumidityVal = hayStack.readHisValById(logPoint.get("id").toString());
						double curHumidityVal = getHumidityConversion(val);
						if(curHumidityVal != oldHumidityVal) {
							hayStack.writeHisValById(phyPoint.get("id").toString(), val);
							hayStack.writeHisValueByIdWithoutCOV(logPoint.get("id").toString(), curHumidityVal);
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
					CcuLog.i(L.TAG_CCU_DEVICE, "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
					currentTempInterface.updateTemperature(th2TempVal, nodeAddr);
				}
			}
			else if(!logicalCurTempPoint.isEmpty()){
				if(oldCurTempVal != curTempVal) {
					hayStack.writeHisValueByIdWithoutCOV(logicalCurTempPoint, curTempVal);
					if (currentTempInterface != null) {
						CcuLog.i(L.TAG_CCU_DEVICE, "Current Temp Refresh Logical:" + logicalCurTempPoint + " Node Address:" + nodeAddr + " currentTempVal:" + curTempVal);
						currentTempInterface.updateTemperature(curTempVal, nodeAddr);
					}
				}
			}
		}
	}

	public static void rebootMessageFromCM(WrmOrCmRebootIndicationMessage_t wrmOrCMReootMsgs){
		CcuLog.d(L.TAG_CCU_DEVICE,"Reboot Messages from CM for = "+wrmOrCMReootMsgs.wrmAddress+","+wrmOrCMReootMsgs.rebootCause);
		short address = (short)wrmOrCMReootMsgs.wrmAddress.get();
		if(address == 0x00 || (address == 0x01) || (address == L.ccu().getAddressBand()+99)){
			LSerial.getInstance().setResetSeedMessage(true);

			String firmwareVersion = wrmOrCMReootMsgs.majorFirmwareVersion+"."+wrmOrCMReootMsgs.minorFirmwareVersion;
			CCUUtils.writeFirmwareVersion(firmwareVersion, address, true);
			String str = "addr:"+address;
			str+= ", master_fw_ver:"+firmwareVersion;
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

			AlertGenerateHandler.handleDeviceMessage(DEVICE_REBOOT,"Device reboot info - "+str,
					CCUHsApi.getInstance().readId("device and addr == \""+address+"\""));
		}
	}
	public static void smartDevicesRebootMessage(SnRebootIndicationMessage_t snRebootIndicationMsgs){

		CcuLog.d(L.TAG_CCU_DEVICE,"smartDevicesRebootMessage = "+snRebootIndicationMsgs.smartNodeAddress+
				", "+snRebootIndicationMsgs.rebootCause+ "Node Status ");
		short address = (short)snRebootIndicationMsgs.smartNodeAddress.get();
			LSerial.getInstance().setResetSeedMessage(true);
		String firmwareVersion =
				snRebootIndicationMsgs.smartNodeMajorFirmwareVersion + "." + snRebootIndicationMsgs.smartNodeMinorFirmwareVersion;
		CCUUtils.writeFirmwareVersion(firmwareVersion, address, false);
			String str = "addr:"+address+ " Node status: ";
			str+= ", master_fw_ver:" + firmwareVersion;
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
			try {
				str += ", device type:" + snRebootIndicationMsgs.smartNodeDeviceType.get().name();
				str += ", device:" + snRebootIndicationMsgs.smartNodeDeviceId;
				str += ", serialnumber:" + snRebootIndicationMsgs.smartNodeSerialNumber;
				CcuLog.i(L.TAG_CCU_DEVICE, "Reboot Alert: "+str);

				AlertGenerateHandler.handleDeviceMessage(DEVICE_REBOOT,"Device reboot info - "+str,
						CCUHsApi.getInstance().readId("device and addr == \""+address+"\""));
			}catch (Exception e){
				CcuLog.e(L.TAG_CCU_DEVICE, "error", e);
			}
	}
	public static void updateSetTempFromSmartNode(CmToCcuOverUsbSnLocalControlsOverrideMessage_t setTempUpdate){
		short nodeAddr = (short)setTempUpdate.smartNodeAddress.get();
		double temp = setTempUpdate.setTemperature.get();
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && device.size() > 0)
		{

			ArrayList<HashMap> phyPoints = hayStack.readAll("point and physical and deviceRef == \"" + device.get("id") + "\"");

			for(HashMap phyPoint : phyPoints) {
				if (phyPoint.isEmpty()) continue;
				hayStack.writeHisValById(phyPoint.get("id").toString(), temp);

				HashMap logPoint = hayStack.read("point and id=="+phyPoint.get("pointRef"));
				if (isDesiredTempPhyPoint(phyPoint)) {//Compare with what was sent out.
					double curValue = LSmartNode.getDesiredTemp(nodeAddr);//hayStack.readHisValById(phyPoint.get("id").toString());
					double desiredTemp = getDesredTempConversion(temp);
					CcuLog.d(L.TAG_CCU_DEVICE, "updateSetTempFromDevice : desiredTemp " + desiredTemp + "," + curValue);
					if (desiredTemp > 0 && (curValue != desiredTemp)) {
						hayStack.writeHisValById(logPoint.get("id").toString(), desiredTemp);
						updateDesiredTemp(nodeAddr, desiredTemp);
						CcuLog.d(L.TAG_CCU_DEVICE,
								"updateSetTempFromSmartNode : desiredTemp updated" + curValue + "->" + desiredTemp);
					}
				}
			}
		}
	}

	public static boolean isDesiredTempPhyPoint(HashMap<Object, Object> p) {
		if (p.containsKey("domainName")) {
			return p.get("domainName").toString().equals((DomainName.desiredTemp));
		}

		if (p.containsKey("port")) {
			return Port.valueOf(p.get("port").toString()) == Port.DESIRED_TEMP;
		}

		return false;
	}

	public static void updateSetTempFromBacnet(short nodeAddr, double temp, String coolheat){

		HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+nodeAddr+"\"");
		Equip equip = new Equip.Builder().setHashMap(equipMap).build();
		double heatDB = TunerUtil.getZoneHeatingDeadband(equip.getRoomRef());
		double coolDB = TunerUtil.getZoneCoolingDeadband(equip.getRoomRef());
		double updatedHeatingDt = 0;
		double updatedCoolingDt = 0;
		HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and cooling and sp and equipRef == \"" + equip.getId() + "\"");
		if (coolingDtPoint == null || coolingDtPoint.isEmpty()) {
			throw new IllegalArgumentException();
		}
		double coolingDesiredTemp = CCUHsApi.getInstance().readPointPriorityVal(coolingDtPoint.get("id").toString());
		Point coolingPt = new Point.Builder().setHashMap(coolingDtPoint).build();

		HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and desired and heating and sp and equipRef == \""+equip.getId()+"\"");
		if (heatinDtPoint == null || heatinDtPoint.isEmpty()) {
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
			SystemScheduleUtil.handleManualDesiredTempUpdate(coolingPt,heatingPt,null,updatedCoolingDt,
					updatedHeatingDt, 0, WhoFiledConstants.BACNET_WHO);

	}
	public static void updateSetTempFromSmartStat(CmToCcuOverUsbSmartStatLocalControlsOverrideMessage_t setTempUpdate){
		short nodeAddr = (short)setTempUpdate.smartNodeAddress.get();
		double temp = setTempUpdate.setTemperature.get();
		SmartStatFanSpeed_t fanSpeed = setTempUpdate.fanSpeed.get();
		CCUHsApi hayStack = CCUHsApi.getInstance();
		HashMap device = hayStack.read("device and addr == \""+nodeAddr+"\"");
		if (device != null && !device.isEmpty())
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
						double curValue = LSmartStat.getDesiredTemp(nodeAddr);
						double desiredTemp = getDesredTempConversion(temp);
						
						boolean validDesiredTemp = DeviceUtil.validateDesiredTempUserLimits(nodeAddr, desiredTemp);
						
						if (desiredTemp > 0 && (curValue != desiredTemp)) {
							hayStack.writeHisValById(logPoint.get("id").toString(), desiredTemp);
							updateSmartStatDesiredTemp(nodeAddr, desiredTemp, true);
							CcuLog.d(L.TAG_CCU_DEVICE,
							         "updateSetTempFromSmartStat : desiredTemp updated" +curValue+"->"+ desiredTemp);
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
					CCUHsApi.getInstance().writePoint(fanOpModePoint.get("id").toString(), TunerConstants.UI_DEFAULT_VAL_LEVEL, WhoFiledConstants.SMARTSTAT_WHO,
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

	//Occupancy has a physical and logical points, which are COV based. In addition to that an occupancyDetection
	//point is used to track occupancy events without COV filtering.
	private static void updateOTNOccupancyStatus(RawPoint sp, double val, Device device){
		if(val == 0 || val == 1){
			if((val == 1) ) {
				HashMap<Object, Object> occDetPoint = CCUHsApi.getInstance().readEntity("point and occupancy and " +
						"detection and his and equipRef==" +
						" \"" + device.getEquipRef() + "\"");
				if (!occDetPoint.isEmpty()){
					CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(occDetPoint.get("id").toString(),val);
				}
			}
			CCUHsApi.getInstance().writeHisValById(sp.getId(), val);
			CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), val);
		}
	}


	private static void updateOccupancyStatus(RawPoint sp, double val,Device device, short addr){
		if(val == 0 || val == 1) {
			CcuLog.i(L.TAG_CCU_SCHEDULER, " updateOccupancyStatus for " + device.getAddr() + " : " + val);
			double occuEnabled = CCUHsApi.getInstance().readDefaultVal("point and zone and config and standalone and enable and occupancy and group == \"" + addr + "\"");
			if (occuEnabled > 0 && val > 0) { //only if occupancy enabled
				HashMap occDetPoint = CCUHsApi.getInstance().read("point and occupancy and detection and his and equipRef== \"" + device.getEquipRef() + "\"");
				if ((occDetPoint != null) && (occDetPoint.size() > 0))
					CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(occDetPoint.get("id").toString(), val);
			}
			CCUHsApi.getInstance().writeHisValById(sp.getId(), val);
			CCUHsApi.getInstance().writeHisValById(sp.getPointRef(), val);
		}
	}

	public static void setCurrentTempInterface(ZoneDataInterface in) { currentTempInterface = in; }

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
				 String ccuName = Domain.ccuDevice.getCcuDisName();
				 AlertGenerateHandler.handleDeviceMessage(DEVICE_DEAD, "For"+" "+ccuName + "," +d.getDisplayName() +" has " +
						 "stopped reporting data. "+CCUUtils.getSupportMsgContent(Globals.getInstance().getApplicationContext()), d.getId());
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
				CcuLog.d("CCU_SN_MESSAGES","=================NOW SENDING SN SEEDS====================="+zone.getDisplayName()+","+addr);
				CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage = LSmartNode.getSeedMessage(zone, Short.parseShort(d.getAddr()),d.getEquipRef(),snprofile);
				tempLogdStructAsJson(seedMessage);
				MeshUtil.sendStructToCM(seedMessage);
				CcuLog.d("CCU_SN_MESSAGES","=================NOW SENDING SN SETTINGS2====================="+zone.getDisplayName()+","+addr);
				CcuToCmOverUsbSnSettings2Message_t settings2Message = LSmartNode.getSettings2Message(zone, Short.parseShort(d.getAddr()), d.getEquipRef(), snprofile);
				tempLogdStructAsJson(settings2Message);
				MeshUtil.sendStructToCM(settings2Message);
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
	static Port getPhysicalPointPort(Map<Object, Object> physicalPoint) {
		CcuLog.d(L.TAG_CCU_DEVICE, "getPhysicalPort "+physicalPoint);
		if (physicalPoint.get("domainName") != null) {
			return DeviceUtil.getPortFromDomainName(physicalPoint.get("domainName").toString());
		}
		return Port.valueOf(physicalPoint.get("port").toString());
	}

	public static void updateRssiPointIfAvailable(CCUHsApi hayStack, String deviceRef, int rssi, int heartbeat, int nodeAddr) {
		HashMap<Object, Object> rssiMap = hayStack.readEntity("point and physical and sensor and deviceRef== \""+ deviceRef + "\" and (port==\"RSSI\" or domainName==\"rssi\")");
		if(!rssiMap.isEmpty() && rssiMap.get("pointRef")!=null && !rssiMap.get("pointRef").toString().isEmpty()) {
			hayStack.writeHisValueByIdWithoutCOV(rssiMap.get("id").toString(), (double)rssi);
			hayStack.writeHisValueByIdWithoutCOV(rssiMap.get("pointRef").toString(), (double)heartbeat);
			if(currentTempInterface != null) {
				currentTempInterface.refreshHeartBeatStatus(String.valueOf(nodeAddr));
			}
		}
	}
}
