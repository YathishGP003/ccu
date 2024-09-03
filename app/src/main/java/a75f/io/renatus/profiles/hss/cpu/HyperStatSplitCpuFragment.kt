package a75f.io.renatus.profiles.hss.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.CancelDialog
import a75f.io.renatus.composables.DuplicatePointDialog
import a75f.io.renatus.composables.MissingPointDialog
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hss.HyperStatSplitFragment
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

        Column {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 50.dp, vertical = 25.dp),
            ) {
                item { Title(viewModel) }
                item { TempOffset(viewModel) }
                item { AutoAwayConfig(viewModel) }

                item { TitleLabel() }
                item { SensorConfig(viewModel) }
                item { RelayConfig(viewModel) }
                item { AnalogOutConfig(viewModel) }
                item { UniversalInConfig(viewModel) }

                item { AnalogOutDynamicConfig(viewModel) }

                item { ZoneOAOConfig(viewModel) }

                item { DisplayInDeviceConfig(viewModel) }

                item { SaveConfig(viewModel) }
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
    fun AnalogOutDynamicConfig(viewModel: HyperStatSplitCpuViewModel) {
        CoolingControl(viewModel)
        HeatingControl(viewModel)
        LinearFanControl(viewModel)
        StagedFanControl(viewModel)
        OAODamperControl(viewModel)
        ReturnDamperControl(viewModel)
    }

    override fun onPairingComplete() {
        this@HyperStatSplitCpuFragment.closeAllBaseDialogFragments()
    }

}