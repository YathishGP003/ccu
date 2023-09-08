package a75f.io.renatus.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.magnifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 12-06-2023.
 */

@Composable
fun SpinnerElement(defaultSelection:Int, items: List<String>, itemSelected: (String) -> Unit) {
    val selectedItem = remember { mutableStateOf(items[defaultSelection]) }
    val expanded = remember { mutableStateOf(false) }
    Box(modifier = Modifier
        .wrapContentSize()
        .padding(
            PaddingValues(
                start = 30.dp,
                top = 5.dp
            )
        )) {
        Column ( modifier = Modifier.width(100.dp).clickable(onClick = { expanded.value = true }) ) {
            Row {
                Text( fontSize = 20.sp, modifier = Modifier.width(50.dp),text = selectedItem.value)
                Image(

                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Custom Icon",
                    modifier = Modifier.size(24.dp).width(50.dp)
                )
            }
            Row {  Divider()  }
        }


        DropdownMenu(modifier = Modifier.background(Color.White),expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    modifier = Modifier.background(Color.White),
                    text = { Text(fontSize = 20.sp,  text = item) }, onClick = {
                    selectedItem.value = item
                    expanded.value = false
                    itemSelected(item)
                })
            }
        }
    }
}

