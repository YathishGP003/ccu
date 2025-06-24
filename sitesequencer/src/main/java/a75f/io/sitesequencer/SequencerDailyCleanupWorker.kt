package a75f.io.sitesequencer


import a75f.io.alerts.AlertManager
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.pointscheduling.model.CustomScheduleManager
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        CoroutineScope(Dispatchers.IO).launch {
            CcuLog.d(L.TAG_CCU_POINT_SCHEDULE, "<<--event cleaning-->>")
            CustomScheduleManager().cleanEvents()
        }
    }
}