package a75f.io.renatus.composables

import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem
import a75f.io.api.haystack.modbus.Parameter
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication.context
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownScrollBarColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownUnderlineColor
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.ComposeUtil.Companion.secondaryColor
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.ui.zonescreen.model.DetailedViewItem
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.views.userintent.UserIntentDialog
import a75f.io.renatus.views.userintent.UserIntentDialog.Companion.isDialogAlreadyVisible
import a75f.io.renatus.views.userintent.UserIntentDialogListener
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity

@Composable
fun DropDownWithLabel(
    label: String, list: List<String>, previewWidth: Int = 80, expandedWidth: Int = 100,
    onSelected: (Int) -> Unit, defaultSelection: Int = 0,spacerLimit:Int=80,paddingLimit:Int=0,heightValue:Int= 435
    , isHeader : Boolean = true, isEnabled : Boolean = true, disabledIndices: List<Int> = emptyList(),labelWidth :Int = 216){

    //var modifiedList = ComposeUtil.getModifiedList(list)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
            if (isHeader) {
                HeaderTextView(text = label, padding = paddingLimit)
            } else {
                LabelTextView(text = label,labelWidth)
            }

        }

        Spacer(modifier = Modifier.width(spacerLimit.dp))

        var expanded by remember { mutableStateOf(false) }
        var selectedIndex by remember { mutableIntStateOf(defaultSelection) }
        val lazyListState = rememberLazyListState()
        val noOfItemsDisplayInDropDown = 8  // This is the number of items to be displayed in the dropdown layout
        val dropDownHeight = 435 // This is the default height of the dropdown for 8 items

        Box(modifier = Modifier
            .width((previewWidth + 20).dp)
            .wrapContentSize(Alignment.TopStart)) {
            Column {
                Row {
                    Text(
                        list[selectedIndex],
                        modifier = Modifier
                            .width((previewWidth).dp)
                            .height(35.dp)
                            .clickable(
                                onClick = { if (isEnabled) expanded = true },
                                enabled = isEnabled
                            ),
                        fontSize = 22.sp,
                            maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                    )

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp))
                            .clickable(onClick = { if (isEnabled) expanded = true }),
                        colorFilter = ColorFilter.tint(if(isEnabled) primaryColor
                        else greyDropDownColor)
                    )
                }
                Divider(color = greyDropDownUnderlineColor)
            }

            val customHeight = getDropdownCustomHeight(list, noOfItemsDisplayInDropDown, dropDownHeight)
            Spacer(modifier = Modifier.height(5.dp))
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width((expandedWidth).dp)
                    .height((customHeight).dp)
                    .background(Color.White)
                    .border(0.5.dp, Color.LightGray)
                    .shadow(1.dp, shape = RoundedCornerShape(2.dp))
                    .simpleVerticalScrollbar(lazyListState)

            ) {
                  LazyColumn(state = lazyListState,
                          modifier = Modifier
                              .width((expandedWidth).dp)
                              .height((customHeight).dp)) {

                      itemsIndexed(list) { index, s ->
                          DropdownMenuItem(onClick = {
                              selectedIndex = index
                              expanded = false
                              onSelected(selectedIndex) }, text = { Text(text = s, style = TextStyle(fontSize = 20.sp)) },
                              modifier = Modifier.background(if (index == selectedIndex) secondaryColor else Color.White),
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

@OptIn(ExperimentalTextApi::class)
@Composable
fun DropDownWithoutLabel(
    list: List<String>, maxLengthString: String, maxContainerWidth: Dp,
    onSelected: (Int) -> Unit, defaultSelection: Int = 0,
    isEnabled : Boolean = true, disabledIndices: List<Int> = emptyList()){

    Row {
        var expanded by remember { mutableStateOf(false) }
        var selectedIndex by remember { mutableIntStateOf(defaultSelection) }
        val lazyListState = rememberLazyListState()
        val noOfItemsDisplayInDropDown = 8  // This is the number of items to be displayed in the dropdown layout
        val dropDownHeight = 435 // This is the default height of the dropdown for 8 items

        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        val measuredTextWidthDp = remember(maxLengthString) {
            with(density) {
                val result = textMeasurer.measure(
                    text = AnnotatedString(maxLengthString),
                    style = TextStyle(fontSize = 22.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                result.size.width.toDp()
            }
        }

        val maxAvailableWidth = maxContainerWidth - 30.dp
        val textWidth = (measuredTextWidthDp + 20.dp).coerceAtMost(maxAvailableWidth)

        Box(modifier = Modifier
            .wrapContentWidth()
            .wrapContentSize(Alignment.TopStart)) {
            Column {
                Row {
                    Text(
                        list[selectedIndex],
                        modifier = Modifier
                            .width((textWidth))
                            .height(35.dp)
                            .clickable(
                                onClick = { if (isEnabled) expanded = true },
                                enabled = isEnabled
                            ),
                        fontSize = 22.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Image(
                        painter = painterResource(id = R.drawable.angle_down_solid),
                        contentDescription = "Custom Icon",
                        modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp))
                            .clickable(onClick = { if (isEnabled) expanded = true }),
                        colorFilter = ColorFilter.tint(if(isEnabled) primaryColor
                        else greyDropDownColor)
                    )
                }
                Divider(color = greyDropDownUnderlineColor, modifier = Modifier.width(textWidth + 30.dp))
            }

            val customHeight = getDropdownCustomHeight(list, noOfItemsDisplayInDropDown, dropDownHeight)
            Spacer(modifier = Modifier.height(5.dp))
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(measuredTextWidthDp + 50.dp)
                    .height((customHeight).dp)
                    .background(Color.White)
                    .border(0.5.dp, Color.LightGray)
                    .shadow(1.dp, shape = RoundedCornerShape(2.dp))
                    .simpleVerticalScrollbar(lazyListState)

            ) {
                LazyColumn(state = lazyListState,
                    modifier = Modifier
                        .width(measuredTextWidthDp + 50.dp)
                        .height((customHeight).dp)) {

                    itemsIndexed(list) { index, s ->
                        DropdownMenuItem(onClick = {
                            selectedIndex = index
                            expanded = false
                            onSelected(selectedIndex) }, text = { Text(text = s, style = TextStyle(fontSize = 20.sp)) },
                            modifier = Modifier.background(if (index == selectedIndex) secondaryColor else Color.White),
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

// This method is used to draw the scrollbar for the dropdown list
@Composable
fun Modifier.simpleVerticalScrollbar(
        state: LazyListState,
        width: Dp = 6.dp,
): Modifier {
    val targetAlpha = 1f

    val alpha by animateFloatAsState( targetValue = targetAlpha )

    return drawWithContent {
        drawContent()

        val firstVisibleElementIndex = state.layoutInfo.visibleItemsInfo.firstOrNull()?.index

        // Draw scrollbar if lazy column has content
        if (firstVisibleElementIndex != null) {
            val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
            val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
            val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight
            if(this.size.height > scrollbarHeight) {
                drawRect(
                        color = greyDropDownScrollBarColor,
                        topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
                        size = Size(width.toPx(), scrollbarHeight),
                        alpha = alpha
                )
            }
        }
    }
}
// TODO: This method calculate the height of the dropdown based on the number of items in the list
// If the number of items in the list is less than the noOfItemsDisplayInDropDown, then the height of the dropdown is calculated based on the number of items in the list
fun getDropdownCustomHeight(list: List<String>, noOfItemsDisplayInDropDown: Int, heightValue: Int): Int {
    var customHeight = heightValue
    if(list.isNotEmpty()) {
        if(list.size < noOfItemsDisplayInDropDown) {
            customHeight = (list.size * 55) + 5 // 55 is the height and padding for each dropdown item and 5 is the padding
        }
        return customHeight
    }
    return 0
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun DetailedViewDropDown(
    label: String,
    list: List<String>,
    previewWidth: Int = 80,
    expandedWidth: Int = 100,
    onSelected: (Int) -> Unit,
    defaultSelection: Int = 0,
    spacerLimit: Int = 4,
    paddingLimit: Int = 0,
    isHeader: Boolean = true,
    isEnabled: Boolean = true,
    disabledIndices: List<Int> = emptyList(),
    externalPoint: ExternalPointItem? = null,
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

        val selectedIndex = externalPoint?.selectedIndex ?: defaultSelection
        val selectedText = list.getOrElse(selectedIndex) { "" }

        val textMeasurer = rememberTextMeasurer()

        // width for collapsed (based on selected item only)
        val selectedWidth = remember(selectedText) {
            textMeasurer.measure(
                text = selectedText,
                style = TextStyle(fontSize = 22.sp)
            ).size.width
        }



        Box(
            modifier = Modifier
                .width(externalPoint!!.collapsedWidth)
        ) {
            Column {
                Row {
                    var canOpenUserIntentDialog by remember { mutableStateOf(false) }
                    Text(
                        selectedText,
                        modifier = Modifier
                            .width(externalPoint.collapsedWidth - 30.dp) // minus icon width
                            .clickable(onClick = {
                                if (externalPoint?.canOverride == true) {
                                    canOpenUserIntentDialog = true
                                    return@clickable
                                }
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
                                if (externalPoint?.canOverride == true) {
                                    canOpenUserIntentDialog = true
                                    return@clickable
                                }
                                if (isEnabled) expanded = true
                            }),
                        colorFilter = ColorFilter.tint(
                            if (isEnabled) primaryColor else greyDropDownColor
                        )
                    )

                    if (canOpenUserIntentDialog) {
                        OpenUserIntent(externalPoint, object : UserIntentDialogListener {
                            override fun onFinished(selectedIndexOnDialogFinish: Int) {
                                externalPoint?.currentValue =
                                    selectedIndexOnDialogFinish.toString()
                                onSelected(selectedIndexOnDialogFinish)
                                Toast.makeText(
                                    context,
                                    R.string.settings_saved_successfully,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        })
                        canOpenUserIntentDialog = false
                    }
                }

                Divider(color = greyDropDownUnderlineColor)
            }

            val customHeight = a75f.io.renatus.compose.getDropdownCustomHeight(
                list,
                noOfItemsDisplayInDropDown,
                dropDownHeight
            )

            Spacer(modifier = Modifier.height(5.dp))

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(externalPoint.expandedWidth) // expanded width = longest item
                    .height(customHeight.dp + 10.dp)
                    .background(Color.White)
                    .border(0.5.dp, Color.LightGray)
                    .shadow(1.dp, shape = RoundedCornerShape(2.dp))
                    .simpleVerticalScrollbar(lazyListState)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .width(externalPoint.expandedWidth)
                        .height(customHeight.dp)
                ) {
                    itemsIndexed(list) { index, item ->
                        DropdownMenuItem(
                            onClick = {
                                externalPoint?.currentValue = index.toString()
                                externalPoint?.selectedIndex = index
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
fun OpenUserIntent(
    externalPoint: ExternalPointItem? = null,
    onFinishedListener: UserIntentDialogListener,
) {
    val context = LocalContext.current
    val fragmentManager = (context as FragmentActivity).supportFragmentManager
    if (externalPoint?.point is Parameter) {
        val modbusParam = externalPoint.point as Parameter
        if (!isDialogAlreadyVisible()) {
            val userIntentDialog = UserIntentDialog(modbusParam.logicalId, null,  onFinishedListener)
            userIntentDialog.show(fragmentManager, "UserIntentDialog")
        }
    } else {
        val bacnetZoneViewItem = externalPoint?.point as BacnetZoneViewItem
        if (!isDialogAlreadyVisible()) {
            val userIntentDialog =
                UserIntentDialog(bacnetZoneViewItem.bacnetObj.id, null,  onFinishedListener)
            userIntentDialog.show(fragmentManager, "UserIntentDialog")
        }
    }
}


@Composable
fun HeaderLabelView(
    label: String,
    paddingLimit: Int,
    onLabelClick: (label: String) -> Unit,
) {
    Text(
        modifier = Modifier
            .clickable {
                onLabelClick(label)
            }
            .wrapContentSize()
            .padding(top = paddingLimit.dp),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Normal,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = Color.Black,
        ),
        text = label
    )
}

@Composable
fun LabelView(label: String, onLabelClick: (label: String) -> Unit) {
    Text(
        modifier = Modifier.padding(start = 4.dp)
            .clickable {
                onLabelClick(label)
            }
            .fillMaxWidth(),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = Color.Black,
        ),
        text = label
    )
}


@OptIn(ExperimentalTextApi::class)
@Composable
fun DetailedViewDropDownHeaderView(
    label: String,
    list: List<String>,
    onSelected: (Int) -> Unit,
    defaultSelection: Int = 0,
    paddingLimit: Int = 0,
    isHeader: Boolean = true,
    isEnabled: Boolean = true,
    disabledIndices: List<Int> = emptyList(),
    externalPoint: HeaderViewItem? = null,
    showCap: Boolean = true,
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

        val selectedIndex = externalPoint?.selectedIndex ?: defaultSelection
        val selectedText = list.getOrElse(selectedIndex) { "" }

        val textMeasurer = rememberTextMeasurer()

        // width for collapsed (based on selected item only)
        val selectedWidth = remember(selectedText) {
            textMeasurer.measure(
                text = selectedText,
                style = TextStyle(fontSize = 22.sp)
            ).size.width
        }

        // width for expanded (based on longest item in the list)
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
                    if (showCap) {
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
                }

                Divider(color = greyDropDownUnderlineColor)
            }
            if (showCap) {
                val customHeight =
                    getDropdownCustomHeight(list, noOfItemsDisplayInDropDown, dropDownHeight)

                Spacer(modifier = Modifier.height(5.dp))

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(expandedWidthDp) // expanded width = longest item
                        .height(customHeight.dp + 10.dp)
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
                                    externalPoint?.currentValue = index.toString()
                                    externalPoint?.selectedIndex = index
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
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TemperatureProfileDetailedViewDropDown(
    label: String,
    list: List<String>,
    onSelected: (Int) -> Unit,
    defaultSelection: Int = 0,
    paddingLimit: Int = 0,
    isHeader: Boolean = true,
    isEnabled: Boolean = true,
    disabledIndices: List<Int> = emptyList(),
    detailedViewItem: DetailedViewItem? = null,
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

        val selectedIndex = detailedViewItem?.selectedIndex ?: defaultSelection
        val selectedText = list.getOrElse(selectedIndex) { "" }

        val textMeasurer = rememberTextMeasurer()

        // width for collapsed (based on selected item only)
        val selectedWidth = remember(selectedText) {
            textMeasurer.measure(
                text = selectedText,
                style = TextStyle(fontSize = 22.sp)
            ).size.width
        }

        // width for expanded (based on longest item in the list)
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
        }.coerceIn(80.dp, 400.dp)

        val expandedWidthDp = with(LocalDensity.current) {
            maxItemWidth.toDp() + 40.dp
        }.coerceIn(80.dp, 400.dp)

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
                        overflow = TextOverflow.Clip
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

            val customHeight = a75f.io.renatus.compose.getDropdownCustomHeight(
                list,
                noOfItemsDisplayInDropDown,
                dropDownHeight
            )

            Spacer(modifier = Modifier.height(5.dp))

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .width(expandedWidthDp) // expanded width = longest item
                    .height(customHeight.dp + 10.dp)
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
                                detailedViewItem?.currentValue = index.toString()
                                detailedViewItem?.selectedIndex = index
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
fun StyledLabelView(label: String, onLabelClick: (label: String) -> Unit) {

    val annotatedText = buildAnnotatedString {
        label.split(" ").forEach { word ->
            when (word.uppercase()) {
                "ON", "OFF" -> withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                    append("$word ")
                }
                else -> append("$word ")
            }
        }
    }

    Text(
        modifier = Modifier.padding(start = 4.dp)
            .clickable {
                onLabelClick(label)
            }
            .fillMaxWidth(),
        style = TextStyle(
            fontFamily = myFontFamily,
            fontWeight = FontWeight.Normal,
            fontStyle = FontStyle.Normal,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = Color.Black,
        ),
        text = annotatedText,
    )
}

