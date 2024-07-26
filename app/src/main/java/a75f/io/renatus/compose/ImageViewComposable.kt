package a75f.io.renatus.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun ImageViewComposable(
    imageResIds: List<Int>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var currentImageIndex by remember { mutableStateOf(0) }
    Box(
        modifier = modifier
            .height(31.dp)
            .width(38.dp)
            //.fillMaxSize()
            .clickable {
                currentImageIndex = (currentImageIndex + 1) % imageResIds.size
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        val painter: Painter = painterResource(id = imageResIds[currentImageIndex])
        Image(
            painter = painter,
            contentDescription = contentDescription
        )
    }
}