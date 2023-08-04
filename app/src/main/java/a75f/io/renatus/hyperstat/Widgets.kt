package a75f.io.renatus.hyperstat

import android.view.View
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton

/**
 * Created by Manjunath K on 18-07-2022.
 */


class RelayWidgets(
    val switch: ToggleButton,
    val selector: Spinner,
)

class AnalogOutWidgets(
    val switch: ToggleButton,
    val selector: Spinner,
    val vAtMinDamperLabel: TextView,
    val vAtMinDamperSelector: Spinner,
    val vAtMaxDamperLabel: TextView,
    val vAtMaxDamperSelector: Spinner,
    val analogOutFanConfig: View,
    val analogOutAtFanLow: Spinner,
    val analogOutAtFanMedium: Spinner,
    val analogOutAtFanHigh: Spinner
)

class AnalogInWidgets(
    val switch: ToggleButton,
    val selector: Spinner
)

class StagedFanWidgets(
    val stagedFanLabel : TextView,
    val selector : Spinner
)