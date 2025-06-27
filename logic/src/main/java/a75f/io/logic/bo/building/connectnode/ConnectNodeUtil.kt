package a75f.io.logic.bo.building.connectnode

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.domain.api.DomainName
import a75f.io.domain.devices.ConnectNodeDevice
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.util.readEntity
import a75f.io.logic.connectnode.ModelMetadata
import a75f.io.logic.connectnode.PointData
import a75f.io.logic.connectnode.SequenceMetaDataDTO
import a75f.io.logic.diag.otastatus.SequenceOtaStatus
import android.os.Environment
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

class ConnectNodeUtil {
    companion object {
        lateinit var pointSlaveIdRegAddress: List<Triple<String, String, String>>

        private fun getConnectNodeDevice(): HashMap<Any, Any> {
            return readEntity(ModelNames.connectNodeDevice)
        }
        fun isConnectNodeAvailable(): Boolean = getConnectNodeDevice().isNotEmpty()

        fun getPointSlaveIdRegAddressPointList(): List<Pair<String, String>> {
            if (pointSlaveIdRegAddress.isEmpty()) {
                CcuLog.e(L.TAG_CONNECT_NODE, "Point slave id and register address list is empty. Call retrievePointSlaveIdRegAddr() first.")
            }
            return pointSlaveIdRegAddress.map { Pair(it.first, it.third) }
        }

        fun isZoneContainingEmptyConnectNode(
            roomRef: String,
            ccuHsApi: CCUHsApi
        ): Boolean {
            val hasConnectNode = ccuHsApi.readEntity(
                """device and domainName == "${DomainName.connectNodeDevice}" and roomRef == "$roomRef""""
            ).isNotEmpty()

            val hasEquips = ccuHsApi.readEntity(
                """equip and roomRef == "$roomRef""""
            ).isNotEmpty()

            return hasConnectNode && !hasEquips
        }

        fun isZoneContainingConnectNodeWithEquips(
            nodeAddress: String,
            ccuHsApi: CCUHsApi
        ): Boolean {
            val hasConnectNode = ccuHsApi.readEntity(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\" and addr == \"" + nodeAddress + "\""
            ).isNotEmpty()

            val hasEquips = ccuHsApi.readEntity(
                """equip and group == "${getEquipSlaveIdByAddress(nodeAddress)}""""
            ).isNotEmpty()

            return hasConnectNode && hasEquips
        }

        fun isEmptyConnectNodeDevice(
            nodeAddress: Long,
            ccuHsApi: CCUHsApi
        ): Boolean {
            val hasConnectNode = ccuHsApi.readEntity(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\" and addr == \"" + nodeAddress + "\""
            ).isNotEmpty()

            val hasEquips = ccuHsApi.readEntity(
                """equip and group == "${getEquipSlaveIdByAddress(nodeAddress.toString())}""""
            ).isNotEmpty()

            return hasConnectNode && !hasEquips
        }

        fun getConnectNodeForZone(roomRef: String, ccuHsApi: CCUHsApi) : HashMap<Any, Any> {
            return ccuHsApi.readEntity(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\" and roomRef == \"" + roomRef + "\""
            )
        }

        fun getConnectNodeByNodeAddress(nodeAddress: String, ccuHsApi: CCUHsApi) : HashMap<Any, Any> {
            return ccuHsApi.readEntity(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\" and addr == \"" + nodeAddress + "\""
            )
        }

        fun getConnectNodeAddressBySlaveAddress(addressBand: Short, slaveAddress: Int): Int {
            return addressBand + slaveAddress
        }

        fun removeConnectNodeEquips(nodeAddress: String, hsApi: CCUHsApi) {
            val equipList = hsApi.readAllEntities("equip and group == \""+ getEquipSlaveIdByAddress(nodeAddress.takeLast(2))+"\"")
            equipList.forEach { equip ->
                hsApi.deleteEntityTree(equip["id"].toString())
                CcuLog.i(L.TAG_CONNECT_NODE,  "Deleted equip: ${equip["id"]} and dis : ${equip["dis"]} from group: ${nodeAddress.takeLast(2)}")
            }
        }

        fun isConnectNodePaired(roomRef: String): Boolean {
            return CCUHsApi.getInstance().readEntity(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\" and roomRef == \"" + roomRef + "\""
            ).size > 0
        }

        /*If connect node is paired we show it as 1000 (No module paired)
        * From this text we need to extract nodeAddress*/
        fun extractNodeAddressIfConnectPaired(nodeAddress: String): String {
            return nodeAddress.substringBefore(" ").trim()
        }

        fun getConnectNodeAddressList(
            ccuHsApi: CCUHsApi
        ): List<String> {
            val connectNodeList = ccuHsApi.readAllEntities(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\""
            )
            return connectNodeList.map { it["addr"].toString() }
        }


        /*For CN slave id will be last 2 digits
        * if address is 1001  slave id will be 1
        * and if 1000 it will be 49*/
        fun getConnectNodeSlaveIdList(
            ccuHsApi: CCUHsApi
        ): List<Int> {
            val connectNodeList = ccuHsApi.readAllEntities(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\""
            )
            return connectNodeList.mapNotNull { node ->
                node["addr"].toString().toInt().let { addr ->
                    if (addr == 1000) 49 else addr % 100
                }
            }
        }

        fun getAddressById(
            id: String,
            ccuHsApi: CCUHsApi
        ): String {
            return ccuHsApi.readMapById(id)[Tags.ADDR].toString()
        }

        fun createMetaFileForCN (
            fileName: String,
            firmwareSignature : String,
            version : Int,
            deviceType : Int,
            updateLength : Int,
        ): File {
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (file.exists()) {
                SequenceMetaDownloader().deleteDownloadedFile(
                    Globals.getInstance().applicationContext,
                    fileName
                )
            }

            val metaData = CNSequenceMetaData(
                firmwareSignature = firmwareSignature,
                sequenceName = fileName.removeSuffix(".meta"),
                version = version,
                deviceType = deviceType,
                updateLength = updateLength
            )

            val json = Gson().toJson(metaData)

            FileWriter(file).use { writer ->
                writer.write(json)
            }
            return file
        }

        fun createDummyMpyFile(fileName: String): File {
            val mpyFileName = if (fileName.endsWith(".mpy")) fileName else "$fileName.mpy"
            val file = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                mpyFileName
            )

            // Delete existing file if it exists
            if (file.exists()) {
                file.delete()
            }

            // Create file with 32 bytes of 0xFF
            FileOutputStream(file).use { fos ->
                fos.write(ByteArray(32) { 0xFF.toByte() })
            }

            return file
        }

