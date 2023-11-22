package a75f.io.domain.logic

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.util.TagsUtil
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr

/**
 * A common implementation of EquipBuilder interface with a generic equip/point build support.
 */
open class DefaultEquipBuilder : EquipBuilder {

    override fun buildEquip(equipConfig : EquipBuilderConfig, profileName: String?) : Equip {

        val equipBuilder = Equip.Builder().setDisplayName("${equipConfig.disPrefix}-${equipConfig.modelDef.name}")
            .setDomainName(equipConfig.modelDef.domainName)
            .setFloorRef(equipConfig.profileConfiguration?.floorRef)
            .setGroup(equipConfig.profileConfiguration?.nodeAddress.toString())
            .setSiteRef(equipConfig.siteRef)
            .setDomainName(equipConfig.modelDef.domainName)

        if (equipConfig.profileConfiguration?.roomRef != null) {
            equipBuilder.setRoomRef(equipConfig.profileConfiguration.roomRef)
        }
        if (profileName != null)
            equipBuilder.setProfile(profileName)

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

        //TODO - Ref validation, zone/system equip differentiator.
        val pointBuilder = Point.Builder().setDisplayName("${pointConfig.disPrefix}-${pointConfig.modelDef.name}")
            .setDomainName(pointConfig.modelDef.domainName)
            .setEquipRef(pointConfig.equipRef)
            .setFloorRef(pointConfig.configuration?.floorRef)
            .setKind(Kind.parsePointType(pointConfig.modelDef.kind.name))
            .setUnit(pointConfig.modelDef.defaultUnit)
            .setGroup(pointConfig.configuration?.nodeAddress.toString())
            .setSiteRef(pointConfig.siteRef)
            .setHisInterpolate("cov")
        if (pointConfig.configuration?.roomRef != null) {
            pointBuilder.setRoomRef(pointConfig.configuration.roomRef)
        }

        if (pointConfig.modelDef.valueConstraint?.constraintType == Constraint.ConstraintType.NUMERIC) {
            val constraint = pointConfig.modelDef.valueConstraint as NumericConstraint
            pointBuilder.setMaxVal(constraint.maxValue.toString())
            pointBuilder.setMinVal(constraint.minValue.toString())

            val incrementValTag = pointConfig.modelDef.presentationData?.entries?.find { it.key == "tagValueIncrement" }
            incrementValTag?.let { pointBuilder.setIncrementVal(it.value.toString()) }
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
        pointConfig.tz.let { pointBuilder.addTag(Tags.TZ, HStr.make(pointConfig.tz)) }
        /* Log.i("DEV_DEBUG", "buildPoint: Domain Name : ${pointConfig.modelDef.domainName} received ${HStr.make(pointConfig.tz)} ")
        if (pointConfig.modelDef.tags.find { it.name == Tags.HIS } != null && pointConfig.modelDef.tags.find { it.name == Tags.TZ } == null) {
            pointConfig.tz.let { pointBuilder.addTag(Tags.TZ, HStr.make(pointConfig.tz)) }
            Log.i("DEV_DEBUG", "buildPoint: TZ added ${HStr.make(pointConfig.tz)} ")
        }*/

        var enums = ""
        if (pointConfig.modelDef.valueConstraint.constraintType.name.contentEquals("MULTI_STATE")) {
            (pointConfig.modelDef.valueConstraint as MultiStateConstraint).allowedValues.forEachIndexed { index, value->
                enums = if (enums.isNotEmpty()) {
                    "$enums$index=${value.value},"
                } else {
                    "$index=${value.value},"
                }
            }
        }
        if (enums.isNotEmpty()) {
            if (enums.endsWith(","))
                enums = enums.substring(0, enums.length - 1)
            pointBuilder.setEnums(enums)
        }
        return pointBuilder.build()
    }
}