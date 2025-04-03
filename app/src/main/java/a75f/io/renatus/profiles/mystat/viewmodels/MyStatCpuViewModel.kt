package a75f.io.renatus.profiles.mystat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.mystat.profiles.packageunit.cpu.MyStatCpuProfile
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.mystat.profiles.util.updateConditioningMode
import a75f.io.logic.bo.building.mystat.profiles.util.getMyStatCpuFanLevel
import a75f.io.logic.bo.building.mystat.profiles.util.setConditioningMode
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.mystat.viewstates.MyStatCpuViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewStateUtil
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
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

        viewState.value = MyStatViewStateUtil.cpuConfigToState(
            profileConfiguration as MyStatCpuConfiguration,
            MyStatCpuViewState()
        )
    }

    override fun saveConfiguration() {
        if (saveJob == null && isValidConfiguration(viewState.value.isDcvMapped())) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
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
                    showToast("MyStat Cpu Configuration saved successfully", context)
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

        if (profileConfiguration.isDefault) {
            val equipId = addEquipment(
                profileConfiguration as MyStatCpuConfiguration, equipModel, deviceModel
            )
            myStatProfile = MyStatCpuProfile()
            (myStatProfile as MyStatCpuProfile).addEquip(equipId)
            L.ccu().zoneProfiles.add(myStatProfile)
            val equip = MyStatCpuEquip(equipId)
            setConditioningMode(profileConfiguration as MyStatCpuConfiguration, equip)
            updateFanMode(
                false, equip, getMyStatCpuFanLevel(profileConfiguration as MyStatCpuConfiguration)
            )
            CcuLog.i(Domain.LOG_TAG, "MyStatCpu profile added")
        } else {
            val equipId = equipBuilder.updateEquipAndPoints(
                profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true
            )
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
            universalInUnit(profileConfiguration, deviceRef)
        }

        profileConfiguration.apply {
            setPortConfiguration(
                nodeAddress, getRelayMap(), getAnalogMap()
            )
        }
    }

    fun isAnyRelayMappedToState(mapping: MyStatCpuRelayMapping): Boolean {
        fun enabledAndMapped(enabled: Boolean, association: Int, mapping: MyStatCpuRelayMapping) = enabled && association == mapping.ordinal
        (viewState.value as MyStatCpuViewState).apply {
            return enabledAndMapped(relay1Config.enabled, relay1Config.association, mapping) || enabledAndMapped(relay2Config.enabled, relay2Config.association, mapping) || enabledAndMapped(relay3Config.enabled, relay3Config.association, mapping) || enabledAndMapped(relay4Config.enabled, relay4Config.association, mapping)
        }
    }

}