package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import android.content.Context
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective

/**
 * Reading of Model through ModelLoder will have the model cached at first read.
 */
object ModelLoader {

    private const val MODEL_BUILDING_EQUIP = "64abb6a3a97798751b2bda14"
    private const val MODEL_SN_VAV_NO_FAN = "nickTestSmartNodeVAVReheatNoFan_v0.0.1"
    private const val MODEL_SN_VAV_SERIES_FAN = "smartnodeVAVReheatSeriesFan_v0.0.6";//""65198813951f37007b7956e9_Vav_Series_Sam"
    private const val MODEL_SN_VAV_PARALLEL_FAN = "smartnodeVAVReheatParallelFan_v0.0.5";//"6519a8e5951f37007b7956ea_Vav_Parallel_Sam"
    private const val MODEL_HN_VAV_NO_FAN = "6519b3e1951f37007b7956eb"
    private const val MODEL_HN_VAV_SERIES_FAN = "6519b451951f37007b7956ec"
    private const val MODEL_HN_VAV_PARALLEL_FAN = "6519b4d6951f37007b7956ed"

    private const val MODEL_SMART_NODE_DEVICE = "64e258c5cb3df279a3608efa"
    private const val MODEL_HELIO_NODE_DEVICE = "64e32aa2cb3df279a3608efc"
    fun getBuildingEquipModelDef(context: Context? = null) : ModelDirective {
        return ModelCache.getModelById(MODEL_BUILDING_EQUIP)
    }

    fun getSmartNodeVavNoFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_NO_FAN)
    }

    fun getSmartNodeVavSeriesModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_SERIES_FAN)
    }

    fun getSmartNodeVavParallelFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_PARALLEL_FAN)
    }

    fun getSmartNodeDevice() : ModelDirective {
        return ModelCache.getModelById(MODEL_SMART_NODE_DEVICE)
    }

    fun getHelioNodeVavNoFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_NO_FAN)
    }

    fun getHelioNodeVavSeriesModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_SERIES_FAN)
    }

    fun getHelioNodeVavParallelFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_PARALLEL_FAN)
    }

    fun getHelioNodeDevice() : ModelDirective {
        return ModelCache.getModelById(MODEL_HELIO_NODE_DEVICE)
    }
}