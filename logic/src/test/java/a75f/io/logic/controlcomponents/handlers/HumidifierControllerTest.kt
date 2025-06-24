package a75f.io.logic.controlcomponents.handlers


/**
 * Created by Manjunath K on 05-05-2025.
 */

import a75f.io.domain.api.Point
import a75f.io.domain.util.CalibratedPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HumidifierControllerTest {


    @Test
    fun controllerTurnsOnWhenHumidityIsBelowMinAndAboveZero() {
        val fakePoint = MockPoint(30.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertTrue(controller.runController())
    }

    @Test
    fun controllerRemainsOffWhenHumidityIsAboveMinPlusHysteresis() {
        val fakePoint = MockPoint(60.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertFalse(controller.runController())
    }

    @Test
    fun controllerRemainsOffWhenHumidityEqualsMinPlusHysteresis() {
        val fakePoint = MockPoint(55.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertFalse(controller.runController())
    }

    @Test
    fun controllerRemainsOffWhenHumidityIsZero() {
        val fakePoint = MockPoint(0.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertFalse(controller.runController())
    }

    @Test
    fun controllerTurnsOffWhenHumidityExceedsMinPlusHysteresis() {
        val fakePoint = MockPoint(56.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertFalse(controller.runController())
    }

    @Test
    fun controllerRemainsEnabledWhenHumidityBetweenMinAndMinPlusHysteresis() {
        val fakePoint = MockPoint(50.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertFalse(controller.runController())
    }

    @Test
    fun controllerTurnsOnWhenHumidityIsExactlyMin() {
        val fakePoint = MockPoint(45.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertFalse(controller.runController())
    }

    @Test
    fun controllerRemainsOffWhenHumidityIsNegative() {
        val fakePoint = MockPoint(-10.0)
        val targetMinHumidity = MockPoint(45.0)
        val hysteresis = MockPoint(10.0)
        val controller = HumidifierController(fakePoint, targetMinHumidity = targetMinHumidity, hysteresis = hysteresis, "TestController",
            CalibratedPoint("occupancy", "", 1.0) // Simulating occupancy
        )
        assertFalse(controller.runController())
    }


 }