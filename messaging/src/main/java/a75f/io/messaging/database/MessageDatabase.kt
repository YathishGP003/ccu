package a75f.io.messaging.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Message::class], version = 2)
abstract class MessageDatabase : RoomDatabase(){
    abstract fun messageDao(): MessageDao
}