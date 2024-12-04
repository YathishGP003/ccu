package a75f.io.renatus.hyperstat.ui

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
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.Thermistor
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getHpuActualFanMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getHpuFanSelectionMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getPipe2ActualFanMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getPipe2FanSelectionMode
import a75f.io.logic.bo.building.hyperstat.common.HSZoneStatus
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.getHpuSelectedFanLevel
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.getPipe2SelectedFanLevel
import a75f.io.logic.bo.building.hyperstat.common.HyperstatProfileNames
import a75f.io.logic.bo.building.hyperstat.common.PossibleConditioningMode
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuEquipToBeDeleted
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2EquipToBeDeleted
import a75f.io.logic.bo.building.hyperstat.profiles.util.getActualConditioningMode
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.util.getCpuFanLevel
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPossibleConditionMode
import a75f.io.logic.bo.building.hyperstat.profiles.util.getSelectedConditioningMode
import a75f.io.logic.bo.building.hyperstat.profiles.util.getSelectedFanMode
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.logic.bo.building.sensors.SensorManager
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.jobs.HyperStatUserIntentHandler.Companion.updateHyperStatUIPoints
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
    showTextView(R.id.textProfile, viewTitle, "${HyperstatProfileNames.HYPERSTAT} - $profileName ( $nodeAddress )")
    showTextView(R.id.text_status, viewStatus, status)
    showTextView(R.id.last_updated_status, viewStatus, HeartBeatUtil.getLastUpdatedTime(nodeAddress))
}

@SuppressLint("DefaultLocale")
private fun showDischargeConfigIfRequired(dischargeView: View, pointsList: HashMap<String,Any>, rootView: LinearLayout ) {
    if (pointsList.containsKey(HSZoneStatus.DISCHARGE_AIRFLOW.name)) {
        var dischargeValue = pointsList[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString() + " ℉"
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted = UnitUtils.fahrenheitToCelsiusTwoDecimal(pointsList[HSZoneStatus.DISCHARGE_AIRFLOW.name] as Double)
            dischargeValue = "${String.format("%.2f", converted)} °C"
        }
        showTextView(R.id.text_airflowValue, dischargeView, dischargeValue)
        rootView.addView(dischargeView)
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
    val config = cpuV2EquipPoints[HSZoneStatus.CONFIG.name]  as CpuConfiguration
    val equip = cpuV2EquipPoints[HSZoneStatus.EQUIP.name]  as CpuV2Equip
    setTitleStatusConfig(viewTitle, viewStatus, nodeAddress, cpuV2EquipPoints[HSZoneStatus.STATUS.name].toString(),
            HyperstatProfileNames.HSCPU.uppercase(Locale.ROOT))
    setUpConditionFanConfig(viewPointRow1, cpuV2EquipPoints, equipId!!, nodeAddress, context, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT, equip, config)
    setUpHumidifierDeHumidifier(viewPointRow2, cpuV2EquipPoints, equipId, context, nodeAddress, equip, config)

    linearLayoutZonePoints.apply {
        addView(viewTitle)
        addView(viewStatus)
        addView(viewPointRow2)
        addView(viewPointRow1)
    }
    showDischargeConfigIfRequired(viewDischarge,cpuV2EquipPoints,linearLayoutZonePoints)
}

fun getHyperStatCpuDetails(equipDetails: Equip): HashMap<String, Any> {
    val equip = Domain.getDomainEquip(equipDetails.id) as CpuV2Equip
    val cpuPoints = HashMap<String, Any>()
    val cpuConfiguration = getConfiguration(equipDetails.id) as CpuConfiguration
    val fanMode = getSelectedFanMode(getCpuFanLevel(cpuConfiguration), equip.fanOpMode.readPriorityVal().toInt())
    val conditioningMode = getSelectedConditioningMode(cpuConfiguration, equip.conditioningMode.readPriorityVal().toInt())
    val possibleConditioningMode = getPossibleConditionMode(cpuConfiguration)
    cpuPoints[HSZoneStatus.CONFIG.name] = cpuConfiguration
    cpuPoints[HSZoneStatus.EQUIP.name] = equip
    cpuPoints[HSZoneStatus.STATUS.name] = equip.equipStatusMessage.readDefaultStrVal()
    cpuPoints[HSZoneStatus.FAN_MODE.name] = fanMode
    cpuPoints[HSZoneStatus.CONDITIONING_MODE.name] = conditioningMode
    if (equip.dischargeAirTemperature.pointExists()) {
        cpuPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name] = equip.dischargeAirTemperature.readHisVal()
    }
    cpuPoints[HSZoneStatus.FAN_LEVEL.name] = getCpuFanLevel(cpuConfiguration)
    cpuPoints[HSZoneStatus.CONDITIONING_ENABLED.name] = possibleConditioningMode
    if (equip.humidifierEnable.pointExists()) {
        cpuPoints[HSZoneStatus.TARGET_HUMIDITY.name] = equip.targetHumidifier.readPriorityVal()
    }
    if (equip.dehumidifierEnable.pointExists()) {
        cpuPoints[HSZoneStatus.TARGET_DEHUMIDIFY.name] = equip.targetDehumidifier.readPriorityVal()
    }
    cpuPoints.forEach { CcuLog.i(L.TAG_CCU_HSHST, "CPU data : ${it.key} : ${it.value}") }
    return cpuPoints
}


