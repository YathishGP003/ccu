package a75f.io.renatus.compose

import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownUnderlineColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greySearchIcon
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.ComposeUtil.Companion.secondaryColor
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuViewModel
import a75f.io.renatus.profiles.system.advancedahu.Option
import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Created by Manjunath K on 12-06-2023.
 */

const val noOfItemsDisplayInDropDown = 8  // This is the number of items to be displayed in the dropdown layout
const val dropDownHeight = 425 // This is the height of the dropdown layout

@Composable
fun SpinnerElement(
    defaultSelection: String,
    items: List<String>,
    unit: String,
    itemSelected: (String) -> Unit
) {
    val selectedItem = remember { mutableStateOf(defaultSelection) }
    val expanded = remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var selectedIndex by remember { mutableStateOf(items.indexOf(defaultSelection)) }
    Box(
        modifier = Modifier
                .wrapContentSize()
                .padding(
                        PaddingValues(
                                top = 5.dp
                        )
                )
    ) {
        Column(modifier = Modifier
                .width(200.dp)
                .clickable(onClick = { expanded.value = true })) {
            Row {
                Text(
                    fontSize = 20.sp,
                    fontFamily = ComposeUtil.myFontFamily,
                    modifier = Modifier.width(150.dp),
                    fontWeight = FontWeight.Normal,
                    text = "${selectedItem.value} $unit"
                )

                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    contentDescription = "Custom Icon",
                    modifier = Modifier
                            .size(28.dp)
                            .padding(PaddingValues(top = 8.dp)),
                    colorFilter = ColorFilter.tint(primaryColor)
                )
            }
            Divider(modifier = Modifier.width(180.dp), color = greyDropDownUnderlineColor)
        }
        val customHeight = getDropdownCustomHeight(items, noOfItemsDisplayInDropDown, dropDownHeight)
        DropdownMenu(
            modifier = Modifier
                    .background(Color.White)
                    .height(customHeight.dp)
                    .width(200.dp)
                    .simpleVerticalScrollbar(lazyListState),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }) {
            LazyColumn(state = lazyListState,
                modifier = Modifier
                        .height((customHeight).dp)
                        .width(200.dp)) {
                itemsIndexed(items) { index, item ->
                    DropdownMenuItem(
                        modifier = Modifier.background(if (item == selectedItem.value) secondaryColor else Color.White),
                        contentPadding = PaddingValues(10.dp),
                        text = {
                            Row {
                                Text(
                                    fontSize = 20.sp,
                                    fontFamily = ComposeUtil.myFontFamily,
                                    modifier = Modifier.padding(end = 10.dp),
                                    fontWeight = FontWeight.Normal,
                                    text = item,
                                )
                                Text(fontSize = 20.sp, fontWeight = FontWeight.Normal, text = unit)
                            }
                        }, onClick = {
                            selectedItem.value = item
                            selectedIndex = index
                            expanded.value = false
                            itemSelected(item)
                        })
                }
            }
            LaunchedEffect(expanded) {
                if(selectedIndex != -1) {
                    lazyListState.scrollToItem(selectedIndex)
                }
            }
        }
    }
}


