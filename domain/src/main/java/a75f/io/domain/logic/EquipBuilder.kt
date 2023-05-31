package a75f.io.domain.logic

import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.dto.ModelDefDto

class EquipBuilder {
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDef) {
        val entityMapper = EntityMapper()
        println("Base points")
        entityMapper.getBasePoints(modelDef).forEach { println(it) }

        println("Associated points")
        entityMapper.getAssociatedPoints(modelDef).forEach { println(it) }

        println("Association points")
        entityMapper.getAssociationPoints(modelDef).forEach { println(it) }

        println("Dependent points")
        entityMapper.getDependentPoints(modelDef).forEach { println(it) }

        println("Dynamic Sensor points")
        entityMapper.getDynamicSensorPoints(modelDef).forEach { println(it) }

        val associatedPoints = entityMapper.getAssociatedPoints(modelDef, configuration)
        associatedPoints.forEach {
            if (configuration.getAssociations().contains(it.domainName)) {
               /*println("Create associated point : ${it.name}")
               val point = getPointByDomainName(modelDefDto, it.name)
               println("Associated point : $point")
               //Read the field , association : "domainName" , find it is enabled.
               //Create the point if association is enabled.*/
            }
        }
    }
}