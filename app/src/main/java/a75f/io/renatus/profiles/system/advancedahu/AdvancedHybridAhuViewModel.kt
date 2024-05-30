package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.cm.getCMControlsMessage
import a75f.io.device.cm.sendControlMoteMessage
import a75f.io.device.connect.ConnectModbusSerialComm
import a75f.io.device.serial.MessageType
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.toDouble
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.getAnalogOutLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getCMRelayLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getConnectAnalogOutLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getConnectRelayLogicalPhysicalMap
import a75f.io.logic.bo.building.system.vav.config.AdvancedHybridAhuConfig
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 20-03-2024.
 */

open class AdvancedHybridAhuViewModel : ViewModel() {

    open var viewState = mutableStateOf(AdvancedHybridAhuState())
    lateinit var cmModel: SeventyFiveFProfileDirective
    lateinit var connectModel: SeventyFiveFProfileDirective
    lateinit var cmDeviceModel: SeventyFiveFDeviceDirective
    lateinit var connectDeviceModel: SeventyFiveFDeviceDirective
    lateinit var context: Context
    lateinit var profileConfiguration: AdvancedHybridAhuConfig
    lateinit var hayStack: CCUHsApi
    lateinit var cmEquipBuilder: ProfileEquipBuilder
    lateinit var connectEquipBuilder: ProfileEquipBuilder
    lateinit var cmDeviceBuilder: DeviceBuilder
    lateinit var connectDeviceBuilder: DeviceBuilder
    private var isEquipPaired = false
    private var isConnectModulePaired = false
    /**
     * This voltage values never going to be changed so hardcoded here
     */
    var minMaxVoltage = List(11) { Option(it, it.toString()) }
    var testVoltage = List(101) { Option(it, it.toString()) }

    var modelLoaded by mutableStateOf(false)

    /**
     * Initialize the ViewModel
     */
    fun init(context: Context, cmProfileModel: ModelDirective, connectProfileModel : ModelDirective, hayStack: CCUHsApi) {

        CcuLog.i(Domain.LOG_TAG, "Advanced AHU Init")

        this.hayStack = hayStack
        cmEquipBuilder = ProfileEquipBuilder(hayStack)
        connectEquipBuilder = ProfileEquipBuilder(hayStack)
        isEquipAvailable()
        cmModel = cmProfileModel as SeventyFiveFProfileDirective
        CcuLog.i(Domain.LOG_TAG, "Advanced AHU CM EquipModel Loaded")
        cmDeviceModel = ModelLoader.getCMDeviceModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "Advanced AHU CM Device Model Loaded")
        val cmEntityMapper = EntityMapper(cmModel)
        cmDeviceBuilder = DeviceBuilder(hayStack, cmEntityMapper)

