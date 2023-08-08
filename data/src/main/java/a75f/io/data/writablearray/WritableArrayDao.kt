package a75f.io.data.writablearray

import androidx.room.*


@Dao
interface WritableArrayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(writableArray : WritableArray)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(writableArrays: List<WritableArray>)

    @Delete
    fun delete(writableArray : WritableArray)

    @Delete
    fun deleteAll(writableArrays: List<WritableArray>)

    @Update
    fun update(writableArray : WritableArray)

    @Update
    fun updateAll(writableArrays: List<WritableArray>)

    @Query("SELECT * FROM writableArray")
    fun getAllwritableArrays(): List<WritableArray>


   @Query("DELETE FROM writableArray WHERE Id = :id")
    fun deletewithId(id : String)

    @Query("SELECT * FROM writableArray WHERE Id = :id")
    fun getWritableWithId(id : String) : WritableArray
}