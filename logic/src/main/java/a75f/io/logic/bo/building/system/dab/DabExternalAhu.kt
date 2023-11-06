package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.writePointByDomainName
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.api.dabAnalogFanSpeedMultiplier
import a75f.io.domain.api.dabHumidityHysteresis
import a75f.io.domain.api.dcvDamperControlEnable
import a75f.io.domain.api.dehumidifierOperationEnable
import a75f.io.domain.api.dualSetpointControlEnable
import a75f.io.domain.api.ductStaticPressureSetpoint
import a75f.io.domain.api.humidifierOperationEnable
import a75f.io.domain.api.occupancyModeControl
import a75f.io.domain.api.outsideHumidity
import a75f.io.domain.api.satSetpointControlEnable
import a75f.io.domain.api.staticPressureSetpointControlEnable
import a75f.io.domain.api.supplyAirflowTemperatureSetpoint
import a75f.io.domain.api.systemCO2DamperOpeningRate
import a75f.io.domain.api.systemCO2Threshold
import a75f.io.domain.api.systemCoolingSATMaximum
import a75f.io.domain.api.systemCoolingSATMinimum
import a75f.io.domain.api.systemDCVDamperPosMaximum
import a75f.io.domain.api.systemDCVDamperPosMinimum
import a75f.io.domain.api.systemHeatingSATMaximum
import a75f.io.domain.api.systemHeatingSATMinimum
import a75f.io.domain.api.systemSATMaximum
import a75f.io.domain.api.systemSATMinimum
import a75f.io.domain.api.systemStaticPressureMaximum
import a75f.io.domain.api.systemStaticPressureMinimum
import a75f.io.domain.api.systemtargetMaxInsideHumidty
import a75f.io.domain.api.systemtargetMinInsideHumidty
import a75f.io.domain.api.targetDehumidifier
import a75f.io.domain.api.targetHumidifier
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
import a75f.io.logic.bo.building.system.mapToSetPoint
import a75f.io.logic.bo.haystack.device.ControlMote
import android.content.Intent
import android.util.Log
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 12-10-2023.
 */

class DabExternalAhu : DabSystemProfile() {

    override fun getProfileName(): String {
        return "DAB External AHU Controller"
    }

    override fun getProfileType(): ProfileType {
        return ProfileType.SYSTEM_DAB_EXTERNAL_AHU
    }

    override fun getStatusMessage(): String {
        return ""
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

        calculateSetPoints()
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())

        val dabSystem = DabSystemController.getInstance()
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

    private fun calculateSetPoints() {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        val coolingLoop = DabSystemController.getInstance().coolingSignal
        val heatingLoop = DabSystemController.getInstance().heatingSignal
        val loopOutput = if (coolingLoop > 0) coolingLoop.toDouble() else heatingLoop.toDouble()
        val satSetPointLimits = getSetPointMinMax(systemEquip!!, heatingLoop)
        val satSetPointValue =
            mapToSetPoint(satSetPointLimits.first, satSetPointLimits.second, loopOutput)
        updateSetPoint(systemEquip, supplyAirflowTemperatureSetpoint, satSetPointValue)

        val analogFanMultiplier =
            Domain.readPointValueByDomainName(dabAnalogFanSpeedMultiplier, systemEquip.id)
        val fanLoopOutput = loopOutput * analogFanMultiplier
        val ductStaticPressureLimits = getDuctStaticPressureLimits(systemEquip)
        val ductStaticPressureSetPoint = mapToSetPoint(
            ductStaticPressureLimits.first,
            ductStaticPressureLimits.second,
            fanLoopOutput
        )
        updateSetPoint(systemEquip, ductStaticPressureSetpoint, ductStaticPressureSetPoint)

        val damperLimits = damperPositionLimits(systemEquip)

        logIt(
            "satSetPointLimits min: ${satSetPointLimits.first} max: ${satSetPointLimits.second}" +
                    "\n ductStaticPressureLimits min: ${ductStaticPressureLimits.first} max: ${ductStaticPressureLimits.second}" +
                    "\n Cooling Loop : $coolingLoop Heating Loop : $heatingLoop" +
                    "\n satSetPointValue: $satSetPointValue" +
                    "\n ductStaticPressureSetPoint: $ductStaticPressureSetPoint"
        )

    }

