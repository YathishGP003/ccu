package a75f.io.logic.bo.building.modbus;

import a75f.io.logic.bo.building.definitions.DamperType;

public enum ModbusEquipTypes {
    UPS("Uninterrupted Power Supply"),PAC ("Precision Air Conditioning"),RRS ("Rodent Repellant Systems"),WLD ("Water Leak Detection"),VRF ("Variable Frequency"),EM ("Energy Meters"),ATS ("Automatic Transfer Switch");
    public String displayName;

    ModbusEquipTypes(String str) {
        displayName = str;
    }

    public static ModbusEquipTypes getEnum(String value) {
        for(ModbusEquipTypes v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
