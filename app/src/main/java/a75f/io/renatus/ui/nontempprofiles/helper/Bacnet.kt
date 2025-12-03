package a75f.io.renatus.ui.nontempprofiles.helper

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.bacnet.parser.AllowedValues
import a75f.io.api.haystack.bacnet.parser.BacnetPoint
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface
import a75f.io.renatus.ENGG.bacnet.services.BacNetConstants
import a75f.io.renatus.R
import a75f.io.renatus.ui.nontempprofiles.utilities.externalEquipsLayoutSetup
import a75f.io.renatus.ui.nontempprofiles.utilities.getPointScheduleHeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.utilities.showExternalEquipPointsUI
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import android.util.Pair
import android.view.View
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.DecimalFormat
import java.util.function.Consumer


fun loadBacnetZone(
    nonTempProfileViewModels: MutableList<NonTempProfileViewModel>,
    equipId: String,
    equipmentDeviceName: String,
    showLastUpdatedTime: Boolean,
    externalEquipDevice: Any,
    zoneDetailsView: View,
    linearLayoutZonePoints: LinearLayout,
    bacnetPointsList: List<BacnetZoneViewItem>,
    bacnetEquipTypeString : String,
    remotePointUpdateInterface: RemotePointUpdateInterface
) {
    val composeView = zoneDetailsView.findViewById<ComposeView>(R.id.detailedComposeView)
    externalEquipsLayoutSetup(linearLayoutZonePoints, zoneDetailsView)
    val viewModel = NonTempProfileViewModel()
    viewModel.profile = "bacnet"
    nonTempProfileViewModels.add(viewModel)
    showExternalEquipPointsUI(composeView, viewModel, equipId, null, remotePointUpdateInterface,bacnetEquipTypeString = bacnetEquipTypeString)

    val bacnetDevice = externalEquipDevice as HashMap<*, *>
    val address = bacnetDevice["addr"].toString()
    viewModel.initializeEquipHealth(
        equipmentDeviceName,
        showLastUpdatedTime,
        address
    ) // lastUpdate and heartbeat
    val bacnetPoints: List<BacnetZoneViewItem> = bacnetPointsList
    viewModel.setEquipStatusPoint(getPointScheduleHeaderViewItem(equipId))
    // status message
    viewModel.backgroundJob = CoroutineScope(Dispatchers.Default).launch {
        val points = withContext(Dispatchers.IO) {
            preparePoints(getBacnetDetailedViewPoints(
                Globals.getInstance().applicationContext,
                bacnetPoints,
                "bacnet"
            ))
        }
        viewModel.initializeDetailedViewPoints(points)
        viewModel.showLoader.value = false
    }
    viewModel.observeExternalEquipHealth(address)
}

fun generateValuesForSpinner(
    minValue: Double,
    maxValue: Double,
    step: Double
): MutableList<Pair<String, Int>> {
    val result = mutableListOf<Pair<String, Int>>()
    var min = BigDecimal.valueOf(minValue)
    val max = BigDecimal.valueOf(maxValue)
    val increment = BigDecimal.valueOf(step)
    val df = DecimalFormat("#.##")
    while (min <= max) {
        result.add(Pair(df.format(min), 0))
        min = min.add(increment)
    }
    return result
}

