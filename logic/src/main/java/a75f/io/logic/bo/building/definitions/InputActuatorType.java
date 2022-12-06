package a75f.io.logic.bo.building.definitions;

import android.util.Log;

/**
 * Created by Yinten isOn 8/15/2017.
 */

public enum InputActuatorType
{
	ZeroTo10ACurrentTransformer("transformerZeroTo10"), ZeroTo20ACurrentTransformer("transformerZeroTo20"), ZeroTo50CurrentTransformer("transformerZeroTo50");

	public String displayName;

	InputActuatorType (String str) {
		displayName = str;
	}

	public static InputActuatorType getEnum (String value) {

		if (value.equals("0")){
			value = "transformerZeroTo10";
		} else if (value.equals("1")) {
			value = "transformerZeroTo20";
		} else {
			value = "transformerZeroTo50";
		}

		for (InputActuatorType v : values())
			if (v.displayName.equalsIgnoreCase(value)) return  v;
		Log.d("CCU", "Actuator Not found: "+value);
		throw new IllegalArgumentException();
	}

}
