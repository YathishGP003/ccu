package a75f.io.renatus.profiles.hss

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.HyperSplit
import a75f.io.device.mesh.hypersplit.HyperSplitMessageSender
import a75f.io.device.serial.MessageType
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitProfile
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitProfileConfiguration
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.profiles.OnPairingCompleteListener
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import kotlinx.coroutines.Job
import kotlin.properties.Delegates

open class HyperStatSplitViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    protected var deviceAddress by Delegates.notNull<Short>()

    protected lateinit var hssProfile: HyperStatSplitProfile
    lateinit var profileConfiguration: HyperStatSplitProfileConfiguration

    lateinit var equipModel : SeventyFiveFProfileDirective
    lateinit var deviceModel : SeventyFiveFDeviceDirective
    open var viewState = mutableStateOf(HyperStatSplitState())

    lateinit var context: Context
    lateinit var hayStack: CCUHsApi

    lateinit var pairingCompleteListener: OnPairingCompleteListener
    protected var saveJob : Job? = null
    var openDuplicateDialog by mutableStateOf(false)
    var openMissingDialog by mutableStateOf(false)
    var openCancelDialog by mutableStateOf(false)

    lateinit var temperatureOffsetsList: List<String>
    lateinit var fanLowMedHighSpeedsList: List<String>
    lateinit var outsideDamperMinOpenList: List<String>
    lateinit var exhaustFanThresholdList: List<String>
    lateinit var exhaustFanHysteresisList: List<String>
    lateinit var zoneCO2DamperOpeningRateList: List<String>
    lateinit var prePurgeOutsideDamperOpenList: List<String>
    lateinit var zoneCO2ThresholdList: List<String>
    lateinit var zoneCO2TargetList: List<String>
    lateinit var zoneVOCThresholdList: List<String>
    lateinit var zoneVOCTargetList: List<String>
    lateinit var zonePM2p5TargetList: List<String>

    var minMaxVoltage = List(11) { Option(it, it.toString()) }
    var testVoltage = List(101) { Option(it, it.toString()) }

    open fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        openDuplicateDialog = false
        openMissingDialog = false
        openCancelDialog = false
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
    }

    /**
     * Get the Allowed values name for the given domain name
     */
    fun getAllowedValues(domainName: String, model: SeventyFiveFProfileDirective): List<Option> {
        val pointDef = getPointByDomainName(model, domainName) ?: return emptyList()
        return if (pointDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            val constraint = pointDef.valueConstraint as MultiStateConstraint
            val enums = mutableListOf<Option>()
            constraint.allowedValues.forEach {
                enums.add(Option(it.index, it.value, it.dis))
            }
            enums
        } else {
            emptyList()
        }
    }

    fun getDeviceDisplayCount() : Int {
        var count = 0
        if (viewState.value.displayHumidity) count++
        if (viewState.value.displayCO2) count++
        if (viewState.value.displayPM2p5) count++

        return count
    }

    private fun getPointByDomainName(
        modelDefinition: SeventyFiveFProfileDirective, domainName: String
    ): SeventyFiveFProfilePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }

    data class Option(val index: Int, val value: String, val dis: String? = null)

    open fun saveConfiguration() {
        // implemented in subclasses
    }

    open fun handleTestRelayChanged(index: Int, isChecked: Boolean) {
        when (index) {
            0 -> viewState.value.testStateRelay1 = isChecked
            1 -> viewState.value.testStateRelay2 = isChecked
            2 -> viewState.value.testStateRelay3 = isChecked
            3 -> viewState.value.testStateRelay4 = isChecked
            4 -> viewState.value.testStateRelay5 = isChecked
            5 -> viewState.value.testStateRelay6 = isChecked
            6 -> viewState.value.testStateRelay7 = isChecked
            7 -> viewState.value.testStateRelay8 = isChecked
        }

        sendTestSignalControlMessage()
    }

    open fun handleTestAnalogOutChanged(index: Int, value: Double) {
        when (index) {
            0 -> viewState.value.testStateAnalogOut1 = value
            1 -> viewState.value.testStateAnalogOut2 = value
            2 -> viewState.value.testStateAnalogOut3 = value
            3 -> viewState.value.testStateAnalogOut4 = value
        }

        sendTestSignalControlMessage()
    }

    open fun sendTestSignalControlMessage() {
        val testMessage = HyperSplit.HyperSplitControlsMessage_t.newBuilder()
            .setRelay1(viewState.value.testStateRelay1)
            .setRelay2(viewState.value.testStateRelay2)
            .setRelay3(viewState.value.testStateRelay3)
            .setRelay4(viewState.value.testStateRelay4)
            .setRelay5(viewState.value.testStateRelay5)
            .setRelay6(viewState.value.testStateRelay6)
            .setRelay7(viewState.value.testStateRelay7)
            .setRelay8(viewState.value.testStateRelay8)
            .setAnalogOut1(
                HyperSplit.HyperSplitAnalogOutputControl_t
                    .newBuilder().setPercent(viewState.value.testStateAnalogOut1.toInt()).build()
            )
            .setAnalogOut2(
                HyperSplit.HyperSplitAnalogOutputControl_t
                    .newBuilder().setPercent(viewState.value.testStateAnalogOut2.toInt()).build()
            )
            .setAnalogOut3(
                HyperSplit.HyperSplitAnalogOutputControl_t
                    .newBuilder().setPercent(viewState.value.testStateAnalogOut3.toInt()).build()
            )
            .setAnalogOut4(
                HyperSplit.HyperSplitAnalogOutputControl_t
                    .newBuilder().setPercent(viewState.value.testStateAnalogOut4.toInt()).build()
            )
            .setSetTempCooling(148)
            .setSetTempHeating(140)
            .setFanSpeed(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO)
            .setConditioningMode(HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_AUTO)
            .build()

        HyperSplitMessageSender.writeControlMessage(
            testMessage, deviceAddress.toInt(),
            MessageType.HYPERSPLIT_CONTROLS_MESSAGE, false
        )

        if (viewState.value.testStateRelay1 || viewState.value.testStateRelay2 ||
            viewState.value.testStateRelay3 || viewState.value.testStateRelay4 ||
            viewState.value.testStateRelay5 || viewState.value.testStateRelay6 ||
            viewState.value.testStateRelay7 || viewState.value.testStateRelay8) {

            if (!Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = true
            }
        } else {
            if (Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = false
            }
        }
    }

    open fun hasUnsavedChanges() : Boolean { return false }


    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

}