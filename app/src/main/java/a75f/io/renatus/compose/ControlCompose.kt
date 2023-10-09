package a75f.io.renatus.compose

import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 04-09-2023.
 */
@Composable
fun SetPointControlCompose(title: String, state: Boolean,  onEnabled: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.padding(10.dp).width(500.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(PaddingValues(top = 5.dp, start = 10.dp))
                .width(450.dp),
            style = TextStyle(
                fontFamily = myFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.Black,
                textAlign = TextAlign.Left,
            ),
            text = title
        )
        ToggleButton(defaultSelection = state) { onEnabled(it) }
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
                .width(350.dp),
            style = TextStyle(
                fontFamily = myFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                color = Color.Black
            ),
            text = title
        )
        SpinnerElement(defaultSelection, items, unit, itemSelected)
    }

}
