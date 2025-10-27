package a75f.io.renatus.ui.tempprofiles.helper

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Schedule
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.equips.mystat.MyStatPipe4Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleManager.getScheduleStateString
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe2FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe4FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatSelectedConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatSelectedFanMode
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.R
import a75f.io.renatus.profiles.mystat.CPU
import a75f.io.renatus.profiles.mystat.HPU
import a75f.io.renatus.profiles.mystat.MYSTAT
import a75f.io.renatus.profiles.mystat.PIPE2
import a75f.io.renatus.profiles.mystat.PIPE4
import a75f.io.renatus.profiles.mystat.ui.getSupplyDirection
import a75f.io.renatus.ui.AUTO
import a75f.io.renatus.ui.CONDITIONING_MODE
import a75f.io.renatus.ui.COOL_ONLY
import a75f.io.renatus.ui.DEHUMIDIFIER
import a75f.io.renatus.ui.DISCHARGE_AIRFLOW_TEMPERATURE
import a75f.io.renatus.ui.EQUIP_SCHEDULE_STATUS
import a75f.io.renatus.ui.FAN_MODE
import a75f.io.renatus.ui.HEAT_ONLY
import a75f.io.renatus.ui.HUMIDIFIER
import a75f.io.renatus.ui.OFF
import a75f.io.renatus.ui.SCHEDULE
import a75f.io.renatus.ui.SPECIAL_SCHEDULE
import a75f.io.renatus.ui.STATUS
import a75f.io.renatus.ui.SUPPLY_WATER_TEMPERATURE
import a75f.io.renatus.ui.VACATIONS
import a75f.io.renatus.ui.model.DetailedViewItem
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.tempprofiles.getNamedSchedulePosition
import a75f.io.renatus.ui.tempprofiles.getScheduleList
import a75f.io.renatus.ui.tempprofiles.view.showMyStatDetailedView
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel
import a75f.io.renatus.util.RelayUtil
import android.annotation.SuppressLint
import android.content.Context
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


