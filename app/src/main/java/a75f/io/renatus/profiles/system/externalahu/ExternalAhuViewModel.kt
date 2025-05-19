package a75f.io.renatus.profiles.system.externalahu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.bacnet.parser.BacnetProperty
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.util.hayStack
import a75f.io.device.modbus.buildModbusModel
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelNames.DAB_EXTERNAL_AHU_CONTROLLER
import a75f.io.domain.util.ModelNames.VAV_EXTERNAL_AHU_CONTROLLER
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.bacnet.BacnetProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusProfile
import a75f.io.logic.bo.building.system.BacnetReadRequestMultiple
import a75f.io.logic.bo.building.system.BacnetWhoIsRequest
import a75f.io.logic.bo.building.system.BroadCast
import a75f.io.logic.bo.building.system.DestinationMultiRead
import a75f.io.logic.bo.building.system.ObjectIdentifierBacNet
import a75f.io.logic.bo.building.system.PropertyReference
import a75f.io.logic.bo.building.system.ReadRequestMultiple
import a75f.io.logic.bo.building.system.RpmRequest
import a75f.io.logic.bo.building.system.SystemProfile
import a75f.io.logic.bo.building.system.WhoIsRequest
import a75f.io.logic.bo.building.system.addSystemEquip
import a75f.io.logic.bo.building.system.client.BaseResponse
import a75f.io.logic.bo.building.system.client.CcuService
import a75f.io.logic.bo.building.system.client.MultiReadResponse
import a75f.io.logic.bo.building.system.client.RpResponseMultiReadItem
import a75f.io.logic.bo.building.system.client.ServiceManager
import a75f.io.logic.bo.building.system.client.WhoIsResponseItem
import a75f.io.logic.bo.building.system.dab.DabExternalAhu
import a75f.io.logic.bo.building.system.getConfiguration
import a75f.io.logic.bo.building.system.vav.VavExternalAhu
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.PreferenceUtil
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.logic.util.bacnet.buildBacnetModelSystem
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.bacnet.models.BacnetDevice
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.bacnet.models.BacnetPointState
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.RegisterItemForSubEquip
import a75f.io.renatus.modbus.util.BACNET_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.LOADING_BACNET_MODELS
import a75f.io.renatus.modbus.util.MODBUS_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.NO_INTERNET
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.OnItemSelectBacnetDevice
import a75f.io.renatus.modbus.util.SAVED
import a75f.io.renatus.modbus.util.SAVING_BACNET
import a75f.io.renatus.modbus.util.formattedToastMessage
import a75f.io.renatus.modbus.util.getBacnetPoints
import a75f.io.renatus.modbus.util.getParameters
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.modbus.util.getSlaveIds
import a75f.io.renatus.modbus.util.isAllLeftParamsSelected
import a75f.io.renatus.modbus.util.isAllParamsSelectedBacNet
import a75f.io.renatus.modbus.util.isAllRightParamsSelected
import a75f.io.renatus.modbus.util.parseModbusDataFromString
import a75f.io.renatus.modbus.util.showErrorDialog
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.CopyConfiguration
import a75f.io.renatus.profiles.oao.updateOaoPoints
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonParseException
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList
import org.json.JSONException
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.text.DecimalFormat
import kotlin.properties.Delegates

/**
 * Created by Manjunath K on 08-08-2022.
 */

class ExternalAhuViewModel(application: Application) : AndroidViewModel(application) {

    var deviceList = mutableStateOf(emptyList<String>())
    var slaveIdList = mutableStateOf(emptyList<String>())
    var childSlaveIdList = mutableStateOf(emptyList<String>())
    var equipModel = mutableStateOf(EquipModel())
    var selectedModbusType = mutableStateOf(0)
    var modelName = mutableStateOf("Select Model")

    var configType = mutableStateOf(ConfigType.BACNET)
    lateinit var deviceModelList: List<ModelMetaData>
    lateinit var profileModelDefinition: SeventyFiveFProfileDirective
    private var selectedSlaveId by Delegates.notNull<Short>()
    var configModel = mutableStateOf(ExternalAhuConfigModel())
    var systemProfile: SystemProfile? = null
    private lateinit var modbusProfile: ModbusProfile

    // bacnet properties start
    lateinit var bacnetDeviceModelList: List<ModelMetaData>
    var bacnetDeviceList = mutableStateOf(emptyList<String>())
    private var selectedSlaveIdBacnet = 0L //by Delegates.notNull<Long>()
    var isErrorMsg = mutableStateOf(false)
    var errorMsg = ""
    var isDeviceIdValid = mutableStateOf(true)
    val bacnetRequestFailed = mutableStateOf(false)
    val bacnetPropertiesFetched = mutableStateOf(false)
    var isStateChanged = mutableStateOf(false)
    var isDeviceValueSelected = mutableStateOf(false)
    val isConnectedDevicesSearchFinished = mutableStateOf(false)
    var connectedDevices = mutableStateOf(emptyList<BacnetDevice>())
    var destinationIp  = mutableStateOf("")
    var deviceId  = mutableStateOf("")
    var destinationPort  = mutableStateOf("")
    var destinationMacAddress  = mutableStateOf("")
    var dnet = mutableStateOf("0")
    val isUpdateMode = mutableStateOf(false)
    private val _isDisabled = MutableLiveData(false)
    val isDisabled: LiveData<Boolean> = _isDisabled
    var bacnetModel = mutableStateOf(BacnetModel())
    private lateinit var bacnetProfile: BacnetProfile
    var deviceSelectionMode = mutableStateOf(0)
    var isDestinationIpValid = mutableStateOf(true)
    val displayInUi = mutableStateOf(true)
    val deviceValue = mutableStateOf(false)
    private lateinit var service : CcuService
    lateinit var deviceIp: String
    lateinit var devicePort: String
    private val _isDialogOpen = MutableLiveData<Boolean>()
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen
    lateinit var moduleLevel: String
    lateinit var zoneRef: String
    lateinit var floorRef: String
    val bacNetItemsMap = hashMapOf<String, RpResponseMultiReadItem>()
    // bacnet properties end

    val TAG: String = "CCU_SYSTEM"
    val TAG_BACNET: String = "ExternalAHU_BACNET"
    var externalBacnetEquipCreated : HashMap<Any, Any>? = null

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context
    lateinit var profileType: ProfileType
    val isBacntEnabled = mutableStateOf(false)

    private var domainService = DomainService()
    val onItemSelect = object : OnItemSelect {
        override fun onItemSelected(index: Int, item: String) {
            selectedModbusType.value = index
            modelName.value = item
            ProgressDialogUtils.showProgressDialog(context, "Fetching $item details")
            fetchModelDetails(item)
        }
    }

    val onBacnetItemSelect = object : OnItemSelect {
        override fun onItemSelected(index: Int, item: String) {
            CcuLog.i(TAG_BACNET, "bacnet onBacnetItemSelect")
            modelName.value = item
            ProgressDialogUtils.showProgressDialog( context, "Fetching $item details")
            fetchBacnetModelDetails(item)
            isCopiedConfigurationAvailable()
        }
    }

    fun configModbusDetails() {
        CcuLog.i(TAG, "configModbusDetails")
        if (!equipModel.value.isDevicePaired) {
            ProgressDialogUtils.showProgressDialog(context, LOADING)
            readDeviceModels()
        }
    }

