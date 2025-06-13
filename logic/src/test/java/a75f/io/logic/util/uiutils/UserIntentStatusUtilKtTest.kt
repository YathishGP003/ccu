package a75f.io.logic.util.uiutils

import a75f.io.logic.bo.building.ZoneTempState
import a75f.io.logic.bo.building.hvac.StatusMsgKeys
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import org.junit.Assert
import org.junit.Test

/**
 * Created by Manjunath K on 24-04-2025.
 */

class UserIntentStatusUtilKtTest {


    private val coolingPrefix = "Cooling"
    private val heatingPrefix = "Heating"

    @Test
    fun tempStateTest() {

        var tempState = ZoneTempState.NONE

        assert(getTempStateMessage(tempState) == "")

        tempState = ZoneTempState.RF_DEAD
        assert(getTempStateMessage(tempState) == "RF Signal dead ")

        tempState = ZoneTempState.TEMP_DEAD
        assert(getTempStateMessage(tempState) == "Zone Temp Dead ")

        tempState = ZoneTempState.EMERGENCY
        assert(getTempStateMessage(tempState) == "Emergency ")

        tempState = ZoneTempState.FAN_OP_MODE_OFF
        assert(getTempStateMessage(tempState) == "OFF ")

    }


    @Test
    fun coolingStagesTest() {


        val expected1 = "$coolingPrefix 1 ON"
        val expected2 = "$coolingPrefix 1&2 ON"
        val expected3 = "$coolingPrefix 1,2&3 ON"

        val desiredStages = listOf(Stage.COOLING_1, Stage.COOLING_2, Stage.COOLING_3)
        val currentStages = HashMap<String, Int>()

        currentStages[Stage.COOLING_1.displayName] = 1
        val result1 = getStagesStatus(currentStages, coolingPrefix, desiredStages)
        println("$expected1 == $result1")
        assert(result1.contentEquals(expected1))

        currentStages[Stage.COOLING_2.displayName] = 1

        val result2 = getStagesStatus(currentStages, coolingPrefix, desiredStages)
        println("$expected2 == $result2")
        assert(result2.contentEquals(expected2))

        currentStages[Stage.COOLING_3.displayName] = 1

        val result3 = getStagesStatus(currentStages, coolingPrefix, desiredStages)
        println("$expected3 == $result3")
        assert(result3.contentEquals(expected3))

    }

    @Test
    fun heatingStagesTest() {


        val expected1 = "$heatingPrefix 1 ON"
        val expected2 = "$heatingPrefix 1&2 ON"
        val expected3 = "$heatingPrefix 1,2&3 ON"

        val desiredStages = listOf(Stage.HEATING_1, Stage.HEATING_2, Stage.HEATING_3)
        val currentStages = HashMap<String, Int>()

        currentStages[Stage.HEATING_1.displayName] = 1
        val result1 = getStagesStatus(currentStages, heatingPrefix, desiredStages)
        println("$expected1 == $result1")
        assert(result1.contentEquals(expected1))

        currentStages[Stage.HEATING_2.displayName] = 1

        val result2 = getStagesStatus(currentStages, heatingPrefix, desiredStages)
        println("$expected2 == $result2")
        assert(result2.contentEquals(expected2))

        currentStages[Stage.HEATING_3.displayName] = 1

        val result3 = getStagesStatus(currentStages, heatingPrefix, desiredStages)
        println("$expected3 == $result3")
        assert(result3.contentEquals(expected3))
    }

    @Test
    fun fanStagesTest() {
        val prefix = "Fan "

        val expected1 = "$prefix 1 ON"
        val expected2 = "$prefix 1&2 ON"
        val expected3 = "$prefix 1,2&3 ON"

        val desiredStages = listOf(Stage.FAN_1, Stage.FAN_2, Stage.FAN_3)
        val currentStages = HashMap<String, Int>()

        currentStages[Stage.FAN_1.displayName] = 1
        val result1 = getStagesStatus(currentStages, prefix, desiredStages)
        println("$expected1 == $result1")
        assert(result1.contentEquals(expected1))

        currentStages[Stage.FAN_2.displayName] = 1

        val result2 = getStagesStatus(currentStages, prefix, desiredStages)
        println("$expected2 == $result2")
        assert(result2.contentEquals(expected2))

        currentStages[Stage.FAN_3.displayName] = 1

        val result3 = getStagesStatus(currentStages, prefix, desiredStages)
        println("$expected3 == $result3")
        assert(result3.contentEquals(expected3))
    }

