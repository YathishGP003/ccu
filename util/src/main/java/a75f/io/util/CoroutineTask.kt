package a75f.io.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object CoroutineTask {

    private val uiScope by lazy { CoroutineScope(Dispatchers.Main) }
    private val bgScope by lazy { CoroutineScope(Dispatchers.IO) }


    fun executeBackground(task: Runnable): Job {
        return bgScope.launch {
            task.run()
        }
    }

    /**
     * Could replace an async tasks
     * @param preExecute: Runnable to be executed before the background task on the ui thread
     * @param background: Runnable to be executed on the background thread
     * @param postExecute: Runnable to be executed after the background task on the ui thread
     */
    fun executeAsync(preExecute: Runnable, background : Runnable, postExecute : Runnable): Job {
        uiScope.launch {
            preExecute.run()
        }
        return bgScope.launch {
            background.run()
            withContext(Dispatchers.Main) {
                postExecute.run()
            }
        }
    }
}