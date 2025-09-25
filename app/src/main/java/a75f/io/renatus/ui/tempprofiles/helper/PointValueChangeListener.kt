package a75f.io.renatus.ui.tempprofiles.helper

interface PointValueChangeListener {
    fun updateHisPoint(id: String, newValue: String)
    fun updateWritePoint(id: String, newValue: String)
    fun refreshSchedules()
}