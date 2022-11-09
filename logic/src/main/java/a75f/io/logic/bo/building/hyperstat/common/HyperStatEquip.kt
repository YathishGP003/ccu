package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.HyperStatDevice

/**
 * Created by Manjunath K on 11-07-2022.
 */

open class HyperStatEquip {


    lateinit var profileType: ProfileType
    val basicInfo = getPointBasicInfo()
    var nodeAddress: Int = 0
    lateinit var hsHaystackUtil: HSHaystackUtil
    lateinit var hyperStatPointsUtil: HyperStatPointsUtil


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
        return HyperStatPointsUtil.createHyperStatEquipPoint(
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
        return HyperStatPointsUtil.createHyperStatEquipPoint(
            profileName, basicInfo.siteRef, roomRef, floorRef,
            priority, basicInfo.gatewayRef, basicInfo.timeZone,
            nodeAddress, equipDis, CCUHsApi.getInstance(), profileType, markers
        )
    }

    fun init(
        profileName: String, equipRef: String, floorRef: String,
        roomRef: String, nodeAddress: Int, equipDis: String
    ) {
        hyperStatPointsUtil = HyperStatPointsUtil(
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

        hsHaystackUtil = HSHaystackUtil(
            equipRef, CCUHsApi.getInstance()
        )

    }

    open fun updateConfiguration(configuration: BaseProfileConfiguration) {
        // update at profile specific
    }

    // Function to update AirFlowTempSensor configuration
    fun updateAirFlowTempSensorConfiguration(
        isOldEnableAirFlowTempSensor: Boolean,
        isNewEnableAirFlowTempSensor: Boolean
    ) {

        if (isOldEnableAirFlowTempSensor != isNewEnableAirFlowTempSensor) {
            val enabledId = hsHaystackUtil
                .readPointID("config and air and discharge and temp") as String
            hyperStatPointsUtil.addDefaultValueForPoint(
                enabledId, if (isNewEnableAirFlowTempSensor) 1.0 else 0.0
            )
            val logicalPointId: String? = hsHaystackUtil.readPointID(
                "air and cur and discharge and logical"
            )
            if (logicalPointId != null) {
                hsHaystackUtil.removePoint(logicalPointId)
            }
            if (isNewEnableAirFlowTempSensor) {
                val pointData: Point =  hyperStatPointsUtil.createAirflowTempSensor()
                val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)
                if (pointData.markers.contains("his")) {
                    hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
                }
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                DeviceUtil.setPointEnabled(nodeAddress, Port.TH1_IN.name, true)
                DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.TH1_IN.name, pointId)
            }
        }
    }


    // Function to update DoorWindowSensor configuration
     fun updateDoorWindowSensorTh2Configuration(
        isOldEnableDoorWindowSensor: Boolean,
        isNewEnableDoorWindowSensor: Boolean
    ){
        if (isOldEnableDoorWindowSensor != isNewEnableDoorWindowSensor) {
            val enabledId = hsHaystackUtil.readPointID(
                "config and window and enabled and not sensing"
            ) as String
            hyperStatPointsUtil.addDefaultValueForPoint(
                enabledId, if (isNewEnableDoorWindowSensor) 1.0 else 0.0
            )
            val logicalPointId = LogicalPointsUtil.readDoorWindowSensor(hsHaystackUtil.equipRef,1)
            if (logicalPointId.isNotEmpty()) {
                hsHaystackUtil.removePoint(logicalPointId[Tags.ID].toString())
            }
            if (isNewEnableDoorWindowSensor) {
                val pointData: Point = hyperStatPointsUtil.createPointForDoorWindowSensor(1)
                val pointId = hyperStatPointsUtil.addPointToHaystack(pointData)
                if (pointData.markers.contains("his")) {
                    hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, 0.0)
                }
                hyperStatPointsUtil.addDefaultValueForPoint(pointId, 0.0)
                DeviceUtil.setPointEnabled(nodeAddress, Port.TH2_IN.name, true)
                DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.TH2_IN.name, pointId)
                DeviceUtil.updatePhysicalPointType(
                    nodeAddress, Port.TH2_IN.name,
                    HyperStatAssociationUtil.getSensorNameByType(AnalogInAssociation.DOOR_WINDOW_SENSOR)
                )
            }
        }
    }

    fun updatePm25Values(
        oldPm2p5Threshold: Double, newPm2p5Threshold: Double,
        oldPm2p5Target: Double, newPm2p5Target: Double ){

        if (oldPm2p5Threshold != newPm2p5Threshold) {
            val pointId = hsHaystackUtil.readPointID("pm2p5 and threshold") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, newPm2p5Threshold)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, newPm2p5Threshold)
        }
        if (oldPm2p5Target != newPm2p5Target) {
            val pointId = hsHaystackUtil.readPointID("pm2p5 and target") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, newPm2p5Target)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, newPm2p5Target)
        }
    }

    fun updateVOCValues(
        oldVOCThreshold: Double, newVOCThreshold: Double,
        oldVOCTarget: Double, newVOCTarget: Double ){

        if (oldVOCThreshold != newVOCThreshold) {
            val pointId = hsHaystackUtil.readPointID("voc and threshold") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, newVOCThreshold)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, newVOCThreshold)
        }
        if (oldVOCTarget != newVOCTarget) {
            val pointId = hsHaystackUtil.readPointID("voc and target") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, newVOCTarget)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, newVOCTarget)
        }
    }

    fun updateCO2Values(
        oldCO2DamperOpeningRate: Double, newCO2DamperOpeningRate: Double,
        oldCO2Threshold: Double, newCO2Threshold: Double,
        oldCO2Target: Double, newCO2Target: Double){
        if (oldCO2DamperOpeningRate != newCO2DamperOpeningRate) {
            val pointId = hsHaystackUtil.readPointID("co2 and opening and rate") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, newCO2DamperOpeningRate)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, newCO2DamperOpeningRate)
        }
        if (oldCO2Threshold != newCO2Threshold) {
            val pointId = hsHaystackUtil.readPointID("co2 and threshold") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, newCO2Threshold)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, newCO2Threshold)
        }
        if (oldCO2Target != newCO2Target) {
            val pointId = hsHaystackUtil.readPointID("co2 and target") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, newCO2Target)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, newCO2Target)
        }
    }

    fun updateDeviceDisplayConfiguration(
        isDisplayHumidityEnabledOld: Boolean,isDisplayHumidityEnabledNew: Boolean,
        isDisplayVOCEnabledOld: Boolean,isDisplayVOCEnabledNew: Boolean,
        isDisplayP2p5EnabledOld: Boolean,isDisplayP2p5EnabledNew: Boolean,
        isDisplayCo2EnabledOld: Boolean,  isDisplayCo2EnabledNew: Boolean,
    ){
        if (isDisplayHumidityEnabledOld != isDisplayHumidityEnabledNew) {
            val pointId = hsHaystackUtil.readPointID("enabled and humidity") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, if(isDisplayHumidityEnabledNew) 1.0 else 0.0)
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayHumidityEnabledNew) 1.0 else 0.0)
        }
        if (isDisplayVOCEnabledOld != isDisplayVOCEnabledNew) {
            val pointId = hsHaystackUtil.readPointID("enabled and voc") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId,if(isDisplayVOCEnabledNew) 1.0 else 0.0 )
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayVOCEnabledNew) 1.0 else 0.0 )
        }
        if (isDisplayP2p5EnabledOld != isDisplayP2p5EnabledNew) {
            val pointId = hsHaystackUtil.readPointID("enabled and pm2p5") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId, if(isDisplayP2p5EnabledNew) 1.0 else 0.0 )
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayP2p5EnabledNew) 1.0 else 0.0 )
        }
        if (isDisplayCo2EnabledOld != isDisplayCo2EnabledNew) {
            val pointId = hsHaystackUtil.readPointID("enabled and co2") as String
            hyperStatPointsUtil.addDefaultValueForPoint(pointId,if(isDisplayCo2EnabledNew) 1.0 else 0.0  )
            hyperStatPointsUtil.addDefaultHisValueForPoint(pointId, if(isDisplayCo2EnabledNew) 1.0 else 0.0)
        }
    }





    fun updateTempOffset(oldValue: Double, newValue: Double){
        if (oldValue != newValue) {
            val tempOffId = hsHaystackUtil.readPointID("temp and offset") as String
            hyperStatPointsUtil.addDefaultValueForPoint(tempOffId, (newValue * 10))
        }
    }

    fun updateAutoAwayAutoForceOccupy(
        oldAutoAwayEnabled: Boolean, newAutoAwayEnabled: Boolean,
        oldAutoForceOccupyEnabled: Boolean, newAutoForceOccupyEnabled: Boolean, ){
        if (oldAutoAwayEnabled != newAutoAwayEnabled) {
            val autoAwayEnabledId = hsHaystackUtil.readPointID("config and auto and away") as String
            hyperStatPointsUtil.addDefaultValueForPoint(autoAwayEnabledId, if (newAutoAwayEnabled) 1.0 else 0.0 )
            hyperStatPointsUtil.addDefaultHisValueForPoint(autoAwayEnabledId,if (newAutoAwayEnabled) 1.0 else 0.0)
            hsHaystackUtil.reWriteOccupancy()
        }

        if (oldAutoForceOccupyEnabled != newAutoForceOccupyEnabled) {
            val autoForceOccupiedEnabledId = hsHaystackUtil.readPointID(
                "config and auto and forced and control"
            ) as String
            hyperStatPointsUtil.addDefaultValueForPoint(autoForceOccupiedEnabledId, if (newAutoForceOccupyEnabled) 1.0 else 0.0)
            hyperStatPointsUtil.addDefaultHisValueForPoint(autoForceOccupiedEnabledId,if (newAutoForceOccupyEnabled) 1.0 else 0.0)
        }
    }

    /**
     * Hyperstat Device configuration
     */
    // setup Device Relays
     fun setupDeviceRelays(
        isRelay1Enabled: Boolean, isRelay2Enabled: Boolean, isRelay3Enabled: Boolean,
        isRelay4Enabled: Boolean, isRelay5Enabled: Boolean, isRelay6Enabled: Boolean,
        masterPoints: HashMap<Any, String>, device: HyperStatDevice) {

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

    }

    // setup Device AnalogOut's
     fun setupDeviceAnalogOuts(
        isAnalogOut1Enabled: Boolean, analogOut1MinVoltage: Int, analogOut1MaxVoltage: Int,
        isAnalogOut2Enabled: Boolean, analogOut2MinVoltage: Int, analogOut2MaxVoltage: Int,
        isAnalogOut3Enabled: Boolean, analogOut3MinVoltage: Int, analogOut3MaxVoltage: Int,
        masterPoints: HashMap<Any,String>, device: HyperStatDevice) {


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
    }

    // setup Device AnalogIn's
     fun setupDeviceAnalogIns(
        isAnalogIn1Enabled: Boolean, isAnalogIn1Association: String,
        isAnalogIn2Enabled: Boolean, isAnalogIn2Association: String,
        masterPoints: HashMap<Any, String>, device: HyperStatDevice) {
        if (isAnalogIn1Enabled) {
            device.analog1In.pointRef = masterPoints[Port.ANALOG_IN_ONE]
            device.analog1In.enabled = true
            device.analog1In.type = isAnalogIn1Association
        }
        if (isAnalogIn2Enabled) {
            device.analog2In.pointRef = masterPoints[Port.ANALOG_IN_TWO]
            device.analog2In.enabled = true
            device.analog2In.type = isAnalogIn2Association
        }

    }

    // setup Device Thermistor's
     fun setupDeviceThermistors(
        isTh1Enabled: Boolean, th1AssociationString: String,
        isTh2Enabled: Boolean, th2AssociationString: String,
        masterPoints: HashMap<Any, String>,
        device: HyperStatDevice) {
        if (isTh1Enabled) {
            device.th1In.pointRef = masterPoints[Port.TH1_IN]
            device.th1In.enabled = true
            device.th1In.type = th1AssociationString
        }
        if (isTh2Enabled) {
            device.th2In.pointRef = masterPoints[Port.TH2_IN]
            device.th2In.enabled = true
            device.th2In.type = th2AssociationString
        }
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

}