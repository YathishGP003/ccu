package a75f.io.renatus.profiles.hyperstatv2.util

/**
 * Created by Manjunath K on 25-07-2022.
 */

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Tags
import a75f.io.device.mesh.hyperstat.getHyperStatDevice
import a75f.io.device.mesh.hyperstat.getHyperStatDomainDevice
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.Thermistor
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.sensors.SensorManager
import a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.pipe2.HyperStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HSCPU
import a75f.io.logic.bo.building.statprofiles.util.HYPERSTAT
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.StatZoneStatus
import a75f.io.logic.bo.building.statprofiles.util.getActualConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSPipe2FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHSSelectedFanMode
import a75f.io.logic.bo.building.statprofiles.util.getHpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getHsConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getSelectedConditioningMode
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.util.uiutils.updateUserIntentPoints
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.RelayUtil
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.compose.ui.res.stringResource
import java.util.Locale


private fun showTextView(viewId: Int, rootView: View, text: String) {
    val textView = rootView.findViewById<TextView>(viewId)
    textView.text = text
}

private fun setTitleStatusConfig(
        viewTitle: View, viewStatus: View, nodeAddress: String, status: String, profileName: String
) {
    val textViewModule = viewTitle.findViewById<TextView>(R.id.module_status)
    HeartBeatUtil.moduleStatus(textViewModule, nodeAddress)
    showTextView(R.id.textProfile, viewTitle, "$HYPERSTAT - $profileName ( $nodeAddress )")
    showTextView(R.id.text_status, viewStatus, status)
    showTextView(R.id.last_updated_status, viewStatus, HeartBeatUtil.getLastUpdatedTime(nodeAddress))
}

@SuppressLint("DefaultLocale")
private fun showDischargeConfigIfRequired(dischargeView: View, pointsList: HashMap<String,Any>, rootView: LinearLayout ) {
    if (pointsList.containsKey(StatZoneStatus.DISCHARGE_AIRFLOW.name)) {
        var dischargeValue = pointsList[StatZoneStatus.DISCHARGE_AIRFLOW.name].toString() + " ℉"
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted = UnitUtils.fahrenheitToCelsiusTwoDecimal(pointsList[StatZoneStatus.DISCHARGE_AIRFLOW.name] as Double)
            dischargeValue = "${String.format("%.2f", converted)} °C"
        }
        showTextView(R.id.text_airflowValue, dischargeView, dischargeValue)
        rootView.removeView(dischargeView)
        rootView.addView(dischargeView)
    } else {
        rootView.removeView(dischargeView)
    }
}

fun loadHyperStatCpuProfile(
        cpuV2EquipPoints: HashMap<String,Any>, inflater: LayoutInflater,
        linearLayoutZonePoints: LinearLayout,
        equipId: String?, nodeAddress: String,
        context: Activity
) {
    val viewTitle: View = inflater.inflate(R.layout.zones_item_title, null)
    val viewStatus: View = inflater.inflate(R.layout.zones_item_status, null)
    val viewPointRow1: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewPointRow2: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewDischarge: View = inflater.inflate(R.layout.zones_item_discharge, null)
    val config = cpuV2EquipPoints[StatZoneStatus.CONFIG.name]  as CpuConfiguration
    val equip = cpuV2EquipPoints[StatZoneStatus.EQUIP.name]  as CpuV2Equip
    setTitleStatusConfig(viewTitle, viewStatus, nodeAddress, cpuV2EquipPoints[StatZoneStatus.STATUS.name].toString(),
            HSCPU.uppercase(Locale.ROOT))
    setUpConditionFanConfig(
        viewPointRow1,
        cpuV2EquipPoints,
        equipId!!,
        context,
        ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
        equip,
        config
    )
    setUpHumidifierDeHumidifier(viewPointRow2, cpuV2EquipPoints, equipId, context, equip, config)

    linearLayoutZonePoints.apply {
        addView(viewTitle)
        addView(viewStatus)
        addView(viewPointRow2)
        addView(viewPointRow1)
        setPadding(0, 0, 0, 10)
    }
    showDischargeConfigIfRequired(viewDischarge,cpuV2EquipPoints,linearLayoutZonePoints)
}

