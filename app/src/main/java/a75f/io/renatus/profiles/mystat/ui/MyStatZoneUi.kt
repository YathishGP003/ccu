package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.fancoilunit.pipe2.MyStatPipe2Profile
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.StatZoneStatus
import a75f.io.logic.bo.building.statprofiles.util.getMyStatActualConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatCpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatHpuFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPipe2FanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatSelectedConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatSelectedFanMode
import a75f.io.logic.util.uiutils.updateUserIntentPoints
import a75f.io.renatus.R
import a75f.io.renatus.profiles.mystat.CPU
import a75f.io.renatus.profiles.mystat.HPU
import a75f.io.renatus.profiles.mystat.PIPE2
import a75f.io.renatus.profiles.mystat.getMyStatAdapterValue
import a75f.io.renatus.profiles.mystat.setMyStatTitleStatusConfig
import a75f.io.renatus.profiles.mystat.showMyStatDischargeConfigIfRequired
import a75f.io.renatus.profiles.mystat.showMyStatSupplyTemp
import a75f.io.renatus.profiles.mystat.showTextView
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.util.RelayUtil
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import java.util.Locale

/**
 * Created by Manjunath K on 24-01-2025.
 */

private fun setUpConditionFanConfig(
    viewPointRow1: View,
    equipPoints: HashMap<String, Any>,
    equipId: String,
    context: Activity,
    profileType: ProfileType,
    equip: MyStatEquip,
    configuration: MyStatConfiguration
) {

    showTextView(R.id.text_point1label, viewPointRow1, "Conditioning Mode : ")
    showTextView(R.id.text_point2label, viewPointRow1, "Fan Mode : ")

    val conditioningModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val fanModeSpinner = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)

    CCUUiUtil.setSpinnerDropDownColor(conditioningModeSpinner, context)
    CCUUiUtil.setSpinnerDropDownColor(fanModeSpinner, context)

    var conditionMode = 0
    var fanMode = 0

    if (equipPoints.containsKey(StatZoneStatus.CONDITIONING_MODE.name)) {
        conditionMode = equipPoints[StatZoneStatus.CONDITIONING_MODE.name] as Int
    }
    if (equipPoints.containsKey(StatZoneStatus.FAN_MODE.name)) {
        fanMode = equipPoints[StatZoneStatus.FAN_MODE.name] as Int
    }

    var conModeAdapter = getMyStatAdapterValue(context, R.array.smartstat_conditionmode)

    if (equipPoints.containsKey(StatZoneStatus.CONDITIONING_ENABLED.name)) {
        val possibleConditionMode = equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name] as PossibleConditioningMode
        when(possibleConditionMode) {
            PossibleConditioningMode.OFF -> {
                conModeAdapter = getMyStatAdapterValue(context, R.array.smartstat_conditionmode_off)
                conditionMode = 0
            }
            PossibleConditioningMode.COOLONLY -> {
                conModeAdapter = getMyStatAdapterValue(context, R.array.smartstat_conditionmode_coolonly)
            }
            PossibleConditioningMode.HEATONLY -> {
                conModeAdapter = getMyStatAdapterValue(context, R.array.smartstat_conditionmode_heatonly)
            }
            else -> {}
        }
    }

    conditioningModeSpinner.adapter = conModeAdapter
    if (conditionMode >= 0 && conditionMode < conModeAdapter.count) {
        conditioningModeSpinner.setSelection(conditionMode, false)
    } else {
        conditioningModeSpinner.setSelection(0, false)
        CcuLog.e(L.TAG_CCU_ZONE, "Condition Mode is not in the range falling back to off")
    }

    val fanLevel = equipPoints[StatZoneStatus.FAN_LEVEL.name] as Int
    val fanSpinnerSelectionValues = RelayUtil.getFanOptionByLevel(fanLevel)
    fanModeSpinner.adapter = getMyStatAdapterValue(context, fanSpinnerSelectionValues)

    CcuLog.e(L.TAG_CCU_MSHST, "received $fanMode")
    val actualFanMode  = getMyStatSelectedFanMode(fanLevel, fanMode)

    if (actualFanMode >= 0 && actualFanMode < fanModeSpinner.adapter.count) {
        fanModeSpinner.setSelection(actualFanMode, false)
    } else {
        fanModeSpinner.setSelection(0, false)
        CcuLog.e(L.TAG_CCU_ZONE, "Fan Mode is not in the range falling back to off")
    }
    setSpinnerListenerForMyStat(
        conditioningModeSpinner,
        StatZoneStatus.CONDITIONING_MODE,
        equipId,
        profileType,
        equip,
        configuration
    )
    setSpinnerListenerForMyStat(
        fanModeSpinner, StatZoneStatus.FAN_MODE, equipId, profileType, equip, configuration
    )
}

