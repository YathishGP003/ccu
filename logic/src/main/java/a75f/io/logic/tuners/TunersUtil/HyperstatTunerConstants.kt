package a75f.io.logic.tuners.TunersUtil


/**
 * @author Manjunath K
 * Created on 28"+"07"+"2021.
 */
class HyperstatTunerConstants {
    companion object {

        // Tuner names
        const val HYPERSTAT_COOLING_DEADBAND: String = "CoolingDeadband"
        const val HYPERSTAT_HEATING_DEADBAND: String = "HeatingDeadband"

        const val HYPERSTAT_COOLING_DEADBAND_MULTIPLIER: String = "CoolingDeadbandMultiplier"
        const val HYPERSTAT_HEATING_DEADBAND_MULTIPLIER: String = "HeatingDeadbandMultiplier"

        const val HYPERSTAT_PROPORTIONAL_KFACTOR: String = "ProportionalKFactor"
        const val HYPERSTAT_INTEGRAL_KFACTOR: String = "IntegralKFactor"

        const val HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE: String = "TemperatureProportionalRange"
        const val HYPERSTAT_RELAY_ACTIVATION_HISTERESIS: String = "RelayActivationHysteresis"
        const val HYPERSTAT_ANALOG_SPEED_MULTIPLIER: String = "AnalogFanSpeedMultiplier"
        const val HYPERSTAT_HUMIDITY_HISTERESIS: String = "HumidityHysteresis"

        const val HYPERSTAT_AUTO_AWAY_ZONE_STEPBACK_TEMP: String = "AutoAwayZoneSetbackTemp"
        const val HYPERSTAT_TEMPERATURE_INTEGRAL_TIME: String = "TemperatureIntegralTime"


        // Tuner Default values

        const val HYPERSTAT_COOLING_DEADBAND_DEFAULT = 2.0
        const val HYPERSTAT_HEATING_DEADBAND_DEFAULT = 2.0

        const val HYPERSTAT_COOLING_DEADBAND_MULTIPLIER_DEFAULT = 0.5
        const val HYPERSTAT_HEATING_DEADBAND_MULTIPLIER_DEFAULT = 0.5

        const val HYPERSTAT_PROPORTIONAL_KFACTOR_DEFAULT = 0.5
        const val HYPERSTAT_INTEGRAL_KFACTOR_DEFAULT = 0.5

        const val HYPERSTAT_TEMPERATURE_PROPORTIONAL_RANGE_DEFAULT = 2.0
        const val HYPERSTAT_RELAY_ACTIVATION_HISTERESIS_DEFAULT = 10.0
        const val HYPERSTAT_ANALOG_SPEED_MULTIPLIER_DEFAULT = 1.0
        const val HYPERSTAT_HUMIDITY_HISTERESIS_DEFAULT = 5.0
        const val HYPERSTAT_FORCED_OCCUPIED_TIMER_DEFAULT = 30.0
        const val HYPERSTAT_AUTO_AWAY_ZONE_TIMER_DEFAULT = 30.0
        const val HYPERSTAT_AUTO_AWAY_ZONE_STEPBACK_TEMP_DEFAULT = 2.0
        const val HYPERSTAT_TEMPERATURE_INTEGRAL_TIME_DEFAULT = 30.0


    }
}