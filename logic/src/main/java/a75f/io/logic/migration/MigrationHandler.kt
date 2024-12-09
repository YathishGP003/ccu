package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.CCUTagsDb.TAG_CCU_DOMAIN
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.OAOEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.writeValAtLevelByDomain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.DefaultProfileConfiguration
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.cutover.DabStagedRtuCutOverMapping
import a75f.io.domain.cutover.DabStagedVfdRtuCutOverMapping
import a75f.io.domain.cutover.DabZoneProfileCutOverMapping
import a75f.io.domain.cutover.HyperStatDeviceCutOverMapping
import a75f.io.domain.cutover.HyperStatSplitCpuCutOverMapping
import a75f.io.domain.cutover.HyperStatSplitDeviceCutoverMapping
import a75f.io.domain.cutover.HyperStatV2EquipCutoverMapping
import a75f.io.domain.cutover.NodeDeviceCutOverMapping
import a75f.io.domain.cutover.OaoCutOverMapping
import a75f.io.domain.cutover.OtnEquipCutOverMapping
import a75f.io.domain.cutover.SseZoneProfileCutOverMapping
import a75f.io.domain.cutover.VavFullyModulatingRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedVfdRtuCutOverMapping
import a75f.io.domain.cutover.VavZoneProfileCutOverMapping
import a75f.io.domain.cutover.getDomainNameForMonitoringProfile
import a75f.io.domain.equips.DabEquip
import a75f.io.domain.equips.OtnEquip
import a75f.io.domain.equips.SseEquip
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager.addCmBoardDevice
import a75f.io.domain.logic.DomainManager.addDomainEquips
import a75f.io.domain.logic.DomainManager.addSystemDomainEquip
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.EquipBuilderConfig
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelCache
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfileConfiguration
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.dab.getDevicePointDict
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.MonitoringConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuUniInType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuProfileConfiguration
import a75f.io.logic.bo.building.oao.OAOProfileConfiguration
import a75f.io.logic.bo.building.otn.OtnProfileConfiguration
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.occupancy.DemandResponse
import a75f.io.logic.bo.building.sse.SseProfileConfiguration
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.util.DemandResponseMode
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.diag.DiagEquip.createMigrationVersionPoint
import a75f.io.logic.migration.VavAndAcbProfileMigration.Companion.cleanACBDuplicatePoints
import a75f.io.logic.migration.VavAndAcbProfileMigration.Companion.cleanVAVDuplicatePoints
import a75f.io.logic.migration.ccuanddiagequipmigration.CCUBaseConfigurationMigrationHandler
import a75f.io.logic.migration.ccuanddiagequipmigration.DiagEquipMigrationHandler
import a75f.io.logic.migration.modbus.correctEnumsForCorruptModbusPoints
import a75f.io.logic.migration.scheduler.SchedulerRevampMigration
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.util.PreferenceUtil
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDevicePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.joda.time.DateTime
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRef
import org.projecthaystack.HRow
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter


class MigrationHandler (hsApi : CCUHsApi) : Migration {

    val TAG_CCU_BYPASS_RECOVER = "CCU_BYPASS_RECOVER"


    override val hayStack = hsApi

