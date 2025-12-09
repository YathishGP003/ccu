package a75f.io.renatus.ui.zonescreen.tempprofiles.view

import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.views.showTemperatureProfileDetailedView
import a75f.io.renatus.ui.zonescreen.tempprofiles.viewmodel.TempProfileViewModel
import androidx.compose.ui.platform.ComposeView


fun showOTNDetailedView(
    composeView: ComposeView,
    tempProfileViewModel: TempProfileViewModel,
    onValueChange: (selectedIndex: Int, point: Any) -> Unit
) {
    composeView.showTemperatureProfileDetailedView(tempProfileViewModel) { selectedIndex, point ->

        if (point is HeaderViewItem) {
            onValueChange(selectedIndex, point)
            return@showTemperatureProfileDetailedView
        }
    }
}
