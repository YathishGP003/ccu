package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.mystat.testVoltage
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatHpuViewModel
import a75f.io.renatus.profiles.mystat.viewstates.MyStatHpuViewState
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
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

    override fun getIdString() = ID

    companion object {
        val ID: String = MyStatHpuFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType,
            deviceVersion: String
        ): MyStatHpuFragment {
            val fragment = MyStatHpuFragment()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.NODE_TYPE, nodeType.ordinal)
            bundle.putString(FragmentCommonBundleArgs.DEVICE_VERSION, deviceVersion)
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
                        Title(stringResource(R.string.heat_pump_unit_caps))
                        TempOffset()
                        AutoForcedOccupiedAutoAwayConfig()
                        Label()
                        MyStatConfiguration()
                        Co2Control()
                        ThresholdTargetConfig()
                        AnalogMinMaxConfigurations()
                        DisplayInDeviceConfig(viewModel)
                        PinPasswordView(viewModel)
                        SaveConfig(viewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun AnalogMinMaxConfigurations() {
        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            CompressorSpeedMinMax()
            FanSpeedMinMax()
            FanConfiguration()
            DcvDamperMinMax()
        }
    }

    @Composable
    fun CompressorSpeedMinMax() {
        (viewModel.viewState.value as MyStatHpuViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.compressorConfig,
                MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.displayName,
                MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal,1,
                viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.compressorConfig,
                MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.displayName,
                MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal,2
            )
        }
    }

    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as MyStatHpuViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.dcvDamperConfig,
                MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.displayName,
                MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal,1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.dcvDamperConfig,
                MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.displayName,
                MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal,2
            )
        }
    }

    @Composable
    fun FanSpeedMinMax() {
        (viewModel.viewState.value as MyStatHpuViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.fanSpeedConfig,
                MyStatHpuAnalogOutMapping.FAN_SPEED.displayName,
                MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal,1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.fanSpeedConfig,
                MyStatHpuAnalogOutMapping.FAN_SPEED.displayName,
                MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal,2
            )
        }
    }

    @Composable
    fun FanConfiguration() {
        (viewModel.viewState.value as MyStatHpuViewState).apply {
            if (universalOut1.enabled && universalOut1.association == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal) {
                if (viewModel.isV1()) {
                    MinMaxConfiguration(
                        minLabel = getString(R.string.analog_out_fan_low),
                        maxLabel = getString(R.string.analog_out_fan_high),
                        itemList = testVoltage,
                        unit = "%",
                        minDefault = analogOut1FanConfig.low.toString(),
                        maxDefault = analogOut1FanConfig.high.toString(),
                        onMinSelected = { analogOut1FanConfig.low = it.value.toInt() },
                        onMaxSelected = { analogOut1FanConfig.high = it.value.toInt() })
                }
                else {
                    MinMaxConfiguration(
                        minLabel = getString(R.string.universal_out1_Fan_low),
                        maxLabel = getString(R.string.universal_out1_Fan_high),
                        itemList = testVoltage,
                        unit = "%",
                        minDefault = analogOut1FanConfig.low.toString(),
                        maxDefault = analogOut1FanConfig.high.toString(),
                        onMinSelected = { analogOut1FanConfig.low = it.value.toInt() },
                        onMaxSelected = { analogOut1FanConfig.high = it.value.toInt() })
                }
            }
            if (universalOut2.enabled && universalOut2.association == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal) {
                MinMaxConfiguration(minLabel = getString(R.string.universal_out2_Fan_low),
                    maxLabel = getString(R.string.universal_out2_Fan_high),
                    itemList = testVoltage,
                    unit = "%",
                    minDefault = analogOut2FanConfig.low.toString(),
                    maxDefault = analogOut2FanConfig.high.toString(),
                    onMinSelected = { analogOut2FanConfig.low = it.value.toInt() },
                    onMaxSelected = { analogOut2FanConfig.high = it.value.toInt() })
            }
        }
    }
}