package a75f.io.logic.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.api.haystack.util.BackfillUtil
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.OAOEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.createDomainDevicePoint
import a75f.io.domain.api.Domain.writeValAtLevelByDomain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.DefaultProfileConfiguration
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.cutover.DabFullyModulatingRtuCutOverMapping
import a75f.io.domain.cutover.DabStagedRtuCutOverMapping
import a75f.io.domain.cutover.DabStagedVfdRtuCutOverMapping
import a75f.io.domain.cutover.DabZoneProfileCutOverMapping
import a75f.io.domain.cutover.DefaultSystemCutOverMapping
import a75f.io.domain.cutover.HyperStatDeviceCutOverMapping
import a75f.io.domain.cutover.HyperStatSplitCpuCutOverMapping
import a75f.io.domain.cutover.HyperStatSplitDeviceCutoverMapping
import a75f.io.domain.cutover.HyperStatV2EquipCutoverMapping
import a75f.io.domain.cutover.NodeDeviceCutOverMapping
import a75f.io.domain.cutover.OaoCutOverMapping
import a75f.io.domain.cutover.OtnEquipCutOverMapping
import a75f.io.domain.cutover.SseZoneProfileCutOverMapping
import a75f.io.domain.cutover.TiCutOverMapping
import a75f.io.domain.cutover.VavFullyModulatingRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedRtuCutOverMapping
import a75f.io.domain.cutover.VavStagedVfdRtuCutOverMapping
import a75f.io.domain.cutover.VavZoneProfileCutOverMapping
import a75f.io.domain.cutover.getDomainNameForMonitoringProfile
import a75f.io.domain.equips.DabEquip
import a75f.io.domain.equips.OtnEquip
import a75f.io.domain.equips.SseEquip
import a75f.io.domain.equips.TIEquip
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.logic.CCUBaseConfigurationBuilder
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager.addCmBoardDevice
import a75f.io.domain.logic.DomainManager.addDomainEquips
import a75f.io.domain.logic.DomainManager.addSystemDomainEquip
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.EquipBuilderConfig
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.migration.DiffManger
import a75f.io.domain.util.CommonQueries
import a75f.io.domain.util.MODEL_SN_OAO
import a75f.io.domain.util.ModelCache
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.L.TAG_CCU_MIGRATION_UTIL
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfileConfiguration
import a75f.io.logic.bo.building.caz.configs.TIConfiguration
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.dab.getDevicePointDict
import a75f.io.logic.bo.building.definitions.DamperType
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ReheatType
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.HpuConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.MonitoringConfiguration
import a75f.io.logic.bo.building.hyperstat.v2.configs.Pipe2Configuration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuUniInType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuProfileConfiguration
import a75f.io.logic.bo.building.oao.OAOProfileConfiguration
import a75f.io.logic.bo.building.otn.OtnProfileConfiguration
import a75f.io.logic.bo.building.plc.PlcProfileConfig
import a75f.io.logic.bo.building.plc.addBaseProfileConfig
import a75f.io.logic.bo.building.plc.doPlcDomainModelCutOverMigration
import a75f.io.logic.bo.building.schedules.occupancy.DemandResponse
import a75f.io.logic.bo.building.sse.SseProfileConfiguration
import a75f.io.logic.bo.building.system.DefaultSystemConfig
import a75f.io.logic.bo.building.system.vav.config.ModulatingRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.bo.building.vav.VavProfileConfiguration
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.bo.haystack.device.SmartNode
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
import a75f.io.logic.util.PreferenceUtil.getMigrateDeleteRedundantOaoPointsBySystemEquip
import a75f.io.logic.util.PreferenceUtil.getModbusKvtagsDataTypeUpdated
import a75f.io.logic.util.PreferenceUtil.setMigrateDeleteRedundantOaoPointsBySystemEquip
import a75f.io.logic.util.PreferenceUtil.setModbusKvtagsDataTypeUpdate
import a75f.io.logic.util.bacnet.BacnetConfigConstants.BACNET_CONFIGURATION
import a75f.io.logic.util.bacnet.BacnetConfigConstants.NETWORK_INTERFACE
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.preference.PreferenceManager
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDevicePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.TagType
import org.joda.time.DateTime
import org.json.JSONObject
import org.projecthaystack.HDateTime
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGrid
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import org.projecthaystack.HRow
import org.projecthaystack.HStr
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter


class MigrationHandler (hsApi : CCUHsApi) : Migration {

    val TAG_CCU_BYPASS_RECOVER = "CCU_BYPASS_RECOVER"

    override val hayStack = hsApi

    private val schedulerRevamp = SchedulerRevampMigration(hayStack)
    private var isMigrationOngoing = false

    val not_external_model_query = "(not ${Tags.MODBUS} and not ${Tags.BACNET_DEVICE_ID})"

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
        CCUBaseConfigurationMigrationHandler().doCCUBaseConfigurationMigration(hayStack)
        doDefaultSystemDomainModelMigration()
        doVavTerminalDomainModelMigration()
        doDabTerminalDomainModelMigration()
        doVavSystemDomainModelMigration()
        doHyperStatSplitCpuDomainModelMigration()
        DiagEquipMigrationHandler().doDiagEquipMigration(hayStack, not_external_model_query)
        doDabSystemDomainModelMigration()
        doOAOProfileMigration()
        doSseStandaloneDomainModelMigration()
        createMigrationVersionPoint(CCUHsApi.getInstance())
        addSystemDomainEquip(CCUHsApi.getInstance())
        addCmBoardDevice(hayStack)
        doOtnTerminalDomainModelMigration()
        doHSCPUDMMigration()
        doHSHPUDMMigration()
        doHSMonitoringDMMigration()
        doHSPipe2DMMigration()
        doTiCutOverMigration()
        doPlcDomainModelCutOverMigration(hayStack, not_external_model_query)

        if(!PreferenceUtil.unoccupiedSetbackMaxUpdate()) {
            updateUnoccupiedSetbackMax()
            PreferenceUtil.setUnoccupiedSetbackMaxUpdate()
        }

