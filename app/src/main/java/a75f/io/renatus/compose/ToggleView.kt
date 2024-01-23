package a75f.io.renatus.compose

import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        Switch(
            checked = defaultSelection,
            onCheckedChange = {
                onEnabled(it)
            },
            thumbContent = {
                Icon(
                    imageVector = if (defaultSelection) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize).padding(0.dp)
                )
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                uncheckedIconColor = greyColor,
                uncheckedTrackColor = greyColor,
                checkedIconColor = primaryColor,
                checkedTrackColor = primaryColor,
                uncheckedBorderColor = greyColor,
                checkedBorderColor = primaryColor
            )
        )
    }
}

/**
 * Manages the mutableState within the composable.
 */
@Composable
fun ToggleButtonStateful(
    defaultSelection : Boolean,
    onEnabled: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier.wrapContentSize(),
        contentAlignment = Alignment.Center
    ) {
        var checked by remember { mutableStateOf(defaultSelection) }
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onEnabled(it)
            },
            thumbContent = {
                Icon(
                    imageVector = if (defaultSelection) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize).padding(0.dp)
                )
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                uncheckedThumbColor = Color.White,
                uncheckedIconColor = greyColor,
                uncheckedTrackColor = greyColor,
                checkedIconColor = primaryColor,
                checkedTrackColor = primaryColor,
                uncheckedBorderColor = greyColor,
                checkedBorderColor = primaryColor
            )
        )
    }
}
