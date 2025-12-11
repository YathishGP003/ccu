package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.*
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VavProfileConfigFragment : BaseDialogFragment(), OnPairingCompleteListener {

    private val viewModel : VavProfileViewModel by viewModels()
    companion object {
        val ID: String = VavProfileConfigFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): VavProfileConfigFragment {
            val fragment = VavProfileConfigFragment()
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
            viewModel.setOnPairingCompleteListener(this@VavProfileConfigFragment)
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
            Text(text = stringResource(R.string.loading_profile_configuration))
        }
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
                        when (viewModel.profileType) {
                            ProfileType.VAV_SERIES_FAN -> TitleTextView(stringResource(R.string.vav_reheat_series))
                            ProfileType.VAV_PARALLEL_FAN -> TitleTextView(stringResource(R.string.vav_reheat_parallel))
                            else -> TitleTextView(stringResource(R.string.vav_no_fan))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(78.dp))
                        DropDownWithLabel(
                            label = stringResource(id = R.string.damper_type),
                            list = viewModel.damperTypesList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damperType = selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperType.toInt(),
                            spacerLimit = 178,
                            heightValue = 272
                        )
                        Spacer(modifier = Modifier.width(85.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            DropDownWithLabel(
                                label = "Size",
                                list = viewModel.damperSizesList,
                                previewWidth = 60,
                                expandedWidth = 90,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.damperSize = selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.damperSize.toInt(),
                                spacerLimit = 20,
                                heightValue = 268
                            )

                            Spacer(modifier = Modifier.width(42.dp))

                            DropDownWithLabel(
                                label = stringResource(R.string.shape),
                                list = viewModel.damperShapesList,
                                previewWidth = 135,
                                expandedWidth = 155,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.damperShape = selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.damperShape.toInt(),
                                spacerLimit = 26,
                                heightValue = 167
                            )


                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(78.dp))
                        DropDownWithLabel(
                            label = stringResource(R.string.label_reheattype),
                            list = viewModel.reheatTypesList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.reheatType = selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.reheatType.toInt(),
                            spacerLimit = 188,
                            heightValue = 268
                        )
                        Spacer(modifier = Modifier.width(85.dp))
                        DropDownWithLabel(
                            label = stringResource(R.string.label_zonepriority),
                            list = viewModel.zonePrioritiesList,
                            previewWidth = 130,
                            expandedWidth = 150,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.zonePriority = selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.zonePriority.toInt(),
                            spacerLimit = 136,
                            heightValue = 211
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(78.dp))
                        Row {
                            HeaderTextView(text = if (viewModel.nodeType.equals(NodeType.SMART_NODE)) stringResource(R.string.vav_thermistor_1_abbreviated) else stringResource(R.string.vav_label_thermistor1), padding = 0)
                            if(viewModel.nodeType.equals(NodeType.SMART_NODE)) Spacer(modifier = Modifier.width(210.dp))
                            else Spacer(modifier = Modifier.width(160.dp))
                            LabelTextView(text = stringResource(R.string.vav_label_thermistor1_val))
                        }
                        Spacer(modifier = Modifier.width(63.dp))
                        Row {
                            HeaderTextView(text = if(viewModel.nodeType.equals(NodeType.SMART_NODE)) stringResource(R.string.vav_thermistor_2_abbreviated) else stringResource(R.string.vav_label_thermistor2), padding = 0)
                            if(viewModel.nodeType.equals(NodeType.SMART_NODE)) Spacer(modifier = Modifier.width(155.dp))
                            else Spacer(modifier = Modifier.width(115.dp))
                            LabelTextView(text = stringResource(R.string.vav_label_thermistor2_val))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(78.dp))
                        Row {
                            when (viewModel.viewState.reheatType) {
                                0.0 -> {
                                    // While reheat type is not install it should not show the Relay or Analog out ports in UI
                                }

                                1.0 -> {
                                    HeaderTextView(text = stringResource(R.string.vav_label_analog_out_2), padding = 0)
                                    Spacer(modifier = Modifier.width(147.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_modulating_reheat), widthValue = 216)
                                }

                                2.0 -> {
                                    HeaderTextView(text = stringResource(R.string.vav_label_analog_out_2), padding = 0)
                                    Spacer(modifier = Modifier.width(147.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_modulating_reheat), widthValue = 216)
                                }

                                3.0 -> {
                                    HeaderTextView(text = stringResource(R.string.vav_label_analog_out_2), padding = 0)
                                    Spacer(modifier = Modifier.width(147.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_modulating_reheat), widthValue = 216)
                                }

                                4.0 -> {
                                    HeaderTextView(text =stringResource(R.string.vav_label_analog_out_2), padding = 0)
                                    Spacer(modifier = Modifier.width(147.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_modulating_reheat), widthValue = 216)
                                }

                                5.0 -> {
                                    HeaderTextView(text = stringResource(R.string.vav_label_analog_out_2), padding = 0)
                                    Spacer(modifier = Modifier.width(147.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_modulating_reheat), widthValue = 216)
                                }

                                6.0 -> {
                                    HeaderTextView(text = stringResource(R.string.relay1), padding = 0)
                                    Spacer(modifier = Modifier.width(186.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_staged_heater), widthValue = 250)
                                }

                                7.0 -> {
                                    HeaderTextView(text = stringResource(R.string.relay1), padding = 0)
                                    Spacer(modifier = Modifier.width(186.dp))
                                    LabelTextView(text =stringResource(R.string.vav_label_staged_heater), widthValue = 250)
                                }
                            }
                        }
                        when (viewModel.viewState.reheatType) {
                            0.0 -> Spacer(modifier = Modifier.width(0.dp))
                            1.0 -> Spacer(modifier = Modifier.width(65.dp))
                            2.0 -> Spacer(modifier = Modifier.width(65.dp))
                            3.0 -> Spacer(modifier = Modifier.width(65.dp))
                            4.0 -> Spacer(modifier = Modifier.width(65.dp))
                            5.0 -> Spacer(modifier = Modifier.width(65.dp))
                            6.0 -> Spacer(modifier = Modifier.width(60.dp))
                            7.0 -> Spacer(modifier = Modifier.width(60.dp))
                        }
                        Row {
                            when (viewModel.profileType) {
                                ProfileType.VAV_SERIES_FAN -> {
                                    HeaderTextView(text = stringResource(R.string.relay2), padding = 0)
                                    Spacer(modifier = Modifier.width(222.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_series_fan))
                                }

                                ProfileType.VAV_PARALLEL_FAN -> {
                                    HeaderTextView(text = stringResource(R.string.relay2), padding = 0)
                                    Spacer(modifier = Modifier.width(208.dp))
                                    LabelTextView(text = stringResource(R.string.vav_label_parallel_fan))
                                }

                                else -> {

                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(78.dp))
                        Row {
                            HeaderTextView(
                                text = viewModel.profileConfiguration.autoForceOccupied.disName,
                                padding = 10
                            )
                            Spacer(modifier = Modifier.width(220.dp))
                            ToggleButtonStateful(
                                defaultSelection = viewModel.viewState.autoForceOccupied,
                                onEnabled = { viewModel.viewState.autoForceOccupied = it }
                            )
                        }
                        Spacer(modifier = Modifier.width(83.dp))
                        Row {
                            HeaderTextView(
                                text = viewModel.profileConfiguration.autoAway.disName,
                                padding = 10
                            )
                            Spacer(modifier = Modifier.width(250.dp))
                            ToggleButtonStateful(
                                defaultSelection = viewModel.viewState.autoAway,
                                onEnabled = { viewModel.viewState.autoAway = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(78.dp))
                        Row {
                            HeaderTextView(
                                text = viewModel.profileConfiguration.enableCo2Control.disName,
                                padding = 10
                            )
                            Spacer(modifier = Modifier.width(236.dp))
                            ToggleButtonStateful(
                                defaultSelection = viewModel.viewState.enableCo2Control,
                                onEnabled = { viewModel.viewState.enableCo2Control = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(78.dp))
                        Row {
                            HeaderTextView(
                                text = viewModel.profileConfiguration.enableCFMControl.disName,
                                padding = 10
                            )
                            Spacer(modifier = Modifier.width(228.dp))
                            ToggleButtonStateful(
                                defaultSelection = viewModel.viewState.enableCFMControl,
                                onEnabled = { viewModel.viewState.enableCFMControl = it }
                            )
                        }
                        Spacer(modifier = Modifier.width(85.dp))
                        Row {
                            if (viewModel.viewState.enableCFMControl) {
                                DropDownWithLabel(
                                    label = stringResource(R.string.k_factor),
                                    list = viewModel.kFactorsList,
                                    previewWidth = 130,
                                    expandedWidth = 150,
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
                                    spacerLimit = 180,
                                    heightValue = 272
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val valuesPickerState = rememberPickerState()

                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .padding(
                                if (viewModel.viewState.enableCFMControl) PaddingValues(
                                    start = 100.dp,
                                    end = 100.dp
                                ) else PaddingValues(start = 135.dp, end = 135.dp)
                            )
                    ) {
                        Picker(
                            header = stringResource(R.string.temperature_offset),
                            state = valuesPickerState,
                            items = viewModel.temperatureOffsetsList,
                            onChanged = { it: String ->
                                viewModel.viewState.temperatureOffset = it.toDouble()
                            },
                            startIndex = viewModel.temperatureOffsetsList.indexOf(viewModel.viewState.temperatureOffset.toString()),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )

                        if (!viewModel.viewState.enableCFMControl) {
                            Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                            Picker(
                                header = stringResource(R.string.max_damper_pos_cooling),
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
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                            Picker(
                                header = stringResource(R.string.min_damper_pos_cooling),
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
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                        } else {
                            Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                            Picker(
                                header = stringResource(R.string.max_cfm_cooling),
                                state = valuesPickerState,
                                items = viewModel.maxCFMCoolingList,
                                onChanged = { it: String ->
                                    viewModel.viewState.maxCFMCooling = it.toDouble()
                                },
                                startIndex = viewModel.maxCFMCoolingList.indexOf(
                                    viewModel.viewState.maxCFMCooling.toInt().toString()
                                ),
                                visibleItemsCount = 3,
                                modifier = Modifier.weight(0.3f),
                                textModifier = Modifier.padding(8.dp),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                            Picker(
                                header = stringResource(R.string.min_cfm_cooling),
                                state = valuesPickerState,
                                items = viewModel.minCFMCoolingList,
                                onChanged = { it: String ->
                                    viewModel.viewState.minCFMCooling = it.toDouble()
                                },
                                startIndex = viewModel.minCFMCoolingList.indexOf(
                                    viewModel.viewState.minCFMCooling.toInt().toString()
                                ),
                                visibleItemsCount = 3,
                                modifier = Modifier.weight(0.3f),
                                textModifier = Modifier.padding(8.dp),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                            Picker(
                                header = stringResource(R.string.max_cfm_reheating),
                                state = valuesPickerState,
                                items = viewModel.maxCFMReheatingList,
                                onChanged = { it: String ->
                                    viewModel.viewState.maxCFMReheating = it.toDouble()
                                },
                                startIndex = viewModel.maxCFMReheatingList.indexOf(
                                    viewModel.viewState.maxCFMReheating.toInt().toString()
                                ),
                                visibleItemsCount = 3,
                                modifier = Modifier.weight(0.3f),
                                textModifier = Modifier.padding(8.dp),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                            Picker(
                                header = stringResource(R.string.min_cfm_reheating),
                                state = valuesPickerState,
                                items = viewModel.minCFMReheatingList,
                                onChanged = { it: String ->
                                    viewModel.viewState.minCFMReheating = it.toDouble()
                                },
                                startIndex = viewModel.minCFMReheatingList.indexOf(
                                    viewModel.viewState.minCFMReheating.toInt().toString()
                                ),
                                visibleItemsCount = 3,
                                modifier = Modifier.weight(0.3f),
                                textModifier = Modifier.padding(8.dp),
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                        Picker(
                            header = stringResource(R.string.max_damper_pos_heating),
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
                        Spacer(modifier = Modifier.width(if (viewModel.viewState.enableCFMControl) 40.dp else 60.dp))
                        Picker(
                            header = stringResource(R.string.min_damper_pos_heating),
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
        this@VavProfileConfigFragment.closeAllBaseDialogFragments()
    }
}