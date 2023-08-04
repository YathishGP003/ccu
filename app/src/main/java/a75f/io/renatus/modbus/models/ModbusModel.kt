package a75f.io.renatus.modbus.models

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Created by Manjunath K on 19-07-2023.
 */

class EquipModel {
    var slaveId = mutableStateOf(0)
    var childSlaveId = mutableStateOf(SAME_AS_PARENT)
    val selectAllParameters = mutableStateOf(false)
    var equipDevice = mutableStateOf(EquipmentDevice())
    var parameters = mutableListOf<RegisterItem>()
    var subEquips = mutableListOf<MutableState<EquipModel>>()
    var jsonContent = String()
    var isDevicePaired = false
}