@Composable
fun SpinnerElementOption(
    defaultSelection: String,
    items: List<Option>,
    unit: String,
    itemSelected: (Option) -> Unit,
    previewWidth : Int = 130,
    viewModel: AdvancedHybridAhuViewModel? = null,
    disabledIndices: List<Int> = emptyList()
) {
    val selectedItem = remember { mutableStateOf(defaultSelection) }
    val expanded = remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var selectedIndex by remember { mutableStateOf(getDefaultSelectionIndex(items, defaultSelection))}
    Box(
        modifier = Modifier
                .wrapContentSize()
                .padding(
                        PaddingValues(
                                top = 5.dp
                        )
                )
    ) {
        Column(modifier = Modifier
                .wrapContentWidth()
                .clickable(onClick = { expanded.value = true })) {
            Row {
                val data = getItemData(selectedItem.value, unit, viewModel)
                Text(
                    fontSize = 20.sp,
                    fontFamily = ComposeUtil.myFontFamily,
                    modifier = Modifier.width(previewWidth.dp),
                    fontWeight = FontWeight.Normal,
                    text = "${data.first} ${data.second}",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    contentDescription = "Custom Icon",
                    modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp)),
                    colorFilter = ColorFilter.tint(primaryColor),
                    alignment = Alignment.CenterEnd
                )
            }
            Divider(modifier = Modifier.width((previewWidth + 30).dp), color = greyDropDownUnderlineColor)
        }
        val customHeight = getDropdownCustomHeight(items, noOfItemsDisplayInDropDown, dropDownHeight)
        DropdownMenu(
            modifier = Modifier
                    .background(Color.White)
                    .width((previewWidth + 30).dp)
                    .height(customHeight.dp)
                    .simpleVerticalScrollbar(lazyListState),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }) {
            LazyColumn(state = lazyListState,
                modifier = Modifier
                        .height((customHeight).dp)
                        .width((previewWidth + 30).dp)) {
                itemsIndexed(items) { index, item ->
                    DropdownMenuItem(
                        modifier = Modifier.background(if (index == selectedIndex) secondaryColor else Color.White),
                        contentPadding = PaddingValues(10.dp),
                        text = {
                            Row {
                                val dataItem = getItemData(item.value, unit, viewModel)
                                Text(
                                    fontSize = 20.sp,
                                    fontFamily = ComposeUtil.myFontFamily,
                                    modifier = Modifier.padding(end = 10.dp),
                                    fontWeight = FontWeight.Normal,
                                    text = dataItem.first
                                )
                                Text(fontSize = 20.sp, fontWeight = FontWeight.Normal, text = dataItem.second)
                            }
                        }, onClick = {
                            selectedItem.value = item.value
                            selectedIndex = index
                            expanded.value = false
                            itemSelected(item)
                        },
                        enabled = !disabledIndices.contains(index))
                }
            }
            LaunchedEffect(expanded) {
                lazyListState.scrollToItem(selectedIndex)
            }
        }
    }
}

@Composable
fun getItemData(value: String, unit: String, viewModel: AdvancedHybridAhuViewModel? = null): Pair<String, String> {
    if (viewModel != null) {
        val isChecked by viewModel.isCelsiusChecked.collectAsState()
        if (isChecked) {
            return if (unit.equals("°F", ignoreCase = true)) {
                val number = BigDecimal(UnitUtils.fahrenheitToCelsiusTwoDecimal(value.toDouble()))
                Pair((number.setScale(1, RoundingMode.DOWN).toString()), "\u00B0C")
            } else {
                Pair(value, unit)
            }
        }
    }
    return Pair(value, unit)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSpinnerElement(
    default: Option,
    allItems: List<Option>,
    unit: String,
    onSelect: (Option) -> Unit,
    width: Int,
    isEnabled: Boolean = true,
    disabledIndices: List<Int> = emptyList()
) {
    val selectedItem = remember { mutableStateOf(default) }
    val expanded = remember { mutableStateOf(false) }
    var searchedOption by rememberSaveable { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    var selectedIndex by remember { mutableStateOf(default.index) }
    Box(
        modifier = Modifier
                .wrapContentSize()
                .padding(
                        PaddingValues(
                                top = 5.dp
                        )
                )
    ) {
        Column(modifier = Modifier
                .width(width.dp)
                .clickable(onClick = { expanded.value = true }, enabled = isEnabled)) {
            Row {
                Text(
                    fontSize = 20.sp,
                    fontFamily = ComposeUtil.myFontFamily,
                    modifier = Modifier.width((width-50).dp),
                    fontWeight = FontWeight.Normal,
                    text = "${ selectedItem.value.dis?: selectedItem.value.dis } $unit",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    contentDescription = "Custom Icon",
                    modifier = Modifier
                            .size(30.dp)
                            .padding(PaddingValues(top = 8.dp)),
                    colorFilter = ColorFilter.tint(if(isEnabled) primaryColor else greyDropDownColor)
                )
            }
            Divider(modifier = Modifier.width((width-20).dp),color = if(expanded.value) Color.Black else greyDropDownUnderlineColor)
        }

        DropdownMenu(
            modifier = Modifier
                    .background(Color.White)
                    .width(width.dp),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }) {
            Column(
                verticalArrangement = Arrangement.Bottom
            ) {

                var searchText by remember { mutableStateOf("") }
                val filteredItems = if (searchText.isEmpty()) {
                    allItems
                } else {
                    allItems.filter { it.value.contains(searchText.replace(" ",""), ignoreCase = true) }
                }
                if (allItems.size > 16) {
                    Row {
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = {
                                Text(fontSize = 20.sp, text = "Search")
                            },
                            singleLine = true,
                            colors = TextFieldDefaults.textFieldColors(
                                focusedIndicatorColor = primaryColor,
                                unfocusedIndicatorColor = ComposeUtil.greyColor,
                                containerColor = Color.White
                            ),
                            modifier = Modifier
                                    .padding(10.dp)
                                    .width((width - 30).dp),
                            textStyle = TextStyle(fontSize = 20.sp, fontFamily = ComposeUtil.myFontFamily),
                            leadingIcon = {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_search),
                                    contentDescription = "Custom Icon",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(greySearchIcon)
                                )
                            },
                                trailingIcon = {
                                    if (searchText.isNotEmpty()) {
                                        Image(
                                                painter = painterResource(id = R.drawable.font_awesome_close),
                                                contentDescription = "Clear Icon",
                                                modifier = Modifier
                                                        .size(24.dp)
                                                        .clickable { searchText = "" },
                                                colorFilter = ColorFilter.tint(primaryColor)
                                        )
                                    }
                                },
                        )
                    }
                }
                    val customHeight = getDropdownCustomHeight(filteredItems, noOfItemsDisplayInDropDown, dropDownHeight)
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                                .height((customHeight).dp)
                                .width(width.dp)
                                .simpleVerticalScrollbar(lazyListState)
                    ) {
                        items(filteredItems) {
                            DropdownMenuItem(
                                modifier = Modifier.background(if (it.value == selectedItem.value.value) secondaryColor else Color.White),
                                contentPadding = PaddingValues(10.dp),
                                text = {
                                    Row {
                                        Text(
                                            fontSize = 20.sp,
                                            fontFamily = ComposeUtil.myFontFamily,
                                            modifier = Modifier.padding(end = 10.dp, start = 10.dp),
                                            fontWeight = FontWeight.Normal,
                                            text = it.dis ?: it.value
                                        )
                                        Text(
                                            fontSize = 20.sp,
                                            fontFamily = ComposeUtil.myFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            text = unit
                                        )
                                    }
                                }, onClick = {
                                    selectedItem.value = it
                                    expanded.value = false
                                    selectedIndex = allItems.indexOf(it)
                                    searchedOption = ""
                                    onSelect(it)
                                }, enabled = !disabledIndices.contains(it.index))
                        }
                    }
                    LaunchedEffect(expanded) {
                        lazyListState.scrollToItem(selectedIndex)
                    }
                }
        }
    }
}

