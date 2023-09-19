package a75f.io.renatus.modbus.models

import a75f.io.api.haystack.modbus.Parameter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Created by Manjunath K on 13-07-2023.
 */

class RegisterItem{
    var param = mutableStateOf(Parameter())
    val displayInUi = mutableStateOf(false)

}
