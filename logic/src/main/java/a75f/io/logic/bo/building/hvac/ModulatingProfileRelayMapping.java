package a75f.io.logic.bo.building.hvac;

public enum ModulatingProfileRelayMapping {

    HUMIDIFIER("Humidifier"),
    DEHUMIDIFIER("De Humidifier"),

    CHANGE_OVER_COOLING("Change Over Cooling"),
    CHANGE_OVER_HEATING("Change Over Heating"),
    FAN_ENABLE("Fan Enable"),
    OCCUPIED_ENABLED("Occupied Enabled"),
    DCV_DAMPER("DCV Damper");

    public final String displayName;

    ModulatingProfileRelayMapping(String str) {
        displayName = str;
    }

    public int getValue() {
        return ordinal() + 1;
    }

    public static ModulatingProfileRelayMapping getEnum(String value) {
        for(ModulatingProfileRelayMapping v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
