package a75f.io.logic.bo.building.pcn

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.domain.api.DomainName
import a75f.io.domain.devices.ConnectNodeDevice
import a75f.io.domain.devices.PCNDevice
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.diag.otastatus.SequenceOtaStatus
import io.seventyfivef.ph.core.Tags

class PCNUtil {
    companion object {

        const val SERVER_ID = "Server Id:"
        const val BAUD_RATE = "BaudRate:"
        const val PARITY = "Parity:"
        const val DATA_BITS = "DataBits:"
        const val STOP_BITS = "StopBits:"

        const val MODBUS_SLAVE_ID_LIMIT = 247

        private lateinit var pointSlaveIdRegAddress: List<Triple<String, String, String>>

        fun isZoneContainingEmptyPCN(
            roomRef: String,
            ccuHsApi: CCUHsApi
        ): Boolean {
            val hasConnectNode = ccuHsApi.readEntity(
                """device and domainName == "${ModelNames.pcnDevice}" and roomRef == "$roomRef""""
            ).isNotEmpty()

            val hasEquips = ccuHsApi.readEntity(
                """equip and roomRef == "$roomRef""""
            ).isNotEmpty()

            return hasConnectNode && !hasEquips
        }

        fun isZoneContainingPCNWithEquips(
            nodeAddress: String,
            ccuHsApi: CCUHsApi
        ): Boolean {
            val roomRef = getPcnByNodeAddress(nodeAddress, ccuHsApi)["roomRef"].toString()

            val hasConnectNode = ccuHsApi.readEntity(
                "device and domainName == \"" + ModelNames.pcnDevice + "\" and addr == \"" + nodeAddress + "\""
            ).isNotEmpty()

            val hasEquips = ccuHsApi.readEntity(
                """equip and roomRef == "$roomRef""""
            ).isNotEmpty()

            return hasConnectNode && hasEquips
        }

        fun getPCNForZone(roomRef: String, ccuHsApi: CCUHsApi): HashMap<Any, Any> {
            return ccuHsApi.readEntity(
                "device and domainName == \"" + ModelNames.pcnDevice + "\" and roomRef == \"" + roomRef + "\""
            )
        }

        fun getZoneNameByPCNAddress(address: String, ccuHsApi: CCUHsApi): String {
            val pcn = ccuHsApi.readEntity(
                "device and domainName == \"" + ModelNames.pcnDevice + "\" and addr == \"$address\""
            )
            return ccuHsApi.readMapById(pcn["roomRef"].toString())[a75f.io.api.haystack.Tags.DIS].toString()
        }

        fun getPcnByNodeAddress(nodeAddress: String, ccuHsApi: CCUHsApi) : HashMap<Any, Any> {
            return ccuHsApi.readEntity(
                "device and domainName == \"" + ModelNames.pcnDevice + "\" and addr == \"" + nodeAddress + "\""
            )
        }

        fun getCNServerId(pairedId: List<Int>): Int {
            var serverId = 1
            while (pairedId.contains(serverId)) {
                serverId++
            }
            return serverId

        }

        fun getUsedIndices(usedIndices: List<Int>): List<Int> {
            val indices = mutableListOf<Int>()
            for (i in 0..MODBUS_SLAVE_ID_LIMIT) {
                if (usedIndices.contains(i)) {
                    indices.add(i)
                }
            }
            return indices
        }


        fun deleteConnectModuleTree(
            haystack: CCUHsApi,
            serverId: Int,
            pcnId: String
        ) {
            val connectNodeDevice = haystack.readEntity(
                "device and domainName == \"${ModelNames.connectNodeDevice}\" and addr == \"$serverId\" and deviceRef == \"$pcnId\""
            )
            if (connectNodeDevice.isNotEmpty()) {
                haystack.deleteEntityTree(connectNodeDevice[Tags.ID].toString())
                CcuLog.i(L.TAG_PCN,  "Deleted DEVICE dis : ${connectNodeDevice["dis"]} from group: $serverId")
            }

            val equipList = haystack.readAllEntities("equip and group == \""+ serverId +"\" and deviceRef == \"${connectNodeDevice[Tags.ID].toString()}\"")
            equipList.forEach { equip ->
                haystack.deleteEntityTree(equip["id"].toString())
                CcuLog.i(L.TAG_PCN,  "Deleted equip: and dis : ${equip["dis"]} from group: $serverId")
            }
        }

        fun removePcnDeviceAndEquips(nodeAddress: String, ccuHsApi: CCUHsApi) {
            val pcnDevice = ccuHsApi.readEntity(
                "device and domainName == \"" + ModelNames.pcnDevice + "\" and addr == \"" + nodeAddress + "\""
            )
            if (pcnDevice.isEmpty()) {
                CcuLog.w(L.TAG_PCN, "PCN device not found for node address: $nodeAddress")
                return
            }

            val roomRef = pcnDevice[Tags.ROOM_REF].toString()
            val equips = ccuHsApi.readAllEntities(
                "equip and roomRef == \"$roomRef\""
            )

            val devices = ccuHsApi.readAllEntities(
                "device and roomRef == \"$roomRef\""
            )
            equips.forEach { equip ->
                ccuHsApi.deleteEntityTree(equip[Tags.ID].toString())
                CcuLog.i(L.TAG_PCN, "Deleted equip: ${equip[Tags.DIS]} from room: $roomRef")
            }

            devices.forEach { device ->
                ccuHsApi.deleteEntityTree(device[Tags.ID].toString())
                CcuLog.i(L.TAG_PCN, "Deleted device: ${device[Tags.DIS]} from room: $roomRef")
            }
        }

        fun removePCNEquipTree(device: String, hayStack: CCUHsApi) {
            val equipList = hayStack.readAllEntities("equip and pcn and deviceRef == \"$device\"")
            equipList.forEach { equip ->
                hayStack.deleteEntityTree(equip["id"].toString())
                CcuLog.i(L.TAG_PCN,  "Deleted equip: and dis : ${equip["dis"]} from pcn device: $device")
            }
        }

        fun removeCNEquipTree(device: String, hayStack: CCUHsApi) {
            val equipList = hayStack.readAllEntities("equip and connectModule and deviceRef == \"$device\"")
            equipList.forEach { equip ->
                hayStack.deleteEntityTree(equip["id"].toString())
                CcuLog.i(L.TAG_PCN,  "Deleted PCN's CN equip: and dis : ${equip["dis"]} from device: $device")
            }
        }

        fun isConnectNodeInPCN(deviceMap: HashMap<Any, Any>): Boolean {
            return deviceMap[a75f.io.api.haystack.Tags.ADDR].toString().toInt() <= MODBUS_SLAVE_ID_LIMIT
        }

        fun deleteExternalEquipTree(hayStack: CCUHsApi, serverId: Int, pcnId: String) {
            val externalEquip = hayStack.readEntity(
                "equip and modbus and group == \"$serverId\" and deviceRef == \"$pcnId\""
            )
            val externalDevice = hayStack.readEntity(
                "device and equipRef == \"${externalEquip[Tags.ID].toString()}\" and addr == \"$serverId\""
            )
            if (externalEquip.isNotEmpty()) {
                hayStack.deleteEntityTree(externalEquip[Tags.ID].toString())
                CcuLog.i(L.TAG_PCN, "Deleted external equip dis: ${externalEquip[Tags.DIS]} from group: $serverId")
            } else {
                CcuLog.w(L.TAG_PCN, "External equip not found for group: $serverId")
            }

            if (externalDevice.isNotEmpty()) {
                hayStack.deleteEntityTree(externalDevice[Tags.ID].toString())
                CcuLog.i(L.TAG_PCN, "Deleted external device dis: ${externalDevice[Tags.DIS]} from group: $serverId")
            } else {
                CcuLog.w(L.TAG_PCN, "External device not found for group: $serverId")
            }
        }

        fun reorderEquipments(equipmentDeviceList: List<EquipmentDevice>): List<EquipmentDevice> {
            equipmentDeviceList.forEach { device ->
                val suffix = device.name.substringAfterLast("_")
                val keepSuffix = if (suffix.toIntOrNull() != null) "_$suffix" else ""

                if (keepSuffix.isNotEmpty()) {
                    device.registers.forEach { register ->
                        if (register.parameters.isNotEmpty()) {
                            register.parameters[0].name += keepSuffix
                        }
                    }
                }
            }

            return equipmentDeviceList.sortedBy { device ->
                device.registers.minOfOrNull { it.registerNumber.toInt() } ?: Int.MAX_VALUE
            }
        }

        fun getPCNConfiguration(deviceId: String): PcnConfiguration {
            val deviceMap = CCUHsApi.getInstance().readMapById(deviceId)
            return PcnConfiguration(
                nodeAddress = deviceMap["addr"].toString().toInt(),
                nodeType = NodeType.PCN.name,
                priority = 0,
                roomRef = deviceMap[Tags.ROOM_REF].toString(),
                floorRef = deviceMap[Tags.FLOOR_REF].toString(),
                profileType = ProfileType.PCN,
            ).getActiveConfiguration()
        }

        fun getPcnNodeByNodeAddress(nodeAddress: String, ccuHsApi: CCUHsApi) : HashMap<Any, Any> {
            return ccuHsApi.readEntity("device and pcn and addr == \"" + nodeAddress + "\"")
        }

        fun getConnectNodeBySlaveAddress(pcnDeviceId: String, nodeAddress: String, ccuHsApi: CCUHsApi) : HashMap<Any, Any> {
            return ccuHsApi.readEntity(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\" and addr == \"" + nodeAddress + "\"" +
                        " and deviceRef == \"" + pcnDeviceId + "\""
            )
        }

        /**
         * Retrieves and builds a `ConnectNodeDevice` instance for a given PCN device
         * and slave ID by resolving the corresponding connect node equipment reference.
         *
         * Steps performed:
         * 1. Look up the connect node entity by calling `getConnectNodeBySlaveAddress`,
         *    passing the parent PCN device ID, the target slave ID, and `CCUHsApi`.
         * 2. Extract the device reference (`id`) from the returned entity.
         * 3. Construct a new `ConnectNodeDevice` using the resolved device reference.
         * 4. Return the new `ConnectNodeDevice` instance.
         *
         * @param pcnDevice The PCN device represented as a key–value `HashMap`, containing at least an `"id"`.
         * @param slaveId   The Modbus slave ID identifying the connect node device attached to the PCN.
         * @return A `ConnectNodeDevice` associated with the provided PCN and slave ID.
         */
        fun getConnectNodeEquip(cnDevice: HashMap<Any, Any>, slaveId: Int): ConnectNodeDevice {
            val deviceRef = getConnectNodeBySlaveAddress(
                cnDevice.get("id").toString(),
                slaveId.toString(),
                CCUHsApi.getInstance()
            ).get("id")
            val cnEquip = ConnectNodeDevice(deviceRef.toString())
            return cnEquip
        }

        fun getPcnNodeDevice(pcnNodeAddress: Int): PCNDevice {
            val deviceRef = getPcnNodeByNodeAddress(
                pcnNodeAddress.toString(),
                CCUHsApi.getInstance()
            ).get("id")
            val pcnEquip = PCNDevice(deviceRef.toString())
            return pcnEquip
        }

        /* Update the OTA sequence status for a given node address.
         * If the node address is less than or equal to 100, it is treated as a Connect Node device.
         * Otherwise, it is treated as a PCN device.
         *
         * @param status The new OTA sequence status to set.
         * @param nodeAddress The node address of the device to update.
         */
        fun updateOtaSequenceStatus(status: SequenceOtaStatus, nodeAddress: Int) {
            if(nodeAddress <= 100) {
                // Not a PCN device, its a connect node device
                val connectNodeDevice = CCUHsApi.getInstance().readEntity(
                    "device and connectModule and domainName == \"" + ModelNames.connectNodeDevice + "\" and addr == \"" + nodeAddress + "\""
                )
                if (connectNodeDevice.isNotEmpty()) {
                    val deviceRef = connectNodeDevice.get(Tags.ID)
                    val connectNodeEquip = ConnectNodeDevice(deviceRef.toString())
                    connectNodeEquip.otaStatusSequence.writeHisVal(status.ordinal.toDouble())
                } else {
                    CcuLog.w(L.TAG_PCN, "Connect Node device not found for node address: $nodeAddress. Cannot update OTA sequence status.")
                }
            }
            else {
                // It's a PCN device
                val pcnDevice = CCUHsApi.getInstance().readEntity(
                    "device and pcn and domainName == \"" + ModelNames.pcnDevice + "\" and addr == \"" + nodeAddress + "\""
                )
                if (pcnDevice.isNotEmpty()) {
                    val deviceRef = pcnDevice.get(Tags.ID)
                    val pcnEquip = PCNDevice(deviceRef.toString())
                    pcnEquip.otaStatusSequence.writeHisVal(status.ordinal.toDouble())
                } else {
                    CcuLog.w(L.TAG_PCN, "PCN device not found for node address: $nodeAddress. Cannot update OTA sequence status.")
                }
            }

        }
    }
}