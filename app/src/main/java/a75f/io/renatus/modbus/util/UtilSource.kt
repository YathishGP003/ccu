package a75f.io.renatus.modbus.util

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.RegisterItem
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
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
    Toast.makeText(context, text, Toast.LENGTH_LONG).show()
}
fun log(msg: String) {
    Log.i("DMModbus",msg)
}


const val MODBUS = "MODBUS"
const val EQUIP_TYPE = "Equipment Type"
const val LOADING = "Loading Modbus Models"
const val SLAVE_ID = "Slave Id"
const val SELECT_ALL = "Select All Parameters"
const val SET = "Set"
const val PARAMETER = "PARAMETER"
const val SAME_AS_PARENT = "Same As Parent"
const val DISPLAY_UI = "DISPLAY UI"
const val SAVING = "Saving Modbus configuration"
const val SAVED = "Saved all the configuration"
const val NO_MODEL_DATA_FOUND = "No model data found..!"
const val MODBUS_DEVICE_LIST_NOT_FOUND = "Modbus device list not found..!"
const val WARNING = "Warning"
const val OK = "Ok"
