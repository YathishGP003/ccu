package a75f.io.data

import a75f.io.data.entities.EntityDao
import a75f.io.data.entities.HayStackEntity
import a75f.io.data.message.Message
import a75f.io.data.message.MessageDao
import a75f.io.data.writablearray.WritableArray
import a75f.io.data.writablearray.WritableArrayDao
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.util.*


@Database(entities = [Message::class,HayStackEntity ::class, WritableArray :: class], version = 4, exportSchema = false)
@TypeConverters(Converters::class, WriteArrayTypeConverter::class)
abstract class RenatusDatabase : RoomDatabase(){
    abstract fun messageDao(): MessageDao
    abstract fun entityDao(): EntityDao

    abstract fun writableArrayDao(): WritableArrayDao
}

class Converters {

    @TypeConverter
    fun fromString(value: String?): ArrayList<String?>? {
        val listType: Type = object : TypeToken<ArrayList<String?>?>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayList(list: ArrayList<String?>?): String? {
        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toListOfStrings(flatStringList: String?): List<String>? {
        return flatStringList?.split(",")
    }

    @TypeConverter
    fun fromListOfStrings(listOfString: List<String>?): String? {
        return listOfString?.joinToString(",")
    }

    @TypeConverter
    fun stringToList(data: String?): List<HashMap<String, String>>? {
        if (data == null) {
            return ArrayList()
        }

        val listType = object : TypeToken<List<HashMap<String, String>>>() {

        }.type

        return Gson().fromJson<List<HashMap<String, String>>>(data, listType)
    }

    @TypeConverter
    fun listToString(objects: List<HashMap<String, String>>?): String? {
        return Gson().toJson(objects)
    }

    @TypeConverter
    fun stringToMap(value: String?): HashMap<String, Any>? {
        return Gson().fromJson(value,  object : TypeToken<HashMap<String, Any>>() {}.type)
    }

    @TypeConverter
    fun mapToString(value: HashMap<String, Any>?): String? {
        return if (value == null) "" else Gson().toJson(value)
    }

//    @TypeConverter
//    fun stringToWA(value: String?):  WriteArray {
//        return Gson().fromJson(value,  object : TypeToken<WriteArray>() {}.type)
//    }
//
//    @TypeConverter
//    fun WAToString(value:  WriteArray?): String? {
//        return if (value == null) "" else Gson().toJson(value)
//    }
}

class WriteArrayTypeConverter {
    @TypeConverter
    fun fromWriteArray(writeArray: WriteArray): String {
        return Gson().toJson(writeArray)
    }

    @TypeConverter
    fun toWriteArray(json: String): WriteArray {
        return Gson().fromJson(json, WriteArray::class.java)
    }
}

class WriteArray() {
    val value: Array<String?> = arrayOfNulls<String>(17)
    val who = arrayOfNulls<String?>(17)
    val duration = LongArray(17)
    val lastModifiedTime: Array<Long?> = arrayOfNulls<Long>(17)

    fun setValue(setvalue: String?, index: Int) {
        value[index] = setvalue
    }

    fun setWho(setvalue: String?, index: Int) {
        who[index] = setvalue
    }

    fun setDuration(setvalue: Long, index: Int) {
        duration[index] = setvalue
    }

    fun setModifiedTime(setvalue: Long?, index: Int) {
        lastModifiedTime[index] = setvalue
    }
}



