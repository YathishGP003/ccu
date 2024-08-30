package a75f.io.domain.util
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

val highPriorityDispatcher = Executors.newSingleThreadExecutor(object : ThreadFactory {
    private val counter = AtomicInteger(0)

    override fun newThread(r: Runnable): Thread {
        return Thread(r, "HighPriorityThread-${counter.incrementAndGet()}").apply {
            priority = Thread.MAX_PRIORITY - 1
        }
    }
}).asCoroutineDispatcher()
