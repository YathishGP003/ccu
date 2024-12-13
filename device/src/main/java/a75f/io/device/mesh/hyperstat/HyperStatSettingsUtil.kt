package a75f.io.device.mesh.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.device.HyperStat
import a75f.io.device.HyperStat.HyperStatSettingsMessage2_t
import a75f.io.device.HyperStat.HyperStatSettingsMessage3_t
import a75f.io.device.HyperStat.HyperStatSettingsMessage_t
import a75f.io.device.util.DeviceConfigurationUtil.Companion.getUserConfiguration
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.readValAtLevelByDomain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.tuners.TunerUtil

/**
 * Created by Manjunath K on 14-12-2021.
 */
class HyperStatSettingsUtil {

    companion object {

        /**
         * Function which construct the setting2 message details
         * @param equipRef
         * @param hsApi
         * @param nodeAddress
         * return HyperStatSettingsMessage2_t
         */
        fun getSetting2Message(nodeAddress: Int, equipRef: String, hsApi: CCUHsApi): HyperStatSettingsMessage2_t {
            val settings2 = HyperStatSettingsMessage2_t.newBuilder()
            val equip = getEquipDetails(nodeAddress)

            // These all common configuration for all the profiles
            settings2.enableForceOccupied = isAutoForceEnabled(hsApi, equipRef)
            settings2.enableAutoAway = isAutoAwayEnabled(hsApi, equipRef)
            settings2.hyperstatRelayConfig = getRelayConfigDetails(hsApi, equipRef)
            settings2.hyperstatAnalogOutConfig = getAnalogOutConfigDetails(hsApi, equipRef)
            settings2.hyperstatAnalogInConfig = getAnalogInConfigDetails(hsApi, equipRef)
            settings2.thermistor1Enable = getTh1Enabled(hsApi, equipRef)
            settings2.zoneCO2Target = readConfig(hsApi, equipRef, "co2 and target and config").toInt()
            settings2.zoneCO2Threshold = readConfig(hsApi, equipRef, "co2 and threshold and config").toInt()
            settings2.zoneCO2DamperOpeningRate = readConfig(hsApi, equipRef, "co2 and damper and config").toInt()
            settings2.proportionalConstant = (TunerUtil.getProportionalGain(equipRef) * 100).toInt()
            settings2.integralConstant = (TunerUtil.getIntegralGain(equipRef) * 100).toInt()
            settings2.proportionalTemperatureRange = (TunerUtil.getProportionalSpread(equipRef) * 10).toInt()
            settings2.integrationTime = TunerUtil.getIntegralTimeout(equipRef).toInt()

            when (equip.profile) {
                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name -> {
                    settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_CONVENTIONAL_PACKAGE_UNIT
                    settings2.thermistor2Enable = getTh2DoorWindow(hsApi, equipRef)
                }
                ProfileType.HYPERSTAT_HEAT_PUMP_UNIT.name -> {
                    settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_HEAT_PUMP_UNIT
                    settings2.thermistor2Enable = getTh2DoorWindow(hsApi, equipRef)
                }
                ProfileType.HYPERSTAT_TWO_PIPE_FCU.name -> {
                    settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_2_PIPE_FANCOIL_UNIT
                    settings2.thermistor2Enable = getTh2SupplyWaterTempEnabled(hsApi, equipRef)
                }

                ProfileType.HYPERSTAT_MONITORING.name -> {
                    settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_SENSE
                }
                ProfileType.HYPERSTAT_VRV.name -> {
                    settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_VRV
                }
            }

            return settings2.build()
        }

        /**
         * Function which construct the setting3 message details
         * @param equipRef
         * @param nodeAddress
         * return HyperStatSettingsMessage3_t
         */
        fun getSetting3Message(nodeAddress: Int, equipRef: String): HyperStatSettingsMessage3_t {
            val settings3 = HyperStatSettingsMessage3_t.newBuilder()
            val equip = getEquipDetails(nodeAddress)
            when (equip.profile) {
                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name -> {
                    settings3.genertiTuners = getGenericTunerDetails(equipRef)
                    settings3.hyperStatConfigsCpu = getStagedFanVoltageDetails(equipRef)
                }
                ProfileType.HYPERSTAT_HEAT_PUMP_UNIT.name -> {
                    settings3.genertiTuners = getGenericTunerDetails(equipRef)
                    settings3.fcuTuners = getHpuTunerDetails(equipRef)
                }
                ProfileType.HYPERSTAT_TWO_PIPE_FCU.name -> {
                    settings3.genertiTuners = getGenericTunerDetails(equipRef)
                    settings3.fcuTuners = getFcuTunerDetails(equipRef)
                }
                ProfileType.HYPERSTAT_MONITORING.name -> {
                    /** Do nothing */
                }
                ProfileType.HYPERSTAT_VRV.name -> {
                    /** Do nothing */
                }
            }
            return settings3.build()
        }

        /**
         * Function which collects the equip details from node address
         * @param nodeAddress
         * return Equip
         */
        private fun getEquipDetails(nodeAddress: Int): Equip {
            return Equip.Builder()
                .setHashMap(CCUHsApi.getInstance().read("equip and group == \"$nodeAddress\"")).build()
        }

        /**
         * Function to check the is AutoForce configuration enabled or not
         * @param equipRef
         * @param hsApi
         * return Boolean
         */
        private fun isAutoForceEnabled(hsApi: CCUHsApi, equipRef: String): Boolean {
            return (readConfig(hsApi, equipRef, "auto and forced and control and enabled") == 1.0)
        }

        /**
         * Function to check the is AutoAway configuration enabled or not
         * @param equipRef
         * @param hsApi
         * @return Boolean
         */
        private fun isAutoAwayEnabled(hsApi: CCUHsApi, equipRef: String): Boolean {
            return (readConfig(hsApi, equipRef, "auto and away and control and enabled") == 1.0)
        }

        /**
         * Function to check the is Thermistor 1 toggle configuration enabled or not
         * @param equipRef
         * @param hsApi
         * @return Boolean
         */
        private fun getTh1Enabled(hsApi: CCUHsApi, equipRef: String): Boolean {
            return (readConfig(hsApi, equipRef, "th1 and config and enabled") == 1.0)
        }

        /**
         * Function to check the is Thermistor 2 toggle configuration enabled or not
         * @param equipRef
         * @param hsApi
         * @return Boolean
         */
        private fun getTh2DoorWindow(hsApi: CCUHsApi, equipRef: String): Boolean {
            return (readConfig(hsApi, equipRef, "th2 and config and enabled") == 1.0)
        }

        /**
         * Function to check the is Thermistor 2 toggle configuration enabled or not
         * @param equipRef
         * @param hsApi
         * @return Boolean
         */
        private fun getTh2SupplyWaterTempEnabled(hsApi: CCUHsApi, equipRef: String): Boolean {
            return (readConfig(hsApi, equipRef, "supply and water and temp and config and enabled") == 1.0)
        }


        /**
         * Function which reads all the Relay toggle configuration and mapping details
         * @param equipRef
         * @param hsApi
         * @return HyperstatRelay_t
         */
        private fun getRelayConfigDetails(hsApi: CCUHsApi, equipRef: String): HyperStat.HyperstatRelay_t {
            val relayConfiguration = HyperStat.HyperstatRelay_t.newBuilder()
            relayConfiguration.relay1Enable = readConfig(hsApi, equipRef, "relay1 and config and enabled") == 1.0
            relayConfiguration.relay2Enable = readConfig(hsApi, equipRef, "relay2 and config and enabled") == 1.0
            relayConfiguration.relay3Enable = readConfig(hsApi, equipRef, "relay3 and config and enabled") == 1.0
            relayConfiguration.relay4Enable = readConfig(hsApi, equipRef, "relay4 and config and enabled") == 1.0
            relayConfiguration.relay5Enable = readConfig(hsApi, equipRef, "relay5 and config and enabled") == 1.0
            relayConfiguration.relay6Enable = readConfig(hsApi, equipRef, "relay6 and config and enabled") == 1.0

            /**
             * Firmware mapping enum has "none" at 0 position but ccu will will not use none.
             * So we are adding 1 to avoid the 0 position all the time
             */

            if (relayConfiguration.relay1Enable)
                relayConfiguration.relay1Mapping = 1 + readConfig(hsApi, equipRef, "relay1 and config and association ")
                    .toInt()

            if (relayConfiguration.relay2Enable)
                relayConfiguration.relay2Mapping = 1 + readConfig(hsApi, equipRef, "relay2 and config and association ")
                    .toInt()

            if (relayConfiguration.relay3Enable)
                relayConfiguration.relay3Mapping = 1 + readConfig(hsApi, equipRef, "relay3 and config and association ")
                    .toInt()

            if (relayConfiguration.relay4Enable)
                relayConfiguration.relay4Mapping = 1 + readConfig(hsApi, equipRef, "relay4 and config and association ")
                    .toInt()

            if (relayConfiguration.relay5Enable)
                relayConfiguration.relay5Mapping = 1 + readConfig(hsApi, equipRef, "relay5 and config and association ")
                    .toInt()

            if (relayConfiguration.relay6Enable)
                relayConfiguration.relay6Mapping = 1 + readConfig(hsApi, equipRef, "relay6 and config and association ")
                    .toInt()

            return relayConfiguration.build()
        }

        /**
         * Function which reads all the Analog Out toggle configuration and mapping details
         * @param equipRef
         * @param hsApi
         * @return HyperstatAnalogOut_t
         */
        private fun getAnalogOutConfigDetails(hsApi: CCUHsApi, equipRef: String): HyperStat.HyperstatAnalogOut_t {
            val analogOutConfiguration = HyperStat.HyperstatAnalogOut_t.newBuilder()
            val defaultAnalogOutMinSetting = 0
            val defaultAnalogOutMaxSetting = 10

            analogOutConfiguration.analogOut1Enable = readConfig(
                hsApi, equipRef, "analog1 and output and config and enabled"
            ) == 1.0

            analogOutConfiguration.analogOut2Enable = readConfig(
                hsApi, equipRef, "analog2 and output and config and enabled"
            ) == 1.0

            analogOutConfiguration.analogOut3Enable = readConfig(
                hsApi, equipRef, "analog3 and output and config and enabled"
            ) == 1.0

            if (analogOutConfiguration.analogOut1Enable) {
                analogOutConfiguration.analogOut1Mapping = readConfig(
                    hsApi, equipRef, "analog1 and output and config and association "
                ).toInt()

                if (analogOutConfiguration.analogOut1Mapping != HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                    analogOutConfiguration.analogOut1AtMinSetting = (readConfig(
                        hsApi, equipRef, "analog1 and output and config and min"
                    ) * 10).toInt()
                    analogOutConfiguration.analogOut1AtMaxSetting = (readConfig(
                        hsApi, equipRef, "analog1 and output and config and max "
                    ) * 10).toInt()
                } else {
                    analogOutConfiguration.analogOut1AtMinSetting = defaultAnalogOutMinSetting
                    analogOutConfiguration.analogOut1AtMaxSetting = defaultAnalogOutMaxSetting * 10
                }
            }

            if (analogOutConfiguration.analogOut2Enable) {
                analogOutConfiguration.analogOut2Mapping = readConfig(
                    hsApi, equipRef, "analog2 and output and config and association "
                ).toInt()

                if (analogOutConfiguration.analogOut2Mapping != HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                    analogOutConfiguration.analogOut2AtMinSetting = (readConfig(
                        hsApi, equipRef, "analog2 and output and config and min"
                    ) * 10).toInt()
                    analogOutConfiguration.analogOut2AtMaxSetting = (readConfig(
                        hsApi, equipRef, "analog2 and output and config and max "
                    ) * 10).toInt()
                } else {
                    analogOutConfiguration.analogOut2AtMinSetting = defaultAnalogOutMinSetting
                    analogOutConfiguration.analogOut2AtMaxSetting = defaultAnalogOutMaxSetting * 10
                }
            }

            if (analogOutConfiguration.analogOut3Enable) {
                analogOutConfiguration.analogOut3Mapping = readConfig(
                    hsApi, equipRef, "analog3 and output and config and association "
                ).toInt()

                if (analogOutConfiguration.analogOut3Mapping != HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                    analogOutConfiguration.analogOut3AtMinSetting = (readConfig(
                        hsApi, equipRef, "analog3 and output and config and min"
                    ) * 10).toInt()
                    analogOutConfiguration.analogOut3AtMaxSetting = (readConfig(
                        hsApi, equipRef, "analog3 and output and config and max "
                    ) * 10).toInt()
                } else {
                    analogOutConfiguration.analogOut3AtMinSetting = defaultAnalogOutMinSetting
                    analogOutConfiguration.analogOut3AtMaxSetting = defaultAnalogOutMaxSetting * 10
                }
            }

            return analogOutConfiguration.build()
        }

        /**
         * Function which reads all the Analog In toggle configuration and mapping details
         * @param equipRef
         * @param hsApi
         * @return HyperstatAnalogIn_t
         */
        private fun getAnalogInConfigDetails(hsApi: CCUHsApi, equipRef: String): HyperStat.HyperstatAnalogIn_t {
            val analogIn = HyperStat.HyperstatAnalogIn_t.newBuilder()

            analogIn.analogIn1Enable = readConfig(
                hsApi, equipRef, "analog1 and input and config and enabled"
            ) == 1.0

            analogIn.analogIn2Enable = readConfig(
                hsApi, equipRef, "analog2 and input and config and enabled"
            ) == 1.0

            if (analogIn.analogIn1Enable) {
                val mapping = readConfig(hsApi, equipRef, "analog1 and input and config and association ").toInt()
                analogIn.analogIn1Mapping = HyperStat.HyperstatAnalogInMapping_t.values()[mapping]
            }
            if (analogIn.analogIn2Enable) {
                val mapping = readConfig(hsApi, equipRef, "analog2 and input and config and association ").toInt()
                analogIn.analogIn2Mapping = HyperStat.HyperstatAnalogInMapping_t.values()[mapping]
            }

            return analogIn.build()
        }

        /**
         * Function to read all the generic tuners which are required for Hyperstat to run on standalone mode
         * @param equipRef
         * @return HyperStatTunersGeneric_t
         */
        private fun getGenericTunerDetails(equipRef: String): HyperStat.HyperStatTunersGeneric_t {
            val genericTuners = HyperStat.HyperStatTunersGeneric_t.newBuilder()
            val equip = CCUHsApi.getInstance().readHDictById(equipRef)
            genericTuners.unoccupiedSetback = (CCUHsApi.getInstance().readPointPriorityValByQuery
                ("schedulable and zone and unoccupied and setback and roomRef == \"" + equip.get("roomRef").toString() + "\"") * 10).toInt()
            genericTuners.minFanRuntimePostconditioning =
                    TunerUtil.readTunerValByQuery("min and fan and runtime and postconditioning", equipRef).toInt()
            genericTuners.relayActivationHysteresis =
                TunerUtil.getHysteresisPoint("relay and activation", equipRef).toInt()
            genericTuners.analogFanSpeedMultiplier =
                (TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", equipRef) * 10 ).toInt()
            genericTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equipRef).toInt()
            genericTuners.autoAwayZoneSetbackTemp =
                (TunerUtil.readTunerValByQuery("auto and away and setback") * 10).toInt()
            genericTuners.autoAwayTime = TunerUtil.readTunerValByQuery("auto and away and time", equipRef).toInt()
            genericTuners.forcedOccupiedTime =
                TunerUtil.readTunerValByQuery("forced and occupied and time", equipRef).toInt()
            return genericTuners.build()
        }

        /**
         * Function which reads a default value for the point from haystack
         * @param hsApi
         * @param equipRef
         * @param markers
         * @return Double
         */
        private fun readConfig(hsApi: CCUHsApi, equipRef: String, markers: String): Double {
            return hsApi.readDefaultVal(
                "point and $markers and equipRef == \"$equipRef\""
            )
        }

        /**
         * Function to read all the fan coil unit specific tuners which are required for Hyperstat to run on standalone mode
         * @param equipRef
         * @return HyperStatTunersGeneric_t
         */
        private fun getFcuTunerDetails(equipRef: String): HyperStat.HyperStatTunersFcu_t {
            val fcuTuners = HyperStat.HyperStatTunersFcu_t.newBuilder()
            fcuTuners.twoPipeHeatingThreshold = TunerUtil.readTunerValByQuery("tuner and heating and threshold and equipRef == \"${equipRef}\"").toInt()
            fcuTuners.twoPipeCoolingThreshold = TunerUtil.readTunerValByQuery("tuner and cooling and threshold and equipRef == \"${equipRef}\"").toInt()
            fcuTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage1 and equipRef == \"${equipRef}\"").toInt()
            fcuTuners.auxHeating2Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage2 and equipRef == \"${equipRef}\"").toInt()
            fcuTuners.waterValueSamplingOnTime = TunerUtil.readTunerValByQuery("tuner and samplingrate and water and on and time and not loop and equipRef == \"${equipRef}\"").toInt()
            fcuTuners.watreValueSamplingWaitTime = TunerUtil.readTunerValByQuery("tuner and samplingrate and water and wait and time and not loop and equipRef == \"${equipRef}\"").toInt()
            fcuTuners.waterValveSamplingDuringLoopDeadbandOnTime = TunerUtil.readTunerValByQuery("tuner and samplingrate and loop and on and time and equipRef == \"${equipRef}\"").toInt()
            fcuTuners.waterValveSamplingDuringLoopDeadbandWaitTime = TunerUtil.readTunerValByQuery("tuner and samplingrate and loop and wait and time and equipRef == \"${equipRef}\"").toInt()
            return fcuTuners.build()
        }

        /**
         * Function to read all the fan coil unit specific tuners which are required for Hyperstat to run on standalone mode
         * @param equipRef
         * @return HyperStatTunersGeneric_t
         */
        private fun getHpuTunerDetails(equipRef: String): HyperStat.HyperStatTunersFcu_t {
            val hpuTuners = HyperStat.HyperStatTunersFcu_t.newBuilder()
            hpuTuners.auxHeating1Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage1 and equipRef == \"${equipRef}\"").toInt()
            hpuTuners.auxHeating2Activate = TunerUtil.readTunerValByQuery("tuner and heating and aux and stage2 and equipRef == \"${equipRef}\"").toInt()
            return hpuTuners.build()
        }

        var  ccuControlMessageTimer :Long = 0
            get() {
                if (field == 0L){
                    ccuControlMessageTimer = System.currentTimeMillis()}
                return field

            }
        // Below method returns query based on DesiredTempMode
        fun getHeatingUserLimitByQuery(query: String, roomRef: String) : String{
            return "schedulable and heating and user and limit and $query and roomRef == \"$roomRef\""
        }

        fun getCoolingUserLimitByQuery(query: String, roomRef: String) : String{
            return "schedulable and cooling and user and limit and $query and roomRef == \"$roomRef\""
        }

        /**
         * Function to read all the staged fan voltages which are required for Hyperstat to run
         * @param equipRef
         * @return HyperstatStagedFanVoltages_t
         */
        private fun getStagedFanVoltageDetails(equipRef: String): HyperStat.HyperStatConfigsCpu_t? {
            val stagedFanVoltages = HyperStat.HyperStatConfigsCpu_t.newBuilder()
            val ccuHsApi = CCUHsApi.getInstance()
            val equipRefQuery = "equipRef == \"$equipRef\""

            val coolingStage1Query = "cooling and stage1 and fan and $equipRefQuery"
            val coolingStage2Query = "cooling and stage2 and fan and $equipRefQuery"
            val coolingStage3Query = "cooling and stage3 and fan and $equipRefQuery"
            val heatingStage1Query = "heating and stage1 and fan and $equipRefQuery"
            val heatingStage2Query = "heating and stage2 and fan and $equipRefQuery"
            val heatingStage3Query = "heating and stage3 and fan and $equipRefQuery"
            val aOutAtRecircQuery = "config and recirculate and $equipRefQuery"

            if (ccuHsApi.readEntity(coolingStage1Query).isNotEmpty()) {
                stagedFanVoltages.coolingStage1FanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(coolingStage1Query) * 10).toInt()
            }
            if (ccuHsApi.readEntity(coolingStage2Query).isNotEmpty()) {
                stagedFanVoltages.coolingStage2FanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(coolingStage2Query) * 10).toInt()
            }
            if (ccuHsApi.readEntity(coolingStage3Query).isNotEmpty()) {
                stagedFanVoltages.coolingStage3FanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(coolingStage3Query) * 10).toInt()
            }
            if (ccuHsApi.readEntity(heatingStage1Query).isNotEmpty()) {
                stagedFanVoltages.heatingStage1FanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(heatingStage1Query) * 10).toInt()
            }
            if (ccuHsApi.readEntity(heatingStage2Query).isNotEmpty()) {
                stagedFanVoltages.heatingStage2FanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(heatingStage2Query) * 10).toInt()
            }
            if (ccuHsApi.readEntity(heatingStage3Query).isNotEmpty()) {
                stagedFanVoltages.heatingStage3FanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(heatingStage3Query) * 10).toInt()
            }
            if (ccuHsApi.readEntity(aOutAtRecircQuery).isNotEmpty()) {
                stagedFanVoltages.analogoutAtRecFanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(aOutAtRecircQuery) * 10).toInt()
            }

            return stagedFanVoltages.build()
        }

        /**
         * Function to read all the linear fan speeds which are required for Hyperstat to run
         * @param equipRef
         * @return HyperstatLinearFanSpeeds_t
         */
        fun getLinearFanSpeedDetails(equipRef: String): HyperStat.HyperstatLinearFanSpeeds_t? {
            val linearFanSpeedBuilder = HyperStat.HyperstatLinearFanSpeeds_t.newBuilder()
            val ccuHsApi = CCUHsApi.getInstance()
            val equipRefQuery = "equipRef == \"$equipRef\""

            val fanSpeedLevels = listOf("low", "medium", "high")

            for (fanSpeed in fanSpeedLevels) {
                for (analog in listOf("analog3", "analog2", "analog1")) {
                    val query = "$analog and $fanSpeed and config and fan and $equipRefQuery"
                    if (ccuHsApi.readEntity(query).isNotEmpty()&& getAnalogOutMapping(ccuHsApi,equipRef,analog)
                        == HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal) {
                        val fanLevel = ccuHsApi.readPointPriorityValByQuery(query).toInt()
                        when (fanSpeed) {
                            "low" -> linearFanSpeedBuilder.linearFanLowSpeedLevel = fanLevel
                            "medium" -> linearFanSpeedBuilder.linearFanMediumSpeedLevel = fanLevel
                            "high" -> linearFanSpeedBuilder.linearFanHighSpeedLevel = fanLevel
                        }
                        break
                    }
                }
            }
            return linearFanSpeedBuilder.build()
        }

        /**
         * Function to read all the staged fan speed which are required for Hyperstat to run
         * @param equipRef
         * @return HyperstatStagedFanSpeeds_t
         */
        fun getStagedFanSpeedDetails(equipRef: String): HyperStat.HyperstatStagedFanSpeeds_t? {
            val stagedFanSpeedBuilder = HyperStat.HyperstatStagedFanSpeeds_t.newBuilder()
            val ccuHsApi = CCUHsApi.getInstance()
            val equipRefQuery = "equipRef == \"$equipRef\""

            val fanSpeedLevels = listOf("low", "medium", "high")

            for (fanSpeed in fanSpeedLevels) {
                for (analog in listOf("analog3", "analog2", "analog1")) {
                    val query = "$analog and $fanSpeed and config and fan and $equipRefQuery"
                    if (ccuHsApi.readEntity(query).isNotEmpty() && getAnalogOutMapping(ccuHsApi,equipRef,analog)
                        == HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                        val fanLevel = ccuHsApi.readPointPriorityValByQuery(query).toInt()
                        when (fanSpeed) {
                            "low" -> stagedFanSpeedBuilder.stagedFanLowSpeedLevel = fanLevel
                            "medium" -> stagedFanSpeedBuilder.stagedFanMediumSpeedLevel = fanLevel
                            "high" -> stagedFanSpeedBuilder.stagedFanHighSpeedLevel = fanLevel
                        }
                        break
                    }
                }
            }
            return stagedFanSpeedBuilder.build()
        }
        fun getAnalogOutMapping(
            ccuHsApi: CCUHsApi,
            equipRef: String,
            analog: String
        ): Any {
            return readConfig(ccuHsApi, equipRef, "$analog and output and config and association").toInt()
        }
    }
}

