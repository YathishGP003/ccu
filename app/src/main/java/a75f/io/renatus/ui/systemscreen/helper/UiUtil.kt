package a75f.io.renatus.ui.systemscreen.helper

import a75f.io.renatus.ui.systemscreen.viewmodel.SystemViewModel
import androidx.compose.ui.platform.ComposeView

fun showExternalEquipPointsUI(
    composeView: ComposeView,
    systemScreenViewModel: SystemViewModel
) {
    composeView.showPoints(systemScreenViewModel)
}


fun ComposeView.showPoints(
    nonTempProfileViewModel: SystemViewModel
) {
    setContent {
    }
}