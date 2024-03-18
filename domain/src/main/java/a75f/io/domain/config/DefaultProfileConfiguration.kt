package a75f.io.domain.config

class DefaultProfileConfiguration(nodeAddress: Int,
                                  nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType: String
) : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType) {
    override fun getDependencies(): List<ValueConfig> {
        return mutableListOf()
    }
}