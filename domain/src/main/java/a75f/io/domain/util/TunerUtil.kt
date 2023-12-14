package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Tags
import a75f.io.domain.util.Constants.TAG_DM_CCU
import a75f.io.logger.CcuLog
import org.projecthaystack.HNum
import org.projecthaystack.HRef

object TunerUtil {

    fun updateTunerLevels(tunerPointId: String, zoneRef: String, domainName: String, hayStack: CCUHsApi) {

        //CcuLog.e(Constants.TAG_DM_CCU, "updateTunerLevels : $domainName")
        //If all the level are successfully copied from a zone tuner, no need to go further.
        if (copyFromZoneTuner(tunerPointId, zoneRef, domainName, hayStack)
        ) {
            return
        }
        if (copyFromSystemTuner(tunerPointId, domainName, hayStack)) {
            return
        }
        if (copyFromBuildingTuner(tunerPointId, domainName, hayStack)) {
            return
        }
        //CcuLog.e(Constants.TAG_DM_CCU, " Tuner initialization is not complete : $domainName")
    }

    private fun copyTunerLevel(
        dstPointId: String,
        srcArray: ArrayList<HashMap<*, *>>,
        level: Int,
        hayStack: CCUHsApi
    ): Boolean {
        val levelMap = srcArray[level - 1]
        if (levelMap != null && levelMap["val"] != null) {
            CcuLog.i(Constants.TAG_DM_CCU, " copyTunerLevel : $levelMap")
            hayStack.pointWrite(
                HRef.copy(dstPointId),
                level,
                levelMap["who"].toString(),
                HNum.make(levelMap["val"].toString().toDouble()),
                HNum.make(0)
            )
            return true
        }
        return false
    }

    private fun copyFromBuildingTuner(
        dstPointId: String,
        domainName:String,
        hayStack: CCUHsApi
    ): Boolean {
        CcuLog.e(Constants.TAG_DM_CCU, " copyFromBuildingTuner : ")

        //building tuners like forcedOccupiedTime,adrCoolingDeadband,adrHeatingDeadband don't have zone marker,so try one more time without zone marker
        val buildingTunerPoint = hayStack.readEntity(
            "point and tuner and default and domainName ==\"$domainName\""
        )

        if (buildingTunerPoint.isEmpty()) {
            CcuLog.e(Constants.TAG_DM_CCU, " copyFromBuildingTuner Failed: $domainName")
            return false
        }
        val buildingTunerPointArray = hayStack.readPoint(buildingTunerPoint["id"].toString())
        copyTunerLevel(dstPointId, buildingTunerPointArray, 16, hayStack)

        //Should always succeed. Otherwise indicates some bug.
        return copyTunerLevel(dstPointId, buildingTunerPointArray, 17, hayStack)
    }

    private fun copyFromSystemTuner(
        dstPointId: String,
        domainName: String,
        hayStack: CCUHsApi
    ): Boolean {
        val systemTunerPoints = hayStack.readAllEntities("point and tuner and not default and domainName ==\"$domainName\"")
        val systemTunerPoint = systemTunerPoints.stream()
            .filter { point: java.util.HashMap<*, *> ->
                point["id"].toString() != dstPointId
            }
            .findFirst()
        if (!systemTunerPoint.isPresent) return false
        CcuLog.e(Constants.TAG_DM_CCU, " copyFromSystemTuner : $systemTunerPoint")
        val systemTunerPointArray = hayStack.readPoint(systemTunerPoint.get()["id"].toString())

        return (copyTunerLevel(dstPointId, systemTunerPointArray, 14, hayStack)
                && copyTunerLevel(dstPointId, systemTunerPointArray, 16, hayStack)
                && copyTunerLevel(dstPointId, systemTunerPointArray, 17, hayStack
        ))
    }

    private fun copyFromZoneTuner(
        dstPointId: String, zoneRef: String, domainName: String,
        hayStack: CCUHsApi
    ): Boolean {

        val zoneTunerPoints = hayStack.readAllEntities("domainName == \"$domainName\" and roomRef == \"$zoneRef\"")
        val zoneTunerPoint = zoneTunerPoints.stream()
            .filter { point: java.util.HashMap<*, *> ->
                point["id"].toString() != dstPointId
            }
            .findFirst()
        if (!zoneTunerPoint.isPresent) return false
        CcuLog.e(Constants.TAG_DM_CCU, " copyFromZoneTuner : $zoneTunerPoint")
        val zoneTunerPointArray = hayStack.readPoint(zoneTunerPoint.get()["id"].toString())
        return (copyTunerLevel(dstPointId, zoneTunerPointArray, 10, hayStack)
                && copyTunerLevel(dstPointId, zoneTunerPointArray, 14, hayStack)
                && copyTunerLevel(dstPointId, zoneTunerPointArray, 16, hayStack)
                && copyTunerLevel(dstPointId, zoneTunerPointArray, 17, hayStack))
    }

    fun copyDefaultBuildingTunerVal(
        systemPointId: String?,
        domainName: String,
        hayStack: CCUHsApi
    ) {
        val buildingPoint = hayStack.readDefaultPointByDomainName(domainName)
        if (buildingPoint.isEmpty()) {
            CcuLog.e(TAG_DM_CCU, "!! Default point does not exist for $domainName")
            return
        }
        val buildingPointArray = hayStack.readPoint(buildingPoint[Tags.ID].toString())
        for (valMap in buildingPointArray) {
            if (valMap["val"] != null) {
                hayStack.pointWrite(
                    HRef.copy(systemPointId),
                    valMap["level"].toString().toDouble().toInt(),
                    valMap["who"].toString(),
                    HNum.make(
                        valMap["val"].toString().toDouble()
                    ),
                    HNum.make(0)
                )
            }
        }
        CcuLog.e(TAG_DM_CCU, "Copy default value for $domainName $buildingPointArray")
        hayStack.writeHisValById(systemPointId, HSUtil.getPriorityVal(systemPointId))
    }

}