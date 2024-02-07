package a75f.io.domain.migration

import io.seventyfivef.domainmodeler.common.Version

/**
 * Created by Manjunath K on 23-06-2023.
 */

data class ModelMeta(val modelId: String,val version: Version)
const val MAJOR = "major"
const val MINOR = "minor"
const val PATCH = "patch"
const val ID = "id"
const val MODEL_VERSION = "version"