// Domain name updated functions


private fun getHeatingUserLimit(type: String, roomRef: String) : Int {
    val hsApi = CCUHsApi.getInstance()
    var pointValue = hsApi.readPointPriorityValByQuery("schedulable and heating and user and limit and $type and roomRef == \"$roomRef\"")
    if (pointValue == 0.0) { // // Fall back to default value
        pointValue = hsApi.readPointPriorityValByQuery("point and schedulable and default and heating and user and limit and min")
    }
    return pointValue.toInt()
}

private fun getCoolingUserLimit(type: String, roomRef: String) : Int {
    val hsApi = CCUHsApi.getInstance()
    var pointValue = hsApi.readPointPriorityValByQuery("schedulable and cooling and user and limit and $type and roomRef == \"$roomRef\"")
    if (pointValue == 0.0) { // Fall back to default value
        pointValue = hsApi.readPointPriorityValByQuery("point and schedulable and default and cooling and user and limit and min")
    }
    return pointValue.toInt()
}

private fun getHeatingDeadBand(roomRef: String): Double {
    val pointId = CCUHsApi.getInstance().readId("deadband and heating and not multiplier and roomRef == \"$roomRef\"")
    if (pointId != null)
        return HSUtil.getPriorityVal(pointId)
    return 0.0
}

