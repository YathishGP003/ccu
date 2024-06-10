package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.BuildConfig
import a75f.io.domain.api.Domain
import a75f.io.domain.util.TagsUtil
import a75f.io.logger.CcuLog
import android.annotation.SuppressLint
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerPointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr
import java.util.Locale

/**
 * A common implementation of EquipBuilder interface with a generic equip/point build support.
 */
open class DefaultEquipBuilder : EquipBuilder {

    override fun buildEquip(equipConfig : EquipBuilderConfig) : Equip {
        val siteDisName = hayStack.site?.displayName
        val equipBuilder =
            Equip.Builder().setDisplayName("$siteDisName-${equipConfig.modelDef.name}")
                .setDomainName(equipConfig.modelDef.domainName)
                .setFloorRef(equipConfig.profileConfiguration?.floorRef)
                .setSiteRef(equipConfig.siteRef)
                .setProfile(equipConfig.profileConfiguration?.profileType)
                .setDomainName(equipConfig.modelDef.domainName)

        if (!equipConfig.modelDef.name.equals("buildingEquip")) { equipBuilder.setDisplayName(equipConfig.disPrefix) }

        if (equipConfig.profileConfiguration?.roomRef != null) {
            equipBuilder.setRoomRef(equipConfig.profileConfiguration.roomRef)
        }
        equipConfig.profileConfiguration?.nodeAddress?.let {
            if (it > 0) {
                equipBuilder.setGroup(equipConfig.profileConfiguration?.nodeAddress.toString())
            }
        }

        equipConfig.modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ tag -> equipBuilder.addMarker(tag.name)}
        equipConfig.modelDef.tags.filter { it.kind == TagType.STR }.forEach{ tag ->
            tag.defaultValue?.let {
                //TODO- Temp Sam : This should be removed in future when profile models consistent with profile tag.
                // We will stick with old profileType enum till then
                if (tag.name != "profile") {
                    equipBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
                }
            }
        }
        equipConfig.modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach { tag ->
            TagsUtil.getTagDefHVal(tag)?.let { equipBuilder.addTag(tag.name, it) }
        }
        equipConfig.modelDef.tags.filter { it.kind == TagType.BOOL }.forEach { tag ->
            tag.defaultValue?.let {
                equipBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
            }
        }

