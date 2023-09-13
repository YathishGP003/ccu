package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.util.ResourceHelper
import android.content.Context
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDiff
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import io.seventyfivef.domainmodeler.common.Version


/**
 * Created by Manjunath K on 16-06-2023.
 */

class DiffManger(var context: Context?) {


    companion object {
        const val ORIGINAL_FIle_PATH = "models/"
        const val NEW_FIle_PATH = "assets/75f/models/"
        const val NEW_VERSION = "assets/75f/versions.json"
        const val VERSION = "models/versions.json"
    }

    /**
     * starts scanning all the models
     * check the model version and find the diff and update the model definition
     */
    fun processModelMigration(siteRef: String) {
        val metaData : MutableList<ModelMeta> =  getModelFileVersionDetails(NEW_VERSION)
        val anyInvalidModels = ModelValidator.validateAllDomainModels(metaData)
        if (anyInvalidModels.isNotEmpty()) {
            anyInvalidModels.forEach {
                Log.i("DOMAIN_MODEL", "Invalid model definition $it: ")
                val invalidModel = metaData.find { model -> model.modelId == it }
                if(invalidModel != null) {
                    metaData.remove(invalidModel)
                }
            }
        }
        if (metaData.isEmpty()) {
            Log.e("DOMAIN_MODEL", "No valid model found for migration ")
            return
        }
        val migrationHandler = MigrationHandler(CCUHsApi.getInstance())
        val newVersionFiles = getModelFileVersionDetails(NEW_VERSION)
        val versionFiles = getModelFileVersionDetails(VERSION)
        updateEquipModels (newVersionFiles, versionFiles, migrationHandler, siteRef)
    }

    fun updateEquipModels(
        newVersionFiles: List<ModelMeta>,
        versionFiles: List<ModelMeta>,
        handler: MigrationHandler,
        siteRef : String
    ) {
        newVersionFiles.forEach { version ->
            val currentModelMeta = getCurrentModel(versionFiles, version.modelId)

            if (currentModelMeta != null) {
                if (isModelVersionUpdated(currentModelMeta.version, version.version)) {
                    val originalModel = getModelDetective("$ORIGINAL_FIle_PATH${version.modelId}.json")
                    val newModel = getModelDetective("$NEW_FIle_PATH${version.modelId}.json")

                    val entityConfiguration =
                        originalModel?.let { getDiffEntityConfiguration(it, newModel!!) }
                    if (entityConfiguration != null && newModel != null) {
                        handler.migrateModel(entityConfiguration,newModel, siteRef)
                    }
                }
            } else {
                // This version model is not found we need to add to current model
            }
        }
    }


    /**
     * Function reads an version files and returns all the model id's list
     *  @return List<String> all the models id's list from version file
     */
    fun getModelFileVersionDetails(fileName: String): MutableList<ModelMeta> {
        val versionDetails = ResourceHelper.getModelVersion(fileName)
        val models = mutableListOf<ModelMeta>()
        versionDetails.keys().forEach {
            val versionModel = versionDetails.getJSONObject(it as String)
            models.add(
                ModelMeta(modelId = it,
                Version(
                    major = versionModel.getInt(MAJOR),
                    minor = versionModel.getInt(MINOR),
                    patch = versionModel.getInt(PATCH)
                    )
                )
            )
        }
        return models
    }

    /**
     * function which compares the version between current and new model
     * @param currentList
     * @return Boolean
     */
    private fun isModelVersionUpdated(currentList: Version, newModel: Version): Boolean {
        return (currentList.major != newModel.major || currentList.minor != newModel.minor || currentList.patch != newModel.patch)
    }

    private fun getCurrentModel(currentList: List<ModelMeta>, modelId: String): ModelMeta? {
        return currentList.find { it.modelId == modelId }
    }

    /**
     * The function which detects the entity configuration to add,update,delete domains
     * if change in new model definition
     * @param original It is current version model definition
     * @param newModel It is new model definition
     * @return EntityConfiguration return details of configuration to add and delete and update
     */
    private fun getDiffEntityConfiguration(
        original: ModelDirective, newModel: ModelDirective
    ): EntityConfiguration {
        val diffFinder = DiffFinder()
        val diff = getDiff(original, newModel)
        val entityConfiguration = EntityConfiguration()
        diffFinder.findEquipUpdate(original.domainName, diff, entityConfiguration)
        diffFinder.findPointUpdate(diff, entityConfiguration)
        return entityConfiguration
    }

    private fun getDiff(original: ModelDirective, newModel: ModelDirective): ModelDiff{
        val diffFinder = DiffFinder()
        return diffFinder.calculateDiff(original, newModel)
    }

    /**
     * Function to get the model Definition from file
     * @return ModelDirective
     */
    private fun getModelDetective(modelFile: String): ModelDirective? {
        return ResourceHelper.loadModelDefinition(modelFile)
    }
}