package a75f.io.renatus.compose

import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.modbus.util.ALERT
import a75f.io.renatus.modbus.util.OK
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuViewModel
import a75f.io.renatus.profiles.system.advancedahu.Option
import a75f.io.renatus.util.CCUUiUtil
import android.content.Context
import android.text.Spanned
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Created by Manjunath K on 16-08-2023.
 */
class ComposeUtil {
    companion object {
        val  primaryColor = getThemeColor()
        val  secondaryColor = getSecondaryThemeColor()
        var greyColor = Color(android.graphics.Color.parseColor("#666666"))
        val greyDropDownColor = Color(android.graphics.Color.parseColor("#B6B6B6"))
        val greyDropDownScrollBarColor = Color(android.graphics.Color.parseColor("#B6B6B6"))
        val greyDropDownUnderlineColor = Color(android.graphics.Color.parseColor("#CCCCCC"))
        val greySearchIcon = Color(android.graphics.Color.parseColor("#999999"))
        val grey05 = Color(android.graphics.Color.parseColor("#EBECED"))
        val grey06 = Color(android.graphics.Color.parseColor("#F9F9F9"))
        val textColor = Color(android.graphics.Color.parseColor("#333333"))
        val myFontFamily = FontFamily(
            Font(R.font.lato_light, FontWeight.Light),
            Font(R.font.lato_regular, FontWeight.Normal),
            Font(R.font.lato_regular, FontWeight.Medium),
            Font(R.font.lato_bold, FontWeight.Bold)
        )

        private fun getThemeColor(): Color {
            var primaryColor = Color(android.graphics.Color.parseColor("#E24301"))
            if (CCUUiUtil.isDaikinEnvironment(Globals.getInstance().applicationContext))
                primaryColor = Color(android.graphics.Color.parseColor("#FF0097E0"))
            if (CCUUiUtil.isCarrierThemeEnabled(Globals.getInstance().applicationContext))
                primaryColor = Color(android.graphics.Color.parseColor("#1891F6"))
            if (CCUUiUtil.isAiroverseThemeEnabled(Globals.getInstance().applicationContext))
                primaryColor = Color(android.graphics.Color.parseColor("#6FC498"))
            return primaryColor
        }

        //Function to modify the list of strings as per UX which are to be displayed in the dropdown
        //logic can be updated to accomodate formatting of new strings in future if needed  as per UX
        fun getModifiedList(list: List<String>): List<String> {
            val modifiedStrings = mutableListOf<String>()
            for (string in list) {
                var modifiedStringBuilder = StringBuilder(string.capitalize())
                for (index in 1 until modifiedStringBuilder.length) {
                    if (modifiedStringBuilder[index].isUpperCase() && modifiedStringBuilder[index - 1].isLowerCase()){
                        modifiedStringBuilder.insert(index, ' ')
                        break
                    }
                }
                if (!modifiedStringBuilder.first().isDigit() && modifiedStringBuilder.last().isDigit()) {
                    if(!(modifiedStringBuilder.length > 1 && modifiedStringBuilder[modifiedStringBuilder.length - 2].isDigit()))
                        modifiedStringBuilder.insert(modifiedStringBuilder.length - 1, ' ')
                }
                if (modifiedStringBuilder.last().isLetter() && modifiedStringBuilder.last().isLowerCase() && modifiedStringBuilder.length > 1 && modifiedStringBuilder[modifiedStringBuilder.length - 2].isDigit()) {
                    modifiedStringBuilder.insert(modifiedStringBuilder.length - 1, ' ')
                    modifiedStringBuilder.setCharAt(modifiedStringBuilder.length - 1, modifiedStringBuilder[modifiedStringBuilder.length - 1].toUpperCase())
                }
                modifiedStrings.add(modifiedStringBuilder.toString())
            }
            return modifiedStrings
        }

        private fun getSecondaryThemeColor(): Color {
            var secondaryColor = Color(android.graphics.Color.parseColor("#1AE5561A"))
            if (CCUUiUtil.isDaikinEnvironment(Globals.getInstance().applicationContext))
                secondaryColor = Color(android.graphics.Color.parseColor("#1A0097E0"))
            if (CCUUiUtil.isCarrierThemeEnabled(Globals.getInstance().applicationContext))
                secondaryColor = Color(android.graphics.Color.parseColor("#1A1891F6"))
            if (CCUUiUtil.isAiroverseThemeEnabled(Globals.getInstance().applicationContext))
                secondaryColor = Color(android.graphics.Color.parseColor("#1A6FC498"))
            return secondaryColor
        }


        @Composable
        fun DashDivider(height: Dp = 50.dp) {
            LazyColumn(modifier = Modifier.width(1200.dp).height(height)) {
                items(1) {
                    Column(
                        modifier = Modifier.width(1200.dp).height(height)
                    ) {
                        DashDivider(
                            color = greyDropDownUnderlineColor,
                            thickness = 2.dp,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
        @Composable
        fun DashDivider(
            thickness: Dp,
            color: Color,
            phase: Float = 10f,
            intervals: FloatArray = floatArrayOf(10f, 10f),
            modifier: Modifier
        ) {
            Canvas(modifier = modifier.fillMaxWidth()) {
                val dividerHeight = thickness.toPx()
                drawRoundRect(
                    color = color,
                    style = Stroke(
                        width = dividerHeight,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals,
                            phase
                        )
                    )
                )
            }
        }

    }
}

@Composable
fun Title(title: String,modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TitleTextView(title)
    }
}


@Composable
fun SingleOptionConfiguration(
    minLabel: String,
    itemList: List<Option>,
    unit: String,
    minDefault: String,
    onMinSelected: (Option) -> Unit = {},
    viewModel: AdvancedHybridAhuViewModel? = null
) {
    Row(
        modifier = Modifier
            .width(550.dp)
            .padding(bottom = 10.dp)
    ) {
        Box(modifier = Modifier
            .weight(1f)
            .padding(top = 10.dp)) {
            StyledTextView(
                minLabel, fontSize = 20, textAlignment = TextAlign.Left
            )
        }
        Box(modifier = Modifier
            .weight(1f)
            .padding(top = 5.dp)) {
            SpinnerElementOption(defaultSelection = minDefault,
                items = itemList,
                unit = unit,
                itemSelected = { onMinSelected(it) }, viewModel = viewModel)
        }
    }
}


@Composable
fun StagedFanConfiguration(
    label1: String,
    label2: String,
    itemList: List<Option>,
    unit: String,
    minDefault: String,
    maxDefault: String,
    onlabel1Selected: (Option) -> Unit = {},
    onlabel2Selected: (Option) -> Unit = {},
    stage1Enabled: Boolean,
    stage2Enabled: Boolean,
    viewModel: AdvancedHybridAhuViewModel? = null
) {
    val twoOptionSelected = stage1Enabled && stage2Enabled
    Row(
        modifier = Modifier
            .then(if (twoOptionSelected) Modifier.fillMaxWidth() else Modifier.width(550.dp))
            .padding(bottom = 10.dp)
    ) {
        if(stage1Enabled) {
            Box(modifier = Modifier
                .weight(1f)
                .padding(top = 10.dp)) {
                StyledTextView(
                    label1, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier
                .weight(1f)
                .padding(top = 5.dp)) {
                SpinnerElementOption(defaultSelection = minDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onlabel1Selected(it) }, viewModel = viewModel)
            }
        }

        if(stage2Enabled) {
            Box(modifier = Modifier
                .then(if (!twoOptionSelected) Modifier.weight(1f) else Modifier.width(550.dp))
                .weight(1f)
                .padding(top = 10.dp)) {
                StyledTextView(
                    label2, fontSize = 20, textAlignment = TextAlign.Left
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SpinnerElementOption(defaultSelection = maxDefault,
                    items = itemList,
                    unit = unit,
                    itemSelected = { onlabel2Selected(it) }, viewModel = viewModel)
            }
        }
    }
}

fun showErrorDialog(context: Context, message: Spanned,
                        onDismiss: () -> Unit = {}) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle(ALERT)
    builder.setIcon(R.drawable.ic_warning)
    builder.setMessage(message)
    builder.setCancelable(false)
    builder.setPositiveButton(OK) { dialog, _ ->
        dialog.dismiss()
        onDismiss()
    }
    builder.create().show()
}

@Composable
fun DividerLine(modifier: Modifier) {
    Box(
        modifier = modifier.background(colorResource(id = R.color.listview_divider_color)),
    )
}

@Composable
fun TableHeaderRow(
    columnList: List<String>,
    toggleCallbackMap: Map<String, Pair<Boolean, (Boolean) -> Unit>> = emptyMap(),
    onWidthMeasured: (Pair<Int, Float>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorResource(id = R.color.tuner_header_bg))
            .height(50.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        for (i in columnList.indices) {
            val column = columnList[i]
            var boxModifier = Modifier.weight(1f)

            if (i > 0) {
                DividerLine(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .align(Alignment.CenterVertically)
                )

                boxModifier = Modifier
                    .wrapContentWidth(align = Alignment.CenterHorizontally)
            }

            Box(
                modifier = boxModifier
                    .padding(horizontal = 20.dp)
                    .align(Alignment.CenterVertically)
                    .onGloballyPositioned { coordinates ->
                        val widthPx = coordinates.size.width.toFloat()
                        onWidthMeasured(Pair(i, widthPx))
                    },
                contentAlignment = Alignment.Center
            ) {
                toggleCallbackMap[column]?.let {
                    TextViewWithToggle(
                        text = column,
                        defaultSelection = it.first,
                        onEnabled = it.second
                    )
                } ?: run {
                    SubTitleNoPadding(column)
                }
            }
        }
    }
}

fun trimStringByCharacterCount(input: String): String {
    val maxLength = 30
    if (input.length > maxLength) {
        return input.substring(0, maxLength)
    } else {
        return input
    }
}

@Composable
fun FormattedTableWithoutHeader(
    rowNo: Int,
    columnWidthList: SnapshotStateList<Float>,
    rowDataList: List<Pair<String, Any?>>
) {

    var backgroundColor = Color.White
    val hasNestedTable = rowDataList[0].first == "text_with_dropdown"

    if (!hasNestedTable) {
        if (rowNo % 2 != 0) {
            backgroundColor = colorResource(id = R.color.tuner_bg_grey)
        }
    } else if (rowNo > 0) {
        Divider(color = Color.Gray, modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 5.dp))
    }

    Row(
        modifier = Modifier.background(backgroundColor),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in rowDataList.indices) {
            val columnWidth = with(LocalDensity.current) { (columnWidthList[i]).toDp() }
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .width(columnWidth)
                    .defaultMinSize(minHeight = 50.dp),
                contentAlignment = Alignment.Center
             ) {
                when (rowDataList[i].first) {
                    "text" -> {
                        val textUIData = rowDataList[i].second as Pair<String, Alignment>
                        val cellText = textUIData.first
                        LabelTextViewForTable(
                            text = cellText,
                            modifier = Modifier.align(textUIData.second),
                            fontSize = 22
                        )
                    }

                    "nestedText" -> {
                        val textUIData = rowDataList[i].second as Triple<String, Alignment, Dp>
                        val cellText = trimStringByCharacterCount(textUIData.first)
                        LabelTextViewForTable(
                            text = cellText, modifier = Modifier
                                .align(textUIData.second)
                                .padding(start = textUIData.third), fontSize = 22
                        )
                    }

                    "toggle" -> {
                        val toggleCallbackMap =
                            rowDataList[i].second as Pair<Boolean, (Boolean) -> Unit>
                        ToggleButton(defaultSelection = toggleCallbackMap.first, Modifier) {
                            toggleCallbackMap.second(it)
                        }
                    }

                    "text_with_dropdown" -> {
                        val data = rowDataList[i].second as Triple<String, List<Int>, () -> Unit>
                        val text = data.first
                        val imageList = data.second
                        val imageClickEvent = data.third
                        TextViewWithDropdown(
                            modifier = Modifier.align(Alignment.CenterStart),
                            imageList = imageList,
                            text = text
                        ) {
                            imageClickEvent()
                        }
                    }

                    "grouped_columns_with_radio_button" -> {
                        val radioData =
                            rowDataList[i].second as Triple<Triple<List<String?>, Int, List<Int>>, Pair<Int, SnapshotMutableState<Int>>, (Int) -> Unit>
                        val radioTexts = radioData.first.first
                        val modelDefaultState = radioData.first.second
                        val radioOptions = radioData.first.third

                        val nestedRowIndex = radioData.second.first
                        val rememberedSelectedItem = radioData.second.second

                        val onSelect = radioData.third

                        RadioButtonComposeBacnetTerminal(
                            modifier = Modifier.align(Alignment.CenterStart),
                            groupedRowIndex = nestedRowIndex,
                            radioTexts = radioTexts,
                            radioOptions = radioOptions,
                            default = modelDefaultState,
                            selectedItem = rememberedSelectedItem,
                            onSelect = onSelect
                        )
                    }

                    "none" -> {
                        // Do nothing
                    }

                    else -> {
                        // Handle other types if needed
                    }
                }
            }
        }
    }
}

