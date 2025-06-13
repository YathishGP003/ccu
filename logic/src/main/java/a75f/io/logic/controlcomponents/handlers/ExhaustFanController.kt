package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 07-05-2025.
 */

class ExhaustFanController(
    outsideAirFinalLoopOutput: Point,
    exhaustFanStageThreshold: Point,
    exhaustFanHysteresis: Point,
    logTag: String
) : ThresholdRelayController(
    currentPoint = outsideAirFinalLoopOutput,
    threshold = exhaustFanStageThreshold,
    hysteresis = exhaustFanHysteresis,
    logTag = logTag,
)