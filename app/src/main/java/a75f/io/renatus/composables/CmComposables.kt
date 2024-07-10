package a75f.io.renatus.composables

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SearchSpinnerElement
import a75f.io.renatus.compose.SpinnerElementOption
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.compose.formatText
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.profiles.system.advancedahu.Option
import a75f.io.renatus.profiles.system.advancedahu.vav.VavAdvancedHybridAhuViewModel
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 27-03-2024.
 */

@Composable
fun EnableCompose(
    sensorAddress: String, default: Boolean, hide: Boolean = false, onEnabled: (Boolean) -> Unit = {}
) {
    Row(modifier = Modifier.width(375.dp)) {
        Box(modifier = Modifier.weight(3f).align(Alignment.CenterVertically).padding(start = 15.dp)) {
            StyledTextView(
                sensorAddress, fontSize = 20
            )
        }
        Box(modifier = Modifier.weight(4f).align(Alignment.CenterVertically)) {
            if (!hide) {
                Image(
                    painter = painterResource(id = R.drawable.map),
                    contentDescription = "Mapping",
                    modifier = Modifier
                        .width(150.dp)
                        .padding(start = 10.dp)
                        .align(Alignment.Center)
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            ToggleButtonStateful(defaultSelection = default) { onEnabled(it) }
        }
    }
}

@Composable
fun ConfigCompose(
    name: String,
    defaultSelection: Option,
    items: List<Option>,
    unit: String,
    isEnabled: Boolean,
    onSelect: (Option) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(10.dp)
    ) {
        Box(modifier = Modifier.weight(1.5f).padding(start = 3.dp)) {
            StyledTextView(name, fontSize = 20)
        }
        Box(modifier = Modifier
            .weight(4f)
            .padding(end = 20.dp)) {
            SearchSpinnerElement(
                default = defaultSelection,
                allItems = items,
                unit = unit,
                onSelect = { onSelect(it) },
                400,
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
            .padding(10.dp)
    ) {
        Box(modifier = Modifier.weight(1.3f)) {
            StyledTextView(name, fontSize = 20)
        }
        Box(modifier = Modifier
            .weight(4f)
            .padding(end = 20.dp, start = 5.dp)) {
            StyledTextView(formatText(defaultSelection), fontSize = 20)
        }
        Box(modifier = Modifier.weight(0.5f))
    }
}

@Composable
fun AOTHConfig(
    name: String,
    default: Boolean,
    onEnabled: (Boolean) -> Unit = {},
    spinnerDefault: Option,
    items: List<Option>,
    unit: String = "",
    isEnabled: Boolean,
    onSelect: (Option) -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(10.dp)
    ) {
        Box(modifier = Modifier.wrapContentWidth()) {
            ToggleButtonStateful(defaultSelection = default) { onEnabled(it) }
        }
        Box(modifier = Modifier.weight(1.5f).padding(start = 10.dp, top = 10.dp)) {
            StyledTextView(name, fontSize = 20)
        }
        Box(modifier = Modifier.weight(4f).padding(top = 5.dp)) {
            SearchSpinnerElement(
                default = spinnerDefault,
                allItems = items,
                unit = unit,
                onSelect = { onSelect(it) },
                width = 400,
                isEnabled = isEnabled
            )
        }
        Box(modifier = Modifier.weight(1f))
    }
}


@Composable
fun RelayConfiguration(
    relayName: String,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    association: Option,
    relayEnums: List<Option>,
    unit: String,
    isEnabled: Boolean,
    onAssociationChanged: (Option) -> Unit,
    testState: Boolean = false,
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
        Box(modifier = Modifier.weight(1.5f).padding(start = 10.dp, top = 10.dp)) {
            StyledTextView(relayName, fontSize = 20)
        }
        Box(modifier = Modifier.weight(4f).padding(top = 5.dp)) {
            SearchSpinnerElement(
                default = association,
                allItems = relayEnums,
                unit = unit,
                onSelect = { onAssociationChanged(it) },
                width = 400,
                isEnabled = isEnabled
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 5.dp)
        ) {
            var buttonState by remember { mutableStateOf(testState) }
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
                        true -> ComposeUtil.primaryColor
                        false -> ComposeUtil.greyColor
                    }, containerColor = Color.Transparent
                ),
                border = BorderStroke(1.dp, color = when (buttonState) {
                    true -> ComposeUtil.primaryColor
                    false -> ComposeUtil.greyColor
                }),
                    modifier = Modifier
                            .width(72.dp)
                            .height(44.dp),
                    contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = when (buttonState) {
                        true -> "ON"
                        false -> "OFF"
                    },
                        fontSize = 20.sp,
                        fontFamily = ComposeUtil.myFontFamily,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Left,
                        modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
        }
    }
}


