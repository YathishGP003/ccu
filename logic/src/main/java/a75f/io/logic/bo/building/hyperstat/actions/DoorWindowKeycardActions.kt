package a75f.io.logic.bo.building.hyperstat.actions

/**
 * Created by Manjunath K on 22-07-2022.
 */

interface DoorWindowKeycardActions {
    fun doorWindowIsOpen(doorWindowEnabled: Double, doorWindowSensor: Double)
    fun keyCardIsInSlot(keycardEnabled: Double, keycardSensor: Double)
}