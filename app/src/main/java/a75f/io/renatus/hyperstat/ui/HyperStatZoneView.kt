package a75f.io.renatus.hyperstat.ui

/**
 * Created by Manjunath K on 25-07-2022.
 */

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getActualConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getFanSelectionMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getHpuActualFanMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getHpuFanSelectionMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getPipe2ActualFanMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getPipe2FanSelectionMode
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getPossibleConditioningModeSettings
import a75f.io.logic.bo.building.hyperstat.common.HSHaystackUtil.Companion.getSelectedConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.HSZoneStatus
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.getHpuSelectedFanLevel
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.getPipe2SelectedFanLevel
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.getSelectedFanLevel
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.isAnyRelayAssociatedToDeHumidifier
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.isAnyRelayAssociatedToHumidifier
import a75f.io.logic.bo.building.hyperstat.common.HyperstatProfileNames
import a75f.io.logic.bo.building.hyperstat.common.PossibleConditioningMode
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuEquip.Companion.getHyperStatEquipRef
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuEquip
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Equip
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.jobs.HyperStatUserIntentHandler.Companion.updateHyperStatUIPoints
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.RelayUtil
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import java.util.Locale


fun loadHyperStatCpuProfile(
    cpuEquipPoints: HashMap<*, *>, inflater: LayoutInflater,
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
        cpuEquipPoints[HSZoneStatus.STATUS.name].toString(),
        HyperstatProfileNames.HSCPU.toUpperCase(Locale.ROOT)
    )
    setUpConditionFanConfig(viewPointRow1, cpuEquipPoints, equipId!!, nodeAddress, context, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT)
    setUpHumidifierDeHumidifier(viewPointRow2, cpuEquipPoints, equipId, context, nodeAddress)
    val textAirflowValue = viewDischarge.findViewById<TextView>(R.id.text_airflowValue)
    if (UnitUtils.isCelsiusTunerAvailableStatus()) {
        textAirflowValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
            cpuEquipPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString()
                .replace("[^0-9\\.]".toRegex(), "").toFloat().toString().toDouble()
        ).toString() + " \u00B0C"
    } else {
        textAirflowValue.text = cpuEquipPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString()
    }
    linearLayoutZonePoints.addView(viewTitle)
    linearLayoutZonePoints.addView(viewStatus)
    linearLayoutZonePoints.addView(viewPointRow2)
    linearLayoutZonePoints.addView(viewPointRow1)
    linearLayoutZonePoints.addView(viewDischarge)
}


private fun setTitleStatusConfig(
    viewTitle: View, viewStatus: View, nodeAddress: String, status: String, profileName: String
) {
    val textViewTitle = viewTitle.findViewById<TextView>(R.id.textProfile)
    textViewTitle.text = "${HyperstatProfileNames.HYPERSTAT} - $profileName ( $nodeAddress )"
    val textViewModule = viewTitle.findViewById<TextView>(R.id.module_status)
    HeartBeatUtil.moduleStatus(textViewModule, nodeAddress)
    val textViewStatus = viewStatus.findViewById<TextView>(R.id.text_status)
    textViewStatus.text = status
    val textViewUpdatedTime = viewStatus.findViewById<TextView>(R.id.last_updated_status)
    textViewUpdatedTime.text = HeartBeatUtil.getLastUpdatedTime(nodeAddress)
}


private fun setUpConditionFanConfig(
    viewPointRow1: View,
    cpuEquipPoints: HashMap<*, *>,
    equipId: String,
    nodeAddress: String,
    context: Activity,
    profileType: ProfileType
) {
    val textViewLabel1 = viewPointRow1.findViewById<TextView>(R.id.text_point1label)
    val textViewLabel2 = viewPointRow1.findViewById<TextView>(R.id.text_point2label)

    textViewLabel1.text = "Conditioning Mode : "
    textViewLabel2.text = "Fan Mode : "

    val conditioningModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val fanModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)

    CCUUiUtil.setSpinnerDropDownColor(conditioningModeSpinner, context)
    CCUUiUtil.setSpinnerDropDownColor(fanModeSpinner, context)

    var conditionMode = 0
    var fanMode = 0
    try {
        conditionMode = cpuEquipPoints[HSZoneStatus.CONDITIONING_MODE.name] as Int
        fanMode = cpuEquipPoints[HSZoneStatus.FAN_MODE.name] as Int
    } catch (e: Exception) {
        e.printStackTrace()
    }
    var conModeAdapter =  getAdapterValue(context, R.array.smartstat_conditionmode)

    if (cpuEquipPoints.containsKey(HSZoneStatus.CONDITIONING_ENABLED.name)) {
        if (cpuEquipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Cool Only")) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_coolonly)
            if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal)
                conditionMode = conModeAdapter.count - 1

        } else if (cpuEquipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Heat Only")) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_heatonly)
            if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal) {
                conditionMode = conModeAdapter.count - 1
            }
        }
        if (cpuEquipPoints[HSZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Off")) {
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
        RelayUtil.getFanOptionByLevel((cpuEquipPoints[HSZoneStatus.FAN_LEVEL.name] as Int?)!!)
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
        conditioningModeSpinner, HSZoneStatus.CONDITIONING_MODE, equipId, nodeAddress,profileType
    )
    setSpinnerListenerForHyperstat(
        fanModeSpinner, HSZoneStatus.FAN_MODE, equipId, nodeAddress,profileType
    )
}

