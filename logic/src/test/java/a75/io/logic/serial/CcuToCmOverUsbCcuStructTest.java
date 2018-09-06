package a75.io.logic.serial;

import junit.framework.Assert;
import org.junit.Test;

import java.io.IOException;

import a75f.io.device.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnSettingsMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbErrorReportMessage_t;
import a75f.io.device.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;
import a75f.io.device.serial.ErrorType;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartNodeLightingCircuit_t;
import a75f.io.device.serial.SnRebootIndicationMessage_t;

import static a75f.io.device.json.serializers.JsonSerializer.fromJson;
import static a75f.io.device.json.serializers.JsonSerializer.toJson;
import static a75f.io.device.serial.FirmwareDeviceType_t.SMART_NODE_DEVICE_TYPE;

/**
 * Created by samjithsadasivan isOn 9/5/17.
 */
public class CcuToCmOverUsbCcuStructTest
{
	@Test
	public void testCcuToCmOverUsbCcuHeartbeatMessage_t() {
		CcuToCmOverUsbCcuHeartbeatMessage_t hbMessage = new CcuToCmOverUsbCcuHeartbeatMessage_t();
		hbMessage.messageType.set(MessageType.CCU_HEARTBEAT_UPDATE);
		hbMessage.interval.set((short) 0x5);
		hbMessage.multiplier.set((short) 0x1);
		try
		{
			byte[] buffer = hbMessage.getOrderedBuffer();
			Assert.assertEquals(MessageType.CCU_HEARTBEAT_UPDATE.ordinal(), buffer[0]);
			Assert.assertEquals(5, buffer[1]);
			Assert.assertEquals(1, buffer[2]);
			String hbString = toJson(hbMessage, true);
			System.out.println(hbMessage);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	
	@Test
	public void testCmToCcuOverUsbSnRegularUpdateMessage_t() {
		CmToCcuOverUsbSnRegularUpdateMessage_t snUpdate = new CmToCcuOverUsbSnRegularUpdateMessage_t();
		snUpdate.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SMART_STAT);
		snUpdate.cmLqi.set((short)100);
		snUpdate.cmRssi.set((byte)-10);
		snUpdate.update.roomTemperature.set(75);
		
		try
		{
			String updateMsg = toJson(snUpdate, true);
			CmToCcuOverUsbSnRegularUpdateMessage_t desMsg = (CmToCcuOverUsbSnRegularUpdateMessage_t)fromJson(updateMsg ,CmToCcuOverUsbSnRegularUpdateMessage_t.class);
			
			Assert.assertEquals(desMsg.messageType.get(), MessageType.CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE);
			Assert.assertEquals(desMsg.cmLqi, 100);
			Assert.assertEquals(desMsg.cmRssi, -10);
			Assert.assertEquals(desMsg.update.roomTemperature ,75);
			
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
	
	@Test
	public void testCcuToCmOverUsbSnLightingScheduleMessage_t () {
		CcuToCmOverUsbSnLightingScheduleMessage_t lightSchedule = new CcuToCmOverUsbSnLightingScheduleMessage_t();
		lightSchedule.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE);
		lightSchedule.smartNodeAddress.set((short)7000);
		
		lightSchedule.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_DIGITAL_1);
		
		try {
			byte[] buffer = lightSchedule.getOrderedBuffer();
			Assert.assertEquals((byte)MessageType.CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE.ordinal(), buffer[0]);
			Assert.assertEquals((byte)0x58, buffer[1]);
			Assert.assertEquals((byte)0x1B, buffer[2]);
			String lights = toJson(lightSchedule, true);
			
			System.out.println(lightSchedule);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}
	
	@Test
	public void testCcuToCmOverUsbDatabaseSeedSnMessage_t() {
		CcuToCmOverUsbDatabaseSeedSnMessage_t seedMsg = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		seedMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		seedMsg.smartNodeAddress.set((short)7000);
		
		try
		{
			byte[] buffer = seedMsg.getOrderedBuffer();
			Assert.assertEquals(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN.ordinal(), buffer[0]);
			Assert.assertEquals((byte)0x58, buffer[1]);
			Assert.assertEquals((byte)0x1B, buffer[2]);
			String seed = toJson(seedMsg, true);
			System.out.println(seed);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}
	
	
	@Test
	public void testCcuToCmOverUsbSnControlsMessage_t() {
		CcuToCmOverUsbSnControlsMessage_t controlMsg = new CcuToCmOverUsbSnControlsMessage_t();
		controlMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
		controlMsg.smartNodeAddress.set((short)7000);
		controlMsg.controls.setTemperature.set((short) 73);
		
		try
		{
			byte[] buffer = controlMsg.getOrderedBuffer();
			Assert.assertEquals((byte)MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS.ordinal(), buffer[0]);
			Assert.assertEquals((byte)0x58, buffer[1]);
			Assert.assertEquals((byte)0x1B, buffer[2]);
			String control = toJson(controlMsg, true);
			
			System.out.println(control);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}
	
	@Test
	public void testCcuToCmOverUsbSnSettingsMessage_t() {
		CcuToCmOverUsbSnSettingsMessage_t settingMsg = new CcuToCmOverUsbSnSettingsMessage_t();
		settingMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS);
		settingMsg.smartNodeAddress.set((short)7000);
		settingMsg.settings.profileBitmap.lightingControl.set((short)1);
		settingMsg.settings.roomName.set("75FRoom");
		
		try
		{
			byte[] buffer = settingMsg.getOrderedBuffer();
			Assert.assertEquals((byte)MessageType.CCU_TO_CM_OVER_USB_SN_SETTINGS.ordinal(), buffer[0]);
			Assert.assertEquals((byte)0x58, buffer[1]);
			Assert.assertEquals((byte)0x1B, buffer[2]);
			
			String setting = toJson(settingMsg, true);
			
			CcuToCmOverUsbSnSettingsMessage_t deSettingMsg = (CcuToCmOverUsbSnSettingsMessage_t) fromJson(setting ,CcuToCmOverUsbSnSettingsMessage_t.class);
			Assert.assertEquals(deSettingMsg.settings.profileBitmap.lightingControl.get() ,1);
			Assert.assertEquals(deSettingMsg.settings.profileBitmap.dynamicAirflowBalancing.get(), 0);
			Assert.assertEquals(deSettingMsg.settings.roomName.get(), "75FRoom");
			
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	
	@Test
	public void testSnRebootIndicationMessage_t() {
		SnRebootIndicationMessage_t rebootMsg = new SnRebootIndicationMessage_t();
		rebootMsg.messageType.set(MessageType.CM_TO_CCU_OVER_USB_SN_REBOOT);
		rebootMsg.smartNodeAddress.set((short)7000);
		rebootMsg.rebootCause.set((short)32);
		rebootMsg.smartNodeDeviceType.set(SMART_NODE_DEVICE_TYPE);
		rebootMsg.smartNodMajoreHardwareVersion.set((short)10);
		
		try {
			String err = toJson(rebootMsg, true);
			SnRebootIndicationMessage_t deRebootMsg = (SnRebootIndicationMessage_t) fromJson(err,
					SnRebootIndicationMessage_t.class);
			Assert.assertEquals(deRebootMsg.messageType, MessageType.CM_TO_CCU_OVER_USB_SN_REBOOT);
			Assert.assertEquals(deRebootMsg.address(),7000);
			Assert.assertEquals(deRebootMsg.rebootCause.get(), 32);
			Assert.assertEquals(deRebootMsg.smartNodeDeviceType.get(), SMART_NODE_DEVICE_TYPE);
			Assert.assertEquals(deRebootMsg.smartNodMajoreHardwareVersion.get(), 10);
			
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
	}
	
	@Test
	public void testCmToCcuOverUsbErrorReportMessage_t() {
		CmToCcuOverUsbErrorReportMessage_t errMsg = new CmToCcuOverUsbErrorReportMessage_t();
		errMsg.messageType.set(MessageType.CM_ERROR_REPORT);
		errMsg.errorType.set(ErrorType.BAD_PACKET_FRAMING);
		
		try {
			String err = toJson(errMsg, true);
			CmToCcuOverUsbErrorReportMessage_t desErr = (CmToCcuOverUsbErrorReportMessage_t) fromJson(err,
					CmToCcuOverUsbErrorReportMessage_t.class);
			Assert.assertEquals(desErr.messageType, MessageType.CM_ERROR_REPORT);
			Assert.assertEquals(desErr.errorType,ErrorType.BAD_PACKET_FRAMING);
			
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
	}
}