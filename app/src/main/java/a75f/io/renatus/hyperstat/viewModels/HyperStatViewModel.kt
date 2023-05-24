package a75f.io.renatus.hyperstat.viewModels

import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.Output
import a75f.io.logic.bo.building.definitions.OutputAnalogActuatorType
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatProfile
import a75f.io.renatus.R
import android.app.Application
import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * Created by Manjunath K on 15-07-2022.
 */

abstract class HyperStatViewModel(application: Application) : AndroidViewModel(application),
    HyperStatModel {

    var address: Short = 0
    lateinit var roomName: String
    lateinit var floorName: String
    lateinit var nodeType: NodeType
    lateinit var profileType: ProfileType
    var hyperStatConfiguration: BaseProfileConfiguration? = null
    var hyperStatProfile: HyperStatProfile? = null

    val viewState: BehaviorSubject<ViewState> = BehaviorSubject.create()

     val currentState: ViewState
        get() = viewState.value as ViewState


    override fun getState(): BehaviorSubject<ViewState> {
       return viewState
    }

     fun addOutputRelayConfigurations(
        relay1: Boolean,relay2: Boolean,relay3: Boolean,
        relay4: Boolean,relay5: Boolean,relay6: Boolean,
        analogOut1: Boolean,analogOut2: Boolean,analogOut3: Boolean,
        config: BaseProfileConfiguration
    ){
        if (relay1) config.outputs.add(constructOutputRelay(address, Port.RELAY_ONE))
        if (relay2) config.outputs.add(constructOutputRelay(address, Port.RELAY_TWO))
        if (relay3) config.outputs.add(constructOutputRelay(address, Port.RELAY_THREE))
        if (relay4) config.outputs.add(constructOutputRelay(address, Port.RELAY_FOUR))
        if (relay5) config.outputs.add(constructOutputRelay(address, Port.RELAY_FIVE))
        if (relay6) config.outputs.add(constructOutputRelay(address, Port.RELAY_SIX))

        if (analogOut1) config.outputs.add(constructOutputAnalogOut(address, Port.ANALOG_OUT_ONE))
        if (analogOut2) config.outputs.add(constructOutputAnalogOut(address, Port.ANALOG_OUT_TWO))
        if (analogOut3) config.outputs.add(constructOutputAnalogOut(address, Port.ANALOG_OUT_THREE))
    }

    private fun constructOutputRelay(
        address: Short,
        port: Port
    ): Output {
        val outputRelay = Output()
        outputRelay.address = address
        outputRelay.port = port
        outputRelay.mOutputRelayActuatorType = OutputRelayActuatorType.NormallyOpen
        return outputRelay
    }

    private fun constructOutputAnalogOut(
        address: Short,
        port: Port
    ): Output {
        val analogOut = Output()
        analogOut.address = address
        analogOut.port = port
        analogOut.mOutputAnalogActuatorType = OutputAnalogActuatorType.ZeroToTenV
        return analogOut
    }


    override fun tempOffsetSelected(newVal: Int) {
        viewState.onNext(
            currentState.copy(
                tempOffsetPosition = newVal
            )
        )
    }

    override fun enableForceOccupiedSwitchChanged(checked: Boolean) {
        viewState.onNext(
            currentState.copy(forceOccupiedEnabled = checked)
        )
    }

    override fun enableAutoAwaySwitchChanged(checked: Boolean) {
        viewState.onNext(
            currentState.copy(autoAwayEnabled = checked)
        )
    }

    override fun relaySwitchChanged(index: Int, checked: Boolean) {
        val relays = currentState.relays
        val newRelays = relays.updated(index, relays[index].copy(enabled = checked))
        viewState.onNext(
            currentState.copy(relays = newRelays)
        )
    }

    override fun relayMappingSelected(index: Int, position: Int) {
        val relays = currentState.relays
        val newRelays = relays.updated(index, relays[index].copy(association = position))
        viewState.onNext(
            currentState.copy(relays = newRelays)
        )
    }

    override fun analogOutSwitchChanged(index: Int, checked: Boolean) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts = analogOuts.updated(index, analogOuts[index].copy(checked))
        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }

    override fun analogOutMappingSelected(index: Int, position: Int) {
        val analogOuts = currentState.analogOutUis
        val association = when (position) {
            0 -> 0
            1 -> 1
            3 -> 2
            4 -> 3
            2 -> 4
            else -> analogOuts[index].association // Use the existing association if position doesn't match any of the specified values
        }

        val newAnalogOuts = analogOuts.updated(
            index, analogOuts[index].copy(association = association)
        )

        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }




    override fun voltageAtDamperSelected(isMinPosition: Boolean, index: Int, position: Int) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts =
            if (isMinPosition) {
                analogOuts.updated(
                    index, analogOuts[index].copy(voltageAtMin = analogVoltageFromIndex(position))
                )
            } else {
                analogOuts.updated(
                    index, analogOuts[index].copy(voltageAtMax = analogVoltageFromIndex(position))
                )
            }
        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }

    override fun updateFanConfigSelected(type: Int, index: Int, position: Int) {
        val analogOuts = currentState.analogOutUis
        val newAnalogOuts = when {
            (type == 1) -> {
                analogOuts.updated(index, analogOuts[index].copy(perAtFanLow =  analogVoltageFromIndex(position)))
            }
            (type == 2) -> {
                analogOuts.updated(index, analogOuts[index].copy(perAtFanMedium = analogVoltageFromIndex(position)))
            }
            (type == 3) -> {
                analogOuts.updated(index, analogOuts[index].copy(perAtFanHigh = analogVoltageFromIndex(position)))
            }
            else -> analogOuts
        }

        viewState.onNext(
            currentState.copy(
                analogOutUis = newAnalogOuts
            )
        )
    }

    override fun analogInSwitchChanged(index: Int, checked: Boolean) {
        val analogIns = currentState.analogIns
        val newAnalogIns = analogIns.updated(index, analogIns[index].copy(enabled = checked))
        viewState.onNext(
            currentState.copy(
                analogIns = newAnalogIns
            )
        )
    }

    override fun analogInMappingSelected(index: Int, position: Int) {
        val analogIns = currentState.analogIns
        val newAnalogIns = analogIns.updated(
            index, analogIns[index].copy(association = position)
        )
        viewState.onNext(
            currentState.copy(
                analogIns = newAnalogIns
            )
        )
    }

    override fun airflowTempSensorSwitchChanged(checked: Boolean) {
        viewState.onNext(
            currentState.copy(
                airflowTempSensorEnabled = checked
            )
        )
    }



    override fun zoneCO2DamperOpeningRateSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneCO2DamperOpeningRatePos = position
            )
        )

    }

    override fun zoneCO2ThresholdSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneCO2ThresholdPos = position
            )
        )

    }

    override fun zoneCO2TargetSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneCO2TargetPos = position
            )
        )
    }


    override fun zoneVOCThresholdSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneVocThresholdPos = position
            )
        )
    }
    override fun zoneVOCTargetSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zoneVocTargetPos = position
            )
        )
    }


    override fun zonePmThresholdSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zonePm2p5ThresholdPos = position
            )
        )
    }
    override fun zonePmTargetSelect(position: Int) {
        viewState.onNext(
            currentState.copy(
                zonePm2p5TargetPos = position
            )
        )
    }

    override fun onDisplayHumiditySelected(checked: Boolean){
        viewState.onNext(
            currentState.copy( isDisplayHumidityEnabled = checked )
        )
    }
    override fun onDisplayCo2Selected(checked: Boolean){
        viewState.onNext(
            currentState.copy( isDisplayCo2Enabled = checked )
        )
    }
    override fun onDisplayVocSelected(checked: Boolean){
        viewState.onNext(
            currentState.copy( isDisplayVOCEnabled = checked )
        )
    }
    override fun onDisplayP2pmSelected(checked: Boolean){
        viewState.onNext(
            currentState.copy( isDisplayPp2p5Enabled = checked )
        )
    }

    override fun getRelayMappingAdapter(context: Context, values: Array<String>): ArrayAdapter<*> {
        return ArrayAdapter(context , R.layout.spinner_dropdown_item, values)
    }

    override fun validateProfileConfig() = true

    override fun getValidationMessage() = ""
}


