package a75f.io.renatus.compose

/**
 * Created by Manjunath K on 14-07-2023.
 */

data class ModelMetaData(
    var id: String, val name: String, var description: String, var tagNames: List<String>
)
const val ID = "id"
const val NAME = "name"
const val DESCRIPTION = "description"
const val TAG_NAMES = "tagNames"
