package a75f.io.logic.migration.ccuanddiagequipmigration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.cutover.DiagEquipMapping
import a75f.io.domain.logic.DiagEquipConfigurationBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.getMigrationVersion
import io.seventyfivef.ph.core.Tags

class DiagEquipMigrationHandler {
    fun doDiagEquipMigration(hayStack: CCUHsApi, excludeExternalModbusQuery: String) {
        val diagEquip = hayStack.readEntity("diag and equip and $excludeExternalModbusQuery")
        if (diagEquip.isEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "No Diag and equip found so creating now")
            val ccu = hayStack.ccuName
            if(ccu == null){
                CcuLog.i(Domain.LOG_TAG, "Diag equip is not created as ccu name is null")
                return
            }
            val diagEquipConfigurationBuilder =  DiagEquipConfigurationBuilder(hayStack)

            val diagEquipId = diagEquipConfigurationBuilder.createDiagEquipAndPoints(ccu, getMigrationVersion())
            DomainManager.addDiagEquip(hayStack)
            CcuLog.i(Domain.LOG_TAG, "Diag equip created with id $diagEquipId")
            return
        }
        if (diagEquip.containsKey(a75f.io.api.haystack.Tags.DOMAIN_NAME)) {
            CcuLog.i(Domain.LOG_TAG, "diag equip already migrated")
            return
        }
        val diagEquipModel = ModelLoader.getDiagEquipModel()
        val equipDis = "${hayStack.ccuName}-${diagEquipModel.name}"
        val equipBuilder = ProfileEquipBuilder(hayStack)
        val equipId = diagEquip[Tags.ID].toString()

        equipBuilder.doCutOverMigration(
            equipId, diagEquipModel,
            equipDis, DiagEquipMapping.entries, null, isSystem = false, diagEquip
        )
        DomainManager.addDiagEquip(hayStack)
    }
}