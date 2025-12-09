package a75f.io.renatus.ui.zonescreen.nontempprofiles.views

import a75f.io.api.haystack.Tags.MONITORING
import a75f.io.renatus.R
import a75f.io.renatus.composables.DetailedViewDropDownHeaderView
import a75f.io.renatus.composables.HeaderLabelView
import a75f.io.renatus.composables.LabelView
import a75f.io.renatus.composables.getDropdownCustomHeight
import a75f.io.renatus.composables.simpleVerticalScrollbar
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownUnderlineColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.ComposeUtil.Companion.secondaryColor
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.viewmodel.NonTempProfileViewModel
import a75f.io.renatus.ui.zonescreen.screens.HeaderRow
import a75f.io.renatus.ui.zonescreen.screens.HeartBeatCompose
import a75f.io.renatus.ui.zonescreen.screens.TextAppearance
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun ComposeView.showHeaderView(
    nonTempProfileViewModel: NonTempProfileViewModel,
    onValueChange: (selectedIndex: Int, point: Any) -> Unit
) {
    setContent {
        HeaderViewList(
            nonTempProfileViewModel,
            onValueChange = { selectedIndex, point ->
                onValueChange(selectedIndex, point)
            })
    }
}


@Composable
fun HeaderViewList(
    nonTempProfileViewModel: NonTempProfileViewModel,
    onValueChange: (
        selectedIndex: Int,
        point: HeaderViewItem
    ) -> Unit,
) {

    val lastUpdatedTime = nonTempProfileViewModel.lastUpdated.value
    val pointList = nonTempProfileViewModel.headerViewPoints

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val style = TextAppearance(R.attr.action_text_appearance)
            Text(
                text = nonTempProfileViewModel.equipName,
                style = style
            )
            Spacer(modifier = Modifier.width(4.dp))

            Box(modifier = Modifier.wrapContentSize()) {
                HeartBeatCompose(
                    isActive = nonTempProfileViewModel.externalEquipHeartBeat,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                )
            }
        }
        Box(modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            HeaderRow(
                lastUpdatedTime,
                onValueChange = { _, _ -> }
            )
        }

        if (pointList.isEmpty()) {
            if (!nonTempProfileViewModel.profile
                    .equals(MONITORING, ignoreCase = true)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(1),
                modifier = Modifier
                    .fillMaxSize()
                    .heightIn(max = 800.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(
                    items = pointList,
                    key = { index, point ->
                        point.id ?: "index_$index"
                    }
                ) { _, point ->
                    HeaderViewGridItem(
                        point = point,
                        modifier = Modifier.fillMaxWidth(),
                        onValueChange = onValueChange
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderViewGridItem(
    point: HeaderViewItem,
    modifier: Modifier = Modifier,
    showCap: Boolean = true,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    Column(modifier = modifier) {
        if (point.usesDropdown) {
            point.disName?.let {
                DetailedViewDropDownHeaderView(
                    label = it,
                    list = point.dropdownOptions,
                    onSelected = { selectedIndex ->
                        onValueChange(selectedIndex, point)
                    },
                    defaultSelection = point.selectedIndex,
                    externalPoint = point,
                    showCap = showCap,
                    onLabelClick = { }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                point.disName?.let { dis ->
                    HeaderLabelView(dis, 0) {                   }
                }
                LabelView(point.currentValue.toString()) {}
            }
        }
    }
}



@Composable
fun EquipStatusView(equipScheduleStatus: HeaderViewItem) {
    HeaderRow(
        equipScheduleStatus,
        onValueChange = { _, _ -> }
    )
}

@Composable
fun ScheduleView(
    schedule: HeaderViewItem,
    modifier: Modifier,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    ScheduleViewItem(point = schedule, modifier, onValueChange = onValueChange)
}

@Composable
fun SpecialScheduleView(
    schedule: HeaderViewItem,
    modifier: Modifier,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    ScheduleViewItem(point = schedule, modifier, onValueChange = onValueChange, showCap = false)
}

@Composable
fun VacationView(
    schedule: HeaderViewItem,
    modifier: Modifier,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    ScheduleViewItem(point = schedule, modifier, onValueChange = onValueChange, showCap = false)
}


@Composable
fun UpdateScheduleButton(
    showEditIcon: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .padding(8.dp)
    ) {
        Icon(
            painter = if (showEditIcon) {
                painterResource(id = R.drawable.ic_edit)
            } else {
                painterResource(id = R.drawable.baseline_visibility_24)
            },
            tint = if (showEditIcon) {
                Color(0xFF000000)
            } else {
                primaryColor
            },
            contentDescription = "Update Schedule"
        )
    }
}

@Composable
fun ScheduleViewItem(
    point: HeaderViewItem,
    modifier: Modifier = Modifier,
    showCap: Boolean = true,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    Column(modifier = modifier) {
        if (point.usesDropdown) {
            point.disName?.let {
                ScheduleDropDownView(
                    label = it,
                    list = point.dropdownOptions,
                    onSelected = { selectedIndex ->
                        onValueChange(selectedIndex, point)
                    },
                    defaultSelection = point.selectedIndex,
                    externalPoint = point,
                    showCap = showCap,
                    onLabelClick = { },
                    disabledIndices = listOf(1)
                )
            }
        }
    }
}




@OptIn(ExperimentalTextApi::class)
@Composable
fun ScheduleDropDownView(
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
                                .size(26.dp)
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
                                    externalPoint?.currentValue = index.toString()
                                    externalPoint?.selectedIndex = index
                                    expanded = false
                                    onSelected(index)
                                },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if(index >= 2){
                                            Spacer(modifier = Modifier.width(2.dp))
                                        }
                                        Text(
                                            item,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = TextStyle(
                                                fontSize = 20.sp,
                                                fontWeight = when (index) {
                                                    1 -> FontWeight.Bold
                                                    else -> FontWeight.Normal
                                                },
                                                color = when (index) {
                                                    1 -> Color.Black
                                                    else -> Color.Unspecified
                                                }
                                            ),
                                            modifier = Modifier.weight(1f).padding(start = if (index >= 2) 4.dp else 0.dp)
                                        )

                                        if (index >= 2) {
                                            Image(
                                                painter = painterResource(id = R.drawable.icon_arrow_right),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .padding(PaddingValues(top = 4.dp)),
                                                colorFilter = ColorFilter.tint(
                                                    if (isEnabled) primaryColor else greyDropDownColor
                                                )
                                            )
                                        }
                                    }
                                },
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




