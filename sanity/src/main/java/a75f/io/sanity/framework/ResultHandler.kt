package a75f.io.sanity.framework

import a75f.io.alerts.AlertManager
import a75f.io.alerts.SANITY_CHECK_STATUS
import a75f.io.api.haystack.CCUHsApi
import android.util.Log

object ResultHandler {
    fun handleResult(result: SanityResult) {
        if (result.result == SanityResultType.PASSED) {
            // Fix only the specific alert for this sanity check if it exists and failed previously
            AlertManager.getInstance().fixCCUSanityAlert(result.name)
            return
        }

        if (result.severity == SanityResultSeverity.HIGH) {
            Log.i(SANITTY_TAG, "High severity sanity failure detected: ${result.name}")

            generateAlert(result)
        }
    }

    fun generateAlert(result: SanityResult) {
        val ccuName = CCUHsApi.getInstance().ccuName
        val alertMessage = String.format("%s Sanity check failed - %s",ccuName, result.name)

        // Since the alert is specific to the failure name, pass it as a unique identifier and store it
        // in the equip id field
        AlertManager.getInstance().generateAlert(
            SANITY_CHECK_STATUS,
            alertMessage,
            result.name
        )
    }
}