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
    private const val BUILDING_EQUIP_MODEL = "assets/75f/models/657739fbd1743f797c4c2ca4.json"

    var context : Context? = null
    fun init(hayStack : CCUHsApi, context: Context) {
        this.context = context
        if (!hayStack.isCCURegistered) {
            CcuLog.i(Domain.LOG_TAG, "Load BuildingEquipModel")
            val buildingEquipModel = ResourceHelper.loadModel(BUILDING_EQUIP_MODEL, context)
            modelContainer[MODEL_BUILDING_EQUIP] = buildingEquipModel
            CcuLog.i(Domain.LOG_TAG, "BuildingEquipModel loaded ${buildingEquipModel.name}")
        }

        loadDeviceModels()
        loadVavZoneEquipModels()

    }

    private fun loadDeviceModels() {
        modelContainer[MODEL_SMART_NODE_DEVICE] = getModelById(MODEL_SMART_NODE_DEVICE)
        CcuLog.i(Domain.LOG_TAG, "smartNodeDevice loaded")

        modelContainer[MODEL_HELIO_NODE_DEVICE] = getModelById(MODEL_HELIO_NODE_DEVICE)
        CcuLog.i(Domain.LOG_TAG, "helioNodeDevice loaded")
    }
    
    private fun loadVavZoneEquipModels() {
        modelContainer[MODEL_SN_VAV_NO_FAN] = getModelById(MODEL_SN_VAV_NO_FAN)
        CcuLog.i(Domain.LOG_TAG, "smartNodeVavReheatNoFan equip model loaded")

        modelContainer[MODEL_SN_VAV_SERIES_FAN] = getModelById(MODEL_SN_VAV_SERIES_FAN)
        CcuLog.i(Domain.LOG_TAG, "smartNodeVavReheatSeriesFan equip model loaded")

        modelContainer[MODEL_SN_VAV_PARALLEL_FAN] = getModelById(MODEL_SN_VAV_PARALLEL_FAN)
        CcuLog.i(Domain.LOG_TAG, "smartNodeVavReheatParallelFan equip model loaded")

        modelContainer[MODEL_SN_VAV_ACB] = getModelById(MODEL_SN_VAV_ACB)
        CcuLog.i(Domain.LOG_TAG, "smartNodeActiveChilledBeam equip model loaded")

        modelContainer[MODEL_HN_VAV_NO_FAN] = getModelById(MODEL_HN_VAV_NO_FAN)
        CcuLog.i(Domain.LOG_TAG, "helioNodeVavReheatNoFan equip model loaded")

        modelContainer[MODEL_HN_VAV_SERIES_FAN] = getModelById(MODEL_HN_VAV_SERIES_FAN)
        CcuLog.i(Domain.LOG_TAG, "helioNodeVavReheatSeriesFan equip model loaded")

        modelContainer[MODEL_HN_VAV_PARALLEL_FAN] = getModelById(MODEL_HN_VAV_PARALLEL_FAN)
        CcuLog.i(Domain.LOG_TAG, "helioNodeVavReheatParallelFan equip model loaded")

        modelContainer[MODEL_HN_VAV_ACB] = getModelById(MODEL_HN_VAV_ACB)
        CcuLog.i(Domain.LOG_TAG, "helioNodeActiveChilledBeam equip model loaded")
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