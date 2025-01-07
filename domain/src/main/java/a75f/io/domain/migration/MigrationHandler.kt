package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.api.Device
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getBypassEquipByDomainName
import a75f.io.domain.api.Domain.getDeviceEntityByDomain
import a75f.io.domain.api.Domain.getEquipDetailsByDomain
import a75f.io.domain.api.Domain.getSystemEquipByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.api.Equip
import a75f.io.domain.config.DefaultProfileConfiguration
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.EquipBuilderConfig
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.TunerEquipBuilder
import a75f.io.domain.util.ModelCache
import a75f.io.logger.CcuLog
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDevicePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import io.seventyfivef.domainmodeler.common.point.AssociationConfiguration
import io.seventyfivef.domainmodeler.common.point.ComparisonType
import io.seventyfivef.domainmodeler.common.point.DependentConfiguration
import io.seventyfivef.domainmodeler.common.point.PointConfiguration

/**
 * Created by Manjunath K on 16-06-2023.
 */
class MigrationHandler(var haystack: CCUHsApi, var listener: DiffManger.OnMigrationCompletedListener) {

    fun migrateModel(entityData: EntityConfiguration,oldModel: ModelDirective, newModel: ModelDirective, siteRef: String) {
        if (newModel is SeventyFiveFTunerDirective) {
            CcuLog.printLongMessage(Domain.LOG_TAG,
                "Building equip model upgrade detected : Run migration to $newModel. modelId: ${newModel.id} "
            )
            val tunerEquipBuilder = TunerEquipBuilder(haystack)
            tunerEquipBuilder.updateEquipAndPoints(newModel,entityData, siteRef)
            tunerEquipBuilder.updateBackendBuildingTuner(siteRef, haystack)
            listener.onMigrationCompletedCompleted(haystack)
        }else if (newModel is SeventyFiveFDeviceDirective) {
            CcuLog.printLongMessage(Domain.LOG_TAG,
                "Device equip model upgrade detected : Run migration to $newModel. modelId: ${newModel.id} "
            )
            migrateDeviceModel(entityData, oldModel, newModel, siteRef)

        } else {
            val equips: List<Equip>
            if (Domain.readEquip(newModel.id)["domainName"].toString() == DomainName.smartnodeBypassDamper) {
                val equip = getBypassEquipByDomainName(newModel.domainName)
                equips = if (equip != null) listOf(equip) else emptyList()
            } else if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                val equip = getSystemEquipByDomainName(newModel.domainName)
                equips = if (equip != null) listOf(equip) else emptyList()
            } else {
                equips = getEquipDetailsByDomain(newModel.domainName)
            }
            CcuLog.printLongMessage(Domain.LOG_TAG,
                "Equip model upgrade detected : Run migration to $newModel; equip size ${equips.size}"
            )
            CcuLog.d(Domain.LOG_TAG, "tobeAdded: size:${entityData.tobeAdded.size}")
            entityData.tobeAdded.forEach { item ->
                CcuLog.d(Domain.LOG_TAG, "tobeAdded: item:${item.domainName}")
            }
            Log.d(Domain.LOG_TAG, "tobeDeleted: size:${entityData.tobeDeleted.size}")
            entityData.tobeDeleted.forEach { item ->
                CcuLog.d(Domain.LOG_TAG, "tobeDeleted: item:${item.domainName}")
            }
            CcuLog.d(Domain.LOG_TAG, "tobeUpdated: size:${entityData.tobeUpdated.size}")
            entityData.tobeUpdated.forEach { item ->
                CcuLog.d(Domain.LOG_TAG, "tobeUpdated: item:${item.domainName}")
            }
            if(equips.isNotEmpty()) {
                addEntityData(entityData.tobeAdded, newModel, equips, siteRef)
                removeEntityData(entityData.tobeDeleted, oldModel, equips, siteRef)
                updateEntityData(entityData.tobeUpdated, newModel, equips, siteRef)
                updateEquipVersion(newModel, equips, siteRef)
            }
        }
    }

    fun migrateDeviceModel(
        entityData: EntityConfiguration,
        oldModel: ModelDirective,
        newModel: ModelDirective,
        siteRef: String
    ) {
        val devices: List<Device> =
            if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                /*val equip = getSystemEquipByDomainName(newModel.domainName)
                if (equip != null) listOf(equip) else*/ emptyList()
            } else {
                getDeviceEntityByDomain(newModel.domainName, newModel.version.toString())
            }
        CcuLog.d(Domain.LOG_TAG, "Device points to be Added: size:${entityData.tobeAdded.size}")
        entityData.tobeAdded.forEach { item ->
            CcuLog.d(Domain.LOG_TAG,"tobeAdded: item:${item.domainName}")
        }
        CcuLog.d(Domain.LOG_TAG, "Device points to be Deleted: size:${entityData.tobeDeleted.size}")
        entityData.tobeDeleted.forEach { item ->
            CcuLog.d(Domain.LOG_TAG, "tobeDeleted: item:${item.domainName}")
        }
        CcuLog.d(Domain.LOG_TAG,"Device points to be Updated: size:${entityData.tobeUpdated.size}")
        entityData.tobeUpdated.forEach { item ->
            CcuLog.d(Domain.LOG_TAG,"tobeUpdated: item:${item.domainName}")
        }
        if (devices.isNotEmpty()) {
            addDeviceEntityData(entityData.tobeAdded, newModel, devices)
            removeDeviceEntityData(entityData.tobeDeleted, oldModel, devices, siteRef)
            updateDeviceEntityData(entityData.tobeUpdated, newModel, devices, siteRef)
        }

    }

    private fun addDeviceEntityData(
        tobeAdded: MutableList<EntityConfig>,
        newModel: ModelDirective,
        devices: List<Device>
    ) {
        if(tobeAdded.isEmpty()) return
        devices.forEach { device ->
            CcuLog.d(Domain.LOG_TAG, "device Id: ${device.id}, domainName: ${device.domainName}")
            val deviceHdict = haystack.readHDict("device and id == "+device.id)
            val haystackDevice = a75f.io.api.haystack.Device.Builder().setHDict(deviceHdict).build()
            val equip = haystack.readEntity("equip and id == "+haystackDevice.equipRef.toString())
            val sourceModel = equip["sourceModel"].toString()
            val modelDirective = ModelCache.getModelById(sourceModel)
            val profileConfiguration = getProfileConfig(equip["profile"].toString())
            updateRef(equip, profileConfiguration)
            val entityMapper = EntityMapper(modelDirective as SeventyFiveFProfileDirective)
            val deviceBuilder = DeviceBuilder(haystack, entityMapper)
            tobeAdded.forEach { diffDomain ->
                CcuLog.d(Domain.LOG_TAG, "tobe added ${diffDomain.domainName}  to the device $device" )
               val modelPointDef =
                    newModel.points.find { it.domainName == diffDomain.domainName }
                try {
                    deviceBuilder.createPoint(
                        modelPointDef as SeventyFiveFDevicePointDef,
                        profileConfiguration,
                        haystackDevice,
                        haystackDevice.displayName
                    )
                } catch (e: Exception) {
                    CcuLog.d(Domain.LOG_TAG,"Exception: $e")
                    e.printStackTrace()
                }
            }

            CcuLog.d(Domain.LOG_TAG,"Update device id ${haystackDevice.id} and device name : ${haystackDevice.displayName}")
            deviceBuilder.updateDevice(
                haystackDevice.id.toString(),
                newModel as SeventyFiveFDeviceDirective,
                haystackDevice.displayName
            )
        }
    }


    private fun updateDeviceEntityData(
        tobeUpdated: MutableList<EntityConfig>,
        newModel: ModelDirective,
        devices: List<Device>,
        siteRef: String
    ){
        devices.forEach { device ->
            CcuLog.d(Domain.LOG_TAG, "Update device Id: ${device.id}, domainName: ${device.domainName}")
            val deviceHdict = haystack.readHDict("device and id == " + device.id)
            val haystackDevice = a75f.io.api.haystack.Device.Builder().setHDict(deviceHdict).build()
            val deviceEntity = haystack.readEntity("equip and id == " + haystackDevice.equipRef)
            val sourceModel = deviceEntity["sourceModel"].toString()
            val modelDirective = ModelCache.getModelById(sourceModel)
            val profileConfiguration = getProfileConfig(deviceEntity["profile"].toString())
            updateRef(deviceEntity, profileConfiguration)
            val entityMapper = EntityMapper(modelDirective as SeventyFiveFProfileDirective)
            val deviceBuilder = DeviceBuilder(haystack, entityMapper)
            var devicePoints = haystack.readAllEntities("point and deviceRef == \"${device.id}\"")

            tobeUpdated.forEach { diffDomain ->
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                val point = devicePoints.find { it["domainName"].toString() == diffDomain.domainName }
                try {
                    if (point != null) {
                        deviceBuilder.updatePoint(
                            modelPointDef as SeventyFiveFDevicePointDef,
                            profileConfiguration,
                            haystackDevice,
                            point)
                    }
                    CcuLog.d(Domain.LOG_TAG, "Device updated: ${haystackDevice.displayName} with domainName: ${diffDomain.domainName} ")
                } catch (e: Exception) {
                    CcuLog.d(Domain.LOG_TAG,"Exception: $e")
                    e.printStackTrace()
                }
            }
            deviceBuilder.updateDevice(
                haystackDevice.id.toString(),
                newModel as SeventyFiveFDeviceDirective,
                haystackDevice.displayName
            )
        }
    }

    private fun removeDeviceEntityData(
        tobeDeleted: MutableList<EntityConfig>,
        oldModel: ModelDirective,
        devices: List<Device>,
        siteRef: String
    ) {
        if(tobeDeleted.isEmpty()) return
        devices.forEach { device ->
            val deviceHdict = haystack.readHDict("device and id == " + device.id)
            val haystackDevice = a75f.io.api.haystack.Device.Builder().setHDict(deviceHdict).build()
            val deviceEntity = haystack.readEntity("equip and id == " + haystackDevice.equipRef)
            val sourceModel = deviceEntity["sourceModel"].toString()
            val modelDirective = ModelCache.getModelById(sourceModel)
            val profileConfiguration = getProfileConfig(deviceEntity["profile"].toString())
            val entityMapper = EntityMapper(modelDirective as SeventyFiveFProfileDirective)
            val deviceBuilder = DeviceBuilder(haystack , entityMapper)
            val devicePoints = haystack.readAllEntities("point and deviceRef == \"${device.id}\"")
            tobeDeleted.forEach { diffDomain ->
                val point =
                    devicePoints.find { it["domainName"].toString() == diffDomain.domainName }
                try {
                    if (!point.isNullOrEmpty()) {
                        CcuLog.d(Domain.LOG_TAG , "Remove device point: ${point["domainName"]} for device ${haystackDevice.displayName}")
                        val modelPointDef = oldModel.points.find { it.domainName == diffDomain.domainName }
                        val pointEntity = deviceBuilder.buildRawPoint(
                            modelPointDef as SeventyFiveFDevicePointDef ,
                            profileConfiguration ,
                            haystackDevice
                        )
                        DomainManager.removeDeviceRawPoint(pointEntity)
                        haystack.deleteEntity(point["id"].toString())
                    }
                } catch (e: Exception) {
                    CcuLog.d(Domain.LOG_TAG , "Exception: $e")
                    e.printStackTrace()
                }
            }
        }
    }

    private fun updateEquipVersion(newModel: ModelDirective, equips: List<Equip>, siteRef: String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        equips.forEach { it ->
            val equipMap = haystack.readMapById(it.id)
            val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
            updateRef(equipMap, profileConfiguration)
            val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                haystack.timeZone, equipMap["dis"].toString()))
            // TODO: once OAO is DM-migrated, a similar conditional should be created for it
            if (Domain.readEquip(newModel.id)["domainName"].toString() == DomainName.smartnodeBypassDamper) {
                hayStackEquip.ahuRef = equipMap["ahuRef"]?.toString()?: haystack.readEntity("system and equip and not connectModule and not modbus")["id"].toString()
                hayStackEquip.roomRef = "SYSTEM"
                hayStackEquip.floorRef = "SYSTEM"
                haystack.updateEquip(hayStackEquip, it.id)
                hayStackEquip.id = it.id
                DomainManager.addBypassEquip(Domain.hayStack, Domain.hayStack.ccuId)
                CcuLog.d(Domain.LOG_TAG, "DM-DM Bypass Equip updated: ${hayStackEquip.domainName}")
            } else if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                hayStackEquip.roomRef = "SYSTEM"
                hayStackEquip.floorRef = "SYSTEM"
                /*For diag equip we should not have profileType*/
                if(equipMap.containsKey("domainName") && equipMap["domainName"] == DomainName.diagEquip) {
                    equipMap["gatewayRef"]?.let { hayStackEquip.gatewayRef = it.toString() }
                    hayStackEquip.profile = null
                }
                equipMap["ahuRef"]?.let { hayStackEquip.ahuRef = it.toString() }
                haystack.updateEquip(hayStackEquip, it.id)
                DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
                DomainManager.addOaoEquip(Domain.hayStack, Domain.hayStack.ccuId)
                DomainManager.addEquipToDomain(Domain.hayStack, Domain.hayStack.ccuId, DomainName.ccuConfiguration)
                DomainManager.addEquipToDomain(Domain.hayStack, Domain.hayStack.ccuId, DomainName.diagEquip)
                CcuLog.d(Domain.LOG_TAG, "DM-DM system Equip updated: ${hayStackEquip.domainName}")
            }else{
                hayStackEquip.ahuRef = equipMap["ahuRef"]?.toString()
                hayStackEquip.gatewayRef = equipMap["gatewayRef"]?.toString()
                haystack.updateEquip(hayStackEquip, it.id)
                hayStackEquip.id = it.id
                DomainManager.addEquip(hayStackEquip)
                CcuLog.d(Domain.LOG_TAG, "DM-DM Equip updated: ${hayStackEquip.domainName}")
            }
        }
    }

    private fun addEntityData(tobeAdded: MutableList<EntityConfig>, newModel: ModelDirective,
                              equips: List<Equip>, siteRef : String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
         // need to revisit this line
        tobeAdded.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                equips.forEach {
                    val equipMap = haystack.readMapById(it.id)
                    val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                    updateRef(equipMap, profileConfiguration)
                    val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                        haystack.timeZone, equipMap["dis"].toString()))
                    hayStackEquip.ahuRef = equipMap["ahuRef"]?.toString()
                    hayStackEquip.gatewayRef = equipMap["gatewayRef"]?.toString()
                    haystack.updateEquip(hayStackEquip, it.id)
                    hayStackEquip.id = it.id
                    if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                        DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
                        DomainManager.addOaoEquip(Domain.hayStack, Domain.hayStack.ccuId)
                    }else{
                        DomainManager.addEquip(hayStackEquip)
                    }

                }
            }
            equips.forEach {equip ->
                val equipMap = haystack.readMapById(equip.id)
                val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                updateRef(equipMap, profileConfiguration)
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    if (toBeAddedForEquip(modelPointDef, equip.id, false)) {
                        equipBuilder.createPoint(
                            PointBuilderConfig(modelPointDef, profileConfiguration, equip.id, siteRef, haystack.timeZone, equipMap["dis"].toString())
                        )
                    }
                }
            }
        }
    }
    private fun removeEntityData(
        tobeRemove: MutableList<EntityConfig>, oldModel: ModelDirective, equips: List<Equip>,
        siteRef: String
    ) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        val profileConfiguration = getProfileConfig("")
        tobeRemove.forEach { diffDomain ->
            equips.forEach {equip ->
                val equipDetails = haystack.readMapById(equip.id)
                val modelPointDef = oldModel.points.find { it.domainName == diffDomain.domainName }
                modelPointDef?.run {
                    val hayStackPoint = equipBuilder.buildPoint(PointBuilderConfig( modelPointDef,
                        profileConfiguration, equip.id, siteRef, haystack.timeZone, equipDetails["dis"].toString()))
                    val point = CCUHsApi.getInstance().readEntity("point and domainName == \"${diffDomain.domainName}\" and equipRef == \"${equip.id}\"")
                    DomainManager.removePoint(hayStackPoint)
                    haystack.deleteEntity(point["id"].toString())
                }
            }
        }
    }

    private fun updateEntityData(tobeUpdate: MutableList<EntityConfig>, newModel: ModelDirective,
                                 equips: List<Equip>, siteRef: String) {
        val equipBuilder = ProfileEquipBuilder (haystack)
        tobeUpdate.forEach { diffDomain ->
            // updated Equip
            if (diffDomain.domainName == newModel.domainName) {
                equips.forEach {
                    val equipMap = haystack.readMapById(it.id)
                    val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                    updateRef(equipMap, profileConfiguration)
                    val hayStackEquip = equipBuilder.buildEquip(EquipBuilderConfig(newModel, profileConfiguration, siteRef,
                        haystack.timeZone, equipMap["dis"].toString()))
                    hayStackEquip.ahuRef = equipMap["ahuRef"]?.toString()
                    hayStackEquip.gatewayRef = equipMap["gatewayRef"]?.toString()
                    haystack.updateEquip(hayStackEquip, it.id)
                    hayStackEquip.id = it.id
                    if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                        DomainManager.addSystemEquip(Domain.hayStack, Domain.hayStack.ccuId)
                        DomainManager.addOaoEquip(Domain.hayStack, Domain.hayStack.ccuId)
                    }else{
                        DomainManager.addEquip(hayStackEquip)
                    }
                }
            }
            equips.forEach {equip ->
                val modelPointDef = newModel.points.find { it.domainName == diffDomain.domainName }
                val equipMap = haystack.readMapById(equip.id)
                val profileConfiguration = getProfileConfig(equipMap["profile"].toString())
                updateRef(equipMap, profileConfiguration)
                modelPointDef?.run {
                    if (toBeAddedForEquip(modelPointDef, equip.id, true)) {
                        val hayStackPoint = equipBuilder.buildPoint(
                            PointBuilderConfig(
                                modelPointDef,
                                profileConfiguration,
                                equip.id,
                                siteRef,
                                haystack.timeZone,
                                equipMap["dis"].toString()
                            )
                        )
                        val point = CCUHsApi.getInstance()
                            .readEntity("point and domainName == \"${diffDomain.domainName}\" and equipRef == \"${equip.id}\"")
                        if(point["id"]==null) {
                            return@forEach
                        }
                        Log.d(Domain.LOG_TAG, "updated haystack point: $hayStackPoint")
                        if (Domain.readEquip(newModel.id)["roomRef"].toString() == "SYSTEM") {
                            hayStackPoint.roomRef = "SYSTEM"
                            hayStackPoint.floorRef = "SYSTEM"
                            haystack.updatePoint(hayStackPoint, point["id"].toString())
                        } else {
                            haystack.updatePoint(hayStackPoint, point["id"].toString())
                            DomainManager.addPoint(hayStackPoint)
                        }
                        try {
                            updatePointAssociation(modelPointDef, hayStackPoint, point["id"].toString())
                        } catch (e: Exception) {
                            /*
                       * Since we are unsure about the specific exception to catch here, we use the generic Exception class instead.
                       * we need to revisit this and add a proper exception handling
                        */
                            CcuLog.d(Domain.LOG_TAG, "Update point Association is failed : " + point["id"].toString())
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    private fun updatePointAssociation(modelPointDef: ModelPointDef, logicalPoint: Point, logicalPointId: String) {
        CcuLog.d(Domain.LOG_TAG, "point association update: ${modelPointDef.domainName}")
        if (modelPointDef is SeventyFiveFProfilePointDef) {
            if (modelPointDef.devicePointAssociation != null) {
                val device =  haystack.readEntity("device and equipRef == \"${logicalPoint.equipRef}\"")
                val physicalPointDict = haystack.readHDict("point and" +
                        " domainName == \"${modelPointDef.devicePointAssociation?.devicePointDomainName.toString()}\" and deviceRef == \"${device["id"]}\"")
                val physicalPoint = RawPoint.Builder().setHDict(physicalPointDict).build()
                physicalPoint.pointRef = logicalPointId
                DomainManager.addRawPoint(physicalPoint)
                haystack.updatePoint(physicalPoint, physicalPoint.id)
                CcuLog.d(
                    Domain.LOG_TAG,
                    "point association updated: ${modelPointDef.domainName} " +
                            "logicalPoint.id: $logicalPointId\" " +
                            "physicalPoint: ${physicalPoint.id} " +
                            "device: ${device}"
                )
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

    private fun updateRef(equipMap: HashMap<Any, Any>, profileConfiguration: ProfileConfiguration) {
        if (equipMap.containsKey("roomRef") && equipMap["roomRef"] != null) profileConfiguration.roomRef = equipMap["roomRef"].toString()
        if (equipMap.containsKey("floorRef") && equipMap["floorRef"] != null) profileConfiguration.floorRef = equipMap["floorRef"].toString()
        if (equipMap.containsKey("group") && equipMap["group"] != null) {
            profileConfiguration.nodeAddress = Integer.parseInt(
            equipMap["group"].toString())
        }
        if(equipMap.containsKey("domainName") && equipMap["domainName"] == DomainName.diagEquip
            || equipMap["domainName"] == DomainName.ccuConfiguration) {
            profileConfiguration.nodeAddress = 0
        }
        if (equipMap.containsKey("profile") && equipMap["profile"] != null) profileConfiguration.profileType = equipMap["profile"].toString()
    }

   /* isDynamicSensorRequired is required to decide whether to add/update a dynamic sensor point or not
       While Adding we never add DYNAMIC_SENSOR points; they are created from the device layer, but updating the point we can allow to update the dynamic sensor points*/
    private fun toBeAddedForEquip(pointDef : ModelPointDef, equipRef : String, isDynamicSensorRequired : Boolean) : Boolean {

        if (pointDef is SeventyFiveFProfilePointDef) {
            return when (pointDef.configuration.configType) {
                PointConfiguration.ConfigType.BASE -> true // always add BASE points
                PointConfiguration.ConfigType.DEPENDENT -> isDependentPointEnabled(pointDef, equipRef)
                PointConfiguration.ConfigType.DYNAMIC_SENSOR -> isDynamicSensorRequired // never add DYNAMIC_SENSOR points; they are created from the device layer
                PointConfiguration.ConfigType.ASSOCIATED -> isAssociatedPointEnabled(pointDef)
                PointConfiguration.ConfigType.ASSOCIATION -> isAssociationPointEnabled(pointDef, equipRef)
            }
        }

        return true
    }

    private fun isDependentPointEnabled(pointDef: SeventyFiveFProfilePointDef, equipRef : String) : Boolean {
        // If a point is DEPENDENT, read the point it depends on from Haystack, then evaluate the config
        val pointConfig = pointDef.configuration as DependentConfiguration
        val configValue = haystack.readPointPriorityValByQuery("point and domainName == \"" + pointConfig.domainName + "\" and equipRef == \"" + equipRef + "\"")

        if (configValue != null) {
            return when (pointConfig.comparisonType) {
                ComparisonType.EQUALS -> (pointConfig.value as Int) == configValue.toInt()
                ComparisonType.NOT_EQUALS -> (pointConfig.value as Int) != configValue.toInt()
                ComparisonType.GREATER_THAN -> (pointConfig.value as Int) > configValue.toInt()
                ComparisonType.LESS_THAN -> (pointConfig.value as Int) < configValue.toInt()
                ComparisonType.GREATER_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) >= configValue.toInt()
                ComparisonType.LESS_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) <= configValue.toInt()
            }
        }
        return true
    }

    private fun isAssociatedPointEnabled(pointDef: SeventyFiveFProfilePointDef) : Boolean {
        /*  ASSOCIATED points should not be added as part of a migration.
            They are created only during configuration (when their domainName shows up in the enum of a created ASSOCIATION point).
         */
        return false
    }

    private fun isAssociationPointEnabled(pointDef: SeventyFiveFProfilePointDef, equipRef : String) : Boolean {
        // Expectation is that new ASSOCIATION points would not be added to a model that's already in production
        // But, if one appears, read the point it depends on from Haystack, then evaluate the config. Add only if it is enabled per the config.
        val pointConfig = pointDef.configuration as AssociationConfiguration
        val configValue = haystack.readPointPriorityValByQuery("point and domainName == \"" + pointConfig.domainName + "\" and equipRef == \"" + equipRef + "\"")

        if (configValue != null) {
            return when (pointConfig.comparisonType) {
                ComparisonType.EQUALS -> (pointConfig.value as Int) == configValue.toInt()
                ComparisonType.NOT_EQUALS -> (pointConfig.value as Int) != configValue.toInt()
                ComparisonType.GREATER_THAN -> (pointConfig.value as Int) > configValue.toInt()
                ComparisonType.LESS_THAN -> (pointConfig.value as Int) < configValue.toInt()
                ComparisonType.GREATER_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) >= configValue.toInt()
                ComparisonType.LESS_THAN_OR_EQUAL_TO -> (pointConfig.value as Int) <= configValue.toInt()
            }
        }
        return false
    }

}