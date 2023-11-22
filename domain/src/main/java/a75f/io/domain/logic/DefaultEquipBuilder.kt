package a75f.io.domain.logic

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.util.TagsUtil
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr

/**
 * A common implementation of EquipBuilder interface with a generic equip/point build support.
 */
open class DefaultEquipBuilder : EquipBuilder {

    override fun buildEquip(equipConfig : EquipBuilderConfig) : Equip {
        CcuLog.i(Domain.LOG_TAG, "buildEquip ${equipConfig.modelDef.domainName}")
        val equipBuilder = Equip.Builder().setDisplayName("${equipConfig.disPrefix}-${equipConfig.modelDef.name}")
            .setDomainName(equipConfig.modelDef.domainName)
            .setFloorRef(equipConfig.profileConfiguration?.floorRef)
            .setGroup(equipConfig.profileConfiguration?.nodeAddress.toString())
            .setSiteRef(equipConfig.siteRef)

        if (equipConfig.profileConfiguration?.roomRef != null) {
            equipBuilder.setRoomRef(equipConfig.profileConfiguration.roomRef)
        }

        equipConfig.modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ tag -> equipBuilder.addMarker(tag.name)}
        equipConfig.modelDef.tags.filter { it.kind == TagType.STR }.forEach{ tag ->
            tag.defaultValue?.let {
                equipBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
            }
        }
        equipConfig.modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach{ tag ->
            TagsUtil.getTagDefHVal(tag)?.let { equipBuilder.addTag(tag.name, it) }
        }
        equipConfig.modelDef.tags.filter { it.kind == TagType.BOOL }.forEach{ tag ->
            tag.defaultValue?.let {
                equipBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
            }
        }

        equipBuilder.addTag("modelId", HStr.make(equipConfig.modelDef.id))
        equipBuilder.addTag("modelVersion", HStr.make("${equipConfig.modelDef.version?.major}" +
                ".${equipConfig.modelDef.version?.minor}.${equipConfig.modelDef.version?.patch}"))
        equipBuilder.setTz(equipConfig.tz)
        return equipBuilder.build()
    }

    override fun buildPoint(pointConfig : PointBuilderConfig) : Point {
        CcuLog.i(Domain.LOG_TAG, "buildPoint ${pointConfig.modelDef.domainName}")
        //TODO - Ref validation, zone/system equip differentiator.
        val pointBuilder = Point.Builder().setDisplayName("${pointConfig.disPrefix}-${pointConfig.modelDef.name}")
            .setDomainName(pointConfig.modelDef.domainName)
            .setEquipRef(pointConfig.equipRef)
            .setFloorRef(pointConfig.configuration?.floorRef)
            .setKind(Kind.parsePointType(pointConfig.modelDef.kind.name))
            .setUnit(pointConfig.modelDef.defaultUnit)
            .setGroup(pointConfig.configuration?.nodeAddress.toString())
            .setSiteRef(pointConfig.siteRef)

        if (pointConfig.configuration?.roomRef != null) {
            pointBuilder.setRoomRef(pointConfig.configuration.roomRef)
        }

        //TODO - Support added for currently used tag types. Might need updates in future.
        pointConfig.modelDef.tags.filter { it.kind == TagType.MARKER && it.name.lowercase() != "tz"}.forEach{ pointBuilder.addMarker(it.name)}
        pointConfig.modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach{ tag ->
            TagsUtil.getTagDefHVal(tag)?.let { pointBuilder.addTag(tag.name, it) }
        }

        pointConfig.modelDef.tags.filter { it.kind == TagType.STR }.forEach{ tag ->
            tag.defaultValue?.let {
                pointBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
            }
        }
        pointConfig.modelDef.tags.filter { it.kind == TagType.BOOL }.forEach{ tag ->
            tag.defaultValue?.let {
                pointBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
            }
        }
        if (pointConfig.modelDef.tags.find { it.name == Tags.HIS } != null /*&& pointConfig.modelDef.tags.find { it.name == Tags.TZ } == null*/) {
            pointConfig.tz.let { pointBuilder.addTag(Tags.TZ, HStr.make(pointConfig.tz)) }
        }

        return pointBuilder.build()
    }
}