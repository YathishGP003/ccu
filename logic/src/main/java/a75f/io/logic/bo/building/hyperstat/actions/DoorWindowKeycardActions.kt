package a75f.io.logic.bo.building.hyperstat.actions

import a75f.io.domain.equips.hyperstat.HyperStatEquip

/**
 * Created by Manjunath K on 22-07-2022.
 */

interface DoorWindowKeycardActions {
    fun doorWindowIsOpen(doorWindowEnabled: Double, doorWindowSensor: Double, equip: HyperStatEquip?)
    fun keyCardIsInSlot(keycardEnabled: Double, keycardSensor: Double, equip: HyperStatEquip?)
}