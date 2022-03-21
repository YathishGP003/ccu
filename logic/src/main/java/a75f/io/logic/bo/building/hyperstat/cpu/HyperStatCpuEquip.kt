package a75f.io.logic.bo.building.hyperstat.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.heartbeat.HeartBeat
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.*
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil.Companion.getSelectedFanLevel
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.HyperStatDevice
import a75f.io.logic.tuners.HyperstatTuners
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
class HyperStatCpuEquip(val node: Short) {

    val haystack: CCUHsApi = CCUHsApi.getInstance()
    private val profileName = "cpu"
    private val profileTag = "hyperstatcpu"
    private var nodeAddress: Short = node
    private var siteMap = haystack.read(Tags.SITE) as HashMap<Any, Any>
    private var siteRef = siteMap[Tags.ID] as String
    private var masterPoints: HashMap<Any, String> = HashMap()

    private var siteDis: String? = null
    private var tz: String? = null
    private var equipDis: String? = null
    private var gatewayRef: String? = null
    var equipRef: String? = null
    private var roomRef: String? = null
    private var floorRef: String? = null
    private var systemEquip = haystack.read("equip and system") as HashMap<Any, Any>
    private lateinit var hyperStatPointsUtil: HyperStatPointsUtil
    var hsHaystackUtil: HSHaystackUtil? = null


    companion object {
        fun getHyperstatEquipRef(nodeAddress: Short): HyperStatCpuEquip {
            val hyperStatCpuEquip = HyperStatCpuEquip(nodeAddress)
            hyperStatCpuEquip.initEquipReference()
            return hyperStatCpuEquip
        }
    }

    fun initializePoints(config: HyperStatCpuConfiguration, room: String, floor: String, node: Short) {
        Log.i(L.TAG_CCU_HSCPU, "initializePoints: ")
        nodeAddress = node
        floorRef = floor
        roomRef = room
        Log.i(L.TAG_CCU_HSCPU, "New Profile init")
        siteDis = siteMap["dis"] as String
        tz = siteMap["tz"].toString()
        equipDis = "$siteDis-$profileTag-$nodeAddress"

        // collect the Gateway reference
        gatewayRef = systemEquip["id"].toString()

        // Create Equip Point
        equipRef = HyperStatPointsUtil.createHyperStatEquipPoint(
            profileName,
            siteRef = siteRef,
            roomRef = roomRef!!,
            floorRef = floorRef!!,
            priority = config.priority.toString(),
            gatewayRef = gatewayRef!!,
            tz = tz!!,
            nodeAddress = nodeAddress.toString(),
            equipDis = equipDis!!, haystack
        )


        // Init required reference
        init()

        Log.i(L.TAG_CCU_HSCPU, "New Profile Hyperstat CPU configuration ")


        // add Tuner Points
        RxTask.executeAsync {
            HyperstatTuners.addHyperstatModuleTuners(
                haystack, siteRef, equipRef!!, "$siteDis-hyperstatcpu-$nodeAddress", tz!!, roomRef!!, floorRef!!
            )
        }
        Log.i(L.TAG_CCU_HSCPU, "New Profile Hyperstat Tuners added ")
        // Create profile points
        hyperStatPointsUtil.addProfilePoints()

        // Create config points
        createProfileConfigurationPoints(hyperStatConfig = config)

        Log.i(L.TAG_CCU_HSCPU, "New Profile Profile configuration points are created ")
        // Create Logical Points
        createProfileLogicalPoints(hyperStatConfig = config)

        Log.i(L.TAG_CCU_HSCPU, "New Profile  logical points are created")

        val hyperStatDevice = HyperStatDevice(
            nodeAddress.toInt(), siteRef, floorRef, roomRef, equipRef, profileName
        )
        configHyperStatDevice(config, hyperStatDevice)

        Log.i(L.TAG_CCU_HSCPU, "Created new physical device")
        // Syncing the Points
        haystack.syncEntityTree()
        Log.i(L.TAG_CCU_HSCPU, "Profile Configuration is done")
        return
    }

    //init
    private fun init() {
        hyperStatPointsUtil = HyperStatPointsUtil(
            profileName = profileName,
            equipRef = equipRef!!,
            floorRef = floorRef!!,
            roomRef = roomRef!!,
            siteRef = siteRef,
            tz = tz!!,
            nodeAddress = nodeAddress.toString(),
            equipDis = equipDis!!,
            hayStackAPI = haystack
        )

        hsHaystackUtil = HSHaystackUtil(
            profileName, equipRef!!, haystack
        )
        Log.i(L.TAG_CCU_HSCPU, "Initialised the complete equip reference")
    }

