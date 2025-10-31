package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.cutover.BuildingEquipCutOverMapping
import a75f.io.domain.cutover.devicePointWithDomainNameExists
import a75f.io.domain.cutover.getDeviceDomainNameFromDis
import a75f.io.domain.logic.DomainManager.addDomainDevices
import a75f.io.domain.util.TagsUtil
import a75f.io.domain.util.extractAndAppendExternalEdits
import a75f.io.domain.util.highPriorityDispatcher
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDevicePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import io.seventyfivef.ph.core.TagType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.projecthaystack.HBool
import org.projecthaystack.HDateTime
import org.projecthaystack.HStr
import kotlin.system.measureTimeMillis

class DeviceBuilder(private val hayStack : CCUHsApi, private val entityMapper: EntityMapper?) {

    fun buildDeviceAndPoints(configuration: ProfileConfiguration, modelDef: SeventyFiveFDeviceDirective, equipRef: String, siteRef : String, deviceDis: String,
                             profileModelDef: SeventyFiveFProfileDirective? = null) : String{
        CcuLog.i(Domain.LOG_TAG, "buildDeviceAndPoints $configuration")
        val hayStackDevice = buildDevice(modelDef, configuration, equipRef, siteRef, deviceDis)
        val deviceId = hayStack.addDevice(hayStackDevice)
        hayStackDevice.id = deviceId
        DomainManager.addDevice(hayStackDevice)
        addDomainDevices(hayStack)
        val time = measureTimeMillis {
            createPoints(modelDef, configuration, hayStackDevice)
        }
        profileModelDef?.let { updateBaseConfigPointRefs(configuration, it, hayStackDevice) }
        CcuLog.i(Domain.LOG_TAG, "Time taken to create device points ${time}ms")
        return deviceId
    }

    fun buildLowCodeDeviceAndPoints(configuration: ProfileConfiguration, modelDef: SeventyFiveFDeviceDirective,
                                    siteRef : String, deviceDis: String, deviceRef: String? = null) : String{
        CcuLog.i(Domain.LOG_TAG, "build $deviceDis DeviceAndPoints $configuration")
        val hayStackDevice = buildDevice(modelDef, configuration, null, siteRef, deviceDis, deviceRef)
        val deviceId = hayStack.addDevice(hayStackDevice)
        hayStackDevice.id = deviceId
        DomainManager.addDevice(hayStackDevice)
        val time = measureTimeMillis {
            createPoints(modelDef, configuration, hayStackDevice)
        }
        CcuLog.i(Domain.LOG_TAG, "Time taken to create device points ${time}ms")
        return deviceId
    }

    fun updateDeviceAndPoints(configuration: ProfileConfiguration, modelDef: SeventyFiveFDeviceDirective, equipRef: String, siteRef : String, deviceDis: String,
                              profileModelDef: SeventyFiveFProfileDirective? = null) : String {
        CcuLog.i(Domain.LOG_TAG, "updateDeviceAndPoints $configuration")
        val hayStackDevice = buildDevice(modelDef, configuration, equipRef, siteRef, deviceDis)

        val device = hayStack.readEntity(
            "device and addr == \"${configuration.nodeAddress}\"")

        val deviceId =  device["id"].toString()
        if(!device.containsKey("domainName")) {
            hayStack.updateDevice(hayStackDevice, deviceId)
        }

        hayStackDevice.id = deviceId
        DomainManager.addDevice(hayStackDevice)
        val time = measureTimeMillis {
            updatePoints(modelDef, configuration, hayStackDevice)
        }
        profileModelDef?.let { updateBaseConfigPointRefs(configuration, it, hayStackDevice) }
        CcuLog.i(Domain.LOG_TAG, "Time taken to update device points ${time}ms")
        return deviceId
    }

