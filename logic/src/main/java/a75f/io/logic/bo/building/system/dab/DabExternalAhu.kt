package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.api.dcvDamperControlEnable
import a75f.io.domain.api.dehumidifierOperationEnable
import a75f.io.domain.api.dualSetpointControlEnable
import a75f.io.domain.api.humidifierOperationEnable
import a75f.io.domain.api.occupancyModeControl
import a75f.io.domain.api.satSetpointControlEnable
import a75f.io.domain.api.staticPressureSetpointControlEnable
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
import a75f.io.domain.api.targetDehumidifier
import a75f.io.domain.api.targetHumidifier
import a75f.io.domain.config.ExternalAhuConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.haystack.device.ControlMote
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

        Log.i("DEV_DEBUG", "Cooling Loop : $coolingLoopOp")
        Log.i("DEV_DEBUG", "Heating Loop : $heatingLoopOp")

        /*
        updateOutsideWeatherParams()
        updateMechanicalConditioning(CCUHsApi.getInstance())
        val dabSystem = DabSystemController.getInstance()
        if (isDcwbEnabled()) {
            if (dcwbAlgoHandler == null) {
                val isAdaptiveDelta: Boolean = getConfigVal("adaptive and delta") > 0
                dcwbAlgoHandler =
                    DcwbAlgoHandler(isAdaptiveDelta, systemEquipRef, CCUHsApi.getInstance())
            }

            //Analog1 controls water valve when the DCWB enabled.
            updateAnalog1DcwbOutput(dabSystem)

            //Could be mapped to cooling or co2 based on configuration.
            updateAnalog4Output(dabSystem)
        } else {
            //Analog1 controls cooling when the DCWB is disabled
            updateAnalog1DabOutput(dabSystem)
        }

        //Analog2 controls Central Fan
        updateAnalog2Output(dabSystem)

        //Analog3 controls heating.
        updateAnalog3Output(dabSystem)
        updateRelayOutputs(dabSystem)
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
        }*/
    }

    override fun addSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val equip = hayStack.read("equip and system and not modbus")
        if (equip != null && equip.size > 0) {
            if (equip["profile"] != ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name) {
                hayStack.deleteEntityTree(equip["id"].toString())
            }
        }
    }

    @Synchronized
    override fun deleteSystemEquip() {
        val equip = CCUHsApi.getInstance().read("equip and system and not modbus")
        if (equip["profile"] == ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name) {
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

}