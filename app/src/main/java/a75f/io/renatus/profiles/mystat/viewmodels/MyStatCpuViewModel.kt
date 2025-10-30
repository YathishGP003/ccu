package a75f.io.renatus.profiles.mystat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatCpuProfile
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleFanModeSettings
import a75f.io.logic.bo.building.statprofiles.util.setConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.updateConditioningMode
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.mystat.viewstates.MyStatCpuViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewStateUtil
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.renatus.R
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatCpuViewModel(application: Application) : MyStatViewModel(application) {
    override var viewState = mutableStateOf(MyStatCpuViewState() as MyStatViewState)
    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getMyStatCpuModel() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is MyStatCpuProfile) {
            myStatProfile = (L.getProfile(deviceAddress) as MyStatCpuProfile)
            val equip =
                (myStatProfile as MyStatCpuProfile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getMyStatConfiguration(equip.equipRef)!!
            equipRef = equip.equipRef
        } else {
            profileConfiguration = MyStatCpuConfiguration(
                nodeAddress = deviceAddress.toInt(),
                nodeType = nodeType.name,
                priority = 0,
                roomRef = zoneRef,
                floorRef = floorRef,
                profileType = profileType,
                model = equipModel
            ).getDefaultConfiguration()
        }
        updateDeviceType(equipRef.toString())
        viewState.value = MyStatViewStateUtil.cpuConfigToState(
            profileConfiguration as MyStatCpuConfiguration,
            MyStatCpuViewState()
        )
        getAnalogOutDefaultValueForMyStatV1(profileConfiguration)
    }
    override fun getAnalogStatIndex() = MyStatCpuRelayMapping.values().size
    override fun saveConfiguration() {
        if (saveJob == null && isValidConfiguration(viewState.value.isDcvMapped())) {
            ProgressDialogUtils.showProgressDialog(context, context.getString(R.string.saving_configuration))
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpCpuProfile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendMyStatSeedMessage(deviceAddress, zoneRef, floorRef)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast(context.getString(R.string.mystat_cpu_config_saved), context)
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

    private fun setUpCpuProfile() {
        MyStatViewStateUtil.cpuStateToConfig(
            viewState.value as MyStatCpuViewState, profileConfiguration as MyStatCpuConfiguration
        )
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(profileConfiguration as MyStatCpuConfiguration, equipModel, deviceModel)
            myStatProfile = MyStatCpuProfile()
            (myStatProfile as MyStatCpuProfile).addEquip(equipId)
            L.ccu().zoneProfiles.add(myStatProfile)
            val equip = MyStatCpuEquip(equipId)
            setConditioningMode(profileConfiguration as MyStatCpuConfiguration, equip)
            updateFanMode(
                false, equip, getMyStatCpuFanLevel(profileConfiguration as MyStatCpuConfiguration)
            )
            updateDeviceVersionTypePointVal(equipId)
            CcuLog.i(Domain.LOG_TAG, "MyStatCpu profile added")
        } else {
            equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            val deviceRef = deviceBuilder.updateDeviceAndPoints(
                profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis()
            )
            val equip = MyStatCpuEquip(equipId)
            updateConditioningMode(profileConfiguration as MyStatCpuConfiguration, equip)
            updateFanMode(
                true, equip, getMyStatCpuFanLevel(profileConfiguration as MyStatCpuConfiguration)
            )
            profileConfiguration.universalInUnit(deviceRef)
        }

        profileConfiguration.apply {
            val possibleConditioningMode = getMyStatPossibleConditionMode(profileConfiguration)
            val possibleFanMode = getMyStatPossibleFanModeSettings(getMyStatCpuFanLevel(profileConfiguration as MyStatCpuConfiguration))
            val equip = MyStatCpuEquip(equipId)
            modifyFanMode(possibleFanMode, equip.fanOpMode)
            modifyConditioningMode(possibleConditioningMode.ordinal, equip.conditioningMode, allStandaloneProfileConditions)
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
            DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, CCUHsApi.getInstance())
            updateEnumConfigs(equip, devicesVersion)
        }

    }

    fun isAnyRelayMappedToState(mapping: MyStatCpuRelayMapping): Boolean {
        fun enabledAndMapped(enabled: Boolean, association: Int, mapping: MyStatCpuRelayMapping) = enabled && association == mapping.ordinal
        (viewState.value as MyStatCpuViewState).apply {
            return enabledAndMapped(relay1Config.enabled, relay1Config.association, mapping)
                    || enabledAndMapped(relay2Config.enabled, relay2Config.association, mapping)
                    || enabledAndMapped(relay3Config.enabled, relay3Config.association, mapping)
                    || enabledAndMapped(universalOut1.enabled, universalOut1.association, mapping)
                    || enabledAndMapped(universalOut2.enabled, universalOut2.association, mapping)
        }
    }

}