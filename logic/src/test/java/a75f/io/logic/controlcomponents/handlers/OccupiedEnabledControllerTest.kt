package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.bo.building.schedules.Occupancy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OccupiedEnabledControllerTest {
    @Test
    fun controllerActivatesWhenOccupancyIsOccupied() {
        val fakeOccupancy = CalibratedPoint("", "TestRef" , Occupancy.OCCUPIED.ordinal.toDouble())
        val controller = OccupiedEnabledController(fakeOccupancy, "TestController")
        assertTrue(controller.runController())
    }

    @Test
    fun controllerDeactivatesWhenOccupancyIsNotOccupied() {
        val fakeOccupancy = CalibratedPoint("", "TestRef" , Occupancy.UNOCCUPIED.ordinal.toDouble())
        val controller = OccupiedEnabledController(fakeOccupancy, "TestController")
        assertFalse(controller.runController())
    }

}