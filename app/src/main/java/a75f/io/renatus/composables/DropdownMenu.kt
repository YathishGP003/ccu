package a75f.io.renatus.composables

import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DropDownWithLabel(label : String, list : List<String>, previewWidth : Int = 80, expandedWidth : Int = 100,
                      onSelected: (Int) -> Unit, defaultSelection : Int = 0) {
    Row {
        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
            HeaderTextView(text = label, padding = 0)
        }
        Spacer(modifier = Modifier.width(20.0.dp))
        var expanded by remember { mutableStateOf(false) }
        var selectedIndex by remember { mutableStateOf(defaultSelection) }
        Box(modifier = Modifier
            .width(previewWidth.dp)
            .wrapContentSize(Alignment.TopStart)) {
            Row {
                Text(
                    list[defaultSelection], modifier = Modifier.width((previewWidth-20).dp).height(30.dp)
                        .clickable(onClick = { expanded = true }),
                        fontSize = 18.sp,
                )

                Image(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Custom Icon",
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(primaryColor)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(expandedWidth.dp)
                    /*.background(
                        Color.Gray
                    )*/
            ) {
                list.forEachIndexed { index, s ->
                    DropdownMenuItem(onClick = {
                        selectedIndex = index
                        expanded = false
                        onSelected(selectedIndex)
                    }, text = { Text(text = s) })
                }
            }
        }
    }
}