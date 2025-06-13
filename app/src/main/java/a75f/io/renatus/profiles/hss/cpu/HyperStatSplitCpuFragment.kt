package a75f.io.renatus.profiles.hss.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.CancelDialog
import a75f.io.renatus.composables.DuplicatePointDialog
import a75f.io.renatus.composables.ErrorDialog
import a75f.io.renatus.composables.MissingPointDialog
import a75f.io.renatus.composables.NO_COMPRESSOR
import a75f.io.renatus.composables.NO_OB_REALLY
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hss.HyperStatSplitFragment
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HyperStatSplitCpuFragment : HyperStatSplitFragment(), OnPairingCompleteListener {

    private val viewModel: HyperStatSplitCpuViewModel by viewModels()

    companion object {
        val ID: String = HyperStatSplitCpuFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): HyperStatSplitCpuFragment {
            val fragment = HyperStatSplitCpuFragment()
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
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
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
            viewModel.setOnPairingCompleteListener(this@HyperStatSplitCpuFragment)
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
        if (viewModel.openCancelDialog) {
            CancelDialog(
                onDismissRequest = { viewModel.openCancelDialog = false },
                onConfirmation = { viewModel.cancelConfirm() },
                dialogTitle = "Confirmation",
                dialogText = "You have unsaved changes. Are you sure you want to cancel them?"
            )
        }

        if (viewModel.openDuplicateDialog) {
            DuplicatePointDialog(
                onDismissRequest = { viewModel.openDuplicateDialog = false },
                duplicates = "Supply Air Temperature, Mixed Air Temperature Sensor, Outside Air Temperature Sensor, Current TX Sensor, Filter Sensor, Condensate Sensor, or Generic Alarm Sensor")
        }

        if (viewModel.openMissingDialog) {
            MissingPointDialog(
                onDismissRequest = { viewModel.openMissingDialog = false },
                missing = "Mixed Air Temperature Sensor, Outside Air Temperature Sensor, or OAO Damper")
        }

        if (viewModel.noOBRelay) {
            ErrorDialog(NO_OB_REALLY, onDismissRequest = { viewModel.noOBRelay = false })
        }
        if (viewModel.noCompressorStages) {
            ErrorDialog(NO_COMPRESSOR, onDismissRequest = { viewModel.noCompressorStages = false })
        }

        Column {
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
                item { Title(viewModel,Modifier.padding(50.dp,25.dp)) }
                item { TempOffset(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { AutoAwayConfig(viewModel,Modifier.padding(50.dp,0.dp)) }

                item { TitleLabel(Modifier.padding(50.dp,0.dp)) }
                item { SensorConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { RelayConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { AnalogOutConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { UniversalInConfig(viewModel,Modifier.padding(50.dp,0.dp)) }

                item { AnalogOutDynamicConfig(viewModel,Modifier.padding(50.dp,0.dp)) }

                item { ZoneOAOConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { DividerRow() }
                item { DisplayInDeviceConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { DividerRow() }
                item { MiscSettingConfig(viewModel) }
                item { SaveConfig(viewModel,Modifier.padding(50.dp,25.dp)) }
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

    @Composable
    fun AnalogOutDynamicConfig(viewModel: HyperStatSplitCpuViewModel,modifier: Modifier = Modifier) {
        CoolingControl(viewModel,modifier)
        HeatingControl(viewModel,modifier)
        CompressorControl(viewModel,modifier)
        DcvModulationControl(viewModel,modifier)
        LinearFanControl(viewModel,modifier)
        StagedFanControl(viewModel,modifier)
        OAODamperControl(viewModel,modifier)
        ReturnDamperControl(viewModel,modifier)
    }

    override fun onPairingComplete() {
        this@HyperStatSplitCpuFragment.closeAllBaseDialogFragments()
    }

}