private fun getCoolingDeadBand(roomRef: String): Double {
    val pointId = CCUHsApi.getInstance().readId("deadband and cooling and not multiplier and roomRef == \"$roomRef\"")
    if (pointId != null)
        return HSUtil.getPriorityVal(pointId)
    return 0.0
}

private fun getTempMode(): HyperStat.HyperStatTemperatureMode_e {
    val temperatureMode = readValAtLevelByDomain(DomainName.temperatureMode, TunerConstants.SYSTEM_BUILDING_VAL_LEVEL).toInt()
    return  if (temperatureMode == 0)
        HyperStat.HyperStatTemperatureMode_e.HYPERSTAT_TEMP_MODE_DUAL_FIXED_DB
    else
        HyperStat.HyperStatTemperatureMode_e.HYPERSTAT_TEMP_MODE_DUAL_VARIABLE_DB
}

private fun isAnalogOutEnabledAndIsMapped(enablePoint: Point, association: Point, mapping: Int): Boolean {
    return (enablePoint.readDefaultVal() == 1.0 && association.readDefaultVal().toInt() == mapping )
}

private fun setLinearFanSpeedDetails(equip: HyperStatEquip): HyperStat.HyperstatLinearFanSpeeds_t {
    val linearFanSpeedBuilder = HyperStat.HyperstatLinearFanSpeeds_t.newBuilder()
    equip.apply {
        if (this is CpuV2Equip) {
            if (isAnalogOutEnabledAndIsMapped(analog1OutputEnable, analog1OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                    linearFanSpeedBuilder.linearFanLowSpeedLevel = analog1FanLow.readPriorityVal().toInt()
                    linearFanSpeedBuilder.linearFanMediumSpeedLevel = analog1FanMedium.readPriorityVal().toInt()
                    linearFanSpeedBuilder.linearFanHighSpeedLevel = analog1FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog2OutputEnable, analog2OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                linearFanSpeedBuilder.linearFanLowSpeedLevel = analog2FanLow.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanMediumSpeedLevel = analog2FanMedium.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanHighSpeedLevel = analog2FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog3OutputEnable, analog3OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                linearFanSpeedBuilder.linearFanLowSpeedLevel = analog3FanLow.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanMediumSpeedLevel = analog3FanMedium.readPriorityVal().toInt()
                linearFanSpeedBuilder.linearFanHighSpeedLevel = analog3FanHigh.readPriorityVal().toInt()
            }
        }
    }
    return linearFanSpeedBuilder.build()
}

private fun setStagedFanSpeedDetails(equip: HyperStatEquip): HyperStat.HyperstatStagedFanSpeeds_t {
    val stagedFanSpeedBuilder = HyperStat.HyperstatStagedFanSpeeds_t.newBuilder()
    equip.apply {
        if (this is CpuV2Equip) {
            if (isAnalogOutEnabledAndIsMapped(analog1OutputEnable, analog1OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog1FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanMediumSpeedLevel = analog1FanMedium.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog1FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog2OutputEnable, analog2OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog2FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanMediumSpeedLevel = analog2FanMedium.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog2FanHigh.readPriorityVal().toInt()
            }
            if (isAnalogOutEnabledAndIsMapped(analog3OutputEnable, analog3OutputAssociation, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal)) {
                stagedFanSpeedBuilder.stagedFanLowSpeedLevel = analog3FanLow.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanMediumSpeedLevel = analog3FanMedium.readPriorityVal().toInt()
                stagedFanSpeedBuilder.stagedFanHighSpeedLevel = analog3FanHigh.readPriorityVal().toInt()
            }
        }
    }
    return stagedFanSpeedBuilder.build()
}

private fun getRelayConfigs(equip: HyperStatEquip): HyperStat.HyperstatRelay_t {
    val relayConfigs = HyperStat.HyperstatRelay_t.newBuilder()
    equip.apply {
        relayConfigs.relay1Enable = relay1OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay2Enable = relay2OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay3Enable = relay3OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay4Enable = relay4OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay5Enable = relay5OutputEnable.readDefaultVal() == 1.0
        relayConfigs.relay6Enable = relay6OutputEnable.readDefaultVal() == 1.0

        /**
         * Firmware mapping enum has "none" at 0 position but ccu will will not use none.
         * So we are adding 1 to avoid the 0 position all the time
         */

        if (relayConfigs.relay1Enable)
            relayConfigs.relay1Mapping = 1 + relay1OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay2Enable)
            relayConfigs.relay2Mapping = 1 + relay2OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay3Enable)
            relayConfigs.relay3Mapping = 1 + relay3OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay4Enable)
            relayConfigs.relay4Mapping = 1 + relay4OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay5Enable)
            relayConfigs.relay5Mapping = 1 + relay5OutputAssociation.readDefaultVal().toInt()
        if (relayConfigs.relay6Enable)
            relayConfigs.relay6Mapping = 1 + relay6OutputAssociation.readDefaultVal().toInt()
    }
    return relayConfigs.build()
}

