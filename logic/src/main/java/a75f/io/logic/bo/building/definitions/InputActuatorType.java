package a75f.io.logic.bo.building.definitions;

import android.util.Log;

/**
 * Created by Yinten isOn 8/15/2017.
 */

public enum InputActuatorType
{
	ZeroTo10ACurrentTransformer("zero_to_10_current_transformer"), ZeroTo20ACurrentTransformer("zero_to_20_current_transformer"), ZeroTo50CurrentTransformer("zero_to_50_current_transformer");

	public String displayName;

	InputActuatorType (String str) {
		displayName = str;
	}

	public static InputActuatorType getEnum (String value) {

		if (value.equals("0")){
			value = "zero_to_10_current_transformer";
		} else if (value.equals("1")) {
			value = "zero_to_20_current_transformer";
		} else {
			value = "zero_to_50_current_transformer";
		}

		for (InputActuatorType v : values())
			if (v.displayName.equalsIgnoreCase(value)) return  v;
		Log.d("CCU", "Actuator Not found: "+value);
		throw new IllegalArgumentException();
	}

}
