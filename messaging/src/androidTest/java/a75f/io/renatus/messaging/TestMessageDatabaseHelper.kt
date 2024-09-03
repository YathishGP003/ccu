package a75f.io.renatus.messaging

import a75f.io.data.message.DatabaseHelper
import a75f.io.data.message.Message
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

 abstract class TestMessageDatabaseHelper : DatabaseHelper{
    override suspend fun insert(message: Message) {
        TODO("Not yet implemented")
    }

    override suspend fun update(message: Message) {
        TODO("Not yet implemented")
    }

    override suspend fun delete(message: Message) {
        TODO("Not yet implemented")
    }

    override fun getAllMessages(): LiveData<List<Message>> {
        return MutableLiveData<List<Message>>()
    }

    override suspend fun insertAll(message: List<Message>) {
        TODO("Not yet implemented")
    }

    override suspend fun updateAll(message: List<Message>) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAll(message: List<Message>) {
        TODO("Not yet implemented")
    }

    override suspend fun getAllUnhandledMessage(): Flow<List<Message>> {
        return flow {
            emit(listOf<Message>())
        }
    }

    override suspend fun getAllFailedMessages(): Flow<List<Message>> {
        TODO("Not yet implemented")
    }

    override suspend fun getMessageById(messageId: String): Message {
        TODO("Not yet implemented")
    }
}