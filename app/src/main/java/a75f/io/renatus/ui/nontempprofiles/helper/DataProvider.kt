package a75f.io.renatus.ui.nontempprofiles.helper

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem
import a75f.io.api.haystack.modbus.Command
import a75f.io.api.haystack.modbus.EquipmentDevice
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.util.isPointFollowingScheduleOrEvent
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.renatus.ui.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.ui.nontempprofiles.utilities.findItemPosition
import a75f.io.renatus.ui.nontempprofiles.utilities.getSortedParameterList
import a75f.io.renatus.ui.nontempprofiles.utilities.getSpinnerValues
import a75f.io.renatus.ui.nontempprofiles.utilities.getUserIntentsCommandMap
import a75f.io.renatus.ui.nontempprofiles.utilities.getUserIntentsDoubleMap
import a75f.io.renatus.ui.nontempprofiles.utilities.readHisVal
import a75f.io.renatus.ui.nontempprofiles.utilities.readPoint
import a75f.io.renatus.ui.nontempprofiles.utilities.readVal
import a75f.io.renatus.ui.nontempprofiles.utilities.searchIndexValue
import a75f.io.renatus.ui.nontempprofiles.utilities.searchKeyForValue
import android.content.Context
import android.preference.PreferenceManager
import org.json.JSONException
import org.json.JSONObject


fun getModbusDetailedViewPoints(
    deviceObj: Any,
    equipType: String,
    equipRef: String
): List<ExternalPointItem> {
    var isConnectNode = false
    if (equipType == "connectModule") {
        isConnectNode = true
    }
    if (equipType == "modbus" || equipType == "connectModule") {

        val externalPoints = mutableListOf<ExternalPointItem>()
        val modbusDevice = deviceObj as EquipmentDevice
        val parameters = getSortedParameterList(modbusDevice)

        parameters.forEach { parameter ->
            val viewItem = ExternalPointItem()
            viewItem.id = parameter.logicalId
            viewItem.dis = parameter.name
            viewItem.point = parameter
            viewItem.profileType = equipType
            if (parameter.parameterDefinitionType != null) {
                if (
                    parameter.parameterDefinitionType in listOf(
                        "range", "float", "decimal", "long", "binary",
                        "integer", "boolean", "digital", "int32", "int64", "unsigned long"
                    )
                ) {
                    var unit: String? = null
                    val pointObject = readPoint(parameter, equipRef, isConnectNode)
                    if (
                        parameter.getUserIntentPointTags() != null &&
                        parameter.getUserIntentPointTags().isNotEmpty()
                    ) {

                        if (parameter.getCommands() != null && parameter.getCommands().isNotEmpty()) {
                            var commands: List<Command> = ArrayList()
                            val userIntentsMap: HashMap<String, List<Command>> =
                                getUserIntentsCommandMap(parameter)
                            for ((key, value) in userIntentsMap) {
                                unit = key
                                commands = value
                            }
                            viewItem.dropdownOptions = commands
                                .map { it.name }
                                .toMutableList()
                            viewItem.dropdownValues = commands
                                .map { it.bitValues.toDouble().toString() }
                                .toMutableList()
                            viewItem.usesDropdown = true

                            for (i in parameter.getCommands().indices) {
                                if (
                                    parameter.getCommands()[i].bitValues.toDouble() ==
                                    readVal(pointObject!!.id)
                                ) {
                                    viewItem.currentValue = i.toString()
                                    viewItem.selectedIndex = i
                                }
                            }

                        } else {
                            var doubleArrayList = ArrayList<Double>()
                            val userIntentsMap = getUserIntentsDoubleMap(parameter)
                            for ((key, value) in userIntentsMap) {
                                unit = key
                                doubleArrayList = value
                            }
                            if (doubleArrayList.isNotEmpty()) {
                                viewItem.usesDropdown = true
                                viewItem.dropdownOptions =
                                    doubleArrayList.map { it.toString() }.toMutableList()
                                viewItem.dropdownValues =
                                    doubleArrayList.map { it.toString().toDouble().toString() }.toMutableList()
                                viewItem.currentValue =
                                    doubleArrayList.indexOf(readVal(pointObject?.id)).toString()
                                viewItem.selectedIndex =
                                    doubleArrayList.indexOf(readVal(pointObject?.id))
                            } else {
                                viewItem.usesDropdown = false
                            }
                        }

                        if (unit != null && unit != "") {
                            viewItem.dis = parameter.name + "($unit)"
                        }

                        viewItem.canOverride =
                            pointObject != null && isPointFollowingScheduleOrEvent(pointObject.id)

                    } else {
                        if (
                            parameter.getLogicalPointTags() != null &&
                            parameter.getLogicalPointTags().size > 0
                        ) {
                            if (pointObject != null) {
                                unit = if (pointObject.unit == null) " " else pointObject.unit
                                var value = ""
                                if (
                                    parameter.getConditions() != null &&
                                    parameter.getConditions().size > 0
                                ) {
                                    for (i in parameter.getConditions().indices) {
                                        val bitValues: String =
                                            parameter.getConditions()[i].bitValues
                                        if (bitValues.toDouble() == readHisVal(pointObject.id)) {
                                            value = parameter.getConditions()[i].name
                                            break
                                        }
                                    }
                                } else {
                                    value =
                                        if (parameter.parameterDefinitionType == "binary") {
                                            if (readHisVal(pointObject.id) == 1.0) "ON" else "OFF"
                                        } else {
                                            readHisVal(pointObject.id).toString()
                                        }
                                }
                                if (unit != null && unit != " ") {
                                    viewItem.dis = parameter.name + " ($unit):"
                                    viewItem.currentValue = value
                                } else {
                                    viewItem.dis = parameter.name + ":"
                                    viewItem.currentValue = value
                                }
                            }
                        }
                    }
                } else {
                    viewItem.currentValue = if (parameter.isDisplayInUI) "ON" else "OFF"
                }
            } else {
                if (
                    parameter.getLogicalPointTags() != null &&
                    parameter.getLogicalPointTags().size > 0
                ) {
                    val p = readPoint(parameter, equipRef, isConnectNode)
                    if (p != null) {
                        val unit = if (p.unit == null) " " else p.unit
                        var value = ""
                        if (
                            parameter.getConditions() != null &&
                            parameter.getConditions().size > 0
                        ) {
                            for (i in parameter.getConditions().indices) {
                                val bitValues: String = parameter.getConditions()[i].bitValues
                                if (bitValues.toDouble() == readHisVal(p.id)) {
                                    value = parameter.getConditions()[i].name
                                    break
                                }
                            }
                        } else {
                            value = readHisVal(p.id).toString()
                        }
                        if (unit != null && unit != " ") {
                            viewItem.dis = parameter.name + "($unit)"
                            viewItem.currentValue = value
                        } else {
                            viewItem.dis = parameter.name + ":"
                            viewItem.currentValue = value
                        }
                    }
                }
            }
            externalPoints.add(viewItem)
        }
        return externalPoints
    } else {
        return emptyList()
    }
}

