package a75f.io.domain.logic

import a75f.io.domain.config.ProfileConfiguration
import io.seventyfivef.domainmodeler.client.ModelPointDef

data class PointBuilderConfig(val modelDef: ModelPointDef, val configuration: ProfileConfiguration?, val equipRef : String,
                              val siteRef : String, val tz : String? = null, val disPrefix : String = "")
