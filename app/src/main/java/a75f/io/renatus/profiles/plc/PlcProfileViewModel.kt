package a75f.io.renatus.profiles.plc

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
import a75f.io.device.mesh.LSerial
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.Domain.getMinMaxIncValuesByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelLoader.getHelioNodePidModel
import a75f.io.domain.util.ModelLoader.getSmartNodePidModel
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.plc.PlcProfile
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import a75f.io.logic.bo.building.sensors.SensorManager
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel.Companion.saveUnUsedPortStatus
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class PlcProfileViewModel : ViewModel() {

    lateinit var zoneRef: String
    lateinit var floorRef: String
    lateinit var profileType: ProfileType
    lateinit var nodeType: NodeType
    private var deviceAddress by Delegates.notNull<Short>()

    private lateinit var plcProfile: PlcProfile
    lateinit var profileConfiguration: PlcProfileConfig

    private lateinit var model : SeventyFiveFProfileDirective
    private lateinit var deviceModel : SeventyFiveFDeviceDirective
    lateinit var viewState: PlcProfileViewState

    private lateinit var context : Context
    lateinit var hayStack : CCUHsApi

    lateinit var analog1InputType: List<String>
    lateinit var pidTargetValue: List<String>
    lateinit var thermistor1InputType: List<String>
    lateinit var pidProportionalRange: List<String>
    lateinit var nativeSensorType: List<String>
    lateinit var analog2InputType: List<String>
    lateinit var setpointSensorOffset: List<String>

    lateinit var analog1MinOutput: List<String>
    lateinit var analog1MaxOutput: List<String>
    lateinit var relay1OnThreshold: List<String>
    lateinit var relay1OffThreshold: List<String>
    lateinit var relay2OnThreshold: List<String>
    lateinit var relay2OffThreshold: List<String>

    private lateinit var unusedPorts: HashMap<String, Boolean>
    private lateinit var pairingCompleteListener: OnPairingCompleteListener
    private var saveJob : Job? = null
    private var targetVal = ArrayList<String>()
    private var errorVal = ArrayList<String>()

    fun init(bundle: Bundle, context: Context, hayStack : CCUHsApi) {
        deviceAddress = bundle.getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
        zoneRef = bundle.getString(FragmentCommonBundleArgs.ARG_NAME)!!
        floorRef = bundle.getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
        profileType = ProfileType.values()[bundle.getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]
        nodeType = NodeType.values()[bundle.getInt(FragmentCommonBundleArgs.NODE_TYPE)]
        CcuLog.i(
            Domain.LOG_TAG, "PlcProfileViewModel Init profileType:$profileType " +
                "nodeType:$nodeType deviceAddress:$deviceAddress")
        model = getProfileDomainModel()
        CcuLog.i(Domain.LOG_TAG, "PlcProfileViewModel EquipModel Loaded")
        deviceModel = getDeviceDomainModel() as SeventyFiveFDeviceDirective
        CcuLog.i(Domain.LOG_TAG, "PlcProfileViewModel Device Model Loaded")

        if (L.getProfile(deviceAddress) != null && L.getProfile(deviceAddress) is PlcProfile) {
            plcProfile = L.getProfile(deviceAddress) as PlcProfile
            profileConfiguration = PlcProfileConfig(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getActiveConfiguration()
            viewState = PlcProfileViewState.fromPlcProfileConfig(profileConfiguration)
            unusedPorts = UnusedPortsModel.initializeUnUsedPorts(deviceAddress, hayStack)
        } else {
            profileConfiguration = PlcProfileConfig(deviceAddress.toInt(), nodeType.name, 0,
                zoneRef, floorRef , profileType, model ).getDefaultConfiguration()
            viewState = PlcProfileViewState.fromPlcProfileConfig(profileConfiguration)
        }
        CcuLog.i(Domain.LOG_TAG, profileConfiguration.toString())
        this.context = context
        this.hayStack = hayStack

        initializeLists()
        CcuLog.i(Domain.LOG_TAG, "Plc profile cofig Loaded")
    }

    /**
     * Generates and returns a list of target values for the specified sensor index.
     *
     * The function retrieves the corresponding sensor from the external sensor list
     * using the provided `selectedIndex`. It then computes a range of target values
     * based on the sensor's minimum value, maximum value, and increment value. These
     * values are scaled and added to the `targetVal` list, which is cleared before
     * being repopulated.
     *
     * @param selectedIndex The index of the sensor to retrieve target values for.
     *                      If `selectedIndex` is 0, the function returns the existing
     *                      `targetVal` list without modification.
     * @return An `ArrayList<String>` containing the computed target values for the specified sensor.
     */
    fun returnTargetValueAi1(selectedIndex: Int): ArrayList<String> {
        if (selectedIndex == 0) {
            return targetVal
        }
        var minMaxInc: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
        var pointString = getProcessVariableMappedPoint()
        minMaxInc = getMinMaxIncValuesByDomainName(pointString.toString(), model)

        targetVal.clear()

        val minVal = (100 * minMaxInc.first).toInt()
        val maxVal = (100 * minMaxInc.second).toInt()
        val increment = (100 * minMaxInc.third).toInt()

        for (pos in minVal..maxVal step increment) {
            targetVal.add((pos / 100.0).toString())
        }

        return targetVal
    }

    /**
     * Generates a list of target values for a selected native sensor.
     *
     * This function creates a list of target values based on the engineering constraints of a
     * specific native sensor selected by its index. The list is populated with values from the
     * sensor's minimum to maximum engineering values, incremented by a predefined step size.
     * If the selected index is 0, the function returns the existing `targetVal` list.
     *
     * @param selectedIndex The index of the native sensor in the sensor list.
     *                      (1-based index: subtracting 1 aligns it to the list index.)
     *                      If `selectedIndex` is 0, the existing `targetVal` is returned.
     * @return An `ArrayList<String>` containing the calculated target values for the selected sensor.
     */
    fun returnTargetValueNativeSensor(selectedIndex: Int): ArrayList<String> {
        if (selectedIndex == 0) {
            return targetVal
        }
        var minMaxInc: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
        var pointString = getProcessVariableMappedPoint()
        minMaxInc = getMinMaxIncValuesByDomainName(pointString.toString(), model)

        targetVal.clear()

        val minVal = (100 * minMaxInc.first).toInt()
        val maxVal = (100 * minMaxInc.second).toInt()
        val increment = (100 * minMaxInc.third).toInt()

        for (pos in minVal..maxVal step increment) {
            targetVal.add((pos / 100.0).toString())
        }

        return targetVal
    }

    /**
     * Generates a list of target values for a selected thermistor 1 sensor.
     *
     * This function creates a list of target values based on the engineering constraints of a
     * specific native sensor selected by its index. The list is populated with values from the
     * sensor's minimum to maximum engineering values, incremented by a predefined step size.
     * If the selected index is 0, the function returns the existing `targetVal` list.
     *
     * @param selectedIndex The index of the native sensor in the sensor list.
     *                      (1-based index: subtracting 1 aligns it to the list index.)
     *                      If `selectedIndex` is 0, the existing `targetVal` is returned.
     * @return An `ArrayList<String>` containing the calculated target values for the selected sensor.
     */
    fun returnTargetValueTH1(selectedIndex: Int): ArrayList<String> {
        if (selectedIndex == 0) {
            return targetVal
        }
        var minMaxInc: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
        var pointString = getProcessVariableMappedPoint()
        minMaxInc = getMinMaxIncValuesByDomainName(pointString.toString(), model)

        targetVal.clear()

        val minVal = (100 * minMaxInc.first).toInt()
        val maxVal = (100 * minMaxInc.second).toInt()
        val increment = (100 * minMaxInc.third).toInt()

        for (pos in minVal..maxVal step increment) {
            targetVal.add((pos / 100.0).toString())
        }

        return targetVal
    }

    /**
     * Returns a list of error values based on the selected index.
     *
     * This function calculates error values using the sensor's engineering values and increments,
     * and stores them in the `errorVal` list. If the selected index is 0, the function simply returns
     * the existing `errorVal` list. Otherwise, it clears the list, recalculates values based on the
     * specified sensor, and removes the first element to avoid division by zero in future calculations.
     *
     * @param selectedIndex The index of the selected sensor. If 0, the existing list is returned.
     * @return A list of error values as strings, excluding the first element (0.0).
     */
    fun returnErrorValueAi1(selectedIndex: Int): ArrayList<String> {
        if (selectedIndex == 0) {
            return errorVal
        }
        var minMaxInc: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
        var pointString = getProcessVariableMappedPoint()
        minMaxInc = getMinMaxIncValuesByDomainName(pointString.toString(), model)

        errorVal.clear()

        val minVal = (100 * minMaxInc.first).toInt()
        val maxVal = (100 * minMaxInc.second).toInt()
        val increment = (100 * minMaxInc.third).toInt()

        for (pos in minVal..maxVal step increment) {
            errorVal.add((pos / 100.0).toString())
        }

        // Remove the negative and zero values from the list
        errorVal.removeIf { it.toDoubleOrNull() == null || it.toDouble() <= 0 }

        return errorVal
    }

    fun returnErrorValueNativeSensor(selectedIndex: Int): ArrayList<String> {
        if (selectedIndex == 0) {
            return errorVal
        }
        var minMaxInc: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
        var pointString = getProcessVariableMappedPoint()
        minMaxInc = getMinMaxIncValuesByDomainName(pointString.toString(), model)

        errorVal.clear()

        val minVal = (100 * minMaxInc.first).toInt()
        val maxVal = (100 * minMaxInc.second).toInt()
        val increment = (100 * minMaxInc.third).toInt()

        for (pos in minVal..maxVal step increment) {
            errorVal.add((pos / 100.0).toString())
        }

        // Remove the negative and zero values from the list
        errorVal.removeIf { it.toDoubleOrNull() == null || it.toDouble() <= 0 }

        return errorVal
    }

    fun returnErrorValueTH1(selectedIndex: Int): ArrayList<String> {
        if (selectedIndex == 0) {
            return errorVal
        }
        var minMaxInc: Triple<Double, Double, Double> = Triple(0.0, 0.0, 0.0)
        var pointString = getProcessVariableMappedPoint()
        minMaxInc = getMinMaxIncValuesByDomainName(pointString.toString(), model)

        errorVal.clear()

        val minVal = (100 * minMaxInc.first).toInt()
        val maxVal = (100 * minMaxInc.second).toInt()
        val increment = (100 * minMaxInc.third).toInt()

        for (pos in minVal..maxVal step increment) {
            errorVal.add((pos / 100.0).toString())
        }

        // Remove the negative and zero values from the list
        errorVal.removeIf { it.toDoubleOrNull() == null || it.toDouble() <= 0 }

        return errorVal
    }

    private fun initializeLists() {
        analog1InputType = getListByDomainName(DomainName.analog1InputType, model)

        // Get the target list based on the selected sensor type
        if(viewState.analog1InputType.toInt() != 0) pidTargetValue = returnTargetValueAi1(viewState.analog1InputType.toInt())
        if(viewState.thermistor1InputType.toInt() != 0) pidTargetValue = returnTargetValueTH1(viewState.thermistor1InputType.toInt())
        if(viewState.nativeSensorType.toInt() != 0) pidTargetValue = returnErrorValueNativeSensor(viewState.nativeSensorType.toInt())

        thermistor1InputType = getListByDomainName(DomainName.thermistor1InputType, model)

        // Get the error list based on the selected sensor type
        if(viewState.analog1InputType.toInt() != 0) pidProportionalRange = returnErrorValueAi1(viewState.analog1InputType.toInt())
        if(viewState.thermistor1InputType.toInt() != 0) pidProportionalRange = returnErrorValueTH1(viewState.thermistor1InputType.toInt())
        if(viewState.nativeSensorType.toInt() != 0) pidProportionalRange = returnErrorValueNativeSensor(viewState.nativeSensorType.toInt())

        nativeSensorType = getListByDomainName(DomainName.nativeSensorType, model)

        analog2InputType = getListByDomainName(DomainName.analog2InputType, model)
        setpointSensorOffset = getListByDomainName(DomainName.setpointSensorOffset, model)
        analog1MinOutput = getListByDomainName(DomainName.analog1MinOutput, model)
        analog1MaxOutput = getListByDomainName(DomainName.analog1MaxOutput, model)
        relay1OnThreshold = getListByDomainName(DomainName.relay1OnThreshold, model)
        relay1OffThreshold = getListByDomainName(DomainName.relay1OffThreshold, model)
        relay2OnThreshold = getListByDomainName(DomainName.relay2OnThreshold, model)
        relay2OffThreshold = getListByDomainName(DomainName.relay2OffThreshold, model)
    }

    fun saveConfiguration() {
        if (saveJob == null) {
            ProgressDialogUtils.showProgressDialog(context, "Saving Plc Configuration")
            saveJob = viewModelScope.launch(highPriorityDispatcher) {
                CCUHsApi.getInstance().resetCcuReady()
                setUpPlcProfile()
                CcuLog.i(Domain.LOG_TAG, "PlcProfile Setup complete")
                withContext(Dispatchers.Main) {
                    context.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    showToast("Plc Configuration saved successfully", context)
                    CcuLog.i(Domain.LOG_TAG, "Close Pairing dialog")
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
                L.saveCCUState()
                hayStack.syncEntityTree()
                CCUHsApi.getInstance().setCcuReady()
                CcuLog.i(Domain.LOG_TAG, "Send seed for $deviceAddress")
                LSerial.getInstance()
                    .sendSeedMessage(false, false, deviceAddress, zoneRef, floorRef)
                DesiredTempDisplayMode.setModeType(zoneRef, CCUHsApi.getInstance())
                CcuLog.i(Domain.LOG_TAG, "PlcProfile Pairing complete")
                plcProfile.init()

                CoroutineScope(Dispatchers.IO).launch {
                    updateTypeForAnalog1Out(profileConfiguration)
                }
                // This check is needed because the dialog sometimes fails to close inside the coroutine.
                // We don't know why this happens.
                if (ProgressDialogUtils.isDialogShowing()) {
                    ProgressDialogUtils.hideProgressDialog()
                    pairingCompleteListener.onPairingComplete()
                }
            }
        }
    }

    private fun setUpPlcProfile() {
        viewState.updateConfigFromViewState(profileConfiguration)

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-PID-" + profileConfiguration.nodeAddress

        addBaseProfileConfigs()
        if (profileConfiguration.isDefault) {

            addEquipAndPoints(deviceAddress, profileConfiguration, nodeType, hayStack, model, deviceModel)
            L.ccu().zoneProfiles.add(plcProfile)

        } else {
            equipBuilder.updateEquipAndPoints(profileConfiguration, model, hayStack.site!!.id, equipDis, true)
            updateDeviceAndPoints(deviceAddress, profileConfiguration, nodeType, hayStack, model, deviceModel)
            CoroutineScope(Dispatchers.IO).launch {
                saveUnUsedPortStatus(profileConfiguration, deviceAddress, hayStack)
            }
        }
    }


    private fun addEquipAndPoints(
        deviceAddress: Short,
        config: ProfileConfiguration,
        nodeType: NodeType?,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?
    ) {
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipDis = hayStack.siteName + "-PID-" + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildEquipAndPoints ${model.domainName} profileType ${config.profileType}" )
        val equipId = equipBuilder.buildEquipAndPoints(
            config, equipModel, hayStack.site!!
                .id, equipDis
        )
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceName = when(nodeType) { NodeType.HELIO_NODE -> "-HN-" else -> "-SN-"}
        val deviceDis = hayStack.siteName + deviceName + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis,
            model
        )
        CcuLog.i(Domain.LOG_TAG, " add Profile")
        plcProfile = PlcProfile(deviceAddress, equipId)

    }

    private fun updateDeviceAndPoints(
        deviceAddress: Short,
        config: ProfileConfiguration,
        nodeType: NodeType?,
        hayStack: CCUHsApi,
        equipModel: SeventyFiveFProfileDirective?,
        deviceModel: SeventyFiveFDeviceDirective?
    ) {
        requireNotNull(equipModel)
        requireNotNull(deviceModel)
        val equipId = hayStack.readEntity("equip and group == \"$deviceAddress\"")["id"].toString()
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val deviceName = when(nodeType) { NodeType.HELIO_NODE -> "-HN-" else -> "-SN-"}
        val deviceDis = hayStack.siteName + deviceName + config.nodeAddress
        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.updateDeviceAndPoints(
            config,
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis,
            model
        )
    }

    private fun getProfileDomainModel() : SeventyFiveFProfileDirective{
        return if (nodeType == NodeType.SMART_NODE) {
             getSmartNodePidModel() as SeventyFiveFProfileDirective
        } else {
            getHelioNodePidModel() as SeventyFiveFProfileDirective
        }
    }

    fun setOnPairingCompleteListener(completeListener: OnPairingCompleteListener) {
        this.pairingCompleteListener = completeListener
    }

    private fun addBaseProfileConfigs() {
        val processVariablePoint = getProcessVariableMappedPoint()
        CcuLog.i(Domain.LOG_TAG, "processVariablePoint $processVariablePoint")
        processVariablePoint?.let {
            profileConfiguration.baseConfigs.add(EntityConfig(processVariablePoint))
        }

        val analog2InputTypePoint = getAnalog2InputTypeMappedPoint()
        CcuLog.i(Domain.LOG_TAG, "analog2InputTypePoint $analog2InputTypePoint")
        analog2InputTypePoint?.let {
            profileConfiguration.baseConfigs.add(EntityConfig(analog2InputTypePoint))
        }

        CcuLog.i(
            Domain.LOG_TAG, "processVariablePoint size" +
                    " ${profileConfiguration.getBaseProfileConfigs().size}"
        )
    }

    private fun getProcessVariableMappedPoint() : String? {
        if (viewState.analog1InputType > 0) {
            val analog1InputPoint = model.points.find { it.domainName == DomainName.analog1InputType }
            return (analog1InputPoint?.valueConstraint as MultiStateConstraint).allowedValues[viewState.analog1InputType.toInt()].value
        } else if (viewState.thermistor1InputType > 0) {
            val thermistor1InputPoint = model.points.find { it.domainName == DomainName.thermistor1InputType }
            return (thermistor1InputPoint?.valueConstraint as MultiStateConstraint).allowedValues[viewState.thermistor1InputType.toInt()].value
        } else if (viewState.nativeSensorType > 0) {
            val nativeSensorTypePoint = model.points.find { it.domainName == DomainName.nativeSensorType }
            return (nativeSensorTypePoint?.valueConstraint as MultiStateConstraint).allowedValues[viewState.nativeSensorType.toInt()].value
        } else {
            CcuLog.i(Domain.LOG_TAG, "Invalid UI Config : No process variable selected")
            return null
        }
    }

    private fun getAnalog2InputTypeMappedPoint() : String? {
        if (viewState.useAnalogIn2ForSetpoint) {
            val analog2InputTypePoint = model.points.find { it.domainName == DomainName.analog2InputType }
            return (analog2InputTypePoint?.valueConstraint as MultiStateConstraint).allowedValues[viewState.analog2InputType.toInt()].value
        } else {
            CcuLog.i(Domain.LOG_TAG, "Invalid UI Config : analog2 input not found")
            return null
        }
    }

    private fun getDeviceDomainModel() : ModelDirective {
        return if (nodeType == NodeType.SMART_NODE) {
            ModelLoader.getSmartNodeDevice()
        } else {
            ModelLoader.getHelioNodeDevice()
        }
    }

    private fun updateTypeForAnalog1Out(config : PlcProfileConfig) {
        val type = "${viewState.analog1MinOutput.toInt()}-${viewState.analog1MaxOutput.toInt()}v"

        val device = hayStack.readEntity("device and addr == \"${config.nodeAddress}\"")
        val analog1OutDict = hayStack.readHDict("point and deviceRef == \"${device["id"]}\" and domainName == \"analog1Out\"")
        val analog1OutPoint = RawPoint.Builder().setHDict(analog1OutDict).build()
        CcuLog.i(Domain.LOG_TAG, "analog1OutDict $analog1OutDict update type $type current type ${analog1OutPoint.type}")

        val controlVariable = hayStack.readEntity("point and domainName == \"controlVariable\" and group == \"${config.nodeAddress}\"")
        if (analog1OutPoint.type != type) {
            analog1OutPoint.type = type
            analog1OutPoint.pointRef = controlVariable["id"].toString()
            hayStack.updatePoint(analog1OutPoint, analog1OutPoint.id)
        } else {
            CcuLog.i(Domain.LOG_TAG, "Analog1Out type is already set to $type")
        }
    }
}