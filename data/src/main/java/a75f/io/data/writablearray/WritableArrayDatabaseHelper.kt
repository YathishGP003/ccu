package a75f.io.data.writablearray

import a75f.io.data.RenatusDatabase


class WritableArrayDatabaseHelper constructor (private val renatusDatabase: RenatusDatabase) :
    DatabaseHelper {
    override suspend fun insert(writableArray: WritableArray) {
        renatusDatabase.writableArrayDao().insert(writableArray)
    }

    override suspend fun update(writableArray: WritableArray) {
        renatusDatabase.writableArrayDao().update(writableArray)
    }

    override suspend fun delete(writableArray: WritableArray) {
        renatusDatabase.writableArrayDao().delete(writableArray)
    }

    override fun getAllwritableArrays(): List<WritableArray> {
        return renatusDatabase.writableArrayDao().getAllwritableArrays()
    }

    override suspend fun insertAll(writableArray: List<WritableArray>) {
        renatusDatabase.writableArrayDao().insertAll(writableArray)
    }

    override suspend fun updateAll(writableArray: List<WritableArray>) {
        renatusDatabase.writableArrayDao().updateAll(writableArray)
    }

    override suspend fun deleteAll(writableArray: List<WritableArray>) {
        renatusDatabase.writableArrayDao().deleteAll(writableArray)
    }

    override suspend fun deleteWithID(id: String) {
        renatusDatabase.writableArrayDao().deletewithId(id)
    }

    override fun getWritableWithId(id: String) : WritableArray {
       return renatusDatabase.writableArrayDao().getWritableWithId(id)
    }
}