package a75f.io.logic.bo.building.definitions;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public enum Port
{
	RELAY_ONE, RELAY_TWO, RELAY_THREE, RELAY_FOUR, RELAY_FIVE, RELAY_SIX, RELAY_SEVEN, RELAY_EIGHT,
	ANALOG_OUT_ONE, ANALOG_OUT_TWO, ANALOG_IN_ONE, ANALOG_IN_TWO,TH1_IN,TH2_IN,
	DESIRED_TEMP, SENSOR_RT, SENSOR_RH, SENSOR_CO2, SENSOR_VOC, SENSOR_CO, SENSOR_NO, SENSOR_PRESSURE,
	SENSOR_OCCUPANCY, SENSOR_ENERGY_METER, SENSOR_SOUND, SENSOR_CO2_EQUIVALENT, SENSOR_ILLUMINANCE, SENSOR_UVI,
	SENSOR_NO2, SENSOR_PM2P5, SENSOR_PM10, RSSI;
	
	public String getPortSensor() {
		switch (this)
		{
			case SENSOR_RH:
				return "humidity";
			case SENSOR_CO2:
				return "co2";
			case SENSOR_VOC:
				return "voc";
			case SENSOR_CO:
				return "co";
			case SENSOR_NO:
				return "no";
			case SENSOR_PRESSURE:
				return "pressure";
			case SENSOR_OCCUPANCY:
				return "occupancy";
			case SENSOR_ENERGY_METER:
				return "emr";
			case SENSOR_SOUND:
				return "sound";
			case SENSOR_CO2_EQUIVALENT:
				return "co2Equivalent";
			case SENSOR_ILLUMINANCE:
				return "illuminance";
			case SENSOR_UVI:
				return "uvi";
			case SENSOR_NO2:
				return "no2";
			case SENSOR_PM2P5:
				return "pm2p5";
			case SENSOR_PM10:
				return "pm10";
			case SENSOR_RT:
				return "temperature";
			case RSSI:
				return "rssi";
			default:
				return name();
		}
		
	}
}
