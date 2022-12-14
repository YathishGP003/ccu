package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.logic.bo.building.BaseProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInState
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.HyperStatCpuConfiguration
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
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readFanLowRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","logical","runtime","writable","zone","his","fan", "low", "speed"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanlowspeed")
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
                    "cmd","logical","runtime","writable","zone","his","fan", "medium", "speed"
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
                    "cmd","logical","runtime","writable","zone","his","fan", "high", "speed"
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
                    "cmd", "logical","runtime", "his", "writable", "zone", "fan", "enabled"
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
                    "cmd", "logical", "runtime", "his", "writable", "zone", "occupied", "enabled"
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
                    "cmd", "logical", "runtime", "his", "writable", "zone", "humidifier"
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
                    "cmd", "logical", "runtime", "his", "writable", "zone", "dehumidifier"
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
                    "cmd", "cooling", "stage1", "dxCooling", "zone", "logical", "runtime","writable","his"
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
                    "cmd", "cooling", "stage2", "dxCooling", "zone", "logical", "runtime","writable","his"
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
                    "cmd", "cooling", "stage3", "dxCooling", "zone", "logical", "runtime","writable","his"
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
                    "cmd", "heating", "stage1", "electHeating", "zone", "logical", "runtime","writable","his"
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
                    "cmd", "heating", "stage2", "electHeating", "zone", "logical", "runtime","writable","his"
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
                    "cmd", "heating", "stage3", "electHeating", "zone", "logical", "runtime","writable","his"
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


        // 2PIPE specific points
         fun createAuxHeatingStage1Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readHeatingAux1RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "zone", "logical", "cmd", "runtime", "his", "writable",
                    "heating", "aux", "stage1", "electheating"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-auxHeatingstage1")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readHeatingAux1RelayLogicalPoint(equipRef)).build()
        }
         fun createAuxHeatingStage2Point(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readHeatingAux2RelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "zone", "logical", "cmd", "runtime", "his", "writable", "relay",
                    "heating", "aux", "stage2", "electheating"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-auxHeatingstage2")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readHeatingAux2RelayLogicalPoint(equipRef)).build()
        }
         fun createWaterValvePoint(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
        ): Point {
            val existingPoint = readWaterValveRelayLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "zone", "logical", "cmd", "runtime", "his", "writable", "relay",
                    "heating", "cooling", "water", "valve"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-waterValve")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readWaterValveRelayLogicalPoint(equipRef)).build()
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

        /**====== Analog Logical points=======**/

         fun createAnalogOutPointForHeating(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
         ): Point {
            val existingPoint = readAnalogHeatingLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","analog", "output", "runtime", "writable",
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
                    "cmd","zone","logical","analog", "output", "runtime", "writable",
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
                    "cmd","zone","logical","analog", "output", "runtime", "writable",
                    "fan", "speed","run","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-fanSpeed")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readAnalogOutFanSpeedLogicalPoint(equipRef)).build()
        }

         fun createAnalogOutPointForDCVDamper(
             equipDis: String, siteRef: String, equipRef: String,
             roomRef: String, floorRef: String, tz: String,
         ): Point {
            val existingPoint = readAnalogOutDcvLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd","zone","logical","analog", "output", "runtime", "writable",
                    "damper", "dcv","actuator","his"
                )
                val point = Point.Builder()
                    .setDisplayName("$equipDis-dcvDamper")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return Point.Builder().setHashMap(readAnalogOutDcvLogicalPoint(equipRef)).build()
        }
         fun createAnalogOutPointForWaterValve(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String
        ): Point {
            val existingPoint = readAnalogOutWaterValveLogicalPoint(equipRef)
            if(existingPoint.isEmpty()) {
                val markers = arrayOf(
                    "cmd", "zone", "logical", "analog", "output", "runtime", "writable",
                    "water", "valve","heating","cooling","his"
                )

                val point = Point.Builder()
                    .setDisplayName("$equipDis-modulatingWaterValve")
                    .setSiteRef(siteRef).setEquipRef(equipRef)
                    .setRoomRef(roomRef).setFloorRef(floorRef)
                    .setTz(tz).setHisInterpolate("cov").setUnit("%")
                markers.forEach { point.addMarker(it) }
                addPointToHaystack(point.build())
            }
            return  Point.Builder().setHashMap(readAnalogOutWaterValveLogicalPoint(equipRef)).build()
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


        /***   Thermistor logical points***/

        fun createPointForAirflowTempSensor(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String,
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
                return point.build()
        }


        fun createPointForDoorWindowSensor(
            equipDis: String, siteRef: String, equipRef: String,
            roomRef: String, floorRef: String, tz: String, windowType: Int
        ): Point {
            var window = "window"
            var doorWindowName= "doorWindowSensor"

              if(windowType == 2){
                  window = "window2"
                  doorWindowName = "doorWindowSensor_2"
              }
              else if(windowType == 3) {
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
            min: String, max: String, inc: String, unit: String, tranferType: Int
        ): Point {

            var transformerTag = "transformer"
            var name = "currentDrawn_10"
            if(tranferType == 20) {
                transformerTag = "transformer20"
                name = "currentDrawn_20"
            }
            if(tranferType == 50) {
                transformerTag = "transformer50"
                name = "currentDrawn_50"
            }
            val existingPoint = readCurrentTransferSensor(equipRef,transformerTag)
            if(existingPoint.isEmpty()) {

                val markers = arrayOf(
                    "current", "sensor", "zone", "his", "logical", transformerTag
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
        fun readDoorWindowSensor(equipRef: String, windowType: Int): HashMap<Any, Any> {
            var window = "window"
            if(windowType == 2){
                window = "window2"
            } else if(windowType == 3) {
                window = "window3"
            }
            return CCUHsApi.getInstance().readEntity(
                "logical and sensor and contact and door and $window and equipRef == \"$equipRef\"")
        }

        private fun readKeycardSensor(equipRef: String, keycardType: Int): HashMap<Any, Any> {
            var keycardMarker ="keycard"
            if(keycardType == 2 ){
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
                removePoint(readAnalogOutFanSpeedLogicalPoint(equipRef))
            if(!HyperStatAssociationUtil.isAnyAnalogAssociatedToDCV(config))
                removePoint(readAnalogOutDcvLogicalPoint(equipRef))
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
                else -> {}
            }
            if(!HyperStatAssociationUtil.isAnyAnalogInMappedCT10(analogIn1State,analogIn2State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer"))
            if(!HyperStatAssociationUtil.isAnyAnalogInMappedCT20(analogIn1State,analogIn2State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer20"))
            if(!HyperStatAssociationUtil.isAnyAnalogInMappedCT50(analogIn1State,analogIn2State))
                removePoint(readCurrentTransferSensor(equipRef,"transformer50"))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn1State, AnalogInAssociation.KEY_CARD_SENSOR))
                removePoint(readKeycardSensor(equipRef,1))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn2State, AnalogInAssociation.KEY_CARD_SENSOR))
                removePoint(readKeycardSensor(equipRef,2))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn1State, AnalogInAssociation.DOOR_WINDOW_SENSOR))
                removePoint(readDoorWindowSensor(equipRef,2))
            if(!HyperStatAssociationUtil.isAnalogInMappedTo(analogIn2State, AnalogInAssociation.DOOR_WINDOW_SENSOR))
                removePoint(readDoorWindowSensor(equipRef,3))
        }
        private fun removePoint(point: HashMap<Any, Any>){
            if(point.isNotEmpty()){
                CCUHsApi.getInstance().deleteEntityTree(Point.Builder().setHashMap(point).build().id)
            }
        }
    }

}