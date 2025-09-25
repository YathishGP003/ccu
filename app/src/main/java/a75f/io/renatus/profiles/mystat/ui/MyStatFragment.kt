package a75f.io.renatus.profiles.mystat.ui

import a75f.io.domain.api.Domain.getListByDomainName
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.R
import a75f.io.renatus.composables.DependentPointMappingView
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.PinPassword
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.composables.TempOffsetPicker
import a75f.io.renatus.composables.UniversalOutConfiguration
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.BoldStyledTextView
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.LabelWithToggleButton
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SearchSpinnerElement
import a75f.io.renatus.compose.SpinnerElementOption
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.showToast
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hss.HyperStatSplitFragment.Companion.CONDITIONING_ACCESS
import a75f.io.renatus.profiles.hss.HyperStatSplitFragment.Companion.INSTALLER_ACCESS
import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.mystat.getAllowedValues
import a75f.io.renatus.profiles.mystat.minMaxVoltage
import a75f.io.renatus.profiles.mystat.testVoltage
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatHpuViewModel
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatPipe2ViewModel
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatViewModel
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment.Companion.DividerRow
import a75f.io.renatus.profiles.system.ENABLE
import a75f.io.renatus.profiles.system.MAPPING
import a75f.io.renatus.profiles.system.SUPPLY_WATER_TEMPERATURE
import a75f.io.renatus.profiles.system.TEST_SIGNAL
import a75f.io.renatus.profiles.system.UNIVERSAL_IN
import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.viewModels
import org.projecthaystack.util.Base64

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

    private fun getHpuDisabledIndices(config: ConfigState): List<Int> {
        return if (viewModel.viewState.value.isAnyRelayMapped(
                MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal,
                config)) {
            listOf(MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)
        } else if (viewModel.viewState.value.isAnyRelayMapped(
                MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal,
                config)) {
            listOf(MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
        } else { emptyList() }
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
                header = stringResource(R.string.temperature_offset),
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
                        text = stringResource(R.string.auto_force_occupy), fontSize = 20, textAlignment = TextAlign.Start
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
                        text = stringResource(R.string.auto_away), fontSize = 20, textAlignment = TextAlign.Start
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
    open fun MyStatConfiguration() {
        Row {
            Image(
                painter = painterResource(id = R.drawable.ms),
                contentDescription = "Relays",
                modifier = Modifier
                    .width(350.dp)
                    .height(350.dp)
                    .padding(start = 10.dp, top = 10.dp, bottom = 10.dp)
            )
            Column {
                val relayEnums = getAllowedValues(DomainName.relay1OutputAssociation, viewModel.equipModel)
                viewModel.viewState.value.apply {
                    repeat(3) { index ->
                        val relayConfig = when (index) {
                            0 -> relay1Config
                            1 -> relay2Config
                            2 -> relay3Config
                            else -> throw IllegalArgumentException("Invalid relay index: $index")
                        }
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
                            padding = (5),
                            disabledIndices = if (viewModel is MyStatHpuViewModel) getHpuDisabledIndices(relayConfig) else emptyList()
                        )

                    }
                    val universalEnums = getAllowedValues(
                        DomainName.universal1OutputAssociation,
                        viewModel.equipModel
                    )
                    UniversalOutConfiguration(
                        portName = "Universal-Out 1",
                        enabled = universalOut1.enabled,
                        onEnabledChanged = {
                            universalOut1.enabled = it
                        },
                        association = universalEnums[universalOut1.association],
                        enumOptions = universalEnums,
                        unit = "",
                        isEnabled = universalOut1.enabled,
                        onAssociationChanged = { associationIndex ->
                            universalOut1.association = associationIndex.index
                        },
                        testState = viewModel.getRelayStatus(4),
                        onTestActivated = {
                            viewModel.sendTestSignal(4, if (it) 1.0 else 0.0)
                        },
                        testSingles = testVoltage,
                        testVal = viewModel.getAnalogValue(4),
                        padding = 5,
                        analogStartPosition = viewModel.getAnalogStatIndex(),
                        onTestSignalSelected = { viewModel.sendTestSignal(universalOut1 =  it) },
                        disabledIndices = if (viewModel is MyStatHpuViewModel) getHpuDisabledIndices(universalOut1) else emptyList()
                    )
                    UniversalOutConfiguration(
                        portName = "Universal-Out 2",
                        enabled = universalOut2.enabled,
                        onEnabledChanged = {
                            universalOut2.enabled = it
                        },
                        association = universalEnums[universalOut2.association],
                        enumOptions = universalEnums,
                        unit = "",
                        isEnabled = universalOut2.enabled,
                        onAssociationChanged = { associationIndex ->
                            universalOut2.association = associationIndex.index
                        },
                        testState = viewModel.getRelayStatus(5),
                        onTestActivated = {
                            viewModel.sendTestSignal(5, if (it) 1.0 else 0.0)
                        },
                        testSingles = testVoltage,
                        testVal = viewModel.getAnalogValue(5),
                        padding = 5,
                        onTestSignalSelected = { viewModel.sendTestSignal(universalOut2 =  it) },
                        analogStartPosition = viewModel.getAnalogStatIndex(),
                        disabledIndices = if (viewModel is MyStatHpuViewModel) getHpuDisabledIndices(universalOut2) else emptyList()
                    )

                }
                if (viewModel is MyStatPipe2ViewModel) {
                    Row(modifier = Modifier.wrapContentWidth()) {
                        Box(modifier = Modifier
                            .weight(4f)
                            .padding(end = 5.dp, top = 10.dp)) {
                            DependentPointMappingView(
                                toggleName = UNIVERSAL_IN,
                                toggleState = true,
                                toggleEnabled = {  },
                                mappingText = SUPPLY_WATER_TEMPERATURE,
                                false
                            )
                        }
                    }
                } else {
                    val universalOptions = getAllowedValues(DomainName.universalIn1Association, viewModel.equipModel)
                    Row(modifier = Modifier.wrapContentWidth()) {
                        Box(modifier = Modifier
                            .wrapContentWidth()
                            .padding(start = 5.dp, top = 8.dp)) {
                            ToggleButtonStateful(defaultSelection = viewModel.viewState.value.universalIn1.enabled) {
                                viewModel.viewState.value.universalIn1.enabled = it
                            }
                        }

                        Box(modifier = Modifier
                            .weight(1.2f)
                            .padding(start = 10.dp, top = 20.dp)) {
                            StyledTextView("Universal-In", fontSize = 20)
                        }
                        Box(modifier = Modifier
                            .weight(4f)
                            .padding(end = 5.dp, top = 10.dp)) {
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 50.dp, top = 30.dp, end = 20.dp)
        ) {
            Box(
                modifier = Modifier.background(
                    primaryColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(all = 20.dp)
            ) {
                StyledTextView(
                    text = "Please check Universal-Out 1, Universal-Out 2 and CO2 is supported/configured correctly via jumpers on the MyStat device.",
                    fontSize = 24,
                    textAlignment = TextAlign.Left

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
            ) { StyledTextView(stringResource(R.string.co2_control), fontSize = 20) }

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
                        StyledTextView(stringResource(R.string.co2_damper_opening_rate), fontSize = 20, textAlignment = TextAlign.Left)
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
    fun ConfigMinMax(association: Int, minMax: MinMaxConfig, displayName: String, mapping: Int, index: Int) {
        if (association == mapping) {
            MinMaxConfiguration(
                minLabel = "Universal-Out $index Min \n${displayName}",
                maxLabel = "Universal-Out $index Max \n${displayName}",
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
            Text(text = stringResource(R.string.loading_profile_configuration))
        }
    }


    @Composable
    fun DisplayInDeviceConfig(viewModel: MyStatViewModel, modifier: Modifier = Modifier) {
        Column(modifier = modifier.padding(start = 25.dp, top = 25.dp)) {
            BoldStyledTextView("Display in Device Home Screen (Select only 1)", fontSize = 20)
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
                        text = viewModel.profileConfiguration.spaceTemp.disName, fontSize = 20
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(defaultSelection = viewModel.viewState.value.spaceTemp,
                        onEnabled = {
                            if (it && viewModel.viewState.value.desiredTemp) {
                                showToast(
                                    "Max of 1 temperature can be displayed at once",
                                    requireContext()
                                )
                            } else {
                                viewModel.viewState.value.desiredTemp = true
                                viewModel.viewState.value.spaceTemp = it
                                showToast(
                                    "Min of 1 temperature should be displayed", requireContext()
                                )
                            }
                        })
                }
                Box(
                    modifier = Modifier
                        .weight(4f)
                        .padding(top = 10.dp)
                ) {
                    StyledTextView(
                        text = viewModel.profileConfiguration.desiredTemp.disName, fontSize = 20
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ToggleButton(defaultSelection = viewModel.viewState.value.desiredTemp,
                        onEnabled = {
                            if (it && viewModel.viewState.value.spaceTemp) {
                                showToast(
                                    "Max of 1 temperature can be displayed at once",
                                    requireContext()
                                )
                            } else {
                                viewModel.viewState.value.spaceTemp = true
                                viewModel.viewState.value.desiredTemp = it
                                showToast(
                                    "Min of 1 temperature should be displayed", requireContext()
                                )
                            }
                        })
                }
            }
        }
        DividerRow()

    }


    @SuppressLint("UnrememberedMutableState")
    @Composable
    fun PinPasswordView(viewModel: MyStatViewModel) {

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
            BoldStyledTextView("PIN Lock", fontSize = 20)
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
                        onCancel(selectedPinTitle, false)
                    },
                    selectedPinTitle = selectedPinTitle,
                    pinDigits = when (selectedPinTitle) {
                        INSTALLER_ACCESS -> installerPin
                        CONDITIONING_ACCESS -> conditioningPin
                        else -> mutableStateListOf(0, 0, 0, 0)
                    }
                )
                {
                    // Save the PIN configuration
                    when (selectedPinTitle) {
                        INSTALLER_ACCESS -> {
                            viewModel.viewState.value.installerPassword =
                                Base64.STANDARD.encode(installerPin.joinToString(separator = "") { it.toString() })
                        }

                        CONDITIONING_ACCESS -> {
                            viewModel.viewState.value.conditioningModePassword =
                                Base64.STANDARD.encode(conditioningPin.joinToString(separator = "") { it.toString() })
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
                Text("PIN Lock: $selectedPinTitle", fontSize = 23.sp, color = Color.Black, fontWeight = FontWeight.Bold,fontFamily = myFontFamily , modifier = Modifier.padding(start = 10.dp) )
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
                        Text("CANCEL", color = primaryColor, fontSize = 22.sp, fontFamily = myFontFamily)
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
                            "SAVE",
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


}