        connectModel = connectProfileModel as SeventyFiveFProfileDirective
        CcuLog.i(Domain.LOG_TAG, "Advanced AHU Connect EquipModel Loaded")
        connectDeviceModel = ModelLoader.getConnectModuleDeviceModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "Advanced AHU Connect Device Model Loaded")
        val connectEntityMapper = EntityMapper(connectModel)
        connectDeviceBuilder = DeviceBuilder(hayStack, connectEntityMapper)

        this.context = context
        this.hayStack = hayStack
        CcuLog.i(Domain.LOG_TAG, "Advanced AHU Loaded")
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
                enums.add(Option(it.index, it.value))
            }
            enums
        } else {
            emptyList()
        }
    }


    fun getListByDomainName(domainName: String, model: SeventyFiveFProfileDirective): List<Option> {
        val valuesList: MutableList<Option> = mutableListOf()
        val point = getPointByDomainName(model, domainName) ?: return emptyList()

        if (point.valueConstraint is NumericConstraint) {
            val minVal = (point.valueConstraint as NumericConstraint).minValue
            val maxVal = (point.valueConstraint as NumericConstraint).maxValue
            val incVal = point.presentationData?.get("tagValueIncrement").toString().toDouble()
            var it = minVal
            var position = 0
            while (it <= maxVal && incVal > 0.0) {
                valuesList.add(Option(position++, ("%.2f").format(it)))
                it += incVal
            }
        }
        return valuesList
    }

    fun getUnit(domainName: String, model: SeventyFiveFProfileDirective): String {
        val point = getPointByDomainName(model, domainName) ?: return ""
        return if (point.defaultUnit != null) point.defaultUnit!! else ""
    }

    private fun getPointByDomainName(
        modelDefinition: SeventyFiveFProfileDirective, domainName: String
    ): SeventyFiveFProfilePointDef? {
        return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
    }

    fun isPressureEnabled() = isAnyAnalogMappedToControl(ControlType.PRESSURE_BASED_FAN_CONTROL)
    fun isSATCoolingEnabled() = isAnyAnalogMappedToControl(ControlType.SAT_BASED_COOLING_CONTROL)
    fun isSATHeatingEnabled() = isAnyAnalogMappedToControl(ControlType.SAT_BASED_HEATING_CONTROL)
    fun isCoolingLoadEnabled() = isAnyAnalogMappedToControl(ControlType.LOAD_BASED_COOLING_CONTROL)
    fun isFanLoadEnabled() = isAnyAnalogMappedToControl(ControlType.LOAD_BASED_FAN_CONTROL)
    fun isHeatLoadEnabled() = isAnyAnalogMappedToControl(ControlType.LOAD_BASED_HEATING_CONTROL)
    fun isDampersEnabled() = isAnyAnalogMappedToControl(ControlType.CO2_BASED_DAMPER_CONTROL)
    fun isCompositeEnabled() = isAnyAnalogMappedToControl(ControlType.COMPOSITE)
    private fun isAnyAnalogMappedToControl(type: ControlType): Boolean {
        return ((this.viewState.value.analogOut1Enabled && this.viewState.value.analogOut1Association == type.ordinal)
                || (this.viewState.value.analogOut2Enabled && this.viewState.value.analogOut2Association == type.ordinal)
                || (this.viewState.value.analogOut3Enabled && this.viewState.value.analogOut3Association == type.ordinal)
                || (this.viewState.value.analogOut4Enabled && this.viewState.value.analogOut4Association == type.ordinal))
    }

    fun isConnectCoolingLoadEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.LOAD_BASED_COOLING_CONTROL)
    fun isConnectFanLoadEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.LOAD_BASED_FAN_CONTROL)
    fun isConnectHeatLoadEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.LOAD_BASED_HEATING_CONTROL)
    fun isConnectCompositeEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.COMPOSITE)
    fun isConnectDampersEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.CO2_BASED_DAMPER_CONTROL)
    private fun isAnyConnectAnalogMappedToControl(type: ConnectControlType): Boolean {
        return ((this.viewState.value.connectAnalogOut1Enabled && this.viewState.value.connectAnalogOut1Association == type.ordinal)
                || (this.viewState.value.connectAnalogOut2Enabled && this.viewState.value.connectAnalogOut2Association == type.ordinal)
                || (this.viewState.value.connectAnalogOut3Enabled && this.viewState.value.connectAnalogOut3Association == type.ordinal)
                || (this.viewState.value.connectAnalogOut4Enabled && this.viewState.value.connectAnalogOut4Association == type.ordinal))
    }

    fun isAnalogEnabledAndMapped(type: ControlType, enabled: Boolean, association: Int) =
        (enabled && association == type.ordinal)

    fun isAnalogEnabledAndMapped(type: ConnectControlType, enabled: Boolean, association: Int) =
        (enabled && association == type.ordinal)

    open fun saveConfiguration() {
        // Implemented at sub class
    }
    fun getModelDefaultValue(relayState: ConfigState): Int {
        lateinit var domainName: String
        when(relayState) {
            viewState.value.relay1Config -> domainName = DomainName.relay1OutputAssociation
            viewState.value.relay2Config -> domainName = DomainName.relay2OutputAssociation
            viewState.value.relay3Config -> domainName = DomainName.relay3OutputAssociation
            viewState.value.relay4Config -> domainName = DomainName.relay4OutputAssociation
            viewState.value.relay5Config -> domainName = DomainName.relay5OutputAssociation
            viewState.value.relay6Config -> domainName = DomainName.relay6OutputAssociation
            viewState.value.relay7Config -> domainName = DomainName.relay7OutputAssociation
            viewState.value.relay8Config -> domainName = DomainName.relay8OutputAssociation
        }
        return profileConfiguration.cmConfiguration. getDefaultValConfig(domainName, profileConfiguration.cmModel).currentVal.toInt()
    }

    fun sendCMRelayTestCommand(relayIndex : Int, testCommand : Boolean) {
        if (!isEquipPaired) {
            CcuLog.i(Domain.LOG_TAG, "System Equip does not exist")
            return
        }
        Globals.getInstance().isTestMode = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Globals.getInstance().isTestMode = true
                val physicalPoint = getPhysicalPointForRelayIndex(relayIndex, false)
                physicalPoint?.let {
                    it.writeHisVal(testCommand.toDouble())
                    val cmControlMessage = getCMControlsMessage()
                    CcuLog.d(L.TAG_CCU_DEVICE, "CM Proto Control Message: $cmControlMessage")
                    CcuLog.i(Domain.LOG_TAG, "Send Test Command relayIndex $relayIndex $testCommand ${physicalPoint.readHisVal()}")
                    sendControlMoteMessage(
                        MessageType.CCU_TO_CM_OVER_USB_CM_SERIAL_CONTROLS,
                        cmControlMessage.toByteArray()
                    )
                }
            }
        }
    }

    fun sendCMAnalogTestCommand(analogIndex : Int, testVal : Double) {
        if (!isEquipPaired) {
            CcuLog.i(Domain.LOG_TAG, "System Equip does not exist")
            return
        }
        Globals.getInstance().isTestMode = true
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Globals.getInstance().isTestMode = true
                val physicalPoint = getPhysicalPointForAnalogIndex(analogIndex)
                physicalPoint?.let {
                    it.writeHisVal(testVal)
                    val cmControlMessage =
                        getCMControlsMessage()
                    CcuLog.d(L.TAG_CCU_DEVICE, "CM Proto Control Message: $cmControlMessage")
                    CcuLog.i(Domain.LOG_TAG, "Send Test Command analogIndex $analogIndex $testVal ${physicalPoint.readHisVal()}")
                    sendControlMoteMessage(
                        MessageType.CCU_TO_CM_OVER_USB_CM_SERIAL_CONTROLS,
                        cmControlMessage.toByteArray()
                    )
                }
            }
        }
    }

    fun sendConnectRelayTestCommand(relayIndex : Int, testCommand : Boolean) {
        if (!isConnectModulePaired) {
            CcuLog.i(Domain.LOG_TAG, "System Equip does not exist")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Globals.getInstance().isTestMode = true
                val physicalPoint = getConnectPhysicalPointForRelayIndex(relayIndex)
                physicalPoint?.let {
                    it.writeHisVal(testCommand.toDouble())
                    CcuLog.i(Domain.LOG_TAG, "Send Test Command relayIndex $relayIndex $testCommand ${physicalPoint.domainName} ${physicalPoint.readHisVal()}")
                    ConnectModbusSerialComm.sendControlsMessage(Domain.connect1Device)
                }
            }
        }
    }

    fun sendConnectAnalogTestCommand(analogIndex : Int, testVal : Double) {
        if (!isConnectModulePaired) {
            CcuLog.i(Domain.LOG_TAG, "System Equip does not exist")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Globals.getInstance().isTestMode = true
                val physicalPoint = getConnectPhysicalPointForAnalogIndex(analogIndex)
                physicalPoint?.let {
                    it.writeHisVal(testVal)
                    CcuLog.i(Domain.LOG_TAG, "Send Test Command analogIndex $analogIndex $testVal ${physicalPoint.readHisVal()}")
                    ConnectModbusSerialComm.sendControlsMessage(Domain.connect1Device)
                }
            }
        }
    }

    fun getPhysicalPointForRelayIndex(relayIndex : Int, isConnect: Boolean) : PhysicalPoint? {
        if (isEquipPaired) {
            val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
                Domain.systemEquip as VavAdvancedHybridSystemEquip
            } else {
                Domain.systemEquip as DabAdvancedHybridSystemEquip
            }
            val relayName = getRelayNameForIndex(relayIndex)
            if (isConnect) {
                getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device).values.find { it.domainName == relayName }
            } else {
                return getCMRelayLogicalPhysicalMap(systemEquip).values.find { it.domainName == relayName }
            }
        }
        return null
    }
    private fun getConnectPhysicalPointForRelayIndex(relayIndex : Int) : PhysicalPoint? {
        if (isEquipPaired) {
            val systemEquip = if (Domain.systemEquip is VavAdvancedHybridSystemEquip) {
                Domain.systemEquip as VavAdvancedHybridSystemEquip
            } else {
                Domain.systemEquip as DabAdvancedHybridSystemEquip
            }
            val relayName = getRelayNameForIndex(relayIndex)
            return getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device).values.find { it.domainName == relayName }
        }
        return null
    }


    private fun getRelayNameForIndex(relayIndex: Int) : String {
        return when(relayIndex) {
            0 -> DomainName.relay1
            1 -> DomainName.relay2
            2 -> DomainName.relay3
            3 -> DomainName.relay4
            4 -> DomainName.relay5
            5 -> DomainName.relay6
            6 -> DomainName.relay7
            7 -> DomainName.relay8
            else -> ""
        }
    }

    fun getPhysicalPointForAnalogIndex(analogIndex : Int) : PhysicalPoint? {
        if (isEquipPaired) {
            val systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
            val analogName = getAnalogNameForIndex(analogIndex)
            return getAnalogOutLogicalPhysicalMap(systemEquip).values.find { it.domainName == analogName }
        }
        return null
    }

    private fun getConnectPhysicalPointForAnalogIndex(analogIndex : Int) : PhysicalPoint? {
        if (isEquipPaired) {
            val systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
            val analogName = getAnalogNameForIndex(analogIndex)
            return getConnectAnalogOutLogicalPhysicalMap(systemEquip.connectEquip1,Domain.connect1Device).values.find { it.domainName == analogName }
        }
        return null
    }


    private fun getAnalogNameForIndex(analogIndex: Int) : String {
        return when(analogIndex) {
            0 -> DomainName.analog1Out
            1 -> DomainName.analog2Out
            2 -> DomainName.analog3Out
            3 -> DomainName.analog4Out
            else -> ""
        }
    }

    fun isEquipAvailable() {
        isEquipPaired =  CCUHsApi.getInstance().readEntity(
                "domainName == \"" + DomainName.vavAdvancedHybridAhuV2 + "\"").isNotEmpty()
        isConnectModulePaired =  CCUHsApi.getInstance().readEntity(
                "domainName == \"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\"").isNotEmpty()
    }
}

/**
 * Following enum class is used to define the control type for the VAV Advanced AHU
 * This enum list is picked from the model & Need to update when any changes in the model for this enum
 */
enum class ControlType {
    PRESSURE_BASED_FAN_CONTROL, SAT_BASED_COOLING_CONTROL, SAT_BASED_HEATING_CONTROL, LOAD_BASED_COOLING_CONTROL, LOAD_BASED_HEATING_CONTROL, LOAD_BASED_FAN_CONTROL, CO2_BASED_DAMPER_CONTROL, COMPOSITE
}

/**
 * Following enum class is used to define the control type for the VAV Advanced AHU Connect Module
 * This enum list is picked from the model & Need to update when any changes in the model for this enum
 */
enum class ConnectControlType {
    LOAD_BASED_COOLING_CONTROL, LOAD_BASED_HEATING_CONTROL, LOAD_BASED_FAN_CONTROL, COMPOSITE, CO2_BASED_DAMPER_CONTROL
}

data class Option(val index: Int, val value: String)