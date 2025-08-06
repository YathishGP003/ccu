package a75f.io.renatus.profiles.hss.unitventilator.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.UniversalInputs
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.profiles.hss.HyperStatSplitState
import a75f.io.renatus.profiles.hss.HyperStatSplitViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe4UvViewState
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import androidx.compose.runtime.mutableStateOf


open class UnitVentilatorViewModel : HyperStatSplitViewModel() {

     private val oaoError = "The profile must have one <b>Mixed Air Temperature Sensor, Outside Air Temperature Sensor & OAO Damper </b>mapped. Please map the Temperature Sensors on either the Sensor Bus or Universal Inputs and OAO Damper on any one of the Analog-Outs"
    private val duplicateSensor = "The profile can have only one <b> Discharge Air Temperature Sensor, Mixed Air Temperature Sensor, Outside Air Temperature Sensor, Current TX Sensor, Filter Clogged, Condensate Overflow, Generic Alarm Sensor</b> mapped. Please check the Sensor Bus or Universal Inputs."
    lateinit var controlViaList : List<String>

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        viewState = mutableStateOf(HyperStatSplitState())
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
    }

   // based on the profile configuration, check if the analog control is enabled and mapped

    fun isAnalogEnabledAndMapped(enabled: Boolean, associate: Int, keyValue: Int):Boolean {
        val enumList = when (profileConfiguration) {
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.values()
            else -> null
        }
         if(enumList!=null){
            return enabled && associate == enumList[keyValue].ordinal
         }
        return false
    }

     private fun isAnyAnalogEnabledAndMapped(keyValue: Int): Boolean {

        val enumList = when (profileConfiguration) {
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.values()
            else -> null
        }

        fun isAnalogOutEnabledAndMapped(analogOutEnabled: Boolean, analogOutAssociation: Int): Boolean {
            return analogOutEnabled && analogOutAssociation == enumList?.get(keyValue)?.ordinal
        }
            return isAnalogOutEnabledAndMapped(viewState.value.analogOut1Enabled , viewState.value.analogOut1Association) ||
                    isAnalogOutEnabledAndMapped(viewState.value.analogOut2Enabled , viewState.value.analogOut2Association) ||
                    isAnalogOutEnabledAndMapped(viewState.value.analogOut3Enabled , viewState.value.analogOut3Association) ||
                    isAnalogOutEnabledAndMapped(viewState.value.analogOut4Enabled , viewState.value.analogOut4Association)
    }

    fun profileValidation(): Pair<Boolean,Spanned> {

        if (viewState.value.enableOutsideAirOptimization && (
                    !isAnyAnalogMappedToControl(Pipe4UvAnalogOutControls.OAO_DAMPER.ordinal) ||
                            !(isAnyUniversalInMapped(UniversalInputs.OUTSIDE_AIR_TEMPERATURE)  || isAnySensorBusMapped(
                                CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) ||
                            !(isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) || isAnySensorBusMapped(
                                CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
                    )
        ) {
            return Pair(true, Html.fromHtml(oaoError,Html.FROM_HTML_MODE_LEGACY))
        }
        if (isUniversalInDuplicated(UniversalInputs.MIXED_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }
        if (isUniversalInDuplicated(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }
            if (isUniversalInDuplicated(UniversalInputs.CONDENSATE_STATUS_NO) ||
            isUniversalInDuplicated(UniversalInputs.CONDENSATE_STATUS_NC) ||
            (isAnyUniversalInMapped(UniversalInputs.CONDENSATE_STATUS_NO) && isAnyUniversalInMapped(UniversalInputs.CONDENSATE_STATUS_NC))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }

        if (isUniversalInDuplicated(UniversalInputs.SUPPLY_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.SUPPLY_AIR_TEMPERATURE) && isAnySensorBusMapped(CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }

        if (isUniversalInDuplicated(UniversalInputs.FILTER_STATUS_NO) ||
            isUniversalInDuplicated(UniversalInputs.FILTER_STATUS_NC) ||
            (isAnyUniversalInMapped(UniversalInputs.FILTER_STATUS_NO) && isAnyUniversalInMapped(UniversalInputs.FILTER_STATUS_NC))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }

        if (isUniversalInDuplicated(UniversalInputs.GENERIC_ALARM_NO) ||
            isUniversalInDuplicated(UniversalInputs.GENERIC_ALARM_NC) ||
            (isAnyUniversalInMapped(UniversalInputs.GENERIC_ALARM_NO) && isAnyUniversalInMapped(UniversalInputs.GENERIC_ALARM_NC))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }

        if (isUniversalInDuplicated(UniversalInputs.CURRENT_TX_10) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_20) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_30) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_50) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_60) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_100) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_120) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_150) ||
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_200) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_10) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_20) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_30) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_50) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_60) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_100) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_120) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200)) ||
            (isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_150) && isAnyUniversalInMapped(UniversalInputs.CURRENT_TX_200))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }
        if ((viewState.value as Pipe4UvViewState).controlVia == 0) {

            if (isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal) ||
                isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal)
            ) {
                return Pair(true, Html.fromHtml("The profile must have <b>Fully Modulating Valve</b> mapped in control via  when <b>Cooling Water Valve</b> or <b>Heating Water Valve</b> is selected in Analog-Out. Please map the <b>Fully Modulating Valve</b>  in control via option .", Html.FROM_HTML_MODE_LEGACY))
             }

        }

        if ((viewState.value as Pipe4UvViewState).controlVia == 1) {
            if (isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal)) {
                return Pair(true, Html.fromHtml("The profile must have <b>Face & Bypass Damper </b> mapped in control via  when <b>Face & Bypass Damper </b> is selected in Analog-Out. Please map the <b>Face & Bypass Damper</b>  in control via option .", Html.FROM_HTML_MODE_LEGACY))
            }
        }

        if ((viewState.value as Pipe4UvViewState).saTempering) {

            if(!((isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.FAN_SPEED.ordinal) ||
                        isAnyRelayMappedToControlForUnitVentilator(Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION))&&
                (isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal) ||
                        isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.OAO_DAMPER.ordinal)) &&
                        (isAnyRelayMappedToControlForUnitVentilator(Pipe4UVRelayControls.HEATING_WATER_VALVE)||
                                isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal)))
                )
            {
                return Pair(true, Html.fromHtml("The profile must have one <b>Heating Water Valve, Fan Low Speed - Ventilation or Fan Speed in Analog-Out and OAO Damper/DCV Damper</b> mapped when Supply Air Tempering is enabled", Html.FROM_HTML_MODE_LEGACY))
            }
        }

        if(viewState.value.pressureSensorAddress0.enabled && viewState.value.sensorAddress0.enabled){
            return Pair(true, Html.fromHtml("The profile must not have <b>Pressure Sensor or Sensor Address 0</b> mapped. Please remove the mapping from the Sensor Bus or Pressure Sensor.", Html.FROM_HTML_MODE_LEGACY))
        }
        if(isAnyRelayMappedToControlForUnitVentilator(Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION) &&
            isAnyRelayMappedToControlForUnitVentilator(Pipe4UVRelayControls.FAN_LOW_SPEED)
        ) {
            return Pair(true, Html.fromHtml("The profile must not have <b>Fan Low Speed - Ventilation and Fan Low Speed</b> mapped. Please remove any one  mapping from the Relay port.", Html.FROM_HTML_MODE_LEGACY))
        }
        return Pair(false, Html.fromHtml("", Html.FROM_HTML_MODE_LEGACY))
    }


}