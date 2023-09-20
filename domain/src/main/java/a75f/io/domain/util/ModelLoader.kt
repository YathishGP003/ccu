package a75f.io.domain.util

import android.content.Context
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective

object ModelLoader {

    private const val BUILDING_EQUIP_MODEL = "assets/75f/models/64abb6a3a97798751b2bda14.json"

    fun getBuildingEquipModelDef(context: Context? = null) : ModelDirective {
        return if (context != null) {
            ResourceHelper.loadModel(BUILDING_EQUIP_MODEL, context) as SeventyFiveFTunerDirective
        } else {
            ResourceHelper.loadModel(BUILDING_EQUIP_MODEL) as SeventyFiveFTunerDirective
        }
    }
}