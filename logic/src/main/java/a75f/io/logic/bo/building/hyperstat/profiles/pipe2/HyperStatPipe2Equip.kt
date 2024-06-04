package a75f.io.logic.bo.building.hyperstat.profiles.pipe2

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.heartbeat.HeartBeat
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.*
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.Th1InState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.Th2InState
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuConfiguration
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.HyperStatDevice
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint.Companion.addOTAStatusPoint
import a75f.io.logic.tuners.HyperStat2PipeTuners
import a75f.io.logic.util.RxTask

/**
 * Created by Manjunath K on 01-08-2022.
 */

class HyperStatPipe2Equip(val node: Short): HyperStatEquip()  {


    val haystack: CCUHsApi = CCUHsApi.getInstance()
    private val profileTag = "hyperstat2pipe"

    private var masterPoints: HashMap<Any, String> = HashMap()

    private var siteDis: String? = null
    private var tz: String? = null
    private var equipDis: String? = null
    private var gatewayRef: String? = null
    var equipRef: String? = null
    private var roomRef: String? = null
    private var floorRef: String? = null
    private var systemEquip = haystack.readEntity("equip and system and not modbus and not connectModule") as HashMap<Any, Any>


    var lastWaterValveTurnedOnTime: Long = System.currentTimeMillis()
    var waterSamplingStartTime: Long = 0


    companion object {
        fun getHyperStatEquipRef(nodeAddress: Short): HyperStatPipe2Equip {
            val equip = HyperStatPipe2Equip(nodeAddress)
            equip.initEquipReference(nodeAddress)
            return equip
        }
    }

    fun initEquipReference(node: Short) {
        val equip = haystack.read("equip and hyperstat and pipe2 and fcu and group == \"$node\"")
        if (equip.isEmpty()) {
            CcuLog.e(L.TAG_CCU_HSPIPE2, " Unable to find the equip details for node $node ")
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
        init(HyperstatProfileNames.HSPIPE2, equipRef!!,floorRef!!,roomRef!!,node.toInt(),equipDis!!)
    }

    override fun initializePoints(
        baseConfig: BaseProfileConfiguration,
        room: String,
        floor: String,
        node: Short
    ) {
        val config = baseConfig as HyperStatPipe2Configuration

        CcuLog.d(L.TAG_CCU_HSPIPE2, "New Profile  Initialising Points: ")
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
            HyperstatProfileNames.HSPIPE2,
            roomRef!!, floorRef!!,config.priority.toString(),nodeAddress.toString(),
            equipDis!!, ProfileType.HYPERSTAT_TWO_PIPE_FCU , arrayOf(Tags.FCU)
        )
        CcuLog.d(L.TAG_CCU_HSPIPE2, "New Profile  New Equip Created: $equipRef")

        // Init required reference
        init(HyperstatProfileNames.HSPIPE2, equipRef!!,floorRef!!,roomRef!!,nodeAddress,equipDis!!)

        // add Tuner Points
        createHyperstatTunerPoints(
            equipRef!!, equipDis!!, roomRef!!, floorRef!!
        )
        CcuLog.d(L.TAG_CCU_HSPIPE2, "New Profile  Tuners Created")

        // Create config points
        createProfileConfigurationPoints(hyperStatConfig = config)

        CcuLog.d(L.TAG_CCU_HSPIPE2, "New Profile Profile configuration points are created ")
        // Create Logical Points
        createProfileLogicalPoints(hyperStatConfig = config)

        CcuLog.d(L.TAG_CCU_HSPIPE2, "New Profile  logical points are created")

        configHyperStatDevice(config, profileEquip)

        addOTAStatusPoint(
            "${Tags.HS}-$nodeAddress", equipRef!!, basicInfo.siteRef,
            roomRef!!, floorRef!!, nodeAddress, basicInfo.timeZone, haystack
        )
        // Syncing the Points
        haystack.syncEntityTree()
    }


