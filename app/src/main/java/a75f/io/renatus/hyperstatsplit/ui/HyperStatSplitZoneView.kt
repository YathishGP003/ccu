package a75f.io.renatus.hyperstatsplit.ui

/**
 * Created by Nick P on 7/10/2023.
 */

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getFanSelectionMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getPossibleConditioningModeSettings
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getSelectedConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getSelectedFanLevel
import a75f.io.logic.bo.building.statprofiles.util.CPUECON_FULL
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HSSPLIT_CPUECON
import a75f.io.logic.bo.building.statprofiles.util.HSSPLIT_FULL
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.StatZoneStatus
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.util.uiutils.updateUserIntentPoints
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.RelayUtil
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter
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

fun loadHyperStatSplitCpuEconProfile(
    cpuEconEquipPoints: HashMap<*, *>, inflater: LayoutInflater,
    linearLayoutZonePoints: LinearLayout,
    hssEquip: HyperStatSplitEquip, nodeAddress: String?,
    context: Activity
) {
    val viewTitle: View = inflater.inflate(R.layout.zones_item_title, null)
    val viewStatus: View = inflater.inflate(R.layout.zones_item_status, null)
    val viewPointRow1: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewPointRow2: View = inflater.inflate(R.layout.zones_item_type2, null)
    val viewSat: View = inflater.inflate(R.layout.zones_item_sat, null)
    val viewMat: View = inflater.inflate(R.layout.zones_item_mat, null)
    setTitleStatusConfig(
        viewTitle, viewStatus, nodeAddress!!,
        cpuEconEquipPoints[StatZoneStatus.STATUS.name].toString(),
        HSSPLIT_CPUECON.toUpperCase(Locale.ROOT)
    )
    setUpConditionFanConfig(viewPointRow1, cpuEconEquipPoints, hssEquip, nodeAddress, context, ProfileType.HYPERSTATSPLIT_CPU)
    setUpHumidifierDeHumidifier(viewPointRow2, cpuEconEquipPoints, hssEquip, context, nodeAddress)

    if (cpuEconEquipPoints.containsKey(StatZoneStatus.DISCHARGE_AIRFLOW.name)) {
        val textAirflowValue = viewSat.findViewById<TextView>(R.id.text_airflowValue)
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            textAirflowValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                cpuEconEquipPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name].toString()
                    .replace("[^0-9\\.]".toRegex(), "").toFloat().toString().toDouble()
            ).toString() + " \u00B0C"
        } else {
            textAirflowValue.text = cpuEconEquipPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name].toString()
        }
        textAirflowValue.visibility = View.VISIBLE
    } else {
        viewSat.findViewById<TextView>(R.id.text_airflowValue).visibility = View.GONE
    }

    val textMatValue = viewMat.findViewById<TextView>(R.id.text_airflowValue)
    if (UnitUtils.isCelsiusTunerAvailableStatus()) {
        textMatValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
            cpuEconEquipPoints[StatZoneStatus.MIXED_AIR_TEMP.name].toString()
                .replace("[^0-9\\.]".toRegex(), "").toFloat().toString().toDouble()
        ).toString() + " \u00B0C"
    } else {
        textMatValue.text = cpuEconEquipPoints[StatZoneStatus.MIXED_AIR_TEMP.name].toString()
    }
    linearLayoutZonePoints.addView(viewTitle)
    linearLayoutZonePoints.addView(viewStatus)
    linearLayoutZonePoints.addView(viewMat)
    if (cpuEconEquipPoints.containsKey(StatZoneStatus.DISCHARGE_AIRFLOW.name)) linearLayoutZonePoints.addView(viewSat)
    linearLayoutZonePoints.addView(viewPointRow2)
    linearLayoutZonePoints.addView(viewPointRow1)

}


