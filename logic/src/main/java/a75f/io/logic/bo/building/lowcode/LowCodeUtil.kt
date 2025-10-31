package a75f.io.logic.bo.building.lowcode

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.modbus.Command
import a75f.io.api.haystack.modbus.Condition
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.api.haystack.modbus.Register
import a75f.io.api.haystack.modbus.UserIntentPointTags
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.connectnode.RegisterItem
import a75f.io.logic.connectnode.SequenceMetaDataDTO
import androidx.compose.runtime.mutableStateOf
import com.google.gson.Gson
import io.seventyfivef.ph.core.Tags
import org.json.JSONObject

class LowCodeUtil {
    companion object {
        fun getParameters(equipment: EquipmentDevice): MutableList<RegisterItem> =
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
                        this.schedulable.value = param.isSchedulable
                    }
                }
            }

        fun isAllParamsSelected(equipDevice: EquipmentDevice): Boolean {
            var isAllSelected = true
            if (equipDevice.registers.isNotEmpty()) {
                equipDevice.registers.forEach {
                    if (!it.parameters[0].isDisplayInUI)
                        isAllSelected = false
                }
            }
            if (!equipDevice.equips.isNullOrEmpty()) {
                equipDevice.equips.forEach { subEquip ->
                    if (subEquip.registers.isNotEmpty()) {
                        subEquip.registers[0].parameters.forEach {
                            if (!it.isDisplayInUI)
                                isAllSelected = false
                        }
                    }
                }
            }
            return isAllSelected
        }

         private fun getTripleOfRegNumAndModelId(
            hayStack: CCUHsApi,
            deviceId: String
        ): List<Triple<String, String, String>> {
             val deviceMap = hayStack.readMapById(deviceId.prependIndent("@"))
             val isPCN = deviceMap["domainName"].toString() == ModelNames.pcnDevice
             val equipsList: ArrayList<HashMap<Any, Any>>
             if (isPCN) {
                 equipsList = hayStack.readAllEntities(
                     "equip and roomRef == \"${deviceMap[Tags.ROOM_REF]}\""
                 )
             } else {
                 val group = ConnectNodeUtil.getSlaveIdByDeviceId(deviceId, hayStack)
                 equipsList = hayStack.readAllEntities("equip and group == \"$group\"")
             }

            val triples = mutableListOf<Triple<String, String, String>>()

            for (equip in equipsList) {
                val modelId = equip["modelId"]?.toString() ?: continue
                val equipId = equip["id"].toString()

                val points = hayStack.readAllEntities("point and (connectModule or pcn) and equipRef == \"$equipId\"")
                for (point in points) {
                    val registerNumber = point["registerNumber"]?.toString() ?: continue
                    val shortDis = point["shortDis"]?.toString() ?: ""
                    triples.add(Triple(modelId, registerNumber, shortDis))
                }
            }
            return triples
        }

        fun isSequenceModified(
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

        fun parseModbusDataFromString(json: String?): EquipmentDevice? = try {
            transformBaseFormatToModbus(json)
        } catch (e: Exception) {
            CcuLog.i(L.TAG_CCU_SEQUENCE_APPLY, "Error parsing modbus data from string: $json")
            e.printStackTrace()
            null
        }


        /*CCU receives data in Base protocol format which is similar to Bacnet data class, so we are using that
        * and convert to modbus format*/

        private fun transformBaseFormatToModbus(response: String?): EquipmentDevice? {

            if (response.isNullOrEmpty()) {
                CcuLog.e(L.TAG_CCU_SEQUENCE_APPLY, "Received empty JSON for EquipmentDevice")
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
                                if (value.isNotEmpty()) {
                                    userIntentPointTags.add(
                                        UserIntentPointTags().apply {
                                            tagName = "incrementVal"
                                            tagValue = value
                                        }
                                    )
                                }
                            }

                            pointObject.equipTagNames.filter { it == Tags.CMD || it == Tags.SP }.map {
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
                            valueConstraint?.maxValue?.let { maxValue ->
                                userIntentPointTags.add(
                                    UserIntentPointTags().apply {
                                        tagName = "maxValue"
                                        tagValue = maxValue.toString()
                                    }
                                )
                            }

                        }

                    })
                }

                registers.add(register)
            }

            equipmentDevice.modelNumbers = listOf(jsonObj.optString("name", ""))
            equipmentDevice.registers = registers

            return equipmentDevice
        }


        fun getEquipWithRegisterNumber(
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

        fun getLowCodeSlaveIdList(
            ccuHsApi: CCUHsApi
        ): List<Int> {
            val connectNodeList = ccuHsApi.readAllEntities(
                "device and (domainName == \"" + DomainName.connectNodeDevice + "\" or domainName == \"" + ModelNames.pcnDevice +"\")"
            )
            return connectNodeList.mapNotNull { node ->
                node["addr"].toString().toInt().let { addr ->
                    if (addr == 1000) 49 else addr % 100
                }
            }
        }
    }
}
