package a75f.io.logic.bo.building.pcn

class PCNViewStateUtil {
    companion object {
        fun configToState (
            pcnConfig: PcnConfiguration,
            pcnState : PCNViewState
        ): PCNViewState {
            pcnState.connectModuleList = pcnConfig.connectModuleList
            pcnState.externalEquipList = pcnConfig.externalEquipList
            pcnState.pcnEquips = pcnConfig.pcnEquips

            pcnState.baudRate = pcnConfig.baudRate
            pcnState.parity = pcnConfig.parity
            pcnState.dataBits = pcnConfig.dataBits
            pcnState.stopBits = pcnConfig.stopBits

            return pcnState
        }

        fun stateToConfig (
            pcnState: PCNViewState,
            pcnConfig : PcnConfiguration
        ): PcnConfiguration {
            pcnConfig.connectModuleList = pcnState.connectModuleList
            pcnConfig.externalEquipList = pcnState.externalEquipList

            pcnConfig.deletedCNList = pcnState.deletedCNList
            pcnConfig.deletedExternalEquipList = pcnState.deletedExternalEquipList

            pcnConfig.pcnEquips = pcnState.pcnEquips

            pcnConfig.baudRate = pcnState.baudRate
            pcnConfig.parity = pcnState.parity
            pcnConfig.dataBits = pcnState.dataBits
            pcnConfig.stopBits = pcnState.stopBits

            return pcnConfig
        }
    }
}