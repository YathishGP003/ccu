package a75f.io.renatus.ui.nontempprofiles.helper

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.renatus.R
import a75f.io.renatus.ui.nontempprofiles.utilities.externalEquipsLayoutSetup
import a75f.io.renatus.ui.nontempprofiles.utilities.getPointScheduleHeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.utilities.showExternalEquipPointsUI
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import android.view.View
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView


fun loadConnectModuleZone(
    nonTempProfileViewModels: MutableList<NonTempProfileViewModel>,
    equipId: String,
    equipmentDeviceName: String,
    showLastUpdatedTime: Boolean,
    externalEquipDevice: Any,
    zoneDetailsView: View,
    linearLayoutZonePoints: LinearLayout,
    connectNodeDevice: HashMap<Any, Any>
) {
    val composeView = zoneDetailsView.findViewById<ComposeView>(R.id.detailedComposeView)
    externalEquipsLayoutSetup(linearLayoutZonePoints, zoneDetailsView)

    val viewModel = NonTempProfileViewModel()
    nonTempProfileViewModels.add(viewModel)
    var connectNodeHeaderName: String? = null
    if (showLastUpdatedTime) {
        val connectNodeAddr = connectNodeDevice["addr"].toString()
        connectNodeHeaderName = "Connect Node ($connectNodeAddr)"
    }
    showExternalEquipPointsUI(composeView, viewModel, equipId, connectNodeHeaderName)

    val cnDevice = externalEquipDevice as EquipmentDevice
    val deviceId = cnDevice.slaveId.toString()
    // lastUpdate and heartbeat
    if (showLastUpdatedTime) {
        viewModel.initializeEquipHealth(equipmentDeviceName, true, deviceId)
    } else {
        viewModel.equipName = equipmentDeviceName
    }
    viewModel.setEquipStatusPoint(getPointScheduleHeaderViewItem(equipId))

    viewModel.initializeDetailedViewPoints(
        getModbusDetailedViewPoints(
            cnDevice, "connectModule",
            cnDevice.deviceEquipRef
        )
    )
    viewModel.observeConnectEquipHealthByGroupId(connectNodeDevice["id"].toString())
}