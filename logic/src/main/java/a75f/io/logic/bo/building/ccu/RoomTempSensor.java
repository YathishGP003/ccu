package a75f.io.logic.bo.building.ccu;

public enum RoomTempSensor {
    SENSOR_BUS_TEMPERATURE ("Sensor Bus Temperature"),
    THERMISTOR_1 ("Thermistor Input 1"),
    THERMISTOR_2 ("Thermistor Input 2");

    private final String name;

    private RoomTempSensor(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
    public static String getEnumStringDefinition() {
        return "Sensor Bus Temperature, Thermistor Input 1, Thermistor Input 2";
    }
}