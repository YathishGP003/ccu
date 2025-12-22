package a75f.io.renatus.profiles.hyperstat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsHpuRelayMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.composables.ShowTestSignalBanner
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.hyperstat.viewmodels.HsHpuViewModel
import a75f.io.renatus.profiles.hyperstat.viewstates.HpuViewState
import a75f.io.renatus.profiles.mystat.minMaxVoltage
import a75f.io.renatus.profiles.mystat.testSignalVoltage
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.profiles.viewstates.ConfigState
import a75f.io.renatus.profiles.viewstates.FanSpeedConfig
import a75f.io.renatus.profiles.viewstates.MinMaxConfig
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 26-09-2024.
 */

class HyperStatHpuFragment : HyperStatFragment(){

    companion object {
        const val ID = "HyperStatFragmentHpu"

        @JvmStatic
        fun newInstance(meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType): HyperStatHpuFragment {
            val args = Bundle()

            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString())
            args.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)

            val fragment = HyperStatHpuFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override val viewModel: HsHpuViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
        }
        //reloading the UI once's paste button is clicked
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
            viewModel.setOnPairingCompleteListener(this@HyperStatHpuFragment)
            withContext(Dispatchers.Main) {
                rootView.setContent {
                    RootView()
                }
            }
        }
        return rootView
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun RootView() {
        val isDisabled by viewModel.isDisabled.observeAsState(false)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {

                stickyHeader {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .fillMaxWidth()
                    ) {
                        if (isDisabled) {
                            PasteBannerFragment.PasteCopiedConfiguration(
                                onPaste = { viewModel.applyCopiedConfiguration() },
                                onClose = { viewModel.disablePasteConfiguration() }
                            )
                        }
                        if (viewModel.testSignal) {
                            ShowTestSignalBanner()
                        }
                    }
                }
                item { Title(stringResource(R.string.hpu_caps), Modifier.padding(50.dp, 25.dp)) }
                item { TempOffset(Modifier.padding(50.dp, 0.dp)) }
                item { AutoForcedOccupiedAutoAwayConfig(Modifier.padding(50.dp, 0.dp)) }
                item { Label(Modifier.padding(50.dp, 0.dp)) }
                item { Configurations(Modifier.padding(50.dp, 0.dp)) }
                item { AnalogMinMaxConfigurations(Modifier.padding(50.dp, 0.dp)) }
                item { ThresholdTargetConfig(viewModel, Modifier.padding(50.dp, 0.dp)) }
                item { DividerRow() }
                item { DisplayInDeviceConfig(viewModel, Modifier.padding(50.dp, 0.dp)) }
                item { DividerRow() }
                item { PinPasswordView(viewModel) }
                item { DividerRow() }
                item { MisSettingConfig(viewModel) }
                item { SaveConfig(viewModel, Modifier.padding(50.dp, 25.dp)) }
            }
        }
    }

    override fun getIdString() =  ID
    /**
     * This function is used to display the Relay configurations
     * overriden because analog out has some staged configuration specific to HPU profile
     */
    @Composable
    override fun Configurations(modifier:Modifier) {

        Row(modifier = modifier.fillMaxWidth()) {
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
                    testState = viewModel.getRelayStatus(index + 1),
                    onTestActivated = { viewModel.sendTestSignal(
                        index + 1,
                        -1,
                        if (it) 1.0 else 0.0
                    ) },
                    padding = 7,
                    disabledIndices = getDisabledIndices(relayConfig)
                )
            }
        }
    }

    @Composable
    fun AnalogMinMaxConfigurations(modifier: Modifier = Modifier) {
        Column(modifier = modifier.padding(start = 25.dp, top = 25.dp)) {
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
                    itemList = testSignalVoltage,
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
                itemList = minMaxVoltage,
                unit = "V",
                minDefault = minMax.min.toString(),
                maxDefault = minMax.max.toString(),
                onMinSelected = { minMax.min = it.value.toInt() },
                onMaxSelected = { minMax.max = it.value.toInt() }
            )
        }
    }

    override fun onPairingComplete() {
        this@HyperStatHpuFragment.closeAllBaseDialogFragments()
    }
}