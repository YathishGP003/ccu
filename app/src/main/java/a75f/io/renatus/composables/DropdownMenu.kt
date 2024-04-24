package a75f.io.renatus.composables

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
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
    onSelected: (Int) -> Unit, defaultSelection: Int = 0,spacerLimit:Int=80,paddingLimit:Int=0,heightValue:Int=275) {

    var modifiedList = ComposeUtil.getModifiedList(list)
    Row {
        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
            HeaderTextView(text = label, padding = paddingLimit)
        }

        Spacer(modifier = Modifier.width(spacerLimit.dp))

        var expanded by remember { mutableStateOf(false) }
        var selectedIndex by remember { mutableStateOf(defaultSelection) }
        Box(modifier = Modifier
            .width((previewWidth+20).dp)
            .wrapContentSize(Alignment.TopStart)) {
            Column() {
                Row {
                    Text(
                        modifiedList[defaultSelection],
                        modifier = Modifier.width((previewWidth ).dp).height(35.dp)
                            .clickable(onClick = { expanded = true }),
                        fontSize = 22.sp,
                    )

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
                    .height(heightValue.dp)
            ) {
                modifiedList.forEachIndexed { index, s ->
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