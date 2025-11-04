package a75f.io.logic.bo.building.pcn

import a75f.io.logic.L
import a75f.io.logic.connectnode.EquipModel

class PCNValidation {
    companion object {

        const val MAX_REGISTER_COUNT : Int = 50
        fun getPairedRegisterCount(
            pcnViewState: PCNViewState
        ) : Int{

            var pairedRegisterCount = 0
            pcnViewState.connectModuleList.forEach { connectModule ->
                pairedRegisterCount += connectModule.equipData.registerCount
            }
            pcnViewState.externalEquipList.forEach { externalEquip ->
                pairedRegisterCount += externalEquip.equipData.registerCount
            }
            pcnViewState.pcnEquips.forEach { pcn ->
                pairedRegisterCount += pcn.equipData.registerCount
            }
            return pairedRegisterCount
        }


        fun isEligibleToAdd(
            pcnViewState: PCNViewState
        ): Boolean {
             fun getPCNEquipSize() : Int {
                 return if(pcnViewState.pcnEquips.isNotEmpty()) {
                     1
                 } else {
                     0
                 }

            }
            return (getPairedRegisterCount(pcnViewState) < MAX_REGISTER_COUNT &&
                    pcnViewState.connectModuleList.size + pcnViewState.externalEquipList.size + getPCNEquipSize() < 4)
        }

        fun isServerIdsValid(
            pcnViewState: PCNViewState
        ): Int? {
            val mergedList: List<PCNViewState.Equip> =
                (pcnViewState.connectModuleList.filter { it.newConfiguration } + pcnViewState.externalEquipList.filter { it.newConfiguration })
                    .sortedBy { it.serverId }
            mergedList.forEach { equip ->
                if (L.isModbusSlaveIdExists(equip.serverId.toShort(), "")) {
                    return equip.serverId
                }
            }
            return null
        }

        fun availableRegisterCount(
            pcnViewState: PCNViewState
        ): Int {
            return MAX_REGISTER_COUNT - getPairedRegisterCount(pcnViewState)
        }

        fun getSumOfRegisters(equipModel: List<EquipModel>): Int {
            var count = 0
            equipModel.flatMap { it.parameters }.forEach { param ->
                when (param.param.value.parameterDefinitionType) {
                    "boolean", "binary", "integer" -> {
                        count += 1 // Count as one register for boolean and binary types
                    }

                    "long", "unsigned long", "digital", "decimal", "float", "int32" -> {
                        count += 2 // Count as two registers for long, unsigned long, digital, decimal, float, and int32 types
                    }

                    "range" -> {
                        count += 3 // Count as three registers for range type
                    }

                    "int64" -> {
                        count += 4 // Count as four registers for int64 type
                    }

                    else -> {
                        count += 2 // Default case, count as one register for any other type
                    }
                }
            }
            return count
        }

        enum class Validation {
            SUCCESS,
            REGISTER_COUNT_EXCEEDED,
            DEVICE_LIMIT_EXCEEDED,
        }
    }
}