fun loadHyperStatPipe2Profile(
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
    setTitleStatusConfig(
        viewTitle, viewStatus, nodeAddress!!,
        equipPoints[HSZoneStatus.STATUS.name].toString(),
        " 2 Pipe FCU"
    )
     setUpConditionFanConfig(viewPointRow1, equipPoints, equipId!!, nodeAddress, context, ProfileType.HYPERSTAT_TWO_PIPE_FCU, null,null)
     setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equipId, context, nodeAddress, null, null)
    val textAirflowValue = viewDischarge.findViewById<TextView>(R.id.text_airflowValue)
    if (UnitUtils.isCelsiusTunerAvailableStatus()) {
        textAirflowValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
            equipPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString()
                .replace("[^0-9\\.]".toRegex(), "").toFloat().toString().toDouble()
        ).toString() + " \u00B0C"
    } else {
        textAirflowValue.text = equipPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString()
    }
    linearLayoutZonePoints.addView(viewTitle)
    linearLayoutZonePoints.addView(viewStatus)
    linearLayoutZonePoints.addView(viewPointRow2)
    linearLayoutZonePoints.addView(viewPointRow1)
    linearLayoutZonePoints.addView(viewDischarge)
}

fun getHyperStatPipe2EquipPoints(equipDetails: Equip): HashMap<String, Any> {

    // All the result points
    val pipe2Points = HashMap<String, Any>()

    // Get points util ref
    val hsHaystackUtil = HSHaystackUtil(
        equipDetails.id, CCUHsApi.getInstance()
    )

    // Get Existing Configuration
    val config = HyperStatPipe2EquipToBeDeleted.getHyperStatEquipRef(equipDetails.group.toShort()).getConfiguration()
    val equipLiveStatus = hsHaystackUtil.getEquipLiveStatus()

    if (equipLiveStatus != null)
        pipe2Points[HSZoneStatus.STATUS.name] = equipLiveStatus
    else
        pipe2Points[HSZoneStatus.STATUS.name] = "OFF"

    val fanOpModePoint = hsHaystackUtil.readPointPriorityVal("zone and fan and mode and operation")
    val fanPosition = getPipe2FanSelectionMode(equipDetails.group, fanOpModePoint.toInt())
    pipe2Points[HSZoneStatus.FAN_MODE.name] = fanPosition
    val conditionModePoint = hsHaystackUtil.readPointPriorityVal(
        "zone and sp and conditioning and mode"
    )
    CcuLog.i(L.TAG_CCU_HSCPU, "Saved conditionModePoint mode $conditionModePoint")
    val selectedConditioningMode =  StandaloneConditioningMode.values()[conditionModePoint.toInt()].ordinal
    CcuLog.i(L.TAG_CCU_HSCPU, "converted conditionModePoint mode $selectedConditioningMode")
    pipe2Points[HSZoneStatus.CONDITIONING_MODE.name] = selectedConditioningMode
    val dischargePoint = hsHaystackUtil.readHisVal(
        "sensor and discharge and air and temp"
    )
    pipe2Points[HSZoneStatus.DISCHARGE_AIRFLOW.name] = "$dischargePoint \u2109"
    if (HyperStatAssociationUtil.isAnyPipe2RelayEnabledAssociatedToHumidifier(config)) {
        val targetHumidity = hsHaystackUtil.readPointPriorityVal("target and humidifier and his")
        pipe2Points[HSZoneStatus.TARGET_HUMIDITY.name] = targetHumidity
    }
    if (HyperStatAssociationUtil.isAnyPipe2RelayEnabledAssociatedToDeHumidifier(config)) {
        val targetDeHumidity = hsHaystackUtil.readPointPriorityVal("target and dehumidifier and his")
        pipe2Points[HSZoneStatus.TARGET_DEHUMIDIFY.name] = targetDeHumidity
    }
     val fanLevel = getPipe2SelectedFanLevel(config)
     pipe2Points[HSZoneStatus.FAN_LEVEL.name] = fanLevel

    pipe2Points[HSZoneStatus.CONDITIONING_ENABLED.name] = "Both"
    pipe2Points.forEach { (s: String, o: Any) ->
        CcuLog.i(L.TAG_CCU_HSCPU, "Config $s : $o")
    }
    return pipe2Points
}


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
    setTitleStatusConfig(
        viewTitle, viewStatus, nodeAddress!!,
        equipPoints[HSZoneStatus.STATUS.name].toString(),
        " Heat Pump Unit"
    )
    setUpConditionFanConfig(viewPointRow1, equipPoints, equipId!!, nodeAddress, context, ProfileType.HYPERSTAT_HEAT_PUMP_UNIT, null, null)
    setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equipId, context, nodeAddress, null, null)
    val textAirflowValue = viewDischarge.findViewById<TextView>(R.id.text_airflowValue)
    if (UnitUtils.isCelsiusTunerAvailableStatus()) {
        textAirflowValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
            equipPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString()
                .replace("[^0-9\\.]".toRegex(), "").toFloat().toString().toDouble()
        ).toString() + " \u00B0C"
    } else {
        textAirflowValue.text = equipPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString()
    }
    linearLayoutZonePoints.addView(viewTitle)
    linearLayoutZonePoints.addView(viewStatus)
    linearLayoutZonePoints.addView(viewPointRow2)
    linearLayoutZonePoints.addView(viewPointRow1)
    linearLayoutZonePoints.addView(viewDischarge)
}

