package a75f.io.logic.migration

import a75f.io.api.haystack.*
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.CCUTagsDb.TAG_CCU_DOMAIN
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.writeValAtLevelByDomain
import a75f.io.domain.api.DomainName
import a75f.io.domain.cutover.HyperStatSplitCpuCutOverMapping
import a75f.io.domain.cutover.HyperStatSplitDeviceCutoverMapping
import a75f.io.domain.cutover.NodeDeviceCutOverMapping
import a75f.io.domain.cutover.VavFullyModulatingRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedVfdRtuCutOverMapping
import a75f.io.domain.cutover.VavZoneProfileCutOverMapping
import a75f.io.domain.cutover.*
import a75f.io.domain.equips.DabEquip
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager.addCmBoardDevice
import a75f.io.domain.logic.DomainManager.addSystemDomainEquip
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.*
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuUniInType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuProfileConfiguration
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.occupancy.DemandResponse
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.util.DemandResponseMode
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.diag.DiagEquip
import a75f.io.logic.diag.DiagEquip.createMigrationVersionPoint
import a75f.io.logic.migration.VavAndAcbProfileMigration.Companion.cleanACBDuplicatePoints
import a75f.io.logic.migration.VavAndAcbProfileMigration.Companion.cleanVAVDuplicatePoints
import a75f.io.logic.migration.modbus.correctEnumsForCorruptModbusPoints
import a75f.io.logic.migration.scheduler.SchedulerRevampMigration
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.util.PreferenceUtil
import a75f.io.logic.util.createOfflineModePoint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRow
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter
import java.util.*


class MigrationHandler (hsApi : CCUHsApi) : Migration {

