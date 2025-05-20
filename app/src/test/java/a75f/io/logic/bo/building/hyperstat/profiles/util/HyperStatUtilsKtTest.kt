package a75f.io.logic.bo.building.hyperstat.profiles.util

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstat.v2.configs.CpuConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.junit.Assert.assertEquals
import org.junit.Test


/**
 * Created by Manjunath K on 30-10-2024.
 */

class HyperStatUtilsKtTest {

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
    fun testFanOffAuto() {
        // these will work with any fan level as they are 0 and 1
        assert(getSelectedFanMode(1, 0) == StandaloneFanStage.OFF.ordinal)
        assert(getSelectedFanMode(1, 1) == StandaloneFanStage.AUTO.ordinal)
    }

    @Test
    fun testAllSelectedFanMode() {
        assert(getSelectedFanMode(21, 2) == StandaloneFanStage.LOW_CUR_OCC.ordinal)
        assert(getSelectedFanMode(21, 3) == StandaloneFanStage.LOW_OCC.ordinal)
        assert(getSelectedFanMode(21, 4) == StandaloneFanStage.LOW_ALL_TIME.ordinal)
        assert(getSelectedFanMode(21, 5) == StandaloneFanStage.MEDIUM_CUR_OCC.ordinal)
        assert(getSelectedFanMode(21, 6) == StandaloneFanStage.MEDIUM_OCC.ordinal)
        assert(getSelectedFanMode(21, 7) == StandaloneFanStage.MEDIUM_ALL_TIME.ordinal)
        assert(getSelectedFanMode(21, 8) == StandaloneFanStage.HIGH_CUR_OCC.ordinal)
        assert(getSelectedFanMode(21, 9) == StandaloneFanStage.HIGH_OCC.ordinal)
        assert(getSelectedFanMode(21, 10) == StandaloneFanStage.HIGH_ALL_TIME.ordinal)
        // Negative test cases
        assert(getSelectedFanMode(5668, 5587) == StandaloneFanStage.OFF.ordinal)
    }

    @Test
    fun testOnlyLowSelectedFanMode() {
        // when only low fan options are available
        assert(getSelectedFanMode(6, 0) == StandaloneFanStage.OFF.ordinal)
        assert(getSelectedFanMode(6, 1) == StandaloneFanStage.AUTO.ordinal)
        assert(getSelectedFanMode(6, 2) == StandaloneFanStage.LOW_CUR_OCC.ordinal)
        assert(getSelectedFanMode(6, 3) == StandaloneFanStage.LOW_OCC.ordinal)
        assert(getSelectedFanMode(6, 4) == StandaloneFanStage.LOW_ALL_TIME.ordinal)
        // Negative test cases
        assert(getSelectedFanMode(6, 5) == StandaloneFanStage.OFF.ordinal)
    }

    @Test
    fun testOnlyMediumSelectedFanMode() {
        // when only medium fan options are available
        assert(getSelectedFanMode(7, 0) == StandaloneFanStage.OFF.ordinal)
        assert(getSelectedFanMode(7, 1) == StandaloneFanStage.AUTO.ordinal)
        assert(getSelectedFanMode(7, 2) == 5)
        assert(getSelectedFanMode(7, 3) == 6)
        assert(getSelectedFanMode(7, 4) == 7)
        assert(getSelectedFanMode(7, 5) == 2)
        assert(getSelectedFanMode(7, 6) == 3)
        assert(getSelectedFanMode(7, 7) == 4)


    }

    @Test
    fun testOnlyHighSelectedFanMode() {
        // when only high fan options are available
        assert(getSelectedFanMode(8, 0) == StandaloneFanStage.OFF.ordinal)
        assert(getSelectedFanMode(8, 1) == StandaloneFanStage.AUTO.ordinal)
        assert(getSelectedFanMode(8, 8) == 2)
        assert(getSelectedFanMode(8, 9) == 3)
        assert(getSelectedFanMode(8, 10) == 4)
        assert(getSelectedFanMode(8, 2) == 8)
        assert(getSelectedFanMode(8, 3) == 9)
        assert(getSelectedFanMode(8, 4) == 10)

        assert(getSelectedFanMode(8, 5) == StandaloneFanStage.OFF.ordinal)
    }

    @Test
    fun testLowHighSelectedFanMode() {
        // when only high fan options are available
        assert(getSelectedFanMode(LOW_HIGH, 0) == StandaloneFanStage.OFF.ordinal)
        assert(getSelectedFanMode(LOW_HIGH, 1) == StandaloneFanStage.AUTO.ordinal)
        assert(getSelectedFanMode(LOW_HIGH, 2) == 2)
        assert(getSelectedFanMode(LOW_HIGH, 3) == 3)
        assert(getSelectedFanMode(LOW_HIGH, 4) == 4)
        assert(getSelectedFanMode(LOW_HIGH, 8) == 5)
        assert(getSelectedFanMode(LOW_HIGH, 9) == 6)
        assert(getSelectedFanMode(LOW_HIGH, 10) == 7)
        assert(getSelectedFanMode(LOW_HIGH, 5) == 8)
        assert(getSelectedFanMode(LOW_HIGH, 6) == 9)
        assert(getSelectedFanMode(LOW_HIGH, 7) == 10)
    }

    @Test
    fun testLowMediumSelectedFanMode() {
        // when only high fan options are available
        assert(getSelectedFanMode(LOW_MED, 0) == StandaloneFanStage.OFF.ordinal)
        assert(getSelectedFanMode(LOW_MED, 1) == StandaloneFanStage.AUTO.ordinal)

        assert(getSelectedFanMode(LOW_MED, 2) == 2)
        assert(getSelectedFanMode(LOW_MED, 3) == 3)
        assert(getSelectedFanMode(LOW_MED, 4) == 4)
        assert(getSelectedFanMode(LOW_MED, 5) == 5)
        assert(getSelectedFanMode(LOW_MED, 6) == 6)
        assert(getSelectedFanMode(LOW_MED, 7) == 7)

    }
}