package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.MessageType;


@JsonInclude(JsonInclude.Include.NON_NULL)
public class LightProfile extends ZoneProfile
{
	public boolean on = true;
	public short mDimmablePercent;
	
	public LightProfile()
	{
	}
	
	@JsonIgnore
	public void on(boolean on)
	{
		this.on = on;
	}
	/*****************
	 *ONLY USED FOR CIRCUITS BELOW
	 ******************/
	
	/***
	 * A profile can have many smart nodes attached to it.   It has to formulate many controls
	 * messages in case there are multiple SmartNodes controlling a single light profile.
	 * @return List<CcuToCmOverUsbSnControlsMessages_t> </CcuToCmOverUsbSnControlsMessages_t>
	 */
	@JsonIgnore
	public List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage()
	{
		HashMap<Short, CcuToCmOverUsbSnControlsMessage_t> controlsMessages =
				new HashMap<Short, CcuToCmOverUsbSnControlsMessage_t>();
		for (SmartNodeOutput smartNodeOutput : this.smartNodeOutputs)
		{
			CcuToCmOverUsbSnControlsMessage_t controlsMessage_t = null;
			if (controlsMessages.containsKey(smartNodeOutput.mSmartNodeAddress))
			{
				controlsMessage_t = controlsMessages.get(smartNodeOutput.mSmartNodeAddress);
			}
			else
			{
				controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
				controlsMessages.put(smartNodeOutput.mSmartNodeAddress, controlsMessage_t);
				controlsMessage_t.smartNodeAddress.set(smartNodeOutput.mSmartNodeAddress);
				controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
			}
			Struct.Unsigned8 port = getPort(controlsMessage_t, smartNodeOutput.mSmartNodePort);
			int localDimmablePercent = 100;
			boolean localOn = true;
			if (smartNodeOutput.isOverride())
			{
				localDimmablePercent = smartNodeOutput.mVal;
				localOn = smartNodeOutput.mVal != 0;
				//USE values for smartnodeoutput rather than smartNodeProfile
			}
			else
			{
				if (smartNodeOutput.hasSchedules())
				{
					localDimmablePercent = smartNodeOutput.getScheduledVal();
					localOn = localDimmablePercent != 0;
				}
				else if (this.hasSchedules())
				{
					localDimmablePercent = this.getScheduledVal();
					localOn = localDimmablePercent != 0;
				}
				else
				{
					localOn = on;
					localDimmablePercent = mDimmablePercent;
				}
			}
			switch (smartNodeOutput.getOutput())
			{
				case Relay:
					switch (smartNodeOutput.mOutputRelayActuatorType)
					{
						case NormallyClose:
							if (localOn)
							{
								port.set((short) 0);
							}
							else
							{
								port.set((short) 1);
							}
							break;
						///Defaults to normally open
						default:
							if (localOn)
							{
								port.set((short) 1);
							}
							else
							{
								port.set((short) 0);
							}
							break;
					}
					break;
				case Analog:
					switch (smartNodeOutput.mOutputAnalogActuatorType)
					{
						case ZeroToTenV:
							if (localOn)
							{
								port.set(getDimmable(localDimmablePercent, 100));
							}
							else
							{
								port.set(getDimmable(localDimmablePercent, 0));
							}
							break;
						case TenToZeroV:
							if (localOn)
							{
								port.set(getDimmable(localDimmablePercent, 0));
							}
							else
							{
								port.set(getDimmable(localDimmablePercent, 100));
							}
							break;
						case TwoToTenV:
							if (localOn)
							{
								port.set(getDimmable(localDimmablePercent, 100));
							}
							else
							{
								port.set(getDimmable(localDimmablePercent, 20));
							}
							break;
						case TenToTwov:
							if (localOn)
							{
								port.set(getDimmable(localDimmablePercent, 20));
							}
							else
							{
								port.set(getDimmable(localDimmablePercent, 100));
							}
							break;
					}
					break;
			}
		}
		return new ArrayList<>(controlsMessages.values());
	}
	
	@JsonIgnore
	private Struct.Unsigned8 getPort(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t,
	                                 Port smartNodePort)
	{
		Struct.Unsigned8 retVal = null;
		switch (smartNodePort)
		{
			case ANALOG_OUT_ONE:
				retVal = controlsMessage_t.controls.analogOut1;
				break;
			case ANALOG_OUT_TWO:
				retVal = controlsMessage_t.controls.analogOut2;
				break;
			case RELAY_ONE:
				retVal = controlsMessage_t.controls.digitalOut1;
				break;
			case RELAY_TWO:
				retVal = controlsMessage_t.controls.digitalOut2;
				break;
		}
		return retVal;
	}
	
	@JsonIgnore
	public int getScheduledVal()
	{
		for (Schedule schedule : mSchedules)
		{
			if (schedule.isInSchedule())
			{
				return schedule.getVal();
			}
		}
		return 0;
	}
	
	@JsonIgnore
	private static short getDimmable(int localDimmablePercent, int analogVoltage)
	{
		return (short) (((float) localDimmablePercent * (float) analogVoltage) / 100.0f);
	}
	
	
}
