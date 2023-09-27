package a75f.io.renatus.compose

import a75f.io.renatus.modbus.util.DISPLAY_UI
import a75f.io.renatus.modbus.util.PARAMETER
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Created by Manjunath K on 20-07-2023.
 */

@Composable
fun ParameterLabel() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(2f)) { SubTitle(PARAMETER) }
        Box(modifier = Modifier.weight(1f)) { SubTitle(DISPLAY_UI) }
        Box(modifier = Modifier.weight(2f)) { SubTitle(PARAMETER) }
        Box(modifier = Modifier.weight(1f)) { SubTitle(DISPLAY_UI) }
    }
}