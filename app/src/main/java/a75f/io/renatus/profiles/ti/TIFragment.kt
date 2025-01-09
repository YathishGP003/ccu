package a75f.io.renatus.profiles.ti

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.TempOffsetPicker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.modbus.util.ALERT
import a75f.io.renatus.modbus.util.OK
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.util.highPriorityDispatcher
import android.content.Context
import android.os.Bundle
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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

class TIFragment : BaseDialogFragment(), OnPairingCompleteListener {
    override fun getIdString(): String {
        return ID
    }

    val viewModel: TIViewModel by viewModels()

    companion object {
        const val ID = "TIFragment"

        @JvmStatic
        fun newInstance(meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType): TIFragment {
            val args = Bundle()

            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString())
            args.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)

            val fragment = TIFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
        }

        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@TIFragment)
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
        CcuLog.i(Domain.LOG_TAG, "RootView")

        val roomHideIndices = mutableListOf<Int>()
        val supplyHideIndices = mutableListOf<Int>()

        if (viewModel.viewState.roomTemperatureType.toInt() == 1) supplyHideIndices.add(1)
        if (viewModel.viewState.roomTemperatureType.toInt() == 2) supplyHideIndices.add(2)

        if (viewModel.viewState.supplyAirTemperatureType.toInt() == 1) roomHideIndices.add(1)
        if (viewModel.viewState.supplyAirTemperatureType.toInt() == 2) roomHideIndices.add(2)

        Column {
            LazyColumn(
                    modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 50.dp, vertical = 25.dp),
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TitleTextView("TEMPERATURE INFLUENCING")
                    }
                    Spacer(modifier = Modifier.height(50.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        DropDownWithLabel(label = "Zone Priority", list = viewModel.zonePrioritiesList, previewWidth = 130, expandedWidth = 150, onSelected = { selectedIndex ->
                            viewModel.viewState.zonePriority = selectedIndex.toDouble()
                        }, isHeader = false, defaultSelection = viewModel.viewState.zonePriority.toInt(), spacerLimit = 136, heightValue = 211)
                    }

                    Spacer(modifier = Modifier.height(50.dp))

                    val valuesPickerState = rememberPickerState()
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TempOffsetPicker(header = "ROOM TEMP OFFSET", state = valuesPickerState, items = viewModel.temperatureOffsetsList, onChanged = { it: String ->
                            viewModel.viewState.temperatureOffset = it.toDouble()
                        }, startIndex = viewModel.temperatureOffsetsList.indexOf(viewModel.viewState.temperatureOffset.toString()), visibleItemsCount = 3, textModifier = Modifier.padding(8.dp), textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal))
                    }

                    Spacer(modifier = Modifier.height(50.dp))


                    Column(Modifier.padding(start = 200.dp)) {


                        key(viewModel.viewState.roomTemperatureType) {
                            DropDownWithLabel(label = "Room Temperature", list = viewModel.roomTempList, previewWidth = 300, expandedWidth = 300, onSelected = { selectedIndex ->
                                viewModel.viewState.roomTemperatureType = selectedIndex.toDouble()
                            }, defaultSelection = viewModel.viewState.roomTemperatureType.toInt(), isHeader = false, spacerLimit = 200, heightValue = 211, disabledIndices = roomHideIndices, labelWidth = 240)
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        key(viewModel.viewState.supplyAirTemperatureType) {
                            DropDownWithLabel(label = "Supply Air Temperature", list = viewModel.supplyAirTempList, previewWidth = 300, expandedWidth = 300, onSelected = { selectedIndex ->
                                viewModel.viewState.supplyAirTemperatureType = selectedIndex.toDouble()
                            }, defaultSelection = viewModel.viewState.supplyAirTemperatureType.toInt(), isHeader = false, paddingLimit = -40, spacerLimit = 200, heightValue = 211, disabledIndices = supplyHideIndices, labelWidth = 240)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(end = 10.dp)), contentAlignment = Alignment.CenterEnd) {
                        SaveTextView(SET) {
                            viewModel.saveConfiguration()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ShowProgressBar() {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }


    override fun onPairingComplete() {
        this@TIFragment.closeAllBaseDialogFragments()
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