package a75f.io.domain.config

import a75f.io.domain.api.EntityConfig
import a75f.io.domain.api.Point
import android.util.Log
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.NumericConstraint

/**
 * A profile configuration is a sort of DTO object that stores all the configuration values required to
 * populate UI for a particular profile. When a configuration is saved , the UI shall update the
 * new configuration values user has entered and pass it to the EntityMapper.
 *
 * Each profile shall override these methods and provide list of domainNames for further processing
 * by the framework.
 */
abstract class ProfileConfiguration (var nodeAddress : Int, var nodeType : String, var priority : Int, var roomRef : String, var floorRef : String, var profileType : String) {

    var isDefault = false

    /**
     * This is the Equip Id of the profile.
     * It is used to identify the profile in the domain.
     */

    var equipId :String = ""

    /**
     * Get a list of domainNames of all base-configs
     * This need not have all base points.
     * Only configs which are configured via UI.
     *
     */
    open fun getEnableConfigs() : List<EnableConfig> {
        return emptyList()
    }

    /**
     * Get a list of domainNames of all associations
     *
     */
    open fun getAssociationConfigs() : List<AssociationConfig> {
        return emptyList()
    }

    /**
     * Get a list of domainNames of all String value configs
     * This need not have all base points.
     * Only configs which are configured via UI.
     *
     */

    open fun getStringConfigs() : List<StringValueConfig> {
        return emptyList()
    }

    /**
     * List of points which frameworks is asked to added every time without running any logic.
     */
    open fun getBaseProfileConfigs() : List<EntityConfig> {
        return emptyList()
    }

    open fun getConfigByDomainName(domainName: String): Any? {
        return ValueConfig(domainName, 0.0)
    }


    /**
     * Get a list of domainNames of all dependencies
     *
     */
    abstract fun getDependencies() : List<ValueConfig>

    open fun getValueConfigs() : List<ValueConfig> {
        return emptyList()
    }

    fun getDefaultValConfig(domainName : String, model : SeventyFiveFProfileDirective) :ValueConfig {
        val point = model.points.find { it.domainName == domainName }
        val config = ValueConfig(domainName, point?.defaultValue?.toString()?.toDouble() ?: 0.0)
        config.disName = point?.name ?:""
        if (point?.valueConstraint is NumericConstraint) {
            config.minVal = (point.valueConstraint as NumericConstraint).minValue
            config.maxVal = (point.valueConstraint as NumericConstraint).maxValue
        }
        point?.presentationData?.get("tagValueIncrement")?.let { config.incVal = it.toString().toDouble() }
        return config
    }
    /**
     * Get a default string config for the given domainName.
     * If the point is not found, it returns an empty string config.
     */

    fun getDefaultStringConfig(domainName : String, model : SeventyFiveFProfileDirective) : StringValueConfig {
        val point = model.points.find { it.domainName == domainName }
        val config = StringValueConfig(domainName, point?.defaultValue?.toString() ?: "")
        config.disName = point?.name ?:""
        return config
    }


    /**
     * Override the function when you have custom points to be created
     *
     */
    open fun getCustomPoints() : List<Pair <SeventyFiveFProfilePointDef, Any>> {
        return emptyList()
    }


    fun getDefaultAssociationConfig(
        domainName: String,
        model: SeventyFiveFProfileDirective
    ): AssociationConfig {
        val point = model.points.find { it.domainName == domainName }
        return AssociationConfig(domainName, point?.defaultValue?.toString()?.toInt() ?: 0)
    }
    fun getDefaultEnableConfig(domainName : String, model : SeventyFiveFProfileDirective) : EnableConfig {
        val point = model.points.find { it.domainName == domainName }
        val config = if (point?.defaultValue is Integer) {
            EnableConfig(domainName, (point?.defaultValue as Integer) > 0)
        } else {
            EnableConfig(domainName, point?.defaultValue?.toString()?.toBoolean() ?: false)
        }
        config.disName = point?.name ?:""
        return config
    }

    override fun toString(): String {
        return "nodeAddr $nodeAddress roomRef $roomRef floorRe $floorRef"
    }

    fun getActivePointValue(point: Point, fallback: ValueConfig): Double {
        if (point.pointExists())
            return point.readDefaultVal()
        return fallback.currentVal
    }

}