        equipBuilder.addTag("sourceModel", HStr.make(equipConfig.modelDef.id))
        equipBuilder.addTag(
            "sourceModelVersion", HStr.make(
                "${equipConfig.modelDef.version?.major}" +
                        ".${equipConfig.modelDef.version?.minor}.${equipConfig.modelDef.version?.patch}"
            )
        )
        equipBuilder.setTz(equipConfig.tz)
        return equipBuilder.build()
    }

    @SuppressLint("SuspiciousIndentation")
    override fun buildPoint(pointConfig: PointBuilderConfig): Point {
        CcuLog.i(Domain.LOG_TAG, "buildPoint ${pointConfig.modelDef.domainName}")
        //TODO - Ref validation, zone/system equip differentiator.
        val pointBuilder = Point.Builder().setDisplayName("${pointConfig.disPrefix}-${getDisplayNameFromVariation(pointConfig.modelDef.name)}")
            .setDomainName(pointConfig.modelDef.domainName)
            .setEquipRef(pointConfig.equipRef)
            .setFloorRef(pointConfig.configuration?.floorRef)
            .setKind(Kind.parsePointType(pointConfig.modelDef.kind.name))
            .setUnit(pointConfig.modelDef.defaultUnit)
            .setSiteRef(pointConfig.siteRef)

        if (pointConfig.modelDef is SeventyFiveFProfilePointDef) {
            if (pointConfig.modelDef.hisInterpolate.name.isNotEmpty()) {
                pointBuilder.setHisInterpolate(
                    pointConfig.modelDef.hisInterpolate.name.lowercase(Locale.ROOT)
                )
            }
        }

        if (pointConfig.configuration?.roomRef != null) {
            pointBuilder.setRoomRef(pointConfig.configuration.roomRef)
        }
        pointConfig.configuration?.nodeAddress?.let {
            if (it > 0) {
                pointBuilder.setGroup(pointConfig.configuration?.nodeAddress.toString())
            }
        }

        if (pointConfig.modelDef.valueConstraint.constraintType == Constraint.ConstraintType.NUMERIC) {
            val constraint = pointConfig.modelDef.valueConstraint as NumericConstraint
            pointBuilder.setMaxVal(constraint.maxValue.toString())
            pointBuilder.setMinVal(constraint.minValue.toString())

            val incrementValTag =
                pointConfig.modelDef.presentationData?.entries?.find { it.key == "tagValueIncrement" }
            incrementValTag?.let { pointBuilder.setIncrementVal(it.value.toString()) }
        } else if (pointConfig.modelDef.valueConstraint?.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            val constraint = pointConfig.modelDef.valueConstraint as MultiStateConstraint
            val enumString = constraint.allowedValues.joinToString { it.value }
            pointBuilder.setEnums(enumString)
        }

        if (pointConfig.modelDef is SeventyFiveFTunerPointDef) {
            if (pointConfig.modelDef.hisInterpolate.name.isNotEmpty()) {
                pointBuilder.setHisInterpolate(pointConfig.modelDef.hisInterpolate.name.lowercase())
            }
        }

        if (pointConfig.modelDef is SeventyFiveFProfilePointDef) {
            if (pointConfig.modelDef.hisInterpolate.name.isNotEmpty()) {
                pointBuilder.setHisInterpolate(pointConfig.modelDef.hisInterpolate.name.lowercase())
            }
        }

        //TODO - Support added for currently used tag types. Might need updates in future.
        pointConfig.modelDef.tags.filter { it.kind == TagType.MARKER && it.name.lowercase() != "tz" }
            .forEach { pointBuilder.addMarker(it.name) }
        pointConfig.modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach { tag ->
            TagsUtil.getTagDefHVal(tag)?.let { pointBuilder.addTag(tag.name, it) }
        }



        pointConfig.modelDef.tags.filter { it.kind == TagType.NUMBER && it.name.lowercase() == "bacnetid" }.forEach{ tag ->
            tag.defaultValue?.let {
                val smartNodeAddressBand = getSmartNodeBand()?.toInt()
                if(smartNodeAddressBand != null && pointConfig.configuration?.nodeAddress != null) {
                    val nodeAdd = pointConfig.configuration.nodeAddress - smartNodeAddressBand + 1000
                    val bacnetId = "$nodeAdd${tag.defaultValue.toString().toInt()}"
                    pointBuilder.setBacnetId(bacnetId.toInt())
                }
            }
        }

        pointConfig.modelDef.tags.filter { it.kind == TagType.STR && it.name.lowercase() == "bacnettype" }.forEach{ tag ->
            tag.defaultValue?.let {
                val bacnetType = tag.defaultValue.toString()
                pointBuilder.setBacnetType(bacnetType)
            }
        }

        pointConfig.modelDef.tags.filter { it.kind == TagType.STR && it.name.lowercase() != "bacnetid" }.forEach{ tag ->
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
        pointBuilder.addTag("sourcePoint", HStr.make(pointConfig.modelDef.id))

        var enums = ""
        if (pointConfig.modelDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            (pointConfig.modelDef.valueConstraint as MultiStateConstraint).allowedValues.forEachIndexed { index, value ->
                enums = if (enums.isNotEmpty()) {
                    "$enums${value.value}=$index,"
                } else {
                    "${value.value}=$index,"
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

    private fun getDisplayNameFromVariation(dis: String): String? {
        var displayName = ""
        displayName =
            if (BuildConfig.BUILD_TYPE.equals(
                    "carrier_prod",
                    ignoreCase = true
                )
            ) {
                dis.replace("(?i)-DAB-".toRegex(), "-VVT-")
            } else {
                dis
            }
        return displayName
    }

    open fun getSmartNodeBand(): String? {
        val device = CCUHsApi.getInstance().readEntity("device and addr")
        CcuLog.i(Domain.LOG_TAG, "Deviceband $device")
        if (device != null && device.size > 0 && device["modbus"] == null && device["addr"] != null) {
            val nodeAdd = device["addr"].toString()
            return nodeAdd.substring(0, nodeAdd.length - 2) + "00"
        } else {
            val band = CCUHsApi.getInstance().readEntity("point and snband")
            CcuLog.i(Domain.LOG_TAG, "Deviceband $device")
            if (band != null && band.size > 0 && band["val"] != null) {
                return band["val"].toString()
            }
        }
        return null
    }
}
