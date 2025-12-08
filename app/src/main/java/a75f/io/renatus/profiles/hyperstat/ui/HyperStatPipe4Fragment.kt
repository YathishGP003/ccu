package a75f.io.renatus.profiles.hyperstat.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4AnalogOutMapping
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.AnalogOutConfig
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.ShowTestSignalBanner
import a75f.io.renatus.compose.Title
import a75f.io.renatus.profiles.hyperstat.viewmodels.HsPipe4ViewModel
import a75f.io.renatus.profiles.hyperstat.viewstates.Pipe4ViewState
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
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

class HyperStatPipe4Fragment : HyperStatFragment() {


    companion object {
        const val ID = "HyperStatFragmentPipe4"

        @JvmStatic
        fun newInstance(meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType, profileType: ProfileType): HyperStatPipe4Fragment {
            val args = Bundle()

            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString())
            args.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)

            val fragment = HyperStatPipe4Fragment()
            fragment.arguments = args
            return fragment
        }

    }

    override val viewModel: HsPipe4ViewModel by viewModels()

    override fun getIdString() = ID

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
            viewModel.setOnPairingCompleteListener(this@HyperStatPipe4Fragment)
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
                item { Title(stringResource(R.string.four_pipe_fcu),Modifier.padding(50.dp,25.dp)) }
                item { TempOffset(Modifier.padding(50.dp,0.dp)) }
                item { AutoForcedOccupiedAutoAwayConfig(Modifier.padding(50.dp,0.dp)) }
                item { Label(Modifier.padding(50.dp,0.dp)) }
                item { Configurations(Modifier.padding(50.dp,0.dp)) }
                item { AnalogMinMaxConfigurations(Modifier.padding(50.dp,0.dp)) }
                item { ThresholdTargetConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { DividerRow() }
                item { DisplayInDeviceConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { DividerRow() }
                item { PinPasswordView(viewModel) }
                item { DividerRow() }
                item { MisSettingConfig(viewModel,Modifier.padding(50.dp,0.dp)) }
                item { SaveConfig(viewModel,Modifier.padding(50.dp,25.dp)) }
            }
        }
    }

    /**
     * This function is used to display the Relay configurations
     * overriden because analog out has some staged configuration specific to 2Pipe profile
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
                DrawAnalogOutput() // only Analog Out function is using overriden function
                DrawThermistors()
                DrawAnalogIn()
            }
        }
    }

    @Composable
    override fun DrawAnalogOutput() {
        val analogEnum =
            viewModel.getAllowedValues(DomainName.analog1OutputAssociation, viewModel.equipModel)

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

            AnalogOutConfig(analogOutName = analogOut,
                enabled = enabled,
                onEnabledChanged = { enabledAction ->
                    when (index) {
                        0 -> viewModel.viewState.value.analogOut1Enabled = enabledAction
                        1 -> viewModel.viewState.value.analogOut2Enabled = enabledAction
                        2 -> viewModel.viewState.value.analogOut3Enabled = enabledAction
                    }

                },
                association = analogEnum[associationIndex],
                analogOutEnums = analogEnum,
                testSingles = viewModel.testSignalVoltage,
                isEnabled = enabled,
                onAssociationChanged = { association ->
                    when (index) {
                        0 -> viewModel.viewState.value.analogOut1Association = association.index
                        1 -> viewModel.viewState.value.analogOut2Association = association.index
                        2 -> viewModel.viewState.value.analogOut3Association = association.index
                    }

                },
                testVal = viewModel.getAnalogOutValue(index + 1) ?: 0.0,
                onTestSignalSelected = {
                    viewModel.sendTestSignal(
                        -1,
                        index + 1,
                        it
                    )
                },
                padding = 7
            )
        }
    }

    @Composable
    fun AnalogMinMaxConfigurations(modifier: Modifier = Modifier) {
        Column(modifier = modifier.padding(start = 25.dp, top = 25.dp)) {
            CoolingWaterModulatingMinMax()
            HeatingWaterModulatingMinMax()
            FanSpeedMinMax()
            DcvDamperMinMax()
            FanConfiguration()
        }
    }


    @Composable
    fun DcvDamperMinMax() {
        (viewModel.viewState.value as Pipe4ViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.dcvDamperConfig, 1, HsPipe4AnalogOutMapping.DCV_DAMPER)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.dcvDamperConfig, 2, HsPipe4AnalogOutMapping.DCV_DAMPER)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.dcvDamperConfig, 3, HsPipe4AnalogOutMapping.DCV_DAMPER)
        }
    }
    @Composable
    fun CoolingWaterModulatingMinMax() {
        (viewModel.viewState.value as Pipe4ViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.coolingModulatingValue, 1, HsPipe4AnalogOutMapping.COOLING_MODULATING_VALUE)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.coolingModulatingValue, 2, HsPipe4AnalogOutMapping.COOLING_MODULATING_VALUE)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.coolingModulatingValue, 3, HsPipe4AnalogOutMapping.COOLING_MODULATING_VALUE)
        }
    }

    @Composable
    fun HeatingWaterModulatingMinMax() {
        (viewModel.viewState.value as Pipe4ViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.heatingModulatingValue, 1, HsPipe4AnalogOutMapping.HEATING_MODULATING_VALUE)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.heatingModulatingValue, 2, HsPipe4AnalogOutMapping.HEATING_MODULATING_VALUE)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.heatingModulatingValue, 3, HsPipe4AnalogOutMapping.HEATING_MODULATING_VALUE)
        }
    }

    @Composable
    fun FanConfiguration() {
        @Composable
        fun ConfigFan(enabled: Boolean, association: Int, fanConfig: FanSpeedConfig, analogIndex: Int) {
            if (enabled && association == HsPipe4AnalogOutMapping.FAN_SPEED.ordinal) {

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

        (viewModel.viewState.value as Pipe4ViewState).apply {
            ConfigFan(analogOut1Enabled, analogOut1Association, analogOut1FanConfig, 1)
            ConfigFan(analogOut2Enabled, analogOut2Association, analogOut2FanConfig, 2)
            ConfigFan(analogOut3Enabled, analogOut3Association, analogOut3FanConfig, 3)
        }
    }

    @Composable
    fun FanSpeedMinMax() {
        (viewModel.viewState.value as Pipe4ViewState).apply {
            if (analogOut1Enabled)
                ConfigMinMax(analogOut1Association, analogOut1MinMax.fanSpeedConfig, 1, HsPipe4AnalogOutMapping.FAN_SPEED)
            if (analogOut2Enabled)
                ConfigMinMax(analogOut2Association, analogOut2MinMax.fanSpeedConfig, 2, HsPipe4AnalogOutMapping.FAN_SPEED)
            if (analogOut3Enabled)
                ConfigMinMax(analogOut3Association, analogOut3MinMax.fanSpeedConfig, 3, HsPipe4AnalogOutMapping.FAN_SPEED)
        }
    }

    @Composable
    private fun ConfigMinMax(association: Int, minMax: MinMaxConfig, analogIndex: Int, mapping: HsPipe4AnalogOutMapping) {
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
        this@HyperStatPipe4Fragment.closeAllBaseDialogFragments()
    }
}