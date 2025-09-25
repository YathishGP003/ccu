package a75f.io.renatus.profiles.mystat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe2FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleFanModeSettings
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.mystat.viewstates.MyStatPipe2ViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewStateUtil
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.renatus.R
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatPipe2ViewModel(application: Application) : MyStatViewModel(application) {
    override var viewState = mutableStateOf(MyStatPipe2ViewState() as MyStatViewState)
    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getMyStatPipe2Model() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is MyStatPipe2Profile) {
            myStatProfile = (L.getProfile(deviceAddress) as MyStatPipe2Profile)
            val equip = (myStatProfile as MyStatPipe2Profile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getMyStatConfiguration(equip.equipRef)!!
            equipRef = equip.equipRef
        } else {
            profileConfiguration = MyStatPipe2Configuration(
                nodeAddress = deviceAddress.toInt(), nodeType = nodeType.name, priority = 0,
                roomRef = zoneRef, floorRef = floorRef, profileType = profileType, model = equipModel
            ).getDefaultConfiguration()
        }

        viewState.value =  MyStatViewStateUtil.pipe2ConfigToState(profileConfiguration as MyStatPipe2Configuration, MyStatPipe2ViewState())
    }

    override fun getAnalogStatIndex() = MyStatPipe2RelayMapping.values().size

    override fun saveConfiguration() {
        if (saveJob == null && isValidConfiguration(viewState.value.isDcvMapped())) {
            ProgressDialogUtils.showProgressDialog(context, context.getString(R.string.saving_configuration))
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPipe2Profile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendMyStatSeedMessage(deviceAddress, zoneRef, floorRef)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast(context.getString(R.string.mystat_2_pipe_config_saved), context)
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
        MyStatViewStateUtil.pipe2StateToConfig(
            viewState.value as MyStatPipe2ViewState,
            profileConfiguration as MyStatPipe2Configuration
        )
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(
                profileConfiguration as MyStatPipe2Configuration,
                equipModel,
                deviceModel
            )
             myStatProfile = MyStatPipe2Profile()
            (myStatProfile as MyStatPipe2Profile).addEquip(equipId)
            L.ccu().zoneProfiles.add(myStatProfile)
            val equip = MyStatPipe2Equip(equipId)
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
            updateFanMode(
                false,
                equip,
                getMyStatPipe2FanLevel(profileConfiguration as MyStatPipe2Configuration)
            )
            CcuLog.i(Domain.LOG_TAG, "Pipe2 profile added")
        } else {
            equipId = equipBuilder.updateEquipAndPoints(
                profileConfiguration,
                equipModel,
                hayStack.site!!.id,
                getEquipDis(),
                true
            )
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            val deviceRef = deviceBuilder.updateDeviceAndPoints(
                profileConfiguration,
                deviceModel,
                equipId,
                hayStack.site!!.id,
                getDeviceDis()
            )
            val equip = MyStatPipe2Equip(equipId)
            updateFanMode(true, equip, getMyStatPipe2FanLevel(profileConfiguration as MyStatPipe2Configuration))
            universalInUnit(profileConfiguration, deviceRef)
        }

        profileConfiguration.apply {
            val possibleConditioningMode = getMyStatPossibleConditionMode(profileConfiguration)
            val possibleFanMode = getMyStatPossibleFanModeSettings(getMyStatPipe2FanLevel(profileConfiguration as MyStatPipe2Configuration))
            val equip = MyStatPipe2Equip(equipId)
            modifyFanMode(possibleFanMode, equip.fanOpMode)
            modifyConditioningMode(possibleConditioningMode.ordinal, equip.conditioningMode, allStandaloneProfileConditions)
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
        }
    }

}