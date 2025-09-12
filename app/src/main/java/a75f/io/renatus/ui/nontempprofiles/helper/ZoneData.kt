package a75f.io.renatus.ui.nontempprofiles.helper

import a75f.io.api.haystack.Equip
import android.view.View

data class ZoneData(
    val zoneDetails: View,
    val equip: Equip?,
    val isZoneExpanded: Boolean = false
)