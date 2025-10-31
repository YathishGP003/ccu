package a75f.io.logic.bo.building.pcn


import a75f.io.domain.api.Domain.hayStack
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.domain.devices.PCNDevice
import a75f.io.domain.util.ModelNames
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.lowcode.LowCodeUtil.Companion.getParameters
import a75f.io.logic.bo.building.lowcode.LowCodeUtil.Companion.isAllParamsSelected
import a75f.io.logic.bo.building.modbus.buildModbusModelByEquipRef
import a75f.io.logic.connectnode.EquipModel
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import io.seventyfivef.ph.core.Tags

class PcnConfiguration(
    nodeAddress: Int,
    nodeType: String,
    priority: Int,
    roomRef: String,
    floorRef: String,
    profileType: ProfileType
) : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {

    var connectModuleList = mutableStateListOf<ConnectModule>()
    var externalEquipList = mutableStateListOf<ExternalEquip>()

    var deletedExternalEquipList = mutableListOf<Int>()
    var deletedCNList = mutableListOf<Int>()

    var pcnEquips = listOf<PCN>()

    var baudRate = mutableDoubleStateOf(0.0)
    var parity = mutableDoubleStateOf(0.0)
    var dataBits = mutableDoubleStateOf(0.0)
    var stopBits = mutableDoubleStateOf(0.0)

    var pcnDeviceMap: HashMap<Any, Any> =
        hayStack.readEntity("domainName == \"${ModelNames.pcnDevice}\" and addr == \"$nodeAddress\"")

    fun getActiveConfiguration(): PcnConfiguration {
        if (pcnDeviceMap.isEmpty()) {
            return getDefaultConfiguration()
        }
        val pcnDeviceId = pcnDeviceMap[Tags.ID].toString()

        val pcnDevice = PCNDevice(pcnDeviceMap[Tags.ID].toString())
        baudRate.doubleValue = pcnDevice.baudRate.readDefaultVal()
        parity.doubleValue = pcnDevice.parity.readDefaultVal()
        dataBits.doubleValue = pcnDevice.dataBits.readDefaultVal()
        stopBits.doubleValue = pcnDevice.stopBits.readDefaultVal()

        val pcnEquipsMap = hayStack.readAllEntities(
            "equip and pcn and deviceRef == \"$pcnDeviceId\""
        )
        pcnEquips = pcnEquipsMap.map { equip ->
            PCN(
                name = equip[Tags.DIS].toString(),
                newConfiguration = false,
                modelUpdated = false,
                equipModelList = getEquipModel(equip[Tags.ID].toString())
            )
        }

        val connectModuleDevices = hayStack.readAllEntities(
            "device and domainName == \"${ModelNames.connectNodeDevice}\" and deviceRef == \"$pcnDeviceId\""
        )
        connectModuleDevices.forEach { device ->
            val equips = hayStack.readAllEntities(
                "equip and connectModule and deviceRef == \"${device[Tags.ID].toString()}\""
            )
            val equipModelList = equips.map { equip ->
                getEquipModel(equip[Tags.ID].toString())
            }
            connectModuleList.add(
                ConnectModule(
                    device[a75f.io.api.haystack.Tags.ADDR].toString().toInt(),
                    newConfiguration = false,
                    modelUpdated = false,
                    equipModelList
                )
            )
        }

        val externalEquipsMap = hayStack.readAllEntities(
            "equip and modbus and deviceRef == \"$pcnDeviceId\""
        )
        externalEquipsMap.map { equip ->
            val equipModel = getEquipModel(equip[Tags.ID].toString())
            externalEquipList.add(
                ExternalEquip(
                    equip["group"].toString().toInt(),
                    equipModel.equipDevice.value.name,
                    equipModel,
                    newConfiguration = false,
                    modelUpdated = false
                )
            )
        }

        isDefault = false
        return this
    }
    private fun getEquipModel (equipId : String): EquipModel {
        val equipmentDevice = buildModbusModelByEquipRef(equipId)

        return EquipModel().apply {
            this.equipDevice.value = equipmentDevice
            this.parameters = getParameters(equipmentDevice)
            this.selectAllParameters.value = isAllParamsSelected(equipmentDevice)
        }
    }

    override fun getDependencies(): List<ValueConfig> {
        return listOf()
    }

    fun getDefaultConfiguration(): PcnConfiguration {
        return this
    }
}