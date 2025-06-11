package a75f.io.logic.bo.building.system

import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

open class DefaultSystemConfig(val model: SeventyFiveFProfileDirective) :
    ProfileConfiguration(99, "", 0, "SYSTEM", "SYSTEM", model.domainName) {

    open fun getDefaultConfiguration(): DefaultSystemConfig {
        isDefault = true
        return this
    }

    open fun getActiveConfiguration(): DefaultSystemConfig {
        isDefault = false
        return this
    }

    override fun getAssociationConfigs(): List<AssociationConfig> {
        return mutableListOf<AssociationConfig>().apply {
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf()
    }

    override fun getEnableConfigs(): List<EnableConfig> {
        return mutableListOf<EnableConfig>().apply {
        }
    }

    override fun toString(): String {
        return "DefaultSystemConfig (model = $model)"
    }
}