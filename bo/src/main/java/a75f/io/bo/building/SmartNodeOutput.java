package a75f.io.bo.building;

import java.util.UUID;

import a75f.io.bo.building.definitions.Output;
import a75f.io.bo.building.definitions.OutputAnalogActuatorType;
import a75f.io.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.bo.building.definitions.Port;

/**
 * Created by Yinten on 8/15/2017.
 */

public class SmartNodeOutput
{
	
	public Port                     mSmartNodePort;
	public short                    mSmartNodeAddress;
	public UUID                     mUniqueID;
	public String                   mName;
	public Output                   mOutput;
	public OutputRelayActuatorType  mOutputRelayActuatorType;
	public OutputAnalogActuatorType mOutputAnalogActuatorType;
}
