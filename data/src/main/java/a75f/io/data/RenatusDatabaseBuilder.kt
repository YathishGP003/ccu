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
            ).addMigrations(RenatusDatabase.MIGRATION_4_5)
                    .addMigrations(RenatusDatabase.MIGRATION_4_6)
                    .addMigrations(RenatusDatabase.MIGRATION_4_7)
                    .addMigrations(RenatusDatabase.MIGRATION_5_6)
                    .addMigrations(RenatusDatabase.MIGRATION_5_7)
                    .addMigrations(RenatusDatabase.MIGRATION_6_7)
                    .addMigrations(RenatusDatabase.MIGRATION_7_8)
                    .build()
}