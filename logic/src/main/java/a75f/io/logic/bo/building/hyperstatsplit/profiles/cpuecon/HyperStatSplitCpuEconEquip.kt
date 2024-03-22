package a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Tags.CPUECON
import a75f.io.api.haystack.Tags.HYPERSTATSPLIT
import a75f.io.api.haystack.util.hayStack
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.heartbeat.HeartBeat
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.common.AnalogOutChanges
import a75f.io.logic.bo.building.hyperstatsplit.common.ConfigState
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getHumidityPort
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getPressPort
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getSelectedFanLevel
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getTempPort
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperStatSplitEquip
import a75f.io.logic.bo.building.hyperstatsplit.common.HyperstatSplitProfileNames
import a75f.io.logic.bo.building.hyperstatsplit.common.LogicalKeyID
import a75f.io.logic.bo.building.hyperstatsplit.common.LogicalPointsUtil
import a75f.io.logic.bo.building.hyperstatsplit.common.PossibleConditioningMode
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.HyperStatSplitDevice
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint.Companion.addOTAStatusPoint
import a75f.io.logic.tuners.HyperstatSplitCpuEconTuners
import a75f.io.logic.tuners.OAOTuners
import a75f.io.logic.util.RxTask
import a75f.io.logic.util.TIMER_TO_BE_VALID
import android.util.Log
import java.util.Date

/**
 * Models CPU/Economiser Equipment in its interface, calls through to datastore (haystack)
 * to get its state.
 * We will have one of these for each HyperStat in the zone profile.
 *
 * @author tcase@75f.io (HyperStat CPU)
 * Created on 7/9/21.
 *
 * Created for HyperStat Split CPU/Economiser by Nick P on 07-24-2023.
 */
class HyperStatSplitCpuEconEquip(val node: Short): HyperStatSplitEquip() {

    val haystack: CCUHsApi = CCUHsApi.getInstance()
    private val profileTag = CPUECON

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
    private var systemEquip = haystack.readEntity("equip and system") as HashMap<Any, Any>

    companion object {
        fun getHyperStatSplitEquipRef(nodeAddress: Short): HyperStatSplitCpuEconEquip {
            val hyperStatSplitCpuEconEquip = HyperStatSplitCpuEconEquip(nodeAddress)
            hyperStatSplitCpuEconEquip.initEquipReference(nodeAddress)
            return hyperStatSplitCpuEconEquip
        }
    }

    override fun initializePoints(
        baseConfig: BaseProfileConfiguration,
        room: String,
        floor: String,
        node: Short
    ) {
        val config = baseConfig as HyperStatSplitCpuEconConfiguration

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "New Profile  Initialising Points: ")
        nodeAddress = node.toInt()
        floorRef = floor
        roomRef = room
        siteDis = basicInfo.siteDis
        tz = basicInfo.timeZone
        equipDis = "$siteDis-$profileTag-$nodeAddress"
        gatewayRef = systemEquip["id"].toString()

        // get general profile
        val profileEquip =  HyperStatSplitEquip()