    fun configModelDefinition(context: Context, profileType: ProfileType) {
        CcuLog.i(TAG, "configModelDefinition")
        try {
            this.context = context
            this.profileType = profileType
            loadModel()
            if (isDABExternal() || isVAVExternal()) {
                val configuration: ExternalAhuConfiguration
                if (isDABExternal()) {
                    systemProfile = L.ccu().systemProfile as DabExternalAhu
                    configuration = getConfiguration(DAB_EXTERNAL_AHU_CONTROLLER, profileType)
                } else {
                    systemProfile = L.ccu().systemProfile as VavExternalAhu
                    configuration = getConfiguration(VAV_EXTERNAL_AHU_CONTROLLER, profileType)
                }
                setCurrentConfig(configuration)
            } else {
                // Initial configuration based on the model
                configModel.value.toConfig(profileModelDefinition)
            }
            slaveIdList.value = getSlaveIds(true)
            childSlaveIdList.value = getSlaveIds(false)
            if (!equipModel.value.isDevicePaired) equipModel.value.slaveId.value = 1


            if(configType.value == ConfigType.MODBUS){
                CcuLog.d(TAG_BACNET, "--old config found from modbus--")
                getModbusConfiguration(profileType)
            }else if(configType.value == ConfigType.BACNET){
                CcuLog.d(TAG_BACNET, "--old config found from bacnet--")
                getBacnetConfiguration(profileType, context)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ProgressDialogUtils.hideProgressDialog()
        }
    }

    fun isBacNetEnabled() {
        val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        isBacntEnabled.value = defaultSharedPrefs.getBoolean("isBACnetinitialized", false)
    }

    private fun isBacnetSystemProfileEnabled(): HashMap<Any, Any> {
        return CCUHsApi.getInstance()
            .readEntity("system and equip and bacnet and external")
    }

    private fun isModbusSystemProfileEnabled(): Boolean {
        return CCUHsApi.getInstance()
            .readEntity("system and equip and modbus and not emr and not btu") != null
    }


    private fun isDABExternal() =
        L.ccu().systemProfile.profileType == ProfileType.dabExternalAHUController

    private fun isVAVExternal() =
        L.ccu().systemProfile.profileType == ProfileType.vavExternalAHUController

    private fun getModbusConfiguration(profileType: ProfileType) {
        val modbusEquip =
            CCUHsApi.getInstance().readEntity("system and equip and modbus and not emr and not btu")

        if (PreferenceUtil.getIsNewExternalAhu()) modbusEquip.clear()

        if (modbusEquip != null && modbusEquip.isNotEmpty()) {
            configType.value = ConfigType.MODBUS
            modbusProfile = ModbusProfile()
            val address: Short = modbusEquip["group"].toString().toShort()
            modbusProfile.addMbEquip(address, profileType)
            selectedSlaveId = modbusProfile.slaveId
            val equipmentDevice = buildModbusModel(selectedSlaveId.toInt())
            val model = EquipModel()
            model.equipDevice.value = equipmentDevice
            model.selectAllParameters_Left.value = isAllLeftParamsSelected(equipmentDevice)
            model.selectAllParameters_Right.value = isAllRightParamsSelected(equipmentDevice)
            model.parameters = getParameters(equipmentDevice)
            val subDeviceList = mutableListOf<MutableState<EquipModel>>()
            equipModel.value = model
            equipModel.value.isDevicePaired = true
            equipModel.value.slaveId.value = equipmentDevice.slaveId
            if (equipmentDevice.equips != null && equipmentDevice.equips.isNotEmpty()) {
                equipmentDevice.equips.forEach {
                    val subEquip = EquipModel()
                    subEquip.equipDevice.value = it
                    subEquip.slaveId.value = it.slaveId
                    subEquip.parameters = getParameters(it)
                    subDeviceList.add(mutableStateOf(subEquip))
                }
            }
            if (subDeviceList.isNotEmpty()) equipModel.value.subEquips = subDeviceList
            else equipModel.value.subEquips = mutableListOf()
        }
    }

    private fun getBacnetConfiguration(profileType: ProfileType, context: Context) {
        val bacnetSystemEquip =
            CCUHsApi.getInstance().readEntity("system and equip and bacnet and external")

        if(bacnetSystemEquip.isEmpty()){
            CcuLog.d(TAG_BACNET, "no bacnet external equip present")
        }
        if (PreferenceUtil.getIsNewExternalAhu()) bacnetSystemEquip.clear()
        if(bacnetSystemEquip != null && bacnetSystemEquip.isNotEmpty()){
            configType.value = ConfigType.BACNET
            configModelDefinitionBacnet(context, bacnetSystemEquip)
        }

    }

    private fun loadModel() {
        val def =
            if (profileType == ProfileType.dabExternalAHUController) ModelLoader.getDabExternalAhuModel() else ModelLoader.getVavExternalAhuModel()
        if (def != null) {
            profileModelDefinition = def as SeventyFiveFProfileDirective
        }
    }

    fun getDefaultValByDomain(domainName: String): String {
        return profileModelDefinition.points.find { (it.domainName.contentEquals(domainName)) }?.defaultValue.toString()
    }

    private fun setCurrentConfig(config: ExternalAhuConfiguration) {
        configModel.value.setPointControl = config.setPointControl.enabled
        configModel.value.dualSetPointControl = config.dualSetPointControl.enabled
        configModel.value.fanStaticSetPointControl = config.fanStaticSetPointControl.enabled
        configModel.value.dcvControl = config.dcvControl.enabled
        configModel.value.occupancyMode = config.occupancyMode.enabled
        configModel.value.humidifierControl = config.humidifierControl.enabled
        configModel.value.dehumidifierControl = config.dehumidifierControl.enabled
        configModel.value.heatingMinSp = config.heatingMinSp.currentVal.toString()
        configModel.value.heatingMaxSp = config.heatingMaxSp.currentVal.toString()
        configModel.value.coolingMinSp = config.coolingMinSp.currentVal.toString()
        configModel.value.coolingMaxSp = config.coolingMaxSp.currentVal.toString()
        configModel.value.fanMinSp = config.fanMinSp.currentVal.toString()
        configModel.value.fanMaxSp = config.fanMaxSp.currentVal.toString()
        configModel.value.dcvMin = config.dcvMin.currentVal.toString()
        configModel.value.dcvMax = config.dcvMax.currentVal.toString()
        configModel.value.co2Threshold = config.co2Threshold.currentVal.toString()
        configModel.value.co2Target = config.co2Target.currentVal.toString()
        configModel.value.damperOpeningRate = config.damperOpeningRate.currentVal.toString()
    }

    fun saveConfiguration() {
        CcuLog.d(TAG_BACNET, "saveConfiguration")
        if (checkValidConfiguration()) {
            configModel.value.isStateChanged = false
            hayStack.resetCcuReady()
            ProgressDialogUtils.showProgressDialog(context, "Saving Profile Configuration")
            viewModelScope.launch (highPriorityDispatcher) {
                if (L.ccu().systemProfile != null) {
                    if (profileType != L.ccu().systemProfile.profileType) {
                        CcuLog.d(TAG_BACNET, "saveConfiguration--this is new profile creation --profileType--$profileType <--from system-->${L.ccu().systemProfile.profileType}--system--${L.ccu().systemProfile}")
                        L.ccu().systemProfile!!.deleteSystemEquip()
                        L.ccu().systemProfile.removeSystemEquipBacnet()
                        L.ccu().systemProfile = null
                        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "ExternalAhuViewModel----addEquip----")
                        addEquip()
                        saveExternalEquip()
                    } else {
                        CcuLog.d(TAG_BACNET, "saveConfiguration--this is profile update")
                        updateSystemProfile()
                    }
                } else {
                    CcuLog.d(TAG_BACNET, "saveConfiguration--no system profile found this is the first time ")
                    addEquip()
                    saveExternalEquip()
                }
                L.saveCCUState()
                context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                withContext(Dispatchers.Main) {
                    ProgressDialogUtils.hideProgressDialog()
                }
                //equipModel.value.isDevicePaired = true

                showToast("Configuration saved successfully", context)
                updateOaoPoints();
                hayStack.syncEntityTree()
                hayStack.setCcuReady()
            }
        }
    }

