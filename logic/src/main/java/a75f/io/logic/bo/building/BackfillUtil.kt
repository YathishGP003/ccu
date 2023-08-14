package a75f.io.logic.bo.building

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.tuners.TunerConstants
import java.util.*

private val backfillPref = BackfillPref()

fun addBackFillDurationPointIfNotExists(ccuHsApi: CCUHsApi) {

    val siteMap = ccuHsApi.readEntity(Tags.SITE)
    val equipMap = ccuHsApi.readEntity("equip and system")
    val equip = Equip.Builder().setHashMap(equipMap).build()
    val equipRef = equip.id
    val siteRef = Objects.requireNonNull(siteMap[Tags.ID]).toString()
    val tz = Objects.requireNonNull(siteMap["tz"]).toString()
    val equipDis = Objects.requireNonNull(siteMap["dis"]).toString() + "-SystemEquip"
    if (verifyBackFillPointAvailability(equipRef)) {
        val backFillDurationPoint = Point.Builder().setDisplayName(
            "$equipDis-backFillDuration"
        )
            .setSiteRef(siteRef).setEquipRef(equipRef).addMarker("sp").addMarker("system")
            .addMarker("backfill").addMarker("writable").addMarker("config")
            .addMarker("duration")
            .addMarker("ventilation")
            .setEnums(BackfillPref.BackFillDuration.toIntArray().contentToString())
            .setTz(tz).setUnit("hrs")
            .build()
        val defaultBackFillDurationSelected = backfillPref.getBackFillTimeDuration()
        val backFillDurationPointId = ccuHsApi.addPoint(backFillDurationPoint)
        ccuHsApi.writePointForCcuUser(
            backFillDurationPointId,
            TunerConstants.UI_DEFAULT_VAL_LEVEL,
            defaultBackFillDurationSelected.toDouble(),
            0
        )
        backfillPref.saveBackfillConfig(
            defaultBackFillDurationSelected,
            Arrays.binarySearch(
                BackfillPref.BackFillDuration.toIntArray(),
                defaultBackFillDurationSelected
            )
        )
    }
}

fun verifyBackFillPointAvailability(equipRef: String?): Boolean {
    val backFillDuration = CCUHsApi.getInstance().readAllEntities(
        "point and system and backfill and duration and equipRef == \"$equipRef\""
    )
    return backFillDuration.isEmpty()
}

fun updateBackfillDuration(currentBackFillTime: Double) {
    val ccuHsApi = CCUHsApi.getInstance()
    val backFIllQuery = "backfill and duration"
    if (isBackfillPointExisting(ccuHsApi)) {
        ccuHsApi.writeDefaultVal(backFIllQuery, currentBackFillTime)
        backfillPref.saveBackfillConfig(
            currentBackFillTime.toInt(),
            Arrays.binarySearch(
                BackfillPref.BackFillDuration.toIntArray(),
                currentBackFillTime.toInt()
            )
        )
    } else {
        addBackFillDurationPointIfNotExists(ccuHsApi)
    }
}
private fun isBackfillPointExisting(ccuHsApi: CCUHsApi): Boolean {
    return ccuHsApi.readEntity("backfill and duration").isNotEmpty()
}
