package a75f.io.bo.building;

import a75f.io.bo.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartNodeLightingCircuit_t;

/**
 * Created by samjithsadasivan on 8/29/17.
 */

public class LightSmartNodeOutput extends SmartNodeOutput
{
	
	public boolean override = false;
	public boolean on = false;
	public short dimmable = 100;
	
	public LightingSchedule schedule = null;
	
	public CcuToCmOverUsbSnLightingScheduleMessage_t getScheduleMessage() {
		
		if (schedule == null)
			return null;
		
		CcuToCmOverUsbSnLightingScheduleMessage_t scheduleMsg = new CcuToCmOverUsbSnLightingScheduleMessage_t();
		scheduleMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE);
		scheduleMsg.smartNodeAddress.set(mSmartNodeAddress);
		scheduleMsg.lightingSchedule.logicalName.set(schedule.name);
		scheduleMsg.lightingSchedule.numEntries.set((short) schedule.lightSchedules.size());
		
		for (int index = 0; index < schedule.lightSchedules.size(); index++)
		{
			scheduleMsg.lightingSchedule.entries[index].startTime.set((short)schedule.lightSchedules.get(index).startTime);
			scheduleMsg.lightingSchedule.entries[index].stopTime.set((short)schedule.lightSchedules.get(index).endTime);
			scheduleMsg.lightingSchedule.entries[index].intensityPercent.set((short)schedule.lightSchedules.get(index).intensityVal);
			scheduleMsg.lightingSchedule.entries[index].applicableDaysOfTheWeek.bitmap.set((short)schedule.lightSchedules.get(index).getScheduleDaysBitmap());
		}
		
		switch(this.mSmartNodePort) {
			case RELAY_ONE:
				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_DIGITAL_1);
				break;
			case RELAY_TWO:
				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_DIGITAL_2);
				break;
			case ANALOG_IN_ONE:
				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_ANALOG_1);
				break;
			case ANALOG_IN_TWO:
				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_ANALOG_2);
				break;
		}
		
		
		return scheduleMsg;
	}
	
}
