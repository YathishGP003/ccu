package a75f.io.logic

import a75f.io.logger.CcuLog
import a75f.io.logic.L.TAG_CCU_UI
import io.reactivex.rxjava3.disposables.Disposable


object TaskManager {
    @Volatile
    private var intrinsicScheduleTask: Disposable? = null

    fun disposeCurrentTask() {
        intrinsicScheduleTask?.let {
            if (!it.isDisposed) {
                it.dispose()
                CcuLog.d(TAG_CCU_UI, "Dispose called: intrinsicScheduleTask disposed.")
            } else {
                CcuLog.d(TAG_CCU_UI, "Dispose called: intrinsicScheduleTask was already disposed.")
            }
            intrinsicScheduleTask = null
        } ?: CcuLog.d(TAG_CCU_UI, "Dispose called: intrinsicScheduleTask is null.")
    }

    fun setNewTask(task: Disposable) {
        intrinsicScheduleTask = task
        CcuLog.i(TAG_CCU_UI, "New intrinsicScheduleTask set: $task")
    }

    fun getTask(): Disposable? {
        return intrinsicScheduleTask
    }
}