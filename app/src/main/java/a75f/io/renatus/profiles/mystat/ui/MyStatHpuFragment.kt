package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatHpuViewModel
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatHpuFragment : MyStatFragment() {

    override val viewModel: MyStatHpuViewModel by viewModels()

    override fun onPairingComplete() {
        this@MyStatHpuFragment.closeAllBaseDialogFragments()
    }

    override fun getIdString() = MyStatPipe2Fragment.ID

    companion object {
        val ID: String = MyStatHpuFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): MyStatHpuFragment {
            val fragment = MyStatHpuFragment()
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
            viewModel.setOnPairingCompleteListener(this@MyStatHpuFragment)
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
        Column {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 50.dp, vertical = 25.dp),
            ) {
                item { Title("2 PIPE FCU") }
                item { TempOffset() }
                item { AutoForcedOccupiedAutoAwayConfig() }
                item { Label() }
                item { Configurations() }
                //item { AnalogMinMaxConfigurations() }
                //item { ThresholdTargetConfig(viewModel) }
                item { SaveConfig(viewModel) }
            }
        }
    }

    @Composable
    fun Configurations() {
        MyStatDrawRelays()
        DrawAnalogOutput()
        UniversalInput()
    }
}