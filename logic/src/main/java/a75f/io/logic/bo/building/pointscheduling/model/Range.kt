package a75f.io.logic.bo.building.pointscheduling.model

import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.util.formatDate
import a75f.io.logic.bo.util.formatTimeValue
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import org.projecthaystack.HDict

data class Range(
    var stdt: String? = null,  // start date
    var etdt: String? = null,  // end date
    var sthh: String? = null,  // start hour
    var ethh: String? = null,  // end hour
    var stmm: String? = null,  // start minute
    var etmm: String? = null,  // end minute

) {
    fun dictToRange(range: HDict): Range {
        val it = range.iterator()
        while (it.hasNext()) {
            val pair = it.next()

            if (pair is Map.Entry<*, *>) {
                if ((pair.key == Tags.STDT)) {
                    this.stdt = pair.value.toString()
                } else if (pair.key == Tags.ETDT) {
                    this.etdt = pair.value.toString()
                } else if (pair.key == Tags.STHH) {
                    this.sthh = pair.value.toString()
                } else if (pair.key == Tags.ETHH) {
                    this.ethh = pair.value.toString()
                } else if (pair.key == Tags.STMM) {
                    this.stmm = pair.value.toString()
                } else if (pair.key == Tags.ETMM) {
                    this.etmm = pair.value.toString()
                }
            }

        }
        return this
    }

    override fun toString(): String {
        return "${formatDate(stdt!!)} | "+formatTimeValue(sthh)+":"+formatTimeValue(stmm)+" to ${formatDate(etdt!!)} | "+formatTimeValue(ethh)+":"+formatTimeValue(etmm)
    }
}