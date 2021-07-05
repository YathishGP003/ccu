package a75f.io.modbusbox;

public enum ModbusCategory {
    BTU("BTU"),
    MODBUS("MODBUS"),
    EMR_SYSTEM("EMR_SYSTEM"),
    EMR_ZONE("EMR_ZONE"),
    EMR("EMR");

    public String displayName;
    ModbusCategory(String str) {
        displayName = str;
    }
}
