package a75f.io.logic.bo.building.hyperstat.profiles.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.heartbeat.HeartBeat
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.*
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.getSelectedFanLevel
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.HyperStatDevice
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint.Companion.addOTAStatusPoint
import a75f.io.logic.tuners.HyperstatCpuTuners
import a75f.io.logic.util.RxTask

/**
 * Models CPU Equipment in its interface, calls through to datastore (haystack)
 * to get its state.
 * We will have one of these for each HyperStat in the zone profile.
 *
 * @author tcase@75f.io
 * Created on 7/9/21.
 */
class HyperStatCpuEquip(val node: Short): HyperStatEquip() {

    val haystack: CCUHsApi = CCUHsApi.getInstance()
    private val profileTag = "hyperstatcpu"
    private val analogOutVoltageMin = 0
    private val analogOutVoltageMax = 10
    private val stageFan1DefaultVal = 10
    private val stageFan2And3DefaultVal = 10

    private var masterPoints: HashMap<Any, String> = HashMap()

    private var siteDis: String? = null
    private var tz: String? = null
    private var equipDis: String? = null
    private var gatewayRef: String? = null
    var equipRef: String? = null
    private var roomRef: String? = null
    private var floorRef: String? = null
    private var systemEquip = haystack.readEntity("equip and system and not modbus and not connectModule") as HashMap<Any, Any>

    companion object {
        fun getHyperStatEquipRef(nodeAddress: Short): HyperStatCpuEquip {
            val hyperStatCpuEquip = HyperStatCpuEquip(nodeAddress)
            hyperStatCpuEquip.initEquipReference(nodeAddress)
            return hyperStatCpuEquip
        }
    }

    override fun initializePoints(
        baseConfig: BaseProfileConfiguration,
        room: String,
        floor: String,
        node: Short
    ) {
        val config = baseConfig as HyperStatCpuConfiguration
        CcuLog.d(L.TAG_CCU_HSCPU, "New Profile  Initialising Points: ")
        nodeAddress = node.toInt()
        floorRef = floor
        roomRef = room
        siteDis = basicInfo.siteDis
        tz = basicInfo.timeZone
        equipDis = "$siteDis-$profileTag-$nodeAddress"
        gatewayRef = systemEquip["id"].toString()

        // get general profile
        val profileEquip =  HyperStatEquip()

        // Create Equip Point
        equipRef =  profileEquip.createNewEquip(
            HyperstatProfileNames.HSCPU,
            roomRef!!, floorRef!!,config.priority.toString(),nodeAddress.toString(),
            equipDis!!, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT
        )
        CcuLog.d(L.TAG_CCU_HSCPU, "New Profile  New Equip Created: $equipRef")

        // Init required reference
        init(HyperstatProfileNames.HSCPU, equipRef!!,floorRef!!,roomRef!!,nodeAddress,equipDis!!)

        // add Tuner Points
        createHyperStatTunerPoints(
            equipRef!!, equipDis!!, roomRef!!, floorRef!!
        )
        CcuLog.d(L.TAG_CCU_HSCPU, "New Profile  Tuners Created")

        // Create config points
        createProfileConfigurationPoints(hyperStatConfig = config)

        CcuLog.d(L.TAG_CCU_HSCPU, "New Profile Profile configuration points are created ")
        // Create Logical Points
       createProfileLogicalPoints(hyperStatConfig = config)

        CcuLog.d(L.TAG_CCU_HSCPU, "New Profile  logical points are created")

        configHyperStatDevice(config, profileEquip)

        addOTAStatusPoint("${Tags.HS}-$nodeAddress", equipRef!!, basicInfo.siteRef, roomRef!!, floorRef!!, nodeAddress, basicInfo.timeZone, haystack)

        updateConditioningMode()
        // Syncing the Points
        haystack.syncEntityTree()

        return
    }

    fun initEquipReference(node: Short) {
        val equip = haystack.read("equip and hyperstat and cpu and group == \"$node\"")
        if (equip.isEmpty()) {
            CcuLog.e(L.TAG_CCU_HSCPU, " Unable to find the equip details for node $node ")
            return
        }
        equipRef = equip["id"].toString()
        roomRef = equip["roomRef"].toString()
        floorRef = equip["floorRef"].toString()
        siteDis = basicInfo.siteDis
        tz = basicInfo.timeZone
        equipDis = "$siteDis-$profileTag-$node"
        nodeAddress = node.toInt()

        // Init required reference
        init(HyperstatProfileNames.HSCPU, equipRef!!,floorRef!!,roomRef!!,node.toInt(),equipDis!!)
    }

    private fun createHyperStatTunerPoints(
        equipRef: String, equipDis: String, roomRef: String, floorRef: String,
    ) {
        RxTask.executeAsync {
            HyperstatCpuTuners.addHyperstatModuleTuners(
                CCUHsApi.getInstance(), basicInfo.siteRef,
                equipRef, equipDis, basicInfo.timeZone, roomRef, floorRef
            )
        }
    }

    // Function which configure HyperStat Device configurations
    private fun configHyperStatDevice(config: HyperStatCpuConfiguration, profileEquip: HyperStatEquip) {

        val hyperStatDevice = HyperStatDevice(
            nodeAddress, basicInfo.siteRef, floorRef, roomRef, equipRef, HyperstatProfileNames.HSCPU
        )

        val heartBeatId = CCUHsApi.getInstance().addPoint(
            nodeAddress.let {
                HeartBeat.getHeartBeatPoint(
                    equipDis, equipRef,
                    basicInfo.siteRef, roomRef, floorRef, it, HyperstatProfileNames.HSCPU, tz
                )
            }
        )
        profileEquip.setupDeviceRelays(
            config.relay1State.enabled, config.relay2State.enabled, config.relay3State.enabled,
            config.relay4State.enabled, config.relay5State.enabled, config.relay6State.enabled,
            masterPoints, hyperStatDevice

        )
        profileEquip.setupDeviceAnalogOuts(
            config.analogOut1State.enabled, getCpuAnalogOutVoltageAtMin(config.analogOut1State),getCpuAnalogOutVoltageAtMax(config.analogOut1State),
            config.analogOut2State.enabled, getCpuAnalogOutVoltageAtMin(config.analogOut2State),getCpuAnalogOutVoltageAtMax(config.analogOut2State),
            config.analogOut3State.enabled, getCpuAnalogOutVoltageAtMin(config.analogOut3State),getCpuAnalogOutVoltageAtMax(config.analogOut3State),
            masterPoints, hyperStatDevice
        )

        profileEquip.setupDeviceAnalogIns(
            config.analogIn1State.enabled,HyperStatAssociationUtil.getSensorNameByType(config.analogIn1State.association),
            config.analogIn2State.enabled,HyperStatAssociationUtil.getSensorNameByType(config.analogIn2State.association),
            masterPoints, hyperStatDevice
        )

        // In Hyperstat CPU profile th1 will be always configured with Airflow Sensor so
        // it is 3 constant because in sensor manager class Airflow Sensor position in external sensor list is 3
        // do not change it
        profileEquip.setupDeviceThermistors(
            config.thermistorIn1State.enabled,HyperStatAssociationUtil.getSensorNameByType(config.thermistorIn1State.association),
            config.thermistorIn2State.enabled,HyperStatAssociationUtil.getSensorNameByType(config.thermistorIn2State.association),
            masterPoints, hyperStatDevice
        )

        hyperStatDevice.currentTemp.pointRef = masterPoints[LogicalKeyID.CURRENT_TEMP]
        hyperStatDevice.currentTemp.enabled = true

        hyperStatDevice.addSensor(Port.SENSOR_RH, masterPoints[Port.SENSOR_RH])
        hyperStatDevice.addSensor(Port.SENSOR_ILLUMINANCE, masterPoints[Port.SENSOR_ILLUMINANCE])
        hyperStatDevice.addSensor(Port.SENSOR_OCCUPANCY, masterPoints[Port.SENSOR_OCCUPANCY])

        hyperStatDevice.rssi.pointRef = heartBeatId
        hyperStatDevice.rssi.enabled = true
        hyperStatDevice.addPointsToDb()
    }

