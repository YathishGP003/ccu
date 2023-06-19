package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Device
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.util.TagsUtil
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.SeventyFiveFProfilePointDef
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr

class EquipBuilder(private val hayStack : CCUHsApi) {
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective) {
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val equipRef = createEquip(modelDef, configuration)
        createPoints(modelDef, configuration, entityConfiguration, equipRef)
    }

    private fun createEquip(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration) : String{

        val equipBuilder = Equip.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setRoomRef(profileConfiguration.roomRef)
            .setFloorRef(profileConfiguration.floorRef)

        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ tag -> equipBuilder.addMarker(tag.name)}
        modelDef.tags.filter { it.kind == TagType.STR }.forEach{ tag ->
            tag.defaultValue?.let {
                equipBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
            }
        }
        modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach{ tag ->
            TagsUtil.getTagDefHVal(tag)?.let { equipBuilder.addTag(tag.name, it) }
        }
        modelDef.tags.filter { it.kind == TagType.BOOL }.forEach{ tag ->
            tag.defaultValue?.let {
                equipBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
            }
        }

        return hayStack.addEquip(equipBuilder.build())
    }

    private fun createPoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                createPoint(modelPointDef, profileConfiguration, equipRef)
            }

        }

    }
    private fun createPoint(modelDef: SeventyFiveFProfilePointDef, configuration: ProfileConfiguration, equipRef : String) : String{

        //TODO - Ref validation, zone/system equip differentiator.
        val pointBuilder = Point.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setEquipRef(equipRef)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parsePointType(modelDef.kind.name))
            .setUnit(modelDef.defaultUnit)


        //TODO - Support added for currently used tag types. Might need updates in future.
        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ pointBuilder.addMarker(it.name)}
        modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach{ tag ->
            TagsUtil.getTagDefHVal(tag)?.let { pointBuilder.addTag(tag.name, it) }
        }

        modelDef.tags.filter { it.kind == TagType.STR }.forEach{ tag ->
            tag.defaultValue?.let {
                pointBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
            }
        }
        modelDef.tags.filter { it.kind == TagType.BOOL }.forEach{ tag ->
            tag.defaultValue?.let {
                pointBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
            }
        }

        return hayStack.addPoint(pointBuilder.build())
    }

    private fun createDevice(modelDef: SeventyFiveFProfileDirective) : String{

        val deviceBuilder = Device.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
        modelDef.tagNames.forEach{ deviceBuilder.addMarker(it)}
        return hayStack.addDevice(deviceBuilder.build())
    }

    private fun createRawPoint(modelDef: SeventyFiveFProfilePointDef, configuration: ProfileConfiguration, deviceRef : String) : String{

        val pointBuilder = RawPoint.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setDeviceRef(deviceRef)
            .setRoomRef(configuration.roomRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parsePointType(modelDef.kind.name))
            .setUnit(modelDef.defaultUnit)


        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ pointBuilder.addMarker(it.name)}
        return hayStack.addPoint(pointBuilder.build())
    }
}