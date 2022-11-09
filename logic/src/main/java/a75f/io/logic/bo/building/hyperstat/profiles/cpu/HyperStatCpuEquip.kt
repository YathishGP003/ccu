package a75f.io.logic.bo.building.hyperstat.profiles.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
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
import a75f.io.logic.tuners.HyperstatCpuTuners
import a75f.io.logic.util.RxTask
import android.util.Log

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

    private var masterPoints: HashMap<Any, String> = HashMap()

    private var siteDis: String? = null
    private var tz: String? = null
    private var equipDis: String? = null
    private var gatewayRef: String? = null
    var equipRef: String? = null
    private var roomRef: String? = null
    private var floorRef: String? = null
    private var systemEquip = haystack.readEntity("equip and system") as HashMap<Any, Any>

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
        Log.i(L.TAG_CCU_HSCPU, "New Profile  Initialising Points: ")
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
        Log.i(L.TAG_CCU_HSCPU, "New Profile  New Equip Created: $equipRef")

        // Init required reference
        init(HyperstatProfileNames.HSCPU, equipRef!!,floorRef!!,roomRef!!,nodeAddress,equipDis!!)

        // add Tuner Points
        createHyperStatTunerPoints(
            equipRef!!, equipDis!!, roomRef!!, floorRef!!
        )
        Log.i(L.TAG_CCU_HSCPU, "New Profile  Tuners Created")

        // Create config points
        createProfileConfigurationPoints(hyperStatConfig = config)

        Log.i(L.TAG_CCU_HSCPU, "New Profile Profile configuration points are created ")
        // Create Logical Points
       createProfileLogicalPoints(hyperStatConfig = config)

        Log.i(L.TAG_CCU_HSCPU, "New Profile  logical points are created")

        configHyperStatDevice(config, profileEquip)

        updateConditioningMode()
        // Syncing the Points
        haystack.syncEntityTree()

        return
    }

    fun initEquipReference(node: Short) {
        val equip = haystack.read("equip and hyperstat and cpu and group == \"$node\"")
        if (equip.isEmpty()) {
            Log.i(L.TAG_CCU_HSCPU, " Unable to find the equip details for node $node ")
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
            config.analogOut1State.enabled, config.analogOut1State.voltageAtMin.toInt(),config.analogOut1State.voltageAtMax.toInt(),
            config.analogOut2State.enabled, config.analogOut2State.voltageAtMin.toInt(),config.analogOut2State.voltageAtMax.toInt(),
            config.analogOut3State.enabled, config.analogOut3State.voltageAtMin.toInt(),config.analogOut3State.voltageAtMax.toInt(),
            masterPoints, hyperStatDevice
        )

        profileEquip.setupDeviceAnalogIns(
            config.analogIn1State.enabled,HyperStatAssociationUtil.getSensorNameByType(config.analogIn1State.association),
            config.analogIn1State.enabled,HyperStatAssociationUtil.getSensorNameByType(config.analogIn1State.association),
            masterPoints, hyperStatDevice
        )

        // In Hyperstat CPU profile th1 will be always configured with Airflow Sensor so
        // it is 3 constant because in sensor manager class Airflow Sensor position in external sensor list is 3
        // do not change it
        profileEquip.setupDeviceThermistors(
            config.isEnableDoorWindowSensor, "3",
            config.isEnableDoorWindowSensor,HyperStatAssociationUtil.getSensorNameByType(AnalogInAssociation.DOOR_WINDOW_SENSOR),
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

        val loopOutputPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createLoopOutputPoints()

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
                ConfigState(hyperStatConfig.isEnableAirFlowTempSensor, 0), // aiflow sensor is constant
                ConfigState(hyperStatConfig.isEnableDoorWindowSensor, 0) // this is cpu config not 2 pipe configuration
            ,false
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

        val allConfigPoints = arrayOf(
            configPointsList, relayConfigPoints, analogOutConfigPoints,
            analogInConfigPoints, thConfigPointsList, userIntentPointsList,
            co2ConfigPointsList, loopOutputPoints,vocPmPointsList,deviceDisplayConfigPoints
        )
        Log.i(L.TAG_CCU_HSCPU, "adding : points default value ")
        hyperStatPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = allConfigPoints)

    }

    //Function to create Logical Points
    private fun createProfileLogicalPoints(hyperStatConfig: HyperStatCpuConfiguration) {
        Log.i(L.TAG_CCU_HSCPU, "createProfileLogicalPoints: ")

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
                 hyperStatConfig.isEnableAirFlowTempSensor,
                 hyperStatConfig.isEnableDoorWindowSensor, false
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

        Log.i(L.TAG_CCU_HSCPU, "Done with Logical points creation")

    }


    // Function to update the existing profile
    override fun updateConfiguration(configuration: BaseProfileConfiguration) {
        Log.i(L.TAG_CCU_HSCPU, "===========HyperStat profile Update  ============")

        val updatedHyperStatConfig =  configuration as HyperStatCpuConfiguration
        val presetConfiguration = getConfiguration()

        updateGeneralConfiguration(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAirFlowTempSensorConfiguration(presetConfiguration.isEnableAirFlowTempSensor,updatedHyperStatConfig.isEnableAirFlowTempSensor)
        updateDoorWindowSensorTh2Configuration(presetConfiguration.isEnableDoorWindowSensor,updatedHyperStatConfig.isEnableDoorWindowSensor)
        updateRelaysConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogOutConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogInConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)

        LogicalPointsUtil.cleanCpuLogicalPoints(updatedHyperStatConfig,equipRef!!)
        Log.i(L.TAG_CCU_HSCPU, "Profile update has been completed  ")
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
    }

    // collect the existing profile configurations
    fun getConfiguration(): HyperStatCpuConfiguration {
        val config = HyperStatCpuConfiguration()

        getRelayConfigurations(config)
        getAnalogOutConfigurations(config)
        getAnalogInConfigurations(config)

        config.temperatureOffset = hsHaystackUtil.getTempOffValue()
        config.isEnableAutoForceOccupied = hsHaystackUtil.isAutoForceOccupyEnabled()
        config.isEnableAutoAway =  hsHaystackUtil.isAutoAwayEnabled()
        config.isEnableAirFlowTempSensor = hsHaystackUtil.isAirFlowSensorTh1Enabled()
        config.isEnableDoorWindowSensor = hsHaystackUtil.isDoorWindowSensorTh2Enabled()
        config.zoneCO2DamperOpeningRate = hsHaystackUtil.getCo2DamperOpeningConfigValue()
        config.zoneCO2Threshold = hsHaystackUtil.getCo2DamperThresholdConfigValue()
        config.zoneCO2Target = hsHaystackUtil.getCo2TargetConfigValue()
        config.zoneVOCThreshold = hsHaystackUtil.getVocThresholdConfigValue()
        config.zoneVOCTarget = hsHaystackUtil.getVocTargetConfigValue()
        config.zonePm2p5Threshold = hsHaystackUtil.getPm2p5ThresholdConfigValue()
        config.zonePm2p5Target = hsHaystackUtil.getPm2p5TargetConfigValue()

        config.displayHumidity = hsHaystackUtil.getDisplayHumidity() == 1.0
        config.displayCo2 = hsHaystackUtil.getDisplayCo2() == 1.0
        config.displayVOC = hsHaystackUtil.getDisplayVoc() == 1.0
        config.displayPp2p5 = hsHaystackUtil.getDisplayP2p5() == 1.0

        return config
    }

    //  To show the existing profile configurations
    // config relays
    private fun getRelayConfigurations(config: HyperStatCpuConfiguration) {
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

    }

    //config Analog Out
    private fun getAnalogOutConfigurations(config: HyperStatCpuConfiguration) {
        val ao1 = hsHaystackUtil.readConfigStatus("analog1 and output  ").toInt()
        val ao2 = hsHaystackUtil.readConfigStatus("analog2 and output ").toInt()
        val ao3 = hsHaystackUtil.readConfigStatus("analog3 and output ").toInt()

        val ao1AssociatedTo = hsHaystackUtil.readConfigAssociation("analog1 and output ")
        val ao2AssociatedTo = hsHaystackUtil.readConfigAssociation("analog2 and output ")
        val ao3AssociatedTo = hsHaystackUtil.readConfigAssociation("analog3 and output ")


        var ao1MinVal = 2.0
        var ao1MaxVal = 10.0

        var ao1fanLow = 30.0
        var ao1fanMedium = 60.0
        var ao1fanHigh = 100.0

        if (ao1 == 1) {
            ao1MinVal = hsHaystackUtil.readConfigPointValue(
                "analog1 and output and min"
            )
            ao1MaxVal = hsHaystackUtil.readConfigPointValue(
                "analog1 and output and max"
            )

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao1AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.FAN_SPEED
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
        }
        // Default Values
        var ao2MinVal = 2.0
        var ao2MaxVal = 10.0

        var ao2fanLow = 30.0
        var ao2fanMedium = 60.0
        var ao2fanHigh = 100.0

        if (ao2 == 1) {
            ao2MinVal = hsHaystackUtil.readConfigPointValue(
                "analog2 and output and min"
            )
            ao2MaxVal = hsHaystackUtil.readConfigPointValue(
                "analog2 and output and max"
            )

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao2AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.FAN_SPEED
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
        }

        var ao3MinVal = 2.0
        var ao3MaxVal = 10.0

        var ao3fanLow = 30.0
        var ao3fanMedium = 60.0
        var ao3fanHigh = 100.0

        if (ao3 == 1) {
            ao3MinVal = hsHaystackUtil.readConfigPointValue(
                "analog3 and output and min"
            )
            ao3MaxVal = hsHaystackUtil.readConfigPointValue(
                "analog3 and output and max"
            )

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao3AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.FAN_SPEED
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
        }

        config.analogOut1State = AnalogOutState(
            ao1 == 1,
            HyperStatAssociationUtil.getAnalogOutAssociatedStage(ao1AssociatedTo.toInt()),
            ao1MinVal, ao1MaxVal, ao1fanLow, ao1fanMedium, ao1fanHigh
        )
        config.analogOut2State = AnalogOutState(
            ao2 == 1,
            HyperStatAssociationUtil.getAnalogOutAssociatedStage(ao2AssociatedTo.toInt()),
            ao2MinVal, ao2MaxVal, ao2fanLow, ao2fanMedium, ao2fanHigh
        )
        config.analogOut3State = AnalogOutState(
            ao3 == 1,
            HyperStatAssociationUtil.getAnalogOutAssociatedStage(ao3AssociatedTo.toInt()),
            ao3MinVal, ao3MaxVal, ao3fanLow, ao3fanMedium, ao3fanHigh
        )
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


    // To Update the existing profile configurations
    private fun updateRelaysConfig(
        newConfiguration: HyperStatCpuConfiguration,
        existingConfiguration: HyperStatCpuConfiguration
    ) {
        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay1State, existingConfiguration.relay1State)) {
            updateRelayDetails(newConfiguration.relay1State, "relay1", Port.RELAY_ONE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay2State, existingConfiguration.relay2State)) {
            updateRelayDetails(newConfiguration.relay2State, "relay2", Port.RELAY_TWO, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay3State, existingConfiguration.relay3State)) {
            updateRelayDetails(newConfiguration.relay3State, "relay3", Port.RELAY_THREE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay4State, existingConfiguration.relay4State)) {
            updateRelayDetails(newConfiguration.relay4State, "relay4", Port.RELAY_FOUR, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay5State, existingConfiguration.relay5State)) {
            updateRelayDetails(newConfiguration.relay5State, "relay5", Port.RELAY_FIVE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay6State, existingConfiguration.relay6State)) {
            updateRelayDetails(newConfiguration.relay6State, "relay6", Port.RELAY_SIX, newConfiguration)
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
            updateAnalogOutDetails(newConfiguration.analogOut1State, "analog1", Port.ANALOG_OUT_ONE, changeIn, newConfiguration)
        }
        if (!HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut2State, existingConfiguration.analogOut2State)
        ) {
            val changeIn = HyperStatAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut2State, existingConfiguration.analogOut2State
            )
            updateAnalogOutDetails(newConfiguration.analogOut2State, "analog2", Port.ANALOG_OUT_TWO, changeIn, newConfiguration)
        }
        if (!HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut3State, existingConfiguration.analogOut3State)
        ) {
            val changeIn = HyperStatAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut3State, existingConfiguration.analogOut3State
            )
            updateAnalogOutDetails(newConfiguration.analogOut3State, "analog3", Port.ANALOG_OUT_THREE, changeIn, newConfiguration)
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
        Log.i(L.TAG_CCU_HSCPU, "updateAnalogInConfig: Done")
    }


    // Function which updates the Relay new configurations
    private fun updateRelayDetails(
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

        CcuLog.i(L.TAG_CCU_ZONE, "updateRelayDetails: ${relayState.association.name} ${relayState.enabled}")
        CcuLog.i(L.TAG_CCU_ZONE, "isRelayAssociatedToAnyOfConditioningModes: "+HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(relayState))
       if ( newConfiguration != null && HyperStatAssociationUtil.isRelayAssociatedToFan(relayState)){
           updateFanMode(newConfiguration)
       }
        if(HyperStatAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(relayState))
            updateConditioningMode()
    }


    // Function which updates the Analog Out new configurations
    private fun updateAnalogOutDetails(
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

        Log.i(L.TAG_CCU_HSCPU, "Reconfiguration changeIn $changeIn")


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
        // SO there's a possibility that logical,min,max, fan config  id can be null

        if (minPointId != null) hsHaystackUtil.removePoint(minPointId)
        if (maxPointId != null) hsHaystackUtil.removePoint(maxPointId)

        if (fanLowPointId != null) hsHaystackUtil.removePoint(fanLowPointId)
        if (fanMediumPointId != null) hsHaystackUtil.removePoint(fanMediumPointId)
        if (fanHighPointId != null) hsHaystackUtil.removePoint(fanHighPointId)

        Log.i(L.TAG_CCU_HSCPU, "Reconfiguration analogOutState.enabled ${analogOutState.enabled}")
        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, analogOutState.enabled)
        if (analogOutState.enabled) {

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
            hyperStatPointsUtil.addDefaultValueForPoint(newMinPointId, analogOutState.voltageAtMin)
            hyperStatPointsUtil.addDefaultValueForPoint(newMaxPointId, analogOutState.voltageAtMax)

            val pointType = "${analogOutState.voltageAtMin.toInt()}-${analogOutState.voltageAtMax.toInt()}v"
            DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
            DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)

            // check if the new state of analog is mapped to Fan Speed
            // then will create new Points for fan configurations

            if (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) {

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
        if (newConfiguration != null && ((changeIn == AnalogOutChanges.MAPPING ||changeIn == AnalogOutChanges.ENABLED)
                    && HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState))) {
            updateFanMode(newConfiguration)
        }

        if (newConfiguration != null && ((changeIn == AnalogOutChanges.MAPPING ||changeIn == AnalogOutChanges.ENABLED)
                    && HyperStatAssociationUtil.isAnalogAssociatedToAnyOfConditioningModes(analogOutState))) {
            updateConditioningMode()
        }
    }


    // Function to check the changes in the point and do the update with new value
    private fun updatePointValueChangeRequired(pointId: String, newValue: Double){
        val presentValue = haystack.readDefaultValById(pointId)
        if(presentValue != newValue)
            hyperStatPointsUtil.addDefaultValueForPoint(pointId,newValue)
    }

    // Function which updates the Analog In new configurations
    private fun updateAnalogInDetails(
        analogInState: AnalogInState,
        analogInTag: String,
        physicalPort: Port
    ) {
        val analogInId = hsHaystackUtil.readPointID("config and $analogInTag and input and enabled") as String
        val analogInAssociatedId =
            hsHaystackUtil.readPointID("config and $analogInTag and input and association") as
                    String
        hyperStatPointsUtil.addDefaultValueForPoint(analogInId, if (analogInState.enabled) 1.0 else 0.0)
        hyperStatPointsUtil.addDefaultValueForPoint(analogInAssociatedId, analogInState.association.ordinal.toDouble())

        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, analogInState.enabled)
        if (analogInState.enabled) {
            val pointData: Point = hyperStatPointsUtil.analogInConfiguration(
                analogInState = analogInState,
                analogTag = analogInTag
            )
            val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)

            DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
            val pointType = HyperStatAssociationUtil.getSensorNameByType(analogInState.association)
            DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)
        }

    }


     private fun updateConditioningMode() {
         CcuLog.i(L.TAG_CCU_ZONE, "updateConditioningMode: ")

        val conditioningModeId = hsHaystackUtil.readPointID("zone and sp and conditioning and mode")

        if (conditioningModeId!!.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "ConditioningMode point does not exist ")
            return
        }

        val curCondMode = haystack.readDefaultValById(conditioningModeId)
        var conditioningMode = curCondMode
        Log.i(L.TAG_CCU_HSCPU, "updateConditioningMode: curCondMode $curCondMode")

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

     fun updateFanMode(config: HyperStatCpuConfiguration) {
        val fanLevel = getSelectedFanLevel(config)
        val curFanSpeed = hsHaystackUtil.readPointValue("zone and sp and fan and operation and mode")
        var fallbackFanSpeed = StandaloneFanStage.OFF.ordinal.toDouble() // Indicating always OFF
         Log.i(L.TAG_CCU_HSCPU, "updateFanMode: fanLevel $fanLevel curFanSpeed $curFanSpeed")
        if (fanLevel > 0 && curFanSpeed.toInt() != StandaloneFanStage.AUTO.ordinal) {
            fallbackFanSpeed = StandaloneFanStage.AUTO.ordinal.toDouble()
        }
         val fanModePointId = hsHaystackUtil.readPointID("zone and sp and fan and operation and mode")
         Log.i(L.TAG_CCU_HSCPU, "updateFanMode: fallbackFanSpeed $fallbackFanSpeed")
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

        Log.i(L.TAG_CCU_HSCPU, "====== Logical Points list : ====")
        logicalPoints.forEach { (key, id) -> Log.i(L.TAG_CCU_HSCPU, "key : $key  : $id") }
        Log.i(L.TAG_CCU_HSCPU, "=================================")
        return logicalPoints
    }
}
