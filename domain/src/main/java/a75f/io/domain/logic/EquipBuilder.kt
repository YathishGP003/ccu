package a75f.io.domain.logic

import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.model.ModelDef

class EquipBuilder {
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDef) {
        val entityMapper = EntityMapper(modelDef)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)
    }
}