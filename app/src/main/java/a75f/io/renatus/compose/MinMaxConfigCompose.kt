package a75f.io.renatus.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Created by Manjunath K on 04-09-2023.
 */

@Composable
fun ConfigCompose(title: String, defaultSelection:Int, items: List<String>, itemSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.padding(10.dp).width(600.dp)
    ) {
        Box(modifier = Modifier.weight(2f)) {  HeaderTextView(text = title) }
        Box(modifier = Modifier.weight(1f)) {  SpinnerElement(defaultSelection, items) { selected -> itemSelected(selected) } }

    }
}