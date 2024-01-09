package a75f.io.domain.logic

import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.domain.config.ProfileConfiguration
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelPointDef

interface EquipBuilder {
    fun buildEquip(equipConfig : EquipBuilderConfig) : Equip
    fun buildPoint(pointConfig : PointBuilderConfig) : Point
}

