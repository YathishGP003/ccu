package a75f.io.logic.bo.building.system.vav

import a75.io.algos.vav.VavTRSystem
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.coolingLoopOutput
import a75f.io.domain.api.DomainName.equipScheduleStatus
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.heatingLoopOutput
import a75f.io.domain.api.DomainName.operatingMode
import a75f.io.domain.api.DomainName.useOutsideTempLockoutCooling
import a75f.io.domain.api.DomainName.useOutsideTempLockoutHeating
import a75f.io.domain.api.DomainName.vavAnalogFanSpeedMultiplier
import a75f.io.domain.api.DomainName.vavHumidityHysteresis
import a75f.io.domain.api.DomainName.vavOutsideTempCoolingLockout
import a75f.io.domain.api.DomainName.vavOutsideTempHeatingLockout
import a75f.io.domain.api.Equip
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.system.BasicConfig
import a75f.io.logic.bo.building.system.SystemController
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.TempDirection
import a75f.io.logic.bo.building.system.calculateDSPSetPoints
import a75f.io.logic.bo.building.system.calculateSATSetPoints
import a75f.io.logic.bo.building.system.getConditioningMode
import a75f.io.logic.bo.building.system.getExternalEquipId
import a75f.io.logic.bo.building.system.getPreviousOperatingModeFromDb
import a75f.io.logic.bo.building.system.getTunerByDomainName
import a75f.io.logic.bo.building.system.handleDeHumidityOperation
import a75f.io.logic.bo.building.system.handleHumidityOperation
import a75f.io.logic.bo.building.system.logIt
import a75f.io.logic.bo.building.system.operateDamper
import a75f.io.logic.bo.building.system.setOccupancyMode
import a75f.io.logic.bo.building.system.updateAutoCommissionOutput
import a75f.io.logic.bo.building.system.updateOperatingMode
import a75f.io.logic.bo.building.system.updatePointHistoryAndDefaultValue
import a75f.io.logic.bo.building.system.updatePointValue
import a75f.io.logic.bo.building.system.updateSystemStatusPoints
import a75f.io.logic.bo.building.system.writePointForCcuUser
import a75f.io.logic.interfaces.ModbusWritableDataInterface

/**
 * Created by Manjunath K on 26-12-2023.
 */

class VavExternalAhu : VavSystemProfile() {

    companion object {
        const val PROFILE_NAME = "VAV External AHU Controller"
        const val SYSTEM_ON = "System ON"
        const val SYSTEM_OFF = "System OFF"
        const val SYSTEM_MODBUS = "equip and system and not modbus"
        private val instance = VavExternalAhu()
        fun getInstance(): VavExternalAhu = instance
    }

    private var modbusInterface: ModbusWritableDataInterface? = null

    /**  externalSpList this contains all the modbus set points id's which is used to write every to cycle to modbus **/
    private var externalSpList = ArrayList<String>()

    private val vavSystem: VavSystemController = VavSystemController.getInstance()
    private var loopRunningDirection = TempDirection.COOLING
    private var hayStack = CCUHsApi.getInstance()

    private var previousOperationMode = SystemMode.OFF.ordinal
    override fun getProfileName(): String = PROFILE_NAME

    override fun getProfileType(): ProfileType = ProfileType.vavExternalAHUController

    fun setModbusWritableDataInterface(callBack: ModbusWritableDataInterface) {
        modbusInterface = callBack
    }

    override fun isCoolingAvailable(): Boolean = true

    override fun isHeatingAvailable(): Boolean = true

    override fun isCoolingActive(): Boolean = systemCoolingLoopOp > 0.0

    override fun isHeatingActive(): Boolean = systemHeatingLoopOp > 0.0

