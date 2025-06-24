package a75f.io.renatus.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RadioButtonComposeBacnet(
    radioTexts: List<Any?>,
    radioOptions: List<Int>,
    default: Int,
    onSelect: (Int) -> Unit
) {
    var selectedItem by remember { mutableStateOf(radioOptions[default]) }

    if (selectedItem != radioOptions[default]) {
        selectedItem = radioOptions[default]
    }
    Row(
        modifier = Modifier
            .selectableGroup()
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        radioOptions.forEach { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .weight(1f)
                    //.wrapContentSize()
                    .height(56.dp)
                    .selectable(
                        selected = (selectedItem == label), onClick = {
                            selectedItem = label
                            onSelect(selectedItem)
                        }, role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
            ) {
                RadioButton(
                    modifier = Modifier
                        .padding(end = 10.dp),
                    selected = (selectedItem == label),
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ComposeUtil.primaryColor,
                        unselectedColor = Color.Gray
                    )
                )
                val tValue = radioTexts[label] ?: "-"
                Text(
                    text = tValue.toString(),
                    style = TextStyle(
                        fontFamily = ComposeUtil.myFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Composable
fun RadioButtonComposeBacnetTerminal(
    modifier: Modifier,
    groupedRowIndex: Int,
    radioTexts: List<String?>,
    radioOptions: List<Int>,
    default: Int,
    selectedItem: SnapshotMutableState<Int>,
    onSelect: (Int) -> Unit
) {
    if (selectedItem.value != radioOptions[default]) {
        selectedItem.value = radioOptions[default]
    }
    val label = radioOptions[groupedRowIndex]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .selectable(
                selected = (selectedItem.value == label), onClick = {
                    selectedItem.value = label
                    onSelect(selectedItem.value)
                }, role = Role.RadioButton
            )
    ) {
        RadioButton(
            modifier = Modifier
                .padding(end = 10.dp),
            selected = (selectedItem.value == label),
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = ComposeUtil.primaryColor,
                unselectedColor = Color.Gray
            )
        )
        val tValue = radioTexts[label] ?: "-"
        Text(
            text = tValue,
            style = TextStyle(
                fontFamily = ComposeUtil.myFontFamily,
                fontSize = 22.sp,
                fontWeight = FontWeight.Normal
            )
        )
    }
}