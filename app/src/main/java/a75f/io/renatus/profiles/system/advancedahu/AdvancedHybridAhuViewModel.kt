package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.cm.sendTestModeMessage
import a75f.io.device.connect.ConnectModbusSerialComm
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.toDouble
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.getAnalogOutLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getCMRelayLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getConnectAnalogOutLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getConnectRelayLogicalPhysicalMap
import a75f.io.logic.bo.building.system.util.AdvancedHybridAhuConfig
import a75f.io.logic.bo.building.system.util.deleteSystemConnectModule
import a75f.io.logic.bo.building.system.util.getAdvancedAhuSystemEquip
import a75f.io.logic.bo.building.system.util.getConnectDevice
import a75f.io.logic.bo.building.system.util.getConnectEquip
import a75f.io.logic.bo.building.system.util.getDabCmEquip
import a75f.io.logic.bo.building.system.util.getDabConnectEquip
import a75f.io.logic.bo.building.system.util.getDis
import a75f.io.logic.bo.building.system.util.getVavCmEquip
import a75f.io.logic.bo.building.system.util.getVavConnectEquip
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.R
import a75f.io.renatus.modbus.util.ALERT
import a75f.io.renatus.modbus.util.OK
import android.annotation.SuppressLint
import android.content.Context
import android.text.Spanned
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import io.seventyfivef.ph.core.Tags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 20-03-2024.
 */

open class AdvancedHybridAhuViewModel : ViewModel() {

    open var viewState = mutableStateOf(AdvancedHybridAhuState())

    private lateinit var cmDeviceBuilder: DeviceBuilder
    private lateinit var cmDeviceModel: SeventyFiveFDeviceDirective

    lateinit var context: Context

    lateinit var cmModel: SeventyFiveFProfileDirective
    lateinit var connectModel: SeventyFiveFProfileDirective
    lateinit var connectDeviceModel: SeventyFiveFDeviceDirective
    lateinit var profileConfiguration: AdvancedHybridAhuConfig
    lateinit var hayStack: CCUHsApi
    lateinit var cmEquipBuilder: ProfileEquipBuilder
    lateinit var connectEquipBuilder: ProfileEquipBuilder
    lateinit var connectDeviceBuilder: DeviceBuilder

    private var isEquipPaired = false
    private val celsiusEnabled = MutableStateFlow(UnitUtils.isCelsiusTunerAvailableStatus())

    var isConnectModulePaired = false
    var saveJob: Job? = null
    val isCelsiusChecked: StateFlow<Boolean> = celsiusEnabled

    fun toggleChecked() {
        celsiusEnabled.value = !celsiusEnabled.value
    }

    /**
     * This voltage values never going to be changed so hardcoded here
     */
    var minMaxVoltage = List(11) { Option(it, it.toString()) }

    @SuppressLint("DefaultLocale")
    var testVoltage = List(101) { Option(it, String.format("%.1f", it * 0.1)) }

    var modelLoadedState = MutableLiveData(false)
    val modelLoaded: LiveData<Boolean> get() = modelLoadedState

