package a75f.io.logic.connectnode

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.modbus.Command
import a75f.io.api.haystack.modbus.Condition
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.api.haystack.modbus.Register
import a75f.io.api.haystack.modbus.UserIntentPointTags
import a75f.io.domain.service.DomainService
import a75f.io.domain.service.ResponseCallback
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.modbus.ModbusEquip
import a75f.io.logic.preconfig.ModbusEquipCreationException
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import com.google.gson.JsonParseException
import io.seventyfivef.ph.core.Tags
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class ConnectNodeEntitiesBuilder {

    fun createConnectNodeSequence(
        sequenceMetaData: SequenceMetaDataDTO,
        addedDeviceList: List<String>,
        hayStack: CCUHsApi,
        domainService: DomainService,
    ): List<Exception> {
        val errors = mutableListOf<Exception>()

        addedDeviceList.forEach { deviceId ->
            val equipWithRegNumber = getEquipWithRegisterNumber(hayStack, deviceId)

            sequenceMetaData.metadata.forEach { modelMetadata ->
                try {
                    val modelPointAddress = modelMetadata.points.firstOrNull()?.registerAddress
                    if (modelPointAddress != null && modelPointAddress in equipWithRegNumber.values.flatten()) {
                        deleteConnectNodeSequence(listOf(deviceId), hayStack)
                        CcuLog.i(
                            L.TAG_CONNECT_NODE,
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
                    L.TAG_CONNECT_NODE,
                    "Sequence Modified, Reconfiguring connect node equips for device: $deviceId"
                )
                deleteConnectNodeSequence(
                    reconfiguredDeviceList,
                    hayStack
                )

                createConnectNodeSequence(
                    sequenceMetaData,
                    reconfiguredDeviceList,
                    hayStack,
                    domainService
                )
            } else {
                CcuLog.i(
                    L.TAG_CONNECT_NODE,
                    "No changes detected for device: $deviceId, skipping reconfiguration"
                )
            }
        }
    }

    private fun isSequenceModified(
        deviceId: String,
        sequenceMetaData: SequenceMetaDataDTO,
        hayStack: CCUHsApi
    ): Boolean {
        val existingModbusTriples = getTripleOfRegNumAndModelId(hayStack, deviceId)
        val expectedModbusTriples = sequenceMetaData.metadata
            .flatMap { modelMetadata ->
                modelMetadata.points.map { point ->
                    Triple(modelMetadata.modelId, point.registerAddress, point.pointName)
                }
            }
        return existingModbusTriples.toSet() != expectedModbusTriples.toSet()
    }


    fun deleteConnectNodeSequence(
        deletedDeviceList: List<String>,
        hayStack: CCUHsApi,
    ) {
        deletedDeviceList.forEach { device ->
            val nodeAddress = ConnectNodeUtil.getAddressById(device, hayStack)
            ConnectNodeUtil.removeConnectNodeEquips(nodeAddress, hayStack)
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
                                L.TAG_CONNECT_NODE,
                                "Created equip: $modelId v$modelVersion name: $modelName for deviceId: $deviceId"
                            )
                        }
                    } else {
                        CcuLog.e(L.TAG_CONNECT_NODE, "Empty response for : $modelId v$modelVersion")
                    }
                } catch (e: JsonParseException) {
                    CcuLog.e(
                        L.TAG_CONNECT_NODE,
                        "Failed parsing model: $modelId v$modelVersion: ${e.message}"
                    )
                    exceptionRef.set(e)
                } finally {
                    latch.countDown()
                }
            }

            override fun onErrorResponse(response: String?) {
                CcuLog.e(L.TAG_CONNECT_NODE, "Error reading model: $modelId v$modelVersion")
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
        val floorRef = deviceMap["floorRef"].toString()
        val roomRef = deviceMap["roomRef"].toString()
        val nodeAddress = deviceMap["addr"].toString()
        val slaveId = ConnectNodeUtil.getEquipSlaveIdByAddress(deviceMap["addr"].toString())
        val equipmentInfo = model.equipDevice.value.apply { this.slaveId = slaveId }
        val parameterList = getParametersList(equipmentInfo)
        val mapOfPointNameAndRegisterAddress =
            ConnectNodeUtil.getMapOfPointNameAndRegisterAddress(points)

        ModbusEquip(ProfileType.CONNECTNODE, slaveId.toShort()).createEntities(
            floorRef,
            roomRef,
            equipmentInfo,
            parameterList,
            null,
            true,
            Tags.ZONE,
            modelVersion,
            true,
            mapOfPointNameAndRegisterAddress,
            false,
            suffix,
            nodeAddress,
            modelId
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

    private fun parseModbusDataFromString(json: String?): EquipmentDevice? = try {
         transformBaseFormatToModbus(json)
    } catch (e: Exception) {
        CcuLog.i(L.TAG_CONNECT_NODE, "Error parsing modbus data from string: $json")
           e.printStackTrace()
        null
    }


    /*CCU receives data in Base protocol format which is similar to Bacnet data class, so we are using that
    * and convert to modbus format*/

    private fun transformBaseFormatToModbus(response: String?): EquipmentDevice? {

        if (response.isNullOrEmpty()) {
            CcuLog.e(L.TAG_CONNECT_NODE, "Received empty JSON for EquipmentDevice")
            return null
        }
        val validResponse =  Gson().fromJson(response, BacnetModelDetailResponse::class.java)
        val jsonObj = JSONObject(response)

        val equipmentDevice = EquipmentDevice()

        equipmentDevice.name = validResponse.name
        equipmentDevice.equipType = "base"

        val points = validResponse.points

        val registers = mutableListOf<Register>()

        for (i in 0 until points.size) {
            val pointObject = points[i]
            val register = Register().apply {
                parameters = listOf(Parameter().apply {
                    name = pointObject.name
                    logicalPointTags = emptyList()
                    userIntentPointTags = emptyList()
                    parameterDefinitionType = "float"
                    isDisplayInUI = false
                    if(pointObject.valueConstraint != null) {
                        val valueConstraint = pointObject.valueConstraint
                        commands = valueConstraint?.allowedValues?.map { value ->
                             Command().apply {
                                this.name = value.value
                                this.bitValues = value.index.toString()
                            }
                        }
                        conditions = valueConstraint?.allowedValues?.map { value ->
                            Condition().apply {
                                this.name = value.value
                                this.bitValues = value.index.toString()
                            }
                        }
                        userIntentPointTags = pointObject.equipTagsList
                            .filter { it.kind == "marker" }
                            .map { tag ->
                                UserIntentPointTags().apply {
                                    tagName = tag.name
                                }
                            }


                        pointObject.point?.equipTagsList?.filter { it.kind == "marker" }
                            ?.map { tag ->
                                userIntentPointTags.add(
                                    UserIntentPointTags().apply {
                                        tagName = tag.name
                                    }
                                )
                            }


                        pointObject.presentationData?.tagValueIncrement?.let { value ->
                            // Dont remove below check, it is required to avoid null or empty values
                            if (!value.isNullOrEmpty()) {
                                userIntentPointTags.add(
                                    UserIntentPointTags().apply {
                                        tagName = "incrementVal"
                                        tagValue = value
                                    }
                                )
                            }
                        }

                        pointObject.equipTagNames.filter { it == Tags.CMD || it == Tags.SP }?.map {
                            userIntentPointTags.add(UserIntentPointTags().apply {
                                tagName = it
                            }
                            )
                        }

                        valueConstraint?.minValue?.let { minValue ->
                            userIntentPointTags.add(
                                UserIntentPointTags().apply {
                                    tagName = "minValue"
                                    tagValue = minValue.toString()
                                }
                            )
                        }

                        valueConstraint?.maxValue?.let { maxValue ->
                            userIntentPointTags.add(
                                UserIntentPointTags().apply {
                                    tagName = "maxValue"
                                    tagValue = maxValue.toString()
                                }
                            )
                        }

                        pointObject.equipTagValues?.forEach { (key, value) ->
                            userIntentPointTags.add(
                                UserIntentPointTags().apply {
                                    tagName = key
                                    tagValue = value
                                }
                            )
                        }
                        userIntentPointTags.add(
                            UserIntentPointTags().apply {
                                tagName = Tags.HIS_INTERPOLATE
                                tagValue = pointObject.hisInterpolate
                            }
                        )

                    }

                })
            }

            registers.add(register)
        }

        equipmentDevice.modelNumbers = listOf(jsonObj.optString("name", ""))
        equipmentDevice.registers = registers

        return equipmentDevice
    }

    private fun getParameters(equipment: EquipmentDevice): MutableList<RegisterItem> =
        equipment.registers.flatMapTo(mutableListOf()) { reg ->
            reg.parameters.mapNotNull { param ->
                param.registerNumber = reg.getRegisterNumber()
                param.registerAddress = reg.getRegisterAddress()
                param.registerType = reg.getRegisterType()
                param.parameterDefinitionType = reg.getParameterDefinitionType()
                param.multiplier = reg.multiplier
                RegisterItem().apply {
                    displayInUi.value = param.isDisplayInUI
                    this.param = mutableStateOf(param)
                }
            }
        }
    private fun getEquipWithRegisterNumber(
        hayStack: CCUHsApi,
        deviceId: String
    ): Map<String, List<String>> {
        val group = ConnectNodeUtil.getSlaveIdByDeviceId(deviceId, hayStack)
        val equipsList = hayStack.readAllEntities("equip and group == \"$group\"")
        return equipsList.associate { equip ->
            val equipId = equip["id"].toString()
            val regNumber =
                hayStack.readAllEntities("point and connectModule and equipRef == \"$equipId\"")
                    .map { it["registerNumber"].toString() }
            equipId to regNumber
        }
    }

    private fun getTripleOfRegNumAndModelId(
        hayStack: CCUHsApi,
        deviceId: String
    ): List<Triple<String, String, String>> {
        val group = ConnectNodeUtil.getSlaveIdByDeviceId(deviceId, hayStack)
        val equipsList = hayStack.readAllEntities("equip and group == \"$group\"")
        val triples = mutableListOf<Triple<String, String, String>>()

        for (equip in equipsList) {
            val modelId = equip["modelId"]?.toString() ?: continue
            val equipId = equip["id"].toString()

            val points = hayStack.readAllEntities("point and connectModule and equipRef == \"$equipId\"")
            for (point in points) {
                val registerNumber = point["registerNumber"]?.toString() ?: continue
                val shortDis = point["shortDis"]?.toString() ?: ""
                triples.add(Triple(modelId, registerNumber, shortDis))
            }
        }
        return triples
    }
}
