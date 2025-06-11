package a75f.io.renatus.sanity

import a75f.io.sanity.framework.SanityManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SanityManagerInstrumentedTest {

    @Test
    fun testPeriodicSanityExecution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workManager = WorkManager.getInstance(context)
        val sanityManager = SanityManager()

        sanityManager.scheduleSuitePeriodic("mockSuite", context, 24)

        val workInfos = workManager.getWorkInfosForUniqueWork("sanitySuite_mockSuite").get()

        assertEquals(1, workInfos.size)
        assertEquals("mockSuite", workInfos[0].tags.firstOrNull())
    }

    @Test
    fun testPeriodicAllSanityExecution() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val workManager = WorkManager.getInstance(context)
        val sanityManager = SanityManager()

        sanityManager.scheduleAllSanityPeriodic(context, 24)

        val workInfos = workManager.getWorkInfosForUniqueWork("sanitySuite_All").get()
        assertEquals(1, workInfos.size)
        assertEquals("All", workInfos[0].tags.firstOrNull())
    }
}