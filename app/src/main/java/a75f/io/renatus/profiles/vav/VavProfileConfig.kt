package a75f.io.renatus.profiles.vav

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels

class VavProfileConfig : BaseDialogFragment() {

    private val viewModel : VavProfileViewModel by viewModels()
    companion object {
        val ID: String = VavProfileConfig::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, profileType: ProfileType
        ): VavProfileConfig {
            val fragment = VavProfileConfig()
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
        viewModel.init(requireArguments())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        /*viewModel.isDialogOpen.observe(viewLifecycleOwner) { isDialogOpen ->
            if (!isDialogOpen) {
                this@VavProfileConfig.closeAllBaseDialogFragments()
            }
        }*/
    }

    @Preview
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
                    DropDownWithLabel("Damper1Type", viewModel.damperTypes, 100, 120)
                    DropDownWithLabel("Size", viewModel.damperSizes, 60, 120)
                    DropDownWithLabel("Shape", viewModel.damperShape, 100, 120 )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween){
                    DropDownWithLabel("ReheatType", viewModel.reheatTypes, 160, 160)
                    DropDownWithLabel("Zone Priority", viewModel.zonePriority, 100,120)
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
                        HeaderTextView(text = "Auto Force Occupied")
                        Spacer(modifier = Modifier.width(10.dp))
                        ToggleButtonStateful(defaultSelection = false, onEnabled = {})
                    }
                    Row {
                        HeaderTextView(text = "Auto Away")
                        Spacer(modifier = Modifier.width(10.dp))
                        ToggleButtonStateful(defaultSelection = false, onEnabled = {})
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        HeaderTextView(text = "IAQ Control")
                        Spacer(modifier = Modifier.width(60.dp))
                        ToggleButtonStateful(defaultSelection = false, onEnabled = {})
                    }
                    Row {
                        HeaderTextView(text = "CO2 Control")
                        Spacer(modifier = Modifier.width(20.dp))
                        ToggleButtonStateful(defaultSelection = false, onEnabled = {})
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row {
                    HeaderTextView(text = "Enable CFM")
                    Spacer(modifier = Modifier.width(60.dp))
                    ToggleButtonStateful(defaultSelection = false, onEnabled = {})
                }

                Spacer(modifier = Modifier.height(20.dp))

                val values = remember { (0..100).map { it.toString() } }
                val valuesPickerState = rememberPickerState()

                Row(modifier = Modifier.fillMaxWidth()) {
                    Picker(
                        header = "Temperature\n    Offset",
                        state = valuesPickerState,
                        items = values,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )


                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Max Damper Pos\n    Cooling",
                        state = valuesPickerState,
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
                        items = values,
                        visibleItemsCount = 3,
                        modifier = Modifier.weight(0.3f),
                        textModifier = Modifier.padding(8.dp),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(60.dp))
                    Picker(
                        header = "Min Damper Pos\n    Heating",
                        state = valuesPickerState,
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
                        //viewModel.saveConfiguration()
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