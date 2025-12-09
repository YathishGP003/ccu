package a75f.io.renatus.ui.zonescreen.screens

import a75f.io.renatus.R
import a75f.io.renatus.composables.HeaderLabelView
import a75f.io.renatus.composables.LabelView
import a75f.io.renatus.composables.getDropdownCustomHeight
import a75f.io.renatus.composables.simpleVerticalScrollbar
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownUnderlineColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.ComposeUtil.Companion.secondaryColor
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import android.util.TypedValue
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import android.graphics.Typeface
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun HeaderRow(
    headerViewItem: HeaderViewItem,
    onValueChange: (
        selectedIndex: Int,
        point: HeaderViewItem
    ) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (headerViewItem.usesDropdown) {
            headerViewItem.disName?.let {
                HeaderViewDropDown(
                    label = it,
                    list = headerViewItem.dropdownOptions,
                    onSelected = { selectedIndex ->
                        onValueChange(selectedIndex, headerViewItem)
                    },
                    defaultSelection = headerViewItem.selectedIndex,
                    headerViewItem = headerViewItem,
                    onLabelClick = {}
                )
            }
        } else {
            headerViewItem.disName?.let { disName ->
                HeaderLabelView(disName, 0) {}
            }
            LabelView(headerViewItem.currentValue.toString()) {}
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun HeaderViewDropDown(
    label: String,
    list: List<String>,
    onSelected: (Int) -> Unit,
    defaultSelection: Int = 0,
    paddingLimit: Int = 0,
    isHeader: Boolean = true,
    isEnabled: Boolean = true,
    disabledIndices: List<Int> = emptyList(),
    headerViewItem: HeaderViewItem? = null,
    onLabelClick: (label: String) -> Unit,

    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
            if (isHeader) {
                HeaderLabelView(label, paddingLimit, onLabelClick)
            } else {
                LabelView(label) {}
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        var expanded by remember { mutableStateOf(false) }
        val lazyListState = rememberLazyListState()
        val noOfItemsDisplayInDropDown = 8
        val dropDownHeight = 435

        val selectedIndex = headerViewItem?.selectedIndex ?: defaultSelection
        val selectedText = list.getOrElse(selectedIndex) { "" }

        val textMeasurer = rememberTextMeasurer()

        val selectedWidth = remember(selectedText) {
            textMeasurer.measure(
                text = selectedText,
                style = TextStyle(fontSize = 22.sp)
            ).size.width
        }

        val maxItemWidth = remember(list) {
            list.maxOfOrNull { item ->
                textMeasurer.measure(
                    text = item,
                    style = TextStyle(fontSize = 22.sp)
                ).size.width
            } ?: 0
        }

        val collapsedWidthDp = with(LocalDensity.current) {
            selectedWidth.toDp() + 40.dp
        }.coerceIn(80.dp, 250.dp)

        val expandedWidthDp = with(LocalDensity.current) {
            maxItemWidth.toDp() + 40.dp
        }.coerceIn(80.dp, 300.dp)

        Box(
            modifier = Modifier
                .width(collapsedWidthDp)
        ) {
            Column {
                Row {
                    Text(
                        selectedText,
                        modifier = Modifier
                            .width(collapsedWidthDp - 30.dp) // minus icon width
                            .clickable(onClick = {
                                if (isEnabled) expanded = true
                            }, enabled = isEnabled),
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 4.dp))
                            .clickable(onClick = {
                                if (isEnabled) expanded = true
                            }),
                        colorFilter = ColorFilter.tint(
                            if (isEnabled) primaryColor else greyDropDownColor
                        )
                    )
                }

                Divider(color = greyDropDownUnderlineColor)
            }

            val customHeight =
                getDropdownCustomHeight(list, noOfItemsDisplayInDropDown, dropDownHeight)

            Spacer(modifier = Modifier.height(5.dp))

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(expandedWidthDp) // expanded width = longest item
                    .height(customHeight.dp)
                    .background(Color.White)
                    .border(0.5.dp, Color.LightGray)
                    .shadow(1.dp, shape = RoundedCornerShape(2.dp))
                    .simpleVerticalScrollbar(lazyListState)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .width(expandedWidthDp)
                        .height(customHeight.dp)
                ) {
                    itemsIndexed(list) { index, item ->
                        DropdownMenuItem(
                            onClick = {
                                headerViewItem?.currentValue = index.toString()
                                headerViewItem?.selectedIndex = index
                                expanded = false
                                onSelected(index)
                            },
                            text = { Text(item, style = TextStyle(fontSize = 20.sp)) },
                            modifier = Modifier.background(
                                if (index == selectedIndex) secondaryColor else Color.White
                            ),
                            contentPadding = PaddingValues(10.dp),
                            enabled = !disabledIndices.contains(index)
                        )
                    }
                }

                LaunchedEffect(expanded) {
                    lazyListState.scrollToItem(selectedIndex)
                }
            }
        }
    }
}
@Composable
fun HeartBeatCompose(
    isActive: Boolean = false,
    modifier: Modifier
) {
    val color = if (isActive) Color(0xFF02C18D) else Color(0xFF999999)

    Box(
        modifier = modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color)
    )
}


@Suppress("ResourceType")
@Composable
fun TextAppearance(attrResId: Int): TextStyle {
    val context = LocalContext.current
    val theme = context.theme
    val density = LocalDensity.current

    val typedValue = TypedValue()
    if (!theme.resolveAttribute(attrResId, typedValue, true)) {
        // fallback to default TextStyle if attribute not found
        return TextStyle.Default
    }
    val styleResId = typedValue.resourceId
    if (styleResId == 0) {
        return TextStyle.Default
    }

    val attrs = theme.obtainStyledAttributes(
        styleResId,
        intArrayOf(
            android.R.attr.textSize,
            android.R.attr.textColor,
            android.R.attr.textStyle,
            android.R.attr.fontFamily
        )
    )

    val fontSizeSp = with(density) {
        val size = attrs.getDimension(0, 16f)
        size.toSp()
    }
    val textColor = Color(attrs.getColor(1, android.graphics.Color.BLACK))

    val fontWeight = when (attrs.getInt(2, Typeface.NORMAL)) {
        Typeface.BOLD, Typeface.BOLD_ITALIC -> FontWeight.Bold
        else -> FontWeight.Normal
    }

    val fontFamilyResId = attrs.getResourceId(3, 0)
    val fontFamily = if (fontFamilyResId != 0) {
        try {
            FontFamily(Font(fontFamilyResId))
        } catch (e: Exception) {
            FontFamily.Default
        }
    } else {
        FontFamily.Default
    }

    attrs.recycle()

    return TextStyle(
        color = textColor,
        fontSize = fontSizeSp,
        fontWeight = fontWeight,
        fontFamily = fontFamily
    )
}


@Composable
fun titleGreyStyle(): TextStyle {
    return TextStyle(
        fontSize = 30.sp,
        color = colorResource(id = R.color.hint_textcolor),
        fontFamily = FontFamily(Font(R.font.lato_light)),
        fontWeight = FontWeight.Normal,
        lineHeight = 30.sp
    )
}