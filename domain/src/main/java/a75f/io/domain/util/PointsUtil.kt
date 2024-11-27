package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr

class PointsUtil(private val hayStack : CCUHsApi) {

    fun createDynamicSensorEquipPoint(equip: Equip, domainName: String, config: ProfileConfiguration): String? {

        val tz = hayStack.timeZone
        CcuLog.i(Domain.LOG_TAG, "add Dynamic Sensor Equip point - $domainName")

        val sensor = hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"${equip.id}\"")
        if (sensor.isNotEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "$domainName is already present for equip ${equip.equipRef}")
            return sensor[Tags.ID].toString()
        }
        val modelDef = getModelFromEquip(equip)
        val modelPointDef = modelDef?.points?.find { it.domainName == domainName }
        modelPointDef?.run {
            val hayStackPoint = buildPoint(PointBuilderConfig(modelPointDef, config, equip.id, equip.siteRef, tz, equip.displayName))
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            DomainManager.addPoint(hayStackPoint)
            return hayStackPoint.id
        }
        return null
    }

    fun getModelFromEquip(equip: Equip): SeventyFiveFProfileDirective? {
        return when (equip.domainName) {
            DomainName.smartnodeDAB -> ModelLoader.getSmartNodeDabModel() as SeventyFiveFProfileDirective
            DomainName.helionodeDAB -> ModelLoader.getHelioNodeDabModel() as SeventyFiveFProfileDirective
            DomainName.smartnodeVAVReheatNoFan -> ModelLoader.getSmartNodeVavNoFanModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeVAVReheatParallelFan -> ModelLoader.getSmartNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeVAVReheatSeriesFan -> ModelLoader.getSmartNodeVavSeriesModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeActiveChilledBeam -> ModelLoader.getSmartNodeVavAcbModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeVAVReheatNoFan -> ModelLoader.getHelioNodeVavNoFanModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeVAVReheatParallelFan -> ModelLoader.getHelioNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeVAVReheatSeriesFan -> ModelLoader.getHelioNodeVavSeriesModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeActiveChilledBeam -> ModelLoader.getHelioNodeVavAcbModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeBypassDamper -> ModelLoader.getSmartNodeBypassDamperModelDef() as SeventyFiveFProfileDirective
            DomainName.hyperstatSplitCPU -> ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
            DomainName.smartnodePID -> ModelLoader.getSmartNodePidModel() as SeventyFiveFProfileDirective
            DomainName.helionodePID -> ModelLoader.getHelioNodePidModel() as SeventyFiveFProfileDirective
            else -> null
        }
    }

    // Not initializing to a default val because the sensor will write to it at the next update interval

    private fun buildPoint(pointConfig : PointBuilderConfig) : Point {
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

        if (pointConfig.modelDef.valueConstraint?.constraintType == Constraint.ConstraintType.NUMERIC) {
            val constraint = pointConfig.modelDef.valueConstraint as NumericConstraint
            pointBuilder.setMaxVal(constraint.maxValue.toString())
            pointBuilder.setMinVal(constraint.minValue.toString())

            val incrementValTag = pointConfig.modelDef.presentationData?.entries?.find { it.key == "tagValueIncrement" }
            incrementValTag?.let { pointBuilder.setIncrementVal(it.value.toString()) }
        } else if (pointConfig.modelDef.valueConstraint?.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            val constraint = pointConfig.modelDef.valueConstraint as MultiStateConstraint
            val enumString = constraint.allowedValues.joinToString { it.value }
            pointBuilder.setEnums(enumString)
        }

        if (pointConfig.modelDef is SeventyFiveFProfilePointDef) {
            if (pointConfig.modelDef.hisInterpolate.name.isNotEmpty()) {
                pointBuilder.setHisInterpolate(pointConfig.modelDef.hisInterpolate.name.lowercase())
            }
        }

        //TODO - Support added for currently used tag types. Might need updates in future.
        pointConfig.modelDef.tags.filter { it.kind == TagType.MARKER && it.name.lowercase() != "tz"}.forEach{ pointBuilder.addMarker(it.name)}
        pointConfig.modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach{ tag ->
            TagsUtil.getTagDefHVal(tag)?.let { pointBuilder.addTag(tag.name, it) }
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
        pointBuilder.addTag("sourcePoint", HStr.make(pointConfig.modelDef.id))
        pointConfig.tz.let { pointBuilder.addTag(Tags.TZ, HStr.make(pointConfig.tz))}

        return pointBuilder.build()
    }

}