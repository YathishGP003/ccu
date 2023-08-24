package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.*
import a75f.io.logic.ANALOG_VALUE
import a75f.io.logic.BINARY_VALUE
import a75f.io.logic.MULTI_STATE_VALUE
import a75f.io.logic.addBacnetTags
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.*
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.*
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.*
import a75f.io.logic.bo.building.hyperstat.profiles.util.*
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.tuners.TunerConstants
import android.util.Log
import java.util.*

/**
 * Created by Manjunath K on 30-07-2021.
 */
class HyperStatPointsUtil(
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

        const val HYPERSTAT = Tags.HYPERSTAT

        // function to create equip Point
        fun createHyperStatEquipPoint(
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
                .addMarker(HYPERSTAT).addMarker(Tags.EQUIP).addMarker(Tags.STANDALONE)
                .addMarker(Tags.ZONE).addMarker(profileName)
                .setGatewayRef(gatewayRef)
                .setTz(tz)
                .setGroup(nodeAddress)
                .setDisplayName(equipDis)

            return hayStackAPI.addEquip(equipPoint.build())
        }
        fun createHyperStatEquipPoint(
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
                .addMarker(HYPERSTAT).addMarker(Tags.EQUIP)
                .addMarker(Tags.ZONE).addMarker(profileName).addMarker(Tags.STANDALONE)
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
            .setCurStatus("0")   //added just for bacnet testing
            .addMarker(profileName).addMarker(Tags.STANDALONE)

        // add specific markers
        markers.forEach { point.addMarker(it) }
        return point.build()
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
            .setEnums(enums)

            .addMarker(profileName).addMarker(Tags.STANDALONE)

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
            .setCurStatus("0")   //added just for bacnet testing
            .addMarker(profileName).addMarker(Tags.STANDALONE)

        val tempDis =  displayName.split("-").last()
            if (tempDis == "heatingLoopOutput" || tempDis == "coolingLoopOutput" || tempDis == "fanLoopOutput") {
                point.setMinVal("0")
                point.setMaxVal("100")
            }
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
            hayStackAPI.addPoint(point)
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

    // Points to hold loop output value
     fun createLoopOutputPoints(isHpu: Boolean): MutableList<Pair<Point, Any>> {
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

        addBacnetTags(coolingLoopOutputPoint, 8, ANALOG_VALUE, nodeAddress.toInt())
        addBacnetTags(heatingLoopOutputPoint, 31, ANALOG_VALUE, nodeAddress.toInt())
        addBacnetTags(fanLoopOutputPoint, 22, ANALOG_VALUE, nodeAddress.toInt())

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
            addBacnetTags(compressorLoopOutputPoint, 52, ANALOG_VALUE, nodeAddress.toInt())
            loopOutputPointsList.add(Pair(compressorLoopOutputPoint, 0.0))
        }
        return loopOutputPointsList
    }



    /**
     * Functions which creates configuration enable/disable Points
     */

    // Function which creates Relay  config Points
    fun createIsRelayEnabledConfigPoints(
        relay1: ConfigState,relay2: ConfigState,relay3: ConfigState,
        relay4: ConfigState,relay5: ConfigState,relay6: ConfigState,
        profileType: ProfileType
    ): MutableList<Pair<Point, Any>> {

        // Hold list of Relay Points
        val configRelayPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        // Common Relay Markers
        val relayMarkers: MutableList<String> = LinkedList()
        relayMarkers.addAll(
            arrayOf("config", "writable", "zone")
        )

        /**  Relay 1 config and association point */
        relayMarkers.add("relay1")
        relayMarkers.add("enabled")
        relayMarkers.add("cmd")
        val relay1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay1OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )

        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        relayMarkers.remove("cmd")
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
        relayMarkers.add("cmd")
        val relay2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay2OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )

        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        relayMarkers.remove("cmd")
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
        relayMarkers.add("cmd")
        val relay3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay3OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        relayMarkers.remove("cmd")
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
        relayMarkers.add("cmd")
        val relay4Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay4OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        relayMarkers.remove("cmd")
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
        relayMarkers.add("cmd")
        val relay5Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay5OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        relayMarkers.remove("cmd")
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
        relayMarkers.add("cmd")
        val relay6Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay6OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        relayMarkers.remove("cmd")
        val relay6AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay6OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum(profileType)
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay6")

        // Enable disable point
        configRelayPointsList.add(Pair(relay1Point, if (relay1.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay2Point, if (relay2.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay3Point, if (relay3.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay4Point, if (relay4.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay5Point, if (relay5.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay6Point, if (relay6.enabled) 1.0 else 0.0))

        // Association  point
        configRelayPointsList.add(Pair(relay1AssociationPoint, relay1.association))
        configRelayPointsList.add(Pair(relay2AssociationPoint, relay2.association))
        configRelayPointsList.add(Pair(relay3AssociationPoint, relay3.association))
        configRelayPointsList.add(Pair(relay4AssociationPoint, relay4.association))
        configRelayPointsList.add(Pair(relay5AssociationPoint, relay5.association))
        configRelayPointsList.add(Pair(relay6AssociationPoint, relay6.association))

        return configRelayPointsList
    }

    //Function which creates analog out config Point
    fun createIsAnalogOutEnabledConfigPoints(
        analogOut1: ConfigState,analogOut2: ConfigState,analogOut3: ConfigState, profileType: ProfileType
    ): MutableList<Pair<Point, Any>> {

        val configAnalogOutPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val analogMarkers: MutableList<String> = LinkedList()
        analogMarkers.addAll(
            arrayOf("config", "writable", "zone", "output")
        )

        /**  analog 1 config and association point */
        analogMarkers.add("analog1")
        analogMarkers.add("enabled")
        analogMarkers.add("cmd")
        val analogOut1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut1Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")
        analogMarkers.remove("cmd")
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
        analogMarkers.add("cmd")
        val analogOut2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut2Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")
        analogMarkers.remove("cmd")

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
        analogMarkers.add("cmd")
        val analogOut3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut3Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")
        analogMarkers.remove("cmd")

        val analogOut3AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut3Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getAnalogOutConfigEnum(profileType)
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog3")

        // Analog out Enable/Disable Points
        configAnalogOutPointsList.add( Pair( analogOut1Point, if (analogOut1.enabled) 1.0 else 0.0))
        configAnalogOutPointsList.add( Pair( analogOut2Point, if (analogOut2.enabled) 1.0 else 0.0))
        configAnalogOutPointsList.add( Pair( analogOut3Point, if (analogOut3.enabled) 1.0 else 0.0))

        // Analog out Association Points
        configAnalogOutPointsList.add( Pair( analogOut1AssociationPoint, analogOut1.association))
        configAnalogOutPointsList.add( Pair( analogOut2AssociationPoint, analogOut2.association))
        configAnalogOutPointsList.add( Pair( analogOut3AssociationPoint, analogOut3.association))
        return configAnalogOutPointsList
    }

    // Function which Creates Analog in config Points
    fun createIsAnalogInEnabledConfigPoints(
        analogIn1: ConfigState,analogIn2: ConfigState
    ): MutableList<Pair<Point, Any>> {

        val configAnalogInPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val analogInEnum = "false,true"

        val analogMarkers: MutableList<String> = LinkedList()
        analogMarkers.addAll(
            arrayOf("config", "writable", "zone", "input")
        )

        analogMarkers.add("analog1")
        analogMarkers.add("enabled")
        val analogIn1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogIn1Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            analogInEnum
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")
        val analogIn1AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogIn1Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            analogInEnum
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog1")


        analogMarkers.add("analog2")
        analogMarkers.add("enabled")

        val analogIn2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogIn2Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            analogInEnum
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")

        val analogIn2AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogIn2Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            analogInEnum
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog2")

        // Analog Enable/Disable Points
        configAnalogInPointsList.add(Pair(analogIn1Point, if (analogIn1.enabled) 1.0 else 0.0))
        configAnalogInPointsList.add(Pair(analogIn2Point, if (analogIn2.enabled) 1.0 else 0.0))

        // Analog in Association points
        configAnalogInPointsList.add( Pair(analogIn1AssociationPoint, analogIn1.association))
        configAnalogInPointsList.add( Pair(analogIn2AssociationPoint, analogIn2.association))

        return configAnalogInPointsList
    }

    // Function which creates Th1,Th2 config Points
    fun createIsThermistorEnabledConfigPoints(
        thIn1: ConfigState, thIn2: ConfigState, is2PipeProfile: Boolean
    ): MutableList<Pair<Point, Any>> {
        val thEnum = "false,true"
        val configThPointsList: MutableList<Pair<Point, Any>> = LinkedList()
        val th1PointMarkers = arrayOf(
            "temp", "zone", "config", "enabled", "writable", "discharge",
              "air","cmd"
        )
        val th1Point = createHaystackPointWithEnums(
            "$equipDis-enableAirflowTempSensor",
            th1PointMarkers,
            thEnum,
        )

        if(is2PipeProfile){
            val supplyWaterSensor = arrayOf(
                "temp", "zone", "config", "enabled", "writable", "cmd","water","supply"
            )
            val th2Point = createHaystackPointWithEnums(
                "$equipDis-enableSupplyWaterSensor",
                supplyWaterSensor,
                thEnum
            )
            configThPointsList.add(Pair(th2Point, if (thIn2.enabled) 1.0 else 0.0))
        }else{
            val doorWindowMarkers = arrayOf(
                "temp", "zone", "config", "enabled", "writable", "window"
            )
            val th2Point = createHaystackPointWithEnums(
                "$equipDis-enableDoorWindowSensor",
                doorWindowMarkers,
                thEnum
            )
            configThPointsList.add(Pair(th2Point, if (thIn2.enabled) 1.0 else 0.0))
        }

        configThPointsList.add(Pair(th1Point, if (thIn1.enabled) 1.0 else 0.0))
        return configThPointsList
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
        autoForceAutoAwayConfigPointsList.addAll(createKeycardWindowSensingPoints())
        return autoForceAutoAwayConfigPointsList
    }

    fun createKeycardWindowSensingPoints() : MutableList<Pair<Point, Any>> {

        val keycardWindowSensingPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val enableKeyCardSensingPointMarkers = arrayOf(
            "config","zone", "keycard", "sensing", "enabled","writable"
        )

        val enableWindowSensingPointMarkers = arrayOf(
            "config","zone", "window", "sensing", "enabled","writable"
        )

        val keycardSensorInputPointMarkers = arrayOf(
            "zone", "keycard", "sensor", "input","his"
        )

        val windowSensorInputPointMarkers = arrayOf(
            "zone", "window", "sensor", "input","his"
        )

        val enableKeycardSensingPointPoint = createHaystackPointWithEnums(
            "$equipDis-keycardSensingEnabled",
            enableKeyCardSensingPointMarkers,
            "off,on"
        )

        val enableWindowSensingPointPoint = createHaystackPointWithEnums(
            "$equipDis-windowSensingEnabled",
            enableWindowSensingPointMarkers,
            "off,on"
        )

        val keycardSensorInput = createHaystackPointWithEnums(
            "$equipDis-keycardSensorInput",
            keycardSensorInputPointMarkers,

            "off,on"
        )

        val windowSensorInput = createHaystackPointWithEnums(
            "$equipDis-windowSensorInput",
            windowSensorInputPointMarkers,
            "off,on"
        )

        keycardWindowSensingPointsList.add(
            Pair(enableKeycardSensingPointPoint, 0)
        )
        keycardWindowSensingPointsList.add(
            Pair(enableWindowSensingPointPoint, 0)
        )

        keycardWindowSensingPointsList.add(
            Pair(keycardSensorInput, 0)
        )

        keycardWindowSensingPointsList.add(
            Pair(windowSensorInput, 0)
        )

        return keycardWindowSensingPointsList
    }
    // Function which creates co2 Points
    fun createPointCO2ConfigPoint(
        zoneCO2DamperOpeningRate: Double,
        zoneCO2Threshold: Double,
        zoneCO2Target: Double
    ): MutableList<Pair<Point, Any>> {

        val co2ConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val co2DamperOpeningRatePointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "damper", "opening", "rate","actuator","sp"
        )
        val zoneCO2ThresholdPointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "threshold", "sp","concentration"
        )
        val zoneCO2TargetPointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "target", "sp","concentration"
        )


        val co2DamperOpeningRatePoint = createHaystackPointWithUnit(
            "$equipDis-co2DamperOpeningRate",
            co2DamperOpeningRatePointMarkers,
            "cov","%"
        )


        val zoneCO2ThresholdPointPoint = createHaystackPointWithUnit(
            "$equipDis-zoneCO2Threshold",
            zoneCO2ThresholdPointMarkers,
            "cov","ppm"
        )
        addBacnetTags(zoneCO2ThresholdPointPoint, 49, ANALOG_VALUE, nodeAddress.toInt())

        val zoneCO2TargetPointPoint = createHaystackPointWithUnit(
            "$equipDis-zoneCO2Target",
            zoneCO2TargetPointMarkers,
            "cov","ppm"
        )
        addBacnetTags(zoneCO2TargetPointPoint, 48, ANALOG_VALUE, nodeAddress.toInt())

        co2ConfigPointsList.add(
            Pair(co2DamperOpeningRatePoint, zoneCO2DamperOpeningRate)
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
        zonePm2p5Threshold: Double,
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

        val zonePm2p5ThresholdPoint = createHaystackPointWithUnit(
            "$equipDis-zonePm2p5Threshold",
            pm2p5Markers.stream().toArray { arrayOfNulls(it) },
            "cov","ug/\u33A5"
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

        pointsList.add(Pair(zonePm2p5ThresholdPoint, zonePm2p5Threshold))
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
        addBacnetTags(fanOperationsModePoint, 21, MULTI_STATE_VALUE, nodeAddress.toInt())

        val conditioningModePointPoint = createHaystackPointWithEnums(
            displayName = "$equipDis-ConditioningMode",
            conditioningModePointMarkers,
            conditioningModePointEnums
        )
        addBacnetTags(conditioningModePointPoint, 46, MULTI_STATE_VALUE, nodeAddress.toInt())

        val targetDehumidifierPointPoint = createHaystackPointWithUnit(
            displayName = "$equipDis-targetDehumidifier",
            dehumidifierPointMarkers,
            "cov",
            "%"
        )
        addBacnetTags(targetDehumidifierPointPoint, 50, ANALOG_VALUE, nodeAddress.toInt())

        val targetHumidifierPointPoint = createHaystackPointWithUnit(
            displayName = "$equipDis-targetHumidifier",
            humidifierPointMarkers,
            "cov",
            "%"
        )
        addBacnetTags(targetHumidifierPointPoint, 51, ANALOG_VALUE, nodeAddress.toInt())

        userIntentPointList.add(Pair(fanOperationsModePoint, defaultFanMode.ordinal.toDouble()))
        userIntentPointList.add(Pair(conditioningModePointPoint, defaultConditioningMode.ordinal.toDouble()))
        userIntentPointList.add(Pair(targetDehumidifierPointPoint, TunerConstants.STANDALONE_TARGET_DEHUMIDIFIER))
        userIntentPointList.add(Pair(targetHumidifierPointPoint, TunerConstants.STANDALONE_TARGET_HUMIDITY))

        return userIntentPointList
    }


    // Function creates logical points for CPU profile
    fun createConfigRelayLogicalPoints(hyperStatConfig: HyperStatCpuConfiguration): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatConfig.relay1State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay1State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_ONE, 0.0))
        }
        if (hyperStatConfig.relay2State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay2State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_TWO, 0.0))
        }
        if (hyperStatConfig.relay3State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay3State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_THREE, 0.0))
        }
        if (hyperStatConfig.relay4State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay4State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FOUR, 0.0))
        }
        if (hyperStatConfig.relay5State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay5State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FIVE, 0.0))
        }
        if (hyperStatConfig.relay6State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay6State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_SIX, 0.0))
        }

        return configLogicalPointsList
    }

    fun relayConfiguration(relayState: RelayState): Point {

        //      Relay Can be Associated to these all states
        //      COOLING_STAGE_1,COOLING_STAGE_2,COOLING_STAGE_3,HEATING_STAGE_1,HEATING_STAGE_2,HEATING_STAGE_3
        //      FAN_LOW_SPEED,FAN_MEDIUM_SPEED,FAN_HIGH_SPEED,FAN_ENABLED,OCCUPIED_ENABLED,HUMIDIFIER,DEHUMIDIFIER

        return when {
            HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(relayState = relayState) ->
                createCoolingStagesPoint(relayState = relayState)
            HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(relayState = relayState) ->
                createHeatingStagesPoint(relayState = relayState)
            HyperStatAssociationUtil.isRelayAssociatedToFan(relayState = relayState) ->
                createFanStagesPoint(fanStage = getFanStageValueForCPU(relayState))
            HyperStatAssociationUtil.isRelayAssociatedToFanEnabled(relayState = relayState) ->
                LogicalPointsUtil.createPointForFanEnable(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            HyperStatAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState = relayState) ->
                LogicalPointsUtil.createPointForOccupiedEnabled(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayAssociatedToHumidifier(relayState = relayState) ->
                LogicalPointsUtil.createPointForHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayAssociatedToDeHumidifier(relayState = relayState) ->
                LogicalPointsUtil.createPointForDeHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toShort())
            else -> Point.Builder().build()
        }

    }

    /**
     * Functions which can map all the (CPU) Relay Ports
     */
    private fun createCoolingStagesPoint(relayState: RelayState): Point {
        when (relayState.association) {
            CpuRelayAssociation.COOLING_STAGE_1 -> {
                return LogicalPointsUtil.createCoolingStage1Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            }
            CpuRelayAssociation.COOLING_STAGE_2 -> {
                return LogicalPointsUtil.createCoolingStage2Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            }
            CpuRelayAssociation.COOLING_STAGE_3 -> {
                return LogicalPointsUtil.createCoolingStage3Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            }
            else -> {}
        }
        throw NullPointerException("Stages can not be null")
    }

    private fun createHeatingStagesPoint(relayState: RelayState): Point {
        when (relayState.association) {
            CpuRelayAssociation.HEATING_STAGE_1 -> {
                return  LogicalPointsUtil.createHeatingStage1Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            }
            CpuRelayAssociation.HEATING_STAGE_2 -> {
                return LogicalPointsUtil.createHeatingStage2Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            }
            CpuRelayAssociation.HEATING_STAGE_3 -> {
                return LogicalPointsUtil.createHeatingStage3Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            }
            else -> {}
        }
        throw NullPointerException("Stages can not be null")
    }

    private fun createFanStagesPoint(fanStage: Int): Point {
        when (fanStage) {
            1-> return LogicalPointsUtil.createFanLowPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()) // Low
            2-> return LogicalPointsUtil.createFanMediumPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()) // Medium
            3-> return LogicalPointsUtil.createFanHighPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()) // High
        }
        throw NullPointerException("Fan stage can not be null")
    }


    fun createConfigAnalogOutLogicalPoints(
        hyperStatConfig: HyperStatCpuConfiguration
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatConfig.analogOut1State.enabled) {

            if (hyperStatConfig.analogOut1State.association != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
                    analogOutState = hyperStatConfig.analogOut1State,
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
                        hyperStatConfig.analogOut1State.voltageAtMin
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        maxPoint.first as Point,
                        maxPoint.second as Any,
                        hyperStatConfig.analogOut1State.voltageAtMax
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(hyperStatConfig.analogOut1State),
                    hyperStatConfig.analogOut1State.perAtFanLow,
                    hyperStatConfig.analogOut1State.perAtFanMedium,
                    hyperStatConfig.analogOut1State.perAtFanHigh,
                    "analog1", configLogicalPointsList
                )
            } else {
                val pointData: Triple<Point, Any?, Any?> = analogOutConfiguration1()
                configLogicalPointsList.add(
                    Triple(
                        pointData.first,
                        Port.ANALOG_OUT_ONE,
                        0.0
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut1State),
                    hyperStatConfig.analogOut1State.perAtFanLow,
                    hyperStatConfig.analogOut1State.perAtFanMedium,
                    hyperStatConfig.analogOut1State.perAtFanHigh,
                    "analog1", configLogicalPointsList
                )
            }

        }

        if (hyperStatConfig.analogOut2State.enabled) {

            if (hyperStatConfig.analogOut2State.association != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
                    analogOutState = hyperStatConfig.analogOut2State,
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
                        hyperStatConfig.analogOut2State.voltageAtMin
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        maxPoint.first as Point,
                        maxPoint.second as Any,
                        hyperStatConfig.analogOut2State.voltageAtMax
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(hyperStatConfig.analogOut2State),
                    hyperStatConfig.analogOut2State.perAtFanLow,
                    hyperStatConfig.analogOut2State.perAtFanMedium,
                    hyperStatConfig.analogOut2State.perAtFanHigh,
                    "analog2", configLogicalPointsList
                )
            } else {
                val pointData: Triple<Point, Any?, Any?> = analogOutConfiguration1()
                configLogicalPointsList.add(
                    Triple(
                        pointData.first,
                        Port.ANALOG_OUT_ONE,
                        0.0
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut2State),
                    hyperStatConfig.analogOut2State.perAtFanLow,
                    hyperStatConfig.analogOut2State.perAtFanMedium,
                    hyperStatConfig.analogOut2State.perAtFanHigh,
                    "analog2", configLogicalPointsList
                )
            }
        }

        if (hyperStatConfig.analogOut3State.enabled) {

            if (hyperStatConfig.analogOut3State.association != CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED) {
                val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
                    analogOutState = hyperStatConfig.analogOut3State,
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
                        hyperStatConfig.analogOut3State.voltageAtMin
                    )
                )
                configLogicalPointsList.add(
                    Triple(
                        maxPoint.first as Point,
                        maxPoint.second as Any,
                        hyperStatConfig.analogOut3State.voltageAtMax
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(hyperStatConfig.analogOut3State),
                    hyperStatConfig.analogOut3State.perAtFanLow,
                    hyperStatConfig.analogOut3State.perAtFanMedium,
                    hyperStatConfig.analogOut3State.perAtFanHigh,
                    "analog3", configLogicalPointsList
                )
            } else {
                val pointData: Triple<Point, Any?, Any?> = analogOutConfiguration1()
                configLogicalPointsList.add(
                    Triple(
                        pointData.first,
                        Port.ANALOG_OUT_ONE,
                        0.0
                    )
                )
                createFanConfigForAnalogOut(
                    HyperStatAssociationUtil.isAnalogOutAssociatedToStagedFanSpeed(hyperStatConfig.analogOut3State),
                    hyperStatConfig.analogOut3State.perAtFanLow,
                    hyperStatConfig.analogOut3State.perAtFanMedium,
                    hyperStatConfig.analogOut3State.perAtFanHigh,
                    "analog3", configLogicalPointsList
                )
            }
        }
        return configLogicalPointsList
    }

    fun analogOutConfiguration1(): Triple<Point, Any?, Any?> {
        return Triple(
            LogicalPointsUtil.createAnalogOutPointForPredefinedFanSpeed(equipDis, siteRef, equipRef, roomRef, floorRef, tz, nodeAddress),
            null,
            null
        )
    }

    fun analogOutConfiguration(analogOutState: AnalogOutState, analogTag: String): Triple<Any, Any, Any> {
        //   AnalogOut can be Associated  to these all state
        //   COOLING, FAN_SPEED, HEATING, DCV_DAMPER
        return when {
            HyperStatAssociationUtil.isAnalogOutAssociatedToCooling(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "cooling",
                    markers = arrayOf("cooling")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForCooling(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_COOLING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_COOLING),
                )
            }

            HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOut = analogOutState) -> {
                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "fanspeed",
                    markers = arrayOf("fan","speed")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForFanSpeed(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_FAN_SPEED),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_FAN_SPEED),
                )
            }

            HyperStatAssociationUtil.isAnalogOutAssociatedToHeating(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "heating",
                    markers = arrayOf("heating")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForHeating(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_HEATING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_HEATING),
                )
            }

            HyperStatAssociationUtil.isAnalogOutAssociatedToDcvDamper(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "dcvdamper",
                    markers = arrayOf("dcv","damper")
                )
                Triple(
                    LogicalPointsUtil.createAnalogOutPointForDCVDamper(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_DCV_DAMPER),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_DCV_DAMPER),
                )
            }
            // Need to check how how handle this
            else -> Triple(Point.Builder().build(), Point.Builder().build(), Point.Builder().build())
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

    fun createConfigAnalogInLogicalPoints(
        analogIn1State: AnalogInState,
        analogIn2State: AnalogInState,
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (analogIn1State.enabled) {
            val pointData: Point = analogInConfiguration(
                analogInState = analogIn1State,
                analogTag = "analog1"
            )
            configLogicalPointsList.add(Triple(pointData, Port.ANALOG_IN_ONE, 0.0))
        }
        if (analogIn2State.enabled) {
            val pointData: Point = analogInConfiguration(
                analogInState = analogIn2State,
                analogTag = "analog2"
            )
            configLogicalPointsList.add(Triple(pointData, Port.ANALOG_IN_TWO, 0.0))
        }

        return configLogicalPointsList
    }


    fun createAirflowTempSensor(): Point{
       return LogicalPointsUtil.createPointForAirflowTempSensor(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
    }

    fun createPointForDoorWindowSensor(windowSensorType: LogicalPointsUtil.WindowSensorType): Point{
       return LogicalPointsUtil.createPointForDoorWindowSensor(equipDis,siteRef,equipRef,roomRef,floorRef,tz,windowSensorType)
    }

    fun createConfigThermistorInLogicalPoints(
        isEnableAirFlowTempSensorEnabled: Boolean,
        isTh2ConfigEnabled: Boolean,
        isPipe2Config: Boolean
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()
        if (isEnableAirFlowTempSensorEnabled) {
            val pointData: Point = LogicalPointsUtil.createPointForAirflowTempSensor(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            configLogicalPointsList.add(Triple(pointData, Port.TH1_IN, 0.0))
        }
        if(isTh2ConfigEnabled) {
            if(isPipe2Config){
                val waterSupplySensor = LogicalPointsUtil.createPointForWaterSupplyTempSensor(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
                configLogicalPointsList.add(Triple(waterSupplySensor, Port.TH2_IN, 0.0))
            }else{
                val doorWindowSensorTh2Point = LogicalPointsUtil.createPointForDoorWindowSensor(equipDis,siteRef,equipRef,roomRef,floorRef,tz,LogicalPointsUtil.WindowSensorType.WINDOW_SENSOR)
                configLogicalPointsList.add(Triple(doorWindowSensorTh2Point, Port.TH2_IN, 0.0))
            }
        }
        return configLogicalPointsList
    }


    private fun getFanStageValueForCPU(relayState: RelayState): Int{
        return when(relayState.association){
            CpuRelayAssociation.FAN_LOW_SPEED -> 1
            CpuRelayAssociation.FAN_MEDIUM_SPEED -> 2
            CpuRelayAssociation.FAN_HIGH_SPEED -> 3
            else -> { 0 } // No mapping found
        }

    }

    fun analogInConfiguration(analogInState: AnalogInState, analogTag: String): Point {
        return when {
            (HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX10(analogInState)) -> {

                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "10", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER
                )
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX20(analogInState)) -> {
                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "20", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER_20
                )
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX50(analogInState)) -> {
                LogicalPointsUtil.createPointForCurrentTx(
                    equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                    min = "0", max = "50", inc = "0.1", unit = "amps",LogicalPointsUtil.TransformerSensorType.TRANSFORMER_50
                )
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(analogInState)) -> {

                 LogicalPointsUtil.createPointForDoorWindowSensor(equipDis,siteRef,equipRef,roomRef,floorRef,tz,
                     (if(analogTag.contentEquals("analog1")) LogicalPointsUtil.WindowSensorType.WINDOW_SENSOR_2 else LogicalPointsUtil.WindowSensorType.WINDOW_SENSOR_3))
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(analogInState)) -> {
                LogicalPointsUtil.createPointForKeyCardSensor(equipDis,siteRef,equipRef,roomRef,floorRef,tz,(if(analogTag.contentEquals("analog1")) 1 else 2))
            }
            else -> Point.Builder().build()
        }
    }



    // Function to logicalPoints
    fun createLogicalSensorPoints(): MutableList<Triple<Point, Any, Any>> {

        val logicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        val humidityPointMarkers =
            arrayOf("zone", "air", "humidity", "sensor", "his", "cur", "logical")
        val illuminanceSensorPointMarkers =
            arrayOf("zone", "illuminance", "sensor", "his", "cur", "logical")
        val occupancySensorPointMarkers =
            arrayOf("zone", "occupancy", "sensor", "his", "cur", "logical")

        val humidityPoint = createHaystackPointWithUnit(
            "$equipDis-zone" + Port.SENSOR_RH.portSensor,
            humidityPointMarkers,
            "cov",
            "%"
        )
        addBacnetTags(humidityPoint, 38, ANALOG_VALUE, nodeAddress.toInt())

        val illuminancePoint = createHaystackPointWithUnit(
            "$equipDis-zone" + Port.SENSOR_ILLUMINANCE.portSensor,
            illuminanceSensorPointMarkers,
            "cov",
            "lux"
        )
        addBacnetTags(illuminancePoint, 39, ANALOG_VALUE, nodeAddress.toInt())

        val occupancyPoint = createHaystackPointWithEnums(
            "$equipDis-"+ Port.SENSOR_OCCUPANCY.portSensor+"sensor",
            occupancySensorPointMarkers,
            "off,on"
        )
        addBacnetTags(occupancyPoint, 40, BINARY_VALUE, nodeAddress.toInt())

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
        addBacnetTags(temperatureOffsetPoint, 17, ANALOG_VALUE, nodeAddress.toInt())
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
        addBacnetTags(desiredTempPoint, 18, ANALOG_VALUE, nodeAddress.toInt())


        val desiredTempCoolingPoint = createHaystackPointWithUnit(
            "$equipDis-desiredTempCooling",
            desiredTempCoolingPointMarkers,
            "cov",
            "\u00B0F"
        )
        addBacnetTags(desiredTempCoolingPoint, 20, ANALOG_VALUE, nodeAddress.toInt())
        val desiredTempHeatingPoint = createHaystackPointWithUnit(
            "$equipDis-desiredTempHeating",
            desiredTempHeatingPointMarkers,
            "cov",
            "\u00B0F"
        )
        addBacnetTags(desiredTempHeatingPoint, 19, ANALOG_VALUE, nodeAddress.toInt())

        val currentTempPoint = createHaystackPointWithUnit(
            "$equipDis-currentTemp",
            currentTempPointMarkers,
            "cov",
            "\u00B0F"
        )
        addBacnetTags(currentTempPoint, 14, ANALOG_VALUE, nodeAddress.toInt())

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
            Tags.ZONE,Tags.OPERATING
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
        addBacnetTags(operatingModeModePoint, 47, MULTI_STATE_VALUE, nodeAddress.toInt())
        addPointToHaystack(point = occupancyModePoint)
        val occupancyDetectionPointId = addPointToHaystack(point = occupancyDetection)
        val operatingModeModePointId = addPointToHaystack(point = operatingModeModePoint)

        addDefaultHisValueForPoint(operatingModeModePointId, 1.0)
        addDefaultHisValueForPoint(occupancyDetectionPointId, 0.0)

    }


    // Function which finds default fan speed
     fun getCPUDefaultFanSpeed(config: HyperStatCpuConfiguration): StandaloneFanStage {

        return if (HyperStatAssociationUtil.isAnyAnalogOutEnabledAssociatedToFanSpeed(config)
            || HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToFan(config) || HyperStatAssociationUtil.isAnyAnalogOutMappedToStagedFan(config)){
             StandaloneFanStage.AUTO
        }
        else StandaloneFanStage.OFF
    }

    // CPU Function which finds default default cooling mode
     fun getCPUDefaultConditioningMode(config: HyperStatCpuConfiguration): StandaloneConditioningMode {

        return if((HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToCooling(config)
                    || HyperStatAssociationUtil.isAnyAnalogOutEnabledAssociatedToCooling(config))
                    && (HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToHeating(config)
                    || HyperStatAssociationUtil.isAnyAnalogOutEnabledAssociatedToHeating(config))) {
            StandaloneConditioningMode.AUTO
        }
        else if(HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToCooling(config)
            ||HyperStatAssociationUtil.isAnyAnalogOutEnabledAssociatedToCooling(config)) {
            StandaloneConditioningMode.COOL_ONLY
        }
        else if(HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToHeating(config)
            ||HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToCooling(config)) {
            StandaloneConditioningMode.HEAT_ONLY
        }
        else StandaloneConditioningMode.OFF
    }



    private fun getRelayConfigEnum(profileType: ProfileType): String {

        when(profileType){
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT-> {
                return "$COOLING_STAGE1,$COOLING_STAGE2,$COOLING_STAGE3,$HEATING_STAGE1," +
                        "$HEATING_STAGE2,$HEATING_STAGE3,$FAN_ENABLED,$OCCUPIED_ENABLED," +
                        "$FAN_HIGH,$FAN_LOW,$FAN_MEDIUM,$HUMIDIFIER,$DEHUMIDIFIER"
            }
            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT-> {
                return "$COMPRESSOR_STAGE1,$COMPRESSOR_STAGE2,$COMPRESSOR_STAGE3,$AUX_HEATING_STAGE1," +
                        "$AUX_HEATING_STAGE2,$FAN_LOW,$FAN_MEDIUM,$FAN_HIGH,$FAN_ENABLED,$OCCUPIED_ENABLED," +
                        ",$HUMIDIFIER,$DEHUMIDIFIER,$CHANGEOVERCOOLING,$CHANGEOVERHEATING"
            }
            ProfileType.HYPERSTAT_TWO_PIPE_FCU-> {
                return "$FAN_LOW,$FAN_MEDIUM,$FAN_HIGH,$AUX_HEATING_STAGE1,$AUX_HEATING_STAGE2," +
                        "$WATER_VALVE,$FAN_ENABLED,$OCCUPIED_ENABLED,$HUMIDIFIER,$DEHUMIDIFIER"
            }
            else -> { }
        }
        return ""
    }
    private fun getAnalogOutConfigEnum(profileType: ProfileType): String {
        when(profileType) {
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT -> {
                return "$COOLING,$MODULATING_FAN_SPEED,$HEATING,$DCV_DAMPER,$PREDEFINED_FAN_SPEED"
            }
            ProfileType.HYPERSTAT_HEAT_PUMP_UNIT -> {
                return "$COMPRESSORSPEED,$FAN_SPEED,$DCV_DAMPER"
            }
            ProfileType.HYPERSTAT_TWO_PIPE_FCU -> {
            return "$WATER_VALVE,$FAN_SPEED,$DCV_DAMPER"
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
        }
        return 0
    }


    /***   Reading logical points  **/
    fun getCpuRelayLogicalPoint(association: CpuRelayAssociation): Point{
        return when(association){
            CpuRelayAssociation.COOLING_STAGE_1-> Point.Builder().setHashMap(LogicalPointsUtil.readCoolingStage1RelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.COOLING_STAGE_2-> Point.Builder().setHashMap(LogicalPointsUtil.readCoolingStage2RelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.COOLING_STAGE_3-> Point.Builder().setHashMap(LogicalPointsUtil.readCoolingStage3RelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.HEATING_STAGE_1-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingStage1RelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.HEATING_STAGE_2-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingStage2RelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.HEATING_STAGE_3-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingStage3RelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.FAN_LOW_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanLowRelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.FAN_MEDIUM_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanMediumRelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.FAN_HIGH_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanHighRelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.FAN_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanEnabledRelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.OCCUPIED_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readOccupiedEnabledRelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.HUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readHumidifierRelayLogicalPoint(equipRef)).build()
            CpuRelayAssociation.DEHUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readDeHumidifierRelayLogicalPoint(equipRef)).build()
        }
    }
    fun getCpuAnalogOutLogicalPoint(association: CpuAnalogOutAssociation): Point {
        return when(association){
            CpuAnalogOutAssociation.COOLING-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogCoolingLogicalPoint(equipRef)).build()
            CpuAnalogOutAssociation.MODULATING_FAN_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutFanSpeedLogicalPoint(equipRef)).build()
            CpuAnalogOutAssociation.HEATING-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogHeatingLogicalPoint(equipRef)).build()
            CpuAnalogOutAssociation.DCV_DAMPER-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutDcvLogicalPoint(equipRef)).build()
            CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef)).build()
        }
    }

    /*** ==================================================================== **/
    // Pipe 2 fan coil Unit

    fun relayPipe2Configuration(relayState: Pipe2RelayState): Point {

        //      Relay Can be Associated to these all states
        //      Fan low speed, Fan medium speed,Fan highspeed,Aux Heating stage1,
        //      Aux Heating Stage2,Water Valve, Fan Enable,Occupied Enable, Humidifier,Dehumidifier

        return when {
            HyperStatAssociationUtil.isRelayFanLowSpeed(relayState = relayState) ->
                return  LogicalPointsUtil.createFanLowPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayFanMediumSpeed(relayState = relayState) ->
                return LogicalPointsUtil.createFanMediumPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayFanHighSpeed(relayState = relayState) ->
                return LogicalPointsUtil.createFanHighPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayAuxHeatingStage1(relayState = relayState) ->
                return LogicalPointsUtil.createAuxHeatingStage1Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayAuxHeatingStage2(relayState = relayState) ->
                return LogicalPointsUtil.createAuxHeatingStage2Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayWaterValveStage(relayState = relayState) ->
                return LogicalPointsUtil.createWaterValvePoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayFanEnabledStage(relayState = relayState) ->
                return LogicalPointsUtil.createPointForFanEnable(equipDis,siteRef,equipRef,roomRef,floorRef,tz)
            HyperStatAssociationUtil.isRelayOccupiedEnabledStage(relayState = relayState) ->
                return LogicalPointsUtil.createPointForOccupiedEnabled(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayHumidifierEnabledStage(relayState = relayState) ->
                return LogicalPointsUtil.createPointForHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            HyperStatAssociationUtil.isRelayDeHumidifierEnabledStage(relayState = relayState) ->
                return LogicalPointsUtil.createPointForDeHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toShort())
            else -> Point.Builder().build()
        }

    }
    fun create2PipeRelayLogicalPoints(hyperStatConfig: HyperStatPipe2Configuration):  MutableList<Triple<Point, Any, Any>> {
        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if(hyperStatConfig.relay1State.enabled){
            val pointData: Point = relayPipe2Configuration(relayState = hyperStatConfig.relay1State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_ONE, 0.0))
        }
        if(hyperStatConfig.relay2State.enabled){
            val pointData: Point = relayPipe2Configuration(relayState = hyperStatConfig.relay2State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_TWO, 0.0))
        }
        if(hyperStatConfig.relay3State.enabled){
            val pointData: Point = relayPipe2Configuration(relayState = hyperStatConfig.relay3State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_THREE, 0.0))
        }
        if(hyperStatConfig.relay4State.enabled){
            val pointData: Point = relayPipe2Configuration(relayState = hyperStatConfig.relay4State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FOUR, 0.0))
        }
        if(hyperStatConfig.relay5State.enabled){
            val pointData: Point = relayPipe2Configuration(relayState = hyperStatConfig.relay5State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FIVE, 0.0))
        }
        if(hyperStatConfig.relay6State.enabled){
            val pointData: Point = relayPipe2Configuration(relayState = hyperStatConfig.relay6State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_SIX, 0.0))
        }
        return configLogicalPointsList
    }


    fun create2PipeAnalogOutLogicalPoints(
        hyperStatConfig: HyperStatPipe2Configuration
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatConfig.analogOut1State.enabled) {
            val pointData: Triple<Any, Any, Any> = pipe2AnalogOutConfiguration(
                analogOutState = hyperStatConfig.analogOut1State,
                analogTag = "analog1"
            )
            val minPoint = (pointData.second as Pair<*, *>)
            val maxPoint = (pointData.third as Pair<*, *>)

            configLogicalPointsList.add(Triple(pointData.first as Point, Port.ANALOG_OUT_ONE, 0.0))
            configLogicalPointsList.add(
                Triple(
                    minPoint.first as Point, minPoint.second as Any, hyperStatConfig.analogOut1State.voltageAtMin
                )
            )
            configLogicalPointsList.add(
                Triple(
                    maxPoint.first as Point, maxPoint.second as Any, hyperStatConfig.analogOut1State.voltageAtMax
                )
            )
            createFanConfigForAnalogOut(
                HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(hyperStatConfig.analogOut1State),
                hyperStatConfig.analogOut1State.perAtFanLow,
                hyperStatConfig.analogOut1State.perAtFanMedium,
                hyperStatConfig.analogOut1State.perAtFanHigh,
                "analog1", configLogicalPointsList
            )

        }
        if (hyperStatConfig.analogOut2State.enabled) {
            val pointData: Triple<Any, Any, Any> = pipe2AnalogOutConfiguration(
                analogOutState = hyperStatConfig.analogOut2State,
                analogTag = "analog2"
            )
            val minPoint = (pointData.second as Pair<*, *>)
            val maxPoint = (pointData.third as Pair<*, *>)

            configLogicalPointsList.add(Triple(pointData.first as Point, Port.ANALOG_OUT_TWO, 0.0))
            configLogicalPointsList.add(
                Triple(
                    minPoint.first as Point, minPoint.second as Any, hyperStatConfig.analogOut2State.voltageAtMin
                )
            )
            configLogicalPointsList.add(
                Triple(
                    maxPoint.first as Point, maxPoint.second as Any, hyperStatConfig.analogOut2State.voltageAtMax
                )
            )
            createFanConfigForAnalogOut(
                HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(hyperStatConfig.analogOut2State),
                hyperStatConfig.analogOut2State.perAtFanLow,
                hyperStatConfig.analogOut2State.perAtFanMedium,
                hyperStatConfig.analogOut2State.perAtFanHigh,
                "analog2", configLogicalPointsList
            )

        }
        if (hyperStatConfig.analogOut3State.enabled) {
            val pointData: Triple<Any, Any, Any> = pipe2AnalogOutConfiguration(
                analogOutState = hyperStatConfig.analogOut3State,
                analogTag = "analog3"
            )
            val minPoint = (pointData.second as Pair<*, *>)
            val maxPoint = (pointData.third as Pair<*, *>)

            configLogicalPointsList.add(Triple(pointData.first as Point, Port.ANALOG_OUT_THREE, 0.0))
            configLogicalPointsList.add(
                Triple(
                    minPoint.first as Point, minPoint.second as Any, hyperStatConfig.analogOut3State.voltageAtMin
                )
            )
            configLogicalPointsList.add(
                Triple(
                    maxPoint.first as Point, maxPoint.second as Any, hyperStatConfig.analogOut3State.voltageAtMax
                )
            )
            createFanConfigForAnalogOut(
                HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(hyperStatConfig.analogOut3State),
                hyperStatConfig.analogOut3State.perAtFanLow,
                hyperStatConfig.analogOut3State.perAtFanMedium,
                hyperStatConfig.analogOut3State.perAtFanHigh,
                "analog3", configLogicalPointsList
            )

        }

        return configLogicalPointsList
    }

    fun pipe2AnalogOutConfiguration(analogOutState: Pipe2AnalogOutState, analogTag: String): Triple<Any, Any, Any> {
        //   AnalogOut can be Associated  to these all state
        //   WATER VALVE, FAN_SPEED, DCV_DAMPER
        return when {
            HyperStatAssociationUtil.isAnalogOutMappedToWaterValve(analogOut = analogOutState) -> {
                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "water",
                    markers = arrayOf("water","valve")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForWaterValve(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_COOLING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_COOLING),
                )
            }

            HyperStatAssociationUtil.isAnalogOutMappedToFanSpeed(analogOut = analogOutState) -> {
                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "fanspeed",
                    markers = arrayOf("fan","speed")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForFanSpeed(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_FAN_SPEED),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_FAN_SPEED),
                )
            }


            HyperStatAssociationUtil.isAnalogOutMappedToDcvDamper(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "dcvdamper",
                    markers = arrayOf("dcv","damper")
                )
                Triple(
                    LogicalPointsUtil.createAnalogOutPointForDCVDamper(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_DCV_DAMPER),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_DCV_DAMPER),
                )
            }
            // Need to check how how handle this
            else -> Triple(Point.Builder().build(), Point.Builder().build(), Point.Builder().build())
        }
    }

    /***   Reading logical points  **/
    fun getPipe2RelayLogicalPoint(association: Pipe2RelayAssociation): Point{
        return when(association){
            Pipe2RelayAssociation.FAN_LOW_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanLowRelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.FAN_MEDIUM_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanMediumRelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.FAN_HIGH_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanHighRelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.AUX_HEATING_STAGE1-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingAux1RelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.AUX_HEATING_STAGE2-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingAux2RelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.WATER_VALVE-> Point.Builder().setHashMap(LogicalPointsUtil.readWaterValveRelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.FAN_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanEnabledRelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.OCCUPIED_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readOccupiedEnabledRelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.HUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readHumidifierRelayLogicalPoint(equipRef)).build()
            Pipe2RelayAssociation.DEHUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readDeHumidifierRelayLogicalPoint(equipRef)).build()
        }
    }
    fun getPipe2AnalogOutLogicalPoint(association: Pipe2AnalogOutAssociation): Point{
        return when(association){
            Pipe2AnalogOutAssociation.FAN_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutFanSpeedLogicalPoint(equipRef)).build()
            Pipe2AnalogOutAssociation.WATER_VALVE-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutWaterValveLogicalPoint(equipRef)).build()
            Pipe2AnalogOutAssociation.DCV_DAMPER-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutDcvLogicalPoint(equipRef)).build()
        }
    }

    // No restrictions so always auto mode is selected
    fun getPipe2DefaultFanSpeed(config: HyperStatPipe2Configuration): StandaloneFanStage {
        return if (HyperStatAssociationUtil.isAnyPipe2AnalogAssociatedToFanSpeed(config)
            || HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToFanLow(config)
            || HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToFanMedium(config)
            || HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToFanHigh(config)){
            StandaloneFanStage.AUTO
        }
        else StandaloneFanStage.OFF
    }
    fun getPipe2DefaultConditioningMode(): StandaloneConditioningMode {
        return StandaloneConditioningMode.AUTO
    }


    // No restrictions so always auto mode is selected
    fun getHpuDefaultFanSpeed(config: HyperStatHpuConfiguration): StandaloneFanStage {
        return if (HyperStatAssociationUtil.isAnyHpuAnalogAssociatedToFanSpeed(config)
            || HyperStatAssociationUtil.isAnyHpuRelayAssociatedToFanLow(config)
            || HyperStatAssociationUtil.isAnyHpuRelayAssociatedToFanMedium(config)
            || HyperStatAssociationUtil.isAnyHpuRelayAssociatedToFanHigh(config)){
            StandaloneFanStage.AUTO
        }
        else StandaloneFanStage.OFF

    }
    fun getHpuDefaultConditioningMode(): StandaloneConditioningMode {
        return StandaloneConditioningMode.AUTO
    }


    // HPU Configuration

    fun createHpuRelayLogicalPoints(hyperStatConfig: HyperStatHpuConfiguration):  MutableList<Triple<Point, Any, Any>> {
        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if(hyperStatConfig.relay1State.enabled){
            val pointData: Point = relayHpuConfiguration(relayState = hyperStatConfig.relay1State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_ONE, 0.0))
        }
        if(hyperStatConfig.relay2State.enabled){
            val pointData: Point = relayHpuConfiguration(relayState = hyperStatConfig.relay2State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_TWO, 0.0))
        }
        if(hyperStatConfig.relay3State.enabled){
            val pointData: Point = relayHpuConfiguration(relayState = hyperStatConfig.relay3State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_THREE, 0.0))
        }
        if(hyperStatConfig.relay4State.enabled){
            val pointData: Point = relayHpuConfiguration(relayState = hyperStatConfig.relay4State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FOUR, 0.0))
        }
        if(hyperStatConfig.relay5State.enabled){
            val pointData: Point = relayHpuConfiguration(relayState = hyperStatConfig.relay5State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FIVE, 0.0))
        }
        if(hyperStatConfig.relay6State.enabled){
            val pointData: Point = relayHpuConfiguration(relayState = hyperStatConfig.relay6State)
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_SIX, 0.0))
        }
        return configLogicalPointsList
    }


    fun relayHpuConfiguration(relayState: HpuRelayState): Point {

        //      Relay Can be Associated to these all states
        //      Fan low speed, Fan medium speed,Fan highspeed,Aux Heating stage1,
        //      Aux Heating Stage2,Water Valve, Fan Enable,Occupied Enable, Humidifier,Dehumidifier

        return when {
            HyperStatAssociationUtil.isHpuRelayCompressorStage1(relayState = relayState) ->
                return  LogicalPointsUtil.createCompressorStage1Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayCompressorStage2(relayState = relayState) ->
                return LogicalPointsUtil.createCompressorStage2Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayCompressorStage3(relayState = relayState) ->
                return LogicalPointsUtil.createCompressorStage3Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayAuxHeatingStage1(relayState = relayState) ->
                return LogicalPointsUtil.createAuxHeatingStage1Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayAuxHeatingStage2(relayState = relayState) ->
                return LogicalPointsUtil.createAuxHeatingStage2Point(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayFanLowSpeed(relayState = relayState) ->
                return LogicalPointsUtil.createFanLowPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayFanMediumSpeed(relayState = relayState) ->
                return LogicalPointsUtil.createFanMediumPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayFanHighSpeed(relayState = relayState) ->
                return LogicalPointsUtil.createFanHighPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayFanEnabled(relayState = relayState) ->
                return LogicalPointsUtil.createPointForFanEnable(equipDis,siteRef,equipRef,roomRef,floorRef,tz)

            HyperStatAssociationUtil.isHpuRelayOccupiedEnabled(relayState = relayState) ->
                return LogicalPointsUtil.createPointForOccupiedEnabled(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayHumidifierEnabled(relayState = relayState) ->
                return LogicalPointsUtil.createPointForHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayDeHumidifierEnabled(relayState = relayState) ->
                return LogicalPointsUtil.createPointForDeHumidifier(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toShort())

            HyperStatAssociationUtil.isHpuRelayChangeOverCooling(relayState = relayState) ->
                return LogicalPointsUtil.createChangeOverCoolingPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())

            HyperStatAssociationUtil.isHpuRelayChangeOverHeating(relayState = relayState) ->
                return LogicalPointsUtil.createChangeOverHeatingPoint(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt())
            else -> Point.Builder().build()
        }

    }

    fun createHpuAnalogOutLogicalPoints(
        hyperStatConfig: HyperStatHpuConfiguration
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatConfig.analogOut1State.enabled) {
            val pointData: Triple<Any, Any, Any> = analogOutHpuConfiguration(
                analogOutState = hyperStatConfig.analogOut1State,
                analogTag = "analog1"
            )
            val minPoint = (pointData.second as Pair<*, *>)
            val maxPoint = (pointData.third as Pair<*, *>)

            configLogicalPointsList.add(Triple(pointData.first as Point, Port.ANALOG_OUT_ONE, 0.0))
            configLogicalPointsList.add(
                Triple(
                    minPoint.first as Point, minPoint.second as Any, hyperStatConfig.analogOut1State.voltageAtMin
                )
            )
            configLogicalPointsList.add(
                Triple(
                    maxPoint.first as Point, maxPoint.second as Any, hyperStatConfig.analogOut1State.voltageAtMax
                )
            )
            createFanConfigForAnalogOut(
                HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(hyperStatConfig.analogOut1State),
                hyperStatConfig.analogOut1State.perAtFanLow,
                hyperStatConfig.analogOut1State.perAtFanMedium,
                hyperStatConfig.analogOut1State.perAtFanHigh,
                "analog1", configLogicalPointsList
            )

        }
        if (hyperStatConfig.analogOut2State.enabled) {
            val pointData: Triple<Any, Any, Any> = analogOutHpuConfiguration(
                analogOutState = hyperStatConfig.analogOut2State,
                analogTag = "analog2"
            )
            val minPoint = (pointData.second as Pair<*, *>)
            val maxPoint = (pointData.third as Pair<*, *>)

            configLogicalPointsList.add(Triple(pointData.first as Point, Port.ANALOG_OUT_TWO, 0.0))
            configLogicalPointsList.add(
                Triple(
                    minPoint.first as Point, minPoint.second as Any, hyperStatConfig.analogOut2State.voltageAtMin
                )
            )
            configLogicalPointsList.add(
                Triple(
                    maxPoint.first as Point, maxPoint.second as Any, hyperStatConfig.analogOut2State.voltageAtMax
                )
            )
            createFanConfigForAnalogOut(
                HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(hyperStatConfig.analogOut2State),
                hyperStatConfig.analogOut2State.perAtFanLow,
                hyperStatConfig.analogOut2State.perAtFanMedium,
                hyperStatConfig.analogOut2State.perAtFanHigh,
                "analog2", configLogicalPointsList
            )

        }
        if (hyperStatConfig.analogOut3State.enabled) {
            val pointData: Triple<Any, Any, Any> = analogOutHpuConfiguration(
                analogOutState = hyperStatConfig.analogOut3State,
                analogTag = "analog3"
            )
            val minPoint = (pointData.second as Pair<*, *>)
            val maxPoint = (pointData.third as Pair<*, *>)

            configLogicalPointsList.add(Triple(pointData.first as Point, Port.ANALOG_OUT_THREE, 0.0))
            configLogicalPointsList.add(
                Triple(
                    minPoint.first as Point, minPoint.second as Any, hyperStatConfig.analogOut3State.voltageAtMin
                )
            )
            configLogicalPointsList.add(
                Triple(
                    maxPoint.first as Point, maxPoint.second as Any, hyperStatConfig.analogOut3State.voltageAtMax
                )
            )
            createFanConfigForAnalogOut(
                HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(hyperStatConfig.analogOut3State),
                hyperStatConfig.analogOut3State.perAtFanLow,
                hyperStatConfig.analogOut3State.perAtFanMedium,
                hyperStatConfig.analogOut3State.perAtFanHigh,
                "analog3", configLogicalPointsList
            )

        }

        return configLogicalPointsList
    }

    fun analogOutHpuConfiguration(analogOutState: HpuAnalogOutState, analogTag: String): Triple<Any, Any, Any> {
        //   AnalogOut can be Associated  to these all state
        //   COMPRESSOR_SPEED, FAN_SPEED, DCV_DAMPER
        return when {
            HyperStatAssociationUtil.isHpuAnalogOutMappedToCompressorSpeed(analogOut = analogOutState) -> {
                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "compressorspeed",
                    markers = arrayOf("compressor","speed")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutCompressorSpeedValve(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_COOLING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_COOLING),
                )
            }

            HyperStatAssociationUtil.isHpuAnalogOutMappedToFanSpeed(analogOut = analogOutState) -> {
                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "fanspeed",
                    markers = arrayOf("fan","speed")
                )

                Triple(
                    LogicalPointsUtil.createAnalogOutPointForFanSpeed(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_FAN_SPEED),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_FAN_SPEED),
                )
            }


            HyperStatAssociationUtil.isHpuAnalogOutMappedToDcvDamper(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "dcvdamper",
                    markers = arrayOf("dcv","damper")
                )
                Triple(
                    LogicalPointsUtil.createAnalogOutPointForDCVDamper(equipDis,siteRef,equipRef,roomRef,floorRef,tz, nodeAddress.toInt()),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_DCV_DAMPER),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_DCV_DAMPER),
                )
            }
            // Need to check how how handle this
            else -> Triple(Point.Builder().build(), Point.Builder().build(), Point.Builder().build())
        }
    }

    /***   Reading logical points  **/
    fun getHpuRelayLogicalPoint(association: HpuRelayAssociation): Point{
        return when(association){

            HpuRelayAssociation.COMPRESSOR_STAGE1-> Point.Builder().setHashMap(LogicalPointsUtil.readCompressorStage1RelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.COMPRESSOR_STAGE2-> Point.Builder().setHashMap(LogicalPointsUtil.readCompressorStage2RelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.COMPRESSOR_STAGE3-> Point.Builder().setHashMap(LogicalPointsUtil.readCompressorStage3RelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.AUX_HEATING_STAGE1-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingAux1RelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.AUX_HEATING_STAGE2-> Point.Builder().setHashMap(LogicalPointsUtil.readHeatingAux2RelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.FAN_LOW_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanLowRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.FAN_MEDIUM_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanMediumRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.FAN_HIGH_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanHighRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.FAN_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readFanEnabledRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.OCCUPIED_ENABLED-> Point.Builder().setHashMap(LogicalPointsUtil.readOccupiedEnabledRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.HUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readHumidifierRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.DEHUMIDIFIER-> Point.Builder().setHashMap(LogicalPointsUtil.readDeHumidifierRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.CHANGE_OVER_O_COOLING-> Point.Builder().setHashMap(LogicalPointsUtil.readChangeOverCoolingRelayLogicalPoint(equipRef)).build()
            HpuRelayAssociation.CHANGE_OVER_B_HEATING-> Point.Builder().setHashMap(LogicalPointsUtil.readChangeOverHeatingRelayLogicalPoint(equipRef)).build()
        }
    }
    fun getHpuAnalogOutLogicalPoint(association: HpuAnalogOutAssociation): Point{
        return when(association){
            HpuAnalogOutAssociation.COMPRESSOR_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutCompressorSpeedLogicalPoint(equipRef)).build()
            HpuAnalogOutAssociation.FAN_SPEED-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutFanSpeedLogicalPoint(equipRef)).build()
            HpuAnalogOutAssociation.DCV_DAMPER-> Point.Builder().setHashMap(LogicalPointsUtil.readAnalogOutDcvLogicalPoint(equipRef)).build()
        }
    }

    fun createStagedFanConfigPoint(
        hyperStatConfig: HyperStatCpuConfiguration,
    ): MutableList<Pair<Point, Any>> {

        val stagedFanConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        if (HyperStatAssociationUtil.isStagedFanEnabled(hyperStatConfig, CpuRelayAssociation.COOLING_STAGE_1)) {
            val coolingStage1FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "cooling", "rate", "output", "sp", "stage1"
            )

            val coolingStage1FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutCoolingStage1",
                coolingStage1FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(coolingStage1FanConfigPoint, hyperStatConfig.coolingStage1FanState)
            )
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(hyperStatConfig, CpuRelayAssociation.COOLING_STAGE_2)) {
            val coolingStage2FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "cooling", "rate", "output", "sp", "stage2"
            )
            val coolingStage2FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutCoolingStage2",
                coolingStage2FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(coolingStage2FanConfigPoint, hyperStatConfig.coolingStage2FanState)
            )
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(hyperStatConfig, CpuRelayAssociation.COOLING_STAGE_3)) {
            val coolingStage3FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "cooling", "rate", "output", "sp", "stage3"
            )

            val coolingStage3FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutCoolingStage3",
                coolingStage3FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(coolingStage3FanConfigPoint, hyperStatConfig.coolingStage3FanState)
            )
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(hyperStatConfig, CpuRelayAssociation.HEATING_STAGE_1)) {
            val heatingStage1FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "heating", "rate", "output", "sp", "stage1"
            )

            val heatingStage1FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutHeatingStage1",
                heatingStage1FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(heatingStage1FanConfigPoint, hyperStatConfig.heatingStage1FanState)
            )
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(hyperStatConfig, CpuRelayAssociation.HEATING_STAGE_2)) {
            val heatingStage2FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "heating", "rate", "output", "sp", "stage2"
            )

            val heatingStage2FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutHeatingStage2",
                heatingStage2FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(heatingStage2FanConfigPoint, hyperStatConfig.heatingStage2FanState)
            )
        }

        if (HyperStatAssociationUtil.isStagedFanEnabled(hyperStatConfig, CpuRelayAssociation.HEATING_STAGE_3)) {
            val heatingStage3FanConfigPointMarkers = arrayOf(
                "config", "writable", "zone", "fan", "heating", "rate", "output", "sp", "stage3"
            )

            val heatingStage3FanConfigPoint = createHaystackPointWithUnit(
                "$equipDis-fanOutHeatingStage3",
                heatingStage3FanConfigPointMarkers,
                null, "V"
            )
            stagedFanConfigPointsList.add(
                Pair(heatingStage3FanConfigPoint, hyperStatConfig.heatingStage3FanState)
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
            newConfiguration: HyperStatCpuConfiguration,
            stage: CpuRelayAssociation
        ): MutableList<Pair<Point, Any>> {
            val stagedFanConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

            when (stage) {
                CpuRelayAssociation.COOLING_STAGE_1 -> {
                    val coolingStage1FanConfigPoint = createFanConfigPoint(
                        "$equipDis-fanOutCoolingStage1",
                        arrayOf("config", "writable", "zone", "fan", "cooling","output", "sp", "stage1"),
                        newConfiguration.coolingStage1FanState
                    )
                    stagedFanConfigPointsList.add(coolingStage1FanConfigPoint)
                }
                CpuRelayAssociation.COOLING_STAGE_2 -> {
                    val coolingStage2FanConfigPoint = createFanConfigPoint(
                        "$equipDis-fanOutCoolingStage2",
                        arrayOf("config", "writable", "zone", "fan", "cooling","output", "sp", "stage2"),
                        newConfiguration.coolingStage2FanState
                    )
                    stagedFanConfigPointsList.add(coolingStage2FanConfigPoint)
                }
                CpuRelayAssociation.COOLING_STAGE_3 -> {
                    val coolingStage3FanConfigPoint = createFanConfigPoint(
                        "$equipDis-fanOutCoolingStage3",
                        arrayOf("config", "writable", "zone", "fan", "cooling","output", "sp", "stage3"),
                        newConfiguration.coolingStage3FanState
                    )
                    stagedFanConfigPointsList.add(coolingStage3FanConfigPoint)
                }
                CpuRelayAssociation.HEATING_STAGE_1 -> {
                    val heatingStage1FanConfigPoint = createFanConfigPoint(
                        "$equipDis-fanOutHeatingStage1",
                        arrayOf("config", "writable", "zone", "fan", "heating", "output", "sp", "stage1"),
                        newConfiguration.heatingStage1FanState
                    )
                    stagedFanConfigPointsList.add(heatingStage1FanConfigPoint)
                }
                CpuRelayAssociation.HEATING_STAGE_2 -> {
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



