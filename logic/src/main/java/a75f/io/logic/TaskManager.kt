package a75f.io.logic

import a75f.io.logger.CcuLog
import a75f.io.logic.L.TAG_CCU_UI
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.Future


object TaskManager {
    @Volatile
    private var intrinsicScheduleTask: Future<*>? = null

    fun disposeCurrentTask() {
        intrinsicScheduleTask?.let {
            if (!it.isDone) {
                it.cancel(true)
                CcuLog.d(TAG_CCU_UI, "Dispose called: intrinsicScheduleTask disposed.")
            } else {
                CcuLog.d(TAG_CCU_UI, "Dispose called: intrinsicScheduleTask was already disposed.")
            }
            intrinsicScheduleTask = null
        } ?: CcuLog.d(TAG_CCU_UI, "Dispose called: intrinsicScheduleTask is null.")
    }

    fun setNewTask(task: Future<*>?) {
        intrinsicScheduleTask = task
        CcuLog.i(TAG_CCU_UI, "New intrinsicScheduleTask set: $task")
    }

    fun getTask(): Future<*>? {
        return intrinsicScheduleTask
    }
}