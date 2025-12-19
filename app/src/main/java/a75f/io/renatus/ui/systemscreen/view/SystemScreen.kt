package a75f.io.renatus.ui.systemscreen.view

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.systemEnhancedVentilationEnable
import a75f.io.domain.api.DomainName.systemPostPurgeEnable
import a75f.io.domain.api.DomainName.systemPrePurgeEnable
import a75f.io.logic.L
import a75f.io.logic.bo.util.DemandResponseMode
import a75f.io.renatus.R
import a75f.io.renatus.composables.HeaderLabelView
import a75f.io.renatus.composables.LabelView
import a75f.io.renatus.composables.StyledLabelView
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.util.isOaoPairedInConnectModule
import a75f.io.renatus.ui.systemscreen.viewmodel.SystemViewModel
import a75f.io.renatus.ui.zonescreen.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.views.DetailedViewGridItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.views.ExternalPointGridItem
import a75f.io.renatus.ui.zonescreen.screens.HeaderRow
import a75f.io.renatus.ui.zonescreen.screens.HeartBeatCompose
import a75f.io.renatus.ui.zonescreen.screens.TextAppearance
import a75f.io.renatus.ui.zonescreen.screens.titleGreyStyle
import a75f.io.renatus.util.SystemProfileUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


fun ComposeView.showHeaderViews(
    systemViewModel: SystemViewModel
) {
    setContent {
            HeaderView()
    }
}

@Composable
fun HeaderView() {
    val ccuName = Domain.ccuDevice.ccuDisName
    val profileName = L.ccu().systemProfile.profileName

    Column(
        modifier = Modifier.wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val titleOrange = TextAppearance(R.attr.title_orange)
        val titleGreyStyle = titleGreyStyle()
        Row(
            modifier = Modifier
                .padding(start = 80.dp, top = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ccuName,
                style = titleOrange,
            )

            Text(
                text = profileName,
                modifier = Modifier.padding(start = 14.dp),
                style = titleGreyStyle,
            )
        }
    }
}

fun ComposeView.showProfileComposeView(
    systemViewModel: SystemViewModel,
    bacnetEquipTypeString: String = "",
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit
) {
    setContent {
        ShowMiddleView(systemViewModel, bacnetEquipTypeString, onValueChange)
    }
}

@Composable
fun PlainLabelComposeView(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = Color.Black,
        )
    )
}

@Composable
fun PlainTextComposeView(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = myFontFamily,
            fontSize = 22.sp,
            color = Color.Black
        )
    )
}

