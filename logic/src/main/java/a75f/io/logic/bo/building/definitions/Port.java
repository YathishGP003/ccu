package a75f.io.logic.bo.building.definitions;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public enum Port
{
	RELAY_ONE("Relay1"), RELAY_TWO("Relay2"), ANALOG_OUT_ONE("Analog1Out"), ANALOG_OUT_TWO("Analog2Out"), ANALOG_IN_ONE("Analog1In"), ANALOG_IN_TWO("Analog2In");
	
	private final String name;
	Port(String s) {
		name = s;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
