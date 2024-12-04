package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logic.ANALOG_VALUE
import a75f.io.logic.BINARY_VALUE
import a75f.io.logic.addBacnetTags
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.Th1InState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.Th2InState
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Configuration
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.Pipe2Th1InState
import java.util.HashMap

/**
 * Created by Manjunath K on 05-09-2022.
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
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readFanLowRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","logical","runtime","zone","his","fan", "low", "speed"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanlowspeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                val fanStage1Point = point.build()
                addBacnetTags(fanStage1Point, 24, BINARY_VALUE, nodeAddress)
                addPointToHaystack(fanStage1Point)
            }
            return  Point.Builder().setHashMap(readFanLowRelayLogicalPoint(equipRef)).build()
        }

        fun createFanMediumPoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val fanStage2Point = point.build()
                addBacnetTags(fanStage2Point, 25, BINARY_VALUE, nodeAddress)
                addPointToHaystack(fanStage2Point)
            }
            return  Point.Builder().setHashMap(readFanMediumRelayLogicalPoint(equipRef)).build()
        }

        fun createFanHighPoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val fanStage3Point = point.build()
                addBacnetTags(fanStage3Point, 26, BINARY_VALUE, nodeAddress)
                addPointToHaystack(fanStage3Point)
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
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val occupiedEnabledPoint = point.build()
                addBacnetTags(occupiedEnabledPoint, 41, BINARY_VALUE, nodeAddress)
                addPointToHaystack(occupiedEnabledPoint)
            }
            return  Point.Builder().setHashMap(readOccupiedEnabledRelayLogicalPoint(equipRef)).build()
        }

        fun createPointForHumidifier(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val humidifierPoint = point.build()
                addBacnetTags(humidifierPoint, 37, BINARY_VALUE, nodeAddress)
                addPointToHaystack(humidifierPoint)
            }
            return  Point.Builder().setHashMap(readHumidifierRelayLogicalPoint(equipRef)).build()
        }

        fun createPointForDeHumidifier(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Short
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
                val deHumidifierPoint = point.build()
                addBacnetTags(deHumidifierPoint, 16, BINARY_VALUE, nodeAddress.toInt())
                addPointToHaystack(deHumidifierPoint)
            }
            return  Point.Builder().setHashMap(readDeHumidifierRelayLogicalPoint(equipRef)).build()
        }


        // 2PIPE specific points
         fun createAuxHeatingStage1Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readHeatingAux1RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "zone", "logical", "cmd", "runtime", "his",
                    "heating", "aux", "stage1", "electheating"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-auxHeatingstage1")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                val auxHeatingStage1 = point.build()
                addBacnetTags(auxHeatingStage1, 59, BINARY_VALUE,  nodeAddress)
                addPointToHaystack(auxHeatingStage1)
            }
            return  Point.Builder().setHashMap(readHeatingAux1RelayLogicalPoint(equipRef)).build()
        }
         fun createAuxHeatingStage2Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readHeatingAux2RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "zone", "logical", "cmd", "runtime", "his", "relay",
                    "heating", "aux", "stage2", "electheating"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-auxHeatingstage2")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                val auxHeatingStage2 = point.build()
                addBacnetTags(auxHeatingStage2, 60, BINARY_VALUE,  nodeAddress)
                addPointToHaystack(auxHeatingStage2)
            }
            return  Point.Builder().setHashMap(readHeatingAux2RelayLogicalPoint(equipRef)).build()
        }
         fun createWaterValvePoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readWaterValveRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "zone", "logical", "cmd", "runtime", "his", "relay",
                    "heating", "cooling", "water", "valve"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-waterValve")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                val waterValve = point.build()
                addBacnetTags(waterValve, 64, BINARY_VALUE,  nodeAddress)
                addPointToHaystack(waterValve)
            }
            return  Point.Builder().setHashMap(readWaterValveRelayLogicalPoint(equipRef)).build()
        }


        // HPU logical point creation

        fun createCompressorStage1Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point{
            val existingPoint = readCompressorStage1RelayLogicalPoint(equipRef)

            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "compressor", "stage1", "runtime", "hpu", "cur", "his",
                    "logical", "standalone", "zone")

                val point = Point.Builder()
                    .setDisplayName("$equipDis-compressorStage1")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                val compressorStage1 = point.build()
                addBacnetTags(compressorStage1, 56, BINARY_VALUE, nodeAddress)
                addPointToHaystack(compressorStage1)
            }
            return  Point.Builder().setHashMap(readCompressorStage1RelayLogicalPoint(equipRef)).build()
        }
        fun createCompressorStage2Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point{
            val existingPoint = readCompressorStage2RelayLogicalPoint(equipRef)

            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "compressor", "stage2", "runtime", "hpu", "cur", "his",
                    "logical", "standalone", "zone")

                val point = Point.Builder()
                    .setDisplayName("$equipDis-compressorStage2")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                val compressorStage2 = point.build()
                addBacnetTags(compressorStage2, 57, BINARY_VALUE, nodeAddress)
                addPointToHaystack(compressorStage2)
            }
            return  Point.Builder().setHashMap(readCompressorStage2RelayLogicalPoint(equipRef)).build()
        }
        fun createCompressorStage3Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point{
            val existingPoint = readCompressorStage3RelayLogicalPoint(equipRef)

            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "compressor", "stage3", "runtime", "hpu", "cur", "his",
                    "logical", "standalone", "zone")

                val point = Point.Builder()
                    .setDisplayName("$equipDis-compressorStage3")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                val compressorStage3 = point.build()
                addBacnetTags(compressorStage3, 58, BINARY_VALUE, nodeAddress)
                addPointToHaystack(compressorStage3)
            }
            return  Point.Builder().setHashMap(readCompressorStage3RelayLogicalPoint(equipRef)).build()
        }

        fun createChangeOverCoolingPoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readChangeOverCoolingRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "changeover", "cooling", "runtime", "hpu", "cur", "his",
                    "logical", "standalone", "zone")
                val point = Point.Builder()
                    .setDisplayName("$equipDis-changeOverCooling")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                point.setEnums("off,on")
                val changeOverCooling = point.build()
                addBacnetTags(changeOverCooling, 61, BINARY_VALUE, nodeAddress)
                addPointToHaystack(changeOverCooling)
            }
            return  Point.Builder().setHashMap(readChangeOverCoolingRelayLogicalPoint(equipRef)).build()
        }

        fun createChangeOverHeatingPoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readChangeOverHeatingRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "changeover", "heating", "runtime", "hpu", "cur", "his",
                    "logical", "standalone", "zone")
                val point = Point.Builder()
                    .setDisplayName("$equipDis-changeOverHeating")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                point.setEnums("off,on")
                val changeOverHeating = point.build()
                addBacnetTags(changeOverHeating, 62, BINARY_VALUE, nodeAddress)
                addPointToHaystack(changeOverHeating)
            }
            return  Point.Builder().setHashMap(readChangeOverHeatingRelayLogicalPoint(equipRef)).build()
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



        // 2PIPE specific points reading
         fun readHeatingAux1RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and heating and aux and stage1 and electheating and equipRef == \"$equipRef\"")
        }
         fun readHeatingAux2RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and heating and aux and stage2 and electheating and equipRef == \"$equipRef\"")
        }
         fun readWaterValveRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and water and valve and relay and equipRef == \"$equipRef\"")
        }

        // Hpu specific points reading

        fun readChangeOverCoolingRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and cooling and changeover and equipRef == \"$equipRef\"")
        }

        fun readChangeOverHeatingRelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and heating and changeover and equipRef == \"$equipRef\"")
        }

        fun readCompressorStage1RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and compressor and stage1 and equipRef == \"$equipRef\"")
        }
        fun readCompressorStage2RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and compressor and stage2 and equipRef == \"$equipRef\"")
        }
        fun readCompressorStage3RelayLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and compressor and stage3 and equipRef == \"$equipRef\"")
        }

        /**====== Analog Logical points=======**/

        fun createAnalogOutPointForFanSpeed(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readAnalogOutFanSpeedLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","analog", "output",
                    "fan", "speed","run","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                val fanSpeedPoint = point.build()
                addBacnetTags(fanSpeedPoint, 23, ANALOG_VALUE, nodeAddress)
                addPointToHaystack(fanSpeedPoint)
            }
            return Point.Builder().setHashMap(readAnalogOutFanSpeedLogicalPoint(equipRef)).build()
        }

         fun createAnalogOutPointForDCVDamper(
             equipDis: String, siteRef: String, equipRef: String,
             roomRef: String, floorRef: String, tz: String, nodeAddress: Int
         ): Point {
            val existingPoint = readAnalogOutDcvLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","analog", "output",
                    "damper", "dcv","actuator","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-dcvDamper")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                val dcvLogicalPoint = point.build()
                addBacnetTags(dcvLogicalPoint, 15, ANALOG_VALUE, nodeAddress)
                addPointToHaystack(dcvLogicalPoint)
            }
            return Point.Builder().setHashMap(readAnalogOutDcvLogicalPoint(equipRef)).build()
        }

        fun createAnalogOutPointForWaterValve(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readAnalogOutWaterValveLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "zone", "logical", "analog", "output",
                    "water", "valve","heating","cooling","his"
                )

                val point = Point.Builder()
                    .setDisplayName("$equipDis-modulatingWaterValve")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                val modulatingWaterValve = point.build()
                addBacnetTags(modulatingWaterValve, 65, BINARY_VALUE, nodeAddress)
                addPointToHaystack(modulatingWaterValve)
            }
            return  Point.Builder().setHashMap(readAnalogOutWaterValveLogicalPoint(equipRef)).build()
        }

        fun createAnalogOutCompressorSpeedValve(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readAnalogOutCompressorSpeedLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "zone", "logical", "analog", "output",
                    "compressor", "speed","his"
                )

                val point = Point.Builder()
                    .setDisplayName("$equipDis-compressorSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                val compressorSpeed = point.build()
                addBacnetTags(compressorSpeed, 63, ANALOG_VALUE, nodeAddress)
                addPointToHaystack(compressorSpeed)
            }
            return  Point.Builder().setHashMap(readAnalogOutCompressorSpeedLogicalPoint(equipRef)).build()
        }



        // Read analog logical points

        fun readAnalogOutWaterValveLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and water and valve and equipRef == \"$equipRef\"")
        }
        fun readAnalogOutFanSpeedLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and fan and speed and equipRef == \"$equipRef\"")
        }
        fun readAnalogOutDcvLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and dcv and damper and equipRef == \"$equipRef\"")
        }

        fun readAnalogOutCompressorSpeedLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and compressor and speed and equipRef == \"$equipRef\"")
        }


        /***   Thermistor logical points***/

        fun createPointForAirflowTempSensor(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val markers = arrayOf(
                "air","cur","discharge","logical","sensor","temp","his"
            )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-airflowTempSensor")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("\u00B0F")
                markers.forEach { point.addMarker(it) }
                val airFlowTempSensorPoint = point.build()
                addBacnetTags(airFlowTempSensorPoint, 1, ANALOG_VALUE, nodeAddress)
                return airFlowTempSensorPoint
        }


        fun createPointForDoorWindowSensor(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, windowSensorType: WindowSensorType
        ): Point {
            var window = "window"
            var doorWindowName = "doorWindowSensor"

            if(windowSensorType == WindowSensorType.WINDOW_SENSOR_2){
                  window = "window2"
                  doorWindowName = "doorWindowSensor_2"
              } else if(windowSensorType == WindowSensorType.WINDOW_SENSOR_3) {
                  window = "window3"
                  doorWindowName = "doorWindowSensor_3"
              }

            val markers = arrayOf(
                "logical","sensor","his","door","contact",window
            )
            val point = Point.Builder()
                .setDisplayName("$equipDis-$doorWindowName")
                .setSiteRef(siteRef).setEquipRef(equipRef)
                .setRoomRef(roomRef).setFloorRef(floorRef)
                .setTz(tz).setHisInterpolate("cov")
            markers.forEach { point.addMarker(it) }

            return point.build()
        }

        fun createPointForGenericFaultNC(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: String
        ): Point {

            val existingPoint = readGenericFaultNC(equipRef)
            if(existingPoint.isEmpty()) {

                val markers = arrayOf(
                    "cur", "his", "zone", "standalone", "cpu", "generic", "sensor", "alarm", "normallyClosed"
                )

                val point = Point.Builder()
                    .setDisplayName("$equipDis-genericFaultNC")
                    .setGroup(nodeAddress)
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setEnums("normal,fault")
                markers.forEach { point.addMarker(it) }
                val builtPoint = point.build()
                addBacnetTags(builtPoint, 91, BINARY_VALUE, nodeAddress.toInt())
                addPointToHaystack(builtPoint)
            }
            return Point.Builder().setHashMap(readGenericFaultNC(equipRef)).build()
        }

        fun createPointForGenericFaultNO(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: String
        ): Point {

            val existingPoint = readGenericFaultNO(equipRef)
            if(existingPoint.isEmpty()) {

                val markers = arrayOf(
                    "cur", "his", "zone", "standalone", "cpu", "generic", "sensor", "alarm", "normallyOpen"
                )

                val point = Point.Builder()
                    .setDisplayName("$equipDis-genericFaultNO")
                    .setGroup(nodeAddress)
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                    .setEnums("normal,fault")
                markers.forEach { point.addMarker(it) }
                val builtPoint = point.build()
                addBacnetTags(builtPoint, 91, BINARY_VALUE, nodeAddress.toInt())
                addPointToHaystack(builtPoint)
            }
            return Point.Builder().setHashMap(readGenericFaultNO(equipRef)).build()
        }

        fun createPointForKeyCardSensor(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
            analogType: Int): Point {

            var name = "keyCardSensor"
            var keycardMarker ="keycard"
            if(analogType ==2){
                name = "keyCardSensor_2"
                keycardMarker = "keycard2"
            }
            val markers = arrayOf(
                "logical","sensor","his",keycardMarker
            )
            val point = Point.Builder()
                .setDisplayName("$equipDis-$name")
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

        fun createPointForWaterSupplyTempSensor(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {

            val markers = arrayOf(
                "supply","water", "sensor", "temp","zone", "cmd","cur","his", "logical"
            )
            val point = Point.Builder()
                .setDisplayName("$equipDis-supplyWaterTemp")
                .setSiteRef(siteRef).setEquipRef(equipRef)
                .setRoomRef(roomRef).setFloorRef(floorRef)
                .setTz(tz).setHisInterpolate("cov").setUnit("\u00B0F")
            markers.forEach { point.addMarker(it) }

            return point.build()
        }



        /** Read sensor points**/
        private fun readCurrentTransferSensor(equipRef: String, transferTag: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "current and logical and sensor and $transferTag and equipRef == \"$equipRef\"")
        }

        fun readDoorWindowSensor(equipRef: String, windowType: WindowSensorType): HashMap<Any, Any> {
            var window = "window"
            if(windowType == WindowSensorType.WINDOW_SENSOR_2){
                window = "window2"
            } else if(windowType == WindowSensorType.WINDOW_SENSOR_3) {
                window = "window3"
            }
            return CCUHsApi.getInstance().readEntity(
                "logical and sensor and contact and door and $window and equipRef == \"$equipRef\"")
        }
        fun readAirflowTemperatureSensor(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "logical and sensor and temp and discharge and air and equipRef == \"$equipRef\"")
        }
        private fun readGenericFaultNC(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "generic and sensor and normallyClosed and zone and equipRef == \"$equipRef\"")
        }
        private fun readGenericFaultNO(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "generic and sensor and normallyOpen and zone and equipRef == \"$equipRef\"")
        }

        private fun readKeycardSensor(equipRef: String, keycardType: KeyCardSensorType): HashMap<Any, Any> {
            var keycardMarker ="keycard"
            if(keycardType == KeyCardSensorType.KEYCARD2 ){
                keycardMarker = "keycard2"
            }
            return CCUHsApi.getInstance().readEntity(
                "logical and sensor and $keycardMarker and equipRef == \"$equipRef\"")
        }


        fun cleanPipe2LogicalPoints(config: HyperStatPipe2Configuration, equipRef: String){
            removePipe2RelayLogicalPoints(config, equipRef)
            removePipe2AnalogLogicalPoints(config, equipRef)
            removeAnalogInLogicalPoints(equipRef,config,ProfileType.HYPERSTAT_TWO_PIPE_FCU)
            removeThermistorInLogicalPoints(equipRef,config)
        }
        private fun removePipe2RelayLogicalPoints(config: HyperStatPipe2Configuration, equipRef: String){
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToAuxHeatingStage1(config))
                removePoint(readHeatingAux1RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToAuxHeatingStage2(config))
                removePoint(readHeatingAux2RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToWaterValve(config))
                removePoint(readWaterValveRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToFanLow(config))
                removePoint(readFanLowRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToFanMedium(config))
                removePoint(readFanMediumRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToFanHigh(config))
                removePoint(readFanHighRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToFanEnabled(config))
                removePoint(readFanEnabledRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2RelayAssociatedToOccupiedEnabled(config))
                removePoint(readOccupiedEnabledRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2RelayEnabledAssociatedToHumidifier(config))
                removePoint(readHumidifierRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2RelayEnabledAssociatedToDeHumidifier(config))
                removePoint(readDeHumidifierRelayLogicalPoint(equipRef))
        }
        private fun removePipe2AnalogLogicalPoints(config: HyperStatPipe2Configuration, equipRef: String){
            if(!HyperStatAssociationUtil.isAnyPipe2AnalogAssociatedToWaterValve(config))
                removePoint(readAnalogOutWaterValveLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2AnalogAssociatedToFanSpeed(config))
                removePoint(readAnalogOutFanSpeedLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyPipe2AnalogAssociatedToDCV(config))
                removePoint(readAnalogOutDcvLogicalPoint(equipRef))
        }

        fun cleanHpuLogicalPoints(config: HyperStatHpuConfiguration, equipRef: String){
            removeHpuRelayLogicalPoints(config, equipRef)
            removeHpuAnalogLogicalPoints(config, equipRef)
            removeAnalogInLogicalPoints(equipRef,config,ProfileType.HYPERSTAT_HEAT_PUMP_UNIT)
            removeThermistorInLogicalPoints(equipRef,config)
        }
        private fun removeHpuRelayLogicalPoints(config: HyperStatHpuConfiguration, equipRef: String){
            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToCompressorStage1(config))
                removePoint(readCompressorStage1RelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToCompressorStage2(config))
                removePoint(readCompressorStage2RelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToCompressorStage3(config))
                removePoint(readCompressorStage3RelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToAuxHeatingStage1(config))
                removePoint(readHeatingAux1RelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToAuxHeatingStage2(config))
                removePoint(readHeatingAux2RelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToFanLow(config))
                removePoint(readFanLowRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToFanMedium(config))
                removePoint(readFanMediumRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToFanHigh(config))
                removePoint(readFanHighRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToFanEnabled(config))
                removePoint(readFanEnabledRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayAssociatedToOccupiedEnabled(config))
                removePoint(readOccupiedEnabledRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayEnabledAssociatedToHumidifier(config))
                removePoint(readHumidifierRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayEnabledAssociatedToDeHumidifier(config))
                removePoint(readDeHumidifierRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayEnabledAssociatedToDeHumidifier(config))
                removePoint(readDeHumidifierRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayEnabledAssociatedToChangeOverCooling(config))
                removePoint(readChangeOverCoolingRelayLogicalPoint(equipRef))

            if(!HyperStatAssociationUtil.isAnyHpuRelayEnabledAssociatedToChangeOverHeating(config))
                removePoint(readChangeOverHeatingRelayLogicalPoint(equipRef))
        }
        private fun removeHpuAnalogLogicalPoints(config: HyperStatHpuConfiguration, equipRef: String){
            if(!HyperStatAssociationUtil.isAnyHpuAnalogAssociatedToCompressorSpeed(config))
                removePoint(readAnalogOutCompressorSpeedLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyHpuAnalogAssociatedToFanSpeed(config))
                removePoint(readAnalogOutFanSpeedLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyHpuAnalogAssociatedToDCV(config))
                removePoint(readAnalogOutDcvLogicalPoint(equipRef))
        }

        private fun removeAnalogInLogicalPoints(
            equipRef: String, baseConfig: BaseProfileConfiguration, profileType: ProfileType
        ){

            var analogIn1State = AnalogInState(false,AnalogInAssociation.KEY_CARD_SENSOR)
            var analogIn2State = AnalogInState(false,AnalogInAssociation.KEY_CARD_SENSOR)

            when(profileType){
                ProfileType.HYPERSTAT_TWO_PIPE_FCU ->{
                    analogIn1State =  (baseConfig as HyperStatPipe2Configuration).analogIn1State
                    analogIn2State =  baseConfig.analogIn2State
                }
                ProfileType.HYPERSTAT_HEAT_PUMP_UNIT ->{
                    analogIn1State =  (baseConfig as HyperStatHpuConfiguration).analogIn1State
                    analogIn2State =  baseConfig.analogIn2State
                }
                else -> {}
            }
            if(!HyperStatAssociationUtil.isAnyAnalogInMappedCT10(analogIn1State,analogIn2State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer"))
            if(!HyperStatAssociationUtil.isAnyAnalogInMappedCT20(analogIn1State,analogIn2State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer20"))
            if(!HyperStatAssociationUtil.isAnyAnalogInMappedCT50(analogIn1State,analogIn2State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer50"))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn1State, AnalogInAssociation.KEY_CARD_SENSOR))
                removePoint(readKeycardSensor(equipRef,KeyCardSensorType.KEYCARD))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn2State, AnalogInAssociation.KEY_CARD_SENSOR))
                removePoint(readKeycardSensor(equipRef,KeyCardSensorType.KEYCARD2))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn1State, AnalogInAssociation.DOOR_WINDOW_SENSOR))
                removePoint(readDoorWindowSensor(equipRef,WindowSensorType.WINDOW_SENSOR_2))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn2State, AnalogInAssociation.DOOR_WINDOW_SENSOR))
                removePoint(readDoorWindowSensor(equipRef,WindowSensorType.WINDOW_SENSOR_3))
        }

        private fun removePoint(point: HashMap<Any, Any>){
            if(point.isNotEmpty()){
                CCUHsApi.getInstance().deleteEntityTree(Point.Builder().setHashMap(point).build().id)
            }
        }

        private fun removeThermistorInLogicalPoints(equipRef: String, config: HyperStatHpuConfiguration) {

            val thIn1State = Th1InState(config.thermistorIn1State.enabled, config.thermistorIn1State.association)
            val thIn2State = Th2InState(config.thermistorIn2State.enabled, config.thermistorIn2State.association)

            if(!HyperStatAssociationUtil.isTh1AirflowSensorEnabled(thIn1State))
                removePoint(readAirflowTemperatureSensor(equipRef))
            if(!HyperStatAssociationUtil.isTh2DoorWindowSensorEnabled(thIn2State))
                removePoint(readDoorWindowSensor(equipRef,WindowSensorType.WINDOW_SENSOR))
            if(!HyperStatAssociationUtil.isAnyThermistorAssociatedToGenericFaultNC(thIn1State, thIn2State))
                removePoint(readGenericFaultNC(equipRef))
            if(!HyperStatAssociationUtil.isAnyThermistorAssociatedToGenericFaultNO(thIn1State, thIn2State))
                removePoint(readGenericFaultNO(equipRef))

        }

        private fun removeThermistorInLogicalPoints(equipRef: String, config: HyperStatPipe2Configuration) {

            val thIn1State = Pipe2Th1InState(config.thermistorIn1State.enabled, config.thermistorIn1State.association)

            if(!HyperStatAssociationUtil.isTh1AirflowSensorEnabled(thIn1State))
                removePoint(readAirflowTemperatureSensor(equipRef))
            if(!HyperStatAssociationUtil.isTh1GenericFaultNCEnabled(thIn1State))
                removePoint(readGenericFaultNC(equipRef))
            if(!HyperStatAssociationUtil.isTh1GenericFaultNOEnabled(thIn1State))
                removePoint(readGenericFaultNO(equipRef))

        }

    }


    enum class WindowSensorType {
        WINDOW_SENSOR,WINDOW_SENSOR_2,WINDOW_SENSOR_3,
    }
    enum class TransformerSensorType {
        TRANSFORMER,TRANSFORMER_20,TRANSFORMER_50
    }
    enum class KeyCardSensorType {
        KEYCARD, KEYCARD2
    }
}