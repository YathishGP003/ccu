package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.logic.ANALOG_VALUE
import a75f.io.logic.BINARY_VALUE
import a75f.io.logic.addBacnetTags
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.hpu.HyperStatHpuConfiguration
import a75f.io.logic.bo.building.hyperstat.profiles.pipe2.HyperStatPipe2Configuration
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


        // CPU specific points
        fun createCoolingStage1Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val coolingStage1Point = point.build()
                addBacnetTags(coolingStage1Point, 9, BINARY_VALUE, nodeAddress)
                addPointToHaystack(coolingStage1Point)
            }
            return  Point.Builder().setHashMap(readCoolingStage1RelayLogicalPoint(equipRef)).build()
        }

         fun createCoolingStage2Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val coolingStage2Point = point.build()
                addBacnetTags(coolingStage2Point, 10, BINARY_VALUE, nodeAddress)
                addPointToHaystack(coolingStage2Point)
            }
            return  Point.Builder().setHashMap(readCoolingStage2RelayLogicalPoint(equipRef)).build()
        }

         fun createCoolingStage3Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val coolingStage3Point = point.build()
                addBacnetTags(coolingStage3Point, 11, BINARY_VALUE, nodeAddress)
                addPointToHaystack(coolingStage3Point)
            }
            return  Point.Builder().setHashMap(readCoolingStage3RelayLogicalPoint(equipRef)).build()
        }

         fun createHeatingStage1Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val heatingStage1Point = point.build()
                addBacnetTags(heatingStage1Point, 32, BINARY_VALUE, nodeAddress)
                addPointToHaystack(heatingStage1Point)
            }
            return  Point.Builder().setHashMap(readHeatingStage1RelayLogicalPoint(equipRef)).build()
        }

         fun createHeatingStage2Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val heatingStage2Point = point.build()
                addBacnetTags(heatingStage2Point, 33, BINARY_VALUE,  nodeAddress)
                addPointToHaystack(heatingStage2Point)
            }
            return  Point.Builder().setHashMap(readHeatingStage2RelayLogicalPoint(equipRef)).build()
        }

         fun createHeatingStage3Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val heatingStage3Point = point.build()
                addBacnetTags(heatingStage3Point, 34, BINARY_VALUE,  nodeAddress)
                addPointToHaystack(heatingStage3Point)
            }
            return  Point.Builder().setHashMap(readHeatingStage3RelayLogicalPoint(equipRef)).build()
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

        // CPU specific points reading
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

         fun createAnalogOutPointForHeating(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val modulatingHeating = point.build()
                addBacnetTags(modulatingHeating, 30, ANALOG_VALUE, nodeAddress)
                addPointToHaystack(modulatingHeating)
            }
            return Point.Builder().setHashMap(readAnalogHeatingLogicalPoint(equipRef)).build()
        }
         fun createAnalogOutPointForCooling(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
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
                val modulatingCoolingPoint = point.build()
                addBacnetTags(modulatingCoolingPoint, 7, ANALOG_VALUE, nodeAddress)
                addPointToHaystack(modulatingCoolingPoint)
                return Point.Builder().setHashMap(readAnalogCoolingLogicalPoint(equipRef)).build()
            }
            return Point.Builder().setHashMap(readAnalogCoolingLogicalPoint(equipRef)).build()
        }

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

        fun createAnalogOutPointForPredefinedFanSpeed(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: String,
        ): Point {
            val existingPoint = readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","cur","standalone",
                    "fan", "speed","his","predefined","cpu"
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
        fun createAnalogOutPointForModulatingFanSpeed(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, nodeAddress: Int
        ): Point {
            val existingPoint = readAnalogOutModulatingFanSpeedLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","cur",
                    "fan", "speed","his","modulating","cpu","standalone"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-modulatingFanSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef).setGroup(nodeAddress.toString())
                    .setRoomRef(roomRef).setFloorRef(floorRef).setKind(Kind.NUMBER)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                val fanSpeedPoint = point.build()
                addBacnetTags(fanSpeedPoint, 23, ANALOG_VALUE, nodeAddress)
                addPointToHaystack(fanSpeedPoint)
            }
            return Point.Builder().setHashMap(readAnalogOutModulatingFanSpeedLogicalPoint(equipRef)).build()
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
        fun readAnalogCoolingLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and cooling and equipRef == \"$equipRef\"")
        }
        fun readAnalogHeatingLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and heating and equipRef == \"$equipRef\"")
        }
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
        fun readAnalogOutModulatingFanSpeedLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "point and logical and fan and modulating and speed and equipRef == \"$equipRef\"")
        }

        fun readAnalogOutCompressorSpeedLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "analog and logical and output and compressor and speed and equipRef == \"$equipRef\"")
        }

        fun readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance().readEntity(
                "point and logical and fan and predefined and speed and equipRef == \"$equipRef\"")
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

        private fun readKeycardSensor(equipRef: String, keycardType: KeyCardSensorType): HashMap<Any, Any> {
            var keycardMarker ="keycard"
            if(keycardType == KeyCardSensorType.KEYCARD2 ){
                keycardMarker = "keycard2"
            }
            return CCUHsApi.getInstance().readEntity(
                "logical and sensor and $keycardMarker and equipRef == \"$equipRef\"")
        }


        /** Delete invalid logical points  ***/


        fun cleanCpuLogicalPoints(config: HyperStatCpuConfiguration, equipRef: String){
            removeCpuRelayLogicalPoints(config, equipRef)
            removeCpuAnalogLogicalPoints(config, equipRef)
            removeAnalogInLogicalPoints(equipRef,config,ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT)
        }
        private fun removeCpuRelayLogicalPoints(config: HyperStatCpuConfiguration, equipRef: String){
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToCoolingStage1(config))
                removePoint(readCoolingStage1RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToCoolingStage2(config))
                removePoint(readCoolingStage2RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToCoolingStage3(config))
                removePoint(readCoolingStage3RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToHeatingStage1(config))
                removePoint(readHeatingStage1RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToHeatingStage2(config))
                removePoint(readHeatingStage2RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToHeatingStage3(config))
                removePoint(readHeatingStage3RelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToFanLow(config))
                removePoint(readFanLowRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToFanMedium(config))
                removePoint(readFanMediumRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToFanHigh(config))
                removePoint(readFanHighRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToFanEnabled(config))
                removePoint(readFanEnabledRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayAssociatedToOccupiedEnabled(config))
                removePoint(readOccupiedEnabledRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToHumidifier(config))
                removePoint(readHumidifierRelayLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToDeHumidifier(config))
                removePoint(readDeHumidifierRelayLogicalPoint(equipRef))
        }
        private fun removeCpuAnalogLogicalPoints(config: HyperStatCpuConfiguration, equipRef: String){
            if(!HyperStatAssociationUtil.isAnyAnalogAssociatedToCooling(config))
                removePoint(readAnalogCoolingLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyAnalogAssociatedToHeating(config))
                removePoint(readAnalogHeatingLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyAnalogAssociatedToFan(config))
                removePoint(readAnalogOutModulatingFanSpeedLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyAnalogAssociatedToDCV(config))
                removePoint(readAnalogOutDcvLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyAnalogAssociatedToStaged(config))
                removePoint(readAnalogOutPredefinedFanSpeedLogicalPoint(equipRef))
        }

        fun cleanPipe2LogicalPoints(config: HyperStatPipe2Configuration, equipRef: String){
            removePipe2RelayLogicalPoints(config, equipRef)
            removePipe2AnalogLogicalPoints(config, equipRef)
            removeAnalogInLogicalPoints(equipRef,config,ProfileType.HYPERSTAT_TWO_PIPE_FCU)
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
                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT ->{
                    analogIn1State =  (baseConfig as HyperStatCpuConfiguration).analogIn1State
                    analogIn2State =  baseConfig.analogIn2State
                }
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