fun getHyperStatHpuEquipPoints(equipDetails: Equip): HashMap<String, Any> {

    // All the result points
    val hpuPoints = HashMap<String, Any>()

    // Get points util ref
    val hsHaystackUtil = HSHaystackUtil(
        equipDetails.id, CCUHsApi.getInstance()
    )

    // Get Existing Configuration
    val config = HyperStatHpuEquipToBeDeleted.getHyperStatEquipRef(equipDetails.group.toShort()).getConfiguration()
    val equipLiveStatus = hsHaystackUtil.getEquipLiveStatus()

    if (equipLiveStatus != null)
        hpuPoints[HSZoneStatus.STATUS.name] = equipLiveStatus
    else
        hpuPoints[HSZoneStatus.STATUS.name] = "OFF"

    val fanOpModePoint = hsHaystackUtil.readPointPriorityVal("zone and fan and mode and operation")
    val fanPosition = getHpuFanSelectionMode(equipDetails.group, fanOpModePoint.toInt())
    hpuPoints[HSZoneStatus.FAN_MODE.name] = fanPosition
    val conditionModePoint = hsHaystackUtil.readPointPriorityVal(
        "zone and sp and conditioning and mode"
    )
    CcuLog.i(L.TAG_CCU_HSCPU, "Saved conditionModePoint mode $conditionModePoint")
    val selectedConditioningMode =  StandaloneConditioningMode.values()[conditionModePoint.toInt()].ordinal
    CcuLog.i(L.TAG_CCU_HSCPU, "converted conditionModePoint mode $selectedConditioningMode")
    hpuPoints[HSZoneStatus.CONDITIONING_MODE.name] = selectedConditioningMode
    val dischargePoint = hsHaystackUtil.readHisVal(
        "sensor and discharge and air and temp"
    )
    hpuPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name] = "$dischargePoint \u2109"
    if (HyperStatAssociationUtil.isAnyHpuRelayEnabledAssociatedToHumidifier(config)) {
        val targetHumidity = hsHaystackUtil.readPointPriorityVal("target and humidifier and his")
        hpuPoints[HSZoneStatus.TARGET_HUMIDITY.name] = targetHumidity
    }
    if (HyperStatAssociationUtil.isAnyHpuRelayEnabledAssociatedToDeHumidifier(config)) {
        val targetDeHumidity = hsHaystackUtil.readPointPriorityVal("target and dehumidifier and his")
        hpuPoints[HSZoneStatus.TARGET_DEHUMIDIFY.name] = targetDeHumidity
    }
    val fanLevel = getHpuSelectedFanLevel(config)
    hpuPoints[HSZoneStatus.FAN_LEVEL.name] = fanLevel

    hpuPoints[HSZoneStatus.CONDITIONING_ENABLED.name] = "Both"
    hpuPoints.forEach { (s: String, o: Any) ->
        CcuLog.i(L.TAG_CCU_HSCPU, "Config $s : $o")
    }
    return hpuPoints
}

