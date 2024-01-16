package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName.coolingLoopOutput
import a75f.io.domain.api.DomainName.dabAnalogFanSpeedMultiplier
import a75f.io.domain.api.DomainName.dabHumidityHysteresis
import a75f.io.domain.api.DomainName.dabOutsideTempCoolingLockout
import a75f.io.domain.api.DomainName.dabOutsideTempHeatingLockout
import a75f.io.domain.api.DomainName.equipScheduleStatus
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.heatingLoopOutput
import a75f.io.domain.api.DomainName.operatingMode
import a75f.io.domain.api.DomainName.useOutsideTempLockoutCooling
import a75f.io.domain.api.DomainName.useOutsideTempLockoutHeating
import a75f.io.domain.api.Equip
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.system.BasicConfig
import a75f.io.logic.bo.building.system.TempDirection
import a75f.io.logic.bo.building.system.calculateDSPSetPoints
import a75f.io.logic.bo.building.system.calculateSATSetPoints
import a75f.io.logic.bo.building.system.getConditioningMode
import a75f.io.logic.bo.building.system.getExternalEquipId
import a75f.io.logic.bo.building.system.getTunerByDomainName
import a75f.io.logic.bo.building.system.handleDeHumidityOperation
import a75f.io.logic.bo.building.system.handleHumidityOperation
import a75f.io.logic.bo.building.system.logIt
import a75f.io.logic.bo.building.system.operateDamper
import a75f.io.logic.bo.building.system.setOccupancyMode
import a75f.io.logic.bo.building.system.updateOperatingMode
import a75f.io.logic.bo.building.system.updatePointHistoryAndDefaultValue
import a75f.io.logic.bo.building.system.updatePointValue
import a75f.io.logic.bo.building.system.updateSystemStatusPoints
import a75f.io.logic.bo.building.system.writePointForCcuUser
import a75f.io.logic.interfaces.ModbusWritableDataInterface

/**
 * Created by Manjunath K on 12-10-2023.
 */

class DabExternalAhu : DabSystemProfile() {

    companion object {
        const val PROFILE_NAME = "DAB External AHU Controller"
        const val SYSTEM_ON = "System ON"
        const val SYSTEM_OFF = "System OFF"
        const val SYSTEM_MODBUS = "equip and system and not modbus"
        private val instance = DabExternalAhu()
        fun getInstance(): DabExternalAhu = instance
    }

    private var modbusInterface: ModbusWritableDataInterface? = null

    /**  externalSpList this contains all the modbus set points id's which is used to write every to cycle to modbus **/
    private var externalSpList = ArrayList<String>()

    private val dabSystem: DabSystemController = DabSystemController.getInstance()
    private var loopRunningDirection = TempDirection.COOLING
    private var hayStack = CCUHsApi.getInstance()

    override fun getProfileName(): String = PROFILE_NAME

    override fun getProfileType(): ProfileType = ProfileType.dabExternalAHUController

    fun setModbusWritableDataInterface(callBack: ModbusWritableDataInterface) {
        modbusInterface = callBack
    }

    override fun isCoolingAvailable(): Boolean = true

    override fun isHeatingAvailable(): Boolean = true

    override fun isCoolingActive(): Boolean = true

    override fun isHeatingActive(): Boolean = true

    override fun getCoolingLockoutVal(): Double {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        return getTunerByDomainName(systemEquip!!, dabOutsideTempCoolingLockout)
    }

    override fun getHeatingLockoutVal(): Double {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        return getTunerByDomainName(systemEquip!!, dabOutsideTempHeatingLockout)
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
        writePointForCcuUser(hayStack, dabOutsideTempCoolingLockout, value)
    }

    override fun setHeatingLockoutVal(hayStack: CCUHsApi, value: Double) {
        writePointForCcuUser(hayStack, dabOutsideTempHeatingLockout, value)
    }

