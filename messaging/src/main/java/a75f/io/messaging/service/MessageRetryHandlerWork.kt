package a75f.io.messaging.service

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.di.MessagingEntryPoint
import android.content.Context
import androidx.work.*
import dagger.hilt.android.EntryPointAccessors.fromApplication
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Schedules a repeating handler job and processes all the unhandled messages.
 */

const val MESSAGE_RETRY_INTERVAL_MINUTES : Long = 3
class MessageRetryHandlerWork(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private var appContext : Context = context

    @Inject
    lateinit var messageHandlerService: MessageHandlerService

    override suspend fun doWork(): Result {

        if (!this::messageHandlerService.isInitialized) {
            CcuLog.i(L.TAG_CCU_MESSAGING,"MessageRetryHandlerWork Init")
            val messagingDIEntryPoint = fromApplication(
                appContext,
                MessagingEntryPoint::class.java
            )

            messageHandlerService = messagingDIEntryPoint.messagingHandlerService
        }
        CcuLog.i(L.TAG_CCU_MESSAGING,"MessageRetryHandlerWork ")

        messageHandlerService.handleMessages()
        //Always return success as this is periodic work.
        scheduleMessageRetryWork(appContext)
        return Result.success()
    }


    /**
     * Periodic job is not used here since the WorkManager requires minimum interval of 15 minutes.
     */
    companion object {
        fun scheduleMessageRetryWork(context: Context) {
            val messageRetryWorkRequest = OneTimeWorkRequest.Builder(MessageRetryHandlerWork::class.java)
                                                .setInitialDelay(MESSAGE_RETRY_INTERVAL_MINUTES, TimeUnit.MINUTES)
                                                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("MessageRetryWork",
                                ExistingWorkPolicy.REPLACE, messageRetryWorkRequest)
        }
    }
}

