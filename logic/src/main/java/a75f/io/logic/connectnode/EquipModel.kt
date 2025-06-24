package a75f.io.logic.connectnode

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Created by Manjunath K on 19-07-2023.
 */

class EquipModel {
    var slaveId = mutableIntStateOf(0)
    val selectAllParameters = mutableStateOf(false)
    var equipDevice = mutableStateOf(EquipmentDevice())
    var parameters = mutableListOf<RegisterItem>()
    var jsonContent = String()
    var isDevicePaired = false
    var version: MutableState<String> = mutableStateOf("")
}

class RegisterItem{
    var param = mutableStateOf(Parameter())
    val displayInUi = mutableStateOf(false)

}