private fun getAnalogOutConfigs(equip: HyperStatEquip): HyperStat.HyperstatAnalogOut_t {
    val analogOutConfigs = HyperStat.HyperstatAnalogOut_t.newBuilder()
    val hsApi = CCUHsApi.getInstance()
    equip.apply {
        analogOutConfigs.analogOut1Enable =  analog1OutputEnable.readDefaultVal() == 1.0
        analogOutConfigs.analogOut2Enable =  analog2OutputEnable.readDefaultVal() == 1.0
        analogOutConfigs.analogOut3Enable =  analog3OutputEnable.readDefaultVal() == 1.0

        if (analogOutConfigs.analogOut1Enable) {
            analogOutConfigs.analogOut1Mapping = analog1OutputAssociation.readDefaultVal().toInt()
            analogOutConfigs.analogOut1AtMinSetting = hsApi.readDefaultVal("min and analog == 1 and equipRef == \"$equipRef\"").toInt()
            analogOutConfigs.analogOut1AtMaxSetting = hsApi.readDefaultVal("max and analog == 1 and equipRef == \"$equipRef\"").toInt()
        }
        if (analogOutConfigs.analogOut2Enable) {
            analogOutConfigs.analogOut2Mapping = analog2OutputAssociation.readDefaultVal().toInt()
            analogOutConfigs.analogOut2AtMinSetting = hsApi.readDefaultVal("min and analog == 2 and equipRef == \"$equipRef\"").toInt()
            analogOutConfigs.analogOut2AtMaxSetting = hsApi.readDefaultVal("max and analog == 2 and equipRef == \"$equipRef\"").toInt()
        }
        if (analogOutConfigs.analogOut3Enable) {
            analogOutConfigs.analogOut3Mapping = analog3OutputAssociation.readDefaultVal().toInt()
            analogOutConfigs.analogOut3AtMinSetting = hsApi.readDefaultVal("min and analog == 3 and equipRef == \"$equipRef\"").toInt()
            analogOutConfigs.analogOut3AtMaxSetting = hsApi.readDefaultVal("max and analog == 3 and equipRef == \"$equipRef\"").toInt()
        }
    }
    return analogOutConfigs.build()
}

