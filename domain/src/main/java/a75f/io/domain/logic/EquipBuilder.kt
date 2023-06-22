package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.util.TagsUtil
import a75f.io.domain.util.TunerUtil
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr

class EquipBuilder(private val hayStack : CCUHsApi) {
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val hayStackEquip = buildEquip(modelDef, configuration)
        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, entityConfiguration, equipId)

        return equipId
    }

    private fun buildEquip(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration) : Equip{

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

        return equipBuilder.build()
    }

    private fun createPoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, profileConfiguration, equipRef)
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
                if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                    initializeDefaultVal(hayStackPoint, modelPointDef.defaultValue as Number)
                }
                DomainManager.addPoint(hayStackPoint)
            }

        }
    }
    private fun buildPoint(modelDef: SeventyFiveFProfilePointDef, configuration: ProfileConfiguration, equipRef : String) : Point{

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

        return pointBuilder.build()
    }

    private fun initializeDefaultVal(point : Point, defaultVal : Number) {
        when{
            point.markers.contains("config") /*&& point.defaultVal is String*/-> hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
            point.markers.contains("tuner") -> TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
        }
    }
}