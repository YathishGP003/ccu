package a75f.io.logic.bo.building.pointscheduling.model

import a75f.io.api.haystack.Tags
import org.projecthaystack.HDict

data class Day(
    var sthh: Int = 0,
    var stmm: Int = 0,
    var ethh: Int = 0,
    var etmm: Int = 0,
    var day: Int = 0,
    var `val`: Double = 0.0
) {
    var intersection: Boolean = false

    fun dictToDay(day: HDict): Day {
        val it = day.iterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (pair is Map.Entry<*, *>) {
                if ((pair.key == Tags.START_HOUR)) {
                    this.sthh = pair.value.toString().toDouble().toInt()
                } else if ((pair.key == Tags.START_MINUTE)) {
                    this.stmm = pair.value.toString().toDouble().toInt()
                } else if ((pair.key == Tags.END_HOUR)) {
                    this.ethh = pair.value.toString().toDouble().toInt()
                } else if ((pair.key == Tags.END_MINUTE)) {
                    this.etmm = pair.value.toString().toDouble().toInt()
                } else if ((pair.key == Tags.DAY)) {
                    this.day = pair.value.toString().toDouble().toInt()
                } else if ((pair.key == Tags.VALUE)) {
                    this.`val` = pair.value.toString().toDouble()
                }
            }
        }
        return this
    }
}