    private fun updateSetPoint(equip: Equip, domainName: String, setPointValue: Double) {
        writePointByDomainName(equip, domainName, setPointValue)
    }

    private fun getDuctStaticPressureLimits(equip: Equip): Pair<Double, Double> {
        return Pair(
            Domain.getPointFromDomain(equip, systemStaticPressureMinimum),
            Domain.getPointFromDomain(equip, systemStaticPressureMaximum),
        )
    }

    private fun damperPositionLimits(equip: Equip): Pair<Double, Double> {
        return Pair(
            Domain.getPointFromDomain(equip, systemDCVDamperPosMinimum),
            Domain.getPointFromDomain(equip, systemDCVDamperPosMaximum),
        )
    }

    private fun getSetPointMinMax(equip: Equip, heatingLoop: Int): Pair<Double, Double> {

        val isDualSetPointEnabled =
            Domain.getPointFromDomain(equip, satSetpointControlEnable) == 1.0
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
                Domain.getPointFromDomain(equip, systemSATMinimum),
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

    fun logIt(msg: String) {
        Log.i("DEV_DEBUG", msg)
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
        val equip = CCUHsApi.getInstance().read("equip and system and not modbus")
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
        config.targetHumidity.currentVal = getConfigValue(modelDef, targetHumidifier, systemEquip)
        config.targetDeHumidity.currentVal =
            getConfigValue(modelDef, targetDehumidifier, systemEquip)
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

    fun doDCVAction(
        modelDefinition: SeventyFiveFProfileDirective,
        zoneCO2Threshold: Double,
        zoneCO2DamperOpeningRate: Double,
        systemEquip: Equip
    ) {
        val systemCO2DamperOpeningRate =
            getConfigValue(modelDefinition, systemCO2DamperOpeningRate, systemEquip)
        val systemCO2Threshold = getConfigValue(modelDefinition, systemCO2Threshold, systemEquip)
        val systemSensorCO2 = getConfigValue(modelDefinition, systemSATMinimum, systemEquip)

        /*
         CO2 sensor based control of the damper opening can be used for better ventilation of a given space
          . For DCV based ventilation which can be used for controlling an independent damper for ventilation
          air into the zone.  This allows for very granular control of the IAQ. This is a huge competitive
          advantage as no other thermostat currently allows for this.

         If systemSensorCO2 > systemCO2Threshold and the zone is in occupied or forced occupied state, then
         dcvLoopOutput = (systemSensorCO2 - systemCO2Threshold)/systemCO2DamperOpeningRate

         If  systemSensorCO2 < systemCO2Threshold or the zone is in unoccupied, pre conditioning, then
         dcvLoopOutput = 0
        */
        if (systemSensorCO2 > 0 && systemSensorCO2 > systemCO2Threshold) {
            var dcvLoopOutput = (systemSensorCO2 - systemCO2Threshold) / systemCO2DamperOpeningRate
            var damperOperationPercent =
                (systemSensorCO2 - zoneCO2Threshold) / zoneCO2DamperOpeningRate
            if (damperOperationPercent > 100)
                damperOperationPercent = 100.0

        } else if (systemSensorCO2 < zoneCO2Threshold) {
            val damperOperationPercent = 100.0
        }
    }

    private fun handleHumidityOperation(systemEquip: Equip, modelDefinition: SeventyFiveFProfileDirective){
        val currentHumidity = getConfigValue(modelDefinition, outsideHumidity, systemEquip)
        val humidityHysteresis = getConfigValue(modelDefinition, dabHumidityHysteresis, systemEquip)
        val targetMinInsideHumidity = getConfigValue(modelDefinition, systemtargetMinInsideHumidty, systemEquip)
        val targetMaxInsideHumidity = getConfigValue(modelDefinition, systemtargetMaxInsideHumidty, systemEquip)
      /*  var relayStatus = 0.0
        if (currentHumidity > 0) {
            if (currentHumidity < targetMinInsideHumidity) {
                relayStatus = 1.0
            } else if (currentPortStatus > 0) {
                relayStatus =
                    if (currentHumidity > (targetMinInsideHumidity + humidityHysteresis)) 0.0 else 1.0
            }
        } else relayStatus = 0.0*/

    }

}