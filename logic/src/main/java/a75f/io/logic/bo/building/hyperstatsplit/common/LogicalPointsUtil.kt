package a75f.io.logic.bo.building.hyperstatsplit.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Kind
import a75f.io.logic.L
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusPressAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusPressState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.SensorBusTempState
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.UniversalInAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.UniversalInState
import android.util.Log
import java.util.HashMap

/**
 * Created for HyperStat by Manjunath K on 05-09-2022.
 * Created for HyperStat Split by Nick P on 07-24-2023.
 */

class LogicalPointsUtil {

    companion object {
        // Adding Point to Haystack and returns point id
        private fun addPointToHaystack(point: Point): String {
            return if(point.id != null){
                CCUHsApi.getInstance().updatePoint(point, point.id)
                point.id
            }else {
                CCUHsApi.getInstance().addPoint(point)
            }
        }

        /** Logical point creation **/

        // Common logical points for all the profiles

        fun createFanLowPoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readFanLowRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","logical","runtime","zone","his","fan", "low", "speed"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanLowSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readFanLowRelayLogicalPoint(equipRef)).build()
        }

        fun createFanMediumPoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readFanMediumRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","logical","runtime","zone","his","fan", "medium", "speed"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanMediumSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readFanMediumRelayLogicalPoint(equipRef)).build()
        }

        fun createFanHighPoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readFanHighRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","logical","runtime","zone","his","fan", "high", "speed"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanHighSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readFanHighRelayLogicalPoint(equipRef)).build()
        }

        fun createPointForFanEnable(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readFanEnabledRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {

                val markers = arrayOf(
                    "cmd", "logical","runtime", "his", "zone", "fan", "enabled"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanEnabled")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readFanEnabledRelayLogicalPoint(equipRef)).build()
        }

        fun createPointForOccupiedEnabled(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readOccupiedEnabledRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "logical", "runtime", "his", "zone", "occupied", "enabled"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-occupiedEnabled")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readOccupiedEnabledRelayLogicalPoint(equipRef)).build()
        }

        fun createPointForHumidifier(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readHumidifierRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "logical", "runtime", "his", "zone", "humidifier"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-humidifierEnableCmd")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readHumidifierRelayLogicalPoint(equipRef)).build()
        }

        fun createPointForDeHumidifier(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readDeHumidifierRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "logical", "runtime", "his", "zone", "dehumidifier"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-dehumidifierEnableCmd")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readDeHumidifierRelayLogicalPoint(equipRef)).build()
        }


        // CPU specific points
        fun createCoolingStage1Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readCoolingStage1RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "cooling", "stage1", "dxCooling", "zone", "logical", "runtime","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-coolingStage1")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readCoolingStage1RelayLogicalPoint(equipRef)).build()
        }

         fun createCoolingStage2Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point{
            val existingPoint = readCoolingStage2RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "cooling", "stage2", "dxCooling", "zone", "logical", "runtime","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-coolingStage2")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readCoolingStage2RelayLogicalPoint(equipRef)).build()
        }

         fun createCoolingStage3Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point{
            val existingPoint = readCoolingStage3RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "cooling", "stage3", "dxCooling", "zone", "logical", "runtime","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-coolingStage3")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }

                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readCoolingStage3RelayLogicalPoint(equipRef)).build()
        }

         fun createHeatingStage1Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point{
            val existingPoint = readHeatingStage1RelayLogicalPoint(equipRef)

            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "heating", "stage1", "electHeating", "zone", "logical", "runtime","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-heatingStage1")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }

                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readHeatingStage1RelayLogicalPoint(equipRef)).build()
        }

         fun createHeatingStage2Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point{
            val existingPoint = readHeatingStage2RelayLogicalPoint(equipRef)

            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "heating", "stage2", "electHeating", "zone", "logical", "runtime","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-heatingStage2")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readHeatingStage2RelayLogicalPoint(equipRef)).build()
        }

         fun createHeatingStage3Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point{
            val existingPoint = readHeatingStage3RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "heating", "stage3", "electHeating", "zone", "logical", "runtime","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-heatingStage3")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readHeatingStage3RelayLogicalPoint(equipRef)).build()
        }

        fun createPointForExhaustFanStage1(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {
            val existingPoint = readExhaustFanStage1RelayLogicalPoint(equipRef)
            if (existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "exhaust", "fan", "stage1", "zone", "logical", "runtime", "his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-exhaustFanStage1")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readExhaustFanStage1RelayLogicalPoint(equipRef)).build()
        }

        fun createPointForExhaustFanStage2(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {
            val existingPoint = readExhaustFanStage2RelayLogicalPoint(equipRef)
            if (existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "exhaust", "fan", "stage2", "zone", "logical", "runtime", "his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-exhaustFanStage2")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readExhaustFanStage2RelayLogicalPoint(equipRef)).build()
        }


        /** Reading logical points **/

        // command points reading
        fun readFanLowRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and fan and low and speed and equipRef == \"$equipRef\"")
        }
        fun readFanMediumRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and fan and medium and speed and equipRef == \"$equipRef\"")
        }
        fun readFanHighRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and fan and high and speed and equipRef == \"$equipRef\"")
        }
        fun readFanEnabledRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and fan and enabled and equipRef == \"$equipRef\"")
        }
        fun readOccupiedEnabledRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and occupied and enabled and equipRef == \"$equipRef\"")
        }
        fun readHumidifierRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and humidifier and equipRef == \"$equipRef\"")
        }
        fun readDeHumidifierRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and dehumidifier and equipRef == \"$equipRef\"")
        }

        // CPU with Economiser-specific points reading
         fun readCoolingStage1RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and cooling and stage1 and equipRef == \"$equipRef\"")
        }
         fun readCoolingStage2RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and cooling and stage2 and equipRef == \"$equipRef\"")
        }
         fun readCoolingStage3RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and cooling and stage3 and equipRef == \"$equipRef\"")
        }
         fun readHeatingStage1RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and heating and stage1 and equipRef == \"$equipRef\"")
        }
         fun readHeatingStage2RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and heating and stage2 and equipRef == \"$equipRef\"")
        }
         fun readHeatingStage3RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and heating and stage3 and equipRef == \"$equipRef\"")
        }
        fun readExhaustFanStage1RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and exhaust and fan and stage1 and equipRef == \"$equipRef\"")
        }
        fun readExhaustFanStage2RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "cmd and logical and exhaust and fan and stage2 and equipRef == \"$equipRef\"")
        }

        /**====== Analog Logical points=======**/

         fun createAnalogOutPointForHeating(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
         ): Point {
            val existingPoint = readAnalogHeatingLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","analog", "output",
                    "modulating", "elecHeating","heating","his"
                )

                val point = Point.Builder()
                    .setDisplayName("$equipDis-modulatingHeating")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readAnalogHeatingLogicalPoint(equipRef)).build()
        }
         fun createAnalogOutPointForCooling(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
         ): Point {
            val existingPoint = readAnalogCoolingLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","analog", "output",
                    "modulating", "dxCooling","cooling","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-modulatingCooling")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
                return Point.Builder().setHashMap(readAnalogCoolingLogicalPoint(equipRef)).build()
            }
            return Point.Builder().setHashMap(readAnalogCoolingLogicalPoint(equipRef)).build()
        }

         fun createAnalogOutPointForFanSpeed(
             equipDis: String, siteRef: String, equipRef: String,
             roomRef: String, floorRef: String, tz: String,
         ): Point {
            val existingPoint = readAnalogOutFanSpeedLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","analog", "output", "modulating",
                    "fan", "speed","run","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-modulatingFanSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readAnalogOutFanSpeedLogicalPoint(equipRef)).build()
        }

        // TODO: to be verified. This uses all existing OAO damper tags, but replaces "system" with "zone".
         fun createAnalogOutPointForOaoDamper(
             equipDis: String, siteRef: String, equipRef: String,
             roomRef: String, floorRef: String, tz: String,
         ): Point {
            val existingPoint = readAnalogOutOaoLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "air", "cmd", "zone","logical","analog", "output",
                    "outside", "damper", "oao","actuator","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-oaoDamper")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readAnalogOutOaoLogicalPoint(equipRef)).build()
        }

        fun createAnalogOutPointForPredefinedFanSpeed(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: String,
        ): Point {
            val existingPoint = readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","cur","analog","standalone","output",
                    "fan", "speed","his","predefined","cpuecon"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-predefinedFanSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef).setGroup(nodeAddress)
                    .setRoomRef(roomRef).setFloorRef(floorRef).setKind(Kind.NUMBER)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef)).build()
        }

        // Read analog logical points
        fun readAnalogCoolingLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and cooling and equipRef == \"$equipRef\"")
        }
        fun readAnalogHeatingLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and heating and equipRef == \"$equipRef\"")
        }
        fun readAnalogOutFanSpeedLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and modulating and fan and speed and equipRef == \"$equipRef\"")
        }
        fun readAnalogOutOaoLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and oao and damper and equipRef == \"$equipRef\"")
        }
        fun readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and predefined and fan and speed and equipRef == \"$equipRef\"")
        }

        /***   Universal In logical points***/

        fun createPointForSupplyAirTemperature(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val existingPoint = readSupplyAirTempSensor(equipRef)
            if(existingPoint.isEmpty()) {
                val supplyAirTempMarkers = arrayOf("supply", "air", "temp", "sensor", "cur", "his", "logical")

                val supplyAirTempPoint = Point.Builder()
                    .setDisplayName("$equipDis-supplyAirTemperature")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setUnit("\u00B0F")
                supplyAirTempMarkers.forEach { supplyAirTempPoint.addMarker(it) }
                addPointToHaystack(supplyAirTempPoint.build())
            }
            return Point.Builder().setHashMap(readSupplyAirTempSensor(equipRef)).build()
        }
        fun createPointForSupplyAirHumidity(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val existingPoint = readSupplyAirHumiditySensor(equipRef)
            if(existingPoint.isEmpty()) {
                val supplyAirHumidityMarkers = arrayOf("supply", "air", "humidity", "sensor", "cur", "his", "logical")

                val supplyAirHumidityPoint = Point.Builder()
                    .setDisplayName("$equipDis-supplyAirHumidity")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setUnit("\u00B0F")
                supplyAirHumidityMarkers.forEach { supplyAirHumidityPoint.addMarker(it) }
                addPointToHaystack(supplyAirHumidityPoint.build())
            }
            return Point.Builder().setHashMap(readSupplyAirHumiditySensor(equipRef)).build()
        }

        fun createPointForMixedAirTemperature(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val existingPoint = readMixedAirTempSensor(equipRef)
            if(existingPoint.isEmpty()) {
                val mixedAirTempMarkers = arrayOf("mixed", "air", "temp", "sensor", "cur", "his", "logical")

                val mixedAirTempPoint = Point.Builder()
                    .setDisplayName("$equipDis-mixedAirTemperature")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setUnit("\u00B0F")
                mixedAirTempMarkers.forEach { mixedAirTempPoint.addMarker(it) }
                addPointToHaystack(mixedAirTempPoint.build())
            }
            return Point.Builder().setHashMap(readMixedAirTempSensor(equipRef)).build()
        }
        fun createPointForMixedAirHumidity(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val existingPoint = readMixedAirHumiditySensor(equipRef)
            if(existingPoint.isEmpty()) {
                val mixedAirHumidityMarkers = arrayOf("mixed", "air", "humidity", "sensor", "cur", "his", "logical")

                val mixedAirHumidityPoint = Point.Builder()
                    .setDisplayName("$equipDis-mixedAirHumidity")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setUnit("\u00B0F")
                mixedAirHumidityMarkers.forEach { mixedAirHumidityPoint.addMarker(it) }
                addPointToHaystack(mixedAirHumidityPoint.build())
            }
            return Point.Builder().setHashMap(readMixedAirHumiditySensor(equipRef)).build()
        }

        fun createPointForOutsideAirTemperature(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val existingPoint = readOutsideAirTempSensor(equipRef)
            if(existingPoint.isEmpty()) {
                val supplyAirTempMarkers = arrayOf("outside", "air", "temp", "sensor", "cur", "his", "logical")

                val supplyAirTempPoint = Point.Builder()
                    .setDisplayName("$equipDis-outsideAirTemperature")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setUnit("\u00B0F")
                supplyAirTempMarkers.forEach { supplyAirTempPoint.addMarker(it) }
                addPointToHaystack(supplyAirTempPoint.build())
            }
            return Point.Builder().setHashMap(readOutsideAirTempSensor(equipRef)).build()
        }
        fun createPointForOutsideAirHumidity(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val existingPoint = readOutsideAirHumiditySensor(equipRef)
            if(existingPoint.isEmpty()) {
                val outsideAirHumidityMarkers = arrayOf("outside", "air", "humidity", "sensor", "cur", "his", "logical")

                val outsideAirHumidityPoint = Point.Builder()
                    .setDisplayName("$equipDis-outsideAirHumidity")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setUnit("\u00B0F")
                outsideAirHumidityMarkers.forEach { outsideAirHumidityPoint.addMarker(it) }
                addPointToHaystack(outsideAirHumidityPoint.build())
            }
            return Point.Builder().setHashMap(readOutsideAirHumiditySensor(equipRef)).build()
        }

        // TODO: verify if polarity will be reversible on this point or not.
        fun createPointForCondensateOverflowStatus(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val markers = arrayOf(
                "condensate","overflow","status","sensor","his"
            )

            val point = Point.Builder()
                .setDisplayName("$equipDis-condensateOverflowStatus")
                .setSiteRef(siteRef).setEquipRef(equipRef)
                .setRoomRef(roomRef).setFloorRef(floorRef)
                .setTz(tz).setHisInterpolate("cov")
            markers.forEach { point.addMarker(it) }

            return point.build()
        }

         fun createPointForCurrentTx(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
            min: String, max: String, inc: String, unit: String, tranferType: TransformerSensorType
        ): Point {

            var transformerTag = "transformer"
            var name = "currentDrawn_10"
            if(tranferType == TransformerSensorType.TRANSFORMER_20) {
                transformerTag = "transformer20"
                name = "currentDrawn_20"
            }
            if(tranferType == TransformerSensorType.TRANSFORMER_50) {
                transformerTag = "transformer50"
                name = "currentDrawn_50"
            }
            if(tranferType == TransformerSensorType.TRANSFORMER_100) {
                transformerTag = "transformer100"
                name = "currentDrawn_100"
            }
            if(tranferType == TransformerSensorType.TRANSFORMER_150) {
                transformerTag = "transformer150"
                name = "currentDrawn_150"
            }
            val existingPoint = readCurrentTransferSensor(equipRef,transformerTag)
            if(existingPoint.isEmpty()) {

                val markers = arrayOf(
                    "current", "sensor", "zone", "his", "logical", "cur", transformerTag
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-$name")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setMinVal(min).setMaxVal(max).setIncrementVal(inc)
                    .setUnit(unit)
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readCurrentTransferSensor(equipRef,transformerTag)).build()
        }

        // Sensor bus point configured from a native 75F Pressure Sensor (DPS)
        fun createPointForDuctPressure(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            var pressureTag = "ductPressure"
            var name = "ductPressure"
            val existingPoint = readDuctPressureSensor(equipRef,pressureTag)
            if(existingPoint.isEmpty()) {

                val markers = arrayOf(
                    "duct", "pressure", "sensor", "zone", "his", "logical", "cur", pressureTag
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-$name")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    // TODO: min, max, inc (verify with firmware once we get there)
                    .setUnit("Pa")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readDuctPressureSensor(equipRef,pressureTag)).build()
        }

        // Duct Pressure point configured from a 0-10V sensor on a Universal Input
        fun createPointForDuctPressure(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
            min: String, max: String, inc: String, unit: String, sensorType: DuctPressureSensorType
        ): Point {

            var pressureTag = "ductPressure1in"
            var name = "ductPressure_1in"
            if(sensorType == DuctPressureSensorType.DUCT_PRESSURE_0_2) {
                pressureTag = "ductPressure2in"
                name = "ductPressure_2in"
            }
            if(sensorType == DuctPressureSensorType.DUCT_PRESSURE_0_5) {
                pressureTag = "ductPressure5in"
                name = "ductPressure_5in"
            }
            val existingPoint = readDuctPressureSensor(equipRef,pressureTag)
            if(existingPoint.isEmpty()) {

                val markers = arrayOf(
                    "duct", "pressure", "sensor", "zone", "his", "logical", "cur", pressureTag
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-$name")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setMinVal(min).setMaxVal(max).setIncrementVal(inc)
                    .setUnit(unit)
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readDuctPressureSensor(equipRef,pressureTag)).build()
        }

        // TODO: verify if polarity will be reversible on this point or not.
        fun createPointForFilterStatus(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            // TODO: add enum (once polarity is determined)
            val markers = arrayOf(
                "filter","pressure","status","sensor","his"
            )

            val point = Point.Builder()
                .setDisplayName("$equipDis-filterStatus")
                .setSiteRef(siteRef).setEquipRef(equipRef)
                .setRoomRef(roomRef).setFloorRef(floorRef)
                .setTz(tz).setHisInterpolate("cov")
            markers.forEach { point.addMarker(it) }

            return point.build()
        }

        /** Read sensor points**/

        private fun readSupplyAirTempSensor(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "supply and air and temp and logical and sensor and equipRef == \"$equipRef\"")
        }
        private fun readSupplyAirHumiditySensor(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "supply and air and humidity and logical and sensor and equipRef == \"$equipRef\"")
        }

        private fun readOutsideAirTempSensor(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "outside and air and temp and not outsideWeather and logical and sensor and equipRef == \"$equipRef\"")
        }
        private fun readOutsideAirHumiditySensor(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "outside and air and humidity and logical and sensor and equipRef == \"$equipRef\"")
        }

        private fun readMixedAirTempSensor(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "mixed and air and temp and logical and sensor and equipRef == \"$equipRef\"")
        }
        private fun readMixedAirHumiditySensor(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "mixed and air and humidity and logical and sensor and equipRef == \"$equipRef\"")
        }

        private fun readCurrentTransferSensor(equipRef: String, transferTag: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "current and logical and sensor and $transferTag and equipRef == \"$equipRef\"")
        }

        private fun readDuctPressureSensor(equipRef: String, pressureTag: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "duct and pressure and logical and sensor and $pressureTag and equipRef == \"$equipRef\"")
        }
        private fun readFilterStatus(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "filter and status and sensor and equipRef == \"$equipRef\"")
        }
        private fun readCondensateOverflowStatus(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "condensate and overflow and status and sensor and equipRef == \"$equipRef\"")
        }


        /** Delete invalid logical points  ***/
        fun cleanCpuEconLogicalPoints(config: HyperStatSplitCpuEconConfiguration, equipRef: String){
            Log.d(L.TAG_CCU_HSSPLIT_CPUECON, "cleanCpuEconLogicalPoints()")
            removeCpuEconRelayLogicalPoints(config, equipRef)
            removeCpuEconAnalogLogicalPoints(config, equipRef)
            removeUniversalInLogicalPoints(equipRef,config,ProfileType.HYPERSTATSPLIT_CPU_ECON)
            removeSensorBusLogicalPoints(equipRef,config,ProfileType.HYPERSTATSPLIT_CPU_ECON)
        }
        private fun removeCpuEconRelayLogicalPoints(config: HyperStatSplitCpuEconConfiguration, equipRef: String){
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage1(config))
                removePoint(readCoolingStage1RelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage2(config))
                removePoint(readCoolingStage2RelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToCoolingStage3(config))
                removePoint(readCoolingStage3RelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToHeatingStage1(config))
                removePoint(readHeatingStage1RelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToHeatingStage2(config))
                removePoint(readHeatingStage2RelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToHeatingStage3(config))
                removePoint(readHeatingStage3RelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanLow(config))
                removePoint(readFanLowRelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanMedium(config))
                removePoint(readFanMediumRelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanHigh(config))
                removePoint(readFanHighRelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToFanEnabled(config))
                removePoint(readFanEnabledRelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayAssociatedToOccupiedEnabled(config))
                removePoint(readOccupiedEnabledRelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToHumidifier(config))
                removePoint(readHumidifierRelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToDeHumidifier(config))
                removePoint(readDeHumidifierRelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToExhaustFanStage1(config))
                removePoint(readExhaustFanStage1RelayLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyRelayEnabledAssociatedToExhaustFanStage2(config))
                removePoint(readExhaustFanStage2RelayLogicalPoint(equipRef))
        }
        private fun removeCpuEconAnalogLogicalPoints(config: HyperStatSplitCpuEconConfiguration, equipRef: String){
            if(!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToCooling(config))
                removePoint(readAnalogCoolingLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToHeating(config))
                removePoint(readAnalogHeatingLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToFan(config))
                removePoint(readAnalogOutFanSpeedLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToOAO(config))
                removePoint(readAnalogOutOaoLogicalPoint(equipRef))
            if(!HyperStatSplitAssociationUtil.isAnyAnalogAssociatedToStaged(config))
                removePoint(readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef))
        }

        /*
            In CPU/Economiser profile, Outside/Mixed/Supply Air Temperature can all be sourced from either
            a Universal Input or the sensor bus.

            This method checks both locations before deleting an outsideAirTemperature, mixedAirTemperature,
            or supplyAirTemperature logical point.
         */
        private fun removeUniversalInLogicalPoints(
            equipRef: String, baseConfig: BaseProfileConfiguration, profileType: ProfileType
        ) {
            var address0State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)
            var address1State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)
            var address2State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)
            var address3State = SensorBusPressState(false, CpuEconSensorBusPressAssociation.DUCT_PRESSURE)

            var universalIn1State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn2State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn3State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn4State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn5State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn6State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn7State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn8State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)

            when(profileType){
                ProfileType.HYPERSTATSPLIT_CPU_ECON ->{
                    address0State =  (baseConfig as HyperStatSplitCpuEconConfiguration).address0State
                    address1State =  baseConfig.address1State
                    address2State =  baseConfig.address2State
                    address3State =  baseConfig.address3State
                    universalIn1State = baseConfig.universalIn1State
                    universalIn2State = baseConfig.universalIn2State
                    universalIn3State = baseConfig.universalIn3State
                    universalIn4State = baseConfig.universalIn4State
                    universalIn5State = baseConfig.universalIn5State
                    universalIn6State = baseConfig.universalIn6State
                    universalIn7State = baseConfig.universalIn7State
                    universalIn8State = baseConfig.universalIn8State
                }
                else -> {}
            }

            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToSupplyAirTemperature(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State) 
                && !HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToSupplyAir(
                    address0State,
                    address1State,
                    address2State)) {
                removePoint(readSupplyAirTempSensor(equipRef))
            }
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToMixedAirTemperature(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State)
                && !HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToMixedAir(
                    address0State,
                    address1State,
                    address2State)) {
                removePoint(readMixedAirTempSensor(equipRef))
            }
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToOutsideAirTemperature(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State)
                && !HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                    address0State,
                    address1State,
                    address2State)) {
                removePoint(readOutsideAirTempSensor(equipRef))
            }
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToCT10(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer"))
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToCT20(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer20"))
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToCT50(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State
            ))
                removePoint(readCurrentTransferSensor(equipRef,"transformer50"))
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToCT100(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State
                ))
                removePoint(readCurrentTransferSensor(equipRef,"transformer100"))
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToCT150(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State
                ))
                removePoint(readCurrentTransferSensor(equipRef,"transformer150"))
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToDuctPressure0to1(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State))
                removePoint(readDuctPressureSensor(equipRef, "ductPressure1in"))
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToDuctPressure0to2(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State))
                removePoint(readDuctPressureSensor(equipRef, "ductPressure2in"))
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToCondensateNO(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State
            ) && !HyperStatSplitAssociationUtil.isAnyUniversalInMappedToCondensateNC(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State
                )) {
                removePoint(readCondensateOverflowStatus(equipRef))
            }
            if(!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToFilterNO(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State
                ) && !HyperStatSplitAssociationUtil.isAnyUniversalInMappedToFilterNC(
                    universalIn1State,universalIn2State,
                    universalIn3State,universalIn4State,
                    universalIn5State,universalIn6State,
                    universalIn7State,universalIn8State
                )) {
                removePoint(readFilterStatus(equipRef))
            }
        }

        /*
            In CPU/Economiser profile, Outside/Mixed/Supply Air Temperature can all be sourced from either
            a Universal Input or the sensor bus.

            This method checks both locations before deleting an outsideAirTemperature, mixedAirTemperature,
            or supplyAirTemperature logical point.
         */
        private fun removeSensorBusLogicalPoints(
            equipRef: String, baseConfig: BaseProfileConfiguration, profileType: ProfileType
        ) {
            var address0State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)
            var address1State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)
            var address2State = SensorBusTempState(false, CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)
            var address3State = SensorBusPressState(false, CpuEconSensorBusPressAssociation.DUCT_PRESSURE)

            var universalIn1State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn2State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn3State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn4State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn5State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn6State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn7State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)
            var universalIn8State = UniversalInState(false, UniversalInAssociation.OUTSIDE_AIR_TEMPERATURE)

            when(profileType){
                ProfileType.HYPERSTATSPLIT_CPU_ECON ->{
                    address0State =  (baseConfig as HyperStatSplitCpuEconConfiguration).address0State
                    address1State =  baseConfig.address1State
                    address2State =  baseConfig.address2State
                    address3State =  baseConfig.address3State
                    universalIn1State = baseConfig.universalIn1State
                    universalIn2State = baseConfig.universalIn2State
                    universalIn3State = baseConfig.universalIn3State
                    universalIn4State = baseConfig.universalIn4State
                    universalIn5State = baseConfig.universalIn5State
                    universalIn6State = baseConfig.universalIn6State
                    universalIn7State = baseConfig.universalIn7State                    
                    universalIn8State = baseConfig.universalIn8State
                }
                else -> {}
            }

            if(!HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToMixedAir(
                    address0State, address1State, address2State)) {
                if (!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToMixedAirTemperature(                    
                        universalIn1State,universalIn2State,
                        universalIn3State,universalIn4State,
                        universalIn5State,universalIn6State,
                        universalIn7State,universalIn8State)) {
                    removePoint(readMixedAirTempSensor(equipRef))
                }
                removePoint(readMixedAirHumiditySensor(equipRef))
            }
            if(!HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToSupplyAir(
                    address0State, address1State, address2State)) {
                if (!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToSupplyAirTemperature(
                        universalIn1State,universalIn2State,
                        universalIn3State,universalIn4State,
                        universalIn5State,universalIn6State,
                        universalIn7State,universalIn8State)) {
                    removePoint(readSupplyAirTempSensor(equipRef))
                }
                removePoint(readSupplyAirHumiditySensor(equipRef))
            }
            if(!HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToOutsideAir(
                    address0State, address1State, address2State)) {
                if (!HyperStatSplitAssociationUtil.isAnyUniversalInMappedToOutsideAirTemperature(
                        universalIn1State,universalIn2State,
                        universalIn3State,universalIn4State,
                        universalIn5State,universalIn6State,
                        universalIn7State,universalIn8State)) {
                    removePoint(readOutsideAirTempSensor(equipRef))
                }
                removePoint(readOutsideAirHumiditySensor(equipRef))
            }
            if(!HyperStatSplitAssociationUtil.isAnySensorBusAddressMappedToDuctPressure(
                    address3State)) {
                removePoint(readDuctPressureSensor(equipRef, "ductPressure"))
            }
        }

        private fun removePoint(point: HashMap<Any, Any>){
            if(point.isNotEmpty()){
                CCUHsApi.getInstance().deleteEntityTree(Point.Builder().setHashMap(point).build().id)
            }
        }
    }

    enum class TransformerSensorType {
        TRANSFORMER,TRANSFORMER_20,TRANSFORMER_50, TRANSFORMER_100, TRANSFORMER_150
    }
    enum class FilterSwitchType {
        NORMALLY_OPEN, NORMALLY_CLOSED
    }
    enum class FilterPressureSensorType {
        FILTER_PRESSURE_0_1, FILTER_PRESSURE_0_2, FILTER_PRESSURE_0_5
    }
    enum class CondensateSwitchType {
        NORMALLY_OPEN, NORMALLY_CLOSED
    }
    enum class DuctPressureSensorType {
        DUCT_PRESSURE_0_1, DUCT_PRESSURE_0_2, DUCT_PRESSURE_0_5
    }
}