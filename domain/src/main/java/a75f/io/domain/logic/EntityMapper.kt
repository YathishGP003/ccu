package a75f.io.domain.logic

import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.model.ModelDef
import a75f.io.domain.model.ModelPointDef
import a75f.io.domain.model.common.point.*

/**
 * EntityMapper helps extracting entity details from domain model.
 * A particular profile configuration can be used to resolve dependency and association relations.
 * Every instance of EntityMapper is tied a specific domain model.
 */
class EntityMapper (private val modelDef: ModelDef) {

    fun getEntityConfiguration(configuration: ProfileConfiguration) {
        val entityConfiguration = EntityConfiguration()
        entityConfiguration.tobeAdded.addAll(getBasePoints().map { EntityConfig(it.domainName)})
        entityConfiguration.tobeAdded.addAll(getEnabledAssociations(configuration).map { EntityConfig(it)})
        entityConfiguration.tobeAdded.addAll(getEnabledDependencies(configuration).map { EntityConfig(it)})
    }

    fun getBasePoints() : List<ModelPointDef> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.BASE
        }
    }

    fun getAssociationPoints() : List<ModelPointDef> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.ASSOCIATION
        }
    }

    fun getDependentPoints() : List<ModelPointDef> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.DEPENDENT
        }
    }

    fun getAssociatedPoints() : List<ModelPointDef> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.ASSOCIATED
        }
    }

    fun getDynamicSensorPoints() : List<ModelPointDef> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.DYNAMIC_SENSOR
        }
    }
    private fun toPoint(pointDef : ModelPointDef) : Map <Any, Any> {
        val point = mutableMapOf<Any, Any>()
        point["dis"] = pointDef.domainName
        //pointDef.rootTagNames.

        return point
    }

    fun getAssociatedPoints(configuration: ModelDef) : List<ModelPointDef> {
        return modelDef.points.filter { point -> point.tags.find { it.name.contains("associated") } != null }

    }

    fun getPointByDomainName(name : String) : ModelPointDef? {
        return modelDef.points.find { it.domainName == name }
    }

    private fun getAssociationDefinitions() : List<ModelPointDef> {
        return modelDef.points.filter {
                point -> point.configuration?.configurationType == PointConfiguration.ConfigType.ASSOCIATION
        }
    }

    fun getEnabledAssociations(profileConfiguration: ProfileConfiguration) : List<String> {

        val enabledAssociations = mutableListOf<String>()
        //Get all association that are present in the profileConfig.
        getAssociationDefinitions().forEach { def ->
            val profileConfig =
                profileConfiguration.getAssociationConfigs().find { it.domainName == def.domainName }

            profileConfig?.let {
                //There is an association, we only create the corresponding point if associated point is enabled.
                val associationIndex = it.associationVal

                //TODO- add to DM validation
                val pointConfiguration = def.configuration as AssociationConfiguration
                val associationPointName = pointConfiguration.domainName

                val baseConfig = profileConfiguration.getEnableConfigs().find { point -> point.domainName == associationPointName }

                //TODO - add to DM validation.
                val constraint = def.valueConstraint as MultiStateConstraint
                if (baseConfig != null && evaluateConfiguration(ComparisonType.valueOf(def.configuration.comparisonType),
                        def.configuration.value.index - 1, //TODO -index starts at 1 ?
                        baseConfig.enabled.toInt())) {
                    enabledAssociations.add(constraint.allowedValues[associationIndex].value)
                }

            }
        }
        return enabledAssociations
    }

    fun getEnabledDependencies(profileConfiguration: ProfileConfiguration) : List<String> {

        val enabledDependencies = mutableListOf<String>()
        //Get all association that are present in the profileConfig.
        getDependentPoints().forEach { def ->
            val pointConfiguration = def.configuration as DependentConfiguration
            val associationPointName = pointConfiguration.domainName

            val baseConfig = profileConfiguration.getEnableConfigs().find { point -> point.domainName == associationPointName }

            if (baseConfig != null && evaluateConfiguration(ComparisonType.EQUALS,
                    def.configuration.value.index - 1, //TODO -index starts at 1 ?
                    baseConfig.enabled.toInt())) {
                enabledDependencies.add(def.domainName)
            }

        }
        return enabledDependencies
    }

    /**
     * Returns a point definition if dynamic sensor point exists for sensor type and
     * current value of the sensor is matching constraint set in point configuration.
     */
    fun getEnabledDynamicSensorPoint(sensorType : Int, currentVal : Double) : ModelPointDef?{
        val sensorPoint = getDynamicSensorPoints().find {
            isDynamicSensorTypeMatching(sensorType, it)
        }
        val sensorConfig = sensorPoint?.configuration as DynamicSensorConfiguration
        if (evaluateConfiguration(ComparisonType.valueOf(sensorConfig.comparisonType),
                sensorConfig.value.toInt(),
                currentVal.toInt())) {
            return sensorPoint
        }
        return null
    }

    private fun isDynamicSensorTypeMatching(sensorType: Int, pointDef: ModelPointDef) : Boolean{
        val sensorConfig = pointDef.configuration as DynamicSensorConfiguration
        return sensorType == sensorConfig.sensorType.toInt()
    }

    private fun evaluateConfiguration(comparisonType: ComparisonType, leftVal : Int, rightVal: Int) :  Boolean{
        return when(comparisonType) {
            ComparisonType.EQUALS -> leftVal == rightVal
            ComparisonType.NOT_EQUALS -> leftVal != rightVal
            ComparisonType.GREATER_THAN -> leftVal > rightVal
            ComparisonType.LESS_THAN -> leftVal < rightVal
            ComparisonType.GREATER_THAN_OR_EQUAL_TO -> leftVal >= rightVal
            ComparisonType.LESS_THAN_OR_EQUAL_TO -> leftVal <= rightVal
        }
    }

    private fun Boolean.toInt() = if (this) 1 else 0
}