        fun getMapOfPointNameAndRegisterAddress(points: List<PointData>): Map<String, Pair<String, String>> {
            return points.associate { point ->
                val pointName = point.pointName
                val register = point.registerAddress
                pointName to Pair(register, ((register.toInt() % 40000) + 1).toString())
            }
        }

        private fun getConnectSlaveAddress(slaveId: Int): Int {
            val currentBand = L.ccu().addressBand
            val connectNodeAddress =
                getConnectNodeAddressBySlaveAddress(currentBand, slaveId)
            return connectNodeAddress
        }

        fun connectNodeEquip(slaveId: Int): ConnectNodeDevice {
            // Step 1: Get the node address from the modbus response
            var connectNodeAddress:Int = 0

            // If the slaveId is less than 100, we need to get full address of the paired connect node
            // Else we have received the full address
            connectNodeAddress = if(slaveId < 100) {
                getConnectSlaveAddress(slaveId) // Eg: 01
            } else {
                slaveId // Eg: 7501
            }

            // Step 2: Get the connect node device reference
            val deviceRef = getConnectNodeByNodeAddress(
                connectNodeAddress.toString(),
                CCUHsApi.getInstance()
            ).get("id")
            val connectNodeEquip = ConnectNodeDevice(deviceRef.toString())
            return connectNodeEquip
        }

