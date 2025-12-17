package a75f.io.renatus.ui.zonescreen.tempprofiles.helper

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Schedule
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.HsCpuEquip
import a75f.io.domain.equips.hyperstat.HsHpuEquip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.HsPipe2Equip
import a75f.io.domain.equips.hyperstat.HsPipe4Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleManager.getScheduleStateString
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.util.HYPERSTAT
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSPipe2FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSPipe4FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSSelectedFanMode
import a75f.io.logic.bo.building.statprofiles.util.getHpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getSelectedConditioningMode
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.R
import a75f.io.renatus.profiles.mystat.CPU
import a75f.io.renatus.profiles.mystat.HPU
import a75f.io.renatus.profiles.mystat.PIPE2
import a75f.io.renatus.profiles.mystat.PIPE4
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
import a75f.io.renatus.ui.zonescreen.tempprofiles.view.showHyperStatDetailedView
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


class HyperStatHelper(
    private val equipMap: Equip,
    private val profileType: ProfileType,
    private val context: Context
) : PointValueChangeListener {
    private val tempProfileViewModel = TempProfileViewModel()
    val equip: HyperStatEquip = Domain.getDomainEquip(equipMap.id) as HyperStatEquip
    val configuration: HyperStatConfiguration = getHsConfiguration(equip.equipRef)!!
    private val detailViewItems = mutableStateMapOf<String, DetailedViewItem>()

    companion object {
        fun create(
            equipMap: Equip,
            profileType: ProfileType,
            context: Context
        ): HyperStatHelper {
            return HyperStatHelper(equipMap, profileType, context)
        }
    }

    fun profileName(): String {
        return when (profileType) {
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                HYPERSTAT + "-" + CPU.uppercase() + " ( " + equipMap.group + " )"
            }

            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
                HYPERSTAT + "-" + HPU.uppercase() + " ( " + equipMap.group + " )"
            }

            ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
                HYPERSTAT + "-" + PIPE2.uppercase() + " ( " + equipMap.group + " )"
            }
            ProfileType.HYPERSTAT_FOUR_PIPE_FCU -> {
                HYPERSTAT + "-" + PIPE4.uppercase() + " ( " + equipMap.group + " )"
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
        showHyperStatDetailedView(
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

    private fun setScheduleProperties() {
        if(tempProfileViewModel.showSchedule) {
            tempProfileViewModel.setEquipScheduleStatusPoint(getEquipScheduleStatus())
            tempProfileViewModel.setSchedule(getSchedule())
            tempProfileViewModel.setSpecialSchedule(getSpecialSchedule())
            tempProfileViewModel.setVacationSchedule(getVacationSchedule())
        }
    }


    private fun loadDetailedViewDefaultValues() {
        when (profileType) {
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                loadHyperStatDefaults(equip as HsCpuEquip)
            }
            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
                loadHyperStatDefaults(equip as HsHpuEquip)
            }
            ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
                loadHyperStatDefaults(equip as HsPipe2Equip)
            }
            ProfileType.HYPERSTAT_FOUR_PIPE_FCU -> {
                loadHyperStatDefaults(equip as HsPipe4Equip)
            }

            else -> {}
        }
    }

    private fun loadHyperStatDefaults(equip: Any) {
        detailViewItems.clear()

        val conditioningMode = getConditioningModeView()
        detailViewItems[conditioningMode.id.toString()] = conditioningMode

        val fanOpMode = getFanOpModeView()
        detailViewItems[fanOpMode.id.toString()] = fanOpMode

        if ((equip as? HsCpuEquip)?.humidifierEnable?.pointExists() == true ||
            (equip as? HsPipe2Equip)?.humidifierEnable?.pointExists() == true ||
            (equip as? HsPipe4Equip)?.humidifierEnable?.pointExists() == true ||
            (equip as? HsHpuEquip)?.humidifierEnable?.pointExists() == true) {
            val humidifierView = getHumidifierView()
            detailViewItems[humidifierView.id.toString()] = humidifierView
        }

        if ((equip as? HsCpuEquip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? HsPipe2Equip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? HsPipe4Equip)?.dehumidifierEnable?.pointExists() == true ||
            (equip as? HsHpuEquip)?.dehumidifierEnable?.pointExists() == true) {
            val deHumidifierView = getDeHumidifierView()
            detailViewItems[deHumidifierView.id.toString()] = deHumidifierView
        }

        if (profileType == ProfileType.HYPERSTAT_TWO_PIPE_FCU) {
            equip as HsPipe2Equip
            if (equip.dischargeAirTemperature.pointExists()) {
                val supplyWaterTemp = getSupplyWaterTempView(false)
                detailViewItems[supplyWaterTemp.id.toString()] = supplyWaterTemp
            } else {
                val supplyWaterTemp = getSupplyWaterTempView(true)
                detailViewItems[supplyWaterTemp.id.toString()] = supplyWaterTemp
            }

        } else {
            if (profileType == ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT) {
                equip as HsCpuEquip
                if (equip.dischargeAirTemperature.pointExists()) {
                    val dischargeAirFlow = getDischargeAirTempView()
                    detailViewItems[dischargeAirFlow.id.toString()] = dischargeAirFlow
                }
            } else if(profileType == ProfileType.HYPERSTAT_HEAT_PUMP_UNIT) {
                equip as HsHpuEquip
                if (equip.dischargeAirTemperature.pointExists()) {
                    val dischargeAirFlow = getDischargeAirTempView()
                    detailViewItems[dischargeAirFlow.id.toString()] = dischargeAirFlow
                }
            } else if(profileType == ProfileType.HYPERSTAT_FOUR_PIPE_FCU) {
                equip as HsPipe4Equip
                if (equip.dischargeAirTemperature.pointExists()) {
                    val dischargeAirFlow = getDischargeAirTempView()
                    detailViewItems[dischargeAirFlow.id.toString()] = dischargeAirFlow
                }
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
                oldItem.disName?.contains(CONDITIONING_MODE) == true -> getConditioningModeView()
                oldItem.disName?.contains(FAN_MODE) == true -> getFanOpModeView()
                oldItem.disName?.contains(SUPPLY_WATER_TEMPERATURE) == true -> {
                    if (profileType == ProfileType.HYPERSTAT_TWO_PIPE_FCU) {
                        if (equip.dischargeAirTemperature.pointExists()) {
                            getSupplyWaterTempView(false)
                        } else {
                            getSupplyWaterTempView()
                        }
                    } else {
                        getSupplyWaterTempView()
                    }
                }
                oldItem.disName?.contains(DISCHARGE_AIRFLOW_TEMPERATURE) == true -> {
                    if (profileType == ProfileType.HYPERSTAT_TWO_PIPE_FCU) {
                        getDischargeAirTempView(false)
                    } else {
                        getDischargeAirTempView()
                    }
                }
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


    private fun getConditioningModeView(): DetailedViewItem {

        val conditioningMode = getSelectedConditioningMode(
            configuration,
            equip.conditioningMode.readPriorityVal().toInt()
        )
        val possibleConditioningMode = getPossibleConditionMode(configuration)
        var conditionMode = 0
        try {
            conditionMode = conditioningMode
        } catch (e: Exception) {
            e.printStackTrace()
        }
        var conModeAdapter: List<String> = listOf(OFF, AUTO, HEAT_ONLY, COOL_ONLY)


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
        var fanOptions: List<String> = listOf()
        when (profileType) {

            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                configuration as CpuConfiguration
                selectedFanMode = getHSSelectedFanMode(
                    getCpuFanLevel(configuration),
                    equip.fanOpMode.readPriorityVal().toInt()
                )
                val fanSpinnerSelectionValues =
                    RelayUtil.getFanOptionByLevel(getCpuFanLevel(configuration))
                fanOptions =
                    context.resources.getStringArray(fanSpinnerSelectionValues).toList()
                if (selectedFanMode > fanOptions.size) {
                    selectedFanMode = 0
                    CcuLog.e(L.TAG_CCU_ZONE, "Fan Mode is not in the range falling back to off")
                }
            }

            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
                configuration as HpuConfiguration
                selectedFanMode = getHSSelectedFanMode(
                    getHpuFanLevel(configuration),
                    equip.fanOpMode.readPriorityVal().toInt()
                )
                val fanSpinnerSelectionValues =
                    RelayUtil.getFanOptionByLevel(getHpuFanLevel(configuration))
                fanOptions =
                    context.resources.getStringArray(fanSpinnerSelectionValues).toList()
                if (selectedFanMode > fanOptions.size) {
                    selectedFanMode = 0
                    CcuLog.e(L.TAG_CCU_ZONE, "Fan Mode is not in the range falling back to off")
                }
            }

            ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
                configuration as Pipe2Configuration
                selectedFanMode = getHSSelectedFanMode(
                    getHSPipe2FanLevel(configuration),
                    equip.fanOpMode.readPriorityVal().toInt()
                )
                val fanSpinnerSelectionValues =
                    RelayUtil.getFanOptionByLevel(getHSPipe2FanLevel(configuration))
                fanOptions =
                    context.resources.getStringArray(fanSpinnerSelectionValues).toList()
                if (selectedFanMode > fanOptions.size) {
                    selectedFanMode = 0
                    CcuLog.e(L.TAG_CCU_ZONE, "Fan Mode is not in the range falling back to off")
                }

            }
            ProfileType.HYPERSTAT_FOUR_PIPE_FCU -> {
                configuration as HsPipe4Configuration
                selectedFanMode = getHSSelectedFanMode(
                    getHSPipe4FanLevel(configuration),
                    equip.fanOpMode.readPriorityVal().toInt()
                )
                val fanSpinnerSelectionValues =
                    RelayUtil.getFanOptionByLevel(getHSPipe4FanLevel(configuration))
                fanOptions =
                    context.resources.getStringArray(fanSpinnerSelectionValues).toList()
                if (selectedFanMode > fanOptions.size) {
                    selectedFanMode = 0
                    CcuLog.e(L.TAG_CCU_ZONE, "Fan Mode is not in the range falling back to off")
                }

            }

            else -> {}
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

    @SuppressLint("DefaultLocale")
    private fun getSupplyWaterTempView(shouldTakeFullRow: Boolean = true): DetailedViewItem {
        equip as HsPipe2Equip
        var supplyTemp = equip.leavingWaterTemperature.readHisVal().toString()
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted =
                UnitUtils.fahrenheitToCelsiusTwoDecimal(supplyTemp.toDouble())
            supplyTemp = "%.2f °C".format(converted)
        } else {
            supplyTemp = "$supplyTemp ℉"
        }

        val profile = L.getProfile(equip.nodeAddress.toString().toLong()) as HyperStatPipe2Profile

        return DetailedViewItem(
            id = equip.leavingWaterTemperature.id,
            disName = SUPPLY_WATER_TEMPERATURE,
            currentValue = "$supplyTemp (${profile.supplyDirection()})",
            selectedIndex = 0,
            dropdownOptions = emptyList(),
            usesDropdown = false,
            point = equip.leavingWaterTemperature,
            configuration = configuration,
            displayOrder = 5,
            shouldTakeFullRow = shouldTakeFullRow
        )
    }


    private fun getDischargeAirTempView(shouldTakeFullRow: Boolean = true): DetailedViewItem {
        var dischargeValue = equip.dischargeAirTemperature.readHisVal().toString()

        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted =
                UnitUtils.fahrenheitToCelsiusTwoDecimal(dischargeValue.toDouble())
            dischargeValue =  "%.2f °C".format(converted)
        } else {
            dischargeValue = "$dischargeValue ℉"
        }


        return DetailedViewItem(
            id = equip.dischargeAirTemperature.id,
            disName = DISCHARGE_AIRFLOW_TEMPERATURE,
            currentValue = dischargeValue,
            usesDropdown = false,
            point = equip.dischargeAirTemperature,
            configuration = configuration,
            displayOrder = 6,
            shouldTakeFullRow = shouldTakeFullRow
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