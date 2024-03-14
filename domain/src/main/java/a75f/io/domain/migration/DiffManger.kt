package a75f.io.domain.migration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.util.ModelCache
import a75f.io.domain.util.ResourceHelper
import a75f.io.logger.CcuLog
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.seventyfivef.domainmodeler.client.ModelDiff
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.ModelDirectiveFactory
import io.seventyfivef.domainmodeler.common.Version
import org.json.JSONObject


/**
 * Created by Manjunath K on 16-06-2023.
 */

class DiffManger(var context: Context?) {


    companion object {
        const val NEW_FILE_PATH = "assets/assets/75f/models/"
        const val BACKUP_FIle_PATH = "assets/models/"
        const val VERSION = "versions.json"
        const val ASSETS_VERSION_FILE_PATH = "assets/assets/75f/versions.json"
        const val MODELS_VERSION_FILE_PATH = "assets/models/versions.json"
        lateinit var  migrationCompletedListener: OnMigrationCompletedListener
    }

    /**
     * starts scanning all the models
     * check the model version and find the diff and update the model definition
     */
    fun processModelMigration(siteRef: String, sharedPref: SharedPreferences?) {
        CcuLog.i(Domain.LOG_TAG, "processModelMigration")
        val newVersionFiles: MutableList<ModelMeta> =
            getModelFileVersionDetails(ASSETS_VERSION_FILE_PATH)
        CcuLog.i(
            Domain.LOG_TAG,
            " Found ${newVersionFiles.size} new models at $NEW_FILE_PATH$VERSION"
        )
        if (newVersionFiles.isEmpty()) {
            CcuLog.e(Domain.LOG_TAG, "No valid model found for migration ")
            return
        }
        val newFileIterator = newVersionFiles.iterator()
        val requiredModels = ModelValidator.getRequiredModels()
        val newModelMetaList: MutableList<ModelMeta> = mutableListOf()
        while (newFileIterator.hasNext()) {
            val item = newFileIterator.next()
            if (item.modelId in requiredModels) {
                newModelMetaList.add(item)
            }
        }
        val migrationHandler = MigrationHandler(CCUHsApi.getInstance(), migrationCompletedListener)

        val oldModelMetaList = if (isModelsSharedPrefAvailable(sharedPref)) {
            CcuLog.e(Domain.LOG_TAG, "fetching modelMeta from the shared preference")
            getModelsFromSharedPref(sharedPref)
        } else {
            CcuLog.e(Domain.LOG_TAG, "fetching modelMeta from the assets/models folder")
            getModelFileVersionDetails("$BACKUP_FIle_PATH$VERSION")
        }
        CcuLog.i(
            Domain.LOG_TAG,
            "old models present in sharedPref: ${oldModelMetaList.size}, currently used models count: ${newModelMetaList.size}"
        )
        updateEquipModels(newModelMetaList, oldModelMetaList, migrationHandler, siteRef, sharedPref)
    }

