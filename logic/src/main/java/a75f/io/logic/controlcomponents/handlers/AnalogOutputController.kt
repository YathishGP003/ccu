package a75f.io.logic.controlcomponents.handlers

import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 08-05-2025.
 */

fun doAnalogOperation(
    conditioningActive: Boolean, analogOutStages: HashMap<String, Int>,
    statusName: String? = null, loopOutput: Int, signalLogicalPoint: Point
) {
    val signal = if (conditioningActive) loopOutput.toDouble() else 0.0
    if (statusName != null && conditioningActive && loopOutput > 0) {
        analogOutStages[statusName] = loopOutput
    }
    signalLogicalPoint.writeHisVal(signal)
}
