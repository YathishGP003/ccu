package a75f.io.bo.building;

import org.javolution.io.Struct;

import java.util.ArrayList;

import a75f.io.bo.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartNodeLightingCircuit_t;

/**
 * Created by samjithsadasivan on 9/7/17.
 */

public class LightingSchedule
{
	LightingSchedule(String name, LightSmartNodeOutput op) {
		this.name = name;
		this.snOutput = op;
	}
	public String name;
	
	public SmartNodeOutput snOutput;
	
	boolean normallyOpen;
	
	public ArrayList<LightSchedule> lightSchedules = new ArrayList<>();
	
	public CcuToCmOverUsbSnLightingScheduleMessage_t getScheduleMessage() {
		
		CcuToCmOverUsbSnLightingScheduleMessage_t scheduleMsg = new CcuToCmOverUsbSnLightingScheduleMessage_t();
		scheduleMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE);
		scheduleMsg.smartNodeAddress.set(snOutput.mSmartNodeAddress);
		scheduleMsg.lightingSchedule.logicalName.set(name);
		scheduleMsg.lightingSchedule.numEntries.set((short) lightSchedules.size());
		
		for (int index = 0; index < lightSchedules.size(); index++)
		{
			scheduleMsg.lightingSchedule.entries[index].startTime.set((short)lightSchedules.get(index).startTime);
			scheduleMsg.lightingSchedule.entries[index].stopTime.set((short)lightSchedules.get(index).endTime);
			scheduleMsg.lightingSchedule.entries[index].intensityPercent.set((short)lightSchedules.get(index).intensityVal);
			scheduleMsg.lightingSchedule.entries[index].applicableDaysOfTheWeek.bitmap.set((short)lightSchedules.get(index).getScheduleDaysBitmap());
		}
		
		switch(snOutput.mSmartNodePort) {
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
