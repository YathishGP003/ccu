package a75f.io.renatus.ui.zonescreen.tempprofiles.helper

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Schedule
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstatsplit.HyperStatSplitEquip
import a75f.io.domain.equips.hyperstatsplit.HsSplitCpuEquip
import a75f.io.domain.equips.hyperstatsplit.Pipe2UVEquip
import a75f.io.domain.equips.hyperstatsplit.Pipe4UVEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleManager.getScheduleStateString
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getFanSelectionMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getHssProfileConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getSelectedConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getHssProfileFanLevel
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UnitVentilatorProfile
import a75f.io.logic.bo.building.statprofiles.util.CPUECON_FULL
import a75f.io.logic.bo.building.statprofiles.util.HYPERSTATSPLIT
import a75f.io.logic.bo.building.statprofiles.util.PIPE2_ECON
import a75f.io.logic.bo.building.statprofiles.util.PIPE4_ECON
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.R
import a75f.io.renatus.ui.zonescreen.AUTO
import a75f.io.renatus.ui.zonescreen.CONDITIONING_MODE
import a75f.io.renatus.ui.zonescreen.COOL_ONLY
import a75f.io.renatus.ui.zonescreen.DEHUMIDIFIER
import a75f.io.renatus.ui.zonescreen.DISCHARGE_AIRFLOW_TEMPERATURE
import a75f.io.renatus.ui.zonescreen.EQUIP_SCHEDULE_STATUS
import a75f.io.renatus.ui.zonescreen.FAN_MODE
import a75f.io.renatus.ui.zonescreen.HEAT_ONLY
import a75f.io.renatus.ui.zonescreen.HUMIDIFIER
import a75f.io.renatus.ui.zonescreen.OFF
import a75f.io.renatus.ui.zonescreen.SCHEDULE
import a75f.io.renatus.ui.zonescreen.SPECIAL_SCHEDULE
import a75f.io.renatus.ui.zonescreen.STATUS
import a75f.io.renatus.ui.zonescreen.SUPPLY_WATER_TEMPERATURE
import a75f.io.renatus.ui.zonescreen.VACATIONS
import a75f.io.renatus.ui.zonescreen.model.DetailedViewItem
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.tempprofiles.getNamedSchedulePosition
import a75f.io.renatus.ui.zonescreen.tempprofiles.getScheduleList
import a75f.io.renatus.ui.zonescreen.tempprofiles.view.showHyperStatSplitDetailedView
import a75f.io.renatus.ui.zonescreen.tempprofiles.viewmodel.TempProfileViewModel
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


