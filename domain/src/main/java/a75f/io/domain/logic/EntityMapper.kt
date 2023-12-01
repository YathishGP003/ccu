package a75f.io.domain.logic

import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.AssociationConfiguration
import io.seventyfivef.domainmodeler.common.point.ComparisonType
import io.seventyfivef.domainmodeler.common.point.DependentConfiguration
import io.seventyfivef.domainmodeler.common.point.DynamicSensorConfiguration
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.PointConfiguration


/**
 * EntityMapper helps extracting entity details from domain model.
 * A particular profile configuration can be used to resolve dependency and association relations.
 * Every instance of EntityMapper is tied a specific domain model.
 */
class EntityMapper (private val modelDef: SeventyFiveFProfileDirective) {

    fun getEntityConfiguration(configuration: ProfileConfiguration) : EntityConfiguration{
        val entityConfiguration = EntityConfiguration()
        entityConfiguration.tobeAdded.addAll(getBasePoints().map { EntityConfig(it.domainName)})
        entityConfiguration.tobeAdded.addAll(getEnabledAssociations(configuration).map { EntityConfig(it)})
        entityConfiguration.tobeAdded.addAll(getEnabledDependencies(configuration).map { EntityConfig(it)})
        return entityConfiguration
    }

    fun getBasePoints() : List<SeventyFiveFProfilePointDef> {
        return modelDef.points.filter {
                it.configuration.configType == PointConfiguration.ConfigType.BASE
        }
    }

    fun getAssociationPoints() : List<SeventyFiveFProfilePointDef> {
        return modelDef.points.filter {
                it.configuration.configType == PointConfiguration.ConfigType.ASSOCIATION
        }
    }

    fun getDependentPoints() : List<SeventyFiveFProfilePointDef> {
        return modelDef.points.filter {
                it.configuration.configType == PointConfiguration.ConfigType.DEPENDENT
        }
    }

    fun getAssociatedPoints() : List<SeventyFiveFProfilePointDef> {
        return modelDef.points.filter {
            it.configuration.configType == PointConfiguration.ConfigType.ASSOCIATED
        }
    }

    fun getDynamicSensorPoints() : List<SeventyFiveFProfilePointDef> {
        return modelDef.points.filter {
                it.configuration.configType == PointConfiguration.ConfigType.DYNAMIC_SENSOR
        }
    }
    private fun toPoint(pointDef : ModelPointDef) : Map <Any, Any> {
        val point = mutableMapOf<Any, Any>()
        point["dis"] = pointDef.domainName
        //pointDef.rootTagNames.

        return point
    }

    fun getPointByDomainName(name : String) : SeventyFiveFProfilePointDef? {
        return modelDef.points.find { it.domainName == name }
    }

    private fun getAssociationDefinitions() : List<SeventyFiveFProfilePointDef> {
        return modelDef.points.filter {
                it.configuration.configType == PointConfiguration.ConfigType.ASSOCIATION
        }
    }

    fun getEnabledAssociations(profileConfiguration: ProfileConfiguration) : List<String> {

        val enabledAssociations = mutableListOf<String>()
        //Get all association that are present in the profileConfig.
        getAssociationDefinitions().forEach { def ->
            val associationEnabled = isAssociationEnabled(def, profileConfiguration)
            val profileConfig =
                profileConfiguration.getAssociationConfigs().find { it.domainName == def.domainName }

            profileConfig?.let {
                if (associationEnabled) {
                    val constraint = def.valueConstraint as MultiStateConstraint
                    enabledAssociations.add(constraint.allowedValues[it.associationVal].value)
                }
            }
        }
        return enabledAssociations
    }

    private fun isAssociationEnabled(pointDef : SeventyFiveFProfilePointDef,
                                     profileConfiguration: ProfileConfiguration) : Boolean {
        //TODO- add to DM validation
        val pointConfiguration = pointDef.configuration as AssociationConfiguration
        val associationPointName = pointConfiguration.domainName

        val baseConfig = profileConfiguration.getEnableConfigs()
            .find { point -> point.domainName == associationPointName }

        if (baseConfig != null && evaluateConfiguration(
                pointConfiguration.comparisonType,
                pointConfiguration.value as Int - 1, //TODO -index starts at 1 ?
                baseConfig.enabled.toInt()
            )
        ) {
            return true
        }
        return false
    }

