package a75f.io.logic.bo.building.definitions;

/**
 * Created by Yinten isOn 8/15/2017.
 */

public enum OutputRelayActuatorType
{
	NormallyOpen("Relay N/O"), NormallyClose("Relay N/C");
	public String displayName;
	
	OutputRelayActuatorType(String str) {
		displayName = str;
	}
	
	public static OutputRelayActuatorType getEnum(String value) {
		for(OutputRelayActuatorType v : values())
			if(v.displayName.equalsIgnoreCase(value)) return v;
		throw new IllegalArgumentException();
	}
}