@Composable
fun AnalogOutConfig(
    analogOutName: String,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit = {},
    association: Option,
    analogOutEnums: List<Option>,
    testSingles: List<Option>,
    unit: String = "",
    isEnabled: Boolean,
    onAssociationChanged: (Option) -> Unit = {},
    testVal: Double,
    onTestSignalSelected: (Double) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(10.dp)
    ) {
        Box(modifier = Modifier
            .wrapContentWidth()) {
            ToggleButtonStateful(defaultSelection = enabled) { onEnabledChanged(it) }
        }
        Box(modifier = Modifier
            .weight(1.5f)
            .padding(start = 10.dp, top = 10.dp)) {
            StyledTextView(analogOutName, fontSize = 20)
        }
        Box(modifier = Modifier
            .weight(4f)
            .padding(end = 5.dp,top = 5.dp)) {
            SearchSpinnerElement(
                default = association,
                allItems = analogOutEnums,
                unit = unit,
                onSelect = { onAssociationChanged(it) },
                width = 400,
                isEnabled = isEnabled
            )
        }
        Box(
            modifier = Modifier
                .weight(1f).padding(start = 5.dp)
        ) {
            SpinnerElementOption(
                defaultSelection = testVal.toString(),
                items = testSingles,
                unit = unit,
                itemSelected = {onTestSignalSelected(it.value.toDouble()) },
                previewWidth = 60
            )
        }
    }
}

@Composable
fun SaveConfig(viewModel: VavAdvancedHybridAhuViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(top = 20.dp, bottom = 40.dp)),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(PaddingValues(bottom = 10.dp, end = 5.dp)),
            contentAlignment = Alignment.Center,
        ) { SaveTextView(CANCEL, isChanged = viewModel.viewState.value.isStateChanged) {
            viewModel.viewState.value.isStateChanged = false
            viewModel.reset()
        } }
        Divider(
            modifier = Modifier
                .height(25.dp)
                .width(2.dp)
                .padding(bottom = 6.dp),
            color = Color.LightGray
        )
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
            contentAlignment = Alignment.Center
        ) {
            SaveTextView(SAVE, isChanged = viewModel.viewState.value.isSaveRequired) {
                CcuLog.i(
                    L.TAG_CCU_SYSTEM, viewModel.viewState.toString()
                )
                viewModel.saveConfiguration()
                viewModel.viewState.value.isSaveRequired = false
                viewModel.viewState.value.isStateChanged = false
            }
        }
    }
}


@Composable
fun MinMaxConfiguration(
    minLabel: String,
    maxLabel: String,
    itemList: List<Option>,
    unit: String,
    minDefault: String,
    maxDefault: String,
    onMinSelected: (Option) -> Unit = {},
    onMaxSelected: (Option) -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Box(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
            StyledTextView(
                minLabel, fontSize = 20, textAlignment = TextAlign.Left
            )
        }
        Box(modifier = Modifier.weight(1f).padding(top = 5.dp)) {
            SpinnerElementOption(defaultSelection = minDefault,
                items = itemList,
                unit = unit,
                itemSelected = { onMinSelected(it) })
        }

        Box(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
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