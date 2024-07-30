package a75f.io.renatus.bacnet.models

import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class BacnetModel {
    var equipDevice = mutableStateOf(BacnetModelDetailResponse())
    val selectAllParameters = mutableStateOf(false)
    var points = mutableListOf<BacnetPointState>()
    var jsonContent = String()
    var isDevicePaired = false
    var version: MutableState<String> = mutableStateOf("")
}

data class BacnetDevice(
    val deviceId: String,
    val deviceIp: String,
    val deviceNetwork: String,
    val deviceName: String,
    val devicePort: String,
    val deviceMacAddress: String?
)