    private fun createHyperstatTunerPoints(
        equipRef: String, equipDis: String, roomRef: String, floorRef: String,
    ) {
        RxTask.executeAsync {
            HyperStat2PipeTuners.addHyperstatModuleTuners(
                CCUHsApi.getInstance(), basicInfo.siteRef,
                equipRef, equipDis, basicInfo.timeZone, roomRef, floorRef
            )
        }
    }


    //function which creates Profile configuration Points
    private fun createProfileConfigurationPoints(hyperStatConfig: HyperStatPipe2Configuration) {

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
            ProfileType.HYPERSTAT_TWO_PIPE_FCU
        )

        val analogOutConfigPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createIsAnalogOutEnabledConfigPoints(
                ConfigState(hyperStatConfig.analogOut1State.enabled, hyperStatConfig.analogOut1State.association.ordinal),
                ConfigState(hyperStatConfig.analogOut2State.enabled, hyperStatConfig.analogOut2State.association.ordinal),
                ConfigState(hyperStatConfig.analogOut3State.enabled, hyperStatConfig.analogOut3State.association.ordinal),
                ProfileType.HYPERSTAT_TWO_PIPE_FCU
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
                hyperStatPointsUtil.getPipe2DefaultFanSpeed(hyperStatConfig),
                hyperStatPointsUtil.getPipe2DefaultConditioningMode()
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
            co2ConfigPointsList, loopOutputPoints,vocPmPointsList, deviceDisplayConfigPoints
        )
        CcuLog.d(L.TAG_CCU_HSPIPE2, "adding : points default value ")
        hyperStatPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = allConfigPoints)

    }


    //Function to create Logical Points
    private fun createProfileLogicalPoints(hyperStatConfig: HyperStatPipe2Configuration) {

        CcuLog.d(L.TAG_CCU_HSPIPE2, "createProfileLogicalPoints: ")

        val temperaturePointsList: MutableList<Triple<Point, Any, Any>> = hyperStatPointsUtil
            .temperatureScheduleLogicalPoints()


        val relayPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatPointsUtil
            .create2PipeRelayLogicalPoints(
                hyperStatConfig = hyperStatConfig
            )

        val analogOutPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatPointsUtil
            .create2PipeAnalogOutLogicalPoints(
                hyperStatConfig = hyperStatConfig
            )

        val analogInPointsList: MutableList<Triple<Point, Any, Any>> =
            hyperStatPointsUtil.createConfigAnalogInLogicalPoints(
                hyperStatConfig.analogIn1State,hyperStatConfig.analogIn2State
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

        CcuLog.d(L.TAG_CCU_HSPIPE2, "Done with Logical points creation")
    }


    // Function which configure HyperStat Device configurations
    private fun configHyperStatDevice(config: HyperStatPipe2Configuration, profileEquip: HyperStatEquip) {

        val hyperStatDevice = HyperStatDevice(
            nodeAddress, basicInfo.siteRef, floorRef, roomRef, equipRef, HyperstatProfileNames.HSPIPE2
        )

        val heartBeatId = CCUHsApi.getInstance().addPoint(
            nodeAddress.let {
                HeartBeat.getHeartBeatPoint(
                    equipDis, equipRef,
                    basicInfo.siteRef, roomRef, floorRef, it, HyperstatProfileNames.HSPIPE2, tz
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
            config.analogIn2State.enabled,HyperStatAssociationUtil.getSensorNameByType(config.analogIn2State.association),
            masterPoints, hyperStatDevice
        )

        // In Hyperstat  profiles th1 will be always configured with Airflow Sensor so
        // it is 3 constant because in sensor manager class Airflow Sensor position in external sensor list is 3
        // do not change it
        // Added new sensor type as supply water temp at 19 position
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



    // collect the existing profile configurations
    fun getConfiguration(): HyperStatPipe2Configuration {
        val config = HyperStatPipe2Configuration()

        getRelayConfigurations(config)
        getAnalogOutConfigurations(config)
        getAnalogInConfigurations(config)
        getThermistorInConfigurations(config)
        getDeviceDisplayConfiguration(config)

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

        return config
    }


    private fun getDeviceDisplayConfiguration(config: HyperStatPipe2Configuration){
        config.displayHumidity = hsHaystackUtil.readConfigPointValue("enabled and humidity") == 1.0
        config.displayVOC = hsHaystackUtil.readConfigPointValue("enabled and voc") == 1.0
        config.displayPp2p5 = hsHaystackUtil.readConfigPointValue("enabled and pm2p5") == 1.0
        config.displayCo2 = hsHaystackUtil.readConfigPointValue("enabled and co2") == 1.0
    }

    private fun getThermistorInConfigurations(config: HyperStatPipe2Configuration) {
        val th1 = hsHaystackUtil.readConfigStatus("th1 and input").toInt()
        val th2 = hsHaystackUtil.readConfigStatus("th2 and input").toInt()

        val th1AssociatedTo = hsHaystackUtil.readConfigAssociation("th1 and input")
        val th2AssociatedTo = hsHaystackUtil.readConfigAssociation("th2 and input")

        config.thermistorIn1State = Pipe2Th1InState(
            th1 == 1, HyperStatAssociationUtil.getPipe2Th1InStage(th1AssociatedTo.toInt())
        )
        config.thermistorIn2State = Pipe2Th2InState(
            th2 == 1, HyperStatAssociationUtil.getPipe2Th2InStage(th2AssociatedTo.toInt())
        )
    }

    //  To show the existing profile configurations
    // config relays
    private fun getRelayConfigurations(config: HyperStatPipe2Configuration) {
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
            Pipe2RelayState(r1 == 1, HyperStatAssociationUtil.getPipe2RelayAssociatedStage(r1AssociatedTo.toInt()))
        config.relay2State =
            Pipe2RelayState(r2 == 1, HyperStatAssociationUtil.getPipe2RelayAssociatedStage(r2AssociatedTo.toInt()))
        config.relay3State =
            Pipe2RelayState(r3 == 1, HyperStatAssociationUtil.getPipe2RelayAssociatedStage(r3AssociatedTo.toInt()))
        config.relay4State =
            Pipe2RelayState(r4 == 1, HyperStatAssociationUtil.getPipe2RelayAssociatedStage(r4AssociatedTo.toInt()))
        config.relay5State =
            Pipe2RelayState(r5 == 1, HyperStatAssociationUtil.getPipe2RelayAssociatedStage(r5AssociatedTo.toInt()))
        config.relay6State =
            Pipe2RelayState(r6 == 1, HyperStatAssociationUtil.getPipe2RelayAssociatedStage(r6AssociatedTo.toInt()))

    }

    //config Analog Out
    private fun getAnalogOutConfigurations(config: HyperStatPipe2Configuration) {
        val ao1 = hsHaystackUtil.readConfigStatus("analog1 and output").toInt()
        val ao2 = hsHaystackUtil.readConfigStatus("analog2 and output").toInt()
        val ao3 = hsHaystackUtil.readConfigStatus("analog3 and output").toInt()

        val ao1AssociatedTo = hsHaystackUtil.readConfigAssociation("analog1 and output")
        val ao2AssociatedTo = hsHaystackUtil.readConfigAssociation("analog2 and output")
        val ao3AssociatedTo = hsHaystackUtil.readConfigAssociation("analog3 and output")


        var ao1MinVal = 2.0
        var ao1MaxVal = 10.0

        var ao1fanLow = 70.0
        var ao1fanMedium = 80.0
        var ao1fanHigh = 100.0

        if (ao1 == 1) {
            ao1MinVal = hsHaystackUtil.readConfigPointValue(
                "analog1 and output and min"
            )
            ao1MaxVal = hsHaystackUtil.readConfigPointValue(
                "analog1 and output and max"
            )

            if (HyperStatAssociationUtil.getPipe2AnalogOutAssociatedStage(
                    ao1AssociatedTo.toInt()
                ) == Pipe2AnalogOutAssociation.FAN_SPEED
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

        var ao2fanLow = 70.0
        var ao2fanMedium = 80.0
        var ao2fanHigh = 100.0

        if (ao2 == 1) {
            ao2MinVal = hsHaystackUtil.readConfigPointValue(
                "analog2 and output and min"
            )
            ao2MaxVal = hsHaystackUtil.readConfigPointValue(
                "analog2 and output and max"
            )

            if (HyperStatAssociationUtil.getPipe2AnalogOutAssociatedStage(
                    ao2AssociatedTo.toInt()
                ) == Pipe2AnalogOutAssociation.FAN_SPEED
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

        var ao3fanLow = 70.0
        var ao3fanMedium = 80.0
        var ao3fanHigh = 100.0

        if (ao3 == 1) {
            ao3MinVal = hsHaystackUtil.readConfigPointValue(
                "analog3 and output and min"
            )
            ao3MaxVal = hsHaystackUtil.readConfigPointValue(
                "analog3 and output and max"
            )

            if (HyperStatAssociationUtil.getPipe2AnalogOutAssociatedStage(
                    ao3AssociatedTo.toInt()
                ) == Pipe2AnalogOutAssociation.FAN_SPEED
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

        config.analogOut1State = Pipe2AnalogOutState(
            ao1 == 1,
            HyperStatAssociationUtil.getPipe2AnalogOutAssociatedStage(ao1AssociatedTo.toInt()),
            ao1MinVal, ao1MaxVal, ao1fanLow, ao1fanMedium, ao1fanHigh
        )
        config.analogOut2State = Pipe2AnalogOutState(
            ao2 == 1,
            HyperStatAssociationUtil.getPipe2AnalogOutAssociatedStage(ao2AssociatedTo.toInt()),
            ao2MinVal, ao2MaxVal, ao2fanLow, ao2fanMedium, ao2fanHigh
        )
        config.analogOut3State = Pipe2AnalogOutState(
            ao3 == 1,
            HyperStatAssociationUtil.getPipe2AnalogOutAssociatedStage(ao3AssociatedTo.toInt()),
            ao3MinVal, ao3MaxVal, ao3fanLow, ao3fanMedium, ao3fanHigh
        )
    }

    //config Analog In
    private fun getAnalogInConfigurations(config: HyperStatPipe2Configuration) {
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


    /**
     * Function  which collects all the logical associated points
     */
    override fun getLogicalPointList(): HashMap<Any, String> {
        val config = getConfiguration()
        val logicalPoints: HashMap<Any, String> = HashMap()

        if (config.relay1State.enabled) logicalPoints[Port.RELAY_ONE] = hyperStatPointsUtil.getPipe2RelayLogicalPoint(config.relay1State.association).id
        if (config.relay2State.enabled) logicalPoints[Port.RELAY_TWO] = hyperStatPointsUtil.getPipe2RelayLogicalPoint(config.relay2State.association).id
        if (config.relay3State.enabled) logicalPoints[Port.RELAY_THREE] = hyperStatPointsUtil.getPipe2RelayLogicalPoint(config.relay3State.association).id
        if (config.relay4State.enabled) logicalPoints[Port.RELAY_FOUR] = hyperStatPointsUtil.getPipe2RelayLogicalPoint(config.relay4State.association).id
        if (config.relay5State.enabled) logicalPoints[Port.RELAY_FIVE] = hyperStatPointsUtil.getPipe2RelayLogicalPoint(config.relay5State.association).id
        if (config.relay6State.enabled) logicalPoints[Port.RELAY_SIX] = hyperStatPointsUtil.getPipe2RelayLogicalPoint(config.relay6State.association).id

        if (config.analogOut1State.enabled) logicalPoints[Port.ANALOG_OUT_ONE] = hyperStatPointsUtil.getPipe2AnalogOutLogicalPoint(config.analogOut1State.association).id
        if (config.analogOut2State.enabled) logicalPoints[Port.ANALOG_OUT_TWO] = hyperStatPointsUtil.getPipe2AnalogOutLogicalPoint(config.analogOut2State.association).id
        if (config.analogOut3State.enabled) logicalPoints[Port.ANALOG_OUT_THREE] = hyperStatPointsUtil.getPipe2AnalogOutLogicalPoint(config.analogOut3State.association).id
        return logicalPoints
    }


    fun getRelayOutputPoints() : HashMap<Int, String>{
        val relayOutputPoints: HashMap<Int, String> = HashMap()
        putPointToMap(LogicalPointsUtil.readFanLowRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.FAN_LOW_SPEED.ordinal)
        putPointToMap(LogicalPointsUtil.readFanMediumRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.FAN_MEDIUM_SPEED.ordinal)
        putPointToMap(LogicalPointsUtil.readFanHighRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.FAN_HIGH_SPEED.ordinal)
        putPointToMap(LogicalPointsUtil.readHeatingAux1RelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.AUX_HEATING_STAGE1.ordinal)
        putPointToMap(LogicalPointsUtil.readHeatingAux2RelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.AUX_HEATING_STAGE2.ordinal)
        putPointToMap(LogicalPointsUtil.readWaterValveRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.WATER_VALVE.ordinal)
        putPointToMap(LogicalPointsUtil.readFanEnabledRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.FAN_ENABLED.ordinal)
        putPointToMap(LogicalPointsUtil.readOccupiedEnabledRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.OCCUPIED_ENABLED.ordinal)
        putPointToMap(LogicalPointsUtil.readHumidifierRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.HUMIDIFIER.ordinal)
        putPointToMap(LogicalPointsUtil.readDeHumidifierRelayLogicalPoint(equipRef!!),relayOutputPoints,Pipe2RelayAssociation.DEHUMIDIFIER.ordinal)
        return relayOutputPoints
    }
    fun getAnalogOutputPoints() : HashMap<Int, String>{
        val analogOutputPoints: HashMap<Int, String> = HashMap()
        putPointToMap(LogicalPointsUtil.readAnalogOutFanSpeedLogicalPoint(equipRef!!),analogOutputPoints,Pipe2AnalogOutAssociation.FAN_SPEED.ordinal)
        putPointToMap(LogicalPointsUtil.readAnalogOutWaterValveLogicalPoint(equipRef!!),analogOutputPoints,Pipe2AnalogOutAssociation.WATER_VALVE.ordinal)
        putPointToMap(LogicalPointsUtil.readAnalogOutDcvLogicalPoint(equipRef!!),analogOutputPoints,Pipe2AnalogOutAssociation.DCV_DAMPER.ordinal)
        return analogOutputPoints
    }




    // Update configuration
    // Function to update the existing profile
    override fun updateConfiguration(configuration: BaseProfileConfiguration) {
        CcuLog.d(L.TAG_CCU_HSPIPE2, "===========Hyperstat profile Update  ============")

        val updatedHyperStatConfig =  configuration as HyperStatPipe2Configuration
        val presetConfiguration = getConfiguration()

        updateGeneralConfiguration(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)

        // th2 is mapped always mapped to supply water temp and it should be always on no need to update
        // we should not allow the user to change the or turn off the toggle  for th2 configuration so commented bellow code
        // updateSupplyWaterTempTh2Configuration(presetConfiguration.isEnableAirFlowTempSensor,updatedHyperStatConfig.isSupplyWaterSensor)

        updateRelaysConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogOutConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogInConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateThermistorInConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)

        LogicalPointsUtil.cleanPipe2LogicalPoints(updatedHyperStatConfig,equipRef!!)
        CcuLog.d(L.TAG_CCU_HSPIPE2, "Profile update has been completed  ")
        haystack.syncEntityTree()

    }


    // function to update (If change in configuration)
    // Temp Off set , CO2 threshold and target, VOC threshold and target, pm2.5 threshold and target
    private fun updateGeneralConfiguration(
        newConfiguration: HyperStatPipe2Configuration,
        existingConfiguration: HyperStatPipe2Configuration){

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

    // To Update the existing profile configurations
    private fun updateRelaysConfig(
        newConfiguration: HyperStatPipe2Configuration,
        existingConfiguration: HyperStatPipe2Configuration
    ) {
        if (!HyperStatAssociationUtil.isPipe2BothRelayHasSameConfigs
                (newConfiguration.relay1State, existingConfiguration.relay1State)) {
            updateRelayDetails(newConfiguration.relay1State, "relay1", Port.RELAY_ONE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isPipe2BothRelayHasSameConfigs
                (newConfiguration.relay2State, existingConfiguration.relay2State)) {
            updateRelayDetails(newConfiguration.relay2State, "relay2", Port.RELAY_TWO, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isPipe2BothRelayHasSameConfigs
                (newConfiguration.relay3State, existingConfiguration.relay3State)) {
            updateRelayDetails(newConfiguration.relay3State, "relay3", Port.RELAY_THREE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isPipe2BothRelayHasSameConfigs
                (newConfiguration.relay4State, existingConfiguration.relay4State)) {
            updateRelayDetails(newConfiguration.relay4State, "relay4", Port.RELAY_FOUR, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isPipe2BothRelayHasSameConfigs
                (newConfiguration.relay5State, existingConfiguration.relay5State)) {
            updateRelayDetails(newConfiguration.relay5State, "relay5", Port.RELAY_FIVE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isPipe2BothRelayHasSameConfigs
                (newConfiguration.relay6State, existingConfiguration.relay6State)) {
            updateRelayDetails(newConfiguration.relay6State, "relay6", Port.RELAY_SIX, newConfiguration)
        }
    }

    // Function which updates the Relay new configurations
    private fun updateRelayDetails(
        relayState: Pipe2RelayState,
        relayTag: String,
        physicalPort: Port,
        newConfiguration: HyperStatPipe2Configuration,
    ) {

        val relayId = hsHaystackUtil.readPointID("config and $relayTag and enabled") as String
        val relayAssociatedId = hsHaystackUtil.readPointID(
            "config and $relayTag and association"
        ) as String


        val relayLogicalPointId: String? = hyperStatPointsUtil.getPipe2RelayLogicalPoint(relayState.association).id

        hyperStatPointsUtil.addDefaultValueForPoint(relayId, if (relayState.enabled) 1.0 else 0.0)
        hyperStatPointsUtil.addDefaultValueForPoint(relayAssociatedId, relayState.association.ordinal.toDouble())
        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, relayState.enabled)

        if (relayLogicalPointId != null && HyperStatAssociationUtil.isDeletionRequired(newConfiguration,relayState)) {
            hsHaystackUtil.removePoint(relayLogicalPointId)
        }

        if (relayState.enabled) {
            val pointData: Point = hyperStatPointsUtil.relayPipe2Configuration(
                relayState = relayState,
            )
            val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)

            if (pointData.markers.contains("his")) {
                hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
            }

            hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
        }

           if (HyperStatAssociationUtil.isPipe2RelayAssociatedToFan(relayState)){
               updateFanMode(newConfiguration)
           }
    }


    // Function which updates the Analog Out new configurations
    private fun updateAnalogOutDetails(
        analogOutState: Pipe2AnalogOutState,
        analogOutTag: String,
        physicalPort: Port,
        changeIn: AnalogOutChanges,
        newConfiguration: HyperStatPipe2Configuration
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

        CcuLog.d(L.TAG_CCU_HSPIPE2, "Reconfiguration changeIn $changeIn")


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

        CcuLog.d(L.TAG_CCU_HSPIPE2, "Reconfiguration analogOutState.enabled ${analogOutState.enabled}")
        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, analogOutState.enabled)
        if (analogOutState.enabled) {

            val pointData: Triple<Any, Any, Any> = hyperStatPointsUtil.pipe2AnalogOutConfiguration (
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

            if (analogOutState.association == Pipe2AnalogOutAssociation.FAN_SPEED) {

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

        if ((changeIn == AnalogOutChanges.MAPPING ||changeIn == AnalogOutChanges.ENABLED)
            && analogOutState.association == Pipe2AnalogOutAssociation.FAN_SPEED
        ) {
            updateFanMode(newConfiguration)
        }
    }

    private fun updateFanMode(config: HyperStatPipe2Configuration) {
        val fanLevel = HyperStatAssociationUtil.getPipe2SelectedFanLevel(config)
        val curFanSpeed = hsHaystackUtil.readPointValue("zone and sp and fan and operation and mode")
        CcuLog.d(L.TAG_CCU_HSPIPE2, "updateFanMode: fanLevel $fanLevel curFanSpeed $curFanSpeed")
        val fallbackFanSpeed: Double = if(fanLevel == 0) { 0.0 } else {
            if (fanLevel > 0 && curFanSpeed.toInt() != StandaloneFanStage.AUTO.ordinal) {
                StandaloneFanStage.AUTO.ordinal.toDouble()
            } else {
                curFanSpeed
            }
        }
        CcuLog.d(L.TAG_CCU_HSPIPE2, "updateFanMode: fallbackFanSpeed $fallbackFanSpeed")
        hsHaystackUtil.writeDefaultVal("zone and sp and fan and operation and mode", fallbackFanSpeed)
        CCUHsApi.getInstance().writeHisValByQuery(
            "zone and sp and fan and operation and mode and equipRef == \"$equipRef\"",
            fallbackFanSpeed
        )
    }


    private fun updateAnalogOutConfig(
        newConfiguration: HyperStatPipe2Configuration,
        existingConfiguration: HyperStatPipe2Configuration
    ) {
        if (!HyperStatAssociationUtil.isPipe2BothAnalogOutHasSameConfigs
                (newConfiguration.analogOut1State, existingConfiguration.analogOut1State)
        ) {
            val changeIn = HyperStatAssociationUtil.findPipe2ChangeInAnalogOutConfig(
                newConfiguration.analogOut1State, existingConfiguration.analogOut1State
            )
            updateAnalogOutDetails(newConfiguration.analogOut1State, "analog1", Port.ANALOG_OUT_ONE, changeIn, newConfiguration)
        }
        if (!HyperStatAssociationUtil.isPipe2BothAnalogOutHasSameConfigs
                (newConfiguration.analogOut2State, existingConfiguration.analogOut2State)
        ) {
            val changeIn = HyperStatAssociationUtil.findPipe2ChangeInAnalogOutConfig(
                newConfiguration.analogOut2State, existingConfiguration.analogOut2State
            )
            updateAnalogOutDetails(newConfiguration.analogOut2State, "analog2", Port.ANALOG_OUT_TWO, changeIn, newConfiguration)
        }
        if (!HyperStatAssociationUtil.isPipe2BothAnalogOutHasSameConfigs
                (newConfiguration.analogOut3State, existingConfiguration.analogOut3State)
        ) {
            val changeIn = HyperStatAssociationUtil.findPipe2ChangeInAnalogOutConfig(
                newConfiguration.analogOut3State, existingConfiguration.analogOut3State
            )
            updateAnalogOutDetails(newConfiguration.analogOut3State, "analog3", Port.ANALOG_OUT_THREE, changeIn, newConfiguration)
        }
    }


    private fun updateAnalogInConfig(
        newConfiguration: HyperStatPipe2Configuration,
        existingConfiguration: HyperStatPipe2Configuration
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
        CcuLog.d(L.TAG_CCU_HSPIPE2, "updateAnalogInConfig: Done")
    }

    private fun updateThermistorInConfig(
        newConfiguration: HyperStatPipe2Configuration,
        existingConfiguration: HyperStatPipe2Configuration
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

    fun getCurrentTemp(): Double {
        return hsHaystackUtil.getCurrentTemp()
    }


}