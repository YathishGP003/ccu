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

    private const val MODEL_BUILDING_EQUIP = "657739fbd1743f797c4c2ca4"
    private const val MODEL_SN_VAV_NO_FAN = "65193ee6951f37007b7956e8"
    private const val MODEL_SN_VAV_SERIES_FAN = "65198813951f37007b7956e9"
    private const val MODEL_SN_VAV_PARALLEL_FAN = "6519a8e5951f37007b7956ea"
    private const val MODEL_HN_VAV_NO_FAN = "6519b3e1951f37007b7956eb"
    private const val MODEL_HN_VAV_SERIES_FAN = "6519b451951f37007b7956ec"
    private const val MODEL_HN_VAV_PARALLEL_FAN = "6519b4d6951f37007b7956ed"
    private const val MODEL_SN_VAV_ACB = "651d53a3951f37007b795703"
    private const val MODEL_HN_VAV_ACB = "651d8330951f37007b795706"

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