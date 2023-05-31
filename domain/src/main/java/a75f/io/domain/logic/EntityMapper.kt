package a75f.io.domain.logic

import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.ModelPointDef
import a75f.io.domain.model.common.point.PointConfiguration
import a75f.io.domain.model.dto.ModelDefDto
import a75f.io.domain.model.dto.ModelPointDto
import a75f.io.domain.model.dto.PointDefDto


class EntityMapper {

    fun getBasePoints(modelDef: ModelDef) : List<Map<Any, Any>> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.BASE
        }.map { toPoint(it) }
    }

    fun getAssociationPoints(modelDef: ModelDef) : List<Map<Any, Any>> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.ASSOCIATION
        }.map { toPoint(it) }
    }

    fun getDependentPoints(modelDef: ModelDef) : List<Map<Any, Any>> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.DEPENDENT
        }.map { toPoint(it) }
    }

    fun getAssociatedPoints(modelDef: ModelDef) : List<Map<Any, Any>> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.ASSOCIATED
        }.map { toPoint(it) }
    }

    fun getDynamicSensorPoints(modelDef: ModelDef) : List<Map<Any, Any>> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.DYNAMIC_SENSOR
        }.map { toPoint(it) }
    }
    private fun toPoint(pointDef : ModelPointDef) : Map <Any, Any> {
        val point = mutableMapOf<Any, Any>()
        point["dis"] = pointDef.domainName
        //pointDef.rootTagNames.

        return point
    }

    fun getAssociatedPoints(modelDef: ModelDef, configuration: ProfileConfiguration) : List<ModelPointDef> {
        return modelDef.points.filter { point -> point.tags.find { it.name.contains("associated") } != null }

    }

    fun getPointByDomainName(modelDef: ModelDef, name : String) : ModelPointDef? {
        return modelDef.points.find { it.domainName == name }
    }
}