package a75f.io.logic.bo.building.hyperstat.v2.configs

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.ModelTagDef
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.Version
import io.seventyfivef.domainmodeler.common.point.ModelAssociation
import org.junit.Test

/**
 * Created by Manjunath K on 26-10-2024.
 */

class HyperStatConfigurationTest {


    private fun getCpuConfig(): CpuConfiguration {
        val dummyModel = SeventyFiveFProfileDirective(id = "", domainName = "", name = "",  "", emptySet(), emptySet(), emptyList())
        val cpuConfiguration = CpuConfiguration(1000, NodeType.HYPER_STAT.name, 0, "", "", ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT, dummyModel)
        cpuConfiguration.getDefaultConfiguration()
        cpuConfiguration.relay1Enabled.enabled = true
        cpuConfiguration.relay2Enabled.enabled = true
        cpuConfiguration.relay3Enabled.enabled = true
        cpuConfiguration.relay4Enabled.enabled = true
        cpuConfiguration.relay5Enabled.enabled = true
        cpuConfiguration.relay6Enabled.enabled = true

        cpuConfiguration.analogOut1Enabled.enabled = true
        cpuConfiguration.analogOut2Enabled.enabled = true
        cpuConfiguration.analogOut3Enabled.enabled = true
        return cpuConfiguration
    }


    @Test
    fun testAvailableCoolingStages() {

        val config = getCpuConfig()
        config.relay1Association.associationVal = HsCpuRelayMapping.COOLING_STAGE_1.ordinal
        assert(config.getHighestCoolingStage() == HsCpuRelayMapping.COOLING_STAGE_1)

        config.relay2Association.associationVal = HsCpuRelayMapping.COOLING_STAGE_2.ordinal
        assert(config.getHighestCoolingStage() == HsCpuRelayMapping.COOLING_STAGE_2)

        config.relay3Association.associationVal = HsCpuRelayMapping.COOLING_STAGE_3.ordinal
        assert(config.getHighestCoolingStage() == HsCpuRelayMapping.COOLING_STAGE_3)

    }

    @Test
    fun testAvailableHeatingStages() {
        val config = getCpuConfig()
        config.relay1Association.associationVal = HsCpuRelayMapping.HEATING_STAGE_1.ordinal
        assert(config.getHighestHeatingStage() == HsCpuRelayMapping.HEATING_STAGE_1)

        config.relay2Association.associationVal = HsCpuRelayMapping.HEATING_STAGE_2.ordinal
        assert(config.getHighestHeatingStage() == HsCpuRelayMapping.HEATING_STAGE_2)

        config.relay3Association.associationVal = HsCpuRelayMapping.HEATING_STAGE_3.ordinal
        assert(config.getHighestHeatingStage() == HsCpuRelayMapping.HEATING_STAGE_3)

    }

    @Test
    fun testAvailableFanStages() {

        val config = getCpuConfig()

        config.relay1Association.associationVal = HsCpuRelayMapping.FAN_LOW_SPEED.ordinal
        config.relay1Association.associationVal = HsCpuRelayMapping.FAN_HIGH_SPEED.ordinal

        assert(config.getHighestFanSelected() == HsCpuRelayMapping.FAN_HIGH_SPEED)

        config.relay1Association.associationVal = HsCpuRelayMapping.FAN_LOW_SPEED.ordinal
        assert(config.getHighestFanSelected() == HsCpuRelayMapping.FAN_LOW_SPEED)
        assert(config.getLowestFanSelected() == HsCpuRelayMapping.FAN_LOW_SPEED)

        config.relay2Association.associationVal = HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal
        assert(config.getHighestFanSelected() == HsCpuRelayMapping.FAN_MEDIUM_SPEED)
        assert(config.getLowestFanSelected() == HsCpuRelayMapping.FAN_LOW_SPEED)

        config.relay3Association.associationVal = HsCpuRelayMapping.FAN_HIGH_SPEED.ordinal
        assert(config.getHighestFanSelected() == HsCpuRelayMapping.FAN_HIGH_SPEED)
        assert(config.getLowestFanSelected() == HsCpuRelayMapping.FAN_LOW_SPEED)

        config.relay3Association.associationVal = HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal
        assert(config.getHighestFanSelected() == HsCpuRelayMapping.FAN_MEDIUM_SPEED)

        config.relay3Association.associationVal = HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal
        assert(config.getHighestFanSelected() == HsCpuRelayMapping.FAN_MEDIUM_SPEED)

        config.relay1Association.associationVal = HsCpuRelayMapping.FAN_ENABLED.ordinal
        config.relay2Association.associationVal = HsCpuRelayMapping.FAN_ENABLED.ordinal
        config.relay3Association.associationVal = HsCpuRelayMapping.FAN_MEDIUM_SPEED.ordinal
        assert(config.getLowestFanSelected() == HsCpuRelayMapping.FAN_MEDIUM_SPEED)

    }


    @Test
    fun isHeatingStage1Available() {
        val config = getCpuConfig()
        config.relay1Association.associationVal = HsCpuRelayMapping.HEATING_STAGE_1.ordinal
        assert(config.isHeatingAvailable())
    }

    @Test
    fun isHeatingStage2Available() {
        val config = getCpuConfig()
        config.relay2Association.associationVal = HsCpuRelayMapping.HEATING_STAGE_2.ordinal
        assert(config.isHeatingAvailable())
    }

    @Test
    fun isHeatingStage3Available() {
        val config = getCpuConfig()
        config.relay3Association.associationVal = HsCpuRelayMapping.HEATING_STAGE_3.ordinal
        assert(config.isHeatingAvailable())
    }

    @Test
    fun isCoolingStage1Available() {
        val config = getCpuConfig()
        config.relay1Association.associationVal = HsCpuRelayMapping.COOLING_STAGE_1.ordinal
        assert(config.isCoolingAvailable())
    }

    @Test
    fun isCoolingStage2Available() {
        val config = getCpuConfig()
        config.relay2Association.associationVal = HsCpuRelayMapping.COOLING_STAGE_2.ordinal
        assert(config.isCoolingAvailable())
    }

    @Test
    fun isCoolingStage3Available() {
        val config = getCpuConfig()
        config.relay3Association.associationVal = HsCpuRelayMapping.COOLING_STAGE_3.ordinal
        assert(config.isCoolingAvailable())
    }

}