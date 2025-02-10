package a75f.io.renatus.modbus.util

import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.bacnet.parser.BacnetPoint
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.renatus.R
import a75f.io.renatus.bacnet.models.BacnetPointState
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.RegisterItem
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
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
 fun isAllParamsSelectedBacNet(equipDevice: BacnetModelDetailResponse) : Boolean {
    var isAllSelected = true
    if (equipDevice.points.isNotEmpty()) {
        equipDevice.points.forEach {
            if (!it.protocolData?.bacnet?.displayInUIDefault!!)
                isAllSelected = false
        }
    }
    return isAllSelected
}
 fun getBacnetPoints(points: List<BacnetPoint>): MutableList<BacnetPointState> {
    val parameterList = mutableListOf<BacnetPointState>()
    if (Objects.nonNull(points)) {
        for (bacnetPoint in points) {
            val bacnetPointState = BacnetPointState(
                bacnetPoint.id,
                bacnetPoint.name,
                bacnetPoint.domainName,
                bacnetPoint.kind,
                bacnetPoint.valueConstraint,
                bacnetPoint.hisInterpolate,
                bacnetPoint.protocolData,
                bacnetPoint.defaultUnit,
                bacnetPoint.defaultValue,
                bacnetPoint.equipTagNames,
                bacnetPoint.rootTagNames,
                bacnetPoint.descriptiveTags,
                bacnetPoint.equipTagsList,
                bacnetPoint.bacnetProperties,
                displayInUi = mutableStateOf(bacnetPoint.protocolData?.bacnet!!.displayInUIDefault),
                disName = bacnetPoint.disName
            )
            parameterList.add(bacnetPointState)
        }
    }
    return parameterList
}

fun isAllParamsSelected(equipDevice: EquipmentDevice) : Boolean {
    var isAllSelected = true
    if (equipDevice.registers.isNotEmpty()) {
        equipDevice.registers.forEach {
            if (!it.parameters[0].isDisplayInUI)
                isAllSelected = false
        }
    }
    if (equipDevice.equips.isNotEmpty()) {
        equipDevice.equips.forEach { subEquip ->
            if (subEquip.registers.isNotEmpty()) {
                subEquip.registers[0].parameters.forEach {
                    if (!it.isDisplayInUI)
                        isAllSelected = false
                }
            }
        }
    }
    return isAllSelected
}
fun isAllLeftParamsSelected(equipDevice: EquipmentDevice) : Boolean {
    var isAllSelected = true
    if (equipDevice.registers.isNotEmpty()) {
        equipDevice.registers.forEachIndexed { index, it ->
            if ((index%2 == 0) && (!it.parameters[0].isDisplayInUI))
                isAllSelected = false
        }
    }
    return isAllSelected
}
fun isAllRightParamsSelected(equipDevice: EquipmentDevice) : Boolean {
    var isAllSelected = true
    if (equipDevice.registers.isNotEmpty()) {
        equipDevice.registers.forEachIndexed { index, it ->
            if ((index%2 != 0) && (!it.parameters[0].isDisplayInUI))
                isAllSelected = false
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
    CcuLog.i(L.TAG_CCU_DM_MODBUS,msg)
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


const val BACNET = "BACNET"
const val MODBUS = "MODBUS"
const val EQUIP_TYPE = "Equipment Type"
const val LOADING = "Loading Modbus Models"
const val SLAVE_ID = "Slave ID"
const val SELECT_ALL = "Select All Parameters"
const val SET = "SET"
const val SAVE = "SAVE"
const val CANCEL = "CANCEL"
const val PARAMETER = "PARAMETER"
const val SAME_AS_PARENT = "Same As Parent"
const val DISPLAY_UI = "DISPLAY IN UI"
const val SAVING = "Saving Modbus configuration"
const val SAVING_BACNET = "Saving Bacnet configuration"
const val SAVED = "Saved all the configuration"
const val NO_MODEL_DATA_FOUND = "No model data found..!"
const val MODBUS_DEVICE_LIST_NOT_FOUND = "Modbus device list not found..!"
const val BACNET_DEVICE_LIST_NOT_FOUND = "Bacnet device list not found..!"
const val NO_INTERNET = "Unable to fetch equipments, please confirm your Wifi connectivity"
const val WARNING = "Warning"
const val ALERT = "Alert"
const val OK = "Okay"
const val SEARCH_MODEL = "Search model"
const val SEARCH_SLAVE_ID = "Search Slave Id"
const val MODELLED_VALUE = "MODELLED VALUE"
const val DEVICE_VALUE = "DEVICE VALUE"

const val BAC_PROP_PRESENT_VALUE = "Present Value"
const val BAC_PROP_UNIT = "Unit"
const val BAC_PROP_MIN_PRESENT_VALUE = "Min_Pres_Value"
const val BAC_PROP_MAX_PRESENT_VALUE = "Max_Pres_Value"
const val BAC_PROP_COV_INCREMENT = "COV_Increment"
const val BAC_PROP_NOT_FETCHED = "Not Fetched"
const val FETCH = "FETCH"
const val RE_FETCH = "RE-FETCH"
const val SEARCH_DEVICE = "Select Device"

const val DEVICE_ID = "Device ID"
const val
        DEVICE_IP = "Device IP/ PORT"
const val DEVICE_NETWORK = "Device Network"
const val DEVICE_NAME = "Device Name"

const val DESTINATION_IP = "Destination IP"
const val DESTINATION_PORT = "PORT"





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
fun formattedToastMessage(message: String, context: Context) {
    val layoutInflater = LayoutInflater.from(context)
    val toastWarning = layoutInflater.inflate(
        R.layout.custom_toast_layout_warning,
        null
    )
    val toastImage = toastWarning.findViewById<ImageView>(R.id.custom_toast_image)
    val successToast = toastWarning.findViewById<TextView>(R.id.custom_toast_Title)
    val pasteText = toastWarning.findViewById<TextView>(R.id.custom_toast_message_detail)
    successToast.setText(R.string.Toast_Success_Title)
    successToast.setTextColor(ContextCompat.getColor(context,R.color.success_status))
    toastImage.setImageResource(R.drawable.font_awesome_custom_check_mark)
    toastImage.setColorFilter(ContextCompat.getColor(context,R.color.success_status))
    toastWarning.setPadding(20, 20, 20, 20)

    pasteText.text = message
    val toast = Toast(context)
    toast.setGravity(Gravity.BOTTOM, 50, 50)
    toast.view = toastWarning
    toast.duration = Toast.LENGTH_LONG
    toast.show()
}
fun getNodeType(device: HashMap<Any, Any>): NodeType? {
    val domainName = device["domainName"]?.toString() ?: return null

    return when {
        domainName.contains("helionode",true) -> NodeType.HELIO_NODE
        domainName.contains("smartnode",true) -> NodeType.SMART_NODE
        domainName.contains("hyperstatsplit",true) -> NodeType.HYPERSTATSPLIT
        domainName.contains("hyperstat",true) -> NodeType.HYPER_STAT
        domainName.contains("otn",true) -> NodeType.OTN
        else -> null
    }
}