fun getHyperStatCpuDetails(equipDetails: Equip): HashMap<String, Any> {
    val equip = Domain.getDomainEquip(equipDetails.id) as CpuV2Equip
    val cpuPoints = HashMap<String, Any>()
    val cpuConfiguration = getHsConfiguration(equipDetails.id) as CpuConfiguration
    val fanMode = getHSSelectedFanMode(getCpuFanLevel(cpuConfiguration), equip.fanOpMode.readPriorityVal().toInt())
    val conditioningMode = getSelectedConditioningMode(cpuConfiguration, equip.conditioningMode.readPriorityVal().toInt())
    val possibleConditioningMode = getPossibleConditionMode(cpuConfiguration)
    cpuPoints[StatZoneStatus.CONFIG.name] = cpuConfiguration
    cpuPoints[StatZoneStatus.EQUIP.name] = equip
    cpuPoints[StatZoneStatus.STATUS.name] = equip.equipStatusMessage.readDefaultStrVal()
    cpuPoints[StatZoneStatus.FAN_MODE.name] = fanMode
    cpuPoints[StatZoneStatus.CONDITIONING_MODE.name] = conditioningMode
    if (equip.dischargeAirTemperature.pointExists()) {
        cpuPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name] = equip.dischargeAirTemperature.readHisVal()
    }
    cpuPoints[StatZoneStatus.FAN_LEVEL.name] = getCpuFanLevel(cpuConfiguration)
    cpuPoints[StatZoneStatus.CONDITIONING_ENABLED.name] = possibleConditioningMode
    if (equip.humidifierEnable.pointExists()) {
        cpuPoints[StatZoneStatus.TARGET_HUMIDITY.name] = equip.targetHumidifier.readPriorityVal()
    }
    if (equip.dehumidifierEnable.pointExists()) {
        cpuPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] = equip.targetDehumidifier.readPriorityVal()
    }
    cpuPoints.forEach { CcuLog.i(L.TAG_CCU_HSHST, "CPU data : ${it.key} : ${it.value}") }
    return cpuPoints
}


@SuppressLint("DefaultLocale")
fun loadHyperStatHpuProfile(
        equipPoints: HashMap<String, Any>, inflater: LayoutInflater,
        linearLayoutZonePoints: LinearLayout,
        equipId: String?, nodeAddress: String?,
        context: Activity
) {

    val viewTitle: View = inflater.inflate(R.layout.zones_item_title, null)
    val viewStatus: View = inflater.inflate(R.layout.zones_item_status, null)
    val viewPointRow1: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewPointRow2: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewDischarge: View = inflater.inflate(R.layout.zones_item_discharge, null)

    val config = equipPoints[StatZoneStatus.CONFIG.name] as HpuConfiguration
    val equip = equipPoints[StatZoneStatus.EQUIP.name] as HpuV2Equip

    setTitleStatusConfig(viewTitle, viewStatus, nodeAddress!!, equipPoints[StatZoneStatus.STATUS.name].toString(), " Heat Pump Unit")
    setUpConditionFanConfig(
        viewPointRow1,
        equipPoints,
        equipId!!,
        context,
        ProfileType.HYPERSTAT_HEAT_PUMP_UNIT,
        equip,
        config
    )
    setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equipId, context, equip, config)

    linearLayoutZonePoints.apply {
        addView(viewTitle)
        addView(viewStatus)
        addView(viewPointRow2)
        addView(viewPointRow1)
        addView(viewDischarge)
        setPadding(0, 0, 0, 10)
    }
    showDischargeConfigIfRequired(viewDischarge,equipPoints,linearLayoutZonePoints)
}

