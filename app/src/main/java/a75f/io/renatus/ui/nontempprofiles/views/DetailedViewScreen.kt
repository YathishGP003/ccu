package a75f.io.renatus.ui.nontempprofiles.views

import a75f.io.renatus.R
import a75f.io.renatus.composables.DetailedViewDropDown
import a75f.io.renatus.composables.HeaderLabelView
import a75f.io.renatus.composables.LabelView
import a75f.io.renatus.composables.TemperatureProfileDetailedViewDropDown
import a75f.io.renatus.ui.EDIT_SCHEDULE
import a75f.io.renatus.ui.EDIT_SPECIAL_SCHEDULE
import a75f.io.renatus.ui.EDIT_VACATION_SCHEDULE
import a75f.io.renatus.ui.VIEW_SCHEDULE
import a75f.io.renatus.ui.model.DetailedViewItem
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import a75f.io.renatus.ui.screens.HeaderRow
import a75f.io.renatus.ui.screens.HeartBeatCompose
import a75f.io.renatus.ui.screens.TextAppearance
import a75f.io.renatus.ui.tempprofiles.viewmodel.TempProfileViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

fun ComposeView.showExternalPointsList(
    nonTempProfileViewModel: NonTempProfileViewModel,
    connectNodeDeviceName: String?,
    onValueChange: (selectedIndex: Int, point: Any) -> Unit
) {
    setContent {
        ExternalPointsList(
            nonTempProfileViewModel,
            connectNodeDeviceName,
            onValueChange = { selectedIndex, point ->
                onValueChange(selectedIndex, point)
            })
    }
}


