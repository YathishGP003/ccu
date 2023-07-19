package a75f.io.renatus.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 18-07-2023.
 */


@Composable
fun HeaderTextView(text: String) {
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
        text = text
    )
}


@Composable
fun LabelTextView(text: String) {
    Text(
        modifier = Modifier
            .padding(PaddingValues(top = 5.dp, start = 20.dp))
            .width(400.dp),
        style = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            color = Color.Black
        ),
        text = text
    )
}

@Composable
fun TitleTextView(text: String) {
    Text(
        modifier = Modifier
            .padding(10.dp)
            .height(100.dp)
            .width(200.dp),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 33.5.sp,
            color = Color(android.graphics.Color.parseColor("#E24301")),
        ),
        text = text
    )
}

@Composable
fun SubTitle(text: String) {
    Text(
        modifier = Modifier
            .height(50.dp),
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            fontSize =  18.sp,
            color = Color.Gray
        ),
        text = text
    )
}
@Composable
fun SingleLineTitle(text: String) {
    Text(
        modifier = Modifier
            .height(100.dp)
            .fillMaxWidth(),
        style = TextStyle(
            textAlign = TextAlign.Left,
            fontFamily = FontFamily.SansSerif,
            fontSize =  18.sp,
            color = Color.Gray
        ),
        text = text
    )
}