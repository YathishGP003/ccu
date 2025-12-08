package a75f.io.renatus.profiles.hss.unitventilator.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain.getListOfDisNameByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.EconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.UniversalInputs
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.SUPPLY_WATER_TEMPERATURE
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.NO_MAT_SENSOR
import a75f.io.renatus.modbus.util.getAppResourceString
import a75f.io.renatus.profiles.hss.HyperStatSplitState
import a75f.io.renatus.profiles.hss.HyperStatSplitViewModel
import a75f.io.renatus.profiles.viewstates.ConfigState
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import androidx.compose.runtime.mutableStateOf


open class UnitVentilatorViewModel : HyperStatSplitViewModel() {

    private val oaoError = getAppResourceString(R.string.profile_must_have_mixed_air_temp)
    private val duplicateSensor = getAppResourceString(R.string.profile_can_have_discharge_air_temperature)
    lateinit var controlViaList : List<String>

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        viewState = mutableStateOf(HyperStatSplitState())
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
    }
     fun genericList() {
        controlViaList = getListOfDisNameByDomainName(DomainName.controlVia, equipModel)
    }

   // based on the profile configuration, check if the analog control is enabled and mapped

    fun isAnalogEnabledAndMapped(enabled: Boolean, associate: Int, keyValue: Int):Boolean {
        val enumList = when (profileConfiguration) {
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.values()
            is Pipe2UVConfiguration -> Pipe2UvAnalogOutControls.values()
            else -> null
        }
         if(enumList!=null){
            return enabled && associate == enumList[keyValue].ordinal
         }
        return false
    }

    fun isAnySupplyWaterTemperatureMappedUniversal(): Boolean {
        return (
                (this.viewState.value.universalIn1Config.enabled && this.viewState.value.universalIn1Config.association == SUPPLY_WATER_TEMPERATURE) ||
                        (this.viewState.value.universalIn2Config.enabled && this.viewState.value.universalIn2Config.association == SUPPLY_WATER_TEMPERATURE) ||
                        (this.viewState.value.universalIn3Config.enabled && this.viewState.value.universalIn3Config.association ==SUPPLY_WATER_TEMPERATURE) ||
                        (this.viewState.value.universalIn4Config.enabled && this.viewState.value.universalIn4Config.association == SUPPLY_WATER_TEMPERATURE) ||
                        (this.viewState.value.universalIn5Config.enabled && this.viewState.value.universalIn5Config.association == SUPPLY_WATER_TEMPERATURE) ||
                        (this.viewState.value.universalIn6Config.enabled && this.viewState.value.universalIn6Config.association == SUPPLY_WATER_TEMPERATURE) ||
                        (this.viewState.value.universalIn7Config.enabled && this.viewState.value.universalIn7Config.association == SUPPLY_WATER_TEMPERATURE) ||
                        (this.viewState.value.universalIn8Config.enabled && this.viewState.value.universalIn8Config.association == SUPPLY_WATER_TEMPERATURE)
                )
    }


     fun isAnyRelayMappedToControlForUnitVentilator(type: String): Boolean {

        val key = getProfileBasedEnumValueRelayType(type)
        val enumList = when (profileConfiguration) {
            is Pipe4UVConfiguration -> Pipe4UVRelayControls.values()
            is Pipe2UVConfiguration -> Pipe2UVRelayControls.values()
            else -> null
        }
        fun isEnabledAndMapped(configState: ConfigState): Boolean {
            return (configState.enabled && configState.association == enumList?.get(key)?.ordinal)
        }
        return (isEnabledAndMapped(this.viewState.value.relay1Config) ||
                isEnabledAndMapped(this.viewState.value.relay2Config) ||
                isEnabledAndMapped(this.viewState.value.relay3Config) ||
                isEnabledAndMapped(this.viewState.value.relay4Config) ||
                isEnabledAndMapped(this.viewState.value.relay5Config) ||
                isEnabledAndMapped(this.viewState.value.relay6Config) ||
                isEnabledAndMapped(this.viewState.value.relay7Config) ||
                isEnabledAndMapped(this.viewState.value.relay8Config))
    }

      fun isAnyAnalogEnabledAndMapped(keyValue: String): Boolean {
         val key = getProfileBasedEnumValueAnalog(keyValue)
        val enumList = when (profileConfiguration) {
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.values()
            is Pipe2UVConfiguration -> Pipe2UvAnalogOutControls.values()
            else -> null
        }

        fun isAnalogOutEnabledAndMapped(analogOutEnabled: Boolean, analogOutAssociation: Int): Boolean {
            return analogOutEnabled && analogOutAssociation == enumList?.get(key)?.ordinal
        }
            return isAnalogOutEnabledAndMapped(viewState.value.analogOut1Enabled , viewState.value.analogOut1Association) ||
                    isAnalogOutEnabledAndMapped(viewState.value.analogOut2Enabled , viewState.value.analogOut2Association) ||
                    isAnalogOutEnabledAndMapped(viewState.value.analogOut3Enabled , viewState.value.analogOut3Association) ||
                    isAnalogOutEnabledAndMapped(viewState.value.analogOut4Enabled , viewState.value.analogOut4Association)
    }

    fun profileGenericValidation(): Pair<Boolean,Spanned> {

        if (viewState.value.enableOutsideAirOptimization && (
                    !isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.OAO_DAMPER.name) ||
                            !(isAnyUniversalInMapped(UniversalInputs.OUTSIDE_AIR_TEMPERATURE)  || isAnySensorBusMapped(
                                EconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) ||
                            !(isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) || isAnySensorBusMapped(
                                EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
                    )
        ) {
            return Pair(true, Html.fromHtml(oaoError,Html.FROM_HTML_MODE_LEGACY))
        }
        if (isUniversalInDuplicated(UniversalInputs.MIXED_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE) && isAnySensorBusMapped(EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY))
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }
        if (isUniversalInDuplicated(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) ||
            isSensorBusDuplicated(EconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.OUTSIDE_AIR_TEMPERATURE) && isAnySensorBusMapped(EconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY))
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
            isSensorBusDuplicated(EconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY) ||
            (isAnyUniversalInMapped(UniversalInputs.SUPPLY_AIR_TEMPERATURE) && isAnySensorBusMapped(EconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY))
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
            isUniversalInDuplicated(UniversalInputs.CURRENT_TX_200)
        ) {
            return Pair(true, Html.fromHtml(duplicateSensor, Html.FROM_HTML_MODE_LEGACY))
        }

        if(viewState.value.pressureSensorAddress0.enabled && viewState.value.sensorAddress0.enabled){
            return Pair(true, Html.fromHtml(context.getString(R.string.profile_must_not_have_pressure_sensor), Html.FROM_HTML_MODE_LEGACY))
        }
        if(isAnyRelayMappedToControlForUnitVentilator(HyperStatSplitControlType.FAN_LOW_SPEED_VENTILATION.name) &&
            isAnyRelayMappedToControlForUnitVentilator(HyperStatSplitControlType.FAN_LOW_SPEED.name)
        ) {
            return Pair(true, Html.fromHtml(context.getString(R.string.profile_must_not_have_fan_low_speed), Html.FROM_HTML_MODE_LEGACY))
        }

        if(profileType == ProfileType.HYPERSTATSPLIT_2PIPE_UV){
            if(!isAnySupplyWaterTemperatureMappedUniversal())
            {
                return Pair(true, Html.fromHtml(context.getString(R.string.profile_must_have_supply_water_temp), Html.FROM_HTML_MODE_LEGACY))

            }
        }

        if ((profileConfiguration is Pipe4UVConfiguration &&
                    isAnyAnalogMappedToControl(Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal)) ||
            (profileConfiguration is Pipe2UVConfiguration &&
                    isAnyAnalogMappedToControl(Pipe2UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal))
        ) {
            if (!(isAnyUniversalInMapped(UniversalInputs.MIXED_AIR_TEMPERATURE)
                        || isAnySensorBusMapped(EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)
                )) {
                return Pair(true, Html.fromHtml(NO_MAT_SENSOR, Html.FROM_HTML_MODE_LEGACY))
            }
        }
        return Pair(false, Html.fromHtml("", Html.FROM_HTML_MODE_LEGACY))
    }


}