package a75f.io.messaging.database

import android.content.Context
import androidx.room.Room

object MessageDatabaseBuilder {
    private var INSTANCE: MessageDatabase? = null
    @JvmStatic fun getInstance(context: Context): MessageDatabase {
        if (INSTANCE == null) {
            synchronized(MessageDatabase::class) {
                INSTANCE = buildRoomDB(context)
            }
        }
        return INSTANCE!!
    }
    private fun buildRoomDB(context: Context) =
        Room.databaseBuilder(
            context.applicationContext,
            MessageDatabase::class.java,
            "messageDb"
        ).fallbackToDestructiveMigration()
         .build()
}