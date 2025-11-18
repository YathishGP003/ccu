package a75f.io.renatus.profiles.hss.unitventilator.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.composables.CancelDialog
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.Pipe4UvViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.UnitVentilatorViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe4UvViewState
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

class Pipe4UVFragment : UnitVentilatorFragment(), OnPairingCompleteListener {

    override val viewModel: Pipe4UvViewModel by viewModels()

    companion object {
        val ID: String = Pipe4UVFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType
        ): Pipe4UVFragment {
            val fragment = Pipe4UVFragment()
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
            viewModel.setOnPairingCompleteListener(this@Pipe4UVFragment)
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            withContext(Dispatchers.Main) {
                rootView.setContent {
                   RootView()
                }
            }
        }

        return rootView
    }


    @Composable
    fun RootView()
    {
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
                        AnalogOutDynamicConfig(viewModel)
                        ProfileBasedAnalogOutView()
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
    }

    @Composable
    fun ProfileBasedAnalogOutView(){
        CoolingControl(viewModel)
        HeatingControl(viewModel)
    }


    @Composable
    fun CoolingControl(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4UvViewModel -> Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal
            else -> { Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal}
        }
        Column(modifier = modifier) {
            if (viewModel.isCoolingAOEnabled(key)) {
                EnableCoolingVoltage(viewModel, key)
            }
        }
    }



    @Composable
    fun EnableCoolingVoltage(viewModel: UnitVentilatorViewModel, type: Int) {
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nCooling Modulating Valve",
                "Analog-out1 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nCooling Modulating Valve",
                "Analog-out2 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nCooling Modulating Valve",
                "Analog-out3 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nCooling Modulating Valve",
                "Analog-out4 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun HeatingControl(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {

        val key = when(viewModel){
            is Pipe4UvViewModel -> Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal
            else -> { Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal}
        }
        Column(modifier = modifier) {
            if (viewModel.isHeatingAOEnabled(key)) {
                EnableHeatingVoltage(viewModel, key)
            }
        }
    }

    @Composable
    fun EnableHeatingVoltage(viewModel: UnitVentilatorViewModel, type: Int) {
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nHeating Modulating Valve",
                "Analog-out1 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value  as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nHeating Modulating Valve",
                "Analog-out2 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nHeating Modulating Valve",
                "Analog-out3 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nHeating Modulating Valve",
                "Analog-out4 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
    }


    override fun onPairingComplete() {
        this.closeAllBaseDialogFragments()
    }


}