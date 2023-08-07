package a75f.io.data.writablearray


interface DatabaseHelper {

    suspend fun insert(writableArray: WritableArray)
    suspend fun update(writableArray: WritableArray)
    suspend fun delete(writableArray: WritableArray)
    fun getAllwritableArrays(): List<WritableArray>
    suspend fun insertAll(writableArray: List<WritableArray>)
    suspend fun updateAll(writableArray: List<WritableArray>)
    suspend fun deleteAll(writableArray: List<WritableArray>)
    suspend fun deleteWithID(id: String)

    fun getWritableWithId(id : String): WritableArray
}