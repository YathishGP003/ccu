package a75f.io.domain.config

import a75f.io.domain.api.EntityConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.NumericConstraint

/**
 * A profile configuration is a sort of DTO object that stores all the configuration values required to
 * populate UI for a particular profile. When a configuration is saved , the UI shall update the
 * new configuration values user has entered and pass it to the EntityMapper.
 *
 * Each profile shall override these methods and provide list of domainNames for further processing
 * by the framework.
 */
abstract class ProfileConfiguration (var nodeAddress : Int, var nodeType : String, var priority : Int, var roomRef : String, var floorRef : String) {

    var isDefault = false
    /**
     * Get a list of domainNames of all base-configs
     * This need not have all base points.
     * Only configs which are configured via UI.
     *
     */
    abstract fun getEnableConfigs() : List<EnableConfig>

    /**
     * Get a list of domainNames of all associations
     *
     */
    abstract fun getAssociationConfigs() : List<AssociationConfig>

    /**
     * Get a list of domainNames of all dependencies
     *
     */
    //abstract fun getDependencies() : List<ProfileConfig>

    fun getDefaultValConfig(domainName : String, model : SeventyFiveFProfileDirective) :ValueConfig {
        val point = model.points.find { it.domainName == domainName }
        val config = ValueConfig(domainName, point?.defaultValue?.toString()?.toDouble() ?: 0.0)
        config.disName = point?.name ?:""
        if (point?.valueConstraint is NumericConstraint) {
            config.minVal = (point?.valueConstraint as NumericConstraint).minValue
            config.maxVal = (point?.valueConstraint as NumericConstraint).maxValue
        }
        point?.presentationData?.get("tagValueIncrement")?.let { config.incVal = it.toString().toDouble() }
        return config
    }

    fun getDefaultAssociationConfig(domainName : String, model : SeventyFiveFProfileDirective) : AssociationConfig {
        val point = model.points.find { it.domainName == domainName }
        val config = AssociationConfig(domainName, point?.defaultValue?.toString()?.toInt() ?: 0)
        return config
    }

    fun getDefaultEnableConfig(domainName : String, model : SeventyFiveFProfileDirective) : EnableConfig {
        val point = model.points.find { it.domainName == domainName }
        val config = EnableConfig(domainName, point?.defaultValue?.toString()?.toBoolean() ?: false)
        config.disName = point?.name ?:""
        return config
    }

}
