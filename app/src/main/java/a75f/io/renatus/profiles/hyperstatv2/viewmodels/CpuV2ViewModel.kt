package a75f.io.renatus.profiles.hyperstatv2.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.cpu.HyperStatCpuProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getHsPossibleFanModeSettings
import a75f.io.logic.bo.building.statprofiles.util.getPossibleConditionMode
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hyperstatv2.util.HyperStatViewStateUtil
import a75f.io.renatus.profiles.hyperstatv2.viewstates.CpuViewState
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HyperStatV2ViewState
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
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

class CpuV2ViewModel(application: Application) : HyperStatViewModel(application) {

    override var viewState = mutableStateOf(CpuViewState() as HyperStatV2ViewState)

    override fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        super.init(bundle, context, hayStack)

        equipModel = ModelLoader.getHyperStatCpuModel() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatCpuProfile) {
            hyperStatProfile = (L.getProfile(deviceAddress) as HyperStatCpuProfile)
            val equip = (hyperStatProfile as HyperStatCpuProfile).getProfileDomainEquip(deviceAddress.toInt())
            profileConfiguration = getHsConfiguration(equip.equipRef)!!.getActiveConfiguration()
            equipRef = equip.equipRef
        } else {
            profileConfiguration = CpuConfiguration(
                    nodeAddress = deviceAddress.toInt(), nodeType = nodeType.name, priority = 0,
                    roomRef = zoneRef, floorRef = floorRef, profileType = profileType, model = equipModel
            ).getDefaultConfiguration()
        }

        viewState.value = HyperStatViewStateUtil.cpuConfigToState(profileConfiguration as CpuConfiguration)
        isCopiedConfigurationAvailable()
    }

    override fun saveConfiguration() {
        (viewState.value as CpuViewState).apply {

            logIt("Saving Configuration " + "$temperatureOffset  $isEnableAutoAway $isEnableAutoForceOccupied\n" + "${relay1Config.enabled} ${relay1Config.association} " + "${relay2Config.enabled} ${relay2Config.association} " + "${relay3Config.enabled} ${relay3Config.association} " + "${relay4Config.enabled} ${relay4Config.association} " + "${relay5Config.enabled} ${relay5Config.association} " + "${relay6Config.enabled} ${relay6Config.association}  \n" + "$analogOut1Enabled $analogOut1Association " + "$analogOut2Enabled $analogOut2Association " + "$analogOut3Enabled $analogOut3Association  \n" + "${analogIn1Config.enabled} ${analogIn1Config.association}" + "${analogIn2Config.enabled} ${analogIn2Config.association}" + "${thermistor1Config.enabled} ${thermistor1Config.association}" + "${thermistor2Config.enabled} ${thermistor2Config.association}\n" + "co2Config ${co2Config.threshold} ${co2Config.target}\n" + "pm2p5Config ${pm2p5Config.threshold} ${pm2p5Config.target} damperOpeningRate $damperOpeningRate\n" + "a1 coolingConfig ${analogOut1MinMax.coolingConfig.min} ${analogOut1MinMax.coolingConfig.max}\n" + "a1 linearFanSpeedConfig ${analogOut1MinMax.linearFanSpeedConfig.min} ${analogOut1MinMax.linearFanSpeedConfig.max}\n" + "a1 heatingConfig ${analogOut1MinMax.heatingConfig.min} ${analogOut1MinMax.heatingConfig.max}\n" + "a1 dcvDamperConfig ${analogOut1MinMax.dcvDamperConfig.min} ${analogOut1MinMax.dcvDamperConfig.max}\n" + "a1 stagedFanSpeedConfig ${analogOut1MinMax.stagedFanSpeedConfig.min} ${analogOut1MinMax.stagedFanSpeedConfig.max}\n" + "a1 analogOut1FanConfig ${analogOut1FanConfig.low} ${analogOut1FanConfig.medium} ${analogOut1FanConfig.high} \n" + "a2 coolingConfig ${analogOut2MinMax.coolingConfig.min} ${analogOut2MinMax.coolingConfig.max}\n" + "a2 linearFanSpeedConfig ${analogOut2MinMax.linearFanSpeedConfig.min} ${analogOut2MinMax.linearFanSpeedConfig.max}\n" + "a2 heatingConfig ${analogOut2MinMax.heatingConfig.min} ${analogOut2MinMax.heatingConfig.max}\n" + "a2 dcvDamperConfig ${analogOut2MinMax.dcvDamperConfig.min} ${analogOut2MinMax.dcvDamperConfig.max}\n" + "a2 stagedFanSpeedConfig ${analogOut2MinMax.stagedFanSpeedConfig.min} ${analogOut2MinMax.stagedFanSpeedConfig.max}\n" + "a2 analogOut2FanConfig ${analogOut2FanConfig.low} ${analogOut2FanConfig.medium} ${analogOut2FanConfig.high} \n" + "a3 coolingConfig ${analogOut3MinMax.coolingConfig.min} ${analogOut3MinMax.coolingConfig.max}\n" + "a3 linearFanSpeedConfig ${analogOut3MinMax.linearFanSpeedConfig.min} ${analogOut3MinMax.linearFanSpeedConfig.max}\n" + "a3 heatingConfig ${analogOut3MinMax.heatingConfig.min} ${analogOut3MinMax.heatingConfig.max}\n" + "a3 dcvDamperConfig ${analogOut3MinMax.dcvDamperConfig.min} ${analogOut3MinMax.dcvDamperConfig.max}\n" + "a3 stagedFanSpeedConfig ${analogOut3MinMax.stagedFanSpeedConfig.min} ${analogOut3MinMax.stagedFanSpeedConfig.max}\n" + "a3 analogOut3FanConfig ${analogOut3FanConfig.low} ${analogOut3FanConfig.medium} ${analogOut3FanConfig.high} \n" + "coolingStageFanConfig ${coolingStageFanConfig.stage1} ${coolingStageFanConfig.stage2} ${coolingStageFanConfig.stage3}\n" + "heatingStageFanConfig ${heatingStageFanConfig.stage1} ${heatingStageFanConfig.stage2} ${heatingStageFanConfig.stage3}\n" + "recirculateFanConfig ${recirculateFanConfig.analogOut1} ${recirculateFanConfig.analogOut2} ${recirculateFanConfig.analogOut3}\n" + "$humidityDisplay $co2Display $pm25Display"

            )
        }
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpCpuProfile()
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                LSerial.getInstance().sendHyperStatSeedMessage(deviceAddress, zoneRef, floorRef, false)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("Configuration saved successfully", context)
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
        HyperStatViewStateUtil.cpuStateToConfig(viewState.value as CpuViewState, profileConfiguration as CpuConfiguration)
        profileConfiguration.nodeType = nodeType.name
        profileConfiguration.nodeAddress = deviceAddress.toInt()
        profileConfiguration.priority = ZonePriority.NONE.ordinal

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId: String
        if (profileConfiguration.isDefault) {
            equipId = addEquipment(profileConfiguration as CpuConfiguration, equipModel, deviceModel)
            hyperStatProfile = HyperStatCpuProfile()
            (hyperStatProfile as HyperStatCpuProfile).addEquip(equipId)
            L.ccu().zoneProfiles.add(hyperStatProfile)
            val equip = CpuV2Equip(equipId)
            setConditioningMode(equip)
            updateFanMode(false,equip, getCpuFanLevel(profileConfiguration as CpuConfiguration))
            CcuLog.i(Domain.LOG_TAG, "Cpu profile added")
        } else {
             equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, getEquipDis(), true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
            val equip = CpuV2Equip(equipId)
            updateConditioningMode(equip)
            updateFanMode(true,equip, getCpuFanLevel(profileConfiguration as CpuConfiguration))
            CcuLog.i(Domain.LOG_TAG, "Cpu profile reconfigured")
        }
        profileConfiguration.apply {
            val possibleConditioningMode = getPossibleConditionMode(profileConfiguration)
            val possibleFanMode = getHsPossibleFanModeSettings(getCpuFanLevel(profileConfiguration as CpuConfiguration))
            val equip = CpuV2Equip(equipId)
            modifyFanMode(possibleFanMode.ordinal, equip.fanOpMode)
            modifyConditioningMode(possibleConditioningMode.ordinal, equip.conditioningMode, allStandaloneProfileConditions)
            setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap())
            DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, CCUHsApi.getInstance())
        }
    }

    private fun setConditioningMode(equip: CpuV2Equip) {

        val possible = getPossibleConditionMode(profileConfiguration)
        var newMode = StandaloneConditioningMode.OFF
        if (possible == PossibleConditioningMode.BOTH) newMode = StandaloneConditioningMode.AUTO
        if (possible == PossibleConditioningMode.HEATONLY) newMode = StandaloneConditioningMode.HEAT_ONLY
        if (possible == PossibleConditioningMode.COOLONLY) newMode = StandaloneConditioningMode.COOL_ONLY
        equip.conditioningMode.writePointValue(newMode.ordinal.toDouble())
    }

    private fun updateConditioningMode(equip: CpuV2Equip) {
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

    private fun addEquipment(config: CpuConfiguration, equipModel: SeventyFiveFProfileDirective, deviceModel: SeventyFiveFDeviceDirective): String {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val equipId = equipBuilder.buildEquipAndPoints(config, equipModel, hayStack.site!!.id, getEquipDis())
        deviceBuilder.buildDeviceAndPoints(config, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        return equipId
    }

    fun isAnyRelayMappedToStage(mapping: HsCpuRelayMapping): Boolean {
        fun enabledAndMapped(enabled: Boolean, association: Int, mapping: HsCpuRelayMapping) = enabled && association == mapping.ordinal

        (viewState.value as CpuViewState).apply {
            return enabledAndMapped(relay1Config.enabled, relay1Config.association, mapping) || enabledAndMapped(relay2Config.enabled, relay2Config.association, mapping) || enabledAndMapped(relay3Config.enabled, relay3Config.association, mapping) || enabledAndMapped(relay4Config.enabled, relay4Config.association, mapping) || enabledAndMapped(relay5Config.enabled, relay5Config.association, mapping) || enabledAndMapped(relay6Config.enabled, relay6Config.association, mapping)
        }
    }

    fun isAnyAnalogOutMappedToStage(mapping: HsCpuAnalogOutMapping): Boolean {
        fun enabledAndMapped(enabled: Boolean, association: Int, mapping: HsCpuAnalogOutMapping) = enabled && association == mapping.ordinal
        (viewState.value as CpuViewState).apply {
            return enabledAndMapped(analogOut1Enabled, analogOut1Association, mapping) || enabledAndMapped(analogOut2Enabled, analogOut2Association, mapping) || enabledAndMapped(analogOut3Enabled, analogOut3Association, mapping)
        }
    }

    fun isConditioningConfigExist(): Boolean {
        (viewState.value as CpuViewState).apply {
            return (isAnyRelayMappedToStage(HsCpuRelayMapping.COOLING_STAGE_1) || isAnyRelayMappedToStage(HsCpuRelayMapping.COOLING_STAGE_2) || isAnyRelayMappedToStage(HsCpuRelayMapping.COOLING_STAGE_3) || isAnyRelayMappedToStage(HsCpuRelayMapping.HEATING_STAGE_1) || isAnyRelayMappedToStage(HsCpuRelayMapping.HEATING_STAGE_2) || isAnyRelayMappedToStage(HsCpuRelayMapping.HEATING_STAGE_3))
        }
    }
}