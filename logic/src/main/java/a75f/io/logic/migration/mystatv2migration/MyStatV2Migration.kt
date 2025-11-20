package a75f.io.logic.migration.mystatv2migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuConfiguration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2Configuration
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe2RelayMapping
import a75f.io.logic.bo.building.statprofiles.util.MyStatDeviceType
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatModelByEquipRef
import a75f.io.logic.util.PreferenceUtil
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Author: Manjunath Kundaragi
 * Created on: 18-09-2025
 */

// This migration is released as part of 4.4.0
// This migration can be remove after 4.5.0
class MyStatV2Migration {
    private val associationCache = mutableMapOf<String, Pair<HashMap<Any, Any>, AssociationData>>()
    private val hsApi = CCUHsApi.getInstance()

    companion object {
        const val MYSTAT_V2_MIGRATION = "MYSTAT_V2_MIGRATION_2.0"
        lateinit var migrationVersion:String
        val cpuDynamicValues = listOf(
            DomainName.analog1MinCooling,
            DomainName.analog1MinHeating,
            DomainName.analog1MaxCooling,
            DomainName.analog1MaxHeating,
            DomainName.analog1FanLow,
            DomainName.analog1FanHigh,
            DomainName.analog1FanRecirculate,
            DomainName.analog1MaxDCVDamper,
            DomainName.analog1MaxLinearFanSpeed,
            DomainName.analog1MinDCVDamper,
            DomainName.analog1FanRecirculate,
            DomainName.analog1MinLinearFanSpeed
        )

        val pipe2DynamicValues = listOf(
            DomainName.analog1MinFanSpeed,
            DomainName.analog1MaxFanSpeed,
            DomainName.analog1FanLow,
            DomainName.analog1FanHigh,
            DomainName.analog1MaxDCVDamper,
            DomainName.analog1MinDCVDamper,
            DomainName.analog1MaxChilledWaterValve,
            DomainName.analog1MinChilledWaterValve,
            DomainName.analog1MaxHotWaterValve,
            DomainName.analog1MinHotWaterValve,
        )
        val hpuDynamicValues = listOf(
            DomainName.analog1MinCompressorSpeed,
            DomainName.analog1MaxCompressorSpeed,
            DomainName.analog1MinDCVDamper,
            DomainName.analog1MaxDCVDamper,
            DomainName.analog1FanLow,
            DomainName.analog1FanHigh,
            DomainName.analog1MinFanSpeed,
            DomainName.analog1MaxFanSpeed
        )
    }

    fun cachePreModelMigrationData() {
        if (PreferenceUtil.getMyStatV2Migration()) return
        val equips = hsApi.readAllEntities("equip and mystat")
        if (equips.isEmpty()) {
            CcuLog.d(MYSTAT_V2_MIGRATION, "No MyStat V2 devices found for migration")
            return
        }
        CcuLog.d(
            MYSTAT_V2_MIGRATION,
            "Caching pre model migration data for ${equips.size} MyStat devices"
        )

        fun getDynamicValues(equipId: String): HashMap<String, Double> {

            val config = getMyStatConfiguration(equipId)
            val list = when (config) {
                is MyStatCpuConfiguration -> cpuDynamicValues
                is MyStatHpuConfiguration -> hpuDynamicValues
                is MyStatPipe2Configuration -> pipe2DynamicValues
                else -> emptyList()
            }
            val dynamicValues = hashMapOf<String, Double>()
            list.forEach { domainName ->
                val pointValue = Point(domainName, equipId)
                if (pointValue.pointExists()) {
                    dynamicValues[domainName] = pointValue.readDefaultVal()
                }
            }
            return dynamicValues
        }
        equips.forEach { equip ->
            val equipId = equip[Tags.ID].toString()
            val deviceMap = CCUHsApi.getInstance().readEntity("domainName and device and equipRef == \"$equipId\"")
            val device = MyStatDevice((deviceMap["id"].toString()))
            val relay4Enabled = Point(DomainName.relay4OutputEnable, equipId)
            val relay4Association = Point(DomainName.relay4OutputAssociation, equipId)

           // it is already mystat v2 device then ignore the migration
            if (relay4Enabled.pointExists().not() && relay4Association.pointExists().not()) {
                CcuLog.d(
                    MYSTAT_V2_MIGRATION,
                    "migration version : $migrationVersion  Skipping migration for equipId: $equipId as it is already MyStat V2 device"
                )
                //if the version > 4.4.2 , we have already in correct enum value not no need to update the version
                if(migrationVersion > "4.4.2") return@forEach

                if (device.mystatDeviceVersion.readPointValue() > 1) {
                    device.mystatDeviceVersion.writePointValue(1.0)
                } else {
                    device.mystatDeviceVersion.writePointValue(0.0)
                }
                return@forEach
            }
            device.mystatDeviceVersion.writePointValue(0.0)
            val analogOut1Enabled = Point(DomainName.analog1OutputEnable, equipId).readDefaultVal() > 0
            val analogOut1Association = Point(DomainName.analog1OutputAssociation, equipId).readDefaultVal()
            associationCache[equipId] = Pair(
                equip, AssociationData(
                    relay4Enabled.readDefaultVal() > 0, relay4Association.readDefaultVal(),
                    analogOut1Enabled, analogOut1Association, getDynamicValues(equipId)
                )
            )
        }
        CcuLog.d(
            MYSTAT_V2_MIGRATION, "associationCache: $associationCache "
        )
    }

