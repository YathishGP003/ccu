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
    private const val MODEL_VAV_NO_FAN = "nickTestSmartNodeVAVReheatNoFan_v0.0.1"
    private const val MODEL_SMART_NODE_DEVICE = "64e258c5cb3df279a3608efa"
    fun getBuildingEquipModelDef(context: Context? = null) : ModelDirective {
        return ModelCache.getModelById(MODEL_BUILDING_EQUIP)
    }

    fun geVavNoFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_VAV_NO_FAN)
    }

    fun getSmartNodeDevice() : ModelDirective {
        return ModelCache.getModelById(MODEL_SMART_NODE_DEVICE)
    }
}