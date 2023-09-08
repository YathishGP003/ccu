package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.api.haystack.*
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.AnalogOutState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconRelayAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.RelayState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusPressState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusTempState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.UniversalInState
import a75f.io.logic.bo.building.hyperstatsplit.util.*
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.tuners.TunerConstants
import android.util.Log
import java.util.*

/**
 * Created by Manjunath K for HyperStat on 30-07-2021.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */
class HyperStatSplitPointsUtil(
    private val profileName: String,
    private val equipRef: String,
    private val floorRef: String,
    private val roomRef: String,
    private val siteRef: String,
    private val tz: String,
    private val nodeAddress: String,
    private val equipDis: String,
    private val hayStackAPI: CCUHsApi


) {
    // Static References
    companion object {

        // function to create equip Point
        fun createHyperStatSplitEquipPoint(
            profileName: String, siteRef: String, roomRef: String, floorRef: String, priority: String,
            gatewayRef: String, tz: String, nodeAddress: String, equipDis: String, hayStackAPI: CCUHsApi,
            profileType: ProfileType
        ): String {
            val equipPoint = Equip.Builder()
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setProfile(profileType.name)
                .setPriority(priority)
                .addMarker(Tags.HYPERSTATSPLIT)
                .addMarker(Tags.EQUIP).addMarker(Tags.STANDALONE)
                .addMarker(Tags.ZONE).addMarker(profileName)
                .setGatewayRef(gatewayRef)
                .setTz(tz)
                .setGroup(nodeAddress)
                .setDisplayName(equipDis)

            return hayStackAPI.addEquip(equipPoint.build())
        }
        fun createHyperStatSplitEquipPoint(
            profileName: String, siteRef: String, roomRef: String, floorRef: String, priority: String,
            gatewayRef: String, tz: String, nodeAddress: String, equipDis: String, hayStackAPI: CCUHsApi,
            profileType: ProfileType, markers: Array<String>
        ): String {
            val equipPoint = Equip.Builder()
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setProfile(profileType.name)
                .setPriority(priority)
                .addMarker(Tags.HYPERSTATSPLIT)
                .addMarker(Tags.EQUIP).addMarker(Tags.ZONE)
                .addMarker(profileName).addMarker(Tags.STANDALONE)
                .setGatewayRef(gatewayRef)
                .setTz(tz)
                .setGroup(nodeAddress)
                .setDisplayName(equipDis)

            markers.forEach { equipPoint.addMarker(it) }

            return hayStackAPI.addEquip(equipPoint.build())
        }
    }

    // Creating new Point
    fun createHaystackPoint(
        displayName: String, markers: Array<String>
    ): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)

            .addMarker(profileName).addMarker(Tags.STANDALONE)

        // add specific markers
        markers.forEach { point.addMarker(it) }
        val toReturn : Point = point.build()
        return toReturn
    }

    private fun createHaystackPointWithEnums(
        displayName: String, markers: Array<String>, enums: String
    ): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)
            .setHisInterpolate(Tags.COV)
            .setEnums(enums)

            .addMarker(profileName).addMarker(Tags.STANDALONE)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithOnlyEnum(
        displayName: String, markers: Array<String>, enums: String
    ): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)
            .setEnums(enums).addMarker(Tags.STANDALONE)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithMinMaxIncUnit(
        displayName: String, markers: Array<String>, min: String, max: String, unit: String, shortDis: String
    ): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)
            .setMinVal(min)
            .setMaxVal(max)
            .setUnit(unit)
            .setHisInterpolate(Tags.COV)
            .setShortDis(shortDis)

            .addMarker(profileName).addMarker(Tags.STANDALONE)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithMinMaxIncUnitInc(
        displayName: String,
        markers: Array<String>,
        min: String,
        max: String,
        inc: String,
        unit: String,
        hisInterpolate: String
    ): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)
            .setMinVal(min)
            .setMaxVal(max)
            .setIncrementVal(inc)
            .setUnit(unit)
            .setHisInterpolate(hisInterpolate)

            .addMarker(profileName).addMarker(Tags.STANDALONE)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithHisInterPolate(
        displayName: String, markers: Array<String>
    ): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)
            .setHisInterpolate(Tags.COV)
            .setKind(Kind.STRING)

            .addMarker(profileName).addMarker(Tags.STANDALONE)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithUnit(
        displayName: String, markers: Array<String>, hisInterpolate: String?, unit: String
    ): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)
            .setUnit(unit)
            .addMarker(profileName).addMarker(Tags.STANDALONE)

        if(!hisInterpolate.isNullOrEmpty())
            point.setHisInterpolate(hisInterpolate)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }


    // Adding Point to Haystack and returns point id
    fun addPointToHaystack(point: Point): String {
        return if(point.id != null){
            hayStackAPI.updatePoint(point, point.id)
            point.id
        }else {
            val id:String = hayStackAPI.addPoint(point)
            //Log.d("CCU_HS_SYNC", id + ", " + point.displayName)
            id
        }
    }

    // function which writes the default value into haystack for the point
    fun addDefaultValueForPoint(pointId: String, defaultValue: Any) {
        if (defaultValue is Double)
            hayStackAPI.writeDefaultValById(pointId, defaultValue.toDouble())
        else
            hayStackAPI.writeDefaultValById(pointId, defaultValue.toString())
    }

    //function which writes his write data
    fun addDefaultHisValueForPoint(pointId: String, defaultValue: Any) {
        if (defaultValue is Double)
            hayStackAPI.writeHisValueByIdWithoutCOV(pointId, defaultValue.toDouble())
    }

    // Add Points to Haystack with Default Values
    fun addPointsListToHaystackWithDefaultValue(listOfAllPoints: Array<MutableList<Pair<Point, Any>>>) {
        listOfAllPoints.forEach { pointsList ->
            pointsList.forEach { actualPoint ->
                // Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "adding point " + actualPoint.first.displayName)
                // actualPoint.first will contains Point
                // actualPoint.second will contains Default Value
                // save the point and hold Id

                val pointId = addPointToHaystack(actualPoint.first)
                if(actualPoint.first.markers.contains(Tags.HIS)){
                    addDefaultHisValueForPoint(pointId, actualPoint.second)
                }
                if(actualPoint.first.markers.contains(Tags.WRITABLE)) {
                    addDefaultValueForPoint(pointId, actualPoint.second)
                }
            }
        }
    }

    // Add Points to Haystack & return points ID
    fun addPointsToHaystack(listOfAllPoints: MutableList<Triple<Point, Any, Any>>): HashMap<Any, String> {
        val pointsIdMap: HashMap<Any, String> = HashMap()
        listOfAllPoints.forEach {
            // it.first -> contains point
            // it.second -> contains key for point
            // it.second -> contains default value for point

            var pointId: String? = it.first.id

            if(it.first.id == null) {
                pointId = addPointToHaystack(it.first)
            }

            pointsIdMap[it.second] = pointId!!
            if (it.first.markers.contains(Tags.HIS)) {
                addDefaultHisValueForPoint(pointId, it.third)
            }
            if (it.first.markers.contains(Tags.WRITABLE)) {
                addDefaultValueForPoint(pointId, it.third)
            }
        }
        return pointsIdMap
    }

    // Points to hold loop output value for CPU algo
    // OAO points are created separately
     fun createConditioningLoopOutputPoints(isHpu: Boolean): MutableList<Pair<Point, Any>> {
        val loopOutputPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val coolingLoopOutputPointMarker = arrayOf(
            "cooling","zone", "cmd", "his", "logical", "loop","output","modulating","runtime"
        )
        val heatingLoopOutputPointMarker = arrayOf(
            "heating","zone", "cmd", "his", "logical", "loop","output","modulating","runtime"
        )
        val fanLoopOutputPointMarker = arrayOf(
            "fan","zone", "cmd", "his", "logical","loop","output","modulating","runtime"
        )

        val coolingLoopOutputPoint = createHaystackPointWithUnit(
            "$equipDis-coolingLoopOutput" ,
            coolingLoopOutputPointMarker,
            "cov",
            "%"
        )
        val heatingLoopOutputPoint = createHaystackPointWithUnit(
            "$equipDis-heatingLoopOutput",
            heatingLoopOutputPointMarker,
            "cov",
            "%"
        )
        val fanLoopOutputPoint = createHaystackPointWithUnit(
            "$equipDis-fanLoopOutput",
            fanLoopOutputPointMarker,
            "cov",
            "%"
        )


        loopOutputPointsList.add(Pair(coolingLoopOutputPoint, 0.0))
        loopOutputPointsList.add(Pair(heatingLoopOutputPoint, 0.0))
        loopOutputPointsList.add(Pair(fanLoopOutputPoint, 0.0))


            if(isHpu){
                val compressorLoopOutputPointMarker = arrayOf(
                    "compressor","zone", "cmd", "his", "logical","loop","output","modulating","runtime"
                )
                val compressorLoopOutputPoint = createHaystackPointWithUnit(
                    "$equipDis-compressorLoopOutput",
                    compressorLoopOutputPointMarker,
                    "cov",
                    "%"
                )
                loopOutputPointsList.add(Pair(compressorLoopOutputPoint, 0.0))
            }
        return loopOutputPointsList
    }

    /*
        Function to create Zone-Level OAO Points
        Same points and tuners as System-Level OAO Profile, just with a "zone" tag instead of "system".
        (Also, no Return Air CO2.)
     */
    fun createZoneOAOPoints(
        outsideDamperMinOpen: Double, exhaustFanStage1Threshold: Double,
        exhaustFanStage2Threshold: Double, exhaustFanHysteresis: Double
    ): MutableList<Pair<Point, Any>> {

        val zoneOAOPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val insideEnthalpyPointMarker = arrayOf(
            "oao", "inside", "enthalpy", "his", "sp", "cpu"
        )
        val insideEnthalpyPoint = createHaystackPointWithUnit(
            "$equipDis-insideEnthalpy",
            insideEnthalpyPointMarker,
            "cov",
            "BTU/lb"
        )
        zoneOAOPointsList.add(Pair(insideEnthalpyPoint, 0.0))

        val outsideEnthalpyPointMarker = arrayOf(
            "oao", "outside", "enthalpy", "his", "sp", "cpu"
        )
        val outsideEnthalpyPoint = createHaystackPointWithUnit(
            "$equipDis-outsideEnthalpy",
            outsideEnthalpyPointMarker,
            "cov",
            "BTU/lb"
        )
        zoneOAOPointsList.add(Pair(outsideEnthalpyPoint, 0.0))

        val economizingAvailablePointMarker = arrayOf(
            "oao", "economizing", "available", "his", "sp", "cpu"
        )
        val economizingAvailablePoint = createHaystackPointWithOnlyEnum(
            "$equipDis-economizingAvailable",
            economizingAvailablePointMarker,
            "$OFF,$ON"
        )
        zoneOAOPointsList.add(Pair(economizingAvailablePoint, 0.0))

        val dcvAvailablePointMarker = arrayOf(
            "oao", "dcv", "available", "his", "sp", "cpu"
        )
        val dcvAvailablePoint = createHaystackPointWithOnlyEnum(
            "$equipDis-dcvAvailable",
            dcvAvailablePointMarker,
            "$OFF,$ON"
        )
        zoneOAOPointsList.add(Pair(dcvAvailablePoint, 0.0))

        val matThrottlePointMarker = arrayOf(
            "oao", "mat", "available", "his", "sp", "cpu"
        )
        val matThrottlePoint = createHaystackPointWithOnlyEnum(
            "$equipDis-matThrottle",
            matThrottlePointMarker,
            "$OFF,$ON"
        )
        zoneOAOPointsList.add(Pair(matThrottlePoint, 0.0))

        val economizingLoopOutputPointMarker = arrayOf(
            "oao", "economizing", "loop", "output", "his", "sp", "cpu"
        )
        val economizingLoopOutputPoint = createHaystackPointWithUnit(
            "$equipDis-economizingLoopOutput",
            economizingLoopOutputPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(economizingLoopOutputPoint, 0.0))

        val outsideAirCalculatedMinDamperPointMarker = arrayOf(
            "oao", "outside", "air", "calculated", "min", "damper", "his", "sp", "cpu"
        )
        val outsideAirCalculatedMinDamperPoint = createHaystackPointWithUnit(
            "$equipDis-outsideAirCalculatedMinDamper",
            outsideAirCalculatedMinDamperPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(outsideAirCalculatedMinDamperPoint, 0.0))

        val outsideAirFinalLoopOutputPointMarker = arrayOf(
            "oao", "outside", "air", "final", "loop", "output", "his", "sp", "cpu"
        )
        val outsideAirFinalLoopOutputPoint = createHaystackPointWithUnit(
            "$equipDis-outsideAirFinalLoopOutput",
            outsideAirFinalLoopOutputPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(outsideAirFinalLoopOutputPoint, 0.0))

        val weatherOutsideTempPointMarker = arrayOf(
            "oao", "outsideWeather", "air", "temp", "sensor", "his", "cpu"
        )
        val weatherOutsideTempPoint = createHaystackPointWithUnit(
            "$equipDis-weatherOutsideTemp",
            weatherOutsideTempPointMarker,
            "cov",
            "\u00B0F"
        )
        zoneOAOPointsList.add(Pair(weatherOutsideTempPoint, 0.0))

        val weatherOutsideHumidityPointMarker = arrayOf(
            "oao", "outsideWeather", "air", "humidity", "sensor", "his", "cpu"
        )
        val weatherOutsideHumidityPoint = createHaystackPointWithUnit(
            "$equipDis-weatherOutsideHumidity",
            weatherOutsideHumidityPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(weatherOutsideHumidityPoint, 0.0))

        val outsideDamperMinOpenPointMarker = arrayOf(
            "config", "oao", "writable", "outside", "damper", "min", "open" , "sp", "cpu"
        )
        val outsideDamperMinOpenPoint = createHaystackPointWithUnit(
            "$equipDis-outsideDamperMinOpen",
            outsideDamperMinOpenPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(outsideDamperMinOpenPoint, outsideDamperMinOpen))

        val exhaustFanStage1ThresholdPointMarker = arrayOf(
            "config", "oao", "writable", "exhaust", "fan", "stage1", "threshold" , "sp", "cpu"
        )
        val exhaustFanStage1ThresholdPoint = createHaystackPointWithUnit(
            "$equipDis-exhaustFanStage1Threshold",
            exhaustFanStage1ThresholdPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(exhaustFanStage1ThresholdPoint, exhaustFanStage1Threshold))

        val exhaustFanStage2ThresholdPointMarker = arrayOf(
            "config", "oao", "writable", "exhaust", "fan", "stage2", "threshold" , "sp", "cpu"
        )
        val exhaustFanStage2ThresholdPoint = createHaystackPointWithUnit(
            "$equipDis-exhaustFanStage2Threshold",
            exhaustFanStage2ThresholdPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(exhaustFanStage2ThresholdPoint, exhaustFanStage2Threshold))

        val exhaustFanHysteresisPointMarker = arrayOf(
            "config", "oao", "writable", "exhaust", "fan", "hysteresis" , "sp", "cpu"
        )
        val exhaustFanHysteresisPoint = createHaystackPointWithUnit(
            "$equipDis-exhaustFanHysteresis",
            exhaustFanHysteresisPointMarker,
            "cov",
            "%"
        )
        zoneOAOPointsList.add(Pair(exhaustFanHysteresisPoint, exhaustFanHysteresis))

        return zoneOAOPointsList

    }

    /**
     * Functions which creates configuration enable/disable Points
     */
    fun createIsSensorBusEnabledConfigPoints(
        address0: ConfigState, address1: ConfigState,
        address2: ConfigState, address3: ConfigState,
        profileType: ProfileType
    ): MutableList<Pair<Point, Any>> {
        
        val configSensorBusPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val sensorBusEnum = "false,true"

        val sensorBusMarkers: MutableList<String> = LinkedList()
        sensorBusMarkers.addAll(
            arrayOf("config", "writable", "zone", "input", "sp", "sensorBus")
        )

        sensorBusMarkers.add("addr0")
        sensorBusMarkers.add("enabled")
        val sensorBusAddress0Point = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress0Enable",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            sensorBusEnum
        )
        sensorBusMarkers.add("association")
        sensorBusMarkers.remove("enabled")
        val sensorBusAddress0AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress0Association",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            getSensorBusTempConfigEnum(profileType)
        )
        sensorBusMarkers.remove("association")
        sensorBusMarkers.remove("addr0")

        sensorBusMarkers.add("addr1")
        sensorBusMarkers.add("enabled")
        val sensorBusAddress1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress1Enable",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            sensorBusEnum
        )
        sensorBusMarkers.add("association")
        sensorBusMarkers.remove("enabled")
        val sensorBusAddress1AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress1Association",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            getSensorBusTempConfigEnum(profileType)
        )
        sensorBusMarkers.remove("association")
        sensorBusMarkers.remove("addr1")


        sensorBusMarkers.add("addr2")
        sensorBusMarkers.add("enabled")
        val sensorBusAddress2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress2Enable",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            sensorBusEnum
        )
        sensorBusMarkers.add("association")
        sensorBusMarkers.remove("enabled")
        val sensorBusAddress2AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress2Association",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            getSensorBusTempConfigEnum(profileType)
        )
        sensorBusMarkers.remove("association")
        sensorBusMarkers.remove("addr2")

        sensorBusMarkers.add("addr3")
        sensorBusMarkers.add("enabled")
        val sensorBusAddress3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress3Enable",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            sensorBusEnum
        )
        sensorBusMarkers.add("association")
        sensorBusMarkers.remove("enabled")
        val sensorBusAddress3AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-sensorBusAddress3Association",
            sensorBusMarkers.stream().toArray { arrayOfNulls(it) },
            getSensorBusPressureConfigEnum(profileType)
        )
        sensorBusMarkers.remove("association")
        sensorBusMarkers.remove("addr3")

        // Enable disable point
        configSensorBusPointsList.add(Pair(sensorBusAddress0Point, if (address0.enabled) 1.0 else 0.0))
        configSensorBusPointsList.add(Pair(sensorBusAddress1Point, if (address1.enabled) 1.0 else 0.0))
        configSensorBusPointsList.add(Pair(sensorBusAddress2Point, if (address2.enabled) 1.0 else 0.0))
        configSensorBusPointsList.add(Pair(sensorBusAddress3Point, if (address3.enabled) 1.0 else 0.0))

        // Association  point
        configSensorBusPointsList.add(Pair(sensorBusAddress0AssociationPoint, address0.association))
        configSensorBusPointsList.add(Pair(sensorBusAddress1AssociationPoint, address1.association))
        configSensorBusPointsList.add(Pair(sensorBusAddress2AssociationPoint, address2.association))
        configSensorBusPointsList.add(Pair(sensorBusAddress3AssociationPoint, address3.association))

        return configSensorBusPointsList
        
    }
    
    // Function which creates Relay  config Points
    fun createIsRelayEnabledConfigPoints(
        relay1: ConfigState,relay2: ConfigState,relay3: ConfigState,
        relay4: ConfigState,relay5: ConfigState,relay6: ConfigState,
        relay7: ConfigState,relay8: ConfigState,
        profileType: ProfileType
    ): MutableList<Pair<Point, Any>> {

        // Hold list of Relay Points
        val configRelayPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        // Common Relay Markers
        val relayMarkers: MutableList<String> = LinkedList()
        relayMarkers.addAll(
            arrayOf("config", "writable", "zone", "output", "sp")
        )

        /**  Relay 1 config and association point */
        relayMarkers.add("relay1")
        relayMarkers.add("enabled")
        val relay1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay1OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )

        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay1AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay1OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )

        relayMarkers.remove("association")
        relayMarkers.remove("relay1")


        /**  Relay 2 config and association point */
        relayMarkers.add("relay2")
        relayMarkers.add("enabled")
        val relay2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay2OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )

        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay2AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay2OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay2")

        /**  Relay 3 config and association point */
        relayMarkers.add("relay3")
        relayMarkers.add("enabled")
        val relay3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay3OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay3AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay3OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay3")

        /**  Relay 4config and association point */
        relayMarkers.add("relay4")
        relayMarkers.add("enabled")
        val relay4Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay4OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay4AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay4OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay4")

        /**  Relay 5 config and association point */
        relayMarkers.add("relay5")
        relayMarkers.add("enabled")
        val relay5Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay5OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay5AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay5OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay5")

        /**  Relay 6 config and association point */
        relayMarkers.add("relay6")
        relayMarkers.add("enabled")
        val relay6Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay6OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay6AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay6OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay6")

        /**  Relay 7 config and association point */
        relayMarkers.add("relay7")
        relayMarkers.add("enabled")
        val relay7Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay7OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay7AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay7OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay7")

        /**  Relay 8 config and association point */
        relayMarkers.add("relay8")
        relayMarkers.add("enabled")
        val relay8Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay8OutputEnable",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay8AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay8OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay8")

        // Enable disable point
        configRelayPointsList.add(Pair(relay1Point, if (relay1.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay2Point, if (relay2.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay3Point, if (relay3.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay4Point, if (relay4.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay5Point, if (relay5.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay6Point, if (relay6.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay7Point, if (relay7.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay8Point, if (relay8.enabled) 1.0 else 0.0))

        // Association  point
        configRelayPointsList.add(Pair(relay1AssociationPoint, relay1.association))
        configRelayPointsList.add(Pair(relay2AssociationPoint, relay2.association))
        configRelayPointsList.add(Pair(relay3AssociationPoint, relay3.association))
        configRelayPointsList.add(Pair(relay4AssociationPoint, relay4.association))
        configRelayPointsList.add(Pair(relay5AssociationPoint, relay5.association))
        configRelayPointsList.add(Pair(relay6AssociationPoint, relay6.association))
        configRelayPointsList.add(Pair(relay7AssociationPoint, relay7.association))
        configRelayPointsList.add(Pair(relay8AssociationPoint, relay8.association))

        return configRelayPointsList
    }

    //Function which creates analog out config Point
    fun createIsAnalogOutEnabledConfigPoints(
        analogOut1: ConfigState,analogOut2: ConfigState,
        analogOut3: ConfigState,analogOut4: ConfigState,
        profileType: ProfileType
    ): MutableList<Pair<Point, Any>> {

        val configAnalogOutPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val analogMarkers: MutableList<String> = LinkedList()
        analogMarkers.addAll(
            arrayOf("config", "writable", "sp", "zone", "output")
        )

        /**  analog 1 config and association point */
        analogMarkers.add("analog1")
        analogMarkers.add("enabled")
        val analogOut1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut1Enable",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")
        val analogOut1AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut1Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getAnalogOutConfigEnum(profileType)
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog1")


        /**  analog 2 config and association point */
        analogMarkers.add("analog2")
        analogMarkers.add("enabled")
        val analogOut2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut2Enable",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")

        val analogOut2AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut2Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getAnalogOutConfigEnum(profileType)
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog2")


        /**  analog 3 config and association point */
        analogMarkers.add("analog3")
        analogMarkers.add("enabled")
        val analogOut3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut3Enable",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")

        val analogOut3AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut3Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getAnalogOutConfigEnum(profileType)
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog3")


        /**  analog 4 config and association point */
        analogMarkers.add("analog4")
        analogMarkers.add("enabled")
        val analogOut4Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut4Enable",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "false,true"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")

        val analogOut4AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut4Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getAnalogOutConfigEnum(profileType)
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog4")


        // Analog out Enable/Disable Points
        configAnalogOutPointsList.add( Pair( analogOut1Point, if (analogOut1.enabled) 1.0 else 0.0))
        configAnalogOutPointsList.add( Pair( analogOut2Point, if (analogOut2.enabled) 1.0 else 0.0))
        configAnalogOutPointsList.add( Pair( analogOut3Point, if (analogOut3.enabled) 1.0 else 0.0))
        configAnalogOutPointsList.add( Pair( analogOut4Point, if (analogOut4.enabled) 1.0 else 0.0))

        // Analog out Association Points
        configAnalogOutPointsList.add( Pair( analogOut1AssociationPoint, analogOut1.association))
        configAnalogOutPointsList.add( Pair( analogOut2AssociationPoint, analogOut2.association))
        configAnalogOutPointsList.add( Pair( analogOut3AssociationPoint, analogOut3.association))
        configAnalogOutPointsList.add( Pair( analogOut4AssociationPoint, analogOut4.association))

        return configAnalogOutPointsList
    }

    // Function which Creates Universal in config Points
    fun createIsUniversalInEnabledConfigPoints(
        universalIn1: ConfigState,universalIn2: ConfigState,
        universalIn3: ConfigState,universalIn4: ConfigState,
        universalIn5: ConfigState,universalIn6: ConfigState,
        universalIn7: ConfigState,universalIn8: ConfigState,
        profileType: ProfileType
    ): MutableList<Pair<Point, Any>> {

        val configUniversalInPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val universalInEnum = "false,true"

        val universalMarkers: MutableList<String> = LinkedList()
        universalMarkers.addAll(
            arrayOf("config", "writable", "zone", "sp", "input")
        )

        universalMarkers.add("universal1")
        universalMarkers.add("enabled")
        val universalIn1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn1Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn1AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn1Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal1")


        universalMarkers.add("universal2")
        universalMarkers.add("enabled")
        val universalIn2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn2Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn2AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn2Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal2")


        universalMarkers.add("universal3")
        universalMarkers.add("enabled")
        val universalIn3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn3Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn3AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn3Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal3")


        universalMarkers.add("universal4")
        universalMarkers.add("enabled")
        val universalIn4Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn4Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn4AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn4Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal4")


        universalMarkers.add("universal5")
        universalMarkers.add("enabled")
        val universalIn5Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn5Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn5AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn5Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal5")


        universalMarkers.add("universal6")
        universalMarkers.add("enabled")
        val universalIn6Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn6Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn6AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn6Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal6")


        universalMarkers.add("universal7")
        universalMarkers.add("enabled")
        val universalIn7Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn7Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn7AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn7Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal7")


        universalMarkers.add("universal8")
        universalMarkers.add("enabled")
        val universalIn8Point = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn8Enable",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            universalInEnum
        )
        universalMarkers.add("association")
        universalMarkers.remove("enabled")
        val universalIn8AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-universalIn8Association",
            universalMarkers.stream().toArray { arrayOfNulls(it) },
            getUniversalInConfigEnum(profileType)
        )
        universalMarkers.remove("association")
        universalMarkers.remove("universal8")
        

        // Universal In Enable/Disable Points
        configUniversalInPointsList.add(Pair(universalIn1Point, if (universalIn1.enabled) 1.0 else 0.0))
        configUniversalInPointsList.add(Pair(universalIn2Point, if (universalIn2.enabled) 1.0 else 0.0))
        configUniversalInPointsList.add(Pair(universalIn3Point, if (universalIn3.enabled) 1.0 else 0.0))
        configUniversalInPointsList.add(Pair(universalIn4Point, if (universalIn4.enabled) 1.0 else 0.0))
        configUniversalInPointsList.add(Pair(universalIn5Point, if (universalIn5.enabled) 1.0 else 0.0))
        configUniversalInPointsList.add(Pair(universalIn6Point, if (universalIn6.enabled) 1.0 else 0.0))
        configUniversalInPointsList.add(Pair(universalIn7Point, if (universalIn7.enabled) 1.0 else 0.0))
        configUniversalInPointsList.add(Pair(universalIn8Point, if (universalIn8.enabled) 1.0 else 0.0))

        // Universal In Association points
        configUniversalInPointsList.add( Pair(universalIn1AssociationPoint, universalIn1.association))
        configUniversalInPointsList.add( Pair(universalIn2AssociationPoint, universalIn2.association))
        configUniversalInPointsList.add( Pair(universalIn3AssociationPoint, universalIn3.association))
        configUniversalInPointsList.add( Pair(universalIn4AssociationPoint, universalIn4.association))
        configUniversalInPointsList.add( Pair(universalIn5AssociationPoint, universalIn5.association))
        configUniversalInPointsList.add( Pair(universalIn6AssociationPoint, universalIn6.association))
        configUniversalInPointsList.add( Pair(universalIn7AssociationPoint, universalIn7.association))
        configUniversalInPointsList.add( Pair(universalIn8AssociationPoint, universalIn8.association))

        return configUniversalInPointsList
    }

    // Function to create autoaway and auto force occupy point
    fun createAutoForceAutoAwayConfigPoints(
        isAutoAwayEnabled: Boolean,
        isAutoForcedOccupiedEnabled: Boolean

    ): MutableList<Pair<Point, Any>> {

        val autoForceAutoAwayConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val enableAutoForceOccupancyControlPointMarkers = arrayOf(
            "config", "writable", "zone", "auto", "occupancy", "enabled", "control", "forced","his","cmd"
        )
        val enableAutoAwayControlPointMarkers = arrayOf(
            "config", "writable", "zone", "auto", "away", "enabled", "control","his"
        )

        // isEnableAutoForceOccupied Point
        val enableAutoForceOccupancyControlPoint = createHaystackPointWithEnums(
            "$equipDis-autoForceOccupiedEnabled",
            enableAutoForceOccupancyControlPointMarkers,
            "off,on"
        )

        // isEnableAutoAway Point
        val enableAutoAwayControlPointPoint = createHaystackPointWithEnums(
            "$equipDis-autoawayEnabled",
            enableAutoAwayControlPointMarkers,
            "off,on"
        )

        autoForceAutoAwayConfigPointsList.add(
            Pair(
                enableAutoForceOccupancyControlPoint,
                if (isAutoForcedOccupiedEnabled) 1.0 else 0.0
            )
        )

        autoForceAutoAwayConfigPointsList.add(
            Pair(
                enableAutoAwayControlPointPoint,
                if (isAutoAwayEnabled) 1.0 else 0.0
            )
        )
        return autoForceAutoAwayConfigPointsList
    }

    // Function which creates co2 Points
    fun createPointCO2ConfigPoint(
        zoneCO2DamperOpeningRate: Double,
        zoneCO2Threshold: Double,
        zoneCO2Target: Double
    ): MutableList<Pair<Point, Any>> {

        val co2ConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val zoneCO2DamperOpeningRatePointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "damper", "opening","rate"
        )
        val zoneCO2ThresholdPointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "threshold", "sp","concentration"
        )
        val zoneCO2TargetPointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "target", "sp","concentration"
        )

        val zoneCO2DamperOpeningRatePointPoint = createHaystackPointWithHisInterPolate(
            "$equipDis-zoneCO2DamperOpeningRate",
            zoneCO2DamperOpeningRatePointMarkers
        )

        val zoneCO2ThresholdPointPoint = createHaystackPointWithUnit(
            "$equipDis-zoneCO2Threshold",
            zoneCO2ThresholdPointMarkers,
            "cov","ppm"
        )

        val zoneCO2TargetPointPoint = createHaystackPointWithUnit(
            "$equipDis-zoneCO2Target",
            zoneCO2TargetPointMarkers,
            "cov","ppm"
        )

        co2ConfigPointsList.add(
            Pair(zoneCO2DamperOpeningRatePointPoint, zoneCO2DamperOpeningRate)
        )
        co2ConfigPointsList.add(
            Pair(zoneCO2ThresholdPointPoint, zoneCO2Threshold)
        )
        co2ConfigPointsList.add(
            Pair(zoneCO2TargetPointPoint, zoneCO2Target)
        )

        return co2ConfigPointsList
    }


    // Function to create VOV and pm2.5 sensor points
    fun createPointVOCPmConfigPoint(
        equipDis: String,
        zoneVOCThreshold: Double,
        zoneVOCTarget: Double,
        zonePm2p5Target: Double
    ): MutableList<Pair<Point, Any>> {
        val pointsList: MutableList<Pair<Point, Any>> = LinkedList()
        //ppb
        val vocMarkers: MutableList<String> = LinkedList()
        val pm2p5Markers: MutableList<String> = LinkedList()

        vocMarkers.addAll(arrayOf(
            "zone","voc","his","config", "writable", "his","threshold","concentration","sp"))
        pm2p5Markers.addAll(arrayOf(
            "zone","pm2p5","his","config", "writable", "his","threshold","concentration","sp"))

        val zoneVOCThresholdPoint = createHaystackPointWithUnit(
            "$equipDis-zoneVOCThreshold",
            vocMarkers.stream().toArray { arrayOfNulls(it) },
            "cov","ppb"
        )
        vocMarkers.remove("threshold")
        vocMarkers.add("target")
        val zoneVOCTargetPoint = createHaystackPointWithUnit(
            "$equipDis-zoneVOCTarget",
            vocMarkers.stream().toArray { arrayOfNulls(it) },
            "cov","ppb"
        )

        pm2p5Markers.remove("threshold")
        pm2p5Markers.add("target")
        val zonePm2p5TargetPoint = createHaystackPointWithUnit(
            "$equipDis-zonePm2p5Target",
            pm2p5Markers.stream().toArray { arrayOfNulls(it) },
            "cov","ug/\u33A5"
        )


        pointsList.add(Pair(zoneVOCThresholdPoint, zoneVOCThreshold))
        pointsList.add(Pair(zoneVOCTargetPoint, zoneVOCTarget))

        pointsList.add(Pair(zonePm2p5TargetPoint, zonePm2p5Target))

        return pointsList
    }


        // Function which creates User Intent Points
    fun createUserIntentPoints(
            defaultFanMode: StandaloneFanStage,
            defaultConditioningMode: StandaloneConditioningMode
        ): MutableList<Pair<Point, Any>> {

        val userIntentPointList: MutableList<Pair<Point, Any>> = LinkedList()

        val fanOperationsModePointMarkers = arrayOf(
            "fan", "writable", "zone", "operation", "mode", "his","sp", "userIntent"
        )
        val conditioningModePointMarkers = arrayOf(
             "writable", "zone", "mode", "his", "conditioning","sp", "userIntent"
        )
        val dehumidifierPointMarkers = arrayOf(
            "userIntent", "writable", "zone", "dehumidifier", "sp", "his", "control", "target"
        )
        val humidifierPointMarkers = arrayOf(
            "userIntent", "writable", "zone", "humidifier", "sp", "his", "control", "target"
        )

        val fanOperationsModePointEnums = "off,auto,low,medium,high"
        val conditioningModePointEnums = "off,auto,heatonly,coolonly"

        val fanOperationsModePoint = createHaystackPointWithEnums(
            displayName = "$equipDis-FanOpMode",
            fanOperationsModePointMarkers,
            fanOperationsModePointEnums
        )

        val conditioningModePointPoint = createHaystackPointWithEnums(
            displayName = "$equipDis-ConditioningMode",
            conditioningModePointMarkers,
            conditioningModePointEnums
        )

        val targetDehumidifierPointPoint = createHaystackPointWithUnit(
            displayName = "$equipDis-targetDehumidifier",
            dehumidifierPointMarkers,
            "cov",
            "%"
        )
        val targetHumidifierPointPoint = createHaystackPointWithUnit(
            displayName = "$equipDis-targetHumidifier",
            humidifierPointMarkers,
            "cov",
            "%"
        )

        userIntentPointList.add(Pair(fanOperationsModePoint, defaultFanMode.ordinal.toDouble()))
        userIntentPointList.add(Pair(conditioningModePointPoint, defaultConditioningMode.ordinal.toDouble()))
        userIntentPointList.add(Pair(targetDehumidifierPointPoint, TunerConstants.STANDALONE_TARGET_DEHUMIDIFIER))
        userIntentPointList.add(Pair(targetHumidifierPointPoint, TunerConstants.STANDALONE_TARGET_HUMIDITY))

        return userIntentPointList
    }

    // Function creates logical points for CPU & Economiser profile
    // Addresses 0-2 are reserved for temp/humidity sensors, address 3 is reserved for DPS.
    fun createConfigSensorBusLogicalPoints(hyperStatSplitConfig: HyperStatSplitCpuEconConfiguration): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatSplitConfig.address0State.enabled) {

            val tempPointData: Point = sensorBusTempConfiguration(sensorBusState = hyperStatSplitConfig.address0State)
            val tempPort: Port = HyperStatSplitAssociationUtil.getTempPort(state = hyperStatSplitConfig.address0State)
            configLogicalPointsList.add(Triple(tempPointData, tempPort,0.0))

            val humidityPointData: Point = sensorBusHumidityConfiguration(sensorBusState = hyperStatSplitConfig.address0State)
            val humidityPort: Port = HyperStatSplitAssociationUtil.getHumidityPort(state = hyperStatSplitConfig.address0State)
            configLogicalPointsList.add(Triple(humidityPointData, humidityPort,0.0))
        }
        if (hyperStatSplitConfig.address1State.enabled) {
            val tempPointData: Point = sensorBusTempConfiguration(sensorBusState = hyperStatSplitConfig.address1State)
            val tempPort: Port = HyperStatSplitAssociationUtil.getTempPort(state = hyperStatSplitConfig.address1State)
            configLogicalPointsList.add(Triple(tempPointData, tempPort,0.0))

            val humidityPointData: Point = sensorBusHumidityConfiguration(sensorBusState = hyperStatSplitConfig.address1State)
            val humidityPort: Port = HyperStatSplitAssociationUtil.getHumidityPort(state = hyperStatSplitConfig.address1State)
            configLogicalPointsList.add(Triple(humidityPointData, humidityPort,0.0))
        }
        if (hyperStatSplitConfig.address2State.enabled) {
            val tempPointData: Point = sensorBusTempConfiguration(sensorBusState = hyperStatSplitConfig.address2State)
            val tempPort: Port = HyperStatSplitAssociationUtil.getTempPort(state = hyperStatSplitConfig.address2State)
            configLogicalPointsList.add(Triple(tempPointData, tempPort,0.0))

            val humidityPointData: Point = sensorBusHumidityConfiguration(sensorBusState = hyperStatSplitConfig.address2State)
            val humidityPort: Port = HyperStatSplitAssociationUtil.getHumidityPort(state = hyperStatSplitConfig.address2State)
            configLogicalPointsList.add(Triple(humidityPointData, humidityPort,0.0))
        }
        if (hyperStatSplitConfig.address3State.enabled) {
            val pressPointData: Point = sensorBusPressureConfiguration(sensorBusState = hyperStatSplitConfig.address3State)
            val pressPort: Port = HyperStatSplitAssociationUtil.getPressPort(state = hyperStatSplitConfig.address3State)
            configLogicalPointsList.add(Triple(pressPointData, pressPort, 0.0))
        }

        return configLogicalPointsList
    }


    // Function creates logical points for CPU & Economiser profile
    fun createConfigRelayLogicalPoints(hyperStatSplitConfig: HyperStatSplitCpuEconConfiguration): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatSplitConfig.relay1State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay1State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_ONE, 0.0))
        }
        if (hyperStatSplitConfig.relay2State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay2State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_TWO, 0.0))
        }
        if (hyperStatSplitConfig.relay3State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay3State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_THREE, 0.0))
        }
        if (hyperStatSplitConfig.relay4State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay4State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FOUR, 0.0))
        }
        if (hyperStatSplitConfig.relay5State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay5State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FIVE, 0.0))
        }
        if (hyperStatSplitConfig.relay6State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay6State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_SIX, 0.0))
        }
        if (hyperStatSplitConfig.relay7State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay7State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_SEVEN, 0.0))
        }
        if (hyperStatSplitConfig.relay8State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatSplitConfig.relay8State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_EIGHT, 0.0))
        }

        return configLogicalPointsList
    }

    fun relayConfiguration(relayState: RelayState): Point {

        //      Relay Can be Associated to these all states
        //      COOLING_STAGE_1,COOLING_STAGE_2,COOLING_STAGE_3,HEATING_STAGE_1,HEATING_STAGE_2,HEATING_STAGE_3
        //      FAN_LOW_SPEED,FAN_MEDIUM_SPEED,FAN_HIGH_SPEED,FAN_ENABLED,OCCUPIED_ENABLED,HUMIDIFIER,DEHUMIDIFIER
        //      EXHAUST_FAN_STAGE_1, EXHAUST_FAN_STAGE_2

        return when {
            HyperStatSplitAssociationUtil.isRelayAssociatedToCoolingStage(relayState = relayState) ->
                createCoolingStagesPoint(relayState = relayState)
            HyperStatSplitAssociationUtil.isRelayAssociatedToHeatingStage(relayState = relayState) ->
                createHeatingStagesPoint(relayState = relayState)
            HyperStatSplitAssociationUtil.isRelayAssociatedToFan(relayState = relayState) ->
                createFanStagesPoint(fanStage = getFanStageValueForCPUEcon(relayState))
            HyperStatSplitAssociationUtil.isRelayAssociatedToFanEnabled(relayState = relayState) ->
                LogicalPointsUtil.createPointForFanEnable(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            HyperStatSplitAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState = relayState) ->
                LogicalPointsUtil.createPointForOccupiedEnabled(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            HyperStatSplitAssociationUtil.isRelayAssociatedToHumidifier(relayState = relayState) ->
                LogicalPointsUtil.createPointForHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            HyperStatSplitAssociationUtil.isRelayAssociatedToDeHumidifier(relayState = relayState) ->
                LogicalPointsUtil.createPointForDeHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage1(relayState = relayState) ->
                LogicalPointsUtil.createPointForExhaustFanStage1(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            HyperStatSplitAssociationUtil.isRelayAssociatedToExhaustFanStage2(relayState = relayState) ->
                LogicalPointsUtil.createPointForExhaustFanStage2(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            else -> {
                Log.d("CCU_HS_SYNC", "Got to the default case for a relay point")
                Point.Builder().build()
            }
        }

    }

    /**
     * Functions which can map all the (CPU & Econ) Relay Ports
     */
    private fun createCoolingStagesPoint(relayState: RelayState): Point {
        when (relayState.association) {
            CpuEconRelayAssociation.COOLING_STAGE_1 -> {
                return LogicalPointsUtil.createCoolingStage1Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            }
            CpuEconRelayAssociation.COOLING_STAGE_2 -> {
                return LogicalPointsUtil.createCoolingStage2Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            }
            CpuEconRelayAssociation.COOLING_STAGE_3 -> {
                return LogicalPointsUtil.createCoolingStage3Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            }
            else -> {}
        }
        throw NullPointerException("Stages can not be null")
    }

    private fun createHeatingStagesPoint(relayState: RelayState): Point {
        when (relayState.association) {
            CpuEconRelayAssociation.HEATING_STAGE_1 -> {
                return  LogicalPointsUtil.createHeatingStage1Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            }
            CpuEconRelayAssociation.HEATING_STAGE_2 -> {
                return LogicalPointsUtil.createHeatingStage2Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            }
            CpuEconRelayAssociation.HEATING_STAGE_3 -> {
                return LogicalPointsUtil.createHeatingStage3Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            }
            else -> {}
        }
        throw NullPointerException("Stages can not be null")
    }

    private fun createFanStagesPoint(fanStage: Int): Point {
        when (fanStage) {
            1-> return LogicalPointsUtil.createFanLowPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz) // Low
            2-> return LogicalPointsUtil.createFanMediumPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz) // Medium
            3-> return LogicalPointsUtil.createFanHighPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz) // High
        }
        throw NullPointerException("Fan stage can not be null")
    }


    fun createConfigAnalogOutLogicalPoints(
        hyperStatSplitConfig: HyperStatSplitCpuEconConfiguration
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatSplitConfig.analogOut1State.enabled) {
            if (hyperStatSplitConfig.analogOut1State.association != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
                    analogOutState = hyperStatSplitConfig.analogOut1State,
                    analogTag = "analog1"
                )
                val minPoint = (pointData.second as Pair<*, *>)
                val maxPoint = (pointData.third as Pair<*, *>)

                configLogicalPointsList.add(
                    Triple(
                        pointData.first as Point,
                        Port.ANALOG_OUT_ONE,
                        0.0
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        minPoint.first as Point,
                        minPoint.second as Any,
                        hyperStatSplitConfig.analogOut1State.voltageAtMin
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        maxPoint.first as Point,
                        maxPoint.second as Any,
                        hyperStatSplitConfig.analogOut1State.voltageAtMax
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(
                        hyperStatSplitConfig.analogOut1State
                    ),
                    hyperStatSplitConfig.analogOut1State.perAtFanLow,
                    hyperStatSplitConfig.analogOut1State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut1State.perAtFanHigh,
                    "analog1", configLogicalPointsList
                )
            } else {
                val pointData: Triple<Point, Any?, Any?> = analogOutConfiguration()
                configLogicalPointsList.add(
                    Triple(
                        pointData.first,
                        Port.ANALOG_OUT_ONE,
                        0.0
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(hyperStatSplitConfig.analogOut1State),
                    hyperStatSplitConfig.analogOut1State.perAtFanLow,
                    hyperStatSplitConfig.analogOut1State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut1State.perAtFanHigh,
                    "analog1", configLogicalPointsList
                )
            }
        }

        if (hyperStatSplitConfig.analogOut2State.enabled) {
            if (hyperStatSplitConfig.analogOut2State.association != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
                    analogOutState = hyperStatSplitConfig.analogOut2State,
                    analogTag = "analog2"
                )
                val minPoint = (pointData.second as Pair<*, *>)
                val maxPoint = (pointData.third as Pair<*, *>)

                configLogicalPointsList.add(
                    Triple(
                        pointData.first as Point,
                        Port.ANALOG_OUT_TWO,
                        0.0
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        minPoint.first as Point,
                        minPoint.second as Any,
                        hyperStatSplitConfig.analogOut2State.voltageAtMin
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        maxPoint.first as Point,
                        maxPoint.second as Any,
                        hyperStatSplitConfig.analogOut2State.voltageAtMax
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(
                        hyperStatSplitConfig.analogOut2State
                    ),
                    hyperStatSplitConfig.analogOut2State.perAtFanLow,
                    hyperStatSplitConfig.analogOut2State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut2State.perAtFanHigh,
                    "analog2", configLogicalPointsList
                )
            } else {
                val pointData: Triple<Point, Any?, Any?> = analogOutConfiguration()
                configLogicalPointsList.add(
                    Triple(
                        pointData.first,
                        Port.ANALOG_OUT_TWO,
                        0.0
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(hyperStatSplitConfig.analogOut2State),
                    hyperStatSplitConfig.analogOut2State.perAtFanLow,
                    hyperStatSplitConfig.analogOut2State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut2State.perAtFanHigh,
                    "analog2", configLogicalPointsList
                )
            }
        }

        if (hyperStatSplitConfig.analogOut3State.enabled) {
            if (hyperStatSplitConfig.analogOut3State.association != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
                    analogOutState = hyperStatSplitConfig.analogOut3State,
                    analogTag = "analog3"
                )
                val minPoint = (pointData.second as Pair<*, *>)
                val maxPoint = (pointData.third as Pair<*, *>)

                configLogicalPointsList.add(
                    Triple(
                        pointData.first as Point,
                        Port.ANALOG_OUT_THREE,
                        0.0
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        minPoint.first as Point,
                        minPoint.second as Any,
                        hyperStatSplitConfig.analogOut3State.voltageAtMin
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        maxPoint.first as Point,
                        maxPoint.second as Any,
                        hyperStatSplitConfig.analogOut3State.voltageAtMax
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(
                        hyperStatSplitConfig.analogOut3State
                    ),
                    hyperStatSplitConfig.analogOut3State.perAtFanLow,
                    hyperStatSplitConfig.analogOut3State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut3State.perAtFanHigh,
                    "analog3", configLogicalPointsList
                )
            } else {
                val pointData: Triple<Point, Any?, Any?> = analogOutConfiguration()
                configLogicalPointsList.add(
                    Triple(
                        pointData.first,
                        Port.ANALOG_OUT_THREE,
                        0.0
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(hyperStatSplitConfig.analogOut3State),
                    hyperStatSplitConfig.analogOut3State.perAtFanLow,
                    hyperStatSplitConfig.analogOut3State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut3State.perAtFanHigh,
                    "analog3", configLogicalPointsList
                )
            }
        }

        if (hyperStatSplitConfig.analogOut4State.enabled) {
            if (hyperStatSplitConfig.analogOut4State.association != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
                    analogOutState = hyperStatSplitConfig.analogOut4State,
                    analogTag = "analog4"
                )
                val minPoint = (pointData.second as Pair<*, *>)
                val maxPoint = (pointData.third as Pair<*, *>)

                configLogicalPointsList.add(
                    Triple(
                        pointData.first as Point,
                        Port.ANALOG_OUT_FOUR,
                        0.0
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        minPoint.first as Point,
                        minPoint.second as Any,
                        hyperStatSplitConfig.analogOut4State.voltageAtMin
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        maxPoint.first as Point,
                        maxPoint.second as Any,
                        hyperStatSplitConfig.analogOut4State.voltageAtMax
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(
                        hyperStatSplitConfig.analogOut4State
                    ),
                    hyperStatSplitConfig.analogOut4State.perAtFanLow,
                    hyperStatSplitConfig.analogOut4State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut4State.perAtFanHigh,
                    "analog4", configLogicalPointsList
                )
            }  else {
                val pointData: Triple<Point, Any?, Any?> = analogOutConfiguration()
                configLogicalPointsList.add(
                    Triple(
                        pointData.first,
                        Port.ANALOG_OUT_FOUR,
                        0.0
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatSplitAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(hyperStatSplitConfig.analogOut4State),
                    hyperStatSplitConfig.analogOut4State.perAtFanLow,
                    hyperStatSplitConfig.analogOut4State.perAtFanMedium,
                    hyperStatSplitConfig.analogOut4State.perAtFanHigh,
                    "analog4", configLogicalPointsList
                )
            }
        }

        return configLogicalPointsList
    }

    fun analogOutConfiguration(): Triple<Point, Any?, Any?> {
        return Triple(
            LogicalPointsUtil.createAnalogOutPointForPredefinedFanSpeed(equipDis, siteRef, equipRef, roomRef, floorRef, tz, nodeAddress),
            null,
            null
        )
    }

    fun analogOutConfiguration(analogOutState: AnalogOutState, analogTag: String): Triple<Any, Any, Any> {
        //   AnalogOut can be Associated  to these all state
        //   COOLING, LINEAR_FAN_SPEED, HEATING, OAO_DAMPER, PREDEFINED_FAN_SPEED
        return when {
            HyperStatSplitAssociationUtil.isAnalogOutAssociatedToCooling(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "cooling",
                    markers = arrayOf("cooling")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForCooling(equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_COOLING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_COOLING),
                )
            }

            HyperStatSplitAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOut = analogOutState) -> {
                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "fanspeed",
                    markers = arrayOf("fan","speed")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForFanSpeed(equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_FAN_SPEED),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_FAN_SPEED),
                )
            }

            HyperStatSplitAssociationUtil.isAnalogOutAssociatedToHeating(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "heating",
                    markers = arrayOf("heating")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForHeating(equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_HEATING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_HEATING),
                )
            }

            HyperStatSplitAssociationUtil.isAnalogOutAssociatedToOaoDamper(analogOut = analogOutState) -> {

                /*
                    There are no "outsideDamperAtMinDrive" and
                    "outsideDamperAtMaxDrive" points in the CPU/Economizer profile. These
                    OAO Damper min/max points replace them.
                 */

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "oaodamper",
                    markers = arrayOf("oao","damper")
                )
                Triple(
                    LogicalPointsUtil.createAnalogOutPointForOaoDamper(equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_OAO_DAMPER),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_OAO_DAMPER),
                )
            }
            // Need to check how how handle this
            else -> {
                Triple(Point.Builder().build(), Point.Builder().build(), Point.Builder().build())
            }
        }
    }

    private fun createFanConfigForAnalogOut(
         isAnalogOutStateEnabled: Boolean,
         perAtFanLow: Double, perAtFanMedium: Double, perAtFanHigh: Double,
         analogTag: String,
         configLogicalPointsList: MutableList<Triple<Point, Any, Any>>
    ) {
        if (isAnalogOutStateEnabled) {
            val pointData: Triple<Point, Point, Point> = createFanLowMediumHighPoint(analogTag)
            configLogicalPointsList.add(
                Triple(pointData.first, LogicalKeyID.FAN_LOW, perAtFanLow)
            )
            configLogicalPointsList.add(
                Triple(pointData.second, LogicalKeyID.FAN_MEDIUM, perAtFanMedium)
            )
            configLogicalPointsList.add(
                Triple(pointData.third, LogicalKeyID.FAN_HIGH, perAtFanHigh)
            )
        }
    }


    private fun createMinMaxPointForAnalogOut(
        analogTag: String, associationType: String, markers: Array<String>?
    ): Pair<Any, Any> {

        val minMarker: MutableList<String> = LinkedList()
        val maxMarker: MutableList<String> = LinkedList()
        if (markers != null) {
            if(markers.isNotEmpty()) {
                markers.forEach { s ->
                    minMarker.add(s)
                    maxMarker.add(s)
                }
            }
        }
        minMarker.addAll(arrayOf(
                analogTag,"cmd","config", "writable", "min","zone", "output","actuator"
            ))
        maxMarker.addAll(arrayOf(
                analogTag,"cmd","config", "writable", "max","zone", "output","actuator"
            ))

        val minPoint = createHaystackPointWithMinMaxIncUnitInc(
            "$equipDis-$analogTag" + "Min" + associationType,
            minMarker.stream().toArray { arrayOfNulls(it) },
            "0.0", "10.0", "1", "V", "cov"
        )
        val maxPoint = createHaystackPointWithMinMaxIncUnitInc(
            "$equipDis-$analogTag" + "Max" + associationType,
            maxMarker.stream().toArray { arrayOfNulls(it) },
            "0.0", "10.0", "1", "V", "cov"
        )

        return Pair(minPoint, maxPoint)
    }


    fun createFanLowMediumHighPoint(
        analogTag: String
    ): Triple<Point, Point, Point> {
        val type = getAnalogString(analogTag)

        val lowMarker = arrayOf(
            analogTag, "writable", "zone", "cmd", "output", "config","fan","speed","low"
        )
        val mediumMarker = arrayOf(
            analogTag, "writable", "zone", "cmd", "output", "config","fan","speed","medium"
        )
        val highMarker = arrayOf(
            analogTag, "writable", "zone", "cmd", "output", "config","fan","speed","high"
        )

        val lowPoint = createHaystackPointWithMinMaxIncUnit(
            "$equipDis-$analogTag" + "FanLow",
            lowMarker,
            "0.0", "100.0", "%","Analog-Out$type at Fan Low"
        )
        val mediumPoint = createHaystackPointWithMinMaxIncUnit(
            "$equipDis-$analogTag" + "FanMedium",
            mediumMarker,
            "0.0", "100.0", "%","Analog-Out$type at Fan Medium"
        )
        val highPoint = createHaystackPointWithMinMaxIncUnit(
            "$equipDis-$analogTag" + "FanHigh",
            highMarker,
            "0.0", "100.0", "%","Analog-Out$type at Fan High"
        )

        return Triple(lowPoint, mediumPoint, highPoint)
    }

    fun createConfigUniversalInLogicalPoints(
        universalIn1State: UniversalInState,
        universalIn2State: UniversalInState,
        universalIn3State: UniversalInState,
        universalIn4State: UniversalInState,
        universalIn5State: UniversalInState,
        universalIn6State: UniversalInState,
        universalIn7State: UniversalInState,
        universalIn8State: UniversalInState,
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (universalIn1State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn1State,
                universalTag = "universal1"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_ONE, 0.0))
        }
        if (universalIn2State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn2State,
                universalTag = "universal2"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_TWO, 0.0))
        }
        if (universalIn3State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn3State,
                universalTag = "universal3"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_THREE, 0.0))
        }
        if (universalIn4State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn4State,
                universalTag = "universal4"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_FOUR, 0.0))
        }
        if (universalIn5State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn5State,
                universalTag = "universal5"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_FIVE, 0.0))
        }
        if (universalIn6State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn6State,
                universalTag = "universal6"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_SIX, 0.0))
        }
        if (universalIn7State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn7State,
                universalTag = "universal7"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_SEVEN, 0.0))
        }
        if (universalIn8State.enabled) {
            val pointData: Point = universalInConfiguration(
                universalInState = universalIn8State,
                universalTag = "universal8"
            )
            configLogicalPointsList.add(Triple(pointData, Port.UNIVERSAL_IN_EIGHT, 0.0))
        }

        return configLogicalPointsList
    }


    private fun getFanStageValueForCPUEcon(relayState: RelayState): Int{
        return when(relayState.association){
            CpuEconRelayAssociation.FAN_LOW_SPEED -> 1
            CpuEconRelayAssociation.FAN_MEDIUM_SPEED -> 2
            CpuEconRelayAssociation.FAN_HIGH_SPEED -> 3
            else -> { 0 } // No mapping found
        }

    }

    fun universalInConfiguration(universalInState: UniversalInState, universalTag: String): Point {
        return when {

            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToSupplyAirTemperature(universalInState)) -> {

                LogicalPointsUtil.createPointForSupplyAirTemperature(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToOutsideAirTemperature(universalInState)) -> {

                LogicalPointsUtil.createPointForOutsideAirTemperature(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToMixedAirTemperature(universalInState)) -> {

                LogicalPointsUtil.createPointForMixedAirTemperature(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToCondensateNC(universalInState)) -> {

                LogicalPointsUtil.createPointForCondensateNC(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToCondensateNO(universalInState)) -> {

                LogicalPointsUtil.createPointForCondensateNO(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToCurrentTX10(universalInState)) -> {

                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "10", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToCurrentTX20(universalInState)) -> {
                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "20", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER_20,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToCurrentTX50(universalInState)) -> {
                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "50", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER_50,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToCurrentTX100(universalInState)) -> {
                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "100", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER_100,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToCurrentTX150(universalInState)) -> {
                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "150", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER_150,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToDuctPressure1In(universalInState)) -> {
                LogicalPointsUtil.createPointForDuctPressure(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "1", inc = "0.01", unit = "inH₂O",LogicalPointsUtil.DuctPressureSensorType.DUCT_PRESSURE_0_1,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToDuctPressure2In(universalInState)) -> {
                LogicalPointsUtil.createPointForDuctPressure(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "2", inc = "0.01", unit = "inH₂O",LogicalPointsUtil.DuctPressureSensorType.DUCT_PRESSURE_0_2,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToFilterNC(universalInState)) -> {

                LogicalPointsUtil.createPointForFilterNC(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToFilterNO(universalInState)) -> {

                LogicalPointsUtil.createPointForFilterNO(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToGenericVoltage(universalInState)) -> {

                LogicalPointsUtil.createPointForGenericVoltage(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz, universalTag,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isUniversalInAssociatedToGenericResistance(universalInState)) -> {

                LogicalPointsUtil.createPointForGenericResistance(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz, universalTag,nodeAddress
                )
            }

            else -> {
                Point.Builder().build()
            }

        }
    }

    // Create Logical Temperature point for Temp/Humidity Sensor on Sensor Bus
    fun sensorBusTempConfiguration(sensorBusState: SensorBusTempState): Point {
        return when {

            (HyperStatSplitAssociationUtil.isSensorBusAddressAssociatedToSupplyAir(sensorBusState)) -> {
                LogicalPointsUtil.createPointForSupplyAirTemperature(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isSensorBusAddressAssociatedToMixedAir(sensorBusState)) -> {
                LogicalPointsUtil.createPointForMixedAirTemperature(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isSensorBusAddressAssociatedToOutsideAir(sensorBusState)) -> {
                LogicalPointsUtil.createPointForOutsideAirTemperature(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }

            else -> {
                Log.d("CCU_HS_SYNC", "Got to the default case for a sensor bus point")
                Point.Builder().build()
            }

        }
    }

    // Create Logical Humidity point for Temp/Humidity Sensor on Sensor Bus
    fun sensorBusHumidityConfiguration(sensorBusState: SensorBusTempState): Point {
        return when {

            (HyperStatSplitAssociationUtil.isSensorBusAddressAssociatedToSupplyAir(sensorBusState)) -> {
                LogicalPointsUtil.createPointForSupplyAirHumidity(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isSensorBusAddressAssociatedToMixedAir(sensorBusState)) -> {
                LogicalPointsUtil.createPointForMixedAirHumidity(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }
            (HyperStatSplitAssociationUtil.isSensorBusAddressAssociatedToOutsideAir(sensorBusState)) -> {
                LogicalPointsUtil.createPointForOutsideAirHumidity(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }

            else -> {
                Log.d("CCU_HS_SYNC", "Got to the default case for a sensor bus point")
                Point.Builder().build()
            }

        }
    }

    // Create Logical Pressure point for Pressure Sensor on Sensor Bus
    fun sensorBusPressureConfiguration(sensorBusState: SensorBusPressState): Point {
        return when {

            (HyperStatSplitAssociationUtil.isSensorBusAddressAssociatedToDuctPressure(sensorBusState)) -> {

                LogicalPointsUtil.createPointForDuctPressure(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,nodeAddress
                )
            }

            else -> {
                Log.d("CCU_HS_SYNC", "Got to the default case for a sensor bus pressure point")
                Point.Builder().build()
            }

        }
    }


    // Logical points for HyperLite onboard sensors
    fun createLogicalSensorPoints(): MutableList<Triple<Point, Any, Any>> {

        val logicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        val co2PointMarkers =
            arrayOf("zone", "air", "co2", "sensor", "his", "cur", "logical")
        val humidityPointMarkers =
            arrayOf("zone", "air", "humidity", "sensor", "his", "cur", "logical")
        val illuminanceSensorPointMarkers =
            arrayOf("zone", "illuminance", "sensor", "his", "cur", "logical")
        val occupancySensorPointMarkers =
            arrayOf("zone", "occupancy", "sensor", "his", "cur", "logical")

        val co2Point = createHaystackPointWithUnit(
            "$equipDis-zone" + Port.SENSOR_CO2.portSensor,
            co2PointMarkers,
            "cov",
            "ppm"
        )
        val humidityPoint = createHaystackPointWithUnit(
            "$equipDis-zone" + Port.SENSOR_RH.portSensor,
            humidityPointMarkers,
            "cov",
            "%"
        )
        val illuminancePoint = createHaystackPointWithUnit(
            "$equipDis-zone" + Port.SENSOR_ILLUMINANCE.portSensor,
            illuminanceSensorPointMarkers,
            "cov",
            "lux"
        )
        val occupancyPoint = createHaystackPointWithEnums(
            "$equipDis-"+ Port.SENSOR_OCCUPANCY.portSensor,
            occupancySensorPointMarkers,
            "off,on"
        )

        logicalPointsList.add(Triple(co2Point, Port.SENSOR_CO2, 0.0))
        logicalPointsList.add(Triple(occupancyPoint, Port.SENSOR_OCCUPANCY, 0.0))
        logicalPointsList.add(Triple(humidityPoint, Port.SENSOR_RH, 0.0))
        logicalPointsList.add(Triple(illuminancePoint, Port.SENSOR_ILLUMINANCE, 0.0))

        return logicalPointsList

    }

    // Function which create Current Temp Point
    fun createTemperatureOffSetPoint(tempOffSetValue: Double): String {

        val temperatureOffsetMarkers = arrayOf(
            Tags.CONFIG, Tags.WRITABLE, Tags.ZONE, Tags.TEMP, Tags.OFFSET, Tags.SP
        )

        val temperatureOffsetPoint = createHaystackPoint(
            displayName = "$equipDis-temperatureOffset",
            markers = temperatureOffsetMarkers
        )
        val temperatureOffsetPointId = addPointToHaystack(point = temperatureOffsetPoint)

        addDefaultValueForPoint(
            pointId = temperatureOffsetPointId,
            defaultValue = tempOffSetValue
        )
        return temperatureOffsetPointId
    }


    // Function to create device display configuration points
    fun createDeviceDisplayConfigurationPoints(
        isDisplayHumidityEnabled: Boolean,
        isDisplayVOCEnabled: Boolean,
        isDisplayP2p5Enabled: Boolean,
        isDisplayCo2Enabled: Boolean,
    ): MutableList<Pair<Point, Any>> {

        val deviceConfigurationPoints: MutableList<Pair<Point, Any>> = LinkedList()

        val humidityEnabled = arrayOf( Tags.CONFIG, Tags.WRITABLE, Tags.ZONE, Tags.SP, Tags.ENABLED, Tags.HUMIDITY)
        val vocEnabled = arrayOf( Tags.CONFIG, Tags.WRITABLE, Tags.ZONE, Tags.SP, Tags.ENABLED, Tags.VOC)
        val co2Enabled = arrayOf( Tags.CONFIG, Tags.WRITABLE, Tags.ZONE, Tags.SP, Tags.ENABLED, Tags.CO2)
        val pm25Enabled = arrayOf( Tags.CONFIG, Tags.WRITABLE, Tags.ZONE, Tags.SP,Tags.ENABLED ,Tags.PM2P5)

        val humidityEnabledPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-humidityDisplayEnabled" ,
            humidityEnabled,
            "$OFF,$ON"
        )
        val vocEnabledPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-vocDisplayEnabled",
            vocEnabled,
            "$OFF,$ON"
        )
        val co2EnabledPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-co2DisplayEnabled",
            co2Enabled,
            "$OFF,$ON"

        )
        val pm25EnabledPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-pm25DisplayEnabled",
            pm25Enabled,
            "$OFF,$ON"
        )

        deviceConfigurationPoints.add(Pair(humidityEnabledPoint, if(isDisplayHumidityEnabled)1.0 else 0.0))
        deviceConfigurationPoints.add(Pair(vocEnabledPoint, if(isDisplayVOCEnabled)1.0 else 0.0))
        deviceConfigurationPoints.add(Pair(co2EnabledPoint, if(isDisplayCo2Enabled)1.0 else 0.0))
        deviceConfigurationPoints.add(Pair(pm25EnabledPoint, if(isDisplayP2p5Enabled)1.0 else 0.0))
        return deviceConfigurationPoints
    }


    // function creates and temperature cur,desired Points
    fun temperatureScheduleLogicalPoints(): MutableList<Triple<Point, Any, Any>> {

        val logicalTempPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        val currentTempPointMarkers =
            arrayOf("zone", "air", "temp", "sensor", "cur", "his", "logical","current")
        val desiredTempCoolingPointMarkers =
            arrayOf("zone", "air", "temp", "desired", "logical", "sp", "writable", "userIntent", "cooling", "his")
        val desiredTempHeatingPointMarkers =
            arrayOf("zone", "air", "temp", "desired", "logical", "sp", "writable", "userIntent", "heating", "his")

        val equipStatusPointMarkers = arrayOf("zone", "status", "his")
        val equipStatusMessagePointMarkers = arrayOf("zone", "status", "his", "message", "writable")
        val scheduleStatusPointMarkers = arrayOf("zone", "scheduleStatus", "his", "message", "writable", "logical")
        val scheduleTypePointMarkers = arrayOf("zone", "scheduleType", "his", "writable", "logical")


        val desiredTempPointMarkers =
            arrayOf("zone", "air", "temp", "desired", "his", "logical", "average", "sp", "writable", "userIntent")
        val desiredTempPoint = createHaystackPointWithUnit(
            "$equipDis-desiredTemp",
            desiredTempPointMarkers,
            "cov",
            "\u00B0F"
        )


        val desiredTempCoolingPoint = createHaystackPointWithUnit(
            "$equipDis-desiredTempCooling",
            desiredTempCoolingPointMarkers,
            "cov",
            "\u00B0F"
        )
        val desiredTempHeatingPoint = createHaystackPointWithUnit(
            "$equipDis-desiredTempHeating",
            desiredTempHeatingPointMarkers,
            "cov",
            "\u00B0F"
        )

        val currentTempPoint = createHaystackPointWithUnit(
            "$equipDis-currentTemp",
            currentTempPointMarkers,
            "cov",
            "\u00B0F"
        )


        val equipStatusPoint = createHaystackPointWithEnums(
            "$equipDis-equipStatus",
            equipStatusPointMarkers,
            "deadband,cooling,heating,tempdead"
        )

        val equipStatusMessagePoint = createHaystackPointWithHisInterPolate(
            "$equipDis-equipStatusMessage",
            equipStatusMessagePointMarkers
        )

        val equipScheduleStatusPoint = createHaystackPointWithHisInterPolate(
            "$equipDis-equipScheduleStatus",
            scheduleStatusPointMarkers,
        )

        val equipScheduleTypePoint = createHaystackPointWithEnums(
            "$equipDis-scheduleType",
            scheduleTypePointMarkers,
            "building,zone,named"
        )

        logicalTempPointsList.add(Triple(desiredTempPoint, LogicalKeyID.DESIRED_TEMP, 72.0))
        logicalTempPointsList.add(Triple(desiredTempHeatingPoint, LogicalKeyID.DESIRED_TEMP_HEATING, 70.0))
        logicalTempPointsList.add(Triple(desiredTempCoolingPoint, LogicalKeyID.DESIRED_TEMP_COOLING, 74.0))
        logicalTempPointsList.add(Triple(currentTempPoint, LogicalKeyID.CURRENT_TEMP, 0.0))
        logicalTempPointsList.add(Triple(equipStatusPoint, LogicalKeyID.EQUIP_STATUS, 0.0))
        logicalTempPointsList.add(Triple(equipStatusMessagePoint, LogicalKeyID.EQUIP_STATUS_MESSAGE, "OFF"))
        logicalTempPointsList.add(Triple(equipScheduleStatusPoint, LogicalKeyID.EQUIP_SCHEDULE_STATUS, ""))
        logicalTempPointsList.add(Triple(equipScheduleTypePoint, LogicalKeyID.SCHEDULE_TYPE, 1.0))

        return logicalTempPointsList
    }

    fun addProfilePoints() {
        val occupancyDetectionMarkers = arrayOf (
            Tags.OCCUPANCY, Tags.HIS, Tags.SP, Tags.ZONE,
            Tags.DETECTION,Tags.AUTO,Tags.AWAY,Tags.WRITABLE
        )
        val occupancyMarkers = arrayOf (
            Tags.OCCUPANCY, Tags.MODE,  Tags.HIS,
            Tags.SP,  Tags.ZONE,Tags.WRITABLE
        )
        val operatingModeMarkers = arrayOf (
            Tags.MODE, Tags.HIS, Tags.SP,
            Tags.ZONE,Tags.OPERATING,Tags.WRITABLE
        )

        val operatingModePointEnums = "${OFF},${COOLING},${HEATING},${TEMPDEAD}"

        val occupancyEnum = Occupancy.getEnumStringDefinition()

        val occupancyDetection = createHaystackPointWithEnums(
            displayName = "$equipDis-autoawayOccupancyDetection",
            markers = occupancyDetectionMarkers,
            enums = "${FALSE},${TRUE}"
        )
        val occupancyModePoint = createHaystackPointWithEnums(
            displayName = "$equipDis-zoneOccupancy",
            markers = occupancyMarkers,
            enums = occupancyEnum
        )
        val operatingModeModePoint = createHaystackPointWithEnums(
            displayName = "$equipDis-OperatingMode",
            markers = operatingModeMarkers,
            enums = operatingModePointEnums
        )

         addPointToHaystack(point = occupancyModePoint)
        val occupancyDetectionPointId = addPointToHaystack(point = occupancyDetection)
        val operatingModeModePointId = addPointToHaystack(point = operatingModeModePoint)

        addDefaultHisValueForPoint(operatingModeModePointId, 1.0)
        addDefaultHisValueForPoint(occupancyDetectionPointId, 0.0)

    }


    // Function which finds default fan speed
     fun getCPUEconDefaultFanSpeed(config: HyperStatSplitCpuEconConfiguration): StandaloneFanStage {

        return if (HyperStatSplitAssociationUtil.isAnyAnalogOutEnabledAssociatedToFanSpeed(config)
            || HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToFan(config) || HyperStatSplitAssociationUtil.isAnyAnalogOutMappedToStagedFan(config)){
             StandaloneFanStage.AUTO
        }
        else StandaloneFanStage.OFF
    }

    // CPU Econ Function which finds default default conditioning mode
     fun getCPUEconDefaultConditioningMode(config: HyperStatSplitCpuEconConfiguration): StandaloneConditioningMode {

        return if((HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToCooling(config)
                    || HyperStatSplitAssociationUtil.isAnyAnalogOutEnabledAssociatedToCooling(config))
                    && (HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToHeating(config)
                    || HyperStatSplitAssociationUtil.isAnyAnalogOutEnabledAssociatedToHeating(config))) {
            StandaloneConditioningMode.AUTO
        }
        else if(HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToCooling(config)
            ||HyperStatSplitAssociationUtil.isAnyAnalogOutEnabledAssociatedToCooling(config)) {
            StandaloneConditioningMode.COOL_ONLY
        }
        else if(HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToHeating(config)
            ||HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToCooling(config)) {
            StandaloneConditioningMode.HEAT_ONLY
        }
        else StandaloneConditioningMode.OFF
    }



    private fun getRelayConfigEnum(profileType: ProfileType): String {

        when(profileType){
            ProfileType.HYPERSTATSPLIT_CPU-> {
                return "$COOLING_STAGE1,$COOLING_STAGE2,$COOLING_STAGE3,$HEATING_STAGE1," +
                        "$HEATING_STAGE2,$HEATING_STAGE3,$FAN_LOW,$FAN_MEDIUM,$FAN_HIGH," +
                        "$FAN_ENABLED,$OCCUPIED_ENABLED,$HUMIDIFIER,$DEHUMIDIFIER," +
                        "$EXHAUST_FAN_STAGE1,$EXHAUST_FAN_STAGE2"
            }

            else -> { }
        }
        return ""
    }
    private fun getAnalogOutConfigEnum(profileType: ProfileType): String {
        when(profileType) {
            ProfileType.HYPERSTATSPLIT_CPU -> {
                return "$COOLING,$LINEAR_FAN_SPEED,$HEATING,$OAO_DAMPER,$STAGED_FAN_SPEED"
            }
            else -> {}
        }
        return ""
    }
    private fun getAnalogString(analogTag: String): Int{
        when(analogTag){
            Tags.ANALOG1-> return 1
            Tags.ANALOG2-> return 2
            Tags.ANALOG3-> return 3
            Tags.ANALOG4-> return 4
        }
        return 0
    }
    private fun getUniversalInConfigEnum(profileType: ProfileType): String {
        when(profileType) {
            ProfileType.HYPERSTATSPLIT_CPU -> {
                return "$CURRENT_10,$CURRENT_20,$CURRENT_50,$CURRENT_100,$CURRENT_150," +
                        "$SUPPLY_AIR_TEMP,$MIXED_AIR_TEMP,$OUTSIDE_AIR_TEMP,$FILTER_NC," +
                        "$FILTER_NO,$CONDENSATE_NC,$CONDENSATE_NO,$PRESSURE_1,$PRESSURE_2,"+
                        "$GENERIC_VOLTAGE,$GENERIC_RESISTANCE"
            }
            else -> {}
        }
        return ""
    }
    private fun getSensorBusTempConfigEnum(profileType: ProfileType): String {
        when(profileType) {
            ProfileType.HYPERSTATSPLIT_CPU -> {
                return "$SUPPLY_AIR,$MIXED_AIR,$OUTSIDE_AIR"
            }
            else -> {}
        }
        return ""
    }
    private fun getSensorBusPressureConfigEnum(profileType: ProfileType): String {
        when(profileType) {
            ProfileType.HYPERSTATSPLIT_CPU -> {
                return "$PRESSURE"
            }
            else -> {}
        }
        return ""
    }


    /***   Reading logical points  **/
    fun getCpuEconRelayLogicalPoint(association: CpuEconRelayAssociation): Point{
        return when(association){
            CpuEconRelayAssociation.COOLING_STAGE_1-> Point.Builder().setHashMap(LogicalPointsUtil.readCoolingStage1RelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.COOLING_STAGE_2-> Point.Builder().setHashMap(LogicalPointsUtil.readCoolingStage2RelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.COOLING_STAGE_3-> Point.Builder().setHashMap(LogicalPointsUtil.readCoolingStage3RelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.HEATING_STAGE_1-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingStage1RelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.HEATING_STAGE_2-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingStage2RelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.HEATING_STAGE_3-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingStage3RelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.FAN_LOW_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanLowRelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.FAN_MEDIUM_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanMediumRelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.FAN_HIGH_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanHighRelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.FAN_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanEnabledRelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.OCCUPIED_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readOccupiedEnabledRelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.HUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readHumidifierRelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.DEHUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readDeHumidifierRelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.EXHAUST_FAN_STAGE_1-> Point.Builder().setHashMap(LogicalPointsUtil.readExhaustFanStage1RelayLogicalPoint(equipRef)).build()
            CpuEconRelayAssociation.EXHAUST_FAN_STAGE_2-> Point.Builder().setHashMap(LogicalPointsUtil.readExhaustFanStage2RelayLogicalPoint(equipRef)).build()
        }
    }
    fun getCpuEconAnalogOutLogicalPoint(association: CpuEconAnalogOutAssociation): Point {
        return when(association){
            CpuEconAnalogOutAssociation.COOLING-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogCoolingLogicalPoint(equipRef)).build()
            CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutFanSpeedLogicalPoint(equipRef)).build()
            CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef)).build()
            CpuEconAnalogOutAssociation.HEATING-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogHeatingLogicalPoint(equipRef)).build()
            CpuEconAnalogOutAssociation.OAO_DAMPER-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutOaoLogicalPoint(equipRef)).build()
        }
    }

    fun createStagedFanConfigPoint(
        HyperStatSplitConfig: HyperStatSplitCpuEconConfiguration,
    ): MutableList<Pair<Point, Any>> {

        val stagedFanConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(HyperStatSplitConfig, CpuEconRelayAssociation.COOLING_STAGE_1)) {
            val coolingStage1FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "cooling", "rate", "output", "sp", "stage1"
            )
            val coolingStage1FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutCoolingStage1",
                coolingStage1FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(coolingStage1FanConfigPoint, HyperStatSplitConfig.coolingStage1FanState)
            )
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(HyperStatSplitConfig, CpuEconRelayAssociation.COOLING_STAGE_2)) {
            val coolingStage2FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "cooling", "rate", "output", "sp", "stage2"
            )
            val coolingStage2FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutCoolingStage2",
                coolingStage2FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(coolingStage2FanConfigPoint, HyperStatSplitConfig.coolingStage2FanState)
            )
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(HyperStatSplitConfig, CpuEconRelayAssociation.COOLING_STAGE_3)) {
            val coolingStage3FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "cooling", "rate", "output", "sp", "stage3"
            )
            val coolingStage3FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutCoolingStage3",
                coolingStage3FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(coolingStage3FanConfigPoint, HyperStatSplitConfig.coolingStage3FanState)
            )
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(HyperStatSplitConfig, CpuEconRelayAssociation.HEATING_STAGE_1)) {
            val heatingStage1FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "heating", "rate", "output", "sp", "stage1"
            )
            val heatingStage1FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutHeatingStage1",
                heatingStage1FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(heatingStage1FanConfigPoint, HyperStatSplitConfig.heatingStage1FanState)
            )
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(HyperStatSplitConfig, CpuEconRelayAssociation.HEATING_STAGE_2)) {
            val heatingStage2FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "heating", "rate", "output", "sp", "stage2"
            )
            val heatingStage2FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutHeatingStage2",
                heatingStage2FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(heatingStage2FanConfigPoint, HyperStatSplitConfig.heatingStage2FanState)
            )
        }

        if (HyperStatSplitAssociationUtil.isStagedFanEnabled(HyperStatSplitConfig, CpuEconRelayAssociation.HEATING_STAGE_3)) {
            val heatingStage3FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "heating", "rate", "output", "sp", "stage3"
            )
            val heatingStage3FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutHeatingStage3",
                heatingStage3FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(heatingStage3FanConfigPoint, HyperStatSplitConfig.heatingStage3FanState)
            )
        }

        for (pair in stagedFanConfigPointsList) {
            val point = pair.first
            val value = pair.second
            Log.d("TAG",
                "createStagedFanConfigPoint: config points created are $point and value $value and id is " + point.id
            )
        }
        return stagedFanConfigPointsList
    }

    fun createStagedFanPoint(
        newConfiguration: HyperStatSplitCpuEconConfiguration,
        stage: CpuEconRelayAssociation
    ): MutableList<Pair<Point, Any>> {
        val stagedFanConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        when (stage) {
            CpuEconRelayAssociation.COOLING_STAGE_1 -> {
                val coolingStage1FanConfigPoint = createFanConfigPoint(
                    "$equipDis-fanOutCoolingStage1",
                    arrayOf("config", "writable", "zone", "fan", "cooling","output", "sp", "stage1"),
                    newConfiguration.coolingStage1FanState
                )
                stagedFanConfigPointsList.add(coolingStage1FanConfigPoint)
            }
            CpuEconRelayAssociation.COOLING_STAGE_2 -> {
                val coolingStage2FanConfigPoint = createFanConfigPoint(
                    "$equipDis-fanOutCoolingStage2",
                    arrayOf("config", "writable", "zone", "fan", "cooling","output", "sp", "stage2"),
                    newConfiguration.coolingStage2FanState
                )
                stagedFanConfigPointsList.add(coolingStage2FanConfigPoint)
            }
            CpuEconRelayAssociation.COOLING_STAGE_3 -> {
                val coolingStage3FanConfigPoint = createFanConfigPoint(
                    "$equipDis-fanOutCoolingStage3",
                    arrayOf("config", "writable", "zone", "fan", "cooling","output", "sp", "stage3"),
                    newConfiguration.coolingStage3FanState
                )
                stagedFanConfigPointsList.add(coolingStage3FanConfigPoint)
            }
            CpuEconRelayAssociation.HEATING_STAGE_1 -> {
                val heatingStage1FanConfigPoint = createFanConfigPoint(
                    "$equipDis-fanOutHeatingStage1",
                    arrayOf("config", "writable", "zone", "fan", "heating", "output", "sp", "stage1"),
                    newConfiguration.heatingStage1FanState
                )
                stagedFanConfigPointsList.add(heatingStage1FanConfigPoint)
            }
            CpuEconRelayAssociation.HEATING_STAGE_2 -> {
                val heatingStage2FanConfigPoint = createFanConfigPoint(
                    "$equipDis-fanOutHeatingStage2",
                    arrayOf("config", "writable", "zone", "fan", "heating", "output", "sp", "stage2"),
                    newConfiguration.heatingStage2FanState
                )
                stagedFanConfigPointsList.add(heatingStage2FanConfigPoint)
            }
            else -> {
                val heatingStage3FanConfigPoint = createFanConfigPoint(
                    "$equipDis-fanOutHeatingStage3",
                    arrayOf("config", "writable", "zone", "fan", "heating", "output", "sp", "stage3"),
                    newConfiguration.heatingStage3FanState
                )
                stagedFanConfigPointsList.add(heatingStage3FanConfigPoint)
            }
        }
        return stagedFanConfigPointsList
    }

    private fun createFanConfigPoint(
        pointName: String,
        markers: Array<String>,
        fanState: Any
    ): Pair<Point, Any> {
        val fanConfigPoint = createHaystackPointWithUnit(pointName, markers, null, "V")
        return Pair(fanConfigPoint, fanState)
    }
    
}



