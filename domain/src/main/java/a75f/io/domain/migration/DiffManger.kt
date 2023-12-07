package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.util.ResourceHelper
import a75f.io.logger.CcuLog
import android.content.Context
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDiff
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.common.Version


/**
 * Created by Manjunath K on 16-06-2023.
 */

class DiffManger(var context: Context?) {


    companion object {
        const val NEW_FILE_PATH = "assets/75f/models/"
        const val BACKUP_FIle_PATH = "models/assets/75f/models/"
        const val VERSION = "versions.json"
    }

    /**
     * starts scanning all the models
     * check the model version and find the diff and update the model definition
     */
    fun processModelMigration(siteRef: String) {
        Log.i(Domain.LOG_TAG, "processModelMigration")
        val newVersionFiles : MutableList<ModelMeta> =  getModelFileVersionDetails("$NEW_FILE_PATH$VERSION")
        Log.i(Domain.LOG_TAG, " Found ${newVersionFiles.size} models at $NEW_FILE_PATH$VERSION")
        val anyInvalidModels = ModelValidator.validateAllDomainModels(newVersionFiles)
        Log.i(Domain.LOG_TAG, " Found ${anyInvalidModels.size} models invalid ")

        if (anyInvalidModels.isNotEmpty()) {
            anyInvalidModels.forEach {
                Log.i(Domain.LOG_TAG, "Invalid model definition $it: ")
                val invalidModel = newVersionFiles.find { model -> model.modelId == it }
                if(invalidModel != null) {
                    newVersionFiles.remove(invalidModel)
                }
            }
        }
        if (newVersionFiles.isEmpty()) {
            Log.e(Domain.LOG_TAG, "No valid model found for migration ")
            return
        }
        val migrationHandler = MigrationHandler(CCUHsApi.getInstance())
        val versionFiles = getModelFileVersionDetails("$BACKUP_FIle_PATH$VERSION")
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
                CcuLog.i(Domain.LOG_TAG, " currentModelMeta $currentModelMeta")
                if (isModelVersionUpdated(currentModelMeta.version, version.version)) {
                    CcuLog.i(Domain.LOG_TAG, " Model Updated  ${version.modelId}")
                    val originalModel = getModelDetective("$BACKUP_FIle_PATH${version.modelId}.json")
                    val newModel = getModelDetective("$NEW_FILE_PATH${version.modelId}.json")

                    val entityConfiguration =
                        originalModel?.let { getDiffEntityConfiguration(it, newModel!!) }
                    if (entityConfiguration != null && newModel != null) {
                        handler.migrateModel(entityConfiguration,newModel, siteRef,
                            getProfileNameByDomainName()
                        )
                    }
                }
            } else {
                // This version model is not found we need to add to current model
            }
        }
    }

    private fun getProfileNameByDomainName(): String {
        /**
         * TODO implementation function to fund the profile name by domain name using existing equip details
         */
        return ""
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