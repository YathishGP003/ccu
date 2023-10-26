package a75f.io.domain.config

import a75f.io.domain.config.*


class HyperStat2pfcuConfiguration(nodeAddress: Int,
                                      nodeType: String, priority: Int, roomRef : String, floorRef : String
) : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef ) {


    var temperatureOffset = ValueConfig("temperatureOffset", 0.0)
    var autoForcedOccupied = EnableConfig("autoForcedOccupiedEnabled", false)
    var autoAway = EnableConfig("autoAwayEnabled", false)

    //Explore data binding to directly map config value to a UI element.

    var relay1OutputEnabled = EnableConfig("relay1Enabled")
    var relay2OutputEnabled = EnableConfig( "relay2Enabled")
    var relay3OutputEnabled = EnableConfig( "relay3Enabled")
    var relay4OutputEnabled = EnableConfig( "relay4Enabled")
    var relay5OutputEnabled = EnableConfig( "relay5Enabled")
    var relay6OutputEnabled = EnableConfig( "relay6Enabled")

    //enabled is already present in outputEnabled config. But both are linked only in the domain model.
    var relay1OutputAssociation = RelayAssociationConfig( "relay1Association", 1)
    var relay2OutputAssociation = RelayAssociationConfig(  "relay2Association", 2)
    var relay3OutputAssociation = RelayAssociationConfig( "relay3Association", 3,)
    var relay4OutputAssociation = RelayAssociationConfig( "relay4Association", 3)
    var relay5OutputAssociation = RelayAssociationConfig( "relay5Association", 4)
    var relay6OutputAssociation = RelayAssociationConfig( "relay6Association", 1)

    var analogOut1Enabled = EnableConfig("analogOut1Enabled")
    var analogOut2Enabled = EnableConfig("analogOut2Enabled")
    var analogOut3Enabled = EnableConfig("analogOut3Enabled")

    var analogOut1Association = AnalogOutAssociationConfig( "analogOut1Association", 2, 2.0, 10.0)
    var analogOut2Association = AnalogOutAssociationConfig( "analogOut2Association", 1, 2.0, 10.0)
    var analogOut3Association = AnalogOutAssociationConfig( "analogOut3Association", 3, 2.0, 10.0)

    var airflowSensorEnabled = EnableConfig("airflowSensorEnabled")
    var supplyWaterSensorEnabled = EnableConfig("supplyWaterSensorEnabled")

    var analogIn1Config = AnalogInAssociationConfig( "analog1InEnabled", 2, 0)
    var analogIn2Config = AnalogInAssociationConfig("analog2InEnabled", 1, 0)

    var zoneCO2DamperOpeningRate = ValueConfig("zoneCO2DamperOpeningRate")
    var zoneCO2Threshold = ValueConfig("zoneCO2Threshold")
    var zoneCO2Target = ValueConfig("zoneCO2Target")
    var zoneVOCThreshold = ValueConfig("zoneVOCThreshold")
    var zoneVOCTarget = ValueConfig("zoneVOCTarget")
    var zonePm2p5Threshold = ValueConfig("zonePm2p5Threshold")
    var zonePm2p5Target = ValueConfig("zonePm2p5Target")

    init {
        zoneCO2DamperOpeningRate.currentVal = 10.0
        zoneCO2Threshold.currentVal = 4000.0
        zoneCO2Target.currentVal = 4000.0
        zoneVOCThreshold.currentVal = 10000.0
        zoneVOCTarget.currentVal = 10000.0
        zonePm2p5Threshold.currentVal = 1000.0
        zonePm2p5Target.currentVal = 1000.0

    }

    var displayHumidity = EnableConfig( "displayHumidity", true)
    var displayVOC = EnableConfig("displayVOC")
    var displayPp2p5 = EnableConfig("displayPp2p5")
    var displayCo2 = EnableConfig("displayCo2")

    var dcwbEnabled = EnableConfig( "dcwbEnabled",true)

    override fun getAssociationConfigs() : List<AssociationConfig> {
        val associations = mutableListOf<AssociationConfig>()
        associations.add(AssociationConfig("relay1Association", 0))
        return associations
    }

    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf()
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        var enabled = mutableListOf<EnableConfig>()
        enabled.add(EnableConfig("relay1Enabled", true))
        enabled.add(EnableConfig("dcwbEnabled", true))
        return enabled
    }
}