fun getHyperStatHpuDetails(equipDetails: Equip): HashMap<String, Any> {

    val equip = Domain.getDomainEquip(equipDetails.id) as HpuV2Equip
    val hpuPoints = HashMap<String, Any>()
    val hpuConfiguration = getHsConfiguration(equipDetails.id) as HpuConfiguration
    val fanMode = getHSSelectedFanMode(getHpuFanLevel(hpuConfiguration), equip.fanOpMode.readPriorityVal().toInt())
    val conditioningMode = getSelectedConditioningMode(hpuConfiguration, equip.conditioningMode.readPriorityVal().toInt())
    val possibleConditioningMode = getPossibleConditionMode(hpuConfiguration)
    hpuPoints[StatZoneStatus.CONFIG.name] = hpuConfiguration
    hpuPoints[StatZoneStatus.EQUIP.name] = equip
    hpuPoints[StatZoneStatus.STATUS.name] = equip.equipStatusMessage.readDefaultStrVal()
    hpuPoints[StatZoneStatus.FAN_MODE.name] = fanMode
    hpuPoints[StatZoneStatus.CONDITIONING_MODE.name] = conditioningMode
    hpuPoints[StatZoneStatus.FAN_LEVEL.name] = getHpuFanLevel(hpuConfiguration)
    hpuPoints[StatZoneStatus.CONDITIONING_ENABLED.name] = possibleConditioningMode

    if (equip.dischargeAirTemperature.pointExists()) {
        hpuPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name] = equip.dischargeAirTemperature.readHisVal()
    }
    if (equip.humidifierEnable.pointExists()) {
        hpuPoints[StatZoneStatus.TARGET_HUMIDITY.name] = equip.targetHumidifier.readPriorityVal()
    }
    if (equip.dehumidifierEnable.pointExists()) {
        hpuPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] = equip.targetDehumidifier.readPriorityVal()
    }
    hpuPoints.forEach { CcuLog.i(L.TAG_CCU_HSHPU, "HPU data : ${it.key} : ${it.value}") }
    return hpuPoints
}


@SuppressLint("DefaultLocale")
fun loadHyperStatPipe2Profile(
        equipPoints: HashMap<String, Any>,
        inflater: LayoutInflater,
        linearLayoutZonePoints: LinearLayout,
        equipId: String?,
        nodeAddress: String?,
        context: Activity
) {

    val viewTitle: View = inflater.inflate(R.layout.zones_item_title, null)
    val viewStatus: View = inflater.inflate(R.layout.zones_item_status, null)
    val viewPointRow1: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewPointRow2: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewDischarge: View = inflater.inflate(R.layout.zones_item_discharge, null)
    val supplyView: View = inflater.inflate(R.layout.zones_item_discharge, null)

    val config = equipPoints[StatZoneStatus.CONFIG.name] as Pipe2Configuration
    val equip = equipPoints[StatZoneStatus.EQUIP.name] as Pipe2V2Equip

    setTitleStatusConfig(viewTitle, viewStatus, nodeAddress!!, equipPoints[StatZoneStatus.STATUS.name].toString(), " 2 Pipe FCU")
    setUpConditionFanConfig(
        viewPointRow1,
        equipPoints,
        equipId!!,
        context,
        ProfileType.HYPERSTAT_TWO_PIPE_FCU,
        equip,
        config
    )
    setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equipId, context, equip, config)

    setTitleStatusConfig(viewTitle, viewStatus,
        nodeAddress, equipPoints[StatZoneStatus.STATUS.name].toString(), " 2 Pipe FCU")
    setUpConditionFanConfig(
        viewPointRow1,
        equipPoints,
        equipId,
        context,
        ProfileType.HYPERSTAT_TWO_PIPE_FCU,
        equip,
        config
    )
    setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equipId, context, equip, config)

    val linearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        addView(viewDischarge)
        addView(supplyView)
    }


    linearLayoutZonePoints.apply {
        addView(viewTitle)
        addView(viewStatus)
        addView(viewPointRow2)
        addView(viewPointRow1)
        addView(linearLayout)
        setPadding(0, 0, 0, 10)
    }


    showDischargeConfigIfRequired(viewDischarge,equipPoints,linearLayout)
    showHSStatSupplyTemp(supplyView, equipPoints, linearLayout, nodeAddress,context)
}

