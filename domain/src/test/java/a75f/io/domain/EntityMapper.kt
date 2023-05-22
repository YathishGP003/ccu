package a75f.io.domain

import a75f.io.domain.modeldef.ModelDefDto
import a75f.io.domain.modeldef.PointDef

class EntityMapper {

    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDefDto: ModelDefDto) {
        buildBasePoints(modelDefDto).forEach { println(it) }
        val associatedPoints = buildAssociatedPoints(modelDefDto, configuration)
        associatedPoints.forEach { println(it)
            if (configuration.getAssociations().contains(it.name)) {
               println("Create associated point : ${it.name}")
            }
        }
    }


    private fun buildBasePoints(modelDefDto: ModelDefDto) : List<Map<Any, Any>> {
        return modelDefDto.points.filter { point -> point.tags.find { it.name.contains("base") } != null }
            .map { it.toPointDef() }
            .map { toPoint(it) }
    }

    private fun toPoint(pointDef : PointDef) : Map <Any, Any> {
        val point = mutableMapOf<Any, Any>()
        point["dis"] = pointDef.name
        //pointDef.rootTagNames.
        return point
    }

    private fun buildAssociatedPoints(modelDefDto: ModelDefDto, configuration: ProfileConfiguration) : List<PointDef> {
        return modelDefDto.points.filter { point -> point.tags.find { it.name.contains("associated") } != null }
                                                .map { it.toPointDef() }
    }
}