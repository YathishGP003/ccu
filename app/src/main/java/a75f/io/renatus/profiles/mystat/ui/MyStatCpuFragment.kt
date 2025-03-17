package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.DependentPointMappingView
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatCpuViewModel
import a75f.io.renatus.profiles.system.UNIVERSAL_IN
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 15-01-2025.
 */

class MyStatCpuFragment : MyStatFragment() {

    override val viewModel: MyStatCpuViewModel by viewModels()

    override fun onPairingComplete() {
        this@MyStatCpuFragment.closeAllBaseDialogFragments()
    }

    override fun getIdString() = ID

    companion object {
        val ID: String = MyStatCpuFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): MyStatCpuFragment {
            val fragment = MyStatCpuFragment()
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
            viewModel.setOnPairingCompleteListener(this@MyStatCpuFragment)
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
                item { Title("Conventional Package Unit") }
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

    /**
     * This function is used to display the Relay configurations
     * overriden because analog out has some staged configuration specific to 2Pipe profile
     */
    @Composable
    fun Configurations() {
        MyStatDrawRelays()
        DrawAnalogOutput()
        UniversalInput()
    }

    @Composable
    override fun UniversalInput() {
        Row {
            Image(
                painter = painterResource(id = R.drawable.universal),
                contentDescription = "UniversalInput",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 5.dp)
                    .height(34.dp)
            )
            Column(modifier = Modifier.weight(4f)) {
                DependentPointMappingView(
                    toggleName = UNIVERSAL_IN,
                    toggleState = true,
                    toggleEnabled = { viewModel.viewState.value.universalIn1.enabled = true },
                    mappingText = "Supply Water Temperature",
                    false
                )
            }
        }
    }


}