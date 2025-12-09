package a75f.io.renatus.ui.zonescreen.tempprofiles.helper

interface PointValueChangeListener {
    fun updateHisPoint(id: String, newValue: String)
    fun updateWritePoint(id: String, newValue: String)
    fun refreshSchedules()
}