    companion object {
        fun doPostModelMigrationTasks() {
            if (!PreferenceUtil.getRecoverHelioNodeACBTunersMigration()) VavAndAcbProfileMigration.recoverHelioNodeACBTuners(CCUHsApi.getInstance())
            if (!PreferenceUtil.getACBRelayLogicalPointsMigration()) VavAndAcbProfileMigration.verifyACBIsoValveLogicalPoints(CCUHsApi.getInstance())
        }
    }

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
        doVavTerminalDomainModelMigration()
        doDabTerminalDomainModelMigration()
        doVavSystemDomainModelMigration()
        doHyperStatSplitCpuDomainModelMigration()
        createMigrationVersionPoint(CCUHsApi.getInstance())
        addSystemDomainEquip(CCUHsApi.getInstance())
        addCmBoardDevice(hayStack)
        if (!isMigrationRequired()) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "---- Migration Not Required ----")
            return
        }
        if (hayStack.readEntity(Tags.SITE).isNotEmpty()) {
            createOfflineModePoint()
            migrationForDRMode()
            migrateEquipStatusEnums()
            if(!PreferenceUtil.getSingleDualMigrationStatus()) {
                migrationToHandleInfluenceOfUserIntentOnSentPoints()
                PreferenceUtil.setSingleDualMigrationStatus()
            }

            DiagEquip.addLogLevelPoint(CCUHsApi.getInstance())
        }
        VavAndAcbProfileMigration.migrateVavAndAcbProfilesToCorrectPortEnabledStatus(hayStack)
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
        if(!PreferenceUtil.isHisTagRemovalFromNonDmDevicesDone()) {
            removeHisTagsFromNonDMDevices()
            PreferenceUtil.setHisTagRemovalFromNonDmDevicesDone()
        }

        if(!PreferenceUtil.isDeadBandMigrationRequired()){
            migrateDeadBandPoints(hayStack)
            PreferenceUtil.setDeadBandMigrationNotRequired()
        }
        if (!PreferenceUtil.getDmToDmCleanupMigration()) {
            cleanACBDuplicatePoints(CCUHsApi.getInstance())
            cleanVAVDuplicatePoints(CCUHsApi.getInstance())
            CCUHsApi.getInstance().syncEntityTree()
            PreferenceUtil.setDmToDmCleanupMigration()
        }
        if(!PreferenceUtil.isVavCfmOnEdgeMigrationDone()) {
            VavAndAcbProfileMigration.addMinHeatingDamperPositionMigration(hayStack)
            PreferenceUtil.setVavCfmOnEdgeMigrationDone()
        }
        hayStack.scheduleSync()
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
                ! DemandResponse.isDRModeActivated(hayStack)
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

    private fun migrationForDRMode() {
        val demandResponse = DemandResponseMode()
        val systemProfile = hayStack.readEntity("equip and system and not modbus")
        val displayName = systemProfile[Tags.DIS].toString()
        val siteRef = systemProfile[Tags.SITEREF].toString()
        val id = systemProfile[Tags.ID].toString()
        val tz = systemProfile[Tags.TZ].toString()
        val demandResponseMode = hayStack.readEntity(
            "demand and" +
                    " response and not activation and not enable and system and not tuner"
        )
        val demandResponseEnrollment = hayStack.readEntity(
            "demand and" +
                    " response and enable and system"
        )
        if (demandResponseMode.size > 0) {
            hayStack.deleteEntityItem(demandResponseMode["id"].toString())
        }
        if (demandResponseEnrollment.isEmpty()) {
            demandResponse.createDemandResponseEnrollmentPoint(displayName, siteRef, id, tz, hayStack
            )
        }
        migrateDemandResponseSetbackTunerForAllTempZones(hayStack)
        migrateDemandResponseForOccupancyEnum(hayStack)
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
        equipsList.add(ccuHsApi.readAllEntities("equip and vav and not system"))
        equipsList.add(ccuHsApi.readAllEntities("equip and dab and not system"))
        equipsList.add(ccuHsApi.readAllEntities("equip and dualDuct"))
        equipsList.add(ccuHsApi.readAllEntities("equip and standalone and smartstat"))
        equipsList.add(ccuHsApi.readAllEntities("equip and standalone and hyperstat"))
        equipsList.add(ccuHsApi.readAllEntities("equip and standalone and hyperstatsplit"))
        equipsList.add(ccuHsApi.readAllEntities("equip and sse"))
        equipsList.add(ccuHsApi.readAllEntities("equip and sse"))
        equipsList.add(ccuHsApi.readAllEntities("equip and ti"))
        equipsList.add(ccuHsApi.readAllEntities("equip and otn"))

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
                    migrateVavStagedSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis)
                }
                (it["profile"].toString() == "SYSTEM_VAV_STAGED_VFD_RTU" ||
                        it["domainName"].toString() == "vavStagedRtuVfdFan") -> {
                   migrateVavStagedVfdSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis)
                }
                (it["profile"].toString() == "SYSTEM_VAV_ANALOG_RTU" ||
                        it["domainName"].toString() == "vavFullyModulatingAhu") -> {
                    migrateVavFullyModulatingSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis)
                }
                else -> {}
            }

        }
    }

    private fun migrateVavStagedSystemProfile (equipId : String, equipBuilder: ProfileEquipBuilder, site: Site,
                                               deviceModel : SeventyFiveFDeviceDirective, deviceDis : String) {

        val model = ModelLoader.getVavStageRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = StagedRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, VavStagedRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration(), isSystem = true)

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
                                               deviceModel : SeventyFiveFDeviceDirective, deviceDis : String) {

        val model = ModelLoader.getVavStagedVfdRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = StagedVfdRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, VavStagedVfdRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration()
            ,isSystem = true)

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
                                                  deviceModel : SeventyFiveFDeviceDirective, deviceDis : String) {
        CcuLog.i(Domain.LOG_TAG, "VavFullyModulatingSystemProfile equipID: $equipId")
        val model = ModelLoader.getVavModulatingRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = ModulatingRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, VavFullyModulatingRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration()
            ,isSystem = true)

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

            Globals.getInstance().copyModels()
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

    fun updateMigrationVersion(){
        val pm = Globals.getInstance().applicationContext.packageManager
        val pi: PackageInfo
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0)
            val currentAppVersion = pi.versionName.substring(pi.versionName.lastIndexOf('_') + 1)
            val migrationVersion = hayStack.readDefaultStrVal("diag and migration and version")
            CcuLog.d(TAG_CCU_DOMAIN, "currentAppVersion: $currentAppVersion, migrationVersion: $migrationVersion")
            if (currentAppVersion != migrationVersion) {
                CCUHsApi.getInstance().writeDefaultVal("point and diag and migration", currentAppVersion)
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

}