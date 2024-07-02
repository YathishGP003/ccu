package a75f.io.device.connect

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * A custom lock that ensures reads/writes to a USB port is aligned.
 * Primarily aimed at reserving the port for one transaction at a time.
 * Once a read is issued , we have to wait until the response is received or timedout , before another
 * read/write request can be issued.
 */
class CommLock (val name : String){
    private val waitTimePollMs = 10L

    private val lock: Lock = ReentrantLock()
    private val condition = lock.newCondition()

    private var transactionPending = AtomicBoolean(false)

    private var operationCode = 0
    fun lock(timeoutMS: Long, operation : Int) {
        CcuLog.d(L.TAG_CCU_SERIAL_CONNECT, "CommLock lock :$name $operation")
        operationCode = operation
        lock.lock()
        try {
            transactionPending.set(true)
            var waitMillis = timeoutMS
            while (transactionPending.get() && waitMillis > 0) {
                condition.await(waitTimePollMs, TimeUnit.MILLISECONDS)
                waitMillis -= waitTimePollMs
            }
        } catch (e: InterruptedException) {
            println(e)
        } finally {
            if (transactionPending.get()) {
                CcuLog.d(L.TAG_CCU_SERIAL_CONNECT, "CommLock Timeout :$name $operation")
            }
            lock.unlock()
        }
    }

    fun unlock() {
        CcuLog.d(L.TAG_CCU_SERIAL_CONNECT, "CommLock unlock :$name $operationCode")
        transactionPending.set(false)
    }

    fun getOperationCode() : Int {
        return operationCode
    }
}