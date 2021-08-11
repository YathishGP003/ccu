package a75f.io.logic.bo.building.vrv

import a75f.io.api.haystack.*
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.heartbeat.HeartBeat
import a75f.io.logic.bo.haystack.device.HyperStatDevice

class VrvEquip(hsApi : CCUHsApi,
               addr: Short) {

    val hayStack = hsApi
    val nodeAddr = addr
    fun createEntities(
        config: VrvProfileConfiguration,
        roomRef: String,
        floorRef: String
    ) {
        val site: Site = hayStack.site ?: return

        createEquip(site, roomRef, floorRef)

        val vrvEquip = Equip.Builder().setHashMap(hayStack.read("equip and group == \"$nodeAddr\""))
            .build()
        checkNotNull(vrvEquip)

        createUserIntentPoints(vrvEquip, roomRef, floorRef)
        createSensorPoints(vrvEquip, roomRef, floorRef)
        createStatusPoints(vrvEquip, roomRef, floorRef)
        createConfigPoints(config, vrvEquip, roomRef, floorRef)

        hayStack.syncEntityWithPointWrite()
    }

    private fun createEquip(
        site: Site,
        roomRef: String,
        floorRef: String,
    ): String {

        val systemEquip = hayStack.read("equip and system")
        lateinit var ahuRef: String
        if (systemEquip.isNotEmpty()) ahuRef = systemEquip["id"].toString()

        val vrvEquip = Equip.Builder()
            .setSiteRef(site.id)
            .setDisplayName(site.displayName + "-VRV-" + nodeAddr)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setProfile(ProfileType.HYPERSTAT_VRV.name)
            //.setPriority(config.getPriority().name)
            .addMarker("equip").addMarker("hyperstat").addMarker("vrv").addMarker("zone")
            .setAhuRef(ahuRef)
            .setGroup(nodeAddr.toString())
            .setTz(site.tz)
            .setGroup(nodeAddr.toString())
        return hayStack.addEquip(vrvEquip.build())
    }

    private fun createUserIntentPoints(
        equip: Equip,
        roomRef: String,
        floorRef: String
    ) {

        val desiredTemp = Point.Builder()
            .setDisplayName(equip.displayName + "-desiredTemp")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("air")
            .addMarker("temp").addMarker("desired").addMarker("vrv").addMarker("average")
            .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
            .setGroup(nodeAddr.toString())
            .setUnit("\u00B0F")
            .setTz(equip.tz)
            .build()
        val desiredTempId = hayStack.addPoint(desiredTemp)
        hayStack.writeDefaultValById(desiredTempId, 72.0)
        hayStack.writeHisValById(desiredTempId, 72.0)

        val desiredTempCooling = Point.Builder()
            .setDisplayName(equip.displayName + "-desiredTempCooling")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("vrv")
            .addMarker("cooling")
            .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
            .setGroup(nodeAddr.toString())
            .setUnit("\u00B0F")
            .setTz(equip.tz)
            .build()
        val desiredTempCoolingId = hayStack.addPoint(desiredTempCooling)
        hayStack.writeDefaultValById(desiredTempCoolingId, 74.0)
        hayStack.writeHisValById(desiredTempCoolingId, 74.0)

        val desiredTempHeating = Point.Builder()
            .setDisplayName(equip.displayName + "-desiredTempHeating")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("vrv")
            .addMarker("heating")
            .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
            .setGroup(nodeAddr.toString())
            .setUnit("\u00B0F")
            .setTz(equip.tz)
            .build()
        val desiredTempHeatingId = hayStack.addPoint(desiredTempHeating)
        hayStack.writeDefaultValById(desiredTempHeatingId, 70.0)
        hayStack.writeHisValById(desiredTempHeatingId, 70.0)


        val operationMode = Point.Builder()
            .setDisplayName(equip.displayName + "-operationMode")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("operation").addMarker("mode").addMarker("vrv")
            .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
            .setGroup(nodeAddr.toString())
            .setEnums("Off,Fan,Heat,Cool,Auto")
            .setTz(equip.tz)
            .build()
        val operationModeId = hayStack.addPoint(operationMode)
        hayStack.writeDefaultValById(operationModeId, 0.0)
        hayStack.writeHisValById(operationModeId, 0.0)

        val fanSpeed = Point.Builder()
            .setDisplayName(equip.displayName + "-fanSpeed")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("fanSpeed").addMarker("vrv")
            .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
            .setGroup(nodeAddr.toString())
            .setEnums("Auto,Low,Medium,High")
            .setTz(equip.tz)
            .build()
        val fanSpeedId = hayStack.addPoint(fanSpeed)
        hayStack.writeDefaultValById(fanSpeedId, 0.0)
        hayStack.writeHisValById(fanSpeedId, 0.0)

        val airflowDirection = Point.Builder()
            .setDisplayName(equip.displayName + "-airflowDirection")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("airflow").addMarker("direction").addMarker("vrv")
            .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
            .setGroup(nodeAddr.toString())
            .setEnums("Position0,Position1,Position2,Position3,Position4,Swing,Auto")
            .setTz(equip.tz)
            .build()
        val airflowDirectionId = hayStack.addPoint(airflowDirection)
        hayStack.writeDefaultValById(airflowDirectionId, 0.0)
        hayStack.writeHisValById(airflowDirectionId, 0.0)

        val coolHeatOptionRight = Point.Builder()
            .setDisplayName(equip.displayName + "-coolHeatOptionRight")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("coolHeatOptionRight").addMarker("vrv")
            .addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
            .setGroup(nodeAddr.toString())
            .setEnums("D,C,A")
            .setTz(equip.tz)
            .build()
        val coolHeatOptionRightId = hayStack.addPoint(coolHeatOptionRight)
        hayStack.writeDefaultValById(coolHeatOptionRightId, 0.0)
        hayStack.writeHisValById(coolHeatOptionRightId, 0.0)

    }

    private fun createSensorPoints(
        equip: Equip,
        roomRef: String,
        floorRef: String
    ) {

        val currentTemp = Point.Builder()
            .setDisplayName(equip.displayName + "-currentTemp")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("vrv")
            .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current").addMarker("his")
            .addMarker("cur").addMarker("logical")
            .setGroup(nodeAddr.toString())
            .setUnit("\u00B0F")
            .setTz(equip.tz)
            .build()
        val ctID = hayStack.addPoint(currentTemp)
        hayStack.writeHisValById(ctID, 0.0)

        val humidity = Point.Builder()
            .setDisplayName(equip.displayName + "-humidity")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setHisInterpolate("cov")
            .addMarker("zone").addMarker("vrv")
            .addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("current").addMarker("his")
            .addMarker("cur").addMarker("logical")
            .setGroup(nodeAddr.toString())
            .setUnit("%")
            .setTz(equip.tz)
            .build()
        val humidityId = hayStack.addPoint(humidity)
        hayStack.writeHisValById(humidityId, 0.0)

        val occupancy = Point.Builder()
            .setDisplayName(equip.displayName + "-occupancy")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef).setHisInterpolate("cov")
            .addMarker("vrv").addMarker("occupancy").addMarker("mode").addMarker("zone").addMarker("his")
            .setEnums("unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing")
            .setGroup(nodeAddr.toString())
            .setTz(equip.tz)
            .build()
        val occupancyId = hayStack.addPoint(occupancy)
        hayStack.writeHisValById(occupancyId, 0.0)

        val heartBeatId = CCUHsApi.getInstance().addPoint(
            HeartBeat.getHeartBeatPoint(
                equip.displayName, equip.id,
                equip.siteRef, roomRef, floorRef, nodeAddr.toInt(), "vrv", equip.tz
            )
        )

        val device = HyperStatDevice(nodeAddr.toInt(), equip.siteRef, floorRef, roomRef, equip.id, "vrv")

        device.currentTemp.pointRef = ctID
        device.currentTemp.enabled = true

        device.addSensor(Port.SENSOR_RH, humidityId)
        device.addSensor(Port.SENSOR_OCCUPANCY, occupancyId)
        device.rssi.pointRef = heartBeatId
        device.rssi.enabled = true
        device.addPointsToDb()
    }

    private fun createStatusPoints(
        equip: Equip,
        roomRef: String,
        floorRef: String
    ) {
        /*val equipStatus = Point.Builder()
            .setDisplayName(equip.displayName + "-equipStatus")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef).setHisInterpolate("cov")
            .addMarker("status").addMarker("his").addMarker("vrv").addMarker("logical").addMarker("zone")
            .setGroup(nodeAddr.toString())
            .setEnums("deadband,cooling,heating,tempdead")
            .setTz(equip.tz)
            .build()
        val equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus)
        CCUHsApi.getInstance().writeHisValById(equipStatusId, 0.0)*/

        val equipStatusMessage = Point.Builder()
            .setDisplayName(equip.displayName + "-equipStatusMessage")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .addMarker("status").addMarker("message").addMarker("vrv").addMarker("writable").addMarker("logical")
            .addMarker("zone")
            .setGroup(nodeAddr.toString())
            .setTz(equip.tz)
            .setKind(Kind.STRING)
            .build()
        val equipStatusMessageId = hayStack.addPoint(equipStatusMessage)

        val equipScheduleStatus = Point.Builder()
            .setDisplayName(equip.displayName + "-equipScheduleStatus")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef).setHisInterpolate("cov")
            .addMarker("scheduleStatus").addMarker("logical").addMarker("vrv").addMarker("zone").addMarker("writable")
            .addMarker("his")
            .setGroup(nodeAddr.toString())
            .setTz(equip.tz)
            .setKind(Kind.STRING)
            .build()
        val equipScheduleStatusId = hayStack.addPoint(equipScheduleStatus)

        val equipScheduleType = Point.Builder()
            .setDisplayName(equip.displayName + "-scheduleType")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef).setHisInterpolate("cov")
            .addMarker("zone").addMarker("vrv").addMarker("scheduleType").addMarker("writable").addMarker("his")
            .setGroup(nodeAddr.toString())
            .setEnums("building,zone,named")
            .setTz(equip.tz)
            .build()
        val equipScheduleTypeId = hayStack.addPoint(equipScheduleType)
        hayStack.writeDefaultValById(equipScheduleTypeId, 0.0)
        hayStack.writeHisValById(equipScheduleTypeId, 0.0)
    }

    private fun createConfigPoints(
        config: VrvProfileConfiguration,
        equip: Equip,
        roomRef: String,
        floorRef: String
    ) {
        val temperatureOffset = Point.Builder()
            .setDisplayName(equip.displayName + "-temperatureOffset")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .addMarker("config").addMarker("vrv").addMarker("writable").addMarker("zone")
            .addMarker("temperature").addMarker("offset").addMarker("sp")
            .setGroup(nodeAddr.toString())
            .setUnit("\u00B0F")
            .setTz(equip.tz)
            .build()
        val temperatureOffsetId = hayStack.addPoint(temperatureOffset)
        hayStack.writeDefaultValById(temperatureOffsetId, config.temperatureOffset)

        val minHumiditySp = Point.Builder()
            .setDisplayName(equip.displayName + "-minHumiditySp")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .addMarker("config").addMarker("vrv").addMarker("writable").addMarker("zone")
            .addMarker("min").addMarker("humidity").addMarker("sp")
            .setGroup(nodeAddr.toString())
            .setUnit("\u00B0F")
            .setTz(equip.tz)
            .build()
        val minHumiditySpId = hayStack.addPoint(minHumiditySp)
        hayStack.writeDefaultValById(minHumiditySpId, config.minHumiditySp)

        val maxHumiditySp = Point.Builder()
            .setDisplayName(equip.displayName + "-maxHumiditySp")
            .setEquipRef(equip.id)
            .setSiteRef(equip.siteRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .addMarker("config").addMarker("vrv").addMarker("writable").addMarker("zone")
            .addMarker("max").addMarker("humidity").addMarker("sp")
            .setGroup(nodeAddr.toString())
            .setUnit("\u00B0F")
            .setTz(equip.tz)
            .build()
        val maxHumiditySpId = hayStack.addPoint(maxHumiditySp)
        hayStack.writeDefaultValById(maxHumiditySpId, config.maxHumiditySp)

    }

    fun getProfileConfiguration(): VrvProfileConfiguration {
        return VrvProfileConfiguration(
            getConfigNumVal("temperature and offset"),
            getConfigNumVal("min and humidity and sp"),
            getConfigNumVal("max and humidity and sp")
        )
    }

    fun update(config : VrvProfileConfiguration) {
        setConfigNumVal("temperature and offset", config.temperatureOffset)
        setConfigNumVal("min and humidity and sp", config.minHumiditySp)
        setConfigNumVal("max and humidity and sp", config.maxHumiditySp)

    }

    fun setConfigNumVal(tags: String, num: Double) {
        CCUHsApi.getInstance()
            .writeDefaultVal("point and zone and config and vrv and $tags and group == \"$nodeAddr\"", num)
    }

    fun getConfigNumVal(tags: String): Double {
        return CCUHsApi.getInstance()
            .readDefaultVal("point and zone and config and vrv and $tags and group == \"$nodeAddr\"")
    }
}