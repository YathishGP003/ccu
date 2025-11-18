package a75f.io.renatus.profiles.hss

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.hypersplit.HyperSplitMessageGenerator
import a75f.io.device.mesh.hypersplit.HyperSplitMessageSender
import a75f.io.device.serial.MessageType
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.EconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitProfile
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.UniversalInputs
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.PossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.getDomainHyperStatSplitDevice
import a75f.io.logic.bo.building.statprofiles.util.getPossibleFanModeSettings
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.formattedToastMessage
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.CopyConfiguration
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuState
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe2UvViewState
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe4UvViewState
import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

open class HyperStatSplitViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
     var deviceAddress by Delegates.notNull<Short>()

     lateinit var hssProfile: HyperStatSplitProfile
    lateinit var profileConfiguration: HyperStatSplitConfiguration

    lateinit var equipModel : SeventyFiveFProfileDirective
    lateinit var deviceModel : SeventyFiveFDeviceDirective
    open lateinit var viewState: MutableState<HyperStatSplitState>

    lateinit var context: Context
    lateinit var hayStack: CCUHsApi

    lateinit var pairingCompleteListener: OnPairingCompleteListener
    protected var saveJob : Job? = null
    var openCancelDialog by mutableStateOf(false)

    lateinit var temperatureOffsetsList: List<String>
    private lateinit var fanLowMedHighSpeedsList: List<String>
    lateinit var outsideDamperMinOpenList: List<String>
    lateinit var exhaustFanThresholdList: List<String>
    lateinit var exhaustFanHysteresisList: List<String>
    lateinit var zoneCO2DamperOpeningRateList: List<String>
    lateinit var prePurgeOutsideDamperOpenList: List<String>
    lateinit var zoneCO2ThresholdList: List<String>
    lateinit var zoneCO2TargetList: List<String>
    private lateinit var zoneVOCThresholdList: List<String>
    private lateinit var zoneVOCTargetList: List<String>
    lateinit var zonePM2p5TargetList: List<String>

    var minMaxVoltage = List(11) { Option(it, it.toString()) }
    var testVoltage = List(101) { Option(it, it.toString()) }
    private val _isDisabled = MutableLiveData(false)
    val isDisabled: LiveData<Boolean> = _isDisabled
    private val _isReloadRequired = MutableLiveData(false)
    val isReloadRequired: LiveData<Boolean> = _isReloadRequired
    var equipRef: String? = null
    open fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        viewState = mutableStateOf(HyperStatSplitState())
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
    }


    fun getEquipDis() = "${hayStack.siteName}-${equipModel.name}-${profileConfiguration.nodeAddress}"
    fun getDeviceDis() = "${hayStack.siteName}-${deviceModel.name}-${profileConfiguration.nodeAddress}"

    fun addEquipment(config: ProfileConfiguration, equipModel: SeventyFiveFProfileDirective, deviceModel: SeventyFiveFDeviceDirective): String {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val equipId = equipBuilder.buildEquipAndPoints(config, equipModel, hayStack.site!!.id, getEquipDis())
        deviceBuilder.buildDeviceAndPoints(config, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        return equipId
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

    fun handleTestRelayChanged(index: Int, isChecked: Boolean) {
        val state =  if (isChecked) 1.0 else 0.0
        if(equipRef!=null){
            val device = getDomainHyperStatSplitDevice(equipRef!!)
            when (index) {
                0 -> device.relay1.writePointValue(state)
                1 -> device.relay2.writePointValue(state)
                2 -> device.relay3.writePointValue(state)
                3 -> device.relay4.writePointValue(state)
                4 -> device.relay5.writePointValue(state)
                5 -> device.relay6.writePointValue(state)
                6 -> device.relay7.writePointValue(state)
                7 -> device.relay8.writePointValue(state)
            }
            sendTestSignalControlMessage()
            CcuLog.d(L.TAG_CCU_HSHST, "R1 ${device.relay1.readPointValue()} R2 ${device.relay2.readPointValue()} R3 ${device.relay3.readPointValue()} R4 ${device.relay4.readPointValue()} A1 ${device.relay5.readPointValue()}, R6 ${device.relay6.readPointValue()} r7 ${device.relay7.readPointValue()} R8 ${device.relay8.readPointValue()} , A1 ${device.analog1Out.readPointValue()} A2 ${device.analog2Out.readPointValue()} A3 ${device.analog3Out.readPointValue()} A4 ${device.analog4Out.readPointValue()}")
        } else { showToast(context.getString(R.string.please_pair_equip),context)
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Please pair equip to send test command")
            return
        }

    }

    open fun handleTestAnalogOutChanged(index: Int, value: Double) {

        if(equipRef!=null){
            val device = getDomainHyperStatSplitDevice(equipRef!!)
            when (index) {
                0 -> device.analog1Out.writePointValue(value)
                1 -> device.analog2Out.writePointValue(value)
                2 -> device.analog3Out.writePointValue(value)
                3 -> device.analog4Out.writePointValue(value)
            }
            sendTestSignalControlMessage()
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "R1 ${device.relay1.readPointValue()} R2 ${device.relay2.readPointValue()} R3 ${device.relay3.readPointValue()} R4 ${device.relay4.readPointValue()} A1 ${device.relay5.readPointValue()}, R6 ${device.relay6.readPointValue()} r7 ${device.relay7.readPointValue()} R8 ${device.relay8.readPointValue()} , A1 ${device.analog1Out.readPointValue()} A2 ${device.analog2Out.readPointValue()} A3 ${device.analog3Out.readPointValue()} A4 ${device.analog4Out.readPointValue()}")
        } else {
            showToast(context.getString(R.string.please_pair_equip), context)
            CcuLog.d(L.TAG_CCU_HSSPLIT_CPUECON, "Please pair equip to send test command")
            return
        }
    }

    open fun sendTestSignalControlMessage() {
        if (!Globals.getInstance().isTestMode) Globals.getInstance().isTestMode = true
        HyperSplitMessageSender.writeControlMessage(
            HyperSplitMessageGenerator.getControlMessage(
                deviceAddress.toInt(),
                equipRef
            ).build(), deviceAddress.toInt(),
            MessageType.HYPERSPLIT_CONTROLS_MESSAGE, false
        )
    }


    fun getTestSignalForRelay(index: Int): Boolean {

        if (equipRef != null) {
            val device = getDomainHyperStatSplitDevice(equipRef!!)
            CcuLog.d("USER_TEST","DEVICES $device")
            return when (index) {
                0 -> device.relay1.readPointValue() > 0
                1 -> device.relay2.readPointValue() > 0
                2 -> device.relay3.readPointValue() > 0
                3 -> device.relay4.readPointValue() > 0
                4 -> device.relay5.readPointValue() > 0
                5 -> device.relay6.readPointValue() > 0
                6 -> device.relay7.readPointValue() > 0
                7 -> device.relay8.readPointValue() > 0
                else -> false
            }
        }
        return false
    }

    fun getTestSignalForAnalogOut(index: Int): Double {
        if(equipRef!=null){
            val equip = getDomainHyperStatSplitDevice(equipRef!!)
            return when (index) {
                0 -> equip.analog1Out.readPointValue()
                1 -> equip.analog2Out.readPointValue()
                2 -> equip.analog3Out.readPointValue()
                3 -> equip.analog4Out.readPointValue()
                else -> 0.0
            }
        }
        return 0.0
    }

    open fun hasUnsavedChanges() : Boolean { return false }


    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    fun applyCopiedConfiguration() {
        val config = CopyConfiguration.getCopiedConfiguration()
        val copiedProfileType = CopyConfiguration.getSelectedProfileType()
        when (copiedProfileType) {

            ProfileType.HYPERSTATSPLIT_4PIPE_UV -> {
                viewState.value = Pipe4UvViewState.fromProfileConfigToState(config as Pipe4UVConfiguration)
            }

            ProfileType.HYPERSTATSPLIT_CPU -> {
                viewState.value = HyperStatSplitCpuState.fromProfileConfigToState(config as HyperStatSplitCpuConfiguration)
            }

            ProfileType.HYPERSTATSPLIT_2PIPE_UV -> {
                viewState.value = Pipe2UvViewState.fromProfileConfigState(config as Pipe2UVConfiguration)
            }

            else -> {
                CcuLog.i(L.TAG_CCU_COPY_CONFIGURATION, "Unsupported profile type for copy configuration: $profileType")
            }
        }
        reloadUiRequired()
        disablePasteConfiguration()
        formattedToastMessage(
            context.getString(R.string.Toast_Success_Message_paste_Configuration),
            context
        )
    }
    private  fun reloadUiRequired(){
        _isReloadRequired.value = !_isReloadRequired.value!!
    }

      fun isCopiedConfigurationAvailable() {
        val selectedProfileType = CopyConfiguration.getSelectedProfileType()
        if (selectedProfileType != null && selectedProfileType == profileType) {
            disablePasteConfiguration()
        }
    }

    fun disablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = !_isDisabled.value!!
        }
    }

    // common methods for need for UV viewmodel


    fun isPrePurgeEnabled() : Boolean {
        return this.viewState.value.prePurge
    }

    fun isOAODamperAOEnabled(enumValue: Int) = isAnyAnalogMappedToControl(enumValue)

    fun initializeLists() {
        temperatureOffsetsList = getListByDomainName(DomainName.temperatureOffset, equipModel)

        fanLowMedHighSpeedsList = getListByDomainName(DomainName.analog1FanLow, equipModel)

        outsideDamperMinOpenList = getListByDomainName(DomainName.outsideDamperMinOpenDuringRecirculation, equipModel)
        exhaustFanThresholdList = getListByDomainName(DomainName.exhaustFanStage1Threshold, equipModel)
        exhaustFanHysteresisList = getListByDomainName(DomainName.exhaustFanHysteresis, equipModel)

        prePurgeOutsideDamperOpenList = getListByDomainName(DomainName.prePurgeOutsideDamperOpen, equipModel)
        zoneCO2DamperOpeningRateList = getListByDomainName(DomainName.co2DamperOpeningRate, equipModel)
        zoneCO2ThresholdList = getListByDomainName(DomainName.co2Threshold, equipModel)
        zoneCO2TargetList = getListByDomainName(DomainName.co2Target, equipModel)
        zoneVOCThresholdList = getListByDomainName(DomainName.vocThreshold, equipModel)
        zoneVOCTargetList = getListByDomainName(DomainName.vocTarget, equipModel)
        zonePM2p5TargetList = getListByDomainName(DomainName.pm25Target, equipModel)
    }


    fun isCompressorMappedWithAnyRelayForUV(): Boolean {
        return (isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE1)||
                isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE2)||
                isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE3))
    }

    fun isChangeOverCoolingMappedForUV(ignoreConfig: ConfigState) = isAnyRelayMappedToControl(
        CpuRelayType.CHANGE_OVER_O_COOLING, ignoreConfig)

    fun isChangeOverHeatingMappedForUV(ignoreConfig: ConfigState) = isAnyRelayMappedToControl(
        CpuRelayType.CHANGE_OVER_B_HEATING, ignoreConfig)

     fun isAnyRelayMappedToControl(type: CpuRelayType): Boolean {

        fun isEnabledAndMapped(configState: ConfigState): Boolean {
            return (configState.enabled && configState.association == type.ordinal)
        }
        return (isEnabledAndMapped(this.viewState.value.relay1Config) ||
                isEnabledAndMapped(this.viewState.value.relay2Config) ||
                isEnabledAndMapped(this.viewState.value.relay3Config) ||
                isEnabledAndMapped(this.viewState.value.relay4Config) ||
                isEnabledAndMapped(this.viewState.value.relay5Config) ||
                isEnabledAndMapped(this.viewState.value.relay6Config) ||
                isEnabledAndMapped(this.viewState.value.relay7Config) ||
                isEnabledAndMapped(this.viewState.value.relay8Config))
    }

    fun isCoolingAOEnabled(enumValue : Int) = isAnyAnalogMappedToControl(enumValue)
    fun isHeatingAOEnabled(enumValue : Int) = isAnyAnalogMappedToControl(enumValue)
    fun isLinearFanAOEnabled(enumValue : Int) = isAnyAnalogMappedToControl(enumValue)
    fun isStagedFanAOEnabled(enumValue : Int) = isAnyAnalogMappedToControl(enumValue)
    fun isReturnDamperAOEnabled(enumValue : Int) = isAnyAnalogMappedToControl(enumValue)
    fun isCompressorAOEnabled(enumValue : Int) = isAnyAnalogMappedToControl(enumValue)
    fun isDamperModulationAOEnabled(enumValue : Int) = isAnyAnalogMappedToControl(enumValue)

    fun isAnyAnalogMappedToControl(enumValue: Int): Boolean {
        val enumType = when (profileConfiguration) {
            is HyperStatSplitCpuConfiguration -> CpuAnalogControlType.values()
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.values()
            is Pipe2UVConfiguration -> Pipe2UvAnalogOutControls.values()
            else -> CpuAnalogControlType.values()
        }

        return (
                (this.viewState.value.analogOut1Enabled && this.viewState.value.analogOut1Association == enumType[enumValue].ordinal) ||
                        (this.viewState.value.analogOut2Enabled && this.viewState.value.analogOut2Association == enumType[enumValue].ordinal) ||
                        (this.viewState.value.analogOut3Enabled && this.viewState.value.analogOut3Association == enumType[enumValue].ordinal) ||
                        (this.viewState.value.analogOut4Enabled && this.viewState.value.analogOut4Association == enumType[enumValue].ordinal)
                )
    }

     fun isAnyUniversalInMapped(type: UniversalInputs): Boolean {
        return (
                (this.viewState.value.universalIn1Config.enabled && this.viewState.value.universalIn1Config.association == type.ordinal) ||
                        (this.viewState.value.universalIn2Config.enabled && this.viewState.value.universalIn2Config.association == type.ordinal) ||
                        (this.viewState.value.universalIn3Config.enabled && this.viewState.value.universalIn3Config.association == type.ordinal) ||
                        (this.viewState.value.universalIn4Config.enabled && this.viewState.value.universalIn4Config.association == type.ordinal) ||
                        (this.viewState.value.universalIn5Config.enabled && this.viewState.value.universalIn5Config.association == type.ordinal) ||
                        (this.viewState.value.universalIn6Config.enabled && this.viewState.value.universalIn6Config.association == type.ordinal) ||
                        (this.viewState.value.universalIn7Config.enabled && this.viewState.value.universalIn7Config.association == type.ordinal) ||
                        (this.viewState.value.universalIn8Config.enabled && this.viewState.value.universalIn8Config.association == type.ordinal)
                )
    }

    fun isUniversalInDuplicated(type: UniversalInputs): Boolean {
        var nInstances = 0

        if (this.viewState.value.universalIn1Config.enabled && this.viewState.value.universalIn1Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn2Config.enabled && this.viewState.value.universalIn2Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn3Config.enabled && this.viewState.value.universalIn3Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn4Config.enabled && this.viewState.value.universalIn4Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn5Config.enabled && this.viewState.value.universalIn5Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn6Config.enabled && this.viewState.value.universalIn6Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn7Config.enabled && this.viewState.value.universalIn7Config.association == type.ordinal) nInstances++
        if (this.viewState.value.universalIn8Config.enabled && this.viewState.value.universalIn8Config.association == type.ordinal) nInstances++

        return nInstances > 1
    }

     fun isAnySensorBusMapped(type: EconSensorBusTempAssociation): Boolean {
        return (
                (this.viewState.value.sensorAddress0.enabled && this.viewState.value.sensorAddress0.association == type.ordinal) ||
                        (this.viewState.value.sensorAddress1.enabled && this.viewState.value.sensorAddress1.association == type.ordinal) ||
                        (this.viewState.value.sensorAddress2.enabled && this.viewState.value.sensorAddress2.association == type.ordinal)
                )
    }

     fun isSensorBusDuplicated(type: EconSensorBusTempAssociation): Boolean {
        var nInstances = 0

        if (this.viewState.value.sensorAddress0.enabled && this.viewState.value.sensorAddress0.association == type.ordinal) nInstances++
        if (this.viewState.value.sensorAddress1.enabled && this.viewState.value.sensorAddress1.association == type.ordinal) nInstances++
        if (this.viewState.value.sensorAddress2.enabled && this.viewState.value.sensorAddress2.association == type.ordinal) nInstances++

        return nInstances > 1
    }

    fun isHeatStage1RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.HEATING_STAGE1)
    fun isHeatStage2RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.HEATING_STAGE2)
    fun isHeatStage3RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.HEATING_STAGE3)
    fun isCoolStage1RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COOLING_STAGE1)
    fun isCoolStage2RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COOLING_STAGE2)
    fun isCoolStage3RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COOLING_STAGE3)
    fun isFanLowRelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.FAN_LOW_SPEED)
    fun isFanMediumRelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.FAN_MEDIUM_SPEED)
    fun isFanHighRelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.FAN_HIGH_SPEED)
    fun isCompressorStage1RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE1)
    fun isCompressorStage2RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE2)
    fun isCompressorStage3RelayEnabled() = isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE3)
    fun isCompressorMappedWithAnyRelay(): Boolean {
        return (isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE1)||
                isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE2)||
                isAnyRelayMappedToControl(CpuRelayType.COMPRESSOR_STAGE3))
    }
    fun isChangeOverCoolingMapped(ignoreConfig: ConfigState) = isAnyRelayMappedToControl(
        CpuRelayType.CHANGE_OVER_O_COOLING, ignoreConfig)
    fun isChangeOverHeatingMapped(ignoreConfig: ConfigState) = isAnyRelayMappedToControl(
        CpuRelayType.CHANGE_OVER_B_HEATING, ignoreConfig)

    fun isAO1MappedToFan() : Boolean {
        return this.viewState.value.analogOut1Enabled &&
                (this.viewState.value.analogOut1Association == CpuAnalogControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut1Association == CpuAnalogControlType.STAGED_FAN.ordinal)
    }
    fun isAO2MappedToFan() : Boolean {
        return this.viewState.value.analogOut2Enabled &&
                (this.viewState.value.analogOut2Association == CpuAnalogControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut2Association == CpuAnalogControlType.STAGED_FAN.ordinal)
    }
    fun isAO3MappedToFan() : Boolean {
        return this.viewState.value.analogOut3Enabled &&
                (this.viewState.value.analogOut3Association == CpuAnalogControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut3Association == CpuAnalogControlType.STAGED_FAN.ordinal)
    }
    fun isAO4MappedToFan() : Boolean {
        return this.viewState.value.analogOut4Enabled &&
                (this.viewState.value.analogOut4Association == CpuAnalogControlType.LINEAR_FAN.ordinal ||
                        this.viewState.value.analogOut4Association == CpuAnalogControlType.STAGED_FAN.ordinal)
    }


    private fun isAnyRelayMappedToControl(
        type: CpuRelayType,
        ignoreConfig: ConfigState
    ): Boolean {
        fun isEnabledAndMapped(configState: ConfigState): Boolean {
            return (configState != ignoreConfig && configState.enabled && configState.association == type.ordinal)
        }
        return (isEnabledAndMapped(this.viewState.value.relay1Config) ||
                isEnabledAndMapped(this.viewState.value.relay2Config) ||
                isEnabledAndMapped(this.viewState.value.relay3Config) ||
                isEnabledAndMapped(this.viewState.value.relay4Config) ||
                isEnabledAndMapped(this.viewState.value.relay5Config) ||
                isEnabledAndMapped(this.viewState.value.relay6Config) ||
                isEnabledAndMapped(this.viewState.value.relay7Config) ||
                isEnabledAndMapped(this.viewState.value.relay8Config))
    }

    fun isAnalogEnabledAndMapped(type: CpuAnalogControlType, enabled: Boolean, association: Int) =
        (enabled && association == type.ordinal)


    // getting the profile based enum value
    fun getProfileBasedEnumValueAnalog(enumName: String): Int {
        return when (profileConfiguration) {
            is HyperStatSplitCpuConfiguration -> CpuAnalogControlType.valueOf(enumName).ordinal
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.valueOf(enumName).ordinal
            is Pipe2UVConfiguration ->Pipe2UvAnalogOutControls.valueOf(enumName).ordinal
            else -> CpuAnalogControlType.valueOf(enumName).ordinal
        }
    }

    // getting the profile based enum value
    fun getProfileBasedEnumValueRelayType(enumName: String): Int {
        return when (profileConfiguration) {
            is HyperStatSplitCpuConfiguration -> CpuRelayType.valueOf(enumName).ordinal
            is Pipe4UVConfiguration -> Pipe4UVRelayControls.valueOf(enumName).ordinal
            is Pipe2UVConfiguration -> Pipe2UVRelayControls.valueOf(enumName).ordinal
            else -> {
                HyperStatSplitControlType.valueOf(enumName).ordinal
            }
        }
    }

    fun updateFanMode(isReconfigure: Boolean, equip: HyperStatSplitEquip, fanLevel: Int) {
        fun resetFanToOff() = equip.fanOpMode.writePointValue(StandaloneFanStage.OFF.ordinal.toDouble())
        val possibleFanMode = getPossibleFanModeSettings(fanLevel)
        val cacheStorage = FanModeCacheStorage.getHyperStatSplitFanModeCache()
        if (possibleFanMode == PossibleFanMode.OFF) {
            resetFanToOff()
            cacheStorage.removeFanModeFromCache(equip.equipRef)
            return
        }
        if (possibleFanMode == PossibleFanMode.AUTO) {
            cacheStorage.removeFanModeFromCache(equip.equipRef)
            equip.fanOpMode.writePointValue(StandaloneFanStage.AUTO.ordinal.toDouble())
            return
        }


        if (isReconfigure) {
            val currentFanMode = StandaloneFanStage.values()[equip.fanOpMode.readPriorityVal().toInt()]
            fun isWithinLow(): Boolean {
                return (currentFanMode.ordinal in listOf(
                    StandaloneFanStage.LOW_ALL_TIME.ordinal,
                    StandaloneFanStage.LOW_OCC.ordinal,
                    StandaloneFanStage.LOW_CUR_OCC.ordinal))
            }
            fun isWithinMedium(): Boolean {
                return (currentFanMode.ordinal in listOf(
                    StandaloneFanStage.MEDIUM_ALL_TIME.ordinal,
                    StandaloneFanStage.MEDIUM_OCC.ordinal,
                    StandaloneFanStage.MEDIUM_CUR_OCC.ordinal))
            }
            fun isWithinHigh(): Boolean {
                return (currentFanMode.ordinal in listOf(
                    StandaloneFanStage.HIGH_ALL_TIME.ordinal,
                    StandaloneFanStage.HIGH_OCC.ordinal,
                    StandaloneFanStage.HIGH_CUR_OCC.ordinal))
            }
            if (currentFanMode != StandaloneFanStage.AUTO) {
                when (possibleFanMode) {
                    PossibleFanMode.LOW -> {
                        if (!isWithinLow()) {
                            resetFanToOff()
                        }
                    }

                    PossibleFanMode.MED -> {
                        if (!isWithinMedium()) {
                            resetFanToOff()
                        }
                    }

                    PossibleFanMode.HIGH -> {
                        if (!isWithinHigh()) {
                            resetFanToOff()
                        }
                    }

                    PossibleFanMode.LOW_MED -> {
                        if (isWithinHigh()) {
                            resetFanToOff()
                        }
                    }

                    PossibleFanMode.LOW_HIGH -> {
                        if (isWithinMedium()) {
                            resetFanToOff()
                        }
                    }

                    PossibleFanMode.MED_HIGH -> {
                        if (isWithinLow()) {
                            resetFanToOff()
                        }
                    }

                    else -> {}
                }
            }
        } else {
            equip.fanOpMode.writePointValue(StandaloneFanStage.AUTO.ordinal.toDouble())
        }
    }

}