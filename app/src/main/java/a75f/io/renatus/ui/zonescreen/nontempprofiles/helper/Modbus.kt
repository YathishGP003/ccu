package a75f.io.renatus.ui.zonescreen.nontempprofiles.helper

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.renatus.R
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.externalEquipsLayoutSetup
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.getPointScheduleHeaderViewItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.showExternalEquipPointsUI
import a75f.io.renatus.ui.zonescreen.nontempprofiles.viewmodel.NonTempProfileViewModel
import android.view.View
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun loadModbusZone(
    nonTempProfileViewModels: MutableList<NonTempProfileViewModel>,
    equipId: String,
    equipmentDeviceName: String,
    showLastUpdatedTime: Boolean,
    externalEquipDevice: Any,
    zoneDetailsView: View,
    linearLayoutZonePoints: LinearLayout,
) {
    val composeView = zoneDetailsView.findViewById<ComposeView>(R.id.detailedComposeView)
    externalEquipsLayoutSetup(linearLayoutZonePoints, zoneDetailsView)

    val viewModel = NonTempProfileViewModel()
    viewModel.profile = "modbus"
    nonTempProfileViewModels.add(viewModel)
    showExternalEquipPointsUI(composeView, viewModel, equipId, null)
    val modbusDevice = externalEquipDevice as EquipmentDevice
    val deviceId = modbusDevice.slaveId.toString()
    viewModel.initializeModbusEquipHealth(
        equipmentDeviceName,
        showLastUpdatedTime,
        deviceId,
        equipId
    ) // lastUpdate and heartbeat
    viewModel.setEquipStatusPoint(getPointScheduleHeaderViewItem(equipId)) // status message

    viewModel.backgroundJob = CoroutineScope(Dispatchers.Default).launch {
        val points = withContext(Dispatchers.IO) {
            preparePoints(
                getModbusDetailedViewPoints(
                    modbusDevice,
                    "modbus",
                    modbusDevice.deviceEquipRef
                )
            )
        }
        viewModel.initializeDetailedViewPoints(points)
    }
    viewModel.observeExternalModbusEquipHealth(deviceId, equipId)
}