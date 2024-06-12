package a75f.io.api.haystack.util

import a75f.io.api.haystack.*
import a75f.io.logger.CcuLog
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HRef
import org.projecthaystack.client.HClient
import java.util.*

val hayStack = CCUHsApi.getInstance();

fun setDiagMigrationVal() {
    val version =
        hayStack.readDefaultStrVal("point and diag and app and version")
    if(validateMigration()){
        hayStack.writeDefaultVal("point and diag and migration", version)
        val pointmig = hayStack.readEntity("point and diag and migration")
       CcuLog.i("CCU_SCHEDULABLE", "Diag Point Id is =  "+pointmig.get("id")+  " Value is set to " + version)
    }else{
       CcuLog.i("CCU_SCHEDULABLE", "Diag Point is not Set")
    }
}

fun validateMigration(): Boolean {
    return true
}
fun deleteDuplication(
    rooms: ArrayList<HashMap<Any, Any>>
) {
    for(room in rooms){
        val zoneScheduleOfTheRoom = hayStack.readAllEntities("zone and schedule and not special and not vacation and roomRef"+" == "+ room["id"].toString())
        if (zoneScheduleOfTheRoom.size > 1) {
            val roomsZoneScheduleId = room["scheduleRef"].toString().replace("@","")
            val scheduleRoomFollowing = hayStack.getScheduleById(roomsZoneScheduleId)
            if ( scheduleRoomFollowing != null
                && scheduleRoomFollowing.isZoneSchedule) {
                for (zoneSchedule in zoneScheduleOfTheRoom) {
                    if (!((zoneSchedule["id"].toString().replace("@","")).equals(roomsZoneScheduleId))) {
                        //delete the duplicate userlimit
                        //copy the limit value.
                        //if dulpicate zone Schedule- clear.
                       CcuLog.i("CCU_SCHEDULABLE", "Delete Schedule"+ zoneSchedule["id"].toString() )
                        hayStack.deleteEntity(zoneSchedule["id"].toString())
                    }
                }
                deleteDuplicateLimits(room["id"].toString())
            } else {
                retainAndRemove(zoneScheduleOfTheRoom)
                deleteDuplicateLimits(room["id"].toString())
            }
        }
    }

   CcuLog.i("SCHEDULE_MIGRATION", "Complete")

}
fun deleteDuplicateLimits(roomRef: String) {
    val queries = listOf(
        "heating and min and user and limit and zone and schedulable and roomRef ==\"$roomRef\"",
        "heating and max and user and limit and zone and schedulable and roomRef ==\"$roomRef\"",
        "cooling and max and user and limit and zone and schedulable and roomRef ==\"$roomRef\"",
        "cooling and min and user and limit and zone and schedulable and roomRef ==\"$roomRef\"",
        "heating and deadband and zone and schedulable and roomRef ==\"$roomRef\"",
        "cooling and deadband and zone and schedulable and roomRef ==\"$roomRef\"",
        "unoccupied and setback and zone and schedulable and roomRef ==\"$roomRef\""
    )

    queries.forEach { query ->
        val entities = hayStack.readAllEntities(query)
        if(entities.size > 1)
            retainAndRemove(entities)
    }
}

fun retainAndRemove(entitiesToRemoveDuplication: ArrayList<HashMap<Any, Any>>) {
    if(entitiesToRemoveDuplication.size > 1){
        val limitToKeep = entitiesToRemoveDuplication.get(0)
        for (limit in entitiesToRemoveDuplication){
            if(!(limit["id"].toString().equals((limitToKeep.get("id").toString())))){
               CcuLog.i("CCU_SCHEDULABLE", "Delete limit"+ limit["id"].toString())
                hayStack.deleteEntity(limit["id"].toString())
            }
        }
    }

    hayStack.syncEntityTree()
}
fun importSchedules() {
    GlobalScope.launch(Dispatchers.IO + coroutineExceptionHandler) {
       CcuLog.i("CCU_SCHEDULABLE", "Update schedule object start")
        val siteUID = hayStack.siteIdRef.toString()
        val hClient =
            HClient(hayStack.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS)
        val restoreCCUHsApi = RestoreCCUHsApi.getInstance()


        val zones = hayStack.readAllEntities("room")
        val zoneRefSet = mutableSetOf<String>()
        for (zone in zones) {
            zoneRefSet.add(zone.get("id").toString())
        }

       CcuLog.i("CCU_SCHEDULABLE", "zoneRefSet = " + zoneRefSet)

        val retryCountCallback =
            RetryCountCallback { retryCount: Int ->
               CcuLog.i(
                    "CCU_SCHEDULABLE",
                    "retrying to get CCU list with the retry count $retryCount"
                )
            }


        restoreCCUHsApi.importBuildingOccupancy(siteUID, hClient,retryCountCallback)
        restoreCCUHsApi.importZoneSchedule(zoneRefSet, retryCountCallback)
        CCUHsApi.getInstance().importNamedSchedule(hClient)
        restoreCCUHsApi.importZoneSpecialSchedule(zoneRefSet,retryCountCallback)
        restoreCCUHsApi.importBuildingSpecialSchedule(siteUID,hClient,true,retryCountCallback)
       // clearScheduleType(zoneRefSet)


       CcuLog.i("CCU_SCHEDULABLE", "Update schedule object completed")
        setZoneEnabled()
        updateZoneScheduleWithinBuildingSchedule(CCUHsApi.getInstance())
    }
}


val coroutineExceptionHandler = CoroutineExceptionHandler{_, throwable ->
    throwable.printStackTrace()
}

fun setZoneEnabled() {
    val zoneSchedules = hayStack.readAllEntities("zone and schedule and not special and not vacation")

    for(zoneSchedule in zoneSchedules){
       CcuLog.i("CCU_SCHEDULABLE", "Enabling =" + zoneSchedule.get("id").toString())
        val s = hayStack.getScheduleById(zoneSchedule.get("id").toString())
        if(CCUHsApi.getInstance().isEntityExisting(s.getRoomRef()) && zoneSchedule["unoccupiedZoneSetback"] != null) {
            s.setDisabled(false)
            CCUHsApi.getInstance().updateZoneSchedule(s, zoneSchedule.get("roomRef").toString())
           CcuLog.i("CCU_SCHEDULABLE", "Enabled =" + s.toString())
        }

    }
   CcuLog.i("CCU_SCHEDULABLE", "Enable Zone Schedule completed")
}

fun doPointWriteForSchedulable(){
   CcuLog.i("CCU_SCHEDULABLE", "PointWrite Schedulable")
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

fun updateZoneScheduleWithinBuildingSchedule(ccuHsApi: CCUHsApi) {
    val buildingOccupancyDict = ccuHsApi.readAsHdict(Queries.BUILDING_OCCUPANCY);
    val buildingOccupancySchedule = Schedule.Builder().setHDict(HDictBuilder().add(buildingOccupancyDict).toDict()).build()
    ccuHsApi.trimZoneSchedules(buildingOccupancySchedule);
}
