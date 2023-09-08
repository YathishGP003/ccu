package a75f.io.api.haystack.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.RestoreCCUHsApi
import a75f.io.api.haystack.RetryCountCallback
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HRef
import org.projecthaystack.client.HClient

val hayStack = CCUHsApi.getInstance();

fun setDiagMigrationVal() {
    val version =
        hayStack.readDefaultStrVal("point and diag and app and version")
    if(validateMigration())
        hayStack.writeDefaultVal("point and diag and migration", version)

    val pointmig = hayStack.readEntity("point and diag and migration")
    Log.d("CCU_SCHEDULABLE", "Diag Point is =  "+pointmig.get("id"))
    Log.d("CCU_SCHEDULABLE", "Diag Point Set")

}

fun validateMigration(): Boolean {
    val numberOfSchedulableZonePoints = 7
    val numberOfSchedulableBuildingPoints = 7
    val numberOfBuildingLimits = 3
    val schedulabaledata: ArrayList<HashMap<Any, Any>> = hayStack.readAllSchedulable()
    val rooms = hayStack.readAllEntities("room")
    val buildinglimits =
        hayStack.readAllEntities("building and (limit or differential) and not tuner")
    return (((rooms.size * numberOfSchedulableZonePoints) + numberOfSchedulableBuildingPoints + numberOfBuildingLimits)
            == (schedulabaledata.size + buildinglimits.size))
}

fun importSchedules() {
    GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
        Log.d("CCU_SCHEDULABLE", "Update schedule object start")
        val siteUID = hayStack.siteIdRef.toString()
        val hClient =
            HClient(hayStack.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS)
        val restoreCCUHsApi = RestoreCCUHsApi.getInstance()


        val zones = hayStack.readAllEntities("room")
        val zoneRefSet = mutableSetOf<String>()
        for (zone in zones) {
            zoneRefSet.add(zone.get("id").toString())
        }

        Log.d("CCU_SCHEDULABLE", "zoneRefSet = " + zoneRefSet)

        val retryCountCallback =
            RetryCountCallback { retryCount: Int ->
                Log.i(
                    "CCU_SCHEDULABLE",
                    "retrying to get CCU list with the retry count $retryCount"
                )
            }


        restoreCCUHsApi.importBuildingOccupancy(siteUID, hClient,retryCountCallback)
        restoreCCUHsApi.importZoneSchedule(zoneRefSet, retryCountCallback)
        restoreCCUHsApi.importNamedSchedule(retryCountCallback)
        restoreCCUHsApi.importZoneSpecialSchedule(zoneRefSet,retryCountCallback)
        restoreCCUHsApi.importBuildingSpecialSchedule(siteUID,hClient,true,retryCountCallback)


        Log.d("CCU_SCHEDULABLE", "Update schedule object completed")
        setZoneEnabled()
    }

}

val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
    throwable.printStackTrace()
}

fun setZoneEnabled() {
    val zoneSchedules = hayStack.readAllEntities("zone and schedule and not special and not vacation")

    for(zoneSchedule in zoneSchedules){
        Log.d("CCU_SCHEDULABLE", "Enabling =" + zoneSchedule.get("id").toString())
        val s = hayStack.getScheduleById(zoneSchedule.get("id").toString())
        if(CCUHsApi.getInstance().isEntityExisting(s.getRoomRef())) {
            s.setDisabled(false)
            CCUHsApi.getInstance().updateZoneSchedule(s, zoneSchedule.get("roomRef").toString())
            Log.d("CCU_SCHEDULABLE", "Enabled =" + s.toString())
        }

    }
    Log.d("CCU_SCHEDULABLE", "Enable Zone Schedule completed")
}

fun doPointWriteForSchedulable(){
    Log.d("CCU_SCHEDULABLE", "PointWrite Schedulable")
    val schedulabaledata: ArrayList<HashMap<Any, Any>> = hayStack.readAllSchedulable()
    val hDicts = java.util.ArrayList<HDict>()
    for (m in schedulabaledata) {
        val pid = HDictBuilder().add("id", HRef.copy(m["id"].toString())).toDict()
        hDicts.add(pid)
    }
    val buildinglimits =
        hayStack.readAllEntities("building and (limit or differential) and not tuner")
    for (m in buildinglimits) {
        val pid = HDictBuilder().add("id", HRef.copy(m["id"].toString())).toDict()
        hDicts.add(pid)
    }
    hayStack.importPointArrays(hDicts)
}
