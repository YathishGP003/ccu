package a75f.io.renatus.schedules


import a75f.io.logic.bo.building.schedules.EquipOccupancyHandler
import a75f.io.logic.bo.building.schedules.Occupancy
import org.junit.Assert
import org.junit.Before
import org.junit.Test


class EquipOccupancyHandlerTest {

    private lateinit var equipOccupancyHandler: EquipOccupancyHandler

    @Before
    fun setUp() {
        equipOccupancyHandler = EquipOccupancyHandler()
    }
    @Test
    fun isZoneInAutoForced() {
        Assert.assertEquals(
            true,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.AUTOFORCEOCCUPIED,
                true,
                false,
                false
            )
        )
    }
    @Test
    fun isZoneInForcedOccupied() {
        equipOccupancyHandler = EquipOccupancyHandler()
        Assert.assertEquals(
            true,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.FORCEDOCCUPIED,
                true,
                false,
                false
            )
        )
    }
    @Test
    fun isZoneInDRAutoForced() {
        equipOccupancyHandler = EquipOccupancyHandler()
        Assert.assertEquals(
            true,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.DEMAND_RESPONSE_UNOCCUPIED,
                true,
                false,
                true
            )
        )
    }
    @Test
    fun isZoneInDRForcedOccupied() {
        equipOccupancyHandler = EquipOccupancyHandler()
        Assert.assertEquals(
            true,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.DEMAND_RESPONSE_UNOCCUPIED,
                true,
                true,
                false
            )
        )
    }
    @Test
    fun isZoneInOccupied() {
        equipOccupancyHandler = EquipOccupancyHandler()
        Assert.assertEquals(
            false,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.OCCUPIED,
                true,
                true,
                false
            )
        )
    }
    @Test
    fun isZoneInUnoccupied() {
        equipOccupancyHandler = EquipOccupancyHandler()
        Assert.assertEquals(
            false,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.UNOCCUPIED,
                true,
                true,
                false
            )
        )
    }
    @Test
    fun isZoneInDRForcedOccupiedUnoccupied() {
        equipOccupancyHandler = EquipOccupancyHandler()
        Assert.assertEquals(
            false,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.DEMAND_RESPONSE_UNOCCUPIED,
                false,
                true,
                false
            )
        )
    }
    @Test
    fun isZoneInDRAutoForcedOccupiedUnoccupied() {
        equipOccupancyHandler = EquipOccupancyHandler()
        Assert.assertEquals(
            false,
            equipOccupancyHandler.isUnoccupiedToOccupiedTransitionRequired(
                Occupancy.DEMAND_RESPONSE_UNOCCUPIED,
                false,
                false,
                true
            )
        )
    }
}