package a75f.io.domain

import a75f.io.domain.modeldef.ModelDefDto
import a75f.io.domain.modeldef.PointDef

class EntityMapper {

    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDefDto: ModelDefDto) {
        getBaseAndAssociationPoints(modelDefDto).forEach { println(it) }
        val associatedPoints = getAssociatedPoints(modelDefDto, configuration)
        associatedPoints.forEach { println(it)
            if (configuration.getAssociations().contains(it.name)) {
               println("Create associated point : ${it.name}")
               val point = getPointByDomainName(modelDefDto, it.name)
               println("Associated point : $point")
               //Read the field , association : "domainName" , find it is enabled.
               //Create the point if association is enabled.
            }
        }
    }


    private fun getBaseAndAssociationPoints(modelDefDto: ModelDefDto) : List<Map<Any, Any>> {
        return modelDefDto.points.filter { point -> point.tags.find { it.name.contains("base") || it.name.contains("association") } != null }
            .map { it.toPointDef() }
            .map { toPoint(it) }
    }

    private fun toPoint(pointDef : PointDef) : Map <Any, Any> {
        val point = mutableMapOf<Any, Any>()
        point["dis"] = pointDef.name
        //pointDef.rootTagNames.
        return point
    }

    private fun getAssociatedPoints(modelDefDto: ModelDefDto, configuration: ProfileConfiguration) : List<PointDef> {
        return modelDefDto.points.filter { point -> point.tags.find { it.name.contains("associated") } != null }
                                                .map { it.toPointDef() }
    }

    private fun getPointByDomainName(modelDefDto: ModelDefDto, name : String) : PointDef? {
        return modelDefDto.points.map {it.toPointDef()}.find { it.name == name }
    }
}