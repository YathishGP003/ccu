package a75f.io.logic.bo.building.system.dab

import a75.io.algos.ControlLoop
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.api.readPoint
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.autocommission.AutoCommissioningUtil
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.AdvancedAhuAlgoHandler
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationTypeConnect
import a75f.io.logic.bo.building.system.AdvancedAhuRelayAssociationType
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.analogOutAssociationToDomainName
import a75f.io.logic.bo.building.system.co2DamperControlTypeToDomainPoint
import a75f.io.logic.bo.building.system.connectAnalogOutAssociationToDomainName
import a75f.io.logic.bo.building.system.connectRelayAssociationToDomainName
import a75f.io.logic.bo.building.system.getCMAnalogAssociationMap
import a75f.io.logic.bo.building.system.getCMRelayAssociationMap
import a75f.io.logic.bo.building.system.getCMRelayLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getConnectAnalogAssociationMap
import a75f.io.logic.bo.building.system.getConnectAnalogOutLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getConnectRelayAssociationMap
import a75f.io.logic.bo.building.system.getConnectRelayLogicalPhysicalMap
import a75f.io.logic.bo.building.system.getDomainPointForName
import a75f.io.logic.bo.building.system.pressureFanControlIndexToDomainPoint
import a75f.io.logic.bo.building.system.relayAssociationDomainNameToType
import a75f.io.logic.bo.building.system.relayAssociationToDomainName
import a75f.io.logic.bo.building.system.satControlIndexToDomainPoint
import a75f.io.logic.bo.building.system.updatePressureSensorDerivedPoints
import a75f.io.logic.bo.building.system.updateTemperatureSensorDerivedPoints
import a75f.io.logic.bo.building.system.util.AhuSettings
import a75f.io.logic.bo.building.system.util.AhuTuners
import a75f.io.logic.bo.building.system.util.UserIntentConfig
import a75f.io.logic.bo.building.system.util.getModulatedOutput
import a75f.io.logic.bo.building.system.util.needToUpdateConditioningMode
import a75f.io.logic.bo.building.system.util.roundOff
import a75f.io.logic.tuners.TunerUtil
import android.annotation.SuppressLint
import android.content.Intent
import java.util.BitSet

/**
 * Created by Manjunath K on 19-05-2024.
 */

class DabAdvancedAhu : DabSystemProfile() {

    private var heatingStages = 0
    private var coolingStages = 0
    private var fanStages = 0
    lateinit var systemEquip: DabAdvancedHybridSystemEquip
    private lateinit var advancedAhuImpl: AdvancedAhuAlgoHandler

    private lateinit var cmStageStatus: Array<Pair<Int, Int>>
    private lateinit var connectStageStatus: Array<Pair<Int, Int>>
    private lateinit var conditioningMode: SystemMode
    private lateinit var ahuSettings: AhuSettings
    private lateinit var ahuTuners: AhuTuners

    private var systemSatCoolingLoopOp = 0.0
    private var systemSatHeatingLoopOp = 0.0
    private var staticPressureFanLoopOp = 0.0

    private val satCoolingPILoop = ControlLoop()
    private val satHeatingPILoop = ControlLoop()
    private val staticPressureFanPILoop = ControlLoop()

    private val coolIndexRange = 0..4
    private val heatIndexRange = 5..9
    private val fanIndexRange = 10..14
    private val satCoolIndexRange = 17..21
    private val satHeatIndexRange = 22..26
    private val loadFanIndexRange = 27..31
    private lateinit var analogControlsEnabled: Set<AdvancedAhuAnalogOutAssociationType>

    private var stageUpTimer = 0.0
    private var stageDownTimer = 0.0
    private var satStageUpTimer = 0.0
    private var satStageDownTimer = 0.0

    val testConfigs = BitSet()

    override fun getProfileName(): String = "DAB Advanced Hybrid AHU v2"

    override fun getProfileType(): ProfileType = ProfileType.SYSTEM_DAB_ADVANCED_AHU

    override fun addSystemEquip() {
        systemEquip = Domain.systemEquip as DabAdvancedHybridSystemEquip
        advancedAhuImpl = AdvancedAhuAlgoHandler(systemEquip)
        initializePILoop()
        analogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls(systemEquip.cmEquip, systemEquip.connectEquip1)
    }

    fun updateDomainEquip(equip: DabAdvancedHybridSystemEquip) {
        val systemMode = SystemMode.values()[systemEquip.conditioningMode.readPriorityVal().toInt()]
        advancedAhuImpl = AdvancedAhuAlgoHandler(equip)
        systemEquip = equip
        updateStagesSelected()
        updateSystemMode(systemMode)
    }

    private fun updateSystemMode(systemMode: SystemMode) {
        if (systemMode == SystemMode.OFF) {
            return
        }
        if (needToUpdateConditioningMode(systemMode, isCoolingAvailable, isHeatingAvailable)) {
            systemEquip.conditioningMode.writeVal(HayStackConstants.DEFAULT_POINT_LEVEL, SystemMode.OFF.ordinal.toDouble())
            systemEquip.conditioningMode.writeHisVal(SystemMode.OFF.ordinal.toDouble())
        }
    }