@SuppressLint("ClickableViewAccessibility")
private fun setSpinnerListenerForMyStat(
    view: View,
    spinnerType: StatZoneStatus,
    equipId: String,
    profileType: ProfileType,
    equip: MyStatEquip,
    configuration: MyStatConfiguration
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
                StatZoneStatus.CONDITIONING_MODE -> handleMyStatConditionMode(
                    position, equipId, profileType, userClickCheck, configuration, equip.conditioningMode
                )

                StatZoneStatus.FAN_MODE -> handleMyStatFanMode(
                    equipId, position, profileType, userClickCheck, configuration, equip.fanOpMode
                )

                StatZoneStatus.TARGET_HUMIDITY -> handleMyStatHumidityMode(position, equipId, equip)
                StatZoneStatus.TARGET_DEHUMIDIFY -> handleMyStatDeHumidityMode(position, equipId, equip)

                else -> {}
            }
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    view.onItemSelectedListener = onItemSelectedListener
}


private fun handleMyStatConditionMode(
    selectedPosition: Int,
    equipId: String,
    profileType: ProfileType,
    userClickCheck: Boolean,
    configuration: MyStatConfiguration,
    conditioningMode: Point
) {
    if (userClickCheck) {
        var actualConditioningMode = -1

        // CPU Profile has combination of conditioning modes
        if (profileType == ProfileType.MYSTAT_CPU) {
            actualConditioningMode =
                getMyStatActualConditioningMode(configuration, selectedPosition)
        } else if (profileType == ProfileType.MYSTAT_PIPE2 || profileType == ProfileType.MYSTAT_HPU) {
            // 2 Pipe & HPU profile will be always has all conditioning modes
            actualConditioningMode = StandaloneConditioningMode.values()[selectedPosition].ordinal
        }
        if (actualConditioningMode != -1) {
            updateUserIntentPoints(
                equipId, conditioningMode,
                actualConditioningMode.toDouble(),
                CCUHsApi.getInstance().ccuUserName
            )
        }
    }
}

// Save the fan mode in cache
private fun handleMyStatFanMode(
    equipId: String,
    selectedPosition: Int,
    profileType: ProfileType,
    userClickCheck: Boolean,
    configuration: MyStatConfiguration,
    fanMode: Point
) {

    val cacheStorage = FanModeCacheStorage.getMyStatFanModeCache()
    fun isFanModeCurrentOccupied(position : Int): Boolean {
        val basicSettings = MyStatFanStages.values()[position]
        return (basicSettings == MyStatFanStages.LOW_CUR_OCC || basicSettings == MyStatFanStages.HIGH_CUR_OCC)
    }

    fun updateFanModeCache(actualFanMode: Int) {

        if (selectedPosition != 0 && ( selectedPosition % 3 == 0 || isFanModeCurrentOccupied(selectedPosition))) cacheStorage.saveFanModeInCache(
            equipId, actualFanMode
        ) // while saving the fan mode, we need to save the actual fan mode instead of selected position
        else cacheStorage.removeFanModeFromCache(equipId)
    }
    if (userClickCheck) {
        val actualFanMode: Int = when (profileType) {
            ProfileType.MYSTAT_CPU -> {
                val selectedFanMode = getMyStatSelectedFanMode(
                    getMyStatCpuFanLevel(configuration as MyStatCpuConfiguration), selectedPosition
                )
                updateFanModeCache(selectedFanMode)
                selectedFanMode
            }

            ProfileType.MYSTAT_HPU -> {
                val selectedFanMode = getMyStatSelectedFanMode(
                    getMyStatHpuFanLevel(configuration as MyStatHpuConfiguration), selectedPosition
                )
                updateFanModeCache(selectedFanMode)
                selectedFanMode
            }

            ProfileType.MYSTAT_PIPE2 -> {
                val selectedFanMode = getMyStatSelectedFanMode(
                    getMyStatPipe2FanLevel(configuration as MyStatPipe2Configuration),
                    selectedPosition
                )
                updateFanModeCache(selectedFanMode)
                selectedFanMode
            }

            else -> {
                -1
            }
        }
        CcuLog.i(L.TAG_CCU_MSHST, "handleMyStatFanMode: $actualFanMode")
        if (actualFanMode != -1) {
            updateUserIntentPoints(
                equipId, fanMode, actualFanMode.toDouble(), CCUHsApi.getInstance().ccuUserName
            )
            if (selectedPosition != 0 && ( selectedPosition % 3 == 0 || isFanModeCurrentOccupied(selectedPosition))) cacheStorage.saveFanModeInCache(
                equipId, actualFanMode
            ) // while saving the fan mode, we need to save the actual fan mode instead of selected position
            else cacheStorage.removeFanModeFromCache(equipId)
        }
    }
}

