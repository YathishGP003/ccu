package a75f.io.domain.model.dto

import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.ph.core.Tags

class ModelEquipDto(
    val equipId: String,
    val domainName: String?,
    val equip: ModelDefDto,
    val name: String?,
    private val tagNames: Set<String>,
    val points: List<ModelPointDto>,
    val tags: List<TagDefDto>?,
    val tagValues: Map<String, Any>?
) {
    fun toModelDef(): ModelDef {
        return ModelDef(
            id = equipId,
            name = name ?: equip.name,
            domainName = domainName ?: equip.name,
            tagNames = (tagNames + equip.tagNames).filterNot { it in DEFAULT_TAG_NAMES }.toSet(),
            tags = ((tags ?: emptyList()) + equip.tags)
                .filterNot { it.name in DEFAULT_TAG_NAMES }
                .map { it.toTagDef(tagValues?.get(it.name)) }.toSet(),
            points = points.map(ModelPointDto::toPointDef),
            equips = equip.equips.map(ModelEquipDto::toModelDef),
            namespace = equip.namespace
        )
    }

    companion object {
        val DEFAULT_TAG_NAMES = setOf(Tags.ID, Tags.DIS, Tags.SITE_REF, Tags.EQUIP)
    }
}
