package a75f.io.renatus.profiles.oao

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.BoldHeader
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextViewNew
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButtonStateful
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OAOProfileFragment : BaseDialogFragment(), OnPairingCompleteListener {

    private val viewModel: OAOViewModel by viewModels()
    val ID: String = OAOProfileFragment::class.java.simpleName


    companion object {
        val ID: String = OAOProfileFragment::class.java.simpleName
        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): OAOProfileFragment {
            val fragment = OAOProfileFragment()
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
            viewModel.setOnPairingCompleteListener(this@OAOProfileFragment)
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
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
                .padding(15.dp),
        ) {
            item {
                if (viewModel.profileConfiguration.isDefault) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TitleTextView("ECONOMIZER (OAO) (" + viewModel.profileConfiguration.nodeAddress + ")")
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BoldHeader("ECONOMIZER (OAO) (" + viewModel.profileConfiguration.nodeAddress + ")")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Outside Damper at Min Drive (V)",
                        list = viewModel.outsideDamperMinDrivePosList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.outsideDamperMinDrivePos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.outsideDamperMinDrivePosList
                            .indexOf(
                                viewModel.viewState.outsideDamperMinDrivePos.toInt().toString()
                            ),
                        spacerLimit = 64,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    DropDownWithLabel(
                        label = "Outside Damper at Max Drive (V)",
                        list = viewModel.outsideDamperMaxDrivePosList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.outsideDamperMaxDrivePos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.outsideDamperMaxDrivePosList
                            .indexOf(
                                viewModel.viewState.outsideDamperMaxDrivePos.toInt().toString()
                            ),
                        spacerLimit = 64,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Return Damper at Min Drive (V)",
                        list = viewModel.returnDamperMinDrivePosList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.returnDamperMinDrivePos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.returnDamperMinDrivePosList
                            .indexOf(
                                viewModel.viewState.returnDamperMinDrivePos.toInt().toString()
                            ),
                        spacerLimit = 76,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(65.dp))
                    DropDownWithLabel(
                        label = "Return Damper at Max Drive (V)",
                        list = viewModel.returnDamperMaxDrivePosList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.returnDamperMaxDrivePos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.returnDamperMaxDrivePosList
                            .indexOf(
                                viewModel.viewState.returnDamperMaxDrivePos.toInt().toString()
                            ),
                        spacerLimit = 74,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Outside Damper Min Open during\n" +
                                "Recirculation (%)",
                        list = viewModel.outsideDamperMinOpenDuringRecirculationList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.outsideDamperMinOpenDuringRecirculationPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.outsideDamperMinOpenDuringRecirculationList
                            .indexOf(
                                viewModel.viewState.outsideDamperMinOpenDuringRecirculationPos.toInt()
                                    .toString()
                            ),
                        spacerLimit = 58,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    DropDownWithLabel(
                        label = "Outside Damper Min Open during\n" +
                                "Conditioning (%)",
                        list = viewModel.outsideDamperMinOpenDuringConditioningList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.outsideDamperMinOpenDuringConditioningPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.outsideDamperMinOpenDuringConditioningList
                            .indexOf(
                                viewModel.viewState.outsideDamperMinOpenDuringConditioningPos.toInt()
                                    .toString()
                            ),
                        spacerLimit = 60,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Outside Damper Min Open during\n" +
                                "Fan Low (%)",
                        list = viewModel.outsideDamperMinOpenDuringFanLowList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.outsideDamperMinOpenDuringFanLowPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.outsideDamperMinOpenDuringFanLowList
                            .indexOf(
                                viewModel.viewState.outsideDamperMinOpenDuringFanLowPos.toInt()
                                    .toString()
                            ),
                        spacerLimit = 58,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    DropDownWithLabel(
                        label = "Outside Damper Min Open during\n" +
                                "Fan Medium (%)",
                        list = viewModel.outsideDamperMinOpenDuringFanMediumList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.outsideDamperMinOpenDuringFanMediumPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.outsideDamperMinOpenDuringFanMediumList
                            .indexOf(
                                viewModel.viewState.outsideDamperMinOpenDuringFanMediumPos.toInt()
                                    .toString()
                            ),
                        spacerLimit = 60,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Outside Damper Min Open during\n" +
                                "Fan High (%)",
                        list = viewModel.outsideDamperMinOpenDuringFanHighList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.outsideDamperMinOpenDuringFanHighPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.outsideDamperMinOpenDuringFanHighList
                            .indexOf(
                                viewModel.viewState.outsideDamperMinOpenDuringFanHighPos.toInt()
                                    .toString()
                            ),
                        spacerLimit = 58,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    DropDownWithLabel(
                        label = "Return Damper Min Open (%)",
                        list = viewModel.returnDamperMinOpenPosList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.returnDamperMinOpenPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.returnDamperMinOpenPosList
                            .indexOf(viewModel.viewState.returnDamperMinOpenPos.toInt().toString()),
                        spacerLimit = 107,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Exhaust Fan Stage 1 Threshold (%)",
                        list = viewModel.exhaustFanStage1ThresholdList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.exhaustFanStage1ThresholdPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.exhaustFanStage1ThresholdList
                            .indexOf(
                                viewModel.viewState.exhaustFanStage1ThresholdPos.toInt().toString()
                            ),
                        spacerLimit = 55,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(63.dp))
                    DropDownWithLabel(
                        label = "Exhaust Fan Stage 2 Threshold (%)",
                        list = viewModel.exhaustFanStage2ThresholdList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.exhaustFanStage2ThresholdPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.exhaustFanStage2ThresholdList
                            .indexOf(
                                viewModel.viewState.exhaustFanStage2ThresholdPos.toInt().toString()
                            ),
                        spacerLimit = 55,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Current Transformer Type",
                        list = viewModel.currentTransformerTypeList,
                        previewWidth = 120,
                        expandedWidth = 120,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.currentTransformerTypePos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.viewState.currentTransformerTypePos.toInt(),
                        spacerLimit = 89,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(64.dp))
                    DropDownWithLabel(
                        label = "CO2 Threshold (ppm)",
                        list = viewModel.co2ThresholdList,
                        previewWidth = 80,
                        expandedWidth = 80,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.co2ThresholdVal =
                                viewModel.co2ThresholdList[selectedIndex].toDouble()
                        },
                        defaultSelection = viewModel.co2ThresholdList
                            .indexOf(viewModel.viewState.co2ThresholdVal.toInt().toString()),
                        spacerLimit = 172,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Exhaust Fan Hysteresis (%)",
                        list = viewModel.exhaustFanHysteresisList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.exhaustFanHysteresisPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.exhaustFanHysteresisList.indexOf(
                            viewModel.viewState.exhaustFanHysteresisPos.toInt().toString()
                        ),
                        spacerLimit = 134,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(66.dp))
                    HeaderTextView(text = "Use Per Room CO2 Sensing", padding = 10)
                    Spacer(modifier = Modifier.width(154.dp))
                    ToggleButtonStateful(
                        defaultSelection = viewModel.viewState.usePerRoomCO2SensingState,
                        onEnabled = { viewModel.viewState.usePerRoomCO2SensingState = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(
                        label = "Smart Purge Outside Damper Min \nOpen",
                        list = viewModel.systemPurgeOutsideDamperMinPosList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.systemPurgeOutsideDamperMinPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.systemPurgeOutsideDamperMinPosList
                            .indexOf(
                                viewModel.viewState.systemPurgeOutsideDamperMinPos.toInt()
                                    .toString()
                            ),
                        spacerLimit = 58,
                        heightValue = 270
                    )
                    Spacer(modifier = Modifier.width(63.dp))
                    DropDownWithLabel(
                        label = "Enhanced Ventilation Outside \nDamper Min Open",
                        list = viewModel.enhancedVentilationOutsideDamperMinOpenList,
                        previewWidth = 60,
                        expandedWidth = 60,
                        onSelected = { selectedIndex ->
                            viewModel.viewState.enhancedVentilationOutsideDamperMinOpenPos =
                                selectedIndex.toDouble()
                        },
                        defaultSelection = viewModel.enhancedVentilationOutsideDamperMinOpenList
                            .indexOf(
                                viewModel.viewState.enhancedVentilationOutsideDamperMinOpenPos.toInt()
                                    .toString()
                            ),
                        spacerLimit = 98,
                        heightValue = 270
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                val mapOfUnUsedPorts = viewModel.viewState.unusedPortState
                if (mapOfUnUsedPorts.isNotEmpty()) {
                    UnusedPortsFragment.DividerRow()
                    UnusedPortsFragment.LabelUnusedPorts()
                    UnusedPortsFragment.UnUsedPortsListView(viewModel)
                }

                Spacer(modifier = Modifier.height(25.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            if (L.ccu().oaoProfile != null) {
                                SaveTextViewNew("UNPAIR") {
                                    viewModel.unpair()
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row() {
                                SaveTextViewNew("SET") {
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
        this@OAOProfileFragment.closeAllBaseDialogFragments()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1240
            val height = 680
            dialog.window!!.setLayout(width, height)
        }
    }
}