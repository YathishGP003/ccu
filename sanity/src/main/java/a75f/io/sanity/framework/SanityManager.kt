package a75f.io.sanity.framework

import a75f.io.alerts.AlertManager
import a75f.io.logger.CcuLog
import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class SanityManager {
    /**
     * Runs all sanity suites once and returns a flow of results.
     */
    fun runOnce(runner: SanityRunner): Flow<Pair<SanityCase, SanityResult>> = flow {
        for (suite in SanitySuiteRegistry.sanitySuites.values) {
            suite.getCases().forEach { case ->
                val result = runner.runCase(case)
                emit(case to result)
                delay(1000) // TODO- TEST
            }
        }
    }

    fun runOnce(runner: SanityRunner, sanitySuite: String): Flow<Pair<SanityCase, SanityResult>> = flow {
        val suite = SanitySuiteRegistry.getSuiteByName(sanitySuite)
        if (suite != null) {
            val results = runner.runSuite(suite)
            for ((case, result) in results) {
                emit(case to result)
            }
        } else {
            throw IllegalArgumentException("No suite found with name: $sanitySuite")
        }
    }

    fun runOnceAndSaveReport(
        runner: SanityRunner,
        context: Context
    ) {
        CcuLog.i(SANITTY_TAG, "Running sanity tests and saving report...")
        runBlocking {
            val prefs = context.getSharedPreferences("ccu_sanity_report", Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val currentTime: String = sdf.format(Date())
            editor.putString("_Report_generated_at : ", currentTime)
            runOnce(runner).collect { (case, result) ->
                editor.putString(case.getName(), result.toString())
            }
            editor.apply()
        }

    }

    fun scheduleSuitePeriodic(sanitySuite : String, context: Context, periodHours: Long) {
        val data = Data.Builder()
            .putString("suite_name", sanitySuite)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SanityWorker>(
            periodHours, TimeUnit.HOURS // ⏰
        )
            .addTag(sanitySuite)
            .setInitialDelay(periodHours, TimeUnit.HOURS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sanitySuite_${sanitySuite}",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun scheduleAllSanityPeriodic(context: Context, periodHours: Long) {
        CcuLog.i(SANITTY_TAG, "Scheduling all sanity suites periodic work every $periodHours hours")
        val data = Data.Builder()
            .putString("suite_name", "all")
            .build()

        val workRequest = PeriodicWorkRequestBuilder<SanityWorker>(
            periodHours, TimeUnit.HOURS
        )
            .addTag("All")
            .setInitialDelay(periodHours, TimeUnit.HOURS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sanitySuite_All",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}