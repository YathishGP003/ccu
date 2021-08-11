package a75f.io.renatus.hyperstat.vrv

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.L
import a75f.io.logic.bo.building.vrv.VrvProfile
import a75f.io.logic.bo.building.vrv.VrvProfileConfiguration
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlin.math.round

class HyperStatVrvViewModel(application: Application) : AndroidViewModel(application) {

    private val TEMP_OFFSET_LIMIT_MAX = 10
    private val TEMP_OFFSET_LIMIT_MIN = -10
    private val TEMP_OFFSET_INC = 0.1f

    private val HUMIDITY_LIMIT_MAX = 100
    private val HUMIDITY_LIMIT_MIN = 0
    private val HUMIDITY_INC = 1.0F

    val viewState: BehaviorSubject<VrvViewState> = BehaviorSubject.create()
    val oneTimeActions: PublishSubject<OneTimeUiActions> = PublishSubject.create()

    var vrvProfile : VrvProfile? = null
    private var nodeAddr : Short = 0
    lateinit var roomRef : String
    lateinit var floorRef : String
    private val currentState: VrvViewState
        get() = viewState.value


    fun initData(addr: Short, room: String, floor: String) {
        nodeAddr = addr
        roomRef = room
        floorRef = floor
        vrvProfile = L.getProfile(addr)?.let {
            it as VrvProfile
        }?:null
        viewState.onNext(getInitialViewState())
    }

    private fun getInitialViewState(): VrvViewState {
        return vrvProfile?.let {
                        val config : VrvProfileConfiguration = it.getProfileConfiguration(nodeAddr)
                        VrvViewState(
                            config.temperatureOffset.toInt(),
                            config.minHumiditySp.toInt(),
                            config.maxHumiditySp.toInt())
                        } ?:VrvViewState(
                        tempOffsetPosition = tempOffsetIndexFromValue(-9.9f),
                        humidityMinPosition = 0,
                        humidityMaxPosition = 100
                        )
    }

    fun saveProfile() {
        vrvProfile?.let {
            it.updateEquip(VrvProfileConfiguration(currentState.tempOffsetPosition.toDouble(),
                            currentState.humidityMinPosition.toDouble(),
                            currentState.humidityMaxPosition.toDouble()))
        }?: run {
            vrvProfile = VrvProfile()
            vrvProfile!!.createVrvEquip(
                CCUHsApi.getInstance(), nodeAddr,
                VrvProfileConfiguration(
                    currentState.tempOffsetPosition.toDouble(),
                    currentState.humidityMinPosition.toDouble(),
                    currentState.humidityMaxPosition.toDouble(),
                ),
                roomRef,
                floorRef
            )
            L.ccu().zoneProfiles.add(vrvProfile)
        }
    }

    private fun tempOffsetIndexFromValue(tempOffset: Float) =
        offsetIndexFromValue(TEMP_OFFSET_LIMIT_MIN, TEMP_OFFSET_INC, tempOffset)

    private fun offsetIndexFromValue(min: Int, inc: Float, offset: Float): Int {
        val offsetFromZeroCount = (min / inc).toInt()
        return (offset / inc).toInt() - offsetFromZeroCount
    }

    fun tempOffsetSelected(newVal: Int) {
        viewState.onNext(currentState.copy(
            tempOffsetPosition = newVal)
        )
    }
    fun humidityMinSelected(newVal: Int) {
        viewState.onNext(currentState.copy(
            humidityMinPosition = newVal)
        )
    }
    fun humidityMaxSelected(newVal: Int) {
        viewState.onNext(currentState.copy(
            humidityMaxPosition = newVal)
        )
    }

    fun tempOffsetSpinnerValues(): Array<String?>
            = offsetSpinnerValues(TEMP_OFFSET_LIMIT_MAX, TEMP_OFFSET_LIMIT_MIN, TEMP_OFFSET_INC)

    fun humiditySpinnerValues(): Array<String?>
            = offsetSpinnerValues(HUMIDITY_LIMIT_MAX, HUMIDITY_LIMIT_MIN, HUMIDITY_INC, true, "%")


    private fun offsetSpinnerValues(
        max: Int,
        min: Int,
        inc: Float,
        displayAsInt: Boolean = false,
        suffix: String = ""
    ): Array<String?> {

        val range = max - min
        val count =   (range / inc).toInt() + 1;
        val offsetFromZeroCount = (min / inc).toInt()

        val nums = arrayOfNulls<String>(count)
        for (nNum in 0 until count) {
            var rawValue = (nNum + offsetFromZeroCount).toFloat() * inc
            if (displayAsInt) {
                nums[nNum] = rawValue.toInt().toString() + suffix
            } else {
                rawValue = round(rawValue * 10 ) /10
                nums[nNum] = rawValue.toString() + suffix
            }
        }
        return nums
    }


}
data class VrvViewState(
    val tempOffsetPosition: Int,
    val humidityMinPosition: Int,
    val humidityMaxPosition: Int
)

data class OneTimeUiActions(
    val errorMessage: String? = null
)