package a75f.io.renatus.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 06-09-2023.
 */

@Composable
fun RadioButtonComposeSelectModel(radioOptions: List<String>, default: Int, onSelect: (String) -> Unit) {
    var selectedItem by remember { mutableStateOf(radioOptions[default]) }

    if (selectedItem != radioOptions[default]) {
         selectedItem = radioOptions[default]
    }
    Row(modifier = Modifier
        .selectableGroup()
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        radioOptions.forEach { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentSize()
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
                    modifier = Modifier.padding(end = 10.dp),
                    selected = (selectedItem == label),
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ComposeUtil.primaryColor,
                        unselectedColor = Color.Gray
                    )
                )
                Text(text = label, style =  TextStyle( fontFamily = ComposeUtil.myFontFamily,fontSize = 20.sp,  fontWeight = FontWeight.Normal))
            }
        }
    }
}

@Composable
fun RadioButtonComposeSelectModelCustom(radioOptions: List<String>, default: Int, onSelect: (String) -> Unit) {
    var selectedItem by remember { mutableStateOf(radioOptions[default]) }

    if (selectedItem != radioOptions[default]) {
        selectedItem = radioOptions[default]
    }
    Row(modifier = Modifier
        .selectableGroup()
        .wrapContentWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(36.dp),
    ) {
        radioOptions.forEach { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentSize()
                    .height(56.dp)
                    .selectable(
                        selected = (selectedItem == label), onClick = {
                            selectedItem = label
                            onSelect(selectedItem)
                        }, role = Role.RadioButton
                    )
            ) {
                RadioButton(
                    modifier = Modifier.padding(end = 14.dp),
                    selected = (selectedItem == label),
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ComposeUtil.primaryColor,
                        unselectedColor = Color.Gray
                    )
                )
                Text(text = label, style =  TextStyle( fontFamily = ComposeUtil.myFontFamily,fontSize = 20.sp,  fontWeight = FontWeight.Normal))
            }
        }
    }
}