package a75f.io.renatus.modbus.models

import a75f.io.api.haystack.modbus.EquipmentDevice
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Created by Manjunath K on 19-07-2023.
 */

class EquipModel {
    var slaveId = mutableStateOf(0)
    val selectAllParameters = mutableStateOf(false)
    var equipDevice = mutableStateOf(EquipmentDevice())
    var parameters = mutableListOf<RegisterItem>()
    var subEquips = mutableListOf<MutableState<EquipModel>>()
}