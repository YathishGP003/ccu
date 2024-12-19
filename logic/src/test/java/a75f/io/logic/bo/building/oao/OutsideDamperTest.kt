package a75f.io.logic.bo.building.oao

import a75f.io.logic.bo.building.definitions.ProfileType
import junit.framework.TestCase.assertEquals
import org.junit.Test

internal class OutsideDamperTest {
    private val outsideDamper: OAOProfile = OAOProfile()

    @Test
    fun testSystemDabStagedRtuAndVavStagedRtu() {
        val fanValue = 10.0
        val conditioningValue = 5.0

        assertEquals(
            fanValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_DAB_STAGED_RTU
            )
        )
        assertEquals(
            fanValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_VAV_STAGED_RTU
            )
        )
    }

    @Test
    fun testSystemDabStagedVfdRtuAndSimilarProfiles() {
        val fanValue = 10.0
        val conditioningValue = 15.0

        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_DAB_STAGED_VFD_RTU
            )
        )
        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_VAV_STAGED_VFD_RTU
            )
        )
        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_VAV_ADVANCED_AHU
            )
        )
        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_DAB_ADVANCED_AHU
            )
        )
        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_DAB_HYBRID_RTU
            )
        )
        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_VAV_HYBRID_RTU
            )
        )
    }

    @Test
    fun testSystemVavAnalogRtuAndDabAnalogRtu() {
        val fanValue = 10.0
        val conditioningValue = 15.0

        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_VAV_ANALOG_RTU
            )
        )
        assertEquals(
            conditioningValue,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.SYSTEM_DAB_ANALOG_RTU
            )
        )
    }

    @Test
    fun testInvalidSystemProfile() {
        val fanValue = 10.0
        val conditioningValue = 15.0

        assertEquals(
            0.0,
            outsideDamper.getOutsideDamperMinOpen(
                fanValue, conditioningValue, ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT
            )
        )
    }
}
