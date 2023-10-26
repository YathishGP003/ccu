package a75f.io.domain.config

import a75f.io.domain.api.EntityConfig

/**
 * A profile configuration is a sort of DTO object that stores all the configuration values required to
 * populate UI for a particular profile. When a configuration is saved , the UI shall update the
 * new configuration values user has entered and pass it to the EntityMapper.
 *
 * Each profile shall override these methods and provide list of domainNames for further processing
 * by the framework.
 */
abstract class ProfileConfiguration (var nodeAddress : Int, var nodeType : String, var priority : Int, var roomRef : String, var floorRef : String) {


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
    abstract fun getDependencies() : List<ValueConfig>

}
