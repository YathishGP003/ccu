package a75f.io.renatus.compose


/**
 * Created by Manjunath K on 14-07-2023.
 */

data class ModelMetaData(
    var id: String, val name: String, var description: String, var tagNames: List<String>, var version: String, var registerCount : Int,
)
const val ID = "id"
const val NAME = "name"
const val DESCRIPTION = "description"
const val TAG_NAMES = "tagNames"
const val VERSION = "version"
const val MAJOR = "major"
const val MINOR = "minor"
const val PATCH = "patch"
const val NUM_OF_REGISTERS = "numberOfRegisters"