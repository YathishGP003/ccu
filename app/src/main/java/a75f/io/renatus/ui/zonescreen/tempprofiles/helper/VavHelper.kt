package a75f.io.renatus.ui.zonescreen.tempprofiles.helper

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Schedule
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.VavEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleManager.getScheduleStateString
import a75f.io.logic.bo.building.truecfm.TrueCFMUtil
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.R
import a75f.io.renatus.ui.zonescreen.AIRFLOW_CFM
import a75f.io.renatus.ui.zonescreen.CHW_VALVE
import a75f.io.renatus.ui.zonescreen.DAMPER
import a75f.io.renatus.ui.zonescreen.DISCHARGE_AIRFLOW_TEMPERATURE
import a75f.io.renatus.ui.zonescreen.ENTERING_AIRFLOW_TEMPERATURE
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
import a75f.io.renatus.ui.zonescreen.tempprofiles.view.showVavDetailedView
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


class VavHelper(
    private val equipMap: Equip,
    private val profileType: ProfileType
) : PointValueChangeListener {
    private val tempProfileViewModel = TempProfileViewModel()
    val equip: VavEquip = Domain.getDomainEquip(equipMap.id) as VavEquip
    private val detailViewItems = mutableStateMapOf<String, DetailedViewItem>()

    companion object {
        fun create(
            equipMap: Equip,
            profileType: ProfileType
        ): VavHelper {
            return VavHelper(equipMap, profileType)
        }
    }

    fun profileName(): String {
        return when (profileType) {
            ProfileType.VAV_REHEAT -> {
                "VAV Reheat - No Fan ( " + equipMap.group + " )"
            }

            ProfileType.VAV_SERIES_FAN -> {
                "VAV Series Fan ( " + equipMap.group + " )"
            }

            ProfileType.VAV_PARALLEL_FAN -> {
                "VAV Parallel Fan ( " + equipMap.group + " )"
            }

            ProfileType.VAV_ACB -> {
                "Active Chilled Beams + DOAS ( " + equipMap.group + " )"
            }

            else -> {
                "No Profile Type Found"
            }
        }
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
        showVavDetailedView(
            composeView,
            tempProfileViewModel
        ) { selectedIndex, point ->
            onValueChange(selectedIndex, point)
        }
        linearLayoutZonePoints.addView(composeLayout)
        loadDetailedViewDefaultValues()
    }

    private fun loadDetailedViewDefaultValues() {
        loadVavDefaults()
    }


    private fun loadVavDefaults() {
        detailViewItems.clear()

        val damperPos = getDamperPos()
        detailViewItems[damperPos.id.toString()] = damperPos

        if (CCUHsApi.getInstance().readDefaultVal(
                "domainName == \"" + DomainName.valveType + "\" " +
                        "and group == \"" + equipMap.group.toLong() + "\""
            ) > 0.0 && profileType.name == ProfileType.VAV_ACB.name
        ) {
            val chwValve = getValveView()
            detailViewItems[chwValve.id.toString()] = chwValve

        } else if (CCUHsApi.getInstance()
                .readDefaultVal(
                    "point and domainName == \"" + DomainName.reheatType + "\"" +
                            " and group == \"" + equipMap.group.toLong() + "\""
                ) > 0.0
        ) {
            val reheatPos = getReheatView()
            detailViewItems[reheatPos.id.toString()] = reheatPos
        }

        val dischargeAirflowTemperature = getDischargeAirflowTemperatureView()
        detailViewItems[dischargeAirflowTemperature.id.toString()] = dischargeAirflowTemperature

        if (profileType.name != ProfileType.VAV_ACB.name) {
            val enteringAirflowTemperature = getEnteringAirflowTemperature()
            detailViewItems[enteringAirflowTemperature.id.toString()] = enteringAirflowTemperature
        }

        if (TrueCFMUtil.isTrueCfmEnabled(CCUHsApi.getInstance(), equip.getId())) {
            val airFlowCfmView = getAirFlowCFMView()
            detailViewItems[airFlowCfmView.id.toString()] = airFlowCfmView
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
                oldItem.disName?.contains(REHEAT_COIL) == true -> getReheatView()
                oldItem.disName?.contains(CHW_VALVE) == true -> getValveView()
                oldItem.disName?.contains(DISCHARGE_AIRFLOW_TEMPERATURE) == true -> getDischargeAirflowTemperatureView()
                oldItem.disName?.contains(ENTERING_AIRFLOW_TEMPERATURE) == true -> getEnteringAirflowTemperature()
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
        if (TrueCFMUtil.isCfmOnEdgeActive(CCUHsApi.getInstance(), equip.getId())) {
            val damperPosPoint = equip.damperCmdCal
            val damperPosPointValue = damperPosPoint.readHisVal()
            val damperValue = if(damperPosPointValue == 0.0) 0 else damperPosPointValue
            return DetailedViewItem(
                id = damperPosPoint.id,
                disName = DAMPER,
                currentValue = "$damperValue% Open",
                selectedIndex = 0,
                dropdownOptions = emptyList(),
                usesDropdown = false,
                point = damperPosPoint,
                configuration = null,
                displayOrder = 1,
                shouldTakeFullRow = true
            )

        } else {
            val damperPosPoint = equip.normalizedDamperCmd
            val damperPosPointValue = damperPosPoint.readHisVal()
            val damperValue = if(damperPosPointValue == 0.0) 0 else damperPosPointValue
            return DetailedViewItem(
                id = damperPosPoint.id,
                disName = DAMPER,
                currentValue = "$damperValue% Open",
                selectedIndex = 0,
                dropdownOptions = emptyList(),
                usesDropdown = false,
                point = damperPosPoint,
                configuration = null,
                displayOrder = 1,
                shouldTakeFullRow = true
            )
        }
    }


    private fun getReheatView(): DetailedViewItem {
        val reheatPoint =
            if (TrueCFMUtil.isCfmOnEdgeActive(CCUHsApi.getInstance(), equip.getId())) {
                equip.reheatCmdCal
            } else {
                equip.reheatCmd
            }

        var reheatPointValue = reheatPoint.readHisVal()

        if (reheatPointValue > 0) {
            reheatPointValue = equip.reheatCmd.readHisVal()
        }
        val reheatValue = if(reheatPointValue == 0.0) 0 else reheatPointValue
        return DetailedViewItem(
            id = reheatPoint.id,
            disName = REHEAT_COIL,
            currentValue = reheatValue.toString(),
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = reheatPoint,
            configuration = null,
            displayOrder = 2,
            shouldTakeFullRow = true
        )
    }

    private fun getValveView(): DetailedViewItem {
        val valvePoint = CCUHsApi.getInstance()
            .readEntity(
                "point and domainName == \"" +
                        DomainName.chilledWaterValve + "\" and equipRef == \"" + equipMap.id + "\""
            )

        if (valvePoint.isEmpty() || valvePoint == null) {
            return DetailedViewItem(
                id = UUID.randomUUID().toString(),
                disName = CHW_VALVE,
                currentValue = "0",
                selectedIndex = 0,
                dropdownOptions = emptyList(),
                usesDropdown = false,
                point = null,
                configuration = null,
                displayOrder = 2,
                shouldTakeFullRow = true
            )
        }

        val valvePointVal = CCUHsApi.getInstance().readHisValById(valvePoint["id"].toString())
        val valveValue = if(valvePointVal == 0.0) 0 else valvePointVal
        return DetailedViewItem(
            id = valvePoint["id"].toString(),
            disName = CHW_VALVE,
            currentValue = "$valveValue",
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = null,
            configuration = null,
            displayOrder = 2,
            shouldTakeFullRow = true
        )
    }

    private fun getDischargeAirflowTemperatureView(): DetailedViewItem {
        val dischargePoint = equip.dischargeAirTemp
        var dischargePointValue = dischargePoint.readHisVal().toString()

        dischargePointValue = if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            UnitUtils.fahrenheitToCelsiusTwoDecimal(
                dischargePointValue.toFloat().toString().toDouble()
            ).toString() + " °C"
        } else {
            "$dischargePointValue ℉"
        }

        return DetailedViewItem(
            id = dischargePoint.id,
            disName = DISCHARGE_AIRFLOW_TEMPERATURE,
            currentValue = dischargePointValue,
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = null,
            configuration = null,
            displayOrder = 3,
            shouldTakeFullRow = true
        )
    }

    private fun getEnteringAirflowTemperature(): DetailedViewItem {
        val enteringAirPoint = equip.enteringAirTemp
        var enteringAirPointValue = enteringAirPoint.readHisVal().toString()
        enteringAirPointValue = if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            UnitUtils.fahrenheitToCelsiusTwoDecimal(
                enteringAirPointValue.toFloat().toString().toDouble()
            ).toString() + " °C"
        } else {
            "$enteringAirPointValue ℉"
        }

        return DetailedViewItem(
            id = enteringAirPoint.id,
            disName = ENTERING_AIRFLOW_TEMPERATURE,
            currentValue = enteringAirPointValue,
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = null,
            configuration = null,
            displayOrder = 4,
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
                val s = getSchedule()
                val vS = getVacationSchedule()
                val sS = getSpecialSchedule()
                val eSS = getEquipScheduleStatus()

                withContext(Dispatchers.Main) {
                    CcuLog.d(L.TAG_CCU_ZONE,  "Refreshing Vacation Schedule")
                    tempProfileViewModel.setSchedule(s)
                    tempProfileViewModel.setVacationSchedule(vS)
                    tempProfileViewModel.setSpecialSchedule(sS)
                    tempProfileViewModel.setEquipScheduleStatusPoint(eSS)
                }
            } catch (e: Exception) {
                CcuLog.e(L.TAG_CCU_ZONE,  "Failed to refresh schedules: ${e.message}", e)
            }
        }
    }

}