package a75f.io.sanity.framework

import a75f.io.sanity.framework.ResultHandler.handleResult
import android.util.Log

class SanityRunner {
    fun runSuite(suite: SanitySuite): Map<SanityCase, SanityResult> {
        val results = mutableMapOf<SanityCase, SanityResult>()

        suite.getCases().forEach { case ->
            Log.i(SANITTY_TAG, "Running case: ${case.getName()}")
            val result = case.execute()
            val report = case.report()

            val resultDto = SanityResult(
                name = case.getName(),
                result = if (result) SanityResultType.PASSED else SanityResultType.FAILED,
                report = report,
                corrected = case.correct(),
                severity = case.getSeverity()
            )

            if (!result) {
                if (case.correct()) {
                    resultDto.corrected = true
                }
            }
            handleResult(resultDto)
            results[case] = resultDto
        }

        return results
    }

    fun runCase(case: SanityCase): SanityResult {
        Log.i(SANITTY_TAG, "Running case: ${case.getName()}")
        val result = case.execute()
        val report = case.report()

        val resultDto = SanityResult(
            name = case.getName(),
            result = if (result) SanityResultType.PASSED else SanityResultType.FAILED,
            report = report,
            corrected = case.correct(),
            severity = case.getSeverity()
        )

        if (!result) {
            if (case.correct()) {
                resultDto.corrected = true
            }
        }
        Log.i(SANITTY_TAG, "Case ${case.getName()} result: ${resultDto.result.name}")
        return resultDto
    }
}