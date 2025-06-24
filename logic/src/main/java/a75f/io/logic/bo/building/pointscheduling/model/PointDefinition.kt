import a75f.io.api.haystack.DAYS
import a75f.io.api.haystack.MockTime
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.TimeUtil
import a75f.io.logic.bo.building.pointscheduling.model.Day
import org.joda.time.DateTime
import org.joda.time.Interval
import org.projecthaystack.HDict
import org.projecthaystack.HList


class PointDefinition {
    lateinit var id: String
    var scheduleGroup: Int = 0
    var defaultValue: Double = 0.0
    var dis: String = ""
    var unit: String = ""
    var query: String = ""
    var builderType: String = ""
    var tagValuesMap: MutableList<String> = mutableListOf()
    var tags: MutableList<String> = mutableListOf()
    var days: MutableList<Day> = mutableListOf()

    fun dictToPointDefinition(pointDefinition: HDict): PointDefinition = apply {
        val it = pointDefinition.iterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (pair is Map.Entry<*, *>) {
                when (pair.key) {
                    Tags.ID -> id = pair.value.toString()
                    Tags.SCHEDULE_GROUP -> this.scheduleGroup = pair.value.toString().toDouble().toInt()
                    Tags.DEFAULT_VALUE -> defaultValue = pair.value.toString().toDouble()
                    Tags.DIS -> dis = pair.value.toString()
                    Tags.UNIT -> unit = pair.value.toString()
                    Tags.BUILDER_TYPE -> builderType = pair.value.toString()
                    Tags.QUERY -> query = pair.value.toString()
                    Tags.DAYS -> {
                        val dayList = pair.value as? HList
                        if (dayList != null) {
                            for (i in 0 until dayList.size()) {
                                val dict = dayList.get(i) as HDict
                                val day = Day().dictToDay(dict)
                                days.add(day)
                            }
                        }
                    }
                    Tags.TAGS -> {
                        val tagList = pair.value as? HList
                        if (tagList != null) {
                            for (i in 0 until tagList.size()) {
                                tags.add(tagList.get(i).toString())
                            }
                        }
                    }
                }
            }
        }
    }
}
