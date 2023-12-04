package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.writePointByDomainName
import a75f.io.domain.api.DomainName.conditioningMode
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.api.DomainName.dabAnalogFanSpeedMultiplier
import a75f.io.domain.api.DomainName.dabHumidityHysteresis
import a75f.io.domain.api.DomainName.dcvDamperCalculatedSetpoint
import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dcvLoopOutput
import a75f.io.domain.api.DomainName.dehumidifierEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.ductStaticPressureSetpoint
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
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.schedules.Occupancy
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.schedules.ScheduleUtil
import a75f.io.logic.bo.building.system.mapToSetPoint
import a75f.io.logic.bo.building.system.pushDamperCmd
import a75f.io.logic.bo.building.system.pushDeHumidifierCmd
import a75f.io.logic.bo.building.system.pushDuctStaticPressure
import a75f.io.logic.bo.building.system.pushHumidifierCmd
import a75f.io.logic.bo.building.system.pushOccupancyMode
import a75f.io.logic.bo.building.system.pushSatSetPoints
import a75f.io.logic.bo.haystack.device.ControlMote
import a75f.io.logic.interfaces.ModbusWritableDataInterface
import android.content.Intent
import android.util.Log
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 12-10-2023.
 */

class DabExternalAhu : DabSystemProfile() {

    private var modbusInterface: ModbusWritableDataInterface? = null
    private var setPointsList = ArrayList<String>()
    override fun getProfileName(): String {
        return "DAB External AHU Controller"
    }

    override fun getProfileType(): ProfileType {
        return ProfileType.SYSTEM_DAB_EXTERNAL_AHU
    }

    override fun getStatusMessage(): String {
        return ""
    }

    fun setModbusWritableDataInterface(callBack: ModbusWritableDataInterface) {
        modbusInterface = callBack
    }

    companion object {
        private val instance = DabExternalAhu()
        fun getInstance(): DabExternalAhu {
            return instance
        }
    }

    fun addSystemEquip(config: ProfileConfiguration?, definition: SeventyFiveFProfileDirective?) {
        val profileEquipBuilder = ProfileEquipBuilder(CCUHsApi.getInstance())
        val equipId = profileEquipBuilder.buildEquipAndPoints(
            config!!, definition!!,
            CCUHsApi.getInstance().site!!.id,
            ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name
        )
        updateAhuRef(equipId)
        ControlMote(equipId)
    }

