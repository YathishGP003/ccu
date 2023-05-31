package a75f.io.domain

import a75f.io.domain.config.*


class HyperStat2pfcuTestConfiguration(nodeAddress: Int,
                                      nodeType: String, priority: Int
) : ProfileConfiguration(nodeAddress, nodeType, priority) {


    var temperatureOffset = ProfileConfig(true, "temperatureOffset")
    var autoForcedOccupied = ProfileConfig(false, "autoForcedOccupiedEnabled")
    var autoAway = ProfileConfig(false, "autoAwayEnabled")

    //Explore data binding to directly map config value to a UI element.

    var relay1OutputEnabled = ProfileConfig(false, "relay1OutputEnabled")
    var relay2OutputEnabled = ProfileConfig(false, "relay2OutputEnabled")
    var relay3OutputEnabled = ProfileConfig(false, "relay3OutputEnabled")
    var relay4OutputEnabled = ProfileConfig(false, "relay4OutputEnabled")
    var relay5OutputEnabled = ProfileConfig(false, "relay5OutputEnabled")
    var relay6OutputEnabled = ProfileConfig(false, "relay6OutputEnabled")

    //enabled is already present in outputEnabled config. But both are linked only in the domain model.
    var relay1OutputAssociation = PortConfig( false, "relay1OutputAssociation", "fanMediumSpeed")
    var relay2OutputAssociation = PortConfig( false, "relay2OutputAssociation", "fanHighSpeed")
    var relay3OutputAssociation = PortConfig( false, "relay3OutputAssociation", "fanStage1")
    var relay4OutputAssociation = PortConfig( false, "relay4OutputAssociation", "auxHeatingStage1")
    var relay5OutputAssociation = PortConfig( false, "relay5OutputAssociation", "auxHeatingStage2")
    var relay6OutputAssociation = PortConfig( false, "relay6OutputAssociation", "auxHeatingStage3")

    var analogOut1Enabled = ProfileConfig(false, "analogOut1Enabled")
    var analogOut2Enabled = ProfileConfig(false, "analogOut2Enabled")
    var analogOut3Enabled = ProfileConfig(false, "analogOut3Enabled")

    var analogOut1Association = AnalogOutConfig( false, "analogOut1Association", "waterValve", 2.0, 10.0)
    var analogOut2Association = AnalogOutConfig( false, "analogOut2Association", "fanSpeed", 2.0, 10.0)
    var analogOut3Association = AnalogOutConfig( false, "analogOut3Association", "dcvDamper", 2.0, 10.0)

    var airflowSensorEnabled = ProfileConfig(false, "airflowSensorEnabled")
    var supplyWaterSensorEnabled = ProfileConfig(true, "supplyWaterSensorEnabled")

    var analogIn1Config = AnalogInConfig(false, "analog1InEnabled", "keyCardSensor", 0)
    var analogIn2Config = AnalogInConfig(false, "analog2InEnabled", "doorWindowSensor", 0)

    var zoneCO2DamperOpeningRate = ProfileConfig(false, "zoneCO2DamperOpeningRate")

    var zoneCO2Threshold = ProfileConfig(false, "zoneCO2Threshold")
    var zoneCO2Target = ProfileConfig(false, "zoneCO2Target")
    var zoneVOCThreshold = ProfileConfig(false, "zoneVOCThreshold")
    var zoneVOCTarget = ProfileConfig(false, "zoneVOCTarget")
    var zonePm2p5Threshold = ProfileConfig(false, "zonePm2p5Threshold")
    var zonePm2p5Target = ProfileConfig(false, "zonePm2p5Target")

    init {
        zoneCO2DamperOpeningRate.currentVal = 10.0
        zoneCO2Threshold.currentVal = 4000.0
        zoneCO2Target.currentVal = 4000.0
        zoneVOCThreshold.currentVal = 10000.0
        zoneVOCTarget.currentVal = 10000.0
        zonePm2p5Threshold.currentVal = 1000.0
        zonePm2p5Target.currentVal = 1000.0

    }

    var displayHumidity = ProfileConfig(true, "displayHumidity")
    var displayVOC = ProfileConfig(false, "displayVOC")
    var displayPp2p5 = ProfileConfig(false, "displayPp2p5")
    var displayCo2 = ProfileConfig(true, "displayCo2")

    override fun getAssociations() : List<String> {
        var associations = mutableListOf<String>()
        associations.add(relay1OutputAssociation.association)
        associations.add(relay2OutputAssociation.association)
        associations.add(relay3OutputAssociation.association)
        associations.add(relay4OutputAssociation.association)
        associations.add(relay5OutputAssociation.association)
        associations.add(relay6OutputAssociation.association)
        return associations
    }
}
