package a75f.io.renatus.ui.nontempprofiles.utilities

import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface
import a75f.io.renatus.R
import a75f.io.renatus.ui.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import a75f.io.renatus.ui.nontempprofiles.views.showExternalPointsList
import a75f.io.renatus.ui.nontempprofiles.views.showHeaderView
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.compose.ui.platform.ComposeView
import org.apache.commons.lang3.math.NumberUtils
import java.util.concurrent.Executors


fun externalEquipsLayoutSetup(linearLayoutZonePoints: LinearLayout, zoneDetails: View) {
    val paramsLL = linearLayoutZonePoints.layoutParams as RelativeLayout.LayoutParams
    paramsLL.removeRule(RelativeLayout.BELOW)
    paramsLL.addRule(RelativeLayout.BELOW, R.id.pointBasedScheduleLayout)
    paramsLL.marginStart = 40
    linearLayoutZonePoints.layoutParams = paramsLL
    linearLayoutZonePoints.setPadding(0, 0, 0, 20)
    linearLayoutZonePoints.addView(zoneDetails)
}

private val backgroundExecutor = Executors.newSingleThreadExecutor()

fun showExternalEquipPointsUI(
    composeView: ComposeView,
    nonTempProfileViewModel: NonTempProfileViewModel,
    equipId: String,
    connectNodeName: String?,
    remotePointUpdateInterface: RemotePointUpdateInterface? = null,
    rs485Text: String? = null,
    address: Int? = null
) {
    composeView
        .showExternalPointsList(
            nonTempProfileViewModel,
            connectNodeName,
            rs485Text = rs485Text
        ) { selectedIndex: Int, point: Any ->
            backgroundExecutor.execute {
                val externalPointItem = point as ExternalPointItem
                val profileType = externalPointItem.profileType
                val isConnectNode = "connectModule".equals(profileType, ignoreCase = true)
                val isPcn = "pcn".equals(profileType, ignoreCase = true) || (isConnectNode && address != null && address < 100)
                if ("modbus".equals(profileType, ignoreCase = true) || isConnectNode || isPcn) {
                    handleModbusOrConnectModulePoint (
                        externalPointItem,
                        equipId,
                        selectedIndex,
                        isConnectNode,
                        isPcn
                    )
                } else {
                    handleBacnetPoint(externalPointItem, selectedIndex, remotePointUpdateInterface!!)
                }
            }
        }
}

private fun handleModbusOrConnectModulePoint(
    externalPointItem: ExternalPointItem,
    equipId: String,
    selectedIndex: Int,
    isConnectNode: Boolean,
    isPCN: Boolean
) {
    val parameter = externalPointItem.point as Parameter?
    val pointObject = readPoint(parameter!!, equipId, isConnectNode, isPCN)

    if (parameter.getCommands() != null && parameter.getCommands().isNotEmpty()) {
        val command = parameter.getCommands()[selectedIndex]
        writePoint(pointObject!!, command.bitValues, parameter, isConnectNode, isPCN)
    } else {
        val value = externalPointItem.dropdownOptions[selectedIndex]
        writePoint(pointObject!!, value, parameter, isConnectNode, isPCN)
    }
}

/**
 * Handles writing values for BACnet points.
 */

private fun handleBacnetPoint(
    externalPointItem: ExternalPointItem, selectedIndex: Int,
    remotePointUpdateInterface: RemotePointUpdateInterface
) {
    val bacnetPoint = externalPointItem.point as BacnetZoneViewItem?
    val selectedValue = bacnetPoint!!.spinnerValues[selectedIndex]

    val serverIp = externalPointItem.serverIpAddress

    if (NumberUtils.isCreatable(selectedValue.first)) {
        writeValueToBacnet(
            bacnetPoint, selectedValue.first,
            serverIp!!, remotePointUpdateInterface
        )
    } else {
        writeValueToBacnet(
            bacnetPoint, selectedValue.second.toString(),
            serverIp!!, remotePointUpdateInterface
        )
    }

    CcuLog.d(
        Tags.CCU_ZONE_SCREEN,
        "onItemSelected: " + selectedValue.first + " " + bacnetPoint.bacnetConfig
    )
}


fun showHeaderViewUI(
    composeView: ComposeView,
    nonTempProfileViewModel: NonTempProfileViewModel,
    equipId: String
) {
    composeView
        .showHeaderView(
            nonTempProfileViewModel
        ) { _: Int?, _: Any? ->
            backgroundExecutor.execute {}
        }
}