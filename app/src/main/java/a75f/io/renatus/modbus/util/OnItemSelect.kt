package a75f.io.renatus.modbus.util

import a75f.io.renatus.bacnet.models.BacnetDevice

/**
 * Created by Manjunath K on 03-08-2023.
 */

interface OnItemSelect {
    fun onItemSelected(index: Int, item: String)
}

interface OnItemSelectBacnetDevice {
    fun onItemSelected(index: Int, item: BacnetDevice)
}