    /**
     * Initialize the ViewModel
     */
    fun init(context: Context, cmProfileModel: ModelDirective, connectProfileModel: ModelDirective, hayStack: CCUHsApi, profile: ProfileType) {

        CcuLog.i(Domain.LOG_TAG, "Advanced AHU Init")

        this.hayStack = hayStack
        cmEquipBuilder = ProfileEquipBuilder(hayStack)
        connectEquipBuilder = ProfileEquipBuilder(hayStack)
        isEquipAvailable(profile)
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


    fun addAdvanceAHUEquip(): String {
        deleteSystemConnectModule()
        val ahuEquipId = cmEquipBuilder.buildEquipAndPoints(
                configuration = profileConfiguration.cmConfiguration,
                modelDef = cmModel,
                siteRef = hayStack.site!!.id,
                equipDis = getDis(cmModel.name)
        )

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        cmDeviceBuilder.buildDeviceAndPoints(
                configuration = profileConfiguration.cmConfiguration,
                modelDef = cmDeviceModel,
                equipRef = ahuEquipId,
                siteRef = hayStack.site!!.id,
                deviceDis = getDis(cmDeviceModel.name)
        )

        DomainManager.addSystemDomainEquip(hayStack)
        DomainManager.addCmBoardDevice(hayStack)
        return ahuEquipId
    }

    fun addConnectModule() {

        if (profileConfiguration.connectConfiguration.connectEnabled) {
            val connectEquipId = connectEquipBuilder.buildEquipAndPoints(
                    configuration = profileConfiguration.connectConfiguration,
                    modelDef = connectModel,
                    siteRef = hayStack.site!!.id,
                    equipDis = getDis(connectModel.name)
            )

            val connectDevice = getConnectDevice()
            if (connectDevice.isNotEmpty()) {
                hayStack.deleteEntityTree(connectDevice["id"].toString())
            }
            connectDeviceBuilder.buildDeviceAndPoints(
                    configuration = profileConfiguration.connectConfiguration,
                    modelDef = connectDeviceModel,
                    equipRef = connectEquipId,
                    siteRef = hayStack.site!!.id,
                    deviceDis = getDis(connectDeviceModel.name)
            )
        }
    }


     fun updateConfiguration(existingConnectEquip: HashMap<Any,Any>, connectModelName: String) {

         cmEquipBuilder.updateEquipAndPoints(
                 configuration = profileConfiguration.cmConfiguration,
                 modelDef = cmModel,
                 siteRef = hayStack.site!!.id,
                 equipDis = getDis(cmModel.name),
                 isReconfiguration = true
         )
         cmDeviceBuilder.updateDeviceAndPoints(
                 configuration = profileConfiguration.cmConfiguration,
                 cmDeviceModel,
                 Domain.systemEquip.equipRef,
                 hayStack.site!!.id,
                 deviceDis = getDis(cmDeviceModel.name),
         )

         if (profileConfiguration.connectConfiguration.connectEnabled) {
             if (existingConnectEquip.isNotEmpty()) {
                 connectEquipBuilder.updateEquipAndPoints(
                         configuration = profileConfiguration.connectConfiguration,
                         modelDef = connectModel,
                         siteRef = hayStack.site!!.id,
                         equipDis = getDis(connectModel.name),
                         isReconfiguration = true
                 )
                 connectDeviceBuilder.updateDeviceAndPoints(
                         configuration = profileConfiguration.connectConfiguration,
                         modelDef = connectDeviceModel,
                         equipRef = existingConnectEquip[Tags.ID].toString(),
                         siteRef = hayStack.site!!.id,
                         deviceDis = getDis(connectDeviceModel.name)
                 )
             } else {

                 val connectEquipId = connectEquipBuilder.buildEquipAndPoints(
                         configuration = profileConfiguration.connectConfiguration,
                         modelDef = connectModel,
                         siteRef = hayStack.site!!.id,
                         equipDis = getDis(connectModel.name)
                 )

                 connectDeviceBuilder.buildDeviceAndPoints(
                         configuration = profileConfiguration.connectConfiguration,
                         modelDef = connectDeviceModel,
                         equipRef = connectEquipId,
                         siteRef = hayStack.site!!.id,
                         deviceDis = getDis(connectDeviceModel.name)
                 )
             }
         }
         else {
             deleteSystemConnectModule(connectModelName)
         }

         DomainManager.addSystemDomainEquip(hayStack)
         DomainManager.addCmBoardDevice(hayStack)

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

    private fun getPointByDomainName(modelDefinition: SeventyFiveFProfileDirective, domainName: String): SeventyFiveFProfilePointDef? {
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
        return ((this.viewState.value.analogOut1Enabled && this.viewState.value.analogOut1Association == type.ordinal) || (this.viewState.value.analogOut2Enabled && this.viewState.value.analogOut2Association == type.ordinal) || (this.viewState.value.analogOut3Enabled && this.viewState.value.analogOut3Association == type.ordinal) || (this.viewState.value.analogOut4Enabled && this.viewState.value.analogOut4Association == type.ordinal))
    }

    fun isConnectCoolingLoadEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.LOAD_BASED_COOLING_CONTROL)
    fun isConnectFanLoadEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.LOAD_BASED_FAN_CONTROL)
    fun isConnectHeatLoadEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.LOAD_BASED_HEATING_CONTROL)
    fun isConnectCompositeEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.COMPOSITE)
    fun isConnectDampersEnabled() = isAnyConnectAnalogMappedToControl(ConnectControlType.CO2_BASED_DAMPER_CONTROL)
    private fun isAnyConnectAnalogMappedToControl(type: ConnectControlType): Boolean {
        return ((this.viewState.value.connectAnalogOut1Enabled && this.viewState.value.connectAnalogOut1Association == type.ordinal) || (this.viewState.value.connectAnalogOut2Enabled && this.viewState.value.connectAnalogOut2Association == type.ordinal) || (this.viewState.value.connectAnalogOut3Enabled && this.viewState.value.connectAnalogOut3Association == type.ordinal) || (this.viewState.value.connectAnalogOut4Enabled && this.viewState.value.connectAnalogOut4Association == type.ordinal))
    }

    fun isAnalogEnabledAndMapped(type: ControlType, enabled: Boolean, association: Int) = (enabled && association == type.ordinal)

    fun isAnalogEnabledAndMapped(type: ConnectControlType, enabled: Boolean, association: Int) = (enabled && association == type.ordinal)

    open fun saveConfiguration() {
        // Implemented at sub class
    }

    fun getModelDefaultValue(relayState: ConfigState): Int {
        lateinit var domainName: String
        when (relayState) {
            viewState.value.relay1Config -> domainName = DomainName.relay1OutputAssociation
            viewState.value.relay2Config -> domainName = DomainName.relay2OutputAssociation
            viewState.value.relay3Config -> domainName = DomainName.relay3OutputAssociation
            viewState.value.relay4Config -> domainName = DomainName.relay4OutputAssociation
            viewState.value.relay5Config -> domainName = DomainName.relay5OutputAssociation
            viewState.value.relay6Config -> domainName = DomainName.relay6OutputAssociation
            viewState.value.relay7Config -> domainName = DomainName.relay7OutputAssociation
            viewState.value.relay8Config -> domainName = DomainName.relay8OutputAssociation
        }
        return profileConfiguration.cmConfiguration.getDefaultValConfig(domainName, profileConfiguration.cmModel).currentVal.toInt()
    }

    fun sendCMRelayTestCommand(relayIndex: Int, testCommand: Boolean) {
        if (!isEquipPaired) {
            CcuLog.i(Domain.LOG_TAG, "System Equip does not exist")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Globals.getInstance().isTestMode = true
                updateTestCacheConfig(relayIndex)
                val physicalPoint = getPhysicalPointForRelayIndex(relayIndex)
                physicalPoint?.let {
                    it.writeHisVal(testCommand.toDouble())
                    sendTestModeMessage()
                }
            }
        }
    }

    fun sendCMAnalogTestCommand(analogIndex: Int, testVal: Double) {
        if (!isEquipPaired) {
            CcuLog.i(Domain.LOG_TAG, "System Equip does not exist")
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Globals.getInstance().isTestMode = true
                updateTestCacheConfig(analogIndex + 8)
                val physicalPoint = getPhysicalPointForAnalogIndex(analogIndex)
                physicalPoint?.let {
                    it.writeHisVal(testVal)
                    sendTestModeMessage()
                }
            }
        }
    }

    fun updateTestCacheConfig(index: Int, resetCache: Boolean = false) {
        when (L.ccu().systemProfile) {
            is VavAdvancedAhu -> {
                if (resetCache) {
                    (L.ccu().systemProfile as VavAdvancedAhu).testConfigs.clear()
                    return
                }
                (L.ccu().systemProfile as VavAdvancedAhu).setTestConfigs(index)
            }

            is DabAdvancedAhu -> {
                if (resetCache) {
                    (L.ccu().systemProfile as DabAdvancedAhu).testConfigs.clear()
                    return
                }
                (L.ccu().systemProfile as DabAdvancedAhu).setTestConfigs(index)
            }
        }
    }

    fun sendConnectRelayTestCommand(relayIndex: Int, testCommand: Boolean) {
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

    fun sendConnectAnalogTestCommand(analogIndex: Int, testVal: Double) {
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

    fun getPhysicalPointForRelayIndex(relayIndex: Int): PhysicalPoint? {
        if (isEquipPaired) {
            val relayName = getRelayNameForIndex(relayIndex)
            return getCMRelayLogicalPhysicalMap(getAdvancedAhuSystemEquip()).values.find { it.domainName == relayName }
        }
        return null
    }

    fun getConnectPhysicalPointForRelayIndex(relayIndex: Int): PhysicalPoint? {
        if (isConnectModulePaired) {
            val relayName = getRelayNameForIndex(relayIndex)
            return getConnectRelayLogicalPhysicalMap(getConnectEquip(), Domain.connect1Device).values.find { it.domainName == relayName }
        }
        return null
    }


    private fun getRelayNameForIndex(relayIndex: Int): String {
        return when (relayIndex) {
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

    fun getPhysicalPointForAnalogIndex(analogIndex: Int): PhysicalPoint? {
        if (isEquipPaired) {
            val systemEquip = getAdvancedAhuSystemEquip()
            val analogName = getAnalogNameForIndex(analogIndex)
            return getAnalogOutLogicalPhysicalMap(systemEquip).values.find { it.domainName == analogName }
        }
        return null
    }

    private fun getConnectPhysicalPointForAnalogIndex(analogIndex: Int): PhysicalPoint? {
        if (isConnectModulePaired) {
            val connectEquip1 = getConnectEquip()
            val analogName = getAnalogNameForIndex(analogIndex)
            return getConnectAnalogOutLogicalPhysicalMap(connectEquip1, Domain.connect1Device).values.find { it.domainName == analogName }
        }
        return null
    }


    private fun getAnalogNameForIndex(analogIndex: Int): String {
        return when (analogIndex) {
            0 -> DomainName.analog1Out
            1 -> DomainName.analog2Out
            2 -> DomainName.analog3Out
            3 -> DomainName.analog4Out
            else -> ""
        }
    }

    fun isEquipAvailable(profile: ProfileType) {
        if (profile == ProfileType.SYSTEM_VAV_ADVANCED_AHU) {
            isEquipPaired = getVavCmEquip().isNotEmpty()
            isConnectModulePaired = getVavConnectEquip().isNotEmpty()
        } else {
            isEquipPaired = getDabCmEquip().isNotEmpty()
            isConnectModulePaired = getDabConnectEquip().isNotEmpty()
        }
    }

    fun showErrorDialog(context: Context, message: Spanned) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(ALERT)
        builder.setIcon(R.drawable.ic_warning)
        builder.setMessage(message)
        builder.setCancelable(false)
        builder.setPositiveButton(OK) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    open fun reset() {}


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

data class Option(val index: Int, val value: String, val dis: String? = null)