package a75f.io.renatus.profiles.mystat.ui

import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.R
import a75f.io.renatus.composables.AnalogOutConfig
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.composables.TempOffsetPicker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SearchSpinnerElement
import a75f.io.renatus.compose.SpinnerElementOption
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.mystat.getAllowedValues
import a75f.io.renatus.profiles.mystat.minMaxVoltage
import a75f.io.renatus.profiles.mystat.testVoltage
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatViewModel
import a75f.io.renatus.profiles.system.ENABLE
import a75f.io.renatus.profiles.system.MAPPING
import a75f.io.renatus.profiles.system.TEST_SIGNAL
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels

/**
 * Created by Manjunath K on 15-01-2025.
 */

abstract class MyStatFragment : BaseDialogFragment(), OnPairingCompleteListener {

    open val viewModel: MyStatViewModel by viewModels()

    override fun onDestroy() {
        super.onDestroy()
        if (Globals.getInstance().isTestMode) {
            Globals.getInstance().isTestMode = false
        }
    }

    @Composable
    fun SaveConfig(viewModel: MyStatViewModel) {
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
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.Center
            ) {
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
    fun TempOffset() {
        val valuesPickerState = rememberPickerState()
        Column(modifier = Modifier.padding(start = 400.dp, end = 400.dp, top = 20.dp)) {
            val temperatureOffsetsList =
                getListByDomainName(DomainName.temperatureOffset, viewModel.equipModel)
            TempOffsetPicker(
                header = "Temperature Offset",
                state = valuesPickerState,
                items = temperatureOffsetsList,
                onChanged = { it: String ->
                    viewModel.viewState.value.temperatureOffset = it.toDouble()
                },
                startIndex = temperatureOffsetsList.indexOf(viewModel.viewState.value.temperatureOffset.toString()),
                visibleItemsCount = 3,
                textModifier = Modifier.padding(8.dp),
                textStyle = TextStyle(fontSize = 18.sp)
            )
        }
    }

    @Composable
    fun AutoForcedOccupiedAutoAwayConfig() {

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.padding(20.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 10.dp, end = 40.dp)
                ) {
                    StyledTextView(
                        text = "Auto Force Occupied", fontSize = 20, textAlignment = TextAlign.Start
                    )
                }
                ToggleButtonStateful(defaultSelection = viewModel.viewState.value.isEnableAutoForceOccupied,
                    onEnabled = { viewModel.viewState.value.isEnableAutoForceOccupied = it })

                Box(
                    modifier = Modifier.padding(
                        start = 50.dp, end = 50.dp, top = 20.dp, bottom = 20.dp
                    )
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 10.dp, end = 40.dp)
                ) {
                    StyledTextView(
                        text = "Auto Away", fontSize = 20, textAlignment = TextAlign.Start
                    )
                }
                ToggleButtonStateful(defaultSelection = viewModel.viewState.value.isEnableAutoAway,
                    onEnabled = { viewModel.viewState.value.isEnableAutoAway = it })
            }
        }
    }

    @Composable
    fun Label() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, bottom = 10.dp)
        ) {
            Box(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .weight(3.5f)
                    .padding(start = 10.dp),
                contentAlignment = Alignment.Center
            ) {
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

    @Composable
    open fun MyStatDrawRelays() {
        Row {
            Image(
                painter = painterResource(id = R.drawable.ms_relays),
                contentDescription = "Relays",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 8.dp)
                    .height(240.dp)
            )
            Column(modifier = Modifier.weight(4f)) {
                val relayEnums =
                    getAllowedValues(DomainName.relay1OutputAssociation, viewModel.equipModel)
                viewModel.viewState.value.apply {
                    repeat(4) { index ->
                        val relayConfig = when (index) {
                            0 -> relay1Config
                            1 -> relay2Config
                            2 -> relay3Config
                            3 -> relay4Config
                            else -> throw IllegalArgumentException("Invalid relay index: $index")
                        }
                        val padding = (index + 1 ) + 2
                        RelayConfiguration(
                            relayName = "Relay ${index + 1}",
                            enabled = relayConfig.enabled,
                            onEnabledChanged = { relayConfig.enabled = it },
                            association = relayEnums[relayConfig.association],
                            unit = "",
                            relayEnums = relayEnums,
                            onAssociationChanged = { associationIndex ->
                                relayConfig.association = associationIndex.index
                            },
                            isEnabled = relayConfig.enabled,
                            testState = viewModel.getRelayStatus(index + 1),
                            onTestActivated = {
                                viewModel.sendTestSignal(index + 1, if (it) 1.0 else 0.0)
                            },
                            padding = (4 + padding)
                        )
                    }
                }
            }
        }
    }

    @Composable
    open fun DrawAnalogOutput() {
        Row {
            Image(
                painter = painterResource(id = R.drawable.my_ao),
                contentDescription = "DrawAnalogOutput",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 10.dp)
                    .height(34.dp)
            )
            Column(modifier = Modifier.weight(4f)) {
                val analogEnum =
                    getAllowedValues(DomainName.analog1OutputAssociation, viewModel.equipModel)
                AnalogOutConfig(
                    analogOutName = "Analog-Out",
                    enabled = viewModel.viewState.value.analogOut1Enabled,
                    onEnabledChanged = { enabledAction ->
                        viewModel.viewState.value.analogOut1Enabled = enabledAction
                    },
                    association = analogEnum[viewModel.viewState.value.analogOut1Association],
                    analogOutEnums = analogEnum,
                    testSingles = testVoltage,
                    isEnabled = viewModel.viewState.value.analogOut1Enabled,
                    onAssociationChanged = { association ->
                        viewModel.viewState.value.analogOut1Association = association.index
                    },
                    testVal = viewModel.getAnalogValue(),
                    onTestSignalSelected = { viewModel.sendTestSignal(5, 1.0, it) },
                    padding = 7
                )
            }
        }
    }

    @Composable
    fun Co2Control() {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .padding(top = 30.dp, bottom = 10.dp, start = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 10.dp, start = 15.dp)
            ) { StyledTextView("CO2 Control", fontSize = 20) }

            Spacer(modifier = Modifier.weight(1f)) // Pushes the toggle to the end

            Box(modifier = Modifier.wrapContentWidth()) {
                ToggleButtonStateful(defaultSelection = viewModel.viewState.value.co2Control) {
                    viewModel.viewState.value.co2Control = it
                }
            }
        }

    }

    @Composable
    fun ThresholdTargetConfig() {
        if (viewModel.viewState.value.co2Control) {
            val co2ThresholdOptions = viewModel.getOptionByDomainName(DomainName.co2Threshold, viewModel.equipModel, true)
            val co2Unit = viewModel.getUnit(DomainName.co2Threshold, viewModel.equipModel)
            Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
                MinMaxConfiguration(
                    "CO2 Threshold", "CO2 Target",
                    itemList = co2ThresholdOptions, co2Unit, minDefault = viewModel.viewState.value.co2Threshold.toInt().toString(),
                    maxDefault = viewModel.viewState.value.co2Target.toInt().toString(),
                    onMinSelected = { viewModel.viewState.value.co2Threshold = it.value.toDouble() },
                    onMaxSelected = { viewModel.viewState.value.co2Target = it.value.toDouble() }
                )

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
                        SpinnerElementOption(viewModel.viewState.value.co2DamperOperatingRate.toInt().toString(), viewModel.damperOpeningRate, "%",
                            itemSelected = { viewModel.viewState.value.co2DamperOperatingRate = it.value.toDouble() }, viewModel = null)
                    }
                }
            }
        }

    }



    @Composable
    open fun UniversalInput() {
        Row {
            Image(
                painter = painterResource(id = R.drawable.universal),
                contentDescription = "UniversalInput",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 10.dp)
                    .height(34.dp)
            )
            Column(modifier = Modifier.weight(4f)) {
                val universalOptions = getAllowedValues(DomainName.universalIn1Association, viewModel.equipModel)
                Row(modifier = Modifier.wrapContentWidth()) {
                    Box(modifier = Modifier.wrapContentWidth().padding(start = 9.dp, top = 8.dp)) {
                        ToggleButtonStateful(defaultSelection = viewModel.viewState.value.universalIn1.enabled) {
                            viewModel.viewState.value.universalIn1.enabled = it
                        }
                    }

                    Box(modifier = Modifier.weight(1.2f).padding(start = 10.dp, top = 20.dp)) {
                        StyledTextView("Universal-In", fontSize = 20)
                    }
                    Box(modifier = Modifier.weight(4f).padding(end = 5.dp, top = 10.dp)) {
                        SearchSpinnerElement(
                            default = universalOptions[viewModel.viewState.value.universalIn1.association],
                            allItems = universalOptions,
                            unit = "",
                            onSelect = { viewModel.viewState.value.universalIn1.association = it.index },
                            width = 400,
                            isEnabled = viewModel.viewState.value.universalIn1.enabled
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ConfigMinMax(association: Int, minMax: MinMaxConfig, displayName: String, mapping: Int) {
        if (association == mapping) {
            MinMaxConfiguration(
                minLabel = "Analog-out at Min \n${displayName}",
                maxLabel = "Analog-out at Max \n${displayName}",
                itemList = minMaxVoltage,
                unit = "V",
                minDefault = minMax.min.toString(),
                maxDefault = minMax.max.toString(),
                onMinSelected = { minMax.min = it.value.toInt() },
                onMaxSelected = { minMax.max = it.value.toInt() }
            )
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
            Text(text = "Loading Profile Configuration")
        }
    }
}