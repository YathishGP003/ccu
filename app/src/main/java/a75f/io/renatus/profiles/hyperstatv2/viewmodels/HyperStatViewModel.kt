package a75f.io.renatus.profiles.hyperstatv2.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.HyperStat.HyperStatAnalogOutputControl_t
import a75f.io.device.mesh.hyperstat.HyperStatMessageGenerator
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender
import a75f.io.device.serial.MessageType
import a75f.io.domain.api.Domain.getStringFormat
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.PossibleFanMode
import a75f.io.logic.bo.building.hyperstat.profiles.HyperStatProfile
import a75f.io.logic.bo.building.hyperstat.profiles.util.getPossibleFanModeSettings
import a75f.io.logic.bo.building.hyperstat.v2.configs.HyperStatConfiguration
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HyperStatV2ViewState
import a75f.io.renatus.profiles.system.advancedahu.Option
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import kotlinx.coroutines.Job
import kotlin.properties.Delegates

/**
 * Created by Manjunath K on 26-09-2024.
 */

open class HyperStatViewModel(application: Application) : AndroidViewModel(application) {


    protected var deviceAddress by Delegates.notNull<Short>()
    protected lateinit var hyperStatProfile: HyperStatProfile

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    lateinit var profileConfiguration: HyperStatConfiguration
    lateinit var context: Context
    lateinit var hayStack: CCUHsApi
    lateinit var equipModel: SeventyFiveFProfileDirective
    lateinit var deviceModel: SeventyFiveFDeviceDirective
    lateinit var temperatureOffsetsList: List<String>
    lateinit var pairingCompleteListener: OnPairingCompleteListener
    var equipRef: String? = null
    protected var saveJob: Job? = null

    open var viewState: MutableState<HyperStatV2ViewState> = mutableStateOf(HyperStatV2ViewState())

    var minMaxVoltage = List(11) { Option(it, it.toString()) }
    var testVoltage = List(101) { Option(it, it.toString()) }
    var damperOpeningRate = (10..100 step 10).toList().map { Option(it, it.toString()) }

    open fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        this.context = context
        this.hayStack = hayStack
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        deviceModel = ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
    }

    open fun saveConfiguration() {}

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

    fun getListByDomainName(domainName: String, model: SeventyFiveFProfileDirective): List<String> {
        val valuesList: MutableList<String> = mutableListOf()
        val point = model.points.find { it.domainName == domainName }

        if (point?.valueConstraint is MultiStateConstraint) {

            (point.valueConstraint as MultiStateConstraint).allowedValues.forEach { state ->
                valuesList.add(state.value)
            }

        } else if (point?.valueConstraint is NumericConstraint) {

            val minVal = (point.valueConstraint as NumericConstraint).minValue
            val maxVal = (point.valueConstraint as NumericConstraint).maxValue
            val incVal = point.presentationData?.get("tagValueIncrement").toString().toDouble()

            var it = minVal
            while (it <= maxVal && incVal > 0.0) {
                valuesList.add(getStringFormat(it, incVal))
                it += incVal
            }

        }
        return valuesList
    }

    fun getOptionByDomainName(domainName: String, model: SeventyFiveFProfileDirective, roundOffRequired: Boolean = false): List<Option> {
        val valuesList: MutableList<Option> = mutableListOf()
        val point = getPointByDomainName(model, domainName) ?: return emptyList()

        if (point.valueConstraint is NumericConstraint) {
            val minVal = (point.valueConstraint as NumericConstraint).minValue
            val maxVal = (point.valueConstraint as NumericConstraint).maxValue
            val incVal = point.presentationData?.get("tagValueIncrement").toString().toDouble()
            var it = minVal
            var position = 0
            while (it <= maxVal && incVal > 0.0) {
                val item = if (roundOffRequired) {
                    Option(position++, ("%.0f").format(it))
                } else {
                    Option(position++, ("%.2f").format(it))
                }
                valuesList.add(item)
                it += incVal
            }
        }
        return valuesList
    }

    fun getUnit(domainName: String, model: SeventyFiveFProfileDirective): String {
        val point = getPointByDomainName(model, domainName) ?: return ""
        return if (point.defaultUnit != null) point.defaultUnit!! else ""
    }

    fun getEquipDis() = "${hayStack.siteName}-${equipModel.name}-${profileConfiguration.nodeAddress}"
    fun getDeviceDis() = "${hayStack.siteName}-${deviceModel.name}-${profileConfiguration.nodeAddress}"

    private fun getPointByDomainName(modelDefinition: SeventyFiveFProfileDirective, domainName: String): SeventyFiveFProfilePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    fun updateTestRelay(relay: ConfigState, status: Boolean) {
        viewState.value.apply {
            when (relay) {
                relay1Config -> testRelay1 = status
                relay2Config -> testRelay2 = status
                relay3Config -> testRelay3 = status
                relay4Config -> testRelay4 = status
                relay5Config -> testRelay5 = status
                relay6Config -> testRelay6 = status
            }
            sendTestSignal()
        }
    }

    fun updateTestAnalogOut(analogOut: Int, value: Int) {
        viewState.value.apply {
            when (analogOut) {
                1 -> testAnalogOut1 = value
                2 -> testAnalogOut2 = value
                3 -> testAnalogOut3 = value
            }
            sendTestSignal()
        }
    }

    private fun sendTestSignal() {
        CcuLog.i(L.TAG_CCU_HSHST, "Test signal triggered")

        if (equipRef == null) {
            CcuLog.i(L.TAG_CCU_HSHST, "---HyperStat test signal sendControl: Not Ready---")
            return
        }
        if (!Globals.getInstance().isTestMode)
            Globals.getInstance().isTestMode = true

        val testMessage = HyperStatMessageGenerator.getControlMessage(deviceAddress.toInt())

        fun getAnalogOut(value: Int) = HyperStatAnalogOutputControl_t.newBuilder().setPercent(value).build()

        viewState.value.apply {
            testMessage.setRelay1(testRelay1)
            testMessage.setRelay2(testRelay2)
            testMessage.setRelay3(testRelay3)
            testMessage.setRelay4(testRelay4)
            testMessage.setRelay5(testRelay5)
            testMessage.setRelay6(testRelay6)

            testMessage.setAnalogOut1(getAnalogOut(testAnalogOut1))
            testMessage.setAnalogOut2(getAnalogOut(testAnalogOut2))
            testMessage.setAnalogOut3(getAnalogOut(testAnalogOut3))
        }

        HyperStatMessageSender.writeControlMessage(
                testMessage!!.build(), deviceAddress.toInt(),
                MessageType.HYPERSTAT_CONTROLS_MESSAGE, false
        )
        CcuLog.i(L.TAG_CCU_HSHST, "---HyperStat test signal sent---\n ${testMessage.build()}")
    }
}

fun updateFanMode(isReconfigure: Boolean, equip: HyperStatEquip, fanLevel: Int) {
    val possibleFanMode = getPossibleFanModeSettings(fanLevel)
    if (possibleFanMode == PossibleFanMode.OFF) {
        equip.fanOpMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }
    if (!isReconfigure) {
        equip.fanOpMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
    }
}

fun logIt(msg: String) {
    CcuLog.i(L.TAG_CCU_HSHST, msg)
}