private fun getAnalogInputConfigs(equip: HyperStatEquip): HyperStat.HyperstatAnalogIn_t {
    val analogIn = HyperStat.HyperstatAnalogIn_t.newBuilder()
    equip.apply {
        analogIn.analogIn1Enable = analog1InputEnable.readDefaultVal() == 1.0
        analogIn.analogIn2Enable = analog2InputEnable.readDefaultVal() == 1.0

        if (analogIn.analogIn1Enable) {
            analogIn.analogIn1Mapping = HyperStat.HyperstatAnalogInMapping_t.values()[analog1InputAssociation.readDefaultVal().toInt()]
        }
        if (analogIn.analogIn2Enable) {
            analogIn.analogIn2Mapping = HyperStat.HyperstatAnalogInMapping_t.values()[analog2InputAssociation.readDefaultVal().toInt()]
        }
    }
    return analogIn.build()
}

fun getHyperStatSettingsMessage(equipRef: String, zone: String): HyperStatSettingsMessage_t {
    val hyperStatEquip = Domain.getDomainEquip(equipRef) as HyperStatEquip
    val zoneId = HSUtil.getZoneIdFromEquipId(equipRef)
    return HyperStatSettingsMessage_t.newBuilder()
            .setRoomName(zone)
            .setHeatingDeadBand((getHeatingDeadBand(zoneId) * 10).toInt())
            .setCoolingDeadBand((getCoolingDeadBand(zoneId) * 10).toInt())
            .setMinCoolingUserTemp(getCoolingUserLimit("min", zoneId))
            .setMaxCoolingUserTemp(getCoolingUserLimit("max", zoneId))
            .setMinHeatingUserTemp(getHeatingUserLimit("min", zoneId))
            .setMaxHeatingUserTemp(getHeatingUserLimit("max", zoneId))
            .setTemperatureOffset((hyperStatEquip.temperatureOffset.readPriorityVal() * 10).toInt())
            .setHumidityMinSetpoint(hyperStatEquip.targetHumidifier.readPriorityVal().toInt())
            .setHumidityMaxSetpoint(hyperStatEquip.targetDehumidifier.readPriorityVal().toInt())
            .setShowCentigrade(getUserConfiguration() == 1.0)
            .setDisplayHumidity(hyperStatEquip.enableHumidityDisplay.readDefaultVal() == 1.0)
            .setDisplayCO2(hyperStatEquip.enableCo2Display.readDefaultVal() == 1.0)
            .setDisplayVOC(false)
            .setDisplayPM25(hyperStatEquip.enablePm25Display.readDefaultVal() == 1.0)
            .setCo2AlertTarget(hyperStatEquip.co2Target.readPriorityVal().toInt())
            .setPm25AlertTarget(hyperStatEquip.pm25Target.readPriorityVal().toInt())
            .setVocAlertTarget(4000)
            .setHyperstatLinearFanSpeeds(setLinearFanSpeedDetails(hyperStatEquip))
            .setHyperstatStagedFanSpeeds(setStagedFanSpeedDetails(hyperStatEquip))
            .setTemperatureMode(getTempMode())
            .build()
}