private fun setTitleStatusConfig(
    viewTitle: View, viewStatus: View, nodeAddress: String, status: String, profileName: String
) {
    val textViewTitle = viewTitle.findViewById<TextView>(R.id.textProfile)
    textViewTitle.text = "$HSSPLIT_FULL - $CPUECON_FULL ( $nodeAddress )"
    val textViewModule = viewTitle.findViewById<TextView>(R.id.module_status)
    HeartBeatUtil.moduleStatus(textViewModule, nodeAddress)
    val textViewStatus = viewStatus.findViewById<TextView>(R.id.text_status)
    textViewStatus.text = status
    val textViewUpdatedTime = viewStatus.findViewById<TextView>(R.id.last_updated_status)
    textViewUpdatedTime.text = HeartBeatUtil.getLastUpdatedTime(nodeAddress)
}

private fun setUpConditionFanConfig(
    viewPointRow1: View,
    cpuEconEquipPoints: HashMap<*, *>,
    hssEquip: HyperStatSplitEquip,
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
        conditionMode = cpuEconEquipPoints[StatZoneStatus.CONDITIONING_MODE.name] as Int
        fanMode = cpuEconEquipPoints[StatZoneStatus.FAN_MODE.name] as Int
    } catch (e: Exception) {
        e.printStackTrace()
    }
    var conModeAdapter =  getAdapterValue(context, R.array.smartstat_conditionmode)

    if (cpuEconEquipPoints.containsKey(StatZoneStatus.CONDITIONING_ENABLED.name)) {
        if (cpuEconEquipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Cool Only")) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_coolonly)
            if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal)
                conditionMode = conModeAdapter.count - 1

        } else if (cpuEconEquipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Heat Only")) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_heatonly)
            if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal) {
                conditionMode = conModeAdapter.count - 1
            }
        }
        if (cpuEconEquipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Off")) {
            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_off)
            conditionMode = 0
        }
    }
    try {
        conditioningModeSpinner.adapter = conModeAdapter
        conditioningModeSpinner.setSelection(conditionMode, false)
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_ZONE, "Exception : ${e.message}")
    }
    val fanSpinnerSelectionValues =
        RelayUtil.getFanOptionByLevel((cpuEconEquipPoints[StatZoneStatus.FAN_LEVEL.name] as Int?)!!)
    val fanModeAdapter = getAdapterValue(context, fanSpinnerSelectionValues)

    try {
        fanModeSpinner.adapter = fanModeAdapter
        if (fanMode > fanModeAdapter.count - 1) {
            // This is just a hack to fall back to OFF mode if the fan mode is not found in the adapter
            fanMode = 0
            handleFanMode(
                hssEquip, fanMode, profileType, true
            )
        }
        fanModeSpinner.setSelection(fanMode, false)

        setSpinnerListenerForHyperstatSplit(
            fanModeSpinner, StatZoneStatus.FAN_MODE, hssEquip, profileType
        )
    } catch (e: Exception) {
        CcuLog.e(
            L.TAG_CCU_ZONE,
            "Exception while setting fan mode: " + e.message + " fan Mode " + fanMode
        )
        e.printStackTrace()
    }
    setSpinnerListenerForHyperstatSplit(
        conditioningModeSpinner, StatZoneStatus.CONDITIONING_MODE, hssEquip, profileType
    )

}


