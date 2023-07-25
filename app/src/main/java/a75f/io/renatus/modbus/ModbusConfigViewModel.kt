package a75f.io.renatus.modbus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusProfile
import a75f.io.modbusbox.ModbusParser
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.compose.ModelMetaData
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.ModbusModelBuilder
import a75f.io.renatus.modbus.models.ModbusModelBuilder.Companion.getModel
import a75f.io.renatus.modbus.models.ModbusModelBuilder.Companion.readFile
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.OK
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
import a75f.io.renatus.modbus.util.SAVED
import a75f.io.renatus.modbus.util.SAVING
import a75f.io.renatus.modbus.util.WARNING
import a75f.io.renatus.modbus.util.getParameters
import a75f.io.renatus.modbus.util.getParametersList
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
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

    lateinit var modbusProfile: ModbusProfile
    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var deviceModelList: List<ModelMetaData>
    lateinit var context: Context

    private var selectedSlaveId by Delegates.notNull<Short>()
    private var domainService = DomainService()
    private val _isDialogOpen = MutableLiveData<Boolean>()
    val isDialogOpen: LiveData<Boolean>
        get() = _isDialogOpen


    fun configModelDefinition(context: Context) {
        this.context = context
        if (L.getProfile(selectedSlaveId) != null) {

            if (L.getProfile(selectedSlaveId) != null) {
                modbusProfile = L.getProfile(selectedSlaveId) as ModbusProfile
                if (modbusProfile != null) {
                    selectedSlaveId = modbusProfile.slaveId

                    val equipmentDevice = getModel(selectedSlaveId.toInt())
                    if (equipmentDevice != null) {
                        val model = EquipModel()
                        model.equipDevice.value = equipmentDevice
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
                        if (subDeviceList.isNotEmpty()) {
                            equipModel.value.subEquips = subDeviceList
                        } else {
                            equipModel.value.subEquips = mutableListOf()
                        }
                    } else {
                        showErrorDialog(context, NO_MODEL_DATA_FOUND, false)
                    }

                }
            }
        } else {
            ProgressDialogUtils.showProgressDialog(context, LOADING)
            readDeviceModels()
        }
        childSlaveIdList.value = getSlaveIds(false)
        slaveIdList.value = getSlaveIds(true)
    }

    fun holdBundleValues(bundle: Bundle) {
        selectedSlaveId = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        val profileOriginalValue = bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)
        profileType = ProfileType.values()[profileOriginalValue]


    }

    private fun readDeviceModels() {
        domainService.readModbusModelsList(String(), object : ResponseCallback {
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
                    showErrorDialog(context, e.localizedMessage!!, true)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, response!!, true)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    fun fetchModelDetails(selectedDevice: String) {
        val modelId = getModelIdByName(selectedDevice)
        domainService.readModelById(modelId, object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                if (!response.isNullOrEmpty()) {
                    try {
                        val equipmentDevice = ModbusParser().parseModbusDataFromString(response)

                        if (equipmentDevice != null) {
                            val model = EquipModel()
                            model.jsonContent = response
                            model.equipDevice.value = equipmentDevice
                            model.parameters = getParameters(equipmentDevice)
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
                        showErrorDialog(context, e.message!!, false)
                    }
                } else {
                    showErrorDialog(context, response!!, false)
                }
                ProgressDialogUtils.hideProgressDialog()
            }

            override fun onErrorResponse(response: String?) {
                showErrorDialog(context, response!!, false)
                ProgressDialogUtils.hideProgressDialog()
            }
        })
    }

    fun getSlaveIds(isParent: Boolean): List<String> {
        val slaveAddress: ArrayList<String> = ArrayList()
        if (!isParent) slaveAddress.add(SAME_AS_PARENT)
        for (i in 1..247) slaveAddress.add(i.toString())
        return slaveAddress
    }

    private fun getModelIdByName(name: String): String {
        return deviceModelList.find { it.name == name }!!.id
    }

    private fun isValidConfiguration(): Boolean {
        // TODO add all validations here
        return true
    }

    fun saveConfiguration() {
        if (isValidConfiguration()) {
            saveConfiguration(
                equipModel.value.equipDevice.value,
                equipModel.value.slaveId.value.toShort(),
                getParametersList(equipModel.value)
            )
        }
    }

    private fun saveConfiguration(
        equipmentDev: EquipmentDevice,
        selectedSlaveId: Short,
        modbusParam: List<Parameter>
    ) {
        RxjavaUtil.executeBackgroundTask({
            ProgressDialogUtils.showProgressDialog(
                context,
                SAVING
            )
        }, {
            CCUHsApi.getInstance().resetCcuReady()
            addModbusProfile(equipmentDev, selectedSlaveId, modbusParam)
            L.saveCCUState()
            CCUHsApi.getInstance().setCcuReady()
        }, {
            ProgressDialogUtils.hideProgressDialog()
            context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
            showToast(SAVED, context)
            _isDialogOpen.value = false
        })
    }

    private fun addModbusProfile(
        equipmentDev: EquipmentDevice, selectedSlaveId: Short, modbusParam: List<Parameter>
    ) {
        val subEquipmentDevices: MutableList<EquipmentDevice> = java.util.ArrayList()
        if (equipModel.value.subEquips.isNotEmpty()) {
            equipModel.value.subEquips.forEach {
                subEquipmentDevices.add(it.value.equipDevice.value)
            }
        }
        val equipId = setUpsModbusProfile(equipmentDev, selectedSlaveId, modbusParam, subEquipmentDevices)
        ModbusModelBuilder.saveToFile("$equipId.json",equipModel.value.jsonContent)
    }

    private fun setUpsModbusProfile(
        equipmentDev: EquipmentDevice, selectedSlaveId: Short, modbusParam: List<Parameter>,
        subEquipmentDevices: List<EquipmentDevice>
    ): String {
        val equipRef: String
        equipmentDev.slaveId = selectedSlaveId.toInt()
        if (getModbusEquipMap(selectedSlaveId)) {
            equipRef = updateModbusProfile(selectedSlaveId.toInt(), equipmentDev, modbusParam)
        }
         else {
            modbusProfile = ModbusProfile()
            modbusProfile.addMbEquip(selectedSlaveId, floorRef, zoneRef, equipmentDev, modbusParam, ProfileType.MODBUS_DEFAULT, subEquipmentDevices)
            L.ccu().zoneProfiles.add(modbusProfile)
            equipRef = modbusProfile.equip.id
            L.saveCCUState()
        }
        CcuLog.d(
            L.TAG_CCU_UI,
            "Set modbus Config: MB Profiles - " + L.ccu().zoneProfiles.size + "," + equipRef + "," + selectedSlaveId
        )
        return equipRef
    }

    private fun updateModbusProfile(
        slave_id: Int,
        equipmentDev: EquipmentDevice?,
        modbusParam: List<Parameter?>?
    ): String {
        modbusProfile.updateMbEquip(
            slave_id.toShort(),
            floorRef,
            zoneRef,
            equipmentDev,
            modbusParam
        )
        L.ccu().zoneProfiles.add(modbusProfile)
        return modbusProfile.equip.id
    }
    private fun getModbusEquipMap(slaveId: Short):Boolean {
        val mapDetails =  CCUHsApi.getInstance().readEntity("equip and modbus and not equipRef and group == \"$slaveId\"")
        return !mapDetails.isNullOrEmpty()
    }

    fun onSelectAll(isSelected: Boolean) {
        if (equipModel.value.parameters.isNotEmpty()) {
            equipModel.value.parameters.forEach {
                it.displayInUi.value = isSelected
            }
            if (equipModel.value.subEquips != null && equipModel.value.subEquips.isNotEmpty()) {
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
                if (!it.displayInUi.value) {
                    isAllSelected = false
                }
            }
        }
        if (!equipModel.value.subEquips.isEmpty() && equipModel.value.subEquips.isNotEmpty()) {
            equipModel.value.subEquips.forEach { subEquip ->
                subEquip.value.parameters.forEach {
                    if (!it.displayInUi.value) {
                        isAllSelected = false
                    }
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
            if (wantToDismiss) {
                _isDialogOpen.value = false
            }
            dialog.dismiss()
        }
        builder.create().show()

    }

}