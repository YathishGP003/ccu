package a75f.io.domain.migration

import a75f.io.domain.api.Domain
import a75f.io.domain.util.ResourceHelper
import a75f.io.logger.CcuLog
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFEquipDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective

/**
 * Created by Manjunath K on 23-06-2023.
 */

class ModelValidator {
    companion object {
        /**
         * Validate all the domain models
         */
        fun validateAllDomainModels(metaDetails: List<ModelMeta>, path: String): List<String> {
            val inValidModels = mutableListOf<String>()
            /*   val newVersionFiles = diffManger.getModelFileVersionDetails(DiffManger.NEW_VERSION)
           // TODO I think no need to validate the current models.
           // val versionFiles = diffManger.getModelFileVersionDetails(DiffManger.VERSION)*/
            metaDetails.forEach {
                val isValidDefinition = isValidaModel(it.modelId, path)
                if (!isValidDefinition) {
                    inValidModels.add(it.modelId)
                    Log.i(Domain.LOG_TAG, "Invalid Model "+it.modelId)
                }
            }
            return inValidModels
        }

        fun isValidaModel(modelId: String, path: String): Boolean {
            try {
                val originalModel = getModelDirective("$path$modelId.json")

                if (originalModel != null) {
                    when (originalModel) {
                        is SeventyFiveFProfileDirective -> { logIt(originalModel.javaClass.name,originalModel.id)}
                        is SeventyFiveFDeviceDirective -> { logIt(originalModel.javaClass.name,originalModel.id)}
                        is SeventyFiveFTunerDirective -> { logIt(originalModel.javaClass.name,originalModel.id)}
                        is SeventyFiveFEquipDirective -> { logIt(originalModel.javaClass.name,originalModel.id)}
                    }
                    return true
                }
            } catch (e: Exception) {
                println("Error while loading ${e.localizedMessage}")
                e.toString()
            }
            return false
        }

        /**
         * Function to get the model Definition from file
         * @return ModelDirective
         */
        private fun getModelDirective(modelFile: String): ModelDirective? {
            return ResourceHelper.loadModelDefinition(modelFile)
        }

        private fun logIt(modelType: String, id: String){
            CcuLog.i (Domain.LOG_TAG, " $modelType Valid Model found. Model Id : $id")
        }
    }
}