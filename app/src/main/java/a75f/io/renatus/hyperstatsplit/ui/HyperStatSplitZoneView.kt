package a75f.io.renatus.hyperstatsplit.ui

/**
 * Created by Nick P on 7/10/2023.
 */

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.Pipe2UVEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getFanSelectionMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getHssProfileConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getSelectedConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getHssProfileFanLevel
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UnitVentilatorProfile
import a75f.io.logic.bo.building.statprofiles.util.CPUECON_FULL
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.HSSPLIT_FULL
import a75f.io.logic.bo.building.statprofiles.util.PIPE2_ECON
import a75f.io.logic.bo.building.statprofiles.util.PIPE4_ECON
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.StatZoneStatus
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.util.DesiredTempDisplayMode
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
import androidx.core.view.isGone
import java.util.Locale

@SuppressLint("SetTextI18n")
fun loadHyperStatSplitProfile(
    equipPoints: HashMap<*, *>, inflater: LayoutInflater,
    linearLayoutZonePoints: LinearLayout,
    equip: HyperStatSplitEquip, nodeAddress: String?,
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
        equipPoints[StatZoneStatus.STATUS.name].toString(),
        equipPoints[StatZoneStatus.PROFILE_NAME.name].toString().uppercase(Locale.getDefault())
    )
    setUpConditionFanConfig(
        viewPointRow1,
        equipPoints,
        equip,
        context)
    setUpHumidifierDeHumidifier(viewPointRow2, equipPoints, equip, context)

    if (equipPoints.containsKey(StatZoneStatus.DISCHARGE_AIRFLOW.name)) {
        val textViewSat = viewSat.findViewById<TextView>(R.id.text_discharge_airflow)

        if(ProfileType.valueOf(equipPoints[StatZoneStatus.PROFILE_NAME.name].toString()) == ProfileType.HYPERSTATSPLIT_4PIPE_UV) {
            textViewSat.text = "Discharge Air Temp :"
        }

        val textAirflowValue = viewSat.findViewById<TextView>(R.id.text_airflowValue)
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            textAirflowValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                equipPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name].toString()
                    .replace("[^0-9.]".toRegex(), "").toFloat().toString().toDouble()
            ).toString() + " \u00B0C"
        } else {
            textAirflowValue.text = equipPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name].toString() + "°F"
        }
        textAirflowValue.visibility = View.VISIBLE
    } else {
        viewSat.findViewById<TextView>(R.id.text_airflowValue).visibility = View.GONE
    }

   //2pipe UV
    val textAirflowValue = viewMat.findViewById<TextView>(R.id.text_airflowValue)
    val textViewMat = viewMat.findViewById<TextView>(R.id.text_discharge_airflow)
    if (equipPoints.containsKey(StatZoneStatus.SUPPLY_TEMP.name)) {
        val profile = L.getProfile(nodeAddress.toLong())
        var supplyWaterTempVal = equipPoints[StatZoneStatus.SUPPLY_TEMP.name].toString() + "°F"
            .replace("[^0-9.]".toRegex(), "")
        if (ProfileType.valueOf(equipPoints[StatZoneStatus.PROFILE_NAME.name].toString()) == ProfileType.HYPERSTATSPLIT_2PIPE_UV) {
            textViewMat.text = "Supply Water Temperature :"
        }
        viewMat.visibility = View.VISIBLE
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            supplyWaterTempVal = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                equipPoints[StatZoneStatus.SUPPLY_TEMP.name].toString()
                    .replace("[^0-9.]".toRegex(), "").toFloat().toString().toDouble()
            ).toString() + " \u00B0C"
        }
        textAirflowValue.text = " $supplyWaterTempVal (${(profile as Pipe2UnitVentilatorProfile).supplyDirection()})"
    }
    ///hssplit
    else if(equipPoints.containsKey(StatZoneStatus.MIXED_AIR_TEMP.name)) {
        viewMat.visibility = View.VISIBLE
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            textAirflowValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                equipPoints[StatZoneStatus.MIXED_AIR_TEMP.name].toString()
                    .replace("[^0-9.]".toRegex(), "").toFloat().toString().toDouble()
            ).toString() + " \u00B0C"
        } else {
            textAirflowValue.text = equipPoints[StatZoneStatus.MIXED_AIR_TEMP.name].toString() + "°F"
        }
    }
    else {
        viewMat.visibility = View.GONE
    }

    linearLayoutZonePoints.addView(viewTitle)
    linearLayoutZonePoints.addView(viewStatus)
    linearLayoutZonePoints.addView(viewMat)
    if (equipPoints.containsKey(StatZoneStatus.DISCHARGE_AIRFLOW.name)) linearLayoutZonePoints.addView(viewSat)
    linearLayoutZonePoints.addView(viewPointRow2)
    linearLayoutZonePoints.addView(viewPointRow1)

}


