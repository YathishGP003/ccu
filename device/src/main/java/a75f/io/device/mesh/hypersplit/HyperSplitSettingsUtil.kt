package a75f.io.device.mesh.hypersplit

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.device.HyperSplit
import a75f.io.device.HyperSplit.HyperSplitSettingsMessage4_t
import a75f.io.domain.api.DomainName
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconAnalogOutAssociation
import a75f.io.logic.tuners.TunerUtil


/**
 * Created by Manjunath K for HyperStat on 14-12-2021.
 * Created by Nick P for HyperStat Split on 8/11/2023.
 */
class HyperSplitSettingsUtil {

    companion object {

        /**
         * Function which construct the setting2 message details
         * @param equipRef
         * @param hsApi
         * @param nodeAddress
         * return HyperSplitSettingsMessage2_t
         */
        fun getSetting2Message(nodeAddress: Int, equipRef: String, hsApi: CCUHsApi): HyperSplit.HyperSplitSettingsMessage2_t {
            val settings2 = HyperSplit.HyperSplitSettingsMessage2_t.newBuilder()
            val equip = getEquipDetails(nodeAddress)

            // These all common configuration for all the profiles
            settings2.enableForceOccupied = isAutoForceEnabled(hsApi, equipRef)
            settings2.enableAutoAway = isAutoAwayEnabled(hsApi, equipRef)
            settings2.hyperSplitRelayConfig = getRelayConfigDetails(hsApi, equipRef)
            settings2.hyperSplitAnalogOutConfig = getAnalogOutConfigDetails(hsApi, equipRef)
            settings2.hyperSplitUniversalInConfig = getUniversalInConfigDetails(hsApi, equipRef)
            settings2.hyperSplitSensorBusConfig = getSensorBusConfigDetails(hsApi, equipRef)
            settings2.zoneCO2Target = readConfig(hsApi, equipRef, "domainName == \"" + DomainName.co2Target + "\"").toInt()
            settings2.zoneCO2Threshold = readConfig(hsApi, equipRef, "domainName == \"" + DomainName.co2Threshold + "\"").toInt()
            settings2.zoneCO2DamperOpeningRate = readConfig(hsApi, equipRef, "domainName == \"" + DomainName.co2DamperOpeningRate + "\"").toInt()
            settings2.proportionalConstant = (TunerUtil.getProportionalGain(equipRef) * 100).toInt()
            settings2.integralConstant = (TunerUtil.getIntegralGain(equipRef) * 100).toInt()
            settings2.proportionalTemperatureRange = (TunerUtil.getProportionalSpread(equipRef) * 10).toInt()
            settings2.integrationTime = TunerUtil.getIntegralTimeout(equipRef).toInt()
            
            when (equip.profile) {
                ProfileType.HYPERSTATSPLIT_CPU.name -> {
                    settings2.profile = HyperSplit.HyperSplitProfiles_t.HYPERSPLIT_PROFILE_CONVENTIONAL_PACKAGE_UNIT_ECONOMIZER
                }
            }

            return settings2.build()
        }

        /**
         * Function which construct the setting3 message details
         * @param equipRef
         * @param nodeAddress
         * return HyperSplitSettingsMessage3_t
         */
        fun getSetting3Message(nodeAddress: Int, equipRef: String, hsApi: CCUHsApi): HyperSplit.HyperSplitSettingsMessage3_t {
            val settings3 = HyperSplit.HyperSplitSettingsMessage3_t.newBuilder()
            val equip = getEquipDetails(nodeAddress)

            val ecoTunersBuilder = HyperSplit.HyperSplitTunersEco_t.newBuilder()
            ecoTunersBuilder.setEconomizingToMainCoolingLoopMap(getEconomizingToMainCoolingLoopMap(hsApi, equipRef))
            ecoTunersBuilder.setEconomizingMinTemp(getEconomizingMinTemp(hsApi, equipRef))
            ecoTunersBuilder.setEconomizingMaxTemp(getEconomizingMaxTemp(hsApi, equipRef))
            ecoTunersBuilder.setEconomizingMinHumidity(getEconomizingMinHumidity(hsApi, equipRef))
            ecoTunersBuilder.setEconomizingMaxHumidity(getEconomizingMaxHumidity(hsApi, equipRef))
            ecoTunersBuilder.setEconomizingDryBulbThreshold(getEconomizingDryBulbThreshold(hsApi, equipRef))
            ecoTunersBuilder.setEnthalpyDuctCompensationOffset(getEnthalpyDuctCompensationOffset(hsApi, equipRef))
            ecoTunersBuilder.setDuctCompensationOffset(getDuctTemperatureOffset(hsApi, equipRef))
            ecoTunersBuilder.setExhaustFanStage1Threshold(getExhaustFanStage1Threshold(hsApi, equipRef))
            ecoTunersBuilder.setExhaustFanStage2Threshold(getExhaustFanStage2Threshold(hsApi, equipRef))
            ecoTunersBuilder.setExhaustFanHysteresis(getExhaustFanHysteresis(hsApi, equipRef))
            ecoTunersBuilder.setOaoDamperMatTarget(getOaoDamperMatTarget(hsApi, equipRef))
            ecoTunersBuilder.setOaoDamperMatMin(getOaoDamperMatMin(hsApi, equipRef))
            ecoTunersBuilder.setOutsideDamperMinOpen(0)
            ecoTunersBuilder.setAoutFanEconomizer(getAnalogOutFanDuringEconomizer(hsApi, equipRef))
            
            settings3.setEcoTuners(ecoTunersBuilder.build())

            when (equip.profile) {
                ProfileType.HYPERSTATSPLIT_CPU.name -> {
                    settings3.genertiTuners = getGenericTunerDetails(equipRef)
                    settings3.hyperStatConfigsCpu = getStagedFanVoltageDetails(equipRef)
                }
            }

            return settings3.build()
        }

        /**
         * Function which constructs the setting4 message details
         * @param equipRef
         * @param nodeAddress
         * return HyperStatSettingsMessage4_t
         */
        fun getSetting4Message(nodeAddress: Int, equipRef: String, hsApi: CCUHsApi): HyperSplitSettingsMessage4_t {
            val settings4 = HyperSplitSettingsMessage4_t.newBuilder()

            settings4.setOutsideDamperMinOpenDuringRecirculation(getOutsideDamperMinOpenDuringRecirculation(hsApi, equipRef))
            settings4.setOutsideDamperMinOpenDuringConditioning(getOutsideDamperMinOpenDuringConditioning(hsApi, equipRef))
            settings4.setOutsideDamperMinOpenDuringFanLow(getOutsideDamperMinOpenDuringFanLow(hsApi, equipRef))
            settings4.setOutsideDamperMinOpenDuringFanMedium(getOutsideDamperMinOpenDuringFanMedium(hsApi, equipRef))
            settings4.setOutsideDamperMinOpenDuringFanHigh(getOutsideDamperMinOpenDuringFanHigh(hsApi, equipRef))

            return settings4.build()
        }



        private fun getEconomizingToMainCoolingLoopMap(hsApi: CCUHsApi, equipRef: String): Int {
            
            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneEconomizingToMainCoolingLoopMap + "\"").toInt()
            
        }

