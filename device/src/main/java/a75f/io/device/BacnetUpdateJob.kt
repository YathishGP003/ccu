package a75f.io.device

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.util.bacnet.checkBacnetHealth
import a75f.io.logic.util.bacnet.updateBacnetMstpLinearAndCovSubscription
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal class BacnetUpdateJob : BaseJob () {
    private val jobLock: Lock = ReentrantLock()

    override fun doJob() {
        if (jobLock.tryLock()) {
            try {
                CcuLog.d(L.TAG_CCU_JOB, "BacnetUpdateJob -> ")
                checkBacnetHealth()
                updateBacnetMstpLinearAndCovSubscription(false)
                CcuLog.d(L.TAG_CCU_JOB, "<- BacnetUpdateJob ")
            } catch (e: Exception) {
                CcuLog.e(L.TAG_CCU_JOB, "Exception in BacnetUpdateJob: ${e.message}", e)
            } finally {
                jobLock.unlock()
            }
        } else {
            CcuLog.d(L.TAG_CCU_JOB, "BacnetUpdateJob is already running, skipping this execution.")
            return
        }

    }
}