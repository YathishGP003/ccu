package a75f.io.domain.util

import a75f.io.api.haystack.BuildConfig
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.ModelTagDef
import io.seventyfivef.ph.core.Tags
import org.projecthaystack.HBool
import org.projecthaystack.HDict
import org.projecthaystack.HNum
import org.projecthaystack.HStr
import org.projecthaystack.HVal

fun extractAndAppendExternalEdits(oldPointDef : ModelPointDef?, newPoint : Point, oldPoint : HashMap<Any, Any>) {
    if (oldPointDef == null) {
        CcuLog.i(Domain.LOG_TAG, "Invalid model point")
        return
    }
    val dbPoint = Point.Builder().setHashMap(oldPoint).build()
    getExternallyAddedTags(oldPointDef.tags, oldPoint).forEach {
        if(it.value.toString() == "marker") {
            newPoint.markers.add(it.key)
        } else {
            newPoint.tags[it.key] = it.value
        }
    }
    if (oldPointDef.name != extractDisplayName(dbPoint)) {
        CcuLog.i(Domain.LOG_TAG, "dis override detected ${oldPointDef.name}   db point dis Name:  ${dbPoint.displayName}  -> extracted DomainName: ${extractDisplayName(dbPoint)}")
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
        if(it.value.toString() == "marker") {
            newPoint.markers.add(it.key)
        } else {
            newPoint.tags[it.key] = it.value
        }
    }
    if (oldPointDef.name != dbPoint.displayName) {
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
        Tags.SITE_REF, Tags.KIND, Tags.TZ,Tags.DIS,Tags.MIN_VAL,Tags.MAX_VAL,Tags.INCREMENT_VAL,
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
        Tags.EQUIP_REF, Tags.DEVICE_REF, Tags.ROOM_REF, Tags.FLOOR_REF,Tags.DIS,Tags.MIN_VAL,Tags.MAX_VAL,Tags.INCREMENT_VAL,
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


fun extractDisplayName(dbPoint: Point): String {
    dbPoint.equipRef?.let {
        val siteName = hayStack.siteName
        val equip = hayStack.readMapById(dbPoint.equipRef.toString())
        if (equip.contains(a75f.io.api.haystack.Tags.SYSTEM)) {
            return dbPoint.displayName.substringAfterLast(siteName + "-${equip[a75f.io.api.haystack.Tags.DOMAIN_NAME]}-")
        } else if (equip.contains(Tags.ZONE)) {
            return dbPoint.displayName.substringAfterLast("${getFormattedZonePointDisName(equip, siteName)}-")
        }
    }
    return dbPoint.displayName.substringAfterLast("-")
}
// hard code the  formatted dis name for zone points
//once the formatted was normalized we can remove this function
fun getFormattedZonePointDisName(equip: HashMap<Any, Any>, siteName: String?): String {

    equip[a75f.io.api.haystack.Tags.DOMAIN_NAME]?.let {
        when (it.toString()) {
            DomainName.hyperstatSplitCPU -> {
                return siteName + "-cpuecon-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }

            DomainName.hyperstatSplit2PEcon -> {
                return siteName + "-pipe2econ-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }

            DomainName.hyperstatSplit4PEcon -> {
                return siteName + "-pipe4econ-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }

            DomainName.smartnodeVAVReheatNoFan, DomainName.smartnodeVAVReheatSeriesFan,
            DomainName.smartnodeVAVReheatParallelFan, DomainName.helionodeVAVReheatNoFan,
            DomainName.helionodeVAVReheatSeriesFan, DomainName.helionodeVAVReheatParallelFan -> {
                return siteName + "-VAV-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }

            DomainName.otnTemperatureInfluence -> {
                return siteName + "-OTN-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }

            DomainName.smartnodeSSE, DomainName.helionodeSSE -> {
                return siteName + "-SSE-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }

            DomainName.smartnodeDAB, DomainName.helionodeDAB -> {
                if (BuildConfig.BUILD_TYPE.equals("carrier_prod", ignoreCase = true)) {
                    return siteName + "-VVT-C-" + equip[a75f.io.api.haystack.Tags.GROUP]
                } else {
                    return siteName + "-DAB-" + equip[a75f.io.api.haystack.Tags.GROUP]
                }
            }

            DomainName.smartnodeActiveChilledBeam, DomainName.helionodeActiveChilledBeam -> {
                return siteName + "-ACB-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }

            DomainName.smartnodePID, DomainName.helionodePID -> {
                return siteName + "-PID-" + equip[a75f.io.api.haystack.Tags.GROUP]
            }
            else -> {
            }
        }
    }
    return siteName + "-${equip[a75f.io.api.haystack.Tags.DOMAIN_NAME]}-" + equip[a75f.io.api.haystack.Tags.GROUP]

}