@SuppressLint("SetTextI18n")
private fun setTitleStatusConfig(
    viewTitle: View, viewStatus: View, nodeAddress: String, status: String, profileName: String
) {
    val profileDescName = when (profileName) {
        ProfileType.HYPERSTATSPLIT_CPU.name -> CPUECON_FULL
        ProfileType.HYPERSTATSPLIT_4PIPE_UV.name -> PIPE4_ECON
        ProfileType.HYPERSTATSPLIT_2PIPE_UV.name -> PIPE2_ECON
        else -> profileName
    }
    val textViewTitle = viewTitle.findViewById<TextView>(R.id.textProfile)
    textViewTitle.text = "$HSSPLIT_FULL - $profileDescName ( $nodeAddress )"
    val textViewModule = viewTitle.findViewById<TextView>(R.id.module_status)
    HeartBeatUtil.moduleStatus(textViewModule, nodeAddress)
    val textViewStatus = viewStatus.findViewById<TextView>(R.id.text_status)
    textViewStatus.text = status
    val textViewUpdatedTime = viewStatus.findViewById<TextView>(R.id.last_updated_status)
    textViewUpdatedTime.text = HeartBeatUtil.getLastUpdatedTime(nodeAddress)
}

@SuppressLint("SetTextI18n")
private fun setUpConditionFanConfig(
    viewPointRow1: View,
    equipPoints: HashMap<*, *>,
    equip: HyperStatSplitEquip,
    context: Activity,
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
        conditionMode = equipPoints[StatZoneStatus.CONDITIONING_MODE.name] as Int
        fanMode = equipPoints[StatZoneStatus.FAN_MODE.name] as Int
    } catch (e: Exception) {
        e.printStackTrace()
    }
    var conModeAdapter =  getAdapterValue(context, R.array.smartstat_conditionmode)

    if (equipPoints.containsKey(StatZoneStatus.CONDITIONING_ENABLED.name)) {
        if (equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains(PossibleConditioningMode.COOLONLY.name)) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_coolonly)
            if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal)
                conditionMode = conModeAdapter.count - 1

        } else if (equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains(PossibleConditioningMode.HEATONLY.name)) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_heatonly)
            if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal) {
                conditionMode = conModeAdapter.count - 1
            }
        }
        if (equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name].toString().contains(PossibleConditioningMode.OFF.name)) {
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
        RelayUtil.getFanOptionByLevel((equipPoints[StatZoneStatus.FAN_LEVEL.name] as Int?)!!)
    val fanModeAdapter = getAdapterValue(context, fanSpinnerSelectionValues)

    try {
        fanModeSpinner.adapter = fanModeAdapter
        if (fanMode > fanModeAdapter.count - 1) {
            // This is just a hack to fall back to OFF mode if the fan mode is not found in the adapter
            fanMode = 0
            handleFanMode(
                equip, fanMode, true
            )
        }
        fanModeSpinner.setSelection(fanMode, false)

        setSpinnerListenerForHyperStatSplit(
            fanModeSpinner, StatZoneStatus.FAN_MODE, equip
        )
    } catch (e: Exception) {
        CcuLog.e(
            L.TAG_CCU_ZONE,
            "Exception while setting fan mode: " + e.message + " fan Mode " + fanMode
        )
        e.printStackTrace()
    }
    setSpinnerListenerForHyperStatSplit(
        conditioningModeSpinner, StatZoneStatus.CONDITIONING_MODE, equip
    )

}


@SuppressLint("SetTextI18n")
private fun setUpHumidifierDeHumidifier(
    viewPointRow2: View,
    equipPoints: HashMap<*, *>,
    equip: HyperStatSplitEquip,
    context: Activity
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
        textViewLabel3.text = "Target Min Humidity :"
        humiditySpinner.adapter = humidityTargetAdapter
        val targetHumidity = equipPoints[StatZoneStatus.TARGET_HUMIDITY.name] as Double
        humiditySpinner.setSelection(targetHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperStatSplit(
            humiditySpinner, StatZoneStatus.TARGET_HUMIDITY, equip)
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column1).visibility = View.GONE
    }
    if (equipPoints.containsKey(StatZoneStatus.TARGET_DEHUMIDIFY.name)) {
        textViewLabel4.text = "Target Max Humidity :"
        dehumidifySpinner.adapter = humidityTargetAdapter
        val targetDeHumidity = equipPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] as Double
        dehumidifySpinner.setSelection(targetDeHumidity.toInt() - 1, false)
          setSpinnerListenerForHyperStatSplit(
                    dehumidifySpinner, StatZoneStatus.TARGET_DEHUMIDIFY, equip)
        if (viewPointRow2.findViewById<View>(R.id.lt_column1).isGone) {
            textViewLabel4.setPadding(52, 0, 0, 0)
        }
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column2).visibility = View.GONE
    }
}

