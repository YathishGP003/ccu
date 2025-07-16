package a75f.io.renatus.views.userintent

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants.FORCE_OVERRIDE_LEVEL
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.util.getEnumsMap
import a75f.io.util.getRangedValueList
import a75f.io.util.getStringFormat
import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class UserIntentViewModel(application: Application): AndroidViewModel(application) {

    private lateinit var point: Point
    private lateinit var pointId: String
    private lateinit var enumMap : Map<String, Int>
    private lateinit var rangeList : List<String>
    private var isHistorizedPoint = false
    private var hasEnums = false
    private var currentValue = 0.0

    lateinit var pointName : String
    val spinnerOptions = mutableStateListOf<String>()
    val defaultSelectionIndex = mutableIntStateOf(0)
    val currentSelectedIndex = mutableIntStateOf(0)
    val hhDurationVal = mutableIntStateOf(0)
    val mmDurationVal = mutableIntStateOf(0)
    val isDurationValid = mutableStateOf(false)
    val isSaveButtonClicked = mutableStateOf(false)

    val isLoadingComplete = mutableStateOf(false)
    lateinit var longestItem : String

    fun configModelView(pointId: String) {
        viewModelScope.launch {
            fetchPointInfo(pointId)
            prepareSpinnerUIInfo()
            isLoadingComplete.value = true
        }
    }

    private fun fetchPointInfo(pointId: String) {
        point = Point.Builder().setHDict(CCUHsApi.getInstance().readHDictById(pointId)).build()
        this.pointId = pointId
        pointName = if(point.markers.contains(Tags.CONNECTMODULE)) point.displayName else point.shortDis
        isHistorizedPoint = point.markers.contains(Tags.HIS)
        currentValue = CCUHsApi.getInstance().readPointPriorityVal(point.id)

        // Check if the point consumes value from enums or ranges
        point.enums?.let { enumString ->
            hasEnums = true
            enumMap = getEnumsMap(enumString)
        } ?: run {
            hasEnums = false
            rangeList = getRangedValueList(point.minVal.toDouble(), point.maxVal.toDouble(), point.incrementVal.toDouble())
        }
    }

    private fun prepareSpinnerUIInfo() {
        if(hasEnums) {
            spinnerOptions.addAll(
                enumMap.keys
            )
            defaultSelectionIndex.intValue = enumMap.values.indexOf(currentValue.toInt())
            currentSelectedIndex.intValue = defaultSelectionIndex.intValue
        } else {
            spinnerOptions.addAll(rangeList.map { it })
            defaultSelectionIndex.intValue =
                rangeList.indexOf(getStringFormat(currentValue, point.incrementVal.toDouble())).coerceAtLeast(0)
            currentSelectedIndex.intValue = defaultSelectionIndex.intValue
        }
        longestItem = spinnerOptions.maxByOrNull { it.length } ?: ""
    }

    fun validateDuration() {
        //TODO: Remove custom logs
        CcuLog.d("CCU_USER_INTENT", "UserIntentViewModel.validateDuration() called with hh: ${hhDurationVal.intValue}, mm: ${mmDurationVal.intValue}")
        if (hhDurationVal.intValue == 0 && mmDurationVal.intValue == 0) {
            CcuLog.d("CCU_USER_INTENT", "UserIntentViewModel.validateDuration() - Duration is zero, setting isDurationValid to false")
            isDurationValid.value = false
        } else {
            CcuLog.d("CCU_USER_INTENT", "UserIntentViewModel.validateDuration() - Duration is valid, setting isDurationValid to true")
            isDurationValid.value = true
        }
    }

    fun saveUserIntent() {
        isSaveButtonClicked.value = true
        CcuLog.d("CCU_USER_INTENT", "UserIntentViewModel.saveUserIntent() called with pointId: ${pointId}, selectedValue: ${currentSelectedIndex.intValue}, duration: ${hhDurationVal.intValue}h ${mmDurationVal.intValue}m")
        val selectedValue = getValueForSelectedItem()
        CCUHsApi.getInstance().writePointForCcuUser(
            pointId,
            FORCE_OVERRIDE_LEVEL,
            selectedValue,
            getDurationInMillis()
        )
        CcuLog.d("CCU_USER_INTENT", "UserIntentViewModel.saveUserIntent() - Point $pointId updated with value: $selectedValue and duration: ${getDurationInMillis()} ms")
        if(isHistorizedPoint) {
            CcuLog.d("CCU_USER_INTENT", "UserIntentViewModel.saveUserIntent() - Writing historical value for point $pointId with value: $selectedValue")
            CCUHsApi.getInstance().writeHisValById(pointId, selectedValue)
            CcuLog.d("CCU_USER_INTENT", "UserIntentViewModel.saveUserIntent() - Historical value written for point $pointId")
        }
    }

    private fun getValueForSelectedItem(): Double {
        return if(hasEnums) {
            enumMap.values.elementAt(currentSelectedIndex.intValue).toDouble()
        } else {
            rangeList[currentSelectedIndex.intValue].toDouble()
        }
    }

    private fun getDurationInMillis(): Int {
        return (hhDurationVal.intValue * 3600 + mmDurationVal.intValue * 60) * 1000
    }
}