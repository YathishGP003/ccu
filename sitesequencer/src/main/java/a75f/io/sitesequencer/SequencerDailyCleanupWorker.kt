package a75f.io.sitesequencer


import a75f.io.alerts.AlertManager
import a75f.io.logger.CcuLog
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SequencerDailyCleanupWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    override fun doWork(): Result {
        performDailyTask()
        return Result.success()
    }

    private fun performDailyTask() {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "--sequencer/blockly alert daily cleanup--")
        SequenceManager.getInstance().fetchPredefinedSequencesForCleanUp()
        AlertManager.getInstance().fetchBlocklyAlertsForCleanUp()
    }
}