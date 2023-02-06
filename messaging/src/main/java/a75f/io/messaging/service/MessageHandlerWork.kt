package a75f.io.messaging.service

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Schedules a repeating handler job and processes all the unhandled messages.
 */
class MessageHandlerWork(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private var appContext : Context = context

    override suspend fun doWork(): Result {
        CcuLog.i(L.TAG_CCU_MESSAGING,"MessageHandlerWork")
        return try {
            MessageHandlerService.getInstance(appContext).handleMessages()
            Result.success()
        } catch (e: java.lang.Exception) {
            Result.failure()
        }
    }

    companion object {
        fun schedulePeriodicMessageWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MessageHandlerWork>(15, TimeUnit.MINUTES)
                                            .setInitialDelay(5, TimeUnit.SECONDS)
                                            .addTag("MessageHandlingWork")
                                            .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("MessageHandlingWork",
                ExistingPeriodicWorkPolicy.KEEP, workRequest
            )
        }
    }
}

