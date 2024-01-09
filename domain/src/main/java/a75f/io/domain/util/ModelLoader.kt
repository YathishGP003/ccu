package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import android.content.Context
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective

object ModelLoader {

    private const val MODEL_BUILDING_EQUIP = "657739fbd1743f797c4c2ca4"
    private const val MODEL_VAV_NO_FAN = "64abb6a3a97798751b2bda14"

    fun getBuildingEquipModelDef(context: Context? = null) : ModelDirective {
        return ModelCache.getModelById(MODEL_BUILDING_EQUIP)
    }

    fun geVavNoFanModelDef() : ModelDirective {
        return ModelCache.getModelById(MODEL_BUILDING_EQUIP)
    }
}