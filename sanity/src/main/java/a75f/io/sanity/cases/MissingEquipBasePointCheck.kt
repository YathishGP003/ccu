package a75f.io.sanity.cases

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.util.ModelCache
import a75f.io.logger.CcuLog
import a75f.io.sanity.framework.SANITTY_TAG
import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityResultSeverity
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.model.ModelType

class MissingEquipBasePointCheck : SanityCase {
    var failCount = 0
    override fun getName(): String = "MissingEquipBasePointCheck"

    /**
     * Executes a sanity check to verify that every device in the system
     * has all its expected base points (as defined in its source model).
     *
     * The function performs the following steps:
     * - Reads all devices from the CCUHs API.
     * - Skips processing if no devices are found.
     * - For each device, retrieves its source model and expected points.
     * - Compares the model’s points to the points currently associated with the device.
     * - Logs any missing base points for each device.
     * - Returns `false` if any missing base points are found; otherwise returns `true`.
     *
     * @return `true` if all devices have their complete base point sets, otherwise `false`.
     */
    override fun execute(): Boolean {
        // Placeholder for actual implementation
        val equipList = CCUHsApi.getInstance().readAllHDictByQuery("equip and domainName")
        if (equipList.isEmpty()) {
            // No need to do anything if there are no devices
            return true
        }
        failCount = 0

        equipList.forEach { equip ->
            val modelId = equip.get("sourceModel")
            val model = ModelCache.getModelById(modelId.toString())
            if(model.modelType != ModelType.PROFILE) {
                // Only profile models are supported
                return@forEach
            }
            val entityMapper = EntityMapper(model as SeventyFiveFProfileDirective)
            val modelPointsList = entityMapper.getBasePoints()
            val currentPointList = CCUHsApi.getInstance().readAllHDictByQuery("point and equipRef == \"${equip.get("id")}\"")
            modelPointsList.forEach { modelPoint ->
                val domainName = modelPoint.domainName
                if (domainName.isNotEmpty()) {
                    if (currentPointList.none { it.get("domainName").toString() == domainName }) {
                        CcuLog.e(SANITTY_TAG, "Missing base point for equip ${equip.get("id")} with domainName $domainName in model $modelId")
                        failCount++
                    }
                }
            }
        }

        if(failCount > 0) {
            CcuLog.e(SANITTY_TAG, "MissingBasePointCheck found $failCount missing base points")
            return false
        }
        return true
    }

    override fun getSeverity(): SanityResultSeverity {
        return SanityResultSeverity.HIGH
    }

    override fun report(): String {
        return "No missing base point"
    }

    override fun correct(): Boolean {
        // Placeholder for correction logic
        return false
    }

    override fun getDescription(): String {
        if(failCount > 0) {
            return "Found $failCount missing base points."
        }
        return "No missing base points found."
    }
}