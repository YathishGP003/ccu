package a75f.io.renatus.compose

import a75f.io.api.haystack.modbus.Parameter

/**
 * Created by Manjunath K on 13-07-2023.
 */

data class RegisterItem(val param: Parameter, var displayInUi: Boolean)
