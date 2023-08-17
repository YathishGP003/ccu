package a75f.io.logic.migration.schedulerevamp

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.util.*
import a75f.io.logic.L
import a75f.io.logic.util.RxjavaUtil
import android.util.Log
import org.apache.commons.lang3.StringUtils
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGridBuilder
import org.projecthaystack.client.HClient

const val CMD = "SCHEDULE_MIGRATED"

fun handleMessage(){
    val ccuHsApi = CCUHsApi.getInstance()
    Log.d(L.TAG_SCHEDULABLE,"handle SCHEDULE_MIGRATED")
    RxjavaUtil.executeBackground {
        val siteRef = CCUHsApi.getInstance().siteIdRef.toString()
        val hClient =
            HClient(ccuHsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS)

        //tunerequipref
        val buildingEquipRef = hayStack.readEntity("equip and tuner")

        val buildingSchedulable = HDictBuilder().add(
            "filter", "schedulable and default and equipRef == "
                    + StringUtils.prependIfMissing(buildingEquipRef.get("id").toString(), "@")
        ).toDict()
        val buildingSchedulableGrid =
            hClient.call("read", HGridBuilder.dictToGrid(buildingSchedulable))
        Log.d(L.TAG_SCHEDULABLE,"buildingSchedulableGrid = "+buildingSchedulableGrid.numRows())
        CCUHsApi.getInstance().updateSchedulable(buildingSchedulableGrid,false)


        //foreach zone
        val zones = hayStack.readAllEntities("room")
        for(zone in zones){
            val schedulableDict = HDictBuilder().add(
                "filter", "schedulable and zone and roomRef == "
                        + StringUtils.prependIfMissing(zone.get("id").toString(), "@")
            ).toDict()
            val schedulableGrid =
                hClient.call("read", HGridBuilder.dictToGrid(schedulableDict))
            Log.d(L.TAG_SCHEDULABLE,"schedulableGrid = "+zone.get("dis").toString()+"==  "+schedulableGrid.numRows())
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


        RxjavaUtil.executeBackgroundTask({ importSchedules() },
            { doPointWriteForSchedulable() },
            { setDiagMigrationVal() })
    }

}