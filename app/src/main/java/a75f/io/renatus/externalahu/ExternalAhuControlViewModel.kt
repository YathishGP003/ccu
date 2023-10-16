package a75f.io.renatus.externalahu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.domain.util.ModelNames
import a75f.io.domain.util.ModelSource.Companion.getModelByProfileName
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusProfile
import a75f.io.logic.bo.building.system.dab.DabExternalAhu
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.NO_INTERNET
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.SAVED
import a75f.io.renatus.modbus.util.SAVING
import a75f.io.renatus.modbus.util.getParameters
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.modbus.util.getSlaveIds
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

/**
 * Created by Manjunath K on 08-08-2022.
 */

class ExternalAhuControlViewModel(application: Application) : AndroidViewModel(application) {

    var deviceList = mutableStateOf(emptyList<String>())
    var slaveIdList = mutableStateOf(emptyList<String>())
    var equipModel = mutableStateOf(EquipModel())
    var selectedModbusType = mutableStateOf(0)
    var modelName = mutableStateOf("Select Model")

    var configType = mutableStateOf(ConfigType.BACNET)
    lateinit var deviceModelList: List<ModelMetaData>
    lateinit var profileModelDefinition: SeventyFiveFProfileDirective

    var configModel = mutableStateOf(ExternalAhuConfigModel())
    var systemProfile: DabExternalAhu? = null
    private lateinit var modbusProfile: ModbusProfile
    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

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
        ProgressDialogUtils.showProgressDialog(context, LOADING)
        readDeviceModels()
    }

    fun configModelDefinition(context: Context) {
        try {
            this.context = context
            if (L.ccu().systemProfile.profileType == ProfileType.SYSTEM_DAB_EXTERNAL_AHU) {
                systemProfile = L.ccu().systemProfile as DabExternalAhu
                setCurrentConfig(systemProfile!!.getConfiguration())
            }
            slaveIdList.value = getSlaveIds(true)
            if (!equipModel.value.isDevicePaired)
                equipModel.value.slaveId.value = 1
            loadModel()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            ProgressDialogUtils.hideProgressDialog()
        }
    }

    private fun loadModel() {
        val def = getModelByProfileName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        if (def != null) {
            profileModelDefinition = def as SeventyFiveFProfileDirective
            configModel.value.render(profileModelDefinition)
        }
    }

    private fun setCurrentConfig(config: ExternalAhuConfiguration) {
        configModel.value.setPointControl = config.setPointControl.enabled
        configModel.value.dualSetPointControl = config.dualSetPointControl.enabled
        configModel.value.fanStaticSetPointControl = config.fanStaticSetPointControl.enabled
        configModel.value.dcvControl = config.dcvControl.enabled
        configModel.value.occupancyMode = config.occupancyMode.enabled
        configModel.value.humidifierControl = config.humidifierControl.enabled
        configModel.value.dehumidifierControl = config.dehumidifierControl.enabled

        configModel.value.satMin = config.satMin.currentVal.toString()
        configModel.value.satMax = config.satMax.currentVal.toString()
        configModel.value.heatingMinSp = config.heatingMinSp.currentVal.toString()
        configModel.value.heatingMaxSp = config.heatingMaxSp.currentVal.toString()
        configModel.value.coolingMinSp = config.coolingMinSp.currentVal.toString()
        configModel.value.coolingMaxSp = config.coolingMaxSp.currentVal.toString()
        configModel.value.fanMinSp = config.fanMinSp.currentVal.toString()
        configModel.value.fanMaxSp = config.fanMaxSp.currentVal.toString()
        configModel.value.dcvMin = config.dcvMin.currentVal.toString()
        configModel.value.dcvMax = config.dcvMax.currentVal.toString()
        configModel.value.targetHumidity = config.targetHumidity.currentVal.toString()
        configModel.value.targetDeHumidity = config.targetDeHumidity.currentVal.toString()
    }

    fun saveConfiguration() {

        RxjavaUtil.executeBackgroundTask(
            { ProgressDialogUtils.showProgressDialog(context, "Saving Profile Configuration...") },
            {
                if (L.ccu().systemProfile != null) {
                    if (L.ccu().systemProfile.profileType != ProfileType.SYSTEM_DAB_EXTERNAL_AHU) {
                        L.ccu().systemProfile!!.deleteSystemEquip()
                        L.ccu().systemProfile = null
                        addEquip()
                    } else {
                        updateSystemProfile()
                    }
                } else {
                    addEquip()
                }
            },

            {
                ProgressDialogUtils.hideProgressDialog()
                CCUHsApi.getInstance().saveTagsData()
                CCUHsApi.getInstance().syncEntityTree()
                showToast("Configuration saved successfully", context)
            }
        )
    }

    fun saveModbusConfiguration() {
        if (isValidConfiguration()) {
            populateSlaveId()
            RxjavaUtil.executeBackgroundTask({
                ProgressDialogUtils.showProgressDialog(context, SAVING)
            }, {
                CCUHsApi.getInstance().resetCcuReady()
                setUpsModbusProfile()
                L.saveCCUState()
                CCUHsApi.getInstance().setCcuReady()
            }, {
                ProgressDialogUtils.hideProgressDialog()
                context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                showToast(SAVED, context)
            })
        }
    }



    private fun setUpsModbusProfile() {
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
            modbusProfile.addMbEquip(equipModel.value.slaveId.value.toShort(), "SYSTEM", "SYSTEM",
                equipModel.value.equipDevice.value, getParametersList(equipModel.value.equipDevice.value),
                 ProfileType.SYSTEM_DAB_EXTERNAL_AHU , subEquipmentDevices,"system",equipModel.value.version.value)

            L.ccu().zoneProfiles.add(modbusProfile)
            L.saveCCUState()
        } else {
            updateModbusProfile()
        }
    }

    private fun isValidConfiguration(): Boolean {
        // do all the validations
        return false
      /*  if (equipModel.value.parameters.isEmpty()) {
            showToast("Please select modbus device", context)
            return false
        }
        if (equipModel.value.isDevicePaired)
            return true // If it is paired then will not allow the use to to edit slave id

        if (zoneRef.contentEquals("SYSTEM")) {
            if (equipModel.value.subEquips.isNotEmpty() && isModbusExist()) {
                showToast("Modbus device already paired", context)
                return false
            }
        } else {
            if (equipModel.value.subEquips.isNotEmpty() && HSUtil.getEquips(zoneRef).isNotEmpty()) {
                showToast("Zone should have no equips to pair modbus with sub equips", context)
                return false
            }
        }

        if (L.isModbusSlaveIdExists(equipModel.value.slaveId.value.toShort())) {
            showToast("Slave Id " + equipModel.value.slaveId.value + " already exists, choose " +
                    "another slave id to proceed",context)
            return false
        }
        if (equipModel.value.subEquips.isNotEmpty()) {
            equipModel.value.subEquips.forEach {
                if  (it.value.slaveId.value != 0) {
                    if (L.isModbusSlaveIdExists(it.value.slaveId.value.toShort())
                        || it.value.slaveId.value == equipModel.value.slaveId.value ) {
                        showToast("Make sure all sub equips have unique slave Id, if it is not same as Parent",context)
                        return false
                    }
                }
            }
        }
        return true*/
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

    fun updateSystemProfile() {

    }

    private fun addEquip() {
        systemProfile = DabExternalAhu()
        systemProfile!!.addSystemEquip(configModel.value.getConfiguration(), profileModelDefinition)
        L.ccu().systemProfile = systemProfile
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

    private fun getModelIdByName(name: String): String {
        return deviceModelList.find { it.name == name }!!.id
    }

    private fun getVersionByID(id: String): String {
        return deviceModelList.find { it.id == id }!!.version
    }

    fun onSelectAll(isSelected: Boolean) {
        if (equipModel.value.parameters.isNotEmpty()) {
            equipModel.value.parameters.forEach {
                it.displayInUi.value = isSelected
            }
            if (equipModel.value.subEquips.isNotEmpty()) {
                equipModel.value.subEquips.forEach { subEquip ->
                    subEquip.value.selectAllParameters.value = isSelected
                    subEquip.value.parameters.forEach {
                        it.displayInUi.value = isSelected
                    }
                }
            }
        }
    }

    fun updateSelectAll() {
        var isAllSelected = true
        if (equipModel.value.parameters.isNotEmpty()) {
            equipModel.value.parameters.forEach {
                if (!it.displayInUi.value)
                    isAllSelected = false
            }
        }
        if (equipModel.value.subEquips.isNotEmpty()) {
            equipModel.value.subEquips.forEach { subEquip ->
                subEquip.value.parameters.forEach {
                    if (!it.displayInUi.value)
                        isAllSelected = false
                }
            }
        }
        equipModel.value.selectAllParameters.value = isAllSelected
    }


    enum class ConfigType {
        BACNET, MODBUS
    }

    fun fetchModelDetails(selectedDevice: String) {
        val modelId = getModelIdByName(selectedDevice)
        domainService.readModelById(modelId, object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                if (!response.isNullOrEmpty()) {
                    try {
                        val equipmentDevice = parseModbusDataFromString(response)
                        if (equipmentDevice != null) {
                            val model = EquipModel()
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
        domainService.readModbusModelsList("external", object : ResponseCallback {
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
            equipModel.value.equipDevice.value,
            getParametersList(equipModel.value)
        )

        equipModel.value.subEquips.forEach {
            modbusProfile.updateModbusEquip(
                it.value.equipDevice.value.deviceEquipRef,
                it.value.equipDevice.value.slaveId.toShort(),
                it.value.equipDevice.value,
                getParametersList(it.value)
            )
        }
        L.ccu().zoneProfiles.add(modbusProfile)
        L.saveCCUState()
    }

}