package a75f.io.renatus.schedules

import PointDefinition
import a75f.io.logic.bo.util.getValueByEnum
import a75f.io.logic.schedule.ScheduleGroup
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.FormattedTableWithoutHeader
import a75f.io.renatus.compose.HeaderLeftAlignedTextViewNew
import a75f.io.renatus.compose.LabelBoldTextViewForTable
import a75f.io.renatus.compose.LabelTextViewForTable
import a75f.io.renatus.compose.SaveTextViewNew
import a75f.io.renatus.compose.TableHeaderRowByWeight
import a75f.io.renatus.compose.TextViewWithDropdown
import a75f.io.renatus.compose.annotatedStringBySpannableString
import a75f.io.renatus.util.ProgressDialogUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider

class CustomControlDialog(val roomRef: String) : BaseDialogFragment() {

    private lateinit var viewModel : CustomControlViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())

        viewModel = ViewModelProvider(this)[CustomControlViewModel::class.java]
        viewModel.configModelView(roomRef)

        rootView.apply {
            setContent {
                RootView()
            }
            return rootView
        }
    }
    override fun getIdString(): String {
        TODO("Not yet implemented")
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }

    @Composable
    fun RootView() {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(44.dp))
        {
            if(viewModel.isScheduleLoading.value) {
                LoadingView()
            } else {
                DialogHeader(dialogTitle = viewModel.dialogTitle.value)
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    for (i in viewModel.isPointDropdownClicked) {
                        PointScheduleEventDisplay_v1(itemIndex = i.key)
                    }
                }
                CloseButton(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
        if(viewModel.isDialogExpanded.value) {
            ShowDialog(
                modifier = Modifier,
                scheduleInfo = viewModel.expandedDialogInfo
            )
        }
    }

    @Composable
    fun DialogHeader(dialogTitle: String) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                HeaderLeftAlignedTextViewNew(text = dialogTitle)
            }
            Legends()
        }
    }

    @Composable
    fun Legends() {
        Row(modifier = Modifier.wrapContentWidth(Alignment.End)) {
            LegendLabel(modifier = Modifier.align(Alignment.CenterVertically), legendColor = R.color.occupancy_occupied, legendText = "CUSTOM VALUE")
            Spacer(modifier = Modifier.width(10.dp))
            LegendLabel(modifier = Modifier.align(Alignment.CenterVertically), legendColor = R.color.occupancy_unoccupied, legendText = "DEFAULT VALUE")
        }
    }

    @Composable
    fun LegendLabel(modifier : Modifier, legendColor : Int, legendText : String) {
        Box(modifier = modifier
            .size(20.dp)
            .background(color = colorResource(id = legendColor)))
        Spacer(modifier = Modifier.width(6.dp))
        LabelTextViewForTable(text = legendText, modifier = Modifier, textColor = Color.Gray)
    }

    @Composable
    fun PointScheduleEventDisplay_v1(itemIndex: Int) {
        val pointData = viewModel.pointCustomControlInfoMap[itemIndex]
        Column(modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()) {
            if(itemIndex > 0) {
                Spacer(modifier = Modifier.height(12.dp))
            }
            TextViewWithDropdown(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 15.dp),
                imageList = listOf(
                    R.drawable.arrow_right_disabled,
                    R.drawable.arrow_down),
                text = annotatedStringBySpannableString(text = "${pointData.equipName} (${pointData.group}) - ${pointData.pointName}: ${pointData.pointDefinitionRef?.let { "${pointData.scheduleName} (${ScheduleGroup.values()[pointData.scheduleGroup!!].group})" } ?: run { "" }}", delimiter = ":"),
                fontSize = 22
            ) {
                viewModel.isPointDropdownClicked[itemIndex] = !viewModel.isPointDropdownClicked[itemIndex]!!
            }
            if(viewModel.isPointDropdownClicked[itemIndex] == true) {
                pointData.pointDefinitionRef?.let { pointDefinitionId ->
                        PointScheduleUI(
                            pointData.scheduleGroup!!,
                            viewModel.pointDefinitionMap[pointDefinitionId]!!,
                            pointData.enums,
                            pointData.unit
                        )
                }
                pointData.eventDefinitionRefs?.let {eventScheduleRefList ->
                    if(eventScheduleRefList.isNotEmpty()) {
                        EventsTable(isEventsPresent = true, eventScheduleRefList = eventScheduleRefList, enumString = pointData.enums, unit = pointData.unit)
                    }
                }
            } else {
                Divider()
            }
        }
    }

    @Composable
    fun CloseButton(modifier: Modifier) {
        Column(modifier = modifier
            .wrapContentHeight()) {
            Spacer(modifier = Modifier.height(46.dp))
            SaveTextViewNew(text = "CLOSE", onClick = {dismiss()})
        }
    }

    @Composable
    fun EventsTable(isEventsPresent: Boolean, eventScheduleRefList: List<String>, enumString: String?, unit: String?) {
        if(isEventsPresent) {
            // Stores the width of each column for the table.
            val tableColumnsWidth = remember {
                mutableStateListOf<Float>().apply {
                    repeat(3) {
                        add(0.0f)
                    }
                }
            }

            // List of Column Names to be displayed in the table header.
            val columnList =
                listOf("Event Name", "Start & End Date", "Value")

            val weightMap = mapOf(0 to 1.4f, 1 to 1.8f, 2 to 0.6f)
            TableHeaderRowByWeight(columnList = columnList, weightMap = weightMap)  {
                    columnWidth ->
                tableColumnsWidth[columnWidth.first] = columnWidth.second
            }

            eventScheduleRefList.forEachIndexed { index, eventScheduleRef ->
                val eventDefinitionApplicable = viewModel.eventDefinitionMap[eventScheduleRef]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {

                    val rowDataList = mutableListOf<Pair<String, Any>>()

                    // For Point Name Column
                    rowDataList.add(
                        Pair(
                            "text", Pair(
                                eventDefinitionApplicable?.eventName ?: "Unnamed Event",
                                Alignment.CenterStart
                            )
                        )
                    )

                    rowDataList.add(
                        Pair(
                            "text", Pair(
//                                "Mar 19, 2025 | 08:00 to Mar 21, 2025 | 08:00",
                                eventDefinitionApplicable?.eventDateTimeRange ?: "NA",
                                Alignment.Center
                            )
                        )
                    )

                    rowDataList.add(
                        Pair(
                            "text", Pair(
                                getValueByEnum(eventDefinitionApplicable?.eventCustomValue!!, enumString, unit),
                                Alignment.CenterStart
                            )
                        )
                    )

                    FormattedTableWithoutHeader(
                        rowNo = index,
                        columnWidthList = tableColumnsWidth,
                        rowDataList = rowDataList,
                    )
                }
            }
        }
    }

    @Composable
    fun PointScheduleUI(scheduleGroup: Int, pointDefinition: PointDefinition, enumString: String?, unit: String?) {
        val containerId = remember { View.generateViewId() }

        AndroidView(
            factory = {context ->
            FrameLayout(context).apply {
                id = containerId
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            } }, update = {
            childFragmentManager.findFragmentById(containerId) ?: run {
                val fragment = ScheduleGroupFragment().showPointCustomSchedulePreviewLayout(scheduleGroup,
                    pointDefinition, enumString, unit)
                fragment.apply {
                    pointScheduleUiClicked = { dataMap ->
                        viewModel.setIsDialogExpanded(true, dataMap)
                    }
                }
                val fragmentTransaction = childFragmentManager.beginTransaction()
                fragmentTransaction.replace(containerId, fragment)
                fragmentTransaction.commitAllowingStateLoss()
            }
        })
    }

    @Composable
    fun ShowDialog(modifier: Modifier, scheduleInfo: Map<String, String>) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = { viewModel.setIsDialogExpanded(false) },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier
                    .wrapContentSize()
                    .background(colorResource(id = R.color.black_transparent))
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Row {
                        val scheduleGroup = "${scheduleInfo["scheduleGroup"]} "
                        val scheduleRange = scheduleInfo["timeRange"] ?: ""
                        val separator = " | "
                        val valueType = scheduleInfo["valueType"] ?: ""
                        val annotatedTitle = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    fontFamily = ComposeUtil.myFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(id = R.color.white)
                                )
                            ) {
                                append(scheduleGroup)
                            }
                            append(scheduleRange)
                            append(separator)
                            withStyle(
                                style = SpanStyle(
                                    color = colorResource(id = R.color.active_schedule),
                                )
                            ) {
                                append(valueType)
                            }
                        }

                        LabelTextViewForTable(text = annotatedTitle, fontColor = Color.White)

                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LabelBoldTextViewForTable(
                            text = "Unit Command",
                            fontColor = colorResource(id = R.color.gray_text)
                        )
                        LabelTextViewForTable(
                            text = scheduleInfo["value"] ?: "",
                            modifier = Modifier,
                            textColor = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun LoadingView() {
        ProgressDialogUtils.showProgressDialog(context, "Loading Custom Control Data...")
    }
}