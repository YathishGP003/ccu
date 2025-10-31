package a75f.io.renatus.profiles.pcn

import a75f.io.domain.service.ResponseCallback
import a75f.io.logic.bo.building.pcn.ExternalEquip
import a75f.io.logic.connectnode.EquipModel
import a75f.io.renatus.compose.getModelListFromJson
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS_DEVICE_LIST_NOT_FOUND
import a75f.io.renatus.modbus.util.NO_INTERNET
import a75f.io.renatus.modbus.util.NO_MODEL_DATA_FOUND
import a75f.io.renatus.modbus.util.getParametersForCN
import a75f.io.renatus.modbus.util.parseModbusDataFromString
import a75f.io.renatus.modbus.util.showErrorDialog
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.JsonParseException
import io.seventyfivef.ph.core.Tags

class PCNRepository {
    companion object {
        fun fetchModelDetails(selectedDevice: String, viewModel: PCNConfigViewModel, context : Context) {
            val modelId = viewModel.getModelIdByName(selectedDevice)
            val version = viewModel.getVersionByID(modelId)
            viewModel.domainService.readModelById(modelId, version, object : ResponseCallback {
                override fun onSuccessResponse(response: String?) {
                    if (!response.isNullOrEmpty()) {
                        try {
                            val modbusResponse = parseModbusDataFromString(response)
                            if (modbusResponse == null) {
                                showErrorDialog(context, NO_MODEL_DATA_FOUND)
                                return
                            } else {
                                viewModel.equipmentDevice = modbusResponse
                            }
                            viewModel.equipmentDevice.registers!!.forEach { equipRegister ->
                                equipRegister.parameters.forEach { param ->
                                    param.userIntentPointTags?.find { it.tagName == Tags.SCHEDULABLE }?.let {
                                        param.isSchedulable = true
                                    }
                                }
                            }
                            viewModel.equipmentDevice.equips?.forEach { subEquipmentDevice ->
                                subEquipmentDevice?.registers!!.forEach { equipRegister ->
                                    equipRegister.parameters.forEach { param ->
                                        param.userIntentPointTags?.find { it.tagName == Tags.SCHEDULABLE }?.let {
                                            param.isSchedulable = true
                                        }
                                    }
                                }
                            }

                            val model = EquipModel()
                            model.jsonContent = response
                            model.equipDevice.value = viewModel.equipmentDevice
                            model.equipDevice.value.slaveId =  viewModel.deviceAddress.toString().takeLast(2).toInt()
                            model.parameters = getParametersForCN(model.equipDevice.value)
                            model.version.value = version
                            val subDeviceList = mutableListOf<MutableState<EquipModel>>()
                            viewModel.equipModel.value = model
                            if (viewModel.equipmentDevice.equips != null && viewModel.equipmentDevice.equips.isNotEmpty()) {
                                viewModel.equipmentDevice.equips.forEach {
                                    val subEquip = EquipModel()
                                    subEquip.equipDevice.value = it
                                    subEquip.parameters = getParametersForCN(it)
                                    subDeviceList.add(mutableStateOf(subEquip))
                                }
                            }
                            if (subDeviceList.isNotEmpty()) {
                                viewModel.equipModel.value.subEquips = subDeviceList
                            } else {
                                viewModel.equipModel.value.subEquips = mutableListOf()
                            }
                            viewModel.viewState.value.externalEquipList.add(
                                ExternalEquip(viewModel.getNextServerId(),
                                viewModel.equipModel.value.equipDevice.value.name, viewModel.equipModel.value,
                                newConfiguration = true, modelUpdated = false)
                            )
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

        fun readModbusModelsList (
            context: Context,
            viewModel: PCNConfigViewModel,
            onResult: (Boolean) -> Unit
        ) {
            ProgressDialogUtils.showProgressDialog(context, LOADING)
            viewModel.domainService.readModbusModelsList("", object : ResponseCallback {
                override fun onSuccessResponse(response: String?) {
                    try {
                        if (!response.isNullOrEmpty()) {
                            val itemList = mutableListOf<Pair<String, Int>>()
                            viewModel.deviceModelList = getModelListFromJson(response)
                            viewModel.deviceModelList.forEach {
                                itemList.add(Pair(it.name, it.registerCount))
                            }
                            viewModel.deviceList.value = itemList

                            onResult (true)
                        } else {
                            showErrorDialog(context, MODBUS_DEVICE_LIST_NOT_FOUND)
                        }
                    } catch (e: Exception) {
                        showErrorDialog(context, NO_INTERNET)
                        onResult(false)
                    }
                    ProgressDialogUtils.hideProgressDialog()
                }

                override fun onErrorResponse(response: String?) {
                    showErrorDialog(context, NO_INTERNET)
                    ProgressDialogUtils.hideProgressDialog()
                    onResult (false)
                }
            })
        }

    }
}