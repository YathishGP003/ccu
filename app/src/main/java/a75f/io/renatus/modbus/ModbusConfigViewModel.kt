package a75f.io.renatus.modbus

import a75f.io.api.haystack.modbus.Parameter
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.modbusbox.ModbusParser
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.RegisterItem
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.compose.testModel
import a75f.io.renatus.util.ProgressDialogUtils
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Objects

/**
 * Created by Manjunath K on 14-07-2023.
 */

class ModbusConfigViewModel(application: Application) : AndroidViewModel(application) {

    var deviceList = mutableStateOf(emptyList<String>())
    var slaveIdList = mutableStateOf(emptyList<String>())
    var equipDetails = mutableStateOf(emptyList<RegisterItem>())
    var domainService = DomainService()
    lateinit var context: Context
    lateinit var deviceModelList:  List<ModelMetaData>
    fun configModelDefinition(context: Context) {
        ProgressDialogUtils.showProgressDialog(context, "Loading Models")
        readDeviceModels()
        slaveIdList.value = getSlaveIds()
        this.context = context
    }


    private fun readDeviceModels() {

        viewModelScope.launch {
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
    }

    fun fetchModelDetails(selectedDevice: String) {
        val modelId = getModelIdByName(selectedDevice)
        viewModelScope.launch {
            domainService.readModelById(modelId, object : ResponseCallback{
                override fun onSuccessResponse(response: String?) {
                    if (!response.isNullOrEmpty()) {
                        try {
                            val equipmentDevice = ModbusParser().parseModbusDataFromString(response)
                            Log.i("Domain", "onSuccessResponse: ${JSONObject(response).toString()}")
                            if (equipmentDevice != null) {
                                Log.i("Domain", "onSuccessResponse: ${equipmentDevice.equipType}")
                            } else {
                                Log.i("Domain", "onSuccessResponse: null")
                            }


                            val parameterList = mutableListOf<RegisterItem>()
                            if (equipmentDevice != null) {
                                if (Objects.nonNull(equipmentDevice.registers)) {
                                    for (registerTemp in equipmentDevice.registers) {
                                        if (registerTemp.parameters != null) {
                                            for (parameterTemp in registerTemp.parameters) {
                                                parameterTemp.registerNumber =
                                                    registerTemp.getRegisterNumber()
                                                parameterTemp.registerAddress =
                                                    registerTemp.getRegisterAddress()
                                                parameterTemp.registerType =
                                                    registerTemp.getRegisterType()
                                                parameterList.add(RegisterItem(parameterTemp,false))
                                            }
                                        }
                                    }
                                }
                                equipDetails.value = parameterList
                            }
                        } catch (e: JsonParseException) {
                            e.printStackTrace()
                        }
                    } else {
                        Log.i("Domain", "onSuccessResponse: $response")
                    }
                }

                override fun onErrorResponse(response: String?) {
                    Log.i("Domain", "onSuccessResponse: $response")
                }

            })
        }
    }
    private fun getSlaveIds(): List<String> {
        val slaveAddress: ArrayList<String> = ArrayList()
        for (i in 1..247) slaveAddress.add(i.toString())
        return slaveAddress
    }
    fun getModelIdByName(name : String) : String {
        return deviceModelList.find { it.name == name }!!.id
    }

}