package a75f.io.bo.building.definitions;

/**
 * Created by Yinten isOn 8/15/2017.
 */

public enum OutputAnalogActuatorType
{
	ZeroToTenV("0-10v"), TwoToTenV("2-10v"), TenToTwov("10-2v"), TenToZeroV("10-0v");
	
	public String displayName;
	
	OutputAnalogActuatorType(String str) {
		displayName = str;
	}
}
