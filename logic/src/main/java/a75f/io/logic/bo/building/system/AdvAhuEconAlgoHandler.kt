package a75f.io.logic.bo.building.system

//import a75f.io.logic.bo.building.oao.OAOProfile
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.epidemicModeSystemState
import a75f.io.domain.api.DomainName.systemEnhancedVentilationEnable
import a75f.io.domain.api.DomainName.systemPostPurgeEnable
import a75f.io.domain.api.DomainName.systemPrePurgeEnable
import a75f.io.domain.equips.ConnectModuleEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.oao.OAOProfile
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationTypeConnect.OAO_DAMPER
import a75f.io.logic.tuners.TunerUtil
import android.content.Context
import java.lang.Double.max
import kotlin.math.min

class AdvAhuEconAlgoHandler(private val connectEquip: ConnectModuleEquip) {
    //    private var economizingAvailable = false
    private var economizingLoopOutput = 0
    private var outsideAirCalculatedMinDamper: Double = 0.0
    private var outsideAirLoopOutput = 0.0
    private var outsideAirFinalLoopOutput = 0
    var systemMode: SystemMode? = null
    var returnAirFinalOutput: Double = 0.0
    var epidemicState: EpidemicState = EpidemicState.OFF
    companion object {
        var analogAssociatedToOAO = false
        private var economizingAvailable: Boolean = false
        private var freeCoolingOn: Boolean = false
        private var dcvAvailable = false
        private var matThrottle = false

        fun getAnalogsAssociatedToOAO(): Boolean {
            return analogAssociatedToOAO
        }

        fun isEconomizingAvailable(): Boolean {
            return economizingAvailable
        }

        private fun setEconomizingAvailable(economizingAvailable: Boolean) {
            this.economizingAvailable = economizingAvailable
        }

        fun isFreeCoolingOn(): Boolean {
            return freeCoolingOn
        }

        private fun setFreeCoolingOn(freeCoolingOn: Boolean) {
            this.freeCoolingOn = freeCoolingOn
        }
        private fun isDcvAvailable(): Boolean {
            return dcvAvailable
        }

        private fun setDcvAvailable(dcvAvailable: Boolean) {
            this.dcvAvailable = dcvAvailable
        }

        private fun setMatThrottle(matThrottle: Boolean) {
            this.matThrottle = matThrottle
        }

        private fun isMatThrottle(): Boolean {
            return matThrottle
        }
    }

