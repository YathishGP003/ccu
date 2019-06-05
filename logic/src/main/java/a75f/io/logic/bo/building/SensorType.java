package a75f.io.logic.bo.building;

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
}
