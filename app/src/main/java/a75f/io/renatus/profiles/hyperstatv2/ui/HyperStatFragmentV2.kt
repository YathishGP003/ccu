package a75f.io.renatus.profiles.hyperstatv2.ui

import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.R
import a75f.io.renatus.composables.AOTHConfig
import a75f.io.renatus.composables.AnalogOutConfig
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.composables.TempOffsetPicker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.BoldStyledTextView
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SpinnerElementOption
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.profiles.CopyConfiguration
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hyperstatv2.viewmodels.HyperStatViewModel
import a75f.io.renatus.profiles.system.ANALOG_IN1
import a75f.io.renatus.profiles.system.ANALOG_IN2
import a75f.io.renatus.profiles.system.ENABLE
import a75f.io.renatus.profiles.system.MAPPING
import a75f.io.renatus.profiles.system.TEST_SIGNAL
import a75f.io.renatus.profiles.system.THERMISTOR_1
import a75f.io.renatus.profiles.system.THERMISTOR_2
import a75f.io.renatus.profiles.system.advancedahu.Option
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels

/**
 * Created by Manjunath K on 26-09-2024.
 */

abstract class HyperStatFragmentV2 : BaseDialogFragment(), OnPairingCompleteListener {

    open val viewModel: HyperStatViewModel by viewModels()

    override fun onDestroy() {
        super.onDestroy()
        if (Globals.getInstance().isTestMode) {
            Globals.getInstance().isTestMode = false
        }
    }

    @Composable
    fun TempOffset(modifier: Modifier = Modifier) {
        val valuesPickerState = rememberPickerState()
        Column(modifier = modifier.padding(start = 400.dp, end = 400.dp)) {
            val temperatureOffsetsList = viewModel.getListByDomainName(DomainName.temperatureOffset, viewModel.equipModel)
            TempOffsetPicker(header = "Temperature Offset", state = valuesPickerState, items = temperatureOffsetsList, onChanged = { it: String -> viewModel.viewState.value.temperatureOffset = it.toDouble() }, startIndex = temperatureOffsetsList.indexOf(viewModel.viewState.value.temperatureOffset.toString()), visibleItemsCount = 3, textModifier = Modifier.padding(8.dp), textStyle = TextStyle(fontSize = 18.sp))
        }
    }

    @Composable
    fun AutoForcedOccupiedAutoAwayConfig(modifier: Modifier = Modifier) {

        Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(20.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 10.dp, end = 40.dp)) {
                    StyledTextView(text = "Auto Force Occupied", fontSize = 20, textAlignment = TextAlign.Start)
                }
                ToggleButtonStateful(defaultSelection = viewModel.viewState.value.isEnableAutoForceOccupied, onEnabled = { viewModel.viewState.value.isEnableAutoForceOccupied = it })

