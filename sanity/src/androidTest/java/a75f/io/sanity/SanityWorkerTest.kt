package a75f.io.renatus.sanity

import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityResult
import a75f.io.sanity.framework.SanityResultType
import a75f.io.sanity.framework.SanityRunner
import a75f.io.sanity.framework.SanitySuiteRegistry
import a75f.io.sanity.framework.SanityWorker
import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.WorkerParameters
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SanityWorkerTest {
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var sanityRunner: SanityRunner
    private lateinit var sanityWorker: SanityWorker
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        workerParams = mockk(relaxed = true)
        sanityRunner = mockk(relaxed = true)
        sharedPreferences = context.getSharedPreferences("sanity_results", Context.MODE_PRIVATE)

        mockkObject(SanitySuiteRegistry)
        every { SanitySuiteRegistry.sanitySuites } returns HashMap(
            mapOf(
            "suite1" to mockk(relaxed = true),
            "suite2" to mockk(relaxed = true)
            )
        )

        sanityWorker = spyk(SanityWorker(context, workerParams)) {
            every { runBlocking {   doWork() }} answers { callOriginal() }
        }
    }

    @Test
    fun testWork() = runBlocking {
        val mockResults = mapOf(
            mockk<SanityCase> { every { getName() } returns "case1" } to SanityResult("case1", SanityResultType.PASSED, "Success", false),
            mockk<SanityCase> { every { getName() } returns "case2" } to SanityResult("case2", SanityResultType.FAILED, "Failure", true)
        )
        every { sanityRunner.runSuite(any()) } returns mockResults

        sanityWorker.doWork()

        val storedCase1 = sharedPreferences.getString("case1", null)
        val storedCase2 = sharedPreferences.getString("case2", null)

        assertEquals("true", storedCase1)
        assertEquals("false", storedCase2)
    }
}