package a75f.io.renatus.profiles.mystat.viewmodels

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.mystat.MyStatMsgSender
import a75f.io.device.mesh.mystat.getMyStatControlMessage
import a75f.io.device.mesh.mystat.getMyStatDomainDevice
import a75f.io.device.serial.MessageType
import a75f.io.domain.api.Domain
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4AnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.profiles.MyStatProfile
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatDeviceType
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatPossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleFanModeSettings
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.formattedToastMessage
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.CopyConfiguration
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.mystat.viewstates.MyStatCpuViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatHpuViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatPipe2ViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatPipe4ViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewState
import a75f.io.renatus.profiles.mystat.viewstates.MyStatViewStateUtil
import a75f.io.renatus.profiles.system.advancedahu.Option
import a75f.io.renatus.util.showErrorDialog
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.text.Html
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

/**
 * Created by Manjunath K on 15-01-2025.
 */

open class MyStatViewModel(application: Application) : AndroidViewModel(application) {
    protected var deviceAddress by Delegates.notNull<Short>()
    protected lateinit var myStatProfile: MyStatProfile

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    lateinit var profileConfiguration: MyStatConfiguration
    lateinit var context: Context
    lateinit var hayStack: CCUHsApi
    lateinit var equipModel: SeventyFiveFProfileDirective
    lateinit var deviceModel: SeventyFiveFDeviceDirective
    lateinit var temperatureOffsetsList: List<String>
    lateinit var pairingCompleteListener: OnPairingCompleteListener
    lateinit var devicesVersion: String

    var damperOpeningRate = (10..100 step 10).toList().map { Option(it, it.toString()) }
    private val _isDisabled = MutableLiveData(false)
    val isDisabled: LiveData<Boolean> = _isDisabled
    private val _isReloadRequired = MutableLiveData(false)
    val isReloadRequired: LiveData<Boolean> = _isReloadRequired

    var equipRef: String? = null
    protected var saveJob: Job? = null
    open var viewState: MutableState<MyStatViewState> = mutableStateOf(MyStatViewState())

    open fun init(bundle: Bundle, context: Context, hayStack: CCUHsApi) {
        this.context = context
        this.hayStack = hayStack
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        devicesVersion = bundle.getString(FragmentCommonBundleArgs.DEVICE_VERSION) ?: MyStatDeviceType.MYSTAT_V1.name
        deviceModel = ModelLoader.getMyStatDeviceModel() as SeventyFiveFDeviceDirective
    }

    open fun saveConfiguration() {}
    open fun getAnalogStatIndex() = 0

    fun disablePasteConfiguration() {
        viewModelScope.launch(Dispatchers.Main) {
            _isDisabled.value = !_isDisabled.value!!
        }
    }

    fun updateDeviceType(equipRef: String) {
        if (equipRef.equals("null", true).not()) {
            val myStatDevice = Domain.getEquipDevices()[equipRef] as MyStatDevice?
            if (myStatDevice != null) {
                devicesVersion =
                    if (myStatDevice.mystatDeviceVersion.readPointValue().toInt() == 1)
                        MyStatDeviceType.MYSTAT_V2.name
                    else MyStatDeviceType.MYSTAT_V1.name
            }
        }
        isCopiedConfigurationAvailable()
    }

    fun isCopiedConfigurationAvailable() {
        val selectedProfileType = CopyConfiguration.getSelectedProfileType()
        if (selectedProfileType != null && selectedProfileType == profileType && CopyConfiguration.getMyStatDeviceType()
                .equals(devicesVersion, true)
        ) {
            disablePasteConfiguration()
        }
    }


