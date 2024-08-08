package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HSUtil.isEquipHasEquipsWithAhuRefOnThisCcu
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.domain.api.Domain
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.sync.PointWriteCache
import a75f.io.domain.util.Constants.TAG_DM_CCU
import a75f.io.logger.CcuLog
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import org.projecthaystack.HVal
import org.projecthaystack.io.HZincWriter

object TunerUtil {

    fun updateSystemTunerVal(tags: String, tunerVal: Double?, reason: String, hayStack: CCUHsApi) {
        val tunerPoint = hayStack.readEntity("tuner and tunerGroup and system and roomRef == \"SYSTEM\" and ccuRef == \"" + hayStack.ccuId + "\" and " + tags)
        val tunerId = tunerPoint.get("id").toString()
        val systemEquipId = tunerPoint.get("equipRef").toString()
        if (tunerVal == null) {
            hayStack.getHSClient().pointWrite(HRef.copy(tunerId), 14, hayStack.ccuUserName, HNum.make(getTuner(tunerId)), HNum.make(1), HDateTime.make(System.currentTimeMillis()))
            val b: HDictBuilder = HDictBuilder().add("id", HRef.copy(tunerId)).add("level",14).add("who",CCUHsApi.getInstance().getCCUUserName()).add("duration", HNum.make(0, "ms")).add("val", null as? HVal).add("reason", reason)
            PointWriteCache.getInstance().writePoint(tunerId, b.toDict())
            hayStack.writeHisValById(tunerId, HSUtil.getPriorityVal(tunerId))
        } else {
            hayStack.writePointForCcuUser(tunerId, 14, tunerVal, 0, reason)
            hayStack.writeHisValById(tunerId, tunerVal)
        }
        if (isEquipHasEquipsWithAhuRefOnThisCcu(systemEquipId)) updateChildEquipsTunerVal(systemEquipId, tags, tunerVal, reason, hayStack)
    }

    fun updateChildEquipsTunerVal(systemEquipId: String, tags: String, tunerVal: Double?, reason: String, hayStack: CCUHsApi) {
        val childEquips = HSUtil.getEquipsWithAhuRefOnThisCcu(systemEquipId)
        val childEquipsIterator = childEquips.iterator()
        while (childEquipsIterator.hasNext()) {
            val childEquipId = childEquipsIterator.next().id.toString()
            val childTunerId = hayStack.readEntity("point and tuner and equipRef == \"" + childEquipId + "\" and " + tags).get("id").toString()
            if (childTunerId != null) {
                if (tunerVal == null) {
                    hayStack.getHSClient().pointWrite(HRef.copy(childTunerId), 14, hayStack.ccuUserName, HNum.make(getTuner(childTunerId)), HNum.make(1), HDateTime.make(System.currentTimeMillis()))
                    val b: HDictBuilder = HDictBuilder().add("id", HRef.copy(childTunerId)).add("level",14).add("who",CCUHsApi.getInstance().getCCUUserName()).add("duration", HNum.make(0, "ms")).add("val", null as? HVal).add("reason", reason)
                    PointWriteCache.getInstance().writePoint(childTunerId, b.toDict())
                    hayStack.writeHisValById(childTunerId, HSUtil.getPriorityVal(childTunerId))
                } else {
                    hayStack.writePointForCcuUser(childTunerId, 14, tunerVal, 0, reason)
                    hayStack.writeHisValById(childTunerId, tunerVal)
                }
            }
        }
    }

    fun getTuner(id: String?): Double {
        val hayStack = CCUHsApi.getInstance()
        val values: java.util.ArrayList<*>? = hayStack.readPoint(id)
        if (values != null && values.size > 0) {
            for (l in 1..values.size) {
                val valMap = values[l - 1] as java.util.HashMap<*, *>
                if (valMap["val"] != null) {
                    return valMap["val"].toString().toDouble()
                }
            }
        }
        return 0.0
    }

    fun updateTunerLevels(tunerPointId: String, zoneRef: String, domainName: String, hayStack: CCUHsApi, defaultVal: Double) {

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

        CcuLog.e(Domain.LOG_TAG_TUNER, "Tuner Copy failed, copying fallback value to tuner point $domainName")
        hayStack.pointWrite(
            HRef.copy(tunerPointId),
            17,
            CCUHsApi.getInstance().ccuUserName,
            HNum.make(defaultVal),
            HNum.make(0)
        )
    }

    fun updateSystemTunerLevels(tunerPointId: String, domainName: String, hayStack: CCUHsApi, defaultVal: Double) {

        if (copyFromBuildingTuner(tunerPointId, domainName, hayStack)) {
            return
        }

        CcuLog.e(Domain.LOG_TAG_TUNER, "Tuner Copy failed, copying fallback value to tuner point $domainName")
        hayStack.pointWrite(
            HRef.copy(tunerPointId),
            17,
            CCUHsApi.getInstance().ccuUserName,
            HNum.make(defaultVal),
            HNum.make(0)
        )
    }

    private fun copyTunerLevel(
        dstPointId: String,
        srcArray: ArrayList<HashMap<*, *>>,
        level: Int,
        hayStack: CCUHsApi
    ): Boolean {
        val levelMap = srcArray[level - 1]
        if (levelMap != null && levelMap["val"] != null) {
            CcuLog.i(Domain.LOG_TAG_TUNER, " copyTunerLevel : $levelMap")
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
        CcuLog.i(Domain.LOG_TAG_TUNER, " copyFromBuildingTuner : ")

         val buildingTunerPoint = hayStack.readEntity(
            "point and tuner and default and domainName == \"$domainName\""
        )

        if (buildingTunerPoint.isEmpty()) {
            CcuLog.e(Domain.LOG_TAG_TUNER, " copyFromBuildingTuner Failed: $domainName")
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
        val systemTunerPoints = hayStack.readAllEntities("point and tuner and not default and domainName == \"$domainName\"")
        val systemTunerPoint = systemTunerPoints.stream()
            .filter { point: java.util.HashMap<*, *> ->
                point["id"].toString() != dstPointId
            }
            .findFirst()
        if (!systemTunerPoint.isPresent) return false
        CcuLog.i(Domain.LOG_TAG_TUNER, " copyFromSystemTuner : $systemTunerPoint")
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
        CcuLog.i(Domain.LOG_TAG_TUNER, " copyFromZoneTuner : $zoneTunerPoint")
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