private fun getAdapterValue(context : Context, itemArray : Int): ArrayAdapter<*> {
    return CustomSpinnerDropDownAdapter( context, R.layout.spinner_zone_item, context.resources.getStringArray(itemArray).toMutableList())
}


private fun setUpConditionFanConfig(
        viewPointRow1: View, equipPoints: HashMap<String, Any>,
        equipId: String, nodeAddress: String,
        context: Activity, profileType: ProfileType,
        equip: HyperStatEquip?, configuration: HyperStatConfiguration?
) {
    showTextView(R.id.text_point1label, viewPointRow1, "Conditioning Mode : ")
    showTextView(R.id.text_point2label, viewPointRow1, "Fan Mode : ")

    val conditioningModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val fanModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)

    CCUUiUtil.setSpinnerDropDownColor(conditioningModeSpinner, context)
    CCUUiUtil.setSpinnerDropDownColor(fanModeSpinner, context)

    var conditionMode = 0
    var fanMode = 0
    try {
        conditionMode = equipPoints[HSZoneStatus.CONDITIONING_MODE.name] as Int
        fanMode = equipPoints[HSZoneStatus.FAN_MODE.name] as Int
    } catch (e: Exception) {
        e.printStackTrace()
    }
    var conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode)

    if (equipPoints.containsKey(HSZoneStatus.CONDITIONING_ENABLED.name)) {
        if (equipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Cool Only")
                || equipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString() == PossibleConditioningMode.COOLONLY.name) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_coolonly)
            if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal)
                conditionMode = conModeAdapter.count - 1

        } else if (equipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Heat Only")
                || equipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString() == PossibleConditioningMode.HEATONLY.name) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_heatonly)
            if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal) {
                conditionMode = conModeAdapter.count - 1
            }
        }
        if (equipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Off")
                || equipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString() == PossibleConditioningMode.OFF.name) {
            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_off)
            conditionMode = 0
        }
    }
    if (conditionMode >= conModeAdapter.count) {
        conditionMode = 0
    }
    try {
        conditioningModeSpinner.adapter = conModeAdapter
        conditioningModeSpinner.setSelection(conditionMode, false)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_ZONE, "Exception : ${e.message}")
    }
    val fanSpinnerSelectionValues =
            RelayUtil.getFanOptionByLevel((equipPoints[HSZoneStatus.FAN_LEVEL.name] as Int?)!!)
    val fanModeAdapter = getAdapterValue(context, fanSpinnerSelectionValues)

    if (fanMode >= fanModeAdapter.count) {
        fanMode = 0
    }
    try {
        fanModeSpinner.adapter = fanModeAdapter
        fanModeSpinner.setSelection(fanMode, false)
    } catch (e: Exception) {
        CcuLog.e(
                L.TAG_CCU_ZONE,
                "Exception while setting fan ode: " + e.message + " fan Mode " + fanMode
        )
        e.printStackTrace()
    }
    setSpinnerListenerForHyperstat(
            conditioningModeSpinner, HSZoneStatus.CONDITIONING_MODE,
            equipId, nodeAddress, profileType, equip, configuration
    )
    setSpinnerListenerForHyperstat(
            fanModeSpinner, HSZoneStatus.FAN_MODE,
            equipId, nodeAddress, profileType, equip, configuration
    )
}

