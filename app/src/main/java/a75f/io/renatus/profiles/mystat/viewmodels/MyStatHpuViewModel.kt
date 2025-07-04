package a75f.io.renatus.profiles.mystat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatHpuProfile
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleFanModeSettings
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.mystat.viewstates.MyStatHpuViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewStateUtil
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.renatus.util.showErrorDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatHpuViewModel(application: Application) : MyStatViewModel(application) {

    override var viewState = mutableStateOf(MyStatHpuViewState() as MyStatViewState)

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getMyStatHpuModel() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is MyStatHpuProfile) {
            myStatProfile = (L.getProfile(deviceAddress) as MyStatHpuProfile)
            val equip =
                (myStatProfile as MyStatHpuProfile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getMyStatConfiguration(equip.equipRef)!!
            equipRef = equip.equipRef
        } else {
            profileConfiguration = MyStatHpuConfiguration(
                nodeAddress = deviceAddress.toInt(),
                nodeType = nodeType.name,
                priority = 0,
                roomRef = zoneRef,
                floorRef = floorRef,
                profileType = profileType,
                model = equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = MyStatViewStateUtil.hpuConfigToState(
            profileConfiguration as MyStatHpuConfiguration,
            MyStatHpuViewState()
        )
    }

    private fun changeOverValidation(): Boolean {
        var isValidConfig = true
        if (viewState.value.isAnyRelayEnabledAndMapped(MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
            && viewState.value.isAnyRelayEnabledAndMapped(MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)) {
            showErrorDialog(context, Html.fromHtml("<br>HPU Profile can only have either 'O-Energize in Cooling' or 'B-Energize in Heating' relays configured.", Html.FROM_HTML_MODE_LEGACY))
            isValidConfig =  false
        }

        if (!viewState.value.isAnyRelayEnabledAndMapped(MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
            && !viewState.value.isAnyRelayEnabledAndMapped(MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)) {
            showErrorDialog(context, Html.fromHtml("<br>HPU Profile should have either 'O-Energize in Cooling' or 'B-Energize in Heating' relays configured.", Html.FROM_HTML_MODE_LEGACY))
            isValidConfig =  false
        }
        return isValidConfig
    }

    override fun saveConfiguration() {
        if (saveJob == null && changeOverValidation() && isValidConfiguration(viewState.value.isDcvMapped())) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpHpuProfile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendMyStatSeedMessage(deviceAddress, zoneRef, floorRef)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("MyStat Hpu Configuration saved successfully", context)
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

    private fun setUpHpuProfile() {
        MyStatViewStateUtil.hpuStateToConfig(
            viewState.value as MyStatHpuViewState, profileConfiguration as MyStatHpuConfiguration
        )
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(profileConfiguration as MyStatHpuConfiguration, equipModel, deviceModel)
            myStatProfile = MyStatHpuProfile()
            (myStatProfile as MyStatHpuProfile).addEquip(equipId)
            L.ccu().zoneProfiles.add(myStatProfile)
            val equip = MyStatHpuEquip(equipId)
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
            updateFanMode(
                false, equip, getMyStatHpuFanLevel(profileConfiguration as MyStatHpuConfiguration)
            )
            CcuLog.i(Domain.LOG_TAG, "MyStatHpu profile added")
        } else {
            equipId = equipBuilder.updateEquipAndPoints(
                profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true
            )
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
           val deviceRef =  deviceBuilder.updateDeviceAndPoints(
                profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis()
            )
            val equip = MyStatHpuEquip(equipId)
            updateFanMode(
                true, equip, getMyStatHpuFanLevel(profileConfiguration as MyStatHpuConfiguration)
            )
            universalInUnit(profileConfiguration, deviceRef)
        }

        profileConfiguration.apply {
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
            val possibleConditioningMode = getMyStatPossibleConditionMode(profileConfiguration)
            val possibleFanMode = getMyStatPossibleFanModeSettings(getMyStatHpuFanLevel(profileConfiguration as MyStatHpuConfiguration))
            val equip = MyStatHpuEquip(equipId)
            modifyFanMode(possibleFanMode, equip.fanOpMode)
            modifyConditioningMode(possibleConditioningMode.ordinal, equip.conditioningMode, allStandaloneProfileConditions)
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
        }
    }

}