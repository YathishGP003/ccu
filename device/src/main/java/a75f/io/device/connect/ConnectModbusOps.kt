package a75f.io.device.connect


enum class ConnectModbusOps {
    READ_SENSOR_BUS_VALUES,
    READ_UNIVERSAL_INPUT_MAPPED_VALUES,
    WRITE_RELAY_OUTPUT_MAPPED_VALUES,
    WRITE_ANALOG_OUTPUT_MAPPED_VALUES,
    WRITE_UNIVERSAL_INPUT_MAPPING_CONFIG,
    WRITE_ANALOG_OUT_MAPPING_CONFIG,
    WRITE_RELAY_MAPPING_CONFIG,
    TEST_OPERATION;
}

inline fun <reified T : Enum<T>> Int.toEnum(): T? {
    return enumValues<T>().find { it.ordinal == this }
}