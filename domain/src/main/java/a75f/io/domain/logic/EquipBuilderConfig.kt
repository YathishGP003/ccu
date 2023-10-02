package a75f.io.domain.logic

import a75f.io.domain.config.ProfileConfiguration
import io.seventyfivef.domainmodeler.client.ModelDirective

data class EquipBuilderConfig(val modelDef: ModelDirective, val profileConfiguration: ProfileConfiguration?, val siteRef :
String, val tz : String? = null, val disPrefix : String = "")
