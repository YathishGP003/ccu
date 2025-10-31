package a75f.io.logic.bo.building.lowcode

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.domain.devices.PCNDevice
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil.Companion.removeConnectNodeEquips
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.lowcode.LowCodeUtil.Companion.getEquipWithRegisterNumber
import a75f.io.logic.bo.building.lowcode.LowCodeUtil.Companion.getParameters
import a75f.io.logic.bo.building.lowcode.LowCodeUtil.Companion.isSequenceModified
import a75f.io.logic.bo.building.lowcode.LowCodeUtil.Companion.parseModbusDataFromString
import a75f.io.logic.bo.building.modbus.ModbusEquip
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.isConnectNodeInPCN
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.removeCNEquipTree
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.removePCNEquipTree
import a75f.io.logic.bo.building.pcn.PCNValidation
import a75f.io.logic.bo.building.pcn.PCNViewState
import a75f.io.logic.bo.building.pcn.PCNViewStateUtil
import a75f.io.logic.bo.building.pcn.PcnConfiguration
import a75f.io.logic.connectnode.EquipModel
import a75f.io.logic.connectnode.ModelMetadata
import a75f.io.logic.connectnode.PointData
import a75f.io.logic.connectnode.SequenceMetaDataDTO
import a75f.io.logic.diag.otastatus.SequenceOtaStatus
import a75f.io.logic.preconfig.ModbusEquipCreationException
import com.google.gson.JsonParseException
import io.seventyfivef.ph.core.Tags
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class LowCodeEntitiesBuilder {

    fun createLowCodeSequence(
        sequenceMetaData: SequenceMetaDataDTO,
        addedDeviceList: List<String>,
        hayStack: CCUHsApi,
        domainService: DomainService,
    ): List<Exception> {
        val errors = mutableListOf<Exception>()

        addedDeviceList.forEach { deviceId ->
            val deviceMap = CCUHsApi.getInstance().readMapById(deviceId)
            val isPCN = deviceMap["domainName"].toString() == ModelNames.pcnDevice
            val isCNInPcn = isConnectNodeInPCN(deviceMap)
            val pcnDeviceMap = if (isPCN) {
                deviceMap
            } else {
                hayStack.readMapById(deviceMap["deviceRef"].toString())
            }
            val pointCount = sequenceMetaData.metadata.flatMap { it.points }.size * 2 // Each point typically requires two registers
            if (isPCN || isCNInPcn) {
                val pcnConfiguration = PcnConfiguration(
                    nodeAddress = pcnDeviceMap["addr"].toString().toInt(),
                    nodeType = NodeType.PCN.name,
                    priority = 0,
                    roomRef = deviceMap[Tags.ROOM_REF].toString(),
                    floorRef = deviceMap[Tags.FLOOR_REF].toString(),
                    profileType = ProfileType.PCN,
                ).getActiveConfiguration()
                val pcnViewState = PCNViewStateUtil.configToState(pcnConfiguration, PCNViewState())
                val pairedRegisterCount = PCNValidation.getPairedRegisterCount(pcnViewState)
                if(pointCount + pairedRegisterCount > PCNValidation.MAX_REGISTER_COUNT) {
                    val pcnDevice = PCNDevice(deviceId.prependIndent("@"))
                    pcnDevice.otaStatusSequence.writeHisVal(SequenceOtaStatus.MAX_REGISTER_COUNT_REACHED.ordinal.toDouble())

                    CcuLog.e(
                        L.TAG_CCU_SEQUENCE_APPLY,
                        "PCN Device: $deviceId has reached maximum register count," +
                                " cannot add more equips. new point count: $pointCount," +
                                " Paired register count: $pairedRegisterCount"
                    )
                    return@forEach
                }
            }

            val equipWithRegNumber = getEquipWithRegisterNumber(hayStack, deviceId)

            sequenceMetaData.metadata.forEach { modelMetadata ->
                try {
                    val modelPointAddress = modelMetadata.points.firstOrNull()?.registerAddress
                    if (modelPointAddress != null && modelPointAddress in equipWithRegNumber.values.flatten()) {
                        deleteLowCodeSequence(listOf(deviceId), hayStack)
                        CcuLog.i(
                            L.TAG_CCU_SEQUENCE_APPLY,
                            "Deleted existing equip for deviceId: $deviceId with model: ${modelMetadata.modelId}"
                        )
                    }
                    // wait until model is created or exception is thrown
                    readAndCreateEquip(
                        modelMetadata,
                        deviceId,
                        domainService,
                        hayStack,
                        isCreate = true
                    )

                } catch (e: Exception) {
                    CcuLog.e(
                        L.TAG_CCU_SEQUENCE_APPLY,
                        "Error while creating sequence for device: $deviceId, model: ${modelMetadata.modelId}, reason: ${e.message}"
                    )
                    errors.add(e)
                }
            }
        }
        return errors
    }

    fun updateExistingSequence(
        sequenceMetaData: SequenceMetaDataDTO,
        reconfiguredDeviceList: List<String>,
        hayStack: CCUHsApi,
        domainService: DomainService
    ) {
        reconfiguredDeviceList.forEach { deviceId ->
            if (isSequenceModified(deviceId, sequenceMetaData, hayStack)) {
                CcuLog.i(
                    L.TAG_CCU_SEQUENCE_APPLY,
                    "Sequence Modified, Reconfiguring equips for device: $deviceId"
                )
                deleteLowCodeSequence(
                    reconfiguredDeviceList,
                    hayStack
                )

                createLowCodeSequence(
                    sequenceMetaData,
                    reconfiguredDeviceList,
                    hayStack,
                    domainService
                )
            } else {
                CcuLog.i(
                    L.TAG_CCU_SEQUENCE_APPLY,
                    "No changes detected for device: $deviceId, skipping reconfiguration"
                )
            }
        }
    }


    fun deleteLowCodeSequence(
        deletedDeviceList: List<String>,
        hayStack: CCUHsApi,
    ) {
        deletedDeviceList.forEach { device ->
            val deviceMap = hayStack.readMapById(device)
            val nodeAddress = deviceMap[a75f.io.api.haystack.Tags.ADDR].toString()
            if (deviceMap.contains(a75f.io.api.haystack.Tags.PCN)) {
                removePCNEquipTree(device.prependIndent("@"), hayStack)
            } else if (isConnectNodeInPCN(deviceMap)) {
                removeCNEquipTree(device.prependIndent("@"), hayStack)
            } else {
                removeConnectNodeEquips(nodeAddress, hayStack)
            }
        }
    }

    private fun readAndCreateEquip(
        metadata: ModelMetadata,
        deviceId: String,
        domainService: DomainService,
        hayStack: CCUHsApi,
        isCreate: Boolean
    ) {
        val modelId = metadata.modelId
        val modelVersion = metadata.modelVersion
        val points = metadata.points
        val modelName = metadata.modelName
        val suffix = metadata.suffix ?: ""

        val latch = CountDownLatch(1)
        val exceptionRef = AtomicReference<Exception?>(null)

         domainService.readExternalModbusModelById(modelId, modelVersion, object : ResponseCallback {
            override fun onSuccessResponse(response: String?) {
                try {
                    if (!response.isNullOrEmpty()) {
                        val equipDevice = parseModbusDataFromString(response)
                        equipDevice?.let {
                            val model = EquipModel().apply {
                                jsonContent = response
                                this.equipDevice.value = it
                                parameters = getParameters(it)
                                version.value = modelVersion
                                slaveId.intValue = it.slaveId
                            }
                            createEquipAndPoints(
                                model,
                                hayStack,
                                points,
                                deviceId,
                                modelVersion,
                                suffix,
                                modelId
                            )
                            CcuLog.i(
                                L.TAG_CCU_SEQUENCE_APPLY,
                                "Created equip: $modelId v$modelVersion name: $modelName for deviceId: $deviceId"
                            )
                        }
                    } else {
                        CcuLog.e(L.TAG_CCU_SEQUENCE_APPLY, "Empty response for : $modelId v$modelVersion")
                    }
                } catch (e: JsonParseException) {
                    CcuLog.e(
                        L.TAG_CCU_SEQUENCE_APPLY,
                        "Failed parsing model: $modelId v$modelVersion: ${e.message}"
                    )
                    exceptionRef.set(e)
                } finally {
                    latch.countDown()
                }
            }

            override fun onErrorResponse(response: String?) {
                CcuLog.e(L.TAG_CCU_SEQUENCE_APPLY, "Error reading model: $modelId v$modelVersion")
                if (isCreate) {
                    exceptionRef.set(
                        ModbusEquipCreationException(
                            "Failed to create equip for deviceId: $deviceId with model: $modelId v$modelVersion"
                        )
                    )
                }
                latch.countDown()
            }
        })

        // Block the thread until callback is received
        latch.await()

        // Throw any exception captured in callback
        exceptionRef.get()?.let { throw it }
    }

    private fun createEquipAndPoints(
        model: EquipModel,
        ccuHsApi: CCUHsApi,
        points: List<PointData>,
        deviceId: String,
        modelVersion: String,
        suffix: String,
        modelId: String
    ) {
        val deviceMap = ccuHsApi.readMapById(deviceId)
        val isConnectModule = deviceMap["domainName"].toString() == ModelNames.connectNodeDevice
        val isPCN = deviceMap["domainName"].toString() == ModelNames.pcnDevice
        val floorRef = deviceMap["floorRef"].toString()
        val roomRef = deviceMap["roomRef"].toString()
        val nodeAddress = deviceMap["addr"].toString()
        val slaveId = ConnectNodeUtil.getEquipSlaveIdByAddress(deviceMap["addr"].toString())
        val equipmentInfo = model.equipDevice.value.apply { this.slaveId = slaveId }
        val parameterList = getParametersList(equipmentInfo)
        val mapOfPointNameAndRegisterAddress =
            ConnectNodeUtil.getMapOfPointNameAndRegisterAddress(points)
        val profileType = if (isPCN) {
            ProfileType.PCN
        } else {
            ProfileType.CONNECTNODE
        }

        ModbusEquip(profileType, slaveId.toShort()).createEntities(
            floorRef,
            roomRef,
            equipmentInfo,
            parameterList,
            null,
            true,
            Tags.ZONE,
            modelVersion,
            isConnectModule,
            isPCN,
            false,
            mapOfPointNameAndRegisterAddress,
            false,
            suffix,
            nodeAddress,
            modelId,
            deviceId
        )
    }

    private fun getParametersList(equipment: EquipmentDevice): List<Parameter> =
        equipment.registers.mapNotNull { reg ->
            reg.parameters.firstOrNull()?.apply {
                registerNumber = reg.getRegisterNumber()
                registerAddress = reg.getRegisterAddress()
                registerType = reg.getRegisterType()
                parameterDefinitionType = reg.getParameterDefinitionType()
                multiplier = reg.getMultiplier()
                wordOrder = reg.getWordOrder()
            }
        }
}
