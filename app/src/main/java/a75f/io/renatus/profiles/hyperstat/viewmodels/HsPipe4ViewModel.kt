package a75f.io.renatus.profiles.hyperstat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.HsPipe4Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.pipe4.HyperStatPipe4Profile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getHSPipe4FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getHsPossibleFanModeSettings
import a75f.io.logic.bo.building.statprofiles.util.getPossibleConditionMode
import a75f.io.logic.bo.building.system.resetConfigDisabledPorts
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hyperstat.util.HyperStatViewStateUtil
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe4ViewState
import a75f.io.renatus.profiles.viewstates.ProfileViewState
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.renatus.util.showErrorDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HsPipe4ViewModel(application: Application) : HyperStatViewModel(application) {

    override var viewState = mutableStateOf(Pipe4ViewState() as ProfileViewState)

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getHyperStatPipe4Model() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatPipe4Profile) {
            hyperStatProfile = (L.getProfile(deviceAddress) as HyperStatPipe4Profile)
            val equip = (hyperStatProfile as HyperStatPipe4Profile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getHsConfiguration(equip.equipRef)!!.getActiveConfiguration()
            equipRef = equip.equipRef
        } else {
            profileConfiguration = HsPipe4Configuration(
                nodeAddress = deviceAddress.toInt(),
                nodeType = nodeType.name,
                priority = 0,
                roomRef = zoneRef,
                floorRef = floorRef,
                profileType = profileType,
                model = equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatViewStateUtil.pipe4ConfigToState(profileConfiguration as HsPipe4Configuration)
        isCopiedConfigurationAvailable()
    }

    override fun saveConfiguration() {
        HyperStatViewStateUtil.pipe4StateToConfig(viewState.value as Pipe4ViewState, profileConfiguration as HsPipe4Configuration)
        if (profileConfiguration.isAnyRelayEnabledAssociated(association = HsPipe4RelayMapping.FAN_LOW_SPEED.ordinal)
            && profileConfiguration.isAnyRelayEnabledAssociated(association = HsPipe4RelayMapping.FAN_LOW_VENTILATION.ordinal)) {
            showErrorDialog(context, Html.fromHtml("<br>\"The profile must not have <b>Fan Low Speed - Ventilation and Fan Low Speed</b> mapped. Please remove any one from the mapping.", Html.FROM_HTML_MODE_LEGACY))
            return
        }
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPipe4Profile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendHyperStatSeedMessage(deviceAddress, zoneRef, floorRef, false)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("HS 4 Pipe Configuration saved successfully", context)
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }

                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }
    
    private fun setUpPipe4Profile() {
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(profileConfiguration as HsPipe4Configuration, equipModel, deviceModel)
            hyperStatProfile = HyperStatPipe4Profile()
            (hyperStatProfile as HyperStatPipe4Profile).addEquip(equipId)
            L.ccu().zoneProfiles.add(hyperStatProfile)
            val equip = HsPipe4Equip(equipId)
            setConditioningMode(equip)
            updateFanMode(false,equip, getHSPipe4FanLevel(profileConfiguration as HsPipe4Configuration))
            CcuLog.i(Domain.LOG_TAG, "Pipe4 profile added")
        } else {
            equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true)
            resetConfigDisabledPorts(
                profileConfiguration.getRelayLogicalPhysicalMap(equipRef!!),
                profileConfiguration.getAnalogOutLogicalPhysicalMap(equipRef!!)
            )
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
            val equip = HsPipe4Equip(equipId)
            updateConditioningMode(equip)
            updateFanMode(true, equip, getHSPipe4FanLevel(profileConfiguration as HsPipe4Configuration))
        }
        profileConfiguration.apply {
            val possibleConditioningMode = getPossibleConditionMode(profileConfiguration)
            val possibleFanMode = getHsPossibleFanModeSettings(getHSPipe4FanLevel(profileConfiguration as HsPipe4Configuration))
            val equip = HsPipe4Equip(equipId)
            modifyFanMode(possibleFanMode.ordinal, equip.fanOpMode)
            modifyConditioningMode(possibleConditioningMode.ordinal, equip.conditioningMode, allStandaloneProfileConditions)
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
        }
    }

    private fun setConditioningMode(equip: HsPipe4Equip) {

        val possible = getPossibleConditionMode(profileConfiguration)
        var newMode = StandaloneConditioningMode.OFF
        if (possible == PossibleConditioningMode.BOTH) newMode = StandaloneConditioningMode.AUTO
        if (possible == PossibleConditioningMode.HEATONLY) newMode = StandaloneConditioningMode.HEAT_ONLY
        if (possible == PossibleConditioningMode.COOLONLY) newMode = StandaloneConditioningMode.COOL_ONLY
        equip.conditioningMode.writePointValue(newMode.ordinal.toDouble())
    }


    private fun updateConditioningMode(equip: HsPipe4Equip) {
        val currentMode = equip.conditioningMode.readPriorityVal().toInt()
        val possible = getPossibleConditionMode(profileConfiguration)

        if (possible == PossibleConditioningMode.OFF) {
            equip.conditioningMode.writePointValue(PossibleConditioningMode.OFF.ordinal.toDouble())
            return
        }

        if(currentMode == StandaloneConditioningMode.AUTO.ordinal
            && (possible == PossibleConditioningMode.HEATONLY || possible == PossibleConditioningMode.COOLONLY)) {
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
            return
        }

        if (currentMode == StandaloneConditioningMode.HEAT_ONLY.ordinal && possible == PossibleConditioningMode.COOLONLY) {
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
            return
        }

        if (currentMode == StandaloneConditioningMode.COOL_ONLY.ordinal && possible == PossibleConditioningMode.HEATONLY) {
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
            return
        }
    }

    private fun addEquipment(config: HsPipe4Configuration, equipModel: SeventyFiveFProfileDirective, deviceModel: SeventyFiveFDeviceDirective): String {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val equipId = equipBuilder.buildEquipAndPoints(config, equipModel, hayStack.site!!.id, getEquipDis())
        deviceBuilder.buildDeviceAndPoints(config, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        return equipId
    }

}