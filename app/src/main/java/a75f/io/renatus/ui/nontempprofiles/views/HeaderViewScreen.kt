package a75f.io.renatus.ui.nontempprofiles.views

import a75f.io.renatus.R
import a75f.io.renatus.composables.DetailedViewDropDownHeaderView
import a75f.io.renatus.composables.HeaderLabelView
import a75f.io.renatus.composables.LabelView
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.viewmodel.NonTempProfileViewModel
import a75f.io.renatus.ui.screens.HeaderRow
import a75f.io.renatus.ui.screens.HeartBeatCompose
import a75f.io.renatus.ui.screens.TextAppearance
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

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
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
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
                    HeaderViewGridItem(point = point, onValueChange = onValueChange)
                }
            }
        }
    }
}

@Composable
fun HeaderViewGridItem(
    point: HeaderViewItem,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

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





