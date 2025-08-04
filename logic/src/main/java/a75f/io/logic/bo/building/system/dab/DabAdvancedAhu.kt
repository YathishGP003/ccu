package a75f.io.logic.bo.building.system.dab

import a75.io.algos.ControlLoop
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.systemPurgeDabMinFanLoopOutput
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.api.readPoint
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.util.CalibratedPoint
import a75f.io.logger.CcuLog
import a75f.io.logic.BuildConfig
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.autocommission.AutoCommissioningUtil
import a75f.io.logic.bo.building.EpidemicState
import a75f.io.logic.bo.building.dab.DabProfile
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.AdvAhuEconAlgoHandler
import a75f.io.logic.bo.building.system.AdvancedAhuAlgoHandler
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationType
import a75f.io.logic.bo.building.system.AdvancedAhuAnalogOutAssociationTypeConnect
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.SystemControllerFactory
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.SystemStageHandler
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
import a75f.io.logic.bo.building.system.relayAssociationToDomainName
import a75f.io.logic.bo.building.system.satControlIndexToDomainPoint
import a75f.io.logic.bo.building.system.updatePressureSensorDerivedPoints
import a75f.io.logic.bo.building.system.updateTemperatureSensorDerivedPoints
import a75f.io.logic.bo.building.system.util.AhuSettings
import a75f.io.logic.bo.building.system.util.AhuTuners
import a75f.io.logic.bo.building.system.util.StagesCounts
import a75f.io.logic.bo.building.system.util.UserIntentConfig
import a75f.io.logic.bo.building.system.util.getConnectModuleSystemStatus
import a75f.io.logic.bo.building.system.util.getDehumidifierStatus
import a75f.io.logic.bo.building.system.util.getHumidifierStatus
import a75f.io.logic.bo.building.system.util.getModulatedOutput
import a75f.io.logic.bo.building.system.util.isConnectEconActive
import a75f.io.logic.bo.building.system.util.isConnectModuleAvailable
import a75f.io.logic.bo.building.system.util.isConnectModuleExist
import a75f.io.logic.bo.building.system.util.isOaEconActive
import a75f.io.logic.bo.building.system.util.needToUpdateConditioningMode
import a75f.io.logic.bo.building.system.util.roundOff
import a75f.io.logic.tuners.TunerUtil
import android.annotation.SuppressLint
import android.content.Intent
import java.util.BitSet
import kotlin.math.max

/**
 * Created by Manjunath K on 19-05-2024.
 */

class DabAdvancedAhu : DabSystemProfile() {

    private var heatingStages = 0
    private var coolingStages = 0
    private var fanStages = 0
    private var coolingStagesConnect = 0
    lateinit var systemEquip: DabAdvancedHybridSystemEquip
    private lateinit var advancedAhuImpl: AdvancedAhuAlgoHandler
    private lateinit var advAhuEconImpl: AdvAhuEconAlgoHandler

    private lateinit var conditioningMode: SystemMode
    private lateinit var ahuSettings: AhuSettings
    private lateinit var ahuTuners: AhuTuners
    private lateinit var systemStatusHandler: SystemStageHandler
    private lateinit var connectStatusHandler: SystemStageHandler

    private var systemSatCoolingLoopOp = 0.0
    private var systemSatHeatingLoopOp = 0.0
    private var staticPressureFanLoopOp = 0.0

    private val satCoolingPILoop = ControlLoop()
    private val satHeatingPILoop = ControlLoop()
    private val staticPressureFanPILoop = ControlLoop()

    private val coolIndexRange = 0..4
    private val heatIndexRange = 5..9
    private val compressorRange = 35..39
    private val fanIndexRange = 10..14
    private val satCoolIndexRange = 17..21
    private val satHeatIndexRange = 22..26
    private val pressureFanIndexRange = 27..31

    private lateinit var analogControlsEnabled: Set<AdvancedAhuAnalogOutAssociationType>
    private lateinit var cmAnalogControlsEnabled: Set<AdvancedAhuAnalogOutAssociationType>
    private lateinit var cnAnalogControlsEnabled: Set<AdvancedAhuAnalogOutAssociationType>

    private var ahuStagesCounts = StagesCounts()
    private var connect1StagesCounts = StagesCounts()
    private var currentConditioning: SystemController.State = SystemController.State.OFF
    private var tempChangeDirectiveIsActive = false

    private var connectControllers: HashMap<String, Any> = HashMap()
    private var connectfactory: SystemControllerFactory = SystemControllerFactory(connectControllers)

    val testConfigs = BitSet()
    val cmRelayStatus = BitSet()
    val analogStatus = arrayOf(0.0, 0.0, 0.0, 0.0, 0.0)
    var factory: SystemControllerFactory = SystemControllerFactory(controllers)
    private lateinit var economizationAvailable: CalibratedPoint

    override fun getProfileName(): String {
        return if (BuildConfig.BUILD_TYPE.equals(DabProfile.CARRIER_PROD, ignoreCase = true)) {
            "VVT-C Advanced Hybrid AHU v2"
        } else {
            "DAB Advanced Hybrid AHU v2"
        }
    }

    override fun getProfileType(): ProfileType = ProfileType.SYSTEM_DAB_ADVANCED_AHU

    override fun addSystemEquip() {
        systemEquip = Domain.systemEquip as DabAdvancedHybridSystemEquip
        advancedAhuImpl = AdvancedAhuAlgoHandler(systemEquip)
        advAhuEconImpl = AdvAhuEconAlgoHandler(systemEquip.connectEquip1)
        initializePILoop()
        analogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls(systemEquip.cmEquip, systemEquip.connectEquip1)
        updateStagesSelected()
        systemStatusHandler = SystemStageHandler(systemEquip.cmEquip.conditioningStages)
        connectStatusHandler = SystemStageHandler(systemEquip.connectEquip1.conditioningStages)
        economizationAvailable = CalibratedPoint(DomainName.economizingAvailable , systemEquip.equipRef, 0.0)
    }

    private fun updatePrerequisite() {
        currentOccupancy.data =
            ScheduleManager.getInstance().systemOccupancy.ordinal.toDouble()
        var economization = 0.0
        if (systemCoolingLoopOp > 0) {
            economization =
                if ((L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable) || AdvAhuEconAlgoHandler.isFreeCoolingOn()) 1.0 else 0.0
        }
        economizationAvailable.data = economization
        systemStatusHandler = SystemStageHandler(systemEquip.cmEquip.conditioningStages)
        connectStatusHandler = SystemStageHandler(systemEquip.connectEquip1.conditioningStages)
    }

    fun getAnalogControlsEnabled(): Set<AdvancedAhuAnalogOutAssociationType> = analogControlsEnabled

    fun getSystemSatCoolingLoopOp(): Double = systemSatCoolingLoopOp