    private fun createPoints(modelDef: SeventyFiveFDeviceDirective, profileConfiguration: ProfileConfiguration, device: Device) {
        runBlocking {
            modelDef.points.map { point ->
                async(highPriorityDispatcher) {
                    try {
                        CcuLog.i(Domain.LOG_TAG, "Adding raw point ${point.domainName}")
                        val hayStackPoint = buildRawPoint(point, profileConfiguration, device)
                        val pointId = hayStack.addPoint(hayStackPoint)
                        hayStackPoint.id = pointId
                        DomainManager.addRawPoint(hayStackPoint)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        CcuLog.e(Domain.LOG_TAG, "Error adding raw point ${point.domainName}")
                    }
                }
            }.awaitAll()
        }
    }
    private fun buildDevice(
        modelDef: SeventyFiveFDeviceDirective, configuration: ProfileConfiguration,
        equipRef: String?, siteRef: String, deviceDis: String, deviceRef: String? = null ) : Device{
        CcuLog.i(Domain.LOG_TAG, "buildDevice ${modelDef.domainName}")
        val deviceBuilder = Device.Builder().setDisplayName(deviceDis)
            .setDomainName(modelDef.domainName)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setAddr(configuration.nodeAddress)
            .setSiteRef(siteRef)

        equipRef?.let {
            deviceBuilder.setEquipRef(it)
        }

        modelDef.tags.filter { it.kind == TagType.MARKER && it.name.lowercase() != "addr"}.forEach{ deviceBuilder.addMarker(it.name)}
        if (modelDef.domainName.equals(DomainName.hyperstatSplitDevice)) { deviceBuilder.addMarker("node") }

        deviceBuilder.addTag("sourceModel", HStr.make(modelDef.id))
        deviceBuilder.addTag(
            "sourceModelVersion", HStr.make(
                "${modelDef.version?.major}" +
                        ".${modelDef.version?.minor}.${modelDef.version?.patch}"
            )
        )

        // If connectModule is paired with PCN then add pcnDeviceId to CN as deviceRef
        deviceRef?.let {
            deviceBuilder.addTag(Tags.DEVICE_REF, HStr.make(it))
        }

        return deviceBuilder.build()
    }

    fun buildRawPoint(modelDef: SeventyFiveFDevicePointDef, configuration: ProfileConfiguration, device: Device) : RawPoint{
        CcuLog.i(Domain.LOG_TAG, "buildRawPoint ${modelDef.domainName}")
        val pointBuilder = RawPoint.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setDeviceRef(device.id)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parsePointType(modelDef.kind.name))
            .setEnabled(false)
            .setUnit(modelDef.defaultUnit)
            .setSiteRef(device.siteRef)

        if (modelDef.valueConstraint?.constraintType == Constraint.ConstraintType.NUMERIC) {
            val constraint = modelDef.valueConstraint as NumericConstraint
            pointBuilder.setMaxVal(constraint.maxValue.toString())
            pointBuilder.setMinVal(constraint.minValue.toString())

            val incrementValTag = modelDef.presentationData?.entries?.find { it.key == "tagValueIncrement" }
            incrementValTag?.let { pointBuilder.setIncrementVal(it.value.toString()) }
        } else if (modelDef.valueConstraint?.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            val constraint = modelDef.valueConstraint as MultiStateConstraint
            val enumString = constraint.allowedValues.joinToString { it.value }
            pointBuilder.setEnums(enumString)
        }

        if (modelDef is SeventyFiveFDevicePointDef) {
            if (modelDef.hisInterpolate.name.isNotEmpty()) {
                pointBuilder.setHisInterpolate(modelDef.hisInterpolate.name.lowercase())
            }
        }

        modelDef.tags.filter { it.kind == TagType.MARKER && it.name.lowercase() != "addr"}.forEach{ pointBuilder.addMarker(it.name)}

        modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach { tag ->
            TagsUtil.getTagDefHVal(tag)?.let {
                pointBuilder.addTag(tag.name, it)
            }
        }

