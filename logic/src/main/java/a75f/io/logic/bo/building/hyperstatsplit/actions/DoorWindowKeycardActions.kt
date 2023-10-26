package a75f.io.logic.bo.building.hyperstatsplit.actions

/**
 * Created by Nick P on 07-24-2023.
 */

interface DoorWindowKeycardActions {
    fun doorWindowIsOpen(doorWindowEnabled: Double, doorWindowSensor: Double)
    fun keyCardIsInSlot(keycardEnabled: Double, keycardSensor: Double)
}