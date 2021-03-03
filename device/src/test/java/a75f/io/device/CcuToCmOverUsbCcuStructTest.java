package a75f.io.device;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;

import a75f.io.device.serial.CcuToCmOverUsbCcuHeartbeatMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.device.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.SmartNodeLightingCircuit_t;

import static a75f.io.device.json.serializers.JsonSerializer.toJson;

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
			// System.out.println(hbMessage);
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
			
			//	System.out.println(lightSchedule);
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
			// System.out.println(seed);
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
			
			// System.out.println(control);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		
		
	}
}