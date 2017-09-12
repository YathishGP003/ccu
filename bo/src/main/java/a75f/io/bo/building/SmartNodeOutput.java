package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartNodeLightingCircuit_t;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmartNodeOutput extends Circuit
{
	
	
	
	/* These are particular to 'outputs' when dealing with 'circuits'  */;
	public OutputRelayActuatorType  mOutputRelayActuatorType;
	public OutputAnalogActuatorType mOutputAnalogActuatorType;
	public ArrayList<Schedule>      mSchedules = new ArrayList<Schedule>();
	
	private boolean mOverride = false; //This circuit controls it's own value
	
	
	/***
	 * This circuit is in manual control mode, zone profile and schedules will be ignored.
	 * @return isInOverRide
	 */
	public boolean isOverride()
	{
		return mOverride;
	}
	
	
	public void setOverride(boolean override)
	{
		this.mOverride = override;
	}
	
	
	public boolean hasSchedules()
	{
		if (mSchedules != null && mSchedules.size() > 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	@JsonIgnore
	public boolean isRelayOn()
	{
		return mVal == 100;
	}
	
	

	
	@JsonIgnore
	public void turnOn(boolean on)
	{
		if (getOutput() == Output.Relay)
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
	}
	
	
	/*****************
	 *ONLY USED FOR CIRCUITS.
	 ******************/
	//TODO: make sure to get sunrise data.  If the schedule is set to sunrise periodically update
	// the sunrise to the weather channel's sunrise on the smart node.
	//String starttime = start.compareTo("sr") == 0 ? WeatherDataDownloadService.getSunriseTime() : start.compareTo("ss") == 0 ? WeatherDataDownloadService.getSunsetTime() : start;
	//String endtime = end.compareTo("sr") == 0 ? WeatherDataDownloadService.getSunriseTime() :
	// end.compareTo("ss")==0?WeatherDataDownloadService.getSunsetTime():end;
	@JsonIgnore
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
	
	

	@JsonIgnore
	public int getScheduledVal()
	{
		for(Schedule schedule : mSchedules)
		{
			if(schedule.isInSchedule())
			{
				return schedule.getVal();
			}
		}
		return 0;
	}
	
	 @JsonIgnore
	public boolean isOn()
	{
		boolean retVal = false;
		
		if (mVal == 100)
			retVal = true;
		
		else
			retVal = false;
			
		if(mOutputRelayActuatorType == OutputRelayActuatorType.NormallyClose)
			return retVal;
		else
			return !retVal;
				
	}
	
	
	@JsonIgnore
	@Override
	public String toString()
	{
		return "SmartNodeOutput{" + "mVal=" + mVal + ", mSmartNodePort=" + mSmartNodePort +
		       ", mSmartNodeAddress=" + mSmartNodeAddress + ", mUniqueID=" + getUuid() +
		       ", mName='" + mName + '\'' + ", mConfigured=" + mConfigured +
		       ", mOutputRelayActuatorType=" + mOutputRelayActuatorType +
		       ", mOutputAnalogActuatorType=" + mOutputAnalogActuatorType + ", mSchedules=" +
		       mSchedules + ", mOverride=" + mOverride + '}';
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
