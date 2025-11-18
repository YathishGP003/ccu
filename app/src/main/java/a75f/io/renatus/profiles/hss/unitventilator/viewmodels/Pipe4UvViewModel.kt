package a75f.io.renatus.profiles.hss.unitventilator.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getHssProfileFanLevel
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.EconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.UniversalInputs
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UnitVentilatorProfile
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorProfile
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.correctSensorBusTempPoints
import a75f.io.logic.bo.building.statprofiles.util.getPipe4UvPossibleConditioningModeSettings
import a75f.io.logic.bo.building.statprofiles.util.getPossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.getUvPossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.mapSensorBusPressureLogicalPoint
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.messaging.handler.setPortConfiguration
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe4UvViewState
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.renatus.util.showErrorDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class Pipe4UvViewModel : UnitVentilatorViewModel() {
    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)
        viewState = mutableStateOf(Pipe4UvViewState())
        equipModel = ModelLoader.getSplitPipe4Model() as SeventyFiveFProfileDirective
        deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective

        if (L.getProfile(deviceAddress)!=null && L.getProfile(deviceAddress) is UnitVentilatorProfile){
            hssProfile = L.getProfile(deviceAddress) as UnitVentilatorProfile
            profileConfiguration = Pipe4UVConfiguration(deviceAddress.toInt(),nodeType.name,0,zoneRef,floorRef,profileType,equipModel).getActiveConfiguration()

            equipRef = profileConfiguration.equipId
        }
        else {
            profileConfiguration = Pipe4UVConfiguration(deviceAddress.toInt(), nodeType.name, 0, zoneRef, floorRef, profileType, equipModel).getDefaultConfiguration()
        }
        viewState.value = Pipe4UvViewState.fromProfileConfigToState(profileConfiguration as Pipe4UVConfiguration)
        this.context = context
        this.hayStack = hayStack

        initializeLists()
        genericList()
        isCopiedConfigurationAvailable()
    }



    override fun saveConfiguration() {
        if(saveJob == null){

            val genericValidation = profileGenericValidation()
            val profileValidation = profileBasedConfig()
            if (genericValidation.first) {
                showErrorDialog(context, genericValidation.second)
                return
            }
            if(profileValidation.first){
                showErrorDialog(context, profileValidation.second)
                return
            }
            ProgressDialogUtils.showProgressDialog(context, context.getString(R.string.saving_configuration))
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPipe4Profile()
                CcuLog.i(Domain.LOG_TAG, "HSS Pipe 4 Profile Setup complete")
                L.saveCCUState()
                CCUHsApi.getInstance().setCcuReady()
                CcuLog.i(Domain.LOG_TAG, "HSS Pipe 4  Profile Pairing complete")
                hayStack.syncEntityTree()
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast(context.getString(R.string.hss_4_pipe_config_saved), context)
                    CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
                LSerial.getInstance()
                    .sendHyperSplitSeedMessage(deviceAddress, zoneRef, floorRef)
                CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
            }
        }


    }

    private fun profileBasedConfig(): Pair<Boolean,Spanned> {

        if ((viewState.value as Pipe4UvViewState).controlVia == 0) {

            if (profileType == ProfileType.HYPERSTATSPLIT_4PIPE_UV) {
                if (isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.HEATING_WATER_MODULATING_VALVE.name) ||
                    isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.COOLING_WATER_MODULATING_VALVE.name)
                ) {
                    return Pair(true, Html.fromHtml(context.getString(R.string.profile_must_have_fully_modulating_valve), Html.FROM_HTML_MODE_LEGACY))
                }

            }
        }

        if ((viewState.value as Pipe4UvViewState).controlVia == 1) {
            if (isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.FACE_DAMPER_VALVE.name)) {
                return Pair(true, Html.fromHtml(context.getString(R.string.profile_must_have_face_bypass_damper), Html.FROM_HTML_MODE_LEGACY))
            }
        }

        if ((viewState.value as Pipe4UvViewState).saTempering) {

            if(!((isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.FAN_SPEED.name) ||
                        isAnyRelayMappedToControlForUnitVentilator(HyperStatSplitControlType.FAN_LOW_SPEED_VENTILATION.name))&&
                        ((isAnyUniversalInMapped(UniversalInputs.SUPPLY_AIR_TEMPERATURE))||isAnySensorBusMapped(EconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY))&&
                        (isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.DCV_MODULATING_DAMPER.name) ||
                                isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.OAO_DAMPER.name)) &&
                        (isAnyRelayMappedToControlForUnitVentilator(HyperStatSplitControlType.HEATING_WATER_VALVE.name)||
                                isAnyAnalogEnabledAndMapped(HyperStatSplitControlType.HEATING_WATER_MODULATING_VALVE.name)))
            )
            {
                return Pair(true, Html.fromHtml(context.getString(R.string.profile_must_havefully_modulating_valve), Html.FROM_HTML_MODE_LEGACY))
            }
        }
        return Pair(false, Html.fromHtml("", Html.FROM_HTML_MODE_LEGACY))

    }

    private fun setUpPipe4Profile() {
        (viewState.value as Pipe4UvViewState).updateConfigFromViewState(profileConfiguration as Pipe4UVConfiguration)
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId :String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(profileConfiguration, equipModel, deviceModel)
            hssProfile = Pipe4UnitVentilatorProfile(equipId, profileConfiguration.nodeAddress.toShort())
            L.ccu().zoneProfiles.add(hssProfile)
            val equip = Pipe4UVEquip(equipId)
            setConditioningMode(equip)
            updateFanMode(false, equip, getHssProfileFanLevel(equip))
        }
        else {
            equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
            val equip = Pipe4UVEquip(equipId)
            updateConditioningMode(equip)
            updateFanMode(true, equip, getHssProfileFanLevel(equip))
        }
        CoroutineScope(highPriorityDispatcher).launch {
            runBlocking {
                correctSensorBusTempPoints(profileConfiguration)
                mapSensorBusPressureLogicalPoint(profileConfiguration, equipId)
            }
        }
        profileConfiguration.apply {
            val pipe4Equip = Pipe4UVEquip(equipId)
            profileConfiguration.updateConditioningMode(equipId)
            profileConfiguration.apply { setPortConfiguration(profileConfiguration.nodeAddress, getRelayMap(), getAnalogMap())  }
            val possibleConditioningMode = getUvPossibleConditioningMode(profileConfiguration as UnitVentilatorConfiguration)
            val possibleFanMode = getPossibleFanMode(pipe4Equip)
            modifyFanMode(possibleFanMode.ordinal, pipe4Equip.fanOpMode)
            modifyConditioningMode(possibleConditioningMode.ordinal, pipe4Equip.conditioningMode, allStandaloneProfileConditions)
            DesiredTempDisplayMode.setModeType(zoneRef, hayStack)
        }
    }


    private fun updateConditioningMode(equip: Pipe4UVEquip) {
        val currentMode = equip.conditioningMode.readPriorityVal().toInt()
        val possible = getPipe4UvPossibleConditioningModeSettings(profileConfiguration as Pipe4UVConfiguration)
        val offValue = StandaloneConditioningMode.OFF.ordinal.toDouble()
        if (possible == PossibleConditioningMode.OFF ||
            (currentMode == StandaloneConditioningMode.AUTO.ordinal &&
                    (possible == PossibleConditioningMode.HEATONLY || possible == PossibleConditioningMode.COOLONLY)) ||
            (currentMode == StandaloneConditioningMode.HEAT_ONLY.ordinal && possible == PossibleConditioningMode.COOLONLY) ||
            (currentMode == StandaloneConditioningMode.COOL_ONLY.ordinal && possible == PossibleConditioningMode.HEATONLY)
        ) {
            equip.conditioningMode.writePointValue(offValue)
        }
    }

    private fun setConditioningMode(equip: Pipe4UVEquip) {
        val possible = getPipe4UvPossibleConditioningModeSettings(profileConfiguration as Pipe4UVConfiguration)
        val newMode = when (possible) {
            PossibleConditioningMode.BOTH -> StandaloneConditioningMode.AUTO
            PossibleConditioningMode.HEATONLY -> StandaloneConditioningMode.HEAT_ONLY
            PossibleConditioningMode.COOLONLY -> StandaloneConditioningMode.COOL_ONLY
            else -> StandaloneConditioningMode.OFF
        }
        equip.conditioningMode.writePointValue(newMode.ordinal.toDouble())
    }


    override fun hasUnsavedChanges(): Boolean {
        return !Pipe4UvViewState.fromProfileConfigToState(profileConfiguration as Pipe4UVConfiguration).equalsViewStatePipe4(viewState.value as Pipe4UvViewState)
    }

    fun cancelConfirm() {
        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is UnitVentilatorProfile) {
            hssProfile = L.getProfile(deviceAddress) as UnitVentilatorProfile
            profileConfiguration = Pipe4UVConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getActiveConfiguration()
        } else {
            profileConfiguration = Pipe4UVConfiguration(
                deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef, profileType, equipModel
            ).getDefaultConfiguration()
        }

        viewState.value =
            Pipe4UvViewState.fromProfileConfigToState(profileConfiguration as Pipe4UVConfiguration)

        openCancelDialog = false
    }
}