private fun setUpHumidifierDeHumidifier(
    viewPointRow2: View,
    cpuEconEquipPoints: HashMap<*, *>,
    hssEquip: HyperStatSplitEquip,
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
    if (cpuEconEquipPoints.containsKey(StatZoneStatus.TARGET_HUMIDITY.name)) {
        textViewLabel3.text = "Target Min Humidity :"
        humiditySpinner.adapter = humidityTargetAdapter
        val targetHumidity = cpuEconEquipPoints[StatZoneStatus.TARGET_HUMIDITY.name] as Double
        humiditySpinner.setSelection(targetHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperstatSplit(
            humiditySpinner, StatZoneStatus.TARGET_HUMIDITY, hssEquip, ProfileType.HYPERSTATSPLIT_CPU)
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column1).visibility = View.GONE
    }
    if (cpuEconEquipPoints.containsKey(StatZoneStatus.TARGET_DEHUMIDIFY.name)) {
        textViewLabel4.text = "Target Max Humidity :"
        dehumidifySpinner.adapter = humidityTargetAdapter
        val targetDeHumidity = cpuEconEquipPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] as Double
        dehumidifySpinner.setSelection(targetDeHumidity.toInt() - 1, false)
          setSpinnerListenerForHyperstatSplit(
                    dehumidifySpinner, StatZoneStatus.TARGET_DEHUMIDIFY, hssEquip, ProfileType.HYPERSTATSPLIT_CPU)
        if (viewPointRow2.findViewById<View>(R.id.lt_column1).visibility == View.GONE) {
            textViewLabel4.setPadding(52, 0, 0, 0)
        }
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column2).visibility = View.GONE
    }
}

private fun setSpinnerListenerForHyperstatSplit(
    view: View, spinnerType: StatZoneStatus, hssEquip: HyperStatSplitEquip, profileType: ProfileType
) {
    var userClickCheck = false
    // adding this listener to check the user click or not ,because this OnItemSelectedListener will be called at same times when we refresh the screen
    val touchListener = View.OnTouchListener { v, event ->
        if (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_DOWN) {

            userClickCheck = true
        }
        false
    }
    (view as Spinner).setOnTouchListener(touchListener)

    val onItemSelectedListener: OnItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            when (spinnerType) {
                StatZoneStatus.CONDITIONING_MODE -> handleConditionMode(
                    position, hssEquip, profileType ,userClickCheck
                )
                StatZoneStatus.FAN_MODE -> handleFanMode(
                    hssEquip, position, profileType,userClickCheck
                )
                StatZoneStatus.TARGET_HUMIDITY -> handleHumidityMode(
                    hssEquip, position
                )
                StatZoneStatus.TARGET_DEHUMIDIFY -> handleDeHumidityMode(
                    hssEquip, position
                )
                else -> {}
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    view.onItemSelectedListener = onItemSelectedListener
}

private fun handleConditionMode(
    selectedPosition: Int,
    hssEquip: HyperStatSplitEquip,
    profileType: ProfileType,
    userClickCheck: Boolean
) {
    if(userClickCheck) {
        var actualConditioningMode = -1

        // CPU/Economizer Profile has combination of conditioning modes
        if(profileType == ProfileType.HYPERSTATSPLIT_CPU) {
            actualConditioningMode = getActualConditioningMode(hssEquip, selectedPosition)
        }
        if(actualConditioningMode != -1) {
            updateUserIntentPoints(
                hssEquip.getId(), hssEquip.conditioningMode,
                actualConditioningMode.toDouble(), CCUHsApi.getInstance().ccuUserName
            )
            val roomRef = HSUtil.getZoneIdFromEquipId(hssEquip.getId())
            DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, CCUHsApi.getInstance())
        }
    }
}


