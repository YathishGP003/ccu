package a75f.io.renatus.hyperstatsplit.viewModels

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import android.content.Context
import android.widget.ArrayAdapter
import io.reactivex.rxjava3.subjects.BehaviorSubject

/**
 * Created for HyperStat by Manjunath K on 08-08-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

interface HyperStatSplitModel {

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
    fun sensorBusTempSwitchChanged(index: Int, checked: Boolean)
    fun sensorBusTempMappingSelected(index: Int, position: Int)
    fun sensorBusPressSwitchChanged(index: Int, checked: Boolean)
    fun sensorBusPressMappingSelected(index: Int, position: Int)
    fun universalInSwitchChanged(index: Int, checked: Boolean)
    fun universalInMappingSelected(index: Int, position: Int)
    fun getUniversalInMapping(): Array<String>
    fun isDamperSelected(association: Int): Boolean
    fun outsideDamperMinOpenSelect(position: Int)
    fun exhaustFanStage1ThresholdSelect(position: Int)
    fun exhaustFanStage2ThresholdSelect(position: Int)
    fun exhaustFanHysteresisSelect(position: Int)
    fun zoneCO2DamperOpeningRateSelect(position: Int)
    fun zoneCO2ThresholdSelect(position: Int)
    fun zoneCO2TargetSelect(position: Int)
    fun zoneVOCThresholdSelect(position: Int)
    fun zoneVOCTargetSelect(position: Int)
    fun zonePmTargetSelect(position: Int)
    fun onDisplayHumiditySelected(checked: Boolean)
    fun onDisplayCo2Selected(checked: Boolean)
    fun onDisplayVocSelected(checked: Boolean)
    fun onDisplayP2pmSelected(checked: Boolean)
    fun setConfigSelected()
    fun getRelayMappingAdapter(context : Context, values: Array<String>): ArrayAdapter<*>
    fun validateProfileConfig() : Boolean
    fun getValidationMessage() : String
    fun voltageAtStagedFanSelected(index: Int, position: Int)
}