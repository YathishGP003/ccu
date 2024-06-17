package a75f.io.logic.bo.building.system.vav

import a75.io.algos.ControlLoop
import a75.io.algos.vav.VavTRSystem
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.api.readPoint
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
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
import a75f.io.logic.bo.building.system.UserIntentConfig
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
import a75f.io.logic.bo.building.system.util.getModulatedOutput
import a75f.io.logic.tuners.TunerUtil
import android.annotation.SuppressLint
import android.content.Intent

open class VavAdvancedAhu : VavSystemProfile() {

    private var heatingStages = 0
    private var coolingStages = 0
    private var fanStages = 0
    lateinit var systemEquip: VavAdvancedHybridSystemEquip
    private lateinit var advancedAhuImpl: AdvancedAhuAlgoHandler

    private lateinit var cmStageStatus : Array<Pair<Int, Int>>
    private lateinit var connectStageStatus : Array<Pair<Int, Int>>
    private lateinit var conditioningMode : SystemMode
    private lateinit var ahuSettings: AhuSettings

    private var systemSatCoolingLoopOp = 0.0
    private var systemSatHeatingLoopOp = 0.0
    private var staticPressureFanLoopOp = 0.0

    private val satCoolingPILoop = ControlLoop()
    private val satHeatingPILoop = ControlLoop()
    private val staticPressureFanPILoop = ControlLoop()

    //TODO - revisit.
    private val coolIndexRange = 0..4
    private val heatIndexRange = 5..9
    private val fanIndexRange = 10..14
    private val satCoolIndexRange = 17..21
    private val satHeatIndexRange = 22..26
    private val loadFanIndexRange = 27..31
    private lateinit var analogControlsEnabled : Set<AdvancedAhuAnalogOutAssociationType>

    private var stageUpTimer = 0.0
    private var stageDownTimer = 0.0
    private fun initTRSystem() {
        trSystem = VavTRSystem()
    }

    override fun getProfileName(): String? {
        return "VAV Advanced Hybrid AHU v2"
    }

    override fun getProfileType(): ProfileType? {
        return ProfileType.SYSTEM_VAV_ADVANCED_AHU
    }
    override fun addSystemEquip() {
        systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
        advancedAhuImpl = AdvancedAhuAlgoHandler(systemEquip)
        initTRSystem()
        initializePILoop()
        analogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls()
    }

    fun updateDomainEquip(equip: VavAdvancedHybridSystemEquip) {
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
        if (needToUpdate(systemMode)) {
            systemEquip.conditioningMode.writeVal(HayStackConstants.DEFAULT_POINT_LEVEL,SystemMode.OFF.ordinal.toDouble())
            systemEquip.conditioningMode.writeHisVal(SystemMode.OFF.ordinal.toDouble())
        }
    }

    private fun needToUpdate(systemMode: SystemMode): Boolean {
        when(systemMode) {
            SystemMode.AUTO -> {
                if (isCoolingAvailable && isHeatingAvailable) {
                    return false
                }
            }
            SystemMode.COOLONLY -> {
                if (isCoolingAvailable) {
                    return false
                }
            }
            SystemMode.HEATONLY -> {
                if (isHeatingAvailable) {
                    return false
                }
            }
            else -> {
                return true
            }
        }
        return true
    }

