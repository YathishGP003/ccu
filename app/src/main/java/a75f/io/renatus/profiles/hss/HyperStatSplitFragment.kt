package a75f.io.renatus.profiles.hss

import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.SUPPLY_WATER_TEMPERATURE
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.R
import a75f.io.renatus.composables.PinPassword
import a75f.io.renatus.composables.TempOffsetPicker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.BoldStyledTextView
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.LabelWithToggleButton
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.compose.dropDownHeight
import a75f.io.renatus.compose.noOfItemsDisplayInDropDown
import a75f.io.renatus.compose.simpleVerticalScrollbar
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.hss.cpu.HyperStatSplitCpuState
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.Pipe2UvViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.Pipe4UvViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe2UvViewState
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe4UvViewState
import a75f.io.renatus.profiles.system.UNIVERSAL_IN1
import a75f.io.renatus.profiles.system.UNIVERSAL_IN2
import a75f.io.renatus.profiles.system.UNIVERSAL_IN3
import a75f.io.renatus.profiles.system.UNIVERSAL_IN4
import a75f.io.renatus.profiles.system.UNIVERSAL_IN5
import a75f.io.renatus.profiles.system.UNIVERSAL_IN6
import a75f.io.renatus.profiles.system.UNIVERSAL_IN7
import a75f.io.renatus.profiles.system.UNIVERSAL_IN8
import a75f.io.renatus.util.TestSignalManager
import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.projecthaystack.util.Base64

