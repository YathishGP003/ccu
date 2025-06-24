import a75f.io.api.haystack.Tags
import org.projecthaystack.HDict
import org.projecthaystack.HVal

data class EventDefinition(
    var id: String? = null,
    var dis: String? = null,
    var builderType: String? = null,
    var query: String? = null,
    var defaultValue: Double = 0.0,
    var unit: String? = null,
    private var tags: MutableMap<String, HVal> = mutableMapOf()
) {
    fun dictToEventDefinition(pointDefinition: HDict): EventDefinition = apply {
        val it = pointDefinition.iterator()
        while (it.hasNext()) {
            val pair = it.next()
            if (pair is Map.Entry<*, *>) {
                when (pair.key) {
                    Tags.ID -> id = pair.value.toString()
                    Tags.DIS -> dis = pair.value.toString()
                    Tags.BUILDER_TYPE -> builderType = pair.value.toString()
                    Tags.QUERY -> query = pair.value.toString()
                    Tags.UNIT -> unit = pair.value.toString()
                    Tags.TAGS ->  {
                        tags[pair.key.toString()] = (pair.value as HVal)
                    }

                    Tags.DEFAULT_VALUE -> defaultValue = pair.value.toString().toDouble()
                }
            }
        }
    }

    override fun toString(): String {
        return "EventDefinition(id=$id, dis=$dis, builderType=$builderType, query=$query, defaultValue=$defaultValue, unit=$unit, tags=$tags)"
    }
}
