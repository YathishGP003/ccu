package a75f.io.renatus.profiles.hyperstatv2.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPipe2FanLevel
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2AnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.hyperstat.v2.configs.Pipe2MinMaxConfig
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hyperstatv2.util.HyperStatViewStateUtil
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HyperStatV2ViewState
import a75f.io.renatus.profiles.hyperstatv2.viewstates.Pipe2ViewState
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

class Pipe2ViewModel(application: Application) : HyperStatViewModel(application) {

    override var viewState = mutableStateOf(Pipe2ViewState() as HyperStatV2ViewState)

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getHyperStatPipe2Model() as SeventyFiveFProfileDirective

        profileConfiguration = if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatPipe2Profile) {

            Pipe2Configuration(deviceAddress.toInt(), nodeType.name, 0, zoneRef, floorRef, profileType, equipModel).getActiveConfiguration()
        } else {
            Pipe2Configuration(deviceAddress.toInt(), nodeType.name, 0, zoneRef, floorRef, profileType, equipModel).getDefaultConfiguration()
        }

        viewState.value = HyperStatViewStateUtil.pipe2ConfigToState(profileConfiguration as Pipe2Configuration)
    }

    override fun saveConfiguration() {
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
        HyperStatViewStateUtil.pipe2StateToConfig(viewState.value as Pipe2ViewState, profileConfiguration as Pipe2Configuration)
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)

        if (profileConfiguration.isDefault) {
            val equipId = addEquipment(profileConfiguration as Pipe2Configuration, equipModel, deviceModel)
            hyperStatProfile = HyperStatPipe2Profile()
            (hyperStatProfile as HyperStatPipe2Profile).addEquip(equipId)
            L.ccu().zoneProfiles.add(hyperStatProfile)
            val equip = Pipe2V2Equip(equipId)
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
            updateFanMode(false,equip, getPipe2FanLevel(profileConfiguration as Pipe2Configuration))
            CcuLog.i(Domain.LOG_TAG, "Pipe2 profile added")
        } else {
            val equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        }
        profileConfiguration.apply { setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap()) }
    }

    private fun addEquipment(config: Pipe2Configuration, equipModel: SeventyFiveFProfileDirective, deviceModel: SeventyFiveFDeviceDirective): String {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val equipId = equipBuilder.buildEquipAndPoints(config, equipModel, hayStack.site!!.id, getEquipDis())
        deviceBuilder.buildDeviceAndPoints(config, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        return equipId
    }

}