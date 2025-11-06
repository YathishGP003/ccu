package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.compose.SingleOptionConfiguration
import a75f.io.renatus.compose.StagedFanConfiguration
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.mystat.minMaxVoltage
import a75f.io.renatus.profiles.mystat.testVoltage
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatCpuViewModel
import a75f.io.renatus.profiles.mystat.viewstates.MyStatCpuViewState
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
            profileType: ProfileType,
            deviceVersion : String
        ): MyStatCpuFragment {
            val fragment = MyStatCpuFragment()
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
                        Title(stringResource(R.string.conventional_package_unit))
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
            CoolingSpeedMinMax()
            LinearFanMinMax()
            HeatingSpeedMinMax()
            FanConfiguration()
            DcvDamperMinMax()
        }
    }

    @Composable
    fun CoolingSpeedMinMax() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.coolingConfig,
                MyStatCpuAnalogOutMapping.COOLING.displayName,
                MyStatCpuAnalogOutMapping.COOLING.ordinal, 1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.coolingConfig,
                MyStatCpuAnalogOutMapping.COOLING.displayName,
                MyStatCpuAnalogOutMapping.COOLING.ordinal, 2
            )
        }
    }

    @Composable
    fun HeatingSpeedMinMax() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.heatingConfig,
                MyStatCpuAnalogOutMapping.HEATING.displayName,
                MyStatCpuAnalogOutMapping.HEATING.ordinal,1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.heatingConfig,
                MyStatCpuAnalogOutMapping.HEATING.displayName,
                MyStatCpuAnalogOutMapping.HEATING.ordinal,2
            )
        }
    }


    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.dcvDamperConfig,
                MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION.displayName,
                MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal,1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.dcvDamperConfig,
                MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION.displayName,
                MyStatCpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal,2
            )
        }
    }

    @Composable
    fun LinearFanMinMax() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (universalOut1.enabled) ConfigMinMax(
                universalOut1.association,
                analogOut1MinMax.linearFanSpeedConfig,
                MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.displayName,
                MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal,1,
                isAnalogType = viewModel.isV1()
            )
            if (universalOut2.enabled) ConfigMinMax(
                universalOut2.association,
                analogOut2MinMax.linearFanSpeedConfig,
                MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.displayName,
                MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal,2
            )
        }
    }

    @Composable
    fun FanConfiguration() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {

            val universalOut1Mapped = (universalOut1.enabled && universalOut1.association == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)
            val universalOut2Mapped = (universalOut2.enabled && universalOut2.association == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)

            if (universalOut1Mapped || universalOut2Mapped) {
                val cooling1Enabled = viewModel.isAnyRelayMappedToState(MyStatCpuRelayMapping.COOLING_STAGE_1)
                val cooling2Enabled = viewModel.isAnyRelayMappedToState(MyStatCpuRelayMapping.COOLING_STAGE_2)
                val heating1Enabled = viewModel.isAnyRelayMappedToState(MyStatCpuRelayMapping.HEATING_STAGE_1)
                val heating2Enabled = viewModel.isAnyRelayMappedToState(MyStatCpuRelayMapping.HEATING_STAGE_2)
                StagedFanConfiguration(
                    label1 = stringResource(R.string.fan_out_cooling_stage1),
                    label2 = stringResource(R.string.fan_out_cooling_stage2),
                    itemList = minMaxVoltage,
                    unit = "V",
                    minDefault = coolingStageFanConfig.stage1.toString(),
                    maxDefault = coolingStageFanConfig.stage2.toString(),
                    onlabel1Selected = { coolingStageFanConfig.stage1 = it.value.toInt() },
                    onlabel2Selected = { coolingStageFanConfig.stage2 = it.value.toInt() },
                    stage1Enabled = cooling1Enabled,
                    stage2Enabled = cooling2Enabled)

                StagedFanConfiguration(
                    label1 = stringResource(R.string.fan_out_heating_stage1),
                    label2 = stringResource(R.string.fan_out_heating_stage2),
                    itemList = minMaxVoltage,
                    unit = "V",
                    minDefault = heatingStageFanConfig.stage1.toString(),
                    maxDefault = heatingStageFanConfig.stage2.toString(),
                    onlabel1Selected = { heatingStageFanConfig.stage1 = it.value.toInt() },
                    onlabel2Selected = { heatingStageFanConfig.stage2 = it.value.toInt() },
                    stage1Enabled = heating1Enabled,
                    stage2Enabled = heating2Enabled)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                if (universalOut1Mapped) {

                    if(viewModel.isV1()){
                        SingleOptionConfiguration(minLabel = getString(R.string.analog_recirculate),
                            itemList = minMaxVoltage,
                            unit = "V",
                            minDefault = universalOut1recirculateFanConfig.toString(),
                            onMinSelected = { universalOut1recirculateFanConfig = it.value.toInt() })
                    } else {
                        SingleOptionConfiguration(minLabel = getString(R.string.universal_out1_recirculate),
                            itemList = testVoltage,
                            unit = "V",
                            minDefault = universalOut1recirculateFanConfig.toString(),
                            onMinSelected = { universalOut1recirculateFanConfig = it.value.toInt() })
                    }
                }
                if (universalOut2Mapped) {
                    SingleOptionConfiguration(minLabel = getString(R.string.universal_out2_during_economizer),
                        itemList = minMaxVoltage,
                        unit = "V",
                        minDefault = universalOut2recirculateFanConfig.toString(),
                        onMinSelected = { universalOut2recirculateFanConfig = it.value.toInt() })
                }
            }


            if (universalOut1.enabled && (universalOut1.association == MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal ||
                        universalOut1.association == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
                if (viewModel.isV1()) {
                    MinMaxConfiguration(
                        minLabel = getString(R.string.analog_fan_low),
                        maxLabel = getString(R.string.analog_fan_high),
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
            if (universalOut2.enabled && (universalOut2.association == MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal ||
                        universalOut2.association == MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
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