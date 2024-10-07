package a75f.io.renatus.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TableContent(
    val columnData: List<TableColumns>
)

data class TableColumns(
    val titleOfColumn: String?,
    val listOfColumnContent: List<String>
)
@Composable
fun ScheduleImpactDialog(tableContent: TableContent, heightDp: Dp, index: Int) {
    LazyColumn(
        modifier = Modifier.height(heightDp)
            .wrapContentWidth().semantics { contentDescription = "buildingOccupancyAlertTable$index" }
    ) {
        // Table Header
        item {
            TableRow(
                columns = tableContent.columnData.map { it.titleOfColumn.orEmpty() },
                isHeader = true,
                backgroundColor = ComposeUtil.grey05
            )
        }

        // Table Rows
        items(tableContent.columnData.maxOf { it.listOfColumnContent.size }) { rowIndex ->
            val backgroundColor = if (rowIndex % 2 == 0) Color.Transparent else ComposeUtil.grey06
            TableRow(
                columns = tableContent.columnData.map { it.listOfColumnContent.getOrElse(rowIndex) { "" } },
                backgroundColor = backgroundColor
            )
        }
    }
}

@Composable
fun TableRow(columns: List<String>, isHeader: Boolean = false, backgroundColor: Color = Color.Transparent) {
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxWidth()
            .height(if (isHeader) 54.dp else 80.dp)
            .padding(horizontal = 20.dp, vertical = if (isHeader) 12.dp else 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        columns.forEach { column ->
            Text(
                text = column,
                color = if (isHeader) ComposeUtil.greyColor else ComposeUtil.textColor,
                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                fontSize = 20.sp,
                textAlign = if(isHeader) TextAlign.Center else { TextAlign.Start}
            )
        }
    }
}
