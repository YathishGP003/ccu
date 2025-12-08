package a75f.io.renatus.ui.tempprofiles.view

import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.renatus.profiles.hyperstat.util.handleConditionMode
import a75f.io.renatus.profiles.hyperstat.util.handleDeHumidityMode
import a75f.io.renatus.profiles.hyperstat.util.handleFanMode
import a75f.io.renatus.profiles.hyperstat.util.handleHumidityMode
import a75f.io.renatus.ui.CONDITIONING_MODE
import a75f.io.renatus.ui.DEHUMIDIFIER
import a75f.io.renatus.ui.FAN_MODE
import a75f.io.renatus.ui.HUMIDIFIER
import a75f.io.renatus.ui.model.DetailedViewItem
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.views.showTemperatureProfileDetailedView
import a75f.io.renatus.ui.tempprofiles.helper.HyperStatHelper
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel
import androidx.compose.ui.platform.ComposeView


fun showHyperStatDetailedView(
    composeView: ComposeView,
    tempProfileViewModel: TempProfileViewModel,
    equipId: String,
    hyperStatHelper: HyperStatHelper,
    onValueChange: (selectedIndex: Int, point: Any) -> Unit
) {
    composeView.showTemperatureProfileDetailedView(tempProfileViewModel) { selectedIndex, point ->

        if (point is HeaderViewItem) {
            onValueChange(selectedIndex, point)
            return@showTemperatureProfileDetailedView
        }


        val detailedPoint = point as DetailedViewItem
        val configuration = detailedPoint.configuration as HyperStatConfiguration

        if (detailedPoint.disName?.contains(CONDITIONING_MODE, ignoreCase = true) == true) {
            handleConditionMode(
                selectedIndex,
                equipId,
                tempProfileViewModel.profileType!!,
                true,
                hyperStatHelper.equip,
                configuration
            )
        }

        if (detailedPoint.disName?.contains(FAN_MODE, ignoreCase = true) == true) {
            handleFanMode(
                equipId,
                selectedIndex,
                tempProfileViewModel.profileType!!,
                true,
                hyperStatHelper.equip,
                configuration
            )
        }

        if (detailedPoint.disName?.contains(HUMIDIFIER, ignoreCase = true) == true) {
            handleHumidityMode(selectedIndex, hyperStatHelper.equip)
        }

        if (detailedPoint.disName?.contains(DEHUMIDIFIER, ignoreCase = true) == true) {
            handleDeHumidityMode(selectedIndex, hyperStatHelper.equip)
        }

    }
}