    fun getEnabledDependencies(profileConfiguration: ProfileConfiguration) : List<String> {

        val enabledDependencies = mutableListOf<String>()
        //Get all association that are present in the profileConfig.
        getDependentPoints().forEach { def ->
            val pointConfiguration = def.configuration as DependentConfiguration
            val associationPointName = pointConfiguration.domainName

            val baseConfig = profileConfiguration.getEnableConfigs().find { point -> point.domainName == associationPointName }

            if (baseConfig != null && evaluateConfiguration(ComparisonType.EQUALS,
                    pointConfiguration.value as Int,
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
        if (evaluateConfiguration(sensorConfig.comparisonType,
                sensorConfig.value as Int,
                currentVal.toInt())) {
            return sensorPoint
        }
        return null
    }

    private fun isDynamicSensorTypeMatching(sensorType: Int, pointDef: SeventyFiveFProfilePointDef) : Boolean{
        val sensorConfig = pointDef.configuration as DynamicSensorConfiguration
        return sensorType == sensorConfig.sensorType.toInt()
    }


    private fun getPhysicalRefMapping(profileConfiguration: ProfileConfiguration) : Map<String, String>{
        val refMapping = mutableMapOf<String, String>()
        val pointsWithPhysicalRefs = modelDef.points.filter {it.devicePointAssociation != null }
        val associationPoints = modelDef.points.filter {
                                        it.configuration.configType == PointConfiguration.ConfigType.ASSOCIATION
                                    }

        pointsWithPhysicalRefs.forEach{ profilePoint ->
            if (associationPoints.contains(profilePoint)) {
                //Linked profile point is an association point. Attach the link if it is enabled.
                //&& isAssociationEnabled()

                val associationPoint = associationPoints.find {
                    val associationConfig = it.configuration as AssociationConfiguration
                    profilePoint.domainName == associationConfig.domainName
                }

                associationPoint?.let {
                    if (isAssociationEnabled(associationPoint, profileConfiguration)) {
                        val profileConfig =
                            profileConfiguration.getAssociationConfigs().find { it.domainName == associationPoint.domainName }

                        val constraint = associationPoint.valueConstraint as MultiStateConstraint
                        val profilePointDomainName = constraint.allowedValues[profileConfig?.associationVal!!].value
                        refMapping[profilePoint.domainName] = profilePointDomainName
                    }
                }
            } else {
                profilePoint.devicePointAssociation?.let {
                    refMapping[profilePoint.domainName] = it.devicePointDomainName
                }

            }
        }
        return refMapping
    }

    fun getPhysicalProfilePointRef(profileConfiguration: ProfileConfiguration, rawPointName : String) : String?{
        val profilePointWithPhysicalMapping = modelDef.points.find {it.devicePointAssociation?.devicePointDomainName == rawPointName }
            ?: return null

        val associationPoints = modelDef.points.filter {
            it.configuration.configType == PointConfiguration.ConfigType.ASSOCIATION
        }

        //Linked profile point is an association point. Attach the link if it is enabled.
        val associationPoint = associationPoints.find {
            val associationConfig = it.configuration as AssociationConfiguration
            profilePointWithPhysicalMapping.domainName == associationConfig.domainName
        }

        if (associationPoint != null) {
            if (isAssociationEnabled(associationPoint, profileConfiguration)) {
                val profileConfig =
                    profileConfiguration.getAssociationConfigs()
                        .find { it.domainName == associationPoint.domainName }

                val constraint = associationPoint.valueConstraint as MultiStateConstraint
                return constraint.allowedValues[profileConfig?.associationVal!!].value
            }
        } else {
            profilePointWithPhysicalMapping.devicePointAssociation?.let {
                if (it.devicePointDomainName == rawPointName) {
                    return profilePointWithPhysicalMapping.domainName
                }
            }
        }

        return null
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


}