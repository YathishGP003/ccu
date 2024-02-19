package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import android.content.Context
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import java.lang.IllegalStateException

/**
 * Reading of Model through ModelLoader will have the model cached after first read.
 */
object ModelLoader {

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

    fun getSmartNodeVavAcbModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_SN_VAV_ACB)
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

    fun getHelioNodeVavAcbModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_HN_VAV_ACB)
    }

    fun getHelioNodeDevice() : ModelDirective {
        return ModelCache.getModelById(MODEL_HELIO_NODE_DEVICE)
    }

    fun getModelForDomainName( domainName : String) : ModelDirective {
        return when(domainName) {
            "smartnodeVAVReheatNoFan" -> getSmartNodeVavNoFanModelDef()
            "smartnodeVAVReheatSeriesFan" -> getSmartNodeVavSeriesModelDef()
            "smartnodeVAVReheatParallelFan" -> getSmartNodeVavParallelFanModelDef()
            "smartnodeActiveChilledBeam" -> getSmartNodeVavAcbModelDef()
            "helionodeVAVReheatNoFan" -> getHelioNodeVavNoFanModelDef()
            "helionodeVAVReheatSeriesFan" -> getHelioNodeVavSeriesModelDef()
            "helionodeVAVReheatParallelFan" -> getHelioNodeVavParallelFanModelDef()
            "helionodeActiveChilledBeam" -> getHelioNodeVavAcbModelDef()
            else -> throw IllegalStateException("Invalid Model Name")
        }
    }
}