package a75f.io.renatus.profiles.sse

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.SystemRelayMappingView
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SseProfileConfigFragment : BaseDialogFragment(), OnPairingCompleteListener {
    private val viewModel: SseProfileViewModel by viewModels()

    companion object {
        val ID: String = SseProfileConfigFragment::class.java.simpleName
        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): SseProfileConfigFragment {
            val fragment = SseProfileConfigFragment()
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
        rootView.apply {
            setContent {
                ShowProgressBar()
                CcuLog.i(Domain.LOG_TAG, "Show Progress")
            }
        }
        //reloading the UI once's paste button is clicked
        viewModel.isReloadRequired.observe(viewLifecycleOwner) { isDialogOpen ->
            if (isDialogOpen) {
                viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
                    withContext(Dispatchers.Main) {
                        rootView.setContent {
                            RootView()
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@SseProfileConfigFragment)
            withContext(Dispatchers.Main) {
                rootView.setContent { RootView() }
            }
        }
        return rootView
    }

    //@Preview
    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            item {
                val isDisabled by viewModel.isDisabled.observeAsState(false)
                if (isDisabled) {
                    PasteBannerFragment.PasteCopiedConfiguration(
                        onPaste = { viewModel.applyCopiedConfiguration() },
                        onClose = { viewModel.disablePasteConfiguration() }
                    )
                }
            }
            item {
                Column(modifier = Modifier.padding(20.dp)) {

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TitleTextView("SSE")
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    )
                    {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                            )
                            {
                                Spacer(modifier = Modifier.padding(start = 30.dp))
                                Text(
                                    text = "ENABLE",
                                    fontSize = 20.sp,
                                    color = ComposeUtil.greyColor
                                )
                                Spacer(modifier = Modifier.width(270.dp))
                                Spacer(modifier = Modifier.padding(start = 40.dp))
                                Text(
                                    text = "ACTUATOR TYPE",
                                    fontSize = 20.sp,
                                    color = ComposeUtil.greyColor
                                )
                                Spacer(modifier = Modifier.width(230.dp))
                                Spacer(modifier = Modifier.padding(start = 80.dp))
                                Text(
                                    text = "TEST SIGNAL",
                                    fontSize = 20.sp,
                                    color = ComposeUtil.greyColor
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            RelayConfiguration()
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.width(20.dp))
                                Row {
                                    HeaderTextView(
                                        text = "TH1 - Airflow Temperature Sensor",
                                        padding = 10
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    ToggleButtonStateful(
                                        defaultSelection = viewModel.viewState.th1State.value,
                                        onEnabled = {
                                            viewModel.viewState.th1State.value = it
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(60.dp))
                                Row {
                                    HeaderTextView(
                                        text = "TH2 - Use external 10k Temperature Sensor",
                                        padding = 10
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    ToggleButtonStateful(
                                        defaultSelection = viewModel.viewState.th2State.value,
                                        onEnabled = { viewModel.viewState.th2State.value = it }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.width(20.dp))
                                Row {
                                    HeaderTextView(text = "Auto Force Occupied", padding = 10)
                                    Spacer(modifier = Modifier.width(30.dp))
                                    ToggleButtonStateful(
                                        defaultSelection = viewModel.viewState.autoForcedOccupiedState.value,
                                        onEnabled = {
                                            viewModel.viewState.autoForcedOccupiedState.value = it
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.width(190.dp))
                                Row {
                                    HeaderTextView(text = "Auto Away", padding = 10)
                                    Spacer(modifier = Modifier.width(30.dp))
                                    ToggleButtonStateful(
                                        defaultSelection = viewModel.viewState.autoAwayState.value,
                                        onEnabled = { viewModel.viewState.autoAwayState.value = it }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(15.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Spacer(modifier = Modifier.width(20.dp))
                                AnalogIn1(
                                    relayText = "Analog-in1",
                                    relayState = viewModel.viewState.analog1InState.value,
                                    onRelayEnabled = {
                                        viewModel.viewState.analog1InState.value = it
                                    },
                                    mappingSelection = viewModel.viewState.analog1InAssociationIndex.value,
                                    mapping = viewModel.analogIn1AssociationList,
                                    onMappingChanged = {
                                        viewModel.viewState.analog1InAssociationIndex.value = it
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(30.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                val valuesPickerState = rememberPickerState()
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Picker(
                                        modifier = Modifier.width(120.dp),
                                        header = "Temperature\n    Offset",
                                        state = valuesPickerState,
                                        items = viewModel.temperatureOffsetsList,
                                        onChanged = { it: String ->
                                            viewModel.viewState.temperatureOffset.value =
                                                it.toDouble()
                                        },
                                        startIndex = viewModel.temperatureOffsetsList
                                            .indexOf(viewModel.viewState.temperatureOffset.value.toString()),
                                        visibleItemsCount = 3,
                                        textModifier = Modifier.padding(8.dp),
                                        textStyle = TextStyle(
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            val mapOfUnUsedPorts = viewModel.viewState.unusedPortState
                            if (mapOfUnUsedPorts.isNotEmpty()) {
                                UnusedPortsFragment.DividerRow()
                                UnusedPortsFragment.LabelUnusedPorts()
                                UnusedPortsFragment.UnUsedPortsListView(viewModel)
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                SaveTextView(SET) {
                                    viewModel.saveConfiguration()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AnalogIn1(
        relayText: String, relayState: Boolean = false, onRelayEnabled: (Boolean) -> Unit,
        mapping: List<String>, mappingSelection: Int = 0, onMappingChanged: (Int) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = relayText, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(30.dp))
                    ToggleButtonStateful(defaultSelection = relayState, onEnabled = onRelayEnabled)
                }
            }
            Spacer(modifier = Modifier.width(30.dp))
            Spacer(modifier = Modifier.padding(top = 20.dp))
            Column {
                DropDownWithLabel(
                    label = "",
                    list = mapping,
                    previewWidth = 260,
                    expandedWidth = 260,
                    spacerLimit = 0,
                    onSelected = onMappingChanged,
                    defaultSelection = mappingSelection,
                    isEnabled = relayState
                )
            }
        }
    }

    @Composable
    private fun RelayConfiguration() {
        SystemRelayMappingView(
            relayText = "Relay-1",
            relayState = viewModel.viewState.relay1State.value,
            onRelayEnabled = {
                viewModel.viewState.relay1State.value = it
            },
            mappingSelection = viewModel.viewState.relay1AssociationIndex.value,
            mapping = viewModel.relay1AssociationList,
            onMappingChanged = {
                viewModel.viewState.relay1AssociationIndex.value = it
            },
            buttonState = viewModel.getRelayState(DomainName.relay1),
            onTestActivated = {
                viewModel.viewState.testRelay1 = it
                viewModel.sendTestCommand(DomainName.relay1, it, viewModel)
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        SystemRelayMappingView(
            relayText = "Relay-2",
            relayState = viewModel.viewState.relay2State.value,
            onRelayEnabled = {
                viewModel.viewState.relay2State.value = it
            },
            mappingSelection = viewModel.viewState.relay2AssociationIndex.value,
            mapping = viewModel.relay2AssociationList,
            onMappingChanged = {
                viewModel.viewState.relay2AssociationIndex.value = it
            },
            buttonState = viewModel.getRelayState(DomainName.relay2),
            onTestActivated = {
                viewModel.viewState.testRelay2 = it
                viewModel.sendTestCommand(DomainName.relay2, it, viewModel)
            }
        )
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

    override fun getIdString(): String {
        return ID
    }

    override fun onPairingComplete() {
        this@SseProfileConfigFragment.closeAllBaseDialogFragments()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1220
            val height = 672
            dialog.window!!.setLayout(width, height)
        }
    }


}