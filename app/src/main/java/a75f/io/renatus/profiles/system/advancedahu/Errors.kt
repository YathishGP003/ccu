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

fun duplicateError(domainName: String): Spanned = Html.fromHtml("Duplicate selection for <b>${domainName}</b> is not allowed.", Html.FROM_HTML_MODE_LEGACY)
