package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelSource
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.vav.VavProfile
import a75f.io.logic.bo.building.vav.VavReheatProfile
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlin.properties.Delegates

class VavProfileViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var vavProfile: VavProfile
    lateinit var profileConfiguration: VavProfileConfiguration

    private lateinit var model : SeventyFiveFProfileDirective
    private lateinit var deviceModel : SeventyFiveFDeviceDirective
    lateinit var viewState: VavConfigViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var damperTypesList: List<String>
    lateinit var damperSizesList: List<String>
    lateinit var damperShapesList: List<String>
    lateinit var reheatTypesList: List<String>
    lateinit var zonePrioritiesList: List<String>

    lateinit var temperatureOffsetsList: List<String>

    lateinit var maxCoolingDamperPosList: List<String>
    lateinit var minCoolingDamperPosList: List<String>
    lateinit var maxHeatingDamperPosList: List<String>
    lateinit var minHeatingDamperPosList: List<String>

    lateinit var kFactorsList: List<String>

    lateinit var maxCFMCoolingList: List<String>
    lateinit var minCFMCoolingList: List<String>
    lateinit var maxCFMReheatingList: List<String>
    lateinit var minCFMReheatingList: List<String>

    private val _isDialogOpen = MutableLiveData<Boolean>()
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen

    fun init(bundle: Bundle, context: Context, hayStack : CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        val profileOriginalValue = bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)
        profileType = ProfileType.values()[profileOriginalValue]

        // Models are temporarily loaded from local files to allow quick model revisions during development.
        // In the released CCU build, these will draw from the Hayloft API.
        model = ModelSource.getProfileModelByFileName("nickTestSmartNodeVAVReheatNoFan_v0.0.1") as SeventyFiveFProfileDirective // ModelSource.getModelByProfileName("smartnodeVAVReheatNoFan") as SeventyFiveFProfileDirective
        deviceModel = ModelSource.getDeviceModelByFileName("nickTestSmartNodeDevice_v0.0.0") as SeventyFiveFDeviceDirective //deviceModel = ModelSource.getModelByProfileName("nickTestSmartNodeDevice") as SeventyFiveFDeviceDirective


        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is VavProfile) {
            vavProfile = L.getProfile(deviceAddress) as VavProfile
            profileConfiguration = VavProfileConfiguration(deviceAddress.toInt(), NodeType.SMART_NODE.name, 0,
                zoneRef, floorRef , model ).getActiveConfiguration()
        } else {
            profileConfiguration = VavProfileConfiguration(deviceAddress.toInt(), NodeType.SMART_NODE.name, 0,
                zoneRef, floorRef , model ).getDefaultConfiguration()
            /*vavProfile = when (profileType) {
                ProfileType.VAV_PARALLEL_FAN -> VavParallelFanProfile()
                ProfileType.VAV_SERIES_FAN -> VavSeriesFanProfile()
                else -> VavReheatProfile()
            }*/
        }

        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())



        viewState = VavConfigViewState.fromVavProfileConfig(profileConfiguration)

        this.context = context
        this.hayStack = hayStack

        initializeLists()

    }

    private fun initializeLists() {
        damperTypesList = getListByDomainName(DomainName.damperType, model)
        damperSizesList = getListByDomainName(DomainName.damperSize, model)
        damperShapesList = getListByDomainName(DomainName.damperShape, model)
        reheatTypesList = getListByDomainName(DomainName.reheatType, model)
        zonePrioritiesList = getListByDomainName(DomainName.zonePriority, model)

        temperatureOffsetsList = getListByDomainName(DomainName.temperatureOffset, model)

        maxCoolingDamperPosList = getListByDomainName(DomainName.maxCoolingDamperPos, model)
        minCoolingDamperPosList = getListByDomainName(DomainName.minCoolingDamperPos, model)
        maxHeatingDamperPosList = getListByDomainName(DomainName.maxHeatingDamperPos, model)
        minHeatingDamperPosList = getListByDomainName(DomainName.minHeatingDamperPos, model)

        // TODO: In v0.0.1 of the model, this is a 2000-item dropdown. Very slow, but it doesn't crash the CCU.
        kFactorsList = getListByDomainName(DomainName.kFactor, model)

        maxCFMCoolingList = getListByDomainName(DomainName.maxCFMCooling, model)
        minCFMCoolingList = getListByDomainName(DomainName.minCFMCooling, model)
        maxCFMReheatingList = getListByDomainName(DomainName.maxCFMReheating, model)
        minCFMReheatingList = getListByDomainName(DomainName.minCFMReheating, model)
    }

    fun saveConfiguration() {
        CcuLog.i(Domain.LOG_TAG, " Save Profile : damperType ${viewState.damperType}")
        CcuLog.i(Domain.LOG_TAG, " Save Profile : damperSize ${viewState.damperSize}")

        RxjavaUtil.executeBackgroundTask({
            ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
        }, {
            CCUHsApi.getInstance().resetCcuReady()

            setUpVavProfile()
            CcuLog.i(Domain.LOG_TAG, "VavProfile Setup complete")
            L.saveCCUState()

            hayStack.syncEntityTree()
            CCUHsApi.getInstance().setCcuReady()
            CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
            LSerial.getInstance().sendSeedMessage(false,false, deviceAddress, zoneRef,floorRef)
            CcuLog.i(Domain.LOG_TAG, "VavProfile Pairing complete")
        }, {
            ProgressDialogUtils.hideProgressDialog()
            context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
            showToast("VAV Configuration saved successfully", context)
            _isDialogOpen.value = false
        })

        // TODO: Sam's original code. Some or all of this will be restored in a future cleanup operation.
        /*
        viewModelScope.launch {
            ProgressDialogUtils.showProgressDialog(context, "Saving VAV Configuration")
            withContext(Dispatchers.IO) {

                viewState.updateConfigFromViewState(profileConfiguration)
                val equipBuilder = ProfileEquipBuilder(hayStack)
                if (profileConfiguration.isDefault) {
                    equipBuilder.buildEquipAndPoints(profileConfiguration, model, hayStack.site!!.id)
                } else {
                    equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id)
                }

                withContext(Dispatchers.Main) {
                    ProgressDialogUtils.hideProgressDialog()
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                }
            }
        }
         */
    }

    private fun setUpVavProfile() {
        //DomainManager.buildDomain(CCUHsApi.getInstance())
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-VAV-" + profileConfiguration.nodeAddress

        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, floorRef, zoneRef, NodeType.SMART_NODE, hayStack, model, deviceModel)
            L.ccu().zoneProfiles.add(vavProfile)

        } else {
            equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis)
        }

    }

    private fun addEquipAndPoints(
        addr: Short,
        config: ProfileConfiguration,
        floorRef: String?,
        roomRef: String?,
        nodeType: NodeType?,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?
    ) {
        //val deviceMap = VavEquip(getProfileType(), addr)
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-VAV-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildEquipAndPoints")
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, equipDis
        )
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceDis = hayStack.siteName + "-SN-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
        CcuLog.i(Domain.LOG_TAG, " add Profile")
        vavProfile = VavReheatProfile(equipId, addr)
        //vavDeviceMap.put(addr, deviceMap)
        //vavEquip = VavEquip(equipId)

        //deviceMap.init();
    }
}
