package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.cutover.NodeDeviceCutOverMapping
import a75f.io.domain.cutover.VavFullyModulatingRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedVfdRtuCutOverMapping
import a75f.io.domain.cutover.VavZoneProfileCutOverMapping
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager.addSystemDomainEquip
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
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
import a75f.io.logic.migration.scheduler.SchedulerRevampMigration
import a75f.io.logic.util.PreferenceUtil
import a75f.io.logic.util.createOfflineModePoint
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRow
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter


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
        doVavTerminalDomainModelMigration()
        doVavSystemDomainModelMigration()
        createMigrationVersionPoint(CCUHsApi.getInstance())
        addSystemDomainEquip(CCUHsApi.getInstance())

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
        if (!isMigrationRequired()) {
            return
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
        hayStack.scheduleSync()
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
            priorityGrid?.let { grid ->
                val iterator: MutableIterator<HRow?>? = grid.iterator() as MutableIterator<HRow?>?
                while (iterator!=null && iterator.hasNext()) {
                    val r: HRow? = iterator.next()
                    if ((isLevelCleanable(r) && isLevelToBeCleared(r)) || isAutoAwayMappedToDemandResponseLevel(r)) {
                        hayStack.clearPointArrayLevel(desiredTempPointId, r!!.getInt("level"), false)
                    }
                }
            }
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
                " and zone and his and enum")
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
        val occModePoints = ccuHsApi.readAllEntities("occupancy and mode")
        occModePoints.forEach { occMode ->
            val occModePoint = Point.Builder().setHashMap(occMode).build()
            if (!occModePoint.enums.toString().contains("demandresponseoccupied")) {
                occModePoint.enums = Occupancy.getEnumStringDefinition()
                hayStack.updatePoint(occModePoint, occModePoint.id)
            }
        }

        val occStatePoints = ccuHsApi.readAllEntities("occupancy and state")
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
                                    equipDis, VavZoneProfileCutOverMapping.entries, profileConfiguration)

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

    fun updateMigrationVersion(){
        val pm = Globals.getInstance().applicationContext.packageManager
        val pi: PackageInfo
        try {
            pi = pm.getPackageInfo("a75f.io.renatus", 0)
            val currentAppVersion = pi.versionName.substring(pi.versionName.lastIndexOf('_') + 1)
            val migrationVersion = hayStack.readDefaultStrVal("diag and migration and version")
            CcuLog.d("CCU_DOMAIN", "currentAppVersion: $currentAppVersion, migrationVersion: $migrationVersion")
            if (currentAppVersion != migrationVersion) {
                CCUHsApi.getInstance().writeDefaultVal("point and diag and migration", currentAppVersion)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

}