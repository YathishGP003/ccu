package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
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
import androidx.compose.foundation.layout.wrapContentWidth
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

class VavProfileConfigFragment : BaseDialogFragment() {

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
            CcuLog.i(L.TAG_CCU_UI, " isDialogOpen $isDialogOpen")
            if (!isDialogOpen) {
                this@VavProfileConfigFragment.closeAllBaseDialogFragments()
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
            CircularProgressIndicator(color = primaryColor,)
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
                    when (viewModel.profileType) {
                        ProfileType.VAV_SERIES_FAN -> TitleTextView("VAV REHEAT - SERIES")
                        ProfileType.VAV_PARALLEL_FAN -> TitleTextView("VAV REHEAT - PARALLEL")
                        else -> TitleTextView("VAV - NO FAN")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start){
                    Spacer(modifier=Modifier.width(78.dp))
                    DropDownWithLabel(
                        label = "Damper Type",
                        list = viewModel.damperTypesList,
                        previewWidth = 160,
                        expandedWidth = 160,
                        onSelected = { selectedIndex -> viewModel.viewState.damperType = selectedIndex.toDouble() },
                        defaultSelection = viewModel.viewState.damperType.toInt(),
                        spacerLimit = 178,
                        heightValue = 272
                    )
                    Spacer(modifier=Modifier.width(85.dp))
                    Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        DropDownWithLabel(
                            label = "Size",
                            list = viewModel.damperSizesList,
                            previewWidth = 60,
                            expandedWidth = 120,
                            onSelected = { selectedIndex ->
                                viewModel.viewState.damperSize = selectedIndex.toDouble()
                            },
                            defaultSelection = viewModel.viewState.damperSize.toInt(),
                            spacerLimit = 20,
                            heightValue = 268
                        )

                        Spacer(modifier = Modifier.width(42.dp))

                        DropDownWithLabel(
                            label = "Shape",
                            list = viewModel.damperShapesList,
                            previewWidth = 130,
                            expandedWidth = 130,
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

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start){
                    Spacer(modifier=Modifier.width(78.dp))
                    DropDownWithLabel(
                        label = "Reheat Type",
                        list = viewModel.reheatTypesList,
                        previewWidth = 160,
                        expandedWidth = 160,
                        onSelected = { selectedIndex -> viewModel.viewState.reheatType = selectedIndex.toDouble()},
                        defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 188,
                        heightValue = 268
                    )
                    Spacer(modifier=Modifier.width(85.dp))
                    DropDownWithLabel(
                        label = "Zone Priority",
                        list = viewModel.zonePrioritiesList,
                        previewWidth = 130,
                        expandedWidth = 120,
                        onSelected = {selectedIndex -> viewModel.viewState.zonePriority = selectedIndex.toDouble() },
                        defaultSelection = viewModel.viewState.zonePriority.toInt(),
                        spacerLimit = 132,
                        heightValue = 211
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start){
                    Spacer(modifier=Modifier.width(78.dp))
                    Row {
                        HeaderTextView(text = "Thermistor-1",padding=0)
                        Spacer(modifier = Modifier.width(160.dp))
                        LabelTextView(text = "Discharge Airflow")
                    }
                    Spacer(modifier=Modifier.width(60.dp))
                    Row {
                        HeaderTextView(text = "Thermistor-2",padding=0)
                        Spacer(modifier = Modifier.width(110.dp))
                        LabelTextView(text = "Supply Airflow")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier=Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier=Modifier.width(78.dp))
                    Row{
                        when(viewModel.viewState.reheatType){
                            0.0->{
                                HeaderTextView(text = "Relay 1",padding=0)
                                Spacer(modifier=Modifier.width(195.dp))
                                LabelTextView(text = "Stage Electric Heater", widthValue = 250)
                            }
                            1.0->{
                                HeaderTextView(text = "Analog Out 2",padding=0)
                                Spacer(modifier=Modifier.width(147.dp))
                                LabelTextView(text = "Modulating Reheat", widthValue = 216)
                            }
                            2.0->{
                                HeaderTextView(text = "Analog Out 2",padding=0)
                                Spacer(modifier=Modifier.width(147.dp))
                                LabelTextView(text = "Modulating Reheat", widthValue = 216)
                            }
                            3.0->{
                                HeaderTextView(text = "Analog Out 2",padding=0)
                                Spacer(modifier=Modifier.width(147.dp))
                                LabelTextView(text = "Modulating Reheat", widthValue = 216)
                            }
                            4.0->{
                                HeaderTextView(text = "Analog Out 2",padding=0)
                                Spacer(modifier=Modifier.width(147.dp))
                                LabelTextView(text = "Modulating Reheat", widthValue = 216)
                            }
                            5.0->{
                                HeaderTextView(text = "Relay 1",padding=0)
                                Spacer(modifier=Modifier.width(195.dp))
                                LabelTextView(text = "Stage Electric Heater", widthValue = 250)
                            }
                            6.0->{
                                HeaderTextView(text = "Relay 1",padding=0)
                                Spacer(modifier=Modifier.width(195.dp))
                                LabelTextView(text = "Stage Electric Heater", widthValue = 250)
                            }
                            7.0->{
                                HeaderTextView(text = "Relay 1",padding=0)
                                Spacer(modifier=Modifier.width(195.dp))
                                LabelTextView(text = "Stage Electric Heater", widthValue = 250)
                            }
                        }
                    }
                    when(viewModel.viewState.reheatType)
                    {
                        0.0->Spacer(modifier=Modifier.width(42.dp))
                        1.0->Spacer(modifier=Modifier.width(60.dp))
                        2.0->Spacer(modifier=Modifier.width(60.dp))
                        3.0->Spacer(modifier=Modifier.width(60.dp))
                        4.0->Spacer(modifier=Modifier.width(60.dp))
                        5.0->Spacer(modifier=Modifier.width(42.dp))
                        6.0->Spacer(modifier=Modifier.width(42.dp))
                        7.0->Spacer(modifier=Modifier.width(42.dp))
                    }
                    Row{
                        when (viewModel.profileType){
                            ProfileType.VAV_SERIES_FAN ->{
                                HeaderTextView(text="Relay 2",padding=0)
                                Spacer(modifier=Modifier.width(218.dp))
                                LabelTextView(text = "Series Fan")
                            }
                            ProfileType.VAV_PARALLEL_FAN ->{
                                HeaderTextView(text="Relay 2",padding=0)
                                Spacer(modifier=Modifier.width(204.dp))
                                LabelTextView(text="Parallel Fan")
                            }
                            else ->{

                            }
                        }
                    }
                }

                Spacer(modifier=Modifier.height(20.dp))

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier=Modifier.width(78.dp))
                    Row {
                        HeaderTextView(text = viewModel.profileConfiguration.autoForceOccupied.disName,padding=10)
                        Spacer(modifier = Modifier.width(220.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.autoForceOccupied,
                            onEnabled = { it -> viewModel.viewState.autoForceOccupied = it }
                        )
                    }
                    Spacer(modifier=Modifier.width(83.dp))
                    Row {
                        HeaderTextView(text = viewModel.profileConfiguration.autoAway.disName, padding = 10)
                        Spacer(modifier = Modifier.width(250.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.autoAway,
                            onEnabled = { it -> viewModel.viewState.autoAway = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier=Modifier.width(78.dp))
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableIAQControl.disName,padding=10)
                        Spacer(modifier = Modifier.width(238.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableIAQControl,
                            onEnabled = { it -> viewModel.viewState.enableIAQControl = it }
                        )
                    }
                    Spacer(modifier=Modifier.width(83.dp))
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableCo2Control.disName,padding=10)
                        Spacer(modifier = Modifier.width(158.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableCo2Control,
                            onEnabled = { it -> viewModel.viewState.enableCo2Control = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier=Modifier.width(78.dp))
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableCFMControl.disName,padding=10)
                        Spacer(modifier = Modifier.width(228.dp))
                        ToggleButtonStateful(
                            defaultSelection = viewModel.viewState.enableCFMControl,
                            onEnabled = { it -> viewModel.viewState.enableCFMControl = it }
                        )
                    }
                    Spacer(modifier=Modifier.width(85.dp))
                    Row {
                        if (viewModel.viewState.enableCFMControl) {
                            DropDownWithLabel(
                                label = "K-Factor",
                                list = viewModel.kFactorsList,
                                previewWidth = 130,
                                expandedWidth = 120,
                                onSelected = { selectedIndex -> viewModel.viewState.kFactor = viewModel.kFactorsList.get(selectedIndex).toDouble() },
                                defaultSelection = viewModel.kFactorsList.indexOf(("%.2f").format(viewModel.viewState.kFactor)),
                                paddingLimit = 10,
                                spacerLimit = 173,
                                heightValue = 272
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))


                val values = remember { (0..100).map { it.toString() } }
                val valuesPickerState = rememberPickerState()

                Row(modifier = Modifier.wrapContentWidth().padding(PaddingValues(start = 135.dp, end = 135.dp))) {
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
                        header = "Max Damper Pos Heating",
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
            val width = 1265
            val height = 672
            dialog.window!!.setLayout(width, height)
        }
    }
}