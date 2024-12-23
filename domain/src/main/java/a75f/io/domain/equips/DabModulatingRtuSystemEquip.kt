package a75f.io.domain.equips

import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Point

open class DabModulatingRtuSystemEquip(equipRef : String) : DabSystemEquip(equipRef) {
    //Base points
    val analog1OutputEnable = Point(DomainName.analog1OutputEnable ,equipRef)
    val analog2OutputEnable = Point(DomainName.analog2OutputEnable ,equipRef)
    val analog3OutputEnable = Point(DomainName.analog3OutputEnable ,equipRef)
    val analog4OutputEnable = Point(DomainName.analog4OutputEnable ,equipRef)
    val relay3OutputEnable = Point(DomainName.relay3OutputEnable ,equipRef)
    val relay7OutputEnable = Point(DomainName.relay7OutputEnable ,equipRef)
    val occupancySignal = Point(DomainName.occupancySignal ,equipRef)
    val dcwbEnable = Point(DomainName.dcwbEnable ,equipRef)
    val adaptiveComfortThresholdMargin = Point(DomainName.adaptiveComfortThresholdMargin ,equipRef)

    //dependant points
    val coolingSignal = Point(DomainName.coolingSignal ,equipRef)
    val fanSignal = Point(DomainName.fanSignal ,equipRef)
    val heatingSignal = Point(DomainName.heatingSignal ,equipRef)
    val outsideAirDamper = Point(DomainName.outsideAirDamper ,equipRef)
    val fanEnable = Point(DomainName.fanEnable ,equipRef)
    val adaptiveDeltaEnable = Point(DomainName.adaptiveDeltaEnable ,equipRef)
    val systemDCWBValveLoopOutput = Point(DomainName.systemDCWBValveLoopOutput ,equipRef)
    val chilledWaterExitTemperatureMargin = Point(DomainName.chilledWaterExitTemperatureMargin ,equipRef)
    val chilledWaterExitTemperatureTarget = Point(DomainName.chilledWaterExitTemperatureTarget ,equipRef)
    val chilledWaterValveSignal = Point(DomainName.chilledWaterValveSignal ,equipRef)
    val maximizedExitWaterTempEnable = Point(DomainName.maximizedExitWaterTempEnable ,equipRef)
    val chilledWaterTargetDelta = Point(DomainName.chilledWaterTargetDelta ,equipRef)
    val chilledWaterMaxFlowRate = Point(DomainName.chilledWaterMaxFlowRate ,equipRef)
    val analog1ValveClosedPosition = Point(DomainName.analog1ValveClosedPosition ,equipRef)
    val analog1ValveFullPosition = Point(DomainName.analog1ValveFullPosition ,equipRef)

    //associated points
    val humidifier = Point(DomainName.humidifierEnable ,equipRef)
    val dehumidifier = Point(DomainName.dehumidifierEnable ,equipRef)
    val analog4OutputAssociation = Point(DomainName.analog4OutputAssociation ,equipRef);

    //dependant points
    val analog1MinCooling = Point(DomainName.analog1MinCooling, equipRef)
    val analog1MaxCooling = Point(DomainName.analog1MaxCooling, equipRef)
    val analog2MinStaticPressure = Point(DomainName.analog2MinStaticPressure, equipRef)
    val analog2MaxStaticPressure = Point(DomainName.analog2MaxStaticPressure, equipRef)
    val analog3MinHeating = Point(DomainName.analog3MinHeating, equipRef)
    val analog3MaxHeating = Point(DomainName.analog3MaxHeating, equipRef)
    val analogOut4MinCoolingLoop = Point(DomainName.analogOut4MinCoolingLoop, equipRef)
    val analogOut4MaxCoolingLoop = Point(DomainName.analogOut4MaxCoolingLoop, equipRef)
    val analog4MinOutsideDamper = Point(DomainName.analog4MinOutsideDamper, equipRef)
    val analog4MaxOutsideDamper = Point(DomainName.analog4MaxOutsideDamper, equipRef)
    val analog2MinFan = Point(DomainName.analog2MinFan, equipRef)
    val analog2MaxFan = Point(DomainName.analog2MaxFan, equipRef)

    //Association point
    val relay7OutputAssociation = Point(DomainName.relay7OutputAssociation , equipRef)
}