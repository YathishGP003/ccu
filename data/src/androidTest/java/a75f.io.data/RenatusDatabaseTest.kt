package a75f.io.data

import a75f.io.data.message.MessageDao
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RenatusDatabaseTest {

    private lateinit var renatusDb : RenatusDatabase
    private lateinit var messageDao : MessageDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        renatusDb = Room.inMemoryDatabaseBuilder(context, RenatusDatabase::class.java).build()
        messageDao = renatusDb.messageDao()
    }

    @After
    fun tearDown() {
        renatusDb.close()
    }

    @Test
    fun insertMessageTest() {
        assertEquals(null, messageDao.getAllMessages().value?.size)
    }

}