package a75f.io.renatus.ui.zonescreen.nontempprofiles.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class ExternalPointItem(
    var id: String? = null,
    var dis: String? = null,
    var currentValue: String? = null,
    var selectedIndex: Int = 0,
    var usesDropdown: Boolean = false,
    var dropdownOptions: List<String> = emptyList(),
    var dropdownValues: List<String> = emptyList(),
    var point: Any? = null,
    var canOverride: Boolean = false,
    var profileType: String? = null,
    var serverIpAddress: String? = null,
    val collapsedWidth: Dp = 90.dp,   // this is required because to show exact width in dropdown based on the currentValue text size
    val expandedWidth: Dp = 90.dp,
)
