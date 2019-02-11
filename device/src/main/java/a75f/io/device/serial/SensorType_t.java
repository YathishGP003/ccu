package a75f.io.device.serial;

public enum SensorType_t {
    SENSOR_NONE, ///< Default
    SENSOR_HUMIDITY, ///< Measured in 1/10 of a %. Range is 0 to 1000.
    SENSOR_CO2, ///< Measured in PPM. Normal range is 400-1000 PPM.
    SENSOR_CO, ///< Measured in PPM. Normal range is 20-50 PPM.
    SENSOR_NO, ///< Measured in PPM. Normal range is 2-5 PPM.
    SENSOR_VOC, ///< Measured in PPB. Normal range is 0-60000 PPB.
    SENSOR_PRESSURE, ///< Measured in inches water column
    SENSOR_OCCUPANCY, ///< 0 = No Occupant Detected, 1 = Occupant Detected
    SENSOR_ENERGY_METER_HIGH, ///< High Data Byte. Measured in hundreds of watt-hours. Values will tally with display on energy meter.
    SENSOR_ENERGY_METER_LOW, ///< Low Data Byte
    SENSOR_SOUND, ///< Measured in decibels. Normal range is 0-140 dB.
    SENSOR_CO2_EQUIVALENT, ///< Measured in PPM. Normal range is 400-60000 PPM.
    SENSOR_ILLUMINANCE, ///< Illuminance in tens of lux. Typical range for office lighting is 320-500 lux.
    SENSOR_UVI ///< A linear scale of UV radiation. Typical range is 0-10.
}
