package a75f.io.logic.bo.building.modbus;

public enum ModbusEquipTypes {

    UPS30K("Uninterrupted Power Supply-30k"), UPS400K("Uninterrupted Power Supply-400k"), UPS80K("Uninterrupted Power Supply-80k"), PAC("Precision Air Conditioning"), RRS("Rodent Repellant Systems"), WLD("Water Leak Detection"), VRF("Variable Frequency"), EM("Energy Meter El Measure"), EMS("Energy Meter Schneider"), ATS("Automatic Transfer Switch"), UPS150K("Uninterrupted Power Supply-150k"), BTU("BTU meter"),EMR("Energy Meter"),EMR_ZONE("Energy Meter Zone");

    public String displayName;

    ModbusEquipTypes(String str) {
        displayName = str;
    }

    public static ModbusEquipTypes getEnum(String value) {
        for (ModbusEquipTypes v : values())
            if (v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
