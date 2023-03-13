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

const val MESSAGE_RETRY_INTERVAL_MINUTES : Long = 30
const val MESSAGE_RETRY_INITIAL_MINUTES : Long = 5
class MessageRetryHandlerWork(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private var appContext : Context = context

    @Inject
    lateinit var messageHandlerService: MessageHandlerService

    override suspend fun doWork(): Result {

        if (!this::messageHandlerService.isInitialized) {
            CcuLog.i(L.TAG_CCU_MESSAGING,"MessageHandlerWork Init")
            val messagingDIEntryPoint = fromApplication(
                appContext,
                MessagingEntryPoint::class.java
            )

            if (messagingDIEntryPoint != null) {
                messageHandlerService = messagingDIEntryPoint.messagingHandlerService
            }
        }
        CcuLog.i(L.TAG_CCU_MESSAGING,"MessageHandlerWork ")

        messageHandlerService.handleMessages()
        //Always return success as we will schedule
        return Result.success()
    }

    companion object {
        fun schedulePeriodicMessageWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MessageRetryHandlerWork>(MESSAGE_RETRY_INTERVAL_MINUTES, TimeUnit.MINUTES)
                                            .setInitialDelay(MESSAGE_RETRY_INITIAL_MINUTES, TimeUnit.SECONDS)
                                            .addTag("MessageHandlingWork")
                                            .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("MessageHandlingWork",
                ExistingPeriodicWorkPolicy.REPLACE, workRequest
            )
        }
    }
}

