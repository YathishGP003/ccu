package a75f.io.renatus.profiles.system.advancedahu.vav

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.system.vav.config.VavAdvancedHybridAhuConfig
import a75f.io.renatus.composables.DeleteDialog
import a75f.io.renatus.composables.SaveConfig
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuFragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels

/**
 * Created by Manjunath K on 14-03-2024.
 */

class VavAdvancedHybridAhuFragment : AdvancedHybridAhuFragment() {

    override val viewModel: VavAdvancedHybridAhuViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewModel.init(requireContext(), CCUHsApi.getInstance())
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }
    }

    @Composable
    fun RootView() {
        Column {
            if (viewModel.viewState.value.pendingDeleteConnect) {
                DeleteDialog(
                    onDismissRequest = { viewModel.viewState.value.pendingDeleteConnect = false },
                    onConfirmation = {
                        viewModel.viewState.value.isConnectEnabled = false
                        viewModel.viewState.value.pendingDeleteConnect = false
                        VavAdvancedAhuState.connectConfigToState(viewModel.profileConfiguration as VavAdvancedHybridAhuConfig, viewModel.viewState.value)
                     },
                    toDelete = "Connect Module 1"
                )
            }
            

            LazyColumn {
                item { TitleLabel() }
                item { CMSensorConfig(viewModel) }
                item { CMAnalogInThIn(viewModel) }
                item { CMRelayConfig(viewModel) }
                item { CMAnalogOutConfig(viewModel) }
                item { CMAnalogOutDynamicConfig(viewModel) }

                if (viewModel.viewState.value.isConnectEnabled) {
                    item { ConnectModule1(viewModel) }
                    item { ConnectSensorConfig(viewModel) }
                    item { ConnectRelayConfig(viewModel) }
                    item { ConnectAnalogOutConfig(viewModel) }
                    item { ConnectUniInConfig(viewModel) }
                    item { ConnectAnalogOutDynamicConfig(viewModel) }
                } else {
                    item { AddConnectModule(viewModel) }
                }

                item { SaveConfig(viewModel) }
            }
        }
    }
}