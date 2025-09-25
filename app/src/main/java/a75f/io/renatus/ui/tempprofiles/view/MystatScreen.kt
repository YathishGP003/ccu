package a75f.io.renatus.ui.tempprofiles.view

import a75f.io.domain.api.Point
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.renatus.profiles.mystat.ui.handleMyStatConditionMode
import a75f.io.renatus.profiles.mystat.ui.handleMyStatDeHumidityMode
import a75f.io.renatus.profiles.mystat.ui.handleMyStatFanMode
import a75f.io.renatus.profiles.mystat.ui.handleMyStatHumidityMode
import a75f.io.renatus.ui.CONDITIONING_MODE
import a75f.io.renatus.ui.DEHUMIDIFIER
import a75f.io.renatus.ui.FAN_MODE
import a75f.io.renatus.ui.HUMIDIFIER
import a75f.io.renatus.ui.model.DetailedViewItem
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.views.showTemperatureProfileDetailedView
import a75f.io.renatus.ui.tempprofiles.helper.MyStatHelper
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel
import androidx.compose.ui.platform.ComposeView


fun showMyStatDetailedView(
    composeView: ComposeView,
    tempProfileViewModel: TempProfileViewModel,
    equipId: String,
    myStatHelper: MyStatHelper,
    onValueChange: (selectedIndex: Int, point: Any) -> Unit
) {
    composeView.showTemperatureProfileDetailedView(tempProfileViewModel) { selectedIndex, point ->

        if (point is HeaderViewItem) {
            onValueChange(selectedIndex, point)
            return@showTemperatureProfileDetailedView
        }

        val detailedPoint = point as DetailedViewItem
        val configuration = detailedPoint.configuration as MyStatConfiguration

        if (detailedPoint.disName?.contains(CONDITIONING_MODE, ignoreCase = true) == true) {
            handleMyStatConditionMode(
                selectedIndex,
                equipId,
                tempProfileViewModel.profileType!!,
                true,
                configuration,
                detailedPoint.point as Point
            )
        }

        if (detailedPoint.disName?.contains(FAN_MODE, ignoreCase = true) == true) {
            handleMyStatFanMode(
                equipId,
                selectedIndex,
                tempProfileViewModel.profileType!!,
                true,
                configuration,
                detailedPoint.point as Point
            )
        }

        if (detailedPoint.disName?.contains(HUMIDIFIER, ignoreCase = true) == true) {
            handleMyStatHumidityMode(selectedIndex, equipId, myStatHelper.equip)
        }

        if (detailedPoint.disName?.contains(DEHUMIDIFIER, ignoreCase = true) == true) {
            handleMyStatDeHumidityMode(selectedIndex, equipId, myStatHelper.equip)
        }

    }
}
