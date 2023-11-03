package a75f.io.renatus.profiles.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.rememberPickerState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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

class VavProfileConfigFragment : BaseDialogFragment() {

    private val viewModel : VavProfileViewModel by viewModels()
    companion object {
        val ID: String = VavProfileConfigFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, profileType: ProfileType
        ): VavProfileConfigFragment {
            val fragment = VavProfileConfigFragment()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /*viewModel.isDialogOpen.observe(viewLifecycleOwner) { isDialogOpen ->
            if (!isDialogOpen) {
                this@VavProfileConfigFragment.closeAllBaseDialogFragments()
            }
        }*/
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
                    TitleTextView("VAV - NO FAN")
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    DropDownWithLabel("Damper Type",
                        viewModel.damperTypesList, 160, 160,
                        { selectedIndex -> viewModel.viewState.damperType = selectedIndex}, viewModel.viewState.damperType)
                    DropDownWithLabel("Damper Size",
                        viewModel.damperSizesList, 60, 120,
                        {selectedIndex -> viewModel.viewState.damperSize = selectedIndex},
                        viewModel.viewState.damperSize)
                    DropDownWithLabel("Damper Shape",
                        viewModel.damperShapesList, 100, 120,
                        {selectedIndex -> viewModel.viewState.damperShape = selectedIndex},
                        viewModel.viewState.damperShape)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    DropDownWithLabel("Reheat Type", viewModel.reheatTypesList, 160, 160,{})
                    DropDownWithLabel("Zone Priority", viewModel.zonePrioritiesList, 100,120,{})
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    Row {
                        HeaderTextView(text = "Thermistor-1")
                        Spacer(modifier = Modifier.width(60.dp))
                        LabelTextView(text = "Discharge Airflow")
                    }
                    Row {
                        HeaderTextView(text = "Thermistor-2")
                        Spacer(modifier = Modifier.width(60.dp))
                        LabelTextView(text = "Supply Airflow")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        HeaderTextView(text = viewModel.profileConfiguration.autoForceOccupied.disName)
                        Spacer(modifier = Modifier.width(10.dp))
                        ToggleButtonStateful(defaultSelection = viewModel.profileConfiguration.autoForceOccupied.enabled, onEnabled = {})
                    }
                    Row {
                        HeaderTextView(text = viewModel.profileConfiguration.autoAway.disName)
                        Spacer(modifier = Modifier.width(10.dp))
                        ToggleButtonStateful(defaultSelection = viewModel.profileConfiguration.autoAway.enabled, onEnabled = {})
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableIAQControl.disName)
                        Spacer(modifier = Modifier.width(60.dp))
                        ToggleButtonStateful(defaultSelection = viewModel.profileConfiguration.enableIAQControl.enabled, onEnabled = {})
                    }
                    Row {
                        HeaderTextView(text =  viewModel.profileConfiguration.enableCo2Control.disName)
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(defaultSelection = viewModel.profileConfiguration.enableCo2Control.enabled, onEnabled = {})
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    HeaderTextView(text =  viewModel.profileConfiguration.enableCFMControl.disName)
                    Spacer(modifier = Modifier.width(60.dp))
                    ToggleButtonStateful(defaultSelection = viewModel.profileConfiguration.enableCFMControl.enabled, onEnabled = {})
                }

                Spacer(modifier = Modifier.height(20.dp))

                val values = remember { (0..100).map { it.toString() } }
                val valuesPickerState = rememberPickerState()

                Row(modifier = Modifier.fillMaxWidth()) {
                    Picker(
                        header = "Temperature\n    Offset",
                        state = valuesPickerState,
                        items = viewModel.temperatureOffsetsList,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )


                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Max Damper Pos\n    Cooling",
                        state = valuesPickerState,
                        // TODO: update to use model once this point is added to it
                        items = values,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Min Damper Pos\n    Cooling",
                        state = valuesPickerState,
                        // TODO: update to use model once this point is added to it
                        items = values,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Max Damper Pos\n    Heating",
                        state = valuesPickerState,
                        items = viewModel.maxHeatingDamperPosList,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Min Damper Pos\n    Heating",
                        state = valuesPickerState,
                        // TODO: update to use model once this point is added to it
                        items = values,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
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