    override fun deleteSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val systemEquip = hayStack.readEntity("system and equip and not modbus and not connectModule")
        if (systemEquip.isNotEmpty()) {
            hayStack.deleteEntity(systemEquip["id"].toString())
        }
        deleteSystemConnectModule(hayStack)
    }

    private fun deleteSystemConnectModule(hayStack: CCUHsApi) {
        val connectSystemEquip = hayStack.readEntity("domainName == \"" + DomainName.vavAdvancedHybridAhuV2_connectModule + "\"")
        if (connectSystemEquip.isNotEmpty()) {
            hayStack.deleteEntityTree(connectSystemEquip["id"].toString())
        }

        val connectDevice = hayStack.readEntity("domainName == \"" + DomainName.connectModuleDevice + "\"")
        if (connectDevice.isNotEmpty()) {
            hayStack.deleteEntityTree(connectDevice["id"].toString())
        }
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

    override fun getSystemSAT(): Int {
        return (trSystem as VavTRSystem).currentSAT
    }

    override fun getStaticPressure(): Double {
        return (trSystem as VavTRSystem).currentSp
    }
    override fun doSystemControl() {
        if (trSystem != null) {
            trSystem.processResetResponse()
        }
        VavSystemController.getInstance().runVavSystemControlAlgo()
        updateSystemPoints()
        setTrTargetVals()
    }

    private fun initializePILoop() {
        val proportionalGain = systemEquip.vavProportionalKFactor.readPriorityVal()
        val integralGain = systemEquip.vavIntegralKFactor.readPriorityVal()
        val proportionalRange = systemEquip.vavTemperatureProportionalRange.readPriorityVal()
        val integralTime = systemEquip.vavTemperatureIntegralTime.readPriorityVal()
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
        systemEquip = Domain.systemEquip as VavAdvancedHybridSystemEquip
        advancedAhuImpl = AdvancedAhuAlgoHandler(systemEquip)
        updateStagesSelected()
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())
        updateDerivedSensorPoints()
        conditioningMode = SystemMode.values()[systemEquip.conditioningMode.readPriorityVal().toInt()]
        cmStageStatus = Array(34) { Pair(0, 0) }
        connectStageStatus = Array(19) { Pair(0, 0) }

        if (stageUpTimer > 0) {
            stageUpTimer--
        } else if (stageDownTimer > 0) {
            stageDownTimer--
        }
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

        systemFanLoopOp = getFanLoop().coerceIn(0.0,100.0)

        if (AutoCommissioningUtil.isAutoCommissioningStarted()) {
            writeSystemLoopOutputValue(Tags.FAN, systemFanLoopOp)
            systemFanLoopOp = getSystemLoopOutputValue(Tags.FAN)
        }


        systemSatCoolingLoopOp = getSystemSatCoolingLoop().coerceIn(0.0, 100.0)
        systemSatHeatingLoopOp = getSystemSatHeatingLoop().coerceIn(0.0, 100.0)
        staticPressureFanLoopOp = getSystemStaticPressureFanLoop().coerceIn(0.0, 100.0)
        systemCo2LoopOp = if (isSystemOccupiedForDcv) getCo2Loop() else 0.0
        ahuSettings = getAhuSettings()

        if (advancedAhuImpl.isEmergencyShutOffEnabledAndActive() || conditioningMode == SystemMode.OFF) {
            resetSystem()
        } else {
            updateLoopOpPoints()
            updateOutputPorts()
        }
        dumpLoops()
        updateSystemStatus()
    }

    private fun getAhuSettings(): AhuSettings {
        return AhuSettings(
            systemEquip = systemEquip,
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
        systemEquip.satCoolingLoopOutput.writeHisVal(systemSatCoolingLoopOp)
        systemEquip.satHeatingLoopOutput.writeHisVal(systemSatHeatingLoopOp)
        systemEquip.fanPressureLoopOutput.writeHisVal(staticPressureFanLoopOp)
        systemEquip.operatingMode.writeHisVal(VavSystemController.getInstance().systemState.ordinal.toDouble())
        systemEquip.co2BasedDamperControl.writeHisVal(systemCo2LoopOp)
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
    private fun getSystemCoolingLoop() : Double {
        return if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            VavSystemController.getInstance().getCoolingSignal().toDouble()
        } else if (VavSystemController.getInstance().getSystemState() == SystemController.State.COOLING
            && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)
        ) {
            val satSpMax = systemEquip.satSPMax.readPriorityVal()
            val satSpMin = systemEquip.satSPMin.readPriorityVal()
            CcuLog.d(L.TAG_CCU_SYSTEM, "satSpMax :$satSpMax satSpMin: $satSpMin SAT: $systemSAT")
            ((satSpMax - systemSAT) * 100 / (satSpMax - satSpMin)).toInt().toDouble()
        } else {
            0.0
        }
    }

    private fun getHeatingLoop() : Double {
        return if (VavSystemController.getInstance().getSystemState() == SystemController.State.HEATING) {
            VavSystemController.getInstance().getHeatingSignal().toDouble()
        } else {
            0.0
        }
    }

    private fun getFanLoop() : Double {
        val analogFanSpeedMultiplier = systemEquip.vavAnalogFanSpeedMultiplier.readPriorityVal()
        val epidemicMode = systemEquip.epidemicModeSystemState.readHisVal()
        val epidemicState = EpidemicState.values()[epidemicMode.toInt()]

        return if (isSingleZoneTIMode(CCUHsApi.getInstance())) {
            getSingleZoneFanLoopOp(analogFanSpeedMultiplier)
        } else if ((epidemicState == EpidemicState.PREPURGE || epidemicState == EpidemicState.POSTPURGE) && L.ccu().oaoProfile != null) {
            //TODO- Part OAO. Will be replaced with domanName later.
            val smartPurgeDabFanLoopOp = TunerUtil.readTunerValByQuery(
                "system and purge and vav and fan and loop and output",
                L.ccu().oaoProfile.equipRef
            )
            val spSpMax = systemEquip.staticPressureSPMax.readPriorityVal()
            val spSpMin = systemEquip.staticPressureSPMin.readPriorityVal()
            CcuLog.d(
                L.TAG_CCU_SYSTEM,
                "spSpMax :$spSpMax spSpMin: $spSpMin SP: $staticPressure,$smartPurgeDabFanLoopOp"
            )
            val staticPressureLoopOutput = ((staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
            if (VavSystemController.getInstance().getSystemState() == SystemController.State.COOLING
                && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)) {
                if (staticPressureLoopOutput < (spSpMax - spSpMin) * smartPurgeDabFanLoopOp) {
                    (spSpMax - spSpMin) * smartPurgeDabFanLoopOp
                } else {
                    ((staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
                }
            } else if (VavSystemController.getInstance().getSystemState() == SystemController.State.HEATING) {
                (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier).toInt()
                    .toDouble().coerceAtLeast(smartPurgeDabFanLoopOp)
            } else {
                smartPurgeDabFanLoopOp
            }
        } else if (VavSystemController.getInstance().getSystemState() == SystemController.State.COOLING
            && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)) {
            val spSpMax = systemEquip.staticPressureSPMax.readPriorityVal()
            val spSpMin = systemEquip.staticPressureSPMin.readPriorityVal()
            CcuLog.d(L.TAG_CCU_SYSTEM, "spSpMax :$spSpMax spSpMin: $spSpMin SP: $staticPressure")
            ((staticPressure - spSpMin) * 100 / (spSpMax - spSpMin)).toInt().toDouble()
        } else if (VavSystemController.getInstance().getSystemState() == SystemController.State.HEATING) {
            (VavSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier).toInt()
                .toDouble()
        } else {
            0.0
        }
    }

    private fun getCo2Loop() : Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.CO2_DAMPER)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "CO2_DAMPER control not enabled")
            return 0.0
        }
        val co2SensorVal = getCo2ControlPoint()
        val co2DamperOpeningRate = systemEquip.co2DamperOpeningRate.readDefaultVal()
        val co2Threshold = systemEquip.co2Threshold.readDefaultVal()
        CcuLog.d(L.TAG_CCU_SYSTEM, "co2SensorVal :$co2SensorVal co2Threshold: $co2Threshold co2DamperOpeningRate: $co2DamperOpeningRate")
        return if (VavSystemController.getInstance().getSystemState() == SystemController.State.OFF) {
            0.0
        } else ((co2SensorVal - co2Threshold)/co2DamperOpeningRate).coerceIn(0.0, 100.0)
    }

    private fun getSystemSatCoolingLoop() : Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Sat cooling control not enabled")
            return 0.0
        }
        val satControlPoint = getSatControlPoint()
        return if (VavSystemController.getInstance().getSystemState() == SystemController.State.COOLING
                && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)
                && (systemEquip.mechanicalCoolingAvailable.readHisVal() > 0)) {
            val satSpMax = systemEquip.systemCoolingSatMax.readDefaultVal()
            val satSpMin = systemEquip.systemCoolingSatMin.readDefaultVal()
            val coolingSatSp = roundOff(satSpMax - systemCoolingLoopOp * (satSpMax - satSpMin) / 100)
            systemEquip.airTempCoolingSp.writeHisVal(coolingSatSp)
            systemEquip.airTempHeatingSp.writeHisVal(systemEquip.systemHeatingSatMin.readDefaultVal())
            CcuLog.d(L.TAG_CCU_SYSTEM, "coolingSatSpMax :$satSpMax coolingSatSpMin: " +
                    "$satSpMin satSensorVal $satControlPoint coolingSatSp: $coolingSatSp")
            if (systemCoolingLoopOp > 0) {
                satCoolingPILoop.getLoopOutput(satControlPoint, coolingSatSp)
            } else {
                0.0
            }
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM, "airTempCoolingSp : ${systemEquip.systemCoolingSatMax.readDefaultVal()}")
            systemEquip.airTempCoolingSp.writeHisVal(systemEquip.systemCoolingSatMax.readDefaultVal())
            0.0
        }
    }

    private fun getSystemSatHeatingLoop() : Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Sat heating control not enabled")
            return 0.0
        }
        val satControlPoint = getSatControlPoint()
        return if (VavSystemController.getInstance().getSystemState() == SystemController.State.HEATING
                && (conditioningMode == SystemMode.HEATONLY || conditioningMode == SystemMode.AUTO)
                && (systemEquip.mechanicalHeatingAvailable.readHisVal() > 0)) {
            val satSpMax = systemEquip.systemHeatingSatMax.readDefaultVal()
            val satSpMin = systemEquip.systemHeatingSatMin.readDefaultVal()
            val heatingSatSp = roundOff(getModulatedOutput(systemHeatingLoopOp, satSpMin, satSpMax))
            systemEquip.airTempHeatingSp.writeHisVal(heatingSatSp)
            systemEquip.airTempCoolingSp.writeHisVal(systemEquip.systemCoolingSatMax.readDefaultVal())
            CcuLog.d(L.TAG_CCU_SYSTEM, "satSpMax :$satSpMax satSpMin: $satSpMin " +
                    "satSensorVal $satControlPoint heatingSatSp: $heatingSatSp")
            if (systemHeatingLoopOp > 0) {
                satHeatingPILoop.getLoopOutput(heatingSatSp, satControlPoint)
            } else {
                0.0
            }
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM, "airTempHeatingSp : ${systemEquip.systemHeatingSatMin.readDefaultVal()}")
            systemEquip.airTempHeatingSp.writeHisVal(systemEquip.systemHeatingSatMin.readDefaultVal())
            0.0
        }
    }

    private fun getSystemStaticPressureFanLoop() : Double {
        if (!analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN)) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "PRESSURE_FAN control not enabled")
            return 0.0
        }
        val staticPressureControlPoint = getStaticPressureControlPoint()
        val staticPressureMin = systemEquip.staticPressureMin.readDefaultVal()
        val staticPressureMax = systemEquip.staticPressureMax.readDefaultVal()
        val staticPressureSp = roundOff(getModulatedOutput(fanLoopOp, staticPressureMin, staticPressureMax))
        systemEquip.ductStaticPressureSetpoint.writeHisVal(staticPressureSp)
        CcuLog.d(L.TAG_CCU_SYSTEM, "staticPressureMin :$staticPressureMin staticPressureMax: $staticPressureMax " +
                "staticPressureSensorVal $staticPressureControlPoint staticPressureSp: $staticPressureSp")
        return staticPressureFanPILoop.getLoopOutput(staticPressureSp, staticPressureControlPoint)
    }

    /**
     * This will be one of the sensors selected by SupplyAirTempControlTemp options.
     * SAT1, SAT2, SAT3, AverageSAT, MinSAT, MaxSAT etc.
     */
    fun getSatControlPoint() : Double {
        val satControlType = systemEquip.supplyAirTempControlOn.readDefaultVal()
        return satControlIndexToDomainPoint(satControlType.toInt(), systemEquip).readHisVal()
    }

    /**
     * This will be one of the sensors selected by Pressure Based Fan control options.
     * DSP1, DSP2, DSP3, AveragePressure. MinPressure, MaxPressure etc.
     */
    fun getStaticPressureControlPoint() : Double {
        val pressureControlType = systemEquip.pressureBasedFanControlOn.readDefaultVal()
        return pressureFanControlIndexToDomainPoint(pressureControlType.toInt(), systemEquip).readHisVal()
    }

    /**
     * This will be one of the sensors selected by Co2 Based Damper control options.
     * ZoneAvgCo2, ReturnAirCo2,MixedAirCo2 etc.
     */
    private fun getCo2ControlPoint() : Double {
        val co2ControlType = systemEquip.co2BasedDamperControlOn.readDefaultVal()
        return co2DamperControlTypeToDomainPoint(co2ControlType.toInt(), systemEquip).readHisVal()
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
        if (advancedAhuImpl.isEmergencyShutOffEnabledAndActive())
            return "Emergency Shut Off mode is active"
        val systemStatus = StringBuilder().apply {
            append(if (systemEquip.loadFanStage1.readHisVal() > 0 || systemEquip.fanPressureStage1.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadFanStage1.readHisVal() > 0 ) "1" else "")
            append(if (systemEquip.loadFanStage2.readHisVal() > 0 || systemEquip.fanPressureStage2.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadFanStage2.readHisVal() > 0) ",2" else "")
            append(if (systemEquip.loadFanStage3.readHisVal() > 0 || systemEquip.fanPressureStage3.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadFanStage3.readHisVal() > 0) ",3" else "")
            append(if (systemEquip.loadFanStage4.readHisVal() > 0 || systemEquip.fanPressureStage4.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadFanStage4.readHisVal() > 0) ",4" else "")
            append(if (systemEquip.loadFanStage5.readHisVal() > 0 || systemEquip.fanPressureStage5.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadFanStage5.readHisVal() > 0) ",5" else "")
        }
        if (systemStatus.isNotEmpty()) {
            if (systemStatus[0] == ',') {
                systemStatus.deleteCharAt(0)
            }
            systemStatus.insert(0, "Fan Stage ")
            systemStatus.append(" ON ")
        }
        val coolingStatus = StringBuilder().apply {
            append(if (systemEquip.loadCoolingStage1.readHisVal() > 0 || systemEquip.satCoolingStage1.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadCoolingStage1.readHisVal() > 0) "1" else "")
            append(if (systemEquip.loadCoolingStage2.readHisVal() > 0 || systemEquip.satCoolingStage2.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadCoolingStage2.readHisVal() > 0) ",2" else "")
            append(if (systemEquip.loadCoolingStage3.readHisVal() > 0 || systemEquip.satCoolingStage3.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadCoolingStage3.readHisVal() > 0) ",3" else "")
            append(if (systemEquip.loadCoolingStage4.readHisVal() > 0 || systemEquip.satCoolingStage4.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadCoolingStage4.readHisVal() > 0) ",4" else "")
            append(if (systemEquip.loadCoolingStage5.readHisVal() > 0 || systemEquip.satCoolingStage5.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadCoolingStage5.readHisVal() > 0) ",5" else "")
        }
        if (coolingStatus.isNotEmpty()) {
            if (coolingStatus[0] == ',') {
                coolingStatus.deleteCharAt(0)
            }
            coolingStatus.insert(0, "Cooling Stage ")
            coolingStatus.append(" ON ")
        }

        val heatingStatus = StringBuilder().apply {
            append(if (systemEquip.loadHeatingStage1.readHisVal() > 0 || systemEquip.satHeatingStage1.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadHeatingStage1.readHisVal() > 0) "1" else "")
            append(if (systemEquip.loadHeatingStage2.readHisVal() > 0 || systemEquip.satHeatingStage2.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadHeatingStage2.readHisVal() > 0) ",2" else "")
            append(if (systemEquip.loadHeatingStage3.readHisVal() > 0 || systemEquip.satHeatingStage3.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadHeatingStage3.readHisVal() > 0) ",3" else "")
            append(if (systemEquip.loadHeatingStage4.readHisVal() > 0 || systemEquip.satHeatingStage4.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadHeatingStage4.readHisVal() > 0) ",4" else "")
            append(if (systemEquip.loadHeatingStage5.readHisVal() > 0 || systemEquip.satHeatingStage5.readHisVal() > 0 ||
                systemEquip.connectEquip1.loadHeatingStage5.readHisVal() > 0) ",5" else "")
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
        if ((analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_FAN) && systemFanLoopOp > 0) ||
            (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN) && staticPressureFanLoopOp > 0)) {
            analogStatus.append("| Fan ON ")
        }
        if ((systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) &&
            ((analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_COOLING) && systemCoolingLoopOp > 0 ) ||
            (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING) && systemSatCoolingLoopOp > 0))) {
            analogStatus.append("| Cooling ON ")
        }
        if ((systemEquip.mechanicalHeatingAvailable.readHisVal() > 0) &&
            ((analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_HEATING) && systemHeatingLoopOp > 0) ||
            ( analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING) && systemSatHeatingLoopOp > 0))) {
            analogStatus.append("| Heating ON ")
        }
        if (analogStatus.isNotEmpty()) {
            analogStatus.insert(0, " ; Analog ")
        }
        systemStatus.append(coolingStatus)
            .append(heatingStatus)
            .append(analogStatus)

        return if (systemStatus.toString() == "") "System OFF$humidifierStatus$dehumidifierStatus" else systemStatus.toString() + humidifierStatus + dehumidifierStatus
    }

    private fun getHumidifierStatus () : String{
        return if (systemEquip.humidifierEnable.pointExists()) {
            if (systemEquip.humidifierEnable.readHisVal() > 0) {
                " | Humidifier ON "
            } else " | Humidifier OFF "
        } else { "" }
    }
    private fun getDehumidifierStatus () : String{
        return if (systemEquip.dehumidifierEnable.pointExists()) {
            if (systemEquip.dehumidifierEnable.readHisVal() > 0) {
                " | Dehumidifier ON "
            } else " | Dehumidifier OFF "
        } else { "" }
    }

    open fun updateStagesSelected() {
        coolingStages = 0
        heatingStages = 0
        fanStages = 0

        updateStagesForRelayMapping(getCMRelayAssociationMap(systemEquip))
        updateStagesForRelayMapping(getConnectRelayAssociationMap(systemEquip))

        if (heatingStages > 0) {
            heatingStages -= Stage.COOLING_5.ordinal
        }
        if (fanStages > 0) {
            fanStages -= Stage.HEATING_5.ordinal
        }
        analogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls()
        CcuLog.d(L.TAG_CCU_SYSTEM, "Cooling stages : $coolingStages Heating stages : $heatingStages Fan stages : $fanStages")
    }

    private fun updateStagesForRelayMapping(relayMapping : Map<Point, Point>) {
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

    override fun getLogicalPhysicalMap(): Map<Point, PhysicalPoint>? {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        map[systemEquip.relay1OutputEnable] = Domain.cmBoardDevice.relay1
        map[systemEquip.relay2OutputEnable] = Domain.cmBoardDevice.relay2
        map[systemEquip.relay3OutputEnable] = Domain.cmBoardDevice.relay3
        map[systemEquip.relay4OutputEnable] = Domain.cmBoardDevice.relay4
        map[systemEquip.relay5OutputEnable] = Domain.cmBoardDevice.relay5
        map[systemEquip.relay6OutputEnable] = Domain.cmBoardDevice.relay6
        map[systemEquip.relay7OutputEnable] = Domain.cmBoardDevice.relay7
        map[systemEquip.relay8OutputEnable] = Domain.cmBoardDevice.relay8
        return map
    }

    private fun getAnalogOutLogicalPhysicalMap() : Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        map[systemEquip.analog1OutputEnable] = Domain.cmBoardDevice.analog1Out
        map[systemEquip.analog2OutputEnable] = Domain.cmBoardDevice.analog2Out
        map[systemEquip.analog3OutputEnable] = Domain.cmBoardDevice.analog3Out
        map[systemEquip.analog4OutputEnable] = Domain.cmBoardDevice.analog4Out

        return map
    }

    private fun updateOutputPorts() {
        //TODO - refactor the encapsulation of relayAssociationMap and analogOutAssociationMap
        updateRelayOutputPorts(getCMRelayAssociationMap(systemEquip), false)
        updateAnalogOutputPorts(getCMAnalogAssociationMap(systemEquip), getAnalogOutLogicalPhysicalMap(), false)

        CcuLog.d(L.TAG_CCU_SYSTEM, "updateOutputPorts : Connect")
        if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
            updateRelayOutputPorts(
                    getConnectRelayAssociationMap(systemEquip),
                    true
            )
            updateAnalogOutputPorts(
                    getConnectAnalogAssociationMap(systemEquip),
                    getConnectAnalogOutLogicalPhysicalMap(
                        systemEquip.connectEquip1,
                        Domain.connect1Device,
                    ),
                    true
            )
        }
    }

    private fun updateRelayOutputPorts(associationMap: Map<Point, Point>, isConnectEquip: Boolean) {
        CcuLog.d(L.TAG_CCU_SYSTEM, "updateRelayOutputPorts")
        associationMap.forEach { (relay: Point, association: Point) ->
            if (relay.readDefaultVal() > 0) {
                try {
                    val (logicalPoint, relayOutput) = advancedAhuImpl.getAdvancedAhuRelayState(
                        association,
                        systemEquip,
                        coolingStages,
                        heatingStages,
                        fanStages,
                        isSystemOccupied,
                        isConnectEquip
                    )
                    CcuLog.d(
                        L.TAG_CCU_SYSTEM,
                        "New relayOutput " + relay.domainName + " " + association.domainName + " " + logicalPoint.domainName + " " + relayOutput
                    )
                    //TODO - should update only if any stage up/down timer is not active.
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
            CcuLog.d(L.TAG_CCU_SYSTEM, "Connect Stage Status :"+connectStageStatus.joinToString { "${it.first}:${it.second}" })
        } else {
            CcuLog.d(L.TAG_CCU_SYSTEM, "CM Stage Status :"+cmStageStatus.joinToString { "${it.first}:${it.second}" })
        }
        updatePointsDbVal(isConnectEquip)
    }

    private fun updateAnalogOutputPorts(associationMap: Map<Point, Point>, physicalMap: Map<Point, PhysicalPoint>?, isConnectEquip: Boolean) {
        associationMap.forEach { (analogOut: Point, association: Point) ->
            if (analogOut.readDefaultVal() > 0) {
                val domainEquip = if (isConnectEquip) systemEquip.connectEquip1 else systemEquip
                val (physicalValue,logicalValue) = advancedAhuImpl.getAnalogLogicalPhysicalValue(
                        analogOut, association, ahuSettings, domainEquip
                )

                CcuLog.d(L.TAG_CCU_SYSTEM, "New analogOutValue ${analogOut.domainName} physicalValue: $physicalValue logicalValue: $logicalValue")
                physicalMap?.get(analogOut)?.writeHisVal(physicalValue )

                if (isConnectEquip) {
                    val domainName = connectAnalogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    getDomainPointForName(domainName, systemEquip.connectEquip1).writeHisVal(logicalValue)
                } else {
                    val domainName = analogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    getDomainPointForName(domainName, systemEquip).writeHisVal(logicalValue)
                }
            }
        }
    }

    private fun updateDerivedSensorPoints() {
        if (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN)) {
            updatePressureSensorDerivedPoints(systemEquip)
        }
        if (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING) ||
            analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING)) {
            updateTemperatureSensorDerivedPoints(systemEquip)
        }
        if (analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.CO2_DAMPER)) {
            if (systemEquip.zoneAvgCo2.pointExists()) {
                systemEquip.zoneAvgCo2.writeHisVal(VavSystemController.getInstance().systemCO2WA)
            }
        }
    }

    private fun updatePointsDbVal(isConnectEquip : Boolean) {
        val stageStatus = if (isConnectEquip) connectStageStatus else cmStageStatus
        stageStatus.forEachIndexed { index, status ->
            val domainName = if (isConnectEquip) connectRelayAssociationToDomainName(index) else relayAssociationToDomainName(index)
            if (status.first < status.second) {
                //Stage is going up
                val associationType = relayAssociationDomainNameToType(domainName)
                if (associationType.isConditioningStage() && isConditioningActive(associationType)) {
                    if (!isStageUpTimerActive()) {
                        updatePointVal(domainName, index, status.second, isConnectEquip)
                        activateStageUpTimer()
                    } else {
                        CcuLog.d(L.TAG_CCU_SYSTEM, "Stage up ignored for $domainName counter: $stageUpTimer")
                    }
                } else {
                    updatePointVal(domainName, index, status.second, isConnectEquip)
                }
            } else if (status.first > status.second) {
                //Stage is going down
                CcuLog.d(L.TAG_CCU_SYSTEM, "Stage down detected for $domainName")
                val associationType = relayAssociationDomainNameToType(domainName)
                if (associationType.isConditioningStage() && isConditioningActive(associationType)) {
                    if (!isStageDownTimerActive()) {
                        updatePointVal(domainName, index, status.second, isConnectEquip)
                        activateStageDownTimer()
                    } else {
                        CcuLog.d(L.TAG_CCU_SYSTEM, "Stage down ignored for $domainName counter: $stageUpTimer")
                    }
                } else {
                    updatePointVal(domainName, index, status.second, isConnectEquip)
                }
            } else {
                updatePointVal(domainName, index, status.second, isConnectEquip)
            }
        }
        updateLogicalToPhysical(isConnectEquip)
    }

    private fun updateLogicalToPhysical(isConnectEquip : Boolean) {
        if (isConnectEquip) {
            getConnectRelayAssociationMap(systemEquip).forEach { (relay, association) ->
                if (relay.readDefaultVal() > 0) {
                    val domainName = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                    val physicalPoint = getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]
                    physicalPoint?.writeHisVal(getDomainPointForName(domainName, systemEquip.connectEquip1).readHisVal())
                }
            }
        } else {
            // sync logical to physical
            getCMRelayAssociationMap(systemEquip).forEach { (relay, association) ->
                if (relay.readDefaultVal() > 0) {
                    val domainName = relayAssociationToDomainName(association.readDefaultVal().toInt())
                    getCMRelayLogicalPhysicalMap(systemEquip)[relay]?.writeHisVal(getDomainPointForName(domainName, systemEquip).readHisVal())
                }
            }
        }
    }


    private fun isConditioningActive(type : AdvancedAhuRelayAssociationType) : Boolean {
        return when(type) {
            AdvancedAhuRelayAssociationType.LOAD_COOLING, AdvancedAhuRelayAssociationType.SAT_COOLING
            -> VavSystemController.getInstance().getSystemState() == SystemController.State.COOLING

            AdvancedAhuRelayAssociationType.LOAD_HEATING, AdvancedAhuRelayAssociationType.SAT_HEATING
            -> VavSystemController.getInstance().getSystemState() == SystemController.State.HEATING

            else -> false
        }
    }
    private fun activateStageUpTimer() {
        stageUpTimer = systemEquip.vavStageUpTimerCounter.readPriorityVal()
    }
    private fun activateStageDownTimer() {
        stageDownTimer = systemEquip.vavStageDownTimerCounter.readPriorityVal()
    }

    private fun isStageUpTimerActive() : Boolean = stageUpTimer > 0
    private fun isStageDownTimerActive() : Boolean = stageDownTimer > 0

    private fun updatePointVal(domainName: String, stageIndex : Int, pointVal : Int, isConnectEquip : Boolean) {
        if (isConnectEquip) {
            updateConnectPointVal(domainName, stageIndex, pointVal)
        } else {
            updateCMPointVal(domainName, stageIndex, pointVal)
        }
    }
    private fun updateCMPointVal(domainName: String, stageIndex : Int, pointVal : Int) {
        getDomainPointForName(domainName, systemEquip).writeHisVal(pointVal.toDouble())
        getCMRelayAssociationMap(systemEquip).forEach { (relay, association) ->
            if (association.readDefaultVal() == stageIndex.toDouble() && relay.readDefaultVal() > 0) {
                getCMRelayLogicalPhysicalMap(systemEquip)[relay]?.writeHisVal(pointVal.toDouble())
            }
        }
    }

    private fun updateConnectPointVal(domainName: String, stageIndex : Int, pointVal : Int) {
        getDomainPointForName(domainName, systemEquip.connectEquip1).writeHisVal(pointVal.toDouble())
        getConnectRelayAssociationMap(systemEquip).forEach { (relay, association) ->
            if (association.readDefaultVal() == stageIndex.toDouble() && relay.readDefaultVal() > 0) {
                getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]?.writeHisVal(pointVal.toDouble())
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun getUnit(domainName: String) : String {
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
        getCMRelayAssociationMap(systemEquip).entries.forEach { (relay, association) ->
            val domainName = relayAssociationToDomainName(association.readDefaultVal().toInt())
            getDomainPointForName(domainName, systemEquip).writeHisVal(0.0)
            getCMRelayLogicalPhysicalMap(systemEquip)[relay]?.writeHisVal(0.0)
        }

        updateAnalogOutputPorts(getCMAnalogAssociationMap(systemEquip), getAnalogOutLogicalPhysicalMap(), false)

        if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
            getConnectRelayAssociationMap(systemEquip).entries.forEach {(relay, association) ->
                val domainName = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                getDomainPointForName(domainName, systemEquip.connectEquip1).writeHisVal(0.0)
                getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]?.writeHisVal(0.0)
            }

            updateAnalogOutputPorts(
                    getConnectAnalogAssociationMap(systemEquip),
                    getConnectAnalogOutLogicalPhysicalMap(
                            systemEquip.connectEquip1,
                            Domain.connect1Device,
                    ),
                    true
            )
        }
    }

    private fun logOutputs() {
        try {
            CcuLog.i(L.TAG_CCU_SYSTEM, "Test mode is active ${Globals.getInstance().isTestMode}")
            CcuLog.i(L.TAG_CCU_SYSTEM, "Operating mode ${VavSystemController.getInstance().systemState}")
            CcuLog.i(L.TAG_CCU_SYSTEM, "Conditioning  mode $conditioningMode")
            getCMRelayAssociationMap(systemEquip).entries.forEach { (relay, association) ->
                val logical = relayAssociationToDomainName(association.readDefaultVal().toInt())
                CcuLog.i(L.TAG_CCU_SYSTEM,
                    "CM ${relay.domainName}:${relay.readDefaultVal()}  => : " +
                            "Physical Value: ${getCMRelayLogicalPhysicalMap(systemEquip)[relay]!!.readHisVal()}   "+
                            "$logical : ${getDomainPointForName(logical, systemEquip).readHisVal()} ")
            }

            getCMAnalogAssociationMap(systemEquip).entries.forEach { (analogOut, association) ->
                val logical = analogOutAssociationToDomainName(association.readDefaultVal().toInt())
                CcuLog.i(L.TAG_CCU_SYSTEM,
                    "CM ${analogOut.domainName}:${analogOut.readDefaultVal()} =>:  " +
                            "Physical Value: ${getAnalogOutLogicalPhysicalMap()[analogOut]!!.readHisVal()}   "+
                            "$logical : ${getDomainPointForName(logical, systemEquip).readHisVal()}" )
            }

            if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
                getConnectRelayAssociationMap(systemEquip).entries.forEach { (relay, association) ->
                    val logical = relayAssociationToDomainName(association.readDefaultVal().toInt())
                    CcuLog.i(L.TAG_CCU_SYSTEM,
                        "Connect ${relay.domainName}:${relay.readDefaultVal()} => : " +
                                "Physical Value: ${getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]!!.readHisVal()}  "+
                             "$logical : ${getDomainPointForName(logical, systemEquip.connectEquip1).readHisVal()} " )
                }
                getConnectAnalogAssociationMap(systemEquip).entries.forEach { (analogOut, association) ->
                    val logical = connectAnalogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    CcuLog.i(L.TAG_CCU_SYSTEM,
                        "Connect ${analogOut.domainName}: ${analogOut.readDefaultVal()} => : " +
                              "Physical Value: ${getConnectAnalogOutLogicalPhysicalMap(systemEquip.connectEquip1,Domain.connect1Device)[analogOut]!!.readHisVal()}  "+
                            "$logical : ${getDomainPointForName(logical, systemEquip.connectEquip1).readHisVal()} " )
                }
            }
        } catch (e: Exception) {
            CcuLog.e(L.TAG_CCU_SYSTEM, "Error in dumping loops", e)
        }
    }

    fun getOperatingMode(): String {
        return when(SystemController.State.values()[systemEquip.operatingMode.readHisVal().toInt()])  {
            SystemController.State.COOLING -> " Cooling"
            SystemController.State.HEATING -> " Heating"
            else -> { "OFF" }
        }
    }

    fun getUserIntentConfig(): UserIntentConfig {
        val userIntentConfig = UserIntentConfig()

        getCMAnalogAssociationMap(systemEquip).forEach { (analog: Point, association: Point) ->
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
            getConnectAnalogAssociationMap(systemEquip).forEach { (analog, association) ->
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

    @SuppressLint("DefaultLocale")
    private fun roundOff(value: Double): Double {
        return String.format("%.2f", value).toDouble()
    }

    fun isEmergencyShutoffActive() : Boolean {
        return advancedAhuImpl.isEmergencyShutOffEnabledAndActive()
    }
    fun isConnectModuleAvailable(): Boolean {
        return CCUHsApi.getInstance().readEntity(
                "domainName == \"" + DomainName.connectModuleDevice + "\"").isNotEmpty()
    }
}
