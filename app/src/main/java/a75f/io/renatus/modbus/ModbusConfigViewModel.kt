package a75f.io.renatus.modbus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.device.modbus.buildModbusModel
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusProfile
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.modbus.util.NO_INTERNET
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.OK
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
import a75f.io.renatus.modbus.util.SAVED
import a75f.io.renatus.modbus.util.SAVING
import a75f.io.renatus.modbus.util.WARNING
import a75f.io.renatus.modbus.util.getParameters
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.modbus.util.isAllParamsSelected
import a75f.io.renatus.modbus.util.parseModbusDataFromString
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.JsonParseException
import kotlin.properties.Delegates


/**
 * Created by Manjunath K on 14-07-2023.
 */

class ModbusConfigViewModel(application: Application) : AndroidViewModel(application) {

    var deviceList = mutableStateOf(emptyList<String>())
    var slaveIdList = mutableStateOf(emptyList<String>())
    var childSlaveIdList = mutableStateOf(emptyList<String>())
    var equipModel = mutableStateOf(EquipModel())
    var selectedModbusType = mutableStateOf(0)
    var modelName = mutableStateOf("Select Model")

    private lateinit var modbusProfile: ModbusProfile
    private lateinit var filer: String

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var moduleLevel: String
    lateinit var profileType: ProfileType
    lateinit var deviceModelList: List<ModelMetaData>

    @SuppressLint("StaticFieldLeak")
    lateinit var context: Context

    private var selectedSlaveId by Delegates.notNull<Short>()
    private var domainService = DomainService()
    private val _isDialogOpen = MutableLiveData<Boolean>()
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen

    val onItemSelect = object : OnItemSelect {
        override fun onItemSelected(index: Int, item: String) {
            selectedModbusType.value = index
            modelName.value = item
            ProgressDialogUtils.showProgressDialog( context, "Fetching $item details")
            fetchModelDetails(item)
        }
    }

    fun configModelDefinition(context: Context) {
        this.context = context
        if (L.getProfile(selectedSlaveId) != null) {
                modbusProfile = L.getProfile(selectedSlaveId) as ModbusProfile
                if (modbusProfile != null) {
                    selectedSlaveId = modbusProfile.slaveId
                    val equipmentDevice = buildModbusModel(selectedSlaveId.toInt())
                    val model = EquipModel()
                    model.equipDevice.value = equipmentDevice
                    model.selectAllParameters.value = isAllParamsSelected(equipmentDevice)
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
                    if (subDeviceList.isNotEmpty())
                        equipModel.value.subEquips = subDeviceList
                    else
                        equipModel.value.subEquips = mutableListOf()
                }
            } else {
                ProgressDialogUtils.showProgressDialog(context, LOADING)
                readDeviceModels()
            }
        childSlaveIdList.value = getSlaveIds(false)
        slaveIdList.value = getSlaveIds(true)
        if (!equipModel.value.isDevicePaired)
            equipModel.value.slaveId.value = 1
    }

    fun holdBundleValues(bundle: Bundle) {
        selectedSlaveId = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        filer = bundle.getString(FragmentCommonBundleArgs.MODBUS_FILTER)!!
        val profileOriginalValue = bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)
        profileType = ProfileType.values()[profileOriginalValue]
        val level = ModbusLevel.values()[bundle.getInt(FragmentCommonBundleArgs.MODBUS_LEVEL)]
        moduleLevel = if (level == ModbusLevel.ZONE ) "zone" else "system"
    }

    private fun readDeviceModels() {
        domainService.readModbusModelsList(filer, object : ResponseCallback {
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
                            model.jsonContent = response
                            model.equipDevice.value = equipmentDevice
                            model.parameters = getParameters(equipmentDevice)
                            model.version.value = version
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

    private fun getSlaveIds(isParent: Boolean): List<String> {
        val slaveAddress: ArrayList<String> = ArrayList()
        if (!isParent) slaveAddress.add(SAME_AS_PARENT)
        for (i in 1..247) slaveAddress.add(i.toString())
        return slaveAddress
    }

    private fun getModelIdByName(name: String): String {
        return deviceModelList.find { it.name == name }!!.id
    }
    private fun getVersionByID(id: String): String {
        return deviceModelList.find { it.id == id }!!.version
    }

    private fun isValidConfiguration(): Boolean {
        if (equipModel.value.parameters.isEmpty()) {
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
        val zoneEquips = HSUtil.getEquips(zoneRef)
        if (!zoneRef.contentEquals("SYSTEM") && zoneEquips.size > 0) {
            if (equipModel.value.equipDevice.value.equipType.contains("EMR",true)) {
                showToast("Unpair all Modbus Modules and try to pair Energy meter",context)
                return false
            }
            zoneEquips.forEach {
                if (it.equipType.contains("EMR",true)) {
                    showToast("Zone should have no equips to pair Energy meter",context)
                    return false
                }
            }
        }
        return true
    }


    fun saveConfiguration() {
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
                _isDialogOpen.value = false
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
            modbusProfile.addMbEquip(equipModel.value.slaveId.value.toShort(), floorRef, zoneRef,
                equipModel.value.equipDevice.value, getParametersList(equipModel.value.equipDevice.value),
                profileType, subEquipmentDevices,moduleLevel,equipModel.value.version.value)

            L.ccu().zoneProfiles.add(modbusProfile)
            L.saveCCUState()
        } else {
            updateModbusProfile()
        }
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

    private fun getModbusEquipMap(slaveId: Short): HashMap<Any, Any>? {
        return CCUHsApi.getInstance()
            .readEntity("equip and modbus and not equipRef and group == \"$slaveId\"")
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

    fun showErrorDialog(context: Context, message: String, wantToDismiss: Boolean) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(WARNING)
        builder.setIcon(R.drawable.ic_warning)
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton(OK) { dialog, _ ->
            if (wantToDismiss)
                _isDialogOpen.value = false
            dialog.dismiss()
        }
        builder.create().show()
    }


    private fun isModbusExist(): Boolean {
        return (CCUHsApi.getInstance().readAllEntities(
            "equip and modbus and roomRef == \"SYSTEM\"")).isNotEmpty()
    }



}