        if (!isMigrationRequired()) {
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "---- Migration Not Required ----")
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
            CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in migrateHisInterpolate $e")
        }

        if (!PreferenceUtil.getOldPortEnabledMigrationStatus()) {
            try {
                VavAndAcbProfileMigration.migrateVavAndAcbProfilesToCorrectPortEnabledStatus(hayStack)
            } catch (e: Exception) {
                //TODO - This is temporary fix till vav model issue is resolved in the next releases.
                //For now, we make sure it does not stop other migrations even if this fails.
                CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in migrateVavAndAcbProfilesToCorrectPortEnabledStatus: ${e.message}")
            }
            PreferenceUtil.setOldPortEnabledMigrationStatus()
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
            CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in deleteRedundantSetbackPointsFromHnAcbEquips $e")
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
                CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in migrateAnalogTypeForVAVanalog1In $e")
           }
        }
        if(!PreferenceUtil.getBacnetSettingPointDeleted()) {
            removeRedundantBacnetSettingPoints()
            PreferenceUtil.setBacnetSettingPointDeleted()
        }
        if (!PreferenceUtil.getMigrateHyperStatSplitFanModeCache()) {
            migrateHyperStatSplitFanModeCache() // Migrate HyperStat Fan Mode Cache to HyperStatSplit Fan Mode Cache if split fan mode is present
            PreferenceUtil.setMigrateHyperStatSplitFanModeCache()
        }
        if(!PreferenceUtil.getPrangePointMigrationFlag()) {
            try {
                prangePointMigration()
                PreferenceUtil.setPrangePointMigrationFlag()
            }catch (e: Exception) {
                // For now, we make sure it does not stop other migrations even if this fails.
                CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in prangePointMigration $e")
            }
        }
        if(!PreferenceUtil.getCorrectRelay2PointRefSeriesParallel()) {
            correctPointRefForRelay2SeriesFan()
            PreferenceUtil.setCorrectRelay2PointRefSeriesParallel()
        }
        if(!PreferenceUtil.getRemoveRedundantSseDevicePoints()) {
            removeRedundantSmartnodeHelionodePointsForSse()
            PreferenceUtil.setRemoveRedundantSseDevicePoints()
        }
        if(!PreferenceUtil.getHisInterpolateCOV()) {
            updateHisInterpolate()
            PreferenceUtil.setHisInterpolateCOV()
        }
        if(!PreferenceUtil.getRestoreSourceModelTagsForOao()) {
            recoverSourceModelTagsForOao()
            correctAhuRefForBypassDamperAndCcuConfig()
            PreferenceUtil.setRestoreSourceModelTagsForOao()
        }

        if(!PreferenceUtil.getUpdatePointsFlagStatus()) {
            updateDataType()
            PreferenceUtil.setUpdatePointsFlagStatus()
        }

        if(!PreferenceUtil.getPlcUpdatePointStatus()) {
            updatePLCPhysicalPoints()
            PreferenceUtil.setPlcUpdatePointStatus()
        }

        if(!PreferenceUtil.getRelay2PortEnabledStatus()) {
            updateRelay2Port(CCUHsApi.getInstance())
            PreferenceUtil.setRelay2PortEnabledStatus()
        }
        if (PreferenceUtil.isProfileTypeCorrectedInCCUConfigEquip()) {
            updateCCUConfigEquipProfileType()
            PreferenceUtil.setProfileTypeCorrectedInCCUConfigEquip()
        }

        if (!PreferenceUtil.getDabEquipPointsUpdate()) {
            createDabMissingPoints()
            PreferenceUtil.setDabEquipPointsUpdate()
        }
        if (!PreferenceUtil.getVocSensorPointAdded()) {
            createVocSensorPointHyperStatEquip()
            PreferenceUtil.setVocSensorPointAdded()
        }

        if(!PreferenceUtil.getUpdateBacnetNetworkInterface()) {
            updateBacnetNetworkInterface()
            PreferenceUtil.setUpdateBacnetNetworkInterface()
        }

        if (!PreferenceUtil.getDevicePointsMigrationStatus()){
            try {
                CcuLog.d(
                    L.TAG_CCU_MIGRATION_UTIL,
                    "DEVICE_POINTS_MIGRATION_STATUS started"
                )
                migrateDevicePoints(CCUHsApi.getInstance())
                PreferenceUtil.setDevicePointsMigrationStatus()
                CcuLog.d(
                    L.TAG_CCU_MIGRATION_UTIL,
                    "DEVICE_POINTS_MIGRATION_STATUS ended"
                )
            }catch (e: Exception){
                CcuLog.e(
                    L.TAG_CCU_MIGRATION_UTIL,
                    "DEVICE_POINTS_MIGRATION_STATUS failed"
                )
                e.printStackTrace()
            }
        }

        if(!PreferenceUtil.getMigrateHssPoints()) {
            updateHSSPoints()
            PreferenceUtil.setMigrateHssPoints()
        }

        if (!PreferenceUtil.getRecoverCpuFromCorrecption()) {
            remigrateCpuPoint()
            PreferenceUtil.setRecoverCpuFromCorrecption()
        }
        if (!PreferenceUtil.isDuplicateBuildingAndSystemPointsAreRemoved()) {
            // Handling exception in case of failure
            try {
                removeDuplicateBuildingAndSystemPoints()
                PreferenceUtil.setDuplicateBuildingAndSystemPointsAreRemoved()
            } catch (e: Exception) {
                e.printStackTrace()
                CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in removeDuplicateBuildingAndSystemPoints $e")
            }
        }
        if (!PreferenceUtil.isBackFillValueUpdateRequired()) {
            updateBackFillDefaultValue()
            PreferenceUtil.setBackFillValueUpdateDone()
        }
        if (!PreferenceUtil.isBypassDamperEquipPointsMigrationRequired()) {
            updateBypassDamperEquipPoints()
            PreferenceUtil.setBypassDamperEquipPointsMigrated()
        }
        if (!PreferenceUtil.isVavAndDabEquipAnalog1InPointsMigrationRequired()) {
            updateVavAndDabEquipPoints()
            PreferenceUtil.setVavAndDabEquipAnalog1InPointsMigrated()
        }
        if (!PreferenceUtil.isConnectModuleOAOPointDeleted()) {
            deleteConnectModuleOaoPoint()
            PreferenceUtil.setConnectModuleOAOPointDeleted()
        }
        if (!PreferenceUtil.isDuplicateDualDuctSensorPointsAreRemoved()) {
            removingDuplicateDualDuctSensorPoints()
            PreferenceUtil.SetDuplicateDualDuctSensorPointsAreRemoved()
        }


        if (!PreferenceUtil.nonDmPointRemoveStatus()) {
            removeNonDmSensorPoints()
            PreferenceUtil.setNonDmPointRemoveStatus()
        }


        if (!PreferenceUtil.getFloorRefUpdateStatus()) {
            try {
                CcuLog.i(
                    TAG_CCU_MIGRATION_UTIL,
                    "floor ref update migration stated"
                )
                updateFloorRefToRoomPoints()
                PreferenceUtil.setFloorRefUpdateStatus()
                CcuLog.i(
                    TAG_CCU_MIGRATION_UTIL,
                    "floor ref update migration ended"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error during Floor Ref Update  ${e.message}")
            }
        }

        if(!getModbusKvtagsDataTypeUpdated()) {
            correctDataTypeForKVPairsInModbus()
            setModbusKvtagsDataTypeUpdate()
        }

        if(!getMigrateDeleteRedundantOaoPointsBySystemEquip()) {
            deleteRedundantOaoPointsBasedOnCurrentSystemProfile()
        }

        hayStack.scheduleSync()
    }

    private fun doDefaultSystemDomainModelMigration() {
        val defaultSystemEquip = hayStack.readEntity("equip and not domainName and system and default and $not_external_model_query")

        val site = hayStack.site
        if (defaultSystemEquip.isEmpty() || site == null) {
            CcuLog.i(Domain.LOG_TAG, "Default DM system equip migration not required : site $site")
            return
        }
        CcuLog.i(Domain.LOG_TAG, "Default DM system equip migration started : equip $defaultSystemEquip")
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val deviceModel = ModelLoader.getCMDeviceModel() as SeventyFiveFDeviceDirective
        val deviceDis = hayStack.siteName +"-"+ deviceModel.name

        val model = ModelLoader.getDefaultSystemProfileModel()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = DefaultSystemConfig(model as SeventyFiveFProfileDirective)
        val equipId = defaultSystemEquip["id"].toString()

        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, DefaultSystemCutOverMapping.entries , profileConfig.getDefaultConfiguration(), isSystem = true,equipHashMap = defaultSystemEquip)

        val entityMapper = EntityMapper(model)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        val cmDevice = hayStack.readEntity("cm and device")
        if (cmDevice.isNotEmpty()) {
            CcuLog.d(Domain.LOG_TAG, "Deleting cm device")
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

    private fun removeDuplicateBuildingAndSystemPoints() {
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Removing duplicate building and system points")

        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        val buildingEquip = Domain.hayStack.readEntityByDomainName("buildingEquip")

        val vavDabAndDuplicatePointMigrationHandler = VavDabAndDuplicatePointMigrationHandler(systemEquip)
        if (systemEquip.isNotEmpty()) {
            vavDabAndDuplicatePointMigrationHandler.createSatSpResPoint()
            vavDabAndDuplicatePointMigrationHandler.deleteSatSPMaxDuplicatePoints()
        } else {
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "System Equip not found")
        }

        if (buildingEquip.isNotEmpty()) {
            vavDabAndDuplicatePointMigrationHandler.deleteReheatZoneMaxDischargeTemp(buildingEquip)
        } else {
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Building Equip not found")
            return
        }
    }

    private fun updateBacnetNetworkInterface() {
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating Bacnet network interface")
        try {
            val connectivityManager = Globals.getInstance().applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val allNetworks = connectivityManager.allNetworks // Get all available networks

            var ethernetAvailable = false
            var wifiAvailable = false

            for (network in allNetworks) {
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                if (networkCapabilities != null) {
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        ethernetAvailable = true
                    }
                    if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        wifiAvailable = true
                    }
                }
            }

            val sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
            val confString: String = sharedPreferences.getString(BACNET_CONFIGURATION, "").toString()

            if (confString.isEmpty()) {
                CcuLog.e(TAG_CCU_MIGRATION_UTIL, "BACnet configuration not found")
                return
            }

            val config = JSONObject(confString)
            val networkObject = config.getJSONObject("network")

            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Ethernet available: $ethernetAvailable, Wi-Fi available: $wifiAvailable")

            when {
                ethernetAvailable && wifiAvailable -> {
                    // If both Ethernet and Wi-Fi are available, prefer Ethernet
                    networkObject.put(NETWORK_INTERFACE, "Ethernet")
                }
                ethernetAvailable -> {
                    networkObject.put(NETWORK_INTERFACE, "Ethernet")
                }
                wifiAvailable -> {
                    networkObject.put(NETWORK_INTERFACE, "Wifi")
                }
                else -> {
                    CcuLog.e(TAG_CCU_MIGRATION_UTIL, "No network interfaces available")
                }
            }

            sharedPreferences.edit().putString(BACNET_CONFIGURATION, config.toString()).apply()

            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updated Bacnet network interface: ${sharedPreferences.getString(BACNET_CONFIGURATION, "")}")

        } catch (e: Exception) {
            CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in updateBacnetNetworkInterface $e")
        }
    }



    private fun updateUnoccupiedSetbackMax() {
        try {
            val listOfUnoccupiedPoints = hayStack.readAllEntities("unoccupied and setback and not domainName")
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Total points found ${listOfUnoccupiedPoints.size}")
            listOfUnoccupiedPoints.forEach {
                val unoccupiedPoint = Point.Builder().setHashMap(it).setMaxVal("60").build()
                hayStack.updatePoint(unoccupiedPoint, unoccupiedPoint.id)
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "${unoccupiedPoint.id} Updated unoccupied setback max value")
            }
        } catch (e: Exception) {
            CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error at updateUnoccupiedSetbackMax ${e.message}", e)
        }
    }

    private fun migrateHyperStatSplitFanModeCache() {
        CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Migrating HyperStat Fan Mode Cache to HyperStatSplit Fan Mode Cache")
        CCUHsApi.getInstance().readAllEntities("equip and hyperstatsplit").forEach { equipMap ->

            val hypertStatFanModeCache = a75f.io.logic.bo.building.hyperstat.common.FanModeCacheStorage()
            val fanMode = hypertStatFanModeCache.getFanModeFromCache(equipMap["id"].toString())
            if (fanMode != 0) {
                val splitFanModeCache = a75f.io.logic.bo.building.hyperstatsplit.common.FanModeCacheStorage()
                splitFanModeCache.saveFanModeInCache(equipMap["id"].toString(), fanMode) // Save the fan mode in the HyperStatSplit cache

                hypertStatFanModeCache.removeFanModeFromCache(equipMap["id"].toString()) // Remove the fan mode from the HyperStat cache
            }
        }
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
        CcuLog.d(TAG_CCU_MIGRATION_UTIL,"Clearing OtaCache Preferences")
        try {
            val sharedPreferences: SharedPreferences = Globals.getInstance().applicationContext.getSharedPreferences("otaCache" , Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
        }
        catch (e : Exception) {
            CcuLog.e(TAG_CCU_MIGRATION_UTIL,"Failed to clear OtaCache ${e.printStackTrace()}")
        }
    }

    private fun updatingBackFillDefaultValues(hayStack: CCUHsApi, backFillPoint: HashMap<Any, Any>) {

        val backFillDurationId = backFillPoint["id"].toString()
        val lastUpdatedTimeForBackFillPoint = hayStack.readPointPriorityLatestTime(backFillDurationId)

        if(lastUpdatedTimeForBackFillPoint == null) {
            CcuLog.i(
                TAG_CCU_MIGRATION_UTIL, "lastUpdatedTimeForBackFillPoint not found" +
                    " so Updated the fallback default value for backfill point")
            hayStack.writeDefaultValById(backFillDurationId,24.0)
            return
        }

        val lastUpdatedTime = DateTime.parse(lastUpdatedTimeForBackFillPoint.substring(0, 19))
        if(backFillPoint["createdDateTime"] == null) {
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Updated the fallback default value for backfill point")
            hayStack.writeDefaultValById(backFillDurationId,24.0)
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Updated backfill default value")
            return
        }
        val createdTime =
            DateTime.parse(backFillPoint["createdDateTime"].toString().substring(0,19))
        if (createdTime.equals(lastUpdatedTime)) {
            hayStack.writeDefaultValById(backFillDurationId,24.0)
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Updated backfill default value")
        } else {
            CcuLog.i(
                TAG_CCU_MIGRATION_UTIL,
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
        CcuLog.d(TAG_CCU_MIGRATION_UTIL,"doPostModelMigrationTasks: check isMigrationOngoing $isMigrationOngoing")
        if(isMigrationOngoing) {
            CcuLog.d(TAG_CCU_MIGRATION_UTIL,"Version update detected, performing minCFMPoints' maxVal update")
            updateMinCfmPointMaxVal(Pair(DomainName.minCFMCooling, DomainName.maxCFMCooling))
            updateMinCfmPointMaxVal(Pair(DomainName.minCFMReheating, DomainName.maxCFMReheating))
        }
        isMigrationOngoing = false
    }

    private fun deleteRedundantSetbackPointsFromHnAcbEquips() {
        CcuLog.d(TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips started")
        hayStack.readAllEntities("equip and domainName == \"helionodeActiveChilledBeam\"").forEach { hnAcbEquip ->
                val nonDmDemandResponse = hayStack.readEntity("demand and response and setback and tuner and not domainName and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                if (nonDmDemandResponse.isNotEmpty()) {
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: ${nonDmDemandResponse["id"].toString()}")
                    val dmDemandResponseSetback = hayStack.readHDict("domainName==\"demandResponseSetback\" and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                    if (dmDemandResponseSetback != null && !dmDemandResponseSetback.isEmpty) {
                        hayStack.updatePoint(Point.Builder().setHDict(dmDemandResponseSetback).build() , nonDmDemandResponse["id"].toString())
                        CcuLog.d(TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: demandResponse ${nonDmDemandResponse["id"].toString()} updated")
                        hayStack.deleteWritablePoint(dmDemandResponseSetback["id"].toString()) // delete the duplicate point
                    }
                } else {
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: no demandResponse found")
                }
                val nonDmAutoAway = hayStack.readEntity("auto and away and setback and not domainName and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                if (nonDmAutoAway.isNotEmpty()) {
                    val dmAutoAway = hayStack.readHDict("domainName==\"autoAway\" and equipRef==\"${hnAcbEquip["id"].toString()}\"")
                    if (dmAutoAway != null && !dmAutoAway.isEmpty) {
                        hayStack.updatePoint(Point.Builder().setHDict(dmAutoAway).build() , nonDmAutoAway["id"].toString())
                        CcuLog.d(TAG_CCU_MIGRATION_UTIL , "deleteRedundantSetbackPointsFromHnAcbEquips: autoAway ${nonDmAutoAway["id"].toString()} updated")
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
        val vavEquips = hayStack.readAllEntities("equip and zone and vav and $not_external_model_query")
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
        val dabEquips = hayStack.readAllEntities("equip and zone and dab and not dualDuct and $not_external_model_query")
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
        val dabEquips = hayStack.readAllEntities("equip and system and dab and $not_external_model_query")
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
                (it["profile"].toString() == "SYSTEM_DAB_ANALOG_RTU" ||
                        it["domainName"].toString() == "dabFullyModulatingAhu") -> {
                    migrateDabFullyModulatingSystemProfile(it["id"].toString(), equipBuilder, site, deviceModel, deviceDis, equipHashMap = it)
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
        val vavEquips = hayStack.readAllEntities("equip and system and vav and $not_external_model_query")
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

    private fun migrateDabFullyModulatingSystemProfile (equipId : String, equipBuilder: ProfileEquipBuilder, site: Site,
                                                  deviceModel : SeventyFiveFDeviceDirective, deviceDis : String, equipHashMap: HashMap<Any, Any>) {
        CcuLog.i(Domain.LOG_TAG, "DabFullyModulatingSystemProfile equipID: $equipId")
        val model = ModelLoader.getDabModulatingRtuModelDef()
        val equipDis = "${site.displayName}-${model.name}"
        val profileConfig = ModulatingRtuProfileConfig(model as SeventyFiveFProfileDirective)
        equipBuilder.doCutOverMigration(equipId, model,
            equipDis, DabFullyModulatingRtuCutOverMapping.entries , profileConfig.getDefaultConfiguration()
            ,isSystem = true, equipHashMap)

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
        val hssEquips = hayStack.readAllEntities("equip and hyperstatsplit and cpu and $not_external_model_query")
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
                device["id"].toString(),
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
        val oao = hayStack.readEntity("equip and oao and $not_external_model_query")
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
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "currentAppVersion: $currentAppVersion, migrationVersion: $migrationVersion")
            if (currentAppVersion != migrationVersion) {
                Domain.writeDefaultValByDomain(DomainName.migrationVersion, currentAppVersion)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

     fun temperatureModeMigration() {
        if (!PreferenceUtil.isTempModeMigrationRequired()) {
            CcuLog.i(TAG_CCU_MIGRATION_UTIL,"Temperature mode migration Initiated")
            writeValAtLevelByDomain(DomainName.temperatureMode, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL,1.0)
            PreferenceUtil.setTempModeMigrationNotRequired()
        }
    }

    private fun migrateAnalogTypeForVavAnalog1In() {
        CcuLog.i(TAG_CCU_MIGRATION_UTIL,"Migrate Analog Type for VAV Analog1In!!")
        hayStack.readAllEntities("equip and zone and vav and domainName").forEach { equip ->
            val device = hayStack.read("device and equipRef == \"${equip["id"]}\"")
            val analog1In = hayStack.read("domainName == \"${DomainName.analog1In}\" and deviceRef == \"${device["id"]}\"")
            val damperType = hayStack.readPointPriorityValByQuery("domainName == \"${DomainName.damperType}\" and equipRef == \"${equip["id"]}\"")
            if (analog1In.isNotEmpty()) {
                val analog1InPoint = RawPoint.Builder().setHDict(hayStack.readHDictById(analog1In["id"].toString())).build()
                analog1InPoint.type = getDamperTypeString(damperType.toInt())
                hayStack.updatePoint(analog1InPoint , analog1In["id"].toString())
                CcuLog.d(TAG_CCU_MIGRATION_UTIL,"Analog1In type updated for device ${device["dis"]}")
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
        CcuLog.i(TAG_CCU_MIGRATION_UTIL,"Migrate His Interpolate for Device Points!!")
        val devices= hayStack.readAllEntities("device and domainName and not modbus and not ccu") // DM integrated devices
        devices.forEach { device ->
            CcuLog.d(TAG_CCU_MIGRATION_UTIL,"device id ${device["id"]} device name ${device["dis"]}")

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


    private fun doTiCutOverMigration() {
        val haystack = CCUHsApi.getInstance()

        val tiEquip = haystack.readEntity("equip and ti and $not_external_model_query")
        if (tiEquip.isEmpty() || (tiEquip.isNotEmpty() && tiEquip.containsKey("domainName"))) {
            CcuLog.i(Domain.LOG_TAG, "TI equip is not found or TI equip is already DM migrated")
            return
        }

        if (tiEquip.isNotEmpty()) {
            val model = ModelLoader.getTIModel() as SeventyFiveFProfileDirective
            val deviceModel = ModelLoader.getTIDeviceModel() as SeventyFiveFDeviceDirective
            val deviceBuilder = DeviceBuilder(haystack, EntityMapper(model))
            val profileType = ProfileType.TEMP_INFLUENCE
            val equipBuilder = ProfileEquipBuilder(hayStack)

            val nodeAddress = tiEquip["group"].toString()
            val equipDis = "${hayStack.siteName}-${model.name}-$nodeAddress"
            val deviceDis = "${hayStack.siteName}-${deviceModel.name}-$nodeAddress"
            val device = hayStack.readEntity("device and addr == \"$nodeAddress\"")
            val configuration = TIConfiguration(
                nodeAddress.toInt(), NodeType.CONTROL_MOTE.name, 0,
                tiEquip["roomRef"].toString(), tiEquip["floorRef"].toString(),
                profileType, model
            ).getActiveConfiguration()
            equipBuilder.doCutOverMigration(
                tiEquip["id"].toString(),
                model,
                equipDis,
                TiCutOverMapping.entries,
                configuration,
                equipHashMap = tiEquip
            )

            deviceBuilder.doCutOverMigration(
                device["id"].toString(),
                deviceModel,
                deviceDis,
                TiCutOverMapping.tiDeviceMapping,
                configuration
            )
            addDomainEquips(hayStack)
            val tiDomainEquip = TIEquip(tiEquip["id"].toString())
            tiDomainEquip.temperatureOffset.writeDefaultVal(tiDomainEquip.temperatureOffset.readDefaultVal() / 10)

            TIConfiguration(
                nodeAddress.toInt(), NodeType.CONTROL_MOTE.name, 0,
                tiEquip["roomRef"].toString(), tiEquip["floorRef"].toString(),
                profileType, model
            ).getActiveConfiguration()
                .updatePhysicalPointRef(tiEquip["id"].toString(), device["id"].toString())
        }
    }


    fun doHSCPUDMMigration() {
        val hyperStatCPUEquip = hayStack.readAllEntities("equip and hyperstat and cpu and $not_external_model_query")
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
                    device["id"].toString(),
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

    private fun doHSHPUDMMigration() {

        val hyperStatHPUEquip = hayStack.readAllEntities("equip and hyperstat and hpu and $not_external_model_query")
            .filter { it["domainName"] == null }
            .toList()
        CcuLog.d(L.TAG_CCU_HSHPU, "HyperStat HPU Equip Migration list of equips $hyperStatHPUEquip")
        if (hyperStatHPUEquip.isNotEmpty()) {
            val model = ModelLoader.getHyperStatHpuModel()
            val deviceModel =
                ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
            val deviceBuilder =
                DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
            val profileType = ProfileType.HYPERSTAT_HEAT_PUMP_UNIT
            val equipBuilder = ProfileEquipBuilder(hayStack)
            hyperStatHPUEquip.forEach {
                CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")
                val equipDis = "${hayStack.siteName}-${model.name}-${it["group"]}"
                val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${it["group"]}"
                val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
                val profileConfiguration = HpuConfiguration(
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
                    HyperStatV2EquipCutoverMapping.getHPUEntries(),
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
        addDomainEquips(hayStack)
    }

    fun updateBacnetProperties(hayStack: CCUHsApi) {
        // migration for system equip and points
        CcuLog.d(TAG_CCU_MIGRATION_UTIL,"updateBacnetProperties method started!!!")
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
        CcuLog.d(TAG_CCU_MIGRATION_UTIL,"executing updateMinCfmPointMaxVal")
        hayStack.readAllEntities(" point and zone and config and domainName == \"${minMaxCfmDomainNames.first}\" ").forEach { minCfmMap ->
            val maxCfmVal = hayStack.readPointPriorityValByQuery(
                " domainName == \"${minMaxCfmDomainNames.second}\" and equipRef == \"${
                    minCfmMap["equipRef"]
                }\" "
            )
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Point value for ${minMaxCfmDomainNames.second}: $maxCfmVal")
            if (maxCfmVal != minCfmMap["maxVal"].toString().toDouble()) {
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating point for ${minMaxCfmDomainNames.first}")
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
        val sseEquips = hayStack.readAllEntities("equip and zone and sse and $not_external_model_query")
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
                updateBacnetProperties(CCUHsApi.getInstance())
                PreferenceUtil.setBacnetIdMigrationDone()
            } catch (e : Exception) {
                //For now, we make sure it does not stop other migrations even if this fails.
                e.printStackTrace()
                CcuLog.e(TAG_CCU_MIGRATION_UTIL, "Error in migrateBacnetIdForVavDevices $e")
            }
        }
    }

    private fun doHSMonitoringDMMigration() {
        val hyperStatMonitoringEquip =
            hayStack.readAllEntities("equip and hyperstat and monitoring and $not_external_model_query")
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
                migaratePm25AndPm10ToDefaultValue(model, hayStack, profileConfiguration, it["id"].toString())
            }
        }
    }

    private fun migaratePm25AndPm10ToDefaultValue(
        model: SeventyFiveFProfileDirective,
        hayStack: CCUHsApi,
        profileConfiguration: MonitoringConfiguration,
        equipRef: String
    ) {
        val pm10DefaultVal = profileConfiguration.getDefaultValConfig(DomainName.pm10Target, model)
        val pm25Target = profileConfiguration.getDefaultValConfig(DomainName.pm25Target, model)
        val co2Target = profileConfiguration.getDefaultValConfig(DomainName.co2Target, model)
        hayStack.writeDefaultVal("point and domainName == \"${DomainName.pm10Target}\" " +
                "and equipRef == \"$equipRef\"", pm10DefaultVal.currentVal)

        hayStack.writeDefaultVal("point and domainName == \"${DomainName.pm25Target}\" " +
                "and equipRef == \"$equipRef\"", pm25Target.currentVal)

        hayStack.writeDefaultVal("point and domainName == \"${DomainName.co2Target}\" " +
                "and equipRef == \"$equipRef\"", co2Target.currentVal)

        CcuLog.i(Domain.LOG_TAG, "PM10 and PM25 points are migrated to default values "+
                "PM10: ${pm10DefaultVal.currentVal} and PM25: ${pm25Target.currentVal}")

    }

    private fun doHSPipe2DMMigration() {
        val hyperStatPipe2Equip =
            hayStack.readAllEntities("equip and hyperstat and pipe2 and $not_external_model_query")
                .filter { it["domainName"] == null }
                .toList()

        val model = ModelLoader.getHyperStatPipe2Model()
        val deviceModel =
            ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
        val deviceBuilder =
            DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
        val profileType = ProfileType.HYPERSTAT_TWO_PIPE_FCU

        if (hyperStatPipe2Equip.isNotEmpty()) {
            val equipBuilder = ProfileEquipBuilder(hayStack)
            hyperStatPipe2Equip.forEach {
                CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")
                val equipDis = "${hayStack.siteName}-${model.name}-${it["group"]}"
                val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${it["group"]}"
                val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
                val profileConfiguration = Pipe2Configuration(
                    Integer.parseInt(it["group"].toString()),
                    NodeType.HYPER_STAT.name,
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
                    HyperStatV2EquipCutoverMapping.getPipe2Entries(),
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
        val otnEquips = hayStack.readAllEntities("equip and zone and otn and $not_external_model_query")
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


    private fun removeRedundantBacnetSettingPoints() {
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Removing redundant bacnet setting points")
        hayStack.readAllEntities("bacnet and setting and point").forEach {
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "bacnet setting points found: ${it["dis"]}. Deleting the redundant point")
            hayStack.deleteEntity(it["id"].toString())
        }
    }

    private fun prangePointMigration() {
        CcuLog.i(TAG_CCU_MIGRATION_UTIL, "vavAirflowCFMProportionalRange migration is started")
        CCUHsApi.getInstance().readAllEntities("equip and (vav or acb) and zone")
            .forEach { equipMap ->
                val vavAirflowCFMProportionalRange = Domain.readPointForEquip(
                    DomainName.vavAirflowCFMProportionalRange,
                    equipMap["id"].toString()
                )
                if (vavAirflowCFMProportionalRange.isNotEmpty()) {
                    // read level 8 val
                    val value = CCUHsApi.getInstance()
                        .readDefaultVal("point and domainName == \"${DomainName.vavAirflowCFMProportionalRange}\" and equipRef == \"${equipMap["id"].toString()}\"")
                    // write to level 10
                    CCUHsApi.getInstance().writeTunerPointForCcuUser(
                        vavAirflowCFMProportionalRange["id"].toString(),
                        10,
                        value,
                        0,
                        "updating vavAirflowCFMProportionalRange value from level 8 to 10"
                    )
                    // clear level 8 val
                    hayStack.clearPointArrayLevel(
                        vavAirflowCFMProportionalRange["id"].toString(), 8, false
                    )
                    CcuLog.i(
                        TAG_CCU_MIGRATION_UTIL,
                        "updated value for point ${vavAirflowCFMProportionalRange["id"]} to 10"
                    )
                }
            }
        CcuLog.i(TAG_CCU_MIGRATION_UTIL, "vavAirflowCFMProportionalRange migration is complete")
    }

    private fun correctPointRefForRelay2SeriesFan() {
        CcuLog.d(TAG_CCU_MIGRATION_UTIL,"correctPointRefForRelay2SeriesFan execution started")
        hayStack.readAllEntities("equip and ( profile == \"${ ProfileType.VAV_SERIES_FAN }\" or profile == \"${ ProfileType.VAV_PARALLEL_FAN }\" )  ").forEach { vavSeriesEquipMap ->
            CcuLog.d(TAG_CCU_MIGRATION_UTIL,"equipName: ${vavSeriesEquipMap["dis"]} and id: ${vavSeriesEquipMap["id"]} and domainName: ${vavSeriesEquipMap["domainName"]} ")
            hayStack.readId("device and equipRef == \"${ vavSeriesEquipMap["id"] }\" ")?.let { deviceId ->
                val logicalPointId = hayStack.readId("(domainName == \"${ DomainName.seriesFanCmd }\" or domainName == \"${ DomainName.parallelFanCmd }\") and equipRef == \"${ vavSeriesEquipMap["id"] }\"")
                val relay2PhyPointDict = hayStack.readHDict("portEnabled and domainName==\"${DomainName.relay2}\" and deviceRef== \"${deviceId}\" ")
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "\tlogicalPointId: $logicalPointId and current pointRef: ${relay2PhyPointDict.get("pointRef", false)}")
                if (relay2PhyPointDict.get("pointRef", false) != null && relay2PhyPointDict.get("pointRef").toString()
                    .replace("@", "") != logicalPointId?.replace("@", "")
                ) {
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL,"\tIncorrect mapping to logical point.")
                    val relay2PhyPoint = RawPoint.Builder().setHDict(relay2PhyPointDict).build()
                    relay2PhyPoint.pointRef = logicalPointId
                    hayStack.updatePoint(relay2PhyPoint, relay2PhyPoint.id)
                }
            }
        }
        CcuLog.d(TAG_CCU_MIGRATION_UTIL,"correctPointRefForRelay2SeriesFan execution completed")
    }

    private fun removeRedundantSmartnodeHelionodePointsForSse() {
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "removeRedundantSmartnodeHelionodePointsForSse started")
        hayStack.readAllEntities("equip and (domainName == \"${ DomainName.smartnodeSSE }\" or domainName == \"${ DomainName.helionodeSSE }\")").forEach { sseEquip ->
            CcuLog.d(TAG_CCU_MIGRATION_UTIL,"sseEquip name: ${sseEquip["domainName"]}, id: ${sseEquip["id"]}, dis: ${sseEquip["dis"]}")
            hayStack.readId("device and equipRef == \"${ sseEquip["id"]?.toString() }\"")?.let { sseDeviceId ->
                listOf(DomainName.sensorEnergyMeter, DomainName.sensorNo2, DomainName.sensorPm10).forEach { redundantDomainName ->
                    val redundantList = hayStack.readAllEntities("point and domainName == \"${ redundantDomainName }\" and deviceRef == \"${ sseDeviceId }\"")
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL, "redundantDomainName= ${redundantDomainName}, redundantList size: ${redundantList.size}")
                    if(redundantList.size == 2) {
                        CcuLog.d(TAG_CCU_MIGRATION_UTIL,"removing a random redundant point: ${redundantList[0]}")
                        hayStack.deleteEntity(redundantList[0]["id"].toString())
                    }
                }
                hayStack.readId("point and (port == \"SENSOR_VOC\" or port == \"vocSensor\") and not domainName and deviceRef == \"${ sseDeviceId }\"")?.let { vocSensorId ->
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL,"Found non dm sensorVOC point id: ${vocSensorId}")
                    hayStack.deleteEntity(vocSensorId)
                }
            }
        }
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "removeRedundantSmartnodeHelionodePointsForSse completed")
    }

    private fun updateHisInterpolate() {
        hayStack.readAllHDictByQuery("ota and status and hisInterpolate == \""+"linear"+"\"")
            .forEach { point ->
                hayStack.updatePoint(
                    Point.Builder().setHDict(point).setHisInterpolate("cov").build(),
                    point["id"].toString()
                )
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updated ${point["dis"]} hisInterpolate for point ${point["id"]}  to cov")
            }
    }

    private fun recoverSourceModelTagsForOao() {
        CcuLog.d(
            TAG_CCU_MIGRATION_UTIL,
            "MigrationHandler.logic recoverSourceModelTagsForOao started"
        )
        hayStack.readAllHDictByQuery("equip and domainName and not sourceModel")
            .forEach { incompleteEquipDict ->
                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL,
                    "\tEquip found without source model: ${incompleteEquipDict.id()} for point" +
                            "with displayName: ${incompleteEquipDict.dis()}"
                )
                val oaoEquipBuilder = Equip.Builder().setHDict(incompleteEquipDict)
                oaoEquipBuilder.addTag("sourceModel", HStr.make(MODEL_SN_OAO))
                DiffManger(hayStack.context).getOldModelMetaList(
                    hayStack.context.filesDir.absolutePath + "/models",
                    hayStack.context.getSharedPreferences(
                        Globals.DOMAIN_MODEL_SF,
                        Context.MODE_PRIVATE
                    )
                )
                    .find { it.modelId == MODEL_SN_OAO }?.let { oaoModelMeta ->
                        oaoEquipBuilder.addTag(
                            "sourceModelVersion",
                            HStr.make(oaoModelMeta.version.toString())
                        )
                    }
                hayStack.updateEquip(oaoEquipBuilder.build(), incompleteEquipDict["id"].toString())
            }
        CcuLog.d(
            TAG_CCU_MIGRATION_UTIL,
            "MigrationHandler.logic recoverSourceModelTagsForOao ended"
        )
    }

    private fun correctAhuRefForBypassDamperAndCcuConfig() {
        CcuLog.d(
            TAG_CCU_MIGRATION_UTIL,
            "MigrationHandler.logic correctAhuRefForBypassDamper started"
        )
        hayStack.readId(CommonQueries.SYSTEM_PROFILE)
            ?.let { systemProfileEquipId ->
                hayStack.readAllHDictByQuery(
                    "equip and (domainName == \"${DomainName.smartnodeBypassDamper}\" or " +
                            "domainName == \"${DomainName.ccuConfiguration}\")"
                ).forEach { equipDict ->
                    CcuLog.d(
                        TAG_CCU_MIGRATION_UTIL,
                        "MigrationHandler.correctAhuRef iterating for entity with" +
                                " dis: ${equipDict.dis()} systemEquipId: $systemProfileEquipId and ahuRef: ${
                                    equipDict.get(
                                        "ahuRef",
                                        false
                                    )
                                } and equipId: ${equipDict.id()}"
                    )
                    if (!equipDict.get("ahuRef", false)?.toString().equals(systemProfileEquipId)) {
                        CcuLog.d(
                            TAG_CCU_MIGRATION_UTIL,
                            "MigrationHandler.correctAhuRef ahuRef discrepancy found"
                        )
                        val equipBuilder = Equip.Builder().setHDict(equipDict)
                        equipBuilder.setAhuRef(systemProfileEquipId)
                        if (equipDict.get("domainName", false)
                                ?.toString() == (DomainName.ccuConfiguration)
                        ) {
                            equipBuilder.setGatewayRef(systemProfileEquipId)
                        }
                        hayStack.updateEquip(equipBuilder.build(), equipDict.id().toString())
                    }
                }
            }
        CcuLog.d(
            TAG_CCU_MIGRATION_UTIL,
            "MigrationHandler.logic correctAhuRefForBypassDamper ended"
        )
    }

    fun updateDataType() {
        val dabPoints = listOf(
            "damper1Shape",
            "damper1Cmd",
            "dischargeAirTemp2",
            "damper1Type",
            "damper2Size",
            "damper2Type",
            "damper2Shape",
            "damper2Cmd",
            "damper1Size",
            "dischargeAirTemp1",
            "normalizedDamper1Cmd",
            "normalizedDamper2Cmd"
        )
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating corrupted dab points Started")
        val dabEquips = hayStack.readAllEntities("equip and dab and zone and domainName")
        dabEquips.forEach {
            updatePoints(CCUHsApi.getInstance(), dabPoints, it)
        }
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating corrupted dab points ends")


        val hyperstatPoints = listOf(
            "fanOutCoolingStage1",
            "heatingStage2",
            "analog2InputEnable",
            "analog3MinHeating",
            "analog2OutputAssociation",
            "analog3OutputEnable",
            "heatingStage1",
            "analog1FanMedium",
            "analog1MinCooling",
            "analog1MaxCooling",
            "coolingStage2",
            "analog2FanLow",
            "analog1InputEnable",
            "analog3FanHigh",
            "analog2OutputEnable",
            "analog2FanMedium",
            "analog3OutputAssociation",
            "analog2InputAssociation",
            "coolingStage1",
            "analog3FanLow",
            "analog1InputAssociation",
            "analog3MaxHeating",
            "analog1FanHigh",
            "analog2MinLinearFanSpeed",
            "fanOutHeatingStage1",
            "analog1OutputAssociation",
            "analog2FanHigh",
            "fanOutHeatingStage2",
            "fanOutCoolingStage2",
            "analog2MaxLinearFanSpeed",
            "analog1OutputEnable",
            "analog1FanLow",
            "analog3FanMedium",
            "compressorStage1",
            "compressorStage2",
            "compressorStage3",
            "auxHeatingStage1",
            "auxHeatingStage2",
            "auxHeating1Activate",
            "auxHeating2Activate",
            "auxHeatingStage1",
            "auxHeatingStage2",
            "auxHeating1Activate",
            "auxHeating2Activate",
            "coolingStage3",
            "fanOutCoolingStage3",
            "heatingStage3",
            "fanOutHeatingStage3"
        )

        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating corrupted hs points Started")
        val cupOrHpuOr2Pipe = hayStack.readAllEntities("equip and (cpu or hpu or pipe2) and hyperstat and zone and domainName")
        cupOrHpuOr2Pipe.forEach {
            updatePoints(CCUHsApi.getInstance(), hyperstatPoints, it)
        }
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating corrupted hs points ended")

        val ssePoints = listOf(
            "standaloneStage1Hysteresis",
            "heatingStage1",
            "coolingStage1",
            "analog1InputEnable",
            "standaloneStage1CoolingUpperOffset",
            "standaloneStage1HeatingUpperOffset",
            "standaloneStage1CoolingLowerOffset",
            "fanStage1",
            "standaloneStage1HeatingLowerOffset"
        )

        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating corrupted sse points Started")
        val sseEquips = hayStack.readAllEntities("equip and sse and zone and domainName")
        sseEquips.forEach {
            updatePoints(CCUHsApi.getInstance(), ssePoints, it)
        }
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating corrupted sse points ends")
    }

    private fun updatePoints(
        hayStack: CCUHsApi,
        corruptedPoints: List<String>,
        equip: java.util.HashMap<Any, Any>
    ) {
        if(equip.size == 0) return
        val site = hayStack.site
        val profileEquipBuilder = ProfileEquipBuilder(hayStack)
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

        val points = hayStack.readAllEntities("point and equipRef == \"$equipId\"")

        points.forEach { point ->

            if (!corruptedPoints.contains(point["domainName"].toString())) {
                return@forEach
            }

            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "prev point: $point")

            equipModel.points.find { it.domainName == point["domainName"].toString() }
                ?.let { pointDef ->
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

            CcuLog.d(
                TAG_CCU_MIGRATION_UTIL,
                "updated point: ${hayStack.readEntity("point and id == ${point["id"]} ")}"
            )
        }
    }

    private fun updatePLCPhysicalPoints() {
        val hayStack = CCUHsApi.getInstance()
        val plcEquips = hayStack.readAllEntities("pid and equip and domainName")
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating PLC physical points. count: "+plcEquips.size)
        plcEquips.forEach { plcEquip ->
            val address = plcEquip["group"].toString()
            val isHelioNode = plcEquip.containsKey("helionode")
            val model =
                if (isHelioNode) ModelLoader.getHelioNodePidModel() as SeventyFiveFProfileDirective
                else ModelLoader.getSmartNodePidModel() as SeventyFiveFProfileDirective
            val config = PlcProfileConfig(
                Integer.parseInt(plcEquip["group"].toString()),
                if (isHelioNode) NodeType.HELIO_NODE.name else NodeType.SMART_NODE.name,
                0,
                plcEquip["roomRef"].toString(),
                plcEquip["floorRef"].toString(),
                ProfileType.PLC,
                model
            ).getActiveConfiguration()
            val entityMapper = EntityMapper(model)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            val device = hayStack.readEntity("device and addr == \"$address\"")
            val deviceModel =
                getModelForDomainName(device["domainName"].toString()) as SeventyFiveFDeviceDirective

            val nativeSensorTypePoint =
                hayStack.readEntity("native and point and config and equipRef == \"" + plcEquip["id"] + "\"");
            val nativeSensorTypeValue =
                hayStack.readDefaultValById(nativeSensorTypePoint["id"].toString())

            // If user is selected native sensor type as VOC, then we are updating the enum value to index - 1
            if (nativeSensorTypeValue >= 7) {
                config.nativeSensorType.currentVal = nativeSensorTypeValue - 1
                //Update custom configurations which are done outside of the model
                addBaseProfileConfig(DomainName.nativeSensorType, config, model)

                val equipBuilder = ProfileEquipBuilder(hayStack)
                equipBuilder.updateEquipAndPoints(
                    config,
                    model,
                    plcEquip["siteRef"].toString(),
                    plcEquip["dis"].toString(), true
                )
            }

            config.updateTypeForAnalog1Out(config)
            config.updatePortConfiguration(hayStack, config, deviceBuilder, deviceModel)
        }
        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Updating PLC physical points completed")
    }

    /*
    * At this point, migration of addressBand point is happening from SettingPoint to Point from version 3.1.x
    * so if any CCU comes from above 2.18.x there will be DM-DM migration happening for addressBand point
    * so initialising addressBand point at level 8 once is necessary.
    * Below function handle that part
    */
    fun initAddressBand() {
        if(!PreferenceUtil.isAddressBandInitCompleted()) {
            CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Initialising addressBand point")
            var addressBandVal = L.ccu().addressBand.toString()
            val ccuEquip = hayStack.readEntityByDomainName(DomainName.ccuConfiguration)
            if(ccuEquip.isEmpty()) {
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "CCU Equip not found")
                return
            }
            // if the device is not paired the addressBand is coming has 0 ,when we migrated from 2.20.X to 3.0.7
            // added the fix to fetch the value from point val if point val is not there ,changing it ot 1000 by default
            if (addressBandVal.toInt() == 0) {
                addressBandVal = 1000.toString()
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, " updated fallback address band value")
            }

            val addressBandQuery = "point and domainName == \"${DomainName.addressBand}\" and equipRef == \"${ccuEquip["id"]}\""

            val addressBandQueryForDuplicatePointWithSameDomainName = "point and domainName == \"${DomainName.addressBand}\""
            val duplicateAddressBandPoints = hayStack.readAllEntities(addressBandQueryForDuplicatePointWithSameDomainName)

            if(duplicateAddressBandPoints.size > 1) {
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Duplicate addressBand points found")
                duplicateAddressBandPoints.forEach {
                    if(!it.containsKey("equipRef") ||  it["equipRef"].toString() != ccuEquip["id"].toString()) {
                        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Deleting duplicate addressBand point with id: ${it["id"]}")
                        hayStack.deleteEntity(it["id"].toString())
                    }
                }
            }

            val duplicateAddressBandPointsPostDeletion = hayStack.readAllEntities(addressBandQueryForDuplicatePointWithSameDomainName)

            if(duplicateAddressBandPointsPostDeletion.size > 1) {
                CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Duplicate addressBand points found")
                duplicateAddressBandPointsPostDeletion.drop(1).forEach {
                    CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Deleting duplicate addressBand point except one ," +
                            " Deleted point id: ${it["id"]}")
                    hayStack.deleteEntity(it["id"].toString())
                }
            }

            CCUBaseConfigurationMigrationHandler().deleteOldAddressBandPoint(hayStack)

            val addressBandPoint = hayStack.readEntity(addressBandQuery)
            if(addressBandPoint.isEmpty()) {
                CCUBaseConfigurationBuilder(hayStack).createAddressBandPoint(ccuEquip)
            }

            hayStack.writeDefaultVal(addressBandQuery, addressBandVal)
            PreferenceUtil.setAddressBandInitCompleted()
            L.ccu().addressBand = if (addressBandVal == null) 1000 else addressBandVal.toShort()
            CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Initialising addressBand point completed")
        }
    }

    private fun updateRelay2Port(ccuHsApi: CCUHsApi) {
        CcuLog.i(TAG_CCU_MIGRATION_UTIL, "updateRelay2Port portEnabled status")

        val vavEquips = ccuHsApi.readAllEntities("equip and vav and not system and not acb")

        vavEquips.forEach { equipMap ->
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Migrate -> " + equipMap["dis"].toString())
            val equip = Equip.Builder().setHashMap(equipMap).build()
            val domainEquip = VavEquip(equip.id)
            val address: Short = equip.group.toShort()
            val devicePorts = DeviceUtil.getPortsForDevice(address, ccuHsApi)
            devicePorts?.forEach { port ->
                val reheatType = domainEquip.reheatType.readPriorityVal().toInt()
                CcuLog.i(
                    TAG_CCU_MIGRATION_UTIL, "id: ${port.id}  port domainName: ${port.domainName}" +
                            " pointRef: ${port.pointRef} enabled: ${port.enabled} profile: ${equip.profile}: reheatType: $reheatType"
                )

                if (port.domainName == DomainName.relay2
                    && equip.profile != null && equip.profile.equals(ProfileType.VAV_REHEAT.name)
                    && port.pointRef != null && !port.enabled && !port.markers.contains("unused")
                ) {
                    port.enabled = true
                    if (reheatType != ReheatType.TwoStage.ordinal + 1) { // if reheat is not using stage2 then disable relay2
                        port.enabled = false
                        port.pointRef = null
                    }
                    CcuLog.i(
                        TAG_CCU_MIGRATION_UTIL,
                        "id: ${port.id} Port: ${port.displayName} is updated with reheatType is $reheatType and is enabled? ${port.enabled}"
                    )
                    ccuHsApi.updatePoint(port, port.id)
                }

            }
        }
    }

    private fun createVocSensorPointHyperStatEquip() {

        val hyperStatEquip =
            hayStack.readAllEntities("equip and (domainName==\"${DomainName.hyperstatCPU}\" or domainName==\"${DomainName.hyperstat2PFCU}\" or domainName==\"${DomainName.hyperstatHPU}\")")
        hyperStatEquip.forEachIndexed { _, equip ->
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, " creating voc sensor point hyper stat equip: $equip")
            val hyperStatDevice =
                hayStack.readEntity("device and equipRef ==\"" + equip["id"].toString() + "\" and  domainName == \"" + DomainName.hyperstatDevice + "\"")

            val deviceModel = ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
            val equipModel = getModelForDomainName(equip["domainName"].toString()) as SeventyFiveFProfileDirective
            val profileConfig: ProfileConfiguration = getConfiguration(equip["id"].toString()) as ProfileConfiguration
            val vocSensorPoint = hayStack.readEntity("domainName == \"" + DomainName.vocSensor + "\" and  deviceRef == \"" + hyperStatDevice["id"].toString() + "\"")

            if (vocSensorPoint.isEmpty()) {
                val device = Device.Builder().setHashMap(hyperStatDevice).build()
                createDomainDevicePoint(
                    deviceModel,
                    equipModel,
                    profileConfig,
                    device,
                    device.displayName,
                    DomainName.vocSensor
                )
            } else {
                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL,
                    " Voc sensor point already exists for hyper stat equip: ${equip["dis"].toString()}"
                )
            }
        }

    }

    private fun createDabMissingPoints(){
        val site = hayStack.site
        val dabSystemEquip =hayStack.readEntity(
            "equip and system and domainName == \"${DomainName.dabFullyModulatingAhu}\" or domainName == \"${DomainName.dabStagedRtu}\" or domainName==\"${DomainName.dabStagedRtuVfdFan}\"")

        if(dabSystemEquip.isNotEmpty()) {
            val model = ModelLoader.getDabModulatingRtuModelDef()
            val equipDis = "${site?.displayName}-${model.name}"
            val profileConfig =
                ModulatingRtuProfileConfig(model as SeventyFiveFProfileDirective).getActiveConfiguration()


            val relay7ToggleVal = hayStack.readDefaultVal(
                "(config and relay7 and enabled) or (domainName == \"" + DomainName.relay7OutputEnable + "\"" +
                        " and equipRef == \"" + dabSystemEquip["id"].toString() + "\")"
            )

            val humidifier = hayStack.readEntity(
                "point and humidifier and equipRef == \"" + dabSystemEquip["id"].toString() + "\""
            )

            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "humidifier map - $humidifier")
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "relay7ToggleVal  - $relay7ToggleVal")

            if (humidifier.isNotEmpty() && !humidifier.containsKey("domainName") && relay7ToggleVal > 0) {
                val profileEquipBuilder = ProfileEquipBuilder(hayStack)
                val modelPoint =
                    model.points.find { it.domainName.equals(DomainName.humidifierEnable, true) }
                if (modelPoint != null) {
                    profileEquipBuilder.updatePoint(
                        PointBuilderConfig(
                            modelPoint,
                            profileConfig,
                            dabSystemEquip["id"].toString(),
                            site!!.id,
                            site.tz,
                            equipDis
                        ), humidifier
                    )
                }
            }

            if (relay7ToggleVal == 0.0 && humidifier.isNotEmpty()) {
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "deleting humidifier point")
                hayStack.deleteEntityTree(humidifier["id"].toString())
            } else {
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "humidifier point not found to delete")
            }

            val dabReheatRelayActivationHysteresis = hayStack.readEntity(
                "domainName == \"" + DomainName.dabReheatRelayActivationHysteresis + "\"" +
                        " and equipRef == \"" + dabSystemEquip["id"].toString() + "\""
            )

            if (dabReheatRelayActivationHysteresis.isEmpty()) {
                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL, "Creating point - dabReheatRelayActivationHysteresis"
                )
                Domain.createDomainPoint(
                    model,
                    profileConfig,
                    dabSystemEquip["id"].toString(),
                    CCUHsApi.getInstance().site!!.id,
                    CCUHsApi.getInstance().site!!.tz,
                    equipDis,
                    DomainName.dabReheatRelayActivationHysteresis
                )
            }
            if (dabSystemEquip["domainName"] == DomainName.dabFullyModulatingAhu) {

                val chilledWaterIntegralKFactor = hayStack.readEntity(
                    "domainName == \"" + DomainName.chilledWaterIntegralKFactor + "\"" +
                            " and equipRef == \"" + dabSystemEquip["id"].toString() + "\""
                )

                if (chilledWaterIntegralKFactor.isEmpty()) {
                    CcuLog.d(
                        TAG_CCU_MIGRATION_UTIL, "Creating point - chilledWaterIntegralKFactor"
                    )
                    Domain.createDomainPoint(
                        model,
                        profileConfig,
                        dabSystemEquip["id"].toString(),
                        CCUHsApi.getInstance().site!!.id,
                        CCUHsApi.getInstance().site!!.tz,
                        equipDis,
                        DomainName.chilledWaterIntegralKFactor
                    )
                }
            }
        }
    }

    private fun updateCCUConfigEquipProfileType() {
        val ccuName = CCUHsApi.getInstance().ccuName
        val ccuEquip = hayStack.readEntityByDomainName(DomainName.ccuConfiguration)

        if (ccuEquip.size == 0) {
            CcuLog.e(TAG_CCU_MIGRATION_UTIL, "CCU Configuration Equip not found")
            return
        }
        if (ccuName == null) {
            CcuLog.e(TAG_CCU_MIGRATION_UTIL, "CCU name is null")
            return
        }
        if (ccuEquip.containsKey(Tags.PROFILE)) {
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "CCU Configuration Equip already has profile")
            return
        }
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        val ccuEquipId = ccuEquip["id"].toString()

        val ccuConfigEquip = CCUBaseConfigurationBuilder(hayStack).getCcuEquip(ccuName)

        systemEquip[Tags.ID]?.toString()?.let { systemEquipId ->
            ccuConfigEquip.gatewayRef = systemEquipId
            ccuConfigEquip.ahuRef = systemEquipId
        }

        ccuConfigEquip.lastModifiedDateTime = HDateTime.make(System.currentTimeMillis())
        ccuConfigEquip.id = ccuEquipId

        hayStack.updateEquip(ccuConfigEquip, ccuConfigEquip.id)
        CcuLog.i(TAG_CCU_MIGRATION_UTIL, "CCU Configuration Equip updated with profile")
    }

    /*In v2.17.3 CCU has created non-DM sensor points, this script handles removing of those*/
    fun removeRedundantDevicePoints() {
        val devices = hayStack.readAllEntities("device and domainName and node and (smartnode or helionode or hyperstat)")
        devices.forEach { device ->
            val deviceRef = device["id"].toString()

            val points = hayStack.readAllEntities(
                "point and not domainName and " +
                        "(port == \"" + Port.SENSOR_PM10 + "\"  or port == \"pm10Sensor\" or port == \""+Port.SENSOR_ENERGY_METER +
                        "\" or" + " port == \""+Port.SENSOR_NO +"\") and deviceRef == \"$deviceRef\"")

            points.forEach { point ->
                hayStack.deleteEntity(point["id"].toString())
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Deleted redundant point:" +
                        " ${point["dis"]} id :${point["dis"]}  for device: ${device["dis"]}")
            }
        }
    }

    private fun remigrateCpuPoint() {
        val hayStack = CCUHsApi.getInstance()
        val nonMigratedPoints: List<java.util.HashMap<Any, Any>> =
            hayStack.readAllEntities("point and not domainName and hyperstat")
        if (nonMigratedPoints.isNotEmpty()) {
            val migrationHandler = MigrationHandler(CCUHsApi.getInstance())
            migrationHandler.doHSCPUDMMigration()
        } else {
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "No non migrated points found for equip ")
        }

        val hyperStatCPUEquip =
            hayStack.readAllEntities("equip and hyperstat and cpu and $not_external_model_query")
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val deviceModel = ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective
        val model = ModelLoader.getHyperStatCpuModel()
        val entityMapper = EntityMapper(model as SeventyFiveFProfileDirective)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

        hyperStatCPUEquip.forEach { equip ->
            val equipId = equip["id"].toString()
            val config = getConfiguration(equipId)?.getActiveConfiguration()
            val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${config!!.nodeAddress}"
            equipBuilder.updateEquipAndPoints(
                config,
                model,
                hayStack.getSite()!!.id,
                equip["dis"].toString(),
                true
            )
            deviceBuilder.updateDeviceAndPoints(
                config,
                deviceModel,
                equip["id"].toString(),
                hayStack.site!!.id,
                deviceDis
            )
            config.apply { setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap()) }
        }
    }

    private fun migrateDevicePoints(ccuHsApi: CCUHsApi) {
        val devices = ccuHsApi.readAllEntities("device and not ccu and domainName")

        devices.forEach deviceLoop@{ device ->
            CcuLog.d(
                L.TAG_CCU_MIGRATION_UTIL,
                "====================== Device: $device ======================"
            )
            val deviceModel: SeventyFiveFDeviceDirective
            try {
                deviceModel =
                    getModelForDomainName(device["domainName"].toString()) as SeventyFiveFDeviceDirective
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                CcuLog.e(L.TAG_CCU_MIGRATION_UTIL, "deviceModel ${device["domainName"].toString()} not found")
                return@deviceLoop
            }

            val deviceId = device["id"].toString()

            CcuLog.d(
                L.TAG_CCU_MIGRATION_UTIL,
                "====================== Device Address: ${device["addr"]} ======================"
            )

            hayStack.readAllHDictByQuery("point and deviceRef == \"$deviceId\"")
                .forEach pointLoop@{ point ->
                    CcuLog.d(
                        L.TAG_CCU_MIGRATION_UTIL,
                        ">>>>>>>>>> device point: $point"
                    )
                    val rawPointBuilder = RawPoint.Builder().setHDict(point)
                    if (!point.has("domainName")) {
                        CcuLog.d(
                            L.TAG_CCU_MIGRATION_UTIL,
                            "domainName not exists for point: $point"
                        )
                        return@pointLoop
                    }
                    val domainPointDef = deviceModel.points.find { it.domainName == point["domainName"].toString() }
                    if(domainPointDef == null) {
                        CcuLog.d(
                            L.TAG_CCU_MIGRATION_UTIL,
                            "Point '${point["domainName"]}' not found in model '${deviceModel.name}'"
                        )
                        return@pointLoop
                    }
                    domainPointDef.tags
                        .filter { tag ->
                            tag.kind == TagType.STR &&
                                    (tag.name.equals("outputtype", ignoreCase = true) ||
                                            tag.name.equals("port", ignoreCase = true) ||
                                            tag.name.equals("inputtype", ignoreCase = true))
                        }
                        .forEach { tag ->
                            val defaultVal = tag.defaultValue ?: ""
                            CcuLog.i(
                                L.TAG_CCU_MIGRATION_UTIL,
                                "Adding tag '${tag.name}' with value: '$defaultVal' to point '${point["domainName"]}'"
                            )

                            rawPointBuilder.addTag(tag.name, HStr.make(defaultVal.toString()))
                        }

                    val rawPoint = rawPointBuilder.build()
                    rawPoint.lastModifiedBy = hayStack.ccuUserName
                    CcuLog.d(
                        L.TAG_CCU_MIGRATION_UTIL,
                        "Updated point '${rawPoint.domainName}' with Markers: ${rawPoint.markers}, Tags: ${rawPoint.tags}"
                    )
                    hayStack.updatePoint(rawPoint, rawPoint.id)
                }
        }
    }

    private fun updateHSSPoints(){
        val hssPoints = listOf(
            "exhaustFanStage1",
            "exhaustFanStage2",
            "coolingStage1",
            "coolingStage2",
            "coolingStage3",
            "heatingStage1",
            "heatingStage2",
            "heatingStage3",
            "fanOutCoolingStage1",
            "fanOutCoolingStage2",
            "fanOutCoolingStage3",
            "fanOutHeatingStage1",
            "fanOutHeatingStage2",
            "fanOutHeatingStage3",
            "exhaustFanStage1Threshold",
            "exhaustFanStage2Threshold"
        )


        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Updating corrupted HSS points Started")
        val hssEquips = hayStack.readAllEntities("equip and zone and domainName == \"hyperstatSplitCPU\"")
        hssEquips.forEach {
            updatePoints(CCUHsApi.getInstance(), hssPoints, it)
        }
        CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Updating corrupted HSS points ends")
    }
    private fun updateBackFillDefaultValue() {
        val backFillPoint = hayStack.readEntity("domainName ==\"${DomainName.backfillDuration}\"")
        if (backFillPoint.isNotEmpty()) {
            val backFilDefaultValue = hayStack.readDefaultValById(backFillPoint["id"].toString())
            if (backFilDefaultValue == 0.0) {
                BackfillUtil.setBackFillDuration(Globals.getInstance().applicationContext)
                CcuLog.i(
                    TAG_CCU_MIGRATION_UTIL,
                    "Backfill duration default value updated to 24 hours"
                )
            }
        }
    }
    private fun updateBypassDamperEquipPoints() {

        val bypassDamperEquip =
            hayStack.readEntity("equip and domainName==\"${DomainName.smartnodeBypassDamper}\"")
        if(bypassDamperEquip.isNotEmpty()) {
            val bypassDamperDevice =
                hayStack.readEntity("device and domainName==\"${DomainName.smartnodeDevice}\" and equipRef==\"${bypassDamperEquip["id"].toString()}\"")
            val physicalAnalog1outPoint =
                hayStack.readHDict(" point and domainName==\"${DomainName.analog1Out}\" and deviceRef == \"${bypassDamperDevice["id"].toString()}\"")
            val logicalPointDamperType =
                hayStack.readEntity("point and domainName==\"${DomainName.damperType}\" and equipRef == \"${bypassDamperEquip["id"].toString()}\"")
            val damperTypeValue =
                hayStack.readDefaultValById(logicalPointDamperType["id"].toString())
            CcuLog.i(
                TAG_CCU_MIGRATION_UTIL,
                "Damper Type Value: $damperTypeValue ,physicalAnalog1outPoint :  ${physicalAnalog1outPoint["analogType"]}"
            )

            if (physicalAnalog1outPoint["analogType"].toString() != DamperType.getBypassDamperDamperTypeString(
                    damperTypeValue.toInt()
                )
            ) {

                //analog 1 Out point
                SmartNode.updateDomainPhysicalPointType(
                    bypassDamperDevice["addr"].toString().toInt(),
                    DomainName.analog1Out,
                    DamperType.values()[damperTypeValue.toInt()].displayName
                )
                // analog 2 In point
                SmartNode.updateDomainPhysicalPointType(
                    bypassDamperDevice["addr"].toString().toInt(),
                    DomainName.analog2In,
                    DamperType.values()[damperTypeValue.toInt()].displayName
                )

                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL,
                    "Updated analog1In point type for ${bypassDamperEquip["dis"].toString()}"
                )
            } else {
                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL,
                    " Already Updated analog1In point type for ${bypassDamperEquip["dis"].toString()}"
                )

            }
        }

    }

    private fun updateVavAndDabEquipPoints() {
        val vavAndDabEquip = hayStack.readAllEntities("equip and zone  and (vav or dab) and domainName")

        vavAndDabEquip.forEach { equip ->
            CcuLog.i(TAG_CCU_MIGRATION_UTIL, "physical point analog type update for ${equip["dis"].toString()}")
            val device =
                hayStack.readEntity("device and (domainName==\"${DomainName.smartnodeDevice}\" or domainName==\"${DomainName.helionodeDevice}\") and  equipRef == \"${equip["id"].toString()}\"")
            val physicalAnalog1inPoint =
                hayStack.readHDict(" point and domainName==\"${DomainName.analog1In}\" and deviceRef == \"${device["id"].toString()}\"")
            val logicalPointDamperType =
                hayStack.readEntity("point and domainName==\"${DomainName.damperType}\" and equipRef == \"${equip["id"].toString()}\"")
            val damperTypeValue =
                hayStack.readDefaultValById(logicalPointDamperType["id"].toString())
            CcuLog.i(
                TAG_CCU_MIGRATION_UTIL,
                "Damper Type Value: $damperTypeValue ,physicalAnalog1inPoint :  ${physicalAnalog1inPoint["analogType"].toString()}"
            )
            if (physicalAnalog1inPoint["analogType"].toString() != DamperType.values()[damperTypeValue.toInt()].displayName) {
                SmartNode.updateDomainPhysicalPointType(
                    device["addr"].toString().toInt(),
                    DomainName.analog1In,
                    DamperType.values()[damperTypeValue.toInt()].displayName
                )
                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL,
                    "Updated analog1In point type for ${equip["dis"].toString()}"
                )
            } else {
                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL,
                    " Already Updated analog1In point type for ${equip["dis"].toString()}"
                )

            }

        }
    }

    private fun deleteConnectModuleOaoPoint() {
        val connectModuleEquip =
            hayStack.readEntity("equip and system and (domainName==\"" + DomainName.dabAdvancedHybridAhuV2_connectModule + "\" or domainName==\"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\")")
        val domainNames = listOf(

            DomainName.returnDamperMinOpen,
            DomainName.exhaustFanStage1Threshold,
            DomainName.exhaustFanStage2Threshold,
            DomainName.currentTransformerType,
            DomainName.exhaustFanHysteresis,
            DomainName.systemPurgeOutsideDamperMinPos,
            DomainName.enhancedVentilationOutsideDamperMinOpen,
            DomainName.enableOutsideAirOptimization
        )
        if (connectModuleEquip.isNotEmpty()) {
            val domainQuery = domainNames.joinToString(" or ") { "domainName==\"$it\"" }
            val oaoPoints = hayStack.readAllEntities(
                "equipRef==\"${connectModuleEquip["id"]}\" and ($domainQuery)"
            )
            oaoPoints.forEach {
                hayStack.deleteEntity(it["id"].toString())
                CcuLog.d(
                    TAG_CCU_MIGRATION_UTIL,
                    "connect Module OAO Point deleted  : ${it["dis"].toString()} ,  id :  ${it["id"].toString()}"
                )
            }
        }
    }

    private fun removingDuplicateDualDuctSensorPoints() {
        val dualDuctEquip = hayStack.readAllEntities("equip and dualDuct and zone")

        dualDuctEquip.forEach { equip ->
            val equipId = equip["id"].toString()
            val equipName = equip["dis"].toString()

            removeDuplicateSensorPoints("pm10", equipId, equipName)
            removeDuplicateSensorPoints("emr", equipId, equipName)
        }
    }

    private fun removeDuplicateSensorPoints(
        sensorType: String,
        equipId: String,
        equipName: String
    ) {
        val sensorPoints =
            hayStack.readAllEntities("$sensorType and sensor and equipRef==\"$equipId\"")

        if (sensorPoints.size <= 1) {
            CcuLog.d(
                TAG_CCU_MIGRATION_UTIL,
                "No duplicate points found for $sensorType sensor $equipName"
            )
            return
        }

        val toDelete = sensorPoints.drop(1)
        toDelete.forEach { point ->
            hayStack.deleteEntity(point["id"].toString())
            CcuLog.d(
                TAG_CCU_MIGRATION_UTIL,
                "Duplicate $sensorType sensor point deleted for $equipName"
            )
        }
    }

    private fun removeNonDmSensorPoints() {
        val equips = hayStack.readAllEntities("(equip and (vav or dab or pid or sse) and domainName and zone) or (equip and (oao or bypassDamper) and domainName)")
        equips.forEach { equip ->
            val equipRef = equip["id"].toString()
            val points = hayStack.readAllEntities(
                "point and not domainName and sensor and equipRef == \"$equipRef\"")
            points.forEach { point ->
                hayStack.deleteEntity(point["id"].toString())
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "removed non dm sensor point $point")
            }
        }
    }

    private fun updateFloorRefToRoomPoints() {
        val roomEntities = hayStack.readAllEntities("room")
        val siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE)
        if (roomEntities.isNotEmpty()) {
            for (room in roomEntities) {
                CcuLog.i(
                    TAG_CCU_MIGRATION_UTIL,
                    "room:>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> {${room["dis"].toString()}}"
                )
                val roomId = room["id"].toString()
                val floorRef = room["floorRef"].toString()
                val floor = hayStack.readEntity("id == $floorRef")
                val dis =
                    siteMap["dis"].toString() + "-" + floor["dis"].toString() + "-" + room["dis"].toString()
                val roomPointList = CCUHsApi.getInstance()
                    .readAllHDictByQuery("not domainName and point and roomRef == \"$roomId\"")
                for (pointDict in roomPointList) {
                    val point = Point.Builder().setHDict(pointDict).build()
                    CcuLog.i(
                        TAG_CCU_MIGRATION_UTIL,
                        "Updating point: ${pointDict.dis()} with floorRef: ${point.floorRef}" +
                                " and displayName: ${point.displayName}"
                    )
                    var pointDisName = point.displayName
                    if (pointDisName != null) {
                        val splitStr =
                            pointDisName.split("-".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                        pointDisName = splitStr[splitStr.size - 1]
                    }
                    point.displayName = "$dis-$pointDisName"
                    point.floorRef = floor["id"].toString()
                    CCUHsApi.getInstance().updatePoint(point, point.getId())
                    CcuLog.i(
                        TAG_CCU_MIGRATION_UTIL,
                        "Updated point: ${point.displayName} with floorRef: ${point.floorRef} " +
                                "and displayName: ${point.displayName}"
                    )
                }
            }
        }
    }

    private fun correctDataTypeForKVPairsInModbus() {
        try {
            val tagValueCorrectionList = listOf("order", "stage", "pointNum")
            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Executing correctDataTypeForKVPairsInModbus")
            hayStack.readAllHDictByQuery("(order or stage or pointNum) and (modbus or bacnetDeviceId) and point").forEach { orderPoint ->
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Order point with name: " + orderPoint.dis())
                val p = Point.Builder().setHDict(orderPoint)
                var updatePoint = false
                for(key in tagValueCorrectionList) {
                    orderPoint.get(key, false)?.let { tagValue ->
                        if(tagValue is HStr) {
                            updatePoint = true
                            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Point incorrect tag datatype for key: $key with value: $tagValue")
                            p.addTag(key, HNum.make(Integer.parseInt(tagValue.toString())))
                        }
                    }
                }
                if(updatePoint) {
                    hayStack.updatePoint(p.build(), orderPoint.id().`val`)
                    CcuLog.i(TAG_CCU_MIGRATION_UTIL, "Modbus point with incorrect tag data type updated: ${orderPoint.dis()}, ${orderPoint.id()} with correct tag datatype")
                }
            }
        } catch (exception: Exception) {
            CcuLog.e(
                TAG_CCU_MIGRATION_UTIL,
                "Error while updating order key data type for modbus points",
                exception
            )
        }
    }

    private fun deleteRedundantOaoPointsBasedOnCurrentSystemProfile() {
        try {
            hayStack.readId("domainName==\"${DomainName.smartnodeOAO}\"")?.let { oaoEquipId ->
                CcuLog.d(TAG_CCU_MIGRATION_UTIL, "OAO Equip Found. ID: $oaoEquipId")
                hayStack.readHDict(CommonQueries.SYSTEM_PROFILE)?.let { systemEquipDict ->
                    CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Deleting redundant OAO points for equip: $oaoEquipId")
                    if(systemEquipDict.has(Tags.VAV)) {
                        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "VAV System Equip found")
                        hayStack.readAllHDictByQuery("${Tags.DAB} and equipRef == \"$oaoEquipId\"")?.forEach { oaoDabPointHDict ->
                            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Deleting DAB based OAO point: ${oaoDabPointHDict.dis()} for id: ${oaoDabPointHDict.id()}")
                            hayStack.deleteEntityItem(oaoDabPointHDict.id().toString())
                            hayStack.deleteWritableArray(oaoDabPointHDict.id().toString())
                        }
                    } else if (systemEquipDict.has(Tags.DAB)) {
                        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "DAB System Equip found")
                        hayStack.readAllHDictByQuery("${Tags.VAV} and equipRef == \"$oaoEquipId\"")?.forEach { oaoVavPointHDict ->
                            CcuLog.d(TAG_CCU_MIGRATION_UTIL, "Deleting VAV based OAO point: ${oaoVavPointHDict.dis()} for id: ${oaoVavPointHDict.id()}")
                            hayStack.deleteEntityItem(oaoVavPointHDict.id().toString())
                            hayStack.deleteWritableArray(oaoVavPointHDict.id().toString())
                        }
                    } else {
                        CcuLog.d(TAG_CCU_MIGRATION_UTIL, "No VAV or DAB System Equip found, Skipping operation.")
                        // Do Nothing
                    }
                }
            }
            setMigrateDeleteRedundantOaoPointsBySystemEquip()
        } catch (exception: Exception) {
            CcuLog.e(
                TAG_CCU_MIGRATION_UTIL,
                "Error while deleting redundant OAO points based on current system profile",
                exception
            )
            exception.printStackTrace()
        }
    }
}