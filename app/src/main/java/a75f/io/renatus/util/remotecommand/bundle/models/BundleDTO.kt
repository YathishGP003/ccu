package a75f.io.renatus.util.remotecommand.bundle.models

import com.google.gson.JsonObject

data class BundleDTO(val obj: JsonObject) {
    val bundleId: String
    val bundleName: String
    val CCUArtifact: ArtifactDTO?
    val BACArtifact: ArtifactDTO?
    val RemoteArtifact: ArtifactDTO?
    val HomeArtifact: ArtifactDTO?

    companion object {
        fun getArtifact(obj: JsonObject, key: String): ArtifactDTO? {
            if (obj.has(key)) {
                val artifactObj = obj.get(key)
                if (!artifactObj.isJsonNull) {
                    return ArtifactDTO(artifactObj.asJsonObject)
                }
            }
            return null
        }
    }

    init {
        bundleId = obj.get("id").asString
        bundleName = obj.get("name").asString
        CCUArtifact = getArtifact(obj, "ccuArtifact")
        BACArtifact = getArtifact(obj, "bacArtifact")
        RemoteArtifact = getArtifact(obj, "remoteArtifact")
        HomeArtifact = getArtifact(obj, "homeArtifact")
    }
}