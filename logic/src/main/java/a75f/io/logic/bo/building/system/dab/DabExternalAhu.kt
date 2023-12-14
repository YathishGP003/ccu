package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.readPoint
import a75f.io.domain.api.Domain.writePointByDomainName
import a75f.io.domain.api.DomainName.conditioningMode
import a75f.io.domain.api.DomainName.coolingLoopOutput
import a75f.io.domain.api.DomainName.dabAnalogFanSpeedMultiplier
import a75f.io.domain.api.DomainName.dabHumidityHysteresis
import a75f.io.domain.api.DomainName.dabOutsideTempCoolingLockout
import a75f.io.domain.api.DomainName.dabOutsideTempHeatingLockout
import a75f.io.domain.api.DomainName.dcvDamperCalculatedSetpoint
import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dcvLoopOutput
import a75f.io.domain.api.DomainName.dehumidifierEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.ductStaticPressureSetpoint
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.fanLoopOutput
import a75f.io.domain.api.DomainName.heatingLoopOutput
import a75f.io.domain.api.DomainName.humidifierEnable
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.satSetpointControlEnable
import a75f.io.domain.api.DomainName.staticPressureSetpointControlEnable
import a75f.io.domain.api.DomainName.supplyAirflowTemperatureSetpoint
import a75f.io.domain.api.DomainName.systemCO2DamperOpeningRate
import a75f.io.domain.api.DomainName.systemCO2Threshold
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemOccupancyMode
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
import a75f.io.domain.api.DomainName.systemtargetMaxInsideHumidty
import a75f.io.domain.api.DomainName.systemtargetMinInsideHumidty
import a75f.io.domain.api.DomainName.useOutsideTempLockoutCooling
import a75f.io.domain.api.DomainName.useOutsideTempLockoutHeating
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.BasicDabConfig
import a75f.io.logic.bo.building.system.SystemMode
import a75f.io.logic.bo.building.system.TempDirection
import a75f.io.logic.bo.building.system.getTempDirection
import a75f.io.logic.bo.building.system.isConfigEnabled
import a75f.io.logic.bo.building.system.logIt
import a75f.io.logic.bo.building.system.mapToSetPoint
import a75f.io.logic.bo.building.system.pushDamperCmd
import a75f.io.logic.bo.building.system.pushDeHumidifierCmd
import a75f.io.logic.bo.building.system.pushDuctStaticPressure
import a75f.io.logic.bo.building.system.pushHumidifierCmd
import a75f.io.logic.bo.building.system.pushOccupancyMode
import a75f.io.logic.bo.building.system.pushSatSetPoints
import a75f.io.logic.bo.building.system.updateDefaultSetPoints
import a75f.io.logic.bo.building.system.updatePointHistoryAndDefaultValue
import a75f.io.logic.bo.building.system.writePointForCcuUser
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.interfaces.ModbusWritableDataInterface
import a75f.io.logic.tuners.TunerUtil
import android.content.Intent
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import java.util.Objects

/**
 * Created by Manjunath K on 12-10-2023.
 */

class DabExternalAhu : DabSystemProfile() {

    companion object {
        const val PROFILE_NAME = "DAB External AHU Controller"
        const val SYSTEM_ON = "System ON"
        const val SYSTEM_OFF = "System OFF"
        private val instance = DabExternalAhu()
        fun getInstance(): DabExternalAhu = instance
    }

    private var modbusInterface: ModbusWritableDataInterface? = null
    private var modbusSetPointsList = ArrayList<String>()
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

    override fun isOutsideTempCoolingLockoutEnabled(hayStack: CCUHsApi): Boolean =
        Domain.readDefaultValByDomainName(useOutsideTempLockoutHeating) > 0

    override fun isOutsideTempHeatingLockoutEnabled(hayStack: CCUHsApi): Boolean =
        Domain.readDefaultValByDomainName(useOutsideTempLockoutCooling) > 0

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

    fun addSystemEquip(config: ProfileConfiguration?, definition: SeventyFiveFProfileDirective?) {
        val profileEquipBuilder = ProfileEquipBuilder(CCUHsApi.getInstance())
        val equipId = profileEquipBuilder.buildEquipAndPoints(
            config!!, definition!!,
            CCUHsApi.getInstance().site!!.id
        )
        updateAhuRef(equipId)
        ControlMote(equipId)
    }

