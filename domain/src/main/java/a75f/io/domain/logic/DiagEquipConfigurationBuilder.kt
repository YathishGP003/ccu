package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.projecthaystack.HDateTime

class DiagEquipConfigurationBuilder(private val hayStack : CCUHsApi): DefaultEquipBuilder() {
    fun createDiagEquipAndPoints(ccuName : String, migrationVersion : String?): String {
        val diagEquip = hayStack.readEntityByDomainName(DomainName.diagEquip)
        if (diagEquip.isNotEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "Diag Equip already exists, skipping creation")
            return diagEquip[Tags.ID].toString()
        }
        val siteRef = hayStack.siteIdRef.toString()
        val hayStackEquip = getDiagEquip(ccuName)
        val diagEquipId = hayStack.addEquip(hayStackEquip)

        CoroutineScope(Dispatchers.IO).launch {
            val diagEquipModelDef = ModelLoader.getDiagEquipModel()
            createPoints(diagEquipModelDef, diagEquipId, siteRef, true)
            DomainManager.addDiagEquip(hayStack)
            if(migrationVersion != null) {
                Domain.getDomainDiagEquip()?.migrationVersion?.writeDefaultVal(migrationVersion)
                CcuLog.i(Domain.LOG_TAG, "Diag Equip Migration Version set to $migrationVersion")
            }
        }
        return diagEquipId
    }

    fun updateDiagGatewayRef(systemEquipId: String) {
        CcuLog.d(Tags.ADD_REMOVE_PROFILE, "DiagEquipConfigurationBuilder----updateDiagGatewayRef----")
        val ccuName = Domain.ccuDevice.ccuDisName
        val hayStackEquip = getDiagEquip(ccuName)
        val diagEquipId = hayStack.readEntityByDomainName(DomainName.diagEquip)[Tags.ID].toString()
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