package a75f.io.renatus.ui.zonescreen.tempprofiles.helper

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Schedule
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.DabEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.dab.DabProfile.CARRIER_PROD
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleManager.getScheduleStateString
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.R
import a75f.io.renatus.ui.zonescreen.AIRFLOW_CFM
import a75f.io.renatus.ui.zonescreen.DAMPER
import a75f.io.renatus.ui.zonescreen.DISCHARGE_AIRFLOW_TEMPERATURE
import a75f.io.renatus.ui.zonescreen.EQUIP_SCHEDULE_STATUS
import a75f.io.renatus.ui.zonescreen.REHEAT_COIL
import a75f.io.renatus.ui.zonescreen.SCHEDULE
import a75f.io.renatus.ui.zonescreen.SPECIAL_SCHEDULE
import a75f.io.renatus.ui.zonescreen.STATUS
import a75f.io.renatus.ui.zonescreen.VACATIONS
import a75f.io.renatus.ui.zonescreen.model.DetailedViewItem
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.tempprofiles.getNamedSchedulePosition
import a75f.io.renatus.ui.zonescreen.tempprofiles.getScheduleList
import a75f.io.renatus.ui.zonescreen.tempprofiles.view.showDabDetailedView
import a75f.io.renatus.ui.zonescreen.tempprofiles.viewmodel.TempProfileViewModel
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
import java.util.UUID


class DabHelper(
    private val equipMap: Equip,
    private val profileType: ProfileType
) : PointValueChangeListener {
    private val tempProfileViewModel = TempProfileViewModel()
    val equip: DabEquip = Domain.getDomainEquip(equipMap.id) as DabEquip
    private val detailViewItems = mutableStateMapOf<String, DetailedViewItem>()

    companion object {
        fun create(
            equipMap: Equip,
            profileType: ProfileType
        ): DabHelper {
            return DabHelper(equipMap, profileType)
        }
    }

    fun profileName(): String  = if (BuildConfig.BUILD_TYPE == CARRIER_PROD) {
        "VVT-C" + " (" + equipMap.group + ")"
    } else {
        "DAB (" + equipMap.group + " )"
    }

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
        showDabDetailedView(
            composeView,
            tempProfileViewModel
        ) { selectedIndex, point ->
            onValueChange(selectedIndex, point)
        }
        linearLayoutZonePoints.addView(composeLayout)
        loadDetailedViewDefaultValues()
    }

    private fun loadDetailedViewDefaultValues() {
        loadDABDefaults()
    }


    private fun loadDABDefaults() {
        detailViewItems.clear()

        val damperPos = getDamperPos()
        detailViewItems[damperPos.id.toString()] = damperPos

        val getDischargeAirTemp = getDischargeAirTempView()
        detailViewItems[getDischargeAirTemp.id.toString()] = getDischargeAirTemp

        if (CCUHsApi.getInstance().readDefaultVal(
                "point and domainName == \""
                        + DomainName.reheatType + "\" and group == \""
                        + equipMap.group.toString() + "\""
            ) > 0
        ) {
            val reheatView = getReheatView()
            detailViewItems[reheatView.id.toString()] = reheatView
        }

        if(TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), equip.getId())){
            val airFlowCFMView = getAirFlowCFMView()
            detailViewItems[airFlowCFMView.id.toString()] = airFlowCFMView
        }

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
                oldItem.disName?.contains(DAMPER) == true -> getDamperPos()
                oldItem.disName?.contains(DISCHARGE_AIRFLOW_TEMPERATURE) == true -> getDischargeAirTempView()
                oldItem.disName?.contains(REHEAT_COIL) == true -> getReheatView()
                oldItem.disName?.contains(AIRFLOW_CFM) == true -> getAirFlowCFMView()
                else -> oldItem
            }
            tempProfileViewModel.detailedViewPoints[id] = newItem
        }
    }

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

    private fun getDamperPos(): DetailedViewItem {
        val damperPosPoint = equip.normalizedDamper1Cmd
        val damperPosVal = damperPosPoint.readHisVal()
        val damperPosUpdatedValue = if (damperPosVal > 0) {
            "$damperPosVal% Open"
        } else {
            "0% Open"
        }
        return DetailedViewItem(
            id = damperPosPoint.id,
            disName = DAMPER,
            currentValue = damperPosUpdatedValue,
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = null,
            configuration = null,
            displayOrder = 1,
            shouldTakeFullRow = true
        )
    }

    private fun getDischargeAirTempView(): DetailedViewItem {
        var dischargePoint = equip.dischargeAirTemp1.readHisVal().toString()
        dischargePoint = if (dischargePoint == "0.0") "0" else dischargePoint
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted =
                UnitUtils.fahrenheitToCelsiusTwoDecimal(dischargePoint.toDouble())
            dischargePoint = "%.2f °C".format(converted)
        } else {
            dischargePoint = "$dischargePoint ℉"
        }

        return DetailedViewItem(
            id = equip.dischargeAirTemp1.id,
            disName = DISCHARGE_AIRFLOW_TEMPERATURE,
            currentValue = dischargePoint,
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = equip.dischargeAirTemp1,
            configuration = null,
            displayOrder = 3,
            shouldTakeFullRow = true
        )
    }


    private fun getReheatView(): DetailedViewItem {
        val reheatPoint = equip.reheatCmd
        val reheatPointVal = reheatPoint.readHisVal()
        val reheatPointUpdatedVal = if (reheatPointVal > 0) {
            "$reheatPointVal% Open"
        } else {
            "0% Open"
        }

        return DetailedViewItem(
            id = reheatPoint.id,
            disName = REHEAT_COIL,
            currentValue = reheatPointUpdatedVal,
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = equip.dischargeAirTemp1,
            configuration = null,
            displayOrder = 3,
            shouldTakeFullRow = true
        )
    }

    private fun getAirFlowCFMView(): DetailedViewItem {
        val airflowCFMPoint = CCUHsApi.getInstance()
            .readEntity(
                "point and domainName == \"" + DomainName.airFlowSensor + "\" " +
                        "and equipRef == \"" + equip.getId() + "\""
            )
        if (airflowCFMPoint.isNotEmpty()) {
            val airflowCFMValue = CCUHsApi.getInstance()
                .readHisValById(airflowCFMPoint["id"].toString())
            return DetailedViewItem(
                id = airflowCFMPoint["id"].toString(),
                disName = AIRFLOW_CFM,
                currentValue = "%.2f".format(airflowCFMValue.toFloat()),
                selectedIndex = 0,
                dropdownOptions = emptyList(),
                usesDropdown = false,
                point = null,
                configuration = null,
                displayOrder = 5,
                shouldTakeFullRow = true
            )
        } else {
            return DetailedViewItem(
                id = UUID.randomUUID().toString(),
                disName = AIRFLOW_CFM,
                currentValue = "N/A",
                selectedIndex = 0,
                dropdownOptions = emptyList(),
                usesDropdown = false,
                point = null,
                configuration = null,
                displayOrder = 5,
                shouldTakeFullRow = true
            )
        }
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