package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.UUID;

import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.bo.serial.MessageConstants;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartNodeLightingCircuit_t;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmartNodeOutput
{
	public boolean mOverride = false;
	public int                      mVal;
	public Port                     mSmartNodePort;
	public short                    mSmartNodeAddress;
	public UUID                     mUniqueID;
	public String                   mName;
	public Output                   mOutput;
	public OutputRelayActuatorType  mOutputRelayActuatorType;
	public OutputAnalogActuatorType mOutputAnalogActuatorType;
	public ArrayList<Schedule>      mSchedules;
	
	
	public boolean isOn()
	{
		return mVal == 100;
	}
	
	
	public void isOn(boolean on)
	{
		if (on)
		{
			mVal = 100;
		}
		else
		{
			mVal = 0;
		}
	}
	
	
	/*****************
	 *ONLY USED FOR CIRCUITS.
	 ******************/
	//TODO: make sure to get sunrise data.
	//String starttime = start.compareTo("sr") == 0 ? WeatherDataDownloadService.getSunriseTime() : start.compareTo("ss") == 0 ? WeatherDataDownloadService.getSunsetTime() : start;
	//String endtime = end.compareTo("sr") == 0 ? WeatherDataDownloadService.getSunriseTime() :
	// end.compareTo("ss")==0?WeatherDataDownloadService.getSunsetTime():end;
	public CcuToCmOverUsbSnLightingScheduleMessage_t getScheduleMessage()
	{
		CcuToCmOverUsbSnLightingScheduleMessage_t scheduleMsg =
				new CcuToCmOverUsbSnLightingScheduleMessage_t();
		scheduleMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE);
		scheduleMsg.smartNodeAddress.set(mSmartNodeAddress);
		scheduleMsg.lightingSchedule.logicalName.set(getCircuitName());
		scheduleMsg.lightingSchedule.numEntries.set((short) mSchedules.size());
		for (int index = 0; index < mSchedules.size(); index++)
		{
			try
			{
				scheduleMsg.lightingSchedule.entries[index].startTime
						.set((short) mSchedules.get(index).getStAsShort());
				scheduleMsg.lightingSchedule.entries[index].stopTime
						.set((short) mSchedules.get(index).getEtAsShort());
				scheduleMsg.lightingSchedule.entries[index].intensityPercent
						.set((short) mSchedules.get(index).getVal());
				scheduleMsg.lightingSchedule.entries[index].applicableDaysOfTheWeek.bitmap
						.set((short) mSchedules.get(index).getScheduleDaysBitmap());
			}
			catch (Exception e)
			{
				//TODO: revisit
				e.printStackTrace();
			}
		}
		switch (mSmartNodePort)
		{
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
	
	
	/***
	 *  This is used when settings a smart node circuit name, when setting a smart node light
	 *  controls message.
	 * @return name when communicating via serial to Smart Node
	 */
	public String getCircuitName()
	{
		if (mName.length() > MessageConstants.MAX_LIGHTING_CONTROL_CIRCUIT_LOGICAL_NAME_BYTES)
		{
			return mName.substring(0, 17) + "...";
		}
		else
		{
			return mName;
		}
	}
	
	
	/* HELP: understand this~
	public static int timeToVal(String str) {
    	int hr = Integer.parseInt(str.substring(0, 2));
    	int min = Integer.parseInt(str.substring(3,5));
    	if (str.contains("PM") && hr < 12)
    		return (48+(hr*4))+(min/15);
		else if(str.contains("AM") && hr == 12){
			return 0+(min / 15);
		} else {
			return (hr * 4) + (min / 15);
		}
    }
	 */
}
