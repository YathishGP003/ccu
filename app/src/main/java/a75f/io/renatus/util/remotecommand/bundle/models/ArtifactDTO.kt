package a75f.io.renatus.util.remotecommand.bundle.models

import com.google.gson.JsonObject

class ArtifactDTO(private val obj: JsonObject) {
    val id: String
    val target: String
    val version: String
    val minVersion: String
    val fileName: String
    val fileSize : String

    init {
        id = obj.get("id").asString
        target = obj.get("target").asString
        version = obj.get("versionString").asString
        minVersion = obj.get("minRequiredVersionString").asString
        fileName = obj.get("filename").asString
        fileSize = obj.get("fileSizeMB").asString
    }
}