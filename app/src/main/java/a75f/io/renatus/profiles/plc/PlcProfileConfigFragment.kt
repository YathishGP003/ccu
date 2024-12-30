package a75f.io.renatus.profiles.plc

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlcProfileConfigFragment : BaseDialogFragment(), OnPairingCompleteListener {

    private val viewModel : PlcProfileViewModel by viewModels()
    companion object {
        val ID: String = PlcProfileConfigFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): PlcProfileConfigFragment {
            val fragment = PlcProfileConfigFragment()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.NODE_TYPE, nodeType.ordinal)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
        }
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@PlcProfileConfigFragment)
            withContext(Dispatchers.Main) {
                rootView.setContent {
                    RootView()
                }
            }
        }
        return rootView
    }

    @Composable
    fun ShowProgressBar() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = ComposeUtil.primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }
    //@Preview
    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TitleTextView("PI LOOP CONTROLLER")
                }
                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    key(viewModel.viewState.thermistor1InputType,
                             viewModel.viewState.nativeSensorType) {
                        DropDownWithLabel(
                            label = "Analog-in 1 Input Sensor",
                            list = viewModel.analog1InputType,
                            previewWidth = 195,
                            expandedWidth = 195,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.thermistor1InputType = 0.0
                                viewModel.viewState.nativeSensorType = 0.0
                                viewModel.viewState.analog1InputType = selectedIndex.toDouble()
                                if (selectedIndex > 0) {
                                    // Update the target value and error range based on the selected input type
                                    viewModel.pidTargetValue = viewModel.returnTargetValueAi1(selectedIndex)
                                    viewModel.pidProportionalRange = viewModel.returnErrorValueAi1(selectedIndex)
                                    // Reset the target and proportional range values
                                    viewModel.viewState.pidTargetValue = viewModel.pidTargetValue[0].toDouble()
                                    viewModel.viewState.pidProportionalRange = viewModel.pidProportionalRange[1].toDouble()
                                    viewModel.viewState.thermistor1InputType = 0.0
                                    viewModel.viewState.nativeSensorType = 0.0
                                }
                                // We are maintaining this default selection to keep it consistent with how it was in non-DM.
                                if (selectedIndex == 1 || selectedIndex == 5 || selectedIndex == 7
                                    || selectedIndex == 8 || selectedIndex == 9
                                    || selectedIndex == 10 || selectedIndex == 11
                                ) {
                                    viewModel.viewState.pidTargetValue = 5.0
                                }
                            },
                            defaultSelection = viewModel.viewState.analog1InputType.toInt(),
                            spacerLimit = 80,
                            heightValue = 268
                        )
                    }
                    Spacer(modifier = Modifier.width(85.dp))
                    key(viewModel.viewState.analog1InputType,
                        viewModel.viewState.thermistor1InputType,
                        viewModel.viewState.nativeSensorType) {
                        DropDownWithLabel(
                            label = "Target Value",
                            list = viewModel.pidTargetValue,
                            previewWidth = 130,
                            expandedWidth = 150,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.pidTargetValue =
                                    viewModel.pidTargetValue[selectedIndex].toDouble()
                            },
                            defaultSelection = viewModel.pidTargetValue.indexOf(
                                viewModel.viewState.pidTargetValue.toDouble().toString()
                            ).let { index ->
                                when {
                                    viewModel.isDefault -> {
                                        try {
                                            val targetIndex = viewModel.pidTargetValue.indexOf("5.0")
                                            viewModel.isDefault = false
                                            targetIndex
                                        } catch (e: Exception) {
                                            0
                                        }
                                    }
                                    index >= 0 -> index
                                    else -> 0 // Fallback to 0 when 0 is present instead of 0.0
                                }

                            },
                            spacerLimit = 180,
                            heightValue = 211
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    key(viewModel.viewState.analog1InputType,
                        viewModel.viewState.nativeSensorType) {
                        DropDownWithLabel(
                            label = "TH-in1 Input Sensor",
                            list = viewModel.thermistor1InputType,
                            previewWidth = 195,
                            expandedWidth = 195,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.analog1InputType = 0.0
                                viewModel.viewState.nativeSensorType = 0.0
                                viewModel.viewState.thermistor1InputType = selectedIndex.toDouble()
                                if (selectedIndex > 0) {
                                    // Update the target value and error range based on the selected input type
                                    viewModel.pidTargetValue = viewModel.returnTargetValueTH1(selectedIndex)
                                    viewModel.pidProportionalRange = viewModel.returnErrorValueTH1(selectedIndex)
                                    // Reset the target and proportional range values
                                    viewModel.viewState.pidTargetValue = viewModel.pidTargetValue[0].toDouble()
                                    viewModel.viewState.pidProportionalRange = viewModel.pidProportionalRange[1].toDouble()
                                    viewModel.viewState.analog1InputType = 0.0
                                    viewModel.viewState.nativeSensorType = 0.0
                                }
                                // We are maintaining this default selection to keep it consistent with how it was in non-DM.
                                if (selectedIndex == 1 || selectedIndex == 2) {
                                    viewModel.viewState.pidTargetValue = 5.0
                                }
                            },
                            defaultSelection = viewModel.viewState.thermistor1InputType.toInt(),
                            spacerLimit = 130,
                            heightValue = 268
                        )
                    }
                    Spacer(modifier = Modifier.width(85.dp))
                    key(viewModel.viewState.analog1InputType,
                        viewModel.viewState.thermistor1InputType,
                        viewModel.viewState.nativeSensorType) {
                        DropDownWithLabel(
                            label = "Expected Error Range",
                            list = viewModel.pidProportionalRange,
                            previewWidth = 130,
                            expandedWidth = 150,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.pidProportionalRange =
                                    viewModel.pidProportionalRange[selectedIndex].toDouble()
                            },
                            defaultSelection = viewModel.pidProportionalRange.indexOf(
                                viewModel.viewState.pidProportionalRange.toDouble().toString()
                            ).let { index ->
                                if (index >= 0) index else 1 // Fallback to value 2 when 0 is present instead of 0.0
                            },
                            spacerLimit = 80,
                            heightValue = 211
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    key(viewModel.viewState.analog1InputType,
                        viewModel.viewState.thermistor1InputType) {
                        DropDownWithLabel(
                            label = "Native Sensor Input",
                            list = viewModel.nativeSensorType,
                            previewWidth = 195,
                            expandedWidth = 195,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.analog1InputType = 0.0
                                viewModel.viewState.thermistor1InputType = 0.0
                                viewModel.viewState.nativeSensorType = selectedIndex.toDouble()
                                if (selectedIndex > 0) {
                                    // Update the target value and error range based on the selected input type
                                    viewModel.pidTargetValue = viewModel.returnTargetValueNativeSensor(selectedIndex)
                                    viewModel.pidProportionalRange = viewModel.returnErrorValueNativeSensor(selectedIndex)
                                    // Reset the target and proportional range values
                                    viewModel.viewState.pidTargetValue = viewModel.pidTargetValue[0].toDouble()
                                    viewModel.viewState.pidProportionalRange = viewModel.pidProportionalRange[1].toDouble()
                                    viewModel.viewState.analog1InputType = 0.0
                                    viewModel.viewState.thermistor1InputType = 0.0
                                }
                                // We are maintaining this default selection to keep it consistent with how it was in non-DM.
                                if (selectedIndex != 0) {
                                    viewModel.viewState.pidTargetValue =
                                        viewModel.pidTargetValue.get(viewModel.pidTargetValue.size / 2)
                                            .toDouble()
                                }
                            },
                            defaultSelection = viewModel.viewState.nativeSensorType.toInt(),
                            spacerLimit = 135,
                            heightValue = 268
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                Row {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Expected Zero Error at Midpoint", padding = 10)
                    Spacer(modifier = Modifier.width(100.dp))
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.expectZeroErrorAtMidpoint,
                        onEnabled = { viewModel.viewState.expectZeroErrorAtMidpoint = it }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Invert Control Loop Output", padding = 10)
                    Spacer(modifier = Modifier.width(155.dp))
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.invertControlLoopoutput,
                        onEnabled = { viewModel.viewState.invertControlLoopoutput = it }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Use Analog-in2 for dynamic setpoint", padding = 10)
                    Spacer(modifier = Modifier.width(55.dp))
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.useAnalogIn2ForSetpoint,
                        onEnabled = { viewModel.viewState.useAnalogIn2ForSetpoint = it }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Analog-in2 Input Sensor",
                        list = viewModel.analog2InputType,
                        previewWidth = 195,
                        expandedWidth = 195,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.analog2InputType = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.analog2InputType.toInt(),
                        spacerLimit = 82,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(95.dp))
                    DropDownWithLabel(
                        label = "Setpoint Sensor Offset",
                        list = viewModel.setpointSensorOffset,
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.setpointSensorOffset = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.setpointSensorOffset.toInt(),
                        spacerLimit = 50,
                        heightValue = 211
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Analog-Out1 at Min Output",
                        list = viewModel.analog1MinOutput,
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.analog1MinOutput = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.analog1MinOutput.toInt(),
                        spacerLimit = 65,
                        heightValue = 268
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Analog-Out1 at Max Output",
                        list = viewModel.analog1MaxOutput,
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.analog1MaxOutput = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.analog1MaxOutput.toInt(),
                        spacerLimit = 60,
                        heightValue = 268
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Relay 1", padding = 10)
                    Spacer(modifier = Modifier.width(100.dp))
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.relay1OutputEnable,
                        onEnabled = { viewModel.viewState.relay1OutputEnable = it }
                    )

                    Spacer(modifier = Modifier.width(375.dp))
                    HeaderTextView(text = "Relay 2", padding = 10)
                    Spacer(modifier = Modifier.width(100.dp))
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.relay2OutputEnable,
                        onEnabled = { viewModel.viewState.relay2OutputEnable = it }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Turn ON Relay 1",
                        list = viewModel.relay1OnThreshold,
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.relay1OnThreshold = viewModel.relay1OnThreshold[selectedIndex].toDouble()
                        },
                        defaultSelection = viewModel.relay1OnThreshold.indexOf(viewModel.viewState.relay1OnThreshold.toInt().toString()),
                        spacerLimit = 200,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(85.dp))
                    DropDownWithLabel(
                        label = "Turn ON Relay 2",
                        list = viewModel.relay2OnThreshold,
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.relay2OnThreshold = viewModel.relay2OnThreshold[selectedIndex].toDouble()
                        },
                        defaultSelection = viewModel.relay2OnThreshold.indexOf(viewModel.viewState.relay2OnThreshold.toInt().toString()),
                        spacerLimit = 120,
                        heightValue = 211
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Turn OFF Relay 1",
                        list = viewModel.relay1OffThreshold,
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.relay1OffThreshold = viewModel.relay1OffThreshold[selectedIndex].toDouble()
                        },
                        defaultSelection = viewModel.relay1OffThreshold.indexOf(viewModel.viewState.relay1OffThreshold.toInt().toString()),
                        spacerLimit = 190,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(85.dp))
                    DropDownWithLabel(
                        label = "Turn OFF Relay 2",
                        list = viewModel.relay2OffThreshold,
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.relay2OffThreshold = viewModel.relay2OffThreshold[selectedIndex].toDouble()
                        },
                        defaultSelection = viewModel.relay2OffThreshold.indexOf(viewModel.viewState.relay2OffThreshold.toInt().toString()),
                        spacerLimit = 110,
                        heightValue = 211
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))

                val mapOfUnUsedPorts = viewModel.viewState.unusedPortState
                if(mapOfUnUsedPorts.isNotEmpty()) {
                    UnusedPortsFragment.DividerRow()
                    UnusedPortsFragment.LabelUnusedPorts()
                    UnusedPortsFragment.UnUsedPortsListView(viewModel)
                }
                Spacer(modifier = Modifier.height(30.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    SaveTextView(SET) {
                        if (viewModel.viewState.analog1InputType > 0.0 ||
                            viewModel.viewState.thermistor1InputType > 0.0 ||
                            viewModel.viewState.nativeSensorType > 0.0) {
                            
                            viewModel.saveConfiguration()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Please select an input sensor",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    }
                }

            }
        }
    }

    override fun getIdString(): String {
        return PlcProfileConfigFragment.ID
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1265
            val height = 672
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onPairingComplete() {
        this@PlcProfileConfigFragment.closeAllBaseDialogFragments()
    }
}