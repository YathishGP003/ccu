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
        setPortConfiguration(profileConfiguration, getRelayMap(), getAnalogMap())
    }

    private fun getAnalogMap(): Map<String, Pair<Boolean, String>> {
        val analogOuts = mutableMapOf<String, Pair<Boolean, String>>()
        analogOuts[DomainName.analog1Out] = Pair(isAnalogExternalMapped(profileConfiguration.analogOut1Enabled, profileConfiguration.analogOut1Association), analogType(profileConfiguration.analogOut1Enabled))
        analogOuts[DomainName.analog2Out] = Pair(isAnalogExternalMapped(profileConfiguration.analogOut2Enabled, profileConfiguration.analogOut2Association), analogType(profileConfiguration.analogOut2Enabled))
        analogOuts[DomainName.analog3Out] = Pair(isAnalogExternalMapped(profileConfiguration.analogOut3Enabled, profileConfiguration.analogOut3Association), analogType(profileConfiguration.analogOut3Enabled))
        return analogOuts
    }

    private fun getRelayMap(): Map<String, Boolean> {
        val relays = mutableMapOf<String, Boolean>()
        relays[DomainName.relay1] = isRelayExternalMapped(profileConfiguration.relay1Enabled, profileConfiguration.relay1Association)
        relays[DomainName.relay2] = isRelayExternalMapped(profileConfiguration.relay2Enabled, profileConfiguration.relay2Association)
        relays[DomainName.relay3] = isRelayExternalMapped(profileConfiguration.relay3Enabled, profileConfiguration.relay3Association)
        relays[DomainName.relay4] = isRelayExternalMapped(profileConfiguration.relay4Enabled, profileConfiguration.relay4Association)
        relays[DomainName.relay5] = isRelayExternalMapped(profileConfiguration.relay5Enabled, profileConfiguration.relay5Association)
        relays[DomainName.relay6] = isRelayExternalMapped(profileConfiguration.relay6Enabled, profileConfiguration.relay6Association)
        return relays
    }

    private fun analogType(analogOutPort: EnableConfig): String {
        (profileConfiguration as Pipe2Configuration).apply {
            return when (analogOutPort) {
                analogOut1Enabled -> getPortType(analogOut1Association, analogOut1MinMaxConfig)
                analogOut2Enabled -> getPortType(analogOut2Association, analogOut2MinMaxConfig)
                analogOut3Enabled -> getPortType(analogOut3Association, analogOut3MinMaxConfig)
                else -> "0-10v"
            }
        }

    }

    private fun getPortType(association: AssociationConfig, minMaxConfig: Pipe2MinMaxConfig): String {
        val portType: String
        when (association.associationVal) {

            HsPipe2AnalogOutMapping.WATER_MODULATING_VALUE.ordinal -> {
                portType = "${minMaxConfig.waterModulatingValue.min.currentVal.toInt()}-${minMaxConfig.waterModulatingValue.max.currentVal.toInt()}v"
            }

            HsPipe2AnalogOutMapping.FAN_SPEED.ordinal -> {
                portType = "${minMaxConfig.fanSpeedConfig.min.currentVal.toInt()}-${minMaxConfig.fanSpeedConfig.max.currentVal.toInt()}v"
            }

            HsPipe2AnalogOutMapping.DCV_DAMPER.ordinal -> {
                portType = "${minMaxConfig.dcvDamperConfig.min.currentVal.toInt()}-${minMaxConfig.dcvDamperConfig.max.currentVal.toInt()}v"
            }

            else -> {
                portType = "0-10v"
            }
        }
        return portType
    }

    private fun isRelayExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == HsPipe2RelayMapping.EXTERNALLY_MAPPED.ordinal)

    private fun isAnalogExternalMapped(enabled: EnableConfig, association: AssociationConfig) = (enabled.enabled && association.associationVal == HsPipe2AnalogOutMapping.EXTERNALLY_MAPPED.ordinal)

    private fun addEquipment(config: Pipe2Configuration, equipModel: SeventyFiveFProfileDirective, deviceModel: SeventyFiveFDeviceDirective): String {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val equipId = equipBuilder.buildEquipAndPoints(config, equipModel, hayStack.site!!.id, getEquipDis())
        deviceBuilder.buildDeviceAndPoints(config, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        return equipId
    }

}