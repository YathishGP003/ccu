package a75f.io.domain.migration

import a75f.io.domain.api.Domain
import a75f.io.domain.util.*
import a75f.io.domain.util.ResourceHelper
import a75f.io.logger.CcuLog
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
            val requiredModels = getRequiredModels()
            requiredModels.forEach {
                val isValidDefinition = isValidaModel(it, path)
                if (!isValidDefinition) {
                    inValidModels.add(it)
                    CcuLog.i(Domain.LOG_TAG, "Invalid Model $it")
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

         fun getRequiredModels(): List<String> {
            return listOf(
                MODEL_BUILDING_EQUIP,
                MODEL_SN_VAV_NO_FAN,
                MODEL_SN_VAV_SERIES_FAN,
                MODEL_SN_VAV_PARALLEL_FAN,
                MODEL_HN_VAV_NO_FAN,
                MODEL_HN_VAV_SERIES_FAN,
                MODEL_HN_VAV_PARALLEL_FAN,
                MODEL_SN_VAV_ACB,
                MODEL_HN_VAV_ACB,
                MODEL_SN_BYPASS_DAMPER,
                MODEL_SMART_NODE_DEVICE,
                MODEL_HELIO_NODE_DEVICE,
                MODEL_EXTERNAL_AHU_DAB,
                MODEL_EXTERNAL_AHU_VAV,
                MODEL_VAV_STAGED_RTU,
                MODEL_VAV_STAGED_VFD_RTU,
                MODEL_VAV_MODULATING_AHU,
                MODEL_VAV_ADVANCED_AHU_V2_CM,
                MODEL_VAV_ADVANCED_AHU_V2_CONNECT,
            )
        }
    }
}