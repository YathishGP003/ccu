package a75f.io.logic.bo.building.hyperstat.v2.configs

import a75f.io.domain.config.ValueConfig
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 26-09-2024.
 */

class Pipe2Configuration(
        nodeAddress: Int, nodeType: String, priority: Int, roomRef: String,
        floorRef: String, profileType: ProfileType, model: SeventyFiveFProfileDirective
) : HyperStatConfiguration(nodeAddress, nodeType, priority, roomRef, floorRef, profileType, model){
    override fun getActiveConfiguration(): HyperStatConfiguration {
        TODO("Not yet implemented")
    }

    override fun getDefaultConfiguration(): HyperStatConfiguration {
        TODO("Not yet implemented")
    }

    override fun getDependencies(): List<ValueConfig> {
        TODO("Not yet implemented")
    }
}