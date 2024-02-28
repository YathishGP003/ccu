package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.cutover.BuildingEquipCutOverMapping
import a75f.io.domain.cutover.devicePointWithDomainNameExists
import a75f.io.domain.cutover.getDeviceDomainNameFromDis
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDevicePointDef
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HStr
import kotlin.math.log

class DeviceBuilder(private val hayStack : CCUHsApi, private val entityMapper: EntityMapper) {
    fun buildDeviceAndPoints(configuration: ProfileConfiguration, modelDef: SeventyFiveFDeviceDirective, equipRef: String, siteRef : String, deviceDis: String) {
        CcuLog.i(Domain.LOG_TAG, "buildDeviceAndPoints $configuration")
        val hayStackDevice = buildDevice(modelDef, configuration, equipRef, siteRef, deviceDis)
        val deviceId = hayStack.addDevice(hayStackDevice)
        hayStackDevice.id = deviceId
        DomainManager.addDevice(hayStackDevice)
        createPoints(modelDef, configuration, hayStackDevice, deviceDis)
    }

    fun updateDeviceAndPoints(configuration: ProfileConfiguration, modelDef: SeventyFiveFDeviceDirective, equipRef: String, siteRef : String, deviceDis: String) {

        val hayStackDevice = buildDevice(modelDef, configuration, equipRef, siteRef, deviceDis)

        val device = hayStack.readEntity(
            "device and addr == \"${configuration.nodeAddress}\"")

        val deviceId =  device["id"].toString()
        hayStack.updateDevice(hayStackDevice, deviceId)

        hayStackDevice.id = deviceId
        DomainManager.addDevice(hayStackDevice)
        updatePoints(modelDef, configuration, hayStackDevice, deviceDis)
    }

    private fun createPoints(modelDef: SeventyFiveFDeviceDirective, profileConfiguration: ProfileConfiguration, device: Device, deviceDis: String) {

        modelDef.points.forEach {
            val hayStackPoint = buildRawPoint(it, profileConfiguration, device, deviceDis)
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            DomainManager.addRawPoint(hayStackPoint)
        }
    }
    private fun buildDevice(modelDef: SeventyFiveFDeviceDirective, configuration: ProfileConfiguration, equipRef: String, siteRef : String, deviceDis: String) : Device{
        CcuLog.i(Domain.LOG_TAG, "buildDevice ${modelDef.domainName}")
        val deviceBuilder = Device.Builder().setDisplayName(deviceDis)
            .setDomainName(modelDef.domainName)
            .setEquipRef(equipRef)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setAddr(configuration.nodeAddress)
            .setSiteRef(siteRef)
        modelDef.tags.forEach{ deviceBuilder.addMarker(it.name)}
        return deviceBuilder.build()
    }

