package a75f.io.logic.bo.building.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.DefaultSystemEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.DomainManager.addSystemDomainEquip
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader.getCMDeviceModel
import a75f.io.domain.util.ModelLoader.getDefaultSystemProfileModel
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.autocommission.AutoCommissioningUtil
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Created by samjithsadasivan on 1/8/19.
 */

/**
 * Default System Profile does nothing.
 *
 */
class DefaultSystem : SystemProfile() {
    override fun getProfileName(): String {
        return "Default"
    }

    override fun getProfileType(): ProfileType {
        return ProfileType.SYSTEM_DEFAULT
    }


    override fun doSystemControl() {
        val defaultSystemEquip = DefaultSystemEquip(systemEquipRef)
        updateOutsideWeatherParams()
        //update default points for apps and portals to consume
        val systemStatus = statusMessage
        val scheduleStatus = "No Central equipment is connected."
        if (defaultSystemEquip.equipStatusMessage.readDefaultStrVal() != systemStatus) {
            defaultSystemEquip.equipStatusMessage.writeDefaultVal(systemStatus)
        }

        if (defaultSystemEquip.equipScheduleStatus.readDefaultStrVal() != scheduleStatus) {
            defaultSystemEquip.equipScheduleStatus.writeDefaultVal(scheduleStatus)
            if (AutoCommissioningUtil.isAutoCommissioningStarted()) {
                CcuLog.i(
                    L.TAG_CCU_SCHEDULER,
                    "System page status(when no central device is available) - AutoCommissioning is Started"
                )
                defaultSystemEquip.equipScheduleStatus.writeDefaultVal("In Diagnostic Mode")
            }
        }
    }

    fun createDefaultSystemEquip(): DefaultSystem {
        val hayStack = CCUHsApi.getInstance()
        val equip = hayStack.readEntity("equip and system and not modbus and not connectModule")
        if (equip != null && equip.isNotEmpty()) {
            if (equip["profile"] != DomainName.defaultSystemEquip) {
                hayStack.deleteEntityTree(equip["id"].toString())
                deleteOAODamperEquip()
                deleteBypassDamperEquip()
                removeSystemEquipModbus()
                deleteSystemConnectModule()
                removeSystemEquipBacnet()
            } else {
                return this
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            val defaultSystemModel = getDefaultSystemProfileModel() as SeventyFiveFProfileDirective
            val deviceModel = getCMDeviceModel() as SeventyFiveFDeviceDirective
            val entityMapper = EntityMapper(defaultSystemModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            val equipBuilder = ProfileEquipBuilder(hayStack)
            val profileConfiguration = DefaultSystemConfig(defaultSystemModel)

            val equipDis = hayStack.siteName + "-" + defaultSystemModel.name
            val equipId = equipBuilder.buildEquipAndPoints(
                profileConfiguration,
                defaultSystemModel,
                hayStack.site!!.id,
                equipDis
            )
            val cmDevice = hayStack.readEntity("cm and device")
            if (cmDevice.isNotEmpty()) {
                hayStack.deleteEntityTree(cmDevice["id"].toString())
            }

            val deviceDis = hayStack.siteName + "-" + deviceModel.name
            deviceBuilder.buildDeviceAndPoints(
                profileConfiguration,
                deviceModel,
                equipId,
                hayStack.site!!.id,
                deviceDis, null
            )

            CcuLog.d(L.TAG_CCU_SYSTEM, "Add Default System Equip")
            updateAhuRef(equipId)

            addSystemDomainEquip(hayStack)
            DomainManager.addCmBoardDevice(hayStack)
            L.saveCCUState()

            DesiredTempDisplayMode.setSystemModeForDefaultSystemProfile(CCUHsApi.getInstance())
        }
        return this
    }    override fun addSystemEquip() {
        CcuLog.d(L.TAG_CCU_SYSTEM, "Updated temp modes for default system")
        DesiredTempDisplayMode.setSystemModeForDefaultSystemProfile(CCUHsApi.getInstance())
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

    override fun deleteSystemEquip() {
        val equip =
            CCUHsApi.getInstance().readEntity("system and equip and not modbus and not connectModule")
        if (equip["profile"] == DomainName.defaultSystemEquip) {
            CCUHsApi.getInstance().deleteEntityTree(equip["id"].toString())
        }
        deleteOAODamperEquip()
        deleteBypassDamperEquip()
        removeSystemEquipModbus()
        deleteSystemConnectModule()
        removeSystemEquipBacnet()
    }

    override fun getStatusMessage(): String {
        return "System is in gateway mode."
    }
}
