package a75f.io.renatus.hyperstatsplit.ui

/**
 * Created by Nick P on 7/10/2023.
 */

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstatsplit.common.*
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualConditioningMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getActualFanMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getFanSelectionMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getPossibleConditioningModeSettings
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getSelectedConditioningMode
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getSelectedFanLevel
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayAssociatedToDeHumidifier
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.isAnyRelayAssociatedToHumidifier
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconEquip.Companion.getHyperStatSplitEquipRef
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.jobs.HyperStatSplitUserIntentHandler.Companion.updateHyperStatSplitUIPoints
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.RelayUtil
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import java.util.*

fun loadHyperStatSplitCpuEconProfile(
    cpuEconEquipPoints: HashMap<*, *>, inflater: LayoutInflater,
    linearLayoutZonePoints: LinearLayout,
    equipId: String?, nodeAddress: String?,
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
        cpuEconEquipPoints[HSSplitZoneStatus.STATUS.name].toString(),
        HyperstatSplitProfileNames.HSSPLIT_CPUECON.toUpperCase(Locale.ROOT)
    )
    setUpConditionFanConfig(viewPointRow1, cpuEconEquipPoints, equipId!!, nodeAddress, context, ProfileType.HYPERSTATSPLIT_CPU)
    setUpHumidifierDeHumidifier(viewPointRow2, cpuEconEquipPoints, equipId, context, nodeAddress)

    if (cpuEconEquipPoints.containsKey(HSSplitZoneStatus.DISCHARGE_AIRFLOW.name)) {
        val textAirflowValue = viewSat.findViewById<TextView>(R.id.text_airflowValue)
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            textAirflowValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                cpuEconEquipPoints[HSSplitZoneStatus.DISCHARGE_AIRFLOW.name].toString()
                    .replace("[^0-9\\.]".toRegex(), "").toFloat().toString().toDouble()
            ).toString() + " \u00B0C"
        } else {
            textAirflowValue.text = cpuEconEquipPoints[HSSplitZoneStatus.DISCHARGE_AIRFLOW.name].toString()
        }
        textAirflowValue.visibility = View.VISIBLE
    } else {
        viewSat.findViewById<TextView>(R.id.text_airflowValue).visibility = View.GONE
    }

    val textMatValue = viewMat.findViewById<TextView>(R.id.text_airflowValue)
    if (UnitUtils.isCelsiusTunerAvailableStatus()) {
        textMatValue.text = UnitUtils.fahrenheitToCelsiusTwoDecimal(
            cpuEconEquipPoints[HSSplitZoneStatus.MIXED_AIR_TEMP.name].toString()
                .replace("[^0-9\\.]".toRegex(), "").toFloat().toString().toDouble()
        ).toString() + " \u00B0C"
    } else {
        textMatValue.text = cpuEconEquipPoints[HSSplitZoneStatus.MIXED_AIR_TEMP.name].toString()
    }
    linearLayoutZonePoints.addView(viewTitle)
    linearLayoutZonePoints.addView(viewStatus)
    linearLayoutZonePoints.addView(viewMat)
    if (cpuEconEquipPoints.containsKey(HSSplitZoneStatus.DISCHARGE_AIRFLOW.name)) linearLayoutZonePoints.addView(viewSat)
    linearLayoutZonePoints.addView(viewPointRow2)
    linearLayoutZonePoints.addView(viewPointRow1)

    // When uncommented, the below lines will create a controls/settings message based on current app state and print it to logs
    // These will be removed later

    // val settingsMessage: HyperSplit.HyperSplitSettingsMessage_t = HyperSplitMessageGenerator.getSettingsMessage("Whatever", nodeAddress.toInt(), equipId, TemperatureMode.DUAL)
    // val settings2Message: HyperSplit.HyperSplitSettingsMessage2_t = HyperSplitMessageGenerator.getSetting2Message(nodeAddress.toInt(), equipId)
    // val settings3Message: HyperSplit.HyperSplitSettingsMessage3_t = HyperSplitMessageGenerator.getSetting3Message(nodeAddress.toInt(), equipId)
    // val controlMessage: HyperSplit.HyperSplitControlsMessage_t = HyperSplitMessageGenerator.getControlMessage(nodeAddress.toInt(), equipId, TemperatureMode.DUAL).build()

}


