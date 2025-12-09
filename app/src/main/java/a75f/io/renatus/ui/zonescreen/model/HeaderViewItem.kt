package a75f.io.renatus.ui.zonescreen.model

data class HeaderViewItem(
    var id: String? = null,
    var disName: String? = null,
    var currentValue: String? = null,
    var selectedIndex: Int = 0,
    var usesDropdown: Boolean = false,
    var dropdownOptions: List<String> = emptyList()
)
