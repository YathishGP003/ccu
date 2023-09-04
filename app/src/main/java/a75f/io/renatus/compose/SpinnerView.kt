package a75f.io.renatus.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        Text(
            text = selectedItem.value,
            modifier = Modifier.clickable(onClick = { expanded.value = true })
        )

        DropdownMenu(expanded = expanded.value, onDismissRequest = { expanded.value = false }) {
            items.forEach { item ->
                DropdownMenuItem(text = { Text(text = item) }, onClick = {
                    selectedItem.value = item
                    expanded.value = false
                    itemSelected(item)
                })
            }
        }
    }
}