    override fun doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo()
        updateSystemPoints()
    }


    @Synchronized
    private fun updateSystemPoints() {

        val dabSystem = DabSystemController.getInstance()
        calculateSetPoints(dabSystem)
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

    override fun addSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val equip = hayStack.readEntity("equip and system and not modbus")
        if (equip != null && equip.size > 0) {
            if (!equip["profile"]?.toString()
                    .contentEquals(ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name)
            ) {
                hayStack.deleteEntityTree(equip["id"].toString())
            }
        }
    }

    @Synchronized
    override fun deleteSystemEquip() {
        val equip = CCUHsApi.getInstance().readEntity("equip and system and not modbus")
        if (equip["profile"]?.toString().contentEquals(ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name)) {
            CCUHsApi.getInstance().deleteEntityTree(equip["id"].toString())
        }
    }

    override fun isCoolingAvailable(): Boolean {
        return true
    }

    override fun isHeatingAvailable(): Boolean {
        return true
    }

    override fun isCoolingActive(): Boolean {
        return true
    }

    override fun isHeatingActive(): Boolean {
        return true
    }

    private fun getConfigByDomainName(equip: Equip, domainName: String): Boolean {
        val config = getPointByDomain(equip, domainName)
        config?.let { return config.readDefaultVal() == 1.0 }
        return false
    }

    private fun getPointByDomain(equip: Equip, domainName: String): Point? {
        return equip.points.entries.find { (it.value.domainName.contentEquals(domainName)) }?.value
    }

    private fun getDefaultValueByDomain(equip: Equip, domainName: String): Double {
        val config = getPointByDomain(equip, domainName)
        config?.let { return config.readDefaultVal() }
        return 0.0
    }

    fun getConfiguration(modelDef: SeventyFiveFProfileDirective): ExternalAhuConfiguration {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        val config = ExternalAhuConfiguration()
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
        modelDefinition: SeventyFiveFProfileDirective,
        domainName: String,
        equip: Equip
    ): Double {
        val currentValue = getDefaultValueByDomain(equip, domainName)
        if (currentValue != 0.0)
            return currentValue
        val point = modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
        if (point != null) {
            return (point.defaultValue ?: 0).toString().toDouble()
        }
        return 0.0
    }


    private fun calculateSetPoints(dabSystemController: DabSystemController) {
        logIt("=============================================================================")
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        if (systemEquip == null) {
            logIt("DAB_EXTERNAL_AHU_CONTROLLER system equip is empty")
            return
        }
        val externalEquipId = getExternalEquipId()
        val coolingLoop = dabSystemController.coolingSignal
        val heatingLoop = dabSystemController.heatingSignal
        val weightedAverageCO2 = dabSystemController.co2WeightedAverageSum
        val loopOutput = if (coolingLoop > 0) coolingLoop.toDouble() else heatingLoop.toDouble()
        val occupancyMode = ScheduleManager.getInstance().systemOccupancy
        val conditioningMode = StandaloneConditioningMode.values()[Domain.getPointFromDomain(systemEquip, conditioningMode).toInt()]
        logIt("System is $occupancyMode conditioningMode : $conditioningMode")
        logIt("coolingLoop $coolingLoop heatingLoop $heatingLoop")
        logIt("weightedAverageCO2 $weightedAverageCO2 loopOutput $loopOutput")
        if (conditioningMode == StandaloneConditioningMode.OFF)
            return
        calculateSATSetPoints(systemEquip, heatingLoop, loopOutput, externalEquipId)
        calculateDuctStaticPressureSetPoints(systemEquip, loopOutput, externalEquipId)
        setOccupancyMode(systemEquip, externalEquipId)
        doDCVAction(systemEquip, weightedAverageCO2, occupancyMode, externalEquipId)
        handleHumidityOperation(systemEquip, externalEquipId, occupancyMode)
        handleDeHumidityOperation(systemEquip, externalEquipId, occupancyMode)

        instance.modbusInterface?.writeSystemModbusRegister(externalEquipId, setPointsList)
    }

    private fun calculateSATSetPoints(
        systemEquip: Equip,
        heatingLoop: Int,
        loopOutput: Double,
        externalEquipId: String?,
    ) {
        val isSetPointEnabled =
            Domain.getPointFromDomain(systemEquip, satSetpointControlEnable) == 1.0
        /*
        // TODO CHECK WITH PRODUCT TEAM HOW IT WORKS
          if (tempDirection == TempDirection.COOLING)  {

          }
          val smartPurgeDabFanLoopOp: Double = TunerUtil.readTunerValByQuery(
              "system and purge and dab and fan and loop and output",
              L.ccu().oaoProfile.equipRef
          )
          if (L.ccu().oaoProfile.isEconomizingAvailable) {
              val economizingToMainCoolingLoopMap = TunerUtil.readTunerValByQuery(
                  "oao and economizing and main and cooling and loop and map",
                  L.ccu().oaoProfile.equipRef
              )
              loopOutput = Math.max(
                  Math.max(
                      systemCoolingLoopOp * 100 / economizingToMainCoolingLoopMap,
                      systemHeatingLoopOp
                  ), smartPurgeDabFanLoopOp
              )
          }*/

        if (isSetPointEnabled) {
            val satSetPointLimits = getSetPointMinMax(systemEquip, heatingLoop)
            val satSetPointValue =
                mapToSetPoint(satSetPointLimits.first, satSetPointLimits.second, loopOutput)
            updateSetPoint(systemEquip, supplyAirflowTemperatureSetpoint, satSetPointValue)
            if (externalEquipId != null)
                pushSatSetPoints(
                    CCUHsApi.getInstance(),
                    externalEquipId,
                    satSetPointValue,
                    setPointsList
                )
            logIt("SATMinimum: ${satSetPointLimits.first} SATMaximum: ${satSetPointLimits.second}")
            logIt("satSetPointValue: $satSetPointValue")

        } else logIt("satSetpointControl disabled")
        logIt("----------------------------------------------------------------")
    }

    private fun calculateDuctStaticPressureSetPoints(
        systemEquip: Equip,
        loopOutput: Double,
        externalEquipId: String?
    ) {
        val isStaticPressureSpEnabled =
            Domain.getPointFromDomain(systemEquip, dualSetpointControlEnable) == 1.0
        if (isStaticPressureSpEnabled) {
            val analogFanMultiplier =
                Domain.readPointValueByDomainName(dabAnalogFanSpeedMultiplier, systemEquip.id)
            val fanLoopOutput = loopOutput * analogFanMultiplier
            val min = Domain.getPointFromDomain(systemEquip, systemStaticPressureMinimum)
            val max = Domain.getPointFromDomain(systemEquip, systemStaticPressureMaximum)
            val ductStaticPressureSetPoint = mapToSetPoint(min, max, fanLoopOutput)
            updateSetPoint(systemEquip, ductStaticPressureSetpoint, ductStaticPressureSetPoint)
            logIt("systemStaticPressureMinimum: $min systemStaticPressureMaximum: $max analogFanMultiplier: $analogFanMultiplier")
            logIt("ductStaticPressureSetPoint: $ductStaticPressureSetPoint")
            if (externalEquipId != null)
                pushDuctStaticPressure(
                    CCUHsApi.getInstance(),
                    externalEquipId,
                    ductStaticPressureSetPoint,
                    setPointsList
                )

        } else logIt("StaticPressureSp is disabled")
        logIt("----------------------------------------------------------------")
    }

    private fun setOccupancyMode(systemEquip: Equip, externalEquipId: String?) {
        val occupancy = if (DabSystemController.getInstance().currSystemOccupancy == Occupancy.UNOCCUPIED) 0.0 else 1.0
        val isOccupancyModeControlEnabled =
            Domain.getPointFromDomain(systemEquip, occupancyModeControl) == 1.0
        if (isOccupancyModeControlEnabled) {
            updateSetPoint(systemEquip, systemOccupancyMode, occupancy)
            if (externalEquipId != null)
                pushOccupancyMode(CCUHsApi.getInstance(), externalEquipId, occupancy, setPointsList)
        } else logIt("OccupancyModeControlEnabled disabled")
    }

    private fun doDCVAction(systemEquip: Equip, systemSensorCO2: Double, occupancyMode: Occupancy,externalEquipId: String?) {
        val isDcvControlEnabled =
            Domain.getPointFromDomain(systemEquip, dcvDamperControlEnable) == 1.0
        if (isDcvControlEnabled ) {
            val dcvMin = Domain.getPointFromDomain(systemEquip, systemDCVDamperPosMinimum)
            val dcvMax = Domain.getPointFromDomain(systemEquip, systemDCVDamperPosMaximum)
            val systemCO2DamperOpeningRate =
                Domain.getPointFromDomain(systemEquip, systemCO2DamperOpeningRate)
            val systemCO2Threshold = Domain.getPointFromDomain(systemEquip, systemCO2Threshold)
            var damperOperationPercent = 0.0
            if (systemSensorCO2 > 0 && systemSensorCO2 > systemCO2Threshold
                        &&(occupancyMode == Occupancy.OCCUPIED
                        || occupancyMode == Occupancy.AUTOFORCEOCCUPIED
                        || occupancyMode == Occupancy.FORCEDOCCUPIED)) {
                damperOperationPercent =
                    (systemSensorCO2 - systemCO2Threshold) / systemCO2DamperOpeningRate
                if (damperOperationPercent > 100)
                    damperOperationPercent = 100.0

            } else if (occupancyMode == Occupancy.UNOCCUPIED
                || occupancyMode == Occupancy.PRECONDITIONING
                || systemSensorCO2 < systemCO2Threshold) {
                damperOperationPercent = 0.0
            }
            val dcvSetPoint = mapToSetPoint(dcvMin, dcvMax, damperOperationPercent)

            updateSetPoint(systemEquip, dcvLoopOutput, damperOperationPercent)
            updateSetPoint(systemEquip, dcvDamperCalculatedSetpoint, dcvSetPoint)
            if (externalEquipId != null)
                pushDamperCmd(CCUHsApi.getInstance(), externalEquipId, dcvSetPoint, setPointsList)
            logIt("systemDCVDamperPosMinimum: $dcvMin  systemDCVDamperPosMaximum: $dcvMax")
            logIt("systemSensorCO2: $systemSensorCO2 systemCO2DamperOpeningRate $systemCO2DamperOpeningRate systemCO2Threshold: $systemCO2Threshold")
            logIt("$ damperOperationPercent $damperOperationPercent dcvSetPoint: $dcvSetPoint")
        } else logIt("DCV control is disabled")
        logIt("----------------------------------------------------------------")
    }

    private fun handleHumidityOperation(systemEquip: Equip, externalEquipId: String?, occupancyMode: Occupancy) {
        if (occupancyMode != Occupancy.UNOCCUPIED || occupancyMode != Occupancy.VACATION) {
            val isHumidifierEnabled =
                Domain.getPointFromDomain(systemEquip, humidifierOperationEnable) == 1.0
            if (isHumidifierEnabled) {
                val currentHumidity = DabSystemController.getInstance().getAverageSystemHumidity()
                val humidityHysteresis =
                    Domain.getPointFromDomain(systemEquip, dabHumidityHysteresis)
                val targetMinInsideHumidity =
                    Domain.getPointFromDomain(systemEquip, systemtargetMinInsideHumidty)
                val currentHumidifierPortStatus =
                    Domain.getPointHisFromDomain(systemEquip, humidifierEnable)
                var newHumidifier = 0.0
                if (currentHumidity > 0) {
                    if (currentHumidity < targetMinInsideHumidity) {
                        newHumidifier = 1.0
                    } else if (currentHumidifierPortStatus > 0) {
                        newHumidifier =
                            if (currentHumidity > (targetMinInsideHumidity + humidityHysteresis)) 0.0 else 1.0
                    }
                } else newHumidifier = 0.0

                logIt("currentHumidity $currentHumidity humidityHysteresis: $humidityHysteresis")
                logIt("targetMinInsideHumidity $targetMinInsideHumidity Humidifier $newHumidifier")
                updateSetPoint(systemEquip, humidifierEnable, newHumidifier)
                if (externalEquipId != null)
                    pushHumidifierCmd(
                        CCUHsApi.getInstance(),
                        externalEquipId,
                        newHumidifier,
                        setPointsList
                    )
            } else logIt("Humidifier control is disabled")
        }
        logIt("----------------------------------------------------------------")

    }

    private fun handleDeHumidityOperation(systemEquip: Equip, externalEquipId: String?, occupancyMode: Occupancy) {
        if (occupancyMode != Occupancy.UNOCCUPIED || occupancyMode != Occupancy.VACATION) {
            val isDeHumidifierEnabled =
                Domain.getPointFromDomain(systemEquip, dehumidifierOperationEnable) == 1.0
            if (isDeHumidifierEnabled) {
                val currentHumidity = DabSystemController.getInstance().getAverageSystemHumidity()
                val humidityHysteresis =
                    Domain.getPointFromDomain(systemEquip, dabHumidityHysteresis)
                val currentDeHumidifierPortStatus =
                    Domain.getPointHisFromDomain(systemEquip, dehumidifierEnable)
                val targetMaxInsideHumidity =
                    Domain.getPointFromDomain(systemEquip, systemtargetMaxInsideHumidty)
                var newDeHumidifier = 0.0
                if (currentHumidity > 0) {
                    if (currentHumidity > targetMaxInsideHumidity) {
                        newDeHumidifier = 1.0
                    } else if (currentDeHumidifierPortStatus > 0) {
                        newDeHumidifier =
                            if (currentHumidity < (targetMaxInsideHumidity - humidityHysteresis)) 0.0 else 1.0
                    }
                } else newDeHumidifier = 0.0
                updateSetPoint(systemEquip, dehumidifierEnable, newDeHumidifier)
                if (externalEquipId != null)
                    pushDeHumidifierCmd(
                        CCUHsApi.getInstance(),
                        externalEquipId,
                        newDeHumidifier,
                        setPointsList
                    )
                logIt(" targetMaxInsideHumidity: $targetMaxInsideHumidity DeHumidifier $newDeHumidifier")
            } else logIt("DeHumidifier control is disabled")
        }
        logIt("----------------------------------------------------------------")

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

    private fun updateSetPoint(equip: Equip, domainName: String, setPointValue: Double) {
        writePointByDomainName(equip, domainName, setPointValue)
    }

    private fun getSetPointMinMax(equip: Equip, heatingLoop: Int): Pair<Double, Double> {

        val isDualSetPointEnabled =
            Domain.getPointFromDomain(equip, dualSetpointControlEnable) == 1.0
        return if (isDualSetPointEnabled) {
            if (getTempDirection(heatingLoop) == TempDirection.COOLING) {
                Pair(
                    Domain.getPointFromDomain(equip, systemCoolingSATMinimum),
                    Domain.getPointFromDomain(equip, systemCoolingSATMaximum),
                )
            } else {
                Pair(
                    Domain.getPointFromDomain(equip, systemHeatingSATMinimum),
                    Domain.getPointFromDomain(equip, systemHeatingSATMaximum),
                )
            }
        } else {
            Pair(
                Domain.getPointFromDomain(equip, systemSATMinimum),
                Domain.getPointFromDomain(equip, systemSATMaximum),
            )
        }
    }

    private fun getTempDirection(heatingLoop: Int): TempDirection {
        return if (heatingLoop > 0)
            TempDirection.HEATING
        else
            TempDirection.COOLING
    }

    enum class TempDirection {
        COOLING, HEATING
    }

    private fun logIt(msg: String) {
        Log.i("DEV_DEBUG", msg)
    }
}