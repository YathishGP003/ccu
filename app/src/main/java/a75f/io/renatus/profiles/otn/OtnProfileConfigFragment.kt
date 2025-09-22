package a75f.io.renatus.profiles.otn

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.TempOffsetPicker
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.ToggleButtonStateful
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.profiles.vav.VavProfileConfigFragment
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

class OtnProfileConfigFragment : BaseDialogFragment(), OnPairingCompleteListener {

    private val viewModel : OtnProfileViewModel by viewModels()
    companion object {
        val ID: String = OtnProfileConfigFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): OtnProfileConfigFragment {
            val fragment = OtnProfileConfigFragment()
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
            viewModel.setOnPairingCompleteListener(this@OtnProfileConfigFragment)
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
                        TitleTextView(stringResource(R.string.temp_influencing_caps))
                    }
                    Spacer(modifier = Modifier.height(40.dp))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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

                    Spacer(modifier = Modifier.height(40.dp))

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
                            Spacer(modifier = Modifier.width(140.dp))
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
                            Spacer(modifier = Modifier.width(160.dp))
                            ToggleButtonStateful(
                                defaultSelection = viewModel.viewState.autoAway,
                                onEnabled = { viewModel.viewState.autoAway = it }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    val valuesPickerState = rememberPickerState()
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TempOffsetPicker(
                            header = stringResource(R.string.room_temp_offset),
                            state = valuesPickerState,
                            items = viewModel.temperatureOffsetsList,
                            onChanged = { it: String ->
                                viewModel.viewState.temperatureOffset = it.toDouble()
                            },
                            startIndex = viewModel.temperatureOffsetsList.indexOf(viewModel.viewState.temperatureOffset.toString()),
                            visibleItemsCount = 3,
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
    }

    override fun getIdString(): String {
        return VavProfileConfigFragment.ID
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
        this@OtnProfileConfigFragment.closeAllBaseDialogFragments()
    }
}