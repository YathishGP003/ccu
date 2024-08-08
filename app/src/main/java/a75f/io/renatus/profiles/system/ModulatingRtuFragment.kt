package a75f.io.renatus.profiles.system

import a75f.io.domain.api.DomainName
import a75f.io.renatus.composables.DropDownWithLabel
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

open class ModulatingRtuFragment : Fragment() {
    private val modulatingViewModel: VavModulatingRtuViewModel by viewModels()
    @Composable
    fun AnalogOutAndRelayComposable(viewModel: VavModulatingRtuViewModel) {

        Spacer(modifier = Modifier.height(12.dp))
        SystemAnalogOutMappingView(
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
            mappingText = "Cooling",
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal =(0..100).map{it}.indexOf(viewModel.viewState.value.analogOut1CoolingTestSignal.toInt()),
            onAnalogOutChanged = {
                viewModel.viewState.value.analogOut1CoolingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog1Out, it.toDouble())
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            mappingTextSpacer = 193
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
            analogOutVal =(0..100).map{it}.indexOf(viewModel.viewState.value.analogOut2FanSpeedTestSignal.toInt()),
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
            analogOutVal = (0..100).map{it}.indexOf(viewModel.viewState.value.analogOut3HeatingTestSignal.toInt()),
            onAnalogOutChanged = {
                viewModel.viewState.value.analogOut3HeatingTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog3Out, it.toDouble())
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            mappingTextSpacer = 190
        )
        Spacer(modifier = Modifier.height(16.dp))

        SystemAnalogOutMappingView(
            analogName = "Analog-Out 4",
            analogOutState = viewModel.viewState.value.isAnalog4OutputEnabled,
            onAnalogOutEnabled = {
                viewModel.viewState.value.isAnalog4OutputEnabled = it
                viewModel.setStateChanged()
                viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                    "Analog 4 Output",
                    it,
                    viewModel.profileConfiguration
                )
            },
            mappingText = "OutSide Air",
            analogOutValList = (0..100).map { it.toDouble().toString() },
            analogOutVal = (0..100).map{it}.indexOf(viewModel.viewState.value.analogOut4OutSideAirTestSignal.toInt()),
            onAnalogOutChanged = {
                viewModel.viewState.value.analogOut4OutSideAirTestSignal = it.toDouble()
                viewModel.sendAnalogRelayTestSignal(DomainName.analog4Out, it.toDouble())
            },
            dropDownWidthPreview = 100,
            dropdownWidthExpanded = 120,
            mappingTextSpacer = 153
        )
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
            buttonState = viewModel.getRelayState(DomainName.relay3),
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
        buttonState: Boolean = false, onTestActivated: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, end = 0.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Row {
                ToggleButtonStateful(defaultSelection = relayState, onEnabled = onRelayEnabled)
                Spacer(modifier = Modifier.width(30.dp))
                Column {
                    Spacer(modifier=Modifier.height(6.dp))
                    Text(text = relayText, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(75.dp))
            DropDownWithLabel(
                label = "",
                list = mapping,
                previewWidth = 235,
                expandedWidth = 253,
                heightValue = 100,
                onSelected = onMappingChanged,
                defaultSelection = mappingSelection,
                isEnabled = modulatingViewModel.viewState.value.isRelay7OutputEnabled
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
}