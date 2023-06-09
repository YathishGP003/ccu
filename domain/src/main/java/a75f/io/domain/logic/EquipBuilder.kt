package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.ModelPointDef

class EquipBuilder(val hayStack : CCUHsApi) {
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDef) {
        val entityMapper = EntityMapper(modelDef)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        createEquip(modelDef)
    }

    private fun createEquip(modelDef: ModelDef) {

        val equipBuilder = Equip.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
        modelDef.tagNames.forEach{ equipBuilder.addMarker(it)}
        hayStack.addEquip(equipBuilder.build())
    }

    private fun createPoint(modelDef: ModelPointDef, equipRef : String, roomRef : String, floorRef : String) : String{

        val pointBuilder = Point.Builder().setDisplayName(modelDef.displayName)
            .setDomainName(modelDef.domainName)
            .setEquipRef(equipRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setKind(Kind.parse(modelDef.pointType.name))
            .setUnit(modelDef.defaultUnit)


        modelDef.tagNames.forEach{ pointBuilder.addMarker(it)}
        return hayStack.addPoint(pointBuilder.build())
    }

    private fun createRawPoint(modelDef: ModelPointDef, deviceRef : String, roomRef : String, floorRef : String) : String{

        val pointBuilder = RawPoint.Builder().setDisplayName(modelDef.displayName)
            .setDomainName(modelDef.domainName)
            .setDeviceRef(deviceRef)
            .setRoomRef(roomRef)
            .setFloorRef(floorRef)
            .setKind(Kind.parse(modelDef.pointType.name))
            .setUnit(modelDef.defaultUnit)


        modelDef.tagNames.forEach{ pointBuilder.addMarker(it)}
        return hayStack.addPoint(pointBuilder.build())
    }
}