package a75f.io.renatus.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
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
fun RadioButtonCompose(radioOptions: List<String>, default: Int,
                       disabledOptions: List<String> = emptyList(),
                       onSelect: (String) -> Unit) {
    var selectedItem by remember { mutableStateOf(radioOptions[default]) }

    if (selectedItem != radioOptions[default]) {
         selectedItem = radioOptions[default]
    }
    Row(modifier = Modifier
        .selectableGroup()
        .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        radioOptions.forEachIndexed  { index, label ->
            val isDisabled = disabledOptions.contains(label)
            val isSelected = selectedItem == label


            val backgroundModifier = if (isDisabled) {
                Modifier.background(Color.Gray)
            } else Modifier

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(backgroundModifier)
                    .wrapContentSize()
                    .height(56.dp)
                    .selectable(
                        selected = isSelected,
                        enabled = !isDisabled,
                        onClick = {
                            selectedItem = label
                            onSelect(selectedItem)
                        }, role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
            ) {
                RadioButton(
                    modifier = Modifier.padding(end = 10.dp),
                    selected = isSelected,
                    onClick = null,
                    enabled = !isDisabled,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = ComposeUtil.primaryColor,
                        unselectedColor = Color.Gray,
                        disabledSelectedColor = Color.Gray,
                        disabledUnselectedColor = Color.Gray
                    )
                )
                Text(text = label, style =  TextStyle( fontFamily = ComposeUtil.myFontFamily,fontSize = 20.sp,  fontWeight = FontWeight.Normal))
            }
        }
    }
}