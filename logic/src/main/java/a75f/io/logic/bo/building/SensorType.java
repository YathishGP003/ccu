package a75f.io.logic.bo.building;

import a75f.io.logic.bo.building.definitions.Port;

/**
 * Created by samjithsadasivan on 1/21/19.
 */

public enum SensorType
{
    NONE("NONE"),
    HUMIDITY("humidity"),
    CO2("co2"),
    CO("co"),
    NO("no"),
    VOC("voc"),
    PRESSURE("pressure"),
    OCCUPANCY("occupancy"),
    ENERGY_METER_HIGH("emr_high"),
    ENERGY_METER_LOW("emr_low"),
    SOUND("sound"),
    CO2_EQUIVALENT("co2_equivalent"),
    ILLUMINANCE ("illuminance"),
    UVI("uvi");
    String name;
    
    SensorType(String val) {
        name = val;
    }
    
    public String toString() {
        return name;
    }
    
    public Port getSensorPort() {
        switch (this) {
            case NONE:
                return null;
            case HUMIDITY:
                return Port.SENSOR_RH;
            case CO2:
                return Port.SENSOR_CO2;
            case CO:
                return Port.SENSOR_CO;
            case NO:
                return Port.SENSOR_NO;
            case VOC:
                return Port.SENSOR_VOC;
            case PRESSURE:
                return Port.SENSOR_PRESSURE;
            case OCCUPANCY:
                return Port.SENSOR_OCCUPANCY;
            case ENERGY_METER_LOW:
            case ENERGY_METER_HIGH:
                return Port.SENSOR_ENERGY_METER;
            case SOUND:
                return Port.SENSOR_SOUND;
            case CO2_EQUIVALENT:
                return Port.SENSOR_CO2_EQUIVALENT;
            case ILLUMINANCE:
                return Port.SENSOR_ILLUMINANCE;
            case UVI:
                return Port.SENSOR_UVI;
            default:
                return null;
        }
    }
}
