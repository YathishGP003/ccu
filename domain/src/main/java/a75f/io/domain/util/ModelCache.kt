package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import android.annotation.SuppressLint
import android.content.Context
import io.seventyfivef.domainmodeler.client.ModelDirective

@SuppressLint("StaticFieldLeak")
object ModelCache {
    private val modelContainer = mutableMapOf<String, ModelDirective>()

    private const val MODEL_ASSET_PREFIX = "assets/75f/models/"
    private const val BUILDING_EQUIP_MODEL = "assets/75f/models/64abb6a3a97798751b2bda14.json"

    var context : Context? = null
    fun init(hayStack : CCUHsApi, context: Context) {
        this.context = context
        if (!hayStack.isCCURegistered) {
            val buildingEquipModel = ResourceHelper.loadModel(BUILDING_EQUIP_MODEL, context)
            CcuLog.i(Domain.LOG_TAG, "BuildingEquipModel loaded ${buildingEquipModel.name}")
            modelContainer["64abb6a3a97798751b2bda14"] = buildingEquipModel
        }
    }

    /**
     * Could directly used in Unit tests without calling init() to set the context.
     */
    fun getModelById( modelId : String) : ModelDirective {
        CcuLog.i(Domain.LOG_TAG, "getModelById $modelId")
        var model = modelContainer[modelId]
        if (model != null) {
            CcuLog.i(Domain.LOG_TAG, "Model Loaded from Cache ${model.name}")
            return model
        }

        model = if (context != null) {
            ResourceHelper.loadModel("$MODEL_ASSET_PREFIX$modelId.json", context!!)
        } else {
            ResourceHelper.loadModel("$MODEL_ASSET_PREFIX$modelId.json")
        }
        CcuLog.i(Domain.LOG_TAG, "Model Loaded from FS ${model.name}")
        modelContainer[modelId] = model
        return model
    }
 }