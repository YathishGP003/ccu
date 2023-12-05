package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.cutover.VavZoneProfileCutOverMapping
import a75f.io.domain.logic.EquipBuilder
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelSource
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.diag.DiagEquip.createMigrationVersionPoint
import a75f.io.logic.migration.scheduler.SchedulerRevampMigration
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGridBuilder
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter
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
        doVavDomainModelMigration()

        if (!isMigrationRequired()) {
            return
        }
        if (schedulerRevamp.isMigrationRequired()) {
            val ccuHsApi = CCUHsApi.getInstance()
            createMigrationVersionPoint(ccuHsApi)
            val remoteScheduleAblePoint = ccuHsApi.fetchRemoteEntityByQuery("schedulable and" +
                    " heating and limit and max and default")
            if(remoteScheduleAblePoint == null)
                return
            val sGrid = HZincReader(remoteScheduleAblePoint).readGrid()
            val it = sGrid.iterator();
            if(!it.hasNext()) {
                syncZoneSchedulesToCloud(ccuHsApi)
            }
            schedulerRevamp.doMigration()
        }
    }

    private fun syncZoneSchedulesToCloud(hayStack: CCUHsApi?) {
        val zoneSchedules =
            hayStack?.readAllEntities("zone and schedule and not special and not vacation")
        if (zoneSchedules != null) {
            val zoneScheduleDictList = ArrayList<HDict>()
            for (zoneSchedule in zoneSchedules) {
                val entity = CCUHsApi.getInstance().readHDictById(zoneSchedule["id"].toString())
                val builder = HDictBuilder()
                builder.add(entity)
                zoneScheduleDictList.add(builder.toDict())
            }
            val zoneScheduleGridData = HGridBuilder.dictsToGrid(zoneScheduleDictList.toTypedArray())
            val response = HttpUtil.executePost(
                CCUHsApi.getInstance().hsUrl +
                        "addEntity", HZincWriter.gridToString(zoneScheduleGridData)
            )
            Log.i(L.TAG_CCU_SCHEDULER, "All zone schedules are synced to cloud$response")
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

    private fun doVavDomainModelMigration() {
        val vavEquips = hayStack.readAllEntities("equip and zone and vav")
                                .filter { it["domainName"] == null }
                                .toList()
        if (vavEquips.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "VAV DM zone equip migration is complete")
            return
        }
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val site = hayStack.site
        vavEquips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")
            val model = when {
                it.containsKey("series") -> ModelSource.getProfileModelByFileName("nickTestSmartNodeVAVReheatNoFan_v0.0.1")
                                                                as SeventyFiveFProfileDirective
                it.containsKey("parallel") -> ModelSource.getProfileModelByFileName("nickTestSmartNodeVAVReheatNoFan_v0.0.1")
                        as SeventyFiveFProfileDirective
                else -> ModelSource.getProfileModelByFileName("nickTestSmartNodeVAVReheatNoFan_v0.0.1")
                        as SeventyFiveFProfileDirective
            }
            val equipDis = "${site?.displayName}-${it["group"]}-${model.name}"
            equipBuilder.doCutOverMigration(it["id"].toString(), model, equipDis, VavZoneProfileCutOverMapping.entries )
        }
    }

}