    fun updateEquipModels(
        newModelMetaList: List<ModelMeta>,
        oldModelMetaList: List<ModelMeta>,
        handler: MigrationHandler,
        siteRef: String,
        sharedPref: SharedPreferences?
    ) {
        newModelMetaList.forEach { assetsModelMeta ->
            val oldModelMeta = getCurrentModel(oldModelMetaList, assetsModelMeta.modelId)

            if (oldModelMeta != null) {
                CcuLog.i(Domain.LOG_TAG, "Currently used model meta: $oldModelMeta")
                if (isModelVersionUpdated(oldModelMeta.version, assetsModelMeta.version)) {
                    //fetching old model from the sharedPref
                    val oldModel = getModelDirectiveFromSf(assetsModelMeta.modelId, sharedPref)
                    Log.d(
                        Domain.LOG_TAG,
                        "sharedPref old Model: ${assetsModelMeta.modelId}: " + sharedPref?.getString(
                            assetsModelMeta.modelId,
                            null
                        )
                    )
                    // Retrieve new model from the modelCache(assets/new model)
                    val newModel = ModelCache.getModelById(assetsModelMeta.modelId)
                    // Retrieve the current equip map by using modelID
                    val currentEquipMap = Domain.readEquip(assetsModelMeta.modelId);
                    // Ensure that the current model JSON and new model JSON are not null
                    if (oldModel != null && newModel != null
                        && (currentEquipMap["modelVersion"] != null || currentEquipMap["sourceModelVersion"] != null)
                    ) {
                        // if block should be removed once modelVersion key migrated completely to sourceModelVersion
                        if (currentEquipMap.containsKey("modelVersion") &&
                            (assetsModelMeta.version.toString() != (currentEquipMap["modelVersion"]).toString())
                        ) {
                            CcuLog.i(
                                Domain.LOG_TAG,
                                "Comparing new model version: ${assetsModelMeta.version}," +
                                        " current equipment version: ${currentEquipMap["modelVersion"]}"
                            )
                            val entityConfiguration =
                                getDiffEntityConfiguration(oldModel, newModel)
                            handler.migrateModel(
                                entityConfiguration,
                                oldModel,
                                newModel,
                                siteRef
                            )
                        } else if (currentEquipMap.containsKey("sourceModelVersion") &&
                            (assetsModelMeta.version.toString() != (currentEquipMap["sourceModelVersion"]).toString())
                        ) {
                            CcuLog.i(
                                Domain.LOG_TAG,
                                "Comparing new model version: ${assetsModelMeta.version}," +
                                        " current equipment version: ${currentEquipMap["sourceModelVersion"]}"
                            )
                            val entityConfiguration =
                                getDiffEntityConfiguration(oldModel, newModel)
                            handler.migrateModel(
                                entityConfiguration,
                                oldModel,
                                newModel,
                                siteRef
                            )
                        } else {
                            CcuLog.i(
                                Domain.LOG_TAG, "Migration skipped: new model version " +
                                        "${assetsModelMeta.version}, current model in use: $currentEquipMap")
                        }
                    } else {
                        CcuLog.i(
                            Domain.LOG_TAG,
                            "Model not updated; backup model: ${currentEquipMap}, New model: $newModel and currentEquipMap: $currentEquipMap"
                        )
                    }
                } else {
                    CcuLog.i(
                        Domain.LOG_TAG,
                        "The model with ID ${assetsModelMeta.modelId} is not updated, " +
                                "as the current model version (${oldModelMeta.version})" +
                                " is the same as the new model version (${assetsModelMeta.version})."
                    )
                }
            } else {
                CcuLog.i(Domain.LOG_TAG, "Model not found for ${assetsModelMeta.modelId}")
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
            val versionModel = versionDetails.getJSONObject(it)
            models.add(
                ModelMeta(
                    modelId = versionModel.getString(ID),
                    Version(
                        major = versionModel.getJSONObject(MODEL_VERSION).getInt(MAJOR),
                        minor = versionModel.getJSONObject(MODEL_VERSION).getInt(MINOR),
                        patch = versionModel.getJSONObject(MODEL_VERSION).getInt(PATCH)
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

    private fun getDiff(original: ModelDirective, newModel: ModelDirective): ModelDiff {
        val diffFinder = DiffFinder()
        return diffFinder.calculateDiff(original, newModel)
    }

    /**
     * Function to get the model Definition from file
     * @return ModelDirective
     */
    private fun getModelDirective(modelFile: String): ModelDirective? {
        return ResourceHelper.loadModelDefinition(modelFile)
    }

    interface OnMigrationCompletedListener {
        fun onMigrationCompletedCompleted(hsApi: CCUHsApi)
    }

    fun registerOnMigrationCompletedListener(listener: OnMigrationCompletedListener) {
        migrationCompletedListener = listener
    }

    fun saveModelsInSharedPref(sharedPref: SharedPreferences) {
        val versionDetails = ResourceHelper.getModelVersion(ASSETS_VERSION_FILE_PATH)
        sharedPref.edit().putString("modelsVersion", versionDetails.toString()).apply()

        versionDetails.keys().forEach {
            val modelId = versionDetails.getJSONObject(it).get("id").toString();
            val modelData: String? = ResourceHelper.loadString("$NEW_FILE_PATH$modelId.json")
            CcuLog.e(Domain.LOG_TAG, "modelId: $modelId, modelData: $modelData")
            sharedPref.edit().putString(modelId, modelData).apply()
        }
    }

    private fun isModelsSharedPrefAvailable(sharedPref: SharedPreferences?): Boolean {
        return sharedPref!!.getString("modelsVersion", null) != null
    }

    private fun getModelsFromSharedPref(sharedPref: SharedPreferences?): MutableList<ModelMeta> {
        val versionDetails = sharedPref?.getString("modelsVersion", null)?.let { JSONObject(it) }
        val models = mutableListOf<ModelMeta>()
        versionDetails?.keys()?.forEach {
            val versionModel = versionDetails.getJSONObject(it)
            models.add(
                ModelMeta(
                    modelId = versionModel.getString(ID),
                    Version(
                        major = versionModel.getJSONObject(MODEL_VERSION).getInt(MAJOR),
                        minor = versionModel.getJSONObject(MODEL_VERSION).getInt(MINOR),
                        patch = versionModel.getJSONObject(MODEL_VERSION).getInt(PATCH)
                    )
                )
            )
        }
        return models
    }

    private fun getModelDirectiveFromSf(
        modelId: String,
        sharedPref: SharedPreferences?
    ): ModelDirective? {
        val modelData = sharedPref?.getString(modelId, null);
        if (modelData.isNullOrEmpty())
            return null
        val modelDirectiveFactory = ModelDirectiveFactory(ResourceHelper.getObjectMapper())
        return modelDirectiveFactory.fromJson(modelData)
    }

}