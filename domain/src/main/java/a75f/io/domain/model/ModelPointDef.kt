package a75f.io.domain.model

import a75f.io.domain.model.common.point.Constraint
import a75f.io.domain.model.common.point.NumericConstraint
import a75f.io.domain.model.common.point.PointConfiguration
import a75f.io.domain.model.ph.core.PointType

class ModelPointDef(
    val id: String,
    val domainName: String,
    val displayName: String?,
    val tagNames: Set<String>,
    val tagValues: Map<String, Any>?,
    val rootTagNames: Set<String>,
    val tags: Set<TagDef>,
    val pointType: PointType,
    val valueConstraint: Constraint?,
    val presentationData: Map<String, Any>?,
    val defaultUnit: String?,
    val defaultValue: Any?,
    val configuration: PointConfiguration?
) {
    init {
        if (valueConstraint is NumericConstraint) {
            require(defaultUnit != null) {
                "Point with a numeric value constraint must have a default unit"
            }
        }
    }

    /**
     * Any 2 ModelPointDefs are considered equal if they have the same pointDefId & tags
     */
    override fun equals(other: Any?): Boolean {
        return other is ModelPointDef && id == other.id && tags == other.tags
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + tags.hashCode()
        return result
    }

    override fun toString(): String {
        return "$displayName $tags"
    }
}
