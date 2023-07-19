package a75f.io.renatus.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Created by Manjunath K on 12-06-2023.
 */

@Composable
fun ToggleButton(
    defaultSelection : Boolean,
    onEnabled: (Boolean) -> Unit,
    ) {
        Switch(
            modifier = Modifier.padding(PaddingValues(start = 0.dp, top = 0.dp)),
            checked = defaultSelection,
            onCheckedChange = { onEnabled(it) },
            colors = getColors()
        )
}

@Composable
fun getColors(): SwitchColors {
    return SwitchDefaults.colors(
        checkedThumbColor = Color(android.graphics.Color.parseColor("#EC3C10")),
        checkedTrackColor = Color(android.graphics.Color.parseColor("#FF5722")),
        uncheckedThumbColor = Color(android.graphics.Color.parseColor("#ffffff")),
        uncheckedTrackColor = Color(android.graphics.Color.parseColor("#C99393A1")),
    )
}