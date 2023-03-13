package a75f.io.data.message
import a75f.io.data.RenatusDatabase
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageDatabaseHelper @Inject constructor (private val messagesDb: RenatusDatabase) : DatabaseHelper {
    override suspend fun insert(message: Message) = messagesDb.messageDao().insert(message)
    override suspend fun update(message: Message) = messagesDb.messageDao().update(message)
    override suspend fun delete(message: Message) = messagesDb.messageDao().delete(message)
    override fun getAllMessages(): LiveData<List<Message>> = messagesDb.messageDao().getAllMessages()
    override fun getAllMessagesList(): List<Message> = messagesDb.messageDao().getAllMessagesList()
    override suspend fun insertAll(messages: List<Message>) = messagesDb.messageDao().insertAll(messages)
    override suspend fun deleteAll(messages: List<Message>) = messagesDb.messageDao().deleteAll(messages)
    override suspend fun updateAll(message: List<Message>) = messagesDb.messageDao().updateAll(message)
    override suspend fun getAllUnhandledMessage() : Flow<List<Message>> = flow {
        emit(messagesDb.messageDao().getAllUnhandledMessage())
    }
    override suspend fun getAllFailedMessages() : Flow<List<Message>> = flow {
        emit(messagesDb.messageDao().getAllFailedMessages())
    }
    override suspend fun getMessageById(messageId: String) : Message = messagesDb.messageDao().getMessageById(messageId)
}