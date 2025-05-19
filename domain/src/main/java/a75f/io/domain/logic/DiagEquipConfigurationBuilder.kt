package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import org.projecthaystack.HDateTime

class DiagEquipConfigurationBuilder(private val hayStack : CCUHsApi): DefaultEquipBuilder() {
    fun createDiagEquipAndPoints(ccuName : String): String {
        val siteRef = hayStack.siteIdRef.toString()
        val diagEquipModelDef = ModelLoader.getDiagEquipModel()
        val hayStackEquip = getDiagEquip(ccuName)
        val diagEquipId = hayStack.addEquip(hayStackEquip)
        createPoints(diagEquipModelDef, diagEquipId, siteRef, true)
        DomainManager.addDiagEquip(hayStack)
        return diagEquipId
    }

    fun updateDiagGatewayRef(systemEquipId: String) {
        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "DiagEquipConfigurationBuilder----updateDiagGatewayRef----")
        val ccuName = Domain.ccuDevice.ccuDisName
        val hayStackEquip = getDiagEquip(ccuName)
        val diagEquipId = Domain.diagEquip.getId()
        hayStackEquip.gatewayRef = systemEquipId
        hayStackEquip.ahuRef = systemEquipId
        hayStackEquip.lastModifiedDateTime = HDateTime.make(System.currentTimeMillis())
        hayStackEquip.id = diagEquipId
        hayStack.updateEquip(hayStackEquip, diagEquipId)
    }
    private fun getDiagEquip(ccuName: String): Equip {
        val siteRef = hayStack.siteIdRef.toString()
        val tz = hayStack.timeZone
        val diagEquipModelDef = ModelLoader.getDiagEquipModel()
        return buildEquip(EquipBuilderConfig(diagEquipModelDef, null, siteRef,tz,
            "$ccuName-${diagEquipModelDef.name}")
        )
    }
}