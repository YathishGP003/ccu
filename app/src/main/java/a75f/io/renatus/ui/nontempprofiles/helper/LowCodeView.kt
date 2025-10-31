package a75f.io.renatus.ui.nontempprofiles.helper

import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.renatus.R
import a75f.io.renatus.ui.nontempprofiles.utilities.externalEquipsLayoutSetup
import a75f.io.renatus.ui.nontempprofiles.utilities.getPointScheduleHeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.utilities.showExternalEquipPointsUI
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import android.view.View
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView


fun loadLowCodeModule(
    nonTempProfileViewModels: MutableList<NonTempProfileViewModel>,
    equipId: String,
    equipmentDeviceName: String,
    showLastUpdatedTime: Boolean,
    externalEquipDevice: Any,
    zoneDetailsView: View,
    linearLayoutZonePoints: LinearLayout,
    lowCodeDevice: HashMap<Any, Any>,
    equipType : String,
    rs485Text : String? = null
) {
    val composeView = zoneDetailsView.findViewById<ComposeView>(R.id.detailedComposeView)
    externalEquipsLayoutSetup(linearLayoutZonePoints, zoneDetailsView)

    val viewModel = NonTempProfileViewModel()
    var headerName: String? = null
    val address = lowCodeDevice["addr"].toString()

    if (showLastUpdatedTime) {
        headerName = when (equipType) {
            Tags.PCN -> {
                "Smart Node-Custom Code ($address)"
            }
            Tags.CONNECTMODULE -> {
                "Connect Module ($address)"
            }
            Tags.MODBUS -> {
                "External Equip ($address)"
            }
            else -> {
                ""
            }
        }
    } else {
        viewModel.lastUpdated.value.id = null
    }
    nonTempProfileViewModels.add(viewModel)
    if (headerName?.contains("External Equip") == true) {
        viewModel.equipName = equipmentDeviceName
    }
    showExternalEquipPointsUI(composeView, viewModel, equipId, headerName, null, rs485Text, address.toInt())

    val equipmentDevice = externalEquipDevice as EquipmentDevice
    val deviceId = equipmentDevice.slaveId.toString()
    // lastUpdate and heartbeat
    if (showLastUpdatedTime) {
        viewModel.initializeEquipHealth(equipmentDeviceName, true, deviceId)
    } else {
        viewModel.equipName = equipmentDeviceName
    }
    viewModel.setEquipStatusPoint(getPointScheduleHeaderViewItem(equipId))

    viewModel.initializeDetailedViewPoints(
        getModbusDetailedViewPoints(
            equipmentDevice, equipType,
            equipmentDevice.deviceEquipRef
        )
    )
    viewModel.observeConnectEquipHealthByGroupId(lowCodeDevice["id"].toString())
}