    private fun checkValidConfiguration(): Boolean {
        if (configType.value == ConfigType.MODBUS && (!isValidConfiguration())) return false
        if (configType.value == ConfigType.BACNET && (!isValidConfigurationBacnet())) return false

        return true
    }

    private fun saveExternalEquip() {
        CcuLog.d(TAG_BACNET, "saveExternalEquip >${configType.value}")
        if (configType.value == ConfigType.MODBUS) {
            L.ccu().systemProfile.removeSystemEquipBacnet()
            populateSlaveId()
            CCUHsApi.getInstance().resetCcuReady()
            setUpsModbusProfile(profileType)
            externalBacnetEquipCreated = null
            resetBacnetModelAndConfig()
            equipModel.value.isDevicePaired = true
        }else if(configType.value == ConfigType.BACNET){
            L.ccu().systemProfile.removeSystemEquipModbus()
            //populateBacnetConfigurationObject()
            saveConfigurationBacnet()
            //setUpBacnetProfile()
        }
    }

    private fun setUpsModbusProfile(profileType: ProfileType) {
        equipModel.value.equipDevice.value.slaveId = equipModel.value.slaveId.value
        val parentMap = getModbusEquipMap(equipModel.value.slaveId.value.toShort())

        if (parentMap.isNullOrEmpty()) {
            val subEquipmentDevices = mutableListOf<EquipmentDevice>()
            if (equipModel.value.subEquips.isNotEmpty()) {
                equipModel.value.subEquips.forEach {
                    subEquipmentDevices.add(it.value.equipDevice.value)
                }
            }
            modbusProfile = ModbusProfile()
            modbusProfile.addMbEquip(
                equipModel.value.slaveId.value.toShort(),
                "SYSTEM",
                "SYSTEM",
                equipModel.value.equipDevice.value,
                getParametersList(equipModel.value.equipDevice.value),
                profileType,
                subEquipmentDevices,
                "system",
                equipModel.value.version.value
            )
        } else {
            updateModbusProfile()
        }
        PreferenceUtil.setSelectedProfileWithAhu("modbus")
    }

    private fun isValidConfiguration(): Boolean {
        if (equipModel.value.parameters.isEmpty()) {
            showToast("Please select modbus device", context)
            return false
        }
        if (equipModel.value.isDevicePaired) return true // If it is paired then will not allow the use to to edit slave id

        if (L.isModbusSlaveIdExists(equipModel.value.slaveId.value.toShort())) {
            showToast(
                "Slave Id " + equipModel.value.slaveId.value + " already exists, choose " + "another slave id to proceed",
                context
            )
            return false
        }
        return true
    }

    private fun isValidConfigurationBacnet(): Boolean {
        if (bacnetModel.value.points.isEmpty()) {
            showToast("Please select bacnet model", context)
            return false
        }
        if(!isBacnetConfigDetailsFilled()){
            showToast("Please fill bacnet config details", context)
            return false
        }

        if(!isValidIPv4(destinationIp.value)){
            showToast("Please provide valid ip address", context)
            return false
        }

        if (bacnetModel.value.isDevicePaired) return true
        return true
    }

    private fun isBacnetConfigDetailsFilled() : Boolean{
        if(destinationIp.value.isEmpty()) return false
        if(deviceId.value.isEmpty()) return false
        if(destinationPort.value.isEmpty()) return false

        return true
    }

    private fun populateSlaveId() {
        equipModel.value.equipDevice.value.slaveId = equipModel.value.slaveId.value
        equipModel.value.parameters.forEach {
            it.param.value.isDisplayInUI = it.displayInUi.value
        }
        equipModel.value.subEquips.forEach {
            it.value.equipDevice.value.slaveId = it.value.slaveId.value
            it.value.parameters.forEach { register ->
                register.param.value.isDisplayInUI = register.displayInUi.value
            }
        }
    }

    private fun updateSystemProfile() {
        val profileEquipBuilder = ProfileEquipBuilder(CCUHsApi.getInstance())
        profileEquipBuilder.updateEquipAndPoints(
            configModel.value.getConfiguration(profileType),
            profileModelDefinition,
            CCUHsApi.getInstance().site!!.id,
            CCUHsApi.getInstance().siteName + "-" + profileModelDefinition.name,
            isReconfiguration = true
        )
        saveExternalEquip()
    }

    private fun addEquip() {
        systemProfile = if (profileType == ProfileType.dabExternalAHUController) DabExternalAhu()
        else VavExternalAhu()
        addSystemEquip(
            configModel.value.getConfiguration(profileType),
            profileModelDefinition,
            systemProfile as SystemProfile
        )
        L.ccu().systemProfile = systemProfile
        systemProfile?.setOutsideTempCoolingLockoutEnabled(hayStack, L.ccu().oaoProfile != null)
        DesiredTempDisplayMode.setSystemModeForDab(CCUHsApi.getInstance())
    }


    fun itemsFromMinMax(min: Double, max: Double, increment: Double): List<String> {
        require(min <= max) { "Minimum value must be less than or equal to the maximum value" }
        require(increment > 0.0) { "Increment value must be greater than zero" }
        val decimalFormat = DecimalFormat("#." + "#".repeat(2))
        val result = mutableListOf<String>()
        var current = min

        while (decimalFormat.format(current).toString().toDouble() <= max) {
            result.add(decimalFormat.format(current).toString())
            current + current + increment
            current += increment
        }
        return result
    }

    private fun getModelIdByName(name: String) = deviceModelList.find { it.name == name }!!.id
    private fun getVersionByID(id: String) = deviceModelList.find { it.id == id }!!.version


    fun onSelectAllLeft(isSelected: Boolean) {
        if (equipModel.value.parameters.isNotEmpty()) {
            equipModel.value.parameters.forEachIndexed { index, it ->
                if (index % 2 == 0) {
                    it.displayInUi.value = isSelected
                }
            }
        }
    }

    fun onSelectAllRight(isSelected: Boolean) {
        if (equipModel.value.parameters.isNotEmpty()) {
            equipModel.value.parameters.forEachIndexed { index, it ->
                if (index % 2 != 0) {
                    it.displayInUi.value = isSelected
                }
            }
        }
    }

    fun onSelectAllRightSubEquip(isSelected: Boolean, subEquip: MutableState<EquipModel>) {
        if (subEquip.value.parameters.isNotEmpty()) {
            subEquip.value.parameters.forEachIndexed { index, it ->
                if (index % 2 != 0) {
                    it.displayInUi.value = isSelected
                }
            }
        }
    }

    fun onSelectAllLeftSubEquip(isSelected: Boolean, subEquip: MutableState<EquipModel>) {
        if (subEquip.value.parameters.isNotEmpty()) {
            subEquip.value.parameters.forEachIndexed { index, it ->
                if (index % 2 == 0) {
                    it.displayInUi.value = isSelected
                }
            }
        }
    }
    fun updateSelectAllBoth() {
        var isAllSelected1 = true
        var isAllSelected2 = true
        if (equipModel.value.parameters.isNotEmpty()) {
            equipModel.value.parameters.forEachIndexed { index, it ->
                if ((index % 2 == 0) && (!it.displayInUi.value)) isAllSelected1 = false
                if ((index % 2 != 0) && (!it.displayInUi.value)) isAllSelected2 = false
            }
        } else {
            isAllSelected1 = false
            isAllSelected2 = false
        }
        equipModel.value.selectAllParameters_Left.value = isAllSelected1
        equipModel.value.selectAllParameters_Right.value = isAllSelected2
    }