fun offsetSpinnerValues(
    max: Int,
    min: Int,
    inc: Double,
    displayAsInt: Boolean = false,
    suffix: String = ""
): Array<String?> {

    val range = max - min
    val count = (range / inc).toInt() + 1
    val offsetFromZeroCount = (min / inc).toInt()

    val numbers = arrayOfNulls<String>(count)
    for (nNum in 0 until count) {
        val rawValue = (nNum + offsetFromZeroCount).toDouble() * inc
        if (displayAsInt) {
            numbers[nNum] = String.format("%d", rawValue.toInt()) + suffix
        } else {
            numbers[nNum] = String.format("%.1f", rawValue) + suffix
        }
    }
    return numbers
}

// Extension method to kotlinize and pretty up our code above
 fun Spinner.setOnItemSelected(onItemSelected: (position: Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onItemSelected.invoke(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {} // no implementation
    }
}


// Dropdown choice value ranges
const val TEMP_OFFSET_LIMIT_MAX = 10
const val TEMP_OFFSET_LIMIT_MIN = -10
const val TEMP_OFFSET_INC = 0.1

const val ANALOG_VOLTAGE_LIMIT_MAX = 10
const val ANALOG_VOLTAGE_LIMIT_MIN = 0
const val ANALOG_VOLTAGE_INC = 1.0

const val ANALOG_FAN_MAX_SPEED = 100
const val ANALOG_FAN_MIN_SPEED = 0
const val ANALOG_FAN_SPEED_INC = 1.0

const val OpeningRateMin = 0
const val OpeningRateMax = 200

const val CO2Min = 0
const val CO2Max = 4000

const val INC = 10

const val VOC_INC = 100
const val PM_INC = 5

const val VOCMin = 0
const val VOCMax = 10000

const val PMMin = 0
const val PMMax = 1000
const val DISABLED = 65535.0
fun analogVoltageIndexFromValue(voltage: Double) =
    offsetIndexFromValue(ANALOG_VOLTAGE_LIMIT_MIN, ANALOG_VOLTAGE_INC, voltage)

fun analogVoltageFromIndex(index: Int) =
    offsetFromIndex(ANALOG_VOLTAGE_LIMIT_MIN, ANALOG_VOLTAGE_INC, index)

fun offsetIndexFromValue(min: Int, inc: Double, offset: Double): Int {
    val offsetFromZeroCount = (min / inc).toInt()
    return (offset / inc).toInt() - offsetFromZeroCount
}

fun offsetFromIndex(min: Int, inc: Double, index: Int): Double {
    val offsetFromZeroCount = (min / inc).toInt()
    return String.format("%.2f", (index + offsetFromZeroCount).toDouble() * inc).toDouble()
}

fun tempOffsetSpinnerValues(): Array<String?> =
    offsetSpinnerValues(TEMP_OFFSET_LIMIT_MAX, TEMP_OFFSET_LIMIT_MIN, TEMP_OFFSET_INC)

fun analogVoltageAtSpinnerValues(): Array<String?> =
    offsetSpinnerValues(ANALOG_VOLTAGE_LIMIT_MAX, ANALOG_VOLTAGE_LIMIT_MIN, ANALOG_VOLTAGE_INC, true, "V")


fun analogFanSpeedIndexFromValue(percent: Double) =
    offsetIndexFromValue(ANALOG_FAN_MIN_SPEED, ANALOG_FAN_SPEED_INC, percent)

fun analogFanSpeedFromIndex(index: Int) =
    offsetFromIndex(ANALOG_FAN_MIN_SPEED, ANALOG_FAN_SPEED_INC, index)

fun analogFanLevelSpeedValue(): Array<String?> {
    return offsetSpinnerValues(
        ANALOG_FAN_MAX_SPEED, ANALOG_FAN_MIN_SPEED,
        ANALOG_FAN_SPEED_INC, true, "%"
    )
}

fun co2DCVDamperValueFromIndex(index: Int) = offsetFromIndex(CO2Min, INC.toDouble(), index)
// fun openingDamperValueFromIndex(index: Int) = offsetFromIndex(OpeningRateMin, INC.toDouble(), index)
fun openingDamperValueFromIndex(index: Int): Double{
    return if (index == 0) DISABLED else co2DCVDamperValue()[index].toString().replace(" ppm","").toDouble()
}
// fun vocValueFromIndex(index: Int) = offsetFromIndex(VOCMin, VOC_INC.toDouble(), index)
fun vocValueFromIndex(index: Int): Double{
    return if (index == 0) DISABLED else vocValues()[index].toString().replace(" ppb","").toDouble()
}
// fun pm25ValueFromIndex(index: Int) = offsetFromIndex(PMMin, PM_INC.toDouble(), index)
fun pm25ValueFromIndex(index: Int): Double{
    return if (index == 0) DISABLED else pmValues()[index].toString().replace(" ug/㎥","").toDouble()
}
fun co2DCVDamperSetIndexFromValue(value: Double): Int {
    return if (value == DISABLED) 0 else offsetIndexFromValue(CO2Min, INC.toDouble(), value) + 1
}
//fun co2DCVDamperSetIndexFromValue(value: Double) = offsetIndexFromValue(CO2Min, INC.toDouble(), value)
fun co2DCVOpeningDamperSetIndexFromValue(value: Double) =
    offsetIndexFromValue(OpeningRateMin, INC.toDouble(), value)
fun vocSetIndexFromValue(value: Double): Int{
    return if(value == DISABLED) 0 else offsetIndexFromValue(VOCMin, VOC_INC.toDouble(), value)+1
}
// fun vocSetIndexFromValue(value: Double) = offsetIndexFromValue(VOCMin, VOC_INC.toDouble(), value)
//fun pmSetIndexFromValue(value: Double) = offsetIndexFromValue(PMMin, PM_INC.toDouble(), value)

fun pmSetIndexFromValue(value: Double): Int{
    return if(value == DISABLED) 0 else offsetIndexFromValue(PMMin, PM_INC.toDouble(), value)+1
}
fun co2DCVDamperValue(): Array<String?> {
    val co2List: MutableList<String?> = offsetSpinnerValues(
        CO2Max, CO2Min,
        INC.toDouble(), true, " ppm"
    ).toMutableList()
    co2List.add(0,"Disabled")
    return co2List.toTypedArray()
}

fun co2DCVOpeningDamperValue(): Array<String?> {
    return offsetSpinnerValues(
        OpeningRateMax, OpeningRateMin,
        INC.toDouble(), true, " %"
    )
}

fun vocValues(): Array<String?> {
    val vocList: MutableList<String?> = offsetSpinnerValues(
        VOCMax, VOCMin,
        VOC_INC.toDouble(), true, " ppb"
    ).toMutableList()
    vocList.add(0,"Disabled")
    return vocList.toTypedArray()
}
fun pmValues(): Array<String?> {
    val pmList: MutableList<String?> = offsetSpinnerValues(
        PMMax, PMMin,
        PM_INC.toDouble(), true, " ug/㎥"
    ).toMutableList()
    pmList.add(0,"Disabled")
    return pmList.toTypedArray()
}

