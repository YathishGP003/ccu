package a75f.io.renatus.composables

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.HeaderTextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Divider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DropDownWithLabel(
    label: String, list: List<String>, previewWidth: Int = 80, expandedWidth: Int = 100,
    onSelected: (Int) -> Unit, defaultSelection: Int = 0) {
    Row {
        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
            if(label.equals("K-Factor")) {
                HeaderTextView(text = label, padding = 10)
            }
            else{
                HeaderTextView(text = label, padding = 0)
            }
        }
        if(label.equals("Damper Type")){
            Spacer(modifier = Modifier.width(178.0.dp))
        }
        else if(label.equals("Valve Type")){
            Spacer(modifier = Modifier.width(205.0.dp))
        }
        else if(label.equals("Zone Priority")){
            Spacer(modifier=Modifier.width(132.dp))
        }
        else if(label.equals("Size")){
            Spacer(modifier=Modifier.width(20.dp))
        }
        else if(label.equals("Shape")){
            Spacer(modifier=Modifier.width(23.dp))
        }
        else if(label.equals("K-Factor")){
            Spacer(modifier=Modifier.width(182.dp))
        }
        else if(label.equals("Thermistor-2")){
            Spacer(modifier=Modifier.width(131.dp))
        }
        else{
            Spacer(modifier = Modifier.width(80.dp))
        }

        var expanded by remember { mutableStateOf(false) }
        var selectedIndex by remember { mutableStateOf(defaultSelection) }
        Box(modifier = Modifier
            .width((previewWidth+20).dp)
            .wrapContentSize(Alignment.TopStart)) {
            Column() {
                Row {
                    Text(
                        list[defaultSelection],
                        modifier = Modifier.width((previewWidth - 20).dp).height(30.dp)
                            .clickable(onClick = { expanded = true }),
                        fontSize = 22.sp,
                    )

                    if(label.equals("Damper Type") || label.equals("Valve Type")){
                        Spacer(modifier=Modifier.width(10.dp))
                    }

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp)),
                        colorFilter = ColorFilter.tint(primaryColor)
                    )
                }
                Divider(color = Color.Gray)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(expandedWidth.dp)
                    .height(275.dp)
                /*.background(
                    Color.Gray
                )*/
            ) {
                list.forEachIndexed { index, s ->
                    DropdownMenuItem(onClick = {
                        selectedIndex = index
                        expanded = false
                        onSelected(selectedIndex) }, text = { Text(text = s,style= TextStyle(fontSize=19.sp)) }
                    )
                }
            }

        }
    }
}