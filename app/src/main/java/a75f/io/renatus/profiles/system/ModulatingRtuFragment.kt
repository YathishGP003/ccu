package a75f.io.renatus.profiles.system

import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.renatus.composables.AddInputWidget
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.SystemAnalogOutMappingDropDown
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

open class ModulatingRtuFragment : Fragment() {
    private val viewModel: VavModulatingRtuViewModel by viewModels()

    @Composable
    fun AnalogOutAndRelayComposable(viewModel: VavModulatingRtuViewModel) {

        Spacer(modifier = Modifier.height(12.dp))
        SystemAnalogOutMappingDropDown(
            analogName = "Analog-Out 1",
            analogOutState = viewModel.viewState.value.isAnalog1OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog1OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState =
                    UnusedPortsModel.setPortState(
                        "Analog 1 Output",
                        it,
                        viewModel.profileConfiguration
                    )
            },
            mapping = viewModel.analog1AssociationList,
            onMappingChanged = {
                viewModel.viewState.value.analog1OutputAssociation = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewModel.viewState.value.analog1OutputAssociation,
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =
            try {
                (0..100).map { it }.indexOf(Domain.cmBoardDevice.analog1Out.readHisVal().toInt())
            } catch (e: UninitializedPropertyAccessException) {
                // When the cmBoardDevice is uninitialized after registration
                (0..100).map { it }.indexOf(20)
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            onAnalogOutChanged = {
                //viewModel.viewState.value.analogOut1CoolingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog1Out, it.toDouble())
            },
        )

        Spacer(modifier = Modifier.height(14.dp))

        SystemAnalogOutMappingDropDown(
            analogName = "Analog-Out 2",
            analogOutState = viewModel.viewState.value.isAnalog2OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog2OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState =
                    UnusedPortsModel.setPortState(
                        "Analog 2 Output",
                        it,
                        viewModel.profileConfiguration
                    )
            },
            mapping = viewModel.analog1AssociationList,
            onMappingChanged = {
                viewModel.viewState.value.analog2OutputAssociation = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewModel.viewState.value.analog2OutputAssociation,
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =
            try {
                (0..100).map { it }.indexOf(Domain.cmBoardDevice.analog2Out.readHisVal().toInt())
            } catch (e: UninitializedPropertyAccessException) {
                // When the cmBoardDevice is uninitialized after registration
                (0..100).map { it }.indexOf(20)
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            onAnalogOutChanged = {
                //viewModel.viewState.value.analogOut2CoolingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog2Out, it.toDouble())
            },
        )

        Spacer(modifier = Modifier.height(14.dp))

        SystemAnalogOutMappingDropDown(
            analogName = "Analog-Out 3",
            analogOutState = viewModel.viewState.value.isAnalog3OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog3OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState =
                    UnusedPortsModel.setPortState(
                        "Analog 3 Output",
                        it,
                        viewModel.profileConfiguration
                    )
            },
            mapping = viewModel.analog1AssociationList,
            onMappingChanged = {
                viewModel.viewState.value.analog3OutputAssociation = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewModel.viewState.value.analog3OutputAssociation,
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =
            try {
                (0..100).map { it }.indexOf(Domain.cmBoardDevice.analog3Out.readHisVal().toInt())
            } catch (e: UninitializedPropertyAccessException) {
                // When the cmBoardDevice is uninitialized after registration
                (0..100).map { it }.indexOf(20)
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            onAnalogOutChanged = {
                //viewModel.viewState.value.analogOut2CoolingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog3Out, it.toDouble())
            },
        )


        Spacer(modifier = Modifier.height(14.dp))
        SystemAnalogOutMappingDropDown(
            analogName = "Analog-Out 4",
            analogOutState = viewModel.viewState.value.isAnalog4OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog4OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState =
                    UnusedPortsModel.setPortState(
                        "Analog 3 Output",
                        it,
                        viewModel.profileConfiguration
                    )
            },
            mapping = viewModel.analog1AssociationList,
            onMappingChanged = {
                viewModel.viewState.value.analog4OutputAssociation = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewModel.viewState.value.analog4OutputAssociation,
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =
            try {
                (0..100).map { it }.indexOf(Domain.cmBoardDevice.analog4Out.readHisVal().toInt())
            } catch (e: UninitializedPropertyAccessException) {
                // When the cmBoardDevice is uninitialized after registration
                (0..100).map { it }.indexOf(20)
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            onAnalogOutChanged = {
                //viewModel.viewState.value.analogOut2CoolingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog4Out, it.toDouble())
            },
        )

        Spacer(modifier = Modifier.height(14.dp))

        SystemRelayMappingView(
            relayText = "Relay 3",
            relayState = viewModel.viewState.value.isRelay3OutputEnabled,
            onRelayEnabled = {
                viewModel.viewState.value.isRelay3OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                    "Relay 3",
                    it,
                    viewModel.profileConfiguration
                )
            },
            mappingSelection = viewModel.viewState.value.relay3Association,
            mapping = viewModel.relay3AssociationList,
            onMappingChanged = {
                viewModel.viewState.value.relay3Association = it
                viewModel.setStateChanged()
            },
            buttonState = viewModel.getRelayState(DomainName.relay3),
            onTestActivated = {
                viewModel.sendTestCommand(DomainName.relay3, it)
            },
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
            mapping = viewModel.relay3AssociationList,
            onMappingChanged = {
                viewModel.viewState.value.relay7Association = it
                viewModel.setStateChanged()
            },
            buttonState = viewModel.getRelayState(DomainName.relay7),
            onTestActivated = {
                viewModel.sendTestCommand(DomainName.relay7, it)
            },
        )
    }

    @Composable
    fun SystemRelayMappingView(
        relayText: String, relayState: Boolean = false, onRelayEnabled: (Boolean) -> Unit,
        mapping: List<String>, mappingSelection: Int = 0, onMappingChanged: (Int) -> Unit,
        buttonState: Boolean = false, onTestActivated: (Boolean) -> Unit,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 0.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Row {
                ToggleButtonStateful(defaultSelection = relayState, onEnabled = onRelayEnabled)
                Spacer(modifier = Modifier.width(15.dp))
                Column {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = relayText, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(40.dp))
            DropDownWithLabel(
                label = "",
                list = mapping,
                previewWidth = 280,
                expandedWidth = 280,
                heightValue = 100,
                onSelected = onMappingChanged,
                defaultSelection = mappingSelection,
                isEnabled = relayState
            )

            var buttonState by remember { mutableStateOf(buttonState) }
            var text by remember { mutableStateOf("OFF") }
            Spacer(modifier = Modifier.width(102.dp))
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
                Text(
                    text = when (buttonState) {
                        true -> "ON"
                        false -> "OFF"
                    }, fontSize = 20.sp,
                    fontFamily = ComposeUtil.myFontFamily,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Left,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
        }
    }


    fun setStateChanged(viewModel: VavModulatingRtuViewModel) {
        viewModel.viewState.value.isStateChanged = true
        viewModel.viewState.value.isSaveRequired = true
    }

    @Composable
    fun AnalogInputConfig() {
        val viewState = viewModel.viewState.value
        //Spacer(modifier = Modifier.height(12.dp))
        AddInputWidget(
            inputName = "Thermistor 1",
            inputState = viewState.thermistor1Enabled,
            onEnabled = {
                viewState.thermistor1Enabled = it
                viewModel.setStateChanged()
            },
            mapping = viewModel.thermistor1AssociationList,
            onMappingChanged = {
                viewState.thermistor1Association = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewState.thermistor1Association,
            horizontalSpacer = 55,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddInputWidget(
            inputName = "Thermistor 2",
            inputState = viewState.thermistor2Enabled,
            onEnabled = {
                viewState.thermistor2Enabled = it
                viewModel.setStateChanged()
            },
            mapping = viewModel.thermistor1AssociationList,
            onMappingChanged = {
                viewState.thermistor2Association = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewState.thermistor2Association,
            horizontalSpacer = 55,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddInputWidget(
            inputName = "Analog-In 1",
            inputState = viewState.analogIn1Enabled,
            onEnabled = {
                viewState.analogIn1Enabled = it
                viewModel.setStateChanged()
            },
            mapping = viewModel.analogIn1AssociationList,
            onMappingChanged = {
                viewState.analogIn1Association = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewState.analogIn1Association,
            horizontalSpacer = 55,
        )
        Spacer(modifier = Modifier.height(16.dp))
        AddInputWidget(
            inputName = "Analog-In 2",
            inputState = viewState.analogIn2Enabled,
            onEnabled = {
                viewState.analogIn2Enabled = it
                viewModel.setStateChanged()
            },
            mapping = viewModel.analogIn1AssociationList,
            onMappingChanged = {
                viewState.analogIn2Association = it
                viewModel.setStateChanged()
            },
            mappingSelection = viewState.analogIn2Association,
            horizontalSpacer = 55,
        )
        //Spacer(modifier = Modifier.height(12.dp))
    }

    @Composable
    fun AnalogOut1MinMaxConfig() {
        if (!viewModel.viewState.value.isAnalog1OutputEnabled) {
            return
        }
        when (viewModel.viewState.value.analog1OutputAssociation) {
            0 -> {
                MinMaxConfiguration("Analog-Out1 at Min \nFan Speed",
                    "Analog-Out1 at Max \nFan Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog1OutMinMaxConfig.fanSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog1OutMinMaxConfig.fanSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.fanSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.fanSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            1 -> {
                MinMaxConfiguration("Analog-Out1 at Min \nCompressor Speed",
                    "Analog-Out1 at Max \nCompressor Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog1OutMinMaxConfig.compressorSpeedConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog1OutMinMaxConfig.compressorSpeedConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.compressorSpeedConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.compressorSpeedConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            2 -> {
                MinMaxConfiguration("Analog-Out1 at Min \nOutside Air Damper",
                    "Analog-Out1 at Max \nOutside Air Damper",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog1OutMinMaxConfig.outsideAirDamperConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog1OutMinMaxConfig.outsideAirDamperConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.outsideAirDamperConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.outsideAirDamperConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            3 -> {
                MinMaxConfiguration("Analog-Out1 at Min \nCooling Signal",
                    "Analog-Out1 at Max \nCooling Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog1OutMinMaxConfig.coolingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog1OutMinMaxConfig.coolingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.coolingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.coolingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            4 -> {
                MinMaxConfiguration("Analog-Out1 at Min \nHeating Signal",
                    "Analog-Out1 at Max \nHeating Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog1OutMinMaxConfig.heatingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog1OutMinMaxConfig.heatingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.heatingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog1OutMinMaxConfig.heatingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
        }
    }

    @Composable
    fun AnalogOut2MinMaxConfig() {
        if (!viewModel.viewState.value.isAnalog2OutputEnabled) {
            return
        }
        when (viewModel.viewState.value.analog2OutputAssociation) {
            0 -> {
                MinMaxConfiguration("Analog-Out2 at Min \nFan Speed",
                    "Analog-Out2 at Max \nFan Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog2OutMinMaxConfig.fanSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog2OutMinMaxConfig.fanSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.fanSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.fanSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            1 -> {
                MinMaxConfiguration("Analog-Out2 at Min \nCompressor Speed",
                    "Analog-Out2 at Max \nCompressor Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog2OutMinMaxConfig.compressorSpeedConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog2OutMinMaxConfig.compressorSpeedConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.compressorSpeedConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.compressorSpeedConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            2 -> {
                MinMaxConfiguration("Analog-Out2 at Min \nOutside Air Damper",
                    "Analog-Out2 at Max \nOutside Air Damper",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog2OutMinMaxConfig.outsideAirDamperConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog2OutMinMaxConfig.outsideAirDamperConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.outsideAirDamperConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.outsideAirDamperConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            3 -> {
                MinMaxConfiguration("Analog-Out2 at Min \nCooling Signal",
                    "Analog-Out2 at Max \nCooling Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog2OutMinMaxConfig.coolingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog2OutMinMaxConfig.coolingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.coolingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.coolingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            4 -> {
                MinMaxConfiguration("Analog-Out2 at Min \nHeating Signal",
                    "Analog-Out2 at Max \nHeating Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog2OutMinMaxConfig.heatingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog2OutMinMaxConfig.heatingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.heatingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog2OutMinMaxConfig.heatingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
        }
    }

    @Composable
    fun AnalogOut3MinMaxConfig() {
        if (!viewModel.viewState.value.isAnalog3OutputEnabled) {
            return
        }
        when (viewModel.viewState.value.analog3OutputAssociation) {
            0 -> {
                MinMaxConfiguration("Analog-Out3 at Min \nFan Speed",
                    "Analog-Out3 at Max \nFan Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog3OutMinMaxConfig.fanSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog3OutMinMaxConfig.fanSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.fanSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.fanSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            1 -> {
                MinMaxConfiguration("Analog-Out3 at Min \nCompressor Speed",
                    "Analog-Out3 at Max \nCompressor Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog3OutMinMaxConfig.compressorSpeedConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog3OutMinMaxConfig.compressorSpeedConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.compressorSpeedConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.compressorSpeedConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            2 -> {
                MinMaxConfiguration("Analog-out3 at Min \nOutside Air Damper",
                    "Analog-Out3 at Max \nOutside Air Damper",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog3OutMinMaxConfig.outsideAirDamperConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog3OutMinMaxConfig.outsideAirDamperConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.outsideAirDamperConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.outsideAirDamperConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            3 -> {
                MinMaxConfiguration("Analog-Out3 at Min \nCooling Signal",
                    "Analog-Out3 at Max \nCooling Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog3OutMinMaxConfig.coolingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog3OutMinMaxConfig.coolingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.coolingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.coolingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            4 -> {
                MinMaxConfiguration("Analog-Out3 at Min \nHeating Signal",
                    "Analog-Out3 at Max \nHeating Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog3OutMinMaxConfig.heatingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog3OutMinMaxConfig.heatingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.heatingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog3OutMinMaxConfig.heatingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
        }
    }

    @Composable
    fun AnalogOut4MinMaxConfig() {
        if (!viewModel.viewState.value.isAnalog4OutputEnabled) {
            return
        }
        when (viewModel.viewState.value.analog4OutputAssociation) {
            0 -> {
                MinMaxConfiguration("Analog-Out4 at Min \nFan Speed",
                    "Analog-Out4 at Max \nFan Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog4OutMinMaxConfig.fanSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog4OutMinMaxConfig.fanSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.fanSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.fanSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }

            1 -> {
                MinMaxConfiguration("Analog-Out4 at Min \nCompressor Speed",
                    "Analog-Out4 at Max \nCompressor Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog4OutMinMaxConfig.compressorSpeedConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog4OutMinMaxConfig.compressorSpeedConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.compressorSpeedConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.compressorSpeedConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }

            2 -> {
                MinMaxConfiguration("Analog-Out4 at Min \nOutside Air Damper",
                    "Analog-Out4 at Max \nOutside Air Damper",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog4OutMinMaxConfig.outsideAirDamperConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog4OutMinMaxConfig.outsideAirDamperConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.outsideAirDamperConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.outsideAirDamperConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }

            3 -> {
                MinMaxConfiguration("Analog-Out4 at Min \nCooling Signal",
                    "Analog-Out4 at Max \nCooling Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog4OutMinMaxConfig.coolingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog4OutMinMaxConfig.coolingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.coolingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.coolingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }

            4 -> {
                MinMaxConfiguration("Analog-Out4 at Min \nHeating Signal",
                    "Analog-Out4 at Max \nHeating Signal",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analog4OutMinMaxConfig.heatingSignalConfig.min.toString(),
                    maxDefault = viewModel.viewState.value.analog4OutMinMaxConfig.heatingSignalConfig.max.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.heatingSignalConfig.min =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analog4OutMinMaxConfig.heatingSignalConfig.max =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }

        }
    }
}