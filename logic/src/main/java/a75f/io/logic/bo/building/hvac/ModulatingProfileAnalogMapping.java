package a75f.io.logic.bo.building.hvac;

public enum ModulatingProfileAnalogMapping {
    FanSpeed("Fan Speed"),
    CompressorSpeed("Compressor Speed"),
    OutsideAirDamper("Outside Air Damper"),
    Cooling("Cooling"),
    Heating("Heating"),
    ChilledWaterValve("Chilled Water Valve");

    private final String displayName;
    ModulatingProfileAnalogMapping(String str) {
        displayName = str;
    }

    public int getValue() {
        return ordinal() + 1;
    }

    public static ModulatingProfileAnalogMapping getEnum(String value) {
        for(ModulatingProfileAnalogMapping v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