    override fun deleteSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
        if (systemEquip.isNotEmpty()) {
            hayStack.deleteEntity(systemEquip["id"].toString())
        }
        deleteSystemConnectModule()
    }

    override fun isCoolingAvailable(): Boolean {
        return coolingStages > 0 || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_COOLING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL)
    }

    override fun isHeatingAvailable(): Boolean {
        return heatingStages > 0 || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_HEATING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL)
    }

    override fun isCoolingActive(): Boolean = (systemCoolingLoopOp > 0 || systemSatCoolingLoopOp > 0)

    override fun isHeatingActive(): Boolean = (systemHeatingLoopOp > 0 || systemSatHeatingLoopOp > 0)


    override fun doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo()
        updateSystemPoints()
    }

    private fun initializePILoop() {
        val proportionalGain = systemEquip.dabProportionalKFactor.readPriorityVal()
        val integralGain = systemEquip.dabIntegralKFactor.readPriorityVal()
        val proportionalRange = systemEquip.dabTemperatureProportionalRange.readPriorityVal()
        val integralTime = systemEquip.dabTemperatureIntegralTime.readPriorityVal()

        satCoolingPILoop.apply {
            setProportionalGain(proportionalGain)
            setIntegralGain(integralGain)
            proportionalSpread = proportionalRange
            integralMaxTimeout = integralTime.toInt()
        }
        satHeatingPILoop.apply {
            setProportionalGain(proportionalGain)
            setIntegralGain(integralGain)
            proportionalSpread = proportionalRange
            integralMaxTimeout = integralTime.toInt()
        }
        staticPressureFanPILoop.apply {
            setProportionalGain(proportionalGain)
            setIntegralGain(integralGain)
            proportionalSpread = proportionalRange
            integralMaxTimeout = integralTime.toInt()
        }
    }

    fun updateSystemPoints() {
        systemEquip = Domain.systemEquip as DabAdvancedHybridSystemEquip
        advancedAhuImpl = AdvancedAhuAlgoHandler(systemEquip)
        conditioningMode = SystemMode.values()[systemEquip.conditioningMode.readPriorityVal().toInt()]
        ahuSettings = getAhuSettings()
        ahuTuners = getAhuTuners()

        updateStagesSelected()
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())
        updateDerivedSensorPoints()

        cmStageStatus = Array(35) { Pair(0, 0) }
        connectStageStatus = Array(20) { Pair(0, 0) }

        updateStageTimers()
        systemCoolingLoopOp = getSystemCoolingLoop()

        if (AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.COOLING, systemCoolingLoopOp)
            systemCoolingLoopOp = getSystemLoopOutputValue(Tags.COOLING)
        }

        systemHeatingLoopOp = getHeatingLoop()

        if (AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.HEATING, systemHeatingLoopOp)
            systemHeatingLoopOp = getSystemLoopOutputValue(Tags.HEATING)
        }

        systemFanLoopOp = getFanLoop().coerceIn(0.0, 100.0)

        if (AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.FAN, systemFanLoopOp)
            systemFanLoopOp = getSystemLoopOutputValue(Tags.FAN)
        }

        systemSatCoolingLoopOp = getSystemSatCoolingLoop().coerceIn(0.0, 100.0)
        systemSatHeatingLoopOp = getSystemSatHeatingLoop().coerceIn(0.0, 100.0)
        staticPressureFanLoopOp = getSystemStaticPressureFanLoop().coerceIn(0.0, 100.0)
        systemCo2LoopOp = if (isSystemOccupiedForDcv) getCo2Loop() else 0.0

        if (advancedAhuImpl.isEmergencyShutOffEnabledAndActive(ahuSettings.systemEquip, ahuSettings.connectEquip1)
                || conditioningMode == SystemMode.OFF) {
            resetSystem()
        } else {
            updateLoopOpPoints()
            updateOutputPorts()
        }
        dumpLoops()
        updateSystemStatus()
    }

    private fun updateStageTimers() {
        if (stageUpTimer > 0) {
            stageUpTimer--
        } else if (stageDownTimer > 0) {
            stageDownTimer--
        }

        if (satStageUpTimer > 0) {
            satStageUpTimer--
        } else if (satStageDownTimer > 0) {
            satStageDownTimer--
        }
    }

    private fun getAhuTuners(): AhuTuners {
        return AhuTuners(
                relayAActivationHysteresis = systemEquip.dabRelayDeactivationHysteresis.readDefaultVal(),
                relayDeactivationHysteresis = systemEquip.dabRelayDeactivationHysteresis.readDefaultVal(),
                humidityHysteresis = systemEquip.dabHumidityHysteresis.readPriorityVal()
        )
    }

    private fun getAhuSettings(): AhuSettings {
        return AhuSettings(
                systemEquip = systemEquip.cmEquip,
                connectEquip1 = systemEquip.connectEquip1,
                conditioningMode = conditioningMode,
                isMechanicalCoolingAvailable = !(systemEquip.mechanicalCoolingAvailable.readHisVal() > 0),
                isMechanicalHeatingAvailable = !(systemEquip.mechanicalHeatingAvailable.readHisVal() > 0),
                isEmergencyShutoffActive = isEmergencyShutoffActive()
        )
    }

    private fun updateLoopOpPoints() {
        systemEquip.coolingLoopOutput.writeHisVal(systemCoolingLoopOp)
        systemEquip.heatingLoopOutput.writeHisVal(systemHeatingLoopOp)
        systemEquip.fanLoopOutput.writeHisVal(systemFanLoopOp)
        systemEquip.co2LoopOutput.writeHisVal(systemCo2LoopOp)
        systemEquip.cmEquip.satCoolingLoopOutput.writeHisVal(systemSatCoolingLoopOp)
        systemEquip.cmEquip.satHeatingLoopOutput.writeHisVal(systemSatHeatingLoopOp)
        systemEquip.cmEquip.fanPressureLoopOutput.writeHisVal(staticPressureFanLoopOp)
        systemEquip.operatingMode.writeHisVal(DabSystemController.getInstance().systemState.ordinal.toDouble())
        systemEquip.cmEquip.co2BasedDamperControl.writeHisVal(systemCo2LoopOp)
        systemEquip.connectEquip1.let {
            it.coolingLoopOutput.writeHisVal(systemCoolingLoopOp)
            it.heatingLoopOutput.writeHisVal(systemHeatingLoopOp)
            it.fanLoopOutput.writeHisVal(systemFanLoopOp)
            it.co2LoopOutput.writeHisVal(systemCo2LoopOp)
        }
    }

    private fun resetLoops() {
        systemCoolingLoopOp = 0.0
        systemHeatingLoopOp = 0.0
        systemFanLoopOp = 0.0
        systemCo2LoopOp = 0.0
        systemSatCoolingLoopOp = 0.0
        systemSatHeatingLoopOp = 0.0
        staticPressureFanLoopOp = 0.0
        systemEquip.connectEquip1.let {
            systemCoolingLoopOp = 0.0
            systemHeatingLoopOp = 0.0
            systemFanLoopOp = 0.0
            systemCo2LoopOp = 0.0
        }
        updateLoopOpPoints()
    }

    private fun dumpLoops() {
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemCoolingLoopOp: $systemCoolingLoopOp " +
                "systemHeatingLoopOp: $systemHeatingLoopOp systemFanLoopOp: $systemFanLoopOp systemCo2LoopOp: " +
                "$systemCo2LoopOp systemSatCoolingLoopOp: $systemSatCoolingLoopOp systemSatHeatingLoopOp: " +
                "$systemSatHeatingLoopOp staticPressureFanLoopOp: $staticPressureFanLoopOp")
        satHeatingPILoop.dumpWithTag(L.TAG_CCU_SYSTEM+":satHeatingPILoop")
        satCoolingPILoop.dumpWithTag(L.TAG_CCU_SYSTEM+":satCoolingPILoop")
        staticPressureFanPILoop.dumpWithTag(L.TAG_CCU_SYSTEM+":staticPressureFanPILoop")
        logOutputs()
    }

    private fun getSystemCoolingLoop(): Double {
        return if (DabSystemController.getInstance().getSystemState() == SystemController.State.COOLING) {
            DabSystemController.getInstance().getCoolingSignal().toDouble()
        } else {
            0.0
        }
    }

    private fun getHeatingLoop(): Double {
        return if (DabSystemController.getInstance().getSystemState() == SystemController.State.HEATING) {
            DabSystemController.getInstance().getHeatingSignal().toDouble()
        } else {
            0.0
        }
    }


    private fun getFanLoop(): Double {
        val analogFanSpeedMultiplier = systemEquip.dabAnalogFanSpeedMultiplier.readPriorityVal()
        val epidemicMode = systemEquip.epidemicModeSystemState.readHisVal()
        val epidemicState = EpidemicState.values()[epidemicMode.toInt()]

        return if ((epidemicState == EpidemicState.PREPURGE
                        || epidemicState == EpidemicState.POSTPURGE) && L.ccu().oaoProfile != null) {
            //TODO- Part OAO. Will be replaced with domanName later.
            val smartPurgeDabFanLoopOp = TunerUtil.readTunerValByQuery("system and purge and dab and fan and loop and output", L.ccu().oaoProfile.equipRef)
            val spSpMax = systemEquip.staticPressureSPMax.readPriorityVal()
            val spSpMin = systemEquip.staticPressureSPMin.readPriorityVal()
            CcuLog.d(L.TAG_CCU_SYSTEM, "spSpMax :$spSpMax spSpMin: $spSpMin SP: $staticPressure,$smartPurgeDabFanLoopOp")
            val staticPressureLoopOutput = ((staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
            if (DabSystemController.getInstance().getSystemState() == SystemController.State.COOLING
                    && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)) {
                if (staticPressureLoopOutput < (spSpMax - spSpMin) * smartPurgeDabFanLoopOp) {
                    (spSpMax - spSpMin) * smartPurgeDabFanLoopOp
                } else {
                    ((staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
                }
            } else if (DabSystemController.getInstance().getSystemState() == SystemController.State.HEATING
                    && (conditioningMode == SystemMode.HEATONLY || conditioningMode == SystemMode.AUTO)) {
                (DabSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier).toInt().toDouble().coerceAtLeast(smartPurgeDabFanLoopOp)
            } else {
                smartPurgeDabFanLoopOp
            }
        } else if (DabSystemController.getInstance().getSystemState() == SystemController.State.COOLING
                && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)) {
            (DabSystemController.getInstance().getCoolingSignal() * analogFanSpeedMultiplier).toInt().toDouble()
        } else if (DabSystemController.getInstance().getSystemState() == SystemController.State.HEATING
                && (conditioningMode == SystemMode.HEATONLY || conditioningMode == SystemMode.AUTO)) {
            (DabSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier).toInt().toDouble()
        } else {
            0.0
        }
    }

    private fun getCo2Loop(): Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.CO2_DAMPER)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "CO2_DAMPER control not enabled")
            return 0.0
        }
        val co2SensorVal = getCo2ControlPoint()
        val co2DamperOpeningRate = systemEquip.cmEquip.co2DamperOpeningRate.readDefaultVal()
        val co2Threshold = systemEquip.cmEquip.co2Threshold.readDefaultVal()
        CcuLog.d(L.TAG_CCU_SYSTEM, "co2SensorVal :$co2SensorVal co2Threshold: $co2Threshold co2DamperOpeningRate: $co2DamperOpeningRate")
        return if (DabSystemController.getInstance().getSystemState() == SystemController.State.OFF) {
            0.0
        } else ((co2SensorVal - co2Threshold) / co2DamperOpeningRate).coerceIn(0.0, 100.0)
    }


    private fun getSystemSatCoolingLoop(): Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Sat cooling control not enabled")
            return 0.0
        }
        val satControlPoint = getSatControlPoint()
        return if (DabSystemController.getInstance().getSystemState() == SystemController.State.COOLING && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO) && (systemEquip.mechanicalCoolingAvailable.readHisVal() > 0)) {
            val satSpMax = systemEquip.cmEquip.systemCoolingSatMax.readDefaultVal()
            val satSpMin = systemEquip.cmEquip.systemCoolingSatMin.readDefaultVal()
            val coolingSatSp = roundOff(satSpMax - systemCoolingLoopOp * (satSpMax - satSpMin) / 100)
            systemEquip.cmEquip.airTempCoolingSp.writeHisVal(coolingSatSp)
            systemEquip.cmEquip.airTempHeatingSp.writeHisVal(systemEquip.cmEquip.systemHeatingSatMin.readDefaultVal())
            CcuLog.d(L.TAG_CCU_SYSTEM, "coolingSatSpMax :$satSpMax coolingSatSpMin: $satSpMin satSensorVal $satControlPoint coolingSatSp: $coolingSatSp")
            if (systemCoolingLoopOp > 0) {
                satCoolingPILoop.getLoopOutput(satControlPoint, coolingSatSp)
            } else {
                0.0
            }
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM, "airTempCoolingSp : ${systemEquip.cmEquip.systemCoolingSatMax.readDefaultVal()}")
            systemEquip.cmEquip.airTempCoolingSp.writeHisVal(systemEquip.cmEquip.systemCoolingSatMax.readDefaultVal())
            0.0
        }
    }


    private fun getSystemSatHeatingLoop(): Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Sat heating control not enabled")
            return 0.0
        }
        val satControlPoint = getSatControlPoint()
        return if (DabSystemController.getInstance().getSystemState() == SystemController.State.HEATING && (conditioningMode == SystemMode.HEATONLY || conditioningMode == SystemMode.AUTO) && (systemEquip.mechanicalHeatingAvailable.readHisVal() > 0)) {
            val satSpMax = systemEquip.cmEquip.systemHeatingSatMax.readDefaultVal()
            val satSpMin = systemEquip.cmEquip.systemHeatingSatMin.readDefaultVal()
            val heatingSatSp = roundOff(getModulatedOutput(systemHeatingLoopOp, satSpMin, satSpMax))
            systemEquip.cmEquip.airTempHeatingSp.writeHisVal(heatingSatSp)
            systemEquip.cmEquip.airTempCoolingSp.writeHisVal(systemEquip.cmEquip.systemCoolingSatMax.readDefaultVal())
            CcuLog.d(L.TAG_CCU_SYSTEM, "satSpMax :$satSpMax satSpMin: $satSpMin satSensorVal $satControlPoint heatingSatSp: $heatingSatSp")
            if (systemHeatingLoopOp > 0) {
                satHeatingPILoop.getLoopOutput(heatingSatSp, satControlPoint)
            } else {
                0.0
            }
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM, "airTempHeatingSp : ${systemEquip.cmEquip.systemHeatingSatMin.readDefaultVal()}")
            systemEquip.cmEquip.airTempHeatingSp.writeHisVal(systemEquip.cmEquip.systemHeatingSatMin.readDefaultVal())
            0.0
        }
    }

    private fun getSystemStaticPressureFanLoop(): Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "PRESSURE_FAN control not enabled")
            return 0.0
        }
        val staticPressureControlPoint = getStaticPressureControlPoint()
        val staticPressureMin = systemEquip.cmEquip.staticPressureMin.readDefaultVal()
        val staticPressureMax = systemEquip.cmEquip.staticPressureMax.readDefaultVal()
        val staticPressureSp = roundOff(getModulatedOutput(fanLoopOp, staticPressureMin, staticPressureMax))
        systemEquip.cmEquip.ductStaticPressureSetpoint.writeHisVal(staticPressureSp)
        CcuLog.d(L.TAG_CCU_SYSTEM, "staticPressureMin :$staticPressureMin staticPressureMax: $staticPressureMax staticPressureSensorVal $staticPressureControlPoint staticPressureSp: $staticPressureSp")
        return staticPressureFanPILoop.getLoopOutput(staticPressureSp, staticPressureControlPoint)
    }


    /**
     * This will be one of the sensors selected by SupplyAirTempControlTemp options.
     * SAT1, SAT2, SAT3, AverageSAT, MinSAT, MaxSAT etc.
     */
    fun getSatControlPoint(): Double {
        val satControlType = systemEquip.cmEquip.supplyAirTempControlOn.readDefaultVal()
        return satControlIndexToDomainPoint(satControlType.toInt(), systemEquip.cmEquip).readHisVal()
    }

    /**
     * This will be one of the sensors selected by Pressure Based Fan control options.
     * DSP1, DSP2, DSP3, AveragePressure. MinPressure, MaxPressure etc.
     */
    fun getStaticPressureControlPoint(): Double {
        val pressureControlType = systemEquip.cmEquip.pressureBasedFanControlOn.readDefaultVal()
        return pressureFanControlIndexToDomainPoint(pressureControlType.toInt(), systemEquip.cmEquip)?.readHisVal()
                ?: 0.0
    }

    /**
     * This will be one of the sensors selected by Co2 Based Damper control options.
     * ZoneAvgCo2, ReturnAirCo2,MixedAirCo2 etc.
     */
    private fun getCo2ControlPoint(): Double {
        val co2ControlType = systemEquip.cmEquip.co2BasedDamperControlOn.readDefaultVal()
        return co2DamperControlTypeToDomainPoint(co2ControlType.toInt(), systemEquip.cmEquip).readHisVal()
    }

    private fun updateSystemStatus() {
        val systemStatus = statusMessage
        val scheduleStatus = ScheduleManager.getInstance().systemStatusString
        CcuLog.d(L.TAG_CCU_SYSTEM, "StatusMessage: $systemStatus")
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: $scheduleStatus")
        if (systemEquip.equipStatusMessage.readDefaultStrVal() != systemStatus) {
            systemEquip.equipStatusMessage.writeDefaultVal(systemStatus)
            Globals.getInstance().applicationContext.sendBroadcast(Intent(ScheduleUtil.ACTION_STATUS_CHANGE))
        }
        if (systemEquip.equipScheduleStatus.readDefaultStrVal() != scheduleStatus) {
            systemEquip.equipScheduleStatus.writeDefaultVal(scheduleStatus)
        }
    }


    override fun getStatusMessage(): String {
        if (advancedAhuImpl.isEmergencyShutOffEnabledAndActive(systemEquip.cmEquip, systemEquip.connectEquip1)) return "Emergency Shut Off mode is active"
        val systemStatus = StringBuilder().apply {
            append(if (systemEquip.cmEquip.loadFanStage1.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage1.readHisVal() > 0 || systemEquip.connectEquip1.loadFanStage1.readHisVal() > 0) "1" else "")
            append(if (systemEquip.cmEquip.loadFanStage2.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage2.readHisVal() > 0 || systemEquip.connectEquip1.loadFanStage2.readHisVal() > 0) ",2" else "")
            append(if (systemEquip.cmEquip.loadFanStage3.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage3.readHisVal() > 0 || systemEquip.connectEquip1.loadFanStage3.readHisVal() > 0) ",3" else "")
            append(if (systemEquip.cmEquip.loadFanStage4.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage4.readHisVal() > 0 || systemEquip.connectEquip1.loadFanStage4.readHisVal() > 0) ",4" else "")
            append(if (systemEquip.cmEquip.loadFanStage5.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage5.readHisVal() > 0 || systemEquip.connectEquip1.loadFanStage5.readHisVal() > 0) ",5" else "")
        }
        if (systemStatus.isNotEmpty()) {
            if (systemStatus[0] == ',') {
                systemStatus.deleteCharAt(0)
            }
            systemStatus.insert(0, "Fan Stage ")
            systemStatus.append(" ON ")
        }
        val coolingStatus = StringBuilder().apply {
            append(if (systemEquip.cmEquip.loadCoolingStage1.readHisVal() > 0 || systemEquip.cmEquip.satCoolingStage1.readHisVal() > 0 || systemEquip.connectEquip1.loadCoolingStage1.readHisVal() > 0) "1" else "")
            append(if (systemEquip.cmEquip.loadCoolingStage2.readHisVal() > 0 || systemEquip.cmEquip.satCoolingStage2.readHisVal() > 0 || systemEquip.connectEquip1.loadCoolingStage2.readHisVal() > 0) ",2" else "")
            append(if (systemEquip.cmEquip.loadCoolingStage3.readHisVal() > 0 || systemEquip.cmEquip.satCoolingStage3.readHisVal() > 0 || systemEquip.connectEquip1.loadCoolingStage3.readHisVal() > 0) ",3" else "")
            append(if (systemEquip.cmEquip.loadCoolingStage4.readHisVal() > 0 || systemEquip.cmEquip.satCoolingStage4.readHisVal() > 0 || systemEquip.connectEquip1.loadCoolingStage4.readHisVal() > 0) ",4" else "")
            append(if (systemEquip.cmEquip.loadCoolingStage5.readHisVal() > 0 || systemEquip.cmEquip.satCoolingStage5.readHisVal() > 0 || systemEquip.connectEquip1.loadCoolingStage5.readHisVal() > 0) ",5" else "")
        }
        if (coolingStatus.isNotEmpty()) {
            if (coolingStatus[0] == ',') {
                coolingStatus.deleteCharAt(0)
            }
            coolingStatus.insert(0, "Cooling Stage ")
            coolingStatus.append(" ON ")
        }

        val heatingStatus = StringBuilder().apply {
            append(if (systemEquip.cmEquip.loadHeatingStage1.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage1.readHisVal() > 0 || systemEquip.connectEquip1.loadHeatingStage1.readHisVal() > 0) "1" else "")
            append(if (systemEquip.cmEquip.loadHeatingStage2.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage2.readHisVal() > 0 || systemEquip.connectEquip1.loadHeatingStage2.readHisVal() > 0) ",2" else "")
            append(if (systemEquip.cmEquip.loadHeatingStage3.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage3.readHisVal() > 0 || systemEquip.connectEquip1.loadHeatingStage3.readHisVal() > 0) ",3" else "")
            append(if (systemEquip.cmEquip.loadHeatingStage4.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage4.readHisVal() > 0 || systemEquip.connectEquip1.loadHeatingStage4.readHisVal() > 0) ",4" else "")
            append(if (systemEquip.cmEquip.loadHeatingStage5.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage5.readHisVal() > 0 || systemEquip.connectEquip1.loadHeatingStage5.readHisVal() > 0) ",5" else "")
        }
        if (heatingStatus.isNotEmpty()) {
            if (heatingStatus[0] == ',') {
                heatingStatus.deleteCharAt(0)
            }
            heatingStatus.insert(0, "Heating Stage ")
            heatingStatus.append(" ON ")
        }

        if (systemCoolingLoopOp > 0 && L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable) {
            systemStatus.insert(0, "Free Cooling Used | ")
        }

        val humidifierStatus = getHumidifierStatus()
        val dehumidifierStatus = getDehumidifierStatus()

        val analogStatus = StringBuilder()
        if ((analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_FAN) && systemFanLoopOp > 0) || (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN) && staticPressureFanLoopOp > 0)) {
            analogStatus.append("| Fan ON ")
        }
        if ((systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) && ((analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_COOLING) && systemCoolingLoopOp > 0) || (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING) && systemSatCoolingLoopOp > 0))) {
            analogStatus.append("| Cooling ON ")
        }
        if ((systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) && ((analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_HEATING) && systemHeatingLoopOp > 0) || (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING) && systemSatHeatingLoopOp > 0))) {
            analogStatus.append("| Heating ON ")
        }
        if (analogStatus.isNotEmpty()) {
            analogStatus.insert(0, " | Analog ")
        }
        systemStatus.append(coolingStatus).append(heatingStatus).append(analogStatus)

        return if (systemStatus.toString() == "") "System OFF$humidifierStatus$dehumidifierStatus" else systemStatus.toString() + humidifierStatus + dehumidifierStatus
    }

    private fun getHumidifierStatus(): String {
        return if (systemEquip.cmEquip.humidifierEnable.pointExists()) {
            if (systemEquip.cmEquip.humidifierEnable.readHisVal() > 0) {
                " | Humidifier ON "
            } else " | Humidifier OFF "
        } else {
            ""
        }
    }

    private fun getDehumidifierStatus(): String {
        return if (systemEquip.cmEquip.dehumidifierEnable.pointExists()) {
            if (systemEquip.cmEquip.dehumidifierEnable.readHisVal() > 0) {
                " | Dehumidifier ON "
            } else " | Dehumidifier OFF "
        } else {
            ""
        }
    }

    fun updateStagesSelected() {
        coolingStages = 0
        heatingStages = 0
        fanStages = 0

        updateStagesForRelayMapping(getCMRelayAssociationMap(systemEquip.cmEquip))
        updateStagesForRelayMapping(getConnectRelayAssociationMap(systemEquip.connectEquip1))

        if (heatingStages > 0) {
            heatingStages -= Stage.COOLING_5.ordinal
        }
        if (fanStages > 0) {
            fanStages -= Stage.HEATING_5.ordinal
        }
        analogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls(systemEquip.cmEquip, systemEquip.connectEquip1)
        CcuLog.d(L.TAG_CCU_SYSTEM, "Cooling stages : $coolingStages Heating stages : $heatingStages Fan stages : $fanStages")
    }

    private fun updateStagesForRelayMapping(relayMapping: Map<Point, Point>) {
        relayMapping.forEach { (relay: Point, association: Point) ->
            if (relay.readDefaultVal() > 0) {
                var associationIndex = association.readDefaultVal().toInt()
                if (satCoolIndexRange.contains(associationIndex) || satHeatIndexRange.contains(associationIndex) || loadFanIndexRange.contains(associationIndex)) {
                    associationIndex -= 17
                }
                if (coolIndexRange.contains(associationIndex) && associationIndex >= coolingStages) {
                    coolingStages = associationIndex + 1
                } else if (heatIndexRange.contains(associationIndex) && associationIndex >= heatingStages) {
                    heatingStages = associationIndex
                } else if (fanIndexRange.contains(associationIndex) && associationIndex >= fanStages) {
                    fanStages = associationIndex
                }
            }
        }
    }

    override fun getLogicalPhysicalMap(): Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        map[systemEquip.cmEquip.relay1OutputEnable] = Domain.cmBoardDevice.relay1
        map[systemEquip.cmEquip.relay2OutputEnable] = Domain.cmBoardDevice.relay2
        map[systemEquip.cmEquip.relay3OutputEnable] = Domain.cmBoardDevice.relay3
        map[systemEquip.cmEquip.relay4OutputEnable] = Domain.cmBoardDevice.relay4
        map[systemEquip.cmEquip.relay5OutputEnable] = Domain.cmBoardDevice.relay5
        map[systemEquip.cmEquip.relay6OutputEnable] = Domain.cmBoardDevice.relay6
        map[systemEquip.cmEquip.relay7OutputEnable] = Domain.cmBoardDevice.relay7
        map[systemEquip.cmEquip.relay8OutputEnable] = Domain.cmBoardDevice.relay8
        return map
    }

    private fun getAnalogOutLogicalPhysicalMap(): Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        map[systemEquip.cmEquip.analog1OutputEnable] = Domain.cmBoardDevice.analog1Out
        map[systemEquip.cmEquip.analog2OutputEnable] = Domain.cmBoardDevice.analog2Out
        map[systemEquip.cmEquip.analog3OutputEnable] = Domain.cmBoardDevice.analog3Out
        map[systemEquip.cmEquip.analog4OutputEnable] = Domain.cmBoardDevice.analog4Out

        return map
    }

    private fun updateOutputPorts() {
        //TODO - refactor the encapsulation of relayAssociationMap and analogOutAssociationMap
        updateRelayOutputPorts(getCMRelayAssociationMap(systemEquip.cmEquip), false)
        updateAnalogOutputPorts(getCMAnalogAssociationMap(systemEquip.cmEquip), getAnalogOutLogicalPhysicalMap(), false)

        CcuLog.d(L.TAG_CCU_SYSTEM, "updateOutputPorts : Connect")
        if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
            updateRelayOutputPorts(getConnectRelayAssociationMap(systemEquip.connectEquip1), true)
            updateAnalogOutputPorts(getConnectAnalogAssociationMap(systemEquip.connectEquip1), getConnectAnalogOutLogicalPhysicalMap(
                    systemEquip.connectEquip1,
                    Domain.connect1Device,
            ), true)
        }
    }

    private fun updateRelayOutputPorts(associationMap: Map<Point, Point>, isConnectEquip: Boolean) {
        CcuLog.d(L.TAG_CCU_SYSTEM, "updateRelayOutputPorts")
        associationMap.forEach { (relay: Point, association: Point) ->
            if (relay.readDefaultVal() > 0) {
                try {
                    val (logicalPoint, relayOutput) = advancedAhuImpl.getAdvancedAhuRelayState(association, coolingStages, heatingStages, fanStages, isSystemOccupied, isConnectEquip, ahuSettings, ahuTuners, isAllowToActiveStage1Fan())
                    CcuLog.d(L.TAG_CCU_SYSTEM, "New relayOutput " + relay.domainName + " " + association.domainName + " " + logicalPoint.domainName + " " + relayOutput)
                    val stageIndex = association.readDefaultVal().toInt()
                    if (isConnectEquip) {
                        connectStageStatus[stageIndex] = Pair(logicalPoint.readHisVal().toInt(), if (relayOutput) 1 else 0)
                    } else {
                        cmStageStatus[stageIndex] = Pair(logicalPoint.readHisVal().toInt(), if (relayOutput) 1 else 0)
                    }
                } catch (e: Exception) {
                    CcuLog.e(L.TAG_CCU_SYSTEM, "Error in updateOutputPorts ${relay.domainName}", e)
                }
            }
        }
        if (isConnectEquip) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Connect Stage Status :" + connectStageStatus.joinToString { "${it.first}:${it.second}" })
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM, "CM Stage Status :" + cmStageStatus.joinToString { "${it.first}:${it.second}" })
        }
        updatePointsDbVal(isConnectEquip)
    }


    private fun updateAnalogOutputPorts(associationMap: Map<Point, Point>, physicalMap: Map<Point, PhysicalPoint>?, isConnectEquip: Boolean) {
        associationMap.forEach { (analogOut: Point, association: Point) ->
            if (analogOut.readDefaultVal() > 0) {
                val domainEquip = if (isConnectEquip) systemEquip.connectEquip1 else systemEquip.cmEquip
                val (physicalValue, logicalValue) = advancedAhuImpl.getAnalogLogicalPhysicalValue(analogOut, association, ahuSettings, domainEquip)

                CcuLog.d(L.TAG_CCU_SYSTEM, "New analogOutValue ${analogOut.domainName} physicalValue: $physicalValue logicalValue: $logicalValue")
                physicalMap?.get(analogOut)?.writeHisVal(physicalValue)

                if (isConnectEquip) {
                    val domainName = connectAnalogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    getDomainPointForName(domainName, systemEquip.connectEquip1).writeHisVal(logicalValue)
                } else {
                    val domainName = analogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    getDomainPointForName(domainName, systemEquip.cmEquip).writeHisVal(logicalValue)
                }
            }
        }
    }

    private fun updateDerivedSensorPoints() {
        if (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN)) {
            updatePressureSensorDerivedPoints()
        }
        if (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING) || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING)) {
            updateTemperatureSensorDerivedPoints(ahuSettings.systemEquip)
        }
        if (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.CO2_DAMPER)) {
            if (systemEquip.cmEquip.zoneAvgCo2.pointExists()) {
                systemEquip.cmEquip.zoneAvgCo2.writeHisVal(DabSystemController.getInstance().systemCO2WA)
            }
        }
    }


    private fun updatePointsDbVal(isConnectEquip: Boolean) {
        val stageStatus = if (isConnectEquip) connectStageStatus else cmStageStatus
        stageStatus.forEachIndexed { index, status ->
            val domainName = if (isConnectEquip) connectRelayAssociationToDomainName(index) else relayAssociationToDomainName(index)
            if (status.first < status.second) {
                //Stage is going up
                val associationType = relayAssociationDomainNameToType(domainName)
                if (associationType.isConditioningStage() && isConditioningActive(associationType)) {
                    if (associationType.isLoadStage()) {
                        if (!isStageUpTimerActive()) {
                            updatePointVal(domainName, index, status.second, isConnectEquip)
                            activateStageUpTimer()
                        } else {
                            CcuLog.d(L.TAG_CCU_SYSTEM, "Stage up ignored for $domainName counter: $stageUpTimer")
                        }
                    }
                    if (associationType.isSatStage()) {
                        if (!isSatStageUpTimerActive()) {
                            updatePointVal(domainName, index, status.second, isConnectEquip)
                            activateSatStageUpTimer()
                        } else {
                            CcuLog.d(L.TAG_CCU_SYSTEM, "Stage up ignored for $domainName counter: $satStageUpTimer")
                        }
                    }
                } else {
                    updatePointVal(domainName, index, status.second, isConnectEquip)
                }
            }
        }

        stageStatus.reversed().forEachIndexed { pos, status ->
            val index = (stageStatus.size - 1) - pos
            val domainName = if (isConnectEquip) connectRelayAssociationToDomainName(index) else relayAssociationToDomainName(index)
            if (status.first > status.second) {
                val associationType = relayAssociationDomainNameToType(domainName)
                if (associationType.isConditioningStage() && isConditioningActive(associationType)) {
                    CcuLog.d(L.TAG_CCU_SYSTEM, "Stage down detected for $domainName")
                    if (associationType.isLoadStage()) {
                        if (!isStageDownTimerActive()) {
                            updatePointVal(domainName, index, status.second, isConnectEquip)
                            activateStageDownTimer()
                        } else {
                            CcuLog.d(L.TAG_CCU_SYSTEM, "Stage down ignored for $domainName counter: $stageDownTimer")
                        }
                    }
                    if (associationType.isSatStage()) {
                        if (!isSatStageDownTimerActive()) {
                            updatePointVal(domainName, index, status.second, isConnectEquip)
                            activateSatStageDownTimer()
                        } else {
                            CcuLog.d(L.TAG_CCU_SYSTEM, "Stage down ignored for $domainName counter: $satStageDownTimer")
                        }
                    }
                } else {
                    updatePointVal(domainName, index, status.second, isConnectEquip)
                }
            }
        }
        updateLogicalToPhysical(isConnectEquip)
    }


    private fun updateLogicalToPhysical(isConnectEquip: Boolean) {
        if (isConnectEquip) {
            getConnectRelayAssociationMap(systemEquip.connectEquip1).forEach { (relay, association) ->
                if (relay.readDefaultVal() > 0) {
                    val domainName = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                    val physicalPoint = getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]
                    physicalPoint?.writeHisVal(getDomainPointForName(domainName, systemEquip.connectEquip1).readHisVal())
                }
            }
        } else {
            // sync logical to physical
            getCMRelayAssociationMap(systemEquip.cmEquip).forEach { (relay, association) ->
                if (relay.readDefaultVal() > 0) {
                    val domainName = relayAssociationToDomainName(association.readDefaultVal().toInt())
                    getCMRelayLogicalPhysicalMap(systemEquip.cmEquip)[relay]?.writeHisVal(getDomainPointForName(domainName, systemEquip.cmEquip).readHisVal())
                }
            }
        }
    }


    private fun isConditioningActive(type: AdvancedAhuRelayAssociationType): Boolean {
        return when (type) {
            AdvancedAhuRelayAssociationType.LOAD_COOLING, AdvancedAhuRelayAssociationType.SAT_COOLING -> DabSystemController.getInstance().getSystemState() == SystemController.State.COOLING

            AdvancedAhuRelayAssociationType.LOAD_HEATING, AdvancedAhuRelayAssociationType.SAT_HEATING -> DabSystemController.getInstance().getSystemState() == SystemController.State.HEATING

            else -> false
        }
    }

    private fun activateStageUpTimer() {
        stageUpTimer = systemEquip.dabStageUpTimerCounter.readPriorityVal()
    }

    private fun activateStageDownTimer() {
        stageDownTimer = systemEquip.dabStageDownTimerCounter.readPriorityVal()
    }

    private fun activateSatStageUpTimer() {
        satStageUpTimer = systemEquip.dabStageUpTimerCounter.readPriorityVal()
    }

    private fun activateSatStageDownTimer() {
        satStageDownTimer = systemEquip.dabStageDownTimerCounter.readPriorityVal()
    }

    private fun isSatStageUpTimerActive(): Boolean = satStageUpTimer > 0
    private fun isSatStageDownTimerActive(): Boolean = satStageDownTimer > 0

    private fun isStageUpTimerActive(): Boolean = stageUpTimer > 0
    private fun isStageDownTimerActive(): Boolean = stageDownTimer > 0

    private fun updatePointVal(domainName: String, stageIndex: Int, pointVal: Int, isConnectEquip: Boolean) {
        if (isConnectEquip) {
            updateConnectPointVal(domainName, stageIndex, pointVal)
        } else {
            updateCMPointVal(domainName, stageIndex, pointVal)
        }
    }

    private fun updateCMPointVal(domainName: String, stageIndex: Int, pointVal: Int) {
        getDomainPointForName(domainName, systemEquip.cmEquip).writeHisVal(pointVal.toDouble())
        getCMRelayAssociationMap(systemEquip.cmEquip).forEach { (relay, association) ->
            if (association.readDefaultVal() == stageIndex.toDouble() && relay.readDefaultVal() > 0) {
                getCMRelayLogicalPhysicalMap(systemEquip.cmEquip)[relay]?.writeHisVal(pointVal.toDouble())
            }
        }
    }

    private fun updateConnectPointVal(domainName: String, stageIndex: Int, pointVal: Int) {
        getDomainPointForName(domainName, systemEquip.connectEquip1).writeHisVal(pointVal.toDouble())
        getConnectRelayAssociationMap(systemEquip.connectEquip1).forEach { (relay, association) ->
            if (association.readDefaultVal() == stageIndex.toDouble() && relay.readDefaultVal() > 0) {
                getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]?.writeHisVal(pointVal.toDouble())
            }
        }
    }


    @SuppressLint("SuspiciousIndentation")
    fun getUnit(domainName: String): String {
        domainName.readPoint(systemEquip.equipRef).let {
            return it["unit"].toString()
        }
    }

    private fun resetSystem() {
        reset() // Resetting PI the loop variables
        resetLoops()
        resetOutput()
    }

    private fun resetOutput() {
        // Resetting the relays , analog outs will be reset based on loops
        getCMRelayAssociationMap(systemEquip.cmEquip).entries.forEach { (relay, association) ->
            val domainName = relayAssociationToDomainName(association.readDefaultVal().toInt())
            getDomainPointForName(domainName, systemEquip.cmEquip).writeHisVal(0.0)
            getCMRelayLogicalPhysicalMap(systemEquip.cmEquip)[relay]?.writeHisVal(0.0)
        }

        updateAnalogOutputPorts(getCMAnalogAssociationMap(systemEquip.cmEquip), getAnalogOutLogicalPhysicalMap(), false)

        if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
            getConnectRelayAssociationMap(systemEquip.connectEquip1).entries.forEach { (relay, association) ->
                val domainName = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                getDomainPointForName(domainName, systemEquip.connectEquip1).writeHisVal(0.0)
                getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]?.writeHisVal(0.0)
            }

            updateAnalogOutputPorts(getConnectAnalogAssociationMap(systemEquip.connectEquip1), getConnectAnalogOutLogicalPhysicalMap(
                    systemEquip.connectEquip1,
                    Domain.connect1Device,
            ), true)
        }
    }


    private fun logOutputs() {
        try {
            CcuLog.i(L.TAG_CCU_SYSTEM, "Test mode is active ${Globals.getInstance().isTestMode}")
            CcuLog.i(L.TAG_CCU_SYSTEM, "Operating mode ${DabSystemController.getInstance().systemState}")
            CcuLog.i(L.TAG_CCU_SYSTEM, "Conditioning  mode $conditioningMode")
            getCMRelayAssociationMap(systemEquip.cmEquip).entries.forEach { (relay, association) ->
                val logical = relayAssociationToDomainName(association.readDefaultVal().toInt())
                CcuLog.i(L.TAG_CCU_SYSTEM, "CM ${relay.domainName}:${relay.readDefaultVal()}  => : " + "Physical Value: ${getCMRelayLogicalPhysicalMap(systemEquip.cmEquip)[relay]!!.readHisVal()}   " + "$logical : ${getDomainPointForName(logical, systemEquip.cmEquip).readHisVal()} ")
            }

            getCMAnalogAssociationMap(systemEquip.cmEquip).entries.forEach { (analogOut, association) ->
                val logical = analogOutAssociationToDomainName(association.readDefaultVal().toInt())
                CcuLog.i(L.TAG_CCU_SYSTEM, "CM ${analogOut.domainName}:${analogOut.readDefaultVal()} =>:  " + "Physical Value: ${getAnalogOutLogicalPhysicalMap()[analogOut]!!.readHisVal()}   " + "$logical : ${getDomainPointForName(logical, systemEquip.cmEquip).readHisVal()}")
            }

            if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
                getConnectRelayAssociationMap(systemEquip.connectEquip1).entries.forEach { (relay, association) ->
                    val logical = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                    CcuLog.i(L.TAG_CCU_SYSTEM, "Connect ${relay.domainName}:${relay.readDefaultVal()} => : " + "Physical Value: ${getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]!!.readHisVal()}  " + "$logical : ${getDomainPointForName(logical, systemEquip.connectEquip1).readHisVal()} ")
                }
                getConnectAnalogAssociationMap(systemEquip.connectEquip1).entries.forEach { (analogOut, association) ->
                    val logical = connectAnalogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    CcuLog.i(L.TAG_CCU_SYSTEM, "Connect ${analogOut.domainName}: ${analogOut.readDefaultVal()} => : " + "Physical Value: ${getConnectAnalogOutLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[analogOut]!!.readHisVal()}  " + "$logical : ${getDomainPointForName(logical, systemEquip.connectEquip1).readHisVal()} ")
                }
            }
        } catch (e: Exception) {
            CcuLog.e(L.TAG_CCU_SYSTEM, "Error in dumping loops", e)
        }
    }

    fun getOperatingMode(): String {
        return when (SystemController.State.values()[systemEquip.operatingMode.readHisVal().toInt()]) {
            SystemController.State.COOLING -> " Cooling"
            SystemController.State.HEATING -> " Heating"
            else -> {
                "OFF"
            }
        }
    }

    fun getUserIntentConfig(): UserIntentConfig {
        val userIntentConfig = UserIntentConfig()

        getCMAnalogAssociationMap(systemEquip.cmEquip).forEach { (analog: Point, association: Point) ->
            if (analog.readDefaultVal() > 0) {
                val analogOutAssociationType = AdvancedAhuAnalogOutAssociationType.values()[association.readDefaultVal().toInt()]
                if (analogOutAssociationType == AdvancedAhuAnalogOutAssociationType.SAT_HEATING) {
                    userIntentConfig.isSatHeatingAvailable = true
                }
                if (analogOutAssociationType == AdvancedAhuAnalogOutAssociationType.SAT_COOLING) {
                    userIntentConfig.isSatCoolingAvailable = true
                }
                if (analogOutAssociationType == AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN) {
                    userIntentConfig.isPressureControlAvailable = true
                }
                if (analogOutAssociationType == AdvancedAhuAnalogOutAssociationType.CO2_DAMPER) {
                    userIntentConfig.isCo2DamperControlAvailable = true
                }
            }
        }
        if (!userIntentConfig.isCo2DamperControlAvailable) {
            getConnectAnalogAssociationMap(systemEquip.connectEquip1).forEach { (analog, association) ->
                if (analog.readDefaultVal() > 0) {
                    val analogOutAssociationType = AdvancedAhuAnalogOutAssociationTypeConnect.values()[association.readDefaultVal().toInt()]
                    if (analogOutAssociationType == AdvancedAhuAnalogOutAssociationTypeConnect.CO2_DAMPER) {
                        userIntentConfig.isCo2DamperControlAvailable = true
                    }
                }
            }
        }
        return userIntentConfig
    }

    fun isEmergencyShutoffActive() : Boolean {
        return advancedAhuImpl.isEmergencyShutOffEnabledAndActive(systemEquip.cmEquip, systemEquip.connectEquip1)
    }

    fun getOccupancy(): Int = if (isSystemOccupied) 1 else 0

    fun setTestConfigs(port: Int) {
        testConfigs.set(port,true) // 0 - 7 Relays and 8 - 11 Analog
        CcuLog.d(L.TAG_CCU_SYSTEM, "Test Configs set for port $port cache $testConfigs")
    }

    private fun isAllowToActiveStage1Fan(): Boolean {
        return (systemCoolingLoopOp > 0 || systemSatCoolingLoopOp > 0 || systemFanLoopOp > 0 || systemHeatingLoopOp > 0 || systemSatHeatingLoopOp > 0 || staticPressureFanLoopOp > 0)
    }
}