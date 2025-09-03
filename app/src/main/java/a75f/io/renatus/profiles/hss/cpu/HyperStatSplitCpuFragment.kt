package a75f.io.renatus.profiles.hss.cpu

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.CpuAnalogControlType
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.CancelDialog
import a75f.io.renatus.composables.DuplicatePointDialog
import a75f.io.renatus.composables.ErrorDialog
import a75f.io.renatus.composables.MissingPointDialog
import a75f.io.renatus.composables.NO_COMPRESSOR
import a75f.io.renatus.composables.NO_MAT_SENSOR
import a75f.io.renatus.composables.NO_OB_REALLY
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hss.HyperStatSplitFragment
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

class HyperStatSplitCpuFragment : HyperStatSplitFragment(), OnPairingCompleteListener {

    private val viewModel: HyperStatSplitCpuViewModel by viewModels()

    companion object {
        val ID: String = HyperStatSplitCpuFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): HyperStatSplitCpuFragment {
            val fragment = HyperStatSplitCpuFragment()
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
            viewModel.setOnPairingCompleteListener(this@HyperStatSplitCpuFragment)
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
        if (viewModel.openCancelDialog) {
            CancelDialog(
                onDismissRequest = { viewModel.openCancelDialog = false },
                onConfirmation = { viewModel.cancelConfirm() },
                dialogTitle = "Confirmation",
                dialogText = "You have unsaved changes. Are you sure you want to cancel them?"
            )
        }

        if (viewModel.openDuplicateDialog) {
            DuplicatePointDialog(
                onDismissRequest = { viewModel.openDuplicateDialog = false },
                duplicates = "Supply Air Temperature, Mixed Air Temperature Sensor, Outside Air Temperature Sensor, Current TX Sensor, Filter Sensor, Condensate Sensor, or Generic Alarm Sensor")
        }

        if (viewModel.openMissingDialog) {
            MissingPointDialog(
                onDismissRequest = { viewModel.openMissingDialog = false },
                missing = "Mixed Air Temperature Sensor, Outside Air Temperature Sensor, or OAO Damper")
        }

        if (viewModel.noOBRelay) {
            ErrorDialog(NO_OB_REALLY, onDismissRequest = { viewModel.noOBRelay = false })
        }
        if (viewModel.noCompressorStages) {
            ErrorDialog(NO_COMPRESSOR, onDismissRequest = { viewModel.noCompressorStages = false })
        }
        if (viewModel.noMatSensor) {
            ErrorDialog(
                msg = NO_MAT_SENSOR,
                onDismissRequest = { viewModel.noMatSensor = false }
            )
        }

        Column {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                item {
                    val isDisabled by viewModel.isDisabled.observeAsState(false)
                    if (isDisabled) {
                        PasteBannerFragment.PasteCopiedConfiguration(
                            onPaste = { viewModel.applyCopiedConfiguration() },
                            onClose = { viewModel.disablePasteConfiguration() }
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(50.dp,25.dp)) {
                        Title(viewModel)
                        TempOffset(viewModel)
                        AutoAwayConfig(viewModel)
                        TitleLabel()
                        SensorConfig(viewModel)
                        RelayConfig(viewModel)
                        AnalogOutConfig(viewModel)
                        UniversalInConfig(viewModel)
                        AnalogOutDynamicConfig(viewModel)
                        ZoneOAOConfig(viewModel)
                        DividerRow()
                        DisplayInDeviceConfig(viewModel)
                        PinPasswordView(viewModel)
                        MiscSettingConfig(viewModel)
                        SaveConfig(viewModel)
                    }
                }
            }
        }
    }

    @Composable
    fun AnalogOutDynamicConfig(viewModel: HyperStatSplitCpuViewModel,modifier: Modifier = Modifier) {
        CoolingControl(viewModel,modifier)
        HeatingControl(viewModel,modifier)
        CompressorControl(viewModel,modifier)
        DcvModulationControl(viewModel,modifier)
        LinearFanControl(viewModel,modifier)
        StagedFanControl(viewModel,modifier)
        OAODamperControl(viewModel,modifier)
        ReturnDamperControl(viewModel,modifier)
    }

    @Composable
    fun LinearFanControl(viewModel: HyperStatSplitCpuViewModel,modifier: Modifier = Modifier) {
        Column(modifier =  modifier) {
            if (viewModel.isLinearFanAOEnabled(CpuAnalogControlType.LINEAR_FAN.ordinal)) {
                EnableLinearFanVoltage(viewModel, CpuAnalogControlType.LINEAR_FAN)
            }
        }
    }

    @Composable
    fun EnableLinearFanVoltage(viewModel: HyperStatSplitCpuViewModel, type: CpuAnalogControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nLinear Fan",
                "Analog-out1 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value  as HyperStatSplitCpuState).analogOut1MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nLinear Fan",
                "Analog-out2 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nLinear Fan",
                "Analog-out3 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nLinear Fan",
                "Analog-out4 at Max \nLinear Fan",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.linearFanMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun CoolingControl(viewModel: HyperStatSplitCpuViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            if (viewModel.isCoolingAOEnabled(CpuAnalogControlType.COOLING.ordinal)) {
                EnableCoolingVoltage(viewModel, CpuAnalogControlType.COOLING)
            }
        }
    }



    @Composable
    fun EnableCoolingVoltage(viewModel: HyperStatSplitCpuViewModel, type: CpuAnalogControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nCooling",
                "Analog-out1 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.coolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nCooling",
                "Analog-out2 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState ).analogOut2MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.coolingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nCooling",
                "Analog-out3 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.coolingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nCooling",
                "Analog-out4 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.coolingMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun HeatingControl(viewModel: HyperStatSplitCpuViewModel,modifier: Modifier = Modifier) {
        Column(modifier = modifier) {
            if (viewModel.isHeatingAOEnabled(CpuAnalogControlType.HEATING.ordinal)) {
                EnableHeatingVoltage(viewModel, CpuAnalogControlType.HEATING)
            }
        }
    }

    @Composable
    fun EnableHeatingVoltage(viewModel: HyperStatSplitCpuViewModel, type: CpuAnalogControlType) {
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nHeating",
                "Analog-out1 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value  as HyperStatSplitCpuState ).analogOut1MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState ).analogOut1MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut1MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nHeating",
                "Analog-out2 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut2MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nHeating",
                "Analog-out3 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut3MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                type,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nHeating",
                "Analog-out4 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as HyperStatSplitCpuState).analogOut4MinMax.heatingMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    override fun onPairingComplete() {
        this@HyperStatSplitCpuFragment.closeAllBaseDialogFragments()
    }

}