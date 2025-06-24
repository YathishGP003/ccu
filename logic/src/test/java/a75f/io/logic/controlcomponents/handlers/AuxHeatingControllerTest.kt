package a75f.io.logic.controlcomponents.handlers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by Manjunath K on 26-05-2025.
 */

class AuxHeatingControllerTest {

    @Test
    fun controllerActivatesWhenCurrentTempIsBelowThreshold() {
        val currentTemp = MockPoint(15.0)
        val heatingDesiredTemp = MockPoint(25.0)
        val auxHeatingActivateTuner = MockPoint(5.0)
        val controller = AuxHeatingController("AuxHeating", currentTemp, heatingDesiredTemp, auxHeatingActivateTuner, "TestLog")
        assertTrue(controller.runController())
    }

    @Test
    fun controllerDeactivatesWhenCurrentTempExceedsThreshold() {
        val currentTemp = MockPoint(21.0)
        val heatingDesiredTemp = MockPoint(25.0)
        val auxHeatingActivateTuner = MockPoint(5.0)
        val controller = AuxHeatingController("AuxHeating", currentTemp, heatingDesiredTemp, auxHeatingActivateTuner, "TestLog")
        assertFalse(controller.runController())
    }

    @Test
    fun controllerRemainsInactiveWhenCurrentTempIsZero() {
        val currentTemp = MockPoint(0.0)
        val heatingDesiredTemp = MockPoint(25.0)
        val auxHeatingActivateTuner = MockPoint(5.0)
        val controller = AuxHeatingController("AuxHeating", currentTemp, heatingDesiredTemp, auxHeatingActivateTuner, "TestLog")
        assertFalse(controller.runController())
    }

}