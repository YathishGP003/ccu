package a75f.io.device.mesh.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.device.HyperStat
import a75f.io.device.HyperStat.HyperStatSettingsMessage2_t
import a75f.io.device.HyperStat.HyperStatSettingsMessage3_t
import a75f.io.logic.bo.building.definitions.ProfileType
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

            when (equip.profile) {
                ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name -> {

                    settings2.profile = HyperStat.HyperStatProfiles_t.HYPERSTAT_PROFILE_CONVENTIONAL_PACKAGE_UNIT

                    settings2.enableForceOccupied = isAutoForceEnabled(hsApi, equipRef)
                    settings2.enableAutoAway = isAutoAwayEnabled(hsApi, equipRef)
                    settings2.hyperstatRelayConfig = getRelayConfigDetails(hsApi, equipRef)
                    settings2.hyperstatAnalogOutConfig = getAnalogOutConfigDetails(hsApi, equipRef)
                    settings2.hyperstatAnalogInConfig = getAnalogInConfigDetails(hsApi, equipRef)
                    settings2.thermistor1Enable = getTh1Enabled(hsApi, equipRef)
                    settings2.thermistor2Enable = getTh2Enabled(hsApi, equipRef)

                    settings2.zoneCO2Target = readConfig(hsApi, equipRef, "co2 and target and config").toInt()
                    settings2.zoneCO2Threshold = readConfig(hsApi, equipRef, "co2 and threshold and config").toInt()
                    settings2.zoneCO2DamperOpeningRate =
                        readConfig(hsApi, equipRef, "co2 and damper and config and damper").toInt()
                    settings2.proportionalConstant = (TunerUtil.getProportionalGain(equipRef) * 100).toInt()
                    settings2.integralConstant = (TunerUtil.getIntegralGain(equipRef) * 100).toInt()
                    settings2.proportionalTemperatureRange = (TunerUtil.getProportionalSpread(equipRef) * 10).toInt()
                    settings2.integrationTime = TunerUtil.getIntegralTimeout(equipRef).toInt()

                }
                ProfileType.HYPERSTAT_SENSE.name -> {
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
                }
                ProfileType.HYPERSTAT_SENSE.name -> {
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
            return (readConfig(hsApi, equipRef, "auto and forced and control and enabled") == 1.0)
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
        private fun getTh2Enabled(hsApi: CCUHsApi, equipRef: String): Boolean {
            return (readConfig(hsApi, equipRef, "th2 and config and enabled") == 1.0)
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

            analogOutConfiguration.analogOut1Enable = readConfig(
                hsApi, equipRef, "analog1 and out and config and enabled"
            ) == 1.0

            analogOutConfiguration.analogOut2Enable = readConfig(
                hsApi, equipRef, "analog2 and out and config and enabled"
            ) == 1.0

            analogOutConfiguration.analogOut3Enable = readConfig(
                hsApi, equipRef, "analog3 and out and config and enabled"
            ) == 1.0

            if (analogOutConfiguration.analogOut1Enable) {
                analogOutConfiguration.analogOut1Mapping = 1 + readConfig(
                    hsApi, equipRef, "analog1 and out and config and association "
                ).toInt()

                analogOutConfiguration.analogOut1AtMinSetting = (readConfig(
                    hsApi, equipRef, "analog1 and out and config and min"
                ) * 10).toInt()
                analogOutConfiguration.analogOut1AtMaxSetting = (readConfig(
                    hsApi, equipRef, "analog1 and out and config and max "
                ) * 10).toInt()
            }

            if (analogOutConfiguration.analogOut2Enable) {
                analogOutConfiguration.analogOut2Mapping = 1 + readConfig(
                    hsApi, equipRef, "analog2 and out and config and association "
                ).toInt()

                analogOutConfiguration.analogOut2AtMinSetting = (readConfig(
                    hsApi, equipRef, "analog2 and out and config and min"
                ) * 10).toInt()
                analogOutConfiguration.analogOut2AtMaxSetting = (readConfig(
                    hsApi, equipRef, "analog2 and out and config and max "
                ) * 10).toInt()
            }

            if (analogOutConfiguration.analogOut3Enable) {
                analogOutConfiguration.analogOut3Mapping = 1 + readConfig(
                    hsApi, equipRef, "analog3 and out and config and association "
                ).toInt()

                analogOutConfiguration.analogOut3AtMinSetting = (readConfig(
                    hsApi, equipRef, "analog3 and out and config and min"
                ) * 10).toInt()
                analogOutConfiguration.analogOut3AtMaxSetting = (readConfig(
                    hsApi, equipRef, "analog3 and out and config and max "
                ) * 10).toInt()
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
                hsApi, equipRef, "analog1 and in and config and enabled"
            ) == 1.0

            analogIn.analogIn2Enable = readConfig(
                hsApi, equipRef, "analog2 and in and config and enabled"
            ) == 1.0

            if (analogIn.analogIn1Enable) {
                val mapping = readConfig(hsApi, equipRef, "analog1 and in and config and association ").toInt()
                analogIn.analogIn1Mapping = HyperStat.HyperstatAnalogInMapping_t.values()[mapping]
            }
            if (analogIn.analogIn2Enable) {
                val mapping = readConfig(hsApi, equipRef, "analog2 and in and config and association ").toInt()
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
            genericTuners.unoccupiedSetback = (TunerUtil.readTunerValByQuery(
                "unoccupied and setback", equipRef) * 10).toInt()
            genericTuners.relayActivationHysteresis =
                TunerUtil.getHysteresisPoint("relay and  activation", equipRef).toInt()
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
                "hyperstat and $markers and equipRef == \"$equipRef\""
            )
        }

    }
}