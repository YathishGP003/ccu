package a75f.io.renatus.profiles.hyperstat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.HsPipe2Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.util.getHSPipe2FanLevel
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
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe2ViewState
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

class HsPipe2ViewModel(application: Application) : HyperStatViewModel(application) {

    override var viewState = mutableStateOf(Pipe2ViewState() as ProfileViewState)

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getHyperStatPipe2Model() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatPipe2Profile) {
            hyperStatProfile = (L.getProfile(deviceAddress) as HyperStatPipe2Profile)
            val equip = (hyperStatProfile as HyperStatPipe2Profile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getHsConfiguration(equip.equipRef)!!.getActiveConfiguration()
            equipRef = equip.equipRef
        } else {
            profileConfiguration = Pipe2Configuration(
                nodeAddress = deviceAddress.toInt(),
                nodeType = nodeType.name,
                priority = 0,
                roomRef = zoneRef,
                floorRef = floorRef,
                profileType = profileType,
                model = equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatViewStateUtil.pipe2ConfigToState(profileConfiguration as Pipe2Configuration)
        isCopiedConfigurationAvailable()
    }

    override fun saveConfiguration() {
        HyperStatViewStateUtil.pipe2StateToConfig(viewState.value as Pipe2ViewState, profileConfiguration as Pipe2Configuration)
        if (profileConfiguration.isAnyRelayEnabledAssociated(association = HsPipe2RelayMapping.FAN_LOW_SPEED.ordinal)
            && profileConfiguration.isAnyRelayEnabledAssociated(association = HsPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal)) {
            showErrorDialog(context, Html.fromHtml("<br>\"The profile must not have <b>Fan Low Speed - Ventilation and Fan Low Speed</b> mapped. Please remove any one from the mapping.", Html.FROM_HTML_MODE_LEGACY))
            return
        }
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPipe2Profile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendHyperStatSeedMessage(deviceAddress, zoneRef, floorRef, false)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("HS 2Pipe Configuration saved successfully", context)
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
    
    private fun setUpPipe2Profile() {
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(profileConfiguration as Pipe2Configuration, equipModel, deviceModel)
            hyperStatProfile = HyperStatPipe2Profile()
            (hyperStatProfile as HyperStatPipe2Profile).addEquip(equipId)
            L.ccu().zoneProfiles.add(hyperStatProfile)
            val equip = HsPipe2Equip(equipId)
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
            updateFanMode(false,equip, getHSPipe2FanLevel(profileConfiguration as Pipe2Configuration))
            CcuLog.i(Domain.LOG_TAG, "Pipe2 profile added")
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
            val equip = HsPipe2Equip(equipId)
            updateFanMode(true, equip, getHSPipe2FanLevel(profileConfiguration as Pipe2Configuration))
        }
        profileConfiguration.apply {
            val possibleConditioningMode = getPossibleConditionMode(profileConfiguration)
            val possibleFanMode = getHsPossibleFanModeSettings(getHSPipe2FanLevel(profileConfiguration as Pipe2Configuration))
            val equip = HsPipe2Equip(equipId)
            modifyFanMode(possibleFanMode.ordinal, equip.fanOpMode)
            modifyConditioningMode(possibleConditioningMode.ordinal, equip.conditioningMode, allStandaloneProfileConditions)
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
        }
    }
}