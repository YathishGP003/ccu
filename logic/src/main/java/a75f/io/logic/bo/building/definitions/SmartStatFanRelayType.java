package a75f.io.logic.bo.building.definitions;

public enum SmartStatFanRelayType {
    NOT_USED("Not Used"),FAN_STAGE2("Fan Stage2"),HUMIDIFIER("Humidifier"), DE_HUMIDIFIER("Dehumidifier");
    public String displayName;

    SmartStatFanRelayType(String str) {
        displayName = str;
    }

    public static SmartStatFanRelayType getEnum(String value) {
        for(SmartStatFanRelayType v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