        modelDef.tags.filter { it.kind == TagType.STR && (it.name.lowercase() != "bacnetid" || it.name.lowercase() != "analogtype") }
            .forEach { tag ->
                tag.defaultValue?.let {
                    if (tag.name.equals(Tags.INPUT_TYPE, true)) {
                        pointBuilder.setInputType(tag.defaultValue.toString())
                    } else if (tag.name.equals(Tags.OUTPUT_TYPE, true)) {
                        pointBuilder.setOutputType(tag.defaultValue.toString())
                    } else {
                        pointBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
                    }
                }

                if(tag.name == "confirmedDateTime" || tag.name == "deliveryDateTime") {
                    val defaultVal = tag.defaultValue?.toString() ?: ""
                    pointBuilder.addTag(tag.name, HStr.make(defaultVal))
                }
            }

        modelDef.tags.filter { it.kind == TagType.BOOL && it.name != "portEnabled" }
            .forEach { tag ->
                tag.defaultValue?.let {
                    pointBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
                }
            }

        pointBuilder.addTag(Tags.TZ, HStr.make(hayStack.site?.tz))
        pointBuilder.addTag("sourcePoint", HStr.make(modelDef.id))

        val rawPoint = pointBuilder.build()
        /*If there is logical point associated with Physical point Then only enable port */
        val isEnabled = entityMapper?.let {
            updatePhysicalRef(configuration, rawPoint, it, device.equipRef)
        } ?: false

        if (isEnabled) {
            CcuLog.i(Domain.LOG_TAG, "Raw point is enabled for  ${modelDef.domainName}")
            rawPoint.enabled = true
        }

