package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.compose.StagedFanConfiguration
import a75f.io.renatus.compose.Title
import a75f.io.renatus.compose.singleOptionConfiguration
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
                        Title("CONVENTIONAL PACKAGE UNIT")
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
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.coolingConfig,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.COOLING.displayName,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.COOLING.ordinal
            )
        }
    }

    @Composable
    fun HeatingSpeedMinMax() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.heatingConfig,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.HEATING.displayName,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.HEATING.ordinal
            )
        }
    }


    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.dcvDamperConfig,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.DCV_DAMPER.displayName,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.DCV_DAMPER.ordinal
            )
        }
    }

    @Composable
    fun LinearFanMinMax() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.linearFanSpeedConfig,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.displayName,
                a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal
            )
        }
    }

    @Composable
    fun FanConfiguration() {
        (viewModel.viewState.value as MyStatCpuViewState).apply {
            if (analogOut1Association == a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                val cooling1Enabled = viewModel.isAnyRelayMappedToState(a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping.COOLING_STAGE_1)
                val cooling2Enabled = viewModel.isAnyRelayMappedToState(a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping.COOLING_STAGE_2)
                val heating1Enabled = viewModel.isAnyRelayMappedToState(a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping.HEATING_STAGE_1)
                val heating2Enabled = viewModel.isAnyRelayMappedToState(a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuRelayMapping.HEATING_STAGE_2)
                singleOptionConfiguration(minLabel = "Analog-out at \nFan Recirculate",
                    itemList = minMaxVoltage,
                    unit = "V",
                    minDefault = recirculateFanConfig.toString(),
                    onMinSelected = { recirculateFanConfig = it.value.toInt() })

                StagedFanConfiguration(
                    label1 = "Fan-Out during\nCooling Stage1",
                    label2 = "Fan-Out during\nCooling Stage2",
                    itemList = minMaxVoltage,
                    unit = "V",
                    minDefault = coolingStageFanConfig.stage1.toString(),
                    maxDefault = coolingStageFanConfig.stage2.toString(),
                    onlabel1Selected = { coolingStageFanConfig.stage1 = it.value.toInt() },
                    onlabel2Selected = { coolingStageFanConfig.stage2 = it.value.toInt() },
                    stage1Enabled = cooling1Enabled,
                    stage2Enabled = cooling2Enabled)

                StagedFanConfiguration(
                    label1 = "Fan-Out during\nHeating Stage1",
                    label2 = "Fan-Out during\nHeating Stage2",
                    itemList = minMaxVoltage,
                    unit = "V",
                    minDefault = heatingStageFanConfig.stage1.toString(),
                    maxDefault = heatingStageFanConfig.stage2.toString(),
                    onlabel1Selected = { heatingStageFanConfig.stage1 = it.value.toInt() },
                    onlabel2Selected = { heatingStageFanConfig.stage2 = it.value.toInt() },
                    stage1Enabled = heating1Enabled,
                    stage2Enabled = heating2Enabled)
            }

            if (analogOut1Enabled && (analogOut1Association == a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal ||
                        analogOut1Association == a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal)) {
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