package a75f.io.domain.util

import a75f.io.domain.api.Domain
import a75f.io.domain.api.Point
import a75f.io.logger.CcuLog
import android.util.Log

/**
 * Created by Manjunath K on 17-04-2025.
 */


const val allSystemProfileConditions = "off=0,auto=1,coolonly=2,heatonly=3"
const val allStandaloneProfileConditions = "off=0,auto=1,heatonly=2,coolonly=3"
const val allHSFanModes =
    "Off=0,Auto=1,Fan Low Current Occupied Period=2,Fan Low Occupied Period=3,Fan Low All Times=4,Fan Medium Current Occupied Period=5,Fan Medium Occupied Period=6,Fan Medium All Times=7,Fan High Current Occupied Period=8,Fan High Occupied Period=9,Fan High All Times=10"
const val allMSFanModes = "Off=0,Auto=1,Fan Low Current Occupied Period=2,Fan Low Occupied Period=3,Fan Low All Times=4,Fan High Current Occupied Period=5,Fan High Occupied Period=6,Fan High All Times=7"


fun updateConditioningEnums(
    point: Point,
    conditioningOptions: String,
    coolOnlyAvailable: Boolean = true,
    heatOnlyAvailable: Boolean = true
) {
    val disabledEnums = mutableListOf<String>()
    if (!coolOnlyAvailable) {
        disabledEnums.add("coolonly")
    }
    if (!heatOnlyAvailable) {
        disabledEnums.add("heatonly")
    }
    if (!coolOnlyAvailable || !heatOnlyAvailable) {
        disabledEnums.add("auto")
    }
    val updatedEnum = disableSpecifiedEnum(point, conditioningOptions, disabledEnums)
    Log.i("CCU_DOMAIN", "Updated enum for point ${point.domainName} is $updatedEnum")
    if (updatedEnum != null) {
        updateEnumToDb(point, updatedEnum)
    }
}

fun updateFanEnums(
    point: Point,
    allFanOptions: String,
    lowAvailable: Boolean = true,
    mediumAvailable: Boolean = true,
    highAvailable: Boolean = true,
    onlyAuto: Boolean = false
) {
    val disabledEnums = mutableListOf<String>()

    if (onlyAuto) {
        disabledEnums.add("Fan Low Current Occupied Period")
        disabledEnums.add("Fan Low Occupied Period")
        disabledEnums.add("Fan Low All Times")

        disabledEnums.add("Fan Medium Current Occupied Period")
        disabledEnums.add("Fan Medium Occupied Period")
        disabledEnums.add("Fan Medium All Times")

        disabledEnums.add("Fan High Current Occupied Period")
        disabledEnums.add("Fan High Occupied Period")
        disabledEnums.add("Fan High All Times")
    } else {
        if (!lowAvailable) {
            disabledEnums.add("Fan Low Current Occupied Period")
            disabledEnums.add("Fan Low Occupied Period")
            disabledEnums.add("Fan Low All Times")
        }
        if (!mediumAvailable) {
            disabledEnums.add("Fan Medium Current Occupied Period")
            disabledEnums.add("Fan Medium Occupied Period")
            disabledEnums.add("Fan Medium All Times")
        }
        if (!highAvailable) {
            disabledEnums.add("Fan High Current Occupied Period")
            disabledEnums.add("Fan High Occupied Period")
            disabledEnums.add("Fan High All Times")
        }
        if (!lowAvailable && !mediumAvailable && !highAvailable) {
            disabledEnums.add("auto")
        }
    }
    val updatedEnum = disableSpecifiedEnum(point, allFanOptions, disabledEnums)
    Log.i("CCU_DOMAIN", "Updated enum for point ${point.domainName} is $updatedEnum")
    if (updatedEnum != null) {
        updateEnumToDb(point, updatedEnum)
    }
}

fun updateEnumToDb(point: Point, enum: String) {
    val dbPoint = a75f.io.api.haystack.Point.Builder()
        .setHDict(Domain.hayStack.readHDictById(point.getPoint()["id"].toString())).setEnums(enum)
        .build()
    Domain.hayStack.updatePoint(dbPoint, dbPoint.id)
}

private fun disableSpecifiedEnum(
    point: Point, allEnums: String, disabledEnums: List<String>
): String? {
    CcuLog.e("CCU_DOMAIN", "Disabled enums: $disabledEnums")
    return try {
        val originalEnum = point.getPoint()["enum"]?.toString()
        CcuLog.e("CCU_DOMAIN", "reading point ${point.getPoint()}  ${point.equipRef}")
        CcuLog.e("CCU_DOMAIN", "reading originalEnum $originalEnum")
        if (originalEnum != null) {
            disabledEnums.fold(allEnums) { acc, enum ->
                acc.replace(enum, "NA", ignoreCase = true)
            }
        } else null
    } catch (e: Exception) {
        CcuLog.e("CCU_DOMAIN", "Exception updating enum for point ${point.id}: ${e.message}")
        null
    }
}

