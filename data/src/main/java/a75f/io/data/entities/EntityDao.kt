package a75f.io.data.entities

import androidx.room.*

@Dao
interface EntityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity : HayStackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(entities: List<HayStackEntity>)

    @Delete
    fun delete(entity : HayStackEntity)

    @Delete
    fun deleteAll(entities: List<HayStackEntity>)

    @Update
    fun update(entity : HayStackEntity)

    @Update
    fun updateAll(entities: List<HayStackEntity>)

    @Query("SELECT * FROM entities")
    fun getAllEntities(): List<HayStackEntity>

    @Query("DELETE FROM entities WHERE id = :Id")
    fun deleteWithId(Id : String)

    @Query("DELETE FROM entities")
    fun nukeTable()
}