private fun setUpHumidifierDeHumidifier(
    viewPointRow2: View,
    cpuEquipPoints: HashMap<*, *>,
    equipId: String,
    context: Activity,
    nodeAddress: String
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
    if (cpuEquipPoints.containsKey(HSZoneStatus.TARGET_HUMIDITY.name)) {
        textViewLabel3.text = "Target Min Humidity :"
        humiditySpinner.adapter = humidityTargetAdapter
        val targetHumidity = cpuEquipPoints[HSZoneStatus.TARGET_HUMIDITY.name] as Double
        humiditySpinner.setSelection(targetHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperstat(
            humiditySpinner,HSZoneStatus.TARGET_HUMIDITY,equipId, nodeAddress , ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT)
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column1).visibility = View.GONE
    }
    if (cpuEquipPoints.containsKey(HSZoneStatus.TARGET_DEHUMIDIFY.name)) {
        textViewLabel4.text = "Target Max Humidity :"
        dehumidifySpinner.adapter = humidityTargetAdapter
        val targetDeHumidity = cpuEquipPoints[HSZoneStatus.TARGET_DEHUMIDIFY.name] as Double
        dehumidifySpinner.setSelection(targetDeHumidity.toInt() - 1, false)
          setSpinnerListenerForHyperstat(
                    dehumidifySpinner,HSZoneStatus.TARGET_DEHUMIDIFY,equipId, nodeAddress, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT)
        if (viewPointRow2.findViewById<View>(R.id.lt_column1).visibility == View.GONE) {
            textViewLabel4.setPadding(52, 0, 0, 0)
        }
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column2).visibility = View.GONE
    }
}

private fun setSpinnerListenerForHyperstat(
    view: View, spinnerType: HSZoneStatus, equipId: String,
    nodeAddress: String, profileType: ProfileType
) {
    val onItemSelectedListener: OnItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            when (spinnerType) {
                HSZoneStatus.CONDITIONING_MODE -> handleConditionMode(
                    position, equipId, nodeAddress,profileType
                )
                HSZoneStatus.FAN_MODE -> handleFanMode(
                    equipId, position, nodeAddress,profileType
                )
                HSZoneStatus.TARGET_HUMIDITY -> handleHumidityMode(
                    position, equipId
                )
                HSZoneStatus.TARGET_DEHUMIDIFY -> handleDeHumidityMode(
                    position, equipId
                )
                else -> {}
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    (view as Spinner).onItemSelectedListener = onItemSelectedListener
}

private fun handleConditionMode(
    selectedPosition: Int,
    equipId: String,
    nodeAddress: String,
    profileType: ProfileType
) {
    var actualConditioningMode = -1

    // CPU Profile has combination of conditioning modes
    if(profileType == ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT
        || profileType == ProfileType.HYPERSTAT_HEAT_PUMP_UNIT) {
        actualConditioningMode  = getActualConditioningMode(nodeAddress, selectedPosition)
    }
    else if(profileType == ProfileType.HYPERSTAT_TWO_PIPE_FCU) {
        // 2 Pipe profile will be always has all conditioning modes
        actualConditioningMode = StandaloneConditioningMode.values()[selectedPosition].ordinal
    }
    if(actualConditioningMode != -1) {
        updateHyperStatUIPoints(
            equipId, "zone and sp and conditioning and mode", actualConditioningMode.toDouble(),
            CCUHsApi.getInstance().ccuUserName
        )

    }
}

// Save the fan mode in cache
private fun handleFanMode(equipId: String, selectedPosition: Int, nodeAddress: String , profileType: ProfileType) {
    val cacheStorage = FanModeCacheStorage()
    val actualFanMode: Int = when (profileType) {
        ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
            getActualFanMode(nodeAddress, selectedPosition)
        }
        ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
            getHpuActualFanMode(nodeAddress, selectedPosition)
        }
        ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
            getPipe2ActualFanMode(nodeAddress, selectedPosition)
        }
        else -> { -1 }
    }
    if(actualFanMode != -1) {
        updateHyperStatUIPoints(equipId, "zone and sp and fan and operation and mode", actualFanMode.toDouble(),
                CCUHsApi.getInstance().ccuUserName)
        if (selectedPosition != 0 && selectedPosition % 3 == 0)
            cacheStorage.saveFanModeInCache(equipId, selectedPosition)
        else
            cacheStorage.removeFanModeFromCache(equipId)
    }
}

