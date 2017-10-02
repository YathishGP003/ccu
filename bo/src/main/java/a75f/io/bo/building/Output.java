package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonInclude;

import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Output extends Circuit
{

	/* These are particular to 'outputs' when dealing with 'circuits'  */;
    public OutputRelayActuatorType  mOutputRelayActuatorType;
    public OutputAnalogActuatorType mOutputAnalogActuatorType;
    
    public short mapDigital(boolean digital)
    {
        switch (getOutputType())
        {
            case Relay:
                switch (mOutputRelayActuatorType)
                {
                    case NormallyClose:
                        return (short) (digital ? 0 : 1);
                    ///Defaults to normally open
                    case NormallyOpen:
                        return (short) (digital ? 1 : 0);
                }
                break;
        }
        return 0;
    }
    
    
    public short mapAnalog(short raw)
    {
        switch (getOutputType())
        {
            case Analog:
                switch (mOutputAnalogActuatorType)
                {
                    case ZeroToTenV:
                        return raw;
                    case TenToZeroV:
                        return (short) (100 - raw);
                    case TwoToTenV:
                        return (short) (20 + scaleAnalog(raw, 80));
                    case TenToTwov:
                        return (short) (100 - scaleAnalog(raw, 80));
                }
                break;
        }
        return (short) 0;
    }
    
    
    protected static short scaleAnalog(short analog, int scale)
    {
        return (short) ((float) scale * ((float) analog / 100.0f));
    }
    
    /*****************
     *ONLY USED FOR CIRCUITS.
     ******************/
    //TODO: make sure to get sunrise data.  If the schedule is set to sunrise periodically update
    // the sunrise to the weather channel's sunrise on the smart node.
    //String starttime = start.compareTo("sr") == 0 ? WeatherDataDownloadService.getSunriseTime() : start.compareTo("ss") == 0 ? WeatherDataDownloadService.getSunsetTime() : start;
    //String endtime = end.compareTo("sr") == 0 ? WeatherDataDownloadService.getSunriseTime() :
    // end.compareTo("ss")==0?WeatherDataDownloadService.getSunsetTime():end;
    //	@JsonIgnore
    //	public CcuToCmOverUsbSnLightingScheduleMessage_t getScheduleMessage()
    //	{
    //		CcuToCmOverUsbSnLightingScheduleMessage_t scheduleMsg =
    //				new CcuToCmOverUsbSnLightingScheduleMessage_t();
    //		scheduleMsg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE);
    //		scheduleMsg.smartNodeAddress.set(mSmartNodeAddress);
    //		scheduleMsg.lightingSchedule.logicalName.set(getCircuitName());
    //		scheduleMsg.lightingSchedule.numEntries.set((short) mSchedules.size());
    //		for (int index = 0; index < mSchedules.size(); index++)
    //		{
    //			try
    //			{
    //				//TODO: review
    //				//scheduleMsg.lightingSchedule.entries[index].startTime
    //				//		.set((short) mSchedules.get(index).getStAsShort());
    //
    //				//TODO: review
    //				//scheduleMsg.lightingSchedule.entries[index].stopTime
    //				//		.set((short) mSchedules.get(index).getEtAsShort());
    //				scheduleMsg.lightingSchedule.entries[index].intensityPercent
    //						.set((short) mSchedules.get(index).getVal());
    //				scheduleMsg.lightingSchedule.entries[index].applicableDaysOfTheWeek.bitmap
    //						.set((short) mSchedules.get(index).getScheduleDaysBitmap());
    //			}
    //			catch (Exception e)
    //			{
    //				//TODO: revisit
    //				e.printStackTrace();
    //			}
    //		}
    //		switch (mSmartNodePort)
    //		{
    //			case RELAY_ONE:
    //				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_DIGITAL_1);
    //				break;
    //			case RELAY_TWO:
    //				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_DIGITAL_2);
    //				break;
    //			case ANALOG_IN_ONE:
    //				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_ANALOG_1);
    //				break;
    //			case ANALOG_IN_TWO:
    //				scheduleMsg.circuit.set(SmartNodeLightingCircuit_t.LIGHTING_CIRCUIT_ANALOG_2);
    //				break;
    //		}
    //		return scheduleMsg;
    //	}

    
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
