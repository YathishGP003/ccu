package a75f.io.logic.bo.building.system.dab

import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.AdvancedAhuAlgoHandler
import a75f.io.logic.bo.building.system.SystemMode

/**
 * Created by Manjunath K on 19-05-2024.
 */

class DabAdvancedAhu : DabSystemProfile() {
    override fun doSystemControl() {
        TODO("Not yet implemented")
    }

    override fun addSystemEquip() {
        TODO("Not yet implemented")
    }

    override fun deleteSystemEquip() {
        TODO("Not yet implemented")
    }

    override fun isCoolingAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHeatingAvailable(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCoolingActive(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHeatingActive(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getProfileType(): ProfileType {
        TODO("Not yet implemented")
    }

    override fun getStatusMessage(): String {
        TODO("Not yet implemented")
    }
    fun updateDomainEquip(equip: DabAdvancedHybridSystemEquip) {
        /*val systemMode = SystemMode.values()[systemEquip.conditioningMode.readPriorityVal().toInt()]
        advancedAhuImpl = AdvancedAhuAlgoHandler(equip)
        systemEquip = equip
        updateStagesSelected()
        updateSystemMode(systemMode)*/
    }
}