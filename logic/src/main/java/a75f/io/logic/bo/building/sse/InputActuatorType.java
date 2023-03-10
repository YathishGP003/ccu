package a75f.io.logic.bo.building.sse;

import android.util.Log;

public enum InputActuatorType {
	ZERO_TO_10A_CURRENT_TRANSFORMER ("Current TX (0-10 Amps)"),
	ZERO_TO_20A_CURRENT_TRANSFORMER ("Current TX (0-20 Amps)"),
	ZERO_TO_50A_CURRENT_TRANSFORMER ("Current TX (0-50 Amps)");

	private final String name;

	private InputActuatorType(String s) {
		name = s;
	}

	public boolean equalsName(String otherName) {
		return name.equals(otherName);
	}
	public static InputActuatorType getEnum (String value) {

		if (value.equals("0")){
			value = ZERO_TO_10A_CURRENT_TRANSFORMER.name;
		} else if (value.equals("1")) {
			value = ZERO_TO_20A_CURRENT_TRANSFORMER.name;
		} else {
			value = ZERO_TO_50A_CURRENT_TRANSFORMER.name;
		}

		for (InputActuatorType v : values())
			if (v.name.equalsIgnoreCase(value)) return  v;
		Log.d("CCU", "Actuator Not found: "+value);
		throw new IllegalArgumentException();
	}

	public String toString() {
		return this.name;
	}
	public static String getEnumStringDefinition() {
		return "Current TX (0-10 Amps), Current TX (0-20 Amps), Current TX (0-50 Amps)";
	}
}