    override fun getCoolingLockoutVal(): Double {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.VAV_EXTERNAL_AHU_CONTROLLER)
        return getTunerByDomainName(systemEquip!!, vavOutsideTempCoolingLockout)
    }

    override fun getHeatingLockoutVal(): Double {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.VAV_EXTERNAL_AHU_CONTROLLER)
        return getTunerByDomainName(systemEquip!!, vavOutsideTempHeatingLockout)
    }

    override fun isOutsideTempCoolingLockoutEnabled(hayStack: CCUHsApi): Boolean =
        Domain.readDefaultValByDomain(useOutsideTempLockoutCooling) > 0

    override fun isOutsideTempHeatingLockoutEnabled(hayStack: CCUHsApi): Boolean =
        Domain.readDefaultValByDomain(useOutsideTempLockoutHeating) > 0

    override fun setOutsideTempCoolingLockoutEnabled(hayStack: CCUHsApi, enabled: Boolean) {
        updatePointHistoryAndDefaultValue(useOutsideTempLockoutCooling, if (enabled) 1.0 else 0.0)
    }

    override fun setOutsideTempHeatingLockoutEnabled(hayStack: CCUHsApi, enabled: Boolean) {
        updatePointHistoryAndDefaultValue(useOutsideTempLockoutHeating, if (enabled) 1.0 else 0.0)
    }

    override fun setCoolingLockoutVal(hayStack: CCUHsApi, value: Double) {
        writePointForCcuUser(hayStack, vavOutsideTempCoolingLockout, value)
    }

    override fun setHeatingLockoutVal(hayStack: CCUHsApi, value: Double) {
        writePointForCcuUser(hayStack, vavOutsideTempHeatingLockout, value)
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
        if (trSystem != null) {
            trSystem.resetRequests()
        }
    }
    override fun getSystemSAT(): Int = (trSystem as VavTRSystem).currentSAT

    fun initTRSystem() {
        trSystem = VavTRSystem()
    }

    override fun addSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val equip = hayStack.readEntity(SYSTEM_MODBUS)
        if (equip != null && equip.size > 0) {
            if (!equip["profile"]?.toString()
                    .contentEquals(ProfileType.vavExternalAHUController.name)
            ) {
                hayStack.deleteEntityTree(equip[Tags.ID].toString())
            }
        }
        initTRSystem()
    }

    @Synchronized
    override fun deleteSystemEquip() {
        val equip = CCUHsApi.getInstance().readEntity(SYSTEM_MODBUS)
        if (equip["profile"]?.toString().contentEquals(ProfileType.vavExternalAHUController.name)) {
            CCUHsApi.getInstance().deleteEntityTree(equip[Tags.ID].toString())
        }
        removeSystemEquipModbus()
        deleteSystemConnectModule()
    }

    @Synchronized
    private fun updateSystemPoints() {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.VAV_EXTERNAL_AHU_CONTROLLER)
        if (systemEquip == null) {
            logIt("VAV_EXTERNAL_AHU_CONTROLLER system equip is empty")
            return
        }
        val scheduleStatus = ScheduleManager.getInstance().systemStatusString
        calculateSetPoints(systemEquip)
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())
        updateSystemStatusPoints(systemEquip.id, scheduleStatus, equipScheduleStatus)
        CcuLog.d(
            L.TAG_CCU_SYSTEM, "systemStatusMessage: $statusMessage ScheduleStatus $scheduleStatus"
        )
    }

    override fun getStatusMessage(): String =
        if (getBasicVavConfigData().loopOutput > 0) SYSTEM_ON else SYSTEM_OFF

    private fun updateCoolingLoop(equip: Equip) {
        if (vavSystem.systemState == SystemController.State.COOLING) {
            val satSpMax = getTunerByDomainName(equip, DomainName.satSPMax)
            val satSpMin = getTunerByDomainName(equip, DomainName.satSPMin)
            systemCoolingLoopOp =
                ((satSpMax - systemSAT) * 100 / (satSpMax - satSpMin)).toInt().toDouble()
            CcuLog.d(
                L.TAG_CCU_SYSTEM,
                "satSpMax :$satSpMax satSpMin: $satSpMin SAT: $systemSAT systemCoolingLoopOp : $systemCoolingLoopOp"
            )
        } else {
            systemCoolingLoopOp = 0.0
        }
    }
    private fun calculateSetPoints(systemEquip: Equip) {
        val externalEquipId = getExternalEquipId()
        updateCoolingLoop(systemEquip)
        val vavConfig = getBasicVavConfigData()
        updateLoopDirection(vavConfig, systemEquip)
        updateAutoCommissionOutput(vavConfig)
        val occupancyMode = ScheduleManager.getInstance().systemOccupancy
        val conditioningMode = getConditioningMode(systemEquip)
        val currentHumidity = VavSystemController.getInstance().getAverageSystemHumidity()
        val humidityHysteresis = getTunerByDomainName(systemEquip, vavHumidityHysteresis)
        val analogFanMultiplier = getTunerByDomainName(systemEquip, vavAnalogFanSpeedMultiplier)
        logIt(
            " System is $occupancyMode conditioningMode : $conditioningMode"
                    + " coolingLoop ${vavConfig.coolingLoop} heatingLoop ${vavConfig.heatingLoop}"
                    + " weightedAverageCO2 ${vavConfig.weightedAverageCO2} loopOutput ${vavConfig.loopOutput}"
        )

        updateOperatingMode(
            systemEquip,
            getUpdatedOperatingMode().toDouble(),
            operatingMode,
            externalSpList,
            externalEquipId,
            hayStack
        )
        calculateSATSetPoints(
            systemEquip,
            vavConfig,
            externalEquipId,
            hayStack,
            externalSpList,
            loopRunningDirection,
        )
        calculateDSPSetPoints(
            systemEquip,
            vavConfig.loopOutput,
            externalEquipId,
            hayStack,
            externalSpList,
            analogFanMultiplier,
            vavConfig.loopOutput,
            conditioningMode,
            vavSystem
        )
        setOccupancyMode(systemEquip,
            externalEquipId,
            occupancyMode,
            hayStack,
            externalSpList,
            vavConfig,
            conditioningMode
        )
        operateDamper(
            systemEquip,
            vavConfig.weightedAverageCO2,
            occupancyMode,
            externalEquipId,
            hayStack,
            externalSpList,
            conditioningMode,
            vavConfig
        )
        handleHumidityOperation(
            systemEquip,
            externalEquipId,
            occupancyMode,
            hayStack,
            externalSpList,
            humidityHysteresis,
            currentHumidity,
            conditioningMode,
            vavConfig
        )
        handleDeHumidityOperation(
            systemEquip,
            externalEquipId,
            occupancyMode,
            hayStack,
            externalSpList,
            humidityHysteresis,
            currentHumidity,
            conditioningMode,
            vavConfig
        )
        updateSystemStatusPoints(systemEquip.id, statusMessage, equipStatusMessage)
        instance.modbusInterface?.writeSystemModbusRegister(externalEquipId, externalSpList)
    }

    private fun updateLoopDirection(basicConfig: BasicConfig, systemEquip: Equip) {
        logIt("Current loop direction $loopRunningDirection  Loop cool: ${basicConfig.coolingLoop} " +
                "heat ${basicConfig.heatingLoop} Mode $previousOperationMode")
        loopRunningDirection = when(vavSystem.systemState) {
            SystemController.State.COOLING -> {
                previousOperationMode = SystemController.State.COOLING.ordinal
                TempDirection.COOLING
            }
            SystemController.State.HEATING -> {
                previousOperationMode = SystemController.State.HEATING.ordinal
                TempDirection.HEATING
            }
            else -> {
                previousOperationMode = if (previousOperationMode == 0) getPreviousOperatingModeFromDb(systemEquip, hayStack) else previousOperationMode
                if (previousOperationMode == SystemController.State.HEATING.ordinal ) TempDirection.HEATING else TempDirection.COOLING
            }
        }

        updatePointValue(systemEquip, coolingLoopOutput, basicConfig.coolingLoop.toDouble())
        updatePointValue(systemEquip, heatingLoopOutput, basicConfig.heatingLoop.toDouble())
        logIt("Changed direction $loopRunningDirection")
    }

    private fun getUpdatedOperatingMode() : Int{
        return when(vavSystem.systemState) {
            SystemController.State.COOLING -> vavSystem.systemState.ordinal
            SystemController.State.HEATING -> vavSystem.systemState.ordinal
            else -> previousOperationMode
        }
    }
    private fun getBasicVavConfigData() = BasicConfig(
        coolingLoop = systemCoolingLoopOp.toInt().coerceIn(0,100),
        heatingLoop = if (vavSystem.heatingSignal <= 0) 0 else vavSystem.heatingSignal,
        loopOutput = if (systemCoolingLoopOp.toInt().coerceIn(0,100) > 0) systemCoolingLoopOp else vavSystem.heatingSignal.toDouble(),
        weightedAverageCO2 = weightedAverageCO2,
    )
}