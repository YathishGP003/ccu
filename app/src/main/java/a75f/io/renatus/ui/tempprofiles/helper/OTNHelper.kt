package a75f.io.renatus.ui.tempprofiles.helper

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Schedule
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.OtnEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleManager.getScheduleStateString
import a75f.io.renatus.R
import a75f.io.renatus.ui.EQUIP_SCHEDULE_STATUS
import a75f.io.renatus.ui.HUMIDITY
import a75f.io.renatus.ui.SCHEDULE
import a75f.io.renatus.ui.SPECIAL_SCHEDULE
import a75f.io.renatus.ui.STATUS
import a75f.io.renatus.ui.TEMPORARY_HOLD
import a75f.io.renatus.ui.VACATIONS
import a75f.io.renatus.ui.model.DetailedViewItem
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.tempprofiles.getNamedSchedulePosition
import a75f.io.renatus.ui.tempprofiles.getScheduleList
import a75f.io.renatus.ui.tempprofiles.view.showOTNDetailedView
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class OTNHelper(
    private val equipMap: Equip,
    private val profileType: ProfileType
) : PointValueChangeListener {
    private val tempProfileViewModel = TempProfileViewModel()
    val equip = OtnEquip(equipMap.id)
    private val detailViewItems = mutableStateMapOf<String, DetailedViewItem>()

    companion object {
        fun create(
            equipMap: Equip,
            profileType: ProfileType
        ): OTNHelper {
            return OTNHelper(equipMap, profileType)
        }
    }

    fun profileName(): String  =  "OTN - Temperature Influencing (" + equipMap.group + ")"

    @SuppressLint("InflateParams")
    fun loadDetailedView(
        inflater: LayoutInflater,
        tempProfileViewModels: MutableList<TempProfileViewModel>,
        linearLayoutZonePoints: LinearLayout,
        showSchedule : Boolean = false,
        onValueChange: (selectedIndex: Int, point: Any) -> Unit
    ){
        val composeLayout: View = inflater.inflate(R.layout.temp_detailed_points, null)
        val composeView = composeLayout.findViewById<ComposeView>(R.id.tempDetailedComposeView)
        tempProfileViewModels.add(tempProfileViewModel)
        tempProfileViewModel.profileType = profileType
        tempProfileViewModel.equipName = profileName()
        tempProfileViewModel.setEquipStatusPoint(getEquipStatus())
        tempProfileViewModel.observeEquipHealth(equipMap.group)
        tempProfileViewModel.showSchedule = showSchedule
        setScheduleProperties()
        showOTNDetailedView(
            composeView,
            tempProfileViewModel
        ) { selectedIndex, point ->
            onValueChange(selectedIndex, point)
        }
        linearLayoutZonePoints.addView(composeLayout)
        loadDetailedViewDefaultValues()
    }

    private fun loadDetailedViewDefaultValues() {
        loadOTNDefaults()
    }


    private fun loadOTNDefaults() {
        detailViewItems.clear()
        val humidity = getHumidityView()
        detailViewItems[humidity.id.toString()] = humidity

        val forceOccupied = getForceOccupiedView()
        detailViewItems[forceOccupied.id.toString()] = forceOccupied

        tempProfileViewModel.initializeDetailedViewPoints(detailViewItems)
        tempProfileViewModel.pointValueChangeListener = this
    }

    override fun updateHisPoint(id: String, newValue: String) {
        if (id.equals("schedule", true)) {
            refreshSchedules()
            return
        }
        tempProfileViewModel.detailedViewPoints[id]?.let { oldItem ->
        val newItem = when {
            oldItem.disName?.contains(HUMIDITY) == true -> getHumidityView()
            oldItem.disName?.contains(TEMPORARY_HOLD) == true -> getForceOccupiedView()
            else -> oldItem
        }
        tempProfileViewModel.detailedViewPoints[id] = newItem
    }}

    override fun updateWritePoint(id: String, newValue: String) {
        if (id == tempProfileViewModel.equipStatusMessage.value.id) {
            val newEquipStatus = getEquipStatus()
            tempProfileViewModel.setEquipStatusPoint(newEquipStatus)
            return
        }

        if (id == tempProfileViewModel.equipScheduleStatus.value.id) {
            val newEquipScheduleStatus = getEquipScheduleStatus()
            tempProfileViewModel.setEquipScheduleStatusPoint(newEquipScheduleStatus)
            return
        }
    }

    private fun getEquipStatus(): HeaderViewItem {
        val status = equip.equipStatusMessage.readDefaultStrVal()
        return HeaderViewItem(
            id = equip.equipStatusMessage.id,
            disName = STATUS,
            currentValue = if (status == "") "OFF" else status,
            usesDropdown = false
        )
    }

    private fun getHumidityView(): DetailedViewItem {
        val zoneHumidity = CCUHsApi.getInstance()
            .readEntity("domainName == \"" + DomainName.zoneHumidity + "\" and " +
                    "equipRef == \"" + equipMap.id.toString() + "\"")

        val zoneHumidityValue = CCUHsApi.getInstance()
            .readHisValById(zoneHumidity["id"].toString())

        return DetailedViewItem(
            id = zoneHumidity["id"].toString(),
            disName = HUMIDITY,
            currentValue = "$zoneHumidityValue%",
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = null,
            configuration = null,
            displayOrder = 2,
            shouldTakeFullRow = false
        )
    }

    private fun getForceOccupiedView(): DetailedViewItem {
        val occupancyMode = CCUHsApi.getInstance()
            .readEntity(
                "domainName == \"" + DomainName.occupancyMode + "\" and " +
                        "equipRef == \"" + equipMap.id.toString() + "\""
            )

        val occupancyModeValue = CCUHsApi.getInstance()
            .readHisValById(occupancyMode["id"].toString())


        val occupancyModeUpdatedValue =
            if (occupancyModeValue == Occupancy.FORCEDOCCUPIED.ordinal.toDouble()
                || occupancyModeValue == Occupancy.AUTOFORCEOCCUPIED.ordinal.toDouble()
            ) {
                "Yes"
            } else {
                "No"
            }

        return DetailedViewItem(
            id = occupancyMode["id"].toString(),
            disName = TEMPORARY_HOLD,
            currentValue = occupancyModeUpdatedValue,
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = null,
            configuration = null,
            displayOrder = 3,
            shouldTakeFullRow = false
        )
    }

    private fun setScheduleProperties() {
        if(tempProfileViewModel.showSchedule) {
            tempProfileViewModel.setEquipScheduleStatusPoint(getEquipScheduleStatus())
            tempProfileViewModel.setSchedule(getSchedule())
            tempProfileViewModel.setSpecialSchedule(getSpecialSchedule())
            tempProfileViewModel.setVacationSchedule(getVacationSchedule())
        }
    }

    private fun getEquipScheduleStatus(): HeaderViewItem {
        val scheduleStatus = equip.equipScheduleStatus.readDefaultStrVal()
        return HeaderViewItem(
            id = equip.equipScheduleStatus.id,
            disName = EQUIP_SCHEDULE_STATUS,
            currentValue = scheduleStatus.ifEmpty { "Loading Schedules" },
            usesDropdown = false
        )
    }

    private fun getSchedule(): HeaderViewItem {
        val scheduleList = getScheduleList()
        var scheduleType = equip.scheduleType.readPriorityVal()

        scheduleType = when (scheduleType.toInt()) {
            2 -> {
                val namedPos = getNamedSchedulePosition(
                    namedSchedules = tempProfileViewModel.namedScheduleList,
                    equip.getId()
                ).toDouble()
                if (namedPos == 0.0) 2.0 else namedPos // fallback to first named schedule if not found
            }
            1 -> 0.0
            else -> scheduleType
        }

        val selectedSchedule = scheduleType.toInt()
        return HeaderViewItem(
            id = "schedule",
            disName = SCHEDULE,
            currentValue = selectedSchedule.toString(),
            usesDropdown = true,
            selectedIndex = selectedSchedule,
            dropdownOptions = scheduleList
        )
    }

    private fun getSpecialSchedule(): HeaderViewItem {
        val zoneId = Schedule.getZoneIdByEquipId(equip.getId())
        val specialScheduleStatus = getScheduleStateString(zoneId)

        return HeaderViewItem(
            id = "specialSchedule",
            disName = SPECIAL_SCHEDULE,
            currentValue = specialScheduleStatus,
            usesDropdown = true,
            selectedIndex = 0,
            dropdownOptions = listOf(specialScheduleStatus)
        )

    }

    private fun getVacationSchedule(): HeaderViewItem {
        val zoneId = Schedule.getZoneIdByEquipId(equip.getId())
        val specialScheduleStatus = ScheduleManager.getInstance().getVacationStateString(zoneId)

        return HeaderViewItem(
            id = "vacationSchedule",
            disName = VACATIONS,
            currentValue = specialScheduleStatus,
            usesDropdown = true,
            selectedIndex = 0,
            dropdownOptions = listOf(specialScheduleStatus)
        )
    }

    override fun refreshSchedules() {
        val schedule = getSchedule()
        tempProfileViewModel.setSchedule(schedule)

        val newSpecialSchedule = getSpecialSchedule()
        tempProfileViewModel.setSpecialSchedule(newSpecialSchedule)

        val newVacationSchedule = getVacationSchedule()
        tempProfileViewModel.setVacationSchedule(newVacationSchedule)

        val equipSchedule = getEquipScheduleStatus()
        tempProfileViewModel.setEquipScheduleStatusPoint(equipSchedule)


        CoroutineScope(Dispatchers.IO).launch {
            // this delay is because we are deriving vacation schedule from the schedule manager
            try {
                delay(60_000)
                val vS = getVacationSchedule()
                val sS = getSpecialSchedule()
                val eSS = getEquipScheduleStatus()

                withContext(Dispatchers.Main) {
                    CcuLog.d(L.TAG_CCU_ZONE, "Refreshing Vacation Schedule")
                    tempProfileViewModel.setVacationSchedule(vS)
                    tempProfileViewModel.setSpecialSchedule(sS)
                    tempProfileViewModel.setEquipScheduleStatusPoint(eSS)
                }
            } catch (e: Exception) {
                CcuLog.e(L.TAG_CCU_ZONE, "Failed to refresh schedules: ${e.message}", e)
            }
        }
    }

}