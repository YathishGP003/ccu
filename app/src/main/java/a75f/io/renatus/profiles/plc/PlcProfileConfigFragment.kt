package a75f.io.renatus.profiles.plc

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.profiles.OnPairingCompleteListener
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlcProfileConfigFragment : BaseDialogFragment(), OnPairingCompleteListener {

    //private val viewModel : VavProfileViewModel by viewModels()
    companion object {
        val ID: String = PlcProfileConfigFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): PlcProfileConfigFragment {
            val fragment = PlcProfileConfigFragment()
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
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            //viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            //viewModel.setOnPairingCompleteListener(this@PlcProfileConfigFragment)
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
            Text(text = "Loading Profile Configuration")
        }
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
                    TitleTextView("PI LOOP CONTROLLER")
                }
                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Analog-in 1 Input Sensor",
                        list = (1..10).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 110,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(85.dp))
                    DropDownWithLabel(
                        label = "Target Value",
                        list = (0..100).toList().map { (it.toFloat()/10).toString() },
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.zonePriority = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.zonePriority.toInt(),
                        spacerLimit = 180,
                        heightValue = 211
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "TH-in1 Input Sensor",
                        list = (1..10).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 160,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(85.dp))
                    DropDownWithLabel(
                        label = "Expected Error Range",
                        list = (0..100).toList().map { (it.toFloat()/10).toString() },
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.zonePriority = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.zonePriority.toInt(),
                        spacerLimit = 80,
                        heightValue = 211
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Native Sensor Input",
                        list = (1..10).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 165,
                        heightValue = 268
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
                Row {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Expected Zero Error at Midpoint"/*viewModel.profileConfiguration.autoAway.disName*/, padding = 10)
                    Spacer(modifier = Modifier.width(100.dp))
                    ToggleButtonStateful(
                        defaultSelection = false,//viewModel.viewState.autoAway,
                        onEnabled = {/* viewModel.viewState.autoAway = it*/ }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Invert Control Loop Output"/*viewModel.profileConfiguration.autoAway.disName*/, padding = 10)
                    Spacer(modifier = Modifier.width(155.dp))
                    ToggleButtonStateful(
                        defaultSelection = false,//viewModel.viewState.autoAway,
                        onEnabled = {/* viewModel.viewState.autoAway = it*/ }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Use Analog-in2 for dynamic setpoint"/*viewModel.profileConfiguration.autoAway.disName*/, padding = 10)
                    Spacer(modifier = Modifier.width(55.dp))
                    ToggleButtonStateful(
                        defaultSelection = false,//viewModel.viewState.autoAway,
                        onEnabled = {/* viewModel.viewState.autoAway = it*/ }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Analog-in2 Input Sensor",
                        list = (1..10).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 112,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(95.dp))
                    DropDownWithLabel(
                        label = "Setpoint Sensor Offset",
                        list = (0..100).toList().map { (it.toFloat()/10).toString() },
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.zonePriority = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.zonePriority.toInt(),
                        spacerLimit = 50,
                        heightValue = 211
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Analog-Out1 1 at Min Output",
                        list = (1..10).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 50,
                        heightValue = 268
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Analog-Out1 1 at Max Output",
                        list = (1..10).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 45,
                        heightValue = 268
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    HeaderTextView(text = "Relay 1"/*viewModel.profileConfiguration.autoAway.disName*/, padding = 10)
                    Spacer(modifier = Modifier.width(100.dp))
                    ToggleButtonStateful(
                        defaultSelection = false,//viewModel.viewState.autoAway,
                        onEnabled = {/* viewModel.viewState.autoAway = it*/ }
                    )

                    Spacer(modifier = Modifier.width(375.dp))
                    HeaderTextView(text = "Relay 2"/*viewModel.profileConfiguration.autoAway.disName*/, padding = 10)
                    Spacer(modifier = Modifier.width(100.dp))
                    ToggleButtonStateful(
                        defaultSelection = false,//viewModel.viewState.autoAway,
                        onEnabled = {/* viewModel.viewState.autoAway = it*/ }
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Turn ON Relay 1",
                        list = (1..100).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 200,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(85.dp))
                    DropDownWithLabel(
                        label = "Turn ON Relay 2",
                        list = (0..100).toList().map { (it.toFloat()/10).toString() },
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.zonePriority = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.zonePriority.toInt(),
                        spacerLimit = 120,
                        heightValue = 211
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                    Spacer(modifier = Modifier.width(50.dp))
                    DropDownWithLabel(
                        label = "Turn OFF Relay 1",
                        list = (1..100).toList().map { it.toString() },
                        previewWidth = 165,
                        expandedWidth = 185,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.reheatType = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.reheatType.toInt(),
                        spacerLimit = 190,
                        heightValue = 268
                    )
                    Spacer(modifier = Modifier.width(85.dp))
                    DropDownWithLabel(
                        label = "Turn OFF Relay 2",
                        list = (0..100).toList().map { (it.toFloat()/10).toString() },
                        previewWidth = 130,
                        expandedWidth = 150,
                        onSelected = { selectedIndex ->
                            //viewModel.viewState.zonePriority = selectedIndex.toDouble()
                        },
                        //defaultSelection = viewModel.viewState.zonePriority.toInt(),
                        spacerLimit = 110,
                        heightValue = 211
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
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
        return PlcProfileConfigFragment.ID
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
        this@PlcProfileConfigFragment.closeAllBaseDialogFragments()
    }
}