    override fun doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo()
        updateSystemPoints()
    }

    override fun addSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val equip = hayStack.readEntity(SYSTEM_MODBUS)
        if (equip != null && equip.size > 0) {
            if (!equip["profile"]?.toString()
                    .contentEquals(ProfileType.dabExternalAHUController.name)
            ) {
                hayStack.deleteEntityTree(equip[Tags.ID].toString())
            }
        }
    }

    @Synchronized
    override fun deleteSystemEquip() {
        val equip = CCUHsApi.getInstance().readEntity(SYSTEM_MODBUS)
        if (equip["profile"]?.toString().contentEquals(ProfileType.dabExternalAHUController.name)) {
            CCUHsApi.getInstance().deleteEntityTree(equip[Tags.ID].toString())
        }
    }

    @Synchronized
    private fun updateSystemPoints() {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        if (systemEquip == null) {
            logIt("DAB_EXTERNAL_AHU_CONTROLLER system equip is empty")
            return
        }
        val scheduleStatus = ScheduleManager.getInstance().systemStatusString
        calculateSetPoints(systemEquip)
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())
        updateSystemStatusPoints(systemEquip.id, scheduleStatus, equipScheduleStatus)
        CcuLog.d(
            L.TAG_CCU_SYSTEM,
            "systemStatusMessage: $statusMessage ScheduleStatus $scheduleStatus"
        )
    }

    override fun getStatusMessage(): String =
        if (getBasicDabConfigData().loopOutput > 0) SYSTEM_ON else SYSTEM_OFF

    private fun calculateSetPoints(systemEquip: Equip) {
        val externalEquipId = getExternalEquipId()
        val dabConfig = getBasicDabConfigData()
        val occupancyMode = ScheduleManager.getInstance().systemOccupancy
        val conditioningMode = getConditioningMode(systemEquip)
        val currentHumidity = DabSystemController.getInstance().getAverageSystemHumidity()
        val humidityHysteresis = getTunerByDomainName(systemEquip, dabHumidityHysteresis)
        val analogFanMultiplier = getTunerByDomainName(systemEquip, dabAnalogFanSpeedMultiplier)
        logIt(
            " System is $occupancyMode conditioningMode : $conditioningMode" + " coolingLoop ${dabConfig.coolingLoop} heatingLoop ${dabConfig.heatingLoop}" + " weightedAverageCO2 ${dabConfig.weightedAverageCO2} loopOutput ${dabConfig.loopOutput}"
        )
        updateLoopDirection(dabConfig, systemEquip)
        updateOperatingMode(
            systemEquip,
            dabSystem.systemState.ordinal.toDouble(),
            operatingMode,
            externalSpList,
            externalEquipId,
            hayStack
        )
        calculateSATSetPoints(
            systemEquip,
            dabConfig,
            externalEquipId,
            conditioningMode,
            hayStack,
            externalSpList,
            loopRunningDirection,
        )
        calculateDSPSetPoints(
            systemEquip,
            dabConfig.loopOutput,
            externalEquipId,
            hayStack,
            externalSpList,
            dabConfig,
            analogFanMultiplier,
            dabConfig.coolingLoop.toDouble(),
            conditioningMode
        )
        setOccupancyMode(systemEquip, externalEquipId, occupancyMode, hayStack, externalSpList)
        operateDamper(
            systemEquip,
            dabConfig.weightedAverageCO2,
            occupancyMode,
            externalEquipId,
            hayStack,
            externalSpList,
            conditioningMode
        )
        handleHumidityOperation(
            systemEquip,
            externalEquipId,
            occupancyMode,
            hayStack,
            externalSpList,
            humidityHysteresis,
            currentHumidity,
            conditioningMode
        )
        handleDeHumidityOperation(
            systemEquip,
            externalEquipId,
            occupancyMode,
            hayStack,
            externalSpList,
            humidityHysteresis,
            currentHumidity,
            conditioningMode
        )
        updateSystemStatusPoints(systemEquip.id, statusMessage, equipStatusMessage)
        instance.modbusInterface?.writeSystemModbusRegister(externalEquipId, externalSpList)
    }

    private fun updateLoopDirection(basicConfig: BasicConfig, systemEquip: Equip) {
        logIt("Current loop direction $loopRunningDirection  Loop cool: ${basicConfig.coolingLoop} heat ${basicConfig.heatingLoop}")
        if (basicConfig.coolingLoop > 0)
            loopRunningDirection = TempDirection.COOLING
        if (basicConfig.heatingLoop > 0)
            loopRunningDirection = TempDirection.HEATING
        updatePointValue(systemEquip, coolingLoopOutput, basicConfig.coolingLoop.toDouble())
        updatePointValue(systemEquip, heatingLoopOutput, basicConfig.heatingLoop.toDouble())
        logIt("Changed direction $loopRunningDirection ");
    }

    private fun getBasicDabConfigData() =
        BasicConfig(
            coolingLoop = if (dabSystem.coolingSignal <= 0 ) 0 else dabSystem.coolingSignal,
            heatingLoop = if (dabSystem.heatingSignal <= 0 ) 0 else dabSystem.heatingSignal,
            loopOutput = if (dabSystem.coolingSignal > 0) dabSystem.coolingSignal.toDouble() else dabSystem.heatingSignal.toDouble(),
            weightedAverageCO2 = dabSystem.co2WeightedAverageSum,
        )
}