class HyperStatSplitHelper(
    private val equipMap: Equip,
    private val profileType: ProfileType,
    private val context: Context
) : PointValueChangeListener {
    private val tempProfileViewModel = TempProfileViewModel()
    val equip: HyperStatSplitEquip = Domain.getEquip(equipMap.id) as HyperStatSplitEquip
    val configuration: HyperStatSplitConfiguration =  getSplitConfiguration(equipMap.id) as HyperStatSplitConfiguration
    private val detailViewItems = mutableStateMapOf<String, DetailedViewItem>()

    companion object {
        fun create(
            equipMap: Equip,
            profileType: ProfileType,
            context: Context
        ): HyperStatSplitHelper {
            return HyperStatSplitHelper(equipMap, profileType, context)
        }
    }

    fun profileName(): String {
        return when (profileType) {
            ProfileType.HYPERSTATSPLIT_CPU -> {
                HYPERSTATSPLIT + " - " + CPUECON_FULL + "( " + equipMap.group + " )"
            }

            ProfileType.HYPERSTATSPLIT_4PIPE_UV -> {
                HYPERSTATSPLIT + " - " + PIPE4_ECON + "( " + equipMap.group + " )"
            }

            ProfileType.HYPERSTATSPLIT_2PIPE_UV -> {
                HYPERSTATSPLIT + " - " + PIPE2_ECON + "( " + equipMap.group + " )"
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
        showHyperStatSplitDetailedView(
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
            ProfileType.HYPERSTATSPLIT_CPU -> {
                loadHyperStatSplitDefaults(equip as HsSplitCpuEquip)
            }
            ProfileType.HYPERSTATSPLIT_4PIPE_UV -> {
                loadHyperStatSplitDefaults(equip as Pipe4UVEquip)
            }
            ProfileType.HYPERSTATSPLIT_2PIPE_UV -> {
                loadHyperStatSplitDefaults(equip as Pipe2UVEquip)
            }
            else -> {}
        }
    }

    private fun loadHyperStatSplitDefaults(equip: HyperStatSplitEquip) {
        detailViewItems.clear()

        val conditioningMode = getConditioningMode()
        detailViewItems[conditioningMode.id.toString()] = conditioningMode

        val fanOpMode = getFanOpModeView()
        detailViewItems[fanOpMode.id.toString()] = fanOpMode

        if ((equip as? HsSplitCpuEquip)?.humidifierEnable?.pointExists() == true ||
            (equip as? Pipe4UVEquip)?.humidifierEnable?.pointExists() == true ||
            (equip as? Pipe2UVEquip)?.humidifierEnable?.pointExists() == true) {
            val humidifierView = getHumidifierView()
            detailViewItems[humidifierView.id.toString()] = humidifierView
        }

        if ((equip as? HsSplitCpuEquip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? Pipe4UVEquip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? Pipe2UVEquip)?.dehumidifierEnable?.pointExists() == true) {
            val dehumidifierView = getDeHumidifierView()
            detailViewItems[dehumidifierView.id.toString()] = dehumidifierView
        }

        if (equip.dischargeAirTemperature.pointExists()) {
            val dischargeAirFlow = getDischargeAirTempView()
            dischargeAirFlow.shouldTakeFullRow = false
            detailViewItems[dischargeAirFlow.id.toString()] = dischargeAirFlow
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
        var conditionMode = 0
        try {
            conditionMode = getSelectedConditioningMode(
                equip.conditioningMode.readPriorityVal().toInt(),
                configuration
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var conModeAdapter: List<String> = listOf(OFF, AUTO, HEAT_ONLY, COOL_ONLY)
        val possibleConditioningMode =  getHssProfileConditioningMode( configuration)
        if (possibleConditioningMode.name.contains(COOL_ONLY)
                || possibleConditioningMode.name == PossibleConditioningMode.COOLONLY.name
        ) {
            conModeAdapter = listOf(OFF, COOL_ONLY)
            if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal)
                conditionMode = conModeAdapter.size - 1

        } else if (possibleConditioningMode.name.contains(HEAT_ONLY)
        || possibleConditioningMode.name == PossibleConditioningMode.HEATONLY.name
        ) {

            conModeAdapter = listOf(OFF, HEAT_ONLY)
            if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal)
                conditionMode = conModeAdapter.size - 1

        }
        if (possibleConditioningMode.name.contains(OFF)
            || possibleConditioningMode.name == PossibleConditioningMode.OFF.name
        ) {
            conModeAdapter = listOf(OFF)
            conditionMode = 0
        }


        if (conditionMode > conModeAdapter.size) {
            conditionMode = 0
        }

        return DetailedViewItem(
            id = equip.conditioningMode.id,
            disName = CONDITIONING_MODE,
            currentValue = equip.conditioningMode.readDefaultStrVal(),
            selectedIndex = conditionMode,
            dropdownOptions = conModeAdapter,
            usesDropdown = true,
            point = equip.conditioningMode,
            configuration = configuration,
            displayOrder = 3,
            shouldTakeFullRow = false
        )
    }


    private fun getFanOpModeView(): DetailedViewItem {
        var selectedFanMode = 0
        try {
            selectedFanMode = getFanSelectionMode(equip, equip.fanOpMode.readPriorityVal().toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val fanSpinnerSelectionValues =
            RelayUtil.getFanOptionByLevel(getHssProfileFanLevel(equip))

        val fanOptions =
            context.resources.getStringArray(fanSpinnerSelectionValues).toList()

        if (selectedFanMode > fanOptions.size) {
            selectedFanMode = 0
            CcuLog.e(L.TAG_CCU_ZONE, "Fan Mode is not in the range falling back to off")
        }

        return DetailedViewItem(
            id = equip.fanOpMode.id,
            disName = FAN_MODE,
            currentValue = equip.fanOpMode.readDefaultStrVal(),
            selectedIndex = selectedFanMode,
            dropdownOptions = fanOptions,
            usesDropdown = true,
            point = equip.fanOpMode,
            configuration = configuration,
            displayOrder = 4,
            shouldTakeFullRow = false
        )
    }


    private fun getSupplyWaterTempView(): DetailedViewItem {
        return DetailedViewItem()
    }


    private fun getDischargeAirTempView(): DetailedViewItem {

        if (equip.mixedAirTemperature.pointExists()) {
            var mixedAirTempVal = equip.mixedAirTemperature.readHisVal().toString()

            mixedAirTempVal = if (UnitUtils.isCelsiusTunerAvailableStatus()) {
                UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    mixedAirTempVal.toFloat().toString().toDouble()
                ).toString() + " °C"
            } else {
                equip.mixedAirTemperature.readHisVal().toString() + " ℉"
            }

            if (profileType == ProfileType.HYPERSTATSPLIT_2PIPE_UV) {
                val profile = L.getProfile(equip.nodeAddress.toLong())
                mixedAirTempVal =
                    mixedAirTempVal + " (" + (profile as Pipe2UnitVentilatorProfile).supplyDirection() + ")"
            }

            return DetailedViewItem(
                id = equip.mixedAirTemperature.id,
                disName = DISCHARGE_AIRFLOW_TEMPERATURE,
                currentValue = mixedAirTempVal,
                usesDropdown = false,
                point = equip.dischargeAirTemperature,
                configuration = configuration,
                displayOrder = 6,
                shouldTakeFullRow = true
            )
        } else {
            var dischargeAirTempVal = equip.dischargeAirTemperature.readHisVal().toString()

            dischargeAirTempVal = if (UnitUtils.isCelsiusTunerAvailableStatus()) {
                UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    dischargeAirTempVal.toFloat().toString().toDouble()
                ).toString() + " °C"
            } else {
                equip.dischargeAirTemperature.readHisVal().toString() + " ℉"
            }

            if (profileType == ProfileType.HYPERSTATSPLIT_2PIPE_UV) {
                val profile = L.getProfile(equip.nodeAddress.toLong())
                dischargeAirTempVal =
                    dischargeAirTempVal + " (" + (profile as Pipe2UnitVentilatorProfile).supplyDirection() + ")"
            }


            return DetailedViewItem(
                id = equip.dischargeAirTemperature.id,
                disName = DISCHARGE_AIRFLOW_TEMPERATURE,
                currentValue = dischargeAirTempVal,
                usesDropdown = false,
                point = equip.dischargeAirTemperature,
                configuration = configuration,
                displayOrder = 6,
                shouldTakeFullRow = true
            )
        }
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