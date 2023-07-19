package a75f.io.renatus.modbus

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.renatus.modbus.models.RegisterItem
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

                    val register = RegisterItem()
                    register.displayInUi.value = false
                    register.param = mutableStateOf(parameterTemp)
                    parameterList.add(register)
                }
            }
        }
    }
    return parameterList
}