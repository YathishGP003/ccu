package a75f.io.renatus.profiles.hyperstatv2.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.monitoring.HyperStatV2MonitoringProfile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.MonitoringConfiguration
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.getSchedule
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HyperStatV2ViewState
import a75f.io.renatus.profiles.hyperstatv2.viewstates.MonitoringViewState
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

class MonitoringModel(application: Application) : HyperStatViewModel(application) {

    private lateinit var monitoringProfile: HyperStatV2MonitoringProfile

    override var viewState = mutableStateOf(MonitoringViewState() as HyperStatV2ViewState)

    lateinit var temperatureOffset: List<String>
    private lateinit var thermistor1List: List<String>
    private lateinit var thermistor2List: List<String>
    private lateinit var analog1List: List<String>
    private lateinit var analog2List: List<String>


    override fun init(bundle: Bundle, context: Context, hayStack : CCUHsApi) {
        super.init(bundle, context, hayStack)
        equipModel = ModelLoader.getHyperStatMonitoringModel() as SeventyFiveFProfileDirective

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is HyperStatV2MonitoringProfile) {
            monitoringProfile = L.getProfile(deviceAddress) as HyperStatV2MonitoringProfile
            profileConfiguration = MonitoringConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, equipModel ).getActiveConfiguration()
        } else {
            profileConfiguration = MonitoringConfiguration(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, equipModel ).getDefaultConfiguration()
        }

        viewState.value = MonitoringViewState.fromMonitoringConfigToState(profileConfiguration as MonitoringConfiguration)
        initializeLists()
        isCopiedConfigurationAvailable()
        CcuLog.i(Domain.LOG_TAG, "Monitoring profile cofig Loaded")
    }

    private fun initializeLists() {
        temperatureOffset = Domain.getListByDomainName(DomainName.temperatureOffset, equipModel)
        thermistor1List = Domain.getListByDomainName(DomainName.thermistor1InputAssociation, equipModel)
        thermistor2List = Domain.getListByDomainName(DomainName.thermistor2InputAssociation, equipModel)
        analog1List = Domain.getListByDomainName(DomainName.analog1InputAssociation, equipModel)
        analog2List = Domain.getListByDomainName(DomainName.analog2InputAssociation, equipModel)
    }

    override fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Monitoring Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setMonitoringProfile()
                CcuLog.i(Domain.LOG_TAG, "Monitoring profile Setup complete")
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("Monitoring Configuration saved successfully", context)
                    CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }

                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                LSerial.getInstance().sendHyperStatSeedMessage(deviceAddress, zoneRef, floorRef, false)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                CcuLog.i(Domain.LOG_TAG, "Monitoring profile Pairing complete")

                // This check is needed because the dialog sometimes fails to close inside the coroutine.
                // We don't know why this happens.
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setMonitoringProfile() {
        MonitoringViewState.monitoringStateToConfig(viewState.value as MonitoringViewState, profileConfiguration as MonitoringConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)

        if (profileConfiguration.isDefault) {
            addEquipAndPoints(profileConfiguration, hayStack, equipModel, deviceModel)
            setScheduleType(profileConfiguration as MonitoringConfiguration)
            L.ccu().zoneProfiles.add(monitoringProfile)
        } else {
            val equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel,
                hayStack.site!!.id, getEquipDis(), true)
            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            deviceBuilder.updateDeviceAndPoints(profileConfiguration, deviceModel,
                equipId, hayStack.site!!.id, getDeviceDis())
            setScheduleType(profileConfiguration as MonitoringConfiguration)
        }
    }

    private fun addEquipAndPoints(
        config: ProfileConfiguration,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?
    ) {
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        CcuLog.i(Domain.LOG_TAG, " build Equip And Points ${equipModel.domainName} profileType ${config.profileType}" )
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, getEquipDis()
        )
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        CcuLog.i(Domain.LOG_TAG, " build Device And Points")
        deviceBuilder.buildDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            getDeviceDis()
        )
        CcuLog.i(Domain.LOG_TAG, " add Profile")
        monitoringProfile = HyperStatV2MonitoringProfile(equipId, config.nodeAddress.toShort())

    }
    private fun setScheduleType(config: MonitoringConfiguration) {
        val scheduleTypePoint = hayStack.readEntity("point and domainName == \"" + DomainName.scheduleType + "\" and group == \"" + config.nodeAddress + "\"")
        val scheduleTypeId = scheduleTypePoint["id"].toString()

        val roomSchedule = getSchedule(zoneRef, floorRef)
        if(roomSchedule.isZoneSchedule) {
            hayStack.writeDefaultValById(scheduleTypeId, 1.0)
        } else {
            hayStack.writeDefaultValById(scheduleTypeId, 2.0)
        }
    }
}