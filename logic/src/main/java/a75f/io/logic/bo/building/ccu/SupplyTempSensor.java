package a75f.io.logic.bo.building.ccu;

public enum SupplyTempSensor {
    NONE ("None"),
    THERMISTOR_1 ("Thermistor Input 1"),
    THERMISTOR_2 ("Thermistor Input 2");

    private final String name;

    SupplyTempSensor(String s) {
        name = s;
    }

    public String toString() {
        return this.name;
    }
    public static String getEnumStringDefinition() {
        return "None, Thermistor Input 1, Thermistor Input 2";
    }
}