fun getHyperStatSettings2Message(equipRef: String): HyperStatSettingsMessage2_t {
    val settings2 = HyperStatSettingsMessage2_t.newBuilder()
    val hyperStatEquip = Domain.getDomainEquip(equipRef) as HyperStatEquip

    /*For monitoring Equip settings2 message not required.
    * CCU will crash if we try to send setting2 for monitoring
    * Because From proto fil we have only 6 values for Analog-in
    * But from CCU configuration we have till 11. */
    if(hyperStatEquip is MonitoringEquip){
        return settings2.build()
    }
    settings2.apply {
        enableForceOccupied = hyperStatEquip.autoForceOccupied.readDefaultVal() == 1.0
        enableAutoAway = hyperStatEquip.autoAway.readDefaultVal() == 1.0
        hyperstatRelayConfig = getRelayConfigs(hyperStatEquip)
        hyperstatAnalogOutConfig = getAnalogOutConfigs(hyperStatEquip)
        hyperstatAnalogInConfig = getAnalogInputConfigs(hyperStatEquip)
        thermistor1Enable = hyperStatEquip.thermistor1InputEnable.readDefaultVal() == 1.0
        thermistor2Enable = hyperStatEquip.thermistor2InputEnable.readDefaultVal() == 1.0
        zoneCO2Target = hyperStatEquip.co2Target.readDefaultVal().toInt()
        zoneCO2Threshold = hyperStatEquip.co2Threshold.readDefaultVal().toInt()
        zoneCO2DamperOpeningRate = hyperStatEquip.co2DamperOpeningRate.readDefaultVal().toInt()
        proportionalConstant = (TunerUtil.getProportionalGain(equipRef) * 100).toInt() // Try to use domain equip
        integralConstant = (TunerUtil.getIntegralGain(equipRef) * 100).toInt()
        proportionalTemperatureRange = (TunerUtil.getProportionalSpread(equipRef) * 10).toInt()
        integrationTime = TunerUtil.getIntegralTimeout(equipRef).toInt()
    }

    when (hyperStatEquip) {
        is CpuV2Equip -> settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_CONVENTIONAL_PACKAGE_UNIT
        is HpuV2Equip -> settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_HEAT_PUMP_UNIT
        is Pipe2V2Equip -> settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_2_PIPE_FANCOIL_UNIT
        // TODO check for bellow profiles HYPERSTAT_MONITORING , HYPERSTAT_PROFILE_VRV
    }
    return settings2.build()
}

