package a75f.io.domain.equips.unitVentilator

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

/**
 * Author: Manjunath Kundaragi
 * Created on: 07-08-2025
 */
class Pipe2UVEquip(equipRef:String):UnitVentilatorEquip(equipRef){

    val analog1MaxWaterValve = Point(DomainName.analog1MaxWaterValve, equipRef)
    val analog2MaxWaterValve = Point(DomainName.analog2MaxWaterValve, equipRef)
    val analog3MaxWaterValve = Point(DomainName.analog3MaxWaterValve, equipRef)
    val analog4MaxWaterValve = Point(DomainName.analog4MaxWaterValve, equipRef)

    val analog1MinWaterValve = Point(DomainName.analog1MinWaterValve, equipRef)
    val analog2MinWaterValve = Point(DomainName.analog2MinWaterValve, equipRef)
    val analog3MinWaterValve = Point(DomainName.analog3MinWaterValve, equipRef)
    val analog4MinWaterValve = Point(DomainName.analog4MinWaterValve, equipRef)
    val waterValve = Point(DomainName.waterValve, equipRef)
    val waterModulatingValve = Point(DomainName.modulatingWaterValve, equipRef)
    val hyperstatPipe2FancoilHeatingThreshold = Point(DomainName.hyperstatPipe2FancoilHeatingThreshold, equipRef)
    val hyperstatPipe2FancoilCoolingThreshold = Point(DomainName.hyperstatPipe2FancoilCoolingThreshold, equipRef)
    val waterValveSamplingOnTime = Point(DomainName.waterValveSamplingOnTime, equipRef)
    val waterValveSamplingWaitTime = Point(DomainName.waterValveSamplingWaitTime, equipRef)
    val waterValveSamplingLoopDeadbandOnTime = Point(DomainName.waterValveSamplingLoopDeadbandOnTime, equipRef)
    val waterValveSamplingLoopDeadbandWaitTime = Point(DomainName.waterValveSamplingLoopDeadbandWaitTime, equipRef)
    val leavingWaterTemperature = Point(DomainName.leavingWaterTemperature, equipRef)

}