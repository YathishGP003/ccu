package a75f.io.renatus.profiles.acb

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SET
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
import androidx.compose.runtime.remember
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

class AcbProfileConfigFragment : BaseDialogFragment() {

    private val viewModel : AcbProfileViewModel by viewModels()
    companion object {
        val ID: String = AcbProfileConfigFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType
        ): AcbProfileConfigFragment {
            val fragment = AcbProfileConfigFragment()
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
        }
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.isDialogOpen.observe(viewLifecycleOwner) { isDialogOpen ->
            if (!isDialogOpen) {
                this@AcbProfileConfigFragment.closeAllBaseDialogFragments()
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
            CircularProgressIndicator(color = ComposeUtil.primaryColor,)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }

    //@Preview
    @Composable
    fun RootView() {
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
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TitleTextView("Active Chilled Beams + DOAS")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    DropDownWithLabel(
                        label = "Damper Type",
                        list = viewModel.damperTypesList,
                        previewWidth = 160,
                        expandedWidth = 160,
                        onSelected = { selectedIndex -> viewModel.viewState.damperType = selectedIndex.toDouble() },
                        defaultSelection = viewModel.viewState.damperType.toInt()
                    )
                    DropDownWithLabel(
                        label = "Damper Size",
                        list = viewModel.damperSizesList,
                        previewWidth = 60,
                        expandedWidth = 120,
                        onSelected = {selectedIndex -> viewModel.viewState.damperSize = selectedIndex.toDouble() },
                        defaultSelection = viewModel.viewState.damperSize.toInt()
                    )
                    DropDownWithLabel(
                        label = "Damper Shape",
                        list = viewModel.damperShapesList,
                        previewWidth = 100,
                        expandedWidth = 120,
                        onSelected = {selectedIndex -> viewModel.viewState.damperShape = selectedIndex.toDouble() },
                        defaultSelection = viewModel.viewState.damperShape.toInt()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    DropDownWithLabel(
                        label = "Valve Type",
                        list = viewModel.valveTypesList,
                        previewWidth = 160,
                        expandedWidth = 160,
                        onSelected = {selectedIndex -> viewModel.viewState.valveType = selectedIndex.toDouble()},
                        defaultSelection = viewModel.viewState.valveType.toInt()
                    )
                    DropDownWithLabel(
                        label = "Zone Priority",
                        list = viewModel.zonePrioritiesList,
                        previewWidth = 100,
                        expandedWidth = 120,
                        onSelected = {selectedIndex -> viewModel.viewState.zonePriority = selectedIndex.toDouble() },
                        defaultSelection = viewModel.viewState.zonePriority.toInt()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    Row() {
                        HeaderTextView(text = "Thermistor 1")
                        Spacer(modifier = Modifier.width(20.dp))
                        LabelTextView(text = "Discharge Airflow")
                    }
                    Row(horizontalArrangement = Arrangement.End) {
                        HeaderTextView(text = "Relay 1")
                        Spacer(modifier = Modifier.width(20.dp))
                        LabelTextView(text = "Shut-Off Valve")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth()){
                    Spacer(modifier = Modifier.width(15.dp))
                    DropDownWithLabel(
                        label = "Thermistor 2",
                        list = viewModel.condensateSensorTypesList,
                        previewWidth = 100,
                        expandedWidth = 120,
                        onSelected = {selectedIndex -> viewModel.viewState.condensateSensorType = selectedIndex > 0},
                        defaultSelection = if (viewModel.viewState.condensateSensorType) 1 else 0
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        HeaderTextView(text = viewModel.profileConfiguration.autoForceOccupied.disName)
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.autoForceOccupied,
                            onEnabled = { it -> viewModel.viewState.autoForceOccupied = it }
                        )
                    }
                    Row {
                        HeaderTextView(text = viewModel.profileConfiguration.autoAway.disName)
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.autoAway,
                            onEnabled = { it -> viewModel.viewState.autoAway = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableIAQControl.disName)
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableIAQControl,
                            onEnabled = { it -> viewModel.viewState.enableIAQControl = it }
                        )
                    }
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableCo2Control.disName)
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableCo2Control,
                            onEnabled = { it -> viewModel.viewState.enableCo2Control = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableCFMControl.disName)
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableCFMControl,
                            onEnabled = { it -> viewModel.viewState.enableCFMControl = it }
                        )
                    }
                    Row {
                        if (viewModel.viewState.enableCFMControl) {
                            DropDownWithLabel(
                                label = "K-Factor",
                                list = viewModel.kFactorsList,
                                previewWidth = 100,
                                expandedWidth = 120,
                                onSelected = { selectedIndex -> viewModel.viewState.kFactor = viewModel.kFactorsList.get(selectedIndex).toDouble() },
                                defaultSelection = viewModel.kFactorsList.indexOf(("%.2f").format(viewModel.viewState.kFactor))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                val values = remember { (0..100).map { it.toString() } }
                val valuesPickerState = rememberPickerState()

                Row(modifier = Modifier.fillMaxWidth()) {
                    Picker(
                        header = "Temperature\n    Offset",
                        state = valuesPickerState,
                        items = viewModel.temperatureOffsetsList,
                        onChanged = { it: String -> viewModel.viewState.temperatureOffset = it.toDouble() },
                        startIndex = viewModel.temperatureOffsetsList.indexOf(viewModel.viewState.temperatureOffset.toString()),
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )

                    if (!viewModel.viewState.enableCFMControl) {
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Max Damper Pos\n    Cooling",
                            state = valuesPickerState,
                            items = viewModel.maxCoolingDamperPosList,
                            onChanged = { it: String -> viewModel.viewState.maxCoolingDamperPos = it.toDouble() },
                            startIndex = viewModel.maxCoolingDamperPosList.indexOf(viewModel.viewState.maxCoolingDamperPos.toInt().toString()),
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
                            onChanged = { it: String -> viewModel.viewState.minCoolingDamperPos = it.toDouble() },
                            startIndex = viewModel.minCoolingDamperPosList.indexOf(viewModel.viewState.minCoolingDamperPos.toInt().toString()),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )

                    } else {
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Max CFM\n    Cooling",
                            state = valuesPickerState,
                            items = viewModel.maxCFMCoolingList,
                            onChanged = { it: String -> viewModel.viewState.maxCFMCooling = it.toDouble() },
                            startIndex = viewModel.maxCFMCoolingList.indexOf(viewModel.viewState.maxCFMCooling.toInt().toString()),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Min CFM\n    Cooling",
                            state = valuesPickerState,
                            items = viewModel.minCFMCoolingList,
                            onChanged = { it: String -> viewModel.viewState.minCFMCooling = it.toDouble() },
                            startIndex = viewModel.minCFMCoolingList.indexOf(viewModel.viewState.minCFMCooling.toInt().toString()),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Max CFM\n    Reheating",
                            state = valuesPickerState,
                            items = viewModel.maxCFMReheatingList,
                            onChanged = { it: String -> viewModel.viewState.maxCFMReheating = it.toDouble() },
                            startIndex = viewModel.maxCFMReheatingList.indexOf(viewModel.viewState.maxCFMReheating.toInt().toString()),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Min CFM\n    Reheating",
                            state = valuesPickerState,
                            items = viewModel.minCFMReheatingList,
                            onChanged = { it: String -> viewModel.viewState.minCFMReheating = it.toDouble() },
                            startIndex = viewModel.minCFMReheatingList.indexOf(viewModel.viewState.minCFMReheating.toInt().toString()),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Max Damper Pos\n    Heating",
                        state = valuesPickerState,
                        items = viewModel.maxHeatingDamperPosList,
                        onChanged = { it: String -> viewModel.viewState.maxHeatingDamperPos = it.toDouble() },
                        startIndex = viewModel.maxHeatingDamperPosList.indexOf(viewModel.viewState.maxHeatingDamperPos.toInt().toString()),
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )

                    if (!viewModel.viewState.enableCFMControl) {
                        Spacer(modifier = Modifier.width(60.dp))
                        Picker(
                            header = "Min Damper Pos\n    Heating",
                            state = valuesPickerState,
                            items = viewModel.minHeatingDamperPosList,
                            onChanged = { it: String -> viewModel.viewState.minHeatingDamperPos = it.toDouble() },
                            startIndex = viewModel.minHeatingDamperPosList.indexOf(viewModel.viewState.minHeatingDamperPos.toInt().toString()),
                            visibleItemsCount = 3,
                            modifier = Modifier.weight(0.3f),
                            textModifier = Modifier.padding(8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }

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