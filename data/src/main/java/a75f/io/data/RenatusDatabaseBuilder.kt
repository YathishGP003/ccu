package a75f.io.data
import android.content.Context
import androidx.room.Room

object RenatusDatabaseBuilder {
    private var INSTANCE: RenatusDatabase? = null
    @JvmStatic fun getInstance(context: Context): RenatusDatabase {
        if (INSTANCE == null) {
            synchronized(RenatusDatabase::class) {
                INSTANCE = buildRoomDB(context)
            }
        }
        return INSTANCE!!
    }
    private fun buildRoomDB(context: Context) =
        Room.databaseBuilder(
            context.applicationContext,
            RenatusDatabase::class.java,
            "renatusDb"
        ).fallbackToDestructiveMigration()
         .build()
}