private fun setUpHumidifierDeHumidifier(
        viewPointRow2: View,
        CpuV2EquipPoints: HashMap<*, *>,
        equipId: String,
        context: Activity,
        nodeAddress: String,
        equip: HyperStatEquip?, configuration: HyperStatConfiguration?
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
    if (CpuV2EquipPoints.containsKey(HSZoneStatus.TARGET_HUMIDITY.name)) {
        textViewLabel3.text = "Target Min Humidity :"
        humiditySpinner.adapter = humidityTargetAdapter
        val targetHumidity = CpuV2EquipPoints[HSZoneStatus.TARGET_HUMIDITY.name] as Double
        humiditySpinner.setSelection(targetHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperstat(
                humiditySpinner, HSZoneStatus.TARGET_HUMIDITY, equipId, nodeAddress, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT, equip, configuration)
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column1).visibility = View.GONE
    }
    if (CpuV2EquipPoints.containsKey(HSZoneStatus.TARGET_DEHUMIDIFY.name)) {
        textViewLabel4.text = "Target Max Humidity :"
        dehumidifySpinner.adapter = humidityTargetAdapter
        val targetDeHumidity = CpuV2EquipPoints[HSZoneStatus.TARGET_DEHUMIDIFY.name] as Double
        dehumidifySpinner.setSelection(targetDeHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperstat(
                dehumidifySpinner, HSZoneStatus.TARGET_DEHUMIDIFY, equipId, nodeAddress, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT, equip, configuration)
        if (viewPointRow2.findViewById<View>(R.id.lt_column1).visibility == View.GONE) {
            textViewLabel4.setPadding(52, 0, 0, 0)
        }
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column2).visibility = View.GONE
    }
}

@SuppressLint("ClickableViewAccessibility")
private fun setSpinnerListenerForHyperstat(
        view: View, spinnerType: HSZoneStatus, equipId: String,
        nodeAddress: String, profileType: ProfileType,
        equip: HyperStatEquip?, configuration: HyperStatConfiguration? = null
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
                HSZoneStatus.CONDITIONING_MODE -> handleConditionMode(position, equipId, nodeAddress, profileType, userClickCheck, equip, configuration)
                HSZoneStatus.FAN_MODE -> handleFanMode(equipId, position, nodeAddress, profileType, userClickCheck, equip, configuration)
                HSZoneStatus.TARGET_HUMIDITY -> handleHumidityMode(position, equipId, equip)
                HSZoneStatus.TARGET_DEHUMIDIFY -> handleDeHumidityMode(position, equipId, equip)
                else -> {}
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    view.onItemSelectedListener = onItemSelectedListener
}