@Composable
fun ExternalPointsList(
    nonTempProfileViewModel: NonTempProfileViewModel,
    connectNodeDeviceName: String?,
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit,
) {
    val lastUpdatedTime = nonTempProfileViewModel.lastUpdated.value
    val pointList = nonTempProfileViewModel.detailedViewPoints
    val equipScheduleStatus = nonTempProfileViewModel.equipStatusPoint.value


    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {
        // this below code is only for connectModule profile type
        if (connectNodeDeviceName != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = connectNodeDeviceName,
                    color = Color.Black,
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
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

            if (lastUpdatedTime.id != null) {
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    HeaderRow(
                        lastUpdatedTime,
                        onValueChange = { _, _ -> }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = nonTempProfileViewModel.equipName,
                color = Color.Black,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))

            val detailedViewItem = nonTempProfileViewModel.detailedViewPoints.firstOrNull()

            if (detailedViewItem != null &&
                detailedViewItem.profileType != "connectModule"
            ) {
                Box(modifier = Modifier.wrapContentSize()) {
                    HeartBeatCompose(
                        isActive = nonTempProfileViewModel.externalEquipHeartBeat,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
        val detailedViewItem = nonTempProfileViewModel.detailedViewPoints.firstOrNull()
        if (detailedViewItem != null &&
            detailedViewItem.profileType != "connectModule"
        ) {
            if (lastUpdatedTime.id != null) {
                Box(modifier = Modifier.padding(bottom = 12.dp)) {
                    HeaderRow(
                        lastUpdatedTime,
                        onValueChange = { _, _ -> }
                    )
                }
            }
        }
        Box(modifier = Modifier.padding(bottom = 12.dp)) {
            HeaderRow(
                equipScheduleStatus,
                onValueChange = { _, _ -> }
            )
        }

        if (pointList.isEmpty()) {
            // no need to show loading for connect module
            if (!nonTempProfileViewModel.profile
                    .equals("", ignoreCase = true)) {
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
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .heightIn(max = 800.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(
                    items = pointList,
                    key = { index, point ->
                        point.id ?: "index_$index"
                    }
                ) { _, point ->
                    ExternalPointGridItem(point = point, onValueChange = onValueChange)
                }
            }
        }
    }
}


@Composable
fun ExternalPointGridItem(
    point: ExternalPointItem,
    onValueChange: (selectedIndex: Int, point: ExternalPointItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        var showToolTip by remember { mutableStateOf(false) }
        var selectedLabel by remember { mutableStateOf("") }

        if (point.usesDropdown) {
            point.dis?.let {
                DetailedViewDropDown(
                    label = it,
                    list = point.dropdownOptions,
                    onSelected = { selectedIndex ->
                        onValueChange(selectedIndex, point)
                    },
                    defaultSelection = point.selectedIndex,
                    spacerLimit = 0,
                    externalPoint = point,
                    onLabelClick = { label: String ->
                        showToolTip = true
                        selectedLabel = label
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                point.dis?.let { dis ->
                    HeaderLabelView(dis, 0) {
                        showToolTip = true
                        selectedLabel = dis
                    }
                }
                LabelView(point.currentValue.toString()) {}
            }
        }
        if (showToolTip) {
            ShowToolTipCompose(selectedLabel) { showToolTip = false }
        }
    }
}

@Composable
fun ShowToolTipCompose(text: String,  onDismiss: () -> Unit = {}) {
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    offset = tapOffset
                }
            }
    ) {
        ShowToolTip(
            text = text,
            anchorOffset = offset,
            onDismiss = { onDismiss() }
        )
    }
}

@Composable
private fun ShowToolTip(
    text: String,
    anchorOffset: Offset,
    onDismiss: () -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        offset = IntOffset(
            x = anchorOffset.x.toInt(),
            y = (anchorOffset.y - 60).toInt() // shift above the label
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = Color.Black,
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Canvas(modifier = Modifier.size(12.dp, 6.dp)) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                drawPath(path, Color.Black)
            }
        }
    }

    // Auto-dismiss in 3 seconds
    LaunchedEffect(Unit) {
        delay(3000)
        onDismiss()
    }
}

fun ComposeView.showTemperatureProfileDetailedView(
    tempProfileViewModel: TempProfileViewModel,
    onValueChange: (selectedIndex: Int, point: Any) -> Unit
) {
    setContent {
        TempDetailedViewCompose(
            tempProfileViewModel,
            onValueChange = { selectedIndex, point ->
                onValueChange(selectedIndex, point)
            })
    }
}



@Composable
fun TempDetailedViewCompose(
    tempProfileViewModel: TempProfileViewModel,
    onValueChange: (
        selectedIndex: Int,
        point: Any
    ) -> Unit,
) {

    val lastUpdatedTime = tempProfileViewModel.lastUpdated.value
    val equipStatus = tempProfileViewModel.equipStatusMessage.value
    val orderedPoints = tempProfileViewModel.detailedViewPoints.values
        .sortedBy { it.displayOrder }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)

    ) {
        // show schedule only for 1st equip; if multiple equips in a room
        if(tempProfileViewModel.showSchedule) ScheduleComposable(tempProfileViewModel, onValueChange)
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val style = TextAppearance(R.attr.action_text_appearance)
            Text(
                text = tempProfileViewModel.equipName,
                style = style
            )
            Spacer(modifier = Modifier.width(4.dp))

            Box(modifier = Modifier.wrapContentSize()) {
                HeartBeatCompose(
                    isActive = tempProfileViewModel.externalEquipHeartBeat,
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
        Box(modifier = Modifier.padding(start = 12.dp, top = 12.dp)) {
            HeaderRow(
                equipStatus,
                onValueChange = { _, _ -> }
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .heightIn(max = 800.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(orderedPoints, key = { it.id ?: it.hashCode().toString() },
                    span = { point ->
                if (point.shouldTakeFullRow) {
                    GridItemSpan(2)
                } else {
                    GridItemSpan(1)
                }
            }) { point ->
                DetailedViewGridItem(point = point, onValueChange = onValueChange)
            }
        }
        Spacer(modifier = Modifier.heightIn(8.dp))
    }
}


@Composable
fun DetailedViewGridItem(
    point: DetailedViewItem,
    onValueChange: (selectedIndex: Int, point: DetailedViewItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (point.usesDropdown) {
            point.disName?.let {
                TemperatureProfileDetailedViewDropDown(
                    label = it,
                    list = point.dropdownOptions,
                    onSelected = { selectedIndex ->
                        onValueChange(selectedIndex, point)
                    },
                    defaultSelection = point.selectedIndex,
                    detailedViewItem = point,
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
fun ScheduleComposable(
    tempProfileViewModel: TempProfileViewModel,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    val equipScheduleStatus = tempProfileViewModel.equipScheduleStatus.value
    val schedule = tempProfileViewModel.schedule.value
    val specialSchedule = tempProfileViewModel.specialSchedule.value
    val vacation = tempProfileViewModel.vacation.value
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        EquipStatusView(equipScheduleStatus)
    }

    Spacer(modifier = Modifier.width(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScheduleView(
                schedule,
                modifier = Modifier.wrapContentWidth(),
                onValueChange = onValueChange
            )
            if (schedule.selectedIndex >= 2) {
                UpdateScheduleButton(false) {
                    onValueChange(schedule.selectedIndex, HeaderViewItem(
                        disName = VIEW_SCHEDULE,
                    ))
                }
            } else {
                UpdateScheduleButton(true) {
                    onValueChange(schedule.selectedIndex, HeaderViewItem(
                        disName = EDIT_SCHEDULE,
                    ))
                }
            }

        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecialScheduleView(
                specialSchedule,
                modifier = Modifier.wrapContentWidth()
            ) { _, _ ->
            }
            UpdateScheduleButton(true) {
                onValueChange(schedule.selectedIndex, HeaderViewItem(
                    disName = EDIT_SPECIAL_SCHEDULE,
                ))
            }
        }
    }

    Spacer(modifier = Modifier.width(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        VacationView(vacation, modifier = Modifier.wrapContentWidth()) { _, _ -> }
        UpdateScheduleButton(true) {
            onValueChange(schedule.selectedIndex, HeaderViewItem(
                disName = EDIT_VACATION_SCHEDULE,
            ))
        }
    }
}




