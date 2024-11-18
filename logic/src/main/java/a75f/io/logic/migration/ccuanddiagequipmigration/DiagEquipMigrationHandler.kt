package a75f.io.logic.migration.ccuanddiagequipmigration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.cutover.DiagEquipMapping
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import io.seventyfivef.ph.core.Tags

class DiagEquipMigrationHandler {
    fun doDiagEquipMigration(hayStack: CCUHsApi) {
        val diagEquip = hayStack.readEntity("diag and equip")
        if (diagEquip.isEmpty()) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "No diag and equip found")
            return
        }
        if (diagEquip.containsKey(a75f.io.api.haystack.Tags.DOMAIN_NAME)) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "diagEquip already migrated")
            return
        }
        val diagEquipModel = ModelLoader.getDiagEquipModel()
        val equipDis = "${hayStack.ccuName}-${diagEquipModel.name}"
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId = diagEquip[Tags.ID].toString()

        equipBuilder.doCutOverMigration(
            equipId, diagEquipModel,
            equipDis, DiagEquipMapping.entries, null, isSystem = false
        )
        DomainManager.addDiagEquip(hayStack)

    }

}