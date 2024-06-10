package a75f.io.logic.bo.building.system.util

/**
 * Created by Manjunath K on 23-05-2024.
 */

/**
 * This function is used to get the modulated output based on the loop output
 */
fun getModulatedOutput(loopOutput: Double, min : Double, max : Double)  = (((max - min) * (loopOutput / 100.0)) + min)

/**
 * This function is get the mid point of min max
 */
fun getComposeMidPoint(minMax: Pair<Double,Double>) = (((minMax.first + minMax.second) / 2).coerceIn(0.0,10.0))