    override fun doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo()
        updateSystemPoints()
    }

    override fun addSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val equip = hayStack.readEntity("equip and system and not modbus")
        if (equip != null && equip.size > 0) {
            if (!equip["profile"]?.toString()
                    .contentEquals(ProfileType.dabExternalAHUController.name)
            ) {
                hayStack.deleteEntityTree(equip["id"].toString())
            }
        }
    }

    @Synchronized
    override fun deleteSystemEquip() {
        val equip = CCUHsApi.getInstance().readEntity("equip and system and not modbus")
        if (equip["profile"]?.toString().contentEquals(ProfileType.dabExternalAHUController.name)) {
            CCUHsApi.getInstance().deleteEntityTree(equip["id"].toString())
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

    private fun getConfigByDomainName(equip: Equip, domainName: String): Boolean {
        val config = getPointByDomain(equip, domainName)
        config?.let { return config.readDefaultVal() == 1.0 }
        return false
    }

    private fun getPointByDomain(equip: Equip, domainName: String): Point? =
        equip.points.entries.find { (it.value.domainName.contentEquals(domainName)) }?.value

    private fun getDefaultValueByDomain(equip: Equip, domainName: String): Double {
        val config = getPointByDomain(equip, domainName)
        config?.let { return config.readDefaultVal() }
        return 0.0
    }

    fun getConfiguration(modelDef: SeventyFiveFProfileDirective): ExternalAhuConfiguration {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        val config = ExternalAhuConfiguration(ProfileType.dabExternalAHUController.name)


        if (systemEquip == null)
            return config

        config.setPointControl.enabled =
            getConfigByDomainName(systemEquip, satSetpointControlEnable)
        config.dualSetPointControl.enabled =
            getConfigByDomainName(systemEquip, dualSetpointControlEnable)
        config.fanStaticSetPointControl.enabled =
            getConfigByDomainName(systemEquip, staticPressureSetpointControlEnable)
        config.dcvControl.enabled = getConfigByDomainName(systemEquip, dcvDamperControlEnable)
        config.occupancyMode.enabled = getConfigByDomainName(systemEquip, occupancyModeControl)
        config.humidifierControl.enabled =
            getConfigByDomainName(systemEquip, humidifierOperationEnable)
        config.dehumidifierControl.enabled =
            getConfigByDomainName(systemEquip, dehumidifierOperationEnable)

        config.satMin.currentVal = getConfigValue(modelDef, systemSATMinimum, systemEquip)
        config.satMax.currentVal = getConfigValue(modelDef, systemSATMaximum, systemEquip)
        config.heatingMinSp.currentVal =
            getConfigValue(modelDef, systemHeatingSATMinimum, systemEquip)
        config.heatingMaxSp.currentVal =
            getConfigValue(modelDef, systemHeatingSATMaximum, systemEquip)
        config.coolingMinSp.currentVal =
            getConfigValue(modelDef, systemCoolingSATMinimum, systemEquip)
        config.coolingMaxSp.currentVal =
            getConfigValue(modelDef, systemCoolingSATMaximum, systemEquip)
        config.fanMinSp.currentVal =
            getConfigValue(modelDef, systemStaticPressureMinimum, systemEquip)
        config.fanMaxSp.currentVal =
            getConfigValue(modelDef, systemStaticPressureMaximum, systemEquip)
        config.dcvMin.currentVal = getConfigValue(modelDef, systemDCVDamperPosMinimum, systemEquip)
        config.dcvMax.currentVal = getConfigValue(modelDef, systemDCVDamperPosMaximum, systemEquip)
        return config
    }

    private fun getConfigValue(
        modelDefinition: SeventyFiveFProfileDirective, domainName: String, equip: Equip): Double {
        val currentValue = getDefaultValueByDomain(equip, domainName)
        if (currentValue != 0.0)
            return currentValue
        val point = modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
        if (point != null)
            return (point.defaultValue ?: 0).toString().toDouble()
        return 0.0
    }

    override fun getStatusMessage(): String =
        if (getBasicDabConfigData().loopOutput > 0) SYSTEM_ON else SYSTEM_OFF


    private fun calculateSetPoints() {
        logIt("=============================================================================")
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        if (systemEquip == null) {
            logIt("DAB_EXTERNAL_AHU_CONTROLLER system equip is empty")
            return
        }
        val externalEquipId = getExternalEquipId()
        val basicDabConfig = getBasicDabConfigData()
        updateLoopDirection(basicDabConfig)
        val occupancyMode = ScheduleManager.getInstance().systemOccupancy
        val conditioningMode =
            SystemMode.values()[Domain.getPointFromDomain(systemEquip, conditioningMode).toInt()]

        logIt("System is $occupancyMode conditioningMode : $conditioningMode")
        logIt("coolingLoop ${basicDabConfig.coolingLoop} heatingLoop ${basicDabConfig.heatingLoop}")
        logIt("weightedAverageCO2 ${basicDabConfig.weightedAverageCO2} loopOutput ${basicDabConfig.loopOutput}")

        calculateSATSetPoints(systemEquip, basicDabConfig, externalEquipId, conditioningMode)
        calculateDSPSetPoints(systemEquip, basicDabConfig.loopOutput, externalEquipId)
        setOccupancyMode(systemEquip, externalEquipId,occupancyMode)
        operateDamper(systemEquip, basicDabConfig.weightedAverageCO2, occupancyMode, externalEquipId)
        handleHumidityOperation(systemEquip, externalEquipId, occupancyMode)
        handleDeHumidityOperation(systemEquip, externalEquipId, occupancyMode)
        writePointByDomainName(systemEquip, equipStatusMessage, statusMessage)
        updatePointValue(systemEquip, coolingLoopOutput, basicDabConfig.coolingLoop.toDouble())
        updatePointValue(systemEquip, heatingLoopOutput, basicDabConfig.heatingLoop.toDouble())
        instance.modbusInterface?.writeSystemModbusRegister(externalEquipId, modbusSetPointsList)
    }


    private fun updateLoopDirection(basicDabConfig: BasicDabConfig) {
        if (basicDabConfig.coolingLoop > 0)
            loopRunningDirection = TempDirection.COOLING
        if (basicDabConfig.heatingLoop > 0)
            loopRunningDirection = TempDirection.HEATING
    }

    private fun calculateSATSetPoints(
        systemEquip: Equip,
        basicDabConfig: BasicDabConfig,
        externalEquipId: String?,
        conditioningMode: SystemMode
    ) {
        val isSetPointEnabled = isConfigEnabled(systemEquip, satSetpointControlEnable)
        val tempDirection = getTempDirection(basicDabConfig.heatingLoop)

        if (L.ccu().oaoProfile != null) {
            if (tempDirection == TempDirection.COOLING &&
                (conditioningMode == SystemMode.COOLONLY || conditioningMode == SystemMode.AUTO)
            ) {
                val smartPurgeDabFanLoopOp: Double = TunerUtil.readTunerValByQuery(
                    "system and purge and dab and fan and loop and output",
                    L.ccu().oaoProfile.equipRef
                )
                if (L.ccu().oaoProfile.isEconomizingAvailable) {
                    val economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery(
                        "oao and economizing and main and cooling and loop and map",
                        L.ccu().oaoProfile.equipRef
                    )
                    basicDabConfig.coolingLoop =
                        (systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap).coerceAtLeast(
                            systemHeatingLoopOp
                        ).coerceAtLeast(smartPurgeDabFanLoopOp).toInt()
                }
            }
        }

        if (isSetPointEnabled) {
            val satSetPointLimits = getSetPointMinMax(systemEquip, basicDabConfig.heatingLoop)
            val satSetPointValue: Double = if (basicDabConfig.loopOutput == 0.0) {
                updateDefaultSetPoints(conditioningMode, systemEquip, loopRunningDirection)
            } else {
                mapToSetPoint(
                    satSetPointLimits.first,
                    satSetPointLimits.second,
                    basicDabConfig.loopOutput
                )
            }
            updatePointValue(systemEquip, supplyAirflowTemperatureSetpoint, satSetPointValue)
            if (externalEquipId != null)
                pushSatSetPoints(
                    hayStack,
                    externalEquipId,
                    satSetPointValue,
                    modbusSetPointsList
                )
            logIt("SATMinimum: ${satSetPointLimits.first} SATMaximum: ${satSetPointLimits.second}")
            logIt("satSetPointValue: $satSetPointValue")

        } else logIt("satSetpointControl disabled")
        logIt("----------------------------------------------------------------")
    }

    private fun calculateDSPSetPoints(
        systemEquip: Equip,
        loopOutput: Double,
        externalEquipId: String?
    ) {
        val isStaticPressureSpEnabled =
            isConfigEnabled(systemEquip, staticPressureSetpointControlEnable)
        if (isStaticPressureSpEnabled) {
            val analogFanMultiplier = TunerUtil.readTunerValByQuery(
                "domainName == \"$dabAnalogFanSpeedMultiplier\"", systemEquip.id
            )

            val fanLoop = loopOutput * analogFanMultiplier
            val min = Domain.getPointFromDomain(systemEquip, systemStaticPressureMinimum)
            val max = Domain.getPointFromDomain(systemEquip, systemStaticPressureMaximum)
            val ductStaticPressureSetPoint: Double = mapToSetPoint(min, max, fanLoop)
            updatePointValue(systemEquip, ductStaticPressureSetpoint, ductStaticPressureSetPoint)
            logIt("systemStaticPressureMinimum: $min systemStaticPressureMaximum: $max analogFanMultiplier: $analogFanMultiplier")
            logIt("ductStaticPressureSetPoint: $ductStaticPressureSetPoint")
            updatePointValue(systemEquip, fanLoopOutput, fanLoop)
            if (externalEquipId != null)
                pushDuctStaticPressure(
                    hayStack,
                    externalEquipId,
                    ductStaticPressureSetPoint,
                    modbusSetPointsList
                )

        } else logIt("StaticPressureSp is disabled")
        logIt("----------------------------------------------------------------")
    }

    private fun setOccupancyMode(systemEquip: Equip, externalEquipId: String?, occupancy: Occupancy) {
        var occupancyMode = 1.0
        if (occupancy == Occupancy.UNOCCUPIED ||
            occupancy == Occupancy.VACATION ||
            occupancy == Occupancy.AUTOAWAY
        ) {
            occupancyMode = 0.0
        }
        val isOccupancyModeControlEnabled = isConfigEnabled(systemEquip, occupancyModeControl)
        if (isOccupancyModeControlEnabled) {
            updatePointValue(systemEquip, systemOccupancyMode, occupancyMode)
            if (externalEquipId != null)
                pushOccupancyMode(
                    hayStack,
                    externalEquipId,
                    occupancyMode,
                    modbusSetPointsList
                )
        } else logIt("OccupancyModeControlEnabled disabled")
    }

    private fun operateDamper(
        systemEquip: Equip,
        systemSensorCO2: Double,
        occupancyMode: Occupancy,
        externalEquipId: String?
    ) {
        val isDcvControlEnabled = isConfigEnabled(systemEquip, dcvDamperControlEnable)
        if (isDcvControlEnabled) {
            val dcvMin = Domain.getPointFromDomain(systemEquip, systemDCVDamperPosMinimum)
            val dcvMax = Domain.getPointFromDomain(systemEquip, systemDCVDamperPosMaximum)
            val damperOpeningRate = Domain.getPointFromDomain(systemEquip, systemCO2DamperOpeningRate)
            val systemCO2Threshold = Domain.getPointFromDomain(systemEquip, systemCO2Threshold)
            var damperOperationPercent = 0.0
            if (systemSensorCO2 > 0 && systemSensorCO2 > systemCO2Threshold
                && (occupancyMode == Occupancy.OCCUPIED
                        || occupancyMode == Occupancy.AUTOFORCEOCCUPIED
                        || occupancyMode == Occupancy.FORCEDOCCUPIED)
            ) {
                damperOperationPercent =
                    (systemSensorCO2 - systemCO2Threshold) / damperOpeningRate
                if (damperOperationPercent > 100)
                    damperOperationPercent = 100.0

            } else if (occupancyMode == Occupancy.UNOCCUPIED
                || occupancyMode == Occupancy.PRECONDITIONING
                || systemSensorCO2 < systemCO2Threshold
            ) {
                damperOperationPercent = 0.0
            }
            val dcvSetPoint = mapToSetPoint(dcvMin, dcvMax, damperOperationPercent)

            updatePointValue(systemEquip, dcvLoopOutput, damperOperationPercent)
            updatePointValue(systemEquip, dcvDamperCalculatedSetpoint, dcvSetPoint)
            if (externalEquipId != null)
                pushDamperCmd(
                    hayStack,
                    externalEquipId,
                    dcvSetPoint,
                    modbusSetPointsList
                )
            logIt("systemDCVDamperPosMinimum: $dcvMin  systemDCVDamperPosMaximum: $dcvMax")
            logIt("systemSensorCO2: $systemSensorCO2 systemCO2DamperOpeningRate $damperOpeningRate systemCO2Threshold: $systemCO2Threshold")
            logIt("$ damperOperationPercent $damperOperationPercent dcvSetPoint: $dcvSetPoint")
        } else logIt("DCV control is disabled")
        logIt("----------------------------------------------------------------")
    }

    private fun handleHumidityOperation(
        systemEquip: Equip,
        externalEquipId: String?,
        occupancyMode: Occupancy
    ) {
        val currentHumidifierPortStatus =
            Domain.getPointHisFromDomain(systemEquip, humidifierEnable)
        var newHumidifier = 0.0

        // Disable humidifier control when in UNOCCUPIED or VACATION mode
        if (occupancyMode == Occupancy.UNOCCUPIED || occupancyMode == Occupancy.VACATION) {
            if (currentHumidifierPortStatus == 1.0) {
                updatePointValue(systemEquip, humidifierEnable, 0.0)
                if (externalEquipId != null) {
                    pushHumidifierCmd(
                        hayStack,
                        externalEquipId,
                        0.0,
                        modbusSetPointsList
                    )
                }
            }
            return
        }

        // Continue with humidifier control logic
        val isHumidifierEnabled = isConfigEnabled(systemEquip, humidifierOperationEnable)
        if (isHumidifierEnabled) {
            val currentHumidity = DabSystemController.getInstance().getAverageSystemHumidity()
            val humidityHysteresis = TunerUtil.readTunerValByQuery(
                "domainName == \"$dabHumidityHysteresis\"", systemEquip.id
            )
            val targetMinInsideHumidity =
                Domain.getPointFromDomain(systemEquip, systemtargetMinInsideHumidty)

            // Combine humidity conditions for readability
            if (currentHumidity > 0 && currentHumidity < targetMinInsideHumidity) {
                newHumidifier = 1.0
            } else if (currentHumidifierPortStatus > 0 && currentHumidity > (targetMinInsideHumidity + humidityHysteresis)) {
                newHumidifier = 0.0
            }

            logIt("currentHumidity $currentHumidity humidityHysteresis: $humidityHysteresis")
            logIt("targetMinInsideHumidity $targetMinInsideHumidity Humidifier $newHumidifier")
        } else {
            logIt("Humidifier control is disabled")
        }

        // Update humidifier status if changed
        if (currentHumidifierPortStatus != newHumidifier) {
            updatePointValue(systemEquip, humidifierEnable, newHumidifier)
            if (externalEquipId != null && currentHumidifierPortStatus != 0.0) {
                pushHumidifierCmd(
                    hayStack,
                    externalEquipId,
                    newHumidifier,
                    modbusSetPointsList
                )
            }
        }
    }

    private fun handleDeHumidityOperation(
        systemEquip: Equip,
        externalEquipId: String?,
        occupancyMode: Occupancy
    ) {
        val currentDeHumidifierPortStatus =
            Domain.getPointHisFromDomain(systemEquip, dehumidifierEnable)
        var newDeHumidifier = 0.0

        // Disable dehumidifier control when in UNOCCUPIED or VACATION mode
        if (occupancyMode == Occupancy.UNOCCUPIED || occupancyMode == Occupancy.VACATION) {
            if (currentDeHumidifierPortStatus == 1.0) {
                updatePointValue(systemEquip, dehumidifierEnable, 0.0)
                if (externalEquipId != null) {
                    pushDeHumidifierCmd(
                        hayStack,
                        externalEquipId,
                        0.0,
                        modbusSetPointsList
                    )
                }
            }
            return
        }

        // Continue with dehumidifier control logic
        val isDeHumidifierEnabled = isConfigEnabled(systemEquip, dehumidifierOperationEnable)
        if (isDeHumidifierEnabled) {
            val currentHumidity = DabSystemController.getInstance().getAverageSystemHumidity()
            val humidityHysteresis = Domain.getPointFromDomain(systemEquip, dabHumidityHysteresis)
            val targetMaxInsideHumidity =
                Domain.getPointFromDomain(systemEquip, systemtargetMaxInsideHumidty)

            // Combine humidity conditions for readability
            if (currentHumidity > 0 && currentHumidity > targetMaxInsideHumidity) {
                newDeHumidifier = 1.0
            } else if (currentDeHumidifierPortStatus > 0 && currentHumidity < (targetMaxInsideHumidity - humidityHysteresis)) {
                newDeHumidifier = 0.0
            }

            logIt(" targetMaxInsideHumidity: $targetMaxInsideHumidity DeHumidifier $newDeHumidifier")
        } else {
            logIt("DeHumidifier control is disabled")
        }

        // Update dehumidifier status if changed
        if (currentDeHumidifierPortStatus != newDeHumidifier) {
            updatePointValue(systemEquip, dehumidifierEnable, newDeHumidifier)
            if (externalEquipId != null && currentDeHumidifierPortStatus != 0.0) {
                pushDeHumidifierCmd(
                    hayStack,
                    externalEquipId,
                    newDeHumidifier,
                    modbusSetPointsList
                )
            }
        }
    }

    private fun getExternalEquipId(): String? {
        // TODO check if bacnet is configured then we need to find bacnet equip id
        val modbusEquip =
            CCUHsApi.getInstance().readEntity("system and equip and modbus and not emr and not btu")
        if (modbusEquip.isNotEmpty()) {
            return modbusEquip["id"].toString()
        }
        return null
    }

    private fun updatePointValue(equip: Equip, domainName: String, pointValue: Double) {
        writePointByDomainName(equip, domainName, pointValue)
    }

    fun getSetPoint(domainName: String, preFix: String): String {
        val point = readPoint(domainName)
        if (point.isEmpty()) return ""
        val unit = Objects.requireNonNull(point["unit"]).toString()
        val value = CCUHsApi.getInstance().readHisValById(point["id"].toString())
        return ("$preFix  $value  $unit")
    }

    fun getConfigValue(domainName: String): Boolean {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        return getConfigByDomainName(systemEquip!!, domainName)
    }

    fun getModbusPointValue(query: String): String {
        val equipId = getExternalEquipId()
        val point = CCUHsApi.getInstance().readEntity("$query and equipRef == \"$equipId\"")

        if (point.isEmpty()) {
            return ""
        }

        val pointId = point["id"].toString()
        val value = CCUHsApi.getInstance().readHisValById(pointId)
        val unit = point["unit"]

        return "Current $value $unit"
    }


    private fun getSetPointMinMax(equip: Equip, heatingLoop: Int): Pair<Double, Double> {
        val tempDirection = getTempDirection(heatingLoop)
        val isDualSetPointEnabled = isConfigEnabled(equip, dualSetpointControlEnable)

        val minKey: String
        val maxKey: String

        when (tempDirection) {
            TempDirection.COOLING -> {
                minKey = if (isDualSetPointEnabled) systemCoolingSATMaximum else systemSATMaximum
                maxKey = if (isDualSetPointEnabled) systemCoolingSATMinimum else systemSATMinimum
            }

            TempDirection.HEATING -> {
                minKey = if (isDualSetPointEnabled) systemHeatingSATMinimum else systemSATMinimum
                maxKey = if (isDualSetPointEnabled) systemHeatingSATMaximum else systemSATMaximum
            }
        }

        val minSetPoint = Domain.getPointFromDomain(equip, minKey)
        val maxSetPoint = Domain.getPointFromDomain(equip, maxKey)

        return Pair(minSetPoint, maxSetPoint)
    }

    private fun getBasicDabConfigData() =
        BasicDabConfig(
            coolingLoop = dabSystem.coolingSignal,
            heatingLoop = dabSystem.heatingSignal,
            loopOutput = (if (dabSystem.coolingSignal > 0) dabSystem.coolingSignal.toDouble() else dabSystem.heatingSignal.toDouble()),
            weightedAverageCO2 = dabSystem.co2WeightedAverageSum,
        )


}