fun formatText(input: String): String {
    val regex = "(?<=[a-zA-Z])(?=\\d)|(?<=\\d)(?=[a-zA-Z])|(?<=[a-z\\d])(?=[A-Z])".toRegex()
    val parsedText = input.split(regex).joinToString(" ").replaceFirstChar { it.uppercase() }
    if (input.contains("co2", true))
        return parsedText.replace("co 2", "CO2", true)
    return parsedText
}

// This method is used to draw the scrollbar for the dropdown list
@SuppressLint("ComposableModifierFactory")
@Composable
fun Modifier.simpleVerticalScrollbar(
    state: LazyListState,
    width: Dp = 6.dp,
): Modifier {
    val targetAlpha = 1f

    val alpha by animateFloatAsState( targetValue = targetAlpha, label = "")

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
                    color = greyDropDownColor,
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
fun getDropdownCustomHeight(list: List<Any>, noOfItemsDisplayInDropDown: Int, heightValue: Int): Int {
    var customHeight = heightValue
    if(list.isNotEmpty()) {
        if(list.size <= noOfItemsDisplayInDropDown) {
            customHeight = (list.size * 48) // 48 is the height and padding for each dropdown item and 5 is the padding
        }
        return customHeight
    }
    return 0
}
fun getDefaultSelectionIndex(items: List<Option>, defaultSelection: String):Int {
    var selectedIndex = 0
    if (items.isEmpty()) {
        return -1
    }

    val isDefaultSelectionIsNumber = defaultSelection.toDoubleOrNull() != null
    var selectedItem = defaultSelection
    if (isDefaultSelectionIsNumber && items[0].value.contains('.')) {
        selectedItem = String.format("%.2f", defaultSelection.toDouble())
    }

    for (i in items.indices) {
        if (items[i].value == selectedItem) {
            selectedIndex = i
            break
        }
    }
    return selectedIndex
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinationSpinner(
    default: Option,
    allItems: List<Option>,
    unit: String,
    onSelect: (Option) -> Unit,
    width: Int,
    isEnabled: Boolean = true,
    disabledIndices: List<Int> = emptyList(),
    analogStartPosition: Int
) {
    val selectedItem = remember { mutableStateOf(default) }
    val expanded = remember { mutableStateOf(false) }
    var searchedOption by rememberSaveable { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    var selectedIndex by remember { mutableStateOf(default.index) }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .width(width.dp)
                .clickable(onClick = { expanded.value = true }, enabled = isEnabled)
        ) {
            Row {
                Text(
                    fontSize = 20.sp,
                    fontFamily = ComposeUtil.myFontFamily,
                    modifier = Modifier.width((width - 50).dp),
                    fontWeight = FontWeight.Normal,
                    text = "${selectedItem.value.dis ?: selectedItem.value.dis} $unit",
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )

                Image(
                    painter = painterResource(id = R.drawable.angle_down_solid),
                    contentDescription = "Custom Icon",
                    modifier = Modifier
                        .size(30.dp)
                        .padding(top = 8.dp),
                    colorFilter = ColorFilter.tint(if (isEnabled) primaryColor else greyDropDownColor)
                )
            }
            Divider(
                modifier = Modifier.width((width - 20).dp),
                color = if (expanded.value) Color.Black else greyDropDownUnderlineColor
            )
        }

        DropdownMenu(
            modifier = Modifier
                .background(Color.White)
                .width(width.dp),
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            Column(verticalArrangement = Arrangement.Bottom) {
                var searchText by remember { mutableStateOf("") }
                val filteredItems = if (searchText.isEmpty()) {
                    allItems
                } else {
                    allItems.filter {
                        it.value.contains(searchText.replace(" ", ""), ignoreCase = true)
                    }
                }

                if (allItems.size > 16) {
                    Row {
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text(fontSize = 20.sp, text = "Search") },
                            singleLine = true,
                            colors = TextFieldDefaults.textFieldColors(
                                focusedIndicatorColor = primaryColor,
                                unfocusedIndicatorColor = ComposeUtil.greyColor,
                                containerColor = Color.White
                            ),
                            modifier = Modifier
                                .padding(10.dp)
                                .width((width - 30).dp),
                            textStyle = TextStyle(
                                fontSize = 20.sp,
                                fontFamily = ComposeUtil.myFontFamily
                            ),
                            leadingIcon = {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_search),
                                    contentDescription = "Search Icon",
                                    modifier = Modifier.size(24.dp),
                                    colorFilter = ColorFilter.tint(greySearchIcon)
                                )
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    Image(
                                        painter = painterResource(id = R.drawable.font_awesome_close),
                                        contentDescription = "Clear Icon",
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { searchText = "" },
                                        colorFilter = ColorFilter.tint(primaryColor)
                                    )
                                }
                            },
                        )
                    }
                }

                val customHeight = getDropdownCustomHeight(
                    filteredItems,
                    noOfItemsDisplayInDropDown,
                    dropDownHeight
                )

                Divider()

                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .height(customHeight.dp)
                        .width(width.dp)
                        .simpleVerticalScrollbar(lazyListState)
                ) {
                    itemsIndexed(filteredItems) { index, option ->
                        if (index == 0) {
                            // 🔹 Header for Relay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Relay",
                                    fontSize = 20.sp,
                                    fontFamily = ComposeUtil.myFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Divider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.LightGray,
                                    thickness = 1.dp
                                )
                            }
                        }
                        if (index == analogStartPosition) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Analog Out",
                                    fontSize = 20.sp,
                                    fontFamily = ComposeUtil.myFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Divider(
                                    modifier = Modifier.weight(1f),
                                    color = Color.LightGray,
                                    thickness = 1.dp
                                )
                            }
                        }

                            // 🔹 Normal selectable row
                            DropdownMenuItem(
                                modifier = Modifier.background(
                                    if (option.value == selectedItem.value.value) secondaryColor else Color.White
                                ),
                                contentPadding = PaddingValues(10.dp),
                                text = {
                                    Row {
                                        Text(
                                            fontSize = 20.sp,
                                            fontFamily = ComposeUtil.myFontFamily,
                                            modifier = Modifier.padding(end = 10.dp, start = 20.dp),
                                            fontWeight = FontWeight.Normal,
                                            text = option.dis ?: option.value
                                        )
                                        Text(
                                            fontSize = 20.sp,
                                            fontFamily = ComposeUtil.myFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            text = unit
                                        )
                                    }
                                },
                                onClick = {
                                    selectedItem.value = option
                                    expanded.value = false
                                    selectedIndex = allItems.indexOf(option)
                                    searchedOption = ""
                                    onSelect(option)
                                },
                                enabled = !disabledIndices.contains(option.index)
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
