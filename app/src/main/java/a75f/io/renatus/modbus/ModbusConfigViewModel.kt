package a75f.io.renatus.modbus

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.modbusbox.ModbusParser
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.util.ProgressDialogUtils
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.google.gson.JsonParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Created by Manjunath K on 14-07-2023.
 */

class ModbusConfigViewModel(application: Application) : AndroidViewModel(application) {

    var deviceList = mutableStateOf(emptyList<String>())
    var slaveIdList = mutableStateOf(emptyList<String>())
    var domainService = DomainService()
    var equipDevice = mutableStateOf(EquipmentDevice())
    lateinit var context: Context
    lateinit var deviceModelList:  List<ModelMetaData>

    var equipModel = mutableStateOf(EquipModel())

    fun configModelDefinition(context: Context) {
        readDeviceModels()
        slaveIdList.value = getSlaveIds()
        this.context = context
    }


    private fun readDeviceModels() {
        domainService.readModbusModelsList("", object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                try {
                    if (!response.isNullOrEmpty()) {
                        val itemList = mutableListOf<String>()
                        deviceModelList = getModelListFromJson(response)
                        deviceModelList.forEach {
                            itemList.add(it.name)
                        }
                        deviceList.value = itemList
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                Log.i("Domain", "onSuccessResponse: $response")
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    fun fetchModelDetails(selectedDevice: String) {
        val modelId = getModelIdByName(selectedDevice)
        domainService.readModelById(modelId, object : ResponseCallback{
            override fun onSuccessResponse(response: String?) {
                if (!response.isNullOrEmpty()) {
                    try {
                        val equipmentDevice = ModbusParser().parseModbusDataFromString(response)
                        Log.i("Domain", "onSuccessResponse: ${JSONObject(response).toString()}")
                        if (equipmentDevice != null) {
                            equipDevice.value = equipmentDevice
                            val model = EquipModel()
                            model.equipDevice = equipDevice
                            model.parameters = getParameters(equipmentDevice)
                            val subDeviceList = mutableListOf<MutableState<EquipModel>>()

                            equipDevice.value.equips.forEach {
                                val subEquip = EquipModel()
                                subEquip.equipDevice.value = it
                                subEquip.parameters = getParameters(it)
                                subDeviceList.add(mutableStateOf(subEquip))
                            }
                            equipModel.value = model
                            equipModel.value.subEquips = subDeviceList
                            // Log.i("Domain", "onSuccessResponse: ${equipmentDevice.equipType}")
                        } else {
                            Log.i("Domain", "onSuccessResponse: null")
                        }
                    } catch (e: JsonParseException) {
                        e.printStackTrace()
                    }
                } else {
                    Log.i("Domain", "onSuccessResponse: $response")
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                Log.i("Domain", "onSuccessResponse: $response")
                ProgressDialogUtils.hideProgressDialog()
            }

        })
    }
    private fun getSlaveIds(): List<String> {
        val slaveAddress: ArrayList<String> = ArrayList()
        for (i in 1..247) slaveAddress.add(i.toString())
        return slaveAddress
    }
    private fun getModelIdByName(name : String) : String {
        return deviceModelList.find { it.name == name }!!.id
    }

    fun saveConfiguration(){
        equipModel.value.parameters.forEach {
            Log.i("Domain", "saveConfiguration: ${it.param.value.name} ${it.displayInUi} ${it.param.value.parameterId}")
        }
        equipModel.value.subEquips.forEach {subEquip ->
            subEquip.value.parameters.forEach {
                Log.i("Domain", "saveConfiguration: ${it.param.value.name} ${it.displayInUi} ${it.param.value.parameterId}")
            }
        }

    }
    fun onSelectAll(isSelected: Boolean){
        equipModel.value.parameters.forEach {
            it.displayInUi.value = isSelected
        }
        equipModel.value.subEquips.forEach { subEquip ->
            subEquip.value.selectAllParameters.value = isSelected
            subEquip.value.parameters.forEach {
                it.displayInUi.value = isSelected
            }
        }
    }


}