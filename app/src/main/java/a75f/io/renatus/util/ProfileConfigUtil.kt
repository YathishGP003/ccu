package a75f.io.renatus.util

import a75f.io.domain.api.Point
import a75f.io.domain.util.allHSFanModes
import a75f.io.domain.util.allMSFanModes
import a75f.io.domain.util.updateConditioningEnums
import a75f.io.domain.util.updateFanEnums
import a75f.io.logic.bo.building.hyperstat.common.PossibleConditioningMode
import a75f.io.logic.bo.building.hyperstat.common.PossibleFanMode
import a75f.io.logic.bo.building.mystat.profiles.util.MyStatPossibleFanMode

/**
 * Created by Manjunath K on 29-04-2025.
 */

fun modifyFanMode(possibleFanMode: Int, fanOpMode: Point) {
    val (low, med, high) = when (possibleFanMode) {
        PossibleFanMode.OFF.ordinal -> Triple(false, false, false)
        PossibleFanMode.LOW.ordinal -> Triple(true, false, false)
        PossibleFanMode.MED.ordinal -> Triple(false, true, false)
        PossibleFanMode.HIGH.ordinal -> Triple(false, false, true)
        PossibleFanMode.LOW_MED_HIGH.ordinal -> Triple(true, true, true)
        PossibleFanMode.LOW_MED.ordinal -> Triple(true, true, false)
        PossibleFanMode.LOW_HIGH.ordinal -> Triple(true, false, true)
        PossibleFanMode.MED_HIGH.ordinal -> Triple(false, true, true)
        PossibleFanMode.AUTO.ordinal -> Triple(false, true, true)
        else -> {
            Triple(true, true, true)
        }
    }

    if (possibleFanMode == PossibleFanMode.AUTO.ordinal) {
        // In case of AUTO, we need to set the fan mode to AUTO
        updateFanEnums(
            fanOpMode,
            allHSFanModes,
            lowAvailable = false,
            mediumAvailable = false,
            highAvailable = false,
            onlyAuto = true
        )
        return
    }

    updateFanEnums(
        fanOpMode, allHSFanModes, lowAvailable = low, mediumAvailable = med, highAvailable = high
    )
}

fun modifyFanMode(possibleFanMode: MyStatPossibleFanMode, fanOpMode: Point) {
    val (low, high, onlyAuto) = when (possibleFanMode) {
        MyStatPossibleFanMode.OFF -> Triple(false, false, false)
        MyStatPossibleFanMode.LOW -> Triple(true, false, false)
        MyStatPossibleFanMode.HIGH -> Triple(false, true, false)
        MyStatPossibleFanMode.LOW_HIGH -> Triple(true, true, false)
        MyStatPossibleFanMode.AUTO -> Triple(false, false, true)
    }

    updateFanEnums(
        fanOpMode, allMSFanModes, lowAvailable = low, highAvailable = high, onlyAuto = onlyAuto
    )
}


fun modifyConditioningMode(
    possibleConditioningMode: Int, conditioningMode: Point, options: String
) {
    val (coolOnly, heatOnly) = when (possibleConditioningMode) {
        PossibleConditioningMode.COOLONLY.ordinal -> Pair(true, false)
        PossibleConditioningMode.HEATONLY.ordinal -> Pair(false, true)
        PossibleConditioningMode.OFF.ordinal -> Pair(false, false)
        PossibleConditioningMode.BOTH.ordinal -> Pair(true, true)
        else -> {
            Pair(true, true)
        }
    }

    updateConditioningEnums(
        conditioningMode, options, coolOnlyAvailable = coolOnly, heatOnlyAvailable = heatOnly
    )
}