package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.readPoint
import a75f.io.domain.api.Domain.writePointByDomain
import a75f.io.domain.api.DomainName.coolingLoopOutput
import a75f.io.domain.api.DomainName.dabHumidityHysteresis
import a75f.io.domain.api.DomainName.dabOutsideTempCoolingLockout
import a75f.io.domain.api.DomainName.dabOutsideTempHeatingLockout
import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.equipStatusMessage
import a75f.io.domain.api.DomainName.heatingLoopOutput
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.satSetpointControlEnable
import a75f.io.domain.api.DomainName.staticPressureSetpointControlEnable
import a75f.io.domain.api.DomainName.systemCO2DamperOpeningRate
import a75f.io.domain.api.DomainName.systemCO2Target
import a75f.io.domain.api.DomainName.systemCO2Threshold
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
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
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.BasicConfig
import a75f.io.logic.bo.building.system.TempDirection
import a75f.io.logic.bo.building.system.calculateDSPSetPoints
import a75f.io.logic.bo.building.system.calculateSATSetPoints
import a75f.io.logic.bo.building.system.getConditioningMode
import a75f.io.logic.bo.building.system.getTunerByDomainName
import a75f.io.logic.bo.building.system.handleDeHumidityOperation
import a75f.io.logic.bo.building.system.handleHumidityOperation
import a75f.io.logic.bo.building.system.logIt
import a75f.io.logic.bo.building.system.operateDamper
import a75f.io.logic.bo.building.system.setOccupancyMode
import a75f.io.logic.bo.building.system.updatePointHistoryAndDefaultValue
import a75f.io.logic.bo.building.system.updatePointValue
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
        Domain.readDefaultValByDomain(useOutsideTempLockoutHeating) > 0

    override fun isOutsideTempHeatingLockoutEnabled(hayStack: CCUHsApi): Boolean =
        Domain.readDefaultValByDomain(useOutsideTempLockoutCooling) > 0

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
        config.co2Threshold.currentVal = getConfigValue(modelDef, systemCO2Threshold, systemEquip)
        config.damperOpeningRate.currentVal = getConfigValue(modelDef, systemCO2DamperOpeningRate, systemEquip)
        config.co2Target.currentVal = getConfigValue(modelDef, systemCO2Target, systemEquip)
        return config
    }

    private fun getConfigValue(
        modelDefinition: SeventyFiveFProfileDirective, domainName: String, equip: Equip
    ): Double {
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
            dabConfig.heatingLoop.toDouble()
        )
        calculateDSPSetPoints(
            systemEquip,
            dabConfig.loopOutput,
            externalEquipId,
            hayStack,
            externalSpList
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
        writePointByDomain(systemEquip, equipStatusMessage, statusMessage)
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

    private fun getExternalEquipId(): String? {
        // TODO check if bacnet is configured then we need to find bacnet equip id
        val modbusEquip =
            CCUHsApi.getInstance().readEntity("system and equip and modbus and not emr and not btu")
        if (modbusEquip.isNotEmpty()) {
            return modbusEquip["id"].toString()
        }
        return null
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

    private fun getBasicDabConfigData() =
        BasicConfig(
            coolingLoop = dabSystem.coolingSignal,
            heatingLoop = dabSystem.heatingSignal,
            loopOutput = (if (dabSystem.coolingSignal > 0) dabSystem.coolingSignal.toDouble() else dabSystem.heatingSignal.toDouble()),
            weightedAverageCO2 = dabSystem.co2WeightedAverageSum,
        )

}