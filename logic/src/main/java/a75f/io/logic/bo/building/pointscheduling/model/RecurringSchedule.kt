package a75f.io.logic.bo.building.pointscheduling.model

import PointDefinition
import a75f.io.api.haystack.Entity
import a75f.io.api.haystack.Tags
import org.projecthaystack.HDict
import org.projecthaystack.HList
import org.projecthaystack.HVal


class RecurringSchedule() : Entity() {

    lateinit var id: String
    lateinit var siteRef: String
    private lateinit var organizationId: String
    lateinit var organization: String
    var dis: String? = null
    var pointDefinitions: MutableList<PointDefinition> = mutableListOf()
    private var tags: MutableMap<String, HVal> = mutableMapOf()

    fun dictToPointSchedule(schedule: HDict): RecurringSchedule {
        val it = schedule.iterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (pair is Map.Entry<*, *>) {
                when (pair.key) {
                    Tags.ID -> this.id = pair.value.toString()
                    Tags.DIS -> this.dis = pair.value.toString()
                    Tags.SITEREF -> this.siteRef = pair.value.toString()
                    Tags.ORGANIZATION_ID -> this.organizationId = pair.value.toString()
                    Tags.ORGANIZATION -> this.organization = pair.value.toString()
                    Tags.POINT_DEFINITIONS -> {
                        val pointDefinitions = pair.value as HList
                        for (i in 0 until pointDefinitions.size()) {
                            val dict = pointDefinitions.get(i) as HDict
                            val pointDefinition = PointDefinition().dictToPointDefinition(dict)
                            this.pointDefinitions.add(pointDefinition)
                        }
                    }
                    else -> {
                        tags[pair.key.toString()] = (pair.value as HVal)
                    }
                }
            }
        }
        return this
    }

}
