package a75f.io.domain.util

import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import com.google.gson.JsonParseException
import io.seventyfivef.domainmodeler.client.ModelDirective
import java.io.FileNotFoundException

/**
 * Created by Manjunath K on 31-08-2023.
 */
class ModelSource {
    companion object {

        private const val NEW_VERSION = "assets/assets/75f/versions.json"
        private const val MODEL_PATH = "assets/assets/75f/models/"

        fun getModelByProfileName(profileName: String): ModelDirective? {
            CcuLog.i(Domain.LOG_TAG, " getModelByProfileName $profileName")
            try {
                val modelId = getModelId(profileName)
                return ResourceHelper.loadModelDefinition("${MODEL_PATH}${modelId}.json")
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
            CcuLog.i(Domain.LOG_TAG, "Model load failed")
            return null
        }

        private fun getModelId(profileName: String): String? {
            var modelId: String? = null
            try {
                val data = ResourceHelper.getModelVersion(NEW_VERSION)
                data.keys().forEach { modelName ->
                    if (modelName!!.contentEquals(profileName)) {
                        val modelData = data.getJSONObject(modelName)
                        modelId = modelData.getString("id")
                    }
                }
            } catch (e: JsonParseException) {
                e.printStackTrace()
            }
            return modelId
        }
    }
}