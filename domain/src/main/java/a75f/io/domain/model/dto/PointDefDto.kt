package a75f.io.domain.model.dto

import a75f.io.domain.model.common.point.*
import a75f.io.domain.model.ph.core.PointType

class PointDefDto(
    val id: String,
    val name: String,
    val description: String,
    val pointType: PointType,
    val tagNames: Set<String>,
    val tags: List<TagDefDto>,
    val defaultUnit: String?,
    val defaultValue: Any?,
    val configuration: PointConfiguration?,
    val is75F: Boolean,
    val valueConstraintType: Constraint.ConstraintType,
    val allowedValues: List<PointState>?,
    val minValue: Double?,
    val maxValue: Double?,
    val presentationData: Map<String, Any>?,
) {
    fun getValueConstraint(): Constraint {
        return if (minValue != null && maxValue !== null) {
            NumericConstraint(minValue, maxValue)
        } else if (!allowedValues.isNullOrEmpty()) {
            MultiStateConstraint(allowedValues)
        } else NoConstraint()
    }
}
