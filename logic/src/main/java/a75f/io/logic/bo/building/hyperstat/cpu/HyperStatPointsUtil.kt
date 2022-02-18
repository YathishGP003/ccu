package a75f.io.logic.bo.building.hyperstat.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil
import a75f.io.logic.bo.building.hyperstat.common.LogicalKeyID
import a75f.io.logic.tuners.TunerConstants
import android.util.Log
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Manjunath K on 30-07-2021.
 */
class HyperStatPointsUtil constructor(
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

        const val HYPERSTAT = "hyperstat"

        // function to create equip Point
        fun createHyperStatEquipPoint(
            profileName: String, siteRef: String, roomRef: String, floorRef: String, priority: String,
            gatewayRef: String, tz: String, nodeAddress: String, equipDis: String, hayStackAPI: CCUHsApi
        )
                : String {
            val equipPoint = Equip.Builder()
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setProfile(ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name)
                .setPriority(priority)
                .addMarker(HYPERSTAT).addMarker("equip")
                .addMarker("zone").addMarker(profileName)
                .setGatewayRef(gatewayRef)
                .setTz(tz)
                .setGroup(nodeAddress)
                .setDisplayName(equipDis)

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

            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

        // add specific markers
        markers.forEach { point.addMarker(it) }
        return point.build()
    }

    private fun createHaystackPointWithEnums(
        displayName: String, markers: Array<String>, hisInterpolate: String, enums: String
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
            .setHisInterpolate(hisInterpolate)
            .setEnums(enums)

            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

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

            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithUnitEnum(
        displayName: String, markers: Array<String>, unit: String, enums: String
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
            .setUnit(unit)
            .setHisInterpolate("cov")
            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

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
            .setHisInterpolate("cov")
            .setShortDis(shortDis)
            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithMinMaxIncUnitInc(
        displayName: String, markers: Array<String>, min: String, max: String, inc: String, unit: String,
        hisInterpolate: String, shortDis: String): Point {
        // Point which has default details
        val point = Point.Builder()
            .setDisplayName(displayName)
            .setSiteRef(siteRef)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setTz(tz)
            .setGroup(nodeAddress)
            .setShortDis(shortDis)
            .setMinVal(min)
            .setMaxVal(max)
            .setIncrementVal(inc)
            .setUnit(unit)
            .setHisInterpolate(hisInterpolate)
            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

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
            .setHisInterpolate("cov")

            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }

    private fun createHaystackPointWithUnit(
        displayName: String, markers: Array<String>, hisInterpolate: String, unit: String
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
            .setHisInterpolate(hisInterpolate)
            .setUnit(unit)
            // add common  markers
            .addMarker(HYPERSTAT).addMarker(profileName)

        // add specific markers
        markers.forEach { point.addMarker(it) }

        return point.build()
    }


    // Adding Point to Haystack and returns point id
    fun addPointToHaystack(point: Point): String {
        return hayStackAPI.addPoint(point)
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
                if(actualPoint.first.markers.contains("his")){
                    addDefaultHisValueForPoint(pointId, actualPoint.second)
                }
                addDefaultValueForPoint(pointId, actualPoint.second)
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
            val pointId = addPointToHaystack(it.first)
            pointsIdMap[it.second] = pointId
            if (it.first.markers.contains("his")) {
                addDefaultHisValueForPoint(pointId, it.third)
            }
            addDefaultValueForPoint(pointId, it.third)
        }
        return pointsIdMap
    }




    // Points to hold loop output value
     fun createLoopOutputPoints(): MutableList<Pair<Point, Any>> {
        val loopOutputPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val coolingLoopOutputPointMarker = arrayOf(
            "cooling",  "zone", "cmd", "his", "logical", "out","loop","output","modulating"
        )
        val heatingLoopOutputPointMarker = arrayOf(
            "heating",  "zone", "cmd", "his", "logical", "out","loop","output","modulating"
        )
        val fanLoopOutputPointMarker = arrayOf(
            "fan",  "zone", "cmd", "his", "logical", "out","loop","output","modulating"
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
        return loopOutputPointsList
    }



    /**
     * Functions which creates configuration enable/disable Points
     */

    // Function which creates Relay  config Points
    fun createIsRelayEnabledConfigPoints(hyperStatConfig: HyperStatCpuConfiguration): MutableList<Pair<Point, Any>> {

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
        val relay1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay1OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )

        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay1AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay1OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )

        relayMarkers.remove("association")
        relayMarkers.remove("relay1")


        /**  Relay 2 config and association point */
        relayMarkers.add("relay2")
        relayMarkers.add("enabled")
        val relay2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay2OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )

        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay2AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay2OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay2")

        /**  Relay 3 config and association point */
        relayMarkers.add("relay3")
        relayMarkers.add("enabled")
        val relay3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay3OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay3AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay3OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay3")

        /**  Relay 4config and association point */
        relayMarkers.add("relay4")
        relayMarkers.add("enabled")
        val relay4Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay4OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay4AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay4OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay4")

        /**  Relay 5 config and association point */
        relayMarkers.add("relay5")
        relayMarkers.add("enabled")
        val relay5Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay5OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay5AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay5OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay5")

        /**  Relay 6 config and association point */
        relayMarkers.add("relay6")
        relayMarkers.add("enabled")
        val relay6Point = createHaystackPointWithOnlyEnum(
            "$equipDis-relay6OutputEnabled",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        relayMarkers.add("association")
        relayMarkers.remove("enabled")
        val relay6AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-relay6OutputAssociation",
            relayMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )
        relayMarkers.remove("association")
        relayMarkers.remove("relay6")

        // Enable disable point
        configRelayPointsList.add(Pair(relay1Point, if (hyperStatConfig.relay1State.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay2Point, if (hyperStatConfig.relay2State.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay3Point, if (hyperStatConfig.relay3State.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay4Point, if (hyperStatConfig.relay4State.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay5Point, if (hyperStatConfig.relay5State.enabled) 1.0 else 0.0))
        configRelayPointsList.add(Pair(relay6Point, if (hyperStatConfig.relay6State.enabled) 1.0 else 0.0))

        // Association  point
        configRelayPointsList.add(Pair(relay1AssociationPoint, hyperStatConfig.relay1State.association.ordinal))
        configRelayPointsList.add(Pair(relay2AssociationPoint, hyperStatConfig.relay2State.association.ordinal))
        configRelayPointsList.add(Pair(relay3AssociationPoint, hyperStatConfig.relay3State.association.ordinal))
        configRelayPointsList.add(Pair(relay4AssociationPoint, hyperStatConfig.relay4State.association.ordinal))
        configRelayPointsList.add(Pair(relay5AssociationPoint, hyperStatConfig.relay5State.association.ordinal))
        configRelayPointsList.add(Pair(relay6AssociationPoint, hyperStatConfig.relay6State.association.ordinal))


        return configRelayPointsList
    }

    //Function which creates analog out config Point
    fun createIsAnalogOutEnabledConfigPoints(hyperStatConfig: HyperStatCpuConfiguration): MutableList<Pair<Point, Any>> {

        val configAnalogOutPointsList: MutableList<Pair<Point, Any>> = LinkedList()


        val analogMarkers: MutableList<String> = LinkedList()
        analogMarkers.addAll(
            arrayOf("config", "writable", "zone", "out")
        )

        /**  analog 1 config and association point */
        analogMarkers.add("analog1")
        analogMarkers.add("enabled")
        val analogOut1Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut1Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")
        val analogOut1AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut1Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getAnalogOutConfigEnum()
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog1")


        /**  analog 2 config and association point */
        analogMarkers.add("analog2")
        analogMarkers.add("enabled")
        val analogOut2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut2Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")

        val analogOut2AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut2Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog2")


        /**  analog 3 config and association point */
        analogMarkers.add("analog3")
        analogMarkers.add("enabled")
        val analogOut3Point = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut3Enabled",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            "off,on"
        )
        analogMarkers.add("association")
        analogMarkers.remove("enabled")

        val analogOut3AssociationPoint = createHaystackPointWithOnlyEnum(
            "$equipDis-analogOut3Association",
            analogMarkers.stream().toArray { arrayOfNulls(it) },
            getRelayConfigEnum()
        )
        analogMarkers.remove("association")
        analogMarkers.remove("analog3")

        // Analog out Enable/Disable Points
        configAnalogOutPointsList.add(Pair(analogOut1Point, if (hyperStatConfig.analogOut1State.enabled) 1.0 else 0.0))
        configAnalogOutPointsList.add(Pair(analogOut2Point, if (hyperStatConfig.analogOut2State.enabled) 1.0 else 0.0))
        configAnalogOutPointsList.add(Pair(analogOut3Point, if (hyperStatConfig.analogOut3State.enabled) 1.0 else 0.0))

        // Analog out Association Points
        configAnalogOutPointsList.add(
            Pair(
                analogOut1AssociationPoint,
                hyperStatConfig.analogOut1State.association.ordinal
            )
        )
        configAnalogOutPointsList.add(
            Pair(
                analogOut2AssociationPoint,
                hyperStatConfig.analogOut2State.association.ordinal
            )
        )
        configAnalogOutPointsList.add(
            Pair(
                analogOut3AssociationPoint,
                hyperStatConfig.analogOut3State.association.ordinal
            )
        )

        return configAnalogOutPointsList
    }

    // Function which Creates Analog in config Points
    fun createIsAnalogInEnabledConfigPoints(hyperStatConfig: HyperStatCpuConfiguration): MutableList<Pair<Point, Any>> {

        val configAnalogInPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val analogInEnum = "false,true"

        val analogMarkers: MutableList<String> = LinkedList()
        analogMarkers.addAll(
            arrayOf("config", "writable", "zone", "in")
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
        configAnalogInPointsList.add(Pair(analogIn1Point, if (hyperStatConfig.analogIn1State.enabled) 1.0 else 0.0))
        configAnalogInPointsList.add(Pair(analogIn2Point, if (hyperStatConfig.analogIn2State.enabled) 1.0 else 0.0))

        // Analog in Association points
        configAnalogInPointsList.add(
            Pair(
                analogIn1AssociationPoint,
                hyperStatConfig.analogIn1State.association.ordinal
            )
        )
        configAnalogInPointsList.add(
            Pair(
                analogIn2AssociationPoint,
                hyperStatConfig.analogIn2State.association.ordinal
            )
        )

        return configAnalogInPointsList
    }

    // Function which creates Th1,Th2 config Points
    fun createIsThermistorEnabledConfigPoints(hyperStatConfig: HyperStatCpuConfiguration): MutableList<Pair<Point, Any>> {
        val thEnum = "false,true"
        val configThPointsList: MutableList<Pair<Point, Any>> = LinkedList()
        val th1PointMarkers = arrayOf(
            "temp", "sensor", "zone", "config", "enabled", "writable", "th1", "airflow", "discharge"
        )
        val th2PointMarkers = arrayOf(
            "temp", "sensor", "zone", "config", "enabled", "writable", "th2", "window"
        )
        val th1Point = createHaystackPointWithUnitEnum(
            "$equipDis-enableAirflowTempSensorTh1",
            th1PointMarkers,
            "\u00B0F",
            thEnum,
        )
        val th2Point = createHaystackPointWithOnlyEnum(
            "$equipDis-enableDoorWindowSensorTh2",
            th2PointMarkers,
            thEnum
        )

        configThPointsList.add(Pair(th1Point, if (hyperStatConfig.isEnableAirFlowTempSensor) 1.0 else 0.0))
        configThPointsList.add(Pair(th2Point, if (hyperStatConfig.isEnableDoorWindowSensor) 1.0 else 0.0))

        return configThPointsList
    }


    fun createAutoForceAutoAwayConfigPoints(
        hyperStatConfig: HyperStatCpuConfiguration
    ): MutableList<Pair<Point, Any>> {

        val autoForceAutoAwayConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val enableAutoForceOccupancyControlPointMarkers = arrayOf(
            "config", "writable", "zone", "auto", "occupancy", "enabled", "control", "forced","his"
        )
        val enableAutoAwayControlPointMarkers = arrayOf(
            "config", "writable", "zone", "auto", "away", "enabled", "control","his"
        )

        // isEnableAutoForceOccupied Point
        val enableAutoForceOccupancyControlPoint = createHaystackPointWithEnums(
            "$equipDis-AutoForceOccupied",
            enableAutoForceOccupancyControlPointMarkers,
            "cov",
            "off,on"
        )

        // isEnableAutoAway Point
        val enableAutoAwayControlPointPoint = createHaystackPointWithEnums(
            "$equipDis-AutoAway",
            enableAutoAwayControlPointMarkers,
            "cov",
            "off,on"
        )

        autoForceAutoAwayConfigPointsList.add(
            Pair(
                enableAutoForceOccupancyControlPoint,
                if (hyperStatConfig.isEnableAutoForceOccupied) 1.0 else 0.0
            )
        )
        autoForceAutoAwayConfigPointsList.add(
            Pair(
                enableAutoAwayControlPointPoint,
                if (hyperStatConfig.isEnableAutoAway) 1.0 else 0.0
            )
        )

        return autoForceAutoAwayConfigPointsList
    }


    // Function which creates co2 Points
    fun createPointCO2ConfigPoint(
        hyperStatConfig: HyperStatCpuConfiguration
    ): MutableList<Pair<Point, Any>> {

        val co2ConfigPointsList: MutableList<Pair<Point, Any>> = LinkedList()

        val co2DamperOpeningRatePointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "damper", "opening", "rate"
        )
        val zoneCO2ThresholdPointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "threshold", "sp"
        )
        val zoneCO2TargetPointMarkers = arrayOf(
            "config", "writable", "his", "zone", "co2", "target", "sp"
        )


        val co2DamperOpeningRatePoint = createHaystackPointWithUnit(
            "$equipDis-co2DamperOpeningRate",
            co2DamperOpeningRatePointMarkers,
            "","%"
        )


        val zoneCO2ThresholdPointPoint = createHaystackPointWithUnit(
            "$equipDis-zoneCO2Threshold",
            zoneCO2ThresholdPointMarkers,
            "","ppm"
        )

        val zoneCO2TargetPointPoint = createHaystackPointWithUnit(
            "$equipDis-zoneCO2Target",
            zoneCO2TargetPointMarkers,
            "","ppm"
        )

        co2ConfigPointsList.add(
            Pair(co2DamperOpeningRatePoint, hyperStatConfig.zoneCO2DamperOpeningRate)
        )
        co2ConfigPointsList.add(
            Pair(zoneCO2ThresholdPointPoint, hyperStatConfig.zoneCO2Threshold)
        )
        co2ConfigPointsList.add(
            Pair(zoneCO2TargetPointPoint, hyperStatConfig.zoneCO2Target)
        )

        return co2ConfigPointsList
    }


    // Function which creates User Intent Points
    fun createUserIntentPoints(hyperStatConfig: HyperStatCpuConfiguration): MutableList<Pair<Point, Any>> {

        val userIntentPointList: MutableList<Pair<Point, Any>> = LinkedList()

        val fanOperationsModePointMarkers = arrayOf(
            "userIntent", "fan", "writable", "zone", "operation", "mode", "his", "control"
        )
        val conditioningModePointMarkers = arrayOf(
            "userIntent", "writable", "zone", "mode", "his", "control", "conditioning", "temp"
        )
        val dehumidifierPointMarkers = arrayOf(
            "userIntent", "writable", "zone", "dehumidifier", "sp", "his", "control", "target"
        )
        val humidifierPointMarkers = arrayOf(
            "userIntent", "writable", "zone", "humidifier", "sp", "his", "control", "target"
        )

        val fanOperationsModePointEnums = "off,auto,low,medium,high"
        val conditioningModePointEnums = "off,auto,heatonly,coolonly"

        val defaultFanMode: StandaloneFanStage = getDefaultFanSpeed(hyperStatConfig)
        val defaultConditioningMode: StandaloneConditioningMode = getDefaultConditioningMode(hyperStatConfig)

        val fanOperationsModePoint = createHaystackPointWithEnums(
            displayName = "$equipDis-FanOpMode",
            fanOperationsModePointMarkers,
            "cov",
            fanOperationsModePointEnums
        )

        val conditioningModePointPoint = createHaystackPointWithEnums(
            displayName = "$equipDis-ConditioningMode",
            conditioningModePointMarkers,
            "cov",
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


    // Function creates configurable logical Point
    fun createConfigRelayLogicalPoints(hyperStatConfig: HyperStatCpuConfiguration): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()
        //writeHisValById default value = 0.0
        if (hyperStatConfig.relay1State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay1State, "relay1")
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_ONE, 0.0))
        }
        if (hyperStatConfig.relay2State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay2State, "relay2")
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_TWO, 0.0))
        }
        if (hyperStatConfig.relay3State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay3State, "relay3")
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_THREE, 0.0))
        }
        if (hyperStatConfig.relay4State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay4State, "relay4")
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FOUR, 0.0))
        }
        if (hyperStatConfig.relay5State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay5State, "relay5")
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_FIVE, 0.0))
        }
        if (hyperStatConfig.relay6State.enabled) {
            val pointData: Point = relayConfiguration(relayState = hyperStatConfig.relay6State, "relay6")
            configLogicalPointsList.add(Triple(pointData, Port.RELAY_SIX, 0.0))
        }



        return configLogicalPointsList
    }

    fun createConfigAnalogOutLogicalPoints(
        hyperStatConfig: HyperStatCpuConfiguration
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatConfig.analogOut1State.enabled) {
            val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
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
                hyperStatConfig.analogOut1State, "analog1", configLogicalPointsList
            )

        }

        if (hyperStatConfig.analogOut2State.enabled) {
            val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
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
                hyperStatConfig.analogOut2State, "analog2", configLogicalPointsList
            )
        }

        if (hyperStatConfig.analogOut3State.enabled) {
            val pointData: Triple<Any, Any, Any> = analogOutConfiguration(
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
                hyperStatConfig.analogOut3State, "analog3", configLogicalPointsList
            )
        }

        return configLogicalPointsList
    }


     fun createFanConfigForAnalogOut(
        analogOutState: AnalogOutState, analogTag: String,
        configLogicalPointsList: MutableList<Triple<Point, Any, Any>>
    ) {
        if (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) {
            val pointData: Triple<Point, Point, Point> = createFanLowMediumHighPoint(analogTag)
            configLogicalPointsList.add(
                Triple(pointData.first, LogicalKeyID.FAN_LOW, analogOutState.perAtFanLow)
            )
            configLogicalPointsList.add(
                Triple(pointData.second, LogicalKeyID.FAN_MEDIUM, analogOutState.perAtFanMedium)
            )
            configLogicalPointsList.add(
                Triple(pointData.third, LogicalKeyID.FAN_HIGH, analogOutState.perAtFanHigh)
            )
        }
    }

    fun createConfigAnalogInLogicalPoints(
        hyperStatConfig: HyperStatCpuConfiguration
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        if (hyperStatConfig.analogIn1State.enabled) {
            val pointData: Point = analogInConfiguration(
                analogInState = hyperStatConfig.analogIn1State,
                analogTag = "analog1"
            )
            configLogicalPointsList.add(Triple(pointData, Port.ANALOG_IN_ONE, 0.0))
        }
        if (hyperStatConfig.analogIn2State.enabled) {
            val pointData: Point = analogInConfiguration(
                analogInState = hyperStatConfig.analogIn2State,
                analogTag = "analog2"
            )
            configLogicalPointsList.add(Triple(pointData, Port.ANALOG_IN_TWO, 0.0))
        }

        return configLogicalPointsList
    }

    fun createConfigThermisterInLogicalPoints(
        hyperStatConfig: HyperStatCpuConfiguration
    ): MutableList<Triple<Point, Any, Any>> {

        val configLogicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()
        if (hyperStatConfig.isEnableAirFlowTempSensor) {
            val pointData: Point = createPointForAirflowTempSensor()
            configLogicalPointsList.add(Triple(pointData, Port.TH1_IN, 0.0))
        }
        if (hyperStatConfig.isEnableDoorWindowSensor) {
            val doorWindowSensorTh2Point = createPointForDoorWindowSensor("th2")
            configLogicalPointsList.add(Triple(doorWindowSensorTh2Point, Port.TH2_IN, 0.0))
        }
        return configLogicalPointsList
    }


    fun relayConfiguration(relayState: RelayState, relayTag: String): Point {

        //      Relay Can be Associated to these all states
        //      COOLING_STAGE_1,COOLING_STAGE_2,COOLING_STAGE_3,HEATING_STAGE_1,HEATING_STAGE_2,HEATING_STAGE_3
        //      FAN_LOW_SPEED,FAN_MEDIUM_SPEED,FAN_HIGH_SPEED,FAN_ENABLED,OCCUPIED_ENABLED,HUMIDIFIER,DEHUMIDIFIER

        return when {
            HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(relayState = relayState) ->
                createCoolingStagesPoint(relayState = relayState, relayTag = relayTag)
            HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(relayState = relayState) ->
                createHeatingStagesPoint(relayState = relayState, relayTag = relayTag)
            HyperStatAssociationUtil.isRelayAssociatedToFan(relayState = relayState) ->
                createFanStagesPoint(relayState = relayState, relayTag = relayTag)
            HyperStatAssociationUtil.isRelayAssociatedToFanEnabled(relayState = relayState) ->
                createPointForFanEnable(relayTag = relayTag)
            HyperStatAssociationUtil.isRelayAssociatedToOccupiedEnabled(relayState = relayState) ->
                createPointForOccupiedEnabled(relayTag = relayTag)
            HyperStatAssociationUtil.isRelayAssociatedToHumidifier(relayState = relayState) ->
                createPointForHumidifier(relayTag = relayTag)
            HyperStatAssociationUtil.isRelayAssociatedToDeHumidifier(relayState = relayState) ->
                createPointForDeHumidifier(relayTag = relayTag)
            else -> Point.Builder().build()
        }

    }

    fun analogOutConfiguration(analogOutState: AnalogOutState, analogTag: String): Triple<Any, Any, Any> {
        //   AnalogOut can be Associated  to these all state
        //   COOLING, FAN_SPEED, HEATING, DCV_DAMPER
        return when {
            HyperStatAssociationUtil.isAnalogOutAssociatedToCooling(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "cooling"
                )

                Triple(
                    createAnalogOutPointForCooling(analogTag = analogTag),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_COOLING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_COOLING),
                )
            }

            HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOut = analogOutState) -> {
                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "fanspeed"
                )

                Triple(
                    createAnalogOutPointForFanSpeed(analogTag = analogTag),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_FAN_SPEED),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_FAN_SPEED),
                )
            }

            HyperStatAssociationUtil.isAnalogOutAssociatedToHeating(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "heating"
                )

                Triple(
                    createAnalogOutPointForHeating(analogTag = analogTag),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_HEATING),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_HEATING),
                )
            }

            HyperStatAssociationUtil.isAnalogOutAssociatedToDcvDamper(analogOut = analogOutState) -> {

                val minMaxPoint: Pair<Any, Any> = createMinMaxPointForAnalogOut(
                    analogTag = analogTag,
                    associationType = "dcvdamper"
                )
                Log.i(L.TAG_CCU_HSCPU, "Reconfiguration dcvdamper ")
                Triple(
                    createAnalogOutPointForDCVDamper(analogTag = analogTag),
                    Pair(minMaxPoint.first, LogicalKeyID.MIN_DCV_DAMPER),
                    Pair(minMaxPoint.second, LogicalKeyID.MAX_DCV_DAMPER),
                )
            }
            // Need to check how how handle this
            else -> Triple(Point.Builder().build(), Point.Builder().build(), Point.Builder().build())
        }
    }

    fun analogInConfiguration(analogInState: AnalogInState, analogTag: String): Point {
        return when {
            (HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX10(analogInState)) -> {
                createPointForCurrentTx(
                    analogTag, "Current Drawn[CT 0-10]",
                    min = "0", max = "10", inc = "0.1", unit = "amps"
                )
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX20(analogInState)) -> {
                createPointForCurrentTx(
                    analogTag, "Current Drawn[CT 0-20]",
                    min = "0", max = "20", inc = "0.1", unit = "amps"
                )
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX50(analogInState)) -> {
                createPointForCurrentTx(
                    analogTag, "Current Drawn[CT 0-50]",
                    min = "0", max = "50", inc = "0.1", unit = "amps"
                )
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(analogInState)) -> {
                createPointForDoorWindowSensor(analogTag)
            }
            (HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(analogInState)) -> {
                createPointForKeyCardSensor(analogTag)
            }
            else -> Point.Builder().build()
        }
    }


    /**
     * Functions which can map all the Relay Ports
     */
    private fun createCoolingStagesPoint(relayState: RelayState, relayTag: String): Point {
        val coolingPointMarkers: MutableList<String> = LinkedList()

        coolingPointMarkers.addAll(
            listOf("cooling", "runtime", "zone", "cmd", "his", "logical", relayTag)
        )

        var displayName: String? = null
        when (relayState.association) {
            CpuRelayAssociation.COOLING_STAGE_1 -> {
                coolingPointMarkers.add("stage1")
                displayName = "coolingStage1"
            }
            CpuRelayAssociation.COOLING_STAGE_2 -> {
                coolingPointMarkers.add("stage2")
                displayName = "coolingStage2"
            }
            CpuRelayAssociation.COOLING_STAGE_3 -> {
                coolingPointMarkers.add("stage3")
                displayName = "coolingStage3"
            }
        }

        return createHaystackPointWithEnums(
            "$equipDis-$displayName",
            coolingPointMarkers.stream().toArray { arrayOfNulls(it) },
            "cov",
           ""
        )
    }

    private fun createHeatingStagesPoint(relayState: RelayState, relayTag: String): Point {
        val heatingPointMarkers: MutableList<String> = LinkedList()

        heatingPointMarkers.addAll(
            listOf("heating", "runtime", "zone", "cmd", "his", "logical", relayTag)
        )

        var displayName: String? = null

        when (relayState.association) {
            CpuRelayAssociation.HEATING_STAGE_1 -> {
                heatingPointMarkers.add("stage1")
                displayName = "heatingStage1"
            }
            CpuRelayAssociation.HEATING_STAGE_2 -> {
                heatingPointMarkers.add("stage2")
                displayName = "heatingStage2"
            }
            CpuRelayAssociation.HEATING_STAGE_3 -> {
                heatingPointMarkers.add("stage3")
                displayName = "heatingStage3"
            }
        }

        return createHaystackPointWithEnums(
            "$equipDis-$displayName",
            heatingPointMarkers.stream().toArray { arrayOfNulls(it) },
            "cov",
           ""
        )

    }

    private fun createFanStagesPoint(relayState: RelayState, relayTag: String): Point {
        val fanPointMarkers: MutableList<String> = LinkedList()

        fanPointMarkers.addAll(
            listOf("fan", "runtime", "zone", "cmd", "his", "logical", relayTag)
        )

        var displayName: String? = null
        when (relayState.association) {
            CpuRelayAssociation.FAN_LOW_SPEED -> {
                fanPointMarkers.add("stage1")
                displayName = "fanStage1"
            }
            CpuRelayAssociation.FAN_MEDIUM_SPEED -> {
                fanPointMarkers.add("stage2")
                displayName = "fanStage2"
            }
            CpuRelayAssociation.FAN_HIGH_SPEED -> {
                fanPointMarkers.add("stage3")
                displayName = "fanStage3"
            }
        }

        return createHaystackPointWithEnums(
            "$equipDis-$displayName",
            fanPointMarkers.stream().toArray { arrayOfNulls(it) },
            "cov",
            ""
        )

    }

    private fun createPointForFanEnable(relayTag: String): Point {
        val fanEnableMarker = arrayOf(
            "fan", "runtime", "zone", "cmd", "his", "logical", "fan", "enable", relayTag
        )
        return createHaystackPointWithEnums(
            "$equipDis-fanEnable",
            fanEnableMarker,
            "cov",
           ""
        )
    }

    private fun createPointForOccupiedEnabled(relayTag: String): Point {
        val occupiedEnabledMarker = arrayOf(
            "occupancy", "runtime", "zone", "cmd", "his", "logical", "enable", relayTag
        )

        return createHaystackPointWithEnums(
            "$equipDis-occupiedEnabled",
            occupiedEnabledMarker,
            "cov",
            ""
        )

    }

    private fun createPointForHumidifier(relayTag: String): Point {
        val humidifierMarker = arrayOf(
            "fan", "runtime", "zone", "cmd", "his", "logical", "humidifier", relayTag
        )

        return createHaystackPointWithEnums(
            "$equipDis-humidifierEnabled",
            humidifierMarker,
            "cov",
           ""
        )
    }

    private fun createPointForDeHumidifier(relayTag: String): Point {
        val dehumidifierMarker = arrayOf(
            "fan", "runtime", "zone", "cmd", "his", "logical", "dehumidifier", relayTag
        )
        return createHaystackPointWithEnums(
            "$equipDis-dehumidifierEnabled",
            dehumidifierMarker,
            "cov",
           ""
        )
    }


    /**
     *  Functions which Analog Out can map
     */
    private fun createAnalogOutPointForDCVDamper(analogTag: String): Point {
        Log.i(L.TAG_CCU_HSCPU, "createAnalogOutPointForDCVDamper dcvdamper ")
        val analogPointMarker = arrayOf(
            "writable","dcv", "damper", "zone", "cmd", "his", "logical", "out", analogTag
        )
        return createHaystackPointWithUnit(
            "$equipDis-DcvDamper",
            analogPointMarker,
            "cov",
            "%"
        )
    }

    private fun createAnalogOutPointForFanSpeed(analogTag: String): Point {

        val analogPointMarker = arrayOf(
            "writable","fan", "speed", "zone", "cmd", "his", "logical", "out", analogTag
        )
        return createHaystackPointWithUnit(
            "$equipDis-FanSpeed",
            analogPointMarker,
            "cov",
            "%"
        )
    }

    private fun createAnalogOutPointForHeating(analogTag: String): Point {
        val analogPointMarker = arrayOf(
            "writable","heating",  "zone", "cmd", "his", "logical", "out", analogTag
        )
        return createHaystackPointWithUnit(
            "$equipDis-Heating",
            analogPointMarker,
            "cov",
            "%"
        )
    }

    private fun createAnalogOutPointForCooling(analogTag: String): Point {
        val analogPointMarker = arrayOf(
            "writable","cooling", "zone", "cmd", "his", "logical", "out", analogTag
        )
        return createHaystackPointWithUnit(
            "$equipDis-Cooling",
            analogPointMarker,
            "cov",
            "%"
        )
    }

    private fun createMinMaxPointForAnalogOut(
        analogTag: String, associationType: String
    ): Pair<Any, Any> {

        val type = getAnalogString(analogTag)
        val minMarker = arrayOf(
            analogTag, associationType, "config", "writable", "min","zone",
            "cmd", "out"
        )
        val maxMarker = arrayOf(
            analogTag, associationType, "config", "writable", "max","zone",
            "cmd", "out"
        )
        val minPoint = createHaystackPointWithMinMaxIncUnitInc(
            "$equipDis-$analogTag" + "atMin" + associationType,
            minMarker,
            "0.0", "10.0", "1","V","","Analog-out$type at Min $associationType"
        )
        val maxPoint = createHaystackPointWithMinMaxIncUnitInc(
            "$equipDis-$analogTag" + "atMax" + associationType,
            maxMarker,
            "0.0", "10.0", "1","V","","Analog-out$type at Max $associationType"
        )

        return Pair(minPoint, maxPoint)
    }


    fun createFanLowMediumHighPoint(
        analogTag: String
    ): Triple<Point, Point, Point> {
        val type = getAnalogString(analogTag)
        val lowMarker = arrayOf(
            analogTag, "config", "writable", "fan", "low", "zone", "cmd", "out"
        )
        val mediumMarker = arrayOf(
            analogTag, "config", "writable", "fan", "medium", "zone", "cmd", "out"
        )
        val highMarker = arrayOf(
            analogTag, "config", "writable", "fan", "high", "zone", "cmd", "out"
        )

        val lowPoint = createHaystackPointWithMinMaxIncUnit(
            "$equipDis-$analogTag" + "atFanLow",
            lowMarker,
            "0.0", "100.0", "%","Analog-Out$type at Fan Low"
        )
        val mediumPoint = createHaystackPointWithMinMaxIncUnit(
            "$equipDis-$analogTag" + "atFanMedium",
            mediumMarker,
            "0.0", "100.0", "%","Analog-Out$type at Fan Medium"
        )
        val highPoint = createHaystackPointWithMinMaxIncUnit(
            "$equipDis-$analogTag" + "atFanHigh",
            highMarker,
            "0.0", "100.0", "%","Analog-Out$type at Fan High"
        )

        return Triple(lowPoint, mediumPoint, highPoint)
    }

     fun createPointForAirflowTempSensor(): Point {
        val markers = arrayOf(
            "writable","air", "sensor", "temp", "zone", "discharge", "his", "logical", "in", "th1", "cur","airflow"
        )
        return createHaystackPointWithUnit(
            "$equipDis-th1AirflowTempSensor",
            markers,
            "cov",
            "\u00B0F"
        )
    }

     fun createPointForDoorWindowSensor(analogTag: String): Point {
        val analogInMarker = arrayOf(
            "writable","window", "sensor", "zone", "cmd", "his", "logical", "in", analogTag
        )
        return createHaystackPointWithHisInterPolate(
            "$equipDis-$analogTag" + "doorWindowSensor",
            analogInMarker
        )
    }


    private fun createPointForKeyCardSensor(analogTag: String): Point {
        val analogInMarker = arrayOf(
            "keycard", "sensor", "zone", "cmd", "his", "logical", "in", analogTag
        )
        return createHaystackPointWithUnitEnum(
            "$equipDis-$analogTag" + "KeyCardSensor",
            analogInMarker,
            "",""
        )
    }

    private fun createPointForCurrentTx(
        analogTag: String, displayName: String, min: String, max: String, inc: String, unit: String
    ): Point {
        val analogInMarker = arrayOf(
            "current", "transformer", "sensor", "zone", "cmd", "his", "logical", "in", analogTag
        )
        return createHaystackPointWithMinMaxIncUnitInc(
            "$equipDis-$analogTag$displayName",
            analogInMarker, min, max, inc, unit,"cov","Current TX ($min-${max}Amps)"
        )
    }

    // Function to logicalPoints
    fun createLogicalSensorPoints(): MutableList<Triple<Point, Any, Any>> {

        val logicalPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        val humidityPointMarkers =
            arrayOf("zone", "air", "humidity", "sensor", "current", "his", "cur", "logical")
        val illuminanceSensorPointMarkers =
            arrayOf("zone", "air", "illuminance", "sensor", "current", "his", "cur", "logical")
        val occupancySensorPointMarkers =
            arrayOf("zone", "air", "occupancy", "sensor", "current", "his", "cur", "logical")

        val humidityPoint = createHaystackPointWithUnit(
            "$equipDis-" + Port.SENSOR_RH.portSensor,
            humidityPointMarkers,
            "cov",
            "%"
        )
        val illuminancePoint = createHaystackPointWithUnit(
            "$equipDis-" + Port.SENSOR_ILLUMINANCE.portSensor,
            illuminanceSensorPointMarkers,
            "cov",
            "lux"
        )
        val occupancyPoint = createHaystackPointWithEnums(
            "$equipDis-" + Port.SENSOR_OCCUPANCY.portSensor+"Sensor",
            occupancySensorPointMarkers,
            "cov",
            "off,on"
        )


        logicalPointsList.add(Triple(occupancyPoint, Port.SENSOR_OCCUPANCY, 0.0))
        logicalPointsList.add(Triple(humidityPoint, Port.SENSOR_RH, 0.0))
        logicalPointsList.add(Triple(illuminancePoint, Port.SENSOR_ILLUMINANCE, 0.0))

        return logicalPointsList

    }


    // Function which create Current Temp Point
    fun createTemperatureOffSetPoint(tempOffSetValue: Double): String {

        val temperatureOffsetMarkers = arrayOf(
            "config", "writable", "zone", "temperature", "offset", "sp"
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

    // function creates and temperature cur,desired Points
    fun temperatureScheduleLogicalPoints(): MutableList<Triple<Point, Any, Any>> {

        val logicalTempPointsList: MutableList<Triple<Point, Any, Any>> = LinkedList()

        val currentTempPointMarkers =
            arrayOf("zone", "air", "temp", "sensor", "current", "cur", "his", "logical")
        val desiredTempCoolingPointMarkers =
            arrayOf("zone", "air", "temp", "desired", "logical", "sp", "writable", "userIntent", "cooling", "his")
        val desiredTempHeatingPointMarkers =
            arrayOf("zone", "air", "temp", "desired", "logical", "sp", "writable", "userIntent", "heating", "his")

        val equipStatusPointMarkers = arrayOf("zone", "status", "his")
        val equipStatusMessagePointMarkers = arrayOf("zone", "status", "his", "message", "writable")
        val scheduleStatusPointMarkers = arrayOf("zone", "scheduleStatus", "his", "message", "writable", "logical")
        val scheduleTypePointMarkers = arrayOf("zone", "scheduleType", "his", "message", "writable", "logical")

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
            "cov",
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
            "cov",
            "building,zone,named"
        )

        logicalTempPointsList.add(Triple(desiredTempPoint, LogicalKeyID.DESIRED_TEMP, 72.0))
        logicalTempPointsList.add(Triple(desiredTempHeatingPoint, LogicalKeyID.DESIRED_TEMP_HEATING, 70.0))
        logicalTempPointsList.add(Triple(desiredTempCoolingPoint, LogicalKeyID.DESIRED_TEMP_COOLING, 74.0))
        logicalTempPointsList.add(Triple(currentTempPoint, LogicalKeyID.CURRENT_TEMP, 0.0))
        logicalTempPointsList.add(Triple(equipStatusPoint, LogicalKeyID.EQUIP_STATUS, 0.0))
        logicalTempPointsList.add(Triple(equipStatusMessagePoint, LogicalKeyID.EQUIP_STATUS_MESSAGE, "OFF"))
        logicalTempPointsList.add(Triple(equipScheduleStatusPoint, LogicalKeyID.EQUIP_SCHEDULE_STATUS, ""))
        logicalTempPointsList.add(Triple(equipScheduleTypePoint, LogicalKeyID.SCHEDULE_TYPE, 0.0))

        return logicalTempPointsList
    }


    fun addProfilePoints() {

        val occupancyDetectionMarkers = arrayOf("occupancy", "his", "sp", "zone","detection")
        val occupancyMarkers = arrayOf("occupancy", "mode", "his", "sp", "zone")

        val operatingModeMarkers = arrayOf("temp", "mode", "his", "sp", "zone","operating")
        val operatingModePointEnums = "off,cooling,heating,tempdead"

        val occupancyEnum = "unoccupied,occupied,preconditioning,forcedoccupied,vacation,occupancysensing," +
                "autoforceoccupy,autoaway"

        val occupancyDetection = createHaystackPointWithEnums(
            displayName = "$equipDis-occupancyDetection",
            markers = occupancyDetectionMarkers,
            hisInterpolate="cov",
            enums = "false,true"
        )
        val occupancyModePoint = createHaystackPointWithEnums(
            displayName = "$equipDis-occupancy",
            markers = occupancyMarkers,
            hisInterpolate="cov",
            enums = occupancyEnum
        )
        val operatingModeModePoint = createHaystackPointWithEnums(
            displayName = "$equipDis-OperatingMode",
            markers = operatingModeMarkers,
            hisInterpolate="cov",
            enums = operatingModePointEnums
        )


        val occupancyDetectionPointId = addPointToHaystack(point = occupancyDetection)
        addDefaultHisValueForPoint(occupancyDetectionPointId, 0.0)
        addDefaultValueForPoint(occupancyDetectionPointId, 0.0)
        addDefaultValueForPoint(addPointToHaystack(point = occupancyModePoint), 0.0)

        val operatingModeModePointId = addPointToHaystack(point = operatingModeModePoint)
        addDefaultHisValueForPoint(operatingModeModePointId, 0.0)
        addDefaultValueForPoint(operatingModeModePointId, 0.0)
    }


    // Function which finds default fan speed
    private fun getDefaultFanSpeed(config: HyperStatCpuConfiguration): StandaloneFanStage {

        return if (config.relay1State.enabled && HyperStatAssociationUtil.isRelayAssociatedToFan(config.relay1State))
            StandaloneFanStage.AUTO
        else if (config.relay2State.enabled && HyperStatAssociationUtil.isRelayAssociatedToFan(config.relay2State))
            StandaloneFanStage.AUTO
        else if (config.relay3State.enabled && HyperStatAssociationUtil.isRelayAssociatedToFan(config.relay3State))
            StandaloneFanStage.AUTO
        else if (config.relay4State.enabled && HyperStatAssociationUtil.isRelayAssociatedToFan(config.relay4State))
            StandaloneFanStage.AUTO
        else if (config.relay5State.enabled && HyperStatAssociationUtil.isRelayAssociatedToFan(config.relay5State))
            StandaloneFanStage.AUTO
        else if (config.relay6State.enabled && HyperStatAssociationUtil.isRelayAssociatedToFan(config.relay6State))
            StandaloneFanStage.AUTO
        else
            StandaloneFanStage.OFF
    }

    // Function which finds default default cooling mode
    private fun getDefaultConditioningMode(config: HyperStatCpuConfiguration): StandaloneConditioningMode {
        return if (config.relay1State.enabled && HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(config.relay1State))
            StandaloneConditioningMode.AUTO
        else if (config.relay2State.enabled && HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(config.relay2State))
            StandaloneConditioningMode.AUTO
        else if (config.relay3State.enabled && HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(config.relay3State))
            StandaloneConditioningMode.AUTO
        else if (config.relay4State.enabled && HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(config.relay4State))
            StandaloneConditioningMode.AUTO
        else if (config.relay5State.enabled && HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(config.relay5State))
            StandaloneConditioningMode.AUTO
        else if (config.relay6State.enabled && HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(config.relay6State))
            StandaloneConditioningMode.AUTO
        else
            StandaloneConditioningMode.OFF
    }


    private fun getRelayConfigEnum(): String {
        return "na,coolingstage1,coolingstage2,coolingstage3,heatingstage1,heatingstage2, " +
                "heatingstage3,fanenabled,occupiedenabled,fanhigh,fanlow,fanmedium,humidifier,dehumidifier"
    }

    private fun getAnalogOutConfigEnum(): String {
        return "na,cooling,heating,fanspeed,dcvdamper"
    }

    private fun getAnalogString(analogTag: String): Int{
        when(analogTag){
            Tags.ANALOG1-> return 1
            Tags.ANALOG2-> return 2
            Tags.ANALOG3-> return 3
        }
        return 0
    }

}



