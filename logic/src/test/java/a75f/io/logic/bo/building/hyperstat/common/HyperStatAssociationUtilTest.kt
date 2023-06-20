package a75f.io.logic.bo.building.hyperstat.common

import a75f.io.logic.bo.building.hyperstat.profiles.cpu.*
import org.junit.Assert
import org.junit.Test

/**
 * Created by Manjunath K on 02-08-2022.
 */

class HyperStatAssociationUtilTest{

    @Test
    fun isRelayAssociatedToFanTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToFan(RelayState(true,CpuRelayAssociation.FAN_LOW_SPEED)))
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToFan(RelayState(true,CpuRelayAssociation.FAN_MEDIUM_SPEED)))
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToFan(RelayState(true,CpuRelayAssociation.FAN_HIGH_SPEED)))

        Assert.assertEquals(false,HyperStatAssociationUtil.isRelayAssociatedToFan(RelayState(true,CpuRelayAssociation.COOLING_STAGE_1)))
    }

    @Test
    fun isRelayAssociatedToCoolingStageTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(RelayState(true,CpuRelayAssociation.COOLING_STAGE_1)))
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(RelayState(true,CpuRelayAssociation.COOLING_STAGE_2)))
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(RelayState(true,CpuRelayAssociation.COOLING_STAGE_3)))

        Assert.assertEquals(false,HyperStatAssociationUtil.isRelayAssociatedToCoolingStage(RelayState(true,CpuRelayAssociation.FAN_HIGH_SPEED)))

    }

    @Test
    fun isRelayAssociatedToHeatingStageTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(RelayState(true,CpuRelayAssociation.HEATING_STAGE_1)))
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(RelayState(true,CpuRelayAssociation.HEATING_STAGE_2)))
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(RelayState(true,CpuRelayAssociation.HEATING_STAGE_3)))

        Assert.assertEquals(false,HyperStatAssociationUtil.isRelayAssociatedToHeatingStage(RelayState(true,CpuRelayAssociation.COOLING_STAGE_3)))
    }
    @Test
    fun getRelayAssociatedStageTest(){
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(0),CpuRelayAssociation.COOLING_STAGE_1)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(1),CpuRelayAssociation.COOLING_STAGE_2)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(2),CpuRelayAssociation.COOLING_STAGE_3)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(3),CpuRelayAssociation.HEATING_STAGE_1)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(4),CpuRelayAssociation.HEATING_STAGE_2)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(5),CpuRelayAssociation.HEATING_STAGE_3)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(6),CpuRelayAssociation.FAN_LOW_SPEED)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(7),CpuRelayAssociation.FAN_MEDIUM_SPEED)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(8),CpuRelayAssociation.FAN_HIGH_SPEED)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(9),CpuRelayAssociation.FAN_ENABLED)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(10),CpuRelayAssociation.OCCUPIED_ENABLED)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(11),CpuRelayAssociation.HUMIDIFIER)
        Assert.assertEquals(HyperStatAssociationUtil.getRelayAssociatedStage(12),CpuRelayAssociation.DEHUMIDIFIER)
    }
    @Test
    fun getAnalogOutAssociatedStageTest(){
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogOutAssociatedStage(0), CpuAnalogOutAssociation.COOLING)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogOutAssociatedStage(1), CpuAnalogOutAssociation.MODULATING_FAN_SPEED)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogOutAssociatedStage(2), CpuAnalogOutAssociation.HEATING)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogOutAssociatedStage(3), CpuAnalogOutAssociation.DCV_DAMPER)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogOutAssociatedStage(4), CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED)
    }
    @Test
    fun getAnalogInStageTest(){
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogInStage(0), AnalogInAssociation.CURRENT_TX_0_10)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogInStage(1), AnalogInAssociation.CURRENT_TX_0_20)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogInStage(2), AnalogInAssociation.CURRENT_TX_0_50)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogInStage(3), AnalogInAssociation.KEY_CARD_SENSOR)
        Assert.assertEquals(HyperStatAssociationUtil.getAnalogInStage(4), AnalogInAssociation.DOOR_WINDOW_SENSOR)
    }

    @Test
    fun isRelayAssociatedToFanEnabledTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToFanEnabled(RelayState(true,CpuRelayAssociation.FAN_ENABLED)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isRelayAssociatedToFanEnabled(RelayState(true,CpuRelayAssociation.OCCUPIED_ENABLED)))
    }

    @Test
    fun isRelayAssociatedToOccupiedEnabledTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToOccupiedEnabled(RelayState(true,CpuRelayAssociation.OCCUPIED_ENABLED)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isRelayAssociatedToOccupiedEnabled(RelayState(true,CpuRelayAssociation.FAN_ENABLED)))
    }

    @Test
    fun isRelayAssociatedToHumidifierTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToHumidifier(RelayState(true,CpuRelayAssociation.HUMIDIFIER)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isRelayAssociatedToHumidifier(RelayState(true,CpuRelayAssociation.FAN_ENABLED)))
    }

    @Test
    fun isRelayAssociatedToDeHumidifierTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isRelayAssociatedToDeHumidifier(RelayState(true,CpuRelayAssociation.DEHUMIDIFIER)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isRelayAssociatedToDeHumidifier(RelayState(true,CpuRelayAssociation.FAN_ENABLED)))
    }

    @Test
    fun isAnalogOutAssociatedToCoolingTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogOutAssociatedToCooling(AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,0.0,0.0,0.0,0.0)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogOutAssociatedToCooling(AnalogOutState(true,CpuAnalogOutAssociation.HEATING,0.0,0.0,0.0,0.0,0.0)))
    }

    @Test
    fun isAnalogOutAssociatedToHeatingTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogOutAssociatedToHeating(AnalogOutState(true,CpuAnalogOutAssociation.HEATING,0.0,0.0,0.0,0.0,0.0)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogOutAssociatedToHeating(AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,0.0,0.0,0.0,0.0)))
    }

    @Test
    fun isAnalogOutAssociatedToFanSpeedTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,0.0,0.0,0.0,0.0)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,0.0,0.0,0.0,0.0)))
    }

    @Test
    fun isAnalogOutAssociatedToDcvDamperTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogOutAssociatedToDcvDamper(AnalogOutState(true,CpuAnalogOutAssociation.DCV_DAMPER,0.0,0.0,0.0,0.0,0.0)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogOutAssociatedToDcvDamper(AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,0.0,0.0,0.0,0.0)))
    }

    @Test
    fun isAnalogInAssociatedToCurrentTX10Test(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX10(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_10)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX10(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_20)))
    }

    @Test
    fun isAnalogInAssociatedToCurrentTX20Test(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX20(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_20)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX20(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_10)))
    }

    @Test
    fun isAnalogInAssociatedToCurrentTX50Test(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX50(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_50)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogInAssociatedToCurrentTX50(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_10)))
    }

    @Test
    fun isAnalogInAssociatedToDoorWindowSensorTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(AnalogInState(true,AnalogInAssociation.DOOR_WINDOW_SENSOR)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogInAssociatedToDoorWindowSensor(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_10)))
    }
    @Test
    fun isAnalogInAssociatedToKeyCardSensorTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(AnalogInState(true,AnalogInAssociation.KEY_CARD_SENSOR)))
        Assert.assertEquals(false,HyperStatAssociationUtil.isAnalogInAssociatedToKeyCardSensor(AnalogInState(true,AnalogInAssociation.CURRENT_TX_0_10)))
    }

    @Test
    fun isBothRelayHasSameConfigsTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isBothRelayHasSameConfigs(
            RelayState(true,CpuRelayAssociation.COOLING_STAGE_1),
            RelayState(true,CpuRelayAssociation.COOLING_STAGE_1),
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothRelayHasSameConfigs(
            RelayState(false,CpuRelayAssociation.COOLING_STAGE_1),
            RelayState(true,CpuRelayAssociation.COOLING_STAGE_1),
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothRelayHasSameConfigs(
            RelayState(false,CpuRelayAssociation.COOLING_STAGE_1),
            RelayState(true,CpuRelayAssociation.COOLING_STAGE_2),
        ))
    }

    @Test
    fun isBothAnalogOutHasSameConfigsTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(false,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.HEATING,0.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,1.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,11.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,11.0,11.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,11.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,11.0,11.0,51.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,11.0,11.0,50.0,100.0)
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogOutHasSameConfigs(
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,11.0,11.0,51.0,99.0),
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,11.0,11.0,51.0,100.0)
        ))
    }

    @Test
    fun findChangeInAnalogOutConfigTest(){
        Assert.assertEquals(AnalogOutChanges.NOCHANGE,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0)
        ))
        Assert.assertEquals(AnalogOutChanges.ENABLED,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(false,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0)
        ))
        Assert.assertEquals(AnalogOutChanges.MAPPING,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.COOLING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.HEATING,0.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(AnalogOutChanges.MIN,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.HEATING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.HEATING,1.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(AnalogOutChanges.MAX,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.HEATING,0.0,10.0,10.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.HEATING,0.0,9.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(AnalogOutChanges.LOW,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,10.0,11.0,50.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,10.0,10.0,50.0,100.0)
        ))

        Assert.assertEquals(AnalogOutChanges.MED,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,10.0,10.0,51.0,100.0),
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,10.0,10.0,50.0,100.0)
        ))
        Assert.assertEquals(AnalogOutChanges.HIGH,HyperStatAssociationUtil.findChangeInAnalogOutConfig(
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,10.0,10.0,50.0,99.0),
            AnalogOutState(true,CpuAnalogOutAssociation.MODULATING_FAN_SPEED,0.0,10.0,10.0,50.0,100.0)
        ))
    }


    @Test
    fun isBothAnalogInHasSameConfigsTest(){
        Assert.assertEquals(true,HyperStatAssociationUtil.isBothAnalogInHasSameConfigs(
            AnalogInState(true,AnalogInAssociation.KEY_CARD_SENSOR),
            AnalogInState(true,AnalogInAssociation.KEY_CARD_SENSOR),
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogInHasSameConfigs(
            AnalogInState(true,AnalogInAssociation.KEY_CARD_SENSOR),
            AnalogInState(false,AnalogInAssociation.KEY_CARD_SENSOR),
        ))

        Assert.assertEquals(false,HyperStatAssociationUtil.isBothAnalogInHasSameConfigs(
            AnalogInState(true,AnalogInAssociation.KEY_CARD_SENSOR),
            AnalogInState(false,AnalogInAssociation.CURRENT_TX_0_20),
        ))
    }


    @Test
    fun isAnyRelayAssociatedToHumidifierTest(){
        val configuration = HyperStatCpuConfiguration()
        Assert.assertEquals( false, HyperStatAssociationUtil.isAnyRelayAssociatedToHumidifier(configuration))

        configuration.relay1State.association = CpuRelayAssociation.HUMIDIFIER
        Assert.assertEquals( true, HyperStatAssociationUtil.isAnyRelayAssociatedToHumidifier(configuration))

    }

    @Test
    fun isAnyRelayAssociatedToDeHumidifierTest(){
        val configuration = HyperStatCpuConfiguration()
        Assert.assertEquals( false, HyperStatAssociationUtil.isAnyRelayAssociatedToDeHumidifier(configuration))

        configuration.relay1State.association = CpuRelayAssociation.DEHUMIDIFIER
        Assert.assertEquals( true, HyperStatAssociationUtil.isAnyRelayAssociatedToDeHumidifier(configuration))

    }

    @Test
    fun isAnyRelayEnabledAssociatedToFanTest(){
        val configuration = HyperStatCpuConfiguration()
        Assert.assertEquals( false, HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToFan(configuration))

        configuration.relay1State =  RelayState(false, CpuRelayAssociation.FAN_LOW_SPEED)
        Assert.assertEquals( false, HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToFan(configuration))

        configuration.relay1State =  RelayState(true, CpuRelayAssociation.FAN_LOW_SPEED)
        Assert.assertEquals( true, HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToFan(configuration))

        configuration.relay1State =  RelayState(true, CpuRelayAssociation.FAN_MEDIUM_SPEED)
        Assert.assertEquals( true, HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToFan(configuration))

        configuration.relay1State =  RelayState(true, CpuRelayAssociation.FAN_HIGH_SPEED)
        Assert.assertEquals( true, HyperStatAssociationUtil.isAnyRelayEnabledAssociatedToFan(configuration))


    }

}