// Save the fan mode in cache
private fun handleFanMode(hssEquip: HyperStatSplitEquip, selectedPosition: Int, profileType: ProfileType ,userClickCheck: Boolean

) {
    if (userClickCheck) {
        val cacheStorage = FanModeCacheStorage.getHyperStatSplitFanModeCache()
        val actualFanMode: Int = when (profileType) {
            ProfileType.HYPERSTATSPLIT_CPU -> {
                getActualFanMode(hssEquip, selectedPosition)
            }
            else -> { -1 }
        }
        if (actualFanMode != -1) {
            updateUserIntentPoints(
                hssEquip.getId(), hssEquip.fanOpMode,
                actualFanMode.toDouble(), CCUHsApi.getInstance().ccuUserName)
            if (selectedPosition != 0 && (selectedPosition % 3 == 0 || isFanModeCurrentOccupied(StandaloneFanStage.values()[actualFanMode])) )
                cacheStorage.saveFanModeInCache(hssEquip.getId(), actualFanMode)
            else
                cacheStorage.removeFanModeFromCache(hssEquip.getId())
        }
    }
}
private fun isFanModeCurrentOccupied(basicSettings: StandaloneFanStage): Boolean {
    return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
}
private fun handleHumidityMode(equip: HyperStatSplitEquip, selectedPosition: Int) {
    updateUserIntentPoints(
            equip.equipRef, equip.targetHumidifier, (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}

private fun handleDeHumidityMode(equip: HyperStatSplitEquip, selectedPosition: Int) {
    updateUserIntentPoints(
        equip.equipRef, equip.targetDehumidifier, (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}

fun getHyperStatSplitCPUEconEquipPoints(equipDetails: Equip): HashMap<String, Any> {

    // All the result points
    val cpuEconPoints = HashMap<String, Any>()

    // Get points util ref
    val hssEquip = HyperStatSplitEquip(equipDetails.id.toString())

    val equipLiveStatus = hssEquip.equipStatusMessage.readDefaultStrVal()

    if (equipLiveStatus != null)
        cpuEconPoints[StatZoneStatus.STATUS.name] = equipLiveStatus
    else
        cpuEconPoints[StatZoneStatus.STATUS.name] = "OFF"

    val fanOpModePoint = hssEquip.fanOpMode.readPriorityVal()
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Saved fan mode $fanOpModePoint")
    val fanPosition = getFanSelectionMode(hssEquip, fanOpModePoint.toInt())
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "converted fan mode $fanPosition")
    cpuEconPoints[StatZoneStatus.FAN_MODE.name] = fanPosition
    val conditionModePoint = hssEquip.conditioningMode.readPriorityVal()
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Saved conditionModePoint mode $conditionModePoint")
    val selectedConditioningMode = getSelectedConditioningMode(hssEquip, conditionModePoint.toInt())
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "converted conditionModePoint mode $selectedConditioningMode")
    cpuEconPoints[StatZoneStatus.CONDITIONING_MODE.name] = selectedConditioningMode

    if (hssEquip.dischargeAirTemperature.pointExists()) {
        val dischargePoint = hssEquip.dischargeAirTemperature.readHisVal()
        cpuEconPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name] = "$dischargePoint \u2109"
    }

    val mixedAirPoint = hssEquip.mixedAirTemperature.readHisVal()
    cpuEconPoints[StatZoneStatus.MIXED_AIR_TEMP.name] = "$mixedAirPoint \u2109"
    if (hssEquip.humidifierEnable.pointExists()) {
        val targetHumidity = hssEquip.targetHumidifier.readPriorityVal()
        cpuEconPoints[StatZoneStatus.TARGET_HUMIDITY.name] = targetHumidity
    }
    if (hssEquip.dehumidifierEnable.pointExists()) {
        val targetDeHumidity = hssEquip.targetDehumidifier.readPriorityVal()
        cpuEconPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] = targetDeHumidity
    }
    val fanLevel = getSelectedFanLevel(hssEquip)
    cpuEconPoints[StatZoneStatus.FAN_LEVEL.name] = fanLevel

    // Add conditioning status
    val status: String
    val possibleConditioningMode = getPossibleConditioningModeSettings(hssEquip)
    status = when (possibleConditioningMode) {
        PossibleConditioningMode.OFF -> "Off"
        PossibleConditioningMode.BOTH -> "Both"
        PossibleConditioningMode.COOLONLY -> "Cool Only"
        PossibleConditioningMode.HEATONLY -> "Heat Only"
    }
    cpuEconPoints[StatZoneStatus.CONDITIONING_ENABLED.name] = status
    cpuEconPoints.forEach { (s: String, o: Any) ->
        CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Config $s : $o")
    }
    return cpuEconPoints
}

private fun getAdapterValue(context : Context, itemArray : Int): ArrayAdapter<*> {
    return CustomSpinnerDropDownAdapter( context, R.layout.spinner_dropdown_item, context.resources.getStringArray(itemArray).toMutableList())
}






