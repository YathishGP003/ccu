package a75f.io.logic.bo.building.system.dab.config

import a75f.io.logic.bo.building.system.vav.config.AdvancedHybridAhuConfig
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 19-05-2024.
 */

class DabAdvancedHybridAhuConfig (private val cmModelDef: SeventyFiveFProfileDirective, private val connectModelDef: SeventyFiveFProfileDirective) : AdvancedHybridAhuConfig(cmModelDef, connectModelDef)  {
    // add if any specific configuration is required for VavAdvancedHybridAhuConfig
}
