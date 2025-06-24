package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.util.CalibratedPoint
import a75f.io.logic.controlcomponents.controls.Constraint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Created by Manjunath K on 26-05-2025.
 */

class StageControlHandlerTest {

    val stageUp = MockPoint(0.0)
    val stageDown = MockPoint(0.0)

    @Test
    fun controllerActivatesStageWhenLoopOutputExceedsThreshold() {
        val loopOutput = MockPoint(60.0)
        val hysteresis = MockPoint(5.0)

        val totalStages = CalibratedPoint("3", "TestRef", 3.0)
        val economizingAvailable = CalibratedPoint("economizingAvailable", "TestRef", 1.0)
        val controller = StageControlHandler("StageControl", loopOutput, hysteresis, totalStages, economizingAvailable = economizingAvailable, stageUpTimer = stageUp, stageDownTimer = stageDown, logTag = "TestLog")
        assertTrue(controller.runController().any { it.second })
    }

    @Test
    fun controllerDeactivatesStageWhenLoopOutputFallsBelowThreshold() {
        val loopOutput = MockPoint(10.0)
        val hysteresis = MockPoint(5.0)
        val totalStages = CalibratedPoint("3", "TestRef", 3.0)
        val economizingAvailable = CalibratedPoint("economizingAvailable", "TestRef", 1.0)
        val controller = StageControlHandler("StageControl", loopOutput, hysteresis, totalStages, economizingAvailable = economizingAvailable, stageUpTimer = stageUp, stageDownTimer = stageDown, logTag = "TestLog")
        assertFalse(controller.runController().any { it.second })
    }

    @Test
    fun controllerHandlesZeroStagesGracefully() {
        val loopOutput = MockPoint(50.0)
        val hysteresis = MockPoint(5.0)
        val totalStages = CalibratedPoint("0", "TestRef", 0.0)
        val economizingAvailable = CalibratedPoint("economizingAvailable", "TestRef", 1.0)
        val controller = StageControlHandler("StageControl", loopOutput, hysteresis, totalStages, economizingAvailable = economizingAvailable, stageUpTimer = stageUp, stageDownTimer = stageDown, logTag = "TestLog")
        assertTrue(controller.runController().isEmpty())
    }

    @Test
    fun controllerActivatesStageWhenCustomOnConstraintIsAdded() {
        val loopOutput = MockPoint(30.0)
        val hysteresis = MockPoint(5.0)
        val totalStages = CalibratedPoint("3", "TestRef", 3.0)
        val economizingAvailable = CalibratedPoint("economizingAvailable", "TestRef", 1.0)
        val controller = StageControlHandler("StageControl", loopOutput, hysteresis, totalStages, economizingAvailable = economizingAvailable, stageUpTimer = stageUp, stageDownTimer = stageDown, logTag = "TestLog")
        controller.addOnConstraint(0, Constraint { loopOutput.readHisVal() == 30.0 })
        assertTrue(controller.runController().any { it.first == 0 && it.second })
    }

    @Test
    fun controllerDeactivatesStageWhenCustomOffConstraintIsAdded() {
        val loopOutput = MockPoint(30.0)
        val hysteresis = MockPoint(5.0)
        val totalStages = CalibratedPoint("3", "TestRef", 3.0)
        val economizingAvailable = CalibratedPoint("economizingAvailable", "TestRef", 1.0)
        val controller = StageControlHandler("StageControl", loopOutput, hysteresis, totalStages, economizingAvailable = economizingAvailable, stageUpTimer = stageUp, stageDownTimer = stageDown, logTag = "TestLog")
        controller.addOffConstraint(0, Constraint { loopOutput.readHisVal() == 30.0 })
        assertTrue(controller.runController().any { it.first == 0 && it.second })
    }
}