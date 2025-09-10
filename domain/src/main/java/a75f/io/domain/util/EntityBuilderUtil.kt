package a75f.io.domain.util

import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.ModelTagDef
import io.seventyfivef.ph.core.Tags
import org.projecthaystack.HBool
import org.projecthaystack.HDict
import org.projecthaystack.HNum
import org.projecthaystack.HStr
import org.projecthaystack.HVal
import kotlin.collections.forEach

fun extractAndAppendExternalEdits(oldPointDef : ModelPointDef?, newPoint : Point, oldPoint : HashMap<Any, Any>) {
    if (oldPointDef == null) {
        CcuLog.i(Domain.LOG_TAG, "Invalid model point")
        return
    }
    val dbPoint = Point.Builder().setHashMap(oldPoint).build()
    getExternallyAddedTags(oldPointDef.tags, oldPoint).forEach {
        newPoint.tags[it.key] = it.value
    }
    if (oldPointDef.name != dbPoint.displayName.substringAfterLast("-")) {
        CcuLog.i(Domain.LOG_TAG, "dis override detected ${oldPointDef.name} - ${dbPoint.displayName}")
        newPoint.displayName = dbPoint.displayName;
    }
    if (!oldPointDef.defaultUnit.isNullOrEmpty()
        && !dbPoint.unit.isNullOrEmpty()
        && oldPointDef.defaultUnit != dbPoint.unit) {
        CcuLog.i(Domain.LOG_TAG,"unit override detected ${oldPointDef.defaultUnit} - ${dbPoint.unit}")
        newPoint.unit = dbPoint.unit
    }
}

fun extractAndAppendExternalEdits(oldPointDef : ModelPointDef?, newPoint : RawPoint, oldPointDict : HDict) {
    if (oldPointDef == null) {
        CcuLog.i(Domain.LOG_TAG, "Invalid model point")
        return
    }
    val dbPoint = RawPoint.Builder().setHDict(oldPointDict).build()
    getExternallyAddedTags(oldPointDef.tags, oldPointDict).forEach {
        newPoint.tags[it.key] = it.value
    }
    if (oldPointDef.name != dbPoint.displayName.substringAfterLast("-")) {
        CcuLog.i(Domain.LOG_TAG, "dis override detected ${oldPointDef.name} - ${dbPoint.displayName}")
        newPoint.displayName = dbPoint.displayName;
    }
    if (!oldPointDef.defaultUnit.isNullOrEmpty()
        && !dbPoint.unit.isNullOrEmpty()
        && oldPointDef.defaultUnit != dbPoint.unit) {
        CcuLog.i(Domain.LOG_TAG,"unit override detected ${oldPointDef.defaultUnit} - ${dbPoint.unit}")
        newPoint.unit = dbPoint.unit
    }
}

fun extractAndAppendExternalEdits(oldPointDef : ModelPointDef?, newPoint : RawPoint, oldPointMap : HashMap<Any, Any>) {
    return extractAndAppendExternalEdits(oldPointDef, newPoint, Domain.hayStack.readHDictById(oldPointMap.get(Tags.ID).toString()))
}

private fun getExternallyAddedTags(pointTagsSet : Set<ModelTagDef>, pointMap : HashMap<Any, Any>)
        : Map<String, HVal> {
    val ccuAddedTags = mutableSetOf(Tags.ID,
        Tags.EQUIP_REF, Tags.DEVICE_REF, Tags.ROOM_REF, Tags.FLOOR_REF,
        Tags.SITE_REF, Tags.KIND, Tags.TZ,
        "domainName" , "sourcePoint", "bacnettype", "bacnetid", "ccuRef",
        "enum","hisInterpolate", "group",
        "createdDateTime","lastModifiedBy","lastModifiedDateTime"
    )
    val modelPointTags = mutableMapOf<String, Any?>()
    pointTagsSet.forEach {
        if (it.name !in ccuAddedTags) {
            modelPointTags[it.name] = it.defaultValue ?: "marker"
        }
    }
    CcuLog.i(Domain.LOG_TAG," modelPointTags : $modelPointTags")

    val externallyAddedTags = mutableMapOf<String, HVal>()
    for ((key, value) in pointMap) {
        if (key !in modelPointTags && key !in ccuAddedTags ) {
            externallyAddedTags[key.toString()] = anyToHVal(value)
        }
    }
    CcuLog.i(Domain.LOG_TAG, " Externally added tag for ${pointMap["dis"]} "+externallyAddedTags)
    return externallyAddedTags
}

private fun getExternallyAddedTags(pointTagsSet : Set<ModelTagDef>, pointMap : HDict)
        : Map<String, HVal> {
    val ccuAddedTags = mutableSetOf(Tags.ID,
        Tags.EQUIP_REF, Tags.DEVICE_REF, Tags.ROOM_REF, Tags.FLOOR_REF,
        Tags.SITE_REF, Tags.KIND, Tags.TZ,
        "domainName" , "sourcePoint", "bacnettype", "bacnetid", "ccuRef",
        "enum","hisInterpolate", "group",
        "createdDateTime","lastModifiedBy","lastModifiedDateTime",
        "analogType", "portEnabled", "port", "pointRef"
    )
    val modelPointTags = mutableMapOf<String, Any?>()
    pointTagsSet.forEach {
        if (it.name !in ccuAddedTags) {
            modelPointTags[it.name] = it.defaultValue ?: "marker"
        }
    }
    CcuLog.i(Domain.LOG_TAG," modelPointTags : $modelPointTags")

    val externallyAddedTags = mutableMapOf<String, HVal>()
    val pointDictIterator = pointMap.iterator()
    while(pointDictIterator.hasNext()) {
        val entry = pointDictIterator.next() as HDict.MapEntry
        if (entry.key !in modelPointTags && entry.key !in ccuAddedTags ) {
            externallyAddedTags[entry.key.toString()] = anyToHVal(entry.value)
        }
    }
    CcuLog.i(Domain.LOG_TAG, " Externally added tag for ${pointMap["dis"]} "+externallyAddedTags)
    return externallyAddedTags
}

private fun anyToHVal(value: Any?): HVal {
    return when (value) {
        is HVal -> value
        is Number -> HNum.make(value.toDouble())
        is Boolean -> HBool.make(value)
        else -> HStr.make(value.toString())
    }
}

