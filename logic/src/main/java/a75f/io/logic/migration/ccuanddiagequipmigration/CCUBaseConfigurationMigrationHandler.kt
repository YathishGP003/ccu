package a75f.io.logic.migration.ccuanddiagequipmigration

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.CCUBaseConfigurationBuilder
import a75f.io.domain.logic.CCUEquipConfiguration
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.ph.core.Tags

class CCUBaseConfigurationMigrationHandler {
    fun doCCUBaseConfigurationMigration(hayStack: CCUHsApi) {
        val ccuModel = ModelLoader.getCCUBaseConfigurationModel()

        val ccuEquip = hayStack.readEntityByDomainName(DomainName.ccuConfiguration)
        val ccuDevice = hayStack.readEntity("ccu and device")
        if (ccuEquip.isNotEmpty()) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "CCU Equip already exists, " +
                    "so CCU Config Migration Not required")
            return
        }
        if (ccuDevice.isEmpty()) {
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "CCU Device does not exist, " +
                    "so CCU Config Migration not possible")
            return
        }

        // Read default values from haystack before creating the CCU-Configuration Equip and its points
        val drEnrollmentStatus = hayStack.readDefaultVal("demand and response and enable")
        val backFillDuration = hayStack.readDefaultVal("backfill and duration")
        val offlineMode = hayStack.readDefaultVal("offline and mode")
        val logLevel = hayStack.readDefaultVal("log and level and diag")
        val addressBandSelected = Globals.getInstance().smartNodeBand ?: "1000"

        CCUBaseConfigurationBuilder(hayStack).createCCUEquip(ccuModel, hayStack.ccuName!!)

        fun updateCCUEquipPoints(ccuModel: ModelDirective, hayStack: CCUHsApi) {
            val ccuEquip = Domain.ccuEquip

            ccuEquip.addressBand.writeDefaultVal(addressBandSelected)

            CCUEquipConfiguration(ccuModel as SeventyFiveFProfileDirective, hayStack)
                .saveDemandResponseEnrollmentStatus(drEnrollmentStatus > 0)

            ccuEquip.demandResponseEnrollment.writeDefaultVal(drEnrollmentStatus)
            ccuEquip.backFillDuration.writeDefaultVal(backFillDuration)
            ccuEquip.offlineMode.writeDefaultVal(offlineMode)
            ccuEquip.logLevel.writeDefaultVal(logLevel)

            if(drEnrollmentStatus > 0) {
                val demandResponseActivation = hayStack.readDefaultVal("demand and response and activation")
                ccuEquip.demandResponseActivation.writeDefaultVal(demandResponseActivation)
            }
        }

        updateCCUEquipPoints(ccuModel, hayStack)
        deleteOldAddressBandPoint(hayStack)
        deleteNonDmDrActivationPoint(hayStack)

        deleteCCURelatedPointsFromNonDmSystemProfile(hayStack)

    }

    private fun deleteCCURelatedPointsFromNonDmSystemProfile(hayStack: CCUHsApi) {

        val systemEquip = hayStack.readEntity("system and equip and not" +
                " modbus and not connectModule and not domainName")
        CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Deleting CCU points present in NON DM systemEquip $systemEquip")

        if (systemEquip.isNotEmpty()) {
            val queries = mapOf(
                "backfill and duration and not domainName" to "backfill duration point",
                "offline and mode and not domainName" to "offline mode point",
                "demand and response and enable and not domainName" to "demand response enrollment point",
                "demand and response and activation and not domainName" to "demand response activation point",
                "ota and status and not domainName and equipRef == \"${systemEquip[Tags.ID]}\"" to "ota status point"
            )

            for ((query, logMessage) in queries) {
                val point = hayStack.readEntity(query)
                if (point.isNotEmpty()) {
                    hayStack.deleteEntity(point[Tags.ID].toString())
                    CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Deleted $logMessage")
                }
            }
        }
    }


    private fun deleteNonDmDrActivationPoint(hayStack: CCUHsApi) {
        val nonDmDrActivationPoint = hayStack.readEntity("demand and activation and not domainName")
        if (nonDmDrActivationPoint.isNotEmpty()) {
            hayStack.deleteEntity(nonDmDrActivationPoint[Tags.ID].toString())
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Deleted non-DM demand response activation point")
        }

    }

    fun deleteOldAddressBandPoint(hayStack: CCUHsApi) {
        val snBandPoints = hayStack.readAllEntities("snband")
        snBandPoints.forEach {
            hayStack.deleteEntity(it[Tags.ID].toString())
            CcuLog.i(L.TAG_CCU_MIGRATION_UTIL, "Deleting old snband point")
        }
    }
}