package a75f.io.logic.bo.building.connectnode

import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType

 class ConnectNodeConfiguration (nodeAddress: Int, nodeType: String, priority: Int, roomRef : String, floorRef : String, profileType : ProfileType)
    : ProfileConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType.name) {
     override fun getDependencies(): List<ValueConfig> {
            return listOf()
     }
      fun getDefaultConfiguration(): ConnectNodeConfiguration {
         return this
     }


 }