package a75f.io.sitesequencer

import a75f.io.logger.CcuLog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SequencerAlarmReceiver : BroadcastReceiver() {
    val TAG = SequencerParser.TAG_CCU_SITE_SEQUENCER
    private val constInterval = "interval"
    private val constSeqId = "seqId"
    override fun onReceive(context: Context, intent: Intent?) {
        val seqId = intent?.getStringExtra(constSeqId)
        val interval = intent?.getStringExtra(constInterval)
        CcuLog.d(TAG, "------------Alarm received--------------")
        if (intent != null) {
            intent.getStringExtra(interval)?.let {
                CcuLog.d(TAG, "Alarm received with interval: $it")
            }
            intent.getStringExtra("seqId")?.let {
                CcuLog.d(TAG, "seqId: $it")
            }
        }
        // Enqueue your WorkManager task
        val data = Data.Builder()
            .putString("seqId", seqId) // Add key-value pairs to the Data object
            .build()
        val workRequest = OneTimeWorkRequestBuilder<SequenceWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
        CcuLog.d(TAG, "------------Alarm received end--------------")
    }
}
