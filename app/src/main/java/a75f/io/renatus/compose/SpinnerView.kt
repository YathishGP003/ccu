package a75f.io.renatus.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.renovo.bacnet4j.type.enumerated.LifeSafetyMode.enabled

/**
 * Created by Manjunath K on 12-06-2023.
 */

@Composable
fun SpinnerView(
    selected: Int, items: MutableState<List<String>>, disable: Boolean, itemSelected: (Int, String) -> Unit
) {
    var selectedText by remember {
        if (items.value.isEmpty()) mutableStateOf("Select Device") else mutableStateOf(
            items.value[selected]
        )
    }
    var selectedIndex by remember { mutableStateOf(0) }
    var expand by remember { mutableStateOf(false) }
    var stroke by remember { mutableStateOf(1) }
    Box(
        modifier = Modifier
            .padding(12.dp)
            .border(
                border = BorderStroke(stroke.dp, Color.Black), shape = RoundedCornerShape(4.dp)
            )
            .clickable {
                expand = true
                stroke = if (expand) 2 else 1
            }, contentAlignment = Alignment.Center,
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(6.dp)
        ) {

            Text(
                text = selectedText,
                color = Color.Black,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(horizontal = 5.dp, vertical = 5.dp)
                    .width(200.dp)
                    .wrapContentHeight(),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            if (!disable) {
                DropdownMenu(
                    expanded = expand, onDismissRequest = {
                        expand = false
                        stroke = if (expand) 2 else 1
                    }, properties = PopupProperties(
                        focusable = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                    ), modifier = Modifier
                        .background(White)
                        .padding(2.dp)
                        .width(400.dp)
                ) {
                    items.value.forEachIndexed { index, item ->
                        DropdownMenuItem(text = {
                            Text(
                                text = item,
                                color = Color.Black,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }, onClick = {
                            selectedIndex = index
                            expand = false
                            stroke = if (expand) 2 else 1
                            itemSelected(index, item)
                            selectedText = item
                        })
                    }
                }
            }
        }
    }
}