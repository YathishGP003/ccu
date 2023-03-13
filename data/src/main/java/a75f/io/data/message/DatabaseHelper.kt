package a75f.io.data.message

import a75f.io.data.message.Message
import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

interface DatabaseHelper {
    suspend fun insert(message: Message)
    suspend fun update(message: Message)
    suspend fun delete(message: Message)
    fun getAllMessages(): LiveData<List<Message>>
    fun getAllMessagesList(): List<Message>
    suspend fun insertAll(message: List<Message>)
    suspend fun updateAll(message: List<Message>)
    suspend fun deleteAll(message: List<Message>)
    suspend fun getAllUnhandledMessage(): Flow<List<Message>>
    suspend fun getAllFailedMessages(): Flow<List<Message>>
    suspend fun getMessageById(messageId : String) : Message
}