private fun handleConditionMode(
        selectedPosition: Int, equipId: String, nodeAddress: String,
        profileType: ProfileType, userClickCheck: Boolean,
        equip: HyperStatEquip? = null, configuration: HyperStatConfiguration?
) {
    if (userClickCheck) {
        var actualConditioningMode = -1

        // CPU Profile has combination of conditioning modes
        if (profileType == ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT
                || profileType == ProfileType.HYPERSTAT_HEAT_PUMP_UNIT) {
            actualConditioningMode = getActualConditioningMode(configuration as CpuConfiguration, selectedPosition)
        } else if (profileType == ProfileType.HYPERSTAT_TWO_PIPE_FCU) {
            // 2 Pipe profile will be always has all conditioning modes
            actualConditioningMode = StandaloneConditioningMode.values()[selectedPosition].ordinal
        }
        if (actualConditioningMode != -1) {
            updateHyperStatUIPoints(
                    equipId, "zone and sp and conditioning and mode", actualConditioningMode.toDouble(),
                    CCUHsApi.getInstance().ccuUserName
            )

        }
    }
}

// Save the fan mode in cache
private fun handleFanMode(equipId: String, selectedPosition: Int, nodeAddress: String, profileType: ProfileType, userClickCheck: Boolean,
                          equip: HyperStatEquip? = null, configuration: HyperStatConfiguration?) {

    fun updateFanModeCache(actualFanMode: Int) {
        val cacheStorage = FanModeCacheStorage()
        if (selectedPosition != 0 && selectedPosition % 3 == 0)
            cacheStorage.saveFanModeInCache(equipId, actualFanMode) // while saving the fan mode, we need to save the actual fan mode instead of selected position
        else
            cacheStorage.removeFanModeFromCache(equipId)
    }

    if (userClickCheck) {
        val cacheStorage = FanModeCacheStorage()
        val actualFanMode: Int = when (profileType) {
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                // TODO Revisit after migrating all profile to clean code
                // Need to do same for all profile once all are migrated to DM
                val fanMode = getSelectedFanMode(getCpuFanLevel(configuration as CpuConfiguration), selectedPosition)
                equip!!.fanOpMode.writePointValue(fanMode.toDouble())
                updateFanModeCache(fanMode)
                -1 // just to avoid compilation error
            }

            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
                getHpuActualFanMode(nodeAddress, selectedPosition)
            }

            ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
                getPipe2ActualFanMode(nodeAddress, selectedPosition)
            }

            else -> {
                -1
            }
        }
        if (actualFanMode != -1) {
            updateHyperStatUIPoints(equipId, "zone and sp and fan and operation and mode", actualFanMode.toDouble(),
                    CCUHsApi.getInstance().ccuUserName)
            if (selectedPosition != 0 && selectedPosition % 3 == 0)
                cacheStorage.saveFanModeInCache(equipId, actualFanMode) // while saving the fan mode, we need to save the actual fan mode instead of selected position
            else
                cacheStorage.removeFanModeFromCache(equipId)
        }
    }
}

private fun handleHumidityMode(
        selectedPosition: Int, equipId: String,
        equip: HyperStatEquip? = null) {
    if (equip != null) {
        equip.targetHumidifier.writePointValue(selectedPosition.toDouble())
    } else {
        updateHyperStatUIPoints(
                equipId, "target and humidifier", (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
        )
    }
}

private fun handleDeHumidityMode(selectedPosition: Int, equipId: String, equip: HyperStatEquip? = null) {
    if (equip != null) {
        equip.targetDehumidifier.writePointValue(selectedPosition.toDouble())
    } else {
        updateHyperStatUIPoints(
                equipId, "target and dehumidifier", (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
        )
    }
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





