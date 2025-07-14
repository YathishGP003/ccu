package a75f.io.renatus.profiles.system.advancedahu

import android.text.Html
import android.text.Spanned

/**
 * Created by Manjunath K on 15-07-2024.
 */
const val SAT_2_MUST_ERROR = "Supply Air Temperature 3 should be <b>selected after Supply Air Temperature 2</b> in Sensor Bus and Thermistor inputs."
const val SAT_1_MUST_ERROR = "Supply Air Temperature 2 should be <b>selected after Supply Air Temperature 1</b> in Sensor Bus and thermistor inputs."
const val PRESSURE_RELAY_ERROR = "Relay based Pressure Fan is mapped but <b>Pressure Fan configuration is not mapped</b>"
const val NO_PRESSURE_SENSOR_ERROR = "Pressure configuration mapped but <b>Pressure Sensor is not available</b>"
const val COOLING_CONFIG_ERROR = "Relay based SAT Cooling is mapped but <b>SAT Cooling configuration</b> is not mapped"
const val HEATING_CONFIG_ERROR = "Relay based SAT Heating is mapped but <b>SAT Heating configuration</b> is not mapped"
const val NO_SAT_HEATING_SENSOR = "SAT Heating configuration is mapped but <b>Supply Air Temperature Sensor is not available</b>"
const val NO_SAT_COOLING_SENSOR = "SAT Cooling configuration is mapped but <b>Supply Air Temperature Sensor is not available</b>"
const val OAO_DAMPER_ERROR = "OAO Damper configuration is mapped but <b>Outside Air Optimization Damper </b> toggle is not enabled "
const val OUTSIDE_AIR_OPTIMIZATION_ERROR = "Outside Air Optimization Damper toggle is enabled but <b>OAO Damper</b> is not mapped "
const val RETURN_DAMPER_OAO_DAMPER_ERROR = "Return Damper configuration is mapped but <b> OAO Damper </b> is not mapped "
const val MAT_OAT_SAT_NOT_MAPPED = "OAO Damper configuration is mapped but <b> Mixed Air Temperature  </b> and <b> Outside Air Temperature  </b> is not mapped "
const val NO_COMPRESSOR = "The O/B changeover relay is mapped, but the compressor is not mapped."
const val NO_OB_REALLY = "The compressor is mapped, but the O/B changeover relay is not mapped."
const val NO_OB_REALLY_CONNECT = "The compressor is mapped in connect module, but the O/B changeover relay is not mapped."
const val NO_COMPRESSOR_CONNECT = "The O/B changeover relay is mapped in connect module, but the compressor is not mapped."
fun duplicateError(domainName: String): Spanned = Html.fromHtml("Duplicate selection for <b>${domainName}</b> is not allowed.", Html.FROM_HTML_MODE_LEGACY)
