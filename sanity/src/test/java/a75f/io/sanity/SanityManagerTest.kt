package a75f.io.renatus.sanity

import a75f.io.sanity.framework.SanityCase
import a75f.io.sanity.framework.SanityManager
import a75f.io.sanity.framework.SanityResult
import a75f.io.sanity.framework.SanityResultType
import a75f.io.sanity.framework.SanityRunner
import a75f.io.sanity.framework.SanitySuite
import a75f.io.sanity.framework.SanitySuiteRegistry
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any

class SanityManagerTest {

    @Test
    fun `runOnce emits results for all suites`() = runBlocking {
        val mockRunner = mock(SanityRunner::class.java)
        val mockSuite = mock(SanitySuite::class.java)
        val mockCase = mock(SanityCase::class.java)
        val mockResult = SanityResult("mockTest", SanityResultType.PASSED, "Success", false)
        val mockResults = mapOf(mockCase to mockResult)

        val mockSuites = hashMapOf("mockSuite" to mockSuite)

        mockkObject(SanitySuiteRegistry)
        every { SanitySuiteRegistry.sanitySuites } returns mockSuites

        `when`(mockRunner.runSuite(any<SanitySuite>())).thenReturn(mockResults)

        val sanityManager = SanityManager()
        val results = sanityManager.runOnce(mockRunner).toList()

        assertEquals(1, results.size)
        assertEquals(mockCase.getName(), results[0].first.getName())
        assertEquals(mockResult, results[0].second)
    }

    @Test
    fun `runOnce with suite name emits results for specific suite`() = runBlocking {
        val mockRunner = mock(SanityRunner::class.java)
        val mockSuite = mock(SanitySuite::class.java)
        val mockCase = mock(SanityCase::class.java)
        val mockResult = SanityResult("mockTest", SanityResultType.PASSED, "Success", false)
        val mockResults = mapOf(mockCase to mockResult)

        mockkObject(SanitySuiteRegistry)
        every { SanitySuiteRegistry.getSuiteByName("mockSuite") } returns mockSuite

        `when`(mockRunner.runSuite(mockSuite)).thenReturn(mockResults)

        val sanityManager = SanityManager()
        val results = sanityManager.runOnce(mockRunner,"mockSuite").toList()

        assertEquals(1, results.size)
        assertEquals(mockCase, results[0].first)
        assertEquals(mockResult, results[0].second)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `runOnce with invalid suite name throws exception`(): Unit = runBlocking {
        val mockRunner = mock(SanityRunner::class.java)
        val sanityManager = SanityManager()
        sanityManager.runOnce(mockRunner, "invalidSuite").toList()
    }
}