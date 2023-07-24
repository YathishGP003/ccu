package a75f.io.renatus.hyperstatsplit

import android.view.View
import android.widget.Spinner
import android.widget.TextView
import android.widget.ToggleButton

/**
 * Created for HyperStat by Manjunath K on 18-07-2022.
 * Created for HyperStatSplit by Nick P on 07-24-2023.
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

class UniversalInWidgets(
    val switch: ToggleButton,
    val selector: Spinner
)

class SensorBusWidgets(
    val switch: ToggleButton,
    val selector: Spinner
)