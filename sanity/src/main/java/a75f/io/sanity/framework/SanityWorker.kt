package a75f.io.sanity.framework

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SanityWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(SANITTY_TAG, "SanityWorker started")
        val sanityManager = SanityManager()
        sanityManager.runOnceAndSaveReport(SanityRunner(), this.applicationContext)
        return Result.success()
    }
}