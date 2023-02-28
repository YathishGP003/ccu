package a75f.io.data

import a75f.io.data.message.Message
import a75f.io.data.message.MessageDao
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Message::class], version = 1)
abstract class RenatusDatabase : RoomDatabase(){
    abstract fun messageDao(): MessageDao
}