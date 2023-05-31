package a75f.io.domain.model.dto

import a75f.io.domain.model.ModelPointDef
import a75f.io.domain.model.common.point.Constraint
import a75f.io.domain.model.common.point.MultiStateConstraint
import a75f.io.domain.model.common.point.NumericConstraint
import a75f.io.domain.model.common.point.PointConfiguration
import a75f.io.domain.model.ph.core.PointType
import a75f.io.domain.model.ph.core.Tags


class ModelPointDto(
    private val pointId: String,
    private val domainName: String?,
    private val point: PointDefDto,
    private val isGpc36: Boolean,
    private val tagNames: Set<String>,
    private val tagValues: Map<String, Any>?,
    val tags: List<TagDefDto> = emptyList(),
    // Model-Level Point Overrides
    val name: String?,
    private val description: String?,
    private val pointType: PointType?,
    private val valueConstraint: Constraint?,
    private val presentationData: Map<String, Any>?,
    private val defaultUnit: String?,
    private val defaultValue: Any?,
    private val configuration: PointConfiguration?,
) {
    fun toPointDef(): ModelPointDef {
        val constraint = valueConstraint ?: point.getValueConstraint()

        val defaultTagNames = when (constraint) {
            is NumericConstraint -> setOf(Tags.ID, Tags.DIS, Tags.EQUIP_REF, Tags.SITE_REF, Tags.SPACE_REF, Tags.MIN_VAL, Tags.MAX_VAL, Tags.INCREMENT_VAL)
            is MultiStateConstraint -> setOf(Tags.ID, Tags.DIS, Tags.SITE_REF, Tags.EQUIP_REF, Tags.ALLOWED_VALUES)
            else -> setOf(Tags.ID, Tags.DIS, Tags.SITE_REF)
        }

        return ModelPointDef(
            id = pointId,
            displayName = name ?: point.name,
            domainName = domainName ?: point.name,
            tagNames = (tagNames + point.tagNames).filterNot { it in defaultTagNames }.toSortedSet(),
            tagValues = tagValues,
            rootTagNames = point.tagNames - defaultTagNames,
            tags = (tags + point.tags).filterNot { it.name in defaultTagNames }.map { it.toTagDef(tagValues?.get(it.name)) }.toSet(),
            pointType = pointType ?: point.pointType,
            valueConstraint = constraint,
            presentationData = presentationData ?: point.presentationData,
            defaultUnit = defaultUnit ?: point.defaultUnit,
            defaultValue = defaultValue,
            configuration = configuration
        )
    }
}