        // Create Equip Point
        equipRef =  profileEquip.createNewEquip(
            HyperstatSplitProfileNames.HSSPLIT_CPUECON,
            roomRef!!, floorRef!!,config.priority.toString(),nodeAddress.toString(),
            equipDis!!, ProfileType.HYPERSTATSPLIT_CPU
        )
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "New Profile  New Equip Created: $equipRef")

        // Init required reference
        init(HyperstatSplitProfileNames.HSSPLIT_CPUECON, equipRef!!,floorRef!!,roomRef!!,nodeAddress,equipDis!!)

        // add Tuner Points
        createHyperStatSplitTunerPoints(
            equipRef!!, equipDis!!, roomRef!!, floorRef!!
        )
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "New Profile  Tuners Created")
        
        // Create config points
        createProfileConfigurationPoints(hyperStatSplitConfig = config)

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "New Profile Profile configuration points are created ")
        // Create Logical Points
        createProfileLogicalPoints(hyperStatSplitConfig = config)

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "New Profile  logical points are created")

        configHyperStatSplitDevice(config, profileEquip)

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "HSS device is created")

        addOTAStatusPoint("${Tags.HS}-$nodeAddress", equipRef!!, basicInfo.siteRef, roomRef!!, floorRef!!, nodeAddress, basicInfo.timeZone, haystack)

        updateConditioningMode()
        // Syncing the Points
        haystack.syncEntityTree()
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "All done initializing points")
        return
    }

    fun initEquipReference(node: Short) {
        val equip = haystack.read("equip and $HYPERSTATSPLIT and group == \"$node\"")
        if (equip.isEmpty()) {
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, " Unable to find the equip details for node $node ")
            return
        } else {
            Log.i(L.TAG_CCU_HSSPLIT_CPUECON, " Found equip for node $node ")
        }
        equipRef = equip["id"].toString()
        roomRef = equip["roomRef"].toString()
        floorRef = equip["floorRef"].toString()
        siteDis = basicInfo.siteDis
        tz = basicInfo.timeZone
        equipDis = "$siteDis-$profileTag-$node"
        nodeAddress = node.toInt()

        // Init required reference
        init(HyperstatSplitProfileNames.HSSPLIT_CPUECON, equipRef!!,floorRef!!,roomRef!!,node.toInt(),equipDis!!)
    }

    private fun createHyperStatSplitTunerPoints(
        equipRef: String, equipDis: String, roomRef: String, floorRef: String,
    ) {
        RxTask.executeAsync {
            HyperstatSplitCpuEconTuners.addHyperStatSplitModuleTuners(
                CCUHsApi.getInstance(), basicInfo.siteRef,
                equipRef, equipDis, basicInfo.timeZone, roomRef, floorRef
            )
        }
    }

    private fun configHyperStatSplitDevice(config: HyperStatSplitCpuEconConfiguration, profileEquip: HyperStatSplitEquip) {

        val hyperStatSplitDevice = HyperStatSplitDevice(
            nodeAddress, basicInfo.siteRef, floorRef, roomRef, equipRef, HyperstatSplitProfileNames.HSSPLIT_CPUECON
        )

        val heartBeatId = CCUHsApi.getInstance().addPoint(
            nodeAddress.let {
                HeartBeat.getHeartBeatPoint(
                    equipDis, equipRef,
                    basicInfo.siteRef, roomRef, floorRef, it, HyperstatSplitProfileNames.HSSPLIT_CPUECON, tz
                )
            }
        )

        profileEquip.setupDeviceRelays(
            config.relay1State.enabled, config.relay2State.enabled, config.relay3State.enabled,
            config.relay4State.enabled, config.relay5State.enabled, config.relay6State.enabled,
            config.relay7State.enabled, config.relay8State.enabled,
            masterPoints, hyperStatSplitDevice

        )

        profileEquip.setupDeviceAnalogOuts(
            config.analogOut1State.enabled, getCpuEconAnalogOutVoltageAtMin(config.analogOut1State),getCpuEconAnalogOutVoltageAtMax(config.analogOut1State),
            config.analogOut2State.enabled, getCpuEconAnalogOutVoltageAtMin(config.analogOut2State),getCpuEconAnalogOutVoltageAtMax(config.analogOut2State),
            config.analogOut3State.enabled, getCpuEconAnalogOutVoltageAtMin(config.analogOut3State),getCpuEconAnalogOutVoltageAtMax(config.analogOut3State),
            config.analogOut4State.enabled, getCpuEconAnalogOutVoltageAtMin(config.analogOut4State),getCpuEconAnalogOutVoltageAtMax(config.analogOut4State),
            masterPoints, hyperStatSplitDevice
        )

        // For Universal Inputs, "analogType" tag corresponds to UniversalInAssociation, not SensorType
        profileEquip.setupDeviceUniversalIns(
            config.universalIn1State.enabled, config.universalIn1State.association,
            config.universalIn2State.enabled, config.universalIn2State.association,
            config.universalIn3State.enabled, config.universalIn3State.association,
            config.universalIn4State.enabled, config.universalIn4State.association,
            config.universalIn5State.enabled, config.universalIn5State.association,
            config.universalIn6State.enabled, config.universalIn6State.association,
            config.universalIn7State.enabled, config.universalIn7State.association,
            config.universalIn8State.enabled, config.universalIn8State.association,
            masterPoints, hyperStatSplitDevice
        )

        hyperStatSplitDevice.currentTemp.pointRef = masterPoints[LogicalKeyID.CURRENT_TEMP]
        hyperStatSplitDevice.currentTemp.enabled = true

        hyperStatSplitDevice.addSensor(Port.SENSOR_RH, masterPoints[Port.SENSOR_RH], "%RH")
        hyperStatSplitDevice.addSensor(Port.SENSOR_ILLUMINANCE, masterPoints[Port.SENSOR_ILLUMINANCE], "lx")
        hyperStatSplitDevice.addSensor(Port.SENSOR_OCCUPANCY, masterPoints[Port.SENSOR_OCCUPANCY])
        hyperStatSplitDevice.addSensor(Port.SENSOR_CO2, masterPoints[Port.SENSOR_CO2], "ppm")
        hyperStatSplitDevice.addSensor(Port.SENSOR_VOC, masterPoints[Port.SENSOR_VOC], "ppb")
        //hyperStatSplitDevice.addSensor(Port.SENSOR_CO2_EQUIVALENT, masterPoints[Port.SENSOR_CO2_EQUIVALENT], "ppm")
        hyperStatSplitDevice.addSensor(Port.SENSOR_SOUND, masterPoints[Port.SENSOR_SOUND], "db")

        profileEquip.setupDeviceSensorBus(
            config.address0State.enabled, config.address0State.association.ordinal,
            config.address1State.enabled, config.address1State.association.ordinal,
            config.address2State.enabled, config.address2State.association.ordinal,
            config.address3State.enabled, config.address3State.association.ordinal,
            masterPoints, hyperStatSplitDevice
        )

        hyperStatSplitDevice.rssi.pointRef = heartBeatId
        hyperStatSplitDevice.rssi.enabled = true

        hyperStatSplitDevice.addPointsToDb()

    }

    private fun getCpuEconAnalogOutVoltageAtMin(analogOutState: AnalogOutState): Int {
        return if (analogOutState.association == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
            analogOutVoltageMin
        } else {
            analogOutState.voltageAtMin.toInt()
        }
    }

    private fun getCpuEconAnalogOutVoltageAtMax(analogOutState: AnalogOutState): Int {
        return if (analogOutState.association == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
            analogOutVoltageMax
        } else {
            analogOutState.voltageAtMax.toInt()
        }
    }

    /*
        Function which creates Profile configuration Points

        Points that are added from CPU include:
            - Config points for new hardware
            - OAO Tuners (now zone-level instead of system-level)
     */
    private fun createProfileConfigurationPoints(hyperStatSplitConfig: HyperStatSplitCpuEconConfiguration) {

        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, hyperStatSplitConfig.toString())

        hyperStatSplitPointsUtil.addProfilePoints()

        hyperStatSplitPointsUtil.createTemperatureOffSetPoint(hyperStatSplitConfig.temperatureOffset * 10)

        // List Of Configuration Points
        val configPointsList: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil.createAutoForceAutoAwayConfigPoints(
             hyperStatSplitConfig.isEnableAutoForceOccupied, hyperStatSplitConfig.isEnableAutoAway
        )

        val co2ConfigPointsList: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil
            .createPointCO2ConfigPoint(
                 hyperStatSplitConfig.zoneCO2DamperOpeningRate,hyperStatSplitConfig.zoneCO2Threshold,hyperStatSplitConfig.zoneCO2Target
        )

        val vocPmPointsList: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil
            .createPointVOCPmConfigPoint( equipDis!!,
                hyperStatSplitConfig.zoneVOCThreshold,hyperStatSplitConfig.zoneVOCTarget,
                hyperStatSplitConfig.zonePm2p5Target
            )

        val loopOutputPoints: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil.createConditioningLoopOutputPoints(false)

        val zoneOAOPoints: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil.createZoneOAOPoints(
            hyperStatSplitConfig.outsideDamperMinOpen, hyperStatSplitConfig.exhaustFanStage1Threshold,
            hyperStatSplitConfig.exhaustFanStage2Threshold, hyperStatSplitConfig.exhaustFanHysteresis)

        OAOTuners.updateStandaloneOaoTuners(
            haystack,
            basicInfo.siteRef,
            equipRef,
            equipDis,
            basicInfo.timeZone,
            floorRef,
            roomRef
        )

        val relayConfigPoints: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil.createIsRelayEnabledConfigPoints(
            ConfigState(hyperStatSplitConfig.relay1State.enabled, hyperStatSplitConfig.relay1State.association.ordinal),
            ConfigState(hyperStatSplitConfig.relay2State.enabled, hyperStatSplitConfig.relay2State.association.ordinal),
            ConfigState(hyperStatSplitConfig.relay3State.enabled, hyperStatSplitConfig.relay3State.association.ordinal),
            ConfigState(hyperStatSplitConfig.relay4State.enabled, hyperStatSplitConfig.relay4State.association.ordinal),
            ConfigState(hyperStatSplitConfig.relay5State.enabled, hyperStatSplitConfig.relay5State.association.ordinal),
            ConfigState(hyperStatSplitConfig.relay6State.enabled, hyperStatSplitConfig.relay6State.association.ordinal),
            ConfigState(hyperStatSplitConfig.relay7State.enabled, hyperStatSplitConfig.relay7State.association.ordinal),
            ConfigState(hyperStatSplitConfig.relay8State.enabled, hyperStatSplitConfig.relay8State.association.ordinal),
            ProfileType.HYPERSTATSPLIT_CPU
        )

        val analogOutConfigPoints: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil
            .createIsAnalogOutEnabledConfigPoints(
                ConfigState(hyperStatSplitConfig.analogOut1State.enabled, hyperStatSplitConfig.analogOut1State.association.ordinal),
                ConfigState(hyperStatSplitConfig.analogOut2State.enabled, hyperStatSplitConfig.analogOut2State.association.ordinal),
                ConfigState(hyperStatSplitConfig.analogOut3State.enabled, hyperStatSplitConfig.analogOut3State.association.ordinal),
                ConfigState(hyperStatSplitConfig.analogOut4State.enabled, hyperStatSplitConfig.analogOut4State.association.ordinal),
                ProfileType.HYPERSTATSPLIT_CPU
            )

        val universalInConfigPoints: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil
            .createIsUniversalInEnabledConfigPoints(
                ConfigState(hyperStatSplitConfig.universalIn1State.enabled, hyperStatSplitConfig.universalIn1State.association.ordinal),
                ConfigState(hyperStatSplitConfig.universalIn2State.enabled, hyperStatSplitConfig.universalIn2State.association.ordinal),
                ConfigState(hyperStatSplitConfig.universalIn3State.enabled, hyperStatSplitConfig.universalIn3State.association.ordinal),
                ConfigState(hyperStatSplitConfig.universalIn4State.enabled, hyperStatSplitConfig.universalIn4State.association.ordinal),
                ConfigState(hyperStatSplitConfig.universalIn5State.enabled, hyperStatSplitConfig.universalIn5State.association.ordinal),
                ConfigState(hyperStatSplitConfig.universalIn6State.enabled, hyperStatSplitConfig.universalIn6State.association.ordinal),
                ConfigState(hyperStatSplitConfig.universalIn7State.enabled, hyperStatSplitConfig.universalIn7State.association.ordinal),
                ConfigState(hyperStatSplitConfig.universalIn8State.enabled, hyperStatSplitConfig.universalIn8State.association.ordinal),
                ProfileType.HYPERSTATSPLIT_CPU
            )

        val sensorBusConfigPoints: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil
            .createIsSensorBusEnabledConfigPoints(
                ConfigState(hyperStatSplitConfig.address0State.enabled, hyperStatSplitConfig.address0State.association.ordinal),
                ConfigState(hyperStatSplitConfig.address1State.enabled, hyperStatSplitConfig.address1State.association.ordinal),
                ConfigState(hyperStatSplitConfig.address2State.enabled, hyperStatSplitConfig.address2State.association.ordinal),
                ConfigState(hyperStatSplitConfig.address3State.enabled, hyperStatSplitConfig.address3State.association.ordinal),
                ProfileType.HYPERSTATSPLIT_CPU
            )

            // For user intent Point CCU level default values need to be added
        val userIntentPointsList: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil
            .createUserIntentPoints(
            hyperStatSplitPointsUtil.getCPUEconDefaultFanSpeed(hyperStatSplitConfig),
                hyperStatSplitPointsUtil.getCPUEconDefaultConditioningMode(hyperStatSplitConfig)
        )

        // device display configuration
        val deviceDisplayConfigPoints: MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil
            .createDeviceDisplayConfigurationPoints(
                hyperStatSplitConfig.displayHumidity,hyperStatSplitConfig.displayVOC,
                hyperStatSplitConfig.displayPp2p5,hyperStatSplitConfig.displayCo2
        )

        var allConfigPoints = arrayOf(
            configPointsList, relayConfigPoints, analogOutConfigPoints,
            universalInConfigPoints, sensorBusConfigPoints, userIntentPointsList,
            co2ConfigPointsList, loopOutputPoints, zoneOAOPoints, vocPmPointsList,deviceDisplayConfigPoints
        )

        if (isStagedFanSelected(hyperStatSplitConfig)) {
            val stagedFanConfigPoints : MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil.createStagedFanConfigPoint(hyperStatSplitConfig)
            if (stagedFanConfigPoints.isNotEmpty()) {
                allConfigPoints += stagedFanConfigPoints
            }
        }

        hyperStatSplitPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = allConfigPoints)
    }

    private fun isStagedFanSelected(hyperStatSplitConfig: HyperStatSplitCpuEconConfiguration): Boolean {
        return  (hyperStatSplitConfig.analogOut1State.enabled && (hyperStatSplitConfig.analogOut1State.association == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED)) ||
                (hyperStatSplitConfig.analogOut2State.enabled && (hyperStatSplitConfig.analogOut2State.association == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED)) ||
                (hyperStatSplitConfig.analogOut3State.enabled && (hyperStatSplitConfig.analogOut3State.association == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED))
    }

    //Function to create Logical Points
    private fun createProfileLogicalPoints(hyperStatSplitConfig: HyperStatSplitCpuEconConfiguration) {

        val temperaturePointsList: MutableList<Triple<Point, Any, Any>> = hyperStatSplitPointsUtil
            .temperatureScheduleLogicalPoints()

        val sensorBusPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatSplitPointsUtil
            .createConfigSensorBusLogicalPoints(
                hyperStatSplitConfig = hyperStatSplitConfig
            )
        val relayPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatSplitPointsUtil
            .createConfigRelayLogicalPoints(
                hyperStatSplitConfig = hyperStatSplitConfig
            )
        val analogOutPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatSplitPointsUtil
            .createConfigAnalogOutLogicalPoints(
                hyperStatSplitConfig = hyperStatSplitConfig
            )
        val universalInPointsList: MutableList<Triple<Point, Any, Any>> =
            hyperStatSplitPointsUtil.createConfigUniversalInLogicalPoints(
                hyperStatSplitConfig.universalIn1State,hyperStatSplitConfig.universalIn2State,
                hyperStatSplitConfig.universalIn3State,hyperStatSplitConfig.universalIn4State,
                hyperStatSplitConfig.universalIn5State,hyperStatSplitConfig.universalIn6State,
                hyperStatSplitConfig.universalIn7State,hyperStatSplitConfig.universalIn8State
            )

        // Device Class will create all the sensor points dynamically based on the input message
        // which is received by the Hyper state device
        // TODO: circle back on this once firmware is ready
        val sensorPointsList: MutableList<Triple<Point, Any, Any>> = hyperStatSplitPointsUtil.createLogicalSensorPoints()

        masterPoints.putAll(hyperStatSplitPointsUtil.addPointsToHaystack(listOfAllPoints = sensorPointsList))
        masterPoints.putAll(hyperStatSplitPointsUtil.addPointsToHaystack(listOfAllPoints = sensorBusPointsList))
        masterPoints.putAll(hyperStatSplitPointsUtil.addPointsToHaystack(listOfAllPoints = temperaturePointsList))
        masterPoints.putAll(hyperStatSplitPointsUtil.addPointsToHaystack(listOfAllPoints = relayPointsList))
        masterPoints.putAll(hyperStatSplitPointsUtil.addPointsToHaystack(listOfAllPoints = analogOutPointsList))
        masterPoints.putAll(hyperStatSplitPointsUtil.addPointsToHaystack(listOfAllPoints = universalInPointsList))

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Done with Logical points creation")

    }

    // Function to update the existing profile
    override fun updateConfiguration(configuration: BaseProfileConfiguration) {
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "===========HyperStat Split Profile Update  ============")

        val updatedHyperStatSplitConfig =  configuration as HyperStatSplitCpuEconConfiguration
        val presetConfiguration = getConfiguration()

        updateGeneralConfiguration(newConfiguration = updatedHyperStatSplitConfig, existingConfiguration = presetConfiguration)
        updateRelaysConfig(newConfiguration = updatedHyperStatSplitConfig, existingConfiguration = presetConfiguration)
        updateAnalogOutConfig(newConfiguration = updatedHyperStatSplitConfig, existingConfiguration = presetConfiguration)
        updateUniversalInConfig(newConfiguration = updatedHyperStatSplitConfig, existingConfiguration = presetConfiguration)
        updateSensorBusConfig(newConfiguration = updatedHyperStatSplitConfig, existingConfiguration = presetConfiguration)

        LogicalPointsUtil.cleanCpuEconLogicalPoints(updatedHyperStatSplitConfig,equipRef!!)

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Profile update has been completed  ")
        haystack.syncEntityTree()

    }

    // function to update (If change in configuration)
    // Temp Off set , CO2 threshold and target, VOC threshold and target, pm2.5 threshold and target
    private fun updateGeneralConfiguration(
        newConfiguration: HyperStatSplitCpuEconConfiguration,
        existingConfiguration: HyperStatSplitCpuEconConfiguration){

        updateTempOffset(existingConfiguration.temperatureOffset ,newConfiguration.temperatureOffset)

        updateAutoAwayAutoForceOccupy(
            existingConfiguration.isEnableAutoAway, newConfiguration.isEnableAutoAway,
            existingConfiguration.isEnableAutoForceOccupied, newConfiguration.isEnableAutoForceOccupied,
        )

        updateOAOValues(
            existingConfiguration.outsideDamperMinOpen,newConfiguration.outsideDamperMinOpen,
            existingConfiguration.exhaustFanStage1Threshold,newConfiguration.exhaustFanStage1Threshold,
            existingConfiguration.exhaustFanStage2Threshold,newConfiguration.exhaustFanStage2Threshold,
            existingConfiguration.exhaustFanHysteresis,newConfiguration.exhaustFanHysteresis
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


    }
    
    private fun updateStagedFanConfigPoints(
        existingConfiguration: HyperStatSplitCpuEconConfiguration,
        newConfiguration: HyperStatSplitCpuEconConfiguration
    ) {
        fun createStagedFanConfigPointIfEnabled(fanStageQuery: String, stage: CpuEconRelayAssociation) {
            if (HyperStatSplitAssociationUtil.isAnyAnalogOutMappedToStagedFan(newConfiguration) && !HyperStatSplitAssociationUtil.isAnyAnalogOutMappedToStagedFan(existingConfiguration)) {
                if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration,stage)) {
                    val stagedFanConfigPoints : MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil.createStagedFanPoint(newConfiguration, stage)
                    hyperStatSplitPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = arrayOf(
                        stagedFanConfigPoints
                    ))
                }
            } else if (HyperStatSplitAssociationUtil.isAnyAnalogOutMappedToStagedFan(newConfiguration) && HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, stage)) {
                if (!HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, stage)) {
                    val stagedFanConfigPoints : MutableList<Pair<Point, Any>> = hyperStatSplitPointsUtil.createStagedFanPoint(newConfiguration, stage)
                    hyperStatSplitPointsUtil.addPointsListToHaystackWithDefaultValue(listOfAllPoints = arrayOf(
                        stagedFanConfigPoints
                    ))
                }
            } else {
                if (HyperStatSplitAssociationUtil.isAnyAnalogOutMappedToStagedFan(existingConfiguration) && HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, stage)) {
                    val pointId = hsSplitHaystackUtil.readPointID(fanStageQuery)
                    if (!pointId.isNullOrEmpty()) {
                        hsSplitHaystackUtil.removePoint(pointId)
                    }
                }
            }
        }
        createStagedFanConfigPointIfEnabled(
            "stage1 and cooling and fan",
            CpuEconRelayAssociation.COOLING_STAGE_1)
        createStagedFanConfigPointIfEnabled(
            "stage2 and cooling and fan",
            CpuEconRelayAssociation.COOLING_STAGE_2
        )
        createStagedFanConfigPointIfEnabled(
            "stage3 and cooling and fan",
            CpuEconRelayAssociation.COOLING_STAGE_3
        )
        createStagedFanConfigPointIfEnabled(
            "stage1 and heating and fan",
            CpuEconRelayAssociation.HEATING_STAGE_1
        )
        createStagedFanConfigPointIfEnabled(
            "stage2 and heating and fan",
            CpuEconRelayAssociation.HEATING_STAGE_2
        )
        createStagedFanConfigPointIfEnabled(
            "stage3 and heating and fan",
            CpuEconRelayAssociation.HEATING_STAGE_3
        )
    }

    private fun updateStagedFanConfiguration(
        existingConfiguration: HyperStatSplitCpuEconConfiguration, newConfiguration: HyperStatSplitCpuEconConfiguration,
        coolingStage1FanOld: Int, coolingStage1FanNew: Int,
        coolingStage2FanOld: Int, coolingStage2FanNew: Int,
        coolingStage3FanOld: Int, coolingStage3FanNew: Int,
        heatingStage1FanOld: Int, heatingStage1FanNew: Int,
        heatingStage2FanOld: Int, heatingStage2FanNew: Int,
        heatingStage3FanOld: Int, heatingStage3FanNew: Int
    ) {
        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.COOLING_STAGE_1) &&
            HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuEconRelayAssociation.COOLING_STAGE_1) &&
            coolingStage1FanOld != coolingStage1FanNew
        ) {
            val pointId = hsSplitHaystackUtil.readPointID("stage1 and cooling and fan")
            val defaultValue = if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.COOLING_STAGE_1))
                coolingStage1FanNew
            else stageFan1DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.COOLING_STAGE_2) &&
            HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuEconRelayAssociation.COOLING_STAGE_2) &&
            coolingStage2FanOld != coolingStage2FanNew
        ) {
            val pointId = hsSplitHaystackUtil.readPointID("stage2 and cooling and fan")
            val defaultValue = if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.COOLING_STAGE_2))
                coolingStage2FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.COOLING_STAGE_3) &&
            HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuEconRelayAssociation.COOLING_STAGE_3) &&
            coolingStage3FanOld != coolingStage3FanNew
        ) {
            val pointId = hsSplitHaystackUtil.readPointID("stage3 and cooling and fan")
            val defaultValue = if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.COOLING_STAGE_3))
                coolingStage3FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.HEATING_STAGE_1) &&
            HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuEconRelayAssociation.HEATING_STAGE_1) &&
            heatingStage1FanOld != heatingStage1FanNew
        ) {
            val pointId = hsSplitHaystackUtil.readPointID("stage1 and heating and fan")
            val defaultValue = if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.HEATING_STAGE_1))
                heatingStage1FanNew
            else stageFan1DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.HEATING_STAGE_2) &&
            HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuEconRelayAssociation.HEATING_STAGE_2) &&
            heatingStage2FanOld != heatingStage2FanNew
        ) {
            val pointId = hsSplitHaystackUtil.readPointID("stage2 and heating and fan")
            val defaultValue = if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.HEATING_STAGE_2))
                heatingStage2FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.HEATING_STAGE_3) &&
            HyperStatSplitAssociationUtil.isStagedFanEnabled(existingConfiguration, CpuEconRelayAssociation.HEATING_STAGE_3) &&
            heatingStage3FanOld != heatingStage3FanNew
        ) {
            val pointId = hsSplitHaystackUtil.readPointID("stage3 and heating and fan")
            val defaultValue = if (HyperStatSplitAssociationUtil.isStagedFanEnabled(newConfiguration, CpuEconRelayAssociation.HEATING_STAGE_3))
                heatingStage3FanNew
            else stageFan2And3DefaultVal
            if (!pointId.isNullOrEmpty()) {
                hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, defaultValue)
            }
        }
    }

    private fun updateOAOValues(
        oldOutsideDamperMinOpen: Double, newOutsideDamperMinOpen: Double,
        oldExhaustFanStage1Threshold: Double, newExhaustFanStage1Threshold: Double,
        oldExhaustFanStage2Threshold: Double, newExhaustFanStage2Threshold: Double,
        oldExhaustFanHysteresis: Double, newExhaustFanHysteresis: Double
    ) {

        if (oldOutsideDamperMinOpen != newOutsideDamperMinOpen) {
            val pointId = hsSplitHaystackUtil.readPointID("outside and damper and min and open") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newOutsideDamperMinOpen)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newOutsideDamperMinOpen)
        }

        if (oldExhaustFanStage1Threshold != newExhaustFanStage1Threshold) {
            val pointId = hsSplitHaystackUtil.readPointID("exhaust and fan and stage1 and threshold") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newExhaustFanStage1Threshold)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newExhaustFanStage1Threshold)
        }

        if (oldExhaustFanStage2Threshold != newExhaustFanStage2Threshold) {
            val pointId = hsSplitHaystackUtil.readPointID("exhaust and fan and stage2 and threshold") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newExhaustFanStage2Threshold)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newExhaustFanStage2Threshold)
        }

        if (oldExhaustFanHysteresis != newExhaustFanHysteresis) {
            val pointId = hsSplitHaystackUtil.readPointID("exhaust and fan and hysteresis") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newExhaustFanHysteresis)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newExhaustFanHysteresis)
        }
        
    }
    
    // collect the existing profile configurations
    fun getConfiguration(): HyperStatSplitCpuEconConfiguration {
        val config = HyperStatSplitCpuEconConfiguration()

        getRelayConfigurations(config)
        getAnalogOutConfigurations(config)
        getUniversalInConfigurations(config)
        getSensorBusConfigurations(config)

        config.temperatureOffset = hsSplitHaystackUtil.getTempOffValue()
        config.isEnableAutoForceOccupied = hsSplitHaystackUtil.isAutoForceOccupyEnabled()
        config.isEnableAutoAway =  hsSplitHaystackUtil.isAutoAwayEnabled()

        config.outsideDamperMinOpen = hsSplitHaystackUtil.getOutsideDamperMinOpen()
        config.exhaustFanStage1Threshold = hsSplitHaystackUtil.getExhaustFanStage1Threshold()
        config.exhaustFanStage2Threshold = hsSplitHaystackUtil.getExhaustFanStage2Threshold()
        config.exhaustFanHysteresis = hsSplitHaystackUtil.getExhaustFanHysteresis()

        config.zoneCO2DamperOpeningRate = hsSplitHaystackUtil.getCO2DamperOpeningRate()
        config.zoneCO2Threshold = hsSplitHaystackUtil.getCo2DamperThresholdConfigValue()
        config.zoneCO2Target = hsSplitHaystackUtil.getCo2TargetConfigValue()
        config.zoneVOCThreshold = hsSplitHaystackUtil.getVocThresholdConfigValue()
        config.zoneVOCTarget = hsSplitHaystackUtil.getVocTargetConfigValue()
        config.zonePm2p5Target = hsSplitHaystackUtil.getPm2p5TargetConfigValue()

        config.coolingStage1FanState = hsSplitHaystackUtil.getFanStageValue("cooling and stage1",7).toInt()
        config.coolingStage2FanState = hsSplitHaystackUtil.getFanStageValue("cooling and stage2",10).toInt()
        config.coolingStage3FanState = hsSplitHaystackUtil.getFanStageValue("cooling and stage3",10).toInt()
        config.heatingStage1FanState = hsSplitHaystackUtil.getFanStageValue("heating and stage1",7).toInt()
        config.heatingStage2FanState = hsSplitHaystackUtil.getFanStageValue("heating and stage2",10).toInt()
        config.heatingStage3FanState = hsSplitHaystackUtil.getFanStageValue("heating and stage3",10).toInt()

        config.displayHumidity = hsSplitHaystackUtil.getDisplayHumidity() == 1.0
        config.displayCo2 = hsSplitHaystackUtil.getDisplayCo2() == 1.0
        config.displayVOC = hsSplitHaystackUtil.getDisplayVoc() == 1.0
        config.displayPp2p5 = hsSplitHaystackUtil.getDisplayP2p5() == 1.0

        return config
    }

    //  To show the existing profile configurations
    // config relays
    fun getRelayConfigurations(config: HyperStatSplitCpuEconConfiguration):HyperStatSplitCpuEconConfiguration {
        val r1 =  hsSplitHaystackUtil.readConfigStatus("relay1").toInt()
        val r2 =  hsSplitHaystackUtil.readConfigStatus("relay2").toInt()
        val r3 =  hsSplitHaystackUtil.readConfigStatus("relay3").toInt()
        val r4 =  hsSplitHaystackUtil.readConfigStatus("relay4").toInt()
        val r5 =  hsSplitHaystackUtil.readConfigStatus("relay5").toInt()
        val r6 =  hsSplitHaystackUtil.readConfigStatus("relay6").toInt()
        val r7 =  hsSplitHaystackUtil.readConfigStatus("relay7").toInt()
        val r8 =  hsSplitHaystackUtil.readConfigStatus("relay8").toInt()

        val r1AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay1")
        val r2AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay2")
        val r3AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay3")
        val r4AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay4")
        val r5AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay5")
        val r6AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay6")
        val r7AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay7")
        val r8AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("relay8")

        config.relay1State =
            RelayState(r1 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r1AssociatedTo.toInt()))
        config.relay2State =
            RelayState(r2 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r2AssociatedTo.toInt()))
        config.relay3State =
            RelayState(r3 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r3AssociatedTo.toInt()))
        config.relay4State =
            RelayState(r4 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r4AssociatedTo.toInt()))
        config.relay5State =
            RelayState(r5 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r5AssociatedTo.toInt()))
        config.relay6State =
            RelayState(r6 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r6AssociatedTo.toInt()))
        config.relay7State =
            RelayState(r7 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r7AssociatedTo.toInt()))
        config.relay8State =
            RelayState(r8 == 1, HyperStatSplitAssociationUtil.getRelayAssociatedStage(r8AssociatedTo.toInt()))

        return config
    }

    //config Analog Out
     fun getAnalogOutConfigurations(config: HyperStatSplitCpuEconConfiguration):HyperStatSplitCpuEconConfiguration {
        val ao1 = hsSplitHaystackUtil.readConfigStatus("analog1 and output  ").toInt()
        val ao2 = hsSplitHaystackUtil.readConfigStatus("analog2 and output ").toInt()
        val ao3 = hsSplitHaystackUtil.readConfigStatus("analog3 and output ").toInt()
        val ao4 = hsSplitHaystackUtil.readConfigStatus("analog4 and output ").toInt()

        val ao1AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("analog1 and output ")
        val ao2AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("analog2 and output ")
        val ao3AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("analog3 and output ")
        val ao4AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("analog4 and output ")

        var ao1MinVal = 2.0
        var ao1MaxVal = 10.0

        var ao1fanLow = 70.0
        var ao1fanMedium = 80.0
        var ao1fanHigh = 100.0

        if (ao1 == 1) {
            if (ao1AssociatedTo.toInt() != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                ao1MinVal = hsSplitHaystackUtil.readConfigPointValue(
                    "analog1 and output and min"
                )
                ao1MaxVal = hsSplitHaystackUtil.readConfigPointValue(
                    "analog1 and output and max"
                )
            }

            if (HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao1AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED ||
                HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao1AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                ao1fanLow = hsSplitHaystackUtil.readConfigPointValue(
                    "analog1 and output and low"
                )
                ao1fanMedium = hsSplitHaystackUtil.readConfigPointValue(
                    "analog1 and output and medium"
                )
                ao1fanHigh = hsSplitHaystackUtil.readConfigPointValue(
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
            if (ao2AssociatedTo.toInt() != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                ao2MinVal = hsSplitHaystackUtil.readConfigPointValue(
                        "analog2 and output and min"
                    )
                ao2MaxVal = hsSplitHaystackUtil.readConfigPointValue(
                    "analog2 and output and max"
                )
            }

            if (HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao2AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED ||
                HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao2AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                ao2fanLow = hsSplitHaystackUtil.readConfigPointValue(
                    "analog2 and output and low"
                )
                ao2fanMedium = hsSplitHaystackUtil.readConfigPointValue(
                    "analog2 and output and medium"
                )
                ao2fanHigh = hsSplitHaystackUtil.readConfigPointValue(
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
            if (ao3AssociatedTo.toInt() != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                ao3MinVal = hsSplitHaystackUtil.readConfigPointValue(
                    "analog3 and output and min"
                )
                ao3MaxVal = hsSplitHaystackUtil.readConfigPointValue(
                    "analog3 and output and max"
                )
            }

            if (HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao3AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED ||
                HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao3AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                ao3fanLow = hsSplitHaystackUtil.readConfigPointValue(
                    "analog3 and output and low"
                )
                ao3fanMedium = hsSplitHaystackUtil.readConfigPointValue(
                    "analog3 and output and medium"
                )
                ao3fanHigh = hsSplitHaystackUtil.readConfigPointValue(
                    "analog3 and output and high"
                )
            }
        }

        var ao4MinVal = 2.0
        var ao4MaxVal = 10.0

        var ao4fanLow = 70.0
        var ao4fanMedium = 80.0
        var ao4fanHigh = 100.0

        if (ao4 == 1) {
            if (ao4AssociatedTo.toInt() != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                ao4MinVal = hsSplitHaystackUtil.readConfigPointValue(
                    "analog4 and output and min"
                )
                ao4MaxVal = hsSplitHaystackUtil.readConfigPointValue(
                    "analog4 and output and max"
                )
            }

            if (HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao4AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED ||
                HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(
                    ao4AssociatedTo.toInt()
                ) == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                ao4fanLow = hsSplitHaystackUtil.readConfigPointValue(
                    "analog4 and output and low"
                )
                ao4fanMedium = hsSplitHaystackUtil.readConfigPointValue(
                    "analog4 and output and medium"
                )
                ao4fanHigh = hsSplitHaystackUtil.readConfigPointValue(
                    "analog4 and output and high"
                )
            }
        }

        config.analogOut1State = AnalogOutState(
            ao1 == 1,
            HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(ao1AssociatedTo.toInt()),
            ao1MinVal, ao1MaxVal, ao1fanLow, ao1fanMedium, ao1fanHigh
        )
        config.analogOut2State = AnalogOutState(
            ao2 == 1,
            HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(ao2AssociatedTo.toInt()),
            ao2MinVal, ao2MaxVal, ao2fanLow, ao2fanMedium, ao2fanHigh
        )
        config.analogOut3State = AnalogOutState(
            ao3 == 1,
            HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(ao3AssociatedTo.toInt()),
            ao3MinVal, ao3MaxVal, ao3fanLow, ao3fanMedium, ao3fanHigh
        )
        config.analogOut4State = AnalogOutState(
            ao4 == 1,
            HyperStatSplitAssociationUtil.getAnalogOutAssociatedStage(ao4AssociatedTo.toInt()),
            ao4MinVal, ao4MaxVal, ao4fanLow, ao4fanMedium, ao4fanHigh
        )
        return config
    }

    private fun getSensorBusConfigurations(config: HyperStatSplitCpuEconConfiguration) {

        // TODO: these tags could potentially change. Re-test if they do.
        val addr0 = hsSplitHaystackUtil.readConfigStatus("sensorBus and addr0 and input").toInt()
        val addr1 = hsSplitHaystackUtil.readConfigStatus("sensorBus and addr1 and input").toInt()
        val addr2 = hsSplitHaystackUtil.readConfigStatus("sensorBus and addr2 and input").toInt()
        val addr3 = hsSplitHaystackUtil.readConfigStatus("sensorBus and addr3 and input").toInt()

        val addr0AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("sensorBus and addr0 and input").toInt()
        val addr1AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("sensorBus and addr1 and input").toInt()
        val addr2AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("sensorBus and addr2 and input").toInt()
        val addr3AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("sensorBus and addr3 and input").toInt()

        config.address0State = SensorBusTempState(
            addr0 == 1, HyperStatSplitAssociationUtil.getSensorBusTempStage(addr0AssociatedTo.toInt())
        )
        config.address1State = SensorBusTempState(
            addr1 == 1, HyperStatSplitAssociationUtil.getSensorBusTempStage(addr1AssociatedTo.toInt())
        )
        config.address2State = SensorBusTempState(
            addr2 == 1, HyperStatSplitAssociationUtil.getSensorBusTempStage(addr2AssociatedTo.toInt())
        )
        config.address3State = SensorBusPressState(
            addr3 == 1, HyperStatSplitAssociationUtil.getSensorBusPressStage(addr3AssociatedTo.toInt())
        )

    }

    //config Universal In
    private fun getUniversalInConfigurations(config: HyperStatSplitCpuEconConfiguration) {
        val ui1 = hsSplitHaystackUtil.readConfigStatus("universal1 and input").toInt()
        val ui2 = hsSplitHaystackUtil.readConfigStatus("universal2 and input").toInt()
        val ui3 = hsSplitHaystackUtil.readConfigStatus("universal3 and input").toInt()
        val ui4 = hsSplitHaystackUtil.readConfigStatus("universal4 and input").toInt()
        val ui5 = hsSplitHaystackUtil.readConfigStatus("universal5 and input").toInt()
        val ui6 = hsSplitHaystackUtil.readConfigStatus("universal6 and input").toInt()
        val ui7 = hsSplitHaystackUtil.readConfigStatus("universal7 and input").toInt()
        val ui8 = hsSplitHaystackUtil.readConfigStatus("universal8 and input").toInt()

        val ui1AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal1 and input")
        val ui2AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal2 and input")
        val ui3AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal3 and input")
        val ui4AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal4 and input")
        val ui5AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal5 and input")
        val ui6AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal6 and input")
        val ui7AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal7 and input")
        val ui8AssociatedTo = hsSplitHaystackUtil.readConfigAssociation("universal8 and input")

        config.universalIn1State = UniversalInState(
            ui1 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui1AssociatedTo.toInt())
        )
        config.universalIn2State = UniversalInState(
            ui2 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui2AssociatedTo.toInt())
        )
        config.universalIn3State = UniversalInState(
            ui3 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui3AssociatedTo.toInt())
        )
        config.universalIn4State = UniversalInState(
            ui4 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui4AssociatedTo.toInt())
        )
        config.universalIn5State = UniversalInState(
            ui5 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui5AssociatedTo.toInt())
        )
        config.universalIn6State = UniversalInState(
            ui6 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui6AssociatedTo.toInt())
        )
        config.universalIn7State = UniversalInState(
            ui7 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui7AssociatedTo.toInt())
        )
        config.universalIn8State = UniversalInState(
            ui8 == 1, HyperStatSplitAssociationUtil.getUniversalInStage(ui8AssociatedTo.toInt())
        )

    }

    // To Update the existing profile configurations
    private fun updateRelaysConfig(
        newConfiguration: HyperStatSplitCpuEconConfiguration,
        existingConfiguration: HyperStatSplitCpuEconConfiguration
    ) {
        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay1State, existingConfiguration.relay1State)) {
            updateRelayDetails(existingConfiguration.relay1State, newConfiguration.relay1State, "relay1", Port.RELAY_ONE, newConfiguration)
        }

        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay2State, existingConfiguration.relay2State)) {
            updateRelayDetails(existingConfiguration.relay2State, newConfiguration.relay2State, "relay2", Port.RELAY_TWO, newConfiguration)
        }

        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay3State, existingConfiguration.relay3State)) {
            updateRelayDetails(existingConfiguration.relay3State, newConfiguration.relay3State, "relay3", Port.RELAY_THREE, newConfiguration)
        }

        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay4State, existingConfiguration.relay4State)) {
            updateRelayDetails(existingConfiguration.relay4State, newConfiguration.relay4State, "relay4", Port.RELAY_FOUR, newConfiguration)
        }

        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay5State, existingConfiguration.relay5State)) {
            updateRelayDetails(existingConfiguration.relay5State, newConfiguration.relay5State, "relay5", Port.RELAY_FIVE, newConfiguration)
        }

        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay6State, existingConfiguration.relay6State)) {
            updateRelayDetails(existingConfiguration.relay6State, newConfiguration.relay6State, "relay6", Port.RELAY_SIX, newConfiguration)
        }

        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay7State, existingConfiguration.relay7State)) {
            updateRelayDetails(existingConfiguration.relay7State, newConfiguration.relay7State, "relay7", Port.RELAY_SEVEN, newConfiguration)
        }

        if (!HyperStatSplitAssociationUtil.isBothRelayHasSameConfigs
                (newConfiguration.relay8State, existingConfiguration.relay8State)) {
            updateRelayDetails(existingConfiguration.relay8State, newConfiguration.relay8State, "relay8", Port.RELAY_EIGHT, newConfiguration)
        }

    }

    private fun updateAnalogOutConfig(
        newConfiguration: HyperStatSplitCpuEconConfiguration,
        existingConfiguration: HyperStatSplitCpuEconConfiguration
    ) {
        if (!HyperStatSplitAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut1State, existingConfiguration.analogOut1State)
        ) {
            val changeIn = HyperStatSplitAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut1State, existingConfiguration.analogOut1State
            )
            updateAnalogOutDetails(existingConfiguration.analogOut1State, newConfiguration.analogOut1State, "analog1", Port.ANALOG_OUT_ONE, changeIn, newConfiguration)
        }
        if (!HyperStatSplitAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut2State, existingConfiguration.analogOut2State)
        ) {
            val changeIn = HyperStatSplitAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut2State, existingConfiguration.analogOut2State
            )
            updateAnalogOutDetails(existingConfiguration.analogOut2State, newConfiguration.analogOut2State, "analog2", Port.ANALOG_OUT_TWO, changeIn, newConfiguration)
        }
        if (!HyperStatSplitAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut3State, existingConfiguration.analogOut3State)
        ) {
            val changeIn = HyperStatSplitAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut3State, existingConfiguration.analogOut3State
            )
            updateAnalogOutDetails(existingConfiguration.analogOut3State, newConfiguration.analogOut3State, "analog3", Port.ANALOG_OUT_THREE, changeIn, newConfiguration)
        }
        if (!HyperStatSplitAssociationUtil.isBothAnalogOutHasSameConfigs
                (newConfiguration.analogOut4State, existingConfiguration.analogOut4State)
        ) {
            val changeIn = HyperStatSplitAssociationUtil.findChangeInAnalogOutConfig(
                newConfiguration.analogOut4State, existingConfiguration.analogOut4State
            )
            updateAnalogOutDetails(existingConfiguration.analogOut4State, newConfiguration.analogOut4State, "analog4", Port.ANALOG_OUT_FOUR, changeIn, newConfiguration)
        }
    }

    private fun updateSensorBusConfig(
        newConfiguration: HyperStatSplitCpuEconConfiguration,
        existingConfiguration: HyperStatSplitCpuEconConfiguration
    ) {
        Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "updateSensorBusConfig()")
        if (!HyperStatSplitAssociationUtil.isBothSensorBusAddressHasSameConfigs
                (newConfiguration.address0State, existingConfiguration.address0State)
        ) {
            updateSensorBusDetails(newConfiguration.address0State, "addr0", getTempPort(newConfiguration.address0State), getHumidityPort(newConfiguration.address0State))
        }
        if (!HyperStatSplitAssociationUtil.isBothSensorBusAddressHasSameConfigs
                (newConfiguration.address1State, existingConfiguration.address1State)
        ) {
            updateSensorBusDetails(newConfiguration.address1State, "addr1", getTempPort(newConfiguration.address1State), getHumidityPort(newConfiguration.address1State))
        }
        if (!HyperStatSplitAssociationUtil.isBothSensorBusAddressHasSameConfigs
                (newConfiguration.address2State, existingConfiguration.address2State)
        ) {
            updateSensorBusDetails(newConfiguration.address2State, "addr2", getTempPort(newConfiguration.address2State), getHumidityPort(newConfiguration.address2State))
        }
        if (!HyperStatSplitAssociationUtil.isBothSensorBusAddressHasSameConfigs
                (newConfiguration.address3State, existingConfiguration.address3State)
        ) {
            updateSensorBusDetails(newConfiguration.address3State, "addr3", getPressPort(newConfiguration.address3State))
        }
    }

    private fun updateUniversalInConfig(
        newConfiguration: HyperStatSplitCpuEconConfiguration,
        existingConfiguration: HyperStatSplitCpuEconConfiguration
    ) {
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn1State, existingConfiguration.universalIn1State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn1State, 1, Port.UNIVERSAL_IN_ONE)
        }
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn2State, existingConfiguration.universalIn2State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn2State, 2, Port.UNIVERSAL_IN_TWO)
        }
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn3State, existingConfiguration.universalIn3State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn3State, 3, Port.UNIVERSAL_IN_THREE)
        }
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn4State, existingConfiguration.universalIn4State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn4State, 4, Port.UNIVERSAL_IN_FOUR)
        }
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn5State, existingConfiguration.universalIn5State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn5State, 5, Port.UNIVERSAL_IN_FIVE)
        }
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn6State, existingConfiguration.universalIn6State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn6State, 6, Port.UNIVERSAL_IN_SIX)
        }
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn7State, existingConfiguration.universalIn7State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn7State, 7, Port.UNIVERSAL_IN_SEVEN)
        }
        if (!HyperStatSplitAssociationUtil.isBothUniversalInHasSameConfigs
                (newConfiguration.universalIn8State, existingConfiguration.universalIn8State)
        ) {
            updateUniversalInDetails(newConfiguration.universalIn8State, 8, Port.UNIVERSAL_IN_EIGHT)
        }
    }

    // Function which updates the Relay new configurations
    private fun updateRelayDetails(
        existingRelay:RelayState,
        relayState: RelayState,
        relayTag: String,
        physicalPort: Port,
        newConfiguration: HyperStatSplitCpuEconConfiguration?,
    ) {
        val relayId = hsSplitHaystackUtil.readPointID("config and $relayTag and enabled") as String
        val relayAssociatedId = hsSplitHaystackUtil.readPointID(
            "config and $relayTag and association"
        ) as String
        hyperStatSplitPointsUtil.addDefaultValueForPoint(relayId, if (relayState.enabled) 1.0 else 0.0)
        hyperStatSplitPointsUtil.addDefaultValueForPoint(relayAssociatedId, relayState.association.ordinal.toDouble())
        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, relayState.enabled)
        if (relayState.enabled) {
            val pointData: Point = hyperStatSplitPointsUtil.relayConfiguration(
                relayState = relayState
            )
            val pointId = hyperStatSplitPointsUtil.addPointToHaystack(pointData)
            if (pointData.markers.contains("his")) {
                hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
            }
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
        }
        if(isRelayFanModeUpdateRequired(existingRelay,relayState,newConfiguration)){
            updateFanMode(newConfiguration!!)
        }
        if(HyperStatSplitAssociationUtil.isRelayAssociatedToAnyOfConditioningModes(relayState))
            updateConditioningMode()
    }


    // Function which updates the Analog Out new configurations
    private fun updateAnalogOutDetails(
        existingAnalogOutState: AnalogOutState,
        analogOutState: AnalogOutState,
        analogOutTag: String,
        physicalPort: Port,
        changeIn: AnalogOutChanges,
        newConfiguration: HyperStatSplitCpuEconConfiguration?
    ) {
        // Read enable/disable Point ID
        val analogOutId = hsSplitHaystackUtil.readPointID(
            "config and $analogOutTag and output and enabled"
        ) as String

        // associated  Point ID
        val analogOutAssociatedId = hsSplitHaystackUtil.readPointID(
            "config and $analogOutTag and output and association"
        ) as String

        // Read Analog Out min max fan config Point Id's
        val minPointId = hsSplitHaystackUtil.readPointID("$analogOutTag and output and min")
        val maxPointId = hsSplitHaystackUtil.readPointID("$analogOutTag and output and max")
        // Read Fan config Points
        val fanLowPointId = hsSplitHaystackUtil.readPointID("$analogOutTag and output and low")
        val fanMediumPointId = hsSplitHaystackUtil.readPointID("$analogOutTag and output and medium")
        val fanHighPointId = hsSplitHaystackUtil.readPointID("$analogOutTag and output and high")

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Reconfiguration changeIn $changeIn")

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
        hyperStatSplitPointsUtil.addDefaultValueForPoint(analogOutId, if (analogOutState.enabled) 1.0 else 0.0)
        hyperStatSplitPointsUtil.addDefaultValueForPoint(analogOutAssociatedId, analogOutState.association.ordinal.toDouble())

        // Check if logical logical,min,max, fan config  Point exist
        // (This situation is when previous state was not enabled for this analog now it is enabled )
        // SO there's a possibility that logical,min,max, fan config  id can be null

        if (minPointId != null) hsSplitHaystackUtil.removePoint(minPointId)
        if (maxPointId != null) hsSplitHaystackUtil.removePoint(maxPointId)

        if (fanLowPointId != null) hsSplitHaystackUtil.removePoint(fanLowPointId)
        if (fanMediumPointId != null) hsSplitHaystackUtil.removePoint(fanMediumPointId)
        if (fanHighPointId != null) hsSplitHaystackUtil.removePoint(fanHighPointId)

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "Reconfiguration analogOutState.enabled ${analogOutState.enabled}")
        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, analogOutState.enabled)

        if (analogOutState.enabled) {
            if (analogOutState.association != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = hyperStatSplitPointsUtil.analogOutConfiguration(
                    analogOutState = analogOutState,
                    analogTag = analogOutTag
                )
                val minPoint = (pointData.second as Pair<*, *>)
                val maxPoint = (pointData.third as Pair<*, *>)

                val pointId = hyperStatSplitPointsUtil.addPointToHaystack(pointData.first as Point)
                val newMinPointId = hyperStatSplitPointsUtil.addPointToHaystack(minPoint.first as Point)
                val newMaxPointId = hyperStatSplitPointsUtil.addPointToHaystack(maxPoint.first as Point)

                if ((pointData.first as Point).markers.contains("his")) {
                    hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
                }
                hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                hyperStatSplitPointsUtil.addDefaultValueForPoint(newMinPointId, analogOutState.voltageAtMin)
                hyperStatSplitPointsUtil.addDefaultValueForPoint(newMaxPointId, analogOutState.voltageAtMax)

                val pointType = "${analogOutState.voltageAtMin.toInt()}-${analogOutState.voltageAtMax.toInt()}v"
                DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
                DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)

            } else {
                val pointData: Triple<Point, Any?, Any?> = hyperStatSplitPointsUtil.analogOutConfiguration()
                val pointId = hyperStatSplitPointsUtil.addPointToHaystack(pointData.first)
                val pointType =
                    "${analogOutVoltageMin}-${analogOutVoltageMax}v"
                if ((pointData.first).markers.contains("his")) {
                    hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                }
                DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)
                DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)
            }

            // check if the new state of analog is mapped to Fan Speed
            // then will create new Points for fan configurations

            if (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState) || HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(analogOutState)) {
                // Create Fan configuration Points
                val fanConfigPoints: Triple<Point, Point, Point> = hyperStatSplitPointsUtil.createFanLowMediumHighPoint(
                    analogTag = analogOutTag
                )

                // Add to Haystack
                val lowPointId = hyperStatSplitPointsUtil.addPointToHaystack(fanConfigPoints.first)
                val mediumPointId = hyperStatSplitPointsUtil.addPointToHaystack(fanConfigPoints.second)
                val highPointId = hyperStatSplitPointsUtil.addPointToHaystack(fanConfigPoints.third)

                // Update the default values
                hyperStatSplitPointsUtil.addDefaultValueForPoint(lowPointId, analogOutState.perAtFanLow)
                hyperStatSplitPointsUtil.addDefaultValueForPoint(mediumPointId, analogOutState.perAtFanMedium)
                hyperStatSplitPointsUtil.addDefaultValueForPoint(highPointId, analogOutState.perAtFanHigh)
            }

        }
        CcuLog.i(L.TAG_CCU_ZONE, "changeIn: Analog $changeIn  ${HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)}")

        if(isAnalogFanModeUpdateRequired(existingAnalogOutState,analogOutState,changeIn,newConfiguration)){
            updateFanMode(newConfiguration!!)
        }

        if (newConfiguration != null && ((changeIn == AnalogOutChanges.MAPPING ||changeIn == AnalogOutChanges.ENABLED)
                    && HyperStatSplitAssociationUtil.isAnalogAssociatedToAnyOfConditioningModes(analogOutState))) {
            updateConditioningMode()
        }
    }

    private fun isRelayFanModeUpdateRequired(
        existingRelay:RelayState,
        relayState: RelayState,
        newConfiguration: HyperStatSplitCpuEconConfiguration?,
    ): Boolean {
        return (newConfiguration != null &&
               (HyperStatSplitAssociationUtil.isRelayAssociatedToFan(relayState) ||
               HyperStatSplitAssociationUtil.isRelayAssociatedToFan(existingRelay)))
   }

    private fun isAnalogFanModeUpdateRequired(
        existingAnalogOutState: AnalogOutState,
        analogOutState: AnalogOutState,
        changeIn: AnalogOutChanges,
        newConfiguration: HyperStatSplitCpuEconConfiguration?
    ): Boolean {
        return (newConfiguration != null && ((changeIn == AnalogOutChanges.MAPPING ||changeIn == AnalogOutChanges.ENABLED)
                    && (HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)
                || HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(existingAnalogOutState) || HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(existingAnalogOutState))))
    }

     private fun updateConditioningMode() {
         CcuLog.i(L.TAG_CCU_ZONE, "updateConditioningMode: ")

        val conditioningModeId = hsSplitHaystackUtil.readPointID("zone and sp and conditioning and mode")

        if (conditioningModeId!!.isEmpty()) {
            CcuLog.e(L.TAG_CCU_ZONE, "ConditioningMode point does not exist ")
            return
        }

        val curCondMode = haystack.readDefaultValById(conditioningModeId)
        var conditioningMode = curCondMode

         when(HSSplitHaystackUtil.getPossibleConditioningModeSettings(nodeAddress)) {
             PossibleConditioningMode.BOTH -> {
                 if(curCondMode != StandaloneConditioningMode.AUTO.ordinal.toDouble())
                     conditioningMode = StandaloneConditioningMode.AUTO.ordinal.toDouble()
             }
             PossibleConditioningMode.COOLONLY, PossibleConditioningMode.HEATONLY , PossibleConditioningMode.OFF -> {
                 if(curCondMode != StandaloneConditioningMode.OFF.ordinal.toDouble())
                     conditioningMode = StandaloneConditioningMode.OFF.ordinal.toDouble()
             }
         }
        CcuLog.i(L.TAG_CCU_ZONE, "adjustCPUEconConditioningMode $curCondMode -> $conditioningMode")
        if (curCondMode != conditioningMode) {
            haystack.writeDefaultValById(conditioningModeId, conditioningMode)
            haystack.writeHisValById(conditioningModeId, conditioningMode)
        }
    }

     private fun updateFanMode(config: HyperStatSplitCpuEconConfiguration) {
        val fanLevel = getSelectedFanLevel(config)
        val curFanSpeed = hsSplitHaystackUtil.readPointValue("zone and sp and fan and operation and mode")
         Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "updateFanMode: fanLevel $fanLevel curFanSpeed $curFanSpeed")
         val fallbackFanSpeed: Double = if(fanLevel == 0) { 0.0 } else {
             if (fanLevel > 0 && curFanSpeed.toInt() != StandaloneFanStage.AUTO.ordinal) {
                 StandaloneFanStage.AUTO.ordinal.toDouble()
             } else {
                 curFanSpeed
             }
         }
         val fanModePointId = hsSplitHaystackUtil.readPointID("zone and sp and fan and operation and mode")
         Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "updateFanMode: fallbackFanSpeed $fallbackFanSpeed")
         if(fanModePointId != null)
            hsSplitHaystackUtil.writeDefaultWithHisValue(fanModePointId, fallbackFanSpeed)
    }

    fun getCurrentTemp(): Double {
        return hsSplitHaystackUtil.getCurrentTemp()
    }

    fun getOutsideAirTempSensor(): Double {
        // Return 0 for local OAT if the reading is more than 15 minutes old
        // This will lead to the economizer being locked out
        val outsideTemp = hayStack.readEntity("point and outside and air and temp and sensor and equipRef == \"$equipRef\"")
        val hisitem = hayStack.curRead(outsideTemp["id"].toString())
        val hisItemDate : Date = hisitem.date
        val lastModifiedDateTime = hisItemDate.time
        val currentTime =  Date (System.currentTimeMillis())
        if (currentTime.time - lastModifiedDateTime > TIMER_TO_BE_VALID) { return 0.0 }

        return hsSplitHaystackUtil.getOutsideAirTempSensor()
    }

    fun getOutsideAirHumiditySensor(): Double {
        // Return 0 for local OAH if the reading is more than 15 minutes old
        // This will lead to the economizer being locked out
        val outsideHumidity = hayStack.readEntity("point and outside and air and humidity and sensor and equipRef == \"$equipRef\"")
        val hisitem = hayStack.curRead(outsideHumidity["id"].toString())
        val hisItemDate : Date = hisitem.date
        val lastModifiedDateTime = hisItemDate.time
        val currentTime =  Date (System.currentTimeMillis())
        if (currentTime.time - lastModifiedDateTime > TIMER_TO_BE_VALID) { return 0.0 }

        return hsSplitHaystackUtil.getOutsideAirHumiditySensor()
    }

    /**
     * Function  which collects all the logical associated points
     */
    override fun getLogicalPointList(): HashMap<Any, String> {
        val config = getConfiguration()
        val logicalPoints: HashMap<Any, String> = HashMap()

        if (config.relay1State.enabled) {
            logicalPoints[Port.RELAY_ONE] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay1State.association).id
        }
        if (config.relay2State.enabled) {
            logicalPoints[Port.RELAY_TWO] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay2State.association).id
        }
        if (config.relay3State.enabled) {
            logicalPoints[Port.RELAY_THREE] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay3State.association).id
        }
        if (config.relay4State.enabled) {
            logicalPoints[Port.RELAY_FOUR] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay4State.association).id
        }
        if (config.relay5State.enabled) {
            logicalPoints[Port.RELAY_FIVE] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay5State.association).id
        }
        if (config.relay6State.enabled) {
            logicalPoints[Port.RELAY_SIX] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay6State.association).id
        }
        if (config.relay7State.enabled) {
            logicalPoints[Port.RELAY_SEVEN] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay7State.association).id
        }
        if (config.relay8State.enabled) {
            logicalPoints[Port.RELAY_EIGHT] = hyperStatSplitPointsUtil.getCpuEconRelayLogicalPoint(config.relay8State.association).id
        }

        if (config.analogOut1State.enabled) logicalPoints[Port.ANALOG_OUT_ONE] = hyperStatSplitPointsUtil.getCpuEconAnalogOutLogicalPoint(config.analogOut1State.association).id
        if (config.analogOut2State.enabled) logicalPoints[Port.ANALOG_OUT_TWO] = hyperStatSplitPointsUtil.getCpuEconAnalogOutLogicalPoint(config.analogOut2State.association).id
        if (config.analogOut3State.enabled) logicalPoints[Port.ANALOG_OUT_THREE] = hyperStatSplitPointsUtil.getCpuEconAnalogOutLogicalPoint(config.analogOut3State.association).id
        if (config.analogOut4State.enabled) logicalPoints[Port.ANALOG_OUT_FOUR] = hyperStatSplitPointsUtil.getCpuEconAnalogOutLogicalPoint(config.analogOut4State.association).id

        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "====== Logical Points list : ====")
        logicalPoints.forEach { (key, id) -> Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "key : $key  : $id") }
        Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "=================================")
        return logicalPoints
    }

    fun setHisVal(tags: String, writeVal: Double) {
        haystack.writeHisValByQuery("point and " + tags + " and group == \"" + nodeAddress + "\"", writeVal)

    }

}
