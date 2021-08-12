package a75f.io.logic.bo.building.vrv

import a75f.io.api.haystack.CCUHsApi

enum class VrvOperationMode {
    OFF, FAN, HEAT, COOL, AUTO
}
enum class VrvFanSpeed {
    Low, Medium, High, Auto
}

enum class VrvAirflowDirection {
    Position0, Position1, Position2, Position3, Position4, Swing, Auto
}

enum class VrvMasterController {
    NotMaster {
              override fun toString() = "Not-Master"
    }, Master
}

fun getEquipPointsForView(equipId : String, hayStack : CCUHsApi) : java.util.HashMap<Any?, Any?> {
    val vrvPoints = java.util.HashMap<Any?, Any?>()
    vrvPoints["Profile"] = "HyperStat VRV"

    val equipStatus =
        hayStack.readDefaultStrVal("point and status and message and equipRef == \"$equipId\"")
    if (equipStatus.isNotEmpty()) {
        vrvPoints["Status"] = equipStatus
    } else {
        vrvPoints["Status"] = "OFF"
    }

    vrvPoints["operationMode"] = hayStack.readDefaultVal("point and userIntent and operation and mode " +
                                                "and equipRef == \"$equipId\"")
    vrvPoints["airflowDirection"] = hayStack.readDefaultVal("point and userIntent and airflow and direction " +
                                                "and equipRef == \"$equipId\"")
    vrvPoints["fanSpeed"] = hayStack.readDefaultVal("point and userIntent and fanSpeed " +
                                                 "and equipRef == \"$equipId\"")
    vrvPoints["coolHeatOptionRight"] = hayStack.readDefaultVal("point and userIntent and coolHeatOptionRight " +
                                                "and equipRef == \"$equipId\"")
    vrvPoints["humidity"] = hayStack.readHisValByQuery("point and humidity and sensor " +
                                                "and equipRef == \"$equipId\"")

    vrvPoints["masterOperationMode"] = hayStack.readHisValByQuery("point and master and operation and mode " +
            "and equipRef == \"$equipId\"")

    return vrvPoints
}