private fun handleMyStatHumidityMode(
    selectedPosition: Int, equipId: String, equip: MyStatEquip
) {
    updateUserIntentPoints(
        equipId, equip.targetHumidifier,
        (selectedPosition + 1).toDouble(),
        CCUHsApi.getInstance().ccuUserName
    )
}

private fun handleMyStatDeHumidityMode(
    selectedPosition: Int, equipId: String, equip: MyStatEquip
) {
    updateUserIntentPoints(
        equipId, equip.targetDehumidifier,
        (selectedPosition + 1).toDouble(),
        CCUHsApi.getInstance().ccuUserName
    )
}


private fun setUpMyStatHumidifierDeHumidifier(
    viewPointRow2: View,
    equipPoints: HashMap<*, *>,
    equipId: String,
    context: Activity,
    equip: MyStatEquip,
    configuration: MyStatConfiguration,
    profileType: ProfileType
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
        setSpinnerListenerForMyStat(
            humiditySpinner,
            StatZoneStatus.TARGET_HUMIDITY,
            equipId,
            profileType,
            equip,
            configuration
        )
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column1).visibility = View.GONE
    }
    if (equipPoints.containsKey(StatZoneStatus.TARGET_DEHUMIDIFY.name)) {
        textViewLabel4.text = "Target Max Humidity :"
        dehumidifySpinner.adapter = humidityTargetAdapter
        val targetDeHumidity = equipPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] as Double
        dehumidifySpinner.setSelection(targetDeHumidity.toInt() - 1, false)
        setSpinnerListenerForMyStat(
            dehumidifySpinner, StatZoneStatus.TARGET_DEHUMIDIFY,
            equipId, profileType, equip, configuration
        )
        if (viewPointRow2.findViewById<View>(R.id.lt_column1).visibility == View.GONE) {
            textViewLabel4.setPadding(52, 0, 0, 0)
        }
    } else {
        viewPointRow2.findViewById<View>(R.id.lt_column2).visibility = View.GONE
    }
}


