package a75f.io.domain

import a75f.io.domain.config.AssociationConfig
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.ValueConfig

class HyperStat2PfcuDeviceUpdateConfiguration(nodeAddress: Int,
                                               nodeType: String, priority: Int, roomRef : String, floorRef : String) :
    HyperStat2pfcuTestConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef){

    override fun getAssociationConfigs() : List<AssociationConfig> {
        var associations = mutableListOf<AssociationConfig>()
        associations.add(AssociationConfig("relay1Association", 2))
        return associations
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        var enabled = mutableListOf<EnableConfig>()
        enabled.add(EnableConfig("relay1Enabled", true))
        enabled.add(EnableConfig("dcwbEnabled", true))
        return enabled
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }
}