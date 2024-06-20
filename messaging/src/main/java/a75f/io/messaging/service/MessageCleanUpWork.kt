package a75f.io.messaging.service

import a75f.io.data.message.MessageDatabaseHelper
import a75f.io.data.message.deleteMessage
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.messaging.di.MessagingEntryPoint
import android.content.Context
import androidx.work.*
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val MESSAGE_RETRY_CLEANUP_MINUTES : Long = 60
const val MESSAGE_CLEANUP_INITIAL_DELAY_SECONDS : Long = 30
const val MESSAGE_EXPIRY_DURATION_MILLIS : Long = 24 * 60 * 60 * 1000
class MessageCleanUpWork(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    private val appContext = context
    @Inject
    lateinit var msgDbHelper: MessageDatabaseHelper

    override suspend fun doWork(): Result {
        if (!this::msgDbHelper.isInitialized) {
            CcuLog.i(L.TAG_CCU_MESSAGING,"MessageCleanUpWork Init")
            val messagingDIEntryPoint = EntryPointAccessors.fromApplication(
                appContext,
                MessagingEntryPoint::class.java
            )

            msgDbHelper = messagingDIEntryPoint.dbHelper
        }
        CcuLog.i(L.TAG_CCU_MESSAGING,"MessageCleanUpWork ")

        msgDbHelper.getAllExpiredMessages(System.currentTimeMillis() - MESSAGE_EXPIRY_DURATION_MILLIS).forEach {
            CcuLog.i(L.TAG_CCU_MESSAGING,"MessageCleanUpWork - expired Message : $it")
            deleteMessage(it, appContext)
        }

        return Result.success()
    }

    companion object {
        fun scheduleMessageCleanUpWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MessageCleanUpWork>(MESSAGE_RETRY_CLEANUP_MINUTES, TimeUnit.MINUTES)
                .setInitialDelay(MESSAGE_CLEANUP_INITIAL_DELAY_SECONDS, TimeUnit.SECONDS)
                .addTag("MessageCleanUpWork")
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("MessageCleanUpWork",
                ExistingPeriodicWorkPolicy.REPLACE, workRequest
            )
        }
    }
}