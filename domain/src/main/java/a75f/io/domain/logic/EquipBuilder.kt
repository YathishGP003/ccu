package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.ModelPointDef
import a75f.io.domain.model.ph.core.TagType

class EquipBuilder(private val hayStack : CCUHsApi) {
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDef) {
        val entityMapper = EntityMapper(modelDef)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val equipRef = createEquip(modelDef, configuration)
        createPoints(modelDef, configuration, entityConfiguration, equipRef)
    }

    private fun createEquip(modelDef: ModelDef, profileConfiguration: ProfileConfiguration) : String{

        val equipBuilder = Equip.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setRoomRef(profileConfiguration.roomRef)
            .setFloorRef(profileConfiguration.floorRef)
        modelDef.tagNames.forEach{ equipBuilder.addMarker(it)}
        return hayStack.addEquip(equipBuilder.build())
    }

    private fun createPoints(modelDef: ModelDef, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                createPoint(modelPointDef, profileConfiguration, equipRef)
            }

        }

    }
    private fun createPoint(modelDef: ModelPointDef, configuration: ProfileConfiguration, equipRef : String) : String{

        //TODO - Ref validation, zone/system equip differentiator.
        val pointBuilder = Point.Builder().setDisplayName(modelDef.displayName)
            .setDomainName(modelDef.domainName)
            .setEquipRef(equipRef)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parse(modelDef.pointType.name))
            .setUnit(modelDef.defaultUnit)


        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ pointBuilder.addMarker(it.name)}

        //TODO- Support for other tag types to be added. Existing point structure does not support arbitrary KVP.
        return hayStack.addPoint(pointBuilder.build())
    }

    private fun createDevice(modelDef: ModelDef) : String{

        val deviceBuilder = Device.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
        modelDef.tagNames.forEach{ deviceBuilder.addMarker(it)}
        return hayStack.addDevice(deviceBuilder.build())
    }

    private fun createRawPoint(modelDef: ModelPointDef, configuration: ProfileConfiguration, deviceRef : String) : String{

        val pointBuilder = RawPoint.Builder().setDisplayName(modelDef.displayName)
            .setDomainName(modelDef.domainName)
            .setDeviceRef(deviceRef)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parse(modelDef.pointType.name))
            .setUnit(modelDef.defaultUnit)


        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ pointBuilder.addMarker(it.name)}
        return hayStack.addPoint(pointBuilder.build())
    }
}