@Composable
fun ShowMiddleView(
    systemViewModel: SystemViewModel,
    bacnetEquipTypeString: String = "",
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit
) {
     systemViewModel.occupancyStatus.value
    val equipStatus = systemViewModel.equipStatus.value
    val lastUpdated = systemViewModel.lastUpdated.value
    val minInsideHumidity = systemViewModel.minInsideHumidity.value
    val maxInsideHumidity = systemViewModel.maxInsideHumidity.value


    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            HeaderLabelView("Occupancy Status:", 0) { }
            LabelView(systemViewModel.occupancyStatus.value) {}
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            if(systemViewModel.isConnectAndAdvanceAHUPaired) {
                HeaderLabelView("CM Status:", 0) { }
            } else {
                HeaderLabelView("Equipment Status:", 0) { }
            }
            StyledLabelView(equipStatus) {}
        }

        if (systemViewModel.isConnectAndAdvanceAHUPaired) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                HeaderLabelView("CN 1 Status:", 0) { }
                StyledLabelView(systemViewModel.cnEquipStatus.value) { }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            HeaderLabelView("Last Updated:", 0) { }
            LabelView(lastUpdated) {}
        }

        if (systemViewModel.isExternalAhuPaired || systemViewModel.isAdvancedAHUPaired) {

            if (systemViewModel.isExternalAhuPaired ||
                (systemViewModel.isPressureControlAvailable.value)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Duct Static Pressure:",
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            fontFamily = myFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            color = Color.Black,
                        )
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "Current:",
                            style = TextStyle(
                                fontFamily = myFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Normal,
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                                color = Color.Black,
                            )
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = systemViewModel.ductStaticPressureSensor.value.currentValue.toString(),
                            modifier = Modifier.weight(1f),
                            style = TextStyle(
                                fontFamily = myFontFamily,
                                fontSize = 22.sp,
                                color = Color.Black
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "SetPoint:",
                            style = TextStyle(
                                fontFamily = myFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Normal,
                                fontSize = 22.sp,
                                lineHeight = 26.sp,
                                color = Color.Black,
                            )
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = systemViewModel.ductStaticPressureSetpoint.value.currentValue.toString(),
                            modifier = Modifier.weight(1f),
                            style = TextStyle(
                                fontFamily = myFontFamily,
                                fontSize = 22.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            if (systemViewModel.isExternalAhuPaired ||
                (systemViewModel.isDualSetPointControlEnabled.value)
            ) {


                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Supply Airflow Temperature:",
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            fontFamily = myFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            color = Color.Black,
                        )
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        PlainLabelComposeView("Current: ")
                        Spacer(modifier = Modifier.width(2.dp))
                        PlainTextComposeView(systemViewModel.dischargeAirTempSensor.value.currentValue.toString())
                    }

                    if (systemViewModel.isDualSetPointControlEnabled.value) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            PlainLabelComposeView("Operating Mode: ")
                            Spacer(modifier = Modifier.width(2.dp))
                            PlainTextComposeView(systemViewModel.operatingMode.value.currentValue.toString())
                        }
                    } else {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            PlainLabelComposeView("SetPoint: ")
                            Spacer(modifier = Modifier.width(2.dp))
                            PlainTextComposeView(systemViewModel.supplyAirflowTemperatureSetpoint.value.currentValue.toString())
                        }
                    }
                }
            }

            if(systemViewModel.isDualSetPointControlEnabled.value){
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "",
                        modifier = Modifier.weight(1f),
                        style = TextStyle(
                            fontFamily = myFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Normal,
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            color = Color.Black,
                        )
                    )

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (systemViewModel.isExternalAhuPaired ||
                            systemViewModel.isSATHeatingAvailable.value
                        ) {
                            PlainLabelComposeView("Heating SetPoint: ")
                            Spacer(modifier = Modifier.width(2.dp))
                            PlainTextComposeView(systemViewModel.satHeatingSetPoint.value.currentValue.toString())
                        }
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (systemViewModel.isExternalAhuPaired ||
                            systemViewModel.isSATCoolingAvailable.value
                        ) {
                            PlainLabelComposeView("Cooling SetPoint: ")
                            Spacer(modifier = Modifier.width(2.dp))
                            PlainTextComposeView(systemViewModel.satCoolingSetPoint.value.currentValue.toString())
                        }
                    }
                }

            }

        }

        if (systemViewModel.isDCVEnabled.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        PlainLabelComposeView("DCV Damper: ")
                        Spacer(modifier = Modifier.width(2.dp))
                        PlainTextComposeView(systemViewModel.dcvDamperPos.value.currentValue.toString())
                    }
                }
            }

        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val coroutineScope = rememberCoroutineScope()
            val orderedPoints = listOf(minInsideHumidity, maxInsideHumidity)

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(
                    items = orderedPoints,
                    key = { index, point ->
                        point.id ?: "index_$index"
                    }
                ) { _, point ->
                    DetailedViewGridItem(point = point) { selectedIndex, selectedPoint ->
                        coroutineScope.launch(Dispatchers.IO) {
                            if (selectedPoint.disName?.contains("Min Inside Humidity") == true) {
                                SystemProfileUtil
                                    .setUserIntentBackground(
                                        "domainName == \"" + DomainName.systemtargetMinInsideHumidity + "\"",
                                        selectedIndex.toDouble()
                                    )
                            } else {
                                SystemProfileUtil
                                    .setUserIntentBackground(
                                        "domainName == \"" + DomainName.systemtargetMaxInsideHumidity + "\"",
                                        selectedIndex.toDouble()
                                    )
                            }
                        }
                    }
                }
            }
        }

        if (systemViewModel.isDemandResponseEnrolled.value.state ||
            systemViewModel.isDemandResponseModeActivated.value.state
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderLabelView("Demand Response Active", 0) { }
                Spacer(modifier = Modifier.width(6.dp))
                ToggleButton(systemViewModel.isDemandResponseModeActivated.value.state) { newValue ->
                    systemViewModel.isDemandResponseModeActivated.value =
                        systemViewModel.isDemandResponseModeActivated.value.copy(state = newValue)
                    DemandResponseMode.setDRModeActivationStatus(
                        systemViewModel.isDemandResponseModeActivated.value.state
                    )

                }
            }
        }

        if (systemViewModel.isVavIERtu) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderLabelView("Occupancy Status on IE Gateway:", 0) { }
                Spacer(modifier = Modifier.width(6.dp))
                LabelView(systemViewModel.occupancyStatusOnIE.value) {}
            }
        }
        if (systemViewModel.isExternalAhuPaired &&
            (systemViewModel.isModbusExists || systemViewModel.isBacnetExists)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((2).dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    EquipNameWithHeartbeat(
                        text = systemViewModel.externalEquipName.value.currentValue.toString(),
                        isActive = systemViewModel.externalEquipHeartBeat.value
                    )
                }
                if (systemViewModel.isBacnetExists) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = bacnetEquipTypeString,
                            color = Color.DarkGray,
                            fontSize = 18.sp,
                        )
                    }
                }
            }
            // last updated
            Row {
                HeaderRow(
                    systemViewModel.externalEquipLastUpdated.value,
                    onValueChange = { _, _ -> }
                )
            }

            if (systemViewModel.detailedViewPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val points = systemViewModel.detailedViewPoints
                val chunkedList = points.chunked(2)
                chunkedList.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        rowItems.forEach { point ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                ExternalPointGridItem(point = point, onValueChange)
                            }
                        }
                        if (rowItems.size < 2) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }

            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}


