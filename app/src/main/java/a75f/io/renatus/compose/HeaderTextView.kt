package a75f.io.renatus.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 12-06-2023.
 */

@Composable
fun HeaderTextView(title: String) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(top = 5.dp, start = 5.dp, end = 5.dp))
            .width(200.dp),
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            color = Color.Black
        ),
        text = title
    )
}