                Box(modifier = Modifier.padding(start = 50.dp, end = 50.dp , top = 20.dp, bottom = 20.dp))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 10.dp, end = 40.dp)) {
                    StyledTextView(text = "Auto Away", fontSize = 20, textAlignment = TextAlign.Start)
                }
                ToggleButtonStateful(defaultSelection = viewModel.viewState.value.isEnableAutoAway, onEnabled = { viewModel.viewState.value.isEnableAutoAway = it })
            }

        }
    }

    @Composable
    fun Label(modifier: Modifier = Modifier) {
        Row(modifier = modifier
                .fillMaxWidth()
                .padding(top = 5.dp, bottom = 10.dp)) {
            Box(modifier = Modifier.weight(1f))
            Box(modifier = Modifier
                    .weight(3.5f)
                    .padding(start = 10.dp), contentAlignment = Alignment.Center) {
                SubTitle(ENABLE)
            }
            Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
                SubTitle(MAPPING)
            }
            Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
                SubTitle(TEST_SIGNAL)
            }
        }
    }


    /**
     * We can use same function until we need any profile specific changes. It is open to override
     * if any profile specific changes required.
     */
    @Composable
    open fun Configurations(modifier: Modifier) {
        Row(modifier = modifier.fillMaxWidth()) {
            Image(painter = painterResource(id = R.drawable.input_hyperstat_cpu), contentDescription = "Relays", modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 25.dp)
                    .height(805.dp))

            Column(modifier = Modifier.weight(4f)) {
                DrawRelays()
                DrawAnalogOutput()
                DrawThermistors()
                DrawAnalogIn()
            }
        }
    }


    @Composable
    open fun DrawRelays() {
        val relayEnums =
            viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.equipModel)
        viewModel.viewState.value.apply {
            repeat(6) { index ->
                val relayConfig = when (index) {
                    0 -> relay1Config
                    1 -> relay2Config
                    2 -> relay3Config
                    3 -> relay4Config
                    4 -> relay5Config
                    5 -> relay6Config
                    else -> throw IllegalArgumentException("Invalid relay index: $index")
                }

                RelayConfiguration(relayName = "Relay ${index + 1}", enabled = relayConfig.enabled, onEnabledChanged = {
                    relayConfig.enabled = it

                }, association = relayEnums[relayConfig.association], unit = "", relayEnums = relayEnums, onAssociationChanged = { associationIndex ->
                    relayConfig.association = associationIndex.index
                }, isEnabled = relayConfig.enabled, testState = false,
                        onTestActivated = { viewModel.updateTestRelay(relayConfig, it) }, padding = 7)
            }
        }
    }

    @Composable
    open fun DrawAnalogOutput() {
        val analogEnum = viewModel.getAllowedValues(DomainName.analog1OutputAssociation, viewModel.equipModel)
        repeat(3) { index ->
            val analogOut = "Analog-Out${index + 1}"
            val enabled = when (index) {
                0 -> viewModel.viewState.value.analogOut1Enabled
                1 -> viewModel.viewState.value.analogOut2Enabled
                2 -> viewModel.viewState.value.analogOut3Enabled
                else -> false
            }
            val associationIndex = when (index) {
                0 -> viewModel.viewState.value.analogOut1Association
                1 -> viewModel.viewState.value.analogOut2Association
                2 -> viewModel.viewState.value.analogOut3Association
                else -> 0
            }

            AnalogOutConfig(analogOutName = analogOut, enabled = enabled, onEnabledChanged = { enabledAction ->
                when (index) {
                    0 -> viewModel.viewState.value.analogOut1Enabled = enabledAction
                    1 -> viewModel.viewState.value.analogOut2Enabled = enabledAction
                    2 -> viewModel.viewState.value.analogOut3Enabled = enabledAction
                }

            }, association = analogEnum[associationIndex], analogOutEnums = analogEnum, testSingles = viewModel.testVoltage, isEnabled = enabled, onAssociationChanged = { association ->
                when (index) {
                    0 -> viewModel.viewState.value.analogOut1Association = association.index
                    1 -> viewModel.viewState.value.analogOut2Association = association.index
                    2 -> viewModel.viewState.value.analogOut3Association = association.index
                }
            }, testVal = viewModel.getAnalogOutValue(index+1) ?: 0.0, onTestSignalSelected = { viewModel.updateTestAnalogOut(index+1 , it.toInt()) }, padding = 7)
        }
    }

    @Composable
    open fun DrawThermistors() {
        // this is specific because CPU has 2 different list for profiles
        val thermistor1Enums = viewModel.getAllowedValues(DomainName.thermistor1InputAssociation, viewModel.equipModel)
        val thermistor2Enums = viewModel.getAllowedValues(DomainName.thermistor2InputAssociation, viewModel.equipModel)
        Column(modifier = Modifier.padding(top = 5.dp)) {
            AOTHConfig(name = THERMISTOR_1, default = viewModel.viewState.value.thermistor1Config.enabled, onEnabled = { viewModel.viewState.value.thermistor1Config.enabled = it }, spinnerDefault = thermistor1Enums[viewModel.viewState.value.thermistor1Config.association], items = thermistor1Enums, unit = "", isEnabled = viewModel.viewState.value.thermistor1Config.enabled, onSelect = { viewModel.viewState.value.thermistor1Config.association = it.index }, padding = 5)
            AOTHConfig(name = THERMISTOR_2, default = viewModel.viewState.value.thermistor2Config.enabled, onEnabled = { viewModel.viewState.value.thermistor2Config.enabled = it }, spinnerDefault = thermistor2Enums[viewModel.viewState.value.thermistor2Config.association], items = thermistor2Enums, unit = "", isEnabled = viewModel.viewState.value.thermistor2Config.enabled, onSelect = { viewModel.viewState.value.thermistor2Config.association = it.index }, padding = 5)

        }
    }

    @Composable
    fun DrawAnalogIn() {
        val analogInEnums = viewModel.getAllowedValues(DomainName.analog1InputAssociation, viewModel.equipModel)
        Column(modifier = Modifier.padding(top = 45.dp)) {
            AOTHConfig(name = ANALOG_IN1, default = viewModel.viewState.value.analogIn1Config.enabled, onEnabled = { viewModel.viewState.value.analogIn1Config.enabled = it }, spinnerDefault = analogInEnums[viewModel.viewState.value.analogIn1Config.association], items = analogInEnums, unit = "", isEnabled = viewModel.viewState.value.analogIn1Config.enabled, onSelect = { viewModel.viewState.value.analogIn1Config.association = it.index }, padding = 5)
            AOTHConfig(name = ANALOG_IN2, default = viewModel.viewState.value.analogIn2Config.enabled, onEnabled = { viewModel.viewState.value.analogIn2Config.enabled = it }, spinnerDefault = analogInEnums[viewModel.viewState.value.analogIn2Config.association], items = analogInEnums, unit = "", isEnabled = viewModel.viewState.value.analogIn2Config.enabled, onSelect = { viewModel.viewState.value.analogIn2Config.association = it.index }, padding = 5)
        }

    }

    @Composable
    fun ThresholdTargetConfig(viewModel: HyperStatViewModel,modifier: Modifier = Modifier) {
        val co2ThresholdOptions = viewModel.getOptionByDomainName(DomainName.co2Threshold, viewModel.equipModel, true)
        val co2Unit = viewModel.getUnit(DomainName.co2Threshold, viewModel.equipModel)
        val pm25Unit = viewModel.getUnit(DomainName.pm25Target, viewModel.equipModel)
        val pm25ThresholdOptions = viewModel.getOptionByDomainName(DomainName.pm25Target, viewModel.equipModel, true)
        Column(modifier = modifier.padding(start = 25.dp, top = 25.dp)) {
            MinMaxConfiguration("CO2 Threshold", "CO2 Target", itemList = co2ThresholdOptions, co2Unit, minDefault = viewModel.viewState.value.co2Config.threshold.toInt().toString(), maxDefault = viewModel.viewState.value.co2Config.target.toInt().toString(), onMinSelected = { viewModel.viewState.value.co2Config.threshold = it.value.toDouble() }, onMaxSelected = { viewModel.viewState.value.co2Config.target = it.value.toDouble() })

            Row {
                Row(modifier = Modifier
                        .width(550.dp)
                        .padding(bottom = 10.dp)) {

                    Box(modifier = Modifier
                            .weight(1f)
                            .padding(top = 10.dp)) {
                        StyledTextView("PM 2.5 Target", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = Modifier
                            .weight(1f)
                            .padding(top = 5.dp)) {
                        SpinnerElementOption(viewModel.viewState.value.pm2p5Config.target.toInt().toString(), pm25ThresholdOptions, pm25Unit,
                                itemSelected = { viewModel.viewState.value.pm2p5Config.target = it.value.toDouble() }, viewModel = null)
                    }
                }

                if (viewModel.viewState.value.isDcvMapped()) {
                    Row(modifier = Modifier
                            .width(550.dp)
                            .padding(bottom = 10.dp)) {

                        Box(modifier = Modifier
                                .weight(1f)
                                .padding(top = 10.dp)) {
                            StyledTextView("CO2 Damper Opening Rate", fontSize = 20, textAlignment = TextAlign.Left)
                        }
                        Box(modifier = Modifier
                                .weight(1f)
                                .padding(top = 5.dp)) {
                            SpinnerElementOption(viewModel.viewState.value.damperOpeningRate.toString(), viewModel.damperOpeningRate, "%", itemSelected = { viewModel.viewState.value.damperOpeningRate = it.value.toInt() }, viewModel = null)
                        }
                    }
                }

            }
        }
    }

    @Composable
    fun DisplayInDeviceConfig(viewModel: HyperStatViewModel,modifier: Modifier = Modifier) {

        val context = LocalContext.current
        var humidityDisplay by remember { mutableStateOf(viewModel.viewState.value.humidityDisplay) }
        var co2Display by remember { mutableStateOf(viewModel.viewState.value.co2Display) }
        var pm25Display by remember { mutableStateOf(viewModel.viewState.value.pm25Display) }

        fun canSelectMore(currentSelections: List<Boolean>): Boolean {
            val selectedCount = currentSelections.count { it }
            if (selectedCount >= 2) {
                Toast.makeText(context, "Only two items can be displayed on the home screen", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        val colors = SwitchDefaults.colors(checkedThumbColor = Color.White, uncheckedThumbColor = Color.White, uncheckedIconColor = greyColor, uncheckedTrackColor = greyColor, checkedIconColor = primaryColor, checkedTrackColor = primaryColor, uncheckedBorderColor = greyColor, checkedBorderColor = primaryColor)
        Column(modifier = modifier.padding(start = 25.dp, top = 25.dp)) {
            BoldStyledTextView("Display in device home screen", fontSize = 20)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)) {
                Box(modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)) {
                    StyledTextView(text = "Humidity", fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    Switch(checked = humidityDisplay, colors = colors, onCheckedChange = {
                        if (!it || canSelectMore(listOf(co2Display, pm25Display))) {
                            humidityDisplay = it
                            viewModel.viewState.value.humidityDisplay = it
                        }
                    }, thumbContent = {
                        Icon(imageVector = if (humidityDisplay) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, modifier = Modifier
                                .size(SwitchDefaults.IconSize)
                                .padding(0.dp))
                    })
                }

                Box(modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)) {
                    StyledTextView(text = "CO2", fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    Switch(checked = co2Display, colors = colors, onCheckedChange = {
                        if (!it || canSelectMore(listOf(humidityDisplay, pm25Display))) {
                            co2Display = it
                            viewModel.viewState.value.co2Display = it
                        }
                    }, thumbContent = {
                        Icon(imageVector = if (co2Display) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, modifier = Modifier
                                .size(SwitchDefaults.IconSize)
                                .padding(0.dp))
                    })
                }
            }

            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)) {
                Box(modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)) {
                    StyledTextView(text = "PM 2.5", fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    Switch(checked = pm25Display, colors = colors, onCheckedChange = {
                        if (!it || canSelectMore(listOf(humidityDisplay, co2Display))) {
                            pm25Display = it
                            viewModel.viewState.value.pm25Display = it
                        }
                    }, thumbContent = {
                        Icon(imageVector = if (pm25Display) Icons.Filled.Check else Icons.Filled.Close, contentDescription = null, modifier = Modifier
                                .size(SwitchDefaults.IconSize)
                                .padding(0.dp))
                    })
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
    fun MisSettingConfig(viewModel: HyperStatViewModel) {
        var disableTouch by remember { mutableStateOf(viewModel.viewState.value.disableTouch) }
        var enableBrightness by remember { mutableStateOf(viewModel.viewState.value.enableBrightness) }
        val colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            uncheckedThumbColor = Color.White,
            uncheckedIconColor = greyColor,
            uncheckedTrackColor = greyColor,
            checkedIconColor = primaryColor,
            checkedTrackColor = primaryColor,
            uncheckedBorderColor = greyColor,
            checkedBorderColor = primaryColor
        )

        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            BoldStyledTextView("Misc Settings", fontSize = 20)
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
                    StyledTextView(text = "Disable Touch", fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    Switch(checked = disableTouch, colors = colors, onCheckedChange = {
                        disableTouch = it
                        viewModel.viewState.value.disableTouch = it

                    }, thumbContent = {
                        Icon(
                            imageVector = if (disableTouch) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier
                                .size(SwitchDefaults.IconSize)
                                .padding(0.dp)
                        )
                    })
                }

                Box(
                    modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)
                ) {
                    StyledTextView(text = "Enable Brightness", fontSize = 20)
                }
                Box(modifier = Modifier.weight(1f)) {
                    Switch(checked = enableBrightness, colors = colors, onCheckedChange = {
                        enableBrightness = it
                        viewModel.viewState.value.enableBrightness = it
                    }, thumbContent = {
                        Icon(
                            imageVector = if (enableBrightness) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier
                                .size(SwitchDefaults.IconSize)
                                .padding(0.dp)
                        )
                    })
                }
            }
        }
    }

    @Composable
    fun DividerRow(modifier: Modifier = Modifier) {
        Spacer(modifier = Modifier.height(20.dp))
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
    fun SaveConfig(viewModel: HyperStatViewModel,modifier: Modifier = Modifier) {
        Row(modifier = modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 20.dp)), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)), contentAlignment = Alignment.Center) {
                SaveTextView(SAVE) {
                    CcuLog.i(L.TAG_CCU_SYSTEM, viewModel.viewState.toString())
                    viewModel.saveConfiguration()
                }
            }
        }
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

    @Composable
    fun FanLowMediumHighConfigurations(firstLabel: String, secondLabel: String, thirdLabel: String, firstDefault: String, secondDefault: String, thirdDefault: String, onFirstSelected: (Option) -> Unit = {}, onSecondSelected: (Option) -> Unit = {}, onThirdSelected: (Option) -> Unit = {}, itemList: List<Option>, unit: String) {
        Row(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 10.dp)) {
            val rowModifier = Modifier
                    .width(185.dp)
                    .padding(top = 10.dp)
            Box(modifier = rowModifier) {
                StyledTextView(firstLabel, fontSize = 20, textAlignment = TextAlign.Left)
            }
            Box(modifier = rowModifier) {
                SpinnerElementOption(defaultSelection = firstDefault, items = itemList, unit = unit, itemSelected = { onFirstSelected(it) }, viewModel = null)
            }

            Box(modifier = rowModifier) {
                StyledTextView(secondLabel, fontSize = 20, textAlignment = TextAlign.Left)
            }
            Box(modifier = rowModifier) {
                SpinnerElementOption(defaultSelection = secondDefault, items = itemList, unit = unit, itemSelected = { onSecondSelected(it) }, viewModel = null)
            }
            Box(modifier = rowModifier) {
                StyledTextView(thirdLabel, fontSize = 20, textAlignment = TextAlign.Left)
            }
            Box(modifier = rowModifier) {
                SpinnerElementOption(defaultSelection = thirdDefault, items = itemList, unit = unit, itemSelected = { onThirdSelected(it) }, viewModel = null)
            }
        }
    }

    @Composable
    fun ShowProgressBar() {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }

}