open class HyperStatSplitFragment : BaseDialogFragment() {
    companion object {
        const val INSTALLER_ACCESS = "Installer Access"
        const val CONDITIONING_ACCESS = "Conditioning Mode & Fan Access"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Globals.getInstance().isTestMode) {
            Globals.getInstance().isTestMode = false
            TestSignalManager.restoreAllPoints()
        }
    }


    @Composable
    fun Title(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (viewModel.profileType) {
                ProfileType.HYPERSTATSPLIT_CPU -> TitleTextView(stringResource(R.string.title_cpu_and_economizer))
                ProfileType.HYPERSTATSPLIT_4PIPE_UV -> TitleTextView(stringResource(R.string.title_4_pipe_and_economizer))
                ProfileType.HYPERSTATSPLIT_2PIPE_UV -> TitleTextView(stringResource(R.string.title_2_pipe_and_economizer))
                else -> TitleTextView(stringResource(R.string.title_cpu_and_economizer))
            }
        }
    }

    @Composable
    fun ShowProgressBar() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = stringResource(R.string.loading_profile_configuration))
        }
    }

    override fun getIdString() : String { return "" }

    @Composable
    fun TempOffset(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        val valuesPickerState = rememberPickerState()
        Column(modifier = modifier.padding(start = 400.dp, end = 400.dp)) {
            TempOffsetPicker(
                header = stringResource(R.string.temperature_offset_caps),
                state = valuesPickerState,
                items = viewModel.temperatureOffsetsList,
                onChanged = { it: String -> viewModel.viewState.value.temperatureOffset = it.toDouble() },
                startIndex = viewModel.temperatureOffsetsList.indexOf(viewModel.viewState.value.temperatureOffset.toString()),
                visibleItemsCount = 3,
                textModifier = Modifier.padding(8.dp),
                textStyle = TextStyle(fontSize = 18.sp),
                labelWidth = 280
            )
        }
    }

    @Composable
    fun AutoAwayConfig(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier.padding(start = 25.dp, top = 25.dp, bottom = 25.dp)) {
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
                        onEnabled = { viewModel.viewState.value.autoForceOccupied = it }
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
                        onEnabled = { viewModel.viewState.value.autoAway = it }
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
                        onEnabled = {
                            if (!it && viewModel.isAnyAnalogMappedToControl(viewModel.getProfileBasedEnumValueAnalog(HyperStatSplitControlType.OAO_DAMPER.name))) {
                                showToast(getString(R.string.to_disable_oao), requireContext())
                            } else if (!it && viewModel.isPrePurgeEnabled()) {
                                showToast(getString(R.string.to_disable_oao_prepurge), requireContext())
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
                        onEnabled = {
                            if (viewModel.viewState.value.enableOutsideAirOptimization) {
                                viewModel.viewState.value.prePurge = it
                            } else {
                                showToast(getString(R.string.toEnable_pre_purge_oao), requireContext())
                            }
                        }
                    )
                }
            }

            if (viewModel.profileType == ProfileType.HYPERSTATSPLIT_4PIPE_UV || viewModel.profileType == ProfileType.HYPERSTATSPLIT_2PIPE_UV) {


                fun setTemping(value: Boolean) {
                    when(viewModel){
                        is Pipe4UvViewModel -> (viewModel.viewState.value as Pipe4UvViewState).saTempering = value
                        is Pipe2UvViewModel -> (viewModel.viewState.value as Pipe2UvViewState).saTempering = value
                    }
                }

                fun getTemping(): Boolean {
                    return when(viewModel){
                        is Pipe4UvViewModel -> (viewModel.viewState.value as Pipe4UvViewState).saTempering
                        is Pipe2UvViewModel -> (viewModel.viewState.value as Pipe2UvViewState).saTempering
                        else -> false
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(365.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)) {
                        StyledTextView(
                            stringResource(R.string.supply_air_tempering),
                            fontSize = 20,
                            textAlignment = TextAlign.Start
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ToggleButtonStateful(
                            defaultSelection = getTemping(),
                            onEnabled = {
                                setTemping(it)
                            }
                        )
                    }

                }
            }

        }
    }

    @Composable
    fun TitleLabel(modifier: Modifier = Modifier) {
        Row(
            modifier = modifier
                .fillMaxWidth()
        ) {
            Spacer(modifier=Modifier.width(300.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                SubTitle(
                    stringResource(R.string.title_enable)
                )
            }
            Spacer(modifier=Modifier.width(0.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                SubTitle(
                    stringResource(R.string.title_mapping)
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 5.dp), contentAlignment = Alignment.CenterEnd) {
                SubTitle(
                    stringResource(R.string.title_testsignal)
                )
            }
        }
    }

    @Composable
    fun SensorConfig(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        val temperatureEnums = viewModel.getAllowedValues(DomainName.temperatureSensorBusAdd0, viewModel.equipModel)
        val humidityEnums = viewModel.getAllowedValues(DomainName.humiditySensorBusAdd0, viewModel.equipModel)
        val pressureEnum = viewModel.getAllowedValues(DomainName.pressureSensorBusAdd0, viewModel.equipModel)

        SubTitle(stringResource(R.string.sensorbus),modifier)
        Row(modifier = modifier.padding(vertical = 10.dp)) {
            Column {
                EnableCompose(stringResource(R.string.sensorbusaddress0), viewModel.viewState.value.sensorAddress0.enabled) {
                    viewModel.viewState.value.sensorAddress0.enabled = it
                }
                Spacer(modifier = Modifier.height(80.dp))
                EnableCompose("", viewModel.viewState.value.pressureSensorAddress0.enabled, true) {
                    viewModel.viewState.value.pressureSensorAddress0.enabled = it
                }
            }
            Column {
                ConfigCompose(
                    stringResource(R.string.title_temperature),
                    temperatureEnums[viewModel.viewState.value.sensorAddress0.association],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.association = it.index
                }
                HumidityCompose(
                    stringResource(R.string.title_humidity),
                    humidityEnums[viewModel.viewState.value.sensorAddress0.association].dis ?: humidityEnums[viewModel.viewState.value.sensorAddress0.association].value
                )
                Spacer(modifier = Modifier.height(17.dp))
                ConfigCompose(
                    stringResource(id=R.string.title_pressure),
                    pressureEnum[viewModel.viewState.value.pressureSensorAddress0.association],
                    pressureEnum,
                    "",
                    viewModel.viewState.value.pressureSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.pressureSensorAddress0.association = it.index
                }
            }
        }

        Row(modifier = modifier.padding(vertical = 25.dp)) {
            EnableCompose(stringResource(R.string.sensorbusaddress1), viewModel.viewState.value.sensorAddress1.enabled) {
                viewModel.viewState.value.sensorAddress1.enabled = it
            }
            Column {
                ConfigCompose(
                    stringResource(R.string.title_temperature),
                    temperatureEnums[viewModel.viewState.value.sensorAddress1.association],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.association = it.index
                }
                HumidityCompose(
                    stringResource(R.string.title_humidity),
                    humidityEnums[viewModel.viewState.value.sensorAddress1.association].dis ?: humidityEnums[viewModel.viewState.value.sensorAddress1.association].value
                )
            }
        }
        Row(modifier = modifier.padding(vertical = 10.dp)) {
            EnableCompose(stringResource(R.string.sensorbusaddress2), viewModel.viewState.value.sensorAddress2.enabled) {
                viewModel.viewState.value.sensorAddress2.enabled = it
            }
            Column {
                ConfigCompose(
                    stringResource(R.string.title_temperature),
                    temperatureEnums[viewModel.viewState.value.sensorAddress2.association],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.association = it.index
                }
                HumidityCompose(
                    stringResource(R.string.title_humidity),
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
                    expandedWidth = 400,
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
    fun RelayConfig(viewModel: HyperStatSplitViewModel, modifier: Modifier = Modifier) {
        val relayEnums =
            viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.equipModel)

        fun getDisabledIndices(relayConfig: ConfigState): List<HyperStatSplitViewModel.Option> {

            val enabledEnums = mutableListOf<HyperStatSplitViewModel.Option>()

            if (viewModel.isCompressorMappedWithAnyRelay()) {
                if (viewModel.isChangeOverCoolingMapped(relayConfig)) {
                    relayEnums.forEach {
                        if (it.value != DomainName.changeOverHeating) {
                            enabledEnums.add(it)
                        }
                    }
                } else if (viewModel.isChangeOverHeatingMapped(relayConfig)) {
                    relayEnums.forEach {
                        if (it.value != DomainName.changeOverCooling) {
                            enabledEnums.add(it)
                        }
                    }
                } else {
                    enabledEnums.addAll(relayEnums)
                }
            } else {
                relayEnums.forEach {
                    if (it.value != DomainName.changeOverCooling && it.value != DomainName.changeOverHeating) {
                        enabledEnums.add(it)
                    }
                }
            }
            return enabledEnums
        }


        SubTitle(stringResource(R.string.hs_connect), modifier)
        Row(
            modifier = modifier.fillMaxWidth()
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
                        enabledItems = getDisabledIndices(relayConfig),
                        isEnabled = relayConfig.enabled,
                        onAssociationChanged = { associationIndex ->
                            relayConfig.association = associationIndex.index
                        },
                        onTestActivated = { isChecked ->

                            if (viewModel.equipRef != null) {
                                viewModel.handleTestRelayChanged(
                                    index,
                                    isChecked
                                )
                            } else {
                                showToast(
                                    getString(R.string.please_pair_equip),
                                    viewModel.context
                                )

                            }
                        },

                        testSignalState = viewModel.getTestSignalForRelay(index)
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
        enabledItems: List<HyperStatSplitViewModel.Option>,
        unit: String,
        isEnabled: Boolean,
        onAssociationChanged: (HyperStatSplitViewModel.Option) -> Unit,
        onTestActivated: (Boolean) -> Unit,
        testSignalState :Boolean = false
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
                .padding(start = 15.dp, top = 12.dp)) {
                StyledTextView(relayName, fontSize = 20)
            }
            Box(modifier = Modifier
                .weight(4f)
                .padding(start = 50.dp)) {
                SearchSpinnerElement(
                    default = association,
                    allItems = relayEnums,
                    enabledItems = enabledItems,
                    unit = unit,
                    onSelect = { onAssociationChanged(it) },
                    width = 350,
                    isEnabled = isEnabled
                )
            }
            var buttonState by remember { mutableStateOf(testSignalState) }
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
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = when (buttonState) {
                        true -> primaryColor
                        false -> Color.Black
                    }, containerColor = Color.Transparent
                ),
                border = BorderStroke(
                    1.dp, color = when (buttonState) {
                        true -> primaryColor
                        false -> Color.Black
                    }
                ), modifier = Modifier
                    .width(72.dp)
                    .height(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = when (buttonState) {
                        true -> "ON"
                        false -> "OFF"
                    },
                    fontSize = 20.sp,
                    fontFamily = myFontFamily,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.wrapContentSize(Alignment.Center)

                )
            }
        }
    }

    @Composable
    fun AnalogOutConfig(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        val analogOutEnums = viewModel.getAllowedValues(DomainName.analog1OutputAssociation, viewModel.equipModel)

        var enabledEnums = if (viewModel.viewState.value.enableOutsideAirOptimization) {
            analogOutEnums
        } else {
            analogOutEnums.filter { it.value != DomainName.oaoDamper }
        }
        if (viewModel.profileType == ProfileType.HYPERSTATSPLIT_4PIPE_UV || viewModel.profileType == ProfileType.HYPERSTATSPLIT_2PIPE_UV)
        {
            val controlViaMode = when (viewModel) {
                is Pipe4UvViewModel -> (viewModel.viewState.value as Pipe4UvViewState).controlVia
                is Pipe2UvViewModel -> (viewModel.viewState.value as Pipe2UvViewState).controlVia
              else -> (viewModel.viewState.value as Pipe4UvViewState).controlVia
            }

            if (controlViaMode == 1) {
                enabledEnums = enabledEnums.filter { it.value != DomainName.faceBypassDamperModulatingCmd }
            } else if (controlViaMode == 0) {
                enabledEnums = if (isPipe2UvProfile(viewModel = viewModel)) {
                    enabledEnums.filter { it.value != DomainName.modulatingWaterValve }
                } else {
                    enabledEnums.filter { it.value != DomainName.hotWaterModulatingHeatValve && it.value != DomainName.chilledWaterModulatingCoolValve }
                }
            }
        }

        Row(modifier = modifier.fillMaxWidth()) {
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
                        onTestSignalSelected = { value ->
                            if (viewModel.equipRef != null) {
                                viewModel.handleTestAnalogOutChanged(index, value)
                            }
                            else {
                                showToast(
                                    getString(R.string.please_pair_equip),
                                    viewModel.context
                                )
                            }
                        },
                        testSignalValue = viewModel.getTestSignalForAnalogOut(index).toString())
                }
            }
        }
    }

    @Composable
    fun AnalogOutConfiguration(
        viewModel: HyperStatSplitViewModel,
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
        onTestSignalSelected: (Double) -> Unit = {},
        testSignalValue :String = "0.0"
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
                ) {
                    if (!it || viewModel.viewState.value.enableOutsideAirOptimization || !(
                            (analogOutName.contains("1") && viewModel.viewState.value.analogOut1Association == viewModel.getProfileBasedEnumValueAnalog(HyperStatSplitControlType.OAO_DAMPER.name)) ||
                            (analogOutName.contains("2") && viewModel.viewState.value.analogOut2Association == viewModel.getProfileBasedEnumValueAnalog(
                                HyperStatSplitControlType.OAO_DAMPER.name)) ||
                            (analogOutName.contains("3") && viewModel.viewState.value.analogOut3Association == viewModel.getProfileBasedEnumValueAnalog(HyperStatSplitControlType.OAO_DAMPER.name)) ||
                            (analogOutName.contains("4") && viewModel.viewState.value.analogOut4Association == viewModel.getProfileBasedEnumValueAnalog(HyperStatSplitControlType.OAO_DAMPER.name))
                        )
                    ) {
                        onEnabledChanged(it)
                    } else {
                        showToast(getString(R.string.to_enable_analog_out), requireContext())
                    }
                }
            }
            Box(modifier = Modifier
                .weight(1.3f)
                .padding(top = 13.dp)) {
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
            ) {
                SpinnerElementOption(
                    defaultSelection = testSignalValue,
                    items = testSingles,
                    unit = unit,
                    itemSelected = {onTestSignalSelected(it.value.toDouble()) },
                    previewWidth = 70
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
        var selectedIndex by remember { mutableIntStateOf(getDefaultSelectionIndex(items, defaultSelection)) }
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
                .width((previewWidth + 30).dp)
                .clickable(onClick = { expanded.value = true })
            ) {
                Row {
                    Text(
                        fontSize = 20.sp,
                        modifier = Modifier.width(70.dp),
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
                                        fontFamily =myFontFamily,
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
        itemSelected: (String) -> Unit,
        previewWidth: Int = 160,
        textWidth : Int = 70
    ) {
        val selectedItem = remember { mutableStateOf(defaultSelection) }
        val lazyListState = rememberLazyListState()
        var selectedIndex by remember { mutableIntStateOf(items.indexOf(defaultSelection)) }
        val expanded = remember { mutableStateOf(false) }
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
                .width((previewWidth + 40).dp)
                .clickable(onClick = { expanded.value = true })) {
                Row {
                    Text(
                        fontSize = 20.sp,
                        modifier = Modifier.width(textWidth.dp),

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
                Row { Divider(modifier = Modifier.width((previewWidth+30).dp)) }
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
                LazyColumn(state = lazyListState,
                    modifier = Modifier
                        .height((customHeight).dp)
                        .width((previewWidth + 30).dp)) {
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

    @SuppressLint("DefaultLocale")
    private fun getDefaultSelectionIndex(items: List<HyperStatSplitViewModel.Option>, defaultSelection: String):Int {
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
    fun UniversalInConfig(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        val universalEnum = viewModel.getAllowedValues(DomainName.universalIn2Association, viewModel.equipModel)

        Row(modifier = modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.connect_ui),
                contentDescription = "Universal Inputs",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 33.dp, bottom = 25.dp)
                    .height(550.dp)
            )
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                AOTHConfig(UNIVERSAL_IN1,
                    viewModel.viewState.value.universalIn1Config.enabled,
                    {
                        if (isPipe2UvProfile(viewModel = viewModel)) {
                            viewModel.viewState.value.universalIn1Config.enabled = it
                        } else
                            viewModel.viewState.value.universalIn1Config.enabled = it
                    },
                    universalEnum.find { it.index == viewModel.viewState.value.universalIn1Config.association } ?: universalEnum[0],
                    universalEnum,
                    "",
                    viewModel.viewState.value.universalIn1Config.enabled,
                    {
                        if (isPipe2UvProfile(viewModel)) {
                            viewModel.viewState.value.universalIn1Config.association = SUPPLY_WATER_TEMPERATURE
                        } else {
                            viewModel.viewState.value.universalIn1Config.association =
                                it.index
                        }
                    },
                    isPipe2UvProfile = isPipe2UvProfile(viewModel),
                    mappingText = a75f.io.renatus.profiles.system.SUPPLY_WATER_TEMPERATURE)

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


    @Composable
    fun AOTHConfig(
        name: String,
        default: Boolean,
        onEnabled: (Boolean) -> Unit = {},
        spinnerDefault: HyperStatSplitViewModel.Option,
        items: List<HyperStatSplitViewModel.Option>,
        unit: String = "",
        isEnabled: Boolean,
        onSelect: (HyperStatSplitViewModel.Option) -> Unit,
        mappingText :String = "",
        isPipe2UvProfile: Boolean = false,
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .padding(10.dp)
        ) {

            Box(modifier = Modifier.wrapContentWidth()) {
                if(isPipe2UvProfile) {
                    ToggleButtonStateful(defaultSelection = true, onEnabled = {}, isDisabled = false)
                }
                else {
                    ToggleButtonStateful(defaultSelection = default) { onEnabled(it) }
                }
            }
            Box(modifier = Modifier
                .weight(1.5f)
                .padding(start = 15.dp, top = 12.dp)) {
                StyledTextView(name, fontSize = 20)
            }

            Box(modifier = Modifier
                .weight(4f)
                .padding(start = 16.dp)) {
                if (isPipe2UvProfile) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        StyledTextView(mappingText, fontSize = 20)
                    }
                } else {
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
        var selectedIndex by remember { mutableIntStateOf(default.index) }
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
                        fontFamily = myFontFamily,
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
                    .background(Color.White)
                    .width((expandedWidth).dp),
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
                                    Text(fontSize = 20.sp, text = stringResource(R.string.search))
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
                                textStyle = TextStyle(fontSize = 20.sp, fontFamily = myFontFamily),
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
                                            fontFamily = myFontFamily,
                                            modifier = Modifier.padding(end = 10.dp, start = 10.dp),
                                            fontWeight = FontWeight.Normal,
                                            text = it.dis ?: it.value
                                        )
                                        Text(
                                            fontSize = 20.sp,
                                            color = if (enabledItems.contains(it)) Color.Black else Color.Gray,
                                            fontFamily = myFontFamily,
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

    private fun formatText(input: String): String {
        val regex = "(?<=[a-zA-Z])(?=\\d)|(?<=\\d)(?=[a-zA-Z])|(?<=[a-z\\d])(?=[A-Z])".toRegex()
        return input.split(regex).joinToString(" ").replaceFirstChar { it.uppercase() }
    }

    @Composable
    fun CompressorControl(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            if (viewModel.isCompressorAOEnabled(CpuAnalogControlType.COMPRESSOR_SPEED.ordinal)) {
                EnableCompressorVoltage(viewModel, CpuAnalogControlType.COMPRESSOR_SPEED)
            }
        }
    }

    @Composable
    fun EnableCompressorVoltage(viewModel: HyperStatSplitViewModel, type: CpuAnalogControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nCompressor Speed",
                "Analog-out1 at Max \nCompressor Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.compressorMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.compressorMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.compressorMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.compressorMaxVoltage = it.value.toInt()
                },
                modifier = Modifier.padding(10.dp)
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nCompressor Speed",
                "Analog-out2 at Max \nCompressor Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.compressorMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.compressorMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.compressorMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.compressorMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nCompressor Speed",
                "Analog-out3 at Max \nCompressor Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.compressorMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.compressorMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.compressorMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.compressorMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nCompressor Speed",
                "Analog-out4 at Max \nCompressor Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.compressorMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.compressorMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.compressorMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.compressorMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun DcvModulationControl(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            if (viewModel.isDamperModulationAOEnabled(CpuAnalogControlType.DCV_MODULATING_DAMPER.ordinal)) {
                EnableDcvModulationVoltage(viewModel, CpuAnalogControlType.DCV_MODULATING_DAMPER)
            }
        }
    }

    @Composable
    fun EnableDcvModulationVoltage(viewModel: HyperStatSplitViewModel, type: CpuAnalogControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nDcv Modulation Damper",
                "Analog-out1 at Max \nDcv Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nDcv Modulation Damper",
                "Analog-out2 at Max \nDcv Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nDcv Modulation Damper",
                "Analog-out3 at Max \nDcv Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nDcv Modulation Damper",
                "Analog-out4 at Max \nDcv Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun StagedFanControl(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            if (viewModel.isStagedFanAOEnabled(CpuAnalogControlType.STAGED_FAN.ordinal)) {
                EnableStagedFanVoltage(viewModel)
            }
        }
    }

    @Composable
    fun EnableStagedFanVoltage(viewModel: HyperStatSplitViewModel) {
        if (viewModel.isStagedFanAOEnabled(CpuAnalogControlType.STAGED_FAN.ordinal)) {
            CpuStagedFanConfiguration("Fan Out\nduring Recirc",
                "Fan Out\nduring Economizer",
                "Fan Out\nduring Heat Stage 1",
                "Fan Out\nduring Heat Stage 2",
                "Fan Out\nduring Heat Stage 3",
                "Fan Out\nduring Cool Stage 1",
                "Fan Out\nduring Cool Stage 2",
                "Fan Out\nduring Cool Stage 3",
                "Fan Out\nduring Compressor Stage 1",
                "Fan Out\nduring Compressor Stage 2",
                "Fan Out\nduring Compressor Stage 3",
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
                compressor1Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.compressorStage1Voltage.toString(),
                compressor2Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.compressorStage2Voltage.toString(),
                compressor3Default = (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.compressorStage3Voltage.toString(),
                onRecircSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState) .stagedFanVoltages.recircVoltage = it.value.toInt()
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
                onCompressor1Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.compressorStage1Voltage = it.value.toInt()
                },
                onCompressor2Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.compressorStage2Voltage = it.value.toInt()
                },
                onCompressor3Selected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).stagedFanVoltages.compressorStage3Voltage = it.value.toInt()
                }, viewModel
            )
        }
    }

    @Composable
    fun OAODamperControl(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            if (viewModel.isOAODamperAOEnabled(CpuAnalogControlType.OAO_DAMPER.ordinal)) {
                EnableOAODamperVoltage(viewModel, CpuAnalogControlType.OAO_DAMPER)
            }
        }
    }

    @Composable
    fun EnableOAODamperVoltage(viewModel:HyperStatSplitViewModel, type: CpuAnalogControlType) {
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
    fun ReturnDamperControl(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            if (viewModel.isReturnDamperAOEnabled(CpuAnalogControlType.RETURN_DAMPER.ordinal)) {
                EnableReturnDamperVoltage(viewModel, CpuAnalogControlType.RETURN_DAMPER)
            }
        }
    }

    @Composable
    fun EnableReturnDamperVoltage(viewModel: HyperStatSplitViewModel, type: CpuAnalogControlType) {
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
        onMaxSelected: (HyperStatSplitViewModel.Option) -> Unit = {},
        modifier: Modifier = Modifier
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
                    itemSelected = { onMinSelected(it) },
                    previewWidth = 70)
            }

            Box(modifier = modifier
                .weight(2f)
                .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)) {
                StyledTextView(
                    maxLabel, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SpinnerElementOption(defaultSelection = maxDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onMaxSelected(it) },
                    previewWidth = 70)
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
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
        compressor1Label: String,
        compressor2Label: String,
        compressor3Label: String,
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
        compressor1Default: String,
        compressor2Default: String,
        compressor3Default: String,
        onRecircSelected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onEconomizerSelected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onHeatStage1Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onHeatStage2Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onHeatStage3Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCoolStage1Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCoolStage2Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCoolStage3Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCompressor1Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCompressor2Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        onCompressor3Selected: (HyperStatSplitViewModel.Option) -> Unit = {},
        viewModel: HyperStatSplitViewModel
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
                    itemSelected = { onRecircSelected(it) }, previewWidth = 70)
            }

            if (viewModel.isOAODamperAOEnabled(CpuAnalogControlType.OAO_DAMPER.ordinal)) {
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
                        itemSelected = { onEconomizerSelected(it) }, previewWidth = 70)
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
                        itemSelected = { onHeatStage1Selected(it) }, previewWidth = 70)
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
                        itemSelected = { onHeatStage2Selected(it) }, previewWidth = 70)
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
                        itemSelected = { onHeatStage3Selected(it) }, previewWidth = 70)
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
                        itemSelected = { onCoolStage1Selected(it) }, previewWidth = 70)
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
                        itemSelected = { onCoolStage2Selected(it) }, previewWidth = 70)
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
                        itemSelected = { onCoolStage3Selected(it) }, previewWidth = 70)
                }
            }
            if (viewModel.isCompressorStage1RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        compressor1Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = compressor1Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onCompressor1Selected(it) }, previewWidth = 70)
                }
            }
            if (viewModel.isCompressorStage2RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        compressor2Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = compressor2Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onCompressor2Selected(it) }, previewWidth = 70)
                }
            }
            if (viewModel.isCompressorStage3RelayEnabled()) {
                nEntries++
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                    StyledTextView(
                        compressor3Label, fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(defaultSelection = compressor3Default,
                        items = itemList,
                        unit = unit,
                        itemSelected = { onCompressor3Selected(it) }, previewWidth = 70)
                }
            }

            if (nEntries % 2 == 1) {
                Box(modifier = Modifier.weight(2f))
                Box(modifier = Modifier.weight(1f))
            }

        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun ZoneOAOConfig(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        FlowRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(start = 25.dp, bottom = 10.dp),
            maxItemsInEachRow = 4
        ) {
            var nEntries = 0

            if (viewModel.isAO1MappedToFan()) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out1 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanLow =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out1 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out1 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanAtFanHigh =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))

            }

            if (viewModel.isAO2MappedToFan()) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out2 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanLow =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out2 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out2 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanAtFanHigh =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }
                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))

            }

            if (viewModel.isAO3MappedToFan()) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out3 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanLow =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out3 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out3 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanAtFanHigh =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))

            }

            if (viewModel.isAO4MappedToFan()) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out4 at Fan Low\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanLow =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out4 at Fan Medium\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out4 at Fan High\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanAtFanHigh =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )

                }

                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))
            }
        }
        FlowRow( modifier = modifier
            .fillMaxWidth()
            .padding(start = 25.dp, bottom = 10.dp),
            maxItemsInEachRow = 4) {
            var nEntries = 0

            if (viewModel.viewState.value.enableOutsideAirOptimization) {
                nEntries += 6
                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Recirc", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringRecirc.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringRecirc = it.toDouble()},
                        previewWidth = 70
                        )
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp, start = 10.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Conditioning", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringConditioning.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringConditioning = it.toDouble()}, previewWidth = 70)
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
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringFanLow = it.toDouble()}, previewWidth = 70)
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp, start = 10.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Fan Medium", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanMedium.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringFanMedium = it.toDouble() }, previewWidth = 70)
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp)) {
                    StyledTextView(
                        "Outside Damper Min Open\nDuring Fan High", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanHigh.toInt().toString(),
                        items = viewModel.outsideDamperMinOpenList, unit = "%",
                        itemSelected = { viewModel.viewState.value.outsideDamperMinOpenDuringFanHigh = it.toDouble()}, previewWidth = 70)
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp, start = 10.dp)) {
                    StyledTextView(
                        "Exhaust Fan Stage 1 Threshold\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.exhaustFanStage1Threshold.toInt().toString(),
                        items = viewModel.exhaustFanThresholdList, unit = "%",
                        itemSelected = { viewModel.viewState.value.exhaustFanStage1Threshold = it.toDouble()}, previewWidth = 70)
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp)) {
                    StyledTextView(
                        "Exhaust Fan Stage 2 Threshold\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.exhaustFanStage2Threshold.toInt().toString(),
                        items = viewModel.exhaustFanThresholdList, unit = "%",
                        itemSelected = { viewModel.viewState.value.exhaustFanStage2Threshold = it.toDouble()}, previewWidth = 70)
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp, start = 10.dp)) {
                    StyledTextView(
                        "Exhaust Fan Hysteresis\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.exhaustFanHysteresis.toInt().toString(),
                        items = viewModel.exhaustFanHysteresisList, unit = "%",
                        itemSelected = { viewModel.viewState.value.exhaustFanHysteresis = it.toDouble()}, previewWidth = 70)
                }

                Box(modifier = Modifier
                    .weight(2f)
                    .padding(top = 10.dp, bottom = 20.dp)) {
                    StyledTextView(
                        "CO2 Damper Opening Rate\n", fontSize = 20, textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementString(defaultSelection = viewModel.viewState.value.zoneCO2DamperOpeningRate.toInt().toString(),
                        items = viewModel.zoneCO2DamperOpeningRateList, unit = "%",
                        itemSelected = { viewModel.viewState.value.zoneCO2DamperOpeningRate = it.toDouble() }, previewWidth = 70)
                }

                if (viewModel.isPrePurgeEnabled()) {
                    nEntries += 1
                    Box(modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 20.dp, start = 10.dp)) {
                        StyledTextView(
                            "Pre Purge Min\nDamper Position", fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SpinnerElementString(defaultSelection = viewModel.viewState.value.prePurgeOutsideDamperOpen.toInt().toString(),
                            items = viewModel.prePurgeOutsideDamperOpenList, unit = "%",
                            itemSelected = { viewModel.viewState.value.prePurgeOutsideDamperOpen = it.toDouble() }, previewWidth = 70)
                    }
                }
            }

            nEntries += 3
            Box(modifier = Modifier
                .weight(2f)
                .padding(
                    top = 10.dp,
                    bottom = 20.dp,
                    start = if (nEntries % 2 == 1) 10.dp else 0.dp
                )) {
                StyledTextView(
                    "Zone CO2 Threshold\n", fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 25.dp)) {
                SpinnerElementString(defaultSelection = viewModel.viewState.value.zoneCO2Threshold.toInt().toString(),
                    items = viewModel.zoneCO2ThresholdList, unit = "ppm",
                    itemSelected = { viewModel.viewState.value.zoneCO2Threshold = it.toDouble() }, previewWidth = 125, textWidth = 120)
            }

            Box(modifier = Modifier
                .weight(2f)
                .padding(
                    top = 10.dp,
                    bottom = 20.dp,
                    start = if (nEntries % 2 == 1) 0.dp else 10.dp
                )) {
                StyledTextView(
                    "Zone CO2 Target\n", fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 25.dp)) {
                SpinnerElementString(defaultSelection = viewModel.viewState.value.zoneCO2Target.toInt().toString(),
                    items = viewModel.zoneCO2TargetList, unit = "ppm",
                    itemSelected = { viewModel.viewState.value.zoneCO2Target = it.toDouble() },previewWidth = 125, textWidth = 120)
            }

            Box(modifier = Modifier
                .weight(2f)
                .padding(
                    top = 10.dp,
                    bottom = 20.dp,
                    start = if (nEntries % 2 == 1) 10.dp else 0.dp
                )) {
                StyledTextView(
                    "Zone PM2.5 Target\n", fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(end = 25.dp)) {
                SpinnerElementString(defaultSelection = viewModel.viewState.value.zonePM2p5Target.toInt().toString(),
                    items = viewModel.zonePM2p5TargetList, unit = "ug/m3",
                    itemSelected = { viewModel.viewState.value.zonePM2p5Target = it.toDouble() },previewWidth = 125, textWidth = 120)
            }

            if (nEntries % 2 == 1) {
                Box(modifier = Modifier.weight(2f))
                Box(modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    fun DisplayInDeviceConfig(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier.padding(start = 25.dp, top = 25.dp)) {
            BoldStyledTextView("Display in Device Home Screen", fontSize = 20)
            Row(horizontalArrangement = Arrangement.Center)
            {
                Box(modifier = Modifier.weight(1f)) {
                    SubTitle("Sensors Value (Select up to 2)")

                }
                Box(modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)) {
                    SubTitle("Temperature Value (Select only 1)")

                }
            }
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
                        onEnabled = {
                            if (it && viewModel.getDeviceDisplayCount() > 1) {
                                showToast(getString(R.string.max_2_params), requireContext())
                            } else {
                                viewModel.viewState.value.displayHumidity = it
                            }
                        }
                    )
                }

                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.spaceTemp.disName, fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.spaceTemp,
                        onEnabled = {
                            if ( it && viewModel.viewState.value.desiredTemp) {
                               viewModel.viewState.value.desiredTemp = false
                                viewModel.viewState.value.spaceTemp = true
                            } else {
                                viewModel.viewState.value.desiredTemp = true
                                viewModel.viewState.value.spaceTemp = it
                            }
                        })
                }
            }

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)) {


                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.displayCO2.disName, fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.displayCO2,
                        onEnabled = {
                            if (it && viewModel.getDeviceDisplayCount() > 1) {
                                showToast(getString(R.string.max_2_params), requireContext())
                            } else {
                                viewModel.viewState.value.displayCO2 = it
                            }
                        }
                    )
                }

                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.desiredTemp.disName, fontSize = 20)
                  }
                Box(modifier = Modifier.weight(1f)){
                    ToggleButton(defaultSelection = viewModel.viewState.value.desiredTemp, onEnabled = {
                        if (it && viewModel.viewState.value.spaceTemp) {
                            viewModel.viewState.value.spaceTemp = false
                            viewModel.viewState.value.desiredTemp = true
                        } else {
                            viewModel.viewState.value.spaceTemp = true
                            viewModel.viewState.value.desiredTemp = it
                        }
                    })
                }

            }

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)){

                Box(modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)) {
                    StyledTextView(text = viewModel.profileConfiguration.displayPM2p5.disName, fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.displayPM2p5,
                        onEnabled = {
                            if (it && viewModel.getDeviceDisplayCount() > 1) {
                                showToast(getString(R.string.max_2_params), requireContext())
                            } else {
                                viewModel.viewState.value.displayPM2p5 = it
                            }
                        }
                    )
                }

                Box(modifier = Modifier.weight(4f))
                Box(modifier = Modifier.weight(1f))
            }
        }
        DividerRow()

    }

    @Composable
    fun MiscSettingConfig(viewModel: HyperStatSplitViewModel) {
        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            BoldStyledTextView(getString(R.string.misc_settings), fontSize = 20)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)
                ) {
                    StyledTextView(
                        text = viewModel.profileConfiguration.disableTouch.disName,
                        fontSize = 20
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.disableTouch,
                        onEnabled = {
                            viewModel.viewState.value.disableTouch = it
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)
                ) {
                    StyledTextView(
                        text = viewModel.profileConfiguration.enableBrightness.disName,
                        fontSize = 20
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.enableBrightness,
                        onEnabled = {
                            viewModel.viewState.value.enableBrightness = it
                        }
                    )
                }
            }
            Row(
                modifier = Modifier
                    .width(550.dp)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ){
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)
                ) {
                    StyledTextView(
                        text = getString(R.string.enable_backlight),
                        fontSize = 20
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(
                        defaultSelection = viewModel.viewState.value.backLight,
                        onEnabled = {
                            viewModel.viewState.value.backLight = it
                        }
                    )
                }

            }
        }
    }

    @Composable
    fun DividerRow(modifier: Modifier = Modifier) {
        Spacer(modifier = Modifier.height(5.dp))
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ComposeUtil.DashDivider(height = 20.dp)
        }
    }

    @Composable
    fun SaveConfig(viewModel: HyperStatSplitViewModel,modifier: Modifier = Modifier) {
        Row(
            modifier = modifier
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

    //Pin section

    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun PinPasswordView(viewModel: HyperStatSplitViewModel) {

        var isDialogVisible by remember { mutableStateOf(false) }
        var selectedPinTitle by remember { mutableStateOf("") }
        val installerPin = remember { mutableStateListOf<Int>() }
        val conditioningPin = remember { mutableStateListOf<Int>() }


        fun decodingPin(pin: String): MutableList<Int> {
            val decodedBytes = Base64.STANDARD.decode(pin)
            val decodedString = decodedBytes.toString()
            return decodedString.map { it.toString().toInt() }.toMutableList()
        }

        fun resetConditioningPin() {
            conditioningPin.clear()
            conditioningPin.addAll(listOf(0, 0, 0, 0))
            viewModel.viewState.value.conditioningModePassword = "0"
        }

        fun resetInstallerPin() {
            installerPin.clear()
            installerPin.addAll(listOf(0, 0, 0, 0))
            viewModel.viewState.value.installerPassword = "0"
        }

        /**
         * case :1 -> if the pin toggle is enabled for first time/ the pin is disabled and enabled  , by default it will be set to 0000
         * case :2 -> if the pin is already set, it will be decode and it will update in the list
         * case :3 -> if the pin toggle is  then if the user click on cancel button, togged will be disabled and the pin will be reset to 0000
         */

        if (viewModel.viewState.value.installerPassword == "0") {
            installerPin.clear()
            installerPin.addAll(listOf(0, 0, 0, 0))
        } else {
            installerPin.clear()
            // Decode the Base64 encoded password and convert to a list of integers
            installerPin.addAll(decodingPin(viewModel.viewState.value.installerPassword))
        }

        if (viewModel.viewState.value.conditioningModePassword == "0") {
            conditioningPin.clear()
            conditioningPin.addAll(listOf(0, 0, 0, 0))
        } else {
            conditioningPin.clear()
            conditioningPin.addAll(decodingPin(viewModel.viewState.value.conditioningModePassword))
        }

        // for toggle change

        val onToggle: (String, Boolean) -> Unit = { pinTitle, isEnabled ->

            selectedPinTitle = if (isEnabled) pinTitle else ""
            isDialogVisible = isEnabled

            when (pinTitle) {

                INSTALLER_ACCESS -> {
                    viewModel.viewState.value.installerPinEnable = isEnabled
                    if (!isEnabled) resetInstallerPin()
                }

                CONDITIONING_ACCESS -> {
                    viewModel.viewState.value.conditioningModePinEnable = isEnabled
                    if (!isEnabled) resetConditioningPin()
                }

                else -> CcuLog.i("USER_TEST", "No valid PIN selected")
            }
        }

        // for cancel button click
        val onCancel: (String, Boolean) -> Unit = { pinTitle, enable ->

            selectedPinTitle = if (enable) pinTitle else ""
            isDialogVisible = enable

            when (pinTitle) {

                INSTALLER_ACCESS -> {
                    if (!enable && (viewModel.viewState.value.installerPassword == "0")) {
                        resetInstallerPin()
                        viewModel.viewState.value.installerPinEnable = false
                    }
                }

                CONDITIONING_ACCESS -> {
                    if (!enable && (viewModel.viewState.value.conditioningModePassword == "0")) {
                        resetConditioningPin()
                        viewModel.viewState.value.conditioningModePinEnable = false
                    }
                }

                else -> CcuLog.i("USER_TEST", "No valid PIN selected")
            }
        }


        Column(
            modifier = Modifier
                .padding(start = 25.dp, top = 25.dp, bottom = 25.dp)
        ) {
            BoldStyledTextView(getString(R.string.pin_lock), fontSize = 20)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.weight(4f)) {
                    LabelWithToggleButton(
                        text = INSTALLER_ACCESS,
                        defaultSelection = viewModel.viewState.value.installerPinEnable,
                        onEnabled = { enabled ->
                            onToggle(INSTALLER_ACCESS, enabled)
                        },
                        eyeIconEnable = viewModel.viewState.value.installerPinEnable
                    )
                    {
                        isDialogVisible = true
                        selectedPinTitle = INSTALLER_ACCESS
                    }

                }
                Box(modifier = Modifier.weight(4f)) {
                    LabelWithToggleButton(
                        text = CONDITIONING_ACCESS,
                        defaultSelection = viewModel.viewState.value.conditioningModePinEnable,
                        onEnabled = { enabled ->
                            onToggle(
                                CONDITIONING_ACCESS,
                                enabled
                            )
                        },
                        eyeIconEnable = viewModel.viewState.value.conditioningModePinEnable
                    ) {
                        isDialogVisible = true
                        selectedPinTitle = CONDITIONING_ACCESS
                    }
                }
            }

            // Pin dialog visibility check

            if (isDialogVisible) {

                ShowPinDialog(
                    onDismiss = {
                        isDialogVisible = false
                        onCancel(selectedPinTitle,false)
                                },
                    selectedPinTitle = selectedPinTitle,
                    pinDigits = when(selectedPinTitle)
                    {
                        INSTALLER_ACCESS -> installerPin
                        CONDITIONING_ACCESS -> conditioningPin
                        else -> mutableStateListOf(0, 0, 0, 0)
                    }
                )
                {
                    // Save the PIN configuration
                    when(selectedPinTitle)
                    {
                        INSTALLER_ACCESS -> {

                            viewModel.viewState.value.installerPassword = Base64.STANDARD.encode(installerPin.joinToString(separator = "") { it.toString() })
                        }
                        CONDITIONING_ACCESS -> {
                            viewModel.viewState.value.conditioningModePassword = Base64.STANDARD.encode(conditioningPin.joinToString(separator = "") { it.toString() })
                        }
                        else -> CcuLog.i("USER_TEST", "No valid PIN selected")
                    }
                    isDialogVisible = false
                }
            }
        }
        DividerRow()
    }


    @Composable
    fun ShowPinDialog(
        onDismiss: () -> Unit,
        selectedPinTitle: String,
        pinDigits: MutableList<Int>,
        onSave: () -> Unit
    ) {
        var saveState by remember { mutableStateOf(false) }
        CcuLog.i("USER_TEST", "ShowPinDialog called with title: $selectedPinTitle and pinDigits: ${pinDigits.toList()}")


        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
        ) {
            Column(
                modifier = Modifier
                    .width(500.dp)
                    .height(370.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(getString(R.string.pin_lock)+":"+" $selectedPinTitle", fontSize = 23.sp, color = Color.Black, fontWeight = FontWeight.Bold,fontFamily = myFontFamily , modifier = Modifier.padding(start = 10.dp) )
                Box(modifier = Modifier.padding(start = 20.dp)) {
                    PinSection(
                        onSaveState = { saveState = it },
                        pinDigits = pinDigits
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    TextButton(onClick = onDismiss,modifier = Modifier.align(alignment = Alignment.CenterVertically)) {
                        Text(getString(R.string.button_cancel), color = primaryColor, fontSize = 22.sp, fontFamily = myFontFamily)
                    }

                    Divider(
                        modifier = Modifier
                            .align(alignment = Alignment.CenterVertically)
                            .height(20.dp)
                            .width(2.dp),
                        color = Color.LightGray
                    )

                    TextButton(onClick = { onSave() }, enabled = saveState, modifier = Modifier.align(alignment = Alignment.CenterVertically))
                    {
                        Text(
                            getString(R.string.button_save),
                            color = if (saveState) primaryColor else Color.Gray,
                            fontSize = 22.sp, fontFamily = myFontFamily
                        )
                    }
                }
            }
        }
    }


    @Composable
    fun PinSection(
        onSaveState: (Boolean) -> Unit,
        pinDigits: MutableList<Int>
    ) {
        // Store the original pin state for comparison
        val originalPin = remember { mutableStateListOf(0, 0, 0, 0) }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(4) { index ->
                PinPassword(
                    items = (0..9).map { it.toString() },
                    modifier = Modifier.weight(1f),
                    textModifier = Modifier.width(30.dp),
                    startIndex = pinDigits[index],
                    onChanged = { selected ->
                        val selectedInt = selected.toInt()
                        // Update the pin digit
                        pinDigits[index] = selectedInt
                        // Compare current PIN with original PIN or the pin 0,0,0,0 making save btn to disable state
                        val isChanged =  (pinDigits.toList()== listOf(0,0,0,0) || pinDigits.toList() != originalPin.toList())
                        onSaveState(isChanged)
                        originalPin[index]= selectedInt
                    }
                )
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
    private fun isPipe2UvProfile(viewModel: HyperStatSplitViewModel): Boolean {
        return viewModel.profileType.name == ProfileType.HYPERSTATSPLIT_2PIPE_UV.name
    }


}