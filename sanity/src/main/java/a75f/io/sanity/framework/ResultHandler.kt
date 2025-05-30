package a75f.io.sanity.framework

import a75f.io.alerts.AlertManager
import a75f.io.alerts.CCU_SANITY_FAILED
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
        AlertManager.getInstance().generateAlert(
            CCU_SANITY_FAILED,
            "Sanity check failed: ${result.name}",
        )
    }
}