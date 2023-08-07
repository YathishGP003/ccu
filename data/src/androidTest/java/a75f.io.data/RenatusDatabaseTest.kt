package a75f.io.data


import a75f.io.data.entities.Entities
import a75f.io.data.entities.EntityDao
import a75f.io.data.message.Message
import a75f.io.data.message.MessageDao
import a75f.io.data.writablearray.WritableArray
import a75f.io.data.writablearray.WritableArrayDao
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class RenatusDatabaseTest {

    private lateinit var renatusDb : RenatusDatabase
    private lateinit var messageDao : MessageDao
    private lateinit var entityDao : EntityDao
    private lateinit var writableArrayDao : WritableArrayDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        renatusDb = Room.inMemoryDatabaseBuilder(context, RenatusDatabase::class.java).build()
        messageDao = renatusDb.messageDao()
        entityDao = renatusDb.entityDao()
        writableArrayDao = renatusDb.writableArrayDao()

    }

    @After
    fun tearDown() {
        renatusDb.close()
    }

    @Test
    fun messageDb_insertMessageTest() = runBlocking{
        val testMessage = Message("testMessageId")
        messageDao.insert(testMessage)
        val messages = messageDao.getAllMessagesList()
        assertThat(messages).contains(testMessage)
    }

    @Test
    fun messageDb_updateMessageTest() = runBlocking{
        val testMessage = Message("testMessageId")
        messageDao.insert(testMessage)
        val message = messageDao.getAllMessagesList()[0]
        assertThat(message).isEqualTo(testMessage)
        message.command = "testCommand"
        messageDao.update(message)
        assertThat(messageDao.getAllMessagesList()[0])
            .isEqualTo(Message(messageId = "testMessageId",
                command = "testCommand"))

    }

    @Test
    fun messageDb_deleteMessageTest() = runBlocking{
        val testMessage = Message("testMessageId")
        messageDao.insert(testMessage)
        val message = messageDao.getAllMessagesList()[0]
        messageDao.delete(message)
        assertThat(messageDao.getAllMessagesList())
            .doesNotContain(testMessage)

    }

    @Test
    fun messageDb_unhandledMessageTest() = runBlocking{
        val testMessage = Message("testMessageId")
        messageDao.insert(testMessage)
        val messages = messageDao.getAllUnhandledMessage()
        assertThat(messages).contains(testMessage)
        testMessage.handlingStatus = true
        messageDao.update(testMessage)
        assertThat(messageDao.getAllUnhandledMessage()).isEmpty()
    }


    @Test
    fun messageDb_insertEntityTest() = runBlocking{
        val entityTable = Entities("testId" )
        entityDao.insert(entityTable)
        val entities = entityDao.getAllEntities()
        assertThat(entities).contains(entityTable)
    }

    @Test
    fun messageDb_insertPersistentDataTest() = runBlocking{
        val data = WritableArray("testId")
        writableArrayDao.insert(data)
        val allData = writableArrayDao.getAllwritableArrays()
        assertThat(allData).contains(data)
    }

}