    fun updateSelectAllSubEquipLeft(indexValue: Int) {
        var isAllSelected = true
        val data = equipModel.value.subEquips[indexValue]
        if (equipModel.value.subEquips.isNotEmpty()) {
            data.value.parameters.forEachIndexed { index, it ->
                if ((index % 2 == 0) && (!it.displayInUi.value)) isAllSelected = false
            }
        } else isAllSelected = false
        if (equipModel.value.selectAllParameters_Left_subEquip.size <= indexValue) {
            equipModel.value.selectAllParameters_Left_subEquip.add(
                indexValue, RegisterItemForSubEquip()
            )
        }
        equipModel.value.selectAllParameters_Left_subEquip[indexValue].displayInUi.value =
            isAllSelected
    }

    fun updateSelectAllSubEquipRight(indexValue: Int) {
        var isAllSelected = true
        val data = equipModel.value.subEquips[indexValue]
        if (equipModel.value.subEquips.isNotEmpty()) {
            data.value.parameters.forEachIndexed { index, it ->
                if ((index % 2 != 0) && (!it.displayInUi.value)) isAllSelected = false
            }
        } else isAllSelected = false
        if (equipModel.value.selectAllParameters_Right_subEquip.size <= indexValue) {
            equipModel.value.selectAllParameters_Right_subEquip.add(
                indexValue, RegisterItemForSubEquip()
            )
        }
        equipModel.value.selectAllParameters_Right_subEquip[indexValue].displayInUi.value =
            isAllSelected
    }
    enum class ConfigType {
        BACNET, MODBUS
    }

