package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.mystat.configs.MyStatPipe2AnalogOutMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.DependentPointMappingView
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.mystat.testVoltage
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatPipe2ViewModel
import a75f.io.renatus.profiles.mystat.viewstates.MyStatPipe2ViewState
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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

class MyStatPipe2Fragment : MyStatFragment() {

    override val viewModel: MyStatPipe2ViewModel by viewModels()

    override fun onPairingComplete() {
        this@MyStatPipe2Fragment.closeAllBaseDialogFragments()
    }

    override fun getIdString() = ID

    companion object {
        val ID: String = MyStatPipe2Fragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): MyStatPipe2Fragment {
            val fragment = MyStatPipe2Fragment()
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
            viewModel.setOnPairingCompleteListener(this@MyStatPipe2Fragment)
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                item {
                    val isDisabled by viewModel.isDisabled.observeAsState(false)
                    if (isDisabled) {
                        PasteBannerFragment.PasteCopiedConfiguration(
                            onPaste = { viewModel.applyCopiedConfiguration(viewModel.viewState.value) },
                            onClose = { viewModel.disablePasteConfiguration() }
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(50.dp, 25.dp)) {
                        Title("2 PIPE FCU")
                        TempOffset()
                        AutoForcedOccupiedAutoAwayConfig()
                        Label()
                        Configurations()
                        Co2Control()
                        AnalogMinMaxConfigurations()
                        ThresholdTargetConfig()
                        SaveConfig(viewModel)
                    }
                }
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
                    .padding(top = 15.dp)
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


    @Composable
    fun AnalogMinMaxConfigurations() {
        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            WaterModulatingMinMax()
            FanSpeedMinMax()
            FanConfiguration()
            DcvDamperMinMax()
        }
    }

    @Composable
    fun WaterModulatingMinMax() {
        (viewModel.viewState.value as MyStatPipe2ViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.waterModulatingValue,
                MyStatPipe2AnalogOutMapping.WATER_MODULATING_VALUE
            )
        }
    }

    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as MyStatPipe2ViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.dcvDamperConfig,
                MyStatPipe2AnalogOutMapping.DCV_DAMPER_MODULATION
            )
        }
    }

    @Composable
    fun FanSpeedMinMax() {
        (viewModel.viewState.value as MyStatPipe2ViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.fanSpeedConfig,
                MyStatPipe2AnalogOutMapping.FAN_SPEED
            )
        }
    }

    @Composable
    fun FanConfiguration() {
        (viewModel.viewState.value as MyStatPipe2ViewState).apply {
            if (analogOut1Enabled && analogOut1Association == MyStatPipe2AnalogOutMapping.FAN_SPEED.ordinal) {
                MinMaxConfiguration(minLabel = "Analog-out at \nFan Low",
                    maxLabel = "Analog-out at \nFan High",
                    itemList = testVoltage,
                    unit = "%",
                    minDefault = analogOut1FanConfig.low.toString(),
                    maxDefault = analogOut1FanConfig.high.toString(),
                    onMinSelected = { analogOut1FanConfig.low = it.value.toInt() },
                    onMaxSelected = { analogOut1FanConfig.high = it.value.toInt() })
            }
        }
    }
}