    private val schedulerRevamp = SchedulerRevampMigration(hayStack)
    private var isMigrationOngoing = false

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
        doVavTerminalDomainModelMigration()
        doDabTerminalDomainModelMigration()
        doVavSystemDomainModelMigration()
        doHyperStatSplitCpuDomainModelMigration()
        CCUBaseConfigurationMigrationHandler().doCCUBaseConfigurationMigration(hayStack)
        DiagEquipMigrationHandler().doDiagEquipMigration(hayStack)
        doDabSystemDomainModelMigration()
        doOAOProfileMigration()
        doSseStandaloneDomainModelMigration()
        createMigrationVersionPoint(CCUHsApi.getInstance())
        addSystemDomainEquip(CCUHsApi.getInstance())
        addCmBoardDevice(hayStack)
        doOtnTerminalDomainModelMigration()
        doHSCPUDMMigration()
        doHSMonitoringDMMigration()
        if (!isMigrationRequired()) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "---- Migration Not Required ----")
            return
        }
        isMigrationOngoing = true
        if (hayStack.readEntity(Tags.SITE).isNotEmpty()) {
            // After DM integration skipping migration for DR mode
//            migrationForDRMode()
            migrateEquipStatusEnums()
            if(!PreferenceUtil.getSingleDualMigrationStatus()) {
                migrationToHandleInfluenceOfUserIntentOnSentPoints()
                PreferenceUtil.setSingleDualMigrationStatus()
            }
        }

        try {
            if(!PreferenceUtil.getMigrateHisInterpolateForDevicePoints()) {
                migrateHisInterpolateForDevicePoints()
                PreferenceUtil.setMigrateHisInterpolateForDevicePoints()
            }
        } catch (e: Exception) {
            //For now, we make sure it does not stop other migrations even if this fails.
            CcuLog.e(L.TAG_CCU_MIGRATION_UTIL, "Error in migrateHisInterpolate $e")
        }

        try {
            VavAndAcbProfileMigration.migrateVavAndAcbProfilesToCorrectPortEnabledStatus(hayStack)
        } catch (e: Exception) {
            //TODO - This is temporary fix till vav model issue is resolved in the next releases.
            //For now, we make sure it does not stop other migrations even if this fails.
            CcuLog.e(L.TAG_CCU_MIGRATION_UTIL, "Error in migrateVavAndAcbProfilesToCorrectPortEnabledStatus: ${e.message}")
        }
        updateAhuRefForTIEquip()
        clearLevel4ValuesOfDesiredTempIfDurationIs0()
        if (schedulerRevamp.isMigrationRequired()) {
            val ccuHsApi = CCUHsApi.getInstance()
            createMigrationVersionPoint(ccuHsApi)
            val remoteScheduleAblePoint = ccuHsApi.fetchRemoteEntityByQuery("schedulable and" +
                    " heating and limit and max and default") ?: return
            val sGrid = HZincReader(remoteScheduleAblePoint).readGrid()
            val it = sGrid.iterator()
            if(!it.hasNext()) {
                syncZoneSchedulesToCloud(ccuHsApi)
            }
            schedulerRevamp.doMigration()
        }
        if(!PreferenceUtil.isModbusEnumCorrectionRequired()) {
            correctEnumsForCorruptModbusPoints(hayStack)
            PreferenceUtil.setModbusEnumCorrectionDone()
        }
        if(!PreferenceUtil.isBackFillValueUpdateRequired()) {
            val backFillDurationDomainPoint  = hayStack.readEntity("domainName == \"backfillDuration\"")
            try {
                if(backFillDurationDomainPoint.isNotEmpty()) {
                    updatingBackFillDefaultValues(
                        hayStack,
                        backFillDurationDomainPoint
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //For now, we make sure it does not stop other migrations even if this fails.
                CcuLog.e(L.TAG_CCU_MIGRATION_UTIL, "Error in updatingBackfillDefaultValues $e")
                if(backFillDurationDomainPoint.isNotEmpty()) {
                    hayStack.writeDefaultValById(backFillDurationDomainPoint["id"].toString(), 24.0)
                    CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Updated backfill default value")
                }
            }

            PreferenceUtil.setBackFillValueUpdateDone()
        }
        if(!PreferenceUtil.isHisTagRemovalFromNonDmDevicesDone()) {
            removeHisTagsFromNonDMDevices()
            PreferenceUtil.setHisTagRemovalFromNonDmDevicesDone()
        }

        if(!PreferenceUtil.isDeadBandMigrationRequired()){
            migrateDeadBandPoints(hayStack)
            PreferenceUtil.setDeadBandMigrationNotRequired()
        }
        try {
            if (!PreferenceUtil.getDeleteRedundantSetbackPointsFromHnAcbEquips()) {
                VavAndAcbProfileMigration.recoverHelioNodeACBTuners(hayStack)
                deleteRedundantSetbackPointsFromHnAcbEquips()
                PreferenceUtil.setDeleteRedundantSetbackPointsFromHnAcbEquips()
            }
        } catch (e: Exception) {
            //For now, we make sure it does not stop other migrations even if this fails.
            CcuLog.e(L.TAG_CCU_MIGRATION_UTIL, "Error in deleteRedundantSetbackPointsFromHnAcbEquips $e")
        }
        if(!PreferenceUtil.isVavCfmOnEdgeMigrationDone()) {
            VavAndAcbProfileMigration.addMinHeatingDamperPositionMigration(hayStack)
            PreferenceUtil.setVavCfmOnEdgeMigrationDone()
        }
        clearOtaCachePreferences() // While migrating to new version, we need to clear the ota cache preferences
        if(!PreferenceUtil.getMigrateAnalogInputTypeForVavDevicePoint()) {
           try {
                migrateAnalogTypeForVavAnalog1In()
                PreferenceUtil.setMigrateAnalogInputTypeForVavDevicePoint()
              } catch (e: Exception) {
                //For now, we make sure it does not stop other migrations even if this fails.
                CcuLog.e(L.TAG_CCU_MIGRATION_UTIL, "Error in migrateAnalogTypeForVAVanalog1In $e")
           }
        }
        if(!PreferenceUtil.getBacnetSettingPointDeleted()) {
            removeRedundantBacnetSettingPoints()
            PreferenceUtil.setBacnetSettingPointDeleted()
        }
        hayStack.scheduleSync()
    }

    fun doDabDamperSizeMigration() {
        val damperSizeMap = mapOf(
            0.0 to 4.0,
            1.0 to 6.0,
            2.0 to 8.0,
            3.0 to 10.0,
            4.0 to 12.0,
            5.0 to 14.0,
            6.0 to 16.0,
            7.0 to 18.0,
            8.0 to 20.0,
            9.0 to 22.0
        )
        val dabEquips = hayStack.readAllEntities("equip and zone and dab and not dualDuct")
            .filter { it["domainName"] != null }
            .toList()

        dabEquips.forEach {
            val dabEquip = DabEquip(it["id"].toString())

            val damper1Size = dabEquip.damper1Size.readPriorityVal()
            dabEquip.damper1Size.writeDefaultVal(damperSizeMap[damper1Size] ?: 4.0)
            CcuLog.d(L.TAG_CCU_DOMAIN, "Damper1 Size: ${dabEquip.damper1Size.readPriorityVal()}")
            val damper2Size = dabEquip.damper2Size.readPriorityVal()
            dabEquip.damper2Size.writeDefaultVal(damperSizeMap[damper2Size] ?: 4.0)
            CcuLog.d(L.TAG_CCU_DOMAIN, "Damper2 Size: ${dabEquip.damper2Size.readPriorityVal()}")
        }
    }


    private fun clearOtaCachePreferences() {
        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL,"Clearing OtaCache Preferences")
        try {
            val sharedPreferences: SharedPreferences = Globals.getInstance().applicationContext.getSharedPreferences("otaCache" , Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
        }
        catch (e : Exception) {
            CcuLog.e(L.TAG_CCU_MIGRATION_UTIL,"Failed to clear OtaCache ${e.printStackTrace()}")
        }
    }

    private fun updatingBackFillDefaultValues(hayStack: CCUHsApi, backFillPoint: HashMap<Any, Any>) {

        val backFillDurationId = backFillPoint["id"].toString()
        val lastUpdatedTimeForBackFillPoint = hayStack.readPointPriorityLatestTime(backFillDurationId)

        if(lastUpdatedTimeForBackFillPoint == null) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "lastUpdatedTimeForBackFillPoint not found" +
                    " so Updated the fallback default value for backfill point")
            hayStack.writeDefaultValById(backFillDurationId,24.0)
            return
        }

        val lastUpdatedTime = DateTime.parse(lastUpdatedTimeForBackFillPoint.substring(0, 19))
        if(backFillPoint["createdDateTime"] == null) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Updated the fallback default value for backfill point")
            hayStack.writeDefaultValById(backFillDurationId,24.0)
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Updated backfill default value")
            return
        }
        val createdTime =
            DateTime.parse(backFillPoint["createdDateTime"].toString().substring(0,19))
        if (createdTime.equals(lastUpdatedTime)) {
            hayStack.writeDefaultValById(backFillDurationId,24.0)
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Updated backfill default value")
        } else {
            CcuLog.i(
                L.TAG_CCU_MIGRATION_UTIL,
                "backfill default value is already updated by user so no need to update"
            )
        }
    }

    fun doPostModelMigrationTasks() {
        if (!PreferenceUtil.getRecoverHelioNodeACBTunersMigration()) VavAndAcbProfileMigration.recoverHelioNodeACBTuners(hayStack)
        if (!PreferenceUtil.getACBRelayLogicalPointsMigration()) VavAndAcbProfileMigration.verifyACBIsoValveLogicalPoints(hayStack)
        try{
            if (!PreferenceUtil.getDmToDmCleanupMigration()) {
                cleanACBDuplicatePoints(hayStack)
                cleanVAVDuplicatePoints(hayStack)
                hayStack.syncEntityTree()
                PreferenceUtil.setDmToDmCleanupMigration()
            }
        } catch (e: Exception) {
            //TODO - This is temporary fix till vav model issue is resolved in the next releases.
            //For now, we make sure it does not stop other migrations even if this fails.
        }
        if(!PreferenceUtil.getRestoreBypassDamperAfterReplace()) {
            try {
                if(restoreMissingBypassDamperAfterReplace()) {
                    PreferenceUtil.setRestoreBypassDamperAfterReplace()
                } else {
                    CcuLog.e(TAG_CCU_BYPASS_RECOVER, "Failed to restore missing bypass damper after replace. Operation failed and not setting the preference")
                }
            } catch (e: Exception) {
                CcuLog.d(TAG_CCU_BYPASS_RECOVER, "Some exception occurred. CCU Bypass Recovery operation stopped abruptly.")
            }
        }
        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL,"doPostModelMigrationTasks: check isMigrationOngoing $isMigrationOngoing")
        if(isMigrationOngoing) {
            CcuLog.d(L.TAG_CCU_MIGRATION_UTIL,"Version update detected, performing minCFMPoints' maxVal update")
            updateMinCfmPointMaxVal(Pair(DomainName.minCFMCooling, DomainName.maxCFMCooling))
            updateMinCfmPointMaxVal(Pair(DomainName.minCFMReheating, DomainName.maxCFMReheating))
        }
        isMigrationOngoing = false
    }

    private fun deleteRedundantSetbackPointsFromHnAcbEquips() {
        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips started")
        hayStack.readAllEntities("equip and domainName == \"helionodeActiveChilledBeam\"").forEach { hnAcbEquip ->
                val nonDmDemandResponse = hayStack.readEntity("demand and response and setback and tuner and not domainName and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                if (nonDmDemandResponse.isNotEmpty()) {
                    CcuLog.d(L.TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: ${nonDmDemandResponse["id"].toString()}")
                    val dmDemandResponseSetback = hayStack.readHDict("domainName==\"demandResponseSetback\" and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                    if (dmDemandResponseSetback != null && !dmDemandResponseSetback.isEmpty) {
                        hayStack.updatePoint(Point.Builder().setHDict(dmDemandResponseSetback).build() , nonDmDemandResponse["id"].toString())
                        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: demandResponse ${nonDmDemandResponse["id"].toString()} updated")
                        hayStack.deleteWritablePoint(dmDemandResponseSetback["id"].toString()) // delete the duplicate point
                    }
                } else {
                    CcuLog.d(L.TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: no demandResponse found")
                }
                val nonDmAutoAway = hayStack.readEntity("auto and away and setback and not domainName and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                if (nonDmAutoAway.isNotEmpty()) {
                    val dmAutoAway = hayStack.readHDict("domainName==\"autoAway\" and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                    if (dmAutoAway != null && !dmAutoAway.isEmpty) {
                        hayStack.updatePoint(Point.Builder().setHDict(dmAutoAway).build() , nonDmAutoAway["id"].toString())
                        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: autoAway ${nonDmAutoAway["id"].toString()} updated")
                        hayStack.deleteWritablePoint(dmAutoAway["id"].toString())
                    }
                } else {
                    CcuLog.d("MigrationHandler" , "deleteRedundantSetbackPointsFromHnAcbEquips: no autoAway found")
                }
            }
    }

    private fun migrateDeadBandPoints(hayStack: CCUHsApi) {
        val listOfZones = hayStack.readAllEntities("room")
        val minDeadBandVal = "0.5"
        listOfZones.forEach { zoneMap ->
            val zone = Zone.Builder().setHashMap(zoneMap).build()
            val zoneId = zone.id
            val heatingDeadBandPoint = hayStack.readEntity("schedulable and heating and deadband and roomRef == \"$zoneId\"")
            val coolingDeadBandPoint = hayStack.readEntity("schedulable and cooling and deadband and roomRef == \"$zoneId\"")
            if (heatingDeadBandPoint.isNotEmpty()) {
                val deadBand = Point.Builder().setHashMap(heatingDeadBandPoint).setMinVal(minDeadBandVal).build()
                hayStack.updatePoint(deadBand, deadBand.id)
            }
            if (coolingDeadBandPoint.isNotEmpty()) {
                val deadBand = Point.Builder().setHashMap(coolingDeadBandPoint).setMinVal(minDeadBandVal).build()
                hayStack.updatePoint(deadBand, deadBand.id)
            }
        }
    }

    private fun removeHisTagsFromNonDMDevices() {
        hayStack.readAllEntities("device and his").forEach { nonDMDeviceMap ->
            nonDMDeviceMap.remove("his")
            nonDMDeviceMap["id"]?.let {
                val nonDMDevice = Device.Builder().setHDict(hayStack.readHDictById(it.toString())).removeMarker("his").build()
                hayStack.updateDevice(nonDMDevice, nonDMDevice.id)
            }
        }
    }

    private fun updateAhuRefForTIEquip() {
        val tiEquipMap = hayStack.readEntity("equip and ti")
        if(tiEquipMap.isNotEmpty()) {
            val equip = Equip.Builder().setHashMap(tiEquipMap).build()
            equip.ahuRef = hayStack.readId("equip and system and not modbus")
            hayStack.updateEquip(equip, equip.id)
        }
    }

    private fun clearLevel4ValuesOfDesiredTempIfDurationIs0() {
        val listOfDesiredTempPoints: List<HashMap<Any, Any>> = hayStack.readAllEntities("desired and temp and (heating or cooling)")
        listOfDesiredTempPoints.forEach { desiredTempPoint ->
            val desiredTempPointId : String = desiredTempPoint["id"].toString()
            val priorityGrid : HGrid? = hayStack.readPointArrRemote(desiredTempPointId)
            var isCleared = false
            priorityGrid?.let { grid ->
                val iterator: MutableIterator<HRow?>? = grid.iterator() as MutableIterator<HRow?>?
                while (iterator!=null && iterator.hasNext()) {
                    val r: HRow? = iterator.next()
                    if ((isLevelCleanable(r) && isLevelToBeCleared(r)) || isAutoAwayMappedToDemandResponseLevel(r)) {
                        hayStack.clearPointArrayLevel(desiredTempPointId, r!!.getInt("level"), false)
                        isCleared = true
                    }
                }
            }
            if(isCleared) { hayStack.writeHisValById(desiredTempPointId, hayStack.readPointPriorityVal(desiredTempPointId)) }
        }
    }

    private fun isAutoAwayMappedToDemandResponseLevel(levelRow: HRow?): Boolean {
        return levelRow!!.getInt("level") == HayStackConstants.DEMAND_RESPONSE_LEVEL &&
                ! DemandResponse.isDRModeActivated()
    }

    private fun isLevelToBeCleared(levelRow: HRow?): Boolean {
        val oneDayInMs = 86400000L
        val duration = levelRow?.getDouble("duration")
        return duration != null && (duration <= 0.0 || duration - System.currentTimeMillis() > oneDayInMs)
    }

    private fun isLevelCleanable(levelRow: HRow?): Boolean {
        return levelRow!!.getInt("level").let { level ->
            level == HayStackConstants.AUTO_AWAY_LEVEL ||
            level == HayStackConstants.FORCE_OVERRIDE_LEVEL ||
            level == HayStackConstants.OCCUPANT_USER_WRITE_LEVEL ||
            level == HayStackConstants.USER_APP_WRITE_LEVEL }
    }

    private fun migrationToHandleInfluenceOfUserIntentOnSentPoints() {
        val standaloneEquips = hayStack.readAllEntities("equip and (hyperstat or smartstat or hyperstatsplit)")
        val roomRefs = getRoomRefsForAllStandaloneProfiles(standaloneEquips)
        roomRefs.forEach{roomRef ->
            DesiredTempDisplayMode.setModeTypeOnUserIntentChange(roomRef, hayStack)
        }
    }

    private fun getRoomRefsForAllStandaloneProfiles(standaloneEquips: ArrayList<HashMap<Any, Any>>):
            List<String> {
        val roomRefs: MutableList<String> = mutableListOf()
        standaloneEquips.forEach {standaloneEquip ->
            roomRefs.add(standaloneEquip["roomRef"].toString())
        }
        return roomRefs.distinct()
    }


    private fun migrateEquipStatusEnums() {
        val equipStatusPointList = hayStack.readAllEntities("status and not ota and not message" +
                " and zone and his and enum and not modbus")
        equipStatusPointList.forEach{equipStatusMap ->
            val equipStatusPoint = Point.Builder().setHashMap(equipStatusMap).build()
            if (!equipStatusPoint.enums.toString().contains("rfdead")) {
                equipStatusPoint.enums = "deadband,cooling,heating,tempdead,rfdead"
                hayStack.updatePoint(equipStatusPoint, equipStatusPoint.id)
            }
        }
    }

    private  fun migrateDemandResponseForOccupancyEnum(ccuHsApi: CCUHsApi) {
        val occModePoints = ccuHsApi.readAllEntities("occupancy and mode and enum and not modbus")
        occModePoints.forEach { occMode ->
            val occModePoint = Point.Builder().setHashMap(occMode).build()
            if (!occModePoint.enums.toString().contains("demandresponseoccupied")) {
                occModePoint.enums = Occupancy.getEnumStringDefinition()
                hayStack.updatePoint(occModePoint, occModePoint.id)
            }
        }

        val occStatePoints = ccuHsApi.readAllEntities("occupancy and state and enum and not modbus")
        occStatePoints.forEach { occState ->
            val occStatePoint = Point.Builder().setHashMap(occState).build()
            if (!occStatePoint.enums.toString().contains("demandresponseoccupied")) {
                occStatePoint.enums = Occupancy.getEnumStringDefinition()
                hayStack.updatePoint(occStatePoint, occStatePoint.id)
            }
        }
    }

    private fun migrateDemandResponseSetbackTunerForAllTempZones(ccuHsApi: CCUHsApi) {
        val demandResponseSetBackTuner = ccuHsApi.readEntity(
            "demand and" +
                    " response and system and tuner"
        )
        if (demandResponseSetBackTuner.isEmpty()) {
            val systemEquip = ccuHsApi.readEntity("equip and system and not modbus")
            val equipRef = systemEquip["id"].toString()
            val equipDis = systemEquip["dis"].toString()
            CcuLog.i(L.TAG_CCU_DR_MODE, "System level tuner is created for: $equipDis")
            DemandResponseMode.createDemandResponseSetBackTuner(
                ccuHsApi,
                equipRef, equipDis, true, null, null
            )
        }
        val equipsList: MutableList<ArrayList<HashMap<Any, Any>>> = ArrayList()
        equipsList.add(ccuHsApi.readAllEntities("equip and vav and not system and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and dab and not system and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and dualDuct and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and standalone and smartstat and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and standalone and hyperstat and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and standalone and hyperstatsplit and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and sse and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and sse and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and ti and not domainName"))
        equipsList.add(ccuHsApi.readAllEntities("equip and otn and not domainName"))

        for (equips in equipsList) {
            for (equipMap in equips) {
                val demandResponseSetBackTunerPoint =
                    ccuHsApi.readEntity("demand and response and setback and equipRef == \"" + equipMap["id"].toString() + "\"")
                if (demandResponseSetBackTunerPoint.isEmpty()) {
                    DemandResponseMode.createDemandResponseSetBackTuner(
                        ccuHsApi,
                        equipMap["id"].toString(), equipMap["dis"].toString(), false,
                        equipMap["roomRef"].toString(), equipMap["floorRef"].toString()
                    )
                    CcuLog.i(
                        L.TAG_CCU_DR_MODE,
                        "Equip level tuner is created for: " + equipMap["dis"].toString()
                    )
                }
            }
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
            CcuLog.i(L.TAG_CCU_SCHEDULER, "All zone schedules are synced to cloud$response")
        }
    }



    private fun getAppVersion() : String {
        val pm = Globals.getInstance().applicationContext.packageManager
        val pi: PackageInfo
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0)
            return pi.versionName.substring(
                    pi.versionName.lastIndexOf('_') + 1,
                    pi.versionName.length
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return ""
    }

    private fun doVavTerminalDomainModelMigration() {
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
            val reheatType = hayStack.readEntity("config and reheat and type and equipRef == \"${it["id"]}\"")
            if (reheatType.isNotEmpty()) {
                val reheatTypeVal = hayStack.readDefaultValById(reheatType["id"].toString())
                CcuLog.i(Domain.LOG_TAG, "Update reheatType for $it - current $reheatTypeVal")
                hayStack.writeDefaultValById(reheatType["id"].toString(), reheatTypeVal + 1)
            }
            val model = when {
                it.containsKey("series") && it.containsKey("smartnode") -> ModelLoader.getSmartNodeVavSeriesModelDef()
                it.containsKey("parallel") && it.containsKey("smartnode") -> ModelLoader.getSmartNodeVavParallelFanModelDef()
                it.containsKey("series") && it.containsKey("helionode") -> ModelLoader.getHelioNodeVavSeriesModelDef()
                it.containsKey("parallel") && it.containsKey("helionode") -> ModelLoader.getHelioNodeVavParallelFanModelDef()
                it.containsKey("helionode") -> ModelLoader.getHelioNodeVavNoFanModelDef()
                else -> ModelLoader.getSmartNodeVavNoFanModelDef()
            }
            val equipDis = "${site?.displayName}-VAV-${it["group"]}"

            val isHelioNode = it.containsKey("helionode")
            val deviceModel = if (isHelioNode) ModelLoader.getHelioNodeDevice() as SeventyFiveFDeviceDirective else ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective
            val deviceDis = if (isHelioNode) "${site?.displayName}-HN-${it["group"]}" else "${site?.displayName}-SN-${it["group"]}"
            val deviceBuilder = DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
            val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
            val profileType = when {
                it.containsKey("series") -> ProfileType.VAV_SERIES_FAN
                it.containsKey("parallel") -> ProfileType.VAV_PARALLEL_FAN
                else -> ProfileType.VAV_REHEAT
            }

            val profileConfiguration = VavProfileConfiguration(
                Integer.parseInt(it["group"].toString()),
                if (isHelioNode) NodeType.HELIO_NODE.name else NodeType.SMART_NODE.name,
                0,
                it["roomRef"].toString(),
                it["floorRef"].toString(),
                profileType,
                model
            ).getActiveConfiguration()

            equipBuilder.doCutOverMigration(it["id"].toString(), model,
                                    equipDis, VavZoneProfileCutOverMapping.entries, profileConfiguration, equipHashMap = it)

            val vavEquip = VavEquip(it["id"].toString())

            // damperSize point changed from a literal to an enum
            val newDamperSize = getDamperSizeEnum(vavEquip.damperSize.readDefaultVal())
            vavEquip.damperSize.writeDefaultVal(newDamperSize)

            // temperature offset is now a literal (was multiplied by 10 before)
            val newTempOffset = String.format("%.1f", vavEquip.temperatureOffset.readDefaultVal() * 0.1).toDouble()
            vavEquip.temperatureOffset.writeDefaultVal(newTempOffset)

            // At app startup, cutover migrations currently run before upgrades.
            // This is a problem because demandResponseSetback is supposed to get its value from a newly-added BuildingTuner point, which isn't available yet.
            // Setting the fallback value manually for now.
            vavEquip.demandResponseSetback.writeVal(17, 2.0)

            deviceBuilder.doCutOverMigration(
                device["id"].toString(),
                deviceModel,
                deviceDis,
                NodeDeviceCutOverMapping.entries,
                profileConfiguration
            )
        }
    }

    private fun doDabTerminalDomainModelMigration() {
        val dabEquips = hayStack.readAllEntities("equip and zone and dab and not dualDuct")
            .filter { it["domainName"] == null }
            .toList()
        if (dabEquips.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "DAB DM zone equip migration is complete")
            return
        }
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val site = hayStack.site
        dabEquips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")
            val reheatType = hayStack.readEntity("config and reheat and type and equipRef == \"${it["id"]}\"")
            if (reheatType.isNotEmpty()) {
                val reheatTypeVal = hayStack.readDefaultValById(reheatType["id"].toString())
                CcuLog.i(Domain.LOG_TAG, "Update reheatType for $it - current $reheatTypeVal")
                hayStack.writeDefaultValById(reheatType["id"].toString(), reheatTypeVal )
            }
            val model = when {
                it.containsKey("dab") && it.containsKey("smartnode") -> ModelLoader.getSmartNodeDabModel()
                else -> ModelLoader.getHelioNodeDabModel()
            }
            val equipDis = "${site?.displayName}-DAB-${it["group"]}"

            val isHelioNode = it.containsKey("helionode")
            val deviceModel = if (isHelioNode) ModelLoader.getHelioNodeDevice() as SeventyFiveFDeviceDirective else ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective
            val deviceDis = if (isHelioNode) "${site?.displayName}-HN-${it["group"]}" else "${site?.displayName}-SN-${it["group"]}"
            val deviceBuilder = DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
            val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
            val profileType = ProfileType.DAB

            val profileConfiguration = DabProfileConfiguration(
                Integer.parseInt(it["group"].toString()),
                if (isHelioNode) NodeType.HELIO_NODE.name else NodeType.SMART_NODE.name,
                0,
                it["roomRef"].toString(),
                it["floorRef"].toString(),
                profileType,
                model
            ).getActiveConfiguration()

            equipBuilder.doCutOverMigration(it["id"].toString(), model,
                equipDis, DabZoneProfileCutOverMapping.entries, profileConfiguration, equipHashMap = it)

            val dabEquip = DabEquip(it["id"].toString())

            // damperSize point changed from a literal to an enum
            val newDamper1Size = getDamperSizeEnum(dabEquip.damper1Size.readDefaultVal())
            dabEquip.damper1Size.writeDefaultVal(newDamper1Size)

            // damperSize point changed from a literal to an enum
            val newDamper2Size = getDamperSizeEnum(dabEquip.damper2Size.readDefaultVal())
            dabEquip.damper2Size.writeDefaultVal(newDamper2Size)

            // temperature offset is now a literal (was multiplied by 10 before)
            val newTempOffset = String.format("%.1f", dabEquip.temperatureOffset.readDefaultVal() * 0.1).toDouble()
            dabEquip.temperatureOffset.writeDefaultVal(newTempOffset)

            // At app startup, cutOver migrations currently run before upgrades.
            // This is a problem because demandResponseSetback is supposed to get its value from a newly-added BuildingTuner point, which isn't available yet.
            // Setting the fallback value manually for now.
            dabEquip.demandResponseSetback.writeVal(17, 2.0)

            deviceBuilder.doCutOverMigration(
                device["id"].toString(),
                deviceModel,
                deviceDis,
                NodeDeviceCutOverMapping.entries,
                profileConfiguration
            )
        }
    }

    private fun getDamperSizeEnum(size: Double) : Double {
        return when (size) {
            6.0 -> 1.0
            8.0 -> 2.0
            10.0 -> 3.0
            12.0 -> 4.0
            14.0 -> 5.0
            16.0 -> 6.0
            18.0 -> 7.0
            20.0 -> 8.0
            22.0 -> 9.0
            24.0 -> 10.0
            else -> 0.0
        }
    }

    private fun doDabSystemDomainModelMigration() {
        val dabEquips = hayStack.readAllEntities("equip and system and dab and not modbus")
            .filter { it["domainName"] == null }
            .toList()

        val site = hayStack.site
        if (dabEquips.isEmpty() || site == null) {
            CcuLog.i(Domain.LOG_TAG, "DAB DM system equip migration not required : site $site")
            return
        }
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val deviceModel = ModelLoader.getCMDeviceModel() as SeventyFiveFDeviceDirective
        val deviceDis = hayStack.siteName +"-"+ deviceModel.name
        dabEquips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Do DM system equip migration for $it")
            when {
                (it["profile"].toString() == "SYSTEM_DAB_STAGED_RTU" ||
                        it["domainName"].toString() == "dabStagedRtu") -> {
                    migrateDabStagedSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis,equipHashMap = it)
                }
                (it["profile"].toString() == "SYSTEM_DAB_STAGED_VFD_RTU" ||
                        it["domainName"].toString() == "dabStagedRtuVfdFan") -> {
                    migrateDabStagedVfdSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis, equipHashMap = it)
                }
                else -> {}
            }

        }
    }

    private fun migrateDabStagedSystemProfile (equipId : String, equipBuilder: ProfileEquipBuilder, site: Site,
                                               deviceModel : SeventyFiveFDeviceDirective, deviceDis : String, equipHashMap: HashMap<Any, Any>) {

        val model = ModelLoader.getDabStageRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = StagedRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, DabStagedRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration(), isSystem = true,equipHashMap = equipHashMap)

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfig.getActiveConfiguration(),
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
    }

    private fun doVavSystemDomainModelMigration() {
        val vavEquips = hayStack.readAllEntities("equip and system and vav and not modbus")
            .filter { it["domainName"] == null }
            .toList()

        val site = hayStack.site
        if (vavEquips.isEmpty() || site == null) {
            CcuLog.i(Domain.LOG_TAG, "VAV DM system equip migration not required : site $site")
            return
        }
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val deviceModel = ModelLoader.getCMDeviceModel() as SeventyFiveFDeviceDirective
        val deviceDis = hayStack.siteName +"-"+ deviceModel.name
        vavEquips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Do DM system equip migration for $it")
            when {
                (it["profile"].toString() == "SYSTEM_VAV_STAGED_RTU" ||
                    it["domainName"].toString() == "vavStagedRtu") -> {
                    migrateVavStagedSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis,equipHashMap = it)
                }
                (it["profile"].toString() == "SYSTEM_VAV_STAGED_VFD_RTU" ||
                        it["domainName"].toString() == "vavStagedRtuVfdFan") -> {
                   migrateVavStagedVfdSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis,equipHashMap = it)
                }
                (it["profile"].toString() == "SYSTEM_VAV_ANALOG_RTU" ||
                        it["domainName"].toString() == "vavFullyModulatingAhu") -> {
                    migrateVavFullyModulatingSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis,equipHashMap = it)
                }
                else -> {}
            }

        }
    }

    private fun migrateVavStagedSystemProfile (equipId : String, equipBuilder: ProfileEquipBuilder, site: Site,
                                               deviceModel : SeventyFiveFDeviceDirective, deviceDis : String, equipHashMap: HashMap<Any, Any>) {

        val model = ModelLoader.getVavStageRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = StagedRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, VavStagedRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration(), isSystem = true,equipHashMap = equipHashMap)

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfig.getActiveConfiguration(),
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
    }

    private fun migrateVavStagedVfdSystemProfile (equipId : String, equipBuilder: ProfileEquipBuilder, site: Site,
                                               deviceModel : SeventyFiveFDeviceDirective, deviceDis : String, equipHashMap: HashMap<Any, Any>) {

        val model = ModelLoader.getVavStagedVfdRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = StagedVfdRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, VavStagedVfdRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration()
            ,isSystem = true,equipHashMap = equipHashMap)

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfig.getActiveConfiguration(),
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
    }

    private fun migrateDabStagedVfdSystemProfile (equipId : String, equipBuilder: ProfileEquipBuilder, site: Site,
                                                  deviceModel : SeventyFiveFDeviceDirective, deviceDis : String,equipHashMap: HashMap<Any, Any>) {

        val model = ModelLoader.getDabStagedVfdRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = StagedVfdRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, DabStagedVfdRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration()
            ,isSystem = true,equipHashMap)

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfig.getActiveConfiguration(),
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
    }

    private fun migrateVavFullyModulatingSystemProfile (equipId : String, equipBuilder: ProfileEquipBuilder, site: Site,
                                                  deviceModel : SeventyFiveFDeviceDirective, deviceDis : String, equipHashMap: HashMap<Any, Any>) {
        CcuLog.i(Domain.LOG_TAG, "VavFullyModulatingSystemProfile equipID: $equipId")
        val model = ModelLoader.getVavModulatingRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = ModulatingRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, VavFullyModulatingRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration()
            ,isSystem = true,equipHashMap = equipHashMap)

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(cmDevice["id"].toString())
        }

        CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
        deviceBuilder.buildDeviceAndPoints(
            profileConfig.getActiveConfiguration(),
            deviceModel,
            equipId,
            hayStack.site!!.id,
            deviceDis
        )
    }

    private fun doHyperStatSplitCpuDomainModelMigration() {
        val hssEquips = hayStack.readAllEntities("equip and hyperstatsplit and cpu")
            .filter { it["domainName"] == null }
            .toList()
        if (hssEquips.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "HyperStat Split CPU DM equip migration is complete")
            return
        }
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val site = hayStack.site
        hssEquips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")

            val model = ModelLoader.getHyperStatSplitCpuModel()
            val equipDis = "${site?.displayName}-cpuecon-${it["group"]}"
            val deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective
            val deviceDis = "${site?.displayName}-HSS-${it["group"]}"
            val deviceBuilder = DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
            val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")

            val profileConfiguration = HyperStatSplitCpuProfileConfiguration(
                Integer.parseInt(it["group"].toString()),
                NodeType.HYPERSTATSPLIT.name,
                0,
                it["roomRef"].toString(),
                it["floorRef"].toString(),
                ProfileType.HYPERSTATSPLIT_CPU,
                model
            ).getActiveConfiguration()

            equipBuilder.doCutOverMigration(it["id"].toString(), model,
                equipDis, HyperStatSplitCpuCutOverMapping.entries, profileConfiguration, equipHashMap = it)

            val hssEquip = HyperStatSplitEquip(it["id"].toString())

            hssEquip.temperatureOffset.writeDefaultVal(0.1 * hssEquip.temperatureOffset.readDefaultVal())

            if (hssEquip.oaoDamper.pointExists()) {
                hssEquip.enableOutsideAirOptimization.writeDefaultVal(1.0)
            }

            if (hssEquip.universalIn1Association.pointExists()) {
                hssEquip.universalIn1Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn1Association.readDefaultVal().toInt()))
            }

            if (hssEquip.universalIn2Association.pointExists()) {
                hssEquip.universalIn2Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn2Association.readDefaultVal().toInt()))
            }

            if (hssEquip.universalIn3Association.pointExists()) {
                hssEquip.universalIn3Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn3Association.readDefaultVal().toInt()))
            }

            if (hssEquip.universalIn4Association.pointExists()) {
                hssEquip.universalIn4Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn4Association.readDefaultVal().toInt()))
            }

            if (hssEquip.universalIn5Association.pointExists()) {
                hssEquip.universalIn5Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn5Association.readDefaultVal().toInt()))
            }

            if (hssEquip.universalIn6Association.pointExists()) {
                hssEquip.universalIn6Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn6Association.readDefaultVal().toInt()))
            }

            if (hssEquip.universalIn7Association.pointExists()) {
                hssEquip.universalIn7Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn7Association.readDefaultVal().toInt()))
            }

            if (hssEquip.universalIn8Association.pointExists()) {
                hssEquip.universalIn8Association.writeDefaultVal(migrateUniversalInValue(hssEquip.universalIn8Association.readDefaultVal().toInt()))
            }

            if (hssEquip.standalonePrePurgeOccupiedTimeOffsetTuner.pointExists()) {
                val prePurgeOffsetMap = hayStack.readMapById(hssEquip.standalonePrePurgeOccupiedTimeOffsetTuner.id)
                val prePurgeOffsetPoint = Point.Builder().setHashMap(prePurgeOffsetMap).addMarker("tuner").build()
                hayStack.updatePoint(prePurgeOffsetPoint, prePurgeOffsetPoint.id)
            }

            if (hssEquip.standalonePrePurgeFanSpeedTuner.pointExists()) {
                val prePurgeFanSpeedMap = hayStack.readMapById(hssEquip.standalonePrePurgeFanSpeedTuner.id)
                val prePurgeFanSpeedPoint = Point.Builder().setHashMap(prePurgeFanSpeedMap).addMarker("tuner").build()
                hayStack.updatePoint(prePurgeFanSpeedPoint, prePurgeFanSpeedPoint.id)
            }

            if (hssEquip.prePurgeEnable.readPriorityVal() > 0.0 && !hssEquip.prePurgeStatus.pointExists()) {
                val prePurgeStatus = model.points.find { it.domainName == DomainName.prePurgeStatus }
                prePurgeStatus.run {
                    equipBuilder.createPoint(
                        PointBuilderConfig(
                            this as ModelPointDef,
                            profileConfiguration,
                            hssEquip.getId(),
                            site?.id ?: "",
                            hayStack.timeZone,
                            equipDis
                        )
                    )
                }
            }

            if (hssEquip.temperatureSensorBusAdd0.pointExists()) {
                val humiditySensorBusAdd0Def = model.points.find { it.domainName == DomainName.humiditySensorBusAdd0 }
                humiditySensorBusAdd0Def.run {
                    val humiditySensorBusAdd0Id = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this as ModelPointDef,
                            profileConfiguration,
                            hssEquip.getId(),
                            site?.id ?: "",
                            hayStack.timeZone,
                            equipDis
                        )
                    )
                }
                hssEquip.humiditySensorBusAdd0.writeDefaultVal(hssEquip.temperatureSensorBusAdd0.readDefaultVal())
            }

            if (hssEquip.temperatureSensorBusAdd1.pointExists()) {
                val humiditySensorBusAdd1Def = model.points.find { it.domainName == DomainName.humiditySensorBusAdd1 }
                humiditySensorBusAdd1Def.run {
                    val humiditySensorBusAdd1Id = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this as ModelPointDef,
                            profileConfiguration,
                            hssEquip.getId(),
                            site?.id ?: "",
                            hayStack.timeZone,
                            equipDis
                        )
                    )
                }
                hssEquip.humiditySensorBusAdd1.writeDefaultVal(hssEquip.temperatureSensorBusAdd1.readDefaultVal())
            }

            if (hssEquip.temperatureSensorBusAdd2.pointExists()) {
                val humiditySensorBusAdd2Def = model.points.find { it.domainName == DomainName.humiditySensorBusAdd2 }
                humiditySensorBusAdd2Def.run {
                    val humiditySensorBusAdd2Id = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this as ModelPointDef,
                            profileConfiguration,
                            hssEquip.getId(),
                            site?.id ?: "",
                            hayStack.timeZone,
                            equipDis
                        )
                    )
                }
                hssEquip.humiditySensorBusAdd2.writeDefaultVal(hssEquip.temperatureSensorBusAdd2.readDefaultVal())
            }

            // Sensor Bus Pressure now has a "None" option at index 0.
            // So, if this point exists, its association should be 1.
            if (hssEquip.sensorBusPressureEnable.readDefaultVal() > 0.0) {
                hssEquip.pressureSensorBusAdd0.writeDefaultVal(1.0)
            }

            deviceBuilder.doCutOverMigration(
                device.get("id").toString(),
                deviceModel,
                deviceDis,
                HyperStatSplitDeviceCutoverMapping.entries,
                profileConfiguration
            )
        }
    }

    private fun migrateUniversalInValue(value: Int): Double {
        return when (value) {
            0 -> CpuUniInType.CURRENT_TX_10.ordinal.toDouble()
            1 -> CpuUniInType.CURRENT_TX_20.ordinal.toDouble()
            2 -> CpuUniInType.CURRENT_TX_50.ordinal.toDouble()
            3 -> CpuUniInType.CURRENT_TX_100.ordinal.toDouble()
            4 -> CpuUniInType.CURRENT_TX_150.ordinal.toDouble()
            5 -> CpuUniInType.SUPPLY_AIR_TEMPERATURE.ordinal.toDouble()
            6 -> CpuUniInType.MIXED_AIR_TEMPERATURE.ordinal.toDouble()
            7 -> CpuUniInType.OUTSIDE_AIR_TEMPERATURE.ordinal.toDouble()
            8 -> CpuUniInType.FILTER_STATUS_NC.ordinal.toDouble()
            9 -> CpuUniInType.FILTER_STATUS_NO.ordinal.toDouble()
            10 -> CpuUniInType.CONDENSATE_STATUS_NC.ordinal.toDouble()
            11 -> CpuUniInType.CONDENSATE_STATUS_NO.ordinal.toDouble()
            12 -> CpuUniInType.DUCT_STATIC_PRESSURE1_1.ordinal.toDouble()
            13 -> CpuUniInType.DUCT_STATIC_PRESSURE1_2.ordinal.toDouble()
            14 -> CpuUniInType.VOLTAGE_INPUT.ordinal.toDouble()
            15 -> CpuUniInType.THERMISTOR_INPUT.ordinal.toDouble()
            16 -> CpuUniInType.DUCT_STATIC_PRESSURE1_10.ordinal.toDouble()
            17 -> CpuUniInType.GENERIC_ALARM_NC.ordinal.toDouble()
            18 -> CpuUniInType.GENERIC_ALARM_NO.ordinal.toDouble()
            else -> CpuUniInType.NONE.ordinal.toDouble()
        }
    }

    private fun doOAOProfileMigration() {
        val oao = hayStack.readEntity("equip and oao")
        if (oao.isEmpty() || (oao.isNotEmpty() && oao.containsKey("domainName"))) {
            CcuLog.i(Domain.LOG_TAG, "OAO equip is not found or OAO equip is already migrated")
            return
        }

        val CT_INDEX_START = 8
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val site = hayStack.site
        CcuLog.i(Domain.LOG_TAG, "Do OAO migration for oao")
        val model = ModelLoader.getSmartNodeOAOModelDef()
        val equipDis = "${site?.displayName}-OAO-${oao["group"]}"
        val deviceModel =  ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective
        val deviceBuilder = DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
        val device = hayStack.readEntity("device and addr == \"" + oao["group"] + "\"")
        val deviceDis = "${site?.displayName}-SN-${oao["group"]}"
        val profileType = ProfileType.OAO

        val oaoConfiguration = OAOProfileConfiguration(
            Integer.parseInt(oao["group"].toString()),
            NodeType.SMART_NODE.name,
            0,
            oao["roomRef"].toString(),
            oao["floorRef"].toString(),
            profileType,
            model
        ).getActiveConfiguration()

        CcuLog.d(L.TAG_CCU_DOMAIN ,"oao migration configuration: $oaoConfiguration, deviceDis: $deviceDis, equipDis: $equipDis")
        equipBuilder.doCutOverMigration(oao["id"].toString(), model,
            equipDis, OaoCutOverMapping.entries, oaoConfiguration, equipHashMap = oao)

        deviceBuilder.doCutOverMigration(
            device["id"].toString(),
            deviceModel,
            deviceDis,
            NodeDeviceCutOverMapping.entries,
            oaoConfiguration
        )

        val oaoEquip = OAOEquip(oao["id"].toString())
        oaoEquip.currentTransformerType.writeDefaultVal(oaoEquip.currentTransformerType.readDefaultVal() - CT_INDEX_START)

        fun getSystemProfileType(): String {
            val profileType = L.ccu().systemProfile.profileType
            return when (profileType) {
                ProfileType.SYSTEM_DAB_ANALOG_RTU, ProfileType.SYSTEM_DAB_HYBRID_RTU, ProfileType.SYSTEM_DAB_STAGED_RTU, ProfileType.SYSTEM_DAB_STAGED_VFD_RTU, ProfileType.dabExternalAHUController, ProfileType.SYSTEM_DAB_ADVANCED_AHU -> "dab"
                ProfileType.SYSTEM_VAV_ANALOG_RTU, ProfileType.SYSTEM_VAV_HYBRID_RTU, ProfileType.SYSTEM_VAV_IE_RTU, ProfileType.SYSTEM_VAV_STAGED_RTU, ProfileType.SYSTEM_VAV_STAGED_VFD_RTU, ProfileType.SYSTEM_VAV_ADVANCED_AHU, ProfileType.vavExternalAHUController -> "vav"
                else -> {
                    "default"
                }
            }
        }

        val deviceEntityId =
            hayStack.readEntity("device and addr == \"${oaoConfiguration.nodeAddress}\"")["id"].toString()
        val device1 = Device.Builder().setHDict(hayStack.readHDictById(deviceEntityId)).build()

        fun updateDevicePoint(domainName: String, port: String, analogType: Any) {
            val pointDef = deviceModel.points.find { it.domainName == domainName }
            pointDef?.let {
                val pointDict = getDevicePointDict(domainName, deviceEntityId, hayStack).apply {
                    this["port"] = port
                    this["analogType"] = analogType
                }
                deviceBuilder.updatePoint(it, oaoConfiguration, device1, pointDict)
            }
        }

        //Update analog input points
        updateDevicePoint(DomainName.analog1In, Port.ANALOG_IN_ONE.name, 5)
        updateDevicePoint(
            DomainName.analog2In,
            Port.ANALOG_IN_TWO.name,
            8 + oaoEquip.currentTransformerType.readDefaultVal().toInt()
        )

        //Update analog output points
        updateDevicePoint(
            DomainName.analog1Out,
            Port.ANALOG_OUT_ONE.name,
            "${oaoEquip.outsideDamperMinDrive.readDefaultVal()} - ${oaoEquip.outsideDamperMaxDrive.readDefaultVal()}"
        )
        updateDevicePoint(
            DomainName.analog2Out,
            Port.ANALOG_OUT_TWO.name,
            "${oaoEquip.returnDamperMinDrive.readDefaultVal()} - ${oaoEquip.returnDamperMaxDrive.readDefaultVal()}"
        )

        //Update TH input points
        updateDevicePoint(DomainName.th1In, Port.TH1_IN.name, 0)
        updateDevicePoint(DomainName.th2In, Port.TH2_IN.name, 0)

        //Update relay points
        updateDevicePoint(
            DomainName.relay1,
            Port.RELAY_ONE.name,
            OutputRelayActuatorType.NormallyClose.displayName
        )
        updateDevicePoint(
            DomainName.relay2,
            Port.RELAY_TWO.name,
            OutputRelayActuatorType.NormallyClose.displayName
        )

    }


    fun updateMigrationVersion(){
        val pm = Globals.getInstance().applicationContext.packageManager
        val pi: PackageInfo
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0)
            val currentAppVersion = pi.versionName.substring(pi.versionName.lastIndexOf('_') + 1)
            val migrationVersion = Domain.readStrPointValueByDomainName(DomainName.migrationVersion)
            CcuLog.d(TAG_CCU_DOMAIN, "currentAppVersion: $currentAppVersion, migrationVersion: $migrationVersion")
            if (currentAppVersion != migrationVersion) {
                Domain.writeDefaultValByDomain(DomainName.migrationVersion, currentAppVersion)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

     fun temperatureModeMigration() {
        if (!PreferenceUtil.isTempModeMigrationRequired()) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL,"Temperature mode migration Initiated")
            writeValAtLevelByDomain(DomainName.temperatureMode, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,1.0)
            PreferenceUtil.setTempModeMigrationNotRequired()
        }
    }

    private fun migrateAnalogTypeForVavAnalog1In() {
        CcuLog.i(L.TAG_CCU_MIGRATION_UTIL,"Migrate Analog Type for VAV Analog1In!!")
        hayStack.readAllEntities("equip and zone and vav and domainName").forEach { equip ->
            val device = hayStack.read("device and equipRef == \"${equip["id"]}\"")
            val analog1In = hayStack.read("domainName == \"${DomainName.analog1In}\" and deviceRef == \"${device["id"]}\"")
            val damperType = hayStack.readPointPriorityValByQuery("domainName == \"${DomainName.damperType}\" and equipRef == \"${equip["id"]}\"")
            if (analog1In.isNotEmpty()) {
                val analog1InPoint = RawPoint.Builder().setHDict(hayStack.readHDictById(analog1In["id"].toString())).build()
                analog1InPoint.type = getDamperTypeString(damperType.toInt())
                hayStack.updatePoint(analog1InPoint , analog1In["id"].toString())
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL,"Analog1In type updated for device ${device["dis"]}")
            }
        }
    }
    private fun getDamperTypeString(index: Int) : String {
        return when(index) {
            0 -> "0-10v"
            1 -> "2-10v"
            2 -> "10-2v"
            3 -> "10-0v"
            4 -> "Smart Damper"
            5 -> "0-5v"
            else -> { "0-10v" }
        }
    }

    private fun migrateHisInterpolateForDevicePoints() {
        CcuLog.i(L.TAG_CCU_MIGRATION_UTIL,"Migrate His Interpolate for Device Points!!")
        val devices= hayStack.readAllEntities("device and domainName and not modbus and not ccu") // DM integrated devices
        devices.forEach { device ->
            CcuLog.d(L.TAG_CCU_MIGRATION_UTIL,"device id ${device["id"]} device name ${device["dis"]}")

            val devicePointsList = hayStack.readAllEntities("deviceRef == \"${device["id"]}\"")
            val haystackDevice = Device.Builder().setHDict(hayStack.readHDictById(device["id"].toString())).build()
            val deviceModel =  if(device["sourceModel"] == null) { // If sourceModel is not found, fetch model by using domainName
                try {
                    ModelLoader.getModelForDomainName(device["domainName"].toString())
                } catch (e: Exception) {
                    CcuLog.e(L.TAG_CCU_DOMAIN, "Error while fetching sourceModel for device ${device["dis"]}. Skipping hisInterpolate migration.....")
                    return@forEach
                }
            }
            else {
                ModelCache.getModelById(device["sourceModel"].toString())
            }

            val equip = hayStack.read("id == ${device["equipRef"]}")
            val profileType = equip["profile"].toString()
            val equipModel = ModelCache.getModelById(equip["sourceModel"].toString())
            val profileConfiguration = getProfileConfig(profileType)

            val entityMapper = EntityMapper(equipModel as SeventyFiveFProfileDirective)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

            devicePointsList.forEach { point ->
                if (!point.containsKey("hisInterpolate")) {
                    CcuLog.d(L.TAG_CCU_DOMAIN,"hisInterpolate not found for device point ${point["dis"]}. Updating hisInterpolate tag.....")
                    val modelPointDef = deviceModel.points.find { it.domainName == point["domainName"].toString() }
                     deviceBuilder.updatePoint(modelPointDef as SeventyFiveFDevicePointDef ,profileConfiguration, haystackDevice,point)
                }
            }
        }
    }

    private fun getProfileConfig(profileType: String) : ProfileConfiguration {
        return when(profileType) {
            "dabExternalAHUController", "vavExternalAHUController" -> {
                val profile = ExternalAhuConfiguration(profileType)
                profile
            }else -> {
                /*
                 This is not a robust solution, but it works for now.
                 Right now, existing configuration classes reside in the :logic package and aren't accessible here.
                 Created a DefaultConfiguration class that holds the few fields (group, roomRef, floorRef, profile) that are needed inside the :domain package.
             */
                val profile = DefaultProfileConfiguration(1000, "", 0, "", "", profileType)
                profile
            }
        }
    }


    private fun doHSCPUDMMigration() {
        val hyperStatCPUEquip = hayStack.readAllEntities("equip and hyperstat and cpu")
            .filter { it["domainName"] == null }
            .toList()
        CcuLog.d(L.TAG_CCU_HSCPU, "HyperStat CPU Equip Migration list of equips $hyperStatCPUEquip")
        if (hyperStatCPUEquip.isNotEmpty()) {
            val model = ModelLoader.getHyperStatCpuModel()
            val deviceModel =
                    ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
            val deviceBuilder =
                    DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
            val profileType = ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT
            val equipBuilder = ProfileEquipBuilder(hayStack)
            hyperStatCPUEquip.forEach {
                CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")
                val equipDis = "${hayStack.siteName}-${model.name}-${it["group"]}"
                val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${it["group"]}"
                val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
                val profileConfiguration = CpuConfiguration(
                    Integer.parseInt(it["group"].toString()),
                    NodeType.HYPER_STAT.name,
                    0,
                    it["roomRef"].toString(),
                    it["floorRef"].toString(),
                    profileType, model
                ).getActiveConfiguration()
                equipBuilder.doCutOverMigration(
                    it["id"].toString(),
                    model,
                    equipDis,
                    HyperStatV2EquipCutoverMapping.getCPUEntries(),
                    profileConfiguration,
                    equipHashMap = it
                )

                deviceBuilder.doCutOverMigration(
                    device.get("id").toString(),
                    deviceModel,
                    deviceDis,
                    HyperStatDeviceCutOverMapping.entries,
                    profileConfiguration
                )
                updateTempOffsetValue(it["id"].toString())
            }
        }
        addDomainEquips(hayStack)
    }

    private fun updateBacnetProperties(hayStack: CCUHsApi) {
        // migration for system equip and points
        val hsSystemEquip = hayStack.readEntity("equip and system and not modbus and domainName")
        val site = hayStack.site
        val profileEquipBuilder = ProfileEquipBuilder(hayStack)

        if (hsSystemEquip.isNotEmpty() && site != null) {
            val systemModel = ModelCache.getModelById(hsSystemEquip["sourceModel"].toString())
            val profileType = hsSystemEquip["profile"].toString()
            val profileConfig = DefaultProfileConfiguration(
                hsSystemEquip["group"].toString().toInt(),
                "",
                0,
                hsSystemEquip["roomRef"].toString(),
                hsSystemEquip["floorRef"].toString(),
                profileType
            )
            val equipDis = hsSystemEquip["dis"].toString()
            val systemEquip = profileEquipBuilder.buildEquip(
                EquipBuilderConfig(
                    systemModel,
                    profileConfig,
                    site.id,
                    hayStack.timeZone,
                    equipDis
                )
            )
            hayStack.updateEquip(systemEquip, hsSystemEquip["id"].toString())

            val bacnetPoints = hayStack.readAllEntities(
                "point and domainName and bacnetId and equipRef == \"${hsSystemEquip["id"]}\""
            )
            bacnetPoints.forEach { point ->
                CcuLog.d(
                    L.TAG_CCU_DOMAIN,
                    "Updating bacnetId for the system point(${point["dis"]})."
                )
                systemModel.points.find { it.domainName == point["domainName"].toString() }?.let { pointDef ->
                    profileEquipBuilder.updatePoint(
                        PointBuilderConfig(
                            pointDef,
                            profileConfig,
                            hsSystemEquip["id"].toString(),
                            site.id,
                            site.tz,
                            equipDis
                        ), point
                    )

                }
            }
        } else {
            CcuLog.i(Domain.LOG_TAG, "system profile is not migrated - $hsSystemEquip")
        }

        // migration for terminal equip and points
        val equips = hayStack.readAllEntities("equip and not system and not modbus and domainName and not building and not diag and not config")
        equips.forEach { equip ->
            val equipModel = ModelCache.getModelById(equip["sourceModel"].toString())
            val equipDis = equip["dis"].toString()
            val equipId = equip["id"].toString()
            val profileType = equip["profile"].toString()
            val profileConfiguration = DefaultProfileConfiguration(
                equip["group"].toString().toInt(),
                "",
                0,
                equip["roomRef"].toString(),
                equip["floorRef"].toString(),
                profileType
            )
            val bacnetPoints = hayStack.readAllEntities("point and domainName and bacnetId and equipRef == \"$equipId\"")
            bacnetPoints.forEach { point ->
                CcuLog.d(
                    L.TAG_CCU_DOMAIN,
                    "Updating bacnetId for the point(${point["dis"]})."
                )
                equipModel.points.find { it.domainName == point["domainName"].toString() }?.let { pointDef ->
                    profileEquipBuilder.updatePoint(
                        PointBuilderConfig(
                            pointDef,
                            profileConfiguration,
                            equipId,
                            site!!.id,
                            site.tz,
                            equipDis
                        ), point
                    )
                }
            }
        }
    }

    private fun updateMinCfmPointMaxVal(minMaxCfmDomainNames: Pair<String, String>) {
        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL,"executing updateMinCfmPointMaxVal")
        hayStack.readAllEntities(" point and zone and config and domainName == \"${minMaxCfmDomainNames.first}\" ").forEach { minCfmMap ->
            val maxCfmVal = hayStack.readPointPriorityValByQuery(
                " domainName == \"${minMaxCfmDomainNames.second}\" and equipRef == \"${
                    minCfmMap["equipRef"]
                }\" "
            )
            CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Point value for ${minMaxCfmDomainNames.second}: $maxCfmVal")
            if (maxCfmVal != minCfmMap["maxVal"].toString().toDouble()) {
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Updating point for ${minMaxCfmDomainNames.first}")
                hayStack.updatePoint(
                    Point.Builder()
                        .setHDict(hayStack.readHDictById(minCfmMap["id"].toString()))
                        .setMaxVal(maxCfmVal.toString()).build(),
                    minCfmMap["id"].toString()
                )
            }
        }
    }

    // This method is to be used in case of CCUs which have been replaced and post which, the bypassDamper data is not available in the new CCU.
    private fun restoreMissingBypassDamperAfterReplace(): Boolean {
        // Perform the operation only on a replaced CCU and if the bypassDamper does not exist in the new CCU.
        if(validateBypassDamperRecoveryForReplacedCcu()) {
            /** If the value return while fetching the bypassDamper equip for this CCU which is missing ahuRef is null, that means,
             * there was error occurred while fetching the remote equip. To ensure retrial return false and not set the preference
             */
            CcuLog.i(TAG_CCU_BYPASS_RECOVER, "This is a replaced CCU. No bypassDamper found locally. Will be checking the cloud now.")
            val remoteBypassDamperEquip = hayStack.readRemoteEntitiesByQuery("equip and domainName == \"${DomainName.smartnodeBypassDamper}\" and not ahuRef and ccuRef==\"${hayStack.ccuId.replace("@","")}\" and siteRef == \"${hayStack.site?.id?.replace("@","")}\"")
                ?. firstOrNull()
                ?: return false
            CcuLog.i(TAG_CCU_BYPASS_RECOVER,"bypassDamper equip with no ahuRef found in cloud for the current ccuID. Continuing with the recovery process.")
            /** If the value return while fetching the bypassDamper equip for this CCU which is missing ahuRef is empty, that means,
             * the bypassDamper does not exist before the CCU was replaced. In this case, return true and set the preference
             * since no migration is required.
             */
            if (remoteBypassDamperEquip.isEmpty) {
                CcuLog.i(TAG_CCU_BYPASS_RECOVER,"bypassDamper does not exist before the CCU was replaced. No migration required. Setting preference.")
                return true
            }
            CcuLog.d(TAG_CCU_BYPASS_RECOVER,"remoteBypassDamper equip = $remoteBypassDamperEquip")
            val equipRef = remoteBypassDamperEquip.id().toString()

            /** The operation is considered to be a failure if the equipPoints or devicePoints or the device itself is null.
             * In this case, return false and not set the preference.
             */
            val remoteBypassDamperEquipPoints = hayStack.readRemoteEntitiesByQuery("point and equipRef == $equipRef")
                ?: return false
            val remoteBypassDamperDevice = hayStack.readRemoteEntitiesByQuery("device and equipRef == $equipRef")
                ?. firstOrNull()
                ?: return false
            CcuLog.d(TAG_CCU_BYPASS_RECOVER,"remoteBypassDamper device = $remoteBypassDamperDevice")
            val remoteBypassDamperDevicePoints = hayStack.readRemoteEntitiesByQuery("point and deviceRef == ${remoteBypassDamperDevice.id()}")
                ?: return false

            CcuLog.i(TAG_CCU_BYPASS_RECOVER, "No null response found for the fetching of the remote bypassDamper entities. Proceeding with their restoration.")
            /** The equips and points are fetched successfully. Now, we can proceed with the migration.
             * First, we will build the equip, device and points in the CCU again using the model based building operations.
             * Then we will update those entities with the existing id of fetched entities.
             * Finally, we will import the priority array for the points.
             */
            val bypassDamperEquipModel = ModelLoader.getSmartNodeBypassDamperModelDef() as SeventyFiveFProfileDirective

            val profileConfiguration = BypassDamperProfileConfiguration (
                Integer.parseInt(remoteBypassDamperEquip["group"].toString()),
                NodeType.SMART_NODE.name ,
                0,
                remoteBypassDamperEquip["roomRef"].toString(),
                remoteBypassDamperEquip["floorRef"].toString(),
                ProfileType.BYPASS_DAMPER,
                bypassDamperEquipModel
            ).getDefaultConfiguration()

            restoreBypassDamperEquipAndPoints(remoteBypassDamperEquip, remoteBypassDamperEquipPoints, profileConfiguration)
            restoreBypassDamperDeviceAndPoints(remoteBypassDamperDevice, remoteBypassDamperDevicePoints, profileConfiguration)
        }
        return true
    }

    private fun validateBypassDamperRecoveryForReplacedCcu() : Boolean {
        return PreferenceUtil.getCcuInstallType().equals("REPLACECCU")
                && hayStack.readId("equip and system and not modbus and not connect and profile == \"SYSTEM_DEFAULT\"") == null
                && hayStack.readId("domainName == \"${DomainName.smartnodeBypassDamper}\"") == null
    }

    private fun restoreBypassDamperEquipAndPoints(remoteBypassDamperEquip: HDict, remoteBypassDamperEquipPoints: List<HDict>, profileConfiguration: BypassDamperProfileConfiguration) {
        CcuLog.i(TAG_CCU_BYPASS_RECOVER, "Restoring bypassDamper equip and points and writable arrays")
        val pointIdList = mutableListOf<HDict>()
        val equipBuilder = ProfileEquipBuilder(hayStack)

        CcuLog.d(TAG_CCU_BYPASS_RECOVER,"total remoteBypassDamperEquipPoints = ${remoteBypassDamperEquipPoints.size}")
        CcuLog.d(TAG_CCU_BYPASS_RECOVER, "remoteBypassDamperEquipPoints = $remoteBypassDamperEquipPoints")

        CcuLog.i(TAG_CCU_BYPASS_RECOVER, "Restoring bypassDamper equip ${remoteBypassDamperEquip.getStr("dis")} with domainName ${remoteBypassDamperEquip.getStr("domainName")} and id ${remoteBypassDamperEquip.id()}")
        val bypassEquip = equipBuilder.buildEquip( EquipBuilderConfig(
            profileConfiguration.model,
            profileConfiguration,
            hayStack.siteIdRef.toString(),
            hayStack.timeZone,
            remoteBypassDamperEquip.getStr("dis"))
        )

        bypassEquip.id = remoteBypassDamperEquip.id().toString()
        bypassEquip.ahuRef = hayStack.readId("equip and system and not modbus and not connect")

        hayStack.addRemoteEquip(bypassEquip, bypassEquip.id.replace("@",""))
        hayStack.syncStatusService.addUpdatedEntity(bypassEquip.id.toString())

        val remotePointMapWithDomainName = remoteBypassDamperEquipPoints.associateBy { it.get("domainName").toString() }
        val toBeUpdatePoints = mutableListOf<String>()

        profileConfiguration.model.points.forEach { pointMetaData ->
            val pointBuilderConfig = PointBuilderConfig(
                pointMetaData,
                profileConfiguration,
                bypassEquip.id,
                hayStack.siteIdRef.toString(),
                hayStack.timeZone,
                remoteBypassDamperEquip.dis()
            )
            CcuLog.d(TAG_CCU_BYPASS_RECOVER,"pointMetaData for equipPoint = $pointMetaData")
            if(remotePointMapWithDomainName.containsKey(pointMetaData.domainName)) {
                CcuLog.d(TAG_CCU_BYPASS_RECOVER,"point model id: ${pointMetaData.domainName}")
                CcuLog.d(TAG_CCU_BYPASS_RECOVER,"remote point found in Model: ${remotePointMapWithDomainName[pointMetaData.domainName]}")
                val equipPoint = equipBuilder.buildPoint(pointBuilderConfig)
                equipPoint.id = remotePointMapWithDomainName[pointMetaData.domainName]!!.id().toString()
                hayStack.addRemotePoint(equipPoint, equipPoint.id.toString().replace("@",""))
                hayStack.syncStatusService.addUpdatedEntity(equipPoint.id)
                if(pointMetaData.tagNames.contains("writable")) {
                    pointIdList.add(HDictBuilder().add("id", HRef.copy(equipPoint.id)).toDict())
                }
                toBeUpdatePoints.add(equipPoint.id)
            } else {
                CcuLog.d(TAG_CCU_BYPASS_RECOVER,"remote point not found in Model: ${pointMetaData.domainName}. Adding new point")
                equipBuilder.createPoint(pointBuilderConfig)
            }
        }
        deleteRemotePointsIfNotAvailableInModel(remoteBypassDamperEquipPoints, toBeUpdatePoints)
        CcuLog.d(TAG_CCU_BYPASS_RECOVER,"Total equip points to have their priority array synced: ${pointIdList.size}")
        CcuLog.d(TAG_CCU_BYPASS_RECOVER,"pointIdList for equipPoints = $pointIdList")
        CcuLog.i(TAG_CCU_BYPASS_RECOVER, "Executing importPointArrays for equip points.")
        hayStack.importPointArrays(pointIdList)
    }

    private fun restoreBypassDamperDeviceAndPoints(remoteBypassDamperDevice: HDict, remoteBypassDamperDevicePoints: List<HDict>, profileConfiguration: BypassDamperProfileConfiguration) {
        CcuLog.i(TAG_CCU_BYPASS_RECOVER, "Restoring bypassDamper device and points and writable arrays")
        val pointIdList = mutableListOf<HDict>()
        val deviceBuilder = DeviceBuilder(hayStack, EntityMapper(profileConfiguration.model))
        val smartNodeDeviceModel = ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective

        CcuLog.i(TAG_CCU_BYPASS_RECOVER, "Restoring bypassDamper device ${remoteBypassDamperDevice.getStr("dis")} with domainName ${remoteBypassDamperDevice.getStr("domainName")} and id ${remoteBypassDamperDevice.id()}")
        val deviceIterator = remoteBypassDamperDevice.iterator() as Iterator<Map.Entry<Any, Any>>
        val deviceMap = HashMap<Any, Any>()
        while (deviceIterator.hasNext()) {
            val mapEntry= deviceIterator.next()
            deviceMap[mapEntry.key] = mapEntry.value
        }
        val device = Device.Builder().setHashMap(deviceMap).build()
        device.id = remoteBypassDamperDevice.id().toString()
        hayStack.addRemoteDevice(device, device.id.replace("@",""))

        deviceBuilder.updateDevice(device.id, smartNodeDeviceModel, device.displayName)

        val remotePointMapWithDomainName = remoteBypassDamperDevicePoints.associateBy { it.getStr("domainName") }
        val toBeUpdatePoints = mutableListOf<String>()

        smartNodeDeviceModel.points.forEach { pointMetaData ->
            CcuLog.d(TAG_CCU_BYPASS_RECOVER,"pointMetaData for devicePoint = $pointMetaData")
            if(remotePointMapWithDomainName.containsKey(pointMetaData.domainName)) {
                CcuLog.d(TAG_CCU_BYPASS_RECOVER,"point model id: ${pointMetaData.domainName}")
                CcuLog.d(TAG_CCU_BYPASS_RECOVER,"remote point found in Model: ${remotePointMapWithDomainName[pointMetaData.domainName]}")
                val devicePoint = deviceBuilder.buildRawPoint(pointMetaData, profileConfiguration, device)
                devicePoint.id = remotePointMapWithDomainName[pointMetaData.domainName]?.id().toString()
                hayStack.addRemotePoint(devicePoint, devicePoint.id.replace("@",""))
                hayStack.syncStatusService.addUpdatedEntity(devicePoint.id.toString())
                if(pointMetaData.tagNames.contains("writable")){
                    pointIdList.add(HDictBuilder().add("id", HRef.copy(devicePoint.id)).toDict())
                }
                toBeUpdatePoints.add(devicePoint.id)
            } else {
                CcuLog.d(TAG_CCU_BYPASS_RECOVER,"remote point not found in Model: ${pointMetaData.domainName}. Adding new point")
                deviceBuilder.createPoint(pointMetaData, profileConfiguration, device, device.displayName)
            }
        }
        deleteRemotePointsIfNotAvailableInModel(remoteBypassDamperDevicePoints, toBeUpdatePoints)
        CcuLog.d(TAG_CCU_BYPASS_RECOVER,"Total device points to have their priority array synced: ${pointIdList.size}")
        CcuLog.d(TAG_CCU_BYPASS_RECOVER,"pointIdList for devicePoints = $pointIdList")
        CcuLog.i(TAG_CCU_BYPASS_RECOVER, "Executing importPointArrays for devicePoints.")
        hayStack.importPointArrays(pointIdList)
    }

    private fun deleteRemotePointsIfNotAvailableInModel(remotePoints: List<HDict>, updatedPointIdList: List<String>) {
        CcuLog.w(TAG_CCU_BYPASS_RECOVER, "Deleting remote points if not available in model")
        remotePoints.forEach { remotePoint ->
            if(!updatedPointIdList.contains(remotePoint.id().toString())) {
                CcuLog.w(TAG_CCU_BYPASS_RECOVER, "Deleting remote point ${remotePoint.getStr("dis")} with id ${remotePoint.id()}")
                hayStack.deleteRemoteEntity(remotePoint.id().toString())
            }
        }
    }

    private fun doSseStandaloneDomainModelMigration() {
        CcuLog.i(L.TAG_CCU_DOMAIN, "SSE standalone equip migration is started")
        val sseEquips = hayStack.readAllEntities("equip and zone and sse")
            .filter { it["domainName"] == null }
            .toList()
        if (sseEquips.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "SSE DM standalone equip migration is complete")
            return
        }
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val site = hayStack.site
        sseEquips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Do DM SSE standalone equip migration for $it")
            val model = when {
                it.containsKey("sse") && it.containsKey("smartnode") -> ModelLoader.getSmartNodeSSEModel()
                else -> ModelLoader.getHelioNodeSSEModel()
            }
            val equipDis = "${site?.displayName}-SSE-${it["group"]}"
            val isHelioNode = it.containsKey("helionode")
            val deviceModel =
                if (isHelioNode) ModelLoader.getHelioNodeDevice() as SeventyFiveFDeviceDirective
                else ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective
            val deviceDis =
                if (isHelioNode) "${site?.displayName}-HN-${it["group"]}" else "${site?.displayName}-SN-${it["group"]}"
            val deviceBuilder =
                DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
            val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
            val profileType = ProfileType.SSE

            val profileConfiguration = SseProfileConfiguration(
                Integer.parseInt(it["group"].toString()),
                if (isHelioNode) NodeType.HELIO_NODE.name else NodeType.SMART_NODE.name,
                0,
                it["roomRef"].toString(),
                it["floorRef"].toString(),
                profileType,
                model
            ).getActiveConfiguration()

            equipBuilder.doCutOverMigration(
                it["id"].toString(),
                model,
                equipDis,
                SseZoneProfileCutOverMapping.entries,
                profileConfiguration,
                equipHashMap = it
            )

            val sseEquip = SseEquip(it["id"].toString())
            // temperature offset is now a literal (was multiplied by 10 before)
            val newTempOffset =
                String.format("%.1f", sseEquip.temperatureOffset.readDefaultVal() * 0.1).toDouble()
            sseEquip.temperatureOffset.writeDefaultVal(newTempOffset)
            sseEquip.demandResponseSetback.writeVal(17, 2.0)
            deviceBuilder.doCutOverMigration(
                device["id"].toString(),
                deviceModel,
                deviceDis,
                NodeDeviceCutOverMapping.entries,
                profileConfiguration
            )

            val relay1OutputEnable = sseEquip.relay1OutputState.readDefaultVal()

            if(relay1OutputEnable > 0) {
                CcuLog.i(L.TAG_CCU_DOMAIN, "Relay1 is enabled, so creating relay1OutputAssociation point")
                val relay1OutputAssociationDef =
                    model.points.find { it.domainName == "relay1OutputAssociation" }
                relay1OutputAssociationDef?.run {
                    equipBuilder.createPoint(
                        PointBuilderConfig(
                            relay1OutputAssociationDef, profileConfiguration,
                            it["id"].toString(), site?.id.toString(), site?.tz, equipDis
                        )
                    )
                }
                val coolingStage1 = hayStack
                    .readEntity("point and group == \"" + it["group"].toString() + "\" and " +
                            "domainName == \"" + DomainName.coolingStage1 + "\"")
                CcuLog.i(L.TAG_CCU_DOMAIN, "coolingStage1 point: $coolingStage1")
                if(coolingStage1.isNotEmpty()) {
                    sseEquip.relay1OutputAssociation.writeDefaultVal(1.0)
                }
                sseEquip.relay1OutputState.writeDefaultVal(1)
            }

            val relay2OutputEnable = sseEquip.relay2OutputState.readDefaultVal()

            if(relay2OutputEnable > 0) {
                CcuLog.i(L.TAG_CCU_DOMAIN, "Relay2 is enabled, so creating relay2OutputAssociation point")
                val relay2OutputAssociationDef =
                    model.points.find { it.domainName == "relay2OutputAssociation" }
                relay2OutputAssociationDef?.run {
                    equipBuilder.createPoint(
                        PointBuilderConfig(
                            relay2OutputAssociationDef, profileConfiguration,
                            it["id"].toString(), site?.id.toString(), site?.tz, equipDis
                        )
                    )
                }
            }
        }
    }

    fun checkBacnetIdMigrationRequired() {
        if(!PreferenceUtil.isBacnetIdMigrationDone()) {
            try {
                updateBacnetProperties(CCUHsApi.getInstance());
                PreferenceUtil.setBacnetIdMigrationDone();
            } catch (e : Exception) {
                //For now, we make sure it does not stop other migrations even if this fails.
                e.printStackTrace();
                CcuLog.e(L.TAG_CCU_MIGRATION_UTIL, "Error in migrateBacnetIdForVavDevices $e")
            }
        }
    }

    private fun doHSMonitoringDMMigration() {
        val hyperStatMonitoringEquip =
            hayStack.readAllEntities("equip and hyperstat and monitoring")
                .filter { it["domainName"] == null }
                .toList()

        val model = ModelLoader.getHyperStatMonitoringModel()
        val deviceModel =
            ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
        val deviceBuilder =
            DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
        val profileType = ProfileType.HYPERSTAT_MONITORING

        if (hyperStatMonitoringEquip.isNotEmpty()) {
            val equipBuilder = ProfileEquipBuilder(hayStack)
            hyperStatMonitoringEquip.forEach {
                CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")
                val equipDis = "${hayStack.siteName}-${model.name}-${it["group"]}"
                val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${it["group"]}"
                val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
                val profileConfiguration = MonitoringConfiguration(
                    Integer.parseInt(it["group"].toString()),
                    NodeType.HYPER_STAT.name,
                    0,
                    it["roomRef"].toString(),
                    it["floorRef"].toString(),
                    profileType,
                    model
                ).getActiveConfiguration()
                migrateLogicalPointsForHyperStatMonitoring(it["id"].toString(), profileConfiguration, model, equipBuilder)

                equipBuilder.doCutOverMigration(
                    it["id"].toString(),
                    model,
                    equipDis,
                    HyperStatV2EquipCutoverMapping.getMonitoringEntries(),
                    profileConfiguration,
                    equipHashMap = it
                )

                deviceBuilder.doCutOverMigration(
                    device["id"].toString(),
                    deviceModel,
                    deviceDis,
                    HyperStatDeviceCutOverMapping.entries,
                    profileConfiguration
                )
                updateTempOffsetValue(it["id"].toString())
            }
        }
    }

    private fun migrateLogicalPointsForHyperStatMonitoring(
        equipRef: String,
        profileConfiguration: MonitoringConfiguration,
        modelDef: ModelDirective,
        profileEquipBuilder: ProfileEquipBuilder
    ) {

        // Map of tags to search query
        val logicalPoints = mapOf(
            Tags.ANALOG1 to "point and analog1 and logical and equipRef == \"$equipRef\"",
            Tags.ANALOG2 to "point and analog2 and logical and equipRef == \"$equipRef\"",
            Tags.TH1 to "point and th1 and logical and equipRef == \"$equipRef\"",
            Tags.TH2 to "point and th2 and logical and equipRef == \"$equipRef\""
        )

        logicalPoints.forEach { (_, query) ->
            val logicalPoint = hayStack.readEntity(query)
            if (logicalPoint.isNotEmpty()) {
                val site = hayStack.site
                val modelPointName = getDomainNameForMonitoringProfile(logicalPoint)
                val modelPoint = modelDef.points.find { it.domainName.equals(modelPointName, true) }
                if (modelPoint != null) {
                    val equipDis = "${hayStack.siteName}-${modelDef.name}-${logicalPoint["group"]}"
                    profileEquipBuilder.updatePoint(
                        PointBuilderConfig(
                            modelPoint,
                            profileConfiguration,
                            equipRef,
                            site!!.id,
                            site.tz,
                            equipDis
                        ), logicalPoint
                    )
                }
            }
        }
    }

    private fun doOtnTerminalDomainModelMigration() {
        val otnEquips = hayStack.readAllEntities("equip and zone and otn")
            .filter { it["domainName"] == null }
            .toList()
        if (otnEquips.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "VAV DM zone equip migration is complete")
            return
        }
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val site = hayStack.site
        otnEquips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")

            val model = ModelLoader.getOtnTiModel() as SeventyFiveFProfileDirective
            val equipDis = "${site?.displayName}-OTN-${it["group"]}"
            val profileType = ProfileType.OTN

            val profileConfiguration = OtnProfileConfiguration(
                Integer.parseInt(it["group"].toString()),
                NodeType.OTN.name,
                0,
                it["roomRef"].toString(),
                it["floorRef"].toString(),
                profileType,
                model
            ).getActiveConfiguration()

            equipBuilder.doCutOverMigration(
                it["id"].toString(), model,
                equipDis, OtnEquipCutOverMapping.entries, profileConfiguration, equipHashMap = it
            )

            // At app startup, cutover migrations currently run before upgrades.
            // This is a problem because demandResponseSetback is supposed to get its value from a newly-added BuildingTuner point, which isn't available yet.
            // Setting the fallback value manually for now.
            //vavEquip.demandResponseSetback.writeVal(17, 2.0)

            val deviceModel = ModelLoader.getOtnDeviceModel() as SeventyFiveFDeviceDirective
            val deviceDis = "${site?.displayName}-OTN-${it["group"]}"
            val deviceBuilder = DeviceBuilder(hayStack, EntityMapper(model))
            val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
            deviceBuilder.doCutOverMigration(
                device["id"].toString(),
                deviceModel,
                deviceDis,
                NodeDeviceCutOverMapping.entries,
                profileConfiguration
            )

            val otnEquip = OtnEquip(it["id"].toString())
            otnEquip.temperatureOffset.writeDefaultVal(0.1 * otnEquip.temperatureOffset.readDefaultVal())
        }
    }

    private fun updateTempOffsetValue(equipRef: String) {
        val hsEquip = HyperStatEquip(equipRef)
        hsEquip.temperatureOffset.writeDefaultVal(hsEquip.temperatureOffset.readDefaultVal() / 10)
    }


    fun removeRedundantBacnetSettingPoints() {
        hayStack.readAllEntities("point and setting and point").forEach {
            CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "bacnet setting points found: ${it["dis"]}. Deleting the redundant point")
            hayStack.deleteEntity(it["id"].toString())
        }
    }
}