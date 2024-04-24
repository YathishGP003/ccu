package a75f.io.renatus.hyperstatsplit

import a75f.io.renatus.views.CustomCCUSwitch
import android.view.View
import android.widget.Spinner
import android.widget.TextView

/**
 * Created for HyperStat by Manjunath K on 18-07-2022.
 * Created for HyperStatSplit by Nick P on 07-24-2023.
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
    val analogOutAtFanRecirculateSelector: Spinner,
    val analogOutDuringEconomizerLabel: TextView,
    val analogOutDuringEconomizerSelector: Spinner
)

class UniversalInWidgets(
    val switch: CustomCCUSwitch,
    val selector: Spinner
)

class SensorBusWidgets(
    val switch: CustomCCUSwitch,
    val selector: Spinner
)

class StagedFanWidgets(
    val stagedFanLabel : TextView,
    val selector : Spinner
)