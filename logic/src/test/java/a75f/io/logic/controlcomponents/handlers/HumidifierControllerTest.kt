package a75f.io.logic.controlcomponents.handlers


/**
 * Created by Manjunath K on 05-05-2025.
 */

import a75f.io.domain.api.Point
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HumidifierControllerTest {


    class MockPoint(var value: Double) : Point("fake Name","equipRef") {
        override fun readHisVal(): Double = value
    }

    @Test
    fun `test controller initializes correctly`() {
        val fakePoint = MockPoint(25.0)
        val controller = HumidifierController(fakePoint, min = 45.0, hysteresis = 10.0)
        assertTrue(controller.isEnabled()) // Default state should be off
    }
    @Test
    fun controllerEnablesWhenHumidityBelowMin() {
        val fakePoint = MockPoint(40.0)
        val controller = HumidifierController(fakePoint, min = 45.0, hysteresis = 10.0)
        assertTrue(controller.isEnabled())
    }

    @Test
    fun controllerDisablesWhenHumidityAboveMinPlusHysteresis() {
        val fakePoint = MockPoint(56.0)
        val controller = HumidifierController(fakePoint, min = 45.0, hysteresis = 10.0)
        assertFalse(controller.isEnabled())
    }

    @Test
    fun controllerRemainsDisabledWhenHumidityEqualsMin() {
        val fakePoint = MockPoint(45.0)
        val controller = HumidifierController(fakePoint, min = 45.0, hysteresis = 10.0)
        assertFalse(controller.isEnabled())
    }

    @Test
    fun controllerRemainsDisabledWhenHumidityEqualsMinPlusHysteresis() {
        val fakePoint = MockPoint(55.0)
        val controller = HumidifierController(fakePoint, min = 45.0, hysteresis = 10.0)
        assertFalse(controller.isEnabled())
    }

    @Test
    fun controllerRemainsEnabledWhenHumidityBetweenMinAndMinPlusHysteresis() {
        val fakePoint = MockPoint(44.0)
        val controller = HumidifierController(fakePoint, min = 45.0, hysteresis = 10.0)
        assertTrue(controller.isEnabled())
        fakePoint.value = 50.0
        assertTrue(controller.isEnabled())
    }


 }