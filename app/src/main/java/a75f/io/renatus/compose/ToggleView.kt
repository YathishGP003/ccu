package a75f.io.renatus.compose

import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.greyColor
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Manjunath K on 12-06-2023.
 */


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
                    imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(SwitchDefaults.IconSize)
                        .padding(0.dp)
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


@Composable
fun ToggleButtonStateful(
        defaultSelection : Boolean,
        onEnabled: (Boolean) -> Unit,
        isDisabled :Boolean = false,
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
                            imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier
                                .size(SwitchDefaults.IconSize)
                                .padding(0.dp)
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
                ),
                enabled = isDisabled
        )
    }
}

@Composable
fun ToggleButton(
    defaultSelection : Boolean,
    modifier: Modifier = Modifier.padding(end = 20.dp),
    onEnabled:  (Boolean) -> Unit,
) {
    Box(
        modifier = modifier.wrapContentSize(),
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
                    modifier = Modifier
                        .size(SwitchDefaults.IconSize)
                        .padding(0.dp)
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
            ),
        )
    }
}
@Composable
fun ToggleButtonNoImplicitSpacing(
    defaultSelection : Boolean,
    modifier: Modifier = Modifier,
    onEnabled: (Boolean) -> Unit,
) {
    Box(
        modifier = modifier.wrapContentSize(),
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
                    modifier = Modifier
                        .size(SwitchDefaults.IconSize)
                        .padding(0.dp)
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
            ),
        )
    }
}

@Composable
fun LabelWithToggleButton(
    text: String,
    modifier: Modifier = Modifier,
    textWeight: Float = 4f,
    toggleWeight: Float = 1f,
    onEnabled: (Boolean) -> Unit,
    eyeIconEnable: Boolean = false,
    defaultSelection: Boolean = false,
    onEyeIconClick: () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(textWeight)) {
            TextWithEyeIcon(text, eyeIconEnable = eyeIconEnable,onEyeIconClick)
        }
        Box(modifier = Modifier.weight(toggleWeight)) {
            ToggleButton(
                defaultSelection = defaultSelection,
                onEnabled = onEnabled
            )
        }
    }
}

@Composable
fun TextWithEyeIcon(text: String, eyeIconEnable: Boolean = false, onEyeIconClick: () -> Unit) {
    val inlineContentId = "eyeIcon"

    val annotatedText = buildAnnotatedString {
        append(text)

        if (eyeIconEnable) {
            append(" ")
            appendInlineContent(inlineContentId, "[eye]")
        }
    }

    val inlineContent = mapOf(
        inlineContentId to InlineTextContent(
            Placeholder(
                width = 28.sp,
                height = 28.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            IconButton(onClick = onEyeIconClick) {
                Icon(
                    painter = painterResource(R.drawable.pineye),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    )

    Text(
        text = annotatedText,
        inlineContent = inlineContent,
        fontSize = 20.sp,
        fontFamily = myFontFamily
    )
}
