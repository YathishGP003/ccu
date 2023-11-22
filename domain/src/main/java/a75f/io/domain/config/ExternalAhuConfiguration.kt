package a75f.io.domain.config

import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.satSetpointControlEnable
import a75f.io.domain.api.DomainName.staticPressureSetpointControlEnable
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
/**
 * Created by Manjunath K on 13-06-2023.
 */

class ExternalAhuConfiguration : ProfileConfiguration(-1, "SYSTEM", 0, "SYSTEM", "SYSTEM" ) {

    var setPointControl = EnableConfig(satSetpointControlEnable)
    var dualSetPointControl = EnableConfig(dualSetpointControlEnable)
    var fanStaticSetPointControl = EnableConfig(staticPressureSetpointControlEnable)
    var dcvControl = EnableConfig(dcvDamperControlEnable)
    var occupancyMode = EnableConfig(occupancyModeControl)
    var humidifierControl = EnableConfig(humidifierOperationEnable)
    var dehumidifierControl = EnableConfig(dehumidifierOperationEnable)

    var satMin = ValueConfig(systemSATMinimum)
    var satMax = ValueConfig(systemSATMaximum)
    var heatingMinSp = ValueConfig(systemHeatingSATMinimum)
    var heatingMaxSp = ValueConfig(systemHeatingSATMaximum)
    var coolingMinSp = ValueConfig(systemCoolingSATMinimum)
    var coolingMaxSp = ValueConfig(systemCoolingSATMaximum)
    var fanMinSp = ValueConfig(systemStaticPressureMinimum)
    var fanMaxSp = ValueConfig(systemStaticPressureMaximum)
    var dcvMin = ValueConfig(systemDCVDamperPosMinimum)
    var dcvMax = ValueConfig(systemDCVDamperPosMaximum)

    override fun getAssociationConfigs() : List<AssociationConfig> {
        val associations = mutableListOf<AssociationConfig>()
        associations.add(AssociationConfig("relay1Association", 0))
        return associations
    }

    override fun getDependencies(): List<ValueConfig> {
        val valueConfiguration = mutableListOf<ValueConfig>()
        valueConfiguration.add(satMin)
        valueConfiguration.add(satMax)
        valueConfiguration.add(heatingMinSp)
        valueConfiguration.add(heatingMaxSp)
        valueConfiguration.add(coolingMinSp)
        valueConfiguration.add(coolingMaxSp)
        valueConfiguration.add(fanMinSp)
        valueConfiguration.add(fanMaxSp)
        valueConfiguration.add(dcvMin)
        valueConfiguration.add(dcvMax)
        return valueConfiguration
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        val enabled = mutableListOf<EnableConfig>()
        enabled.add(setPointControl)
        enabled.add(dualSetPointControl)
        enabled.add(fanStaticSetPointControl)
        enabled.add(dcvControl)
        enabled.add(occupancyMode)
        enabled.add(humidifierControl)
        enabled.add(dehumidifierControl)
        return enabled
    }
}