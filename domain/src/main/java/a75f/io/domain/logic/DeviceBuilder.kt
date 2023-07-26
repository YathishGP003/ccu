package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.config.ProfileConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDevicePointDef
import io.seventyfivef.ph.core.TagType

class DeviceBuilder(private val hayStack : CCUHsApi, private val entityMapper: EntityMapper) {

    fun buildDeviceAndPoints(configuration: ProfileConfiguration, modelDef: SeventyFiveFDeviceDirective, equipRef: String) {

        val hayStackDevice = buildDevice(modelDef, configuration, equipRef)
        val deviceId = hayStack.addDevice(hayStackDevice)
        hayStackDevice.id = deviceId
        DomainManager.addDevice(hayStackDevice)
        createPoints(modelDef, configuration, hayStackDevice)
    }

    fun updateDeviceAndPoints(configuration: ProfileConfiguration, modelDef: SeventyFiveFDeviceDirective, equipRef: String) {

        val hayStackDevice = buildDevice(modelDef, configuration, equipRef)

        val device = hayStack.readEntity(
            "device and addr == \"${configuration.nodeAddress}\"")

        val deviceId =  device["id"].toString()
        hayStack.updateDevice(hayStackDevice, deviceId)

        hayStackDevice.id = deviceId
        DomainManager.addDevice(hayStackDevice)
        updatePoints(modelDef, configuration, hayStackDevice)
    }

    private fun createPoints(modelDef: SeventyFiveFDeviceDirective, profileConfiguration: ProfileConfiguration, device: Device) {

        modelDef.points.forEach {
            val hayStackPoint = buildRawPoint(it, profileConfiguration, device)
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            DomainManager.addRawPoint(hayStackPoint)
        }
    }
    private fun buildDevice(modelDef: SeventyFiveFDeviceDirective, configuration: ProfileConfiguration, equipRef: String) : Device{

        val deviceBuilder = Device.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setEquipRef(equipRef)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setAddr(configuration.nodeAddress)
        modelDef.tagNames.forEach{ deviceBuilder.addMarker(it)}
        return deviceBuilder.build()
    }

    private fun buildRawPoint(modelDef: SeventyFiveFDevicePointDef, configuration: ProfileConfiguration, device: Device) : RawPoint{

        val pointBuilder = RawPoint.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setDeviceRef(device.id)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parsePointType(modelDef.kind.name))
            .setEnabled(true)
            .setUnit(modelDef.defaultUnit)


        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ pointBuilder.addMarker(it.name)}

        val rawPoint = pointBuilder.build()
        updatePhysicalRef(configuration, rawPoint, entityMapper, device.equipRef)

        return rawPoint

    }

    private fun updatePoints(modelDef: SeventyFiveFDeviceDirective, profileConfiguration: ProfileConfiguration, device: Device) {
        modelDef.points.forEach {
            val newPoint = buildRawPoint(it, profileConfiguration, device)
            val hayStackPointDict = hayStack.readHDict("domainName == \""+it.domainName+"\" and deviceRef == \""+device.id+"\"")
            val hayStackPoint = Point.Builder().setHDict(hayStackPointDict).build()
            if (!hayStackPoint.equals(newPoint)) {
                hayStack.updatePoint(newPoint, hayStackPoint.id)
                newPoint.id = hayStackPoint.id
                DomainManager.addRawPoint(newPoint)
            }
        }
    }
    private fun updatePhysicalRef(configuration: ProfileConfiguration, rawPoint : RawPoint, entityMapper: EntityMapper , equipRef : String) {
        val logicalPointRefName = entityMapper.getPhysicalProfilePointRef(configuration, rawPoint.domainName)

        /*val logicalPointId = Domain.site?.floors?.get(rawPoint.floorRef)?.
                                        rooms?.get(rawPoint.roomRef)?.equips?.get(equipRef)?.
                                        points?.get(logicalPointRefName)?.id*/
        logicalPointRefName?.let {
            val logicalPointId = hayStack.readEntity("point and domainName == \"$logicalPointRefName\"")
            rawPoint.pointRef = logicalPointId["id"]?.toString()
        }
    }
}