package a75f.io.logic.bo.building.definitions;

public enum SmartStatHeatPumpChangeOverType {
    NOT_USED("Not Used"),ENERGIZE_IN_COOLING("Energize in cooling"),ENERGIZE_IN_HEATING("Energize in heating");
    public String displayName;

    SmartStatHeatPumpChangeOverType(String str) {
        displayName = str;
    }

    public static SmartStatHeatPumpChangeOverType getEnum(String value) {
        for(SmartStatHeatPumpChangeOverType v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