    private fun buildRawPoint(modelDef: SeventyFiveFDevicePointDef, configuration: ProfileConfiguration, device: Device, deviceDis: String) : RawPoint{
        CcuLog.i(Domain.LOG_TAG, "buildRawPoint ${modelDef.domainName}")
        val pointBuilder = RawPoint.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setDeviceRef(device.id)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parsePointType(modelDef.kind.name))
            .setEnabled(true)
            .setUnit(modelDef.defaultUnit)
            .setSiteRef(device.siteRef)

        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ pointBuilder.addMarker(it.name)}
        if (modelDef.tags.find { it.name == Tags.HIS } != null) {
            pointBuilder.addTag(Tags.TZ, HStr.make(hayStack.site?.tz))
        }

        val rawPoint = pointBuilder.build()
        updatePhysicalRef(configuration, rawPoint, entityMapper, device.equipRef, deviceDis)

        return rawPoint

    }

    private fun updatePoints(modelDef: SeventyFiveFDeviceDirective, profileConfiguration: ProfileConfiguration, device: Device, deviceDis: String) {
        modelDef.points.forEach {
            val newPoint = buildRawPoint(it, profileConfiguration, device, deviceDis)
            val hayStackPointDict = hayStack.readHDict("domainName == \""+it.domainName+"\" and deviceRef == \""+device.id+"\"")
            val hayStackPoint = Point.Builder().setHDict(hayStackPointDict).build()
            if (!hayStackPoint.equals(newPoint)) {
                hayStack.updatePoint(newPoint, hayStackPoint.id)
                newPoint.id = hayStackPoint.id
                DomainManager.addRawPoint(newPoint)
            }
        }
    }
    private fun updatePhysicalRef(configuration: ProfileConfiguration, rawPoint : RawPoint, entityMapper: EntityMapper , equipRef : String, deviceDis: String) {
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

        }
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

    private fun updatePoint(def : SeventyFiveFDevicePointDef, equipConfiguration : ProfileConfiguration, device: Device, deviceDis: String, existingPoint : HashMap<Any, Any>) {
        val hayStackPoint = buildRawPoint(def, equipConfiguration, device, deviceDis)
        hayStackPoint.id = existingPoint["id"].toString()
        if (existingPoint.containsKey("pointRef")) hayStackPoint.pointRef = existingPoint["pointRef"].toString()
        if (existingPoint.containsKey("portEnabled")) hayStackPoint.enabled = existingPoint["portEnabled"].toString().equals("true")
        if (existingPoint.containsKey("analogType")) hayStackPoint.type = existingPoint["analogType"].toString()

        hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())

        DomainManager.addRawPoint(hayStackPoint)
        CcuLog.i(Domain.LOG_TAG," Updated Equip point ${def.domainName}")
    }
    private fun updateDevice(deviceRef: String, modelDef : SeventyFiveFDeviceDirective, deviceDis: String) {
        var deviceDict = hayStack.readHDictById(deviceRef)
        val device = Device.Builder().setHDict(deviceDict).build()
        device.domainName = modelDef.domainName
        device.displayName = deviceDis
        device.tags["sourceModel"] = HStr.make(modelDef.id)
        device.tags["sourceModelVersion"] = HStr.make(
            "${modelDef.version?.major}" +
                    ".${modelDef.version?.minor}.${modelDef.version?.patch}"
        )
        hayStack.updateDevice(device, device.id)
        CcuLog.i(Domain.LOG_TAG, " Updated Device ${device.addr}-${device.domainName}")
    }
    private fun createCutoverMigrationPoint(def : SeventyFiveFDevicePointDef, equipConfiguration : ProfileConfiguration, device: Device, deviceDis: String) {
        val hayStackPoint = buildRawPoint(def, equipConfiguration, device, deviceDis)
        val pointId = hayStack.addPoint(hayStackPoint)
        hayStackPoint.id = pointId
        DomainManager.addRawPoint(hayStackPoint)
        CcuLog.i(Domain.LOG_TAG," Created Device point ${def.domainName}")
    }
    fun doCutOverMigration(deviceRef: String, modelDef: SeventyFiveFDeviceDirective, deviceDis: String,
                           mapping : Map<String, String>, equipConfiguration: ProfileConfiguration) {
        var devicePoints = hayStack.readAllEntities("point and deviceRef == \"$deviceRef\"")
        val site = hayStack.site
        var deviceDict = hayStack.readHDictById(deviceRef)
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
                    //hayStack.deleteEntityTree(dbPoint["id"].toString())
                } else {
                    update++
                    CcuLog.e(Domain.LOG_TAG, " Cut-Over migration Update with domainName $modelPointName : $dbPoint")
                    //println("Cut-Over migration Update $dbPoint")
                    val modelPoint = modelDef.points.find { it.domainName.equals(modelPointName, true)}
                    if (modelPoint != null) {
                        updatePoint(modelPoint, equipConfiguration, device, deviceDis, dbPoint)
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
                    createCutoverMigrationPoint(modelPointDef, equipConfiguration, device, deviceDis)
                }
            } else {
                //TODO- Need to consider the case when point exists in map but not in DB.
                CcuLog.e(Domain.LOG_TAG, " Cut-Over migration PASS $modelPointDef")
                //println(" Cut-Over migration PASS $modelPointDef")
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

}