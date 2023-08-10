package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusPressState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusTempState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.UniversalInState
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.HyperStatSplitDevice
import android.util.Log

/**
 * Created for HyperStat by Manjunath K on 11-07-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

open class HyperStatSplitEquip {


    lateinit var profileType: ProfileType
    val basicInfo = getPointBasicInfo()
    var nodeAddress: Int = 0
    lateinit var hsSplitHaystackUtil: HSSplitHaystackUtil
    lateinit var hyperStatSplitPointsUtil: HyperStatSplitPointsUtil


    fun getProfileConfiguration(): BaseProfileConfiguration{
        return BaseProfileConfiguration()
    }

    open fun initializePoints(baseConfig: BaseProfileConfiguration, room: String, floor: String, node: Short) {
        // Override and implement at profile
    }

    fun createNewEquip(
        profileName: String, roomRef: String, floorRef: String,
        priority: String, nodeAddress: String, equipDis: String,
        profileType: ProfileType
    ): String {
        return HyperStatSplitPointsUtil.createHyperStatSplitEquipPoint(
            profileName, basicInfo.siteRef, roomRef, floorRef,
            priority, basicInfo.gatewayRef, basicInfo.timeZone,
            nodeAddress, equipDis, CCUHsApi.getInstance(), profileType
        )
    }
    fun createNewEquip(
        profileName: String, roomRef: String, floorRef: String,
        priority: String, nodeAddress: String, equipDis: String,
        profileType: ProfileType, markers: Array<String>
    ): String {
        return HyperStatSplitPointsUtil.createHyperStatSplitEquipPoint(
            profileName, basicInfo.siteRef, roomRef, floorRef,
            priority, basicInfo.gatewayRef, basicInfo.timeZone,
            nodeAddress, equipDis, CCUHsApi.getInstance(), profileType, markers
        )
    }

    fun init(
        profileName: String, equipRef: String, floorRef: String,
        roomRef: String, nodeAddress: Int, equipDis: String
    ) {
        hyperStatSplitPointsUtil = HyperStatSplitPointsUtil(
            profileName = profileName,
            equipRef = equipRef,
            floorRef = floorRef,
            roomRef = roomRef,
            siteRef = basicInfo.siteRef,
            tz = basicInfo.timeZone,
            nodeAddress = nodeAddress.toString(),
            equipDis = equipDis,
            hayStackAPI = CCUHsApi.getInstance()
        )

        hsSplitHaystackUtil = HSSplitHaystackUtil(
            equipRef, CCUHsApi.getInstance()
        )

    }

    open fun updateConfiguration(configuration: BaseProfileConfiguration) {
        // update at profile specific
    }

    // Function which updates the Sensor Bus Temp new configurations
    fun updateSensorBusDetails(
        sensorBusState: SensorBusTempState,
        sensorBusTag: String,
        tempPort: Port,
        humidityPort: Port,
    ) {
        Log.d("CCU_DEVICE_TEST", " updateSensorBusDetails $sensorBusState $sensorBusTag $tempPort $humidityPort")
        val sensorBusId = hsSplitHaystackUtil.readPointID("config and sensorBus and input and $sensorBusTag and enabled") as String
        val sensorBusAssociatedId = hsSplitHaystackUtil.readPointID("config and sensorBus and input and $sensorBusTag and association") as String
        hyperStatSplitPointsUtil.addDefaultValueForPoint(sensorBusId, if (sensorBusState.enabled) 1.0 else 0.0)
        hyperStatSplitPointsUtil.addDefaultValueForPoint(sensorBusAssociatedId, sensorBusState.association.ordinal.toDouble())
        DeviceUtil.setPointEnabled(nodeAddress, tempPort.toString(), sensorBusState.enabled)
        DeviceUtil.setPointEnabled(nodeAddress, humidityPort.toString(), sensorBusState.enabled)

        if (sensorBusState.enabled) {
            val tempPointData: Point = hyperStatSplitPointsUtil.sensorBusTempConfiguration(
                sensorBusState = sensorBusState
            )
            val tempPointId = hyperStatSplitPointsUtil.addPointToHaystack(tempPointData)
            hyperStatSplitPointsUtil.addDefaultValueForPoint(tempPointId, 0.0)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(tempPointId, 0.0)

            val humidityPointData: Point = hyperStatSplitPointsUtil.sensorBusHumidityConfiguration(
                sensorBusState = sensorBusState
            )
            val humidityPointId = hyperStatSplitPointsUtil.addPointToHaystack(humidityPointData)
            hyperStatSplitPointsUtil.addDefaultValueForPoint(humidityPointId, 0.0)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(humidityPointId, 0.0)
        }

    }

    // Function which updates the Sensor Bus Temp new configurations
    fun updateSensorBusDetails(
        sensorBusState: SensorBusPressState,
        sensorBusTag: String,
        pressPort: Port
    ) {
        val sensorBusId = hsSplitHaystackUtil.readPointID("config and sensorBus and input and $sensorBusTag and enabled") as String
        val sensorBusAssociatedId = hsSplitHaystackUtil.readPointID("config and sensorBus and input and $sensorBusTag and association") as String
        hyperStatSplitPointsUtil.addDefaultValueForPoint(sensorBusId, if (sensorBusState.enabled) 1.0 else 0.0)
        hyperStatSplitPointsUtil.addDefaultValueForPoint(sensorBusAssociatedId, sensorBusState.association.ordinal.toDouble())
        DeviceUtil.setPointEnabled(nodeAddress, pressPort.name, sensorBusState.enabled)

        if (sensorBusState.enabled) {
            val pointData: Point = hyperStatSplitPointsUtil.sensorBusPressureConfiguration(
                sensorBusState = sensorBusState
            )
            val pointId = hyperStatSplitPointsUtil.addPointToHaystack(pointData)


            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
        }
    }

    // Function which updates the Universal In new configurations
    fun updateUniversalInDetails(
        universalInState: UniversalInState,
        universalInTag: String,
        physicalPort: Port
    ) {
        val universalInId = hsSplitHaystackUtil.readPointID("config and $universalInTag and enabled") as String
        val universalInAssociatedId = hsSplitHaystackUtil.readPointID("config and $universalInTag and association") as String
        hyperStatSplitPointsUtil.addDefaultValueForPoint(universalInId, if (universalInState.enabled) 1.0 else 0.0)
        hyperStatSplitPointsUtil.addDefaultValueForPoint(universalInAssociatedId, universalInState.association.ordinal.toDouble())

        DeviceUtil.setPointEnabled(nodeAddress, physicalPort.name, universalInState.enabled)

        if (universalInState.enabled) {

            val pointData: Point = hyperStatSplitPointsUtil.universalInConfiguration(
                universalInState = universalInState,
                universalTag = universalInTag
            )

            val pointId = hyperStatSplitPointsUtil.addPointToHaystack(pointData)

            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, 0.0)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)


            DeviceUtil.updatePhysicalPointRef(nodeAddress, physicalPort.name, pointId)

            val pointType = HyperStatSplitAssociationUtil.getSensorNameByType(universalInState.association)
            DeviceUtil.updatePhysicalPointType(nodeAddress, physicalPort.name, pointType)

            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)

        }
    }

    fun updatePm25Values(
        oldPm2p5Threshold: Double, newPm2p5Threshold: Double,
        oldPm2p5Target: Double, newPm2p5Target: Double ){

        if (oldPm2p5Threshold != newPm2p5Threshold) {
            val pointId = hsSplitHaystackUtil.readPointID("pm2p5 and threshold") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newPm2p5Threshold)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newPm2p5Threshold)
        }
        if (oldPm2p5Target != newPm2p5Target) {
            val pointId = hsSplitHaystackUtil.readPointID("pm2p5 and target") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newPm2p5Target)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newPm2p5Target)
        }
    }

    fun updateVOCValues(
        oldVOCThreshold: Double, newVOCThreshold: Double,
        oldVOCTarget: Double, newVOCTarget: Double ){

        if (oldVOCThreshold != newVOCThreshold) {
            val pointId = hsSplitHaystackUtil.readPointID("voc and threshold") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newVOCThreshold)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newVOCThreshold)
        }
        if (oldVOCTarget != newVOCTarget) {
            val pointId = hsSplitHaystackUtil.readPointID("voc and target") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newVOCTarget)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newVOCTarget)
        }
    }

    fun updateCO2Values(
        oldCO2DamperOpeningRate: Double, newCO2DamperOpeningRate: Double,
        oldCO2Threshold: Double, newCO2Threshold: Double,
        oldCO2Target: Double, newCO2Target: Double){
        if (oldCO2DamperOpeningRate != newCO2DamperOpeningRate) {
            val pointId = hsSplitHaystackUtil.readPointID("zone and co2 and damper and opening and rate") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newCO2DamperOpeningRate)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newCO2DamperOpeningRate)
        }
        if (oldCO2Threshold != newCO2Threshold) {
            val pointId = hsSplitHaystackUtil.readPointID("co2 and threshold") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newCO2Threshold)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newCO2Threshold)
        }
        if (oldCO2Target != newCO2Target) {
            val pointId = hsSplitHaystackUtil.readPointID("co2 and target") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, newCO2Target)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, newCO2Target)
        }
    }

    fun updateDeviceDisplayConfiguration(
        isDisplayHumidityEnabledOld: Boolean,isDisplayHumidityEnabledNew: Boolean,
        isDisplayVOCEnabledOld: Boolean,isDisplayVOCEnabledNew: Boolean,
        isDisplayP2p5EnabledOld: Boolean,isDisplayP2p5EnabledNew: Boolean,
        isDisplayCo2EnabledOld: Boolean,  isDisplayCo2EnabledNew: Boolean,
    ){
        if (isDisplayHumidityEnabledOld != isDisplayHumidityEnabledNew) {
            val pointId = hsSplitHaystackUtil.readPointID("enabled and humidity") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, if(isDisplayHumidityEnabledNew) 1.0 else 0.0)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayHumidityEnabledNew) 1.0 else 0.0)
        }
        if (isDisplayVOCEnabledOld != isDisplayVOCEnabledNew) {
            val pointId = hsSplitHaystackUtil.readPointID("enabled and voc") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId,if(isDisplayVOCEnabledNew) 1.0 else 0.0 )
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayVOCEnabledNew) 1.0 else 0.0 )
        }
        if (isDisplayP2p5EnabledOld != isDisplayP2p5EnabledNew) {
            val pointId = hsSplitHaystackUtil.readPointID("enabled and pm2p5") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId, if(isDisplayP2p5EnabledNew) 1.0 else 0.0 )
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayP2p5EnabledNew) 1.0 else 0.0 )
        }
        if (isDisplayCo2EnabledOld != isDisplayCo2EnabledNew) {
            val pointId = hsSplitHaystackUtil.readPointID("enabled and co2") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId,if(isDisplayCo2EnabledNew) 1.0 else 0.0  )
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayCo2EnabledNew) 1.0 else 0.0)
        }
    }
    
    fun updateTempOffset(oldValue: Double, newValue: Double){
        if (oldValue != newValue) {
            val tempOffId = hsSplitHaystackUtil.readPointID("temp and offset") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(tempOffId, (newValue * 10))
        }
    }

    fun updateAutoAwayAutoForceOccupy(
        oldAutoAwayEnabled: Boolean, newAutoAwayEnabled: Boolean,
        oldAutoForceOccupyEnabled: Boolean, newAutoForceOccupyEnabled: Boolean,
    ){
        if (oldAutoAwayEnabled != newAutoAwayEnabled) {
            val autoAwayEnabledId = hsSplitHaystackUtil.readPointID("config and auto and away") as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(autoAwayEnabledId, if (newAutoAwayEnabled) 1.0 else 0.0 )
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(autoAwayEnabledId,if (newAutoAwayEnabled) 1.0 else 0.0)
            hsSplitHaystackUtil.reWriteOccupancy()
        }

        if (oldAutoForceOccupyEnabled != newAutoForceOccupyEnabled) {
            val autoForceOccupiedEnabledId = hsSplitHaystackUtil.readPointID(
                "config and auto and forced and control"
            ) as String
            hyperStatSplitPointsUtil.addDefaultValueForPoint(autoForceOccupiedEnabledId, if (newAutoForceOccupyEnabled) 1.0 else 0.0)
            hyperStatSplitPointsUtil.addDefaultHisValueForPoint(autoForceOccupiedEnabledId,if (newAutoForceOccupyEnabled) 1.0 else 0.0)
        }
    }

    /**
     * Hyperstat Split Device configuration
     */
    // setup Device Relays
     fun setupDeviceRelays(
        isRelay1Enabled: Boolean, isRelay2Enabled: Boolean, isRelay3Enabled: Boolean,
        isRelay4Enabled: Boolean, isRelay5Enabled: Boolean, isRelay6Enabled: Boolean,
        isRelay7Enabled: Boolean, isRelay8Enabled: Boolean,
        masterPoints: HashMap<Any, String>, device: HyperStatSplitDevice
    ) {

        if (isRelay1Enabled) {
            device.relay1.pointRef = masterPoints[Port.RELAY_ONE]
            device.relay1.enabled = true
        }
        if (isRelay2Enabled) {
            device.relay2.pointRef = masterPoints[Port.RELAY_TWO]
            device.relay2.enabled = true
        }
        if (isRelay3Enabled) {
            device.relay3.pointRef = masterPoints[Port.RELAY_THREE]
            device.relay3.enabled = true
        }
        if (isRelay4Enabled) {
            device.relay4.pointRef = masterPoints[Port.RELAY_FOUR]
            device.relay4.enabled = true
        }
        if (isRelay5Enabled) {
            device.relay5.pointRef = masterPoints[Port.RELAY_FIVE]
            device.relay5.enabled = true
        }
        if (isRelay6Enabled) {
            device.relay6.pointRef = masterPoints[Port.RELAY_SIX]
            device.relay6.enabled = true
        }
        if (isRelay7Enabled) {
            device.relay7.pointRef = masterPoints[Port.RELAY_SEVEN]
            device.relay7.enabled = true
        }
        if (isRelay8Enabled) {
            device.relay8.pointRef = masterPoints[Port.RELAY_EIGHT]
            device.relay8.enabled = true
        }

    }

    // setup Device AnalogOut's
     fun setupDeviceAnalogOuts(
        isAnalogOut1Enabled: Boolean, analogOut1MinVoltage: Int, analogOut1MaxVoltage: Int,
        isAnalogOut2Enabled: Boolean, analogOut2MinVoltage: Int, analogOut2MaxVoltage: Int,
        isAnalogOut3Enabled: Boolean, analogOut3MinVoltage: Int, analogOut3MaxVoltage: Int,
        isAnalogOut4Enabled: Boolean, analogOut4MinVoltage: Int, analogOut4MaxVoltage: Int,
        masterPoints: HashMap<Any,String>, device: HyperStatSplitDevice) {


        if (isAnalogOut1Enabled) {
            device.analog1Out.pointRef = masterPoints[Port.ANALOG_OUT_ONE]
            device.analog1Out.enabled = true
            device.analog1Out.type = "${analogOut1MinVoltage}-${analogOut1MaxVoltage}v"
        }
        if (isAnalogOut2Enabled) {
            device.analog2Out.pointRef = masterPoints[Port.ANALOG_OUT_TWO]
            device.analog2Out.enabled = true
            device.analog2Out.type ="${analogOut2MinVoltage}-${analogOut2MaxVoltage}v"
        }
        if (isAnalogOut3Enabled) {
            device.analog3Out.pointRef = masterPoints[Port.ANALOG_OUT_THREE]
            device.analog3Out.enabled = true
            device.analog3Out.type ="${analogOut3MinVoltage}-${analogOut3MaxVoltage}v"
        }
        if (isAnalogOut4Enabled) {
            device.analog4Out.pointRef = masterPoints[Port.ANALOG_OUT_FOUR]
            device.analog4Out.enabled = true
            device.analog4Out.type ="${analogOut4MinVoltage}-${analogOut4MaxVoltage}v"
        }
    }

    // setup Device Universal In's
     fun setupDeviceUniversalIns(
        isUniversalIn1Enabled: Boolean, isUniversalIn1Association: String,
        isUniversalIn2Enabled: Boolean, isUniversalIn2Association: String,
        isUniversalIn3Enabled: Boolean, isUniversalIn3Association: String,
        isUniversalIn4Enabled: Boolean, isUniversalIn4Association: String,
        isUniversalIn5Enabled: Boolean, isUniversalIn5Association: String,
        isUniversalIn6Enabled: Boolean, isUniversalIn6Association: String,
        isUniversalIn7Enabled: Boolean, isUniversalIn7Association: String,
        isUniversalIn8Enabled: Boolean, isUniversalIn8Association: String,
        masterPoints: HashMap<Any, String>, device: HyperStatSplitDevice) {
        if (isUniversalIn1Enabled) {
            device.universal1In.pointRef = masterPoints[Port.UNIVERSAL_IN_ONE]
            device.universal1In.enabled = true
            device.universal1In.type = isUniversalIn1Association
        }
        if (isUniversalIn2Enabled) {
            device.universal2In.pointRef = masterPoints[Port.UNIVERSAL_IN_TWO]
            device.universal2In.enabled = true
            device.universal2In.type = isUniversalIn2Association
        }
        if (isUniversalIn3Enabled) {
            device.universal3In.pointRef = masterPoints[Port.UNIVERSAL_IN_THREE]
            device.universal3In.enabled = true
            device.universal3In.type = isUniversalIn3Association
        }
        if (isUniversalIn4Enabled) {
            device.universal4In.pointRef = masterPoints[Port.UNIVERSAL_IN_FOUR]
            device.universal4In.enabled = true
            device.universal4In.type = isUniversalIn4Association
        }
        if (isUniversalIn5Enabled) {
            device.universal5In.pointRef = masterPoints[Port.UNIVERSAL_IN_FIVE]
            device.universal5In.enabled = true
            device.universal5In.type = isUniversalIn5Association
        }
        if (isUniversalIn6Enabled) {
            device.universal6In.pointRef = masterPoints[Port.UNIVERSAL_IN_SIX]
            device.universal6In.enabled = true
            device.universal6In.type = isUniversalIn6Association
        }
        if (isUniversalIn7Enabled) {
            device.universal7In.pointRef = masterPoints[Port.UNIVERSAL_IN_SEVEN]
            device.universal7In.enabled = true
            device.universal7In.type = isUniversalIn7Association
        }
        if (isUniversalIn8Enabled) {
            device.universal8In.pointRef = masterPoints[Port.UNIVERSAL_IN_EIGHT]
            device.universal8In.enabled = true
            device.universal8In.type = isUniversalIn8Association
        }

    }

    // setup Device Sensor Bus
    fun setupDeviceSensorBus(
        isAddress0Enabled: Boolean, isAddress0Association: Int,
        isAddress1Enabled: Boolean, isAddress1Association: Int,
        isAddress2Enabled: Boolean, isAddress2Association: Int,
        isAddress3Enabled: Boolean, isAddress3Association: Int,
        masterPoints: HashMap<Any, String>, device: HyperStatSplitDevice
    ) {
        
        // Mixed Air Temp/Humidity is enabled
        if ((isAddress0Enabled && isAddress0Association == 0) ||
            (isAddress1Enabled && isAddress1Association == 0) ||
            (isAddress2Enabled && isAddress2Association == 0)) {
            device.mixedAirTempSensor.enabled = true
            device.mixedAirHumiditySensor.enabled = true
        } else {
            device.mixedAirTempSensor.enabled = false
            device.mixedAirHumiditySensor.enabled = false
        }

        // Supply Air Temp/Humidity is enabled
        if ((isAddress0Enabled && isAddress0Association == 1) ||
            (isAddress1Enabled && isAddress1Association == 1) ||
            (isAddress2Enabled && isAddress2Association == 1)) {
            device.supplyAirTempSensor.enabled = true
            device.supplyAirHumiditySensor.enabled = true
        } else {
            device.supplyAirTempSensor.enabled = false
            device.supplyAirHumiditySensor.enabled = false
        }

        // Outside Air Temp/Humidity is enabled
        if ((isAddress0Enabled && isAddress0Association == 2) ||
            (isAddress1Enabled && isAddress1Association == 2) ||
            (isAddress2Enabled && isAddress2Association == 2)) {
            device.outsideAirTempSensor.enabled = true
            device.outsideAirHumiditySensor.enabled = true
        } else {
            device.outsideAirTempSensor.enabled = false
            device.outsideAirHumiditySensor.enabled = false
        }

        if (isAddress3Enabled && isAddress3Association == 0) {
            device.ductStaticPressureSensor.enabled = true
        } else {
            device.ductStaticPressureSensor.enabled = false
        }

        device.addSensor(Port.SENSOR_MAT, masterPoints[Port.SENSOR_MAT], "\u00B0F", device.mixedAirTempSensor.enabled)
        device.mixedAirTempSensor.pointRef = masterPoints[Port.SENSOR_MAT]

        device.addSensor(Port.SENSOR_MAH, masterPoints[Port.SENSOR_MAH], "%RH", device.mixedAirHumiditySensor.enabled)
        device.mixedAirHumiditySensor.pointRef = masterPoints[Port.SENSOR_MAH]

        device.addSensor(Port.SENSOR_SAT, masterPoints[Port.SENSOR_SAT], "\u00B0F", device.supplyAirTempSensor.enabled)
        device.supplyAirTempSensor.pointRef = masterPoints[Port.SENSOR_SAT]

        device.addSensor(Port.SENSOR_SAH, masterPoints[Port.SENSOR_SAH], "%RH", device.supplyAirHumiditySensor.enabled)
        device.supplyAirHumiditySensor.pointRef = masterPoints[Port.SENSOR_SAH]

        device.addSensor(Port.SENSOR_OAT, masterPoints[Port.SENSOR_OAT], "\u00B0F", device.outsideAirTempSensor.enabled)
        device.outsideAirTempSensor.pointRef = masterPoints[Port.SENSOR_OAT]

        device.addSensor(Port.SENSOR_OAH, masterPoints[Port.SENSOR_OAH], "%RH", device.outsideAirHumiditySensor.enabled)
        device.outsideAirHumiditySensor.pointRef = masterPoints[Port.SENSOR_OAH]

        device.addSensor(Port.SENSOR_PRESSURE, masterPoints[Port.SENSOR_PRESSURE], "inH2O", device.ductStaticPressureSensor.enabled)
        device.ductStaticPressureSensor.pointRef = masterPoints[Port.SENSOR_PRESSURE]

    }

    /**
     * Function  which collects all the logical associated points
     */
    open fun getLogicalPointList(): HashMap<Any, String> {
       // override at profile level
        return HashMap()
    }

    private fun getPointBasicInfo(): PointBasicInfo {
        val siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE) as HashMap<Any, Any>
        val systemEquip = CCUHsApi.getInstance().readEntity("equip and system") as HashMap<Any, Any>

        return PointBasicInfo(
            siteMap[Tags.ID].toString(),
            systemEquip["id"].toString(),
            siteMap["tz"].toString(),
            siteMap["dis"].toString()
        )
    }

    // Function to check the changes in the point and do the update with new value
     fun updatePointValueChangeRequired(pointId: String, newValue: Double){
        val presentValue = CCUHsApi.getInstance().readDefaultValById(pointId)
        if(presentValue != newValue)
            hyperStatSplitPointsUtil.addDefaultValueForPoint(pointId,newValue)
    }
    fun putPointToMap(pointData: HashMap<Any, Any>, outputPointMap: HashMap<Int, String>, mapping: Int){
        if (pointData.isNotEmpty() && pointData.containsKey(Tags.ID))
            outputPointMap[mapping] = pointData[Tags.ID].toString()
    }
}