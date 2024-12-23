package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.DeviceUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.SimpleSwitch
import a75f.io.renatus.composables.SwitchWithLabel
import a75f.io.renatus.composables.SystemAnalogOutMappingView
import a75f.io.renatus.composables.SystemAnalogOutMappingViewButtonComposable
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

open class DModulatingRtuFragment : Fragment() {
    @Composable
    fun AnalogOutComposable(
        viewModel: DabModulatingRtuViewModel,
        dcwbEnabledMutableState: Boolean
    ) {

        Spacer(modifier = Modifier.height(12.dp))
        SystemAnalogOutDynamicMappingView(
            analogName = "Analog-Out 1",
            analogOutState = viewModel.viewState.value.isAnalog1OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog1OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                    "Analog 1 Output",
                    it,
                    viewModel.profileConfiguration
                )
            },
            mappingText = if(dcwbEnabledMutableState) "CHW valve" else "Cooling",
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =
                try {
                    (0..100).map { it }.indexOf(Domain.cmBoardDevice.analog1Out.readHisVal().toInt())
                } catch (e: UninitializedPropertyAccessException) {
                    // When the cmBoardDevice is uninitialized after registration
                    (0..100).map { it }.indexOf(20)
                },
            onAnalogOutChanged = {
                viewModel.viewState.value.analogOut1CoolingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog1Out, it.toDouble())
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            mappingTextSpacer = if(dcwbEnabledMutableState) 163 else 193
        )
        Spacer(modifier = Modifier.height(14.dp))

        SystemAnalogOutMappingView(
            analogName = "Analog-Out 2",
            analogOutState = viewModel.viewState.value.isAnalog2OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog2OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                    "Analog 2 Output",
                    it,
                    viewModel.profileConfiguration
                )
            },
            mappingText = "Fan Speed",
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =
                try {
                    (0..100).map { it }.indexOf(Domain.cmBoardDevice.analog2Out.readHisVal().toInt())
                } catch (e: UninitializedPropertyAccessException) {
                    // When the cmBoardDevice is uninitialized after registration
                    (0..100).map { it }.indexOf(20)
                },
            onAnalogOutChanged = {
                viewModel.viewState.value.analogOut2FanSpeedTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog2Out, it.toDouble())
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            mappingTextSpacer = 163
        )
        Spacer(modifier = Modifier.height(16.dp))

        SystemAnalogOutMappingView(
            analogName = "Analog-Out 3",
            analogOutState = viewModel.viewState.value.isAnalog3OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog3OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                    "Analog 3 Output",
                    it,
                    viewModel.profileConfiguration
                )
            },
            mappingText = "Heating",
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =
                try {
                    (0..100).map { it }.indexOf(Domain.cmBoardDevice.analog3Out.readHisVal().toInt())
                } catch (e: UninitializedPropertyAccessException) {
                    // When the cmBoardDevice is uninitialized after registration
                    (0..100).map { it }.indexOf(20)
                },
            onAnalogOutChanged = {
                viewModel.viewState.value.analogOut3HeatingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog3Out, it.toDouble())
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            mappingTextSpacer = 190
        )
    }

    @Composable
    fun DModulatingRtuFragment.RelayComposable(viewModel: DabModulatingRtuViewModel) {
        Spacer(modifier = Modifier.height(14.dp))
        SystemAnalogOutMappingViewButtonComposable(
            analogName = "Relay 3",
            analogOutState = viewModel.viewState.value.isRelay3OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isRelay3OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                    "Relay 3",
                    it,
                    viewModel.profileConfiguration
                )
            },
            mappingText = "Fan Enable",
            paddingLimitEnd = 0,
            buttonState =
            try {
                Domain.cmBoardDevice.relay3.readHisVal() > 0
            } catch (e: UninitializedPropertyAccessException) {
                // When the cmBoardDevice is uninitialized after registration
                false
            },
            onTestActivated = {
                viewModel.sendTestCommand(DomainName.relay3, it)
            }
        )
        Spacer(modifier = Modifier.height(14.dp))
        SystemRelayMappingView(
            relayText = "Relay 7",
            relayState = viewModel.viewState.value.isRelay7OutputEnabled,
            onRelayEnabled = {
                viewModel.viewState.value.isRelay7OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                    "Relay 7",
                    it,
                    viewModel.profileConfiguration
                )
            },
            mappingSelection = viewModel.viewState.value.relay7Association,
            mapping = viewModel.relay7AssociationList,
            onMappingChanged = {
                viewModel.viewState.value.relay7Association = it
                viewModel.setStateChanged()
            },
            buttonState =
            try {
                Domain.cmBoardDevice.relay7.readHisVal() > 0
            } catch (e: UninitializedPropertyAccessException) {
                // When the cmBoardDevice is uninitialized after registration
                false
            },
            onTestActivated = {
                viewModel.sendTestCommand(DomainName.relay7, it)
            },
        )
    }

    @Composable
    fun SystemRelayMappingView(
        relayText: String,
        relayState: Boolean = false,
        onRelayEnabled: (Boolean) -> Unit,
        mappingSpace: Dp = 75.dp,
        mapping: List<String>,
        mappingSelection: Int = 0,
        onMappingChanged: (Int) -> Unit,
        buttonState: Boolean = false,
        onTestActivated: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, end = 0.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Row {
                SimpleSwitch(isChecked = relayState, onCheckedChange = onRelayEnabled)
                Spacer(modifier = Modifier.width(30.dp))
                Column {
                    Spacer(modifier=Modifier.height(6.dp))
                    Text(text = relayText, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(mappingSpace))
            DropDownWithLabel(
                label = "",
                list = mapping,
                previewWidth = 235,
                expandedWidth = 253,
                heightValue = 100,
                onSelected = onMappingChanged,
                defaultSelection = mappingSelection,
                isEnabled = relayState
            )

            var buttonState by remember { mutableStateOf(buttonState) }
            var text by remember { mutableStateOf("OFF") }
            Spacer(modifier =Modifier.width(102.dp))
            Button(
                onClick = {
                    buttonState = !buttonState
                    text = when (buttonState) {
                        true -> "ON"
                        false -> "OFF"
                    }
                    onTestActivated(buttonState)
                },
                colors = ButtonDefaults.buttonColors(
                    contentColor = when (buttonState) {
                        true -> ComposeUtil.primaryColor
                        false -> ComposeUtil.greyColor
                    },
                    containerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(
                    1.dp, when (buttonState) {
                        true -> ComposeUtil.primaryColor
                        false -> ComposeUtil.greyColor
                    }
                ),
                modifier = Modifier
                    .width(80.dp)
                    .height(44.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(text = when(buttonState) {
                    true -> "ON"
                    false -> "OFF"
                }, fontSize = 20.sp,
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.wrapContentSize(Alignment.Center))
            }
        }
    }

    /*
    * This function is used to display the Analog Out Mapping view with the list items for selection
    * */
    @Composable
    fun SystemAnalogOutMappingViewWithList(
        analogName: String,
        analogOutState: Boolean = false,
        onAnalogOutEnabled: (Boolean) -> Unit,
        mappingSpace: Dp = 75.dp,
        mapping: List<String>,
        analogOutValList: List<String>,
        mappingSelection: Int = 0,
        onMappingChanged: (Int) -> Unit,
        analogOutVal: Int = 0,
        dropDownWidthPreview: Int = 160,
        dropdownWidthExpanded: Int = 160,
        onAnalogOutChanged: (Int) -> Unit,
        mappingTextSpacer: Int = 155
    ) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 20.dp), horizontalArrangement = Arrangement.Start) {
            Row {
                ToggleButtonStateful(
                    defaultSelection = analogOutState,
                    onEnabled = onAnalogOutEnabled
                )
                Spacer(modifier = Modifier.width(30.dp))
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = analogName, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(mappingSpace))
            DropDownWithLabel(
                label = "",
                list = mapping,
                previewWidth = 235,
                expandedWidth = 253,
                heightValue = 100,
                onSelected = onMappingChanged,
                defaultSelection = mappingSelection,
                isEnabled = true
            )
            Spacer(modifier = Modifier.width(mappingTextSpacer.dp))
            DropDownWithLabel(
                label = "",
                list = analogOutValList,
                previewWidth = dropDownWidthPreview,
                expandedWidth = dropdownWidthExpanded,
                onSelected = onAnalogOutChanged,
                defaultSelection = analogOutVal
            )
        }
    }

    @Composable
    fun DcwbDisabledAnalogView(viewModel: DabModulatingRtuViewModel) {
        val viewState = viewModel.viewState
        val dabModulatingViewModel: DabModulatingRtuViewModel by viewModels()

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(label = "Analog-Out1 at\nMin Cooling",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analogOut1CoolingMin,
                onSelected = {
                    viewState.value.analogOut1CoolingMin = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog1OutputEnabled,
                spacerLimit = 102,
                previewWidth = 100,
                expandedWidth = 120)
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(label = "Analog-Out1 at\nMax Cooling",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analogOut1CoolingMax,
                onSelected = {
                    viewState.value.analogOut1CoolingMax = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog1OutputEnabled,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120)
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(
                label = "Analog-Out2 at\nMin Static Pressure",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog2MinFan,
                onSelected = {
                    viewState.value.analog2MinFan = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog2OutputEnabled,
                spacerLimit = 102,
                previewWidth = 100,
                expandedWidth = 120
            )
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(
                label = "Analog-Out2 at\nMax Static Pressure",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog2MaxFan,
                onSelected = {
                    viewState.value.analog2MaxFan = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog2OutputEnabled,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(
                label = "Analog-Out3 at\nMin Heating",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog3MinHeating,
                onSelected = {
                    viewState.value.analog3MinHeating = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog3OutputEnabled,
                spacerLimit = 102,
                previewWidth = 100,
                expandedWidth = 120
            )
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(
                label = "Analog-Out3 at\nMax Heating",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog3MaxHeating,
                onSelected = {
                    viewState.value.analog3MaxHeating = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog3OutputEnabled,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120
            )
        }
    }

    @Composable
    fun DcwbEnabledAnalogView(viewModel: DabModulatingRtuViewModel, mappedToDamper: Boolean) {
        val viewState = viewModel.viewState
        val dabModulatingViewModel: DabModulatingRtuViewModel by viewModels()
        Spacer(modifier = Modifier.height(14.dp))
        // State variable to track which toggle is selected
        var selectedSwitch by remember { mutableStateOf(viewState.value.ismaximizedExitWaterTempEnable) }

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),  // Makes the row take up the full width of the screen
        ) {
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = "BTU Meter\nStatus",
                fontSize = 20.sp,
            )
            Spacer(modifier = Modifier.width(180.dp))
            if (CCUHsApi.getInstance().read("btu and equip")
                    .isEmpty()
            ) {
                Text(
                    text = "BTU meter is not configured. DCWB will not work correctly",
                    fontSize = 20.sp,
                )
            } else {
                Text(
                    text = "BTU meter configured",
                    fontSize = 20.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),  // Makes the row take up the full width of the screen
        ) {
            // First Switch
            SwitchWithLabel(
                label = "Adaptive Delta T",
                isChecked = !selectedSwitch,
                onCheckedChange = {
                    selectedSwitch = !it
                    if(it) {
                        viewState.value.isAdaptiveDeltaEnabled = it
                        viewState.value.ismaximizedExitWaterTempEnable = !it
                    } else {
                        viewState.value.isAdaptiveDeltaEnabled = !it
                        viewState.value.ismaximizedExitWaterTempEnable = it
                    }
                    dabModulatingViewModel.setStateChanged()
                }
            )
            Spacer(modifier = Modifier.weight(1f))

            // Second Switch
            SwitchWithLabel(
                label = "Maximized\nExit Water",
                isChecked = selectedSwitch,
                onCheckedChange = {
                    selectedSwitch = it
                    if(it) {
                        viewState.value.isAdaptiveDeltaEnabled = !it
                        viewState.value.ismaximizedExitWaterTempEnable = it
                    } else {
                        viewState.value.isAdaptiveDeltaEnabled = it
                        viewState.value.ismaximizedExitWaterTempEnable = !it
                    }
                    dabModulatingViewModel.setStateChanged()
                }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        // Chilled water configuration
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            if (selectedSwitch) {
                DropDownWithLabel(
                    label = "Chilled water Exit\nTemperature Margin (°F)",
                    list = (0..15).map { it.toDouble().toString() }, isHeader = false,
                    defaultSelection = viewState.value.chilledWaterExitTemperatureMargin.toInt(),
                    onSelected = {
                        viewState.value.chilledWaterExitTemperatureMargin = it.toDouble()
                        dabModulatingViewModel.setStateChanged()
                    },
                    isEnabled = true,
                    spacerLimit = 102,
                    previewWidth = 100,
                    expandedWidth = 120
                )
            } else {
                DropDownWithLabel(
                    label = "Chilled water\ntarget Delta T (°F)",
                    list = (0..30).map { it.toDouble().toString() }, isHeader = false,
                    defaultSelection = viewState.value.chilledWaterTargetDelta.toInt(),
                    onSelected = {
                        viewState.value.chilledWaterTargetDelta = it.toDouble()
                        dabModulatingViewModel.setStateChanged()
                    },
                    isEnabled = true,
                    spacerLimit = 102,
                    previewWidth = 100,
                    expandedWidth = 120
                )
            }
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(label = "Chilled water Max\nFlow rate(GPM)",
                list = (0..200 step viewState.value.chilledWaterMaxFlowRateInc.toInt()).map { it.toDouble().toString() }, isHeader = false,
                defaultSelection = viewState.value.chilledWaterMaxFlowRate.toInt(),
                onSelected = {
                    viewState.value.chilledWaterMaxFlowRate = it.toDouble()
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = true,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120)
        }

        // Analog in valve configuration
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(label = "Analog-In 1 at\nValve Closed",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog1ValveClosedPosition,
                onSelected = {
                    viewState.value.analog1ValveClosedPosition = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog1OutputEnabled,
                spacerLimit = 102,
                previewWidth = 100,
                expandedWidth = 120)
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(label = "Analog-In 1 at\nValve Full Position",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog1ValveFullPosition,
                onSelected = {
                    viewState.value.analog1ValveFullPosition = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog1OutputEnabled,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120)
        }

        // Analog out-1 CHW valve configuration
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(label = "Analog-Out1 at\nMin CHW Valve",
                list = (0..10).map { it.toString() }, isHeader = false,
                // We are reusing the analogOut1CoolingMin which was used in non dcwb enabled case
                defaultSelection = viewState.value.analogOut1CoolingMin,
                onSelected = {
                    viewState.value.analogOut1CoolingMin = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog1OutputEnabled,
                spacerLimit = 102,
                previewWidth = 100,
                expandedWidth = 120)
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(label = "Analog-Out1 at\nMax CHW Valve",
                list = (0..10).map { it.toString() }, isHeader = false,
                // We are reusing the analogOut1CoolingMax which was used in non dcwb enabled case
                defaultSelection = viewState.value.analogOut1CoolingMax,
                onSelected = {
                    viewState.value.analogOut1CoolingMax = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog1OutputEnabled,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120)
        }

        // Analog-Out 2 fan speed configuration
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(
                label = "Analog-Out2 at\nMin Fan Speed",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog2MinFan,
                onSelected = {
                    viewState.value.analog2MinFan = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog2OutputEnabled,
                spacerLimit = 102,
                previewWidth = 100,
                expandedWidth = 120
            )
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(
                label = "Analog-Out2 at\nMax Fan Speed",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog2MaxFan,
                onSelected = {
                    viewState.value.analog2MaxFan = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog2OutputEnabled,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120
            )
        }

        // Analog-Out 3 heating configuration
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(
                label = "Analog-Out3 at\nMin Heating",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog3MinHeating,
                onSelected = {
                    viewState.value.analog3MinHeating = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog3OutputEnabled,
                spacerLimit = 102,
                previewWidth = 100,
                expandedWidth = 120
            )
            Spacer(modifier = Modifier.width(130.dp))
            DropDownWithLabel(
                label = "Analog-Out3 at\nMax Heating",
                list = (0..10).map { it.toString() }, isHeader = false,
                defaultSelection = viewState.value.analog3MaxHeating,
                onSelected = {
                    viewState.value.analog3MaxHeating = it
                    dabModulatingViewModel.setStateChanged()
                },
                isEnabled = viewState.value.isAnalog3OutputEnabled,
                spacerLimit = 147,
                previewWidth = 100,
                expandedWidth = 120
            )
        }

        // Analog-Out 4 cooling loop configuration
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 40.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            if(mappedToDamper) {
                DropDownWithLabel(
                    label = "Analog-Out4 at\nMin Outside Damper",
                    list = (0..10).map { it.toString() }, isHeader = false,
                    defaultSelection = viewState.value.analogOut4FreshAirMin,
                    onSelected = {
                        viewState.value.analogOut4FreshAirMin = it
                        dabModulatingViewModel.setStateChanged()
                    },
                    isEnabled = viewState.value.isAnalog4OutputEnabled,
                    spacerLimit = 102,
                    previewWidth = 100,
                    expandedWidth = 120
                )
            } else {
                DropDownWithLabel(
                    label = "Analog-Out4 at\nMin Cooling Loop",
                    list = (0..10).map { it.toString() }, isHeader = false,
                    defaultSelection = viewState.value.analogOut4MinCoolingLoop,
                    onSelected = {
                        viewState.value.analogOut4MinCoolingLoop = it
                        dabModulatingViewModel.setStateChanged()
                    },
                    isEnabled = viewState.value.isAnalog4OutputEnabled,
                    spacerLimit = 102,
                    previewWidth = 100,
                    expandedWidth = 120
                )
            }

            Spacer(modifier = Modifier.width(130.dp))
            if(mappedToDamper) {
                DropDownWithLabel(
                    label = "Analog-Out4 at\nMax Outside Damper",
                    list = (0..10).map { it.toString() }, isHeader = false,
                    defaultSelection = viewState.value.analogOut4FreshAirMax,
                    onSelected = {
                        viewState.value.analogOut4FreshAirMax = it
                        dabModulatingViewModel.setStateChanged()
                    },
                    isEnabled = viewState.value.isAnalog4OutputEnabled,
                    spacerLimit = 147,
                    previewWidth = 100,
                    expandedWidth = 120
                )
            } else {
                DropDownWithLabel(
                    label = "Analog-Out4 at\nMax Cooling Loop",
                    list = (0..10).map { it.toString() }, isHeader = false,
                    defaultSelection = viewState.value.analogOut4MaxCoolingLoop,
                    onSelected = {
                        viewState.value.analogOut4MaxCoolingLoop = it
                        dabModulatingViewModel.setStateChanged()
                    },
                    isEnabled = viewState.value.isAnalog4OutputEnabled,
                    spacerLimit = 147,
                    previewWidth = 100,
                    expandedWidth = 120
                )
            }
        }
    }

    /**
     * Composable to display the Analog Out whose toggle button toggles according to the DCWB toggle
     */
    @Composable
    fun SystemAnalogOutDynamicMappingView( analogName : String, analogOutState :Boolean = false, onAnalogOutEnabled: (Boolean) -> Unit,
                                           mappingText : String, analogOutValList : List<String>, analogOutVal : Int = 0 , dropDownWidthPreview : Int = 160,dropdownWidthExpanded : Int = 160,
                                           onAnalogOutChanged : (Int) -> Unit,mappingTextSpacer : Int = 155) {
        Row (modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 20.dp), horizontalArrangement = Arrangement.Start){
            Row {
                SimpleSwitch(isChecked = analogOutState , onCheckedChange = onAnalogOutEnabled)
                Spacer(modifier = Modifier.width(30.dp))
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = analogName, fontSize = 20.sp)
                }
            }
            Spacer(modifier=Modifier.width(41.dp))
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = mappingText, fontSize = 20.sp, modifier = Modifier
                    .padding(start = 55.dp))
            }
            Spacer(modifier = Modifier.width(mappingTextSpacer.dp))
            DropDownWithLabel(label = "", list = analogOutValList, previewWidth = dropDownWidthPreview, expandedWidth = dropdownWidthExpanded,
                onSelected = onAnalogOutChanged, defaultSelection = analogOutVal)
        }
    }
}
