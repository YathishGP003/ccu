package a75f.io.domain.logic

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.domain.config.ProfileConfiguration
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef

interface EquipBuilder {
    fun buildEquip(modelDef: ModelDirective, profileConfiguration: ProfileConfiguration?, siteRef : String) : Equip
    fun buildPoint(modelDef: ModelPointDef, configuration: ProfileConfiguration?, equipRef : String, siteRef : String) : Point
}