private fun setTitleStatusConfig(
    viewTitle: View, viewStatus: View, nodeAddress: String, status: String, profileName: String
) {
    val textViewTitle = viewTitle.findViewById<TextView>(R.id.textProfile)
    textViewTitle.text = "${HyperstatSplitProfileNames.HSSPLIT_FULL} - ${HyperstatSplitProfileNames.CPUECON_FULL} ( $nodeAddress )"
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
        conditionMode = cpuEconEquipPoints[HSSplitZoneStatus.CONDITIONING_MODE.name] as Int
        fanMode = cpuEconEquipPoints[HSSplitZoneStatus.FAN_MODE.name] as Int
    } catch (e: Exception) {
        e.printStackTrace()
    }
    var conModeAdapter =  getAdapterValue(context, R.array.smartstat_conditionmode)

    if (cpuEconEquipPoints.containsKey(HSSplitZoneStatus.CONDITIONING_ENABLED.name)) {
        if (cpuEconEquipPoints[HSSplitZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Cool Only")) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_coolonly)
            if (conditionMode == StandaloneConditioningMode.COOL_ONLY.ordinal)
                conditionMode = conModeAdapter.count - 1

        } else if (cpuEconEquipPoints[HSSplitZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Heat Only")) {

            conModeAdapter = getAdapterValue(context, R.array.smartstat_conditionmode_heatonly)
            if (conditionMode == StandaloneConditioningMode.HEAT_ONLY.ordinal) {
                conditionMode = conModeAdapter.count - 1
            }
        }
        if (cpuEconEquipPoints[HSSplitZoneStatus.CONDITIONING_ENABLED.name].toString().contains("Off")) {
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
        RelayUtil.getFanOptionByLevel((cpuEconEquipPoints[HSSplitZoneStatus.FAN_LEVEL.name] as Int?)!!)
    val fanModeAdapter = getAdapterValue(context, fanSpinnerSelectionValues)

    try {
        fanModeSpinner.adapter = fanModeAdapter
        fanModeSpinner.setSelection(fanMode, false)
    } catch (e: Exception) {
        CcuLog.e(
            L.TAG_CCU_ZONE,
            "Exception while setting fan mode: " + e.message + " fan Mode " + fanMode
        )
        e.printStackTrace()
    }
    setSpinnerListenerForHyperstatSplit(
        conditioningModeSpinner, HSSplitZoneStatus.CONDITIONING_MODE, equipId, nodeAddress,profileType
    )
    setSpinnerListenerForHyperstatSplit(
        fanModeSpinner, HSSplitZoneStatus.FAN_MODE, equipId, nodeAddress,profileType
    )
}


private fun setUpHumidifierDeHumidifier(
    viewPointRow2: View,
    cpuEconEquipPoints: HashMap<*, *>,
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
    if (cpuEconEquipPoints.containsKey(HSSplitZoneStatus.TARGET_HUMIDITY.name)) {
        textViewLabel3.text = "Target Min Humidity :"
        humiditySpinner.adapter = humidityTargetAdapter
        val targetHumidity = cpuEconEquipPoints[HSSplitZoneStatus.TARGET_HUMIDITY.name] as Double
        humiditySpinner.setSelection(targetHumidity.toInt() - 1, false)
        setSpinnerListenerForHyperstatSplit(
            humiditySpinner,HSSplitZoneStatus.TARGET_HUMIDITY,equipId, nodeAddress , ProfileType.HYPERSTATSPLIT_CPU)
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column1).visibility = View.GONE
    }
    if (cpuEconEquipPoints.containsKey(HSSplitZoneStatus.TARGET_DEHUMIDIFY.name)) {
        textViewLabel4.text = "Target Max Humidity :"
        dehumidifySpinner.adapter = humidityTargetAdapter
        val targetDeHumidity = cpuEconEquipPoints[HSSplitZoneStatus.TARGET_DEHUMIDIFY.name] as Double
        dehumidifySpinner.setSelection(targetDeHumidity.toInt() - 1, false)
          setSpinnerListenerForHyperstatSplit(
                    dehumidifySpinner,HSSplitZoneStatus.TARGET_DEHUMIDIFY,equipId, nodeAddress, ProfileType.HYPERSTATSPLIT_CPU)
        if (viewPointRow2.findViewById<View>(R.id.lt_column1).visibility == View.GONE) {
            textViewLabel4.setPadding(52, 0, 0, 0)
        }
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column2).visibility = View.GONE
    }
}

private fun setSpinnerListenerForHyperstatSplit(
    view: View, spinnerType: HSSplitZoneStatus, equipId: String,
    nodeAddress: String, profileType: ProfileType
) {
    val onItemSelectedListener: OnItemSelectedListener = object : OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
            when (spinnerType) {
                HSSplitZoneStatus.CONDITIONING_MODE -> handleConditionMode(
                    position, equipId, nodeAddress,profileType
                )
                HSSplitZoneStatus.FAN_MODE -> handleFanMode(
                    equipId, position, nodeAddress,profileType
                )
                HSSplitZoneStatus.TARGET_HUMIDITY -> handleHumidityMode(
                    position, equipId
                )
                HSSplitZoneStatus.TARGET_DEHUMIDIFY -> handleDeHumidityMode(
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

    // CPU/Economizer Profile has combination of conditioning modes
    if(profileType == ProfileType.HYPERSTATSPLIT_CPU) {
        actualConditioningMode  = getActualConditioningMode(nodeAddress, selectedPosition)
    }
    if(actualConditioningMode != -1) {
        updateHyperStatSplitUIPoints(
            equipId, "zone and sp and conditioning and mode",
            actualConditioningMode.toDouble(), CCUHsApi.getInstance().ccuUserName
        )
        val roomRef = HSUtil.getZoneIdFromEquipId(equipId)
        DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, CCUHsApi.getInstance())
    }
}


// Save the fan mode in cache
private fun handleFanMode(equipId: String, selectedPosition: Int, nodeAddress: String , profileType: ProfileType) {
    val cacheStorage = FanModeCacheStorage()
    val actualFanMode: Int = when (profileType) {
        ProfileType.HYPERSTATSPLIT_CPU -> {
            getActualFanMode(nodeAddress, selectedPosition)
        }
        else -> { -1 }
    }
    if(actualFanMode != -1) {
        updateHyperStatSplitUIPoints(equipId, "zone and sp and fan and operation and mode", actualFanMode.toDouble(), CCUHsApi.getInstance().ccuUserName)
        if (selectedPosition != 0 && selectedPosition % 3 == 0)
            cacheStorage.saveFanModeInCache(equipId, selectedPosition)
        else
            cacheStorage.removeFanModeFromCache(equipId)
    }
}

private fun handleHumidityMode(selectedPosition: Int, equipId: String) {
    updateHyperStatSplitUIPoints(
        equipId, "target and humidifier", (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}

private fun handleDeHumidityMode(selectedPosition: Int, equipId: String) {
    updateHyperStatSplitUIPoints(
        equipId, "target and dehumidifier", (selectedPosition + 1).toDouble(), CCUHsApi.getInstance().ccuUserName
    )
}

fun getHyperStatSplitCPUEconEquipPoints(equipDetails: Equip): HashMap<String, Any> {

    // All the result points
    val cpuEconPoints = HashMap<String, Any>()

    // Get points util ref
    val hsSplitHaystackUtil = HSSplitHaystackUtil(
        equipDetails.id, CCUHsApi.getInstance()
    )

    // Get Existing Configuration
    val config = getHyperStatSplitEquipRef(equipDetails.group.toShort()).getConfiguration()
    val equipLiveStatus = hsSplitHaystackUtil.getEquipLiveStatus()

    if (equipLiveStatus != null)
        cpuEconPoints[HSSplitZoneStatus.STATUS.name] = equipLiveStatus
    else
        cpuEconPoints[HSSplitZoneStatus.STATUS.name] = "OFF"

    val fanOpModePoint = hsSplitHaystackUtil.readPointPriorityVal("zone and fan and mode and operation")
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Saved fan mode $fanOpModePoint")
    val fanPosition = getFanSelectionMode(equipDetails.group, fanOpModePoint.toInt())
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "converted fan mode $fanPosition")
    cpuEconPoints[HSSplitZoneStatus.FAN_MODE.name] = fanPosition
    val conditionModePoint = hsSplitHaystackUtil.readPointPriorityVal(
        "zone and sp and conditioning and mode"
    )
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Saved conditionModePoint mode $conditionModePoint")
    val selectedConditioningMode = getSelectedConditioningMode(equipDetails.group, conditionModePoint.toInt())
    CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "converted conditionModePoint mode $selectedConditioningMode")
    cpuEconPoints[HSSplitZoneStatus.CONDITIONING_MODE.name] = selectedConditioningMode

    if (isAnyInputMappedToSupplyAirTemperature(config)) {
        val dischargePoint = hsSplitHaystackUtil.readHisVal(
            "sensor and discharge and air and temp"
        )
        cpuEconPoints[HSSplitZoneStatus.DISCHARGE_AIRFLOW.name] = "$dischargePoint \u2109"
    }

    val mixedAirPoint = hsSplitHaystackUtil.readHisVal(
        "sensor and mixed and air and temp"
    )
    cpuEconPoints[HSSplitZoneStatus.MIXED_AIR_TEMP.name] = "$mixedAirPoint \u2109"
    if (isAnyRelayAssociatedToHumidifier(config)) {
        val targetHumidity = hsSplitHaystackUtil.readPointPriorityVal("target and humidifier")
        cpuEconPoints[HSSplitZoneStatus.TARGET_HUMIDITY.name] = targetHumidity
    }
    if (isAnyRelayAssociatedToDeHumidifier(config)) {
        val targetDeHumidity = hsSplitHaystackUtil.readPointPriorityVal("target and dehumidifier")
        cpuEconPoints[HSSplitZoneStatus.TARGET_DEHUMIDIFY.name] = targetDeHumidity
    }
    val fanLevel = getSelectedFanLevel(config)
    cpuEconPoints[HSSplitZoneStatus.FAN_LEVEL.name] = fanLevel

    // Add conditioning status
    val status: String
    val possibleConditioningMode = getPossibleConditioningModeSettings(equipDetails.group.toInt())
    status = when (possibleConditioningMode) {
        PossibleConditioningMode.OFF -> "Off"
        PossibleConditioningMode.BOTH -> "Both"
        PossibleConditioningMode.COOLONLY -> "Cool Only"
        PossibleConditioningMode.HEATONLY -> "Heat Only"
    }
    cpuEconPoints[HSSplitZoneStatus.CONDITIONING_ENABLED.name] = status
    cpuEconPoints.forEach { (s: String, o: Any) ->
        CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "Config $s : $o")
    }
    return cpuEconPoints
}

private fun isAnyInputMappedToSupplyAirTemperature(config: HyperStatSplitCpuEconConfiguration): Boolean {
    return (
        HyperStatSplitAssociationUtil.isAnyUniversalInMappedToSupplyAirTemperature(
            config.universalIn1State, config.universalIn2State,
            config.universalIn3State, config.universalIn4State,
            config.universalIn5State, config.universalIn6State,
            config.universalIn7State, config.universalIn8State
        ) || HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToSupplyAir(
            config.address0State,
            config.address1State,
            config.address2State
        )
    )
}
private fun getAdapterValue(context : Context, itemArray : Int): ArrayAdapter<*> {
    return CustomSpinnerDropDownAdapter( context, R.layout.spinner_dropdown_item, context.resources.getStringArray(itemArray).toMutableList())
}