    /**
     * @brief Performs Outside Air Optimization (OAO) logic to control dampers and exhaust fans.
     *
     * This function calculates and controls the outside air damper, return air damper, and exhaust fan stages
     * based on economizing, demand control ventilation (DCV), and mixed air temperature (MAT) safety logic.
     * It ensures optimal ventilation while maintaining energy efficiency and safety.
     *
     * @details
     * 1: Calculate the economizing availability and economizing loop output
     * 2: Calculate the dcvAvailable and minimum outside damper opening required for DCV
     * 3: Determine calculated OAO damper command by taking the maximum value out of the loops that are currently enabled
     * 4: Finally, apply low-MAT safety logic to get outsideAirFinalLoopOutput
     * 5: Calculate the return air damper output
     * 6: Update the loop output and damper values to the points
     * 7: Update the exhaust fan stages based on the outside air final loop output
     * 8: Update the MAT throttle value to the mat throttle point
     *
     */
    fun doOAO() {
        if (!isAnyAnalogAssociatedToOAO()) {
            return
        }
        val systemProfile = L.ccu().systemProfile
        val outsideDamperMinOpen: Double = getEffectiveOutsideDamperMinOpen(systemProfile)
        // Step 0: Perform epidemic control
        doEpidemicControl()
        // Step 1: Calculate the economizing availability and economizing loop output
        doEconomizing(systemProfile)
        // Step 2: Calculate the dcvAvailable and minimum outside damper opening required for DCV
        doDcvControl(outsideDamperMinOpen, systemProfile)
        // Step 3: Determine calculated OAO damper command by taking the maximum value out of the loops that are currently enabled
        outsideAirLoopOutput = max(economizingLoopOutput.toDouble(), outsideAirCalculatedMinDamper)

        val outsideDamperMatTarget: Double =
            connectEquip.outsideDamperMixedAirTarget.readPriorityVal()
        val outsideDamperMatMin: Double =
            connectEquip.outsideDamperMixedAirMinimum.readPriorityVal()

        val returnDamperMinOpen: Double = connectEquip.returnDamperMinOpen.readDefaultVal()
        val matTemp: Double = connectEquip.mixedAirTemperature.readHisVal()

        CcuLog.d(
            L.TAG_CCU_OAO,
            "outsideAirLoopOutput $outsideAirLoopOutput outsideDamperMatTarget $outsideDamperMatTarget outsideDamperMatMin $outsideDamperMatMin matTemp $matTemp"
        )
        // Step 4: Finally, apply low-MAT safety logic to get outsideAirFinalLoopOutput
        setMatThrottle(false)
        if (outsideAirLoopOutput > outsideDamperMinOpen) {
            if (matTemp < outsideDamperMatTarget && matTemp > outsideDamperMatMin) {
                outsideAirFinalLoopOutput =
                    (outsideAirLoopOutput - outsideAirLoopOutput * ((outsideDamperMatTarget - matTemp) / (outsideDamperMatTarget - outsideDamperMatMin))).toInt()
            } else {
                outsideAirFinalLoopOutput =
                    if ((matTemp <= outsideDamperMatMin)) 0 else outsideAirLoopOutput.toInt()
            }
            if (matTemp < outsideDamperMatTarget) {
                setMatThrottle(true)
            }
        } else {
            outsideAirFinalLoopOutput = outsideDamperMinOpen.toInt()
        }

        if (matThrottle) {
            outsideAirFinalLoopOutput = max(outsideAirFinalLoopOutput.toDouble(), 0.0).toInt()
        } else {
            outsideAirFinalLoopOutput =
                max(outsideAirFinalLoopOutput.toDouble(), outsideDamperMinOpen).toInt()
        }

        outsideAirFinalLoopOutput = min(outsideAirFinalLoopOutput.toDouble(), 100.0).toInt()

        // Step 5: Calculate the return air damper output
        returnAirFinalOutput =
            max(returnDamperMinOpen, ((100 - outsideAirFinalLoopOutput).toDouble()))

        CcuLog.d(
            L.TAG_CCU_OAO,
            " economizingLoopOutput $economizingLoopOutput outsideAirCalculatedMinDamper $outsideAirCalculatedMinDamper outsideAirFinalLoopOutput $outsideAirFinalLoopOutput,$returnAirFinalOutput"
        )

        // Update the status of free cooling to indicate the ports are open for cooling
        if(connectEquip.economizingAvailable.readHisVal() > 0) {
            setFreeCoolingOn(true)
        } else {
            setFreeCoolingOn(false)
        }

        // Step 6: Update the loop output and damper values to the points
        connectEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())
        connectEquip.outsideDamperCmd.writeHisVal(outsideAirFinalLoopOutput.toDouble())
        connectEquip.returnDamperCmd.writeHisVal(returnAirFinalOutput)

        val exhaustFanHysteresis: Double = connectEquip.exhaustFanHysteresis.readDefaultVal()
        val exhaustFanStage1Threshold: Double =
            connectEquip.exhaustFanStage1Threshold.readDefaultVal()
        val exhaustFanStage2Threshold: Double =
            connectEquip.exhaustFanStage2Threshold.readDefaultVal()

        CcuLog.d(
            L.TAG_CCU_OAO,
            " exhaustFanHysteresis $exhaustFanHysteresis exhaustFanStage1Threshold $exhaustFanStage1Threshold exhaustFanStage2Threshold $exhaustFanStage2Threshold outsideAirFinalLoopOutput $outsideAirFinalLoopOutput"
        )

        // Step 7: Update the exhaust fan stages based on the outside air final loop output
        if (outsideAirFinalLoopOutput > exhaustFanStage1Threshold) {
            connectEquip.exhaustFanStage1.writeHisVal(1.0)
        } else if (outsideAirFinalLoopOutput < (exhaustFanStage1Threshold - exhaustFanHysteresis)) {
            connectEquip.exhaustFanStage1.writeHisVal(0.0)
        }

