package a75f.io.logic.bo.building.pointscheduling.model

import EventDefinition
import a75f.io.api.haystack.Entity
import a75f.io.api.haystack.Tags
import org.projecthaystack.HDict
import org.projecthaystack.HList
import org.projecthaystack.HVal

data class EventSchedule(
    var id: String? = null,
    var dis: String? = null,
    var eventDefinitions: MutableList<EventDefinition> = mutableListOf(),
    var organization: String? = null,
    var organizationId: String? = null,
    var range: Range = Range(),
    var reason : String? = null,
    var tags: MutableMap<String, HVal> = mutableMapOf(),
) : Entity() {

    fun dictToEventSchedule(event: HDict): EventSchedule {
        val it = event.iterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (pair is Map.Entry<*, *>) {
                when (pair.key) {
                    Tags.ID -> this.id = pair.value.toString()
                    Tags.DIS -> this.dis = pair.value.toString()
                    Tags.EVENT_DEFINITIONS -> {
                        val pd = pair.value as HList
                        for (i in 0 until pd.size()) {
                            val dict = pd.get(i) as HDict
                            val eventDefinition = EventDefinition().dictToEventDefinition(dict)
                            eventDefinitions.add(eventDefinition)
                        }
                    }
                    Tags.ORGANIZATION -> this.organization = pair.value.toString()
                    Tags.ORGANIZATION_ID -> this.organizationId = pair.value.toString()
                    Tags.RANGE -> {
                        val rangeDict = pair.value as HDict
                        this.range = Range().dictToRange(rangeDict)
                    }
                    Tags.REASON -> this.reason = pair.value.toString()
                    else -> {
                        tags[pair.key.toString()] = (pair.value as HVal)
                    }
                }
            }
        }
        return this
    }

}