        return rawPoint

    }

    private fun updatePoints(modelDef: SeventyFiveFDeviceDirective, profileConfiguration: ProfileConfiguration, device: Device) {
        runBlocking {
            val deferredResults = modelDef.points.map { point ->
                async(highPriorityDispatcher) {
                    try {
                        val newPoint = buildRawPoint(point, profileConfiguration, device)
                        val hayStackPointDict = hayStack.readHDict("domainName == \"" + point.domainName + "\" and deviceRef == \"" + device.id + "\"")
                        val hayStackPoint = RawPoint.Builder().setHDict(hayStackPointDict).build()

                        extractAndAppendExternalEdits(point, newPoint, hayStackPointDict)

                        CcuLog.i(Domain.LOG_TAG, "updateHaystackPoint ${point.domainName}")
                        hayStack.updatePoint(newPoint, hayStackPoint.id)
                        newPoint.id = hayStackPoint.id
                        DomainManager.addRawPoint(newPoint)
                    } catch (e: Exception) {
                        CcuLog.e(Domain.LOG_TAG, "Error updating point ${point.domainName}", e)
                    }
                }
            }
            deferredResults.awaitAll()
        }
    }
    private fun updatePhysicalRef(configuration: ProfileConfiguration, rawPoint : RawPoint, entityMapper: EntityMapper , equipRef : String): Boolean {
        val logicalPointRefName = entityMapper.getPhysicalProfilePointRef(configuration, rawPoint.domainName)
        CcuLog.i(Domain.LOG_TAG, "updatePhysicalRef $logicalPointRefName")
        /*val logicalPointId = Domain.site?.floors?.get(rawPoint.floorRef)?.
                                        rooms?.get(rawPoint.roomRef)?.equips?.get(equipRef)?.
                                        points?.get(logicalPointRefName)?.id*/
        logicalPointRefName?.let {
            val logicalPoint = hayStack.readEntity("point and domainName == \"$logicalPointRefName\" " +
                                            "and equipRef == \"$equipRef\"")
            rawPoint.pointRef = logicalPoint["id"]?.toString()
            rawPoint.type= getType(logicalPointRefName, equipRef)
            return true
        }
        return false
    }

    private fun getType(domainName : String, equipRef: String) : String{
        val typeMapping = getTypePointMappingFromCmd(domainName)
        if (typeMapping.isNotEmpty()) {
            val typePoint = hayStack.readEntity(
                "point and domainName == \"$typeMapping\" " +
                        "and equipRef == \"$equipRef\""
            )
            CcuLog.i(Domain.LOG_TAG, "typePoint $typePoint")
            val enumArray = typePoint["enum"]?.toString()?.split(",")
            CcuLog.i(Domain.LOG_TAG, "enumArray $enumArray")
            if (enumArray?.isNotEmpty() == true) {
                val logicalPointVal = hayStack.readPointPriorityVal(typePoint["id"]?.toString())
                val type = enumArray[logicalPointVal.toInt()]
                CcuLog.i(Domain.LOG_TAG, "setType $type")
                return type
            }
        }
        return ""
    }

    private fun getTypePointMappingFromCmd(cmd : String) : String{
        return when(cmd) {
            "normalizedDamperCmd" -> "damperType"
            else -> {""}
        }
    }

    fun updatePoint(
        def: SeventyFiveFDevicePointDef,
        equipConfiguration: ProfileConfiguration,
        device: Device,
        existingPoint: HashMap<Any, Any>
    ) {
        val hayStackPoint = buildRawPoint(def, equipConfiguration, device)
        hayStackPoint.id = existingPoint["id"].toString()
        if (existingPoint.containsKey("pointRef")) hayStackPoint.pointRef = existingPoint["pointRef"].toString()
        if (existingPoint.containsKey("portEnabled")) hayStackPoint.enabled =
            existingPoint["portEnabled"].toString() == "true"
        if (existingPoint.containsKey("analogType")) hayStackPoint.type = existingPoint["analogType"].toString()
        if (existingPoint.containsKey("port")) hayStackPoint.port = existingPoint["port"].toString()
        if (existingPoint.containsKey("writable")) hayStackPoint.markers.add(Tags.WRITABLE)
        existingPoint["createdDateTime"]?.let {
            hayStackPoint.createdDateTime = HDateTime.make(existingPoint["createdDateTime"].toString())
        }
        hayStackPoint.lastModifiedBy = hayStack.ccuUserName

        extractAndAppendExternalEdits(def, hayStackPoint, existingPoint)

        hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())

        DomainManager.addRawPoint(hayStackPoint)
        CcuLog.i(Domain.LOG_TAG," Updated Equip point ${def.domainName}")
    }
    fun updateDevice(deviceRef: String, modelDef : SeventyFiveFDeviceDirective, deviceDis: String) {
        val deviceDict = hayStack.readHDictById(deviceRef)
        val device = Device.Builder().setHDict(deviceDict).build()
        device.domainName = modelDef.domainName
        device.displayName = deviceDis
        device.tags["sourceModel"] = HStr.make(modelDef.id)
        device.tags["sourceModelVersion"] = HStr.make(
            "${modelDef.version?.major}" +
                    ".${modelDef.version?.minor}.${modelDef.version?.patch}"
        )
        deviceDict.get("createdDateTime",false)?.let {
            device.createdDateTime = HDateTime.make(deviceDict["createdDateTime"].toString())
        }
        device.lastModifiedBy = hayStack.ccuUserName
        hayStack.updateDevice(device, device.id)
        CcuLog.i(Domain.LOG_TAG, " Updated Device ${device.addr}-${device.domainName}")
    }
    private fun createCut0verMigrationPoint(
        def: SeventyFiveFDevicePointDef,
        equipConfiguration: ProfileConfiguration,
        device: Device
    ) {
        val hayStackPoint = buildRawPoint(def, equipConfiguration, device)
        val pointId = hayStack.addPoint(hayStackPoint)
        hayStackPoint.id = pointId
        DomainManager.addRawPoint(hayStackPoint)
        CcuLog.i(Domain.LOG_TAG," Created Device point ${def.domainName}")
    }
    fun doCutOverMigration(deviceRef: String, modelDef: SeventyFiveFDeviceDirective, deviceDis: String,
                           mapping : Map<String, String>, equipConfiguration: ProfileConfiguration) {
        val devicePoints = hayStack.readAllEntities("point and deviceRef == \"$deviceRef\"")
        val deviceDict = hayStack.readHDictById(deviceRef)
        val device = Device.Builder().setHDict(deviceDict).build()
        //TODO-To be removed after testing is complete
        var update = 0
        var delete = 0
        var add = 0
        var pass = 0
        devicePoints.filter { it["domainName"] == null }
            .forEach { dbPoint ->
                val modelPointName = getDeviceDomainNameFromDis(dbPoint, mapping)

                if (modelPointName == null) {
                    delete++
                    //DB point does not exist in model. Should be deleted.
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration : Redundant Point $dbPoint")
                    hayStack.deleteEntityTree(dbPoint["id"].toString())
                } else {
                    update++
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration Update with domainName $modelPointName : $dbPoint")
                    //println("Cut-Over migration Update $dbPoint")
                    val modelPoint = modelDef.points.find { it.domainName.equals(modelPointName, true)}
                    if (modelPoint != null) {
                        updatePoint(modelPoint, equipConfiguration, device, dbPoint)
                    } else {
                        CcuLog.e(Domain.LOG_TAG, " Model point does not exist for domain name $modelPointName")
                    }
                }
            }

        modelDef.points.forEach { modelPointDef ->

            val displayName = BuildingEquipCutOverMapping.findDisFromDomainName(modelPointDef.domainName)
            if (displayName == null) {
                add++
                //Point exists in model but not in mapping table or local db. create it.
                CcuLog.e(Domain.LOG_TAG, " Cut-Over migration Add ${modelPointDef.domainName} - $modelPointDef")
                //println(" Cut-Over migration Add ${modelPointDef.domainName}- $modelPointDef")
                if (!devicePointWithDomainNameExists(devicePoints, modelPointDef.domainName, mapping)) {
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration createPoint ${modelPointDef.domainName}")
                    createCut0verMigrationPoint(modelPointDef, equipConfiguration, device)
                }
            } else {
                //TODO- Need to consider the case when point exists in map but not in DB.
                CcuLog.e(Domain.LOG_TAG, " Cut-Over migration PASS $modelPointDef")
                pass++
            }
        }

        CcuLog.e(
            Domain.LOG_TAG, "CutOver migration for ${modelDef.domainName} Total points DB: ${devicePoints.size} " +
                    " Model: ${modelDef.points.size} Map: ${mapping.size} ")
        CcuLog.e(Domain.LOG_TAG, " Deleted $delete Updated $update added $add pass $pass")

        updateDevice(deviceRef, modelDef, deviceDis)
        CcuLog.e(Domain.LOG_TAG, " Cut-Over migration completed for Device ${modelDef.domainName}")

    }

    fun createPoint(modelDef: SeventyFiveFDevicePointDef, profileConfiguration: ProfileConfiguration, device: Device, deviceDis: String) {
            val hayStackPoint = buildRawPoint(modelDef, profileConfiguration, device)
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            DomainManager.addRawPoint(hayStackPoint)
            CcuLog.d(Domain.LOG_TAG,"point created ${hayStackPoint.domainName} id = ${hayStackPoint.id} for the device: $deviceDis " )
    }

    private fun updateBaseConfigPointRefs(configuration: ProfileConfiguration, profileModelDef: SeventyFiveFProfileDirective, device : Device) {
        configuration.getBaseProfileConfigs().forEach { entityConfig ->
            val pointDef = profileModelDef.points.find { it.domainName == entityConfig.domainName }
            pointDef?.devicePointAssociation?.devicePointDomainName?.let {
                val devicePoint = hayStack.readHDict("point and domainName == \"$it\" and deviceRef == \"${device.id}\"")
                val pointEntity = RawPoint.Builder().setHDict(devicePoint).build()

                val profilePoint = hayStack.readEntity("point and domainName == \"${entityConfig.domainName}\" and equipRef == \"${device.equipRef}\"")
                val profilePointId = profilePoint["id"].toString()
                pointEntity.pointRef = profilePointId
                hayStack.updatePoint(pointEntity, pointEntity.id)
                CcuLog.i(Domain.LOG_TAG, "updateBaseConfigPointRefs point ${entityConfig.domainName}-${pointEntity.domainName} with pointRef $profilePointId")
            }
        }
    }
}