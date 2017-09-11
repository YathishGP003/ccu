package a75f.io.bo.building;

import java.util.UUID;

import a75f.io.bo.building.definitions.Input;
import a75f.io.bo.building.definitions.InputActuatorType;
import a75f.io.bo.building.definitions.Port;

/**
 * Created by Yinten isOn 8/15/2017.
 */

public class SmartNodeInput
{
	public Port              mSmartNodePort;
	public UUID              mUniqueID;
	public short             mSmartNodeAddress;
	public String            mName;
	public Input             mInput;
	public InputActuatorType mInputActuatorType;
}
