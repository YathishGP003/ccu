package a75f.io.messaging.database

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow

interface DatabaseHelper {
    suspend fun insert(message: Message)
    suspend fun update(message: Message)
    suspend fun delete(message: Message)
    fun getAllMessages(): LiveData<List<Message>>
    suspend fun insertAll(message: List<Message>)
    suspend fun updateAll(message: List<Message>)
    suspend fun deleteAll(message: List<Message>)
    suspend fun getAllUnhandledMessage(): Flow<List<Message>>
    suspend fun getAllFailedMessages(): Flow<List<Message>>
}