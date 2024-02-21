package a75f.io.renatus.compose

import a75f.io.renatus.R
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 12-06-2023.
 */

@Composable
fun SpinnerElement(defaultSelection:String, items: List<String>, unit: String, itemSelected: (String) -> Unit) {
    val selectedItem = remember { mutableStateOf(defaultSelection) }
    val expanded = remember { mutableStateOf(false) }
    Box(modifier = Modifier
        .wrapContentSize()
        .padding(
            PaddingValues(
                top = 5.dp
            )
        )) {
        Column ( modifier = Modifier.width(200.dp).clickable(onClick = { expanded.value = true }) ) {
            Row {
                Text( fontSize = 20.sp, modifier = Modifier.width(150.dp), fontWeight = FontWeight.Normal,text = "${selectedItem.value} $unit")

                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    contentDescription = "Custom Icon",
                    modifier = Modifier
                        .size(28.dp)
                        .padding(PaddingValues(top = 8.dp)),
                    colorFilter = ColorFilter.tint(ComposeUtil.primaryColor)
                )
            }
            Row {  Divider(modifier = Modifier.width(180.dp))  }
        }


        DropdownMenu(modifier = Modifier.background(Color.White),expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    modifier = Modifier.background(Color.White),
                    text = {  Row { Text(fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp), fontWeight = FontWeight.Normal, text = item)
                                    Text(fontSize = 20.sp,fontWeight = FontWeight.Normal, text = unit) }
                           }, onClick = {
                    selectedItem.value = item
                    expanded.value = false
                    itemSelected(item)
                })
            }
        }
    }
}