fun ComposeView.showEpidemicModeView(
    systemViewModel: SystemViewModel,
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit
) {
    setContent {
        ShowEpidemicComposeView(systemViewModel, onValueChange)
    }
}

@Composable
fun ShowEpidemicComposeView(
    systemViewModel: SystemViewModel,
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        EpidemicModeComposable(systemViewModel, onValueChange)
        EMRComposable(systemViewModel, onValueChange)
        BtuMeterComposable(systemViewModel, onValueChange)
    }
}

@Composable
fun EpidemicModeComposable(
    systemViewModel: SystemViewModel,
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit
) {
    if (systemViewModel.isOAOProfilePaired) {
        Column(
            modifier = Modifier
                .wrapContentSize()
                .padding(top = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeaderLabelView("Epidemic Mode Settings", 0) { }

            if (!isOaoPairedInConnectModule()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderLabelView("OAO Last Updated: ", 0) { }
                    Spacer(modifier = Modifier.width(4.dp))
                    LabelView(systemViewModel.oaoLastUpdated.value) {}
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                HeaderLabelView("Smart Pre Purge ", 0) { }
                ToggleButton(systemViewModel.smartPrePurgeState.value.state) { newValue ->
                    systemViewModel.smartPrePurgeState.value =
                        systemViewModel.smartPrePurgeState.value.copy(state = newValue)
                    SystemProfileUtil.setUserIntentByDomain(
                        systemPrePurgeEnable,
                        if (systemViewModel.smartPrePurgeState.value.state) 1.0 else 0.0
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderLabelView("Smart Post Purge ", 0) { }
                ToggleButton(systemViewModel.smartPostPurgeState.value.state) { newValue ->
                    systemViewModel.smartPostPurgeState.value =
                        systemViewModel.smartPostPurgeState.value.copy(state = newValue)
                    SystemProfileUtil.setUserIntentByDomain(
                        systemPostPurgeEnable,
                        if (systemViewModel.smartPostPurgeState.value.state) 1.0 else 0.0
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderLabelView("Enhanced Ventilation ", 0) { }
                ToggleButton(systemViewModel.enhancedVentilation.value.state) { newValue ->
                    systemViewModel.enhancedVentilation.value =
                        systemViewModel.enhancedVentilation.value.copy(state = newValue)
                    SystemProfileUtil.setUserIntentByDomain(
                        systemEnhancedVentilationEnable,
                        if (systemViewModel.enhancedVentilation.value.state) 1.0 else 0.0
                    )
                }
            }

        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}


@Composable
fun BtuMeterComposable(
    systemViewModel: SystemViewModel,
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit
) {
    if (systemViewModel.isBTUPaired.value) {
        Spacer(modifier = Modifier.height(4.dp))

        // heartbeat and equip name
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = systemViewModel.btuEquipName.value.currentValue.toString(),
                color = Color.Black,
                fontSize = 24.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            Box(modifier = Modifier.wrapContentSize()) {
                HeartBeatCompose(
                    isActive = systemViewModel.btuHeartBeat.value,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                )
            }
        }
        // last updated
        Row {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                HeaderRow(
                    systemViewModel.btuLastUpdated.value,
                    onValueChange = { _, _ -> }
                )
            }
        }
        if (systemViewModel.btuMeterPoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val points = systemViewModel.btuMeterPoints
            val chunkedList = points.chunked(2)
            chunkedList.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { point ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            ExternalPointGridItem(point = point, onValueChange)
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun EMRComposable(systemViewModel: SystemViewModel, onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit){
    if (systemViewModel.isEMRPaired.value) {
        Spacer(modifier = Modifier.height(4.dp))

        // heartbeat and equip name
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = systemViewModel.emrEquipName.value.currentValue.toString(),
                color = Color.Black,
                fontSize = 24.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            Box(modifier = Modifier.wrapContentSize()) {
                HeartBeatCompose(
                    isActive = systemViewModel.emrHeartBeat.value,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                )
            }
        }
        // last updated
        Row {
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                HeaderRow(
                    systemViewModel.emrLastUpdated.value,
                    onValueChange = { _, _ -> }
                )
            }
        }
        if (systemViewModel.emrPoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val points = systemViewModel.emrPoints
            val chunkedList = points.chunked(2)
            chunkedList.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { point ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            ExternalPointGridItem(point = point, onValueChange)
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun EquipNameWithHeartbeat(
    text: String,
    isActive: Boolean
) {
    val annotated = buildAnnotatedString {
        append(text)
        append(" ")
        appendInlineContent("heartbeat")
    }

    val inlineContent = mapOf(
        "heartbeat" to InlineTextContent(
            Placeholder(
                width = 10.sp,
                height = 10.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextTop
            )
        ) {
            HeartBeatCompose(
                isActive = isActive,
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
            )
        }
    )

    Text(
        text = annotated,
        inlineContent = inlineContent,
        fontSize = 24.sp,
        lineHeight = 42.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}


