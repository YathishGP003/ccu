package a75f.io.logic.bo.building.definitions;

/**
 * Created by Yinten isOn 8/15/2017.
 */

public enum OutputAnalogActuatorType
{
	ZeroToTenV("0-10v"), TwoToTenV("2-10v"), TenToTwov("10-2v"), TenToZeroV("10-0v"), Pulse ("Pulsed Electric"), MAT ("Smart Damper");//TODO- Revisit
	
	public String displayName;
	
	OutputAnalogActuatorType(String str) {
		displayName = str;
	}
	
	public static OutputAnalogActuatorType getEnum(String value) {
		for(OutputAnalogActuatorType v : values())
			if(v.displayName.equalsIgnoreCase(value)) return v;
		throw new IllegalArgumentException();
	}
}
