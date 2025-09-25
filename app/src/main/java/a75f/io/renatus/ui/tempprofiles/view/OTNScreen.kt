package a75f.io.renatus.ui.tempprofiles.view

import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.views.showTemperatureProfileDetailedView
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel
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