fun getHyperStatPipe2EquipPoints(equipDetails: Equip): HashMap<String, Any> {

    val equip = Domain.getDomainEquip(equipDetails.id) as Pipe2V2Equip
    val pipe2Points = HashMap<String, Any>()
    val pipe2Configuration = getHsConfiguration(equipDetails.id) as Pipe2Configuration
    val fanMode = getHSSelectedFanMode(getHSPipe2FanLevel(pipe2Configuration), equip.fanOpMode.readPriorityVal().toInt())
    val conditioningMode = getSelectedConditioningMode(pipe2Configuration, equip.conditioningMode.readPriorityVal().toInt())
    val possibleConditioningMode = getPossibleConditionMode(pipe2Configuration)
    pipe2Points[StatZoneStatus.CONFIG.name] = pipe2Configuration
    pipe2Points[StatZoneStatus.EQUIP.name] = equip
    pipe2Points[StatZoneStatus.STATUS.name] = equip.equipStatusMessage.readDefaultStrVal()
    pipe2Points[StatZoneStatus.FAN_MODE.name] = fanMode
    pipe2Points[StatZoneStatus.CONDITIONING_MODE.name] = conditioningMode

    pipe2Points[StatZoneStatus.FAN_LEVEL.name] = getHSPipe2FanLevel(pipe2Configuration)
    pipe2Points[StatZoneStatus.CONDITIONING_ENABLED.name] = possibleConditioningMode
    pipe2Points[StatZoneStatus.SUPPLY_TEMP.name] = equip.leavingWaterTemperature.readHisVal()
    if (equip.dischargeAirTemperature.pointExists()) {
        pipe2Points[StatZoneStatus.DISCHARGE_AIRFLOW.name] = equip.dischargeAirTemperature.readHisVal()
    }
    if (equip.humidifierEnable.pointExists()) {
        pipe2Points[StatZoneStatus.TARGET_HUMIDITY.name] = equip.targetHumidifier.readPriorityVal()
    }
    if (equip.dehumidifierEnable.pointExists()) {
        pipe2Points[StatZoneStatus.TARGET_DEHUMIDIFY.name] = equip.targetDehumidifier.readPriorityVal()
    }
    pipe2Points.forEach { CcuLog.i(L.TAG_CCU_HSPIPE2, "Pipe2 data : ${it.key} : ${it.value}") }
    return pipe2Points
}


private fun getAdapterValue(context : Context, itemArray : Int): ArrayAdapter<*> {
    return CustomSpinnerDropDownAdapter( context, R.layout.spinner_zone_item, context.resources.getStringArray(itemArray).toMutableList())
}


private fun setUpConditionFanConfig(
    viewPointRow1: View, equipPoints: HashMap<String, Any>,
    equipId: String, context: Activity,
    profileType: ProfileType, equip: HyperStatEquip,
    configuration: HyperStatConfiguration
) {
    showTextView(R.id.text_point1label, viewPointRow1, context.getString(R.string.conditioning_mode))
    showTextView(R.id.text_point2label, viewPointRow1, context.getString(R.string.fan_mode))

    val conditioningModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val fanModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)

    CCUUiUtil.setSpinnerDropDownColor(conditioningModeSpinner, context)
    CCUUiUtil.setSpinnerDropDownColor(fanModeSpinner, context)

    var conditionMode = 0
    var fanMode = 0
    try {
        conditionMode = equipPoints[StatZoneStatus.CONDITIONING_MODE.name] as Int
        fanMode = equipPoints[StatZoneStatus.FAN_MODE.name] as Int
    } catch (e: Exception) {
        e.printStackTrace()
    }
    var conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode)

    if (equipPoints.containsKey(StatZoneStatus.CONDITIONING_ENABLED.name)) {
        if (equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Cool Only")
                || equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString() == PossibleConditioningMode.COOLONLY.name) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_coolonly)
            if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal)
                conditionMode = conModeAdapter.count - 1

        } else if (equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Heat Only")
                || equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString() == PossibleConditioningMode.HEATONLY.name) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_heatonly)
            if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal) {
                conditionMode = conModeAdapter.count - 1
            }
        }
        if (equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Off")
                || equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString() == PossibleConditioningMode.OFF.name) {
            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_off)
            conditionMode = 0
        }
    }

    conditioningModeSpinner.adapter = conModeAdapter
    if (conditionMode >= 0 && conditionMode < conModeAdapter.count) {
        conditioningModeSpinner.setSelection(conditionMode, false)
    } else {
        conditioningModeSpinner.setSelection(0, false)
        CcuLog.e(L.TAG_CCU_ZONE, "Condition Mode is not in the range falling back to off")
    }

    val fanSpinnerSelectionValues =
            RelayUtil.getFanOptionByLevel((equipPoints[StatZoneStatus.FAN_LEVEL.name] as Int?)!!)
    val fanModeAdapter = getAdapterValue(context, fanSpinnerSelectionValues)

    fanModeSpinner.adapter = fanModeAdapter
    if (fanMode >= 0 && fanMode < fanModeSpinner.adapter.count) {
        fanModeSpinner.setSelection(fanMode, false)
    } else {
        fanModeSpinner.setSelection(0, false)
        CcuLog.e(L.TAG_CCU_ZONE, "Fan Mode is not in the range falling back to off")
    }


    setSpinnerListenerForHyperstat(
        conditioningModeSpinner, StatZoneStatus.CONDITIONING_MODE,
        equipId, profileType, equip, configuration
    )
    setSpinnerListenerForHyperstat(
        fanModeSpinner, StatZoneStatus.FAN_MODE,
        equipId, profileType, equip, configuration
    )
}

