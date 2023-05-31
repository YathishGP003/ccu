package a75f.io.domain.model

import a75f.io.domain.model.common.point.ModelAssociation

class ModelDef(
    val id: String,
    val name: String,
    val domainName: String,
    val tagNames: Set<String>,
    val tags: Set<TagDef>,
    val points: List<ModelPointDef>,
    val equips: List<ModelDef>,
    val namespace: String? = null,
    val manufacturerName: String? = null,
    val associatedWithDevice: ModelAssociation? = null,
    val associatedWithTuner: ModelAssociation? = null,
) {
    override fun toString(): String {
        return "$name $tagNames"
    }

    fun allPoints(): Set<ModelPointDef> {
        return (points + equips.map { it.allPoints() }.flatten()).toSet()
    }

    fun allPointIds(): Set<String> {
        return (points.map { it.id } + equips.map { it.allPointIds() }.flatten()).toSet()
    }
}

class ModelVersion(val id: String, val version: String? = null)
