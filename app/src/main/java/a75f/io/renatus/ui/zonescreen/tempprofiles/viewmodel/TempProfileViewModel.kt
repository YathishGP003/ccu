package a75f.io.renatus.ui.zonescreen.tempprofiles.viewmodel


import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.observer.HisWriteObservable
import a75f.io.api.haystack.observer.PointSubscriber
import a75f.io.api.haystack.observer.PointWriteObservable
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.ui.zonescreen.LAST_UPDATED
import a75f.io.renatus.ui.zonescreen.model.DetailedViewItem
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.tempprofiles.helper.PointValueChangeListener
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.HeartBeatUtil.isModuleAlive
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class TempProfileViewModel : ViewModel(), PointSubscriber {

    var equipScheduleStatus = mutableStateOf(HeaderViewItem())
    var schedule = mutableStateOf(HeaderViewItem())
    var specialSchedule = mutableStateOf(HeaderViewItem())
    var vacation = mutableStateOf(HeaderViewItem())
    var showSchedule = false

    var profile = ""
    var equipId = ""
    var equipName = ""
    var externalEquipHeartBeat = false
    var lastUpdated = mutableStateOf(HeaderViewItem())
    var equipStatusMessage = mutableStateOf(HeaderViewItem())
    var profileType: ProfileType? = null
    var detailedViewPoints = mutableStateMapOf<String, DetailedViewItem>()
    var pointValueChangeListener: PointValueChangeListener? = null
    var handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null
    var namedScheduleList = listOf<HashMap<Any, Any>>()

    init {
        namedScheduleList = CCUHsApi.getInstance().getAllNamedSchedules()
    }

    fun setEquipScheduleStatusPoint(item: HeaderViewItem) {
        equipScheduleStatus.value = item
        PointWriteObservable.subscribe(item.id.toString(), this)
    }

    fun setSchedule(item: HeaderViewItem) {
        schedule.value = item
        HisWriteObservable.subscribe("schedule", this)
    }

    fun setSpecialSchedule(item: HeaderViewItem) {
        specialSchedule.value = item
        HisWriteObservable.subscribe("schedule", this)
    }

    fun setVacationSchedule(item: HeaderViewItem) {
        vacation.value = item
        HisWriteObservable.subscribe("schedule", this)
    }

    fun setEquipStatusPoint(item: HeaderViewItem) {
        equipStatusMessage.value = item
        PointWriteObservable.subscribe(item.id.toString(), this)
    }

    fun setLastUpdated(item: HeaderViewItem) {
        lastUpdated.value = item
    }

    fun observeEquipHealth(
        groupId: String
    ) {
        runnable = object : Runnable {
            override fun run() {
                val lastUpdatedTime = HeartBeatUtil.getLastUpdatedTime(groupId)
                setLastUpdated(
                    HeaderViewItem(
                        id = groupId,
                        disName = LAST_UPDATED,
                        currentValue = lastUpdatedTime,
                        usesDropdown = false
                    )
                )
                externalEquipHeartBeat = isModuleAlive(groupId)
                handler.postDelayed(this, 60_000L)
            }
        }
        handler.post(runnable!!)
    }

    private fun stopObservingEquipHealth() {
        runnable?.let {
            handler.removeCallbacks(it)
        }
        runnable = null
    }

    fun cleanUp() {
        PointWriteObservable.clear()
        HisWriteObservable.clear()
        stopObservingEquipHealth()
    }

    fun initializeDetailedViewPoints(detailedViewItems: MutableMap<String, DetailedViewItem>) {
        viewModelScope.launch(Dispatchers.Main) {
            detailedViewPoints.clear()
            for (item in detailedViewItems) {
                val existingItem = detailedViewItems[item.key]
                detailedViewPoints[item.key] = existingItem!!
            }
            subscribeHisPoints(detailedViewItems.values.toMutableList())
        }
    }

    private fun subscribeHisPoints(hisPoints: List<DetailedViewItem>) {
        hisPoints.forEach { hisPoint ->
            hisPoint.id?.let { id ->
                HisWriteObservable.subscribe(id, this)
            }
        }
    }

    override fun onHisPointChanged(pointId: String, value: Double) {
        viewModelScope.launch(Dispatchers.Main) {
            pointValueChangeListener?.updateHisPoint(pointId, value.toString())
        }
    }

    override fun onWritablePointChanged(pointId: String, value: Any) {
        viewModelScope.launch(Dispatchers.Main) {
            pointValueChangeListener?.updateWritePoint(pointId, value.toString())
        }
    }

    override fun onCleared() {
        cleanUp()
    }
}