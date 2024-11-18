package a75f.io.renatus.profiles.profileUtils

import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.GrayLabelTextColor
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.profiles.acb.AcbProfileViewModel
import a75f.io.renatus.profiles.dab.DabProfileViewModel
import a75f.io.renatus.profiles.oao.OAOViewModel
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel.Companion.saveConfiguration
import a75f.io.renatus.profiles.system.DabStagedRtuViewModel
import a75f.io.renatus.profiles.system.DabStagedVfdRtuViewModel
import a75f.io.renatus.profiles.system.StagedRtuProfileViewModel
import a75f.io.renatus.profiles.system.VavModulatingRtuViewModel
import a75f.io.renatus.profiles.vav.VavProfileViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import java.util.*

open class UnusedPortsFragment : Fragment() {
    companion object {
        @Composable
        fun UnUsedPortsListView(viewModel: Any) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShowUnUsedPorts(viewModel)
            }
        }

        @Composable
        fun ShowUnUsedPorts(viewModel: Any) {
            val mapOfUnUsedPorts = when (viewModel) {
                is StagedRtuProfileViewModel -> TreeMap(viewModel.viewState.value.unusedPortState)
                is VavModulatingRtuViewModel -> TreeMap(viewModel.viewState.value.unusedPortState)
                is VavProfileViewModel -> TreeMap(viewModel.viewState.unusedPortState)
                is AcbProfileViewModel -> TreeMap(viewModel.viewState.unusedPortState)
                is DabProfileViewModel -> TreeMap(viewModel.viewState.unusedPortState)
                is DabStagedRtuViewModel -> TreeMap(viewModel.viewState.value.unusedPortState)
                is DabStagedVfdRtuViewModel -> TreeMap(viewModel.viewState.value.unusedPortState)
                is OAOViewModel -> TreeMap(viewModel.viewState.unusedPortState)
                else -> null
            }

            if (mapOfUnUsedPorts != null && mapOfUnUsedPorts.isNotEmpty()) {
                val unusedPortsInOneRow = 2
                val unusedPortNamesList = ArrayList(mapOfUnUsedPorts.keys)
                val unusedPortsInList = unusedPortNamesList.chunked(unusedPortsInOneRow)
                Column(modifier = Modifier.fillMaxWidth()) {
                    unusedPortsInList.forEach { unusedPort ->
                        ParameterRow(
                            firstUnusedPort = unusedPort.getOrNull(0),
                            firstUnusedPortState = mapOfUnUsedPorts[unusedPort.getOrNull(0)]
                                ?: false,
                            secondUnusedPort = if (unusedPort.size > 1) unusedPort.getOrNull(1) else null,
                            secondUnusedPortState = if (unusedPort.size > 1)
                                mapOfUnUsedPorts[unusedPort.getOrNull(1)] ?: false else false,
                            viewModel
                        )
                    }
                }
            }
        }

        @Composable
        fun ParameterRow(
            firstUnusedPort: String?,
            firstUnusedPortState: Boolean,
            secondUnusedPort: String?,
            secondUnusedPortState: Boolean,
            viewModel: Any
        ) {
            var currentStateOfFirstUnusedPort by remember { mutableStateOf(firstUnusedPortState) }
            var currentStateOfSecondUnusedPort by remember { mutableStateOf(secondUnusedPortState) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 33.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                firstUnusedPort?.let {
                    if (it.isNotBlank()) {
                        Box(modifier = Modifier.width(208.dp)) {
                            LabelTextView(text = it)
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(
                            defaultSelection = currentStateOfFirstUnusedPort,
                            onEnabled = { currentStatus ->
                                currentStateOfFirstUnusedPort = currentStatus
                                saveConfiguration(viewModel, firstUnusedPort, currentStatus)

                            }
                        )
                        if (currentStateOfFirstUnusedPort) {
                            Box(modifier = Modifier.width(290.dp)) {
                                GrayLabelTextColor(text = "Mapped Externally")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(290.dp))
                        }
                    }
                }
                secondUnusedPort?.let {
                    if (it.isNotBlank()) {
                        Box(modifier = Modifier.wrapContentWidth()) {
                            LabelTextView(text = it)
                        }
                        Spacer(modifier = Modifier.width(30.dp))
                        ToggleButtonStateful(
                            defaultSelection = currentStateOfSecondUnusedPort,
                            onEnabled = { currentStatus ->
                                currentStateOfSecondUnusedPort = currentStatus
                                saveConfiguration(viewModel, secondUnusedPort, currentStatus)
                            }
                        )
                        if (currentStateOfSecondUnusedPort) {
                            GrayLabelTextColor(text = "Mapped Externally")
                        }
                    }
                }
            }
        }

        @Composable
        fun DividerRow(modifier: Modifier = Modifier) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComposeUtil.DashDivider()
            }
        }

        @Composable
        fun LabelUnusedPorts() {
            LabelTextView(text = "Unused ports for External mapping", widthValue = 400)
        }
    }
}