    private fun getCpuAnalogOutVoltageAtMin(analogOutState: AnalogOutState): Int {
        return if (analogOutState.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
            analogOutVoltageMin
        } else {
            analogOutState.voltageAtMin.toInt()
        }
    }

    private fun getCpuAnalogOutVoltageAtMax(analogOutState: AnalogOutState): Int {
        return if (analogOutState.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
            analogOutVoltageMax
        } else {
            analogOutState.voltageAtMax.toInt()
        }
    }


    //function which creates Profile configuration Points
    private fun createProfileConfigurationPoints(hyperStatConfig: HyperStatCpuConfiguration) {

        hyperStatPointsUtil.addProfilePoints()

        hyperStatPointsUtil.createTemperatureOffSetPoint(hyperStatConfig.temperatureOffset * 10)

        // List Of Configuration Points
        val configPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createAutoForceAutoAwayConfigPoints(
             hyperStatConfig.isEnableAutoForceOccupied, hyperStatConfig.isEnableAutoAway
        )

        val co2ConfigPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createPointCO2ConfigPoint(
                 hyperStatConfig.zoneCO2DamperOpeningRate,hyperStatConfig.zoneCO2Threshold,hyperStatConfig.zoneCO2Target
        )

        val vocPmPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createPointVOCPmConfigPoint( equipDis!!,
                hyperStatConfig.zoneVOCThreshold,hyperStatConfig.zoneVOCTarget,
                hyperStatConfig.zonePm2p5Threshold,hyperStatConfig.zonePm2p5Target
            )

        val loopOutputPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createLoopOutputPoints(false)

        val relayConfigPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createIsRelayEnabledConfigPoints(
            ConfigState(hyperStatConfig.relay1State.enabled, hyperStatConfig.relay1State.association.ordinal),
            ConfigState(hyperStatConfig.relay2State.enabled, hyperStatConfig.relay2State.association.ordinal),
            ConfigState(hyperStatConfig.relay3State.enabled, hyperStatConfig.relay3State.association.ordinal),
            ConfigState(hyperStatConfig.relay4State.enabled, hyperStatConfig.relay4State.association.ordinal),
            ConfigState(hyperStatConfig.relay5State.enabled, hyperStatConfig.relay5State.association.ordinal),
            ConfigState(hyperStatConfig.relay6State.enabled, hyperStatConfig.relay6State.association.ordinal),
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT
        )

        val analogOutConfigPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createIsAnalogOutEnabledConfigPoints(
                ConfigState(hyperStatConfig.analogOut1State.enabled, hyperStatConfig.analogOut1State.association.ordinal),
                ConfigState(hyperStatConfig.analogOut2State.enabled, hyperStatConfig.analogOut2State.association.ordinal),
                ConfigState(hyperStatConfig.analogOut3State.enabled, hyperStatConfig.analogOut3State.association.ordinal),
                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT
            )

        val analogInConfigPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createIsAnalogInEnabledConfigPoints(
                ConfigState(hyperStatConfig.analogIn1State.enabled, hyperStatConfig.analogIn1State.association.ordinal),
                ConfigState(hyperStatConfig.analogIn2State.enabled, hyperStatConfig.analogIn2State.association.ordinal),
            )

        val thConfigPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createIsThermistorEnabledConfigPoints(
                ConfigState(hyperStatConfig.thermistorIn1State.enabled, hyperStatConfig.thermistorIn1State.association.ordinal), // aiflow sensor is constant
                ConfigState(hyperStatConfig.thermistorIn2State.enabled, hyperStatConfig.thermistorIn2State.association.ordinal) // this is cpu config not 2 pipe configuration
            )

        // For user intent Point CCU level default values need to be added
        val userIntentPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createUserIntentPoints(
            hyperStatPointsUtil.getCPUDefaultFanSpeed(hyperStatConfig),
            hyperStatPointsUtil.getCPUDefaultConditioningMode(hyperStatConfig)
        )

        // device display configuration
        val deviceDisplayConfigPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createDeviceDisplayConfigurationPoints(
                hyperStatConfig.displayHumidity,hyperStatConfig.displayVOC,
                hyperStatConfig.displayPp2p5,hyperStatConfig.displayCo2
        )

        var allConfigPoints = arrayOf(
            configPointsList, relayConfigPoints, analogOutConfigPoints,
            analogInConfigPoints, thConfigPointsList, userIntentPointsList,
            co2ConfigPointsList, loopOutputPoints,vocPmPointsList,deviceDisplayConfigPoints
        )

        if (isStagedFanSelected(hyperStatConfig)) {
            val stagedFanConfigPoints : MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createStagedFanConfigPoint(hyperStatConfig)
            if (stagedFanConfigPoints.isNotEmpty()) {
                allConfigPoints += stagedFanConfigPoints
            }
        }
        hyperStatPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = allConfigPoints)
        CcuLog.d(L.TAG_CCU_HSCPU, "adding : points default value ")

    }

    private fun isStagedFanSelected(hyperStatConfig: HyperStatCpuConfiguration): Boolean {
        return  (hyperStatConfig.analogOut1State.enabled && (hyperStatConfig.analogOut1State.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED)) ||
                (hyperStatConfig.analogOut2State.enabled && (hyperStatConfig.analogOut2State.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED)) ||
                (hyperStatConfig.analogOut3State.enabled && (hyperStatConfig.analogOut3State.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED))
    }

    //Function to create Logical Points
    private fun createProfileLogicalPoints(hyperStatConfig: HyperStatCpuConfiguration) {
        CcuLog.d(L.TAG_CCU_HSCPU, "createProfileLogicalPoints: ")

        val temperaturePointsList: MutableList<Triple<Point, Any, Any>> = hyperStatPointsUtil
            .temperatureScheduleLogicalPoints()

        val relayPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatPointsUtil
            .createConfigRelayLogicalPoints(
                hyperStatConfig = hyperStatConfig
            )
        val analogOutPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatPointsUtil
            .createConfigAnalogOutLogicalPoints(
                hyperStatConfig = hyperStatConfig
            )
        val analogInPointsList: MutableList<Triple<Point, Any, Any>> =
            hyperStatPointsUtil.createConfigAnalogInLogicalPoints(
                hyperStatConfig.analogIn1State,hyperStatConfig.analogIn2State,
            )
        val thermistorInPointsList: MutableList<Triple<Point, Any, Any>> =
            hyperStatPointsUtil.createConfigThermistorInLogicalPoints(
                 hyperStatConfig.thermistorIn1State, hyperStatConfig.thermistorIn2State
            )
        // Device Class will create all the sensor points dynamically based on the input message
        // which is received by the Hyper state device
        val sensorPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatPointsUtil.createLogicalSensorPoints()

        masterPoints.putAll(hyperStatPointsUtil.addPointsToHaystack(listOfAllPoints = sensorPointsList))
        masterPoints.putAll(hyperStatPointsUtil.addPointsToHaystack(listOfAllPoints = temperaturePointsList))
        masterPoints.putAll(hyperStatPointsUtil.addPointsToHaystack(listOfAllPoints = relayPointsList))
        masterPoints.putAll(hyperStatPointsUtil.addPointsToHaystack(listOfAllPoints = analogOutPointsList))
        masterPoints.putAll(hyperStatPointsUtil.addPointsToHaystack(listOfAllPoints = analogInPointsList))
        masterPoints.putAll(hyperStatPointsUtil.addPointsToHaystack(listOfAllPoints = thermistorInPointsList))

        CcuLog.d(L.TAG_CCU_HSCPU, "Done with Logical points creation")

    }


    // Function to update the existing profile
    override fun updateConfiguration(configuration: BaseProfileConfiguration) {
        CcuLog.d(L.TAG_CCU_HSCPU, "===========HyperStat profile Update  ============")

        val updatedHyperStatConfig =  configuration as HyperStatCpuConfiguration
        val presetConfiguration = getConfiguration()

        updateGeneralConfiguration(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateRelaysConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogOutConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogInConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateThermistorInConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)

        LogicalPointsUtil.cleanCpuLogicalPoints(updatedHyperStatConfig,equipRef!!)
        CcuLog.d(L.TAG_CCU_HSCPU, "Profile update has been completed  ")
        haystack.syncEntityTree()

    }

    // function to update (If change in configuration)
    // Temp Off set , CO2 threshold and target, VOC threshold and target, pm2.5 threshold and target
    private fun updateGeneralConfiguration(
        newConfiguration: HyperStatCpuConfiguration,
        existingConfiguration: HyperStatCpuConfiguration){

        updateTempOffset(existingConfiguration.temperatureOffset ,newConfiguration.temperatureOffset)

        updateAutoAwayAutoForceOccupy(
            existingConfiguration.isEnableAutoAway, newConfiguration.isEnableAutoAway,
            existingConfiguration.isEnableAutoForceOccupied, newConfiguration.isEnableAutoForceOccupied,
        )

        updateCO2Values(
            existingConfiguration.zoneCO2DamperOpeningRate,newConfiguration.zoneCO2DamperOpeningRate,
            existingConfiguration.zoneCO2Threshold,newConfiguration.zoneCO2Threshold,
            existingConfiguration.zoneCO2Target,newConfiguration.zoneCO2Target
        )

        updateVOCValues(
            existingConfiguration.zoneVOCThreshold, newConfiguration.zoneVOCThreshold,
            existingConfiguration.zoneVOCTarget, newConfiguration.zoneVOCTarget
        )

        updatePm25Values(
            existingConfiguration.zonePm2p5Threshold, newConfiguration.zonePm2p5Threshold,
            existingConfiguration.zonePm2p5Target, newConfiguration.zonePm2p5Target
        )
        updateDeviceDisplayConfiguration(
            existingConfiguration.displayHumidity,newConfiguration.displayHumidity,
            existingConfiguration.displayVOC,newConfiguration.displayVOC,
            existingConfiguration.displayPp2p5,newConfiguration.displayPp2p5,
            existingConfiguration.displayCo2,newConfiguration.displayCo2
            )
        updateStagedFanConfiguration(
            existingConfiguration, newConfiguration,
            existingConfiguration.coolingStage1FanState, newConfiguration.coolingStage1FanState,
            existingConfiguration.coolingStage2FanState, newConfiguration.coolingStage2FanState,
            existingConfiguration.coolingStage3FanState, newConfiguration.coolingStage3FanState,
            existingConfiguration.heatingStage1FanState, newConfiguration.heatingStage1FanState,
            existingConfiguration.heatingStage2FanState, newConfiguration.heatingStage2FanState,
            existingConfiguration.heatingStage3FanState, newConfiguration.heatingStage3FanState,
        )

        updateStagedFanConfigPoints(
            existingConfiguration, newConfiguration,
        )
        updateAnalogRecirculateConfigPoints(
            existingConfiguration, newConfiguration
        )
    }

    /**
     * Checks if the current stage has a new staged fan mapping based on the comparison of the new and existing analog output states.
     *
     * @param newAnalogOutState The new AnalogOutState object representing the state of the analog output.
     * @param existingAnalogOutState The existing AnalogOutState object representing the state of the analog output.
     * @return True if the current stage has a new staged fan mapping, false otherwise.
     */
    private fun checkCurrentStageHasNewStagedFanMapping(
            newAnalogOutState: AnalogOutState,
            existingAnalogOutState: AnalogOutState
    ): Boolean {
        return ((newAnalogOutState.enabled && !existingAnalogOutState.enabled) ||
                (newAnalogOutState.enabled &&
                        (newAnalogOutState.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED) &&
                        (existingAnalogOutState.association != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED))
                )
    }

    private fun checkForNewRecirculateValue(
            newAnalogOutState: AnalogOutState,
            existingAnalogOutState: AnalogOutState
    ): Boolean {
        return (!HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs
        (newAnalogOutState, existingAnalogOutState) &&
                (HyperStatAssociationUtil.findChangeInAnalogOutConfig(
                        newAnalogOutState, existingAnalogOutState) == AnalogOutChanges.RECIRCULATE))
    }

    private fun updateAnalogRecirculatePoint(analogTag: String, analogVal: Double) {
        val analogAtRecirculate = hsHaystackUtil.readPointID("$analogTag and recirculate")
        if (analogAtRecirculate != null)
            updatePointValueChangeRequired(analogAtRecirculate, analogVal)
    }

    private fun createNewRecirculatePoint(analogTag: String, analogVal: Double) {
        val analogAtRecirculatePoints : MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createAnalogAtRecirculatePoint(analogVal, analogTag)
        hyperStatPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = arrayOf(
                analogAtRecirculatePoints
        ))
        updateAnalogRecirculatePoint(analogTag, analogVal)
    }

    private fun deleteRecirculatePointIfNotRequired(analogTag: String) {
        val pointId = hsHaystackUtil.readPointID("$analogTag and recirculate")
        if (!pointId.isNullOrEmpty()) {
            hsHaystackUtil.removePoint(pointId)
        }
    }

    /**
     * Updates the configuration points related to analog outputs for the recirculate stage based on changes
     * between the existing configuration and the new configuration.
     *
     * @param existingConfiguration The existing HyperStatCpuConfiguration.
     * @param newConfiguration The new HyperStatCpuConfiguration.
     */
    private fun updateAnalogRecirculateConfigPoints(
        existingConfiguration: HyperStatCpuConfiguration,
        newConfiguration: HyperStatCpuConfiguration
    ) {
        // Step 1: Create points for newly staged mapped fan
        if (checkCurrentStageHasNewStagedFanMapping(newConfiguration.analogOut1State, existingConfiguration.analogOut1State)) {
            createNewRecirculatePoint("analog1", newConfiguration.analogOut1State.voltageAtRecirculate)
        }

        if(checkCurrentStageHasNewStagedFanMapping(newConfiguration.analogOut2State, existingConfiguration.analogOut2State)) {
            createNewRecirculatePoint("analog2", newConfiguration.analogOut2State.voltageAtRecirculate)
        }

        if(checkCurrentStageHasNewStagedFanMapping(newConfiguration.analogOut3State, existingConfiguration.analogOut3State)) {
            createNewRecirculatePoint("analog3", newConfiguration.analogOut3State.voltageAtRecirculate)
        }

        // Step 2: Delete stages if they are no longer in use
        if(!HyperStatAssociationUtil.isAnalogAssociatedToStaged(newConfiguration.analogOut1State)) deleteRecirculatePointIfNotRequired("analog1")
        if(!HyperStatAssociationUtil.isAnalogAssociatedToStaged(newConfiguration.analogOut2State)) deleteRecirculatePointIfNotRequired("analog2")
        if(!HyperStatAssociationUtil.isAnalogAssociatedToStaged(newConfiguration.analogOut3State)) deleteRecirculatePointIfNotRequired("analog3")

        // Step 3: Update the existing staged fan recirculate values
        if(checkForNewRecirculateValue(newConfiguration.analogOut1State, existingConfiguration.analogOut1State))
            updateAnalogRecirculatePoint("analog1", newConfiguration.analogOut1State.voltageAtRecirculate)
        if(checkForNewRecirculateValue(newConfiguration.analogOut2State, existingConfiguration.analogOut2State))
            updateAnalogRecirculatePoint("analog2", newConfiguration.analogOut2State.voltageAtRecirculate)
        if(checkForNewRecirculateValue(newConfiguration.analogOut3State, existingConfiguration.analogOut3State))
            updateAnalogRecirculatePoint("analog3", newConfiguration.analogOut3State.voltageAtRecirculate)
    }

    private fun updateStagedFanConfigPoints(
        existingConfiguration: HyperStatCpuConfiguration,
        newConfiguration: HyperStatCpuConfiguration
    ) {
        fun createStagedFanConfigPointIfEnabled(fanStageQuery: String, stage: CpuRelayAssociation) {
            if (HyperStatAssociationUtil.isAnyAnalogOutMappedToStagedFan(newConfiguration) && !HyperStatAssociationUtil.isAnyAnalogOutMappedToStagedFan(existingConfiguration)) {
                if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration,stage)) {
                    val stagedFanConfigPoints : MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createStagedFanPoint(newConfiguration, stage)
                    hyperStatPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = arrayOf(
                        stagedFanConfigPoints
                    ))
                }
            } else if (HyperStatAssociationUtil.isAnyAnalogOutMappedToStagedFan(newConfiguration) && HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, stage)) {
                if (!HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, stage)) {
                    val stagedFanConfigPoints : MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createStagedFanPoint(newConfiguration, stage)
                    hyperStatPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = arrayOf(
                        stagedFanConfigPoints
                    ))
                }
            } else {
                if (HyperStatAssociationUtil.isAnyAnalogOutMappedToStagedFan(existingConfiguration) && HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, stage)) {
                    val pointId = hsHaystackUtil.readPointID(fanStageQuery)
                    if (!pointId.isNullOrEmpty()) {
                        hsHaystackUtil.removePoint(pointId)
                    }
                }
            }
        }
        createStagedFanConfigPointIfEnabled(
            "stage1 and cooling and fan",
            CpuRelayAssociation.COOLING_STAGE_1)
        createStagedFanConfigPointIfEnabled(
            "stage2 and cooling and fan",
            CpuRelayAssociation.COOLING_STAGE_2
        )
        createStagedFanConfigPointIfEnabled(
            "stage3 and cooling and fan",
            CpuRelayAssociation.COOLING_STAGE_3
        )
        createStagedFanConfigPointIfEnabled(
            "stage1 and heating and fan",
            CpuRelayAssociation.HEATING_STAGE_1
        )
        createStagedFanConfigPointIfEnabled(
            "stage2 and heating and fan",
            CpuRelayAssociation.HEATING_STAGE_2
        )
        createStagedFanConfigPointIfEnabled(
            "stage3 and heating and fan",
            CpuRelayAssociation.HEATING_STAGE_3
        )
    }


    private fun updateStagedFanConfiguration(
        existingConfiguration: HyperStatCpuConfiguration, newConfiguration: HyperStatCpuConfiguration,
        coolingStage1FanOld: Int, coolingStage1FanNew: Int,
        coolingStage2FanOld: Int, coolingStage2FanNew: Int,
        coolingStage3FanOld: Int, coolingStage3FanNew: Int,
        heatingStage1FanOld: Int, heatingStage1FanNew: Int,
        heatingStage2FanOld: Int, heatingStage2FanNew: Int,
        heatingStage3FanOld: Int, heatingStage3FanNew: Int
    ) {
        if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.COOLING_STAGE_1) &&
            HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuRelayAssociation.COOLING_STAGE_1) &&
            coolingStage1FanOld != coolingStage1FanNew
        ) {
            val pointId = hsHaystackUtil.readPointID("stage1 and cooling and fan")
            val defaultValue = if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.COOLING_STAGE_1))
                coolingStage1FanNew
            else stageFan1DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.COOLING_STAGE_2) &&
            HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuRelayAssociation.COOLING_STAGE_2) &&
            coolingStage2FanOld != coolingStage2FanNew
        ) {
            val pointId = hsHaystackUtil.readPointID("stage2 and cooling and fan")
            val defaultValue = if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.COOLING_STAGE_2))
                coolingStage2FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.COOLING_STAGE_3) &&
            HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuRelayAssociation.COOLING_STAGE_3) &&
            coolingStage3FanOld != coolingStage3FanNew
        ) {
            val pointId = hsHaystackUtil.readPointID("stage3 and cooling and fan")
            val defaultValue = if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.COOLING_STAGE_3))
                coolingStage3FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.HEATING_STAGE_1) &&
            HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuRelayAssociation.HEATING_STAGE_1) &&
            heatingStage1FanOld != heatingStage1FanNew
        ) {
            val pointId = hsHaystackUtil.readPointID("stage1 and heating and fan")
            val defaultValue = if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.HEATING_STAGE_1))
                heatingStage1FanNew
            else stageFan1DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.HEATING_STAGE_2) &&
            HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuRelayAssociation.HEATING_STAGE_2) &&
            heatingStage2FanOld != heatingStage2FanNew
        ) {
            val pointId = hsHaystackUtil.readPointID("stage2 and heating and fan")
            val defaultValue = if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.HEATING_STAGE_2))
                heatingStage2FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.HEATING_STAGE_3) &&
            HyperStatAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuRelayAssociation.HEATING_STAGE_3) &&
            heatingStage3FanOld != heatingStage3FanNew
        ) {
            val pointId = hsHaystackUtil.readPointID("stage3 and heating and fan")
            val defaultValue = if (HyperStatAssociationUtil.isStagedFanEnabled(newConfiguration, CpuRelayAssociation.HEATING_STAGE_3))
                heatingStage3FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

    }


    // collect the existing profile configurations
    fun getConfiguration(): HyperStatCpuConfiguration {
        val config = HyperStatCpuConfiguration()

        getRelayConfigurations(config)
        getAnalogOutConfigurations(config)
        getAnalogInConfigurations(config)
        getThermistorInConfigurations(config)

        config.temperatureOffset = hsHaystackUtil.getTempOffValue()
        config.isEnableAutoForceOccupied = hsHaystackUtil.isAutoForceOccupyEnabled()
        config.isEnableAutoAway =  hsHaystackUtil.isAutoAwayEnabled()
        config.zoneCO2DamperOpeningRate = hsHaystackUtil.getCo2DamperOpeningConfigValue()
        config.zoneCO2Threshold = hsHaystackUtil.getCo2DamperThresholdConfigValue()
        config.zoneCO2Target = hsHaystackUtil.getCo2TargetConfigValue()
        config.zoneVOCThreshold = hsHaystackUtil.getVocThresholdConfigValue()
        config.zoneVOCTarget = hsHaystackUtil.getVocTargetConfigValue()
        config.zonePm2p5Threshold = hsHaystackUtil.getPm2p5ThresholdConfigValue()
        config.zonePm2p5Target = hsHaystackUtil.getPm2p5TargetConfigValue()

        config.coolingStage1FanState = hsHaystackUtil.getFanStageValue("cooling and stage1",7).toInt()
        config.coolingStage2FanState = hsHaystackUtil.getFanStageValue("cooling and stage2",10).toInt()
        config.coolingStage3FanState = hsHaystackUtil.getFanStageValue("cooling and stage3",10).toInt()
        config.heatingStage1FanState = hsHaystackUtil.getFanStageValue("heating and stage1",7).toInt()
        config.heatingStage2FanState = hsHaystackUtil.getFanStageValue("heating and stage2",10).toInt()
        config.heatingStage3FanState = hsHaystackUtil.getFanStageValue("heating and stage3",10).toInt()

        config.displayHumidity = hsHaystackUtil.getDisplayHumidity() == 1.0
        config.displayCo2 = hsHaystackUtil.getDisplayCo2() == 1.0
        config.displayVOC = hsHaystackUtil.getDisplayVoc() == 1.0
        config.displayPp2p5 = hsHaystackUtil.getDisplayP2p5() == 1.0

        return config
    }

    //  To show the existing profile configurations
    // config relays
    fun getRelayConfigurations(config: HyperStatCpuConfiguration):HyperStatCpuConfiguration {
        val r1 =  hsHaystackUtil.readConfigStatus("relay1").toInt()
        val r2 =  hsHaystackUtil.readConfigStatus("relay2").toInt()
        val r3 =  hsHaystackUtil.readConfigStatus("relay3").toInt()
        val r4 =  hsHaystackUtil.readConfigStatus("relay4").toInt()
        val r5 =  hsHaystackUtil.readConfigStatus("relay5").toInt()
        val r6 =  hsHaystackUtil.readConfigStatus("relay6").toInt()

        val r1AssociatedTo = hsHaystackUtil.readConfigAssociation("relay1")
        val r2AssociatedTo = hsHaystackUtil.readConfigAssociation("relay2")
        val r3AssociatedTo = hsHaystackUtil.readConfigAssociation("relay3")
        val r4AssociatedTo = hsHaystackUtil.readConfigAssociation("relay4")
        val r5AssociatedTo = hsHaystackUtil.readConfigAssociation("relay5")
        val r6AssociatedTo = hsHaystackUtil.readConfigAssociation("relay6")

        config.relay1State =
            RelayState(r1 == 1, HyperStatAssociationUtil.getRelayAssociatedStage(r1AssociatedTo.toInt()))
        config.relay2State =
            RelayState(r2 == 1, HyperStatAssociationUtil.getRelayAssociatedStage(r2AssociatedTo.toInt()))
        config.relay3State =
            RelayState(r3 == 1, HyperStatAssociationUtil.getRelayAssociatedStage(r3AssociatedTo.toInt()))
        config.relay4State =
            RelayState(r4 == 1, HyperStatAssociationUtil.getRelayAssociatedStage(r4AssociatedTo.toInt()))
        config.relay5State =
            RelayState(r5 == 1, HyperStatAssociationUtil.getRelayAssociatedStage(r5AssociatedTo.toInt()))
        config.relay6State =
            RelayState(r6 == 1, HyperStatAssociationUtil.getRelayAssociatedStage(r6AssociatedTo.toInt()))

        return config
    }

    //config Analog Out
     fun getAnalogOutConfigurations(config: HyperStatCpuConfiguration):HyperStatCpuConfiguration {
        val ao1 = hsHaystackUtil.readConfigStatus("analog1 and output  ").toInt()
        val ao2 = hsHaystackUtil.readConfigStatus("analog2 and output ").toInt()
        val ao3 = hsHaystackUtil.readConfigStatus("analog3 and output ").toInt()

        val ao1AssociatedTo = hsHaystackUtil.readConfigAssociation("analog1 and output ")
        val ao2AssociatedTo = hsHaystackUtil.readConfigAssociation("analog2 and output ")
        val ao3AssociatedTo = hsHaystackUtil.readConfigAssociation("analog3 and output ")


        var ao1MinVal = 2.0
        var ao1MaxVal = 10.0

        var ao1fanLow = 70.0
        var ao1fanMedium = 80.0
        var ao1fanHigh = 100.0

        var ao1AtRecirculate = 4.0

        if (ao1 == 1) {
            if (ao1AssociatedTo.toInt() != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                ao1MinVal = hsHaystackUtil.readConfigPointValue(
                    "analog1 and output and min"
                )
                ao1MaxVal = hsHaystackUtil.readConfigPointValue(
                    "analog1 and output and max"
                )
            }

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao1AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.MODULATING_FAN_SPEED ||
                HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao1AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED
            ) {
                ao1fanLow = hsHaystackUtil.readConfigPointValue(
                    "analog1 and output and low"
                )
                ao1fanMedium = hsHaystackUtil.readConfigPointValue(
                    "analog1 and output and medium"
                )
                ao1fanHigh = hsHaystackUtil.readConfigPointValue(
                    "analog1 and output and high"
                )
            }
            ao1AtRecirculate = hsHaystackUtil.readConfigPointValue(
                "analog1 and recirculate"
            )
        }
        // Default Values
        var ao2MinVal = 2.0
        var ao2MaxVal = 10.0

        var ao2fanLow = 70.0
        var ao2fanMedium = 80.0
        var ao2fanHigh = 100.0

        var ao2AtRecirculate = 4.0

        if (ao2 == 1) {
            if (ao2AssociatedTo.toInt() != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                ao2MinVal = hsHaystackUtil.readConfigPointValue(
                    "analog2 and output and min"
                )
                ao2MaxVal = hsHaystackUtil.readConfigPointValue(
                    "analog2 and output and max"
                )
            }

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao2AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.MODULATING_FAN_SPEED ||
                HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao2AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED
            ) {
                ao2fanLow = hsHaystackUtil.readConfigPointValue(
                    "analog2 and output and low"
                )
                ao2fanMedium = hsHaystackUtil.readConfigPointValue(
                    "analog2 and output and medium"
                )
                ao2fanHigh = hsHaystackUtil.readConfigPointValue(
                    "analog2 and output and high"
                )
            }
            ao2AtRecirculate = hsHaystackUtil.readConfigPointValue(
                "analog2 and recirculate"
            )
        }

        var ao3MinVal = 2.0
        var ao3MaxVal = 10.0

        var ao3fanLow = 70.0
        var ao3fanMedium = 80.0
        var ao3fanHigh = 100.0

        var ao3AtRecirculate = 4.0

        if (ao3 == 1) {
            if (ao3AssociatedTo.toInt() != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                ao3MinVal = hsHaystackUtil.readConfigPointValue(
                    "analog3 and output and min"
                )
                ao3MaxVal = hsHaystackUtil.readConfigPointValue(
                    "analog3 and output and max"
                )
            }

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao3AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.MODULATING_FAN_SPEED ||
                HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao3AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED
            ) {
                ao3fanLow = hsHaystackUtil.readConfigPointValue(
                    "analog3 and output and low"
                )
                ao3fanMedium = hsHaystackUtil.readConfigPointValue(
                    "analog3 and output and medium"
                )
                ao3fanHigh = hsHaystackUtil.readConfigPointValue(
                    "analog3 and output and high"
                )
            }
            ao3AtRecirculate = hsHaystackUtil.readConfigPointValue(
                "analog3 and recirculate"
            )

        }

        config.analogOut1State = AnalogOutState(
            ao1 == 1,
            HyperStatAssociationUtil.getAnalogOutAssociatedStage(ao1AssociatedTo.toInt()),
            ao1MinVal, ao1MaxVal, ao1fanLow, ao1fanMedium, ao1fanHigh, ao1AtRecirculate
        )
        config.analogOut2State = AnalogOutState(
            ao2 == 1,
            HyperStatAssociationUtil.getAnalogOutAssociatedStage(ao2AssociatedTo.toInt()),
            ao2MinVal, ao2MaxVal, ao2fanLow, ao2fanMedium, ao2fanHigh, ao2AtRecirculate
        )
        config.analogOut3State = AnalogOutState(
            ao3 == 1,
            HyperStatAssociationUtil.getAnalogOutAssociatedStage(ao3AssociatedTo.toInt()),
            ao3MinVal, ao3MaxVal, ao3fanLow, ao3fanMedium, ao3fanHigh, ao3AtRecirculate
        )
        return config
    }

    //config Analog In
    private fun getAnalogInConfigurations(config: HyperStatCpuConfiguration) {
        val ai1 = hsHaystackUtil.readConfigStatus("analog1 and input").toInt()
        val ai2 = hsHaystackUtil.readConfigStatus("analog2 and input").toInt()

        val ai1AssociatedTo = hsHaystackUtil.readConfigAssociation("analog1 and input")
        val ai2AssociatedTo = hsHaystackUtil.readConfigAssociation("analog2 and input")

        config.analogIn1State = AnalogInState(
            ai1 == 1, HyperStatAssociationUtil.getAnalogInStage(ai1AssociatedTo.toInt())
        )
        config.analogIn2State = AnalogInState(
            ai2 == 1, HyperStatAssociationUtil.getAnalogInStage(ai2AssociatedTo.toInt())
        )

    }

    private fun getThermistorInConfigurations(config: HyperStatCpuConfiguration) {
        val th1 = hsHaystackUtil.readConfigStatus("th1 and input").toInt()
        val th2 = hsHaystackUtil.readConfigStatus("th2 and input").toInt()

        val th1AssociatedTo = hsHaystackUtil.readConfigAssociation("th1 and input")
        val th2AssociatedTo = hsHaystackUtil.readConfigAssociation("th2 and input")

        config.thermistorIn1State = Th1InState(
            th1 == 1, HyperStatAssociationUtil.getTh1InStage(th1AssociatedTo.toInt())
        )
        config.thermistorIn2State = Th2InState(
            th2 == 1, HyperStatAssociationUtil.getTh2InStage(th2AssociatedTo.toInt())
        )
    }


    // To Update the existing profile configurations
    private fun updateRelaysConfig(
        newConfiguration: HyperStatCpuConfiguration,
        existingConfiguration: HyperStatCpuConfiguration
    ) {
        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay1State, existingConfiguration.relay1State)) {
            updateRelayDetails(existingConfiguration.relay1State, newConfiguration.relay1State, "relay1", Port.RELAY_ONE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay2State, existingConfiguration.relay2State)) {
            updateRelayDetails(existingConfiguration.relay2State, newConfiguration.relay2State, "relay2", Port.RELAY_TWO, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay3State, existingConfiguration.relay3State)) {
            updateRelayDetails(existingConfiguration.relay3State, newConfiguration.relay3State, "relay3", Port.RELAY_THREE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay4State, existingConfiguration.relay4State)) {
            updateRelayDetails(existingConfiguration.relay4State, newConfiguration.relay4State, "relay4", Port.RELAY_FOUR, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay5State, existingConfiguration.relay5State)) {
            updateRelayDetails(existingConfiguration.relay5State, newConfiguration.relay5State, "relay5", Port.RELAY_FIVE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay6State, existingConfiguration.relay6State)) {
            updateRelayDetails(existingConfiguration.relay6State, newConfiguration.relay6State, "relay6", Port.RELAY_SIX, newConfiguration)
        }
    }

    private fun updateAnalogOutConfig(
        newConfiguration: HyperStatCpuConfiguration,
        existingConfiguration: HyperStatCpuConfiguration
    ) {
        if (!HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut1State, existingConfiguration.analogOut1State)
        ) {
            val changeIn = HyperStatAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut1State, existingConfiguration.analogOut1State
            )
            updateAnalogOutDetails(existingConfiguration.analogOut1State, newConfiguration.analogOut1State, "analog1", Port.ANALOG_OUT_ONE, changeIn, newConfiguration)
        }
        if (!HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut2State, existingConfiguration.analogOut2State)
        ) {
            val changeIn = HyperStatAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut2State, existingConfiguration.analogOut2State
            )
            updateAnalogOutDetails(existingConfiguration.analogOut2State, newConfiguration.analogOut2State, "analog2", Port.ANALOG_OUT_TWO, changeIn, newConfiguration)
        }
        if (!HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut3State, existingConfiguration.analogOut3State)
        ) {
            val changeIn = HyperStatAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut3State, existingConfiguration.analogOut3State
            )
            updateAnalogOutDetails(existingConfiguration.analogOut3State, newConfiguration.analogOut3State, "analog3", Port.ANALOG_OUT_THREE, changeIn, newConfiguration)
        }
    }

    private fun updateAnalogInConfig(
        newConfiguration: HyperStatCpuConfiguration,
        existingConfiguration: HyperStatCpuConfiguration
    ) {
        if (!HyperStatAssociationUtil.isBothAnalogInHasSameConfigs
                (newConfiguration.analogIn1State, existingConfiguration.analogIn1State)
        ) {
            updateAnalogInDetails(newConfiguration.analogIn1State, "analog1", Port.ANALOG_IN_ONE)
        }
        if (!HyperStatAssociationUtil.isBothAnalogInHasSameConfigs
                (newConfiguration.analogIn2State, existingConfiguration.analogIn2State)
        ) {
            updateAnalogInDetails(newConfiguration.analogIn2State, "analog2", Port.ANALOG_IN_TWO)
        }
        CcuLog.d(L.TAG_CCU_HSCPU, "updateAnalogInConfig: Done")
    }

    private fun updateThermistorInConfig(
        newConfiguration: HyperStatCpuConfiguration,
        existingConfiguration: HyperStatCpuConfiguration
    ) {
        if (!HyperStatAssociationUtil.isBothTh1InHasSameConfigs
                (newConfiguration.thermistorIn1State, existingConfiguration.thermistorIn1State)
        ) {
            updateTh1InDetails(newConfiguration.thermistorIn1State)
        }
        if (!HyperStatAssociationUtil.isBothTh2InHasSameConfigs
                (newConfiguration.thermistorIn2State, existingConfiguration.thermistorIn2State)
        ) {
            updateTh2InDetails(newConfiguration.thermistorIn2State)
        }
        CcuLog.i(L.TAG_CCU_HSCPU, "updateThermistorInConfig: Done")
    }


    // Function which updates the Relay new configurations
    private fun updateRelayDetails(
        existingRelay:RelayState,
        relayState: RelayState,
        relayTag: String,
        physicalPort: Port,
        newConfiguration: HyperStatCpuConfiguration?,
    ) {

        val relayId = hsHaystackUtil.readPointID("config and $relayTag and enabled") as String
        val relayAssociatedId = hsHaystackUtil.readPointID(
            "config and $relayTag and association"
        ) as String

        hyperStatPointsUtil.addDefaultValueForPoint(relayId, if (relayState.enabled) 1.0 else 0.0)
        hyperStatPointsUtil.addDefaultValueForPoint(relayAssociatedId, relayState.association.ordinal.toDouble())
        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, relayState.enabled)

        if (relayState.enabled) {
            val pointData: Point = hyperStatPointsUtil.relayConfiguration(
                relayState = relayState
            )
            val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)

            if (pointData.markers.contains("his")) {
                hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
            }

            hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
        }

        if(isRelayFanModeUpdateRequired(existingRelay,relayState,newConfiguration)){
            updateFanMode(newConfiguration!!)
        }
        if(HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(relayState))
            updateConditioningMode()
    }


    // Function which updates the Analog Out new configurations
    private fun updateAnalogOutDetails(
        existingAnalogOutState: AnalogOutState,
        analogOutState: AnalogOutState,
        analogOutTag: String,
        physicalPort: Port,
        changeIn: AnalogOutChanges,
        newConfiguration: HyperStatCpuConfiguration?
    ) {

        // Read enable/disable Point ID
        val analogOutId = hsHaystackUtil.readPointID(
            "config and $analogOutTag and output and enabled"
        ) as String

        // associated  Point ID
        val analogOutAssociatedId = hsHaystackUtil.readPointID(
            "config and $analogOutTag and output and association"
        ) as String

        // Read Analog Out min max fan config Point Id's
        val minPointId = hsHaystackUtil.readPointID("$analogOutTag and output and min")
        val maxPointId = hsHaystackUtil.readPointID("$analogOutTag and output and max")
        // Read Fan config Points
        val fanLowPointId = hsHaystackUtil.readPointID("$analogOutTag and output and low")
        val fanMediumPointId = hsHaystackUtil.readPointID("$analogOutTag and output and medium")
        val fanHighPointId = hsHaystackUtil.readPointID("$analogOutTag and output and high")

        val analogAtRecirculate = hsHaystackUtil.readPointID("$analogOutTag and recirculate")

        CcuLog.d(L.TAG_CCU_HSCPU, "Reconfiguration changeIn $changeIn")


        // Do the update when change is found in configuration
        if(changeIn != AnalogOutChanges.NOCHANGE
            && changeIn != AnalogOutChanges.ENABLED
            && changeIn != AnalogOutChanges.MAPPING) {
            if (fanHighPointId != null)
                updatePointValueChangeRequired(fanHighPointId, analogOutState.perAtFanHigh)
            if (fanMediumPointId != null)
                updatePointValueChangeRequired(fanMediumPointId, analogOutState.perAtFanMedium)
            if (fanLowPointId != null)
                updatePointValueChangeRequired(fanLowPointId, analogOutState.perAtFanLow)
            if (analogAtRecirculate != null)
                updatePointValueChangeRequired(analogAtRecirculate, analogOutState.voltageAtRecirculate)

            if (minPointId != null) {
                updatePointValueChangeRequired(minPointId, analogOutState.voltageAtMin)
                val pointType = "${analogOutState.voltageAtMin.toInt()}-${analogOutState.voltageAtMax.toInt()}v"
                DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)
            }
            if (maxPointId != null) {
                updatePointValueChangeRequired(maxPointId, analogOutState.voltageAtMax)
                val pointType = "${analogOutState.voltageAtMin.toInt()}-${analogOutState.voltageAtMax.toInt()}v"
                DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)
            }
            return
        }

        // Update the Enable , association status with configurations
        hyperStatPointsUtil.addDefaultValueForPoint(analogOutId, if (analogOutState.enabled) 1.0 else 0.0)
        hyperStatPointsUtil.addDefaultValueForPoint(analogOutAssociatedId, analogOutState.association.ordinal.toDouble())

        // Check if logical logical,min,max, fan config  Point exist
        // (This situation is when previous state was not enabled for this analog now it is enabled )
        // SO there's a possibility that logical,min,max, fan config id can be null

        if (minPointId != null) hsHaystackUtil.removePoint(minPointId)
        if (maxPointId != null) hsHaystackUtil.removePoint(maxPointId)

        if (fanLowPointId != null) hsHaystackUtil.removePoint(fanLowPointId)
        if (fanMediumPointId != null) hsHaystackUtil.removePoint(fanMediumPointId)
        if (fanHighPointId != null) hsHaystackUtil.removePoint(fanHighPointId)

        CcuLog.i(L.TAG_CCU_HSCPU, "Reconfiguration analogOutState.enabled ${analogOutState.enabled}")
        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, analogOutState.enabled)
        if (analogOutState.enabled) {

            if (analogOutState.association != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = hyperStatPointsUtil.analogOutConfiguration(
                    analogOutState = analogOutState,
                    analogTag = analogOutTag
                )
                val minPoint = (pointData.second as Pair<*, *>)
                val maxPoint = (pointData.third as Pair<*, *>)

                val pointId = hyperStatPointsUtil.addPointToHaystack(pointData.first as Point)
                val newMinPointId = hyperStatPointsUtil.addPointToHaystack(minPoint.first as Point)
                val newMaxPointId = hyperStatPointsUtil.addPointToHaystack(maxPoint.first as Point)

                if ((pointData.first as Point).markers.contains("his")) {
                    hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
                }

                hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                hyperStatPointsUtil.addDefaultValueForPoint(
                    newMinPointId,
                    analogOutState.voltageAtMin
                )
                hyperStatPointsUtil.addDefaultValueForPoint(
                    newMaxPointId,
                    analogOutState.voltageAtMax
                )

                val pointType =
                    "${analogOutState.voltageAtMin.toInt()}-${analogOutState.voltageAtMax.toInt()}v"
                DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
                DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)
            } else {
                val pointData: Triple<Point, Any?, Any?> = hyperStatPointsUtil.analogOutConfiguration1()
                val pointId = hyperStatPointsUtil.addPointToHaystack(pointData.first)
                val pointType =
                    "${analogOutVoltageMin}-${analogOutVoltageMax}v"
                if ((pointData.first).markers.contains("his")) {
                    hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                }
                DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
                DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)
            }

            // check if the new state of analog is mapped to Fan Speed
            // then will create new Points for fan configurations

            if (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState) || HyperStatAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(analogOutState)) {

                // Create Fan configuration Points
                val fanConfigPoints: Triple<Point, Point, Point> = hyperStatPointsUtil.createFanLowMediumHighPoint(
                    analogTag = analogOutTag
                )

                // Add to Haystack
                val lowPointId = hyperStatPointsUtil.addPointToHaystack(fanConfigPoints.first)
                val mediumPointId = hyperStatPointsUtil.addPointToHaystack(fanConfigPoints.second)
                val highPointId = hyperStatPointsUtil.addPointToHaystack(fanConfigPoints.third)

                // Update the default values
                hyperStatPointsUtil.addDefaultValueForPoint(lowPointId, analogOutState.perAtFanLow)
                hyperStatPointsUtil.addDefaultValueForPoint(mediumPointId, analogOutState.perAtFanMedium)
                hyperStatPointsUtil.addDefaultValueForPoint(highPointId, analogOutState.perAtFanHigh)
            }

        }
        CcuLog.i(L.TAG_CCU_ZONE, "changeIn: Analog $changeIn  ${HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)}")

        if(isAnalogFanModeUpdateRequired(existingAnalogOutState,analogOutState,changeIn,newConfiguration)){
            updateFanMode(newConfiguration!!)
        }

        if (newConfiguration != null && ((changeIn == AnalogOutChanges.MAPPING ||changeIn == AnalogOutChanges.ENABLED)
                    && HyperStatAssociationUtil.isAnalogAssociatedToAnyOfConditioningModes(analogOutState))) {
            updateConditioningMode()
        }
    }

    private fun isRelayFanModeUpdateRequired(
        existingRelay:RelayState,
        relayState: RelayState,
        newConfiguration: HyperStatCpuConfiguration?,
    ): Boolean {
        return (newConfiguration != null &&
               (HyperStatAssociationUtil.isRelayAssociatedToFan(relayState) ||
               HyperStatAssociationUtil.isRelayAssociatedToFan(existingRelay)))
   }

    private fun isAnalogFanModeUpdateRequired(
        existingAnalogOutState: AnalogOutState,
        analogOutState: AnalogOutState,
        changeIn: AnalogOutChanges,
        newConfiguration: HyperStatCpuConfiguration?
    ): Boolean {
        return (newConfiguration != null && ((changeIn == AnalogOutChanges.MAPPING ||changeIn == AnalogOutChanges.ENABLED)
                    && (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)
                    || HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(existingAnalogOutState) || HyperStatAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(existingAnalogOutState))))
    }

     private fun updateConditioningMode() {
         CcuLog.d(L.TAG_CCU_ZONE, "updateConditioningMode: ")

        val conditioningModeId = hsHaystackUtil.readPointID("zone and sp and conditioning and mode")

        if (conditioningModeId!!.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "ConditioningMode point does not exist ")
            return
        }

        val curCondMode = haystack.readDefaultValById(conditioningModeId)
        var conditioningMode = curCondMode
        CcuLog.i(L.TAG_CCU_HSCPU, "updateConditioningMode: curCondMode $curCondMode")

         when(HSHaystackUtil.getPossibleConditioningModeSettings(nodeAddress)){
             PossibleConditioningMode.BOTH -> {
                 if(curCondMode != StandaloneConditioningMode.AUTO.ordinal.toDouble())
                     conditioningMode = StandaloneConditioningMode.AUTO.ordinal.toDouble()
             }
             PossibleConditioningMode.COOLONLY, PossibleConditioningMode.HEATONLY , PossibleConditioningMode.OFF -> {
                 if(curCondMode != StandaloneConditioningMode.OFF.ordinal.toDouble())
                     conditioningMode = StandaloneConditioningMode.OFF.ordinal.toDouble()
             }
         }
        CcuLog.i(L.TAG_CCU_ZONE, "adjustCPUConditioningMode $curCondMode -> $conditioningMode")
        if (curCondMode != conditioningMode) {
            haystack.writeDefaultValById(conditioningModeId, conditioningMode)
            haystack.writeHisValById(conditioningModeId, conditioningMode)
        }
    }

     private fun updateFanMode(config: HyperStatCpuConfiguration) {
        val fanLevel = getSelectedFanLevel(config)
        val curFanSpeed = hsHaystackUtil.readPointValue("zone and sp and fan and operation and mode")
         CcuLog.i(L.TAG_CCU_HSCPU, "updateFanMode: fanLevel $fanLevel curFanSpeed $curFanSpeed")
         val fallbackFanSpeed: Double = if(fanLevel == 0) { 0.0 } else {
             if (fanLevel > 0 && curFanSpeed.toInt() != StandaloneFanStage.AUTO.ordinal) {
                 StandaloneFanStage.AUTO.ordinal.toDouble()
             } else {
                 curFanSpeed
             }
         }
         val fanModePointId = hsHaystackUtil.readPointID("zone and sp and fan and operation and mode")
         CcuLog.i(L.TAG_CCU_HSCPU, "updateFanMode: fallbackFanSpeed $fallbackFanSpeed")
         if(fanModePointId != null)
            hsHaystackUtil.writeDefaultWithHisValue(fanModePointId, fallbackFanSpeed)
    }

    fun getCurrentTemp(): Double {
        return hsHaystackUtil.getCurrentTemp()
    }

    /**
     * Function  which collects all the logical associated points
     */
    override fun getLogicalPointList(): HashMap<Any, String> {
        val config = getConfiguration()
        val logicalPoints: HashMap<Any, String> = HashMap()

        if (config.relay1State.enabled) logicalPoints[Port.RELAY_ONE] = hyperStatPointsUtil.getCpuRelayLogicalPoint(config.relay1State.association).id
        if (config.relay2State.enabled) logicalPoints[Port.RELAY_TWO] = hyperStatPointsUtil.getCpuRelayLogicalPoint(config.relay2State.association).id
        if (config.relay3State.enabled) logicalPoints[Port.RELAY_THREE] = hyperStatPointsUtil.getCpuRelayLogicalPoint(config.relay3State.association).id
        if (config.relay4State.enabled) logicalPoints[Port.RELAY_FOUR] = hyperStatPointsUtil.getCpuRelayLogicalPoint(config.relay4State.association).id
        if (config.relay5State.enabled) logicalPoints[Port.RELAY_FIVE] = hyperStatPointsUtil.getCpuRelayLogicalPoint(config.relay5State.association).id
        if (config.relay6State.enabled) logicalPoints[Port.RELAY_SIX] = hyperStatPointsUtil.getCpuRelayLogicalPoint(config.relay6State.association).id

        if (config.analogOut1State.enabled) logicalPoints[Port.ANALOG_OUT_ONE] = hyperStatPointsUtil.getCpuAnalogOutLogicalPoint(config.analogOut1State.association).id
        if (config.analogOut2State.enabled) logicalPoints[Port.ANALOG_OUT_TWO] = hyperStatPointsUtil.getCpuAnalogOutLogicalPoint(config.analogOut2State.association).id
        if (config.analogOut3State.enabled) logicalPoints[Port.ANALOG_OUT_THREE] = hyperStatPointsUtil.getCpuAnalogOutLogicalPoint(config.analogOut3State.association).id

        CcuLog.d(L.TAG_CCU_HSCPU, "====== Logical Points list : ====")
        logicalPoints.forEach { (key, id) -> CcuLog.d(L.TAG_CCU_HSCPU, "key : $key  : $id") }
        CcuLog.d(L.TAG_CCU_HSCPU, "=================================")
        return logicalPoints
    }
}