fun getBacnetDetailedViewPoints(
    context: Context,
    paramList: List<BacnetZoneViewItem>,
    profileType: String
): List<ExternalPointItem> {

    val externalPointItems = mutableListOf<ExternalPointItem>()
    var serverIpAddress = ""

    val bacnetServerConfig = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
    if (bacnetServerConfig != null) {
        try {
            val config = JSONObject(bacnetServerConfig)
            val networkObject = config.getJSONObject("network")
            serverIpAddress = networkObject.getString(BacnetConfigConstants.IP_ADDRESS)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    for (param in paramList){
        val viewItem = ExternalPointItem()
        viewItem.id = param.bacnetObj.id
        viewItem.profileType = profileType
        viewItem.point = param
        viewItem.serverIpAddress = serverIpAddress
        val bacnetZoneViewItem: BacnetZoneViewItem = param
        val title = if (bacnetZoneViewItem.bacnetObj.defaultUnit.isEmpty()) {
            bacnetZoneViewItem.disName + " : "
        } else {
            bacnetZoneViewItem.disName + " (" + bacnetZoneViewItem.bacnetObj.defaultUnit + ") : "
        }

        CcuLog.d(Tags.CCU_ZONE_SCREEN, "onBindViewHolder -->$title")
        viewItem.dis = title


        val pointMap = CCUHsApi.getInstance().readHDictById(bacnetZoneViewItem.bacnetObj.id)
        val p = Point.Builder().setHDict(pointMap).build()

        if (bacnetZoneViewItem.isWritable) {
            val dropdownOptions: List<String> = getSpinnerValues(bacnetZoneViewItem.spinnerValues)
            viewItem.dropdownOptions = dropdownOptions.toMutableList()
            viewItem.usesDropdown = true
            viewItem.canOverride = p != null && isPointFollowingScheduleOrEvent(p.id)

            val enumValues = p.enums
            var itemIndex: Int
            if (enumValues != null) {
                CcuLog.d(
                    Tags.CCU_ZONE_SCREEN,
                    "onBindViewHolder bacnetZoneViewItem writable point enum-->" + bacnetZoneViewItem.bacnetObj.id + "--" + enumValues
                )
                itemIndex = searchIndexValue(
                    enumValues,
                    bacnetZoneViewItem.value.toDouble().toInt().toString()
                )
            } else {
                try {
                    itemIndex = findItemPosition(
                        bacnetZoneViewItem.spinnerValues,
                        bacnetZoneViewItem.value.toDouble()
                    )
                } catch (e: NumberFormatException) {
                    CcuLog.d(Tags.CCU_ZONE_SCREEN, "this is not a number")
                    itemIndex = bacnetZoneViewItem.value.toDouble().toInt()
                }
            }
            CcuLog.d(
                Tags.CCU_ZONE_SCREEN,
                "onBindViewHolder:: " + bacnetZoneViewItem.spinnerValues + " searching for-> " + bacnetZoneViewItem.value.toDouble()
                    .toString() + "<--found at index-->" + itemIndex
            )
            viewItem.currentValue = itemIndex.toString()
            viewItem.selectedIndex = itemIndex
            viewItem.canOverride = p != null && isPointFollowingScheduleOrEvent(p.id)
        } else {
            val enumValues = CCUHsApi.getInstance()
                .readMapById(bacnetZoneViewItem.bacnetObj.id)["enum"] as String?
            if (enumValues != null) {
                CcuLog.d(
                    Tags.CCU_ZONE_SCREEN,
                    "onBindViewHolder bacnetZoneViewItem-->" + bacnetZoneViewItem.bacnetObj.id + "--" + enumValues
                )
                val key: String? = searchKeyForValue(
                    enumValues,
                    bacnetZoneViewItem.value.toDouble().toInt().toString()
                )
                viewItem.currentValue = key ?: ""

            } else {
                viewItem.currentValue = bacnetZoneViewItem.value
            }
        }
        externalPointItems.add(viewItem)
        CcuLog.d(
            Tags.CCU_ZONE_SCREEN,
            "item added-->" + viewItem.dis + " with value -->" + viewItem.currentValue
        )
    }
    return externalPointItems
}