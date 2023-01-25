package a75f.io.messaging.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface MessageDao {

    @Insert
    fun insert(message: Message)

    @Insert
    fun insertAll(messages: List<Message>)

    @Delete
    fun delete(message: Message)

    @Delete
    fun deleteAll(messages: List<Message>)

    @Update
    fun update(message: Message)

    @Update
    fun updateAll(messages: List<Message>)

    @Query("SELECT * FROM messages")
            fun getAllMessages(): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE handlingStatus = 0")
    fun getAllUnhandledMessage(): List<Message>

    @Query("SELECT * FROM messages WHERE retryCount >= 5")
    fun getAllFailedMessages(): List<Message>
}