package a75f.io.renatus.profiles.mystat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatPipe4Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4RelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatPipe4Profile
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe4FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleFanModeSettings
import a75f.io.logic.bo.building.statprofiles.util.updateConditioningMode
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.mystat.viewstates.MyStatPipe4ViewState
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

class MyStatPipe4ViewModel(application: Application) : MyStatViewModel(application) {
    override var viewState = mutableStateOf(MyStatPipe4ViewState() as MyStatViewState)
    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getMyStatPipe4Model() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is MyStatPipe4Profile) {
            myStatProfile = (L.getProfile(deviceAddress) as MyStatPipe4Profile)
            val equip =
                (myStatProfile as MyStatPipe4Profile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getMyStatConfiguration(equip.equipRef)!!
            equipRef = equip.equipRef
        } else {
            profileConfiguration = MyStatPipe4Configuration(
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
        profileConfiguration.getAnalogOutDefaultValueForMyStatV1(profileConfiguration,devicesVersion)
        viewState.value = MyStatViewStateUtil.pipe4ConfigToState(
            profileConfiguration as MyStatPipe4Configuration,
            MyStatPipe4ViewState()
        )
    }

    override fun getAnalogStatIndex() = MyStatPipe4RelayMapping.values().size

    override fun saveConfiguration() {
        if (saveJob == null && isValidConfiguration(viewState.value.isDcvMapped())) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPipe4Profile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendMyStatSeedMessage(deviceAddress, zoneRef, floorRef)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("MyStat 4 Pipe Configuration saved successfully", context)
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
        MyStatViewStateUtil.pipe4StateToConfig(
            viewState.value as MyStatPipe4ViewState,
            profileConfiguration as MyStatPipe4Configuration
        )
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(
                profileConfiguration as MyStatPipe4Configuration, equipModel, deviceModel
            )
            myStatProfile = MyStatPipe4Profile()
            (myStatProfile as MyStatPipe4Profile).addEquip(equipId)
            L.ccu().zoneProfiles.add(myStatProfile)
            val equip = MyStatPipe4Equip(equipId)
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
            updateFanMode(
                false,
                equip,
                getMyStatPipe4FanLevel(profileConfiguration as MyStatPipe4Configuration)
            )
            updateDeviceVersionTypePointVal(equipId)
            CcuLog.i(Domain.LOG_TAG, "Pipe4 profile added")
        } else {
            equipId = equipBuilder.updateEquipAndPoints(
                profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true
            )
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            val deviceRef = deviceBuilder.updateDeviceAndPoints(
                profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis()
            )
            val equip = MyStatPipe4Equip(equipId)
            updateConditioningMode(profileConfiguration, equip)
            updateFanMode(
                true,
                equip,
                getMyStatPipe4FanLevel(profileConfiguration as MyStatPipe4Configuration)
            )
            profileConfiguration.universalInUnit(deviceRef)
        }

        profileConfiguration.apply {
            val possibleConditioningMode = getMyStatPossibleConditionMode(profileConfiguration)
            val possibleFanMode =
                getMyStatPossibleFanModeSettings(getMyStatPipe4FanLevel(profileConfiguration as MyStatPipe4Configuration))
            val equip = MyStatPipe4Equip(equipId)
            modifyFanMode(possibleFanMode, equip.fanOpMode)
            modifyConditioningMode(
                possibleConditioningMode.ordinal,
                equip.conditioningMode,
                allStandaloneProfileConditions
            )
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
            updateEnumConfigs(equip, devicesVersion)
        }
        DesiredTempDisplayMode.setModeTypeOnUserIntentChange(zoneRef, CCUHsApi.getInstance())
    }
}