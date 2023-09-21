package a75f.io.renatus.dabextahu

import a75f.io.domain.config.AdvancedAhuConfiguration
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.NO_INTERNET
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.getParameters
import a75f.io.renatus.modbus.util.getSlaveIds
import a75f.io.renatus.modbus.util.parseModbusDataFromString
import a75f.io.renatus.modbus.util.showErrorDialog
import a75f.io.renatus.util.ProgressDialogUtils
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.gson.JsonParseException

/**
 * Created by Manjunath K on 08-08-2022.
 */

class AhuControlViewModel(application: Application) : AndroidViewModel(application) {

    var deviceList = mutableStateOf(emptyList<String>())
    var slaveIdList = mutableStateOf(emptyList<String>())
    var childSlaveIdList = mutableStateOf(emptyList<String>())
    var equipModel = mutableStateOf(EquipModel())
    var selectedModbusType = mutableStateOf(0)
    var modelName = mutableStateOf("Select Model")

    var configType = mutableStateOf(ConfigType.BACNET)
    lateinit var deviceModelList: List<ModelMetaData>

    var setPointControl: Boolean by mutableStateOf(false)
    var dualSetPointControl: Boolean by mutableStateOf(false)
    var fanStaticSetPointControl: Boolean by mutableStateOf(false)
    var dcvControl: Boolean by mutableStateOf(false)
    var occupancyMode: Boolean by mutableStateOf(false)
    var humidifierControl: Boolean by mutableStateOf(false)
    var dehumidifierControl: Boolean by mutableStateOf(false)

    var heatingMinSp: String by mutableStateOf("1.0")
    var heatingMaxSp: String by mutableStateOf("1.0")
    var coolingMinSp: String by mutableStateOf("1.0")
    var coolingMaxSp: String by mutableStateOf("1.0")

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    //private val domainModeler = DomainModeler(application.baseContext)
    private var domainService = DomainService()
    val onItemSelect = object : OnItemSelect {
        override fun onItemSelected(index: Int, item: String) {
            selectedModbusType.value = index
            modelName.value = item
            ProgressDialogUtils.showProgressDialog( context, "Fetching $item details")
            fetchModelDetails(item)
        }
    }

    fun configModbusDetails() {
        ProgressDialogUtils.showProgressDialog(context, LOADING)
        readDeviceModels()
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
                        showErrorDialog(context, MODBUS_DEVICE_LIST_NOT_FOUND, true)
                    }
                } catch (e: Exception) {
                    showErrorDialog(context, NO_INTERNET, true)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET, true)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }


    fun configModelDefinition(nodeType: NodeType, profile: ProfileType, context: Context) {
        this.context = context
        childSlaveIdList.value = getSlaveIds(false)
        slaveIdList.value = getSlaveIds(true)
        if (!equipModel.value.isDevicePaired)
            equipModel.value.slaveId.value = 1
        //var modelDef = getModelByProfileName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
    }


    fun saveConfiguration() {
        val profile = AdvancedAhuConfiguration(1000, "HS", 0, "", "")
        //domainModeler.addEquip(profileConfiguration = profile)
        //Log.i("Domain", "save configuration: ${getValues()}")

    }

    private fun getValues(): String {
        return "setPointControl: $setPointControl " +
                "dualSetPointControl $dualSetPointControl " +
                "heatingMinSp $heatingMinSp heatingMaxSp $heatingMaxSp " +
                "coolingMinSp $coolingMinSp coolingMaxSp $coolingMaxSp "
    }


    fun getOptions(): List<String> {
        // TODO read it from model profile definition
        return listOf("1.0", "2.0", "3.0", "4.0", "5.0")
    }

    fun getIndexFromVal(value: String): Int {
        return getOptions().indexOf(value)
    }

    enum class ConfigType {
        BACNET,MODBUS
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
                            showErrorDialog(context, NO_MODEL_DATA_FOUND, false)
                        }
                    } catch (e: JsonParseException) {
                        showErrorDialog(context, NO_INTERNET, true)
                    }
                } else {
                    showErrorDialog(context, NO_INTERNET, true)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, NO_INTERNET, true)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
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


}