        private fun getEconomizingMinTemp(hsApi: CCUHsApi, equipRef: String): Int {

            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneEconomizingMinTemperature + "\"").toInt()

        }

        private fun getEconomizingMaxTemp(hsApi: CCUHsApi, equipRef: String): Int {

            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneEconomizingMaxTemperature + "\"").toInt()

        }
        
        private fun getEconomizingMinHumidity(hsApi: CCUHsApi, equipRef: String): Int {

            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneEconomizingMinHumidity + "\"").toInt()

        }

        private fun getEconomizingMaxHumidity(hsApi: CCUHsApi, equipRef: String): Int {

            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneEconomizingMaxHumidity + "\"").toInt()

        }

        private fun getEconomizingDryBulbThreshold(hsApi: CCUHsApi, equipRef: String): Int {

            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneEconomizingDryBulbThreshold + "\"").toInt()

        }

        private fun getEnthalpyDuctCompensationOffset(hsApi: CCUHsApi, equipRef: String): Int {

            return Math.round(readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneEnthalpyDuctCompensationOffset + "\"").toFloat())

        }

        private fun getDuctTemperatureOffset(hsApi: CCUHsApi, equipRef: String): Int {

            return Math.round(readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneDuctTemperatureOffset + "\"").toFloat())

        }

        private fun getExhaustFanStage1Threshold(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.exhaustFanStage1Threshold + "\"").toInt()

        }

        private fun getExhaustFanStage2Threshold(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.exhaustFanStage2Threshold + "\"").toInt()

        }

        private fun getExhaustFanHysteresis(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.exhaustFanHysteresis + "\"").toInt()

        }

        private fun getOaoDamperMatTarget(hsApi: CCUHsApi, equipRef: String): Int {

            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneOutsideDamperMixedAirTarget + "\"").toInt()

        }

        private fun getOaoDamperMatMin(hsApi: CCUHsApi, equipRef: String): Int {

            return readTuner(hsApi, equipRef, "domainName == \"" + DomainName.standaloneOutsideDamperMixedAirMinimum + "\"").toInt()

        }

        private fun getOutsideDamperMinOpenDuringConditioning(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.outsideDamperMinOpenDuringConditioning + "\"").toInt()

        }

        private fun getAnalogOutFanDuringEconomizer(hsApi: CCUHsApi, equipRef: String): Int {
            return 10 * readConfig(hsApi, equipRef, "domainName == \"" + DomainName.fanOutEconomizer + "\"").toInt()
        }

        private fun getOutsideDamperMinOpenDuringRecirculation(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.outsideDamperMinOpenDuringRecirculation + "\"").toInt()

        }

        private fun getOutsideDamperMinOpenDuringFanLow(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.outsideDamperMinOpenDuringFanLow + "\"").toInt()

        }

        private fun getOutsideDamperMinOpenDuringFanMedium(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.outsideDamperMinOpenDuringFanMedium + "\"").toInt()

        }

        private fun getOutsideDamperMinOpenDuringFanHigh(hsApi: CCUHsApi, equipRef: String): Int {

            return readConfig(hsApi, equipRef, "domainName == \"" + DomainName.outsideDamperMinOpenDuringFanHigh + "\"").toInt()

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
            return (readConfig(
                hsApi, 
                equipRef,
                "domainName == \"" + DomainName.autoForceOccupied + "\""
            ) == 1.0)
        }

        /**
         * Function to check the is AutoAway configuration enabled or not
         * @param equipRef
         * @param hsApi
         * @return Boolean
         */
        private fun isAutoAwayEnabled(hsApi: CCUHsApi, equipRef: String): Boolean {
            return (readConfig(
                hsApi,
                equipRef,
                "domainName == \"" + DomainName.autoAway + "\""
            ) == 1.0)
        }

        /**
         * Function which reads all the Relay toggle configuration and mapping details
         * @param equipRef
         * @param hsApi
         * @return HyperstatRelay_t
         */
        /*
            In HyperStat Controls Message "Relay Config" contains separate variables for "enabled" and "mapping".
            
            In HyperSplit Controls Message, each relay has a single "mapping" variable where 0=disabled.
         */
        private fun getRelayConfigDetails(hsApi: CCUHsApi, equipRef: String): HyperSplit.HyperSplitRelayConfig_t {
            val relayConfiguration = HyperSplit.HyperSplitRelayConfig_t.newBuilder()
            
            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay1OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay1Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay1Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay1OutputAssociation + "\"").toInt() + 1)
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay2OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay2Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay2Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay2OutputAssociation + "\"").toInt() + 1)
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay3OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay3Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay3Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay3OutputAssociation + "\"").toInt() + 1)
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay4OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay4Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay4Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay4OutputAssociation + "\"").toInt() + 1)
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay5OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay5Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay5Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay5OutputAssociation + "\"").toInt() + 1)
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay6OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay6Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay6Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay6OutputAssociation + "\"").toInt() + 1)
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay7OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay7Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay7Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay7OutputAssociation + "\"").toInt() + 1)
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay8OutputEnable + "\"") == 0.0) {
                relayConfiguration.relay8Mapping = getHyperSplitRelayMapping(0)
            } else {
                relayConfiguration.relay8Mapping = getHyperSplitRelayMapping(readConfig(hsApi, equipRef, "domainName == \"" + DomainName.relay8OutputAssociation + "\"").toInt() + 1)
            }

            return relayConfiguration.build()
        }

        private fun getHyperSplitRelayMapping(intAssociation: Int): HyperSplit.HyperSplitRelayMapping_t {
            // These are hard-coded. If the order of either enum changes (Haystack or Controll Message), this will need to be updated.
            if (intAssociation == 0) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED
            else if (intAssociation == 1) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_COOLING_STAGE_1
            else if (intAssociation == 2) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_COOLING_STAGE_2
            else if (intAssociation == 3) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_COOLING_STAGE_3
            else if (intAssociation == 4) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HEATING_STAGE_1
            else if (intAssociation == 5) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HEATING_STAGE_2
            else if (intAssociation == 6) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HEATING_STAGE_3
            else if (intAssociation == 7) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_LOW_SPEED
            else if (intAssociation == 8) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_MEDIUM_SPEED
            else if (intAssociation == 9) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_HIGH_SPEED
            else if (intAssociation == 10) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_ENABLE
            else if (intAssociation == 11) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_OCCUPIED_ENABLE
            else if (intAssociation == 12) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HUMIDIFIER
            else if (intAssociation == 13) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DEHUMIDIFIER
            else if (intAssociation == 14) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_EXAUST_1
            else if (intAssociation == 15) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_EXAUST_2
            else if (intAssociation == 16) return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED // TODO: DCV Damper

            // This should never happen
            else { return HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED }

        }

        /**
         * Function which reads all the Analog Out toggle configuration and mapping details
         * @param equipRef
         * @param hsApi
         * @return HypersplitAnalogOut_t
         */
        /*
            In HyperStat Controls Message "Analog Out Config" contains separate variables for "enabled" and "mapping".

            In HyperSplit Controls Message, each analog out has a single "mapping" variable where 0=disabled.
         */
        private fun getAnalogOutConfigDetails(hsApi: CCUHsApi, equipRef: String): HyperSplit.HyperSplitAnalogOutConfig_t {
            val analogOutConfiguration = HyperSplit.HyperSplitAnalogOutConfig_t.newBuilder()
            val defaultAnalogOutMinSetting = 0
            val defaultAnalogOutMaxSetting = 10


            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog1OutputEnable + "\"") == 0.0) {
                analogOutConfiguration.analogOut1Mapping = getHyperSplitAnalogOutMapping(0)
            } else {
                analogOutConfiguration.analogOut1Mapping = getHyperSplitAnalogOutMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog1OutputAssociation + "\"").toInt() + 1
                )

                if ((analogOutConfiguration.analogOut1Mapping.ordinal - 1) != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                    analogOutConfiguration.analogOut1AtMinSetting = (readConfig(
                        hsApi, equipRef, "analog == 1 and sp and config and min"
                    ) * 10).toInt()
                    analogOutConfiguration.analogOut1AtMaxSetting = (readConfig(
                        hsApi, equipRef, "analog == 1 and sp and config and max "
                    ) * 10).toInt()
                } else {
                    analogOutConfiguration.analogOut1AtMinSetting = defaultAnalogOutMinSetting
                    analogOutConfiguration.analogOut1AtMaxSetting = defaultAnalogOutMaxSetting * 10
                }
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog2OutputEnable + "\"") == 0.0) {
                analogOutConfiguration.analogOut2Mapping = getHyperSplitAnalogOutMapping(0)
            } else {
                analogOutConfiguration.analogOut2Mapping = getHyperSplitAnalogOutMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog2OutputAssociation + "\"").toInt() + 1
                )

                if ((analogOutConfiguration.analogOut2Mapping.ordinal - 1) != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                    analogOutConfiguration.analogOut2AtMinSetting = (readConfig(
                        hsApi, equipRef, "analog == 2 and sp and config and min"
                    ) * 10).toInt()
                    analogOutConfiguration.analogOut2AtMaxSetting = (readConfig(
                        hsApi, equipRef, "analog == 2 and sp and config and max "
                    ) * 10).toInt()
                } else {
                    analogOutConfiguration.analogOut2AtMinSetting = defaultAnalogOutMinSetting
                    analogOutConfiguration.analogOut2AtMaxSetting = defaultAnalogOutMaxSetting * 10
                }
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog3OutputEnable + "\"") == 0.0) {
                analogOutConfiguration.analogOut3Mapping = getHyperSplitAnalogOutMapping(0)
            } else {
                analogOutConfiguration.analogOut3Mapping = getHyperSplitAnalogOutMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog3OutputAssociation + "\"").toInt() + 1
                )

                if ((analogOutConfiguration.analogOut3Mapping.ordinal - 1) != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                    analogOutConfiguration.analogOut3AtMinSetting = (readConfig(
                        hsApi, equipRef, "analog == 3 and sp and config and min"
                    ) * 10).toInt()
                    analogOutConfiguration.analogOut3AtMaxSetting = (readConfig(
                        hsApi, equipRef, "analog == 3 and sp and config and max "
                    ) * 10).toInt()
                } else {
                    analogOutConfiguration.analogOut3AtMinSetting = defaultAnalogOutMinSetting
                    analogOutConfiguration.analogOut3AtMaxSetting = defaultAnalogOutMaxSetting * 10
                }
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog4OutputEnable + "\"") == 0.0) {
                analogOutConfiguration.analogOut4Mapping = getHyperSplitAnalogOutMapping(0)
            } else {
                analogOutConfiguration.analogOut4Mapping = getHyperSplitAnalogOutMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.analog4OutputAssociation + "\"").toInt() + 1
                )

                if ((analogOutConfiguration.analogOut4Mapping.ordinal - 1) != CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                    analogOutConfiguration.analogOut4AtMinSetting = (readConfig(
                        hsApi, equipRef, "analog == 4 and sp and config and min"
                    ) * 10).toInt()
                    analogOutConfiguration.analogOut4AtMaxSetting = (readConfig(
                        hsApi, equipRef, "analog == 4 and sp and config and max "
                    ) * 10).toInt()
                } else {
                    analogOutConfiguration.analogOut4AtMinSetting = defaultAnalogOutMinSetting
                    analogOutConfiguration.analogOut4AtMaxSetting = defaultAnalogOutMaxSetting * 10
                }
            }

            return analogOutConfiguration.build()
        }

        private fun getHyperSplitAnalogOutMapping(intAssociation: Int): HyperSplit.HyperSplitAnalogOutMapping_t {

            if (intAssociation == 0) return HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_DISABLED
            else if (intAssociation == 1) return HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_COOLING
            else if (intAssociation == 2) return HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_LINEAR_FAN
            else if (intAssociation == 3) return HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_HEATING
            else if (intAssociation == 4) return HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_OAO_DAMPER
            else if (intAssociation == 5) return HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_STAGED_FAN

            // This should never happen
            else { return HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_DISABLED }

        }

        /**
         * Function which reads all the Universal In toggle configuration and mapping details
         * @param equipRef
         * @param hsApi
         * @return HypersplitUniversalIn_t
         */
        private fun getUniversalInConfigDetails(hsApi: CCUHsApi, equipRef: String): HyperSplit.HyperSplitUniversalInConfig_t {
            val universalIn = HyperSplit.HyperSplitUniversalInConfig_t.newBuilder()

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn1Enable + "\"") == 0.0) {
                universalIn.universalIn1Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn1Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn1Association + "\"").toInt()
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn2Enable + "\"") == 0.0) {
                universalIn.universalIn2Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn2Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn2Association + "\"").toInt()
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn3Enable + "\"") == 0.0) {
                universalIn.universalIn3Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn3Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn3Association + "\"").toInt()
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn4Enable + "\"") == 0.0) {
                universalIn.universalIn4Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn4Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn4Association + "\"").toInt()
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn5Enable + "\"") == 0.0) {
                universalIn.universalIn5Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn5Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn5Association + "\"").toInt()
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn6Enable + "\"") == 0.0) {
                universalIn.universalIn6Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn6Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn6Association + "\"").toInt()
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn7Enable + "\"") == 0.0) {
                universalIn.universalIn7Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn7Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn7Association + "\"").toInt()
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn8Enable + "\"") == 0.0) {
                universalIn.universalIn8Mapping = getHyperSplitUniversalInMapping(0)
            } else {
                universalIn.universalIn8Mapping = getHyperSplitUniversalInMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.universalIn8Association + "\"").toInt()
                )
            }

            return universalIn.build()
        }

        private fun getHyperSplitUniversalInMapping(intAssociation: Int): HyperSplit.HyperSplitUniversalInMapping_t {
            return when (intAssociation)  {
                0 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_DISABLED
                1, 3, 4, 5, 29, 43, 45, 46, 47, 48, 49 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                2, 53, 54, 55, 58, 59, 64, 73, 74, 85, 91, 92 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                14 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_SAT
                28 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_MAT
                33 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_OAT
                41 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_10
                42 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_20
                44 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_50
                60 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_FILTER_NO
                61 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_FILTER_NC
                82 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CONDENSATE_NO
                84 -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CONDENSATE_NC
                else -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_DISABLED
            }
        }

        /**
         * Function which reads all the Sensor Bus toggle configuration and mapping details
         * @param equipRef
         * @param hsApi
         * @return HypersplitSenorBusMapping_t
         */
        private fun getSensorBusConfigDetails(hsApi: CCUHsApi, equipRef: String): HyperSplit.HyperSplitSensorBusConfig_t {
            val sensorBus = HyperSplit.HyperSplitSensorBusConfig_t.newBuilder()

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.sensorBusAddress0Enable + "\"") == 0.0) {
                sensorBus.sensorBus1Mapping = getHyperSplitSensorBusMapping(0)
            } else {
                sensorBus.sensorBus1Mapping = getHyperSplitSensorBusMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.temperatureSensorBusAdd0 + "\"").toInt() + 1
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.sensorBusAddress1Enable + "\"") == 0.0) {
                sensorBus.sensorBus2Mapping = getHyperSplitSensorBusMapping(0)
            } else {
                sensorBus.sensorBus2Mapping = getHyperSplitSensorBusMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.temperatureSensorBusAdd1 + "\"").toInt() + 1
                )
            }

            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.sensorBusAddress2Enable + "\"") == 0.0) {
                sensorBus.sensorBus3Mapping = getHyperSplitSensorBusMapping(0)
            } else {
                sensorBus.sensorBus3Mapping = getHyperSplitSensorBusMapping(
                    readConfig(hsApi, equipRef, "domainName == \"" + DomainName.temperatureSensorBusAdd2 + "\"").toInt() + 1
                )
            }

            // addr3 has a different association enum; it is either "pressure" (3) or disabled (0)
            if (readConfig(hsApi, equipRef, "domainName == \"" + DomainName.sensorBusAddress3Enable + "\"") == 0.0) {
                sensorBus.sensorBus4Mapping = getHyperSplitSensorBusMapping(0)
            } else {
                sensorBus.sensorBus4Mapping = getHyperSplitSensorBusMapping(4)
            }

            return sensorBus.build()

        }

        private fun getHyperSplitSensorBusMapping(intAssociation: Int): HyperSplit.HyperSplitSenorBusMapping_t {
            return when (intAssociation) {
                0 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_DISABLED
                1 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_SAT
                2 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_MAT
                3 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_OAT
                4 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_PRESSURE
                else -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_DISABLED
            }
        }

        /**
         * Function to read all the generic tuners which are required for Hyperstat to run on standalone mode
         * @param equipRef
         * @return HyperSplitTunersGeneric_t
         */
        private fun getGenericTunerDetails(equipRef: String): HyperSplit.HyperSplitTunersGeneric_t {
            val genericTuners = HyperSplit.HyperSplitTunersGeneric_t.newBuilder()
            genericTuners.unoccupiedSetback = (TunerUtil.readTunerValByQuery(
                "domainName == \"" + DomainName.unoccupiedZoneSetback + "\"", equipRef) * 10).toInt()
            genericTuners.minFanRuntimePostconditioning = (
                    TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.minFanRuntimePostConditioning + "\"", equipRef)).toInt()
            genericTuners.relayActivationHysteresis =
                TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.standaloneRelayActivationHysteresis + "\"", equipRef).toInt()
            genericTuners.analogFanSpeedMultiplier =
                (TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.standaloneAnalogFanSpeedMultiplier + "\"", equipRef) * 10 ).toInt()
            genericTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equipRef).toInt()
            genericTuners.autoAwayZoneSetbackTemp =
                (TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.autoAwaySetback + "\"") * 10).toInt()
            genericTuners.autoAwayTime = TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.autoAwayTime + "\"", equipRef).toInt()
            genericTuners.forcedOccupiedTime =
                TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.forcedOccupiedTime + "\"", equipRef).toInt()
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
         * Function which reads a Tuner point from haystack
         * @param hsApi
         * @param equipRef
         * @param markers
         * @return Double
         */
        private fun readTuner(hsApi: CCUHsApi, equipRef: String, markers: String): Double {
            return TunerUtil.readTunerValByQuery(
                "$markers and equipRef == \"$equipRef\""
            )
        }
        
        var ccuControlMessageTimer :Long = 0
            get() {
                if (field == 0L){
                    ccuControlMessageTimer = System.currentTimeMillis()}
                return field

            }

        // Below method returns query based on DesiredTempMode
        fun getHeatingUserLimitByQuery(query: String, equipRef: String) : String{
            return "schedulable and heating and user and limit and $query and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+"\""
        }

        fun getCoolingUserLimitByQuery(query: String, equipRef: String) : String{
            return "schedulable and cooling and user and limit and $query and roomRef == \""+HSUtil.getZoneIdFromEquipId(equipRef)+"\""
        }

        /**
         * Function to read all the staged fan voltages which are required for Hyperstat Split to run
         * @param equipRef
         * @return HyperSplitStagedFanVoltages_t
         */
        private fun getStagedFanVoltageDetails(equipRef: String): HyperSplit.HyperSplitConfigsCcu_t? {
            val stagedFanVoltages = HyperSplit.HyperSplitConfigsCcu_t.newBuilder()
            val ccuHsApi = CCUHsApi.getInstance()
            val equipRefQuery = "equipRef == \"$equipRef\""

            val coolingStage1Query = "domainName == \"" + DomainName.fanOutCoolingStage1 + "\" and $equipRefQuery"
            val coolingStage2Query = "domainName == \"" + DomainName.fanOutCoolingStage2 + "\" and $equipRefQuery"
            val coolingStage3Query = "domainName == \"" + DomainName.fanOutCoolingStage3 + "\" and $equipRefQuery"
            val heatingStage1Query = "domainName == \"" + DomainName.fanOutHeatingStage1 + "\" and $equipRefQuery"
            val heatingStage2Query = "domainName == \"" + DomainName.fanOutHeatingStage2 + "\" and $equipRefQuery"
            val heatingStage3Query = "domainName == \"" + DomainName.fanOutHeatingStage3 + "\" and $equipRefQuery"
            val aOutAtRecircQuery = "domainName == \"" + DomainName.fanOutRecirculate + "\" and $equipRefQuery"

            if (ccuHsApi.readEntity(coolingStage1Query).isNotEmpty()) {
                stagedFanVoltages.coolingStage1FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(coolingStage1Query)).toInt()
            }
            if (ccuHsApi.readEntity(coolingStage2Query).isNotEmpty()) {
                stagedFanVoltages.coolingStage2FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(coolingStage2Query)).toInt()
            }
            if (ccuHsApi.readEntity(coolingStage3Query).isNotEmpty()) {
                stagedFanVoltages.coolingStage3FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(coolingStage3Query)).toInt()
            }
            if (ccuHsApi.readEntity(heatingStage1Query).isNotEmpty()) {
                stagedFanVoltages.heatingStage1FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(heatingStage1Query)).toInt()
            }
            if (ccuHsApi.readEntity(heatingStage2Query).isNotEmpty()) {
                stagedFanVoltages.heatingStage2FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(heatingStage2Query)).toInt()
            }
            if (ccuHsApi.readEntity(heatingStage3Query).isNotEmpty()) {
                stagedFanVoltages.heatingStage3FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(heatingStage3Query)).toInt()
            }
            if (ccuHsApi.readEntity(aOutAtRecircQuery).isNotEmpty()) {
                stagedFanVoltages.analogoutAtRecFanAnalogVoltage = (ccuHsApi.readPointPriorityValByQuery(aOutAtRecircQuery) * 10).toInt()
            }

            return stagedFanVoltages.build()
        }

        /**
         * Function to read all the linear fan speeds which are required for Hyperstat Split to run
         * @param equipRef
         * @return HyperSplitLinearFanSpeeds_t
         */
        fun getLinearFanSpeedDetails(equipRef: String): HyperSplit.HypersplitLinearFanSpeeds_t? {
            val linearFanSpeedBuilder = HyperSplit.HypersplitLinearFanSpeeds_t.newBuilder()
            val ccuHsApi = CCUHsApi.getInstance()
            val equipRefQuery = "equipRef == \"$equipRef\""

            val fanSpeedLevels = listOf("low", "medium", "high")

            for (fanSpeed in fanSpeedLevels) {
                for (analog in listOf(1, 2, 3, 4)) {
                    val query = "analog == $analog and $fanSpeed and sp and fan and $equipRefQuery"
                    if (ccuHsApi.readEntity(query).isNotEmpty()&& getAnalogOutMapping(ccuHsApi,equipRef,analog)
                        == CpuEconAnalogOutAssociation.MODULATING_FAN_SPEED.ordinal) {
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
         * @return HypersplitStagedFanSpeeds_t
         */
        fun getStagedFanSpeedDetails(equipRef: String): HyperSplit.HypersplitStagedFanSpeeds_t? {
            val stagedFanSpeedBuilder = HyperSplit.HypersplitStagedFanSpeeds_t.newBuilder()
            val ccuHsApi = CCUHsApi.getInstance()
            val equipRefQuery = "equipRef == \"$equipRef\""

            val fanSpeedLevels = listOf("low", "medium", "high")

            for (fanSpeed in fanSpeedLevels) {
                for (analog in listOf(1, 2, 3, 4)) {
                    val query = "analog == $analog and $fanSpeed and sp and fan and $equipRefQuery"
                    if (ccuHsApi.readEntity(query).isNotEmpty() && getAnalogOutMapping(ccuHsApi,equipRef,analog)
                        == CpuEconAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
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

        private fun getAnalogOutMapping(
            ccuHsApi: CCUHsApi,
            equipRef: String,
            analog: Int
        ): Any {
            val domainName = when (analog) {
                1 -> DomainName.analog1OutputAssociation
                2 -> DomainName.analog2OutputAssociation
                3 -> DomainName.analog3OutputAssociation
                4 -> DomainName.analog4OutputAssociation
                else -> DomainName.analog1OutputAssociation
            }

            return readConfig(
                ccuHsApi,
                equipRef,
                "domainName == \"$domainName\""
            ).toInt()
        }

    }
    
}