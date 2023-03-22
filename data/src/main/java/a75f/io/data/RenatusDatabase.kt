package a75f.io.data

import a75f.io.data.message.Message
import a75f.io.data.message.MessageDao
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [Message::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RenatusDatabase : RoomDatabase(){
    abstract fun messageDao(): MessageDao
}

class Converters {
    @TypeConverter
    fun toListOfStrings(flatStringList: String?): List<String>? {
        return flatStringList?.split(",")
    }
    @TypeConverter
    fun fromListOfStrings(listOfString: List<String>?): String? {
        return listOfString?.joinToString(",")
    }
}