@Composable
fun ExternalConfigDropdownSelector (
    titleText: String,
    isPaired: Boolean,
    selectedItemName: MutableState<String>,
    modelVersion: String,
    onClickEvent: () -> Unit,
    otherUiComposable: @Composable (() -> Unit)? = null,
    isNested: Boolean = false
) {
    Box(
        modifier = if(!isNested) Modifier.fillMaxWidth().padding(top = 40.dp) else Modifier.padding(top = 30.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize(),
            ) {
                HeaderLeftAlignedTextViewNew(
                    titleText,
                    fontSize = 18,
                    Modifier.padding(bottom = 0.dp)
                )
                Row {
                    if (isPaired) {
                        TextViewWithClickNoLeadingSpace(
                            text = selectedItemName,
                            onClick = { },
                            enableClick = false,
                            isCompress = false
                        )
                    } else {
                        TextViewWithClickNoLeadingSpace(
                            text = selectedItemName,
                            onClick = {
                                onClickEvent()
                            },
                            enableClick = true, isCompress = false
                        )
                    }
                    if ((isPaired && !isNested) || modelVersion.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .fillMaxHeight()
                                .align(Alignment.Bottom)
                                .padding(bottom = 20.dp, start = 8.dp)
                        ) {
                            HeaderTextView(
                                "V $modelVersion",
                                fontSize = 18,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                otherUiComposable?.let {
                    it()
                }
            }
        }
    }
}

@Composable
fun annotatedStringBySpannableString(text: String): AnnotatedString {
    return buildAnnotatedString {
        append(text)
        withStyle(
            style = SpanStyle(
                color = Color.Red,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                baselineShift = BaselineShift(0.3f)
            )
        ) {
            append("*")
        }
    }
}

@Composable
fun annotatedStringBySpannableString(text: String, delimiter: String): AnnotatedString {
    return buildAnnotatedString {
        val delimiterIndex = text.indexOf(delimiter)
        if(delimiterIndex != -1) {
            withStyle(
                style = SpanStyle(
                    fontFamily = myFontFamily,
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append(text.substring(0, delimiterIndex))
            }
            append(text.substring(delimiterIndex))
        } else {
            append(text)
        }
    }
}

@Composable
fun ButtonListRow(
    modifier: Modifier = Modifier.fillMaxWidth().padding(end = 20.dp, bottom = 40.dp, top = 40.dp),
    textActionPairMap: Map<String, Pair<Boolean, () -> Unit>>
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End
    ) {
        textActionPairMap.entries.forEachIndexed { index, entry ->
            val buttonText = entry.key
            val buttonEnable = entry.value.first
            val buttonAction = entry.value.second
            if (index > 0) {
                DividerLine(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .align(Alignment.CenterVertically)
                )
            }
            SaveTextView(text = buttonText, isChanged = buttonEnable, fontSize = 22f) {
                buttonAction()
            }
        }
    }
}

@Composable
fun TableHeaderRowByWeight(
    columnList: List<String>,
    weightMap: Map<Int, Float>,
    toggleCallbackMap: Map<String, Pair<Boolean, (Boolean) -> Unit>> = emptyMap(),
    onWidthMeasured: (Pair<Int, Float>) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = colorResource(id = R.color.tuner_header_bg))
            .height(50.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        for (i in columnList.indices) {
            val column = columnList[i]
            var boxModifier = Modifier.weight(weightMap[i]!!)

            if (i > 0) {
                DividerLine(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .align(Alignment.CenterVertically)
                )

//                boxModifier = Modifier
//                    .wrapContentWidth(align = Alignment.CenterHorizontally)
            }

            Box(
                modifier = boxModifier
                    .padding(horizontal = 20.dp)
                    .align(Alignment.CenterVertically)
                    .onGloballyPositioned { coordinates ->
                        val widthPx = coordinates.size.width.toFloat()
                        onWidthMeasured(Pair(i, widthPx))
                    },
                contentAlignment = Alignment.Center
            ) {
                toggleCallbackMap[column]?.let {
                    TextViewWithToggle(
                        text = column,
                        defaultSelection = it.first,
                        onEnabled = it.second
                    )
                } ?: run {
                    SubTitleNoPadding(column)
                }
            }
        }
    }
}