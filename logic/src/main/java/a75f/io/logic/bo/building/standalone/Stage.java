package a75f.io.logic.bo.building.standalone;

public enum  Stage {
    COOLING_1("Cooling Stage1"),COOLING_2("Cooling Stage2"),
    HEATING_1("Heating Stage1"), HEATING_2("Heating Stage2"),
    FAN_1("Fan Stage1"), FAN_2("Fan Stage2");

    public String displayName;

    Stage(String str) {
        displayName = str;
    }

    public int getValue() {
        return ordinal() + 1;
    }

    public static Stage getEnum(String value) {
        for(Stage v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
