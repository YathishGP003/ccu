package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.SystemConfigFragment
import a75f.io.renatus.composables.CancelDialog
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.HeaderLeftAlignedTextViewNew
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextViewNew
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.VerticalDivider
import a75f.io.renatus.profiles.bypass.BypassConfigViewModel
import a75f.io.renatus.util.ProgressDialogUtils
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
import androidx.compose.foundation.layout.wrapContentWidth
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

class BypassConfigFragment : BaseDialogFragment() {

    private val viewModel : BypassConfigViewModel by viewModels()

    fun hasUnsavedChanges() : Boolean { return viewModel.hasUnsavedChanges() }

    fun tryNavigateAway(intent : Int) {
        if (!viewModel.hasUnsavedChanges()) {
            SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(intent)
        } else {
            viewModel.nextDestination = intent
            viewModel.openCancelDialog = true
        }
    }


    companion object {
        val ID: String = BypassConfigFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): BypassConfigFragment {
            val fragment = BypassConfigFragment()
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
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            }
            withContext(Dispatchers.Main) {
                viewModel.isDialogOpen.observe(viewLifecycleOwner) { isDialogOpen ->
                    CcuLog.i(L.TAG_CCU_UI, " isDialogOpen $isDialogOpen")
                    if (!isDialogOpen) {
                        this@BypassConfigFragment.closeAllBaseDialogFragments()
                    }
                }
            }
        }
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }

    }

    @Composable
    fun ShowProgressBar() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }
    @Composable
    fun RootView() {

        if (viewModel.openCancelDialog) {
            CancelDialog(
                onDismissRequest = { viewModel.openCancelDialog = false },
                onConfirmation = {
                    if (viewModel.nextDestination == 6 && L.ccu().oaoProfile != null) {
                        ProgressDialogUtils.showProgressDialog(context, "Loading OAO Profile")
                    }
                    viewModel.cancelConfirm()
                    SystemConfigFragment.SystemConfigFragmentHandler.sendEmptyMessage(viewModel.nextDestination)
                    viewModel.openCancelDialog = false
                },
                dialogTitle = "Confirmation",
                dialogText = "You have unsaved changes. Are you sure you want to change the tab?"
            )
        }
        if (!viewModel.modelLoaded) {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
            return
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            item {
                if (viewModel.profileConfiguration.isDefault) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TitleTextView("BYPASS DAMPER (" + viewModel.profileConfiguration.nodeAddress + ")")
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        HeaderLeftAlignedTextViewNew("BYPASS DAMPER (" + viewModel.profileConfiguration.nodeAddress + ")")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                if(viewModel.profileConfiguration.isDefault){
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        DropDownWithLabel(
                            label = "Pressure Sensor",
                            list = viewModel.pressureSensorTypesList,
                            previewWidth = 165,
                            expandedWidth = 190,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.pressureSensorType =
                                    selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.pressureSensorType.toInt(),
                            spacerLimit = 46,
                            heightValue = 130
                        )
                        Spacer(modifier=Modifier.width(64.dp))
                        DropDownWithLabel(
                            label = "Damper Type",
                            list = viewModel.damperTypesList,
                            previewWidth = 165,
                            expandedWidth = 175,
                            onSelected = { selectedIndex -> viewModel.viewState.damperType =
                                selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperType.toInt(),
                            spacerLimit = 95,
                            heightValue = 270
                        )
                    }
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        Box(modifier = Modifier.wrapContentWidth(), contentAlignment = Alignment.Center) {
                            HeaderTextView(text = "Type")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        DropDownWithLabel(
                            label = "Damper Min",
                            list = viewModel.damperMinPosList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damperMinPosition =
                                    selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperMinPosition.toInt(),
                            spacerLimit = 84,
                            heightValue = 270
                        )
                        Spacer(modifier=Modifier.width(64.dp))
                        DropDownWithLabel(
                            label = "Damper Max",
                            list = viewModel.damperMaxPosList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex -> viewModel.viewState.damperMaxPosition =
                                selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperMaxPosition.toInt(),
                            spacerLimit = 98,
                            heightValue = 270
                        )
                    }
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            HeaderTextView(text = "Position(%)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t      Position(%)")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    if (viewModel.viewState.pressureSensorType > 0) {
                        Row (modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                            DropDownWithLabel(
                                label = "Pressure Sensor",
                                list = viewModel.pressureSensorMinValList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.pressureSensorMinVal =
                                        selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.pressureSensorMinVal.toInt(),
                                spacerLimit = 43,
                                heightValue = 260
                            )
                            Spacer(modifier=Modifier.width(64.dp));
                            DropDownWithLabel(
                                label = "Pressure Sensor",
                                list = viewModel.pressureSensorMaxValList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex -> viewModel.viewState.pressureSensorMaxVal =
                                    selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.pressureSensorMaxVal.toInt(),
                                spacerLimit=65,
                                heightValue = 270
                            )
                        }
                        Row (modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                HeaderTextView(text = "Min Val(InH₂O)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t  Max Val(InH₂O)")
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row (modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                            DropDownWithLabel(
                                label = "Sensor Min",
                                list = viewModel.sensorMinVoltageList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.sensorMinVoltage =
                                        selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.sensorMinVoltage.toInt(),
                                spacerLimit = 97,
                                heightValue = 270
                            )
                            Spacer(modifier=Modifier.width(65.dp));
                            DropDownWithLabel(
                                label = "Sensor Max",
                                list = viewModel.sensorMaxVoltageList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex -> viewModel.viewState.sensorMaxVoltage =
                                    selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.sensorMaxVoltage.toInt(),
                                spacerLimit = 109,
                                heightValue = 270
                            )
                        }
                        Row (modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                HeaderTextView(text = "Voltage Output (V)\t\t\t\t\t\t\t\t\t\t\t\t\t  Voltage Output (V)")
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        DropDownWithLabel(
                            label = "SAT Min",
                            list = viewModel.satMinThresholdList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex -> viewModel.viewState.satMinThreshold = viewModel.satMinThresholdList.get(selectedIndex).toDouble() },
                            defaultSelection = viewModel.satMinThresholdList.indexOf(("%.0f").format(viewModel.viewState.satMinThreshold)),
                            spacerLimit = 126,
                            heightValue = 270
                        )
                        Spacer(modifier=Modifier.width(67.dp))
                        DropDownWithLabel(
                            label = "SAT Max",
                            list = viewModel.satMaxThresholdList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex -> viewModel.viewState.satMaxThreshold = viewModel.satMaxThresholdList.get(selectedIndex).toDouble() },
                            defaultSelection = viewModel.satMaxThresholdList.indexOf(("%.0f").format(viewModel.viewState.satMaxThreshold)),
                            spacerLimit = 142,
                            heightValue = 270
                        )
                    }
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            HeaderTextView(text = "Threshold(°F)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t  Threshold(°F)")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        DropDownWithLabel(
                            label = "Expected Pressure",
                            list = viewModel.expectedPressureErrorList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex -> viewModel.viewState.expectedPressureError = viewModel.expectedPressureErrorList.get(selectedIndex).toDouble() },
                            defaultSelection = viewModel.expectedPressureErrorList.indexOf(("%.1f").format(viewModel.viewState.expectedPressureError)),
                            spacerLimit = 11,
                            heightValue = 270

                        )
                        Spacer(modifier=Modifier.width(65.dp))
                        DropDownWithLabel(
                            label = "Pressure Setpoint",
                            list = viewModel.pressureSetpointsList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex -> viewModel.viewState.pressureSetpoint = viewModel.pressureSetpointsList.get(selectedIndex).toDouble() },
                            defaultSelection = viewModel.pressureSetpointsList.indexOf(("%.1f").format(viewModel.viewState.pressureSetpoint)),
                            spacerLimit = 42,
                            heightValue = 270
                        )
                    }
                    Row (modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 100.dp, end = 10.dp), horizontalArrangement = Arrangement.Start) {
                        Box(
                            modifier = Modifier.wrapContentWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            HeaderTextView(text = "Error(InH₂O)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t (InH₂O)")
                        }
                    }
                }
                else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        DropDownWithLabel(
                            label = "Pressure Sensor",
                            list = viewModel.pressureSensorTypesList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.pressureSensorType =
                                    selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.pressureSensorType.toInt(),
                            spacerLimit = 46,
                            heightValue = 130
                        )
                        Spacer(modifier = Modifier.width(64.dp))
                        DropDownWithLabel(
                            label = "Damper Type",
                            list = viewModel.damperTypesList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damperType =
                                    selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperType.toInt(),
                            spacerLimit = 95,
                            heightValue = 270
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier.wrapContentWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            HeaderTextView(text = "Type")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        DropDownWithLabel(
                            label = "Damper Min",
                            list = viewModel.damperMinPosList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damperMinPosition =
                                    selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperMinPosition.toInt(),
                            spacerLimit = 84,
                            heightValue = 270
                        )
                        Spacer(modifier = Modifier.width(64.dp))
                        DropDownWithLabel(
                            label = "Damper Max",
                            list = viewModel.damperMaxPosList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damperMaxPosition =
                                    selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperMaxPosition.toInt(),
                            spacerLimit = 98,
                            heightValue = 270
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            HeaderTextView(text = "Position(%)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t      Position(%)")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    if (viewModel.viewState.pressureSensorType > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            DropDownWithLabel(
                                label = "Pressure Sensor",
                                list = viewModel.pressureSensorMinValList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.pressureSensorMinVal =
                                        selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.pressureSensorMinVal.toInt(),
                                spacerLimit = 43,
                                heightValue = 260
                            )
                            Spacer(modifier = Modifier.width(64.dp));
                            DropDownWithLabel(
                                label = "Pressure Sensor",
                                list = viewModel.pressureSensorMaxValList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.pressureSensorMaxVal =
                                        selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.pressureSensorMaxVal.toInt(),
                                spacerLimit = 65,
                                heightValue = 270
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                HeaderTextView(text = "Min Val(InH₂O)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t  Max Val(InH₂O)")
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            DropDownWithLabel(
                                label = "Sensor Min",
                                list = viewModel.sensorMinVoltageList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.sensorMinVoltage =
                                        selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.sensorMinVoltage.toInt(),
                                spacerLimit = 97,
                                heightValue = 270
                            )
                            Spacer(modifier = Modifier.width(65.dp));
                            DropDownWithLabel(
                                label = "Sensor Max",
                                list = viewModel.sensorMaxVoltageList,
                                previewWidth = 165,
                                expandedWidth = 185,
                                onSelected = { selectedIndex ->
                                    viewModel.viewState.sensorMaxVoltage =
                                        selectedIndex.toDouble()
                                },
                                defaultSelection = viewModel.viewState.sensorMaxVoltage.toInt(),
                                spacerLimit = 109,
                                heightValue = 270
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 10.dp, end = 10.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                HeaderTextView(text = "Voltage Output (V)\t\t\t\t\t\t\t\t\t\t\t\t\t  Voltage Output (V)")
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        DropDownWithLabel(
                            label = "SAT Min",
                            list = viewModel.satMinThresholdList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.satMinThreshold =
                                    viewModel.satMinThresholdList.get(selectedIndex).toDouble()
                            },
                            defaultSelection = viewModel.satMinThresholdList.indexOf(
                                ("%.0f").format(
                                    viewModel.viewState.satMinThreshold
                                )
                            ),
                            spacerLimit = 126,
                            heightValue = 270
                        )
                        Spacer(modifier = Modifier.width(67.dp))
                        DropDownWithLabel(
                            label = "SAT Max",
                            list = viewModel.satMaxThresholdList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.satMaxThreshold =
                                    viewModel.satMaxThresholdList.get(selectedIndex).toDouble()
                            },
                            defaultSelection = viewModel.satMaxThresholdList.indexOf(
                                ("%.0f").format(
                                    viewModel.viewState.satMaxThreshold
                                )
                            ),
                            spacerLimit = 142,
                            heightValue = 270
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            HeaderTextView(text = "Threshold(°F)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t  Threshold(°F)")
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        DropDownWithLabel(
                            label = "Expected Pressure",
                            list = viewModel.expectedPressureErrorList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.expectedPressureError =
                                    viewModel.expectedPressureErrorList.get(selectedIndex)
                                        .toDouble()
                            },
                            defaultSelection = viewModel.expectedPressureErrorList.indexOf(
                                ("%.1f").format(
                                    viewModel.viewState.expectedPressureError
                                )
                            ),
                            spacerLimit = 11,
                            heightValue = 270

                        )
                        Spacer(modifier = Modifier.width(65.dp))
                        DropDownWithLabel(
                            label = "Pressure Setpoint",
                            list = viewModel.pressureSetpointsList,
                            previewWidth = 165,
                            expandedWidth = 185,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.pressureSetpoint =
                                    viewModel.pressureSetpointsList.get(selectedIndex).toDouble()
                            },
                            defaultSelection = viewModel.pressureSetpointsList.indexOf(
                                ("%.1f").format(
                                    viewModel.viewState.pressureSetpoint
                                )
                            ),
                            spacerLimit = 42,
                            heightValue = 270
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            HeaderTextView(text = "Error(InH₂O)\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t (InH₂O)")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(45.dp))
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.Start) {
                            if (L.ccu().bypassDamperProfile != null) {
                                SaveTextViewNew("UNPAIR") {
                                    viewModel.unpair()
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row() {
                                if (viewModel.hasUnsavedChanges()) {
                                    SaveTextViewNew("CANCEL") { viewModel.openCancelDialog = true }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    VerticalDivider(heightValue = 27, offsetValue = 11)
                                    Spacer(modifier=Modifier.width(12.dp))
                                }

                                SaveTextViewNew("SAVE") {
                                    viewModel.saveConfiguration()
                                }
                            }
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
            val width = 1165
            val height = 672
            dialog.window!!.setLayout(width, height)
        }
    }

}