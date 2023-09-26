package a75f.io.renatus.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 04-09-2023.
 */
@Composable
fun SetPointControlCompose(title: String, state: Boolean,  onEnabled: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.padding(10.dp).width(600.dp)
    ) {
        Box(modifier = Modifier.weight(2f)) {  HeaderTextView(text = title) }
        Box(modifier = Modifier.weight(1f)) {  ToggleButton(defaultSelection = state) { onEnabled(it) } }
    }
}

@Composable
fun SetPointConfig(title: String, defaultSelection:String, items: List<String>, unit: String, itemSelected: (String) -> Unit){
    Row(
        modifier = Modifier.padding(10.dp).width(500.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(PaddingValues(top = 5.dp, start = 10.dp))
                .width(300.dp),
            style = TextStyle(
                fontFamily = ComposeUtil.myFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = Color.Black
            ),
            text = title
        )
        SpinnerElement(defaultSelection, items, unit, itemSelected)
    }

}
