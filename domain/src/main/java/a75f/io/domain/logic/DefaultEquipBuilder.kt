package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.BuildConfig
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.TagsUtil
import a75f.io.logger.CcuLog
import android.annotation.SuppressLint
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFEquipPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerPointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import io.seventyfivef.domainmodeler.common.point.PointState
import io.seventyfivef.ph.core.TagType
import kotlinx.coroutines.asCoroutineDispatcher
import org.projecthaystack.HBool
import org.projecthaystack.HStr
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/**
 * A common implementation of EquipBuilder interface with a generic equip/point build support.
 */
open class DefaultEquipBuilder : EquipBuilder {

    protected val highPriorityDispatcher = Executors.newFixedThreadPool(4, object : ThreadFactory {
        private val counter = AtomicInteger(0)

        override fun newThread(r: Runnable): Thread {
            return Thread(r, "HighPriorityThread-${counter.incrementAndGet()}").apply {
                priority = Thread.MAX_PRIORITY
            }
        }
    }).asCoroutineDispatcher()

    override fun buildEquip(equipConfig : EquipBuilderConfig) : Equip {
        val siteDisName = hayStack.site?.displayName
        val equipBuilder =
            Equip.Builder().setDisplayName("$siteDisName-${equipConfig.modelDef.name}")
                .setDomainName(equipConfig.modelDef.domainName)
                .setFloorRef(equipConfig.profileConfiguration?.floorRef)
                .setSiteRef(equipConfig.siteRef)
                .setProfile(equipConfig.profileConfiguration?.profileType)
                .setDomainName(equipConfig.modelDef.domainName)

        if (!(equipConfig.modelDef.name.contentEquals("buildingEquip"))) {
            equipBuilder.setDisplayName(equipConfig.disPrefix)
        }
        if (equipConfig.modelDef.name.contentEquals("diagEquip") ||
                    equipConfig.modelDef.name.contentEquals("ccuConfiguration")) {
            equipBuilder.setDisplayName("${equipConfig.disPrefix}")
        }

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

        equipConfig.modelDef.tags.filter { it.kind == TagType.NUMBER && it.name.lowercase() == "bacnetid" }.forEach{ tag ->
            tag.defaultValue?.let {
                equipBuilder.setBacnetId(HSUtil.generateBacnetId(equipConfig.profileConfiguration?.nodeAddress.toString()))
            }
        }

        equipConfig.modelDef.tags.filter { it.kind == TagType.STR && it.name.lowercase() == "bacnettype" }.forEach{ tag ->
            tag.defaultValue?.let {
                val bacnetType = tag.defaultValue.toString()
                equipBuilder.setBacnetType(bacnetType)
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
        if (pointConfig.modelDef is SeventyFiveFEquipPointDef) {
            if (pointConfig.modelDef.hisInterpolate.name.isNotEmpty()) {
                pointBuilder.setHisInterpolate(pointConfig.modelDef.hisInterpolate.name.lowercase())
            }
        }

        pointConfig.modelDef.tags.forEach { tag ->
            when(tag.kind) {
                TagType.MARKER -> if (tag.name.lowercase() != "tz") pointBuilder.addMarker(tag.name)
                TagType.STR -> {
                    if (tag.name.lowercase() == "bacnettype") {
                        pointBuilder.setBacnetType(tag.defaultValue.toString())
                    } else {
                        tag.defaultValue?.let { pointBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString())) }
                    }
                }
                TagType.NUMBER -> {
                    if (tag.name.lowercase() == "bacnetid") {
                        tag.defaultValue?.let {
                            if (pointConfig.configuration?.roomRef != null) {
                                if (pointConfig.configuration.roomRef.lowercase() == "system") {
                                    val nodeAdd = pointConfig.configuration.nodeAddress
                                    val bacnetId = "$nodeAdd${tag.defaultValue.toString().toInt()}"
                                    pointBuilder.setBacnetId(abs(bacnetId.toInt()))
                                } else {
                                    val smartNodeAddressBand = getSmartNodeBand()?.toInt()
                                    if (smartNodeAddressBand != null && pointConfig.configuration.nodeAddress != null) {
                                        val nodeAdd =
                                            pointConfig.configuration.nodeAddress - smartNodeAddressBand + 1000
                                        val bacnetId =
                                            "$nodeAdd${tag.defaultValue.toString().toInt()}"
                                        pointBuilder.setBacnetId(abs(bacnetId.toInt()))
                                    }
                                }
                            }
                        }
                    } else {
                        TagsUtil.getTagDefHVal(tag)?.let { pointBuilder.addTag(tag.name, it) }
                    }
                }
                TagType.BOOL -> tag.defaultValue?.let { pointBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean)) }
                else -> {}
            }
        }

        pointConfig.tz.let { pointBuilder.addTag(Tags.TZ, HStr.make(pointConfig.tz)) }
        pointBuilder.addTag("sourcePoint", HStr.make(pointConfig.modelDef.id))

        var enums = ""
        if (pointConfig.modelDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            (pointConfig.modelDef.valueConstraint as MultiStateConstraint).allowedValues.forEachIndexed { _, value ->
                enums = if (enums.isNotEmpty()) {
                    "$enums${getEnum(value)}=${value.index},"
                } else {
                    "${getEnum(value)}=${value.index},"
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

    private fun getEnum(pointState: PointState): String {
        pointState.dis?.let {
            if (pointState.dis!!.isNotEmpty()) {
                return pointState.dis.toString()
            }
        }
        return pointState.value
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
        val device = CCUHsApi.getInstance().readEntity("device and node and addr")
        CcuLog.i(Domain.LOG_TAG, "Deviceband $device")
        if (device != null && device.size > 0 && device["modbus"] == null && device["addr"] != null) {
            val nodeAdd = device["addr"].toString()
            return nodeAdd.substring(0, nodeAdd.length - 2) + "00"
        } else {
            val band = Domain.readPoint(DomainName.addressBand)
            CcuLog.i(Domain.LOG_TAG, "Deviceband $device")
            if (band != null && band.isNotEmpty() && band["val"] != null) {
                return band["val"].toString()
            }
        }
        return null
    }

    /*Once testing is completed we can remove isDiagEquip parameter to make this function clean*/
    fun createPoints(modelDef: ModelDirective, equipRef: String, siteRef: String, isDiagEquip: Boolean) {
        val tz = hayStack.timeZone
        val equipDis = hayStack.readMapById(equipRef)["dis"].toString()
        modelDef.points.forEach {
            createPoint(PointBuilderConfig(it, null, equipRef, siteRef, tz, equipDis))
            CcuLog.i(Domain.LOG_TAG," Created "+if(isDiagEquip){"CCUDiagEquip"} else {"CCU_Configuration"}+" point ${it.domainName}")
        }
    }
    private fun createPoint(pointConfig: PointBuilderConfig) {
        val hayStackPoint = buildPoint(pointConfig)
        val pointId = hayStack.addPoint(hayStackPoint)
        hayStackPoint.id = pointId
        DomainManager.addPoint(hayStackPoint)

        pointConfig.modelDef.run {
            if ("writable" in tagNames && defaultValue is Number) {
                hayStack.writeDefaultValById(pointId, (defaultValue as Number).toDouble())
                CcuLog.i(Domain.LOG_TAG, "write Default Val for point ${hayStackPoint.domainName} value $defaultValue")
            }
        }

        pointConfig.modelDef.run {
            if ("his" in tagNames) {
                val priorityVal = hayStack.readPointPriorityVal(pointId)
                hayStack.writeHisValById(pointId, priorityVal)
                CcuLog.i(Domain.LOG_TAG, "write his Val for point ${hayStackPoint.domainName} value $priorityVal")
            }
        }
    }
}
