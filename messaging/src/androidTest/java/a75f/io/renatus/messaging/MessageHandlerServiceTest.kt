package a75f.io.renatus.messaging

import a75f.io.messaging.service.MessageHandlerService
import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.flow.flow
import org.junit.Test
import javax.inject.Inject

class MessageHandlerServiceTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var service: MessageHandlerService

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun testMessageService_whenCallingHandleMessage_thenFetchesMessagesFromDatabase() {

        //val mockFLow = mockk<Flow<List<Message>>>()
        val msgDbHelper = spyk<TestMessageDatabaseHelper>() {
            coEvery { getAllUnhandledMessage() } returns flow {  }
        }

        //val msgDbHelper = TestMessageDatabaseHelper()

        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        //when
        service.handleMessages()

        //then
        coVerify { msgDbHelper.getAllUnhandledMessage() }
    }
}