package a75f.io.sanity.framework

import a75f.io.alerts.AlertManager
import a75f.io.alerts.SANITY_CHECK_STATUS
import a75f.io.api.haystack.CCUHsApi
import android.util.Log

object ResultHandler {
    fun handleResult(result: SanityResult) {
        if (result.result == SanityResultType.PASSED) {
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

        AlertManager.getInstance().generateAlert(
            SANITY_CHECK_STATUS,
            alertMessage,
        )
    }
}