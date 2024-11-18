package a75f.io.domain.logic

import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.config.getConfig
import a75f.io.logger.CcuLog
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelPointDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.AssociationConfiguration
import io.seventyfivef.domainmodeler.common.point.ComparisonType
import io.seventyfivef.domainmodeler.common.point.Constraint
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
        configuration.getBaseProfileConfigs().forEach {
            entityConfiguration.tobeAdded.add(it)
            CcuLog.i(Domain.LOG_TAG, "Profile base config ${it.domainName} added")
        }
        CcuLog.i(Domain.LOG_TAG, "All base points "+entityConfiguration.tobeAdded.map { it.domainName }.joinToString { "," })

        // There can be same associations for multiple points So checking the duplicates
        getEnabledAssociations(configuration).forEach { newDomain ->
            if (entityConfiguration.tobeAdded.find { it.domainName == newDomain } == null) {
                entityConfiguration.tobeAdded.add(EntityConfig(newDomain))
            }
        }
        getEnabledDependencies(configuration).forEach { newDomain ->
            if (entityConfiguration.tobeAdded.find { it.domainName == newDomain } == null) {
                entityConfiguration.tobeAdded.add(EntityConfig(newDomain))
            }
        }
        getMultiDependentPoints(configuration, entityConfiguration.tobeAdded).forEach { newDomain ->
            if (entityConfiguration.tobeAdded.find { it.domainName == newDomain } == null) {
                entityConfiguration.tobeAdded.add(EntityConfig(newDomain))
            }
        }
        return entityConfiguration
    }

    // Returns all the points that are base points in the model definition.
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
        point[Tags.DIS] = pointDef.domainName
        //pointDef.rootTagNames.

        return point
    }

    private fun getPointByDomainName(name : String) : SeventyFiveFProfilePointDef? {
        return modelDef.points.find { it.domainName == name }
    }

    private fun getAssociationDefinitions() : List<SeventyFiveFProfilePointDef> {
        return modelDef.points.filter {
                it.configuration.configType == PointConfiguration.ConfigType.ASSOCIATION
        }
    }

    /**
     * Returns a list of association and associated points list.
     * Reads list of association points and get the config of the association point.
     * create association point when config is enabled (example Relay1Association, Relay2Association)
     * along with that associated point also will be added here (example CoolingStage1, CoolingStage2)
     */
    fun getEnabledAssociations(profileConfiguration: ProfileConfiguration) : List<String> {

        val enabledAssociations = mutableListOf<String>()
        //Get all association that are present in the profileConfig.
        getAssociationDefinitions().forEach { def ->
            val associationEnabled = isAssociationEnabled(def, profileConfiguration)
            val profileConfig =
                profileConfiguration.getAssociationConfigs().find { it.domainName == def.domainName }

            profileConfig?.let { associationConfig ->
                if (associationEnabled) {
                    val constraint = def.valueConstraint as MultiStateConstraint
                    // Add Association point
                    enabledAssociations.add(def.domainName)
                    // Add mapped associated point here
                    constraint.allowedValues.find { it.index == associationConfig.associationVal }?.value?.let {
                        association -> enabledAssociations.add(association)
                    }
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
                pointConfiguration.value as Int, //TODO -index starts at 1 ?
                baseConfig.enabled.toInt()
            )
        ) {
            return true
        }
        return false
    }

    /**
     * Returns a list of dependent points that are enabled based on the profile configuration.
     * Reads list of dependent points and get the config of the dependent point.
     * Finds the associated and its configuration
     */
    fun getEnabledDependencies(profileConfiguration: ProfileConfiguration) : List<String> {

        val enabledDependencies = mutableListOf<String>()
        //Get all association that are present in the profileConfig.
        getDependentPoints().forEach { def ->
            val pointConfiguration = def.configuration as DependentConfiguration
            val associationPointName = pointConfiguration.domainName

            val baseEnableConfig = profileConfiguration.getEnableConfigs().find { point -> point.domainName == associationPointName }
            if (baseEnableConfig != null && evaluateConfiguration(ComparisonType.EQUALS,
                    pointConfiguration.value as Int,
                    baseEnableConfig.enabled.toInt())) {
                enabledDependencies.add(def.domainName)
            }

            val baseValueConfig = profileConfiguration.getValueConfigs().find { point -> point.domainName == associationPointName }
            if (baseValueConfig != null && evaluateConfiguration(ComparisonType.EQUALS,
                    pointConfiguration.value as Int,
                    baseValueConfig.currentVal.toInt()
            )) {
                enabledDependencies.add(def.domainName)
            }
        }
        return enabledDependencies
    }
    private fun getMultiDependentPoints(configuration: ProfileConfiguration, pointsToAdd: MutableList<EntityConfig>): List<String> {
        val dynamicConfigs = mutableListOf<String>()

        getDependentPoints().forEach { def ->
            val dynamicPoint = def.configuration as? DependentConfiguration ?: return@forEach
            val dependentPoint = getPointByDomainName(dynamicPoint.domainName) ?: return@forEach

            val configType = dependentPoint.configuration.configType
            if (configType != PointConfiguration.ConfigType.ASSOCIATION &&
                configType != PointConfiguration.ConfigType.DEPENDENT &&
                configType != PointConfiguration.ConfigType.ASSOCIATED) {
                return@forEach
            }

            if (configType == PointConfiguration.ConfigType.ASSOCIATION &&
                !isAssociatedConfigEnabled(dependentPoint, configuration)) {
                return@forEach
            }

            val associationConfig = configuration.getAssociationConfigs().getConfig(dynamicPoint.domainName)
            val shouldAdd =
                when (configType) {
                    PointConfiguration.ConfigType.ASSOCIATION -> {
                        associationConfig?.associationVal?.let {
                            evaluateConfiguration(dynamicPoint.comparisonType, dynamicPoint.value as Int, it)
                        } ?: false
                    }
                    PointConfiguration.ConfigType.DEPENDENT -> {
                    // TODO Revisit if it has any use case and check it for base config type
                        val valueConfig = configuration.getValueConfigs().getConfig(dynamicPoint.domainName)
                        valueConfig?.currentVal?.let {
                            evaluateConfiguration(dynamicPoint.comparisonType, dynamicPoint.value as Int, it.toInt())
                        } ?: false
                    }
                    PointConfiguration.ConfigType.ASSOCIATED -> {
                        val isDependencyExist = pointsToAdd.find { it.domainName == dynamicPoint.domainName }
                        isDependencyExist != null
                    }
                    else -> true
                }

            if (shouldAdd) {
                dynamicConfigs.add(def.domainName)
                logIt("added ${def.domainName}  > depends on  ${dependentPoint.domainName} & its Config $configType")
            }

            getEnumPointIfExist(def, configuration)?.let { enumPoint ->
                dynamicConfigs.add(enumPoint)
                logIt("Added Dynamic enum point $enumPoint")
            }
        }
        dynamicConfigs.addAll(getCustomPoints(configuration).map { it })
        return dynamicConfigs
    }

    /**
     * Function to add custom points to the entity configuration.
     * This is in special case when point has dependency on multiple points then it will be added as
     * base point. So framework can not create association for this point if it has multi association
     * so it has to be added as custom point.
     */
    private fun getCustomPoints(configuration: ProfileConfiguration): List<String> {
        val customPoints = mutableListOf<String>()
        configuration.getCustomPoints().forEach { (pointDef, config) ->
            if (pointDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
                when (config) {
                    is ValueConfig -> {
                        val constraint = pointDef.valueConstraint as MultiStateConstraint
                        customPoints.add(constraint.allowedValues[config.currentVal.toInt()].value)
                    }
                    is EnableConfig -> {
                        val constraint = pointDef.valueConstraint as MultiStateConstraint
                        customPoints.add(constraint.allowedValues[config.enabled.toInt()].value)
                    }
                    is AssociationConfig -> {
                        val constraint = pointDef.valueConstraint as MultiStateConstraint
                        constraint.allowedValues.find { it.index == config.associationVal }?.value?.let {
                            association -> customPoints.add(association)
                        }
                    }
                }
            }
        }
        return customPoints
    }


    private fun getEnumPointIfExist(
        pointDef: SeventyFiveFProfilePointDef,
        configuration: ProfileConfiguration
    ): String? {
        if (pointDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
            val constraint = pointDef.valueConstraint as MultiStateConstraint
            val value = configuration.getConfigByDomainName(pointDef.domainName)
            logIt("${pointDef.domainName} : ${value?.javaClass?.simpleName}")

            if (value != null) {
                when (value) {
                    is ValueConfig -> {
                        return (constraint.allowedValues[value.currentVal.toInt()].value)
                    }
                    is EnableConfig -> {
                        return (constraint.allowedValues[value.enabled.toInt()].value)
                    }
                    is AssociationConfig -> {
                        return constraint.allowedValues.find { it.index == value?.associationVal!! }?.value
                    }
                }
            }
        }
       return null
    }


    /**
     * Returns true if the associated config is enabled for dynamic points.
     */
    private fun isAssociatedConfigEnabled(point: SeventyFiveFProfilePointDef, config: ProfileConfiguration): Boolean {
        val dependency = point.configuration as AssociationConfiguration
        val configPoint = config.getEnableConfigs().find { it.domainName == dependency.domainName }
        if (configPoint != null) {
            return evaluateConfiguration(
                dependency.comparisonType,
                dependency.value as Int,
                configPoint.enabled.toInt()
            )
        }
        return false
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

    fun getPhysicalProfilePointRef(profileConfiguration: ProfileConfiguration, rawPointName : String) : String?{
        val profilePointWithPhysicalMapping = modelDef.points.find {it.devicePointAssociation?.devicePointDomainName == rawPointName && pointIsPresentInConfig(it, profileConfiguration) }
            ?: return null

        val associationPoints = modelDef.points.filter {
            it.configuration.configType == PointConfiguration.ConfigType.ASSOCIATION
        }

        //Linked profile point is an association point. Attach the link if it is enabled.
        val associationPoint = associationPoints.find {
            val associationConfig = it.configuration as AssociationConfiguration
            profilePointWithPhysicalMapping.domainName == associationConfig.domainName && pointIsPresentInConfig(it, profileConfiguration)
        }

        if (associationPoint != null) {
            if (isAssociationEnabled(associationPoint, profileConfiguration)) {
                val profileConfig =
                    profileConfiguration.getAssociationConfigs()
                        .find { it.domainName == associationPoint.domainName }

                val constraint = associationPoint.valueConstraint as MultiStateConstraint
                return constraint.allowedValues.find { it.index == profileConfig?.associationVal!! }?.value
            }
        } else {
            profilePointWithPhysicalMapping.devicePointAssociation?.devicePointDomainName?.let {
                if (it == rawPointName) {
                    return profilePointWithPhysicalMapping.domainName
                }
            }
        }

        return null
    }

    private fun pointIsPresentInConfig(point: SeventyFiveFProfilePointDef, config: ProfileConfiguration): Boolean {
        if (point.configuration.configType == PointConfiguration.ConfigType.DEPENDENT) {
            if (getEnabledDependencies(config).find { it == point.domainName } == null) return false
        }
        return true
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

    // prints log message
    fun logIt(msg: String, exception: Exception? = null) {
        if (exception != null) {
            CcuLog.e(Domain.LOG_TAG, msg, exception)
        } else {
            CcuLog.i(Domain.LOG_TAG, msg)
        }
    }

}