private fun setUpHumidifierDeHumidifier(
    viewPointRow2: View, equipPoints: HashMap<*, *>, equipId: String,
    context: Activity, equip: HyperStatEquip, configuration: HyperStatConfiguration
) {
    val textViewLabel3 = viewPointRow2.findViewById<TextView>(R.id.text_point1label)
    val textViewLabel4 = viewPointRow2.findViewById<TextView>(R.id.text_point2label)
    val humiditySpinner = viewPointRow2.findViewById<Spinner>(R.id.spinnerValue1)
    val dehumidifySpinner = viewPointRow2.findViewById<Spinner>(R.id.spinnerValue2)
    val arrayHumidityTargetList = ArrayList<String>()
    for (pos in 1..100) arrayHumidityTargetList.add("$pos%")
    val humidityTargetAdapter = CustomSpinnerDropDownAdapter(
            context, R.layout.spinner_dropdown_item, arrayHumidityTargetList
    )
    humidityTargetAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
    if (equipPoints.containsKey(StatZoneStatus.TARGET_HUMIDITY.name)) {
        textViewLabel3.text = context.getString(R.string.target_min_humidity)
        humiditySpinner.adapter = humidityTargetAdapter
        val targetHumidity = equipPoints[StatZoneStatus.TARGET_HUMIDITY.name] as Double
        humiditySpinner.setSelection(targetHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperstat(
            humiditySpinner,
            StatZoneStatus.TARGET_HUMIDITY,
            equipId,
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
            equip,
            configuration
        )
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column1).visibility = View.GONE
    }
    if (equipPoints.containsKey(StatZoneStatus.TARGET_DEHUMIDIFY.name)) {
        textViewLabel4.text = context.getString(R.string.target_max_humidity)
        dehumidifySpinner.adapter = humidityTargetAdapter
        val targetDeHumidity = equipPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] as Double
        dehumidifySpinner.setSelection(targetDeHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperstat(
            dehumidifySpinner,
            StatZoneStatus.TARGET_DEHUMIDIFY,
            equipId,
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,
            equip,
            configuration
        )
        if (viewPointRow2.findViewById<View>(R.id.lt_column1).visibility == View.GONE) {
            textViewLabel4.setPadding(52, 0, 0, 0)
        }
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column2).visibility = View.GONE
    }
}

@SuppressLint("ClickableViewAccessibility")
private fun setSpinnerListenerForHyperstat(
    view: View, spinnerType: StatZoneStatus, equipId: String,
    profileType: ProfileType, equip: HyperStatEquip,
    configuration: HyperStatConfiguration
) {
    var userClickCheck = false
    // adding this listener to check the user click or not ,because this OnItemSelectedListener will be called at same times when we refresh the screen
    val touchListener = View.OnTouchListener { _, event ->
        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_DOWN) {
            userClickCheck = true
        }
        false
    }
    (view as Spinner).setOnTouchListener(touchListener)

    val onItemSelectedListener: OnItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            when (spinnerType) {
                StatZoneStatus.CONDITIONING_MODE -> handleConditionMode(position, equipId, profileType, userClickCheck, equip, configuration)
                StatZoneStatus.FAN_MODE -> handleFanMode(
                    equipId,
                    position,
                    profileType,
                    userClickCheck,
                    equip,
                    configuration
                )
                StatZoneStatus.TARGET_HUMIDITY -> handleHumidityMode(position, equip)
                StatZoneStatus.TARGET_DEHUMIDIFY -> handleDeHumidityMode(position, equip)
                else -> {}
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    view.onItemSelectedListener = onItemSelectedListener
}

