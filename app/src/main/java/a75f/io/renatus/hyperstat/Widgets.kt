package a75f.io.renatus.hyperstat

import a75f.io.renatus.views.CustomCCUSwitch
import android.view.View
import android.widget.Spinner
import android.widget.TextView

/**
 * Created by Manjunath K on 18-07-2022.
 */


class RelayWidgets(
    val switch: CustomCCUSwitch,
    val selector: Spinner,
)

class AnalogOutWidgets(
    val switch: CustomCCUSwitch,
    val selector: Spinner,
    val vAtMinDamperLabel: TextView,
    val vAtMinDamperSelector: Spinner,
    val vAtMaxDamperLabel: TextView,
    val vAtMaxDamperSelector: Spinner,
    val analogOutFanConfig: View,
    val analogOutAtFanLow: Spinner,
    val analogOutAtFanMedium: Spinner,
    val analogOutAtFanHigh: Spinner,
    val analogOutAtFanRecirculateLabel: TextView,
    val analogOutAtFanRecirculateSelector: Spinner
)

class AnalogInWidgets(
    val switch: CustomCCUSwitch,
    val selector: Spinner
)

class StagedFanWidgets(
    val stagedFanLabel : TextView,
    val selector : Spinner
)