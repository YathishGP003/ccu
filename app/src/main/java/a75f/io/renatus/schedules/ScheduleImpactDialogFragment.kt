package a75f.io.renatus.schedules

import a75f.io.logic.schedule.PossibleScheduleImpactTable
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ScheduleImpactDialog
import a75f.io.renatus.compose.TableColumns
import a75f.io.renatus.compose.TableContent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment


class ScheduleImpactDialogFragment(
    listOfScheduleImpact: MutableList<ScheduleImpact>,
    private val mOnScheduleUpdateListener: BuildingOccupancyDialogListener
) : DialogFragment() {
    private val scheduleImpactList = listOfScheduleImpact


    data class ScheduleImpact(
        val floorName: String,
        val zoneName: String,
        val impactedSlots: String,
        val scheduleType: PossibleScheduleImpactTable
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent {
                ScheduleImpactDialog()
            }
            return rootView
        }
    }
    interface BuildingOccupancyDialogListener {
        fun onForceTrimSchedule()
        fun onReEditSchedule()
    }
    @Composable
    private fun ScheduleImpactDialog() {
    val groupedTableContent = remember(scheduleImpactList) { prepareTableContent(scheduleImpactList) }
    val isNamedScheduledZoneExists = remember(scheduleImpactList) {
        scheduleImpactList.any {
            it.scheduleType in listOf(
                PossibleScheduleImpactTable.NAMED_SEVEN_DAY,
                PossibleScheduleImpactTable.NAMED_WEEKDAY_SATURDAY_SUNDAY,
                PossibleScheduleImpactTable.NAMED_WEEKDAY_WEEKEND,
                PossibleScheduleImpactTable.NAMED_EVERYDAY
            )
        }
    }
    val isZoneScheduledZoneExists = remember(scheduleImpactList) {
        scheduleImpactList.any {
            it.scheduleType in listOf(
                PossibleScheduleImpactTable.SEVEN_DAY,
                PossibleScheduleImpactTable.WEEKDAY_SATURDAY_SUNDAY,
                PossibleScheduleImpactTable.WEEKDAY_WEEKEND,
                PossibleScheduleImpactTable.EVERYDAY
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(24.dp, 28.dp, 24.dp, 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dialog_alert),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text(
                        text = getHeaderText(isNamedScheduledZoneExists, isZoneScheduledZoneExists),
                        color = ComposeUtil.textColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 18.dp)
                            .wrapContentWidth().semantics { contentDescription = "buildingOccupancyAlertHeader" },
                        fontSize = 24.sp
                    )
                    Text(
                        text = getBottomText(isNamedScheduledZoneExists),
                        color = ComposeUtil.textColor,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(start = 18.dp),
                        fontSize = 20.sp
                    )
                }
            }

            LazyColumn(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 36.dp)) {
                var index = 0
                items(groupedTableContent.entries.toList()) { (scheduleType, tableContent) ->
                    ScheduleTypeGroupSection(scheduleType, tableContent, index)
                    index++
                }
            }
        }

        ActionButtons(
            isNamedScheduledZoneExists = isNamedScheduledZoneExists,
            mOnScheduleUpdateListener = mOnScheduleUpdateListener,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ScheduleTypeGroupSection(
    scheduleType: PossibleScheduleImpactTable, tableContent: TableContent, index: Int
) {
    val scheduleTypeGroup = scheduleType.group
    val descriptionText = buildAnnotatedString {
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append("${tableContent.columnData[0].columnSize}")
        }
        append(" zones following ${getScheduleType(scheduleType)} with ")
        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
            append(scheduleTypeGroup)
        }
        append(" schedule group are impacted:")
    }

    Column {
        Text(
            text = descriptionText,
            color = ComposeUtil.textColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .wrapContentWidth().semantics { contentDescription = "buildingOccupancyAlertTitle$index" },
            fontSize = 20.sp
        )
        ScheduleImpactDialog(tableContent, 80.dp * (tableContent.columnData[0].listOfColumnContent.size) + 54.dp, index)
    }
}

@Composable
private fun ActionButtons(isNamedScheduledZoneExists: Boolean, mOnScheduleUpdateListener: BuildingOccupancyDialogListener
                          , modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
            .background(Color.White),
        horizontalArrangement = if (isNamedScheduledZoneExists) Arrangement.Center else Arrangement.End
    ) {
        if (isNamedScheduledZoneExists) {
            TextButton(onClick = {
                mOnScheduleUpdateListener.onReEditSchedule()
                dismiss()
            }) {
                Text("RE-EDIT", color = ComposeUtil.primaryColor, fontSize = 20.sp)
            }
        } else {
            TextButton(onClick = {
                mOnScheduleUpdateListener.onReEditSchedule()
                dismiss()
            }) {
                Text("RE-EDIT", color = ComposeUtil.primaryColor, fontSize = 20.sp)
            }

            Text("|", color = ComposeUtil.greyDropDownUnderlineColor, fontSize = 20.sp, modifier = Modifier.padding(5.dp, 4.dp, 5.dp, 0.dp))


            TextButton(onClick = {
                mOnScheduleUpdateListener.onForceTrimSchedule()
                dismiss()
            }) {
                Text("FORCE TRIM & SAVE", color = ComposeUtil.primaryColor, fontSize = 20.sp)

            }
        }
    }
}

private fun getHeaderText(isNamedScheduledZoneExists: Boolean, isZoneScheduledZoneExists: Boolean): String {
    return when {
        isNamedScheduledZoneExists && isZoneScheduledZoneExists -> "Shared & Zone Schedule is outside Building Occupancy"
        isNamedScheduledZoneExists -> "Shared Schedule is outside Building Occupancy"
        else -> "Zone Schedule is outside Building Occupancy"
    }
}

    private fun getBottomText(isNamedScheduledZoneExists: Boolean): String {
        return when {
            isNamedScheduledZoneExists -> "for below zones"
            else -> "Force trimming will affect the zones listed below," +
                    " and they will move to an unoccupied state for those respective days."
        }
    }

private fun prepareTableContent(scheduleImpacts: List<ScheduleImpact>): Map<PossibleScheduleImpactTable, TableContent> {
    return scheduleImpacts.groupBy { it.scheduleType }.mapValues { entry ->
        val floorNames = entry.value.map { it.floorName }
        val zoneNames = entry.value.map { it.zoneName }
        val impactedSlots = entry.value.map { it.impactedSlots }
        val uniqueZoneNameSize = zoneNames.toSet().size
        TableContent(
            columnData = listOf(
                TableColumns("Floor Name", floorNames, uniqueZoneNameSize),
                TableColumns("Zone Name", zoneNames, uniqueZoneNameSize),
                TableColumns("Impacted Slots", impactedSlots, uniqueZoneNameSize)
            )
        )
    }
}


    private fun getScheduleType(scheduleType: PossibleScheduleImpactTable): String {
        return when (scheduleType) {
            PossibleScheduleImpactTable.SEVEN_DAY -> "Zone schedule"
            PossibleScheduleImpactTable.WEEKDAY_SATURDAY_SUNDAY -> "Zone schedule"
            PossibleScheduleImpactTable.WEEKDAY_WEEKEND -> "Zone schedule"
            PossibleScheduleImpactTable.EVERYDAY -> "Zone schedule"

            else -> {
                "Shared schedule"
            }
        }

    }
    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            val width = 1120
            dialog!!.window!!.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}


