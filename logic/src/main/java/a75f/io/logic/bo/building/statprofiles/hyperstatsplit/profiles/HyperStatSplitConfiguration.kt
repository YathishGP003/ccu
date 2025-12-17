package a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles

import a75f.io.domain.api.Domain
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.devices.HyperStatSplitDevice
import a75f.io.domain.equips.hyperstatsplit.HyperStatSplitEquip
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.StatProfileConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

abstract class HyperStatSplitConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType, model : SeventyFiveFProfileDirective) :
    StatProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model) {

    /**
     * Get the default enable config for the domain name
     */
    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf()
    }

    override fun toString(): String {
        return "HyperStatSplitProfileConfiguration( " +
                "\naddress0Enabled=${address0Enabled.enabled}," + " address1Association=${address0SensorAssociation.temperatureAssociation}," +
                " address1Enabled=${address1Enabled.enabled}," + " address1Association=${address1SensorAssociation.temperatureAssociation}," +
                "\naddress2Enabled=${address2Enabled.enabled}, " + " address2Association=${address2SensorAssociation.temperatureAssociation}," +
                "\nuniversal1InEnabled=${universal1InEnabled.enabled}," + " universal2InEnabled=${universal2InEnabled.enabled}, " +
                "\nuniversal1InAssociation=${universal1InAssociation.associationVal}, " + "universal2InAssociation=${universal2InAssociation.associationVal}," +
                "\nuniversal3InAssociation=${universal3InAssociation.associationVal}, " + "universal4InAssociation=${universal4InAssociation.associationVal}," +
                "\nuniversal5InAssociation=${universal5InAssociation.associationVal}, " + "universal6InAssociation=${universal6InAssociation.associationVal}," +
                "\nuniversal7InAssociation=${universal7InAssociation.associationVal}, " + "universal8InAssociation=${universal8InAssociation.associationVal}," +
                "\nrelay1Enabled=${relay1Enabled.enabled}," + " relay2Enabled=${relay2Enabled.enabled}," +
                "\nrelay3Enabled=${relay3Enabled.enabled}, " + "relay4Enabled=${relay4Enabled.enabled}, " +
                "\nrelay5Enabled=${relay5Enabled.enabled}, " + "relay6Enabled=${relay6Enabled.enabled}," +
                "\nrelay7Enabled=${relay7Enabled.enabled}," + " relay8Enabled=${relay8Enabled.enabled}," +
                "\nrelay1Association=${relay1Association.associationVal}, " + "relay2Association=${relay2Association.associationVal}," +
                "\nrelay3Association=${relay3Association.associationVal}," + " relay4Association=${relay4Association.associationVal}," +
                "\nrelay5Association=${relay5Association.associationVal}," + " relay6Association=${relay6Association.associationVal}," +
                "\nrelay7Association=${relay7Association.associationVal}, " + "relay8Association=${relay8Association.associationVal}," +
                "\nanalogOut1Enabled=${analogOut1Enabled.enabled}, " + "analogOut2Enabled=${analogOut2Enabled.enabled}," +
                "\nanalogOut3Enabled=${analogOut3Enabled.enabled}, " + "analogOut4Enabled=${analogOut4Enabled.enabled}," +
                "\nanalogOut1Association=${analogOut1Association.associationVal}," + " " + "\nanalogOut2Association=${analogOut2Association.associationVal}," +
                "" + "\nanalogOut3Association=${analogOut3Association.associationVal}," + "\nanalogOut4Association=${analogOut4Association.associationVal})" +
                "\ntemperatureOffset=${temperatureOffset.currentVal}, " + "\nautoForceOccupied=${autoForceOccupied.enabled}," +
                "\nautoAway=${autoAway.enabled} " + "\noutsideDamperMinOpenDuringRecirc=${outsideDamperMinOpenDuringRecirc.currentVal}," +
                "\noutsideDamperMinOpenDuringConditioning=${outsideDamperMinOpenDuringConditioning.currentVal}," +
                "\noutsideDamperMinOpenDuringFanLow=${outsideDamperMinOpenDuringFanLow.currentVal}," +
                "\noutsideDamperMinOpenDuringFanMedium=${outsideDamperMinOpenDuringFanMedium.currentVal}," +
                "\noutsideDamperMinOpenDuringFanHigh=${outsideDamperMinOpenDuringFanHigh.currentVal}," +
                "\nexhaustFanStage1Threshold=${exhaustFanStage1Threshold.currentVal}," +
                "\nexhaustFanStage2Threshold=${exhaustFanStage2Threshold.currentVal}," +
                "\nexhaustFanHysteresis=${exhaustFanHysteresis.currentVal}," +
                "\nzoneCO2DamperOpeningRate=${zoneCO2DamperOpeningRate.currentVal}," +
                "\nzoneCO2Threshold=${zoneCO2Threshold.currentVal}," + "\nzoneCO2Target=${zoneCO2Target.currentVal}," +
                "\nzonePM2p5Target=${zonePM2p5Target.currentVal}," +
                "\ndisplayHumidity=${displayHumidity.enabled}," + "\ndisplayCO2=${displayCO2.enabled}," +
                "\ndisplayPM2p5=${displayPM2p5.enabled}),  installer pin Enable =${installerPinEnable.enabled} , conditioning mode pin enable =${conditioningModePinEnable}" +
                "\ninstallerPassword=${installerPassword.currentVal}, conditioningModePassword=${conditioningModePassword.currentVal}" +
                "\ndisableTouch=${disableTouch.enabled}, backlight =${backLight.enabled} " + "\nenableBrightness=${enableBrightness.enabled}"
    }

    fun getHighestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.third) stage3
        else if (availableStages.second) stage2
        else if (availableStages.first) stage1
        else -1
    }

    private fun availableHighestStages(
        stage1: Int,
        stage2: Int,
        stage3: Int
    ): Triple<Boolean, Boolean, Boolean> {
        var isStage1Selected = false
        var isStage2Selected = false
        var isStage3Selected = false

        getRelayEnabledAssociations().forEach { (enabled, associated) ->
            if (enabled) {
                if (associated == stage1) isStage1Selected = true
                if (associated == stage2) isStage2Selected = true
                if (associated == stage3) isStage3Selected = true
            }
        }
        return Triple(isStage1Selected, isStage2Selected, isStage3Selected)
    }

    protected fun getLowestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.first) stage1
        else if (availableStages.second) stage2
        else if (availableStages.third) stage3
        else -1
    }

    fun getRelayConfigurationMapping(): List<Triple<Boolean, Int, Port>> {
        return listOf(
            Triple(relay1Enabled.enabled, relay1Association.associationVal, Port.RELAY_ONE),
            Triple(relay2Enabled.enabled, relay2Association.associationVal, Port.RELAY_TWO),
            Triple(relay3Enabled.enabled, relay3Association.associationVal, Port.RELAY_THREE),
            Triple(relay4Enabled.enabled, relay4Association.associationVal, Port.RELAY_FOUR),
            Triple(relay5Enabled.enabled, relay5Association.associationVal, Port.RELAY_FIVE),
            Triple(relay6Enabled.enabled, relay6Association.associationVal, Port.RELAY_SIX),
            Triple(relay7Enabled.enabled, relay7Association.associationVal, Port.RELAY_SEVEN),
            Triple(relay8Enabled.enabled, relay8Association.associationVal, Port.RELAY_EIGHT),
        )
    }

    fun getAnalogOutsConfigurationMapping(): List<Triple<Boolean, Int, Port>> {
        return listOf(
            Triple(
                analogOut1Enabled.enabled,
                analogOut1Association.associationVal,
                Port.ANALOG_OUT_ONE
            ),
            Triple(
                analogOut2Enabled.enabled,
                analogOut2Association.associationVal,
                Port.ANALOG_OUT_TWO
            ),
            Triple(
                analogOut3Enabled.enabled,
                analogOut3Association.associationVal,
                Port.ANALOG_OUT_THREE
            ),
            Triple(
                analogOut4Enabled.enabled,
                analogOut4Association.associationVal,
                Port.ANALOG_OUT_FOUR
            )
        )
    }

    abstract fun getHighestFanStageCount(): Int

    private fun getRelayEnabledAssociations(): List<Pair<Boolean, Int>> = buildList {
        listOf(
            relay1Enabled to relay1Association,
            relay2Enabled to relay2Association,
            relay3Enabled to relay3Association,
            relay4Enabled to relay4Association,
            relay5Enabled to relay5Association,
            relay6Enabled to relay6Association,
            relay7Enabled to relay7Association,
            relay8Enabled to relay8Association
        ).forEach { (enabled, association) ->
            if (enabled.enabled) add(true to association.associationVal)
        }
    }

    private fun getAnalogEnabledAssociations(): List<Pair<Boolean, Int>> = buildList {
        listOf(
            analogOut1Enabled to analogOut1Association,
            analogOut2Enabled to analogOut2Association,
            analogOut3Enabled to analogOut3Association,
            analogOut4Enabled to analogOut4Association
        ).forEach { (enable, association) ->
            if (enable.enabled) add(true to association.associationVal)
        }
    }

    fun isAnySensorBusMapped(
        config: HyperStatSplitConfiguration,
        type: EconSensorBusTempAssociation
    ): Boolean {
        return (config.address0Enabled.enabled && config.address0SensorAssociation.temperatureAssociation.associationVal == type.ordinal) ||
                (config.address1Enabled.enabled && config.address1SensorAssociation.temperatureAssociation.associationVal == type.ordinal) ||
                (config.address2Enabled.enabled && config.address2SensorAssociation.temperatureAssociation.associationVal == type.ordinal)
    }

    /**
     * Function to get the point value if config exist else return the current value model default value
     */
    fun getDefault(point: Point, equip: HyperStatSplitEquip, valueConfig: ValueConfig): Double {
        return if (Domain.readPointForEquip(point.domainName, equip.equipRef).isEmpty())
            valueConfig.currentVal
        else
            point.readDefaultVal()
    }

    fun isFanEnabled(
        config: HyperStatSplitConfiguration,
        relayControl: String = HyperStatSplitControlType.FAN_ENABLED.name
    ) = isAnyRelayEnabledAndMapped(config, relayControl)


    fun isAnyAnalogEnabledAndMapped(
        config: HyperStatSplitConfiguration,
        analogType: String
    ): Boolean {
        return when (config) {
            is Pipe4UVConfiguration -> {
                val target = Pipe4UvAnalogOutControls.valueOf(analogType)
                config.getAnalogEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            is HyperStatSplitCpuConfiguration -> {
                val target = CpuAnalogControlType.valueOf(analogType)
                config.getAnalogEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            is Pipe2UVConfiguration -> {
                val target = Pipe2UvAnalogOutControls.valueOf(analogType)
                config.getAnalogEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            else -> false
        }
    }

    fun isAnyRelayEnabledAndMapped(
        config: HyperStatSplitConfiguration,
        relayType: String
    ): Boolean {
        return when (config) {
            is Pipe4UVConfiguration -> {
                val target = Pipe4UVRelayControls.valueOf(relayType)
                config.getRelayEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            is HyperStatSplitCpuConfiguration -> {
                val target = CpuRelayType.valueOf(relayType)
                config.getRelayEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            is Pipe2UVConfiguration -> {
                val target = Pipe4UVRelayControls.valueOf(relayType)
                config.getRelayEnabledAssociations().any { (enabled, type) ->
                    enabled && type == target.ordinal
                }
            }

            else -> false
        }
    }

    // getting the profile based enum value
    fun getProfileBasedEnumValueAnalogType(
        enumName: String,
        profileConfiguration: HyperStatSplitConfiguration
    ): Int {
        return when (profileConfiguration) {
            is HyperStatSplitCpuConfiguration -> CpuAnalogControlType.valueOf(enumName).ordinal
            is Pipe4UVConfiguration -> Pipe4UvAnalogOutControls.valueOf(enumName).ordinal
            is Pipe2UVConfiguration -> Pipe2UvAnalogOutControls.valueOf(enumName).ordinal
            else -> CpuAnalogControlType.valueOf(enumName).ordinal
        }
    }

    // getting the profile based enum value
    fun getProfileBasedEnumValueRelayType(
        enumName: String,
        profileConfiguration: HyperStatSplitConfiguration
    ): Int {
        return when (profileConfiguration) {
            is HyperStatSplitCpuConfiguration -> CpuRelayType.valueOf(enumName).ordinal
            is Pipe4UVConfiguration -> Pipe4UVRelayControls.valueOf(enumName).ordinal
            is Pipe2UVConfiguration -> Pipe2UVRelayControls.valueOf(enumName).ordinal
            else -> {
                HyperStatSplitControlType.valueOf(enumName).ordinal
            }
        }
    }

    fun updateConditioningMode(equipId: String) {
        val equip = HyperStatSplitEquip(equipId)
        val isCoolingAvailable = isCoolingAvailable()
        val isHeatingAvailable = isHeatingAvailable()
        equip.apply {
            if (!isCoolingAvailable && !isHeatingAvailable) {
                conditioningMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
            } else if (!isCoolingAvailable) {
                conditioningMode.writePointValue(StandaloneConditioningMode.HEAT_ONLY.ordinal.toDouble())
            } else if (!isHeatingAvailable) {
                conditioningMode.writePointValue(StandaloneConditioningMode.COOL_ONLY.ordinal.toDouble())
            }
        }
    }

    fun getRelayLogicalPhysicalMap(equipId: String): Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        val equip = Domain.equips[equipId] as HyperStatSplitEquip
        val device = Domain.devices[equipId] as HyperStatSplitDevice
        map[equip.relay1OutputEnable] = device.relay1
        map[equip.relay2OutputEnable] = device.relay2
        map[equip.relay3OutputEnable] = device.relay3
        map[equip.relay4OutputEnable] = device.relay4
        map[equip.relay5OutputEnable] = device.relay5
        map[equip.relay6OutputEnable] = device.relay6
        map[equip.relay7OutputEnable] = device.relay7
        map[equip.relay8OutputEnable] = device.relay8
        return map
    }

    fun getAnalogOutLogicalPhysicalMap(equipId: String): Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        val equip = Domain.equips[equipId] as HyperStatSplitEquip
        val device = Domain.devices[equipId] as HyperStatSplitDevice
        map[equip.analog1OutputEnable] = device.analog1Out
        map[equip.analog2OutputEnable] = device.analog2Out
        map[equip.analog3OutputEnable] = device.analog3Out
        map[equip.analog4OutputEnable] = device.analog4Out
        return map
    }
}

    /// this enum is used only  for string reference
    enum class HyperStatSplitControlType {
        HEATING_WATER_VALVE,COOLING_WATER_VALVE,FACE_DAMPER_VALVE ,OAO_DAMPER, DCV_MODULATING_DAMPER, EXTERNALLY_MAPPED, COOLING,
        LINEAR_FAN, HEATING, STAGED_FAN, RETURN_DAMPER, COMPRESSOR_SPEED,FAN_ENABLED , FAN_SPEED
        ,HEATING_WATER_MODULATING_VALVE ,COOLING_WATER_MODULATING_VALVE ,FAN_LOW_SPEED_VENTILATION  ,FAN_LOW_SPEED,WATER_VALVE ,WATER_MODULATING_VALVE

    }


data class SensorTempHumidityAssociationConfig(
    var temperatureAssociation: AssociationConfig,
    var humidityAssociation: AssociationConfig
)

enum class UniversalInputs {
    NONE, VOLTAGE_INPUT, THERMISTOR_INPUT, BUILDING_STATIC_PRESSURE1, BUILDING_STATIC_PRESSURE2, BUILDING_STATIC_PRESSURE10, INDEX_6, INDEX_7, INDEX_8, INDEX_9,
    INDEX_10, INDEX_11, INDEX_12, INDEX_13, SUPPLY_AIR_TEMPERATURE, INDEX_15, DUCT_STATIC_PRESSURE1_1, DUCT_STATIC_PRESSURE1_2, DUCT_STATIC_PRESSURE1_10, INDEX_19, INDEX_20,
    INDEX_21, INDEX_22, INDEX_23, INDEX_24, INDEX_25, INDEX_26, INDEX_27, MIXED_AIR_TEMPERATURE, OUTSIDE_AIR_DAMPER_FEEDBACK, INDEX_30,
    INDEX_31, INDEX_32, OUTSIDE_AIR_TEMPERATURE, INDEX_34, INDEX_35, INDEX_36, INDEX_37, INDEX_38, INDEX_39, INDEX_40,
    CURRENT_TX_10, CURRENT_TX_20, CURRENT_TX_30, CURRENT_TX_50, CURRENT_TX_60, CURRENT_TX_100, CURRENT_TX_120, CURRENT_TX_150, CURRENT_TX_200, INDEX_50,
    INDEX_51, INDEX_52, DISCHARGE_FAN_AM_STATUS, DISCHARGE_FAN_RUN_STATUS, DISCHARGE_FAN_TRIP_STATUS, INDEX_56, INDEX_57, EXHAUST_FAN_RUN_STATUS, EXHAUST_FAN_TRIP_STATUS, FILTER_STATUS_NO,
    FILTER_STATUS_NC, INDEX_62, INDEX_63, FIRE_ALARM_STATUS, INDEX_65, INDEX_66, INDEX_67, INDEX_68, INDEX_69, INDEX_70,
    INDEX_71, INDEX_72, HIGH_DIFFERENTIAL_PRESSURE_SWITCH, LOW_DIFFERENTIAL_PRESSURE_SWITCH, INDEX_75, INDEX_76, INDEX_77, INDEX_78, INDEX_79, INDEX_80,
    INDEX_81, INDEX_82, CONDENSATE_STATUS_NO, CONDENSATE_STATUS_NC, INDEX_85, INDEX_86, INDEX_87, INDEX_88, INDEX_89, INDEX_90,
    EMERGENCY_SHUTOFF_NO, EMERGENCY_SHUTOFF_NC, GENERIC_ALARM_NO, GENERIC_ALARM_NC, DOOR_WINDOW_SENSOR_NC, DOOR_WINDOW_SENSOR, DOOR_WINDOW_SENSOR_TITLE24_NC, DOOR_WINDOW_SENSOR_TITLE24, RUN_FAN_STATUS_NO, RUN_FAN_STATUS_NC,
    FIRE_ALARM_STATUS_NC, DOOR_WINDOW_SENSOR_NO, DOOR_WINDOW_SENSOR_NO_TITLE_24, KEYCARD_SENSOR_NO, KEYCARD_SENSOR_NC, CHILLED_WATER_SUPPLY_TEMPERATURE, HOT_WATER_SUPPLY_TEMPERATURE,SUPPLY_WATER_TEMPERATURE

}

enum class CpuSensorBusType {
    SUPPLY_AIR, MIXED_AIR, OUTSIDE_AIR
}

enum class EconSensorBusTempAssociation {
   SUPPLY_AIR_TEMPERATURE_HUMIDITY,
   MIXED_AIR_TEMPERATURE_HUMIDITY,
   OUTSIDE_AIR_TEMPERATURE_HUMIDITY
}