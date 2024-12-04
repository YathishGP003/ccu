package a75f.io.renatus.profiles.hyperstatv2.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsCpuRelayMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.AnalogOutConfig
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.compose.SpinnerElementOption
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.hyperstatv2.util.FanSpeedConfig
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.util.StagedConfig
import a75f.io.renatus.profiles.hyperstatv2.viewmodels.CpuV2ViewModel
import a75f.io.renatus.profiles.hyperstatv2.viewstates.CpuViewState
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HyperStatV2CpuFragment : HyperStatFragmentV2() {


    companion object {
        const val ID = "HyperStatFragmentCpu"

        @JvmStatic
        fun newInstance(meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType): HyperStatV2CpuFragment {
            val args = Bundle()

            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString())
            args.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)

            val fragment = HyperStatV2CpuFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override val viewModel: CpuV2ViewModel by viewModels()

    override fun getIdString() = ID

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
        }

        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@HyperStatV2CpuFragment)
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
                item { Title("CONVENTIONAL PACKAGE UNIT") }
                item { TempOffset() }
                item { AutoForcedOccupiedAutoAwayConfig() }
                item { Label() }
                item { Configurations() }
                item { AnalogMinMaxConfigurations() }
                item { ThresholdTargetConfig(viewModel) }
                item { DisplayInDeviceConfig(viewModel) }
                item { SaveConfig(viewModel) }
            }
        }
    }

    /**
     * This function is used to display the Relay configurations
     * overriden because analog out has some staged configuration specific to CPU profile
     */
    @Composable
    override fun Configurations() {

        Row(modifier = Modifier.fillMaxWidth()) {
            Image(painter = painterResource(id = R.drawable.input_hyperstat_cpu), contentDescription = "Relays", modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 25.dp)
                    .height(805.dp))

            Column(modifier = Modifier.weight(4f)) {
                DrawRelays()
                DrawAnalogOutput() // only Analog Out function is using overriden function
                DrawThermistors()
                DrawAnalogIn()
            }
        }
    }

    @Composable
    override fun DrawAnalogOutput() {
        val analogEnum = viewModel.getAllowedValues(DomainName.analog1OutputAssociation, viewModel.equipModel)

        val disabledIndices = if (!viewModel.isConditioningConfigExist()) listOf(HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) else emptyList()

        repeat(3) { index ->
            val analogOut = "Analog-Out${index + 1}"
            val enabled = when (index) {
                0 -> viewModel.viewState.value.analogOut1Enabled
                1 -> viewModel.viewState.value.analogOut2Enabled
                2 -> viewModel.viewState.value.analogOut3Enabled
                else -> false
            }
            val associationIndex = when (index) {
                0 -> viewModel.viewState.value.analogOut1Association
                1 -> viewModel.viewState.value.analogOut2Association
                2 -> viewModel.viewState.value.analogOut3Association
                else -> 0
            }

            AnalogOutConfig(analogOutName = analogOut, enabled = enabled, onEnabledChanged = { enabledAction ->
                when (index) {
                    0 -> viewModel.viewState.value.analogOut1Enabled = enabledAction
                    1 -> viewModel.viewState.value.analogOut2Enabled = enabledAction
                    2 -> viewModel.viewState.value.analogOut3Enabled = enabledAction
                }

            }, association = analogEnum[associationIndex], analogOutEnums = analogEnum, testSingles = viewModel.testVoltage, isEnabled = enabled,
                    onAssociationChanged = { association ->
                        when (index) {
                            0 -> viewModel.viewState.value.analogOut1Association = association.index
                            1 -> viewModel.viewState.value.analogOut2Association = association.index
                            2 -> viewModel.viewState.value.analogOut3Association = association.index
                        }

                    }, testVal = 0.0,
                    onTestSignalSelected = {
                            viewModel.updateTestAnalogOut(index+1 , it.toInt())
                        },
                    padding = 7, disabledIndices = disabledIndices)
        }
    }

    @Composable
    fun AnalogMinMaxConfigurations() {
        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            CoolingMinMax()
            HeatingMinMax()
            LinearFanSpeedMinMax()
            DcvDamperMinMax()
            StagedFanConfiguration()
            RecirculateFanConfiguration()
            FanConfiguration()
        }
    }

    @Composable
    fun StagedFanConfiguration() {

        @Composable
        fun StagedCooling(stagedConfig: StagedConfig) {

            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp)) {
                val rowModifier = Modifier
                        .width(200.dp)
                        .padding(top = 10.dp)
                if (viewModel.isAnyRelayMappedToStage(HsCpuRelayMapping.COOLING_STAGE_1)) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Fan-Out during\nCooling Stage1", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = stagedConfig.stage1.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { stagedConfig.stage1 = it.value.toInt() }, viewModel = null)
                    }
                }
                if (viewModel.isAnyRelayMappedToStage(HsCpuRelayMapping.COOLING_STAGE_2)) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Fan-Out during\nCooling Stage2", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = stagedConfig.stage2.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { stagedConfig.stage2 = it.value.toInt() }, viewModel = null)
                    }
                }
                if (viewModel.isAnyRelayMappedToStage(HsCpuRelayMapping.COOLING_STAGE_3)) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Fan-Out during\nCooling Stage3", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = stagedConfig.stage3.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { stagedConfig.stage3 = it.value.toInt() }, viewModel = null)
                    }
                }
            }
        }

        @Composable
        fun StagedHeating(stagedConfig: StagedConfig) {

            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp)) {
                val rowModifier = Modifier
                        .width(200.dp)
                        .padding(top = 10.dp)
                if (viewModel.isAnyRelayMappedToStage(HsCpuRelayMapping.HEATING_STAGE_1)) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Fan-Out during\nHeating Stage1", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = stagedConfig.stage1.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { stagedConfig.stage1 = it.value.toInt() }, viewModel = null)
                    }
                }
                if (viewModel.isAnyRelayMappedToStage(HsCpuRelayMapping.HEATING_STAGE_2)) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Fan-Out during\nHeating Stage2", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = stagedConfig.stage2.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { stagedConfig.stage2 = it.value.toInt() }, viewModel = null)
                    }
                }
                if (viewModel.isAnyRelayMappedToStage(HsCpuRelayMapping.HEATING_STAGE_3)) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Fan-Out during\nHeating Stage3", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = stagedConfig.stage3.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { stagedConfig.stage3 = it.value.toInt() }, viewModel = null)
                    }
                }
            }
        }
        (viewModel.viewState.value as CpuViewState).apply {
            if (viewModel.isAnyAnalogOutMappedToStage(HsCpuAnalogOutMapping.STAGED_FAN_SPEED)) {
                StagedCooling(coolingStageFanConfig)
                StagedHeating(heatingStageFanConfig)
            }
        }
    }

    @Composable
    fun RecirculateFanConfiguration() {

        (viewModel.viewState.value as CpuViewState).apply {
            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp)) {
                val rowModifier = Modifier
                        .width(200.dp)
                        .padding(top = 10.dp)
                if (analogOut1Enabled && analogOut1Association == HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Analog-Out1 at\nFan Recirculate", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = recirculateFanConfig.analogOut1.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { recirculateFanConfig.analogOut1 = it.value.toInt() }, viewModel = null)
                    }
                }
                if (analogOut2Enabled && analogOut2Association == HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Analog-Out2 at\nFan Recirculate", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = recirculateFanConfig.analogOut2.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { recirculateFanConfig.analogOut2 = it.value.toInt() }, viewModel = null)
                    }
                }
                if (analogOut3Enabled && analogOut3Association == HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {
                    Box(modifier = rowModifier) {
                        StyledTextView("Analog-Out3 at\nFan Recirculate", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = rowModifier) {
                        SpinnerElementOption(defaultSelection = recirculateFanConfig.analogOut3.toString(),
                                items = viewModel.minMaxVoltage,
                                unit = "V",
                                itemSelected = { recirculateFanConfig.analogOut3 = it.value.toInt() }, viewModel = null)
                    }
                }
            }
        }
    }

    @Composable
    fun LinearFanSpeedMinMax() {
        (viewModel.viewState.value as CpuViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.linearFanSpeedConfig, 1, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.linearFanSpeedConfig, 2, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.linearFanSpeedConfig, 3, HsCpuAnalogOutMapping.LINEAR_FAN_SPEED)
        }
    }

    @Composable
    fun HeatingMinMax() {
        (viewModel.viewState.value as CpuViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.heatingConfig, 1, HsCpuAnalogOutMapping.HEATING)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.heatingConfig, 2, HsCpuAnalogOutMapping.HEATING)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.heatingConfig, 3, HsCpuAnalogOutMapping.HEATING)
        }
    }

    @Composable
    fun CoolingMinMax() {
        (viewModel.viewState.value as CpuViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.coolingConfig, 1, HsCpuAnalogOutMapping.COOLING)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.coolingConfig, 2, HsCpuAnalogOutMapping.COOLING)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.coolingConfig, 3, HsCpuAnalogOutMapping.COOLING)
        }
    }

    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as CpuViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.dcvDamperConfig, 1, HsCpuAnalogOutMapping.DCV_DAMPER)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.dcvDamperConfig, 2, HsCpuAnalogOutMapping.DCV_DAMPER)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.dcvDamperConfig, 3, HsCpuAnalogOutMapping.DCV_DAMPER)
        }
    }

    @Composable
    fun FanConfiguration() {
        @Composable
        fun ConfigFan(enabled: Boolean, association: Int, fanConfig: FanSpeedConfig, analogIndex: Int) {
            if (enabled && association == HsCpuAnalogOutMapping.LINEAR_FAN_SPEED.ordinal
                    || association == HsCpuAnalogOutMapping.STAGED_FAN_SPEED.ordinal) {

                FanLowMediumHighConfigurations(
                        firstLabel = "Analog-out$analogIndex at\n Fan Low",
                        secondLabel = "Analog-out$analogIndex at\n Fan Medium",
                        thirdLabel = "Analog-out$analogIndex at\n Fan High",
                        firstDefault = fanConfig.low.toString(),
                        secondDefault = fanConfig.medium.toString(),
                        thirdDefault = fanConfig.high.toString(),
                        onFirstSelected = { fanConfig.low = it.value.toInt() },
                        onSecondSelected = { fanConfig.medium = it.value.toInt() },
                        onThirdSelected = { fanConfig.high = it.value.toInt() },
                        itemList = viewModel.testVoltage,
                        unit = "%"
                )
            }
        }

        (viewModel.viewState.value as CpuViewState).apply {
            ConfigFan(analogOut1Enabled, analogOut1Association, analogOut1FanConfig, 1)
            ConfigFan(analogOut2Enabled, analogOut2Association, analogOut2FanConfig, 2)
            ConfigFan(analogOut3Enabled, analogOut3Association, analogOut3FanConfig, 3)
        }
    }

    @Composable
    private fun ConfigMinMax(association: Int, minMax: MinMaxConfig, analogIndex: Int, mapping: HsCpuAnalogOutMapping) {
        if (association == mapping.ordinal) {
            MinMaxConfiguration(
                    minLabel = "Analog-out$analogIndex at Min \n${mapping.displayName}",
                    maxLabel = "Analog-out$analogIndex at Max \n${mapping.displayName}",
                    itemList = viewModel.minMaxVoltage,
                    unit = "V",
                    minDefault = minMax.min.toString(),
                    maxDefault = minMax.max.toString(),
                    onMinSelected = { minMax.min = it.value.toInt() },
                    onMaxSelected = { minMax.max = it.value.toInt() }
            )
        }
    }

    override fun onPairingComplete() {
        this@HyperStatV2CpuFragment.closeAllBaseDialogFragments()
    }
}