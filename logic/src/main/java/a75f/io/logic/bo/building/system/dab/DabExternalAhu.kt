package a75f.io.logic.bo.building.system.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.api.dcvDamperControlEnable
import a75f.io.domain.api.dehumidifierOperationEnable
import a75f.io.domain.api.dualSetpointControlEnable
import a75f.io.domain.api.humidifierOperationEnable
import a75f.io.domain.api.satSetpointControlEnable
import a75f.io.domain.api.staticPressureSetpointControlEnable
import a75f.io.domain.api.systemCoolingSATMaximum
import a75f.io.domain.api.systemCoolingSATMinimum
import a75f.io.domain.api.systemDCVDamperPosMaximum
import a75f.io.domain.api.systemDCVDamperPosMinimum
import a75f.io.domain.api.systemHeatingSATMaximum
import a75f.io.domain.api.systemHeatingSATMinimum
import a75f.io.domain.api.systemOccupancyMode
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
import a75f.io.domain.util.ModelSource
import a75f.io.logic.L
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
        L.saveCCUState()
        CCUHsApi.getInstance().syncEntityTree()
    }

    override fun doSystemControl() {

    }

    override fun addSystemEquip() {
        val hayStack = CCUHsApi.getInstance()
        val equip = hayStack.read("equip and system and not modbus")
        if (equip != null && equip.size > 0) {

            if (equip["profile"] != ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name) {
                Log.i("DEV_DEBUG", "deleteSystemEquip: ")
                hayStack.deleteEntityTree(equip["id"].toString())
            } else {

                Log.i("DEV_DEBUG", "asking to create new profile: ")
               /* val profileEquipBuilder = ProfileEquipBuilder(CCUHsApi.getInstance())
                val equipId = profileEquipBuilder.buildEquipAndPoints(
                    getConfiguration(),
                    loadModel()!!,
                    CCUHsApi.getInstance().site!!.id,
                    ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name
                )
                updateAhuRef(equipId)
                ControlMote(equipId)
                L.saveCCUState()
                CCUHsApi.getInstance().syncEntityTree()*/
            }
        }

    }

    @Synchronized
    override fun deleteSystemEquip() {
        Log.i("DEV_DEBUG", "deleteSystemEquip: ")
        val equip = CCUHsApi.getInstance().read("equip and system and not modbus")
        if (equip["profile"] == ProfileType.SYSTEM_DAB_EXTERNAL_AHU.name) {
            CCUHsApi.getInstance().deleteEntityTree(equip["id"].toString())
        }
    }

    private fun loadModel(): SeventyFiveFProfileDirective? {
        val def = ModelSource.getModelByProfileName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        if (def != null) {
           return def as SeventyFiveFProfileDirective
        }
        return null
    }

    override fun isCoolingAvailable(): Boolean {
        return false
    }

    override fun isHeatingAvailable(): Boolean {
        return false
    }

    override fun isCoolingActive(): Boolean {
        return false
    }

    override fun isHeatingActive(): Boolean {
        return false
    }

    private fun getConfigByDomainName(equip: Equip, domainName: String): Boolean {
        val config = getPointByDomain(equip,domainName)
        config?.let { return config.readDefaultVal() == 1.0 }
        return false
    }

    private fun getPointByDomain(equip: Equip, domainName: String): Point? {
        return equip.points.entries.find { (it.value.domainName.contentEquals(domainName)) }?.value
    }

    private fun getDefaultValueByDomain(equip: Equip, domainName: String): Double {
        val config = getPointByDomain(equip,domainName)
        config?.let { return config.readDefaultVal() }
        return 0.0
    }

    public fun getConfiguration(): ExternalAhuConfiguration {
        val systemEquip = Domain.getSystemEquipByDomainName(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        val config = ExternalAhuConfiguration()
        if (systemEquip == null)
            return config

        config.setPointControl.enabled = getConfigByDomainName(systemEquip, satSetpointControlEnable)
        config.dualSetPointControl.enabled = getConfigByDomainName(systemEquip, dualSetpointControlEnable)
        config.fanStaticSetPointControl.enabled = getConfigByDomainName(systemEquip, staticPressureSetpointControlEnable)
        config.dcvControl.enabled = getConfigByDomainName(systemEquip, dcvDamperControlEnable)
        config.occupancyMode.enabled = getConfigByDomainName(systemEquip, systemOccupancyMode)
        config.humidifierControl.enabled = getConfigByDomainName(systemEquip, humidifierOperationEnable)
        config.dehumidifierControl.enabled = getConfigByDomainName(systemEquip, dehumidifierOperationEnable)


        config.satMin.currentVal = getDefaultValueByDomain(systemEquip, systemSATMinimum)
        config.satMax.currentVal = getDefaultValueByDomain(systemEquip, systemSATMaximum)
        config.heatingMinSp.currentVal = getDefaultValueByDomain(systemEquip, systemHeatingSATMinimum)
        config.heatingMaxSp.currentVal = getDefaultValueByDomain(systemEquip, systemHeatingSATMaximum)
        config.coolingMinSp.currentVal = getDefaultValueByDomain(systemEquip, systemCoolingSATMinimum)
        config.coolingMaxSp.currentVal = getDefaultValueByDomain(systemEquip, systemCoolingSATMaximum)
        config.fanMinSp.currentVal = getDefaultValueByDomain(systemEquip, systemStaticPressureMinimum)
        config.fanMaxSp.currentVal = getDefaultValueByDomain(systemEquip, systemStaticPressureMaximum)
        config.dcvMin.currentVal = getDefaultValueByDomain(systemEquip, systemDCVDamperPosMinimum)
        config.dcvMax.currentVal = getDefaultValueByDomain(systemEquip, systemDCVDamperPosMaximum)
        config.targetHumidity.currentVal = getDefaultValueByDomain(systemEquip, targetHumidifier)
        config.targetDeHumidity.currentVal = getDefaultValueByDomain(systemEquip, targetDehumidifier)
        return config
    }

}