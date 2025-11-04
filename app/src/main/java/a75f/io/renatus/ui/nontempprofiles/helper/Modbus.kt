package a75f.io.renatus.ui.nontempprofiles.helper

import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.logger.CcuLog
import a75f.io.renatus.R
import a75f.io.renatus.ui.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.ui.nontempprofiles.utilities.externalEquipsLayoutSetup
import a75f.io.renatus.ui.nontempprofiles.utilities.getPointScheduleHeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.utilities.showExternalEquipPointsUI
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import android.view.View
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView


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
    nonTempProfileViewModels.add(viewModel)
    val points: List<ExternalPointItem>
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
    points =
        getModbusDetailedViewPoints(modbusDevice, "modbus", modbusDevice.deviceEquipRef)
    viewModel.initializeDetailedViewPoints(points)
    viewModel.observeExternalModbusEquipHealth(deviceId, equipId)
}