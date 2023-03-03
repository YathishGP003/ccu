package a75f.io.renatus.hyperstat.viewModels

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.sensors.SensorType
import android.content.Context
import android.widget.ArrayAdapter
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * Created by Manjunath K on 08-08-2022.
 */

interface HyperStatModel {

    fun getProfileName(): String
    fun isProfileConfigured() :Boolean

    fun getState(): BehaviorSubject<ViewState>
    fun initData(address: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType)
    fun tempOffsetSelected(newVal: Int)
    fun enableForceOccupiedSwitchChanged(checked: Boolean)
    fun enableAutoAwaySwitchChanged(checked: Boolean)
    fun relaySwitchChanged(index: Int, checked: Boolean)
    fun relayMappingSelected(index: Int, position: Int)
    fun getRelayMapping():Array<String>
    fun analogOutSwitchChanged(index: Int, checked: Boolean)
    fun analogOutMappingSelected(index: Int, position: Int)
    fun getAnalogOutMapping(): Array<String>
    fun voltageAtDamperSelected(isMinPosition: Boolean, index: Int, position: Int)
    fun updateFanConfigSelected(type: Int, index: Int, position: Int)
    fun analogInSwitchChanged(index: Int, checked: Boolean)
    fun analogInMappingSelected(index: Int, position: Int)
    fun airflowTempSensorSwitchChanged(checked: Boolean)
    fun th2SwitchChanged(checked: Boolean)
    fun getTh2SensorLabel(): String
    fun isDamperSelected(association: Int): Boolean
    fun zoneCO2DamperOpeningRateSelect(position: Int)
    fun zoneCO2ThresholdSelect(position: Int)
    fun zoneCO2TargetSelect(position: Int)
    fun zoneVOCThresholdSelect(position: Int)
    fun zoneVOCTargetSelect(position: Int)
    fun zonePmThresholdSelect(position: Int)
    fun zonePmTargetSelect(position: Int)
    fun onDisplayHumiditySelected(checked: Boolean)
    fun onDisplayCo2Selected(checked: Boolean)
    fun onDisplayVocSelected(checked: Boolean)
    fun onDisplayP2pmSelected(checked: Boolean)
    fun setConfigSelected()
    fun getRelayMappingAdapter(context : Context, values: Array<String>): ArrayAdapter<*>
    fun validateProfileConfig() : Boolean
    fun getValidationMessage() : String
}