private fun getCommonTuners(equipRef: String): HyperStat.HyperStatTunersGeneric_t {
    val equip = CCUHsApi.getInstance().readHDictById(equipRef)
    return HyperStat.HyperStatTunersGeneric_t.newBuilder().apply {
        unoccupiedSetback = (CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and zone and unoccupied and setback and roomRef == \"" + equip.get("roomRef").toString() + "\"") * 10).toInt()
        minFanRuntimePostconditioning = TunerUtil.readTunerValByQuery("min and fan and runtime and postconditioning", equipRef).toInt()
        relayActivationHysteresis = TunerUtil.getHysteresisPoint("relay and activation", equipRef).toInt()
        analogFanSpeedMultiplier = (TunerUtil.readTunerValByQuery("analog and fan and speed and multiplier", equipRef) * 10 ).toInt()
        humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equipRef).toInt()
        autoAwayZoneSetbackTemp = (TunerUtil.readTunerValByQuery("auto and away and setback") * 10).toInt()
        autoAwayTime = TunerUtil.readTunerValByQuery("auto and away and time", equipRef).toInt()
        forcedOccupiedTime = TunerUtil.readTunerValByQuery("forced and occupied and time", equipRef).toInt()
    }.build()
}

private fun getStagedFanDetails(equip: CpuV2Equip): HyperStat.HyperStatConfigsCpu_t {
    return HyperStat.HyperStatConfigsCpu_t.newBuilder().apply {
        coolingStage1FanAnalogVoltage = (equip.fanOutCoolingStage1.readPriorityVal() * 10).toInt()
        coolingStage2FanAnalogVoltage = (equip.fanOutCoolingStage2.readPriorityVal() * 10).toInt()
        coolingStage3FanAnalogVoltage = (equip.fanOutCoolingStage3.readPriorityVal() * 10).toInt()
        heatingStage1FanAnalogVoltage = (equip.fanOutHeatingStage1.readPriorityVal() * 10).toInt()
        heatingStage2FanAnalogVoltage = (equip.fanOutHeatingStage2.readPriorityVal() * 10).toInt()
        heatingStage3FanAnalogVoltage = (equip.fanOutHeatingStage3.readPriorityVal() * 10).toInt()
    }.build()
}

fun getHyperStatSettings3Message(equipRef: String): HyperStatSettingsMessage3_t {
    val hyperStatEquip = Domain.getDomainEquip(equipRef) as HyperStatEquip
    return HyperStatSettingsMessage3_t.newBuilder().apply {
        genertiTuners = getCommonTuners(equipRef)
        when(hyperStatEquip) {
            is CpuV2Equip ->  hyperStatConfigsCpu = getStagedFanDetails(hyperStatEquip) // add for other profils
        }
    }.build()

}