class MyStatHelper(
    private val equipMap: Equip,
    private val profileType: ProfileType,
    private val context: Context
) : PointValueChangeListener {
    private val tempProfileViewModel = TempProfileViewModel()
    val equip: MyStatEquip = Domain.getDomainEquip(equipMap.id) as MyStatEquip
    val configuration: MyStatConfiguration = getMyStatConfiguration(equipMap.id) as MyStatConfiguration
    private val detailViewItems = mutableStateMapOf<String, DetailedViewItem>()

    companion object {
        fun create(
            equipMap: Equip,
            profileType: ProfileType,
            context: Context
        ): MyStatHelper {
            return MyStatHelper(equipMap, profileType, context)
        }
    }

    fun profileName(): String {
        return when (profileType) {
            ProfileType.MYSTAT_CPU -> {
                MYSTAT + "-" + CPU + "( " + equipMap.group + " )"
            }

            ProfileType.MYSTAT_HPU -> {
                MYSTAT + "-" + HPU + "( " + equipMap.group + " )"
            }

            ProfileType.MYSTAT_PIPE2 -> {
                MYSTAT + "-" + PIPE2 + "( " + equipMap.group + " )"
            }
            ProfileType.MYSTAT_PIPE4 -> {
                MYSTAT + "-" + PIPE4  + "( " + equipMap.group + " )"
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
        showMyStatDetailedView(
            composeView,
            tempProfileViewModel,
            equipMap.id,
            this
        ) { selectedIndex, point ->
            onValueChange(selectedIndex, point)
        }
        linearLayoutZonePoints.addView(composeLayout)
        loadDetailedViewDefaultValues()
    }



    private fun loadDetailedViewDefaultValues() {
        when (profileType) {
            ProfileType.MYSTAT_CPU -> {
                loadMyStatDefaults(equip as MyStatCpuEquip)
            }
            ProfileType.MYSTAT_HPU -> {
                loadMyStatDefaults(equip as MyStatHpuEquip)
            }
            ProfileType.MYSTAT_PIPE2 -> {
                loadMyStatDefaults(equip as MyStatPipe2Equip)
            }
            ProfileType.MYSTAT_PIPE4 -> {
                loadMyStatDefaults(equip as MyStatPipe4Equip)
            }

            else -> {}
        }
    }

    private fun loadMyStatDefaults(equip: Any) {
        detailViewItems.clear()

        val conditioningMode = getConditioningMode()
        detailViewItems[conditioningMode.id.toString()] = conditioningMode

        val fanOpMode = getFanOpModeView()
        detailViewItems[fanOpMode.id.toString()] = fanOpMode

        if ((equip as? MyStatCpuEquip)?.humidifierEnable?.pointExists() == true ||
            (equip as? MyStatPipe2Equip)?.humidifierEnable?.pointExists() == true ||
            (equip as? MyStatPipe4Equip)?.humidifierEnable?.pointExists() == true ||
            (equip as? MyStatHpuEquip)?.humidifierEnable?.pointExists() == true) {
            val humidifierView = getHumidifierView()
            detailViewItems[humidifierView.id.toString()] = humidifierView
        }

        if ((equip as? MyStatCpuEquip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? MyStatPipe2Equip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? MyStatPipe4Equip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? MyStatHpuEquip)?.dehumidifierEnable?.pointExists() == true) {
            val deHumidifierView = getDeHumidifierView()
            detailViewItems[deHumidifierView.id.toString()] = deHumidifierView
        }

        if (profileType == ProfileType.MYSTAT_PIPE2) {
            val supplyWaterTemp = getSupplyWaterTempView()
            detailViewItems[supplyWaterTemp.id.toString()] = supplyWaterTemp
        } else {
            if (configuration.universalIn1Enabled.enabled &&
                configuration.universalIn1Association.associationVal.toDouble() == 0.0
            ) {
                val dischargeAirFloe = getDischargeAirTempView()
                detailViewItems[dischargeAirFloe.id.toString()] = dischargeAirFloe
            }
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
                oldItem.disName?.contains(CONDITIONING_MODE) == true -> getConditioningMode()
                oldItem.disName?.contains(FAN_MODE) == true -> getFanOpModeView()
                oldItem.disName?.contains(SUPPLY_WATER_TEMPERATURE) == true -> getSupplyWaterTempView()
                oldItem.disName?.contains(DISCHARGE_AIRFLOW_TEMPERATURE) == true -> getDischargeAirTempView()
                oldItem.disName?.contains(HUMIDIFIER) == true -> getHumidifierView()
                oldItem.disName?.contains(DEHUMIDIFIER) == true -> getDeHumidifierView()

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
        equip.equipStatusMessage.pointExists()
        return HeaderViewItem(
            id = equip.equipStatusMessage.id,
            disName = STATUS,
            currentValue = equip.equipStatusMessage.readDefaultStrVal(),
            usesDropdown = false
        )
    }


    private fun getConditioningMode(): DetailedViewItem {
        var conditionMode = getMyStatSelectedConditioningMode(
            configuration, equip.conditioningMode.readPriorityVal().toInt()
        )

        var conditioningModeDropDownSize = 0
        val possibleConditioningMode = getMyStatPossibleConditionMode(configuration)
        val dropDownOptions = when (possibleConditioningMode) {
            PossibleConditioningMode.OFF -> {
                conditioningModeDropDownSize = 1
                conditionMode = 0
                listOf(OFF)
            }

            PossibleConditioningMode.COOLONLY -> {
                conditioningModeDropDownSize = 2
                listOf(OFF, COOL_ONLY)
            }

            PossibleConditioningMode.HEATONLY -> {
                conditioningModeDropDownSize = 2
                listOf(OFF, HEAT_ONLY)
            }

            PossibleConditioningMode.BOTH -> {
                conditioningModeDropDownSize = 5
                listOf(OFF, AUTO, HEAT_ONLY, COOL_ONLY)
            }

            else -> listOf()
        }

        return DetailedViewItem(
            id = equip.conditioningMode.id,
            disName = CONDITIONING_MODE,
            currentValue = equip.conditioningMode.readDefaultStrVal(),
            selectedIndex = if (conditionMode in 0 until conditioningModeDropDownSize) conditionMode else 0,
            dropdownOptions = dropDownOptions,
            usesDropdown = true,
            point = equip.conditioningMode,
            configuration = configuration,
            displayOrder = 3
        )
    }


    private fun getFanOpModeView(): DetailedViewItem {
        var fanLevel = -1

        when (profileType) {
            ProfileType.MYSTAT_PIPE2 -> {
                configuration as MyStatPipe2Configuration
                fanLevel = getMyStatPipe2FanLevel(configuration)
            }
            ProfileType.MYSTAT_CPU -> {
                configuration as MyStatCpuConfiguration
                fanLevel = getMyStatCpuFanLevel(configuration)
            }
            ProfileType.MYSTAT_HPU -> {
                configuration as MyStatHpuConfiguration
                fanLevel = getMyStatHpuFanLevel(configuration)
            }
            ProfileType.MYSTAT_PIPE4 -> {
                configuration as MyStatPipe4Configuration
                fanLevel = getMyStatPipe4FanLevel(configuration)
            }
            else -> {}
        }

        val fanMode = equip.fanOpMode.readPriorityVal().toInt()
        val fanSpinnerSelectionValues = RelayUtil.getFanOptionByLevel(fanLevel)
        val fanOptions: List<String> =
            context.resources.getStringArray(fanSpinnerSelectionValues).toList()
        val actualFanMode = getMyStatSelectedFanMode(fanLevel, fanMode)

        return DetailedViewItem(
            id = equip.fanOpMode.id,
            disName = FAN_MODE,
            currentValue = equip.fanOpMode.readDefaultStrVal(),
            selectedIndex = if (actualFanMode >= 0 && actualFanMode < fanOptions.size) actualFanMode else 0,
            dropdownOptions = fanOptions,
            usesDropdown = true,
            point = equip.fanOpMode,
            configuration = configuration,
            displayOrder = 4
        )
    }

    @SuppressLint("DefaultLocale")
    private fun getSupplyWaterTempView(): DetailedViewItem {
        equip as MyStatPipe2Equip
        var supplyTemp = equip.leavingWaterTemperature.readHisVal().toString()
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted =
                UnitUtils.fahrenheitToCelsiusTwoDecimal(supplyTemp.toDouble())
            supplyTemp = "${String.format("%.2f", converted)} °C"
        } else {
            supplyTemp = "$supplyTemp ℉"
        }

        return DetailedViewItem(
            id = equip.leavingWaterTemperature.id,
            disName = SUPPLY_WATER_TEMPERATURE,
            currentValue = "$supplyTemp (${getSupplyDirection(equip.nodeAddress.toString())})",
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = equip.leavingWaterTemperature,
            configuration = configuration,
            displayOrder = 5,
            shouldTakeFullRow = true
        )
    }


    private fun getDischargeAirTempView(): DetailedViewItem {
        if (profileType == ProfileType.MYSTAT_CPU)
            equip as MyStatCpuEquip
        else if (profileType == ProfileType.MYSTAT_PIPE4)
            equip as MyStatPipe4Equip
        else if (profileType == ProfileType.MYSTAT_HPU)
            equip as MyStatHpuEquip

        var dischargeAirTemperature = equip.dischargeAirTemperature.readHisVal().toString()
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted =
                UnitUtils.fahrenheitToCelsiusTwoDecimal(dischargeAirTemperature.toDouble())
            dischargeAirTemperature = "%.2f °C".format(converted)
        } else {
            dischargeAirTemperature = "$dischargeAirTemperature ℉"
        }

        return DetailedViewItem(
            id = equip.dischargeAirTemperature.id,
            disName = DISCHARGE_AIRFLOW_TEMPERATURE,
            currentValue = dischargeAirTemperature,
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = equip.dischargeAirTemperature,
            configuration = configuration,
            displayOrder = 6,
            shouldTakeFullRow = true
        )
    }

    private fun getHumidifierView(): DetailedViewItem {
        val targetHumidifier = equip.targetHumidifier.readPriorityVal()
        val arrayHumidityTargetList = ArrayList<String>()
        for (pos in 1..100) arrayHumidityTargetList.add("$pos%")

        return  DetailedViewItem(
            id = equip.targetHumidifier.id,
            disName = HUMIDIFIER,
            currentValue = equip.targetHumidifier.readDefaultStrVal(),
            selectedIndex = targetHumidifier.toInt() - 1,
            dropdownOptions = arrayHumidityTargetList,
            usesDropdown = true,
            point = equip.targetHumidifier,
            configuration = configuration,
            displayOrder = 1,
            shouldTakeFullRow = !equip.dehumidifierEnable.pointExists()
        )
    }

    private fun getDeHumidifierView(): DetailedViewItem {
        val targetDeHumidifier = equip.targetDehumidifier.readPriorityVal()
        val arrayDeHumidityTargetList = ArrayList<String>()
        for (pos in 1..100) arrayDeHumidityTargetList.add("$pos%")

        return  DetailedViewItem(
            id = equip.targetDehumidifier.id,
            disName = DEHUMIDIFIER,
            currentValue = equip.targetDehumidifier.readDefaultStrVal(),
            selectedIndex = targetDeHumidifier.toInt() - 1,
            dropdownOptions = arrayDeHumidityTargetList,
            usesDropdown = true,
            point = equip.targetDehumidifier,
            configuration = configuration,
            displayOrder = 2,
            shouldTakeFullRow = !equip.humidifierEnable.pointExists()
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