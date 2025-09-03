package a75f.io.device.mesh.hypersplit

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.HSUtil
import a75f.io.device.HyperSplit
import a75f.io.device.HyperSplit.HyperSplitAnalogOutConfig_t
import a75f.io.device.HyperSplit.HyperSplitSettingsMessage4_t
import a75f.io.device.mesh.Base64Util
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.equips.unitVentilator.Pipe2UVEquip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.domain.equips.unitVentilator.UnitVentilatorEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.CpuSensorBusType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.UniversalInputs
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuRelayType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
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
         * return HyperSplitSettingsMessage2_t
         */
        fun getSetting2Message(equipRef: String): HyperSplit.HyperSplitSettingsMessage2_t {
            val settings2 = HyperSplit.HyperSplitSettingsMessage2_t.newBuilder()
            val splitEquip = Domain.getDomainEquip(equipRef) as HyperStatSplitEquip
            settings2.enableForceOccupied = splitEquip.autoForceOccupied.isEnabled()
            settings2.enableAutoAway = splitEquip.autoAway.isEnabled()
            settings2.hyperSplitRelayConfig = getRelayConfigDetails(splitEquip)
            settings2.hyperSplitAnalogOutConfig = getAnalogConfigDetails(splitEquip)
            settings2.hyperSplitUniversalInConfig = getUniversalInConfigDetails(splitEquip)
            settings2.hyperSplitSensorBusConfig = getSensorBusConfigDetails(splitEquip)
            settings2.zoneCO2Target = splitEquip.co2Target.readDefaultVal().toInt()
            settings2.zoneCO2Threshold = splitEquip.co2Threshold.readDefaultVal().toInt()
            settings2.zoneCO2DamperOpeningRate = splitEquip.co2DamperOpeningRate.readDefaultVal().toInt()
            settings2.proportionalConstant = (TunerUtil.getProportionalGain(equipRef) * 100).toInt()
            settings2.integralConstant = (TunerUtil.getIntegralGain(equipRef) * 100).toInt()
            settings2.proportionalTemperatureRange = (TunerUtil.getProportionalSpread(equipRef) * 10).toInt()
            settings2.integrationTime = TunerUtil.getIntegralTimeout(equipRef).toInt()

            when (splitEquip) {
                is HsSplitCpuEquip-> {
                    settings2.profile = HyperSplit.HyperSplitProfiles_t.HYPERSPLIT_PROFILE_CONVENTIONAL_PACKAGE_UNIT_ECONOMIZER
                }
                is Pipe4UVEquip-> {
                    settings2.profile = HyperSplit.HyperSplitProfiles_t.HYPERSPLIT_PROFILE_4_PIPE_UNIT_VENTILATOR
                }
                is Pipe2UVEquip -> {
                    settings2.profile = HyperSplit.HyperSplitProfiles_t.HYPERSPLIT_PROFILE_2_PIPE_UNIT_VENTILATOR
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
            val hsEquip = Domain.getDomainEquip(equipRef) as HyperStatSplitEquip
            val settings3 = HyperSplit.HyperSplitSettingsMessage3_t.newBuilder()
            val equip = getEquipDetails(nodeAddress)

            val ecoTunersBuilder = HyperSplit.HyperSplitTunersEco_t.newBuilder()
            ecoTunersBuilder.setEconomizingToMainCoolingLoopMap(getEconomizingToMainCoolingLoopMap(equipRef))
            ecoTunersBuilder.setEconomizingMinTemp(getEconomizingMinTemp(equipRef))
            ecoTunersBuilder.setEconomizingMaxTemp(getEconomizingMaxTemp(equipRef))
            ecoTunersBuilder.setEconomizingMinHumidity(getEconomizingMinHumidity(equipRef))
            ecoTunersBuilder.setEconomizingMaxHumidity(getEconomizingMaxHumidity(equipRef))
            ecoTunersBuilder.setEconomizingDryBulbThreshold(getEconomizingDryBulbThreshold(equipRef))
            ecoTunersBuilder.setEnthalpyDuctCompensationOffset(getEnthalpyDuctCompensationOffset(equipRef))
            ecoTunersBuilder.setDuctCompensationOffset(getDuctTemperatureOffset(equipRef))
            ecoTunersBuilder.setOaoDamperMatTarget(getOaoDamperMatTarget(equipRef))
            ecoTunersBuilder.setOaoDamperMatMin(getOaoDamperMatMin(equipRef))
            ecoTunersBuilder.setOutsideDamperMinOpen(0)
            ecoTunersBuilder.setAoutFanEconomizer(getAnalogOutFanDuringEconomizer(hsApi, equipRef))
            settings3.genertiTuners = getGenericTunerDetails(hsEquip)

            when (equip.profile) {
                ProfileType.HYPERSTATSPLIT_CPU.name -> {
                    settings3.hyperStatConfigsCpu = getStagedFanVoltageDetails(equipRef)
                    ecoTunersBuilder.setExhaustFanStage1Threshold(hsEquip.exhaustFanStage1Threshold.readDefaultVal().toInt())
                    ecoTunersBuilder.setExhaustFanStage2Threshold(hsEquip.exhaustFanStage2Threshold.readDefaultVal().toInt())
                    ecoTunersBuilder.setExhaustFanHysteresis(hsEquip.exhaustFanHysteresis.readDefaultVal().toInt())
                }
                ProfileType.HYPERSTATSPLIT_4PIPE_UV.name -> {
                    val fcuTuners = HyperSplit.HyperSplitTunersFcu_t.newBuilder()
                    fcuTuners.setAuxHeating1Activate(hsEquip.auxHeating1Activate.readPriorityVal().toInt())
                    fcuTuners.setAuxHeating2Activate(hsEquip.auxHeating2Activate.readPriorityVal().toInt())
                    fcuTuners.apply {
                        setAnalogoutAtRecFanAnalogVoltage(hsEquip.fanOutRecirculate.readPriorityVal().toInt() * 10)
                        setAuxHeating1Activate(hsEquip.auxHeating1Activate.readPriorityVal().toInt())
                        setAuxHeating2Activate(hsEquip.auxHeating2Activate.readPriorityVal().toInt())
                    }
                    settings3.fcuTuners = fcuTuners.build()
                }
                ProfileType.HYPERSTATSPLIT_2PIPE_UV.name -> {
                    val fcuTuners = HyperSplit.HyperSplitTunersFcu_t.newBuilder()
                    fcuTuners.apply {
                        analogoutAtRecFanAnalogVoltage = hsEquip.fanOutRecirculate.readPriorityVal().toInt() * 10
                        auxHeating1Activate = hsEquip.auxHeating1Activate.readPriorityVal().toInt()
                        auxHeating2Activate = hsEquip.auxHeating2Activate.readPriorityVal().toInt()
                        twoPipeHeatingThreshold = (hsEquip as Pipe2UVEquip).hyperstatPipe2FancoilHeatingThreshold.readPriorityVal().toInt()
                        twoPipeCoolingThreshold = hsEquip.hyperstatPipe2FancoilCoolingThreshold.readPriorityVal().toInt()
                        waterValueSamplingOnTime = hsEquip.waterValveSamplingOnTime.readPriorityVal().toInt()
                        watreValueSamplingWaitTime = hsEquip.waterValveSamplingWaitTime.readPriorityVal().toInt()
                        waterValveSamplingDuringLoopDeadbandOnTime = hsEquip.waterValveSamplingLoopDeadbandOnTime.readPriorityVal().toInt()
                        waterValveSamplingDuringLoopDeadbandWaitTime = hsEquip.waterValveSamplingLoopDeadbandWaitTime.readPriorityVal().toInt()
                    }
                    settings3.fcuTuners = fcuTuners.build()
                }
            }

            settings3.setEcoTuners(ecoTunersBuilder.build())
            return settings3.build()
        }

        fun getSetting4Message(equipRef: String): HyperSplitSettingsMessage4_t {

            val settings4 = HyperSplitSettingsMessage4_t.newBuilder()
            val hsEquip = Domain.getDomainEquip(equipRef) as HyperStatSplitEquip
            settings4.setOutsideDamperMinOpenDuringRecirculation(hsEquip.outsideDamperMinOpenDuringRecirculation.readDefaultVal().toInt())
            settings4.setOutsideDamperMinOpenDuringConditioning(hsEquip.outsideDamperMinOpenDuringConditioning.readDefaultVal().toInt())
            settings4.setOutsideDamperMinOpenDuringFanLow(hsEquip.outsideDamperMinOpenDuringFanLow.readDefaultVal().toInt())
            settings4.setOutsideDamperMinOpenDuringFanMedium(hsEquip.outsideDamperMinOpenDuringFanMedium.readDefaultVal().toInt())
            settings4.setOutsideDamperMinOpenDuringFanHigh(hsEquip.outsideDamperMinOpenDuringFanHigh.readDefaultVal().toInt())
            settings4.setStageUpTimer(readTunerByDomainName(DomainName.hyperstatStageUpTimerCounter, equipRef).toInt())
            settings4.setStageDownTimer(readTunerByDomainName(DomainName.hyperstatStageDownTimerCounter, equipRef).toInt())
            when(hsEquip) {
                is Pipe4UVEquip -> {
                    settings4.setSaTemperingSetpoint(readTunerByDomainName(DomainName.saTemperingSetpoint, equipRef).toInt() * 10)
                    settings4.setFaceBypassDamperRelayActivationHysteresis(
                        readTunerByDomainName(DomainName.faceBypassDamperRelayActivationHysteresis, equipRef).toInt())
                    settings4.setControlVia(
                        if (hsEquip.controlVia.readDefaultVal().toInt() == 0) {
                            HyperSplit.HyperSplitControlVia_t.HYPERSPLIT_CONTROL_VIA_F_AND_B_DAMPER
                        } else {
                            HyperSplit.HyperSplitControlVia_t.HYPERSPLIT_CONTROL_VIA_FULLY_MODULATING_VALVE
                        }
                    )
                    settings4.setEnableSaTempering(hsEquip.enableSaTemperingControl.isEnabled())
                    settings4.setSaTemperingIntegrationTime(
                        readTunerByDomainName(DomainName.saTemperingTemperatureIntegralTime, equipRef).toInt()
                    )
                    settings4.setSaTemperingIntegralConstant(
                        (readTunerByDomainName(DomainName.saTemperingIntegralKFactor, equipRef) * 100).toInt()
                    )
                    settings4.setSaTemperingProportionalTemperatureRange(
                        readTunerByDomainName(DomainName.saTemperingTemperatureProportionalRange, equipRef).toInt()
                    )
                    settings4.setSaTemperingProportionalConstant(
                        (readTunerByDomainName(DomainName.saTemperingProportionalKFactor, equipRef) * 100).toInt()
                    )
                }
            }
            return settings4.build()
        }

        private fun getEconomizingToMainCoolingLoopMap(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneEconomizingToMainCoolingLoopMap + "\""
            ).toInt()
        }

        private fun getEconomizingMinTemp(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneEconomizingMinTemperature + "\""
            ).toInt()
        }

        private fun getEconomizingMaxTemp(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneEconomizingMaxTemperature + "\""
            ).toInt()

        }

        private fun getEconomizingMinHumidity(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneEconomizingMinHumidity + "\""
            ).toInt()
        }

        private fun getEconomizingMaxHumidity(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneEconomizingMaxHumidity + "\""
            ).toInt()
        }

        private fun getEconomizingDryBulbThreshold(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneEconomizingDryBulbThreshold + "\""
            ).toInt()
        }

        private fun getEnthalpyDuctCompensationOffset(equipRef: String): Int {
            return Math.round(readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneEnthalpyDuctCompensationOffset + "\""
            ).toFloat())
        }

        private fun getDuctTemperatureOffset(equipRef: String): Int {
            return Math.round(readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneDuctTemperatureOffset + "\""
            ).toFloat())
        }


        private fun getOaoDamperMatTarget(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneOutsideDamperMixedAirTarget + "\""
            ).toInt()
        }

        private fun getOaoDamperMatMin(equipRef: String): Int {
            return readTuner(
                equipRef,
                "domainName == \"" + DomainName.standaloneOutsideDamperMixedAirMinimum + "\""
            ).toInt()
        }

        private fun getAnalogOutFanDuringEconomizer(hsApi: CCUHsApi, equipRef: String): Int {
            return 10 * readConfig(hsApi, equipRef, "domainName == \"" + DomainName.fanOutEconomizer + "\"").toInt()
        }

        /**
         * Function which collects the equip details from node address
         * @param nodeAddress
         * return Equip
         */
        private fun getEquipDetails(nodeAddress: Int): Equip {
            return Equip.Builder()
                .setHashMap(CCUHsApi.getInstance().readEntity("equip and group == \"$nodeAddress\"")).build()
        }

        /**
         * Function which reads all the Relay toggle configuration and mapping details
         * @return HyperstatRelay_t
         */
        private fun getRelayConfigDetails(equip: HyperStatSplitEquip): HyperSplit.HyperSplitRelayConfig_t {
            val relayConfiguration = HyperSplit.HyperSplitRelayConfig_t.newBuilder()
            val relays = listOf(
                Pair(equip.relay1OutputEnable.isEnabled(), equip.relay1OutputAssociation),
                Pair(equip.relay2OutputEnable.isEnabled(), equip.relay2OutputAssociation),
                Pair(equip.relay3OutputEnable.isEnabled(), equip.relay3OutputAssociation),
                Pair(equip.relay4OutputEnable.isEnabled(), equip.relay4OutputAssociation),
                Pair(equip.relay5OutputEnable.isEnabled(), equip.relay5OutputAssociation),
                Pair(equip.relay6OutputEnable.isEnabled(), equip.relay6OutputAssociation),
                Pair(equip.relay7OutputEnable.isEnabled(), equip.relay7OutputAssociation),
                Pair(equip.relay8OutputEnable.isEnabled(), equip.relay8OutputAssociation)
            )

            fun disableIt() = HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED
            relays.forEachIndexed { index, (enabled, association) ->
                when (index) {
                    0 -> relayConfiguration.relay1Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()

                    1 -> relayConfiguration.relay2Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()

                    2 -> relayConfiguration.relay3Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()

                    3 -> relayConfiguration.relay4Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()

                    4 -> relayConfiguration.relay5Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()

                    5 -> relayConfiguration.relay6Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()

                    6 -> relayConfiguration.relay7Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()

                    7 -> relayConfiguration.relay8Mapping =
                        if (enabled) getRelayMapping(association, equip) else disableIt()
                }
            }
            return relayConfiguration.build()
        }

        private fun getRelayMapping(
            point: Point,
            equip: HyperStatSplitEquip
        ): HyperSplit.HyperSplitRelayMapping_t {
            val association = point.readDefaultVal().toInt()

            fun getCpuRelayMapping(): HyperSplit.HyperSplitRelayMapping_t {
                return when (CpuRelayType.values()[association]) {
                    CpuRelayType.COOLING_STAGE1 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_COOLING_STAGE_1
                    CpuRelayType.COOLING_STAGE2 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_COOLING_STAGE_2
                    CpuRelayType.COOLING_STAGE3 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_COOLING_STAGE_3
                    CpuRelayType.HEATING_STAGE1 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HEATING_STAGE_1
                    CpuRelayType.HEATING_STAGE2 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HEATING_STAGE_2
                    CpuRelayType.HEATING_STAGE3 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HEATING_STAGE_3
                    CpuRelayType.FAN_LOW_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_LOW_SPEED
                    CpuRelayType.FAN_MEDIUM_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_MEDIUM_SPEED
                    CpuRelayType.FAN_HIGH_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_HIGH_SPEED
                    CpuRelayType.FAN_ENABLED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_ENABLE
                    CpuRelayType.OCCUPIED_ENABLED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_OCCUPIED_ENABLE
                    CpuRelayType.HUMIDIFIER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HUMIDIFIER
                    CpuRelayType.DEHUMIDIFIER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DEHUMIDIFIER
                    CpuRelayType.EX_FAN_STAGE1 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_EXAUST_1
                    CpuRelayType.EX_FAN_STAGE2 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_EXAUST_2
                    CpuRelayType.DCV_DAMPER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_DCV_DAMPER
                    CpuRelayType.EXTERNALLY_MAPPED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED
                    CpuRelayType.COMPRESSOR_STAGE1 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_COMPRESSOR_STAGE_1
                    CpuRelayType.COMPRESSOR_STAGE2 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_COMPRESSOR_STAGE_2
                    CpuRelayType.COMPRESSOR_STAGE3 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_COMPRESSOR_STAGE_3
                    CpuRelayType.CHANGE_OVER_O_COOLING -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_O_ENERGISE_IN_COOLING
                    CpuRelayType.CHANGE_OVER_B_HEATING -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_B_ENERGISE_IN_HEATING
                    CpuRelayType.AUX_HEATING_STAGE1 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_AUX_HEATING_1
                    CpuRelayType.AUX_HEATING_STAGE2 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_AUX_HEATING_2
                    else -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED
                }
            }

            fun getPipe4UVRelayMapping(): HyperSplit.HyperSplitRelayMapping_t {
                return when (Pipe4UVRelayControls.values()[association]) {
                    Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_LOW_SPEED_VENTILATION
                    Pipe4UVRelayControls.FAN_LOW_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_LOW_SPEED
                    Pipe4UVRelayControls.FAN_MEDIUM_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_MEDIUM_SPEED
                    Pipe4UVRelayControls.FAN_HIGH_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_HIGH_SPEED
                    Pipe4UVRelayControls.COOLING_WATER_VALVE -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_COOLING_VALVE
                    Pipe4UVRelayControls.HEATING_WATER_VALVE -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HEATING_VALVE
                    Pipe4UVRelayControls.AUX_HEATING_STAGE1 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_AUX_HEATING_1
                    Pipe4UVRelayControls.AUX_HEATING_STAGE2 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_AUX_HEATING_2
                    Pipe4UVRelayControls.FAN_ENABLED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_ENABLE
                    Pipe4UVRelayControls.OCCUPIED_ENABLED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_OCCUPIED_ENABLE
                    Pipe4UVRelayControls.FACE_BYPASS_DAMPER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_F_AND_B_DAMPER
                    Pipe4UVRelayControls.DCV_DAMPER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_DCV_DAMPER
                    Pipe4UVRelayControls.HUMIDIFIER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HUMIDIFIER
                    Pipe4UVRelayControls.DEHUMIDIFIER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DEHUMIDIFIER
                    Pipe4UVRelayControls.EXTERNALLY_MAPPED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED
                }
            }

            fun getPipe2UVRelayMapping(): HyperSplit.HyperSplitRelayMapping_t {
                return when (Pipe2UVRelayControls.values()[association]) {
                    Pipe2UVRelayControls.FAN_LOW_SPEED_VENTILATION -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_LOW_SPEED_VENTILATION
                    Pipe2UVRelayControls.FAN_LOW_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_LOW_SPEED
                    Pipe2UVRelayControls.FAN_MEDIUM_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_MEDIUM_SPEED
                    Pipe2UVRelayControls.FAN_HIGH_SPEED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_HIGH_SPEED
                    Pipe2UVRelayControls.WATER_VALVE -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_WATER_VALVE
                    Pipe2UVRelayControls.AUX_HEATING_STAGE1 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_AUX_HEATING_1
                    Pipe2UVRelayControls.AUX_HEATING_STAGE2 -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_AUX_HEATING_2
                    Pipe2UVRelayControls.FACE_BYPASS_DAMPER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_F_AND_B_DAMPER
                    Pipe2UVRelayControls.DCV_DAMPER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPILT_RELAY_DCV_DAMPER
                    Pipe2UVRelayControls.FAN_ENABLED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_FAN_ENABLE
                    Pipe2UVRelayControls.OCCUPIED_ENABLED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_OCCUPIED_ENABLE
                    Pipe2UVRelayControls.HUMIDIFIER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_HUMIDIFIER
                    Pipe2UVRelayControls.DEHUMIDIFIER -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DEHUMIDIFIER
                    Pipe2UVRelayControls.EXTERNALLY_MAPPED -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED
                }
            }
            return when (equip) {
                is HsSplitCpuEquip -> getCpuRelayMapping()
                is Pipe4UVEquip -> getPipe4UVRelayMapping()
                is Pipe2UVEquip -> getPipe2UVRelayMapping()
                else -> HyperSplit.HyperSplitRelayMapping_t.HYPERSPLIT_RELAY_DISABLED
            }
        }

        private fun getAnalogConfigDetails(equip: HyperStatSplitEquip): HyperSplitAnalogOutConfig_t {
            val config = HyperSplitAnalogOutConfig_t.newBuilder()
            val analogOuts = listOf(
                Pair(equip.analog1OutputEnable.isEnabled(), equip.analog1OutputAssociation),
                Pair(equip.analog2OutputEnable.isEnabled(), equip.analog2OutputAssociation),
                Pair(equip.analog3OutputEnable.isEnabled(), equip.analog3OutputAssociation),
                Pair(equip.analog4OutputEnable.isEnabled(), equip.analog4OutputAssociation)
            )

            fun disableIt() = HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_DISABLED

            fun minMaxValue(equipRef: String, analog: Int): Pair<Int, Int> {
                return Pair(
                    CCUHsApi.getInstance()
                        .readDefaultVal("min and analog == $analog and equipRef == \"$equipRef\"")
                        .toInt() * 10,
                    CCUHsApi.getInstance()
                        .readDefaultVal("max and analog == $analog and equipRef == \"$equipRef\"")
                        .toInt() * 10
                )
            }

            analogOuts.forEachIndexed { index, (enabled, association) ->
                when (index) {
                    0 -> {
                        val mapping = if (enabled) getHyperSplitAnalogOutMapping(
                            association,
                            equip
                        ) else disableIt()
                        val minMax = minMaxValue(equip.equipRef, 1)
                        config.setAnalogOut1Mapping(mapping)
                        config.setAnalogOut1AtMinSetting(minMax.first)
                        config.setAnalogOut1AtMaxSetting(minMax.second)
                    }

                    1 -> {
                        val mapping = if (enabled) getHyperSplitAnalogOutMapping(
                            association,
                            equip
                        ) else disableIt()
                        val minMax = minMaxValue(equip.equipRef, 2)
                        config.setAnalogOut2Mapping(mapping)
                        config.setAnalogOut2AtMinSetting(minMax.first)
                        config.setAnalogOut2AtMaxSetting(minMax.second)
                    }

                    2 -> {
                        val mapping = if (enabled) getHyperSplitAnalogOutMapping(
                            association,
                            equip
                        ) else disableIt()
                        val minMax = minMaxValue(equip.equipRef, 3)
                        config.setAnalogOut3Mapping(mapping)
                        config.setAnalogOut3AtMinSetting(minMax.first)
                        config.setAnalogOut3AtMaxSetting(minMax.second)
                    }

                    3 -> {
                        val mapping = if (enabled) getHyperSplitAnalogOutMapping(
                            association,
                            equip
                        ) else disableIt()
                        val minMax = minMaxValue(equip.equipRef, 4)
                        config.setAnalogOut4Mapping(mapping)
                        config.setAnalogOut4AtMinSetting(minMax.first)
                        config.setAnalogOut4AtMaxSetting(minMax.second)
                    }

                    else -> {}
                }
            }
            return config.build()
        }

        private fun getHyperSplitAnalogOutMapping(point: Point, equip: HyperStatSplitEquip): HyperSplit.HyperSplitAnalogOutMapping_t {
            val association = point.readDefaultVal().toInt()
            fun getCpuAnalogOutMapping(): HyperSplit.HyperSplitAnalogOutMapping_t {
                return when (CpuAnalogControlType.values()[association]) {
                    CpuAnalogControlType.COOLING -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_COOLING
                    CpuAnalogControlType.LINEAR_FAN -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_LINEAR_FAN
                    CpuAnalogControlType.HEATING -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_HEATING
                    CpuAnalogControlType.OAO_DAMPER -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_OAO_DAMPER
                    CpuAnalogControlType.STAGED_FAN -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_STAGED_FAN
                    CpuAnalogControlType.RETURN_DAMPER -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_RETURN_DAMPER
                    CpuAnalogControlType.EXTERNALLY_MAPPED -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_DISABLED
                    CpuAnalogControlType.COMPRESSOR_SPEED -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPILT_AOUT_COMPRESSOR_SPEED
                    CpuAnalogControlType.DCV_MODULATING_DAMPER -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPILT_AOUT_DCV_DAMPER
                }
            }
            fun getPipe4UVAnalogOutMapping(): HyperSplit.HyperSplitAnalogOutMapping_t {
                return when (Pipe4UvAnalogOutControls.values()[association]) {
                    Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_HEATING_MODULATING_VALVE
                    Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_COOLING_MODULATING_VALVE
                    Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_F_AND_B_DAMPER
                    Pipe4UvAnalogOutControls.FAN_SPEED -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_LINEAR_FAN
                    Pipe4UvAnalogOutControls.OAO_DAMPER -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_OAO_DAMPER
                    Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPILT_AOUT_DCV_DAMPER
                    Pipe4UvAnalogOutControls.EXTERNALLY_MAPPED -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_DISABLED
                }
            }

            fun getPipe2UVAnalogOutMapping(): HyperSplit.HyperSplitAnalogOutMapping_t {
                return when (Pipe2UvAnalogOutControls.values()[association]) {
                    Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_WATER_VALVE
                    Pipe2UvAnalogOutControls.FACE_DAMPER_VALVE -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_F_AND_B_DAMPER
                    Pipe2UvAnalogOutControls.FAN_SPEED -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_LINEAR_FAN
                    Pipe2UvAnalogOutControls.DCV_MODULATING_DAMPER -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPILT_AOUT_DCV_DAMPER
                    Pipe2UvAnalogOutControls.OAO_DAMPER -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_OAO_DAMPER
                    Pipe2UvAnalogOutControls.EXTERNALLY_MAPPED -> HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_DISABLED
                }
            }

            return when(equip) {
                is HsSplitCpuEquip -> getCpuAnalogOutMapping()
                is Pipe4UVEquip -> getPipe4UVAnalogOutMapping()
                is Pipe2UVEquip -> getPipe2UVAnalogOutMapping()
                else -> { HyperSplit.HyperSplitAnalogOutMapping_t.HYPERSPLIT_AOUT_DISABLED }
            }
        }
        /**
         * Function which reads all the Universal In toggle configuration and mapping details
         * @return HypersplitUniversalIn_t
         */
        private fun getUniversalInConfigDetails(equip: HyperStatSplitEquip): HyperSplit.HyperSplitUniversalInConfig_t {
            val universalIn = HyperSplit.HyperSplitUniversalInConfig_t.newBuilder()
            fun disableIt() = HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_DISABLED
            listOf(
                Pair(equip.universalIn1Enable.isEnabled(), equip.universalIn1Association),
                Pair(equip.universalIn2Enable.isEnabled(), equip.universalIn2Association),
                Pair(equip.universalIn3Enable.isEnabled(), equip.universalIn3Association),
                Pair(equip.universalIn4Enable.isEnabled(), equip.universalIn4Association),
                Pair(equip.universalIn5Enable.isEnabled(), equip.universalIn5Association),
                Pair(equip.universalIn6Enable.isEnabled(), equip.universalIn6Association),
                Pair(equip.universalIn7Enable.isEnabled(), equip.universalIn7Association),
                Pair(equip.universalIn8Enable.isEnabled(), equip.universalIn8Association)
            ).forEachIndexed { index, (enabled, association) ->
                when (index) {
                    0 -> {
                        universalIn.universalIn1Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    1 -> {
                        universalIn.universalIn2Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    2 -> {
                        universalIn.universalIn3Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    3 -> {
                        universalIn.universalIn4Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    4 -> {
                        universalIn.universalIn5Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    5 -> {
                        universalIn.universalIn6Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    6 -> {
                        universalIn.universalIn7Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    7 -> {
                        universalIn.universalIn8Mapping =
                            if (enabled) getSplitUniversalMapping(
                                association.readDefaultVal().toInt()
                            ) else disableIt()
                    }
                    else -> {}
                }

            }
            return universalIn.build()
        }

        private fun getSplitUniversalMapping(intAssociation: Int): HyperSplit.HyperSplitUniversalInMapping_t {
            return when (intAssociation)  {
                UniversalInputs.VOLTAGE_INPUT.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.BUILDING_STATIC_PRESSURE1.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.BUILDING_STATIC_PRESSURE2.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.BUILDING_STATIC_PRESSURE10.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.OUTSIDE_AIR_DAMPER_FEEDBACK.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.CURRENT_TX_30.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.CURRENT_TX_60.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.CURRENT_TX_120.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE
                UniversalInputs.CURRENT_TX_200.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_VOLTAGE

                UniversalInputs.THERMISTOR_INPUT.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.DISCHARGE_FAN_AM_STATUS.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.DISCHARGE_FAN_RUN_STATUS.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.DISCHARGE_FAN_TRIP_STATUS.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.EXHAUST_FAN_RUN_STATUS.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.EXHAUST_FAN_TRIP_STATUS.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.FIRE_ALARM_STATUS.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE // FIRE_ALARM_STATUS_NO
                UniversalInputs.FIRE_ALARM_STATUS_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.HIGH_DIFFERENTIAL_PRESSURE_SWITCH.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.LOW_DIFFERENTIAL_PRESSURE_SWITCH.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.CHILLED_WATER_SUPPLY_TEMPERATURE.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE
                UniversalInputs.HOT_WATER_SUPPLY_TEMPERATURE.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UNI_GENERIC_RESISTANCE

                UniversalInputs.EMERGENCY_SHUTOFF_NO.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_EMERGENCY_SHUT_OFF_NO
                UniversalInputs.EMERGENCY_SHUTOFF_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_EMERGENCY_SHUT_OFF_NC

                UniversalInputs.GENERIC_ALARM_NO.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_GENERIC_FAULT_NO
                UniversalInputs.GENERIC_ALARM_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_GENERIC_FAULT_NC

                UniversalInputs.SUPPLY_AIR_TEMPERATURE.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_SAT
                UniversalInputs.MIXED_AIR_TEMPERATURE.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_MAT
                UniversalInputs.OUTSIDE_AIR_TEMPERATURE.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_OAT

                UniversalInputs.DUCT_STATIC_PRESSURE1_1.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_PRESSURE_0_1
                UniversalInputs.DUCT_STATIC_PRESSURE1_2.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_PRESSURE_0_2
                UniversalInputs.DUCT_STATIC_PRESSURE1_10.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_PRESSURE_0_10

                UniversalInputs.CURRENT_TX_10.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_10
                UniversalInputs.CURRENT_TX_20.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_20
                UniversalInputs.CURRENT_TX_50.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_50
                UniversalInputs.CURRENT_TX_100.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_100
                UniversalInputs.CURRENT_TX_150.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CURRENT_0_150

                UniversalInputs.FILTER_STATUS_NO.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_FILTER_NO
                UniversalInputs.FILTER_STATUS_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_FILTER_NC

                UniversalInputs.CONDENSATE_STATUS_NO.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CONDENSATE_NO
                UniversalInputs.CONDENSATE_STATUS_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_CONDENSATE_NC
                UniversalInputs.RUN_FAN_STATUS_NO.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_RUN_FAN_STATUS_NO
                UniversalInputs.RUN_FAN_STATUS_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_RUN_FAN_STATUS_NC

                UniversalInputs.DOOR_WINDOW_SENSOR_NO.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_NON_TITLE24_DOOR_WINDOW_NO
                UniversalInputs.DOOR_WINDOW_SENSOR_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_NON_TITLE24_DOOR_WINDOW_NC

                UniversalInputs.DOOR_WINDOW_SENSOR_NO_TITLE_24.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_TITLE24_DOOR_WINDOW_NO
                UniversalInputs.DOOR_WINDOW_SENSOR_TITLE24_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_TITLE24_DOOR_WINDOW_NC

                UniversalInputs.DOOR_WINDOW_SENSOR.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_NON_TITLE24_AI_DOOR_WINDOW // AI
                UniversalInputs.DOOR_WINDOW_SENSOR_TITLE24.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_TITLE24_AI_DOOR_WINDOW // AI

                UniversalInputs.KEYCARD_SENSOR_NO.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_KEY_CARD_NO
                UniversalInputs.KEYCARD_SENSOR_NC.ordinal -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_KEY_CARD_NC

                else -> HyperSplit.HyperSplitUniversalInMapping_t.HYPERSPLIT_UIN_DISABLED

            }
        }

        /**
         * Function which reads all the Sensor Bus toggle configuration and mapping details
         * @return HypersplitSenorBusMapping_t
         */
        private fun getSensorBusConfigDetails(equip: HyperStatSplitEquip): HyperSplit.HyperSplitSensorBusConfig_t {
            val sensorBus = HyperSplit.HyperSplitSensorBusConfig_t.newBuilder()

            fun getHyperSplitSensorBusTempMapping(intAssociation: Int): HyperSplit.HyperSplitSenorBusMapping_t {
                return when (intAssociation) {
                    0 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_DISABLED
                    CpuSensorBusType.SUPPLY_AIR.ordinal + 1 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_SAT
                    CpuSensorBusType.MIXED_AIR.ordinal + 1 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_MAT
                    CpuSensorBusType.OUTSIDE_AIR.ordinal + 1 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_OAT
                    else -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_DISABLED
                }
            }

            fun getHyperSplitSensorBusPressureMapping(intAssociation: Int): HyperSplit.HyperSplitSenorBusMapping_t {
                return when (intAssociation) {
                    0 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_DISABLED
                    1 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_PRESSURE
                    2 -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_FILTER_MONITOR
                    else -> HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_DISABLED
                }
            }

            fun disableIt() = HyperSplit.HyperSplitSenorBusMapping_t.HYPERSPLIT_SBUS_DISABLED

            if (equip.sensorBusAddress0Enable.isEnabled()) {
                sensorBus.sensorBus1Mapping = getHyperSplitSensorBusTempMapping(
                    equip.temperatureSensorBusAdd0.readDefaultVal().toInt() + 1
                )
            } else if (equip.sensorBusPressureEnable.isEnabled()) {
                sensorBus.sensorBus1Mapping = getHyperSplitSensorBusPressureMapping(
                    equip.pressureSensorBusAdd0.readDefaultVal().toInt()
                )
            } else {
                sensorBus.sensorBus1Mapping = disableIt()
            }

            if (equip.sensorBusAddress1Enable.isEnabled()) {
                sensorBus.sensorBus2Mapping = getHyperSplitSensorBusTempMapping(
                    equip.temperatureSensorBusAdd1.readDefaultVal().toInt() + 1
                )
            } else {
                sensorBus.sensorBus2Mapping = disableIt()
            }

            if (equip.sensorBusAddress2Enable.isEnabled()) {
                sensorBus.sensorBus3Mapping = getHyperSplitSensorBusTempMapping(
                    equip.temperatureSensorBusAdd2.readDefaultVal().toInt() + 1
                )
            } else {
                sensorBus.sensorBus3Mapping = disableIt()
            }
            return sensorBus.build()

        }

        /**
         * Function to read all the generic tuners which are required for Hyperstat to run on standalone mode
         * @return HyperSplitTunersGeneric_t
         */
        private fun getGenericTunerDetails(equip: HyperStatSplitEquip): HyperSplit.HyperSplitTunersGeneric_t {
            val genericTuners = HyperSplit.HyperSplitTunersGeneric_t.newBuilder()
            genericTuners.unoccupiedSetback = (TunerUtil.readTunerValByQuery(
                "domainName == \"" + DomainName.unoccupiedZoneSetback + "\"", equip.equipRef) * 10).toInt()
            genericTuners.unoccupiedSetback = (CCUHsApi.getInstance().readPointPriorityValByQuery("schedulable and zone and unoccupied and setback and roomRef == \"" + equip.roomRef + "\"") * 10).toInt()

            genericTuners.minFanRuntimePostconditioning = (
                    TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.minFanRuntimePostConditioning + "\"", equip.equipRef)).toInt()
            genericTuners.relayActivationHysteresis =
                TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.standaloneRelayActivationHysteresis + "\"", equip.equipRef).toInt()
            genericTuners.analogFanSpeedMultiplier =
                (TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.standaloneAnalogFanSpeedMultiplier + "\"", equip.equipRef) * 10 ).toInt()
            genericTuners.humidityHysteresis = TunerUtil.getHysteresisPoint("humidity", equip.equipRef).toInt()
            genericTuners.autoAwayZoneSetbackTemp =
                (TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.autoAwaySetback + "\"") * 10).toInt()
            genericTuners.autoAwayTime = TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.autoAwayTime + "\"", equip.equipRef).toInt()
            genericTuners.forcedOccupiedTime =
                TunerUtil.readTunerValByQuery("domainName == \"" + DomainName.forcedOccupiedTime + "\"", equip.equipRef).toInt()
            return genericTuners.build()
        }

        private fun readConfig(hsApi: CCUHsApi, equipRef: String, markers: String): Double {
            return hsApi.readDefaultVal(
                "point and $markers and equipRef == \"$equipRef\""
            )
        }

        private fun readTuner(equipRef: String, markers: String): Double {
            return TunerUtil.readTunerValByQuery(
                "$markers and equipRef == \"$equipRef\""
            )
        }

        private fun readTunerByDomainName(domainName: String, equipRef: String): Double {
            return TunerUtil.readTunerValByQuery("domainName == \"$domainName\" and equipRef == \"$equipRef\"")
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
            val compressorStage1Query = "domainName == \"" + DomainName.fanOutCompressorStage1 + "\" and $equipRefQuery"
            val compressorStage2Query = "domainName == \"" + DomainName.fanOutCompressorStage2 + "\" and $equipRefQuery"
            val compressorStage3Query = "domainName == \"" + DomainName.fanOutCompressorStage3 + "\" and $equipRefQuery"

            val auxHeating1Activate = "domainName == \"" + DomainName.auxHeating1Activate + "\" and $equipRefQuery"
            val auxHeating2Activate = "domainName == \"" + DomainName.auxHeating2Activate + "\" and $equipRefQuery"

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
            if (ccuHsApi.readEntity(compressorStage1Query).isNotEmpty()) {
                stagedFanVoltages.compressorStage1FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(compressorStage1Query)).toInt()
            }
            if (ccuHsApi.readEntity(compressorStage2Query).isNotEmpty()) {
                stagedFanVoltages.compressorStage2FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(compressorStage2Query)).toInt()
            }
            if (ccuHsApi.readEntity(compressorStage3Query).isNotEmpty()) {
                stagedFanVoltages.compressorStage3FanAnalogVoltage =
                    (10 * ccuHsApi.readPointPriorityValByQuery(compressorStage3Query)).toInt()
            }
            if (ccuHsApi.readEntity(auxHeating1Activate).isNotEmpty()) {
                stagedFanVoltages.auxHeating1Activate =
                    TunerUtil.readTunerValByQuery(auxHeating1Activate).toInt()
            }
            if (ccuHsApi.readEntity(auxHeating2Activate).isNotEmpty()) {
                stagedFanVoltages.auxHeating2Activate =
                    TunerUtil.readTunerValByQuery(auxHeating2Activate).toInt()
            }
            return stagedFanVoltages.build()
        }


        fun getLinearFanSpeedDetails(equip: HyperStatSplitEquip): HyperSplit.HypersplitLinearFanSpeeds_t? {
            val linearFanSpeedBuilder = HyperSplit.HypersplitLinearFanSpeeds_t.newBuilder()
            val ccuHsApi = CCUHsApi.getInstance()
            val equipRefQuery = "equipRef == \"${equip.equipRef}\""

            val fanSpeedLevels = listOf("low", "medium", "high")
            val linearMapping = if (equip is HsSplitCpuEquip) {
                CpuAnalogControlType.LINEAR_FAN.ordinal
            } else {
                Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
            }
            for (fanSpeed in fanSpeedLevels) {
                for (analog in listOf(1, 2, 3, 4)) {
                    val query = "analog == $analog and $fanSpeed and sp and fan and $equipRefQuery"
                    if (ccuHsApi.readEntity(query).isNotEmpty()&& getAnalogOutMapping(ccuHsApi,equip.equipRef,analog) == linearMapping) {
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


        fun getStagedFanSpeedDetails(equip: HyperStatSplitEquip): HyperSplit.HypersplitStagedFanSpeeds_t? {
            val stagedFanSpeedBuilder = HyperSplit.HypersplitStagedFanSpeeds_t.newBuilder()
            if (equip is HsSplitCpuEquip) {
                val ccuHsApi = CCUHsApi.getInstance()
                val equipRefQuery = "equipRef == \"${equip.equipRef}\""

                val fanSpeedLevels = listOf("low", "medium", "high")

                for (fanSpeed in fanSpeedLevels) {
                    for (analog in listOf(1, 2, 3, 4)) {
                        val query =
                            "analog == $analog and $fanSpeed and sp and fan and $equipRefQuery"
                        if (ccuHsApi.readEntity(query).isNotEmpty() && getAnalogOutMapping(
                                ccuHsApi,
                                equip.equipRef,
                                analog
                            )
                            == CpuAnalogControlType.STAGED_FAN.ordinal
                        ) {
                            val fanLevel = ccuHsApi.readPointPriorityValByQuery(query).toInt()
                            when (fanSpeed) {
                                "low" -> stagedFanSpeedBuilder.stagedFanLowSpeedLevel = fanLevel
                                "medium" -> stagedFanSpeedBuilder.stagedFanMediumSpeedLevel =
                                    fanLevel

                                "high" -> stagedFanSpeedBuilder.stagedFanHighSpeedLevel = fanLevel
                            }
                            break
                        }
                    }
                }
            }
            return stagedFanSpeedBuilder.build()
        }

        fun getMisSettings(equipRef: String): Int {
            val equip = HyperStatSplitEquip(equipRef)
            // bit 0: enableExternal10kTemperatureSensor sending always 0 currently not used, but can be set in future
            val enableExternal10kTemperatureSensor = false
            val disableTouch = equip.disableTouch.isEnabled()
            val brightnessVariationEnable = equip.enableBrightness.isEnabled()
            val desiredTempDisplay = equip.enableDesiredTempDisplay.isEnabled()
            val enableBacklight = equip.enableBacklight.isEnabled()

            var miscSettings = 0

            if (enableExternal10kTemperatureSensor) miscSettings = miscSettings or (1 shl 0)
            if (disableTouch)                       miscSettings = miscSettings or (1 shl 1)
            if (brightnessVariationEnable)          miscSettings = miscSettings or (1 shl 2)
            if (desiredTempDisplay)                 miscSettings = miscSettings or (1 shl 3)
            if (enableBacklight)                    miscSettings = miscSettings or (1 shl 4)

            return miscSettings
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

        fun getPin(point: Point): Int {
            return try {
                Base64Util.decode(point.readDefaultStrVal()).toInt()
            } catch (e: Exception) {
                0
            }
        }
    }
    
}