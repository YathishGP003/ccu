package a75f.io.renatus.ui.model

import a75f.io.domain.api.Point

data class DetailedViewItem(
    var id: String? = null,
    var disName: String? = null,
    var currentValue: String? = null,
    var selectedIndex: Int = 0,
    var usesDropdown: Boolean = false,
    var dropdownOptions: List<String> = emptyList(),
    var point: Point? = null,
    var configuration: Any? = null,
    var displayOrder : Int = 0,
    var shouldTakeFullRow : Boolean = false
)
