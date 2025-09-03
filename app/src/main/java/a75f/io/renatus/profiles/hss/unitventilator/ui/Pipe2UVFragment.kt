package a75f.io.renatus.profiles.hss.unitventilator.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.CancelDialog
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.Pipe2UvViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe2UvViewState
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

class Pipe2UVFragment : UnitVentilatorFragment(),OnPairingCompleteListener{

    override val viewModel: Pipe2UvViewModel by viewModels()

    companion object {
        val ID: String = Pipe2UVFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): Pipe2UVFragment {
            val fragment = Pipe2UVFragment()
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            viewModel.setOnPairingCompleteListener(this@Pipe2UVFragment)
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

        LazyColumn(modifier = Modifier.fillMaxSize()) {

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
                Column(modifier = Modifier.padding(50.dp, 25.dp)) {
                    if (viewModel.openCancelDialog) {
                        CancelDialog(
                            onDismissRequest = { viewModel.openCancelDialog = false },
                            onConfirmation = { viewModel.cancelConfirm() },
                            dialogTitle = "Confirmation",
                            dialogText = "You have unsaved changes. Are you sure you want to cancel them?"
                        )
                    }
                    GenericView(viewModel)
                    ConfigurationsView(viewModel)
                    WaterValve(viewModel)
                    AnalogOutDynamicConfig(viewModel)
                    OAODamperConfig(viewModel)
                    DividerRow()
                    DisplayInDeviceConfig(viewModel)
                    PinPasswordView(viewModel)
                    MiscSettingConfig(viewModel)
                    SaveConfig(viewModel)
                }
            }
        }

    }

    @Composable
    fun WaterValve(viewModel: Pipe2UvViewModel) {

        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE.ordinal
            )
        ) {
            MinMaxConfiguration(
                "Analog-out1 at Min \nWater Valve",
                "Analog-out1 at Max \n Water Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut1MinMax.waterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut1MinMax.waterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut1MinMax.waterValveMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut1MinMax.waterValveMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE.ordinal
            )
        ) {
            MinMaxConfiguration(
                "Analog-out2 at Min \nWater Valve",
                "Analog-out2 at Max \n Water Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut2MinMax.waterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut2MinMax.waterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut2MinMax.waterValveMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut2MinMax.waterValveMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE.ordinal
            )
        ) {
            MinMaxConfiguration(
                "Analog-out3 at Min \nWater Valve",
                "Analog-out3 at Max \n Water Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut3MinMax.waterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut3MinMax.waterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut3MinMax.waterValveMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut3MinMax.waterValveMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                Pipe2UvAnalogOutControls.WATER_MODULATING_VALVE.ordinal
            )
        ) {
            MinMaxConfiguration(
                "Analog-out4 at Min \nWater Valve",
                "Analog-out4 at Max \n Water Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut4MinMax.waterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe2UvViewState).analogOut4MinMax.waterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut4MinMax.waterValveMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe2UvViewState).analogOut4MinMax.waterValveMaxVoltage =
                        it.value.toInt()
                }
            )
        }

    }

    override fun onPairingComplete() {
        this.closeAllBaseDialogFragments()
    }
}