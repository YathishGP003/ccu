package a75f.io.logic.bo.building.system

/**
 * Created by Manjunath K on 27-10-2023.
 */

fun mapToSetPoint(min: Double, max: Double, current: Double): Double {
    return ((max - min) * (current / 100.0) + min).toInt().toDouble()
}