    @Test
    fun auxHsHeatTest() {
        val expected1 = "Aux Heating 1 ON"
        val expected2 = "Aux Heating 1&2 ON"

        val currentStages = HashMap<String, Int>()

        currentStages[HsPipe2RelayMapping.AUX_HEATING_STAGE1.name] = 1
        val result1 = getAuxHeatStatus(currentStages)
        println("$expected1 == $result1")
        assert(result1.contentEquals(expected1))

        currentStages[HsPipe2RelayMapping.AUX_HEATING_STAGE2.name] = 1

        val result2 = getAuxHeatStatus(currentStages)
        println("$expected2 == $result2")
        assert(result2.contentEquals(expected2))

    }

    @Test
    fun auxMSHeatTest() {
        val expected1 = "Aux Heating 1 ON"

        val currentStages = HashMap<String, Int>()

        currentStages[MyStatPipe2RelayMapping.AUX_HEATING_STAGE1.name] = 1
        val result1 = getAuxHeatStatus(currentStages)
        println("$expected1 == $result1")
        assert(result1.contentEquals(expected1))



    }

    @Test
    fun analogStatusTest() {
        val expected1 = "$coolingPrefix Analog ON"
        val expected2 = "$coolingPrefix Analog ON | $heatingPrefix Analog ON"
        val expected3 = "$coolingPrefix Analog ON | $heatingPrefix Analog ON | Fan Analog ON"
        val expected4 = "$coolingPrefix Analog ON | $heatingPrefix Analog ON | Fan Analog ON | DCV ON"

        val currentStages = HashMap<String, Int>()

        currentStages[StatusMsgKeys.COOLING.name] = 1
        val result1 = getAnalogStatus(currentStages)
        println("$expected1 == $result1")
        assert(result1.contentEquals(expected1))

        currentStages[StatusMsgKeys.HEATING.name] = 1

        val result2 = getAnalogStatus(currentStages)
        println("$expected2 == $result2")
        assert(result2.contentEquals(expected2))

        currentStages[StatusMsgKeys.FAN_SPEED.name] = 1

        val result3 = getAnalogStatus(currentStages)
        println("$expected3 == $result3")
        assert(result3.contentEquals(expected3))

        currentStages[StatusMsgKeys.DCV_DAMPER.name] = 1

        val result4 = getAnalogStatus(currentStages)
        println("$expected4 == $result4")
        assert(result4.contentEquals(expected4))
    }


    @Test
    fun hsHeatingDirectionTest() {
        val portStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE

        analogOutStages[StatusMsgKeys.HEATING.name] = 1
        analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1

        portStages[Stage.HEATING_1.displayName] = 1
        portStages[Stage.FAN_1.displayName] = 1

        getStatusMsg(
            portStages,
            analogOutStages,
            temperatureState
        ).let { status ->
            println(status)
            println("$heatingPrefix 1 ON, Fan 1 ON, $heatingPrefix Analog ON | Fan Analog ON")
            Assert.assertEquals(
                status,
                "$heatingPrefix 1 ON, Fan 1 ON, $heatingPrefix Analog ON | Fan Analog ON"
            )
        }

        portStages[Stage.HEATING_2.displayName] = 1
        portStages[Stage.FAN_2.displayName] = 1

        getStatusMsg(
            portStages,
            analogOutStages,
            temperatureState
        ).let { status ->
            println(status)
            println("$heatingPrefix 1&2 ON, Fan 1&2 ON, $heatingPrefix Analog ON | Fan Analog ON")
            Assert.assertEquals(
                status,
                "$heatingPrefix 1&2 ON, Fan 1&2 ON, $heatingPrefix Analog ON | Fan Analog ON"
            )
        }

        portStages[Stage.HEATING_3.displayName] = 1
        portStages[Stage.FAN_3.displayName] = 1

        getStatusMsg(
            portStages,
            analogOutStages,
            temperatureState
        ).let { status ->
            println(status)
            println("$heatingPrefix 1,2&3 ON, Fan 1,2&3 ON, $heatingPrefix Analog ON | Fan Analog ON")
            Assert.assertEquals(
                status,
                "$heatingPrefix 1,2&3 ON, Fan 1,2&3 ON, $heatingPrefix Analog ON | Fan Analog ON"
            )
        }
    }


