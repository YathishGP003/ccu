package a75f.io.domain.logic

import a75f.io.domain.api.Domain
import a75f.io.domain.api.EntityConfig
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.config.getConfig
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.AssociationConfiguration
import io.seventyfivef.domainmodeler.common.point.ComparisonType
import io.seventyfivef.domainmodeler.common.point.Condition
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.DependentConfiguration
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.Operator
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
            val profileConfig = profileConfiguration.getAssociationConfigs().find { it.domainName == def.domainName }

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

        // conditions list will always have ony one element because it is association point
        val conditionConfig = (pointDef.configuration as AssociationConfiguration).conditions.first()
        val baseConfig = profileConfiguration.getEnableConfigs()
            .find { point -> point.domainName == conditionConfig.domainName }

        return baseConfig != null && evaluateConfiguration(
            conditionConfig.comparisonType,
            conditionConfig.value as Int,
            baseConfig.enabled.toInt()
        )
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
            val currentPoint = def.configuration as? DependentConfiguration ?: return@forEach
            val conditions = currentPoint.conditions

            val operators = mutableListOf<Operator>()
            val statusList = mutableListOf<Boolean>()

            conditions.forEach { condition ->
                val status = evaluationCondition(condition, profileConfiguration)
                statusList.add(status)
                operators.add(condition.operator)
            }

            if (applyLogicalOperations(statusList, operators)) {
                enabledDependencies.add(def.domainName)
            }
        }
        return enabledDependencies
    }

    private fun getMultiDependentPoints(configuration: ProfileConfiguration, pointsToAdd: MutableList<EntityConfig>): List<String> {
        val dynamicConfigs = mutableListOf<String>()

        getDependentPoints().forEach { def ->
            val currentPoint = def.configuration as? DependentConfiguration ?: return@forEach
            val conditions = currentPoint.conditions
            logIt("-------------------------------------------")
            logIt("Checking dynamic point ${def.domainName}\n ( with conditions ${getDependencyLog(conditions)})")

            val operators = mutableListOf<Operator>()
            val statusList = mutableListOf<Boolean>()

            conditions.forEach { condition ->
                val status = evaluationCondition(condition, configuration, pointsToAdd)
                statusList.add(status)
                operators.add(condition.operator)
            }
            val shouldAdd = applyLogicalOperations(statusList, operators)

            logIt("${def.domainName}  $statusList  $operators shouldAdd $shouldAdd")
            if (shouldAdd) {
                dynamicConfigs.add(def.domainName)
            }
            getEnumPointIfExist(def, configuration)?.let { enumPoint ->

                if (!enumPoint.contentEquals("off", ignoreCase = true)
                    && !enumPoint.contentEquals("on", ignoreCase = true)
                    && !enumPoint.contentEquals("true", ignoreCase = true)
                    && !enumPoint.contentEquals("false", ignoreCase = true)) {
                    dynamicConfigs.add(enumPoint)
                    logIt("Added Dynamic enum point $enumPoint")
                }

            }
            logIt("-------------------------------------------")
        }
        dynamicConfigs.addAll(getCustomPoints(configuration).map { it })
        return dynamicConfigs
    }


    fun evaluationCondition(condition: Condition, configuration: ProfileConfiguration, pointsToAdd: MutableList<EntityConfig> = mutableListOf()): Boolean {

        logIt("evaluationCondition is ${condition.domainName} ${condition.dependentConfigType}")
        return when (condition.dependentConfigType) {

            PointConfiguration.ConfigType.BASE, PointConfiguration.ConfigType.DEPENDENT -> {

                val findConfig = configuration.getEnableConfigs().find { point -> point.domainName == condition.domainName }
                if (findConfig != null) {
                    logIt("found in getEnableConfigs is ${findConfig.domainName} ${findConfig.enabled}")
                    (evaluateConfiguration(condition.comparisonType, condition.value as Int, findConfig.enabled.toInt()))
                } else {
                    val valueConfig = configuration.getValueConfigs().find { point -> point.domainName == condition.domainName }
                    logIt("found in getValueConfigs is ${valueConfig?.domainName} ${valueConfig?.currentVal}")
                    if (valueConfig != null) {
                        (evaluateConfiguration(condition.comparisonType, condition.value as Int, valueConfig.currentVal.toInt()))
                    } else {
                        logIt("dependency not found in getEnableConfigs or getValueConfigs")
                        false
                    }
                }
            }

            PointConfiguration.ConfigType.ASSOCIATION -> {
                logIt("condition ${condition.domainName} checking isAssociatedConfigEnabled")
                if (isAssociatedConfigEnabled(condition, configuration)) {
                    val associationConfig =
                        configuration.getAssociationConfigs().getConfig(condition.domainName)
                    associationConfig?.associationVal?.let {
                        evaluateConfiguration(condition.comparisonType, condition.value as Int, it)
                    } ?: false
                } else {
                    false
                }
            }

            PointConfiguration.ConfigType.ASSOCIATED -> {
                val isDependencyExist = pointsToAdd.find { it.domainName == condition.domainName }
                isDependencyExist != null
            }

            PointConfiguration.ConfigType.DYNAMIC_SENSOR -> {
                // TODO need to check how to handle dynamic sensor points
                false
            }

        }
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
                return when (value) {
                    is ValueConfig -> constraint.allowedValues[value.currentVal.toInt()].value
                    is EnableConfig -> constraint.allowedValues[value.enabled.toInt()].value
                    is AssociationConfig -> constraint.allowedValues.find { it.index == value.associationVal }?.value
                    else -> { null }
                }
            }
        }
       return null
    }


    /**
     * Returns true if the associated config is enabled for dynamic points.
     */
    private fun isAssociatedConfigEnabled(associationConfig: Condition, config: ProfileConfiguration): Boolean {
        logIt("associationConfig checking ${associationConfig.domainName} ")
        val dependPointDef = getPointByDomainName(associationConfig.domainName)
        logIt("get association point def $dependPointDef")
        val dependentConfig = (dependPointDef!!.configuration as? AssociationConfiguration)
        logIt("get dependentConfig $dependentConfig")
        val enabledConfig = dependentConfig?.conditions?.first()
        logIt("associationConfig depends on this config ${enabledConfig?.domainName} ")

        if (enabledConfig == null) {
            return false
        }
        val configPoint = config.getEnableConfigs().find { it.domainName == enabledConfig.domainName }
        logIt("finding in getEnableConfigs $dependentConfig")
        if (configPoint != null) {
            return evaluateConfiguration(
                enabledConfig.comparisonType,
                enabledConfig.value as Int,
                configPoint.enabled.toInt()
            )
        }
        return false
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
            profilePointWithPhysicalMapping.domainName == associationConfig.conditions.first().domainName && pointIsPresentInConfig(it, profileConfiguration)
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
    private fun getDependencyLog(condition: Set<Condition>): String {
        val log = StringBuilder()
        condition.forEach {
            log.append("\n${it.domainName} ${it.comparisonType} ${it.value} ${it.dependentConfigType} ${it.operator}")
        }
        return log.toString()
    }

}