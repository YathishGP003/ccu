package a75f.io.sanity.cases

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.util.ModelCache
import a75f.io.logger.CcuLog
import a75f.io.sanity.framework.SANITTY_TAG
import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityResultSeverity

class MissingDeviceBasePointCheck : SanityCase {
    var failCount = 0
    override fun getName(): String = "MissingDeviceBasePointCheck"

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
        val deviceList = CCUHsApi.getInstance().readAllHDictByQuery("device and domainName")
        if (deviceList.isEmpty()) {
            // No need to do anything if there are no devices
            return true
        }
        failCount = 0

        deviceList.forEach { device ->
            val modelId = device.get("sourceModel")
            val model = ModelCache.getModelById(modelId.toString())
            val modelPointsList = model.allPoints()
            val currentPointList = CCUHsApi.getInstance().readAllHDictByQuery("point and deviceRef == \"${device.get("id")}\"")
            modelPointsList.forEach { modelPoint ->
                val domainName = modelPoint.domainName
                if (domainName.isNotEmpty()) {
                    if (currentPointList.none { it.get("domainName").toString() == domainName }) {
                        CcuLog.e(SANITTY_TAG, "Missing base point for device ${device.get("id")} with domainName $domainName in model $modelId")
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