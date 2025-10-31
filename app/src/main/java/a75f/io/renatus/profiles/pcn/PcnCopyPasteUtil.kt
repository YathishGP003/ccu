package a75f.io.renatus.profiles.pcn

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.pcn.ConnectModule
import a75f.io.logic.bo.building.pcn.ExternalEquip
import a75f.io.logic.bo.building.pcn.PCN
import a75f.io.logic.bo.building.pcn.PCNViewState
import a75f.io.logic.bo.building.pcn.PcnConfiguration
import a75f.io.logic.connectnode.EquipModel
import androidx.compose.runtime.MutableState

class PcnCopyPasteUtil {
    companion object {

        fun checkSelectedConfigurationAndCurrentEquipListSame(pcnConfiguration: PcnConfiguration ,selectedConfiguration: PcnConfiguration): Boolean {
            val copiedPcnEquips = selectedConfiguration.pcnEquips.map { it.equipModelList }.sortedBy { it.equipDevice.value.name }
            val currentPcnEquips = pcnConfiguration.pcnEquips.map { it.equipModelList }.sortedBy { it.equipDevice.value.name }

            val copiedConnectModules = selectedConfiguration.connectModuleList.sortedBy { it.serverId }
            val currentConnectModules = pcnConfiguration.connectModuleList.sortedBy { it.serverId }

            val copiedExternalEquips = selectedConfiguration.externalEquipList.sortedBy { it.serverId }
            val currentExternalEquips = pcnConfiguration.externalEquipList.sortedBy { it.serverId }

            if (isCurrentAndCopiedModelSame(currentPcnEquips, copiedPcnEquips) &&
                isCurrentAndCopiedModelSameForConnect(currentConnectModules, copiedConnectModules) &&
                isCurrentAndCopiedModelSameForExternal(currentExternalEquips, copiedExternalEquips)) {
                CcuLog.i(L.TAG_PCN, "Copied configuration matches current equipment list")
                return true
            }

            CcuLog.i(L.TAG_PCN, "Copied configuration Does not matches current equipment list")
            return false
        }

        private fun isCurrentAndCopiedModelSameForExternal(currentExternalEquips: List<ExternalEquip>, copiedExternalEquips: List<ExternalEquip>): Boolean {
            if (currentExternalEquips.size != copiedExternalEquips.size) {
                CcuLog.i(L.TAG_PCN, "Copied configuration does not match current equipment list size")
                return false
            }
            for (i in currentExternalEquips.indices) {
                val currentEquip = currentExternalEquips[i]
                val copiedEquip = copiedExternalEquips[i]

                if (currentEquip.equipData.equipModel.size != copiedEquip.equipData.equipModel.size ||
                    currentEquip.serverId != copiedEquip.serverId ||
                    !isCurrentAndCopiedModelSame(currentEquip.equipData.equipModel.sortedBy { it.equipDevice.value.name }, copiedEquip.equipData.equipModel.sortedBy { it.equipDevice.value.name })
                ) {
                    CcuLog.i(L.TAG_PCN, "Copied configuration does not match current equipment list data")
                    return false
                }
            }
            return true
        }

        private fun isCurrentAndCopiedModelSameForConnect(currentPcnEquips: List<ConnectModule>, copiedPcnEquips: List<ConnectModule>): Boolean {
            // Check if the sizes AND Data of the lists are the same
            if (currentPcnEquips.size != copiedPcnEquips.size) {
                CcuLog.i(L.TAG_PCN, "Copied configuration does not match current equipment list size")
                return false
            }
            for (i in copiedPcnEquips.indices) {
                val currentEquip = currentPcnEquips[i]
                val copiedEquip = copiedPcnEquips[i]

                if (currentEquip.equipData.equipModel.size != copiedEquip.equipData.equipModel.size ||
                    currentEquip.serverId != copiedEquip.serverId ||
                    !isCurrentAndCopiedModelSame(currentEquip.equipData.equipModel.sortedBy { it.equipDevice.value.name }, copiedEquip.equipData.equipModel.sortedBy { it.equipDevice.value.name })
                ) {
                    CcuLog.i(L.TAG_PCN, "Copied configuration does not match current equipment list data")
                    return false
                }
            }
            return true
        }

        private fun isCurrentAndCopiedModelSame(
            currentPcnEquips: List<EquipModel>,
            copiedPcnEquips: List<EquipModel>
        ) : Boolean {

            if (currentPcnEquips.size != copiedPcnEquips.size) {
                CcuLog.i(L.TAG_PCN, "Copied configuration does not match current equipment list size")
                return false
            }
            for (i in copiedPcnEquips.indices) {
                val currentEquip = currentPcnEquips[i]
                val copiedEquip = copiedPcnEquips[i]

                if (currentEquip.equipDevice.value.registers.size != copiedEquip.equipDevice.value.registers.size ||
                    currentEquip.equipDevice.value.name != copiedEquip.equipDevice.value.name
                ) {
                    CcuLog.i(L.TAG_PCN, "Copied configuration does not match current equipment list data")
                    return false
                }
            }
            return true
        }

        fun copyState(pcnState: MutableState<PCNViewState>, copiedConfig: PcnConfiguration) {

            pcnState.value.apply {
                copyPCNEquips(this.pcnEquips.sortedBy { it.name }, copiedConfig.pcnEquips.sortedBy { it.name })
                copyCNEquips(this.connectModuleList.sortedBy { it.serverId }, copiedConfig.connectModuleList.sortedBy { it.serverId })
                copyExtEquips(this.externalEquipList.sortedBy { it.serverId }, copiedConfig.externalEquipList.sortedBy { it.serverId } )
                copyRS485BridgingDetails(this, copiedConfig)
            }
        }

        private fun copyRS485BridgingDetails(pcnViewState: PCNViewState, copiedConfig: PcnConfiguration) {
            pcnViewState.baudRate.doubleValue = copiedConfig.baudRate.doubleValue
            pcnViewState.parity.doubleValue = copiedConfig.parity.doubleValue
            pcnViewState.dataBits.doubleValue = copiedConfig.dataBits.doubleValue
            pcnViewState.stopBits.doubleValue = copiedConfig.stopBits.doubleValue
        }

        private fun copyPCNEquips(pcnEquips: List<PCN>, copiedPcnEquips: List<PCN>) {

            pcnEquips.forEachIndexed { index, equipModel ->
                val copiedEquipModel = copiedPcnEquips[index].equipModelList
                copy(equipModel.equipModelList, copiedEquipModel)
            }

        }

        private fun copyCNEquips(connectModuleList: List<ConnectModule>, copiedConnectModuleList: List<ConnectModule>) {

            connectModuleList.forEachIndexed { index, equipModel ->
                val copiedEquipModel = copiedConnectModuleList[index].equipModelList.sortedBy { it.equipDevice.value.name }
                equipModel.equipModelList.sortedBy { it.equipDevice.value.name }.forEachIndexed { subIndex, subEquipModel ->
                    copy(subEquipModel, copiedEquipModel[subIndex])
                }
            }
        }

        private fun copyExtEquips(externalEquips: List<ExternalEquip>, copiedExternalEquipList: List<ExternalEquip>) {

            externalEquips.forEachIndexed { index, equipModel ->
                copy(equipModel.equipModel, copiedExternalEquipList[index].equipModel)
            }
        }

        private fun copy(equipmentDeviceList: EquipModel, copiedEquipList: EquipModel) {
            val newRegisters = copiedEquipList.parameters.sortedBy { it.param.value.name }

            equipmentDeviceList.parameters.sortedBy { it.param.value.name }.forEachIndexed { index, param ->
                val newParam = newRegisters[index]
                param.displayInUi.value = newParam.param.value.isDisplayInUI
                param.schedulable.value = newParam.param.value.isSchedulable
            }
        }

    }
}