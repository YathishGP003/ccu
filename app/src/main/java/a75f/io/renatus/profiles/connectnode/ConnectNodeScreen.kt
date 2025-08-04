package a75f.io.renatus.profiles.connectnode

import a75f.io.api.haystack.modbus.UserIntentPointTags
import a75f.io.renatus.R
import a75f.io.renatus.compose.BoldStyledGreyTextView
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.FormattedTableWithoutHeader
import a75f.io.renatus.compose.GrayLabelTextColor
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextViewNew
import a75f.io.renatus.compose.TableHeaderRow
import a75f.io.renatus.modbus.util.DISPLAY_UI_CAPITALIZED
import a75f.io.renatus.modbus.util.PARAMETER_CAPITALIZED
import a75f.io.renatus.modbus.util.SCHEDULABLE_CAPITALIZED
import a75f.io.renatus.modbus.util.isAllParamsSelected
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.seventyfivef.ph.core.Tags

object ConnectNodeScreen {

    @Composable
    fun EmptyConnectNodeScreen(
        onCancel: () -> Unit = {},
        onSave: () -> Unit = {},
        isNewPairing: Boolean
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderTextView(
                text = "Connect Node",
                fontSize = 34
            )

            Spacer(modifier = Modifier.height(130.dp))

            Image(
                painter = painterResource(id = R.drawable.no_content),
                contentDescription = null
            )

            Spacer(modifier = Modifier.height(24.dp))

            BoldStyledGreyTextView(
                text = "No Model Configured.",
                fontSize = 22
            )

            Spacer(modifier = Modifier.height(4.dp))

            GrayLabelTextColor(
                text = "Go to Site Sequencer to set up a model for this Connect Node.",
                widthValue = 800,
                textAlignment = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            if (isNewPairing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaveTextViewNew("CANCEL", onClick = onCancel)
                    Spacer(modifier = Modifier.width(40.dp))
                    SaveTextViewNew("SAVE", onClick = onSave)
                }
            }
        }
    }

    @Composable
    fun  ConnectNodeConfigScreen(
        onCancel: () -> Unit,
        onSave: () -> Unit,
        viewModel: ConnectNodeViewModel
    ) {
        val scrollState = rememberScrollState()
        var forceUpdate by remember { mutableIntStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            val isDisabled by viewModel.isDisabled.observeAsState(false)
            if (isDisabled) {
                PasteBannerFragment.PasteCopiedConfiguration(
                    onPaste = { viewModel.applyCopiedConfiguration() },
                    onClose = { viewModel.disablePasteConfiguration() }
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                HeaderTextView(
                    text = "Connect Node",
                    fontSize = 34
                )
            }
            viewModel.equipmentDeviceList.forEachIndexed { modelIndex, equipmentDevice ->
                val topPadding = if (modelIndex == 0) 40 else 15
                Column(
                    Modifier
                        .padding(top = topPadding.dp, start = 40.dp, end = 40.dp)
                ) {

                    forceUpdate // triggers recomposition

                    HeaderTextView(
                        text = equipmentDevice.name,
                        fontSize = 20,
                        textAlignment = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val tableColumnsWidth = remember {
                        mutableStateListOf<Float>().apply {
                            repeat(3) { add(0.0f) }
                        }
                    }

                    Row {
                        val toggleCallbackMap = mapOf(
                            DISPLAY_UI_CAPITALIZED to Pair(
                                isAllParamsSelected(equipmentDevice)
                            ) { state: Boolean ->
                                equipmentDevice.registers.forEach { item ->
                                    item.parameters.forEach { param ->
                                        param.isDisplayInUI = state
                                        forceUpdate++
                                    }
                                }
                            }
                        )

                        val columnList = listOf(
                            PARAMETER_CAPITALIZED,
                            DISPLAY_UI_CAPITALIZED,
                            SCHEDULABLE_CAPITALIZED
                        )

                        TableHeaderRow(
                            columnList = columnList,
                            toggleCallbackMap = toggleCallbackMap
                        ) { columnWidth ->
                            tableColumnsWidth[columnWidth.first] = columnWidth.second
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        equipmentDevice.registers.sortedBy { it.registerNumber }.forEachIndexed { index, register ->
                            val param = register.parameters[0]
                            val rowDataList = mutableListOf<Pair<String, Any>>()

                            // Parameter Name Column
                            rowDataList.add("text" to (param.name to Alignment.CenterStart))

                            // Display in UI Column
                            rowDataList.add("toggle" to (param.isDisplayInUI to { state: Boolean ->
                                param.isDisplayInUI = state
                                forceUpdate++
                            }))

                            // Schedulable Column
                            if (param.userIntentPointTags.isEmpty()) {
                                rowDataList.add("text" to ("NA" to Alignment.Center))
                            } else {
                                rowDataList.add("toggle" to (
                                        param.userIntentPointTags.any {it.tagName.equals(Tags.SCHEDULABLE)} to { state: Boolean ->
                                            if(state) {
                                                param.userIntentPointTags.add(UserIntentPointTags().apply {
                                                    tagName = Tags.SCHEDULABLE
                                                })
                                            } else {
                                                param.userIntentPointTags.removeIf { it.tagName.equals(Tags.SCHEDULABLE) }
                                            }
                                            param.setIsSchedulable(state)
                                            forceUpdate++
                                        }
                                        ))
                            }

                            FormattedTableWithoutHeader(
                                rowNo = index,
                                columnWidthList = tableColumnsWidth,
                                rowDataList = rowDataList,
                            )
                        }
                    }
                    if (modelIndex != viewModel.equipmentDeviceList.lastIndex) {
                        Spacer(modifier = Modifier.height(15.dp))
                        ComposeUtil.DashDivider()
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 50.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SaveTextViewNew("CANCEL", onClick = onCancel)
                Spacer(modifier = Modifier.width(40.dp))
                SaveTextViewNew("SAVE", onClick = onSave)
            }
        }
    }
}
