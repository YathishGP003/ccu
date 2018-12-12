package a75f.io.logic.bo.building.hvac;

/**
 * Created by samjithsadasivan on 12/11/18.
 */

public enum Stage
{
    COOLING_1("Cooling Stage 1"),COOLING_2("Cooling Stage 2"), COOLING_3("Cooling Stage 3"), COOLING_4("Cooling Stage 4"), COOLING_5("Cooling Stage 5"),
    HEATING_1("Heating Stage 1"), HEATING_2("Heating Stage 2"), HEATING_3("Heating Stage 3"), HEATING_4("Heating Stage 4"), HEATING_5("Heating Stage 5"),
    FAN_1("Fan Stage 1"), FAN_2("Fan Stage 2");
    
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
