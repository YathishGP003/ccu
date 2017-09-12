package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.serial.MessageConstants;
/**
 * Created by Yinten on 9/12/2017.
 */

/***
 * A circuit is an IO from a device like the smart node. Extensions of this class are
 * SmartNodeInput and SmartNodeOutput.   SmartNodeInput and SmartNodeOutput should be renamed to
 * CircuitInput and CircuitOutput.  This maintains the logical to physical mapping of a circuit IO.
 */
public abstract class Circuit
{
	public int   mVal;
	public Port  mSmartNodePort;
	public short mSmartNodeAddress;
	public  String mName = "";
	@JsonIgnore
	public boolean mConfigured;
	private UUID   uuid  = UUID.randomUUID();
	
	
	public Output getOutput()
	{
		switch (mSmartNodePort)
		{
			case RELAY_ONE:
				return Output.Relay;
			case RELAY_TWO:
				return Output.Relay;
			case ANALOG_OUT_ONE:
				return Output.Analog;
			case ANALOG_OUT_TWO:
			case ANALOG_IN_ONE:
			case ANALOG_IN_TWO:
			default:
				return Output.Analog;
		}
	}
	
	
	/***
	 *  This is used when settings a smart node circuit name, when setting a smart node light
	 *  controls message.
	 * @return name when communicating via serial to Smart Node
	 */
	@JsonIgnore
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
	
	
	public UUID getUuid()
	{
		return uuid;
	}
}
