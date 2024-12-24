package a75f.io.renatus.profiles.hyperstatv2.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuProfile
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getHpuFanLevel
import a75f.io.logic.bo.building.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HpuMinMaxConfig
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.logic.bo.building.system.logIt
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hyperstatv2.util.HyperStatViewStateUtil
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HpuViewState
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HyperStatV2ViewState
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

class HpuV2ViewModel(application: Application) : HyperStatViewModel(application) {

    override var viewState = mutableStateOf(HpuViewState() as HyperStatV2ViewState)

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getHyperStatHpuModel() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatHpuProfile) {
            hyperStatProfile = L.getProfile(deviceAddress) as HyperStatHpuProfile
            val equip = (hyperStatProfile as HyperStatHpuProfile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getConfiguration(equip.equipRef)!!.getActiveConfiguration()
            equipRef = equip.equipRef
        } else {
            profileConfiguration = HpuConfiguration(
                    nodeAddress = deviceAddress.toInt(), nodeType = nodeType.name, priority = 0,
                    roomRef = zoneRef, floorRef = floorRef, profileType = profileType, model = equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatViewStateUtil.hpuConfigToState(profileConfiguration as HpuConfiguration)
    }

    override fun saveConfiguration() {
        HyperStatViewStateUtil.hpuStateToConfig(viewState.value as HpuViewState, profileConfiguration as HpuConfiguration)
        if ( !profileConfiguration.isAnyRelayEnabledAssociated(association = HsHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal) &&
               !profileConfiguration.isAnyRelayEnabledAssociated(association = HsHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)) {
            showToast("Heatpump cannot be configured without enabling relay for ChangeOver valve", context)
            return
        }

        (viewState.value as HpuViewState).apply {
            logIt("Saving Configuration " + "$temperatureOffset  $isEnableAutoAway $isEnableAutoForceOccupied\n" +
                    "${relay1Config.enabled} ${relay1Config.association} " + "${relay2Config.enabled} ${relay2Config.association} "
                    + "${relay3Config.enabled} ${relay3Config.association} " + "${relay4Config.enabled} ${relay4Config.association} "
                    + "${relay5Config.enabled} ${relay5Config.association} " + "${relay6Config.enabled} ${relay6Config.association}  \n"
                    + "$analogOut1Enabled $analogOut1Association " + "$analogOut2Enabled $analogOut2Association "
                    + "$analogOut3Enabled $analogOut3Association  \n" + "${analogIn1Config.enabled} ${analogIn1Config.association}"
                    + "${analogIn2Config.enabled} ${analogIn2Config.association}" +
                    "${thermistor1Config.enabled} ${thermistor1Config.association}" +
                    "${thermistor2Config.enabled} ${thermistor2Config.association}\n" +
                    "co2Config ${co2Config.threshold} ${co2Config.target}\n" +
                    "pm2p5Config ${pm2p5Config.threshold} ${pm2p5Config.target} damperOpeningRate $damperOpeningRate\n" +
                    "a1 coolingConfig ${analogOut1MinMax.compressorConfig.min} ${analogOut1MinMax.compressorConfig.max}\n" +
                    "a1 dcv ${analogOut1MinMax.dcvDamperConfig.min} ${analogOut1MinMax.dcvDamperConfig.max}\n" +

                    "a1 stagedFanSpeedConfig ${analogOut1MinMax.fanSpeedConfig.min} ${analogOut1MinMax.fanSpeedConfig.max}\n" +
                    "a1 analogOut1FanConfig ${analogOut1FanConfig.low} ${analogOut1FanConfig.medium} ${analogOut1FanConfig.high} \n" +
                    "a2 coolingConfig ${analogOut2MinMax.compressorConfig.min} ${analogOut2MinMax.compressorConfig.max}\n" +
                    "a2 linearFanSpeedConfig ${analogOut2MinMax.fanSpeedConfig.min} ${analogOut2MinMax.fanSpeedConfig.max}\n" +
                    "a2 dcvDamperConfig ${analogOut2MinMax.dcvDamperConfig.min} ${analogOut2MinMax.dcvDamperConfig.max}\n" +

                    "a2 analogOut2FanConfig ${analogOut2FanConfig.low} ${analogOut2FanConfig.medium} ${analogOut2FanConfig.high} \n" +
                    "a3 coolingConfig ${analogOut3MinMax.compressorConfig.min} ${analogOut3MinMax.compressorConfig.max}\n" +
                    "a3 linearFanSpeedConfig ${analogOut3MinMax.fanSpeedConfig.min} ${analogOut3MinMax.fanSpeedConfig.max}\n" +
                    "a3 dcvDamperConfig ${analogOut3MinMax.dcvDamperConfig.min} ${analogOut3MinMax.dcvDamperConfig.max}\n" +

                    "a3 analogOut3FanConfig ${analogOut3FanConfig.low} ${analogOut3FanConfig.medium} ${analogOut3FanConfig.high} \n" +
                    "$humidityDisplay $co2Display $pm25Display"

            )
        }
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpHpuProfile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendHyperStatSeedMessage(deviceAddress, zoneRef, floorRef, false)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("Configuration  saved successfully", context)
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
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)

        if (profileConfiguration.isDefault) {
            val equipId = addEquipment(profileConfiguration as HpuConfiguration, equipModel, deviceModel)
            hyperStatProfile = HyperStatHpuProfile()
            (hyperStatProfile as HyperStatHpuProfile).addEquip(equipId)
            L.ccu().zoneProfiles.add(hyperStatProfile)
            val equip = HpuV2Equip(equipId)
            equip.conditioningMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
            updateFanMode(false,equip, getHpuFanLevel(profileConfiguration as HpuConfiguration))
            CcuLog.i(Domain.LOG_TAG, "Hpu profile added")

        } else {
            val equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
            val equip = HpuV2Equip(equipId)
            updateFanMode(true,equip, getHpuFanLevel(profileConfiguration as HpuConfiguration))
        }
        profileConfiguration.apply { setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap()) }
    }

    private fun addEquipment(config: HpuConfiguration, equipModel: SeventyFiveFProfileDirective, deviceModel: SeventyFiveFDeviceDirective): String {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val equipId = equipBuilder.buildEquipAndPoints(config, equipModel, hayStack.site!!.id, getEquipDis())
        deviceBuilder.buildDeviceAndPoints(config, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        return equipId
    }
}