    fun fetchModelDetails(selectedDevice: String) {
        val modelId = getModelIdByName(selectedDevice)
        val version = getVersionByID(modelId)
        domainService.readModelById(modelId, version, object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                if (!response.isNullOrEmpty()) {
                    try {
                        val equipmentDevice = parseModbusDataFromString(response)
                        if (equipmentDevice != null) {
                            val model = EquipModel()
                            model.slaveId.value = 1
                            model.jsonContent = response
                            model.equipDevice.value = equipmentDevice
                            model.parameters = getParameters(equipmentDevice)
                            model.version.value = getVersionByID(modelId)
                            val subDeviceList = mutableListOf<MutableState<EquipModel>>()
                            equipModel.value = model
                            if (equipmentDevice.equips != null && equipmentDevice.equips.isNotEmpty()) {
                                equipmentDevice.equips.forEach {
                                    val subEquip = EquipModel()
                                    subEquip.equipDevice.value = it
                                    subEquip.parameters = getParameters(it)
                                    subDeviceList.add(mutableStateOf(subEquip))
                                }
                            }
                            if (subDeviceList.isNotEmpty()) {
                                equipModel.value.subEquips = subDeviceList
                            } else {
                                equipModel.value.subEquips = mutableListOf()
                            }
                        } else {
                            showErrorDialog(context, NO_MODEL_DATA_FOUND)
                        }
                    } catch (e: JsonParseException) {
                        showErrorDialog(context, NO_INTERNET)
                    }
                } else {
                    showErrorDialog(context, NO_INTERNET)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    private fun readDeviceModels() {
        domainService.readModbusModelsList("ahu", object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                try {
                    if (!response.isNullOrEmpty()) {
                        val itemList = mutableListOf<String>()
                        deviceModelList = getModelListFromJson(response)
                        deviceModelList.forEach {
                            itemList.add(it.name)
                        }
                        deviceList.value = itemList
                    } else {
                        showErrorDialog(context, MODBUS_DEVICE_LIST_NOT_FOUND)
                    }
                } catch (e: Exception) {
                    showErrorDialog(context, NO_INTERNET)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    private fun getModbusEquipMap(slaveId: Short): HashMap<Any, Any>? {
        return CCUHsApi.getInstance()
            .readEntity("equip and modbus and not equipRef and group == \"$slaveId\"")
    }

    private fun getBacnetEquipMap(): HashMap<Any, Any>? {
        val checkBacnetEquip =  CCUHsApi.getInstance()
            .readEntity("equip and bacnet and system and external")
        CcuLog.d(TAG_BACNET, "--checkBacnetEquip--$checkBacnetEquip")
        return CCUHsApi.getInstance()
            .readEntity("equip and bacnet and system and external")
    }

    private fun updateModbusProfile() {
        modbusProfile.updateModbusEquip(
            equipModel.value.equipDevice.value.deviceEquipRef,
            equipModel.value.equipDevice.value.slaveId.toShort(),
            getParametersList(equipModel.value)
        )

        equipModel.value.subEquips.forEach {
            modbusProfile.updateModbusEquip(
                it.value.equipDevice.value.deviceEquipRef,
                it.value.equipDevice.value.slaveId.toShort(),
                getParametersList(it.value)
            )
        }
        L.ccu().zoneProfiles.add(modbusProfile)
        L.saveCCUState()
    }

    fun hasUnsavedChanges(): Boolean {
        try {
            val configuration: ExternalAhuConfiguration
            if (isDABExternal()) {
                systemProfile = L.ccu().systemProfile as DabExternalAhu
                configuration = getConfiguration(DAB_EXTERNAL_AHU_CONTROLLER, profileType)
            } else {
                systemProfile = L.ccu().systemProfile as VavExternalAhu
                configuration = getConfiguration(VAV_EXTERNAL_AHU_CONTROLLER, profileType)
            }
            return verifyChanges(configuration, configModel.value.getConfiguration(profileType))
        } catch (e: Exception) {
            return false
        }
    }

    private fun verifyChanges(
        initialConfig: ExternalAhuConfiguration,
        config: ExternalAhuConfiguration
    ): Boolean {
        return initialConfig.setPointControl.enabled != config.setPointControl.enabled ||
                initialConfig.dualSetPointControl.enabled != config.dualSetPointControl.enabled ||
                initialConfig.fanStaticSetPointControl.enabled != config.fanStaticSetPointControl.enabled ||
                initialConfig.dcvControl.enabled != config.dcvControl.enabled ||
                initialConfig.occupancyMode.enabled != config.occupancyMode.enabled ||
                initialConfig.humidifierControl.enabled != config.humidifierControl.enabled ||
                initialConfig.dehumidifierControl.enabled != config.dehumidifierControl.enabled ||
                initialConfig.heatingMinSp.currentVal != config.heatingMinSp.currentVal ||
                initialConfig.heatingMaxSp.currentVal != config.heatingMaxSp.currentVal ||
                initialConfig.coolingMinSp.currentVal != config.coolingMinSp.currentVal ||
                initialConfig.coolingMaxSp.currentVal != config.coolingMaxSp.currentVal ||
                initialConfig.fanMinSp.currentVal != config.fanMinSp.currentVal ||
                initialConfig.fanMaxSp.currentVal != config.fanMaxSp.currentVal ||
                initialConfig.dcvMin.currentVal != config.dcvMin.currentVal ||
                initialConfig.dcvMax.currentVal != config.dcvMax.currentVal ||
                initialConfig.co2Threshold.currentVal != config.co2Threshold.currentVal ||
                initialConfig.co2Target.currentVal != config.co2Target.currentVal ||
                initialConfig.damperOpeningRate.currentVal != config.damperOpeningRate.currentVal
    }

    val onBacnetDeviceSelect = object : OnItemSelectBacnetDevice {
        override fun onItemSelected(index: Int, item: BacnetDevice) {
            CcuLog.d(TAG, "onItemSelected-->$item")
            destinationIp.value = item.deviceIp
            deviceId.value = item.deviceId
            destinationPort.value = item.devicePort
            destinationMacAddress.value = item.deviceMacAddress?.let { macAddressToByteArray(it) } ?: ""
            dnet.value = item.deviceNetwork
        }
    }

    private fun macAddressToByteArray(mac: String): String {
        var byteArray = mac.split(":")
            .map { it.toInt(16).toByte() }
            .toByteArray()
        return byteArray.joinToString(".") { (it.toInt() and 0xFF).toString() }

    }

    fun configModelDefinitionBacnet(
        context: Context,
        bacnetSystemEquip: java.util.HashMap<Any, Any>
    ) {
        CcuLog.d(TAG_BACNET, "----configModelDefinitionBacnet---")
        this.context = context

        if (bacnetSystemEquip != null) {

            bacnetProfile = BacnetProfile()
            if (bacnetProfile != null) {

                //selectedSlaveIdBacnet = bacnetProfile.slaveId
                CcuLog.d(TAG_BACNET, "-------load model from bacnet equip id==>${bacnetSystemEquip["id"].toString()}")
                val equipmentDevice = buildBacnetModelSystem(bacnetSystemEquip)
                val version  = bacnetSystemEquip["version"].toString()
                CcuLog.d(TAG_BACNET, "configModelDefinitionBacnet----loaded all points-->${equipmentDevice.size} --->version<-->$version")
                if(equipmentDevice.isNotEmpty()){
                    CcuLog.d(TAG_BACNET, "configModelDefinitionBacnet----loaded all points-->${equipmentDevice[0].points.size}")
                }
                //val equipmentDevice = buildBacnetModel(bacnetSystemEquip["id"].toString())

                CcuLog.d(TAG_BACNET, "------2-------")
                val model = BacnetModel()
                model.equipDevice.value = equipmentDevice[0]
                CcuLog.d(TAG_BACNET, "------3-------")
                model.selectAllParameters.value = isAllParamsSelectedBacNet(equipmentDevice[0])
                CcuLog.d(TAG_BACNET, "------4-------")
                model.points = getBacnetPoints(equipmentDevice[0].points.toImmutableList()) //equipmentDevice[0].points

                CcuLog.d(TAG_BACNET, "bacnet points to reconfigure==>${model.points.size}")

                for(point in model.points){
                    if(!point.displayInUi.value){
                        displayInUi.value = false
                    }
                }

                CcuLog.d(TAG_BACNET, "update UI as bacnetModel got the value")
                model.version.value = version
                bacnetModel.value = model
                bacnetModel.value.isDevicePaired = true

                model.equipDevice.value.bacnetConfig?.let { config ->
                    config.split(",").forEach {
                        val configParam = it.split(":")
                        when(configParam[0]){
                            "deviceId" -> deviceId.value = configParam[1]
                            "destinationIp" -> destinationIp.value = configParam[1]
                            "destinationPort" -> destinationPort.value = configParam[1]
                            "macAddress" -> destinationMacAddress.value = configParam[1]
                            "deviceNetwork" -> dnet.value = configParam[1]
                        }
                    }
                }
                CcuLog.d(TAG, "--> deviceId ${deviceId.value} destinationIp ${destinationIp.value} destinationPort ${destinationPort.value} macAddress ${destinationMacAddress.value} dnet ${dnet.value}")
                model.equipDevice.value.modelConfig?.let { config ->
                    config.split(",").forEach {
                        val configParam = it.split(":")
                        when (configParam[0]) {
                            "modelName" -> modelName.value = configParam[1]
                        }
                    }
                }
            }
            isCopiedConfigurationAvailable()
            isUpdateMode.value = true
        } else {
            isUpdateMode.value = false
            ProgressDialogUtils.showProgressDialog(context, "Loading Bacnet Models")
            if(!::deviceModelList.isInitialized || deviceModelList.isEmpty()){
                readDeviceModels()
            }else{
                ProgressDialogUtils.hideProgressDialog()
            }
        }
    }

    fun applyCopiedConfiguration() {
        val copiedModbusConfiguration = CopyConfiguration.getCopiedBacnetConfiguration()

        if(bacnetModel.value.isDevicePaired){
            bacnetModel.value.points.forEach { bacNetPoint ->
                copiedModbusConfiguration.value.points.forEach {
                    if (bacNetPoint.disName.equals(it.disName,true)) {
                        bacNetPoint.displayInUi.value = it.displayInUi.value
                        bacNetPoint.protocolData?.bacnet?.displayInUIDefault = it.displayInUi.value
                    }
                }
            }
        } else {
            bacnetModel.value.points.forEach { bacNetPoint ->
                copiedModbusConfiguration.value.points.forEach {
                    if (bacNetPoint.domainName.equals(it.disName.replace(" ", "").toLowerCase(), true) || bacNetPoint.name.equals(it.disName, true)) {
                        bacNetPoint.displayInUi.value = it.displayInUi.value
                        bacNetPoint.protocolData?.bacnet?.displayInUIDefault = it.displayInUi.value
                    }
                }
            }
        }
        disablePasteConfiguration()
        formattedToastMessage(context.getString(R.string.Toast_Success_Message_paste_Configuration), context)
    }

    fun disablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = false
        }
    }

    private fun enablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = true
        }
    }

    fun fetchData(){

        isDeviceIdValid.value = !isDeviceIdAlreadyInUse(deviceId.value)

        isDestinationIpValid.value = isValidIpAddress(destinationIp.value)
        CcuLog.d(TAG_BACNET, "--------------fetchData--isDestinationIpInvalidValid-----$isDestinationIpValid--------")
        if(isDestinationIpValid.value.not()){
            return
        }

        deviceValue.value = false
        bacnetPropertiesFetched.value = false
        CcuLog.d(TAG_BACNET, "--------------fetchData--isDestinationIpInvalidValid-----$deviceIp--------")
        service = ServiceManager.makeCcuService(ipAddress = deviceIp)
        val destination =
            DestinationMultiRead(destinationIp.value, destinationPort.value, deviceId.value, dnet.value, destinationMacAddress.value)
        val readAccessSpecification = mutableListOf<ReadRequestMultiple>()

        bacnetModel.value.points.forEach {
            val objectId = it.protocolData?.bacnet?.objectId
            val objectType = it.protocolData?.bacnet?.objectType

            val objectIdentifier = getDetailsFromObjectLayout(objectType!!, objectId.toString())
            val propertyReference = getDetailsOfProperties(it.bacnetProperties, it)
            readAccessSpecification.add(
                ReadRequestMultiple(
                    objectIdentifier,
                    propertyReference
                )
            )
        }

        val rpmRequest = RpmRequest(readAccessSpecification)
        ProgressDialogUtils.showProgressDialog(context, "Fetching data from device")
        sendRequestMultipleRead(BacnetReadRequestMultiple(destination, rpmRequest))
    }
    fun saveConfigurationBacnet() {
        CcuLog.d(TAG_BACNET, "saveConfigurationBacnet")
        viewModelScope.launch {
            val parentMap : HashMap<Any, Any>?
            CcuLog.d(TAG_BACNET, "saveConfigurationBacnet check equip-->$externalBacnetEquipCreated")
            if(externalBacnetEquipCreated != null){
                parentMap = externalBacnetEquipCreated
            }else{
                parentMap = getBacnetEquipMap()
            }

            if(parentMap != null){
                if(parentMap.size == 0){
                    CcuLog.d(TAG_BACNET, "bacnet system for ahu not exists creating new one")
                    saveBacnetProfile()
                }else{
                    CcuLog.d(TAG_BACNET, "bacnet system for ahu already exists ---1--go for update")
                    updateBacnetProfile(parentMap)
                }

            }else{
                saveBacnetProfile()
                CcuLog.d(TAG_BACNET, "bacnet system for ahu not exists creating new one -- 2--")
            }

        }
    }

    private suspend fun saveBacnetProfile() {
        ProgressDialogUtils.showProgressDialog(context, SAVING_BACNET)
        try {
            CcuLog.d(TAG_BACNET, "saveConfigurationBacnet start")
            updateOriginalModel()
            CCUHsApi.getInstance().resetCcuReady()
            setUpBacnetProfile()
            L.saveCCUState()
            CCUHsApi.getInstance().setCcuReady()
            CcuLog.d(TAG_BACNET, "saveConfigurationBacnet end")
            PreferenceUtil.setSelectedProfileWithAhu("bacnet")
            val equip = isBacnetSystemProfileEnabled()
            CcuLog.d(TAG_BACNET, "saveConfigurationBacnet end ${equip.size}")
            externalBacnetEquipCreated = equip
            CcuLog.d(TAG_BACNET, "saveConfigurationBacnet end equip id $externalBacnetEquipCreated")

            if (externalBacnetEquipCreated != null) {
                CcuLog.d(TAG_BACNET, "----configModelDefinitionBacnet after save--2---")
                configModelDefinitionBacnet(context, java.util.HashMap(externalBacnetEquipCreated))
            }
            context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
            showToast(SAVED, context)
            bacnetModel.value.isDevicePaired = true
            _isDialogOpen.value = false
            Globals.getInstance().checkBacnetSystemProfileStatus()
        } catch (e: Exception) {
            // Handle exceptions if needed
            ProgressDialogUtils.hideProgressDialog()
            showToast("Error saving configuration: ${e.message}", context)
        }
    }

    fun updateSelectAll(b: Boolean, item: BacnetPointState) {
        bacnetModel.value.equipDevice.value.points.forEach { bacnetPoint ->
            if(bacnetPoint.id == item.id){
                CcuLog.d(TAG, "updateSelectAll--${bacnetPoint.id}--${item.id} value== $b")
                bacnetPoint.protocolData?.bacnet?.displayInUIDefault  = b
            }
        }
        CcuLog.d(TAG_BACNET, "--##updateSelectAll finished check status now--");
        updateDisplayInUi(b)
    }

    fun searchDevices() {
        getServerIp()
        CcuLog.d(TAG_BACNET, "searchDevices---ip of device is --$deviceIp");
        isConnectedDevicesSearchFinished.value = false
        fetchConnectedDeviceGlobally()
    }

    fun updatePropertyStatus(
        i: Int,
        pointId: String,
        bacnetProperty: MutableState<BacnetProperty>,
        objectId: Int?
    ) {
        val updatedPoints = bacnetModel.value.points.map { point ->
            val keyFromPoint = "${point.id}-${point.protocolData?.bacnet?.objectId}"
            val keyFromUpdate = "${pointId}-${objectId}"
            if (keyFromPoint == keyFromUpdate) {
                //if (point.id == pointId) {
                val updatedProperties = point.bacnetProperties.map { bacnetProp ->
                    if (bacnetProp.id == bacnetProperty.value.id) {
                        bacnetProp.copy(selectedValue = i)
                    } else {
                        bacnetProp
                    }
                }
                point.copy(bacnetProperties = updatedProperties.toMutableList())
            } else {
                point
            }
        }
        bacnetModel.value.points = updatedPoints.toMutableList()
        CcuLog.d(TAG, "Updated point is $pointId and property is ${bacnetProperty.value.id} value is $i")
    }

    fun updateDisplayInUiModules(b: Boolean) {
        for (point in bacnetModel.value.points) {
            point.displayInUi.value = b
        }
        bacnetModel.value.equipDevice.value.points.forEach { bacnetPoint ->
            bacnetPoint.protocolData?.bacnet?.displayInUIDefault = b
        }
    }

    fun updateDeviceValueInUiModules(b: Boolean) {
        isDeviceValueSelected.value = b
        val testBacnetModel = bacnetModel.value
        for (point in testBacnetModel.points) {
            point.bacnetProperties?.forEach { bacnetProperty ->
                if (b) {
                    bacnetProperty.selectedValue = 1
                } else {
                    bacnetProperty.selectedValue = 0
                }
            }
        }
        bacnetModel.value = testBacnetModel
        isStateChanged.value = !isStateChanged.value

        for (point in bacnetModel.value.equipDevice.value.points) {
            point.bacnetProperties.forEach() { bacnetProperty ->
                if (b) {
                    bacnetProperty.selectedValue = 1
                } else {
                    bacnetProperty.selectedValue = 0
                }
            }
        }
    }

    private fun getServerIp() : String{
        var serverIpAddress = ""
        var bacnetServerConfig = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
        if (bacnetServerConfig != null)
        {
            try {
                val config = JSONObject(bacnetServerConfig)
                val networkObject = config.getJSONObject("network")
                serverIpAddress = networkObject.getString(BacnetConfigConstants.IP_ADDRESS)
                devicePort = networkObject.getInt(BacnetConfigConstants.PORT).toString()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        deviceIp = serverIpAddress
        return serverIpAddress
    }

    private fun fetchConnectedDeviceGlobally() {
        service = ServiceManager.makeCcuService(ipAddress = deviceIp)
        CcuLog.d(TAG, "--fetchConnectedDevice for ip --> ${deviceIp} --- service--${service.toString()}---devicePort-->$devicePort")
        try {
            val broadCastValue = "global"

            val bacnetWhoIsRequest = BacnetWhoIsRequest(
                WhoIsRequest(
                    "",
                    ""
                ),
                BroadCast(broadCastValue),
                devicePort,
                deviceIp
            )
            val request = Gson().toJson(
                bacnetWhoIsRequest
            )
            CcuLog.d(TAG, "this is the broadcast request-->$request")
            sendRequest(bacnetWhoIsRequest)
        }catch (e : NumberFormatException){
            CcuLog.d(TAG, "please provide valid input - ${e.message}")
        }
    }

    fun isCopiedConfigurationAvailable() {
        if(CopyConfiguration.getSelectedBacNetModel() != null && moduleLevel!="system" && (bacnetModel.value.isDevicePaired == false || bacnetModel.value.isDevicePaired == true) && modelName.value.equals(
                CopyConfiguration.getSelectedBacNetModel(),true)){
            enablePasteConfiguration()
        }
        else{
            disablePasteConfiguration()
        }
    }

    private fun updateOriginalModel() {
        val mapOfModifiedPoints = mutableMapOf<String, BacnetPointState>()
        val testBacnetModel = bacnetModel.value

        for (point in testBacnetModel.points) {
            val key = "${point.id}-${point.protocolData?.bacnet?.objectId}"
            //mapOfModifiedPoints[point.id] = point
            mapOfModifiedPoints[key] = point
        }

        testBacnetModel.equipDevice.value.points.forEach { point ->
            val key = "${point.id}-${point.protocolData?.bacnet?.objectId}"
            //val modifiedPoint = mapOfModifiedPoints[point.id]
            val modifiedPoint = mapOfModifiedPoints[key]
            if (modifiedPoint != null) {
                point.defaultWriteLevel = modifiedPoint.defaultWriteLevel
                point.bacnetProperties = modifiedPoint.bacnetProperties.toMutableList()
            }
        }
    }

    private fun isDeviceIdAlreadyInUse(deviceId: String): Boolean {
        return isEquipExists(deviceId)
    }

    private fun isValidIpAddress(ip: String): Boolean {
        val octets = ip.split(".")
        if (octets.size != 4) {
            return false
        }
        for (octet in octets) {
            val number = octet.toIntOrNull()
            if (number == null || number < 0 || number > 255) {
                return false
            }
        }
        return true

    }

    private fun isEquipExists(equipId: String): Boolean {
        CcuLog.d(TAG, "isEquipExists --> $equipId")
        val equip: HashMap<*, *> = CCUHsApi.getInstance().read("equip and bacnet and group == \"$equipId\"")
        return equip.isNotEmpty()
    }

    private fun getDetailsFromObjectLayout(objectType : String, objectId: String): ObjectIdentifierBacNet {
        return ObjectIdentifierBacNet(
            BacNetConstants.ObjectType.valueOf("OBJECT_$objectType").value,
            objectId
        )
    }

    private fun getDetailsOfProperties(
        listOfProperties: MutableList<BacnetProperty>?,
        bacnetPointState: BacnetPointState
    ): MutableList<PropertyReference> {
        val list = mutableListOf<PropertyReference>()
        listOfProperties?.forEach {
            list.add(
                PropertyReference(
                    it.id,
                    -1
                )
            )
        }
        if(bacnetPointState.equipTagNames.contains("writable")){
            for (i in 0..15) {
                list.add(
                    PropertyReference(
                        87,
                        i
                    )
                )
            }
        }
        return list
    }

    private fun setUpBacnetProfile() {
        CcuLog.d(TAG_BACNET, "--setUpBacnetProfile node address ${deviceId.value}")
        //val parentMap = getBacnetEquipMap(deviceId.value)
        //if (parentMap.isNullOrEmpty()) {

            bacnetProfile = BacnetProfile()
        CcuLog.d(TAG_BACNET, "--setUpBacnetProfile -----1-----")
            val configParam = "deviceId:${deviceId.value},destinationIp:${destinationIp.value},destinationPort:${destinationPort.value},macAddress:${destinationMacAddress.value},deviceNetwork:${dnet.value}"

        CcuLog.d(TAG_BACNET, "--setUpBacnetProfile -----2-----")
        val modelConfig = "modelName:${modelName.value},modelId:${getBacnetModelIdByName(modelName.value)}"
        CcuLog.d(TAG_BACNET, "------addBacAppEquip-----")
        bacnetProfile.addBacAppEquip(configParam, modelConfig, deviceId.value, deviceId.value, "SYSTEM", "SYSTEM",
                bacnetModel.value.equipDevice.value,
                profileType,"system",bacnetModel.value.version.value, true)

            //L.ccu().zoneProfiles.add(bacnetProfile)
            //L.saveCCUState()
            CcuLog.d(TAG_BACNET, "setUpBacnetProfile completed")
        //} else {
            //updateBacnetProfile()
        //}
    }

    private fun sendRequestMultipleRead(rpmRequest: BacnetReadRequestMultiple) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.multiread(rpmRequest)
                val resp = BaseResponse(response)
                ProgressDialogUtils.hideProgressDialog()
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG_BACNET, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            updateUi(readResponse)
                        }
                    } else {
                        CcuLog.d(TAG_BACNET, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG_BACNET, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.d(TAG_BACNET, "--SocketTimeoutException--${e.message}")
                showToastMessage("SocketTimeoutException {e.message}")
            } catch (e: ConnectException) {
                CcuLog.d(TAG_BACNET, "--ConnectException--${e.message}")
                showToastMessage("ConnectException ${e.message}")
            } catch (e: java.lang.Exception) {
                CcuLog.d(TAG_BACNET, "--connection time out--${e.message}")
                showToastMessage("Connection time out ${e.message}")
            }
            ProgressDialogUtils.hideProgressDialog()
        }
    }

    private fun sendRequest(bacnetWhoIsRequest: BacnetWhoIsRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.whois(bacnetWhoIsRequest)
                val resp = BaseResponse(response)
                if (response.isSuccessful) {
                    val result = resp.data
                    if (result != null) {
                        val readResponse = result.body()
                        CcuLog.d(TAG_BACNET, "received response->${readResponse}")
                        CoroutineScope(Dispatchers.Main).launch {
                            if (readResponse != null && !readResponse.whoIsResponseList.isNullOrEmpty()) {
                                populateBacnetDevices(readResponse.whoIsResponseList)
                                isConnectedDevicesSearchFinished.value = true
                            }else{
                                CcuLog.d(TAG_BACNET, "no devices found")
                                showToastMessage("No devices found check configuration and initialize bacnet again")
                            }
                        }
                    } else {
                        CcuLog.d(TAG_BACNET, "--null response--")
                    }
                } else {
                    CcuLog.d(TAG_BACNET, "--error--${resp.error}")
                }
            } catch (e: SocketTimeoutException) {
                CcuLog.d(TAG_BACNET, "--SocketTimeoutException--${e.message}")
                showToastMessage("SocketTimeoutException ${e.message}")
            } catch (e: ConnectException) {
                CcuLog.d(TAG_BACNET, "--ConnectException--${e.message}")
                showToastMessage("ConnectException ${e.message}")
            } catch (e: java.lang.Exception) {
                showToastMessage("connection time out")
                CcuLog.d(TAG_BACNET, "--connection time out--${e.message}")
            }
            isConnectedDevicesSearchFinished.value = true
            ProgressDialogUtils.hideProgressDialog()
        }
    }

    private fun getBacnetEquipMap(slaveId: String): HashMap<Any, Any>? {
        return CCUHsApi.getInstance()
            .readEntity("equip and bacnet and not equipRef and group == \"$slaveId\"")
    }

    private fun updateBacnetProfile(parentMap: HashMap<Any, Any>) {
        bacnetProfile = BacnetProfile()
        bacnetProfile.addBacAppEquip(parentMap["group"].toString().toLong(), ProfileType.BACNET_DEFAULT)
        //val bacnetModeResponse = buildBacnetModelSystem(parentMap)

//        val model = BacnetModel()
//        model.jsonContent = ""
//        model.equipDevice.value = bacnetModeResponse[0]
//        model.version.value = "1.0"
//        model.points = getBacnetPoints(bacnetModeResponse[0].points)
//        bacnetModel.value = model


        CcuLog.d(TAG_BACNET, "--updateBacnetProfile--${parentMap}<--points size-->${bacnetModel.value.equipDevice.value.points.size}<---->${parentMap["id"]}")
        bacnetProfile.updateBacnetEquip(parentMap["id"].toString(), bacnetModel.value.equipDevice.value.points)
        //L.ccu().zoneProfiles.add(modbusProfile)
        L.saveCCUState()
        PreferenceUtil.setSelectedProfileWithAhu("bacnet")
        bacnetModel.value.isDevicePaired = true
    }

    private fun updateUi(readResponse: MultiReadResponse?) {
        if (readResponse != null) {
            if (readResponse.error != null) {
                val errorCode = BacNetConstants.BacnetErrorCodes.from(readResponse.error!!.errorCode.toInt())
                val errorClass = BacNetConstants.BacnetErrorClasses.from(readResponse.error!!.errorClass.toInt())
                showToastMessage("error code->${errorCode}--error class->${errorClass}")
            } else if(readResponse.errorAbort != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAbortErrors.from(
                    readResponse.errorAbort!!.abortReason.toInt())}")
            }else if(readResponse.errorBacApp != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetAppErrors.from(readResponse.errorBacApp!!.abortReason.toInt())}")
            }else if(readResponse.errorReject != null){
                showToastMessage("abort reason->${BacNetConstants.BacnetRejectErrors.from(
                    readResponse.errorReject!!.abortReason.toInt())}")
            }else if(readResponse.errorASide != null){
                showToastMessage("abort reason->${readResponse.errorASide!!.abortReason}")
            }else {
                if(readResponse.rpResponse != null){
                    for (item in readResponse.rpResponse.listOfItems) {
                        item.results.forEach() {
                            if(it.propertyIdentifier == "87"){
                                if(it.propertyValue != null && it.propertyValue.type == "34"){
                                    val key = "${item.objectIdentifier.objectInstance}-${it.propertyIdentifier}"
                                    bacNetItemsMap.putIfAbsent(key, item)
                                    //bacNetItemsMap[key] = item
                                }
                            }else{
                                val key = "${item.objectIdentifier.objectInstance}-${it.propertyIdentifier}"
                                bacNetItemsMap[key] = item
                            }
                        }
                    }

                    CcuLog.d(TAG,"result map size-->${bacNetItemsMap.size}")
                    updateBacnetProperties()
                    bacnetPropertiesFetched.value = true
                }else{
                    showToastMessage("BAC app failed try again")
                    CcuLog.d(TAG,"BAC app failed check logs--")
                    bacnetRequestFailed.value = true
                }
            }
        }
    }

    private fun populateBacnetDevices(whoIsResponseList: MutableList<WhoIsResponseItem>) {
        val list = mutableListOf<BacnetDevice>()
        whoIsResponseList.forEach { item ->
            list.add(
                BacnetDevice(
                    item.deviceIdentifier,
                    item.ipAddress,
                    item.networkNumber,
                    item.vendorIdentifier,
                    item.portNumber,
                    item.macAddress
                )
            )
        }
        connectedDevices = mutableStateOf(emptyList())
        connectedDevices.value = list
    }

    private fun showToastMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            isErrorMsg.value = true
            errorMsg = message
        }
    }

    private fun updateBacnetProperties(){
        bacnetModel.value.points.forEach { point ->
            var isChangeInProperty = false
            point.displayInEditor.value = false
            val writableLevelKey = "${point.protocolData?.bacnet?.objectId}-87"
            if(bacNetItemsMap[writableLevelKey] != null){
                val results = bacNetItemsMap[writableLevelKey]?.results
                if (results != null) {
                    for (item in results) {
                        if(item.propertyValue.type == "34"){
                            if(item.propertyArrayIndex != null){
                                point.defaultWriteLevel = item.propertyArrayIndex!!
                                CcuLog.d(TAG,"default write level is -->${item.propertyArrayIndex}")
                                break
                            }
                        }
                    }
                }
            }
            point.bacnetProperties?.forEach { bacnetProperty ->
                val key = "${point.protocolData?.bacnet?.objectId}-${bacnetProperty.id}"
                bacnetProperty.fetchedValue =
                    bacNetItemsMap[key]?.results?.get(0)?.propertyValue?.value ?: "NA"

                if(bacnetProperty.defaultValue != bacnetProperty.fetchedValue && !isChangeInProperty){
                    point.displayInEditor.value = true
                    isChangeInProperty = true
                }
            }
        }
    }

    fun configBacnetDetails() {
        CcuLog.i(TAG, "configBacnetDetails")
        if (!bacnetModel.value.isDevicePaired) {
            ProgressDialogUtils.showProgressDialog(context, LOADING_BACNET_MODELS)
            readBacnetDeviceModels()
        }
    }

    private fun readBacnetDeviceModels() {
        domainService.readBacNetModelsList("BACNET", "ahu", object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                try {
                    if (!response.isNullOrEmpty()) {
                        val itemList = mutableListOf<String>()
                        bacnetDeviceModelList = getModelListFromJson(response)
                        bacnetDeviceModelList.forEach {
                            itemList.add(it.name)
                        }
                        bacnetDeviceList.value = itemList
                    } else {
                        showErrorDialog(context, BACNET_DEVICE_LIST_NOT_FOUND)
                    }
                } catch (e: Exception) {
                    showErrorDialog(context, NO_INTERNET)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    private fun getBacnetModelIdByName(name: String) = bacnetDeviceModelList.find { it.name == name }!!.id
    private fun getBacnetModelVersionByID(id: String) = bacnetDeviceModelList.find { it.id == id }!!.version

    fun fetchBacnetModelDetails(selectedDevice: String) {
        CcuLog.i(TAG_BACNET, "bacnet fetchBacnetModelDetails -->$selectedDevice")
        val modelId = getBacnetModelIdByName(selectedDevice)
        val version = getBacnetModelVersionByID(modelId)
        domainService.readBacNetModelById(context, modelId, version, object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                if (!response.isNullOrEmpty()) {
                    try {
                        val equipmentDevice = Gson().fromJson(response, BacnetModelDetailResponse::class.java)
                        if (equipmentDevice != null) {
                            Log.d(TAG, "received data-->$equipmentDevice updating model")
                            val model = BacnetModel()
                            model.jsonContent = response
                            model.equipDevice.value = equipmentDevice
                            model.version.value = version
                            model.points = getBacnetPoints(equipmentDevice.points)
                            bacnetModel.value = model

                            CcuLog.i(TAG, "bacnet fetchBacnetModelDetails success received points-->${model.points.size}")
                            CcuLog.i(TAG, "bacnet fetchBacnetModelDetails success received id-->${equipmentDevice.id}")
                            CcuLog.i(TAG, "bacnet fetchBacnetModelDetails success received name-->${equipmentDevice.name} <--display name->${equipmentDevice.displayName}")
                            CcuLog.i(TAG, "bacnet fetchBacnetModelDetails success received modelType-->${equipmentDevice.modelType}")
                            CcuLog.i(TAG, "bacnet fetchBacnetModelDetails success received modelConfig-->${equipmentDevice.modelConfig}")
                            CcuLog.i(TAG, "bacnet fetchBacnetModelDetails success received bacnetConfig-->${equipmentDevice.bacnetConfig}")
                            //CcuLog.i(TAG, "bacnet fetchBacnetModelDetails success received bacnetConfig-->${equipmentDevice.bacnetConfig}")
                            configModel.value.isStateChanged = true
                        } else {
                            showErrorDialog(context, NO_MODEL_DATA_FOUND)
                        }
                    } catch (e: JsonParseException) {
                        showErrorDialog(context, NO_INTERNET)
                    }
                } else {
                    showErrorDialog(context, NO_INTERNET)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    fun getExternalProfileSelected(){
        if(PreferenceUtil.getSelectedProfileWithAhu() == "bacnet"){
            configType.value = ConfigType.BACNET
        }else if(PreferenceUtil.getSelectedProfileWithAhu() == "modbus"){
            configType.value = ConfigType.MODBUS
        }
    }

    private fun resetBacnetModelAndConfig(){
        CcuLog.d(TAG_BACNET, "--resetBacnetModelAndConfig--if user switch from bacnet to modbus and back to bacnet")
        bacnetModel.value = BacnetModel()
        isConnectedDevicesSearchFinished.value = false
        connectedDevices.value =emptyList<BacnetDevice>()
        destinationIp.value  = ""
        deviceId.value  = ""
        destinationPort.value  = ""
        destinationMacAddress.value  = ""
        dnet.value = ""
        bacnetPropertiesFetched.value = false
    }

    fun resetBacnetNetworkConfig(){
        isConnectedDevicesSearchFinished.value = false
        connectedDevices.value =emptyList<BacnetDevice>()
        destinationIp.value  = ""
        deviceId.value  = ""
        destinationPort.value  = ""
        destinationMacAddress.value  = ""
        dnet.value = ""
        bacnetPropertiesFetched.value = false
    }

    private fun updateDisplayInUi(newState: Boolean) {
        var isAllOn = true
        var isAllOff = true
        for(point in bacnetModel.value.points){
            if(!point.displayInUi.value){
                isAllOn = false
                break
            }

            if(!point.displayInUi.value){
                isAllOff = false
                break
            }
        }
        if(newState){
            displayInUi.value = isAllOn
        }else{
            displayInUi.value = !isAllOff
        }
    }

    fun getDisabledOptions(): List<String> {
        if(isBacntEnabled.value){
            return listOf()
        }else{
            return listOf(BACNET)
        }
    }

    fun isValidIPv4(ip: String): Boolean {
        val ipv4Regex =
            Regex("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$")
        return ipv4Regex.matches(ip)
    }
}