package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.mesh.DeviceUtil
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.composables.SwitchWithLabelOnRight
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment.Companion.DividerRow
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.AddProgressGif
import a75f.io.renatus.util.TestSignalManager
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DabModulatingRtuFragment : DModulatingRtuFragment() {

    private val dabModulatingViewModel: DabModulatingRtuViewModel by viewModels()

    fun hasUnsavedChanged(): Boolean{
        return dabModulatingViewModel.hasUnsavedChanges()
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext()).apply {
            setContent {
                AddProgressGif()
                CcuLog.i(Domain.LOG_TAG, "Show Progress")
            }
        }

        viewLifecycleOwner.lifecycleScope.launch (highPriorityDispatcher) {
            dabModulatingViewModel.init(
                    requireContext(),
                    CCUHsApi.getInstance()
            )
            withContext(Dispatchers.Main) {
                rootView.setContent {
                    CcuLog.i(Domain.LOG_TAG, "Hide Progress")
                    RootView()
                }
            }

        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
            }

            override fun onViewDetachedFromWindow(v: View) {
                if (Globals.getInstance().isTestMode) {
                    Globals.getInstance().isTestMode = false
                    TestSignalManager.restoreAllPoints()
                }
            }
        })
    }

    @Preview
    @Composable
    fun RootView() {
        val viewState = dabModulatingViewModel.viewState
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            item {
                // Adaptive delta and maximized exit water buttons
                var dcwbEnabledMutableState by remember { mutableStateOf(viewState.value.isDcwbEnabled) }
                // State to show or hide alert dialog
                var showDialog by remember { mutableStateOf(false) }
                var changeAnalog4Values = remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Image(
                        painter = if (dcwbEnabledMutableState) painterResource(id = R.drawable.input_dab_fullyahu_ao4) else painterResource(
                            id = R.drawable.input_dab_fullyahu
                        ),
                        contentDescription = "Relays",
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(top = 42.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    ) {
                        key(dabModulatingViewModel.viewState.value.isStateChanged) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 5.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.width(20.dp))
                                Text(
                                    text = "ENABLE",
                                    fontSize = 20.sp,
                                    color = ComposeUtil.greyColor
                                )
                                Spacer(modifier = Modifier.width(230.dp))
                                Text(
                                    text = "MAPPING",
                                    fontSize = 20.sp,
                                    color = ComposeUtil.greyColor
                                )
                                Spacer(modifier = Modifier.width(200.dp))
                                Text(
                                    text = "TEST SIGNAL",
                                    fontSize = 20.sp,
                                    color = ComposeUtil.greyColor
                                )
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            AnalogOutComposable(
                                viewModel = dabModulatingViewModel,
                                dcwbEnabledMutableState
                            )
                            // Analog out 4 composable when dcwb is enabled
                            if (dcwbEnabledMutableState) {
                                Spacer(modifier = Modifier.height(16.dp))
                                SystemAnalogOutMappingViewWithList(
                                    analogName = "Analog-Out 4",
                                    analogOutState = viewState.value.isAnalog4OutputEnabled,
                                    onAnalogOutEnabled = {
                                        viewState.value.isAnalog4OutputEnabled = it
                                        dabModulatingViewModel.setStateChanged()
                                        viewState.value.unusedPortState =
                                            UnusedPortsModel.setPortState(
                                                "Analog 4 Output",
                                                it,
                                                dabModulatingViewModel.profileConfiguration
                                            )
                                    },
                                    mappingSpace = 15.dp,
                                    mappingSelection = viewState.value.analog4Association,
                                    mapping = dabModulatingViewModel.analog4AssociationList,
                                    onMappingChanged = {
                                        viewState.value.analog4Association = it
                                        dabModulatingViewModel.setStateChanged()
                                        changeAnalog4Values.value = true
                                    },
                                    analogOutValList = (0..100).map { it.toDouble().toString() },
                                    analogOutVal =
                                    try {
                                        (0..100).map { it }.indexOf(
                                            Domain.cmBoardDevice.analog4Out.readHisVal().toInt()
                                        )
                                    } catch (e: UninitializedPropertyAccessException) {
                                        // When the cmBoardDevice is uninitialized after registration
                                        (0..100).map { it }.indexOf(20)
                                    },
                                    onAnalogOutChanged = {
                                        viewState.value.analogOut4OutSideAirTestSignal =
                                            it.toDouble()
                                        dabModulatingViewModel.sendAnalogRelayTestSignal(
                                            DomainName.analog4Out,
                                            it.toDouble()
                                        )
                                    },
                                    dropDownWidthPreview = 100,
                                    dropdownWidthExpanded = 120,
                                    mappingTextSpacer = 20
                                )
                            }
                            RelayComposable(viewModel = dabModulatingViewModel)
                        }
                    }
                }


                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),  // Makes the row take up the full width of the screen
                ) {
                    Spacer(modifier = Modifier.padding(start = 280.dp))
                    SwitchWithLabelOnRight(
                        label = "DCWB Enable",
                        isChecked = dcwbEnabledMutableState,
                        onCheckedChange = {
                            // Reset the button state
                            viewState.value.isAnalog1OutputEnabled = false
                            viewState.value.isAnalog4OutputEnabled = false
                            if(it) {
                                dcwbEnabledMutableState = true
                                showDialog = true
                            } else {
                                viewState.value.isDcwbEnabled = false
                                dabModulatingViewModel.setStateChanged()
                                dcwbEnabledMutableState = false
                            }
                        }
                    )
                    // Alert Dialog to show when switch is toggled
                    if (showDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                // When the user clicks outside the dialog or back button do nothing. Keep the dialog ON
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically // Align icon and text vertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_dialog_alert),
                                        contentDescription = "Warning",
                                        modifier = Modifier
                                            .size(40.dp) // Adjust the size of the icon
                                            .padding(end = 8.dp) // Add space between icon and text
                                    )
                                    Text(
                                        text = "Please Configure the BTU meter's\nModbus parameters",
                                        fontSize = 16.sp
                                    )
                                }

                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        dcwbEnabledMutableState = true
                                        showDialog = false // Close dialog on OK click
                                        viewState.value.isDcwbEnabled = true
                                        viewState.value.isAdaptiveDeltaEnabled = true
                                        dabModulatingViewModel.setStateChanged()
                                    }
                                ) {
                                    Text("PROCEED")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        // Action on "Cancel"
                                        dcwbEnabledMutableState = false
                                        showDialog = false
                                        viewState.value.isDcwbEnabled = false
                                        dabModulatingViewModel.setStateChanged()
                                    }
                                ) {
                                    Text("CANCEL")
                                }
                            }
                        )
                    }
                }
                if (dcwbEnabledMutableState) {
                    DcwbEnabledAnalogView(
                        dabModulatingViewModel,
                        viewState.value.analog4Association > 0
                    )
                } else {
                    DcwbDisabledAnalogView(dabModulatingViewModel)
                }
            }

            if(dabModulatingViewModel.viewState.value.unusedPortState.isNotEmpty()) {
                item {
                    DividerRow()
                }
                item {
                    LabelTextView(text = "Unused ports for External mapping", widthValue = 400)
                }
                item {
                    UnusedPortsFragment.UnUsedPortsListView(dabModulatingViewModel)
                }
            }

            item {
                SaveConfig()
            }
        }
    }

    @Composable
    fun SaveConfig() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 20.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 5.dp)),
                contentAlignment = Alignment.Center
            ) {
                SaveTextView(CANCEL, dabModulatingViewModel.viewState.value.isStateChanged) {
                    dabModulatingViewModel.reset()
                    dabModulatingViewModel.viewState.value.isSaveRequired = false
                    dabModulatingViewModel.viewState.value.isStateChanged = false
                }
            }
            Divider(
                modifier = Modifier
                    .height(25.dp)
                    .width(2.dp)
                    .padding(bottom = 6.dp),
                color = Color.LightGray
            )
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.Center
            ) {
                SaveTextView(SAVE, dabModulatingViewModel.viewState.value.isSaveRequired) {
                    dabModulatingViewModel.saveConfiguration()
                    dabModulatingViewModel.viewState.value.isSaveRequired = false
                    dabModulatingViewModel.viewState.value.isStateChanged = false
                }
            }
        }
    }
}