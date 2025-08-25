package a75f.io.renatus.ui.nontempprofiles.model

import androidx.compose.runtime.Immutable

@Immutable
data class ExternalPointItem(
    var id: String? = null,
    var dis: String? = null,
    var currentValue: String? = null,
    var selectedIndex: Int = 0,
    var usesDropdown: Boolean = false,
    var dropdownOptions: List<String> = emptyList(),
    var point: Any? = null,
    var canOverride: Boolean = false,
    var profileType: String? = null,
    var serverIpAddress: String? = null,
)
