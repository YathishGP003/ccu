package a75f.io.renatus.profiles.system

import a75f.io.domain.api.DomainName
import a75f.io.renatus.composables.SystemRelayMappingView
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment

open class StagedRtuFragment : Fragment() {
    @Composable
    fun StagedRtuRelayMappingView(viewModel: StagedRtuProfileViewModel) {
        Spacer(modifier = Modifier.height(5.dp))

        SystemRelayMappingView(
            relayText = "Relay 1",
            relayState = viewModel.viewState.relay1Enabled,
            onRelayEnabled = { viewModel.viewState.relay1Enabled = it
                viewModel.saveConfiguration()},
            mappingSelection = viewModel.viewState.relay1Association,
            mapping = viewModel.relay1AssociationList,
            onMappingChanged = { viewModel.viewState.relay1Association = it
                viewModel.saveConfiguration()},
            buttonState = viewModel.getRelayState(DomainName.relay1),
            onTestActivated = {viewModel.sendTestCommand(DomainName.relay1, it)})

        Spacer(modifier = Modifier.height(18.dp))

        SystemRelayMappingView(
            relayText = "Relay 2",
            relayState = viewModel.viewState.relay2Enabled,
            onRelayEnabled = { viewModel.viewState.relay2Enabled = it
                viewModel.saveConfiguration()},
            mappingSelection = viewModel.viewState.relay2Association,
            mapping = viewModel.relay2AssociationList,
            onMappingChanged = { viewModel.viewState.relay2Association = it
                viewModel.saveConfiguration()},
            buttonState = viewModel.getRelayState(DomainName.relay2),
            onTestActivated = {viewModel.sendTestCommand(DomainName.relay2, it)})

        Spacer(modifier = Modifier.height(21.dp))

        SystemRelayMappingView(
            relayText = "Relay 3",
            relayState = viewModel.viewState.relay3Enabled,
            onRelayEnabled = { viewModel.viewState.relay3Enabled = it
                viewModel.saveConfiguration()},
            mappingSelection = viewModel.viewState.relay3Association,
            mapping = viewModel.relay3AssociationList,
            onMappingChanged = { viewModel.viewState.relay3Association = it
                viewModel.saveConfiguration()},
            buttonState = viewModel.getRelayState(DomainName.relay3),
            onTestActivated = {viewModel.sendTestCommand(DomainName.relay3, it)})

        Spacer(modifier = Modifier.height(14.dp))

        SystemRelayMappingView(
            relayText = "Relay 4",
            relayState = viewModel.viewState.relay4Enabled,
            onRelayEnabled = { viewModel.viewState.relay4Enabled = it
                viewModel.saveConfiguration()},
            mappingSelection = viewModel.viewState.relay4Association,
            mapping = viewModel.relay4AssociationList,
            onMappingChanged = { viewModel.viewState.relay4Association = it
                viewModel.saveConfiguration()},
            buttonState = viewModel.getRelayState(DomainName.relay4),
            onTestActivated = {viewModel.sendTestCommand(DomainName.relay4, it)})

        Spacer(modifier = Modifier.height(15.dp))

        SystemRelayMappingView(
            relayText = "Relay 5",
            relayState = viewModel.viewState.relay5Enabled,
            onRelayEnabled = { viewModel.viewState.relay5Enabled = it
                viewModel.saveConfiguration()},
            mappingSelection = viewModel.viewState.relay5Association,
            mapping = viewModel.relay5AssociationList,
            onMappingChanged = { viewModel.viewState.relay5Association = it
                viewModel.saveConfiguration()},
            buttonState = viewModel.getRelayState(DomainName.relay5),
            onTestActivated = {viewModel.sendTestCommand(DomainName.relay5, it)})

        Spacer(modifier = Modifier.height(20.dp))

        SystemRelayMappingView(
            relayText = "Relay 6",
            relayState = viewModel.viewState.relay6Enabled,
            onRelayEnabled = { viewModel.viewState.relay6Enabled = it
                viewModel.saveConfiguration()},
            mappingSelection = viewModel.viewState.relay6Association,
            mapping = viewModel.relay6AssociationList,
            onMappingChanged = { viewModel.viewState.relay6Association = it
                viewModel.saveConfiguration()},
            buttonState = viewModel.getRelayState(DomainName.relay6),
            onTestActivated = {viewModel.sendTestCommand(DomainName.relay6, it)})

        Spacer(modifier = Modifier.height(17.dp))

        SystemRelayMappingView(
            relayText = "Relay 7",
            relayState = viewModel.viewState.relay7Enabled,
            onRelayEnabled = { viewModel.viewState.relay7Enabled = it
                viewModel.saveConfiguration()},
            mappingSelection = viewModel.viewState.relay7Association,
            mapping = viewModel.relay7AssociationList,
            onMappingChanged = { viewModel.viewState.relay7Association = it
                viewModel.saveConfiguration()},
            buttonState = viewModel.getRelayState(DomainName.relay7),
            onTestActivated = {viewModel.sendTestCommand(DomainName.relay7, it)})
    }
}