package a75f.io.renatus.profiles.hss

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.CANCEL
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import a75f.io.domain.api.DomainName
import a75f.io.logic.Globals
import a75f.io.messaging.handler.HyperstatSplitReconfigurationHandler
import a75f.io.renatus.R
import a75f.io.renatus.composables.TempOffsetPicker
import a75f.io.renatus.compose.BoldStyledTextView
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.compose.dropDownHeight
import a75f.io.renatus.compose.noOfItemsDisplayInDropDown
import a75f.io.renatus.compose.simpleVerticalScrollbar
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuState
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

open class HyperStatSplitFragment : BaseDialogFragment() {

    override fun onDestroy() {
        super.onDestroy()
        if (Globals.getInstance().isTestMode) {
            Globals.getInstance().isTestMode = false
        }
    }

    @Composable
    fun Title(viewModel: HyperStatSplitCpuViewModel) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (viewModel.profileType) {
                ProfileType.HYPERSTATSPLIT_CPU -> TitleTextView("CPU & ECONOMIZER")
                else -> TitleTextView("CPU & ECONOMIZER")
            }
        }
    }

    override fun getIdString() : String { return "" }

    @Composable
    fun TempOffset(viewModel: HyperStatSplitCpuViewModel) {
        val valuesPickerState = rememberPickerState()
        Column(modifier = Modifier.padding(start = 400.dp, end = 400.dp)) {
            TempOffsetPicker(
                header = "TEMPERATURE OFFSET",
                state = valuesPickerState,
                items = viewModel.temperatureOffsetsList,
                onChanged = { it: String -> viewModel.viewState.value.temperatureOffset = it.toDouble() },
                startIndex = viewModel.temperatureOffsetsList.indexOf(viewModel.viewState.value.temperatureOffset.toString()),
                visibleItemsCount = 3,
                textModifier = Modifier.padding(8.dp),
                textStyle = TextStyle(fontSize = 18.sp)
            )
        }
    }

    @Composable
    fun AutoAwayConfig(viewModel: HyperStatSplitCpuViewModel) {
        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp, bottom = 25.dp)) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)) {
                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.autoForceOccupied.disName, fontSize = 20, textAlignment = TextAlign.Start)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.value.autoForceOccupied,
                        onEnabled = { it -> viewModel.viewState.value.autoForceOccupied = it }
                    )
                }
                Spacer(modifier = Modifier.width(375.dp))
                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.autoAway.disName, fontSize = 20, textAlignment = TextAlign.Start)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.value.autoAway,
                        onEnabled = { it -> viewModel.viewState.value.autoAway = it }
                    )
                }
            }

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)) {
                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.enableOutsideAirOptimization.disName, fontSize = 20, textAlignment = TextAlign.Start)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.enableOutsideAirOptimization,
                        onEnabled = { it ->
                            if (!it && viewModel.isOAODamperAOEnabled()) {
                                showToast("To disable Outside Air Optimization, disable all OAO Damper analog outputs first.", requireContext())
                            } else if (!it && viewModel.isPrePurgeEnabled()) {
                                showToast("To disable Outside Air Optimization, disable Pre-Purge first.", requireContext())
                            } else {
                                viewModel.viewState.value.enableOutsideAirOptimization = it
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.width(375.dp))

                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.prePurge.disName, fontSize = 20, textAlignment = TextAlign.Start)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.prePurge,
                        onEnabled = { it ->
                            if (viewModel.viewState.value.enableOutsideAirOptimization) {
                                viewModel.viewState.value.prePurge = it
                            } else {
                                showToast("To enable Pre-Purge, enable Outside Air Optimization first.", requireContext())
                            }
                        }
                    )
                }
            }

        }
    }

    @Composable
    fun TitleLabel() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Spacer(modifier=Modifier.width(300.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                SubTitle(
                    "ENABLE"
                )
            }
            Spacer(modifier=Modifier.width(0.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                SubTitle(
                    "MAPPING"
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 5.dp), contentAlignment = Alignment.CenterEnd) {
                SubTitle(
                    "TEST SIGNAL"
                )
            }
        }
    }

    @Composable
    fun SensorConfig(viewModel: HyperStatSplitCpuViewModel) {
        val temperatureEnums = viewModel.getAllowedValues(DomainName.temperatureSensorBusAdd0, viewModel.equipModel)
        val humidityEnums = viewModel.getAllowedValues(DomainName.humiditySensorBusAdd0, viewModel.equipModel)
        val pressureEnum = viewModel.getAllowedValues(DomainName.pressureSensorBusAdd0, viewModel.equipModel)

        SubTitle("SENSOR BUS")
        Row(modifier = Modifier.padding(vertical = 10.dp)) {
            Column {
                EnableCompose("Address 0", viewModel.viewState.value.sensorAddress0.enabled) {
                    viewModel.viewState.value.sensorAddress0.enabled = it
                }
                Spacer(modifier = Modifier.height(80.dp))
                EnableCompose("", viewModel.viewState.value.pressureSensorAddress0.enabled, true) {
                    viewModel.viewState.value.pressureSensorAddress0.enabled = it
                }
            }
            Column {
                ConfigCompose(
                    "Temperature",
                    temperatureEnums[viewModel.viewState.value.sensorAddress0.association],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.association = it.index
                }
                HumidityCompose(
                    "Humidity",
                    humidityEnums[viewModel.viewState.value.sensorAddress0.association].dis ?: humidityEnums[viewModel.viewState.value.sensorAddress0.association].value
                )
                Spacer(modifier = Modifier.height(12.dp))
                ConfigCompose(
                    "Pressure",
                    pressureEnum[viewModel.viewState.value.pressureSensorAddress0.association],
                    pressureEnum,
                    "",
                    viewModel.viewState.value.pressureSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.pressureSensorAddress0.association = it.index
                }
            }
        }

        Row(modifier = Modifier.padding(vertical = 25.dp)) {
            EnableCompose("Address 1", viewModel.viewState.value.sensorAddress1.enabled) {
                viewModel.viewState.value.sensorAddress1.enabled = it
            }
            Column {
                ConfigCompose(
                    "Temperature",
                    temperatureEnums[viewModel.viewState.value.sensorAddress1.association],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.association = it.index
                }
                HumidityCompose(
                    "Humidity",
                    humidityEnums[viewModel.viewState.value.sensorAddress1.association].dis ?: humidityEnums[viewModel.viewState.value.sensorAddress1.association].value
                )
            }
        }
        Row(modifier = Modifier.padding(vertical = 10.dp)) {
            EnableCompose("Address 2", viewModel.viewState.value.sensorAddress2.enabled) {
                viewModel.viewState.value.sensorAddress2.enabled = it
            }
            Column {
                ConfigCompose(
                    "Temperature",
                    temperatureEnums[viewModel.viewState.value.sensorAddress2.association],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.association = it.index
                }
                HumidityCompose(
                    "Humidity",
                    humidityEnums[viewModel.viewState.value.sensorAddress2.association].dis ?: humidityEnums[viewModel.viewState.value.sensorAddress2.association].value
                )
            }
        }
    }

    @Composable
    fun EnableCompose(
        sensorAddress: String, default: Boolean, hide: Boolean = false, onEnabled: (Boolean) -> Unit = {}
    ) {
        Row(modifier = Modifier
            .width(355.dp)
            .padding(start = 25.dp)) {
            Box(modifier = Modifier
                .weight(2.5f)
                .align(Alignment.CenterVertically)) {
                StyledTextView(
                    sensorAddress, fontSize = 20
                )
            }
            Box(modifier = Modifier
                .weight(4f)
                .align(Alignment.CenterVertically)) {
                if (!hide) {
                    Image(
                        painter = painterResource(id = R.drawable.map),
                        contentDescription = "Mapping",
                        modifier = Modifier
                            .width(140.dp)
                            .padding(start = 5.dp)
                            .align(Alignment.Center)
                    )
                }
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp)) {
                ToggleButtonStateful(defaultSelection = default) { onEnabled(it) }
            }
        }
    }

    @Composable
    fun ConfigCompose(
        name: String,
        defaultSelection: HyperStatSplitViewModel.Option,
        items: List<HyperStatSplitViewModel.Option>,
        unit: String,
        isEnabled: Boolean,
        onSelect: (HyperStatSplitViewModel.Option) -> Unit = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Box(modifier = Modifier
                .weight(1.3f)
                .padding(start = 22.dp)
                .align(Alignment.CenterVertically)) {
                StyledTextView(name, fontSize = 20)
            }
            Box(modifier = Modifier
                .weight(3f)
                .padding(start = 45.dp)
                .align(Alignment.CenterVertically)) {
                SearchSpinnerElement(
                    default = defaultSelection,
                    allItems = items,
                    enabledItems = items,
                    unit = unit,
                    onSelect = { onSelect(it) },
                    width = 375,
                    expandedWidth = 450,
                    isEnabled = isEnabled
                )
            }
            Box(modifier = Modifier.weight(1f))
        }
    }


    @Composable
    fun HumidityCompose(
        name: String,
        defaultSelection: String
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(top = 30.dp, start = 10.dp, end = 10.dp, bottom = 10.dp)
        ) {
            Box(modifier = Modifier
                .weight(1.3f)
                .padding(start = 22.dp)) {
                StyledTextView(name, fontSize = 20)
            }
            Box(modifier = Modifier
                .weight(3.5f)
                .padding(start = 45.dp, end = 20.dp)) {
                StyledTextView(formatText(defaultSelection), fontSize = 20)
            }
            Box(modifier = Modifier.weight(0.5f))
        }
    }

    @Composable
    fun RelayConfig(viewModel: HyperStatSplitCpuViewModel) {
        val relayEnums = viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.equipModel)

        SubTitle("HS CONNECT")
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Display relay image
            Image(
                painter = painterResource(id = R.drawable.connect_relays),
                contentDescription = "Relays",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 32.dp)
                    .height(508.dp)
            )

            // Display relay configurations
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                repeat(8) { index ->
                    val relayConfig = when (index) {
                        0 -> viewModel.viewState.value.relay1Config
                        1 -> viewModel.viewState.value.relay2Config
                        2 -> viewModel.viewState.value.relay3Config
                        3 -> viewModel.viewState.value.relay4Config
                        4 -> viewModel.viewState.value.relay5Config
                        5 -> viewModel.viewState.value.relay6Config
                        6 -> viewModel.viewState.value.relay7Config
                        7 -> viewModel.viewState.value.relay8Config
                        else -> throw IllegalArgumentException("Invalid relay index: $index")
                    }

                    RelayConfiguration(relayName = "Relay ${index + 1}",
                        enabled = relayConfig.enabled,
                        onEnabledChanged = { enabled -> relayConfig.enabled = enabled },
                        association = relayEnums[relayConfig.association],
                        unit = "",
                        relayEnums = relayEnums,
                        isEnabled = relayConfig.enabled,
                        onAssociationChanged = { associationIndex ->
                            relayConfig.association = associationIndex.index
                        },
                        onTestActivated = { isChecked -> viewModel.handleTestRelayChanged(index, isChecked) }
                    )
                }
            }
        }
    }

    @Composable
    fun RelayConfiguration(
        relayName: String,
        enabled: Boolean,
        onEnabledChanged: (Boolean) -> Unit,
        association: HyperStatSplitViewModel.Option,
        relayEnums: List<HyperStatSplitViewModel.Option>,
        unit: String,
        isEnabled: Boolean,
        onAssociationChanged: (HyperStatSplitViewModel.Option) -> Unit,
        onTestActivated: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(10.dp)
        ) {
            Box(modifier = Modifier.wrapContentWidth()) {
                ToggleButtonStateful(defaultSelection = enabled) { onEnabledChanged(it) }
            }
            Box(modifier = Modifier
                .weight(1.5f)
                .padding(start = 15.dp, top = 8.dp)) {
                StyledTextView(relayName, fontSize = 20)
            }
            Box(modifier = Modifier
                .weight(4f)
                .padding(start = 50.dp)) {
                SearchSpinnerElement(
                    default = association,
                    allItems = relayEnums,
                    enabledItems = relayEnums,
                    unit = unit,
                    onSelect = { onAssociationChanged(it) },
                    width = 350,
                    isEnabled = isEnabled
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 5.dp)
            ) {
                var buttonState by remember { mutableStateOf(false) }
                var text by remember { mutableStateOf("OFF") }
                Button(
                    onClick = {
                        buttonState = !buttonState
                        text = when (buttonState) {
                            true -> "ON"
                            false -> "OFF"
                        }
                        onTestActivated(buttonState)
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        contentColor = when (buttonState) {
                            true -> primaryColor
                            false -> Color.Black
                        }, containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, color = when (buttonState) {
                        true -> primaryColor
                        false -> Color.Black
                    }),
                ) {
                    Text(
                        text = when (buttonState) {
                            true -> "ON"
                            false -> "OFF"
                        }
                    )
                }
            }
        }
    }

    @Composable
    fun AnalogOutConfig(viewModel: HyperStatSplitCpuViewModel) {
        val analogOutEnums = viewModel.getAllowedValues(DomainName.analog1OutputAssociation, viewModel.equipModel)
        val enabledEnums = if (viewModel.viewState.value.enableOutsideAirOptimization) {
            analogOutEnums
        } else {
            analogOutEnums.filter { it.value != DomainName.oaoDamper }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Display analog out image
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.connect_ao),
                    contentDescription = "Analog Out",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                        .height(280.dp)
                )
            }

            // Display analog out configurations
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                repeat(4) { index ->
                    val analogOut = "Analog-out${index + 1}"
                    val enabled = when (index) {
                        0 -> viewModel.viewState.value.analogOut1Enabled
                        1 -> viewModel.viewState.value.analogOut2Enabled
                        2 -> viewModel.viewState.value.analogOut3Enabled
                        3 -> viewModel.viewState.value.analogOut4Enabled
                        else -> false
                    }
                    val associationIndex = when (index) {
                        0 -> viewModel.viewState.value.analogOut1Association
                        1 -> viewModel.viewState.value.analogOut2Association
                        2 -> viewModel.viewState.value.analogOut3Association
                        3 -> viewModel.viewState.value.analogOut4Association
                        else -> 0
                    }

                    AnalogOutConfiguration(
                        viewModel = viewModel,
                        analogOutName = analogOut,
                        enabled = enabled,
                        onEnabledChanged = { enabledAction ->
                            when (index) {
                                0 -> viewModel.viewState.value.analogOut1Enabled = enabledAction
                                1 -> viewModel.viewState.value.analogOut2Enabled = enabledAction
                                2 -> viewModel.viewState.value.analogOut3Enabled = enabledAction
                                3 -> viewModel.viewState.value.analogOut4Enabled = enabledAction
                            }
                        },
                        association = analogOutEnums[associationIndex],
                        analogOutEnums = analogOutEnums,
                        enabledEnums = enabledEnums,
                        testSingles = viewModel.testVoltage,
                        isEnabled = enabled,
                        onAssociationChanged = { association ->
                            when (index) {
                                0 -> viewModel.viewState.value.analogOut1Association =
                                    association.index

                                1 -> viewModel.viewState.value.analogOut2Association =
                                    association.index

                                2 -> viewModel.viewState.value.analogOut3Association =
                                    association.index

                                3 -> viewModel.viewState.value.analogOut4Association =
                                    association.index
                            }
                        },
                        onTestSignalSelected = { value -> viewModel.handleTestAnalogOutChanged(index, value) })
                }
            }
        }
    }

    @Composable
    fun AnalogOutConfiguration(
        viewModel: HyperStatSplitCpuViewModel,
        analogOutName: String,
        enabled: Boolean,
        onEnabledChanged: (Boolean) -> Unit = {},
        association: HyperStatSplitViewModel.Option,
        analogOutEnums: List<HyperStatSplitViewModel.Option>,
        enabledEnums: List<HyperStatSplitViewModel.Option>,
        testSingles: List<HyperStatSplitViewModel.Option>,
        unit: String = "",
        isEnabled: Boolean,
        onAssociationChanged: (HyperStatSplitViewModel.Option) -> Unit = {},
        onTestSignalSelected: (Double) -> Unit = {}
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(10.dp)
        ) {
            Box(modifier = Modifier
                .wrapContentWidth()) {
                ToggleButton(
                    defaultSelection = enabled,
                    modifier = Modifier.wrapContentSize()
                ) {
                    if (!it || viewModel.viewState.value.enableOutsideAirOptimization || !(
                            (analogOutName.contains("1") && viewModel.viewState.value.analogOut1Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.OAO_DAMPER.ordinal) ||
                            (analogOutName.contains("2") && viewModel.viewState.value.analogOut2Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.OAO_DAMPER.ordinal) ||
                            (analogOutName.contains("3") && viewModel.viewState.value.analogOut3Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.OAO_DAMPER.ordinal) ||
                            (analogOutName.contains("4") && viewModel.viewState.value.analogOut4Association == HyperstatSplitReconfigurationHandler.Companion.CpuControlType.OAO_DAMPER.ordinal)
                        )
                    ) {
                        onEnabledChanged(it)
                    } else {
                        showToast("To enable Analog Out, enable Outside Air Optimization first.", requireContext())
                    }
                }
            }
            Box(modifier = Modifier
                .weight(1.5f)
                .padding(start = 15.dp, top = 8.dp)) {
                StyledTextView(analogOutName, fontSize = 20)
            }
            Box(modifier = Modifier
                .weight(4f)
                .padding(start = 50.dp, end = 5.dp)) {
                SearchSpinnerElement(
                    default = association,
                    allItems = analogOutEnums,
                    enabledItems = enabledEnums,
                    unit = unit,
                    onSelect = { onAssociationChanged(it) },
                    width = 350,
                    isEnabled = isEnabled
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            ) {
                SpinnerElementOption(
                    defaultSelection = "0",
                    items = testSingles,
                    unit = unit,
                    itemSelected = {onTestSignalSelected(it.value.toDouble()) }
                )
            }
        }
    }

    @Composable
    fun SpinnerElementOption(
        defaultSelection: String,
        items: List<HyperStatSplitViewModel.Option>,
        unit: String,
        itemSelected: (HyperStatSplitViewModel.Option) -> Unit,
        previewWidth : Int = 130
    ) {
        val selectedItem = remember { mutableStateOf(defaultSelection) }
        val expanded = remember { mutableStateOf(false) }
        val lazyListState = rememberLazyListState()
        var selectedIndex by remember { mutableStateOf(getDefaultSelectionIndex(items, defaultSelection))}
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(
                    PaddingValues(
                        top = 5.dp
                    )
                )
        ) {
            Column(modifier = Modifier
                .width(160.dp)
                .clickable(onClick = { expanded.value = true })
            ) {
                Row {
                    Text(
                        fontSize = 20.sp,
                        modifier = Modifier.width(110.dp),
                        fontWeight = FontWeight.Normal,
                        text = "${selectedItem.value} $unit",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp)),
                        colorFilter = ColorFilter.tint(primaryColor),
                        alignment = Alignment.CenterEnd
                    )
                }
                Divider(modifier = Modifier.width((previewWidth + 30).dp), color = ComposeUtil.greyDropDownUnderlineColor)
            }
            val customHeight = getDropdownCustomHeight(items, noOfItemsDisplayInDropDown, dropDownHeight)
            DropdownMenu(
                modifier = Modifier
                    .background(Color.White)
                    .width((previewWidth + 30).dp)
                    .height(customHeight.dp)
                    .simpleVerticalScrollbar(lazyListState),
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .height((customHeight).dp)
                        .width((previewWidth + 30).dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        DropdownMenuItem(
                            modifier = Modifier.background(if (index == selectedIndex) ComposeUtil.secondaryColor else Color.White),
                            contentPadding = PaddingValues(10.dp),
                            text = {
                                Row {
                                    Text(
                                        fontSize = 20.sp,
                                        fontFamily = ComposeUtil.myFontFamily,
                                        modifier = Modifier.padding(end = 10.dp),
                                        fontWeight = FontWeight.Normal,
                                        text = item.value
                                    )
                                    Text(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Normal,
                                        text = unit
                                    )
                                }
                            }, onClick = {
                                selectedItem.value = item.value
                                selectedIndex = index
                                expanded.value = false
                                itemSelected(item)
                            })
                    }
                }
                LaunchedEffect(expanded) {
                    lazyListState.scrollToItem(selectedIndex)
                }
            }
        }
    }

    @Composable
    fun SpinnerElementString(
        defaultSelection: String,
        items: List<String>,
        unit: String,
        itemSelected: (String) -> Unit
    ) {
        val selectedItem = remember { mutableStateOf(defaultSelection) }
        val lazyListState = rememberLazyListState()
        var selectedIndex by remember { mutableStateOf(items.indexOf(defaultSelection)) }
        var expanded = remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(
                    PaddingValues(
                        top = 5.dp
                    )
                )
        ) {
            Column(modifier = Modifier
                .width(160.dp)
                .clickable(onClick = { expanded.value = true })) {
                Row {
                    Text(
                        fontSize = 20.sp,
                        modifier = Modifier.width(110.dp),
                        fontWeight = FontWeight.Normal,
                        text = "${selectedItem.value} $unit"
                    )

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp)),
                        colorFilter = ColorFilter.tint(primaryColor),
                        alignment = Alignment.CenterEnd
                    )
                }
                Row { Divider(modifier = Modifier.width(160.dp)) }
            }
            val customHeight = getDropdownCustomHeight(items, noOfItemsDisplayInDropDown, dropDownHeight)
            DropdownMenu(
                modifier = Modifier
                    .background(Color.White)
                    .width(160.dp)
                    .height(customHeight.dp)
                    .simpleVerticalScrollbar(lazyListState),
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }) {
                LazyColumn(state = lazyListState,
                    modifier = Modifier
                        .height((customHeight).dp)
                        .width(200.dp)) {
                    itemsIndexed(items) {index, item ->
                        DropdownMenuItem(
                            modifier = Modifier.background(if (item == selectedItem.value) ComposeUtil.secondaryColor else Color.White),
                            text = {
                                Row {
                                    Text(
                                        fontSize = 20.sp,
                                        modifier = Modifier.padding(end = 10.dp),
                                        fontWeight = FontWeight.Normal,
                                        text = item
                                    )
                                    Text(
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Normal,
                                        text = unit
                                    )
                                }
                            }, onClick = {
                                selectedItem.value = item
                                selectedIndex = index
                                expanded.value = false
                                itemSelected(item)
                            })
                    }
                }
                LaunchedEffect(expanded) {
                    lazyListState.scrollToItem(selectedIndex)
                }
            }
        }
    }

    fun getDefaultSelectionIndex(items: List<HyperStatSplitViewModel.Option>, defaultSelection: String):Int {
        var selectedIndex = 0
        if (items.isEmpty()) {
            return -1
        }

        val isDefaultSelectionIsNumber = defaultSelection.toDoubleOrNull() != null
        var selectedItem = defaultSelection
        if (isDefaultSelectionIsNumber && items[0].value.contains('.')) {
            selectedItem = String.format("%.2f", defaultSelection.toDouble())
        }

        for (i in items.indices) {
            if (items[i].value == selectedItem) {
                selectedIndex = i
                break
            }
        }
        return selectedIndex
    }

    @Composable
    fun UniversalInConfig(viewModel: HyperStatSplitCpuViewModel) {
        val universalEnum = viewModel.getAllowedValues(DomainName.universalIn1Association, viewModel.equipModel)

        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.connect_ui),
                contentDescription = "Universal Inputs",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 25.dp, bottom = 25.dp)
                    .height(550.dp)
            )
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                AOTHConfig(UNIVERSAL_IN1,
                    viewModel.viewState.value.universalIn1Config.enabled,
                    { viewModel.viewState.value.universalIn1Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn1Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn1Config.enabled,
                    { viewModel.viewState.value.universalIn1Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN2,
                    viewModel.viewState.value.universalIn2Config.enabled,
                    { viewModel.viewState.value.universalIn2Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn2Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn2Config.enabled,
                    { viewModel.viewState.value.universalIn2Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN3,
                    viewModel.viewState.value.universalIn3Config.enabled,
                    { viewModel.viewState.value.universalIn3Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn3Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn3Config.enabled,
                    { viewModel.viewState.value.universalIn3Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN4,
                    viewModel.viewState.value.universalIn4Config.enabled,
                    { viewModel.viewState.value.universalIn4Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn4Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn4Config.enabled,
                    { viewModel.viewState.value.universalIn4Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN5,
                    viewModel.viewState.value.universalIn5Config.enabled,
                    { viewModel.viewState.value.universalIn5Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn5Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn5Config.enabled,
                    { viewModel.viewState.value.universalIn5Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN6,
                    viewModel.viewState.value.universalIn6Config.enabled,
                    { viewModel.viewState.value.universalIn6Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn6Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn6Config.enabled,
                    { viewModel.viewState.value.universalIn6Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN7,
                    viewModel.viewState.value.universalIn7Config.enabled,
                    { viewModel.viewState.value.universalIn7Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn7Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn7Config.enabled,
                    { viewModel.viewState.value.universalIn7Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN8,
                    viewModel.viewState.value.universalIn8Config.enabled,
                    { viewModel.viewState.value.universalIn8Config.enabled = it },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn8Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn8Config.enabled,
                    { viewModel.viewState.value.universalIn8Config.association = it.index })
            }
        }
    }

    val UNIVERSAL_IN1 = "Universal-In 1"
    val UNIVERSAL_IN2 = "Universal-In 2"
    val UNIVERSAL_IN3 = "Universal-In 3"
    val UNIVERSAL_IN4 = "Universal-In 4"
    val UNIVERSAL_IN5 = "Universal-In 5"
    val UNIVERSAL_IN6 = "Universal-In 6"
    val UNIVERSAL_IN7 = "Universal-In 7"
    val UNIVERSAL_IN8 = "Universal-In 8"

    @Composable
    fun AOTHConfig(
        name: String,
        default: Boolean,
        onEnabled: (Boolean) -> Unit = {},
        spinnerDefault: HyperStatSplitViewModel.Option,
        items: List<HyperStatSplitViewModel.Option>,
        unit: String = "",
        isEnabled: Boolean,
        onSelect: (HyperStatSplitViewModel.Option) -> Unit
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(10.dp)
        ) {
            Box(modifier = Modifier.wrapContentWidth()) {
                ToggleButtonStateful(defaultSelection = default) { onEnabled(it) }
            }
            Box(modifier = Modifier
                .weight(1.5f)
                .padding(start = 15.dp, top = 8.dp)) {
                StyledTextView(name, fontSize = 20)
            }
            Box(modifier = Modifier
                .weight(4f)
                .padding(start = 16.dp)) {
                SearchSpinnerElement(
                    default = spinnerDefault,
                    allItems = items,
                    enabledItems = items,
                    unit = unit,
                    onSelect = { onSelect(it) },
                    width = 375,
                    expandedWidth = 490,
                    isEnabled = isEnabled
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchSpinnerElement(
        default: HyperStatSplitViewModel.Option,
        allItems: List<HyperStatSplitViewModel.Option>,
        enabledItems: List<HyperStatSplitViewModel.Option>,
        unit: String,
        onSelect: (HyperStatSplitViewModel.Option) -> Unit,
        width: Int,
        expandedWidth: Int = width,
        isEnabled: Boolean = true
    ) {
        val selectedItem = remember { mutableStateOf(default) }
        val expanded = remember { mutableStateOf(false) }
        var searchedOption by rememberSaveable { mutableStateOf("") }
        val lazyListState = rememberLazyListState()
        var selectedIndex by remember { mutableStateOf(default.index) }
        Box(
            modifier = Modifier
                .wrapContentSize()
                .padding(
                    PaddingValues(
                        top = 5.dp
                    )
                )
        ) {
            Column(modifier = Modifier
                .width(width.dp)
                .clickable(onClick = { expanded.value = true }, enabled = isEnabled)) {
                Row {
                    Text(
                        fontSize = 20.sp,
                        fontFamily = ComposeUtil.myFontFamily,
                        modifier = Modifier.width((width-50).dp),
                        fontWeight = FontWeight.Normal,
                        text = "${ selectedItem.value.dis?: selectedItem.value.dis } $unit",
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp)),
                        colorFilter = ColorFilter.tint(if(isEnabled) primaryColor else greyDropDownColor)
                    )
                }
                Divider(modifier = Modifier.width((width-20).dp),color = if(expanded.value) Color.Black else ComposeUtil.greyDropDownUnderlineColor)
            }

            DropdownMenu(
                modifier = Modifier
                    .background(Color.White).width((expandedWidth).dp),
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }) {
                Column(
                    verticalArrangement = Arrangement.Bottom
                ) {

                    var searchText by remember { mutableStateOf("") }
                    val filteredItems = if (searchText.isEmpty()) {
                        allItems
                    } else {
                        allItems.filter { it.value.contains(searchText.replace(" ",""), ignoreCase = true) }
                    }
                    if (allItems.size > 16) {
                        Row {
                            TextField(
                                value = searchText,
                                onValueChange = { searchText = it },
                                placeholder = {
                                    Text(fontSize = 20.sp, text = "Search")
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.textFieldColors(
                                    focusedIndicatorColor = primaryColor,
                                    unfocusedIndicatorColor = ComposeUtil.greyColor,
                                    containerColor = Color.White
                                ),
                                modifier = Modifier
                                    .padding(10.dp)
                                    .width((expandedWidth).dp),
                                textStyle = TextStyle(fontSize = 20.sp, fontFamily = ComposeUtil.myFontFamily),
                                leadingIcon = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_search),
                                        contentDescription = "Custom Icon",
                                        modifier = Modifier.size(24.dp),
                                        colorFilter = ColorFilter.tint(ComposeUtil.greySearchIcon)
                                    )
                                },
                                trailingIcon = {
                                    if (searchText.isNotEmpty()) {
                                        Image(
                                            painter = painterResource(id = R.drawable.font_awesome_close),
                                            contentDescription = "Clear Icon",
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clickable { searchText = "" },
                                            colorFilter = ColorFilter.tint(primaryColor)
                                        )
                                    }
                                },
                            )
                        }
                    }
                    val customHeight = getDropdownCustomHeight(filteredItems, noOfItemsDisplayInDropDown, dropDownHeight)
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .height((customHeight).dp)
                            .width(expandedWidth.dp)
                            .simpleVerticalScrollbar(lazyListState)
                    ) {
                        items(filteredItems) {
                            DropdownMenuItem(
                                modifier = Modifier.background(if (it.value == selectedItem.value.value) ComposeUtil.secondaryColor else Color.White),
                                contentPadding = PaddingValues(10.dp),
                                text = {
                                    Row {
                                        Text(
                                            fontSize = 20.sp,
                                            color = if (enabledItems.contains(it)) Color.Black else Color.Gray,
                                            fontFamily = ComposeUtil.myFontFamily,
                                            modifier = Modifier.padding(end = 10.dp, start = 10.dp),
                                            fontWeight = FontWeight.Normal,
                                            text = it.dis ?: it.value
                                        )
                                        Text(
                                            fontSize = 20.sp,
                                            color = if (enabledItems.contains(it)) Color.Black else Color.Gray,
                                            fontFamily = ComposeUtil.myFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            text = unit
                                        )
                                    }
                                }, onClick = {
                                    if (enabledItems.contains(it)) {
                                        selectedItem.value = it
                                        expanded.value = false
                                        selectedIndex = allItems.indexOf(it)
                                        searchedOption = ""
                                        onSelect(it)
                                    }
                                })
                        }
                    }
                    LaunchedEffect(expanded) {
                        lazyListState.scrollToItem(allItems.indexOf(selectedItem.value))
                    }
                }
            }
        }
    }

    fun formatText(input: String): String {
        val regex = "(?<=[a-zA-Z])(?=\\d)|(?<=\\d)(?=[a-zA-Z])|(?<=[a-z\\d])(?=[A-Z])".toRegex()
        return input.split(regex).joinToString(" ").replaceFirstChar { it.uppercase() }
    }

    @Composable
    fun CoolingControl(viewModel: HyperStatSplitCpuViewModel) {
        Column() {
            if (viewModel.isCoolingAOEnabled()) {
                EnableCoolingVoltage(viewModel, HyperstatSplitReconfigurationHandler.Companion.CpuControlType.COOLING)
            }
        }
    }

    @Composable
    fun EnableCoolingVoltage(viewModel: HyperStatSplitCpuViewModel, type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nCooling",
                "Analog-out1 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nCooling",
                "Analog-out2 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.coolingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nCooling",
                "Analog-out3 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nCooling",
                "Analog-out4 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun HeatingControl(viewModel: HyperStatSplitCpuViewModel) {
        Column() {
            if (viewModel.isHeatingAOEnabled()) {
                EnableHeatingVoltage(viewModel, HyperstatSplitReconfigurationHandler.Companion.CpuControlType.HEATING)
            }
        }
    }

    @Composable
    fun EnableHeatingVoltage(viewModel: HyperStatSplitCpuViewModel, type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nHeating",
                "Analog-out1 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nHeating",
                "Analog-out2 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nHeating",
                "Analog-out3 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nHeating",
                "Analog-out4 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun LinearFanControl(viewModel: HyperStatSplitCpuViewModel) {
        Column() {
            if (viewModel.isLinearFanAOEnabled()) {
                EnableLinearFanVoltage(viewModel, HyperstatSplitReconfigurationHandler.Companion.CpuControlType.LINEAR_FAN)
            }
        }
    }

    @Composable
    fun EnableLinearFanVoltage(viewModel: HyperStatSplitCpuViewModel, type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nLinear Fan",
                "Analog-out1 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nLinear Fan",
                "Analog-out2 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nLinear Fan",
                "Analog-out3 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nLinear Fan",
                "Analog-out4 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun StagedFanControl(viewModel: HyperStatSplitCpuViewModel) {
        Column() {
            if (viewModel.isStagedFanAOEnabled()) {
                EnableStagedFanVoltage(viewModel, HyperstatSplitReconfigurationHandler.Companion.CpuControlType.STAGED_FAN)
            }
        }
    }

    @Composable
    fun EnableStagedFanVoltage(viewModel:HyperStatSplitCpuViewModel, type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType) {
        if (viewModel.isStagedFanAOEnabled()) {
            CpuStagedFanConfiguration("Fan Out\nduring Recirc",
                "Fan Out\nduring Economizer",
                "Fan Out\nduring Heat Stage 1",
                "Fan Out\nduring Heat Stage 2",
                "Fan Out\nduring Heat Stage 3",
                "Fan Out\nduring Cool Stage 1",
                "Fan Out\nduring Cool Stage 2",
                "Fan Out\nduring Cool Stage 3",
                viewModel.minMaxVoltage,
                "V",
                recircDefault = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.recircVoltage.toString(),
                economizerDefault = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.economizerVoltage.toString(),
                heatStage1Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.heatStage1Voltage.toString(),
                heatStage2Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.heatStage2Voltage.toString(),
                heatStage3Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.heatStage3Voltage.toString(),
                coolStage1Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.coolStage1Voltage.toString(),
                coolStage2Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.coolStage2Voltage.toString(),
                coolStage3Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.coolStage3Voltage.toString(),
                onRecircSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.recircVoltage = it.value.toInt()
                },
                onEconomizerSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.economizerVoltage = it.value.toInt()
                },
                onHeatStage1Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.heatStage1Voltage = it.value.toInt()
                },
                onHeatStage2Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.heatStage2Voltage = it.value.toInt()
                },
                onHeatStage3Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.heatStage3Voltage = it.value.toInt()
                },
                onCoolStage1Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.coolStage1Voltage = it.value.toInt()
                },
                onCoolStage2Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.coolStage2Voltage = it.value.toInt()
                },
                onCoolStage3Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.coolStage3Voltage = it.value.toInt()
                },
                (viewModel as HyperStatSplitCpuViewModel)
            )
        }
    }

    @Composable
    fun OAODamperControl(viewModel: HyperStatSplitCpuViewModel) {
        Column() {
            if (viewModel.isOAODamperAOEnabled()) {
                EnableOAODamperVoltage(viewModel, HyperstatSplitReconfigurationHandler.Companion.CpuControlType.OAO_DAMPER)
            }
        }
    }

    @Composable
    fun EnableOAODamperVoltage(viewModel:HyperStatSplitCpuViewModel, type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nOAO Damper",
                "Analog-out1 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nOAO Damper",
                "Analog-out2 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nOAO Damper",
                "Analog-out3 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nOAO Damper",
                "Analog-out4 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun ReturnDamperControl(viewModel: HyperStatSplitCpuViewModel) {
        Column() {
            if (viewModel.isReturnDamperAOEnabled()) {
                EnableReturnDamperVoltage(viewModel, HyperstatSplitReconfigurationHandler.Companion.CpuControlType.RETURN_DAMPER)
            }
        }
    }

    @Composable
    fun EnableReturnDamperVoltage(viewModel: HyperStatSplitCpuViewModel, type: HyperstatSplitReconfigurationHandler.Companion.CpuControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nReturn Damper",
                "Analog-out1 at Max \nReturn Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.returnDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.returnDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.returnDamperMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.returnDamperMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nReturn Damper",
                "Analog-out2 at Max \nReturn Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.returnDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.returnDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.returnDamperMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.returnDamperMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nReturn Damper",
                "Analog-out3 at Max \nReturn Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.returnDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.returnDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.returnDamperMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.returnDamperMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nReturn Damper",
                "Analog-out4 at Max \nReturn Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.returnDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.returnDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.returnDamperMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.returnDamperMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun MinMaxConfiguration(
        minLabel: String,
        maxLabel: String,
        itemList: List<HyperStatSplitViewModel.Option>,
        unit: String,
        minDefault: String,
        maxDefault: String,
        onMinSelected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onMaxSelected: (HyperStatSplitViewModel.Option) -> Unit = {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp)
        ) {
            Box(modifier = Modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 10.dp)) {
                StyledTextView(
                    minLabel, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SpinnerElementOption(defaultSelection = minDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onMinSelected(it) })
            }

            Box(modifier = Modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 10.dp)) {
                StyledTextView(
                    maxLabel, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SpinnerElementOption(defaultSelection = maxDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onMaxSelected(it) })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun CpuStagedFanConfiguration(
        recircLabel: String,
        economizerLabel: String,
        heatStage1Label: String,
        heatStage2Label: String,
        heatStage3Label: String,
        coolStage1Label: String,
        coolStage2Label: String,
        coolStage3Label: String,
        itemList: List<HyperStatSplitViewModel.Option>,
        unit: String,
        recircDefault: String,
        economizerDefault: String,
        heatStage1Default: String,
        heatStage2Default: String,
        heatStage3Default: String,
        coolStage1Default: String,
        coolStage2Default: String,
        coolStage3Default: String,
        onRecircSelected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onEconomizerSelected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onHeatStage1Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onHeatStage2Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onHeatStage3Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCoolStage1Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCoolStage2Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCoolStage3Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        viewModel: HyperStatSplitCpuViewModel
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp),
            maxItemsInEachRow = 4
        ) {
            var nEntries = 1
            Box(modifier = Modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 10.dp)) {
                StyledTextView(
                    recircLabel, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SpinnerElementOption(defaultSelection = recircDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onRecircSelected(it) })
            }

            if (viewModel.isOAODamperAOEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        economizerLabel, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = economizerDefault,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onEconomizerSelected(it) })
                }
            }

            if (viewModel.isHeatStage1RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        heatStage1Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = heatStage1Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onHeatStage1Selected(it) })
                }
            }
            
            if (viewModel.isHeatStage2RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        heatStage2Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = heatStage2Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onHeatStage2Selected(it) })
                }
            }
            
            if (viewModel.isHeatStage3RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        heatStage3Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = heatStage3Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onHeatStage3Selected(it) })
                }
            }
            
            if (viewModel.isCoolStage1RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        coolStage1Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = coolStage1Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onCoolStage1Selected(it) })
                }
            }
            
            if (viewModel.isCoolStage2RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        coolStage2Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = coolStage2Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onCoolStage2Selected(it) })
                }
            }
            
            if (viewModel.isCoolStage3RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        coolStage3Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = coolStage3Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onCoolStage3Selected(it) })
                }
            }

            if (nEntries % 2 == 1) {
                Box(modifier = Modifier.weight(2f))
                Box(modifier = Modifier.weight(1f))
            }

        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    fun ZoneOAOConfig(viewModel: HyperStatSplitCpuViewModel) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, bottom = 10.dp),
            maxItemsInEachRow = 4
        ) {
            var nEntries = 0

            if (viewModel.isAO1MappedToFan()) {
                nEntries += 3
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out1 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanLow = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out1 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanMedium = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out1 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanHigh = it.value.toInt() }
                    )
                }
            }

            if (viewModel.isAO2MappedToFan()) {
                nEntries += 3
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out2 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanLow = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out2 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanMedium = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out2 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanHigh = it.value.toInt() }
                    )
                }
            }

            if (viewModel.isAO3MappedToFan()) {
                nEntries += 3
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out3 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanLow = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out3 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanMedium = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out3 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanHigh = it.value.toInt() }
                    )
                }
            }

            if (viewModel.isAO4MappedToFan()) {
                nEntries += 3
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out4 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanLow = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out4 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanMedium = it.value.toInt() }
                    )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Analog-out4 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = { (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanHigh = it.value.toInt() }
                    )
                }
            }

            if (viewModel.viewState.value.enableOutsideAirOptimization) {
                nEntries += 9
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Recirc", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringRecirc.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringRecirc = it.toDouble()})
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Conditioning", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringConditioning.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringConditioning = it.toDouble()})
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Fan Low", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanLow.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringFanLow = it.toDouble()})
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Fan Medium", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanMedium.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringFanMedium = it.toDouble() })
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Fan High", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanHigh.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringFanHigh = it.toDouble()})
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Exhaust Fan Stage 1 Threshold\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.exhaustFanStage1Threshold.toInt().toString(),
                        items = viewModel.exhaustFanThresholdList, unit = "%",
                        itemSelected = { viewModel.viewState.value.exhaustFanStage1Threshold = it.toDouble()})
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Exhaust Fan Stage 2 Threshold\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.exhaustFanStage2Threshold.toInt().toString(),
                        items = viewModel.exhaustFanThresholdList, unit = "%",
                        itemSelected = { viewModel.viewState.value.exhaustFanStage2Threshold = it.toDouble()})
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "Exhaust Fan Hysteresis\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.exhaustFanHysteresis.toInt().toString(),
                        items = viewModel.exhaustFanHysteresisList, unit = "%",
                        itemSelected = { viewModel.viewState.value.exhaustFanHysteresis = it.toDouble()})
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        "CO2 Damper Opening Rate\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.zoneCO2DamperOpeningRate.toInt().toString(),
                        items = viewModel.zoneCO2DamperOpeningRateList, unit = "%",
                        itemSelected = { viewModel.viewState.value.zoneCO2DamperOpeningRate = it.toDouble() })
                }

                if (viewModel.isPrePurgeEnabled()) {
                    nEntries += 1
                    Box(modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)) {
                        StyledTextView(
                            "Pre Purge Min\nDamper Position", fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SpinnerElementString(defaultSelection = viewModel.viewState.value.prePurgeOutsideDamperOpen.toInt().toString(),
                            items = viewModel.prePurgeOutsideDamperOpenList, unit = "%",
                            itemSelected = { viewModel.viewState.value.prePurgeOutsideDamperOpen = it.toDouble() })
                    }
                }
            }

            nEntries += 5
            Box(modifier = Modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 10.dp)) {
                StyledTextView(
                    "Zone CO2 Threshold\n", fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 25.dp)) {
                SpinnerElementString(defaultSelection = viewModel.viewState.value.zoneCO2Threshold.toInt().toString(),
                    items = viewModel.zoneCO2ThresholdList, unit = "ppm",
                    itemSelected = { viewModel.viewState.value.zoneCO2Threshold = it.toDouble() })
            }

            Box(modifier = Modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 10.dp)) {
                StyledTextView(
                    "Zone CO2 Target\n", fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 25.dp)) {
                SpinnerElementString(defaultSelection = viewModel.viewState.value.zoneCO2Target.toInt().toString(),
                    items = viewModel.zoneCO2TargetList, unit = "ppm",
                    itemSelected = { viewModel.viewState.value.zoneCO2Target = it.toDouble() })
            }

            Box(modifier = Modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 10.dp)) {
                StyledTextView(
                    "Zone PM2.5 Target\n", fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 25.dp)) {
                SpinnerElementString(defaultSelection = viewModel.viewState.value.zonePM2p5Target.toInt().toString(),
                    items = viewModel.zonePM2p5TargetList, unit = "ug/m3",
                    itemSelected = { viewModel.viewState.value.zonePM2p5Target = it.toDouble() })
            }

            if (nEntries % 2 == 1) {
                Box(modifier = Modifier.weight(2f))
                Box(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    fun DisplayInDeviceConfig(viewModel: HyperStatSplitCpuViewModel) {
        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            BoldStyledTextView("Display in Device Home Screen", fontSize = 20)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)) {
                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.displayHumidity.disName, fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.displayHumidity,
                        onEnabled = { it ->
                            if (it && viewModel.getDeviceDisplayCount() > 1) {
                                showToast("Max of 2 parameters can be displayed at once", requireContext())
                            } else {
                                viewModel.viewState.value.displayHumidity = it
                            }
                        }
                    )
                }

                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.displayCO2.disName, fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.displayCO2,
                        onEnabled = { it ->
                            if (it && viewModel.getDeviceDisplayCount() > 1) {
                                showToast("Max of 2 parameters can be displayed at once", requireContext())
                            } else {
                                viewModel.viewState.value.displayCO2 = it
                            }
                        }
                    )
                }
            }

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)) {
                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.displayPM2p5.disName, fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.displayPM2p5,
                        onEnabled = { it ->
                            if (it && viewModel.getDeviceDisplayCount() > 1) {
                                showToast("Max of 2 parameters can be displayed at once", requireContext())
                            } else {
                                viewModel.viewState.value.displayPM2p5 = it
                            }
                        }
                    )
                }

                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                }
                Box(modifier = Modifier.weight(1f)) {
                }
            }
        }
    }

    @Composable
    fun SaveConfig(viewModel: HyperStatSplitCpuViewModel) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 20.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 5.dp)),
                contentAlignment = Alignment.Center
            ) { if (viewModel.hasUnsavedChanges()) { SaveTextView(CANCEL) { viewModel.openCancelDialog = true } } }
            if (viewModel.hasUnsavedChanges()) {
                Divider(
                    modifier = Modifier
                        .height(25.dp)
                        .width(2.dp)
                        .padding(bottom = 6.dp),
                    color = Color.LightGray
                )
            }
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.Center
            ) {
                SaveTextView(SAVE) {
                    CcuLog.i(
                        L.TAG_CCU_SYSTEM, viewModel.viewState.toString()
                    )
                    viewModel.saveConfiguration()
                }
            }
        }
    }

    private fun getDropdownCustomHeight(list: List<Any>, noOfItemsDisplayInDropDown: Int, heightValue: Int): Int {
        var customHeight = heightValue
        if(list.isNotEmpty()) {
            if(list.size <= noOfItemsDisplayInDropDown) {
                customHeight = (list.size * 48) // 48 is the height and padding for each dropdown item and 5 is the padding
            }
            return customHeight
        }
        return 0
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1265
            val height = 672
            dialog.window!!.setLayout(width, height)
        }
    }

}