@SuppressLint("ClickableViewAccessibility")
private fun setSpinnerListenerForHyperStatSplit(
    view: View, spinnerType: StatZoneStatus, equip: HyperStatSplitEquip
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
                StatZoneStatus.CONDITIONING_MODE -> handleConditionMode(
                    position, equip, userClickCheck
                )
                StatZoneStatus.FAN_MODE -> handleFanMode(
                    equip, position, userClickCheck
                )
                StatZoneStatus.TARGET_HUMIDITY -> handleHumidityMode(
                    equip, position
                )
                StatZoneStatus.TARGET_DEHUMIDIFY -> handleDeHumidityMode(
                    equip, position
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
    equip: HyperStatSplitEquip,
    userClickCheck: Boolean
) {
    if(userClickCheck) {

        // CPU/Economizer Profile has combination of conditioning modes
        val actualConditioningMode: Int = getActualConditioningMode(equip, selectedPosition)

        if(actualConditioningMode != -1) {
            updateUserIntentPoints(
                equip.getId(), equip.conditioningMode,
                actualConditioningMode.toDouble(), CCUHsApi.getInstance().ccuUserName
            )
            val roomRef = HSUtil.getZoneIdFromEquipId(equip.getId())
            DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, CCUHsApi.getInstance())
        }
    }
}


// Save the fan mode in cache
private fun handleFanMode(
    equip: HyperStatSplitEquip, selectedPosition: Int, userClickCheck: Boolean

) {
    if (userClickCheck) {
        val cacheStorage = FanModeCacheStorage.getHyperStatSplitFanModeCache()
        val actualFanMode: Int = getActualFanMode(equip, selectedPosition)
        if (actualFanMode != -1) {
            updateUserIntentPoints(
                equip.getId(), equip.fanOpMode,
                actualFanMode.toDouble(), CCUHsApi.getInstance().ccuUserName)
            if (selectedPosition != 0 && (selectedPosition % 3 == 0 || isFanModeCurrentOccupied(StandaloneFanStage.values()[actualFanMode])) )
                cacheStorage.saveFanModeInCache(equip.getId(), actualFanMode)
            else
                cacheStorage.removeFanModeFromCache(equip.getId())
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

fun getHyperStatSplitProfileEquipPoints(equipMap: Equip, profileType: ProfileType): HashMap<String, Any> {

    val equipPoints = HashMap<String, Any>()

    val equip = Domain.getEquip(equipMap.id) as HyperStatSplitEquip
    val config = getSplitConfiguration(equipMap.id) as HyperStatSplitConfiguration
    when(profileType){

        ProfileType.HYPERSTATSPLIT_CPU->{

            if(equip.dischargeAirTemperature.pointExists()){
                equipPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name] = "${equip.dischargeAirTemperature.readHisVal()} \u2109"
            }
            equipPoints[StatZoneStatus.MIXED_AIR_TEMP.name] = "${equip.mixedAirTemperature.readHisVal()} \u2109"
        }

        ProfileType.HYPERSTATSPLIT_4PIPE_UV->{
        if(equip.dischargeAirTemperature.pointExists()){
            equipPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name] = "${equip.dischargeAirTemperature.readHisVal()} \u2109"
        }
        }

        ProfileType.HYPERSTATSPLIT_2PIPE_UV->{

            if((equip as Pipe2UVEquip).leavingWaterTemperature.pointExists()) {
                equipPoints[StatZoneStatus.SUPPLY_TEMP.name] = "${equip.leavingWaterTemperature.readHisVal()} \u2109"
            }

        }
        else->{}

    }
    // common for all the Hss profiles
    if(equip.humidifierEnable.pointExists()){
        equipPoints[StatZoneStatus.TARGET_HUMIDITY.name] = equip.targetHumidifier.readHisVal()
    }

    if (equip.dehumidifierEnable.pointExists()){
        equipPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] = equip.targetDehumidifier.readHisVal()
    }

    equipPoints[StatZoneStatus.EQUIP.name] = equip
    equipPoints[StatZoneStatus.STATUS.name] = equip.equipStatusMessage.readDefaultStrVal()
    equipPoints[StatZoneStatus.FAN_MODE.name] = getFanSelectionMode(equip, equip.fanOpMode.readPriorityVal().toInt())
    equipPoints[StatZoneStatus.CONDITIONING_MODE.name] = getSelectedConditioningMode(equip.conditioningMode.readPriorityVal().toInt(),config)
    equipPoints[StatZoneStatus.FAN_LEVEL.name] = getHssProfileFanLevel(equip)
    equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name] = getHssProfileConditioningMode( config)
    equipPoints[StatZoneStatus.PROFILE_NAME.name] = profileType.name

    equipPoints.forEach { (s: String, o: Any) ->
        CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Config $s : $o")
    }

    return equipPoints
}

private fun getAdapterValue(context : Context, itemArray : Int): ArrayAdapter<*> {
    return CustomSpinnerDropDownAdapter( context, R.layout.spinner_dropdown_item, context.resources.getStringArray(itemArray).toMutableList())
}






