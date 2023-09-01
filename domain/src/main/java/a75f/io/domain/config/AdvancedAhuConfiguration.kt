package a75f.io.domain.config

/**
 * Created by Manjunath K on 13-06-2023.
 */

class AdvancedAhuConfiguration(nodeAddress: Int,
                               nodeType: String, priority: Int, roomRef : String, floorRef : String
) : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef ) {
    override fun getAssociationConfigs() : List<AssociationConfig> {
        val associations = mutableListOf<AssociationConfig>()
        associations.add(AssociationConfig("relay1Association", 0))
        return associations
    }

    override fun getEnableConfigs() : List<EnableConfig> {
        val enabled = mutableListOf<EnableConfig>()
        enabled.add(EnableConfig("relay1Enabled", true))
        enabled.add(EnableConfig("dcwbEnabled", true))
        return enabled
    }
}