    fun applyCopiedConfiguration(updatedViewState: MyStatViewState) {

        val config = CopyConfiguration.getCopiedConfiguration()
        if (CopyConfiguration.getSelectedProfileType() == ProfileType.MYSTAT_CPU) {
            viewState.value = MyStatViewStateUtil.cpuConfigToState(
                config as MyStatCpuConfiguration,
                updatedViewState as MyStatCpuViewState
            )
        } else if (CopyConfiguration.getSelectedProfileType() == ProfileType.MYSTAT_HPU) {
            viewState.value = MyStatViewStateUtil.hpuConfigToState(
                config as MyStatHpuConfiguration,
                updatedViewState as MyStatHpuViewState
            )
        } else if (CopyConfiguration.getSelectedProfileType() == ProfileType.MYSTAT_PIPE2) {
            viewState.value = MyStatViewStateUtil.pipe2ConfigToState(
                config as MyStatPipe2Configuration,
                updatedViewState as MyStatPipe2ViewState
            )
        }
        else if (CopyConfiguration.getSelectedProfileType() == ProfileType.MYSTAT_PIPE4) {
            viewState.value = MyStatViewStateUtil.pipe4ConfigToState(
                config as MyStatPipe4Configuration,
                updatedViewState as MyStatPipe4ViewState
            )
        }

        profileConfiguration.getAnalogOutDefaultValueForMyStatV1(config as MyStatConfiguration,devicesVersion)
        reloadUiRequired()
        disablePasteConfiguration()
        formattedToastMessage(
            context.getString(R.string.Toast_Success_Message_paste_Configuration),
            context
        )
    }

