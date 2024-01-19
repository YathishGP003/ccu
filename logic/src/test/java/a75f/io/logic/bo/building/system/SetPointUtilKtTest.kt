package a75f.io.logic.bo.building.system

import junit.framework.TestCase

/**
 * Created by Manjunath K on 27-10-2023.
 */

class SetPointUtilKtTest : TestCase() {
    private val spMin = 35.0
    private val spMax = 105.0

    private val spHeatingMin = 85.0
    private val spHeatingMax = 105.0

    private val spCoolingMin = 35.0
    private val spCoolingMax = 68.0
    fun testMapToSetPoint() {
        for (i in 0..100) {
            println("$spMin $spMax $i ${mapToSetPoint(spMin,spMax,i.toDouble())}")
            println("$spHeatingMin  $spHeatingMax   $i  ${mapToSetPoint(spHeatingMin,spHeatingMax,i.toDouble())}")
            println("$spCoolingMin  $spCoolingMax   $i  ${mapToSetPoint(spCoolingMin,spCoolingMax,i.toDouble())}")
        }
    }

    fun testTestMapToSetPoint() {}
}