package a75f.io.domain.model.dto

import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.common.point.ModelAssociation

class ModelDefDto(
    val id: String,
    val name: String,
    val tagNames: Set<String>,
    val tags: List<TagDefDto>,
    val points: List<ModelPointDto>,
    val equips: List<ModelEquipDto>,
    val namespace: String,
    private val manufacturerName: String? = null,
    val associatedWithDevice: ModelAssociation? = null,
    val associatedWithTuner: ModelAssociation? = null,
) {
    fun toModelDef(): ModelDef {
        return ModelDef(
            id = id,
            name = name,
            domainName = name,
            tagNames = tagNames.toSet(),
            tags = tags.map(TagDefDto::toTagDef).toSet(),
            points = points.map(ModelPointDto::toPointDef),
            equips = equips.map(ModelEquipDto::toModelDef),
            namespace = namespace,
            manufacturerName = manufacturerName,
            associatedWithDevice = associatedWithDevice,
            associatedWithTuner = associatedWithTuner,
        )
    }
}