private fun handleHumidityMode(selectedPosition: Int, equipId: String) {
    updateHyperStatUIPoints(
        equipId, "target and humidifier", (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}

private fun handleDeHumidityMode(selectedPosition: Int, equipId: String) {
    updateHyperStatUIPoints(
        equipId, "target and dehumidifier", (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}

fun getHyperStatCPUEquipPoints(equipDetails: Equip): HashMap<String, Any> {

    // All the result points
    val cpuPoints = HashMap<String, Any>()

    // Get points util ref
    val hsHaystackUtil = HSHaystackUtil(
        equipDetails.id, CCUHsApi.getInstance()
    )

    // Get Existing Configuration
    val config = getHyperStatEquipRef(equipDetails.group.toShort()).getConfiguration()
    val equipLiveStatus = hsHaystackUtil.getEquipLiveStatus()

    if (equipLiveStatus != null)
        cpuPoints[HSZoneStatus.STATUS.name] = equipLiveStatus
    else
        cpuPoints[HSZoneStatus.STATUS.name] = "OFF"

    val fanOpModePoint = hsHaystackUtil.readPointPriorityVal("zone and fan and mode and operation")
    CcuLog.i(L.TAG_CCU_HSCPU, "Saved fan mode $fanOpModePoint")
    val fanPosition = getFanSelectionMode(equipDetails.group, fanOpModePoint.toInt())
    CcuLog.i(L.TAG_CCU_HSCPU, "converted fan mode $fanPosition")
    cpuPoints[HSZoneStatus.FAN_MODE.name] = fanPosition
    val conditionModePoint = hsHaystackUtil.readPointPriorityVal(
        "zone and sp and conditioning and mode"
    )
    CcuLog.i(L.TAG_CCU_HSCPU, "Saved conditionModePoint mode $conditionModePoint")
    val selectedConditioningMode = getSelectedConditioningMode(equipDetails.group, conditionModePoint.toInt())
    CcuLog.i(L.TAG_CCU_HSCPU, "converted conditionModePoint mode $selectedConditioningMode")
    cpuPoints[HSZoneStatus.CONDITIONING_MODE.name] = selectedConditioningMode
    val dischargePoint = hsHaystackUtil.readHisVal(
        "sensor and discharge and air and temp"
    )
    cpuPoints[HSZoneStatus.DISCHARGE_AIRFLOW.name] = "$dischargePoint \u2109"
    if (isAnyRelayAssociatedToHumidifier(config)) {
        val targetHumidity = hsHaystackUtil.readPointPriorityVal("target and humidifier")
        cpuPoints[HSZoneStatus.TARGET_HUMIDITY.name] = targetHumidity
    }
    if (isAnyRelayAssociatedToDeHumidifier(config)) {
        val targetDeHumidity = hsHaystackUtil.readPointPriorityVal("target and dehumidifier")
        cpuPoints[HSZoneStatus.TARGET_DEHUMIDIFY.name] = targetDeHumidity
    }
    val fanLevel = getSelectedFanLevel(config)
    cpuPoints[HSZoneStatus.FAN_LEVEL.name] = fanLevel

    // Add conditioning status
    var status: String
    val possibleConditioningMode = getPossibleConditioningModeSettings(equipDetails.group.toInt())
    status = when (possibleConditioningMode) {
        PossibleConditioningMode.OFF -> "Off"
        PossibleConditioningMode.BOTH -> "Both"
        PossibleConditioningMode.COOLONLY -> "Cool Only"
        PossibleConditioningMode.HEATONLY -> "Heat Only"
    }
    cpuPoints[HSZoneStatus.CONDITIONING_ENABLED.name] = status
    cpuPoints.forEach { (s: String, o: Any) ->
        CcuLog.i(L.TAG_CCU_HSCPU, "Config $s : $o")
    }
    return cpuPoints
}


fun loadHyperStatPipe2Profile(
    equipPoints: HashMap<*, *>, inflater: LayoutInflater,
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
     setUpConditionFanConfig(viewPointRow1, equipPoints, equipId!!, nodeAddress, context, ProfileType.HYPERSTAT_TWO_PIPE_FCU)
     setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equipId, context, nodeAddress)
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
    val config = HyperStatPipe2Equip.getHyperStatEquipRef(equipDetails.group.toShort()).getConfiguration()
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
    equipPoints: HashMap<*, *>, inflater: LayoutInflater,
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
    setUpConditionFanConfig(viewPointRow1, equipPoints, equipId!!, nodeAddress, context, ProfileType.HYPERSTAT_HEAT_PUMP_UNIT)
    setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equipId, context, nodeAddress)
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
    val config = HyperStatHpuEquip.getHyperStatEquipRef(equipDetails.group.toShort()).getConfiguration()
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







