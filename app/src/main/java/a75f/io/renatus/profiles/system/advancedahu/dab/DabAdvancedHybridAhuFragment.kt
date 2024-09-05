package a75f.io.renatus.profiles.system.advancedahu.dab

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.system.dab.config.DabAdvancedHybridAhuConfig
import a75f.io.renatus.composables.DeleteDialog
import a75f.io.renatus.composables.SaveConfig
import a75f.io.renatus.profiles.system.advancedahu.AdvancedHybridAhuFragment
import a75f.io.renatus.util.AddProgressGif
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Manjunath K on 19-05-2024.
 */

class DabAdvancedHybridAhuFragment : AdvancedHybridAhuFragment() {
    override val viewModel: DabAdvancedHybridAhuViewModel by viewModels()

    companion object {
        lateinit var instance: DabAdvancedHybridAhuFragment
    }
    init {
        instance = this
    }


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewLifecycleOwner.lifecycleScope.launch (highPriorityDispatcher) {
            withContext(Dispatchers.Main) {
                rootView.apply {
                    setContent { AddProgressGif() }
                }
            }
            viewModel.init(requireContext(), CCUHsApi.getInstance())
            withContext(Dispatchers.Main) {
                rootView.apply {
                    setContent { RootView() }
                }
            }
        }
        return rootView
    }

    @Composable
    fun RootView() {
        Column {
            if (viewModel.viewState.value.pendingDeleteConnect) {
                DeleteDialog(onDismissRequest = { viewModel.viewState.value.pendingDeleteConnect = false }, onConfirmation = {
                    viewModel.viewState.value.isConnectEnabled = false
                    viewModel.viewState.value.pendingDeleteConnect = false
                    DabAdvancedAhuState.connectConfigToState(viewModel.profileConfiguration as DabAdvancedHybridAhuConfig, viewModel.viewState.value)
                }, toDelete = "Connect Module 1")
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