    fun updateDomainEquip(equip: DabAdvancedHybridSystemEquip) {
        val systemMode = SystemMode.values()[systemEquip.conditioningMode.readPriorityVal().toInt()]
        advancedAhuImpl = AdvancedAhuAlgoHandler(equip)
        advAhuEconImpl = AdvAhuEconAlgoHandler(equip.connectEquip1)
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
        val listOfEquips = hayStack.readAllEntities("domainName == \"${DomainName.dabAdvancedHybridAhuV2}\"")
        for(equip in listOfEquips){
            if (equip.isNotEmpty()) {
                CcuLog.d(Tags.ADD_REMOVE_PROFILE, "DabAdvancedAhu removing profile with it -->${equip[Tags.ID].toString()}")
                CCUHsApi.getInstance().deleteEntityTree(equip[Tags.ID].toString())
            }
        }
        deleteSystemConnectModule()
    }

    override fun isCoolingAvailable(): Boolean {
        return ahuStagesCounts.compressorStages.data > 0 || connect1StagesCounts.compressorStages.data > 0
                || coolingStages > 0 || coolingStagesConnect > 0
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_COOLING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.COMPRESSOR_SPEED)
    }

    override fun isHeatingAvailable(): Boolean {
        return ahuStagesCounts.compressorStages.data > 0 || connect1StagesCounts.compressorStages.data > 0
                || heatingStages > 0 || coolingStagesConnect > 0
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_HEATING)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.COMPOSITE_SIGNAL)
                || analogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.COMPRESSOR_SPEED)
    }

    override fun isCoolingActive(): Boolean {
        return systemEquip.cmEquip.conditioningStages.loadCoolingStage1.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadCoolingStage2.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadCoolingStage3.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadCoolingStage4.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadCoolingStage5.readHisVal() > 0 ||
                systemEquip.cmEquip.satCoolingStage1Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satCoolingStage2Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satCoolingStage3Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satCoolingStage4Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satCoolingStage5Feedback.readHisVal() > 0
    }

    override fun isHeatingActive(): Boolean {
        return systemEquip.cmEquip.conditioningStages.loadHeatingStage1.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadHeatingStage2.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadHeatingStage3.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadHeatingStage4.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadHeatingStage5.readHisVal() > 0 ||
                systemEquip.cmEquip.satHeatingStage1Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satHeatingStage2Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satHeatingStage3Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satHeatingStage4Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.satHeatingStage5Feedback.readHisVal() > 0
    }

    private fun isCompressorActive(): Boolean {
        return systemEquip.cmEquip.conditioningStages.compressorStage1.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.compressorStage2.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.compressorStage3.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.compressorStage4.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.compressorStage5.readHisVal() > 0
    }

    override fun doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo()
        advAhuEconImpl.doOAO()
        updateSystemPoints()
    }

    private fun initializePILoop() {
        val proportionalGain = systemEquip.dabSupplyAirProportionalKFactor.readPriorityVal()
        val integralGain = systemEquip.dabSupplyAirIntegralKFactor.readPriorityVal()
        val proportionalRange = systemEquip.dabSupplyAirTemperatureProportionalRange.readPriorityVal()
        val integralTime = systemEquip.dabSupplyAirTemperatureIntegralTime.readPriorityVal()

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
            setProportionalGain(systemEquip.dabDuctStaticProportionalKFactor.readPriorityVal())
            setIntegralGain(systemEquip.dabDuctStaticPressureIntegralKFactor.readPriorityVal())
            proportionalSpread = systemEquip.dabDuctStaticPressureProportionalRange.readPriorityVal()
            integralMaxTimeout = systemEquip.dabDuctStaticPressureIntegralTime.readPriorityVal().toInt()
        }
    }

    fun updateSystemPoints() {
        systemEquip = Domain.systemEquip as DabAdvancedHybridSystemEquip
        if (currentOccupancy == null) {
            currentOccupancy = CalibratedPoint(DomainName.occupancyMode, systemEquip.equipRef, 0.0)
        }
        advancedAhuImpl = AdvancedAhuAlgoHandler(systemEquip)
        advAhuEconImpl = AdvAhuEconAlgoHandler(systemEquip.connectEquip1)
        conditioningMode = SystemMode.values()[systemEquip.conditioningMode.readPriorityVal().toInt()]
        ahuSettings = getAhuSettings()
        ahuTuners = getAhuTuners()

        updateStagesSelected()
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())
        updateDerivedSensorPoints()
        addAhuControllers()
        addConnectControllers()

        if (currentConditioning == SystemController.State.OFF) {
            currentConditioning = systemController.getSystemState()
            tempChangeDirectiveIsActive = false
        } else if (currentConditioning != systemController.getSystemState()) {
            currentConditioning = systemController.getSystemState()
            tempChangeDirectiveIsActive = true
        } else {
            tempChangeDirectiveIsActive = false
        }

        if (tempChangeDirectiveIsActive) {
            resetControllers(factory)
        }
        currentConditioning = systemController.getSystemState()

        getConditioningLoops()

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

    private fun getConditioningLoops() {
        systemCoolingLoopOp = getSystemCoolingLoop()
        systemHeatingLoopOp = getSystemHeatingLoop()
        systemFanLoopOp = getFanLoop().coerceIn(0.0, 100.0)
        systemCompressorLoop = 0.0

        systemSatCoolingLoopOp = getSystemSatCoolingLoop().coerceIn(0.0, 100.0)
        systemSatHeatingLoopOp = getSystemSatHeatingLoop().coerceIn(0.0, 100.0)
        staticPressureFanLoopOp = getSystemStaticPressureFanLoop().coerceIn(0.0, 100.0)
        systemCo2LoopOp = if (isSystemOccupiedForDcv) getCo2Loop() else 0.0

        if (currentConditioning == SystemController.State.COOLING) {
            systemCompressorLoop = systemCoolingLoopOp
        }

        if (currentConditioning == SystemController.State.HEATING) {
            systemCompressorLoop = systemHeatingLoopOp
        }
        overrideLoopsIfAutoCommissioningStarted()
    }

    private fun overrideLoopsIfAutoCommissioningStarted() {

        if (AutoCommissioningUtil.isAutoCommissioningStarted()) {

            systemEquip.coolingLoopOutput.writeDefaultVal(systemCoolingLoopOp)
            systemCoolingLoopOp = systemEquip.coolingLoopOutput.readPriorityVal()

            systemEquip.heatingLoopOutput.writeDefaultVal(systemHeatingLoopOp)
            systemHeatingLoopOp = systemEquip.heatingLoopOutput.readPriorityVal()

            systemEquip.fanLoopOutput.writeDefaultVal(systemFanLoopOp)
            systemFanLoopOp = systemEquip.fanLoopOutput.readPriorityVal()

            systemEquip.co2LoopOutput.writeDefaultVal(systemCo2LoopOp)
            systemCo2LoopOp = systemEquip.co2LoopOutput.readPriorityVal()

            systemEquip.cmEquip.satCoolingLoopOutput.writeDefaultVal(systemSatCoolingLoopOp)
            systemSatCoolingLoopOp = systemEquip.cmEquip.satCoolingLoopOutput.readPriorityVal()

            systemEquip.cmEquip.satHeatingLoopOutput.writeDefaultVal(systemSatHeatingLoopOp)
            systemSatHeatingLoopOp = systemEquip.cmEquip.satHeatingLoopOutput.readPriorityVal()

            systemEquip.cmEquip.fanPressureLoopOutput.writeDefaultVal(staticPressureFanLoopOp)
            staticPressureFanLoopOp = systemEquip.cmEquip.fanPressureLoopOutput.readPriorityVal()

            systemEquip.compressorLoopOutput.writeDefaultVal(systemCompressorLoop)
            systemCompressorLoop = systemEquip.compressorLoopOutput.readPriorityVal()

            systemEquip.dcvLoopOutput.writeDefaultVal(systemCo2LoopOp)
            systemCo2LoopOp = systemEquip.dcvLoopOutput.readPriorityVal()

        }
    }

    private fun getAhuTuners(): AhuTuners {
        return AhuTuners(
                relayAActivationHysteresis = systemEquip.dabRelayDeactivationHysteresis.readPriorityVal(),
                relayDeactivationHysteresis = systemEquip.dabRelayDeactivationHysteresis.readPriorityVal(),
                humidityHysteresis = systemEquip.dabHumidityHysteresis.readPriorityVal()
        )
    }

    private fun getAhuSettings(): AhuSettings {
        return AhuSettings(
                systemEquip = systemEquip.cmEquip,
                connectEquip1 = systemEquip.connectEquip1,
                conditioningMode = conditioningMode,
                isMechanicalCoolingAvailable = (systemEquip.mechanicalCoolingAvailable.readHisVal() == 0.0),
                isMechanicalHeatingAvailable = (systemEquip.mechanicalHeatingAvailable.readHisVal() == 0.0),
                isEmergencyShutoffActive = isEmergencyShutoffActive(),
                isEconomizationAvailable = isEconomizationAvailable(),
                systemState = DabSystemController.getInstance().getSystemState(),
        )
    }

    private fun updateLoopOpPoints() {
        systemEquip.coolingLoopOutput.writePointValue(systemCoolingLoopOp)
        systemEquip.heatingLoopOutput.writePointValue(systemHeatingLoopOp)
        systemEquip.fanLoopOutput.writePointValue(systemFanLoopOp)
        systemEquip.co2LoopOutput.writePointValue(systemCo2LoopOp)
        systemEquip.cmEquip.satCoolingLoopOutput.writePointValue(systemSatCoolingLoopOp)
        systemEquip.cmEquip.satHeatingLoopOutput.writePointValue(systemSatHeatingLoopOp)
        systemEquip.cmEquip.fanPressureLoopOutput.writePointValue(staticPressureFanLoopOp)
        systemEquip.operatingMode.writePointValue(DabSystemController.getInstance().systemState.ordinal.toDouble())
        systemEquip.cmEquip.co2BasedDamperControl.writePointValue(systemCo2LoopOp)
        systemEquip.cmEquip.compressorLoopOutput.writePointValue(systemCompressorLoop)
        systemEquip.connectEquip1.let {
            it.coolingLoopOutput.writePointValue(systemCoolingLoopOp)
            it.heatingLoopOutput.writePointValue(systemHeatingLoopOp)
            it.fanLoopOutput.writePointValue(systemFanLoopOp)
            it.co2LoopOutput.writePointValue(systemCo2LoopOp)
            it.compressorLoopOutput.writePointValue(systemCompressorLoop)
        }
        // Update the connect module related economizer points
        advAhuEconImpl.updateLoopPoints()
    }

    private fun resetLoops() {
        systemCoolingLoopOp = 0.0
        systemHeatingLoopOp = 0.0
        systemFanLoopOp = 0.0
        systemCompressorLoop = 0.0
        systemCo2LoopOp = 0.0
        systemSatCoolingLoopOp = 0.0
        systemSatHeatingLoopOp = 0.0
        staticPressureFanLoopOp = 0.0
        systemEquip.connectEquip1.let {
            systemCoolingLoopOp = 0.0
            systemHeatingLoopOp = 0.0
            systemFanLoopOp = 0.0
            systemCo2LoopOp = 0.0
            systemCompressorLoop = 0.0
        }
        updateLoopOpPoints()
        // Reset the connect module related economizer points
        advAhuEconImpl.resetLoopPoints()
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

    private fun getSystemHeatingLoop(): Double {
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
            val smartPurgeDabFanLoopOp = L.ccu().oaoProfile.oaoEquip.systemPurgeDabMinFanLoopOutput.readPriorityVal()
            if (L.ccu().oaoProfile.isEconomizingAvailable) {
                val economizingToMainCoolingLoopMap = L.ccu().oaoProfile.oaoEquip.economizingToMainCoolingLoopMap.readPriorityVal()
                max(max(systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap, systemHeatingLoopOp), smartPurgeDabFanLoopOp)
            } else if (currentConditioning == SystemController.State.COOLING) {
                max(systemCoolingLoopOp * analogFanSpeedMultiplier, smartPurgeDabFanLoopOp)
            } else if (currentConditioning == SystemController.State.HEATING) {
                max(systemHeatingLoopOp * analogFanSpeedMultiplier, smartPurgeDabFanLoopOp)
            } else {
                smartPurgeDabFanLoopOp
            }
        } else if((epidemicState == EpidemicState.PREPURGE
                    || epidemicState == EpidemicState.POSTPURGE) && isConnectModuleAvailable() && AdvAhuEconAlgoHandler.getAnalogsAssociatedToOAO()) {
            // Connect module OAO calculation
            val smartPurgeConnectFanLoopOp = TunerUtil.readTunerValByQuery("domainName == \"$systemPurgeDabMinFanLoopOutput\"", ahuSettings.connectEquip1.equipRef)
            if (DabSystemController.getInstance().getSystemState() == SystemController.State.COOLING
                && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)) {
                var tempFanLoopOp = 0.0
                if(ahuSettings.connectEquip1.economizingToMainCoolingLoopMap.readHisVal() != 0.0) {
                    tempFanLoopOp = (coolingLoopOp * 100)/ ahuSettings.connectEquip1.economizingToMainCoolingLoopMap.readHisVal()
                }
                maxOf(tempFanLoopOp, smartPurgeConnectFanLoopOp)
            }else if (DabSystemController.getInstance().getSystemState() == SystemController.State.HEATING
                && (conditioningMode == SystemMode.HEATONLY || conditioningMode == SystemMode.AUTO)) {
                (DabSystemController.getInstance().getHeatingSignal() * analogFanSpeedMultiplier).toInt().toDouble().coerceAtLeast(smartPurgeConnectFanLoopOp)
            } else {
                smartPurgeConnectFanLoopOp
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
        return if (DabSystemController.getInstance().getSystemState() == SystemController.State.COOLING && (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)) {
            val satSpMax = systemEquip.cmEquip.systemCoolingSatMax.readDefaultVal()
            val satSpMin = systemEquip.cmEquip.systemCoolingSatMin.readDefaultVal()
            val coolingSatSp = roundOff(satSpMax - systemCoolingLoopOp * (satSpMax - satSpMin) / 100)
            systemEquip.cmEquip.airTempCoolingSp.writeHisVal(coolingSatSp)
            systemEquip.cmEquip.airTempHeatingSp.writeHisVal(systemEquip.cmEquip.systemHeatingSatMin.readDefaultVal())
            CcuLog.d(L.TAG_CCU_SYSTEM, "coolingSatSpMax :$satSpMax coolingSatSpMin: $satSpMin satSensorVal $satControlPoint coolingSatSp: $coolingSatSp")
                val satCoolingPILoopLocal = satCoolingPILoop.getLoopOutput(satControlPoint, coolingSatSp)
                // When econ is ON and less than economizingToMainCoolingLoopMap, write SpMax value since we need to prevent cooling
                var economizingToMainCoolingLoopMap = 0.0
                if(L.ccu().oaoProfile != null) economizingToMainCoolingLoopMap = L.ccu().oaoProfile.oaoEquip.economizingToMainCoolingLoopMap.readPriorityVal()
                if(ahuSettings.isEconomizationAvailable) {
                    // When the economization is active in the connect module case, fetch the coolingLoopMap from the connectEquip1
                    economizingToMainCoolingLoopMap = ahuSettings.connectEquip1.economizingToMainCoolingLoopMap.readPriorityVal()
                }
                if(((L.ccu().oaoProfile != null && L.ccu().oaoProfile.isEconomizingAvailable) ||
                        ahuSettings.isEconomizationAvailable) &&
                    (systemCoolingLoopOp > 0 && systemCoolingLoopOp < economizingToMainCoolingLoopMap)) {
                    CcuLog.d(L.TAG_CCU_SYSTEM, "Econ ON overridden at systemCoolingLoopOp: $systemCoolingLoopOp with max value: coolingSatSp: $satSpMax")
                    systemEquip.cmEquip.airTempCoolingSp.writeHisVal(satSpMax + 2)
                }
                satCoolingPILoopLocal
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
        return if (DabSystemController.getInstance().getSystemState() == SystemController.State.HEATING && (conditioningMode == SystemMode.HEATONLY || conditioningMode == SystemMode.AUTO)) {
            val satSpMax = systemEquip.cmEquip.systemHeatingSatMax.readDefaultVal()
            val satSpMin = systemEquip.cmEquip.systemHeatingSatMin.readDefaultVal()
            val heatingSatSp = roundOff(getModulatedOutput(systemHeatingLoopOp, satSpMin, satSpMax))
            systemEquip.cmEquip.airTempHeatingSp.writeHisVal(heatingSatSp)
            systemEquip.cmEquip.airTempCoolingSp.writeHisVal(systemEquip.cmEquip.systemCoolingSatMax.readDefaultVal())
            CcuLog.d(L.TAG_CCU_SYSTEM, "satSpMax :$satSpMax satSpMin: $satSpMin satSensorVal $satControlPoint heatingSatSp: $heatingSatSp")
            satHeatingPILoop.getLoopOutput(heatingSatSp, satControlPoint)
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
        cnAnalogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls(connectEquip1 = systemEquip.connectEquip1)
        val connectModuleStatus = getConnectModuleSystemStatus(
            systemEquip.connectEquip1,
            advancedAhuImpl,
            systemCoolingLoopOp,
            cnAnalogControlsEnabled,
            ahuSettings
        )
        val scheduleStatus = ScheduleManager.getInstance().systemStatusString
        CcuLog.d(L.TAG_CCU_SYSTEM, "StatusMessage: $systemStatus")
        CcuLog.d(L.TAG_CCU_SYSTEM, "connect module StatusMessage: $connectModuleStatus")
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: $scheduleStatus")
        if (systemEquip.equipStatusMessage.readDefaultStrVal() != systemStatus) {
            systemEquip.equipStatusMessage.writeDefaultVal(systemStatus)
            Globals.getInstance().applicationContext.sendBroadcast(Intent(ScheduleUtil.ACTION_STATUS_CHANGE))
        }
        if (systemEquip.equipScheduleStatus.readDefaultStrVal() != scheduleStatus) {
            systemEquip.equipScheduleStatus.writeDefaultVal(scheduleStatus)
        }
        if (isConnectModuleExist()) {
            if (systemEquip.connectEquip1.equipStatusMessage.readDefaultStrVal() != connectModuleStatus) {
                systemEquip.connectEquip1.equipStatusMessage.writeDefaultVal(connectModuleStatus)
            }
        }
    }

    override fun getStatusMessage(): String {
        cmAnalogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls(systemEquip.cmEquip)
        val systemStages = systemEquip.cmEquip.conditioningStages
        val economizerActive = isOaEconActive() || isConnectEconActive()
        if (advancedAhuImpl.isEmergencyShutOffEnabledAndActive(systemEquip.cmEquip, systemEquip.connectEquip1)) return "Emergency Shut Off mode is active"
        val systemStatus = StringBuilder().apply {
            append(if (systemStages.loadFanStage1.readHisVal() > 0 ||systemEquip.cmEquip.fanPressureStage1Feedback.readHisVal() > 0) "1" else "")
            append(if (systemStages.loadFanStage2.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage2Feedback.readHisVal() > 0) ",2" else "")
            append(if (systemStages.loadFanStage3.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage3Feedback.readHisVal() > 0) ",3" else "")
            append(if (systemStages.loadFanStage4.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage4Feedback.readHisVal() > 0) ",4" else "")
            append(if (systemStages.loadFanStage5.readHisVal() > 0 || systemEquip.cmEquip.fanPressureStage5Feedback.readHisVal() > 0) ",5" else "")
        }
        if (systemStatus.isNotEmpty()) {
            if (systemStatus[0] == ',') {
                systemStatus.deleteCharAt(0)
            }
            systemStatus.insert(0, "Fan Stage ")
            systemStatus.append(" ON ")
        }
        val coolingStatus = StringBuilder().apply {
            if (isCoolingActive || DabSystemController.getInstance().systemState == SystemController.State.COOLING && isCompressorActive()) {
                append(if (systemStages.loadCoolingStage1.readHisVal() > 0 || systemStages.compressorStage1.readHisVal() > 0  || systemEquip.cmEquip.satCoolingStage1Feedback.readHisVal() > 0) "1" else "")
                append(if (systemStages.loadCoolingStage2.readHisVal() > 0 || systemStages.compressorStage2.readHisVal() > 0  || systemEquip.cmEquip.satCoolingStage2Feedback.readHisVal() > 0) ",2" else "")
                append(if (systemStages.loadCoolingStage3.readHisVal() > 0 || systemStages.compressorStage3.readHisVal() > 0  || systemEquip.cmEquip.satCoolingStage3Feedback.readHisVal() > 0) ",3" else "")
                append(if (systemStages.loadCoolingStage4.readHisVal() > 0 || systemStages.compressorStage4.readHisVal() > 0  || systemEquip.cmEquip.satCoolingStage4Feedback.readHisVal() > 0) ",4" else "")
                append(if (systemStages.loadCoolingStage5.readHisVal() > 0 || systemStages.compressorStage5.readHisVal() > 0  || systemEquip.cmEquip.satCoolingStage5Feedback.readHisVal() > 0) ",5" else "")
            }
        }
        if (coolingStatus.isNotEmpty()) {
            if (coolingStatus[0] == ',') {
                coolingStatus.deleteCharAt(0)
            }
            coolingStatus.insert(0, "Cooling Stage ")
            coolingStatus.append(" ON ")
        }

        val heatingStatus = StringBuilder().apply {
            if (isHeatingActive || DabSystemController.getInstance().systemState == SystemController.State.HEATING && isCompressorActive()) {
                append(if (systemStages.loadHeatingStage1.readHisVal() > 0 || systemStages.compressorStage1.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage1Feedback.readHisVal() > 0) "1" else "")
                append(if (systemStages.loadHeatingStage2.readHisVal() > 0 || systemStages.compressorStage2.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage2Feedback.readHisVal() > 0) ",2" else "")
                append(if (systemStages.loadHeatingStage3.readHisVal() > 0 || systemStages.compressorStage3.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage3Feedback.readHisVal() > 0) ",3" else "")
                append(if (systemStages.loadHeatingStage4.readHisVal() > 0 || systemStages.compressorStage4.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage4Feedback.readHisVal() > 0) ",4" else "")
                append(if (systemStages.loadHeatingStage5.readHisVal() > 0 || systemStages.compressorStage5.readHisVal() > 0 || systemEquip.cmEquip.satHeatingStage5Feedback.readHisVal() > 0) ",5" else "")
            }
        }
        if (heatingStatus.isNotEmpty()) {
            if (heatingStatus[0] == ',') {
                heatingStatus.deleteCharAt(0)
            }
            heatingStatus.insert(0, "Heating Stage ")
            heatingStatus.append(" ON ")
        }

        if (economizerActive && !(!isSystemOccupied && isLockoutActiveDuringUnoccupied)) {
            systemStatus.insert(0, "Free Cooling Used | ")
        }

        val humidifierStatus = getHumidifierStatus(systemEquip = systemEquip.cmEquip)
        val dehumidifierStatus = getDehumidifierStatus(systemEquip = systemEquip.cmEquip)

        val analogStatus = StringBuilder()
        if (((cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_FAN) && systemFanLoopOp > 0)
                    || (cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.PRESSURE_FAN)
                    && systemEquip.cmEquip.fanLoopOutputFeedback.readHisVal() > 0))
            && !(!isSystemOccupied && isLockoutActiveDuringUnoccupied)
        ) {
            analogStatus.append("| Fan ON ")
        }
        // 1. When the economizer from OAO system profile is active, then cooling status is on only if the cooling loop output is greater than economizingToMainCoolingLoopMap
        // 2. When the economizer from OAO system profile is not active, then cooling status is on only if the cooling loop output is greater than 0
        if (economizerActive) {
            // Only if te systemCoolingLoopOp greater than economizingToMainCoolingLoopMap, update the analog cooling status
            var economizingToMainCoolingLoopMap = 30.0
            if (isOaEconActive()) {
                economizingToMainCoolingLoopMap =
                    L.ccu().oaoProfile.oaoEquip.economizingToMainCoolingLoopMap.readPriorityVal()
            }
            if (isConnectEconActive()) {
                economizingToMainCoolingLoopMap =
                    ahuSettings.connectEquip1.economizingToMainCoolingLoopMap.readPriorityVal()
            }
            if ((systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) && ((cmAnalogControlsEnabled.contains(
                    AdvancedAhuAnalogOutAssociationType.LOAD_COOLING
                ) && systemCoolingLoopOp >= economizingToMainCoolingLoopMap) ||
                        (cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING) && systemSatCoolingLoopOp >= economizingToMainCoolingLoopMap)) &&
                (cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_COOLING) ||
                        systemEquip.cmEquip.coolingLoopOutputFeedback.readHisVal() > 0)
            ) {
                analogStatus.append("| Cooling ON ")
            }
        } else {
            if ((systemEquip.mechanicalCoolingAvailable.readHisVal() > 0) && ((cmAnalogControlsEnabled.contains(
                    AdvancedAhuAnalogOutAssociationType.LOAD_COOLING
                ) && systemCoolingLoopOp > 0) ||
                        (cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_COOLING) && systemSatCoolingLoopOp > 0)) &&
                (cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_COOLING) ||
                        systemEquip.cmEquip.coolingLoopOutputFeedback.readHisVal() > 0)
            ) {
                analogStatus.append("| Cooling ON ")
            }
        }

        if ((systemEquip.mechanicalHeatingAvailable.readHisVal() > 0) && ((cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.LOAD_HEATING) && systemHeatingLoopOp > 0)
                        || (cmAnalogControlsEnabled.contains(AdvancedAhuAnalogOutAssociationType.SAT_HEATING)
                        && systemEquip.cmEquip.heatingLoopOutputFeedback.readHisVal() > 0))) {
            analogStatus.append("| Heating ON ")
        }
        if (analogStatus.isNotEmpty()) {
            analogStatus.insert(0, " | Analog ")
        }
        
        if (coolingStatus.toString().isEmpty()
            && heatingStatus.toString().isEmpty()
            && analogStatus.toString().isEmpty()
            && systemEquip.cmEquip.conditioningStages.fanEnable.readHisVal() > 0) {
            systemStatus.append("Fan ON ")
        } else {
            systemStatus.append(coolingStatus)
                .append(heatingStatus)
                .append(analogStatus)
        }

        return if (systemStatus.toString() == "") "System OFF$humidifierStatus$dehumidifierStatus" else systemStatus.toString() + humidifierStatus + dehumidifierStatus
    }


    fun updateStagesSelected() {
        coolingStages = 0
        heatingStages = 0
        fanStages = 0
        coolingStagesConnect = 0
        ahuStagesCounts.resetCounts()
        connect1StagesCounts.resetCounts()

        updateStagesForRelayMapping(getCMRelayAssociationMap(systemEquip.cmEquip))
        updateStagesForRelayMappingConnect(getConnectRelayAssociationMap(systemEquip.connectEquip1))

        if (heatingStages > 0) {
            heatingStages -= Stage.COOLING_5.ordinal
        }
        if (fanStages > 0) {
            fanStages -= Stage.HEATING_5.ordinal
        }
        analogControlsEnabled = advancedAhuImpl.getEnabledAnalogControls(systemEquip.cmEquip, systemEquip.connectEquip1)
        CcuLog.d(L.TAG_CCU_SYSTEM, "Cooling stages : $coolingStages Heating stages : $heatingStages Fan stages : $fanStages" +
                " Cooling stages connect : $coolingStagesConnect")
        CcuLog.d(L.TAG_CCU_SYSTEM, "Stages Counts: $ahuStagesCounts \n Connect1 Stages Counts: $connect1StagesCounts")
    }

    private fun updateStagesForRelayMapping(relayMapping: Map<Point, Point>) {
        relayMapping.forEach { (relay: Point, association: Point) ->
            if (relay.readDefaultVal() > 0) {
                var associationIndex = association.readDefaultVal().toInt()
                updateStageCounts(associationIndex, ahuStagesCounts, false)
                if (satCoolIndexRange.contains(associationIndex) || satHeatIndexRange.contains(associationIndex) || pressureFanIndexRange.contains(associationIndex)) {
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

    private fun updateStagesForRelayMappingConnect(relayMapping: Map<Point, Point>) {
        relayMapping.forEach { (relay: Point, association: Point) ->
            if (relay.readDefaultVal() > 0) {
                val associationIndex = association.readDefaultVal().toInt()
                updateStageCounts(associationIndex, connect1StagesCounts, true)
                if (coolIndexRange.contains(associationIndex) && associationIndex >= coolingStagesConnect) {
                    coolingStagesConnect = associationIndex + 1
                } else if (heatIndexRange.contains(associationIndex) && associationIndex >= heatingStages) {
                    heatingStages = associationIndex
                } else if (fanIndexRange.contains(associationIndex) && associationIndex >= fanStages) {
                    fanStages = associationIndex
                }
            }
        }
    }

    private fun updateStageCounts(associationIndex: Int, counts: StagesCounts, isConnectEquip: Boolean) {
        if (coolIndexRange.contains(associationIndex) ) {
            if(associationIndex + 1 >= counts.loadCoolingStages.data.toInt() || counts.loadCoolingStages.data.toInt() == 0) {
                counts.loadCoolingStages.data = (associationIndex + 1).toDouble()
            }
        }
        if (heatIndexRange.contains(associationIndex)) {
            if (associationIndex - 4 >= counts.loadHeatingStages.data.toInt() || counts.loadHeatingStages.data.toInt() == 0) {
                counts.loadHeatingStages.data = (associationIndex - 4).toDouble()
            }
        }
        if (fanIndexRange.contains(associationIndex)) {
            if (associationIndex - 9 >= counts.loadFanStages.data.toInt() || counts.loadFanStages.data.toInt() == 0) {
                counts.loadFanStages.data = (associationIndex - 9).toDouble()
            }
        }
        if (!isConnectEquip) {
            if (satCoolIndexRange.contains(associationIndex)) {
                if (associationIndex - 16 >= counts.satCoolingStages.data.toInt() || counts.satCoolingStages.data.toInt() == 0) {
                    counts.satCoolingStages.data = (associationIndex - 16).toDouble()
                }
            }
            if (satHeatIndexRange.contains(associationIndex)) {
                if (associationIndex - 21 >= counts.satHeatingStages.data.toInt() || counts.satHeatingStages.data.toInt() == 0) {
                    counts.satHeatingStages.data = (associationIndex - 21).toDouble()
                }
            }
            if (pressureFanIndexRange.contains(associationIndex)) {
                if (associationIndex - 26 >= counts.loadFanStages.data.toInt() || counts.loadFanStages.data.toInt() == 0) {
                    counts.pressureFanStages.data = (associationIndex - 26).toDouble()
                }
            }
        }

        if (isConnectEquip && (22..26).contains(associationIndex)) {
            if (associationIndex - 21 >= counts.compressorStages.data.toInt() || counts.compressorStages.data.toInt() == 0) {
                counts.compressorStages.data = (associationIndex - 21).toDouble()
            }
        }
        else if (compressorRange.contains(associationIndex)) {
            if (associationIndex - 34 >= counts.compressorStages.data.toInt() || counts.compressorStages.data.toInt() == 0) {
                counts.compressorStages.data = (associationIndex - 34).toDouble()
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
        CcuLog.d(L.TAG_CCU_SYSTEM, "CM  status")
        updatePrerequisite()
        systemStatusHandler.runControllersAndUpdateStatus(controllers,
            systemEquip.conditioningMode.readPriorityVal().toInt(),
            systemEquip.cmEquip.conditioningStages)
        updateLogicalToPhysical(false)
        updateAnalogOutputPorts(getCMAnalogAssociationMap(systemEquip.cmEquip), getAnalogOutLogicalPhysicalMap(), false)

        CcuLog.d(L.TAG_CCU_SYSTEM, "Connect module status")
        if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
            connectStatusHandler.runControllersAndUpdateStatus(connectControllers,
                systemEquip.conditioningMode.readPriorityVal().toInt(),
                systemEquip.connectEquip1.conditioningStages)
            updateLogicalToPhysical(true)
            updateAnalogOutputPorts(getConnectAnalogAssociationMap(systemEquip.connectEquip1), getConnectAnalogOutLogicalPhysicalMap(
                    systemEquip.connectEquip1,
                    Domain.connect1Device,
            ), true)
        }
    }

    private fun updateAnalogOutputPorts(associationMap: Map<Point, Point>, physicalMap: Map<Point, PhysicalPoint>?, isConnectEquip: Boolean) {
        associationMap.forEach { (analogOut: Point, association: Point) ->
            if (analogOut.readDefaultVal() > 0) {
                val domainEquip = if (isConnectEquip) systemEquip.connectEquip1 else systemEquip.cmEquip
                val (physicalValue, logicalValue) = advancedAhuImpl.getAnalogLogicalPhysicalValue(analogOut, association, ahuSettings, domainEquip)

                CcuLog.d(L.TAG_CCU_SYSTEM, "New analogOutValue ${analogOut.domainName} physicalValue: $physicalValue logicalValue: $logicalValue")
                physicalMap?.get(analogOut)?.writePointValue(physicalValue)

                if (isConnectEquip) {
                    val domainName = connectAnalogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    getDomainPointForName(domainName, systemEquip.connectEquip1).writeHisVal(roundOff(logicalValue))
                } else {
                    val domainName = analogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    getDomainPointForName(domainName, systemEquip.cmEquip).writeHisVal(roundOff(logicalValue))
                }
            } else {
                physicalMap?.get(analogOut)?.writePointValue(0.0)
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

    private fun updateLogicalToPhysical(isConnectEquip: Boolean) {
        if (isConnectEquip) {
            val physicalMap = getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)
            getConnectRelayAssociationMap(systemEquip.connectEquip1).forEach { (relay, association) ->
                val physicalPoint = physicalMap[relay]
                if (relay.readDefaultVal() > 0) {
                    val domainName = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                    physicalPoint?.writePointValue(getDomainPointForName(domainName, systemEquip.connectEquip1).readHisVal())
                } else {
                    physicalPoint?.writePointValue(0.0)
                }
            }
        } else {
            // sync logical to physical
            val physicalPointMap = getCMRelayLogicalPhysicalMap(systemEquip.cmEquip)
            getCMRelayAssociationMap(systemEquip.cmEquip).forEach { (relay, association) ->
                if (relay.readDefaultVal() > 0) {
                    val domainName = relayAssociationToDomainName(association.readDefaultVal().toInt())
                    physicalPointMap[relay]?.writePointValue(getDomainPointForName(domainName, systemEquip.cmEquip).readHisVal())
                } else {
                    physicalPointMap[relay]?.writePointValue(0.0)
                }
            }
        }
    }

    @SuppressLint("SuspiciousIndentation")
    fun getUnit(domainName: String): String {
        domainName.readPoint(systemEquip.equipRef).let {
            return it["unit"].toString()
        }
    }

    fun getSatUnit(): String {
        if (systemEquip.cmEquip.airTempHeatingSp.pointExists()) {
            return getUnit(DomainName.airTempHeatingSp)
        }
        if (systemEquip.cmEquip.airTempCoolingSp.pointExists()) {
            return getUnit(DomainName.airTempCoolingSp)
        }
        return ""
    }

    private fun resetSystem() {
        reset() // Resetting PI the loop variables
        resetLoops()
        resetOutput()
    }

    private fun resetOutput() {
        // Resetting the relays , analog outs will be reset based on loops
        val cmRelayPhysicalMap = getCMRelayLogicalPhysicalMap(systemEquip.cmEquip)
        getCMRelayAssociationMap(systemEquip.cmEquip).entries.forEach { (relay, association) ->
            val domainName = relayAssociationToDomainName(association.readDefaultVal().toInt())
            getDomainPointForName(domainName, systemEquip.cmEquip).writeHisVal(0.0)
            cmRelayPhysicalMap[relay]?.writePointValue(0.0)
        }

        updateAnalogOutputPorts(getCMAnalogAssociationMap(systemEquip.cmEquip), getAnalogOutLogicalPhysicalMap(), false)

        if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
            val physicalMap = getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)
            getConnectRelayAssociationMap(systemEquip.connectEquip1).entries.forEach { (relay, association) ->
                val domainName = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                getDomainPointForName(domainName, systemEquip.connectEquip1).writePointValue(0.0)
                physicalMap[relay]?.writePointValue(0.0)
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
            CcuLog.i(L.TAG_CCU_SYSTEM, "Conditioning  mode $conditioningMode CM Relay Status $cmRelayStatus")
            getCMRelayAssociationMap(systemEquip.cmEquip).entries.forEach { (relay, association) ->
                if (relay.readDefaultVal() > 0) {
                    val logical = relayAssociationToDomainName(association.readDefaultVal().toInt())
                    CcuLog.i(L.TAG_CCU_SYSTEM, "CM ${relay.domainName}:${relay.readDefaultVal()}  => : " + "Physical Value: ${getCMRelayLogicalPhysicalMap(systemEquip.cmEquip)[relay]!!.readHisVal()}   " + "$logical : ${getDomainPointForName(logical, systemEquip.cmEquip).readHisVal()} ")
                }
            }

            getCMAnalogAssociationMap(systemEquip.cmEquip).entries.forEach { (analogOut, association) ->
                if (analogOut.readDefaultVal() > 0) {
                    val logical = analogOutAssociationToDomainName(association.readDefaultVal().toInt())
                    CcuLog.i(L.TAG_CCU_SYSTEM, "CM ${analogOut.domainName}:${analogOut.readDefaultVal()} =>:  " + "Physical Value: ${getAnalogOutLogicalPhysicalMap()[analogOut]!!.readHisVal()}   " + "$logical : ${getDomainPointForName(logical, systemEquip.cmEquip).readHisVal()}")
                }
            }

            if (!systemEquip.connectEquip1.equipRef.contentEquals("null")) {
                getConnectRelayAssociationMap(systemEquip.connectEquip1).entries.forEach { (relay, association) ->
                    if (relay.readDefaultVal() > 0) {
                        val logical = connectRelayAssociationToDomainName(association.readDefaultVal().toInt())
                        CcuLog.i(L.TAG_CCU_SYSTEM, "Connect ${relay.domainName}:${relay.readDefaultVal()} => : " + "Physical Value: ${getConnectRelayLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[relay]!!.readHisVal()}  " + "$logical : ${getDomainPointForName(logical, systemEquip.connectEquip1).readHisVal()} ")
                    }
                }
                getConnectAnalogAssociationMap(systemEquip.connectEquip1).entries.forEach { (analogOut, association) ->
                    if (analogOut.readDefaultVal() > 0) {
                        val logical = connectAnalogOutAssociationToDomainName(association.readDefaultVal().toInt())
                        CcuLog.i(L.TAG_CCU_SYSTEM, "Connect ${analogOut.domainName}: ${analogOut.readDefaultVal()} => : " + "Physical Value: ${getConnectAnalogOutLogicalPhysicalMap(systemEquip.connectEquip1, Domain.connect1Device)[analogOut]!!.readHisVal()}  " + "$logical : ${getDomainPointForName(logical, systemEquip.connectEquip1).readHisVal()} ")
                    }
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

    private fun isEconomizationAvailable(): Boolean {
        return AdvAhuEconAlgoHandler.isEconomizingAvailable()
    }

    fun getOccupancy(): Int = if (isSystemOccupied) 1 else 0

    fun setTestConfigs(port: Int) {
        testConfigs.set(port,true) // 0 - 7 Relays and 8 - 11 Analog
        CcuLog.d(L.TAG_CCU_SYSTEM, "Test Configs set for port $port cache $testConfigs")
    }

    fun setCMRelayStatus(relay: Int, status: Int) = cmRelayStatus.set(relay, (status == 1))
    fun setAnalogStatus(analog: Int, status: Double)  { analogStatus[analog] = ((status / 10)) }


    fun isStageEnabled(stage: String, cmRelayMappings: Map<Point, Point>): Boolean {
        cmRelayMappings.forEach { (relay, association) ->
            if (relay.readDefaultVal() > 0) {
                val domainName = relayAssociationToDomainName(association.readDefaultVal().toInt())
                if(domainName == stage) {
                    return true
                }
            }
        }
        return false
    }
    fun isHighFanStagesEnabled(): Boolean {
        return systemEquip.cmEquip.conditioningStages.loadFanStage5.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadFanStage4.readHisVal() > 0 ||
                systemEquip.cmEquip.conditioningStages.loadFanStage3.readHisVal() > 0 ||

                systemEquip.cmEquip.fanPressureStage5Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.fanPressureStage4Feedback.readHisVal() > 0 ||
                systemEquip.cmEquip.fanPressureStage3Feedback.readHisVal() > 0 ||

                systemEquip.connectEquip1.conditioningStages.loadFanStage5.readHisVal() > 0 ||
                systemEquip.connectEquip1.conditioningStages.loadFanStage4.readHisVal() > 0 ||
                systemEquip.connectEquip1.conditioningStages.loadFanStage3.readHisVal() > 0
    }

    fun isMediumFanStageEnabled(): Boolean {
        return systemEquip.cmEquip.conditioningStages.loadFanStage2.readHisVal() > 0 ||
                systemEquip.cmEquip.fanPressureStage2Feedback.readHisVal() > 0 ||
                systemEquip.connectEquip1.conditioningStages.loadFanStage2.readHisVal() > 0
    }

    fun isLowFanStageEnabled(): Boolean {
        return systemEquip.cmEquip.conditioningStages.loadFanStage1.readHisVal() > 0 ||
                systemEquip.cmEquip.fanPressureStage1Feedback.readHisVal() > 0 ||
                systemEquip.connectEquip1.conditioningStages.loadFanStage1.readHisVal() > 0
    }


    private fun addAhuControllers() {

        factory.addLoadCoolingControllers(
            systemEquip.cmEquip.coolingLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            economizationAvailable,
            mechanicalCoolingActive,
            ahuStagesCounts.loadCoolingStages
        )

        factory.addLoadHeatingControllers(
            systemEquip.cmEquip.heatingLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            mechanicalHeatingActive,
            ahuStagesCounts.loadHeatingStages
        )

        factory.addLoadFanControllers(
            systemEquip.cmEquip.fanLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            ahuStagesCounts.loadFanStages
        )

        factory.addSatCoolingControllers(
            systemEquip.cmEquip.satCoolingLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            economizationAvailable,
            mechanicalCoolingActive,
            ahuStagesCounts.satCoolingStages
        )

        factory.addSatHeatingControllers(
            systemEquip.heatingLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            mechanicalHeatingActive,
            ahuStagesCounts.satHeatingStages
        )

        factory.addPressureFanControllers(
            systemEquip.fanLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            ahuStagesCounts.pressureFanStages
        )
        factory.addHumidifierController(
            systemEquip.averageHumidity,
            systemEquip.systemtargetMinInsideHumidity,
            systemEquip.dabHumidityHysteresis,
            currentOccupancy,
            systemEquip.cmEquip.conditioningStages.humidifierEnable.pointExists()
        )

        factory.addDeHumidifierController(
            systemEquip.averageHumidity,
            systemEquip.systemtargetMaxInsideHumidity,
            systemEquip.dabHumidityHysteresis,
            currentOccupancy,
            systemEquip.cmEquip.conditioningStages.dehumidifierEnable.pointExists()
        )

        factory.addOccupiedEnabledController(
            currentOccupancy,
            systemEquip.cmEquip.conditioningStages.occupiedEnabled.pointExists()
        )

        factory.addFanEnableController(
            systemEquip.fanLoopOutput,
            currentOccupancy,
            systemEquip.cmEquip.conditioningStages.fanEnable.pointExists()
        )

        factory.addFanRunCommandController(
            systemEquip.co2LoopOutput,
            currentOccupancy,
            systemEquip.cmEquip.conditioningStages.ahuFreshAirFanRunCommand.pointExists()
        )

        factory.addDcvDamperController(
            systemEquip.dcvLoopOutput,
            systemEquip.relayActivationHysteresis,
            currentOccupancy,
            systemEquip.cmEquip.conditioningStages.dcvDamper.pointExists()
        )

        factory.addCompressorControllers(
            systemEquip.compressorLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            economizationAvailable,
            ahuStagesCounts.compressorStages,
            lockoutCompressorActive
        )

        factory.addChangeCoolingChangeOverRelay(
            systemEquip.coolingLoopOutput,
            systemEquip.cmEquip.conditioningStages.changeOverCooling.pointExists()
        )

        factory.addChangeHeatingChangeOverRelay(
            systemEquip.heatingLoopOutput,
            systemEquip.cmEquip.conditioningStages.changeOverHeating.pointExists()
        )

    }

    private fun addConnectControllers() {
        connectfactory.addLoadCoolingControllers(
            systemEquip.connectEquip1.coolingLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            economizationAvailable,
            mechanicalCoolingActive,
            connect1StagesCounts.loadCoolingStages
        )

        connectfactory.addLoadHeatingControllers(
            systemEquip.connectEquip1.heatingLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            mechanicalHeatingActive,
            connect1StagesCounts.loadHeatingStages
        )

        connectfactory.addLoadFanControllers(
            systemEquip.connectEquip1.fanLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            connect1StagesCounts.loadFanStages
        )

        connectfactory.addHumidifierController(
            systemEquip.averageHumidity,
            systemEquip.systemtargetMinInsideHumidity,
            systemEquip.dabHumidityHysteresis,
            currentOccupancy,
            systemEquip.connectEquip1.conditioningStages.humidifierEnable.pointExists()
        )
        connectfactory.addDeHumidifierController(
            systemEquip.averageHumidity,
            systemEquip.systemtargetMaxInsideHumidity,
            systemEquip.dabHumidityHysteresis,
            currentOccupancy,
            systemEquip.connectEquip1.conditioningStages.dehumidifierEnable.pointExists()
        )

        connectfactory.addOccupiedEnabledController(
            currentOccupancy,
            systemEquip.connectEquip1.conditioningStages.occupiedEnabled.pointExists()
        )

        connectfactory.addFanEnableController(
            systemEquip.connectEquip1.fanLoopOutput,
            currentOccupancy,
            systemEquip.connectEquip1.conditioningStages.fanEnable.pointExists()
        )

        connectfactory.addFanRunCommandController(
            systemEquip.connectEquip1.co2LoopOutput,
            currentOccupancy,
            systemEquip.connectEquip1.conditioningStages.ahuFreshAirFanRunCommand.pointExists()
        )

        connectfactory.addExhaustFanStage1Controller(
            systemEquip.connectEquip1.outsideAirFinalLoopOutput,
            systemEquip.connectEquip1.exhaustFanStage1,
            systemEquip.connectEquip1.exhaustFanHysteresis,
            systemEquip.connectEquip1.conditioningStages.exhaustFanStage1.pointExists()
        )

        connectfactory.addExhaustFanStage2Controller(
            systemEquip.connectEquip1.outsideAirFinalLoopOutput,
            systemEquip.connectEquip1.exhaustFanStage2,
            systemEquip.connectEquip1.exhaustFanHysteresis,
            systemEquip.connectEquip1.conditioningStages.exhaustFanStage2.pointExists()
        )

        connectfactory.addCompressorControllers(
            systemEquip.connectEquip1.compressorLoopOutput,
            systemEquip.relayActivationHysteresis,
            systemEquip.dabStageUpTimerCounter,
            systemEquip.dabStageDownTimerCounter,
            economizationAvailable,
            connect1StagesCounts.compressorStages,
            lockoutCompressorActive
        )

        connectfactory.addChangeCoolingChangeOverRelay(
            systemEquip.connectEquip1.coolingLoopOutput,
            systemEquip.connectEquip1.conditioningStages.changeOverCooling.pointExists()
        )

        connectfactory.addChangeHeatingChangeOverRelay(
            systemEquip.connectEquip1.heatingLoopOutput,
            systemEquip.connectEquip1.conditioningStages.changeOverHeating.pointExists()
        )

    }
    
}