        if (outsideAirFinalLoopOutput > exhaustFanStage2Threshold) {
            connectEquip.exhaustFanStage2.writeHisVal(1.0)
        } else if (outsideAirFinalLoopOutput < (exhaustFanStage2Threshold - exhaustFanHysteresis)) {
            connectEquip.exhaustFanStage2.writeHisVal(0.0)
        }
        // Step 8: Update the MAT throttle value to the point
        connectEquip.matThrottle.writeHisVal((if (isMatThrottle()) 1 else 0).toDouble())
    }

    private fun handleSmartPrePurgeControl() {
        val smartPrePurgeRunTime: Double = connectEquip.systemPrePurgeRuntimeTuner.readPriorityVal()
        val smartPrePurgeOccupiedTimeOffset: Double =
            connectEquip.systemPrePurgeOccupiedTimeOffsetTuner.readPriorityVal()
        val occuSchedule = ScheduleManager.getInstance().nextOccupiedTimeInMillis
        val minutesToOccupancy =
            if (occuSchedule != null) occuSchedule.millisecondsUntilNextChange.toInt() / 60000 else -1
        if ((minutesToOccupancy != -1) && (smartPrePurgeOccupiedTimeOffset >= minutesToOccupancy) && (minutesToOccupancy >= (smartPrePurgeOccupiedTimeOffset - smartPrePurgeRunTime))) {
            outsideAirCalculatedMinDamper = connectEquip.systemPurgeOutsideDamperMinPos.readDefaultVal()

            CCUHsApi.getInstance().writeHisValByQuery(
                "domainName == \"$epidemicModeSystemState\"",
                EpidemicState.PREPURGE.ordinal.toDouble()
            )
            epidemicState = EpidemicState.PREPURGE
        }
    }

    private fun handleSmartPostPurgeControl() {
        val smartPostPurgeRunTime: Double = connectEquip.systemPostPurgeRuntimeTuner.readPriorityVal()
        val smartPostPurgeOccupiedTimeOffset: Double =
            connectEquip.systemPostPurgeOccupiedTimeOffsetTuner.readPriorityVal()
        val occuSchedule = ScheduleManager.getInstance().prevOccupiedTimeInMillis
        if (occuSchedule != null) CcuLog.d(
            L.TAG_CCU_OAO,
            "System Unoccupied, check postpurge22 = " + occuSchedule.millisecondsUntilPrevChange + "," + (occuSchedule.millisecondsUntilPrevChange) / 60000 + "," + smartPostPurgeOccupiedTimeOffset + "," + smartPostPurgeRunTime
        )
        val minutesInUnoccupied =
            if (occuSchedule != null) (occuSchedule.millisecondsUntilPrevChange / 60000).toInt() else -1
        if ((epidemicState == EpidemicState.OFF) && (minutesInUnoccupied != -1) && (minutesInUnoccupied >= smartPostPurgeOccupiedTimeOffset) && (minutesInUnoccupied <= (smartPostPurgeRunTime + smartPostPurgeOccupiedTimeOffset))) {
            outsideAirCalculatedMinDamper = connectEquip.systemPurgeOutsideDamperMinPos.readDefaultVal()
            CCUHsApi.getInstance().writeHisValByQuery(
                "domainName == \"$epidemicModeSystemState\"",
                EpidemicState.POSTPURGE.ordinal.toDouble()
            )
            epidemicState = EpidemicState.POSTPURGE
        }
    }

    private fun handleEnhancedVentilationControl() {
        epidemicState = EpidemicState.ENHANCED_VENTILATION
        outsideAirCalculatedMinDamper =
            connectEquip.enhancedVentilationOutsideDamperMinOpen.readDefaultVal()
        CCUHsApi.getInstance().writeHisValByQuery(
            "domainName == \"$epidemicModeSystemState\"",
            EpidemicState.ENHANCED_VENTILATION.ordinal.toDouble()
        )
        CcuLog.d(
            L.TAG_CCU_OAO,
            "System occupied, check enhanced ventilation = " + outsideAirCalculatedMinDamper + "," + epidemicState.name
        )
    }

    private fun doEpidemicControl() {
        epidemicState = EpidemicState.OFF
        if (systemMode != SystemMode.OFF) {
            val systemOccupancy = ScheduleManager.getInstance().systemOccupancy
            when (systemOccupancy) {
                Occupancy.UNOCCUPIED -> {
                    val isSmartPrePurge: Boolean = TunerUtil.readSystemUserIntentVal("domainName == \"$systemPrePurgeEnable\"") > 0
                    val isSmartPostPurge: Boolean = TunerUtil.readSystemUserIntentVal("domainName == \"$systemPostPurgeEnable\"") > 0
                    if (isSmartPrePurge) {
                        handleSmartPrePurgeControl()
                    }
                    if (isSmartPostPurge) {
                        handleSmartPostPurgeControl()
                    }
                }

                Occupancy.OCCUPIED, Occupancy.FORCEDOCCUPIED, Occupancy.OCCUPANCYSENSING -> {
                    val isEnhancedVentilation =TunerUtil.readSystemUserIntentVal(
                        "domainName == \"$systemEnhancedVentilationEnable\""
                    ) > 0

                    if (isEnhancedVentilation) handleEnhancedVentilationControl()
                }

                else -> {}
            }
        }
        if (epidemicState == EpidemicState.OFF) {
            val prevEpidemicStateValue = CCUHsApi.getInstance()
                .readHisValByQuery("point and sp and system and epidemic and mode and state")
            if (prevEpidemicStateValue != EpidemicState.OFF.ordinal.toDouble()) CCUHsApi.getInstance()
                .writeHisValByQuery(
                    "point and sp and system and epidemic and mode and state",
                    EpidemicState.OFF.ordinal.toDouble()
                )
        }
    }

    /**
     * Checks the external temp against drybulb threshold tuner.
     * @param externalTemp external temperature
     * @param externalHumidity external humidity
     * @param economizingMinTemp economizing min temp
     */
    private fun isDryBulbTemperatureGoodForEconomizing(
        externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double
    ): Boolean {
        val dryBulbTemperatureThreshold: Double =
            connectEquip.economizingDryBulbThreshold.readPriorityVal()
        var outsideAirTemp = externalTemp

        /* Both the weather parameters may be zero when CCU cant reach remote weather service
         * Then fallback to Local Outside Air Temp.
         */
        if (externalHumidity == 0.0 && externalTemp == 0.0) {
            outsideAirTemp = connectEquip.outsideTemperature.readHisVal()
        }

        if (outsideAirTemp > economizingMinTemp) {
            return outsideAirTemp < dryBulbTemperatureThreshold
        }
        return false
    }

    /**
     * Checks if the outside whether is suitable for economizing.
     * @param externalTemp external temperature
     * @param externalHumidity external humidity
     * @param economizingMinTemp economizing min temp
     */
    private fun isOutsideWeatherSuitableForEconomizing(
        externalTemp: Double, externalHumidity: Double, economizingMinTemp: Double
    ): Boolean {
        val economizingMaxTemp: Double = connectEquip.economizingMaxTemperature.readPriorityVal()
        val economizingMinHumidity: Double = connectEquip.economizingMinHumidity.readPriorityVal()
        val economizingMaxHumidity: Double = connectEquip.economizingMaxHumidity.readPriorityVal()

        if (externalTemp > economizingMinTemp && externalTemp < economizingMaxTemp && externalHumidity > economizingMinHumidity && externalHumidity < economizingMaxHumidity) {
            return true
        }
        CcuLog.d(
            L.TAG_CCU_OAO,
            "Outside air not suitable for economizing Temp : $externalTemp Humidity : $externalHumidity"
        )
        return false
    }

    /**
     * Compare the inside vs outside enthalpy.
     */
    private fun isInsideEnthalpyGreaterThanOutsideEnthalpy(
        insideEnthalpy: Double, outsideEnthalpy: Double
    ): Boolean {
        CcuLog.d(L.TAG_CCU_OAO, " insideEnthalpy $insideEnthalpy, outsideEnthalpy $outsideEnthalpy")

        val enthalpyDuctCompensationOffset: Double =
            connectEquip.enthalpyDuctCompensationOffset.readPriorityVal()

        return insideEnthalpy > outsideEnthalpy + enthalpyDuctCompensationOffset
    }

    /**
     * Evaluates outside temperature and humidity to determine if free-cooling can be used.
     *
     * @param externalTemp     external temperature
     * @param externalHumidity external humidity
     * @param systemProfile present system profile
     */
    private fun canDoEconomizing(
        externalTemp: Double, externalHumidity: Double, systemProfile: SystemProfile
    ): Boolean {
        val economizingMinTemp: Double = connectEquip.economizingMinTemperature.readPriorityVal()

        val insideEnthalpy = OAOProfile.getAirEnthalpy(
            systemProfile.systemController.averageSystemTemperature,
            systemProfile.systemController.averageSystemHumidity
        )

        val outsideEnthalpy = OAOProfile.getAirEnthalpy(externalTemp, externalHumidity)
        connectEquip.insideEnthalpy.writeHisVal(insideEnthalpy)
        connectEquip.outsideEnthalpy.writeHisVal(outsideEnthalpy)


        CcuLog.d(
            L.TAG_CCU_OAO,
            " canDoEconomizing externalTemp $externalTemp externalHumidity $externalHumidity"
        )

        if (systemProfile.systemController.getSystemState() != SystemController.State.COOLING) {
            return false
        }

        if (isDryBulbTemperatureGoodForEconomizing(
                externalTemp, externalHumidity, economizingMinTemp
            )
        ) {
            CcuLog.d(L.TAG_CCU_OAO, "Do economizing based on drybulb temperature")
            return true
        }

        if (!isOutsideWeatherSuitableForEconomizing(
                externalTemp, externalHumidity, economizingMinTemp
            )
        ) {
            return false
        }


        //If outside enthalpy is lower, do economizing.
        if (isInsideEnthalpyGreaterThanOutsideEnthalpy(insideEnthalpy, outsideEnthalpy)) {
            CcuLog.d(L.TAG_CCU_OAO, "Do economizing based on enthalpy")
            return true
        }

        return false
    }

    /**
     * @brief Performs economizing logic based on external conditions and system profile.
     *
     * This function determines whether economizing is possible based on external temperature,
     * humidity, and the system profile. If economizing is allowed, it calculates the economizing
     * loop output and updates the system's economizing availability and output values.
     *
     * @param systemProfile The system profile containing cooling loop operation data.
     *
     *
     * @note The function relies on external data (temperature, humidity) and system configuration
     *       to determine economizing availability and output.
     *
     */
    private fun doEconomizing(systemProfile: SystemProfile) {
        //TODO: This needs to changed with domainName
        var externalTemp: Double
        var externalHumidity: Double

        val sharedPreferences = Globals.getInstance().applicationContext.getSharedPreferences(
            "ccu_devsetting",
            Context.MODE_PRIVATE
        )
        externalTemp = if (connectEquip.outsideTemperature.pointExists()) {
            connectEquip.outsideTemperature.readHisVal()
        } else {
            sharedPreferences.getInt("outside_temp", 0).toDouble()
        }
        externalHumidity = if (connectEquip.outsideHumidity.pointExists()) {
            connectEquip.outsideHumidity.readHisVal()
        } else {
            sharedPreferences.getInt("outside_humidity", 0).toDouble()
        }


        val economizingToMainCoolingLoopMap: Double =
            connectEquip.economizingToMainCoolingLoopMap.readPriorityVal()

        if (canDoEconomizing(externalTemp, externalHumidity, systemProfile)) {
            setEconomizingAvailable(true)
            economizingLoopOutput = min(
                systemProfile.coolingLoopOp * 100 / economizingToMainCoolingLoopMap,
                100.0
            ).toInt()
        } else {
            setEconomizingAvailable(false)
            economizingLoopOutput = 0
        }
        // If the free cooling output is non zero, then set the economizingAvailable to true
        connectEquip.economizingAvailable.writeHisVal((if (economizingLoopOutput > 0) 1 else 0).toDouble())
        connectEquip.economizingLoopOutput.writeHisVal(economizingLoopOutput.toDouble())
    }

    private fun isAnyAnalogAssociatedToOAO(): Boolean {
        analogAssociatedToOAO = false
        getConnectAnalogAssociationMap(connectEquip).entries.forEach { (analogOut, association) ->
            if (association.readDefaultVal().toInt() == OAO_DAMPER.ordinal || association.readDefaultVal().toInt() == AdvancedAhuAnalogOutAssociationTypeConnect.RETURN_DAMPER.ordinal ) {
                val logical =
                    connectAnalogOutAssociationToDomainName(association.readDefaultVal().toInt())
                CcuLog.i(
                    L.TAG_CCU_SYSTEM,
                    "Connect ${analogOut.domainName}: ${analogOut.readDefaultVal()} => : " + "Physical Value: ${
                        getConnectAnalogOutLogicalPhysicalMap(
                            connectEquip, Domain.connect1Device
                        )[analogOut]!!.readHisVal()
                    }  " + "$logical : ${
                        getDomainPointForName(
                            logical, connectEquip
                        ).readHisVal()
                    } "
                )
                analogAssociatedToOAO = true
                return true
            }
        }
        return false
    }

    /**
     * @brief Performs Demand Control Ventilation (DCV) logic based on CO2 levels and system occupancy.
     *
     * This function calculates the minimum outside damper opening required for DCV based on CO2 levels
     * and system occupancy. It also determines if DCV is available and updates the system's DCV-related
     * values.
     *
     * @param outsideDamperMinOpen The minimum outside damper opening value.
     * @param systemProfile The system profile containing CO2 loop operation data and weighted average CO2.
     *
     * @note The function uses CO2 levels and system occupancy to determine DCV availability and damper opening.
     *
     */
    private fun doDcvControl(outsideDamperMinOpen: Double, systemProfile: SystemProfile) {
        setDcvAvailable(false)
        var dcvCalculatedMinDamper = 0.0
        val usePerRoomCO2Sensing: Boolean = connectEquip.usePerRoomCO2Sensing.readDefaultVal() > 0
        var isCo2levelUnderThreshold = true
        if (usePerRoomCO2Sensing) {
            dcvCalculatedMinDamper = systemProfile.co2LoopOp
            CcuLog.d(
                L.TAG_CCU_OAO, "usePerRoomCO2Sensing dcvCalculatedMinDamper $dcvCalculatedMinDamper"
            )
        } else {
            val returnAirCO2: Double = connectEquip.returnAirCo2.readHisVal()
            val co2Threshold: Double = connectEquip.co2Threshold.readDefaultVal()
            val co2DamperOpeningRate: Double = connectEquip.co2DamperOpeningRate.readPriorityVal()

            if (returnAirCO2 > co2Threshold) {
                dcvCalculatedMinDamper = (returnAirCO2 - co2Threshold) / co2DamperOpeningRate
                isCo2levelUnderThreshold = false
            }
            CcuLog.d(
                L.TAG_CCU_OAO,
                " dcvCalculatedMinDamper $dcvCalculatedMinDamper returnAirCO2 $returnAirCO2 co2Threshold $co2Threshold"
            )
        }
        connectEquip.co2WeightedAverage.writeHisVal(systemProfile.weightedAverageCO2)
        val systemOccupancy = ScheduleManager.getInstance().systemOccupancy
        when (systemOccupancy) {
            Occupancy.OCCUPIED, Occupancy.FORCEDOCCUPIED, Occupancy.DEMAND_RESPONSE_OCCUPIED -> if (systemMode != SystemMode.OFF) {
                val tempOutsideDamperMinOpen =
                    if (epidemicState != EpidemicState.OFF) outsideAirCalculatedMinDamper else outsideDamperMinOpen
                outsideAirCalculatedMinDamper =
                    min(tempOutsideDamperMinOpen + dcvCalculatedMinDamper, 100.0)
                if (!isCo2levelUnderThreshold) {
                    setDcvAvailable(true)
                }
            } else outsideAirCalculatedMinDamper = outsideDamperMinOpen

            Occupancy.PRECONDITIONING, Occupancy.VACATION -> outsideAirCalculatedMinDamper =
                outsideDamperMinOpen

            Occupancy.UNOCCUPIED, Occupancy.DEMAND_RESPONSE_UNOCCUPIED -> if (epidemicState == EpidemicState.OFF) outsideAirCalculatedMinDamper =
                outsideDamperMinOpen

            else -> {}
        }
        connectEquip.outsideAirCalculatedMinDamper.writeHisVal(outsideAirCalculatedMinDamper)
        connectEquip.dcvAvailable.writeHisVal((if (isDcvAvailable()) 1.0 else 0.0))
    }

    private fun getOutsideDamperMinOpen(
        outsideDamperMinOpenFromFan: Double, outsideDamperMinOpenFromConditioning: Double
    ): Double {
        return max(outsideDamperMinOpenFromFan, outsideDamperMinOpenFromConditioning)
    }

    private fun getEffectiveOutsideDamperMinOpen(systemProfile: SystemProfile): Double {
        var outsideDamperMinOpenFromFan = 0.0

        systemMode =
            SystemMode.values()[HSUtil.getSystemUserIntentVal("conditioning and mode").toInt()]
        val connectRelayAssociationMap = getConnectRelayAssociationMap(connectEquip)

        if (isConnectStageEnabled(
                DomainName.loadFanStage3,
                connectRelayAssociationMap
            ) || isConnectStageEnabled(
                DomainName.loadFanStage4,
                connectRelayAssociationMap
            ) || isConnectStageEnabled(DomainName.loadFanStage5, connectRelayAssociationMap)
        ) {
            // 3+ Stages mapped: Stage 1 = LOW, Stage 2 = MEDIUM, Stage 3+ = HIGH
            if (isConnectHighFanStagesEnabled(connectEquip)) {
                outsideDamperMinOpenFromFan =
                    connectEquip.outsideDamperMinOpenDuringFanHigh.readDefaultVal()
            } else if (isConnectMediumFanStagesEnabled(connectEquip)) {
                outsideDamperMinOpenFromFan =
                    connectEquip.outsideDamperMinOpenDuringFanMedium.readDefaultVal()
            } else if (isConnectLowFanStageEnabled(connectEquip)) {
                outsideDamperMinOpenFromFan =
                    connectEquip.outsideDamperMinOpenDuringFanLow.readDefaultVal()
            }
        } else if (isConnectStageEnabled(DomainName.loadFanStage2, connectRelayAssociationMap)) {
            // 2 stages mapped: Stage 2 = HIGH, Stage 1 = MEDIUM
            if (isConnectMediumFanStagesEnabled(connectEquip)) {
                outsideDamperMinOpenFromFan =
                    connectEquip.outsideDamperMinOpenDuringFanHigh.readDefaultVal()
            } else if (isConnectLowFanStageEnabled(connectEquip)) {
                outsideDamperMinOpenFromFan =
                    connectEquip.outsideDamperMinOpenDuringFanMedium.readDefaultVal()
            }
        } else if (isConnectStageEnabled(DomainName.loadFanStage1, connectRelayAssociationMap)) {
            // 1 stage mapped: Stage 1 = HIGH
            if (isConnectLowFanStageEnabled(connectEquip)) {
                outsideDamperMinOpenFromFan =
                    connectEquip.outsideDamperMinOpenDuringFanHigh.readDefaultVal()
            }
        }

        val outsideDamperMinOpenFromConditioning: Double
        if (systemProfile.isCoolingActive || systemProfile.isHeatingActive || connectEquip.economizingLoopOutput.readHisVal() > 0.0) {
            outsideDamperMinOpenFromConditioning =
                connectEquip.outsideDamperMinOpenDuringConditioning.readDefaultVal()
        } else {
            outsideDamperMinOpenFromConditioning =
                connectEquip.outsideDamperMinOpenDuringRecirculation.readDefaultVal()
        }

        val systemOccupancy = ScheduleManager.getInstance().systemOccupancy
        if ((systemOccupancy == Occupancy.OCCUPIED || systemOccupancy == Occupancy.FORCEDOCCUPIED) && systemMode != SystemMode.OFF) {
            val outsideDamperMinOpen: Double = getOutsideDamperMinOpen(
                outsideDamperMinOpenFromFan, outsideDamperMinOpenFromConditioning
            )
            CcuLog.d(
                L.TAG_CCU_OAO,
                """Occupied mode, SystemProfile currently selected ${systemProfile.profileType}
                    outsideDamperMinOpenFromFan $outsideDamperMinOpenFromFan outsideDamperMinOpenFromConditioning $outsideDamperMinOpenFromConditioning
                    outside damper min open for OAO based on system profile selected: $outsideDamperMinOpen"""
            )
            return outsideDamperMinOpen
        } else {
            return 0.0
        }
    }

    fun updateLoopPoints() {
        connectEquip.economizingLoopOutput.writeHisVal(economizingLoopOutput.toDouble())
        connectEquip.dcvAvailable.writeHisVal((if (isDcvAvailable()) 1 else 0).toDouble())
        connectEquip.outsideAirCalculatedMinDamper.writeHisVal(outsideAirCalculatedMinDamper)
        connectEquip.matThrottle.writeHisVal((if (matThrottle) 1 else 0).toDouble())
        connectEquip.outsideAirFinalLoopOutput.writeHisVal(outsideAirFinalLoopOutput.toDouble())
    }

    fun resetLoopPoints() {
        economizingLoopOutput = 0
        outsideAirCalculatedMinDamper = 0.0
        outsideAirLoopOutput = 0.0
        matThrottle = false
        outsideAirFinalLoopOutput = 0
        updateLoopPoints()
    }

}