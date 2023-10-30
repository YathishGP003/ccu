package a75f.io.renatus.modbus.util

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.logger.CcuLog
import a75f.io.renatus.R
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.RegisterItem
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import java.util.Objects

/**
 * Created by Manjunath K on 19-07-2023.
 */

fun getParameters(equipment: EquipmentDevice): MutableList<RegisterItem> {
    val parameterList = mutableListOf<RegisterItem>()
    if (Objects.nonNull(equipment.registers)) {
        for (registerTemp in equipment.registers) {
            if (registerTemp.parameters != null) {
                for (parameterTemp in registerTemp.parameters) {
                    parameterTemp.registerNumber = registerTemp.getRegisterNumber()
                    parameterTemp.registerAddress = registerTemp.getRegisterAddress()
                    parameterTemp.registerType = registerTemp.getRegisterType()
                    parameterTemp.parameterDefinitionType = registerTemp.getParameterDefinitionType()
                    parameterTemp.multiplier = registerTemp.multiplier
                    val register = RegisterItem()
                    register.displayInUi.value = parameterTemp.isDisplayInUI
                    register.param = mutableStateOf(parameterTemp)
                    parameterList.add(register)
                }
            }
        }
    }
    return parameterList
}

fun getParametersList(equipment: EquipModel): List<Parameter> {
    val parameterList = mutableListOf<Parameter>()
    if (Objects.nonNull(equipment.parameters.isNotEmpty())) {
        equipment.parameters.forEach {
            val param = it.param.value
            param.isDisplayInUI = it.displayInUi.value
            parameterList.add(param)
        }
    }
    return parameterList
}

fun getParametersList(equipment: EquipmentDevice): List<Parameter> {
    val parameterList = mutableListOf<Parameter>()
    equipment.registers.forEach {
        val param = it.parameters[0]
        param.registerNumber = it.getRegisterNumber()
        param.registerAddress = it.getRegisterAddress()
        param.registerType = it.getRegisterType()
        param.parameterDefinitionType = it.getParameterDefinitionType()
        param.multiplier = it.getMultiplier()
        param.wordOrder = it.getWordOrder()
        parameterList.add(param)
    }
    return parameterList
}

fun isAllParamsSelected(equipDevice: EquipmentDevice) : Boolean {
    var isAllSelected = true
    if (equipDevice.registers.isNotEmpty()) {
        equipDevice.registers[0].parameters.forEach {
            if (!it.isDisplayInUI)
                isAllSelected = false
        }
    }
    if (equipDevice.equips.isNotEmpty()) {
        equipDevice.equips.forEach { subEquip ->
            subEquip.registers[0].parameters.forEach {
                if (!it.isDisplayInUI)
                    isAllSelected = false
            }
        }
    }
    return isAllSelected
}

 fun showToast(text: String, context: Context){
     Handler(Looper.getMainLooper()).post(kotlinx.coroutines.Runnable {
         Toast.makeText(context, text, Toast.LENGTH_LONG).show()
     })

}
fun log(msg: String) {
    CcuLog.i("DMModbus",msg)
}

fun parseModbusDataFromString(json: String?): EquipmentDevice? {
    var equipmentDevice: EquipmentDevice? = null
    try {
        val gson = Gson()
        equipmentDevice = gson.fromJson(json, EquipmentDevice::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return equipmentDevice
}


const val MODBUS = "MODBUS"
const val EQUIP_TYPE = "Equipment Type"
const val LOADING = "Loading Modbus Models"
const val SLAVE_ID = "Slave Id"
const val SELECT_ALL = "Select All Parameters"
const val SET = "SET"
const val SAVE = "SAVE"
const val CANCEL = "CANCEL"
const val PARAMETER = "PARAMETER"
const val SAME_AS_PARENT = "Same As Parent"
const val DISPLAY_UI = "DISPLAY UI"
const val SAVING = "Saving Modbus configuration"
const val SAVED = "Saved all the configuration"
const val NO_MODEL_DATA_FOUND = "No model data found..!"
const val MODBUS_DEVICE_LIST_NOT_FOUND = "Modbus device list not found..!"
const val NO_INTERNET = "Unable to fetch equipments, please confirm your Wifi connectivity"
const val WARNING = "Warning"
const val OK = "Ok"
const val SEARCH_MODEL = "Search model"
const val SEARCH_SLAVE_ID = "Search Slave Id"

fun getSlaveIds(isParent: Boolean): List<String> {
    val slaveAddress: ArrayList<String> = ArrayList()
    if (!isParent) slaveAddress.add(SAME_AS_PARENT)
    for (i in 1..247) slaveAddress.add(i.toString())
    return slaveAddress
}

fun showErrorDialog(context: Context, message: String) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(WARNING)
    builder.setIcon(R.drawable.ic_warning)
    builder.setMessage(message)
    builder.setCancelable(false)
    builder.setPositiveButton(OK) { dialog, _ ->
        dialog.dismiss()
    }
    builder.create().show()
}