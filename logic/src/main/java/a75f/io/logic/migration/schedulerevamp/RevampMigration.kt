package a75f.io.logic.migration.schedulerevamp

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.util.*
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.util.RxjavaUtil
import a75f.io.util.ExecutorTask
import org.apache.commons.lang3.StringUtils
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGridBuilder
import org.projecthaystack.client.HClient

const val CMD = "SCHEDULE_MIGRATED"

fun handleMessage(){
    val ccuHsApi = CCUHsApi.getInstance()
    CcuLog.d(L.TAG_SCHEDULABLE,"handle SCHEDULE_MIGRATED")
    RxjavaUtil.executeBackground {
        val siteRef = CCUHsApi.getInstance().siteIdRef.toString()
        val hClient =
            HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS)

        //tunerequipref
        val buildingEquipRef = hayStack.readEntity("equip and tuner")

        val buildingSchedulable = HDictBuilder().add(
            "filter", "schedulable and default and equipRef == "
                    + StringUtils.prependIfMissing(buildingEquipRef["id"].toString(), "@")
        ).toDict()
        val buildingSchedulableGrid =
            hClient.call("read", HGridBuilder.dictToGrid(buildingSchedulable))
        CcuLog.d(L.TAG_SCHEDULABLE,"buildingSchedulableGrid = "+buildingSchedulableGrid.numRows())
        CCUHsApi.getInstance().updateSchedulable(buildingSchedulableGrid,false)


        //foreach zone
        val zones = hayStack.readAllEntities("room")
        for(zone in zones){
            val schedulableDict = HDictBuilder().add(
                "filter", "schedulable and zone and roomRef == "
                        + StringUtils.prependIfMissing(zone["id"].toString(), "@")
            ).toDict()
            val schedulableGrid =
                hClient.call("read", HGridBuilder.dictToGrid(schedulableDict))
            CcuLog.d(L.TAG_SCHEDULABLE,"schedulableGrid = "+ zone["dis"].toString()+"==  "+schedulableGrid.numRows())
            CCUHsApi.getInstance().updateSchedulable(schedulableGrid, true)
        }

        val buildingLimitDict = HDictBuilder().add(
            "filter",
            "building and (limit or differential) and not tuner and siteRef == "
                    + StringUtils.prependIfMissing(siteRef, "@")
        ).toDict()
        val buildingLimitGrid =
            hClient.call("read", HGridBuilder.dictToGrid(buildingLimitDict))
        CCUHsApi.getInstance().updateSchedulable(buildingLimitGrid, false)


        ExecutorTask.executeAsync({ importSchedules() },
            { doPointWriteForSchedulable() },
            { setDiagMigrationVal() })
    }

}