fun getMyStatEquipPoints(equipMap: Equip, profileType: ProfileType): HashMap<String, Any> {
    val equipPoints = HashMap<String, Any>()
    val equip = Domain.getDomainEquip(equipMap.id) as MyStatEquip
    val config = getMyStatConfiguration(equipMap.id) as MyStatConfiguration
    var fanLevel = -1
    when (profileType) {
        ProfileType.MYSTAT_PIPE2 -> {
            equipPoints[StatZoneStatus.EQUIP.name] = equip as MyStatPipe2Equip
            equipPoints[StatZoneStatus.CONFIG.name] = config as MyStatPipe2Configuration
            equipPoints[StatZoneStatus.PROFILE_NAME.name] = PIPE2.uppercase(Locale.ROOT)
            equipPoints[StatZoneStatus.PROFILE_TYPE.name] = ProfileType.MYSTAT_PIPE2
            equipPoints[StatZoneStatus.SUPPLY_TEMP.name] = equip.leavingWaterTemperature.readHisVal()
            fanLevel = getMyStatPipe2FanLevel(config)
        }

        ProfileType.MYSTAT_CPU -> {
            equipPoints[StatZoneStatus.EQUIP.name] = equip as MyStatCpuEquip
            equipPoints[StatZoneStatus.CONFIG.name] = config as MyStatCpuConfiguration
            equipPoints[StatZoneStatus.PROFILE_NAME.name] = CPU.uppercase(Locale.ROOT)
            equipPoints[StatZoneStatus.PROFILE_TYPE.name] = ProfileType.MYSTAT_CPU
            fanLevel = getMyStatCpuFanLevel(config)
        }

        ProfileType.MYSTAT_HPU -> {
            equipPoints[StatZoneStatus.EQUIP.name] = equip as MyStatHpuEquip
            equipPoints[StatZoneStatus.CONFIG.name] = config as MyStatHpuConfiguration
            equipPoints[StatZoneStatus.PROFILE_NAME.name] = HPU.uppercase(Locale.ROOT)
            equipPoints[StatZoneStatus.PROFILE_TYPE.name] = ProfileType.MYSTAT_HPU
            fanLevel = getMyStatHpuFanLevel(config)
        }

        else -> {}
    }

    equipPoints[StatZoneStatus.FAN_LEVEL.name] = fanLevel
    equipPoints[StatZoneStatus.CONDITIONING_ENABLED.name] = getMyStatPossibleConditionMode(config)
    equipPoints[StatZoneStatus.STATUS.name] = equip.equipStatusMessage.readDefaultStrVal()
    equipPoints[StatZoneStatus.FAN_MODE.name] = equip.fanOpMode.readPriorityVal().toInt()
    equipPoints[StatZoneStatus.CONDITIONING_MODE.name] = getMyStatSelectedConditioningMode(
        config, equip.conditioningMode.readPriorityVal().toInt()
    )
    if (equip.dischargeAirTemperature.pointExists()) {
        equipPoints[StatZoneStatus.DISCHARGE_AIRFLOW.name] =
            equip.dischargeAirTemperature.readHisVal()
    }
    if (equip.humidifierEnable.pointExists()) {
        equipPoints[StatZoneStatus.TARGET_HUMIDITY.name] = equip.targetHumidifier.readPriorityVal()
    }
    if (equip.dehumidifierEnable.pointExists()) {
        equipPoints[StatZoneStatus.TARGET_DEHUMIDIFY.name] =
            equip.targetDehumidifier.readPriorityVal()
    }
    equipPoints.forEach { CcuLog.i(L.TAG_CCU_MSHST, "MyStat data : ${it.key} : ${it.value}") }
    return equipPoints
}


fun loadMyStatProfile(
    myStatPoints: HashMap<String, Any>,
    inflater: LayoutInflater,
    zonePointsView: LinearLayout,
    equipId: String,
    nodeAddress: String,
    context: Activity
) {

    fun inflateView(layoutId: Int) = inflater.inflate(layoutId, null)
    val viewTitle = inflateView(R.layout.zones_item_title)
    val viewStatus = inflateView(R.layout.zones_item_status)
    val viewRow1 = inflateView(R.layout.zones_item_type2)
    val viewRow2 = inflateView(R.layout.zones_item_type2)
    val viewDischarge = inflateView(R.layout.zones_item_discharge)
    val supplyTempSensor = inflateView(R.layout.zones_item_discharge)

    val config = myStatPoints[StatZoneStatus.CONFIG.name] as MyStatConfiguration
    val equip = myStatPoints[StatZoneStatus.EQUIP.name] as MyStatEquip
    val profileName = myStatPoints[StatZoneStatus.PROFILE_NAME.name] as String
    val profileType = myStatPoints[StatZoneStatus.PROFILE_TYPE.name] as ProfileType
    val status = myStatPoints[StatZoneStatus.STATUS.name] as String

    setMyStatTitleStatusConfig(viewTitle, viewStatus, nodeAddress, status, profileName)
    setUpConditionFanConfig(viewRow1, myStatPoints, equipId, context, profileType, equip, config)
    setUpMyStatHumidifierDeHumidifier(viewRow2, myStatPoints, equipId, context, equip, config, profileType)

    zonePointsView.apply {
        addView(viewTitle)
        addView(viewStatus)
        addView(viewRow2)
        addView(viewRow1)
        setPadding(0, 0, 0, 10)
    }
    showMyStatSupplyTemp(supplyTempSensor, myStatPoints, zonePointsView, nodeAddress)
    showMyStatDischargeConfigIfRequired(viewDischarge, myStatPoints, zonePointsView)
}

fun getSupplyDirection(nodeAddress: String): String {
    val profile = L.getProfile(nodeAddress.toLong()) as MyStatPipe2Profile
    return profile.supplyDirection()
}