package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4AnalogOutMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.mystat.testVoltage
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatPipe4ViewModel
import a75f.io.renatus.profiles.mystat.viewstates.MyStatPipe4ViewState
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
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyStatPipe4Fragment : MyStatFragment(),OnPairingCompleteListener {
    override fun getIdString(): String = ID
    override val viewModel: MyStatPipe4ViewModel by viewModels()
    override fun onPairingComplete() {
        this.closeAllBaseDialogFragments()
    }

    companion object {
        val ID: String = MyStatPipe4Fragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType,
            deviceType:String
        ): MyStatPipe4Fragment {
            val fragment = MyStatPipe4Fragment()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.NODE_TYPE, nodeType.ordinal)
            bundle.putString(FragmentCommonBundleArgs.DEVICE_VERSION,deviceType)
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
            viewModel.setOnPairingCompleteListener(this@MyStatPipe4Fragment)
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
                        Title(getString(R.string.title_4pipe))
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
            ModulatingMinMax()
            FanSpeedMinMax()
            FanConfiguration()
            DcvDamperMinMax()
        }
    }

    @Composable
    fun ModulatingMinMax() {
        (viewModel.viewState.value as MyStatPipe4ViewState).apply {
            if (universalOut1.enabled) {
                ConfigMinMax(
                    universalOut1.association,
                    analogOut1MinMax.hotWaterValve,
                    MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE.displayName,
                    MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE.ordinal, 1,
                    isAnalogType = viewModel.isV1()
                )
                ConfigMinMax(
                    universalOut1.association,
                    analogOut1MinMax.chilledWaterValve,
                    MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.displayName,
                    MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.ordinal, 1,
                    isAnalogType = viewModel.isV1()
                )
            }
            if (universalOut2.enabled) {
                ConfigMinMax(
                    universalOut2.association,
                    analogOut2MinMax.chilledWaterValve,
                    MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.displayName,
                    MyStatPipe4AnalogOutMapping.CHILLED_MODULATING_VALUE.ordinal, 2,
                    isAnalogType = viewModel.isV1()
                )
                ConfigMinMax(
                    universalOut2.association,
                    analogOut2MinMax.hotWaterValve,
                    MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE.displayName,
                    MyStatPipe4AnalogOutMapping.HOT_MODULATING_VALUE.ordinal, 2,
                    isAnalogType = viewModel.isV1()
                )
            }
        }
    }

    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as MyStatPipe4ViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.dcvDamperConfig,
                MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.displayName,
                MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal, 1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.dcvDamperConfig,
                MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.displayName,
                MyStatPipe4AnalogOutMapping.DCV_DAMPER_MODULATION.ordinal, 2,
                isAnalogType = viewModel.isV1()
            )
        }
    }

    @Composable
    fun FanSpeedMinMax() {
        (viewModel.viewState.value as MyStatPipe4ViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.fanSpeedConfig,
                MyStatPipe4AnalogOutMapping.FAN_SPEED.displayName,
                MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal, 1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.fanSpeedConfig,
                MyStatPipe4AnalogOutMapping.FAN_SPEED.displayName,
                MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal, 2,
                isAnalogType = viewModel.isV1()
            )
        }
    }

    @Composable
    fun FanConfiguration() {
        (viewModel.viewState.value as MyStatPipe4ViewState).apply {
            if (universalOut1.enabled && universalOut1.association == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal) {
                if (!viewModel.isV1()) {
                    MinMaxConfiguration(
                        minLabel = getString(R.string.universal_out1_Fan_Low),
                        maxLabel = getString(R.string.universal_out1_Fan_high),
                        itemList = testVoltage,
                        unit = "%",
                        minDefault = analogOut1FanConfig.low.toString(),
                        maxDefault = analogOut1FanConfig.high.toString(),
                        onMinSelected = { analogOut1FanConfig.low = it.value.toInt() },
                        onMaxSelected = { analogOut1FanConfig.high = it.value.toInt() })
                }
                else
                {
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
            }
            if (universalOut2.enabled && universalOut2.association == MyStatPipe4AnalogOutMapping.FAN_SPEED.ordinal) {
                MinMaxConfiguration(
                    minLabel = getString(R.string.universal_out2_Fan_Low),
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
