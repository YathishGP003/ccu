package a75f.io.renatus.profiles.system.externalahu

import a75f.io.api.haystack.CCUHsApi
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
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusProfile
import a75f.io.logic.bo.building.system.SystemProfile
import a75f.io.logic.bo.building.system.addSystemEquip
import a75f.io.logic.bo.building.system.dab.DabExternalAhu
import a75f.io.logic.bo.building.system.getConfiguration
import a75f.io.logic.bo.building.system.vav.VavExternalAhu
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.PreferenceUtil
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.RegisterItemForSubEquip
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.NO_INTERNET
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.getParameters
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.modbus.util.getSlaveIds
import a75f.io.renatus.modbus.util.isAllLeftParamsSelected
import a75f.io.renatus.modbus.util.isAllRightParamsSelected
import a75f.io.renatus.modbus.util.parseModbusDataFromString
import a75f.io.renatus.modbus.util.showErrorDialog
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.google.gson.JsonParseException
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
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

    val TAG: String = "CCU_SYSTEM"

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context
    lateinit var profileType: ProfileType

    private var domainService = DomainService()
    val onItemSelect = object : OnItemSelect {
        override fun onItemSelected(index: Int, item: String) {
            selectedModbusType.value = index
            modelName.value = item
            ProgressDialogUtils.showProgressDialog(context, "Fetching $item details")
            fetchModelDetails(item)
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

            getModbusConfiguration(profileType)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ProgressDialogUtils.hideProgressDialog()
        }
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
        if (checkValidConfiguration()) {
            RxjavaUtil.executeBackgroundTask({
                ProgressDialogUtils.showProgressDialog(
                    context, "Saving Profile Configuration..."
                )
            }, {
                if (L.ccu().systemProfile != null) {
                    if (profileType != L.ccu().systemProfile.profileType) {
                        L.ccu().systemProfile!!.deleteSystemEquip()
                        L.ccu().systemProfile = null
                        addEquip()
                        saveExternalEquip()
                    } else {
                        updateSystemProfile()
                    }
                } else {
                    addEquip()
                    saveExternalEquip()
                }
            }, {
                L.saveCCUState()
                CCUHsApi.getInstance().setCcuReady()
                CCUHsApi.getInstance().syncEntityTree()
                context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                ProgressDialogUtils.hideProgressDialog()
                equipModel.value.isDevicePaired = true
                showToast("Configuration saved successfully", context)
                configModel.value.isStateChanged = false
            })
        }
    }

    private fun checkValidConfiguration(): Boolean {
        //TODO check validations for bacnet if configured as bacnet
        if (configType.value == ConfigType.MODBUS && (!isValidConfiguration())) return false
        if (configType.value == ConfigType.BACNET) {
            showToast("Bacnet configuration is not available", context)
            return false
        }
        return true
    }

    private fun saveExternalEquip() {
        if (configType.value == ConfigType.MODBUS) {
            populateSlaveId()
            CCUHsApi.getInstance().resetCcuReady()
            setUpsModbusProfile(profileType)
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

}