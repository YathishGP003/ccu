package a75f.io.renatus.profiles.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.dab.DabEquipToBeDeleted.CARRIER_PROD
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.rememberPickerState
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

class DabProfileConfigFragment : BaseDialogFragment(), OnPairingCompleteListener {

    private val viewModel: DabProfileViewModel by viewModels()

    companion object {
        val ID: String = DabProfileConfigFragment::class.java.simpleName
        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): DabProfileConfigFragment {
            val fragment = DabProfileConfigFragment()
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
        }
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
                viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
                viewModel.setOnPairingCompleteListener(this@DabProfileConfigFragment)
                withContext(Dispatchers.Main) {
                    rootView.setContent {
                        RootView()
                    }
                }
        }
        return rootView
    }

    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (BuildConfig.BUILD_TYPE) {
                        CARRIER_PROD -> TitleTextView("VVT-C")
                        else -> TitleTextView("DAB")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    val valuesPickerState = rememberPickerState()
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Picker(
                            modifier = Modifier.width(120.dp),
                            header = "Temperature\n    Offset",
                            state = valuesPickerState,
                            items = viewModel.temperatureOffsetsList,
                            onChanged = { it: String ->
                                viewModel.viewState.temperatureOffset = it.toDouble()
                            },
                            startIndex = viewModel.temperatureOffsetsList
                                .indexOf(viewModel.viewState.temperatureOffset.toString()),
                            visibleItemsCount = 3,
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(78.dp))
                    DropDownWithLabel(
                        label = "Damper1 Type",
                        list = viewModel.damper1TypesList,
                        previewWidth = 233,
                        expandedWidth = 253,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.damper1Type = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.damper1Type.toInt(),
                        spacerLimit = 105,
                        heightValue = 272
                    )
                    Spacer(modifier = Modifier.width(80.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        DropDownWithLabel(
                            label = "Size",
                            list = viewModel.damper1SizesList,
                            previewWidth = 60,
                            expandedWidth = 80,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damper1Size = viewModel.damper1SizesList[selectedIndex].toDouble()
                            },
                            defaultSelection = viewModel.damper1SizesList.indexOf(viewModel.viewState.damper1Size.toInt().toString()),
                            spacerLimit = 20,
                            heightValue = 268
                        )

                        Spacer(modifier = Modifier.width(47.dp))

                        DropDownWithLabel(
                            label = "Shape",
                            list = viewModel.damper1ShapesList,
                            previewWidth = 150,
                            expandedWidth = 170,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damper1Shape = selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damper1Shape.toInt(),
                            spacerLimit = 26,
                            heightValue = 167
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(78.dp))
                    DropDownWithLabel(
                        label = "Damper2 Type",
                        list = viewModel.damper2TypesList,
                        previewWidth = 233,
                        expandedWidth = 253,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.damper2Type = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.damper2Type.toInt(),
                        spacerLimit = 105,
                        heightValue = 272,
                        disabledIndices = if (viewModel.viewState.reheatType.toInt() == 1 ||
                            viewModel.viewState.reheatType.toInt() == 2 ||
                            viewModel.viewState.reheatType.toInt() == 3 ||
                            viewModel.viewState.reheatType.toInt() == 4 ||
                            viewModel.viewState.reheatType.toInt() == 5
                        ) {
                            listOf(0, 1, 2, 3, 5)
                        } else {
                            listOf()
                        }
                    )
                    Spacer(modifier = Modifier.width(80.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        DropDownWithLabel(
                            label = "Size",
                            list = viewModel.damper2SizesList,
                            previewWidth = 60,
                            expandedWidth = 80,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damper2Size = viewModel.damper2SizesList[selectedIndex].toDouble()
                            },
                            defaultSelection = viewModel.damper2SizesList.indexOf(viewModel.viewState.damper2Size.toInt().toString()),
                            spacerLimit = 20,
                            heightValue = 268
                        )

                        Spacer(modifier = Modifier.width(47.dp))

                        DropDownWithLabel(
                            label = "Shape",
                            list = viewModel.damper2ShapesList,
                            previewWidth = 150,
                            expandedWidth = 170,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damper2Shape = selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damper2Shape.toInt(),
                            spacerLimit = 26,
                            heightValue = 167
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(78.dp))
                    DropDownWithLabel(
                        label = "Use Reheat",
                        list = viewModel.reheatTypesList,
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 210,
                        heightValue = 268,
                        disabledIndices = if (viewModel.viewState.damper2Type.toInt() == 4) {
                            listOf()
                        } else {
                            listOf(1, 2, 3, 4, 5)
                        }
                    )
                    Spacer(modifier = Modifier.width(81.dp))
                    DropDownWithLabel(
                        label = "Zone Priority",
                        list = viewModel.zonePrioritiesList,
                        previewWidth = 150,
                        expandedWidth = 170,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.zonePriority = selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.zonePriority.toInt(),
                        spacerLimit = 135,
                        heightValue = 211
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(78.dp))
                    Row {
                        HeaderTextView(text = "Enable IAQ Control", padding = 10)
                        Spacer(modifier = Modifier.width(248.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableIAQControl,
                            onEnabled = { viewModel.viewState.enableIAQControl = it }
                        )
                    }
                    Spacer(modifier = Modifier.width(83.dp))
                    Row {
                        HeaderTextView(text = "Enable CO2 Control", padding = 10)
                        Spacer(modifier = Modifier.width(178.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableCo2Control,
                            onEnabled = { viewModel.viewState.enableCo2Control = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(78.dp))
                    Row {
                        HeaderTextView(text = "Auto Force Occupied", padding = 10)
                        Spacer(modifier = Modifier.width(230.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.autoForceOccupied,
                            onEnabled = { viewModel.viewState.autoForceOccupied = it }
                        )
                    }
                    Spacer(modifier = Modifier.width(83.dp))
                    Row {
                        HeaderTextView(text = "Auto Away", padding = 10)
                        Spacer(modifier = Modifier.width(275.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.autoAway,
                            onEnabled = { viewModel.viewState.autoAway = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(78.dp))
                    Row {
                        HeaderTextView(text = "Enable CFM", padding = 10)
                        Spacer(modifier = Modifier.width(330.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableCFMControl,
                            onEnabled = { viewModel.viewState.enableCFMControl = it }
                        )
                    }
                    Spacer(modifier = Modifier.width(85.dp))
                    Row {
                        if (viewModel.viewState.enableCFMControl) {
                            DropDownWithLabel(
                                label = "K-Factor",
                                list = viewModel.kFactorsList,
                                previewWidth = 150,
                                expandedWidth = 170,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.kFactor =
                                        viewModel.kFactorsList[selectedIndex].toDouble()
                                },
                                defaultSelection = viewModel.kFactorsList.indexOf(
                                    ("%.2f").format(
                                        viewModel.viewState.kFactor
                                    )
                                ),
                                paddingLimit = 10,
                                spacerLimit = 185,
                                heightValue = 272
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
                val valuesPickerState = rememberPickerState()
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(PaddingValues(start = 80.dp, end = 80.dp))
                ) {

                    if (viewModel.viewState.reheatType.toInt() != 0) {
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Min Reheat \nDamper Pos",
                            state = valuesPickerState,
                            items = viewModel.minReheatDamperPosList,
                            onChanged = { it: String ->
                                viewModel.viewState.minReheatDamperPos = it.toDouble()
                            },
                            startIndex = viewModel.minReheatDamperPosList.indexOf(
                                viewModel.viewState.minReheatDamperPos.toInt().toString()
                            ),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    if (viewModel.viewState.enableCFMControl) {
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Min CFM For\n    IAQ",
                            state = valuesPickerState,
                            items = viewModel.minCfmIaqList,
                            onChanged = { it: String ->
                                viewModel.viewState.minCFMForIAQ = it.toDouble()
                            },
                            startIndex = viewModel.minCfmIaqList.indexOf(
                                viewModel.viewState.minCFMForIAQ.toInt().toString()
                            ),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Max Damper Pos\n    Cooling",
                        state = valuesPickerState,
                        items = viewModel.maxCoolingDamperPosList,
                        onChanged = { it: String ->
                            viewModel.viewState.maxCoolingDamperPos = it.toDouble()
                        },
                        startIndex = viewModel.maxCoolingDamperPosList.indexOf(
                            viewModel.viewState.maxCoolingDamperPos.toInt().toString()
                        ),
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Min Damper Pos\n    Cooling",
                        state = valuesPickerState,
                        items = viewModel.minCoolingDamperPosList,
                        onChanged = { it: String ->
                            viewModel.viewState.minCoolingDamperPos = it.toDouble()
                        },
                        startIndex = viewModel.minCoolingDamperPosList.indexOf(
                            viewModel.viewState.minCoolingDamperPos.toInt().toString()
                        ),
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Max Damper Pos Heating",
                        state = valuesPickerState,
                        items = viewModel.maxHeatingDamperPosList,
                        onChanged = { it: String ->
                            viewModel.viewState.maxHeatingDamperPos = it.toDouble()
                        },
                        startIndex = viewModel.maxHeatingDamperPosList.indexOf(
                            viewModel.viewState.maxHeatingDamperPos.toInt().toString()
                        ),
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Min Damper Pos\n    Heating",
                        state = valuesPickerState,
                        items = viewModel.minHeatingDamperPosList,
                        onChanged = { it: String ->
                            viewModel.viewState.minHeatingDamperPos = it.toDouble()
                        },
                        startIndex = viewModel.minHeatingDamperPosList.indexOf(
                            viewModel.viewState.minHeatingDamperPos.toInt().toString()
                        ),
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                }
                val mapOfUnUsedPorts = viewModel.viewState.unusedPortState
                if(mapOfUnUsedPorts.isNotEmpty()) {
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
        this@DabProfileConfigFragment.closeAllBaseDialogFragments()
    }
}