    @Test
    fun hsCoolingDirectionTest() {
        val portStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE


        analogOutStages[StatusMsgKeys.COOLING.name] = 1
        analogOutStages[StatusMsgKeys.FAN_SPEED.name] = 1

        portStages[Stage.COOLING_1.displayName] = 1
        portStages[Stage.FAN_1.displayName] = 1

        getStatusMsg(
            portStages,
            analogOutStages,
            temperatureState
        ).let { status ->
            println(status)
            println("$coolingPrefix 1 ON, Fan 1 ON, $coolingPrefix Analog ON | Fan Analog ON")
            Assert.assertEquals(
                status,
                "$coolingPrefix 1 ON, Fan 1 ON, $coolingPrefix Analog ON | Fan Analog ON"
            )
        }

        portStages[Stage.COOLING_2.displayName] = 1
        portStages[Stage.FAN_2.displayName] = 1

        getStatusMsg(
            portStages,
            analogOutStages,
            temperatureState
        ).let { status ->
            println(status)
            println("$coolingPrefix 1&2 ON, Fan 1&2 ON, $coolingPrefix Analog ON | Fan Analog ON")
            Assert.assertEquals(
                status,
                "$coolingPrefix 1&2 ON, Fan 1&2 ON, $coolingPrefix Analog ON | Fan Analog ON"
            )
        }

        portStages[Stage.COOLING_3.displayName] = 1
        portStages[Stage.FAN_3.displayName] = 1

        getStatusMsg(
            portStages,
            analogOutStages,
            temperatureState
        ).let { status ->
            println(status)
            println("$coolingPrefix 1,2&3 ON, Fan 1&2&3 ON, $coolingPrefix Analog ON | Fan Analog ON")
            Assert.assertEquals(
                status,
                "$coolingPrefix 1,2&3 ON, Fan 1,2&3 ON, $coolingPrefix Analog ON | Fan Analog ON"
            )
        }
    }

    @Test
    fun statusMsgHandlesEmptyInputs() {
        val portStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "OFF")
    }

    @Test
    fun statusMsgHandlesRfDeadState() {
        val portStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.RF_DEAD
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "RF Signal dead")
    }

    @Test
    fun statusMsgHandlesTempDeadState() {
        val portStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.TEMP_DEAD
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "Zone Temp Dead")
    }

    @Test
    fun statusMsgHandlesEmergencyStateWithCoolingStages() {
        val portStages = HashMap<String, Int>()
        portStages[Stage.COOLING_1.displayName] = 1
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.EMERGENCY
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "Emergency, Cooling 1 ON")
    }

    @Test
    fun statusMsgHandlesWaterValveAndHeatingStages() {
        val portStages = HashMap<String, Int>()
        portStages["Water Valve"] = 1
        portStages[Stage.HEATING_1.displayName] = 1
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        println(result)
        assert(result == "Heating 1 ON, Water Valve ON")
    }

    @Test
    fun statusMsgHandlesFanEnabledWithoutAnalogOutputs() {
        val portStages = HashMap<String, Int>()
        portStages[StatusMsgKeys.FAN_ENABLED.name] = 1
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "Fan ON")
    }

    @Test
    fun statusMsgHandlesCoolingAndHeatingStages() {
        val portStages = HashMap<String, Int>()
        portStages[Stage.COOLING_1.displayName] = 1
        portStages[Stage.HEATING_1.displayName] = 1
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "Cooling 1 ON, Heating 1 ON")
    }

    @Test
    fun statusMsgHandlesAnalogOutputsOnly() {
        val portStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        analogOutStages[StatusMsgKeys.COOLING.name] = 1
        analogOutStages[StatusMsgKeys.HEATING.name] = 1
        val temperatureState = ZoneTempState.NONE
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "Cooling Analog ON | Heating Analog ON")
    }

    @Test
    fun statusMsgHandlesAllPossibleStates() {
        val portStages = HashMap<String, Int>()
        portStages[Stage.COOLING_1.displayName] = 1
        portStages[Stage.HEATING_1.displayName] = 1
        portStages["Water Valve"] = 1
        portStages[StatusMsgKeys.FAN_ENABLED.name] = 1
        val analogOutStages = HashMap<String, Int>()
        analogOutStages[StatusMsgKeys.COOLING.name] = 1
        analogOutStages[StatusMsgKeys.HEATING.name] = 1
        val temperatureState = ZoneTempState.EMERGENCY
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        println(result)
        assert(result == "Emergency, Cooling 1 ON, Heating 1 ON, Water Valve ON, Fan ON, Cooling Analog ON | Heating Analog ON")
    }

    @Test
    fun statusMsgHandlesNoFanStagesButFanEnabled() {
        val portStages = HashMap<String, Int>()
        portStages[StatusMsgKeys.FAN_ENABLED.name] = 1
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "Fan ON")
    }

    @Test
    fun statusMsgHandlesNoStagesOrAnalogOutputs() {
        val portStages = HashMap<String, Int>()
        val analogOutStages = HashMap<String, Int>()
        val temperatureState = ZoneTempState.NONE
        val result = getStatusMsg(portStages, analogOutStages, temperatureState)
        assert(result == "OFF")
    }


}