        fun updateOtaSequenceStatus(status: SequenceOtaStatus, slaveId: Int) {
            // Step 1: Get the connect node device reference based on slave address
            val connectNodeEquip = connectNodeEquip(slaveId)
            connectNodeEquip.otaStatusSequence.writeHisVal(status.ordinal.toDouble())
        }

        fun updateOtaSequenceState(state: Short, slaveId: Int) {
            val connectNodeEquip = connectNodeEquip(slaveId)
            connectNodeEquip.sequenceUpdateState.writeHisVal(state.toDouble())
        }

        fun getAddressByDeviceId(
            deviceId: String,
            ccuHsApi: CCUHsApi
        ): String {
            return ccuHsApi.readMapById(deviceId)[Tags.ADDR].toString()
        }

        /**
         * Retrieves and organizes point information (ID, group, register number) for a specific device.
         *
         * This function:
         * 1. Looks up the slave ID for the given device
         * 2. Queries all entities belonging to that slave ID group
         * 3. Filters and transforms valid entities into Triples
         * 4. Sorts the results by register number (numeric sort)
         *
         * @param deviceId The unique identifier of the target device
         * @return List of Triples containing (point ID, group, registerNumber) sorted by register number,
         *         or empty list if no valid points found
         *
         * Each Triple contains:
         * - First: Point ID (String)
         * - Second: Group/Slave ID (String)
         * - Third: Register number (String, but guaranteed numeric from filtering)
         */
        fun retrievePointSlaveIdRegAddr(deviceId: String) : List<Triple<String, String, String>>  {
            val slaveId = getSlaveIdByDeviceId(deviceId, CCUHsApi.getInstance())

            pointSlaveIdRegAddress =  CCUHsApi.getInstance().readAllEntities("group == \"$slaveId\"")
                .mapNotNull { entity ->
                    val id = entity[Tags.ID]?.toString()
                    val group = entity["group"]?.toString()
                    val registerNumber = entity["registerNumber"]?.toString()
                    if (registerNumber?.toIntOrNull() != null && id != null && group != null) {
                        Triple(id, group, registerNumber)
                    } else null
                }
                .sortedBy { it.third.toInt() }
            return pointSlaveIdRegAddress
        }

        fun getSlaveIdByDeviceId(
            deviceId: String,
            ccuHsApi: CCUHsApi
        ): Int {
            val address = ccuHsApi.readMapById(deviceId)[Tags.ADDR].toString().takeLast(2)
            return getEquipSlaveIdByAddress(address)
        }

        // In meta data, modelId can have a suffix like "model_1", "model_2", etc, filtering it out
        fun normaliseSeqMetaData(sequenceMetaData: SequenceMetaDataDTO): List<ModelMetadata> {
            val updatedList = mutableListOf<ModelMetadata>()
            val metaList = sequenceMetaData.metadata
            metaList.forEach { modelId ->
                if (modelId.modelId.contains("_")) {
                    val baseId = modelId.modelId.substringBefore("_")
                    val suffix = modelId.modelId.substringAfterLast("_")
                    updatedList.add(modelId.copy(modelId = baseId, suffix = suffix.prependIndent("_")))
                } else {
                    updatedList.add(modelId)
                }
            }

            return updatedList
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

        fun getEquipSlaveIdByAddress(
            address: String,
        ): Int {
            return address.takeLast(2).toInt()
        }

        fun getZoneNameByConnectNodeAddress(address: String, ccuHsApi: CCUHsApi): String {
            val connectNode = ccuHsApi.readEntity(
                "device and domainName == \"" + DomainName.connectNodeDevice + "\" and addr == \"$address\""
            )
            return ccuHsApi.readMapById(connectNode["roomRef"].toString())[Tags.DIS].toString()
        }
    }
}