fun handleConditionMode(
        selectedPosition: Int, equipId: String, profileType: ProfileType,
        userClickCheck: Boolean, equip: HyperStatEquip,
        configuration: HyperStatConfiguration
) {
    if (userClickCheck) {
        var actualConditioningMode = -1

        // CPU Profile has combination of conditioning modes
        if (profileType == ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT) {
            actualConditioningMode = getActualConditioningMode(configuration, selectedPosition)
        } else if (profileType == ProfileType.HYPERSTAT_TWO_PIPE_FCU || profileType == ProfileType.HYPERSTAT_HEAT_PUMP_UNIT) {
            // 2 Pipe & HPU profile will be always has all conditioning modes
            actualConditioningMode = StandaloneConditioningMode.values()[selectedPosition].ordinal
        }
        if (actualConditioningMode != -1) {
            updateUserIntentPoints(
                    equipId, equip.conditioningMode, actualConditioningMode.toDouble(),
                    CCUHsApi.getInstance().ccuUserName
            )

        }
    }
}

// Save the fan mode in cache
fun handleFanMode(
    equipId: String,
    selectedPosition: Int,
    profileType: ProfileType,
    userClickCheck: Boolean,
    equip: HyperStatEquip,
    configuration: HyperStatConfiguration
) {
    val cacheStorage = FanModeCacheStorage.getHyperStatFanModeCache()
    fun isFanModeCurrentOccupied(basicSettings: StandaloneFanStage): Boolean {
        return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
    }

    fun updateFanModeCache(actualFanMode: Int) {

        if (selectedPosition != 0 && (selectedPosition % 3 == 0 || isFanModeCurrentOccupied(StandaloneFanStage.values()[actualFanMode])) )
            cacheStorage.saveFanModeInCache(equipId, actualFanMode) // while saving the fan mode, we need to save the actual fan mode instead of selected position
        else
            cacheStorage.removeFanModeFromCache(equipId)
    }

    if (userClickCheck) {
        val actualFanMode: Int = when (profileType) {
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                // TODO Revisit after migrating all profile to clean code
                // Need to do same for all profile once all are migrated to DM
                val fanMode = getHSSelectedFanMode(getCpuFanLevel(configuration as CpuConfiguration), selectedPosition)
                equip.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }

            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
                val fanMode = getHSSelectedFanMode(getHpuFanLevel(configuration as HpuConfiguration), selectedPosition)
                equip.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }

            ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
                val fanMode = getHSSelectedFanMode(getHSPipe2FanLevel(configuration as Pipe2Configuration), selectedPosition)
                equip.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }

            else -> {
                -1
            }
        }
        if (actualFanMode != -1) {
            updateUserIntentPoints(equipId, equip.fanOpMode, actualFanMode.toDouble(),
                    CCUHsApi.getInstance().ccuUserName)
            if (selectedPosition != 0 && selectedPosition % 3 == 0)
                cacheStorage.saveFanModeInCache(equipId, actualFanMode) // while saving the fan mode, we need to save the actual fan mode instead of selected position
            else
                cacheStorage.removeFanModeFromCache(equipId)
        }
    }
}

fun handleHumidityMode(selectedPosition: Int, equip: HyperStatEquip) {
    equip.targetHumidifier.writePointValue((selectedPosition + 1).toDouble())
}

fun handleDeHumidityMode(selectedPosition: Int, equip: HyperStatEquip) {
    equip.targetDehumidifier.writePointValue((selectedPosition + 1).toDouble())
}

