package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.diag.DiagEquip.createMigrationVersionPoint
import a75f.io.logic.migration.scheduler.SchedulerRevampMigration
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import java.util.*


class MigrationHandler (hsApi : CCUHsApi) : Migration {
    override val hayStack = hsApi

    private val schedulerRevamp = SchedulerRevampMigration(hayStack)

    override fun isMigrationRequired(): Boolean {
        val appVersion = getAppVersion()
        val migrationVersion = hayStack.readDefaultStrVal("diag and migration and version")
        /**
         * We invoke migration if appVersion and migrationVersion are different.
         * Individual migration handlers should have proper check in place to avoid creating duplicate
         * entities during version downgrades.
         */
        CcuLog.i(L.TAG_CCU_SCHEDULER," isMigrationRequired $appVersion $migrationVersion")
        return appVersion != migrationVersion
    }

    override fun doMigration() {
        if (!isMigrationRequired()) {
            return
        }
        if (schedulerRevamp.isMigrationRequired()) {
            val ccuHsApi = CCUHsApi.getInstance()
            createMigrationVersionPoint(ccuHsApi)
            val remoteScheduleAblePoint = ccuHsApi.fetchRemoteEntityByQuery("schedulable and" +
                    " heating and limit and max and default")
            if(remoteScheduleAblePoint.isEmpty()) {
                syncZoneSchedulesToCloud(ccuHsApi)
            }
            /*Initiate scheduler revamp migration after 10 seconds so that all new zone schedules
            will be synced to backend */
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    schedulerRevamp.doMigration()
                }
            }, 10 * 1000)
        }
    }

    private fun syncZoneSchedulesToCloud(hayStack: CCUHsApi?) {
        val zoneSchedules = hayStack?.readAllEntities("zone and schedule and not special and not vacation")
        if (zoneSchedules != null) {
            for(zoneSchedule in zoneSchedules){
                val s = hayStack.getScheduleById(zoneSchedule["id"].toString())
                if(CCUHsApi.getInstance().isEntityExisting(s.roomRef)) {
                    CCUHsApi.getInstance().updateZoneSchedule(s, zoneSchedule.get("roomRef").toString())
                }
            }
            hayStack.syncEntityTree()
        }
    }



    private fun getAppVersion() : String {
        val pm = Globals.getInstance().applicationContext.packageManager
        val pi: PackageInfo
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0)
            val version = pi.versionName.substring(
                pi.versionName.lastIndexOf('_') + 1,
                pi.versionName.length - 2
            )
            return version
        } catch (e: PackageManager.NameNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return ""
    }
}