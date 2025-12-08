package a75f.io.sanity.cases

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.util.ModelCache
import a75f.io.logger.CcuLog
import a75f.io.sanity.framework.SANITTY_TAG
import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityResultSeverity

class DiagEquipCheck : SanityCase {
    var failCount = 0
    val TAG_CCU_MIGRATION_UTIL: String = "CCU_MIGRATION_UTIL"
    val haystack: CCUHsApi
        get() = CCUHsApi.getInstance()
    val diagEquips = haystack.readAllEntities("domainName == \"diagEquip\"")
    var errorMessage = "No diagnostic equipment issues found"

    override fun getName(): String = "DiagEquipCheck"

    override fun execute(): Boolean {
        failCount = 0
        errorMessage = "No diagnostic equipment issues found"

        // Step 1: Check for unique diag equipment
        if(diagEquips.size != 1) {
            return false
        }
        // Since some points dont have diag tag, add it in or condition below
        val diagPoints = haystack.readAllEntities("(diag and equipRef == \"${diagEquips[0].get(Tags.ID)}\")" +
                "or domainName == \"otaStatusCCU\" or domainName == \"otaStatusCM\" or domainName == \"otaStatusBundle\"")
        if(diagPoints.isEmpty()) {
            return false
        }

        // Step 2: Check for missing diag points and compare it with the model points
        val modelId = diagEquips[0]["sourceModel"]
        val model = ModelCache.getModelById(modelId.toString())
        val modelPointsList = model.allPoints()
        modelPointsList.forEach { modelPoint ->
            val domainName = modelPoint.domainName
            if (domainName.isNotEmpty()) {
                if (diagPoints.none { it.get("domainName").toString() == domainName }) {
                    CcuLog.e(SANITTY_TAG, "Missing base point for device ${diagPoints[0].get("id")} with domainName $domainName in model $modelId")
                    failCount++
                }
            }
        }

        // Step 3: Validate diag equipment fields
        return (checkIfDiagEquipIsValid(diagEquips[0]) && failCount == 0)
    }

    override fun report(): String {
        if (diagEquips.isEmpty()) {
            errorMessage = "No diagnostic equipment found"
        } else if (diagEquips.size > 1) {
            errorMessage = "Multiple diagnostic equipment found: ${diagEquips.size}"
        } else if(failCount > 0) {
            errorMessage = "Fail points missing $failCount diag points"
        }
        return errorMessage
    }

    override fun correct(): Boolean {
        if (diagEquips.isEmpty() || diagEquips.size > 1) {
            haystack.removeEntity(diagEquips[0].get(Tags.ID).toString())
            return true
        }
        return false
    }

    override fun getSeverity(): SanityResultSeverity {
        return SanityResultSeverity.HIGH
    }

    override fun getDescription(): String {
        return errorMessage
    }

    private fun checkIfDiagEquipIsValid(diagEquip: HashMap<Any, Any>): Boolean {
        if (diagEquip[Tags.AHU_REF] == null || diagEquip[Tags.AHU_REF].toString().isEmpty() ||
            diagEquip[Tags.GATEWAY_REF] == null || diagEquip[Tags.GATEWAY_REF].toString().isEmpty() ||
            diagEquip[Tags.CCUREF] == null || diagEquip[Tags.CCUREF].toString().isEmpty() ||
            diagEquip[Tags.SITEREF] == null || diagEquip[Tags.SITEREF].toString().isEmpty()) {
            errorMessage = "AhuRef gatewayRef ccuRef or siteRef is missing in Diag Equip"
            return false
        } else if (diagEquip[Tags.DOMAIN_NAME] == null || diagEquip[Tags.DOMAIN_NAME].toString().isEmpty()) {
            errorMessage = "Diag Equip missing domain name"
            return false
        }
        return true
    }
}