    private fun reloadUiRequired() {
        _isReloadRequired.value = !_isReloadRequired.value!!
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    fun getEquipDis() = "${hayStack.siteName}-${equipModel.name}-${profileConfiguration.nodeAddress}"
    fun getDeviceDis() = "${hayStack.siteName}-${deviceModel.name}-${profileConfiguration.nodeAddress}"

    fun addEquipment(config: MyStatConfiguration, equipModel: SeventyFiveFProfileDirective, deviceModel: SeventyFiveFDeviceDirective): String  {
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val equipId = equipBuilder.buildEquipAndPoints(config, equipModel, hayStack.site!!.id, getEquipDis())
        val deviceRef = deviceBuilder.buildDeviceAndPoints(config, deviceModel, equipId, hayStack.site!!.id, getDeviceDis())
        config.universalInUnit(deviceRef)
        return equipId
    }

    fun updateFanMode(isReconfigure: Boolean, equip: MyStatEquip, fanLevel: Int) {

        val possibleFanMode = getMyStatPossibleFanModeSettings(fanLevel)

        if (possibleFanMode == MyStatPossibleFanMode.OFF) {
            equip.fanOpMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
            FanModeCacheStorage.getMyStatFanModeCache().removeFanModeFromCache(equip.equipRef)
            return
        }

        if (possibleFanMode == MyStatPossibleFanMode.AUTO) {
            equip.fanOpMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
            FanModeCacheStorage.getMyStatFanModeCache().removeFanModeFromCache(equip.equipRef)
            return
        }

        if (isReconfigure) {
            val currentFanMode = MyStatFanStages.values()[equip.fanOpMode.readPriorityVal().toInt()]
            if (currentFanMode != MyStatFanStages.AUTO) {
                if (possibleFanMode == MyStatPossibleFanMode.LOW && currentFanMode.ordinal > MyStatFanStages.LOW_ALL_TIME.ordinal) {
                    equip.fanOpMode.writePointValue(MyStatFanStages.OFF.ordinal.toDouble())
                }
                if (possibleFanMode == MyStatPossibleFanMode.HIGH && currentFanMode.ordinal < MyStatFanStages.HIGH_CUR_OCC.ordinal) {
                    equip.fanOpMode.writePointValue(MyStatFanStages.OFF.ordinal.toDouble())
                }
            }
        } else {
            equip.fanOpMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
        }
    }
    fun isV1() = devicesVersion.equals(MyStatDeviceType.MYSTAT_V1.name, true)

    fun getRelayStatus(relayIndex: Int): Boolean {

        if (equipRef != null) {
            val device = getMyStatDomainDevice("", equipRef!!)
            device.apply {
                return when (relayIndex) {
                    1 -> relay1.readPointValue() > 0
                    2 -> relay2.readPointValue() > 0
                    3 -> relay3.readPointValue() > 0
                    4 -> (if (isV1()) universalOut2 else universalOut1).readPointValue() > 0
                    5 -> (if (isV1()) universalOut1 else universalOut2).readPointValue() > 0
                    else -> false
                }
            }
        }
        return false
    }

    fun getAnalogValue(index: Int): Double {
        if (equipRef != null) {
            // Device ref is not required here so passing empty string
            val device = getMyStatDomainDevice("", equipRef!!)
            device.apply {
                return when(index) {
                    4 -> (if (isV1()) universalOut2 else universalOut1).readPointValue()
                    5 -> (if (isV1()) universalOut1 else universalOut2).readPointValue()
                    else -> 0.0
                }
            }
        }
        return 0.0
    }

    fun sendTestSignal(relayIndex: Int = 0, relayStatus: Double = 0.0) {

        if (equipRef != null) {
            // Device ref is not required here so passing empty string
            val device = getMyStatDomainDevice("", equipRef!!)
            device.apply {
                when (relayIndex) {
                    1 -> relay1.writePointValue(relayStatus)
                    2 -> relay2.writePointValue(relayStatus)
                    3 -> relay3.writePointValue(relayStatus)
                    4 -> (if (isV1()) universalOut2 else universalOut1).writePointValue(relayStatus)
                    5 -> (if (isV1()) universalOut1 else universalOut2).writePointValue(relayStatus)
                }
            }
            CcuLog.d(L.TAG_CCU_MSHST, "R1 ${device.relay1.readPointValue()} R2 ${device.relay2.readPointValue()} R3 ${device.relay3.readPointValue()} \n" +
                    "U1 ${device.universalOut1.readPointValue()} U2 ${device.universalOut2.readPointValue()}")
        } else {
            showToast("Please pair equip to send test command",context)
            CcuLog.d(L.TAG_CCU_MSHST, "Please pair equip to send test command")
            return
        }
        CcuLog.d(L.TAG_CCU_MSHST, "Sending mystat test signal")
        if (!Globals.getInstance().isTestMode) Globals.getInstance().isTestMode = true
        MyStatMsgSender.writeControlMessage(
            getMyStatControlMessage(deviceAddress.toInt()).build(),
            deviceAddress.toInt(),
            MessageType.MYSTAT_CONTROLS_MESSAGE,
            false
        )
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

    private fun getPointByDomainName(modelDefinition: SeventyFiveFProfileDirective, domainName: String): SeventyFiveFProfilePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }

    fun getUnit(domainName: String, model: SeventyFiveFProfileDirective): String {
        val point = getPointByDomainName(model, domainName) ?: return ""
        return if (point.defaultUnit != null) point.defaultUnit!! else ""
    }

    fun isValidConfiguration(isDcvMapped: Boolean): Boolean {
        var isValidConfig = true
        if (viewState.value.co2Control) {
            if (!isDcvMapped) {
                showErrorDialog(context, Html.fromHtml("<br>CO2 Control toggle is Enabled, but DCV Damper is not mapped.", Html.FROM_HTML_MODE_LEGACY))
                isValidConfig =  false
            }

        } else {
            if (isDcvMapped) {
                showErrorDialog(context, Html.fromHtml("<br>CO2 Control toggle is Disabled, but DCV Damper is mapped.", Html.FROM_HTML_MODE_LEGACY))
                isValidConfig =  false
            }
        }

        if (this is MyStatPipe2ViewModel) {
            if (viewState.value.isAnyRelayEnabledAndMapped(MyStatPipe2RelayMapping.FAN_LOW_VENTILATION.ordinal)
                && viewState.value.isAnyRelayEnabledAndMapped(MyStatPipe2RelayMapping.FAN_LOW_SPEED.ordinal)) {
                showErrorDialog(
                    context,
                    Html.fromHtml(
                        "The profile must not have <b>Fan Low Speed - Ventilation and Fan Low Speed</b> mapped. Please remove any one  mapping from the mapping.",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                )
                isValidConfig = false
            }
        }

        return isValidConfig
    }

    fun updateDeviceVersionTypePointVal(myStatEquipId: String) {
        val device = Domain.getEquipDevices()[myStatEquipId] as MyStatDevice
        device.mystatDeviceVersion.writePointValue(if (devicesVersion == MyStatDeviceType.MYSTAT_V2.name) 1.0 else 0.0)
        CcuLog.d(
            L.TAG_CCU_DOMAIN,
            "Device version updated to ${device.mystatDeviceVersion.readPointValue()} for equip $myStatEquipId"
        )

    }
}