    fun initEquipReference() {
        val equip = haystack.read("equip and hyperstat and group == \"$nodeAddress\"")
        if (equip.isEmpty()) {
            Log.i(L.TAG_CCU_HSCPU, " Unable to find the equip details for node $nodeAddress ")
            return
        }
        equipRef = equip["id"].toString()
        roomRef = equip["roomRef"].toString()
        floorRef = equip["floorRef"].toString()
        siteDis = siteMap["dis"] as String
        tz = siteMap["tz"].toString()
        equipDis = "$siteDis-$profileTag-$nodeAddress"
        init()
    }

    // Function which configure HyperStat Device configurations
    private fun configHyperStatDevice(config: HyperStatCpuConfiguration, device: HyperStatDevice) {
        Log.i(L.TAG_CCU_HSCPU, "configHyperStatDevice: ")
        val heartBeatId = CCUHsApi.getInstance().addPoint(
            nodeAddress.let {
                HeartBeat.getHeartBeatPoint(
                    equipDis, equipRef,
                    siteRef, roomRef, floorRef, it.toInt(), profileName, tz
                )
            }
        )
        setupDeviceRelays(config = config, device = device)
        setupDeviceAnalogOuts(config = config, device = device)
        setupDeviceAnalogIns(config = config, device = device)
        setupDeviceThermistors(config = config, device = device)


        device.currentTemp.pointRef = masterPoints[LogicalKeyID.CURRENT_TEMP]
        device.currentTemp.enabled = true

        device.addSensor(Port.SENSOR_RH, masterPoints[Port.SENSOR_RH])
        device.addSensor(Port.SENSOR_ILLUMINANCE, masterPoints[Port.SENSOR_ILLUMINANCE])
        device.addSensor(Port.SENSOR_OCCUPANCY, masterPoints[Port.SENSOR_OCCUPANCY])

        device.rssi.pointRef = heartBeatId
        device.rssi.enabled = true
        device.addPointsToDb()
    }

    private fun setupDeviceThermistors(config: HyperStatCpuConfiguration, device: HyperStatDevice) {

        if (config.isEnableAirFlowTempSensor) {
            device.th1In.pointRef = masterPoints[Port.TH1_IN]
            device.th1In.enabled = true
            // it is 3 constant because in sensor manager class Airflow Sensor position in external sensor list is 3
            // do not change it
            device.th1In.type = "3"
        }
        if (config.isEnableDoorWindowSensor) {
            device.th2In.pointRef = masterPoints[Port.TH2_IN]
            device.th2In.enabled = true
            device.th2In.type = HyperStatAssociationUtil.getSensorNameByType(CpuAnalogInAssociation.DOOR_WINDOW_SENSOR)
            // constant do not change
        }
    }

    private fun setupDeviceAnalogIns(config: HyperStatCpuConfiguration, device: HyperStatDevice) {
        if (config.analogIn1State.enabled) {
            device.analog1In.pointRef = masterPoints[Port.ANALOG_IN_ONE]
            device.analog1In.enabled = true
            device.analog1In.type = HyperStatAssociationUtil.getSensorNameByType(config.analogIn1State.association)
        }
        if (config.analogIn2State.enabled) {
            device.analog2In.pointRef = masterPoints[Port.ANALOG_IN_TWO]
            device.analog2In.enabled = true
            device.analog2In.type = HyperStatAssociationUtil.getSensorNameByType(config.analogIn1State.association)
        }

    }

