package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.javolution.io.Struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * Created by Yinten on 8/15/2017.
 */

public class LightProfile extends ZoneProfile
{
	public boolean on              = true;
	public boolean dimmable        = true;
	public short   dimmablePercent = 100;
	public ArrayList<LightSmartNodeOutput> smartNodeOutputs = new ArrayList<>();
	public LightProfile(){
		
	}
	
	public LightProfile(String name)
	{
		super(name);
	}
	
	
	public void on(boolean on)
	{
		this.on = on;
	}
	
	
	/***
	 * A profile can have many smart nodes attached to it.   It has to formulate many controls
	 * messages in case there are multiple SmartNodes controlling a single light profile.
	 * @return List<CcuToCmOverUsbSnControlsMessages_t> </CcuToCmOverUsbSnControlsMessages_t>
	 */
	@JsonIgnore
	public List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage()
	{
		ensureDimmable();
		HashMap<Short, CcuToCmOverUsbSnControlsMessage_t> controlsMessages =
				new HashMap<Short, CcuToCmOverUsbSnControlsMessage_t>();
		
		boolean override = overrideEnabled(); //Used to ignore profile settings while sending override control message.
		
		
		for (LightSmartNodeOutput smartNodeOutput : this.smartNodeOutputs)
		{
			
			CcuToCmOverUsbSnControlsMessage_t controlsMessage_t = null;
			if (controlsMessages.containsKey(smartNodeOutput.mSmartNodeAddress))
			{
				controlsMessage_t = controlsMessages.get(smartNodeOutput.mSmartNodeAddress);
			}
			else
			{
				controlsMessage_t = new CcuToCmOverUsbSnControlsMessage_t();
				controlsMessages
						                    .put(smartNodeOutput.mSmartNodeAddress, controlsMessage_t);
				controlsMessage_t.smartNodeAddress.set(smartNodeOutput.mSmartNodeAddress);
				controlsMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_CONTROLS);
			}
			
			
			Struct.Unsigned8 port = getPort(controlsMessage_t, smartNodeOutput.mSmartNodePort);
			
			short localDimmablePercent = 100;
			boolean localOn = false;
			if(override)
			{
				localDimmablePercent = smartNodeOutput.dimmable;
				localOn = smartNodeOutput.on;
				//USE values for smartnodeoutput rather than smartNodeProfile
			}
			else
			{
				localDimmablePercent = this.dimmablePercent;
				localOn = this.on;
			}
			
			switch (smartNodeOutput.mOutput)
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
								port.set(getDimmable(localDimmablePercent,20));
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
	
	
	private boolean overrideEnabled() {
		for (LightSmartNodeOutput op : this.smartNodeOutputs) {
			if (op.override) {
				return true;
			}
			
		}
		return false;
	}
	
	private void ensureDimmable()
	{
		if (dimmable == false)
		{
			dimmablePercent = 100;
		}
	}
	
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
	
	
	private static short getDimmable(short localDimmablePercent, int analogVoltage)
	{
		return (short) ((localDimmablePercent * analogVoltage) / 100);
	}
}
