package a75f.io.logic.bo.building.vrv

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point

enum class VrvOperationMode {
    OFF {
        override fun toString() = "Off"
    },
    FAN {
        override fun toString() = "Fan (Ventilation)"
    },
    HEAT {
        override fun toString() = "Heat only mode"
    },
    COOL {
        override fun toString() = "Cool only mode"
    },
    AUTO {
        override fun toString() = "Auto"
    }
}
enum class VrvFanSpeed {
    Low, Medium, High, Auto
}

enum class VrvAirflowDirection {
    Position0, Position1, Position2, Position3, Position4, Swing, Auto
}

enum class VrvMasterController {
    NOT_MASTER {
              override fun toString() = "Not-Master"
    },
    MASTER {
        override fun toString() = "Master"
    }
}

enum class IduThermister{
    th1, th2, th3, th4, th5, th6
}
public class VrvPoints {


    companion object {
        fun createThermisterPoints(
                equip: Equip,
                roomRef: String,
                floorRef: String,
                nodeAddr: Short,
                hayStack: CCUHsApi
        ) {
            val thermistorName: Array<String> = arrayOf("iduTh1", "iduTh2", "iduTh3", "iduTh4", "iduTh5", "iduTh6")
            IduThermister.values().forEach {
                val th1 = Point.Builder()
                        .setDisplayName(equip.displayName + "-" + thermistorName[it.ordinal])
                        .setEquipRef(equip.id)
                        .setSiteRef(equip.siteRef)
                        .setRoomRef(roomRef)
                        .setFloorRef(floorRef)
                        .setHisInterpolate("cov")
                        .addMarker("zone").addMarker("vrv")
                        .addMarker("idu").addMarker(it.name).addMarker("sensor").addMarker("his")
                        .addMarker("cur").addMarker("logical")
                        .setUnit("\u00B0F")
                        .setGroup(nodeAddr.toString())
                        .setTz(equip.tz)
                        .build()
                val th1ID = hayStack.addPoint(th1)
                hayStack.writeHisValById(th1ID, 0.0)
            }
        }

        fun createTestOperationPoint(
                equip: Equip,
                roomRef: String,
                floorRef: String,
                nodeAddr: Short,
                hayStack: CCUHsApi
        ) {
            val testOp = Point.Builder()
                    .setDisplayName(equip.displayName + "-testOperation")
                    .setEquipRef(equip.id)
                    .setSiteRef(equip.siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .setHisInterpolate("cov")
                    .addMarker("zone").addMarker("vrv")
                    .addMarker("idu").addMarker("testOperation").addMarker("sensor").addMarker("his")
                    .addMarker("cur").addMarker("logical")
                    .setGroup(nodeAddr.toString())
                    .setEnums("testOpNotInProgress,testOpInProgress")
                    .setTz(equip.tz)
                    .build()
            val th1ID = hayStack.addPoint(testOp)
            hayStack.writeHisValById(th1ID, 0.0)
        }

        fun createTelecoCheckPoint(
                equip: Equip,
                roomRef: String,
                floorRef: String,
                nodeAddr: Short,
                hayStack: CCUHsApi
        ) {
            val telcoCheck = Point.Builder()
                    .setDisplayName(equip.displayName + "-telecoCheck")
                    .setEquipRef(equip.id)
                    .setSiteRef(equip.siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .setHisInterpolate("cov")
                    .addMarker("zone").addMarker("vrv")
                    .addMarker("idu").addMarker("telecoCheck").addMarker("sensor").addMarker("his")
                    .addMarker("cur").addMarker("logical")
                    .setGroup(nodeAddr.toString())
                    .setEnums( "telecoInactive,telecoCheckActive")
                    .setTz(equip.tz)
                    .build()
            val th1ID = hayStack.addPoint(telcoCheck)
            hayStack.writeHisValById(th1ID, 0.0)
        }
    }
}