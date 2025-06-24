package a75f.io.device.connect

/**
 * ConnectModule Universal input mapped values are 8 Float values over 16 registers.
 * Register type : Input Register (0x03)
 */
const val UNIN_MAPPED_VAL_START_ADDR = 60200
const val UNIN_MAPPED_VAL_REG_COUNT = 16

/**
 * ConnectModule Universal input configurations are 8 enum values (Int) over 8 registers.
 * Register type : Holding Register (0x04)
 */
const val UNIN_MAPPING_CONFIG_START_ADDR = 60200
const val UNIN_MAPPING_CONFIG_REG_COUNT = 8


/**
 * ConnectModule Relay configurations are 8 enum values (Int) over 8 registers.
 * Register type : Holding Register (0x04)
 */
const val RELAY_MAPPING_CONFIG_START_ADDR = 60240
const val RELAY_MAPPING_CONFIG_REG_COUNT = 8


/**
 * ConnectModule AnalogOut configurations are 8 enum values (Int) over 8 registers.
 * Register type : Holding Register (0x04)
 */
const val AOUT_MAPPING_CONFIG_START_ADDR = 60280
const val AOUT_MAPPING_CONFIG_REG_COUNT = 4


/**
 * ConnectModule Relay outs are 8 Integer values 8 registers.
 * Register type : Holding Register (0x04)
 */
const val RELAY_MAPPED_VAL_START_ADDR = 60316
const val RELAY_MAPPED_VAL_REG_COUNT = 8

/**
 * ConnectModule Analog outs are 4 float values over 8 registers.
 * Register type : Holding Register (0x04)
 */
const val AOUT_MAPPED_VAL_START_ADDR = 60356
const val AOUT_MAPPED_VAL_REG_COUNT = 8


const val ADVANCED_AHU_CONNECT1_SLAVE_ADDR = 98
const val ADVANCED_AHU_CONNECT2_SLAVE_ADDR = 97

const val SENSOR_BUS_START_ADDR = 60037
const val SENSOR_BUS_REG_COUNT = 25

const val DIAGONASTIC_INFO_START_ADDR = 60600
const val DIAGONASTIC_INFO_REG_COUNT = 47

/**
 * ConnectModule modbus address for sensor bus values
 */
const val SENSOR_BUS_TEMPERATURE_AVERAGE = 60037
const val SENSOR_BUS_HUMIDITY_AVERAGE = 60038
const val SENSOR_BUS_CO2_AVERAGE = 60039
const val SENSOR_BUS_OCCUPANCY_AVERAGE = 60040
const val SENSOR_BUS_PRESSURE_AVERAGE = 60041
const val SENSOR_BUS_TEMPERATURE_1 = 60042
const val SENSOR_BUS_TEMPERATURE_2 = 60043
const val SENSOR_BUS_TEMPERATURE_3 = 60044
const val SENSOR_BUS_TEMPERATURE_4 = 60045
const val SENSOR_BUS_HUMIDITY_1 = 60046
const val SENSOR_BUS_HUMIDITY_2 = 60047
const val SENSOR_BUS_HUMIDITY_3 = 60048
const val SENSOR_BUS_HUMIDITY_4 = 60049
const val SENSOR_BUS_CO2_1 = 60050
const val SENSOR_BUS_CO2_2 = 60051
const val SENSOR_BUS_CO2_3 = 60052
const val SENSOR_BUS_CO2_4 = 60053
const val SENSOR_BUS_OCCUPANCY_1 = 60054
const val SENSOR_BUS_OCCUPANCY_2 = 60055
const val SENSOR_BUS_OCCUPANCY_3 = 60056
const val SENSOR_BUS_OCCUPANCY_4 = 60057
const val SENSOR_BUS_PRESSURE_1 = 60058
const val SENSOR_BUS_PRESSURE_2 = 60059
const val SENSOR_BUS_PRESSURE_3 = 60060
const val SENSOR_BUS_PRESSURE_4 = 60061

const val LOW_CODE_INPUT_REG_ACTIVE_SEQUENCE_METADATA_LENGTH_MSB = 60600
const val LOW_CODE_INPUT_REG_ACTIVE_SEQUENCE_METADATA_SIGNATURE_0 = 60602
const val LOW_CODE_INPUT_REG_ACTIVE_SEQUENCE_METADATA_NAME_0 = 60618
const val LOW_CODE_INPUT_REG_SEQ_RUN_COUNT = 60640
const val LOW_CODE_INPUT_REG_SEQ_LAST_RUN_TIME = 60642
const val LOW_CODE_INPUT_REG_SEQ_LONG_RUN_TIME = 60643
const val LOW_CODE_INPUT_REG_SEQ_STATUS = 60644
const val LOW_CODE_INPUT_REG_SEQ_ERROR_CODE = 60645
const val LOW_CODE_INPUT_REG_SEQUENCE_UPDATE_STATE = 60646
const val LOW_CODE_INPUT_REG_SEQUENCE_UPDATE_ERROR = 60690