    fun migratePostModelMigrationData() {
        if (PreferenceUtil.getMyStatV2Migration()) return

        fun updateConfig(config: MyStatConfiguration, data: AssociationData, prefixCount: Int) {
            config.apply {
                universalOut2.enabled = data.relay4Enabled
                universalOut2Association.associationVal = data.relay4Association.toInt()
                universalOut1.enabled = data.analogOut1Enabled
                universalOut1Association.associationVal =
                    prefixCount + data.analogOut1Association.toInt()
                getValueConfigs().forEach { valueConfig ->
                    if (data.dynamicChangedValues.containsKey(valueConfig.domainName)) {
                        valueConfig.currentVal = data.dynamicChangedValues[valueConfig.domainName]!!
                    }
                }
            }
        }

        val equipBuilder = ProfileEquipBuilder(hsApi)
        val deviceModel = ModelLoader.getMyStatDeviceModel() as SeventyFiveFDeviceDirective

        associationCache.forEach { (equipId, data) ->
            val config = getMyStatConfiguration(equipId)
            val model = getMyStatModelByEquipRef(equipId)
            val entityMapper = EntityMapper(model as SeventyFiveFProfileDirective)
            val deviceBuilder = DeviceBuilder(hsApi, entityMapper)
            val deviceDis = "${hsApi.siteName}-${deviceModel.name}-${config!!.nodeAddress}"

            when (config) {
                is MyStatCpuConfiguration -> {
                    updateConfig(config, data.second, MyStatCpuRelayMapping.values().size)
                }

                is MyStatHpuConfiguration -> {
                    updateConfig(config, data.second, MyStatHpuRelayMapping.values().size)
                }

                is MyStatPipe2Configuration -> {
                    updateConfig(config, data.second, MyStatPipe2RelayMapping.values().size)
                }
            }
            val equipRef = equipBuilder.updateEquipAndPoints(
                config,
                model,
                hsApi.getSite()!!.id,
                data.first["dis"].toString(),
                true
            )
            val deviceRef = deviceBuilder.updateDeviceAndPoints(
                config,
                deviceModel,
                equipId,
                hsApi.site!!.id,
                deviceDis
            )
            config.universalInUnit(deviceRef)
            config.apply { setPortConfiguration(nodeAddress, getRelayMap(), getAnalogMap()) }
            when (config) {
                is MyStatCpuConfiguration -> config.updateEnumConfigs(
                    MyStatCpuEquip(equipRef),
                    MyStatDeviceType.MYSTAT_V1.name
                )

                is MyStatHpuConfiguration -> config.updateEnumConfigs(
                    MyStatHpuEquip(equipRef),
                    MyStatDeviceType.MYSTAT_V1.name
                )

                is MyStatPipe2Configuration -> config.updateEnumConfigs(
                    MyStatPipe2Equip(equipRef),
                    MyStatDeviceType.MYSTAT_V1.name
                )
            }
            MyStatDevice(deviceRef).mystatDeviceVersion.writePointValue(0.0)
        }
        CcuLog.d(
            MYSTAT_V2_MIGRATION,
            "migration is successfully completed"
        )
        PreferenceUtil.setMyStatV2Migration()
    }
}

data class AssociationData(
    val relay4Enabled: Boolean,
    val relay4Association: Double,
    val analogOut1Enabled: Boolean,
    val analogOut1Association: Double,
    val dynamicChangedValues: HashMap<String, Double>
)