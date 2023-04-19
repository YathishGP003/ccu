package a75f.io.domain

import a75f.io.domain.modeldef.ModelDefDto
import a75f.io.domain.modeldef.PointDef
import androidx.lifecycle.Observer

class DMEquipBuilder {

    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDefDto: ModelDefDto) {
        val basePoints = buildBasePoints(modelDefDto)
        basePoints.forEach { println(it) }
    }

    private fun buildBasePoints(modelDefDto: ModelDefDto) : List<Map<Any, Any>> {
        return modelDefDto.points.map { it.toPointDef() }
                            .map { toPoint(it) }
    }

    private fun toPoint(pointDef : PointDef) : Map <Any, Any> {
        val point = mutableMapOf<Any, Any>()
        point["dis"] = pointDef.name
        //pointDef.rootTagNames.
        return point
    }
}