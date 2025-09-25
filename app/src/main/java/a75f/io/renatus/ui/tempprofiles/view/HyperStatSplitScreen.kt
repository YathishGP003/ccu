package a75f.io.renatus.ui.tempprofiles.view

import a75f.io.renatus.hyperstatsplit.ui.handleConditionMode
import a75f.io.renatus.hyperstatsplit.ui.handleDeHumidityMode
import a75f.io.renatus.hyperstatsplit.ui.handleFanMode
import a75f.io.renatus.hyperstatsplit.ui.handleHumidityMode
import a75f.io.renatus.ui.CONDITIONING_MODE
import a75f.io.renatus.ui.DEHUMIDIFIER
import a75f.io.renatus.ui.FAN_MODE
import a75f.io.renatus.ui.HUMIDIFIER
import a75f.io.renatus.ui.model.DetailedViewItem
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.views.showTemperatureProfileDetailedView
import a75f.io.renatus.ui.tempprofiles.helper.HyperStatSplitHelper
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel
import androidx.compose.ui.platform.ComposeView


fun showHyperStatSplitDetailedView(
    composeView: ComposeView,
    tempProfileViewModel: TempProfileViewModel,
    equipId: String,
    hyperStatSplitHelper: HyperStatSplitHelper,
    onValueChange: (selectedIndex: Int, point: Any) -> Unit
) {
    composeView.showTemperatureProfileDetailedView(tempProfileViewModel) { selectedIndex, point ->

        if (point is HeaderViewItem) {
            onValueChange(selectedIndex, point)
            return@showTemperatureProfileDetailedView
        }

        val detailedPoint = point as DetailedViewItem

        if (detailedPoint.disName?.contains(CONDITIONING_MODE, ignoreCase = true) == true) {
            handleConditionMode(
                selectedIndex, hyperStatSplitHelper.equip, true
            )
        }

        if (detailedPoint.disName?.contains(FAN_MODE, ignoreCase = true) == true) {
            handleFanMode(
                hyperStatSplitHelper.equip, selectedIndex, true
            )
        }

        if (detailedPoint.disName?.contains(HUMIDIFIER, ignoreCase = true) == true) {
            handleHumidityMode(
                hyperStatSplitHelper.equip, selectedIndex
            )
        }

        if (detailedPoint.disName?.contains(DEHUMIDIFIER, ignoreCase = true) == true) {
            handleDeHumidityMode(
                hyperStatSplitHelper.equip, selectedIndex
            )
        }

    }
}
