package a75f.io.renatus.profiles.mystat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuAnalogOutMapping
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatHpuRelayMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState
import a75f.io.renatus.profiles.mystat.getAllowedValues
import a75f.io.renatus.profiles.mystat.testVoltage
import a75f.io.renatus.profiles.mystat.viewmodels.MyStatHpuViewModel
import a75f.io.renatus.profiles.mystat.viewstates.MyStatHpuViewState
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
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
                        Title("HEAT PUMP UNIT")
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


    @Composable
    fun Configurations() {
        MyStatDrawRelays()
        DrawAnalogOutput()
        UniversalInput()
    }


    @Composable
    override fun MyStatDrawRelays() {
        fun getDisabledIndices(config: ConfigState): List<Int> {
            return if (viewModel.viewState.value.isAnyRelayMapped(
                    MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal,
                    config)) {
                listOf(MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal)
            } else if (viewModel.viewState.value.isAnyRelayMapped(
                    MyStatHpuRelayMapping.CHANGE_OVER_O_COOLING.ordinal,
                    config)) {
                listOf(MyStatHpuRelayMapping.CHANGE_OVER_B_HEATING.ordinal)
            } else { emptyList() }
        }

        Row {
            Image(
                painter = painterResource(id = R.drawable.ms_relays),
                contentDescription = "Relays",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 8.dp)
                    .height(240.dp)
            )

            val relayEnums =
                getAllowedValues(DomainName.relay1OutputAssociation, viewModel.equipModel)

            Column(modifier = Modifier.weight(4f)) {

                viewModel.viewState.value.apply {
                    repeat(4) { index ->
                        val relayConfig = when (index) {
                            0 -> relay1Config
                            1 -> relay2Config
                            2 -> relay3Config
                            3 -> relay4Config
                            else -> throw IllegalArgumentException("Invalid relay index: $index")
                        }
                        val padding = (index + 1 ) + 2
                        RelayConfiguration(
                            relayName = "Relay ${index + 1}",
                            enabled = relayConfig.enabled,
                            onEnabledChanged = { relayConfig.enabled = it },
                            association = relayEnums[relayConfig.association],
                            unit = "",
                            relayEnums = relayEnums,
                            onAssociationChanged = { associationIndex ->
                                relayConfig.association = associationIndex.index
                            },
                            isEnabled = relayConfig.enabled,
                            testState = viewModel.getRelayStatus(index + 1),
                            onTestActivated = {
                                viewModel.sendTestSignal(index + 1, if (it) 1.0 else 0.0)
                            },
                            padding = (4 + padding),
                            disabledIndices = getDisabledIndices(relayConfig)
                        )
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
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.compressorConfig,
                MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.displayName,
                MyStatHpuAnalogOutMapping.COMPRESSOR_SPEED.ordinal
            )
        }
    }

    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as MyStatHpuViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.dcvDamperConfig,
                MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.displayName,
                MyStatHpuAnalogOutMapping.DCV_DAMPER_MODULATION.ordinal
            )
        }
    }

    @Composable
    fun FanSpeedMinMax() {
        (viewModel.viewState.value as MyStatHpuViewState).apply {
            if (analogOut1Enabled) ConfigMinMax(
                analogOut1Association,
                analogOut1MinMax.fanSpeedConfig,
                MyStatHpuAnalogOutMapping.FAN_SPEED.displayName,
                MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal
            )
        }
    }

    @Composable
    fun FanConfiguration() {
        (viewModel.viewState.value as MyStatHpuViewState).apply {
            if (analogOut1Enabled && analogOut1Association == MyStatHpuAnalogOutMapping.FAN_SPEED.ordinal) {
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