package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFTunerDirective
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import org.projecthaystack.HStr

class TunerEquipBuilder(private val hayStack : CCUHsApi) : DefaultEquipBuilder() {

    fun buildTunerEquipAndPoints(modelDef: SeventyFiveFTunerDirective): String {
        val hayStackEquip = buildEquip(modelDef, null)
        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, equipId)
        return equipId
    }

    private fun createPoints(modelDef: SeventyFiveFTunerDirective, equipRef: String) {

        modelDef.points.forEach {
            val hayStackPoint = buildPoint(it, null, equipRef)
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            hayStack.writeDefaultTunerValById(pointId, it.defaultValue.toString().toDouble())
            DomainManager.addPoint(hayStackPoint)
        }
    }

}