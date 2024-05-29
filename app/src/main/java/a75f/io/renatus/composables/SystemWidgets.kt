package a75f.io.renatus.composables

import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ToggleButtonStateful
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
import androidx.compose.material3.ButtonDefaults.buttonColors
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

@Composable
fun SystemRelayMappingView( relayText : String, relayState :Boolean = false, onRelayEnabled: (Boolean) -> Unit,
                            mapping : List<String>, mappingSelection : Int = 0 ,onMappingChanged: (Int) -> Unit,
                            buttonState : Boolean  = false, onTestActivated : (Boolean) -> Unit) {
    Row (modifier = Modifier
        .fillMaxWidth()
        .padding(start = 20.dp, end = 95.dp), horizontalArrangement = Arrangement.SpaceBetween,verticalAlignment = Alignment.CenterVertically){
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToggleButtonStateful(defaultSelection = relayState, onEnabled = onRelayEnabled )
            Spacer(modifier = Modifier.width(30.dp))
            Text(text = relayText, fontSize = 20.sp)
        }
        DropDownWithLabel(label = "", list = mapping, previewWidth = 260, expandedWidth = 260, spacerLimit = 0,
            onSelected = onMappingChanged, defaultSelection = mappingSelection, isEnabled = relayState)

        var buttonState by remember { mutableStateOf(buttonState) }
        var text by remember { mutableStateOf("OFF") }
        Button(onClick = {
            buttonState = !buttonState
            text = when(buttonState) {
                true -> "ON"
                false -> "OFF"
                }
            onTestActivated(buttonState)
        },
            colors = buttonColors(
                contentColor = when(buttonState) {
                    true -> ComposeUtil.primaryColor
                    false -> ComposeUtil.greyColor
                },
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.dp, when(buttonState) {
                true -> ComposeUtil.primaryColor
                false -> ComposeUtil.greyColor
            }),
            modifier = Modifier
                .width(72.dp)
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

@Composable
fun SystemAnalogOutMappingView( analogName : String, analogOutState :Boolean = false, onAnalogOutEnabled: (Boolean) -> Unit,
                                mappingText : String, analogOutValList : List<String>, analogOutVal : Int = 0 , dropDownWidthPreview : Int = 160,dropdownWidthExpanded : Int = 160,
                            onAnalogOutChanged : (Int) -> Unit,mappingTextSpacer : Int = 155) {
    Row (modifier = Modifier
        .fillMaxWidth()
        .padding(start = 25.dp, end = 20.dp), horizontalArrangement = Arrangement.Start){
        Row {
            ToggleButtonStateful(defaultSelection = analogOutState, onEnabled = onAnalogOutEnabled )
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
@Composable
fun SystemAnalogOutMappingViewVavStagedVfdRtu( analogName : String, analogOutState :Boolean = false, onAnalogOutEnabled: (Boolean) -> Unit,
                                mappingText : String, analogOutValList : List<String>, analogOutVal : Int = 0 ,
                                onAnalogOutChanged : (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 25.dp), horizontalArrangement = Arrangement.Start
    ) {
        Column {
            ToggleButtonStateful(defaultSelection = analogOutState, onEnabled = onAnalogOutEnabled)
        }
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Spacer(modifier = Modifier.width(30.dp))
                Text(text = analogName, fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.width(46.dp))
        Column {
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(text = mappingText, fontSize = 20.sp)
                DropDownWithLabel(
                    label = "",
                    list = analogOutValList,
                    previewWidth = 100,
                    expandedWidth = 100,
                    onSelected = onAnalogOutChanged,
                    defaultSelection = analogOutVal,
                    spacerLimit = 282
                )
            }
        }
    }
}


@Composable
fun SystemAnalogOutMappingViewButtonComposable(
    analogName: String,
    analogOutState: Boolean = false,
    onAnalogOutEnabled: (Boolean) -> Unit,
    mappingText: String,
    paddingLimitEnd: Int = 10,
    buttonState: Boolean = false,
    onTestActivated: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 20.dp), horizontalArrangement = Arrangement.Start
    ) {
        Row(modifier = Modifier
            .padding(end = paddingLimitEnd.dp)) {
            ToggleButtonStateful(defaultSelection = analogOutState, onEnabled = onAnalogOutEnabled)
            Spacer(modifier = Modifier.width(30.dp))
            Column {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = analogName, fontSize = 20.sp)
            }
        }
        Spacer(modifier = Modifier.width(135.dp))
        Column {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = mappingText, fontSize = 20.sp, modifier = Modifier
                    .padding(start = 21.dp)
            )
        }
        var buttonState by remember { mutableStateOf(buttonState) }
        var text by remember { mutableStateOf("OFF") }
        Spacer(modifier = Modifier.width(241.dp))
        Button(onClick = {
            buttonState = !buttonState
            text = when(buttonState) {
                true -> "ON"
                false -> "OFF"
            }
            onTestActivated(buttonState)
        },
            colors = buttonColors(
                contentColor = when(buttonState) {
                    true -> ComposeUtil.primaryColor
                    false -> ComposeUtil.greyColor
                },
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(5.dp),
            border = BorderStroke(1.dp, when(buttonState) {
                true -> ComposeUtil.primaryColor
                false -> ComposeUtil.greyColor
            }),
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

