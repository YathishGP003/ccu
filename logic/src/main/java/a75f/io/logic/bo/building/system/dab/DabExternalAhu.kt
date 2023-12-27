package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain

import a75f.io.domain.api.DomainName.coolingLoopOutput
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.heatingLoopOutput
import a75f.io.domain.api.DomainName.useOutsideTempLockoutCooling
import a75f.io.domain.api.DomainName.useOutsideTempLockoutHeating
import a75f.io.domain.api.DomainName.dabAnalogFanSpeedMultiplier
import a75f.io.domain.api.DomainName.dabHumidityHysteresis
import a75f.io.domain.api.DomainName.dabOutsideTempCoolingLockout
import a75f.io.domain.api.DomainName.dabOutsideTempHeatingLockout
import a75f.io.domain.api.Equip
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleUtil
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
import a75f.io.logic.bo.building.system.updatePointHistoryAndDefaultValue
import a75f.io.logic.bo.building.system.updatePointValue
import a75f.io.logic.bo.building.system.writePointForCcuUser
import a75f.io.logic.interfaces.ModbusWritableDataInterface
import android.content.Intent

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
        calculateSetPoints()
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())
        setSystemPoint("operating and mode", dabSystem.systemState.ordinal.toDouble())
        val systemStatus = statusMessage
        val scheduleStatus = ScheduleManager.getInstance().systemStatusString
        CcuLog.d(L.TAG_CCU_SYSTEM, "systemStatusMessage: $systemStatus")
        CcuLog.d(L.TAG_CCU_SYSTEM, "ScheduleStatus: $scheduleStatus")
        if (CCUHsApi.getInstance()
                .readDefaultStrVal("system and status and message") != systemStatus
        ) {
            CCUHsApi.getInstance().writeDefaultVal("system and status and message", systemStatus)
            Globals.getInstance().applicationContext.sendBroadcast(Intent(ScheduleUtil.ACTION_STATUS_CHANGE))
        }
        if (CCUHsApi.getInstance()
                .readDefaultStrVal("system and scheduleStatus") != scheduleStatus
        ) {
            CCUHsApi.getInstance().writeDefaultVal("system and scheduleStatus", scheduleStatus)
        }

    }

    override fun getStatusMessage(): String =
        if (getBasicDabConfigData().loopOutput > 0) SYSTEM_ON else SYSTEM_OFF

    private fun calculateSetPoints() {

        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        if (systemEquip == null) {
            logIt("DAB_EXTERNAL_AHU_CONTROLLER system equip is empty")
            return
        }
        val externalEquipId = getExternalEquipId()
        val dabConfig = getBasicDabConfigData()
        val occupancyMode = ScheduleManager.getInstance().systemOccupancy
        val conditioningMode = getConditioningMode(systemEquip)
        val currentHumidity = DabSystemController.getInstance().getAverageSystemHumidity()
        val humidityHysteresis = getTunerByDomainName(systemEquip, dabHumidityHysteresis)
        val analogFanMultiplier = getTunerByDomainName(systemEquip, dabAnalogFanSpeedMultiplier)
        logIt(
            " System is $occupancyMode conditioningMode : $conditioningMode" +
                    " coolingLoop ${dabConfig.coolingLoop} heatingLoop ${dabConfig.heatingLoop}" +
                    " weightedAverageCO2 ${dabConfig.weightedAverageCO2} loopOutput ${dabConfig.loopOutput}"
        )
        updateLoopDirection(dabConfig, systemEquip)
        calculateSATSetPoints(
            systemEquip,
            dabConfig,
            externalEquipId,
            conditioningMode,
            hayStack,
            externalSpList,
            loopRunningDirection,
            dabConfig.coolingLoop.toDouble(),
            dabConfig.heatingLoop.toDouble(),
            Tags.DAB
        )
        calculateDSPSetPoints(
            systemEquip,
            dabConfig.loopOutput,
            externalEquipId,
            hayStack,
            externalSpList,
            analogFanMultiplier
        )
        setOccupancyMode(systemEquip, externalEquipId, occupancyMode, hayStack, externalSpList)
        operateDamper(
            systemEquip,
            dabConfig.weightedAverageCO2,
            occupancyMode,
            externalEquipId,
            hayStack,
            externalSpList
        )
        handleHumidityOperation(
            systemEquip,
            externalEquipId,
            occupancyMode,
            hayStack,
            externalSpList,
            humidityHysteresis,
            currentHumidity
        )
        handleDeHumidityOperation(
            systemEquip,
            externalEquipId,
            occupancyMode,
            hayStack,
            externalSpList,
            humidityHysteresis,
            currentHumidity
        )
        Domain.writePointByDomain(systemEquip, equipStatusMessage, statusMessage)
        instance.modbusInterface?.writeSystemModbusRegister(externalEquipId, externalSpList)
    }

    private fun updateLoopDirection(basicConfig: BasicConfig, systemEquip: Equip) {
        if (basicConfig.coolingLoop > 0)
            loopRunningDirection = TempDirection.COOLING
        if (basicConfig.heatingLoop > 0)
            loopRunningDirection = TempDirection.HEATING
        updatePointValue(systemEquip, coolingLoopOutput, basicConfig.coolingLoop.toDouble())
        updatePointValue(systemEquip, heatingLoopOutput, basicConfig.heatingLoop.toDouble())
    }

    private fun getBasicDabConfigData() =
        BasicConfig(
            coolingLoop = dabSystem.coolingSignal,
            heatingLoop = dabSystem.heatingSignal,
            loopOutput = (if (dabSystem.coolingSignal > 0) dabSystem.coolingSignal.toDouble() else dabSystem.heatingSignal.toDouble()),
            weightedAverageCO2 = dabSystem.co2WeightedAverageSum,
        )
}