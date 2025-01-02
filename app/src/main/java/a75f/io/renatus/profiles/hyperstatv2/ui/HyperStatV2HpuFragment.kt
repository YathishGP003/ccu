package a75f.io.renatus.profiles.hyperstatv2.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState
import a75f.io.renatus.profiles.hyperstatv2.util.FanSpeedConfig
import a75f.io.renatus.profiles.hyperstatv2.util.MinMaxConfig
import a75f.io.renatus.profiles.hyperstatv2.viewmodels.HpuV2ViewModel
import a75f.io.renatus.profiles.hyperstatv2.viewstates.HpuViewState
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Created by Manjunath K on 26-09-2024.
 */

class HyperStatV2HpuFragment : HyperStatFragmentV2(){

    companion object {
        const val ID = "HyperStatFragmentHpu"

        @JvmStatic
        fun newInstance(meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType): HyperStatV2HpuFragment {
            val args = Bundle()

            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString())
            args.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)

            val fragment = HyperStatV2HpuFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override val viewModel: HpuV2ViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
        }

        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@HyperStatV2HpuFragment)
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
                item { Title("HEAT PUMP UNIT") }
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

    override fun getIdString() =  ID
    /**
     * This function is used to display the Relay configurations
     * overriden because analog out has some staged configuration specific to HPU profile
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
                DrawAnalogOutput()
                DrawThermistors()
                DrawAnalogIn()
            }
        }
    }

    @Composable
    override fun DrawRelays() {

        fun getDisabledIndices(config: ConfigState): List<Int> {
            return if (viewModel.viewState.value.isAnyRelayMapped(
                    HsHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal,
                    config
                )
            ) {
                listOf(HsHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)
            } else if (viewModel.viewState.value.isAnyRelayMapped(
                    HsHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal,
                    config
                )
            ) {
                listOf(HsHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
            } else {
                emptyList()
            }
        }

        val relayEnums =
            viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.equipModel)

        viewModel.viewState.value.apply {
            repeat(6) { index ->
                val relayConfig = when (index) {
                    0 -> relay1Config
                    1 -> relay2Config
                    2 -> relay3Config
                    3 -> relay4Config
                    4 -> relay5Config
                    5 -> relay6Config
                    else -> throw IllegalArgumentException("Invalid relay index: $index")
                }

                RelayConfiguration(relayName = "Relay ${index + 1}",
                    enabled = relayConfig.enabled,
                    onEnabledChanged = {
                        relayConfig.enabled = it

                    },
                    association = relayEnums[relayConfig.association],
                    unit = "",
                    relayEnums = relayEnums,
                    onAssociationChanged = { associationIndex ->
                        relayConfig.association = associationIndex.index
                    },
                    isEnabled = relayConfig.enabled,
                    testState = false,
                    onTestActivated = { viewModel.updateTestRelay(relayConfig, it) },
                    padding = 7,
                    disabledIndices = getDisabledIndices(relayConfig)
                )
            }
        }
    }

    @Composable
    fun AnalogMinMaxConfigurations() {
        Column(modifier = Modifier.padding(start = 25.dp, top = 25.dp)) {
            CompressorMinMax()
            DcvDamperMinMax()
            FanSpeedMinMax()
            FanConfiguration()
        }
    }

    @Composable
    fun CompressorMinMax() {
        (viewModel.viewState.value as HpuViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.compressorConfig, 1, HsHpuAnalogOutMapping.COMPRESSOR_SPEED)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.compressorConfig, 2, HsHpuAnalogOutMapping.COMPRESSOR_SPEED)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.compressorConfig, 3, HsHpuAnalogOutMapping.COMPRESSOR_SPEED)
        }
    }

    @Composable
    fun FanSpeedMinMax() {
        (viewModel.viewState.value as HpuViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.fanSpeedConfig, 1, HsHpuAnalogOutMapping.FAN_SPEED)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.fanSpeedConfig, 2, HsHpuAnalogOutMapping.FAN_SPEED)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.fanSpeedConfig, 3, HsHpuAnalogOutMapping.FAN_SPEED)
        }
    }

    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as HpuViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.dcvDamperConfig, 1, HsHpuAnalogOutMapping.DCV_DAMPER)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.dcvDamperConfig, 2, HsHpuAnalogOutMapping.DCV_DAMPER)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.dcvDamperConfig, 3, HsHpuAnalogOutMapping.DCV_DAMPER)
        }
    }

    @Composable
    fun FanConfiguration() {
        @Composable
        fun ConfigFan(enabled: Boolean, association: Int, fanConfig: FanSpeedConfig, analogIndex: Int) {
            if (enabled && association == HsHpuAnalogOutMapping.FAN_SPEED.ordinal) {

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

        (viewModel.viewState.value as HpuViewState).apply {
            ConfigFan(analogOut1Enabled, analogOut1Association, analogOut1FanConfig, 1)
            ConfigFan(analogOut2Enabled, analogOut2Association, analogOut2FanConfig, 2)
            ConfigFan(analogOut3Enabled, analogOut3Association, analogOut3FanConfig, 3)
        }
    }

    @Composable
    private fun ConfigMinMax(association: Int, minMax: MinMaxConfig, analogIndex: Int, mapping: HsHpuAnalogOutMapping) {
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
        this@HyperStatV2HpuFragment.closeAllBaseDialogFragments()
    }
}