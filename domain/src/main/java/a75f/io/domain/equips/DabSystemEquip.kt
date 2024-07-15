package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 07-06-2024.
 */

open class DabSystemEquip(equipRef: String) : SystemEquip(equipRef) {

    val dabAnalogFanSpeedMultiplier = Point(DomainName.dabAnalogFanSpeedMultiplier, equipRef)
    val dabHumidityHysteresis = Point(DomainName.dabHumidityHysteresis, equipRef)
    val dabRelayDeactivationHysteresis = Point(DomainName.dabRelayDeactivationHysteresis, equipRef)
    val dabStageUpTimerCounter = Point(DomainName.dabStageUpTimerCounter, equipRef)
    val dabStageDownTimerCounter = Point(DomainName.dabStageDownTimerCounter, equipRef)
    val staticPressureSPMin = Point(DomainName.staticPressureSPMin, equipRef)
    val staticPressureSPMax = Point(DomainName.staticPressureSPMax, equipRef)
}