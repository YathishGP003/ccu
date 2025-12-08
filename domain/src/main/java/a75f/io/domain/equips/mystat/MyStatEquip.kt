package a75f.io.domain.equips.mystat

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point
import a75f.io.domain.equips.StandAloneEquip

/**
 * Created by Manjunath K on 26-09-2024.
 */

open class MyStatEquip(equipRef: String) : StandAloneEquip(equipRef) {
    val mystatAuxHeating1Activate = Point(DomainName.mystatAuxHeating1Activate, equipRef)
    val mystatStageUpTimerCounter = Point(DomainName.mystatStageUpTimerCounter, equipRef)
    val mystatStageDownTimerCounter = Point(DomainName.mystatStageDownTimerCounter, equipRef)
}