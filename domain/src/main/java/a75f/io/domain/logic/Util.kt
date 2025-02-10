package a75f.io.domain.logic

import a75f.io.domain.config.ProfileConfiguration
import a75f.io.logger.CcuLog

fun Boolean.toInt() = if (this) 1 else 0
fun Boolean.toDouble() = if (this) 1.0 else 0.0

fun hasChanges(oldConfig: ProfileConfiguration, newConfig: ProfileConfiguration): Boolean {

    oldConfig.getEnableConfigs().forEach { oldConfigItem ->
        newConfig.getEnableConfigs().find { newConfigItem ->
            oldConfigItem.domainName == newConfigItem.domainName &&
                    oldConfigItem.enabled != newConfigItem.enabled
        }
    }

    oldConfig.getValueConfigs().forEach { oldConfigItem ->
        newConfig.getValueConfigs().find { newConfigItem ->
            oldConfigItem.domainName == newConfigItem.domainName &&
                    oldConfigItem.currentVal != newConfigItem.currentVal
        }
    }

    oldConfig.getAssociationConfigs().forEach { oldConfigItem ->
        newConfig.getAssociationConfigs().find { newConfigItem ->
            oldConfigItem.domainName == newConfigItem.domainName &&
                    oldConfigItem.associationVal != newConfigItem.associationVal
        }
    }

    return false
}