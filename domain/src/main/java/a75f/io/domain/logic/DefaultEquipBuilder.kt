package a75f.io.domain.logic

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.util.TagsUtil
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr

/**
 * A common implementation of EquipBuilder interface with a generic equip/point build support.
 */
open class DefaultEquipBuilder : EquipBuilder {

    override fun buildEquip(modelDef: ModelDirective, profileConfiguration: ProfileConfiguration?) : Equip {

        val equipBuilder = Equip.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setFloorRef(profileConfiguration?.floorRef)
            .setGroup(profileConfiguration?.nodeAddress.toString())

        if (profileConfiguration?.roomRef != null) {
            equipBuilder.setRoomRef(profileConfiguration.roomRef)
        }

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

    override fun buildPoint(modelDef: ModelPointDef, configuration: ProfileConfiguration?, equipRef : String) : Point {

        //TODO - Ref validation, zone/system equip differentiator.
        val pointBuilder = Point.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setEquipRef(equipRef)
            .setFloorRef(configuration?.floorRef)
            .setKind(Kind.parsePointType(modelDef.kind.name))
            .setUnit(modelDef.defaultUnit)
            .setGroup(configuration?.nodeAddress.toString())

        if (configuration?.roomRef != null) {
            pointBuilder.setRoomRef(configuration.roomRef)
        }

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
}