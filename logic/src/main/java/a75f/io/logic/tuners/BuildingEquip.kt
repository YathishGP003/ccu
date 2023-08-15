package a75f.io.logic.tuners

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.logic.TunerEquipBuilder
import a75f.io.domain.util.ModelLoader

object BuildingEquip {
    fun initialize(haystack : CCUHsApi) {
        val tunerEquip: HashMap<Any, Any> = haystack.readEntity("equip and tuner")
        //Building Equip does not exist, create new one.
        if (tunerEquip.isEmpty()) {
            val equipBuilder = TunerEquipBuilder(haystack)
            equipBuilder.buildTunerEquipAndPoints(ModelLoader.getBuildingEquipModelDef())
        }
    }
}