fun getHyperStatMonitoringEquipPoints(
    equip: Equip,
    hayStack: CCUHsApi
): java.util.HashMap<Any, Any> {
    val monitoringEquip = Domain.getDomainEquip(equip.id) as MonitoringEquip
    val hyperStatDeviceMap = getHyperStatDevice(equip.group.toInt())
    val hyperStatDevice =  getHyperStatDomainDevice(hyperStatDeviceMap!![Tags.ID].toString(), equip.id)


    val monitoringPoints: HashMap<Any, Any> = HashMap()
    monitoringPoints["Profile"] = "MONITORING"

    val currentTemp = monitoringEquip.currentTemp.readHisVal()
    val tempOffset = monitoringEquip.tempOffset.readHisVal()

    val analogIn1Association = monitoringEquip.analogIn1Association.readDefaultVal()
    val analogIn2Association = monitoringEquip.analogIn2Association.readDefaultVal()
    val thermistor1Association = monitoringEquip.thermistor1Association.readDefaultVal()
    val thermistor2Association = monitoringEquip.thermistor2Association.readDefaultVal()

    val isAnalog1Enable = monitoringEquip.analogIn1Enabled.readDefaultVal() > 0
    val isAnalog2Enable = monitoringEquip.analogIn2Enabled.readDefaultVal() > 0
    val isTh1Enable = monitoringEquip.thermistor1Enabled.readDefaultVal() > 0
    val isTh2Enable = monitoringEquip.thermistor2Enabled.readDefaultVal() > 0

    fun getHistoricalValue(pointRef: String?) = pointRef?.let { hayStack.readHisValById(it) } ?: 0.0

    val an1Val = getHistoricalValue(hyperStatDevice.analog1In.readPoint().pointRef)
    val an2Val = getHistoricalValue(hyperStatDevice.analog2In.readPoint().pointRef)
    val th1Val = getHistoricalValue(hyperStatDevice.th1In.readPoint().pointRef)
    val th2Val = getHistoricalValue(hyperStatDevice.th2In.readPoint().pointRef)

    var size = 0

    monitoringPoints["curtempwithoffset"] = currentTemp

    if (tempOffset != 0.0) {
        monitoringPoints["TemperatureOffset"] = tempOffset
    } else {
        monitoringPoints["TemperatureOffset"] = 0
    }

    if (isAnalog1Enable) {
        size++
        monitoringPoints["iAn1Enable"] = "true"
    } else monitoringPoints["iAn1Enable"] = "false"

    if (isAnalog2Enable) {
        size++
        monitoringPoints["iAn2Enable"] = "true"
    } else monitoringPoints["iAn2Enable"] = "false"

    if (isTh1Enable) {
        size++
        monitoringPoints["isTh1Enable"] = "true"
    } else monitoringPoints["isTh1Enable"] = "false"

    if (isTh2Enable) {
        size++
        monitoringPoints["isTh2Enable"] = "true"
    } else monitoringPoints["isTh2Enable"] = "false"

    monitoringPoints["size"] = size
    if (analogIn1Association >= 0) {
        val selectedSensor = SensorManager.getInstance().externalSensorList[analogIn1Association.toInt()]
        monitoringPoints["Analog1"] = selectedSensor.sensorName
        monitoringPoints["Unit1"] = selectedSensor.engineeringUnit ?: ""
        monitoringPoints["An1Val"] = an1Val
    }

    if (analogIn2Association >= 0) {
        val selectedSensor = SensorManager.getInstance().externalSensorList[analogIn2Association.toInt()]
        monitoringPoints["Analog2"] = selectedSensor.sensorName
        monitoringPoints["Unit2"] = selectedSensor.engineeringUnit ?: ""
        monitoringPoints["An2Val"] = an2Val
    }

    if (thermistor1Association >= 0) {
        val selectedSensor = Thermistor.getThermistorList()[thermistor1Association.toInt()]
        monitoringPoints["Thermistor1"] = selectedSensor.sensorName
        monitoringPoints["Unit3"] = selectedSensor.engineeringUnit ?: ""
        monitoringPoints["Th1Val"] = th1Val
    }
    if (thermistor2Association >= 0) {
        val selectedSensor = Thermistor.getThermistorList()[thermistor2Association.toInt()]
        monitoringPoints["Thermistor2"] = selectedSensor.sensorName
        monitoringPoints["Unit4"] = selectedSensor.engineeringUnit ?: ""
        monitoringPoints["Th2Val"] = th2Val
    }

    return monitoringPoints
}

@SuppressLint("DefaultLocale")
fun showHSStatSupplyTemp(
    dischargeView: View,
    pointsList: HashMap<String, Any>,
    rootView: LinearLayout,
    nodeAddress: String,
    context: Context
) {

    val profile = L.getProfile(nodeAddress.toLong())

    if (profile is HyperStatPipe2Profile) {



        val textView = dischargeView.findViewById<TextView>(R.id.text_discharge_airflow)
        textView.text = context.getString(R.string.supply_water_temperature)
        var supplyTemp = pointsList[StatZoneStatus.SUPPLY_TEMP.name].toString() + " ℉"
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted =
                UnitUtils.fahrenheitToCelsiusTwoDecimal(pointsList[StatZoneStatus.SUPPLY_TEMP.name] as Double)
            supplyTemp = "${String.format("%.2f", converted)} °C"
        }
        showTextView(
            R.id.text_airflowValue,
            dischargeView,
            "$supplyTemp (${profile.supplyDirection()})"
        )
        rootView.removeView(dischargeView)
        rootView.addView(dischargeView)
    } else {
        rootView.removeView(dischargeView)
    }
}





