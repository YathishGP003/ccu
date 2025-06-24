package a75f.io.renatus.modbus.models

import a75f.io.api.haystack.modbus.Parameter
import androidx.compose.runtime.mutableStateOf

/**
 * Created by Manjunath K on 13-07-2023.
 */

class RegisterItem{
    var param = mutableStateOf(Parameter())
    val displayInUi = mutableStateOf(false)
    val schedulable = mutableStateOf(false)

}
class RegisterItemForSubEquip{
    val displayInUi = mutableStateOf(false)
}