fun fetchZoneDataForBacnet(
    bacnetPoints: List<BacnetPoint>,
    bacnetConfig: String
): List<BacnetZoneViewItem> {
    val listBacnetZoneViewItems: MutableList<BacnetZoneViewItem> = ArrayList()
    val parameterList: MutableList<String> = ArrayList()
    for (bacnetPoint in bacnetPoints) {
        CcuLog.d(Tags.BACNET, "bacnet tags: " + bacnetPoint.equipTagNames)
        var isWritable = false
        val objectTypeFromProtocolData = bacnetPoint.protocolData!!.bacnet.objectType
        var objectType = BacNetConstants.ObjectType.OBJECT_ANALOG_VALUE.key
        var spinnerValues: MutableList<Pair<String, Int>> = ArrayList()
        if (bacnetPoint.equipTagNames.contains("writable")) {
            isWritable = true
            val presentationData = bacnetPoint.presentationData
            val valueConstraint = bacnetPoint.valueConstraint
            objectType =
                if (objectTypeFromProtocolData.equals("MultiStateValue", ignoreCase = true)) {
                    BacNetConstants.ObjectType.OBJECT_MULTI_STATE_VALUE.key
                } else if (objectTypeFromProtocolData.equals(
                        "MultiStateInput",
                        ignoreCase = true
                    )
                ) {
                    BacNetConstants.ObjectType.OBJECT_MULTI_STATE_INPUT.key
                } else if (objectTypeFromProtocolData.equals(
                        "MultiStateOutput",
                        ignoreCase = true
                    )
                ) {
                    BacNetConstants.ObjectType.OBJECT_MULTI_STATE_OUTPUT.key
                } else if (objectTypeFromProtocolData.equals(
                        "BinaryValue",
                        ignoreCase = true
                    )
                ) {
                    BacNetConstants.ObjectType.OBJECT_BINARY_VALUE.key
                } else if (objectTypeFromProtocolData.equals(
                        "BinaryInput",
                        ignoreCase = true
                    )
                ) {
                    BacNetConstants.ObjectType.OBJECT_BINARY_INPUT.key
                } else if (objectTypeFromProtocolData.equals(
                        "BinaryOutput",
                        ignoreCase = true
                    )
                ) {
                    BacNetConstants.ObjectType.OBJECT_BINARY_OUTPUT.key
                } else if (objectTypeFromProtocolData.equals(
                        "AnalogInput",
                        ignoreCase = true
                    )
                ) {
                    BacNetConstants.ObjectType.OBJECT_ANALOG_INPUT.key
                } else if (objectTypeFromProtocolData.equals(
                        "AnalogOutput",
                        ignoreCase = true
                    )
                ) {
                    BacNetConstants.ObjectType.OBJECT_ANALOG_OUTPUT.key
                } else {
                    BacNetConstants.ObjectType.OBJECT_ANALOG_VALUE.key
                }

            if (presentationData != null && valueConstraint != null) {
                if (valueConstraint.minValue != null && valueConstraint.maxValue != null) {
                    val minValue = valueConstraint.minValue!!.toInt()
                    val maxValue = valueConstraint.maxValue!!.toInt()
                    //float step = Float.parseFloat(presentationData.getTagValueIncrement(
                    val step = presentationData.tagValueIncrement!!.toDouble()
                    spinnerValues =
                        generateValuesForSpinner(minValue.toDouble(), maxValue.toDouble(), step)
                    CcuLog.d(
                        Tags.BACNET,
                        "For point id--> " + bacnetPoint.id + " MinValue: " + minValue + " MaxValue: " + maxValue + " Increment Value: " + step+"\n Spinner values: "+spinnerValues.toString()
                    )
                } else {
                    CcuLog.d(
                        Tags.BACNET,
                        "For point id--> " + bacnetPoint.id + " there is no min max value checking multi state"
                    )
                    val finalSpinnerValues = spinnerValues
                    valueConstraint.allowedValues!!.forEach(Consumer { allowedValue: AllowedValues ->
                        CcuLog.d(
                            Tags.BACNET,
                            "For point id--> " + bacnetPoint.id + " Allowed Value: " + allowedValue
                        )
                        finalSpinnerValues.add(Pair(allowedValue.value, allowedValue.index))
                    })
                }
            }
        }

        if (bacnetPoint.protocolData!!.bacnet.displayInUIDefault) {
            val pointName = bacnetPoint.disName
            val pointId = bacnetPoint.id
            val value = CCUHsApi.getInstance().readDefaultStrVal(pointId)
            val hisValue = CCUHsApi.getInstance().readHisValById(pointId).toString()
            val defaultValById = CCUHsApi.getInstance().readDefaultValById(pointId).toString()
            CcuLog.d(
                Tags.BACNET,
                ("pointName:: " + pointName + "pointId: " + pointId + "value: " + value
                        + "hisValue: " + hisValue + "defaultValById: " + defaultValById)
            )
            parameterList.add("$pointName : $hisValue")
            val currentValue: String = hisValue

            val bacnetZoneViewItem = BacnetZoneViewItem(
                pointName, currentValue,
                bacnetConfig, true, bacnetPoint, isWritable, spinnerValues, objectType
            )
            listBacnetZoneViewItems.add(bacnetZoneViewItem)
        }
    }
    return listBacnetZoneViewItems
}