    // setup Device AnalogOuts
    private fun setupDeviceAnalogOuts(config: HyperStatCpuConfiguration, device: HyperStatDevice) {
        if (config.analogOut1State.enabled) {
            device.analog1Out.pointRef = masterPoints[Port.ANALOG_OUT_ONE]
            device.analog1Out.enabled = true
            device.analog1Out.type = "${config.analogOut1State.voltageAtMin.toInt()}-${
                config.analogOut1State
                    .voltageAtMax.toInt()
            }v"
        }
        if (config.analogOut2State.enabled) {
            device.analog2Out.pointRef = masterPoints[Port.ANALOG_OUT_TWO]
            device.analog2Out.enabled = true
            device.analog2Out.type = "${config.analogOut2State.voltageAtMin.toInt()}-${
                config.analogOut2State
                    .voltageAtMax.toInt()
            }v"
        }
        if (config.analogOut3State.enabled) {
            device.analog3Out.pointRef = masterPoints[Port.ANALOG_OUT_THREE]
            device.analog3Out.enabled = true
            device.analog3Out.type = "${config.analogOut3State.voltageAtMin.toInt()}-${
                config.analogOut3State
                    .voltageAtMax.toInt()
            }v"
        }
    }

    // setup Device Relays
    private fun setupDeviceRelays(config: HyperStatCpuConfiguration, device: HyperStatDevice) {

        if (config.relay1State.enabled) {
            device.relay1.pointRef = masterPoints[Port.RELAY_ONE]
            device.relay1.enabled = true
        }
        if (config.relay2State.enabled) {
            device.relay2.pointRef = masterPoints[Port.RELAY_TWO]
            device.relay2.enabled = true
        }
        if (config.relay3State.enabled) {
            device.relay3.pointRef = masterPoints[Port.RELAY_THREE]
            device.relay3.enabled = true
        }
        if (config.relay4State.enabled) {
            device.relay4.pointRef = masterPoints[Port.RELAY_FOUR]
            device.relay4.enabled = true
        }
        if (config.relay5State.enabled) {
            device.relay5.pointRef = masterPoints[Port.RELAY_FIVE]
            device.relay5.enabled = true
        }
        if (config.relay6State.enabled) {
            device.relay6.pointRef = masterPoints[Port.RELAY_SIX]
            device.relay6.enabled = true
        }

    }


    //function which creates Profile configuration Points
    private fun createProfileConfigurationPoints(hyperStatConfig: HyperStatCpuConfiguration) {

        hyperStatPointsUtil.createTemperatureOffSetPoint(hyperStatConfig.temperatureOffset * 10)

        // List Of Configuration Points
        val configPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createAutoForceAutoAwayConfigPoints(
            hyperStatConfig = hyperStatConfig
        )

        val co2ConfigPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil
            .createPointCO2ConfigPoint(
                hyperStatConfig = hyperStatConfig
            )
        val loopOutputPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createLoopOutputPoints()

        val relayConfigPoints: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createIsRelayEnabledConfigPoints(
            hyperStatConfig = hyperStatConfig
        )
        val analogOutConfigPoints: MutableList<Pair<Point, Any>> =
            hyperStatPointsUtil.createIsAnalogOutEnabledConfigPoints(
                hyperStatConfig = hyperStatConfig
            )
        val analogInConfigPoints: MutableList<Pair<Point, Any>> =
            hyperStatPointsUtil.createIsAnalogInEnabledConfigPoints(
                hyperStatConfig = hyperStatConfig
            )
        val thConfigPointsList: MutableList<Pair<Point, Any>> =
            hyperStatPointsUtil.createIsThermistorEnabledConfigPoints(
                hyperStatConfig = hyperStatConfig
            )
        // For user intent Point CCU level default values need to be added
        val userIntentPointsList: MutableList<Pair<Point, Any>> = hyperStatPointsUtil.createUserIntentPoints(
            hyperStatConfig = hyperStatConfig
        )

        val allConfigPoints = arrayOf(
            configPointsList, relayConfigPoints, analogOutConfigPoints,
            analogInConfigPoints, thConfigPointsList, userIntentPointsList, co2ConfigPointsList, loopOutputPoints
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
                hyperStatConfig = hyperStatConfig
            )
        val thermistorInPointsList: MutableList<Triple<Point, Any, Any>> =
            hyperStatPointsUtil.createConfigThermisterInLogicalPoints(
                hyperStatConfig = hyperStatConfig
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
    fun updateConfigPoints(updatedHyperStatConfig: HyperStatCpuConfiguration) {

        val presetConfiguration = getConfiguration()

        if (updatedHyperStatConfig.temperatureOffset != presetConfiguration.temperatureOffset) {
            val tempOffId = hsHaystackUtil!!.readPointID("temperature and offset") as String
            hyperStatPointsUtil.addDefaultValueForPoint(tempOffId, (updatedHyperStatConfig.temperatureOffset * 10))
        }
        if (updatedHyperStatConfig.zoneCO2DamperOpeningRate != presetConfiguration.zoneCO2DamperOpeningRate) {
            val pointId = hsHaystackUtil!!.readPointID("co2 and opening and rate") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, updatedHyperStatConfig.zoneCO2DamperOpeningRate)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, updatedHyperStatConfig.zoneCO2DamperOpeningRate)
        }
        if (updatedHyperStatConfig.zoneCO2Threshold != presetConfiguration.zoneCO2Threshold) {
            val pointId = hsHaystackUtil!!.readPointID("co2 and threshold") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, updatedHyperStatConfig.zoneCO2Threshold)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, updatedHyperStatConfig.zoneCO2Threshold)
        }
        if (updatedHyperStatConfig.zoneCO2Target != presetConfiguration.zoneCO2Target) {
            val pointId = hsHaystackUtil!!.readPointID("co2 and target") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, updatedHyperStatConfig.zoneCO2Target)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, updatedHyperStatConfig.zoneCO2Target)
        }


        if (updatedHyperStatConfig.isEnableAutoForceOccupied != presetConfiguration.isEnableAutoForceOccupied) {
            val autoForceOccupiedEnabledId = hsHaystackUtil!!.readPointID(
                "config and auto and occupancy and forced and control"
            ) as String
            hyperStatPointsUtil.addDefaultValueForPoint(
                autoForceOccupiedEnabledId, if (updatedHyperStatConfig.isEnableAutoForceOccupied) 1.0 else 0.0
            )
            hyperStatPointsUtil.addDefaultHisValueForPoint(
                autoForceOccupiedEnabledId,if (updatedHyperStatConfig.isEnableAutoForceOccupied) 1.0 else 0.0
            )
        }

        if (updatedHyperStatConfig.isEnableAutoAway != presetConfiguration.isEnableAutoAway) {
            val autoAwayEnabledId = hsHaystackUtil!!.readPointID(
                "config and  auto and away"
            ) as String
            hyperStatPointsUtil.addDefaultValueForPoint(
                autoAwayEnabledId, if (updatedHyperStatConfig.isEnableAutoAway) 1.0 else 0.0
            )
            hyperStatPointsUtil.addDefaultHisValueForPoint(
                autoAwayEnabledId,if (updatedHyperStatConfig.isEnableAutoAway) 1.0 else 0.0
            )
            hsHaystackUtil!!.reWriteOccupancy(equipRef!!)
        }
        if (updatedHyperStatConfig.isEnableAirFlowTempSensor != presetConfiguration.isEnableAirFlowTempSensor) {
            val enabledId = hsHaystackUtil!!.readPointID(
                "config and airflow and temp and th1"
            ) as String
            hyperStatPointsUtil.addDefaultValueForPoint(
                enabledId, if (updatedHyperStatConfig.isEnableAirFlowTempSensor) 1.0 else 0.0
            )
            val logicalPointId: String? = hsHaystackUtil!!.readPointID(
                "th1 and airflow and discharge and logical"
            )
            if (logicalPointId != null) {
                hsHaystackUtil!!.removePoint(logicalPointId)
            }
            if (updatedHyperStatConfig.isEnableAirFlowTempSensor) {
                val pointData: Point = hyperStatPointsUtil.createPointForAirflowTempSensor()
                val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)
                if (pointData.markers.contains("his")) {
                    hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
                }
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                DeviceUtil.setPointEnabled(nodeAddress.toInt(), Port.TH1_IN.name, true)
                DeviceUtil.updatePhysicalPointRef(nodeAddress.toInt(), Port.TH1_IN.name, pointId)
            }

        }
        if (updatedHyperStatConfig.isEnableDoorWindowSensor != presetConfiguration.isEnableDoorWindowSensor) {
            val enabledId = hsHaystackUtil!!.readPointID(
                "config and window and sensor and th2"
            ) as String
            hyperStatPointsUtil.addDefaultValueForPoint(
                enabledId, if (updatedHyperStatConfig.isEnableDoorWindowSensor) 1.0 else 0.0
            )
            val logicalPointId: String? = hsHaystackUtil!!.readPointID(
                "th2 and window and sensor and logical"
            )
            if (logicalPointId != null) {
                hsHaystackUtil!!.removePoint(logicalPointId)
            }
            if (updatedHyperStatConfig.isEnableDoorWindowSensor) {
                val pointData: Point = hyperStatPointsUtil.createPointForDoorWindowSensor("th2")
                val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)
                if (pointData.markers.contains("his")) {
                    hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
                }
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                DeviceUtil.setPointEnabled(nodeAddress.toInt(), Port.TH2_IN.name, true)
                DeviceUtil.updatePhysicalPointRef(nodeAddress.toInt(), Port.TH2_IN.name, pointId)
                DeviceUtil.updatePhysicalPointType(
                    nodeAddress.toInt(), Port.TH2_IN.name,
                    HyperStatAssociationUtil.getSensorNameByType(CpuAnalogInAssociation.DOOR_WINDOW_SENSOR)
                )
            }
        }
        Log.i(L.TAG_CCU_HSCPU, "===========Hyperstat profile Update  ============")
        updateRelaysConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogOutConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        updateAnalogInConfig(newConfiguration = updatedHyperStatConfig, existingConfiguration = presetConfiguration)
        Log.i(L.TAG_CCU_HSCPU, "Profile update has been completed  ")
        haystack.syncEntityTree()

    }


    // collect the existing profile configurations
    fun getConfiguration(): HyperStatCpuConfiguration {
        val config = HyperStatCpuConfiguration()
        val tempOff = haystack.readDefaultVal("point and temperature and offset and equipRef == \"$equipRef\"")

        getRelayConfigurations(config)
        getAnalogOutConfigurations(config)
        getAnalogInConfigurations(config)


        config.temperatureOffset = (tempOff / 10)
        config.isEnableAutoForceOccupied = (
                equipRef?.let {
                    hsHaystackUtil!!.readConfigStatus(it, "auto and occupancy and forced and control")
                        .toInt()
                } == 1)
        config.isEnableAutoAway = (equipRef?.let {
            hsHaystackUtil!!.readConfigStatus(it, "auto and away")
                .toInt()
        } == 1)

        config.isEnableAirFlowTempSensor = (
                equipRef?.let {
                    hsHaystackUtil!!.readConfigStatus(it, "airflow and temp and sensor").toInt()
                } == 1)
        config.isEnableDoorWindowSensor = (
                equipRef?.let {
                    hsHaystackUtil!!.readConfigStatus(it, "window and sensor").toInt()
                } == 1)

        config.zoneCO2DamperOpeningRate = haystack.readDefaultVal(
            "point and hyperstat and co2 and opening and rate and equipRef == \"$equipRef\""
        )

        config.zoneCO2Threshold = haystack.readDefaultVal(
            "point and hyperstat and co2 and threshold and equipRef == \"$equipRef\""
        )

        config.zoneCO2Target = haystack.readDefaultVal(
            "point and hyperstat and co2 and target and equipRef == \"$equipRef\""
        )

        return config
    }

    //  To show the existing profile configurations
    // config relays
    private fun getRelayConfigurations(config: HyperStatCpuConfiguration) {
        val r1 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "relay1").toInt() }
        val r2 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "relay2").toInt() }
        val r3 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "relay3").toInt() }
        val r4 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "relay4").toInt() }
        val r5 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "relay5").toInt() }
        val r6 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "relay6").toInt() }

        val r1AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "relay1")
        val r2AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "relay2")
        val r3AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "relay3")
        val r4AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "relay4")
        val r5AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "relay5")
        val r6AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "relay6")

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
        val ao1 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "analog1 and out").toInt() }
        val ao2 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "analog2 and out").toInt() }
        val ao3 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "analog3 and out").toInt() }

        val ao1AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "analog1 and out")
        val ao2AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "analog2 and out")
        val ao3AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "analog3 and out")


        var ao1MinVal = 0.2
        var ao1MaxVal = 10.0

        var ao1fanLow = 30.0
        var ao1fanMedium = 60.0
        var ao1fanHigh = 100.0

        if (ao1 == 1) {
            ao1MinVal = hsHaystackUtil!!.readConfigPointValue(
                "analog1 and out and min"
            )
            ao1MaxVal = hsHaystackUtil!!.readConfigPointValue(
                "analog1 and out and max"
            )

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao1AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.FAN_SPEED
            ) {
                ao1fanLow = hsHaystackUtil!!.readConfigPointValue(
                    "analog1 and out and low"
                )
                ao1fanMedium = hsHaystackUtil!!.readConfigPointValue(
                    "analog1 and out and medium"
                )
                ao1fanHigh = hsHaystackUtil!!.readConfigPointValue(
                    "analog1 and out and high"
                )
            }
        }
        // Default Values
        var ao2MinVal = 0.2
        var ao2MaxVal = 10.0

        var ao2fanLow = 30.0
        var ao2fanMedium = 60.0
        var ao2fanHigh = 100.0

        if (ao2 == 1) {
            ao2MinVal = hsHaystackUtil!!.readConfigPointValue(
                "analog2 and out and min"
            )
            ao2MaxVal = hsHaystackUtil!!.readConfigPointValue(
                "analog2 and out and max"
            )

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao2AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.FAN_SPEED
            ) {
                ao2fanLow = hsHaystackUtil!!.readConfigPointValue(
                    "analog2 and out and low"
                )
                ao2fanMedium = hsHaystackUtil!!.readConfigPointValue(
                    "analog2 and out and medium"
                )
                ao2fanHigh = hsHaystackUtil!!.readConfigPointValue(
                    "analog2 and out and high"
                )
            }
        }

        var ao3MinVal = 0.2
        var ao3MaxVal = 10.0

        var ao3fanLow = 30.0
        var ao3fanMedium = 60.0
        var ao3fanHigh = 100.0

        if (ao3 == 1) {
            ao3MinVal = hsHaystackUtil!!.readConfigPointValue(
                "analog3 and out and min"
            )
            ao3MaxVal = hsHaystackUtil!!.readConfigPointValue(
                "analog3 and out and max"
            )

            if (HyperStatAssociationUtil.getAnalogOutAssociatedStage(
                    ao3AssociatedTo.toInt()
                ) == CpuAnalogOutAssociation.FAN_SPEED
            ) {
                ao3fanLow = hsHaystackUtil!!.readConfigPointValue(
                    "analog3 and out and low"
                )
                ao3fanMedium = hsHaystackUtil!!.readConfigPointValue(
                    "analog3 and out and medium"
                )
                ao3fanHigh = hsHaystackUtil!!.readConfigPointValue(
                    "analog3 and out and high"
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
        val ai1 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "analog1 and in").toInt() }
        val ai2 = equipRef?.let { hsHaystackUtil!!.readConfigStatus(it, "analog2 and in").toInt() }

        val ai1AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "analog1 and in")
        val ai2AssociatedTo = hsHaystackUtil!!.readConfigAssociation(equipRef as String, "analog2 and in")

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
                (newConfiguration.relay1State, existingConfiguration.relay1State)
        ) {
            updateRelayDetails(newConfiguration.relay1State, "relay1", Port.RELAY_ONE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay2State, existingConfiguration.relay2State)
        ) {
            updateRelayDetails(newConfiguration.relay2State, "relay2", Port.RELAY_TWO, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay3State, existingConfiguration.relay3State)
        ) {
            updateRelayDetails(newConfiguration.relay3State, "relay3", Port.RELAY_THREE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay4State, existingConfiguration.relay4State)
        ) {
            updateRelayDetails(newConfiguration.relay4State, "relay4", Port.RELAY_FOUR, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay5State, existingConfiguration.relay5State)
        ) {
            updateRelayDetails(newConfiguration.relay5State, "relay5", Port.RELAY_FIVE, newConfiguration)
        }

        if (!HyperStatAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay6State, existingConfiguration.relay6State)
        ) {
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
     fun updateRelayDetails(
        relayState: RelayState,
        relayTag: String,
        physicalPort: Port,
        newConfiguration: HyperStatCpuConfiguration?,
    ) {

        val relayId = hsHaystackUtil!!.readPointID("config and $relayTag and enabled") as String
        val relayAssociatedId = hsHaystackUtil!!.readPointID(
            "config and $relayTag and association"
        ) as String
        val relayLogicalPointId: String? = hsHaystackUtil!!.readPointID(
            "$relayTag and logical"
        )
        hyperStatPointsUtil.addDefaultValueForPoint(relayId, if (relayState.enabled) 1.0 else 0.0)
        hyperStatPointsUtil.addDefaultValueForPoint(relayAssociatedId, relayState.association.ordinal.toDouble())
        DeviceUtil.setPointEnabled(nodeAddress.toInt(), physicalPort.name, relayState.enabled)
        if (relayLogicalPointId != null) {
            hsHaystackUtil!!.removePoint(relayLogicalPointId)
        }

        if (relayState.enabled) {
            val pointData: Point = hyperStatPointsUtil.relayConfiguration(
                relayState = relayState,
                relayTag = relayTag
            )
            val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)

            if (pointData.markers.contains("his")) {
                hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
            }

            hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            DeviceUtil.updatePhysicalPointRef(nodeAddress.toInt(), physicalPort.name, pointId)
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
     fun updateAnalogOutDetails(
        analogOutState: AnalogOutState,
        analogOutTag: String,
        physicalPort: Port,
        changeIn: AnalogOutChanges,
        newConfiguration: HyperStatCpuConfiguration?
    ) {

        // Read enable/disable Point ID
        val analogOutId = hsHaystackUtil!!.readPointID(
            "config and $analogOutTag and out and enabled"
        ) as String

        // associated  Point ID
        val analogOutAssociatedId = hsHaystackUtil!!.readPointID(
            "config and $analogOutTag and out and association"
        ) as String

        // Read logical  Point ID
        val analogOutLogicalPointId: String? = hsHaystackUtil!!.readPointID(
            "$analogOutTag and logical and out"
        )

        // Read Analog Out min max fan config Point Id's
        val minPointId = hsHaystackUtil!!.readPointID("$analogOutTag and out and min")
        val maxPointId = hsHaystackUtil!!.readPointID("$analogOutTag and out and max")
        // Read Fan config Points
        val fanLowPointId = hsHaystackUtil!!.readPointID("$analogOutTag and out and low")
        val fanMediumPointId = hsHaystackUtil!!.readPointID("$analogOutTag and out and medium")
        val fanHighPointId = hsHaystackUtil!!.readPointID("$analogOutTag and out and high")

        Log.i(L.TAG_CCU_HSCPU, "Reconfiguration changeIn $changeIn")


        // Do the update when change is found in configuration
        if(changeIn != AnalogOutChanges.NOCHANGE
            && changeIn != AnalogOutChanges.ENABLED
            && changeIn != AnalogOutChanges.MAPPING) {
            if (fanHighPointId != null)
                updatePointValueIfchangeRequired(fanHighPointId, analogOutState.perAtFanHigh)
            if (fanMediumPointId != null)
                updatePointValueIfchangeRequired(fanMediumPointId, analogOutState.perAtFanMedium)
            if (fanLowPointId != null)
                updatePointValueIfchangeRequired(fanLowPointId, analogOutState.perAtFanLow)

            if (minPointId != null) {
                updatePointValueIfchangeRequired(minPointId, analogOutState.voltageAtMin)
                val pointType = "${analogOutState.voltageAtMin.toInt()}-${analogOutState.voltageAtMax.toInt()}v"
                DeviceUtil.updatePhysicalPointType(nodeAddress.toInt(), physicalPort.name, pointType)
            }
            if (maxPointId != null) {
                updatePointValueIfchangeRequired(maxPointId, analogOutState.voltageAtMax)
                val pointType = "${analogOutState.voltageAtMin.toInt()}-${analogOutState.voltageAtMax.toInt()}v"
                DeviceUtil.updatePhysicalPointType(nodeAddress.toInt(), physicalPort.name, pointType)
            }
            return
        }

        // Update the Enable , association status with configurations
        hyperStatPointsUtil.addDefaultValueForPoint(analogOutId, if (analogOutState.enabled) 1.0 else 0.0)
        hyperStatPointsUtil.addDefaultValueForPoint(analogOutAssociatedId, analogOutState.association.ordinal.toDouble())

        // Check if logical logical,min,max, fan config  Point exist
        // (This situation is when previous state was not enabled for this analog now it is enabled )
        // SO there's a possibility that logical,min,max, fan config  id can be null

        if (analogOutLogicalPointId != null) hsHaystackUtil!!.removePoint(analogOutLogicalPointId)

        if (minPointId != null) hsHaystackUtil!!.removePoint(minPointId)
        if (maxPointId != null) hsHaystackUtil!!.removePoint(maxPointId)

        if (fanLowPointId != null) hsHaystackUtil!!.removePoint(fanLowPointId)
        if (fanMediumPointId != null) hsHaystackUtil!!.removePoint(fanMediumPointId)
        if (fanHighPointId != null) hsHaystackUtil!!.removePoint(fanHighPointId)

        Log.i(L.TAG_CCU_HSCPU, "Reconfiguration analogOutState.enabled ${analogOutState.enabled}")
        DeviceUtil.setPointEnabled(nodeAddress.toInt(), physicalPort.name, analogOutState.enabled)
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
            DeviceUtil.updatePhysicalPointRef(nodeAddress.toInt(), physicalPort.name, pointId)
            DeviceUtil.updatePhysicalPointType(nodeAddress.toInt(), physicalPort.name, pointType)

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
        CcuLog.i(L.TAG_CCU_ZONE, "changeIn: Anaalog $changeIn")
        CcuLog.i(L.TAG_CCU_ZONE, "changeIn: ${HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)}")
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
    private fun updatePointValueIfchangeRequired(pointId: String, newValue: Double){
        val presentValue = haystack.readDefaultValById(pointId)
        if(presentValue != newValue)
            hyperStatPointsUtil.addDefaultValueForPoint(pointId,newValue)
    }

    // Function which updates the Analog In new configurations
    fun updateAnalogInDetails(
        analogInState: AnalogInState,
        analogInTag: String,
        physicalPort: Port
    ) {
        val analogInId = hsHaystackUtil!!.readPointID("config and $analogInTag and in and enabled") as String
        val analogInAssociatedId =
            hsHaystackUtil!!.readPointID("config and $analogInTag and in and association") as
                    String
        val analogInLogicalPointId: String? = hsHaystackUtil!!.readPointID(
            "$analogInTag and in and logical"
        )
        hyperStatPointsUtil.addDefaultValueForPoint(analogInId, if (analogInState.enabled) 1.0 else 0.0)
        hyperStatPointsUtil.addDefaultValueForPoint(analogInAssociatedId, analogInState.association.ordinal.toDouble())
        if (analogInLogicalPointId != null) {
            hsHaystackUtil!!.removePoint(analogInLogicalPointId)
        }
        DeviceUtil.setPointEnabled(nodeAddress.toInt(), physicalPort.name, analogInState.enabled)
        if (analogInState.enabled) {
            val pointData: Point = hyperStatPointsUtil.analogInConfiguration(
                analogInState = analogInState,
                analogTag = analogInTag
            )
            val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)

            DeviceUtil.updatePhysicalPointRef(nodeAddress.toInt(), physicalPort.name, pointId)
            val pointType = HyperStatAssociationUtil.getSensorNameByType(analogInState.association)
            DeviceUtil.updatePhysicalPointType(nodeAddress.toInt(), physicalPort.name, pointType)
        }

    }


     fun updateConditioningMode() {
         CcuLog.i(L.TAG_CCU_ZONE, "updateConditioningMode: ")
        val conditioningModeId = hsHaystackUtil!!.readPointID("zone and userIntent and conditioning and mode")

        if (conditioningModeId!!.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "ConditioningMode point does not exist ")
            return
        }

        val curCondMode = haystack.readDefaultValById(conditioningModeId)
        var conditioningMode = curCondMode
        Log.i(L.TAG_CCU_HSCPU, "updateConditioningMode: curCondMode $curCondMode")

         when(HSHaystackUtil.getPossibleConditioningModeSettings(nodeAddress.toInt())){
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
        val curFanSpeed = hsHaystackUtil!!.readPointValue("zone and userIntent and fan and mode")
        var fallbackFanSpeed = StandaloneFanStage.OFF.ordinal.toDouble() // Indicating always OFF
         Log.i(L.TAG_CCU_HSCPU, "updateFanMode: fanLevel $fanLevel curFanSpeed $curFanSpeed")
        if (fanLevel > 0 && curFanSpeed.toInt() != StandaloneFanStage.AUTO.ordinal) {
            fallbackFanSpeed = StandaloneFanStage.AUTO.ordinal.toDouble()
        }
         Log.i(L.TAG_CCU_HSCPU, "updateFanMode: fallbackFanSpeed $fallbackFanSpeed")
         hsHaystackUtil!!.writeDefaultVal("zone and userIntent and fan and mode", fallbackFanSpeed)
    }

    fun getCurrentTemp(): Double {
        return hsHaystackUtil!!.getCurrentTemp()
    }

    /**
     * Function  which collects all the logical associated points
     */
    fun getLogicalPointList(): HashMap<Any, String> {
        val logicalPoints: HashMap<Any, String> = HashMap()

        val relay1LogicalPointId: String? = hsHaystackUtil!!.readPointID("relay1 and logical")
        val relay2LogicalPointId: String? = hsHaystackUtil!!.readPointID("relay2 and logical")
        val relay3LogicalPointId: String? = hsHaystackUtil!!.readPointID("relay3 and logical")
        val relay4LogicalPointId: String? = hsHaystackUtil!!.readPointID("relay4 and logical")
        val relay5LogicalPointId: String? = hsHaystackUtil!!.readPointID("relay5 and logical")
        val relay6LogicalPointId: String? = hsHaystackUtil!!.readPointID("relay6 and logical")

        val ao1LogicalPointId: String? = hsHaystackUtil!!.readPointID("analog1 and out and logical")
        val ao2LogicalPointId: String? = hsHaystackUtil!!.readPointID("analog2 and out and logical")
        val ao3LogicalPointId: String? = hsHaystackUtil!!.readPointID("analog3 and out and logical")


        if (relay1LogicalPointId != null) logicalPoints[Port.RELAY_ONE] = relay1LogicalPointId
        if (relay2LogicalPointId != null) logicalPoints[Port.RELAY_TWO] = relay2LogicalPointId
        if (relay3LogicalPointId != null) logicalPoints[Port.RELAY_THREE] = relay3LogicalPointId
        if (relay4LogicalPointId != null) logicalPoints[Port.RELAY_FOUR] = relay4LogicalPointId
        if (relay5LogicalPointId != null) logicalPoints[Port.RELAY_FIVE] = relay5LogicalPointId
        if (relay6LogicalPointId != null) logicalPoints[Port.RELAY_SIX] = relay6LogicalPointId

        if (ao1LogicalPointId != null) logicalPoints[Port.ANALOG_OUT_ONE] = ao1LogicalPointId
        if (ao2LogicalPointId != null) logicalPoints[Port.ANALOG_OUT_TWO] = ao2LogicalPointId
        if (ao3LogicalPointId != null) logicalPoints[Port.ANALOG_OUT_THREE] = ao3LogicalPointId

        Log.i(L.TAG_CCU_HSCPU, "======Logical Points list : ====")
        logicalPoints.forEach { (key, id) ->
            Log.i(L.TAG_CCU_HSCPU, "key : $key  : $id")
        }
        Log.i(L.TAG_CCU_HSCPU, "================================")
        return logicalPoints
    }

}
